package com.puchain.fep.web.callback.credential.migration;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 惰性双读迁移的 legacy 明文 keyId 集合配置（前缀 {@code fep.callback.credential.migration}）。
 *
 * <p>mock 透传期写入的凭证 key_id 恒为 {@code "mock-key-v1"}，其密文列实为明文 UTF-8 字节。
 * 本集合标识哪些 key_id 应被当作 legacy 明文处理（读时双读 + 重加密）。默认
 * {@code ["mock-key-v1"]}。{@code active-key-id} 禁与本集合交集（否则真密文被误判明文）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "fep.callback.credential.migration")
public class CallbackLegacyCredentialKeyIdProperties {

    /** 被视为 legacy 明文的 key_id 集合。 */
    private List<String> legacyPlaintextKeyIds = new ArrayList<>(List.of("mock-key-v1"));

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
