package com.puchain.fep.web.collector.mapper;

import com.puchain.fep.common.util.FepConstants;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.validation.ValidationResult;
import com.puchain.fep.processor.validation.XsdValidator;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Task A7 共用 helper — Body POJO → JAXB marshal → CFX envelope → XsdValidator 真验证。
 * 由 5 个 XsdComplianceTest 复用（Rule-of-Three 前置抽取）。
 *
 * <p>装配策略：按 {@code Per3101AndPer3102EnvelopeXsdValidateTest} 既有惯例，
 * JAXB marshal Body POJO（JAXB_FRAGMENT=true）后拼接 CFX envelope（HEAD + MSG wrapper），
 * 再提交全 CFX XML 给 {@link XsdValidator}（与 XSD 根元素 {@code <CFX>} 匹配）。
 *
 * <p>v0.4: 改为全 CFX envelope 验证，修复原版 body-only marshal 导致
 * {@code cvc-elt.1.a} XSD "找不到元素声明" 错误。
 *
 * @since 1.0.0
 */
@Component
public class XsdComplianceHelper {

    /** 固定 SendOrgCode fixture — OrgCode length=14（DataType.xsd）。 */
    private static final String SEND_ORG_CODE = "12345678901234";

    /** 固定 EntrustDate fixture — Date pattern yyyyMMdd。 */
    private static final String ENTRUST_DATE = "20260501";

    /** 固定 TransitionNo fixture — Number length=8（DataType.xsd）。 */
    private static final String TRANSITION_NO = "00000001";

    /** 固定 MsgId/CorrMsgId fixture — Number length=20（DataType.xsd）。 */
    private static final String MSG_ID = "20260501120000000001";

    /** 固定 WorkDate fixture — Date pattern yyyyMMdd。 */
    private static final String WORK_DATE = "20260501";

    /** RequestHead 字段集（无 Result）— 3102/3109/3116/3009 适用。 */
    private static final String REQUEST_HEAD_FIELDS =
            "<SendOrgCode>" + SEND_ORG_CODE + "</SendOrgCode>"
            + "<EntrustDate>" + ENTRUST_DATE + "</EntrustDate>"
            + "<TransitionNo>" + TRANSITION_NO + "</TransitionNo>";

    /** ResponseHead 字段集（含 Result）— 3101 适用（BatchHead3101 type=ResponseHead）。 */
    private static final String RESPONSE_HEAD_FIELDS =
            "<SendOrgCode>" + SEND_ORG_CODE + "</SendOrgCode>"
            + "<EntrustDate>" + ENTRUST_DATE + "</EntrustDate>"
            + "<TransitionNo>" + TRANSITION_NO + "</TransitionNo>"
            + "<Result>00000</Result>";

    private final XsdValidator validator;

    /**
     * 构造 XsdComplianceHelper。
     *
     * @param validator 真实 XsdValidator（Spring context 注入，非 MockBean）
     */
    @Autowired
    public XsdComplianceHelper(final XsdValidator validator) {
        this.validator = validator;
    }

    /**
     * 验证 Body POJO 经 JAXB marshal 后，包裹在对应 msgNo 的 CFX envelope 中能通过真 XSD 校验。
     *
     * <p>按 {@code Per3101AndPer3102EnvelopeXsdValidateTest} 惯例：
     * <ol>
     *   <li>JAXB marshal Body POJO（JAXB_FRAGMENT=true）为 body XML fragment</li>
     *   <li>拼接 CFX envelope（HEAD + MSG wrapper，HEAD element / 字段集按 msgNo 选取）</li>
     *   <li>提交全 CFX XML 给 {@link XsdValidator#validate(MessageType, byte[])}（真 XSD 校验）</li>
     * </ol>
     *
     * @param msgNo Body 对应 msgNo (如 "3101")
     * @param body  Body POJO 实例 (如 ContractInfo3101)
     * @throws Exception JAXB marshal 失败或 XSD 校验失败 (含 errors)
     */
    public void validateMapperOutput(final String msgNo, final Object body) throws Exception {
        final Optional<MessageType> type = MessageType.byMsgNo(msgNo);
        assertThat(type)
                .as("MessageType.byMsgNo(%s) must be registered", msgNo)
                .isPresent();

        // Step 1: marshal body POJO as XML fragment (no XML declaration)
        final JAXBContext ctx = JAXBContext.newInstance(body.getClass());
        final Marshaller m = ctx.createMarshaller();
        m.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.FALSE);
        final StringWriter sw = new StringWriter();
        m.marshal(body, sw);
        final String bodyFragment = sw.toString();

