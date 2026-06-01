# FEP Simplify Q Drain — DEF-Reuse-R3 AbstractXsdValidationTest 节点/App 常量 (v0.2)

> **执行方式:** superpowers:executing-plans 逐 Task（主线程内联实施 + 每 Task 独立 spec+quality review subagent）。步骤用 `- [ ]` 跟踪。
> **v0.2 修订（Round 1 评审 NEEDS REVISION 后收窄）:** muzhou 选定「收窄到安全子集」。scope 从"全量 33 文件盲 sed"收窄为**仅替换 20 个 subclass 文件中 54 行 wrapCfxTemplate 参数三元组**（`srcNode, desNode, app` 同行）。彻底规避 Round 1 三个 BLOCKER（注释误伤 / NO-EXTEND setter-assert / 残留假绿）。

**目标:** 消化 DEF-Reuse-R3 —— 在 `AbstractXsdValidationTest` 基类定义 4 个节点/App public 常量，并将 fep-processor 测试中 **54 行 `wrapCfxTemplate(...)` 参数三元组**的重复字面量迁移为继承常量引用。**纯测试常量抽取 + 引用迁移，零行为变更**（常量值 === 字面量 → 产出 XML 字节相同）。

**前置依赖:** 无（PR #34 已 merge；0 别会话 worktree 触碰 fep-processor validation 测试 / AbstractXsdValidationTest）。

**baseline:** origin/main HEAD `2cb0a15`（2026-06-01 实测）。

**执行 Worktree:** `/Users/muzhou/FEP_v1.0_wt-r3-xsd-constants`（分支 `chore/r3-abstractxsdtest-node-constants`，触发条件第 2 项 + 多会话活跃）
> 触发判定: ② 与已签字未执行 Plan 并存 ✅（Callback 2b + 7 活跃别会话 worktree）。其余条件不命中。T0 实施前 4 步重测（`feedback_worktree_trigger_is_dynamic_recheck_at_execution`）。

**架构:** 纯测试侧。T1 在 `AbstractXsdValidationTest`（public abstract 基类）新增 4 个 `public static final String`。T2 将 **20 个 `extends AbstractXsdValidationTest` 子类**中的 54 行 wrapCfxTemplate 参数三元组迁移为**无限定**继承常量引用（全 subclass → 无需 import）。**安全性论证（v0.2 核心）:** 替换目标是 3 个**完整三元组**（同行含 srcNode+desNode+app 两个 node 字面量），注释/setter/assert/文本块每处仅含**单个**字面量，**结构上不可能匹配三元组** → 零误伤。行为等同性硬证 = fep-processor 全回归 GREEN（常量值 === 字面量）。

**技术栈:** Java 17 / JUnit 5 / fep-processor test。

**AI 协同模式:** 全 Task **模式 A**（测试常量抽取 + 机械引用迁移，零业务逻辑、零安全边界）。

---

## §0 PRD 追溯与范围声明

**Plan 性质:** Simplify deferred pool drain（meta-Plan），不引入新 FR-ID。

**追溯依据:**
- deferred 来源: `2026-05-19-p4-msg-i-progress-report.md` §Simplify deferred pool: `DEF-Reuse-R3 | LOW-MED | AbstractXsdValidationTest 公开 4 字面量常量（HNDEMP_NODE / INSTITUTION_NODE / APP_HNDEMP / APP_FEPX）`
- muzhou 2026-06-01 AskUserQuestion: ① drain DEF-Reuse-R3 ② Round 1 评审 NEEDS REVISION 后选「收窄到安全子集」（显式批准收窄 scope，满足 `feedback_batch_scope_creep`）

