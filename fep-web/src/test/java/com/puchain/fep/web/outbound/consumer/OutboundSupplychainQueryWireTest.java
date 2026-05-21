package com.puchain.fep.web.outbound.consumer;

import com.puchain.fep.converter.model.RequestBusinessHead;
import com.puchain.fep.converter.model.ResponseBusinessHead;
import com.puchain.fep.processor.body.supplychain.ProgressQuery3001;
import com.puchain.fep.processor.body.supplychain.ProgressQueryReturn3002;
import com.puchain.fep.processor.body.supplychain.PzInfoQuery3003;
import com.puchain.fep.processor.body.supplychain.PzInfoReturn3004;
import com.puchain.fep.processor.body.supplychain.QyAccQuery3005;
import com.puchain.fep.processor.body.supplychain.QyAccQueryReturn3006;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * P4-MSG-F T4 — 6 供应链查询报文 outbound wire 链路 bean 集成 IT（参数化 × 6 case）。
 *
 * <p>对每个供应链查询 msgNo（3001/3002/3003/3004/3005/3006）验证 Spring context 内
 * {@link BodyClassRegistry} +
 * {@link com.puchain.fep.converter.wire.OutboundWireShapeDispatcher}
 * 两个 wire 链路 bean 协调一致：</p>
 *
 * <ul>
 *   <li>3001/3003/3005 上行请求 → RealHead{n} + RequestBusinessHead + no result</li>
 *   <li>3002/3004/3006 上行回执 → RealHead{n} + ResponseBusinessHead + result=true</li>
 *   <li>6 msgNo 全部 isRegisteredOutboundMsgNo=true</li>
 * </ul>
 *
 * <p>继承自 {@link AbstractOutboundWireMatrixTest}（DEF-Reuse-R1,2026-05-21）。</p>
 *
 * <p>PRD 依据: v1.3 §3.2 报文结构 + §4.2 实时类业务报文 + §4.6 报文方向 + §4.7 模式 1 同步。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@DisplayName("6 supplychain query outbound wire bean 协调")
class OutboundSupplychainQueryWireTest extends AbstractOutboundWireMatrixTest {

    static Stream<Arguments> wireMatrix() {
        return Stream.of(
                Arguments.of("3001", ProgressQuery3001.class,
                        "RealHead3001", RequestBusinessHead.class, false),
                Arguments.of("3002", ProgressQueryReturn3002.class,
                        "RealHead3002", ResponseBusinessHead.class, true),
                Arguments.of("3003", PzInfoQuery3003.class,
                        "RealHead3003", RequestBusinessHead.class, false),
                Arguments.of("3004", PzInfoReturn3004.class,
                        "RealHead3004", ResponseBusinessHead.class, true),
                Arguments.of("3005", QyAccQuery3005.class,
                        "RealHead3005", RequestBusinessHead.class, false),
                Arguments.of("3006", QyAccQueryReturn3006.class,
                        "RealHead3006", ResponseBusinessHead.class, true)
        );
    }
}
