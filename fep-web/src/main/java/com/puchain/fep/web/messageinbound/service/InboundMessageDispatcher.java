package com.puchain.fep.web.messageinbound.service;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.converter.model.CfxMessage;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.converter.xml.JaxbContextCache;
import com.puchain.fep.processor.body.supplychain.BankCheckDay3116;
import com.puchain.fep.processor.body.supplychain.PlatPay3115;
import com.puchain.fep.processor.body.supplychain.PzCheckQuery3107;
import com.puchain.fep.processor.body.supplychain.PzCheckQueryReturn3108;
import com.puchain.fep.processor.event.InboundMessageProcessedEvent;
import com.puchain.fep.processor.pipeline.SyncMessageProcessorService;
import com.puchain.fep.processor.state.MessageProcessRecord;
import com.puchain.fep.processor.state.MessageProcessStatus;
import com.puchain.fep.web.messageinbound.dto.InboundMessageResponse;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * 入站报文分发器（PRD §5.3.2.13 + §1991 状态机）。
 *
 * <p>P3 Task 2 — 把"路由 + 持久化 + body 解析 + 事件发布"四件事串起来：</p>
 * <ol>
 *   <li>{@link MessageType#byMsgNo(String)} 反查报文类型 — 未注册抛
 *       {@link FepErrorCode#MSG_INBOUND_INVALID_TYPE}。</li>
 *   <li>委派 {@link SyncMessageProcessorService#processInbound} 完成 XSD 校验
 *       + RECEIVED→VALIDATED→PROCESSING→COMPLETED 状态流转。</li>
 *   <li>仅当流水线终态 {@link MessageProcessStatus#COMPLETED} 时再 unmarshal body
 *       并 publish {@link InboundMessageProcessedEvent}；FAILED 不发事件。</li>
 *   <li>整方法标 {@link Transactional}（v1a P0-Q1） — listener 同步同事务，
 *       listener 抛异常会回滚 message_process_record + 下游 reconciliation_records。</li>
 * </ol>
 *
 * <p>Body POJO 解析使用本地 JAXBContext 缓存（per body class），按 P3 Phase 2
 * 范围登记 4 种业务 body：3107 / 3108 / 3115 / 3116。其他 messageType 不解析 body，
 * 事件 {@code body} 字段留 null（listener 自行降级处理）。</p>
 *
 * <p>所有日志参数走 {@link LogSanitizer#sanitize}，防御 CRLF 日志注入。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class InboundMessageDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(InboundMessageDispatcher.class);

    /**
     * P3 Phase 2 注册的 body POJO 反查表：messageType.msgNo → body class。
     * 仅用于 dispatcher 解析 body POJO；listener 各自再做安全 cast。
     */
    private static final Map<String, Class<?>> BODY_TYPE_REGISTRY = Map.of(
            MessageType.MSG_3107.msgNo(), PzCheckQuery3107.class,
            MessageType.MSG_3108.msgNo(), PzCheckQueryReturn3108.class,
            MessageType.MSG_3115.msgNo(), PlatPay3115.class,
            MessageType.MSG_3116.msgNo(), BankCheckDay3116.class);

    private final SyncMessageProcessorService syncProcessor;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Spring 构造注入。
     *
     * @param syncProcessor  同步流水线，非空
     * @param eventPublisher Spring 事件发布器，非空
     */
    public InboundMessageDispatcher(final SyncMessageProcessorService syncProcessor,
                                    final ApplicationEventPublisher eventPublisher) {
        this.syncProcessor = Objects.requireNonNull(syncProcessor, "syncProcessor");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
    }

    /**
     * 分发入站报文：路由 → 同步处理 → body 解析 → 事件发布。
     *
     * <p>事务边界：{@link Propagation#REQUIRED} + 任意异常回滚。listener 在同
     * 事务上下文中执行（Spring {@code @EventListener} 默认同步同事务），
     * listener 抛异常时整事务回滚 — message_process_record 与
     * reconciliation_records 一致提交或一致回滚。</p>
     *
     * @param messageType  4 位数字报文类型，非空（已被 controller {@code @Pattern} 校验）
     * @param transitionNo 8 位业务流水号，非空
     * @param xml          UTF-8 XML payload，非空
     * @return 处理响应：recordId / status / eventPublished
     * @throws FepBusinessException {@link FepErrorCode#MSG_INBOUND_INVALID_TYPE}
     *                               未注册的 messageType
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public InboundMessageResponse dispatch(final String messageType,
                                            final String transitionNo,
                                            final byte[] xml) {
        Objects.requireNonNull(messageType, "messageType");
        Objects.requireNonNull(transitionNo, "transitionNo");
        Objects.requireNonNull(xml, "xml");

        final MessageType type = MessageType.byMsgNo(messageType)
                .orElseThrow(() -> new FepBusinessException(
                        FepErrorCode.MSG_INBOUND_INVALID_TYPE,
                        "messageType=" + LogSanitizer.sanitize(messageType)));

        final MessageProcessRecord record = syncProcessor.processInbound(type, transitionNo, xml);

        boolean eventPublished = false;
        if (record.getStatus() == MessageProcessStatus.COMPLETED) {
            final Object body = tryUnmarshalBody(type, xml);
            final String serialNo = extractSerialNo(body, transitionNo);
            eventPublisher.publishEvent(new InboundMessageProcessedEvent(
                    type, transitionNo, serialNo, body, Instant.now()));
            eventPublished = true;
            LOG.info("inbound dispatched msg={} transitionNo={} recordId={} eventPublished=true",
                    type.msgNo(),
                    LogSanitizer.sanitize(transitionNo),
                    record.getId());
        } else {
            LOG.warn("inbound pipeline non-completed msg={} transitionNo={} recordId={} status={}",
                    type.msgNo(),
                    LogSanitizer.sanitize(transitionNo),
                    record.getId(),
                    record.getStatus());
        }

        return new InboundMessageResponse(record.getId(), record.getStatus().name(), eventPublished);
    }

    /**
     * 按 messageType 查 P3 Phase 2 注册表，使用 per-class 缓存的 JAXBContext
     * 反序列化 CfxMessage 并按 isInstance 过滤取首个匹配的 body POJO。
     *
     * <p><b>Envelope shape contract</b>: 生产 CFX {@code <MSG>} 容器由 XSD 强制
     * BatchHeadXxxx 在前 / Body POJO 在后两子元素并存（参见 PRD §3.2.4）。
     * BatchHead 类型未在 dispatcher JAXBContext 注册，回退为 lax-mode
     * {@code org.w3c.dom.Element}；用 {@code msg.getBody()} 取 position 0
     * 永远拿到 BatchHead DOM Element 而非真 body POJO（P3 Task 5 IT 暴露的
     * 关键 wiring bug — 旧实现 listener body=null silent skip）。</p>
     *
     * <p>修复后逻辑遍历 {@code msg.getBodies()} 并按 {@code bodyClass::isInstance}
     * 过滤，对 (a) 单 body 报文 (b) BatchHead+body 双子元素 (c) 0 命中 (d) 多
     * body 候选 四类 envelope 全部正确。0 命中时返回 null + LOG.warn，由
     * dispatcher caller 处理；listener 端 null-check 兜底。</p>
     *
     * <p>未注册的 messageType 返回 {@code null}（listener 自行降级）；
     * unmarshal 失败抛 {@link FepBusinessException} 让 {@link Transactional}
     * 整事务回滚（v1a P0-Q1 + santa 教训：unmarshal 失败 = 数据不一致风险）。</p>
     *
     * @param type 入站报文类型，非空
     * @param xml  原始 XML，非空
     * @return 解析后的 body POJO 或 {@code null}
     * @throws FepBusinessException unmarshal 失败时让事务回滚
     */
    private Object tryUnmarshalBody(final MessageType type, final byte[] xml) {
        final Class<?> bodyClass = BODY_TYPE_REGISTRY.get(type.msgNo());
        if (bodyClass == null) {
            return null;
        }
        try {
            final JAXBContext ctx = JaxbContextCache.getForBody(bodyClass);
            final Unmarshaller u = ctx.createUnmarshaller();
            u.setEventHandler(event -> false);
            final CfxMessage msg = (CfxMessage) u.unmarshal(new ByteArrayInputStream(xml));
            // P3 Task 5 finding: production CFX samples carry both BatchHeadXxxx
            // (lax-mode DOM Element) and the registered body class as siblings
            // under <MSG>. Walk the bodies list and pick the first registered-
            // type instance instead of trusting position 0 — otherwise the
            // BatchHead DOM element shadows the real body POJO and the listener
            // receives event.body=null (P3 wiring inert). XSD-required sequence
            // ordering (BatchHead → body) means getBody() is always wrong here.
            for (Object candidate : msg.getBodies()) {
                if (candidate != null && bodyClass.isInstance(candidate)) {
                    return candidate;
                }
            }
            LOG.warn("inbound body type mismatch msg={} expected={} bodies={}",
                    type.msgNo(),
                    bodyClass.getSimpleName(),
                    msg.getBodies().size());
            return null;
        } catch (JAXBException e) {
            throw new FepBusinessException(
                    FepErrorCode.MSG_INBOUND_DECODE_FAILURE,
                    "unmarshal body failed for msg=" + type.msgNo(), e);
        }
    }

    /**
     * 从 body POJO 反射调用 {@code getSerialNo()}；body 为 null 或方法不存在时
     * fallback 到 {@code transitionNo}。所有 P3 Phase 2 注册 body 都有此方法
     * （grep 实测 BankCheckDay3116/PzCheckQuery3107/PzCheckQueryReturn3108/PlatPay3115）。
     *
     * @param body         body POJO，可空
     * @param transitionNo 备用流水号，非空
     * @return 业务 serialNo（非 null）
     */
    private static String extractSerialNo(final Object body, final String transitionNo) {
        if (body == null) {
            return transitionNo;
        }
        try {
            final Method getter = body.getClass().getMethod("getSerialNo");
            final Object value = getter.invoke(body);
            if (value instanceof String s && !s.isEmpty()) {
                return s;
            }
        } catch (NoSuchMethodException e) {
            // body 类型未公开 getSerialNo，回退到 transitionNo
            LOG.debug("body class={} lacks getSerialNo; fallback to transitionNo",
                    body.getClass().getSimpleName());
        } catch (IllegalAccessException | InvocationTargetException e) {
            LOG.warn("body getSerialNo invocation failed class={}",
                    body.getClass().getSimpleName(), e);
        }
        return transitionNo;
    }

    /**
     * 暴露给单测验证 body 反查表内容（避免单测做反射读取私有静态字段）。
     * 返回值是显式 {@link Map#copyOf} 副本，确保调用方无法改写内部静态注册表。
     *
     * @return 4 种 body POJO 注册表的不可变副本
     */
    public static Map<String, Class<?>> bodyTypeRegistry() {
        return Map.copyOf(BODY_TYPE_REGISTRY);
    }
}
