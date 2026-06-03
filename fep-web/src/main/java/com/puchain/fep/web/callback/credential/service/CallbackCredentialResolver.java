package com.puchain.fep.web.callback.credential.service;

import com.puchain.fep.web.callback.credential.crypto.CredentialEncryptionFacade;
import com.puchain.fep.web.callback.credential.domain.CallbackCredentialEntity;
import com.puchain.fep.web.callback.credential.oauth.OAuth2ClientCredentialsClient;
import com.puchain.fep.web.callback.credential.oauth.OAuth2TokenCache;
import com.puchain.fep.web.callback.credential.oauth.OAuth2TokenResponse;
import com.puchain.fep.web.callback.credential.repository.CallbackCredentialRepository;
import com.puchain.fep.web.submission.outputinterface.domain.SubOutputInterface;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * 回调鉴权头解析器 — 按 {@link com.puchain.fep.web.submission.outputinterface.domain.InterfaceAuthType}
 * 为目标输出接口解析实际鉴权 HTTP 头。
 *
 * <p>三分支语义：</p>
 * <ul>
 *   <li>{@code NONE} — 返回 {@link Optional#empty()}，不加鉴权头。</li>
 *   <li>{@code TOKEN} — 查库取 token 密文 → {@link CredentialEncryptionFacade#decrypt} 还原明文 →
 *       {@code <tokenHeader>: <plaintext>}（tokenHeader 默认 {@code Authorization}）。</li>
 *   <li>{@code OAUTH2} — 先查 {@link OAuth2TokenCache}，命中则 {@code Authorization: Bearer <cached>}；
 *       未命中则查库 → 解密 client_id/secret → {@link OAuth2ClientCredentialsClient#fetchToken}
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
    private final CredentialEncryptionFacade facade;
    private final OAuth2TokenCache cache;
    private final OAuth2ClientCredentialsClient oauthClient;

    /**
     * 构造解析器，注入凭证仓储、加解密 facade、OAuth2 token 缓存与 client。
     *
     * @param repo        凭证仓储，不可为 null
     * @param facade      凭证加解密 facade，不可为 null
     * @param cache       OAuth2 token 缓存，不可为 null
     * @param oauthClient OAuth2 client credentials 客户端，不可为 null
     */
    public CallbackCredentialResolver(final CallbackCredentialRepository repo,
                                      final CredentialEncryptionFacade facade,
                                      final OAuth2TokenCache cache,
                                      final OAuth2ClientCredentialsClient oauthClient) {
        this.repo = repo;
        this.facade = facade;
        this.cache = cache;
        this.oauthClient = oauthClient;
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
        final String plain = facade.decrypt(entity.getTokenCiphertext(), entity.getKeyId());
        return new AuthHeader(entity.getTokenHeader(), plain);
    }

    private AuthHeader resolveOAuth2(final String interfaceId) {
        final Optional<String> cached = cache.get(interfaceId);
        if (cached.isPresent()) {
            return new AuthHeader(AUTHORIZATION_HEADER, BEARER_PREFIX + cached.get());
        }
        final CallbackCredentialEntity entity = repo.findByInterfaceId(interfaceId)
                .orElseThrow(() -> new CallbackCredentialMissingException(
                        "OAUTH2 credential missing for interfaceId=" + interfaceId));
        final String clientId = facade.decrypt(entity.getOauthClientIdCiphertext(), entity.getKeyId());
        final String clientSecret =
                facade.decrypt(entity.getOauthClientSecretCiphertext(), entity.getKeyId());
        final OAuth2TokenResponse resp = oauthClient.fetchToken(
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
