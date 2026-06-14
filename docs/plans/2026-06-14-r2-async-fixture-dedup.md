# FEP R2 REUSE Drain — AsyncPipelineIntegrationTest XML Fixture 去重 实施计划

> **执行方式:** superpowers:executing-plans 内联执行（单 Task test-only refactor）。步骤使用 `- [ ]` 复选框跟踪。

**目标:** 抽取 `AsyncPipelineIntegrationTest`（fep-processor）文件内逐字重复的 3001 请求 / 3002 响应 XML fixture 为两个 `private static byte[]` helper，去重约 -70 LOC；纯行为保持 refactor，现有测试为安全网。

**前置依赖:** 无（test-only，不依赖其他 Plan）。

**执行 Worktree:** `/Users/muzhou/FEP_v1.0_wt-r2-async-fixture`（分支 `refactor/r2-async-fixture-dedup`，触发条件第 ② 项 — 与别会话已签字/在途 Plan 并存：本会话起步实测 3 个活跃 worktree `wt-gm-s2b` / `wt-rule-phase3` / `wt-simplify-q-drain`，红线 `feedback_worktree_isolates_fs_not_logic_domain` 要求多会话活跃时会话起始即建独立 worktree）。已基于 `origin/main` `6895adfb`（= PR #89 "EFF-S5-1 Simplify drain" head）建立，规避本地 main `d7fc8199` 的 stale 落后（红线 `feedback_stale_local_main_worktree` — 派 review/实施于 clean origin/main worktree，本地 main 不主动 ff 由 session-end 处理）。

**架构:** 两个新增 `private static byte[]` helper（`request3001()` / `response3002()`）封装当前在 5 处（3001 请求）/ 3 处（3002 响应）逐字复制的完整 CFX envelope fixture；各调用点替换为单行 helper 调用。helper 内部产物与原字面量**逐字节相同**，故 XSD 校验与所有断言行为不变。

**技术栈:** Java 17 / JUnit 5 / AssertJ / Maven（fep-processor 单模块）。

**AI 协同模式:**

| 模式 | AI 占比 | 本计划中的应用 |
|:----:|:-------:|--------------|
| A | 90% | 测试 fixture 重构（test-only，无生产代码、无业务逻辑变更） |

**本 Plan 仅 1 个 Task，模式 A。**

---

## 设计背景

### 来源
2026-06-11 `rule-pipeline-batch-async` 会话 Simplify 三审产出的 deferred pool 项 **REUSE-R2**（"async IT XML fixture helper ~-40 LOC"）。R3（`ValidationResult.firstError()` 统一）已随 PR #86 drain；R2 由 muzhou 2026-06-13 拍板"留下会话"。本会话（2026-06-14）muzhou 选定仅执行此项（同批候选 #1 候选红线实测已落盘、#3 DEF-B2-1 实测为 premature 继续 defer）。

### 重复实测（baseline `6895adfb`，grep 实测）
| fixture | 出现次数 | 调用点（行号，baseline 文件 487 行） |
|---|---|---|
| 3001 `<ProgressQuery3001>` 完整请求 envelope | **5** | `asyncFlow_3001_3002`(97-110) / `performanceBaseline`(304-317) / `asyncRequest_businessRuleViolation`(355-368) / `asyncResponse_businessRuleViolation`(383-396) / `asyncFlow_businessRulesPass`(429-442) |
| 3002 `<ProgressQueryReturn3002>` 完整响应 envelope | **3** | `asyncFlow_3001_3002`(118-132) / `asyncResponse_businessRuleViolation`(397-411) / `asyncFlow_businessRulesPass`(443-457) |

5 处 3001 块逐字相同（同 `SERIAL_NO` / `KEY001` / `QueryType=1`）；3 处 3002 块逐字相同（含 `<ReturnCode>01</ReturnCode>`）。已用 `git diff` 确认 worktree 内文件与 baseline 一致。

