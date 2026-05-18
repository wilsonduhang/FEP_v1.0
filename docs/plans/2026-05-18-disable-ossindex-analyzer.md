# FEP 禁用 OWASP OSS Index Analyzer 实施计划

> **执行方式:** superpowers:executing-plans 内联执行（推荐 — 单 Plan / 单文件 pom config / 0 业务代码 / 1 Task）。步骤 `- [ ]` 复选框跟踪。

**目标:** 修复 Nightly Deep Scan 因 OWASP OSS Index analyzer 匿名访问 Sonatype 返回 HTTP 401（`AnalysisException: Invalid credentials provided for OSS Index`）导致的终止失败 — 在 dependency-check-maven 配置中显式禁用冗余的 OSS Index analyzer，使 Nightly OWASP 门禁仅依赖 NVD（已覆盖主 CVE 检测）。

**前置依赖:** SB 3.5.14 升级已闭环（origin/main 含 ab41103，OWASP CVSS≥7 阻断 31→0 已实证）。本 Plan 处理 SB 升级实证暴露的衍生独立问题。

**执行 Worktree:** `main`（无需独立 worktree，触发条件均不命中）
> 红线 `feedback_worktree_for_parallel_work` 触发条件实测: ① 跨≥3模块=否（仅 root pom.xml 1 文件）② 已签字未执行 Plan 并存=否（grep 实测唯一 SIGNED-OPEN 为已 ship 的 SB Plan checkbox 未回填，同域已完成非并发）③ 安全 vs AI=否 ④ TLQ tongtech=否 ⑤ ≥5min long-running verify 并行=否（验证为远端 Nightly CI ~30min，非本地阻塞）⑥ muzhou WIP 并存=签字时实测。符合 CLAUDE.md "小改动+单模块+无并存 Plan+串行可控" 例外，普通 feature 分支 `chore/disable-ossindex-analyzer`。

**架构:** dependency-check-maven 10.0.4 默认启用多个 analyzer。NVD analyzer 是主 CVE 数据源（已实证 SB 3.5.14 31→0 检测完全由 NVD 完成）。OSS Index (Sonatype) 是补充 analyzer，自匿名访问被弃用后需认证凭证；FEP 无 Sonatype 账号 → 401 硬失败。禁用 OSS Index analyzer 移除冗余 + 401 终止点，零 secret 管理。

**技术栈:** Maven / org.owasp:dependency-check-maven:10.0.4 / GHA nightly.yml

**AI 协同模式:**

| 模式 | AI 占比 | 本计划中的应用 |
|:----:|:-------:|--------------|
| A | 90% | pom.xml plugin config 1 行 + 验证 |
| B-D | — | 不涉及 |
| E | 0% | ⛔ 无国密代码改动 |

---

## 设计背景

### Why（动因）

SB 3.5.14 升级实证（暖缓存 scheduled Nightly run `25969517708`，sha=ab41103，2026-05-16）确认：

1. ✅ **OWASP CVSS≥7 阻断 = 0**（`grep -c "CVSS score greater than or equal to|.jar: CVE-" = 0`）— SB 升级核心目标达成
2. ❌ Nightly 仍 RED，根因换成 **pre-existing 无关问题**:

```
[ERROR] Failed to execute goal org.owasp:dependency-check-maven:10.0.4:check
  (owasp-dependency-check) on project fep-common:
  One or more exceptions occurred during dependency-check analysis:
  AnalysisException: Invalid credentials provided for OSS Index
  caused by TransportException: Unexpected response; status: 401
```

OSS Index 401 是 pre-existing 问题（旧 nightly 日志同有 "Invalid credentials for the
OSS Index, disabling the analyzer"，但当时 **CVSS≥7 gate 先失败**掩盖；CVE 清零后
OSS Index 401 异常成为新终止点）。与 SB 升级 / CVE 修复完全正交。

**为何 auto-disable 没救场（评审 MAJOR-1）**: dependency-check-maven 的 OSS Index
analyzer "无凭证则自动禁用" 仅在**凭证缺失**时触发；但匿名访问 Sonatype OSS Index
被**主动拒绝返回硬 401**（非"无凭证"而是"凭证无效"），该 401 在 auto-disable
fallback 生效前就抛出为 `AnalysisException` 终止 build（plugin 已知行为，ref
jeremylong/DependencyCheck#2278/#5728）。故必须**显式** `ossindexAnalyzerEnabled=false`
而非依赖 auto-disable —— 这不是"整洁化"而是必需修复。

### What changes（修改范围）

**1 个文件 1 行新增**：`pom.xml` dependency-check-maven `<configuration>` 内加
`<ossindexAnalyzerEnabled>false</ossindexAnalyzerEnabled>`。

