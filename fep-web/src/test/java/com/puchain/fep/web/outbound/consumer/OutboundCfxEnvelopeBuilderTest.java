package com.puchain.fep.web.outbound.consumer;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.converter.wire.OutboundWireShapeDispatcher;
import com.puchain.fep.processor.intake.port.OutboundHeadFields;
import com.puchain.fep.processor.validation.ValidationResult;
import com.puchain.fep.processor.validation.XsdValidator;
import com.puchain.fep.web.outbound.OutboundMessageQueueEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P5 T4 Step 7 — {@link OutboundCfxEnvelopeBuilder} 单元测试。
 *
 * <p>覆盖：</p>
 * <ul>
 *   <li>3101 → wire-shape head 元素 {@code <BatchHead3101>} + ResponseBusinessHead Result 字段</li>
 *   <li>3009 → wire-shape head 元素 {@code <RealHead3009>}（无 Result）</li>
 *   <li>非法 msgNo "9999" → {@link MessageType#byMsgNo} Optional empty → OUTBOUND_5101</li>
 *   <li>XSD validate 失败 → OUTBOUND_5102（task prompt 要求新增的第 4 个测试）</li>
 * </ul>
 *
 * <p><b>策略</b>：Mock {@link XsdValidator} 返回 {@code ValidationResult.ok()} 即可断言
 * builder 的 envelope 装配结果（不做真 XSD 校验，因为合成空 body POJO 不满足 XSD 必填字段）。
 * 第 4 个测试单独 stub validator 返回 failed 来验证 OUTBOUND_5102 路径。</p>
 *
 * <p><b>3101 body XML</b>：使用 ContractInfo3101 的 @XmlRootElement 实测值
 * {@code <ContractInfo3101/>}（capital C，源自 fep-processor body POJO）。</p>
 *
 * <p><b>3009 body XML</b>：使用 RzReturnInfo3009 的 @XmlRootElement 实测值
 * {@code <rzReturnInfo3009/>}（lowercase r，源自 fep-processor body POJO 的 XSD 命名）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class OutboundCfxEnvelopeBuilderTest {

    private OutboundWireShapeDispatcher dispatcher;
    private BodyClassRegistry registry;
    private CommonHeadComposer composer;
    private XsdValidator xsdValidator;
    private OutboundCfxEnvelopeBuilder builder;

    @BeforeEach
    void setUp() {
        dispatcher = new OutboundWireShapeDispatcher();
        registry = new BodyClassRegistry();
        composer = new CommonHeadComposer();
        xsdValidator = mock(XsdValidator.class);
        when(xsdValidator.validate(any(), any())).thenReturn(ValidationResult.ok());
        final BodyMsgIdGenerator msgIdGenerator = new BodyMsgIdGenerator(
                Clock.fixed(Instant.parse("2026-05-25T06:00:00Z"), ZoneOffset.UTC));
        builder = new OutboundCfxEnvelopeBuilder(dispatcher, registry, composer, xsdValidator, msgIdGenerator);
    }

    @Test
    void build_3101_should_contain_BatchHead3101_and_Result_element() {
        final OutboundMessageQueueEntity entity = givenEntity("3101", "<ContractInfo3101/>");
        final OutboundHeadFields headFields = new OutboundHeadFields(
                "BANK0010000001", "20260505", "00000002");

        final OutboundCfxEnvelopeBuilder.EnvelopeBuildResult built = builder.build(entity, headFields);

        assertThat(built.envelope()).contains("<BatchHead3101");
        assertThat(built.envelope()).contains("<Result>");
        assertThat(built.envelope()).contains("</CFX>");
        verify(xsdValidator).validate(eq(MessageType.MSG_3101), any(byte[].class));
    }

    @Test
    void build_3009_should_contain_RealHead3009_no_Result() {
        final OutboundMessageQueueEntity entity = givenEntity("3009", "<rzReturnInfo3009/>");
        final OutboundHeadFields headFields = new OutboundHeadFields(
                "BANK0010000001", "20260505", "00000001");

        final OutboundCfxEnvelopeBuilder.EnvelopeBuildResult built = builder.build(entity, headFields);

        assertThat(built.envelope()).contains("<RealHead3009");
        // 3009 用 RequestBusinessHead 不含 Result 字段
        assertThat(built.envelope()).doesNotContain("<Result>");
        verify(xsdValidator).validate(eq(MessageType.MSG_3009), any(byte[].class));
    }

    @Test
    void build_invalid_msgNo_should_throw_5101() {
        // entity.getMessageType() == "9999"，MessageType.byMsgNo Optional empty → orElseThrow
        final OutboundMessageQueueEntity entity = givenEntity("9999", "<x/>");
        final OutboundHeadFields headFields = new OutboundHeadFields(
                "BANK0010000001", "20260505", "00000099");

        assertThatThrownBy(() -> builder.build(entity, headFields))
                .isInstanceOf(FepBusinessException.class)
                .hasFieldOrPropertyWithValue(
                        "errorCode", FepErrorCode.OUTBOUND_5101_ENVELOPE_BUILD_FAILURE);
    }

    @Test
    void build_xsd_validation_failure_should_throw_5102() {
        when(xsdValidator.validate(any(), any())).thenReturn(
                ValidationResult.failed(List.of("line 1 col 1: cvc-complex-type.2.4.a: missing SerialNo")));
        final OutboundMessageQueueEntity entity = givenEntity("3102", "<ArchiveInfo3102/>");
        final OutboundHeadFields headFields = new OutboundHeadFields(
                "BANK0010000001", "20260505", "00000004");

        assertThatThrownBy(() -> builder.build(entity, headFields))
                .isInstanceOf(FepBusinessException.class)
                .hasFieldOrPropertyWithValue(
                        "errorCode", FepErrorCode.OUTBOUND_5102_XSD_VALIDATION_FAILURE);
    }

    private OutboundMessageQueueEntity givenEntity(final String msgNo, final String bodyXml) {
        final OutboundMessageQueueEntity e = new OutboundMessageQueueEntity();
        e.setMessageType(msgNo);
        e.setMessageBodyXml(bodyXml);
        // messageHeadXml 不在 builder 内被反序列化（headFields 由 Runner 上层提供），合成占位即可
        e.setMessageHeadXml("<OutboundHeadFields/>");
        return e;
    }
}
