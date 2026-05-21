package com.puchain.fep.web.outbound.consumer;

import com.puchain.fep.converter.model.RequestBusinessHead;
import com.puchain.fep.converter.model.RequestResponseHead;
import com.puchain.fep.processor.body.supplychain.Forward3120;
import com.puchain.fep.processor.body.supplychain.PlatPay3115;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * P4-MSG-H T4 — 2 供应链 batch3 报文 outbound wire 链路 bean 集成 IT（参数化 × 2 case）。
 *
 * <p>对每个 batch3 msgNo（3115/3120）验证 Spring context 内 {@link BodyClassRegistry} +
 * {@link com.puchain.fep.converter.wire.OutboundWireShapeDispatcher}
 * 两个 wire 链路 bean 协调一致：</p>
 *
 * <ul>
 *   <li>3115 资金清算信息指令及回执 → BatchHead3115 + RequestResponseHead + no result
 *       （P4-MSG-H 新第 6 类目 BatchHead+RequestResponseHead）</li>
 *   <li>3120 供应链非实时业务通用转发 → BatchHead3120 + RequestBusinessHead + no result
 *       （3120.xsd type=RequestHead，归既有第 2 类目）</li>
 * </ul>
 *
 * <p>继承自 {@link AbstractOutboundWireMatrixTest}（DEF-Reuse-R1，2026-05-21）。</p>
 *
 * <p>PRD 依据: v1.3 §4.1.1 报文清单 + §3.2 报文结构 + §4.6 报文方向 + §4.4 供应链报文。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@DisplayName("2 supplychain batch3 outbound wire bean 协调")
class OutboundSupplychainBatch3WireTest extends AbstractOutboundWireMatrixTest {

    static Stream<Arguments> wireMatrix() {
        return Stream.of(
                Arguments.of("3115", PlatPay3115.class,
                        "BatchHead3115", RequestResponseHead.class, false),
                Arguments.of("3120", Forward3120.class,
                        "BatchHead3120", RequestBusinessHead.class, false)
        );
    }
}
