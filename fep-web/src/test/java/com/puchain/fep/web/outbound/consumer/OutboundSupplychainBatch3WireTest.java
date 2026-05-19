package com.puchain.fep.web.outbound.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import com.puchain.fep.converter.model.RequestBusinessHead;
import com.puchain.fep.converter.model.RequestResponseHead;
import com.puchain.fep.converter.wire.OutboundWireShapeDispatcher;
import com.puchain.fep.converter.wire.WireShapeDescriptor;
import com.puchain.fep.processor.body.supplychain.Forward3120;
import com.puchain.fep.processor.body.supplychain.PlatPay3115;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.stream.Stream;

/**
 * P4-MSG-H T4 — 2 供应链 batch3 报文 outbound wire 链路 bean 集成 IT（参数化 × 2 case）。
 *
 * <p>对每个 batch3 msgNo（3115/3120）验证 Spring context 内 {@link BodyClassRegistry} +
 * {@link OutboundWireShapeDispatcher} 两个 wire 链路 bean 协调一致：</p>
 *
 * <ul>
 *   <li>3115 资金清算信息指令及回执 → BatchHead3115 + RequestResponseHead + no result
 *       （P4-MSG-H 新第 6 类目 BatchHead+RequestResponseHead；与第 5 类目 3020 同
 *       RequestResponseHead 类型但 head 前缀 BatchHead 而非 RealHead）</li>
 *   <li>3120 供应链非实时业务通用转发 → BatchHead3120 + RequestBusinessHead + no result
 *       （3120.xsd type=RequestHead，归既有第 2 类目）</li>
 * </ul>
 *
 * <p>与 {@link OutboundSupplychainQueryBatch2WireTest}（P4-MSG-G batch2）互补，pattern 完全对齐 —
 * 仅断言 registry+dispatcher bean 协调，不调用 marshal / {@code XsdValidator.validate()}
 * （XSD validate 由 {@code SupplyChainExtXsdValidationTest} 3115/3120 @ValueSource 完整覆盖）。</p>
 *
 * <p>共享 {@code @TestPropertySource} 配置以触发 Spring context 缓存复用，与
 * {@link OutboundSupplychainQueryBatch2WireTest} 同 context（无 {@code @MockBean}，保留缓存复用）。</p>
 *
 * <p>PRD 依据: v1.3 §4.1.1 报文清单 + §3.2 报文结构 + §4.6 报文方向 + §4.4 供应链报文。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@TestPropertySource(properties = {
        "fep.collector.scheduling.enabled=false",
        "management.health.redis.enabled=false"
})
class OutboundSupplychainBatch3WireTest {

    @Autowired
    private BodyClassRegistry registry;

    @Autowired
    private OutboundWireShapeDispatcher dispatcher;

    static Stream<Arguments> supplychainBatch3WireMatrix() {
        return Stream.of(
                Arguments.of("3115", PlatPay3115.class,
                        "BatchHead3115", RequestResponseHead.class, false),
                Arguments.of("3120", Forward3120.class,
                        "BatchHead3120", RequestBusinessHead.class, false)
        );
    }

    @ParameterizedTest(name = "[{index}] msgNo={0} → body={1}, head={2}({3}), result={4}")
    @MethodSource("supplychainBatch3WireMatrix")
    @DisplayName("2 supplychain batch3 outbound wire bean 协调")
    void wire_supplychainBatch3_should_resolve_consistently(
            final String msgNo,
            final Class<?> expectedBodyClass,
            final String expectedHeadElementName,
            final Class<? extends RequestBusinessHead> expectedHeadClass,
            final boolean expectedRequiresResultCode) {

        // BodyClassRegistry: msgNo → SupplyChain batch3 POJO Class
        assertThat(registry.resolve(msgNo))
                .as("BodyClassRegistry.resolve(\"%s\") 必须返回 %s（P4-MSG-H T2 注册）",
                        msgNo, expectedBodyClass.getSimpleName())
                .isEqualTo(expectedBodyClass);

        // OutboundWireShapeDispatcher: msgNo → wire-shape 描述符
        final WireShapeDescriptor desc = dispatcher.describeFor(msgNo);
        assertThat(desc.headElementName())
                .as("msgNo=%s headElementName（与 %s.xsd 一致）", msgNo, msgNo)
                .isEqualTo(expectedHeadElementName);
        assertThat(desc.headClass())
                .as("msgNo=%s headClass（%s）", msgNo,
                        "3115".equals(msgNo) ? "第 6 类目 BatchHead+RequestResponseHead" : "第 2 类目 RequestBusinessHead")
                .isEqualTo(expectedHeadClass);
        assertThat(desc.requiresResultCode())
                .as("msgNo=%s requiresResultCode（batch3 均不带 ReturnCode）", msgNo)
                .isEqualTo(expectedRequiresResultCode);

        assertThat(dispatcher.isRegisteredOutboundMsgNo(msgNo))
                .as("msgNo=%s 必须在 isRegisteredOutboundMsgNo true，否则 inbound 路径走 legacy fallback",
                        msgNo)
                .isTrue();
    }
}
