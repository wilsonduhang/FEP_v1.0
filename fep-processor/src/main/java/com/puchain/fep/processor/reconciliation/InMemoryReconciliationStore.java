package com.puchain.fep.processor.reconciliation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * 内存版 {@link ReconciliationStore}。默认注册为 Spring bean；仅当
 * fep-web 的 JPA Adapter（bean 名 {@code jpaReconciliationStore}）缺席时生效，
 * 允许 fep-processor 单模块测试与 IT 独立运行。
 *
 * <p><strong>JPA Adapter 契约</strong>：fep-web {@code JpaReconciliationStore} 必须以
 * {@code @Component("jpaReconciliationStore") @Primary} 注册，以便本实现通过
 * {@code @ConditionalOnMissingBean(name = "jpaReconciliationStore")} 自动让位。</p>
 *
 * <p><strong>线程安全</strong>：底层使用 {@link ConcurrentHashMap}；
 * {@link #findBySerialNoAndMessageType} / {@link #findByDateAndStatus} 为弱一致读，
 * 满足单测与无 DB 部署场景。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
@ConditionalOnMissingBean(name = "jpaReconciliationStore")
public class InMemoryReconciliationStore implements ReconciliationStore {

    private final ConcurrentMap<String, ReconciliationRecord> store = new ConcurrentHashMap<>();

    @Override
    public ReconciliationRecord save(final ReconciliationRecord record) {
        Objects.requireNonNull(record, "record");
        store.put(record.getReconciliationId(), record);
        return record;
    }

    @Override
    public Optional<ReconciliationRecord> findBySerialNoAndMessageType(final String serialNo,
                                                                       final String messageType) {
        Objects.requireNonNull(serialNo, "serialNo");
        Objects.requireNonNull(messageType, "messageType");
        return store.values().stream()
                .filter(r -> serialNo.equals(r.getSerialNo())
                        && messageType.equals(r.getMessageType()))
                .findFirst();
    }

    @Override
    public List<ReconciliationRecord> findByDateAndStatus(final LocalDate date, final String status) {
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(status, "status");
        return store.values().stream()
                .filter(r -> date.equals(r.getReconciliationDate())
                        && status.equals(r.getStatus()))
                .collect(Collectors.toList());
    }

    @Override
    public long countByDate(final LocalDate date) {
        Objects.requireNonNull(date, "date");
        return store.values().stream()
                .filter(r -> date.equals(r.getReconciliationDate()))
                .count();
    }
}
