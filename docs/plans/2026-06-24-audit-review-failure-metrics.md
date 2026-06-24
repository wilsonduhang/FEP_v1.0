# §5.8 审核失败 Metrics 可观测性 实施计划

> **For Claude:** 本 Plan 经 muzhou 签字后，按 FEP 工作流逐 Task 执行（每 Task: 写失败测试 → 确认 RED → 最小实现 → GREEN → spec+quality review → commit）。

**Goal:** 在 §5.8 审核决策路径的三个失败 throw 点增加 Micrometer 失败计数器 `fep_audit_review_failure_total{reason}`，把审核失败从「黑盒异常」升级为「灰盒可观测」，让运维看到并发双决策压力与客户端错误率。

**Architecture:** 纯加法 telemetry，零新表、零 Flyway、零三通路（Sync/Async/Batch）改动。在 `AuditReviewMetrics` 门面新增 `recordFailure(reason)`，在 `MessageReviewTaskService` 的 `loadPending`（not_found / terminal）与 `saveWithOptimisticGuard`（lock_conflict）三处 throw 前打点。镜像既有 `recordDecision` 设计与 `CallbackMetrics`/`OutboundMetrics` 风格。

**Tech Stack:** Java 17 / Spring Boot 3.x / Micrometer / Mockito / JUnit 5 / AssertJ。

**开发模式:** A（AI 主导 90%，muzhou 审核签字）。

**执行 Worktree:** `E:\FEP_v1.0_wt-audit-fail-metrics`（分支 `feat/audit-review-failure-metrics`，触发条件第 2 项「与已签字未执行的 Plan 并存」+ 第 5 项「>5min fep-web verify 并行」+ 多会话高频并发，红线 `worktree_isolates_fs_not_logic_domain` 会话起始即隔离）。

**FR 追溯:** PRD v1.3 §5.8 多级审核 Phase2 → `FR-WEB-AUDIT-REVIEW`（可观测性增强；PRD §8.6 telemetry 一致化）。

---

## 范围与边界（muzhou 已定）

**做（本 Plan 范围）:**
- `fep_audit_review_failure_total{reason="not_found"|"terminal"|"lock_conflict"}` 失败计数器。
- 三个 throw 点打点 + 单测 + 服务级行为验证。

**不做（正式 DROP / deferred，本 Plan 显式记录）:**
- **审核 DLQ 表 / `AuditReviewDeadLetterEvent` / 重试回放——正式 DROP（非 deferred）。** 论证：审核路径**不存在真正的「死信 / 丢失工作」场景**——失败全部落入两类：① 客户端错误（`not_found` 404 / `terminal` 400 / 驳回原因空白 400），不是死信，是请求方问题；② 并发冲突且已幂等处理（决策乐观锁冲突 `BIZ_5003` 客户端重试即可；创建路径 UNIQUE 冲突意味着任务已被另一线程建好，**无丢失工作**——见 `MessageReviewTaskService.createFromFailedRecord` 行 104-108 的 `findByMessageRecordId().isPresent()` 幂等前置检查 + REQUIRES_NEW + UNIQUE 约束）。若强建 `audit_review_dlq` 表将是「建了没人用」，正中红线 `feedback_tracking_table_hook_needs_production_driver`（与 2026-06-17 forward-record-tables defer 同型）。故 DLQ 半边**不建**。
- **驳回原因空白**（`reject` 的 `IllegalArgumentException`）**不打点**：纯表单校验、UI 前置拦截、非审核任务状态事件，打点价值边际。
- **Batch 逐条审核失败 metrics**——受「不改三通路」+ batch 无 errorCode 持久化约束，仍 deferred（需 muzhou 放宽约定 + batch 持久化 errorCode）。

---

## 实测地基（已 grep origin/main `9a64f4e`）

