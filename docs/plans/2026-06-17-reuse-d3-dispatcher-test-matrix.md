# REUSE-D3 OutboundWireShapeDispatcherTest 数据驱动矩阵化 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 把 `OutboundWireShapeDispatcherTest`（fep-converter，485 行）里 ~16 个手写散列 `@Test` 合并为单一数据驱动 `@ParameterizedTest` 全量矩阵 + 漂移哨兵断言，行为等价，消除复发的 Javadoc/枚举漂移痛点（2026-05-28 Q-FIX-1 漏 2102/2103/2104 即源于此处手维护列表）。

**Architecture:** 测试保留**独立 oracle**——硬编码 41 行期望矩阵（msgNo → headElementName / headClass / requiresResultCode），不从被测 dispatcher 反推（否则断言退化为重言式）。新增 1 个 size-guard `@Test` 把矩阵 msgNo 集合与 dispatcher 的 6 个 public `Set<String>` 类目常量并集 + `REGISTERED_MSG_NO_COUNT` 双向交叉核验，未来任一侧增删 msgNo 而另一侧漏改即 RED。`describeFor` 非法 msgNo `@Test` 与既有 `supplychainQueryShapeMatrix` 模式保留/吸收。纯 test-only，单文件，单模块。

**Tech Stack:** Java 17 / JUnit 5（`@ParameterizedTest` + `@MethodSource`）/ AssertJ。

**执行 Worktree:** `E:\FEP_v1.0_wt-reuse-d3`（分支 `chore/reuse-d3-dispatcher-test-matrix`，触发条件第 2 项「与已签字未执行 Plan 并存」+ shared-working-tree memory；别会话 `wt-dzpz-record` 活跃）。

**回归范围（红线 `feedback_plan_regression_scope_explicit`）:**
- **Minimum（本机必跑）:** `./mvnw -pl fep-converter -o test`（**不带 `-am`**，红线 `feedback_single_module_regression_no_am_flag`；上游 jar 已在 `~/.m2`，若缺则先一次性 `-am install -DskipTests`）。聚焦 `OutboundWireShapeDispatcherTest` GREEN + 全 fep-converter test GREEN + spotbugs:check BugInstance 0 + ArchUnit PASS。
- **Strong（GHA）:** Build/Test & Quality + SonarCloud（PR 触发）。

**不在范围（明示）:** 不改 production `OutboundWireShapeDispatcher`；不改 `WireShapeDescriptor`；不动 REUSE-D2 跨模块 `A1000143000104` 字面量（large/low-value/risky，deferred 保留）；不动 fep-web/fep-processor 其他 wire 测试。

---

## 背景：实测确认的当前状态（红线 `feedback_plan_must_grep_actual_api`）

被测 `OutboundWireShapeDispatcher`（`fep-converter/src/main/java/com/puchain/fep/converter/wire/OutboundWireShapeDispatcher.java`）暴露 6 个 public 类目集合 + 总数常量（实测 b546e48）：

| 类目常量 | head 前缀 | headClass | requiresResultCode | 成员（实测） | 数 |
|---|---|---|---|---|---|
| `REAL_HEAD_REQUEST_MSG_NOS` | RealHead | `RequestBusinessHead` | false | 1001,1004,3000,3001,3003,3005,3007,3009,9000,9005,9006,9008 | 12 |
| `BATCH_HEAD_REQUEST_MSG_NOS` | BatchHead | `RequestBusinessHead` | false | 1101,1102,1103,1104,3102,3105,3107,3109,3112,3116,3120,9100 | 12 |
| `REAL_HEAD_RESPONSE_MSG_NOS` | RealHead | `ResponseBusinessHead` | true | 2001,2004,3002,3004,3006,3008,9020 | 7 |
| `BATCH_HEAD_RESPONSE_MSG_NOS` | BatchHead | `ResponseBusinessHead` | true | 2102,2103,2104,3101,3103,3108,3113,9120 | 8 |
| `REAL_HEAD_REQUEST_RESPONSE_MSG_NOS` | RealHead | `RequestResponseHead` | false | 3020 | 1 |
| `BATCH_HEAD_REQUEST_RESPONSE_MSG_NOS` | BatchHead | `RequestResponseHead` | false | 3115 | 1 |

