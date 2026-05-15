# FEP P4-MSG-F Deferred Drain (Parallel) 实施计划

> **版本:** v0.3（2026-05-15 签字后实施前重测发现 T1 DEF-Q1 被另一会话 `wt-simplify-d1-rename` 认领 → muzhou 决策撤 T1，改 **T2+T3 双 ticket 并行**；v0.2 经 AI R1 NOT CLEARED→修订→R2 PASS→muzhou 签字）
>
> **执行方式:** 3 ticket 文件互不重叠可**真并行** — 主对话创建 3 独立 worktree 后单消息派发 3 implementer subagent，各自 commit 在自己 worktree，主对话 merge 3 branch → main → push → cleanup。

**目标:** 消化 2026-05-14 末段¹⁰ P4-MSG-F Simplify 三审 4 项 deferred（DEF-Q1 / DEF-R2 / DEF-Q2 / DEF-R1），改善 fep-web test 命名 future-proof + fep-converter Dispatcher 4 类目常量结构化 + fep-processor test CFX envelope 重复消除（6 子类 992 行 → 目标 ≤750，净消减 ≥240）。

**前置依赖:** 无（独立 Quality polish + 测试基础设施 refactor，无业务逻辑改动 / 无 PRD FR 状态变更）

**执行 Worktree:** 3 个独立 worktree（触发条件 ② "与已签字未执行的 Plan 并存"）
- ~~`wt-deferred-q1`（T1 DEF-Q1）~~ **v0.3 撤销** — 实施前重测发现 `wt-simplify-d1-rename [chore/simplify-d1-registry-test-rename]` 已被另一会话认领同一 DEF-Q1（BodyClassRegistryTest rename），红线 `feedback_parallel_session_task_allocation_discipline` + `feedback_new_session_no_intervention_in_prior_tasks` → 不重复不接手，worktree+branch 已删
- `/Users/muzhou/FEP_v1.0_wt-deferred-r2q2`（分支 `chore/deferred-r2q2`，T2 DEF-R2+Q2 合并）
- `/Users/muzhou/FEP_v1.0_wt-deferred-r1`（分支 `chore/deferred-r1`，T3 DEF-R1）

> 红线 `feedback_worktree_for_parallel_work` 触发条件命中 ②（SB 3.5.9 升级 Plan v2 已签字未执行，独立 worktree 防止主 branch HEAD 在本 Plan 实施期间被 SB 升级 commit 移位 + 并发 commit race condition）。

**并排除项**（红线 `feedback_parallel_session_task_allocation_discipline` 强制声明）:

- **Spring Boot 3.3.7 → 3.5.9 升级 Plan v2**（`docs/plans/2026-05-15-spring-boot-parent-upgrade-3.5.9.md`，已签字未执行，目标 worktree `wt-sb35-upgrade`）锁定范围：pom.xml 全 8 模块 BOM + 全 reactor verify (10-15 min) + Spring/Tomcat/Hibernate/log4j/Jackson 版本相关代码 + Spring Security 6 lambda DSL + spring-boot-properties-migrator 临时 runtime 依赖
- **e2e 远古 worktree**（`/Users/muzhou/FEP_v1.0.e2e` 分支 `e2e/p7.1-smoke-local`）锁定范围：fep-admin-ui 前端 / Playwright e2e — 与本 Plan 物理隔离（fep-admin-ui 不在本 Plan 改动列表）

**架构:** 3 Task 文件路径互不重叠 → 主对话单消息派发 3 implementer subagent 真并行 → 各 subagent 在自己 worktree commit → 主对话集中 merge 3 branch → main → push origin → cleanup 3 worktree。每 Task 后独立派发 spec + quality reviewer subagent（红线 `feedback_task_review_discipline`）。

**技术栈:** Java 17 / Spring Boot 3.3.7 / Maven / JUnit 5 / AssertJ

**AI 协同模式:**

| 模式 | AI 占比 | 本计划中的应用 |
|:----:|:-------:|--------------|
| A | 90% | T1 测试方法重命名 + @DisplayName 调整（无业务逻辑） |
| B | 70% | T2 4 Set 常量提炼 + 行为等价 refactor / T3 CFX helper 抽取 + 6 测试类重构 |
| C | 60% | 不涉及 |
| D | 50% | 不涉及 |
| E | 0%  | ⛔ 无国密代码改动 |

---

## 设计背景

### Why now（动因）

P4-MSG-F 闭环 (2026-05-14) Simplify 三审产出 4 项 deferred，独立 ticket 候选已在 `docs/daily_reports/2026-05-14-p4-msg-f-supplychain-query-batch1-progress-report.md` §未实施项 / `docs/daily_reports/2026-05-14-p4-msg-f-next-session-prompt.md` §上轮遗留事项 登记。当前会话 (2026-05-15) 已完成 R1 Shared Registry Residual Refactor（18f43e1 单 commit），main worktree clean，origin/main = main = `18f43e1`，0 ahead / 0 behind，是独立 deferred 消化的好窗口。

### What changes（修改范围）

**3 ticket / 3 文件 / 3 模块**（互不重叠）:

| Ticket | 文件路径 | 模块 | 变更类型 | 行数预估 |
|---|---|---|---|---|
| DEF-Q1 | `fep-web/src/test/java/com/puchain/fep/web/outbound/consumer/BodyClassRegistryTest.java` | fep-web/test | 方法重命名 + @DisplayName 添加 | +1 / -0 / mod 1 |
| DEF-R2+Q2（合并）| `fep-converter/src/main/java/com/puchain/fep/converter/wire/OutboundWireShapeDispatcher.java` | fep-converter/main | 4 Set 常量提炼 + switch 转 if-else + Javadoc 硬编码消除 | ~+25 / -20 / refactor 50 |
| DEF-R1 | `fep-processor/src/test/java/com/puchain/fep/processor/validation/AbstractXsdValidationTest.java` + 6 个 *XsdValidationTest（ProgressQuery3001 / ProgressQueryReturn3002 / PzInfoQuery3003 / PzInfoReturn3004 / QyAccQuery3005 / QyAccQueryReturn3006）| fep-processor/test | helper 方法抽取 + 6 子类调用重构 | helper +~30 / 6 子类实测 992 → 目标 ≤750（净 ≥240）|

### 并排除项 grep 实测交集（红线 `feedback_parallel_session_task_allocation_discipline` Step 2 规则 1 验证）

SB 3.5.9 升级 Plan 锁定文件 ∩ 本 Plan 改动文件 = ∅:

- SB 升级修改 `pom.xml`（根 + 8 模块 pom.xml）
- 本 Plan 修改：1 test file (fep-web) + 1 main file (fep-converter) + 1 test base class + 6 test files (fep-processor)
- 交集：无（pom.xml 与 .java 文件路径完全分离）

SB 升级 BOM 变更不会影响：
- DEF-Q1：方法重命名仅文字层
- DEF-R2+Q2：使用 Java 17 内置 `Set.of()` + `switch` 表达式，BOM 升级不影响
- DEF-R1：JUnit 5 + AssertJ，Spring Boot 3.5.9 仍兼容（JUnit 5.x line 稳定 4 年）

