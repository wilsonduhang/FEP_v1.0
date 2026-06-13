# FEP Spring Boot 3.5.15 BOM override 清理实施计划（follow-up）

> **执行方式:** hybrid（红线 `feedback_harness_bg_detach_hybrid_default`：主对话实施 edits + 前台 mvn + commit；subagent 仅做 spec/quality 评审）。步骤使用 `- [ ]` 复选框跟踪。

**版本:** v0.2（2026-06-13；v0.1 原 scope 为 parent 3.5.14→3.5.15 升级，santa Round 1 BLOCKER-1 实测发现与别会话已签字在途 Plan `2026-06-13-spring-boot-3515-cve-patch.md`（wt-sb-3515）重复 → muzhou 2026-06-13 拍板改版为本 follow-up；同轮修 BLOCKER-2 + C1-C5）

**目标:** 在途 Plan merge（parent 已 3.5.15）后，删除根 pom 两个因 3.5.14 BOM 滞后而加的手动 override：
- `tomcat.version=10.1.55` — 3.5.15 BOM 钉同版本，override 冗余（删除后版本不变）
- `netty.version=4.1.134.Final` — 3.5.15 BOM 钉 **4.1.135.Final**，override 现反向钉低 netty（删除后净升一个 patch）

**前置依赖（硬 gate）:** 在途 PR（分支 `chore/spring-boot-3515`）已 merge 到 origin/main，即 origin/main 根 pom parent = 3.5.15。**Task 0 含实测 gate 命令，gate 不过禁止实施。**

**执行 Worktree:** `/Users/muzhou/FEP_v1.0_wt-override-drop`（分支 `chore/sb3515-override-cleanup`，基线 `origin/main`(merge 后)；触发条件第 ⑥ 项多会话活跃——wt-gm-s2b / wt-simplify-r3 等并发，红线 `feedback_worktree_isolates_fs_not_logic_domain` 无交集不豁免；命名刻意避开在途 `wt-sb-3515` 近似碰撞）

**架构:** 仅改根 `pom.xml`：删 2 个 property override 及其注释块（共 2 块）。不动 parent 版本、不动 owasp-suppression.xml、不动任何 Java 代码。

**技术栈:** Java 17 / Spring Boot 3.5.15 / Maven 多模块（netty 仅经 lettuce-core ← spring-boot-starter-data-redis 落在 fep-web classpath；tomcat-embed 落在 fep-web）。

**AI 协同模式:** 全计划 `模式 A`（依赖/配置变更 + 既有测试回归，无新业务代码）。

---

## 实测依据（2026-06-13，红线 `feedback_currency_check_datasource_coverage` / `feedback_doc_data_grep_first`）

| 数据点 | 来源 | 实测值 |
|---|---|---|
| 3.5.15 BOM tomcat | Maven Central `spring-boot-dependencies-3.5.15.pom`（curl 实测） | 10.1.55（= 现 override，持平） |
| 3.5.15 BOM netty | 同上 | 4.1.135.Final（> 现 override 4.1.134.Final；netty-transport 4.1.135.Final 在 Central 实测 HTTP 200） |
| tomcat override 块 | `pom.xml` L64-69（注释块起始 L64，santa C1 修正 off-by-one）| 注释 + `<tomcat.version>10.1.55</tomcat.version>`(L69) |
| netty override 块 | `pom.xml` L71-80 | 注释 + `<netty.version>4.1.134.Final</netty.version>`(L80) |
| property 引用范围 | `grep -rn "tomcat.version\|netty.version" --include=pom.xml`（全 repo） | 仅根 pom 2 处，子模块零引用 |
| owasp-suppression netty 条目 | `owasp-suppression.xml` CVE-2026-42582 块（santa 实测 L180-181） | `packageUrl regex="^pkg:maven/io\.netty/netty-.*@.*$"` 版本段 `@.*` 通配 + 单 CVE pin，netty 4.1.135 不失配；**既有条目逐字不动**（红线 `feedback_owasp_cpe_false_positive_discipline`） |
| 在途 Plan 不含 override 清理 | `git show ab0d4cfc:docs/plans/2026-06-13-spring-boot-3515-cve-patch.md` grep `tomcat|netty|override` | 0 命中（本 Plan 增量价值依据） |
| Flyway | 本 Plan 无迁移 | N/A |

**行号漂移条款（红线 `feedback_baseline_drift_during_long_review_cycle`）:** 上表 L64-69/L71-80 为 3.5.14 基线实测；在途 Plan 仅替换 parent version 一行（不增删行），行号预期不变，但 **Task 1 Step 1 实施前必须重 grep 实测行号**，以实施时实测为准。