合计 12+12+7+8+1+1 = **41** = `REGISTERED_MSG_NO_COUNT`（实测 L129）。`describeFor(msgNo)` 返回 `WireShapeDescriptor(headElementName, headClass, requiresResultCode)`；`isRegisteredOutboundMsgNo(msgNo)` 对 41 集合返 true。

被测试文件（`fep-converter/src/test/java/com/puchain/fep/converter/wire/OutboundWireShapeDispatcherTest.java`，485 行）现含：
- ~16 个散列 `@Test`（按类目/批次手写 `describeFor_*` + `isRegisteredOutboundMsgNo_*`，多含内部 `for` 循环遍历硬编码 msgNo 数组）；
- 1 个 `@ParameterizedTest`（`supplychainQueryShapeMatrix`，仅覆盖 3001-3006）；
- 1 个非法 msgNo `@Test`（9999/abc/null/长度错，断言 `OUTBOUND_5108_MSGNO_INVALID`）；
- 类头 19-38 行手维护 41-msgNo 矩阵 Javadoc（**漂移源**）。

---

## Task 1: 全量数据驱动矩阵 + size-guard（替换散列 @Test）

**Files:**
- Modify/rewrite: `fep-converter/src/test/java/com/puchain/fep/converter/wire/OutboundWireShapeDispatcherTest.java`

**Step 1: 写新的全量矩阵 MethodSource + 漂移哨兵（先让 size-guard 失败以证伪能抓漂移）**

在测试类内定义**单一真相源** = 一个类型安全的 `ShapeRow` record 列表，枚举**全部 41 行**期望矩阵（独立 oracle，硬编码，不从 dispatcher 反推）。参数化测试与 size-guard 共享此 `List`，避免 `Arguments.get()` 反射取参（评审 Round1 BLOCKER 规避——`ShapeRow::msgNo` 类型安全直取）：