### Why 禁用而非配置凭证

| 维度 | 禁用 OSS Index（本 Plan）| 配置 Sonatype 凭证 |
|------|------------------------|-------------------|
| CVE 覆盖 | NVD 已覆盖（实证 SB 31→0 全由 NVD 检出）| OSS Index 为补充冗余 |
| Secret 管理 | 零（无新 secret）| 需注册 Sonatype + 维护 token secret |
| 失败面 | 移除 401 硬失败 | 凭证过期/轮换又会 401 |
| 维护成本 | 一次性 | 持续 |

NVD 是 dependency-check 的主数据源且已证明对本项目 CVSS≥7 门禁充分。OSS Index
的差异化价值是**生态系统通告**（npm/RubyGems/Sonatype 策展，部分不在 NVD）——
但本项目 Maven reactor 无 JS/npm 生产依赖（fep-admin-ui 为独立 pnpm 树，不在本
OWASP 扫描范围）。故 **对本项目 Maven-only 依赖面，NVD-only 充分**，OSS Index
在此扫描范围内冗余（评审 MINOR-1：限定为 Maven-only 面，非普适"冗余"），
无凭证下只引入 401 噪声，禁用为净收益。

### 属性名验证（红线 `feedback_adr_grep_command_executability_blindspot`）

dependency-check-maven 10.0.4 配置属性 `<ossindexAnalyzerEnabled>`（boolean，
默认 true），与 `<assemblyAnalyzerEnabled>` / `<nodeAnalyzerEnabled>` 同族。
本机 Maven Central 被沙盒拦截（红线 `feedback_mvn_sandbox_exit144_pattern`）+ 无
.m2 plugin 缓存 → 属性名**无法本机 dry-run**，必须 GHA Nightly 实证：禁用后
OSS Index 段不再出现 "Invalid credentials" + 不抛 AnalysisException。
若 `ossindexAnalyzerEnabled` 属性名无效（CI 报 unknown parameter）→ T1 Step
fallback 改用官方文档确认的属性名（contingency clause）。

### 不在本 Plan 范围

- ❌ 配置 Sonatype OSS Index 凭证（明确选择禁用路径）
- ❌ 升级 dependency-check-maven 版本（10.0.4 功能足够）
- ❌ 修改 NVD analyzer / failBuildOnCVSS / suppressionFiles（SB 升级已配，不动）
- ❌ Nightly CI 冷缓存 hang 问题（独立，本 Plan 不涉；暖缓存 run 已证不 hang）
- ❌ SB Plan checkbox 回填（独立 housekeeping）

### PRD 追溯

**基础设施 Plan 例外**（CLAUDE.md "Plan 治理" 基础设施/元流程 Plan 除外）。
依据 CLAUDE.md "Nightly 深扫" §OWASP Dependency-Check 门禁可用性。

---

## 文件结构

| 文件路径 | 职责 | 操作 | AI 模式 |
|----------|------|------|:-------:|
| `pom.xml` | dependency-check-maven config 加 ossindexAnalyzerEnabled=false | 修改 | A |
| `docs/plans/2026-05-18-disable-ossindex-analyzer.md` | 本 Plan | 新建 | A |

### 共享工具类清单
**N/A** — 无 Java 代码。

### 核心类职责边界声明
**N/A** — 无 Service。

---

## Tasks

### Task 1: 禁用 OSS Index analyzer + Nightly 实证 `模式 A`

**PRD 依据:** 基础设施 Plan 例外（CLAUDE.md "Nightly 深扫" §OWASP）
**追溯 ID:** N/A

**验收标准（从 CI 门禁可用性推导）:**

1. **配置生效** — `pom.xml` dependency-check-maven `<configuration>` 含
   `<ossindexAnalyzerEnabled>false</ossindexAnalyzerEnabled>`
2. **PR CI 不回归** — PR 触发 ci.yml reactor verify GREEN（owasp.skip=true 默认，
   PR 不跑 OWASP，本 step 仅确认 pom 改动不破坏构建）
3. **Nightly OSS Index 401 消失** — 手动触发 Nightly on 分支，OWASP 段日志
   **不再出现** `Invalid credentials for the OSS Index` + **不再抛**
   `AnalysisException: Invalid credentials provided for OSS Index`
4. **Nightly OWASP CVSS≥7 仍 = 0** — 禁用 OSS Index 后 NVD 仍正常检测，
   `grep -c "CVSS score greater than or equal to|.jar: CVE-" = 0`（不因移除
   analyzer 漏检 — 实证 SB 31→0 本就全由 NVD 完成）
