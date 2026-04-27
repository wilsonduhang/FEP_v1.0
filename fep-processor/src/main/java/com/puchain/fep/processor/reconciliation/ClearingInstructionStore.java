package com.puchain.fep.processor.reconciliation;

import java.util.List;
import java.util.Optional;

/**
 * 清算指令持久化 Port 接口。fep-processor 仅依赖本接口；fep-web 在生产 profile
 * 下提供 JPA Adapter（bean 名 {@code jpaClearingInstructionStore}），测试环境/
 * 无 DB 场景使用 {@link InMemoryClearingInstructionStore}。
 *
 * <p>对齐 P2a {@code MessageProcessStore} Hexagonal 模式。所有实现必须线程安全。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public interface ClearingInstructionStore {

    /**
     * 保存或更新清算指令记录（upsert 语义）。复合主键
     * ({@code instructionId}, {@code qsSerialNo}) 后续调用覆盖先前条目。
     *
     * @param record 待保存的清算指令，非空
     * @return 已保存的记录（可能与入参为不同实例）
     */
    ClearingInstructionRecord save(ClearingInstructionRecord record);

    /**
     * 按复合主键查找清算指令。
     *
     * @param instructionId 指令 ID
     * @param qsSerialNo    清算流水号
     * @return 匹配记录的 {@link Optional}；未命中返回 {@link Optional#empty()}
     */
    Optional<ClearingInstructionRecord> findByInstructionIdAndQsSerialNo(String instructionId, String qsSerialNo);

    /**
     * 按报文 ID 查找关联的所有清算指令。
     *
     * @param messageId 报文处理记录 ID
     * @return 匹配记录列表，可能为空
     */
    List<ClearingInstructionRecord> findByMessageId(String messageId);

    /**
     * 按指令状态批量查询，主要用于监控指标 / 失败重试扫描。
     *
     * @param status 指令状态（{@code PENDING} / {@code PROCESSING} / {@code SUCCESS} / {@code FAILED}）
     * @return 匹配记录列表，可能为空
     */
    List<ClearingInstructionRecord> findByStatus(String status);
}
