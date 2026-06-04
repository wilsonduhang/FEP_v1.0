# FEP S2 请求生命周期追踪 (request_state) 实施计划 · v0.2

> **执行方式:** superpowers:subagent-driven-development 逐 Task 执行，步骤用 `- [ ]` 跟踪。
> **状态:** v0.2（santa Round 1 REVISE 已修：BLOCKER-1 5 状态 / BLOCKER-2 normalizer / MAJOR correlation_blocked）→ 待 santa Round 2 → muzhou 签字。**未签字禁止执行。**
> **起草日:** 2026-06-04 · Claude Code (mode A 起草) · 来源 ABCD agent team D initiative 子系统 S2

**目标:** retrofit 现有 outbound 流，新建 `request_state` 聚合表追踪请求**发送→HNDEMP→结果返回**生命周期（CREATED→SENT→RESULT_RECEIVED + FAILED/STUCK 旁支），correlation key = 8 位业务 transitionNo，提供 reaper stuck 检测 + Micrometer metrics。立即 ops/对账可用 + 为 D 的 S1 上行网关 future 铺挂载点。

**前置依赖:** 现有 outbound 流（OutboundMessageQueueEntity + OutboundStatusWriterService）+ inbound 流（InboundMessageProcessedEvent）均已 ship。

**执行 Worktree:** `/Users/muzhou/FEP_v1.0_wt-s2-request-state`（分支 `feat/s2-request-state-tracking`，触发条件第 2 项「与已签字 Plan 并存」+ 第 6 项「多会话活跃」；含 V33 DB 迁移须隔离）。baseline = T0 实测 origin/main HEAD（当前 `7b3b3eb`+，须 T0 重测）。

**架构:** 事件驱动 retrofit，最小耦合既有两条流。新增 `request_state` 表 + `RequestStateService`(单写者，独立 @Service Tx 边界，镜像 OutboundStatusWriterService 拆分原则) + `RequestStateInboundListener`(监听 InboundMessageProcessedEvent，镜像 CallbackInboundListener) + outbound 既有写点加 hook + `RequestStateReaper`(@Scheduled stuck 检测，排除 correlation_blocked) + Micrometer gauges。

**技术栈:** Java 17 / Spring Boot 3.x / JPA / Flyway / Micrometer。

**AI 协同模式:** A（entity/repo/migration/metrics/测试）+ B（Service 状态机 + correlation trim + listener + reaper）。

**PRD 追溯:** PRD §2.1.2（回调辅助模块）+ §2.2.1（接口模式双向流）。FR-ID: 新增 FR-CALLBACK-REQUEST-STATE（session-end 登记矩阵）。D initiative 子系统（D 总览 `docs/plans/2026-06-03-abcd-parallel-draft-research-output.md` 任务 D 段）。

---

## 设计背景（brainstorming 决策链 + grep 实测 + santa Round 1 修订）

### 决策链（muzhou AskUserQuestion 决策门）
1. **purpose**: retrofit 现有 outbound 流做生命周期追踪（避 C 的"建了没人用"）
2. **架构**: 方案 A 新建 `request_state` 聚合表
3. **correlation key**: 8 位业务 `transitionNo`（两侧唯一共有；outbound 无 serialNo；R3 风险接受）
4. **【santa Round 1 BLOCKER-1 → muzhou】lifecycle 5 状态**：去 CALLBACK_DELIVERED（CallbackQueueEntity 无 transitionNo/serialNo，idempotencyKey=SHA-256 不可逆，hook 无键定位）；callback 投递由既有 CallbackQueueStatus 机 + Phase 2b reaper 独立追踪；callback 关联 deferred 至 callback 补 serialNo 列的独立 Plan