5. **Nightly mvn step 通过 OWASP** — `dependency-check-maven:check on fep-common`
   不再因 OSS Index 异常 BUILD FAILURE（后续 Pitest/timeout 为独立问题不在本 Plan）

> **规则**: 验收标准 3/4/5 必须由 GHA Nightly 实测（本机沙盒无法验证）。
> 标准 4 是关键安全回归闸：禁用 analyzer 不得降低 CVE 检出。

**Files:**
- Modify: `pom.xml`（dependency-check-maven configuration 段）

**⛔ 安全提示:** 无国密代码。但标准 4 是安全回归闸 — 禁用补充 analyzer 必须实证
NVD 检出能力不降（OWASP 门禁有效性不得削弱）。

- [ ] **Step 1: 起态确认 — OSS Index 当前默认启用**

```bash
cd /Users/muzhou/FEP_v1.0
git checkout -b chore/disable-ossindex-analyzer origin/main
grep -n "ossindex\|ossIndex" pom.xml || echo "确认: 无 ossindex 配置 = analyzer 默认启用"
grep -n -A 3 "dependency-check-maven" pom.xml | grep -E "failBuildOnCVSS|suppressionFiles|skipTestScope"
# 期望: 无 ossindex 行；failBuildOnCVSS/suppressionFiles/skipTestScope 存在（SB 升级遗留配置，不动）
```

- [ ] **Step 2: 加入 ossindexAnalyzerEnabled=false**

在 `pom.xml` dependency-check-maven `<configuration>` 内，`<skipTestScope>` 行之后加：

```xml
                    <!-- 禁用 OSS Index (Sonatype) 补充 analyzer：匿名访问被弃用后
                         需认证凭证，FEP 无 Sonatype 账号导致 401 硬失败
                         (AnalysisException: Invalid credentials provided for OSS
                         Index)。NVD analyzer 为主数据源且已实证对本项目 CVSS≥7
                         门禁充分（SB 3.5.14 31→0 全由 NVD 检出）。
                         Plan: docs/plans/2026-05-18-disable-ossindex-analyzer.md -->
                    <ossindexAnalyzerEnabled>false</ossindexAnalyzerEnabled>
```

> **属性名 contingency**: 若 Step 4 CI 报 `unknown parameter ossindexAnalyzerEnabled`
> → 查 dependency-check-maven 10.0.4 官方文档确认确切属性名/嵌套结构后修正重推。

- [ ] **Step 3: 本地构建不破坏验证（owasp 默认 skip）**

```bash
git add pom.xml && git diff --cached pom.xml | grep -E "^\+.*ossindex"
# 期望: +<ossindexAnalyzerEnabled>false</ossindexAnalyzerEnabled> (+注释)
# 本机不跑 mvn（Maven Central 沙盒拦截，红线 feedback_mvn_sandbox_exit144_pattern）
# pom XML 良构性由 git diff 目视 + Step 4 CI 兜底
```

- [ ] **Step 4: Commit + push + PR CI**

```bash
git commit -m "$(cat <<'EOF'
chore(ci): disable OWASP OSS Index analyzer (anonymous 401 hard-fail)

SB 3.5.14 OWASP CVE remediation empirically confirmed (warm-cache scheduled
Nightly run 25969517708 sha=ab41103: CVSS>=7 blockers = 0). But Nightly stays
RED on a pre-existing, unrelated issue: dependency-check-maven OSS Index
analyzer anonymous access returns HTTP 401 (AnalysisException: Invalid
credentials provided for OSS Index), now the terminal failure since the CVE
gate no longer fails first.

Disable the redundant OSS Index analyzer. NVD is the primary data source and
proven sufficient for this project's CVSS>=7 gate (SB 3.5.14 31->0 detected
entirely via NVD). Zero secret management vs configuring Sonatype creds.

Verification delegated to GHA Nightly (local Maven Central sandbox-blocked).

Plan: docs/plans/2026-05-18-disable-ossindex-analyzer.md

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
git push origin chore/disable-ossindex-analyzer
gh pr create --base main --head chore/disable-ossindex-analyzer \
  --title "chore(ci): disable OWASP OSS Index analyzer (401 hard-fail)" \
  --body "Fixes Nightly RED root cause #2 (OSS Index 401) after SB 3.5.14 CVE remediation. NVD-only OWASP gate. Plan: docs/plans/2026-05-18-disable-ossindex-analyzer.md" --draft
gh pr checks  # ci.yml reactor verify GREEN（PR 不跑 OWASP，确认 pom 改动不破坏构建）
```

- [ ] **Step 5: Nightly 实证（关键验收 3/4/5）**

