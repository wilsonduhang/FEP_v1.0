# FEP 接口模式回调模块 Phase 2 — 投递可靠性核心实施计划

> **执行方式:** 使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐 Task 执行。步骤使用 `- [ ]` 复选框跟踪。
> **版本:** v0.2（2026-05-26 起草 → AI 独立评审 PASS WITH MINOR → inline 修订 1 MAJOR + 4 MINOR；待 muzhou 签字）
> **基线 origin/main:** `b68da64`（实测 `git ls-remote origin main`，含 P4-MSG-J 3112 inbound merge；并发期持续 drift，T0 实施前重测）

**目标:** 为接口模式回调队列（Phase 1 已 ship happy-path）补齐**投递可靠性核心三件套**——① retry/DLQ 状态机（4xx 失败分类 + 指数退避 + 死信兜底）② claimBatch lease 防双发（`FOR UPDATE SKIP LOCKED` + SENDING 声领）③ Micrometer 指标（`fep_callback_*`）。全部镜像已 ship 的 outbound consumer 同型实现（`OutboundRetryHandler` / `OutboundMetrics` / `OutboundQueueRepository.claimBatch`），零安全敏感代码。

**前置依赖:** 接口模式回调 Phase 1 MVP 已 ship（`fep-web/.../callback/**` 全套 listener/resolver/enqueue/runner/httpClient + V27 `callback_queue` 表 + 8 测试类，矩阵 `FR-INFRA-CALLBACK` 🟡）。outbound consumer 可靠性基础设施已 ship（P5 outbound-send T7/T8/T2）作为镜像参照。

**执行 Worktree:** `/Users/muzhou/FEP_v1.0_wt-callback-p2`（分支 `feat/callback-module-phase2`，触发条件第 ② + 第 ⑦ 项）

> 红线 `feedback_worktree_for_parallel_work` + `feedback_worktree_isolates_fs_not_logic_domain`：起草时 `git worktree list` 实测多会话活跃（`.e2e` smoke `559e0f3` + `wt-outbound-msgid-xsd-fix` 别会话 outbound envelope 修复 `dfd6e37` + `wt-rc7-dirtiescontext` 残留）→ 第 ⑦ 项「多会话即触发」命中 → 须独立 worktree（会话起始即建，文件级无交集不豁免）。worktree 从 `origin/main` 派生。
>
> **T0 动态复检（强制，红线 `feedback_worktree_trigger_is_dynamic_recheck_at_execution`）**: 实施前 4 步实测 `git worktree list` + `git status --short` + `git log origin/main..main` + grep 其他 untracked Plan 头部锁定文件集。执行中发现新并发立即隔离不补建。
>
> **并排除项**（红线 `feedback_parallel_session_task_allocation_discipline`，起草时实测）:
> - **`wt-outbound-msgid-xsd-fix [fix/outbound-envelope-msgid-corrmsgid-xsd]`** 锁定 **outbound 装配域**（`fep-web/.../outbound/consumer/` builder/composer/sender/runner + `OutboundEnvelopeXsdComplianceTest`）。本 Plan 仅动 **callback 域**（`fep-web/.../callback/**`）+ 新 Flyway V29 + `application.yml` percentile 段（追加 1 行，不动 outbound 既有行）。两域文件级无交集。**唯一共享文件 = `application.yml`**：本 Plan 仅 **append** 1 行 `fep_callback_send_latency_seconds` percentile（T5），outbound 修复 Plan 实测不动 `application.yml`（其锁定 `outbound/consumer/*.java`）→ 低冲突；T5 实施前重测该文件 diff。
> - inbound 域（`messageinbound/` + body POJO）Plan A 已 merge、无活跃会话 → 不锁定。本 Plan 不触 inbound。
> - 6 触发条件实测：① 跨模块 = 1（仅 fep-web）< 3 不命中 ② **命中**（与别会话 in-flight outbound Plan 并存）③ 安全禁入无（凭证已拆独立 Plan，见下「不在本 Plan 范围」）④ TLQ tongtech 无 ⑤ >5min verify 期间需并行（命中，long-running fep-web verify）⑦ 多会话活跃命中。

**架构:** callback 域投递可靠性镜像 outbound consumer 同型设计。Phase 1 的 `CallbackQueueRunner.poll()` 用 `findTop50ByStatusOrderByCreateTimeAsc(PENDING)` 简单轮询 + 失败即 `markFailed`；Phase 2 升级为 `claimBatch(batchSize)`（`FOR UPDATE SKIP LOCKED` 多实例安全声领）→ `markSending`（status PENDING/RETRY→SENDING 防双发，claimBatch 不再重选 SENDING 行）→ HTTP POST → 终态。失败路径委派 `CallbackRetryHandler`：4xx（配置/契约错误）直接 DEAD_LETTER 不重试；5xx/超时/IO 累加 `retry_count` 指数退避（`next_retry_at = now + min(base<<count, max)`），累计 ≥ 有效 maxAttempts（per-interface `SubOutputInterface.retryCount`，回退全局默认）转 DEAD_LETTER。`CallbackMetrics` 记 `fep_callback_send_total{status}` Counter + `fep_callback_send_latency_seconds` Timer，由 runner 按终态调用。

**技术栈:** Java 17 / Spring Boot 3.x / Maven（仅 fep-web 模块）/ JPA + Flyway（H2 `MODE=MySQL` / MySQL）/ JUnit 5 + Mockito + AssertJ / Micrometer（已装配 PrometheusMeterRegistry）/ JDK `java.net.http.HttpClient`（Phase 1 已有，本 Plan 不改）。

**AI 协同模式:**

| 模式 | AI 占比 | 本计划中的应用 |
|:----:|:-------:|--------------|
| A | 90% | Flyway 迁移 / 配置 record / 测试 / closing 文档 |
| B | 70% | retry/DLQ 状态机 / claimBatch lease / runner 集成 / metrics 门面 |
| E | 0% | ⛔ **无安全禁入代码**（TOKEN/OAUTH2 凭证存储已拆独立 Plan 由 ③ 安全专家负责，见「不在本 Plan 范围」；本 Plan retry/lease/metrics 仅状态机 + 计时器，零密钥/脱敏/加解密）|

---

## 设计背景（grep 实测核实，红线 `feedback_plan_must_grep_actual_api`）

### B1. PRD 依据（接口模式回调可靠性）

- **PRD §2.2.1 接口模式** + **§2.1.2 回调**（"异步业务结果回调通知机制"）：FEP→行内系统结果回传，Phase 1 已打通 happy-path（持久队列 + HTTP §7.1 封套推送）。Phase 2 = 生产可靠性加固。
- **PRD §5.5.2 输出接口管理**：`SubOutputInterface.retryCount`（per-interface 重试次数）实测已存在（`SubOutputInterface.java` `@Column(name = "retry_count", nullable = false) private int retryCount;`），Phase 1 运行时未消费，Phase 2 retry handler 据此设 per-interface maxAttempts（设计文档 `2026-05-19-interface-mode-callback-module-design.md` §4「指数退避 ≤ `SubOutputInterface.retryCount`」）。
- **PRD §8.6 监控告警**：`fep_callback_*` Micrometer 指标（镜像 outbound `FR-INFRA-OUTBOUND-METRICS` §8.6 先例）。
- **设计来源**：`/Users/muzhou/FEP/docs/plans/2026-05-19-interface-mode-callback-module-design.md` §6 Phase 2 范围（`CallbackRetryHandler` 指数退避 + 4xx 失败分类 + 队列行 lease 防双发 + `CallbackMetrics`）。本 Plan = 该 Phase 2 的**投递可靠性核心子集**。

### B2. Phase 1 现状（实测，待升级的锚点）

| 组件 | 路径:行 | Phase 1 行为 | Phase 2 升级 |
|---|---|---|---|
| `CallbackQueueStatus` | `callback/domain/CallbackQueueStatus.java:13-19` | PENDING/SENDING/DONE/FAILED（**SENDING 已声明但未使用**，注释「Phase 2 启用 lease」`:14-15`）| 新增 `RETRY` + `DEAD_LETTER`（FAILED 保留给 interface-not-found 配置错误）|
| `CallbackQueueEntity` | `callback/domain/CallbackQueueEntity.java:95-110` | `markDone()` / `markFailed(error)`（`MAX_ERROR_LENGTH=500` 截断 `:28`/`:105-110`）；`:112-113` 注释「Phase 2: add markSending() + @Version」 | 新增字段 `retryCount`/`nextRetryAt`/`claimedAt` + 方法 `markSending()`/`markRetry(...)`/`markDeadLetter(...)` |
| `CallbackQueueRepository` | `callback/repository/CallbackQueueRepository.java:30` | `findTop50ByStatusOrderByCreateTimeAsc(status)` | 新增 `claimBatch(int)` 原生 `FOR UPDATE SKIP LOCKED` |
| `CallbackQueueRunner` | `callback/runner/CallbackQueueRunner.java:80-94`(poll) `:110-146`(processOne) | poll→findTop50(PENDING)→processOne；失败 `markFailed` `:138-140` | poll→claimBatch→markSending→processOne；失败委派 retry handler + metrics |
| `CallbackHttpClient` | `callback/http/CallbackHttpClient.java:114-122` | TOKEN→`""` / OAUTH2→`"Bearer "` 脚手架（凭证空占位）| **不改**（凭证 = 独立 Plan，见「不在本 Plan 范围」）|
| `CallbackResult` | `callback/http/CallbackResult.java` | `record(boolean success, int statusCode, String error)`（IO 异常 statusCode=0）| 不改（4xx 分类读 `statusCode()`）|
| V27 schema | `db/migration/V27__callback_queue.sql:3-16` | 无 retry_count/next_retry_at/claimed_at | **V29 ALTER ADD** 三列 + 索引 |

### B3. outbound 镜像参照（实测，零创新照搬）

