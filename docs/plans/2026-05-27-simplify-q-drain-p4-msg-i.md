# FEP Simplify Quality Drain — P4-MSG-I Deferred Low-Risk Batch 实施计划

> **执行方式:** 使用 superpowers:subagent-driven-development（推荐）逐 Task 执行。步骤使用 `- [ ]` 复选框跟踪。

**目标:** 消化 P4-MSG-I 三审 Quality deferred pool 中 4 项 LOW 优先级（DEF-Q-NEW-4/5/6/7），全部为纯重构/Javadoc/测试模板优化，零行为变更。**v0.3 修订**: T2 (DEF-Q-NEW-6) 因 `wt-r-new-1-real-xsd` 别会话同文件冲突 → 🚫 BLOCKED 推迟到 r-new-1 ship 后 follow-up Plan；本轮实际 drain **3 项 (T1/T3/T4)**。baseline 从 `457a5e4` rebase 到 `df15613`（pitest-maven 1.25.0 升级别会话 ship，单 pom.xml 改 0 冲突）。

**前置依赖:** 无业务依赖。**baseline = origin/main `df15613`**（v0.3 rebase 自 `457a5e4` 推进至 `df15613` — drift commit `df15613 chore(deps): upgrade pitest-maven 1.23.1 -> 1.25.0` 仅触 `pom.xml` 与本 Plan test 文件零重叠）。

**执行 Worktree:** `/Users/muzhou/FEP_v1.0_wt-simplify-q-drain`（分支 `chore/simplify-q-drain-p4-msg-i`，触发条件第 ② + ⑥ + worktree_isolates_fs 第 7 项）

> 红线 `feedback_worktree_for_parallel_work` + `feedback_worktree_isolates_fs_not_logic_domain` 命中实测（v0.3 更新）:
> - ② 与已签字未执行的 Plan 并存: 主 worktree 含 `2026-05-26-callback-module-phase2-reliability.md`（PR #27 待 merge）+ `2026-05-26-r-new-1-real-xsd-validator.md`（别会话签字 + worktree `wt-r-new-1-real-xsd` 在执行中）+ `2026-05-26-asyncpipeline-perf-flake-disable-on-mac.md`（别会话 worktree `wt-asyncpipeline-flake` 在执行中）
> - ⑥ muzhou WIP 与 AI 任务并存: PR #27 + r-new-1 + asyncpipeline 三别会话并行
> - worktree_isolates_fs 第 7 项: 多会话活跃（e2e + callback-p2 + r-new-1 + asyncpipeline + 本会话 = 5 worktree 同时活跃）
>
> **并排除项**（v0.3 显式声明，遵 `feedback_parallel_session_task_allocation_discipline` Step 0 锁定文件）:
> - `fep-web/.../callback/*` 任何文件（PR #27 owns）
> - `fep-web/.../outbound/consumer/Outbound9120AckEnvelopeBuilderTest.java`（r-new-1 owns，本 Plan T2 🚫 BLOCKED 推迟）
> - `fep-web/.../messageinbound/listener/Inbound2101WireTest.java` / `Inbound3112WireTest.java` / `InboundAck9120BatchWireTest.java`（r-new-1 owns）
> - `fep-web/.../outbound/consumer/P5OutboundEndToEndIntegrationTest.java` + `src/test/resources/sql/p5/outbound_queue_8_messages.sql`（r-new-1 owns）
> - `fep-processor/.../pipeline/AsyncPipelineIntegrationTest.java`（asyncpipeline-flake owns）
> - `pom.xml`（pitest-maven 升级已 ship `df15613`，0 触动）

**架构:** 0 业务变更纯重构 — Javadoc/import order/builder.build() 复用/per-class wrap helper 4 类 polish。所有验收为"既有测试断言路径不变 + 全部 GREEN"。

**技术栈:** Java 17 / Spring Boot 3.x / Maven / AssertJ / JUnit 5 / @SpringBootTest

**PRD 追溯:** **不适用**（元流程/refactor Plan，红线 `CLAUDE.md` "每个 Task 必须引用 PRD 章节 + FR-ID（基础设施/元流程 Plan 除外）"）。本 Plan 0 业务功能变更，0 新增 FR-ID 覆盖；对 P4-MSG-I 已 ship 的 9120/3113/9100/9000 outbound wire（FR-MSG-9120/3113/9100/9000）的代码品质 polish。PRD 矩阵无需更新。

**AI 协同模式:**

| 模式 | AI 占比 | 本计划中的应用 |
|:----:|:-------:|--------------|
| A | 90% | 全部 4 Task — 纯 Javadoc/import/refactor，无业务/算法/安全相关 |

**deferred 来源映射（per-Task 与 P4-MSG-I §5 deferred pool 对照）:**

| Task | deferred ID | 主题 | 文件 | 改动量预估 | 状态 |
|---|---|---|---|---|---|
| T1 | DEF-Q-NEW-5 | dispatcher 测试 Javadoc "31" → "37" + 6 新 msgNo 入列 | `OutboundWireShapeDispatcherTest.java` Javadoc | ~15 行 doc | ✅ ACTIVE |
| T2 | DEF-Q-NEW-6 | builder.build() 2 次调用 → 1 次共享结果 | `Outbound9120AckEnvelopeBuilderTest.java` 1 method | -2 net 行 | 🚫 **BLOCKED v0.3** — r-new-1 别会话 owns 同文件 → defer to follow-up Plan after r-new-1 ship |
| T3 | DEF-Q-NEW-7 | 4 *XsdValidationTest 引入 per-class `wrap()` helper | 4 files in `fep-processor/.../validation/` | ~30 net 行 | ✅ ACTIVE |
| T4 | DEF-Q-NEW-4 | `OutboundCommonForwardWireTest` import order **决策点** | `OutboundCommonForwardWireTest.java` 10 imports | 0 行（v0.3 muzhou 签字 = (A) Skip） | ✅ ACTIVE — (A) Skip |

> **T4 备注 — DEF-Q-NEW-4 决策点**: P4-MSG-I 三审 reviewer 标 LOW "checkstyle nit"，但 grep 实测 `checkstyle.xml` 仅 `AvoidStarImport / UnusedImports / RedundantImport`，**无 ImportOrder 规则**；且 sibling `Outbound1101WireTest.java` + `AbstractOutboundWireMatrixTest.java` 均 `com.* → java.* → org.*` 同序。Plan 头部预留 T4 决策项：muzhou 签字时选 (A) skip with rationale or (B) Java-convention 重排（java.* first）。Plan 写两套 Step，按签字结果走一支。

---

## §1 文件结构

| 文件路径 | 职责 | 操作 | AI 模式 |
|----------|------|------|:-------:|
| `fep-converter/src/test/java/com/puchain/fep/converter/wire/OutboundWireShapeDispatcherTest.java` | dispatcher 单元测试（27 @Test + 1 @ParameterizedTest×6） | Modify Javadoc | A |
| `fep-web/src/test/java/com/puchain/fep/web/outbound/consumer/Outbound9120AckEnvelopeBuilderTest.java` | 9120 ack envelope 装配段 P0 IT | Modify 1 method | A |
| `fep-processor/src/test/java/com/puchain/fep/processor/validation/MsgReturn9120XsdValidationTest.java` | 9120 XSD validation 测试 | Refactor wrap helper | A |
| `fep-processor/src/test/java/com/puchain/fep/processor/validation/HxqyCreditAmt3113XsdValidationTest.java` | 3113 XSD validation 测试 | Refactor wrap helper | A |
| `fep-processor/src/test/java/com/puchain/fep/processor/validation/Forward9100XsdValidationTest.java` | 9100 XSD validation 测试 | Refactor wrap helper | A |
| `fep-processor/src/test/java/com/puchain/fep/processor/validation/Forward9000XsdValidationTest.java` | 9000 XSD validation 测试 | Refactor wrap helper | A |
| `fep-web/src/test/java/com/puchain/fep/web/outbound/consumer/OutboundCommonForwardWireTest.java` | 4 batch4 outbound wire IT | Maybe modify imports | A |