### 不在范围（Rule-of-Three 保留 inline）
- 3003/3004、3005/3006 请求/响应各仅出现 **1 次** → 不抽取（单次使用抽取无去重收益，徒增间接层）。
- `asyncFlow_xsdFailure` / `AsyncMessageProcessorServiceTest` 的平凡 `"<CFX><HEAD>...</HEAD></CFX>"` invalid-XML（行 258 / 90 / 144）→ 非同源 fixture，不动。
- 既有 `HEAD_TEMPLATE` / `REQUEST_HEAD` / `RESPONSE_HEAD` / `SERIAL_NO` 常量 + `cfx()` / `toBytes()` helper → 已是复用基元，保留，新 helper 复用之。
- 行 42-47 误置的 R-2 类级 Javadoc（A1000143000104 字面量说明）→ 预存历史，与本次去重无关，逐字不动。

### 核心类职责边界
不涉及 ≥3 依赖的 Service（test-only）。无新增生产类、无 ArchUnit/依赖方向影响。

### 共享工具类清单
新 helper 为**文件内** `private static`，非跨类共享工具，不入 `common.util`（仅本测试类 8 处调用，无第二消费类——`AsyncMessageProcessorServiceTest` 实测不复用该 fixture）。

---

## Task 1: AsyncPipelineIntegrationTest fixture 去重 `模式 A`

**PRD 依据:** 不适用（test-only 质量重构，无新功能需求；Simplify deferred pool drain，PRD 追溯豁免，与基础设施/元流程 Plan 同类）。
**追溯 ID:** 不适用（无 FR-ID；本 Task 不新增/修改任何 PRD 功能行为）。

**验收标准（行为保持 refactor — 安全网为现有测试）:**
1. 重构后 `AsyncPipelineIntegrationTest` 全部 8 个 `@Test` 方法断言不变、全绿（含 `@DisabledOnOs(MAC)` 的 perf 测试在 macOS 跳过、Linux/CI 跑）。
2. 新 helper `request3001()` 产物与原 5 处 3001 字面量 envelope **逐字节相同**；`response3002()` 与原 3 处 3002 字面量 **逐字节相同**（保证 XSD 校验路径与业务规则 gate 行为不变）。
3. 3001 请求 `<ProgressQuery3001>` 文件内出现次数由 5 → 1（仅 helper 内）；3002 `<ProgressQueryReturn3002>` 由 3 → 1。
4. 净行数减少 ≥ 40 LOC（预期约 -70）。
5. fep-processor 模块 `spotbugs:check` 无新增 finding；无新增生产代码故 ArchUnit 不受影响（test 类不在 ArchUnit production scope）。

> **规则**: 本 Task 不改任何断言值、不改任何 fixture 内容字节。helper 仅做"原地字面量 → 方法封装"的机械抽取。若发现任一调用点 fixture 与其它**不逐字相同**，则该点不纳入 helper（保留 inline），并在 commit message 披露。

**Files:**
- Modify: `fep-processor/src/test/java/com/puchain/fep/processor/pipeline/AsyncPipelineIntegrationTest.java`

- [ ] **Step 1: 重构前基线 — 跑测试确认全绿（安全网起点）**

```bash
export JAVA_HOME=/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home; export PATH="$JAVA_HOME/bin:$PATH"
cd /Users/muzhou/FEP_v1.0_wt-r2-async-fixture
./mvnw -o -pl fep-processor test -Dtest=AsyncPipelineIntegrationTest \
  -Dsurefire.failIfNoSpecifiedTests=false --batch-mode --no-transfer-progress \
  > /tmp/r2-before.log 2>&1
```
期望（单独 `grep -E "Tests run|BUILD" /tmp/r2-before.log` 读 log 确认，禁链式）: `Tests run: 8, Failures: 0, Errors: 0, Skipped: 1`（macOS perf 测试 `@DisabledOnOs(MAC)` 计入 Skipped:1）、`BUILD SUCCESS`。记录此基线数字，Step 5 须与之逐字一致。
> 注：`-o` 离线（红线 `feedback_single_module_regression_no_am_flag` 不带 `-am`）；上游 SNAPSHOT 缺则先一次性 `./mvnw -o -pl fep-processor -am install -DskipTests` 装 jar 再单模块跑。
> 注：本机 load 实测 `uptime`，>100 杀**本 worktree-slug 精确匹配**的孤儿 fork，等 load<30 续（红线 `feedback_macos_apfs_fork_classloader_race` / `feedback_surefire_fork_topology`）。