```java
/** 单一真相源：41 行期望 wire-shape 矩阵（独立硬编码 oracle）。 */
private record ShapeRow(String msgNo, String headElementName,
                        Class<?> headClass, boolean requiresResultCode) { }

private static final List<ShapeRow> WIRE_SHAPE_MATRIX = List.of(
        // RealHead + RequestBusinessHead + false (12)
        new ShapeRow("1001", "RealHead1001", RequestBusinessHead.class, false),
        new ShapeRow("1004", "RealHead1004", RequestBusinessHead.class, false),
        new ShapeRow("3000", "RealHead3000", RequestBusinessHead.class, false),
        new ShapeRow("3001", "RealHead3001", RequestBusinessHead.class, false),
        new ShapeRow("3003", "RealHead3003", RequestBusinessHead.class, false),
        new ShapeRow("3005", "RealHead3005", RequestBusinessHead.class, false),
        new ShapeRow("3007", "RealHead3007", RequestBusinessHead.class, false),
        new ShapeRow("3009", "RealHead3009", RequestBusinessHead.class, false),
        new ShapeRow("9000", "RealHead9000", RequestBusinessHead.class, false),
        new ShapeRow("9005", "RealHead9005", RequestBusinessHead.class, false),
        new ShapeRow("9006", "RealHead9006", RequestBusinessHead.class, false),
        new ShapeRow("9008", "RealHead9008", RequestBusinessHead.class, false),
        // BatchHead + RequestBusinessHead + false (12)
        new ShapeRow("1101", "BatchHead1101", RequestBusinessHead.class, false),
        new ShapeRow("1102", "BatchHead1102", RequestBusinessHead.class, false),
        new ShapeRow("1103", "BatchHead1103", RequestBusinessHead.class, false),
        new ShapeRow("1104", "BatchHead1104", RequestBusinessHead.class, false),
        new ShapeRow("3102", "BatchHead3102", RequestBusinessHead.class, false),
        new ShapeRow("3105", "BatchHead3105", RequestBusinessHead.class, false),
        new ShapeRow("3107", "BatchHead3107", RequestBusinessHead.class, false),
        new ShapeRow("3109", "BatchHead3109", RequestBusinessHead.class, false),
        new ShapeRow("3112", "BatchHead3112", RequestBusinessHead.class, false),
        new ShapeRow("3116", "BatchHead3116", RequestBusinessHead.class, false),
        new ShapeRow("3120", "BatchHead3120", RequestBusinessHead.class, false),
        new ShapeRow("9100", "BatchHead9100", RequestBusinessHead.class, false),
        // RealHead + ResponseBusinessHead + true (7)
        new ShapeRow("2001", "RealHead2001", ResponseBusinessHead.class, true),
        new ShapeRow("2004", "RealHead2004", ResponseBusinessHead.class, true),
        new ShapeRow("3002", "RealHead3002", ResponseBusinessHead.class, true),
        new ShapeRow("3004", "RealHead3004", ResponseBusinessHead.class, true),
        new ShapeRow("3006", "RealHead3006", ResponseBusinessHead.class, true),
        new ShapeRow("3008", "RealHead3008", ResponseBusinessHead.class, true),
        new ShapeRow("9020", "RealHead9020", ResponseBusinessHead.class, true),
        // BatchHead + ResponseBusinessHead + true (8)
        new ShapeRow("2102", "BatchHead2102", ResponseBusinessHead.class, true),
        new ShapeRow("2103", "BatchHead2103", ResponseBusinessHead.class, true),
        new ShapeRow("2104", "BatchHead2104", ResponseBusinessHead.class, true),
        new ShapeRow("3101", "BatchHead3101", ResponseBusinessHead.class, true),
        new ShapeRow("3103", "BatchHead3103", ResponseBusinessHead.class, true),
        new ShapeRow("3108", "BatchHead3108", ResponseBusinessHead.class, true),
        new ShapeRow("3113", "BatchHead3113", ResponseBusinessHead.class, true),
        new ShapeRow("9120", "BatchHead9120", ResponseBusinessHead.class, true),
        // RealHead + RequestResponseHead + false (1, 孤儿第 5 类目)
        new ShapeRow("3020", "RealHead3020", RequestResponseHead.class, false),
        // BatchHead + RequestResponseHead + false (1, 第 6 类目)
        new ShapeRow("3115", "BatchHead3115", RequestResponseHead.class, false)
);

static Stream<Arguments> wireShapeMatrix() {
    return WIRE_SHAPE_MATRIX.stream().map(r ->
            Arguments.of(r.msgNo(), r.headElementName(), r.headClass(), r.requiresResultCode()));
}
```

参数化测试体（吸收既有断言三元组 + `isRegisteredOutboundMsgNo` true 断言，复用 `supplychainQueryShapeMatrix` 既有写法）：

```java
@ParameterizedTest(name = "[{index}] msgNo={0} → {1}({2}), result={3}")
@MethodSource("wireShapeMatrix")
@DisplayName("41 上行报文 wire-shape 路由全量矩阵")
void describeFor_shouldRouteAllRegisteredMsgNos(
        final String msgNo, final String expectedHeadElementName,
        final Class<?> expectedHeadClass, final boolean expectedRequiresResultCode) {
    final WireShapeDescriptor desc = dispatcher.describeFor(msgNo);
    assertThat(desc.headElementName()).as("msgNo=%s headElementName", msgNo)
            .isEqualTo(expectedHeadElementName);
    assertThat(desc.headClass()).as("msgNo=%s headClass", msgNo)
            .isEqualTo(expectedHeadClass);
    assertThat(desc.requiresResultCode()).as("msgNo=%s requiresResultCode", msgNo)
            .isEqualTo(expectedRequiresResultCode);
    assertThat(dispatcher.isRegisteredOutboundMsgNo(msgNo))
            .as("msgNo=%s 必须在 isRegisteredOutboundMsgNo true", msgNo).isTrue();
}
```

