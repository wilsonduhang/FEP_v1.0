# B-8 Dashboard 关键告警 WebSocket 实时推送 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 为管理 Web 首页「关键告警信息」新增 WebSocket 秒级实时推送（PRD §5.2.5 实时更新层），复用既有 in-app 通知系统（`CallbackNotificationService` / `CallbackInAppAlertChannel` / 前端 `NotificationBell`）——新通知创建时经 WebSocket 即时推送到对应用户的 `NotificationBell`，无需等 30s 轮询。**常规统计 30s 轮询保持不变**（PRD §5.2.5 定时轮询层已正确实装），WebSocket 仅承载告警实时层。

**Architecture:** greenfield WebSocket（fep-web 无既有 SSE/WebSocket）。后端：`spring-boot-starter-websocket` + 原生 `TextWebSocketHandler`（单向 server→client 通知推送，比 STOMP 轻）+ JWT 鉴权（连接后首帧 auth-token 校验，规避浏览器 WebSocket 无法设 Authorization header）+ 按 userId 路由的会话注册表；通知创建点（`CallbackInAppAlertChannel` 扇出 save 后）发布 `InAppNotificationCreatedEvent` → WS 广播监听器推送到目标 user 的活跃会话。前端：`src/shared/ws/` WebSocket 客户端（自动重连 + auth 首帧）+ `notification-store` 订阅 → 推送到达即刷新 unreadCount/list；**WS 断开降级回退既有轮询**（不破坏现状）。

**Tech Stack:** Spring Boot 3.x WebSocket（`TextWebSocketHandler` + `WebSocketConfigurer`）/ JWT（复用 `JwtTokenProvider`）/ Vue3 + TS（原生 `WebSocket` API）/ JUnit 5 + `StandardWebSocketClient` IT / Vitest。

**执行 Worktree:** `E:\FEP_v1.0_wt-b8-dashboard-ws`（分支 `feat/b8-dashboard-websocket-alerts`，触发条件第 1 项「跨 ≥3 模块」否—仅 fep-web+fep-admin-ui，但第 2 项「与已签字未执行 Plan 并存」+ 第 6 项「多会话并发活跃」命中，须隔离）。

---

## PRD 追溯

| FR-ID | PRD 章节 | 需求 | 本 Plan |
|---|---|---|---|
| FR-WEB-DASH-REFRESH | §5.2.5 | 数据更新机制——**实时更新（关键告警信息）秒级推送 WebSocket** | T1-T4（实时层） |

> **PRD §5.2.5 原文实测（`FEP_综合前置平台_PRD_v1.3.md:981-995`）**：三层更新机制——① **实时更新（关键告警信息）→ 秒级推送（WebSocket）**；② 定时轮询（常规统计数据）→ 30s（**已实装** `useAutoRefresh.ts:18`，PRD 正确，本 Plan 不动）；③ 手动刷新 → 刷新按钮（已有）。**本 Plan 仅补 ① 实时告警 WebSocket 层。PRD 明确 WebSocket（非 SSE）。**

---

## 背景与既有基础设施实测（复用而非另建）

| 组件 | 既有实测路径 | B-8 复用方式 |
|---|---|---|
| 通知创建（扇出 ADMIN） | `fep-web/.../callback/alert/channel/CallbackInAppAlertChannel.java`（`send()` → 角色三步查询定位 ADMIN users → `notifRepo.save`） | 在 save 后发布 `InAppNotificationCreatedEvent`（每 user 一个 notificationId） |
| 通知服务 | `fep-web/.../callback/notification/service/CallbackNotificationService.java`（`listUnread(userId)` / `unreadCount(userId)` / `markRead`） | 不改，前端推送到达后调既有 listUnread/unreadCount |
| 通知实体/仓储 | `CallbackNotificationEntity` / `CallbackNotificationRepository`（V32 in_app_notification） | 不改 |
| 告警评估→渠道分发 | `CallbackAlertEvaluator` → `CallbackInAppAlertChannel`（DLQ 等告警事件触发） | 上游不动，告警仍走既有链，仅在 in-app 落库后加 WS 推送 |
| 前端通知 UI | `fep-admin-ui/src/features/callback/components/NotificationBell.vue` + `stores/notification-store.ts` + `api/callbackNotification.ts` | NotificationBell mount 时连 WS，推送到达 store 刷新 |
| JWT | `fep-web/.../auth/jwt/JwtTokenProvider`（签名校验）+ `JwtAuthFilter`（header Bearer 提取） | WS 首帧 auth 复用 `JwtTokenProvider` 校验 token |
| 安全配置 | `fep-web/.../config/SecurityConfiguration.java`（PUBLIC_PATHS + 其余须 JWT） | WS endpoint 路径加入握手放行 + 自管 auth（首帧） |

