package com.puchain.fep.collector.support;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 基于 {@link ConcurrentHashMap} 的内存水位存储。
 *
 * <p><b>仅供单测使用</b>，生产环境用 {@code com.puchain.fep.web.collector.JpaWatermarkStore}
 * （T8 引入；T8-fix 由 JDBC MERGE 迁移到 JPA Repository 以支持 MySQL 8 dialect），
 * 后者支持持久化、跨进程共享与 backup 恢复。本实现进程结束后水位即丢失。
 *
 * <p><b>未声明为 Spring Bean</b>：保持工具类身份，由调用方（一般是测试）显式 new 出来注入，
 * 避免误装配到生产 ApplicationContext 而破坏水位持久化语义。
 *
 * <p><b>线程安全：</b>{@link ConcurrentHashMap} 后端，{@link #get} / {@link #put}
 * 均原子。
 *
 * @author FEP Team
 * @since 1.0.0
 */
public final class InMemoryWatermarkStore implements WatermarkStore {

    private final ConcurrentMap<String, String> store = new ConcurrentHashMap<>();

    @Override
    public Optional<String> get(final String adapterId) {
        Objects.requireNonNull(adapterId, "adapterId");
        return Optional.ofNullable(store.get(adapterId));
    }

    @Override
    public void put(final String adapterId, final String watermark) {
        Objects.requireNonNull(adapterId, "adapterId");
        Objects.requireNonNull(watermark, "watermark");
        store.put(adapterId, watermark);
    }
}
