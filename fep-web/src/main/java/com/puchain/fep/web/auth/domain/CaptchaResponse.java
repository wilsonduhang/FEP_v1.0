package com.puchain.fep.web.auth.domain;

/**
 * 图形验证码响应 DTO。
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class CaptchaResponse {

    /** 验证码 UUID（登录时随 code 一起传回） */
    private final String captchaId;

    /** base64 编码的 PNG 图片（含 "data:image/png;base64," 前缀） */
    private final String imageBase64;

    /** 有效期秒数 */
    private final long ttlSeconds;

    /**
     * 构造验证码响应。
     *
     * @param captchaId   验证码 ID
     * @param imageBase64 base64 编码的 PNG 图片
     * @param ttlSeconds  有效期秒数
     */
    public CaptchaResponse(final String captchaId, final String imageBase64, final long ttlSeconds) {
        this.captchaId = captchaId;
        this.imageBase64 = imageBase64;
        this.ttlSeconds = ttlSeconds;
    }

    /**
     * 获取验证码 ID。
     *
     * @return 验证码 UUID
     */
    public String getCaptchaId() {
        return captchaId;
    }

    /**
     * 获取 base64 编码的 PNG 图片。
     *
     * @return base64 图片字符串
     */
    public String getImageBase64() {
        return imageBase64;
    }

    /**
     * 获取有效期秒数。
     *
     * @return TTL 秒数
     */
    public long getTtlSeconds() {
        return ttlSeconds;
    }
}
