package com.puchain.fep.web.callback.credential.migration;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 惰性双读迁移的 legacy 明文 keyId 集合配置（前缀 {@code fep.callback.credential.migration}）。
 *
 * <p>本集合标识哪些 key_id 的凭证密文列实为明文 UTF-8 字节（SM4 加密引入前写入的存量数据），
 * 解析期惰性双读迁移为真实 SM4 密文。</p>
 *
 * <p><strong>默认空 {@code []}</strong>：mock/dev/test 环境凭证（keyId={@code mock-key-v1}）为
 * mock 透传当前格式，非「待迁移 legacy」——默认空使其走 facade 直解密，避免 mock active key
 * （恒 {@code mock-key-v1}）∈ legacy 集合致 {@link CallbackLegacyCredentialMigrator#migrateToActiveKey}
 * 因 active∈legacy 抛 {@code refusing to write}（2026-06-23 follow-up 根治；mock active==legacy 陷阱）。
 * impl/prod 若有存量明文凭证待迁移，经 {@code application-prod.yml} 显式配真实旧 keyId。
 * {@code active-key-id} 禁与本集合交集（否则真密文被误判明文，C1 启动守护）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "fep.callback.credential.migration")
public class CallbackLegacyCredentialKeyIdProperties {

    /** 被视为 legacy 明文的 key_id 集合（默认空；impl/prod 经 prod.yml 显式配真实旧 keyId）。 */
    private List<String> legacyPlaintextKeyIds = new ArrayList<>();

    @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Spring @ConfigurationProperties JavaBean binding mutates the list via "
                    + "this getter (relaxed binding); returning a defensive copy would break "
                    + "property binding. CallbackLegacyCredentialMigrator copies the list into a Set "
                    + "at construction, so no live reference leaks beyond startup.")
    public List<String> getLegacyPlaintextKeyIds() {
        return legacyPlaintextKeyIds;
    }

    public void setLegacyPlaintextKeyIds(final List<String> legacyPlaintextKeyIds) {
        this.legacyPlaintextKeyIds = legacyPlaintextKeyIds == null
                ? new ArrayList<>() : new ArrayList<>(legacyPlaintextKeyIds);
    }
}
