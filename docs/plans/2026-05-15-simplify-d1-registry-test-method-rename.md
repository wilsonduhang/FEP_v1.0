# FEP Simplify D'.1 Registry Sanity Test Method Rename 实施计划

> **版本:** v0.1（2026-05-15 起草）
>
> **执行方式:** 单 Task 机械重命名，superpowers:executing-plans 内联执行或 subagent-driven 均可。步骤使用 `- [ ]` 复选框跟踪。

**目标:** 将 fep-processor 6 个 XSD 校验测试类中的 sanity-check 方法 `registry_should_supports_msg_NNNN` 重命名为 `registrySupportsMsgNNNN`，修复 snake_case→lowerCamelCase + 语法错误（`should_supports` → 动词原形缺失），统一项目命名约定。

**前置依赖:** 无（纯测试方法重命名，无业务逻辑改动 / 无 PRD FR 状态变更 / 无 schema 改动）

**执行 Worktree:** `/Users/muzhou/FEP_v1.0_wt-simplify-d1-rename`（分支 `chore/simplify-d1-registry-test-rename`，触发条件第 ② 项）

> 红线 `feedback_worktree_for_parallel_work` 触发条件: ① 跨 ≥3 模块 refactor ② 与已签字 Plan 并存 ③ ⛔ 安全 vs AI 并行 ④ TLQ tongtech 联调 ⑤ >5min long-running verify 并行 ⑥ muzhou WIP 与 AI 并存
>
> 命中 ②：Spring Boot 3.3.7→3.5.9 升级 Plan v2.1（`docs/plans/2026-05-15-spring-boot-parent-upgrade-3.5.9.md`，muzhou 已签字未执行，目标 worktree `wt-sb35-upgrade`）— 独立 worktree 防止主 branch HEAD 在本 Plan 实施期间被 SB 升级 commit 移位 + 并发 commit race condition。

**并排除项**（红线 `feedback_parallel_session_task_allocation_discipline` 强制声明）:

- **Spring Boot 3.3.7 → 3.5.9 升级 Plan v2.1**（`docs/plans/2026-05-15-spring-boot-parent-upgrade-3.5.9.md`，已签字未执行，目标 worktree `wt-sb35-upgrade`）锁定范围：根 + 8 模块 `pom.xml` BOM + 全 reactor verify (10-15 min) + Spring/Tomcat/Hibernate/log4j/Jackson 版本相关代码 + Spring Security 6 lambda DSL + spring-boot-properties-migrator。本 Plan 仅改 6 个 `*XsdValidationTest.java` 测试方法名，∩ pom.xml = ∅。
- **P4-MSG-F deferred drain Plan v0.1**（`docs/plans/2026-05-15-p4-msg-f-deferred-drain-parallel.md`，draft 未签字）锁定范围：`fep-web/.../BodyClassRegistryTest.java` + `fep-converter/.../OutboundWireShapeDispatcher.java` + `fep-processor/.../AbstractXsdValidationTest.java` + 6 个 **3001-3006** XSD 测试（ProgressQuery3001 / ProgressQueryReturn3002 / PzInfoQuery3003 / PzInfoReturn3004 / QyAccQuery3005 / QyAccQueryReturn3006）。本 Plan 改动文件集 = {DataTransfer1101, DataTransfer2101, CompanyInfoRequest1001, CompanyInfoResponse2001, CompanyAuthFileTransfer1004, CompanyAuthFileResponse2004}XsdValidationTest，与其 **3001-3006** 集合及 AbstractXsdValidationTest **完全不相交**（grep 实测交集 = ∅，见下文“grep 实测交集”段）。
- **e2e 远古 worktree**（`/Users/muzhou/FEP_v1.0.e2e` 分支 `e2e/p7.1-smoke-local`）锁定范围：fep-admin-ui 前端 / Playwright e2e — 与本 Plan 物理隔离（fep-admin-ui 不在本 Plan 改动列表）。

