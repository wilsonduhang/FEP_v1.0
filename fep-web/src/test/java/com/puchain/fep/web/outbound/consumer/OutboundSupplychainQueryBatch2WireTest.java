package com.puchain.fep.web.outbound.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import com.puchain.fep.converter.model.RequestBusinessHead;
import com.puchain.fep.converter.model.RequestResponseHead;
import com.puchain.fep.converter.model.ResponseBusinessHead;
import com.puchain.fep.converter.wire.OutboundWireShapeDispatcher;
import com.puchain.fep.converter.wire.WireShapeDescriptor;
import com.puchain.fep.processor.body.supplychain.ArchiveReturnInfo3103;
import com.puchain.fep.processor.body.supplychain.Forward3020;
import com.puchain.fep.processor.body.supplychain.InvoCheckReturn3008;
import com.puchain.fep.processor.body.supplychain.PzCheckQueryReturn3108;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.stream.Stream;

/**
 * P4-MSG-G T5 — 4 供应链查询 batch2 报文 outbound wire 链路 bean 集成 IT（参数化 × 4 case）。
 *
 * <p>对每个 batch2 msgNo（3008/3020/3103/3108）验证 Spring context 内
 * {@link BodyClassRegistry} + {@link OutboundWireShapeDispatcher} 两个 wire 链路 bean
 * 协调一致：</p>
 *
 * <ul>
 *   <li>3008 发票核验回执 → RealHead3008 + ResponseBusinessHead + result=true</li>
 *   <li>3020 供应链实时业务通用转发 → RealHead3020 + RequestResponseHead + no result
 *       （孤儿成员第 5 类目，P4-MSG-G T3 隔离断言）</li>
 *   <li>3103 企业建档信息回执 → BatchHead3103 + ResponseBusinessHead + result=true</li>
 *   <li>3108 平台凭证核对回执 → BatchHead3108 + ResponseBusinessHead + result=true</li>
 * </ul>
 *
 * <p>与 {@link OutboundSupplychainQueryWireTest}（P4-MSG-F batch1 3001-3006）互补：本测覆盖
 * batch2 3008/3020/3103/3108，pattern 完全对齐 — 仅断言 registry+dispatcher bean 协调，
 * 不调用 marshal / {@code XsdValidator.validate()}（XSD validate 由 P4-MSG-G T4
 * {@code *XsdValidationTest} 内存路径完整覆盖，T5 仅协调）。</p>
 *
 * <p>共享 {@code @TestPropertySource} 配置（{@code fep.collector.scheduling.enabled=false} +
 * {@code management.health.redis.enabled=false}）以触发 Spring context 缓存复用，与
 * {@link OutboundSupplychainQueryWireTest} 同 context（无 {@code @MockBean}，保留缓存复用）。</p>
 *
 * <p>PRD 依据: v1.3 §4.1.1 报文清单 + §3.2 报文结构 + §4.6 报文方向。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@TestPropertySource(properties = {
        "fep.collector.scheduling.enabled=false",
        "management.health.redis.enabled=false"
})
class OutboundSupplychainQueryBatch2WireTest {

    @Autowired
    private BodyClassRegistry registry;

    @Autowired
    private OutboundWireShapeDispatcher dispatcher;

    static Stream<Arguments> supplychainQueryBatch2WireMatrix() {
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

    @ParameterizedTest(name = "[{index}] msgNo={0} → body={1}, head={2}({3}), result={4}")
    @MethodSource("supplychainQueryBatch2WireMatrix")
    @DisplayName("4 supplychain query batch2 outbound wire bean 协调")
    void wire_supplychainQueryBatch2_should_resolve_consistently(
            final String msgNo,
            final Class<?> expectedBodyClass,
            final String expectedHeadElementName,
            final Class<? extends RequestBusinessHead> expectedHeadClass,
            final boolean expectedRequiresResultCode) {

        // BodyClassRegistry: msgNo → SupplyChain query batch2 POJO Class
        assertThat(registry.resolve(msgNo))
                .as("BodyClassRegistry.resolve(\"%s\") 必须返回 %s（P4-MSG-G T2 注册）",
                        msgNo, expectedBodyClass.getSimpleName())
                .isEqualTo(expectedBodyClass);

        // OutboundWireShapeDispatcher: msgNo → wire-shape 描述符
        final WireShapeDescriptor desc = dispatcher.describeFor(msgNo);
        assertThat(desc.headElementName())
                .as("msgNo=%s headElementName（与 %s.xsd 一致）", msgNo, msgNo)
                .isEqualTo(expectedHeadElementName);
        assertThat(desc.headClass())
                .as("msgNo=%s headClass（%s）", msgNo,
                        "3020".equals(msgNo) ? "孤儿成员第 5 类目 RequestResponseHead" : "回执 ResponseBusinessHead")
                .isEqualTo(expectedHeadClass);
        assertThat(desc.requiresResultCode())
                .as("msgNo=%s requiresResultCode（%s）", msgNo,
                        expectedRequiresResultCode ? "回执含 ReturnCode" : "3020 转发不带 ReturnCode")
                .isEqualTo(expectedRequiresResultCode);

        // BatchMessageProcessorService.resolveHeadElementName 路径
        assertThat(dispatcher.isRegisteredOutboundMsgNo(msgNo))
                .as("msgNo=%s 必须在 isRegisteredOutboundMsgNo true，否则 inbound 路径会走 legacy fallback",
                        msgNo)
                .isTrue();
    }
}
