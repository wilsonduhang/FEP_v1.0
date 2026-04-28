package com.puchain.fep.transport.tongtech.adapter;

import com.puchain.fep.transport.model.TlqChannel;
import com.puchain.fep.transport.support.QueueNameResolver.QueueType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link TongtechChannelMapper}.
 *
 * <p>Covers PRD §3.1.2 channel-to-queue mapping rules:</p>
 * <ul>
 *   <li>{@code REALTIME_SEND} / {@code BATCH_SEND} → corresponding {@code *_SEND} queue type</li>
 *   <li>{@code REALTIME_RECEIVE} / {@code BATCH_RECEIVE} → corresponding {@code *_DEST} queue type
 *       (receivers consume from the destination queue)</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class TongtechChannelMapperTest {

    private final TongtechChannelMapper mapper = new TongtechChannelMapper();

    @Test
    @DisplayName("REALTIME_SEND maps to REALTIME_SEND queue type (PRD §3.1.2)")
    void realtimeSend_mapsToRealtimeSendQueueType() {
        assertThat(mapper.toQueueType(TlqChannel.REALTIME_SEND))
                .isEqualTo(QueueType.REALTIME_SEND);
    }

    @Test
    @DisplayName("BATCH_SEND maps to BATCH_SEND queue type (PRD §3.1.2)")
    void batchSend_mapsToBatchSendQueueType() {
        assertThat(mapper.toQueueType(TlqChannel.BATCH_SEND))
                .isEqualTo(QueueType.BATCH_SEND);
    }

    @Test
    @DisplayName("REALTIME_RECEIVE maps to REALTIME_DEST queue type (receiver consumes destination)")
    void realtimeReceive_mapsToRealtimeDestQueueType() {
        assertThat(mapper.toQueueType(TlqChannel.REALTIME_RECEIVE))
                .isEqualTo(QueueType.REALTIME_DEST);
    }

    @Test
    @DisplayName("BATCH_RECEIVE maps to BATCH_DEST queue type (receiver consumes destination)")
    void batchReceive_mapsToBatchDestQueueType() {
        assertThat(mapper.toQueueType(TlqChannel.BATCH_RECEIVE))
                .isEqualTo(QueueType.BATCH_DEST);
    }
}