| outbound（已 ship）| 路径:行 | callback 对应（本 Plan 新建）|
|---|---|---|
| `OutboundRetryHandler.handleFailure` | `outbound/consumer/OutboundRetryHandler.java:93-117`（retry_count++ / error 截断 / ≥maxAttempts→DEAD_LETTER+next_retry_at=null / else RETRY+exp_backoff `shift=min(count,30)`,`backoff=min(base<<shift,max)`）| `CallbackRetryHandler.handleDeliveryFailure`（+ 4xx 直接 DLQ 分类）|
| `OutboundMetrics` | `outbound/consumer/OutboundMetrics.java:78-100`（`fep_outbound_send_total{status}` Counter + `fep_outbound_send_latency_seconds` Timer，`recordSent(nanos)`/`recordRetry()`/`recordDeadLetter()`）| `CallbackMetrics`（`fep_callback_*`）|
| `OutboundQueueRepository.claimBatch` | `outbound/consumer/OutboundQueueRepository.java:46-54`（`WHERE status='PENDING' OR (status='RETRY' AND next_retry_at<=CURRENT_TIMESTAMP) ORDER BY next_retry_at NULLS FIRST, queue_id ASC LIMIT :batchSize FOR UPDATE SKIP LOCKED`）| `CallbackQueueRepository.claimBatch` |
| `OutboundQueueProperties` | `outbound/consumer/OutboundQueueProperties.java`（record，prefix `fep.outbound.queue`，`batchSize`/`pollIntervalMs`/`retry(backoffMillis,maxBackoffMillis,maxAttempts)`）| `CallbackQueueProperties`（prefix `fep.callback`）|
| `OutboundQueueConsumer.poll` | `outbound/consumer/OutboundQueueConsumer.java`（`@Scheduled`→`claimBatch(props.batchSize())`→`runner.run(id)` per-row try/catch）| `CallbackQueueRunner.poll`（重构）|
| Clock bean | `outbound/consumer/OutboundConsumerClockConfiguration.java:36` `systemClock()` `@ConditionalOnMissingBean(Clock.class)` | **复用全局唯一 Clock bean**（不新建，避免重复 bean）|
| `@EnableConfigurationProperties` 注册 | `OutboundConsumerClockConfiguration.java:23` `@EnableConfigurationProperties(OutboundQueueProperties.class)` | `CallbackConsumerConfiguration`（新建，注册 `CallbackQueueProperties`，**不定义 Clock bean**）|
| application.yml percentile | `application.yml:45`(include health,info,prometheus) `:55-56`(`percentiles.fep_outbound_send_latency_seconds: 0.5,0.95,0.99`)| append `fep_callback_send_latency_seconds: 0.5,0.95,0.99` |

### B4. 错误/可靠性语义（设计文档 §4，本 Plan 落地的子集）

- **失败分类**：4xx（400-499，配置/契约错误，重试无益）→ 直接 DEAD_LETTER；5xx / 超时 / IO（statusCode=0）→ 可重试。
- **per-interface maxAttempts**：有效 maxAttempts = `SubOutputInterface.retryCount > 0 ? retryCount : props.retry().maxAttempts()`（per-interface 配置优先，回退全局默认）。退避 base/max 取全局 `CallbackQueueProperties.retry()`。
- **⚠️ PRD §5.5.2 默认值对账（评审 MAJOR-1）**：PRD v1.3 §5.5.2（`FEP_综合前置平台_PRD_v1.3.md:1429`）明文「重试次数（默认 **3** 次）」。该「重试次数」由 `SubOutputInterface.retryCount`（per-interface 字段，admin 配置）承载——正常运行时每接口的重试次数即此字段值。全局 `CallbackQueueProperties.retry().maxAttempts` 仅当 per-interface `retryCount ≤ 0`（未配置）时兜底，**亦对齐 PRD 取默认 `3`**（不照搬 outbound 的 5；outbound §3.x 无 PRD 明文 3 次约束故取 5，callback §5.5.2 有明文 3 故对齐 3）。muzhou 签字时可改全局兜底为其它值。
- **防双发**：claimBatch 仅选 `PENDING` 或 `RETRY` 且到期行；声领后 `markSending`（→SENDING），claimBatch 不再重选 SENDING → 单/多实例均不重复发（`FOR UPDATE SKIP LOCKED` 保证并发声领互斥）。
- **interface-not-found**：保留 Phase 1 `markFailed`（FAILED，配置错误非投递重试，不进 retry/DLQ）。

---

## 不在本 Plan 范围（强制声明，红线 writing-plans 自检 #1）

| 排除项 | 理由 | 去向 |
|---|---|---|
| **TOKEN/OAUTH2 §5.5.3 凭证存储/注入** | 触及 ⛔ AI 禁入「密钥管理-存储」（CLAUDE.md），muzhou 2026-05-24 拍板**拆独立 Plan**与 ③ 安全专家协同 | 独立 Plan（凭证 at-rest 加密 = Mode E 人工）。本 Plan 不动 `CallbackHttpClient` |
| **`CallbackDeadLetterHandler` + 告警（`NotifyMethod`）** | 设计 §6 Phase 2 加固项，依赖告警通道集成；本 Plan 聚焦投递可靠性核心 | Phase 2b follow-up（DEAD_LETTER 行已持久可见，admin 可经查询识别）|
| **DLQ admin 查看/重放（管理 Web 前端）** | 前端 + controller，与本 Plan 后端可靠性正交 | Phase 2b/3 follow-up |
| **stale-SENDING 回收 reaper** | crash 中途遗留 SENDING 行的自动回收（claimed_at + lease 超时→RETRY）；rare crash window | Phase 2b follow-up，**本 Plan 验收标准 + Daily Report §教训 明示该运维约束**（SENDING 行 crash 后需人工/reaper 恢复）|

矩阵 `FR-INFRA-CALLBACK`（line 332）🟡 当前已列「Phase 2 retry/DLQ/metrics + TOKEN/OAUTH2 §5.5.3 凭证 + claimBatch lease 待」。本 Plan 完成 retry/DLQ + lease + metrics 三项，凭证保持「待（独立 Plan）」。

---

## FR 追溯（Plan 阶段建立 Phase 2 子 FR-ID）

| FR-ID | PRD 章节 | 需求 | 覆盖 Task |
|---|---|---|---|
| FR-INFRA-CALLBACK-RETRY | §2.2.1 + §2.1.2 + §5.5.2 | 回调投递失败 retry/DLQ 状态机（4xx 分类 + 指数退避 + per-interface maxAttempts + 死信兜底）| T1 / T2 / T3 / T4 |
| FR-INFRA-CALLBACK-LEASE | §2.2.1 | claimBatch `FOR UPDATE SKIP LOCKED` + SENDING markSending 防双发 | T1 / T4 |
| FR-INFRA-CALLBACK-METRICS | §8.6 | `fep_callback_send_total{status}` + `fep_callback_send_latency_seconds` Micrometer 指标 | T5 |

> session-end Phase 6 在 `prd-traceability-matrix.md` 更新 `FR-INFRA-CALLBACK` 行 + 登记上述 3 子 FR-ID（红线 `feedback_session_end_prd_matrix_auto_update`）。

---

## 文件结构

| 文件路径 | 职责 | 操作 | AI 模式 |
|---|---|---|:-:|
| `fep-web/src/main/resources/db/migration/V29__callback_queue_retry.sql` | callback_queue ADD retry_count/next_retry_at/claimed_at + 索引 | 新建 | A |
| `fep-web/.../callback/domain/CallbackQueueStatus.java` | 加 RETRY/DEAD_LETTER 常量 | 修改 | A |
| `fep-web/.../callback/domain/CallbackQueueEntity.java` | 加 3 字段 + markSending/markRetry/markDeadLetter | 修改 | B |
| `fep-web/.../callback/domain/CallbackQueueEntityTest.java` | 实体新方法单测 | 新建 | A |
| `fep-web/.../callback/config/CallbackQueueProperties.java` | 配置 record（batchSize/pollIntervalMs/retry）| 新建 | A |
| `fep-web/.../callback/config/CallbackConsumerConfiguration.java` | `@EnableConfigurationProperties` 注册（不建 Clock bean）| 新建 | A |
| `fep-web/.../callback/config/CallbackQueuePropertiesTest.java` | 配置绑定默认值测试 | 新建 | A |
| `fep-web/.../callback/runner/CallbackRetryHandler.java` | 4xx 分类 + 指数退避 + DLQ；返回 CallbackFailureOutcome | 新建 | B |
| `fep-web/.../callback/runner/CallbackFailureOutcome.java` | 失败终态枚举（RETRY / DEAD_LETTER）| 新建 | A |
| `fep-web/.../callback/runner/CallbackRetryHandlerTest.java` | retry/DLQ/4xx/per-interface 单测 | 新建 | A |
| `fep-web/.../callback/repository/CallbackQueueRepository.java` | 加 claimBatch 原生查询 | 修改 | B |
| `fep-web/.../callback/repository/CallbackQueueRepositoryTest.java` | claimBatch IT（选/跳/锁）| 修改 | A |
| `fep-web/.../callback/runner/CallbackQueueRunner.java` | poll 重构 claimBatch+markSending+retry+metrics | 修改 | B |
| `fep-web/.../callback/runner/CallbackQueueRunnerTest.java` | runner 新流程单测 | 修改 | A |
| `fep-web/.../callback/metrics/CallbackMetrics.java` | fep_callback_* Counter/Timer 门面 | 新建 | B |
| `fep-web/.../callback/metrics/CallbackMetricsTest.java` | metrics 门面单测 | 新建 | A |
| `fep-web/src/main/resources/application.yml` | append callback percentile | 修改 | A |

### 共享工具类清单

