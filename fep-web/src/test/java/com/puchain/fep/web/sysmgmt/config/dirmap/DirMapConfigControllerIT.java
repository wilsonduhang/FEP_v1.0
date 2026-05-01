package com.puchain.fep.web.sysmgmt.config.dirmap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.routing.AccessRole;
import com.puchain.fep.processor.routing.DirMapConfigStore;
import com.puchain.fep.processor.routing.DirMapConfigUpdate;
import com.puchain.fep.processor.routing.DirMapKey;
import com.puchain.fep.processor.routing.DirectionMapping;
import com.puchain.fep.processor.routing.DynamicMessageDirectionMap;
import com.puchain.fep.processor.routing.MessageDirectionMap;
import com.puchain.fep.processor.routing.RoleDirection;
import com.puchain.fep.web.auth.domain.CaptchaResponse;
import com.puchain.fep.web.auth.domain.LoginRequest;
import com.puchain.fep.web.auth.service.CaptchaService;
import com.puchain.fep.web.config.TestRedisConfiguration;
import com.puchain.fep.web.messageinbound.service.InboundMessageDispatcher;
import com.puchain.fep.web.sysmgmt.config.dirmap.dto.DirMapConfigUpdateRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
 *   <li>Real CFX-envelope 3115 sample (loaded from
 *       {@code samples/3115-valid.xml}) fired through
 *       {@link InboundMessageDispatcher}; asserts the listener-thrown
 *       {@code FepBusinessException("orphan 3115 return: ... PLATPAY3115001")}
 *       via {@code assertThatThrownBy} (the precise exception type + message
 *       proves the chain dispatcher → XSD → body POJO 解析 → publishEvent →
 *       ClearingInstructionEventListener.onProcessed → cast → qsReturnInfo
 *       judgement → ClearingInstructionService.processInboundReturn is fully
 *       wired). Covers the {@code feedback_dispatcher_payload_shape_blind_spot}
 *       red line by using a real CFX envelope, not a hand-rolled fictitious one.</li>
 * </ol>
 *
 * <p><b>Test isolation</b>（T7 quality reviewer P0-1 / P1-1 / P1-3 修复
 * 2026-05-01）：H2 dev profile {@code DB_CLOSE_DELAY=-1} 让 t_dir_map_config
 * 跨 {@code @SpringBootTest} ctx 持久化，故每个 mutate test 必须 try/finally
 * 还原；@AfterEach 双保险还原 + clearForTest；@BeforeEach 强制 baseline
 * preconditions（防 sibling 漂移污染入侵本 IT）。</p>
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

    /** V20 static baseline for MSG_3001/ACCEPTING_ORG. */
    private static final RoleDirection BASELINE_3001_DIR = RoleDirection.INBOUND_PASSIVE;

    /** V20 static baseline for MSG_3115/ACCEPTING_ORG. */
    private static final RoleDirection BASELINE_3115_DIR = RoleDirection.INBOUND_PASSIVE;

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper om;
    @Autowired private DynamicMessageDirectionMap dynamicMap;
    @Autowired private InboundMessageDispatcher dispatcher;
    @Autowired private CaptchaService captchaService;
    @Autowired private DirMapConfigStore store;

    /** Cached bearer token (one login per test method to keep tokens fresh and avoid SSO kick). */
    private String bearer;

    @BeforeEach
    void setUp() throws Exception {
        TestRedisConfiguration.getStore().clear();
        // T7 quality reviewer P1-3 修复：强制 baseline preconditions —
        // 防 sibling test 漂移（如 JpaDirMapConfigStoreIT 旧版未 revert）污染本 IT。
        forceBaselineFor(MessageType.MSG_3001, AccessRole.ACCEPTING_ORG);
        forceBaselineFor(MessageType.MSG_3115, AccessRole.ACCEPTING_ORG);
        bearer = "Bearer " + loginAndGetToken();
    }

    @AfterEach
    void tearDown() {
        // T7 quality reviewer P0-1 修复：双保险还原（即使 try/finally 失效）。
        // 注意：本 IT *不* 调 MessageDirectionMapBridge.clearForTest() — 与
        // JpaDirMapConfigStoreIT 不同。原因：本 ctx 跨 @Test 复用（无 @DirtiesContext），
        // dynamicMap bean 仅在 Spring ctx 启动期 setDynamic 一次；@AfterEach 清空后
        // sibling test 调 MessageDirectionMap.lookup 会 fallback 到静态 88 行，
        // 直接遮蔽 dynamic cache 的真实状态（→ 测试 false-RED）。
        // JpaDirMapConfigStoreIT 用 @DirtiesContext(AFTER_CLASS)，整 ctx 关掉重启，
        // 故必须显式 clear 防 static field 残留指向已停 ctx 的 bean。本 IT 不需要。
        forceBaselineFor(MessageType.MSG_3001, AccessRole.ACCEPTING_ORG);
        forceBaselineFor(MessageType.MSG_3115, AccessRole.ACCEPTING_ORG);
        // 还原后强制 reload cache，让下一个 test 看到 baseline 而非上一个 test 的 mutation
        // 残留（store.update 直调 Adapter 不发 event，cache 未自动 invalidate）
        dynamicMap.reload();
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
                assertThat(m.direction()).isEqualTo(BASELINE_3001_DIR));

        // Step 2: PUT /api → 反转方向（OUTBOUND_ACTIVE）
        DirMapConfigUpdateRequest req = new DirMapConfigUpdateRequest(
                "OUTBOUND_ACTIVE", true, "MODE_1", "IT verifies immediate reflect");
        mvc.perform(put("/api/v1/sys/config/dir-map/3001/ACCEPTING_ORG")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk());

        // Step 3: 立即重新查 — should be new value
        // (Caffeine cache reload triggered synchronously by AFTER_COMMIT
        //  TransactionalEventListener — Spring 默认在 commit 后同步 dispatch event，
        //  HTTP response 等到 commit 完成才返回，故 isOk() 通过后 cache 必已 reload)
        Optional<DirectionMapping> after = MessageDirectionMap.lookup(
                MessageType.MSG_3001, AccessRole.ACCEPTING_ORG);
        assertThat(after).hasValueSatisfying(m ->
                assertThat(m.direction()).isEqualTo(RoleDirection.OUTBOUND_ACTIVE));

        // Step 4: 还原由 @AfterEach 兜底（双保险），本步骤显式还原以保留中间状态可读性
        DirMapConfigUpdateRequest revert = new DirMapConfigUpdateRequest(
                BASELINE_3001_DIR.name(), true, "MODE_1", "revert");
        mvc.perform(put("/api/v1/sys/config/dir-map/3001/ACCEPTING_ORG")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(revert)))
                .andExpect(status().isOk());
    }

    /**
     * T7 quality reviewer P0-2 修复（2026-05-01）：替换手写 fictitious 信封为真实
     * {@code samples/3115-valid.xml}（uppercase {@code <CFX>} / {@code <PlatPayNo>} /
     * {@code <SrcNode>} 等），让 SyncMessageProcessorService XSD 校验通过 → listener
     * 真实触发，从而验证 dispatcher 能在新 direction 下处理报文。
     *
     * <p>断言策略（T7 quality fixup round-2 / 2026-05-01）：3115-valid 样本含
     * {@code <qsReturnInfo>} → listener 走 {@link com.puchain.fep.processor.reconciliation.ClearingInstructionService#processInboundReturn}
     * 路径，而 H2 测试库无对应前置 PENDING clearing 行 → "orphan 3115 return"
     * {@link FepBusinessException}。<b>这恰恰证明链路完整通达：dispatcher → XSD →
     * body POJO 解析 → publishEvent → listener.onProcessed → service 业务校验</b>。
     * 用 {@link org.assertj.core.api.Assertions#assertThatThrownBy} 期望此精确异常，
     * 比"非 FAILED"强得多 — 后者无法证明 listener 是否触达 service 层。</p>
     *
     * <p>设计权衡：方案 B（pre-seed clearing 行）能让 listener 业务成功，但需引入
     * {@code ClearingInstructionRecordRepository} 依赖 + 跨表 cleanup，且测试关注点
     * 偏移到 reconciliation 域。本 IT 关注点为 dirmap 动态配置 + dispatcher 路由触发，
     * 不涉及 reconciliation 业务正确性（已由 P3 ReconciliationE2EIntegrationTest 覆盖）。
     * 故选择方案 A：用 expected exception 锚定 listener 触达 service 层即可。</p>
     *
     * <p>{@code feedback_dispatcher_payload_shape_blind_spot} 红线：sample 必须为
     * 真实 CFX envelope + 真实 3115 body POJO，不得手写残缺字段。</p>
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

        // 2. 验证 lookup 已看到新方向（reconciliation services 下游消费者视角 —
        //    dispatcher 自身不读 dirmap，但 reconciliation services 会调 lookup）
        Optional<DirectionMapping> seenByConsumer = MessageDirectionMap.lookup(
                MessageType.MSG_3115, AccessRole.ACCEPTING_ORG);
        assertThat(seenByConsumer).hasValueSatisfying(m ->
                assertThat(m.direction())
                        .as("reconciliation services 调 MessageDirectionMap.lookup 应已看到新方向")
                        .isEqualTo(RoleDirection.OUTBOUND_ACTIVE));

        // 3. 加载真实 CFX envelope sample（uppercase tags + complete schema）
        ClassPathResource samplePath = new ClassPathResource("samples/3115-valid.xml");
        byte[] sampleXml = samplePath.getInputStream().readAllBytes();
        // sample 中 BatchHead3115/TransitionNo == "00000111"，与 dispatcher 入参解耦
        String transitionNo = "00000111";

        // 4. dispatcher.dispatch 真实跑链路 — 期望 ClearingInstructionService.processInboundReturn
        //    抛 "orphan 3115 return"（无 pre-seed clearing 行）。该精确异常证明：
        //    XSD 通过 → body POJO 解析成功 → event 发布 → listener.onProcessed 触发 →
        //    PlatPay3115 cast 通过 → qsReturnInfo 非空判定为 isInboundReturn → service 业务层。
        //    任一环节断链则异常类型 / message 不同。
        assertThatThrownBy(() -> dispatcher.dispatch("3115", transitionNo, sampleXml))
                .as("dispatcher → listener → service 链路完整时必抛 orphan return；"
                        + "若是 FepBusinessException(MSG_INBOUND_DECODE_FAILURE) 则 body 解析失败；"
                        + "若是 IllegalStateException 则 cast 失败；其他类型/message 即链路断点")
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("orphan 3115 return")
                .hasMessageContaining("PLATPAY3115001");

        // 5. 还原由 @AfterEach 兜底；显式还原保留中间态可读
        DirMapConfigUpdateRequest revert = new DirMapConfigUpdateRequest(
                BASELINE_3115_DIR.name(), true, "MODE_5", "revert");
        mvc.perform(put("/api/v1/sys/config/dir-map/3115/ACCEPTING_ORG")
                        .header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(revert)))
                .andExpect(status().isOk());
    }

    /**
     * 强制把指定 (msgType, accessRole) 还原到 V20 静态基线 — 从
     * {@link MessageDirectionMap#entries()} 取真权威值（direction / requiresFep /
     * mode），保证 baseline 测试的 DB↔static 等价不变性。直调
     * {@link DirMapConfigStore#update} 跳过权限层与 history 写入；幂等。
     *
     * <p>历史踩坑：之前硬编码 {@code MODE_1} 兜底，{@link MessageType#MSG_3115}
     * 实际 baseline 为 {@code MODE_5}，统一回 MODE_1 会让 {@code shouldMatchStaticBaseline}
     * 测试 RED。改用 entries() 取每行真值。</p>
     *
     * @param msg  报文类型
     * @param role 接入角色
     */
    private void forceBaselineFor(MessageType msg, AccessRole role) {
        DirectionMapping baseline = MessageDirectionMap.entries()
                .get(new DirMapKey(msg, role));
        if (baseline == null) {
            throw new IllegalStateException(
                    "No static baseline for " + msg + "/" + role);
        }
        store.update(new DirMapConfigUpdate(
                msg, role,
                baseline.direction(), baseline.requiresFep(), baseline.mode(),
                "system"));
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
