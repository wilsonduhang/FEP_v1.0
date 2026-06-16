# B-8 Deferred 池清理（DEF-1/2/3/4）Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 清理 B-8 Dashboard WebSocket 实时告警遗留的 4 项 Deferred——**DEF-2** `InMemorySessionRegistry.unregister` 反向索引（O(U)→O(1)）+ **DEF-1** 多实例 `RedisPubSubSessionRegistry`（pub/sub 跨实例广播 + provider-switch bean 装配）+ **DEF-3** 其他「关键告警」源（TLQ 故障 §5.7.3）接入实时推送（🚫 **BLOCKED on B-9**，仅文档化设计缝，无代码）+ **DEF-4** WebSocket E2E Playwright smoke。

**Architecture:** 既有 B-8 推送链不动（`CallbackInAppAlertChannel` → `InAppNotificationCreatedEvent` → `DashboardNotificationPushListener`(AFTER_COMMIT) → `WebSocketSessionRegistry.sendToUser`）。DEF-2 在 `InMemorySessionRegistry` 内加 `sessionId→userId` 反向索引，把 `unregister` 从「遍历所有用户的会话集」降到 O(1) 定位；DEF-1 新增 `RedisPubSubSessionRegistry`（**组合**一份本地 `InMemorySessionRegistry` 持本实例会话 + Redis 频道 `fep:dashboard:ws` 发布/订阅跨实例 `sendToUser`，订阅回调统一投递本地会话，发布者不直接本地投递以避免重复），经**单个** `@Configuration @ConditionalOnProperty(fep.dashboard.ws.registry)` 二选一装配（红线 `provider_switch_impl_no_stereotype_bean_registration`——故须**摘除** `InMemorySessionRegistry` 的 `@Component`）；DEF-3 因 TLQ 故障告警数据源不存在（grep=0，依赖 B-9）保持阻塞，仅记设计缝（B-9 落地后其检测器走既有 in-app 通知链即自动复用本推送，无需新推送基础设施）；DEF-4 加一条 Playwright smoke 验 badge 秒级更新。

**Tech Stack:** Java 17 / Spring Boot 3.x（`spring-boot-starter-data-redis` 已在 fep-web pom:145，`RedisMessageListenerContainer` + `StringRedisTemplate`）/ Spring WebSocket（既有）/ JUnit 5 + Mockito + `@SpringBootTest` / Playwright（`fep-admin-ui/e2e`，既有 `loginAsAdmin` fixture + webServer 自启）。

**执行 Worktree:** `E:\FEP_v1.0_wt-b8-deferred`（分支 `feat/b8-deferred-pool-drain`，触发条件：第 2 项「与已签字未执行 Plan 并存」+ 第 6 项「多会话并发活跃」命中——别会话 `wt-qf1-fixture` 活跃，须隔离；红线 `shared-working-tree-needs-worktree` + `worktree_for_parallel_work`）。

---

## PRD 追溯

| FR-ID | PRD 章节 | 需求 | 本 Plan |
|---|---|---|---|
| FR-WEB-DASH-REFRESH | §5.2.5 | 数据更新机制——实时更新（关键告警）秒级 WebSocket 推送 | DEF-1/DEF-4（推送基础设施健壮性/多实例 + E2E 验收）；DEF-3（其他告警源，🚫 BLOCKED）|
| （工程债，无独立 FR） | — | DEF-2 反向索引性能优化 | DEF-2（`unregister` O(1)）——纯效率重构，行为保持 |

> DEF-2 为 Simplify efficiency deferred 工程项（B8-DEF-2），非新 PRD 需求；DEF-1/DEF-3/DEF-4 服务 FR-WEB-DASH-REFRESH §5.2.5 实时层的健壮性/完备性/验收。

---

## Scope 决策与就绪度披露（muzhou 2026-06-16 已拍板「仍要全 4 项」）

起草前对 4 项逐一 grep 实测就绪度，结论如下；muzhou 在知情下选择**全 4 项纳入**，对 DEF-1 build-ahead 风险与 DEF-3 阻塞占位均已知晓接受：