### 共享工具类清单

无新增共享工具。本 Plan 仅消费既有 `AbstractXsdValidationTest.wrapCfxTemplate(String×8)`，T3 在各 `*XsdValidationTest` 子类内 **private static** 加 `wrap(...)` 局部包装，不抽到父类（不同子类需固定不同的 srcNode/desNode/app/msgNo 组合，父类抽不合理）。

### 核心类职责边界声明

不适用 — 本 Plan 0 触动 production class 职责边界，全部为 test class 内部重构。

---

## §2 Tasks

### Task 0 — Baseline rebase 457a5e4 → df15613 + 重测 worktree 状态 `模式 A`

**目标**: v0.3 muzhou 签字决策 — Plan 起草后 baseline drift `457a5e4 → df15613`（pitest-maven 升级别会话 ship），T1 实施前必须先 rebase worktree onto df15613 验证 0 冲突。同时重测 Step 0 锁定文件冲突（红线 `feedback_baseline_drift_during_long_review_cycle` + `feedback_parallel_session_task_allocation_discipline` 第 ③ T0 时点）。

- [ ] **Step 1: Fetch + rebase worktree onto df15613**

```bash
cd /Users/muzhou/FEP_v1.0_wt-simplify-q-drain
git fetch origin main 2>&1
NEW_BASE=$(git rev-parse origin/main)
echo "rebase target: $NEW_BASE"
# 预期 df15613 或更新 commit
git rebase $NEW_BASE 2>&1
# 预期 0 conflict（pom.xml 改与本 Plan 文件 0 重叠）
```
期望: `Successfully rebased and updated refs/heads/chore/simplify-q-drain-p4-msg-i`。如有冲突 → STOP，向 muzhou 报告。

- [ ] **Step 2: 重测 worktree 锁定文件冲突**

```bash
cd /Users/muzhou/FEP_v1.0_wt-simplify-q-drain
git worktree list
# 对每个别会话 worktree 实测正在改的文件
for w in /Users/muzhou/FEP_v1.0_wt-r-new-1-real-xsd /Users/muzhou/FEP_v1.0_wt-asyncpipeline-flake /Users/muzhou/FEP_v1.0_wt-callback-p2; do
  [ -d "$w" ] && echo "=== $w ===" && cd "$w" && git diff --name-only main..HEAD 2>/dev/null; git status --short 2>/dev/null
done
```
期望: 锁定 6 文件（5 r-new-1 + 1 asyncpipeline + N callback）仍属别会话，本 Plan T1/T3/T4 标的文件 0 重叠：
- T1: `fep-converter/src/test/.../OutboundWireShapeDispatcherTest.java` — 0 别会话引用
- T3: `fep-processor/src/test/.../MsgReturn9120/HxqyCreditAmt3113/Forward9100/Forward9000 XsdValidationTest.java` — 0 别会话引用
- T4: (A) Skip path — 0 文件改动

- [ ] **Step 3: 0 commit**（rebase 已 update branch HEAD；锁定重测仅 grep 输出）

> **判定**: 如 Step 1 rebase 冲突或 Step 2 锁定文件出现新冲突 → 立即 STOP + 向 muzhou 报告，不进入 T1。

---

### Task 1 — DEF-Q-NEW-5 `OutboundWireShapeDispatcherTest` Javadoc 更新 `模式 A`

**追溯依据:** P4-MSG-I §5 deferred pool `DEF-Q-NEW-5 | LOW | T1 测试数 doc 更新（dispatcher Javadoc 测试数计数）`

**验收标准:**
1. `OutboundWireShapeDispatcherTest.java` 顶部 Javadoc "覆盖 31 上行报文" 改 "覆盖 37 上行报文"（与 `OutboundWireShapeDispatcher.REGISTERED_MSG_NO_COUNT = 37` 同步）
2. Javadoc 6 处类目列表追加 P4-MSG-H/I 扩展 msgNos（3115/3120/9000/9100/3113/9120），格式与既有 P4-MSG-G 扩展行一致
3. Javadoc "非法 msgNo（null / 非数字 / 长度错 / 不在 31 集合）" 改为 "不在 37 集合"
4. 既有 27 @Test + 1 @ParameterizedTest×6 case **全部 GREEN**，断言路径与代码逻辑均无改动

**Files:**
- Modify: `fep-converter/src/test/java/com/puchain/fep/converter/wire/OutboundWireShapeDispatcherTest.java`

- [ ] **Step 1: grep 实测 Javadoc 现状 + dispatcher 注册 6 类目当前编号**

```bash
cd /Users/muzhou/FEP_v1.0_wt-simplify-q-drain
JAVA_HOME=/usr/local/opt/openjdk@17 PATH=/usr/local/opt/openjdk@17/bin:$PATH

# 1) 实测当前 Javadoc 行号
sed -n '19,36p' fep-converter/src/test/java/com/puchain/fep/converter/wire/OutboundWireShapeDispatcherTest.java

# 2) 实测 dispatcher 6 Set 当前完整元素（验证 Javadoc 列表与代码同步）
grep -nE 'REAL_HEAD_REQUEST_MSG_NOS|BATCH_HEAD_REQUEST_MSG_NOS|REAL_HEAD_RESPONSE_MSG_NOS|BATCH_HEAD_RESPONSE_MSG_NOS|REAL_HEAD_REQUEST_RESPONSE_MSG_NOS|BATCH_HEAD_REQUEST_RESPONSE_MSG_NOS|REGISTERED_MSG_NO_COUNT' fep-converter/src/main/java/com/puchain/fep/converter/wire/OutboundWireShapeDispatcher.java
```
期望: dispatcher 6 Set 总数 = 9+12+6+8+1+1 = **37**（实测已计算）。Javadoc 当前 line 22 "31 上行报文" + line 31 "31 集合"。

- [ ] **Step 2: 修改 Javadoc — class header**

```java
/**
 * {@link OutboundWireShapeDispatcher} 单元测试（P5 T3 + P4-MSG-B T4 扩展）。
 *
 * <p>覆盖 37 上行报文的 dispatch 矩阵（P4-MSG-A T1 起 10→16 含 6 BATCH，P4-MSG-D T3 起 17 含 1101，
 * P4-MSG-E T2 起 21 含 4 realtime 1001/2001/1004/2004，P4-MSG-F T2 起 27 含 6 supplychain query
 * 3001/3002/3003/3004/3005/3006，P4-MSG-G T3 起 31 含 3008/3020/3103/3108，
 * P4-MSG-H 起 33 含 3115/3120 第 5/6 类目，P4-MSG-I 起 37 含 9000/9100/3113/9120 batch4 + 9120 ack）：</p>
 * <ul>
 *   <li>1001/1004/3000/3001/3003/3005/3007/3009/9000 → RealHead{msgNo} + RequestBusinessHead + false（P4-MSG-I 扩展 9000）</li>
 *   <li>2001/2004/3002/3004/3006/3008 → RealHead{msgNo} + ResponseBusinessHead + true（P4-MSG-E/F/G）</li>
 *   <li>3020 → RealHead3020 + RequestResponseHead + false（P4-MSG-G T3 第 5 类目，孤儿成员）</li>
 *   <li>3115 → BatchHead3115 + RequestResponseHead + false（P4-MSG-H 第 6 类目）</li>
 *   <li>3101/3103/3108/3113/9120 → BatchHead{msgNo} + ResponseBusinessHead + true（3103/3108 P4-MSG-G T3 扩展；3113/9120 P4-MSG-I 扩展）</li>
 *   <li>1101/1102/1103/1104/3102/3105/3107/3109/3112/3116/3120/9100 → BatchHead{msgNo} + RequestBusinessHead + false（3120 P4-MSG-H 扩展，9100 P4-MSG-I 扩展）</li>
 *   <li>非法 msgNo（null / 非数字 / 长度错 / 不在 37 集合）→ OUTBOUND_5108_MSGNO_INVALID</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
```