| 工具类 | 包 | 关键方法 | 提供者 | 消费者 |
|---|---|---|---|---|
| `LogSanitizer` | `common.util` | `sanitize(String)` | 已存在 | T3 retry handler / T4 runner（新 logger 调用须 wrap，红线 `feedback_logsanitizer_alone_insufficient_for_findsecbugs`）|
| `CallbackQueueProperties` | `callback.config` | `batchSize()` / `retry()` | T2 | T3 / T4 |
| `CallbackFailureOutcome` | `callback.runner` | enum RETRY/DEAD_LETTER | T3 | T4（metrics 分流）|
| `Clock`（全局 bean）| — | `systemClock()` | 已存在（`OutboundConsumerClockConfiguration:36`）| T3 注入（**不新建**）|

### 核心类职责边界声明

#### CallbackRetryHandler 职责边界（依赖 3：repo / props / clock）

- **负责**: 回调**投递失败**的状态机转移决策（4xx 分类 + retry_count 累加 + 指数退避 next_retry_at 计算 + DLQ 兜底）+ 持久化该转移（save 一次）。返回 `CallbackFailureOutcome` 供 runner 记 metrics。
- **不负责**: HTTP 推送（→ `CallbackHttpClient`）/ 声领与 markSending（→ runner）/ metrics 记录（→ runner 据返回值调 `CallbackMetrics`）/ interface-not-found 处理（→ runner markFailed）/ 成功路径（→ runner markDone）。
- **依赖上限**: 7（当前 3）。**行数上限**: 300。
- **如果超出**: 4xx 分类规则若膨胀 → 抽 `CallbackFailureClassifier`。

#### CallbackQueueRunner 职责边界（Phase 2 后依赖 6：callbackQueueRepository / httpClient / subOutputInterfaceRepository / props / retryHandler / metrics）

- **负责**: poll 编排（claimBatch → 逐行 markSending → processOne → 终态分流）+ per-row 异常隔离。
- **不负责**: 退避/DLQ 决策（→ retry handler）/ 指标计算（→ metrics）/ 声领 SQL（→ repository claimBatch）。
- **依赖上限**: 7（Phase 2 后 6，未超）。**行数上限**: 300。
- **如果超出**: 拆 `CallbackProcessUseCase`（解 `@Scheduled` 自调用事务问题，设计 §runner Phase 2 注 `:38-40`）。

---

## Task 1: V29 迁移 + 状态常量 + 实体新方法 `模式 A/B`

**PRD 依据:** v1.3 §2.2.1 接口模式 + §2.1.2 回调
**追溯 ID:** FR-INFRA-CALLBACK-RETRY + FR-INFRA-CALLBACK-LEASE

**验收标准（从 PRD/设计推导）:**
1. `callback_queue` 表新增 `retry_count`(INT NOT NULL DEFAULT 0) / `next_retry_at`(TIMESTAMP NULL) / `claimed_at`(TIMESTAMP NULL) 三列 + 索引 `(status, next_retry_at)`。
2. `CallbackQueueStatus` 新增 `RETRY` + `DEAD_LETTER` 常量（PENDING/SENDING/DONE/FAILED 保留）。
3. `entity.markSending()` → status=SENDING + claimedAt=now + updateTime 刷新。
4. `entity.markRetry(3, t, "err")` → status=RETRY + retryCount=3 + nextRetryAt=t + lastError 截断 ≤500。
5. `entity.markDeadLetter(5, "err")` → status=DEAD_LETTER + retryCount=5 + nextRetryAt=null + lastError 截断。

**Files:**
- Create: `fep-web/src/main/resources/db/migration/V29__callback_queue_retry.sql`
- Modify: `fep-web/src/main/java/com/puchain/fep/web/callback/domain/CallbackQueueStatus.java`
- Modify: `fep-web/src/main/java/com/puchain/fep/web/callback/domain/CallbackQueueEntity.java`
- Create: `fep-web/src/test/java/com/puchain/fep/web/callback/domain/CallbackQueueEntityTest.java`

- [ ] **Step 0: Flyway V 冲突复检（红线 `feedback_plan_flyway_v_collision_check`）**

```bash
cd /Users/muzhou/FEP_v1.0 && ls fep-web/src/main/resources/db/migration/ | grep -oE "^V[0-9]+" | sort -t V -k2 -n | tail -1
```
期望: `V28`（确认 V29 无冲突；若 ≥ V29 则改用下一空号 + 同步本 Task 文件名）。

- [ ] **Step 1: 编写失败测试**

```java
// fep-web/src/test/java/com/puchain/fep/web/callback/domain/CallbackQueueEntityTest.java
package com.puchain.fep.web.callback.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CallbackQueueEntityTest {

    private static CallbackQueueEntity newPending() {
        return CallbackQueueEntity.pending("idem-key", "iface-1", "3001", "{\"k\":\"v\"}");
    }

    @Test
    void pending_shouldStartWithZeroRetryAndNullScheduling() {
        final CallbackQueueEntity e = newPending();
        assertThat(e.getStatus()).isEqualTo(CallbackQueueStatus.PENDING);
        assertThat(e.getRetryCount()).isZero();
        assertThat(e.getNextRetryAt()).isNull();
        assertThat(e.getClaimedAt()).isNull();
    }

    @Test
    void markSending_shouldSetSendingStatusAndClaimedAt() {
        final CallbackQueueEntity e = newPending();
        e.markSending();
        assertThat(e.getStatus()).isEqualTo(CallbackQueueStatus.SENDING);
        assertThat(e.getClaimedAt()).isNotNull();
    }

    @Test
    void markRetry_shouldSetRetryStatusCountAndNextRetryAt() {
        final CallbackQueueEntity e = newPending();
        final LocalDateTime next = LocalDateTime.of(2026, 5, 26, 10, 0, 0);
        e.markRetry(3, next, "boom");
        assertThat(e.getStatus()).isEqualTo(CallbackQueueStatus.RETRY);
        assertThat(e.getRetryCount()).isEqualTo(3);
        assertThat(e.getNextRetryAt()).isEqualTo(next);
        assertThat(e.getLastError()).isEqualTo("boom");
    }

    @Test
    void markDeadLetter_shouldSetDeadLetterClearNextRetryAt() {
        final CallbackQueueEntity e = newPending();
        e.markRetry(4, LocalDateTime.now(), "prev");
        e.markDeadLetter(5, "fatal");
        assertThat(e.getStatus()).isEqualTo(CallbackQueueStatus.DEAD_LETTER);
        assertThat(e.getRetryCount()).isEqualTo(5);
        assertThat(e.getNextRetryAt()).isNull();
        assertThat(e.getLastError()).isEqualTo("fatal");
    }

    @Test
    void markRetry_shouldTruncateErrorTo500() {
        final CallbackQueueEntity e = newPending();
        e.markRetry(1, LocalDateTime.now(), "x".repeat(600));
        assertThat(e.getLastError()).hasSize(500);
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd /Users/muzhou/FEP_v1.0 && mvn test -Dtest=CallbackQueueEntityTest -pl fep-web -am -Dsurefire.failIfNoSpecifiedTests=false
```
期望: 编译失败 — `cannot find symbol: method markSending()` / `getRetryCount()` / `CallbackQueueStatus.RETRY`。

- [ ] **Step 3: 写 V29 迁移**

```sql
-- fep-web/src/main/resources/db/migration/V29__callback_queue_retry.sql
-- 接口模式回调 Phase 2：retry/DLQ + lease 字段（镜像 outbound_message_queue 模式）
ALTER TABLE callback_queue ADD COLUMN retry_count INT NOT NULL DEFAULT 0;
ALTER TABLE callback_queue ADD COLUMN next_retry_at TIMESTAMP;
ALTER TABLE callback_queue ADD COLUMN claimed_at TIMESTAMP;
-- claimBatch 过滤/排序索引：WHERE status=... ORDER BY next_retry_at
CREATE INDEX idx_callback_queue_status_nra ON callback_queue (status, next_retry_at);
```

- [ ] **Step 4: 加状态常量**

```java
// CallbackQueueStatus.java —— 在 FAILED 常量后追加（保留 PENDING/SENDING/DONE/FAILED）：
    /** 推送失败待重试（next_retry_at 到期后由 claimBatch 重新声领）。 */
    public static final String RETRY = "RETRY";
    /** 重试耗尽 / 不可重试（4xx）→ 死信，停止调度，持久可见。 */
    public static final String DEAD_LETTER = "DEAD_LETTER";
```

- [ ] **Step 5: 加实体字段与方法**

```java
// CallbackQueueEntity.java —— 字段区（在 lastError 字段后）追加：
    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "next_retry_at")
    private LocalDateTime nextRetryAt;

    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;
```

```java
// CallbackQueueEntity.java —— 方法区（在 markFailed 后、Phase 2 注释处）替换注释并追加：

    /**
     * 声领标记：PENDING/RETRY → SENDING + 记 claimedAt（防双发，claimBatch 不再重选）。
     */
    public void markSending() {
        this.status = CallbackQueueStatus.SENDING;
        this.claimedAt = LocalDateTime.now();
        this.updateTime = this.claimedAt;
    }

    /**
     * 标记待重试：累加后的 retryCount + 下次调度时间。
     *
     * @param newRetryCount 累加后的重试计数
     * @param nextRetry     下次可声领时间（now + 指数退避）
     * @param error         错误摘要，截断至 ≤500
     */
    public void markRetry(final int newRetryCount, final LocalDateTime nextRetry, final String error) {
        this.status = CallbackQueueStatus.RETRY;
        this.retryCount = newRetryCount;
        this.nextRetryAt = nextRetry;
        this.lastError = truncateError(error);
        this.updateTime = LocalDateTime.now();
    }

    /**
     * 标记死信：重试耗尽或 4xx 不可重试，清空 nextRetryAt 停止调度。
     *
     * @param newRetryCount 累加后的重试计数
     * @param error         错误摘要，截断至 ≤500
     */
    public void markDeadLetter(final int newRetryCount, final String error) {
        this.status = CallbackQueueStatus.DEAD_LETTER;
        this.retryCount = newRetryCount;
        this.nextRetryAt = null;
        this.lastError = truncateError(error);
        this.updateTime = LocalDateTime.now();
    }

    private static String truncateError(final String error) {
        return error == null ? null
                : error.substring(0, Math.min(error.length(), MAX_ERROR_LENGTH));
    }

    /** @return 重试计数 */
    public int getRetryCount() {
        return retryCount;
    }

    /** @return 下次可声领时间（RETRY 时非空，DEAD_LETTER/终态为 null）*/
    public LocalDateTime getNextRetryAt() {
        return nextRetryAt;
    }

    /** @return 声领时间（markSending 后非空）*/
    public LocalDateTime getClaimedAt() {
        return claimedAt;
    }
```