---

## 文件结构

| 文件路径 | 职责 | 操作 | AI 模式 |
|----------|------|------|:-------:|
| `fep-web/src/test/java/com/puchain/fep/web/outbound/consumer/BodyClassRegistryTest.java` | 注册表测试 | 修改 | A |
| `fep-converter/src/main/java/com/puchain/fep/converter/wire/OutboundWireShapeDispatcher.java` | wire-shape 分派 | 修改 | B |
| `fep-processor/src/test/java/com/puchain/fep/processor/validation/AbstractXsdValidationTest.java` | XSD 验证测试基类 | 修改 | B |
| `fep-processor/src/test/java/com/puchain/fep/processor/validation/ProgressQuery3001XsdValidationTest.java` | 3001 XSD 验证 | 修改 | B |
| `fep-processor/src/test/java/com/puchain/fep/processor/validation/ProgressQueryReturn3002XsdValidationTest.java` | 3002 XSD 验证 | 修改 | B |
| `fep-processor/src/test/java/com/puchain/fep/processor/validation/PzInfoQuery3003XsdValidationTest.java` | 3003 XSD 验证 | 修改 | B |
| `fep-processor/src/test/java/com/puchain/fep/processor/validation/PzInfoReturn3004XsdValidationTest.java` | 3004 XSD 验证 | 修改 | B |
| `fep-processor/src/test/java/com/puchain/fep/processor/validation/QyAccQuery3005XsdValidationTest.java` | 3005 XSD 验证 | 修改 | B |
| `fep-processor/src/test/java/com/puchain/fep/processor/validation/QyAccQueryReturn3006XsdValidationTest.java` | 3006 XSD 验证 | 修改 | B |

### 共享工具类清单

| 工具类 | 包 | 关键方法 | 提供者 Task | 消费者 Task |
|---|---|---|---|---|
| `AbstractXsdValidationTest.wrapCfx(...)` | processor.validation | 包装 CFX `<HEAD>`(8字段)+`<MSG>` envelope（无 namespace；srcNode/desNode/msgNo/msgId/corrMsgId + msgInnerXml）| T3 | T3 内 6 测试子类（同 Task 完成）|

### 核心类职责边界声明

#### OutboundWireShapeDispatcher 职责边界

**负责**: 根据 msgNo 4 位数字字符串分派对应 wire-shape descriptor (RealHead/BatchHead × Request/Response Head × isResponse boolean)；同时维护 `isRegisteredOutboundMsgNo` 27-msg 白名单查询。

**不负责**:
- 序列化 XML / 解析 XML → JaxbContextCache / 各 Outbound 链路 service
- 业务逻辑判断 → BatchMessageProcessorService

**依赖上限**: 7 个（ArchUnit 强制 — 当前依赖 4 个: `FepBusinessException`, `FepErrorCode`, `RequestBusinessHead`, `ResponseBusinessHead`，本 Plan refactor 后仍 4 个）

**行数上限**: 300 行（当前 123 行，本 Plan 后 ~125 行）

**如果超出**: 拆分为 `WireShapeMsgNoSets`（msgNo 集合定义）+ `OutboundWireShapeDispatcher`（switch/分派逻辑）

#### AbstractXsdValidationTest 职责边界

**负责**: 提供 fep-processor 模块所有 XSD 验证测试的共享基础设施（`SHARED_REGISTRY` / `SHARED_VALIDATOR` 模块级 singleton + `wrapCfx` 模板助手）

**不负责**:
- 单条 msgNo 的 fixture 数据填充（XSD 字段约束级别）→ 各 *XsdValidationTest 子类
- XSD 编译规则 → XsdSchemaRegistry

**依赖上限**: 5 个（基础设施类，依赖少）

**行数上限**: 200 行（当前 58 行，本 Plan 后 ~85 行）

**如果超出**: 拆分为 `XsdTestBase` (singleton holder) + `CfxTemplateBuilder` (模板助手 builder pattern)

---

## Task 列表

### Task 0: Setup 3 并行 Worktree `模式 A`

**PRD 依据:** 不适用（基础设施 Task，无 FR-ID）
**追溯 ID:** infra-deferred-drain-T0

**验收标准:**
1. **v0.3：2 个** worktree（wt-deferred-r2q2 + wt-deferred-r1，T1/q1 已撤）基于 origin/main = `18f43e1`
2. `git worktree list` 含本 Plan 2 新 worktree；**实测 base 已 5 个其他会话 worktree**（主 + e2e + wt-sb35-upgrade + wt-p4-msg-g + wt-simplify-d1-rename — 均非本 Plan，不介入）→ 总 7 行
3. 2 个新 worktree 各自 working tree clean（实测已确认）

**Files:**
- 不改动文件，仅创建 worktree

- [ ] **Step 1: 实测当前 baseline**

```bash
cd /Users/muzhou/FEP_v1.0 && git rev-parse origin/main main && git status --short
```
期望：`18f43e1...` × 2 + working tree clean（仅 untracked SB 升级 Plan 文件，非本 Plan 责任）

- [x] **Step 2: 创建 worktree（v0.3：2 个 — r2q2 + r1；q1 创建后因 T1 撤销已删）**

```bash
# v0.3 实际执行：q1 已创建→实施前重测发现冲突→已 git worktree remove + branch -D
cd /Users/muzhou/FEP_v1.0 && \
  git worktree add /Users/muzhou/FEP_v1.0_wt-deferred-r2q2 -b chore/deferred-r2q2 origin/main && \
  git worktree add /Users/muzhou/FEP_v1.0_wt-deferred-r1 -b chore/deferred-r1 origin/main
```

- [x] **Step 3: 验证 worktree 创建成功**（已实测 — wt-deferred-r2q2 + wt-deferred-r1 HEAD = `18f43e1` clean；其他 5 worktree 为别会话不介入）

- [x] **Step 4: 不 commit**（无文件改动）

---

### ~~Task 1: DEF-Q1 BodyClassRegistryTest `unboundedSize` 重命名~~ ⛔ **v0.3 撤销 — 不执行**

**撤销依据**: 2026-05-15 muzhou 签字 v0.2 后、T0 worktree 创建完成、实施前最后一刻重测 `git worktree list`，发现另一会话已创建 `/Users/muzhou/FEP_v1.0_wt-simplify-d1-rename [chore/simplify-d1-registry-test-rename]`（locked），分支名指向同一 DEF-Q1（fep-web 唯一 registry test = BodyClassRegistryTest 重命名）。

红线 `feedback_parallel_session_task_allocation_discipline`（避免重复任务）+ `feedback_new_session_no_intervention_in_prior_tasks`（不接手其他会话认领的 WIP）→ muzhou 经 AskUserQuestion 决策**撤销 T1，移交 simplify-d1 会话**。`wt-deferred-q1` worktree + `chore/deferred-q1` branch 已删除（`git worktree remove` + `git branch -D`，实测 base 18f43e1 无改动安全删）。

