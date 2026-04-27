package com.puchain.fep.processor.reconciliation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * 内存版 {@link ClearingInstructionStore}。默认注册为 Spring bean；仅当
 * fep-web 的 JPA Adapter（bean 名 {@code jpaClearingInstructionStore}）缺席时生效。
 *
 * <p>复合主键 ({@code instructionId}, {@code qsSerialNo}) 通过
 * {@link Map.Entry} 形式作为内部键。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
@ConditionalOnMissingBean(name = "jpaClearingInstructionStore")
public class InMemoryClearingInstructionStore implements ClearingInstructionStore {

    private final ConcurrentMap<CompositeKey, ClearingInstructionRecord> store = new ConcurrentHashMap<>();

    @Override
    public ClearingInstructionRecord save(final ClearingInstructionRecord record) {
        Objects.requireNonNull(record, "record");
        store.put(new CompositeKey(record.getInstructionId(), record.getQsSerialNo()), record);
        return record;
    }

    @Override
    public Optional<ClearingInstructionRecord> findByInstructionIdAndQsSerialNo(final String instructionId,
                                                                                 final String qsSerialNo) {
        Objects.requireNonNull(instructionId, "instructionId");
        Objects.requireNonNull(qsSerialNo, "qsSerialNo");
        return Optional.ofNullable(store.get(new CompositeKey(instructionId, qsSerialNo)));
    }

    @Override
    public List<ClearingInstructionRecord> findByMessageId(final String messageId) {
        Objects.requireNonNull(messageId, "messageId");
        return store.values().stream()
                .filter(r -> messageId.equals(r.getMessageId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ClearingInstructionRecord> findByStatus(final String status) {
        Objects.requireNonNull(status, "status");
        return store.values().stream()
                .filter(r -> status.equals(r.getInstructionStatus()))
                .collect(Collectors.toList());
    }

    /**
     * 复合主键内部值类型。仅用于内存索引，不暴露给 Port 调用方。
     */
    private static final class CompositeKey {
        private final String instructionId;
        private final String qsSerialNo;

        CompositeKey(final String instructionId, final String qsSerialNo) {
            this.instructionId = instructionId;
            this.qsSerialNo = qsSerialNo;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof CompositeKey other)) {
                return false;
            }
            return instructionId.equals(other.instructionId)
                    && qsSerialNo.equals(other.qsSerialNo);
        }

        @Override
        public int hashCode() {
            return Objects.hash(instructionId, qsSerialNo);
        }
    }
}