> **重构提示**: `markFailed`（`:105-110`）现有 `error.substring(...)` 改调 `truncateError(error)` 复用（避免重复，红线共享工具）。删除 `:112-113` 的旧 Phase 2 占位注释。

- [ ] **Step 6: 运行测试确认通过**

```bash
cd /Users/muzhou/FEP_v1.0 && mvn test -Dtest=CallbackQueueEntityTest -pl fep-web -am -Dsurefire.failIfNoSpecifiedTests=false
```
期望: `BUILD SUCCESS`, 5 tests passed。
> 沙盒受限时（红线 `feedback_mvn_sandbox_exit144_pattern` exit 144）跳本机，GHA CI 兜底；起草已静态确认。

- [ ] **Step 7: 提交**

```bash
git add fep-web/src/main/resources/db/migration/V29__callback_queue_retry.sql \
        fep-web/src/main/java/com/puchain/fep/web/callback/domain/ \
        fep-web/src/test/java/com/puchain/fep/web/callback/domain/
git commit -m "$(cat <<'EOF'
feat(web): callback_queue V29 retry/lease columns + entity state methods

V29 ADD retry_count/next_retry_at/claimed_at + index; CallbackQueueStatus
RETRY/DEAD_LETTER; entity markSending/markRetry/markDeadLetter (mirror outbound).

FR-INFRA-CALLBACK-RETRY / FR-INFRA-CALLBACK-LEASE
AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

## Task 2: 回调队列配置 record + 注册 `模式 A`

**PRD 依据:** v1.3 §2.2.1 接口模式
**追溯 ID:** FR-INFRA-CALLBACK-RETRY

**验收标准:**
1. `fep.callback` 前缀空配置源 → `batchSize=50` / `pollIntervalMs=5000` / `retry.backoffMillis=30000` / `retry.maxBackoffMillis=1800000` / `retry.maxAttempts=3`（默认值，maxAttempts 对齐 PRD §5.5.2 默认 3 次）。
2. 显式配置源覆盖默认值生效。
3. `CallbackConsumerConfiguration` 注册 `CallbackQueueProperties`，**不定义 Clock bean**（复用全局唯一 `systemClock`）。

**Files:**
- Create: `fep-web/src/main/java/com/puchain/fep/web/callback/config/CallbackQueueProperties.java`
- Create: `fep-web/src/main/java/com/puchain/fep/web/callback/config/CallbackConsumerConfiguration.java`
- Create: `fep-web/src/test/java/com/puchain/fep/web/callback/config/CallbackQueuePropertiesTest.java`

- [ ] **Step 1: 编写失败测试**

```java
// fep-web/src/test/java/com/puchain/fep/web/callback/config/CallbackQueuePropertiesTest.java
package com.puchain.fep.web.callback.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.mock.env.MockEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CallbackQueuePropertiesTest {

    private static CallbackQueueProperties bind(final Map<String, Object> props) {
        final MockEnvironment env = new MockEnvironment();
        props.forEach(env::setProperty);
        return new Binder(ConfigurationPropertySources.get(env))
                .bind("fep.callback", CallbackQueueProperties.class)
                .orElseGet(() -> new Binder(ConfigurationPropertySources.get(new MockEnvironment()))
                        .bindOrCreate("fep.callback", CallbackQueueProperties.class));
    }

    @Test
    void emptySource_shouldYieldDefaults() {
        final CallbackQueueProperties p = new Binder(
                ConfigurationPropertySources.get(new MockEnvironment()))
                .bindOrCreate("fep.callback", CallbackQueueProperties.class);
        assertThat(p.batchSize()).isEqualTo(50);
        assertThat(p.pollIntervalMs()).isEqualTo(5000L);
        assertThat(p.retry().backoffMillis()).isEqualTo(30000L);
        assertThat(p.retry().maxBackoffMillis()).isEqualTo(1800000L);
        assertThat(p.retry().maxAttempts()).isEqualTo(3); // PRD §5.5.2 默认 3 次
    }

    @Test
    void explicitSource_shouldOverrideDefaults() {
        final CallbackQueueProperties p = bind(Map.of(
                "fep.callback.batch-size", "20",
                "fep.callback.retry.max-attempts", "3"));
        assertThat(p.batchSize()).isEqualTo(20);
        assertThat(p.retry().maxAttempts()).isEqualTo(3);
        assertThat(p.retry().backoffMillis()).isEqualTo(30000L); // 未覆盖项保留默认
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd /Users/muzhou/FEP_v1.0 && mvn test -Dtest=CallbackQueuePropertiesTest -pl fep-web -am -Dsurefire.failIfNoSpecifiedTests=false
```
期望: 编译失败 — `cannot find symbol: class CallbackQueueProperties`。

- [ ] **Step 3: 写配置 record（镜像 `OutboundQueueProperties`）**

```java
// fep-web/src/main/java/com/puchain/fep/web/callback/config/CallbackQueueProperties.java
package com.puchain.fep.web.callback.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 接口模式回调队列消费配置（immutable record，镜像 {@code OutboundQueueProperties}）。
 *
 * <p>绑定前缀 {@code fep.callback}。{@code poll-interval-ms} 与
 * {@code CallbackQueueRunner} 的 {@code @Scheduled} 占位符
 * {@code ${fep.callback.poll-interval-ms:5000}} 双绑（注解读占位符，代码读本 record）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "fep.callback")
public record CallbackQueueProperties(
        @DefaultValue("50") int batchSize,
        @DefaultValue("5000") long pollIntervalMs,
        @DefaultValue Retry retry) {

    /**
     * 重试退避策略嵌套配置（默认 base=30s / max=30min / maxAttempts=5）。
     *
     * @param backoffMillis    首次重试退避（{@code retry.backoff-millis}）
     * @param maxBackoffMillis 退避上限（{@code retry.max-backoff-millis}）
     * @param maxAttempts      全局兜底最大重试次数（per-interface {@code SubOutputInterface.retryCount&gt;0}
     *                         时优先；默认 3 对齐 PRD §5.5.2「重试次数默认 3 次」）
     */
    public record Retry(
            @DefaultValue("30000") long backoffMillis,
            @DefaultValue("1800000") long maxBackoffMillis,
            @DefaultValue("3") int maxAttempts) {
    }
}
```

- [ ] **Step 4: 写注册配置（不建 Clock bean）**

```java
// fep-web/src/main/java/com/puchain/fep/web/callback/config/CallbackConsumerConfiguration.java
package com.puchain.fep.web.callback.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 回调队列消费配置注册。仅启用 {@link CallbackQueueProperties} 绑定。
 *
 * <p><strong>不</strong>定义 {@code Clock} bean —— 全局唯一
 * {@code OutboundConsumerClockConfiguration#systemClock()}
 * （{@code @ConditionalOnMissingBean(Clock.class)}）已提供，
 * {@code CallbackRetryHandler} 直接注入复用，避免重复 bean。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties(CallbackQueueProperties.class)
public class CallbackConsumerConfiguration {
}
```

- [ ] **Step 5: 运行测试确认通过**

```bash
cd /Users/muzhou/FEP_v1.0 && mvn test -Dtest=CallbackQueuePropertiesTest -pl fep-web -am -Dsurefire.failIfNoSpecifiedTests=false
```
期望: `BUILD SUCCESS`, 2 tests passed。

- [ ] **Step 6: 提交**

```bash
git add fep-web/src/main/java/com/puchain/fep/web/callback/config/ \
        fep-web/src/test/java/com/puchain/fep/web/callback/config/
git commit -m "$(cat <<'EOF'
feat(web): CallbackQueueProperties config record + registration

Mirror OutboundQueueProperties (prefix fep.callback); reuse global Clock bean.

FR-INFRA-CALLBACK-RETRY
AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

## Task 3: 回调失败 retry/DLQ 处理器 `模式 B`

**PRD 依据:** v1.3 §2.2.1 + §2.1.2 + §5.5.2（per-interface retryCount）
**追溯 ID:** FR-INFRA-CALLBACK-RETRY

**验收标准（手算可验证）:**
1. 4xx（如 403）→ DEAD_LETTER，retryCount=1，nextRetryAt=null，返回 `DEAD_LETTER`（不重试）。
2. 5xx（500），retryCount 0→1，maxAttempts=5 → RETRY，nextRetryAt = fixedNow + (30000<<1=60000ms=60s)，返回 `RETRY`。
3. IO（statusCode=0），retryCount 4→5，maxAttempts=5 → 5≥5 → DEAD_LETTER，nextRetryAt=null，返回 `DEAD_LETTER`。
4. per-interface retryCount=2 覆盖全局默认 5：retryCount 1→2，2≥2 → DEAD_LETTER。
5. per-interface retryCount=0 → 回退全局默认 maxAttempts=3（PRD §5.5.2）。
6. 退避封顶：retryCount 大时 backoff = min(30000<<shift, 1800000=30min)。

**Files:**
- Create: `fep-web/src/main/java/com/puchain/fep/web/callback/runner/CallbackFailureOutcome.java`
- Create: `fep-web/src/main/java/com/puchain/fep/web/callback/runner/CallbackRetryHandler.java`
- Create: `fep-web/src/test/java/com/puchain/fep/web/callback/runner/CallbackRetryHandlerTest.java`

- [ ] **Step 1: 编写失败测试**

```java
// fep-web/src/test/java/com/puchain/fep/web/callback/runner/CallbackRetryHandlerTest.java
package com.puchain.fep.web.callback.runner;

import com.puchain.fep.web.callback.config.CallbackQueueProperties;
import com.puchain.fep.web.callback.domain.CallbackQueueEntity;
import com.puchain.fep.web.callback.domain.CallbackQueueStatus;
import com.puchain.fep.web.callback.http.CallbackResult;
import com.puchain.fep.web.callback.repository.CallbackQueueRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CallbackRetryHandlerTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 5, 26, 10, 0, 0);

    private CallbackQueueRepository repo;
    private CallbackRetryHandler handler;

    private static CallbackQueueProperties defaultProps() {
        // global fallback maxAttempts=3 (PRD §5.5.2); per-interface count passed explicitly per test
        return new CallbackQueueProperties(50, 5000L,
                new CallbackQueueProperties.Retry(30000L, 1800000L, 3));
    }

    private static CallbackQueueEntity pendingWithRetryCount(final int count) {
        final CallbackQueueEntity e =
                CallbackQueueEntity.pending("idem", "iface-1", "3001", "{}");
        for (int i = 0; i < count; i++) {
            e.markRetry(i + 1, FIXED_NOW, "prev");
        }
        return e;
    }

    @BeforeEach
    void setUp() {
        repo = mock(CallbackQueueRepository.class);
        final Clock clock = Clock.fixed(FIXED_NOW.atZone(ZONE).toInstant(), ZONE);
        handler = new CallbackRetryHandler(repo, defaultProps(), clock);
    }

    @Test
    void clientError4xx_shouldGoDeadLetterImmediately() {
        final CallbackQueueEntity e = pendingWithRetryCount(0);
        final CallbackFailureOutcome outcome =
                handler.handleDeliveryFailure(e, 5, new CallbackResult(false, 403, "http 403"));
        assertThat(outcome).isEqualTo(CallbackFailureOutcome.DEAD_LETTER);
        assertThat(e.getStatus()).isEqualTo(CallbackQueueStatus.DEAD_LETTER);
        assertThat(e.getRetryCount()).isEqualTo(1);
        assertThat(e.getNextRetryAt()).isNull();
        verify(repo).save(e);
    }

    @Test
    void serverError5xx_belowMax_shouldRetryWithBackoff() {
        final CallbackQueueEntity e = pendingWithRetryCount(0);
        final CallbackFailureOutcome outcome =
                handler.handleDeliveryFailure(e, 5, new CallbackResult(false, 500, "http 500"));
        assertThat(outcome).isEqualTo(CallbackFailureOutcome.RETRY);
        assertThat(e.getStatus()).isEqualTo(CallbackQueueStatus.RETRY);
        assertThat(e.getRetryCount()).isEqualTo(1);
        // shift=min(1,30)=1; backoff=min(30000<<1,1800000)=60000ms
        assertThat(e.getNextRetryAt()).isEqualTo(FIXED_NOW.plusSeconds(60));
    }

    @Test
    void ioFailure_atMax_shouldGoDeadLetter() {
        final CallbackQueueEntity e = pendingWithRetryCount(4); // retryCount=4 → +1=5
        final CallbackFailureOutcome outcome =
                handler.handleDeliveryFailure(e, 5, new CallbackResult(false, 0, "io: timeout"));
        assertThat(outcome).isEqualTo(CallbackFailureOutcome.DEAD_LETTER);
        assertThat(e.getStatus()).isEqualTo(CallbackQueueStatus.DEAD_LETTER);
        assertThat(e.getRetryCount()).isEqualTo(5);
        assertThat(e.getNextRetryAt()).isNull();
    }

    @Test
    void perInterfaceRetryCount_shouldOverrideGlobalDefault() {
        final CallbackQueueEntity e = pendingWithRetryCount(1); // retryCount=1 → +1=2
        final CallbackFailureOutcome outcome =
                handler.handleDeliveryFailure(e, 2, new CallbackResult(false, 503, "http 503"));
        assertThat(outcome).isEqualTo(CallbackFailureOutcome.DEAD_LETTER); // 2>=2
    }

    @Test
    void zeroInterfaceRetryCount_shouldFallBackToGlobalDefault() {
        final CallbackQueueEntity e = pendingWithRetryCount(0);
        final CallbackFailureOutcome outcome =
                handler.handleDeliveryFailure(e, 0, new CallbackResult(false, 500, "http 500"));
        assertThat(outcome).isEqualTo(CallbackFailureOutcome.RETRY); // 1<3 (global default fallback)
    }

    @Test
    void backoff_shouldCapAtMaxBackoff() {
        final CallbackQueueEntity e = pendingWithRetryCount(9); // retryCount=9 → +1=10
        final ArgumentCaptor<CallbackQueueEntity> cap = ArgumentCaptor.forClass(CallbackQueueEntity.class);
        handler.handleDeliveryFailure(e, 100, new CallbackResult(false, 500, "http 500"));
        verify(repo).save(cap.capture());
        // shift=min(10,30)=10; 30000<<10=30720000 > 1800000 → cap 1800000ms=30min
        assertThat(cap.getValue().getNextRetryAt()).isEqualTo(FIXED_NOW.plusSeconds(1800));
    }
}
```
> 说明：`when(...)` 未用到的 import 在最终实现若无 stub 调用须删（避免 unused，自检 #5）。

- [ ] **Step 2: 运行测试确认失败**

```bash
cd /Users/muzhou/FEP_v1.0 && mvn test -Dtest=CallbackRetryHandlerTest -pl fep-web -am -Dsurefire.failIfNoSpecifiedTests=false
```
期望: 编译失败 — `cannot find symbol: class CallbackRetryHandler / CallbackFailureOutcome`。

- [ ] **Step 3: 写失败终态枚举**

```java
// fep-web/src/main/java/com/puchain/fep/web/callback/runner/CallbackFailureOutcome.java
package com.puchain.fep.web.callback.runner;

/**
 * 回调投递失败处理终态，供 {@code CallbackQueueRunner} 据此记 metrics。
 *
 * @author FEP Team
 * @since 1.0.0
 */
public enum CallbackFailureOutcome {
    /** 已转 RETRY，等待 next_retry_at 到期重新声领。 */
    RETRY,
    /** 已转 DEAD_LETTER（重试耗尽或 4xx 不可重试）。 */
    DEAD_LETTER
}
```

- [ ] **Step 4: 写 retry handler（镜像 `OutboundRetryHandler`）**

```java
// fep-web/src/main/java/com/puchain/fep/web/callback/runner/CallbackRetryHandler.java
package com.puchain.fep.web.callback.runner;

import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.web.callback.config.CallbackQueueProperties;
import com.puchain.fep.web.callback.domain.CallbackQueueEntity;
import com.puchain.fep.web.callback.http.CallbackResult;
import com.puchain.fep.web.callback.repository.CallbackQueueRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 回调投递失败处理器：4xx 失败分类 + 指数退避 + 死信兜底（镜像
 * {@code OutboundRetryHandler}，HTTP 语义特化）。
 *
 * <ul>
 *   <li>4xx（400-499，配置/契约错误）→ 直接 DEAD_LETTER（重试无益，促 admin 修配置）</li>
 *   <li>5xx / 超时 / IO（statusCode=0）→ retry_count++，累计 ≥ 有效 maxAttempts → DEAD_LETTER，
 *       否则 RETRY + {@code next_retry_at = now + min(base<<min(count,30), max)}</li>
 * </ul>
 *
 * <p>有效 maxAttempts = {@code interfaceRetryCount > 0 ? interfaceRetryCount
 * : props.retry().maxAttempts()}（per-interface 优先，回退全局默认）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class CallbackRetryHandler {

    private static final Logger LOG = LoggerFactory.getLogger(CallbackRetryHandler.class);
    private static final int MAX_SHIFT_BITS = 30;
    private static final int HTTP_CLIENT_ERROR_MIN = 400;
    private static final int HTTP_CLIENT_ERROR_MAX = 499;

    private final CallbackQueueRepository repo;
    private final CallbackQueueProperties props;
    private final Clock clock;

    /**
     * @param repo  回调队列 Repository，非空
     * @param props 退避配置（base/max/maxAttempts 默认），非空
     * @param clock 全局唯一 Clock bean（复用 systemClock），非空
     */
    public CallbackRetryHandler(final CallbackQueueRepository repo,
                                final CallbackQueueProperties props,
                                final Clock clock) {
        this.repo = Objects.requireNonNull(repo, "repo");
        this.props = Objects.requireNonNull(props, "props");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * 处理一次投递失败：决定 RETRY 或 DEAD_LETTER 并持久化（save 一次）。
     *
     * @param entity              失败的队列条目（已声领 SENDING），非空
     * @param interfaceRetryCount {@code SubOutputInterface.retryCount}（&le;0 回退全局默认）
     * @param result              HTTP 推送失败结果（statusCode 用于 4xx 分类）
     * @return 终态 {@link CallbackFailureOutcome}，供 runner 记 metrics
     */
    @SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
            justification = "non-literal String log args wrapped by LogSanitizer.sanitize; "
                    + "find-sec-bugs cannot detect user-defined sanitizer; int args CRLF-safe")
    public CallbackFailureOutcome handleDeliveryFailure(final CallbackQueueEntity entity,
                                                        final int interfaceRetryCount,
                                                        final CallbackResult result) {
        final int newRetryCount = entity.getRetryCount() + 1;
        final String error = result == null ? null : result.error();

        if (isNonRetryable(result)) {
            entity.markDeadLetter(newRetryCount, error);
            repo.save(entity);
            LOG.warn("callback DEAD_LETTER (4xx non-retryable) queueId={} status={}",
                    LogSanitizer.sanitize(entity.getQueueId()),
                    result == null ? -1 : result.statusCode());
            return CallbackFailureOutcome.DEAD_LETTER;
        }

        final int maxAttempts = interfaceRetryCount > 0
                ? interfaceRetryCount : props.retry().maxAttempts();
        if (newRetryCount >= maxAttempts) {
            entity.markDeadLetter(newRetryCount, error);
            repo.save(entity);
            LOG.warn("callback DEAD_LETTER (retries exhausted) queueId={} retryCount={}",
                    LogSanitizer.sanitize(entity.getQueueId()), newRetryCount);
            return CallbackFailureOutcome.DEAD_LETTER;
        }

        final long shift = Math.min(newRetryCount, MAX_SHIFT_BITS);
        final long backoffMs = Math.min(
                props.retry().backoffMillis() << shift,
                props.retry().maxBackoffMillis());
        final LocalDateTime nextRetry = LocalDateTime.now(clock).plusNanos(backoffMs * 1_000_000L);
        entity.markRetry(newRetryCount, nextRetry, error);
        repo.save(entity);
        return CallbackFailureOutcome.RETRY;
    }

    private static boolean isNonRetryable(final CallbackResult result) {
        return result != null
                && result.statusCode() >= HTTP_CLIENT_ERROR_MIN
                && result.statusCode() <= HTTP_CLIENT_ERROR_MAX;
    }
}
```
> **退避时间基准对齐**：测试用 `Clock.fixed(FIXED_NOW)` + 断言 `FIXED_NOW.plusSeconds(60)`。实现 `LocalDateTime.now(clock).plusNanos(backoffMs*1e6)` —— `Clock.fixed` 下 `LocalDateTime.now(clock)` = FIXED_NOW，+60000ms = +60s，精确匹配。

- [ ] **Step 5: 运行测试确认通过**

```bash
cd /Users/muzhou/FEP_v1.0 && mvn test -Dtest=CallbackRetryHandlerTest -pl fep-web -am -Dsurefire.failIfNoSpecifiedTests=false
```
期望: `BUILD SUCCESS`, 6 tests passed。

- [ ] **Step 6: 提交**

```bash
git add fep-web/src/main/java/com/puchain/fep/web/callback/runner/CallbackFailureOutcome.java \
        fep-web/src/main/java/com/puchain/fep/web/callback/runner/CallbackRetryHandler.java \
        fep-web/src/test/java/com/puchain/fep/web/callback/runner/CallbackRetryHandlerTest.java
git commit -m "$(cat <<'EOF'
feat(web): CallbackRetryHandler 4xx classify + exp backoff + DLQ

Mirror OutboundRetryHandler; per-interface maxAttempts fallback to global.

FR-INFRA-CALLBACK-RETRY
AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

## Task 4: claimBatch lease + runner 集成 `模式 B`

**PRD 依据:** v1.3 §2.2.1（防双发）+ §2.1.2
**追溯 ID:** FR-INFRA-CALLBACK-LEASE + FR-INFRA-CALLBACK-RETRY

**验收标准:**
1. `claimBatch(int)`：`status='PENDING'` 选中；`status='RETRY' AND next_retry_at<=now` 选中；`status='RETRY' AND next_retry_at>now` 跳过；`SENDING`/`DONE`/`DEAD_LETTER`/`FAILED` 不选。
2. runner poll：claimBatch → 逐 id findById → markSending → processOne。
3. 投递 2xx → markDone + callCount+1 + lastCallTime（保留 Phase 1）。
4. 投递非 2xx/IO → 委派 `CallbackRetryHandler.handleDeliveryFailure`（不再直接 markFailed）。
5. interface-not-found → markFailed（保留 Phase 1）。
6. **运维约束（明示，红线兑现 line 86）**：runner `markSending` 后若进程 crash，该行 stuck 在 SENDING（claimBatch 不再选 SENDING）。本 Plan **无 stale-SENDING reaper**（Phase 2b follow-up），crash 遗留行需人工查询恢复（或重置 status→RETRY）。Daily Report §教训 同步明示。

**Files:**
- Modify: `fep-web/.../callback/repository/CallbackQueueRepository.java`
- Modify: `fep-web/.../callback/runner/CallbackQueueRunner.java`
- Modify: `fep-web/src/test/java/.../callback/repository/CallbackQueueRepositoryTest.java`
- Modify: `fep-web/src/test/java/.../callback/runner/CallbackQueueRunnerTest.java`

- [ ] **Step 1: 加 claimBatch（镜像 outbound）+ 失败测试**

```java
// CallbackQueueRepository.java —— 加 import + 方法：
// import org.springframework.data.jpa.repository.Query;
// import org.springframework.data.repository.query.Param;

    /**
     * 批量声领 ≤ {@code batchSize} 条待发送/到期重试的 queue_id（多实例安全）。
     *
     * <p>过滤 {@code status='PENDING'} 或（{@code status='RETRY'} 且
     * {@code next_retry_at<=CURRENT_TIMESTAMP}）；{@code FOR UPDATE SKIP LOCKED}
     * 让多实例并行声领互不阻塞（镜像 {@code OutboundQueueRepository.claimBatch}）。</p>
     *
     * @param batchSize 单轮声领上限（{@code CallbackQueueProperties.batchSize}）
     * @return 已持锁的 queue_id 列表，可能为空
     */
    @Query(value = """
        SELECT queue_id FROM callback_queue
        WHERE status = 'PENDING'
           OR (status = 'RETRY' AND next_retry_at <= CURRENT_TIMESTAMP)
        ORDER BY next_retry_at NULLS FIRST, queue_id ASC
        LIMIT :batchSize
        FOR UPDATE SKIP LOCKED
        """, nativeQuery = true)
    List<String> claimBatch(@Param("batchSize") int batchSize);
```

测试（在既有 `CallbackQueueRepositoryTest` 追加，复用其 `@SpringBootTest` 装配 —— 实测 `CallbackQueueRepositoryTest.java:8` 用 `@SpringBootTest` 非 `@DataJpaTest`，因 H2 `MODE=MySQL` 需真实数据源跑 `FOR UPDATE SKIP LOCKED`）：

```java
    @Test
    void claimBatch_shouldSelectPendingAndDueRetry_skipFutureAndTerminal() {
        final LocalDateTime past = LocalDateTime.now().minusMinutes(1);
        final LocalDateTime future = LocalDateTime.now().plusMinutes(10);
        repository.save(CallbackQueueEntity.pending("k-pending", "i", "3001", "{}"));
        final CallbackQueueEntity dueRetry = CallbackQueueEntity.pending("k-due", "i", "3001", "{}");
        dueRetry.markRetry(1, past, "e");
        repository.save(dueRetry);
        final CallbackQueueEntity futureRetry = CallbackQueueEntity.pending("k-future", "i", "3001", "{}");
        futureRetry.markRetry(1, future, "e");
        repository.save(futureRetry);
        final CallbackQueueEntity done = CallbackQueueEntity.pending("k-done", "i", "3001", "{}");
        done.markDone();
        repository.save(done);

        final List<String> ids = repository.claimBatch(50);

        assertThat(ids).hasSize(2);
        // 排序 next_retry_at NULLS FIRST → PENDING(null) 在 due-retry(past) 之前
    }
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd /Users/muzhou/FEP_v1.0 && mvn test -Dtest=CallbackQueueRepositoryTest -pl fep-web -am -Dsurefire.failIfNoSpecifiedTests=false
```
期望: 编译失败 — `cannot find symbol: method claimBatch`。

- [ ] **Step 3: 重构 runner poll（claimBatch + markSending + retry/metrics 注入）**

```java
// CallbackQueueRunner.java —— 构造器加 2 依赖（props + retryHandler；metrics 由 T5 加）
// 字段：
//   private final CallbackQueueProperties props;
//   private final CallbackRetryHandler retryHandler;
// 构造器签名追加 (CallbackQueueProperties props, CallbackRetryHandler retryHandler)

    @SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
            justification = "non-literal String log args wrapped by LogSanitizer.sanitize; "
                    + "find-sec-bugs cannot detect user-defined sanitizer")
    @Scheduled(fixedDelayString = "${fep.callback.poll-interval-ms:5000}",
            initialDelayString = "${fep.callback.poll-initial-delay-ms:5000}")
    public void poll() {
        final List<String> ids = callbackQueueRepository.claimBatch(props.batchSize());
        if (ids.isEmpty()) {
            return;
        }
        for (final String id : ids) {
            try {
                processOne(id);
            } catch (final RuntimeException e) {
                LOG.error("callback runner row failed queueId={}", LogSanitizer.sanitize(id), e);
            }
        }
    }
```

```java
// processOne 改为按 id 取实体 + markSending + 失败委派 retry handler：
    @SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
            justification = "non-literal String log args wrapped by LogSanitizer.sanitize "
                    + "(find-sec-bugs cannot detect user-defined sanitizer); int statusCode CRLF-safe")
    void processOne(final String queueId) {
        final Optional<CallbackQueueEntity> entOpt = callbackQueueRepository.findById(queueId);
        if (entOpt.isEmpty()) {
            return; // 已被其它实例处理或清理，跳过
        }
        final CallbackQueueEntity entity = entOpt.get();
        entity.markSending();
        callbackQueueRepository.save(entity);

        final String interfaceId = entity.getTargetInterfaceId();
        final Optional<SubOutputInterface> opt = subOutputInterfaceRepository.findById(interfaceId);
        if (opt.isEmpty()) {
            entity.markFailed("interface not found");
            callbackQueueRepository.save(entity);
            LOG.warn("callback target not found queueId={} interfaceId={}",
                    LogSanitizer.sanitize(entity.getQueueId()), LogSanitizer.sanitize(interfaceId));
            return;
        }

        final SubOutputInterface target = opt.get();
        final CallbackResult result = httpClient.post(target, entity.getPayloadJson());

        if (result.success()) {
            entity.markDone();
            callbackQueueRepository.save(entity);
            target.setCallCount(target.getCallCount() + 1L);
            target.setLastCallTime(LocalDateTime.now());
            subOutputInterfaceRepository.save(target);
            LOG.info("callback sent queueId={} interfaceId={} status={}",
                    LogSanitizer.sanitize(entity.getQueueId()),
                    LogSanitizer.sanitize(interfaceId), result.statusCode());
        } else {
            retryHandler.handleDeliveryFailure(entity, target.getRetryCount(), result);
            LOG.warn("callback delivery failed queueId={} interfaceId={} error={}",
                    LogSanitizer.sanitize(entity.getQueueId()),
                    LogSanitizer.sanitize(interfaceId), LogSanitizer.sanitize(result.error()));
        }
    }
