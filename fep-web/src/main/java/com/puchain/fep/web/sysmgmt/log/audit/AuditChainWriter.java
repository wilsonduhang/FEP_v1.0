package com.puchain.fep.web.sysmgmt.log.audit;

import com.puchain.fep.security.api.AuditIntegrityService;
import com.puchain.fep.web.sysmgmt.log.domain.SysOperationLog;
import com.puchain.fep.web.sysmgmt.log.repository.SysOperationLogRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 审计日志链式写入（单写者串行化，架构 §1219 不可篡改）。
 *
 * <p>单实例部署假设下以 synchronized 临界区保证链不分叉；DB uk_audit_seq 唯一约束
 * 兜底（意外多实例时链分叉立即以唯一键冲突暴露而非静默）。save 经 REQUIRES_NEW
 * 独立事务提交（外层业务事务 rollback 不产生"行消失而链尾已推进"假阳）；提交成功
 * 才推进内存链尾——失败时链尾不动，下次复用同 seq，链无空洞。启动期从链尾行恢复。</p>
 *
 * <p>链尾 seq 经 Micrometer gauge {@code fep_audit_chain_tail_seq} 外锚（纯删尾截断
 * 攻击的监控侧告警线索，Plan 抉择⑩；EFF-S5-1 后主检测已升级为持久化 checkpoint 锚的
 * {@code TRUNCATION} 断点，本 gauge 降为回退/删锚联合攻击的兜底双外锚之一）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class AuditChainWriter {

    private static final Logger LOG = LoggerFactory.getLogger(AuditChainWriter.class);

    /** MDC 链路追踪键（TraceIdFilter 约定）。 */
    private static final String MDC_TRACE_ID = "traceId";

    private final SysOperationLogRepository repository;
    private final AuditIntegrityService auditIntegrityService;
    private final TransactionTemplate requiresNewTx;
    private final AtomicLong tailSeqGauge = new AtomicLong();

    private long lastSeq;
    private String lastHash;

    /**
     * 构造（落库 REQUIRES_NEW 独立事务 + gauge 外锚）。
     *
     * @param repository            操作日志仓储
     * @param auditIntegrityService 完整性原语
     * @param transactionManager    事务管理器
     * @param meterRegistry         指标注册
     */
    public AuditChainWriter(final SysOperationLogRepository repository,
            final AuditIntegrityService auditIntegrityService,
            final PlatformTransactionManager transactionManager,
            final MeterRegistry meterRegistry) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.auditIntegrityService = Objects.requireNonNull(auditIntegrityService,
                "auditIntegrityService");
        this.requiresNewTx = new TransactionTemplate(
                Objects.requireNonNull(transactionManager, "transactionManager"));
        this.requiresNewTx.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        Gauge.builder("fep_audit_chain_tail_seq", tailSeqGauge, AtomicLong::get)
                .description("审计 hash 链尾 seq（回退即截断告警线索，架构 §1219）")
                .register(Objects.requireNonNull(meterRegistry, "meterRegistry"));
    }

    /**
     * 启动期链尾恢复：取 seq 最大行；无链上行则从 GENESIS 起。
     * 亦作 poison-state 自愈入口（Plan v0.2 C-2）。
     */
    @PostConstruct
    public synchronized void recoverChainTail() {
        final Optional<SysOperationLog> tail = repository.findTopBySeqIsNotNullOrderBySeqDesc();
        if (tail.isPresent()) {
            this.lastSeq = tail.get().getSeq();
            this.lastHash = tail.get().getHash();
        } else {
            this.lastSeq = 0L;
            this.lastHash = AuditIntegrityService.GENESIS_PREV_HASH;
        }
        tailSeqGauge.set(lastSeq);
        // 仅 long seq 入日志（无 taint 字段，CRLF 面为零）
        LOG.info("audit chain tail recovered: seq={}", lastSeq);
        try {
            auditIntegrityService.auditKeyId();
        } catch (final IllegalStateException e) {
            // Plan v0.3 抉择⑪：impl 域 audit 段漏配 = 部署错误，append 将持续失败
            // （切面 WARN 吞）——启动期一次性醒目告警（纯常量文案，零 CRLF taint 面）
            LOG.warn("audit signing keys not configured - operation log rows will fail "
                    + "to persist until fep.security.sm2.audit-* is provided");
        }
    }

    /**
     * 链式落库：分配 seq、链接 prevHash、计算 hash 与行签名后独立事务保存。
     *
     * @param entity 已填业务字段的日志实体（seq/hash 等完整性字段由本方法填充）
     */
    public synchronized void append(final SysOperationLog entity) {
        final long seq = lastSeq + 1;
        entity.setSeq(seq);
        entity.setPrevHash(lastHash);
        entity.setTraceId(MDC.get(MDC_TRACE_ID));
        final byte[] canonical = AuditCanonicalizer.canonicalize(entity, seq)
                .getBytes(StandardCharsets.UTF_8);
        final String hash = auditIntegrityService.computeEntryHash(lastHash, canonical);
        entity.setHash(hash);
        entity.setSignature(auditIntegrityService.signEntryHash(hash));
        entity.setSignKeyId(auditIntegrityService.auditKeyId());
        try {
            requiresNewTx.executeWithoutResult(status -> repository.save(entity));
        } catch (final DataIntegrityViolationException e) {
            // 意外 seq 占用（如多实例误部署）：重锚 DB 链尾自愈后上抛（切面 WARN 吞），
            // 下一次 append 在正确链尾继续——poison-state 不静默持续（Plan v0.2 C-2）
            recoverChainTail();
            throw e;
        }
        // 提交成功才推进链尾（失败时下次复用同 seq，链无空洞）
        this.lastSeq = seq;
        this.lastHash = hash;
        tailSeqGauge.set(seq);
    }
}