**确认 greenfield**：`grep SseEmitter\|WebSocket\|EventSource` 在 fep-web Java + fep-admin-ui src 均无实现（仅 README/e2e 文本误报）。`spring-boot-starter-websocket` **未引入**（须新增）。

### ⚠️ 风险与决策披露（须 muzhou 在 santa-review/签字时确认）

1. **WebSocket 鉴权（浏览器约束）**：浏览器原生 `WebSocket` API **不能设自定义 Authorization header**。三方案：(a) **连接后首帧发 token**（不入 URL，最安全，本 Plan 默认）；(b) query param `?token=`（URL 易泄露/日志留痕，不推荐）；(c) cookie（跨域复杂）。本 Plan 采 (a)：连接 → 客户端首帧 `{"type":"auth","token":...}` → 服务端 `JwtTokenProvider` 校验 → 注册会话；N 秒内未认证或认证失败则 server close。**muzhou 确认 (a) vs 备选**。
2. **原生 WebSocket vs STOMP**：单向 server→client 通知推送，原生 `TextWebSocketHandler` 足够且轻；STOMP 需消息 broker + 更重。本 Plan 默认原生。**若未来需双向/订阅多 topic 再评估 STOMP**。
3. **告警范围**：MVP 推送 in-app 通知（`CallbackInAppAlertChannel` 落库的告警，含 DLQ）。其他「关键告警」（TLQ 故障 §5.7.3 等）受其数据源阻塞（B-9），本 Plan 不含。
4. **降级**：WS 不可用/断开 → 前端回退既有轮询（NotificationBell 现有 load），**不破坏现状**；WS 是增强非替代。常规 stats 30s 轮询完全不动。
5. **集群多实例**：当前单实例 WS 会话注册表（内存 Map）。多实例部署需 Redis pub/sub 广播跨实例（本 Plan 单实例 MVP，多实例 deferred + 显式记录）。
6. **消费方就绪**：NotificationBell 已存在且消费通知——本 Plan 有真实消费者（非"建了没人用"）。

---

## Task 0：Worktree 隔离 + 基线 + 依赖

**Step 1**: `cd /e/FEP_v1.0 && git fetch origin && git rev-parse HEAD origin/main`（确认无 drift）→ `git worktree add -b feat/b8-dashboard-websocket-alerts ../FEP_v1.0_wt-b8-dashboard-ws origin/main`。

**Step 2**: 确认 Flyway 最大 V（本 Plan **无 DB 变更**，通知表 V32 已存）；确认 `fep-web/pom.xml` 加 `spring-boot-starter-websocket` 依赖（version 由 parent BOM 管理，不写显式版本）。

---

## Task 1（tracer-bullet）：后端 WebSocket 基础设施 + JWT 首帧鉴权

**Files:**
- Modify: `fep-web/pom.xml`（加 spring-boot-starter-websocket）
- Create: `fep-web/.../web/realtime/DashboardWebSocketHandler.java`（`TextWebSocketHandler`：首帧 auth → 注册 userId→session；afterConnectionClosed 注销；含未认证超时 close）
- Create: `fep-web/.../web/realtime/DashboardWebSocketConfig.java`（`@Configuration @EnableWebSocket implements WebSocketConfigurer`，注册 handler 于 `/ws/dashboard`）
- Create: `fep-web/.../web/realtime/WebSocketSessionRegistry.java`（`ConcurrentHashMap<String userId, Set<WebSocketSession>>` 线程安全注册表 + `sendToUser(userId, json)`）
- Modify: `fep-web/.../config/SecurityConfiguration.java`（`/ws/dashboard` 加入握手放行——WS 自管 auth）
- Test: `DashboardWebSocketHandlerTest`（单元：首帧合法 token→注册；非法/超时→close；mock JwtTokenProvider + WebSocketSession）
- Test: `DashboardWebSocketAuthIT`（@SpringBootTest webEnvironment=RANDOM_PORT + StandardWebSocketClient：真连 `/ws/dashboard` → 发合法 auth 首帧→保持连接；发非法 token→被 close）