> **注**: 替换 line 19-36 既有 Javadoc 整段（implementer 用 Edit 工具 old_string=line 19-36 完整内容 / new_string=上方）。**不动 class body** — 27 @Test method + ParameterizedTest 0 改动。

- [ ] **Step 3: 运行确认全 GREEN**

```bash
cd /Users/muzhou/FEP_v1.0_wt-simplify-q-drain
JAVA_HOME=/usr/local/opt/openjdk@17 PATH=/usr/local/opt/openjdk@17/bin:$PATH ./mvnw test -pl fep-converter -Dtest='OutboundWireShapeDispatcherTest' -Dsurefire.failIfNoSpecifiedTests=false --batch-mode --no-transfer-progress
```
- **Strong 验收**: `./mvnw test -pl fep-converter` 全 GREEN（27 @Test + 6 ParameterizedTest case = 33 invocations）
- **Minimum 验收**: `OutboundWireShapeDispatcherTest` 33 case all GREEN（红线 `feedback_plan_regression_scope_explicit`）
- **mvn sandbox fallback**: GHA CI 兜底（红线 `feedback_mvn_sandbox_exit144_pattern`）— 提交后 push 等 GHA 绿

- [ ] **Step 4: Checkstyle 验证**

```bash
cd /Users/muzhou/FEP_v1.0_wt-simplify-q-drain
JAVA_HOME=/usr/local/opt/openjdk@17 PATH=/usr/local/opt/openjdk@17/bin:$PATH ./mvnw checkstyle:check -pl fep-converter --batch-mode --no-transfer-progress
```
期望: BUILD SUCCESS（Javadoc 改动不引入 Checkstyle 违规）

- [ ] **Step 5: 提交**

```bash
git add fep-converter/src/test/java/com/puchain/fep/converter/wire/OutboundWireShapeDispatcherTest.java
git commit -m "$(cat <<'EOF'
docs(converter): refresh OutboundWireShapeDispatcherTest Javadoc 31→37 msgNo inventory

- update header count "31 上行报文" → "37 上行报文" (sync with REGISTERED_MSG_NO_COUNT)
- extend P4-MSG-G inventory with P4-MSG-H (3115/3120) + P4-MSG-I (9000/9100/3113/9120)
- 0 production code change; 0 test assertion change

DEF-Q-NEW-5 (P4-MSG-I Quality deferred pool drain)

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

### Task 2 — 🚫 BLOCKED (v0.3) — DEF-Q-NEW-6 `Outbound9120AckEnvelopeBuilderTest` builder.build() 共享调用 `模式 A`

> **🚫 v0.3 BLOCKED**（muzhou 2026-05-28 签字决策 — 串行等待路径）:
> - **冲突**: `wt-r-new-1-real-xsd` 别会话 v0.3 Plan §范围内 #1 同文件，正在删 `@MockBean XsdValidator` + when stub
> - **冲突类型**: 同文件非重叠行（r-new-1 删 line 95-96 @MockBean + line 102 when stub；本 T2 改 line 109+115 build calls）。git auto-merge 理论可行但 muzhou 选 **串行等待** 避免风险
> - **处置**: T2 移到 r-new-1 ship 后的 **follow-up Plan**（届时 Outbound9120AckEnvelopeBuilderTest 无 @MockBean 注入，T2 是否仍需评估在新语境下；可能 single-build 重构更自然）
> - **本轮影响**: 本 Plan 实际 drain 3 项 (T1/T3/T4)，T2 计入 DEF-Q-NEW backlog "follow-up after r-new-1"
> - 红线 `feedback_plan_gate_must_handle_blocked_state` — 用 🚫 替换 [ ] 让下游 gate 跳过
> - 原 Step 1-5 实施细节保留在下方供 follow-up Plan 复用

**【以下原 Step 1-5 仅供 follow-up 时参考，本轮不执行】**



**追溯依据:** P4-MSG-I §5 deferred pool `DEF-Q-NEW-6 | LOW | T5 builder.build() 2 次调用合并（共享 builder instance）`

**验收标准:**
1. `build9120AckEnvelope_shouldSucceedAfterT1T2Registration` 内 `builder.build(entity, headFields)` 调用次数 2 → 1（line 109 + 115 → 1 次）
2. `assertThatCode(...).doesNotThrowAnyException()` 保留（语义：build 不抛 Exception 仍是断言之一）
3. 6 个 `.contains(...)` envelope 结构断言全部保留，使用共享 build 结果
4. 测试断言路径与 assertion message 0 改动
5. 既有 IT GREEN（Spring context cache 复用，无新 context 启动）

**Files:**
- Modify: `fep-web/src/test/java/com/puchain/fep/web/outbound/consumer/Outbound9120AckEnvelopeBuilderTest.java`

- [ ] **Step 1: grep 实测当前 build 调用 + assertion 结构**

```bash
cd /Users/muzhou/FEP_v1.0_wt-simplify-q-drain
sed -n '98,135p' fep-web/src/test/java/com/puchain/fep/web/outbound/consumer/Outbound9120AckEnvelopeBuilderTest.java
```
期望 (line 109 + line 115):
```
        assertThatCode(() -> builder.build(entity, headFields))
                .as("build 9120 ack 应成功（T1+T2 注册后无 OUTBOUND_5107/5108）...")
                .doesNotThrowAnyException();

        // 断言 2: envelope 结构含关键元素（装配段产物完整性）
        final String envelope = builder.build(entity, headFields).envelope();
```

- [ ] **Step 2: 重构为单次 build + 双语义断言**

将 line 98-134 既有 method body 改为下方实现。**等价性论证**: AssertJ `assertThatCode(ThrowingCallable).doesNotThrowAnyException()` 接受的是 `() -> void` callable，**无法**返回值；强行单次 build 须改用直接调用方式 — 任何 Throwable 直接传播给 JUnit → FAIL（语义等价 + JUnit stack trace 比 AssertJ 更详细）。在直接调用基础上加 `assertThat(envelope).isNotNull()` 补 "build 成功" 硬语义断言 + 保留原 `.as(...)` 回归基准说明。

```java
    @Test
    @DisplayName("build 9120 ack envelope — P0 闭合 2101 模式 6 ack 装配段缺口（registry+dispatcher 双查通过）")
    void build9120AckEnvelope_shouldSucceedAfterT1T2Registration() {
        // mock XsdValidator 返回 ok — 装配段行为聚焦，XSD 严格校验由 fep-processor 单测覆盖
        when(xsdValidator.validate(any(), any())).thenReturn(ValidationResult.ok());

        final OutboundMessageQueueEntity entity = givenAck9120Entity();
        final OutboundHeadFields headFields = new OutboundHeadFields(
                SEND_ORG_CODE, ENTRUST_DATE, TRANSITION_NO);

        // 单次 build — 不抛即继续走 envelope 结构断言（throw → JUnit 抛出测试 FAIL，等价
        // assertThatCode().doesNotThrowAnyException() 语义。assertion message 通过
        // method @DisplayName 显式说明回归基准角色）
        final String envelope = builder.build(entity, headFields).envelope();

        // 断言 1: envelope 必须存在（build 成功的 P0 闭合证明 — 不抛 5107/5108）
        assertThat(envelope)
                .as("build 9120 ack 应成功（T1+T2 注册后无 OUTBOUND_5107/5108）—— "
                        + "本 IT 在 T1+T2 之前会 FAIL，作为 2101 模式 6 ack 装配段缺口闭合的回归基准")
                .isNotNull();

        // 断言 2-7: envelope 结构含关键元素（装配段产物完整性，6 项保留 0 改动）
        assertThat(envelope)
                .as("9120 ack envelope 必含 BatchHead9120（dispatcher.describeFor headElementName）")
                .contains("<BatchHead9120");
        assertThat(envelope)
                .as("9120 ack envelope 必含 MsgReturn9120 body 元素（bodyClassRegistry.resolve 反序列化 + marshal）")
                .contains("<MsgReturn9120");
        assertThat(envelope)
                .as("9120 ack envelope 必含 OriMsgNo=2101（MsgReturn9120 业务体字段穿透 marshal）")
                .contains("<OriMsgNo>2101</OriMsgNo>");
        assertThat(envelope)
                .as("9120 ack envelope CommonHead 必含 MsgNo=9120（CommonHeadComposer.compose 注入 entity.messageType）")
                .contains("<MsgNo>9120</MsgNo>");
        assertThat(envelope)
                .as("9120 ack envelope BatchHead9120 必含 Result 占位（ResponseBusinessHead requiresResultCode=true）")
                .contains("<Result>00000</Result>");
        assertThat(envelope)
                .as("9120 ack envelope 必含 CFX 外层封装（marshalToString 完整 envelope 产物）")
                .contains("</CFX>");
    }