### grep 实测原语（baseline）
- `OutboundMessageQueueEntity`: queueId(PK 32) + **transitionNo(列宽 VARCHAR30)** + idempotencyKey(unique) + status(6 值机, ADR `2026-05-04-outbound-status-machine`) + sourceRef(255)。**无 serialNo**。
- `OutboundStatusWriterService`(单 outbound status 写者, @Service 方法级 @Transactional, 严禁同类 this 自调用): `recordSent(String queueId, String msgId, String tlqSendResult, Instant sentAt)` → SENT；`recordFailure(queueId, error)` → 失败。
- `JpaOutboundMessageEnqueueService.java:145` `entity.setTransitionNo(envelope.headFields().transitionNo())` — enqueue 写入点（CREATED hook 处）。
- `InboundMessageProcessedEvent(MessageType type, String transitionNo, String serialNo, Object body, Instant occurredAt)`（位于 **fep-processor**；transitionNo 8 位）。
- `InboundTransitionNoExtractor`: 产出 `value.trim()` 的 8 位 TransitionNo（无补位）。
- node-login `deriveTransitionNo`: 8 位数字（msgId 末 8）。
- Flyway max **V32**（V30-32 callback 系列，santa 实测未漂）→ 新表 **V33**（T0 须重测，红线 `plan_flyway_v_collision_check`）。
- ⚠️ `CallbackQueueEntity`: 仅 idempotencyKey + msgNo + targetInterfaceId + payloadJson + status...，**无 transitionNo 无 serialNo**（BLOCKER-1 依据）。
- callback Phase 2b stale reaper 未 merge（wt-callback-p2b）→ RequestStateReaper 用 `@Scheduled`（既有 4 处先例）自建。

### correlation key 与归一（santa Round 1 BLOCKER-2 修订）
- correlation key = **8 位业务 transitionNo**，两侧同源（outbound enqueue 值 + inbound extractor 值均为 8 位业务流水号；"VARCHAR(30)" 是**列宽**非值长，原 v0.1"30 vs 8 截断"表述错误已纠正）。
- `TransitionNoNormalizer.canonical(String)` = **防御性 trim**（处理潜在边界空白/DB 列宽 padding），**非 30→8 截断**；若归一后值非 8 位数字 canonical → 视为异常走 unmatched/log（不强转）。
- 实施时 T2 Step 1 grep 复核 general outbound（非 node-login）`envelope.headFields().transitionNo()` 上游值长度，确认 8 位前提；若发现非 8 位路径，canonical 规则按实测调整（不臆造）。

### R3/P3 BLOCKED + unmatched 兜底 + STUCK 污染隔离（santa Round 1 MAJOR CONCERN 修订）
- **真正结构性 BLOCKED 集合须按 ADR 当前 Status 逐 msgNo trace，禁字面外推**（santa Round 2 CONCERN-1）：R3 占位缺陷 **2026-06-01 已 DONE**（`InboundTransitionNoExtractor.extract()` 真值提取 + `deriveTransitionNo` fallback）→ **R3 类型不再结构性阻塞**；9006/9008 节点登录登出 transitionNo 由 `deriveTransitionNo` 派生 8 位 **亦非阻塞**；**当前真结构性永等不到匹配的仅 P3 Phase 2 platPayNo 类**（3115 链，ADR-P2e-2，需 ③安全+PRD instruction_id 仍 BLOCKED）。误纳 9006/9008 入集合 → 误置 correlation_blocked=true → 真 STUCK 永不告警（红线 `audit_maturity_label_needs_prd_trace`）。
- **隔离方案**: request_state 增 `correlation_blocked` 布尔列；CREATED 时按 messageType 判定（属 BLOCKED 类型集合 → true）；reaper `findStuck` 查询 `AND correlation_blocked = false` 排除；另设 `request_state.blocked` gauge 与 STUCK 区分。避免 STUCK counter 被结构性已知缺口污染（red line `audit_maturity` consumer surface 真消费）。
- 未匹配 inbound 结果（无对应 request_state，如非我方 outbound 的 inbound-initiated）→ log + metric 不 fail；upsert 幂等。