```
> Phase 1 `processOne(CallbackQueueEntity)` 签名变为 `processOne(String queueId)`。
>
> **孤儿方法清理（评审 MINOR-2，红线 `feedback_obsolete_negative_test_cleanup` 同源精神）**：runner 改用 `claimBatch` 后，`CallbackQueueRepository.findTop50ByStatusOrderByCreateTimeAsc`（`:30`）唯一生产调用方（runner `:82`）消失 → 沦为孤儿。**同 commit 删除该方法** + 同步删除 `CallbackQueueRepositoryTest` 中引用它的用例（实测 `:43`，改用 claimBatch 用例覆盖）。

- [ ] **Step 3.5: 迁移既有 `CallbackQueueRunnerTest`（评审 MINOR-3，破裂断言显式清单）**

实测既有 6 用例全 stub `findTop50`（`:76/100/118/139/161/167`），T4 后破裂，逐项迁移（红线 `feedback_unit_test_bypass` 行为验证非直调内部方法）：

| 既有用例:行 | 破裂原因 | 迁移 |
|---|---|---|
| 全 6 用例 stub `findTop50`（:76/100/118/139/161/167）| poll 改调 `claimBatch` | stub 改 `claimBatch(anyInt())` 返回 `List.of(queueId)` + `findById(queueId)` 返回实体 |
| `:109` `httpNon2xx_shouldMarkFailed`（503→FAILED 断言）| 503 现经 retryHandler→RETRY 非 FAILED | 改为 `verify(retryHandler).handleDeliveryFailure(entity, retryCount, result)`（不再断言 FAILED）|
| `:127` interface-not-found→FAILED | **不变**（保留 Phase 1 markFailed 路径）| 仅 stub 由 findTop50 改 claimBatch+findById |
| `:167` `verify(...findTop50...)` | poll 不再调 findTop50 | 改 `verify(repo).claimBatch(props.batchSize())` |
| 成功路径用例 | 需验 markSending 先于 post | 加 `verify(repo).save(实体 status=SENDING)` + httpClient.post 调用顺序 |

- [ ] **Step 4: 更新 runner 测试（新流程）**

```java
// CallbackQueueRunnerTest.java —— 关键用例（mockito）：
// - claimBatch 返回 [id1] → findById(id1) 返回 PENDING 实体 → poll() → verify save(SENDING) +
//   httpClient.post 调用
// - post 返回 success → verify markDone 路径（status DONE + subOutputInterfaceRepository.save）
// - post 返回 failure(500) → verify retryHandler.handleDeliveryFailure(entity, retryCount, result)
// - findById empty → 无 NPE，跳过
// - interface not found → markFailed
// （完整断言在实施时按既有 CallbackQueueRunnerTest 风格补全；mock CallbackRetryHandler）
```
> 实施者须读既有 `CallbackQueueRunnerTest` 全文，逐个迁移旧断言到新流程（不留悬空旧 mock）。

- [ ] **Step 5: 运行测试确认通过**

```bash
cd /Users/muzhou/FEP_v1.0 && mvn test -Dtest='CallbackQueueRepositoryTest,CallbackQueueRunnerTest' -pl fep-web -am -Dsurefire.failIfNoSpecifiedTests=false
```
期望: `BUILD SUCCESS`，全部 passed。

- [ ] **Step 6: 提交**

```bash
git add fep-web/src/main/java/com/puchain/fep/web/callback/repository/ \
        fep-web/src/main/java/com/puchain/fep/web/callback/runner/CallbackQueueRunner.java \
        fep-web/src/test/java/com/puchain/fep/web/callback/repository/ \
        fep-web/src/test/java/com/puchain/fep/web/callback/runner/CallbackQueueRunnerTest.java
