package com.puchain.fep.web.callback;

import com.puchain.fep.common.domain.EnableDisableStatus;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.event.InboundMessageProcessedEvent;
import com.puchain.fep.web.callback.credential.dto.CallbackCredentialCreateRequest;
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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Callback Phase 2b 凭证 TOKEN / NONE 端到端测试（FR-INFRA-CALLBACK-CREDENTIAL）。
 *
 * <p>补两个认证方式端到端缺口（既有 {@code CallbackPhase2bEndToEndIT} 仅 OAuth2 path 且因
 * {@code *IT} 命名在 {@code mvn verify} 被 surefire 静默跳过，从不执行——本类用 {@code *Test}
 * 命名确保 CI 真实执行）：</p>
 * <ul>
 *   <li>TOKEN — 静态 token 密文落库，运行时解密注入自定义 header，bank 收到明文 token。</li>
 *   <li>NONE — 不建凭证记录，bank 收到的 Authorization 头为 null。</li>
 * </ul>
 *
 * <p><strong>mock 凭证非-legacy 配置:</strong> mock 透传期凭证 keyId 恒为 {@code mock-key-v1}，
 * 而 {@code legacy-plaintext-key-ids} 默认含 {@code mock-key-v1} → 解析期 {@code isLegacy=true}
 * 触发 {@code migrateToActiveKey}，但 mock 活跃 key 即 {@code mock-key-v1}（active∈legacy）致
 * 迁移产物仍 legacy → 抛 {@code refusing to write}。本测试经
 * {@code legacy-plaintext-key-ids=}（空）令 mock 凭证非-legacy，解析走 facade 直解密（mock 透传）
 * 路径——镜像生产 impl 凭证非-legacy 的真实解析行为。</p>
 *
 * <p>harness：JDK {@link HttpServer} 单 context {@code /callback} 录请求头；bank 直接返回 200。
 * 无 IDP（TOKEN/NONE 不取 token）。调度中性化，测试内手动 {@link CallbackQueueRunner#poll()}。</p>
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
@DisplayName("Callback Phase 2b E2E — TOKEN header injection + NONE no-auth")
class CallbackCredentialTokenNoneEndToEndTest {

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
    private CallbackCredentialAdminService credentialAdminService;

    private HttpServer mockServer;
    private List<String> tokenHeaders;
    private List<String> authHeaders;
    private String bankCallbackUrl;

    private String seededBtId;
    private String seededMsgNoId;
    private String seededInterfaceId;

    @BeforeEach
    void setUp() throws IOException {
        tokenHeaders = new CopyOnWriteArrayList<>();
        authHeaders = new CopyOnWriteArrayList<>();
        mockServer = HttpServer.create(new InetSocketAddress(0), 0);
        mockServer.createContext("/callback", exchange -> {
            tokenHeaders.add(String.valueOf(exchange.getRequestHeaders().getFirst("X-Api-Token")));
            authHeaders.add(String.valueOf(exchange.getRequestHeaders().getFirst("Authorization")));
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
        // 先删 credential（FK 指向 interface），否则 interface 删除触发 FK 约束失败致行残留 → 重名冲突。
        if (seededInterfaceId != null) {
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

    /** Seed an ENABLED business type + msgNo(2103) + interface (unique name) with the given authType. */
    private void seedInterface(final InterfaceAuthType authType) {
        final long nonce = System.nanoTime();
        final SysBusinessType bt = new SysBusinessType();
        bt.setTypeId(UUID.randomUUID().toString().replace("-", ""));
        bt.setTypeCode("TC-TN-" + nonce);
        bt.setTypeName("Token/None E2E business type " + nonce);
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
        iface.setInterfaceName("Token/None E2E interface " + nonce);
        iface.setInterfaceUrl(bankCallbackUrl);
        iface.setBusinessTypeId(bt.getTypeId());
        iface.setAuthType(authType);
        iface.setTimeoutSeconds(5);
        iface.setRetryCount(0);
        iface.setInterfaceStatus(EnableDisableStatus.ENABLED);
        iface.setCallCount(0L);
        subOutputInterfaceRepository.save(iface);
    }

    private void triggerCallback() {
        eventPublisher.publishEvent(new InboundMessageProcessedEvent(
                MessageType.MSG_2103, "T2100001", "SER-TN-" + System.nanoTime(), null, Instant.now()));
        callbackQueueRunner.poll();
    }

    // CallbackQueueStatus 是 final class + String 常量（非 enum），getStatus() 返回 String。
    private String statusOf2103() {
        return callbackQueueRepository.findAll().stream()
                .filter(r -> "2103".equals(r.getMsgNo()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("queue row for msgNo=2103 must exist"))
                .getStatus();
    }

    @Test
    @DisplayName("TOKEN: decrypted static token injected into configured header, callback DONE")
    void tokenAuth_injectsDecryptedTokenHeader() {
        seedInterface(InterfaceAuthType.TOKEN);
        final CallbackCredentialCreateRequest req = new CallbackCredentialCreateRequest();
        req.setInterfaceId(seededInterfaceId);
        req.setAuthType(InterfaceAuthType.TOKEN);
        req.setToken("static-tok-123");
        req.setTokenHeader("X-Api-Token");
        credentialAdminService.create(req);

        triggerCallback();

        Assertions.assertThat(tokenHeaders)
                .as("bank should receive exactly one POST carrying the decrypted token")
                .hasSize(1);
        Assertions.assertThat(tokenHeaders.get(0))
                .as("TOKEN auth must inject decrypted plaintext token into the configured header")
                .isEqualTo("static-tok-123");
        Assertions.assertThat(statusOf2103())
                .as("successful TOKEN callback should reach DONE")
                .isEqualTo(CallbackQueueStatus.DONE);
    }

    @Test
    @DisplayName("NONE: no auth header injected, callback DONE")
    void noneAuth_sendsNoAuthHeader() {
        seedInterface(InterfaceAuthType.NONE);

        triggerCallback();

        Assertions.assertThat(authHeaders).hasSize(1);
        Assertions.assertThat(authHeaders.get(0))
                .as("NONE auth must not inject any Authorization header")
                .isEqualTo("null");
        Assertions.assertThat(tokenHeaders.get(0))
                .as("NONE auth must not inject any token header")
                .isEqualTo("null");
        Assertions.assertThat(statusOf2103())
                .as("successful NONE callback should reach DONE")
                .isEqualTo(CallbackQueueStatus.DONE);
    }
}
