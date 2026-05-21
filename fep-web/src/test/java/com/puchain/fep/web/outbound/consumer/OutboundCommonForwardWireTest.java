package com.puchain.fep.web.outbound.consumer;

import com.puchain.fep.converter.model.RequestBusinessHead;
import com.puchain.fep.converter.model.ResponseBusinessHead;
import com.puchain.fep.processor.body.common.Forward9000;
import com.puchain.fep.processor.body.common.Forward9100;
import com.puchain.fep.processor.body.common.MsgReturn9120;
import com.puchain.fep.processor.body.supplychain.HxqyCreditAmt3113;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * P4-MSG-I T4 — 4 common/supplychain batch4 报文 outbound wire 链路 bean 集成 IT（参数化 × 4 case）。
 *
 * <p>对每个 batch4 msgNo（9120/3113/9100/9000）验证 Spring context 内
 * {@link BodyClassRegistry} +
 * {@link com.puchain.fep.converter.wire.OutboundWireShapeDispatcher}
 * 两个 wire 链路 bean 协调一致：</p>
 *
 * <ul>
 *   <li>9120 通用应答（2101 模式 6 ack）→ BatchHead9120 + ResponseBusinessHead + with result
 *       （既有 BatchHead+ResponseBusinessHead+true 类目扩展，P4-MSG-I T1）</li>
 *   <li>3113 核心企业授信额度回执 → BatchHead3113 + ResponseBusinessHead + with result</li>
 *   <li>9100 非实时业务通用转发（模式 3）→ BatchHead9100 + RequestBusinessHead + no result</li>
 *   <li>9000 实时业务通用转发 → RealHead9000 + RequestBusinessHead + no result</li>
 * </ul>
 *
 * <p>继承自 {@link AbstractOutboundWireMatrixTest}（DEF-Reuse-R1，2026-05-21）。
 * 本类仅声明 {@code wireMatrix()} 数据 + 子类业务 Javadoc。</p>
 *
 * <p>PRD 依据: v1.3 §4.1.1 报文清单 + §3.2 报文结构 + §4.6 报文方向 + §4.5 通用报文。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@DisplayName("4 common/supplychain batch4 outbound wire bean 协调")
class OutboundCommonForwardWireTest extends AbstractOutboundWireMatrixTest {

    static Stream<Arguments> wireMatrix() {
        return Stream.of(
                Arguments.of("9120", MsgReturn9120.class,
                        "BatchHead9120", ResponseBusinessHead.class, true),
                Arguments.of("3113", HxqyCreditAmt3113.class,
                        "BatchHead3113", ResponseBusinessHead.class, true),
                Arguments.of("9100", Forward9100.class,
                        "BatchHead9100", RequestBusinessHead.class, false),
                Arguments.of("9000", Forward9000.class,
                        "RealHead9000", RequestBusinessHead.class, false)
        );
    }
}