**v0.2 实测精确 scope（baseline `2cb0a15`，grep 可复现）:**
```bash
# 54 行 wrapCfxTemplate 参数三元组（srcNode+desNode 同行），跨 20 文件，全 extends AbstractXsdValidationTest
grep -rln '"A1000142000001", "A1000143000104"\|"A1000143000104", "A1000142000001"' --include='*.java' fep-processor/src/test | wc -l  # 20
grep -rhoE '"(A1000142000001|A1000143000104)", "(A1000142000001|A1000143000104)", "(FEPx|HNDEMP)"' --include='*.java' fep-processor/src/test | sort | uniq -c
#   24  "A1000142000001", "A1000143000104", "FEPx"   (outbound: src=机构 des=HNDEMP app=FEPx)
#   20  "A1000143000104", "A1000142000001", "FEPx"   (src=HNDEMP des=机构 app=FEPx)
#   10  "A1000143000104", "A1000142000001", "HNDEMP" (inbound: src=HNDEMP des=机构 app=HNDEMP)
# 实测 0 跨行 split 参数 / 0 standalone app-arg → 54 三元组即全部 genuine wrapCfxTemplate args
```

**明确不在范围（Round 1 BLOCKER 教训）:**
- **注释**: 11 个文件含 R-2 javadoc 注释 `* ...文本块内嵌入字面量 "A1000143000104"...`（单字面量）—— 不动（三元组不匹配单字面量行）。
- **setter/assert**: `setApp("HNDEMP")` / `setDesNodeCode("A1000143000104")` / `isEqualTo("A1000143000104")`（单字面量；isEqualTo→常量会弱化断言独立性 CONCERN-1）—— 不动。
- **文本块**: `<SendNodeCode>A1000142000001</SendNodeCode>`（无引号，68+ 处）—— text block 无法常量化，不动。
- **NO-EXTEND 文件**: WrapBodyInCfxFix / SupplyChain* / PzInfo / Dzpz / Batch...Integration / AsyncPipeline —— 实测均无三元组（其字面量在 setter/comment），自然排除，无需 static import。
- 低频其它节点（A1000143000201 等）—— 非 4 常量集，保留。
- **占位 srcNode 三元组**: `DzpzInfo3000XsdValidationTest` 含 `"12345678901234", "A1000143000104", "HNDEMP"`（srcNode 为占位 "12345678901234" 非 INSTITUTION_NODE）—— 不匹配 3 个 exact-string 三元组（首节点不同），正确不迁移、无对应常量，保留原样（Round 2 CONCERN-B）。

---

## §1 文件结构

| 文件 | 职责 | 操作 | 模式 |
|------|------|------|:---:|
| `fep-processor/src/test/.../validation/AbstractXsdValidationTest.java` | 新增 4 个 public static final 常量 | 修改 | A |
| 20 个 `extends AbstractXsdValidationTest` 子类（含 54 行三元组） | arg 三元组 → 无限定常量引用 | 修改 | A |

**共享工具类:** 复用既有 `AbstractXsdValidationTest`。**核心类职责边界:** 无新增 Service。

---

## §2 Task 拆解

| Task | 主题 | 依赖 | 模式 |
|------|------|------|:---:|
| T1 | 基类定义 4 常量 | 无 | A |
| T2 | 20 subclass 文件 54 行三元组迁移（无限定引用） | T1 | A |
| T3 | closing（全回归 + 残留核对 + push + PR） | T1-T2 | A |

---

## §3 Task 详细步骤

### Task 1: AbstractXsdValidationTest 定义 4 节点/App 常量 `模式 A`

**验收标准:**
1. 新增 4 个 `public static final String`: `HNDEMP_NODE="A1000143000104"` / `INSTITUTION_NODE="A1000142000001"` / `APP_HNDEMP="HNDEMP"` / `APP_FEPX="FEPx"`（值逐字相等，注意 `FEPx` 小写 x）。
2. 每常量含 Javadoc（checkstyle 要求）。
3. fep-processor 编译 + 既有测试 GREEN（仅增常量，零引用方变更）。

**Files:** Modify `fep-processor/src/test/java/com/puchain/fep/processor/validation/AbstractXsdValidationTest.java`

- [ ] **Step 1: 在 SHARED_VALIDATOR 之后、validator 字段之前插入 4 常量**