> **本撤销是本会话开头落盘的红线 `feedback_parallel_session_task_allocation_discipline` 的实战实证**：规划阶段（~1.5h 前）实测仅 SB 升级 1 个 WIP；经 Plan 起草 + R1/R2 双轮 AI 评审 + muzhou 签字（长 cycle）后，实施前重测发现其他会话新认领 DEF-Q1。**教训：T0 worktree 创建后、派发 implementer 前，必须再实测一次 `git worktree list` 检测新增冲突认领**（红线待补此条）。

DEF-Q1 后续由 `wt-simplify-d1-rename` 会话完成，本 Plan 不再追踪。

---

### Task 2: DEF-R2+Q2 OutboundWireShapeDispatcher 4 Set 常量 + Javadoc 消除 `模式 B`

**PRD 依据:** 不适用（Quality + Reuse refactor，行为等价无 FR 状态变更）
**追溯 ID:** DEF-R2 + DEF-Q2（合并，同文件无法独立）

**验收标准:**
1. 4 个 class-level `static final Set<String>` 常量：`REAL_HEAD_REQUEST_MSG_NOS` (8 entries: 1001/1004/3000/3001/3003/3005/3007/3009)、`BATCH_HEAD_REQUEST_MSG_NOS` (10 entries: 1101/1102/1103/1104/3102/3105/3107/3109/3112/3116)、`REAL_HEAD_RESPONSE_MSG_NOS` (5 entries: 2001/2004/3002/3004/3006)、`BATCH_HEAD_RESPONSE_MSG_NOS` (4 entries: 2102/2103/2104/3101)
2. 4 Set size 加总 = 27（保持当前 dispatch 行为不变 = `REGISTERED_MSG_NO_COUNT = 27`）
3. `dispatch()` 方法 switch 表达式改为 4 个 `if (X.contains(msgNo))` 块 + 末尾 throw（行为等价，msgNo→WireShapeDescriptor 映射不变）
4. `isRegisteredOutboundMsgNo()` 改为 `return REAL_HEAD_REQUEST_MSG_NOS.contains(msgNo) || BATCH_HEAD_REQUEST_MSG_NOS.contains(msgNo) || REAL_HEAD_RESPONSE_MSG_NOS.contains(msgNo) || BATCH_HEAD_RESPONSE_MSG_NOS.contains(msgNo);`（单一 truth source）
5. Javadoc 硬编码 "27 集合" / "27 上行报文" 全部替换为 `{@value #REGISTERED_MSG_NO_COUNT}` 自更新引用（line 10/13/41/54/100/107）
6. 错误消息 `"msgNo 不在 27 上行报文集合: "` 改为 `"msgNo 不在 " + REGISTERED_MSG_NO_COUNT + " 上行报文集合: "`（line 95）
7. 行为不变：`OutboundWireShapeDispatcherTest`（既有，~30+ tests）+ 4 outbound IT（`Outbound{3000,3007}WireTest` + `OutboundSupplyChainWireTest` + `OutboundEnterpriseQueryRealtimeWireTest` + `OutboundSupplychainQueryWireTest` + `OutboundBatchWireTest`）全部 PASS

**Files:**
- Modify: `fep-converter/src/main/java/com/puchain/fep/converter/wire/OutboundWireShapeDispatcher.java`

**Worktree:** `/Users/muzhou/FEP_v1.0_wt-deferred-r2q2`（分支 `chore/deferred-r2q2`）

- [ ] **Step 1: 实测当前结构**

```bash
grep -n "switch\|case\|REGISTERED\|RealHead\|BatchHead\|MSG_NO_PATTERN" \
  /Users/muzhou/FEP_v1.0_wt-deferred-r2q2/fep-converter/src/main/java/com/puchain/fep/converter/wire/OutboundWireShapeDispatcher.java | head -30
```
期望：2 个 switch 表达式（line 80 + line 113），4 类目内嵌

- [ ] **Step 2: 添加 4 Set 常量 + 计数常量**

在 class body 顶部（`MSG_NO_PATTERN` 常量后）新增：

```java
/**
 * RealHead + RequestBusinessHead + false 类目（{@value #REAL_HEAD_REQUEST_COUNT} 个报文）.
 *
 * <p>用于实时单笔上行请求报文：企业信息查询请求 / 授权书发送 / 电子凭证报送 /
 * 业务进展查询请求 / 凭证融资状态查询请求 / 对公账户状态查询请求 /
 * 受理单位发起核验请求 / 实时单笔报送。</p>
 */
public static final Set<String> REAL_HEAD_REQUEST_MSG_NOS = Set.of(
        "1001", "1004", "3000", "3001", "3003", "3005", "3007", "3009"
);

/**
 * BatchHead + RequestBusinessHead + false 类目（{@value #BATCH_HEAD_REQUEST_COUNT} 个报文）.
 */
public static final Set<String> BATCH_HEAD_REQUEST_MSG_NOS = Set.of(
        "1101", "1102", "1103", "1104",
        "3102", "3105", "3107", "3109", "3112", "3116"
);

/**
 * RealHead + ResponseBusinessHead + true 类目（{@value #REAL_HEAD_RESPONSE_COUNT} 个报文，含 ResultCode）.
 */
public static final Set<String> REAL_HEAD_RESPONSE_MSG_NOS = Set.of(
        "2001", "2004", "3002", "3004", "3006"
);

/**
 * BatchHead + ResponseBusinessHead + true 类目（{@value #BATCH_HEAD_RESPONSE_COUNT} 个报文，含 ResultCode）.
 */
public static final Set<String> BATCH_HEAD_RESPONSE_MSG_NOS = Set.of(
        "2102", "2103", "2104", "3101"
);

/** Count constants（Javadoc {@value} 引用支持，避免 size() 编译时不可见）. */
private static final int REAL_HEAD_REQUEST_COUNT = 8;
private static final int BATCH_HEAD_REQUEST_COUNT = 10;
private static final int REAL_HEAD_RESPONSE_COUNT = 5;
private static final int BATCH_HEAD_RESPONSE_COUNT = 4;

/** 已登记的 27 上行报文总数（自更新引用：{@value}）. */
public static final int REGISTERED_MSG_NO_COUNT =
        REAL_HEAD_REQUEST_COUNT + BATCH_HEAD_REQUEST_COUNT
                + REAL_HEAD_RESPONSE_COUNT + BATCH_HEAD_RESPONSE_COUNT;
```

加 import：

```java
import java.util.Set;
```

- [ ] **Step 3: refactor dispatch() 方法**

替换 line 80-96 的 switch 表达式：

