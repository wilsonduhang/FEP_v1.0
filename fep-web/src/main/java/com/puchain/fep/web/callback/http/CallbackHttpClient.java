package com.puchain.fep.web.callback.http;

import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.web.submission.outputinterface.domain.InterfaceAuthType;
import com.puchain.fep.web.submission.outputinterface.domain.SubOutputInterface;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 行内 RESTful 回调 HTTP 客户端。
 *
 * <p>使用 JDK 11+ {@link java.net.http.HttpClient}，单例共享连接池。
 * 每次调用按目标接口配置的 {@link SubOutputInterface#getTimeoutSeconds()} 设置请求超时。
 * 鉴权头逻辑按 {@link InterfaceAuthType} 分支设置：</p>
 * <ul>
 *   <li>{@code TOKEN} — {@code Authorization: <scaffold>}（凭证值来源 Phase 2 §5.5.3）</li>
 *   <li>{@code OAUTH2} — {@code Authorization: Bearer <scaffold>}（同上）</li>
 *   <li>{@code NONE} — 无 {@code Authorization} 头</li>
 * </ul>
 *
 * <p><strong>P1 限制</strong>：{@code SubOutputInterface} 暂无鉴权凭证字段（Phase 2 §5.5.3 补入），
 * TOKEN/OAUTH2 场景下 P1 仅置空占位头，作为脚手架预留，不含真实凭证。</p>
 *
 * <p>所有失败均翻译为 {@link CallbackResult}，不向调用方抛出异常（接口契约，
 * 与 P4-MSG-D {@code BizMessage2101InboundListener} 同类文档化契约）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class CallbackHttpClient {

    private static final Logger LOG = LoggerFactory.getLogger(CallbackHttpClient.class);

    private static final String CONTENT_TYPE_JSON = "application/json";
    private static final int HTTP_OK_MIN = 200;
    private static final int HTTP_OK_MAX = 299;

    /**
     * 单例 HttpClient，共享底层连接池，线程安全。
     */
    private final HttpClient httpClient;

    /**
     * 无参构造器：初始化 JDK HttpClient 单例。
     */
    public CallbackHttpClient() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    /**
     * 向目标接口 POST JSON 载荷。
     *
     * <p>2xx 返回 {@link CallbackResult#success()} == {@code true}；
     * 非 2xx 或 IO 异常返回 {@code false}，不抛出。超时按
     * {@link SubOutputInterface#getTimeoutSeconds()} 设置。</p>
     *
     * @param target      目标输出接口配置，非空
     * @param payloadJson JSON 字符串载荷，非空
     * @return 推送结果，永不为 null
     */
    @SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
            justification = "non-literal String log args wrapped by LogSanitizer.sanitize; "
                    + "find-sec-bugs cannot detect user-defined sanitizer")
    public CallbackResult post(final SubOutputInterface target, final String payloadJson) {
        final HttpRequest request = buildRequest(target, payloadJson);
        try {
            final HttpResponse<Void> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            final int statusCode = response.statusCode();
            if (statusCode >= HTTP_OK_MIN && statusCode <= HTTP_OK_MAX) {
                return new CallbackResult(true, statusCode, null);
            }
            return new CallbackResult(false, statusCode, "http " + statusCode);
        } catch (final IOException e) {
            LOG.debug("callback HTTP IO failure interfaceId={} type={}",
                    LogSanitizer.sanitize(target.getInterfaceId()),
                    LogSanitizer.sanitize(e.getClass().getSimpleName()));
            return new CallbackResult(false, 0, "io: " + e.getClass().getSimpleName());
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return new CallbackResult(false, 0, "interrupted");
        }
    }

    /**
     * 构建 HTTP 请求，按 authType 添加鉴权头。
     *
     * @param target      目标接口配置
     * @param payloadJson 请求体
     * @return 构建好的 HttpRequest
     */
    private HttpRequest buildRequest(final SubOutputInterface target, final String payloadJson) {
        final HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(target.getInterfaceUrl()))
                .timeout(Duration.ofSeconds(target.getTimeoutSeconds()))
                .header("Content-Type", CONTENT_TYPE_JSON)
                .POST(HttpRequest.BodyPublishers.ofString(payloadJson, StandardCharsets.UTF_8));

        // P1 scaffold: auth header set per authType; credential value source = Phase 2 §5.5.3.
        // SubOutputInterface has no credential/token field in P1 — header value is placeholder.
        final InterfaceAuthType authType = target.getAuthType();
        if (authType == InterfaceAuthType.TOKEN) {
            // P1: header present as scaffold; actual token injected in Phase 2 §5.5.3
            builder.header("Authorization", "");
        } else if (authType == InterfaceAuthType.OAUTH2) {
            // P1: Bearer scaffold; actual bearer token resolved in Phase 2 §5.5.3
            builder.header("Authorization", "Bearer ");
        }
        // NONE: no Authorization header

        return builder.build();
    }
}
