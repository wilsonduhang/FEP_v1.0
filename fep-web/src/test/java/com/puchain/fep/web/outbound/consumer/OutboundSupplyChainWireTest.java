package com.puchain.fep.web.outbound.consumer;

import com.puchain.fep.converter.model.RequestBusinessHead;
import com.puchain.fep.processor.body.supplychain.DzpzInfo3000;
import com.puchain.fep.processor.body.supplychain.InvoCheckQuery3007;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * P4-MSG-B 合并版 — 3000 + 3007 outbound wire 链路 bean 集成 IT（参数化 × 2 case）。
 *
 * <p>合并自 {@code Outbound3000WireTest}（P4-MSG-B T4，2026-05-08 ship）+
 * {@code Outbound3007WireTest}（P4-MSG-B T1，2026-05-08 ship）— Plan B R1
 * deferred 消化合并节省 1 次 Spring context boot。</p>
 *
 * <ul>
 *   <li>{@code registry.resolve("3000")} → {@link DzpzInfo3000}.class /
 *       {@code registry.resolve("3007")} → {@link InvoCheckQuery3007}.class</li>
 *   <li>{@code dispatcher.describeFor(msgNo)} → {@code RealHead{msgNo}} +
 *       {@link RequestBusinessHead} + {@code requiresResultCode=false}（与
 *       3000.xsd / 3007.xsd {@code <element name="RealHead{msgNo}" type="RequestHead"/>} 一致）</li>
 *   <li>{@code dispatcher.isRegisteredOutboundMsgNo(msgNo)} → true</li>
 * </ul>
 *
 * <p>继承自 {@link AbstractOutboundWireMatrixTest}（DEF-Reuse-R1，2026-05-21）—
 * Arguments 由原 3-tuple {@code (msgNo, Class, RealHead)} 扩展为 5-tuple
 * {@code (msgNo, Class, RealHead, RequestBusinessHead.class, false)}；
 * 方法名 {@code supplyChainOutboundCases} → {@code wireMatrix} 对齐父类 @MethodSource；
 * template {@code ->} 统一为 {@code →}（与其他 7 sibling 一致）。</p>
 *
 * <p>PRD 依据: §4.6 (3000/3007 主动发起，line 372 for 3007) + §4.7 (3000 模式 3 异步无回执 / 3007 模式 1 同步 line 831)。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@DisplayName("2 供应链报文 outbound wire bean 协调 (3000 + 3007)")
class OutboundSupplyChainWireTest extends AbstractOutboundWireMatrixTest {

    static Stream<Arguments> wireMatrix() {
        return Stream.of(
                Arguments.of("3000", DzpzInfo3000.class,
                        "RealHead3000", RequestBusinessHead.class, false),
                Arguments.of("3007", InvoCheckQuery3007.class,
                        "RealHead3007", RequestBusinessHead.class, false)
        );
    }
}