漂移哨兵 size-guard（双向交叉核验：矩阵 ↔ dispatcher 6 个 public 集合并集 + 总数常量）：

```java
@Test
@DisplayName("矩阵覆盖与 dispatcher 登记集合双向一致（漂移哨兵）")
void wireShapeMatrix_mustCoverExactlyRegisteredSet() {
    final Set<String> matrixMsgNos = WIRE_SHAPE_MATRIX.stream()
            .map(ShapeRow::msgNo)
            .collect(Collectors.toSet());

    final Set<String> dispatcherMsgNos = new HashSet<>();
    dispatcherMsgNos.addAll(OutboundWireShapeDispatcher.REAL_HEAD_REQUEST_MSG_NOS);
    dispatcherMsgNos.addAll(OutboundWireShapeDispatcher.BATCH_HEAD_REQUEST_MSG_NOS);
    dispatcherMsgNos.addAll(OutboundWireShapeDispatcher.REAL_HEAD_RESPONSE_MSG_NOS);
    dispatcherMsgNos.addAll(OutboundWireShapeDispatcher.BATCH_HEAD_RESPONSE_MSG_NOS);
    dispatcherMsgNos.addAll(OutboundWireShapeDispatcher.REAL_HEAD_REQUEST_RESPONSE_MSG_NOS);
    dispatcherMsgNos.addAll(OutboundWireShapeDispatcher.BATCH_HEAD_REQUEST_RESPONSE_MSG_NOS);

    // 矩阵行数 = 总数常量（List.of 元素重复无法去重，此断言锁定无重复 msgNo 行）
    assertThat(WIRE_SHAPE_MATRIX)
            .as("矩阵行数 = REGISTERED_MSG_NO_COUNT")
            .hasSize(OutboundWireShapeDispatcher.REGISTERED_MSG_NO_COUNT);
    assertThat(matrixMsgNos)
            .as("矩阵 msgNo distinct 集合数 = 总数常量（捕获重复行）")
            .hasSize(OutboundWireShapeDispatcher.REGISTERED_MSG_NO_COUNT);
    // 双向：矩阵 msgNo 集合 == dispatcher 6 类目并集（任一侧增删而另一侧漏改即 RED）
    assertThat(matrixMsgNos)
            .as("测试矩阵与 dispatcher 登记集合必须逐一致（漂移哨兵）")
            .containsExactlyInAnyOrderElementsOf(dispatcherMsgNos);
}
```

> **设计说明（评审关注点）:** 矩阵是**硬编码独立 oracle**：每行 `headElementName`/`headClass`/`requiresResultCode` 期望值独立写死，由**参数化测试体**逐字段断言——dispatcher 若把某 msgNo 错配类目（如 3008 误归 RealHead+Request），参数化体 `headClass` 断言即 RED。漂移哨兵**只做 msgNo 集合成员**双向核验（矩阵 ↔ dispatcher 6 类目并集），职责是捕获**增删漂移**（dispatcher 加报文而测试漏更新，或反之），**不**重复校验三字段——故二者不互相反推，无重言式；两层互补覆盖「逻辑回归」+「集合漂移」。

新增 import：`java.util.HashSet` / `java.util.List` / `java.util.Set` / `java.util.stream.Collectors`（`Stream`/`Arguments`/`MethodSource`/`ParameterizedTest`/`Test`/`assertThat` 已在）。

