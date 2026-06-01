# FEP AsyncPipelineIntegrationTest macOS perf flake 修复实施计划

> **执行方式:** 使用 superpowers:executing-plans 内联执行（推荐 — 1 Task 单测试类微改 / 0 业务代码 / 2 LOC + 1 import）。步骤使用 `- [ ]` 复选框跟踪。
> **版本:** v0.3（2026-05-27 — v0.2 Round 2 AI 评审 ✅ PASS 后 boil-lake 加固：2 NEW CONCERN 全部应用；待 muzhou 签字）

**目标:** 在 `AsyncPipelineIntegrationTest.performanceBaseline_100AsyncInbound_shouldHaveP95LessThan15ms` 方法上加 `@DisabledOnOs(value = OS.MAC, disabledReason = ...)` 注解，让该 perf test 在 macOS（本机 dev 环境）跳过、在 Linux（GHA Ubuntu CI 兜底）保留正常运行，消除"本机 mvnw verify 持续 fail / surefire rerun 噪声"开发者 friction，根因彻底解（macOS host scheduler/GC variance under load — 见 investigate 报告实证）。

**前置依赖:** 无（独立 test-only 微改，无 Java 业务逻辑变更，无 schema/迁移）

**执行 Worktree:** `/Users/muzhou/FEP_v1.0_wt-asyncpipeline-flake`（分支 `chore/asyncpipeline-disable-on-mac`，触发条件第 6 项 + `feedback_worktree_isolates_fs_not_logic_domain` 多会话即触发命中；v0.1 起草日 [2026-05-26] 第 2 项亦命中，v0.2 修订日 [2026-05-27] 实测 pitest 升级已 ship 第 2 项不再适用）
> 红线 `feedback_worktree_for_parallel_work` + `feedback_worktree_isolates_fs_not_logic_domain` + `feedback_worktree_trigger_is_dynamic_recheck_at_execution`（动态判定，T0 实施前最后一道闸重测）

**并排除项（红线 `feedback_parallel_session_task_allocation_discipline`，v0.2 修订日 2026-05-27 重测）:**
- wt-callback-p2 `feat/callback-module-phase2` PR #27 锁定的 18 文件（callback 域全部 — 与本 Plan 无文件交集）
- wt-r-new-1-real-xsd `refactor/r-new-1-real-xsd-validator` HEAD `457a5e4` 锁定 `fep-processor/src/main/java/.../validation/` + 关联 test（与本 Plan 仅 modify `AsyncPipelineIntegrationTest.java` 注解 + Javadoc 无文件交集）
- wt-simplify-q-drain `chore/simplify-q-drain-p4-msg-i` HEAD `457a5e4` 锁定（待 grep 实测；推断 Simplify deferred ticket 散点，与本 Plan 单测试类无交集）
- 20 untracked Plans 在 `docs/plans/`（与本 Plan 文件无重叠，本 Plan 新建独立文件）
- `.e2e` worktree 锁定 `e2e/p7.1-smoke-local` 分支（无文件交集）
- ~~wt-pitest-maven-125-upgrade~~ — pitest 1.25.0 升级已 ship 2026-05-26 origin/main `df15613`，已无并存风险

**架构:** JUnit 5.5+ `@DisabledOnOs` 条件注解，方法级；macOS 自动 skip，其他 OS（Linux/Windows）正常执行；macOS dev 强制跑可手动注释 annotation / 用 IDE Method-level Run override / 临时 `-Dgroups=...` 重定向（IDE-friendly 不复杂化注解组合）。

**技术栈:** Java 17 / Spring Boot 3.5.14 / JUnit Jupiter 5.12.2（spring-boot-starter-test BOM 引入）— `@DisabledOnOs` / `org.junit.jupiter.api.condition.OS` 自 JUnit 5.1 GA，已在 codebase classpath 中（`/Users/muzhou/.m2/repository/org/junit/jupiter/junit-jupiter-api/5.12.2/`，2026-05-26 grep 实测确认）

**AI 协同模式:**

| 模式 | AI 占比 | 本计划中的应用 |
|:----:|:-------:|--------------|
| A | 90% | 测试文件加注解 + Javadoc 同步（本 Plan 全部 Task） |

**PRD 关联:** 本 Plan 为**测试基础设施 / 元流程**性质（修复 perf gate 在 macOS 上的环境敏感性 false positive），无新业务功能 / 无新 PRD FR-ID 映射 — 适用 CLAUDE.md "Plan 治理" §"PRD 追溯"段："每个 Task 必须引用 PRD 章节 + FR-ID（**基础设施/元流程 Plan 除外**）"。

---

## 设计背景

### Root Cause（已 investigate 实证 — 见报告 `/Users/muzhou/FEP/docs/daily_reports/2026-05-26-async-pipeline-p95-investigate-report.md`）

| 环境 | P95 实测区间 | gate 余量 | flake 状态 |
|------|------------|----------|-----------|
| GHA Ubuntu CI（4 runs cross-validation） | 1.14 - 3.14ms（max=5.02ms） | 13x 安全 | ✅ 全绿 |
| macOS local（fork JVM） | 17.94 - 122ms（多 daily reports 实证） | 超 gate 1.2x - 8x | ⛔ flake 高频 |

**根因**：test 用 `System.nanoTime()` 测每次 `service.processAsyncInbound()` 100 次同步调用 wall-clock 时长。macOS 本机 host scheduler / GC pause / 系统负载（spotlight / TimeMachine / 并发 mvn fork JVM）对单次 call 时长有显著 outlier 影响 → 进 sorted 100-sample top 5% → P95 飘高。非业务/算法 bug；非 cache miss；非 thread contention（单线程同步 loop）。