### lifecycle 状态（5 值）
`CREATED`（outbound enqueue）→ `SENT`（outbound TLQ send 成功）→ `RESULT_RECEIVED`（inbound 结果归一 transitionNo 匹配，**happy 终态**）；旁支终态 `FAILED`（outbound 永久失败/DLQ）；reaper 标 `STUCK`（SENT 超 TTL 无结果，**排除 correlation_blocked**）。

### Scope 边界
**含**: request_state 表(V33) + RequestStateService + 2 写点(outbound CREATED/SENT hook、inbound RESULT_RECEIVED listener) + RequestStateReaper + metrics + correlation_blocked 隔离。
**不含**: **callback 投递追踪/CALLBACK_DELIVERED**（deferred，需 callback 补 serialNo 列独立 Plan；现由 CallbackQueueStatus 机独立追踪）/ admin Web 查询 UI（S1 future）/ S1 REST 端点 / 修 R3 transitionNo BLOCKED 缺口（unmatched+correlation_blocked 兜底）/ SM2 PUSH 签名（S4 ⛔安全）。

---

## 文件结构

| 文件路径 | 职责 | 操作 | 模式 |
|----------|------|------|:---:|
| `fep-web/.../requeststate/RequestStateEntity.java` | JPA 聚合实体（含 correlation_blocked） | 新建 | A |
| `fep-web/.../requeststate/RequestStateLifecycle.java` | lifecycle enum(5 值) | 新建 | A |
| `fep-web/.../requeststate/RequestStateRepository.java` | repo + findByCorrelationKey + findStuck(排除 blocked) | 新建 | A |
| `fep-web/.../requeststate/RequestStateService.java` | 单写者状态机(create/markSent/markResultReceived/markFailed/markStuck)，独立 @Service Tx | 新建 | B |
| `fep-web/.../requeststate/TransitionNoNormalizer.java` | correlation key 防御 trim 归一 | 新建 | B |
| `fep-web/.../requeststate/BlockedMessageTypes.java` | R3/P3 BLOCKED msgNo 集合（correlation_blocked 判定，引 ADR） | 新建 | A |
| `fep-web/.../requeststate/RequestStateInboundListener.java` | 监听 InboundMessageProcessedEvent → markResultReceived | 新建 | B |
| `fep-web/.../requeststate/RequestStateReaper.java` | @Scheduled stuck 检测(排除 blocked) + metrics | 新建 | B |
| `fep-web/.../requeststate/RequestStateMetrics.java` | Micrometer gauge(按 lifecycle + blocked 计数) | 新建 | A |
| `fep-web/.../outbound/consumer/OutboundStatusWriterService.java` | recordSent 加 request_state SENT hook | 修改 | B |
| `fep-web/.../outbound/JpaOutboundMessageEnqueueService.java` | enqueue 加 request_state CREATED hook | 修改 | B |
| `fep-web/src/main/resources/db/migration/V33__request_state.sql` | 建表 | 新建 | A |

> **callback DELIVERED hook 文件已删除**（BLOCKER-1：5 状态无 CALLBACK_DELIVERED）。

### 共享工具类
| 工具类 | 关键方法 | 提供 Task | 消费 Task |
|---|---|---|---|
| TransitionNoNormalizer | canonical(String) | T2 | T3/T4 |
| BlockedMessageTypes | isBlocked(MessageType) | T1 | T4(CREATED)/T5(reaper) |

### RequestStateService 职责边界
**负责**: request_state 行全部状态转换（单写者），correlation 匹配。
**不负责**: outbound/inbound 各自业务 → 各自 Service。**依赖上限**: 7（ArchUnit）。**行数上限**: 300。**超出**: 拆 Command/Query Service。

---