git commit -m "$(cat <<'EOF'
feat(web): callback claimBatch lease + runner retry/DLQ integration

claimBatch FOR UPDATE SKIP LOCKED (mirror outbound); poll->markSending->
processOne; delivery failure delegates to CallbackRetryHandler.

FR-INFRA-CALLBACK-LEASE / FR-INFRA-CALLBACK-RETRY
AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

## Task 5: 回调 metrics 门面 + runner 装配 + percentile 配置 `模式 B`

**PRD 依据:** v1.3 §8.6 监控告警
**追溯 ID:** FR-INFRA-CALLBACK-METRICS

**验收标准:**
1. `recordSent(nanos)` → `fep_callback_send_total{status="SENT"}` +1 + `fep_callback_send_latency_seconds{status="SENT"}` 记录。
2. `recordRetry()` → `fep_callback_send_total{status="RETRY"}` +1。
3. `recordDeadLetter()` → `fep_callback_send_total{status="DEAD_LETTER"}` +1。
4. runner 2xx 调 `recordSent`；失败按 `CallbackFailureOutcome` 分流调 `recordRetry`/`recordDeadLetter`。
5. `application.yml` percentile 暴露 `fep_callback_send_latency_seconds: 0.5,0.95,0.99`。

**Files:**
- Create: `fep-web/.../callback/metrics/CallbackMetrics.java`
- Create: `fep-web/src/test/java/.../callback/metrics/CallbackMetricsTest.java`
- Modify: `fep-web/.../callback/runner/CallbackQueueRunner.java`（注入 metrics + 调用点）
- Modify: `fep-web/src/main/resources/application.yml`