```java
// Before:
return switch (msgNo) {
    case "1001", "1004",
         "3000", "3001", "3003", "3005",
         "3007", "3009" -> new WireShapeDescriptor(...);
    // ... 4 case blocks
    default -> throw new FepBusinessException(...);
};

// After:
if (REAL_HEAD_REQUEST_MSG_NOS.contains(msgNo)) {
    return new WireShapeDescriptor("RealHead" + msgNo, RequestBusinessHead.class, false);
}
if (BATCH_HEAD_REQUEST_MSG_NOS.contains(msgNo)) {
    return new WireShapeDescriptor("BatchHead" + msgNo, RequestBusinessHead.class, false);
}
if (REAL_HEAD_RESPONSE_MSG_NOS.contains(msgNo)) {
    return new WireShapeDescriptor("RealHead" + msgNo, ResponseBusinessHead.class, true);
}
if (BATCH_HEAD_RESPONSE_MSG_NOS.contains(msgNo)) {
    return new WireShapeDescriptor("BatchHead" + msgNo, ResponseBusinessHead.class, true);
}
throw new FepBusinessException(
        FepErrorCode.OUTBOUND_5108_MSGNO_INVALID,
        "msgNo 不在 " + REGISTERED_MSG_NO_COUNT + " 上行报文集合: " + msgNo);
```

- [ ] **Step 4: refactor isRegisteredOutboundMsgNo() 方法**

替换 line 113-122 的 switch：

```java
// After:
return REAL_HEAD_REQUEST_MSG_NOS.contains(msgNo)
        || BATCH_HEAD_REQUEST_MSG_NOS.contains(msgNo)
        || REAL_HEAD_RESPONSE_MSG_NOS.contains(msgNo)
        || BATCH_HEAD_RESPONSE_MSG_NOS.contains(msgNo);
```

- [ ] **Step 5: 更新 Javadoc 硬编码 "27"**

逐条替换（grep 实测硬编码 "27" 精确位于 line 10/13/54/71/95/100/107；**line 41 是 "4 类 wire-shape" 不含 "27"，不动**）：
- line 10: `决定 27 上行报文 msgNo` → `决定 {@value #REGISTERED_MSG_NO_COUNT} 上行报文 msgNo`
- line 13: `实测自 27 份 XSD` → `实测自 {@value #REGISTERED_MSG_NO_COUNT} 份 XSD`
- line 54: `不在 27 集合` → `不在 {@value #REGISTERED_MSG_NO_COUNT} 集合`
- line 71: `describeFor` 方法 `@throws` Javadoc `不在 27 上行报文集合` → `不在 {@value #REGISTERED_MSG_NO_COUNT} 上行报文集合`
- line 95: 错误消息 `"msgNo 不在 27 上行报文集合: "` → `"msgNo 不在 " + REGISTERED_MSG_NO_COUNT + " 上行报文集合: "`（已在 Step 3 处理，此处复核）
- line 100: `已登记的 27 上行报文集合内` → `已登记的 {@value #REGISTERED_MSG_NO_COUNT} 上行报文集合内`
- line 107: `27 上行报文之一` → `{@value #REGISTERED_MSG_NO_COUNT} 上行报文之一`

- [ ] **Step 6: 跑 fep-converter 测试 + 下游 IT**

```bash
cd /Users/muzhou/FEP_v1.0_wt-deferred-r2q2 && \
  JAVA_HOME=/usr/local/opt/openjdk@17 PATH=/usr/local/opt/openjdk@17/bin:$PATH \
  ./mvnw -pl fep-converter test -Dtest=OutboundWireShapeDispatcherTest -DfailIfNoTests=false -q 2>&1 | tail -20
```
期望：BUILD SUCCESS / 30+ tests / 0 failure

随后跑下游 IT（如 sandbox exit 144 触发，跳过等 GHA CI 兜底，红线 `feedback_mvn_sandbox_exit144_pattern`）：

```bash
cd /Users/muzhou/FEP_v1.0_wt-deferred-r2q2 && \
  JAVA_HOME=/usr/local/opt/openjdk@17 PATH=/usr/local/opt/openjdk@17/bin:$PATH \
  ./mvnw -pl fep-web test -Dtest='Outbound*WireTest' -DfailIfNoTests=false -am -q 2>&1 | tail -30
```
期望：`Tests run: 25+, Failures: 0`

- [ ] **Step 7: Commit**

