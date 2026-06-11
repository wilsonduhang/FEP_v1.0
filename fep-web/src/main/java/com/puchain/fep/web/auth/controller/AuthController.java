package com.puchain.fep.web.auth.controller;

import com.puchain.fep.common.domain.ApiResult;
import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepAuthException;
import com.puchain.fep.security.api.KeyService;
import com.puchain.fep.web.auth.domain.CaptchaResponse;
import com.puchain.fep.web.auth.domain.LoginRequest;
import com.puchain.fep.web.auth.domain.LoginResponse;
import com.puchain.fep.web.auth.domain.PublicKeyResponse;
import com.puchain.fep.web.auth.domain.RefreshRequest;
import com.puchain.fep.web.auth.domain.UserInfoResponse;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证相关 REST API。
 *
 * <p>提供验证码获取、用户登录、登出、Token 刷新、当前用户信息查询、SM2 公钥分发六个接口。
 * 除 {@code GET /me} 需要 JWT 鉴权外，其余接口为公开访问。</p>
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
    private final KeyService keyService;

    /**
     * 构造 AuthController。
     *
     * @param authService    认证服务
     * @param captchaService 验证码服务
     * @param keyService     SM2 密钥管理服务
     */
    public AuthController(final AuthService authService,
                          final CaptchaService captchaService,
                          final KeyService keyService) {
        this.authService = authService;
        this.captchaService = captchaService;
        this.keyService = keyService;
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

    /**
     * 获取当前登录用户信息（含权限码 + 菜单树）。
     *
     * @param userId JWT 中注入的用户 ID
     * @return 用户详情、角色列表、权限码列表、可访问菜单树
     */
    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息",
            description = "返回用户详情、角色列表、权限码列表和菜单树")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @ApiResponse(responseCode = "401", description = "未登录或 Token 无效")
    public ApiResult<UserInfoResponse> getMe(
            @AuthenticationPrincipal final String userId) {
        if (userId == null) {
            throw new FepAuthException(FepErrorCode.AUTH_0401);
        }
        return ApiResult.success(authService.getUserInfo(userId));
    }

    /**
     * 获取 SM2 公钥（公开端点，登录前调用）。
     *
     * @return SM2 公钥 Base64、密钥版本号、算法标识
     */
    @GetMapping("/public-key")
    @SecurityRequirements({})
    @Operation(summary = "获取 SM2 公钥",
            description = "返回当前有效的 SM2 公钥，用于前端加密登录密码")
    @ApiResponse(responseCode = "200", description = "返回公钥")
    public ApiResult<PublicKeyResponse> getPublicKey() {
        return ApiResult.success(new PublicKeyResponse(
                keyService.getSm2PublicKeyBase64(),
                keyService.getSm2LoginKeyId(),
                "SM2"));
    }
}