**TDD 五步**（红线 TDD + full_regression）：写失败测试（IT 连接+auth）→ `-pl fep-web -o test -Dtest=...` 确认 RED →实现 handler/config/registry →`-pl fep-web -o verify` GREEN（spotbugs 0 + ArchUnit：新包 `web.realtime` 符合分层/命名）→ commit（footer `AI-Generated: claude-code` + `Reviewed-By: pending`，独立 commit 不链式 verify）。

> ⚠️ 鉴权实现：首帧 `{"type":"auth","token":"<jwt>"}` → `JwtTokenProvider.validate` + 取 userId → `registry.register(userId, session)`；未认证会话用 `@Scheduled` 或连接级超时 close（防资源泄漏）。日志 `LogSanitizer.sanitize` + `@SuppressFBWarnings(CRLF_INJECTION_LOGS)`（红线 logsanitizer）。

---

## Task 2：通知创建 → WebSocket 推送（事件驱动）

**Files:**
- Create: `fep-web/.../callback/notification/event/InAppNotificationCreatedEvent.java`（record：`String userId, String notificationId, Instant occurredAt`，compact 构造器非空校验）
- Modify: `fep-web/.../callback/alert/channel/CallbackInAppAlertChannel.java`（每 user `notifRepo.save` 后 `eventPublisher.publishEvent(new InAppNotificationCreatedEvent(userId, id, now))`——注入 `ApplicationEventPublisher`）
- Create: `fep-web/.../web/realtime/DashboardNotificationPushListener.java`（`@Component @EventListener onCreated(InAppNotificationCreatedEvent)` → `registry.sendToUser(userId, json{type:"notification",notificationId})`——用户无活跃会话则静默 skip）
- Test: `DashboardNotificationPushListenerTest`（单元：事件→registry.sendToUser 调用；无会话→不抛）
- Test: 扩展 `DashboardWebSocketAuthIT` 或新 `NotificationPushIT`（@SpringBootTest：连 WS+auth → 发布 InAppNotificationCreatedEvent → 客户端收到 notification 帧）

> ⚠️ `CallbackInAppAlertChannel` 现有扇出循环（ADMIN users）内 save 后逐个发事件；事件发布在既有逻辑后追加，不改告警判定。需重核 CallbackInAppAlertChannel 现有签名/扇出结构（实施前 grep，红线 plan_must_grep_actual_api）。spotbugs EI_EXPOSE 注意 ApplicationEventPublisher 注入（Spring bean 无须抑制）。

**TDD 五步** 同 T1。

---

## Task 3：前端 WebSocket 客户端 + NotificationBell 实时订阅

**Files:**
- Create: `fep-admin-ui/src/shared/ws/dashboardWsClient.ts`（封装原生 `WebSocket`：connect(`/ws/dashboard`，wss/ws 按 location.protocol) → onopen 发 auth 首帧（token 取自 auth store）→ onmessage 分发 → 自动重连指数退避 → onclose/onerror 触发降级回调）
- Modify: `fep-admin-ui/src/features/callback/stores/notification-store.ts`（新增 `subscribeRealtime()`：连 WS，收到 `notification` 帧 → 调既有 `loadUnread`/`refreshCount`；WS 断开 → 启用既有轮询 fallback）
- Modify: `fep-admin-ui/src/features/callback/components/NotificationBell.vue`（mount 时 `store.subscribeRealtime()`；unmount 断开；WS 不可用回退现有轮询——保持现状不破坏）
- Test: `dashboardWsClient.test.ts`（Vitest：mock WebSocket，验 auth 首帧发送 / onmessage 分发 / 重连 / 降级回调）
- Test: `notification-store` 测试扩展（收到推送帧→触发 loadUnread；断开→fallback）

> ⚠️ 前端 token 取自既有 auth store（与 `client.ts:22-28` Bearer 注入同源）；WS URL 用 `import.meta.env` 或 location 推导（禁硬编码，红线 #6 无硬编码）。组件测试用 trigger+flushPromises 非直接 vm（红线 unit_test_bypass）。

**TDD 五步**（前端）：写失败 Vitest → `pnpm test:run` RED → 实现 → GREEN → `pnpm type-check` + `pnpm lint` → commit。

---

## Task 4（closing）：E2E 验证 + 回归 + worktree 闭环

