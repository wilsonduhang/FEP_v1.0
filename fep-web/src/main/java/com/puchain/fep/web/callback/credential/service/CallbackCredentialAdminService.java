package com.puchain.fep.web.callback.credential.service;

import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.web.callback.credential.crypto.CallbackCredentialEncryptionFacade;
import com.puchain.fep.web.callback.credential.crypto.CallbackCredentialEncryptionFacade.EncryptedCredential;
import com.puchain.fep.web.callback.credential.domain.CallbackCredentialEntity;
import com.puchain.fep.web.callback.credential.dto.CallbackCredentialCreateRequest;
import com.puchain.fep.web.callback.credential.dto.CallbackCredentialResponse;
import com.puchain.fep.web.callback.credential.dto.CallbackCredentialSweepResponse;
import com.puchain.fep.web.callback.credential.dto.CallbackCredentialUpdateRequest;
import com.puchain.fep.web.callback.credential.migration.CallbackLegacyCredentialKeyIdProperties;
import com.puchain.fep.web.callback.credential.migration.CallbackLegacyCredentialMigrator;
import com.puchain.fep.web.callback.credential.oauth.CallbackOAuth2TokenCache;
import com.puchain.fep.web.callback.credential.repository.CallbackCredentialRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    private static final Logger LOG = LoggerFactory.getLogger(CallbackCredentialAdminService.class);

    private final CallbackCredentialRepository repo;
    private final CallbackCredentialEncryptionFacade facade;
    private final CallbackOAuth2TokenCache tokenCache;
    private final CallbackLegacyCredentialMigrator migrator;
    private final CallbackLegacyCredentialKeyIdProperties legacyProps;

    /**
     * 构造凭证管理服务。
     *
     * @param repo        凭证仓储
     * @param facade      SM4 加密门面
     * @param tokenCache  OAuth2 token 缓存（凭证变更后失效）
     * @param migrator    legacy 明文凭证迁移器（批量 sweep 逐行委托）
     * @param legacyProps legacy keyId 配置（sweep 时惰性读取）
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring-managed singletons stored by reference per container contract")
    public CallbackCredentialAdminService(final CallbackCredentialRepository repo,
                                          final CallbackCredentialEncryptionFacade facade,
                                          final CallbackOAuth2TokenCache tokenCache,
                                          final CallbackLegacyCredentialMigrator migrator,
                                          final CallbackLegacyCredentialKeyIdProperties legacyProps) {
        this.repo = repo;
        this.facade = facade;
        this.tokenCache = tokenCache;
        this.migrator = migrator;
        this.legacyProps = legacyProps;
    }

    /**
     * 主动批量迁移全部 legacy 明文凭证（冷接口收口，运维 DB 备份后触发）。
     *
     * <p>逐行委托 {@link CallbackLegacyCredentialMigrator#migrateToActiveKey}
     * （REQUIRES_NEW 独立短事务 + 幂等 + C1 守护）；单行失败 WARN 计数后继续，
     * 不阻断扫描。本方法以 {@link Propagation#NOT_SUPPORTED} 覆盖类级事务，
     * 避免外层长事务包裹批量循环。legacy 集合每次调用从配置惰性读取。</p>
     *
     * @return 迁移/失败/剩余计数（不回显任何凭证内容）
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
            justification = "interfaceId sanitized via LogSanitizer.sanitize before logging")
    public CallbackCredentialSweepResponse migrateLegacy() {
        final Set<String> legacyKeyIds = new HashSet<>(legacyProps.getLegacyPlaintextKeyIds());
        final List<CallbackCredentialEntity> rows = repo.findByKeyIdIn(legacyKeyIds);
        int migrated = 0;
        int failed = 0;
        for (final CallbackCredentialEntity row : rows) {
            try {
                migrator.migrateToActiveKey(row.getInterfaceId());
                migrated++;
            } catch (final RuntimeException ex) {
                failed++;
                LOG.warn("legacy credential sweep failed for interfaceId={}; continuing",
                        LogSanitizer.sanitize(row.getInterfaceId()), ex);
            }
        }
        final long remaining = repo.countByKeyIdIn(legacyKeyIds);
        LOG.info("legacy credential sweep done: migrated={}, failed={}, remaining={}",
                migrated, failed, remaining);
        return new CallbackCredentialSweepResponse(migrated, failed, remaining);
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
        validateExpiresAt(req.getExpiresAt());
        final CallbackCredentialEntity entity = switch (req.getAuthType()) {
            case TOKEN -> {
                final EncryptedCredential enc = facade.encrypt(req.getToken());
                yield CallbackCredentialEntity.newToken(req.getInterfaceId(),
                        enc.ciphertext(), req.getTokenHeader(), enc.keyId(), req.getExpiresAt());
            }
            case OAUTH2 -> {
                final EncryptedCredential encId = facade.encrypt(req.getOauthClientId());
                final EncryptedCredential encSec = facade.encrypt(req.getOauthClientSecret());
                yield CallbackCredentialEntity.newOauth(req.getInterfaceId(),
                        encId.ciphertext(), encSec.ciphertext(),
                        req.getOauthTokenEndpoint(), req.getOauthScope(), encId.keyId(), req.getExpiresAt());
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
        validateExpiresAt(req.getExpiresAt());

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
        e.updateExpiresAt(req.getExpiresAt());

        tokenCache.invalidate(interfaceId);
        return CallbackCredentialResponse.from(e);
    }

    /**
     * 轮换凭证密钥：用密文记录的旧 keyId 解密全部密文，再以当前活跃 key 重加密落库。
     * 适用于运维轮换活跃 SM4 主密钥后，将存量凭证迁移至新密钥版本。
     * 轮换后失效 OAuth2 token 缓存。
     *
     * @param interfaceId 接口 ID
     * @return 不含密文的凭证响应
     * @throws FepBusinessException 凭证不存在（BIZ_5001）或 NONE 类型无密文可轮换（BIZ_5003）
     */
    public CallbackCredentialResponse rotateKey(final String interfaceId) {
        final CallbackCredentialEntity e = repo.findByInterfaceId(interfaceId)
                .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                        "credential not found, interfaceId=" + interfaceId));
        final String oldKeyId = e.getKeyId();
        byte[] newTokenCipher = null;
        byte[] newClientIdCipher = null;
        byte[] newClientSecretCipher = null;
        final String newKeyId;
        switch (e.getAuthType()) {
            case TOKEN -> {
                final String plain = facade.decrypt(e.getTokenCiphertext(), oldKeyId);
                final EncryptedCredential enc = facade.encrypt(plain);
                newTokenCipher = enc.ciphertext();
                newKeyId = enc.keyId();
            }
            case OAUTH2 -> {
                final String id = facade.decrypt(e.getOauthClientIdCiphertext(), oldKeyId);
                final String secret = facade.decrypt(e.getOauthClientSecretCiphertext(), oldKeyId);
                final EncryptedCredential encId = facade.encrypt(id);
                final EncryptedCredential encSec = facade.encrypt(secret);
                newClientIdCipher = encId.ciphertext();
                newClientSecretCipher = encSec.ciphertext();
                newKeyId = encId.keyId();
            }
            case NONE -> throw new FepBusinessException(FepErrorCode.BIZ_5003,
                    "NONE authType has no credential to rotate");
            // default 为 final newKeyId 的 definite-assignment 兜底（statement-switch 非穷尽判定）
            // + 防 InterfaceAuthType 未来新增枚举值，禁删。
            default -> throw new FepBusinessException(FepErrorCode.BIZ_5003,
                    "unsupported authType for key rotation");
        }
        e.rotate(newTokenCipher, newClientIdCipher, newClientSecretCipher, newKeyId);
        tokenCache.invalidate(interfaceId);
        return CallbackCredentialResponse.from(e);
    }

    /**
     * 校验凭证有效期：非 null 时必须为将来时刻，否则抛 {@link FepBusinessException}（BIZ_5003）。
     * null 表示永不过期（create）或不变（update），合法放行。
     *
     * @param expiresAt 待校验有效期
     * @throws FepBusinessException 当 {@code expiresAt} 非 null 且不晚于当前时刻
     */
    private void validateExpiresAt(final LocalDateTime expiresAt) {
        if (expiresAt != null && !expiresAt.isAfter(LocalDateTime.now())) {
            throw new FepBusinessException(FepErrorCode.BIZ_5003,
                    "expiresAt must be in the future");
        }
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