```

**等价性论证**:
- 原 `assertThatCode(() -> builder.build(...)).doesNotThrowAnyException()` 语义 = 调用不抛 Exception
- 重构后 `builder.build(...).envelope()` 直接 unboxed 调用 — **任何 Exception 直接传播给 JUnit → FAIL**（同等捕获，且 JUnit 自动 format stack trace 比 AssertJ 更详）
- 同时 `assertThat(envelope).isNotNull()` 补足"build 成功"硬语义断言
- 6 项 `.contains(...)` 全部保留，assertion message 全部保留
- AssertJ static import 已含 `assertThat` + `assertThatCode`（line 3-4），重构后 `assertThatCode` 不再使用，**implementer 务必 remove unused import** 否则 Checkstyle UnusedImports fail（**红线 `feedback_obsolete_negative_test_cleanup` 同源精神 — 删 negative case 时清扫 obsolete import**）

- [ ] **Step 3: 删除 `assertThatCode` unused import 防 Checkstyle**

```java
// 删除 line 4: import static org.assertj.core.api.Assertions.assertThatCode;
```

- [ ] **Step 4: 运行确认 GREEN + Checkstyle 通过**

```bash
cd /Users/muzhou/FEP_v1.0_wt-simplify-q-drain
JAVA_HOME=/usr/local/opt/openjdk@17 PATH=/usr/local/opt/openjdk@17/bin:$PATH ./mvnw test -pl fep-web -Dtest='Outbound9120AckEnvelopeBuilderTest' -Dsurefire.failIfNoSpecifiedTests=false --batch-mode --no-transfer-progress
JAVA_HOME=/usr/local/opt/openjdk@17 PATH=/usr/local/opt/openjdk@17/bin:$PATH ./mvnw checkstyle:check -pl fep-web --batch-mode --no-transfer-progress
```
- **Strong 验收**: `./mvnw test -pl fep-web -am` 全 GREEN
- **Minimum 验收**: `Outbound9120AckEnvelopeBuilderTest.build9120AckEnvelope_shouldSucceedAfterT1T2Registration` GREEN + `checkstyle:check` BUILD SUCCESS
- **mvn sandbox fallback**: GHA CI 兜底

- [ ] **Step 5: 提交**

```bash
git add fep-web/src/test/java/com/puchain/fep/web/outbound/consumer/Outbound9120AckEnvelopeBuilderTest.java
git commit -m "$(cat <<'EOF'
test(web): collapse builder.build() 2 calls into 1 in Outbound9120AckEnvelopeBuilderTest

- replace assertThatCode + envelope() 二次调用 → 单次 build().envelope() + assertNotNull
- 等价语义: Exception 直接传播给 JUnit (FAIL with stack trace) = assertThatCode().doesNotThrowAnyException()
- remove unused assertThatCode static import (Checkstyle UnusedImports compliance)
- 6 项 envelope contains 断言 0 改动

DEF-Q-NEW-6 (P4-MSG-I Quality deferred pool drain)

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

### Task 3 — DEF-Q-NEW-7 4 *XsdValidationTest per-class `wrap()` helper 抽取 `模式 A`

**追溯依据:** P4-MSG-I §5 deferred pool `DEF-Q-NEW-7 | LOW | T3 sample XML fixture 抽 helper 减少模板`

**验收标准:**
1. 4 个 XsdValidationTest 文件（9120/3113/9100/9000）各自添加 **private static** `wrap(...)` 或 `wrap(String, String)` 局部 helper（不在父类 `AbstractXsdValidationTest` 抽，因 4 子类各自固定不同 srcNode/desNode/app/msgNo 组合）
2. 每 helper 签名按 该 class 可变参数定制：
   - 9120/3113: `wrap(String msgIdSeq, String corrMsgIdSeq, String msgInnerXml)` — 3 args
   - 9100/9000: `wrap(String msgIdSeq, String msgInnerXml)` — 2 args（corrMsgId 全 0 固定）
3. 既有 9 个 `private static final String VALID_*/INVALID_*_XML = wrapCfxTemplate(...)` 字面量改为 `= wrap(...)`
4. body 字符串 textblock 0 改动（XML 内容字节 perfect 等价）
5. 全部 9 case 既有 @Test PASS，断言 0 改动

**Files:**
- Modify: `fep-processor/src/test/java/com/puchain/fep/processor/validation/MsgReturn9120XsdValidationTest.java`
- Modify: `fep-processor/src/test/java/com/puchain/fep/processor/validation/HxqyCreditAmt3113XsdValidationTest.java`
- Modify: `fep-processor/src/test/java/com/puchain/fep/processor/validation/Forward9100XsdValidationTest.java`
- Modify: `fep-processor/src/test/java/com/puchain/fep/processor/validation/Forward9000XsdValidationTest.java`

- [ ] **Step 1: grep 实测 4 文件 wrapCfxTemplate 调用模式**

```bash
cd /Users/muzhou/FEP_v1.0_wt-simplify-q-drain
for f in MsgReturn9120 HxqyCreditAmt3113 Forward9100 Forward9000; do
  echo "=== $f ==="
  grep -nE 'wrapCfxTemplate|VALID_|INVALID_' fep-processor/src/test/java/com/puchain/fep/processor/validation/${f}XsdValidationTest.java | head -20
done
```
期望:
- 9120: 2 wrapCfxTemplate 调用（msgId/corrMsgId 各自 seq 1/2）
- 3113: 3 wrapCfxTemplate 调用（seq 1/2/3）
- 9100: 2 调用，corrMsgId 全 "00000000000000000000"
- 9000: 2 调用，corrMsgId 全 "00000000000000000000"

- [ ] **Step 2: MsgReturn9120XsdValidationTest 重构**

加 helper（class body 顶部，在 `extends AbstractXsdValidationTest {` 后第一个 member）:

