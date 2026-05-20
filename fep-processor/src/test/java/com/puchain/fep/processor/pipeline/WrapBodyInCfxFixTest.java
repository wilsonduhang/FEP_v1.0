package com.puchain.fep.processor.pipeline;

import com.puchain.fep.common.util.FepConstants;
import com.puchain.fep.converter.model.CommonHead;
import com.puchain.fep.converter.wire.OutboundWireShapeDispatcher;
import com.puchain.fep.processor.body.supplychain.ContractInfo3101;
import com.puchain.fep.processor.body.supplychain.PzCheckQuery3107;
import com.puchain.fep.processor.body.supplychain.PzInfoQuery3003;
import com.puchain.fep.processor.body.supplychain.RzReturnInfo3009;
import com.puchain.fep.processor.state.InMemoryMessageProcessStore;
import com.puchain.fep.processor.state.MessageStateMachine;
import com.puchain.fep.processor.validation.AbstractXsdValidationTest;
import com.puchain.fep.processor.validation.XsdValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P5 T3 修复测试 — 验证 {@link BatchMessageProcessorService#wrapBodyInCfx} 头元素名
 * 按 {@link OutboundWireShapeDispatcher} 决定，而非历史 hardcoded {@code "RealHead" + msgNo}。
 *
 * <p>对照 16 上行报文 wire-shape（P4-MSG-A T1 起 8→16，本测试覆盖典型 3 例）：</p>
 * <ul>
 *   <li>3009 → {@code <RealHead3009>}（保留 RealHead，dispatcher 验证不破坏 1/8 正确路径）</li>
 *   <li>3101 → {@code <BatchHead3101>}（修复：不应再是 RealHead3101，dispatcher 切换为 BatchHead）</li>
 *   <li>3107 → {@code <BatchHead3107>}（修复：批量报文头）</li>
 * </ul>
 *
 * <p>同时验证未登记 outbound msgNo（如 9005 心跳类通用报文，9005.xsd MSG 下无 body
 * 元素）走 legacy 路径不回归，避免对 {@link BatchMessageProcessorServiceIntegrationTest}
 * 已有 IT 产生破坏。</p>
 *
 * <p>测试 fixture msgNo 演进史（红线 {@code feedback_obsolete_negative_test_cleanup}
 * + {@code feedback_cross_task_obsolete_fixture_assumption_when_set_extended} 候选）：</p>
 * <ul>
 *   <li>原 P5 T3：用 3003</li>
 *   <li>P4-MSG-F T2：3003 加入 dispatcher 27 集合不再 inbound-only → 升级用 9000</li>
 *   <li><b>P4-MSG-I T1：9000 加入 dispatcher 37 集合不再 inbound-only → 升级用 9005</b></li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@DisplayName("BatchMessageProcessorService.wrapBodyInCfx fix — wire-shape dispatch")
class WrapBodyInCfxFixTest {

    private BatchMessageProcessorService service;

    @BeforeEach
    void setUp() {
        XsdValidator validator = AbstractXsdValidationTest.SHARED_VALIDATOR;
        InMemoryMessageProcessStore store = new InMemoryMessageProcessStore();
        MessageStateMachine machine = new MessageStateMachine(store);
        BatchPayloadAdapter adapter = new BatchPayloadAdapter();
        OutboundWireShapeDispatcher dispatcher = new OutboundWireShapeDispatcher();
        service = new BatchMessageProcessorService(validator, machine, store, adapter, dispatcher);
    }

    @Test
    @DisplayName("3101 → wraps body with <BatchHead3101>, NOT <RealHead3101>")
    void wrapBodyInCfx_3101_should_use_BatchHead3101_not_RealHead3101() {
        CommonHead head = head("3101");
        ContractInfo3101 body = new ContractInfo3101();

        String wrapped = service.wrapBodyInCfx(head, "3101", body);

        assertThat(wrapped)
                .as("3101 must use BatchHead per XSD 3101.xsd")
                .contains("<BatchHead3101");
        assertThat(wrapped)
                .as("3101 must NOT use RealHead (P5 T3 fix)")
                .doesNotContain("<RealHead3101");
    }

    @Test
    @DisplayName("3009 → wraps body with <RealHead3009> (1/8 RealHead branch preserved)")
    void wrapBodyInCfx_3009_should_use_RealHead3009() {
        CommonHead head = head("3009");
        RzReturnInfo3009 body = new RzReturnInfo3009();

        String wrapped = service.wrapBodyInCfx(head, "3009", body);

        assertThat(wrapped)
                .as("3009 must use RealHead per XSD 3009.xsd (only RealHead in 8-set)")
                .contains("<RealHead3009");
        assertThat(wrapped)
                .as("3009 must NOT use BatchHead")
                .doesNotContain("<BatchHead3009");
    }

    @Test
    @DisplayName("3107 → wraps body with <BatchHead3107>, NOT <RealHead3107>")
    void wrapBodyInCfx_3107_should_use_BatchHead3107_not_RealHead3107() {
        CommonHead head = head("3107");
        PzCheckQuery3107 body = new PzCheckQuery3107();

        String wrapped = service.wrapBodyInCfx(head, "3107", body);

        assertThat(wrapped).contains("<BatchHead3107");
        assertThat(wrapped).doesNotContain("<RealHead3107");
    }

    @Test
    @DisplayName("Unregistered outbound msgNo (9005) preserved via legacy path — no OUTBOUND_5108 regression")
    void wrapBodyInCfx_unregisteredOutboundMsgNo_should_preserve_legacy_RealHead() {
        // 9005 是心跳类通用报文，9005.xsd MSG 下无 body 元素，不在 dispatcher 的 37 上行
        // 报文集合内。dispatcher.describeFor("9005") 会抛 OUTBOUND_5108。
        // resolveHeadElementName 必须 fall back 到 legacy "RealHead{msgNo}" 保持向后
        // 兼容（BatchMessageProcessorServiceIntegrationTest 等 IT 不回归）。
        //
        // fixture msgNo 演进史（详见 class-level Javadoc）：
        //   3003 (P5 T3) → 9000 (P4-MSG-F T2) → 9005 (P4-MSG-I T1)
        // P4-MSG-I T1 将 9000 加入 dispatcher 37 集合后不再 inbound-only，本测试升级用
        // 9005 心跳类报文保持 legacy fallback 覆盖。body class 任选已注册 JAXB 类
        // （PzInfoQuery3003 仅用于触发 marshal，不影响 head element name 拼装路径判断）。
        CommonHead head = head("9005");
        PzInfoQuery3003 body = new PzInfoQuery3003();

        String wrapped = service.wrapBodyInCfx(head, "9005", body);

        assertThat(wrapped)
                .as("unregistered outbound 9005 must use RealHead9005 via legacy fallback")
                .contains("<RealHead9005");
    }

    private static CommonHead head(final String msgNo) {
        CommonHead h = new CommonHead();
        h.setVersion("1.0");
        h.setSrcNode("10000000000001");
        h.setDesNode(FepConstants.HNDEMP_NODE_CODE);
        h.setApp("HNDEMP");
        h.setMsgNo(msgNo);
        h.setMsgId("20260423120000000001");
        h.setCorrMsgId("20260423120000000000");
        h.setWorkDate("20260423");
        return h;
    }
}
