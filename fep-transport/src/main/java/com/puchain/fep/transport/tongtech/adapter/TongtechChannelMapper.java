package com.puchain.fep.transport.tongtech.adapter;

import com.puchain.fep.transport.model.TlqChannel;
import com.puchain.fep.transport.support.QueueNameResolver.QueueType;
import org.springframework.stereotype.Component;

/**
 * Maps {@link TlqChannel} to the corresponding
 * {@link com.puchain.fep.transport.support.QueueNameResolver.QueueType} per
 * PRD §3.1.2 queue naming conventions.
 *
 * <p>Mapping rules:</p>
 * <ul>
 *   <li>{@link TlqChannel#REALTIME_SEND} → {@link QueueType#REALTIME_SEND}</li>
 *   <li>{@link TlqChannel#BATCH_SEND} → {@link QueueType#BATCH_SEND}</li>
 *   <li>{@link TlqChannel#REALTIME_RECEIVE} → {@link QueueType#REALTIME_DEST}
 *       (receivers consume from the destination queue)</li>
 *   <li>{@link TlqChannel#BATCH_RECEIVE} → {@link QueueType#BATCH_DEST}</li>
 * </ul>
 *
 * <p>Registered as a Spring {@link Component} discovered by
 * {@code TongtechTransportConfiguration}'s component scan, which is gated by
 * {@code fep.transport.provider=tongtech}.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class TongtechChannelMapper {

    /**
     * Resolve the queue type that backs the given FEP channel.
     *
     * @param ch the FEP channel (non-null)
     * @return the corresponding queue type for queue-name resolution
     */
    public QueueType toQueueType(final TlqChannel ch) {
        return switch (ch) {
            case REALTIME_SEND    -> QueueType.REALTIME_SEND;
            case BATCH_SEND       -> QueueType.BATCH_SEND;
            case REALTIME_RECEIVE -> QueueType.REALTIME_DEST;
            case BATCH_RECEIVE    -> QueueType.BATCH_DEST;
        };
    }
}
