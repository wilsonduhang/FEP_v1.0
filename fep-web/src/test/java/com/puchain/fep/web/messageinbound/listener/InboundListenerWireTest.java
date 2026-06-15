package com.puchain.fep.web.messageinbound.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.body.batch.CompanyAuthFileBatchResponse2104;
import com.puchain.fep.processor.body.batch.CompanyInfoBatchResponse2103;
import com.puchain.fep.processor.body.batch.DataTransferCheckBatchResponse2102;
import com.puchain.fep.processor.event.InboundMessageProcessedEvent;
import com.puchain.fep.processor.pipeline.SyncMessageProcessorService;
import com.puchain.fep.processor.state.MessageProcessRecord;
import com.puchain.fep.processor.state.MessageProcessStatus;
import com.puchain.fep.transport.api.TlqProducer;
import com.puchain.fep.transport.model.TlqChannel;
import com.puchain.fep.transport.model.TlqMessage;
import com.puchain.fep.transport.model.TlqMessageAttributes;
import com.puchain.fep.web.messageinbound.service.InboundMessageDispatcher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

/**
 * P4-MSG-A inbound listener wire IT — end-to-end TLQ producer → broker
 * → {@link TlqInboundListener} → {@link InboundMessageDispatcher} →
 * {@link InboundMessageProcessedEvent} 链路验证（4 case）。
 *
 * <p><b>验证目标</b>: 锁定 P4-MSG-A 3 BATCH 报文（2102/2103/2104）从 TLQ
 * 投递入站到 Spring 事件广播的全链路 wire 协调一致；同时锁定 P3-DEFER-DLQ
 * silent-failure 行为契约（malformed XML → listener 不抛 + dispatcher 未调用）。</p>
 *
 * <p><b>测试拓扑</b>:
 * <ul>
 *   <li>{@code @SpringBootTest} 启动完整 fep-web context，激活默认
 *       {@code fep.transport.provider=mock}（matchIfMissing），
 *       {@link com.puchain.fep.web.messageinbound.config.TlqInboundConfiguration}
 *       自动 subscribe REALTIME_RECEIVE + BATCH_RECEIVE。</li>
 *   <li>{@code @SpyBean} 包装真实 {@link InboundMessageDispatcher}，验证
 *       dispatch(msgNo, transitionNo, xml) 入参 + 调用次数。</li>
 *   <li>{@code @MockBean SyncMessageProcessorService} 跳过 XSD validation +
 *       DB persist pipeline；dispatcher 仍真跑 tryUnmarshalBody +
 *       publishEvent 完整 wire 链路（Plan v1.1 修订 santa Reviewer C' M-1）。</li>
 *   <li>{@link CapturedEventConfig} 注册 {@code @EventListener} 收集
 *       {@link InboundMessageProcessedEvent}，断言 messageType / transitionNo /
 *       body class / serialNo fallback。</li>
 *   <li>{@link TlqProducer} 投递 BATCH XML → broker 同步触发 listener.onMessage
 *       （{@link com.puchain.fep.transport.mock.InMemoryMessageBroker#publish}
 *       同步调用 listener，无需 await）。</li>
 * </ul>
 *
 * <p><b>不在范围内</b>（参见 Plan §0 范围声明）:
 * <ul>
 *   <li>DLQ 实现 — silent-failure 行为本 IT 锁定，DLQ 升级路径见 ADR
 *       {@code 2026-05-09-p3-defer-dlq-deferred.md}。</li>
 *   <li>其他 6 entries（3007/3008/3107/3108/3115/3116）end-to-end IT —
 *       dispatcher unit test 已覆盖同 routing 路径，listener wire 不重复。</li>
 *   <li>真 broker IT-bridge / R3 transitionNo 真值 — 见 BLOCKED ADR
 *       {@code 2026-05-05-inbound-realhead-extraction-blocked.md}。</li>
 * </ul>
 *
 * <p><b>transitionNo 提取约定</b>（R3 升级后）: {@code TlqInboundListener} 优先经
 * {@code InboundTransitionNoExtractor} 从业务头 {@code BatchHead{n}.TransitionNo}
 * 提取真值（PRD §3.2.3/§3.2.4「按原值回填」），msgId 末 8 位仅为提取失败时的兜底
 * （ADR {@code 2026-05-05-inbound-realhead-extraction-blocked.md} §R3 Addendum）。本 IT
 * 断言业务头 TransitionNo 值（本矩阵 3 fixture 均 = "00000003"），mock when() 与
 * verify() 用同一 expectedTransitionNo（= 业务头真值）。</p>
 *
 * <p>PRD 依据: v1.3 §3.1.1（四通道）+ §4.6（双角色）+ §4.7（处理模式：非实时）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@TestPropertySource(properties = {
        "fep.collector.scheduling.enabled=false",
        "management.health.redis.enabled=false"
})
class InboundListenerWireTest {

    /**
     * BATCH 2102 数据报送核对回执 — BatchHead2102.TransitionNo = "00000003"
     * （R3 后 dispatch 取此业务头真值；msgId 末 8 位 "00000001" 仅兜底）。
     */
    private static final String VALID_2102_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <CFX>
              <HEAD>
                <Version>1.0</Version>
                <SrcNode>A1000143000104</SrcNode>
                <DesNode>12345678901234</DesNode>
                <App>HNDEMP</App>
                <MsgNo>2102</MsgNo>
                <MsgId>20260509120000000001</MsgId>
                <CorrMsgId>20260509120000000001</CorrMsgId>
                <WorkDate>20260509</WorkDate>
              </HEAD>
              <MSG>
                <BatchHead2102>
                  <SendOrgCode>A1000143000104</SendOrgCode>
                  <EntrustDate>20260509</EntrustDate>
                  <TransitionNo>00000003</TransitionNo>
                  <Result>90000</Result>
                </BatchHead2102>
                <DataTransferCheckResponse2102>
                  <DataTransferResult>
                    <ItemId>1</ItemId>
                    <MainClass>COINFO</MainClass>
                    <SecondClass>I1001</SecondClass>
                    <Period>01</Period>
                    <FileDate>20260509</FileDate>
                    <Status>01</Status>
                  </DataTransferResult>
                </DataTransferCheckResponse2102>
              </MSG>
            </CFX>
            """;

    /**
     * BATCH 2103 企业信息批量查询回执 — BatchHead2103.TransitionNo = "00000003"
     * （= msgId 末 8 位，巧合一致）。
     */
    private static final String VALID_2103_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <CFX>
              <HEAD>
                <Version>1.0</Version>
                <SrcNode>A1000143000104</SrcNode>
                <DesNode>12345678901234</DesNode>
                <App>HNDEMP</App>
                <MsgNo>2103</MsgNo>
                <MsgId>20260509120000000003</MsgId>
                <CorrMsgId>20260509120000000001</CorrMsgId>
                <WorkDate>20260509</WorkDate>
              </HEAD>
              <MSG>
                <BatchHead2103>
                  <SendOrgCode>12345678901234</SendOrgCode>
                  <EntrustDate>20260509</EntrustDate>
                  <TransitionNo>00000003</TransitionNo>
                  <Result>90000</Result>
                </BatchHead2103>
                <CompanyInfoBatchResponse2103>
                  <CompanyInfo>
                    <ItemId>1</ItemId>
                    <CompanyName>湖南示例实业有限公司</CompanyName>
                    <CompanyCode>91430100MA4L5XXXX1</CompanyCode>
                    <MainClass>COINFO</MainClass>
                    <SecondClass>I1001</SecondClass>
                    <AuthOrgCode>12345678901234</AuthOrgCode>
                    <QueryResult>90000</QueryResult>
                  </CompanyInfo>
                </CompanyInfoBatchResponse2103>
              </MSG>
            </CFX>
            """;

    /**
     * BATCH 2104 授权书批量回执 — BatchHead2104.TransitionNo = "00000003"
     * （R3 后 dispatch 取此业务头真值；msgId 末 8 位 "00000001" 仅兜底）。
     */
    private static final String VALID_2104_XML = """
            <?xml version="1.0" encoding="UTF-8"?>
            <CFX>
              <HEAD>
                <Version>1.0</Version><SrcNode>A1000143000104</SrcNode>
                <DesNode>12345678901234</DesNode><App>HNDEMP</App>
                <MsgNo>2104</MsgNo><MsgId>20260509120000000001</MsgId>
                <CorrMsgId>20260509120000000001</CorrMsgId><WorkDate>20260509</WorkDate>
              </HEAD>
              <MSG>
                <BatchHead2104>
                  <SendOrgCode>A1000143000104</SendOrgCode>
                  <EntrustDate>20260509</EntrustDate>
                  <TransitionNo>00000003</TransitionNo>
                  <Result>90000</Result>
                </BatchHead2104>
                <CompanyAuthFileBatchResponse2104>
                  <CompanyAuthFileResponse>
                    <ItemId>1</ItemId>
                    <CompanyName>湖南示例实业有限公司</CompanyName>
                    <CompanyCode>91430100MA4L5XXXX1</CompanyCode>
                    <AuthBeginDate>20260101</AuthBeginDate>
                    <AuthEndDate>20261231</AuthEndDate>
                    <AuthNo>AUTH2026050500001</AuthNo>
                    <AuthOrgCode>12345678901234</AuthOrgCode>
                    <IsUpdate>0</IsUpdate>
                    <RecordResult>90000</RecordResult>
                  </CompanyAuthFileResponse>
                </CompanyAuthFileBatchResponse2104>
              </MSG>
            </CFX>
            """;

    @Autowired
    private TlqProducer tlqProducer;

    @SpyBean
    private InboundMessageDispatcher dispatcher;

    /**
     * v1.1 修订 (santa Reviewer C' M-1)：mock SyncMessageProcessorService 跳过
     * XSD validation + DB persist pipeline。本 IT 主旨是 listener wire chain
     * (producer → broker → listener → dispatcher → publishEvent)，不验证
     * dispatcher 下游 syncProcessor.processInbound 内部 XSD/DB 行为
     * (dispatcher unit test 已覆盖：InboundMessageDispatcherTest 8 cases 全
     * mock SyncMessageProcessorService 用 when().thenReturn(completed) 模式)。
     */
    @MockBean
    private SyncMessageProcessorService syncProcessor;

    @Autowired
    private CapturedEventCollector eventCollector;

    /**
     * v1.1 修订 (santa Reviewer B' M-2)：每个 test method 重置 spy + mock + event
     * collector。原 inline `eventCollector.clear()` 不够 — `@SpyBean dispatcher`
     * 累积 ParameterizedTest 3 个 case 的 dispatch 调用记录，silent_failure case 用
     * `verify(dispatcher, never()).dispatch(...)` 会因 spy 历史 invocation 失败。
     * `Mockito.reset(syncProcessor, dispatcher)` 同时清掉 mock setup 和 spy
     * invocation 历史，符合 SubDashboardControllerTest 现有 @BeforeEach 模式。
     */
    @BeforeEach
    void setUpMocks() {
        Mockito.reset(syncProcessor, dispatcher);
        eventCollector.clear();
    }

    /**
     * 提供 3 BATCH 测试矩阵: msgNo / xml / expected body class / expected transitionNo
     * （R3 后 = 业务头 BatchHead{n}.TransitionNo 真值；本矩阵 3 fixture TransitionNo 均 = "00000003"）.
     */
    static Stream<Arguments> batchInboundMatrix() {
        return Stream.of(
                Arguments.of("2102", VALID_2102_XML,
                        DataTransferCheckBatchResponse2102.class, "00000003"),
                Arguments.of("2103", VALID_2103_XML,
                        CompanyInfoBatchResponse2103.class, "00000003"),
                Arguments.of("2104", VALID_2104_XML,
                        CompanyAuthFileBatchResponse2104.class, "00000003")
        );
    }

    @ParameterizedTest(name = "[{index}] msgNo={0} → dispatcher invoked + event published with body={2}")
    @MethodSource("batchInboundMatrix")
    @DisplayName("BATCH inbound: producer.send → broker → listener → dispatcher → event published")
    void inbound_batchMessage_endToEndWireSucceeds(
            final String msgNo,
            final String xml,
            final Class<?> expectedBodyClass,
            final String expectedTransitionNo) {

        // v1.1 修订 (santa Reviewer C' M-1)：mock syncProcessor.processInbound
        // 返回 COMPLETED 状态，让 dispatcher 进入 tryUnmarshalBody + publishEvent
        // 分支。这样 IT 仍然真跑 listener → dispatcher.dispatch + JAXB body
        // unmarshal + Spring event publish 的 wire 链路；syncProcessor 内部
        // XSD validation + DB persist 在 dispatcher unit test 已覆盖，不重复。
        final MessageType type = MessageType.byMsgNo(msgNo).orElseThrow();
        final MessageProcessRecord completed = MessageProcessRecord.initial(
                        "rec-it-" + msgNo + "-001abcdef0123456789abcdef0123",
                        type, expectedTransitionNo, Instant.now())
                .withStatus(MessageProcessStatus.COMPLETED, Instant.now());
        when(syncProcessor.processInbound(eq(type), eq(expectedTransitionNo), any(byte[].class)))
                .thenReturn(completed);

        final TlqMessageAttributes attrs =
                TlqMessageAttributes.forBatch("MSG-ID-INBOUND-IT-" + msgNo);
        final TlqMessage message = new TlqMessage(
                xml,
                attrs,
                TlqChannel.BATCH_RECEIVE);

        tlqProducer.send(message);

        // broker.publish 同步触发 listener.onMessage —— 同事务 dispatch 完成
        verify(dispatcher).dispatch(
                eq(msgNo),
                eq(expectedTransitionNo),
                any(byte[].class));

        // ApplicationEventListener 在 dispatcher publishEvent 后同步触发
        final List<InboundMessageProcessedEvent> captured = eventCollector.snapshot();
        assertThat(captured).as("exactly one event published for msgNo=%s", msgNo).hasSize(1);

        final InboundMessageProcessedEvent event = captured.get(0);
        assertThat(event.type().msgNo()).as("event messageType").isEqualTo(msgNo);
        assertThat(event.transitionNo()).as("event transitionNo").isEqualTo(expectedTransitionNo);
        assertThat(event.body()).as("event body class").isInstanceOf(expectedBodyClass);
        // BATCH Response body 无 getSerialNo getter，dispatcher.extractSerialNo
        // NoSuchMethodException → fallback 到 transitionNo（对照 ADR
        // 2026-05-05-inbound-realhead-extraction-blocked.md R3 占位）。
        assertThat(event.serialNo()).as("event serialNo fallback to transitionNo")
                .isEqualTo(expectedTransitionNo);
    }

    @Test
    @DisplayName("silent_failure: malformed XML → listener swallows + dispatcher.dispatch never invoked (ADR P3-DEFER-DLQ)")
    void silent_failure_malformedXml_listenerSwallowsException_dispatcherNotInvoked() {
        // @BeforeEach 已 reset spy + mock + clear collector

        final TlqMessageAttributes attrs =
                TlqMessageAttributes.forBatch("MSG-ID-INBOUND-IT-MALFORMED");
        final TlqMessage message = new TlqMessage(
                "<not-cfx>broken</not-cfx>",
                attrs,
                TlqChannel.BATCH_RECEIVE);

        // listener catches xmlCodec.unmarshal RuntimeException + log ERROR + return void.
        // broker.publish 不抛（symptom: dispatcher 完全未被调用）。
        tlqProducer.send(message);

        verify(dispatcher, never()).dispatch(any(), any(), any(byte[].class));
        // 二次防护 (santa Reviewer C' M-1)：dispatcher 既然没调用，syncProcessor 也必然没调用。
        verify(syncProcessor, never()).processInbound(any(), any(), any(byte[].class));
        assertThat(eventCollector.snapshot()).as("no event published on parse failure").isEmpty();
    }

    /**
     * Spring {@code @TestConfiguration} 注册同步事件收集器 bean。
     *
     * <p>{@code @EventListener} 默认同步触发（与 dispatcher publishEvent 同事务），
     * 无需 await。</p>
     */
    @TestConfiguration
    static class CapturedEventConfig {

        @Bean
        CapturedEventCollector capturedEventCollector() {
            return new CapturedEventCollector();
        }
    }

    /**
     * 线程安全事件收集器（CopyOnWriteArrayList）。`snapshot` 返回 List 副本避免
     * 测试断言期间被修改（broker.publish 同步触发，理论无并发，但防御性写法）。
     */
    static class CapturedEventCollector {

        private final List<InboundMessageProcessedEvent> events = new CopyOnWriteArrayList<>();

        @EventListener
        public void onInboundProcessed(final InboundMessageProcessedEvent event) {
            events.add(event);
        }

        public void clear() {
            events.clear();
        }

        public List<InboundMessageProcessedEvent> snapshot() {
            return List.copyOf(events);
        }
    }
}
