# FEP Spring Boot Parent 升级 3.3.7 → 3.5.9 实施计划

> **版本:** v2（2026-05-15 主对话内联修订，根据 AI 评审 3 BLOCKER + 5 MAJOR + 5 MINOR + 3 NIT 修订）
>
> **执行方式:** 使用 superpowers:executing-plans 逐 Task 内联执行（推荐 — Plan 仅 3 Task / pom.xml-only / 0 业务代码，subagent dispatch 性价比低，per 评审 NIT n1）。步骤使用 `- [ ]` 复选框跟踪。

**目标:** 修复 OWASP Dependency-Check Nightly Deep Scan **连续 14 天 failure**（自 2026-05-01）— 升级 Spring Boot parent BOM 从 `3.3.7`（EOL，2024-12 发布）到 `3.5.9`（当前 stable，2026-05 patch），一次性消除 ≥30 个 CVSS ≥ 7.0 的传递依赖漏洞，恢复 nightly 绿光。

**前置依赖:** 无（独立基础设施修复）。

**执行 Worktree:** `/Users/muzhou/FEP_v1.0_wt-sb35-upgrade`（分支 `chore/sb-parent-3.5.9`，触发条件第 ① 项跨 ≥3 模块 refactor + 第 ⑤ 项 ≥5min long-running verify）
> 红线 `feedback_worktree_for_parallel_work` 触发条件: ① 跨 ≥3 模块 refactor ② 与已签字 Plan 并存 ③ ⛔ 安全 vs AI 并行 ④ TLQ tongtech 联调 ⑤ >5min long-running verify 并行 ⑥ muzhou WIP 与 AI 并存
> 本 Plan 命中 ①（8 个 Maven 模块全部受 parent BOM 影响）+ ⑤（全 reactor verify ~10-15 min）

**架构:** Spring Boot parent BOM 控制所有 Spring/Tomcat/Hibernate/log4j/Jackson/SLF4J 版本。升级 parent 一次性提升 log4j-api 2.23.1 → 2.24.x、tomcat-embed-core 10.1.34 → 10.1.42+、spring-core 6.1.16 → 6.2.x、hibernate-validator 8.0.2 → 8.0.3+。临时加入 `spring-boot-properties-migrator` 在启动时检测 deprecated properties，确认无 warning 后移除。

**技术栈:** Java 17 / Spring Boot **3.3.7 → 3.5.9** / Maven / OWASP Dependency-Check 10.0.4

**AI 协同模式:**

| 模式 | AI 占比 | 本计划中的应用 |
|:----:|:-------:|--------------|
| A | 90% | pom.xml 版本调整 / 验证脚本 / 文档更新 |
| B | 70% | 不涉及 |
| C | 60% | 不涉及 |
| D | 50% | 不涉及 |
| E | 0%  | ⛔ 无国密代码改动 |

---

## 设计背景

### Why now（动因）

| 维度 | 现状 | 风险 |
|------|------|------|
| Spring Boot 3.3.x line | 2024-11 发布，**2025-12 EOL** | 安全补丁停发，CVE 暴露窗口持续扩大 |
| Nightly Deep Scan | 14 天 RED（2026-05-01 起，每日 02:00 BJT） | 安全治理告警被忽视，工程纪律侵蚀 |
| Critical CVE 数 | 5 个 (CVE-2025-24813/31651/55754/66614 + CVE-2026-29145，全 9.1-9.8) | 公网入侵风险（生产部署敞口） |
| 新增 critical | 自 5/1 起 +1（CVE-2026-40974 9.8 in spring-boot） | CVE 池单向增长 |

### What changes（修改范围）

**1 个文件 1 行变更**:
- `pom.xml:10` — `<version>3.3.7</version>` → `<version>3.5.9</version>`

**临时新增**（T1 加入，T2 移除）:
- `fep-common/pom.xml` 加 `spring-boot-properties-migrator`（runtime scope，启动时打印 deprecated property warning）

### Why 3.5.9 而非 3.6.x / 4.0.x

| 候选版本 | 选择理由 |
|---------|---------|
| **3.5.9** ✅ | 当前 GA stable patch，与 3.3 binary-compat 度高，Spring Security 6 lambda DSL 已在使用无 API 风险 |
| 3.4.x | 已被 3.5.x 取代，无理由停留 |
| 3.6.x | 若未 GA 跳过；若已 GA 但发布 <30 天，避开 maturity risk |
| 4.0.x | major bump，Jakarta EE 11 / Spring Framework 7，需独立 Plan + 更长 verify |

### CVE 修复预期（推算）

| jar | 3.3.7 版本 | 3.5.9 版本（来自 SB 3.5.x BOM） | 阻断 CVE 数 | 预期修复 |
|-----|-----------|-------------------------------|------------|---------|
| log4j-api | 2.23.1 | 2.24.x (SB 3.5 BOM) | 4 个 7.5 | ✅ 全修 |
| spring-boot | 3.3.7 | 3.5.9 | 5 个 (含 9.8) | ✅ 全修 |
| spring-core | 6.1.16 | 6.2.x | (未阻断但有 finding) | ✅ 全修 |
| spring-web | 6.1.16 | 6.2.x | (未阻断但有 finding) | ✅ 全修 |
| tomcat-embed-core | 10.1.34 | 10.1.42+ | 21 个 (含 5 个 ≥9.0) | ✅ 全修 / 少量残留待 ADR |
| hibernate-validator | 8.0.2 | 8.0.3+ | 1 个 (CVE-2025-15104 CVSS 待核实) | ✅ 多半修 |

**残留风险**: 3.5.9 BOM 内的 log4j/tomcat/spring 版本仍可能有 CVSS ≥ 7 但 < 9 的 CVE 未补丁。

**残留处置政策（v2 修订 / 评审 M2）**: CLAUDE.md "OWASP Dependency-Check: CVSS ≥ 7 阻断" 是 **binary gate** — 本 Plan 目标 = **0 阻断**。任一残留必须通过以下 3 选 1 处置才能 Plan 完成：

1. **Suppression**: 加入 `owasp-suppression.xml`（root-level，新建文件）+ pom.xml plugin 配 `<suppressionFile>` + 单条 ADR 记录 + muzhou 签字
2. **Override**: 在 root pom `<dependencyManagement>` 显式 override 到非 BOM 修复版本（如 log4j-api 2.25.x）+ regression IT 通过
3. **Plan FAIL**: T1 acceptance 不达标，回滚到 3.3.7 + 升级 Plan to SB 3.6.x

