package com.puchain.fep.collector.support;

import java.util.Optional;

/**
 * 分布式锁抽象。
 *
 * <p>用于采集层在多进程 / 多线程部署下保证同一 adapter / 同一 key 的串行执行
 * （例如同一 JDBC 表的 cursor 推进必须独占）。当前 {@link InProcessDistributedLock}
 * 仅覆盖单进程多线程；P5+ 可替换为 Redis / ZooKeeper 后端实现以支持跨进程。
 *
 * <p><b>语义：</b>
 * <ul>
 *   <li>{@link #tryLock} 非阻塞 —— 锁忙立即返回 {@link Optional#empty}</li>
 *   <li>TTL 过期视为自动释放 —— 后续 {@code tryLock} 可获取（无需显式 release）</li>
 *   <li>{@link #release} 是 token 校验的精准释放 —— 防误释放他人持有的锁</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public interface DistributedLock {

    /**
     * 尝试获取分布式锁（非阻塞）。
     *
     * <p>语义：
     * <ul>
     *   <li>{@code key} 当前未上锁 → 立即创建新 {@link LockToken} 并返回</li>
     *   <li>{@code key} 已上锁但 TTL 已过期 → 视为自动释放，创建新 token 并返回</li>
     *   <li>{@code key} 已上锁且未过期 → 返回 {@link Optional#empty}</li>
     * </ul>
     *
     * @param key       锁键（非 null / 非空）
     * @param ttlMillis TTL（毫秒，必须 &gt; 0）—— 过期后允许其他持有人接管
     * @return 加锁成功返回持有人 token，失败返回 {@link Optional#empty}
     * @throws IllegalArgumentException 当 {@code key} 为 null / 空，或 {@code ttlMillis} ≤ 0
     */
    Optional<LockToken> tryLock(String key, long ttlMillis);

    /**
     * 释放分布式锁（仅当 token 匹配时生效）。
     *
     * <p>语义：
     * <ul>
     *   <li>{@code token.key()} 当前持有人的 token 与传入 token 字段相等 → 删除条目，释放锁</li>
     *   <li>token 不匹配（持有人已切换 / 已过期被接管） → silently no-op</li>
     *   <li>{@code token.key()} 当前无持有人记录 → silently no-op</li>
     * </ul>
     *
     * <p>选择 silently no-op 而非抛异常的理由：分布式语义下 release 可能在 TTL 过期
     * 后到达，此时锁可能已被新持有人接管 —— 抛异常会污染调用方代码（处处 try-catch），
     * no-op 让调用方逻辑保持线性。
     *
     * @param token 要释放的锁 token（非 null）
     * @throws NullPointerException 当 {@code token} 为 null
     */
    void release(LockToken token);
}
