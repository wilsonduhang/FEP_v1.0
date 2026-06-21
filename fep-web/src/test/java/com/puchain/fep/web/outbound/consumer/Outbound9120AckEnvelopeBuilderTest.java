package com.puchain.fep.web.outbound.consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.processor.intake.port.OutboundHeadFields;
import com.puchain.fep.web.outbound.OutboundMessageQueueEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * P4-MSG-I T5 — P0 缺口闭合证明：2101 模式 6 ack 装配段端到端 IT（{@code @SpringBootTest}
 * + 真 Spring context wiring + 真实 {@link OutboundCfxEnvelopeBuilder} 装配链路）.
 *
 * <p><b>背景缺口（Plan §1.2 实测调用链 + commit 59ba4c7/76fe0fc 闭合）</b>：
 * {@code BizMessage2101InboundListener.enqueue9120Ack} 在 2101 inbound 后将
 * {@code messageType="9120"} + body={@code MsgReturn9120} 入
 * {@code outbound_message_queue}。consumer 侧
 * {@link OutboundCfxEnvelopeBuilder#build} 双查
 * {@code dispatcher.describeFor("9120")} +
 * {@code bodyClassRegistry.resolve("9120")} —— P4-MSG-I T1+T2 之前两查均抛
 * {@code OUTBOUND_5108_MSGNO_INVALID} / {@code OUTBOUND_5107_BODY_CLASS_NOT_FOUND}，
 * 致 9120 ack 永远无法上线，2101 模式 6 流断在装配段（PRD v1.3 §模式 6:863 强制 9120 ack）。</p>
 *
 * <p><b>既有覆盖盲区</b>：{@code Inbound2101WireTest} AC-3 仅断言
 * ack 入队（status=PENDING + body XML 持久化），<b>未</b>驱动
 * {@code OutboundQueueRunner → OutboundCfxEnvelopeBuilder.build("9120")}
 * 装配段，故缺口在 P4-MSG-I 之前从未被既有 IT 捕获。</p>
 *
 * <p><b>本 IT 的 P0 闭合证明语义</b>：
 * <ol>
 *   <li>显式以"9120" + {@code MsgReturn9120} body XML 调用 {@code build}，
 *       证明 dispatcher/registry 双查均通过（不抛 {@code OUTBOUND_5108/5107}）。</li>
 *   <li>断言产物 envelope 含 {@code <BatchHead9120>} 元素（BatchHead +
 *       ResponseBusinessHead 类目，{@code OutboundWireShapeDispatcher.BATCH_HEAD_RESPONSE_MSG_NOS}
 *       含 9120）+ {@code <MsgReturn9120>} body 元素 + {@code <MsgNo>9120</MsgNo>} CommonHead
 *       + {@code <Result>90000</Result>} 占位（ResponseBusinessHead requiresResultCode=true）。</li>
 *   <li>本 IT 在 T1+T2 之前会 FAIL（5107/5108）；T1+T2 后 PASS，作为缺口闭合的
 *       <b>回归基准</b>。T1+T2 已 ship，本 IT 直接 PASS 证明装配段已通。</li>
 * </ol></p>
 *
 * <p><b>真 XsdValidator（R-NEW-1 起）</b>：自 2026-05-26 R-NEW-1 起，本测试不再
 * {@code @MockBean XsdValidator}，由 Spring context 注入真实 bean。fixture
 * {@code BODY_XML_9120_ACK} 字段值满足 9120.xsd MsgReturn9120 sequence：
 * OriMsgNo MsgNo {@code length=4} numeric + Debug Text optional。envelope HEAD 由
 * 5-25 P0 fix 的 {@code BodyMsgIdGenerator} 注入 20 数字 MsgId + {@code CommonHeadComposer}
 * 注入 CorrMsgId 20 零 + App=HNDEMP；BatchHead9120 ResponseHead 含 Result="90000"
 * placeholder（{@code OutboundCfxEnvelopeBuilder} 装配段）。与 sibling
 * {@link OutboundEnvelopeXsdComplianceTest} 同 cache key（统一 ApplicationContext 复用）。</p>
 *
 * <p><b>命名沿用 sibling {@code Outbound1101WireTest} / {@code OutboundCommonForwardWireTest}
 * （{@code Test} 后缀以纳入 Surefire 默认 include {@code *Test.java}，
 * 红线 P2b-DEFECT-002 + CLAUDE.md 已知约束 / 主动披露 Plan §3 T5 表
 * {@code IT.java} 命名偏离）。</b></p>
 *
 * <p>PRD 依据: §3.2 报文结构 + §模式 6:863 2101 强制 9120 ack + §3.1.3 TLQ 消息属性。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@TestPropertySource(properties = {
        "fep.collector.scheduling.enabled=false",
        "management.health.redis.enabled=false"
})
class Outbound9120AckEnvelopeBuilderTest {

    /** 9120 ack 业务体 — 最小满足 {@code MsgReturn9120} complexType sequence (OriMsgNo required, Debug optional)。 */
    private static final String BODY_XML_9120_ACK = "<MsgReturn9120>"
            + "<OriMsgNo>2101</OriMsgNo>"
            + "<Debug>mode-6-ack</Debug>"
            + "</MsgReturn9120>";

    /**
     * 负向 fixture（DEF-Q-NEW-1 NC-2）— 省略 {@code MsgReturn9120} 必填元素 OriMsgNo
     * （9120.xsd 中 OriMsgNo type=MsgNo 无 minOccurs ⇒ 默认必填）。经 build 内宽松 JAXB
     * unmarshal（oriMsgNo=null，不抛）→ marshal 省略该元素 → 真 {@code XsdValidator}
     * 判缺必填元素 → {@code OUTBOUND_5102}。
     */
    private static final String BODY_XML_9120_MISSING_ORIMSGNO = "<MsgReturn9120>"
            + "<Debug>mode-6-ack</Debug>"
            + "</MsgReturn9120>";

    /** {@code OutboundHeadFields.sendOrgCode} fixture — OrgCode length=14 实测约束。 */
    private static final String SEND_ORG_CODE = "BANK0010000001";

    /** {@code OutboundHeadFields.entrustDate} fixture — Date pattern yyyyMMdd 实测约束。 */
    private static final String ENTRUST_DATE = "20260519";

    /** {@code OutboundHeadFields.transitionNo} fixture — TransitionNo length=8 实测约束。 */
    private static final String TRANSITION_NO = "00002101";

    @Autowired
    private OutboundCfxEnvelopeBuilder builder;

    @Test
    @DisplayName("build 9120 ack envelope — P0 闭合 2101 模式 6 ack 装配段缺口（registry+dispatcher 双查通过）")
    void build9120AckEnvelope_shouldSucceedAfterT1T2Registration() {
        final OutboundMessageQueueEntity entity = givenAck9120Entity();
        final OutboundHeadFields headFields = new OutboundHeadFields(
                SEND_ORG_CODE, ENTRUST_DATE, TRANSITION_NO);

        // 单次 build — 不抛即继续走 envelope 结构断言（throw → JUnit FAIL，等价
        // assertThatCode().doesNotThrowAnyException() 语义。assertion message 通过
        // method @DisplayName 显式说明回归基准角色）
        final String envelope = builder.build(entity, headFields).envelope();

        // 断言 1: envelope 必须存在（build 成功的 P0 闭合证明 — 不抛 5107/5108）
        assertThat(envelope)
                .as("build 9120 ack 应成功（T1+T2 注册后无 OUTBOUND_5107/5108）—— "
                        + "本 IT 在 T1+T2 之前会 FAIL，作为 2101 模式 6 ack 装配段缺口闭合的回归基准")
                .isNotNull();

        // 断言 2-7: envelope 结构含关键元素（装配段产物完整性，6 项保留 0 改动）
        assertThat(envelope)
                .as("9120 ack envelope 必含 BatchHead9120（dispatcher.describeFor headElementName）")
                .contains("<BatchHead9120");
        assertThat(envelope)
                .as("9120 ack envelope 必含 MsgReturn9120 body 元素（bodyClassRegistry.resolve 反序列化 + marshal）")
                .contains("<MsgReturn9120");
        assertThat(envelope)
                .as("9120 ack envelope 必含 OriMsgNo=2101（MsgReturn9120 业务体字段穿透 marshal）")
                .contains("<OriMsgNo>2101</OriMsgNo>");
        assertThat(envelope)
                .as("9120 ack envelope CommonHead 必含 MsgNo=9120（CommonHeadComposer.compose 注入 entity.messageType）")
                .contains("<MsgNo>9120</MsgNo>");
        assertThat(envelope)
                .as("9120 ack envelope BatchHead9120 必含 Result 占位（ResponseBusinessHead requiresResultCode=true）")
                .contains("<Result>90000</Result>");
        assertThat(envelope)
                .as("9120 ack envelope 必含 CFX 外层封装（marshalToString 完整 envelope 产物）")
                .contains("</CFX>");
    }

    /**
     * DEF-Q-NEW-1 NC-1 — 负向：未注册 msgNo("0000") 应抛 {@code OUTBOUND_5101}。
     *
     * <p>{@code build} 首关 {@code MessageType.byMsgNo("0000")} 返回 {@code Optional.empty()}
     * （"0000" 不在 {@code MessageType} enum）→ {@code orElseThrow} 抛
     * {@link FepErrorCode#OUTBOUND_5101_ENVELOPE_BUILD_FAILURE}。锁住「build 拒未注册 msgNo，
     * 不静默放行」的防御能力。注意：5107/5108 仅在「合法 MessageType 但 dispatcher/registry
     * 未注册」窄窗口触发，非未知 msgNo 的稳定故障码。</p>
     */
    @Test
    @DisplayName("build 未注册 msgNo(0000) — 应抛 OUTBOUND_5101（MessageType.byMsgNo 缺失，锁 build 拒未知 msgNo）")
    void buildUnknownMsgNo_shouldThrow5101EnvelopeBuildFailure() {
        final OutboundMessageQueueEntity entity = givenEntity("0000", BODY_XML_9120_ACK);
        final OutboundHeadFields headFields = new OutboundHeadFields(
                SEND_ORG_CODE, ENTRUST_DATE, TRANSITION_NO);

        assertThatThrownBy(() -> builder.build(entity, headFields))
                .as("未注册 msgNo 应在 MessageType.byMsgNo orElseThrow 处抛 5101，不静默放行")
                .isInstanceOf(FepBusinessException.class)
                .satisfies(e -> assertThat(((FepBusinessException) e).getErrorCode())
                        .isEqualTo(FepErrorCode.OUTBOUND_5101_ENVELOPE_BUILD_FAILURE));
    }

    /**
     * DEF-Q-NEW-1 NC-2 — 负向：9120 ack 缺必填 OriMsgNo 应抛 {@code OUTBOUND_5102}。
     *
     * <p>与正向 {@code build9120AckEnvelope_shouldSucceedAfterT1T2Registration} 对称：
     * 缺必填 OriMsgNo 的 {@code MsgReturn9120} body 经 build 内宽松 JAXB unmarshal
     * （{@code JaxbContextCache} 的 Unmarshaller 未设严格 ValidationEventHandler，
     * 缺元素 ERROR 被默认吞掉，oriMsgNo=null 不抛）→ marshal 省略元素 → 真
     * {@code XsdValidator.validate} 判缺必填元素 invalid →
     * {@link FepErrorCode#OUTBOUND_5102_XSD_VALIDATION_FAILURE}。证明真 XSD 校验对
     * 非合规 body 的负向拦截能力（R-NEW-1 起真 validator 注入的负向半边覆盖）。</p>
     */
    @Test
    @DisplayName("build 9120 ack 缺必填 OriMsgNo — 应抛 OUTBOUND_5102（真 XsdValidator 负向拒绝，与正向 T5 对称）")
    void build9120AckMissingRequiredOriMsgNo_shouldThrow5102XsdValidationFailure() {
        final OutboundMessageQueueEntity entity = givenEntity("9120", BODY_XML_9120_MISSING_ORIMSGNO);
        final OutboundHeadFields headFields = new OutboundHeadFields(
                SEND_ORG_CODE, ENTRUST_DATE, TRANSITION_NO);

        assertThatThrownBy(() -> builder.build(entity, headFields))
                .as("缺必填 OriMsgNo 的 9120 body 经宽松 unmarshal → marshal 省略 → 真 XsdValidator 判缺必填 → 5102")
                .isInstanceOf(FepBusinessException.class)
                .satisfies(e -> assertThat(((FepBusinessException) e).getErrorCode())
                        .isEqualTo(FepErrorCode.OUTBOUND_5102_XSD_VALIDATION_FAILURE));
    }

    /**
     * 构造任意 msgNo + body 的 {@link OutboundMessageQueueEntity}（负向用例复用）。
     *
     * <p>{@code messageHeadXml} 在 {@code build} 内不被反序列化（{@code headFields}
     * 由 Runner 上层提供），合成占位即可（与 {@link #givenAck9120Entity()} 同策略）。</p>
     *
     * @param msgNo   报文类型号（如 "9120" / 未注册 "0000"）
     * @param bodyXml 业务体 XML（合规或缺必填元素的负向 fixture）
     * @return entity fixture
     */
    private OutboundMessageQueueEntity givenEntity(final String msgNo, final String bodyXml) {
        final OutboundMessageQueueEntity e = new OutboundMessageQueueEntity();
        e.setMessageType(msgNo);
        e.setMessageBodyXml(bodyXml);
        e.setMessageHeadXml("<OutboundHeadFields/>");
        return e;
    }

    /**
     * 构造 9120 ack {@link OutboundMessageQueueEntity}，模拟
     * {@code BizMessage2101InboundListener.enqueue9120Ack} 入队产物。
     *
     * <p>{@code messageHeadXml} 字段在 {@code build} 内不被反序列化（{@code headFields}
     * 由 Runner 上层提供），合成占位即可（与 sibling {@code OutboundCfxEnvelopeBuilderTest}
     * 一致策略）。</p>
     *
     * @return ack entity fixture
     */
    private OutboundMessageQueueEntity givenAck9120Entity() {
        return givenEntity("9120", BODY_XML_9120_ACK);
    }
}
