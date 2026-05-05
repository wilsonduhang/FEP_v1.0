package com.puchain.fep.web.outbound.consumer;

import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P5 T8 OutboundMetrics + Actuator Prometheus 端点集成测试。
 *
 * <p>验收标准（Plan §Task 8 AC1-AC4）：</p>
 * <ul>
 *   <li>AC1: {@code fep_outbound_send_total{status="SENT|RETRY|DEAD_LETTER"}} Counter 暴露</li>
 *   <li>AC2: {@code fep_outbound_send_latency_seconds} Timer 暴露（含 percentile 50/95/99，
 *       通过 {@code application.yml} 的 distribution.percentiles 配置）</li>
 *   <li>AC3: {@code /actuator/prometheus} 包含上述 2 metrics 并已豁免 Spring Security
 *       （Plan AC3 reality-check：原 P0.5 配置不含 prometheus，本 Task 显式补全
 *       {@code SecurityConfiguration.PUBLIC_PATHS} 与 {@code application.yml}
 *       {@code management.endpoints.web.exposure.include}）</li>
 *   <li>AC4: 测试类后缀 {@code IntegrationTest}（非 {@code IT}，规避 fep-web Surefire
 *       静默跳过 — defect_p2b_silent_skip_it 红线）</li>
 * </ul>
 *
 * <p>使用与 {@code SecurityConfigurationTest} 同款 redis health 关闭手段，避免
 * {@code /actuator/health} 周边自动配置因外部 Redis 不可达拖累上下文加载。</p>
 *
 * <p><b>{@code management.prometheus.metrics.export.enabled=true} 必加</b>：
 * Spring Boot 测试基础设施 {@code MetricsExportContextCustomizerFactory} 在测试类
 * 未声明 {@code @AutoConfigureMetrics}（Spring Boot 3 已移除）时注入
 * {@code DisableMetricExportContextCustomizer}，自动设置
 * {@code management.defaults.metrics.export.enabled=false}（防止测试向真实 Prometheus
 * push gateway 等外部系统泄漏指标）。这导致
 * {@code PrometheusMetricsExportAutoConfiguration} 的
 * {@code @ConditionalOnEnabledMetricsExport("prometheus")} 条件为 false，
 * {@code PrometheusMeterRegistry} bean 不创建，{@code /actuator/prometheus} 端点不暴露，
 * MockMvc 请求落到静态资源 fallback 抛 {@code NoResourceFoundException} → HTTP 500。
 * 显式属性 {@code management.prometheus.metrics.export.enabled=true} 优先于 defaults，
 * 恢复 application.yml 的 {@code include: prometheus} 行为。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest(properties = {
        "management.health.redis.enabled=false",
        "management.prometheus.metrics.export.enabled=true"
})
@AutoConfigureMockMvc
class OutboundMetricsActuatorIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private OutboundMetrics metrics;

    @Autowired
    private MeterRegistry registry;

    /**
     * 验证 SENT counter + latency timer 注册并通过 {@code /actuator/prometheus} 暴露，
     * 同时直接查询 {@link MeterRegistry} 校验业务计数值（quality reviewer #2 修订：
     * 仅 {@code containsString} 不能区分 0 / 1 / 100，业务断言强度不足）。
     */
    @Test
    void prometheus_endpoint_should_expose_outbound_send_total_and_latency() throws Exception {
        metrics.recordSent(123L);
        metrics.recordRetry();

        // Direct registry assertions — verify counter values, not just metric names.
        assertThat(registry.counter("fep_outbound_send_total", "status", "SENT").count())
                .isEqualTo(1.0);
        assertThat(registry.counter("fep_outbound_send_total", "status", "RETRY").count())
                .isEqualTo(1.0);
        assertThat(registry.timer("fep_outbound_send_latency_seconds", "status", "SENT").count())
                .isEqualTo(1L);

        // Endpoint smoke — verify scrape exposes the metric series with correct tags.
        mvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("fep_outbound_send_total")))
                .andExpect(content().string(containsString("fep_outbound_send_latency_seconds")))
                .andExpect(content().string(containsString("status=\"SENT\"")))
                .andExpect(content().string(containsString("status=\"RETRY\"")));
    }

    /**
     * 验证 DEAD_LETTER counter 在 {@code /actuator/prometheus} 中以独立 status tag 行暴露，
     * 直接查询 {@link MeterRegistry} 校验计数值（quality reviewer #2 修订）。
     */
    @Test
    void prometheus_endpoint_should_expose_dead_letter_counter() throws Exception {
        metrics.recordDeadLetter();

        assertThat(registry.counter("fep_outbound_send_total", "status", "DEAD_LETTER").count())
                .isEqualTo(1.0);

        mvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("status=\"DEAD_LETTER\"")));
    }
}
