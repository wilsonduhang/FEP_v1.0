package com.puchain.fep.web.callback.credential.migration;

import com.puchain.fep.web.callback.credential.crypto.CallbackCredentialEncryptionFacade;
import com.puchain.fep.web.callback.credential.crypto.CallbackCredentialEncryptionFacade.EncryptedCredential;
import com.puchain.fep.web.callback.credential.domain.CallbackCredentialEntity;
import com.puchain.fep.web.callback.credential.repository.CallbackCredentialRepository;
import com.puchain.fep.web.callback.metrics.CallbackMetrics;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * callback_credential 明文→SM4 密文惰性双读迁移器。
 *
 * <p>mock 透传期凭证以明文 UTF-8 字节存于密文列、key_id={@code mock-key-v1}。本迁移器在
 * 读路径（{@code CallbackCredentialResolver}）检测到 legacy key_id 时被触发：在
 * {@link Propagation#REQUIRES_NEW} 独立短事务中把明文字节重加密为真实 SM4 密文并
 * {@link CallbackCredentialEntity#rotate} 翻 key_id 到活跃版本。单行只迁一次（幂等）。</p>
 *
 * <p>判别器为 key_id（确定性，非 try-decrypt 猜测）；legacy 集合配置见
 * {@link CallbackLegacyCredentialKeyIdProperties}。</p>
 *
 * <p><strong>C1 安全不变量:</strong> 当 {@code fep.security.provider=impl}（真实加密）时，
 * 活跃 keyId 不得 ∈ legacyKeyIds（否则新密文被打 legacy 标记→下次读当明文泄漏）。
 * {@link #assertActiveKeyNotLegacy} 启动校验 + {@link #migrateToActiveKey} 运行时二次守护。
 * mock 透传期 active==legacy（{@code mock-key-v1}）是预期且安全的（密文==明文），故校验仅 impl 生效。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class CallbackLegacyCredentialMigrator {

    private static final Logger LOG = LoggerFactory.getLogger(CallbackLegacyCredentialMigrator.class);

    private final CallbackCredentialRepository repo;
    private final CallbackCredentialEncryptionFacade facade;
    private final CallbackMetrics metrics;
    private final Set<String> legacyKeyIds;
    private final String provider;

    /**
     * 构造迁移器。
     *
     * @param repo     凭证仓储，非 null
     * @param facade   加解密 facade，非 null
     * @param metrics  回调指标门面，非 null
     * @param props    legacy keyId 配置，非 null
     * @param provider 当前安全 provider（mock/impl），决定 C1 校验是否生效
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring-managed singletons stored by reference per container contract")
    public CallbackLegacyCredentialMigrator(final CallbackCredentialRepository repo,
                                    final CallbackCredentialEncryptionFacade facade,
                                    final CallbackMetrics metrics,
                                    final CallbackLegacyCredentialKeyIdProperties props,
                                    @Value("${fep.security.provider:mock}") final String provider) {
        this.repo = Objects.requireNonNull(repo, "repo");
        this.facade = Objects.requireNonNull(facade, "facade");
        this.metrics = Objects.requireNonNull(metrics, "metrics");
        this.legacyKeyIds = new HashSet<>(props.getLegacyPlaintextKeyIds());
        this.provider = provider;
    }

    /**
     * C1 启动校验 + C2 可观测：impl 模式下活跃 keyId 不得为 legacy 标记；注册 legacy 剩余量
     * gauge + 启动 WARN 计数。
     *
     * @throws IllegalStateException impl 模式下活跃 keyId ∈ legacyKeyIds（误配致真密文泄漏）
     */
    @PostConstruct
    public void assertActiveKeyNotLegacy() {
        if ("impl".equals(provider) && legacyKeyIds.contains(facade.activeKeyId())) {
            throw new IllegalStateException("active keyId [" + facade.activeKeyId()
                    + "] must not be a legacy-plaintext marker when provider=impl");
        }
        metrics.registerLegacyCredentialGauge(() -> repo.countByKeyIdIn(legacyKeyIds));
        final long remaining = repo.countByKeyIdIn(legacyKeyIds);
        if (remaining > 0) {
            LOG.warn("callback_credential lazy migration pending: {} legacy-plaintext rows remaining "
                    + "(migrated on next read; cold interfaces stay plaintext until read)", remaining);
        }
    }

    /**
     * 判断 key_id 是否为 legacy 明文标记。
     *
     * @param keyId 凭证记录的 key_id
     * @return true 表示密文列实为明文字节
     */
    public boolean isLegacy(final String keyId) {
        return legacyKeyIds.contains(keyId);
    }

    /**
     * 将指定接口的 legacy 明文凭证重加密为真实 SM4 密文（独立事务 + 幂等）。
     *
     * @param interfaceId 接口标识
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void migrateToActiveKey(final String interfaceId) {
        final CallbackCredentialEntity entity = repo.findByInterfaceId(interfaceId).orElse(null);
        if (entity == null || !isLegacy(entity.getKeyId())) {
            return;
        }
        final EncryptedCredential token = reencrypt(entity.getTokenCiphertext());
        final EncryptedCredential clientId = reencrypt(entity.getOauthClientIdCiphertext());
        final EncryptedCredential clientSecret =
                reencrypt(entity.getOauthClientSecretCiphertext());
        final String activeKeyId = firstNonNullKeyId(token, clientId, clientSecret);
        if (legacyKeyIds.contains(activeKeyId)) {           // C1 运行时二次守护
            throw new IllegalStateException("re-encrypt produced legacy keyId [" + activeKeyId
                    + "]; refusing to write (misconfiguration)");
        }
        entity.rotate(
                token == null ? null : token.ciphertext(),
                clientId == null ? null : clientId.ciphertext(),
                clientSecret == null ? null : clientSecret.ciphertext(),
                activeKeyId);
        repo.save(entity);
        metrics.recordCredentialMigrated();
    }

    private EncryptedCredential reencrypt(final byte[] legacyPlaintextBytes) {
        if (legacyPlaintextBytes == null) {
            return null;
        }
        return facade.encrypt(new String(legacyPlaintextBytes, StandardCharsets.UTF_8));
    }

    private static String firstNonNullKeyId(final EncryptedCredential... candidates) {
        for (final EncryptedCredential c : candidates) {
            if (c != null) {
                return c.keyId();
            }
        }
        throw new IllegalStateException("legacy credential has no non-null ciphertext column");
    }
}
