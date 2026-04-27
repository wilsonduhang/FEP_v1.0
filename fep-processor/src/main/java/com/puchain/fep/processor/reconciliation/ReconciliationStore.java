package com.puchain.fep.processor.reconciliation;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 对账记录持久化 Port 接口。fep-processor 仅依赖本接口；
 * fep-web 在生产 profile 下提供 JPA Adapter，测试环境/无 DB 场景使用
 * {@link InMemoryReconciliationStore}。
 *
 * <p>对齐 P2a {@code MessageProcessStore} Hexagonal 模式。所有实现必须线程安全。
 * JPA Adapter 必须以 bean 名 {@code jpaReconciliationStore} 注册
 * （{@code @Component("jpaReconciliationStore") @Primary}），以便
 * {@link InMemoryReconciliationStore} 通过
 * {@code @ConditionalOnMissingBean(name = "jpaReconciliationStore")} 自动让位。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public interface ReconciliationStore {

    /**
     * 保存或更新对账记录（upsert 语义）。同一 {@code reconciliationId} 后续调用覆盖先前条目。
     *
     * <p><strong>返回值不保证是同一引用</strong>：内存实现返回入参对象，JPA Adapter
     * 可能返回 Hibernate managed entity 转换后的新 POJO；调用方应使用返回值。</p>
     *
     * @param record 待保存的对账记录，非空
     * @return 已保存的记录（可能与入参为不同实例）
     */
    ReconciliationRecord save(ReconciliationRecord record);

    /**
     * 按业务流水号 + 报文类型查找对账记录。生产环境通过唯一索引
     * {@code uq_recon_serial_message} 保证组合唯一。
     *
     * @param serialNo    业务流水号
     * @param messageType 报文类型（{@code 3107} / {@code 3108} / {@code 3116}）
     * @return 匹配记录的 {@link Optional}；未命中返回 {@link Optional#empty()}
     */
    Optional<ReconciliationRecord> findBySerialNoAndMessageType(String serialNo, String messageType);

    /**
     * 按对账日期 + 状态批量查询。
     *
     * @param date   对账日期
     * @param status 对账状态（{@code PENDING} / {@code IN_PROGRESS} / {@code COMPLETED} / {@code DISCREPANCY}）
     * @return 匹配记录列表，可能为空
     */
    List<ReconciliationRecord> findByDateAndStatus(LocalDate date, String status);

    /**
     * 统计指定日期的对账记录总数。
     *
     * @param date 对账日期
     * @return 非负计数
     */
    long countByDate(LocalDate date);
}
