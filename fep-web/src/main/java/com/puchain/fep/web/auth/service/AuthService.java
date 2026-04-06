package com.puchain.fep.web.auth.service;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepAuthException;
import com.puchain.fep.common.security.PasswordHasher;
import com.puchain.fep.web.auth.domain.LoginRequest;
import com.puchain.fep.web.auth.domain.LoginResponse;
import com.puchain.fep.web.auth.jwt.JwtProperties;
import com.puchain.fep.web.auth.jwt.JwtTokenProvider;
import com.puchain.fep.web.sysmgmt.rel.repository.SysUserRoleRepository;
import com.puchain.fep.web.sysmgmt.role.domain.SysRole;
import com.puchain.fep.web.sysmgmt.role.repository.SysRoleRepository;
import com.puchain.fep.web.sysmgmt.user.domain.SysUser;
import com.puchain.fep.web.sysmgmt.user.domain.UserStatus;
import com.puchain.fep.web.sysmgmt.user.repository.SysUserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 认证服务（登录/登出/刷新）。
 *
 * <p>登录流程按 PRD §5.1.4 实现：验证码 -> 账号密码 -> 失败计数 -> 锁定 -> 签发 token -> SSO。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final String BLACKLIST_PREFIX = "fep:jwt:blacklist:";
    private static final Duration LOCK_DURATION = Duration.ofMinutes(30);

    private final SysUserRepository userRepository;
    private final SysUserRoleRepository userRoleRepository;
    private final SysRoleRepository roleRepository;
    private final PasswordHasher passwordHasher;
    private final CaptchaService captchaService;
    private final LoginAttemptService loginAttemptService;
    private final SingleSignOnService singleSignOnService;
    private final JwtTokenProvider tokenProvider;
    private final StringRedisTemplate redisTemplate;
    private final long accessTokenTtlSeconds;

    /**
     * 构造 AuthService。
     *
     * @param userRepository      用户 Repository
     * @param userRoleRepository  用户-角色关联 Repository
     * @param roleRepository      角色 Repository
     * @param passwordHasher      密码散列服务
     * @param captchaService      验证码服务
     * @param loginAttemptService 登录尝试服务
     * @param singleSignOnService SSO 服务
     * @param tokenProvider       JWT 签发/解析
     * @param redisTemplate       Redis 模板
     * @param jwtProperties       JWT 配置属性
     */
    public AuthService(final SysUserRepository userRepository,
                       final SysUserRoleRepository userRoleRepository,
                       final SysRoleRepository roleRepository,
                       final PasswordHasher passwordHasher,
                       final CaptchaService captchaService,
                       final LoginAttemptService loginAttemptService,
                       final SingleSignOnService singleSignOnService,
                       final JwtTokenProvider tokenProvider,
                       final StringRedisTemplate redisTemplate,
                       final JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.roleRepository = roleRepository;
        this.passwordHasher = passwordHasher;
        this.captchaService = captchaService;
        this.loginAttemptService = loginAttemptService;
        this.singleSignOnService = singleSignOnService;
        this.tokenProvider = tokenProvider;
        this.redisTemplate = redisTemplate;
        this.accessTokenTtlSeconds = jwtProperties.getAccessTokenTtlSeconds();
    }

    /**
     * 用户登录。
     *
     * <p>流程：验证码校验 -> 用户查找 -> 状态检查 -> 密码校验 ->
     * 清除失败计数 -> 签发 token -> 注册 SSO 会话。</p>
     *
     * @param request 登录请求
     * @return 登录响应（含 token 和用户信息）
     * @throws FepAuthException 认证失败
     */
    @Transactional
    public LoginResponse login(final LoginRequest request) {
        // 1. 校验验证码
        if (!captchaService.verifyAndConsume(request.getCaptchaId(), request.getCaptchaCode())) {
            throw new FepAuthException(FepErrorCode.AUTH_0404);
        }

        // 2. 查找用户
        SysUser user = userRepository.findByUserAccount(request.getAccount())
                .orElseThrow(() -> {
                    loginAttemptService.recordFailure(request.getAccount());
                    return new FepAuthException(FepErrorCode.AUTH_0402);
                });

        // 3. 检查状态
        if (user.getUserStatus() == UserStatus.DISABLED) {
            throw new FepAuthException(FepErrorCode.AUTH_0406);
        }
        if (user.getUserStatus() == UserStatus.LOCKED
                && user.getLockedUntil() != null
                && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new FepAuthException(FepErrorCode.AUTH_0405,
                    "账号已锁定，请 " + user.getLockedUntil() + " 后重试");
        }

        // 4. 检查密码
        if (!passwordHasher.matches(request.getPassword(), user.getPasswordHash())) {
            int attempts = loginAttemptService.recordFailure(request.getAccount());
            if (attempts >= loginAttemptService.getMaxAttempts()) {
                user.setUserStatus(UserStatus.LOCKED);
                user.setLockedUntil(LocalDateTime.now().plus(LOCK_DURATION));
                userRepository.save(user);
                throw new FepAuthException(FepErrorCode.AUTH_0405,
                        "账号已锁定 30 分钟，请稍后重试");
            }
            throw new FepAuthException(FepErrorCode.AUTH_0402,
                    "账号或密码错误（剩余 " + (loginAttemptService.getMaxAttempts() - attempts) + " 次）");
        }

        // 5. 登录成功 -> 清除失败计数 + 更新用户状态
        loginAttemptService.clearFailures(request.getAccount());
        if (user.getUserStatus() == UserStatus.LOCKED) {
            user.setUserStatus(UserStatus.ACTIVE);
            user.setLockedUntil(null);
        }
        user.setLastLoginTime(LocalDateTime.now());
        user.setLoginFailCount(0);
        userRepository.save(user);

        // 6. 查询角色
        List<String> roleCodes = loadRoleCodes(user.getUserId());

        // 7. 签发 token
        String accessToken = tokenProvider.createAccessToken(
                user.getUserId(), user.getUserAccount(), roleCodes);
        String refreshToken = tokenProvider.createRefreshToken(
                user.getUserId(), user.getUserAccount());

        // 8. 记录当前会话 jti（覆盖旧 jti，实现单点踢出 PRD §5.1.5）
        String jti = tokenProvider.extractJti(accessToken);
        singleSignOnService.registerSession(user.getUserId(), jti, accessTokenTtlSeconds);

        boolean mustChangePassword = user.getLastPasswordChangeTime() == null;

        log.info("User login success: account={}", sanitize(request.getAccount()));

        return new LoginResponse(accessToken, refreshToken,
                user.getUserId(), user.getUserAccount(), user.getUserName(),
                roleCodes, mustChangePassword);
    }

    /**
     * 用户登出 — 将当前 JWT 加入黑名单 + 清除 SSO 会话。
     *
     * @param accessToken 当前 access token
     */
    public void logout(final String accessToken) {
        try {
            Claims claims = tokenProvider.parse(accessToken);
            String jti = claims.getId();
            String userId = claims.getSubject();
            long ttlMs = claims.getExpiration().getTime() - System.currentTimeMillis();
            if (ttlMs > 0) {
                redisTemplate.opsForValue().set(BLACKLIST_PREFIX + jti, "1",
                        Duration.ofMillis(ttlMs));
            }
            singleSignOnService.clearSession(userId);
            log.info("User logout: userId={}", sanitize(userId));
        } catch (JwtException ex) {
            log.debug("Logout with invalid token: {}", sanitize(ex.getMessage()));
        }
    }

    /**
     * 刷新 Access Token — 解析 refresh token，签发新 access token，更新 SSO 会话。
     *
     * @param refreshToken refresh token 字符串
     * @return 新的登录响应（含新 access token）
     * @throws FepAuthException refresh token 无效
     */
    public LoginResponse refresh(final String refreshToken) {
        Claims claims;
        try {
            claims = tokenProvider.parse(refreshToken);
        } catch (JwtException ex) {
            throw new FepAuthException(FepErrorCode.AUTH_0401, "Refresh token 无效或过期");
        }
        if (!"REFRESH".equals(claims.get("type"))) {
            throw new FepAuthException(FepErrorCode.AUTH_0401, "非 Refresh token");
        }
        String userId = claims.getSubject();
        SysUser user = userRepository.findById(userId)
                .orElseThrow(() -> new FepAuthException(FepErrorCode.AUTH_0401));
        List<String> roleCodes = loadRoleCodes(userId);
        String newAccess = tokenProvider.createAccessToken(
                user.getUserId(), user.getUserAccount(), roleCodes);
        String newJti = tokenProvider.extractJti(newAccess);
        singleSignOnService.registerSession(userId, newJti, accessTokenTtlSeconds);
        return new LoginResponse(newAccess, refreshToken,
                user.getUserId(), user.getUserAccount(), user.getUserName(),
                roleCodes, false);
    }

    /**
     * 根据用户 ID 加载角色编码列表。
     *
     * @param userId 用户 ID
     * @return 角色编码列表（可能为空列表）
     */
    private List<String> loadRoleCodes(final String userId) {
        List<String> roleIds = userRoleRepository.findRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return roleRepository.findByRoleIdIn(roleIds).stream()
                .map(SysRole::getRoleCode)
                .toList();
    }

    /**
     * 清除 CRLF 字符，防止日志注入。
     */
    private static String sanitize(final String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\r", "\\r").replace("\n", "\\n");
    }
}