```java
    /** 平台/中心侧节点代码（HNDEMP 数据交换中心，{@code HEAD/DesNode} 上行 / {@code SrcNode} 下行）。 */
    public static final String HNDEMP_NODE = "A1000143000104";

    /** 接入机构侧节点代码（银行/供应链信息服务机构，{@code HEAD/SrcNode} 上行 / {@code DesNode} 下行）。 */
    public static final String INSTITUTION_NODE = "A1000142000001";

    /** 平台侧应用代码（{@code HEAD/App}，下行报文）。 */
    public static final String APP_HNDEMP = "HNDEMP";

    /** 机构侧应用代码（{@code HEAD/App}，上行报文；注意小写 x）。 */
    public static final String APP_FEPX = "FEPx";
```

- [ ] **Step 2: 编译 + checkstyle**

```bash
cd /Users/muzhou/FEP_v1.0_wt-r3-xsd-constants
JAVA_HOME=/usr/local/opt/openjdk@17 PATH=/usr/local/opt/openjdk@17/bin:$PATH \
  ./mvnw test-compile checkstyle:check -pl fep-processor > ./t1.log 2>&1; echo "exit=$?" >> ./t1.log
grep -E 'BUILD SUCCESS|BUILD FAILURE|violation|exit=' ./t1.log | tail -5
```
期望: `BUILD SUCCESS` + 0 violations。

- [ ] **Step 3: 提交**

```bash
cd /Users/muzhou/FEP_v1.0_wt-r3-xsd-constants
git add fep-processor/src/test/java/com/puchain/fep/processor/validation/AbstractXsdValidationTest.java
git commit -m "$(cat <<'EOF'
test(processor): add 4 node/app constants to AbstractXsdValidationTest

DEF-Reuse-R3 (P4-MSG-I Reuse deferred pool drain) — define HNDEMP_NODE /
INSTITUTION_NODE / APP_HNDEMP / APP_FEPX public static final constants to
back the wrapCfxTemplate arg-triple migration (54 occurrences / 20 files).

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

### Task 2: 20 subclass 文件 54 行三元组 → 无限定常量引用 `模式 A`

**验收标准:**
1. 3 个确切三元组字符串替换为常量三元组（exact-string 替换，无 regex 歧义）:
   - `"A1000142000001", "A1000143000104", "FEPx"` → `INSTITUTION_NODE, HNDEMP_NODE, APP_FEPX`（24 处）
   - `"A1000143000104", "A1000142000001", "FEPx"` → `HNDEMP_NODE, INSTITUTION_NODE, APP_FEPX`（20 处）
   - `"A1000143000104", "A1000142000001", "HNDEMP"` → `HNDEMP_NODE, INSTITUTION_NODE, APP_HNDEMP`（10 处）
2. 注释/setter/assert/文本块**不动**（三元组结构上不匹配单字面量行 → 自动保证）。
3. fep-processor 全套测试 GREEN（XML 字节相同）。

**Files:** Modify 20 个 subclass 文件（实施时 `grep -rln '"A1000142000001", "A1000143000104"\|"A1000143000104", "A1000142000001"'` 实测列出）。

- [ ] **Step 1: 列出 20 个目标文件**

```bash
cd /Users/muzhou/FEP_v1.0_wt-r3-xsd-constants
grep -rln '"A1000142000001", "A1000143000104"\|"A1000143000104", "A1000142000001"' --include='*.java' fep-processor/src/test > /tmp/r3-triple-files.txt
wc -l /tmp/r3-triple-files.txt   # 期望 20
# 全 extends 核对（期望全部 extends，0 NO-EXTEND）:
while read -r f; do grep -q 'extends AbstractXsdValidationTest' "$f" || echo "NO-EXTEND: $f"; done < /tmp/r3-triple-files.txt
# 期望无输出
```

- [ ] **Step 2: exact-string 三元组替换（不可能误伤单字面量行）**

```bash
cd /Users/muzhou/FEP_v1.0_wt-r3-xsd-constants
while read -r f; do
  sed -i '' \
    -e 's/"A1000142000001", "A1000143000104", "FEPx"/INSTITUTION_NODE, HNDEMP_NODE, APP_FEPX/g' \
    -e 's/"A1000143000104", "A1000142000001", "FEPx"/HNDEMP_NODE, INSTITUTION_NODE, APP_FEPX/g' \
    -e 's/"A1000143000104", "A1000142000001", "HNDEMP"/HNDEMP_NODE, INSTITUTION_NODE, APP_HNDEMP/g' \
    "$f"
