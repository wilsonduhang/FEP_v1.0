package com.puchain.fep.web.callback.credential.migration;

import com.puchain.fep.web.callback.credential.crypto.CallbackCredentialEncryptionFacade;
import com.puchain.fep.web.callback.credential.crypto.CallbackCredentialEncryptionFacade.EncryptedCredential;
import com.puchain.fep.web.callback.credential.domain.CallbackCredentialEntity;
import com.puchain.fep.web.callback.credential.repository.CallbackCredentialRepository;
import com.puchain.fep.web.callback.metrics.CallbackMetrics;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CallbackLegacyCredentialMigrator 判别 + 幂等 + 重加密回写单元测试。
 */
class CallbackLegacyCredentialMigratorTest {

    private final CallbackCredentialRepository repo = mock(CallbackCredentialRepository.class);
    private final CallbackCredentialEncryptionFacade facade =
            mock(CallbackCredentialEncryptionFacade.class);
    private final CallbackMetrics metrics = mock(CallbackMetrics.class);

    private CallbackLegacyCredentialMigrator newMigrator() {
        final CallbackLegacyCredentialKeyIdProperties props =
                new CallbackLegacyCredentialKeyIdProperties();
        props.setLegacyPlaintextKeyIds(List.of("mock-key-v1"));
        // provider="mock"：单测不经 Spring，@PostConstruct 不自动触发；C1 校验 IT(provider=impl) 覆盖
        return new CallbackLegacyCredentialMigrator(repo, facade, metrics, props, "mock");
    }

    @Test
    void isLegacy_matchesConfiguredKeyId() {
        final CallbackLegacyCredentialMigrator migrator = newMigrator();
        assertThat(migrator.isLegacy("mock-key-v1")).isTrue();
        assertThat(migrator.isLegacy("sm4-cred-v1")).isFalse();
    }

    @Test
    void migrate_legacyTokenRow_reencryptsAndRotates() {
        final byte[] plainBytes = "secret-token".getBytes(StandardCharsets.UTF_8);
        final CallbackCredentialEntity entity = CallbackCredentialEntity.newToken(
                "iface-1", plainBytes, "Authorization", "mock-key-v1", null);
        when(repo.findByInterfaceId("iface-1")).thenReturn(Optional.of(entity));
        when(facade.encrypt("secret-token"))
                .thenReturn(new EncryptedCredential(new byte[]{9, 9, 9}, "sm4-cred-v1"));

        newMigrator().migrateToActiveKey("iface-1");

        assertThat(entity.getKeyId()).isEqualTo("sm4-cred-v1");
        verify(repo).save(entity);
        verify(metrics).recordCredentialMigrated();
    }

    @Test
    void migrate_legacyOauthRow_reencryptsBothColumnsAndRotates() {
        final byte[] clientIdBytes = "client-id-val".getBytes(StandardCharsets.UTF_8);
        final byte[] clientSecretBytes = "client-secret-val".getBytes(StandardCharsets.UTF_8);
        final CallbackCredentialEntity entity = CallbackCredentialEntity.newOauth(
                "iface-oauth", clientIdBytes, clientSecretBytes,
                "https://token.test/oauth", "read", "mock-key-v1", null);
        when(repo.findByInterfaceId("iface-oauth")).thenReturn(Optional.of(entity));
        when(facade.encrypt("client-id-val"))
                .thenReturn(new EncryptedCredential(new byte[]{1, 1, 1}, "sm4-cred-v1"));
        when(facade.encrypt("client-secret-val"))
                .thenReturn(new EncryptedCredential(new byte[]{2, 2, 2}, "sm4-cred-v1"));

        newMigrator().migrateToActiveKey("iface-oauth");

        assertThat(entity.getKeyId()).isEqualTo("sm4-cred-v1");
        assertThat(entity.getOauthClientIdCiphertext()).isEqualTo(new byte[]{1, 1, 1});
        assertThat(entity.getOauthClientSecretCiphertext()).isEqualTo(new byte[]{2, 2, 2});
        verify(repo).save(entity);
        verify(metrics).recordCredentialMigrated();
    }

    @Test
    void migrate_alreadyMigratedRow_isIdempotentNoop() {
        final CallbackCredentialEntity entity = CallbackCredentialEntity.newToken(
                "iface-2", new byte[]{1, 2, 3}, "Authorization", "sm4-cred-v1", null);
        when(repo.findByInterfaceId("iface-2")).thenReturn(Optional.of(entity));

        newMigrator().migrateToActiveKey("iface-2");

        verify(repo, never()).save(any());
        verify(metrics, never()).recordCredentialMigrated();
    }
}
