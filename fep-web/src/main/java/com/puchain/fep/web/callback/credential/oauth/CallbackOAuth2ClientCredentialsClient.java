package com.puchain.fep.web.callback.credential.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

/**
 * OAuth2 Client Credentials Grant 客户端（RFC 6749 §4.4）。
 *
 * <p>仅支持 client_credentials 授权类型：POST token endpoint，form body 含
 * {@code grant_type=client_credentials}（+ 可选 {@code scope}），client_id/client_secret 经
 * HTTP Basic auth 头传递（RFC 6749 §2.3.1），不入 form body 避免凭证泄漏。</p>
 *
 * <p>错误分类：401/403 → {@link CallbackOAuth2InvalidCredentialException}（不可重试）；
 * 其余非 200 / IO / 中断 → {@link CallbackOAuth2RetryableException}（可重试）。</p>
 *
 * <p>沿用项目既有 JDK {@link HttpClient} 栈（与 {@code CallbackHttpClient} 一致）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class CallbackOAuth2ClientCredentialsClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);
    private static final int HTTP_OK = 200;
    private static final int HTTP_UNAUTHORIZED = 401;
    private static final int HTTP_FORBIDDEN = 403;

    private final HttpClient http;
    private final ObjectMapper mapper;

    /**
     * @param mapper 共享 Jackson {@link ObjectMapper}（Spring 注入）
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring-managed shared ObjectMapper stored by reference per container contract")
    public CallbackOAuth2ClientCredentialsClient(final ObjectMapper mapper) {
        this.http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        this.mapper = mapper;
    }

    /**
     * 以 Client Credentials Grant 换取 access_token。
     *
     * @param tokenEndpoint token endpoint URL，非空
     * @param clientId      客户端标识，非空
     * @param clientSecret  客户端密钥，非空
     * @param scope         请求的 scope，可空 / 空串则省略
     * @return token endpoint 响应
     * @throws CallbackOAuth2InvalidCredentialException 凭证被拒（401/403），不可重试
     * @throws CallbackOAuth2RetryableException         5xx / 其它非 200 / IO / 中断，可重试
     */
    public CallbackOAuth2TokenResponse fetchToken(final String tokenEndpoint, final String clientId,
            final String clientSecret, final String scope) {
        final StringBuilder bodyBuilder = new StringBuilder("grant_type=client_credentials");
        if (scope != null && !scope.isEmpty()) {
            bodyBuilder.append("&scope=").append(URLEncoder.encode(scope, StandardCharsets.UTF_8));
        }
        final String body = bodyBuilder.toString();

        final String basicAuth = Base64.getEncoder().encodeToString(
                (clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));

        final HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(tokenEndpoint))
                .timeout(REQUEST_TIMEOUT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", "Basic " + basicAuth)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            final HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            final int status = resp.statusCode();
            if (status == HTTP_OK) {
                return mapper.readValue(resp.body(), CallbackOAuth2TokenResponse.class);
            }
            if (status == HTTP_UNAUTHORIZED || status == HTTP_FORBIDDEN) {
                throw new CallbackOAuth2InvalidCredentialException(
                        "OAuth2 endpoint rejected credentials, status=" + status);
            }
            throw new CallbackOAuth2RetryableException("OAuth2 endpoint failure, status=" + status);
        } catch (final IOException e) {
            throw new CallbackOAuth2RetryableException(
                    "OAuth2 IO failure: " + e.getClass().getSimpleName(), e);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CallbackOAuth2RetryableException("OAuth2 interrupted", e);
        }
    }
}
