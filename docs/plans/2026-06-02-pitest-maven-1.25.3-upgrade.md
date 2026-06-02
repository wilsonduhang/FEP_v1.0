# FEP pitest-maven 1.25.0 → 1.25.3 升级实施计划

> **执行方式:** 使用 superpowers:subagent-driven-development 或 superpowers:executing-plans 逐 Step 执行。步骤使用 `- [ ]` 复选框跟踪。

**目标:** 将根 `pom.xml` 中 `pitest-maven` 插件版本从 `1.25.0` 升至 Maven Central 最新 release `1.25.3`（同 1.25.x patch 线），并以 fep-common 实测 mutation score 回归验证升级无功能退化。

**前置依赖:** 无（infra / 依赖升级 Plan）

**执行 Worktree:** `/Users/muzhou/FEP_v1.0_wt-pitest-1253`（分支 `chore/pitest-1.25.3-upgrade`，基于 `origin/main@c74a1ce`）
> 红线 `feedback_worktree_isolates_fs_not_logic_domain` 触发：本会话与 ≥8 个别会话 worktree 并发活跃（callback-p2/p2b/p2b-sec/p4-msg-l/r3-transitionno/r3-xsd-constants/simplify-q-drain + mode2-mapper），"多会话活跃自身即触发"，文件级无交集亦不豁免。同时命中 CLAUDE.md 触发条件 #2（与多个已签字/在途 Plan 并存）。worktree 已于会话起始建立。

**架构:** 单点版本号修改 — 根 `pom.xml:479` `<version>1.25.0</version>` → `1.25.3`。`pitest-junit5-plugin` 保持 `1.2.3`（已是 Maven Central 最新，无需动）。pitest 仅在 `fep-common`（`fep-common/pom.xml:19` `<pitest.skip>false</pitest.skip>`）实际执行 mutation，其余模块 `pitest.skip=true`（根 `pom.xml:51`）。

**技术栈:** Java 17 / Spring Boot 3.x / Maven / pitest-maven (mutation testing)

**PRD 追溯:** 不适用 — 本 Plan 为构建基础设施 / 依赖升级，不映射 PRD 功能需求（CLAUDE.md Plan 治理"基础设施/元流程 Plan 除外"）。

**安全边界:** 不涉及 `security/impl/` 或国密代码，无 ⛔ 模式 E Task。

---

## 设计背景

### Currency 实测（红线 `feedback_currency_check_datasource_coverage` / `feedback_dependency_plan_currency_recheck`）

| 项 | 当前（grep 实测） | 目标（Maven Central 权威源） |
|---|---|---|
| pitest-maven | `1.25.0`（`pom.xml:479`） | **`1.25.3`** |
| pitest-junit5-plugin | `1.2.3`（`pom.xml:484`） | `1.2.3`（已最新，不动） |

**权威源**（2026-06-02 实测）:
- `https://repo1.maven.org/maven2/org/pitest/pitest-maven/maven-metadata.xml` → `<release>1.25.3</release>`，`<lastUpdated>20260529073535</lastUpdated>`
- `https://repo1.maven.org/maven2/org/pitest/pitest-junit5-plugin/maven-metadata.xml` → `<release>1.2.3</release>`

> backlog（CLAUDE.md 下一步候选 #7）"1.23.1→1.24.1" 已陈旧：当前实为 1.25.0（2026-05-26 `df15613` 升级未回写 backlog）。muzhou 2026-06-02 确认修正目标为 1.25.0→1.25.3。

### 升级性质与风险

- 1.25.0 → 1.25.3 为**同 minor（1.25.x）patch 线**升级，pitest 语义化版本 patch 仅 bugfix，API/配置不变。
- 现有 `pitest` profile 配置（`targetClasses` / `excludedClasses` / `mutationThreshold=80` / `coverageThreshold=80` / junit5-plugin 1.2.3）全部保持不变。
- 风险点：patch 版可能微调 mutation operator 集合或检测精度，导致 fep-common mutation score 浮动。验收以"score 仍 ≥ 阈值 80 且 BUILD SUCCESS"为准，不要求与 1.25.0 逐 mutation 一致。

