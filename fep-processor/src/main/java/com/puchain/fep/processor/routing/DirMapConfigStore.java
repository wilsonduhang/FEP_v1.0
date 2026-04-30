package com.puchain.fep.processor.routing;

import com.puchain.fep.converter.type.MessageType;

import java.util.List;
import java.util.Optional;

/**
 * 报文方向映射配置持久化 Port 接口。
 *
 * <p><b>Hexagonal Port + Adapter（muzhou D7 决策 2026-04-29）</b>：
 * fep-processor 不依赖 spring-data-jpa；fep-web 在 production profile 下
 * 提供 JPA Adapter（{@code @Component("jpaDirMapConfigStore") @Primary}），
 * 测试 / 单模块场景使用 {@link InMemoryDirMapConfigStore}。模式与 P2e
 * {@code com.puchain.fep.processor.reconciliation.ReconciliationStore} 一致。</p>
 *
 * <p>所有实现必须线程安全。{@link #findAll()} 用于启动期 + reload 全量载入；
 * {@link #findOne} 用于 cache miss 时按需回查；{@link #update} 由 fep-web
 * Service 层调用并由实现自行决定是否同步写 history audit row；{@link #count}
 * 供 Service 层 88 行不变性 {@code Assert.state} 兜底（D8 应用层）。</p>
 *
 * <p><b>禁止</b>：实现返回 JPA Entity；返回类型一律为 {@link DirMapConfigSnapshot}
 * record，避免 Hibernate managed entity 跨模块泄漏。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public interface DirMapConfigStore {

    /**
     * 全量载入所有方向映射配置。启动期 {@code @PostConstruct} + 事件触发
     * {@code reload()} 都调用本方法。
     *
     * @return 全部映射 Snapshot 列表（生产期固定 88 条），永不为 null
     */
    List<DirMapConfigSnapshot> findAll();

    /**
     * 按 (msgType, accessRole) 复合键查询单条映射。供 cache miss 时按需回查。
     *
     * @param msg  报文类型，非 null
     * @param role 接入角色，非 null
     * @return 匹配 Snapshot 的 Optional；未命中返回 {@link Optional#empty()}
     */
    Optional<DirMapConfigSnapshot> findOne(MessageType msg, AccessRole role);

    /**
     * 更新单条映射并写 history audit row（具体写 history 逻辑由 Adapter
     * 内部决定，Port 接口不规定）。
     *
     * @param update 更新参数，非 null
     * @return 更新后的 Snapshot；调用方应使用返回值（Hibernate managed entity
     *     转换后可能为新实例）
     */
    DirMapConfigSnapshot update(DirMapConfigUpdate update);

    /**
     * 统计当前配置行数。供 fep-web Service 层 {@code Assert.state(count == 88)}
     * 兜底（D8 应用层快速失败 + schema trigger 双重守护 88 行不变性）。
     *
     * @return 非负计数
     */
    long count();
}
