package com.puchain.fep.web.sysmgmt.log.audit;

import com.puchain.fep.security.api.AuditIntegrityService;
import com.puchain.fep.web.sysmgmt.log.domain.SysOperationLog;
import com.puchain.fep.web.sysmgmt.log.repository.SysOperationLogRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * 审计链篡改检测（架构 §1219）：分页重算 hash 链 + 逐行恒验签（S5 Plan v0.2 B-1——
 * 无任何按签名值跳过的旁路；mock 域 MockSignService 恒 true 自然通过，
 * impl 域占位/篡改签名诚实报断点）。
 *
 * <p>EFF-S5-1 增量化：持久化 SM2 签名 checkpoint 锚（单行表）。INCREMENTAL 模式
 * 锚三查（签名 → 链尾截断比较 → 锚行在场且 hash 匹配）后仅校验锚后新增行；
 * intact 推进锚至本次校验末行。纯删尾截断由 {@link BreakType#TRUNCATION} 可检
 * （S5 抉择⑩ 升级）——前提是攻击者无法删除或回填旧 checkpoint：回退/删锚+删尾
 * 联合攻击使检测退回 S5 基线（非低于），由双 gauge 外锚兜底
 * （{@code fep_audit_chain_tail_seq} + {@code fep_audit_chain_checkpoint_seq}）。
 * INCREMENTAL 不检测已验段内事后篡改，FULL 仍为权威校验。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class AuditChainVerifier {

    private static final Logger LOG = LoggerFactory.getLogger(AuditChainVerifier.class);

    /** 分页批量（防全表载入 OOM）。 */
    private static final int PAGE_SIZE = 500;

    /** checkpoint 签名域分隔前缀（与行签名 64-hex 输入空间不相交，Plan 抉择④）。 */
    private static final String CHECKPOINT_SIGN_PREFIX = "audit-checkpoint:";

    private final SysOperationLogRepository repository;
    private final AuditIntegrityService auditIntegrityService;
    private final AuditChainCheckpointRepository checkpointRepository;
    private final AtomicLong checkpointSeqGauge = new AtomicLong();

    /**
     * 构造（注册 checkpoint 锚 gauge，crypto C-1 监控外锚）。
     *
     * @param repository            操作日志仓储
     * @param auditIntegrityService 完整性原语
     * @param checkpointRepository  checkpoint 锚仓储
     * @param meterRegistry         监控注册器
     */
    public AuditChainVerifier(final SysOperationLogRepository repository,
            final AuditIntegrityService auditIntegrityService,
            final AuditChainCheckpointRepository checkpointRepository,
            final MeterRegistry meterRegistry) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.auditIntegrityService = Objects.requireNonNull(auditIntegrityService,
                "auditIntegrityService");
        this.checkpointRepository = Objects.requireNonNull(checkpointRepository,
                "checkpointRepository");
        Gauge.builder("fep_audit_chain_checkpoint_seq", checkpointSeqGauge, AtomicLong::get)
                .description("audit chain checkpoint verified_until_seq (external anchor)")
                .register(Objects.requireNonNull(meterRegistry, "meterRegistry"));
    }

    /** 启动时以既有 checkpoint 锚初始化 gauge（重启后监控连续性）。 */
    @PostConstruct
    public void initCheckpointGauge() {
        checkpointRepository.findById(AuditChainCheckpoint.SINGLETON_ID)
                .ifPresent(cp -> checkpointSeqGauge.set(cp.getVerifiedUntilSeq()));
    }

    /**
     * 默认入口：INCREMENTAL 增量校验（EFF-S5-1 起默认；FULL 经
     * {@link #verifyChain(VerifyMode)} 显式选择）。
     *
     * @return 校验结果
     */
    public ChainVerifyResult verifyChain() {
        return verifyChain(VerifyMode.INCREMENTAL);
    }

    /**
     * 链校验入口。
     *
     * @param mode FULL=GENESIS 起全链权威校验；INCREMENTAL=checkpoint 锚后增量
     *             （缺锚退化 FULL 语义并 WARN 一条）
     * @return 校验结果（intact 且有新增行时已推进 checkpoint）
     */
    public ChainVerifyResult verifyChain(final VerifyMode mode) {
        long startSeq = 1;
        String expectedPrev = AuditIntegrityService.GENESIS_PREV_HASH;
        Long checkpointSeq = null;
        if (mode == VerifyMode.INCREMENTAL) {
            final Optional<AuditChainCheckpoint> cp =
                    checkpointRepository.findById(AuditChainCheckpoint.SINGLETON_ID);
            if (cp.isEmpty()) {
                LOG.warn("INCREMENTAL verify requested but checkpoint absent,"
                        + " degrading to full scan");
            } else {
                final AuditChainCheckpoint anchor = cp.get();
                checkpointSeq = anchor.getVerifiedUntilSeq();
                final String signedPayload = CHECKPOINT_SIGN_PREFIX
                        + anchor.getVerifiedUntilSeq() + ":" + anchor.getAnchorHash();
                boolean cpSigValid;
                try {
                    cpSigValid = auditIntegrityService.verifyEntry(
                            signedPayload, anchor.getCheckpointSignature(),
                            anchor.getSignKeyId());
                } catch (final IllegalArgumentException e) {
                    // unknown sign_key_id（含轮换退役 keyId 旧锚）→ 篡改证据归并；
                    // 下次 FULL intact 推进将以活跃密钥重签自愈（Plan 抉择④ C3）
                    cpSigValid = false;
                }
                if (!cpSigValid) {
                    return ChainVerifyResult.broken(0, anchor.getVerifiedUntilSeq(),
                            BreakType.CHECKPOINT_INVALID, mode, checkpointSeq);
                }
                final Optional<SysOperationLog> tail =
                        repository.findTopBySeqIsNotNullOrderBySeqDesc();
                if (tail.isEmpty() || tail.get().getSeq() < anchor.getVerifiedUntilSeq()) {
                    return ChainVerifyResult.broken(0, anchor.getVerifiedUntilSeq(),
                            BreakType.TRUNCATION, mode, checkpointSeq);
                }
                final Optional<SysOperationLog> anchorRow =
                        repository.findBySeq(anchor.getVerifiedUntilSeq());
                if (anchorRow.isEmpty()) {
                    return ChainVerifyResult.broken(0, anchor.getVerifiedUntilSeq(),
                            BreakType.TRUNCATION, mode, checkpointSeq);
                }
                if (!anchorRow.get().getHash().equals(anchor.getAnchorHash())) {
                    return ChainVerifyResult.broken(0, anchor.getVerifiedUntilSeq(),
                            BreakType.HASH_MISMATCH, mode, checkpointSeq);
                }
                startSeq = anchor.getVerifiedUntilSeq() + 1;
                expectedPrev = anchor.getAnchorHash();
            }
        }
        return scanChain(startSeq, expectedPrev, mode, checkpointSeq);
    }

    /**
     * 自 startSeq 起逐行链校验（seq 连续 → prev 链接 → hash 重算 → 行恒验签，
     * 首断点即停）；intact 且有新增行时推进 checkpoint。
     */
    private ChainVerifyResult scanChain(final long startSeq, final String startPrev,
            final VerifyMode mode, final Long checkpointSeq) {
        long checked = 0;
        long expectedSeq = startSeq;
        String expectedPrev = startPrev;
        long lastSeq = startSeq - 1;
        String lastHash = startPrev;
        int page = 0;
        while (true) {
            final Page<SysOperationLog> batch = repository
                    .findBySeqGreaterThanEqualOrderBySeqAsc(startSeq,
                            PageRequest.of(page, PAGE_SIZE));
            for (final SysOperationLog row : batch.getContent()) {
                final long seq = row.getSeq();
                if (seq != expectedSeq) {
                    return ChainVerifyResult.broken(checked, seq, BreakType.GAP,
                            mode, checkpointSeq);
                }
                if (!expectedPrev.equals(row.getPrevHash())) {
                    return ChainVerifyResult.broken(checked, seq, BreakType.PREV_LINK,
                            mode, checkpointSeq);
                }
                final byte[] canonical = AuditCanonicalizer.canonicalize(row, seq)
                        .getBytes(StandardCharsets.UTF_8);
                final String recomputed = auditIntegrityService
                        .computeEntryHash(expectedPrev, canonical);
                if (!recomputed.equals(row.getHash())) {
                    return ChainVerifyResult.broken(checked, seq, BreakType.HASH_MISMATCH,
                            mode, checkpointSeq);
                }
                final boolean signatureValid;
                try {
                    signatureValid = auditIntegrityService.verifyEntry(
                            row.getHash(), row.getSignature(), row.getSignKeyId());
                } catch (final IllegalArgumentException e) {
                    // unknown sign_key_id（含 dev→prod 晋升后的 mock 历史行）→ 诚实断点
                    return ChainVerifyResult.broken(checked, seq, BreakType.UNKNOWN_KEY,
                            mode, checkpointSeq);
                }
                if (!signatureValid) {
                    return ChainVerifyResult.broken(checked, seq,
                            BreakType.SIGNATURE_INVALID, mode, checkpointSeq);
                }
                checked++;
                expectedSeq = seq + 1;
                expectedPrev = row.getHash();
                lastSeq = seq;
                lastHash = row.getHash();
            }
            if (!batch.hasNext()) {
                if (checked == 0) {
                    // 空链 / 零新增行：无新锚可立，不重签不推进（Plan C5）
                    return ChainVerifyResult.intact(0, mode, checkpointSeq);
                }
                final Long advanced = advanceCheckpoint(lastSeq, lastHash);
                return ChainVerifyResult.intact(checked, mode,
                        advanced != null ? advanced : checkpointSeq);
            }
            page++;
        }
    }

    /**
     * 推进 checkpoint 锚至本次校验末行（域分隔串签名 + save 覆盖 + gauge 外锚）。
     * 推进失败（并发首建 PK 竞争）不影响校验结果，下轮自愈（Plan 抉择⑤ C2）。
     *
     * @return 推进后的锚 seq；推进失败 null
     */
    private Long advanceCheckpoint(final long lastSeq, final String lastHash) {
        final String payload = CHECKPOINT_SIGN_PREFIX + lastSeq + ":" + lastHash;
        final AuditChainCheckpoint cp = checkpointRepository
                .findById(AuditChainCheckpoint.SINGLETON_ID)
                .orElseGet(AuditChainCheckpoint::new);
        cp.setVerifiedUntilSeq(lastSeq);
        cp.setAnchorHash(lastHash);
        cp.setCheckpointSignature(auditIntegrityService.signEntryHash(payload));
        cp.setSignKeyId(auditIntegrityService.auditKeyId());
        cp.setVerifiedAt(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        try {
            checkpointRepository.save(cp);
        } catch (final DataIntegrityViolationException e) {
            LOG.warn("checkpoint advance lost a concurrent-insert race;"
                    + " verification result unaffected, next run self-heals", e);
            return null;
        }
        checkpointSeqGauge.set(lastSeq);
        return lastSeq;
    }

    /** 校验模式（EFF-S5-1）。 */
    public enum VerifyMode {
        /** 全链权威校验（O(n)，运维低频窗口使用）。 */
        FULL,
        /** checkpoint 增量校验（O(Δ)，默认；缺锚退化 FULL 语义）。 */
        INCREMENTAL
    }

    /** 断点类型。 */
    public enum BreakType {
        /** seq 不连续（删行/链首非 1）。 */
        GAP,
        /** prev_hash 与前行 hash 失配。 */
        PREV_LINK,
        /** 行 hash 重算失配（字段被改；含锚行 hash 与锚失配）。 */
        HASH_MISMATCH,
        /** SM2 行验签失败（签名被改/伪造）。 */
        SIGNATURE_INVALID,
        /** sign_key_id 不在配置密钥集（含 mock 历史行于 impl 域）。 */
        UNKNOWN_KEY,
        /**
         * 链尾 &lt; checkpoint 锚或锚行缺失（删尾/中段删除致锚不在场；EFF-S5-1）。
         * 备份恢复到旧位点同样触发——属正确的数据丢失证据而非误报。
         */
        TRUNCATION,
        /** checkpoint 签名验签失败或 keyId 未知（锚被篡改/伪造前移；EFF-S5-1）。 */
        CHECKPOINT_INVALID
    }
}
