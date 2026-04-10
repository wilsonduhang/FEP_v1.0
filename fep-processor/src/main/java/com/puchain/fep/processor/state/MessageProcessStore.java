package com.puchain.fep.processor.state;

import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * 报文处理记录持久化端口。fep-processor 仅依赖本接口；
 * fep-web 在生产 profile 下提供 JPA 适配器，测试环境使用
 * {@link InMemoryMessageProcessStore}。
 *
 * <p>所有实现必须线程安全。JPA 适配器必须以 bean 名
 * {@code jpaMessageProcessStore} 注册（{@code @Component("jpaMessageProcessStore") @Primary}），
 * 以便 {@link InMemoryMessageProcessStore} 通过
 * {@code @ConditionalOnMissingBean(name = "jpaMessageProcessStore")} 让位。</p>
 */
public interface MessageProcessStore {

    /**
     * 保存或更新记录（upsert 语义）。{@code save} 幂等：相同 id 的后续调用覆盖先前的内容。
     *
     * <p><strong>返回值不保证是同一引用</strong>：内存实现返回入参对象本身，但
     * JPA 适配器可能返回 Hibernate managed entity 副本，调用方应使用返回值而非入参。</p>
     *
     * @param record 待保存的记录，非空
     * @return 已保存的记录（可能与 {@code record} 为不同实例）
     */
    MessageProcessRecord save(MessageProcessRecord record);

    /**
     * 按主键查找记录。
     *
     * @param id 32 位 UUID
     * @return 记录的 {@link Optional}；未命中返回 {@link Optional#empty()}
     */
    Optional<MessageProcessRecord> findById(String id);

    /**
     * 按业务流水号查找记录。生产环境应通过数据库唯一索引保证 {@code transitionNo}
     * 全局唯一；若实现侧检测到多命中（数据异常），应快速失败。
     *
     * @param transitionNo 业务流水号
     * @return 单条匹配的 {@link Optional}；未命中返回 {@link Optional#empty()}
     * @throws IllegalStateException 同一 {@code transitionNo} 存在多条记录
     */
    Optional<MessageProcessRecord> findByTransitionNo(String transitionNo);

    /**
     * 更新指定记录的状态。若 {@code newStatus} 为
     * {@link MessageProcessStatus#FAILED}，{@code errorCode} 与
     * {@code errorMessage} 会被一并写入；否则二者可为 {@code null}。
     *
     * <p>本方法不校验状态转移合法性，合法性由
     * {@code MessageStateMachine}（Task 5）在调用本方法前守护。</p>
     *
     * @param id 目标记录主键
     * @param newStatus 目标状态，非空
     * @param errorCode 错误码（仅 FAILED 态必填）
     * @param errorMessage 错误描述（仅 FAILED 态必填）
     * @return 更新后的记录
     * @throws NoSuchElementException 指定 {@code id} 不存在
     */
    MessageProcessRecord updateStatus(String id,
                                      MessageProcessStatus newStatus,
                                      String errorCode,
                                      String errorMessage);

    /**
     * 统计指定状态的记录总数，主要用于监控指标上报。
     *
     * @param status 待统计的状态
     * @return 匹配记录数，非负
     */
    long countByStatus(MessageProcessStatus status);
}
