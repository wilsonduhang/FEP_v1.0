package com.puchain.fep.web.outbound.consumer;

import com.puchain.fep.converter.model.RequestBusinessHead;
import com.puchain.fep.converter.model.RequestResponseHead;
import com.puchain.fep.converter.model.ResponseBusinessHead;
import com.puchain.fep.processor.body.supplychain.ArchiveReturnInfo3103;
import com.puchain.fep.processor.body.supplychain.Forward3020;
import com.puchain.fep.processor.body.supplychain.InvoCheckReturn3008;
import com.puchain.fep.processor.body.supplychain.PzCheckQueryReturn3108;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * P4-MSG-G T5 — 4 供应链查询 batch2 报文 outbound wire 链路 bean 集成 IT（参数化 × 4 case）。
 *
 * <p>对每个 batch2 msgNo（3008/3020/3103/3108）验证 Spring context 内
 * {@link BodyClassRegistry} +
 * {@link com.puchain.fep.converter.wire.OutboundWireShapeDispatcher}
 * 两个 wire 链路 bean 协调一致：</p>
 *
 * <ul>
 *   <li>3008 发票核验回执 → RealHead3008 + ResponseBusinessHead + result=true</li>
 *   <li>3020 供应链实时业务通用转发 → RealHead3020 + RequestResponseHead + no result
 *       （孤儿成员第 5 类目，P4-MSG-G T3 隔离断言）</li>
 *   <li>3103 企业建档信息回执 → BatchHead3103 + ResponseBusinessHead + result=true</li>
 *   <li>3108 平台凭证核对回执 → BatchHead3108 + ResponseBusinessHead + result=true</li>
 * </ul>
 *
 * <p>继承自 {@link AbstractOutboundWireMatrixTest}（DEF-Reuse-R1，2026-05-21）。</p>
 *
 * <p>PRD 依据: v1.3 §4.1.1 报文清单 + §3.2 报文结构 + §4.6 报文方向。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@DisplayName("4 supplychain query batch2 outbound wire bean 协调")
class OutboundSupplychainQueryBatch2WireTest extends AbstractOutboundWireMatrixTest {

    static Stream<Arguments> wireMatrix() {
        return Stream.of(
                Arguments.of("3008", InvoCheckReturn3008.class,
                        "RealHead3008", ResponseBusinessHead.class, true),
                Arguments.of("3020", Forward3020.class,
                        "RealHead3020", RequestResponseHead.class, false),
                Arguments.of("3103", ArchiveReturnInfo3103.class,
                        "BatchHead3103", ResponseBusinessHead.class, true),
                Arguments.of("3108", PzCheckQueryReturn3108.class,
                        "BatchHead3108", ResponseBusinessHead.class, true)
        );
    }
}
