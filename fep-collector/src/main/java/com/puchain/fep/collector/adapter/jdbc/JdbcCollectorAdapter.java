package com.puchain.fep.collector.adapter.jdbc;

import com.puchain.fep.collector.support.AdapterType;
import com.puchain.fep.collector.support.CollectionRecord;
import com.puchain.fep.collector.support.CollectionRunContext;
import com.puchain.fep.collector.support.CollectorAdapter;
import com.puchain.fep.collector.support.IdempotencyKeyGenerator;
import com.puchain.fep.collector.support.WatermarkStore;
import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.LogSanitizer;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * JDBC 数据采集适配器（PRD v1.3 §2.2.2 数仓模式 / §2.1 JDBC 适配器）。
 *
 * <p><b>采集语义：</b>基于 {@link WatermarkStore} 持久化的 cursor 列水位驱动增量取数 —
 * 每次 {@link #collect} 用 {@code :cursor = 上次水位} + {@code :batch_size = ctx.batchSize}
 * 命名参数执行 SQL，得到本批次记录；下游入队成功后 {@link #acknowledge} 推进 watermark
 * 到本批最大 cursor 值。
 *
 * <p><b>多数据源支持：</b>构造时注入 {@code Map<String, NamedParameterJdbcTemplate>}，
 * 按 {@code config.dataSourceBeanName()} 路由。Bean 不存在时抛
 * {@link FepErrorCode#COLLECT_ADAPTER_FAILURE}（日志中 dataSourceBeanName 经
 * {@link LogSanitizer#sanitize} 防 CRLF 注入）。
 *
 * <p><b>异常包装：</b>SQL 层 {@link DataAccessException} 一律包装为
 * {@link FepBusinessException}（COLLECT_ADAPTER_FAILURE）保留 root cause；
 * 不允许 SQL 异常逃逸到调度器（调度器无法识别 Spring DataAccessException 体系）。
 *
 * <p><b>null cursor 处理：</b>cursor 列值为 null 时 → {@code sourceRef = "null"} 字符串
 * （{@link String#valueOf(Object)} 行为）。{@link #acknowledge} 显式将 {@code "null"}
 * 字面值排除在 max 计算外（{@link #NULL_LITERAL} 哨兵），保证 nullable cursor 列即使
 * 出现 null 值也不会拔高水位 — "null" 的 ASCII（0x6E）字典序大于 ISO-8601 时间串与任意
 * 数字开头串（'0'-'9' = 0x30-0x39），朴素 max 比较会错误推进 watermark；故必须显式跳过。
 * 生产应避免 nullable 列作水位，本类仅作退化保护。
 *
 * <p><b>线程安全：</b>本类无可变字段（全 final），可单实例多线程并发调用。
 *
 * <p><b>非 Spring Bean：</b>遵循 {@link com.puchain.fep.collector.support.InMemoryWatermarkStore}
 * 先例不加 {@code @Component}；由调用方（{@code AdapterFactory} / 配置驱动装配）
 * 显式 new 出来。
 *
 * @author FEP Team
 * @since 1.0.0
 */
public final class JdbcCollectorAdapter implements CollectorAdapter {

    /** "null" 字面值常量 — null cursor 列退化处理时的 sourceRef。 */
    private static final String NULL_LITERAL = "null";

    private final JdbcAdapterConfig config;
    private final Map<String, NamedParameterJdbcTemplate> jdbcTemplates;
    private final WatermarkStore watermarkStore;

    /**
     * 构造 JDBC 采集适配器。
     *
     * @param config         配置（非 null）
     * @param jdbcTemplates  数据源路由 Map（非 null；key = bean name，value = template）
     * @param watermarkStore 水位存储（非 null）
     * @throws NullPointerException 任一参数为 null
     */
    public JdbcCollectorAdapter(
            final JdbcAdapterConfig config,
            final Map<String, NamedParameterJdbcTemplate> jdbcTemplates,
            final WatermarkStore watermarkStore) {
        this.config = Objects.requireNonNull(config, "config");
        // 防御拷贝：外部修改 Map 不影响内部状态
        this.jdbcTemplates = Map.copyOf(
                Objects.requireNonNull(jdbcTemplates, "jdbcTemplates"));
        this.watermarkStore = Objects.requireNonNull(watermarkStore, "watermarkStore");
    }

    @Override
    public String getId() {
        return config.adapterId();
    }

    @Override
    public AdapterType getType() {
        return AdapterType.JDBC;
    }

    @Override
    public List<CollectionRecord> collect(final CollectionRunContext context) {
        Objects.requireNonNull(context, "context");
        final String watermark = watermarkStore.get(getId())
                .orElse(config.initialWatermark());
        final NamedParameterJdbcTemplate jdbc = jdbcTemplates.get(config.dataSourceBeanName());
        if (jdbc == null) {
            throw new FepBusinessException(
                    FepErrorCode.COLLECT_ADAPTER_FAILURE,
                    "datasource bean not found: "
                            + LogSanitizer.sanitize(config.dataSourceBeanName()));
        }
        final Map<String, Object> params = Map.of(
                "cursor", watermark,
                "batch_size", context.batchSize());
        final Instant collectedAt = Instant.now();
        try {
            final List<CollectionRecord> rows = jdbc.query(
                    config.sql(),
                    params,
                    (rs, rowNum) -> mapRow(rs, collectedAt));
            return List.copyOf(rows);
        } catch (DataAccessException e) {
            throw new FepBusinessException(
                    FepErrorCode.COLLECT_ADAPTER_FAILURE,
                    "JDBC query failed for adapter " + LogSanitizer.sanitize(getId()),
                    e);
        }
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
        // 取本批 cursor 列最大值（字典序）— 推进 watermark
        // null cursor 行（sourceRef = "null"）排除在 max 计算外（Plan §T2 #6 "watermark 不退"）—
        // 避免字面值 "null"（ASCII 'n' = 0x6E）拔高合法 ISO-8601 / 数字水位
        // T10 Simplify Q-3 fix: 空白 cursor 同样跳过 — 落到 watermarkStore.put 会触发
        // IllegalArgumentException（JpaWatermarkStore 已加 isBlank 守护），但在 adapter
        // 层先过滤可保留批量内其它有效行的水位推进语义。
        String maxCursor = null;
        for (final CollectionRecord record : records) {
            final String cursorAsString = record.getSourceRef();
            if (cursorAsString == null
                    || cursorAsString.isBlank()
                    || NULL_LITERAL.equals(cursorAsString)) {
                continue;
            }
            if (maxCursor == null || cursorAsString.compareTo(maxCursor) > 0) {
                maxCursor = cursorAsString;
            }
        }
        if (maxCursor != null) {
            watermarkStore.put(getId(), maxCursor);
        }
    }

    /**
     * 将 {@link ResultSet} 单行映射为 {@link CollectionRecord}。
     *
     * <p><b>null 列值处理：</b>{@link Map#copyOf}（{@link CollectionRecord} 内部使用）
     * 不接受 null value，因此本方法<b>跳过</b> ResultSet 中值为 null 的列 — 下游
     * {@code PayloadAssembler} 看到列缺失即视为 null。cursor 列的 null 单独捕获用于
     * sourceRef 退化（{@link #NULL_LITERAL}）。
     *
     * @param rs          ResultSet（已定位到当前行）
     * @param collectedAt 采集时刻（与本批次共享，避免行间时间漂移）
     * @return 映射后的 {@link CollectionRecord}
     * @throws SQLException ResultSet 元数据访问失败
     */
    private CollectionRecord mapRow(final ResultSet rs, final Instant collectedAt) throws SQLException {
        final ResultSetMetaData meta = rs.getMetaData();
        final int columnCount = meta.getColumnCount();
        // LinkedHashMap 保留列序方便日志/调试可读性
        final Map<String, Object> rawData = new LinkedHashMap<>();
        Object cursorValue = null;
        for (int i = 1; i <= columnCount; i++) {
            final String columnLabel = meta.getColumnLabel(i);
            final Object columnValue = rs.getObject(i);
            // cursor 列的 null 值单独保留以决定 sourceRef，不进 rawData
            if (columnLabel.equals(config.cursorColumn())) {
                cursorValue = columnValue;
            }
            // 跳过 null 列（Map.copyOf 不接受 null value，CollectionRecord.Builder 内部强制）
            if (columnValue != null) {
                rawData.put(columnLabel, columnValue);
            }
        }
        // null cursor 退化为字面值 "null" — 与 String.valueOf(null) 一致
        final String sourceRef = cursorValue == null ? NULL_LITERAL : String.valueOf(cursorValue);
        final String idempotencyKey = IdempotencyKeyGenerator.generate(getId(), sourceRef);
        return CollectionRecord.builder()
                .adapterId(getId())
                .sourceRef(sourceRef)
                .payloadDataType(config.payloadDataType())
                .rawData(rawData)
                .collectedAt(collectedAt)
                .idempotencyKey(idempotencyKey)
                .build();
    }
}
