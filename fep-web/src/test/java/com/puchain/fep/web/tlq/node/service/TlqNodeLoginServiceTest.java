package com.puchain.fep.web.tlq.node.service;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.FepConstants;
import com.puchain.fep.converter.model.CfxMessage;
import com.puchain.fep.converter.pipeline.EncodeResult;
import com.puchain.fep.converter.pipeline.MessageEncoder;
import com.puchain.fep.converter.pipeline.MessagePipelineOptions;
import com.puchain.fep.transport.api.NodeLifecycleManager;
import com.puchain.fep.transport.api.SendResult;
import com.puchain.fep.transport.api.TlqProducer;
import com.puchain.fep.transport.model.TlqChannel;
import com.puchain.fep.transport.model.TlqMessage;
import com.puchain.fep.web.outbound.consumer.BodyMsgIdGenerator;
import com.puchain.fep.web.tlq.node.domain.TlqNode;
import com.puchain.fep.web.tlq.node.domain.TlqNodeRole;
import com.puchain.fep.web.tlq.node.domain.TlqNodeStatus;
import com.puchain.fep.web.tlq.node.repository.TlqNodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TlqNodeLoginService} — verifies the 9006 / 9008
 * orchestration assembles the correct CFX message, calls the producer, and
 * advances the lifecycle state machine on success.
 *
 * <p>Critical assertions: {@code captor.getValue().getPayload()} contains the
 * required HEAD/MSG xml fragments — guards against silent field-binding bugs
 * that mere {@code verify(encoder).encode(any())} would miss (P1c T7 v1c
 * B-P0-4 hardening).</p>
 *
 * <p>R-1 (2026-05-06): added 2 cases verifying CommonHead.MsgId conforms to
 * PRD v1.3 §3.1.3 全数字格式 ({@code \d{20}}) since 9006/9008 装配 swapped
 * from {@link com.puchain.fep.common.util.IdGenerator#uuid20()} (base36) to
 * {@link BodyMsgIdGenerator}. See ADR
 * {@code docs/decisions/2026-05-06-bodymsgid-vs-uuid20-rationale.md}.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class TlqNodeLoginServiceTest {

    /** Empirical real broker password (mirrors test config). */
    private static final String TEST_PASSWORD = "BrokerPwd-9006";
    /** 14-char src node code (passes CommonHead.SrcNode length validation). */
    private static final String TEST_SRC_NODE = "B1234567890123";
    /** HNDEMP fixed dest. */
    private static final String HNDEMP_NODE = FepConstants.HNDEMP_NODE_CODE;
    private static final String NODE_ID = "node-x-001";

    /**
     * Sample 20-char all-digit MsgId (PRD §3.1.3 format: yyyyMMddHHmmss + 6-digit seq).
     * Used to stub {@link BodyMsgIdGenerator#generate()} in tests so assertions can
     * verify the value flows into CommonHead.MsgId unchanged. R-1 (2026-05-06).
     */
    private static final String SAMPLE_MSG_ID_DIGITS_20 = "20260507103200000001";

    private NodeLifecycleManager lifecycle;
    private TlqProducer producer;
    private MessageEncoder encoder;
    private TlqNodeRepository nodeRepository;
    private BodyMsgIdGenerator bodyMsgIdGenerator;
    private TlqNodeLoginService service;

    /** Shared encoded payload returned by the encoder mock — kept simple so assertions can grep substrings. */
    private static final String FAKE_ENCODED_PAYLOAD =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<CFX>"
            + "<HEAD>"
            + "<MsgNo>9006</MsgNo>"
            + "<DesNode>" + HNDEMP_NODE + "</DesNode>"
            + "<SrcNode>" + TEST_SRC_NODE + "</SrcNode>"
            + "</HEAD>"
            + "<MSG>"
            + "<RealHead9006>"
            + "<SendOrgCode>" + TEST_SRC_NODE + "</SendOrgCode>"
            + "</RealHead9006>"
            + "<LoginRequest9006>"
            + "<Password>" + TEST_PASSWORD + "</Password>"
            + "</LoginRequest9006>"
            + "</MSG>"
            + "</CFX>";

    /**
     * Head-only 9005 心跳 payload（无 body / 仅 RealHead9005）— 供 heartbeat 测试断言
     * {@code .contains("<RealHead9005>")} + {@code .doesNotContain("LoginRequest")}，
     * 防 head-only 退化为含 body 假绿（P4-MSG-O v0.2 评审 advisory 锚点）。
     */
    private static final String FAKE_ENCODED_PAYLOAD_9005 =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<CFX>"
            + "<HEAD>"
            + "<MsgNo>9005</MsgNo>"
            + "<DesNode>" + HNDEMP_NODE + "</DesNode>"
            + "<SrcNode>" + TEST_SRC_NODE + "</SrcNode>"
            + "</HEAD>"
            + "<MSG>"
            + "<RealHead9005>"
            + "<SendOrgCode>" + TEST_SRC_NODE + "</SendOrgCode>"
            + "</RealHead9005>"
            + "</MSG>"
            + "</CFX>";

    @BeforeEach
    void setUp() {
        lifecycle = mock(NodeLifecycleManager.class);
        producer = mock(TlqProducer.class);
        encoder = mock(MessageEncoder.class);
        nodeRepository = mock(TlqNodeRepository.class);
        bodyMsgIdGenerator = mock(BodyMsgIdGenerator.class);
        service = new TlqNodeLoginService(lifecycle, producer, encoder, nodeRepository,
                TEST_SRC_NODE, TEST_PASSWORD, bodyMsgIdGenerator);

        when(encoder.encode(any(CfxMessage.class), any(MessagePipelineOptions.class)))
                .thenReturn(new EncodeResult(FAKE_ENCODED_PAYLOAD, false, false));
        when(bodyMsgIdGenerator.generate()).thenReturn(SAMPLE_MSG_ID_DIGITS_20);
    }

    private TlqNode existingNode() {
        TlqNode node = new TlqNode();
        node.setNodeId(NODE_ID);
        node.setNodeName("primary");
        node.setNodeRole(TlqNodeRole.MASTER_PRODUCER);
        node.setHostIp("10.0.0.1");
        node.setPort(20001);
        node.setNodeStatus(TlqNodeStatus.UNKNOWN);
        return node;
    }

    @Test
    @DisplayName("login: 装配 9006 → encode → producer.send → lifecycle.login，TlqMessage 走 REALTIME_SEND 通道")
    void login_send9006_thenLifecycleLogin() {
        when(nodeRepository.findById(NODE_ID)).thenReturn(Optional.of(existingNode()));
        when(producer.send(any(TlqMessage.class))).thenReturn(SendResult.ok("MSG-9006"));
        when(lifecycle.login()).thenReturn(true);

        boolean result = service.login(NODE_ID);

        assertThat(result).isTrue();

        // 1) encoder 被以 CfxMessage 调用
        ArgumentCaptor<CfxMessage> cfxCaptor = ArgumentCaptor.forClass(CfxMessage.class);
        verify(encoder).encode(cfxCaptor.capture(), any(MessagePipelineOptions.class));
        CfxMessage cfx = cfxCaptor.getValue();
        assertThat(cfx.getHead().getMsgNo()).isEqualTo("9006");
        assertThat(cfx.getHead().getDesNode()).isEqualTo(HNDEMP_NODE);
        assertThat(cfx.getHead().getSrcNode()).isEqualTo(TEST_SRC_NODE);
        assertThat(cfx.getBodies()).hasSize(2)
                .as("MSG 容器应含 RealHead9006 + LoginRequest9006 两个子元素");

        // 2) producer 拿到了 encode 后的 payload + REALTIME_SEND 通道
        ArgumentCaptor<TlqMessage> msgCaptor = ArgumentCaptor.forClass(TlqMessage.class);
        verify(producer).send(msgCaptor.capture());
        TlqMessage sent = msgCaptor.getValue();
        assertThat(sent.getChannel()).isEqualTo(TlqChannel.REALTIME_SEND);
        assertThat(sent.getPayload())
                .contains("<MsgNo>9006</MsgNo>")
                .contains("<DesNode>" + HNDEMP_NODE + "</DesNode>")
                .contains("<RealHead9006>")
                .contains("<SendOrgCode>" + TEST_SRC_NODE + "</SendOrgCode>")
                .contains("<LoginRequest9006>")
                .contains("<Password>" + TEST_PASSWORD + "</Password>");

        // 3) lifecycle 被调
        verify(lifecycle).login();
    }

    @Test
    @DisplayName("login: producer.send 失败时不调 lifecycle.login")
    void login_sendFails_shouldNotInvokeLifecycle() {
        when(nodeRepository.findById(NODE_ID)).thenReturn(Optional.of(existingNode()));
        when(producer.send(any(TlqMessage.class)))
                .thenReturn(SendResult.fail("MSG-9006", "queue full"));

        boolean result = service.login(NODE_ID);

        assertThat(result).isFalse();
        verify(lifecycle, never()).login();
    }

    @Test
    @DisplayName("login: 节点不存在抛 BIZ_5015")
    void login_nonExistentNode_throwsBiz5015() {
        when(nodeRepository.findById("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login("ghost"))
                .isInstanceOf(FepBusinessException.class)
                .extracting(e -> ((FepBusinessException) e).getErrorCode())
                .isEqualTo(FepErrorCode.BIZ_5015);

        verify(producer, never()).send(any());
        verify(lifecycle, never()).login();
    }

    @Test
    @DisplayName("logout: 装配 9008 → encode → producer.send → lifecycle.logout")
    void logout_send9008_thenLifecycleLogout() {
        when(nodeRepository.findById(NODE_ID)).thenReturn(Optional.of(existingNode()));
        when(producer.send(any(TlqMessage.class))).thenReturn(SendResult.ok("MSG-9008"));
        when(lifecycle.logout()).thenReturn(true);

        boolean result = service.logout(NODE_ID);

        assertThat(result).isTrue();

        ArgumentCaptor<CfxMessage> cfxCaptor = ArgumentCaptor.forClass(CfxMessage.class);
        verify(encoder).encode(cfxCaptor.capture(), any(MessagePipelineOptions.class));
        CfxMessage cfx = cfxCaptor.getValue();
        assertThat(cfx.getHead().getMsgNo()).isEqualTo("9008");
        assertThat(cfx.getHead().getDesNode()).isEqualTo(HNDEMP_NODE);
        assertThat(cfx.getBodies()).hasSize(2);

        verify(lifecycle, times(1)).logout();
    }

    @Test
    @DisplayName("logout: producer.send 失败时不调 lifecycle.logout")
    void logout_sendFails_shouldNotInvokeLifecycle() {
        when(nodeRepository.findById(NODE_ID)).thenReturn(Optional.of(existingNode()));
        when(producer.send(any(TlqMessage.class)))
                .thenReturn(SendResult.fail("MSG-9008", "broker offline"));

        boolean result = service.logout(NODE_ID);

        assertThat(result).isFalse();
        verify(lifecycle, never()).logout();
    }

    @Test
    @DisplayName("logout: 节点不存在抛 BIZ_5015")
    void logout_nonExistentNode_throwsBiz5015() {
        when(nodeRepository.findById("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.logout("ghost"))
                .isInstanceOf(FepBusinessException.class)
                .extracting(e -> ((FepBusinessException) e).getErrorCode())
                .isEqualTo(FepErrorCode.BIZ_5015);
    }

    // ---------- R-1 (2026-05-06) PRD §3.1.3 合规验证 ----------

    @Test
    @DisplayName("R-1: 9006 login CommonHead.MsgId 必须 20 字符全数字 (PRD v1.3 §3.1.3)")
    void login9006_msgId_shouldBeAllDigits20Chars_perPrd313() {
        when(nodeRepository.findById(NODE_ID)).thenReturn(Optional.of(existingNode()));
        when(producer.send(any(TlqMessage.class))).thenReturn(SendResult.ok("MSG-9006"));
        when(lifecycle.login()).thenReturn(true);

        service.login(NODE_ID);

        ArgumentCaptor<CfxMessage> cfxCaptor = ArgumentCaptor.forClass(CfxMessage.class);
        verify(encoder).encode(cfxCaptor.capture(), any(MessagePipelineOptions.class));
        String msgId = cfxCaptor.getValue().getHead().getMsgId();

        assertThat(msgId)
                .as("PRD v1.3 §3.1.3 强制 CommonHead.MsgId 20 字符全数字 (R-1 swap from uuid20 base36)")
                .hasSize(20)
                .matches("\\d{20}")
                .isEqualTo(SAMPLE_MSG_ID_DIGITS_20);

        // 验证 R-1 swap：bodyMsgIdGenerator 被调用，IdGenerator.uuid20() 不再用于 CommonHead.MsgId
        verify(bodyMsgIdGenerator, times(1)).generate();
    }

    @Test
    @DisplayName("R-1: 9008 logout CommonHead.MsgId 必须 20 字符全数字 (PRD v1.3 §3.1.3)")
    void logout9008_msgId_shouldBeAllDigits20Chars_perPrd313() {
        when(nodeRepository.findById(NODE_ID)).thenReturn(Optional.of(existingNode()));
        when(producer.send(any(TlqMessage.class))).thenReturn(SendResult.ok("MSG-9008"));
        when(lifecycle.logout()).thenReturn(true);

        service.logout(NODE_ID);

        ArgumentCaptor<CfxMessage> cfxCaptor = ArgumentCaptor.forClass(CfxMessage.class);
        verify(encoder).encode(cfxCaptor.capture(), any(MessagePipelineOptions.class));
        String msgId = cfxCaptor.getValue().getHead().getMsgId();

        assertThat(msgId)
                .as("PRD v1.3 §3.1.3 强制 CommonHead.MsgId 20 字符全数字 (R-1 swap from uuid20 base36)")
                .hasSize(20)
                .matches("\\d{20}")
                .isEqualTo(SAMPLE_MSG_ID_DIGITS_20);

        verify(bodyMsgIdGenerator, times(1)).generate();
    }

    // ---------- P4-MSG-O: 9005 心跳发送 (heartbeat head-only) ----------

    @Test
    @DisplayName("heartbeat: 装配 head-only 9005 → encode(sign=false) → send，return true，不调 lifecycle")
    void heartbeat_send9005HeadOnly_shouldReturnTrue_withoutLifecycle() {
        when(nodeRepository.findById(NODE_ID)).thenReturn(Optional.of(existingNode()));
        when(encoder.encode(any(CfxMessage.class), any(MessagePipelineOptions.class)))
                .thenReturn(new EncodeResult(FAKE_ENCODED_PAYLOAD_9005, false, false));
        when(producer.send(any(TlqMessage.class))).thenReturn(SendResult.ok("MSG-9005"));

        boolean result = service.heartbeat(NODE_ID);

        assertThat(result).isTrue();

        // 1) encoder 被以 head-only CfxMessage 调用：MsgNo=9005 + 仅 1 个 MSG 子元素 (RealHead9005，无 body)
        ArgumentCaptor<CfxMessage> cfxCaptor = ArgumentCaptor.forClass(CfxMessage.class);
        verify(encoder).encode(cfxCaptor.capture(), any(MessagePipelineOptions.class));
        CfxMessage cfx = cfxCaptor.getValue();
        assertThat(cfx.getHead().getMsgNo()).isEqualTo("9005");
        assertThat(cfx.getHead().getDesNode()).isEqualTo(HNDEMP_NODE);
        assertThat(cfx.getHead().getSrcNode()).isEqualTo(TEST_SRC_NODE);
        assertThat(cfx.getBodies())
                .as("head-only 心跳：MSG 仅 RealHead9005 一个子元素（对比 9006 的 hasSize(2)）")
                .hasSize(1);

        // 2) producer 拿到 head-only payload + REALTIME_SEND 通道，且不含 body
        ArgumentCaptor<TlqMessage> msgCaptor = ArgumentCaptor.forClass(TlqMessage.class);
        verify(producer).send(msgCaptor.capture());
        TlqMessage sent = msgCaptor.getValue();
        assertThat(sent.getChannel()).isEqualTo(TlqChannel.REALTIME_SEND);
        assertThat(sent.getPayload())
                .contains("<MsgNo>9005</MsgNo>")
                .contains("<RealHead9005>")
                .doesNotContain("LoginRequest");

        // 3) 心跳是 keepalive：不调 lifecycle 状态机（区别于 login/logout）
        verify(lifecycle, never()).login();
        verify(lifecycle, never()).logout();
    }

    @Test
    @DisplayName("heartbeat: producer.send 失败时 return false，仍不调 lifecycle")
    void heartbeat_sendFails_shouldReturnFalse_withoutLifecycle() {
        when(nodeRepository.findById(NODE_ID)).thenReturn(Optional.of(existingNode()));
        when(producer.send(any(TlqMessage.class)))
                .thenReturn(SendResult.fail("MSG-9005", "broker offline"));

        boolean result = service.heartbeat(NODE_ID);

        assertThat(result).isFalse();
        verify(lifecycle, never()).login();
        verify(lifecycle, never()).logout();
    }

    @Test
    @DisplayName("heartbeat: 节点不存在抛 BIZ_5015，不发送")
    void heartbeat_nonExistentNode_throwsBiz5015() {
        when(nodeRepository.findById("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.heartbeat("ghost"))
                .isInstanceOf(FepBusinessException.class)
                .extracting(e -> ((FepBusinessException) e).getErrorCode())
                .isEqualTo(FepErrorCode.BIZ_5015);

        verify(producer, never()).send(any());
        verify(lifecycle, never()).login();
        verify(lifecycle, never()).logout();
    }
}