- **E2E（可选 strong）**：Playwright smoke——登录→首页→触发一条 in-app 通知（或后端 test hook）→断言 NotificationBell badge 秒级更新（非等 30s）。若 E2E 环境成本高则 deferred + 文档记录，由后端 IT + 前端单测兜底。
- **回归**（红线 single_module_regression_no_am_flag + plan_regression_scope_explicit）：
  - minimum：`.\mvnw.cmd -pl fep-web -o verify`（上游缺则先一次性 `-am install -DskipTests`）fep-web 0 fail；`cd fep-admin-ui && pnpm test:run && pnpm type-check && pnpm lint` 全绿。
  - strong：PR → GHA Build/Test/Quality 全绿（本地非权威，铁律 #3）。
- **PRD 矩阵**（docs 仓 E:\FEP）：FR-WEB-DASH-REFRESH §5.2.5 实时层标实装（30s 轮询层注记不变）。
- **worktree teardown**：merge 后 `git worktree remove ../FEP_v1.0_wt-b8-dashboard-ws`。

---

## 质量门禁自检（9 项，每 Task commit 前）

1. 无吞异常（WS onError/close 记录+降级，非吞） 2. 测试验业务含义（auth 拒绝/推送到达/降级，非假断言） 3. 边界（无会话推送/非法 token/WS 断开重连/多会话同 user） 4. 日志无敏感数据（token 不入日志，LogSanitizer + @SuppressFBWarnings） 5. 无未用抽象 6. 无硬编码（WS URL/超时/路径走 env/常量） 7. Javadoc/TSDoc 完整 8. 无 System.out/console 残留 9. 风格一致（后端镜像既有 listener；前端镜像既有 store/composable）

## 评审与签字

- 二次 AI 评审（涉 auth/安全 + 新通信形态强制）：每 Task spec+quality review（只读静态，禁 mvn，红线 review_subagent_must_not_run_mvn）；全 Task 后 final whole-impl review（逐 commit 自洽）。
- **WebSocket 鉴权属安全相关** → 建议加密码学/安全专项 review（首帧 token 校验 + 会话生命周期 + 未认证超时）。
- 长跑 mvn → hybrid（主对话实施 + 前台 mvn + commit / subagent 仅评审，红线 harness_bg_detach_hybrid_default）。
- 本 Plan 须 santa-method 独立评审 + muzhou 签字后方可执行。

### 评审与签字记录

| 角色 | 谁 | 结论 | 日期 |
|---|---|---|---|
| Plan 作者 | Claude Code (mode A/B) | 起草 v0.1（含 §5.2.5 实测重定位为告警实时层 + 复用既有通知系统 + WS 鉴权三方案） | 2026-06-15 |
| santa 评审 | Claude Code (santa-method) | ✅ PASS-WITH-MINOR（无 BLOCKER；所有事实声明逐条 grep 实测属实：PRD §5.2.5:987 确为 WebSocket、8 大既有组件全核验、greenfield 确认、V32 已存无需新 migration；6 条 MINOR 加固建议均属实施细节折叠进 Task） | 2026-06-15 |
| **Plan 批准者** | **muzhou** | **✅ APPROVED**（3 设计点拍板：WS 鉴权 = **(a) 首帧 token**[60s 未认证 server close] / 协议 = **原生 TextWebSocketHandler** / 集群 = **单实例 MVP**[registry 接口化 + TODO 指向 Redis pub/sub P2，多实例 deferred]） | 2026-06-15 |

### santa MINOR 加固清单（折叠进实施，每条标对应 Task）

1. **[T1]** AUTH_TIMEOUT_SECONDS 参数化（默认 60s，可配置）+ 超时会话定时清理（@Scheduled / 连接级）
2. **[T1]** WS handler 日志严禁打印完整 JWT；仅记 userId + jti（`extractJti`）经 LogSanitizer + `@SuppressFBWarnings(CRLF_INJECTION_LOGS)`；失败认证记审计日志
3. **[T1]** `afterConnectionEstablished` 临时存未认证会话等首帧；`afterConnectionClosed` 原子清理 registry
4. **[T1]** registry 接口化（`WebSocketSessionRegistry` 接口 + `InMemorySessionRegistry` 实现）+ TODO 注释指向 Redis pub/sub P2
5. **[T3]** 前端自动重连指数退避上限（如 2min max backoff）；WS 断开降级回退既有轮询不破坏现状
6. **[T4]** E2E 若环境成本高则 deferred + 文档记录，后端 IT + 前端单测兜底

> **API 实测订正（T1 实施按此）**：`JwtTokenProvider` 实际方法为 `parse(token) → Claims`（非 Plan 正文写的 `validate`），取 userId 用 `parse(token).getSubject()`；jti 用 `extractJti(token)`。
