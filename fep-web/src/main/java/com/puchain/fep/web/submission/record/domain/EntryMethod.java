package com.puchain.fep.web.submission.record.domain;

/**
 * 报送记录录入方式枚举。
 *
 * <p>参见 PRD v1.3 §5.6.1 手动报文上传 / §5.5.5 报文数据列表。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum EntryMethod {

    /** API 接口调用录入。 */
    API_CALL,

    /** 手动录入。 */
    MANUAL_ENTRY
}
