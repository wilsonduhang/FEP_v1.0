package com.puchain.fep.collector.adapter.file;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;

/**
 * {@link FileCollectorAdapter} 不可变配置（Java record）。
 *
 * <p>覆盖 PRD v1.3 §2.2.2 数仓模式 + §3.6 UTF-8 编码约束：
 * <ul>
 *   <li>{@code adapterId}        — 与 {@code fep.collector.adapters[*].id} 一致</li>
 *   <li>{@code payloadDataType}  — 报文数据类型（如 {@code INVOICE_CONTRACT_3101}）</li>
 *   <li>{@code watchDirectory}   — 待采集文件目录（必须为绝对路径）</li>
 *   <li>{@code archiveDirectory} — 归档目录（acknowledge 后文件移入此处）</li>
 *   <li>{@code fileFormat}       — {@link FileFormat#CSV} / {@link FileFormat#XLSX}</li>
 *   <li>{@code charset}          — 字符集（默认 {@link StandardCharsets#UTF_8}，
 *                                   仅对 CSV 生效；XLSX 编码由 POI 内部决定）</li>
 *   <li>{@code csvSeparator}     — CSV 分隔符（默认 {@code ','}）</li>
 * </ul>
 *
 * <p>compact 构造函数对引用字段执行 {@link Objects#requireNonNull} 校验，
 * 并校验 {@code watchDirectory != archiveDirectory}（防止归档回写水池）。
 *
 * <p><b>本 Plan 仅支持本地 POSIX FS</b>：SFTP/NFS 锁文件原子性不保证，
 * 相关支持延后到 Plan §Deferred D2。
 *
 * @author FEP Team
 * @since 1.0.0
 *
 * @param adapterId        适配器 ID（非 null）
 * @param payloadDataType  报文数据类型（非 null）
 * @param watchDirectory   待采集目录（非 null）
 * @param archiveDirectory 归档目录（非 null，必须 != watchDirectory）
 * @param fileFormat       文件格式（非 null）
 * @param charset          字符集（非 null，CSV 模式下生效）
 * @param csvSeparator     CSV 分隔符
 */
public record FileAdapterConfig(
        String adapterId,
        String payloadDataType,
        Path watchDirectory,
        Path archiveDirectory,
        FileFormat fileFormat,
        Charset charset,
        char csvSeparator
) {

    /** 默认 CSV 分隔符 — 逗号。 */
    public static final char DEFAULT_CSV_SEPARATOR = ',';

    /**
     * compact 构造函数 — null 校验 + watchDirectory != archiveDirectory 校验。
     */
    public FileAdapterConfig {
        Objects.requireNonNull(adapterId, "adapterId");
        Objects.requireNonNull(payloadDataType, "payloadDataType");
        Objects.requireNonNull(watchDirectory, "watchDirectory");
        Objects.requireNonNull(archiveDirectory, "archiveDirectory");
        Objects.requireNonNull(fileFormat, "fileFormat");
        Objects.requireNonNull(charset, "charset");
        if (watchDirectory.equals(archiveDirectory)) {
            throw new IllegalArgumentException(
                    "watchDirectory must differ from archiveDirectory (avoid archive feedback loop)");
        }
    }

    /**
     * 工厂方法 — 使用默认 charset (UTF-8) 与默认 csvSeparator (',')。
     *
     * @param adapterId        适配器 ID（非 null）
     * @param payloadDataType  报文数据类型（非 null）
     * @param watchDirectory   待采集目录（非 null）
     * @param archiveDirectory 归档目录（非 null，必须 != watchDirectory）
     * @param fileFormat       文件格式（非 null）
     * @return 新 {@link FileAdapterConfig}（charset = UTF-8，csvSeparator = ','）
     */
    public static FileAdapterConfig withDefaults(
            final String adapterId,
            final String payloadDataType,
            final Path watchDirectory,
            final Path archiveDirectory,
            final FileFormat fileFormat) {
        return new FileAdapterConfig(
                adapterId, payloadDataType,
                watchDirectory, archiveDirectory,
                fileFormat,
                StandardCharsets.UTF_8,
                DEFAULT_CSV_SEPARATOR);
    }
}