```java
    /**
     * 9120-specific CFX envelope wrapper — 固定 SrcNode=A1000143000104 (HNDEMP) →
     * DesNode=A1000142000001 (FEP), App=HNDEMP, MsgNo=9120, WorkDate=20260519.
     *
     * @param msgIdSeq 20-digit MsgId（caller 提供 seq 后缀，e.g. {@code "91200000000000000001"}）
     * @param corrMsgIdSeq 20-digit CorrMsgId
     * @param msgInnerXml MSG 内层 XML（{@code BatchHead9120} + {@code MsgReturn9120}）
     * @return 完整 CFX envelope
     */
    private static String wrap(String msgIdSeq, String corrMsgIdSeq, String msgInnerXml) {
        return wrapCfxTemplate(
                "A1000143000104", "A1000142000001", "HNDEMP", "9120",
                msgIdSeq, corrMsgIdSeq, "20260519",
                msgInnerXml);
    }
```

改 2 fixture：
```java
    private static final String VALID_FULL_FIELDS_XML = wrap(
            "91200000000000000001", "30000000000000000001", """
                <BatchHead9120>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260519</EntrustDate>
                  <TransitionNo>00000001</TransitionNo>
                  <Result>10000</Result>
                  <AddWord>处理成功</AddWord>
                </BatchHead9120>
                <MsgReturn9120>
                  <OriMsgNo>3000</OriMsgNo>
                  <Debug>processed at node A1000143000104</Debug>
                </MsgReturn9120>""");

    private static final String INVALID_MISSING_ORI_MSG_NO_XML = wrap(
            "91200000000000000002", "30000000000000000002", """
                <BatchHead9120>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260519</EntrustDate>
                  <TransitionNo>00000002</TransitionNo>
                  <Result>10000</Result>
                </BatchHead9120>
                <MsgReturn9120>
                  <Debug>missing OriMsgNo on purpose</Debug>
                </MsgReturn9120>""");
```

> **关键**: body textblock 字符串 0 改动，与 origin 完全等价。

- [ ] **Step 3: HxqyCreditAmt3113XsdValidationTest 重构**

加 helper（同 9120 模式）:
```java
    /**
     * 3113-specific CFX envelope wrapper — 固定 SrcNode=A1000143000104 (HNDEMP) →
     * DesNode=A1000142000001 (FEP), App=HNDEMP, MsgNo=3113, WorkDate=20260519.
     */
    private static String wrap(String msgIdSeq, String corrMsgIdSeq, String msgInnerXml) {
        return wrapCfxTemplate(
                "A1000143000104", "A1000142000001", "HNDEMP", "3113",
                msgIdSeq, corrMsgIdSeq, "20260519",
                msgInnerXml);
    }
```

改 3 fixture：`VALID_FULL_FIELDS_XML = wrap("31130000000000000001", "31120000000000000001", """ ... """);` 同 pattern 3 处替换（valid full / valid body omitted / invalid missing serialNo）。

- [ ] **Step 4: Forward9100XsdValidationTest 重构（corrMsgId 固定全 0，2-arg helper）**

加 helper:
```java
    /**
     * 9100-specific CFX envelope wrapper — 固定 SrcNode=A1000142000001 (FEP) →
     * DesNode=A1000143000104 (HNDEMP), App=FEPx, MsgNo=9100, WorkDate=20260519,
     * CorrMsgId 全 0（9100 是 FEP-initiated 转发，无 correlation）.
     */
    private static String wrap(String msgIdSeq, String msgInnerXml) {
        return wrapCfxTemplate(
                "A1000142000001", "A1000143000104", "FEPx", "9100",
                msgIdSeq, "00000000000000000000", "20260519",
                msgInnerXml);
    }
```
改 2 fixture：`VALID_FULL_FIELDS_XML = wrap("91000000000000000001", """ ... """);` 等。

- [ ] **Step 5: Forward9000XsdValidationTest 重构**

加 helper（同 9100 模式，仅 msgNo 改 "9000"）:
```java
    private static String wrap(String msgIdSeq, String msgInnerXml) {
        return wrapCfxTemplate(
                "A1000142000001", "A1000143000104", "FEPx", "9000",
                msgIdSeq, "00000000000000000000", "20260519",
                msgInnerXml);
    }
```
改 2 fixture 同 9100 pattern。

- [ ] **Step 6: 运行确认 9 case 全 GREEN + Checkstyle 通过**

```bash
cd /Users/muzhou/FEP_v1.0_wt-simplify-q-drain
JAVA_HOME=/usr/local/opt/openjdk@17 PATH=/usr/local/opt/openjdk@17/bin:$PATH ./mvnw test -pl fep-processor -Dtest='MsgReturn9120XsdValidationTest,HxqyCreditAmt3113XsdValidationTest,Forward9100XsdValidationTest,Forward9000XsdValidationTest' -Dsurefire.failIfNoSpecifiedTests=false --batch-mode --no-transfer-progress
JAVA_HOME=/usr/local/opt/openjdk@17 PATH=/usr/local/opt/openjdk@17/bin:$PATH ./mvnw checkstyle:check -pl fep-processor --batch-mode --no-transfer-progress
```
- **Strong 验收**: `./mvnw test -pl fep-processor` 全 GREEN
- **Minimum 验收**: 4 类 9 case all PASS（9120×2 + 3113×3 + 9100×2 + 9000×2）+ checkstyle BUILD SUCCESS
- **mvn sandbox fallback**: GHA CI 兜底

- [ ] **Step 7: xmllint 字节等价性 spot-check（红线 `feedback_fixture_data_must_satisfy_xsd_constraints` 同源精神）**

```bash
cd /Users/muzhou/FEP_v1.0_wt-simplify-q-drain
# 写一小段 Java 验证脚本生成 wrap("seq", "corr", "body") vs wrapCfxTemplate(...) 字节级等价
# 简化：refactor 后跑 test，既然 SHARED_VALIDATOR 对每个 XML 跑全 XSD validation，PASS = 字节等价（XSD 容忍 whitespace 但不容忍 substantive 字符差异 — 测试本身就是字节级等价证明）
echo "字节等价性由 fep-processor test PASS 间接证明（任何 wrap → wrapCfxTemplate 字节差异会致 XSD 校验失败或 contains 断言失败）"
```

- [ ] **Step 8: 提交**

