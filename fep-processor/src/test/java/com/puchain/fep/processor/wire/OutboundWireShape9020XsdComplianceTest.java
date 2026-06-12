package com.puchain.fep.processor.wire;

import com.puchain.fep.converter.model.ResponseBusinessHead;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.converter.wire.OutboundWireShapeDispatcher;
import com.puchain.fep.converter.wire.WireShapeDescriptor;
import com.puchain.fep.processor.body.common.MsgReturn9020;
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
 * P4-MSG-M T1 — 9020 outbound wire-shape + BodyClassRegistry + 真 {@link XsdValidator} 合规测试。
 *
 * <p>覆盖两个维度：</p>
 * <ul>
 *   <li>{@link OutboundWireShapeDispatcher#describeFor(String)} 对 9020 返回正确 wire-shape
 *       （{@code RealHead9020} + {@link ResponseBusinessHead} + requiresResultCode=true，与 2001/3002
 *       完全同 REAL_HEAD_RESPONSE 类目），以及已登记上行报文总数
 *       {@code REGISTERED_MSG_NO_COUNT}=41 / {@code REAL_HEAD_RESPONSE_MSG_NOS} 含 9020（size 7）。</li>
 *   <li>{@code MsgReturn9020} body POJO marshal 嵌入完整 CFX envelope（RealHead9020 ResponseHead +
 *       Result 5 位占位）后，用真 {@link XsdValidator} 跑 SUT 实际产物校验 OriMsgNo MsgNo length=4 facet。</li>
 * </ul>
 *
 * <p>红线 {@code feedback_xsd_compliance_fix_real_validator_on_sut}：用真 XsdValidator 跑实际 marshal
 * 产物，禁 {@code @MockBean} validator。红线 {@code feedback_xsd_validator_requires_full_envelope_redline}：
 * schema 根定义在 {@code <CFX>} 元素，必须传完整 envelope（HEAD + MSG + RealHead9020 + body），
 * 禁直送 body-only fragment。复用 {@link AbstractXsdValidationTest#wrapCfxTemplate} 装配完整 envelope +
 * {@link AbstractXsdValidationTest#SHARED_VALIDATOR}。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class OutboundWireShape9020XsdComplianceTest {

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
    void describeFor_9020_shouldReturnRealHeadResponse() {
        WireShapeDescriptor desc = dispatcher.describeFor("9020");
        assertThat(desc.headElementName()).isEqualTo("RealHead9020");
        assertThat(desc.headClass()).isEqualTo(ResponseBusinessHead.class);
        assertThat(desc.requiresResultCode()).isTrue();
    }

    @Test
    void registeredMsgNoCountShouldBe41() {
        assertThat(OutboundWireShapeDispatcher.REGISTERED_MSG_NO_COUNT).isEqualTo(41);
    }

    @Test
    void realHeadResponseMsgNosShouldIncludeGeneralResponse() {
        assertThat(OutboundWireShapeDispatcher.REAL_HEAD_RESPONSE_MSG_NOS)
                .contains("9020")
                .hasSize(7);
    }

    @Test
    void msgReturn9020_validPayload_passesXsdValidation() throws Exception {
        MsgReturn9020 body = new MsgReturn9020();
        body.setOriMsgNo("3000"); // valid MsgNo (Number, length=4)

        String bodyXml = marshal(body, MsgReturn9020.class);
        assertThat(bodyXml).contains("<OriMsgNo>3000</OriMsgNo>");

        String envelope = wrap9020(bodyXml);
        ValidationResult result = validator.validate(MessageType.MSG_9020,
                envelope.getBytes(StandardCharsets.UTF_8));
        assertThat(result.valid())
                .as("9020 valid payload errors=%s", result.errors())
                .isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void msgReturn9020_invalidOriMsgNo_failsXsdValidation() throws Exception {
        MsgReturn9020 body = new MsgReturn9020();
        body.setOriMsgNo("abcd"); // 非数字，违反 MsgNo base=Number

        String bodyXml = marshal(body, MsgReturn9020.class);
        String envelope = wrap9020(bodyXml);
        ValidationResult result = validator.validate(MessageType.MSG_9020,
                envelope.getBytes(StandardCharsets.UTF_8));
        assertThat(result.valid())
                .as("9020 OriMsgNo='abcd' 非数字必须 fail")
                .isFalse();
    }

    private static String wrap9020(final String bodyXml) {
        // RealHead9020 ResponseHead: SendOrgCode/EntrustDate/TransitionNo/Result(5位)
        return AbstractXsdValidationTest.wrapCfxTemplate(
                SRC_NODE, DES_NODE, APP, "9020",
                "20260421100000000010", "20260421100000000011", "20260421", """
                <RealHead9020>
                  <SendOrgCode>10000000000001</SendOrgCode>
                  <EntrustDate>20260421</EntrustDate>
                  <TransitionNo>00000010</TransitionNo>
                  <Result>90000</Result>
                </RealHead9020>
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