| 项 | 就绪度 | 实测依据 | 本 Plan 处置 |
|---|---|---|---|
| **DEF-2** 反向索引 | ✅ 完全就绪 | `InMemorySessionRegistry.unregister` 现为 `sessionsByUser.values().forEach(set->set.remove(session))` = O(U) 扫全用户 | **Task 1**：加 `sessionId→userId` 反向索引，行为保持 + 不变量测试 |
| **DEF-1** Redis 注册表 | ⚠️ build-ahead（YAGNI） | `WebSocketSessionRegistry` 接口已是替换缝；**当前零多实例消费者**（部署单实例/sticky-session 即可）；`spring-boot-starter-data-redis` 已在 pom:145 | **Task 2**：实装 + provider-switch（默认仍 memory，redis 须显式开关）；**muzhou 须在签字时显式接受「提前构建无当前消费者的基础设施」** |
| **DEF-3** 其他告警源 | 🚫 BLOCKED | grep `tlq.*fail\|node.*down\|5.7.3\|criticalalert` 全模块 = **0 命中**；告警数据源不存在，依赖 B-9 | **Task 3**：**无代码**，仅文档化设计缝；不计入 gate 未完成项（红线 `plan_gate_must_handle_blocked_state` 用 🚫） |
| **DEF-4** E2E smoke | ✅ 就绪（含成本注记） | `fep-admin-ui/e2e` 有 `playwright.config.ts` + `loginAsAdmin` fixture + 6 既有 smoke spec；webServer 自启 | **Task 4**：加 WS badge 秒级更新 smoke；若 E2E 环境成本（后端+前端+Redis 同启）评估过高则 deferred + 文档兜底（既有 IT + 单测覆盖）|

---

## 背景与既有基础设施实测（复用而非另建）

| 组件 | 既有实测路径 | 本 Plan 关系 |
|---|---|---|
| 会话注册表接口 | `fep-web/.../web/realtime/WebSocketSessionRegistry.java`（`register`/`unregister`/`sendToUser`/`sessionCount`）| 不改接口签名（DEF-1/DEF-2 均在接口契约内）|
| 内存实现 | `fep-web/.../web/realtime/InMemorySessionRegistry.java`（`@Component`，`Map<userId, Set<WebSocketSession>>`，`unregister` O(U)）| DEF-2 改内部加反向索引；DEF-1 摘 `@Component` 改 `@Configuration @Bean` 装配 |
| WS 处理器 | `DashboardWebSocketHandler`（`afterConnectionClosed` → `registry.unregister(session)`；`handleTextMessage` 认证后 `registry.register(userId, session)`）| 不改（仅依赖接口）|
| 推送监听器 | `DashboardNotificationPushListener`（`@TransactionalEventListener(AFTER_COMMIT)` → `registry.sendToUser`）| 不改 |
| Redis 基础设施 | `fep-web/.../config/RedisConfiguration.java` + pom:145 `spring-boot-starter-data-redis` + 既有 `StringRedisTemplate` 消费者（Captcha/LoginAttempt/SSO/JwtAuthFilter）| DEF-1 复用（`RedisMessageListenerContainer` + `StringRedisTemplate`）|
| ConditionalOnProperty 先例 | `TlqInboundConfiguration` / `CallbackStaleReaper`（fep-web 既有 `@ConditionalOnProperty` 装配）| DEF-1 镜像 provider-switch 装配模式 |
| E2E | `fep-admin-ui/e2e/playwright.config.ts`（baseURL `localhost:5173` + webServer 自启）+ `e2e/fixtures/auth.ts`（`loginAsAdmin`）+ `p7.2d-tlq-smoke.spec.ts`（serial describe 模板）| DEF-4 复用 |