**为什么 `@DisabledOnOs(MAC)` 而非 `@DisabledOnOs(MAC)` + `@EnabledIfSystemProperty` 组合**：
- JUnit `ConditionEvaluator` 用"任一 disabled 即 disabled"短路逻辑，组合 `@DisabledOnOs` + `@EnabledIfSystemProperty` 在 macOS+property=true 时 `@DisabledOnOs` 仍判 disabled → 短路 → property override 无效
- 单 `@DisabledOnOs(MAC)` 简单清晰；macOS dev 强制跑可（a）IDE 单方法 Run override 绕过条件评估（Jetbrains IntelliJ / Eclipse 标准支持）（b）临时注释 annotation（c）切 Linux Docker 跑
- 微改 ≤ 5 LOC 原则匹配（红线精神：don't over-engineer 3-LOC 修复）

**为什么 `@DisabledOnOs(MAC)` 而非 `@EnabledOnOs(LINUX)`（v0.2 CONCERN-4 修订补充）**：
- 当前 dev 矩阵：macOS 本机（muzhou + 未来加入开发者）+ Linux GHA CI（Ubuntu 22.04）。无 Windows / 其他 dev 环境
- `@DisabledOnOs(MAC)`（本 Plan 采用）= macOS 跳过 / Linux + Windows + 其他都跑 — **更宽容**：未来若加 Windows dev / WSL2 / Linux 笔记本贡献者，P95 数据可观察累积
- `@EnabledOnOs(LINUX)` 备选 = 仅 Linux 跑 / 其他都跳 — **更保守**：只在已知稳定环境跑，未来加 OS 时默认 skip 直到证明稳
- 选 `@DisabledOnOs(MAC)`：基于 "Linux 已实证稳（GHA 4 cross-validation）+ macOS 已实证不稳" 二点正面证据；其他 OS 未实证但 default 允许跑符合 fail-open 精神
- 切换条件：未来 dev 矩阵扩展含已知不稳 OS（如 Apple Silicon emulation / 老内核 Linux），切到 `@EnabledOnOs(LINUX)` 保守路径

### 已考察并放弃的备选

| 方案 | 放弃理由 |
|------|---------|
| 现状（surefire rerun + 文档化）| 开发者本地 mvnw verify 持续 fail，friction 不解决；红线 `feedback_macos_apfs_fork_classloader_race` 案例附录已警示 "7 天内累积 ≥3 次 → 升级"，已超阈值 |
| 动态阈值（macOS=100ms / Linux=15ms）| 100ms gate 实际抓不到任何有用 degradation；arbitrary 阈值；不解决根因 |
| 200 iter + trimmed mean + P90 | 改测试语义；可能掩盖真 degradation；同样 host-sensitive 不解决根因 |
| 提取为 JMH benchmark + 专 perf CI job | 投入大；JMH 同样 host-sensitive；当前 GHA billing 阻塞下 perf CI job 也跑不了 |

---

## 文件结构

| 文件路径 | 职责 | 操作 | AI 模式 |
|----------|------|------|:-------:|
| `fep-processor/src/test/java/com/puchain/fep/processor/pipeline/AsyncPipelineIntegrationTest.java` | end-to-end IT 含 perf baseline | Modify（+2 import + 2 annotation 行 + Javadoc 增补） | A |

### 共享工具类清单

N/A — 单文件单方法注解修改，无新工具类。

### 核心类职责边界声明

N/A — 测试类，无 Service 职责分析。

---

## Task 0: 创建独立 worktree + 分支 `模式 A`

**追溯 ID:** N/A（基础设施 Task）

**验收标准:**
1. `git worktree list` 显示新 worktree `/Users/muzhou/FEP_v1.0_wt-asyncpipeline-flake` 在分支 `chore/asyncpipeline-disable-on-mac`
2. 新 worktree HEAD 与 main 一致（`457a5e4` 或更新 — 实施前重测）
3. Plan 文件 `docs/plans/2026-05-26-asyncpipeline-perf-flake-disable-on-mac.md` 在新 worktree 中可访问（通过 git 共享对象不需 cp）

**Files:** 无文件改动（git 元操作）

- [ ] **Step 1: 实施前最后一道闸重测 baseline**（红线 `feedback_baseline_drift_during_long_review_cycle` + `feedback_worktree_trigger_is_dynamic_recheck_at_execution` T0 重测）

```bash
cd /Users/muzhou/FEP_v1.0
git fetch origin main
git log origin/main..main --oneline  # 期望: 空（无未推送 WIP）
git log -1 --oneline origin/main      # 记录 baseline HEAD（期望: df15613 或更新；签字日 2026-05-27 实测）
git worktree list                     # 确认无 wt-asyncpipeline-flake 残留；预期含 wt-callback-p2 / wt-r-new-1-real-xsd / wt-simplify-q-drain（多会话活跃）
```

> **baseline drift 历史**（红线 `feedback_baseline_drift_during_long_review_cycle` 4 时点跨日实测）：
> - v0.1 起草日 (2026-05-26): origin/main = `457a5e4`
> - v0.2 修订日 (2026-05-27): origin/main = `df15613`（drift 2 commits：`f9...` + `df15613` pitest-maven 1.25.0 ship；别会话产物）
> - T0 实施日: **重新 grep 实测取最新 HEAD**，不预设

- [ ] **Step 2: 创建 worktree + 分支**

```bash
cd /Users/muzhou/FEP_v1.0
git worktree add /Users/muzhou/FEP_v1.0_wt-asyncpipeline-flake -b chore/asyncpipeline-disable-on-mac origin/main
cd /Users/muzhou/FEP_v1.0_wt-asyncpipeline-flake
git status        # 期望: On branch chore/asyncpipeline-disable-on-mac, nothing to commit, working tree clean
git log -1 --oneline  # 期望: 与 origin/main HEAD 一致
```

- [ ] **Step 3: NO git commit（Task 0 仅 git 元操作，无文件改动）**

> 红线 `feedback_plan_step3_commit_template_residue`: 元操作 Task 模板必须显式 "NO git commit"。Plan 文件本身在主 worktree 为 untracked，通过 git share objects 在新 worktree 中不可见（新 worktree HEAD 来自 origin/main，不含 Plan）；Task 1 commit 前从主 wt copy。

---

## Task 1: 加 `@DisabledOnOs(OS.MAC)` 注解 + Javadoc 同步 `模式 A`