删除被矩阵吸收的散列 `@Test`：所有 `describeFor_*` 单/多 msgNo 方法 + 所有 `isRegisteredOutboundMsgNo_*` 方法 + 旧 `supplychainQueryShapeMatrix` 参数化（其 6 行并入全量矩阵）。**保留** 非法 msgNo `@Test`（`describeFor_invalid_msgNo_should_throw_5108`，独立关注点，矩阵不覆盖负路径）。

**Step 2: 同步类头 Javadoc 去漂移**

把 19-38 行手维护的 41-msgNo 逐条 `<ul>` 列表，改为指向 `wireShapeMatrix()` 为单一真相源的简短说明（保留 6 类目概述，删逐 msgNo 手列表），杜绝 Q-FIX-1 型 header-count vs bullet-sum 漂移。Javadoc 数据点须自洽（红线 `feedback_plan_template_data_point_self_consistency`）：若保留任何计数，须 = 41。

**Step 3: 运行单文件测试确认 GREEN（漂移哨兵能抓漂移的反证）**

```
cd E:\FEP_v1.0_wt-reuse-d3
$env:JAVA_HOME=...; ./mvnw -pl fep-converter -o test -Dtest=OutboundWireShapeDispatcherTest > E:\FEP_v1.0_wt-reuse-d3\target-d3-test.log 2>&1
```
> ⚠️ 长跑 mvn 须 redirect-to-file，禁 `|tail`（红线 `feedback_pipe_tail_deadlock_with_bg_bash`）。
预期：`OutboundWireShapeDispatcherTest` 全 GREEN（41 参数化 case + 1 size-guard + 1 invalid = ~43 行 run）。
反证哨兵有效性（**人工一次性**，不提交）：临时把矩阵删 1 行 → 重跑 → size-guard RED（`hasSize`/`containsExactlyInAnyOrder` 失败）→ 还原。证明哨兵真能抓漏。

**Step 4: 全 fep-converter 回归 + 质量门（红线 `feedback_subagent_must_run_spotbugs_check` / `feedback_full_regression_before_commit`）**

```
./mvnw -pl fep-converter -o verify > E:\FEP_v1.0_wt-reuse-d3\target-d3-verify.log 2>&1
```
> `verify` 触发 spotbugs:check（绑 verify phase）+ ArchUnit + jacoco。纯测试改动，须 BugInstance 0 + ArchUnit PASS + fep-converter 全测试 GREEN（既有总数以实跑日志为准，禁估算——红线 `feedback_doc_data_grep_first`；本文件方法数 27 → 3 但参数化 41 行使断言覆盖等价，旧 ~16 散列方法逐条吸收进矩阵，invalid-msgNo 负路径单独保留）。
> 若 `-o` 报上游 SNAPSHOT 缺失（别会话 .m2 clobber，红线 `feedback_shared_m2_snapshot_cross_session_clobber`）：先查 baseline drift（`git fetch` + `rev-parse HEAD origin/main`），无漂移再一次性 `-am install -DskipTests` 装上游链。

**Step 5: 提交（独立命令，红线 `feedback_commit_no_chain_with_verify_command`）**

先单独 `cat`/`Select-String` 看 verify 日志确认 `BUILD SUCCESS` + spotbugs 0，**再**独立执行 commit：

```
git add fep-converter/src/test/java/com/puchain/fep/converter/wire/OutboundWireShapeDispatcherTest.java
git commit -m "test(converter): REUSE-D3 OutboundWireShapeDispatcherTest 数据驱动矩阵化 + 漂移哨兵

合并 ~16 散列 @Test 为单一 41 行 @ParameterizedTest 全量矩阵（独立硬编码
oracle），新增 size-guard 双向核验矩阵 ↔ dispatcher 6 类目集合并集 +
REGISTERED_MSG_NO_COUNT，消除 Q-FIX-1 型 Javadoc/枚举漂移。行为等价，纯
test-only，0 production 改动。

Simplify deferred 池 REUSE-D3 闭合（来源 2026-05-28 P4-MSG-I Simplify 三审）。

AI-Generated: claude-code
Reviewed-By: pending"
```

---

