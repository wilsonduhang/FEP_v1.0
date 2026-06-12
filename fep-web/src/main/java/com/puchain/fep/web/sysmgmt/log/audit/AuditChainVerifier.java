package com.puchain.fep.web.sysmgmt.log.audit;

import com.puchain.fep.security.api.AuditIntegrityService;
import com.puchain.fep.web.sysmgmt.log.domain.SysOperationLog;
import com.puchain.fep.web.sysmgmt.log.repository.SysOperationLogRepository;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * 审计链篡改检测（架构 §1219）：分页重算 hash 链 + 逐行恒验签（Plan v0.2 B-1——
 * 无任何按签名值跳过的旁路；mock 域 MockSignService 恒 true 自然通过，
 * impl 域占位/篡改签名诚实报断点）。纯删尾截断为结构性盲区（Plan 抉择⑩ 披露，
 * 缓解靠链尾 gauge 外锚）。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class AuditChainVerifier {

    /** 分页批量（防全表载入 OOM）。 */
    private static final int PAGE_SIZE = 500;

    private final SysOperationLogRepository repository;
    private final AuditIntegrityService auditIntegrityService;

    /**
     * 构造。
     *
     * @param repository            操作日志仓储
     * @param auditIntegrityService 完整性原语
     */
    public AuditChainVerifier(final SysOperationLogRepository repository,
            final AuditIntegrityService auditIntegrityService) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.auditIntegrityService = Objects.requireNonNull(auditIntegrityService,
                "auditIntegrityService");
    }

    /**
     * 全链校验：seq 连续性 → prev 链接 → hash 重算 → 行验签，首断点即停。
     *
     * @return 校验结果（intact / 首断点 seq 与类型）
     */
    public ChainVerifyResult verifyChain() {
        long checked = 0;
        long expectedSeq = 1;
        String expectedPrev = AuditIntegrityService.GENESIS_PREV_HASH;
        int page = 0;
        while (true) {
            final Page<SysOperationLog> batch = repository
                    .findBySeqIsNotNullOrderBySeqAsc(PageRequest.of(page, PAGE_SIZE));
            for (final SysOperationLog row : batch.getContent()) {
                final long seq = row.getSeq();
                if (seq != expectedSeq) {
                    return ChainVerifyResult.broken(checked, seq, BreakType.GAP);
                }
                if (!expectedPrev.equals(row.getPrevHash())) {
                    return ChainVerifyResult.broken(checked, seq, BreakType.PREV_LINK);
                }
                final byte[] canonical = AuditCanonicalizer.canonicalize(row, seq)
                        .getBytes(StandardCharsets.UTF_8);
                final String recomputed = auditIntegrityService
                        .computeEntryHash(expectedPrev, canonical);
                if (!recomputed.equals(row.getHash())) {
                    return ChainVerifyResult.broken(checked, seq, BreakType.HASH_MISMATCH);
                }
                final boolean signatureValid;
                try {
                    signatureValid = auditIntegrityService.verifyEntry(
                            row.getHash(), row.getSignature(), row.getSignKeyId());
                } catch (final IllegalArgumentException e) {
                    // unknown sign_key_id（含 dev→prod 晋升后的 mock 历史行）→ 诚实断点
                    return ChainVerifyResult.broken(checked, seq, BreakType.UNKNOWN_KEY);
                }
                if (!signatureValid) {
                    return ChainVerifyResult.broken(checked, seq, BreakType.SIGNATURE_INVALID);
                }
                checked++;
                expectedSeq = seq + 1;
                expectedPrev = row.getHash();
            }
            if (!batch.hasNext()) {
                return ChainVerifyResult.intact(checked);
            }
            page++;
        }
    }

    /** 断点类型。 */
    public enum BreakType {
        /** seq 不连续（删行/链首非 1）。 */
        GAP,
        /** prev_hash 与前行 hash 失配。 */
        PREV_LINK,
        /** 行 hash 重算失配（字段被改）。 */
        HASH_MISMATCH,
        /** SM2 行验签失败（签名被改/伪造）。 */
        SIGNATURE_INVALID,
        /** sign_key_id 不在配置密钥集（含 mock 历史行于 impl 域）。 */
        UNKNOWN_KEY
    }
}
