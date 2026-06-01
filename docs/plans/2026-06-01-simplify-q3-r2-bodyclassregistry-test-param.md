# FEP Simplify Q Drain — DEF-Q-NEW-3 + DEF-Reuse-R2 实施计划

> **执行方式:** 使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐 Task 执行。步骤使用 `- [ ]` 复选框跟踪。

**目标:** 消化 P4-MSG-I Simplify deferred pool 2 项 LOW-MED 项 —— ① DEF-Q-NEW-3: `BodyClassRegistryTest` 31 个单映射样板 `@Test` 合并为 1 个 `@ParameterizedTest` + 统一 `MethodSource` 矩阵（沿用已有 `supplychainQueryWireMatrix` 范式，Rule-of-3 已满足）；② DEF-Reuse-R2: `BodyClassRegistry` 头部 Javadoc 硬编码 "37 entries" 改为 `{@link OutboundWireShapeDispatcher#REGISTERED_MSG_NO_COUNT}` 跨模块引用，消除双数字硬编码漂移风险。**纯测试重构 + Javadoc，零生产逻辑变更、零行为变更。**

**前置依赖:** 无（P4-MSG-A 至 P4-MSG-I 全部已 ship；BodyClassRegistry 37 entries 已稳定在 origin/main `f89fe38`）。

**baseline:** origin/main HEAD `d8322a3`（feat(web) Callback Phase 2 #27 — 2026-06-01 重测）。
> baseline drift 修正（红线 `feedback_baseline_drift_during_long_review_cycle`）: 起草时记 `f89fe38`，评审时 origin/main 已推进至 `d8322a3`（PR #27/#28/#29/#32 全 merge，GHA billing 已解决）。`f89fe38` 仍是 `d8322a3` 祖先，且两目标文件 `BodyClassRegistryTest`/`BodyClassRegistry` 实测**未被这 4 commit 改动**（`git diff --stat f89fe38 d8322a3 -- <两文件>` 为空），Plan 内容不受影响。worktree 须从 `d8322a3` 切分支。

