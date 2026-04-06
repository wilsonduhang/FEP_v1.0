package com.puchain.fep.web.sysmgmt.download.domain;

/**
 * 下载任务状态枚举。
 *
 * <p>参见 PRD v1.3 §5.10.5 下载任务、§6.4 下载任务表。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum TaskStatus {

    /** 等待中（任务已提交，尚未开始生成）。 */
    WAITING,

    /** 生成中（文件正在被异步生成）。 */
    GENERATING,

    /** 已完成（文件已生成，可供下载）。 */
    COMPLETED,

    /** 失败（文件生成过程中发生错误）。 */
    FAILED,

    /**
     * 已过期（文件已清理，用户仍可见此状态）。
     *
     * <p>保留期（{@code FILE_RETENTION_DAYS}）到期后由定时任务将状态标记为此值，
     * filePath 同时置为 null；原始记录不删除，前端可展示"已过期"提示。</p>
     */
    EXPIRED
}