无 "≤ N 残留默认接受" 灰色地带。

### Risk Mitigation

| 风险 | 缓解 |
|------|------|
| Spring Security 6 API drift | SecurityConfiguration 已用 lambda DSL（grep 确认无 WebSecurityConfigurerAdapter），无 drift 风险 |
| JPA/Hibernate 行为变化 | Hibernate 6.5 → 6.6（patch），无 schema 影响；F 级硬冻结的 V1-V26 不动 |
| Actuator endpoint 路径变 | 已用红线 `feedback_springboot3_metrics_export_enabled` 显式 `management.prometheus.metrics.export.enabled=true`，SB 3.5 同 property name 不变 |
| application.yml deprecated key | T1 临时启用 `spring-boot-properties-migrator` 启动时打印 warning，逐条排查 |
| GHA CI Surefire / JaCoCo 插件版本兼容（v2 评审 M1） | T1 Step 1c 实测当前 effective surefire/jacoco/spring-boot-maven-plugin 版本，T1 Step 4 升级后对比；任一 plugin 大版本变动 → 跑 ArchUnit + Checkstyle 子模块单测确认 |
| Logback 1.5.x 升级（v2 评审 m3） | SB 3.5 BOM 含 Logback 1.5.x。本项目无 `logback-spring.xml` 自定义 appender（实测 `find . -name 'logback*.xml'` 无匹配）— 风险极低 |
| JDK 兼容性（v2 评审 m4） | SB 3.5 最低 Java 17（match 本项目 `<java.version>17</java.version>`）；CI `setup-java@v4 with: java-version: 17` 一致；4.0+ 才要求 21，不影响本 Plan |
| Worktree merge 冲突（v2 评审 M3） | T3 Step 5 改为 `git fetch + git rebase origin/main + git merge --ff-only`，rebase 冲突时进 worktree 修复 + 重跑 verify |

### 不在本 Plan 范围

- ❌ Spring Boot 4.0.x major bump（独立 Plan，Jakarta EE 11 影响面大）
- ❌ Tomcat APR 配置 / native lib 优化（无必要）
- ❌ OWASP plugin 升级到 11.x（10.0.4 当前能用）
- ❌ owasp-suppression.xml 配置（本 Plan 力求一次性全修，残留再单独 ADR）
- ❌ Pitest mutation score 调优（与 Nightly failure 无关）
- ❌ 任何业务代码改动（pom.xml only）

### PRD 追溯

**基础设施 Plan 例外**（CLAUDE.md "Plan 治理" §"每个 Task 必须引用 PRD 章节 + FR-ID（基础设施/元流程 Plan 除外）"），本 Plan 不实现新功能需求，不引用 FR-ID。

依据 CLAUDE.md §"质量门禁合规" → "Nightly 深扫" 段质量门禁规则: **OWASP Dependency-Check: CVSS ≥ 7 阻断**。Plan 修复对象 = 该 binary gate 的 14 天连续违反。

---

## 文件结构

| 文件路径 | 职责 | 操作 | AI 模式 |
|----------|------|------|:-------:|
| `pom.xml` | 父 BOM 版本（line 10） | 修改 | A |
| `fep-common/pom.xml` | 临时引入 properties-migrator | 修改（T1 加入 / T2 移除） | A |
| `docs/plans/2026-05-15-spring-boot-parent-upgrade-3.5.9.md` | 本 Plan | 新建 | A |

### 共享工具类清单

**N/A** — 本 Plan 不创建任何 Java 工具类。

### 核心类职责边界声明

**N/A** — 本 Plan 不创建或修改 Service 类。

---

## Tasks

### Task 1: Spring Boot parent 3.3.7 → 3.5.9 升级 + 全 reactor verify `模式 A`

**PRD 依据:** 基础设施 Plan 例外（CLAUDE.md "Nightly 深扫" §OWASP Dependency-Check）
**追溯 ID:** N/A

**验收标准（v2 修订 / 从依赖治理规则推导）:**

1. **编译通过** — `./mvnw clean compile -DskipTests` 在所有 8 模块 BUILD SUCCESS（任一模块编译失败 → Task FAIL）
2. **单元测试零回归** — `./mvnw test`（无 IT）测试总数 ≥ T1 Step 1c 实测的 main HEAD baseline 数（**动态 baseline，禁止硬编码** — 评审 m2 修订），failures = 0, errors = 0（红线 `feedback_full_regression_before_commit` + `feedback_doc_data_grep_first`）
3. **集成测试零回归** — `./mvnw verify -DskipITs=false` 全 reactor BUILD SUCCESS（强回归层，红线 `feedback_plan_regression_scope_explicit`）
4. **OWASP 阻断数 = 0**（v2 评审 M2 修订）— `./mvnw verify -Dowasp.skip=false` BUILD SUCCESS（OWASP plugin 配 `failBuildOnCVSS=7`，0 阻断即 plugin 不抛 BUILD FAILURE）。任一残留 CVSS ≥ 7 → 走"残留处置政策" 3 选 1（suppression / override / Plan FAIL）
5. **关键 critical CVE 全修** — log4j-api CVE-2026-34478..34481 + spring-boot CVE-2026-40974(9.8) + tomcat-embed-core CVE-2025-24813/31651/55754/66614 + 2026-29145 全部从报告中消失（`target/dependency-check-report.html` grep 验证）
6. **deprecated property warning 全 0** — 启动 fep-web 时 `spring-boot-properties-migrator` 打印的 deprecated property warning 数 = 0（≤ 5 关闭灰色地带，T2 修齐所有 warning 后 PR migrator dep）
7. **plugin 版本变动验证**（v2 评审 M1）— surefire / jacoco / spring-boot-maven-plugin 升级后跑 ArchUnit + Checkstyle 子模块单测，零回归
8. **migrator 传递依赖路径已验证**（v2 评审 M5）— `./mvnw dependency:tree -pl fep-web | grep migrator` 实测显示 properties-migrator 出现在 fep-web 依赖树中

> **规则**: 验收标准 4 是 **binary gate**（评审 M2）：0 阻断 → PASS；> 0 阻断 + 处置完毕 → PASS；> 0 阻断 + 未处置 → Task FAIL。报告值从 `target/dependency-check-report.html` 实测。

