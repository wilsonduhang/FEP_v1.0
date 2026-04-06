package com.puchain.fep.web.auth;

/**
 * Redis key 前缀常量 — 认证授权模块使用的所有 Redis key 集中定义。
 *
 * @author FEP Team
 * @since 1.0.0
 */
public final class RedisKeyConstants {

    private RedisKeyConstants() { }

    /** 登录失败计数: fep:login:fail:{account} */
    public static final String LOGIN_FAIL_PREFIX = "fep:login:fail:";

    /** 图形验证码: fep:captcha:{uuid} */
    public static final String CAPTCHA_PREFIX = "fep:captcha:";

    /** JWT 黑名单: fep:jwt:blacklist:{jti} */
    public static final String JWT_BLACKLIST_PREFIX = "fep:jwt:blacklist:";

    /** SSO 会话: fep:session:{userId} */
    public static final String SSO_SESSION_PREFIX = "fep:session:";
}
