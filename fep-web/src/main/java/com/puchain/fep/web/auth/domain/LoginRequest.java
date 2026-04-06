package com.puchain.fep.web.auth.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 登录请求 DTO。
 *
 * <p>参见 PRD v1.3 §5.1.3 / §5.1.4。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class LoginRequest {

    /** 登录账号：字母开头，6-20位字母数字下划线。 */
    @NotBlank(message = "账号不能为空")
    @Size(min = 6, max = 20)
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]*$", message = "账号必须字母开头，仅字母数字下划线")
    private String account;

    /** 密码：8-20位（登录时不做复杂度校验，兼容旧密码）。 */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 20)
    private String password;

    /** 验证码 UUID。 */
    @NotBlank(message = "验证码 ID 不能为空")
    private String captchaId;

    /** 用户输入的验证码。 */
    @NotBlank(message = "验证码不能为空")
    @Size(min = 4, max = 4)
    private String captchaCode;

    /**
     * 获取登录账号。
     *
     * @return 登录账号
     */
    public String getAccount() {
        return account;
    }

    /**
     * 设置登录账号。
     *
     * @param account 登录账号
     */
    public void setAccount(final String account) {
        this.account = account;
    }

    /**
     * 获取密码。
     *
     * @return 密码
     */
    public String getPassword() {
        return password;
    }

    /**
     * 设置密码。
     *
     * @param password 密码
     */
    public void setPassword(final String password) {
        this.password = password;
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
     * 设置验证码 ID。
     *
     * @param captchaId 验证码 UUID
     */
    public void setCaptchaId(final String captchaId) {
        this.captchaId = captchaId;
    }

    /**
     * 获取验证码。
     *
     * @return 用户输入的验证码
     */
    public String getCaptchaCode() {
        return captchaCode;
    }

    /**
     * 设置验证码。
     *
     * @param captchaCode 用户输入的验证码
     */
    public void setCaptchaCode(final String captchaCode) {
        this.captchaCode = captchaCode;
    }
}
