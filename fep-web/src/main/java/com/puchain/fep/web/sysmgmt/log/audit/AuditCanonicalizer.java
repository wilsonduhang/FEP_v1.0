package com.puchain.fep.web.sysmgmt.log.audit;

import com.puchain.fep.web.sysmgmt.log.domain.SysOperationLog;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * 审计行规范化（netstring 风格，确定性 + DB 往返稳定）。
 *
 * <p>字段序固定 15 项：seq, logId, userId, userAccount, module, operation, description,
 * method, requestUrl, requestParams, responseStatus, ipAddress, durationMs,
 * createTime(截断秒 ISO_LOCAL_DATE_TIME——t_sys_operation_log TIMESTAMP 无小数位，
 * 截断保证写入与校验重算两侧一致), traceId。每字段编码为 {@code <utf8字节长>:<值>}，
 * null 编码为 {@code -1:}（与空串 {@code 0:} 可区分）。写入（AuditChainWriter）与
 * 校验（AuditChainVerifier）共用本类，保证两侧一致。</p>
 *
 * <p><strong>不变量（Plan v0.2 crypto N-2）：</strong>入链字段必须在 save 前完成一切
 * 截断/规范化（requestParams 已由切面截 2000；若未来引入超列宽来源须先截断再 append，
 * 否则 DB 静默截断致校验假阳 HASH_MISMATCH）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public final class AuditCanonicalizer {

    private AuditCanonicalizer() {
    }

    /**
     * 生成规范化串。
     *
     * @param e   日志实体（业务字段已填）
     * @param seq 链序号
     * @return 确定性规范化串
     */
    public static String canonicalize(final SysOperationLog e, final long seq) {
        final StringBuilder sb = new StringBuilder(256);
        field(sb, Long.toString(seq));
        field(sb, e.getLogId());
        field(sb, e.getUserId());
        field(sb, e.getUserAccount());
        field(sb, e.getModule());
        field(sb, e.getOperation() == null ? null : e.getOperation().name());
        field(sb, e.getDescription());
        field(sb, e.getMethod());
        field(sb, e.getRequestUrl());
        field(sb, e.getRequestParams());
        field(sb, e.getResponseStatus() == null ? null : e.getResponseStatus().toString());
        field(sb, e.getIpAddress());
        field(sb, e.getDurationMs() == null ? null : e.getDurationMs().toString());
        field(sb, e.getCreateTime() == null ? null
                : DateTimeFormatter.ISO_LOCAL_DATE_TIME
                        .format(e.getCreateTime().truncatedTo(ChronoUnit.SECONDS)));
        field(sb, e.getTraceId());
        return sb.toString();
    }

    private static void field(final StringBuilder sb, final String value) {
        if (value == null) {
            sb.append("-1:");
            return;
        }
        sb.append(value.getBytes(StandardCharsets.UTF_8).length).append(':').append(value);
    }
}