- [ ] **Step 2: 在 `// ── helpers ──` 段（`cfx` 方法之前）插入两个 fixture helper**

```java
    /**
     * Canonical 3001 ProgressQuery request envelope (used by 5 test methods).
     * Byte-identical to the previously-inlined literal; extracted to remove
     * 4 duplicate copies (REUSE-R2 Simplify drain).
     */
    private static byte[] request3001() {
        return toBytes(cfx("3001", """
                <RealHead3001>
            """ + REQUEST_HEAD + """
                </RealHead3001>
                <ProgressQuery3001>
                    <SerialNo>""" + SERIAL_NO + """
            </SerialNo>
                    <SendNodeCode>12345678901234</SendNodeCode>
                    <DesNodeCode>A1000143000104</DesNodeCode>
                    <hxqyName>\u6D4B\u8BD5\u6838\u5FC3\u4F01\u4E1A</hxqyName>
                    <hxqyCode>123456789012345678</hxqyCode>
                    <QueryType>1</QueryType>
                    <QueryKey>KEY001</QueryKey>
                </ProgressQuery3001>"""));
    }

    /**
     * Canonical 3002 ProgressQueryReturn response envelope (used by 3 test methods).
     * Byte-identical to the previously-inlined literal; extracted to remove
     * 2 duplicate copies (REUSE-R2 Simplify drain).
     */
    private static byte[] response3002() {
        return toBytes(cfx("3002", """
                <RealHead3002>
            """ + RESPONSE_HEAD + """
                </RealHead3002>
                <ProgressQueryReturn3002>
                    <SerialNo>""" + SERIAL_NO + """
            </SerialNo>
                    <SendNodeCode>12345678901234</SendNodeCode>
                    <DesNodeCode>A1000143000104</DesNodeCode>
                    <hxqyName>\u6D4B\u8BD5\u6838\u5FC3\u4F01\u4E1A</hxqyName>
                    <hxqyCode>123456789012345678</hxqyCode>
                    <QueryType>1</QueryType>
                    <QueryKey>KEY001</QueryKey>
                    <ReturnCode>01</ReturnCode>
                </ProgressQueryReturn3002>"""));
    }
```

> helper 内容**逐字符复制**自 `asyncFlow_3001_3002` 的原 3001/3002 块（baseline 行 97-110 / 118-132）。`hxqyName` 保留 baseline 源码的 `\u6D4B\u8BD5\u6838\u5FC3\u4F01\u4E1A` unicode 转义写法（即中文「测试核心企业」六字的 \uXXXX 形式），**不改用 raw CJK 字面**，确保 helper 源码字符序列与 baseline 完全一致、产物字节一致。santa Round1 MAJOR-1 修订：消除"等价但源码形态不同"的认知负担。

- [ ] **Step 3: 替换 5 处 3001 请求调用点为 `request3001()`**

各方法内 `byte[] requestXml = toBytes(cfx("3001", """ ... """));`（及 `performanceBaseline` 的 `byte[] validXml = toBytes(cfx("3001", """ ... """));`）整体替换为单行：

- `asyncFlow_3001_3002_shouldCompleteSuccessfully`（行 97-110）→ `byte[] requestXml = request3001();`
- `performanceBaseline_100AsyncInbound_shouldHaveP95LessThan15ms`（行 304-317）→ `byte[] validXml = request3001();`
- `asyncRequest_businessRuleViolation_shouldFailWithProc8507`（行 355-368）→ `byte[] requestXml = request3001();`
- `asyncResponse_businessRuleViolation_shouldFailWithProc8507`（行 383-396）→ `byte[] requestXml = request3001();`
- `asyncFlow_businessRulesPass_shouldCompleteAsBeforehand`（行 429-442）→ `byte[] requestXml = request3001();`

- [ ] **Step 4: 替换 3 处 3002 响应调用点为 `response3002()`**