**实施前必做 grep（红线 `plan_must_grep_actual_api` + `plan_revision_must_grep_actual_api`）**：① `RedisMessageListenerContainer` 在 fep-web 是否已有 bean（`RedisConfiguration` 内 grep），无则 DEF-1 自建；② `InMemorySessionRegistry` 是否被任何测试以 `new InMemorySessionRegistry()` 直接构造（DEF-2 改内部需同步；已知 `InMemorySessionRegistryTest` 用 `new`）；③ 摘 `@Component` 后确认无 `@Autowired InMemorySessionRegistry`（按具体类注入）的消费者——应全按接口 `WebSocketSessionRegistry` 注入。

### ⚠️ 风险与决策披露（须 muzhou 在 santa-review/签字时确认）

1. **DEF-1 build-ahead（YAGNI）**：实装多实例注册表时**无当前消费者**（部署单实例）。同 NodeStateCache 当初被 muzhou 暂缓的模式。本 Plan 经 muzhou 知情选择纳入；**provider 默认仍 `memory`**（`matchIfMissing=true`），redis 路径须显式 `fep.dashboard.ws.registry=redis` 才激活——即「建好但默认不启用」，零生产行为变更。**muzhou 签字须显式确认接受 build-ahead。**
2. **DEF-1 bean 装配红线**：`InMemorySessionRegistry` 现为 `@Component`；新增第二个 `WebSocketSessionRegistry` 实现若也带 stereotype + 被 `@ComponentScan("com.puchain.fep")` 广扫 → `expected single bean but 2 found` + 可能违反 `NamingConventionTest`（红线 `provider_switch_impl_no_stereotype_bean_registration`）。故**摘除** `InMemorySessionRegistry` 的 `@Component`，两实现统一经单个 `DashboardWebSocketRegistryConfiguration`（`*Configuration` 结尾过命名约束）的 `@Bean @ConditionalOnProperty` 互斥装配。
3. **DEF-1 多实例测试边界**：跨实例 pub/sub 端到端需双 app context，成本高 → 本 Plan 测到 ① `sendToUser` 发布到正确频道（mock/verify）② 订阅回调 `onMessage` → 本地投递 ③ `@SpringBootTest(properties=registry=redis)` 装配选中 redis 实现且 context 起得来（需 Redis；CI 有 `services: redis`，本地需 `fep-redis` 容器）。真双实例广播验证 deferred + 文档记录。
4. **DEF-3 阻塞非可绕过**：TLQ 故障/节点宕机告警**数据源不存在**（grep=0）。不臆造源（臆造红线）。B-9 落地后，其检测器只需把告警写入既有 in-app 通知（经 `CallbackInAppAlertChannel` 或等价），即自动复用本 B-8 推送链——**无需新推送基础设施**。本项 = 文档化此设计缝，无代码 Task。
5. **DEF-4 环境成本**：WS smoke 需后端 dev + 前端 dev + Redis 三者同启。若评估成本过高 → deferred + 文档记录，由后端 `DashboardWebSocketAuthIT`/`DashboardNotificationPushIT` + 前端单测兜底（不阻塞本 Plan 闭环）。
6. **无 DB 变更**：4 项均无 Flyway 迁移（通知表 V32 已存，DEF-1 用 Redis 无库表）。

---

## Task 0：Worktree 隔离 + 基线确认

**Step 1**：`cd /e/FEP_v1.0 && git fetch origin && git rev-parse HEAD origin/main`（确认无 drift，红线 `stale_local_main_worktree` / `baseline_drift_during_long_review_cycle`）。

**Step 2**：`git worktree add -b feat/b8-deferred-pool-drain ../FEP_v1.0_wt-b8-deferred origin/main`。

**Step 3**：实施前 3 grep（见上「实施前必做 grep」）落清单；确认 `fep-redis` 容器可用（DEF-1 IT / DEF-4 依赖）：`docker run -d --name fep-redis -p 6379:6379 redis:7-alpine`（已存则 `docker start fep-redis`）。

> 本 Task 无 commit（仅环境准备）。

---

## Task 1：DEF-2 — `InMemorySessionRegistry` 反向索引（O(U)→O(1)）