```bash
git add fep-processor/src/test/java/com/puchain/fep/processor/validation/MsgReturn9120XsdValidationTest.java
git add fep-processor/src/test/java/com/puchain/fep/processor/validation/HxqyCreditAmt3113XsdValidationTest.java
git add fep-processor/src/test/java/com/puchain/fep/processor/validation/Forward9100XsdValidationTest.java
git add fep-processor/src/test/java/com/puchain/fep/processor/validation/Forward9000XsdValidationTest.java
git commit -m "$(cat <<'EOF'
test(processor): extract per-class wrap() helpers in 4 batch4 XsdValidationTests

- MsgReturn9120/HxqyCreditAmt3113: wrap(msgIdSeq, corrMsgIdSeq, body) — 3 args
- Forward9100/Forward9000: wrap(msgIdSeq, body) — 2 args (corrMsgId 全 0 固定)
- 9 fixture (2+3+2+2) call sites 8-arg wrapCfxTemplate → 2/3-arg wrap
- body textblock 字符串 0 改动；XSD validation pass = 字节等价证明

DEF-Q-NEW-7 (P4-MSG-I Quality deferred pool drain)

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

### Task 4 — DEF-Q-NEW-4 `OutboundCommonForwardWireTest` import order 决策点 `模式 A`

**追溯依据:** P4-MSG-I §5 deferred pool `DEF-Q-NEW-4 | LOW | T4 import order checkstyle nit`

**Plan 备注 — 决策点**: muzhou 签字时选 (A) skip 或 (B) Java-convention 重排，Plan 提供两套 Step。

**📍 实测背景（已 grep 落地，T4 Step 1 实测过）**:
- `checkstyle.xml` **无 ImportOrder rule**（仅 `AvoidStarImport / UnusedImports / RedundantImport`）— "nit" 非 checkstyle 强制
- sibling `Outbound1101WireTest.java` (P4-MSG-D) + `AbstractOutboundWireMatrixTest.java` (DEF-Reuse-R1) 均 `com.* → java.* → org.*` **同序**
- 修 `OutboundCommonForwardWireTest` 单文件改 java-first 会与 8 个 sibling 不一致

**验收标准（条件分支，按 muzhou 签字结果）:**

**(A) Skip with rationale**:
1. 不修代码，0 改动
2. 在 §备注 + Daily Report § skip 决策记录："checkstyle 不强制 + sibling 模式一致，无改动价值；保留 sibling 一致性优先"
3. DEF-Q-NEW-4 在 Daily Report Simplify deferred pool 标 "降级 — skipped per signature decision"

**(B) Java-convention 重排**:
1. `OutboundCommonForwardWireTest.java` imports 重排为 `java.* → com.* → org.*`（IntelliJ default 风格 + JDK convention）
2. 各 import 组之间空行分隔（如其他重排约定）
3. 既有 IT 4 ParameterizedTest case GREEN
4. **必须同 commit 重排 9 sibling**（实测 `ls` 命中 — 注意 `SupplyChain` vs `Supplychain` 历史 casing 不一致，复制时**逐字符核对**）:
   - `Outbound1101WireTest.java`
   - `OutboundBatchWireTest.java`
   - `OutboundEnterpriseQueryRealtimeWireTest.java`
   - `OutboundSupplyChainWireTest.java`（**capital C** — 实测命名）
   - `OutboundSupplychainBatch3WireTest.java`（lowercase c）
   - `OutboundSupplychainQueryWireTest.java`（lowercase c）
   - `OutboundSupplychainQueryBatch2WireTest.java`（lowercase c）
   - `AbstractOutboundWireMatrixTest.java`
   - `Outbound9120AckEnvelopeBuilderTest.java`
   
   维持一致性，否则破坏 sibling pattern（红线 `feedback_cross_task_obsolete_fixture_assumption_when_set_extended` 同源精神 — 加 entry 时同 commit 扫除腐烂引用）

**Files:**
- **(A) Skip**: 0 file modified
- **(B) Reorder**: 1 target + 9 sibling = **10 files** in `fep-web/src/test/java/com/puchain/fep/web/outbound/consumer/`

#### 路径 (A) — Skip with rationale

- [ ] **Step 1: 记录 skip 决策到 Daily Report**（T5 §closing 中嵌入）
- [ ] **Step 2: 0 git commit**（无代码改动）
- [ ] **Step 3: §备注 中明示 muzhou 签字结果 = (A)，引用本 §决策点 论证段**

#### 路径 (B) — Java-convention 重排 + 9 sibling 同步

> **警示**: 路径 (B) Scope 大于 Task 4 标的范围（原 deferred 仅指 OutboundCommonForwardWireTest 单文件），扩展到 9 sibling 触发红线 `feedback_batch_scope_creep`。如选 (B)，muzhou 须显式批准 scope 扩张到 9 sibling。

- [ ] **Step B1: grep 实测 9 sibling 文件路径 + 当前 import order**（10 files 总计：target + 9 siblings）

```bash
cd /Users/muzhou/FEP_v1.0_wt-simplify-q-drain
ls fep-web/src/test/java/com/puchain/fep/web/outbound/consumer/Outbound*WireTest.java
ls fep-web/src/test/java/com/puchain/fep/web/outbound/consumer/Abstract*.java
ls fep-web/src/test/java/com/puchain/fep/web/outbound/consumer/Outbound9120Ack*Test.java
# 显示每文件 import 块（前 15 行）
for f in $(ls fep-web/src/test/java/com/puchain/fep/web/outbound/consumer/Outbound*WireTest.java fep-web/src/test/java/com/puchain/fep/web/outbound/consumer/AbstractOutboundWireMatrixTest.java fep-web/src/test/java/com/puchain/fep/web/outbound/consumer/Outbound9120AckEnvelopeBuilderTest.java); do
  echo "=== $f ==="
  sed -n '1,18p' "$f"
done
```

- [ ] **Step B2-B10: 逐文件（10 files）按 IntelliJ default 重排（java → javax → com → org，组内字母序，组间空行分隔）**

每文件 import 块重排为标准 IntelliJ Java import layout。Body 0 改动。

- [ ] **Step B11: 全 fep-web test 回归**

```bash
cd /Users/muzhou/FEP_v1.0_wt-simplify-q-drain
JAVA_HOME=/usr/local/opt/openjdk@17 PATH=/usr/local/opt/openjdk@17/bin:$PATH ./mvnw test -pl fep-web --batch-mode --no-transfer-progress
JAVA_HOME=/usr/local/opt/openjdk@17 PATH=/usr/local/opt/openjdk@17/bin:$PATH ./mvnw checkstyle:check -pl fep-web --batch-mode --no-transfer-progress
```

- [ ] **Step B12: 提交（含 9 sibling + 1 target = 10 files）**

```bash
git add fep-web/src/test/java/com/puchain/fep/web/outbound/consumer/
git commit -m "$(cat <<'EOF'
style(web): reorder imports to Java-convention in 10 outbound wire test files

- target OutboundCommonForwardWireTest + 9 sibling (Outbound{1101,Batch,
  EnterpriseQueryRealtime,SupplyChain,SupplychainBatch3,SupplychainQuery,
  SupplychainQueryBatch2}WireTest + AbstractOutboundWireMatrixTest +
  Outbound9120AckEnvelopeBuilderTest)
- new order: java.* → javax.* → com.* → org.* (IntelliJ default)
- 0 code change; 0 test assertion change; checkstyle 不强制此规则但 sibling 一致性受益

DEF-Q-NEW-4 (P4-MSG-I Quality deferred pool drain, signature path B)

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

### Task 5 — closing（session-end Phase 2 ① Simplify + ② 9-维文档 + ③ Daily Report + ④ git push） `模式 A`

> 红线 `feedback_mandatory_post_task` + `feedback_four_step_closing` — 四步收尾挪到 session-end 统一做，本 Plan 闭环以 session-end skill 触发为准。本 Task 仅 grep 收尾前置检查。

- [ ] **Step 1: Step 0 重测**（红线 `feedback_parallel_session_task_allocation_discipline` 第④时点 + `feedback_baseline_drift_during_long_review_cycle`）

```bash
cd /Users/muzhou/FEP_v1.0_wt-simplify-q-drain
git status --short
git log --oneline origin/main..HEAD
git worktree list
# 重测 origin/main HEAD
git fetch origin main 2>&1 | tail -3
git rev-parse origin/main
```
期望: HEAD 含 T1+T2+T3+T4 commits（按签字 path），origin/main 仍 `457a5e4`（或别会话 fast-forward 后的新 SHA — drift 检测交 session-end 处理）。

- [ ] **Step 2: 触发 `/session-end`**（合规检查 + Phase 2 ① Simplify 三审 / ② 9-维文档 / ③ Daily Report + PDF / ④ git push + ⑤ next-session-prompt + ⑥ PRD 矩阵 — 本 Plan 元流程 0 PRD 业务变更，矩阵 0 改动）