- `asyncFlow_3001_3002_shouldCompleteSuccessfully`（行 118-132）→ `byte[] responseXml = response3002();`
- `asyncResponse_businessRuleViolation_shouldFailWithProc8507`（行 397-411）→ `byte[] responseXml = response3002();`
- `asyncFlow_businessRulesPass_shouldCompleteAsBeforehand`（行 443-457）→ `byte[] responseXml = response3002();`

> 3003/3004、3005/3006 调用点（`asyncFlow_3003_3004` / `asyncFlow_3005_3006`）**不动**。

- [ ] **Step 5: 重构后跑测试确认全绿（安全网终点）+ 去重计数核验**

```bash
export JAVA_HOME=/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home; export PATH="$JAVA_HOME/bin:$PATH"
cd /Users/muzhou/FEP_v1.0_wt-r2-async-fixture
./mvnw -o -pl fep-processor test -Dtest=AsyncPipelineIntegrationTest \
  -Dsurefire.failIfNoSpecifiedTests=false --batch-mode --no-transfer-progress \
  > /tmp/r2-after.log 2>&1
```
单独 `grep -E "Tests run|BUILD" /tmp/r2-after.log` 确认与 Step 1 同样 `BUILD SUCCESS` + 同测试数 0 fail。再核验去重计数：
```bash
grep -c "<ProgressQuery3001>" fep-processor/src/test/java/com/puchain/fep/processor/pipeline/AsyncPipelineIntegrationTest.java   # 期望 1
grep -c "<ProgressQueryReturn3002>" fep-processor/src/test/java/com/puchain/fep/processor/pipeline/AsyncPipelineIntegrationTest.java  # 期望 1
git diff --stat fep-processor/src/test/java/com/puchain/fep/processor/pipeline/AsyncPipelineIntegrationTest.java  # 期望净减 ≥40 行
```

- [ ] **Step 6: spotbugs 核验（test 类不触生产 finding，但确认模块无回退）**

```bash
export JAVA_HOME=/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home; export PATH="$JAVA_HOME/bin:$PATH"
cd /Users/muzhou/FEP_v1.0_wt-r2-async-fixture
./mvnw -o -pl fep-processor spotbugs:check --batch-mode --no-transfer-progress > /tmp/r2-spotbugs.log 2>&1
```
单独 `grep -E "BUILD|BugInstance" /tmp/r2-spotbugs.log` 确认 `BUILD SUCCESS`（spotbugs 只扫 main，test 重构不引入 finding；此步为防回退兜底）。
> 注：spotbugs:check 不重编译，若改了注解需先 compile——本 Task 无注解变更，直接 check 即可（红线 `feedback_spotbugs_check_needs_recompile_after_annotation` 本例不触发）。

- [ ] **Step 7: 提交（验证与 commit 分离，禁链式 — 红线 `feedback_commit_no_chain_with_verify_command`）**