**Files:**
- Modify: `fep-web/src/main/java/com/puchain/fep/web/realtime/InMemorySessionRegistry.java`
- Test: `fep-web/src/test/java/com/puchain/fep/web/realtime/InMemorySessionRegistryTest.java`（扩展）

**设计**：新增 `private final Map<String, String> userIdBySessionId = new ConcurrentHashMap<>();`（`sessionId → userId`）。
- `register(userId, session)`：现逻辑 + `userIdBySessionId.put(session.getId(), userId)`。
- `unregister(session)`：`String userId = userIdBySessionId.remove(session.getId()); if (userId != null) { Set<WebSocketSession> set = sessionsByUser.get(userId); if (set != null) { set.remove(session); if (set.isEmpty()) sessionsByUser.remove(userId); } }`——O(1) 定位，**不再遍历全用户**。未注册会话（reverse 无条目）静默忽略（保持幂等契约）。
- `sendToUser` 内**两处惰性丢弃**坏/关闭会话（① `!isOpen()` continue 前 ② `IOException` catch `sessions.remove` 处）均须**同步**清反向索引：`userIdBySessionId.remove(session.getId())`——否则反向索引与正向集漂移（关键不变量，santa v0.1 MINOR-2 实证：两点不可漏一）。

> ⚠️ **不变量（DEF-2 核心）**：任一时刻 `userIdBySessionId` 的 keySet == `sessionsByUser` 所有 value Set 中会话的 sessionId 集合。`register`/`unregister`/`sendToUser` 惰性丢弃三处均须维持。这是反向索引正确性的命脉，须有专测。

**Step 1：写失败测试**（扩展 `InMemorySessionRegistryTest`，新增至少 3 例）：

```java
@Test
void unregister_removesSessionWithoutScanningOtherUsers() {
    final WebSocketSession s1 = openSession("s1");
    final WebSocketSession s2 = openSession("s2");
    registry.register("user-1", s1);
    registry.register("user-2", s2);
    registry.unregister(s1);
    assertThat(registry.sessionCount()).isEqualTo(1); // 仅 s2 残留
}

@Test
void unregister_lastSessionOfUser_dropsUserKey_andReverseIndexConsistent() {
    final WebSocketSession s1 = openSession("s1");
    registry.register("user-1", s1);
    registry.unregister(s1);
    assertThat(registry.sessionCount()).isZero();
    // 重新注册同 sessionId 不应受残留反向条目干扰
    registry.register("user-1", s1);
    assertThat(registry.sessionCount()).isEqualTo(1);
}

@Test
void sendToUser_droppingClosedSession_alsoClearsReverseIndex() throws Exception {
    final WebSocketSession s1 = openSession("s1");
    when(s1.isOpen()).thenReturn(false); // 惰性丢弃路径
    registry.register("user-1", s1);
    registry.sendToUser("user-1", "{}"); // 触发 isOpen()==false 丢弃
    // 反向索引须已清：再 unregister 幂等不抛、count 一致
    registry.unregister(s1);
    assertThat(registry.sessionCount()).isZero();
}

@Test
void unregister_unknownSession_isSilentlyIgnored() {
    final WebSocketSession unknown = openSession("nope");
    registry.unregister(unknown); // 不抛
    assertThat(registry.sessionCount()).isZero();
}

@Test
void sendToUser_ioErrorDroppingSession_alsoClearsReverseIndex() throws Exception {
    // 第二惰性丢弃点：isOpen()==true 但 sendMessage 抛 IOException → catch 分支丢弃
    final WebSocketSession s1 = openSession("s1"); // isOpen()==true
    org.mockito.Mockito.doThrow(new java.io.IOException("boom"))
            .when(s1).sendMessage(any());
    registry.register("user-1", s1);
    registry.sendToUser("user-1", "{}"); // 触发 IOException catch 丢弃
    registry.unregister(s1);             // 反向索引须已清，幂等不抛
    assertThat(registry.sessionCount()).isZero();
}
```