**currency 复测条款:** 签字到实施跨 ≥24h（含等待在途 merge 的时间）→ 实施前重跑 BOM 钉版 curl 实测（红线 `feedback_dependency_plan_currency_recheck`）。

**不在本 Plan 范围:**
- parent 版本升级（在途 Plan 负责）
- Spring Boot 4.x 大版本升级
- owasp-suppression.xml 任何改动
- backlog 既有 <CVSS7 机会性升级（lang3/poi/commons-compress）

---

## 风险与回退（santa C3）

patch 内 BOM 回归管理，理论零 API 变更：tomcat 删 override 后版本逐字节不变（10.1.55=10.1.55）；netty 4.1.134→4.1.135 为上游 bug-fix patch（无 CVE 驱动，纯卫生升级）。回退 = revert 单 commit（两块 override 原文随 git history 可恢复）。若回归暴露 netty/lettuce 行为偏移 → revert 后改为仅删 tomcat override、netty override 升 pin 4.1.135 + 注释说明（须 muzhou 另行确认）。

---

## 文件结构

| 文件路径 | 职责 | 操作 | AI 模式 |
|----------|------|------|:-------:|
| `pom.xml`（根） | 删 2 个 override property 块 | 修改 | A |
| `docs/plans/2026-06-13-sb3515-bom-override-cleanup.md` | 本 Plan | 新建 | A |

共享工具类清单：无（无新代码）。核心类职责边界：无（不触及任何 Java 类）。

---

### Task 0: 前置 gate 实测 + worktree 创建 `模式 A`（santa BLOCKER-2 修复）

- [ ] **Step 1: gate — 在途 PR 已 merge 实测**

```bash
cd /Users/muzhou/FEP_v1.0 && git fetch -q origin
git show origin/main:pom.xml | grep -A2 "spring-boot-starter-parent" | grep "<version>"
```
期望: `<version>3.5.15</version>`。**非 3.5.15 → STOP，本 Plan 不实施。**

- [ ] **Step 2: currency 复测（跨 ≥24h 时）**

```bash
curl -s https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/3.5.15/spring-boot-dependencies-3.5.15.pom | grep -E "<(tomcat|netty)\.version>"
```
期望: 10.1.55 / 4.1.135.Final（BOM 已发布内容不可变，此步为防御性复核）。

- [ ] **Step 3: worktree 创建（先确认无残留同名）**

```bash
cd /Users/muzhou/FEP_v1.0
git worktree list   # 确认无 wt-override-drop 残留；如有先探活（红线 session_start_existing_worktree_must_question）
git worktree add -b chore/sb3515-override-cleanup /Users/muzhou/FEP_v1.0_wt-override-drop origin/main
```

- [ ] **Step 4: 签字 Plan 复制入 worktree docs/plans/ + commit**

```bash
cp /Users/muzhou/FEP_v1.0/docs/plans/2026-06-13-sb3515-bom-override-cleanup.md /Users/muzhou/FEP_v1.0_wt-override-drop/docs/plans/
cd /Users/muzhou/FEP_v1.0_wt-override-drop
git add docs/plans/2026-06-13-sb3515-bom-override-cleanup.md
git commit -m "$(cat <<'EOF'
docs(plans): add signed SB 3.5.15 BOM override cleanup plan

AI-Generated: claude-code
Reviewed-By: muzhou
EOF
)"
```

---

### Task 1: 删除 override + 解析验证 `模式 A`

**验收标准:**
1. `./mvnw help:evaluate -Dexpression=tomcat.version` → `10.1.55`（来源 BOM 而非本地 property）
2. `./mvnw help:evaluate -Dexpression=netty.version` → `4.1.135.Final`
3. 升级前后 `dependency:tree -Dincludes=io.netty:*` artifact 集合一致（santa C2：动态 baseline 比对，禁硬编码 7），仅版本号 4.1.134→4.1.135
4. 根 pom 不再含 `<tomcat.version>` / `<netty.version>` property 及其注释块

- [ ] **Step 1: 实测行号 + 删除前基线 tree**

```bash
export JAVA_HOME=/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home; export PATH="$JAVA_HOME/bin:$PATH"
cd /Users/muzhou/FEP_v1.0_wt-override-drop
grep -n "tomcat.version\|netty.version\|Tomcat override\|netty override" pom.xml   # 实测当前行号（行号漂移条款）
./mvnw dependency:tree -pl fep-web -Dincludes="io.netty:*" --batch-mode > /tmp/override-drop-netty-before.log 2>&1
grep -oE "io\.netty:[a-z-]+" /tmp/override-drop-netty-before.log | sort -u > /tmp/netty-artifacts-before.txt
cat /tmp/netty-artifacts-before.txt
```

