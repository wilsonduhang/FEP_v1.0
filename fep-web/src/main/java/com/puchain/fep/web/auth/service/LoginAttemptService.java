package com.puchain.fep.web.auth.service;

import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.web.auth.RedisKeyConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 登录失败次数跟踪（Redis）。
 *
 * <p>连续失败 5 次后触发账号锁定 30 分钟（PRD §5.1.4）。</p>
 *
 * <p>Redis key: {@code fep:login:fail:{account}}，TTL 30 分钟。
 * 每次失败递增，登录成功时清除。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class LoginAttemptService {

    private static final Logger log = LoggerFactory.getLogger(LoginAttemptService.class);
    private static final Duration COUNT_TTL = Duration.ofMinutes(30);
    private static final int MAX_ATTEMPTS = 5;

    private final StringRedisTemplate redisTemplate;

    /**
     * 构造 LoginAttemptService。
     *
     * @param redisTemplate Redis 模板
     */
    public LoginAttemptService(final StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 记录一次登录失败，返回当前累计失败次数。
     *
     * @param account 登录账号
     * @return 累计失败次数
     */
    public int recordFailure(final String account) {
        String key = RedisKeyConstants.LOGIN_FAIL_PREFIX + account;
        Long count = redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, COUNT_TTL);
        int result = count != null ? count.intValue() : 0;
        log.info("Login failure recorded for account={}, attempts={}",
                LogSanitizer.sanitize(account), result);
        return result;
    }

    /**
     * 清除登录失败计数（登录成功时调用）。
     *
     * @param account 登录账号
     */
    public void clearFailures(final String account) {
        redisTemplate.delete(RedisKeyConstants.LOGIN_FAIL_PREFIX + account);
    }

    /**
     * 检查账号是否因失败次数过多而被锁定。
     *
     * @param account 登录账号
     * @return true 已锁定
     */
    public boolean isLocked(final String account) {
        String value = redisTemplate.opsForValue().get(RedisKeyConstants.LOGIN_FAIL_PREFIX + account);
        return value != null && Integer.parseInt(value) >= MAX_ATTEMPTS;
    }

    /**
     * 获取最大允许失败次数。
     *
     * @return 最大失败次数
     */
    public int getMaxAttempts() {
        return MAX_ATTEMPTS;
    }
}
