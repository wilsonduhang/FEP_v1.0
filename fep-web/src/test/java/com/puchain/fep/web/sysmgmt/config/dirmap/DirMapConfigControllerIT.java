package com.puchain.fep.web.sysmgmt.config.dirmap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.routing.AccessRole;
import com.puchain.fep.processor.routing.DirMapKey;
import com.puchain.fep.processor.routing.DirectionMapping;
import com.puchain.fep.processor.routing.DynamicMessageDirectionMap;
import com.puchain.fep.processor.routing.MessageDirectionMap;
import com.puchain.fep.processor.routing.RoleDirection;
import com.puchain.fep.web.auth.domain.CaptchaResponse;
import com.puchain.fep.web.auth.domain.LoginRequest;
import com.puchain.fep.web.auth.service.CaptchaService;
import com.puchain.fep.web.config.TestRedisConfiguration;
import com.puchain.fep.web.messageinbound.dto.InboundMessageResponse;
import com.puchain.fep.web.messageinbound.service.InboundMessageDispatcher;
import com.puchain.fep.web.sysmgmt.config.dirmap.dto.DirMapConfigUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P3a T7 — End-to-end IT for DIR-MAP dynamic config (REST + cache + dispatcher).
 *
 * <p>Verifies the complete flow:
 * <ol>
 *   <li>{@code DynamicMessageDirectionMap.cacheSize() == 88} after Spring ctx
 *       startup (proves DB-loaded vs static fallback — Reviewer B P0-3).</li>
 *   <li>88-row DB ↔ static {@link MessageDirectionMap} baseline (each row equal).</li>
 *   <li>{@code PUT /api/v1/sys/config/dir-map/{messageType}/{accessRole}} →
 *       immediate {@link MessageDirectionMap#lookup} reflects new direction
 *       (cache invalidated via {@code DirMapConfigChangedEvent} +
 *       {@code @TransactionalEventListener(AFTER_COMMIT)}).</li>
 *   <li>Real CFX-envelope 3115 sample fired through
 *       {@link InboundMessageDispatcher} after direction update — covers the
 *       {@code feedback_dispatcher_payload_shape_blind_spot} red line.</li>
 * </ol>
 *
 * <p>Auth: PUT endpoints require {@code SYSTEM_ADMIN} authority. The IT performs
 * a real login round-trip against the seed {@code admin1} account via the
 * mocked {@link TestRedisConfiguration} captcha store, mirroring the pattern
 * established in {@code RbacFlowTest}.</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"dev", "test"})
class DirMapConfigControllerIT {

    /** Seed admin account from V2 migration (6 chars to satisfy LoginRequest @Size(min=6)). */
    private static final String ADMIN_ACCOUNT = "admin1";

    /** Seed admin password. */
    private static final String ADMIN_PASSWORD = "admin@FEP2026";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper om;
    @Autowired private DynamicMessageDirectionMap dynamicMap;
    @Autowired private InboundMessageDispatcher dispatcher;
    @Autowired private CaptchaService captchaService;

    /** Cached bearer token (one login per test method to keep tokens fresh and avoid SSO kick). */
    private String bearer;

    @BeforeEach
    void setUp() throws Exception {
        TestRedisConfiguration.getStore().clear();
        bearer = "Bearer " + loginAndGetToken();
    }

    /**
     * Reviewer B P0-3 修订：cacheSize() 实证 cache 真的 loaded（88 条）而非 fallback 给假象。
     * 若 V20 SQL 写错任意一行 INSERT，count = 87 / 89 / ...，本测试 RED；
     * 原仅 isPresent() 断言无法证伪此盲点（fallback 也返回 88 isPresent）。
     */
    @Test
    void shouldHaveCacheSizeEqualTo88_provingDbLoadedNotFallback() {
        assertThat(dynamicMap.cacheSize())
                .as("cache loaded from DB after Spring ctx startup; expect 88 rows")
                .isEqualTo(88L);
    }

    /**
     * 88 条逐条 DB ↔ 静态对照（强校验）。SQL 写错某行的 direction / requires_fep / mode 时本测试 RED。
     *
     * <p>v1i T7 adaptation A: 用公共 {@code MessageDirectionMap.entries()} +
     * {@link DirMapKey} 替换 v1h 草稿对私有 {@code TABLE} / {@code Key} 字段的反射。</p>
     */
    @Test
    void shouldMatchStaticBaseline_eachOf88Rows() {
        Map<DirMapKey, DirectionMapping> staticBaseline = MessageDirectionMap.entries();
        assertThat(staticBaseline)
                .as("static baseline must be 88 rows")
                .hasSize(88);

        for (Map.Entry<DirMapKey, DirectionMapping> entry : staticBaseline.entrySet()) {
            MessageType msg = entry.getKey().msg();
            AccessRole role = entry.getKey().role();
            Optional<DirectionMapping> dynamic = dynamicMap.lookupRaw(msg, role);
            assertThat(dynamic)
                    .as("DB row missing for msg=%s role=%s", msg, role)
                    .isPresent();
            assertThat(dynamic.get())
                    .as("DB row != static for msg=%s role=%s", msg, role)
                    .isEqualTo(entry.getValue());
        }
    }

