package com.puchain.fep.web.outbound.consumer;

import com.puchain.fep.converter.model.RequestBusinessHead;
import com.puchain.fep.converter.model.ResponseBusinessHead;
import com.puchain.fep.processor.body.batch.CompanyAuthFileBatchResponse2104;
import com.puchain.fep.processor.body.batch.CompanyAuthFileBatchTransfer1104;
import com.puchain.fep.processor.body.batch.CompanyInfoBatchRequest1103;
import com.puchain.fep.processor.body.batch.CompanyInfoBatchResponse2103;
import com.puchain.fep.processor.body.batch.DataTransferCheckBatchRequest1102;
import com.puchain.fep.processor.body.batch.DataTransferCheckBatchResponse2102;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * P4-MSG-A T3 — 6 BATCH 报文 outbound wire 链路 bean 集成 IT（参数化 × 6 case）。
 *
 * <p>对每个 BATCH msgNo（1102/1103/1104/2102/2103/2104）验证 Spring context 内
 * {@link BodyClassRegistry} +
 * {@link com.puchain.fep.converter.wire.OutboundWireShapeDispatcher}
 * 两个 wire 链路 bean 协调一致：</p>
 *
 * <ul>
 *   <li>1102/1103/1104 上行请求 → BatchHead{n} + RequestBusinessHead + no result</li>
 *   <li>2102/2103/2104 上行回执 → BatchHead{n} + ResponseBusinessHead + result=true</li>
 *   <li>6 BATCH 全部 isRegisteredOutboundMsgNo=true</li>
 * </ul>
 *
 * <p>继承自 {@link AbstractOutboundWireMatrixTest}（DEF-Reuse-R1，2026-05-21）—
 * 4 块共享断言 + Spring/JUnit 集成配置在父类。本类仅声明 {@code wireMatrix()}
 * 数据源 + 子类业务 Javadoc。</p>
 *
 * <p>本 IT 仅覆盖 BATCH wire 注册路径，不重复 Body POJO 的 JAXB roundtrip / XSD validation
 * 测试（已 ship 于 BATCH-1102-2102-1104-2104 / BATCH-1103 阶段，
 * fep-processor/src/test/java/com/puchain/fep/processor/body/batch/*Test.java 6 文件）。</p>
 *
 * <p>PRD 依据: v1.3 §4.6 报文方向 + §4.7 处理模式（非实时）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@DisplayName("6 BATCH outbound wire bean 协调")
class OutboundBatchWireTest extends AbstractOutboundWireMatrixTest {

    static Stream<Arguments> wireMatrix() {
        return Stream.of(
                Arguments.of("1102", DataTransferCheckBatchRequest1102.class,
                        "BatchHead1102", RequestBusinessHead.class, false),
                Arguments.of("1103", CompanyInfoBatchRequest1103.class,
                        "BatchHead1103", RequestBusinessHead.class, false),
                Arguments.of("1104", CompanyAuthFileBatchTransfer1104.class,
                        "BatchHead1104", RequestBusinessHead.class, false),
                Arguments.of("2102", DataTransferCheckBatchResponse2102.class,
                        "BatchHead2102", ResponseBusinessHead.class, true),
                Arguments.of("2103", CompanyInfoBatchResponse2103.class,
                        "BatchHead2103", ResponseBusinessHead.class, true),
                Arguments.of("2104", CompanyAuthFileBatchResponse2104.class,
                        "BatchHead2104", ResponseBusinessHead.class, true)
        );
    }
}
