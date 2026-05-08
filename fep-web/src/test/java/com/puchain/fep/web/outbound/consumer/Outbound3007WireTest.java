package com.puchain.fep.web.outbound.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import com.puchain.fep.converter.model.RequestBusinessHead;
import com.puchain.fep.converter.wire.OutboundWireShapeDispatcher;
import com.puchain.fep.converter.wire.WireShapeDescriptor;
import com.puchain.fep.processor.body.supplychain.InvoCheckQuery3007;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * P4-MSG-B T1 — 3007 outbound wire 链路 bean 集成 IT。
 *
 * <p>验证受理单位发起发票核验请求 outbound 场景下，Spring context 内
 * {@link BodyClassRegistry} + {@link OutboundWireShapeDispatcher} 两个 wire
 * 链路 bean 协调一致：</p>
 *
 * <ul>
 *   <li>{@code registry.resolve("3007")} → {@link InvoCheckQuery3007}.class（
 *       P4-MSG-B T1 注册的第 9 entry）</li>
 *   <li>{@code dispatcher.describeFor("3007")} → {@code RealHead3007} +
 *       {@link RequestBusinessHead} + {@code requiresResultCode=false}（与 3007.xsd
 *       {@code <element name="RealHead3007" type="RequestHead"/>} 一致，模式 1 同步）</li>
 *   <li>{@code dispatcher.isRegisteredOutboundMsgNo("3007")} → true（
 *       BatchMessageProcessorService.resolveHeadElementName 走 dispatcher 路径）</li>
 * </ul>
 *
 * <p><b>完整 e2e 流水（enqueue → consumer.poll → claim → envelope build → sign →
 * send → SENT）</b>由 Plan B closing 阶段（T5）的全 reactor verify 兜底验证 —
 * 既有 {@link P5OutboundEndToEndIntegrationTest} 的 8 报文 e2e codepath 在
 * 9 上行报文集合（含 3007）下行为不变（dispatcher + registry 是 append-only
 * lookup，不引入新分支）。</p>
 *
 * <p>互补已签字 Plan {@code 2026-05-07-p4-3007-3008-inbound-wire.md}（处理 3007
 * inbound 路径 + XSD positive IT）；本 IT 仅覆盖 3007 outbound wire 注册路径，
 * 不重复 inbound + XSD 范围。</p>
 *
 * <p>PRD 依据: §4.6 line 372（3007 主动发起）+ §4.7 line 831（模式 1 同步）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@TestPropertySource(properties = {
        "fep.collector.scheduling.enabled=false",
        "management.health.redis.enabled=false"
})
class Outbound3007WireTest {

    @Autowired
    private BodyClassRegistry registry;

    @Autowired
    private OutboundWireShapeDispatcher dispatcher;

    @Test
    @DisplayName("3007 outbound wire bean 协调: registry.resolve + dispatcher.describeFor + isRegistered")
    void wire_3007_should_resolve_consistently_across_registry_and_dispatcher() {
        // BodyClassRegistry: msgNo "3007" → InvoCheckQuery3007.class（第 9 entry）
        assertThat(registry.resolve("3007"))
                .as("BodyClassRegistry.resolve(\"3007\") 必须返回 InvoCheckQuery3007.class（P4-MSG-B T1 注册）")
                .isEqualTo(InvoCheckQuery3007.class);

        // OutboundWireShapeDispatcher: msgNo "3007" → RealHead3007 + RequestBusinessHead + no result
        final WireShapeDescriptor desc = dispatcher.describeFor("3007");
        assertThat(desc.headElementName())
                .as("3007 head 元素名（与 3007.xsd 一致）")
                .isEqualTo("RealHead3007");
        assertThat(desc.headClass())
                .as("3007 head 类型（请求报文用 RequestBusinessHead，模式 1 同步）")
                .isEqualTo(RequestBusinessHead.class);
        assertThat(desc.requiresResultCode())
                .as("3007 是请求报文不带 ResultCode（仅回执 3008 走 ResponseBusinessHead）")
                .isFalse();

        // BatchMessageProcessorService.resolveHeadElementName 路径：3007 走 dispatcher
        assertThat(dispatcher.isRegisteredOutboundMsgNo("3007"))
                .as("3007 必须在 isRegisteredOutboundMsgNo true，否则 inbound 路径会走 legacy fallback")
                .isTrue();
    }
}
