package com.puchain.fep.processor.state;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 内存版 {@link MessageProcessStore}。默认注册为 Spring bean；仅当
 * fep-web 的 JPA 适配器（bean 名 {@code jpaMessageProcessStore}）缺席时生效，
 * 允许 fep-processor 单模块测试与 IT 独立运行。
 *
 * <p><strong>JPA 适配器契约</strong>：Task 10 交付的
 * {@code JpaMessageProcessStore} 必须以
 * {@code @Component("jpaMessageProcessStore") @Primary} 注册，
 * 以便本实现通过 {@code @ConditionalOnMissingBean(name = "jpaMessageProcessStore")}
 * 自动让位；否则两个实现同时存在会引起 {@code NoUniqueBeanDefinitionException}。</p>
 *
 * <p><strong>线程安全</strong>：底层使用 {@link ConcurrentHashMap}，
 * {@link #save}、{@link #findById}、{@link #findByTransitionNo}、{@link #countByStatus}
 * 均为线程安全的原子或弱一致读操作。</p>
 *
 * <p><strong>并发限制</strong>：{@link #updateStatus} 内部先 {@code get} 再 {@code put}，
 * 两步非原子——两条线程并发更新同一 {@code id} 可能发生"lost update"。
 * 仅适用于单线程测试环境；生产环境由 JPA 适配器以数据库事务 + 乐观锁保证一致性。</p>
 *
 * <p><strong>幂等性</strong>：{@link #save} 相同 {@code id} 的后续调用覆盖先前条目；
 * {@code transitionNo} 要求全局唯一，同流水号多条写入时
 * {@link #findByTransitionNo} 会抛出 {@link IllegalStateException} 以快速暴露测试数据污染。</p>
 */
@Component
@ConditionalOnMissingBean(name = "jpaMessageProcessStore")
public class InMemoryMessageProcessStore implements MessageProcessStore {

    private final ConcurrentMap<String, MessageProcessRecord> byId = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> transitionNoToId = new ConcurrentHashMap<>();

    @Override
    public MessageProcessRecord save(final MessageProcessRecord record) {
        byId.put(record.getId(), record);
        String prev = transitionNoToId.putIfAbsent(record.getTransitionNo(), record.getId());
        if (prev != null && !prev.equals(record.getId())) {
            throw new IllegalStateException(
                    "Duplicate records for transitionNo=" + record.getTransitionNo());
        }
        return record;
    }

    @Override
    public Optional<MessageProcessRecord> findById(final String id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<MessageProcessRecord> findByTransitionNo(final String transitionNo) {
        String id = transitionNoToId.get(transitionNo);
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public MessageProcessRecord updateStatus(final String id,
                                             final MessageProcessStatus newStatus,
                                             final String errorCode,
                                             final String errorMessage) {
        MessageProcessRecord existing = byId.get(id);
        if (existing == null) {
            throw new NoSuchElementException("record not found: " + id);
        }
        MessageProcessRecord updated = (newStatus == MessageProcessStatus.FAILED)
                ? existing.withFailure(errorCode, errorMessage, Instant.now())
                : existing.withStatus(newStatus, Instant.now());
        byId.put(id, updated);
        return updated;
    }

    @Override
    public long countByStatus(final MessageProcessStatus status) {
        return byId.values().stream().filter(r -> r.getStatus() == status).count();
    }
}
