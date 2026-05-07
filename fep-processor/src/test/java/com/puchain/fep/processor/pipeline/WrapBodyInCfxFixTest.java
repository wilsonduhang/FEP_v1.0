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
import com.puchain.fep.processor.validation.XsdSchemaRegistry;
import com.puchain.fep.processor.validation.XsdValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P5 T3 修复测试 — 验证 {@link BatchMessageProcessorService#wrapBodyInCfx} 头元素名
 * 按 {@link OutboundWireShapeDispatcher} 决定，而非历史 hardcoded {@code "RealHead" + msgNo}。
 *
 * <p>对照 8 上行报文 wire-shape：</p>
 * <ul>
 *   <li>3009 → {@code <RealHead3009>}（保留 RealHead，dispatcher 验证不破坏 1/8 正确路径）</li>
 *   <li>3101 → {@code <BatchHead3101>}（修复：不应再是 RealHead3101，dispatcher 切换为 BatchHead）</li>
 *   <li>3107 → {@code <BatchHead3107>}（修复：批量报文头）</li>
 * </ul>
 *
 * <p>同时验证 inbound-only msgNo（如 3003）走 legacy 路径不回归，避免对
 * {@link BatchMessageProcessorServiceIntegrationTest} 已有 IT 产生破坏。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@DisplayName("BatchMessageProcessorService.wrapBodyInCfx fix — wire-shape dispatch")
class WrapBodyInCfxFixTest {

    private BatchMessageProcessorService service;

    @BeforeEach
    void setUp() {
        XsdSchemaRegistry registry = new XsdSchemaRegistry();
        XsdValidator validator = new XsdValidator(registry);
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
    @DisplayName("Inbound-only msgNo (3003) preserved via legacy path — no OUTBOUND_5108 regression")
    void wrapBodyInCfx_inboundOnlyMsgNo_should_preserve_legacy_RealHead() {
        // 3003 not in 8 outbound set; dispatcher.describeFor("3003") would throw
        // OUTBOUND_5108. The fix must fall back to legacy "RealHead{msgNo}" for
        // inbound-only msgNos to keep BatchMessageProcessorServiceIntegrationTest green.
        CommonHead head = head("3003");
        PzInfoQuery3003 body = new PzInfoQuery3003();

        String wrapped = service.wrapBodyInCfx(head, "3003", body);

        assertThat(wrapped)
                .as("inbound-only 3003 must keep RealHead3003 via legacy fallback")
                .contains("<RealHead3003");
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
