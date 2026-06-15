package com.puchain.fep.web.callback;

import com.puchain.fep.common.domain.EnableDisableStatus;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.event.InboundMessageProcessedEvent;
import com.puchain.fep.web.callback.credential.domain.CallbackCredentialEntity;
import com.puchain.fep.web.callback.credential.repository.CallbackCredentialRepository;
import com.puchain.fep.web.callback.credential.service.CallbackCredentialAdminService;
import com.puchain.fep.web.callback.domain.CallbackQueueStatus;
import com.puchain.fep.web.callback.repository.CallbackQueueRepository;
import com.puchain.fep.web.callback.runner.CallbackQueueRunner;
import com.puchain.fep.web.submission.outputinterface.domain.InterfaceAuthType;
import com.puchain.fep.web.submission.outputinterface.domain.SubOutputInterface;
import com.puchain.fep.web.submission.outputinterface.repository.SubOutputInterfaceRepository;
import com.puchain.fep.web.sysmgmt.config.businesstype.domain.SysBusinessType;
import com.puchain.fep.web.sysmgmt.config.businesstype.domain.SysBusinessTypeMsgNo;
import com.puchain.fep.web.sysmgmt.config.businesstype.repository.SysBusinessTypeMsgNoRepository;
import com.puchain.fep.web.sysmgmt.config.businesstype.repository.SysBusinessTypeRepository;
import com.sun.net.httpserver.HttpServer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Callback Phase 2b 凭证过期端到端拒用测试（FR-INFRA-CALLBACK-CREDENTIAL / §8.3）。
 *
 * <p>种子一个 {@code expiresAt} 在过去的 TOKEN 凭证（直接构造 entity + repo.save，绕过
 * {@code CallbackCredentialAdminService.create} 的将来校验），验证 {@code CallbackCredentialResolver}
 * 在解析期 {@code ensureNotExpired} 拒用过期凭证 → 不发出任何 HTTP（bank 收到 0 请求），
 * 回调行不达 DONE（不静默降级为无鉴权）。</p>
 *
 * <p>命名 {@code *Test}（非 {@code *IT}）：本项目无 failsafe plugin + surefire 默认 includes
 * 静默跳过 {@code *IT}，故 {@code *Test} 确保覆盖在 CI 真实执行。{@code legacy-plaintext-key-ids=}
 * 空令 mock 凭证非-legacy（过期检查先于 legacy 检查，此处为一致性保留）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@TestPropertySource(properties = {
        "fep.transport.provider=mock",
        "fep.collector.institution-code=12345678901234",
        "fep.collector.scheduling.enabled=false",
        "fep.outbound.queue.poll-interval-ms=99999",
        "fep.outbound.queue.poll-initial-delay-ms=99999",
        "fep.callback.poll-interval-ms=600000",
        "fep.callback.poll-initial-delay-ms=600000",
        "fep.callback.credential.migration.legacy-plaintext-key-ids=",
        "management.health.redis.enabled=false"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Callback Phase 2b E2E — expired credential rejected before any HTTP send")
class CallbackCredentialExpiryEndToEndTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;
    @Autowired
    private CallbackQueueRepository callbackQueueRepository;
    @Autowired
    private SysBusinessTypeRepository sysBusinessTypeRepository;
    @Autowired
    private SysBusinessTypeMsgNoRepository sysBusinessTypeMsgNoRepository;
    @Autowired
    private SubOutputInterfaceRepository subOutputInterfaceRepository;
    @Autowired
    private CallbackQueueRunner callbackQueueRunner;
    @Autowired
    private CallbackCredentialRepository credentialRepository;
    @Autowired
    private CallbackCredentialAdminService credentialAdminService;

    private HttpServer mockServer;
    private final AtomicInteger bankHits = new AtomicInteger(0);
    private String bankCallbackUrl;

    private String seededBtId;
    private String seededMsgNoId;
    private String seededInterfaceId;

    @BeforeEach
    void setUp() throws IOException {
        // 防御性前置清场：消除对 peer @SpringBootTest tearDown 完整性的隐式依赖，确保共享 H2
        // 下 msgNo=2103 队列行查询无歧义（红线 shared_h2_topn_aggregation_test_isolation）。
        callbackQueueRepository.deleteAll();
        bankHits.set(0);
        mockServer = HttpServer.create(new InetSocketAddress(0), 0);
        mockServer.createContext("/callback", exchange -> {
            bankHits.incrementAndGet();
            try (InputStream is = exchange.getRequestBody()) {
                is.readAllBytes();
            }
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write("X".getBytes(StandardCharsets.UTF_8));
            }
        });
        mockServer.start();
        bankCallbackUrl = "http://127.0.0.1:" + mockServer.getAddress().getPort() + "/callback";
    }

    @AfterEach
    void tearDown() {
        if (mockServer != null) {
            mockServer.stop(0);
        }
        callbackQueueRepository.deleteAll();
        if (seededInterfaceId != null) {
            // 经 @Transactional 服务删凭证（derived deleteByInterfaceId 需事务，非事务测试直调
            // 不可靠 "No EntityManager with actual transaction"），再删 interface。
            credentialAdminService.delete(seededInterfaceId);
            subOutputInterfaceRepository.findById(seededInterfaceId)
                    .ifPresent(subOutputInterfaceRepository::delete);
        }
        if (seededMsgNoId != null) {
            sysBusinessTypeMsgNoRepository.findById(seededMsgNoId)
                    .ifPresent(sysBusinessTypeMsgNoRepository::delete);
        }
        if (seededBtId != null) {
            sysBusinessTypeRepository.findById(seededBtId)
                    .ifPresent(sysBusinessTypeRepository::delete);
        }
    }

    /** Seed an ENABLED TOKEN interface + a credential whose expiresAt is in the past. */
    private void seedExpiredTokenInterface() {
        final long nonce = System.nanoTime();
        final SysBusinessType bt = new SysBusinessType();
        bt.setTypeId(UUID.randomUUID().toString().replace("-", ""));
        bt.setTypeCode("TC-EXP-" + nonce);
        bt.setTypeName("Expiry E2E business type " + nonce);
        bt.setSortOrder(1);
        bt.setTypeStatus(EnableDisableStatus.ENABLED);
        sysBusinessTypeRepository.save(bt);
        seededBtId = bt.getTypeId();

        final SysBusinessTypeMsgNo msgNoEntry = new SysBusinessTypeMsgNo(bt.getTypeId(), "2103");
        sysBusinessTypeMsgNoRepository.save(msgNoEntry);
        seededMsgNoId = msgNoEntry.getId();

        final SubOutputInterface iface = new SubOutputInterface();
        seededInterfaceId = UUID.randomUUID().toString().replace("-", "");
        iface.setInterfaceId(seededInterfaceId);
        iface.setInterfaceName("Expiry E2E interface " + nonce);
        iface.setInterfaceUrl(bankCallbackUrl);
        iface.setBusinessTypeId(bt.getTypeId());
        iface.setAuthType(InterfaceAuthType.TOKEN);
        iface.setTimeoutSeconds(5);
        iface.setRetryCount(0);
        iface.setInterfaceStatus(EnableDisableStatus.ENABLED);
        iface.setCallCount(0L);
        subOutputInterfaceRepository.save(iface);

        // 直接构造过期凭证落库（绕过 create 将来校验 + 不依赖 credential.crypto facade —— 避免
        // ArchUnit R8「credential.crypto 仅限 credential 包内使用」违规，本 callback 包测试不可依赖
        // crypto facade）。解析器 ensureNotExpired 先于解密执行，故 cipher 字节/keyId 内容不影响
        // 过期判定。expiresAt 取 1 年前：解析器 Clock 时区与本地墙钟有偏移（减 1 小时会被误判未
        // 过期），1 年余量彻底消除任何时钟/时区脆弱性，明确已过期。
        final CallbackCredentialEntity expired = CallbackCredentialEntity.newToken(
                seededInterfaceId, "expired-token-cipher".getBytes(StandardCharsets.UTF_8),
                "X-Api-Token", "mock-key-v1", LocalDateTime.now().minusYears(1));
        credentialRepository.save(expired);
    }

    @Test
    @DisplayName("expired TOKEN credential is rejected before any HTTP send (bank 0 requests, row not DONE)")
    void expiredCredential_isRejectedBeforeHttp() {
        seedExpiredTokenInterface();

        // 前置：确认 expiresAt 真持久化为过去（否则诊断是持久化问题而非解析器时钟问题）。
        final LocalDateTime persistedExpiry = credentialRepository.findByInterfaceId(seededInterfaceId)
                .orElseThrow(() -> new AssertionError("seeded credential must exist"))
                .getExpiresAt();
        Assertions.assertThat(persistedExpiry)
                .as("seeded credential expiresAt must be persisted in the past")
                .isNotNull()
                .isBefore(LocalDateTime.now());

        eventPublisher.publishEvent(new InboundMessageProcessedEvent(
                MessageType.MSG_2103, "T2100001", "SER-EXP-" + System.nanoTime(), null, Instant.now()));
        callbackQueueRunner.poll();

        Assertions.assertThat(bankHits.get())
                .as("expired credential must be rejected before any HTTP send")
                .isZero();
        final String status = callbackQueueRepository.findAll().stream()
                .filter(r -> "2103".equals(r.getMsgNo()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("queue row for msgNo=2103 must exist"))
                .getStatus();
        Assertions.assertThat(status)
                .as("expired-credential callback must not reach DONE")
                .isNotEqualTo(CallbackQueueStatus.DONE);
    }
}
