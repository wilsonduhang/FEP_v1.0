package com.puchain.fep.web.requeststate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

/**
 * 请求生命周期单写者状态机 Service（S2 request-state tracking）。
 *
 * <p>作为 {@link RequestStateEntity} 5 态生命周期（CREATED→SENT→RESULT_RECEIVED，旁支
 * FAILED/STUCK）的<strong>唯一写入入口</strong>。correlation key = 8 位业务 transitionNo，
 * 经 {@link TransitionNoNormalizer#canonical(String)} 归一后比对（两侧同源 8 位，见该类 Javadoc）。</p>
 *
 * <p><b>Tx 边界（best-effort 隔离）</b>：每个 {@code create}/{@code markXxx} 方法
 * {@code @Transactional(propagation = REQUIRES_NEW)}——在<strong>独立挂起事务</strong>内完成，
 * 与 caller（outbound enqueue hook / inbound 结果 listener）事务隔离。如此 request_state 追踪写入
 * 失败（如 correlation_key UNIQUE 碰撞 / DB 瞬态故障）回滚仅限本短事务，<strong>不标记 caller 事务
 * rollback-only、不连累 outbound 主发送流程</strong>（retrofit 追踪表 best-effort 设计意图，Plan §架构）。
 * 反向代价：request_state 行可能在 caller 后续回滚时成为 orphan CREATED 行——但 reaper
 * {@code findStuck} 仅扫 SENT 态，orphan CREATED 不被误处理（仅 cosmetic drift，可接受）。</p>
 *
 * <p><b>Spring 代理约束</b>：{@code @Transactional} 由外部 Bean 调用方触发 AOP 代理，本类内部不自调用
 * 带 Tx 的方法。</p>
 *
 * <p><b>未匹配语义</b>：{@link #markSent}/{@link #markResultReceived}/{@link #markFailed} 对不存在
 * 的 correlation key 返回 {@code false}（不抛异常）——inbound 结果可能对应非本 FEP 发起的请求，或
 * correlation 行已被清理，属正常 unmatched 而非错误。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class RequestStateService {

    private final RequestStateRepository repository;

    /**
     * Spring 构造器注入。
     *
     * @param repository request_state JPA repository（非空）
     */
    public RequestStateService(final RequestStateRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    /**
     * 新建 {@link RequestStateLifecycle#CREATED} 行（outbound enqueue hook 调用）。
     *
     * <p>correlation key 经归一后存储；{@code correlation_blocked} 按
     * {@link BlockedMessageTypes#isBlocked(String)}（报文号判定）置位。</p>
     *
     * @param correlationKey  8 位业务 transitionNo（归一前，可带边界空白），非空
     * @param messageType     报文号（4 位，如 "3115"），非空
     * @param outboundQueueId 关联 outbound 队列主键，可空
     * @throws NullPointerException correlationKey 归一后为空 / messageType 为 null
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void create(final String correlationKey, final String messageType,
                       final String outboundQueueId) {
        final String canonicalKey = Objects.requireNonNull(
                TransitionNoNormalizer.canonical(correlationKey),
                "correlationKey must not be null or blank after normalization");
        Objects.requireNonNull(messageType, "messageType");
        final boolean blocked = BlockedMessageTypes.isBlocked(messageType);
        repository.save(RequestStateEntity.created(
                canonicalKey, messageType, outboundQueueId, blocked));
    }

    /**
     * 标记已发送（outbound TLQ send 成功 hook）：找到 correlation 行 → {@link RequestStateEntity#markSent()}。
     *
     * <p>幂等：对已 SENT 行重复调用刷新 {@code sentAt}（{@code markSent} 无前置状态校验，重复执行
     * 安全）；对不存在行返回 {@code false} 不抛。</p>
     *
     * @param correlationKey 8 位业务 transitionNo（归一前）
     * @return {@code true} 命中并更新；{@code false} unmatched（无该 correlation 行 / key 归一后为空）
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markSent(final String correlationKey) {
        return findByCanonicalKey(correlationKey).map(entity -> {
            entity.markSent();
            repository.save(entity);
            return true;
        }).orElse(false);
    }

    /**
     * 标记结果已返回（inbound 结果归一匹配 hook）：找到 correlation 行 →
     * {@link RequestStateEntity#markResultReceived(String, String)} 回填 inbound 字段。
     *
     * @param correlationKey      8 位业务 transitionNo（归一前），inbound 结果归一值
     * @param serialNo            inbound 结果业务流水号，可空
     * @param inboundTransitionNo inbound transitionNo（归一前，存储前再归一），可空
     * @return {@code true} 命中并回填；{@code false} unmatched（无该 correlation 行，正常情况不抛）
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markResultReceived(final String correlationKey, final String serialNo,
                                      final String inboundTransitionNo) {
        return findByCanonicalKey(correlationKey).map(entity -> {
            entity.markResultReceived(serialNo,
                    TransitionNoNormalizer.canonical(inboundTransitionNo));
            repository.save(entity);
            return true;
        }).orElse(false);
    }

    /**
     * 标记失败（outbound 永久失败 / DLQ hook）：找到 correlation 行 →
     * {@link RequestStateEntity#markFailed()}。
     *
     * @param correlationKey 8 位业务 transitionNo（归一前）
     * @return {@code true} 命中并更新；{@code false} unmatched（无该 correlation 行）
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean markFailed(final String correlationKey) {
        return findByCanonicalKey(correlationKey).map(entity -> {
            entity.markFailed();
            repository.save(entity);
            return true;
        }).orElse(false);
    }

    private Optional<RequestStateEntity> findByCanonicalKey(final String correlationKey) {
        final String canonicalKey = TransitionNoNormalizer.canonical(correlationKey);
        if (canonicalKey == null) {
            return Optional.empty();
        }
        return repository.findByCorrelationKey(canonicalKey);
    }
}