先**单独**读 Step 5/6 log 明述已确认 GREEN，再**单独**执行 commit：
```bash
cd /Users/muzhou/FEP_v1.0_wt-r2-async-fixture
git add fep-processor/src/test/java/com/puchain/fep/processor/pipeline/AsyncPipelineIntegrationTest.java docs/plans/2026-06-14-r2-async-fixture-dedup.md
git commit -m "$(cat <<'EOF'
refactor(processor): dedup AsyncPipelineIntegrationTest 3001/3002 fixtures (REUSE-R2)

Extract byte-identical 3001 request (×5) and 3002 response (×3) CFX
envelope fixtures into private static request3001()/response3002()
helpers. Behaviour-preserving: helper output is byte-identical to the
inlined literals, so XSD validation and all assertions are unchanged.
3003/3004 + 3005/3006 fixtures (single-use) left inline per Rule-of-Three.

Simplify deferred pool drain REUSE-R2 (2026-06-11 rule-pipeline-batch-async).
test-only, single module fep-processor.

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

## 回归验收（两层 — 红线 `feedback_plan_regression_scope_explicit`）

- **minimum（本地）:** `./mvnw -o -pl fep-processor test -Dtest=AsyncPipelineIntegrationTest` 全绿（macOS perf skip）+ 去重计数 1/1 + 净减 ≥40 行 + `spotbugs:check` BUILD SUCCESS。
- **strong（CI）:** PR 触发 GHA Build/Test/Quality 全 fep-processor 模块绿（含 perf 测试在 Ubuntu 跑 P95<15ms）+ PR-Size（test-only diff，预期净负行数，无超限风险）。

> 全量 reactor verify 不在本机跑（红线 `feedback_single_module_no_am_flag` + 多会话 4 核 load 约束）；委托 GHA。

---

## Worktree 触发条件自检（红线 `feedback_worktree_for_parallel_work`）

- [ ] 跨 ≥3 模块？ **否**（单模块 fep-processor）
- [x] 与已签字未执行的 Plan 并存？ **是** — 本会话起步实测 3 个活跃 worktree（`wt-gm-s2b` S2b 报文签验 / `wt-rule-phase3` 规则引擎 phase3 / `wt-simplify-q-drain`），其中 S2b/phase3 携带各自在途分支；红线 `feedback_worktree_isolates_fs_not_logic_domain` 要求多会话活跃时会话起始即建独立 worktree（不以"无文件交集"豁免）。
- [ ] ⛔ 安全 vs AI 并行？ **否**
- [ ] TLQ tongtech 联调？ **否**
- [ ] ≥5min long-running verify 并行？ **否**（单测 <5min）
- [ ] muzhou WIP 并存？ **否**

命中第 ② 项 → 已建 `/Users/muzhou/FEP_v1.0_wt-r2-async-fixture`（分支 `refactor/r2-async-fixture-dedup`，基于 origin/main `6895adfb`）。

## 闭环 Task（session-end）

- [ ] PR 创建 + GHA 绿 + muzhou merge（或依红线 `feedback_systemic_ci_blocker_defers_positive_backing` 处置 CI 阻塞）。
- [ ] `git worktree remove /Users/muzhou/FEP_v1.0_wt-r2-async-fixture` + 分支删 + 本地 main ff。
- [ ] session-end 四步收尾（Simplify 三审 / 9 维技术文档 / Daily Report / push）。

---

## 自检清单核对

| # | 检查项 | 状态 |
|:-:|---|:-:|
| 1 | PRD 覆盖度 | 豁免（test-only 质量重构，无 FR） |
| 2 | 安全边界（SM2/3/4/密钥/脱敏） | 不涉及 |
| 3 | 占位符扫描（TBD/TODO/类似） | 无 |
| 4 | 类型一致性（helper 名/签名） | `request3001()`/`response3002()` 返回 `byte[]`，与调用点 `byte[] x = ...` 一致 |
| 5 | 测试命令可执行（`-Dtest=` 匹配类名） | `AsyncPipelineIntegrationTest` 实存 |
| 6 | CLAUDE.md 更新 | session-end Phase 2 统一更新当前状态段 |
| 7 | 验收标准来自需求（非代码推测） | 行为保持 refactor，安全网=现有测试断言 |
| 8 | 共享工具类无遗漏 | helper 文件内 private static（无第二消费类，不入 common.util） |
| 9 | 核心类职责边界 | 不涉及 Service |
| 10 | Worktree 触发自检 | 命中 ②，已建 worktree |

---

## 评审与签字

### AI 独立评审（santa-method）
- **Round 1: REVISE** — MAJOR-1（helper `hxqyName` 用 raw CJK 而 baseline 用 `\u` 转义，运行时等价但 Plan 自称"逐字复制"失真）+ MINOR-1（Step1 期望含糊）+ MINOR-2（line9 stale 措辞）。字节一致性实测：5×3001 内容 md5 全等、3×3002 全等，抽取本身安全。
- **修订**：helper 字面量改回 `测...` 转义（真逐字符复制 baseline）；Step1 期望明确为 `Tests run: 8, Failures: 0, Errors: 0, Skipped: 1`；line9 worktree 论证澄清；line131 prose 订正。
- **Round 2: PASS** — 确认 MAJOR-1 代码修订正确（helper 与 baseline 逐字一致）、MINOR-1/2 修复；line131 prose 残留同步修正。

### muzhou 批准签字
**✅ APPROVED — muzhou 2026-06-14**（santa Round2 PASS 后签字，批准内联实施）。
Reviewed-By: muzhou
