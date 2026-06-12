package com.puchain.fep.web.callback.credential.dto;

/**
 * legacy 明文凭证批量迁移结果（密文/明文均不回显，仅计数）。
 *
 * <p>参见 PRD v1.3 §5.5.3 凭证配置（FR-INFRA-CALLBACK-CREDENTIAL）+ §8.3 敏感数据保护。</p>
 *
 * @param migrated  本次成功迁移行数
 * @param failed    本次迁移失败行数（详情见 WARN 日志，单行失败不阻断扫描）
 * @param remaining 迁移后仍滞留 legacy 明文的行数（监控 gauge 同源计数）
 * @author FEP Team
 * @since 1.0.0
 */
public record CallbackCredentialSweepResponse(int migrated, int failed, long remaining) {
}