## Task 0: worktree + baseline + Flyway V 冲突核对 `模式 A`
**验收:** origin/main HEAD 实测；`grep ^V db/migration` 确认 V33 无冲突（红线 `plan_flyway_v_collision_check`，注意别会话 callback 并发可能新增 V）；ArchUnit baseline GREEN。
- [ ] Step 1: `git fetch origin` + `git worktree add /Users/muzhou/FEP_v1.0_wt-s2-request-state -b feat/s2-request-state-tracking origin/main`
- [ ] Step 2: `ls fep-web/src/main/resources/db/migration/ | grep -oE '^V[0-9]+' | sort -t V -k2 -n | tail -3` 确认 max → 取下一 V（v0.2 假设 V33，若漂移改号）
- [ ] Step 3: 重测并发 worktree（红线 `worktree_trigger_is_dynamic_recheck_at_execution`）

## Task 1: V33 表 + Entity + Lifecycle(5) + BlockedMessageTypes + Repository `模式 A`
**PRD:** §2.1.2+§2.2.1 · **追溯:** FR-CALLBACK-REQUEST-STATE
**验收:** V33 建表（correlation_key unique + correlation_blocked 列 + 无 callback 字段）；Lifecycle 5 值；BlockedMessageTypes 含 R3/P3 BLOCKED 集合（引 ADR `2026-05-XX-r3...` / P3 BLOCKED 清单）；repo findByCorrelationKey + findStuck（lifecycle=SENT and updatedAt<:t **and correlation_blocked=false**）；flyway:info V33 migrated。
- [ ] Step 1: `V33__request_state.sql`（参照 V30-32 DDL 风格：表 `t_request_state`，列 = request_state_id PK(32) / correlation_key VARCHAR(32) UNIQUE / message_type(8) / outbound_queue_id(32) / lifecycle_status(16) / correlation_blocked BOOLEAN default false / inbound_serial_no(64) / inbound_transition_no(8) / created_at/sent_at/result_received_at/updated_at；索引 correlation_key unique + lifecycle_status + (lifecycle_status,correlation_blocked,updated_at) for stuck）
- [ ] Step 2: `RequestStateEntity` + `RequestStateLifecycle`(CREATED/SENT/RESULT_RECEIVED/FAILED/STUCK)
- [ ] Step 3: `BlockedMessageTypes`（static Set<MessageType> 或配置）。**按 ADR 当前 Status 逐 msgNo trace 构造集合（santa Round 2 CONCERN-1，红线 `audit_maturity_label_needs_prd_trace`）：R3 已 DONE 不入集合 / 9006/9008 deriveTransitionNo 非阻塞不入 / 仅纳 P3 Phase2 platPayNo 真结构性阻塞类型**；Javadoc 引 ADR-P2e-2 + 注明判定依据。空集合亦可（若当前无真阻塞 msgNo 经此 outbound）。
- [ ] Step 4: 失败测试 `RequestStateRepositoryTest`（@DataJpaTest，save + findByCorrelationKey + findStuck 断言**排除 correlation_blocked=true 行**）
- [ ] Step 5: `RequestStateRepository`（JpaRepository + findByCorrelationKey + @Query findStuck 含 correlation_blocked=false）
- [ ] Step 6: `./mvnw test -pl fep-web -Dtest=RequestStateRepositoryTest -Dsurefire.failIfNoSpecifiedTests=false` GREEN
- [ ] Step 7: commit