- [ ] **Step 2: 编辑根 pom — 整块删除两段（以 Step 1 实测行号为准）**

删除内容（3.5.14 基线 L64-69）：
```xml
        <!-- Tomcat override: SB 3.5.14 BOM pins 10.1.54 which carries
             CVE-2026-41284 (7.5) + CVE-2026-42498 (7.3). Both fixed in 10.1.55
             (patch within 10.1.x line). Spring Boot honors this property to
             bump tomcat-embed-* without changing the parent BOM. See
             docs/plans/2026-05-15-spring-boot-3.5.14-residual-cve-adr.md -->
        <tomcat.version>10.1.55</tomcat.version>
```

删除内容（3.5.14 基线 L71-80）：
```xml
        <!-- netty override: SB 3.5.14 BOM pins 4.1.132.Final which carries 12
             netty CVEs (FEP classpath hits CVE-2026-42583 via netty-codec).
             Other 11 affect codec-http/dns/http3/redis/mqtt/handler-proxy not
             in classpath). All 12 fixed in 4.1.133.Final (2026-05-04 security
             release); 4.1.134.Final (2026-05-20 latest GA) carries those fixes
             plus bug-fixes. netty arrives transitively via lettuce-core <-
             spring-boot-starter-data-redis. Spring Boot honors this property to
             bump netty-* without changing the parent BOM. See
             docs/plans/2026-05-22-netty-4.1.134-upgrade.md -->
        <netty.version>4.1.134.Final</netty.version>
```

> 删除理由写入 commit message（BOM 追平/超过 override），不留墓碑注释。

- [ ] **Step 3: 解析验证 + 前后 tree 比对**

```bash
export JAVA_HOME=/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home; export PATH="$JAVA_HOME/bin:$PATH"
cd /Users/muzhou/FEP_v1.0_wt-override-drop
./mvnw -q help:evaluate -Dexpression=tomcat.version -DforceStdout --batch-mode; echo
./mvnw -q help:evaluate -Dexpression=netty.version -DforceStdout --batch-mode; echo
./mvnw dependency:tree -pl fep-web -Dincludes="io.netty:*" --batch-mode > /tmp/override-drop-netty-after.log 2>&1
grep -oE "io\.netty:[a-z-]+" /tmp/override-drop-netty-after.log | sort -u > /tmp/netty-artifacts-after.txt
diff /tmp/netty-artifacts-before.txt /tmp/netty-artifacts-after.txt && echo "ARTIFACT SET UNCHANGED"
grep -c "4.1.135.Final" /tmp/override-drop-netty-after.log
grep -c "4.1.134.Final" /tmp/override-drop-netty-after.log || true
```
期望: 10.1.55 / 4.1.135.Final / diff 空（集合一致）/ 4.1.134 计数 0。

- [ ] **Step 4: 提交**

```bash
cd /Users/muzhou/FEP_v1.0_wt-override-drop
git add pom.xml
git commit -m "$(cat <<'EOF'
chore(deps): drop tomcat/netty version overrides superseded by SB 3.5.15 BOM

3.5.15 BOM pins tomcat 10.1.55 (equal to override, no version change)
and netty 4.1.135.Final (newer than override 4.1.134.Final). Both
overrides were added against the stale 3.5.14 BOM and are now obsolete;
the netty override was actively pinning netty below the BOM version.
Net effect: tomcat unchanged, netty +1 patch via BOM.

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

### Task 2: 回归验证 + PR `模式 A`

**验收标准（红线 `feedback_plan_regression_scope_explicit` 分两层）:**
- **strong:** GHA CI 全绿（Build/Test/Quality 含全 reactor verify + spotbugs + ArchUnit + Checkstyle + JaCoCo）——镜像在途 3515 Plan "本地 minimum、strong 委托 GHA" 先例
- **minimum（本地，commit 前）:** Task 1 Step 3 解析断言全过 + `./mvnw -pl fep-web -o test` 0 fail（netty/tomcat 唯一落点模块；红线 `feedback_single_module_regression_no_am_flag` 不带 -am；若 ~/.m2 缺上游 SNAPSHOT 先一次性 `./mvnw -am -pl fep-web install -DskipTests` 装 jar 再单模块跑）

- [ ] **Step 1: 并发与负载前置检查（santa C4：通配检测所有非本会话构建进程）**

```bash
uptime   # load 1min < 30 才起跑
pgrep -fl "MavenWrapperMain|surefirebooter" | grep -v "wt-override-drop" || echo "无其他会话构建进程"
# 非空 → 别会话构建中，等待或仅跑解析断言推迟单模块 test
```

- [ ] **Step 2: 本地 minimum 回归（前台 redirect-to-file，禁 |tail）**

```bash
export JAVA_HOME=/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home; export PATH="$JAVA_HOME/bin:$PATH"
cd /Users/muzhou/FEP_v1.0_wt-override-drop
./mvnw -pl fep-web -o test --batch-mode --no-transfer-progress > /tmp/override-drop-fepweb-test.log 2>&1
echo "exit=$?"
grep -E "Tests run:.*Failures: [1-9]|Tests run:.*Errors: [1-9]|BUILD FAILURE" /tmp/override-drop-fepweb-test.log | head -20   # 期望空
```

- [ ] **Step 3: push + PR**

```bash
cd /Users/muzhou/FEP_v1.0_wt-override-drop
git push -u origin chore/sb3515-override-cleanup
gh pr create --title "chore(deps): drop tomcat/netty overrides superseded by SB 3.5.15 BOM" --body "$(cat <<'EOF'
## Summary
Follow-up to the SB 3.5.15 parent upgrade (chore/spring-boot-3515):
- Drop tomcat.version=10.1.55 override — 3.5.15 BOM pins the same version
- Drop netty.version=4.1.134.Final override — 3.5.15 BOM pins 4.1.135.Final; the override was pinning netty BELOW the BOM
- owasp-suppression.xml untouched (CVE-2026-42582 entry is CVE-pinned with @.* version wildcard, unaffected)

