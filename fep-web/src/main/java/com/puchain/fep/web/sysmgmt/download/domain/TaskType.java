package com.puchain.fep.web.sysmgmt.download.domain;

/**
 * 下载任务类型枚举。
 *
 * <p>参见 PRD v1.3 §5.10.5 下载任务。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum TaskType {

    /** 数据导出（查询结果集导出为文件）。 */
    DATA_EXPORT,

    /** 报表生成（统计报表文件生成）。 */
    REPORT_GEN,

    /** 日志下载（操作日志/报文日志导出）。 */
    LOG_DOWNLOAD
}