done < /tmp/r3-triple-files.txt
```
> v0.2 安全论证: 三个被替换串各含**两个 node 字面量 + app**，注释（单字面量）/setter（单字面量）/assert（单字面量）/文本块（无引号）结构上无法匹配 → 零误伤。无需注释行过滤。

- [ ] **Step 3: 误伤核对 + 编译**

```bash
cd /Users/muzhou/FEP_v1.0_wt-r3-xsd-constants
echo "残留三元组（应 0）:"; grep -rE '"(A1000142000001|A1000143000104)", "(A1000142000001|A1000143000104)", "(FEPx|HNDEMP)"' --include='*.java' fep-processor/src/test | wc -l
echo "R-2 注释保留（应 11，未被误伤）:"; grep -rln '\* .*"A1000143000104"' --include='*.java' fep-processor/src/test | wc -l
echo "文本块保留（应 =108，未伤）:"; grep -rhoE '>A1000143000104<|>A1000142000001<' --include='*.java' fep-processor/src/test | wc -l
echo "setter/assert 保留（应 =12，未伤）:"; grep -rnE 'setApp\("HNDEMP"\)|setDesNodeCode\("A100|isEqualTo\("A100' --include='*.java' fep-processor/src/test | wc -l
JAVA_HOME=/usr/local/opt/openjdk@17 PATH=/usr/local/opt/openjdk@17/bin:$PATH \
  ./mvnw test-compile -pl fep-processor > ./t2-compile.log 2>&1; echo "exit=$?" >> ./t2-compile.log
grep -E 'BUILD SUCCESS|BUILD FAILURE|ERROR' ./t2-compile.log | tail -5
```
期望: 残留三元组 = 0 / R-2 注释 = 11（未伤）/ 文本块 >0 / setter-assert >0 / `BUILD SUCCESS`（无限定常量继承可见）。

- [ ] **Step 4: 跑 *XsdValidationTest 子集确认 GREEN**

```bash
cd /Users/muzhou/FEP_v1.0_wt-r3-xsd-constants
JAVA_HOME=/usr/local/opt/openjdk@17 PATH=/usr/local/opt/openjdk@17/bin:$PATH \
  ./mvnw test -pl fep-processor -Dtest='*XsdValidationTest' -Dsurefire.failIfNoSpecifiedTests=false \
  > ./t2-test.log 2>&1; echo "exit=$?" >> ./t2-test.log
grep -E 'Tests run:|BUILD SUCCESS|BUILD FAILURE|exit=' ./t2-test.log | tail -6
```
期望: `BUILD SUCCESS`，全 GREEN。

- [ ] **Step 5: 提交**

```bash
cd /Users/muzhou/FEP_v1.0_wt-r3-xsd-constants
git add -A fep-processor/src/test
git commit -m "$(cat <<'EOF'
test(processor): migrate 54 wrapCfxTemplate arg-triples to inherited constants

DEF-Reuse-R3 — replace the 3 distinct (srcNode, desNode, app) literal triples
in 20 AbstractXsdValidationTest subclasses with inherited HNDEMP_NODE /
INSTITUTION_NODE / APP_FEPX / APP_HNDEMP. Exact-string triple match cannot
touch single-literal comment / setter / assert / text-block occurrences.
Zero behavior change (constant value === literal, XML byte-identical).

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

### Task 3: closing — 全回归 + push + PR `模式 A`

- [ ] **Step 1: fep-processor 全套回归（红线 `feedback_full_regression_before_commit`）**

