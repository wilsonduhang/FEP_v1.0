package com.puchain.fep.collector.adapter.file;

import com.puchain.fep.collector.support.AdapterType;
import com.puchain.fep.collector.support.CollectionMetrics;
import com.puchain.fep.collector.support.CollectionRecord;
import com.puchain.fep.collector.support.CollectionRunContext;
import com.puchain.fep.collector.support.CollectorAdapter;
import com.puchain.fep.collector.support.IdempotencyKeyGenerator;
import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.LogSanitizer;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * 文件数据采集适配器（PRD v1.3 §2.2.2 数仓模式 / §3.6 UTF-8 编码约束）。
 *
 * <p><b>采集语义：</b>
 * <ol>
 *   <li>{@link #collect} 扫描 watchDirectory（不递归）匹配 fileFormat 后缀的文件</li>
 *   <li>跳过已存在 {@code .processing} 锁的文件（Plan §T3 #5）</li>
 *   <li>取 batchSize 个候选；为每个原子创建 {@code .processing} 锁
 *       （{@link Files#createFile} 原子）</li>
 *   <li>{@link FileAlreadyExistsException} 静默跳过（race lost，Plan §T3 #5）</li>
 *   <li>解析 CSV/XLSX 每行 → {@link CollectionRecord}
 *       （{@code sourceRef = filename#row=N}，header 行 = row 1）</li>
 *   <li>解析失败：重命名为 {@code <orig>.failed-<timestamp>}，删锁，
 *       {@link CollectionMetrics#incFailed} ++，记 warn 日志，<b>不阻塞其他文件</b>
 *       （Plan §T3 #6）</li>
 * </ol>
 *
 * <p>{@link #acknowledge} 将每个源文件移入 archiveDirectory（带 timestamp 前缀防重名），
 * 并删除其 {@code .processing} 锁。归档失败抛
 * {@link FepBusinessException}（{@link FepErrorCode#COLLECT_ADAPTER_FAILURE}），
 * 防止水位静默推进。
 *
 * <p><b>本 Plan 仅支持本地 POSIX FS</b>：SFTP/NFS 锁文件原子性不保证，相关支持
 * 延后到 Plan §Deferred D2。
 *
 * <p><b>线程安全：</b>本类无可变字段（全 final），可单实例多线程并发调用。
 * 锁文件保证多实例/多线程间互斥（POSIX 原子 createFile 语义）。
 *
 * <p><b>非 Spring Bean：</b>遵循 {@link com.puchain.fep.collector.adapter.jdbc.JdbcCollectorAdapter}
 * 先例不加 {@code @Component}，由调用方（{@code AdapterFactory} / 配置驱动装配）
 * 显式 new。
 *
 * @author FEP Team
 * @since 1.0.0
 */
public final class FileCollectorAdapter implements CollectorAdapter {

    /** 处理锁后缀。 */
    private static final String LOCK_SUFFIX = ".processing";

    /** 解析失败重命名前缀。 */
    private static final String FAILED_INFIX = ".failed-";

    /** 归档时间戳格式（紧凑 17 位 = yyyyMMddHHmmssSSS） — 防文件名 collision。 */
    private static final DateTimeFormatter ARCHIVE_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").withZone(ZoneOffset.UTC);

    /** 解析失败重命名时间戳格式（与归档同精度） — 同毫秒重复时附加随机 nano）。 */
    private static final DateTimeFormatter FAILED_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS").withZone(ZoneOffset.UTC);

    /** sourceRef 中文件名与行号分隔模式。 */
    private static final String SOURCE_REF_ROW_SEP = "#row=";

    /** XLSX 表头扫描的最大列数（防 POI getLastCellNum 异常拉伸）。 */
    private static final int XLSX_MAX_COLUMNS = 1024;

    private static final Logger log = LoggerFactory.getLogger(FileCollectorAdapter.class);

    private final FileAdapterConfig config;
    private final CollectionMetrics metrics;

    /**
     * 构造文件采集适配器。
     *
     * @param config  配置（非 null）
     * @param metrics 采集指标（非 null；解析失败时 {@code incFailed} 用）
     * @throws NullPointerException 任一参数为 null
     */
    public FileCollectorAdapter(final FileAdapterConfig config, final CollectionMetrics metrics) {
        this.config = Objects.requireNonNull(config, "config");
        this.metrics = Objects.requireNonNull(metrics, "metrics");
    }

    @Override
    public String getId() {
        return config.adapterId();
    }

    @Override
    public AdapterType getType() {
        return AdapterType.FILE;
    }

    @Override
    public List<CollectionRecord> collect(final CollectionRunContext context) {
        Objects.requireNonNull(context, "context");
        final Path watchDir = config.watchDirectory();
        if (!Files.isDirectory(watchDir)) {
            // 不存在目录视为空目录 — 调度器会按空批次处理，不应抛错破坏采集循环
            return List.of();
        }
        final List<Path> candidates = scanCandidates(watchDir, context.batchSize());
        final List<CollectionRecord> result = new ArrayList<>();
        final Instant collectedAt = Instant.now();
        for (final Path file : candidates) {
            final Path lock = lockPathOf(file);
            // 原子创建锁 — race 丢失（FileAlreadyExistsException）静默跳过
            try {
                Files.createFile(lock);
            } catch (FileAlreadyExistsException raceLost) {
                // Plan §T3 #5 — 另一个进程/线程抢先持锁，本批跳过该文件
                continue;
            } catch (IOException ioe) {
                throw new FepBusinessException(
                        FepErrorCode.COLLECT_ADAPTER_FAILURE,
                        "failed to create lock file: "
                                + LogSanitizer.sanitize(safeFilename(lock)),
                        ioe);
            }
            // 已持锁 — 解析；失败则改名 .failed-<ts>，删锁，metrics.failed++（不阻塞）
            try {
                final List<CollectionRecord> rows = parseFile(file, collectedAt);
                result.addAll(rows);
            } catch (IOException | RuntimeException parseFailure) {
                handleParseFailure(file, lock, parseFailure);
            }
        }
        return List.copyOf(result);
    }

    @Override
    public void acknowledge(final CollectionRunContext context, final List<CollectionRecord> records) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(records, "records");
        if (records.isEmpty()) {
            return;
        }
        // 按源文件去重（同一文件多行只 archive 一次）
        final Map<String, Path> filesToArchive = new LinkedHashMap<>();
        for (final CollectionRecord record : records) {
            final String sourceRef = record.getSourceRef();
            final int idx = sourceRef.indexOf(SOURCE_REF_ROW_SEP);
            final String filename = idx < 0 ? sourceRef : sourceRef.substring(0, idx);
            filesToArchive.putIfAbsent(filename, config.watchDirectory().resolve(filename));
        }
        for (final Map.Entry<String, Path> entry : filesToArchive.entrySet()) {
            final Path source = entry.getValue();
            archiveOne(source, entry.getKey());
        }
    }

    /**
     * 扫描 watchDirectory 取最多 {@code batchSize} 个匹配 fileFormat 后缀且未被锁的文件。
     *
     * @param watchDir  待扫描目录
     * @param batchSize 上限（{@code List.subList} 截取）
     * @return 候选文件列表（按文件名字典序，便于测试稳定性）
     */
    private List<Path> scanCandidates(final Path watchDir, final int batchSize) {
        final String suffix = config.fileFormat().getExtension();
        try (Stream<Path> stream = Files.list(watchDir)) {
            final List<Path> all = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> safeFilename(p).endsWith(suffix))
                    .filter(p -> !Files.exists(lockPathOf(p)))
                    .sorted((a, b) -> safeFilename(a).compareTo(safeFilename(b)))
                    .toList();
            return all.size() > batchSize ? all.subList(0, batchSize) : all;
        } catch (IOException ioe) {
            throw new FepBusinessException(
                    FepErrorCode.COLLECT_ADAPTER_FAILURE,
                    "failed to list watchDirectory: "
                            + LogSanitizer.sanitize(watchDir.toString()),
                    ioe);
        }
    }

    /**
     * 解析单个文件为 {@link CollectionRecord} 列表（按 {@link FileFormat} 分发）。
     */
    private List<CollectionRecord> parseFile(final Path file, final Instant collectedAt) throws IOException {
        final String filename = safeFilename(file);
        final List<Map<String, Object>> rows;
        switch (config.fileFormat()) {
            case CSV -> rows = CsvFileParser.parse(file, config.charset(), config.csvSeparator());
            case XLSX -> rows = parseXlsx(file);
            default -> throw new IllegalStateException("unsupported format: " + config.fileFormat());
        }
        final List<CollectionRecord> records = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            // header 行 = row 1，数据起始 = row 2
            final int rowNumber = i + 2;
            final String sourceRef = filename + SOURCE_REF_ROW_SEP + rowNumber;
            final Map<String, Object> rawData = rows.get(i);
            records.add(CollectionRecord.builder()
                    .adapterId(getId())
                    .sourceRef(sourceRef)
                    .payloadDataType(config.payloadDataType())
                    .rawData(rawData)
                    .collectedAt(collectedAt)
                    .idempotencyKey(IdempotencyKeyGenerator.generate(getId(), sourceRef))
                    .build());
        }
        return records;
    }

    /**
     * 解析 XLSX 文件 — 仅取首个 sheet，首行为表头。
     *
     * <p>使用 {@link DataFormatter} 获取与 Excel UI 一致的字符串值（避免数值精度漂移）。
     * 空单元格不写入 rawData（与 JDBC 适配器 null 列处理一致 —
     * {@link Map#copyOf} 不接受 null value）。
     */
    private List<Map<String, Object>> parseXlsx(final Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file);
             Workbook workbook = new XSSFWorkbook(in)) {
            final Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return List.of();
            }
            final Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                return List.of();
            }
            final DataFormatter formatter = new DataFormatter();
            final Map<Integer, String> headers = readHeaders(headerRow, formatter);
            final List<Map<String, Object>> rows = new ArrayList<>();
            // 数据行：从 firstRowNum+1 到 lastRowNum
            for (int r = sheet.getFirstRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
                final Row row = sheet.getRow(r);
                if (row == null) {
                    continue;
                }
                final Map<String, Object> rowMap = new LinkedHashMap<>();
                for (final Map.Entry<Integer, String> hdr : headers.entrySet()) {
                    final Cell cell = row.getCell(hdr.getKey());
                    if (cell == null) {
                        continue;
                    }
                    final String value = formatter.formatCellValue(cell);
                    if (value != null && !value.isEmpty()) {
                        rowMap.put(hdr.getValue(), value);
                    }
                }
                rows.add(rowMap);
            }
            return rows;
        }
    }

    private Map<Integer, String> readHeaders(final Row headerRow, final DataFormatter formatter) {
        final Map<Integer, String> headers = new HashMap<>();
        final int last = Math.min(headerRow.getLastCellNum(), XLSX_MAX_COLUMNS);
        for (int c = headerRow.getFirstCellNum(); c < last; c++) {
            final Cell cell = headerRow.getCell(c);
            if (cell == null) {
                continue;
            }
            final String name = formatter.formatCellValue(cell);
            if (name != null && !name.isEmpty()) {
                headers.put(c, name);
            }
        }
        return headers;
    }

    /**
     * 处理解析失败 — 重命名 {@code <orig>.failed-<timestamp>}，删锁，metrics.failed++。
     *
     * <p>本方法本身不抛异常（已记日志），保证调用方循环对其他文件继续。
     */
    private void handleParseFailure(final Path file, final Path lock, final Throwable cause) {
        final String safeName = LogSanitizer.sanitize(safeFilename(file));
        final String timestamp = FAILED_TIMESTAMP_FORMAT.format(Instant.now());
        final Path failedTarget = file.resolveSibling(safeFilename(file) + FAILED_INFIX + timestamp);
        try {
            Files.move(file, failedTarget, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException renameError) {
            // 重命名失败 — 仍尝试删锁，记 warn 让运维介入（异常 message 同样需要清理 CRLF）
            log.warn("failed to rename parse-failed file [{}]: {}",
                    safeName, LogSanitizer.sanitize(renameError.getMessage()));
        }
        try {
            Files.deleteIfExists(lock);
        } catch (IOException lockError) {
            log.warn("failed to delete .processing lock for [{}]: {}",
                    safeName, LogSanitizer.sanitize(lockError.getMessage()));
        }
        metrics.incFailed(1L);
        log.warn("file parse failed [{}], renamed to .failed-{}; cause={}",
                safeName, timestamp, cause.getClass().getSimpleName());
    }

    /**
     * 归档单个文件：移动到 archiveDirectory（带 timestamp 前缀），并删除 .processing 锁。
     *
     * <p>归档失败包装为 {@link FepBusinessException}（{@link FepErrorCode#COLLECT_ADAPTER_FAILURE}），
     * 防止上游静默推进水位（Plan §T3 #3 ack 失败必抛）。
     */
    private void archiveOne(final Path source, final String filename) {
        final String safeName = LogSanitizer.sanitize(filename);
        final String timestamp = ARCHIVE_TIMESTAMP_FORMAT.format(Instant.now());
        final Path archiveTarget = config.archiveDirectory().resolve(timestamp + "_" + filename);
        try {
            Files.createDirectories(config.archiveDirectory());
        } catch (IOException ioe) {
            throw new FepBusinessException(
                    FepErrorCode.COLLECT_ADAPTER_FAILURE,
                    "failed to create archive directory for " + safeName,
                    ioe);
        }
        try {
            Files.move(source, archiveTarget, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ioe) {
            throw new FepBusinessException(
                    FepErrorCode.COLLECT_ADAPTER_FAILURE,
                    "failed to archive file " + safeName,
                    ioe);
        }
        // 锁删除失败仅 warn — 文件已成功归档，残留锁不影响下次扫描
        // （锁对应文件已不在 watchDir，scanCandidates 不会再选中它）
        try {
            Files.deleteIfExists(lockPathOf(source));
        } catch (IOException ioe) {
            log.warn("failed to delete .processing lock after archive [{}]: {}",
                    safeName, LogSanitizer.sanitize(ioe.getMessage()));
        }
    }

    /**
     * 给定数据文件路径，返回其 {@code .processing} 锁文件路径。
     *
     * <p>{@link Path#getFileName()} 在根路径下可能返回 null（如 {@code /}），
     * 但 {@link FileAdapterConfig} 校验 watchDirectory 是有效目录，根路径在生产
     * 配置不可达；此处显式 {@link Objects#requireNonNull} 让 SpotBugs 静态分析满意。
     */
    private static Path lockPathOf(final Path file) {
        return file.resolveSibling(safeFilename(file) + LOCK_SUFFIX);
    }

    /**
     * 安全获取文件名 string — 防御 {@link Path#getFileName()} 返回 null（仅在根路径出现）。
     *
     * @param p 路径（非 null）
     * @return 文件名 string；若 getFileName() 为 null 则抛 NPE（指示编程错误，不应发生在合法采集流程）
     */
    private static String safeFilename(final Path p) {
        final Path fname = p.getFileName();
        if (fname == null) {
            throw new IllegalArgumentException("path has no file name component: " + p);
        }
        return fname.toString();
    }
}
