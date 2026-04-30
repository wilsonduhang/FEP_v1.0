package com.puchain.fep.collector.adapter.file;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * CSV 文件解析工具类（Apache Commons CSV）。
 *
 * <p>语义：首行视为表头，每行返回 {@code Map<String, Object>}（列名 → 单元格字符串值），
 * 保持列序（{@link LinkedHashMap}）便于日志/调试可读性。
 *
 * <p><b>UTF-8 严格模式：</b>使用 {@link CodingErrorAction#REPORT} 让无效字节序列触发
 * {@link java.nio.charset.MalformedInputException}（被 commons-csv 包装为
 * {@link java.io.UncheckedIOException}），由调用方捕获并执行 {@code .failed-<ts>} 重命名。
 *
 * <p><b>本 Plan 仅支持本地 POSIX FS</b>：SFTP/NFS 锁文件原子性不保证，相关支持
 * 延后到 Plan §Deferred D2。
 *
 * @author FEP Team
 * @since 1.0.0
 */
public final class CsvFileParser {

    private CsvFileParser() {
        // 工具类禁止实例化
    }

    /**
     * 解析 CSV 文件为列名→值 Map 列表。
     *
     * @param file      CSV 文件路径（非 null）
     * @param charset   字符集（非 null；建议 UTF-8）
     * @param separator 字段分隔符
     * @return 每行一个 Map（列名 → 单元格值，{@link LinkedHashMap} 保留列序）
     * @throws IOException 读取失败 / 解析失败 / 无效字符编码
     */
    public static List<Map<String, Object>> parse(
            final Path file, final Charset charset, final char separator) throws IOException {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(charset, "charset");

        final CSVFormat format = CSVFormat.DEFAULT.builder()
                .setDelimiter(separator)
                .setHeader()
                .setSkipHeaderRecord(true)
                .build();

        // CharsetDecoder 配 REPORT 模式 — 无效 UTF-8 字节序列直接抛 MalformedInputException
        // 而非静默替换为 ?，确保上游能捕获并执行 .failed-<ts> 重命名（Plan §T3 #6）
        try (InputStream in = Files.newInputStream(file);
             InputStreamReader reader = new InputStreamReader(in,
                     charset.newDecoder()
                             .onMalformedInput(CodingErrorAction.REPORT)
                             .onUnmappableCharacter(CodingErrorAction.REPORT));
             BufferedReader buffered = new BufferedReader(reader);
             CSVParser parser = format.parse(buffered)) {

            final List<String> headers = parser.getHeaderNames();
            final List<Map<String, Object>> rows = new ArrayList<>();
            for (final CSVRecord record : parser) {
                final Map<String, Object> row = new LinkedHashMap<>();
                for (final String header : headers) {
                    row.put(header, record.get(header));
                }
                rows.add(row);
            }
            return rows;
        }
    }
}