```bash
cd /Users/muzhou/FEP_v1.0_wt-deferred-r2q2 && \
  git add fep-converter/src/main/java/com/puchain/fep/converter/wire/OutboundWireShapeDispatcher.java && \
  git commit -m "$(cat <<'EOF'
refactor(converter): extract 4 wire-shape Sets + replace hardcoded "27" with REGISTERED_MSG_NO_COUNT (DEF-R2 + DEF-Q2)

Replace inline switch case labels with named static Sets — single source of
truth for the 4 wire-shape categories (RealHead/BatchHead × Request/Response).
Future msgNo additions only need to update the appropriate Set, not edit
2 separate switch statements.

Javadoc references {@value #REGISTERED_MSG_NO_COUNT} for self-updating
documentation. Error message uses computed count.

Behavior unchanged: 27 msgNo → WireShapeDescriptor mapping identical.

DEF-R2 + DEF-Q2 from 2026-05-14 P4-MSG-F Simplify 三审 deferred.

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

### Task 3: DEF-R1 AbstractXsdValidationTest CFX Helper 抽取 `模式 B`

**PRD 依据:** 不适用（测试基础设施 refactor，无 FR-ID）
**追溯 ID:** DEF-R1（来源 2026-05-14 P4-MSG-F Simplify 三审 deferred）

> **⚠️ v0.2 BLOCKER 修订（B1）**: v0.1 §Step1/2/3 基于虚构 `<CfxMessage xmlns="http://hndemp/cfx/v1"><RealHead><BusinessHead><Body>` 结构。**grep 实测真实结构**（ProgressQuery3001/ProgressQueryReturn3002）：
> ```
> <?xml version="1.0" encoding="UTF-8"?>
> <CFX>
>   <HEAD>
>     <Version>1.0</Version><SrcNode>{N}</SrcNode><DesNode>{N}</DesNode><App>FEPx</App>
>     <MsgNo>{msgNo}</MsgNo><MsgId>{20位}</MsgId><CorrMsgId>{20位}</CorrMsgId><WorkDate>20260513</WorkDate>
>   </HEAD>
>   <MSG><RealHead{msgNo}>...</RealHead{msgNo}>{BodyElement}</MSG>
> </CFX>
> ```
> **无 XML namespace、无 `<BusinessHead>`、无 `<Body>` 包裹元素**；fixture 用 Java text block `"""`。**request/response 方向 HEAD 字段不同**：3001(request) SrcNode=A1000142000001/DesNode=A1000143000104/CorrMsgId=全0；3002(response) SrcNode/DesNode **调转** + CorrMsgId **引用 request MsgId**。helper 必须参数化 SrcNode/DesNode/MsgNo/MsgId/CorrMsgId。

**验收标准:**
1. `AbstractXsdValidationTest` 新增 helper（推荐签名，implementer 按 Step 1 实测 6 子类 HEAD 字段矩阵后可调整）：
   `protected static String wrapCfx(String srcNode, String desNode, String msgNo, String msgId, String corrMsgId, String msgInnerXml)`
   生成 `<CFX><HEAD>(8字段，Version=1.0/App=FEPx/WorkDate 由实测决定固定值)</HEAD><MSG>{msgInnerXml}</MSG></CFX>`，**无 namespace**
2. 6 个 *XsdValidationTest 子类（ProgressQuery3001 / ProgressQueryReturn3002 / PzInfoQuery3003 / PzInfoReturn3004 / QyAccQuery3005 / QyAccQueryReturn3006）每个 text block fixture 的重复 CFX/HEAD envelope 改为 `wrapCfx(...)` 调用，`msgInnerXml`（RealHead{msgNo} + body element）保留各子类原值不变
3. 6 子类总行数较 main HEAD（实测当前 **992 行**：161/170/157/186/154/164）显著下降，目标 **≤ 750 行**（净消减 ≥ 240，定性："envelope 重复消除 + 每子类 HEAD 段不再逐 fixture 重复"，不绑死精确行数）
4. 6 子类原有全部 `@Test`（实测约 18-22 个，含 valid/optional-omitted/invalid-missing-field 三类）全部 PASS（行为等价，仅 envelope 抽取，fixture 业务字段值与 XSD 约束满足性不变 — 红线 `feedback_fixture_data_must_satisfy_xsd_constraints`）
5. `AbstractXsdValidationTest` 行数 ≤ 100（当前 58 → 加 helper 后约 78-95）
6. implementer Step 1 必须实测全部 6 子类 HEAD 8 字段矩阵（哪些固定 / 哪些随 msgNo / 哪些随 request-response 方向），若 Version/App/WorkDate 在 6 子类间不一致则升级 helper 签名增加对应参数（mode B 允许按实测调整签名）

**Files:**
- Modify: `fep-processor/src/test/java/com/puchain/fep/processor/validation/AbstractXsdValidationTest.java`
- Modify: `fep-processor/src/test/java/com/puchain/fep/processor/validation/ProgressQuery3001XsdValidationTest.java`
- Modify: `fep-processor/src/test/java/com/puchain/fep/processor/validation/ProgressQueryReturn3002XsdValidationTest.java`
- Modify: `fep-processor/src/test/java/com/puchain/fep/processor/validation/PzInfoQuery3003XsdValidationTest.java`
- Modify: `fep-processor/src/test/java/com/puchain/fep/processor/validation/PzInfoReturn3004XsdValidationTest.java`
- Modify: `fep-processor/src/test/java/com/puchain/fep/processor/validation/QyAccQuery3005XsdValidationTest.java`
- Modify: `fep-processor/src/test/java/com/puchain/fep/processor/validation/QyAccQueryReturn3006XsdValidationTest.java`

**Worktree:** `/Users/muzhou/FEP_v1.0_wt-deferred-r1`（分支 `chore/deferred-r1`）

- [ ] **Step 1: 实测全部 6 子类真实 CFX envelope + HEAD 字段矩阵**

```bash
for f in ProgressQuery3001 ProgressQueryReturn3002 PzInfoQuery3003 PzInfoReturn3004 QyAccQuery3005 QyAccQueryReturn3006; do
  echo "=== $f ===";
  grep -n "<CFX>\|<HEAD>\|<MSG>\|<RealHead\|<SrcNode>\|<DesNode>\|<App>\|<MsgNo>\|<MsgId>\|<CorrMsgId>\|<WorkDate>\|<Version>\|\"\"\"" \
    /Users/muzhou/FEP_v1.0_wt-deferred-r1/fep-processor/src/test/java/com/puchain/fep/processor/validation/${f}XsdValidationTest.java | head -25;
done
```
期望：定位每子类 text block fixture（`"""`）+ HEAD 8 字段 + MSG/RealHead 边界

**实测真实结构（B1 修订后，已 grep 验证 ProgressQuery3001/ProgressQueryReturn3002）**：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<CFX>
  <HEAD>
    <Version>1.0</Version>
    <SrcNode>A1000142000001</SrcNode>   <!-- request；response 方向调转为 A1000143000104 -->
    <DesNode>A1000143000104</DesNode>   <!-- request；response 方向调转为 A1000142000001 -->
    <App>FEPx</App>
    <MsgNo>3001</MsgNo>
    <MsgId>30010000000000000001</MsgId>
    <CorrMsgId>00000000000000000000</CorrMsgId>  <!-- request 全0；response 引用对应 request MsgId -->
    <WorkDate>20260513</WorkDate>
  </HEAD>
  <MSG>
    <RealHead3001>
      <SendOrgCode>30500000000000</SendOrgCode>
      <EntrustDate>20260513</EntrustDate>
      <TransitionNo>00000001</TransitionNo>
    </RealHead3001>
    <ProgressQuery3001>...body fields...</ProgressQuery3001>
  </MSG>
</CFX>
```
**无 XML namespace、无 `<BusinessHead>`、body element（如 `<ProgressQuery3001>`）直接在 `<MSG>` 内与 RealHead 平级、无 `<Body>` 包裹**。

- [ ] **Step 2: 在 AbstractXsdValidationTest 新增 wrapCfx helper（Java text block）**

> implementer 必须先完成 Step 1 全部 6 子类 HEAD 矩阵实测。若 `Version`/`App`/`WorkDate` 在 6 子类间存在差异，升级签名增加对应参数（mode B 允许调整）。以下为推荐签名（假设 Version=1.0/App=FEPx/WorkDate=20260513 在 6 子类固定，需 Step 1 确认）：

```java
/**
 * Wrap the CFX envelope (HEAD + MSG) around per-test inner XML.
 *
 * <p>Eliminates duplicate {@code <CFX><HEAD>...8 fields...</HEAD>} boilerplate
 * across {@code *XsdValidationTest} subclasses. The {@code <MSG>} inner XML
 * (RealHead{msgNo} + body element) is supplied verbatim per fixture.</p>
 *
 * <p>Request vs response direction differs in SrcNode/DesNode/CorrMsgId —
 * callers pass these explicitly.</p>
 *
 * @param srcNode     {@code <SrcNode>} value (request: A1000142000001 / response swapped)
 * @param desNode     {@code <DesNode>} value
 * @param msgNo       4-digit message number, also the {@code <MsgNo>} value
 * @param msgId       20-char {@code <MsgId>}
 * @param corrMsgId   20-char {@code <CorrMsgId>} (request: all-zero / response: corresponding request MsgId)
 * @param msgInnerXml verbatim XML inside {@code <MSG>...</MSG>} (RealHead{msgNo} + body element)
 * @return full CFX envelope XML string
 */
protected static String wrapCfx(final String srcNode,
                                  final String desNode,
                                  final String msgNo,
                                  final String msgId,
                                  final String corrMsgId,
                                  final String msgInnerXml) {
    return """
            <?xml version="1.0" encoding="UTF-8"?>
            <CFX>
              <HEAD>
                <Version>1.0</Version>
                <SrcNode>%s</SrcNode>
                <DesNode>%s</DesNode>
                <App>FEPx</App>
                <MsgNo>%s</MsgNo>
                <MsgId>%s</MsgId>
                <CorrMsgId>%s</CorrMsgId>
                <WorkDate>20260513</WorkDate>
              </HEAD>
              <MSG>%s</MSG>
            </CFX>
            """.formatted(srcNode, desNode, msgNo, msgId, corrMsgId, msgInnerXml);
}
```

