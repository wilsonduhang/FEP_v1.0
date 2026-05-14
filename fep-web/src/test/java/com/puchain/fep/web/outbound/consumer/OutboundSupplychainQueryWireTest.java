package com.puchain.fep.web.outbound.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import com.puchain.fep.converter.model.RequestBusinessHead;
import com.puchain.fep.converter.model.ResponseBusinessHead;
import com.puchain.fep.converter.wire.OutboundWireShapeDispatcher;
import com.puchain.fep.converter.wire.WireShapeDescriptor;
import com.puchain.fep.processor.body.supplychain.ProgressQuery3001;
import com.puchain.fep.processor.body.supplychain.ProgressQueryReturn3002;
import com.puchain.fep.processor.body.supplychain.PzInfoQuery3003;
import com.puchain.fep.processor.body.supplychain.PzInfoReturn3004;
import com.puchain.fep.processor.body.supplychain.QyAccQuery3005;
import com.puchain.fep.processor.body.supplychain.QyAccQueryReturn3006;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.stream.Stream;

/**
 * P4-MSG-F T4 — 6 供应链查询报文 outbound wire 链路 bean 集成 IT（参数化 × 6 case）。
 *
 * <p>对每个供应链查询 msgNo（3001/3002/3003/3004/3005/3006）验证 Spring context 内
 * {@link BodyClassRegistry} + {@link OutboundWireShapeDispatcher} 两个 wire 链路 bean
 * 协调一致：</p>
 *
 * <ul>
 *   <li>3001/3003/3005 上行请求 → RealHead{n} + RequestBusinessHead + no result</li>
 *   <li>3002/3004/3006 上行回执 → RealHead{n} + ResponseBusinessHead + result=true（与
 *       P4-MSG-E 2001/2004 同类目）</li>
 *   <li>6 msgNo 全部 isRegisteredOutboundMsgNo=true</li>
 * </ul>
 *
 * <p>与既有 {@code OutboundEnterpriseQueryRealtimeWireTest} (P4-MSG-E T4) /
 * {@code OutboundBatchWireTest} (P4-MSG-A T3) pattern 完全对齐：仅断言 registry+dispatcher
 * bean 协调，不调用 {@code OutboundCfxEnvelopeBuilder}。XSD validate 由 T3 6
 * {@code *XsdValidationTest} 内存路径覆盖；端到端 XML 装配由 {@code OutboundCfxEnvelopeBuilderTest}
 * 既有覆盖（21+6 msgNos 自动通过 dispatcher 路由覆盖）。</p>
 *
 * <p>共享 {@code @TestPropertySource} 配置（{@code fep.collector.scheduling.enabled=false} +
 * {@code management.health.redis.enabled=false}）以触发 Spring context 缓存复用，与
 * {@link OutboundEnterpriseQueryRealtimeWireTest} 同 context。</p>
 *
 * <p>PRD 依据: v1.3 §3.2 报文结构 + §4.2 实时类业务报文 + §4.6 报文方向 + §4.7 模式 1 同步。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@TestPropertySource(properties = {
        "fep.collector.scheduling.enabled=false",
        "management.health.redis.enabled=false"
})
class OutboundSupplychainQueryWireTest {

    @Autowired
    private BodyClassRegistry registry;

    @Autowired
    private OutboundWireShapeDispatcher dispatcher;

    static Stream<Arguments> supplychainQueryWireMatrix() {
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

    @ParameterizedTest(name = "[{index}] msgNo={0} → body={1}, head={2}({3}), result={4}")
    @MethodSource("supplychainQueryWireMatrix")
    @DisplayName("6 supplychain query outbound wire bean 协调")
    void wire_supplychainQuery_should_resolve_consistently(
            final String msgNo,
            final Class<?> expectedBodyClass,
            final String expectedHeadElementName,
            final Class<? extends RequestBusinessHead> expectedHeadClass,
            final boolean expectedRequiresResultCode) {

        // BodyClassRegistry: msgNo → SupplyChain query POJO Class
        assertThat(registry.resolve(msgNo))
                .as("BodyClassRegistry.resolve(\"%s\") 必须返回 %s（P4-MSG-F T1 注册）",
                        msgNo, expectedBodyClass.getSimpleName())
                .isEqualTo(expectedBodyClass);

        // OutboundWireShapeDispatcher: msgNo → wire-shape 描述符
        final WireShapeDescriptor desc = dispatcher.describeFor(msgNo);
        assertThat(desc.headElementName())
                .as("msgNo=%s headElementName（与 %s.xsd 一致）", msgNo, msgNo)
                .isEqualTo(expectedHeadElementName);
        assertThat(desc.headClass())
                .as("msgNo=%s headClass（%s）", msgNo,
                        msgNo.endsWith("1") || msgNo.endsWith("3") || msgNo.endsWith("5")
                                ? "请求" : "回执")
                .isEqualTo(expectedHeadClass);
        assertThat(desc.requiresResultCode())
                .as("msgNo=%s requiresResultCode（%s）", msgNo,
                        expectedRequiresResultCode ? "回执含 ReturnCode" : "请求不带 ReturnCode")
                .isEqualTo(expectedRequiresResultCode);

        // BatchMessageProcessorService.resolveHeadElementName 路径
        assertThat(dispatcher.isRegisteredOutboundMsgNo(msgNo))
                .as("msgNo=%s 必须在 isRegisteredOutboundMsgNo true，否则 inbound 路径会走 legacy fallback",
                        msgNo)
                .isTrue();
    }
}
