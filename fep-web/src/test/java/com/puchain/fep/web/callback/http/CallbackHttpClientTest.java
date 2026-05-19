package com.puchain.fep.web.callback.http;

import com.puchain.fep.web.submission.outputinterface.domain.InterfaceAuthType;
import com.puchain.fep.web.submission.outputinterface.domain.SubOutputInterface;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link CallbackHttpClient} 单元测试 — 使用 JDK 内置 HttpServer（无依赖）
 * 验证 2xx/non-2xx/连接拒绝/TOKEN 鉴权头四种场景。
 *
 * @author FEP Team
 * @since 1.0.0
 */
class CallbackHttpClientTest {

    private HttpServer server;
    private int port;
    private CallbackHttpClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
        server.start();
        client = new CallbackHttpClient();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    private SubOutputInterface makeInterface(final String path, final InterfaceAuthType authType) {
        final SubOutputInterface iface = new SubOutputInterface();
        iface.setInterfaceId("test-if-01");
        iface.setInterfaceUrl("http://127.0.0.1:" + port + path);
        iface.setAuthType(authType);
        iface.setTimeoutSeconds(5);
        return iface;
    }

    @Test
    void post_returns2xx_shouldSucceed() throws Exception {
        server.createContext("/ok", exchange -> {
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().close();
        });

        final CallbackResult result = client.post(makeInterface("/ok", InterfaceAuthType.NONE), "{\"k\":\"v\"}");

        assertThat(result.success()).isTrue();
        assertThat(result.statusCode()).isEqualTo(200);
        assertThat(result.error()).isNull();
    }

    @Test
    void post_returns500_shouldFail() throws Exception {
        server.createContext("/err", exchange -> {
            final byte[] body = "server error".getBytes();
            exchange.sendResponseHeaders(500, body.length);
            try (final OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        final CallbackResult result = client.post(makeInterface("/err", InterfaceAuthType.NONE), "{}");

        assertThat(result.success()).isFalse();
        assertThat(result.statusCode()).isEqualTo(500);
        assertThat(result.error()).isNotNull();
    }

    @Test
    void post_connectionRefused_shouldReturnFailureWithoutThrowing() {
        // Stop server so the port is closed
        server.stop(0);
        server = null;

        final SubOutputInterface iface = new SubOutputInterface();
        iface.setInterfaceId("test-refused");
        iface.setInterfaceUrl("http://127.0.0.1:" + port + "/closed");
        iface.setAuthType(InterfaceAuthType.NONE);
        iface.setTimeoutSeconds(3);

        final CallbackResult result = client.post(iface, "{}");

        assertThat(result.success()).isFalse();
        assertThat(result.statusCode()).isEqualTo(0);
        assertThat(result.error()).isNotNull();
    }

    @Test
    void post_tokenAuth_shouldIncludeAuthorizationHeader() throws Exception {
        final AtomicReference<String> capturedAuthHeader = new AtomicReference<>();
        final AtomicInteger requestCount = new AtomicInteger(0);

        server.createContext("/auth", exchange -> {
            capturedAuthHeader.set(exchange.getRequestHeaders().getFirst("Authorization"));
            requestCount.incrementAndGet();
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().close();
        });

        final CallbackResult result = client.post(makeInterface("/auth", InterfaceAuthType.TOKEN), "{\"data\":\"x\"}");

        assertThat(result.success()).isTrue();
        assertThat(requestCount.get()).isEqualTo(1);
        // P1 scaffold: Authorization header must be present for TOKEN auth type
        // Actual credential value is Phase 2 §5.5.3; P1 sets a scaffold placeholder
        assertThat(capturedAuthHeader.get()).isNotNull();
    }
}
