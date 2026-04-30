package com.puchain.fep.processor.routing;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.event.DirMapConfigChangedEvent;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.DependsOn;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Port-backed direction map with Caffeine cache.
 *
 * <p><b>Two-level lookup</b>（lookupRaw 入口）：
 * <ol>
 *   <li>Caffeine cache（启动期 + 事件触发 reload 整体替换）</li>
 *   <li>{@link DirMapConfigStore#findOne} 按需回查（cache miss 但 DB 在线）</li>
 * </ol>
 * 静态 fallback（{@link MessageDirectionMap#staticLookup}）由
 * {@link MessageDirectionMap#lookup} 在 {@link #lookupRaw} 返回 empty 时承接，
 * 避免本类内部 dynamic ↔ static 互调环。
 *
 * <p><b>D5 fallback 策略</b>：{@link DirMapConfigStore#findAll()} 在启动 / reload 时
 * 抛 {@link DataAccessException}（含 {@code QueryTimeoutException} / 连接失败 /
 * 表不存在），日志 WARN 后保持 cache 上次状态（启动期则空），
 * {@link MessageDirectionMap#lookup} 在本方法返回 empty 时走静态 fallback；
 * <b>不</b>抛异常 / <b>不</b>阻断 Spring 启动。</p>
 *
 * <p><b>D7 Hexagonal</b>：本类仅依赖 {@link DirMapConfigStore} Port 接口与
 * {@link ApplicationEventPublisher}，<b>不</b>依赖 spring-data-jpa / JPA Repository。
 * 实际 JPA 实现在 fep-web 的 {@code JpaDirMapConfigStore} Adapter（T2 owner）。</p>
 *
 * <p><b>{@code @DependsOn("messageDirectionMapBridge")}</b>：T4 引入的 Bridge bean
 * 通过 {@link MessageDirectionMapBridge#setDynamic(DynamicMessageDirectionMap)}
 * 静态 setter 完成 dynamic 实例注入；本类 {@link #PostConstruct} 必须在 Bridge
 * 完成后再 reload，避免静态 fallback 与 dynamic cache 同时为空时 lookup 路径不一致。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
@DependsOn("messageDirectionMapBridge")
public class DynamicMessageDirectionMap {

    private static final Logger log = LoggerFactory.getLogger(DynamicMessageDirectionMap.class);

    /** Caffeine cache 容量上限（88 行 + headroom）。 */
    private static final long CACHE_MAX_SIZE = 200L;

    private final DirMapConfigStore store;
    private final ApplicationEventPublisher publisher;
    private final Cache<DirMapKey, DirMapConfigSnapshot> cache;

    /**
     * Spring DI 构造函数。构造末尾显式调
     * {@link MessageDirectionMapBridge#setDynamic(DynamicMessageDirectionMap)}
     * 注入 Bridge 静态字段，使 {@link MessageDirectionMap#lookup} 公共 API
     * 走 Bridge → 本实例 → cache + Port → 静态 fallback 三级链。
     *
     * @param store     {@link DirMapConfigStore} Port 实现，非 null
     * @param publisher Spring {@link ApplicationEventPublisher}，非 null
     */
    public DynamicMessageDirectionMap(final DirMapConfigStore store,
                                      final ApplicationEventPublisher publisher) {
        this.store = Objects.requireNonNull(store, "store");
        this.publisher = Objects.requireNonNull(publisher, "publisher");
        this.cache = Caffeine.newBuilder()
                .maximumSize(CACHE_MAX_SIZE)
                .build();
        // v1g P0-A — wire dynamic instance into Bridge so MessageDirectionMap.lookup
        // delegates here. Round 4 F P0-2 / G P0-3 抓出 v1f 漏调用此契约 → DB 编辑不生效。
        MessageDirectionMapBridge.setDynamic(this);
    }

    /**
     * 启动期载入。Spring 容器初始化后调用一次；DB 异常静默 fallback 不阻断启动。
     */
    @PostConstruct
    public void load() {
        loadInto(this.cache);
    }

    /**
     * 整体替换 cache（atomic putAll）。供 {@link DirMapCacheInvalidator} 事件
     * 触发使用；DB 异常时保持 cache 上次状态（不清空已有 cache）。
     */
    public void reload() {
        Cache<DirMapKey, DirMapConfigSnapshot> next = Caffeine.newBuilder()
                .maximumSize(CACHE_MAX_SIZE)
                .build();
        boolean loaded = loadInto(next);
        if (loaded) {
            this.cache.invalidateAll();
            this.cache.putAll(asMap(next));
            log.info("DIR-MAP cache reloaded: {} entries from store", this.cache.estimatedSize());
        } else {
            log.warn("DIR-MAP reload skipped: store unavailable, keeping previous cache "
                    + "({} entries)", this.cache.estimatedSize());
        }
    }

    /**
     * Cache + Port 二级查询（<b>不</b>走静态 fallback）。
     *
     * <p>v1g P0-D 修复 — Round 4 G P0-1 抓出 v1f T3 用 {@code lookup} 但 T4/T7 期望
     * {@code lookupRaw}，方法签名跨 Task 漂移。本方法承担"读 cache，未命中则查 Port，
     * 仍未命中或 DB 异常返回 {@code Optional.empty()}"职责；静态 fallback 由
     * {@link MessageDirectionMap#lookup(MessageType, AccessRole)} 在本方法返回 empty
     * 时承接（dynamic-bypass 入口 = {@link MessageDirectionMap#staticLookup}）。</p>
     *
     * @param msg  报文类型，非 null
     * @param role 接入角色，非 null
     * @return 匹配 {@link DirectionMapping}；DB 范围外返回 {@link Optional#empty()}
     */
    public Optional<DirectionMapping> lookupRaw(final MessageType msg, final AccessRole role) {
        Objects.requireNonNull(msg, "msg");
        Objects.requireNonNull(role, "role");
        DirMapKey key = new DirMapKey(msg, role);

        DirMapConfigSnapshot cached = this.cache.getIfPresent(key);
        if (cached != null) {
            return Optional.of(toMapping(cached));
        }

        try {
            Optional<DirMapConfigSnapshot> queried = this.store.findOne(msg, role);
            if (queried.isPresent()) {
                this.cache.put(key, queried.get());
                return Optional.of(toMapping(queried.get()));
            }
        } catch (DataAccessException ex) {
            log.warn("DIR-MAP findOne failed for ({}, {}): {}", msg, role, ex.getMessage());
        }

        return Optional.empty();
    }

    /**
     * Cache 当前条目数（IT 实证用 — Round 2 C P0-3 修复）。
     *
     * <p>正常启动场景: 88 / DB 不可达 fallback 场景: 0。供 IT 区分
     * "cache 真的 loaded" vs "fallback 巧合命中相同结果"。</p>
     *
     * @return 非负计数
     */
    public long cacheSize() {
        return this.cache.estimatedSize();
    }

    private boolean loadInto(final Cache<DirMapKey, DirMapConfigSnapshot> target) {
        try {
            List<DirMapConfigSnapshot> rows = this.store.findAll();
            for (DirMapConfigSnapshot row : rows) {
                target.put(new DirMapKey(row.messageType(), row.accessRole()), row);
            }
            log.info("DIR-MAP cache loaded: {} entries from store", target.estimatedSize());
            return true;
        } catch (DataAccessException ex) {
            log.warn("DIR-MAP store unavailable, cache stays empty (static fallback active): {}",
                    ex.getMessage());
            return false;
        }
    }

    private static Map<DirMapKey, DirMapConfigSnapshot> asMap(
            final Cache<DirMapKey, DirMapConfigSnapshot> source) {
        Map<DirMapKey, DirMapConfigSnapshot> copy = new HashMap<>();
        source.asMap().forEach(copy::put);
        return copy;
    }

    private static DirectionMapping toMapping(final DirMapConfigSnapshot snapshot) {
        return new DirectionMapping(snapshot.direction(), snapshot.requiresFep(), snapshot.mode());
    }

    /**
     * 测试用：手动发布事件触发 reload，模拟 fep-web Service 层调用路径。
     * 仅供 IT 用，生产代码不调用。
     *
     * @param event 待发布事件，非 null
     */
    void publishReloadEvent(final DirMapConfigChangedEvent event) {
        this.publisher.publishEvent(Objects.requireNonNull(event, "event"));
    }
}