session-end Phase 2 委托做：
- **① Simplify 三审**: 主对话 dispatch reuse / quality / efficiency 3 agent 并行（红线 `feedback_simplify_implementer_self_review_no_subagent`） — applied + deferred 分类
- **② 9-维技术文档**: 写到 `/Users/muzhou/FEP/docs/technical/{api,modules,database,config,security,deployment,acceptance,testing,communication}/2026-05-28-simplify-q-drain-p4-msg-i-*.md`（**file write only，红线 `feedback_fep_docs_repo_commit_taboo`**）— muzhou v0.3 签字 = **full 9-dim per `feedback_infra_plan_still_needs_full_8dim_docs`**（本 Plan 2 code commit T1+T3 → 全 9 维强制）
- **③ Daily Report**: 写到 `/Users/muzhou/FEP/docs/daily_reports/2026-05-28-simplify-q-drain-p4-msg-i-progress-report.md` + PDF；含 §教训 + §T2 BLOCKED 与 r-new-1 串行决策记录 + §T4 (A) Skip 决策记录 + §Simplify deferred pool 残余 8 项明示
- **④ git push + PR 路径** (muzhou 签字 = P): `git push -u origin chore/simplify-q-drain-p4-msg-i` → `gh pr create` → 等 GHA CI 绿 → squash merge；GHA 账单仍阻塞时依红线 `feedback_systemic_ci_blocker_defers_positive_backing` tier-A 充分即 CLOSED
- **⑤ next-session-prompt**: 写到 `/Users/muzhou/FEP/docs/daily_reports/2026-05-28-simplify-q-drain-p4-msg-i-next-session-prompt.md`；含残余 deferred pool（P4-MSG-I 8 余项含 T2 follow-up / DEF-Reuse-R1 14 项未动 / EFF-D1 HIGH ROI 待独立 Plan / PR #27 仍待 merge / r-new-1 进度 / asyncpipeline 进度）
- **⑥ PRD 矩阵**: 无变更（元流程）

- [ ] **Step 3: Worktree 清理**（红线 `feedback_worktree_for_parallel_work` 闭环纪律）

```bash
# 待 push 完成 + muzhou 确认 ship 后
cd /Users/muzhou/FEP_v1.0
git worktree remove ../FEP_v1.0_wt-simplify-q-drain
git branch -d chore/simplify-q-drain-p4-msg-i  # 若已 merge 到 main 则 delete；否则 -D
```

- [ ] **Step 4: CLAUDE.md 更新**

session-end skill Phase 2 ⑥ 中：
- 顶部 metadata 日期 → 2026-05-28
- "最近里程碑" 顶部加 "2026-05-28 Simplify Q drain P4-MSG-I batch v0.3（DEF-Q-NEW-4 skip + DEF-Q-NEW-5/7 ship + DEF-Q-NEW-6 BLOCKED→follow-up） — `<merge SHA>`"
- "下一步候选 #6 Simplify deferred ticket pool" 修订：P4-MSG-I **原 12 项 = DEF-Reuse-R1（已 ship 2026-05-21 → drain 后 11 余项）**；本 Plan v0.3 实际 drain 3 项 (T1+T3+T4=skip) 后剩 **8 项**（DEF-Q-NEW-6 follow-up + DEF-Reuse-R2/R3 + DEF-Q-NEW-1/2/3 + EFF-D1 + EFF-D4）
- 加 follow-up ticket 记录: "DEF-Q-NEW-6 follow-up — 等 r-new-1 ship Outbound9120AckEnvelopeBuilderTest 删 @MockBean XsdValidator 后重启评估 T2 single-build 重构"

---

## §3 自检清单

### 1. PRD 覆盖度 — 不适用
本 Plan 元流程/refactor，0 PRD 业务功能变更，0 新增 FR-ID。明示在 §头部 "PRD 追溯" 章节。

### 2. 安全边界检查 — 通过
全 4 Task 0 触动 SM2/SM3/SM4/密钥/脱敏/审计日志完整性。0 `security/impl/` 文件。0 ⛔ 模式 E Task。

### 3. 占位符扫描 — 通过
全 Plan 0 "TBD/TODO/待补/类似 Task N/参考 Task" 占位符。Task 4 决策点 (A)/(B) 双路径完整可执行（不是 "TBD"）。

### 4. 类型一致性 — 通过
T1 修改 Javadoc 0 触动 27 @Test method 名；T2 单 method body 改 0 影响其他 method；T3 4 file private static helper 各自封闭；T4 imports 改 0 影响 class body。

### 5. 测试命令可执行 — 已 dry-run grep
所有 `./mvnw test -pl <module> -Dtest='<ClassName>'` 已 grep 确认 ClassName 存在 + `-Dsurefire.failIfNoSpecifiedTests=false` 兼容 surefire 3.x 参数（红线 `feedback_surefire3_failifno_specified_tests_param_rename`）。

### 6. CLAUDE.md 更新 — T5 Step 4 覆盖
session-end skill Phase 2 ⑥ 中执行。

### 7. 验收标准完整性 — 通过
T1-T3 验收标准从 deferred pool 原 finding 推导（不是从代码 reverse-engineer）；T4 双路径分别独立验收标准。所有断言"既有 GREEN"用 mvn test 验证。

### 8. 共享工具类无遗漏 — 通过
T3 helper 不抽到父类（rationale 已在 §1 共享工具类清单 + Task 验收标准 1 明示）。

### 9. 核心类职责边界 — 不适用
Plan 0 触动 production class。

### 10. Worktree 触发条件自检 — 命中并已建 worktree
已建 `/Users/muzhou/FEP_v1.0_wt-simplify-q-drain` (`chore/simplify-q-drain-p4-msg-i` from origin/main `457a5e4`)，T5 Step 3 含 `git worktree remove` 实测命令。

---

## §4 红线适用清单

本 Plan 起草/执行过程预期触发或主动遵循的红线（按时序）:

| 红线 | 触发点 | 处置 |
|---|---|---|
| `feedback_plan_must_grep_actual_api` | Plan 起草前 | ✅ 已 grep 实测 6 file + checkstyle.xml + AssertJ API |
| `feedback_worktree_for_parallel_work` + `feedback_worktree_isolates_fs_not_logic_domain` | Plan 起草前 worktree 决策 | ✅ 已建独立 worktree |
| `feedback_parallel_session_task_allocation_discipline` Step 0 | Plan 起草前/T0/T4 4 时点 | ✅ T0/T1/T2/T3 主对话起草前 grep； T5 Step 1 重测；下次 reviewer 派发前再测 |
| `feedback_mvn_sandbox_exit144_pattern` | mvn 命令 fallback | Plan 各 Step 4-6/7 显式 "mvn sandbox fallback: GHA CI 兜底" |
| `feedback_surefire3_failifno_specified_tests_param_rename` | -Dtest= 参数 | ✅ 所有 mvn test 命令含 `-Dsurefire.failIfNoSpecifiedTests=false` |
| `feedback_plan_regression_scope_explicit` | 验收 strong/minimum 二分 | ✅ T1-T3 全有 strong+minimum 双层验收 |
| `feedback_full_regression_before_commit` | 每 Task commit 前 | Step 4-6 含 module 级 mvn test + checkstyle:check |
| `feedback_simplify_implementer_self_review_no_subagent` | T5 Simplify 三审 | T5 Step 2 明示主对话 dispatch 3 agent |
| `feedback_fep_docs_repo_commit_taboo` | T5 9-维文档 | T5 Step 2 明示"file write only" |
| `feedback_workflow_truncation_disguised_as_recommended` | Plan 决策点 | T4 (A) skip 选项不标 Recommended，让 muzhou 自决 (A) vs (B) |
| `feedback_obsolete_negative_test_cleanup` | T2 删 assertThatCode import | T2 Step 3 显式 import 清理 step |
| `feedback_subagent_must_commit_before_exit` + `feedback_subagent_no_background_bash_in_workflow` + `feedback_subagent_meta_comment_no_tool_use` + `feedback_sendmessage_tool_unavailable_equals_dual_fail` + `feedback_subagent_model_override_auth_fragility` | T1-T4 implementer subagent dispatch | implementer prompt 强制：no `model:` override / no run_in_background bash / Status 起头 / commit-before-exit / dual-fail 主对话接管 |
| `feedback_baseline_drift_during_long_review_cycle` | reviewer 派发前/签字时/T0/T4 时点 | T5 Step 1 重测 |

