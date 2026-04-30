package com.puchain.fep.collector.adapter.file;

/**
 * 文件采集适配器支持的文件格式枚举（PRD v1.3 §2.2.2 文件采集）。
 *
 * <p>覆盖：
 * <ul>
 *   <li>{@link #CSV}  — 逗号/自定义分隔的纯文本（UTF-8 默认）</li>
 *   <li>{@link #XLSX} — Office Open XML 电子表格（首行表头）</li>
 * </ul>
 *
 * <p><b>本 Plan 仅支持本地 POSIX FS</b>：SFTP/NFS 锁文件原子性不保证，
 * 相关支持延后到 Plan §Deferred D2。
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum FileFormat {

    /** 逗号分隔值文件（默认 UTF-8 编码，分隔符可配）。 */
    CSV(".csv"),

    /** Office Open XML 电子表格（.xlsx，首行视为表头）。 */
    XLSX(".xlsx");

    private final String extension;

    FileFormat(final String extension) {
        this.extension = extension;
    }

    /**
     * 文件后缀（含 {@code .} 前缀）。
     *
     * @return 例如 {@code ".csv"} / {@code ".xlsx"}（非 null）
     */
    public String getExtension() {
        return extension;
    }
}
