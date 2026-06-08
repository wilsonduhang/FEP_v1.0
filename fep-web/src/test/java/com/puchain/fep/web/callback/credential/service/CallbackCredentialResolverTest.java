package com.puchain.fep.web.callback.credential.service;

import com.puchain.fep.web.callback.credential.crypto.CallbackCredentialEncryptionFacade;
import com.puchain.fep.web.callback.credential.domain.CallbackCredentialEntity;
import com.puchain.fep.web.callback.credential.migration.CallbackLegacyCredentialMigrator;
import com.puchain.fep.web.callback.credential.oauth.CallbackOAuth2ClientCredentialsClient;
import com.puchain.fep.web.callback.credential.oauth.CallbackOAuth2TokenCache;
import com.puchain.fep.web.callback.credential.oauth.CallbackOAuth2TokenResponse;
import com.puchain.fep.web.callback.credential.repository.CallbackCredentialRepository;
import com.puchain.fep.web.callback.credential.service.CallbackCredentialResolver.AuthHeader;
import com.puchain.fep.web.callback.metrics.CallbackMetrics;
import com.puchain.fep.web.submission.outputinterface.domain.InterfaceAuthType;
import com.puchain.fep.web.submission.outputinterface.domain.SubOutputInterface;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link CallbackCredentialResolver} 单元测试 — Mockito mock 4 依赖，
 * 覆盖 NONE / TOKEN / OAUTH2 cache-hit / OAUTH2 cache-miss / TOKEN 缺凭证 五分支。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class CallbackCredentialResolverTest {

    @Mock
    private CallbackCredentialRepository repo;
    @Mock
    private CallbackCredentialEncryptionFacade facade;
    @Mock
    private CallbackOAuth2TokenCache cache;
    @Mock
    private CallbackOAuth2ClientCredentialsClient oauthClient;
    @Mock
    private CallbackLegacyCredentialMigrator migrator;

    private CallbackCredentialResolver resolver;

    @BeforeEach
    void setUp() {
        // migrator.isLegacy(...) 默认 false（Mockito boolean 默认）→ 走真实解密原路径，行为保持
        resolver = new CallbackCredentialResolver(repo, facade, cache, oauthClient,
                Clock.systemUTC(), new CallbackMetrics(new SimpleMeterRegistry()), migrator);
    }

    /** 固定时钟 now=2030-06-01（过期门禁确定性测试）。 */
    private CallbackCredentialResolver resolverAt(final LocalDateTime now) {
        final Clock fixed = Clock.fixed(now.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        return new CallbackCredentialResolver(repo, facade, cache, oauthClient, fixed,
                new CallbackMetrics(new SimpleMeterRegistry()), migrator);
    }

    @Test
    void resolveNoneReturnsEmptyHeader() {
        final SubOutputInterface target = mockTarget("IF-001", InterfaceAuthType.NONE);

        final Optional<AuthHeader> h = resolver.resolveAuthHeader(target);

        assertThat(h).isEmpty();
        verifyNoInteractions(repo, facade, cache, oauthClient);
    }

    @Test
    void resolveTokenReturnsDecryptedToken() {
        final SubOutputInterface target = mockTarget("IF-001", InterfaceAuthType.TOKEN);
        final CallbackCredentialEntity entity = CallbackCredentialEntity.newToken(
                "IF-001", new byte[]{1}, "X-Auth", "KEY-V1", null);
        when(repo.findByInterfaceId("IF-001")).thenReturn(Optional.of(entity));
        when(facade.decrypt(new byte[]{1}, "KEY-V1")).thenReturn("tok-plain");

        final Optional<AuthHeader> h = resolver.resolveAuthHeader(target);

        assertThat(h).isPresent();
        assertThat(h.get().name()).isEqualTo("X-Auth");
        assertThat(h.get().value()).isEqualTo("tok-plain");
        verifyNoInteractions(oauthClient);
    }

    @Test
    void resolveOAuth2CacheHitSkipsFetch() {
        final SubOutputInterface target = mockTarget("IF-001", InterfaceAuthType.OAUTH2);
        when(cache.get("IF-001")).thenReturn(Optional.of("cached-tok"));

        final Optional<AuthHeader> h = resolver.resolveAuthHeader(target);

        assertThat(h).isPresent();
        assertThat(h.get().name()).isEqualTo("Authorization");
        assertThat(h.get().value()).isEqualTo("Bearer cached-tok");
        verifyNoInteractions(oauthClient, facade, repo);
    }

    @Test
    void resolveOAuth2CacheMissFetchesDecryptsAndCaches() {
        final SubOutputInterface target = mockTarget("IF-001", InterfaceAuthType.OAUTH2);
        when(cache.get("IF-001")).thenReturn(Optional.empty());
        final CallbackCredentialEntity cred = CallbackCredentialEntity.newOauth(
                "IF-001", new byte[]{1}, new byte[]{2}, "https://idp/token", "read", "KEY-V1", null);
        when(repo.findByInterfaceId("IF-001")).thenReturn(Optional.of(cred));
        when(facade.decrypt(new byte[]{1}, "KEY-V1")).thenReturn("clid");
        when(facade.decrypt(new byte[]{2}, "KEY-V1")).thenReturn("csec");
        when(oauthClient.fetchToken("https://idp/token", "clid", "csec", "read"))
                .thenReturn(new CallbackOAuth2TokenResponse("new-tok", 3600, "Bearer"));

        final Optional<AuthHeader> h = resolver.resolveAuthHeader(target);

        assertThat(h).isPresent();
        assertThat(h.get().name()).isEqualTo("Authorization");
        assertThat(h.get().value()).isEqualTo("Bearer new-tok");
        verify(cache).put(eq("IF-001"), eq("new-tok"),
                argThat(d -> d.toSeconds() == 3600 - 30));
    }

    @Test
    void resolveTokenMissingCredentialThrows() {
        final SubOutputInterface target = mockTarget("IF-001", InterfaceAuthType.TOKEN);
        when(repo.findByInterfaceId("IF-001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolveAuthHeader(target))
                .isInstanceOf(CallbackCredentialMissingException.class)
                .hasMessageContaining("IF-001");
    }

    @Test
    void resolveToken_expiredCredential_throwsExpired() {
        final CallbackCredentialResolver expResolver = resolverAt(LocalDateTime.of(2030, 6, 1, 0, 0));
        final CallbackCredentialEntity expired = CallbackCredentialEntity.newToken(
                "IF-001", new byte[]{1}, "Authorization", "k1",
                LocalDateTime.of(2030, 5, 1, 0, 0));
        when(repo.findByInterfaceId("IF-001")).thenReturn(Optional.of(expired));
        final SubOutputInterface target = mockTarget("IF-001", InterfaceAuthType.TOKEN);

        assertThatThrownBy(() -> expResolver.resolveAuthHeader(target))
                .isInstanceOf(CallbackCredentialExpiredException.class)
                .hasMessageContaining("IF-001");
        verifyNoInteractions(facade);
    }

    @Test
    void resolveToken_notExpired_resolvesNormally() {
        final CallbackCredentialResolver okResolver = resolverAt(LocalDateTime.of(2030, 6, 1, 0, 0));
        final CallbackCredentialEntity ok = CallbackCredentialEntity.newToken(
                "IF-001", new byte[]{1}, "X-Token", "k1",
                LocalDateTime.of(2030, 7, 1, 0, 0));
        when(repo.findByInterfaceId("IF-001")).thenReturn(Optional.of(ok));
        when(facade.decrypt(any(), eq("k1"))).thenReturn("plain");
        final SubOutputInterface target = mockTarget("IF-001", InterfaceAuthType.TOKEN);

        final Optional<AuthHeader> h = okResolver.resolveAuthHeader(target);

        assertThat(h).isPresent();
        assertThat(h.get().name()).isEqualTo("X-Token");
        assertThat(h.get().value()).isEqualTo("plain");
    }

    private SubOutputInterface mockTarget(final String id, final InterfaceAuthType at) {
        final SubOutputInterface t = new SubOutputInterface();
        t.setInterfaceId(id);
        t.setAuthType(at);
        return t;
    }
}