> **注**: `String.formatted()` 是 Java 15+ 标准 API（项目 Java 17 ✓）。helper 不含 namespace（实测 fixture 无 `xmlns`，XSD 也不强制 — implementer Step 1 顺带 `grep -L targetNamespace fep-processor/src/main/resources/xsd/3001.xsd` 复核确认无 namespace 约束）。

- [ ] **Step 3: 重构 6 子类调用 wrapCfx（保留 msgInner 原值）**

以 ProgressQuery3001XsdValidationTest（request 方向）为例，fixture 是 `static final String ... = """..."""`：

```java
// Before (~30 行 text block per fixture):
private static final String VALID_FULL_FIELDS_XML = """
        <?xml version="1.0" encoding="UTF-8"?>
        <CFX>
          <HEAD>
            <Version>1.0</Version>
            <SrcNode>A1000142000001</SrcNode>
            <DesNode>A1000143000104</DesNode>
            <App>FEPx</App>
            <MsgNo>3001</MsgNo>
            <MsgId>30010000000000000001</MsgId>
            <CorrMsgId>00000000000000000000</CorrMsgId>
            <WorkDate>20260513</WorkDate>
          </HEAD>
          <MSG>
            <RealHead3001>...</RealHead3001>
            <ProgressQuery3001>...</ProgressQuery3001>
          </MSG>
        </CFX>
        """;

// After (~12 行：MSG inner text block 保留原值，envelope 由 helper 生成):
private static final String VALID_FULL_FIELDS_XML = wrapCfx(
        "A1000142000001", "A1000143000104", "3001",
        "30010000000000000001", "00000000000000000000",
        """
        <RealHead3001>
          <SendOrgCode>30500000000000</SendOrgCode>
          <EntrustDate>20260513</EntrustDate>
          <TransitionNo>00000001</TransitionNo>
        </RealHead3001>
        <ProgressQuery3001>
          <SerialNo>SN300100000000000000000000001A</SerialNo>
          ...（body 字段保留原值不变 — 红线 feedback_fixture_data_must_satisfy_xsd_constraints）
        </ProgressQuery3001>
        """);
```

> **关键约束**: `msgInnerXml`（RealHead + body element 字段值）**逐字保留各子类原 fixture 值**，仅把 CFX/HEAD envelope 抽到 helper。response 方向子类（3002/3004/3006）的 SrcNode/DesNode 传调转值、CorrMsgId 传对应 request MsgId（按 Step 1 实测各子类原值传参）。`static final String = wrapCfx(...)` 在静态初始化期调用（helper 是 `static` ✓）。

- [ ] **Step 4: 跑 fep-processor 测试**

```bash
cd /Users/muzhou/FEP_v1.0_wt-deferred-r1 && \
  JAVA_HOME=/usr/local/opt/openjdk@17 PATH=/usr/local/opt/openjdk@17/bin:$PATH \
  ./mvnw -pl fep-processor test -Dtest='ProgressQuery3001*,ProgressQueryReturn3002*,PzInfoQuery3003*,PzInfoReturn3004*,QyAccQuery3005*,QyAccQueryReturn3006*' -DfailIfNoTests=false -q 2>&1 | tail -30
```
期望：18+ tests PASS, 0 failures

- [ ] **Step 5: 行数消减验证**

```bash
cd /Users/muzhou/FEP_v1.0_wt-deferred-r1 && \
  wc -l fep-processor/src/test/java/com/puchain/fep/processor/validation/{ProgressQuery3001,ProgressQueryReturn3002,PzInfoQuery3003,PzInfoReturn3004,QyAccQuery3005,QyAccQueryReturn3006}XsdValidationTest.java
```
期望：6 子类总行数由 main HEAD 实测 **992 行** 降至 **≤ 750 行**（净消减 ≥ 240；定性目标 — envelope 重复消除，不绑死精确值；若 < 240 但 envelope 确已消除且测试全绿，由 quality reviewer 判定是否可接受）

- [ ] **Step 6: Commit**

```bash
cd /Users/muzhou/FEP_v1.0_wt-deferred-r1 && \
  git add fep-processor/src/test/java/com/puchain/fep/processor/validation/ && \
  git commit -m "$(cat <<'EOF'
test(processor): extract wrapCfx helper, refactor 6 supplychain XSD validation tests (DEF-R1)

Add `AbstractXsdValidationTest.wrapCfx(srcNode, desNode, msgNo, msgId,
corrMsgId, msgInnerXml)` helper to wrap the CFX <HEAD>+<MSG> envelope
(no namespace) around per-fixture MSG inner XML. Refactor 6 P4-MSG-F
supplychain query test classes (3001-3006) to call the helper instead of
repeating the 8-field <HEAD> block in every text-block fixture.

Line reduction: 6 subclasses 992 → <=750 lines. MSG inner XML (RealHead +
body fields) preserved verbatim — XSD constraint satisfaction unchanged.
Behavior unchanged: all existing @Test pass.

DEF-R1 from 2026-05-14 P4-MSG-F Simplify 三审 deferred.

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

### Task 4: Merge 3 Branch → main + Push + Cleanup `模式 A`

**PRD 依据:** 不适用（基础设施 closing Task）
**追溯 ID:** infra-deferred-drain-T4

**验收标准:**
1. **v0.3：2 个** commit (T2/T3) merge 到 main worktree（顺序：T2 → T3；T1 已撤）
2. 主 worktree main 上 2 个 commit ahead origin/main（push 前）
3. `git push origin main` 成功
4. 2 个**本 Plan** worktree cleanup（wt-deferred-r2q2 + wt-deferred-r1 remove；**其他会话 wt-sb35-upgrade / wt-p4-msg-g / wt-simplify-d1-rename 保留不动**）
5. 2 个 branch (`chore/deferred-r2q2` / `chore/deferred-r1`) 本地删除（远端无 push）
6. origin/main = main = cherry-pick 后第 2 个 commit SHA（T3）

**Files:**
- 无文件改动（仅 git 操作）

**Worktree:** 主 worktree `/Users/muzhou/FEP_v1.0`

- [ ] **Step 1: 主 worktree fetch + 实测 baseline**

```bash
cd /Users/muzhou/FEP_v1.0 && git fetch origin --quiet && \
  echo "origin/main: $(git rev-parse origin/main)" && \
  echo "main: $(git rev-parse main)" && \
  git log origin/main..main --oneline
