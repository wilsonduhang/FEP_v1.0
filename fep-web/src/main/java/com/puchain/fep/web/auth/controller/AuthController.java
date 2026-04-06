package com.puchain.fep.web.auth.controller;

import com.puchain.fep.common.domain.ApiResult;
import com.puchain.fep.web.auth.domain.CaptchaResponse;
import com.puchain.fep.web.auth.domain.LoginRequest;
import com.puchain.fep.web.auth.domain.LoginResponse;
import com.puchain.fep.web.auth.domain.RefreshRequest;
import com.puchain.fep.web.auth.service.AuthService;
import com.puchain.fep.web.auth.service.CaptchaService;
import com.puchain.fep.web.sysmgmt.log.annotation.OperationLog;
import com.puchain.fep.web.sysmgmt.log.domain.OperationType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证相关 REST API。
 *
 * <p>提供验证码获取、用户登录、登出、Token 刷新四个接口。
 * 所有接口均为公开访问（不需要 JWT）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "01. 认证", description = "登录 / 登出 / Token 刷新 / 验证码")
@SecurityRequirements({})
public class AuthController {

    private final AuthService authService;
    private final CaptchaService captchaService;

    /**
     * 构造 AuthController。
     *
     * @param authService    认证服务
     * @param captchaService 验证码服务
     */
    public AuthController(final AuthService authService,
                          final CaptchaService captchaService) {
        this.authService = authService;
        this.captchaService = captchaService;
    }

    /**
     * 获取图形验证码。
     *
     * @return 验证码 ID + base64 图片
     */
    @GetMapping("/captcha")
    @Operation(summary = "获取图形验证码", description = "4 位字母数字，TTL 5 分钟，一次性使用")
    @ApiResponse(responseCode = "200", description = "验证码生成成功")
    public ApiResult<CaptchaResponse> captcha() {
        return ApiResult.success(captchaService.generate());
    }

    /**
     * 用户登录。
     *
     * @param request 登录请求（账号+密码+验证码）
     * @return access/refresh token + 用户信息
     */
    @PostMapping("/login")
    @OperationLog(module = "认证", type = OperationType.LOGIN, description = "用户登录")
    @Operation(summary = "用户登录", description = "账号+密码+图形验证码，返回 accessToken + refreshToken")
    @ApiResponse(responseCode = "200", description = "登录成功")
    @ApiResponse(responseCode = "401", description = "认证失败")
    public ApiResult<LoginResponse> login(@Valid @RequestBody final LoginRequest request) {
        return ApiResult.success(authService.login(request));
    }

    /**
     * 用户登出 — 将当前 access token 加入黑名单。
     *
     * @param request HTTP 请求（从 Authorization 头提取 token）
     * @return 空响应
     */
    @PostMapping("/logout")
    @OperationLog(module = "认证", type = OperationType.LOGOUT, description = "用户登出")
    @Operation(summary = "用户登出", description = "将当前 accessToken 加入黑名单")
    @ApiResponse(responseCode = "200", description = "登出成功")
    public ApiResult<Void> logout(final HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            authService.logout(header.substring("Bearer ".length()));
        }
        return ApiResult.success();
    }

    /**
     * 刷新 Access Token。
     *
     * @param request 刷新请求（含 refresh token）
     * @return 新的 access token + 用户信息
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新 Access Token")
    @ApiResponse(responseCode = "200", description = "刷新成功")
    @ApiResponse(responseCode = "401", description = "Refresh Token 无效")
    public ApiResult<LoginResponse> refresh(@Valid @RequestBody final RefreshRequest request) {
        return ApiResult.success(authService.refresh(request.getRefreshToken()));
    }
}