> santa v0.1 MINOR-2 加固：`sendToUser` 有**两个**惰性丢弃点——① `!isOpen()` continue（上 test #3 覆盖）② `IOException` catch `sessions.remove`（line 78，本 test #5 覆盖）。两处均须同步 `userIdBySessionId.remove(session.getId())`，否则反向索引泄漏残留条目。

**Step 2：运行确认 RED**：`cd ../FEP_v1.0_wt-b8-deferred && ./mvnw.cmd -pl fep-web -o test -Dtest=InMemorySessionRegistryTest`（上游 SNAPSHOT 缺则先一次性 `-am install -DskipTests`，红线 `single_module_regression_no_am_flag`）。预期 FAIL。

**Step 3：写最小实现**（按上「设计」改 `InMemorySessionRegistry`）。

**Step 4：运行确认 GREEN**：`./mvnw.cmd -pl fep-web -o test -Dtest=InMemorySessionRegistryTest`，再 `./mvnw.cmd -pl fep-web -o verify`（spotbugs 0：新 Map field 无 EI_EXPOSE 风险；ArchUnit PASS）。

**Step 5：Commit**（独立命令，不链式 verify，红线 `commit_no_chain_with_verify_command`）：
```
git add fep-web/src/main/java/com/puchain/fep/web/realtime/InMemorySessionRegistry.java fep-web/src/test/java/com/puchain/fep/web/realtime/InMemorySessionRegistryTest.java
git commit -m "perf(web): DEF-2 InMemorySessionRegistry reverse index O(U)->O(1) unregister

AI-Generated: claude-code
Reviewed-By: pending"
```

---

## Task 2：DEF-1 — `RedisPubSubSessionRegistry` + provider-switch 装配（build-ahead）

**Files:**
- Modify: `fep-web/.../web/realtime/InMemorySessionRegistry.java`（**摘除** `@Component` + import）
- Create: `fep-web/.../web/realtime/RedisPubSubSessionRegistry.java`（`implements WebSocketSessionRegistry`，**无** stereotype）
- Create: `fep-web/.../web/realtime/DashboardWebSocketRegistryConfiguration.java`（`@Configuration`，`@Bean @ConditionalOnProperty` 二选一 + `RedisMessageListenerContainer`/订阅装配）
- Test: `RedisPubSubSessionRegistryTest.java`（单元：mock `StringRedisTemplate` + 本地 `InMemorySessionRegistry` 组合）
- Test: `DashboardWebSocketRegistryConfigurationIT.java`（`@SpringBootTest`：默认 memory；`properties=fep.dashboard.ws.registry=redis` 选 redis 实现且 context 起得来）

**设计（组合，非继承）**：
```java
public final class RedisPubSubSessionRegistry implements WebSocketSessionRegistry {
    static final String CHANNEL = "fep:dashboard:ws";
    private final InMemorySessionRegistry local;          // 本实例会话与投递逻辑
    private final StringRedisTemplate redisTemplate;      // 发布
    private final ObjectMapper objectMapper;
    // register/unregister/sessionCount → 委托 local
    @Override public void register(String userId, WebSocketSession s) { local.register(userId, s); }
    @Override public void unregister(WebSocketSession s) { local.unregister(s); }
    @Override public int sessionCount() { return local.sessionCount(); }
    // sendToUser → 仅发布到频道；不直接本地投递（避免重复——本实例也订阅该频道）
    @Override public void sendToUser(String userId, String payload) {
        redisTemplate.convertAndSend(CHANNEL, objectMapper.writeValueAsString(
                Map.of("userId", userId, "payload", payload)));  // JsonProcessingException → 包装/记录
    }
    // 订阅回调（由 Configuration 注册的 MessageListener 调用）：解出 {userId,payload} → local.sendToUser
    public void onMessage(String body) { /* parse → local.sendToUser(userId, payload) */ }
}
```
> **不重复投递的关键**：Redis pub/sub 把消息投给**所有**订阅者（含发布者本实例）。故 `sendToUser` 只发布、不本地直投；所有实例（含自己）经 `onMessage` 统一投递本地会话——天然 exactly-once-per-instance，无需 instanceId 去重。