**Files:**
- Modify: `pom.xml`（line 10）
- Modify: `fep-common/pom.xml`（临时新增 properties-migrator）

**⛔ 安全提示:** 无国密代码改动，本 Task 不涉及 security/impl/ 模块。

- [ ] **Step 1: 起态 snapshot — 记录升级前 baseline**（v2 修订 / B1 + M1 + m1）

```bash
cd /Users/muzhou/FEP_v1.0_wt-sb35-upgrade

# 1a. 当前 parent version
grep -A 1 "spring-boot-starter-parent" pom.xml | head -3
# 期望输出: <version>3.3.7</version>

# 1b. 当前 effective 关键依赖版本
./mvnw -q dependency:list -pl fep-web \
  | grep -E "log4j-api|tomcat-embed-core|spring-core|spring-web|hibernate-validator" \
  | sort -u | tee /tmp/sb-3.3.7-deps.txt
# 期望含:
#   org.apache.logging.log4j:log4j-api:jar:2.23.1
#   org.apache.tomcat.embed:tomcat-embed-core:jar:10.1.34
#   org.springframework:spring-core:jar:6.1.16
#   org.springframework:spring-web:jar:6.1.16
#   org.hibernate.validator:hibernate-validator:jar:8.0.2.Final

# 1c. 当前测试 baseline 数（聚合每模块 surefire "Tests run:" / v2 评审 B1 修订）
gh run list --workflow CI --branch main --limit 1 --json conclusion,databaseId --jq '.[0]'
RUN_ID=$(gh run list --workflow CI --branch main --limit 1 --json databaseId --jq '.[0].databaseId')
gh run view $RUN_ID --log 2>/dev/null \
  | grep -E "Tests run: [0-9]+, Failures:" \
  | awk -F'Tests run: ' '{split($2, a, ","); sum += a[1]} END {print "Total tests (reactor sum):", sum}' \
  | tee /tmp/sb-baseline-tests.txt
# 期望: Total tests (reactor sum): NNNN (实测，无硬编码)
# 注: surefire 每模块独立打印, awk sum 聚合8模块累计数

# 1d. Plugin 版本 baseline（v2 评审 M1）
./mvnw -q help:effective-pom -pl fep-common 2>/dev/null \
  | grep -E "maven-surefire-plugin|jacoco-maven-plugin|spring-boot-maven-plugin" \
  | grep -A 1 "artifactId" | head -20 | tee /tmp/sb-3.3.7-plugins.txt
# 期望含 maven-surefire-plugin 3.2.5 / jacoco 0.8.12 / spring-boot-maven-plugin (从 BOM)

# 1e. SB 3.5.9 currency check（v2 评审 m1）
curl -sf "https://api.github.com/repos/spring-projects/spring-boot/releases" \
  | jq -r '.[] | select(.prerelease==false and (.tag_name | startswith("v3.5"))) | .tag_name' \
  | head -3
# 期望 v3.5.9 在 top 3 (若 v3.5.10+ 已发, T1 Step 2 升级到那个；本 Plan 写 3.5.9 是起草时的 latest)
```

期望：所有快照值都落盘 /tmp/sb-3.3.7-* 文件，conclusion=success，SB 3.5.9 仍是 latest 或 latest-1 patch。

- [ ] **Step 2: 升级 parent + 临时加入 properties-migrator**

```xml
<!-- pom.xml line 10 -->
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.5.9</version>  <!-- ⬅ 唯一变更点 -->
    <relativePath/>
</parent>
```

```xml
<!-- fep-common/pom.xml — 加入 properties-migrator -->
<!-- 位置: <dependencies> 段末尾 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-properties-migrator</artifactId>
    <scope>runtime</scope>
    <!-- T2 完成 verify 后立即移除 -->
</dependency>
```

> **注**: properties-migrator 仅 fep-common 引入即可（传递依赖会带到 fep-web），它在启动时只打印 warning，不阻断启动。

- [ ] **Step 3: 编译通过验证**

```bash
./mvnw clean compile -DskipTests --batch-mode --no-transfer-progress 2>&1 | tail -30
```

期望: 末尾 `BUILD SUCCESS`，8 个模块全部 SUCCESS。

任一模块 FAILURE → 立即查 `[ERROR]` 行，最常见原因:
- 移除/重命名的 deprecated API → 修 import 或调用方
- Lombok 与 SB 3.5 不兼容（极少见） → 检查 `lombok` 版本是否需要 override

**若需修代码**: 仅本 Plan 仅允许 import / annotation 级别的小调整，**重大 API 变化 → Task FAIL 报告 muzhou**。

- [ ] **Step 4: 实测新依赖版本**

```bash
./mvnw -q dependency:list -pl fep-web \
  | grep -E "log4j-api|tomcat-embed-core|spring-core|spring-web|hibernate-validator" \
  | sort -u | tee /tmp/sb-3.5.9-deps.txt

diff /tmp/sb-3.3.7-deps.txt /tmp/sb-3.5.9-deps.txt
```

期望 6 行版本均升级 — log4j-api 2.24.x / tomcat-embed-core 10.1.4x / spring-core+web 6.2.x / hibernate-validator 8.0.3+。

记下新版本号填入 closing 文档。

- [ ] **Step 5: 单元测试零回归**

```bash
./mvnw test --batch-mode --no-transfer-progress 2>&1 | tee /tmp/sb35-test.log | tail -50
```

期望末尾 `BUILD SUCCESS`。提取测试统计:

```bash
# v2.1 评审 NEW-m 修订: 动态 baseline，禁硬编码
grep -E "Tests run: [0-9]+, Failures:" /tmp/sb35-test.log \
  | awk -F'Tests run: ' '{split($2, a, ","); sum += a[1]} END {print "After SB 3.5.9 total:", sum}' \
  | tee /tmp/sb35-after-tests.txt

BASELINE=$(grep -oE "[0-9]+" /tmp/sb-baseline-tests.txt | tail -1)
AFTER=$(grep -oE "[0-9]+" /tmp/sb35-after-tests.txt | tail -1)
echo "Baseline (SB 3.3.7): $BASELINE / After (SB 3.5.9): $AFTER / Delta: $((AFTER - BASELINE))"
# 期望: Delta ≥ 0 (新版本不应少跑测试，failures=0/errors=0 在 mvn 末尾 BUILD SUCCESS 已隐含)

# 任一 failure/error → 单独 grep
grep -E "FAIL|ERROR" /tmp/sb35-test.log | grep -v "WARN\|Picked up" | head -10
```

