package com.puchain.fep.collector.adapter.esb;

import com.puchain.fep.collector.support.AdapterType;
import com.puchain.fep.collector.support.CollectionRecord;
import com.puchain.fep.collector.support.CollectionRunContext;
import com.puchain.fep.collector.support.InMemoryWatermarkStore;
import com.puchain.fep.collector.support.TriggerType;
import com.puchain.fep.collector.support.WatermarkStore;
import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * {@link EsbCollectorAdapter} 单元测试（{@link MockRestServiceServer} + 真 RestClient）。
 *
 * <p>覆盖 Plan §T5 验收标准：
 * <ul>
 *   <li>#1 GET endpoint → JSON 数组 → 每 row → CollectionRecord</li>
 *   <li>#2 配置：endpoint / authHeaderName / authHeaderValueRef / cursorParam / initialCursor / timeout</li>
 *   <li>#3 启动校验：authHeaderValueRef 缺失环境变量 → IllegalStateException</li>
 *   <li>#4 collect()：watermark → URI（带 cursorParam）→ GET → 每 row → CollectionRecord</li>
 *   <li>#5 acknowledge()：取本批 cursorParam 字段最大值 → save</li>
 *   <li>#6 超时 → COLLECT_ADAPTER_FAILURE "ESB GET timeout"</li>
 *   <li>#7 MockRestServiceServer + JSON sample</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class EsbCollectorAdapterTest {

    private static final String ADAPTER_ID = "ESB_INVOICE_TEST";
    private static final String PAYLOAD_DATA_TYPE = "INVOICE_TEST_3101";
    private static final String CURSOR_PARAM = "lastSeen";
    private static final String INITIAL_CURSOR = "INIT";
    private static final URI ENDPOINT = URI.create("http://esb.test.local/api/invoices");

    /**
     * Plan §T5 #1 + #4 — GET endpoint 返回 JSON 数组 → 每 row 映射为 CollectionRecord。
     */
    @Test
    void shouldGetEndpointAndParseRows() {
        final RestClient.Builder builder = RestClient.builder();
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final RestClient restClient = builder.build();
        server.expect(requestTo(buildExpectedUri(INITIAL_CURSOR)))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess(threeRowsJson(), MediaType.APPLICATION_JSON));

        final EsbCollectorAdapter adapter = newAdapter(noAuthConfig(), restClient);
        final List<CollectionRecord> rows = adapter.collect(ctx());

        assertThat(rows)
                .as("JSON 数组 3 行 → 3 条 CollectionRecord")
                .hasSize(3);
        assertThat(rows.get(0).getRawData())
                .as("rawData 必须保留 JSON 字段")
                .containsEntry("invoiceId", "INV001")
                .containsEntry(CURSOR_PARAM, "C1");
        assertThat(rows.get(0).getSourceRef())
                .as("sourceRef 必须等于 cursorParam 字段值")
                .isEqualTo("C1");
        assertThat(rows.get(0).getAdapterId()).isEqualTo(ADAPTER_ID);
        assertThat(rows.get(0).getPayloadDataType()).isEqualTo(PAYLOAD_DATA_TYPE);
        assertThat(rows.get(0).getIdempotencyKey()).matches("[0-9a-f]{32}");
        assertThat(rows.get(0).getCollectedAt()).isNotNull();
        server.verify();
    }

    /**
     * Plan §T5 #3 — authHeaderValueRef 缺失环境变量 → 构造时抛 IllegalStateException。
     */
    @Test
    void shouldThrowOnAuthEnvVarMissing() {
        final EsbAdapterConfig config = new EsbAdapterConfig(
                ADAPTER_ID, PAYLOAD_DATA_TYPE, ENDPOINT,
                "Authorization", "FEP_ESB_TEST_NONEXISTENT_VAR_" + UUID.randomUUID(),
                1000L, CURSOR_PARAM, INITIAL_CURSOR, Duration.ofSeconds(10));

        assertThatThrownBy(() -> new EsbCollectorAdapter(
                config, RestClient.builder().build(), new InMemoryWatermarkStore()))
                .as("env 变量缺失必须 fail-fast 抛 IllegalStateException")
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FEP_ESB_TEST_NONEXISTENT_VAR_");
    }

    /**
     * Plan §T5 #2 — authHeaderName + authHeaderValueRef 配置，env 存在时请求带 auth header。
     */
    @Test
    void shouldUseAuthHeaderWhenEnvVarPresent() {
        // 选 PATH 作为 env 变量（macOS / Linux / CI 通用）
        final String pathValue = System.getenv("PATH");
        assertThat(pathValue)
                .as("测试前置：PATH 环境变量必须存在")
                .isNotNull();

        final RestClient.Builder builder = RestClient.builder();
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final RestClient restClient = builder.build();
        server.expect(requestTo(buildExpectedUri(INITIAL_CURSOR)))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, pathValue))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        final EsbAdapterConfig authConfig = new EsbAdapterConfig(
                ADAPTER_ID, PAYLOAD_DATA_TYPE, ENDPOINT,
                "Authorization", "PATH",
                1000L, CURSOR_PARAM, INITIAL_CURSOR, Duration.ofSeconds(10));
        final EsbCollectorAdapter adapter = newAdapter(authConfig, restClient);
        final List<CollectionRecord> rows = adapter.collect(ctx());

        assertThat(rows).isEmpty();
        server.verify();
    }

    /**
     * Plan §T5 #4 — watermark 必须作为 cursorParam 查询参数附加到 URL。
     */
    @Test
    void shouldAppendCursorParamToUrl() {
        final WatermarkStore store = new InMemoryWatermarkStore();
        store.put(ADAPTER_ID, "WM_VALUE_42");

        final RestClient.Builder builder = RestClient.builder();
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final RestClient restClient = builder.build();
        server.expect(method(org.springframework.http.HttpMethod.GET))
                .andExpect(queryParam(CURSOR_PARAM, "WM_VALUE_42"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        final EsbCollectorAdapter adapter = new EsbCollectorAdapter(
                noAuthConfig(), restClient, store);
        adapter.collect(ctx());
        server.verify();
    }

    /**
     * Plan §T5 #6 — RestClient 超时（{@code ResourceAccessException}）必须包装为
     * {@link FepErrorCode#COLLECT_ADAPTER_FAILURE} 并保留 cause。
     */
    @Test
    void shouldWrapTimeoutAsCollectAdapterFailure() {
        // 真 RestClient + 1ms 超时 + 非路由地址 → 必触发 ResourceAccessException
        final SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofMillis(1).toMillis());
        factory.setReadTimeout((int) Duration.ofMillis(1).toMillis());
        final RestClient restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
        // 192.0.2.0/24 是 RFC 5737 文档保留地址，不可路由 → connect 必超时
        final URI nonRoutable = URI.create("http://192.0.2.1:9/notreal");
        final EsbAdapterConfig timeoutConfig = new EsbAdapterConfig(
                ADAPTER_ID, PAYLOAD_DATA_TYPE, nonRoutable,
                null, null, 1000L, CURSOR_PARAM, INITIAL_CURSOR,
                Duration.ofMillis(1));
        final EsbCollectorAdapter adapter = new EsbCollectorAdapter(
                timeoutConfig, restClient, new InMemoryWatermarkStore());

        assertThatThrownBy(() -> adapter.collect(ctx()))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("ESB GET")
                .extracting("errorCode")
                .isEqualTo(FepErrorCode.COLLECT_ADAPTER_FAILURE);
    }

    /**
     * Plan §T5 #5 — acknowledge 取本批 cursorParam 字段最大值（字典序）→ save。
     */
    @Test
    void shouldAdvanceWatermarkOnAcknowledge() {
        final WatermarkStore store = new InMemoryWatermarkStore();
        final RestClient.Builder builder = RestClient.builder();
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final RestClient restClient = builder.build();
        server.expect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess(threeRowsJson(), MediaType.APPLICATION_JSON));

        final EsbCollectorAdapter adapter = new EsbCollectorAdapter(
                noAuthConfig(), restClient, store);
        final List<CollectionRecord> rows = adapter.collect(ctx());
        adapter.acknowledge(ctx(), rows);

        assertThat(store.get(ADAPTER_ID))
                .as("watermark 必须推进到本批 cursorParam 最大值 C3")
                .contains("C3");
    }

    /**
     * Plan §T5 #5 边界 — 空集合 ack 必须 no-op（与 T2/T3/T4 先例一致）。
     */
    @Test
    void shouldNotAdvanceWatermarkOnEmptyAck() {
        final WatermarkStore store = new InMemoryWatermarkStore();
        final EsbCollectorAdapter adapter = new EsbCollectorAdapter(
                noAuthConfig(), RestClient.builder().build(), store);
        adapter.acknowledge(ctx(), List.of());
        assertThat(store.get(ADAPTER_ID))
                .as("空 records ack 不应推进 watermark")
                .isEmpty();
    }

    /**
     * CollectorAdapter 接口契约 — getType() 返回 ESB / getId() 回显 config.adapterId()。
     */
    @Test
    void getTypeShouldReturnEsb() {
        final EsbCollectorAdapter adapter = new EsbCollectorAdapter(
                noAuthConfig(), RestClient.builder().build(), new InMemoryWatermarkStore());
        assertThat(adapter.getType()).isEqualTo(AdapterType.ESB);
        assertThat(adapter.getId()).isEqualTo(ADAPTER_ID);
    }

    /**
     * Plan §T5 边界 — Config 必填字段 null guard。
     */
    @Test
    void configShouldRejectMissingFields() {
        assertThatThrownBy(() -> new EsbAdapterConfig(
                null, PAYLOAD_DATA_TYPE, ENDPOINT,
                null, null, 1000L, CURSOR_PARAM, INITIAL_CURSOR, Duration.ofSeconds(10)))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("adapterId");

        assertThatThrownBy(() -> new EsbAdapterConfig(
                ADAPTER_ID, PAYLOAD_DATA_TYPE, null,
                null, null, 1000L, CURSOR_PARAM, INITIAL_CURSOR, Duration.ofSeconds(10)))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("endpoint");

        assertThatThrownBy(() -> new EsbAdapterConfig(
                ADAPTER_ID, PAYLOAD_DATA_TYPE, ENDPOINT,
                null, null, 1000L, CURSOR_PARAM, INITIAL_CURSOR, Duration.ofMillis(0)))
                .as("timeout 必须 > 0")
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("timeout");
    }

    /**
     * Plan §T5 #6 — HTTP 4xx/5xx 必须包装为 COLLECT_ADAPTER_FAILURE（不同于 timeout 走 ResourceAccessException）。
     */
    @Test
    void shouldWrapHttpErrorAsCollectAdapterFailure() {
        final RestClient.Builder builder = RestClient.builder();
        final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        final RestClient restClient = builder.build();
        server.expect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withStatus(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR));

        final EsbCollectorAdapter adapter = newAdapter(noAuthConfig(), restClient);

        assertThatThrownBy(() -> adapter.collect(ctx()))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("ESB GET")
                .extracting("errorCode")
                .isEqualTo(FepErrorCode.COLLECT_ADAPTER_FAILURE);
    }

    /**
     * Plan §T5 #2 工厂 — withDefaults 必须填充 timeout 默认 10s。
     */
    @Test
    void configWithDefaultsShouldUseTenSecondTimeout() {
        final EsbAdapterConfig defaulted = EsbAdapterConfig.withDefaults(
                ADAPTER_ID, PAYLOAD_DATA_TYPE, ENDPOINT,
                CURSOR_PARAM, INITIAL_CURSOR);
        assertThat(defaulted.timeout())
                .as("withDefaults 必须填充 DEFAULT_TIMEOUT = 10s")
                .isEqualTo(EsbAdapterConfig.DEFAULT_TIMEOUT)
                .isEqualTo(Duration.ofSeconds(10));
        assertThat(defaulted.authHeaderName()).isNull();
        assertThat(defaulted.authHeaderValueRef()).isNull();
    }

    private EsbAdapterConfig noAuthConfig() {
        return new EsbAdapterConfig(
                ADAPTER_ID, PAYLOAD_DATA_TYPE, ENDPOINT,
                null, null, 1000L, CURSOR_PARAM, INITIAL_CURSOR,
                Duration.ofSeconds(10));
    }

    private EsbCollectorAdapter newAdapter(final EsbAdapterConfig config, final RestClient restClient) {
        return new EsbCollectorAdapter(config, restClient, new InMemoryWatermarkStore());
    }

    private CollectionRunContext ctx() {
        return new CollectionRunContext(
                UUID.randomUUID().toString().replace("-", ""),
                ADAPTER_ID,
                TriggerType.SCHEDULED,
                Optional.empty(),
                Instant.now(),
                100);
    }

    private static String buildExpectedUri(final String cursor) {
        return ENDPOINT + "?" + CURSOR_PARAM + "=" + cursor;
    }

    /**
     * 三行 sample JSON — cursorParam 字段值递增 C1 / C2 / C3。
     */
    private static String threeRowsJson() {
        return "["
                + "{\"invoiceId\":\"INV001\",\"amount\":100,\"" + CURSOR_PARAM + "\":\"C1\"},"
                + "{\"invoiceId\":\"INV002\",\"amount\":200,\"" + CURSOR_PARAM + "\":\"C2\"},"
                + "{\"invoiceId\":\"INV003\",\"amount\":300,\"" + CURSOR_PARAM + "\":\"C3\"}"
                + "]";
    }

}
