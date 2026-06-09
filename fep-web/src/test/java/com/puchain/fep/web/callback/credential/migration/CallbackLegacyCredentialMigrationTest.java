package com.puchain.fep.web.callback.credential.migration;

import com.puchain.fep.web.callback.credential.domain.CallbackCredentialEntity;
import com.puchain.fep.web.callback.credential.repository.CallbackCredentialRepository;
import com.puchain.fep.web.callback.credential.service.CallbackCredentialResolver;
import com.puchain.fep.web.callback.credential.service.CallbackCredentialResolver.AuthHeader;
import com.puchain.fep.security.api.SignService;
import com.puchain.fep.web.submission.outputinterface.domain.InterfaceAuthType;
import com.puchain.fep.web.submission.outputinterface.domain.SubOutputInterface;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 惰性双读迁移端到端：legacy 明文行 → 首读返回明文 + 重加密 + key_id 翻转 → 二读真实解密。
 *
 * <p>启用真实 impl provider（GB/T 测试密钥）覆盖 mock（{@code provider=impl} 使 mock
 * 互斥关闭、impl 装配，单 bean），验证迁移闭环。命名 {@code *Test} 确保 Surefire 收录。</p>
 */
@SpringBootTest
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "fep.security.provider=impl",
        "fep.security.sm4.active-key-id=sm4-cred-v1",
        "fep.security.sm4.sm4-keys.sm4-cred-v1=0123456789abcdeffedcba9876543210",
        "fep.callback.credential.migration.legacy-plaintext-key-ids=mock-key-v1"
})
class CallbackLegacyCredentialMigrationTest {

    @Autowired
    private CallbackCredentialRepository repo;

    @Autowired
    private CallbackCredentialResolver resolver;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // provider=impl 下 SignService（SM2 签名）属 S2 未实现、mock 已门控关 → 桩补使全 context 启动
    // （R7：provider=impl 须 S2 才完整；本 IT 仅验 SM4 凭证迁移，不涉签名）。
    @MockBean
    private SignService signService;

    @Test
    void legacyTokenRow_lazilyMigratesToRealSm4OnFirstRead() {
        // seed t_sub_output_interface 父行满足 callback_credential FK（JdbcTemplate 自动提交，
        // 镜像 CallbackCredentialRepositoryTest.seedInterface 的 native INSERT 范式）
        jdbcTemplate.update("INSERT INTO t_sub_output_interface "
                + "(interface_id, interface_name, interface_url) VALUES (?, ?, ?)",
                "mig-iface-1", "test-mig-iface-1", "https://callback.test/mig-iface-1");

        final byte[] plaintextBytes = "legacy-token-value".getBytes(StandardCharsets.UTF_8);
        final CallbackCredentialEntity legacy = CallbackCredentialEntity.newToken(
                "mig-iface-1", plaintextBytes, "Authorization", "mock-key-v1", null);
        repo.save(legacy);

        final SubOutputInterface target = mock(SubOutputInterface.class);
        when(target.getAuthType()).thenReturn(InterfaceAuthType.TOKEN);
        when(target.getInterfaceId()).thenReturn("mig-iface-1");

        // 首次读：双读返回明文 + 触发迁移
        final Optional<AuthHeader> first = resolver.resolveAuthHeader(target);
        assertThat(first).isPresent();
        assertThat(first.get().value()).isEqualTo("legacy-token-value");

        // DB 行已迁移：key_id 翻转 + 密文列真实加密（≠ 原明文字节）
        final CallbackCredentialEntity migrated =
                repo.findByInterfaceId("mig-iface-1").orElseThrow();
        assertThat(migrated.getKeyId()).isEqualTo("sm4-cred-v1");
        assertThat(migrated.getTokenCiphertext()).isNotEqualTo(plaintextBytes);

        // 二次读：真实 SM4 解密路径，返回相同明文
        final Optional<AuthHeader> second = resolver.resolveAuthHeader(target);
        assertThat(second).isPresent();
        assertThat(second.get().value()).isEqualTo("legacy-token-value");
    }
}