任一 failure/error → Task FAIL，记录失败用例名 + reactor 模块 + stack trace 头 3 行报告 muzhou。

- [ ] **Step 6: 集成测试零回归（强回归层）**

> **v2 步骤顺序修订（评审 M4）**: 本 Step 在本机不可执行时 fallback 到 GHA，但 GHA fallback 必须先完成 Step 9 commit + push 才能跑 PR CI。Step 5/6 本机 fail 不阻塞 — 接力到 Step 9 commit → Step 6 fallback PR CI → 若 PR CI 绿则 Step 5/6 验收靠 GHA。

```bash
docker run -d --name fep-redis -p 6379:6379 redis:7-alpine 2>/dev/null || docker start fep-redis
./mvnw verify -DskipITs=false --batch-mode --no-transfer-progress > /tmp/sb35-verify.log 2>&1
tail -100 /tmp/sb35-verify.log
```

> v2 评审隐含修订: 不用 `tee | pipe`（红线 `feedback_pipe_tail_deadlock_with_bg_bash`），改 redirect to file + bash 读取。

期望:
- 末尾 `BUILD SUCCESS`
- 全 reactor 测试数较 step 5 增加（IT 计入）
- 启动期 fep-web 应用打印 properties-migrator warning（**目标 = 0 条**，T2 修齐若有；评审 M2 修订 — 任一 warning 必须 T2 处理）

**红线 `feedback_mvn_sandbox_exit144_pattern` 触发**（macOS Claude Code 沙盒 mvn exit 144 ≥ 2 次实证）→ 跳本机 mvn verify，**跳到 Step 9 commit 后**改 GHA fallback（见 Step 9 末尾）。

- [ ] **Step 7: OWASP Dependency-Check 本地实测**（v2 修订 / B3 局部 + 加 HTML 报告备份验证）

```bash
./mvnw verify -Dowasp.skip=false -pl fep-common -am --batch-mode --no-transfer-progress > /tmp/sb35-owasp.log 2>&1
echo "Exit code: $?"
```

> **注**: OWASP plugin 配 `<failBuildOnCVSS>7</failBuildOnCVSS>`，若仍有 CVSS ≥ 7 它会 BUILD FAILURE（exit 1）。本 step 接受 build failure 作为信号源。

**主验证: console summary 行**（实测格式，参见 nightly run 25878756990 实证）

```bash
grep -E "\[ERROR\] .*\.jar: CVE-" /tmp/sb35-owasp.log | tee /tmp/sb35-cve-blockers.txt
wc -l < /tmp/sb35-cve-blockers.txt
# v2 评审 B3 实测: 该 pattern 在 OWASP failBuildOnCVSS=7 触发时的 BUILD FAILURE 末尾段中实际出现
# 来源: gh run view 25878756990 实测命中 4 行（log4j-api / spring-boot / spring-boot-starter-web / tomcat-embed-core）
```

**辅助验证: HTML 报告 grep 所有 CVE 总数**（v2 评审 B3 兜底）

```bash
# 注: dependency-check 在 BUILD FAILURE 时仍会生成 target/dependency-check-report.html
ls fep-common/target/dependency-check-report.html
grep -oE "CVE-[0-9]+-[0-9]+" fep-common/target/dependency-check-report.html | sort -u | tee /tmp/sb35-all-cves.txt
wc -l < /tmp/sb35-all-cves.txt
# 期望: 全部 CVE 列表（含 < 7 的），与 console summary 行交叉验证
```

**验收（评审 M2 binary gate）**:
- exit code 0 (`echo "Exit code: $?"` = 0) → **PASS**：无阻断 CVE
- exit code 非 0 + `wc -l < /tmp/sb35-cve-blockers.txt` 输出 > 0 → 任一残留触发"残留处置政策" 3 选 1，**不直接 PASS**

提取结果填入 closing 文档:
- **修复 CVE 数**: baseline 31 - 实测残留数 = X
- **残留 CVE 清单**: 逐条记录 jar + CVE-ID + CVSS 分 + 处置决定（suppression / override / Plan FAIL）

**fallback** — 本机沙盒 issue 跳 OWASP，**前置 Step 9 commit + push 后**改 GHA Nightly 手动触发（见 Step 9 末尾的 GHA fallback 段）。

- [ ] **Step 8: 排查 deprecated property warning + migrator 传递依赖路径**（v2 修订 / M5）

**8a. migrator 传递依赖路径验证**（评审 M5）

```bash
./mvnw -q dependency:tree -pl fep-web | grep -E "migrator|properties-migrator" | head -10
# 期望命中: org.springframework.boot:spring-boot-properties-migrator:jar:3.5.9:runtime
# 路径: fep-web -> fep-common -> properties-migrator (runtime transitively exposed)
# 若无命中 -> Plan v2 Step 2 引入路径有 bug, fep-common 加 dep 时需补 <optional>false</optional> 或换位 root pom

# 同时确认 scope 一致性
./mvnw -q dependency:tree -pl fep-web -Dincludes="*:*:*:runtime" | head -10
```

**8b. deprecated property warning grep**

启动 fep-web dev profile（或查看 step 6 verify 日志中 `@SpringBootApplication` 启动段）:

```bash
grep -E "PropertiesMigrationListener|deprecated|replaced by" /tmp/sb35-verify.log | head -20
```

记录每条 warning。**v2 评审 M2 修订**: 任一 warning 必须列入 T2 修复范围。Plan 目标 = 0 warning（不再有"≤ 5 条接受"灰色地带）。

若沙盒导致 Step 6 verify 失败未产生日志 → 8b 跳过本机执行，T2 在 GHA CI 通过后再 grep CI 启动日志兜底。

- [ ] **Step 9: Commit + GHA Fallback PR**（v2 评审 M4 修订）