## Task 2: TransitionNoNormalizer + RequestStateService(5 态) `模式 B`
**验收:** normalizer = 防御 trim（**非 30→8 截断**；非 8 位 canonical → 异常信号不强转）；Step 1 grep 复核 general outbound transitionNo 值长度确认 8 位前提；Service create/markSent/markResultReceived/markFailed/markStuck 幂等 + 非法转换防御；独立 @Service Tx。
- [ ] Step 1: grep `envelope.headFields().transitionNo()` 上游 + `InboundTransitionNoExtractor` 实测值长度，确认 8 位（不符则 canonical 规则按实测调整，写回设计背景）
- [ ] Step 2: 失败测试 `TransitionNoNormalizerTest`（含空白 trim / 已 8 位幂等 / null 防御 / 非 8 位异常信号）
- [ ] Step 3: `TransitionNoNormalizer.canonical(String)`（防御 trim）
- [ ] Step 4: 失败测试 `RequestStateServiceTest`（CREATED→SENT→RESULT_RECEIVED happy + 幂等 no-op + markFailed 旁支 + 未匹配 markResultReceived 返 unmatched 信号不抛 + correlation_blocked 行 create 时置位）
- [ ] Step 5: `RequestStateService`（注入 repo + normalizer + BlockedMessageTypes；create 时 isBlocked→correlation_blocked）
- [ ] Step 6: `./mvnw test ...` GREEN
- [ ] Step 7: commit

## Task 3: RequestStateInboundListener (RESULT_RECEIVED 写点) `模式 B`
**验收:** 监听 InboundMessageProcessedEvent → normalizer 归一 → markResultReceived；未匹配 → log+metric 不 fail（红线 `dispatcher_payload_shape_blind_spot` 真实 sample IT）；新 logger wrap LogSanitizer + @SuppressFBWarnings CRLF（红线 `logsanitizer_alone_insufficient_for_findsecbugs`）。
- [ ] Step 1: 失败测试 `RequestStateInboundListenerTest`（发布真实 event → 已存 request_state 置 RESULT_RECEIVED；未匹配 → unmatched metric+log 不抛；用 trigger 非直调，红线 `unit_test_bypass` 精神）
- [ ] Step 2: `RequestStateInboundListener`（@TransactionalEventListener 镜像 CallbackInboundListener；LogSanitizer + @SuppressFBWarnings import）
- [ ] Step 3: `./mvnw test ...` GREEN
- [ ] Step 4: commit

## Task 4: outbound CREATED/SENT hook `模式 B`
**验收:** JpaOutboundMessageEnqueueService enqueue → request_state CREATED（correlation_blocked 按 messageType 置位）；OutboundStatusWriterService.recordSent → SENT；hook 独立 @Service 调用不破坏既有 Tx 边界（严禁同类 this 自调用）；既有 outbound 测试不回归。
- [ ] Step 1: 失败测试（enqueue → CREATED row by correlation_key + correlation_blocked 正确；recordSent → SENT）
- [ ] Step 2: enqueue/recordSent 注入 RequestStateService 调用（独立 @Service，Tx 边界复用既有模式）
- [ ] Step 3: `./mvnw test -pl fep-web -Dtest=OutboundStatusWriterServiceTest,JpaOutboundMessageEnqueueServiceTest,RequestState*` GREEN（含既有 outbound 回归）
- [ ] Step 4: commit

## Task 5: RequestStateReaper + Micrometer metrics `模式 B`
**验收:** @Scheduled reaper 检测 STUCK（SENT 超可配 TTL **且 correlation_blocked=false**）→ 置 STUCK + WARN(LogSanitizer) + counter；Micrometer gauge 按 lifecycle + blocked 分别计数（STUCK 与 blocked 区分）；TTL 从配置（红线 6 无硬编码超时）；prod application.yml + IT 显式 `management.prometheus.metrics.export.enabled=true`（红线 `springboot3_metrics_export_enabled`；santa MINOR：核实 prod yml 缺该行须补）。
- [ ] Step 1: 失败测试 `RequestStateReaperTest`（SENT 超 TTL 非 blocked → STUCK + counter++；SENT 超 TTL **blocked → 不标 STUCK** 计入 blocked gauge）
- [ ] Step 2: `RequestStateReaper`（@Scheduled(fixedDelayString config) + findStuck + markStuck）+ `RequestStateMetrics`（MeterBinder gauge lifecycle+blocked）
- [ ] Step 3: 配置 `fep.request-state.stuck-ttl` + `reaper.fixed-delay`；核实/补 prod yml metrics export.enabled
- [ ] Step 4: `./mvnw test ...` GREEN
- [ ] Step 5: commit