```bash
cd /Users/muzhou/FEP_v1.0_wt-r3-xsd-constants
JAVA_HOME=/usr/local/opt/openjdk@17 PATH=/usr/local/opt/openjdk@17/bin:$PATH \
  ./mvnw test -pl fep-processor > ./t3-full.log 2>&1; echo "exit=$?" >> ./t3-full.log
grep -E 'Tests run:|BUILD SUCCESS|BUILD FAILURE|Failures: [1-9]|Errors: [1-9]|exit=' ./t3-full.log | tail -8
```
期望: `BUILD SUCCESS`，全 GREEN（行为等同硬证）。
> ⚠️ 已知 flake（红线 `feedback_macos_apfs_fork_classloader_race`）: `SyncMessageProcessorServiceIntegrationTest.tenValidSamples_shouldReachCompleted` perf 断言（10 samples within 3s）macOS 重负载偶发 fail，与本迁移无关（XML 字节相同）。如 fail 单独重跑 + GHA Linux CI 兜底。

- [ ] **Step 2: push + PR**

```bash
cd /Users/muzhou/FEP_v1.0_wt-r3-xsd-constants
git push -u origin chore/r3-abstractxsdtest-node-constants
gh pr create --base main --head chore/r3-abstractxsdtest-node-constants \
  --title "test(processor): DEF-Reuse-R3 — AbstractXsdValidationTest node/app constants + arg-triple migration" \
  --body "Simplify Q drain DEF-Reuse-R3 (v0.2 收窄安全子集): 定义 4 节点/App public 常量 + 迁移 20 subclass 文件 54 行 wrapCfxTemplate 参数三元组。注释/setter/assert/文本块不动（三元组结构自消歧）。test-only 零行为变更（常量值===字面量）。fep-processor 全回归 GREEN。Round 1 评审 3 BLOCKER 经收窄 scope 规避。🤖 Generated with Claude Code"
```

- [ ] **Step 3: worktree teardown（merge 后登记）**

```bash
# PR merge 后: cd /Users/muzhou/FEP_v1.0 && git worktree remove /Users/muzhou/FEP_v1.0_wt-r3-xsd-constants
```

- [ ] **Step 4: 触发 /session-end**

---

## §4 全 Plan grep 实测命令清单（红线 `feedback_plan_must_grep_actual_api` / `feedback_plan_revision_must_grep_actual_api`）

v0.2 修订阶段已实测（2026-06-01 `2cb0a15`）:
```bash
# 1. 三元组精确数 54 / 20 文件 / 3 形式 (24+20+10) — 见 §0 ✅
# 2. 0 跨行 split 参数 + 0 standalone app-arg → 三元组即全部 genuine args ✅
# 3. 20 文件全 extends AbstractXsdValidationTest → 无限定引用，0 import ✅
# 4. 11 R-2 注释 + setter/assert + 文本块 均单字面量，三元组不匹配 → 零误伤 ✅
# 5. 4 常量值逐字（FEPx 小写 x）✅
# 6. 0 别会话 worktree 触碰 validation 测试 ✅
```
执行阶段（T0 前）重测: `git fetch origin main && git log -1 --oneline origin/main` + `git worktree list`。

---

## §5 7 项 Plan 评审清单

| # | 检查项 | 自检 |
|:-:|--------|------|
| 1 | PRD 覆盖 | meta-Plan 无新 FR；§0 追溯 + muzhou 收窄 scope 批准 ✅ |
| 2 | 安全边界 | 纯测试，无 ⛔ 模式 E ✅ |
| 3 | 无占位符 | 3 exact-string 替换规则 + 命令完整；文件列表实施时实测（20 已确认）✅ |
| 4 | 类型一致性 | 4 常量值逐字相等（FEPx 小写 x）✅ |
| 5 | 测试命令可执行 | `-pl fep-processor` + `failIfNoSpecifiedTests=false` + JAVA_HOME 前缀 ✅ |
| 6 | 数据点自洽 | 54 = 24+20+10；20 文件全 extends ✅ |
| 7 | Worktree 触发 | 第 2 项 + 多会话 → 独立 worktree + teardown ✅ |

---

## §6 红线触发矩阵