```
期望：origin/main = main = `18f43e1`（与 T0 baseline 一致，期间若 main 有别会话 commit drift 需 STOP + 红线 `feedback_baseline_drift_during_long_review_cycle` 走 muzhou 拍板）

- [ ] **Step 2: 主 worktree 切到 main + cherry-pick 2 commit（v0.3：T2 → T3）**

> **v0.2 逻辑修订**: v0.1 `git merge --ff-only` ×N 必失败（同基 branch 首个 ff 后第 2 个非后代）→ cherry-pick（文件 disjoint 无冲突，线性历史）。**v0.3**: T1/q1 已撤，仅 cherry-pick T2 + T3。

```bash
cd /Users/muzhou/FEP_v1.0 && git checkout main && \
  R2Q2=$(git rev-parse chore/deferred-r2q2) && \
  R1=$(git rev-parse chore/deferred-r1) && \
  git cherry-pick "$R2Q2" && \
  git cherry-pick "$R1"
```

> **注**: 2 commit 改动文件互不重叠（fep-converter/main / fep-processor/test），cherry-pick 顺序 T2→T3 无冲突。报冲突即文件 disjoint 假设破裂，STOP + 红线诊断。

- [ ] **Step 3: 实测 2 commit ahead origin/main**

```bash
cd /Users/muzhou/FEP_v1.0 && git log origin/main..main --oneline
```
期望：2 行 commit SHA (T2 / T3)

- [ ] **Step 4: Push origin main**

```bash
cd /Users/muzhou/FEP_v1.0 && git push origin main
```

- [ ] **Step 5: Cleanup 2 本 Plan worktree + branch（其他会话 worktree 不动）**

```bash
cd /Users/muzhou/FEP_v1.0 && \
  git worktree remove /Users/muzhou/FEP_v1.0_wt-deferred-r2q2 && \
  git worktree remove /Users/muzhou/FEP_v1.0_wt-deferred-r1 && \
  git branch -D chore/deferred-r2q2 chore/deferred-r1 && \
  git worktree list