### Maven phase 绑定（红线 `feedback_plan_mvn_phase_must_match_plugin_execution_binding`）

`pom.xml:515-522` pitest execution `<phase>verify</phase>`，goal `mutationCoverage`。故验证命令 phase 必须为 **`verify`**，**严禁用 `mvn test`**（test phase 早于 verify，pitest 不执行 → surefire 假绿 mutation 段空）。这是 2026-05-26 pitest 1.25.0 升级实证过的同型陷阱。

---

## Task 1: pitest-maven 1.25.0 → 1.25.3 版本升级 + mutation 回归 `模式 A`

**验收标准:**
1. `pom.xml:479` `<version>` 由 `1.25.0` 变为 `1.25.3`，无其他改动。
2. `pitest-junit5-plugin` 版本仍为 `1.2.3`（未被误改）。
3. `mvn verify -Ppitest -pl fep-common -am` 执行 → 控制台出现 pitest 实际运行标志（`>> Generated N mutations` / `>> Line Coverage` / `Mutators` 段），**非** BUILD SUCCESS 假绿。
4. fep-common mutation score ≥ 80（`mutationThreshold`）且 line coverage ≥ 80（`coverageThreshold`），BUILD SUCCESS。

**Files:**
- Modify: `pom.xml`（根，仅第 479 行 version 字段）

- [ ] **Step 1: 实施前 currency 重测（红线 `feedback_dependency_plan_currency_recheck` — 签字到实施跨时段须重测）**

```bash
cd /Users/muzhou/FEP_v1.0_wt-pitest-1253
curl -s "https://repo1.maven.org/maven2/org/pitest/pitest-maven/maven-metadata.xml" | grep -E "<release>|<lastUpdated>"
```
期望: `<release>1.25.3</release>`（若已出更新 patch，回到作者确认是否调整目标，不擅自改）。

- [ ] **Step 2: 修改版本号**

```bash
cd /Users/muzhou/FEP_v1.0_wt-pitest-1253
# 仅改 pitest-maven version，精确匹配避免误伤 junit5-plugin 1.2.3
sed -i '' 's|<version>1.25.0</version>|<version>1.25.3</version>|' pom.xml
```

- [ ] **Step 3: 核实改动精确（grep 双向确认）**

```bash
cd /Users/muzhou/FEP_v1.0_wt-pitest-1253
sed -n '477,485p' pom.xml
git diff --stat pom.xml
```
期望: 第 479 行 `<version>1.25.3</version>`；第 484 行仍 `<version>1.2.3</version>`；`git diff --stat` 显示 `pom.xml | 2 +-`（1 增 1 删）。

- [ ] **Step 4: mutation 回归验证（phase = verify，红线 phase binding）**

```bash
cd /Users/muzhou/FEP_v1.0_wt-pitest-1253
./mvnw verify -Ppitest -pl fep-common -am --batch-mode --no-transfer-progress 2>&1 | tee /tmp/pitest-1253-verify.log
```
期望（sanity check 关键词，证 pitest 真跑非假绿）:
- 日志含 `>> Generated` + `mutations` + `Line Coverage` / `Mutators`
- 末尾 `BUILD SUCCESS`
- `grep -E ">> Line Coverage|mutations|>> Generated" /tmp/pitest-1253-verify.log` 非空

> **关键词兜底（AI 评审 CONCERN #1）**: pitest 各版本控制台 banner 措辞偶有微调。若上述 grep 关键词在 1.25.3 输出中未命中但 BUILD SUCCESS，**不直接判失败** — 改查 `fep-common/target/pit-reports/*/index.html` 是否生成且 mutation 表非空（report 生成 + 表非空 = pitest 确已执行），以报告产物为终判依据。