```bash
gh workflow run "Nightly Deep Scan" --ref chore/disable-ossindex-analyzer
# poll loop 监控（红线 feedback_pipe_tail_deadlock — redirect to file 不 pipe）
for i in $(seq 1 30); do ST=$(gh run list --branch chore/disable-ossindex-analyzer \
  --workflow "Nightly Deep Scan" --limit 1 --json status,conclusion \
  --jq '.[0].status+"|"+.[0].conclusion' 2>/dev/null); echo "[$i] $ST"; \
  case "$ST" in completed*) break;; esac; sleep 90; done > /tmp/ossidx-night.log 2>&1
```

下载日志验证：
```bash
RID=$(gh run list --branch chore/disable-ossindex-analyzer --workflow "Nightly Deep Scan" --limit 1 --json databaseId --jq '.[0].databaseId')
JID=$(gh run view $RID --json jobs --jq '.jobs[0].databaseId')
gh run view $RID --job $JID --log > /tmp/ossidx-night-full.log 2>&1
echo "OSS Index 401 残留 (期望 0):"; grep -c "Invalid credentials.*OSS Index\|Invalid credentials provided for OSS Index" /tmp/ossidx-night-full.log
echo "OWASP CVSS>=7 阻断 (期望 0):"; grep -c "CVSS score greater than or equal to\|\.jar: CVE-[0-9]" /tmp/ossidx-night-full.log
echo "dependency-check OWASP 段是否过 (期望无 dependency-check BUILD FAILURE):"; grep -E "dependency-check-maven.*check.*FAILURE|AnalysisException" /tmp/ossidx-night-full.log | head -3
```

期望:
- OSS Index 401 残留 = **0**（验收 3）
- OWASP CVSS≥7 阻断 = **0**（验收 4 — NVD 仍检测，无漏检回归）
- 无 dependency-check AnalysisException BUILD FAILURE（验收 5）
- 注: Pitest/timeout 若仍 fail 为独立问题（本 Plan 不涉），只要 OWASP 段通过即达标

> **Nightly hang contingency（评审 NIT-1）**: 若 poll loop >40min 仍无 OWASP
> 日志行（冷缓存 hang，SB 升级实证已遇 ×2）→ 重触发 1 次（复用前 run 暖 NVD
> 缓存）；第 2 次仍 hang → 升级为独立 CI infra issue 上报 muzhou（**非本 Plan
> 失败** —— OSS Index 配置正确性与 Nightly 冷缓存 hang 是两个正交问题；本 Plan
> 验收仅需某次 run 跑到 OWASP 段并满足验收 3/4/5）。

- [ ] **Step 6: closing**

```bash
# CLAUDE.md 当前项目状态 + PHASE_HISTORY 更新（file write，/FEP 非 git）见 session-end
cd /Users/muzhou/FEP_v1.0
git fetch origin
git log origin/main..main --oneline | wc -l  # 别会话 WIP 检测（红线 feedback_main_worktree_unpushed_wip_detection）
git checkout main && git merge --ff-only origin/main
# 评审 MINOR-2: baseline drift fallback —— 若 origin/main 在 cycle 中前进，
# chore 分支非 fast-forwardable，先 rebase 再 ff-merge（红线
# feedback_baseline_drift_during_long_review_cycle）
git merge --ff-only chore/disable-ossindex-analyzer 2>/dev/null || {
  git checkout chore/disable-ossindex-analyzer
  git rebase origin/main   # 冲突仅可能在 pom.xml dependency-check 段，保留本 Plan 改动
  git checkout main && git merge --ff-only chore/disable-ossindex-analyzer
}
git push origin main
git branch -d chore/disable-ossindex-analyzer
git push origin --delete chore/disable-ossindex-analyzer
```

---

## 自检清单

| # | 检查项 | 状态 |
|---|--------|------|
| 1 | PRD 覆盖度 | N/A — 基础设施例外，依据 CLAUDE.md Nightly 深扫 §OWASP |
| 2 | 安全边界 | ✅ 无 security/impl；标准 4 设安全回归闸（NVD 检出不降）|
| 3 | 占位符扫描 | ✅ 无 TBD/TODO（属性名 contingency 显式标注非占位）|
| 4 | 类型一致性 | ✅ 无 Java 类，纯 XML |
| 5 | 测试命令可执行 | ✅ Step 4/5 gh + grep 命令具体；本机 mvn 沙盒 fallback 写明 |
| 6 | CLAUDE.md 更新 | ✅ Step 6 + session-end |
| 7 | 验收来自规则 | ✅ 标准来自 CI 门禁可用性 + 安全回归闸 |
| 8 | 共享工具类 | N/A |
| 9 | 核心类职责 | N/A |
| 10 | Worktree 触发 | ✅ 全不命中，main + feature 分支（头部已实测明示）|

