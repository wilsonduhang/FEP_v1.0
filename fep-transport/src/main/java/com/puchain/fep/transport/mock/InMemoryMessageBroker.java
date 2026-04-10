package com.puchain.fep.transport.mock;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import com.puchain.fep.transport.api.MessageListener;
import com.puchain.fep.transport.model.TlqChannel;
import com.puchain.fep.transport.model.TlqMessage;

/**
 * In-memory message broker for dev/test environments.
 *
 * <p>Maintains per-channel blocking queues and optional push-mode listeners.
 * NOT a Spring {@code @Component} — created by AutoConfiguration.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class InMemoryMessageBroker {

    private final Map<TlqChannel, BlockingQueue<TlqMessage>> queues = new ConcurrentHashMap<>();
    private final Map<TlqChannel, MessageListener> listeners = new ConcurrentHashMap<>();

    /**
     * Publish a message to its channel queue and notify the listener if registered.
     *
     * @param message the message to publish, must not be {@code null}
     */
    public void publish(final TlqMessage message) {
        Objects.requireNonNull(message, "message must not be null");
        final TlqChannel channel = message.getChannel();
        queueFor(channel).add(message);

        final MessageListener listener = listeners.get(channel);
        if (listener != null) {
            listener.onMessage(message);
        }
    }

    /**
     * Poll a message from the given channel with a timeout.
     *
     * @param channel   the channel to poll from, must not be {@code null}
     * @param timeoutMs maximum wait time in milliseconds
     * @return the polled message, or {@code null} if timed out
     */
    public TlqMessage poll(final TlqChannel channel, final long timeoutMs) {
        Objects.requireNonNull(channel, "channel must not be null");
        try {
            return queueFor(channel).poll(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    /**
     * Register a push-mode listener for the given channel.
     *
     * @param channel  the channel to listen on, must not be {@code null}
     * @param listener the listener callback, must not be {@code null}
     */
    public void addListener(final TlqChannel channel, final MessageListener listener) {
        Objects.requireNonNull(channel, "channel must not be null");
        Objects.requireNonNull(listener, "listener must not be null");
        listeners.put(channel, listener);
    }

    /**
     * Remove the push-mode listener for the given channel.
     *
     * @param channel the channel to stop listening on, must not be {@code null}
     */
    public void removeListener(final TlqChannel channel) {
        Objects.requireNonNull(channel, "channel must not be null");
        listeners.remove(channel);
    }

    private BlockingQueue<TlqMessage> queueFor(final TlqChannel channel) {
        return queues.computeIfAbsent(channel, k -> new LinkedBlockingQueue<>());
    }
}