**装配（红线 `provider_switch_impl_no_stereotype_bean_registration`）**：
```java
@Configuration
public class DashboardWebSocketRegistryConfiguration {
    @Bean
    @ConditionalOnProperty(name = "fep.dashboard.ws.registry", havingValue = "memory", matchIfMissing = true)
    WebSocketSessionRegistry inMemorySessionRegistry() { return new InMemorySessionRegistry(); }

    @Bean
    @ConditionalOnProperty(name = "fep.dashboard.ws.registry", havingValue = "redis")
    RedisPubSubSessionRegistry redisPubSubSessionRegistry(StringRedisTemplate t, ObjectMapper om) {
        return new RedisPubSubSessionRegistry(new InMemorySessionRegistry(), t, om);
    }

    @Bean
    @ConditionalOnProperty(name = "fep.dashboard.ws.registry", havingValue = "redis")
    RedisMessageListenerContainer dashboardWsListenerContainer(
            RedisConnectionFactory cf, RedisPubSubSessionRegistry reg) {
        var c = new RedisMessageListenerContainer();
        c.setConnectionFactory(cf);
        c.addMessageListener((message, pattern) ->
                reg.onMessage(new String(message.getBody(), StandardCharsets.UTF_8)),
                new ChannelTopic(RedisPubSubSessionRegistry.CHANNEL));
        return c;
    }
}
```
> ⚠️ 摘 `@Component` 后 `InMemorySessionRegistry` 仅经 `@Bean` 实例化。确认无消费者按**具体类**注入（实施前 grep #3）。`matchIfMissing=true` 保证默认行为零变更（仍 memory）。

**TDD 五步**：
1. 写失败测试：`RedisPubSubSessionRegistryTest`——① `sendToUser` 调 `redisTemplate.convertAndSend(CHANNEL, <json 含 userId+payload>)`（mock verify）；② `onMessage(json)` → 委托 `local.sendToUser`（用真 `InMemorySessionRegistry` + mock open session 验帧送达）；③ register/unregister/sessionCount 委托 local。`DashboardWebSocketRegistryConfigurationIT`——默认 profile bean 类型为 `InMemorySessionRegistry`；`@SpringBootTest(properties="fep.dashboard.ws.registry=redis")` bean 类型为 `RedisPubSubSessionRegistry` 且 context 启动成功。
2. RED：`./mvnw.cmd -pl fep-web -o test -Dtest=RedisPubSubSessionRegistryTest,DashboardWebSocketRegistryConfigurationIT`。
3. 实现 registry + configuration + 摘 `@Component`。
4. GREEN：`./mvnw.cmd -pl fep-web -o verify`（spotbugs 0——`RedisPubSubSessionRegistry` 构造存 `local`/`redisTemplate`/`objectMapper` 引用，若触 EI_EXPOSE_REP2 则**构造器级** `@SuppressFBWarnings`，红线 `spotbugs_check_needs_recompile_after_annotation` + `configurationproperties_collection_getter_ei_expose`；ArchUnit `NamingConventionTest` PASS——`*Configuration` 结尾、无游离 stereotype）。
5. Commit（接口未变 + 两实现 + 装配 + 摘 @Component **同一 commit 自洽可编译**，红线 `commit_tree_self_consistent_per_commit`）：
```
git add fep-web/src/main/java/com/puchain/fep/web/realtime/ fep-web/src/test/java/com/puchain/fep/web/realtime/
git commit -m "feat(web): DEF-1 RedisPubSubSessionRegistry + provider-switch wiring (build-ahead, default memory)

AI-Generated: claude-code
Reviewed-By: pending"
```

> ⚠️ **DEF-1 build-ahead 标注**：本 Task 默认不改生产行为（`matchIfMissing=true` → memory）。redis 路径须 muzhou 部署多实例时显式开启。Plan 签字须含对 build-ahead 的显式接受。

---

## Task 3：DEF-3 — 其他「关键告警」源接入实时推送 🚫 BLOCKED（文档化设计缝，无代码）