---

## §5 PR vs Direct Merge 决策

本 Plan 因主 worktree 已有 untracked Plans 与 PR #27 等待状态，建议路径:

**(默认推荐) 路径 P — PR 路径**:
- T5 Step 2 ④ `git push -u origin chore/simplify-q-drain-p4-msg-i` → `gh pr create`
- PR 等 GHA CI 绿（如 PR #27 账单同源阻塞 → 依红线 `feedback_systemic_ci_blocker_defers_positive_backing` tier-A 充分即 CLOSED，但本 Plan tier-A 仅本机 mvn 全绿）
- muzhou squash merge

**路径 M — Direct fast-forward merge**:
- `cd /Users/muzhou/FEP_v1.0 && git fetch && git merge --ff-only origin/main && git push`（在主 worktree 操作）
- 跳过 PR review/CI gate（适合纯重构 0 风险变更）
- 风险: bypasses GHA quality gate

muzhou 签字时拍板 P vs M。

---

## §6 备注与签字区

### 备注

**关于 DEF-Q-NEW-4 路径决策**（重要）:

DEF-Q-NEW-4 是本 batch 4 项 deferred 中**唯一争议点**。LOW 优先级标签准确反映其边缘性。Plan 起草中 grep 实测发现两个反证：
1. `checkstyle.xml` 无 ImportOrder 强制规则 → 改与不改 0 CI 影响
2. 8 个 sibling test file 全部 `com.* → java.* → org.*` 同序 → 单文件改 java-first 破坏 codebase 一致性

签字时建议 muzhou 在 (A) Skip / (B) 8 sibling 同步重排 / (C) 其他（如 deferred 转移到独立 ticket pool）三选一拍板。Plan 起草者倾向 (A) — 论证：低优先级 LOW + 无 CI 约束 + sibling 一致性优先 + 80/20 原则 drain 优先 high-value 项（DEF-Q-NEW-5/6/7 +DEF-Reuse-R2 etc.）。

**关于本 batch deferred drain ROI**:

| Task | 价值 | 风险 | 估时 | drain 后剩余 P4-MSG-I deferred | v0.3 状态 |
|---|---|---|---|---|---|
| T1 DEF-Q-NEW-5 | 文档准确性 — Javadoc 计数与代码同步 | ~0 | 5-10min | -1 | ✅ ACTIVE |
| T2 DEF-Q-NEW-6 | 测试 API 优化 — 减少 2x context 启动 | 极低 | 10-15min | (defer) | 🚫 BLOCKED → follow-up |
| T3 DEF-Q-NEW-7 | 模板复用 — 4 file 减重 ~30 行 | 极低 | 30-45min | -1 | ✅ ACTIVE |
| T4 DEF-Q-NEW-4 | (A) Skip — sibling 一致性 + checkstyle 不强制 | 0 | 0min | -1 (signed off) | ✅ ACTIVE (A) |
| **合计** v0.3 | | | **35-55min** | **3 项 + 1 deferred** | |

v0.3 drain 后 P4-MSG-I 11 deferred 剩 **8 项**: DEF-Q-NEW-6 (T2 deferred to follow-up after r-new-1) + DEF-Reuse-R2 (LOW) + DEF-Reuse-R3 (LOW-MED) + DEF-Q-NEW-1 (MED) + DEF-Q-NEW-2 (MED) + DEF-Q-NEW-3 (LOW-MED) + EFF-D1 (HIGH) + EFF-D4 (MED)。DEF-Reuse-R1 已 ship。

### 签字区

**[Plan 评审签字 v0.1 → v0.2 → v0.3 修订记录]**:

**v0.1 → v0.2** (2026-05-27):
- 评审 agentId v0.1: `aecd5d74a8542560b` (general-purpose subagent, no model override per `feedback_subagent_model_override_auth_fragility`)
- v0.1 结论: **NEEDS REVISION** — 4 issue（1 MAJOR + 1 MINOR + 2 NIT）
- 修订摘要:
  - **MAJOR** Issue #1 (T4 sibling enumeration) — 实测 `ls` 9 sibling + capital C 标注
  - **MINOR** Issue #2 (T2 retracted draft) — 删除错误尝试，保留精炼等价性论证
  - **NIT** Issue #3 (red-line dedup) — `feedback_workflow_truncation_disguised_as_recommended` 单行
  - **NIT** Issue #4 (11/12 算术) — "原 12 项 − DEF-Reuse-R1 ship − 4 drain = 7 余"

**v0.2 → v0.3** (2026-05-28):
- 评审 agentId v0.2 (re-review): `a8010da6b9c2928a6`
- v0.2 结论: **PASS** — 4 v0.1 issue 全部 FIXED + 0 新 issue
- v0.3 修订触发: 2026-05-27 → 2026-05-28 跨日 baseline drift 实测发现:
  - drift: origin/main `457a5e4` → `df15613`（pitest-maven 1.25.0 升级别会话 ship，仅触 pom.xml）
  - 别会话新 worktree: `wt-r-new-1-real-xsd` (refactor/r-new-1) + `wt-asyncpipeline-flake`
  - **file-level 冲突**: r-new-1 v0.3 §范围内 touches `Outbound9120AckEnvelopeBuilderTest.java`（本 Plan T2 同文件）
- v0.3 修订内容（muzhou 决策驱动）:
  1. **新增 Task 0** — baseline rebase `457a5e4 → df15613` + 锁定文件重测（red lines `feedback_baseline_drift_during_long_review_cycle` + `feedback_parallel_session_task_allocation_discipline` T0 时点）
  2. **T2 🚫 BLOCKED** — 串行等待 r-new-1 ship 后 follow-up Plan 处理（避免同文件 race）
  3. **本轮 drain 4 项 → 3 项** (T1/T3/T4)；T2 计入 DEF-Q-NEW backlog "follow-up after r-new-1"
  4. **并排除项** 头部声明扩展含 r-new-1 + asyncpipeline + callback 三别会话锁定文件

**[人工 Plan Approver 签字 — by muzhou]**:
- 批准日期: **2026-05-28**
- v0.2 + v0.3 (T2 BLOCKED + Task 0 rebase) 一并批准
- **T4 决策路径**: **(A) Skip** with rationale（checkstyle 不强制 + 9 sibling 同序保持一致）— 决策时间 2026-05-27
- **PR/Direct merge**: **(P) PR 路径 + 等 GHA 绿**（默认推荐，GHA 账单阻塞时依红线 `feedback_systemic_ci_blocker_defers_positive_backing` tier-A 充分即 CLOSED）— 决策时间 2026-05-27
- **Simplify 范围**: **full 9-dim per `feedback_infra_plan_still_needs_full_8dim_docs`**（本 Plan 有 2 code commit T1+T3，T4=skip 0 commit；≥1 code commit → 全 9 维强制）— 决策时间 2026-05-27
- **T2 冲突 + baseline drift 处置**: **串行等待 r-new-1 ship + rebase onto df15613** — 决策时间 2026-05-28
- 备注: 7 决策项均经 AskUserQuestion 走 muzhou 签字；T2 BLOCKED 推迟 ≠ 永久丢弃，r-new-1 ship 后 follow-up Plan 重启评估

---

> **生成方式**: writing-plans (FEP 定制版) skill / Plan 起草者 = Claude Code mode A / 起草日期 2026-05-27 / baseline origin/main `457a5e4`
