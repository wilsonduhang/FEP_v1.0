package com.puchain.fep.web.callback.credential.service;

import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.web.callback.credential.crypto.CallbackCredentialEncryptionFacade;
import com.puchain.fep.web.callback.credential.crypto.CallbackCredentialEncryptionFacade.EncryptedCredential;
import com.puchain.fep.web.callback.credential.domain.CallbackCredentialEntity;
import com.puchain.fep.web.callback.credential.dto.CallbackCredentialCreateRequest;
import com.puchain.fep.web.callback.credential.dto.CallbackCredentialResponse;
import com.puchain.fep.web.callback.credential.dto.CallbackCredentialUpdateRequest;
import com.puchain.fep.web.callback.credential.oauth.CallbackOAuth2TokenCache;
import com.puchain.fep.web.callback.credential.repository.CallbackCredentialRepository;
import com.puchain.fep.web.submission.outputinterface.domain.InterfaceAuthType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CallbackCredentialAdminService} 单元测试（Mockito）。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class CallbackCredentialAdminServiceTest {

    @Mock
    private CallbackCredentialRepository repo;

    @Mock
    private CallbackCredentialEncryptionFacade facade;

    @Mock
    private CallbackOAuth2TokenCache cache;

    @InjectMocks
    private CallbackCredentialAdminService svc;

    @Test
    void createTokenEncryptsAndPersists() {
        final CallbackCredentialCreateRequest req = new CallbackCredentialCreateRequest();
        req.setInterfaceId("IF-001");
        req.setAuthType(InterfaceAuthType.TOKEN);
        req.setToken("plain-tok");
        when(repo.findByInterfaceId("IF-001")).thenReturn(Optional.empty());
        when(facade.encrypt("plain-tok"))
                .thenReturn(new EncryptedCredential(new byte[]{9}, "KEY-V1"));

        final CallbackCredentialResponse resp = svc.create(req);

        final ArgumentCaptor<CallbackCredentialEntity> cap =
                ArgumentCaptor.forClass(CallbackCredentialEntity.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getTokenCiphertext()).containsExactly(9);
        assertThat(cap.getValue().getKeyId()).isEqualTo("KEY-V1");
        assertThat(resp.isTokenConfigured()).isTrue();
        assertThat(resp.isOauthClientIdConfigured()).isFalse();
    }

    @Test
    void createOauth2EncryptsBothIdAndSecret() {
        final CallbackCredentialCreateRequest req = new CallbackCredentialCreateRequest();
        req.setInterfaceId("IF-002");
        req.setAuthType(InterfaceAuthType.OAUTH2);
        req.setOauthClientId("clid");
        req.setOauthClientSecret("csec");
        req.setOauthTokenEndpoint("https://idp/token");
        when(repo.findByInterfaceId("IF-002")).thenReturn(Optional.empty());
        when(facade.encrypt("clid")).thenReturn(new EncryptedCredential(new byte[]{1}, "KEY-V1"));
        when(facade.encrypt("csec")).thenReturn(new EncryptedCredential(new byte[]{2}, "KEY-V1"));

        final CallbackCredentialResponse resp = svc.create(req);

        final ArgumentCaptor<CallbackCredentialEntity> cap =
                ArgumentCaptor.forClass(CallbackCredentialEntity.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getOauthClientIdCiphertext()).containsExactly(1);
        assertThat(cap.getValue().getOauthClientSecretCiphertext()).containsExactly(2);
        assertThat(resp.isOauthClientIdConfigured()).isTrue();
        assertThat(resp.isOauthClientSecretConfigured()).isTrue();
        assertThat(resp.isTokenConfigured()).isFalse();
    }

    @Test
    void create_pastExpiresAt_throwsBiz5003() {
        final CallbackCredentialCreateRequest req = new CallbackCredentialCreateRequest();
        req.setInterfaceId("IF-001");
        req.setAuthType(InterfaceAuthType.TOKEN);
        req.setToken("plain-token");
        req.setExpiresAt(LocalDateTime.now().minusDays(1));
        when(repo.findByInterfaceId("IF-001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> svc.create(req))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("expiresAt");
    }

    @Test
    void create_futureExpiresAt_persistsValue() {
        final LocalDateTime future = LocalDateTime.now().plusDays(1);
        final CallbackCredentialCreateRequest req = new CallbackCredentialCreateRequest();
        req.setInterfaceId("IF-001");
        req.setAuthType(InterfaceAuthType.TOKEN);
        req.setToken("plain-token");
        req.setExpiresAt(future);
        when(repo.findByInterfaceId("IF-001")).thenReturn(Optional.empty());
        when(facade.encrypt("plain-token"))
                .thenReturn(new EncryptedCredential(new byte[]{1}, "KEY-V1"));

        final CallbackCredentialResponse resp = svc.create(req);

        assertThat(resp.getExpiresAt()).isEqualTo(future);
    }

    @Test
    void createDuplicateInterfaceThrows() {
        final CallbackCredentialCreateRequest req = new CallbackCredentialCreateRequest();
        req.setInterfaceId("IF-001");
        req.setAuthType(InterfaceAuthType.TOKEN);
        req.setToken("x");
        when(repo.findByInterfaceId("IF-001")).thenReturn(Optional.of(
                CallbackCredentialEntity.newToken("IF-001", new byte[]{7}, null, "KEY-V1", null)));

        assertThatThrownBy(() -> svc.create(req))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("already exists");
        verify(repo, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updatePartialKeepsExistingFieldsAndInvalidatesCache() {
        final CallbackCredentialEntity existing = CallbackCredentialEntity.newOauth("IF-001",
                new byte[]{1}, new byte[]{2}, "https://idp/token", "read", "KEY-V1", null);
        when(repo.findByInterfaceId("IF-001")).thenReturn(Optional.of(existing));
        final CallbackCredentialUpdateRequest req = new CallbackCredentialUpdateRequest();
        req.setOauthClientSecret("new-csec");
        when(facade.encrypt("new-csec"))
                .thenReturn(new EncryptedCredential(new byte[]{99}, "KEY-V1"));

        svc.update("IF-001", req);

        assertThat(existing.getOauthClientIdCiphertext()).containsExactly(1);
        assertThat(existing.getOauthClientSecretCiphertext()).containsExactly(99);
        assertThat(existing.getRotatedAt()).isNotNull();
        verify(cache).invalidate("IF-001");
    }

    @Test
    void updateNonSecretMetadataOnly() {
        final CallbackCredentialEntity existing = CallbackCredentialEntity.newOauth("IF-001",
                new byte[]{1}, new byte[]{2}, "https://idp/token", "read", "KEY-V1", null);
        when(repo.findByInterfaceId("IF-001")).thenReturn(Optional.of(existing));
        final CallbackCredentialUpdateRequest req = new CallbackCredentialUpdateRequest();
        req.setOauthScope("read write");

        svc.update("IF-001", req);

        assertThat(existing.getOauthScope()).isEqualTo("read write");
        // no ciphertext rotation -> rotatedAt remains null
        assertThat(existing.getRotatedAt()).isNull();
        assertThat(existing.getOauthClientSecretCiphertext()).containsExactly(2);
        verify(cache).invalidate("IF-001");
    }

    @Test
    void updateMissingThrows() {
        when(repo.findByInterfaceId("IF-404")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> svc.update("IF-404", new CallbackCredentialUpdateRequest()))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void getByInterfaceIdReturnsResponseWithoutCiphertext() {
        final CallbackCredentialEntity entity = CallbackCredentialEntity.newToken("IF-001",
                new byte[]{1, 2, 3}, "Authorization", "KEY-V1", null);
        when(repo.findByInterfaceId("IF-001")).thenReturn(Optional.of(entity));

        final CallbackCredentialResponse resp = svc.get("IF-001");

        assertThat(resp.getCredentialId()).isEqualTo(entity.getCredentialId());
        assertThat(resp.getTokenHeader()).isEqualTo("Authorization");
        assertThat(resp.isTokenConfigured()).isTrue();
    }

    @Test
    void getMissingThrows() {
        when(repo.findByInterfaceId("IF-404")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> svc.get("IF-404"))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void rotateKey_tokenCredential_reEncryptsWithNewActiveKey() {
        final CallbackCredentialEntity entity = CallbackCredentialEntity.newToken(
                "IF-001", new byte[]{7}, "Authorization", "k-old", null);
        when(repo.findByInterfaceId("IF-001")).thenReturn(Optional.of(entity));
        when(facade.decrypt(any(), eq("k-old"))).thenReturn("plain-token");
        when(facade.encrypt("plain-token"))
                .thenReturn(new EncryptedCredential(new byte[]{8}, "k-new"));

        final CallbackCredentialResponse resp = svc.rotateKey("IF-001");

        assertThat(entity.getKeyId()).isEqualTo("k-new");
        assertThat(entity.getRotatedAt()).isNotNull();
        assertThat(entity.getTokenCiphertext()).containsExactly(8);
        assertThat(resp.getInterfaceId()).isEqualTo("IF-001");
        verify(cache).invalidate("IF-001");
    }

    @Test
    void rotateKey_oauth2Credential_reEncryptsBothCiphers() {
        final CallbackCredentialEntity entity = CallbackCredentialEntity.newOauth(
                "IF-001", new byte[]{1}, new byte[]{2}, "https://idp/token", "read", "k-old", null);
        when(repo.findByInterfaceId("IF-001")).thenReturn(Optional.of(entity));
        when(facade.decrypt(new byte[]{1}, "k-old")).thenReturn("clid");
        when(facade.decrypt(new byte[]{2}, "k-old")).thenReturn("csec");
        when(facade.encrypt("clid")).thenReturn(new EncryptedCredential(new byte[]{11}, "k-new"));
        when(facade.encrypt("csec")).thenReturn(new EncryptedCredential(new byte[]{22}, "k-new"));

        svc.rotateKey("IF-001");

        assertThat(entity.getKeyId()).isEqualTo("k-new");
        assertThat(entity.getRotatedAt()).isNotNull();
        assertThat(entity.getOauthClientIdCiphertext()).containsExactly(11);
        assertThat(entity.getOauthClientSecretCiphertext()).containsExactly(22);
        verify(cache).invalidate("IF-001");
    }

    @Test
    void rotateKey_missing_throwsBiz5001() {
        when(repo.findByInterfaceId("IF-404")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> svc.rotateKey("IF-404"))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void deleteRemovesAndInvalidatesCache() {
        when(repo.findByInterfaceId("IF-001")).thenReturn(Optional.of(
                CallbackCredentialEntity.newToken("IF-001", new byte[]{1}, null, "KEY-V1", null)));

        svc.delete("IF-001");

        verify(repo).deleteByInterfaceId("IF-001");
        verify(cache).invalidate("IF-001");
    }

    @Test
    void deleteMissingIsIdempotent() {
        when(repo.findByInterfaceId("IF-404")).thenReturn(Optional.empty());

        svc.delete("IF-404");

        verify(repo, never()).deleteByInterfaceId(org.mockito.ArgumentMatchers.anyString());
        verify(cache, never()).invalidate(org.mockito.ArgumentMatchers.anyString());
    }
}