`AuditReviewMetrics`（`fep-web/src/main/java/com/puchain/fep/web/audit/review/metrics/AuditReviewMetrics.java`）:
- 构造器三参 `(MeterRegistry registry, Clock clock, Duration countCacheTtl)`，行 43-48。
- 包级常量 `COUNTER_DECISION_TOTAL`/`GAUGE_PENDING_COUNT`、私有 `TAG_DECISION`，行 30-32。
- `recordDecision(String)` 行 56-58：`registry.counter(name, tag, value).increment()`。

`MessageReviewTaskService`（`.../audit/review/service/MessageReviewTaskService.java`）:
- 构造器 `(repository, properties, metrics)`（OptimisticLockTest 行 41-42 实证）。
- `saveWithOptimisticGuard`（行 231-238）：catch `OptimisticLockingFailureException` → `FepBusinessException(BIZ_5003)`。
- `loadPending`（行 240-249）：`findById().orElseThrow(BIZ_5001)`（not_found）；`!PENDING` → throw `BIZ_5003`（terminal）。
- **注意 BIZ_5003 被 terminal 与 lock_conflict 复用**——故必须在两个不同 throw 点分别打不同 `reason` tag，不能靠错误码区分。

测试模式:
- `AuditReviewMetricsTest`：纯单测，独立 `SimpleMeterRegistry`，精确值断言。
- `MessageReviewTaskServiceOptimisticLockTest`：纯 Mockito，`new AuditReviewMetrics(new SimpleMeterRegistry(), Clock.systemUTC(), Duration.ofSeconds(10))`。
- `MessageReviewTaskServiceDecisionTest`：`@SpringBootTest` 共享 `MeterRegistry`，**差值断言**（共享 registry 跨测试累加，禁绝对值）。

---

## Task 1: AuditReviewMetrics 新增 recordFailure 计数器

**Files:**
- Modify: `fep-web/src/main/java/com/puchain/fep/web/audit/review/metrics/AuditReviewMetrics.java`
- Test: `fep-web/src/test/java/com/puchain/fep/web/audit/review/metrics/AuditReviewMetricsTest.java`

**Step 1: 写失败测试**

在 `AuditReviewMetricsTest` 末尾追加（保持现有 3 测试不动）：

```java
@Test
void recordFailure_incrementsPerReasonCounterIndependently() {
    metrics.recordFailure(AuditReviewMetrics.REASON_NOT_FOUND);
    metrics.recordFailure(AuditReviewMetrics.REASON_LOCK_CONFLICT);
    metrics.recordFailure(AuditReviewMetrics.REASON_LOCK_CONFLICT);

    assertThat(registry.counter("fep_audit_review_failure_total", "reason", "not_found").count())
            .isEqualTo(1.0);
    assertThat(registry.counter("fep_audit_review_failure_total", "reason", "lock_conflict").count())
            .isEqualTo(2.0);
    // terminal tag 独立，未被污染
    assertThat(registry.counter("fep_audit_review_failure_total", "reason", "terminal").count())
            .isEqualTo(0.0);
}
```

**Step 2: 运行确认 RED**

Run（worktree 内，PowerShell）:
```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot'
cd E:\FEP_v1.0_wt-audit-fail-metrics
.\mvnw.cmd -o -pl fep-web test '-Dtest=AuditReviewMetricsTest' '-Djacoco.skip=true' > E:\t1-red.log 2>&1
```
Expected: 编译失败（`REASON_NOT_FOUND` / `recordFailure` 不存在）或测试 RED。单独 `cat`/`Select-String` 看 log 确认，再进 Step 3。

**Step 3: 最小实现**

在 `AuditReviewMetrics` 增常量与方法（紧挨 `recordDecision`）：

```java
    static final String COUNTER_FAILURE_TOTAL = "fep_audit_review_failure_total";
    private static final String TAG_REASON = "reason";

    /** 审核任务不存在（BIZ_5001，HTTP 404）。 */
    public static final String REASON_NOT_FOUND = "not_found";
    /** 审核任务已终态，不可重复决策（BIZ_5003，HTTP 400）。 */
    public static final String REASON_TERMINAL = "terminal";
    /** 并发乐观锁冲突，任务已被他人处理（BIZ_5003，HTTP 400）。 */
    public static final String REASON_LOCK_CONFLICT = "lock_conflict";
```