## Verification
- help:evaluate: tomcat.version=10.1.55, netty.version=4.1.135.Final (both from BOM)
- dependency:tree before/after: identical netty artifact set, 4.1.134 residue = 0
- Local: fep-web module tests green; full reactor delegated to GHA CI (precedent pattern)

Plan: docs/plans/2026-06-13-sb3515-bom-override-cleanup.md

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```
> gh mutation 遇网络错先 `gh pr view --json state` 实测再决定（红线 `feedback_gh_mutation_network_error_verify_before_retry`）。

- [ ] **Step 4: GHA CI 全绿确认（strong 层）**

```bash
gh pr checks --watch
```

---

### Task 3: 闭环 `模式 A`

- [ ] **Step 1: merge（muzhou 确认后）**

```bash
gh pr merge --squash --delete-branch
gh pr view --json state,mergedAt   # read-verify
```

- [ ] **Step 2: worktree 闭环（实测命令）**

```bash
cd /Users/muzhou/FEP_v1.0 && git fetch -q origin && git merge --ff-only origin/main
git worktree remove /Users/muzhou/FEP_v1.0_wt-override-drop
git worktree list   # 确认无残留
```

- [ ] **Step 3: 更新 `/Users/muzhou/FEP/CLAUDE.md` 当前项目状态段（file write only，禁 git add/commit——红线 `feedback_fep_docs_repo_commit_taboo`）**（santa C5 bold 闭合修正）

---

## 自检清单（writing-plans 10 项）

1. PRD 覆盖度: N/A（基础设施 Plan 豁免，头部已明示）
2. 安全边界: 不触及 security/*，无 ⛔ Task
3. 占位符: 无 TBD/TODO
4. 类型一致性: 无新类型
5. 测试命令可执行: `-pl fep-web -o test` 符合单模块回归红线
6. CLAUDE.md 更新: Task 3 Step 3
7. 验收标准: 全部来自 Maven Central/pom/git 实测，可手验
8. 共享工具类: 无
9. 职责边界: 无
10. Worktree: 命中第 ⑥ 项多会话活跃 → 头部已填路径/分支（避开 wt-sb-3515 近似碰撞）；Task 0 含 add、Task 3 含 remove 实测命令

---

## 签字区

- AI 独立评审（santa-method）Round 1（v0.1 原 scope）: REVISE — BLOCKER-1 在途 Plan 重复（muzhou 拍板改版）+ BLOCKER-2 缺 worktree add + C1-C5（本版全部修复）
- AI 独立评审（santa-method）Round 2（v0.2 本版）: ✅ PASS（2026-06-13；Round 1 全部问题闭合实测确认 + merge 后基线重测零 drift + 删除内容与 origin/main 逐字一致；3 NOTE：① gate 已满足可立即实施 ② Task 1 tree 复用 Task 2 install fallback ③ 实施时严格执行并发检查——评审时实证检出 2 个别会话构建进程）
- Plan Approver（muzhou）: ✅ APPROVED 2026-06-13（AskUserQuestion 批准签字，立即实施）
