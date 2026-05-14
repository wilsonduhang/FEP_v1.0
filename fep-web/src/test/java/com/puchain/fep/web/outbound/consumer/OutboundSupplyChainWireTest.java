// fep-web/src/test/java/com/puchain/fep/web/outbound/consumer/OutboundSupplyChainWireTest.java
package com.puchain.fep.web.outbound.consumer;
// NOTE: BodyClassRegistry resolved by same-package import (com.puchain.fep.web.outbound.consumer)
//       — sibling test class Outbound3000WireTest / Outbound3007WireTest precedent.

import static org.assertj.core.api.Assertions.assertThat;

import com.puchain.fep.converter.model.RequestBusinessHead;
import com.puchain.fep.converter.wire.OutboundWireShapeDispatcher;
import com.puchain.fep.converter.wire.WireShapeDescriptor;
import com.puchain.fep.processor.body.supplychain.DzpzInfo3000;
import com.puchain.fep.processor.body.supplychain.InvoCheckQuery3007;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.stream.Stream;

/**
 * 3000 + 3007 outbound wire 链路 bean 集成 IT 参数化合并版（Plan B R1 deferred 消化）。
 *
 * <p>合并自 {@code Outbound3000WireTest}（P4-MSG-B T4，2026-05-08 ship）+
 * {@code Outbound3007WireTest}（P4-MSG-B T1，2026-05-08 ship）。两者结构同质
 * （同 {@code @SpringBootTest} + 同 {@code @TestPropertySource} + 同 wire bean 断言形态），
 * 仅 msgNo / body class / RealHead 元素名不同。合并为 {@code @ParameterizedTest} × 2 case
 * 节省 1 次 Spring context boot（~10-15s）。</p>
 *
 * <p>原 P4-MSG-B T4 (3000) + T1 (3007) 验证范围保持不变：</p>
 * <ul>
 *   <li>{@code registry.resolve("3000")} → {@link DzpzInfo3000}.class /
 *       {@code registry.resolve("3007")} → {@link InvoCheckQuery3007}.class（P4-MSG-A T2
 *       起 16 entries 之一，BodyClassRegistry 注册）</li>
 *   <li>{@code dispatcher.describeFor(msgNo)} → {@code RealHead{msgNo}} +
 *       {@link RequestBusinessHead} + {@code requiresResultCode=false}（与
 *       {@code 3000.xsd} / {@code 3007.xsd} {@code <element name="RealHead{msgNo}"
 *       type="RequestHead"/>} 一致）</li>
 *   <li>{@code dispatcher.isRegisteredOutboundMsgNo(msgNo)} → true（
 *       {@code BatchMessageProcessorService.resolveHeadElementName} 走 dispatcher 路径）</li>
 * </ul>
 *
 * <p><b>完整 e2e 流水（enqueue → consumer.poll → claim → envelope build → sign →
 * send → SENT）</b>由 Plan B closing 阶段（T5）的全 reactor verify 兜底验证 —
 * 既有 {@code P5OutboundEndToEndIntegrationTest} 的 e2e codepath 在 16 上行报文集合
 * （含 3000/3007）下行为不变（dispatcher + registry 是 append-only lookup，不引入新分支）。</p>
 *
 * <p>命名后缀 {@code WireTest}（P4-MSG-A Q2 dead-test rename 后改为 {@code Test} 后缀
 * 以纳入 surefire 默认 include {@code *Test.java}）。</p>
 *
 * <p>PRD 依据: §4.6 (3000/3007 主动发起，line 372 for 3007) + §4.7 (3000 模式 3 异步无回执 / 3007 模式 1 同步 line 831)。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@TestPropertySource(properties = {
        "fep.collector.scheduling.enabled=false",
        "management.health.redis.enabled=false"
})
class OutboundSupplyChainWireTest {

    @Autowired
    private BodyClassRegistry registry;

    @Autowired
    private OutboundWireShapeDispatcher dispatcher;

    static Stream<Arguments> supplyChainOutboundCases() {
        return Stream.of(
                Arguments.of("3000", DzpzInfo3000.class, "RealHead3000"),
                Arguments.of("3007", InvoCheckQuery3007.class, "RealHead3007"));
    }

    @ParameterizedTest(name = "[{index}] msgNo={0} -> {1} wire bean coordination")
    @MethodSource("supplyChainOutboundCases")
    @org.junit.jupiter.api.DisplayName("2 供应链报文 outbound wire bean 协调 (3000 + 3007)")
    void wireBeansShouldCoordinate(String msgNo, Class<?> expectedBodyClass, String expectedRealHead) {
        assertThat(registry.resolve(msgNo)).isEqualTo(expectedBodyClass);

        WireShapeDescriptor descriptor = dispatcher.describeFor(msgNo);
        assertThat(descriptor.headElementName()).isEqualTo(expectedRealHead);
        assertThat(descriptor.headClass()).isEqualTo(RequestBusinessHead.class);
        assertThat(descriptor.requiresResultCode()).isFalse();

        assertThat(dispatcher.isRegisteredOutboundMsgNo(msgNo)).isTrue();
    }
}