```java
    /**
     * 记录一次审核决策失败（按 {@code reason} 维度区分客户端错误 / 并发冲突压力）。
     *
     * @param reason 失败原因 tag（{@link #REASON_NOT_FOUND} / {@link #REASON_TERMINAL}
     *               / {@link #REASON_LOCK_CONFLICT}），非空
     */
    public void recordFailure(final String reason) {
        registry.counter(COUNTER_FAILURE_TOTAL, TAG_REASON, reason).increment();
    }
```

同步更新类级 Javadoc 的 `<ul>`，新增一行：
```
 *   <li>{@code fep_audit_review_failure_total{reason="not_found"|"terminal"|"lock_conflict"}} — 审核决策失败计数（客户端错误 / 并发冲突可观测）</li>
```

**Step 4: 运行确认 GREEN**

Run: 同 Step 2 命令（输出到 `E:\t1-green.log`）。单独 `Select-String "Tests run|BUILD"` 看 log 确认 4 测试全过、`BUILD SUCCESS`。

**Step 5: spotbugs 自检**（红线 `subagent_must_run_spotbugs_check`）

```powershell
.\mvnw.cmd -o -pl fep-web compile '-Djacoco.skip=true' > E:\t1-compile.log 2>&1
.\mvnw.cmd -o -pl fep-web spotbugs:check '-Djacoco.skip=true' > E:\t1-spotbugs.log 2>&1
```
Expected: `BugInstance size is 0`（改注解/常量后须先 compile 再 check，红线 `spotbugs_check_needs_recompile_after_annotation`）。

**Step 6: 每 Task 独立 spec + quality review**（红线 `feedback_task_review_discipline`）

派 2 个只读 Explore agent（禁 mvn，红线 `review_subagent_must_not_run_mvn`）：spec review（行为/边界/Javadoc/与 recordDecision 对称性）+ quality review（9 项清单 / 命名 / 常量可见性）。BLOCKER 修到 0 再 commit。

**Step 7: Commit**

```powershell
git add fep-web/src/main/java/com/puchain/fep/web/audit/review/metrics/AuditReviewMetrics.java fep-web/src/test/java/com/puchain/fep/web/audit/review/metrics/AuditReviewMetricsTest.java
git commit -m "feat(audit): AuditReviewMetrics 新增 recordFailure 失败计数器 (reason 维度)`n`nAI-Generated: claude-code`nReviewed-By: pending"
```

---

## Task 2: 在三个失败 throw 点打点

**Files:**
- Modify: `fep-web/src/main/java/com/puchain/fep/web/audit/review/service/MessageReviewTaskService.java`（`loadPending` 行 240-249 + `saveWithOptimisticGuard` 行 231-238）
- Test: 新建 `fep-web/src/test/java/com/puchain/fep/web/audit/review/service/MessageReviewTaskServiceFailureMetricsTest.java`

**Step 1: 写失败测试**（纯 Mockito，镜像 OptimisticLockTest，持有自己的 SimpleMeterRegistry 引用以读计数）

```java
package com.puchain.fep.web.audit.review.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.web.audit.review.config.ReviewWorkflowProperties;
import com.puchain.fep.web.audit.review.domain.MessageReviewTaskEntity;
import com.puchain.fep.web.audit.review.domain.ReviewStatus;
import com.puchain.fep.web.audit.review.metrics.AuditReviewMetrics;
import com.puchain.fep.web.audit.review.repository.MessageReviewTaskRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

/**
 * {@link MessageReviewTaskService} 三个失败 throw 点的失败 metrics 打点验证（纯 Mockito，
 * 持有真 {@link SimpleMeterRegistry} 读计数）。
 *
 * @author FEP Team
 * @since 1.0.0
 */
class MessageReviewTaskServiceFailureMetricsTest {

