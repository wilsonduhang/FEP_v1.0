package com.puchain.fep.web.sysmgmt.user.dto;

import com.puchain.fep.common.validation.PasswordComplexity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 重置密码请求 DTO。
 *
 * <p>参见 PRD v1.3 §5.10.1 用户管理。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class ResetPasswordRequest {

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 20, message = "密码长度 8-20 字符")
    @PasswordComplexity
    private String newPassword;

    /**
     * 获取新密码。
     *
     * @return 新密码
     */
    public String getNewPassword() {
        return newPassword;
    }

    /**
     * 设置新密码。
     *
     * @param newPassword 新密码
     */
    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