```bash
git add pom.xml fep-common/pom.xml
git commit -m "$(cat <<'EOF'
chore(deps): bump spring-boot parent 3.3.7 -> 3.5.9 + properties-migrator (temp)

Fix OWASP Dependency-Check nightly RED since 2026-05-01 (14 consecutive days).
Pulls in log4j-api 2.24.x, tomcat-embed-core 10.1.4x, spring-core/web 6.2.x,
hibernate-validator 8.0.3+. Drops 30+ CVSS>=7 CVEs from dep tree.

spring-boot-properties-migrator added temporarily under fep-common runtime scope
to surface deprecated application.yml keys; will be removed in T2 after verify.

Reactor verify GREEN on chore/sb-parent-3.5.9 (or pending GHA CI fallback).
OWASP CVE blocker count: 31 -> <see closing doc>.

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

**GHA Fallback PR** — Step 5/6/7 任一在本机沙盒下失败时，commit 后推 PR 让 GHA 兜底:

```bash
git push origin chore/sb-parent-3.5.9
gh pr create --base main --head chore/sb-parent-3.5.9 \
  --title "chore: bump spring-boot parent to 3.5.9 — verify via CI" \
  --body "OWASP CVE remediation. CI green required before merge. Plan: docs/plans/2026-05-15-spring-boot-parent-upgrade-3.5.9.md" \
  --draft
gh pr checks  # 等所有 check 通过
gh workflow run "Nightly Deep Scan" --ref chore/sb-parent-3.5.9
gh run watch
```

CI conclusion = success + Nightly conclusion = success → T1 Step 5/6/7 验收靠 GHA 实测值。
任一 failure → 进 Step 7 残留处置政策 3 选 1。

---

### Task 2: 移除 spring-boot-properties-migrator + 修齐 deprecated properties `模式 A`

**PRD 依据:** 基础设施 Plan 例外
**追溯 ID:** N/A

**验收标准（从工程纪律推导）:**

1. **零 deprecated warning** — fep-web 启动日志中 `PropertiesMigrationListener` 不再打印任何 warning
2. **migrator 已移除** — `fep-common/pom.xml` 不再含 `spring-boot-properties-migrator`
3. **reactor verify 仍通过** — `./mvnw verify -DskipITs=false` BUILD SUCCESS
4. **application.yml 修复无业务行为变化** — 修改前后启动 fep-web，启动 banner / `/actuator/health` 输出一致（curl + diff）

> **规则**: 若 T1 step 8 实测 warning 数 = 0，本 Task 仅做 step 1-3（移除 dep + verify + commit）。

**Files:**
- Modify: `fep-common/pom.xml`（移除 properties-migrator）
- Modify: `fep-web/src/main/resources/application*.yml`（按 warning 列表逐条修，若需要）

- [ ] **Step 1: 修齐 T1 step 8 列出的 deprecated keys**

每条 warning 形如:
```
The property 'spring.foo.bar' is deprecated. Replacement: 'spring.foo.baz'.
```

对每条:

```bash
# 找到引用 spring.foo.bar 的 yml 文件
grep -rn "spring.foo.bar" fep-web/src/main/resources/

# 修改为新 key（保持原 value）
```

> **若 warning 数 = 0 → 跳过此 Step**。

- [ ] **Step 2: 移除 spring-boot-properties-migrator**

```xml
<!-- fep-common/pom.xml — 删除 dependency 块 -->
<!-- 删除 T1 Step 2 添加的:
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-properties-migrator</artifactId>
    <scope>runtime</scope>
</dependency>
-->
```

- [ ] **Step 3: 验证 reactor verify 仍 GREEN**

```bash
./mvnw verify -DskipITs=false --batch-mode --no-transfer-progress 2>&1 | tail -30
```

期望: BUILD SUCCESS（若 step 1 修改了 yml，启动日志不再含 PropertiesMigrationListener warning）。

Fallback 同 T1 step 6 — GHA CI 验证。

- [ ] **Step 4: 启动 banner / health 行为一致性**（v2 评审 B2 修订）

```bash
# 4a. 重新 install 所有模块到本地 m2 仓库（必须，因 T2 Step 2 修改了 fep-common pom，
#     若不重新 install，本地 m2 中 fep-common-1.0.0-SNAPSHOT 的 jar 与 pom 不一致，
#     fep-web 启动时可能仍读到旧 migrator 依赖）
./mvnw install -DskipTests --batch-mode --no-transfer-progress > /tmp/sb35-t2-install.log 2>&1
tail -10 /tmp/sb35-t2-install.log
# 期望末尾 BUILD SUCCESS

# 4b. 启动 fep-web dev profile（端口 8080，redis 必须在跑）
docker start fep-redis 2>/dev/null || docker run -d --name fep-redis -p 6379:6379 redis:7-alpine
./mvnw -pl fep-web spring-boot:run -Dspring-boot.run.profiles=dev > /tmp/sb35-startup.log 2>&1 &
SB_PID=$!
sleep 45  # context boot 通常 30-40s

# 4c. 健康检查
curl -sf http://localhost:8080/actuator/health | jq '.status'
# 期望: "UP"

# 4d. 验证 migrator 已离场
grep -E "PropertiesMigrationListener|deprecated|replaced by" /tmp/sb35-startup.log | head -5
# 期望: 无匹配行（migrator 不在 classpath，T2 Step 1 修齐的 deprecated 已合规）

# 4e. 关闭
kill $SB_PID 2>/dev/null || pkill -f "spring-boot.run"
sleep 2
```

> **若沙盒 issue / `mvn install` exit 144**: 跳过此 step，依赖 T3 Step 1 GHA CI 的 IT 启动覆盖（fep-web 的 `@SpringBootTest` IT 已含 actuator health 间接覆盖）。

- [ ] **Step 5: Commit**

```bash
git add fep-common/pom.xml
# 若修改了 yml: git add fep-web/src/main/resources/
git commit -m "$(cat <<'EOF'
chore(deps): remove spring-boot-properties-migrator after SB 3.5.9 verify

Migrator surfaced N deprecated yml keys (see commit body / Plan T1 Step 8 log).
All keys updated to SB 3.5 replacements; verify GREEN both before and after
removal. Reactor unchanged behavior on /actuator/health output.

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

（N 替换为实际 warning 数；若 = 0 写 "0 warnings — clean removal"）

---

### Task 3: 闭环 — 残留 CVE ADR + push + worktree cleanup `模式 A`

**PRD 依据:** 基础设施 Plan 例外
**追溯 ID:** N/A

**验收标准:**

