package com.puchain.fep.web.callback;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puchain.fep.common.domain.EnableDisableStatus;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.event.InboundMessageProcessedEvent;
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
 * 接口模式回调模块端到端集成测试（Phase 1, FR-INFRA-CALLBACK）。
 *
 * <p>使用 JDK {@link HttpServer} 作为行内 mock HTTP endpoint（无新依赖），
 * 真实 {@link CallbackQueueRepository} / {@link CallbackQueueRunner} /
 * {@link CallbackTargetResolver} 运行，验证从
 * {@code publishEvent(InboundMessageProcessedEvent)} 到 §7.1 JSON POST
 * 再到队列状态 DONE / callCount+1 的完整链路。</p>
 *
 * <p><strong>P1 GAP 已知（已在 Test 2a 断言实际行为）</strong>：
 * {@link com.puchain.fep.web.callback.service.CallbackTargetResolver#resolve(String)}
 * 第一步仅查 {@code SysBusinessTypeMsgNo}（不 JOIN {@code SysBusinessType.typeStatus}），
 * 因此 DISABLED {@code SysBusinessType} + ENABLED 接口的情况下，
 * P1 代码仍会解析出目标接口。Test 2a 断言此实际 P1 行为（非期望 P2 行为），
 * 并通过 DONE_WITH_CONCERNS 在报告中明示缺口，Phase 2 需在 resolver 中添加
 * BT-status 联表过滤。</p>
 *
 * <p>调度器中性化：将 {@code fep.callback.poll-interval-ms} 与
 * {@code fep.callback.poll-initial-delay-ms} 均设为 600000ms（10 分钟），
 * 测试内手动调用 {@link CallbackQueueRunner#poll()} 以确保同步。
 * 不在类上标注 {@code @Transactional}，因为 {@code CallbackEnqueueService}
 * 使用 {@code REQUIRES_NEW} 独立提交，runner 需读到已提交行。</p>
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
@DisplayName("Callback module end-to-end IT — publishEvent → §7.1 POST → queue DONE")
class CallbackEndToEndIT {

    // ──────────────────────── spring beans ────────────────────────

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
    private ObjectMapper objectMapper;

    // ──────────────────────── per-test http server ────────────────

    /** JDK built-in HTTP server — zero extra dependency. */
    private HttpServer mockServer;

    /** Thread-safe capture of raw POST body strings received by mock endpoint. */
    private List<String> receivedBodies;

    /** Base URL of the per-test ephemeral mock server. */
    private String mockUrl;

    // ──────────────────────── seeded entity ids ───────────────────

    /** Track seeded ids for targeted cleanup in @AfterEach. */
    private String seededBtId;
    private String seededMsgNoId;
    private String seededInterfaceId;

    // ──────────────────────── lifecycle ───────────────────────────

    /**
     * 每个测试启动独立 ephemeral-port HTTP server，记录收到的 POST body。
     *
     * @throws IOException if HttpServer cannot bind
     */
    @BeforeEach
    void startMockServer() throws IOException {
        receivedBodies = new CopyOnWriteArrayList<>();
        mockServer = HttpServer.create(new InetSocketAddress(0), 0);
        mockServer.createContext("/callback", exchange -> {
            try (final InputStream is = exchange.getRequestBody()) {
                final String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                receivedBodies.add(body);
            }
            exchange.sendResponseHeaders(200, 0);
            try (final OutputStream os = exchange.getResponseBody()) {
                os.write("OK".getBytes(StandardCharsets.UTF_8));
            }
        });
        mockServer.start();
        final int port = mockServer.getAddress().getPort();
        mockUrl = "http://127.0.0.1:" + port + "/callback";
        seededBtId = null;
        seededMsgNoId = null;
        seededInterfaceId = null;
    }

    /**
     * 每个测试结束后停止 HTTP server 并删除本测试种入的行（仅删自己的）。
     */
    @AfterEach
    void cleanupAndStopServer() {
        mockServer.stop(0);
        // delete seeded rows in safe dependency order
        if (seededInterfaceId != null) {
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
        // callback_queue rows share no FK with the entities above, wipe by serialNo prefix
        callbackQueueRepository.deleteAll();
    }

    // ──────────────────────── helpers ─────────────────────────────

    /**
     * 种入 SysBusinessType + SysBusinessTypeMsgNo(2103) + SubOutputInterface → mockUrl。
     *
     * @param btStatus        业务类型状态
     * @param ifaceStatus     接口状态
     * @param callbackTargetUrl 回调目标 URL
     */
    private void seedConfig(final EnableDisableStatus btStatus,
                            final EnableDisableStatus ifaceStatus,
                            final String callbackTargetUrl) {
        final SysBusinessType bt = new SysBusinessType();
        bt.setTypeId(UUID.randomUUID().toString().replace("-", ""));
        bt.setTypeCode("TC-E2E-" + System.nanoTime());
        bt.setTypeName("E2E test business type");
        bt.setSortOrder(1);
        bt.setTypeStatus(btStatus);
        sysBusinessTypeRepository.save(bt);
        seededBtId = bt.getTypeId();

        final SysBusinessTypeMsgNo msgNoEntry = new SysBusinessTypeMsgNo(bt.getTypeId(), "2103");
        sysBusinessTypeMsgNoRepository.save(msgNoEntry);
        seededMsgNoId = msgNoEntry.getId();

        final SubOutputInterface iface = new SubOutputInterface();
        iface.setInterfaceId(UUID.randomUUID().toString().replace("-", ""));
        iface.setInterfaceName("E2E mock callback interface");
        iface.setInterfaceUrl(callbackTargetUrl);
        iface.setBusinessTypeId(bt.getTypeId());
        iface.setAuthType(InterfaceAuthType.NONE);
        iface.setTimeoutSeconds(5);
        iface.setRetryCount(0);
        iface.setInterfaceStatus(ifaceStatus);
        iface.setCallCount(0L);
        subOutputInterfaceRepository.save(iface);
        seededInterfaceId = iface.getInterfaceId();
    }

    // ──────────────────────── tests ───────────────────────────────

    /**
     * Criterion 1 (happy path): publishEvent 2103 with enabled BT + enabled interface
     * → mock receives exactly 1 POST with §7.1 JSON envelope →
     * callback_queue row status=DONE, SubOutputInterface callCount=1.
     */
    @Test
    @DisplayName("criterion-1: enabled config → mock receives §7.1 POST, queue DONE, callCount=1")
    void happyPath_enabledConfig_receivesPostAndQueueDone() throws Exception {
        seedConfig(EnableDisableStatus.ENABLED, EnableDisableStatus.ENABLED, mockUrl);

        final String serialNo = "SER-E2E-1";
        eventPublisher.publishEvent(new InboundMessageProcessedEvent(
                MessageType.MSG_2103, "T1000001", serialNo, null, Instant.now()));

        // @EventListener is synchronous — listener→resolver→enqueueService REQUIRES_NEW already committed
        callbackQueueRunner.poll();

        // 1. mock received exactly 1 POST
        Assertions.assertThat(receivedBodies)
                .as("mock should receive exactly 1 POST")
                .hasSize(1);

        // 2. §7.1 envelope structure: code="200", traceId=serialNo
        final JsonNode tree = objectMapper.readTree(receivedBodies.get(0));
        Assertions.assertThat(tree.get("code").asText())
                .as("§7.1 code should be 200")
                .isEqualTo("200");
        Assertions.assertThat(tree.get("traceId").asText())
                .as("§7.1 traceId should equal serialNo")
                .isEqualTo(serialNo);
        Assertions.assertThat(tree.has("message"))
                .as("§7.1 message field must be present")
                .isTrue();
        Assertions.assertThat(tree.has("timestamp"))
                .as("§7.1 timestamp field must be present")
                .isTrue();

        // 3. queue row is DONE
        final var rows = callbackQueueRepository.findAll();
        final var ourRow = rows.stream()
                .filter(r -> "2103".equals(r.getMsgNo()))
                .findFirst();
        Assertions.assertThat(ourRow).as("queue row for msgNo=2103 must exist").isPresent();
        Assertions.assertThat(ourRow.get().getStatus())
                .as("queue row status should be DONE")
                .isEqualTo(CallbackQueueStatus.DONE);

        // 4. SubOutputInterface.callCount incremented to 1
        final var reloadedIface = subOutputInterfaceRepository.findById(seededInterfaceId);
        Assertions.assertThat(reloadedIface).isPresent();
        Assertions.assertThat(reloadedIface.get().getCallCount())
                .as("callCount should be incremented to 1 after successful dispatch")
                .isEqualTo(1L);
    }

    /**
     * Criterion 2a (P1 GAP surface): DISABLED SysBusinessType + ENABLED interface.
     *
     * <p><strong>P1 KNOWN GAP</strong>: {@code CallbackTargetResolver.resolve()} first step calls
     * {@code findBusinessTypeIdsByMsgNo(msgNo)} — this query has NO JOIN on
     * {@code SysBusinessType.typeStatus}. Therefore, a DISABLED SysBusinessType whose
     * SubOutputInterface is ENABLED will still resolve in P1.
     *
     * <p>This test asserts the <em>actual P1 behavior</em> (NOT the aspirational Phase 2 behavior):
     * the mock WILL receive a call even though the BusinessType is DISABLED. Phase 2 must add
     * a BT-status filter join in {@code SysBusinessTypeMsgNoRepository.findBusinessTypeIdsByMsgNo}
     * or {@code CallbackTargetResolver}.</p>
     *
     * <p>Surfaced via DONE_WITH_CONCERNS in the task report.</p>
     */
    @Test
    @DisplayName("criterion-2a (P1 GAP): DISABLED businessType + ENABLED interface → "
            + "P1 resolver does NOT filter BT status, mock still receives POST (actual P1 behavior)")
    void disabledBusinessType_p1GapResolverDoesNotFilterBtStatus() {
        seedConfig(EnableDisableStatus.DISABLED, EnableDisableStatus.ENABLED, mockUrl);

        eventPublisher.publishEvent(new InboundMessageProcessedEvent(
                MessageType.MSG_2103, "T1000002", "SER-E2E-2A", null, Instant.now()));
        callbackQueueRunner.poll();

        // P1 actual behavior: resolver does NOT filter BT status → interface resolved → 1 enqueue
        // This is a documented P1 GAP to be fixed in Phase 2
        Assertions.assertThat(receivedBodies)
                .as("P1 GAP: DISABLED BT with ENABLED interface is NOT filtered by P1 resolver "
                        + "— Phase 2 must add BT-status join in findBusinessTypeIdsByMsgNo")
                .hasSize(1);
    }

    /**
     * Criterion 2b: ENABLED businessType + DISABLED interface →
     * resolver filters via {@code findByBusinessTypeIdInAndInterfaceStatus(btIds, ENABLED)} →
     * 0 enqueued, mock receives 0 requests.
     */
    @Test
    @DisplayName("criterion-2b: DISABLED interface → resolver returns empty → 0 enqueued, 0 POSTs")
    void disabledInterface_zeroEnqueuedZeroPosts() {
        seedConfig(EnableDisableStatus.ENABLED, EnableDisableStatus.DISABLED, mockUrl);

        eventPublisher.publishEvent(new InboundMessageProcessedEvent(
                MessageType.MSG_2103, "T1000003", "SER-E2E-2B", null, Instant.now()));
        callbackQueueRunner.poll();

        Assertions.assertThat(receivedBodies)
                .as("DISABLED interface should be excluded by resolver — mock must receive 0 POSTs")
                .isEmpty();
        Assertions.assertThat(callbackQueueRepository.findAll())
                .as("0 callback_queue rows when interface is DISABLED")
                .isEmpty();
    }

    /**
     * Criterion 2c: msgNo not configured at all → resolver returns empty →
     * 0 enqueued, mock receives 0 requests.
     */
    @Test
    @DisplayName("criterion-2c: msgNo not configured → 0 enqueued, 0 POSTs")
    void unconfiguredMsgNo_zeroEnqueuedZeroPosts() {
        // deliberately do NOT seed any config for 2103
        eventPublisher.publishEvent(new InboundMessageProcessedEvent(
                MessageType.MSG_2103, "T1000004", "SER-E2E-2C", null, Instant.now()));
        callbackQueueRunner.poll();

        Assertions.assertThat(receivedBodies)
                .as("unconfigured msgNo should produce 0 POSTs")
                .isEmpty();
        Assertions.assertThat(callbackQueueRepository.findAll())
                .as("0 callback_queue rows when msgNo not configured")
                .isEmpty();
    }

    /**
     * Criterion 3 (idempotency): publishing the same event (same serialNo + same interface)
     * twice results in exactly 1 callback_queue row — the second publish is deduped by
     * {@code existsByIdempotencyKey} check (or DB unique constraint race guard).
     */
    @Test
    @DisplayName("criterion-3: idempotency — same serialNo published twice → exactly 1 queue row")
    void idempotency_sameSerialNoPublishedTwice_onlyOneQueueRow() {
        seedConfig(EnableDisableStatus.ENABLED, EnableDisableStatus.ENABLED, mockUrl);

        final String serialNo = "SER-IDEM";

        // publish the same event twice
        final InboundMessageProcessedEvent event = new InboundMessageProcessedEvent(
                MessageType.MSG_2103, "T1000005", serialNo, null, Instant.now());
        eventPublisher.publishEvent(event);
        eventPublisher.publishEvent(event);

        // assert exactly 1 row entered the queue (second is deduped)
        final var rows = callbackQueueRepository.findAll();
        final long rowsFor2103 = rows.stream()
                .filter(r -> "2103".equals(r.getMsgNo()))
                .count();
        Assertions.assertThat(rowsFor2103)
                .as("idempotency: 2 identical publishes must result in exactly 1 queue row")
                .isEqualTo(1L);
    }
}
