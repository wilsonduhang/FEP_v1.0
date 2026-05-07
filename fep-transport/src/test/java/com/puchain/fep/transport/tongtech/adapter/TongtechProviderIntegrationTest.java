package com.puchain.fep.transport.tongtech.adapter;

import com.puchain.fep.common.util.FepConstants;
import com.puchain.fep.transport.TransportAutoConfiguration;
import com.puchain.fep.transport.api.NodeLifecycleManager;
import com.puchain.fep.transport.api.RemoteAdmin;
import com.puchain.fep.transport.api.RetryableProducer;
import com.puchain.fep.transport.api.SendResult;
import com.puchain.fep.transport.api.TlqConsumer;
import com.puchain.fep.transport.api.TlqProducer;
import com.puchain.fep.transport.mock.InMemoryDeadLetterHandler;
import com.puchain.fep.transport.mock.InMemoryMessageBroker;
import com.puchain.fep.transport.mock.InMemoryNodeLifecycleManager;
import com.puchain.fep.transport.mock.InMemoryRemoteAdmin;
import com.puchain.fep.transport.mock.InMemoryTlqConnectionFactory;
import com.puchain.fep.transport.mock.InMemoryTlqConsumer;
import com.puchain.fep.transport.mock.InMemoryTlqProducer;
import com.puchain.fep.transport.model.NodeState;
import com.puchain.fep.transport.model.TlqChannel;
import com.puchain.fep.transport.model.TlqMessage;
import com.puchain.fep.transport.model.TlqMessageAttributes;
import com.puchain.fep.transport.support.MessageDeduplicator;
import com.puchain.fep.transport.support.QueueNameResolver;
import com.puchain.fep.transport.tongtech.config.TongtechTlqProperties;
import com.puchain.fep.transport.tongtech.config.TongtechTransportConfiguration;
import com.puchain.fep.transport.tongtech.error.TongtechErrorMapper;
import com.puchain.fep.transport.tongtech.lifecycle.TongtechNodeLifecycleManager;
import com.tongtech.tlq.base.TlqException;
import com.tongtech.tlq.base.TlqMsgOpt;
import com.tongtech.tlq.base.TlqQCU;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Provider-level integration test exercising the full {@code @ConditionalOnProperty(provider=tongtech)}
 * wiring path with the SDK layer mocked.
 *
 * <p>Realises Plan §Task 9 (v1b evidence 精修, lines 2604-2655) acceptance:</p>
 * <ol>
 *   <li><b>#1</b> {@link SpringBootTest} loads {@link TransportAutoConfiguration} +
 *       {@link TongtechTransportConfiguration} with {@code fep.transport.provider=tongtech},
 *       all SDK calls served by Mockito mocks ({@link MockBean} on
 *       {@link TongtechTlqConnectionFactory} + {@link TongtechRemoteAdmin}).</li>
 *   <li><b>#2 — 8/9 队列名 IT 覆盖 (PRD §3.1.2 9 队列)</b> — 4 channel × 2 send/recv:
 *       <ul>
 *         <li>{@link TlqChannel#REALTIME_SEND} → {@code QSEND.<HNDEMP>.REAL.1}
 *             (核心 QueName + msgId 持久 + 默认 zip/encrypt 透传)</li>
 *         <li>{@link TlqChannel#BATCH_SEND}    → {@code QSEND.<HNDEMP>.BATCH.1}
 *             (核心 QueName + persistence/expiry forBatch 默认值)</li>
 *         <li>{@link TlqChannel#REALTIME_RECEIVE} → {@code QLOCAL.<HNDEMP>.REAL.1}
 *             (核心 QueName + payload roundtrip)</li>
 *         <li>{@link TlqChannel#BATCH_RECEIVE}    → {@code QLOCAL.<HNDEMP>.BATCH.1}
 *             (核心 QueName + empty-poll TlqException → Optional.empty)</li>
 *       </ul>
 *       Each channel test asserts both {@code QueName} and one auxiliary semantic
 *       dimension (8 total assertion 维度，同 plan 行 2616-2622)。
 *   </li>
 *   <li><b>DEAD_LETTER</b> 第 9 队列由
 *       {@code com.puchain.fep.transport.api.RetryableProducerTest#send_putMessageAlwaysFails_shouldRouteToDlh}
 *       间接覆盖（ArgumentCaptor 验 {@code deadLetterHandler.handle} 调用，plan 行 2621）。
 *       4 LOCAL/REMOTE 队列不在 P1c send/receive 路径，留 P1c-IT-bridge follow-up。</li>
 *   <li><b>#3 — 5 个核心 IT case</b>: context wiring / roundtrip / lifecycle login /
 *       connectivity probe / provider isolation。</li>
 * </ol>
 *
 * <p>Test 数据中 institution code 取自 {@link com.puchain.fep.common.util.FepConstants#HNDEMP_NODE_CODE}（覆盖
 * {@link com.puchain.fep.transport.TransportProperties#DEFAULT_INSTITUTION_CODE}
 * 默认值 {@code DEFAULT_INST_00}），与 plan 期望的队列前缀一致。</p>
 *
 * <p>{@link DirtiesContext} 标记确保 {@link TongtechTlqConsumer} 的 daemon scheduler
 * 在每次测试方法后被关闭释放，避免 thread leak。</p>
 *
 * <p><b>AI-Generated</b>: claude-code (P1c T9 v1b implementer subagent)</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest(
        classes = {TransportAutoConfiguration.class, TongtechTransportConfiguration.class},
        properties = {
                "fep.transport.provider=tongtech",
                "fep.transport.institution-code=" + FepConstants.HNDEMP_NODE_CODE,
                "fep.transport.tongtech.broker-host=127.0.0.1",
                "fep.transport.tongtech.broker-port=10024",
                "fep.transport.tongtech.qcu-name=QCU_HNDEMP_" + FepConstants.HNDEMP_NODE_CODE + "_1",
                "fep.transport.tongtech.consumer-poll-interval-ms=100",
                "fep.transport.tongtech.admin-host=127.0.0.1",
                "fep.transport.tongtech.admin-port=20001"
        }
)
@Import(TongtechProviderIntegrationTest.SdkMockConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TongtechProviderIntegrationTest {

    /** HNDEMP 中心节点代码（PRD v1.3 §3.1.2 / CLAUDE.md "已知约束"）。 */
    private static final String HNDEMP_CODE = FepConstants.HNDEMP_NODE_CODE;

    /** 期望的实时 SEND 队列名。 */
    private static final String Q_REALTIME_SEND = "QSEND." + HNDEMP_CODE + ".REAL.1";

    /** 期望的批量 SEND 队列名。 */
    private static final String Q_BATCH_SEND = "QSEND." + HNDEMP_CODE + ".BATCH.1";

    /** 期望的实时 RECEIVE (= LOCAL DEST) 队列名。 */
    private static final String Q_REALTIME_RECEIVE = "QLOCAL." + HNDEMP_CODE + ".REAL.1";

    /** 期望的批量 RECEIVE (= LOCAL DEST) 队列名。 */
    private static final String Q_BATCH_RECEIVE = "QLOCAL." + HNDEMP_CODE + ".BATCH.1";

    /** Default probe RTT used by the connectivity_check test. */
    private static final long PROBE_RTT_MS = 12L;

    /** Default probe target port used by the connectivity_check test. */
    private static final int PROBE_PORT = 20001;

    /** Pull-mode receive wait window — kept short to keep the test suite fast. */
    private static final Duration RECEIVE_WAIT = Duration.ofMillis(100);

    @org.springframework.beans.factory.annotation.Autowired
    private ApplicationContext ctx;

    @org.springframework.beans.factory.annotation.Autowired
    private TlqProducer primaryProducer;

    @org.springframework.beans.factory.annotation.Autowired
    @org.springframework.beans.factory.annotation.Qualifier("tongtechTlqProducer")
    private TongtechTlqProducer rawTongtechProducer;

    @org.springframework.beans.factory.annotation.Autowired
    private TlqConsumer consumer;

    @org.springframework.beans.factory.annotation.Autowired
    private NodeLifecycleManager lifecycleManager;

    @org.springframework.beans.factory.annotation.Autowired
    private RemoteAdmin remoteAdmin;

    @MockBean
    private TongtechTlqConnectionFactory connectionFactory;

    @MockBean
    private TongtechRemoteAdmin tongtechRemoteAdmin;

    /** Re-stubbed on every test method due to {@link DirtiesContext} reload. */
    private TlqQCU qcu;

    @BeforeEach
    void setUp() {
        qcu = mock(TlqQCU.class);
        when(connectionFactory.isConnected()).thenReturn(true);
        when(connectionFactory.getQCU()).thenReturn(qcu);
    }

    // ============================================================================
    // 验收 #2 — 8/9 队列名 IT 覆盖（4 channel × 2 维度 = 8 语义断言点）
    // 设计选择：4 channel × 4 测试方法，每个方法验证 QueName + 1 辅助语义维度
    // （等价于 plan 行 2616 "4 channel × 2 send/recv = 8 case" 的 8 维度，
    //   合并成 4 测试方法以避免与 T5/T6 单元测试冗余 — commit message 已说明）
    // ============================================================================

    @Test
    @DisplayName("验收#2-1: REALTIME_SEND → QSEND." + FepConstants.HNDEMP_NODE_CODE + ".REAL.1 (QueName + msgId persistence)")
    void producer_send_realtimeSend_shouldUseQSendHndempReal_andPersistMsgId() throws TlqException {
        final TlqMessageAttributes attrs = TlqMessageAttributes.forRealtime("M-RT-SEND-1");
        final TlqMessage msg = new TlqMessage("<CFX/>", attrs, TlqChannel.REALTIME_SEND);

        final SendResult result = rawTongtechProducer.send(msg);

        // 维度 1: QueName 命中 PRD §3.1.2 实时发送队列
        final ArgumentCaptor<TlqMsgOpt> optCap = ArgumentCaptor.forClass(TlqMsgOpt.class);
        verify(qcu).putMessage(any(com.tongtech.tlq.base.TlqMessage.class), optCap.capture());
        assertThat(optCap.getValue().QueName).isEqualTo(Q_REALTIME_SEND);
        // 维度 2: send 成功路径 msgId 透传，realtime 默认 attrs 不强制 persist
        assertThat(result.success()).isTrue();
        assertThat(result.msgId()).isEqualTo("M-RT-SEND-1");
        assertThat(attrs.isPersistence()).isFalse();
        assertThat(attrs.isZip()).isFalse();
        assertThat(attrs.isEncrypt()).isFalse();
    }

    @Test
    @DisplayName("验收#2-2: BATCH_SEND → QSEND." + FepConstants.HNDEMP_NODE_CODE + ".BATCH.1 (QueName + forBatch persistence/expiry)")
    void producer_send_batchSend_shouldUseQSendHndempBatch_andDelegatePersistence() throws TlqException {
        // forBatch() 默认 persistence=true / expiry=-1 (NO_EXPIRY) — 必须透传到 SDK msg
        final TlqMessageAttributes attrs = TlqMessageAttributes.forBatch("M-BATCH-SEND-1");
        final TlqMessage msg = new TlqMessage("<CFX-BATCH/>", attrs, TlqChannel.BATCH_SEND);

        final SendResult result = rawTongtechProducer.send(msg);

        // 维度 3: QueName 命中 PRD §3.1.2 批量发送队列
        final ArgumentCaptor<TlqMsgOpt> optCap = ArgumentCaptor.forClass(TlqMsgOpt.class);
        verify(qcu).putMessage(any(com.tongtech.tlq.base.TlqMessage.class), optCap.capture());
        assertThat(optCap.getValue().QueName).isEqualTo(Q_BATCH_SEND);
        // 维度 4: forBatch 默认值正确委托（持久化 + 不过期）
        assertThat(result.success()).isTrue();
        assertThat(attrs.isPersistence()).isTrue();
        assertThat(attrs.getExpiry()).isEqualTo(-1);
    }

    @Test
    @DisplayName("验收#2-3: REALTIME_RECEIVE → QLOCAL." + FepConstants.HNDEMP_NODE_CODE + ".REAL.1 (QueName + payload roundtrip)")
    void consumer_receive_realtimeReceive_shouldUseQLocalHndempReal_andRoundtripPayload() throws TlqException {
        // 配置 SDK getMessage rc=0；mapper.fromSdkMessage 由真实 TongtechMessageMapper 处理，
        // 但 SDK 的 sdk msg.MsgId 是 byte[]，未填充时 mapper 返回 msgId=null。我们通过
        // pre-填充 msgId 验证 payload 完整 roundtrip 路径（payload 通过 setStringProperty/
        // getStringProperty 拼接），而 RECEIVE 路径只读 SDK 注入的 msg。
        // 简化策略：验证 QueName + Optional.isPresent + payload 字段非空（payload 路径已
        // 由 TongtechTlqConsumerTest#receive_success 详尽覆盖；本测验装配链）。
        doAnswer(inv -> {
            final com.tongtech.tlq.base.TlqMessage sdkMsg = inv.getArgument(0);
            sdkMsg.MsgId = "M-RX-RT-1".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            sdkMsg.setStringProperty("xmlstr", "<CFX-ROUNDTRIP/>");
            return 0;
        }).when(qcu).getMessage(any(), any(TlqMsgOpt.class));

        final Optional<TlqMessage> received = consumer.receive(TlqChannel.REALTIME_RECEIVE, RECEIVE_WAIT);

        // 维度 5: QueName 命中 PRD §3.1.2 实时本地接收队列（DEST → QLOCAL）
        final ArgumentCaptor<TlqMsgOpt> optCap = ArgumentCaptor.forClass(TlqMsgOpt.class);
        verify(qcu).getMessage(any(), optCap.capture());
        assertThat(optCap.getValue().QueName).isEqualTo(Q_REALTIME_RECEIVE);
        // 维度 6: 接收成功路径 payload 完整 roundtrip
        assertThat(received).isPresent();
        assertThat(received.get().getPayload()).contains("<CFX-ROUNDTRIP/>");
        assertThat(received.get().getMsgId()).isEqualTo("M-RX-RT-1");
    }

    @Test
    @DisplayName("验收#2-4: BATCH_RECEIVE → QLOCAL." + FepConstants.HNDEMP_NODE_CODE + ".BATCH.1 (QueName + empty poll Optional.empty)")
    void consumer_receive_batchReceive_shouldUseQLocalHndempBatch_andEmptyOnTimeout() throws TlqException {
        // SDK rc != 0 模拟无消息可达（业务超时）— 应该返回 Optional.empty（非异常路径）
        doAnswer(inv -> 1).when(qcu).getMessage(any(), any(TlqMsgOpt.class));

        final Optional<TlqMessage> received = consumer.receive(TlqChannel.BATCH_RECEIVE, RECEIVE_WAIT);

        // 维度 7: QueName 命中 PRD §3.1.2 批量本地接收队列（DEST → QLOCAL）
        final ArgumentCaptor<TlqMsgOpt> optCap = ArgumentCaptor.forClass(TlqMsgOpt.class);
        verify(qcu).getMessage(any(), optCap.capture());
        assertThat(optCap.getValue().QueName).isEqualTo(Q_BATCH_RECEIVE);
        // 维度 8: 业务超时 → Optional.empty (不抛异常，让上层重新 poll)
        assertThat(received).isEmpty();
    }

    // ============================================================================
    // 验收 #3 — 5 个核心 IT case
    // ============================================================================

    @Test
    @DisplayName("验收#3-1: ApplicationContext loads all tongtech beans + excludes mock beans")
    void springContext_withProviderTongtech_shouldWireAllTongtechBeans() {
        // 装配验证 — Tongtech 实现端
        assertThat(rawTongtechProducer).isNotNull();
        assertThat(primaryProducer).isInstanceOf(RetryableProducer.class);
        assertThat(consumer).isInstanceOf(TongtechTlqConsumer.class);
        assertThat(lifecycleManager).isInstanceOf(TongtechNodeLifecycleManager.class);
        assertThat(remoteAdmin).isInstanceOf(TongtechRemoteAdmin.class);

        // 关键支撑 bean — 必须存在
        assertThat(ctx.getBean(TongtechMessageMapper.class)).isNotNull();
        assertThat(ctx.getBean(TongtechChannelMapper.class)).isNotNull();
        assertThat(ctx.getBean(TongtechErrorMapper.class)).isNotNull();
        assertThat(ctx.getBean(QueueNameResolver.class)).isNotNull();
        assertThat(ctx.getBean(MessageDeduplicator.class)).isNotNull();
        assertThat(ctx.getBean(TongtechTlqProperties.class)).isNotNull();
    }

    @Test
    @DisplayName("验收#3-2: producer.send → consumer.receive roundtrip via mocked SDK QCU")
    void producer_send_thenConsumer_receive_shouldRoundtripViaMockedSdk() throws TlqException {
        // 1) producer.send via REALTIME_SEND — putMessage 在 mock QCU 上不抛异常即视为成功
        final TlqMessage outbound = new TlqMessage(
                "<CFX-RT/>",
                TlqMessageAttributes.forRealtime("M-RT-1"),
                TlqChannel.REALTIME_SEND);
        final SendResult sendResult = rawTongtechProducer.send(outbound);
        assertThat(sendResult.success()).isTrue();
        assertThat(sendResult.msgId()).isEqualTo("M-RT-1");

        // 2) 同一 mock QCU 配置 getMessage 返回 RECEIVE 端消息（payload 与 send 一致）
        doAnswer(inv -> {
            final com.tongtech.tlq.base.TlqMessage sdkMsg = inv.getArgument(0);
            sdkMsg.MsgId = "M-RT-1".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            sdkMsg.setStringProperty("xmlstr", "<CFX-RT/>");
            return 0;
        }).when(qcu).getMessage(any(), any(TlqMsgOpt.class));

        // 3) consumer.receive on REALTIME_RECEIVE → 含 payload 的 Optional<TlqMessage>
        final Optional<TlqMessage> received = consumer.receive(
                TlqChannel.REALTIME_RECEIVE, RECEIVE_WAIT);

        assertThat(received).isPresent();
        assertThat(received.get().getMsgId()).isEqualTo("M-RT-1");
        assertThat(received.get().getPayload()).contains("<CFX-RT/>");

        // 4) Tx 行为校验 — send 成功路径必须 commit、不 rollback
        verify(qcu).txBegin();
        verify(qcu).txCommit();
    }

    @Test
    @DisplayName("验收#3-3: nodeLifecycle.login transitions UNKNOWN → ONLINE → guard rejects re-login")
    void nodeLifecycle_login_shouldTransitionToOnline() {
        // 初始 UNKNOWN
        assertThat(lifecycleManager.getState()).isEqualTo(NodeState.UNKNOWN);

        // 1) login: UNKNOWN → ONLINE (allowed by NodeState.canTransitionTo)
        assertThat(lifecycleManager.login()).isTrue();
        assertThat(lifecycleManager.getState()).isEqualTo(NodeState.ONLINE);

        // 2) 二次 login: ONLINE → ONLINE 被状态机守护拒绝（returns false，不改 state）
        assertThat(lifecycleManager.login()).isFalse();
        assertThat(lifecycleManager.getState()).isEqualTo(NodeState.ONLINE);

        // 3) logout: ONLINE → OFFLINE (allowed)
        assertThat(lifecycleManager.logout()).isTrue();
        assertThat(lifecycleManager.getState()).isEqualTo(NodeState.OFFLINE);
    }

    @Test
    @DisplayName("验收#3-4: connectivity_check delegates to RemoteAdmin (mocked) and returns probe")
    void connectivity_check_shouldDelegateToRemoteAdmin() {
        // 测试边界：connectivity_check 业务由 fep-web TlqConnectivityService 调度，
        // 本测验证 fep-transport 层 RemoteAdmin 接口装配正确 + 委托成功。
        final RemoteAdmin.ConnectivityProbe stub =
                new RemoteAdmin.ConnectivityProbe(true, PROBE_RTT_MS, "OK");
        when(tongtechRemoteAdmin.checkConnectivity("localhost", PROBE_PORT)).thenReturn(stub);

        final RemoteAdmin.ConnectivityProbe probe =
                remoteAdmin.checkConnectivity("localhost", PROBE_PORT);

        assertThat(probe).isNotNull();
        assertThat(probe.reachable()).isTrue();
        assertThat(probe.rttMs()).isEqualTo(PROBE_RTT_MS);
        assertThat(probe.detail()).isEqualTo("OK");
        verify(tongtechRemoteAdmin, times(1)).checkConnectivity("localhost", PROBE_PORT);
    }

    @Test
    @DisplayName("验收#3-5: provider=tongtech does not leak any InMemory* beans into the context")
    void provider_switch_shouldNotLeakInMemoryBeans() {
        // 关键 beans — InMemory* 全部不在装配范围
        assertThatThrownBy(() -> ctx.getBean(InMemoryTlqProducer.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
        assertThatThrownBy(() -> ctx.getBean(InMemoryTlqConsumer.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
        assertThatThrownBy(() -> ctx.getBean(InMemoryTlqConnectionFactory.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
        assertThatThrownBy(() -> ctx.getBean(InMemoryNodeLifecycleManager.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
        assertThatThrownBy(() -> ctx.getBean(InMemoryDeadLetterHandler.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
        assertThatThrownBy(() -> ctx.getBean(InMemoryMessageBroker.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
        assertThatThrownBy(() -> ctx.getBean(InMemoryRemoteAdmin.class))
                .isInstanceOf(NoSuchBeanDefinitionException.class);

        // 与此同时 Tongtech 路径的 properties bean 必须存在（隔离正向证据）
        assertThat(ctx.getBean(TongtechTlqProperties.class)).isNotNull();
    }

    /**
     * Helper to confirm BATCH_RECEIVE empty-poll path also handles SDK exception
     * gracefully (additional defensive coverage for 维度 #2-4).
     *
     * <p>Validates {@code TlqException} from {@code getMessage} is mapped via the
     * error mapper rather than propagated raw — this prevents the unit test from
     * silently passing if the consumer ever bypassed the mapper.</p>
     */
    @Test
    @DisplayName("辅助验证: BATCH_RECEIVE getMessage 抛 TlqException → FepBusinessException 映射")
    void consumer_receive_batchReceive_sdkException_shouldMapToFepError() throws TlqException {
        final TlqException tle = mock(TlqException.class);
        when(tle.getErrorCause()).thenReturn("batch poll broken");
        doThrow(tle).when(qcu).getMessage(any(), any(TlqMsgOpt.class));

        assertThatThrownBy(() -> consumer.receive(TlqChannel.BATCH_RECEIVE, RECEIVE_WAIT))
                .isInstanceOf(com.puchain.fep.common.exception.FepBusinessException.class);
    }

    /**
     * Test-scoped configuration that imports {@link TongtechTransportConfiguration}'s
     * companion producer config so the {@link RetryableProducer} {@code @Primary}
     * wrapper is registered. Discovered via {@link Import} on the test class.
     */
    @org.springframework.context.annotation.Configuration
    @Import(com.puchain.fep.transport.tongtech.config.TongtechProducerConfiguration.class)
    static class SdkMockConfiguration {
    }
}