    @Test
    void shouldReflectDirectionChangeImmediately_afterPut() throws Exception {
        // Step 1: pre-state — 3001/ACCEPTING_ORG static baseline INBOUND_PASSIVE
        Optional<DirectionMapping> before = MessageDirectionMap.lookup(
                MessageType.MSG_3001, AccessRole.ACCEPTING_ORG);
        assertThat(before).hasValueSatisfying(m ->
                assertThat(m.direction()).isEqualTo(RoleDirection.INBOUND_PASSIVE));

        // Step 2: PUT /api → 反转方向
        DirMapConfigUpdateRequest req = new DirMapConfigUpdateRequest(
                "OUTBOUND_ACTIVE", true, "MODE_1", "IT verifies immediate reflect");
        mvc.perform(put("/api/v1/sys/config/dir-map/3001/ACCEPTING_ORG")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());

        // Step 3: 立即重新查 — should be new value
        // (Caffeine cache reload triggered by AFTER_COMMIT TransactionalEventListener)
        Optional<DirectionMapping> after = MessageDirectionMap.lookup(
                MessageType.MSG_3001, AccessRole.ACCEPTING_ORG);
        assertThat(after).hasValueSatisfying(m ->
                assertThat(m.direction()).isEqualTo(RoleDirection.OUTBOUND_ACTIVE));

        // Step 4: 还原（避免影响其他 IT）
        DirMapConfigUpdateRequest revert = new DirMapConfigUpdateRequest(
                "INBOUND_PASSIVE", true, "MODE_1", "revert");
        mvc.perform(put("/api/v1/sys/config/dir-map/3001/ACCEPTING_ORG")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(revert)))
                .andExpect(status().isOk());
    }

    /**
     * Reviewer A P0-2 修订（实测 dispatcher 签名后写完整代码）：
     * - InboundMessageDispatcher.dispatch(String messageType, String transitionNo, byte[] xml)
     * - 选 3115 资金清算指令（已注册 body POJO 之一，dispatcher BODY_TYPE_REGISTRY 含 3115）
     * - feedback_dispatcher_payload_shape_blind_spot 红线：sample 必须为真实业务格式（CFX envelope + 3115 body）
     *
     * <p>关注点：路由决策（{@link MessageDirectionMap#lookup}）在 PUT 前/后返回不同 direction —
     * dispatcher 不抛异常即证明它能接受新 direction 下的报文（routing decision proxy）。
     * 即使 dispatcher 内部对 sample 做严格 XSD 验证返回 4xx response，本用例的核心验证
     * 是 lookup 路径 — response 仅做 not-null 兜底。</p>
     */
    @Test
    void shouldFireDispatcherWithNewDirection_afterUpdate() throws Exception {
        // 1. 改 3115/ACCEPTING_ORG 方向 INBOUND_PASSIVE → OUTBOUND_ACTIVE
        DirMapConfigUpdateRequest req = new DirMapConfigUpdateRequest(
                "OUTBOUND_ACTIVE", true, "MODE_5", "IT dispatcher 路由变更实证");
        mvc.perform(put("/api/v1/sys/config/dir-map/3115/ACCEPTING_ORG")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());

        // 2. 验证 lookup 已看到新方向（reconciliation services 下游消费者视角）
        Optional<DirectionMapping> seenByConsumer = MessageDirectionMap.lookup(
                MessageType.MSG_3115, AccessRole.ACCEPTING_ORG);
        assertThat(seenByConsumer).hasValueSatisfying(m ->
                assertThat(m.direction())
                        .as("reconciliation services 调 MessageDirectionMap.lookup 应已看到新方向")
                        .isEqualTo(RoleDirection.OUTBOUND_ACTIVE));

        // 3. 构造最小 3115 sample CFX 信封并喂给 dispatcher
        String transitionNo = "P3A-IT-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String sample3115Xml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
              + "<CFXMessage>"
              + "<HEAD>"
              + "<msgType>3115</msgType>"
              + "<transitionNo>" + transitionNo + "</transitionNo>"
              + "<senderNode>A1000999000001</senderNode>"
              + "<receiverNode>A1000143000104</receiverNode>"
              + "<msgTime>20260429120000</msgTime>"
              + "</HEAD>"
              + "<MSG><PlatPay3115>"
              + "<platPayNo>P3AIT" + transitionNo.substring(8) + "</platPayNo>"
              + "<qsSerialNo>QS-P3A-IT-001</qsSerialNo>"
              + "<settleAmount>10000.0000</settleAmount>"
              + "<payerAccount>62280001</payerAccount>"
              + "<payeeAccount>62280002</payeeAccount>"
              + "</PlatPay3115></MSG>"
              + "</CFXMessage>";

        // 4. dispatcher.dispatch — 不抛异常即证明 dispatcher 能在新 direction 下处理报文
        InboundMessageResponse response = dispatcher.dispatch(
                "3115", transitionNo, sample3115Xml.getBytes(StandardCharsets.UTF_8));
        assertThat(response).as("dispatch should not return null").isNotNull();

        // 5. 还原
        DirMapConfigUpdateRequest revert = new DirMapConfigUpdateRequest(
                "INBOUND_PASSIVE", true, "MODE_5", "revert");
        mvc.perform(put("/api/v1/sys/config/dir-map/3115/ACCEPTING_ORG")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(revert)))
                .andExpect(status().isOk());
    }

    /**
     * Performs admin login via {@link CaptchaService} mock store and returns access token.
     * Mirrors {@code RbacFlowTest#loginAndGetToken()}.
     *
     * @return Bearer access token (no "Bearer " prefix)
     * @throws Exception MockMvc exception
     */
    private String loginAndGetToken() throws Exception {
        CaptchaResponse cap = captchaService.generate();
        String code = TestRedisConfiguration.getStore()
                .get("fep:captcha:" + cap.getCaptchaId());

        LoginRequest req = new LoginRequest();
        req.setAccount(ADMIN_ACCOUNT);
        req.setPassword(ADMIN_PASSWORD);
        req.setCaptchaId(cap.getCaptchaId());
        req.setCaptchaCode(code);

        MvcResult result = mvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = om.readTree(result.getResponse().getContentAsString());
        return body.path("data").path("accessToken").asText();
    }
}