**追溯 ID:** N/A（测试基础设施）

**验收标准（不来自代码 — 来自 root cause investigate 报告）:**
1. macOS 上跑 `mvn test -pl fep-processor -Dtest=AsyncPipelineIntegrationTest` 后 surefire 输出含 `Tests run: 5, Failures: 0, Errors: 0, Skipped: 1`（`performanceBaseline_*` 方法 skipped，其他 4 个 test 正常跑过）
2. Linux 上跑（GHA 兜底，本 Task 不直接验证）期望 `Tests run: 5, Failures: 0, Errors: 0, Skipped: 0` — `performanceBaseline_*` 正常执行；保留对未来 uncached JAXBContext / lost XSD cache regression 的捕获能力
3. Javadoc 含 `@implNote` 段说明 macOS skip 决策依据 + investigate 报告引用 + macOS dev 强制跑的 3 种 workaround
4. SpotBugs / Checkstyle / find-sec-bugs 无新增 finding（注解本身无规则违反）
5. 9 项 AI 代码自检清单全过（详见 `docs/guides/ai-code-review-checklist.md`）

**Files:**
- Modify: `fep-processor/src/test/java/com/puchain/fep/processor/pipeline/AsyncPipelineIntegrationTest.java`
- Modify: `docs/plans/2026-05-26-asyncpipeline-perf-flake-disable-on-mac.md`（本 Plan 文件本身从主 worktree copy 进新 worktree）

- [ ] **Step 1: Plan 文件 sync 到新 worktree**

```bash
cd /Users/muzhou/FEP_v1.0_wt-asyncpipeline-flake
cp /Users/muzhou/FEP_v1.0/docs/plans/2026-05-26-asyncpipeline-perf-flake-disable-on-mac.md docs/plans/
git status --short  # 期望: ?? docs/plans/2026-05-26-asyncpipeline-perf-flake-disable-on-mac.md
```

- [ ] **Step 2: TDD - RED 阶段实测 macOS flake 基线**（实施前抓证据 — 不是为了新增 test，是为了验证 fix 后行为切换）

```bash
cd /Users/muzhou/FEP_v1.0_wt-asyncpipeline-flake
# macOS 本机连跑 3 次抓 RED 证据（如果 3 次都 PASS，本 fix 必要性需重评估）
for i in 1 2 3; do
  echo "=== Run $i ==="
  ./mvnw test -pl fep-processor -Dtest='AsyncPipelineIntegrationTest#performanceBaseline_100AsyncInbound_shouldHaveP95LessThan15ms' -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | grep -E "PERF.*P95|FAIL|PASS|BUILD"
done
# 期望: ≥1 次 BUILD FAILURE + P95 > 15ms 出现（前述 daily reports + investigate 报告实证支持）
# 若 3/3 PASS（罕见）→ 不立即 abort，按下方 "3/3 PASS 处置流程" 执行
```

> **3/3 PASS 处置流程**（v0.2 CONCERN-5 修订补充，量化判定避免 implementer 主观 abort）:
> 1. 再连跑 7 次（共 10 次），全部 P95 数据 + BUILD 结果记录到本地 `/tmp/asyncpipeline-fix-red-10runs.log`
> 2. 判定矩阵：
>    - ≥1 次 fail（10 次中）→ RED 证据成立，继续 Step 3 实施 fix
>    - 0 次 fail（10 次全 PASS）→ **升级 muzhou 决策**："macOS 当前 P95 已稳定（10 runs 全 PASS），本 fix 是否还必需？" via AskUserQuestion；
>      - 选项 A: 继续 ship（fix 防御未来回归，零代价）
>      - 选项 B: abort Plan（macOS 当前无 friction，等下次 flake 复发再起 Plan）
> 3. 全部 10 次 P95 数据（含 mean / max）保留到 commit message body 作长期 baseline 参考

> **Note**: 此 Step 是 TDD 红绿循环中的 "RED" — 通过环境实证而非新增 test 类。fix 后 GREEN = annotation 让 method skip，对照 PASS 数 4→4 + Skipped 0→1。

- [ ] **Step 3: 编写 fix（加 import + annotation + Javadoc 增补）**

```java
// AsyncPipelineIntegrationTest.java

// ── 新增 imports（按字母序插到现有 imports 中 — 与 existing 风格一致）
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

// ── 新增 annotation + Javadoc 增补（替换原 line 261-273 Javadoc + @Test）
    /**
     * Performance gate: 95th-percentile (P95) async inbound latency must be
     * below 15ms over a 100-iteration sample (after a 5-iteration warmup that
     * primes the XSD schema cache).
     *
     * <p>Replaces the previous mean&lt;5ms gate (TD12) which was flaky on
     * shared CI hardware: a single GC pause in the 100-sample run could push
     * the arithmetic mean over 5ms even though the steady-state path stayed
     * fast. P95 absorbs single outliers while still catching real
     * degradations (e.g. uncached JAXBContext, lost XSD cache).</p>
     *
     * @implNote 2026-05-26: skipped on macOS via {@link DisabledOnOs}. Root
     * cause: macOS host scheduler / GC pause / system load (spotlight,
     * TimeMachine, concurrent mvn fork JVMs) drive per-call latency outliers
     * into the top 5% of the 100-sample distribution, pushing P95 well over
     * 15ms (observed 17.94 - 122ms across daily reports). GHA Ubuntu CI runs
     * stable at P95 = 1.14 - 3.14ms (max=5.02ms) across 4 cross-validation
     * runs — 13x safety margin. See investigate report at
     * {@code /Users/muzhou/FEP/docs/daily_reports/2026-05-26-async-pipeline-p95-investigate-report.md}
     * (local-only, non-git-tracked — muzhou private workspace).
     *
     * <p>macOS developers who need to force-run this test locally:</p>
     * <ol>
     *   <li>IDE: right-click method → Run (IDE overrides condition evaluation)</li>
     *   <li>Temporarily comment out {@code @DisabledOnOs} annotation</li>
     *   <li>Run inside a Linux Docker container</li>
     * </ol>
     */
    @Test
    @DisabledOnOs(value = OS.MAC, disabledReason =
            "Wall-clock perf gate is sensitive to macOS host scheduler/GC variance "
                    + "(see @implNote). GHA Ubuntu CI provides the steady baseline.")
    void performanceBaseline_100AsyncInbound_shouldHaveP95LessThan15ms() {
        // ... (函数体保持不变 — 仅注解 + Javadoc 改动)
```