- [ ] **Step 1: 失败测试（SimpleMeterRegistry）**

```java
// fep-web/src/test/java/com/puchain/fep/web/callback/metrics/CallbackMetricsTest.java
package com.puchain.fep.web.callback.metrics;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CallbackMetricsTest {

    @Test
    void recordSent_shouldIncrementSentCounterAndTimer() {
        final SimpleMeterRegistry reg = new SimpleMeterRegistry();
        final CallbackMetrics metrics = new CallbackMetrics(reg);
        metrics.recordSent(1_000_000L);
        assertThat(reg.counter("fep_callback_send_total", "status", "SENT").count()).isEqualTo(1.0);
        assertThat(reg.timer("fep_callback_send_latency_seconds", "status", "SENT").count()).isEqualTo(1L);
    }

    @Test
    void recordRetry_andDeadLetter_shouldIncrementTaggedCounters() {
        final SimpleMeterRegistry reg = new SimpleMeterRegistry();
        final CallbackMetrics metrics = new CallbackMetrics(reg);
        metrics.recordRetry();
        metrics.recordDeadLetter();
        assertThat(reg.counter("fep_callback_send_total", "status", "RETRY").count()).isEqualTo(1.0);
        assertThat(reg.counter("fep_callback_send_total", "status", "DEAD_LETTER").count()).isEqualTo(1.0);
    }
}
```

- [ ] **Step 2: 运行确认失败**

```bash
cd /Users/muzhou/FEP_v1.0 && mvn test -Dtest=CallbackMetricsTest -pl fep-web -am -Dsurefire.failIfNoSpecifiedTests=false
```
期望: 编译失败 — `cannot find symbol: class CallbackMetrics`。

- [ ] **Step 3: 写 metrics 门面（镜像 `OutboundMetrics`）**

```java
// fep-web/src/main/java/com/puchain/fep/web/callback/metrics/CallbackMetrics.java
package com.puchain.fep.web.callback.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 接口模式回调推送 telemetry：Counter / Timer 门面（镜像 {@code OutboundMetrics}，PRD §8.6）。
 *
 * <ul>
 *   <li>{@code fep_callback_send_total{status="SENT"|"RETRY"|"DEAD_LETTER"}}</li>
 *   <li>{@code fep_callback_send_latency_seconds} — 推送延迟 Timer</li>
 * </ul>
 *
 * <p>percentile 依赖 {@code application.yml}
 * {@code management.metrics.distribution.percentiles.fep_callback_send_latency_seconds}。
 * 本类仅 telemetry 门面，不涉状态机/DB。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class CallbackMetrics {

    static final String COUNTER_SEND_TOTAL = "fep_callback_send_total";
    static final String TIMER_SEND_LATENCY = "fep_callback_send_latency_seconds";
    private static final String TAG_STATUS = "status";
    private static final String STATUS_SENT = "SENT";
    private static final String STATUS_RETRY = "RETRY";
    private static final String STATUS_DEAD_LETTER = "DEAD_LETTER";

    private final MeterRegistry registry;

    /**
     * @param registry Micrometer 注册中心（Actuator 自动装配 PrometheusMeterRegistry），非空
     */
    public CallbackMetrics(final MeterRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "MeterRegistry must not be null");
    }

    /**
     * 记录一次成功推送：SENT counter+1 + latency timer。
     *
     * @param latencyNanos 推送耗时（纳秒，{@code System.nanoTime()} 差值，≥0）
     */
    public void recordSent(final long latencyNanos) {
        registry.counter(COUNTER_SEND_TOTAL, TAG_STATUS, STATUS_SENT).increment();
        registry.timer(TIMER_SEND_LATENCY, TAG_STATUS, STATUS_SENT)
                .record(latencyNanos, TimeUnit.NANOSECONDS);
    }

    /** 记录一次失败转 RETRY。 */
    public void recordRetry() {
        registry.counter(COUNTER_SEND_TOTAL, TAG_STATUS, STATUS_RETRY).increment();
    }

    /** 记录一次失败转 DEAD_LETTER。 */
    public void recordDeadLetter() {
        registry.counter(COUNTER_SEND_TOTAL, TAG_STATUS, STATUS_DEAD_LETTER).increment();
    }
}
```