## Task 2: deferred 池 retirement 记账（doc-only）

**Files:**
- Create: `docs/daily_reports/2026-06-17-reuse-d3-deferred-pool-retire.md`（记账：本轮实测确认的 Simplify deferred 池真实状态）

**Step 1: 落盘 retirement 记账**

记录实测结论（grep 实证）：
- **REUSE-D1 / EFF-D2（wrap helper）**：RESOLVED-BY-INTERVENING-WORK——`AbstractXsdValidationTest.wrapCfxTemplate(...)` 共享父助手已存在并被 31 文件引用；残留 4 个本地 `wrap(` 是 `extends AbstractXsdValidationTest` 的薄适配器（委托 `wrapCfxTemplate`），为设计形态非重复。
- **REUSE-D2（HNDEMP node-code 常量）**：SATISFIED——常量已存在两处（`FepConstants.HNDEMP_NODE_CODE` + `AbstractXsdValidationTest.HNDEMP_NODE`），validation 包 31 测试已引用；残留 ~145 跨模块字面量（fep-web/converter/security）是 large/low-value/risky churn → **DEFERRED 保留**（不在本轮）。
- **B-8 `openMockSession`**：RESOLVED——`DashboardWebSocketHandlerTest.newSession(id, attrs)` 共享助手已存在，8 测试复用；残留 per-test `new HashMap<>()` 为有意（断言/变更本地引用）。
- **REUSE-D3**：本轮 ✅ Task 1 ship。
- **DEF-B2-1 / DEF-B2-2 / DEF-3（B-8）**：BLOCKED on external（甲方/muzhou 决策 or B-9 部署），非代码项，保留。

**Step 2: 提交（doc-only，独立命令）**

```
git add docs/daily_reports/2026-06-17-reuse-d3-deferred-pool-retire.md
git commit -m "docs(reports): REUSE-D3 + Simplify deferred 池 retirement 记账

实测确认 REUSE-D1/EFF-D2 已被 AbstractXsdValidationTest 消化、REUSE-D2 常量已
存在（跨模块字面量 churn deferred）、B-8 openMockSession 已有 newSession 助手；
DEF-B2-1/B2-2/DEF-3 blocked on external 保留。

AI-Generated: claude-code
Reviewed-By: pending"
```

---

## 验收清单（red-line 对照）

- [ ] Task 1：`OutboundWireShapeDispatcherTest` 全 GREEN（41 矩阵 + size-guard + invalid）；行为等价覆盖。
- [ ] size-guard 反证有效（人工删 1 行 RED 实测，还原）。
- [ ] fep-converter `-pl fep-converter -o verify` BUILD SUCCESS + spotbugs:check BugInstance 0 + ArchUnit PASS。
- [ ] 类头 Javadoc 去逐-msgNo 手列表，数据点自洽（计数若存在 = 41）。
- [ ] 2 commit 各 `AI-Generated: claude-code` + `Reviewed-By:`；commit 作独立命令未与验证链式。
- [ ] 逐 commit 自洽（红线 `feedback_commit_tree_self_consistent_per_commit`）：Task 1 test commit 单文件独立可编译；Task 2 doc-only 独立。
- [ ] 9-维技术文档 + Daily Report 走 session-end（红线 `feedback_infra_plan_still_needs_full_8dim_docs`：有 code commit → 全维文档）。
- [ ] worktree `wt-reuse-d3` session-end teardown。

## 评审 / 签字门

1. 起草（本文件）→ 2. ✅ Explore 只读评审 Round1 = NEEDS-REVISION（1 BLOCKER `Arguments.get()` + minors）→ 修订（ShapeRow record 单源 + size-guard 措辞 + 删未核计数）→ 3. ✅ muzhou 签字（2026-06-17，hybrid 执行）→ 4. 执行（主对话 hybrid：edits + 前台 mvn + commit per task，红线 `feedback_harness_bg_detach_hybrid_default`）。
