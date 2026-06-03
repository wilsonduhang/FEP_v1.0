package com.puchain.fep.web.callback.credential.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link OAuth2ClientCredentialsClient} 集成测试 — 使用 JDK 内置 {@link HttpServer}（无外部依赖，
 * 沿用项目 {@code CallbackHttpClientTest} 既有 stub 栈，不引入 WireMock）。
 *
 * <p>验证 RFC 6749 §4.4 Client Credentials Grant：200 成功解析 + form body / Basic auth 装配，
 * 5xx → {@link OAuth2RetryableException}，401/403 → {@link OAuth2InvalidCredentialException}。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class OAuth2ClientCredentialsClientIT {

    private HttpServer server;
    private int port;
    private OAuth2ClientCredentialsClient client;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
        server.start();
        client = new OAuth2ClientCredentialsClient(new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    private String tokenUrl(final String path) {
        return "http://127.0.0.1:" + port + path;
    }

    @Test
    void fetchTokenViaClientCredentialsGrant() throws Exception {
        final AtomicReference<String> capturedBody = new AtomicReference<>();
        final AtomicReference<String> capturedAuth = new AtomicReference<>();
        final AtomicReference<String> capturedContentType = new AtomicReference<>();
        server.createContext("/token", exchange -> {
            capturedAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            capturedContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            capturedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            final byte[] resp = "{\"access_token\":\"tok-xyz\",\"expires_in\":3600,\"token_type\":\"Bearer\"}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });

        final OAuth2TokenResponse response = client.fetchToken(
                tokenUrl("/token"), "client-id", "client-secret", "read");

        assertThat(response.accessToken()).isEqualTo("tok-xyz");
        assertThat(response.expiresIn()).isEqualTo(3600);
        assertThat(capturedBody.get()).contains("grant_type=client_credentials");
        assertThat(capturedBody.get()).contains("scope=read");
        assertThat(capturedContentType.get()).isEqualTo("application/x-www-form-urlencoded");
        // RFC 6749 §2.3.1 — client_id:client_secret via HTTP Basic auth header.
        assertThat(capturedAuth.get()).startsWith("Basic ");
        // Credentials must NOT leak into the form body when Basic auth is used.
        assertThat(capturedBody.get()).doesNotContain("client_secret=");
    }

    @Test
    void fetchTokenWithoutScopeOmitsScopeParam() throws Exception {
        final AtomicReference<String> capturedBody = new AtomicReference<>();
        server.createContext("/token", exchange -> {
            capturedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            final byte[] resp = "{\"access_token\":\"t\",\"expires_in\":60,\"token_type\":\"Bearer\"}"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, resp.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(resp);
            }
        });

        final OAuth2TokenResponse response = client.fetchToken(tokenUrl("/token"), "x", "y", null);

        assertThat(response.accessToken()).isEqualTo("t");
        assertThat(capturedBody.get()).doesNotContain("scope=");
    }

    @Test
    void fetch5xxThrowsRetryableException() {
        server.createContext("/token", exchange -> {
            exchange.sendResponseHeaders(503, -1);
            exchange.close();
        });

        assertThatThrownBy(() -> client.fetchToken(tokenUrl("/token"), "x", "y", ""))
                .isInstanceOf(OAuth2RetryableException.class);
    }

    @Test
    void fetch401ThrowsNonRetryableException() {
        server.createContext("/token", exchange -> {
            exchange.sendResponseHeaders(401, -1);
            exchange.close();
        });

        assertThatThrownBy(() -> client.fetchToken(tokenUrl("/token"), "x", "y", ""))
                .isInstanceOf(OAuth2InvalidCredentialException.class);
    }

    @Test
    void fetchConnectionFailureThrowsRetryableException() {
        // Stop server so the port is closed → IOException → retryable.
        server.stop(0);
        server = null;

        assertThatThrownBy(() -> client.fetchToken(tokenUrl("/token"), "x", "y", ""))
                .isInstanceOf(OAuth2RetryableException.class);
    }
}
