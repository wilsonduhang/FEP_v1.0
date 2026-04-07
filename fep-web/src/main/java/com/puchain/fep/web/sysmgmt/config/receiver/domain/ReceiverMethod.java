package com.puchain.fep.web.sysmgmt.config.receiver.domain;

/**
 * 数据接收方接收方式枚举。
 *
 * <p>参见 PRD v1.3 §5.10.7.2b 数据接收方管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum ReceiverMethod {

    /** 接口方式。 */
    INTERFACE,
    /** 文件方式。 */
    FILE,
    /** FTP 方式。 */
    FTP
}
