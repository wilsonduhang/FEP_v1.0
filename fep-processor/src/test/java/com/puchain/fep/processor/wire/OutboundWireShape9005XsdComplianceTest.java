package com.puchain.fep.processor.wire;

import com.puchain.fep.converter.model.RequestBusinessHead;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.converter.wire.OutboundWireShapeDispatcher;
import com.puchain.fep.converter.wire.WireShapeDescriptor;
import com.puchain.fep.processor.validation.AbstractXsdValidationTest;
import com.puchain.fep.processor.validation.ValidationResult;
import com.puchain.fep.processor.validation.XsdValidator;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P4-MSG-N T1 — 9005 节点心跳 head-only outbound wire-shape + 真 {@link XsdValidator} 合规测试。
 *
 * <p>9005.xsd MSG 仅 RealHead9005（type=RequestHead）无 body 元素 — head-only。镜像 9006/9008 双轨
 * （dispatcher 覆盖注册，走 TlqNodeLoginService 直接路径，不经 OutboundCfxEnvelopeBuilder，
 * 不入 BodyClassRegistry）。红线 {@code feedback_xsd_validator_requires_full_envelope_redline}：
 * schema 根定义在 {@code <CFX>} 元素，必须传完整 CFX envelope（HEAD + MSG + RealHead9005，
 * MSG 内无 body），禁直送 head-only fragment。红线
 * {@code feedback_xsd_compliance_fix_real_validator_on_sut}：用真 {@link XsdValidator} 跑实际产物，
 * 禁 {@code @MockBean} validator。复用 {@link AbstractXsdValidationTest#wrapCfxTemplate} 装配完整
 * envelope + {@link AbstractXsdValidationTest#SHARED_VALIDATOR}。</p>
 *
 * <p><b>落点模块说明</b>：与 {@code OutboundWireShape9006And9008XsdComplianceTest} 同，本类落
 * {@code fep-processor}（依赖方向 fep-processor → fep-converter，可同时见 dispatcher 与
 * {@link XsdValidator}）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class OutboundWireShape9005XsdComplianceTest {

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
    void describeFor_9005_shouldReturnRealHeadRequest() {
        WireShapeDescriptor desc = dispatcher.describeFor("9005");
        assertThat(desc.headElementName()).isEqualTo("RealHead9005");
        assertThat(desc.headClass()).isEqualTo(RequestBusinessHead.class);
        assertThat(desc.requiresResultCode()).isFalse();
    }

    @Test
    void registeredMsgNoCountShouldBe41() {
        assertThat(OutboundWireShapeDispatcher.REGISTERED_MSG_NO_COUNT).isEqualTo(41);
    }

    @Test
    void realHeadRequestMsgNosShouldIncludeHeartbeat() {
        assertThat(OutboundWireShapeDispatcher.REAL_HEAD_REQUEST_MSG_NOS)
                .contains("9005")
                .hasSize(12);
    }

    @Test
    void heartbeat9005_headOnly_passesXsdValidation() {
        // head-only：msgInnerXml 仅 RealHead9005，无 body
        String envelope = wrap9005("""
                <RealHead9005>
                  <SendOrgCode>10000000000001</SendOrgCode>
                  <EntrustDate>20260421</EntrustDate>
                  <TransitionNo>00000005</TransitionNo>
                </RealHead9005>
                """);
        ValidationResult result = validator.validate(MessageType.MSG_9005,
                envelope.getBytes(StandardCharsets.UTF_8));
        assertThat(result.valid())
                .as("9005 head-only valid envelope errors=%s", result.errors())
                .isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    void heartbeat9005_invalidTransitionNo_failsXsdValidation() {
        // TransitionNo type=Number length=8（DataType.xsd:425），'BAD' 违反 length=8 + pattern [0-9]*
        // 单一约束族（TransitionNo），其余字段满足 RequestHead facet
        String envelope = wrap9005("""
                <RealHead9005>
                  <SendOrgCode>10000000000001</SendOrgCode>
                  <EntrustDate>20260421</EntrustDate>
                  <TransitionNo>BAD</TransitionNo>
                </RealHead9005>
                """);
        ValidationResult result = validator.validate(MessageType.MSG_9005,
                envelope.getBytes(StandardCharsets.UTF_8));
        assertThat(result.valid())
                .as("9005 TransitionNo='BAD' 违反 TransitionNo facet 必须 fail")
                .isFalse();
    }

    private static String wrap9005(final String realHeadXml) {
        return AbstractXsdValidationTest.wrapCfxTemplate(
                SRC_NODE, DES_NODE, APP, "9005",
                "20260421100000000020", "20260421100000000021", "20260421", realHeadXml);
    }
}
