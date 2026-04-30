package com.puchain.fep.collector.adapter.file;

import com.puchain.fep.collector.support.AdapterType;
import com.puchain.fep.collector.support.CollectionMetrics;
import com.puchain.fep.collector.support.CollectionRecord;
import com.puchain.fep.collector.support.CollectionRunContext;
import com.puchain.fep.collector.support.TriggerType;
import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link FileCollectorAdapter} 单元测试（CSV+XLSX, UTF-8, .processing lock, archive）。
 *
 * <p>覆盖 Plan §T3 验收标准：
 * <ul>
 *   <li>#4 UTF-8 中文 CSV — 「湖南某某科技有限公司」断言</li>
 *   <li>#5 文件锁防并发 — 已存在 .processing 跳过；FileAlreadyExistsException 不抛错</li>
 *   <li>#6 单文件解析失败 → 改名为 .failed-{timestamp} + metrics.failed++ + 不阻塞</li>
 *   <li>archive — acknowledge 移动到 archiveDirectory（带 timestamp 前缀）+ 删锁</li>
 *   <li>batch size — 严格遵循 ctx.batchSize() 不超额</li>
 *   <li>getType() 契约 — 返回 AdapterType.FILE</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class FileCollectorAdapterTest {

    private static final String ADAPTER_ID = "FILE_INVOICE_TEST";
    private static final String PAYLOAD_DATA_TYPE = "INVOICE_TEST_3101";
    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final String LOCK_SUFFIX = ".processing";

    /**
     * Plan §T3 #4 — UTF-8 中文 CSV 解析验收：buyer_name 必须包含「湖南某某科技有限公司」。
     */
    @Test
    void shouldParseUtf8ChineseCsv(@TempDir final Path watchDir, @TempDir final Path archiveDir)
            throws IOException {
        final String csv = "invoice_id,buyer_name,amount\n"
                + "123,湖南某某科技有限公司,500.00\n"
                + "456,北京某某有限公司,1000.00\n";
        Files.writeString(watchDir.resolve("invoices.csv"), csv, StandardCharsets.UTF_8);

        final FileCollectorAdapter adapter = newCsvAdapter(watchDir, archiveDir);
        final List<CollectionRecord> records = adapter.collect(ctx());

        assertThat(records).as("两行 CSV → 两条记录").hasSize(2);
        assertThat(records.get(0).getRawData())
                .as("Plan §T3 #4 keystone — UTF-8 中文 buyer_name 完整保留")
                .containsEntry("buyer_name", "湖南某某科技有限公司")
                .containsEntry("invoice_id", "123")
                .containsEntry("amount", "500.00");
        assertThat(records.get(0).getSourceRef())
                .as("sourceRef = filename#row=N（数据起始行 = row 2，header 为 row 1）")
                .isEqualTo("invoices.csv#row=2");
        assertThat(records.get(1).getSourceRef()).isEqualTo("invoices.csv#row=3");
        assertThat(records.get(0).getPayloadDataType()).isEqualTo(PAYLOAD_DATA_TYPE);
        assertThat(records.get(0).getAdapterId()).isEqualTo(ADAPTER_ID);
    }

    /**
     * Plan §T3 #5 — 已存在 .processing 锁 → 文件被跳过。
     */
    @Test
    void shouldSkipFilesWithExistingProcessingLock(
            @TempDir final Path watchDir, @TempDir final Path archiveDir) throws IOException {
        final String csv = "invoice_id,buyer_name,amount\n1,A,1.0\n";
        final Path data1 = watchDir.resolve("data1.csv");
        Files.writeString(data1, csv, StandardCharsets.UTF_8);
        // 预先创建 .processing 锁，模拟另一个进程正在处理
        Files.createFile(watchDir.resolve("data1.csv" + LOCK_SUFFIX));

        // 同时放一个无锁的合法文件，验证只跳过有锁的
        final Path data2 = watchDir.resolve("data2.csv");
        Files.writeString(data2, csv, StandardCharsets.UTF_8);

        final FileCollectorAdapter adapter = newCsvAdapter(watchDir, archiveDir);
        final List<CollectionRecord> records = adapter.collect(ctx());

        assertThat(records)
                .as("data1.csv 已被锁，data2.csv 无锁 — 只采集 data2 的 1 行")
                .hasSize(1);
        assertThat(records.get(0).getSourceRef()).startsWith("data2.csv#row=");
        assertThat(Files.exists(data1))
                .as("被锁的 data1.csv 应保留在 watchDir，未被采集")
                .isTrue();
    }

    /**
     * Plan §T3 #5 — race 条件下 FileAlreadyExistsException 静默跳过，不抛错。
     *
     * <p>场景：collect 进入扫描后、createFile 锁前另一进程抢先创建了锁。
     * 我们通过预创建 .processing 锁但不创建对应数据文件来近似 — adapter
     * 应在该文件上放弃并继续，整体不抛异常。
     */
    @Test
    void shouldNotThrowWhenLockAlreadyExists(
            @TempDir final Path watchDir, @TempDir final Path archiveDir) throws IOException {
        // 5 个数据文件 + 全部预创建锁（极端模拟全部 race lost）
        for (int i = 1; i <= 5; i++) {
            Files.writeString(watchDir.resolve("data" + i + ".csv"),
                    "h\nv\n", StandardCharsets.UTF_8);
            Files.createFile(watchDir.resolve("data" + i + ".csv" + LOCK_SUFFIX));
        }

        final FileCollectorAdapter adapter = newCsvAdapter(watchDir, archiveDir);
        // 不抛异常，返回空 list
        final List<CollectionRecord> records = adapter.collect(ctx());
        assertThat(records)
                .as("所有文件都已被锁 → 静默跳过 → 返回空列表（不抛异常）")
                .isEmpty();
    }

    /**
     * Plan §T3 #6 — 解析失败：文件改名为 .failed-{timestamp}，metrics.failed++，
     * 其他合法文件不受影响。
     */
    @Test
    void shouldRenameToFailedOnParseError(
            @TempDir final Path watchDir, @TempDir final Path archiveDir) throws IOException {
        // 合法 CSV
        final String goodCsv = "invoice_id,buyer_name,amount\n1,A,1.0\n";
        final Path good = watchDir.resolve("good.csv");
        Files.writeString(good, goodCsv, StandardCharsets.UTF_8);

        // 无效 UTF-8 字节序列 — commons-csv 读取时由 InputStreamReader 触发
        // MalformedInputException（通过 CharsetDecoder REPORT 模式）
        final Path bad = watchDir.resolve("bad.csv");
        // 0xC3 0x28 是无效的 UTF-8 二字节序列
        Files.write(bad, new byte[]{(byte) 0xC3, (byte) 0x28, (byte) 0x28, '\n'});

        final CollectionMetrics metrics = new CollectionMetrics();
        final FileCollectorAdapter adapter = newCsvAdapter(watchDir, archiveDir, metrics);
        final List<CollectionRecord> records = adapter.collect(ctx());

        // 合法文件仍被采集
        assertThat(records)
                .as("good.csv 应正常采集，bad.csv 不阻塞")
                .hasSize(1);
        assertThat(records.get(0).getSourceRef()).startsWith("good.csv#");

        // bad.csv 已重命名
        assertThat(Files.exists(bad))
                .as("bad.csv 应被重命名为 .failed-<timestamp>")
                .isFalse();
        try (Stream<Path> files = Files.list(watchDir)) {
            final long failedCount = files
                    .filter(p -> p.getFileName().toString().startsWith("bad.csv.failed-"))
                    .count();
            assertThat(failedCount).as(".failed-<ts> 文件存在").isEqualTo(1L);
        }
        // bad.csv 的 .processing 锁被清理
        assertThat(Files.exists(watchDir.resolve("bad.csv" + LOCK_SUFFIX)))
                .as(".processing 锁应被清理")
                .isFalse();
        // metrics.failed++
        assertThat(metrics.snapshot().failed())
                .as("Plan §T3 #6 — metrics.failed 必须 ++ 一次")
                .isEqualTo(1L);
    }

    /**
     * acknowledge — 文件移动到 archiveDir（带 timestamp 前缀），.processing 锁删除。
     */
    @Test
    void shouldArchiveFileOnAcknowledge(
            @TempDir final Path watchDir, @TempDir final Path archiveDir) throws IOException {
        final String csv = "invoice_id,buyer_name,amount\n1,A,1.0\n2,B,2.0\n";
        final Path data = watchDir.resolve("invoices.csv");
        Files.writeString(data, csv, StandardCharsets.UTF_8);

        final FileCollectorAdapter adapter = newCsvAdapter(watchDir, archiveDir);
        final List<CollectionRecord> records = adapter.collect(ctx());
        assertThat(records).hasSize(2);

        adapter.acknowledge(ctx(), records);

        assertThat(Files.exists(data))
                .as("原文件应已从 watchDir 移除")
                .isFalse();
        assertThat(Files.exists(watchDir.resolve("invoices.csv" + LOCK_SUFFIX)))
                .as(".processing 锁应被删除")
                .isFalse();
        try (Stream<Path> files = Files.list(archiveDir)) {
            final List<Path> archived = files.toList();
            assertThat(archived)
                    .as("archive 中应有一个带 timestamp 前缀的归档文件")
                    .hasSize(1);
            assertThat(archived.get(0).getFileName().toString())
                    .as("归档文件名格式 = <timestamp>_<originalName>")
                    .endsWith("_invoices.csv")
                    .matches("\\d{17}_invoices\\.csv");
        }
    }

    /**
     * 批次大小约束 — 5 个文件、batchSize=2 → 最多采集 2 个文件的内容。
     */
    @Test
    void shouldRespectBatchSize(
            @TempDir final Path watchDir, @TempDir final Path archiveDir) throws IOException {
        // 5 个文件，每个 2 数据行
        for (int i = 1; i <= 5; i++) {
            Files.writeString(watchDir.resolve("data" + i + ".csv"),
                    "h\n1\n2\n", StandardCharsets.UTF_8);
        }

        final FileCollectorAdapter adapter = newCsvAdapter(watchDir, archiveDir);
        final CollectionRunContext ctxBatch2 = new CollectionRunContext(
                UUID.randomUUID().toString().replace("-", ""),
                ADAPTER_ID,
                TriggerType.SCHEDULED,
                Optional.empty(),
                Instant.now(),
                2);
        final List<CollectionRecord> records = adapter.collect(ctxBatch2);

        // batch=2 → 取 2 个文件 × 每个 2 行 = 4 条记录
        assertThat(records)
                .as("batchSize=2 → 最多取 2 个文件的全部数据行（共 4 条记录）")
                .hasSize(4);
        // 验证只覆盖了 2 个不同的源文件
        final long uniqueFiles = records.stream()
                .map(r -> r.getSourceRef().split("#")[0])
                .distinct()
                .count();
        assertThat(uniqueFiles)
                .as("仅覆盖 2 个不同的源文件")
                .isEqualTo(2L);
    }

    /**
     * Adapter 类型契约 — getType() 返回 FILE。
     */
    @Test
    void getTypeShouldReturnFile(@TempDir final Path watchDir, @TempDir final Path archiveDir) {
        final FileCollectorAdapter adapter = newCsvAdapter(watchDir, archiveDir);
        assertThat(adapter.getType())
                .as("FileCollectorAdapter.getType() 必须返回 AdapterType.FILE")
                .isEqualTo(AdapterType.FILE);
        assertThat(adapter.getId()).isEqualTo(ADAPTER_ID);
    }

    /**
     * acknowledge 失败必须包装为 COLLECT_ADAPTER_FAILURE — Plan §T3 #3 ack 失败不可静默吞。
     *
     * <p>触发方式：archive 路径已被一个普通文件占用（非目录），
     * {@link Files#createDirectories} 会抛 {@link java.nio.file.FileAlreadyExistsException}。
     */
    @Test
    void acknowledgeShouldThrowWhenArchiveDirCannotBeCreated(@TempDir final Path watchDir)
            throws IOException {
        // 将 archive 路径预先创建为普通文件 — 触发 createDirectories 失败
        final Path archiveAsFile = watchDir.resolve("archive_taken_by_file");
        Files.writeString(archiveAsFile, "I am a file, not a dir", StandardCharsets.UTF_8);
        final Path data = watchDir.resolve("data.csv");
        Files.writeString(data, "h\nv\n", StandardCharsets.UTF_8);
        final FileAdapterConfig config = FileAdapterConfig.withDefaults(
                ADAPTER_ID, PAYLOAD_DATA_TYPE, watchDir, archiveAsFile, FileFormat.CSV);
        final FileCollectorAdapter adapter = new FileCollectorAdapter(config, new CollectionMetrics());
        final List<CollectionRecord> records = adapter.collect(ctx());
        // 仅 data.csv 进入候选（archive_taken_by_file 不匹配 .csv 后缀，已被 scanCandidates 排除）
        assertThat(records).hasSize(1);

        assertThatThrownBy(() -> adapter.acknowledge(ctx(), records))
                .isInstanceOf(FepBusinessException.class)
                .extracting("errorCode")
                .isEqualTo(FepErrorCode.COLLECT_ADAPTER_FAILURE);
    }

    /**
     * 空目录 — 返回空列表，不抛异常。
     */
    @Test
    void emptyDirectoryShouldReturnEmpty(
            @TempDir final Path watchDir, @TempDir final Path archiveDir) {
        final FileCollectorAdapter adapter = newCsvAdapter(watchDir, archiveDir);
        assertThat(adapter.collect(ctx()))
                .as("空目录返回空列表")
                .isEmpty();
    }

    /**
     * 非目标格式文件被忽略 — 例如 *.txt 在 CSV 模式下应跳过。
     */
    @Test
    void shouldIgnoreNonMatchingExtensions(
            @TempDir final Path watchDir, @TempDir final Path archiveDir) throws IOException {
        Files.writeString(watchDir.resolve("readme.txt"), "ignore me", StandardCharsets.UTF_8);
        Files.writeString(watchDir.resolve("data.json"), "{}", StandardCharsets.UTF_8);
        Files.writeString(watchDir.resolve("data.csv"), "h\n1\n", StandardCharsets.UTF_8);

        final FileCollectorAdapter adapter = newCsvAdapter(watchDir, archiveDir);
        final List<CollectionRecord> records = adapter.collect(ctx());
        assertThat(records)
                .as("仅 .csv 文件被采集，.txt / .json 被忽略")
                .hasSize(1);
        assertThat(records.get(0).getSourceRef()).startsWith("data.csv#");
    }

    /**
     * Config 工厂方法 — withDefaults 必须填充 UTF-8 + ',' 默认值。
     */
    @Test
    void configWithDefaultsShouldUseUtf8AndComma(@TempDir final Path watchDir, @TempDir final Path archiveDir) {
        final FileAdapterConfig config = FileAdapterConfig.withDefaults(
                ADAPTER_ID, PAYLOAD_DATA_TYPE, watchDir, archiveDir, FileFormat.CSV);
        assertThat(config.charset())
                .as("withDefaults 必须填充 UTF-8")
                .isEqualTo(StandardCharsets.UTF_8);
        assertThat(config.csvSeparator())
                .as("withDefaults 必须填充 ','")
                .isEqualTo(',');
        assertThat(config.fileFormat()).isEqualTo(FileFormat.CSV);
    }

    /**
     * Config — watchDirectory == archiveDirectory 必须拒绝（防止归档回写水池）。
     */
    @Test
    void configShouldRejectSameWatchAndArchiveDir(@TempDir final Path same) {
        assertThatThrownBy(() -> FileAdapterConfig.withDefaults(
                ADAPTER_ID, PAYLOAD_DATA_TYPE, same, same, FileFormat.CSV))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("watchDirectory");
    }

    /**
     * FileFormat — getExtension() 契约。
     */
    @Test
    void fileFormatExtensionShouldMatchEnum() {
        assertThat(FileFormat.CSV.getExtension()).isEqualTo(".csv");
        assertThat(FileFormat.XLSX.getExtension()).isEqualTo(".xlsx");
    }

    /**
     * XLSX 解析 — 三列表头 + 两数据行 + 一空行（应跳过），含中文 buyer_name。
     */
    @Test
    void shouldParseXlsxFile(@TempDir final Path watchDir, @TempDir final Path archiveDir)
            throws IOException {
        final Path xlsx = watchDir.resolve("invoices.xlsx");
        try (XSSFWorkbook wb = new XSSFWorkbook(); OutputStream out = Files.newOutputStream(xlsx)) {
            final Sheet sheet = wb.createSheet("data");
            final Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("invoice_id");
            header.createCell(1).setCellValue("buyer_name");
            header.createCell(2).setCellValue("amount");
            final Row r1 = sheet.createRow(1);
            r1.createCell(0).setCellValue("100");
            r1.createCell(1).setCellValue("湖南某某科技有限公司");
            r1.createCell(2).setCellValue("500.00");
            final Row r2 = sheet.createRow(2);
            r2.createCell(0).setCellValue("200");
            r2.createCell(1).setCellValue("Beijing Co");
            r2.createCell(2).setCellValue("1200");
            // sheet.createRow(3) 留空 — 验证 null row 跳过
            wb.write(out);
        }

        final FileAdapterConfig config = FileAdapterConfig.withDefaults(
                ADAPTER_ID, PAYLOAD_DATA_TYPE, watchDir, archiveDir, FileFormat.XLSX);
        final FileCollectorAdapter adapter = new FileCollectorAdapter(config, new CollectionMetrics());
        final List<CollectionRecord> records = adapter.collect(ctx());
        assertThat(records)
                .as("XLSX 两条数据行")
                .hasSize(2);
        assertThat(records.get(0).getRawData())
                .as("UTF-8 中文 buyer_name 在 XLSX 同样保留")
                .containsEntry("buyer_name", "湖南某某科技有限公司")
                .containsEntry("invoice_id", "100");
        assertThat(records.get(0).getSourceRef()).isEqualTo("invoices.xlsx#row=2");
    }

    /**
     * 损坏 XLSX → 解析失败 → .failed-<ts> 重命名 + metrics.failed++。
     * 同时覆盖 RuntimeException catch 分支（POI 抛 NotOfficeXmlFileException 等）。
     */
    @Test
    void shouldRenameCorruptXlsxToFailed(@TempDir final Path watchDir, @TempDir final Path archiveDir)
            throws IOException {
        final Path bad = watchDir.resolve("bad.xlsx");
        Files.writeString(bad, "this is not a valid xlsx", StandardCharsets.UTF_8);

        final CollectionMetrics metrics = new CollectionMetrics();
        final FileAdapterConfig config = FileAdapterConfig.withDefaults(
                ADAPTER_ID, PAYLOAD_DATA_TYPE, watchDir, archiveDir, FileFormat.XLSX);
        final FileCollectorAdapter adapter = new FileCollectorAdapter(config, metrics);

        final List<CollectionRecord> records = adapter.collect(ctx());
        assertThat(records).as("损坏 XLSX 不应产出记录").isEmpty();
        try (Stream<Path> files = Files.list(watchDir)) {
            final long failedCount = files
                    .filter(p -> p.getFileName().toString().startsWith("bad.xlsx.failed-"))
                    .count();
            assertThat(failedCount).isEqualTo(1L);
        }
        assertThat(metrics.snapshot().failed()).isEqualTo(1L);
    }

    /**
     * watchDirectory 不存在 → 返回空列表（不抛异常，符合采集循环弹性预期）。
     */
    @Test
    void missingWatchDirShouldReturnEmptyNotThrow(@TempDir final Path archiveDir) {
        final Path nonExistent = archiveDir.resolve("does-not-exist-yet");
        final FileAdapterConfig config = FileAdapterConfig.withDefaults(
                ADAPTER_ID, PAYLOAD_DATA_TYPE, nonExistent, archiveDir, FileFormat.CSV);
        final FileCollectorAdapter adapter = new FileCollectorAdapter(config, new CollectionMetrics());
        assertThat(adapter.collect(ctx()))
                .as("watchDir 不存在 → 空 List，不抛异常")
                .isEmpty();
    }

    /**
     * 空 records ack → no-op，不抛异常，归档目录无变化。
     */
    @Test
    void emptyAcknowledgeShouldBeNoOp(@TempDir final Path watchDir, @TempDir final Path archiveDir)
            throws IOException {
        final FileCollectorAdapter adapter = newCsvAdapter(watchDir, archiveDir);
        adapter.acknowledge(ctx(), List.of());
        try (Stream<Path> archived = Files.list(archiveDir)) {
            assertThat(archived.count())
                    .as("空 ack 不应产生归档文件")
                    .isZero();
        }
    }

    private FileCollectorAdapter newCsvAdapter(final Path watchDir, final Path archiveDir) {
        return newCsvAdapter(watchDir, archiveDir, new CollectionMetrics());
    }

    private FileCollectorAdapter newCsvAdapter(final Path watchDir, final Path archiveDir,
                                               final CollectionMetrics metrics) {
        final FileAdapterConfig config = FileAdapterConfig.withDefaults(
                ADAPTER_ID, PAYLOAD_DATA_TYPE, watchDir, archiveDir, FileFormat.CSV);
        return new FileCollectorAdapter(config, metrics);
    }

    private CollectionRunContext ctx() {
        return new CollectionRunContext(
                UUID.randomUUID().toString().replace("-", ""),
                ADAPTER_ID,
                TriggerType.SCHEDULED,
                Optional.empty(),
                Instant.now(),
                DEFAULT_BATCH_SIZE);
    }
}
