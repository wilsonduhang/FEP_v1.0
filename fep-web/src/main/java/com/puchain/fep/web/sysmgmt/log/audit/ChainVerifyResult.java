package com.puchain.fep.web.sysmgmt.log.audit;

/**
 * 审计链校验结果（integrity 端点 JSON 载体）。
 *
 * @param totalChecked  已校验链上行数
 * @param intact        链完整
 * @param firstBreakSeq 首断点 seq（intact 时 null）
 * @param breakType     断点类型（intact 时 null）
 * @author FEP Team
 * @since 1.0.0
 */
public record ChainVerifyResult(long totalChecked, boolean intact,
        Long firstBreakSeq, AuditChainVerifier.BreakType breakType) {

    /**
     * 完整链结果。
     *
     * @param totalChecked 已校验行数
     * @return intact 结果
     */
    public static ChainVerifyResult intact(final long totalChecked) {
        return new ChainVerifyResult(totalChecked, true, null, null);
    }

    /**
     * 断链结果。
     *
     * @param totalChecked  断点前已校验行数
     * @param firstBreakSeq 首断点 seq
     * @param breakType     断点类型
     * @return broken 结果
     */
    public static ChainVerifyResult broken(final long totalChecked,
            final long firstBreakSeq, final AuditChainVerifier.BreakType breakType) {
        return new ChainVerifyResult(totalChecked, false, firstBreakSeq, breakType);
    }
}
