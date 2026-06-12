package com.puchain.fep.web.outbound.consumer;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.converter.model.CfxMessage;
import com.puchain.fep.converter.model.CommonHead;
import com.puchain.fep.converter.model.RequestBusinessHead;
import com.puchain.fep.converter.model.ResponseBusinessHead;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.converter.wire.OutboundWireShapeDispatcher;
import com.puchain.fep.converter.wire.WireShapeDescriptor;
import com.puchain.fep.converter.xml.JaxbContextCache;
import com.puchain.fep.processor.intake.port.OutboundHeadFields;
import com.puchain.fep.processor.validation.ValidationResult;
import com.puchain.fep.processor.validation.XsdValidator;
import com.puchain.fep.web.outbound.OutboundMessageQueueEntity;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 出站 CFX envelope 组装器（P5 T4，PRD v1.3 §3.2 报文结构 + §3.1.3 TLQ 消息属性）。
 *
 * <p>组装管道（按顺序）：</p>
 * <ol>
 *   <li>{@link MessageType#byMsgNo} 解析 {@code msgNo}（spec S6 {@code orElseThrow}）</li>
 *   <li>{@link OutboundWireShapeDispatcher#describeFor} 路由 wire-shape</li>
 *   <li>{@link CommonHeadComposer#compose} 装配 CommonHead</li>
 *   <li>{@link BodyClassRegistry#resolve} 解析 body 类型 + JAXB unmarshal {@code entity.messageBodyXml}</li>
 *   <li>{@link WireShapeDescriptor#newHeadInstance} 反射 wire-shape head（共享 quality 非阻塞 #2）</li>
 *   <li>JAXB marshal 完整 CFX envelope（CommonHead + wire-shape head 元素 + body）</li>
 *   <li>{@link XsdValidator#validate} 结构校验，失败抛
 *       {@link FepErrorCode#OUTBOUND_5102_XSD_VALIDATION_FAILURE}</li>
 * </ol>
 *
 * <p>故障路径：</p>
 * <ul>
 *   <li>{@code msgNo} 未注册 / 反射 / unmarshal / marshal 失败 →
 *       {@link FepErrorCode#OUTBOUND_5101_ENVELOPE_BUILD_FAILURE}</li>
 *   <li>body class 未注册 → {@link FepErrorCode#OUTBOUND_5107_BODY_CLASS_NOT_FOUND}</li>
 *   <li>XSD 校验失败 → {@link FepErrorCode#OUTBOUND_5102_XSD_VALIDATION_FAILURE}</li>
 *   <li>已抛 {@link FepBusinessException} 透传不再包装（避免错误码丢失）</li>
 * </ul>
 *
 * <p><b>JaxbContextCache 复用</b>：每次 build 调用 {@code getForClasses(CfxMessage.class,
 * wireHeadClass, bodyClass)}。{@link JaxbContextCache} 以 unordered Set 为 key，命中跨多次
 * 调用共享 context（建池开销 50-300ms 一次性）。</p>
 *
 * <p><b>Result 元素占位</b>：RESPONSE 类 wire-shape head（{@link ResponseBusinessHead}，
 * dispatcher RESPONSE 集合 15 报文）含必填 {@code <Result>} 5 位数字字段。空值不会被 marshal
 * 输出（JAXB {@code required=true} 不强制 marshal null 字段），故本组装器在
 * desc.requiresResultCode() 路径下注入占位 {@code "90000"}（表 5.1.2-3 业务处理成功，
 * 2026-06-12 muzhou 拍板由 "00000" 改为码表合法值以兼容 Result 业务规则；
 * 业务真值注入仍由后续 P5 T6 / 业务流程承接）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class OutboundCfxEnvelopeBuilder {

    /** RESPONSE 类 wire-shape head Result 占位（表 5.1.2-3 合法码 90000；业务真值由后续阶段注入）。 */
    static final String RESULT_PLACEHOLDER = "90000";

    private final OutboundWireShapeDispatcher dispatcher;
    private final BodyClassRegistry bodyClassRegistry;
    private final CommonHeadComposer commonHeadComposer;
    private final XsdValidator xsdValidator;
    private final BodyMsgIdGenerator msgIdGenerator;

    /**
     * 构造 builder，5 依赖均不可 {@code null}。
     *
     * @param dispatcher        wire-shape 路由
     * @param bodyClassRegistry msgNo → Body class 注册表
     * @param commonHeadComposer CommonHead 装配器
     * @param xsdValidator      XSD 校验器
     * @param msgIdGenerator    20 位全数字 MsgId 生成器（在 build 入口生成，透传至 CommonHeadComposer 和 Runner）
     */
    public OutboundCfxEnvelopeBuilder(
            final OutboundWireShapeDispatcher dispatcher,
            final BodyClassRegistry bodyClassRegistry,
            final CommonHeadComposer commonHeadComposer,
            final XsdValidator xsdValidator,
            final BodyMsgIdGenerator msgIdGenerator) {
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
        this.bodyClassRegistry = Objects.requireNonNull(bodyClassRegistry, "bodyClassRegistry");
        this.commonHeadComposer = Objects.requireNonNull(commonHeadComposer, "commonHeadComposer");
        this.xsdValidator = Objects.requireNonNull(xsdValidator, "xsdValidator");
        this.msgIdGenerator = Objects.requireNonNull(msgIdGenerator, "msgIdGenerator");
    }

    /**
     * 组装完整 CFX envelope。
     *
     * @param entity     消费侧 entity（提供 {@code messageType} + {@code messageBodyXml}），非 {@code null}
     * @param headFields 反序列化自 {@code entity.getMessageHeadXml()} 的 {@link OutboundHeadFields}，非 {@code null}
     * @return {@link EnvelopeBuildResult}：完整 CFX envelope XML + 其 HEAD/MsgId
     *         （runner 透传 send，统一 TLQ 属性 + entity.msg_id）
     * @throws FepBusinessException 组装或校验失败（错误码见类 Javadoc 故障路径表）
     */
    public EnvelopeBuildResult build(final OutboundMessageQueueEntity entity, final OutboundHeadFields headFields) {
        Objects.requireNonNull(entity, "entity");
        Objects.requireNonNull(headFields, "headFields");
        final String msgNo = entity.getMessageType();
        try {
            // I. 解析 msgNo + 生成本次 build 的 msgId（XSD MsgId type = Number + length 20）
            final MessageType type = MessageType.byMsgNo(msgNo)
                    .orElseThrow(() -> new FepBusinessException(
                            FepErrorCode.OUTBOUND_5101_ENVELOPE_BUILD_FAILURE,
                            "未注册 MessageType: " + msgNo));
            final String msgId = msgIdGenerator.generate();

            // II. dispatch wire-shape
            final WireShapeDescriptor desc = dispatcher.describeFor(msgNo);

            // III. 组装 CommonHead（msgId 透传，CorrMsgId 恒 20 位全零）
            final CommonHead commonHead = commonHeadComposer.compose(entity, headFields, msgId);

            // IV. 解析 body class
            final Class<?> bodyClass = bodyClassRegistry.resolve(msgNo);

            // V. 构造 wire-shape head 实例（Simplify E-1 修订：提前到 unmarshal 之前，
            // 以便步骤 IV.2 unmarshal 与步骤 VI marshal 共享同一 4-class JaxbContextCache 条目，
            // 消除原 1-class bodyCtx 仅用于 unmarshal 的冗余 cache slot）
            final RequestBusinessHead wireHead = desc.newHeadInstance();
            populateWireHead(wireHead, headFields, desc.requiresResultCode());

            // IV.2 body XML JAXB unmarshal — 复用步骤 VI marshalToString 内将再次命中的同一 4-class context
            final JAXBContext envelopeCtx = JaxbContextCache.getForClasses(
                    CfxMessage.class,
                    RequestBusinessHead.class,
                    wireHead.getClass(),
                    bodyClass);
            final Object bodyObj;
            try {
                final Unmarshaller bodyUnmarshaller = envelopeCtx.createUnmarshaller();
                bodyObj = bodyUnmarshaller.unmarshal(new StringReader(entity.getMessageBodyXml()));
            } catch (JAXBException e) {
                throw new FepBusinessException(
                        FepErrorCode.OUTBOUND_5101_ENVELOPE_BUILD_FAILURE,
                        "body XML 反序列化失败: msgNo=" + msgNo, e);
            }

            // VI. marshal 完整 CFX envelope
            final String xml = marshalToString(commonHead, wireHead, bodyObj, desc);

            // VII. XSD validate
            final ValidationResult result = xsdValidator.validate(type, xml.getBytes(StandardCharsets.UTF_8));
            if (!result.valid()) {
                throw new FepBusinessException(
                        FepErrorCode.OUTBOUND_5102_XSD_VALIDATION_FAILURE,
                        "XSD 校验失败: " + String.join(";", result.errors()));
            }

            return new EnvelopeBuildResult(xml, msgId);
        } catch (FepBusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new FepBusinessException(
                    FepErrorCode.OUTBOUND_5101_ENVELOPE_BUILD_FAILURE,
                    "envelope 组装失败: msgNo=" + msgNo, e);
        }
    }

    /** build 产物：完整 CFX envelope + 其 HEAD/MsgId（runner 透传 send，统一 TLQ 属性 + entity.msg_id）。 */
    public record EnvelopeBuildResult(String envelope, String msgId) { }

    /**
     * 将 {@link OutboundHeadFields} 三字段灌注到 wire-shape head；RESPONSE 类 ResponseBusinessHead
     * 额外注入 5 位 Result 占位。
     *
     * @param wireHead              通过反射构造的 head 实例
     * @param hf                    head 字段载体
     * @param requiresResultCode    是否要求 Result（dispatcher RESPONSE 集合 15 报文 true）
     */
    private void populateWireHead(
            final RequestBusinessHead wireHead,
            final OutboundHeadFields hf,
            final boolean requiresResultCode) {
        wireHead.setSendOrgCode(hf.sendOrgCode());
        wireHead.setEntrustDate(hf.entrustDate());
        wireHead.setTransitionNo(hf.transitionNo());
        if (requiresResultCode && wireHead instanceof ResponseBusinessHead resp) {
            // PRD §3.2.4 Result 5 位数字必填，业务真值由后续阶段注入；占位避免 marshal 缺元素
            resp.setResult(RESULT_PLACEHOLDER);
        }
    }

    /**
     * marshal CFX envelope 为字符串。head 元素名按 {@link WireShapeDescriptor#headElementName}
     * 动态指定（{@code RealHead3009} / {@code BatchHead3101} / {@code BatchHead{msgNo}}），
     * 通过 {@link JAXBElement} {@link QName} 实现而非 Java class name。
     *
     * @param commonHead    CommonHead
     * @param wireShapeHead wire-shape head 实例
     * @param body          body POJO
     * @param desc          wire-shape 描述符（取 headElementName）
     * @return CFX envelope XML 字符串
     * @throws JAXBException marshal 失败（外层 catch 包装为 OUTBOUND_5101）
     */
    private String marshalToString(
            final CommonHead commonHead,
            final RequestBusinessHead wireShapeHead,
            final Object body,
            final WireShapeDescriptor desc) throws JAXBException {
        final CfxMessage wrapper = new CfxMessage();
        wrapper.setHead(commonHead);
        final CfxMessage.MsgContainer container = new CfxMessage.MsgContainer();
        // wire-shape head 通过 JAXBElement 动态指定元素名（与 BatchMessageProcessorService.wrapBodyInCfx 同模式）
        @SuppressWarnings("unchecked")
        final Class<RequestBusinessHead> headClass = (Class<RequestBusinessHead>) wireShapeHead.getClass();
        final JAXBElement<RequestBusinessHead> headElement = new JAXBElement<>(
                new QName(desc.headElementName()), headClass, wireShapeHead);
        container.getContents().add(headElement);
        container.getContents().add(body);
        wrapper.setMsgContainer(container);

        // JAXBContext 注册：CfxMessage + RequestBusinessHead（基类）+ wireHead 实际类（子类，
        // 必须同时注册才能解析 ResponseBusinessHead 的 propOrder 包含父类 sendOrgCode/entrustDate/
        // transitionNo 字段；缺父类会触发 JAXB IllegalAnnotationsException）+ body class。
        // 命中 JaxbContextCache 跨调用共享（unordered Set key 命中规则）。
        final JAXBContext ctx = JaxbContextCache.getForClasses(
                CfxMessage.class,
                RequestBusinessHead.class,
                wireShapeHead.getClass(),
                body.getClass());
        final Marshaller marshaller = ctx.createMarshaller();
        final StringWriter sw = new StringWriter();
        marshaller.marshal(wrapper, sw);
        return sw.toString();
    }
}
