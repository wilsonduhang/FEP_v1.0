package com.puchain.fep.web.submission.outputinterface.domain;

/**
 * 输出接口鉴权类型枚举。
 *
 * <p>参见 PRD v1.3 §5.5.2 输出接口管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum InterfaceAuthType {

    /** Token 鉴权（Bearer Token）。 */
    TOKEN,

    /** OAuth2 授权码鉴权。 */
    OAUTH2,

    /** 无鉴权。 */
    NONE
}
