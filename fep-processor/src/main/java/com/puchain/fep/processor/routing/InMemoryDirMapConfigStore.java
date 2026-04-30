package com.puchain.fep.processor.routing;

import com.puchain.fep.converter.type.MessageType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 内存版 {@link DirMapConfigStore}。默认注册为 Spring bean；仅当 fep-web 的
 * JPA Adapter（bean 名 {@code jpaDirMapConfigStore}）缺席时生效，允许 fep-processor
 * 单模块测试 / 无 DB 部署 / IT 独立运行。
 *
 * <p><b>JPA Adapter 契约</b>：fep-web {@code JpaDirMapConfigStore} 必须以
 * {@code @Component("jpaDirMapConfigStore") @Primary} 注册，以便本实现通过
 * {@code @ConditionalOnMissingBean(name = "jpaDirMapConfigStore")} 自动让位。
 * 模式与 P2e {@code com.puchain.fep.processor.reconciliation.InMemoryReconciliationStore} 一致。</p>
 *
 * <p>初始数据来自 {@link MessageDirectionMap} 88 条静态常量，避免 Adapter 缺席时
 * fep-processor 启动期空 cache。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
@ConditionalOnMissingBean(name = "jpaDirMapConfigStore")
public class InMemoryDirMapConfigStore implements DirMapConfigStore {

    private static final String SYSTEM_USER = "system";

    private final ConcurrentMap<DirMapKey, DirMapConfigSnapshot> store = new ConcurrentHashMap<>();

    /**
     * 默认构造函数：从 {@link MessageDirectionMap#entries()} 读取 88 条静态常量
     * 作为初始数据。
     */
    public InMemoryDirMapConfigStore() {
        Instant now = Instant.now();
        for (Map.Entry<DirMapKey, DirectionMapping> e : MessageDirectionMap.entries().entrySet()) {
            DirMapKey key = e.getKey();
            DirectionMapping mapping = e.getValue();
            store.put(key, new DirMapConfigSnapshot(
                    key.msg(), key.role(),
                    mapping.direction(), mapping.requiresFep(), mapping.mode(),
                    SYSTEM_USER, now));
        }
    }

    @Override
    public List<DirMapConfigSnapshot> findAll() {
        return new ArrayList<>(store.values());
    }

    @Override
    public Optional<DirMapConfigSnapshot> findOne(final MessageType msg, final AccessRole role) {
        Objects.requireNonNull(msg, "msg");
        Objects.requireNonNull(role, "role");
        return Optional.ofNullable(store.get(new DirMapKey(msg, role)));
    }

    @Override
    public DirMapConfigSnapshot update(final DirMapConfigUpdate update) {
        Objects.requireNonNull(update, "update");
        DirMapKey key = new DirMapKey(update.messageType(), update.accessRole());
        DirMapConfigSnapshot updated = new DirMapConfigSnapshot(
                update.messageType(), update.accessRole(),
                update.direction(), update.requiresFep(), update.mode(),
                update.updatedBy(), Instant.now());
        store.put(key, updated);
        return updated;
    }

    @Override
    public long count() {
        return store.size();
    }
}
