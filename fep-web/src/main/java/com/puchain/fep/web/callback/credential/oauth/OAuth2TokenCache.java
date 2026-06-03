package com.puchain.fep.web.callback.credential.oauth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * 行内 OAuth2 access_token 内存缓存（per interfaceId）。
 *
 * <p>TTL 由 token 的 {@code expires_in - 30s} safety margin 决定；401/403 时调用方应主动
 * {@link #invalidate}。进程重启 cache 清空，不持久化（Step 5.5 安全审计 C.3 — JVM heap dump
 * 明文 token 风险已 acknowledged，不引入磁盘持久化）。</p>
 *
 * <p>N1 修订（v0.2）: 用 {@link Expiry} per-entry {@code expireAfterCreate} 表达可变 TTL；
 * v0.1 用 {@code expireVariably().ifPresent + cache.put} 双写有 race，本版改 entry-wrapped
 * {@link CachedToken} 单写路径。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class OAuth2TokenCache {

    /** 缓存条目上限，防止恶意 interfaceId 爆量占用 heap。 */
    private static final long MAX_ENTRIES = 1_000L;

    private final Cache<String, CachedToken> cache;

    /**
     * 构造缓存：per-entry 动态 TTL（由 {@link CachedToken#ttlNanos()} 决定），上限 {@value #MAX_ENTRIES} 条。
     */
    public OAuth2TokenCache() {
        this.cache = Caffeine.newBuilder()
                .maximumSize(MAX_ENTRIES)
                .expireAfter(new Expiry<String, CachedToken>() {
                    @Override
                    public long expireAfterCreate(final String key, final CachedToken value,
                            final long currentTime) {
                        return value.ttlNanos();
                    }

                    @Override
                    public long expireAfterUpdate(final String key, final CachedToken value,
                            final long currentTime, final long currentDuration) {
                        return value.ttlNanos();
                    }

                    @Override
                    public long expireAfterRead(final String key, final CachedToken value,
                            final long currentTime, final long currentDuration) {
                        return currentDuration;
                    }
                })
                .build();
    }

    /**
     * 写入 token 及其 TTL。
     *
     * @param interfaceId 接口标识（缓存 key），非空
     * @param token       access_token 明文，非空
     * @param ttl         剩余有效期，非空
     */
    public void put(final String interfaceId, final String token, final Duration ttl) {
        Objects.requireNonNull(interfaceId, "interfaceId");
        Objects.requireNonNull(token, "token");
        Objects.requireNonNull(ttl, "ttl");
        cache.put(interfaceId, new CachedToken(token, ttl.toNanos()));
    }

    /**
     * 读取未过期的 token。
     *
     * @param interfaceId 接口标识
     * @return token（若存在且未过期），否则 {@link Optional#empty()}
     */
    public Optional<String> get(final String interfaceId) {
        return Optional.ofNullable(cache.getIfPresent(interfaceId)).map(CachedToken::token);
    }

    /**
     * 主动失效指定接口的缓存（如 401/403 后）。
     *
     * @param interfaceId 接口标识
     */
    public void invalidate(final String interfaceId) {
        cache.invalidate(interfaceId);
    }

    /**
     * 缓存条目：token 明文 + 其纳秒 TTL。
     *
     * @param token    access_token 明文（敏感）
     * @param ttlNanos 有效期纳秒数
     */
    private record CachedToken(String token, long ttlNanos) {

        @Override
        public String toString() {
            return "CachedToken{token=***, ttlNanos=" + ttlNanos + '}';
        }
    }
}
