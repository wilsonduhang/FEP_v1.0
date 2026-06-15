package com.puchain.fep.web.realtime;

import com.puchain.fep.web.auth.jwt.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Dashboard WebSocket 首帧鉴权端到端 IT（真 {@link StandardWebSocketClient} 连接随机端口）。
 *
 * <p>验证：合法 JWT 首帧 → 连接保持；非法 JWT 首帧 → 服务端主动 close
 * （muzhou 2026-06-15 签字方案 a 首帧 token）。</p>
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
@DisplayName("Dashboard WebSocket auth IT — first-frame JWT accept/reject")
class DashboardWebSocketAuthIT {

    @LocalServerPort
    private int port;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private URI wsUri() {
        return URI.create("ws://localhost:" + port + DashboardWebSocketConfiguration.DASHBOARD_WS_PATH);
    }

    /** 捕获服务端 close 事件的客户端处理器。 */
    private static final class CapturingClientHandler extends TextWebSocketHandler {
        private final String authFrame;
        private final CountDownLatch closeLatch = new CountDownLatch(1);

        CapturingClientHandler(final String authFrame) {
            this.authFrame = authFrame;
        }

        @Override
        public void afterConnectionEstablished(final WebSocketSession session) throws Exception {
            session.sendMessage(new TextMessage(authFrame));
        }

        @Override
        public void afterConnectionClosed(final WebSocketSession session, final CloseStatus status) {
            closeLatch.countDown();
        }
    }

    @Test
    void validToken_keepsConnectionOpen() throws Exception {
        final String token = jwtTokenProvider.createAccessToken("user-it", "acct-it", List.of("ADMIN"));
        final CapturingClientHandler client =
                new CapturingClientHandler("{\"type\":\"auth\",\"token\":\"" + token + "\"}");

        final WebSocketSession session =
                new StandardWebSocketClient().execute(client, wsUri().toString()).get(5, TimeUnit.SECONDS);

        // 合法鉴权后连接不应在窗口内被服务端关闭。
        final boolean closed = client.closeLatch.await(2, TimeUnit.SECONDS);
        assertThat(closed).as("server must NOT close an authenticated session").isFalse();
        assertThat(session.isOpen()).isTrue();
        session.close();
    }

    @Test
    void invalidToken_isClosedByServer() throws Exception {
        final CapturingClientHandler client =
                new CapturingClientHandler("{\"type\":\"auth\",\"token\":\"not-a-valid-jwt\"}");

        new StandardWebSocketClient().execute(client, wsUri().toString()).get(5, TimeUnit.SECONDS);

        final boolean closed = client.closeLatch.await(5, TimeUnit.SECONDS);
        assertThat(closed).as("server must close a session that sent an invalid token").isTrue();
    }
}
