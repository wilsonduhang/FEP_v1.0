package com.puchain.fep.web.callback.http;

import com.puchain.fep.web.callback.credential.service.CallbackCredentialResolver;
import com.puchain.fep.web.callback.credential.service.CallbackCredentialResolver.AuthHeader;
import com.puchain.fep.web.submission.outputinterface.domain.SubOutputInterface;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link CallbackHttpClient#probe} 单测：凭证感知 HEAD 探测对鉴权头的注入与结果翻译。
 * 用 JDK {@link HttpServer} 作 mock 目标，{@link CallbackCredentialResolver} 用 Mockito。
 *
 * @author FEP Team
 * @since 1.0.0
 */
class CallbackHttpClientProbeTest {

    private HttpServer server;
    private final AtomicInteger status = new AtomicInteger(200);
    private List<String> seenAuthHeaders;
    private List<String> seenMethods;
    private String targetUrl;
    private CallbackCredentialResolver resolver;
    private CallbackHttpClient client;

    @BeforeEach
    void setUp() throws IOException {
        seenAuthHeaders = new CopyOnWriteArrayList<>();
        seenMethods = new CopyOnWriteArrayList<>();
        status.set(200);
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/probe", exchange -> {
            seenMethods.add(exchange.getRequestMethod());
            seenAuthHeaders.add(String.valueOf(exchange.getRequestHeaders().getFirst("X-Token")));
            exchange.sendResponseHeaders(status.get(), -1);
            exchange.close();
        });
        server.start();
        targetUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/probe";
        resolver = mock(CallbackCredentialResolver.class);
        client = new CallbackHttpClient(resolver);
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    private SubOutputInterface target() {
        final SubOutputInterface t = new SubOutputInterface();
        t.setInterfaceId("if-1");
        t.setInterfaceUrl(targetUrl);
        t.setTimeoutSeconds(5);
        return t;
    }

    @Test
    void probe_withAuthHeader_injectsHeaderAndReportsReachable() {
        when(resolver.resolveAuthHeader(any()))
                .thenReturn(Optional.of(new AuthHeader("X-Token", "secret-tok")));

        final CallbackProbeResult result = client.probe(target());

        assertThat(seenMethods).containsExactly("HEAD");
        assertThat(seenAuthHeaders).containsExactly("secret-tok");
        assertThat(result.reachable()).isTrue();
        assertThat(result.authApplied()).isTrue();
        assertThat(result.statusCode()).isEqualTo(200);
        assertThat(result.latencyMs()).isGreaterThanOrEqualTo(0L);
    }

    @Test
    void probe_noneAuth_sendsNoAuthHeader() {
        when(resolver.resolveAuthHeader(any())).thenReturn(Optional.empty());

        final CallbackProbeResult result = client.probe(target());

        assertThat(seenAuthHeaders).containsExactly("null");
        assertThat(result.authApplied()).isFalse();
        assertThat(result.reachable()).isTrue();
    }

    @Test
    void probe_authResolutionFails_returnsAuthErrorWithoutThrowing() {
        when(resolver.resolveAuthHeader(any()))
                .thenThrow(new IllegalStateException("decrypt boom"));

        final CallbackProbeResult result = client.probe(target());

        assertThat(result.reachable()).isFalse();
        assertThat(result.authApplied()).isFalse();
        assertThat(result.statusCode()).isZero();
        assertThat(result.message()).startsWith("auth:");
    }

    @Test
    void probe_nonReachableTarget_reportsIoFailure() {
        when(resolver.resolveAuthHeader(any())).thenReturn(Optional.empty());
        final SubOutputInterface t = target();
        t.setInterfaceUrl("http://127.0.0.1:1/dead");

        final CallbackProbeResult result = client.probe(t);

        assertThat(result.reachable()).isFalse();
        assertThat(result.statusCode()).isZero();
        assertThat(result.message()).startsWith("io:");
    }

    @Test
    void probe_non2xx_reportsStatusCode() {
        when(resolver.resolveAuthHeader(any())).thenReturn(Optional.empty());
        status.set(500);

        final CallbackProbeResult result = client.probe(target());

        assertThat(result.reachable()).isFalse();
        assertThat(result.statusCode()).isEqualTo(500);
        assertThat(result.message()).isEqualTo("http 500");
    }
}
