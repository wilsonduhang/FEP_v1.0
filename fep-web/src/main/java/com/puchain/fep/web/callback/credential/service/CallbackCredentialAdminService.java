package com.puchain.fep.web.callback.credential.service;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.web.callback.credential.crypto.CallbackCredentialEncryptionFacade;
import com.puchain.fep.web.callback.credential.crypto.CallbackCredentialEncryptionFacade.EncryptedCredential;
import com.puchain.fep.web.callback.credential.domain.CallbackCredentialEntity;
import com.puchain.fep.web.callback.credential.dto.CallbackCredentialCreateRequest;
import com.puchain.fep.web.callback.credential.dto.CallbackCredentialResponse;
import com.puchain.fep.web.callback.credential.dto.CallbackCredentialUpdateRequest;
import com.puchain.fep.web.callback.credential.oauth.CallbackOAuth2TokenCache;
import com.puchain.fep.web.callback.credential.repository.CallbackCredentialRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 回调凭证管理服务（admin CRUD）。
 *
 * <p>明文凭证经 {@link CallbackCredentialEncryptionFacade} SM4 加密后落库为密文列；
 * 更新为 partial（null 字段保留原值，密文字段非空时轮换）；凭证变更后清空
 * {@link CallbackOAuth2TokenCache} 强制下次以新凭证重新换取 token。
 * 响应 DTO {@link CallbackCredentialResponse} 绝不回显任何密文。
 * 参见 PRD v1.3 §5.5.3 凭证配置（FR-INFRA-CALLBACK-CREDENTIAL）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
@Transactional
public class CallbackCredentialAdminService {

    private final CallbackCredentialRepository repo;
    private final CallbackCredentialEncryptionFacade facade;
    private final CallbackOAuth2TokenCache tokenCache;

    /**
     * 构造凭证管理服务。
     *
     * @param repo       凭证仓储
     * @param facade     SM4 加密门面
     * @param tokenCache OAuth2 token 缓存（凭证变更后失效）
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring-managed singletons stored by reference per container contract")
    public CallbackCredentialAdminService(final CallbackCredentialRepository repo,
                                          final CallbackCredentialEncryptionFacade facade,
                                          final CallbackOAuth2TokenCache tokenCache) {
        this.repo = repo;
        this.facade = facade;
        this.tokenCache = tokenCache;
    }

    /**
     * 新建凭证。明文字段 server 端加密后落库。
     *
     * @param req 新建请求
     * @return 不含密文的凭证响应
     * @throws FepBusinessException 接口已存在凭证（BIZ_5002）或 authType 不支持持久化（NONE，BIZ_5002）
     */
    public CallbackCredentialResponse create(final CallbackCredentialCreateRequest req) {
        if (repo.findByInterfaceId(req.getInterfaceId()).isPresent()) {
            throw new FepBusinessException(FepErrorCode.BIZ_5002,
                    "credential already exists for interfaceId=" + req.getInterfaceId());
        }
        final CallbackCredentialEntity entity = switch (req.getAuthType()) {
            case TOKEN -> {
                final EncryptedCredential enc = facade.encrypt(req.getToken());
                yield CallbackCredentialEntity.newToken(req.getInterfaceId(),
                        enc.ciphertext(), req.getTokenHeader(), enc.keyId());
            }
            case OAUTH2 -> {
                final EncryptedCredential encId = facade.encrypt(req.getOauthClientId());
                final EncryptedCredential encSec = facade.encrypt(req.getOauthClientSecret());
                yield CallbackCredentialEntity.newOauth(req.getInterfaceId(),
                        encId.ciphertext(), encSec.ciphertext(),
                        req.getOauthTokenEndpoint(), req.getOauthScope(), encId.keyId());
            }
            case NONE -> throw new FepBusinessException(FepErrorCode.BIZ_5002,
                    "NONE authType has no credential to persist");
        };
        repo.save(entity);
        return CallbackCredentialResponse.from(entity);
    }

    /**
     * 局部更新凭证。null 字段保留原值；密文字段非空时重新加密轮换；
     * 非密文元数据非空时直接更新。更新后失效 OAuth2 token 缓存。
     *
     * @param interfaceId 接口 ID
     * @param req         更新请求
     * @return 不含密文的凭证响应
     * @throws FepBusinessException 凭证不存在（BIZ_5001）
     */
    public CallbackCredentialResponse update(final String interfaceId, final CallbackCredentialUpdateRequest req) {
        final CallbackCredentialEntity e = repo.findByInterfaceId(interfaceId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "credential not found, interfaceId=" + interfaceId));

        byte[] newTokenCipher = null;
        byte[] newClientIdCipher = null;
        byte[] newClientSecretCipher = null;
        String newKeyId = e.getKeyId();

        if (req.getToken() != null) {
            final EncryptedCredential enc = facade.encrypt(req.getToken());
            newTokenCipher = enc.ciphertext();
            newKeyId = enc.keyId();
        }
        if (req.getOauthClientId() != null) {
            final EncryptedCredential enc = facade.encrypt(req.getOauthClientId());
            newClientIdCipher = enc.ciphertext();
            newKeyId = enc.keyId();
        }
        if (req.getOauthClientSecret() != null) {
            final EncryptedCredential enc = facade.encrypt(req.getOauthClientSecret());
            newClientSecretCipher = enc.ciphertext();
            newKeyId = enc.keyId();
        }
        if (newTokenCipher != null || newClientIdCipher != null || newClientSecretCipher != null) {
            e.rotate(newTokenCipher, newClientIdCipher, newClientSecretCipher, newKeyId);
        }
        e.updateNonSecretFields(req.getTokenHeader(), req.getOauthTokenEndpoint(), req.getOauthScope());

        tokenCache.invalidate(interfaceId);
        return CallbackCredentialResponse.from(e);
    }

    /**
     * 查询凭证（不回显密文）。
     *
     * @param interfaceId 接口 ID
     * @return 不含密文的凭证响应
     * @throws FepBusinessException 凭证不存在（BIZ_5001）
     */
    @Transactional(readOnly = true)
    public CallbackCredentialResponse get(final String interfaceId) {
        return repo.findByInterfaceId(interfaceId).map(CallbackCredentialResponse::from)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "credential not found, interfaceId=" + interfaceId));
    }

    /**
     * 列出全部凭证（不回显密文）。
     *
     * @return 凭证响应列表
     */
    @Transactional(readOnly = true)
    public List<CallbackCredentialResponse> list() {
        return repo.findAll().stream().map(CallbackCredentialResponse::from).toList();
    }

    /**
     * 删除凭证并失效 OAuth2 token 缓存。凭证不存在时静默返回（幂等）。
     *
     * @param interfaceId 接口 ID
     */
    public void delete(final String interfaceId) {
        repo.findByInterfaceId(interfaceId).ifPresent(e -> {
            repo.deleteByInterfaceId(interfaceId);
            tokenCache.invalidate(interfaceId);
        });
    }
}