    private final MessageReviewTaskRepository repository = mock(MessageReviewTaskRepository.class);
    private final ReviewWorkflowProperties properties = new ReviewWorkflowProperties();
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final AuditReviewMetrics metrics =
            new AuditReviewMetrics(registry, Clock.systemUTC(), Duration.ofSeconds(10));
    private final MessageReviewTaskService service =
            new MessageReviewTaskService(repository, properties, metrics);

    private double failureCount(final String reason) {
        return registry.counter("fep_audit_review_failure_total", "reason", reason).count();
    }

    private static MessageReviewTaskEntity task(final String id, final String status) {
        final MessageReviewTaskEntity t = new MessageReviewTaskEntity();
        t.setReviewId(id);
        t.setReviewStatus(status);
        t.setReviewLevel(1);
        t.setCurrentLevel(1);
        return t;
    }

    @Test
    void approve_notFound_recordsNotFoundFailure() {
        when(repository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approve("missing", "rv", "ok"))
                .isInstanceOf(FepBusinessException.class);
        assertThat(failureCount("not_found")).isEqualTo(1.0);
    }

    @Test
    void approve_terminal_recordsTerminalFailure() {
        when(repository.findById("done")).thenReturn(
                Optional.of(task("done", ReviewStatus.APPROVED.name())));

        assertThatThrownBy(() -> service.approve("done", "rv", "ok"))
                .isInstanceOf(FepBusinessException.class);
        assertThat(failureCount("terminal")).isEqualTo(1.0);
    }

    @Test
    void approve_lockConflict_recordsLockConflictFailure() {
        when(repository.findById("rev")).thenReturn(
                Optional.of(task("rev", ReviewStatus.PENDING.name())));
        when(repository.saveAndFlush(any())).thenThrow(
                new ObjectOptimisticLockingFailureException(MessageReviewTaskEntity.class, "rev"));

        assertThatThrownBy(() -> service.approve("rev", "rv", "ok"))
                .isInstanceOf(FepBusinessException.class);
        assertThat(failureCount("lock_conflict")).isEqualTo(1.0);
    }

    @Test
    void reject_notFound_recordsNotFoundFailure() {
        when(repository.findById("missing2")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reject("missing2", "rv", "bad"))
                .isInstanceOf(FepBusinessException.class);
        assertThat(failureCount("not_found")).isEqualTo(1.0);
    }
}
```

> 注：需补 `import static org.assertj.core.api.Assertions.assertThat;`（上方片段省略，实施时补全）。

**Step 2: 运行确认 RED**

Run:
```powershell
.\mvnw.cmd -o -pl fep-web test '-Dtest=MessageReviewTaskServiceFailureMetricsTest' '-Djacoco.skip=true' > E:\t2-red.log 2>&1
```
Expected: 4 测试 RED（计数为 0.0，打点未实现）。

**Step 3: 最小实现**

`loadPending` 改造为先打点再抛（保留语义不变）：

```java
    private MessageReviewTaskEntity loadPending(final String reviewId) {
        final MessageReviewTaskEntity t = repository.findById(reviewId).orElse(null);
        if (t == null) {
            metrics.recordFailure(AuditReviewMetrics.REASON_NOT_FOUND);
            throw new FepBusinessException(FepErrorCode.BIZ_5001, "审核任务不存在: " + reviewId);
        }
        if (!ReviewStatus.PENDING.name().equals(t.getReviewStatus())) {
            metrics.recordFailure(AuditReviewMetrics.REASON_TERMINAL);
            throw new FepBusinessException(FepErrorCode.BIZ_5003,
                    "审核任务已终态，不可重复决策: " + t.getReviewStatus());
        }
        return t;
    }
