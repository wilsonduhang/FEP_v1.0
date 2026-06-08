package com.puchain.fep.web.callback.credential.service;

import com.puchain.fep.web.callback.credential.crypto.CallbackCredentialEncryptionFacade;
import com.puchain.fep.web.callback.credential.domain.CallbackCredentialEntity;
import com.puchain.fep.web.callback.credential.migration.CallbackLegacyCredentialMigrator;
import com.puchain.fep.web.callback.credential.oauth.CallbackOAuth2ClientCredentialsClient;
import com.puchain.fep.web.callback.credential.oauth.CallbackOAuth2TokenCache;
import com.puchain.fep.web.callback.credential.oauth.CallbackOAuth2TokenResponse;
import com.puchain.fep.web.callback.credential.repository.CallbackCredentialRepository;
import com.puchain.fep.web.callback.metrics.CallbackMetrics;
import com.puchain.fep.web.submission.outputinterface.domain.SubOutputInterface;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 回调鉴权头解析器 — 按 {@link com.puchain.fep.web.submission.outputinterface.domain.InterfaceAuthType}
 * 为目标输出接口解析实际鉴权 HTTP 头。
 *
 * <p>三分支语义：</p>
 * <ul>
 *   <li>{@code NONE} — 返回 {@link Optional#empty()}，不加鉴权头。</li>
 *   <li>{@code TOKEN} — 查库取 token 密文 → {@link CallbackCredentialEncryptionFacade#decrypt} 还原明文 →
 *       {@code <tokenHeader>: <plaintext>}（tokenHeader 默认 {@code Authorization}）。</li>
 *   <li>{@code OAUTH2} — 先查 {@link CallbackOAuth2TokenCache}，命中则 {@code Authorization: Bearer <cached>}；
 *       未命中则查库 → 解密 client_id/secret → {@link CallbackOAuth2ClientCredentialsClient#fetchToken}
 *       → 缓存（TTL = {@code expires_in - } {@value #OAUTH_SAFETY_MARGIN_SECONDS} 秒）→
 *       {@code Authorization: Bearer <token>}。</li>
 * </ul>
 *
 * <p>TOKEN / OAUTH2 类型接口在 {@code callback_credential} 表无记录时抛
 * {@link CallbackCredentialMissingException}，不静默降级为无鉴权。</p>
 *
 * <p>参见 PRD v1.3 §5.5.3 凭证配置（FR-INFRA-CALLBACK-CREDENTIAL）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class CallbackCredentialResolver {

    /** OAuth2 token 缓存 TTL 相对 {@code expires_in} 的提前失效秒数（避免边界过期使用）。 */
    private static final int OAUTH_SAFETY_MARGIN_SECONDS = 30;

    /** Bearer 鉴权头标准前缀。 */
    private static final String BEARER_PREFIX = "Bearer ";

    /** OAuth2 默认鉴权 header 名。 */
    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final CallbackCredentialRepository repo;
    private final CallbackCredentialEncryptionFacade facade;
    private final CallbackOAuth2TokenCache cache;
    private final CallbackOAuth2ClientCredentialsClient oauthClient;
    private final Clock clock;
    private final CallbackMetrics metrics;
    private final CallbackLegacyCredentialMigrator migrator;

    /**
     * 构造解析器，注入凭证仓储、加解密 facade、OAuth2 token 缓存与 client、时钟、指标门面与迁移器。
     *
     * @param repo        凭证仓储，不可为 null
     * @param facade      凭证加解密 facade，不可为 null
     * @param cache       OAuth2 token 缓存，不可为 null
     * @param oauthClient OAuth2 client credentials 客户端，不可为 null
     * @param clock       时钟（过期判定，可测试），不可为 null
     * @param metrics     回调指标门面（过期计数），不可为 null
     * @param migrator    legacy 明文凭证惰性双读迁移器（封装 legacy keyId 判别），不可为 null
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring-managed singletons stored by reference per container contract")
    public CallbackCredentialResolver(final CallbackCredentialRepository repo,
                                      final CallbackCredentialEncryptionFacade facade,
                                      final CallbackOAuth2TokenCache cache,
                                      final CallbackOAuth2ClientCredentialsClient oauthClient,
                                      final Clock clock,
                                      final CallbackMetrics metrics,
                                      final CallbackLegacyCredentialMigrator migrator) {
        this.repo = repo;
        this.facade = facade;
        this.cache = cache;
        this.oauthClient = oauthClient;
        this.clock = clock;
        this.metrics = metrics;
        this.migrator = migrator;
    }

    /**
     * 为目标输出接口解析鉴权 HTTP 头。
     *
     * @param target 目标输出接口配置，非空（含 authType 与 interfaceId）
     * @return 鉴权头（NONE 类型返回 {@link Optional#empty()}）
     * @throws CallbackCredentialMissingException 当 TOKEN / OAUTH2 类型接口无凭证记录
     */
    public Optional<AuthHeader> resolveAuthHeader(final SubOutputInterface target) {
        return switch (target.getAuthType()) {
            case NONE -> Optional.empty();
            case TOKEN -> Optional.of(resolveToken(target.getInterfaceId()));
            case OAUTH2 -> Optional.of(resolveOAuth2(target.getInterfaceId()));
        };
    }

    /**
     * 失效指定接口的 OAuth2 token 缓存（如对端返回 401 后调用方触发重取）。
     *
     * @param interfaceId 接口标识
     */
    public void invalidateOAuthToken(final String interfaceId) {
        cache.invalidate(interfaceId);
    }

    private AuthHeader resolveToken(final String interfaceId) {
        final CallbackCredentialEntity entity = repo.findByInterfaceId(interfaceId)
                .orElseThrow(() -> new CallbackCredentialMissingException(
                        "TOKEN credential missing for interfaceId=" + interfaceId));
        ensureNotExpired(entity);
        if (migrator.isLegacy(entity.getKeyId())) {
            final String plain = new String(
                    entity.getTokenCiphertext(), StandardCharsets.UTF_8);
            migrator.migrateToActiveKey(interfaceId);
            return new AuthHeader(entity.getTokenHeader(), plain);
        }
        final String plain = facade.decrypt(entity.getTokenCiphertext(), entity.getKeyId());
        return new AuthHeader(entity.getTokenHeader(), plain);
    }

    /**
     * 解析期过期门禁：凭证 {@code expires_at} 非 null 且早于当前时刻则计数并拒用，
     * 不静默降级。{@code null} 有效期（永不过期）放行。
     *
     * @param entity 凭证实体
     * @throws CallbackCredentialExpiredException 凭证已过期
     */
    private void ensureNotExpired(final CallbackCredentialEntity entity) {
        final LocalDateTime exp = entity.getExpiresAt();
        if (exp != null && LocalDateTime.now(clock).isAfter(exp)) {
            metrics.recordCredentialExpired();
            throw new CallbackCredentialExpiredException(
                    "credential expired for interfaceId=" + entity.getInterfaceId());
        }
    }

    private AuthHeader resolveOAuth2(final String interfaceId) {
        final Optional<String> cached = cache.get(interfaceId);
        if (cached.isPresent()) {
            return new AuthHeader(AUTHORIZATION_HEADER, BEARER_PREFIX + cached.get());
        }
        final CallbackCredentialEntity entity = repo.findByInterfaceId(interfaceId)
                .orElseThrow(() -> new CallbackCredentialMissingException(
                        "OAUTH2 credential missing for interfaceId=" + interfaceId));
        ensureNotExpired(entity);
        final String clientId;
        final String clientSecret;
        if (migrator.isLegacy(entity.getKeyId())) {
            clientId = new String(entity.getOauthClientIdCiphertext(), StandardCharsets.UTF_8);
            clientSecret = new String(entity.getOauthClientSecretCiphertext(), StandardCharsets.UTF_8);
            migrator.migrateToActiveKey(interfaceId);
        } else {
            clientId = facade.decrypt(entity.getOauthClientIdCiphertext(), entity.getKeyId());
            clientSecret = facade.decrypt(entity.getOauthClientSecretCiphertext(), entity.getKeyId());
        }
        final CallbackOAuth2TokenResponse resp = oauthClient.fetchToken(
                entity.getOauthTokenEndpoint(), clientId, clientSecret, entity.getOauthScope());
        cache.put(interfaceId, resp.accessToken(),
                Duration.ofSeconds((long) resp.expiresIn() - OAUTH_SAFETY_MARGIN_SECONDS));
        return new AuthHeader(AUTHORIZATION_HEADER, BEARER_PREFIX + resp.accessToken());
    }

    /**
     * 鉴权 HTTP 头键值对。
     *
     * @param name  HTTP header 名（如 {@code Authorization} / 自定义 token header）
     * @param value HTTP header 值（已解密的 token 或 {@code Bearer <token>}）
     */
    public record AuthHeader(String name, String value) {
    }
}