**架构:** 6 个独立测试类，每类恰 1 个目标方法，方法体为单行 `assertThat(SHARED_REGISTRY.schemaOf(MessageType.MSG_NNNN)).isNotNull();`，无 `@DisplayName`，无类内/跨模块外部引用（grep 实测）。纯 mechanical rename，行为完全等价，零运行时影响。

**技术栈:** Java 17 / Spring Boot 3.3.7 / Maven / JUnit 5 / AssertJ

**AI 协同模式:**

| 模式 | AI 占比 | 本计划中的应用 |
|:----:|:-------:|--------------|
| A | 90% | T1 测试方法机械重命名（无业务逻辑 / 无新断言 / 行为等价） |
| B | 70% | 不涉及 |
| C | 60% | 不涉及 |
| D | 50% | 不涉及 |
| E | 0%  | ⛔ 无国密代码改动 |

---

## 设计背景

### Why now（动因）

2026-05-15 R1 Shared Registry Residual Refactor 闭环 Simplify 三审产出 11 项 deferred。其中 D-Q1（quality-polish-should_supports-rename，next-session-prompt §107-110 轨 D'.1）为 LOW risk 独立小 ticket。2026-05-15 session-end muzhou 拍板原选 #1 = 轨 D'.2 simplify-xsd-test-shared-helpers-v2；但本会话开局 grep 实测发现并行会话已起草 P4-MSG-F deferred drain Plan v0.1（未签字），其 DEF-R1 与 D'.2 在 `AbstractXsdValidationTest.java` 直接撞车。经 AskUserQuestion，muzhou 改选 **轨 D'.1**（隔离 rename，与所有并行轨文件集不相交），本 Plan 即 D'.1。

### What changes（修改范围）

**6 文件 / 1 模块（fep-processor/test）/ 6 方法重命名**：

| 文件 | 旧方法名（行号实测） | 新方法名 |
|---|---|---|
| `CompanyInfoRequest1001XsdValidationTest.java` | `registry_should_supports_msg_1001`（L231）| `registrySupportsMsg1001` |
| `CompanyInfoResponse2001XsdValidationTest.java` | `registry_should_supports_msg_2001`（L235）| `registrySupportsMsg2001` |
| `CompanyAuthFileTransfer1004XsdValidationTest.java` | `registry_should_supports_msg_1004`（L211）| `registrySupportsMsg1004` |
| `CompanyAuthFileResponse2004XsdValidationTest.java` | `registry_should_supports_msg_2004`（L241）| `registrySupportsMsg2004` |
| `DataTransfer1101XsdValidationTest.java` | `registry_should_supports_msg_1101`（L224）| `registrySupportsMsg1101` |
| `DataTransfer2101XsdValidationTest.java` | `registry_should_supports_msg_2101`（L166）| `registrySupportsMsg2101` |

所在目录：`fep-processor/src/test/java/com/puchain/fep/processor/validation/`

### grep 实测交集（红线 `feedback_parallel_session_task_allocation_discipline` Step 2 规则 1 验证）

- 本 Plan 改动文件集 ∩ SB 3.5.9 Plan 锁定文件（pom.xml ×9）= ∅
- 本 Plan 改动文件集 ∩ P4-MSG-F drain 锁定文件集 = ∅
  - P4-MSG-F drain DEF-R1 = {AbstractXsdValidationTest, ProgressQuery3001, ProgressQueryReturn3002, PzInfoQuery3003, PzInfoReturn3004, QyAccQuery3005, QyAccQueryReturn3006}
  - 本 Plan = {DataTransfer1101, DataTransfer2101, CompanyInfoRequest1001, CompanyInfoResponse2001, CompanyAuthFileTransfer1004, CompanyAuthFileResponse2004}
  - 两集合按文件名逐一比对无交集；本 Plan 不读写 AbstractXsdValidationTest（仅继承其 `SHARED_REGISTRY` 静态字段，不修改基类）