**状态：🚫 BLOCKED on B-9**（不计入 gate 未完成项，红线 `plan_gate_must_handle_blocked_state`）。

**实测阻塞依据**：全模块 `grep -iE "tlq.*fail|node.*down|5.7.3|criticalalert|critical_alert"` = **0 命中**。TLQ 故障/节点宕机告警的**检测与数据源不存在**，属 B-9 范畴。无源可接，**禁臆造**（臆造红线）。

**设计缝（B-9 落地后的接入路径，本 Plan 仅记录不实现）**：
- B-9 实装 TLQ 故障检测器后，令其将「关键告警」写入既有站内通知（经 `CallbackInAppAlertChannel.send(...)` 或对等落库点，扇出到 ADMIN）。
- 既有 B-8 链 `InAppNotificationCreatedEvent`(AFTER_COMMIT) → `DashboardNotificationPushListener` → `registry.sendToUser` **自动复用**——**无需新增任何推送基础设施**。
- 即：DEF-3 的「推送侧」B-8 已提前铺好；缺的只是「告警源侧」（B-9）。

**产物**：仅在 session-end 8 维技术文档 `modules` 维度补一节「DEF-3 设计缝 + B-9 依赖」；**无代码、无测试、无 commit**。

---

## Task 4：DEF-4 — WebSocket badge 秒级更新 E2E smoke

**Files:**
- Create: `fep-admin-ui/e2e/b8-ws-realtime-smoke.spec.ts`
- （可能）Modify: `fep-admin-ui/e2e/fixtures/`（若需触发一条通知的后端 test hook helper）

**设计**：镜像 `p7.2d-tlq-smoke.spec.ts`（serial describe + `loginAsAdmin`）。流程——登录 ADMIN → 进首页 → WS 连接建立（断言 `notification-store.subscribeRealtime` 已订阅/连接态）→ 触发一条 in-app 通知（优先后端 test hook/REST；若无则文档记录该子断言 deferred）→ 断言 `NotificationBell` badge 在**秒级**内更新（非等 30s 轮询，用 `expect.poll` 短超时 ≤5s 而非 ≥30s 窗口区分实时 vs 轮询）。

**Step 1**：写 spec（先红——选择器/hook 未就位）。
**Step 2**：`cd fep-admin-ui && pnpm exec playwright test b8-ws-realtime-smoke.spec.ts`（需后端 dev,dev-e2e + 前端 dev + Redis 同启）确认 RED。
**Step 3**：补齐选择器/test hook 使 GREEN。
**Step 4**：`pnpm exec playwright test b8-ws-realtime-smoke.spec.ts` GREEN。
**Step 5**：Commit `test(e2e): DEF-4 WebSocket badge realtime-update smoke`（footer 同上）。

> ⚠️ **环境成本闸**（红线 #5）：若 WS E2E 三件套同启在本机/CI 成本过高或不稳，**本 Task 可 deferred**——三重记录（Plan 标注 + Daily Report + 衔接提示词）+ muzhou 拍板，由后端 `DashboardWebSocketAuthIT`/`DashboardNotificationPushIT` + 前端 `dashboardWsClient` 单测兜底。deferred 不阻塞 DEF-1/DEF-2 闭环。

---

## Task 5（closing）：回归 + PRD 矩阵 + worktree 闭环

- **回归**（红线 `single_module_regression_no_am_flag` + `plan_regression_scope_explicit`）：
  - **minimum**：`./mvnw.cmd -pl fep-web -o verify`（上游缺则先一次性 `-am install -DskipTests`）fep-web 0 fail + spotbugs 0 + ArchUnit PASS；DEF-4 若实装则 `pnpm exec playwright test b8-ws-realtime-smoke.spec.ts` 绿。
  - **strong**：PR → GHA Build/Test/Quality 全绿（本地非权威，铁律 #3；含两个既有 WS IT + 新 `DashboardWebSocketRegistryConfigurationIT` 须 GHA 跑——本地 failsafe 不绑/Redis 依赖）。**若 GHA billing 仍阻塞** → tier-A 充分 + muzhou admin override（红线 `systemic_ci_blocker_defers_positive_backing`），billing 恢复后补背书。
