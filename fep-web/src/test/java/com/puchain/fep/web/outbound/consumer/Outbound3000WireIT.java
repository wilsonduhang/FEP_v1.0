package com.puchain.fep.web.outbound.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import com.puchain.fep.converter.model.RequestBusinessHead;
import com.puchain.fep.converter.wire.OutboundWireShapeDispatcher;
import com.puchain.fep.converter.wire.WireShapeDescriptor;
import com.puchain.fep.processor.body.supplychain.DzpzInfo3000;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * P4-MSG-B T4 — 3000 outbound wire 链路 bean 集成 IT。
 *
 * <p>验证电子凭证信息报送 outbound 场景下，Spring context 内
 * {@link BodyClassRegistry} + {@link OutboundWireShapeDispatcher} 两个 wire
 * 链路 bean 协调一致：</p>
 *
 * <ul>
 *   <li>{@code registry.resolve("3000")} → {@link DzpzInfo3000}.class（
 *       P4-MSG-B T4 注册的第 10 entry，按 msgNo 升序排在首位）</li>
 *   <li>{@code dispatcher.describeFor("3000")} → {@code RealHead3000} +
 *       {@link RequestBusinessHead} + {@code requiresResultCode=false}（与 3000.xsd
 *       {@code <element name="RealHead3000" type="RequestHead"/>} 一致，模式 3 异步）</li>
 *   <li>{@code dispatcher.isRegisteredOutboundMsgNo("3000")} → true（
 *       BatchMessageProcessorService.resolveHeadElementName 走 dispatcher 路径）</li>
 * </ul>
 *
 * <p><b>完整 e2e 流水（enqueue → consumer.poll → claim → envelope build → sign →
 * send → SENT）</b>由 Plan B closing 阶段（T5）的全 reactor verify 兜底验证 —
 * 既有 {@code P5OutboundEndToEndIntegrationTest} 的 e2e codepath 在 10 上行报文集合
 * （含 3000）下行为不变（dispatcher + registry 是 append-only lookup，不引入新分支）。</p>
 *
 * <p>命名沿用 sibling {@code Outbound3007WireIT}（Plan B v0.4 T4 约定）；与 T1 已 ship
 * 的 {@code Outbound3007WireIT} 一致以 {@code WireIT} 后缀命名。<b>注</b>：surefire 默认
 * 仅 include {@code *Test.java}，{@code *IT.java} 当前会被静默跳过（red line
 * {@code DEFECT-002}）；该问题源于 T1 命名选择，T4 不引入新偏差。后续若启用 failsafe
 * 或重命名为 {@code *Test.java}，本 IT 会自动被纳入执行流。</p>
 *
 * <p>PRD 依据: §3.2 报文结构 + §4.6 报文方向（3000 主动上报）+ §4.7 模式 3 异步无回执。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@TestPropertySource(properties = {
        "fep.collector.scheduling.enabled=false",
        "management.health.redis.enabled=false"
})
class Outbound3000WireIT {

    @Autowired
    private BodyClassRegistry registry;

    @Autowired
    private OutboundWireShapeDispatcher dispatcher;

    @Test
    @DisplayName("3000 outbound wire bean 协调: registry.resolve + dispatcher.describeFor + isRegistered")
    void wire_3000_should_resolve_consistently_across_registry_and_dispatcher() {
        // BodyClassRegistry: msgNo "3000" → DzpzInfo3000.class（第 10 entry）
        assertThat(registry.resolve("3000"))
                .as("BodyClassRegistry.resolve(\"3000\") 必须返回 DzpzInfo3000.class（P4-MSG-B T4 注册）")
                .isEqualTo(DzpzInfo3000.class);

        // OutboundWireShapeDispatcher: msgNo "3000" → RealHead3000 + RequestBusinessHead + no result
        final WireShapeDescriptor desc = dispatcher.describeFor("3000");
        assertThat(desc.headElementName())
                .as("3000 head 元素名（与 3000.xsd 一致）")
                .isEqualTo("RealHead3000");
        assertThat(desc.headClass())
                .as("3000 head 类型（请求报文用 RequestBusinessHead，模式 3 异步无回执）")
                .isEqualTo(RequestBusinessHead.class);
        assertThat(desc.requiresResultCode())
                .as("3000 是请求报文不带 ResultCode（异步无回执路径）")
                .isFalse();

        // BatchMessageProcessorService.resolveHeadElementName 路径：3000 走 dispatcher
        assertThat(dispatcher.isRegisteredOutboundMsgNo("3000"))
                .as("3000 必须在 isRegisteredOutboundMsgNo true，否则 inbound 路径会走 legacy fallback")
                .isTrue();
    }
}
