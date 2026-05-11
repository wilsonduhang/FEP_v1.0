package com.puchain.fep.web.outbound.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import com.puchain.fep.converter.model.RequestBusinessHead;
import com.puchain.fep.converter.model.ResponseBusinessHead;
import com.puchain.fep.converter.wire.OutboundWireShapeDispatcher;
import com.puchain.fep.converter.wire.WireShapeDescriptor;
import com.puchain.fep.processor.body.realtime.CompanyAuthFileResponse2004;
import com.puchain.fep.processor.body.realtime.CompanyAuthFileTransfer1004;
import com.puchain.fep.processor.body.realtime.CompanyInfoRequest1001;
import com.puchain.fep.processor.body.realtime.CompanyInfoResponse2001;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.stream.Stream;

/**
 * P4-MSG-E T4 — 4 企业查询实时报文 outbound wire 链路 bean 集成 IT（参数化 × 4 case）。
 *
 * <p>对每个企业查询实时 msgNo（1001/2001/1004/2004）验证 Spring context 内
 * {@link BodyClassRegistry} + {@link OutboundWireShapeDispatcher} 两个 wire 链路 bean
 * 协调一致：</p>
 *
 * <ul>
 *   <li>1001/1004 上行请求 → RealHead{n} + RequestBusinessHead + no result</li>
 *   <li>2001/2004 上行回执 → RealHead{n} + ResponseBusinessHead + result=true（FEP 双角色 —
 *       作为供应链信息服务机构角色返回回执时走 outbound 路径，与 BodyClassRegistry Javadoc
 *       "出站报文 msgNo → Body POJO Class 主映射" 语义一致）</li>
 *   <li>4 个 msgNo 全部 isRegisteredOutboundMsgNo=true</li>
 * </ul>
 *
 * <p>与既有 {@code Outbound1101WireTest} / {@code OutboundBatchWireTest} pattern 完全对齐：
 * 仅断言 registry+dispatcher bean 协调，不调用 {@code OutboundCfxEnvelopeBuilder}。XSD
 * 校验由 T3 {@code Company*XsdValidationTest} 内存路径覆盖；端到端 XML 装配由
 * {@code OutboundCfxEnvelopeBuilderTest} 覆盖。</p>
 *
 * <p>共享 {@code @TestPropertySource} 配置（{@code fep.collector.scheduling.enabled=false} +
 * {@code management.health.redis.enabled=false}）以触发 Spring context 缓存复用。</p>
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
class OutboundEnterpriseQueryRealtimeWireTest {

    @Autowired
    private BodyClassRegistry registry;

    @Autowired
    private OutboundWireShapeDispatcher dispatcher;

    static Stream<Arguments> realtimeQueryWireMatrix() {
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

    @ParameterizedTest(name = "[{index}] msgNo={0} -> body={1}, head={2}({3}), result={4}")
    @MethodSource("realtimeQueryWireMatrix")
    @DisplayName("4 企业查询实时报文 outbound wire bean 协调")
    void wire_realtimeQuery_should_resolve_consistently(
            final String msgNo,
            final Class<?> expectedBodyClass,
            final String expectedHeadElementName,
            final Class<? extends RequestBusinessHead> expectedHeadClass,
            final boolean expectedRequiresResultCode) {

        // BodyClassRegistry: msgNo -> Realtime POJO Class
        assertThat(registry.resolve(msgNo))
                .as("BodyClassRegistry.resolve(\"%s\") 必须返回 %s（P4-MSG-E T1 注册）",
                        msgNo, expectedBodyClass.getSimpleName())
                .isEqualTo(expectedBodyClass);

        // OutboundWireShapeDispatcher: msgNo -> wire-shape 描述符
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

        // BatchMessageProcessorService.resolveHeadElementName 路径：实时查询走 dispatcher
        assertThat(dispatcher.isRegisteredOutboundMsgNo(msgNo))
                .as("msgNo=%s 必须在 isRegisteredOutboundMsgNo true，否则 inbound 路径会走 legacy fallback",
                        msgNo)
                .isTrue();
    }
}