> **重要**: 函数体（line 274-317）一字不改 — 仅头部加 annotation + Javadoc 替换。Edit 工具应用 `old_string` 必须包含 line 261 起的整段原 Javadoc + `@Test` + 方法签名 line 273 直到 `void performanceBaseline_...() {`，`new_string` 替换为上述新版本。

- [ ] **Step 4: TDD - GREEN 阶段实测 fix 行为切换**（v0.3 CONCERN-2-1 修订：拆 `tee | tail` 为两步，避免 pipe buffer 阻塞）

```bash
cd /Users/muzhou/FEP_v1.0_wt-asyncpipeline-flake
# Step 4a: 跑测试 + 完整日志落盘（不 pipe 到 tail，避免 mvn 输出 > 65KB pipe buffer 阻塞）
./mvnw test -pl fep-processor -Dtest=AsyncPipelineIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false > /tmp/asyncpipeline-fix-green.log 2>&1
echo "EXIT_CODE: $?"
# Step 4b: 独立读末尾 + grep 关键断言
tail -20 /tmp/asyncpipeline-fix-green.log
# 期望（macOS）:
#   [INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 1
#   [INFO] BUILD SUCCESS
# Step 4c: 检查 surefire-reports XML 含 disabled reason:
grep -A2 "performanceBaseline" fep-processor/target/surefire-reports/com.puchain.fep.processor.pipeline.AsyncPipelineIntegrationTest.txt
# 期望: 方法标记 disabled，reason 文本含 "Wall-clock perf gate is sensitive to macOS"
```

- [ ] **Step 5: 全 fep-processor 模块 verify（含 ArchUnit / Checkstyle / SpotBugs / find-sec-bugs / JaCoCo）**（红线 `feedback_full_regression_before_commit`）（v0.3 CONCERN-2-1 修订：拆 `tee | tail` 为两步）

```bash
cd /Users/muzhou/FEP_v1.0_wt-asyncpipeline-flake
# Step 5a: 跑全模块 verify + 完整日志落盘（不 pipe 到 tail，避免 mvn 输出 > 65KB pipe buffer 阻塞 — fep-processor 全模块 verify 常 100KB+）
./mvnw verify -pl fep-processor --batch-mode --no-transfer-progress > /tmp/asyncpipeline-fix-verify.log 2>&1
echo "EXIT_CODE: $?"
# Step 5b: 独立读末尾 + grep 关键 gate
tail -30 /tmp/asyncpipeline-fix-verify.log
grep -E "BUILD (SUCCESS|FAILURE)|Tests run.*Skipped|SpotBugs.*violations|Checkstyle.*violations" /tmp/asyncpipeline-fix-verify.log
# 期望: BUILD SUCCESS
# - Tests run: <N>, Failures: 0, Errors: 0, Skipped: 1 (从原 0 加 1)
# - SpotBugs: 无新增 finding
# - Checkstyle: 无新增 violation
# - find-sec-bugs: 无新增（注解非 CRLF logger 调用 — 不触发 LogSanitizer + @SuppressFBWarnings 配对要求，红线 `feedback_logsanitizer_alone_insufficient_for_findsecbugs` 不适用）
# - JaCoCo: 行覆盖不会因 skip 1 perf test 而下降（perf test 已被 4 个 asyncFlow_*_3001/3003/3005/xsdFailure test 覆盖同代码路径 — line coverage 不依赖此 perf test）
```

> **沙盒风险防御**（红线 `feedback_mvn_sandbox_exit144_pattern`）：若 mvn exit 144 ≥2 次，跳本机 mvn + 静态等价 review（diff 仅 +4 行 import/annotation/javadoc，spotbugs/checkstyle 规则集对 annotation 行无适用规则，可静态推断绿）+ GHA CI 兜底（待 billing 解决）。

> **单模块 verify 豁免显式声明**（v0.2 CONCERN-3 修订补充，红线 `feedback_full_regression_before_commit` 触发条件评估）：本变更 test-only 仅改 `AsyncPipelineIntegrationTest.java` 1 个测试类的 annotation + Javadoc，**无 production class / 无 body POJO / 无 converter / 无 processor 新类引入**；红线 `feedback_full_regression_before_commit` 触发条件"新增 body/processor/converter 类 commit 前须跑全模块 mvn test"不命中（本 Plan 修改既有测试，非新增 production 类），单模块 verify 充分；CI 兜底跑完整 reactor verify（待 GHA billing 解决）。

- [ ] **Step 6: 9 项 AI 代码自检（红线强制）**

对照 `docs/guides/ai-code-review-checklist.md` 逐项：

| # | 检查项 | 本 Task 评估 |
|:-:|--------|------------|
| 1 | 无吞异常 / 无空 catch | ✅ 无 try/catch 改动 |
| 2 | 测试断言验证业务含义 | ✅ 无新增断言；既有 P95 < 15.0 断言保留（Linux 跑 GHA 兜底） |
| 3 | 边界覆盖：null / 空集合 / 极限值 | ✅ N/A（无 production 代码改动） |
| 4 | 日志无敏感数据 | ✅ N/A（无新增 logger 调用） |
| 5 | 无未使用的抽象 / 泛型 / 参数 | ✅ 仅 import 已用 annotation/OS |
| 6 | 无硬编码（URL/密码/密钥/路径/超时） | ✅ disabledReason 含 "macOS"字串非硬编码业务参数；15ms gate 既有未改 |
| 7 | 公共类/方法有 Javadoc | ✅ Javadoc 完整含 @implNote |
| 8 | 禁用 System.out / printStackTrace | ✅ 无 println |
| 9 | 与本模块已有代码风格一致 | ✅ import 按字母序插入 condition 包；annotation 与 `@Test` 同层；Javadoc 风格与既有 Javadoc 一致 |