```
> **注**: `wt-deferred-q1` 已于实施前撤销时删除（不在此 Step）。cherry-pick 后 branch SHA ≠ main，`-d` 拒绝，用 `-D` 强删（内容已进 main，安全）。期望 worktree list 保留 wt-sb35-upgrade / wt-p4-msg-g / wt-simplify-d1-rename（别会话，不介入）。

期望：worktree list **3 行**（主 + e2e + **wt-sb35-upgrade 保留**）+ 3 个 chore/deferred-* branch 删除

- [ ] **Step 6: 不 commit**（Task 4 仅 git 操作，无文件改动）

---

## 自检清单

### 1. PRD 覆盖度

本 Plan 不涉及 FR-ID（Quality polish + 测试基础设施 refactor）。`prd-traceability-matrix.md` 不需更新。

### 2. 安全边界检查

无 `security/impl/` 改动。3 个文件全部在 `fep-web/test` / `fep-converter/main` / `fep-processor/test`，无国密 / 密钥 / 脱敏 / 审计日志关键词。

### 3. 占位符扫描

本 Plan 已通读，无 TBD/TODO/待/后续/类似 Task 占位符。

### 4. 类型一致性

- T2 引用 `Set.of(...)` (Java 9+) — 项目 Java 17 兼容
- T2 引用 `FepBusinessException` / `FepErrorCode.OUTBOUND_5108_MSGNO_INVALID` — 已存在
- T3 引用 `wrapCfx`（v0.2 重命名，原 `wrapCfxTemplate`）— T3 内一致提供 + 消费；`String.formatted()` Java 15+（项目 17 ✓）；Java text block `"""` Java 15+ ✓

### 5. 测试命令可执行

- T1: `mvn test -Dtest=BodyClassRegistryTest#registry_shouldUseMapOfEntries_unboundedSize` → 测试方法名重命名后一致
- T2: `mvn test -Dtest=OutboundWireShapeDispatcherTest` → 既有测试类，本 Plan 不重命名
- T3: `mvn test -Dtest='ProgressQuery3001*,...'` glob → 6 子类不重命名

### 6. CLAUDE.md 更新

T4 closing 不更新 CLAUDE.md `§当前项目状态`（本 Plan 闭环后 session-end Phase 6 会统一更新，与 R1 末段¹¹ 段并入"2026-05-15 末段¹² P4-MSG-F deferred drain 闭环"）。

### 7. 验收标准完整性

每个 Task 验收 ≥ 4 条具体可验证标准，断言值（如 27、4 类目、6 子类、992→≤750 行）均来自 grep 实测。

### 8. 共享工具类无遗漏

`wrapCfx` 唯一共享工具，T3 内提供 + T3 内 6 子类消费（同 Task 完成，无跨 Task 依赖问题）。

### 9. 核心类职责边界

- `OutboundWireShapeDispatcher`: 当前 123 行，refactor 后 ~125 行（不超 300 上限），依赖 4 个 (≤7 上限)
- `AbstractXsdValidationTest`: 当前 58 行，refactor 后 ~85 行（不超 200 上限），依赖 ~3 个

### 10. Worktree 触发条件自检

逐条核对：

- [x] 跨 ≥ 3 个 Maven 模块？✅ fep-web + fep-converter + fep-processor（恰 3，等于阈值）
- [x] 与已签字未执行的 Plan 并存？✅ Spring Boot 3.5.9 升级 Plan v2 已签字未执行
- [ ] ⛔ 安全 vs AI 并行？❌
- [ ] TLQ `tongtech` profile 联调？❌
- [ ] ≥ 5 min long-running verify？❌（3 个 -pl 单模块测试，各 < 1 min）
- [ ] muzhou WIP 与 AI 任务并存？❌

命中 2 项 → 使用 3 独立 worktree（已在头部声明 + T0 Setup + T4 Cleanup 中实测）。

---

## 风险与缓解

| 风险 | 概率 | 缓解 |
|---|---|---|
| SB 3.5.9 升级 Plan 在本 Plan 实施期间执行 + main HEAD drift | **高**（评审周期内已实测 1 次 drift `8d669b2`→`18f43e1` = SB Plan v2.1 docs commit；SB 升级**代码实施**随时可能开始）| ① 3 worktree 物理隔离主 branch；② T4 Step 1 实测 origin/main vs 签字时 baseline，drift 即 STOP；③ drift 若为 docs-only（如本次）则文字更新 baseline 继续，若含 pom.xml/代码则 STOP 走 muzhou 拍板（红线 `feedback_baseline_drift_during_long_review_cycle`）|
| sandbox mvn exit 144 拦截本机测试 | 中 | 各 Task 测试用 `-pl <module> -am -Dsurefire.failIfNoSpecifiedTests=false` 限定范围（红线 `feedback_surefire3_failifno_specified_tests_param_rename` — 已修正参数名）+ sandbox fail 时跳本机 → GHA CI 远端兜底（红线 `feedback_mvn_sandbox_exit144_pattern`） |
| 3 并行 implementer subagent commit step abandoned | 低 | 每 subagent 独立 worktree 物理隔离，commit 互不影响；fail 时主对话务实接管 + footer 透明披露（红线 `feedback_subagent_commit_step_dual_fail_pattern` + `feedback_sendmessage_tool_unavailable_equals_dual_fail`） |
| **T3 helper HEAD 字段方向矩阵实测错误**（B1 修订后核心风险）| 中 | implementer Step 1 强制实测全部 6 子类 HEAD 8 字段（Version/SrcNode/DesNode/App/MsgNo/MsgId/CorrMsgId/WorkDate）矩阵：哪些固定 / 随 msgNo / 随 request-response 方向调转；response 子类（3002/3004/3006）SrcNode/DesNode 调转 + CorrMsgId 引用 request — 必须逐子类按原 fixture 值传参，不可套用 3001 request 模板 |
| T3 fixture body 字段值在重构中被意外修改 → XSD 验证 fail | 中 | `msgInnerXml` 逐字保留各子类原值（红线 `feedback_fixture_data_must_satisfy_xsd_constraints`）；implementer 重构后 diff 验证仅 envelope 段变化，body 段 0 改动 |
| T2 Javadoc `{@value}` 反向引用编译警告 | 极低 | `int` 常量 `{@value}` 是标准 Javadoc 语法 |

---

## 评审记录

> 本段在 AI 评审完成后追加，muzhou 签字后才能进入实施。

### v0.1 起草（2026-05-15）

- **起草人**: Claude Code 主对话（mode A）
- **起草基线**: origin/main = `8d669b2`（R1 Shared Registry Residual Refactor 已 ship；起草时实测值，v0.2 已漂移）
- **AI 评审 R1**: general-purpose subagent 独立评审 → **NOT CLEARED**（3 BLOCKER + 2 MAJOR + 2 MINOR + 1 NIT）
  - B1: DEF-R1 §Step1/2/3 基于虚构 `<CfxMessage xmlns><BusinessHead><Body>` 结构（实测真实 `<CFX><HEAD><MSG>` 无 namespace）
  - B2: 全文 baseline `8d669b2` 已漂移至 `18f43e1`
  - B3: DEF-R1 "≥400 行消减" 不可达（6 子类实测 992 行非声称 1500-1800）
  - M1: Javadoc 行号清单错误（含不该改的 41，漏 71）
  - M2: §Step1 grep 模式与真实标签零匹配
  - 根因：起草违反红线 `feedback_plan_must_grep_actual_api`（DEF-R1 envelope / baseline / 行数三项未 grep 实测）

### v0.2 内联修订（2026-05-15）

- **修订人**: Claude Code 主对话
- **修订基线**: origin/main = main = `18f43e1`（实测漂移 = 1 docs-only commit `18f43e1 docs(plan): add Spring Boot 3.5.9 upgrade Plan v2.1`，不碰代码，文字更新 baseline 即可，红线 `feedback_baseline_drift_during_long_review_cycle` 透明记录）
- **修订项**:
  - B1: Task 3 验收标准 + Step 1/2/3 全重写，基于 grep 实测真实结构 `<CFX><HEAD>(8字段)</HEAD><MSG><RealHead{msgNo}>+body</MSG></CFX>`（无 namespace），helper 重命名 `wrapCfxTemplate`→`wrapCfx(srcNode,desNode,msgNo,msgId,corrMsgId,msgInnerXml)`，参数化 request/response 方向 HEAD 字段，改用 Java text block；新增"6 子类 HEAD 矩阵必须实测"约束
  - B2: 全文 7 处 `8d669b2`→`18f43e1`
  - B3: §验收标准3 改定性（992 → ≤750，净 ≥240）+ commit message 行数修正
  - M1: §Step5 Javadoc 行号改 10/13/54/71/95/100/107（移除 41）
  - M2: §Step1 grep 模式改 `<CFX>`/`<HEAD>`/`<MSG>`/`<RealHead`
  - m1: Task0/T4 worktree 计数 +1（计入已存在 wt-sb35-upgrade）
  - m2: §Step3 改 Java text block 示例
  - **额外发现（非评审项）**: Task 4 Step 2 `git merge --ff-only`×3 逻辑 bug（3 branch 同基首个 ff 后第 2 个必失败）→ 改 cherry-pick；`-DfailIfNoTests` deprecated → `-Dsurefire.failIfNoSpecifiedTests`（红线 `feedback_surefire3_failifno_specified_tests_param_rename`）
- **AI 评审 R2**: general-purpose subagent 复审 → **PASS**（3 BLOCKER + 2 MAJOR 全 FIXED，cherry-pick + text block 逻辑独立 grep 验证正确，3 NIT 已于签字前清除）

### muzhou 批准签字（2026-05-15）

✅ **APPROVED** — muzhou 经 AskUserQuestion 批准，选择"3 implementer 真并行实施"。

- **批准人**: muzhou（Plan Approver / 项目质量最终负责人）
- **批准时间**: 2026-05-15
- **签字基线**: origin/main = main = `18f43e1`（签字时实测，与 v0.2 修订基线一致，红线 `feedback_baseline_drift_during_long_review_cycle` 签字点实测通过）
- **执行方式**: subagent-driven-development — T0 创建 3 worktree → 单消息并行派发 3 implementer subagent (T1/T2/T3) 各自 worktree commit → 每 Task 独立 spec + quality reviewer → T4 cherry-pick merge + push + cleanup → /session-end 6-phase
- **AI 评审通过前提**: R1 NOT CLEARED → v0.2 修订 → R2 PASS（双轮独立 subagent 评审）

### v0.3 签字后实施前重测修订（2026-05-15）

- **触发**: T0 worktree 创建完成后、派发 implementer 前，按工程纪律重测 `git worktree list`，发现 worktree 由规划时 2 个增至 8 个。实测确认：
  - `wt-simplify-d1-rename [chore/simplify-d1-registry-test-rename]`（locked，空 worktree）→ **与本 Plan T1 DEF-Q1 重复**（fep-web 唯一 registry test）
  - `wt-sb35-upgrade` HEAD `18f43e1`→`a631da6`（1 commit `bump spring-boot 3.3.7→3.5.14`，仅改 pom.xml，未 merge origin/main，物理隔离不冲突）
  - `wt-p4-msg-g [feat/p4-msg-g-supplychain-query-batch2]`（空 worktree，与 T3 fep-processor/test disjoint，不冲突）
- **muzhou 决策**（AskUserQuestion）: 撤 T1，T2+T3 双 ticket 继续并行
- **修订**: 全文 v0.2→v0.3 — Task 1 标 ⛔ 撤销不执行；执行 Worktree/What changes/文件结构/Task 0/Task 4/自检清单 全部由 3 ticket 降为 2（T2 fep-converter + T3 fep-processor/test）；`wt-deferred-q1` worktree + branch 已删
- **工程纪律实证**: 本撤销验证本会话开头落盘的红线 `feedback_parallel_session_task_allocation_discipline` —— 长 review cycle（~1.5h：起草+R1+v0.2+R2+签字）期间其他会话 WIP 变化，**实施前最后一刻必须重测**。红线待 muzhou 拍板补充第 5 步"T0 worktree 创建后 / 派发 implementer 前重测 git worktree list"
- **AI 评审**: 范围缩减（撤 Task 非新增风险，T2/T3 内容 R2 已 PASS 未变）→ 不重派评审，inline 记录 + muzhou 已授权双 ticket 并行
