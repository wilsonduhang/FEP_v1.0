package com.puchain.fep.web.realtime;

import com.puchain.fep.web.auth.jwt.JwtTokenProvider;
import com.puchain.fep.web.callback.notification.event.InAppNotificationCreatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Dashboard WebSocket 通知推送端到端 IT：认证会话 → 发布
 * {@link InAppNotificationCreatedEvent} → 客户端实时收到 notification 帧（B-8 Task 2）。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "fep.transport.provider=mock",
        "fep.collector.scheduling.enabled=false",
        "fep.outbound.queue.poll-interval-ms=99999",
        "fep.outbound.queue.poll-initial-delay-ms=99999",
        "fep.callback.poll-interval-ms=600000",
        "fep.callback.poll-initial-delay-ms=600000",
        "management.health.redis.enabled=false"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Dashboard WebSocket push IT — event → realtime notification frame")
class DashboardNotificationPushIT {

    @LocalServerPort
    private int port;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private WebSocketSessionRegistry registry;

    private URI wsUri() {
        return URI.create("ws://localhost:" + port + DashboardWebSocketConfiguration.DASHBOARD_WS_PATH);
    }

    /** 捕获服务端推送帧的客户端处理器。 */
    private static final class CapturingClientHandler extends TextWebSocketHandler {
        private final String authFrame;
        private final List<String> received = new CopyOnWriteArrayList<>();
        private final CountDownLatch messageLatch = new CountDownLatch(1);

        CapturingClientHandler(final String authFrame) {
            this.authFrame = authFrame;
        }

        @Override
        public void afterConnectionEstablished(final WebSocketSession session) throws Exception {
            session.sendMessage(new TextMessage(authFrame));
        }

        @Override
        protected void handleTextMessage(final WebSocketSession session, final TextMessage message) {
            received.add(message.getPayload());
            messageLatch.countDown();
        }

        @Override
        public void afterConnectionClosed(final WebSocketSession session, final CloseStatus status) {
            // no-op
        }
    }

    @Test
    void authenticatedSession_receivesNotificationPush() throws Exception {
        final String userId = "user-push-it";
        final String token = jwtTokenProvider.createAccessToken(userId, "acct", List.of("ADMIN"));
        final CapturingClientHandler client =
                new CapturingClientHandler("{\"type\":\"auth\",\"token\":\"" + token + "\"}");

        final WebSocketSession session =
                new StandardWebSocketClient().execute(client, wsUri().toString()).get(5, TimeUnit.SECONDS);

        // 等待服务端完成首帧认证 + 注册（轮询注册表，避免在注册前就发布事件）。
        awaitRegistered();

        eventPublisher.publishEvent(
                new InAppNotificationCreatedEvent(userId, "notif-it-1", Instant.now()));

        final boolean got = client.messageLatch.await(5, TimeUnit.SECONDS);
        assertThat(got).as("client must receive a realtime notification frame").isTrue();
        assertThat(client.received).anyMatch(p ->
                p.contains("\"type\":\"notification\"") && p.contains("notif-it-1"));
        session.close();
    }

    private void awaitRegistered() throws InterruptedException {
        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (registry.sessionCount() > 0) {
                return;
            }
            TimeUnit.MILLISECONDS.sleep(50);
        }
    }
}