### Worktree 触发条件实测

- [ ] ① 跨≥3模块? **否** — 仅 root pom.xml 1 文件
- [ ] ② 已签字未执行 Plan 并存? **否** — grep 实测唯一 SIGNED-OPEN 为已 ship SB Plan（checkbox 未回填，同域已完成，非并发未签字 Plan）
- [ ] ③ 安全 vs AI 并行? 否
- [ ] ④ TLQ tongtech? 否
- [ ] ⑤ ≥5min long-running verify 并行? 否（验证为远端 Nightly，非本地阻塞）
- [ ] ⑥ muzhou WIP 并存? 签字时实测

→ 全不命中，无需 worktree（CLAUDE.md 例外：小改动+单模块+无并存+串行可控）

---

## 红线合规自检

| 红线 | 应对 |
|------|------|
| `feedback_plan_must_grep_actual_api` | 起草前已 grep 实测 pom.xml dependency-check config + nightly.yml secrets + 无 ossindex 配置现状 |
| `feedback_mvn_sandbox_exit144_pattern` | Step 3 不跑本机 mvn，Step 4/5 GHA CI/Nightly 实证 |
| `feedback_adr_grep_command_executability_blindspot` | 属性名无法本机 dry-run 已显式标注 + Step 2 contingency clause |
| `feedback_main_worktree_unpushed_wip_detection` | Step 6 ff-merge 前 `git log origin/main..main` 别会话 WIP 检测 |
| `feedback_pipe_tail_deadlock_with_bg_bash` | Step 5 poll loop redirect to file，无 `\| tail` pipe |
| `feedback_worktree_for_parallel_work` | 头部 + 自检 10 逐条实测，全不命中明示 main |
| `feedback_security_doc_must_distinguish_spotbugs_layers` | 验收标准 4 设 NVD 检出不降回归闸（OWASP 门禁有效性不削弱）|

---

## 执行交接

**⚠️ 须经 AI 独立评审 + muzhou 签字后方可执行。**

### 步骤 1: AI 独立评审
Reviewer: general-purpose subagent。输入 Plan 全文 + `docs/guides/plan-review-checklist.md`。
重点验证: 属性名 contingency 是否充分 / 验收标准 4 安全回归闸是否严谨 / worktree 判定。

### 步骤 2: muzhou 签字
AI 评审通过 → AskUserQuestion 请 muzhou 决策（Approve / Revise / Reject）。

### 步骤 3: 签字后执行
内联（executing-plans）单 Task 推进。**禁止未签字直接执行。**

---

## v1.1 修订摘要（AI 评审 PASS WITH MINOR 后 inline）

general-purpose subagent 独立评审判决 **PASS WITH MINOR**（属性名 `ossindexAnalyzerEnabled`
经官方 check-mojo.html + GitHub#2278 双源确认正确，**无 BLOCKER**）。已修：

- **MAJOR-1**: §Why 动因 加"为何 auto-disable 没救场"段（硬 401 在 auto-disable fallback 前抛 AnalysisException，故必须显式 false）
- **MINOR-1**: §Why 禁用 限定"冗余"为 Maven-only 依赖面（OSS Index 差异价值是 npm/生态通告，FEP Maven reactor 无 JS 生产依赖）
- **MINOR-2**: Step 6 加 baseline drift fallback（ff 失败 → rebase origin/main 再 ff-merge）
- **NIT-1**: Step 5 加 Nightly hang contingency（>40min 无 OWASP 日志重触发 1 次，2 次升级独立 infra issue 非本 Plan 失败）
- **MINOR-3 / NIT-2**: 评审确认 worktree=main 判定正确 + 自检表诚实，无需改

评审建议"Address MAJOR-1 + MINOR-1/2/3 before muzhou signs; NITs optional" 已全满足
→ 进 muzhou 签字（PASS WITH MINOR 条件批准，同 SB Plan v2.1 模式，无需全量重评）。

## 签字块

```
Plan Approver: muzhou
Approval Date: 2026-05-18
Approval Status: [x] Approved  [ ] Revise  [ ] Reject
Approval Notes: v1.1 (AI 评审 PASS WITH MINOR，属性名 ossindexAnalyzerEnabled 经
官方 check-mojo.html + GitHub#2278 双源确认无 BLOCKER；MAJOR-1+MINOR-1/2+NIT-1 已
inline 修订) 经 AskUserQuestion Approved 进 Task 1 内联执行 (executing-plans 风格)。
```