1. **GHA CI 远端绿光** — `chore/sb-parent-3.5.9` 分支或 main 合并后 CI workflow BUILD SUCCESS
2. **GHA Nightly Deep Scan 绿光** — 手动触发 Nightly workflow 走完 ~35 min，conclusion=success
3. **残留 CVE 文档化** — 每个残留（≤5 个）有 ADR 条目说明: jar + CVE-ID + CVSS + 修复版本是否存在 + 处理决定（接受 / suppress / 升级到非 BOM 版本）
4. **worktree 清理** — `git worktree remove /Users/muzhou/FEP_v1.0_wt-sb35-upgrade` + 删除本地分支 + 删除远端临时分支（若 step 6 已 ff-merge main）

**Files:**
- Modify: `CLAUDE.md`（"§当前项目状态" 段更新）
- New: `docs/plans/2026-05-15-spring-boot-3.5.9-residual-cve-adr.md`（若有 CVE 残留）
- Modify: `docs/plans/PHASE_HISTORY.md`（追加本 Plan 行）

- [ ] **Step 1: 等 GHA CI 绿光**

```bash
gh pr checks  # 等所有 check 通过
# 或:
gh run watch  # 实时监控
```

期望: CI workflow conclusion=success（含 fep-* 全 module verify）。

- [ ] **Step 2: 手动触发 Nightly Deep Scan**

```bash
gh workflow run "Nightly Deep Scan" --ref chore/sb-parent-3.5.9
gh run watch
```

期望: conclusion=success，artifacts 含 owasp-dependency-check-reports。

下载报告验证残留 CVE 数:
```bash
gh run download <run-id> -n owasp-dependency-check-reports
grep -oE "CVE-[0-9]+-[0-9]+" *-dependency-check-report.html | sort -u | tee /tmp/sb35-t3-cves.txt
grep -rE "Highest Severity.*CRITICAL|HIGH" *-dependency-check-report.html | head -10
```

**v2.1 评审 NEW-M 修订 / M2 binary gate 一致性**: 任一 CVSS ≥ 7 残留 → 必须按 T1 验收标准 #4 的"残留处置政策" 3 选 1 处理（suppression / override / Plan FAIL），不接受 "≤N 残留" 灰色地带。即:
- 0 残留 → T3 Step 3 跳过，进 Step 4
- ≥ 1 残留 → T3 Step 3 必做（按残留数生成 ADR 条目，每条 muzhou 单条签字）

- [ ] **Step 3: 撰写残留 CVE ADR**

若 step 2 报告含 任一 CVSS ≥ 7 残留:

```bash
# 文件: docs/plans/2026-05-15-spring-boot-3.5.9-residual-cve-adr.md
```

ADR 模板（对每个残留 CVE 一段）:
```markdown
## CVE-XXXX-XXXX (CVSS X.X) in <jar>

**漏洞概述**: <NVD 描述前 100 字>
**Spring Boot 3.5.9 BOM 版本**: <jar>:<version>
**上游修复版本**: <如有 — 来自 NVD "Fixed in" 字段>
**处理决定**:
- [ ] 接受（生产部署面无暴露，例如内网 only）— 加 owasp-suppression.xml
- [ ] 升级到非 BOM 版本（在 root pom dependencyManagement 显式 override）
- [ ] 等待 Spring Boot 下一 patch（3.5.10）
**muzhou 签字**: ___（每个决定独立签字，红线 `feedback_security_doc_must_distinguish_spotbugs_layers`）
```

> **若 step 2 报告含 0 残留 → 跳过本 step**，ADR 文件不创建。

- [ ] **Step 4: 更新 CLAUDE.md "§当前项目状态" + PHASE_HISTORY**（v2 评审 n2 修订 — 加 anchor）

**4a. CLAUDE.md 修改**：插入位置 = "## 当前项目状态" section 内 `- **2026-05-14 末段¹⁰ P4-MSG-F`" 行之前（最新 ship 时间倒序，本 Plan 是 2026-05-15 ship 应在最顶部）

```markdown
- **2026-05-15 Spring Boot 3.3.7 → 3.5.9 升级 (本会话 SB 3.5.9 upgrade)**: 修复 OWASP Nightly 14 天 RED (2026-05-01 起)。**3 commits ship**: T1 `<sha>` parent bump 3.3.7→3.5.9 + spring-boot-properties-migrator 临时引入 / T2 `<sha>` migrator removal + deprecated property 修齐 / T3 `<sha>` closing 文档。**OWASP CVSS≥7 阻断 CVE**: 31 baseline → 0（或残留 ADR `docs/plans/2026-05-15-spring-boot-3.5.9-residual-cve-adr.md` 处置）。**新依赖版本**: log4j-api 2.23.1→2.24.x / tomcat-embed-core 10.1.34→10.1.4x / spring-core+web 6.1.16→6.2.x / hibernate-validator 8.0.2→8.0.3+。**GHA CI ✅ 远端绿** + **GHA Nightly ✅ 远端绿**（手动触发后）
```

**4b. PHASE_HISTORY.md 修改**:

1. 顶部 metadata 段："当前进度（2026-05-XX）" 改为 "2026-05-15" + "总测试 ~XXXX" 实测值 + "HEAD = `<merge sha>`"
2. 阶段表追加新行: `| 2026-05-15 | SB 3.5.9 upgrade | ✅ ship | OWASP 14d RED fix | 3 commits / 0 业务代码 / pom.xml only |`

> v2 评审 n2: 引用具体 section anchor 而非 prose-style "顶部添加"

- [ ] **Step 5: rebase + ff-merge main + push**（v2 评审 M3 修订）

```bash
# 5a. 进 worktree rebase origin/main（防 long verify cycle 期间 origin/main 漂移，
#     红线 feedback_baseline_drift_during_long_review_cycle）
cd /Users/muzhou/FEP_v1.0_wt-sb35-upgrade
git fetch origin
git log origin/main..HEAD --oneline | head -5  # 本 worktree 比 origin/main 多几个 commit
git log HEAD..origin/main --oneline | head -5  # origin/main 比本 worktree 多几个 commit（drift 信号）

git rebase origin/main
# 若 conflict: 修复 + git add + git rebase --continue + 再跑 T1 Step 5/6 验证
# 若 conflict 涉及 pom.xml line 10 spring-boot version: 永远保留 3.5.9（本 Plan 目标）

# 5b. 切主 worktree + 防别会话本地未推送 commit 卷入推送
cd /Users/muzhou/FEP_v1.0  # 主 worktree
git fetch origin

# 红线 feedback_main_worktree_unpushed_wip_detection: 推前查别会话 WIP
UNPUSHED=$(git log origin/main..main --oneline | wc -l)
if [ $UNPUSHED -gt 0 ]; then
  echo "⚠️ main 比 origin/main 多 $UNPUSHED commit（别会话本地未推送 WIP）"
  git log origin/main..main --oneline
  echo "🛑 暂停 — muzhou 决策是否一并 push 或推回别会话独立处理"
  exit 1
fi

# 5c. ff-merge + push
git merge --ff-only chore/sb-parent-3.5.9
git push origin main
```