- 本 Plan 改动文件集 ∩ e2e worktree 锁定（fep-admin-ui/**）= ∅
- 外部引用 grep 实测（`grep -rn "registry_should_supports_msg" --include="*.java" .` 排除 target）：仅这 6 文件各 1 处，无反射 / 无 `@MethodSource` / 无 ArchUnit 规则按此名断言 / 无文档引用 → rename 零外溢

### PRD 追溯

本 Plan 为**测试基础设施 quality-polish 重构**（Simplify 三审 deferred 消化），无 PRD 功能需求映射，无 FR-ID 状态变更（CLAUDE.md Plan 治理“基础设施/元流程 Plan 除外”条款适用，等同既往 E-NIT-1 / R1 等 Simplify deferred drain Plan）。`prd-traceability-matrix.md` 无需更新。

---

## Task 1: 6 XSD 测试类 sanity-check 方法重命名 `模式 A`

**PRD 依据:** 不适用（测试基础设施重构，CLAUDE.md Plan 治理基础设施例外条款）
**追溯 ID:** 不适用（无 FR-ID 映射）

**验收标准（行为等价 — 重命名前后语义不变）:**
1. 6 个旧方法名 `registry_should_supports_msg_NNNN` 在 fep-processor 全模块 grep（排除 target）结果数 = 0（重命名彻底，无残留）
2. 6 个新方法名 `registrySupportsMsgNNNN`（NNNN ∈ {1001,2001,1004,2004,1101,2101}）各存在 1 处，仍带 `@Test` 注解，方法体逐字节不变（仅方法名 token 变更）
3. `mvn test -pl fep-processor`：6 个重命名方法全部 PASS，fep-processor 模块测试总数与重命名前一致（实测既有 452，rename 不增减），0 fail / 0 error
4. 无 `@DisplayName` 新增（与本 6 文件既有约定一致 — grep 实测 6 文件均无 `@DisplayName`，质量自检 #9 模块风格一致）

> **规则**: 本 Task 为机械重命名，无新业务断言。验收以“行为等价 + 命名修正”为准，不引入新测试逻辑。

**Files:**
- Modify: `fep-processor/src/test/java/com/puchain/fep/processor/validation/CompanyInfoRequest1001XsdValidationTest.java`
- Modify: `fep-processor/src/test/java/com/puchain/fep/processor/validation/CompanyInfoResponse2001XsdValidationTest.java`
- Modify: `fep-processor/src/test/java/com/puchain/fep/processor/validation/CompanyAuthFileTransfer1004XsdValidationTest.java`
- Modify: `fep-processor/src/test/java/com/puchain/fep/processor/validation/CompanyAuthFileResponse2004XsdValidationTest.java`
- Modify: `fep-processor/src/test/java/com/puchain/fep/processor/validation/DataTransfer1101XsdValidationTest.java`
- Modify: `fep-processor/src/test/java/com/puchain/fep/processor/validation/DataTransfer2101XsdValidationTest.java`

- [ ] **Step 1: 创建 worktree（隔离并行 SB3.5.9 已签字 Plan）**

```bash
cd /Users/muzhou/FEP_v1.0
git worktree add -b chore/simplify-d1-registry-test-rename /Users/muzhou/FEP_v1.0_wt-simplify-d1-rename main
cd /Users/muzhou/FEP_v1.0_wt-simplify-d1-rename
git log -1 --oneline   # 期望: 与 origin/main HEAD 一致 (基线 baseline 记录)
```

- [ ] **Step 2: 基线确认 — 6 个旧方法名测试当前 GREEN**

```bash
cd /Users/muzhou/FEP_v1.0_wt-simplify-d1-rename
JAVA_HOME=/usr/local/opt/openjdk@17 PATH=/usr/local/opt/openjdk@17/bin:$PATH \
  ./mvnw test -pl fep-processor -am \
  -Dtest='CompanyInfoRequest1001XsdValidationTest#registry_should_supports_msg_1001+CompanyInfoResponse2001XsdValidationTest#registry_should_supports_msg_2001+CompanyAuthFileTransfer1004XsdValidationTest#registry_should_supports_msg_1004+CompanyAuthFileResponse2004XsdValidationTest#registry_should_supports_msg_2004+DataTransfer1101XsdValidationTest#registry_should_supports_msg_1101+DataTransfer2101XsdValidationTest#registry_should_supports_msg_2101' \
  -Dsurefire.failIfNoSpecifiedTests=false --batch-mode --no-transfer-progress
```
期望: `BUILD SUCCESS`, Tests run: 6, Failures: 0, Errors: 0（基线绿，证明重命名前可执行）
> 红线 `feedback_surefire3_failifno_specified_tests_param_rename`：用 `-Dsurefire.failIfNoSpecifiedTests=false`（非 deprecated `-DfailIfNoTests`）。
> 红线 `feedback_bg_bash_path_inheritance`：mvn 前缀 JAVA_HOME/PATH（若后台跑）。

- [ ] **Step 3: 重命名 6 方法（Edit 逐文件，仅方法名 token，方法体/注解不动）**

对每个文件执行 Edit，`old_string` → `new_string`：

| 文件 | old_string | new_string |
|---|---|---|
| CompanyInfoRequest1001XsdValidationTest.java | `void registry_should_supports_msg_1001() {` | `void registrySupportsMsg1001() {` |
| CompanyInfoResponse2001XsdValidationTest.java | `void registry_should_supports_msg_2001() {` | `void registrySupportsMsg2001() {` |
| CompanyAuthFileTransfer1004XsdValidationTest.java | `void registry_should_supports_msg_1004() {` | `void registrySupportsMsg1004() {` |
| CompanyAuthFileResponse2004XsdValidationTest.java | `void registry_should_supports_msg_2004() {` | `void registrySupportsMsg2004() {` |
| DataTransfer1101XsdValidationTest.java | `void registry_should_supports_msg_1101() {` | `void registrySupportsMsg1101() {` |
| DataTransfer2101XsdValidationTest.java | `void registry_should_supports_msg_2101() {` | `void registrySupportsMsg2101() {` |

> 仅替换方法声明行的方法名标识符。`@Test` 注解、方法体单行 `assertThat(SHARED_REGISTRY.schemaOf(MessageType.MSG_NNNN)).isNotNull();`、缩进、闭合花括号一律不动。

- [ ] **Step 4: 重命名彻底性 grep 校验（验收标准 1+2）**

```bash
cd /Users/muzhou/FEP_v1.0_wt-simplify-d1-rename
echo "=== 旧名残留（期望 0）===" 
grep -rn "registry_should_supports_msg" --include="*.java" . | grep -v "/target/" | wc -l
echo "=== 新名计数（期望各 1，共 6）==="
grep -rn "void registrySupportsMsg\(1001\|2001\|1004\|2004\|1101\|2101\)()" --include="*.java" fep-processor/src/test | wc -l
echo "=== @Test 仍在（期望每文件命中）==="
for f in CompanyInfoRequest1001 CompanyInfoResponse2001 CompanyAuthFileTransfer1004 CompanyAuthFileResponse2004 DataTransfer1101 DataTransfer2101; do grep -B1 "void registrySupportsMsg" "fep-processor/src/test/java/com/puchain/fep/processor/validation/${f}XsdValidationTest.java" | grep -q "@Test" && echo "$f OK" || echo "$f MISSING @Test"; done
```
期望: 旧名残留 = 0；新名计数 = 6；6 文件全部 `OK`

- [ ] **Step 5: 重命名后 6 测试 GREEN（验收标准 3，新方法名）**

```bash
cd /Users/muzhou/FEP_v1.0_wt-simplify-d1-rename
JAVA_HOME=/usr/local/opt/openjdk@17 PATH=/usr/local/opt/openjdk@17/bin:$PATH \
  ./mvnw test -pl fep-processor -am \
  -Dtest='CompanyInfoRequest1001XsdValidationTest#registrySupportsMsg1001+CompanyInfoResponse2001XsdValidationTest#registrySupportsMsg2001+CompanyAuthFileTransfer1004XsdValidationTest#registrySupportsMsg1004+CompanyAuthFileResponse2004XsdValidationTest#registrySupportsMsg2004+DataTransfer1101XsdValidationTest#registrySupportsMsg1101+DataTransfer2101XsdValidationTest#registrySupportsMsg2101' \
  -Dsurefire.failIfNoSpecifiedTests=false --batch-mode --no-transfer-progress
```
期望: `BUILD SUCCESS`, Tests run: 6, Failures: 0, Errors: 0

- [ ] **Step 6: fep-processor 全模块回归（红线 `feedback_full_regression_before_commit`）**

```bash
cd /Users/muzhou/FEP_v1.0_wt-simplify-d1-rename
JAVA_HOME=/usr/local/opt/openjdk@17 PATH=/usr/local/opt/openjdk@17/bin:$PATH \
  ./mvnw test -pl fep-processor -am --batch-mode --no-transfer-progress
```
期望: `BUILD SUCCESS`, Tests run 与重命名前一致（既有 452，rename 不增减），0 fail / 0 error
> 若本机 macOS APFS fork classloader race / mvn 沙盒 exit 144（红线 `feedback_macos_apfs_fork_classloader_race` / `feedback_mvn_sandbox_exit144_pattern`）→ pkill+clean+retry 一次；仍失败则 GHA CI 远端兜底，commit footer 透明披露本机未跑通原因。

- [ ] **Step 7: 9 项质量自检对照**

| # | 检查项 | 本 Task 结论 |
|:-:|--------|---|
| 1 | 无吞异常 / 无空 catch | N/A（无 catch 改动）|
| 2 | 测试断言验证业务含义 | 保留原 `assertThat(...schemaOf...).isNotNull()` 不变，非假断言 |
| 3 | 边界覆盖 | N/A（行为等价重命名）|
| 4 | 日志无敏感数据 | N/A（无日志改动）|
| 5 | 无未使用抽象/泛型/参数 | N/A |
| 6 | 无硬编码 | N/A（无新逻辑）|
| 7 | 公共类/方法 Javadoc | N/A（package-private 测试方法，项目约定测试方法无 Javadoc）|
| 8 | 禁用 System.out / printStackTrace | grep 实测 6 文件无 |
| 9 | 与模块风格一致 | ✅ 6 文件既有约定无 `@DisplayName`，本 Task 不新增；新名 `registrySupportsMsgNNNN` 为 lowerCamelCase 符合编码规范 |

- [ ] **Step 8: 提交（worktree 分支）**

```bash
cd /Users/muzhou/FEP_v1.0_wt-simplify-d1-rename
git add fep-processor/src/test/java/com/puchain/fep/processor/validation/CompanyInfoRequest1001XsdValidationTest.java \
        fep-processor/src/test/java/com/puchain/fep/processor/validation/CompanyInfoResponse2001XsdValidationTest.java \
        fep-processor/src/test/java/com/puchain/fep/processor/validation/CompanyAuthFileTransfer1004XsdValidationTest.java \
        fep-processor/src/test/java/com/puchain/fep/processor/validation/CompanyAuthFileResponse2004XsdValidationTest.java \
        fep-processor/src/test/java/com/puchain/fep/processor/validation/DataTransfer1101XsdValidationTest.java \
        fep-processor/src/test/java/com/puchain/fep/processor/validation/DataTransfer2101XsdValidationTest.java
git commit -m "$(cat <<'EOF'
refactor(processor-test): D1 rename registry_should_supports_msg_NNNN to registrySupportsMsgNNNN

Rename 6 sanity-check methods in XSD validation tests (1001/2001/1004/2004/1101/2101)
to lowerCamelCase, fixing snake_case + grammar (should_supports -> supports). Behavior
unchanged: method bodies, @Test annotations, assertions all identical. Simplify R1
three-review deferred D-Q1 drain.

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
git log -1 --oneline   # 记录 commit SHA
```

---

## Task 2: closing — merge + 文档收尾 `模式 A`

> 本 Task 在 muzhou 决策 merge 后执行；session-end Phase 2 统一做四步收尾。

- [ ] **Step 1: merge 回 main worktree + push（待 muzhou 决策）**

```bash
cd /Users/muzhou/FEP_v1.0
git fetch origin --quiet
git log origin/main..main --oneline   # 红线 feedback_main_worktree_unpushed_wip_detection：确认主 worktree 无别会话未推送 commit；若非空，先告警不接管
git checkout main && git merge --ff-only chore/simplify-d1-registry-test-rename
git push origin main
git log -1 --oneline origin/main
```
> 若 `git log origin/main..main` 非空（别会话 WIP）→ 停止，告警 muzhou，不被动承担推送（红线 `feedback_main_worktree_unpushed_wip_detection` + `feedback_new_session_no_intervention_in_prior_tasks`）。

- [ ] **Step 2: worktree 闭环（红线 `feedback_worktree_for_parallel_work` 闭环纪律）**

```bash
cd /Users/muzhou/FEP_v1.0
git worktree remove /Users/muzhou/FEP_v1.0_wt-simplify-d1-rename
git branch -d chore/simplify-d1-registry-test-rename
git worktree list   # 确认本 ticket worktree 已移除；main + .e2e 保留；wt-sb35-upgrade 若存在为 SB 升级别会话所有（informational，非 gate）
```

- [ ] **Step 3: 更新 CLAUDE.md “当前项目状态”段（file write only，红线 `feedback_fep_docs_repo_commit_taboo`：`/FEP/CLAUDE.md` 非 git tracked，禁 git add/commit）**

追加本阶段闭环条目（commit SHA / 6 文件 / 测试数不变 / Simplify D-Q1 drain / 与并行 SB3.5.9 + P4-MSG-F drain 隔离）。

---

## 自检清单结论

| # | 项 | 结论 |
|:-:|---|---|
| 1 | PRD 覆盖度 | N/A — 测试基础设施重构，基础设施例外条款；prd-traceability-matrix.md 无需更新 |
| 2 | 安全边界 | ✅ 无 SM2/SM3/SM4/密钥/脱敏/审计 关键词，无 ⛔ 模式 E Task |
| 3 | 占位符扫描 | ✅ 无 TBD/TODO/待/后续/类似/参考 Task |
| 4 | 类型一致性 | ✅ 6 新方法名独立无交叉引用 |
| 5 | 测试命令可执行 | ✅ `-Dtest=` 类名#方法名 与实测 grep 一致；`-Dsurefire.failIfNoSpecifiedTests=false` |
| 6 | CLAUDE.md 更新 | ✅ Task 2 Step 3（file write only）|
| 7 | 验收标准完整性 | ✅ 4 项均可 grep/mvn 实测，不依赖代码推测 |
| 8 | 共享工具类 | N/A — 无新工具类 |
| 9 | 核心类职责边界 | N/A — 无 ≥3 依赖 Service 改动 |
| 10 | Worktree 触发自检 | ✅ 命中 ②（SB3.5.9 已签字未执行 Plan 并存）→ 头部声明 worktree 路径+分支；Task 2 含 `git worktree remove` 实测 |

---

## 执行交接

⚠️ 本 Plan 不能直接执行。流程：
1. **AI 独立评审**（code-reviewer / general-purpose subagent）对照 `docs/guides/plan-review-checklist.md` 7 项 + 本 Plan grep 实测交集独立复核
2. **muzhou 签字**（Plan 末尾追加批准）
3. 签字后选 subagent-driven / 内联执行

**禁止：未签字直接执行。**

---

## muzhou 批准签字

> 本段在 AI 评审完成后追加，muzhou 签字后才能进入实施。

- **AI 评审**: ✅ PASS（general-purpose 独立 reviewer，2026-05-15）— 独立重跑 grep 验证 6 文件 ∩ SB3.5.9 pom.xml = ∅、∩ P4-MSG-F drain DEF-R1 {AbstractXsdValidationTest + 3001-3006} = ∅；executability（./mvnw / BRE alternation / `-Dsurefire.failIfNoSpecifiedTests=false` / JUnit5 `A#m+B#m`）全 PASS；plan-review-checklist 7 项无违规；2 NIT（Task2 worktree list 措辞 + footnote 重复）已修
- **muzhou 批准**: ✅ APPROVED（2026-05-15）— 执行方式：subagent-driven-development
