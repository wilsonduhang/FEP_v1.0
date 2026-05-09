package com.puchain.fep.web.outbound.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import com.puchain.fep.converter.model.RequestBusinessHead;
import com.puchain.fep.converter.model.ResponseBusinessHead;
import com.puchain.fep.converter.wire.OutboundWireShapeDispatcher;
import com.puchain.fep.converter.wire.WireShapeDescriptor;
import com.puchain.fep.processor.body.batch.CompanyAuthFileBatchResponse2104;
import com.puchain.fep.processor.body.batch.CompanyAuthFileBatchTransfer1104;
import com.puchain.fep.processor.body.batch.CompanyInfoBatchRequest1103;
import com.puchain.fep.processor.body.batch.CompanyInfoBatchResponse2103;
import com.puchain.fep.processor.body.batch.DataTransferCheckBatchRequest1102;
import com.puchain.fep.processor.body.batch.DataTransferCheckBatchResponse2102;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.stream.Stream;

/**
 * P4-MSG-A T3 — 6 BATCH 报文 outbound wire 链路 bean 集成 IT（参数化 × 6 case）。
 *
 * <p>对每个 BATCH msgNo（1102/1103/1104/2102/2103/2104）验证 Spring context 内
 * {@link BodyClassRegistry} + {@link OutboundWireShapeDispatcher} 两个 wire 链路 bean
 * 协调一致：</p>
 *
 * <ul>
 *   <li>1102/1103/1104 上行请求 → BatchHead{n} + RequestBusinessHead + no result</li>
 *   <li>2102/2103/2104 上行回执 → BatchHead{n} + ResponseBusinessHead + result=true</li>
 *   <li>6 BATCH 全部 isRegisteredOutboundMsgNo=true</li>
 * </ul>
 *
 * <p>与既有 {@code Outbound3000WireTest} / {@code Outbound3007WireTest}（Plan B T1/T4）
 * 共享 {@code @TestPropertySource} 配置（{@code fep.collector.scheduling.enabled=false} +
 * {@code management.health.redis.enabled=false}）以触发 Spring context 缓存复用。</p>
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
@TestPropertySource(properties = {
        "fep.collector.scheduling.enabled=false",
        "management.health.redis.enabled=false"
})
class OutboundBatchWireTest {

    @Autowired
    private BodyClassRegistry registry;

    @Autowired
    private OutboundWireShapeDispatcher dispatcher;

    static Stream<Arguments> batchWireMatrix() {
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

    @ParameterizedTest(name = "[{index}] msgNo={0} → body={1}, head={2}({3}), result={4}")
    @MethodSource("batchWireMatrix")
    @DisplayName("6 BATCH outbound wire bean 协调")
    void wire_batch_should_resolve_consistently(
            final String msgNo,
            final Class<?> expectedBodyClass,
            final String expectedHeadElementName,
            final Class<? extends RequestBusinessHead> expectedHeadClass,
            final boolean expectedRequiresResultCode) {

        // BodyClassRegistry: msgNo → BATCH POJO Class
        assertThat(registry.resolve(msgNo))
                .as("BodyClassRegistry.resolve(\"%s\") 必须返回 %s（P4-MSG-A T2 注册）",
                        msgNo, expectedBodyClass.getSimpleName())
                .isEqualTo(expectedBodyClass);

        // OutboundWireShapeDispatcher: msgNo → wire-shape 描述符
        final WireShapeDescriptor desc = dispatcher.describeFor(msgNo);
        assertThat(desc.headElementName())
                .as("msgNo=%s headElementName（与 %s.xsd 一致）", msgNo, msgNo)
                .isEqualTo(expectedHeadElementName);
        assertThat(desc.headClass())
                .as("msgNo=%s headClass（%s 上行%s）", msgNo,
                        msgNo.startsWith("1") ? "1xxx" : "2xxx",
                        msgNo.startsWith("1") ? "请求" : "回执")
                .isEqualTo(expectedHeadClass);
        assertThat(desc.requiresResultCode())
                .as("msgNo=%s requiresResultCode（%s）", msgNo,
                        expectedRequiresResultCode ? "2xxx 回执含 ResultCode" : "1xxx 请求不带 ResultCode")
                .isEqualTo(expectedRequiresResultCode);

        // BatchMessageProcessorService.resolveHeadElementName 路径：BATCH 走 dispatcher
        assertThat(dispatcher.isRegisteredOutboundMsgNo(msgNo))
                .as("msgNo=%s 必须在 isRegisteredOutboundMsgNo true，否则 inbound 路径会走 legacy fallback",
                        msgNo)
                .isTrue();
    }
}
