package com.puchain.fep.web.callback.credential.repository;

import com.puchain.fep.web.callback.credential.domain.CallbackCredentialEntity;
import com.puchain.fep.web.submission.outputinterface.domain.InterfaceAuthType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link CallbackCredentialRepository} 行为验证。
 *
 * <p>使用 {@code @SpringBootTest} 而非 {@code @DataJpaTest}，因为 H2 MODE=MySQL 的
 * DDL（V10 t_sub_output_interface 含 ENGINE/COMMENT 等 MySQL 方言）需要完整 Flyway +
 * 应用上下文（与 {@code CallbackQueueRepositoryTest} 保持一致）。</p>
 *
 * <p><strong>FK 前提</strong>：{@code callback_credential.interface_id} FK→
 * {@code t_sub_output_interface(interface_id)}（V30 / B1），因此每个测试在保存凭证前
 * 必须先 seed 对应的 {@link SubOutputInterface} 父行（{@link #seedInterface}）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@Transactional
class CallbackCredentialRepositoryTest {

    @Autowired
    private CallbackCredentialRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void findByInterfaceId_shouldReturnPersistedTokenCredential() {
        seedInterface("IF-T3-001");
        final CallbackCredentialEntity entity = CallbackCredentialEntity.newToken(
                "IF-T3-001", new byte[]{1, 2, 3}, "Authorization", "KEY-V1", null);
        repository.save(entity);
        entityManager.flush();
        entityManager.clear();

        final Optional<CallbackCredentialEntity> found = repository.findByInterfaceId("IF-T3-001");

        assertThat(found).isPresent();
        assertThat(found.get().getAuthType()).isEqualTo(InterfaceAuthType.TOKEN);
        assertThat(found.get().getTokenCiphertext()).containsExactly(1, 2, 3);
        assertThat(found.get().getTokenHeader()).isEqualTo("Authorization");
        assertThat(found.get().getKeyId()).isEqualTo("KEY-V1");
    }

    @Test
    void save_shouldRejectDuplicateInterfaceIdByUniqueConstraint() {
        seedInterface("IF-T3-002");
        repository.saveAndFlush(CallbackCredentialEntity.newToken(
                "IF-T3-002", new byte[]{1}, null, "KEY-V1", null));

        assertThatThrownBy(() -> repository.saveAndFlush(
                CallbackCredentialEntity.newToken(
                        "IF-T3-002", new byte[]{2}, null, "KEY-V1", null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    /**
     * Seed 一个 {@code t_sub_output_interface} 父行以满足 callback_credential FK 约束。
     *
     * <p>用 native INSERT 仅填无 DEFAULT 的列（interface_id / interface_name /
     * interface_url）；其余列（auth_type / timeout_seconds / retry_count /
     * interface_status / call_count / create_time / update_time）由 V10 DDL
     * {@code DEFAULT} 兜底，避免 {@code SubOutputInterface} 缺 createTime setter
     * 的 JPA 持久化障碍。</p>
     *
     * @param interfaceId 接口 ID（同时作为 callback_credential.interface_id FK 目标）
     */
    private void seedInterface(final String interfaceId) {
        entityManager.createNativeQuery(
                "INSERT INTO t_sub_output_interface "
                        + "(interface_id, interface_name, interface_url) VALUES (?, ?, ?)")
                .setParameter(1, interfaceId)
                .setParameter(2, "test-" + interfaceId)
                .setParameter(3, "https://callback.test/" + interfaceId)
                .executeUpdate();
        entityManager.flush();
    }
}
