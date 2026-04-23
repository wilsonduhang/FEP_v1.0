package com.puchain.fep.processor.pipeline;

import java.util.List;
import java.util.Objects;

/**
 * 批量处理结果（PRD §4.7 模式 3 批量流水线）。
 *
 * <p>Compact constructor 强制 {@code processedCount == successCount + failedCount}
 * 不变量；违反抛 {@link IllegalArgumentException}。</p>
 *
 * <p>批量模式不新增 {@code PARTIAL_SUCCESS} 状态；整体状态由
 * {@link MessageStateMachine 状态机} 依据 {@code failedCount == 0} 决策
 * ({@code COMPLETED} 或 {@code FAILED})，失败明细完整保留在 {@link #errors()}。</p>
 *
 * @param processedCount 本批次已处理条数
 * @param successCount 成功条数
 * @param failedCount 失败条数
 * @param errors 失败明细（按 index 升序，批量场景错误信息保留于此而非
 *               {@code message_process_record.error_code/error_message}）
 * @author FEP Team
 * @since 1.0.0
 */
public record BatchResult(
        int processedCount,
        int successCount,
        int failedCount,
        List<BatchError> errors) {

    /**
     * Compact constructor：校验非负 + 总数一致性 + 防御性复制 errors。
     *
     * @throws NullPointerException {@code errors} 为 {@code null}
     * @throws IllegalArgumentException 任一 count 为负，或
     *         {@code processedCount != successCount + failedCount}
     */
    public BatchResult {
        Objects.requireNonNull(errors, "errors");
        if (processedCount < 0 || successCount < 0 || failedCount < 0) {
            throw new IllegalArgumentException("counts must be non-negative");
        }
        if (processedCount != successCount + failedCount) {
            throw new IllegalArgumentException(
                    "processedCount (" + processedCount + ") must equal successCount ("
                            + successCount + ") + failedCount (" + failedCount + ")");
        }
        errors = List.copyOf(errors);
    }

    /**
     * 空批次结果（{@code msg == null} 或无 body 时返回）。
     *
     * @return 全零结果
     */
    public static BatchResult empty() {
        return new BatchResult(0, 0, 0, List.of());
    }

    /**
     * 是否全部成功（且至少处理了 1 条）。
     *
     * @return {@code true} iff {@code failedCount == 0 && processedCount > 0}
     */
    public boolean allSucceeded() {
        return failedCount == 0 && processedCount > 0;
    }

    /**
     * 单条失败明细。
     *
     * @param index 批次内 0-based 索引
     * @param errorMessage 脱敏后的错误描述，非空
     */
    public record BatchError(int index, String errorMessage) {
        /**
         * Compact constructor：校验 {@code index} 非负 + {@code errorMessage} 非空。
         *
         * @throws NullPointerException {@code errorMessage} 为 {@code null}
         * @throws IllegalArgumentException {@code index < 0}
         */
        public BatchError {
            Objects.requireNonNull(errorMessage, "errorMessage");
            if (index < 0) {
                throw new IllegalArgumentException("index must be non-negative");
            }
        }
    }
}