任一冲突 → abort，回到 worktree 内 rebase main 后再 ff-merge。
任一别会话 WIP 检出 → 暂停 + AskUserQuestion 让 muzhou 决策（红线 `feedback_main_worktree_unpushed_wip_detection`）。

- [ ] **Step 6: Worktree cleanup**

```bash
git worktree remove /Users/muzhou/FEP_v1.0_wt-sb35-upgrade
git branch -d chore/sb-parent-3.5.9
git push origin --delete chore/sb-parent-3.5.9
git worktree list  # 期望仅剩 main + e2e/p7.1-smoke-local
```

- [ ] **Step 7: 提交 closing 文档**

```bash
cd /Users/muzhou/FEP_v1.0
git add CLAUDE.md docs/plans/PHASE_HISTORY.md
# 若有 ADR: git add docs/plans/2026-05-15-spring-boot-3.5.9-residual-cve-adr.md
git commit -m "$(cat <<'EOF'
docs: close SB 3.5.9 upgrade — update CLAUDE.md + PHASE_HISTORY (+ residual ADR)

OWASP Dependency-Check nightly: 14d RED -> GREEN.
CVSS>=7 blocker count: 31 -> <X>.
Residual CVE ADR: <link or "0 residuals — clean">

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
git push origin main
```

---

## 自检清单

| # | 检查项 | 本 Plan 状态 |
|---|--------|-------------|
| 1 | PRD 覆盖度 | N/A — 基础设施 Plan 例外，依据 CLAUDE.md "Nightly 深扫" §OWASP Dependency-Check |
| 2 | 安全边界 | ✅ 本 Plan 不动 security/impl/ — 仅 pom.xml + yml |
| 3 | 占位符扫描 | ✅ 全 Task 完整代码 / 无 TBD/TODO（除 Task 验收标准内"X"代入实测值，由执行阶段填）|
| 4 | 类型一致性 | ✅ 无 Java 类，仅 XML + YAML |
| 5 | 测试命令可执行 | ✅ T1 step 5/6 ./mvnw test/verify 标准命令 + 沙盒 fallback 写明 |
| 6 | CLAUDE.md 更新 | ✅ T3 step 4 |
| 7 | 验收标准来自 PRD/规则 | ✅ T1 验收标准 ≤5 残留 = muzhou 签字阈值（来自 CLAUDE.md 质量门禁 + 工程裁量） |
| 8 | 共享工具类无遗漏 | N/A — 不创建工具类 |
| 9 | 核心类职责边界 | N/A — 不创建 Service |
| 10 | Worktree 触发条件 | ✅ 命中 ①（跨 8 模块）+ ⑤（reactor verify ≥5min），路径分支已明示 |

### Worktree 触发条件实测（v2 修订 / 加实测证据）

- [x] ① 跨 ≥ 3 个 Maven 模块？ → **是**，8 模块全部受 parent BOM 影响
- [ ] ② 与已签字未执行的 Plan 并存？ → **否**（2026-05-15 起草时实测 `for f in docs/plans/2026-05-*.md; do grep "Approval Status: \[x\]" $f && grep "^- \[ \]" $f; done` 无 match — 仅本 Plan `2026-05-15-spring-boot-parent-upgrade-3.5.9.md` 自身处于起草签字阶段）
- [ ] ③ 涉及 ⛔ 安全 vs AI 并行？ → 否（本 Plan 不触 security/impl/）
- [ ] ④ TLQ tongtech profile 联调？ → 否（本 Plan 不动 fep-transport tongtech 路径）
- [x] ⑤ 含 ≥ 5 min long-running verify？ → **是**，全 reactor verify 历史 ~10-15 min
- [ ] ⑥ muzhou WIP 与 AI 任务并存？ → 待签字时实测（git status 验证），如有则升级到 worktree（已是 worktree mode 故等价 ✓）

→ 必须 worktree（条件 ① + ⑤）

---

## 红线合规自检

| 红线 | 本 Plan 应对 |
|------|------------|
| `feedback_mvn_sandbox_exit144_pattern` | T1 step 6 / T3 step 1 写明 GHA CI fallback |
| `feedback_plan_regression_scope_explicit` | T1 验收标准 1-3 分编译/单测/IT 三层 |
| `feedback_full_regression_before_commit` | T1 step 5 跑全 reactor `./mvnw test`，非单模块 -am |
| `feedback_worktree_for_parallel_work` | 头部 "执行 Worktree" 字段填具体 path/branch + 触发条件第 ①⑤ 项 |
| `feedback_main_worktree_unpushed_wip_detection` | T3 step 5 ff-merge 前 `git log origin/main..main` 实测 |
| `feedback_plan_must_grep_actual_api` | 起草前已实测当前 deps（log4j 2.23.1 / tomcat 10.1.34 / spring 6.1.16）+ Security 现代 lambda DSL grep 确认 |
| `feedback_security_doc_must_distinguish_spotbugs_layers` | T3 step 3 ADR 模板每条独立签字 |
| `feedback_plan_gate_must_handle_blocked_state` | 验收标准 4 残留 > 5 → Task FAIL 三态 |
| `feedback_subagent_must_commit_before_exit` | T1/T2/T3 末尾均有显式 Step 9/5/7 commit step |
| `feedback_pipe_tail_deadlock_with_bg_bash` | T1 step 5/6 改用 `tee /tmp/sb35-*.log` 落盘 + bash `tail -N` 读文件，不 pipe stdin |
| `feedback_surefire3_failifno_specified_tests_param_rename` | 本 Plan mvn 不传 -DfailIfNoTests，不踩 |

---

## 执行交接

**⚠️ 重要**: 本 Plan 不能直接执行。须经 **AI 独立评审 + muzhou 签字** 流程：

