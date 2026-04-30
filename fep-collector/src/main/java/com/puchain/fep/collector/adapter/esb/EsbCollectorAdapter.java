package com.puchain.fep.collector.adapter.esb;

import com.puchain.fep.collector.support.AdapterType;
import com.puchain.fep.collector.support.CollectionRecord;
import com.puchain.fep.collector.support.CollectionRunContext;
import com.puchain.fep.collector.support.CollectorAdapter;
import com.puchain.fep.collector.support.IdempotencyKeyGenerator;
import com.puchain.fep.collector.support.WatermarkStore;
import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.LogSanitizer;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * ESB（HTTP / REST）数据采集适配器（PRD v1.3 §2.2.2 数仓模式 / §2.1 ESB 适配器）。
 *
 * <p><b>采集语义：</b>
 * <ol>
 *   <li>{@link #collect} 从 {@link WatermarkStore} 取 watermark（首次用 {@link EsbAdapterConfig#initialCursor()}）
 *       → {@link UriComponentsBuilder} 在 {@code endpoint} 后追加 {@code cursorParam=watermark} 构造 URI</li>
 *   <li>{@link RestClient} GET → {@code List<Map<String, Object>>}（JSON 数组反序列化）</li>
 *   <li>每行 row → {@link CollectionRecord}（{@code sourceRef = row[cursorParam]}）</li>
 * </ol>
 * {@link #acknowledge} 取本批 records 中 {@code sourceRef}（即 {@code row[cursorParam]}）字典序最大值 →
 * {@code watermarkStore.put}（at-least-once 语义）。
 *
 * <p><b>启动时鉴权校验（Plan §T5 #3）：</b>构造时若 {@link EsbAdapterConfig#authHeaderValueRef()}
 * 非 null，立即调用 {@link System#getenv(String)} 解析 — 若返回 null（环境变量缺失） 抛
 * {@link IllegalStateException}，禁止 adapter 实例化。这是 fail-fast 设计，避免运行时反复抛错。
 * 解析后的值缓存为 {@link #resolvedAuthHeaderValue}，每次 {@link #collect} 直接使用。
 *
 * <p><b>异常包装：</b>
 * <ul>
 *   <li>{@link ResourceAccessException}（含底层 {@link java.net.SocketTimeoutException} /
 *       {@link java.io.IOException}） → {@link FepBusinessException}
 *       ({@link FepErrorCode#COLLECT_ADAPTER_FAILURE}, "ESB GET timeout")</li>
 *   <li>{@link RestClientException}（HTTP 4xx/5xx + 反序列化错误） → 同样包装为
 *       {@code COLLECT_ADAPTER_FAILURE}（消息 "ESB GET failed"）</li>
 *   <li>错误消息中 adapterId 经 {@link LogSanitizer#sanitize} 处理防 CRLF 注入（CWE-117）</li>
 * </ul>
 *
 * <p><b>RestClient 注入策略：</b>构造方需注入<b>已配置好超时的</b> {@link RestClient}。
 * Spring RestClient 不支持 per-call timeout，调用方应通过
 * {@link org.springframework.http.client.SimpleClientHttpRequestFactory} 等 factory
 * 配置 connect / read timeout 后再 {@code RestClient.builder().requestFactory(...)}。
 * Adapter 不在内部组装 RestClient 是为了便于测试（注入 {@link org.springframework.test.web.client.MockRestServiceServer}
 * 绑定的 builder）以及 multi-adapter 共享 connection pool 的可能性。
 *
 * <p><b>线程安全：</b>本类无可变字段（全 final），可单实例多线程并发调用。
 *
 * <p><b>非 Spring Bean：</b>遵循 {@link com.puchain.fep.collector.adapter.jdbc.JdbcCollectorAdapter}
 * 与 {@link com.puchain.fep.collector.adapter.file.FileCollectorAdapter} 先例 —
 * 由调用方（{@code AdapterFactory} / 配置驱动装配）显式 new 出来。
 *
 * @author FEP Team
 * @since 1.0.0
 */
public final class EsbCollectorAdapter implements CollectorAdapter {

    /**
     * JSON 数组反序列化目标类型 — {@code List<Map<String, Object>>}。
     *
     * <p>静态实例避免每次 {@link #collect} 重复构造（{@link ParameterizedTypeReference}
     * 是不可变 type witness）。
     */
    private static final ParameterizedTypeReference<List<Map<String, Object>>> ROW_LIST_TYPE =
            new ParameterizedTypeReference<>() { };

    private final EsbAdapterConfig config;
    private final RestClient restClient;
    private final WatermarkStore watermarkStore;

    /**
     * 启动时一次性解析的鉴权 header 值（{@link System#getenv} 结果），不鉴权时为 null。
     */
    private final String resolvedAuthHeaderValue;

    /**
     * 构造 ESB 采集适配器。
     *
     * <p><b>Plan §T5 #3 启动校验：</b>若 {@code config.authHeaderValueRef()} 非 null，
     * 通过 {@link System#getenv(String)} 解析；返回 null 时抛 {@link IllegalStateException}
     * 禁止 adapter 启动（fail-fast，避免运行时反复抛错）。
     *
     * @param config         配置（非 null）
     * @param restClient     已配置好超时的 {@link RestClient}（非 null）
     * @param watermarkStore 水位存储（非 null）
     * @throws NullPointerException  任一参数为 null
     * @throws IllegalStateException {@code authHeaderValueRef} 指定的环境变量缺失
     */
    public EsbCollectorAdapter(
            final EsbAdapterConfig config,
            final RestClient restClient,
            final WatermarkStore watermarkStore) {
        this.config = Objects.requireNonNull(config, "config");
        this.restClient = Objects.requireNonNull(restClient, "restClient");
        this.watermarkStore = Objects.requireNonNull(watermarkStore, "watermarkStore");
        this.resolvedAuthHeaderValue = resolveAuthHeader(config);
    }

    /**
     * 解析鉴权 header 值（启动时一次性）。
     *
     * <p>{@code authHeaderValueRef} 为 null 时返回 null（不鉴权）；非 null 时调用
     * {@link System#getenv(String)} 取环境变量值，缺失时抛 {@link IllegalStateException}。
     *
     * @param config 配置
     * @return 解析后的 header 值，不鉴权时为 null
     * @throws IllegalStateException 环境变量缺失
     */
    private static String resolveAuthHeader(final EsbAdapterConfig config) {
        final String envVarName = config.authHeaderValueRef();
        if (envVarName == null) {
            return null;
        }
        final String value = System.getenv(envVarName);
        if (value == null) {
            throw new IllegalStateException(
                    "ESB auth env var missing: " + LogSanitizer.sanitize(envVarName));
        }
        return value;
    }

    @Override
    public String getId() {
        return config.adapterId();
    }

    @Override
    public AdapterType getType() {
        return AdapterType.ESB;
    }

    @Override
    public List<CollectionRecord> collect(final CollectionRunContext context) {
        Objects.requireNonNull(context, "context");
        final String watermark = watermarkStore.get(getId())
                .orElse(config.initialCursor());
        final URI uri = UriComponentsBuilder.fromUri(config.endpoint())
                .queryParam(config.cursorParam(), watermark)
                .build()
                .toUri();

        final List<Map<String, Object>> rows;
        try {
            RestClient.RequestHeadersSpec<?> spec = restClient.get().uri(uri);
            if (config.authHeaderName() != null && resolvedAuthHeaderValue != null) {
                spec = spec.header(config.authHeaderName(), resolvedAuthHeaderValue);
            }
            final List<Map<String, Object>> body = spec.retrieve().body(ROW_LIST_TYPE);
            rows = body == null ? List.of() : body;
        } catch (ResourceAccessException e) {
            // ResourceAccessException 包装 SocketTimeoutException / IOException / connect 失败
            throw new FepBusinessException(
                    FepErrorCode.COLLECT_ADAPTER_FAILURE,
                    "ESB GET timeout: " + LogSanitizer.sanitize(getId()),
                    e);
        } catch (RestClientException e) {
            // RestClientException 覆盖 HTTP 4xx/5xx + 反序列化错误（HttpClientErrorException 等）
            throw new FepBusinessException(
                    FepErrorCode.COLLECT_ADAPTER_FAILURE,
                    "ESB GET failed: " + LogSanitizer.sanitize(getId()),
                    e);
        }

        if (rows.isEmpty()) {
            return List.of();
        }
        final Instant collectedAt = Instant.now();
        final List<CollectionRecord> records = new ArrayList<>(rows.size());
        for (final Map<String, Object> row : rows) {
            // sourceRef = cursorParam 字段值（与 acknowledge 推水位的字段一致）
            final String sourceRef = String.valueOf(row.get(config.cursorParam()));
            records.add(CollectionRecord.builder()
                    .adapterId(getId())
                    .sourceRef(sourceRef)
                    .payloadDataType(config.payloadDataType())
                    .rawData(row)
                    .collectedAt(collectedAt)
                    .idempotencyKey(IdempotencyKeyGenerator.generate(getId(), sourceRef))
                    .build());
        }
        return List.copyOf(records);
    }

    @Override
    public void acknowledge(
            final CollectionRunContext context,
            final List<CollectionRecord> records) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(records, "records");
        if (records.isEmpty()) {
            return;
        }
        // 取本批 sourceRef（即 row[cursorParam]）字典序最大值推进 watermark
        String maxCursor = null;
        for (final CollectionRecord record : records) {
            final String cursor = record.getSourceRef();
            if (maxCursor == null || cursor.compareTo(maxCursor) > 0) {
                maxCursor = cursor;
            }
        }
        if (maxCursor != null) {
            watermarkStore.put(getId(), maxCursor);
        }
    }
}