- **PRD 矩阵**（docs 仓 `E:\FEP`）：FR-WEB-DASH-REFRESH §5.2.5 实时层注记更新——DEF-1 多实例能力 build-ahead（默认 memory）/ DEF-4 E2E 状态 / DEF-3 仍 🚫 BLOCKED on B-9。
- **worktree teardown**：merge 后 `git worktree remove ../FEP_v1.0_wt-b8-deferred`（红线闭环纪律）。

---

## 质量门禁自检（9 项，每 Task commit 前）

1. 无吞异常（DEF-1 `onMessage`/序列化异常记录非吞；reverse-index 一致性不靠吞错）2. 测试验业务含义（DEF-2 反向索引一致性不变量 / DEF-1 发布频道+委托+装配选择，非假断言）3. 边界（DEF-2：未注册会话/惰性丢弃/最后一条；DEF-1：默认 memory/redis 切换/无订阅者）4. 日志无敏感数据（沿用既有 LogSanitizer + `@SuppressFBWarnings(CRLF_INJECTION_LOGS)`）5. 无未用抽象（DEF-1 组合非继承，无冗余层）6. 无硬编码（频道名常量、registry 开关走 property、超时走既有）7. Javadoc 完整（新类/公共方法）8. 无 System.out/console 残留 9. 风格一致（DEF-1 镜像既有 `RedisConfiguration`/`TlqInboundConfiguration` 装配；DEF-2 镜像既有 registry 风格）。

## 评审与签字

- 每 Task spec+quality review（只读静态，**禁 mvn/mvnw**，红线 `review_subagent_must_not_run_mvn`）；全 Task 后 final whole-impl review（逐 commit 自洽，红线 `commit_tree_self_consistent_per_commit`）。
- 长跑 mvn → hybrid（主对话实施 + 前台 mvn + commit / subagent 仅评审，红线 `harness_bg_detach_hybrid_default`）；implementer 不设 `model` override（红线 `subagent_model_override_auth_fragility`）。
- DEF-1 触 Redis + bean 装配 + 安全无关（无 auth 改动）→ 常规 santa 双审，无须密码学专项。
- 本 Plan 须 santa-method 独立评审 + **muzhou 签字（含对 DEF-1 build-ahead 的显式接受）** 后方可执行。

### 评审与签字记录

| 角色 | 谁 | 结论 | 日期 |
|---|---|---|---|
| Plan 作者 | Claude Code (mode A) | 起草 v0.1（4 项就绪度逐一 grep 实测；DEF-2 ✅ / DEF-1 ⚠️build-ahead / DEF-3 🚫BLOCKED 无代码 / DEF-4 ✅含成本闸）→ v0.2（折叠 santa MINOR-2：DEF-2 加 IOException 惰性丢弃路径反向索引清理 test #5）| 2026-06-16 |
| santa 评审 | Claude Code (santa-method) | ✅ PASS-WITH-MINOR（无 BLOCKER；15 条事实声明逐条 grep 实测全 TRUE：unregister O(U) `InMemorySessionRegistry.java:50-54`、@Component `:32`、redis starter `pom.xml:145`、@ConditionalOnProperty 先例 RedisConfiguration/TlqInboundConfiguration、消费者全按接口注入、DEF-3 grep=0 无源、pub/sub 无重复投递设计 sound、E2E fixture 齐备、PRD §5.2.5 追溯属实；2 MINOR：M-2 IOException 丢弃路径测试已 v0.2 加固，M-11/13 两 commit 同文件/grep 范围属非问题）| 2026-06-16 |
| **Plan 批准者** | **muzhou** | **✅ APPROVED**（全批准 4 项 + **显式接受 DEF-1 build-ahead**：默认 memory 零生产变更、redis 须显式开关；DEF-3 🚫BLOCKED 仅文档化；DEF-4 含成本闸）| 2026-06-16 |