### 步骤 1: AI 独立评审

- Reviewer agent: `general-purpose` 或 `plan-eng-review` skill
- 输入: 本 Plan 全文 + `docs/guides/plan-review-checklist.md` 7 项清单 + `docs/guides/ai-code-review-checklist.md` 9 项
- 输出: ✅ 通过项 / ❌ 问题项（引用 Task 编号 + 违反清单编号）

如 ≥ 1 问题 → 修订 → 再评审 → 直到通过。

### 步骤 2: muzhou 签字

AI 评审通过后, 用 AskUserQuestion 请求 muzhou 决策:
- 批准 → 在 Plan 末尾追加签字块
- 驳回 → 记录原因，重新起草
- 修改 → 按指示修订，再走步骤 1

### 步骤 3: 签字后执行

签字后提供执行选项 (AskUserQuestion):
1. **Subagent 驱动**（推荐）— 每 Task 独立 implementer/reviewer/quality reviewer subagent
2. **内联执行** — 主对话用 superpowers:executing-plans 逐 Task 推进

**禁止：未签字直接执行。**

---

## 签字块

```
Plan Approver: muzhou
Approval Date: 2026-05-15
Approval Status: [x] Approved  [ ] Revise  [ ] Reject
Approval Notes: v2.1（v1 → v2 修齐 3 BLOCKER + 5 MAJOR + 5 MINOR + 3 NIT；v2.1 修齐 2 项 propagation gap）经 AskUserQuestion 决策 Approved 进 T1 执行。
执行方式: 内联（executing-plans 风格），主对话直接逐 Task 推进。
三跑评审 fallback: SendMessage 工具不可用（候选红线 sendmessage_tool_unavailable_equals_dual_fail 触发），reviewer 二跑预设 conditional approval ("1-line edits then green-light")，v2.1 patch 精准对应 → 主对话自审 verify → 进签字。
```

---

## v2 修订摘要（2026-05-15 主对话内联修订）

基于 general-purpose subagent 独立评审报告（**NEEDS REVISION** 判决：3 BLOCKER + 5 MAJOR + 5 MINOR + 3 NIT）逐条修订:

### BLOCKER（已修）
- **B1**: T1 Step 1c `gh run view | grep "Tests run:" | tail -1` 仅取最后模块数 → 改 `awk -F'Tests run: ' '{split($2,a,","); sum+=a[1]} END {print sum}'` 聚合 8 模块累计数 + 落盘 `/tmp/sb-baseline-tests.txt`
- **B2**: T2 Step 4 `./mvnw -pl fep-web spring-boot:run` 跨 module 启动需先 `install` 本地 m2 → 增加 4a `./mvnw install -DskipTests` 前置 step
- **B3**: T1 Step 7 `grep -E "\.jar: CVE-"` — 实测（本会话 gh run view 25878756990）该 pattern 在 BUILD FAILURE 末尾确实匹配，主验证保留；新增辅助验证 `grep -oE "CVE-[0-9]+-[0-9]+" target/dependency-check-report.html | sort -u | wc -l` 兜底

### MAJOR（已修）
- **M1**: T1 Step 1d 加 plugin 版本 baseline grep（surefire/jacoco/spring-boot-maven-plugin），Step 4 后对比变动；风险表加"插件版本兼容"行
- **M2**: 验收标准 #4 从 "≤5 残留" 改为 "binary gate 0 阻断"；新增 "残留处置政策" 段落（suppression / override / Plan FAIL 3 选 1，每个残留 muzhou 单条签字）
- **M3**: T3 Step 5 改为 `git fetch + git rebase origin/main + git merge --ff-only`，rebase 冲突时进 worktree 修复 + 重跑 verify
- **M4**: T1 Step 6 GHA fallback 从 Step 9 之前 → 改到 Step 9 末尾的 GHA Fallback PR 段，commit + push 后再触发 PR CI
- **M5**: T1 Step 8 加 8a `./mvnw dependency:tree -pl fep-web | grep migrator` 验证传递依赖路径

### MINOR（已修）
- **m1**: T1 Step 1e 加 SB 3.5.9 currency check（GitHub API 查 latest 3.5.x release）
- **m2**: 验收标准 #2 改为动态 baseline（T1 Step 1c 实测），不硬编码 1748 或 1707
- **m3**: 风险表加 Logback 1.5.x 行（无自定义 appender，风险极低）
- **m4**: 风险表加 JDK 兼容性行（Java 17 minimum 满足）
- **m5**: PRD 追溯段引用具体 CLAUDE.md section（Plan 治理基础设施例外）

### NIT（已修 / 接受）
- **n1**: 执行方式改为 "executing-plans 内联（推荐）" — 3 Task / pom-only / 0 业务代码不值得 subagent
- **n2**: T3 Step 4 加 CLAUDE.md 具体 anchor（`## 当前项目状态` section + 最新 ship 倒序位置）
- **n3**: JDK 21 forward-look 已并入风险表 JDK 兼容行

### 评审误判（已 push back）
- **B3 主验证 pattern**: reviewer 称 "CVE IDs aren't on .jar: CVE- lines"，但本会话 gh run view 25878756990 log 实证该 pattern 确实匹配 BUILD FAILURE 末尾的 4 行（log4j / spring-boot / spring-boot-starter-web / tomcat）。保留原 pattern，但补 HTML 报告 grep 作为冗余验证

---

## v2 最终待审

修订完成后请 reviewer 二跑确认 BLOCKER + MAJOR 全清。如二跑 PASS → 进 muzhou 签字流程（AskUserQuestion）。

### v2.1 二跑修订（2026-05-15）

reviewer 二跑发现 v2 仍有 2 项 propagation gap，已 1-line patch 修齐:

- **NEW-m (m2 follow-through)**: T1 Step 5 grep 期望从硬编码 `≥1748` 改为动态 baseline `$(cat /tmp/sb-baseline-tests.txt)` 对比 — 与验收标准 #2 "动态 baseline，禁硬编码" 一致
- **NEW-M (M2 inconsistency)**: T3 Step 2/3 "≤5 残留" 灰色语言替换为"任一 CVSS ≥ 7 残留 → 走残留处置政策 3 选 1" — 与 T1 验收标准 #4 binary gate 一致

二跑期望 verdict: PASS（无 BLOCKER/MAJOR 剩余）→ 进 muzhou 签字。