> **沙盒兜底（红线 `feedback_mvn_sandbox_exit144_pattern`）**: 若本机 `mvn` 出现 exit 144（SIGSYS 沙盒拦截）≥2 次，跳本机实测，改为静态等价审查（patch 升级语义不变 + 配置未改）+ 依赖 GHA Nightly Deep Scan 的 Pitest job 远端兜底验证；该情况下 Daily Report 须显式登记"本机 mutation 未实测，待 Nightly 绿背书"。

- [ ] **Step 5: 提交**

```bash
cd /Users/muzhou/FEP_v1.0_wt-pitest-1253
git add pom.xml
git commit -m "$(cat <<'EOF'
chore(deps): bump pitest-maven 1.25.0 -> 1.25.3

Patch upgrade within 1.25.x line (Maven Central latest release,
lastUpdated 2026-05-29). pitest-junit5-plugin stays 1.2.3 (already latest).
fep-common mutation regression verified via `mvn verify -Ppitest`.

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

## Task 2: Closing — worktree 清理 `模式 A`

> 本 Task 在 PR merge 后于 session-end 执行。

- [ ] **Step 1: 确认分支已 push + PR 开启**

```bash
cd /Users/muzhou/FEP_v1.0_wt-pitest-1253
git log origin/main..chore/pitest-1.25.3-upgrade --oneline   # 期望 1 commit
git push -u origin chore/pitest-1.25.3-upgrade
gh pr create --fill --base main
```

- [ ] **Step 2: PR merge 后清理 worktree**

```bash
cd /Users/muzhou/FEP_v1.0
git worktree remove /Users/muzhou/FEP_v1.0_wt-pitest-1253
git worktree list   # 确认已移除
```

---

## 回归验收（红线 `feedback_plan_regression_scope_explicit` 两层）

- **Strong（理想）**: `./mvnw verify -Ppitest -pl fep-common -am` BUILD SUCCESS + mutation score ≥ 80 + line coverage ≥ 80（本机实测）。
- **Minimum（兜底）**: 本机 mvn 沙盒 exit 144 时，静态等价审查（version 字段单点改 + profile 配置零改动 + patch 同 minor 线）+ GHA Nightly Deep Scan Pitest job 绿背书（红线 `feedback_systemic_ci_blocker_defers_positive_backing`：若 Nightly 因 NVD-429 等系统性阻塞连续取消，tier-A 静态充分即 CLOSED，tier-B 远端背书 deferred 至阻塞解除）。

---

## 自检清单结果

1. **PRD 覆盖度**: N/A（infra Plan，已在头部声明不映射 FR-ID）✅
2. **安全边界**: 无 SM2/3/4 / 密钥 / 脱敏，无 ⛔ 模式 E ✅
3. **占位符扫描**: 无 TBD/TODO/待/后续/类似 ✅
4. **类型一致性**: N/A（无新增类）✅
5. **测试命令可执行**: 验证用 `mvn verify -Ppitest`，phase 与 `pom.xml:518` execution binding 一致 ✅
6. **CLAUDE.md 更新**: 由 session-end 统一更新"当前项目状态" + backlog #7 修正 ✅
7. **验收标准完整性**: 来自 pitest profile 实测阈值（80/80），可实算 ✅
8. **共享工具类**: N/A ✅
9. **核心类职责边界**: N/A ✅
10. **Worktree 触发**: 命中（多会话并发 + 与已签字 Plan 并存），头部已填具体路径与分支，Closing Task 含 `git worktree remove` 实测命令 ✅

---

## 批准签字

- **AI 独立评审**: PASS（agentId `ac96e017e17c55cb7`）— 全 baseline 实测对齐；2 项 🟡 CONCERN（0 LOC 观测/流程），CONCERN #1 关键词兜底已吸纳进 Step 4
- **Plan Approver**: **muzhou ✅ APPROVED**（2026-06-02）— 签字 + subagent 驱动实施
- **Baseline at signature**: `origin/main@c74a1ce`，worktree `wt-pitest-1253` 同 HEAD
