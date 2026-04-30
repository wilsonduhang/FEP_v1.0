package com.puchain.fep.collector;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySource;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link CollectorProperties} 绑定单元测试。
 *
 * <p>验证：</p>
 * <ul>
 *   <li>所有字段（含 institutionCode / lockTtlMillis / batchSize / retry / adapters）能正确绑定</li>
 *   <li>默认值（batchSize=500 / lockTtlMillis=300_000 / retry.maxAttempts=3 / retry.backoffMillis=1_000L）</li>
 *   <li>{@code retry.backoffMillis} 字段类型为 long（防止 T8 移位溢出回归）</li>
 * </ul>
 */
class CollectorPropertiesTest {

    private static CollectorProperties bind(final Map<String, Object> source) {
        ConfigurationPropertySource src = new MapConfigurationPropertySource(source);
        return new Binder(src).bindOrCreate("fep.collector", CollectorProperties.class);
    }

    @Test
    void shouldBindAllFields() {
        Map<String, Object> source = Map.of(
                "fep.collector.batch-size", 100,
                "fep.collector.lock-ttl-millis", 600000L,
                "fep.collector.institution-code", "FEP00000000001",
                "fep.collector.retry.max-attempts", 5,
                "fep.collector.retry.backoff-millis", 2000L,
                "fep.collector.adapters[0].id", "JDBC_3101",
                "fep.collector.adapters[0].type", "JDBC",
                "fep.collector.adapters[0].cron", "0 0 2 * * *",
                "fep.collector.adapters[0].enabled", true,
                "fep.collector.adapters[0].payload-data-type", "INVOICE_CONTRACT_3101"
        );

        CollectorProperties props = bind(source);

        assertThat(props.getBatchSize()).isEqualTo(100);
        assertThat(props.getLockTtlMillis()).isEqualTo(600_000L);
        assertThat(props.getInstitutionCode()).isEqualTo("FEP00000000001");
        assertThat(props.getRetry().getMaxAttempts()).isEqualTo(5);
        assertThat(props.getRetry().getBackoffMillis()).isEqualTo(2_000L);
        assertThat(props.getAdapters()).hasSize(1).first().satisfies(a -> {
            assertThat(a.getId()).isEqualTo("JDBC_3101");
            assertThat(a.getType()).isEqualTo("JDBC");
            assertThat(a.getCron()).isEqualTo("0 0 2 * * *");
            assertThat(a.isEnabled()).isTrue();
            assertThat(a.getPayloadDataType()).isEqualTo("INVOICE_CONTRACT_3101");
        });
    }

    @Test
    void shouldApplyDefaultsWhenAllFieldsAbsent() {
        CollectorProperties props = bind(Map.of());

        assertThat(props.getBatchSize()).isEqualTo(CollectorProperties.DEFAULT_BATCH_SIZE);
        assertThat(props.getLockTtlMillis()).isEqualTo(CollectorProperties.DEFAULT_LOCK_TTL_MILLIS);
        assertThat(props.getRetry().getMaxAttempts())
                .isEqualTo(CollectorProperties.Retry.DEFAULT_MAX_ATTEMPTS);
        assertThat(props.getRetry().getBackoffMillis())
                .isEqualTo(CollectorProperties.Retry.DEFAULT_BACKOFF_MILLIS);
        assertThat(props.getAdapters()).isEmpty();
        assertThat(props.getInstitutionCode()).isNull();
    }

    /**
     * 边界：institutionCode 空字符串绑定通过（Properties 层不做 fail-fast；
     * 长度/非空校验在 T1+ 调度/组装层兜底，参 CollectorProperties.java field comment）。
     */
    @Test
    void shouldBindEmptyInstitutionCode() {
        CollectorProperties props = bind(Map.of("fep.collector.institution-code", ""));

        assertThat(props.getInstitutionCode()).isEqualTo("");
    }

    /**
     * 边界：adapters 列表多元素 + 不同 type 同时绑定。
     */
    @Test
    void shouldBindMultipleAdapters() {
        Map<String, Object> source = Map.of(
                "fep.collector.adapters[0].id", "JDBC_3101",
                "fep.collector.adapters[0].type", "JDBC",
                "fep.collector.adapters[0].payload-data-type", "INVOICE_CONTRACT_3101",
                "fep.collector.adapters[1].id", "FILE_3102",
                "fep.collector.adapters[1].type", "FILE",
                "fep.collector.adapters[1].payload-data-type", "ARCHIVE_3102",
                "fep.collector.adapters[2].id", "MQ_3109",
                "fep.collector.adapters[2].type", "MQ",
                "fep.collector.adapters[2].payload-data-type", "QY_REGISTER_3109"
        );

        CollectorProperties props = bind(source);

        assertThat(props.getAdapters()).hasSize(3);
        assertThat(props.getAdapters().get(0).getId()).isEqualTo("JDBC_3101");
        assertThat(props.getAdapters().get(1).getType()).isEqualTo("FILE");
        assertThat(props.getAdapters().get(2).getPayloadDataType()).isEqualTo("QY_REGISTER_3109");
    }

    /**
     * 边界：adapters 为空列表（无任何 adapter 配置）默认空。
     */
    @Test
    void shouldBindEmptyAdaptersList() {
        CollectorProperties props = bind(Map.of("fep.collector.batch-size", 100));

        assertThat(props.getAdapters()).isNotNull().isEmpty();
    }

    @Test
    void shouldBindSourceConfigMap() {
        Map<String, Object> source = Map.of(
                "fep.collector.adapters[0].id", "JDBC_BIZ",
                "fep.collector.adapters[0].type", "JDBC",
                "fep.collector.adapters[0].source-config.url", "jdbc:h2:mem:bizdb",
                "fep.collector.adapters[0].source-config.username", "sa"
        );

        CollectorProperties props = bind(source);

        assertThat(props.getAdapters()).hasSize(1);
        assertThat(props.getAdapters().get(0).getSourceConfig())
                .containsEntry("url", "jdbc:h2:mem:bizdb")
                .containsEntry("username", "sa");
    }

    /**
     * 守护 backoffMillis 字段类型为 long（Plan T0 #2 critical）。
     *
     * <p>若被改回 int，T8 退避算式 {@code backoffMillis << shift} 在 shift≈30 时
     * {@code 1000 << 30 ≈ 10^12} 超过 {@link Integer#MAX_VALUE} 变负数。
     */
    @Test
    void retryBackoffMillisShouldBeLongTypeToAvoidShiftOverflow() throws NoSuchFieldException {
        java.lang.reflect.Field field =
                CollectorProperties.Retry.class.getDeclaredField("backoffMillis");
        assertThat(field.getType())
                .as("retry.backoffMillis 必须为 long，避免 T8 退避算式 int 域溢出")
                .isEqualTo(long.class);
    }

    @Test
    void lockTtlMillisShouldBeLongType() throws NoSuchFieldException {
        java.lang.reflect.Field field =
                CollectorProperties.class.getDeclaredField("lockTtlMillis");
        assertThat(field.getType()).isEqualTo(long.class);
    }
}
