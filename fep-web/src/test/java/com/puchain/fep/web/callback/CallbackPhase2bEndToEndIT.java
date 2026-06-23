package com.puchain.fep.web.callback;

import com.puchain.fep.common.domain.EnableDisableStatus;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.event.InboundMessageProcessedEvent;
import com.puchain.fep.web.callback.credential.dto.CallbackCredentialCreateRequest;
import com.puchain.fep.web.callback.credential.service.CallbackCredentialAdminService;
import com.puchain.fep.web.callback.dlq.dto.DlqReplayResponse;
import com.puchain.fep.web.callback.dlq.service.CallbackReplayService;
import com.puchain.fep.web.callback.domain.CallbackQueueStatus;
import com.puchain.fep.web.callback.notification.domain.CallbackNotificationEntity;
import com.puchain.fep.web.callback.notification.repository.CallbackNotificationRepository;
import com.puchain.fep.web.callback.repository.CallbackQueueRepository;
import com.puchain.fep.web.callback.runner.CallbackQueueRunner;
import com.puchain.fep.web.submission.outputinterface.domain.InterfaceAuthType;
import com.puchain.fep.web.submission.outputinterface.domain.SubOutputInterface;
import com.puchain.fep.web.submission.outputinterface.repository.SubOutputInterfaceRepository;
import com.puchain.fep.web.sysmgmt.config.businesstype.domain.SysBusinessType;
import com.puchain.fep.web.sysmgmt.config.businesstype.domain.SysBusinessTypeMsgNo;
import com.puchain.fep.web.sysmgmt.config.businesstype.repository.SysBusinessTypeMsgNoRepository;
import com.puchain.fep.web.sysmgmt.config.businesstype.repository.SysBusinessTypeRepository;
import com.puchain.fep.web.sysmgmt.role.domain.RoleStatus;
import com.puchain.fep.web.sysmgmt.role.domain.RoleType;
import com.puchain.fep.web.sysmgmt.role.domain.SysRole;
import com.puchain.fep.web.sysmgmt.role.repository.SysRoleRepository;
import com.puchain.fep.web.sysmgmt.rel.domain.SysUserRole;
import com.puchain.fep.web.sysmgmt.rel.repository.SysUserRoleRepository;
import com.puchain.fep.web.sysmgmt.user.domain.SysUser;
import com.puchain.fep.web.sysmgmt.user.domain.UserStatus;
import com.puchain.fep.web.sysmgmt.user.repository.SysUserRepository;
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
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Callback Phase 2b 端到端集成测试（T16，FR-INFRA-CALLBACK-CREDENTIAL /
 * -DLQ-REPLAY / -IN-APP-ALERT）。
 *
 * <p>验证 Phase 2b 完整链路：OAuth2 凭证 → IDP 取 token → 携带 Bearer 回调行内
 * endpoint → 4xx 直接 DEAD_LETTER → {@code CallbackDeadLetterEvent} 事件解耦 →
 * {@code CallbackAlertEvaluator} → {@code CallbackInAppAlertChannel} 为 ADMIN 用户写 {@code in_app_notification}
 * → admin 复制重放 → 新 PENDING 行（{@code original_dlq_id} 关联）→ 行内修复后
 * 重新投递成功 → DONE。</p>
 *
 * <p>harness 沿用 Phase 1 {@link CallbackEndToEndIT} 约定：JDK {@link HttpServer}
 * 作 mock endpoint（无新依赖），单 server 双 context — {@code /token}（OAuth2 IDP）
 * + {@code /callback}（行内回调）；真实 {@code CallbackQueueRunner} /
 * {@code CallbackCredentialResolver} / {@code CallbackOAuth2ClientCredentialsClient}
 * / {@code CallbackReplayService} 运行。调度中性化（poll 间隔 10 分钟），测试内手动
 * {@link CallbackQueueRunner#poll()}。bank 响应码可变（先 400 触发 DEAD_LETTER，
 * 重放前改 200）。</p>
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
        "management.health.redis.enabled=false"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Callback Phase 2b E2E — OAuth2→callback 4xx→DEAD_LETTER→IN_APP→replay→DONE")
class CallbackPhase2bEndToEndIT {

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
    @Autowired
    private CallbackNotificationRepository notificationRepository;
    @Autowired
    private CallbackReplayService replayService;
    @Autowired
    private SysRoleRepository sysRoleRepository;
    @Autowired
    private SysUserRepository sysUserRepository;
    @Autowired
    private SysUserRoleRepository sysUserRoleRepository;

    /** JDK built-in HTTP server — IDP {@code /token} + bank {@code /callback}. */
    private HttpServer mockServer;
    /** Bank callback response code — mutable so the test can flip 400 → 200 before replay. */
    private final AtomicInteger bankStatus = new AtomicInteger(400);
    /** Authorization headers seen by the bank endpoint (asserts Bearer token attached). */
    private List<String> bankAuthHeaders;
    /** Count of OAuth2 token requests hitting the IDP endpoint. */
    private final AtomicInteger tokenRequestCount = new AtomicInteger(0);
    private String idpTokenUrl;
    private String bankCallbackUrl;

    // seeded ids for targeted cleanup
    private String seededBtId;
    private String seededMsgNoId;
    private String seededInterfaceId;
    private String seededAdminUserId;
    private Long seededUserRoleId;
    private String createdRoleId;

    @BeforeEach
    void setUp() throws IOException {
        bankAuthHeaders = new CopyOnWriteArrayList<>();
        bankStatus.set(400);
        tokenRequestCount.set(0);

        mockServer = HttpServer.create(new InetSocketAddress(0), 0);
        // OAuth2 IDP token endpoint — client_credentials grant.
        mockServer.createContext("/token", exchange -> {
            tokenRequestCount.incrementAndGet();
            try (InputStream is = exchange.getRequestBody()) {
                is.readAllBytes();
            }
            final byte[] body = ("{\"access_token\":\"tok-e2e-1\","
                    + "\"expires_in\":3600,\"token_type\":\"Bearer\"}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        // Bank callback endpoint — records Authorization header, returns the mutable status.
        mockServer.createContext("/callback", exchange -> {
            bankAuthHeaders.add(String.valueOf(exchange.getRequestHeaders().getFirst("Authorization")));
            try (InputStream is = exchange.getRequestBody()) {
                is.readAllBytes();
            }
            exchange.sendResponseHeaders(bankStatus.get(), 0);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write("X".getBytes(StandardCharsets.UTF_8));
            }
        });
        mockServer.start();
        final String base = "http://127.0.0.1:" + mockServer.getAddress().getPort();
        idpTokenUrl = base + "/token";
        bankCallbackUrl = base + "/callback";

        seedAdminUser();
    }

    @AfterEach
    void tearDown() {
        if (mockServer != null) {
            mockServer.stop(0);
        }
        callbackQueueRepository.deleteAll();
        notificationRepository.deleteAll();
        if (seededInterfaceId != null) {
            // 先经 @Transactional 服务删凭证（fk_callback_credential_interface 指向 interface）：
            // 否则删 interface 触发 FK 约束违规致 tearDown 中途抛出，残留 interface/bt/msgNo 污染
            // 后续共享 H2 的 *IT（红线 shared_h2_topn_aggregation_test_isolation）。
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
        if (seededUserRoleId != null) {
            sysUserRoleRepository.findById(seededUserRoleId)
                    .ifPresent(sysUserRoleRepository::delete);
        }
        if (seededAdminUserId != null) {
            sysUserRepository.findById(seededAdminUserId)
                    .ifPresent(sysUserRepository::delete);
        }
        if (createdRoleId != null) {
            sysRoleRepository.findById(createdRoleId)
                    .ifPresent(sysRoleRepository::delete);
        }
    }

    /** Seed (find-or-create) an ADMIN role + a unique admin user linked to it. */
    private void seedAdminUser() {
        final Optional<SysRole> existing = sysRoleRepository.findByRoleCode("ADMIN");
        final String roleId;
        if (existing.isPresent()) {
            roleId = existing.get().getRoleId();
        } else {
            final SysRole role = new SysRole();
            roleId = UUID.randomUUID().toString().replace("-", "");
            role.setRoleId(roleId);
            role.setRoleCode("ADMIN");
            role.setRoleName("E2E Admin");
            role.setRoleType(RoleType.SYSTEM);
            role.setRoleStatus(RoleStatus.ACTIVE);
            sysRoleRepository.save(role);
            createdRoleId = roleId;
        }

        final SysUser admin = new SysUser();
        seededAdminUserId = UUID.randomUUID().toString().replace("-", "");
        admin.setUserId(seededAdminUserId);
        admin.setUserAccount("admin-e2e-" + System.nanoTime());
        admin.setUserName("E2E Admin User");
        admin.setPasswordHash("x");
        admin.setUserStatus(UserStatus.ACTIVE);
        admin.setLoginFailCount(0);
        sysUserRepository.save(admin);

        final SysUserRole link = new SysUserRole(seededAdminUserId, roleId);
        sysUserRoleRepository.save(link);
        seededUserRoleId = link.getId();
    }

    /** Seed an ENABLED business type + msgNo(2103) + OAUTH2 interface → bank callback URL. */
    private void seedOauthInterface() {
        final SysBusinessType bt = new SysBusinessType();
        bt.setTypeId(UUID.randomUUID().toString().replace("-", ""));
        bt.setTypeCode("TC-P2B-" + System.nanoTime());
        bt.setTypeName("Phase2b E2E business type");
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
        iface.setInterfaceName("Phase2b OAuth2 callback interface");
        iface.setInterfaceUrl(bankCallbackUrl);
        iface.setBusinessTypeId(bt.getTypeId());
        iface.setAuthType(InterfaceAuthType.OAUTH2);
        iface.setTimeoutSeconds(5);
        iface.setRetryCount(0);
        iface.setInterfaceStatus(EnableDisableStatus.ENABLED);
        iface.setCallCount(0L);
        subOutputInterfaceRepository.save(iface);

        final CallbackCredentialCreateRequest req = new CallbackCredentialCreateRequest();
        req.setInterfaceId(seededInterfaceId);
        req.setAuthType(InterfaceAuthType.OAUTH2);
        req.setOauthClientId("clid-e2e");
        req.setOauthClientSecret("csec-e2e");
        req.setOauthTokenEndpoint(idpTokenUrl);
        req.setOauthScope("callback");
        credentialAdminService.create(req);
    }

    @Test
    @DisplayName("full chain: OAuth2 cred → bank 4xx → DEAD_LETTER → IN_APP → replay → DONE")
    void fullChain_oauth2_deadLetter_inApp_replay() {
        seedOauthInterface();

        // 1. Bank returns 400 → first send goes straight to DEAD_LETTER (4xx non-retryable).
        bankStatus.set(400);
        final String serialNo = "SER-P2B-" + System.nanoTime();
        eventPublisher.publishEvent(new InboundMessageProcessedEvent(
                MessageType.MSG_2103, "T2100001", serialNo, null, Instant.now()));
        callbackQueueRunner.poll();

        // 2. The IDP was hit (token fetched) and the bank POST carried a Bearer token.
        Assertions.assertThat(tokenRequestCount.get())
                .as("OAuth2 IDP token endpoint should have been called")
                .isGreaterThanOrEqualTo(1);
        Assertions.assertThat(bankAuthHeaders)
                .as("bank should have received exactly one POST")
                .hasSize(1);
        Assertions.assertThat(bankAuthHeaders.get(0))
                .as("bank POST must carry the resolved OAuth2 Bearer token")
                .isEqualTo("Bearer tok-e2e-1");

        // 3. Queue row is DEAD_LETTER.
        final var deadRow = callbackQueueRepository.findAll().stream()
                .filter(r -> "2103".equals(r.getMsgNo()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("dead-letter row for msgNo=2103 must exist"));
        Assertions.assertThat(deadRow.getStatus())
                .as("4xx callback should land in DEAD_LETTER")
                .isEqualTo(CallbackQueueStatus.DEAD_LETTER);
        final String deadQueueId = deadRow.getQueueId();

        // 4. IN_APP notification was created for the seeded admin user.
        final List<CallbackNotificationEntity> notifs =
                notificationRepository.findByUserIdAndReadFalseOrderByCreateTimeDesc(seededAdminUserId);
        Assertions.assertThat(notifs)
                .as("seeded admin should have one unread DLQ notification")
                .hasSize(1);
        final CallbackNotificationEntity notif = notifs.get(0);
        Assertions.assertThat(notif.getCategory()).isEqualTo("CALLBACK_DLQ");
        Assertions.assertThat(notif.getRefId())
                .as("notification refId should point at the dead-letter queue row")
                .isEqualTo(deadQueueId);

        // 5. Bank fixed; admin replays the dead row.
        bankStatus.set(200);
        final DlqReplayResponse replay = replayService.replay(deadQueueId, seededAdminUserId);
        Assertions.assertThat(replay.originalDlqId())
                .as("replay response should link back to the dead-letter row")
                .isEqualTo(deadQueueId);

        // 6. Runner picks up the new PENDING row → succeeds → DONE.
        callbackQueueRunner.poll();
        final var replayed = callbackQueueRepository.findById(replay.newQueueId())
                .orElseThrow(() -> new AssertionError("replayed row must exist"));
        Assertions.assertThat(replayed.getStatus())
                .as("replayed callback should reach DONE after the bank is fixed")
                .isEqualTo(CallbackQueueStatus.DONE);
        Assertions.assertThat(replayed.getOriginalDlqId())
                .as("replayed row must retain the original_dlq_id audit link")
                .isEqualTo(deadQueueId);

        // 7. Original dead row is preserved unchanged (audit evidence).
        final var originalReloaded = callbackQueueRepository.findById(deadQueueId).orElseThrow();
        Assertions.assertThat(originalReloaded.getStatus())
                .as("original dead-letter row must be retained as audit evidence")
                .isEqualTo(CallbackQueueStatus.DEAD_LETTER);
    }
}
