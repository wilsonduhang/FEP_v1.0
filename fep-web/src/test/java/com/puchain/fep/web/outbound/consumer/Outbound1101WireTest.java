package com.puchain.fep.web.outbound.consumer;

import com.puchain.fep.converter.model.RequestBusinessHead;
import com.puchain.fep.processor.body.batch.DataTransfer1101;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * P4-MSG-D T3 — 1101 outbound wire 链路 bean 集成 IT（1-element 参数化）。
 *
 * <p>验证外联机构数据报送 outbound 场景下，Spring context 内
 * {@link BodyClassRegistry} +
 * {@link com.puchain.fep.converter.wire.OutboundWireShapeDispatcher} 两个 wire
 * 链路 bean 协调一致：</p>
 *
 * <ul>
 *   <li>{@code registry.resolve("1101")} → {@link DataTransfer1101}.class（
 *       P4-MSG-D T3 注册，BodyClassRegistry 21+ entries 之一）</li>
 *   <li>{@code dispatcher.describeFor("1101")} → {@code BatchHead1101} +
 *       {@link RequestBusinessHead} + {@code requiresResultCode=false}（与 1101.xsd
 *       {@code <BatchHead1101 type="RequestHead"/>} 一致，模式 3 异步无业务回执，9120 ack）</li>
 *   <li>{@code dispatcher.isRegisteredOutboundMsgNo("1101")} → true</li>
 * </ul>
 *
 * <p>继承自 {@link AbstractOutboundWireMatrixTest}（DEF-Reuse-R1，2026-05-21）—
 * 由 1 个 {@code @Test} 升级为 1-element {@code @ParameterizedTest @MethodSource}，
 * 单 case 参数化形态与其他 sibling 一致。</p>
 *
 * <p><b>完整 e2e 流水（enqueue → consumer.poll → claim → envelope build → sign →
 * send → SENT）</b>由 Plan D closing 阶段（T5）的全 reactor verify 兜底验证。</p>
 *
 * <p>PRD 依据: §3.2 报文结构 + §4.6 报文方向（1101 受理→HNDEMP）+ §4.7 模式 3 异步
 * + §5.5 数据报送管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@DisplayName("1101 outbound wire bean 协调")
class Outbound1101WireTest extends AbstractOutboundWireMatrixTest {

    static Stream<Arguments> wireMatrix() {
        return Stream.of(
                Arguments.of("1101", DataTransfer1101.class,
                        "BatchHead1101", RequestBusinessHead.class, false)
        );
    }
}
