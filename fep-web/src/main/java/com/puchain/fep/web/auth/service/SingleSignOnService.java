package com.puchain.fep.web.auth.service;

import com.puchain.fep.web.auth.RedisKeyConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 单点登录会话管理 — 实现"后登录踢出先登录" (PRD §5.1.5)。
 *
 * <p>每次登录成功时，将用户的当前 JWT jti 写入 Redis，覆盖旧值。
 * {@link com.puchain.fep.web.auth.jwt.JwtAuthFilter} 在每次请求时比对
 * token 的 jti 与 Redis 中当前 jti，不一致则视为已被后登录踢出。</p>
 *
 * <p>TTL 与 Access Token 有效期一致（默认 2 小时）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class SingleSignOnService {

    private final StringRedisTemplate redisTemplate;

    /**
     * 构造 SingleSignOnService。
     *
     * @param redisTemplate Redis 模板
     */
    public SingleSignOnService(final StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 记录用户当前会话的 jti（登录/刷新 token 时调用）。
     *
     * @param userId     用户 ID
     * @param jti        JWT ID
     * @param ttlSeconds TTL 秒数（与 access token 有效期一致）
     */
    public void registerSession(final String userId, final String jti, final long ttlSeconds) {
        redisTemplate.opsForValue().set(
                RedisKeyConstants.SSO_SESSION_PREFIX + userId,
                jti,
                Duration.ofSeconds(ttlSeconds));
    }

    /**
     * 清除用户会话（登出时调用）。
     *
     * @param userId 用户 ID
     */
    public void clearSession(final String userId) {
        redisTemplate.delete(RedisKeyConstants.SSO_SESSION_PREFIX + userId);
    }
}
