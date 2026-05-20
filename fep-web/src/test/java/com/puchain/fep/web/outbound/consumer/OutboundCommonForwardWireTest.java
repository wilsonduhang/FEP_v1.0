package com.puchain.fep.web.outbound.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import com.puchain.fep.converter.model.RequestBusinessHead;
import com.puchain.fep.converter.model.ResponseBusinessHead;
import com.puchain.fep.converter.wire.OutboundWireShapeDispatcher;
import com.puchain.fep.converter.wire.WireShapeDescriptor;
import com.puchain.fep.processor.body.common.Forward9000;
import com.puchain.fep.processor.body.common.Forward9100;
import com.puchain.fep.processor.body.common.MsgReturn9120;
import com.puchain.fep.processor.body.supplychain.HxqyCreditAmt3113;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * P4-MSG-I T4 — 4 common/supplychain batch4 报文 outbound wire 链路 bean 集成 IT（参数化 × 4 case）。
 *
 * <p>对每个 batch4 msgNo（9120/3113/9100/9000）验证 Spring context 内 {@link BodyClassRegistry} +
 * {@link OutboundWireShapeDispatcher} 两个 wire 链路 bean 协调一致：</p>
 *
 * <ul>
 *   <li>9120 通用应答（2101 模式 6 ack）→ BatchHead9120 + ResponseBusinessHead + with result
 *       （既有 BatchHead+ResponseBusinessHead+true 类目扩展，P4-MSG-I T1）</li>
 *   <li>3113 核心企业授信额度回执 → BatchHead3113 + ResponseBusinessHead + with result
 *       （同上类目扩展，P4-MSG-I T1）</li>
 *   <li>9100 非实时业务通用转发（模式 3）→ BatchHead9100 + RequestBusinessHead + no result
 *       （既有 BatchHead+RequestBusinessHead+false 类目扩展，P4-MSG-I T1）</li>
 *   <li>9000 实时业务通用转发 → RealHead9000 + RequestBusinessHead + no result
 *       （既有 RealHead+RequestBusinessHead+false 类目扩展，P4-MSG-I T1）</li>
 * </ul>
 *
 * <p>pattern 严格对齐 {@link OutboundSupplychainBatch3WireTest}（P4-MSG-H batch3）——
 * 仅断言 registry+dispatcher bean 协调，不调用 marshal / {@code XsdValidator.validate()}
 * （XSD validate 由 {@code CommonXsdValidationTest} + {@code SupplyChainExtXsdValidationTest}
 * 9120/3113/9100/9000 @ValueSource 完整覆盖于 T3）。</p>
 *
 * <p>共享 {@code @TestPropertySource} 配置以触发 Spring context 缓存复用，与
 * {@link OutboundSupplychainBatch3WireTest} 同 context（无 {@code @MockBean}，保留缓存复用）。</p>
 *
 * <p>PRD 依据: v1.3 §4.1.1 报文清单 + §3.2 报文结构 + §4.6 报文方向 + §4.5 通用报文。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@TestPropertySource(properties = {
        "fep.collector.scheduling.enabled=false",
        "management.health.redis.enabled=false"
})
class OutboundCommonForwardWireTest {

    @Autowired
    private BodyClassRegistry registry;

    @Autowired
    private OutboundWireShapeDispatcher dispatcher;

    static Stream<Arguments> commonForwardBatch4WireMatrix() {
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

    @ParameterizedTest(name = "[{index}] msgNo={0} → body={1}, head={2}({3}), result={4}")
    @MethodSource("commonForwardBatch4WireMatrix")
    @DisplayName("4 common/supplychain batch4 outbound wire bean 协调")
    void wire_commonForwardBatch4_should_resolve_consistently(
            final String msgNo,
            final Class<?> expectedBodyClass,
            final String expectedHeadElementName,
            final Class<? extends RequestBusinessHead> expectedHeadClass,
            final boolean expectedRequiresResultCode) {

        // BodyClassRegistry: msgNo → batch4 POJO Class
        assertThat(registry.resolve(msgNo))
                .as("BodyClassRegistry.resolve(\"%s\") 必须返回 %s（P4-MSG-I T2 注册）",
                        msgNo, expectedBodyClass.getSimpleName())
                .isEqualTo(expectedBodyClass);

        // OutboundWireShapeDispatcher: msgNo → wire-shape 描述符
        final WireShapeDescriptor desc = dispatcher.describeFor(msgNo);
        assertThat(desc.headElementName())
                .as("msgNo=%s headElementName（与 %s.xsd 一致）", msgNo, msgNo)
                .isEqualTo(expectedHeadElementName);
        assertThat(desc.headClass())
                .as("msgNo=%s headClass（既有类目扩展，P4-MSG-I T1）", msgNo)
                .isEqualTo(expectedHeadClass);
        assertThat(desc.requiresResultCode())
                .as("msgNo=%s requiresResultCode（9120/3113 含 ResultCode；9100/9000 无）", msgNo)
                .isEqualTo(expectedRequiresResultCode);

        assertThat(dispatcher.isRegisteredOutboundMsgNo(msgNo))
                .as("msgNo=%s 必须在 isRegisteredOutboundMsgNo true，否则 inbound 路径走 legacy fallback",
                        msgNo)
                .isTrue();
    }
}
