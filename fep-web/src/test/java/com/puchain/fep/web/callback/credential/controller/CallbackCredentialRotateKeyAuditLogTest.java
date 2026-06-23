package com.puchain.fep.web.callback.credential.controller;

import com.puchain.fep.common.domain.EnableDisableStatus;
import com.puchain.fep.web.callback.credential.dto.CallbackCredentialCreateRequest;
import com.puchain.fep.web.callback.credential.service.CallbackCredentialAdminService;
import com.puchain.fep.web.submission.outputinterface.domain.InterfaceAuthType;
import com.puchain.fep.web.submission.outputinterface.domain.SubOutputInterface;
import com.puchain.fep.web.submission.outputinterface.repository.SubOutputInterfaceRepository;
import com.puchain.fep.web.sysmgmt.log.domain.OperationType;
import com.puchain.fep.web.sysmgmt.log.domain.SysOperationLog;
import com.puchain.fep.web.sysmgmt.log.repository.SysOperationLogRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * rotate-key 端点审计日志落库验证（FR-INFRA-CALLBACK-CREDENTIAL / §8.3 凭证轮换可审计）。
 *
 * <p>验证 {@code POST /{interfaceId}/rotate-key}（{@code @OperationLog(type=UPDATE)}）经
 * {@code OperationLogAspect} 落 1 条 {@code SysOperationLog}（module=回调凭证管理 / operation=UPDATE /
 * requestUrl 含 interfaceId），且审计记录不含任何凭证明文/密文。</p>
 *
 * <p>{@code @SpringBootTest + @AutoConfigureMockMvc} 经 MockMvc POST 驱动（AOP 取 requestUrl/method
 * 依赖 {@code RequestContextHolder} HTTP 上下文，standalone MockMvc 不启 AOP）；{@code addFilters=false}
 * 跳过 {@code @PreAuthorize}。审计经 append-only 链写入，<strong>不删 operation_log</strong>
 * （ArchUnit append-only 强制）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@TestPropertySource(properties = {
        "fep.transport.provider=mock",
        "fep.collector.scheduling.enabled=false",
        "management.health.redis.enabled=false"
})
@DisplayName("Callback rotate-key writes an audit log without credential material")
class CallbackCredentialRotateKeyAuditLogTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private CallbackCredentialAdminService credentialAdminService;
    @Autowired
    private SubOutputInterfaceRepository subOutputInterfaceRepository;
    @Autowired
    private SysOperationLogRepository operationLogRepository;

    private String seededInterfaceId;

    @AfterEach
    void tearDown() {
        if (seededInterfaceId != null) {
            // 经 @Transactional 服务删凭证（derived deleteByInterfaceId 需事务，非事务测试直调会
            // "No EntityManager with actual transaction"），再删 interface。
            credentialAdminService.delete(seededInterfaceId);
            subOutputInterfaceRepository.findById(seededInterfaceId)
                    .ifPresent(subOutputInterfaceRepository::delete);
        }
        // 不删 operation_log：append-only 审计链（ArchUnit OperationLogAppendOnlyArchTest 强制）。
    }

    private void seedOauthCredential() {
        final long nonce = System.nanoTime();
        final SubOutputInterface iface = new SubOutputInterface();
        seededInterfaceId = UUID.randomUUID().toString().replace("-", "");
        iface.setInterfaceId(seededInterfaceId);
        iface.setInterfaceName("RotateKey audit interface " + nonce);
        iface.setInterfaceUrl("http://127.0.0.1:9/callback");
        iface.setBusinessTypeId("bt-" + nonce);
        iface.setAuthType(InterfaceAuthType.OAUTH2);
        iface.setTimeoutSeconds(5);
        iface.setRetryCount(0);
        iface.setInterfaceStatus(EnableDisableStatus.ENABLED);
        iface.setCallCount(0L);
        subOutputInterfaceRepository.save(iface);

        final CallbackCredentialCreateRequest req = new CallbackCredentialCreateRequest();
        req.setInterfaceId(seededInterfaceId);
        req.setAuthType(InterfaceAuthType.OAUTH2);
        req.setOauthClientId("clid-rotate");
        req.setOauthClientSecret("csec-rotate");
        req.setOauthTokenEndpoint("http://127.0.0.1:9/token");
        req.setOauthScope("callback");
        credentialAdminService.create(req);
    }

    @Test
    @DisplayName("rotate-key produces an UPDATE audit log entry carrying interfaceId but no secrets")
    void rotateKey_writesAuditLogWithoutCredentialMaterial() throws Exception {
        seedOauthCredential();

        mockMvc.perform(post("/api/v1/callback/credentials/" + seededInterfaceId + "/rotate-key"))
                .andExpect(status().isOk());

        final List<SysOperationLog> matches = operationLogRepository.findAll().stream()
                .filter(l -> "回调凭证管理".equals(l.getModule()))
                .filter(l -> l.getRequestUrl() != null && l.getRequestUrl().contains(seededInterfaceId))
                .toList();

        Assertions.assertThat(matches)
                .as("rotate-key must produce exactly one audit log entry for this interface")
                .hasSize(1);
        final SysOperationLog audit = matches.get(0);
        Assertions.assertThat(audit.getOperation())
                .as("rotate-key audit operation must be UPDATE")
                .isEqualTo(OperationType.UPDATE);
        Assertions.assertThat(audit.getDescription())
                .as("rotate-key audit description")
                .contains("轮换");
        // 审计记录不得含任何凭证明文/密文（requestParams 仅查询串，rotate-key 无 body→null）。
        Assertions.assertThat(String.valueOf(audit.getRequestParams()) + audit.getDescription())
                .as("audit record must not leak any credential material")
                .doesNotContain("clid-rotate")
                .doesNotContain("csec-rotate");
    }
}
