package com.puchain.fep.collector.support;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 单进程内 {@link DistributedLock} 实现（基于 {@link ConcurrentHashMap}）。
 *
 * <p><b>仅支持单进程多线程互斥。</b>跨进程部署（多个 FEP 实例同时拉同一数据源）请改用
 * Redis / ZooKeeper 后端实现 —— 计划在 P5+ 引入。
 *
 * <p><b>实现要点：</b>
 * <ul>
 *   <li>{@link ConcurrentMap#compute} 单调用原子完成"读 + 过期检测 + 写"，
 *       避免 check-then-act race</li>
 *   <li>{@link Clock} 注入便于测试（fixed clock + 时钟推进无需 Thread.sleep）</li>
 *   <li>release 用 {@link ConcurrentMap#compute} 的"返回 null 即删除"语义实现
 *       原子 remove-if-token-matches</li>
 * </ul>
 *
 * <p><b>未声明为 Spring Bean</b>：保持工具类身份，由调用方显式 new 出来注入。
 * 生产环境会被 Redis 实现替换，避免误装配 in-process 实现到生产环境。
 *
 * @author FEP Team
 * @since 1.0.0
 */
public final class InProcessDistributedLock implements DistributedLock {

    private final ConcurrentMap<String, LockToken> locks = new ConcurrentHashMap<>();
    private final Clock clock;

    /**
     * 默认构造（系统 UTC 时钟）。
     */
    public InProcessDistributedLock() {
        this(Clock.systemUTC());
    }

    /**
     * 注入自定义 {@link Clock}（测试用 —— fixed / mutable clock）。
     *
     * @param clock 时钟（非 null）
     */
    public InProcessDistributedLock(final Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public Optional<LockToken> tryLock(final String key, final long ttlMillis) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("key must not be null or empty");
        }
        if (ttlMillis <= 0L) {
            throw new IllegalArgumentException("ttlMillis must be > 0, got " + ttlMillis);
        }

        long now = clock.millis();
        // 单元素数组承载本次新建 token —— compute 闭包外部不能赋值非 final 局部变量，
        // 用数组引用做"输出参数"。compute 原子完成"读 + 过期检测 + 写"。
        LockToken[] freshToken = new LockToken[1];
        locks.compute(key, (k, existing) -> {
            if (existing == null || existing.isExpired(now)) {
                LockToken minted = new LockToken(k, UUID.randomUUID().toString(), now, ttlMillis);
                freshToken[0] = minted;
                return minted;
            }
            // 既存 token 未过期 —— 保持原值，不写 freshToken
            return existing;
        });

        return Optional.ofNullable(freshToken[0]);
    }

    @Override
    public void release(final LockToken token) {
        Objects.requireNonNull(token, "token");
        // compute 返回 null 即删除条目；token 不匹配返回原值即不变
        locks.compute(token.key(), (k, existing) -> {
            if (existing != null && existing.token().equals(token.token())) {
                return null;
            }
            return existing;
        });
    }
}