        // Step 2: build full CFX envelope
        final String cfxXml = buildCfxEnvelope(msgNo, bodyFragment);

        // Step 3: validate full CFX XML against real XSD
        final ValidationResult result = validator.validate(
                type.get(), cfxXml.getBytes(StandardCharsets.UTF_8));
        assertThat(result.valid())
                .as("XSD validation must pass for msgNo=%s, errors=%s", msgNo, result.errors())
                .isTrue();
    }

    /** Envelope config: head element name + head fields XML fragment. */
    private record EnvelopeConfig(String headElement, String headFields) { }

    /**
     * 构造 msgNo 对应的完整 CFX envelope XML（HEAD + MSG wrapper + body fragment）。
     *
     * <p>HEAD element 和 body head 字段集按 msgNo 查 XSD schema：
     * <ul>
     *   <li>3101: BatchHead3101 (ResponseHead = SendOrgCode + EntrustDate + TransitionNo + Result)</li>
     *   <li>3102: BatchHead3102 (RequestHead)</li>
     *   <li>3109: BatchHead3109 (RequestHead)</li>
     *   <li>3116: BatchHead3116 (RequestHead)</li>
     *   <li>3009: RealHead3009 (RequestHead)</li>
     *   <li>3105: BatchHead3105 (RequestHead)</li>
     *   <li>3107: BatchHead3107 (RequestHead)</li>
     * </ul>
     *
     * @param msgNo        4-digit message number
     * @param bodyFragment JAXB-marshaled body XML fragment (no declaration)
     * @return complete CFX XML string
     */
    private static String buildCfxEnvelope(final String msgNo, final String bodyFragment) {
        final EnvelopeConfig cfg = switch (msgNo) {
            case "3101" -> new EnvelopeConfig("BatchHead3101", RESPONSE_HEAD_FIELDS);
            case "3102" -> new EnvelopeConfig("BatchHead3102", REQUEST_HEAD_FIELDS);
            case "3109" -> new EnvelopeConfig("BatchHead3109", REQUEST_HEAD_FIELDS);
            case "3116" -> new EnvelopeConfig("BatchHead3116", REQUEST_HEAD_FIELDS);
            case "3009" -> new EnvelopeConfig("RealHead3009", REQUEST_HEAD_FIELDS);
            case "3105" -> new EnvelopeConfig("BatchHead3105", REQUEST_HEAD_FIELDS);
            case "3107" -> new EnvelopeConfig("BatchHead3107", REQUEST_HEAD_FIELDS);
            default -> throw new IllegalArgumentException(
                    "Unsupported msgNo for XsdComplianceHelper: " + msgNo);
        };

        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<CFX>"
                + "<HEAD>"
                + "<Version>1.0</Version>"
                + "<SrcNode>12345678901234</SrcNode>"
                + "<DesNode>" + FepConstants.HNDEMP_NODE_CODE + "</DesNode>"
                + "<App>HNDEMP</App>"
                + "<MsgNo>" + msgNo + "</MsgNo>"
                + "<MsgId>" + MSG_ID + "</MsgId>"
                + "<CorrMsgId>" + MSG_ID + "</CorrMsgId>"
                + "<WorkDate>" + WORK_DATE + "</WorkDate>"
                + "</HEAD>"
                + "<MSG>"
                + "<" + cfg.headElement() + ">" + cfg.headFields() + "</" + cfg.headElement() + ">"
                + bodyFragment
                + "</MSG>"
                + "</CFX>";
    }
}