```

`saveWithOptimisticGuard` catch 内打点：

```java
    private void saveWithOptimisticGuard(final MessageReviewTaskEntity t) {
        try {
            repository.saveAndFlush(t);
        } catch (final OptimisticLockingFailureException ex) {
            metrics.recordFailure(AuditReviewMetrics.REASON_LOCK_CONFLICT);
            throw new FepBusinessException(FepErrorCode.BIZ_5003,
                    "审核任务已被他人处理，请刷新后重试: " + t.getReviewId(), ex);
        }
    }
```

**Step 4: 运行确认 GREEN**

Run: 同 Step 2（输出 `E:\t2-green.log`）。`Select-String "Tests run|BUILD"` 确认 4 测试全过。

**Step 5: 服务回归 + spotbugs**

```powershell
.\mvnw.cmd -o -pl fep-web test '-Dtest=MessageReviewTaskService*Test,AuditReviewMetricsTest' '-Djacoco.skip=true' > E:\t2-regress.log 2>&1
.\mvnw.cmd -o -pl fep-web compile '-Djacoco.skip=true' > E:\t2-compile.log 2>&1
.\mvnw.cmd -o -pl fep-web spotbugs:check '-Djacoco.skip=true' > E:\t2-spotbugs.log 2>&1
```
Expected: 既有审核测试全绿（不回归）+ `BugInstance size is 0`。注意 `loadPending` 改 `orElseThrow`→`orElse(null)` 后 SpotBugs 可能报 `NP` 路径，已用显式 null 检查守卫——确认无新 finding。

**Step 6: 每 Task 独立 spec + quality review**

派 spec review（三 throw 点语义不变 / 打点位置在 throw 前 / reason↔错误码映射正确）+ quality review（9 项 / orElse(null) 防御 / 无吞异常）。BLOCKER 修到 0。

**Step 7: Commit**

```powershell
git add fep-web/src/main/java/com/puchain/fep/web/audit/review/service/MessageReviewTaskService.java fep-web/src/test/java/com/puchain/fep/web/audit/review/service/MessageReviewTaskServiceFailureMetricsTest.java
git commit -m "feat(audit): 审核三失败路径打点 fep_audit_review_failure_total`n`nnot_found/terminal/lock_conflict 三 reason 区分客户端错误与并发冲突压力`n`nAI-Generated: claude-code`nReviewed-By: pending"
```

---

## 回归与验收（session-end / PR 前）

**Minimum（本地，load 允许时 `-pl fep-web -o verify` 自动跑全 ArchTest）:**
- `MessageReviewTaskService*Test`（含新增 `MessageReviewTaskServiceFailureMetricsTest`）+ `AuditReviewMetricsTest` 全绿。
- fep-web 通用 4 ArchTest（ModuleDependency/Naming/ClassDesign/PackageStructure）——`AuditReviewMetrics` 是 `@Component` 名 `*Metrics`，无 `*Service`/`*Configuration` 命名约束，预期 PASS。
- **无 audit 包级专属 ArchTest**（已 grep 实测 `fep-web/src/test` 下 `*ArchTest` 无 audit/review 命中），故 minimum 用通用 4 个即足；若 load 不允许全量则显式标注「本地未跑全量，依赖 GHA strong 兜底」（红线 `local_targeted_regression_must_include_module_archtest`）。
- spotbugs `BugInstance size is 0`。

**Strong（GHA）:** PR 触发 `Build, Test & Quality Gates` + SonarCloud 全绿背书（fep-web 全量 ~11min）。

**无新 Flyway**（确认：纯 Micrometer 内存计数器，零 DB schema 变更，无须 V45）。

---

## 闭环（session-end）

- Simplify 三审（reuse/quality/efficiency）→ applied + deferred。
- 8/9 维技术文档（有 code commit，红线 `feedback_infra_plan_still_needs_full_8dim_docs`）。
- Daily Report（含 §教训）。
- PR → muzhou squash merge（不可逆决策）→ 分支删 + worktree teardown + main ff。
- **DROP 记录**：审核 DLQ 半边正式从 deferred 池移除（写入 Daily Report + 衔接提示词，引红线 `tracking_table_hook_needs_production_driver` 论证「审核无真死信驱动」）。