- [ ] **Step 7: Commit**

```bash
cd /Users/muzhou/FEP_v1.0_wt-asyncpipeline-flake
git add fep-processor/src/test/java/com/puchain/fep/processor/pipeline/AsyncPipelineIntegrationTest.java
git add docs/plans/2026-05-26-asyncpipeline-perf-flake-disable-on-mac.md
git status --short  # 期望: M fep-processor/...AsyncPipelineIntegrationTest.java + A docs/plans/2026-05-26-asyncpipeline-perf-flake-disable-on-mac.md
git commit -m "$(cat <<'EOF'
test(processor): skip AsyncPipeline P95 perf gate on macOS

Add @DisabledOnOs(MAC) to performanceBaseline_100AsyncInbound_*. Root
cause: macOS host scheduler/GC variance under load drives per-call
latency outliers, pushing P95 to 17-122ms vs 15ms gate. GHA Ubuntu CI
runs at P95=1.14-3.14ms (13x margin) — no flake. See investigate report
at /FEP/docs/daily_reports/2026-05-26-async-pipeline-p95-investigate-report.md.

Eliminates local mvnw verify friction (recurring surefire rerun noise).
GHA CI retains the regression catcher for uncached JAXBContext / lost
XSD cache scenarios.

macOS dev force-run options documented in @implNote.

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
git log -1 --oneline
```

---

## Task 2: Closing — 推送分支 + 文档同步 + worktree 移除 `模式 A`

**追溯 ID:** N/A（基础设施 closing）

**验收标准:**
1. `chore/asyncpipeline-disable-on-mac` 分支推到 `origin`（GHA billing 解决后自动触发 PR CI；当前 deferred — 红线 `feedback_systemic_ci_blocker_defers_positive_backing`）
2. `/Users/muzhou/FEP/CLAUDE.md` "下一步候选 #8" 段落更新结论（fix Plan 已 ship → ✅ CLOSED 或 DONE）
3. 红线 `feedback_macos_apfs_fork_classloader_race` 案例附录加 2026-05-26 4 GHA vs N macOS 二分实证 + fix 方案登记
4. PR （等 billing）已提到 GitHub，或本地 push 后 PR 创建可由 muzhou 行政完成
5. worktree `/Users/muzhou/FEP_v1.0_wt-asyncpipeline-flake` 已 `git worktree remove`
6. main worktree `git worktree list` 不再显示 wt-asyncpipeline-flake
7. **`<TBD>` SHA 占位实测全部回填**（v0.2 BLOCKER-2 修订补充）：`grep '<TBD>' /Users/muzhou/FEP/CLAUDE.md /Users/muzhou/.claude/projects/-Users-muzhou-FEP/memory/feedback_macos_apfs_fork_classloader_race.md` 期望 0 hit

**Files:**
- Modify (file write only, NO git commit): `/Users/muzhou/FEP/CLAUDE.md`（红线 `feedback_fep_docs_repo_commit_taboo`）
- Modify (file write only, NO git commit): `/Users/muzhou/.claude/projects/-Users-muzhou-FEP/memory/feedback_macos_apfs_fork_classloader_race.md`（memory 文件）
- Modify (file write only, NO git commit): `/Users/muzhou/FEP/docs/daily_reports/2026-05-26-asyncpipeline-fix-progress-report.md`（新 daily report）

- [ ] **Step 1: 推送分支**

```bash
cd /Users/muzhou/FEP_v1.0_wt-asyncpipeline-flake
git push -u origin chore/asyncpipeline-disable-on-mac
# 期望: Branch 'chore/asyncpipeline-disable-on-mac' set up to track 'origin/chore/asyncpipeline-disable-on-mac'.
```

- [ ] **Step 2: 创建 PR（gh CLI）**

