package com.puchain.fep.processor.wire;

import com.puchain.fep.converter.model.RequestBusinessHead;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.converter.wire.OutboundWireShapeDispatcher;
import com.puchain.fep.converter.wire.WireShapeDescriptor;
import com.puchain.fep.processor.body.common.LoginRequest9006;
import com.puchain.fep.processor.body.common.LogoutRequest9008;
import com.puchain.fep.processor.validation.AbstractXsdValidationTest;
import com.puchain.fep.processor.validation.ValidationResult;
import com.puchain.fep.processor.validation.XsdValidator;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P4-MSG-L T1 — 9006/9008 outbound wire-shape 注册 + 真 {@link XsdValidator} 合规测试。
 *
 * <p>覆盖两个维度：</p>
 * <ul>
 *   <li>{@link OutboundWireShapeDispatcher#describeFor(String)} 对 9006/9008 返回正确 wire-shape
 *       （{@code RealHead{msgNo}} + {@link RequestBusinessHead} + requiresResultCode=false），以及
 *       已登记上行报文总数 {@code REGISTERED_MSG_NO_COUNT}=39 / {@code REAL_HEAD_REQUEST_MSG_NOS} 含 9006/9008。</li>
 *   <li>{@code LoginRequest9006} / {@code LogoutRequest9008} body POJO marshal 嵌入完整 CFX envelope 后，
 *       用真 {@link XsdValidator} 跑 SUT 实际产物校验 Password minLength=8 / maxLength=32 facet。</li>
 * </ul>
 *
 * <p>红线 {@code feedback_xsd_compliance_fix_real_validator_on_sut}：用真 XsdValidator 跑实际 marshal 产物，
 * 禁 {@code @MockBean} validator。红线 {@code feedback_xsd_validator_requires_full_envelope_redline}：
 * {@code XsdValidator.validate} 的 schema 根定义在 {@code <CFX>} 元素，必须传完整 envelope（HEAD + MSG +
 * RealHead{msgNo} + body），禁直送 body-only fragment。复用
 * {@link AbstractXsdValidationTest#wrapCfxTemplate} 装配完整 envelope +
 * {@link AbstractXsdValidationTest#SHARED_VALIDATOR}。</p>
 *
 * <p><b>落点模块说明（对 Plan v0.2 伪代码的偏离）</b>：Plan §Task1 把本测试类置于 {@code fep-converter} 模块，
 * 但实测依赖方向为 {@code fep-processor → fep-converter}（fep-converter 不可见 {@link XsdValidator} /
 * {@link LoginRequest9006}）。因此本类落 {@code fep-processor}（可同时见 dispatcher 与 validator），
 * 符合红线 {@code feedback_xsd_compliance_fix_real_validator_on_sut} 以"能用真 validator 跑完整 envelope"为准。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class OutboundWireShape9006And9008XsdComplianceTest {

    private static final String SRC_NODE = "10000000000001";
    private static final String DES_NODE = "A1000143000104";
    private static final String APP = "HNDEMP";

    private OutboundWireShapeDispatcher dispatcher;
    private XsdValidator validator;

    @BeforeEach
    void setUp() {
        dispatcher = new OutboundWireShapeDispatcher();
        validator = AbstractXsdValidationTest.SHARED_VALIDATOR;
    }

    @Test
    void describeFor_9006_shouldReturnRealHeadRequest() {
        WireShapeDescriptor desc = dispatcher.describeFor("9006");
        assertThat(desc.headElementName()).isEqualTo("RealHead9006");
        assertThat(desc.headClass()).isEqualTo(RequestBusinessHead.class);
        assertThat(desc.requiresResultCode()).isFalse();
    }

    @Test
    void describeFor_9008_shouldReturnRealHeadRequest() {
        WireShapeDescriptor desc = dispatcher.describeFor("9008");
        assertThat(desc.headElementName()).isEqualTo("RealHead9008");
        assertThat(desc.headClass()).isEqualTo(RequestBusinessHead.class);
        assertThat(desc.requiresResultCode()).isFalse();
    }

    @Test
    void registeredMsgNoCountShouldBe39() {
        assertThat(OutboundWireShapeDispatcher.REGISTERED_MSG_NO_COUNT).isEqualTo(39);
    }

    @Test
    void realHeadRequestMsgNosShouldIncludeNodeLifecycleMsgs() {
        assertThat(OutboundWireShapeDispatcher.REAL_HEAD_REQUEST_MSG_NOS)
                .contains("9006", "9008")
                .hasSize(11);
    }

    @Test
    void loginRequest9006_validPayload_passesXsdValidation() throws Exception {
        LoginRequest9006 body = new LoginRequest9006();
        body.setPassword("Strong#01"); // 9 chars satisfies minLength=8 ≤ 32
        // NewPassword left null → optional minOccurs=0

        String bodyXml = marshal(body, LoginRequest9006.class);
        assertThat(bodyXml).contains("<Password>Strong#01</Password>");
        assertThat(bodyXml).doesNotContain("<NewPassword");

        String envelope = wrap9006(bodyXml);
        ValidationResult result = validator.validate(MessageType.MSG_9006,
                envelope.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid())
                .as("9006 valid payload errors=%s", result.errors())
                .isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void loginRequest9006_passwordTooShort_failsXsdValidation() throws Exception {
        LoginRequest9006 body = new LoginRequest9006();
        body.setPassword("abc"); // 3 chars < minLength=8

        String bodyXml = marshal(body, LoginRequest9006.class);
        String envelope = wrap9006(bodyXml);
        ValidationResult result = validator.validate(MessageType.MSG_9006,
                envelope.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid())
                .as("9006 Password='abc' (3<8) must fail XSD minLength")
                .isFalse();
        assertThat(result.errors())
                .anyMatch(e -> e.contains("minLength"));
    }

    @Test
    void logoutRequest9008_validPayload_passesXsdValidation() throws Exception {
        LogoutRequest9008 body = new LogoutRequest9008();
        body.setPassword("Strong#01"); // 9 chars satisfies minLength=8 ≤ 32

        String bodyXml = marshal(body, LogoutRequest9008.class);
        assertThat(bodyXml).contains("<Password>Strong#01</Password>");

        String envelope = wrap9008(bodyXml);
        ValidationResult result = validator.validate(MessageType.MSG_9008,
                envelope.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid())
                .as("9008 valid payload errors=%s", result.errors())
                .isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void logoutRequest9008_passwordTooShort_failsXsdValidation() throws Exception {
        LogoutRequest9008 body = new LogoutRequest9008();
        body.setPassword("abc"); // 3 chars < minLength=8

        String bodyXml = marshal(body, LogoutRequest9008.class);
        String envelope = wrap9008(bodyXml);
        ValidationResult result = validator.validate(MessageType.MSG_9008,
                envelope.getBytes(StandardCharsets.UTF_8));

        assertThat(result.valid())
                .as("9008 Password='abc' (3<8) must fail XSD minLength")
                .isFalse();
        assertThat(result.errors())
                .anyMatch(e -> e.contains("minLength"));
    }

    private static String wrap9006(final String bodyXml) {
        return AbstractXsdValidationTest.wrapCfxTemplate(
                SRC_NODE, DES_NODE, APP, "9006",
                "20260421100000000003", "20260421100000000004", "20260421", """
                <RealHead9006>
                  <SendOrgCode>10000000000001</SendOrgCode>
                  <EntrustDate>20260421</EntrustDate>
                  <TransitionNo>00000002</TransitionNo>
                </RealHead9006>
                """ + bodyXml);
    }

    private static String wrap9008(final String bodyXml) {
        return AbstractXsdValidationTest.wrapCfxTemplate(
                SRC_NODE, DES_NODE, APP, "9008",
                "20260421100000000006", "20260421100000000007", "20260421", """
                <RealHead9008>
                  <SendOrgCode>10000000000001</SendOrgCode>
                  <EntrustDate>20260421</EntrustDate>
                  <TransitionNo>00000004</TransitionNo>
                </RealHead9008>
                """ + bodyXml);
    }

    private static <T> String marshal(final T body, final Class<T> bodyClass) throws Exception {
        JAXBContext ctx = JAXBContext.newInstance(bodyClass);
        Marshaller m = ctx.createMarshaller();
        m.setProperty(Marshaller.JAXB_FRAGMENT, true);
        StringWriter sw = new StringWriter();
        m.marshal(body, sw);
        return sw.toString();
    }
}