- [ ] **Step 4: runner 装配 metrics**

```java
// CallbackQueueRunner.java —— 构造器加 CallbackMetrics metrics 依赖（字段 + 构造参数）。
// processOne 成功路径：记录耗时
//   final long start = System.nanoTime();
//   final CallbackResult result = httpClient.post(target, entity.getPayloadJson());
//   if (result.success()) { ...; metrics.recordSent(System.nanoTime() - start); ... }
//   else {
//       final CallbackFailureOutcome outcome =
//               retryHandler.handleDeliveryFailure(entity, target.getRetryCount(), result);
//       if (outcome == CallbackFailureOutcome.RETRY) { metrics.recordRetry(); }
//       else { metrics.recordDeadLetter(); }
//       ...log...
//   }
```

- [ ] **Step 5: application.yml 追加 percentile（仅 append，不动 outbound 行）**

```yaml
# application.yml —— management.metrics.distribution.percentiles 段，在
# fep_outbound_send_latency_seconds 行下追加：
        fep_callback_send_latency_seconds: 0.5,0.95,0.99
```

- [ ] **Step 6: 运行确认通过 + runner 测试补 metrics 验证**

```bash
cd /Users/muzhou/FEP_v1.0 && mvn test -Dtest='CallbackMetricsTest,CallbackQueueRunnerTest' -pl fep-web -am -Dsurefire.failIfNoSpecifiedTests=false
```
期望: `BUILD SUCCESS`。runner 测试 mock `CallbackMetrics` 验证 success→recordSent / RETRY→recordRetry / DEAD_LETTER→recordDeadLetter。

- [ ] **Step 7: 提交**

```bash
git add fep-web/src/main/java/com/puchain/fep/web/callback/metrics/ \
        fep-web/src/test/java/com/puchain/fep/web/callback/metrics/ \
        fep-web/src/main/java/com/puchain/fep/web/callback/runner/CallbackQueueRunner.java \
        fep-web/src/test/java/com/puchain/fep/web/callback/runner/CallbackQueueRunnerTest.java \
        fep-web/src/main/resources/application.yml
git commit -m "$(cat <<'EOF'
feat(web): CallbackMetrics fep_callback_* + runner wiring + percentile

Mirror OutboundMetrics; runner records SENT/RETRY/DEAD_LETTER by outcome.

FR-INFRA-CALLBACK-METRICS
AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

## Task 6: 全模块回归 + closing 文档 `模式 A`（无 git commit 代码改动）

**追溯 ID:** 元流程（基础设施 closing）

- [ ] **Step 1: 全 fep-web 回归（红线 `feedback_full_regression_before_commit` + ArchUnit）**

```bash
cd /Users/muzhou/FEP_v1.0 && mvn verify -pl fep-web -am --batch-mode --no-transfer-progress
```
期望: `BUILD SUCCESS` —— 含 `CallbackModuleArchTest`（callback 模块边界）+ Checkstyle + SpotBugs/find-sec-bugs（新 logger 调用须 LogSanitizer + `@SuppressFBWarnings(CRLF_INJECTION_LOGS)`，红线 `feedback_logsanitizer_alone_insufficient_for_findsecbugs`）+ JaCoCo。
> 沙盒 exit 144 时跳本机，GHA CI（PR check）兜底（红线 `feedback_mvn_sandbox_exit144_pattern`）。

- [ ] **Step 2: 更新 CLAUDE.md「当前项目状态」（file write only，⛔ NO git commit）**

> 红线 `feedback_fep_docs_repo_commit_taboo`：`/Users/muzhou/FEP/CLAUDE.md` 非 git tracked，仅 file write，**禁止 git add/commit**。
> 更新「最近里程碑」加 callback Phase 2 retry/DLQ/lease/metrics；「下一步候选」callback 项更新为「凭证（独立 Plan，③安全专家）+ DeadLetter告警 + DLQ admin（Phase 2b）」。

- [ ] **Step 3: 闭环 worktree（执行完成后，红线 CLAUDE.md 并行约束）**

```bash
# 全部 Task commit + PR merge origin/main 后：
cd /Users/muzhou/FEP_v1.0 && git worktree list
git worktree remove /Users/muzhou/FEP_v1.0_wt-callback-p2
git branch -d feat/callback-module-phase2   # 已 merge 后
```

> **注**: PRD 追溯矩阵 + 8/9 维技术文档 + Daily Report 由 **session-end Phase 2/6** 统一产出（红线 `feedback_mandatory_post_task` 四步收尾挪 session-end）。本 Task 不重复。

---

## 自检清单（writing-plans 强制）

1. **PRD 覆盖度** ✅ FR-INFRA-CALLBACK-RETRY/LEASE/METRICS 全有 Task；凭证/告警/admin 已在「不在本 Plan 范围」列明理由。
2. **安全边界** ✅ 无 SM2/SM3/SM4/密钥/脱敏；凭证（唯一安全敏感项）已拆独立 Plan（muzhou 拍板）。retry/lease/metrics 零安全代码。
3. **占位符扫描** ✅ 无 TBD/TODO（T4 Step 4 / T5 Step 4 的"实施者补全"是对既有测试文件的迁移指引，非新逻辑占位；核心新代码均完整）。
4. **类型一致性** ✅ `CallbackFailureOutcome`(T3)→runner(T4/T5)；`CallbackQueueProperties`(T2)→handler(T3)/runner(T4)；实体新方法(T1)→handler/runner。
5. **测试命令可执行** ✅ `-Dtest=` 类名与文件匹配；`-Dsurefire.failIfNoSpecifiedTests=false`（红线 `feedback_surefire3_failifno_specified_tests_param_rename`）。
6. **CLAUDE.md 更新** ✅ T6 Step 2（file write only）。
7. **验收标准完整性** ✅ 退避值手算（30000<<1=60s / cap 30min）；状态转移值明确。
8. **共享工具类** ✅ LogSanitizer/Clock/Properties/Outcome 已登记。
9. **核心类职责边界** ✅ CallbackRetryHandler(依赖3)/CallbackQueueRunner(依赖6) 均声明，未超 7/300。
10. **Worktree 触发自检** ✅ 命中 ②+⑤+⑦ → 头部声明 `wt-callback-p2` + T6 闭环 `git worktree remove`。

---

## 执行交接

**⚠️ 本 Plan 未签字，禁止直接执行。** 流程：

1. **AI 独立评审** — 派 santa-method / code-reviewer agent，输入本 Plan + PRD §2.2.1/§2.1.2/§5.5.2/§8.6 + `plan-review-checklist.md` 7 项。发现 ≥1 问题 → 修订 → 复审至通过。
2. **muzhou 签字** — 阅 Plan + 评审报告 + PRD 抽样核对 → 批准/驳回/部分修改 → Plan 末尾追加签字。
3. **执行选择** — subagent-driven（推荐，每 Task 独立 implementer + spec/quality reviewer）或 inline executing-plans。

---

## 评审记录

**v0.1 → v0.2 — AI 独立评审（general-purpose santa-method，2026-05-26，agentId `af3e11124f9ff902a`）**

- **结论**: PASS WITH MINOR（1 MAJOR + 4 MINOR，**0 BLOCKER**）。独立 grep 实测 16 处 API 声明全证实准确（镜像 outbound 6 处 + backoff 时区等价 + V28 最大 + Clock bean 唯一 + ArchUnit/命名兼容）。
- **修订已全部 inline 应用（v0.2）**:
  - **MAJOR-1**（PRD §5.5.2「默认 3 次」vs Plan 全局默认 5 未对账）→ 全局兜底 `maxAttempts` 由 5 改 **3** 对齐 PRD §5.5.2（per-interface `SubOutputInterface.retryCount` 优先承载）+ B4 加对账说明 + T2 record/test/验收 + T3 fixture/验收 同步（per-interface 显式传参的 T3 用例不受影响）。
  - **MINOR-1**（T4 误述 `@DataJpaTest`）→ 修正为 `@SpringBootTest`（实测 `CallbackQueueRepositoryTest.java:8`）。
  - **MINOR-2**（T4 后 `findTop50` 沦为孤儿生产方法）→ T4 Step 3 加同 commit 删除 + 删 `CallbackQueueRepositoryTest:43` 用例（红线 `feedback_obsolete_negative_test_cleanup`）。
  - **MINOR-3**（既有 `CallbackQueueRunnerTest` 6 用例破裂迁移留实施者）→ T4 加 Step 3.5 破裂断言显式清单（:109 503→RETRY / :167 verify claimBatch / markSending 顺序）。
  - **MINOR-4**（stale-SENDING 运维约束仅 Daily Report 承诺）→ T4 验收标准 #6 明示落地。
- **待 muzhou 拍板项**: 全局兜底 maxAttempts 默认值（Plan 取 PRD §5.5.2 的 3；muzhou 可改）。

## 批准签字

**muzhou ✅ APPROVED v0.2（2026-05-26）**

- **决策**: 批准 + subagent 驱动执行（AskUserQuestion 第 1 项 Recommended）
- **maxAttempts 默认值**: 取 3（对齐 PRD §5.5.2，muzhou 未提出覆盖）
- **执行方式**: `superpowers:subagent-driven-development`，每 Task 独立 implementer + spec/quality reviewer
- **Worktree**: T0 实施前 4 步动态复检（红线 `feedback_worktree_trigger_is_dynamic_recheck_at_execution`）后建 `/Users/muzhou/FEP_v1.0_wt-callback-p2` 分支 `feat/callback-module-phase2`
