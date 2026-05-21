package com.puchain.fep.web.outbound.consumer;

import com.puchain.fep.converter.model.RequestBusinessHead;
import com.puchain.fep.converter.model.ResponseBusinessHead;
import com.puchain.fep.processor.body.realtime.CompanyAuthFileResponse2004;
import com.puchain.fep.processor.body.realtime.CompanyAuthFileTransfer1004;
import com.puchain.fep.processor.body.realtime.CompanyInfoRequest1001;
import com.puchain.fep.processor.body.realtime.CompanyInfoResponse2001;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * P4-MSG-E T4 — 4 企业查询实时报文 outbound wire 链路 bean 集成 IT（参数化 × 4 case）。
 *
 * <p>对每个企业查询实时 msgNo（1001/2001/1004/2004）验证 Spring context 内
 * {@link BodyClassRegistry} +
 * {@link com.puchain.fep.converter.wire.OutboundWireShapeDispatcher}
 * 两个 wire 链路 bean 协调一致：</p>
 *
 * <ul>
 *   <li>1001/1004 上行请求 → RealHead{n} + RequestBusinessHead + no result</li>
 *   <li>2001/2004 上行回执 → RealHead{n} + ResponseBusinessHead + result=true</li>
 *   <li>4 个 msgNo 全部 isRegisteredOutboundMsgNo=true</li>
 * </ul>
 *
 * <p>继承自 {@link AbstractOutboundWireMatrixTest}（DEF-Reuse-R1，2026-05-21）—
 * 同时 ParameterizedTest name template 由 {@code ->} 统一为 {@code →}（与其他 5 sibling
 * 一致，纯字符变动不影响语义）。</p>
 *
 * <p>PRD 依据: v1.3 §3.2 报文结构 + §4.2 实时类业务报文 + §4.6 报文方向 + §4.7 模式 1 同步。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@DisplayName("4 企业查询实时报文 outbound wire bean 协调")
class OutboundEnterpriseQueryRealtimeWireTest extends AbstractOutboundWireMatrixTest {

    static Stream<Arguments> wireMatrix() {
        return Stream.of(
                Arguments.of("1001", CompanyInfoRequest1001.class,
                        "RealHead1001", RequestBusinessHead.class, false),
                Arguments.of("1004", CompanyAuthFileTransfer1004.class,
                        "RealHead1004", RequestBusinessHead.class, false),
                Arguments.of("2001", CompanyInfoResponse2001.class,
                        "RealHead2001", ResponseBusinessHead.class, true),
                Arguments.of("2004", CompanyAuthFileResponse2004.class,
                        "RealHead2004", ResponseBusinessHead.class, true)
        );
    }
}