```bash
cd /Users/muzhou/FEP_v1.0_wt-asyncpipeline-flake
gh pr create --base main --head chore/asyncpipeline-disable-on-mac \
  --title "test(processor): skip AsyncPipeline P95 perf gate on macOS" \
  --body "$(cat <<'EOF'
## 摘要

Add `@DisabledOnOs(MAC)` to `performanceBaseline_100AsyncInbound_*`。根因：macOS 本机 host scheduler/GC variance under load 让 wall-clock P95 飘到 17-122ms vs 15ms gate；GHA Ubuntu CI P95=1.14-3.14ms 13x 安全余量无 flake。

## 实证

| 环境 | P95 区间 |
|------|---------|
| GHA Ubuntu CI（4 cross-validation runs） | 1.14 - 3.14ms（max=5.02ms） |
| macOS local | 17.94 - 122ms |

详见 [investigate 报告](../../FEP/docs/daily_reports/2026-05-26-async-pipeline-p95-investigate-report.md)

## 测试

- macOS: `Tests run: 5, Failures: 0, Errors: 0, Skipped: 1` ✅
- Linux: 待 GHA billing 解决后 CI 兜底（既有 P95 < 15ms gate 保留）

## 影响

- 消除本机 `mvnw verify` 持续 fail / surefire rerun 噪声
- GHA CI 保留 uncached JAXBContext / lost XSD cache regression 捕获
- macOS dev 强制跑：IDE override / 注释 annotation / Linux Docker

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

> **PR CI 风险**（红线 `feedback_systemic_ci_blocker_defers_positive_backing`）：当前 GHA billing 未补缴 CI 不会跑。tier-A 静态验证（diff +4 行 import/annotation，无业务代码改动）+ 本机 Task 1 Step 4-5 实证已充分；tier-B GHA 全绿背书 deferred 至 billing 解决。

- [ ] **Step 2.5: 解析 commit SHA + 存在性验证 + 准备占位回填**（v0.2 BLOCKER-2 修订新增 / v0.3 CONCERN-2-2 修订加 `git rev-parse --verify` 兜底 typo）

```bash
cd /Users/muzhou/FEP_v1.0_wt-asyncpipeline-flake
SHA=$(git log -1 --format=%h)
echo "Resolved Task 1 Step 7 commit SHA: $SHA"
# 期望: 7 字符 git short SHA，非空
test -n "$SHA" || { echo "FATAL: SHA empty — Task 1 commit not found"; exit 1; }
# v0.3 CONCERN-2-2 修订：验证 SHA 真实存在（防主对话手抄 typo 后 Step 3/4 写入不存在的 SHA，验收 #7 grep '<TBD>' 0 hit 仍 PASS）
git rev-parse --verify "$SHA^{commit}" > /dev/null 2>&1 || { echo "FATAL: SHA $SHA does not resolve to a valid commit"; exit 1; }
echo "SHA $SHA verified as valid commit"
# 在后续 Step 3/4 Edit 命令中：old_string 含字面量 "<TBD>"，new_string 用 $SHA 替换
# 主对话执行 Edit 时手动注入 $SHA 值（Edit 工具不支持 bash 变量，需手抄 7 字符）
echo ""
echo "MANUAL ACTION: Step 3/4 Edit 命令落地 <TBD> 占位时用上方 SHA 值 $SHA 替换"
echo "手抄后建议立即跑: git rev-parse --verify <抄写值>  确认 typo 未发生"
```

> **执行纪律**：Step 2.5 必须在 Step 3 / Step 4 之前执行；Edit `<TBD>` 时主对话从 echo 输出读取 SHA 手抄注入。执行者不得跳过 Step 2.5 直接做 Step 3/4（会留 `<TBD>` 字面量）。Step 5 验收 #7 grep `<TBD>` 0 hit 是兜底闸 — fail 即 回到 Step 3/4 补回填。v0.3 加固后即使手抄 typo（写成不存在的 7 字符），由 Step 2.5 末尾的"建议跑 git rev-parse --verify <抄写值>" 提示在写入前抓 typo。

- [ ] **Step 3: 更新 /FEP/CLAUDE.md "下一步候选 #8"**（file write only, NO git commit — 红线 `feedback_fep_docs_repo_commit_taboo`）

将 `/Users/muzhou/FEP/CLAUDE.md` 中 "下一步候选 #8" 段落原文：

```
8. **root cause 7 P5 flaky cron evidence 复核** — Opt3 @DirtiesContext 已 ship `77bdd1e`（DONE_WITH_CONCERNS）；7-30 天 cron 累积后 grep P5 fail 率：≤10% → ✅ CLOSED / ≥30% → 升级 BEFORE_EACH_TEST_METHOD 或 Opt2 reuseForks（独立 Plan）。AsyncPipelineIntegrationTest P95 perf flake 机负载敏感，GHA CI 兜底
```

替换为：

```
8. **root cause 7 P5 flaky cron evidence 复核** — Opt3 @DirtiesContext 已 ship `77bdd1e`（DONE_WITH_CONCERNS）；7-30 天 cron 累积后 grep P5 fail 率：≤10% → ✅ CLOSED / ≥30% → 升级 BEFORE_EACH_TEST_METHOD 或 Opt2 reuseForks（独立 Plan）。~~AsyncPipelineIntegrationTest P95 perf flake 机负载敏感，GHA CI 兜底~~ → **✅ CLOSED 2026-05-26** AsyncPipeline P95 perf flake @DisabledOnOs(MAC) fix Plan ship（commit `<TBD>`）+ 红线 `feedback_macos_apfs_fork_classloader_race` 案例 3 加 GHA vs macOS 二分实证
```

> `<TBD>` 在 Task 1 Step 7 commit 完成后回填具体 SHA。

- [ ] **Step 4: 更新红线 `feedback_macos_apfs_fork_classloader_race` 案例附录**（memory 文件，file write only — memory 由用户管理）

在 `/Users/muzhou/.claude/projects/-Users-muzhou-FEP/memory/feedback_macos_apfs_fork_classloader_race.md` "案例补充" 表格末尾加一行：

```
| 2026-05-26 | AsyncPipelineIntegrationTest P95 fix Plan（实证 + 修复）| GHA Ubuntu CI 4 runs 跨验证 P95=1.14-3.14ms 全绿 vs macOS 本机 N runs P95=17.94-122ms 全飘 — 完整二分确认 host scheduler/GC 敏感性 | `@DisabledOnOs(MAC)` ship commit `<TBD>`；Linux 保留兜底 |
```

并在 "性能基线类 flake 适配规则" 段末尾加：

```
- **AsyncPipelineIntegrationTest 已 disable on macOS（2026-05-26 commit `<TBD>`）**：本规则后续触发不再适用此 test；其他 wall-clock perf 测试新增时同等情境（GHA 稳 + macOS 飘）应同样加 `@DisabledOnOs(MAC)`
```

> `<TBD>` 在 Task 1 Step 7 commit 完成后回填具体 SHA。

- [ ] **Step 5: 创建 daily report**（file write only — `/FEP/docs/` 路径红线 `feedback_fep_docs_repo_commit_taboo`）

写 `/Users/muzhou/FEP/docs/daily_reports/2026-05-26-asyncpipeline-fix-progress-report.md`（参考 [`feedback_daily_report`](../../../.claude/projects/-Users-muzhou-FEP/memory/feedback_daily_report.md) 结构 — 含 §教训 章节）。

- [ ] **Step 6: worktree 闭环**（红线 `feedback_worktree_for_parallel_work` Plan T-closing 强制）

```bash
cd /Users/muzhou/FEP_v1.0
git worktree list
# 期望仍含 wt-asyncpipeline-flake — 准备移除
git worktree remove /Users/muzhou/FEP_v1.0_wt-asyncpipeline-flake
git worktree list
# 期望: 不再含 wt-asyncpipeline-flake
ls -la /Users/muzhou/FEP_v1.0_wt-asyncpipeline-flake 2>&1
# 期望: ls: cannot access ... No such file or directory
git branch -a | grep asyncpipeline
# 期望: 仅 remotes/origin/chore/asyncpipeline-disable-on-mac（本地分支已被 worktree remove 清理）
# 注：本地分支保留也可（git worktree remove 不删 branch ref）。若需清理: git branch -d chore/asyncpipeline-disable-on-mac
```

- [ ] **Step 7: NO git commit**（Task 2 全部步骤 file write only + git 元操作 — 红线 `feedback_plan_step3_commit_template_residue` + `feedback_fep_docs_repo_commit_taboo`）

> Task 2 Step 1-6 全部为非 git tracked 文件写入 + git 推送/元操作，**不产生新 commit**。Task 1 Step 7 commit 已是本 Plan 唯一 commit。

---

## 自检清单（红线强制）

### 1. PRD 覆盖度
✅ 本 Plan 为测试基础设施 / 元流程，无 PRD FR-ID 映射 — 适用 CLAUDE.md "Plan 治理" §"PRD 追溯" 例外条款。

### 2. 安全边界检查
✅ 无 SM2/SM3/SM4 / 密钥 / encrypt / decrypt / sign / 脱敏 / 审计 关键词。无 ⛔ 模式 E Task。

### 3. 占位符扫描
✅ Task 2 Step 3/4 含 `<TBD>` 是预计 commit SHA 回填位（必须在 Task 1 Step 7 commit 完成后填具体 SHA），由 v0.2 BLOCKER-2 修订新增 Step 2.5 自动解析 + Step 5 验收 #7 `grep '<TBD>' 期望 0 hit` 兜底闸保证不遗漏 — 受控。无 "TBD"/"TODO"/"待"/"后续"/"类似"在行为描述处。

### 4. 类型一致性
✅ `@DisabledOnOs` / `OS.MAC` 在 Step 3 import 段定义，Step 3 annotation 段引用 — 一致。

### 5. 测试命令可执行
✅ `mvn test -pl fep-processor -Dtest=AsyncPipelineIntegrationTest -Dsurefire.failIfNoSpecifiedTests=false` 实测命令；`-Dsurefire.failIfNoSpecifiedTests=false` 用 surefire 3.x 新参数名（红线 `feedback_surefire3_failifno_specified_tests_param_rename`）。

### 6. CLAUDE.md 更新
✅ Task 2 Step 3 包含 `/FEP/CLAUDE.md` "下一步候选 #8" 更新；file write only NO commit（红线 `feedback_fep_docs_repo_commit_taboo`）。

### 7. 验收标准完整性
✅ Task 0 / Task 1 / Task 2 验收标准均来自 root cause investigate 报告 + 红线既定规则，非代码推测。Task 1 验收 #1-#2 断言数（5/0/0/1）可手算：5 = 既有 4 asyncFlow + 1 perf；Skipped = 1（仅 perf 被 disable）。

### 8. 共享工具类无遗漏
✅ N/A — 单文件单方法注解。

### 9. 核心类职责边界
✅ N/A — 测试类。

### 10. Worktree 触发条件自检（红线 `feedback_worktree_for_parallel_work`）

| 触发条件 | 命中？ | 实证（2026-05-27 v0.2 修订日重测） |
|---------|--------|------|
| 跨 ≥ 3 个 Maven 模块 | ❌ | 仅 fep-processor 1 模块 |
| 与已签字未执行 Plan 并存 | ❌（理由更新） | v0.1 引用 `2026-05-26-pitest-maven-1.25.0-upgrade.md` 已 ship 2026-05-26 commit `df15613` 不再适用；本次 modi 评审实测 origin/main HEAD `df15613` 已含该升级 |
| ⛔ 安全 (`security/impl/`) 与 AI 并行 | ❌ | 无 security 涉及 |
| TLQ tongtech profile 联调 | ❌ | 无 |
| ≥ 5 min long-running verify 并行 | ❌ | fep-processor 单模块 verify ~60s |
| muzhou WIP 与 AI 任务并存 | ✅ | `git worktree list` 实测：`wt-callback-p2 b26f4a8` PR #27 in-flight + `wt-r-new-1-real-xsd 457a5e4` + `wt-simplify-q-drain 457a5e4` 三别会话 worktree 活跃；20 untracked Plans 在 main wt |

**单触发**（条件 #6 muzhou WIP 与 AI 并存）+ `feedback_worktree_isolates_fs_not_logic_domain` "多会话即触发" — **独立 worktree 必需**。
头部 `执行 Worktree:` 字段已填具体路径 `/Users/muzhou/FEP_v1.0_wt-asyncpipeline-flake` 与分支 `chore/asyncpipeline-disable-on-mac`。
Task 2 Step 6 含 `git worktree remove` 实测命令 — 闭环纪律满足。

---

## AI 独立评审报告

### 第 1 轮（v0.1）— 2026-05-27 by `pr-review-toolkit:code-reviewer` agentId `a162a1a18411e3bed`

**VERDICT**: 🟡 REVISE — 2 BLOCKER + 4 CONCERN

| 严重度 | ID | 摘要 | v0.2 落地状态 |
|--------|----|------|----|
| BLOCKER | 1 | baseline drift `457a5e4`→`df15613` 跨日（v0.1 起草 2026-05-26 → v0.2 评审 2026-05-27） | ✅ 已应用：头部 §自检 10 + Task 0 Step 1 注释 + baseline drift 历史段 |
| BLOCKER | 2 | `<TBD>` SHA 占位无自动回填机制，人工易遗漏致字面量永久污染 | ✅ 已应用：Task 2 Step 2.5 自动解析 + 验收 #7 grep 0 hit 兜底闸 |
| CONCERN | 2 | worktree 触发条件 #2 理由 stale（pitest Plan 已 ship） | ✅ 已应用：§自检 10 表 #2 改 ❌ + 头部 `执行 Worktree` 字段触发理由更新为单 #6 + 并排除项更新 |
| CONCERN | 3 | 单模块 verify 豁免显式声明 | ✅ 已应用：Task 1 Step 5 注脚补红线触发条件评估段 |
| CONCERN | 4 | `@EnabledOnOs(LINUX)` 备选 vs `@DisabledOnOs(MAC)` 取舍讨论缺失 | ✅ 已应用：设计背景段补"为什么 ...而非 `@EnabledOnOs(LINUX)`" 段 |
| CONCERN | 5 | RED 阶段 3/3 PASS 时量化判定标准缺失 | ✅ 已应用：Task 1 Step 2 补"3/3 PASS 处置流程"段（10 runs + muzhou 决策升级矩阵）|
| CONCERN | 6 | Javadoc investigate report 路径未标 local-only | ✅ 已应用：`{@code ...}` 后追加 `(local-only, non-git-tracked — muzhou private workspace)` |

**评审报告全文存证**: 主对话 / 上轮 reviewer agent 输出（agentId `a162a1a18411e3bed`）。

### 第 2 轮（v0.2）— 2026-05-27 by `pr-review-toolkit:code-reviewer` agentId `afe5a89ddbbb0a962`

**VERDICT**: ✅ PASS — 2 BLOCKER FIXED + 6 CONCERN 接受 + 仅 2 项 NEW CONCERN 🟡 工程次优可接受 — 可交 muzhou 签字

| 严重度 | ID | 摘要 | 落地 / 处置 |
|--------|----|------|----|
| BLOCKER-1 | ✅ FIXED | baseline drift 历史段 + Task 0 Step 1 注释完整 | 无残留 |
| BLOCKER-2 | ✅ FIXED | Task 2 Step 2.5 SHA 自动解析 + 验收 #7 grep 0 hit 兜底闸 | 1 处小瑕疵（Step 5 vs 验收 #7 编号表述模糊，实质防御机制完整）— 接受 |
| CONCERN 2/3/4/5/6 | ✅ FIXED | 全部修订位置 / 改动深度 / 红线引用对齐预期 | 无残留 |
| NEW CONCERN-2-1 | 🟡 工程次优 | Task 1 Step 4-5 含 `\| tee ... \| tail` 同步执行；字面不违反红线（同步前台执行 pipe 不死锁），但 mvn 输出 >65KB 可能短暂阻塞 | 见下方 v0.3 拍板 |
| NEW CONCERN-2-2 | 🟡 工程次优 | Step 2.5 SHA 手抄注入存 typo 风险，验收 #7 grep `<TBD>` 抓不到"手抄成 abc1234 但 SHA 不存在"场景 | 见下方 v0.3 拍板 |

**评审报告全文存证**: 主对话 / 第 2 轮 reviewer agent 输出（agentId `afe5a89ddbbb0a962`）。

### v0.3 boil-lake 加固（2026-05-27 muzhou 决策选 "Recommended" 全加固）

| NEW CONCERN | 加固位置 | 改动 |
|----|------|------|
| CONCERN-2-1 | Task 1 Step 4 / Step 5 | 拆 `tee \| tail` 为两步：(a) `mvn ... > /tmp/...log 2>&1` 完整日志落盘 (b) 独立 `tail -30 /tmp/...log` + `grep -E "BUILD ..."`。避免 pipe buffer ~65KB 阻塞，mvn 输出 100KB+ 安全 |
| CONCERN-2-2 | Task 2 Step 2.5 | 加 `git rev-parse --verify "$SHA^{commit}"` 验证 SHA 真实存在 + Step 末尾 echo 提示主对话手抄后跑 `git rev-parse --verify <抄写值>` 抓 typo |

**v0.3 状态**: 全部 v0.1 BLOCKER + CONCERN + v0.2 NEW CONCERN 均已 ✅ FIXED。无未决项。可直接 muzhou 签字 ship。

按 reviewer 建议：v0.3 加固为纯防御性改动（4 LOC 命令拆分 + 验证），不引入新业务逻辑，**无需第 3 轮 AI 评审**；直接进 muzhou 签字。

---

## Plan Approver (muzhou) 签字位

**状态：** ✅ 已签字 — 2026-05-27 muzhou via AskUserQuestion "签字 + 本会话进 Task 0/1/2 实施 (Recommended)"

**Plan Approver (muzhou):** ✅ APPROVED 2026-05-27 — v0.3 boil-lake；2 BLOCKER + 6 CONCERN + 2 NEW CONCERN 全部 FIXED；investigate root cause 实证清晰（GHA P95 1.14-3.14ms vs macOS 17.94-122ms 二分）；test-only 微改 ≈ 5 LOC + 1 import；macOS dev friction 解 + GHA Linux 保留 regression catch

签字格式（参考 `docs/guides/plan-review-checklist.md`）：

```
**Plan Approver (muzhou):** [✅ APPROVED | ❌ REJECTED | 🟡 REVISE] YYYY-MM-DD — <一句话决策依据>
```

**建议签字文本**（muzhou 拍板时可直接复用 / 修改）：

```
**Plan Approver (muzhou):** ✅ APPROVED 2026-05-27 — v0.3 boil-lake；2 BLOCKER + 6 CONCERN + 2 NEW CONCERN 全部 FIXED；investigate root cause 实证清晰（GHA P95 1.14-3.14ms vs macOS 17.94-122ms 二分）；test-only 微改 ≈ 5 LOC + 1 import；macOS dev friction 解 + GHA Linux 保留 regression catch
```

---

## GSTACK REVIEW REPORT

| Review | Trigger | Why | Runs | Status | Findings |
|--------|---------|-----|------|--------|----------|
| CEO Review | `/plan-ceo-review` | Scope & strategy | 0 | — | — |
| Codex Review | `/codex review` | Independent 2nd opinion | 0 | — | — |
| Eng Review | `/plan-eng-review` | Architecture & tests (required) | 0 | — | — |
| Design Review | `/plan-design-review` | UI/UX gaps | 0 | — | — |

**VERDICT:** NO REVIEWS YET — 按 FEP 治理流程派发 `code-reviewer` 或 `santa-method` agent 做 AI 独立评审；或运行 `/autoplan` 跑完整评审管线。