## Task 6: 全 reactor 回归 + ArchUnit + CLAUDE.md/PRD 矩阵 `模式 A`
**验收:** `./mvnw verify -pl fep-web -am` GREEN（ArchUnit 依赖方向 + checkstyle 0 + spotbugs/find-sec-bugs 0 + JaCoCo）；新包 requeststate 通过 ArchUnit 命名/依赖。
- [ ] Step 1: `./mvnw verify -pl fep-web -am`（红线 `full_regression_before_commit`；cross-module 须 -am；macOS 沙盒 exit 144 ≥2 次 → GHA 兜底）
- [ ] Step 2: CLAUDE.md 数据库表 +1（file write only，session-end 实测基数）+ PRD 矩阵 FR-CALLBACK-REQUEST-STATE（file write only）
- [ ] Step 3: push + PR
- [ ] Step 4: PR merge 后 `git worktree remove /Users/muzhou/FEP_v1.0_wt-s2-request-state`

---

## 自检清单
1. PRD: §2.1.2+§2.2.1 → FR-CALLBACK-REQUEST-STATE ✅
2. 安全: 无 SM2/SM3/SM4/密钥（S4 出范围）✅
3. 占位符: **Task 5 callback hook 已删（BLOCKER-1）**，无隐性 TBD；Task 2 Step1 grep 是复核非 TBD（值长度已实测倾向 8 位）✅
4. 类型一致: RequestStateLifecycle(5) / correlation_key / correlation_blocked 全 Task 一致 ✅
5. 测试命令: `-Dtest=` 匹配类名 ✅
6. CLAUDE.md: Task 6 ✅
7. 验收标准: 来自设计 + lifecycle 语义 ✅
8. 共享工具: TransitionNoNormalizer + BlockedMessageTypes 登记 ✅
9. 职责边界: RequestStateService 声明 ✅
10. Worktree: 第 2+6 项 + V33 隔离 + Task 6 worktree remove ✅

## santa Round 1 修订对照（v0.1→v0.2）
- **BLOCKER-1**（callback hook 无键）→ lifecycle 6→5 删 CALLBACK_DELIVERED + 删 Task 5 callback hook 文件 + Scope 明示 callback 追踪 deferred（muzhou 拍板 5 状态）
- **BLOCKER-2**（30 vs 8 混淆列宽/值长）→ correlation 改 8 位同源 + normalizer 改防御 trim 非截断 + Task 2 Step1 grep 复核
- **MAJOR CONCERN**（STUCK 污染）→ +correlation_blocked 列 + BlockedMessageTypes + reaper findStuck 排除 + blocked gauge 区分
- **MINOR**（prod yml metrics）→ Task 5 Step3 核实补 prod yml export.enabled
- **MINOR**（Task5 虚标自检#3）→ Task 5 callback 已删，自检#3 实 ✅

## 签字区
- [x] AI 独立评审 Round 1（santa-method）: ❌ **REVISE**（agentId `a9ad4f28`，2 BLOCKER+1 MAJOR+2 MINOR，已 v0.2 全修）
- [x] AI 独立评审 Round 2（santa-method）: ✅ **PASS**（agentId `ac023b7b`，3 问题逐一验证修对 + 数据点自洽 + V33 仍可用 + Tx 边界 + 红线全覆盖；1 CONCERN-1 BlockedMessageTypes 按 ADR Status trace 已 boil-lake 折入 §设计背景+Task1 Step3）
- [x] muzhou 批准签字: ✅ **APPROVED** 2026-06-04（AskUserQuestion 决策门 — 批准 + 现在实施；santa Round 2 PASS + CONCERN-1 折入后签字）
