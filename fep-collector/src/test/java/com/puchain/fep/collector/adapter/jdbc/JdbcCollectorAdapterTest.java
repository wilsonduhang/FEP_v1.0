package com.puchain.fep.collector.adapter.jdbc;

import com.puchain.fep.collector.support.CollectionRecord;
import com.puchain.fep.collector.support.CollectionRunContext;
import com.puchain.fep.collector.support.AdapterType;
import com.puchain.fep.collector.support.IdempotencyKeyGenerator;
import com.puchain.fep.collector.support.InMemoryWatermarkStore;
import com.puchain.fep.collector.support.TriggerType;
import com.puchain.fep.collector.support.WatermarkStore;
import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link JdbcCollectorAdapter} 单元测试（H2 内存库 + 手动 schema setup）。
 *
 * <p>覆盖 Plan §T2 验收标准：
 * <ul>
 *   <li>#4 断点续采三轮：100 条 → 0 条 → 50 条</li>
 *   <li>#5 异常路径：DataSource 不存在 → COLLECT_ADAPTER_FAILURE；SQL 异常 → 包装抛 + cause 保留</li>
 *   <li>#6 null cursor 值处理：sourceRef = "null"，watermark 不退</li>
 *   <li>幂等键稳定性：同行多次采集得相同 key</li>
 * </ul>
 *
 * <p>未引入 Testcontainers（Plan §T2 #7 显式延后），使用 H2 PostgreSQL 兼容模式。
 *
 * @author FEP Team
 * @since 1.0.0
 */
class JdbcCollectorAdapterTest {

    private static final String DS_BEAN_NAME = "ds_test";
    private static final String ADAPTER_ID = "JDBC_INVOICE_TEST";
    /**
     * 用 invoice_id (BIGINT) 作为 cursor 列 — schema 中通过 VARCHAR 形式 cursor_key
     * 列做字典序水位（零填充 20 位保证与数值序一致）。
     */
    private static final String CURSOR_COLUMN = "cursor_key";
    private static final String PAYLOAD_DATA_TYPE = "INVOICE_TEST_3101";
    /** 初始水位 — 20 位零填充的字典序最小值。 */
    private static final String INITIAL_WATERMARK = "00000000000000000000";
    /** 命名参数 SQL — :cursor 字典序比较（VARCHAR），:batch_size 单次取数上限。 */
    private static final String SQL = "SELECT invoice_id, buyer_name, amount, created_at, cursor_key "
            + "FROM biz_invoice WHERE cursor_key > :cursor "
            + "ORDER BY cursor_key ASC LIMIT :batch_size";
    /** 默认批次大小（足以一次拉满 100 条） */
    private static final int DEFAULT_BATCH_SIZE = 200;

    private JdbcTemplate setupJdbc;
    private NamedParameterJdbcTemplate jdbcTemplate;
    private WatermarkStore watermarkStore;
    private JdbcCollectorAdapter adapter;
    /** 单调递增计数器 — 保证每行 created_at 唯一（H2 TIMESTAMP 精度毫秒） */
    private long timestampCounter;

