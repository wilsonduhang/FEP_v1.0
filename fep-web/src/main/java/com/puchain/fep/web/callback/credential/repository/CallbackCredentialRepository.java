package com.puchain.fep.web.callback.credential.repository;

import com.puchain.fep.web.callback.credential.domain.CallbackCredentialEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * {@link CallbackCredentialEntity} 持久化仓储。
 *
 * <p>1:1 凭证查询/删除按 {@code interface_id} 索引（V30 uk_callback_credential_interface
 * 唯一约束保证最多一条）。参见 PRD v1.3 §5.5.3 凭证配置（FR-INFRA-CALLBACK-CREDENTIAL）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Repository
public interface CallbackCredentialRepository
        extends JpaRepository<CallbackCredentialEntity, String> {

    /**
     * 按输出接口 ID 查询凭证。
     *
     * @param interfaceId 输出接口 ID
     * @return 凭证实体（不存在返回 {@link Optional#empty()}）
     */
    Optional<CallbackCredentialEntity> findByInterfaceId(String interfaceId);

    /**
     * 按输出接口 ID 删除凭证（接口解绑时调用）。
     *
     * @param interfaceId 输出接口 ID
     */
    void deleteByInterfaceId(String interfaceId);
}
