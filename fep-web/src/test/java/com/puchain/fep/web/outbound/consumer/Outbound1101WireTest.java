package com.puchain.fep.web.outbound.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import com.puchain.fep.converter.model.RequestBusinessHead;
import com.puchain.fep.converter.wire.OutboundWireShapeDispatcher;
import com.puchain.fep.converter.wire.WireShapeDescriptor;
import com.puchain.fep.processor.body.batch.DataTransfer1101;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * P4-MSG-D T3 — 1101 outbound wire 链路 bean 集成 IT。
 *
 * <p>验证外联机构数据报送 outbound 场景下，Spring context 内
 * {@link BodyClassRegistry} + {@link OutboundWireShapeDispatcher} 两个 wire
 * 链路 bean 协调一致：</p>
 *
 * <ul>
 *   <li>{@code registry.resolve("1101")} → {@link DataTransfer1101}.class（
 *       P4-MSG-D T3 注册，按 msgNo 升序排在首位 17 entries）</li>
 *   <li>{@code dispatcher.describeFor("1101")} → {@code BatchHead1101} +
 *       {@link RequestBusinessHead} + {@code requiresResultCode=false}（与 1101.xsd
 *       {@code <BatchHead1101 type="RequestHead"/>} 一致，模式 3 异步无业务回执，9120 ack）</li>
 *   <li>{@code dispatcher.isRegisteredOutboundMsgNo("1101")} → true（
 *       BatchMessageProcessorService.resolveHeadElementName 走 dispatcher 路径）</li>
 * </ul>
 *
 * <p><b>完整 e2e 流水（enqueue → consumer.poll → claim → envelope build → sign →
 * send → SENT）</b>由 Plan D closing 阶段（T5）的全 reactor verify 兜底验证 —
 * 既有 {@code P5OutboundEndToEndIntegrationTest} 的 e2e codepath 在 17 上行报文集合
 * （含 1101）下行为不变（dispatcher + registry 是 append-only lookup，不引入新分支）。</p>
 *
 * <p>命名沿用 sibling {@code Outbound3000WireTest} / {@code Outbound3007WireTest}
 * （{@code WireTest} 后缀以纳入 surefire 默认 include {@code *Test.java}，红线
 * P2b-DEFECT-002 / Q2 dead-test rename 教训）。</p>
 *
 * <p>PRD 依据: §3.2 报文结构 + §4.6 报文方向（1101 受理→HNDEMP）+ §4.7 模式 3 异步
 * + §5.5 数据报送管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@TestPropertySource(properties = {
        "fep.collector.scheduling.enabled=false",
        "management.health.redis.enabled=false"
})
class Outbound1101WireTest {

    @Autowired
    private BodyClassRegistry registry;

    @Autowired
    private OutboundWireShapeDispatcher dispatcher;

    @Test
    @DisplayName("1101 outbound wire bean 协调: registry.resolve + dispatcher.describeFor + isRegistered")
    void wire_1101_should_resolve_consistently_across_registry_and_dispatcher() {
        // BodyClassRegistry: msgNo "1101" → DataTransfer1101.class（17 entries 之一）
        assertThat(registry.resolve("1101"))
                .as("BodyClassRegistry.resolve(\"1101\") 必须返回 DataTransfer1101.class（P4-MSG-D T3 注册）")
                .isEqualTo(DataTransfer1101.class);

        // OutboundWireShapeDispatcher: msgNo "1101" → BatchHead1101 + RequestBusinessHead + no result
        final WireShapeDescriptor desc = dispatcher.describeFor("1101");
        assertThat(desc.headElementName())
                .as("1101 head 元素名（与 1101.xsd 一致）")
                .isEqualTo("BatchHead1101");
        assertThat(desc.headClass())
                .as("1101 head 类型（请求报文用 RequestBusinessHead，模式 3 异步 9120 ack）")
                .isEqualTo(RequestBusinessHead.class);
        assertThat(desc.requiresResultCode())
                .as("1101 是请求报文不带 ResultCode（异步无业务回执路径）")
                .isFalse();

        // BatchMessageProcessorService.resolveHeadElementName 路径：1101 走 dispatcher
        assertThat(dispatcher.isRegisteredOutboundMsgNo("1101"))
                .as("1101 必须在 isRegisteredOutboundMsgNo true，否则 inbound 路径会走 legacy fallback")
                .isTrue();
    }
}
