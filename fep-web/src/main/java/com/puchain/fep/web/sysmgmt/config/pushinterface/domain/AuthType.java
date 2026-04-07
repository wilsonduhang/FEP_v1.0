package com.puchain.fep.web.sysmgmt.config.pushinterface.domain;

/**
 * 推送接口鉴权类型枚举。
 *
 * <p>参见 PRD v1.3 §5.10.7.2c 推送接口管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum AuthType {

    /** Token 鉴权（Bearer Token）。 */
    TOKEN,

    /** OAuth2 授权码鉴权。 */
    OAUTH2,

    /** 无鉴权。 */
    NONE
}
