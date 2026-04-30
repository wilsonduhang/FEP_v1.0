package com.puchain.fep.collector.support;

import java.util.Objects;

/**
 * 分布式锁令牌（不可变 record）。
 *
 * <p>由 {@link DistributedLock#tryLock(String, long)} 在加锁成功时返回，
 * 调用方持有此 token 用于 {@link DistributedLock#release(LockToken)}。token 字段
 * 是"持有人凭证"，用于防止误释放他人持有的锁（典型场景：原持有人 TTL 过期后被新
 * 持有人接管，原持有人晚到的 release 不应释放新持有人的锁）。
 *
 * <p><b>字段语义：</b>
 * <ul>
 *   <li>{@code key}             — 锁键（业务标识，例如 {@code "ADP_JDBC_001"}）</li>
 *   <li>{@code token}           — 持有人凭证，UUID 派生字符串（每次 tryLock 新生成）</li>
 *   <li>{@code acquiredAtMillis} — 加锁时刻（epoch millis，源自 {@link java.time.Clock}）</li>
 *   <li>{@code ttlMillis}       — TTL（毫秒），过期后允许其他持有人接管</li>
 * </ul>
 *
 * <p>compact 构造函数对 {@code key} / {@code token} 做 null 校验，{@code ttlMillis}
 * 必须 &gt; 0（否则 TTL 语义无意义）。{@code acquiredAtMillis} 不校验（允许测试用 0L 等
 * 任意时刻）。
 *
 * @param key              锁键
 * @param token            持有人凭证（UUID 派生）
 * @param acquiredAtMillis 加锁时刻（epoch millis）
 * @param ttlMillis        TTL（毫秒，必须 &gt; 0）
 * @author FEP Team
 * @since 1.0.0
 */
public record LockToken(
        String key,
        String token,
        long acquiredAtMillis,
        long ttlMillis
) {

    /**
     * compact 构造函数 —— null + 范围校验。
     */
    public LockToken {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(token, "token");
        if (ttlMillis <= 0L) {
            throw new IllegalArgumentException("ttlMillis must be > 0, got " + ttlMillis);
        }
    }

    /**
     * 判断当前 token 在给定时刻是否已过期。
     *
     * <p>边界语义：{@code nowMillis > acquiredAtMillis + ttlMillis} 才过期；
     * 等于 TTL 边界（{@code now == acquiredAt + ttl}）尚未过期，
     * 严格大于才允许新持有人接管。
     *
     * @param nowMillis 当前时刻（epoch millis）
     * @return 当前时刻已超过 TTL 边界返回 true，否则 false
     */
    public boolean isExpired(final long nowMillis) {
        return nowMillis > acquiredAtMillis + ttlMillis;
    }
}
