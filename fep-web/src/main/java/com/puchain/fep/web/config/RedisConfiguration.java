package com.puchain.fep.web.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

/**
 * Redis 配置。
 *
 * <p>仅在 {@code fep.redis.enabled=true} 时激活。
 * 用于验证码缓存、登录失败计数、JWT 黑名单、SSO 会话管理等。</p>
 *
 * <p>开发/测试环境不设置该属性时，Redis 自动配置不会生效，
 * 确保无 Redis 实例也能正常运行测试。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Configuration
@ConditionalOnProperty(name = "fep.redis.enabled", havingValue = "true", matchIfMissing = false)
@EnableRedisRepositories(basePackages = "com.puchain.fep.web")
public class RedisConfiguration {
}