| 红线 | 关联点 |
|------|--------|
| `feedback_batch_scope_creep` | scope 收窄已 muzhou 批准（Round 1 后） |
| `feedback_plan_revision_must_grep_actual_api` | v0.2 修订重 grep 实测三元组精确数 + extend 状态 |
| `feedback_worktree_isolates_fs_not_logic_domain` / `_trigger_is_dynamic_recheck_at_execution` | 多会话 → 独立 worktree + T0 重测 |
| `feedback_baseline_drift_during_long_review_cycle` | baseline `2cb0a15` 各时点重测 |
| `feedback_full_regression_before_commit` | T3 fep-processor 全回归 |
| `feedback_macos_apfs_fork_classloader_race` | T3 SyncMessageProcessor perf flake 兜底 |
| `feedback_surefire3_failifno_specified_tests_param_rename` / `feedback_bg_bash_path_inheritance` / `feedback_pipe_tail_deadlock_with_bg_bash` | mvn 命令规范 |

---

## §7 待 reviewer / muzhou 决策项

1. **scope 收窄共识**: v0.2 仅迁移 54 行 wrapCfxTemplate 参数三元组（最干净最安全子集）。注释/setter/assert/文本块/低频节点保留。如 muzhou 希望后续单独 drain setter/assert（CONCERN-1），可成立 follow-up（不建议改 isEqualTo）。
2. **APP_FEPX 命名**: 常量名 `APP_FEPX` 值 `"FEPx"`（小写 x），ticket 既定，保留。

---

## 评审记录

**Round 1（general-purpose, agentId `a80a77295675f87e5`, 2026-06-01）: NEEDS REVISION**
- ❌ BLOCKER-1: 盲 sed `s/"A1000143000104"/HNDEMP_NODE/g` 会误伤 11 个文件的 R-2 javadoc 注释（注释含带引号字面量）—— 主对话 grep 实证属实。
- ❌ BLOCKER-2: 7 NO-EXTEND 文件多为 setApp/setDesNodeCode/isEqualTo/注释，非 wrapCfxTemplate；AsyncPipeline 仅注释命中 —— 主对话 grep 实证属实。
- ❌ BLOCKER-3: 残留核对会因注释 hit 消失假绿。
- 🟡 CONCERN-1/2/3: setter/assert scope 蔓延 + "~84"不可复现 + import order。
- **v0.2 修订**: muzhou 选「收窄到安全子集」→ scope 改为仅 54 行三元组（结构自消歧，零误伤）/ 20 文件全 subclass（0 import）/ 残留核对改三元组计数 + 注释/setter/文本块保留断言。全部 BLOCKER + CONCERN 经收窄 scope 消除。

**Round 2（general-purpose, agentId `aa48bd88bd0ba5df1`, 2026-06-01）: PASS**
- ✅ Round 1 三 BLOCKER 经 v0.2 收窄**全部消除**（实测验证）: ① 三元组 exact-string 含两 node 字面量，注释/setter/assert 均单字面量结构上不匹配→零误伤；② 20 文件全 extends（0 import）+ 7 NO-EXTEND 文件 triples=0 自然排除；③ 残留核对改 4 独立断言（残留三元组 54→0 / R-2 注释=11 / 文本块=108 / setter=12）非平凡。
- ✅ 7 清单全过 + 三元组 54=24+20+10 实测可复现 + 0 跨行 split + 编译/类型正确（String 位置实参）。
- 🟡 CONCERN-A（gate 期望值 >0→精确 108/12）+ CONCERN-B（§0 补 DzpzInfo3000 占位 srcNode 排除注记）—— **已 boil-lake 应用**（纯文档收紧，reviewer 明示无需 v0.3 重评）。

## 批准签字

**Plan Approver: muzhou — ✅ APPROVED（2026-06-02，via AskUserQuestion）**
- 决策: 批准 + 立即实施（subagent review 逐 Task，独立 worktree `wt-r3-xsd-constants`）
- 依据: Round 1 NEEDS REVISION（3 BLOCKER）→ muzhou 选收窄安全子集 → v0.2 → Round 2 PASS（3 BLOCKER 全消除 + 7 清单全过）+ 2 CONCERN 已 boil-lake
- 执行: T1（4 常量）→ T2（54 三元组迁移）→ T3（closing），每 Task 独立 spec+quality review（红线 `feedback_task_review_discipline`）
