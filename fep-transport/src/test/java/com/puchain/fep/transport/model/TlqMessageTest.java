package com.puchain.fep.transport.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TLQ 消息模型单元测试。
 *
 * <p>覆盖 TlqChannel、TlqMessageAttributes、TlqMessage、NodeState 四个类。</p>
 */
class TlqMessageTest {

    // ---- TlqChannel ----

    @Test
    void tlqChannel_realtimeSend_shouldHavePort20001() {
        assertEquals(20001, TlqChannel.REALTIME_SEND.getPort());
        assertTrue(TlqChannel.REALTIME_SEND.isRealtime());
        assertTrue(TlqChannel.REALTIME_SEND.isSend());
    }

    @Test
    void tlqChannel_batchReceive_shouldHavePort20002() {
        assertEquals(20002, TlqChannel.BATCH_RECEIVE.getPort());
        assertFalse(TlqChannel.BATCH_RECEIVE.isRealtime());
        assertFalse(TlqChannel.BATCH_RECEIVE.isSend());
    }

    // ---- TlqMessageAttributes ----

    @Test
    void realtimeAttributes_shouldBeNonPersistentWith30sExpiry() {
        TlqMessageAttributes attrs = TlqMessageAttributes.forRealtime("msg-001");

        assertEquals("msg-001", attrs.getMsgId());
        assertFalse(attrs.isPersistence());
        assertEquals(30, attrs.getExpiry());
        assertFalse(attrs.isZip());
        assertFalse(attrs.isEncrypt());
    }

    @Test
    void batchAttributes_shouldBePersistentWithNoExpiry() {
        TlqMessageAttributes attrs = TlqMessageAttributes.forBatch("msg-002");

        assertEquals("msg-002", attrs.getMsgId());
        assertTrue(attrs.isPersistence());
        assertEquals(-1, attrs.getExpiry());
        assertFalse(attrs.isZip());
        assertFalse(attrs.isEncrypt());
    }

    // ---- TlqMessage ----

    @Test
    void tlqMessage_nullPayload_shouldThrowNpe() {
        TlqMessageAttributes attrs = TlqMessageAttributes.forRealtime("msg-003");
        assertThrows(NullPointerException.class,
                () -> new TlqMessage(null, attrs, TlqChannel.REALTIME_SEND));
    }

    @Test
    void tlqMessage_shouldExposeAllFields() {
        TlqMessageAttributes attrs = TlqMessageAttributes.forRealtime("msg-004");
        attrs.setZip(true);
        attrs.setEncrypt(true);
        attrs.setCorrMsgId("corr-001");

        TlqMessage msg = new TlqMessage("<xml/>", attrs, TlqChannel.BATCH_SEND);

        assertEquals("<xml/>", msg.getPayload());
        assertEquals("msg-004", msg.getMsgId());
        assertEquals(TlqChannel.BATCH_SEND, msg.getChannel());
        assertTrue(msg.getAttributes().isZip());
        assertTrue(msg.getAttributes().isEncrypt());
        assertEquals("corr-001", msg.getAttributes().getCorrMsgId());
        assertTrue(msg.toString().contains("msg-004"));
        assertTrue(msg.toString().contains("BATCH_SEND"));
    }

    // ---- NodeState ----

    @Test
    void nodeState_unknown_canTransitionToOnline() {
        assertTrue(NodeState.UNKNOWN.canTransitionTo(NodeState.ONLINE));
        assertFalse(NodeState.UNKNOWN.canTransitionTo(NodeState.OFFLINE));
    }

    @Test
    void nodeState_online_canTransitionToOffline() {
        assertTrue(NodeState.ONLINE.canTransitionTo(NodeState.OFFLINE));
        assertFalse(NodeState.ONLINE.canTransitionTo(NodeState.UNKNOWN));
    }

    @Test
    void nodeState_anyState_canTransitionToError() {
        for (NodeState state : NodeState.values()) {
            assertTrue(state.canTransitionTo(NodeState.ERROR),
                    state + " should be able to transition to ERROR");
        }
    }

    @Test
    void nodeState_offline_canTransitionToOnlineOrUnknown() {
        assertTrue(NodeState.OFFLINE.canTransitionTo(NodeState.ONLINE));
        assertTrue(NodeState.OFFLINE.canTransitionTo(NodeState.UNKNOWN));
        assertFalse(NodeState.OFFLINE.canTransitionTo(NodeState.OFFLINE));
    }

    @Test
    void nodeState_error_canOnlyTransitionToUnknownOrError() {
        assertTrue(NodeState.ERROR.canTransitionTo(NodeState.UNKNOWN));
        assertTrue(NodeState.ERROR.canTransitionTo(NodeState.ERROR));
        assertFalse(NodeState.ERROR.canTransitionTo(NodeState.ONLINE));
        assertFalse(NodeState.ERROR.canTransitionTo(NodeState.OFFLINE));
    }
}