    @BeforeEach
    void setUp() throws Exception {
        // 每个 @Test 用独立 H2 实例（DB_CLOSE_DELAY=-1 + 唯一 db name）避免串扰
        final String dbName = "test_t2_" + UUID.randomUUID().toString().replace("-", "");
        // DATABASE_TO_LOWER=TRUE 让 ResultSetMetaData 列名小写（与 cursorColumn 配置一致）
        final DataSource dataSource = new SimpleDriverDataSource(
                new org.h2.Driver(),
                "jdbc:h2:mem:" + dbName
                        + ";DB_CLOSE_DELAY=-1;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
                "sa", "");
        this.setupJdbc = new JdbcTemplate(dataSource);
        // classpath 加载 — 让 IDE / 任意 cwd 都能正确定位 schema 资源
        final String ddl = Files.readString(
                Path.of(getClass().getClassLoader()
                        .getResource("jdbc-adapter-test-schema.sql")
                        .toURI()));
        // H2 不支持 multi-statement 用 execute(String) 时分号分隔 — 拆分逐句执行
        for (final String stmt : ddl.split(";")) {
            final String trimmed = stmt.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                setupJdbc.execute(trimmed);
            }
        }
        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        this.watermarkStore = new InMemoryWatermarkStore();
        this.timestampCounter = 0L;
        final JdbcAdapterConfig config = new JdbcAdapterConfig(
                DS_BEAN_NAME, SQL, CURSOR_COLUMN, INITIAL_WATERMARK,
                ADAPTER_ID, PAYLOAD_DATA_TYPE);
        final Map<String, NamedParameterJdbcTemplate> templates = Map.of(DS_BEAN_NAME, jdbcTemplate);
        this.adapter = new JdbcCollectorAdapter(config, templates, watermarkStore);
    }

    /**
     * Plan §T2 #4 — 断点续采三轮：第一次 100 条 → 第二次 0 条 → 新增 50 条第三次 50 条。
     */
    @Test
    void shouldCollectIncrementally_andAdvanceWatermark() {
        insertRows(100);

        final List<CollectionRecord> batch1 = adapter.collect(ctx());
        assertThat(batch1)
                .as("首轮取 100 条")
                .hasSize(100);
        adapter.acknowledge(ctx(), batch1);

        final List<CollectionRecord> batch2 = adapter.collect(ctx());
        assertThat(batch2)
                .as("第二轮无新增 → 0 条")
                .isEmpty();

        insertRows(50);
        final List<CollectionRecord> batch3 = adapter.collect(ctx());
        assertThat(batch3)
                .as("新增 50 条 → 第三轮取 50 条")
                .hasSize(50);
    }

    /**
     * Plan §T2 #5 — DataSource Bean 不存在 → COLLECT_ADAPTER_FAILURE。
     */
    @Test
    void shouldThrowWhenDataSourceBeanNotFound() {
        final JdbcAdapterConfig badConfig = new JdbcAdapterConfig(
                "nonexistent_ds", SQL, CURSOR_COLUMN, INITIAL_WATERMARK,
                ADAPTER_ID, PAYLOAD_DATA_TYPE);
        final JdbcCollectorAdapter badAdapter = new JdbcCollectorAdapter(
                badConfig, Map.of(DS_BEAN_NAME, jdbcTemplate), watermarkStore);

        assertThatThrownBy(() -> badAdapter.collect(ctx()))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("nonexistent_ds")
                .extracting("errorCode")
                .isEqualTo(FepErrorCode.COLLECT_ADAPTER_FAILURE);
    }

    /**
     * Plan §T2 #5 — SQL 异常包装抛 + 保留 root cause。
     */
    @Test
    void shouldWrapSqlExceptionAsCollectAdapterFailure() {
        final JdbcAdapterConfig invalidSqlConfig = new JdbcAdapterConfig(
                DS_BEAN_NAME,
                "SELECT * FROM table_that_does_not_exist WHERE id > :cursor LIMIT :batch_size",
                CURSOR_COLUMN, INITIAL_WATERMARK, ADAPTER_ID, PAYLOAD_DATA_TYPE);
        final JdbcCollectorAdapter invalidAdapter = new JdbcCollectorAdapter(
                invalidSqlConfig, Map.of(DS_BEAN_NAME, jdbcTemplate), watermarkStore);

        assertThatThrownBy(() -> invalidAdapter.collect(ctx()))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("JDBC query failed")
                .extracting("errorCode")
                .isEqualTo(FepErrorCode.COLLECT_ADAPTER_FAILURE);

        // 验证 root cause 链路保留 DataAccessException
        try {
            invalidAdapter.collect(ctx());
        } catch (FepBusinessException e) {
            assertThat(e.getCause())
                    .as("DataAccessException 必须作为 cause 保留")
                    .isInstanceOf(DataAccessException.class);
        }
    }

    /**
     * Plan §T2 #6 — null cursor 值：sourceRef = "null"，watermark 不退（不被 null 拔高）。
     */
    @Test
    void shouldHandleNullCursorValue() {
        // 用 buyer_name（VARCHAR 可为 null）作为 cursor 列模拟 nullable 场景；
        // SQL filter 用 invoice_id 保证 null cursor 行也能被取到
        final String nullableSql = "SELECT invoice_id, buyer_name, amount, created_at, cursor_key "
                + "FROM biz_invoice WHERE invoice_id > CAST(:cursor AS BIGINT) "
                + "ORDER BY invoice_id ASC LIMIT :batch_size";
        final JdbcAdapterConfig nullableCursorConfig = new JdbcAdapterConfig(
                DS_BEAN_NAME, nullableSql, "buyer_name", "0",
                ADAPTER_ID + "_NULL", PAYLOAD_DATA_TYPE);
        final WatermarkStore nullStore = new InMemoryWatermarkStore();
        final JdbcCollectorAdapter nullableAdapter = new JdbcCollectorAdapter(
                nullableCursorConfig, Map.of(DS_BEAN_NAME, jdbcTemplate), nullStore);

        // 插入一行 buyer_name = NULL
        setupJdbc.update(
                "INSERT INTO biz_invoice (invoice_id, buyer_name, amount, created_at, cursor_key) "
                        + "VALUES (?, ?, ?, ?, ?)",
                999L, null, new java.math.BigDecimal("1.00"),
                Timestamp.from(Instant.parse("2026-04-30T00:00:00Z")),
                "00000000000000000999");

        final List<CollectionRecord> rows = nullableAdapter.collect(
                runContext(ADAPTER_ID + "_NULL"));
        assertThat(rows)
                .as("null cursor 行应被采集（不被静默丢弃）")
                .hasSize(1);
        final CollectionRecord nullRow = rows.get(0);
        assertThat(nullRow.getSourceRef())
                .as("null cursor 列 → sourceRef = \"null\" 字面值")
                .isEqualTo("null");
        assertThat(nullRow.getRawData())
                .as("null cursor 列不应出现在 rawData（Map.copyOf 不接受 null value）")
                .doesNotContainKey("buyer_name");

        // ack 后 watermark 仍为 empty（"null" 字面值排除在 max 计算外，不拔高水位）
        nullableAdapter.acknowledge(runContext(ADAPTER_ID + "_NULL"), rows);
        assertThat(nullStore.get(ADAPTER_ID + "_NULL"))
                .as("Plan §T2 #6 watermark 不退：仅 null cursor 行 → watermark 不推进")
                .isEmpty();
    }

    /**
     * 幂等键稳定性 — 同一行采集两次得到相同 key。
     */
    @Test
    void idempotencyKeyShouldBeStableAcrossRuns() {
        insertRows(1);
        final List<CollectionRecord> first = adapter.collect(ctx());
        // 不 ack，让第二次也能取到同一条
        final List<CollectionRecord> second = adapter.collect(ctx());

        assertThat(first).hasSize(1);
        assertThat(second).hasSize(1);
        assertThat(first.get(0).getIdempotencyKey())
                .as("同 (adapterId, sourceRef) 多次 collect 必须得相同 idempotencyKey")
                .isEqualTo(second.get(0).getIdempotencyKey())
                .matches("[0-9a-f]{32}");

        // 与 IdempotencyKeyGenerator 直接计算一致
        final String expected = IdempotencyKeyGenerator.generate(
                ADAPTER_ID, first.get(0).getSourceRef());
        assertThat(first.get(0).getIdempotencyKey())
                .as("idempotencyKey 必须等于 generator 直接计算结果")
                .isEqualTo(expected);
    }

    /**
     * 混合批次 null cursor — 同一批含 null + 多个非 null cursor，watermark 必须推进到
     * 非 null 中的最大值，且 "null" 不被误用（Plan §T2 #6 mandate / quality review HIGH）。
     */
    @Test
    void mixedBatchWithNullCursorShouldAdvanceToMaxNonNull() {
        final String nullableSql = "SELECT invoice_id, buyer_name, amount, created_at, cursor_key "
                + "FROM biz_invoice WHERE invoice_id > CAST(:cursor AS BIGINT) "
                + "ORDER BY invoice_id ASC LIMIT :batch_size";
        final JdbcAdapterConfig nullableConfig = new JdbcAdapterConfig(
                DS_BEAN_NAME, nullableSql, "buyer_name", "0",
                ADAPTER_ID + "_MIX", PAYLOAD_DATA_TYPE);
        final WatermarkStore mixStore = new InMemoryWatermarkStore();
        final JdbcCollectorAdapter mixAdapter = new JdbcCollectorAdapter(
                nullableConfig, Map.of(DS_BEAN_NAME, jdbcTemplate), mixStore);

        // 插三行：cursor (buyer_name) = "buyer_A" / NULL / "buyer_Z"
        // SQL filter 用 invoice_id 保证 null cursor 行也能被取到
        setupJdbc.update("INSERT INTO biz_invoice (invoice_id, buyer_name, amount, created_at, cursor_key) "
                        + "VALUES (?, ?, ?, ?, ?)",
                1L, "buyer_A", new java.math.BigDecimal("1.00"),
                Timestamp.from(Instant.parse("2026-04-30T00:00:00Z")),
                "00000000000000000001");
        setupJdbc.update("INSERT INTO biz_invoice (invoice_id, buyer_name, amount, created_at, cursor_key) "
                        + "VALUES (?, ?, ?, ?, ?)",
                2L, null, new java.math.BigDecimal("2.00"),
                Timestamp.from(Instant.parse("2026-04-30T00:00:01Z")),
                "00000000000000000002");
        setupJdbc.update("INSERT INTO biz_invoice (invoice_id, buyer_name, amount, created_at, cursor_key) "
                        + "VALUES (?, ?, ?, ?, ?)",
                3L, "buyer_Z", new java.math.BigDecimal("3.00"),
                Timestamp.from(Instant.parse("2026-04-30T00:00:02Z")),
                "00000000000000000003");

        final List<CollectionRecord> mixed = mixAdapter.collect(runContext(ADAPTER_ID + "_MIX"));
        assertThat(mixed).hasSize(3);
        mixAdapter.acknowledge(runContext(ADAPTER_ID + "_MIX"), mixed);

        assertThat(mixStore.get(ADAPTER_ID + "_MIX"))
                .as("混合批次 watermark 必须推进到非 null 中最大值（"
                        + "排除 \"null\" 哨兵；Plan §T2 #6 + quality HIGH）")
                .contains("buyer_Z");
    }

    /**
     * acknowledge max-watermark 比较 — 输入顺序非升序（先大后小）时，watermark 必须取
     * 最大值而非最后一条。覆盖 line 142 lower-cursor 分支（quality review MEDIUM）。
     */
    @Test
    void acknowledgeShouldKeepMaxRegardlessOfOrder() {
        // 手工构造非升序的 records 列表（绕过 SQL ORDER BY）
        final CollectionRecord highFirst = CollectionRecord.builder()
                .adapterId(ADAPTER_ID)
                .sourceRef("00000000000000000999")
                .payloadDataType(PAYLOAD_DATA_TYPE)
                .rawData(Map.of("cursor_key", "00000000000000000999"))
                .collectedAt(Instant.now())
                .idempotencyKey(IdempotencyKeyGenerator.generate(ADAPTER_ID, "00000000000000000999"))
                .build();
        final CollectionRecord lowSecond = CollectionRecord.builder()
                .adapterId(ADAPTER_ID)
                .sourceRef("00000000000000000100")
                .payloadDataType(PAYLOAD_DATA_TYPE)
                .rawData(Map.of("cursor_key", "00000000000000000100"))
                .collectedAt(Instant.now())
                .idempotencyKey(IdempotencyKeyGenerator.generate(ADAPTER_ID, "00000000000000000100"))
                .build();

        // 先 high 后 low — 触发 line 142 maxCursor != null && compareTo <= 0 分支
        adapter.acknowledge(ctx(), List.of(highFirst, lowSecond));

        assertThat(watermarkStore.get(ADAPTER_ID))
                .as("acknowledge 必须取最大值，不被后续 lower-cursor 行覆盖")
                .contains("00000000000000000999");
    }

    /**
     * Adapter type 暴露 — getType() 必须返回 JDBC（CollectorAdapter 接口契约）。
     */
    @Test
    void getTypeShouldReturnJdbc() {
        assertThat(adapter.getType())
                .as("JdbcCollectorAdapter.getType() 必须返回 AdapterType.JDBC")
                .isEqualTo(AdapterType.JDBC);
        assertThat(adapter.getId())
                .as("getId() 必须回显 config.adapterId()")
                .isEqualTo(ADAPTER_ID);
    }

    /**
     * Config 工厂方法 — withDefaults 必须填充 DEFAULT_INITIAL_WATERMARK。
     */
    @Test
    void configWithDefaultsShouldUseEpoch1970Watermark() {
        final JdbcAdapterConfig defaulted = JdbcAdapterConfig.withDefaults(
                "ds_x", "SELECT 1", "id", "ADP_X", "TYPE_X");

        assertThat(defaulted.initialWatermark())
                .as("withDefaults 必须填充 DEFAULT_INITIAL_WATERMARK = 1970-01-01T00:00:00Z")
                .isEqualTo(JdbcAdapterConfig.DEFAULT_INITIAL_WATERMARK)
                .isEqualTo("1970-01-01T00:00:00Z");
        assertThat(defaulted.dataSourceBeanName()).isEqualTo("ds_x");
        assertThat(defaulted.adapterId()).isEqualTo("ADP_X");
    }

    /**
     * 边界 — 空集合 ack 应 no-op，不动 watermark。
     */
    @Test
    void acknowledgeEmptyShouldBeNoOp() {
        final Optional<String> before = watermarkStore.get(ADAPTER_ID);
        adapter.acknowledge(ctx(), List.of());
        final Optional<String> after = watermarkStore.get(ADAPTER_ID);
        assertThat(after)
                .as("空 records ack 不应推进 watermark")
                .isEqualTo(before);
    }

    /**
     * 构造默认 ctx（adapter = ADAPTER_ID）。
     */
    private CollectionRunContext ctx() {
        return runContext(ADAPTER_ID);
    }

    /**
     * 构造指定 adapterId 的 ctx。
     */
    private CollectionRunContext runContext(final String adapterId) {
        return new CollectionRunContext(
                UUID.randomUUID().toString().replace("-", ""),
                adapterId,
                TriggerType.SCHEDULED,
                Optional.empty(),
                Instant.now(),
                DEFAULT_BATCH_SIZE);
    }

    /**
     * 插入 N 条测试数据 — invoice_id 单调递增 + cursor_key 零填充 20 位字典序对齐数值序。
     */
    private void insertRows(final int n) {
        final Instant base = Instant.parse("2026-04-30T00:00:00Z");
        for (int i = 0; i < n; i++) {
            // 单调递增计数器贯穿测试生命周期 — 跨多次 insertRows 调用不重复
            final long offset = ++timestampCounter;
            setupJdbc.update(
                    "INSERT INTO biz_invoice (invoice_id, buyer_name, amount, created_at, cursor_key) "
                            + "VALUES (?, ?, ?, ?, ?)",
                    offset,
                    "buyer_" + offset,
                    new java.math.BigDecimal("100.00"),
                    Timestamp.from(base.plus(offset, ChronoUnit.MILLIS)),
                    String.format("%020d", offset));
        }
    }
}