**执行 Worktree:** `/Users/muzhou/FEP_v1.0_wt-simplify-q3-r2`（分支 `chore/simplify-q3-r2-bodyclassregistry-test`，触发条件第 2 项 + 红线 `feedback_worktree_isolates_fs_not_logic_domain` 多会话活跃扩展）
> 红线 `feedback_worktree_for_parallel_work` 触发判定: ① 跨 ≥3 模块 refactor ❌（仅 fep-web 单模块，@link 引用 fep-converter 但不改）② **与已签字未执行 Plan 并存 ✅**（Callback Phase 2b Plan v0.3 已签字 `8613568` 实施中；2026-06-01 实测 7 个别会话 worktree 活跃: wt-callback-p2/p2b/p2b-sec/v04opt + wt-p4-msg-l `18efce5` + wt-simplify-q-drain `1f651bf`(#29 已 merge 待 teardown)）③ ⛔ 安全 vs AI 并行 ❌ ④ TLQ tongtech 联调 ❌ ⑤ >5min long-running verify 并行 ❌ ⑥ muzhou WIP 并存 ❌
> 红线 `feedback_worktree_isolates_fs_not_logic_domain`: 3 个别会话同仓库活跃，git index 工作树级共享 → 即使文件级无交集仍须独立 worktree 防杂散 commit 误落他人分支。
> ⚠️ T0 实施前（红线 `feedback_worktree_trigger_is_dynamic_recheck_at_execution`）须 4 步重测一次确认并发态。

**架构:** 纯测试侧 + Javadoc 重构。Task 1 把 `BodyClassRegistryTest` 的 31 个单映射 `@Test`（含已有 `supplychainQueryWireMatrix` 覆盖的 6 个）统一为单个 `@ParameterizedTest shouldResolveRegisteredBody` + `registeredBodyWireMatrix()` MethodSource（37 行 Arguments，按 P4-MSG 批次分组注释保留 provenance）；保留 2 个 negative `@Test` + 1 个结构 growth-guard `@Test` 不动。Task 2 把 `BodyClassRegistry` 头部 Javadoc 的字面量 "37 entries" 改为 `{@link}` 引用 dispatcher 的 `REGISTERED_MSG_NO_COUNT` 常量（全限定名，无需新 import）。行为等同性由"重构前后同一组 37 映射全 GREEN + 结构测试 `countRegistryEntries()==37` 不变"硬证。

**技术栈:** Java 17 / Spring Boot 3.x / Maven / JUnit 5 (`@ParameterizedTest` + `@MethodSource`) / AssertJ。

**AI 协同模式:**

| 模式 | AI 占比 | 本计划中的应用 |
|:----:|:-------:|--------------|
| A | 90% | 测试重构（参数化）/ Javadoc 引用修订 —— 纯机械、零业务逻辑、零安全边界 |

本 Plan 全部 Task 为 **模式 A**（测试代码 + Javadoc，无生产业务逻辑、无安全禁入区域）。

---

## §0 PRD 追溯与范围声明

**Plan 性质:** Simplify deferred pool drain（质量重构 meta-Plan），按 writing-plans 原则 7 + CLAUDE.md「基础设施/元流程 Plan 除外」**不引入新 FR-ID**。

**追溯依据:** 本 Plan 不新增功能，仅重构既有已交付能力的测试与文档：
- `BodyClassRegistry` 37 entries 映射追溯 PRD v1.3 §4.6 上行/下行 msgNo↔Body POJO 映射（已由 P4-MSG-A 至 P4-MSG-I 全部交付，FR 已闭环）。
- deferred 来源: `docs/daily_reports/2026-05-19-p4-msg-i-progress-report.md` §Simplify 三审 deferred pool:
  - `DEF-Q-NEW-3 | LOW-MED | BodyClassRegistryTest 38 @Test → @ParameterizedTest 重构（消除模板代码）`
  - `DEF-Reuse-R2 | LOW | BodyClassRegistry Javadoc "37" 跨模块引用 dispatcher COUNT 常量（消除双数字硬编码漂移风险）`
- muzhou 2026-06-01 via AskUserQuestion 批准「DEF-Q-NEW-3 + DEF-Reuse-R2 打包」（含 scope 扩张到 R2 的显式批准，满足红线 `feedback_batch_scope_creep`）。

**不在本 Plan 范围（deferred pool 其余项，审计实测后判定）:**
- `EFF-D1`（HIGH）—— ❌ 已过时: `OutboundCommonForwardWireTest` 实测已无 `@MockBean XsdValidator`（R-NEW-1 真 validator 改造已消化），ticket 失效。
- `DEF-Q-NEW-1`（MED）—— ⚠️ 前提存疑: 目标文件 `OutboundCommonForwardWireTest` 实测 0 个 `@Test`（疑被重构/迁移），需独立调查后再定。
- `DEF-Reuse-R3`（LOW-MED）—— 🚫 与未 merge 的 PR #29 重叠（`AbstractXsdValidationTest` 基类的 4 子类正在 PR #29 改动中），推迟到 PR #29 merge 后。
- `DEF-Q-NEW-2`（MED context cache）/ `EFF-D4`（MED verify 命令 doc 化）—— 本轮不动。

---

## §1 文件结构

| 文件路径 | 职责 | 操作 | AI 模式 |
|----------|------|------|:-------:|
| `fep-web/src/test/java/com/puchain/fep/web/outbound/consumer/BodyClassRegistryTest.java` | 31 单映射 @Test → 1 @ParameterizedTest + 统一矩阵 | 修改 | A |
| `fep-web/src/main/java/com/puchain/fep/web/outbound/consumer/BodyClassRegistry.java` | 头部 Javadoc "37 entries" → @link dispatcher 常量 | 修改 | A |

**共享工具类清单:** 无新增（复用 JUnit 5 `@ParameterizedTest`/`@MethodSource`/`Arguments`/`Stream`，`BodyClassRegistryTest` 已 import）。

**核心类职责边界:** 无新增 Service（不涉及依赖 ≥3 的 Service 类）。

---

## §2 Task 拆解与依赖

| Task | 主题 | deferred ID | 文件 | 依赖 | 模式 |
|------|------|-------------|------|------|:----:|
| T1 | BodyClassRegistryTest 参数化 | DEF-Q-NEW-3 | BodyClassRegistryTest.java | 无 | A |
| T2 | BodyClassRegistry Javadoc @link | DEF-Reuse-R2 | BodyClassRegistry.java | 无 | A |
| T3 | closing（worktree teardown + 状态更新） | — | CLAUDE.md（file write only） | T1,T2 | A |

T1 与 T2 文件不交叉，可独立；建议 T1→T2→T3 串行 commit。

---

## §3 Task 详细步骤

### Task 1: BodyClassRegistryTest 31 单映射 @Test → 1 @ParameterizedTest `模式 A`

**deferred 依据:** `DEF-Q-NEW-3 | LOW-MED | BodyClassRegistryTest 38 @Test → @ParameterizedTest 重构（消除模板代码）`（实测当前 34 @Test + 1 @ParameterizedTest，单映射样板 31 个）

**验收标准（行为等同性，从既有测试覆盖推导，不从代码反推）:**
1. 重构后 `BodyClassRegistryTest` 对全部 37 个已登记 msgNo 的解析断言**完整保留**（37 = 31 原单映射 + 6 原 supplychainQueryWireMatrix），断言语义 `registry.resolve(msgNo) == ExpectedClass` 逐一不变。
2. 2 个 negative `@Test`（`resolve_invalid_msgNo_should_throw_5107` / `resolve_null_msgNo_should_throw_5107`）**原样保留**，断言不变。
3. 结构 growth-guard `@Test registry_shouldUseMapOfEntries_supportingUnboundedSize`（`countRegistryEntries()==37` + `Map.ofEntries(` + 不含 `= Map.of(`）**原样保留**，断言不变。
4. 重构后 `mvn test -Dtest=BodyClassRegistryTest` GREEN，**参数化执行计数 = 37**（matrix 行数），加 2 negative + 1 结构 = JUnit 报告 40 个 test invocation 全 PASS。
5. 矩阵 `registeredBodyWireMatrix()` 行数 = 37，与 `REGISTERED_MSG_NO_COUNT` / `countRegistryEntries()` 一致（自洽，红线 `feedback_plan_template_data_point_self_consistency`）。

**Files:**
- Modify: `fep-web/src/test/java/com/puchain/fep/web/outbound/consumer/BodyClassRegistryTest.java`

- [ ] **Step 1: 重构前确认绿灯基线（重构安全网）**

```bash
cd /Users/muzhou/FEP_v1.0_wt-simplify-q3-r2
JAVA_HOME=/usr/local/opt/openjdk@17 PATH=/usr/local/opt/openjdk@17/bin:$PATH \
  ./mvnw test -pl fep-web -am -Dtest=BodyClassRegistryTest -Dsurefire.failIfNoSpecifiedTests=false \
  > /tmp/q3-baseline.log 2>&1; tail -25 /tmp/q3-baseline.log
```
期望: `BUILD SUCCESS`，`Tests run: 40`（31 单映射 @Test + 2 negative @Test + 1 结构 @Test + supplychainQueryWireMatrix 参数化展开 6 invocation = 31+2+1+6 = 40）。
> ⚠️ 行为等同性硬证（红线 `feedback_doc_data_grep_first`）: baseline 与重构后 Tests run **必须相等 = 40**（重构只把 31 单映射 + 6 旧参数化 = 37 invocation 重新分组为 1 个参数化展开 37 invocation，总 invocation 数不变 37 + 2 negative + 1 结构 = 40）。Step 1 实跑后以 `/tmp/q3-baseline.log` 实测数为准，Step 3 实测须等于该数。

- [ ] **Step 2: 替换单映射 @Test 为统一参数化矩阵**

将 `BodyClassRegistryTest.java` 中 **第 74-282 行**（从 `resolve_1101_should_return_DataTransfer1101` 到 `supplychainQueryWireMatrix()` 静态方法结束）整体替换为以下单个参数化测试 + 统一矩阵。**保留** 第 284 行起的 2 个 negative `@Test` + 结构 `@Test` + `countRegistryEntries()` helper 不动。

```java
    @ParameterizedTest(name = "[{index}] msgNo={0} → {1}")
    @MethodSource("registeredBodyWireMatrix")
    @DisplayName("全部已登记上行报文 msgNo → Body POJO Class 主映射解析")
    void shouldResolveRegisteredBody(final String msgNo, final Class<?> expected) {
        assertThat(registry.resolve(msgNo))
                .as("BodyClassRegistry.resolve(\"%s\") 必须返回 %s", msgNo, expected.getSimpleName())
                .isEqualTo(expected);
    }

    /**
     * 全部已登记上行报文 msgNo → Body POJO Class 主映射矩阵（单一真相源）。
     *
     * <p>行数必须等于 {@link com.puchain.fep.converter.wire.OutboundWireShapeDispatcher#REGISTERED_MSG_NO_COUNT}
     * 与 {@code BodyClassRegistry.REGISTRY} entry 数（结构 growth-guard
     * {@code registry_shouldUseMapOfEntries_supportingUnboundedSize} 独立断言）。
     * 每次 append 新报文时同步在此追加一行。分组注释对齐 {@code BodyClassRegistry} 注册批次。</p>
     *
     * @return (msgNo, 期望 Body POJO Class) 参数流
     */
    static Stream<Arguments> registeredBodyWireMatrix() {
        return Stream.of(
                // P4-MSG-A T2 — 6 BATCH（注：1104 为 Transfer 非 Request）
                Arguments.of("1102", DataTransferCheckBatchRequest1102.class),
                Arguments.of("1103", CompanyInfoBatchRequest1103.class),
                Arguments.of("1104", CompanyAuthFileBatchTransfer1104.class),
                Arguments.of("2102", DataTransferCheckBatchResponse2102.class),
                Arguments.of("2103", CompanyInfoBatchResponse2103.class),
                Arguments.of("2104", CompanyAuthFileBatchResponse2104.class),
                // Plan B T4 — 3000
                Arguments.of("3000", DzpzInfo3000.class),
                // P4-MSG-B T1 — 3007
                Arguments.of("3007", InvoCheckQuery3007.class),
                // P4-MSG-D T3 — 1101
                Arguments.of("1101", DataTransfer1101.class),
                // P4-MSG-E T1 — 4 realtime
                Arguments.of("1001", CompanyInfoRequest1001.class),
                Arguments.of("2001", CompanyInfoResponse2001.class),
                Arguments.of("1004", CompanyAuthFileTransfer1004.class),
                Arguments.of("2004", CompanyAuthFileResponse2004.class),
                // P4-MSG-F T1 — 6 supplychain query（原 supplychainQueryWireMatrix）
                Arguments.of("3001", ProgressQuery3001.class),
                Arguments.of("3002", ProgressQueryReturn3002.class),
                Arguments.of("3003", PzInfoQuery3003.class),
                Arguments.of("3004", PzInfoReturn3004.class),
                Arguments.of("3005", QyAccQuery3005.class),
                Arguments.of("3006", QyAccQueryReturn3006.class),
                // P4-MSG-G T2 — 4 supplychain query batch2
                Arguments.of("3008", InvoCheckReturn3008.class),
                Arguments.of("3020", Forward3020.class),
                Arguments.of("3103", ArchiveReturnInfo3103.class),
                Arguments.of("3108", PzCheckQueryReturn3108.class),
                // P4-MSG-H — 2 supplychain batch3
                Arguments.of("3115", PlatPay3115.class),
                Arguments.of("3120", Forward3120.class),
                // P4-MSG-I T2 — 4 通用转发 + ack + 授信回执
                Arguments.of("9000", Forward9000.class),
                Arguments.of("9100", Forward9100.class),
                Arguments.of("9120", MsgReturn9120.class),
                Arguments.of("3113", HxqyCreditAmt3113.class),
                // 数仓 collector mapper（3009/3101/3102/3105/3107/3109/3112/3116）
                Arguments.of("3009", RzReturnInfo3009.class),
                Arguments.of("3101", ContractInfo3101.class),
                Arguments.of("3102", ArchiveInfo3102.class),
                Arguments.of("3105", RzApplyInfo3105.class),
                Arguments.of("3107", PzCheckQuery3107.class),
                Arguments.of("3109", QyRegister3109.class),
                Arguments.of("3112", HxqyCreditAmt3112.class),
                Arguments.of("3116", BankCheckDay3116.class));
    }
```

> **完整性核对（红线 `feedback_plan_template_data_point_self_consistency`）**: 上述矩阵共 **37** 行 Arguments = 6(MSG-A) + 1(3000) + 1(3007) + 1(1101) + 4(MSG-E) + 6(MSG-F) + 4(MSG-G) + 2(MSG-H) + 4(MSG-I) + 8(collector) = 37。与 `REGISTERED_MSG_NO_COUNT=37` / `countRegistryEntries()==37` 自洽。
> **删除项核对**: 原 31 单映射 @Test 的 msgNo 集 {1101,3000,3007,3008,3020,3103,3108,3115,3120,3009,3101,3102,3105,3107,3109,3112,3116,1102,1103,1104,2102,2103,2104,1001,2001,1004,2004,9000,9100,3113,9120} = 31 个 + 原 supplychainQueryWireMatrix 6 个 {3001-3006} = 37，与新矩阵逐一对应无遗漏。
> **保留 import 核对**: 替换后所有 37 个 Body POJO class import 仍被矩阵引用，无 unused import（checkstyle UnusedImports 不报）；`DisplayName`/`Test`/`ParameterizedTest`/`Arguments`/`MethodSource`/`Stream` 仍使用。

- [ ] **Step 3: 重构后确认绿灯 + 行为等同性**

```bash
cd /Users/muzhou/FEP_v1.0_wt-simplify-q3-r2
JAVA_HOME=/usr/local/opt/openjdk@17 PATH=/usr/local/opt/openjdk@17/bin:$PATH \
  ./mvnw test -pl fep-web -am -Dtest=BodyClassRegistryTest -Dsurefire.failIfNoSpecifiedTests=false \
  > /tmp/q3-after.log 2>&1; tail -25 /tmp/q3-after.log
```
期望: `BUILD SUCCESS`；`Tests run` = 37(参数化) + 2(negative) + 1(结构) = **40**，全 PASS。
> 行为等同性: 重构前覆盖的 37 映射 + 2 negative + 1 结构断言全部保留且 GREEN，无 msgNo 丢失（结构 growth-guard `countRegistryEntries()==37` 独立兜底）。

- [ ] **Step 4: checkstyle 自检**

```bash
cd /Users/muzhou/FEP_v1.0_wt-simplify-q3-r2
JAVA_HOME=/usr/local/opt/openjdk@17 PATH=/usr/local/opt/openjdk@17/bin:$PATH \
  ./mvnw checkstyle:check -pl fep-web > /tmp/q3-checkstyle.log 2>&1; tail -15 /tmp/q3-checkstyle.log
```
期望: `BUILD SUCCESS`，0 violations（参数化方法 Javadoc 完整 / 无 unused import / 行长 ≤ 限制）。

- [ ] **Step 5: 提交**

```bash
cd /Users/muzhou/FEP_v1.0_wt-simplify-q3-r2
git add fep-web/src/test/java/com/puchain/fep/web/outbound/consumer/BodyClassRegistryTest.java
git commit -m "$(cat <<'EOF'
test(web): consolidate 31 BodyClassRegistry resolve @Test into one @ParameterizedTest

DEF-Q-NEW-3 (P4-MSG-I Quality deferred pool drain) — merge 31 single-mapping
@Test methods (+ existing supplychainQueryWireMatrix 6) into one
shouldResolveRegisteredBody @ParameterizedTest backed by a unified 37-row
registeredBodyWireMatrix MethodSource. Negative cases + structural
growth-guard @Test unchanged. Zero behavior change (37 mappings preserved,
countRegistryEntries()==37 invariant intact).

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

### Task 2: BodyClassRegistry 头部 Javadoc "37 entries" → @link dispatcher 常量 `模式 A`

**deferred 依据:** `DEF-Reuse-R2 | LOW | BodyClassRegistry Javadoc "37" 跨模块引用 dispatcher COUNT 常量（消除双数字硬编码漂移风险）`

**验收标准:**
1. `BodyClassRegistry` 头部 Javadoc 中字面量 `37 entries` 改为 `{@link com.puchain.fep.converter.wire.OutboundWireShapeDispatcher#REGISTERED_MSG_NO_COUNT}` 引用（全限定名，**不新增 import**，避免 import-order churn）。
2. `@link` 目标可解析（fep-web pom 实测依赖 fep-converter，`OutboundWireShapeDispatcher` 为 `public`，`REGISTERED_MSG_NO_COUNT` 为 `public static final int`）。
3. 生产代码逻辑零变更（仅 Javadoc 注释行）；`countRegistryEntries()==37` 结构测试仍 GREEN（数量真相源仍是 REGISTRY 本身，Javadoc 仅文档引用）。
4. checkstyle 0 violations。

**Files:**
- Modify: `fep-web/src/main/java/com/puchain/fep/web/outbound/consumer/BodyClassRegistry.java`

- [ ] **Step 1: 修订头部 Javadoc 字面量**

定位头部 Javadoc 段（实测含 `37 entries / 后续 9XXX 通用报文（9005/9006/9007/9008/9009）独立 Plan 处理。`）。将该行的字面量 `37 entries` 替换为 `{@link}` 引用。Edit old→new:

old:
```
 * P4-MSG-I T2 注册 9000/9100/9120/3113 共 4 报文（实时通用转发 + 非实时通用转发 + 2101 模式6 ack + 核心企业授信回执）；
 * 37 entries / 后续 9XXX 通用报文（9005/9006/9007/9008/9009）独立 Plan 处理。
```
new:
```
 * P4-MSG-I T2 注册 9000/9100/9120/3113 共 4 报文（实时通用转发 + 非实时通用转发 + 2101 模式6 ack + 核心企业授信回执）；
 * 共 {@link com.puchain.fep.converter.wire.OutboundWireShapeDispatcher#REGISTERED_MSG_NO_COUNT} 份已登记上行报文（与 dispatcher 单一真相源一致，消除双数字硬编码漂移）；后续 9XXX 通用报文（9005/9006/9007/9008/9009）独立 Plan 处理。
```

> **设计说明**: 用 `{@link ...#REGISTERED_MSG_NO_COUNT}`（链接引用）而非 `{@value ...}`（值替换）—— `{@value}` 跨模块在 per-module javadoc 生成时可能不解析（需 doclet classpath 含 fep-converter），`{@link}` 仅做引用链接无此约束，且本项目 CI 门禁为 Checkstyle/SpotBugs/JaCoCo/ArchUnit（不跑 javadoc 生成严格模式），引用安全。字面量数字从 prose 移除，实际数量真相源由 `REGISTRY` entry 数 + 结构 growth-guard 测试 `countRegistryEntries()==37` 强制。

- [ ] **Step 2: 编译 + 结构测试确认无回归**

```bash
cd /Users/muzhou/FEP_v1.0_wt-simplify-q3-r2
JAVA_HOME=/usr/local/opt/openjdk@17 PATH=/usr/local/opt/openjdk@17/bin:$PATH \
  ./mvnw test -pl fep-web -am -Dtest=BodyClassRegistryTest -Dsurefire.failIfNoSpecifiedTests=false \
  > /tmp/r2-after.log 2>&1; tail -20 /tmp/r2-after.log
```
期望: `BUILD SUCCESS`，40 tests PASS（同 T1 Step 3，Javadoc 改动不影响运行时）。

- [ ] **Step 3: checkstyle 自检**

```bash
cd /Users/muzhou/FEP_v1.0_wt-simplify-q3-r2
JAVA_HOME=/usr/local/opt/openjdk@17 PATH=/usr/local/opt/openjdk@17/bin:$PATH \
  ./mvnw checkstyle:check -pl fep-web > /tmp/r2-checkstyle.log 2>&1; tail -12 /tmp/r2-checkstyle.log
```
期望: `BUILD SUCCESS`，0 violations。

- [ ] **Step 4: 提交**

```bash
cd /Users/muzhou/FEP_v1.0_wt-simplify-q3-r2
git add fep-web/src/main/java/com/puchain/fep/web/outbound/consumer/BodyClassRegistry.java
git commit -m "$(cat <<'EOF'
docs(web): link BodyClassRegistry javadoc count to dispatcher REGISTERED_MSG_NO_COUNT

DEF-Reuse-R2 (P4-MSG-I Reuse deferred pool drain) — replace hardcoded
"37 entries" literal in BodyClassRegistry header javadoc with
{@link OutboundWireShapeDispatcher#REGISTERED_MSG_NO_COUNT} cross-module
reference, eliminating the double-hardcode drift risk. Count truth source
stays the REGISTRY entries + countRegistryEntries() growth-guard test.

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

### Task 3: closing — worktree teardown + 项目状态更新 `模式 A`

**Files:**
- Modify: `/Users/muzhou/FEP_v1.0/CLAUDE.md`（git tracked，「当前项目状态」段；注：`/Users/muzhou/FEP/CLAUDE.md` 是另一份非 tracked 知识库，本 Task 只动 FEP_v1.0 仓库内的，**实际本项目 CLAUDE.md 状态段维护以 muzhou 习惯为准，若 FEP_v1.0 无 tracked CLAUDE.md 状态段则跳过仅做 session-end**）

- [ ] **Step 1: 全量回归（fep-web 模块）确认无连带回归**

```bash
cd /Users/muzhou/FEP_v1.0_wt-simplify-q3-r2
JAVA_HOME=/usr/local/opt/openjdk@17 PATH=/usr/local/opt/openjdk@17/bin:$PATH \
  ./mvnw test -pl fep-web -am > /tmp/q3-r2-fepweb-full.log 2>&1; tail -30 /tmp/q3-r2-fepweb-full.log
```
期望: `BUILD SUCCESS`（fep-web 全模块测试 GREEN；红线 `feedback_full_regression_before_commit` —— 本 Plan 仅改 1 test + 1 javadoc 不新增生产类，全 fep-web 回归足够，无须全 reactor）。
> ⚠️ 沙盒 mvn exit 144 兜底（红线 `feedback_mvn_sandbox_exit144_pattern`）: 若本机 mvn ≥2 次 SIGSYS 失败，跳本机实测 + 静态等价 review（diff 仅参数化重构 + 1 Javadoc 行，零运行时风险）+ GHA CI 远端兜底（待 billing 恢复）。

- [ ] **Step 2: push 分支 + 开 PR（GHA billing 阻塞下 tier-A 充分）**

```bash
cd /Users/muzhou/FEP_v1.0_wt-simplify-q3-r2
git push -u origin chore/simplify-q3-r2-bodyclassregistry-test
gh pr create --title "test(web): DEF-Q-NEW-3 + DEF-Reuse-R2 — parameterize BodyClassRegistryTest + link javadoc count" \
  --body "$(cat <<'EOF'
## Summary
Simplify Q drain（P4-MSG-I deferred pool）2 项 LOW-MED：
- **DEF-Q-NEW-3**: 31 个单映射 `@Test` → 1 个 `@ParameterizedTest` + 37 行统一 `registeredBodyWireMatrix`
- **DEF-Reuse-R2**: `BodyClassRegistry` javadoc 硬编码 "37" → `{@link OutboundWireShapeDispatcher#REGISTERED_MSG_NO_COUNT}`

test-only + javadoc，零行为变更（37 映射保留 + `countRegistryEntries()==37` 不变）。

## Test
- `BodyClassRegistryTest`: 40 invocation（37 参数化 + 2 negative + 1 结构）GREEN
- checkstyle 0 violations
- ⚠️ GHA Actions billing 阻塞中，tier-A 本机充分（红线 feedback_systemic_ci_blocker_defers_positive_backing），CI 绿光待 billing 恢复

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

- [ ] **Step 3: worktree teardown（merge 后由 session-end / 后续会话执行；本 Task 仅登记命令）**

```bash
# PR merge 后执行（非本会话强制，登记到 next-session-prompt）:
cd /Users/muzhou/FEP_v1.0
git worktree remove /Users/muzhou/FEP_v1.0_wt-simplify-q3-r2
git worktree list   # 确认 wt-simplify-q3-r2 已移除
```

- [ ] **Step 4: 触发 /session-end 6-phase 收尾**（四步收尾仅 session-end 时做，红线 `feedback_mandatory_post_task`）

---

## §4 全 Plan grep 实测命令清单（红线 `feedback_plan_must_grep_actual_api`）

起草阶段已实测（2026-06-01）:
```bash
# 1. dispatcher 常量存在性 + public + 值
grep -n 'public static final int REGISTERED_MSG_NO_COUNT' \
  fep-converter/src/main/java/com/puchain/fep/converter/wire/OutboundWireShapeDispatcher.java
# → line 123: public static final int REGISTERED_MSG_NO_COUNT = 37; ✅

# 2. fep-web 依赖 fep-converter（@link 可解析）
grep -n 'fep-converter' fep-web/pom.xml   # → line 37 ✅

# 3. BodyClassRegistryTest 当前结构
grep -c '@Test' BodyClassRegistryTest.java          # → 34
grep -c '@ParameterizedTest' BodyClassRegistryTest.java  # → 1
# 单映射 @Test 31 + negative 2 + 结构 1 = 34；参数化 supplychainQueryWireMatrix 覆盖 3001-3006

# 4. BodyClassRegistry Javadoc 硬编码 "37"
grep -n '37 entries' BodyClassRegistry.java   # → 头部 Javadoc 单处 ✅

# 5. REGISTRY 37 entries 真相源
# 结构测试 countRegistryEntries()==37 + REGISTERED_MSG_NO_COUNT==37 双向一致
```

执行阶段（T0 实施前）须重测:
```bash
cd /Users/muzhou/FEP_v1.0 && git fetch origin main && git log -1 --oneline origin/main   # baseline drift 检查（红线 feedback_baseline_drift_during_long_review_cycle）
git worktree list   # 并发态重测（红线 feedback_worktree_trigger_is_dynamic_recheck_at_execution）
```

---

## §5 7 项 Plan 评审清单（`docs/guides/plan-review-checklist.md` 强制）

| # | 检查项 | 本 Plan 自检 |
|:-:|--------|-------------|
| 1 | PRD 覆盖（FR-ID） | meta-Plan 无新 FR；§0 声明追溯既有 P4-MSG-* 已交付能力 ✅ |
| 2 | 安全边界（⛔ 模式 E） | 无安全禁入区域（纯测试 + Javadoc），全 模式 A ✅ |
| 3 | 无占位符 | 全步骤含完整代码 + 命令，无 TBD/TODO ✅ |
| 4 | 类型一致性 | 37 矩阵 class 名与既有 import / @Test 断言逐一核对一致 ✅ |
| 5 | 测试命令可执行 | `-Dtest=BodyClassRegistryTest` 与类名匹配 + `-Dsurefire.failIfNoSpecifiedTests=false`（红线 `feedback_surefire3_failifno_specified_tests_param_rename`）✅ |
| 6 | 数据点自洽 | 矩阵 37 行 = 各批次 sum + 与 COUNT/countRegistryEntries 一致（红线 `feedback_plan_template_data_point_self_consistency`）✅ |
| 7 | Worktree 触发 | 第 2 项 + 多会话活跃命中 → 独立 worktree 声明 + teardown ✅ |

---

## §6 红线触发矩阵

| 红线 | 本 Plan 关联点 |
|------|--------------|
| `feedback_batch_scope_creep` | R2 scope 扩张已获 muzhou 显式批准（2026-06-01 AskUserQuestion）|
| `feedback_worktree_isolates_fs_not_logic_domain` | 3 别会话活跃 → 独立 worktree（即使单文件无交集）|
| `feedback_worktree_trigger_is_dynamic_recheck_at_execution` | T0 实施前 4 步重测并发态 |
| `feedback_baseline_drift_during_long_review_cycle` | baseline 起草 `f89fe38` → 评审重测 `d8322a3`（PR #27/#28/#29/#32 merge）；签字/实施各时点再测 |
| `feedback_plan_template_data_point_self_consistency` | 矩阵 37 行自洽核对（各批次 sum + COUNT 一致）|
| `feedback_surefire3_failifno_specified_tests_param_rename` | mvn 命令用 `-Dsurefire.failIfNoSpecifiedTests=false` |
| `feedback_full_regression_before_commit` | T3 fep-web 全模块回归 |
| `feedback_mvn_sandbox_exit144_pattern` | T3 沙盒 mvn 失败兜底（静态等价 review + GHA）|
| `feedback_systemic_ci_blocker_defers_positive_backing` | GHA billing 阻塞下 tier-A 充分 ✅ CLOSED |
| `feedback_bg_bash_path_inheritance` | mvn 命令显式前缀 JAVA_HOME/PATH |
| `feedback_doc_data_grep_first` | Step 1 baseline Tests run 数实测为准非估算 |

---

## §7 待 reviewer / muzhou 决策项

1. **R2 @link vs @value 取舍** —— Plan 选 `{@link}`（引用链接，跨模块安全）而非 `{@value}`（值替换，per-module javadoc 可能不解析）。如 muzhou 倾向保留显式数字「共 37 份（见 {@link ...}）」混合写法，可在签字时指定。
2. **T3 CLAUDE.md 状态段** —— `/Users/muzhou/FEP_v1.0` 仓库内是否有 tracked CLAUDE.md 状态段需更新待实测；若无则 T3 仅 push + session-end。
3. **矩阵分组注释 provenance** —— 原 31 @Test 的 `@DisplayName` 业务注解（如「核心企业授信额度回执」）在参数化后由批次分组注释承载，业务细粒度注解有损失；如需保留可加第 3 列 description（轻微 bloat），默认不加。

---

## 评审记录

**Round 1 — 独立 AI 评审（general-purpose, agentId `af251036c140c2d9a`, 2026-06-01）: PASS WITH CONCERN**

- ✅ 7 项 plan-review-checklist 全过 + 重点核查 A-E 全绿:
  - A 完整性: 差集 = 空集（真实 31 单映射 @Test + 6 supplychainQueryWireMatrix = 37 = 新矩阵 37，逐一对应无遗漏无新增）
  - B class 名: 37/37 全对零 typo（易错项 1104=...Transfer 非 Request、3009=RzReturnInfo3009 非备用 RzAmtInfo3009 实测正确）
  - C @link: fep-web pom 行 37 依赖 fep-converter ✓ / dispatcher public ✓ / `REGISTERED_MSG_NO_COUNT` public static final int=37 ✓ / FQN 拼写正确
  - D 行为等同: 结构 growth-guard + 2 negative 在删除范围(74-282)之外保留不动 ✓
  - E 删除范围: 行 74-282 恰为 31 单映射 + 旧参数化 + matrix 静态方法，不触及 negative(284-298)/结构(314-327)/helper(335-340) ✓
- 🟡 CONCERN 1（baseline drift）: 起草 baseline `f89fe38` 评审时已推进至 `d8322a3` → **已修订**（baseline 字段更新 + 实测两目标文件未被 4 commit 改动 + worktree 从 d8322a3 切）
- 🟡 CONCERN 2（行为等同对照数）: T1 Step1 期望 35 应为 40 → **已修订**（baseline 与 after 均 40 = 31+2+1+6，硬证前后相等）

**boil-lake 修订自验（红线 `feedback_concern_boil_lake_when_cheap_and_safe`）**: 2 处 CONCERN 均为 Plan 表述修订（非代码 diff），reviewer 明确预授权"修订后可签字"；修订内容 grep 自验（`d8322a3` 实为当前 origin/main HEAD；`40 = 31+2+1+6` 算术确认），无需 Round 2 全量重派。

## 批准签字

**Plan Approver: muzhou — ✅ APPROVED（2026-06-01，via AskUserQuestion）**

- 决策: 批准 + 立即实施（subagent-driven，独立 worktree `wt-simplify-q3-r2` 从 `d8322a3` 切）
- 依据: Round 1 AI 评审 7 项 plan-review-checklist 全过 + A-E 重点核查全绿（37/37 class 名零 typo、删除范围精确、行为等同硬证充分）；2 项 CONCERN（baseline drift + 对照数）已 boil-lake 修订并自验
- 执行: T1（DEF-Q-NEW-3）→ T2（DEF-Reuse-R2）→ T3（closing），每 Task 独立 spec + quality review subagent（红线 `feedback_task_review_discipline`）
