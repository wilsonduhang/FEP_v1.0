# FEP P4-MSG-F 供应链查询 outbound wire batch1 实施计划

> **执行方式:** 使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐 Task 执行。步骤使用 `- [ ]` 复选框跟踪。

**目标:** 将供应链查询类 6 报文（3001 业务进展查询请求 / 3002 业务进展查询回执 / 3003 凭证融资状态查询请求 / 3004 凭证融资状态查询回执 / 3005 对公账户查询请求 / 3006 对公账户查询回执）的 outbound wire-out 通路注册到 `BodyClassRegistry` + `OutboundWireShapeDispatcher`，配套 XSD 验证 IT + 端到端 wire bean 集成 IT，达成 FR-MSG-3001..3006 outbound 维度合规。

**前置依赖:**
- P4-Plan-C T1（已完成 2026-05-11）— 6 个 Body POJO + 6 XSD + inbound dispatcher 注册 + SerialNoBearing 接口合规
- P4-MSG-E（已完成 2026-05-11）— OutboundWireShapeDispatcher 新增 "RealHead + ResponseBusinessHead + true" wire-shape 类目，本 Plan 复用此分组
- baseline HEAD `4cb73ee` (origin/main，working tree clean，2026-05-13)

**执行 Worktree:** `/Users/muzhou/FEP_v1.0_wt-p4-msg-f-batch1`（分支 `feat/p4-msg-f-supplychain-query-batch1`，触发条件第 ②⑤ 项 — 与 P4-MSG-D-2101-consumer worktree `feat/p4-msg-d-2101-inbound-consumer` 并存；T4 fep-web `@SpringBootTest` context ~65s + 6 case verify ≥5min 长跑期间需起草 batch2/batch3 候选）

**架构:** 6 个 Body POJO 与 6 个 XSD 已在 P4-Plan-C T1 落地，本 Plan 仅在 outbound 装配层做注册：BodyClassRegistry 21→27 entries（Map.ofEntries append-only）+ OutboundWireShapeDispatcher 21→27 上行报文（3001/3003/3005 合并到 "RealHead+RequestBusinessHead+false" 既有 case，3002/3004/3006 合并到 P4-MSG-E 新建的 "RealHead+ResponseBusinessHead+true" 类目）。设计依据：FEP 双角色 outbound symmetric registration 模式（与 P4-MSG-E 1001/2001/1004/2004 完全一致 — outbound 注册不区分角色，运行时 fep.role 配置决定 active dispatch 集合）。

**技术栈:** Java 17 / Spring Boot 3.2.x / Maven / JAXB jakarta / JUnit 5 / AssertJ / @SpringBootTest

**AI 协同模式:**

| 模式 | AI 占比 | 本计划中的应用 |
|:----:|:-------:|--------------|
| A | 90% | T1 Registry 注册 / T3 XsdValidationTest fixture 落盘 / T4 IT 参数化 |
| B | 70% | T2 OutboundWireShapeDispatcher case 拓扑 / Javadoc 表述修订 |
| C | 60% | 无 |
| D | 50% | 无 |
| E | 0%  | 无（本 Plan 不涉及 security/impl/） |

---

## 设计背景（design context）

**PRD 双角色方向矩阵实测**（PRD v1.3 §3.1 双角色定义 + §4.6 报文方向，line 825-830）:

| Code | 银行（受理单位） | 信息服务机构 | outbound 注册理由 |
|------|---------------|------------|------------------|
| 3001 业务进展查询请求 | 被动接收 | **主动发起** | 信息服务角色 outbound |
| 3002 业务进展查询回执 | **主动发起** | 被动接收 | 银行角色 outbound |
| 3003 融资状态查询请求 | 主动发起 | **主动发起** | 双角色 outbound |
| 3004 融资状态查询回执 | 被动接收 | 被动接收 | 双角色被动；但响应入站 3003 query 时仍需 outbound 通路（参考 P4-MSG-E 2001 注册逻辑）|
| 3005 对公账户查询请求 | 被动接收 | **主动发起** | 信息服务角色 outbound |
| 3006 对公账户查询回执 | **主动发起** | 被动接收 | 银行角色 outbound |

**决策**: 6 个 code 全部注册为 outbound（含 3004），与 P4-MSG-E 对 1001/2001/1004/2004 完整注册策略一致。运行时 `fep.role` 配置（bank / supplychain）决定 ScheduledOutboundProducer 实际选哪些 msgNo dispatch。

**XSD root element 混合命名实测**（红线 `feedback_plan_revision_must_grep_actual_api` 重点）:

| Code | XSD 文件 | @XmlRootElement 命名 | 大小写规范 |
|------|---------|--------------------|----------|
| 3001 | 3001.xsd line 36 | `ProgressQuery3001` | PascalCase |
| 3002 | 3002.xsd | `ProgressQueryReturn3002` | PascalCase |
| 3003 | 3003.xsd line 36 | `pzInfoQuery3003` | **camelCase** ⚠️ |
| 3004 | 3004.xsd | `pzInfoReturn3004` | **camelCase** ⚠️ |
| 3005 | 3005.xsd | `qyAccQuery3005` | **camelCase** ⚠️ |
| 3006 | 3006.xsd line | `qyAccQueryReturn3006` | **camelCase** ⚠️ |

POJO @XmlRootElement 已在 P4-Plan-C T1 与 XSD 一致，本 Plan 仅在 fixture / Javadoc 中重复混合命名时需小心。

**Worktree 触发条件自检**（红线 `feedback_worktree_for_parallel_work`）:
- [x] ① 跨 fep-processor + fep-converter + fep-web 3 模块 — 命中
- [x] ② 与已签字未执行的 Plan 并存 — `feat/p4-msg-d-2101-inbound-consumer` worktree 当前活跃（HEAD `d6f8591`，待 muzhou 决定 merge；本 Plan 改动范围 BodyClassRegistry + OutboundWireShapeDispatcher 与 P4-MSG-D 改动范围 InboundMessageDispatcher 无文件级冲突，实测确认）
- [ ] ③ ⛔ 安全 vs AI 并行？无
- [ ] ④ TLQ tongtech 联调？无
- [x] ⑤ ≥5min long-running verify — T4 fep-web `@SpringBootTest` context ~65s + 6 case wire-test 端到端 + 全 reactor verify > 5min
- [ ] ⑥ muzhou WIP 与 AI 并存？签字前未知，按"可能"处理

3 条触发条件命中 (①②⑤) → 独立 worktree 强制。

---

## 文件结构

| 文件路径 | 职责 | 操作 | AI 模式 |
|----------|------|------|:-------:|
| `fep-web/src/main/java/com/puchain/fep/web/outbound/consumer/BodyClassRegistry.java` | outbound msgNo→Body Class 主映射 21→27 entries + Javadoc 更新 | Modify | A |
| `fep-web/src/test/java/com/puchain/fep/web/outbound/consumer/BodyClassRegistryTest.java` | Registry resolve 单测 +6 case | Modify | A |
| `fep-converter/src/main/java/com/puchain/fep/converter/wire/OutboundWireShapeDispatcher.java` | wire-shape 路由 21→27 case + Javadoc 21→27 + 移除"inbound-only 3003/3005"过时注释 | Modify | B |
| `fep-converter/src/test/java/com/puchain/fep/converter/wire/OutboundWireShapeDispatcherTest.java` | Dispatcher describeFor + isRegisteredOutboundMsgNo 单测 +6 case | Modify | A |
| `fep-processor/src/test/java/com/puchain/fep/processor/validation/ProgressQuery3001XsdValidationTest.java` | 3001 XSD validate 3 case | Create | A |
| `fep-processor/src/test/java/com/puchain/fep/processor/validation/ProgressQueryReturn3002XsdValidationTest.java` | 3002 XSD validate 3 case | Create | A |
| `fep-processor/src/test/java/com/puchain/fep/processor/validation/PzInfoQuery3003XsdValidationTest.java` | 3003 XSD validate 3 case（camelCase root） | Create | A |
| `fep-processor/src/test/java/com/puchain/fep/processor/validation/PzInfoReturn3004XsdValidationTest.java` | 3004 XSD validate 3 case（camelCase root） | Create | A |
| `fep-processor/src/test/java/com/puchain/fep/processor/validation/QyAccQuery3005XsdValidationTest.java` | 3005 XSD validate 3 case（camelCase root） | Create | A |
| `fep-processor/src/test/java/com/puchain/fep/processor/validation/QyAccQueryReturn3006XsdValidationTest.java` | 3006 XSD validate 3 case（camelCase root） | Create | A |
| `fep-web/src/test/java/com/puchain/fep/web/outbound/consumer/OutboundSupplychainQueryWireTest.java` | 6 case 端到端 wire bean 协调 IT | Create | A |
| `docs/plans/prd-traceability-matrix.md` | FR-MSG-3001..3006 追加 outbound wire-out 标记 | Modify | A (在 T5 session-end 阶段) |
| `docs/plans/PHASE_HISTORY.md` | 顶部 metadata + 阶段历史登记本 Plan | Modify | A (在 T5 session-end 阶段) |
| `CLAUDE.md` | "当前项目状态"段 + "下一步候选"段更新 | Modify | A (在 T5 session-end 阶段) |

### 共享工具类清单

本 Plan 不新增共享工具类，全部复用既有：
- `AbstractXsdValidationTest`（fep-processor 既有，所有 6 个 XsdValidationTest 继承）
- `BodyClassRegistry` / `OutboundWireShapeDispatcher`（既有，仅 append entries）

### 核心类职责边界声明

**`BodyClassRegistry` 职责边界**（已存在，本 Plan 仅扩展 entry）:
- 负责: outbound 路径上 msgNo→Body POJO Class 主映射查询（`Map#ofEntries` 不可变）
- 不负责: 校验业务字段、生成 head 元素名（→ `OutboundWireShapeDispatcher`）、JAXB marshal（→ `OutboundCfxEnvelopeBuilder`）
- 依赖上限: 0（纯 Map lookup component）
- 行数上限: 200（当前 119 → 本 Plan +9 后约 128，余量充足）
- 如果超出: 不会，本 Plan 仅 +6 Map.entry + Javadoc

**`OutboundWireShapeDispatcher` 职责边界**（已存在，本 Plan 仅扩展 case）:
- 负责: outbound msgNo → WireShapeDescriptor（headElementName / headClass / requiresResultCode）路由 + isRegisteredOutboundMsgNo 查询
- 不负责: Body POJO 解析（→ `BodyClassRegistry`）、XSD validate（→ `XsdValidator`）
- 依赖上限: 0（纯枚举 switch）
- 行数上限: 200（当前 112 → 本 Plan +12 后约 124，余量充足）
- 如果超出: 不会，本 Plan 仅 +6 case label + Javadoc

---

## PRD 覆盖度

本 Plan 覆盖 FR-MSG-3001/3002/3003/3004/3005/3006 的 outbound wire-out 维度（inbound dispatcher 维度已 P4-Plan-C T1 完成）。

| FR-ID | 当前矩阵状态 | 本 Plan 后追加 |
|-------|------------|--------------|
| FR-MSG-3001 | ✅ XSD + Body POJO + XSD IT + 异步流 IT + inbound dispatcher (P4-Plan-C T1) | + outbound wire-out 完成 (P4-MSG-F T1+T2，RealHead3001+RequestBusinessHead+false 合并到既有 case) |
| FR-MSG-3002 | ✅ ... + inbound dispatcher | + outbound wire-out 完成 (P4-MSG-F T1+T2，RealHead3002+ResponseBusinessHead+true 合并到 P4-MSG-E 新类目) |
| FR-MSG-3003 | ✅ ... | + outbound wire-out 完成 (RealHead3003+RequestBusinessHead+false) |
| FR-MSG-3004 | ✅ ... | + outbound wire-out 完成 (RealHead3004+ResponseBusinessHead+true；双角色被动接收，注册为 inbound 响应回路兜底) |
| FR-MSG-3005 | ✅ ... | + outbound wire-out 完成 (RealHead3005+RequestBusinessHead+false) |
| FR-MSG-3006 | ✅ ... | + outbound wire-out 完成 (RealHead3006+ResponseBusinessHead+true) |

**不在本 Plan 范围**: 3004 实际业务流（双角色均被动）— PRD 矩阵层面注册但运行时不主动发送。运行时 dispatch 是否启用由 ScheduledOutboundProducer + fep.role 配置决定，本 Plan 不涉及。

---

## Task 拆分

### Task 1: BodyClassRegistry +6 entries（3001-3006 outbound 注册） `模式 A`

**PRD 依据:** v1.3 §3.2 报文结构 + §4.6 报文方向 + §4.2 实时类业务报文
**追溯 ID:** FR-MSG-3001 / FR-MSG-3002 / FR-MSG-3003 / FR-MSG-3004 / FR-MSG-3005 / FR-MSG-3006

**验收标准:**
1. `registry.resolve("3001")` → `ProgressQuery3001.class`
2. `registry.resolve("3002")` → `ProgressQueryReturn3002.class`
3. `registry.resolve("3003")` → `PzInfoQuery3003.class`
4. `registry.resolve("3004")` → `PzInfoReturn3004.class`
5. `registry.resolve("3005")` → `QyAccQuery3005.class`
6. `registry.resolve("3006")` → `QyAccQueryReturn3006.class`
7. `registry.resolve(null)` 抛 `FepBusinessException` + `FepErrorCode.OUTBOUND_5107_BODY_CLASS_NOT_FOUND`（既有行为不变）
8. `registry.resolve("9999")` 抛同上（既有行为不变）
9. Javadoc 文档块 `<ul>` 列表 21→27 entries 与 `Map.ofEntries` 调用 1:1 一致

**Files:**
- Modify: `fep-web/src/main/java/com/puchain/fep/web/outbound/consumer/BodyClassRegistry.java`
- Modify: `fep-web/src/test/java/com/puchain/fep/web/outbound/consumer/BodyClassRegistryTest.java`

- [ ] **Step 1: 先 grep 实测当前 Javadoc + import 顺序**

```bash
cd /Users/muzhou/FEP_v1.0_wt-p4-msg-f-batch1
grep -n "Map.entry" fep-web/src/main/java/com/puchain/fep/web/outbound/consumer/BodyClassRegistry.java
grep -n "import com.puchain.fep.processor.body.supplychain" fep-web/src/main/java/com/puchain/fep/web/outbound/consumer/BodyClassRegistry.java
```
期望: 21 个 Map.entry + 现有 supplychain import 已 alphabetical（ArchiveInfo3102 / BankCheckDay3116 / ContractInfo3101 / DzpzInfo3000 / HxqyCreditAmt3112 / InvoCheckQuery3007 / PzCheckQuery3107 / QyRegister3109 / RzApplyInfo3105 / RzReturnInfo3009）。

- [ ] **Step 2: 编写失败测试 — BodyClassRegistryTest +6 case**

```java
// fep-web/src/test/java/com/puchain/fep/web/outbound/consumer/BodyClassRegistryTest.java
// 既有方法 supportingMoreThan16Entries 保持不变（P4-MSG-E T2 reviewer 反馈：future-proof 命名）
// 既有 @ParameterizedTest registry_should_resolve_known_msgNos 扩展 6 case

@ParameterizedTest(name = "[{index}] msgNo={0} → {1}")
@MethodSource("supplychainQueryWireMatrix")
@DisplayName("3001-3006 supplychain query body 注册解析")
void shouldResolveSupplychainQueryBodies(final String msgNo, final Class<?> expected) {
    assertThat(registry.resolve(msgNo))
            .as("BodyClassRegistry.resolve(\"%s\") 必须返回 %s（P4-MSG-F T1 注册）",
                    msgNo, expected.getSimpleName())
            .isEqualTo(expected);
}

static Stream<Arguments> supplychainQueryWireMatrix() {
    return Stream.of(
            Arguments.of("3001", ProgressQuery3001.class),
            Arguments.of("3002", ProgressQueryReturn3002.class),
            Arguments.of("3003", PzInfoQuery3003.class),
            Arguments.of("3004", PzInfoReturn3004.class),
            Arguments.of("3005", QyAccQuery3005.class),
            Arguments.of("3006", QyAccQueryReturn3006.class)
    );
}
```

注意 import:
```java
import com.puchain.fep.processor.body.supplychain.ProgressQuery3001;
import com.puchain.fep.processor.body.supplychain.ProgressQueryReturn3002;
import com.puchain.fep.processor.body.supplychain.PzInfoQuery3003;
import com.puchain.fep.processor.body.supplychain.PzInfoReturn3004;
import com.puchain.fep.processor.body.supplychain.QyAccQuery3005;
import com.puchain.fep.processor.body.supplychain.QyAccQueryReturn3006;
```

- [ ] **Step 3: 运行测试确认失败**

```bash
JAVA_HOME=/usr/local/opt/openjdk@17 PATH=/usr/local/opt/openjdk@17/bin:$PATH \
  ./mvnw test -pl fep-web -am \
  -Dtest=BodyClassRegistryTest#shouldResolveSupplychainQueryBodies \
  -Dsurefire.failIfNoSpecifiedTests=false
```
期望: 6 case 全部 FAIL — `BodyClassRegistry.resolve("3001")` 抛 `FepBusinessException` OUTBOUND_5107_BODY_CLASS_NOT_FOUND（未注册）

> **红线遵守**:
> - `feedback_bg_bash_path_inheritance`: JAVA_HOME 显式前缀
> - `feedback_surefire3_failifno_specified_tests_param_rename`: `-Dsurefire.failIfNoSpecifiedTests=false`
> - `feedback_pipe_tail_deadlock_with_bg_bash`: 若 run_in_background:true 必须 `> /tmp/log.txt 2>&1`（本步前台）

- [ ] **Step 4: 修改 BodyClassRegistry — 注册 6 entries**

按 alphabetic 顺序（与既有 supplychain import 风格一致）插入到 supplychain import 段：

```java
import com.puchain.fep.processor.body.supplychain.ArchiveInfo3102;
import com.puchain.fep.processor.body.supplychain.BankCheckDay3116;
import com.puchain.fep.processor.body.supplychain.ContractInfo3101;
import com.puchain.fep.processor.body.supplychain.DzpzInfo3000;
import com.puchain.fep.processor.body.supplychain.HxqyCreditAmt3112;
import com.puchain.fep.processor.body.supplychain.InvoCheckQuery3007;
import com.puchain.fep.processor.body.supplychain.ProgressQuery3001;        // ← 新增
import com.puchain.fep.processor.body.supplychain.ProgressQueryReturn3002;  // ← 新增
import com.puchain.fep.processor.body.supplychain.PzCheckQuery3107;
import com.puchain.fep.processor.body.supplychain.PzInfoQuery3003;          // ← 新增
import com.puchain.fep.processor.body.supplychain.PzInfoReturn3004;         // ← 新增
import com.puchain.fep.processor.body.supplychain.QyAccQuery3005;           // ← 新增
import com.puchain.fep.processor.body.supplychain.QyAccQueryReturn3006;     // ← 新增
import com.puchain.fep.processor.body.supplychain.QyRegister3109;
import com.puchain.fep.processor.body.supplychain.RzApplyInfo3105;
import com.puchain.fep.processor.body.supplychain.RzReturnInfo3009;
```

REGISTRY Map.ofEntries 按 msgNo 升序插入到 3000 之后 / 3007 之前等位置（保持升序）：

```java
private static final Map<String, Class<?>> REGISTRY = Map.ofEntries(
        Map.entry("1001", CompanyInfoRequest1001.class),
        Map.entry("1004", CompanyAuthFileTransfer1004.class),
        Map.entry("1101", DataTransfer1101.class),
        Map.entry("1102", DataTransferCheckBatchRequest1102.class),
        Map.entry("1103", CompanyInfoBatchRequest1103.class),
        Map.entry("1104", CompanyAuthFileBatchTransfer1104.class),
        Map.entry("2001", CompanyInfoResponse2001.class),
        Map.entry("2004", CompanyAuthFileResponse2004.class),
        Map.entry("2102", DataTransferCheckBatchResponse2102.class),
        Map.entry("2103", CompanyInfoBatchResponse2103.class),
        Map.entry("2104", CompanyAuthFileBatchResponse2104.class),
        Map.entry("3000", DzpzInfo3000.class),
        Map.entry("3001", ProgressQuery3001.class),             // ← 新增 P4-MSG-F T1
        Map.entry("3002", ProgressQueryReturn3002.class),       // ← 新增 P4-MSG-F T1
        Map.entry("3003", PzInfoQuery3003.class),               // ← 新增 P4-MSG-F T1
        Map.entry("3004", PzInfoReturn3004.class),              // ← 新增 P4-MSG-F T1
        Map.entry("3005", QyAccQuery3005.class),                // ← 新增 P4-MSG-F T1
        Map.entry("3006", QyAccQueryReturn3006.class),          // ← 新增 P4-MSG-F T1
        Map.entry("3007", InvoCheckQuery3007.class),
        Map.entry("3009", RzReturnInfo3009.class),
        Map.entry("3101", ContractInfo3101.class),
        Map.entry("3102", ArchiveInfo3102.class),
        Map.entry("3105", RzApplyInfo3105.class),
        Map.entry("3107", PzCheckQuery3107.class),
        Map.entry("3109", QyRegister3109.class),
        Map.entry("3112", HxqyCreditAmt3112.class),
        Map.entry("3116", BankCheckDay3116.class));
```

Javadoc `<ul>` 列表同步更新 21→27 entries（插入位置在 3000 与 3007 之间）：

```java
 *   <li>3000 → {@link DzpzInfo3000}（电子凭证信息报送，P4-MSG-B T4）</li>
 *   <li>3001 → {@link ProgressQuery3001}（业务进展实时查询请求，P4-MSG-F T1）</li>
 *   <li>3002 → {@link ProgressQueryReturn3002}（业务进展查询回执，P4-MSG-F T1）</li>
 *   <li>3003 → {@link PzInfoQuery3003}（电子凭证融资状态查询请求，P4-MSG-F T1）</li>
 *   <li>3004 → {@link PzInfoReturn3004}（电子凭证融资状态查询回执，P4-MSG-F T1）</li>
 *   <li>3005 → {@link QyAccQuery3005}（对公账户状态查询请求，P4-MSG-F T1）</li>
 *   <li>3006 → {@link QyAccQueryReturn3006}（对公客户状态查询回执，P4-MSG-F T1）</li>
 *   <li>3007 → {@link InvoCheckQuery3007}（受理单位发起发票核验请求，P4-MSG-B T1）</li>
```

类级 Javadoc 文本段（line 60-66 区域）也需修订：
- "本 Plan E T1 起 1001/2001/1004/2004 已注册（P4-MSG-E T1，企业查询 8/12→12/12 收尾）；21 entries / 下一阶段（P4-MSG-F+）处理 9XXX 通用报文与 supplychain 剩余。"
- 改为: "P4-MSG-F T1 注册 3001-3006 供应链查询 6 报文（业务进展查询/凭证融资状态查询/对公账户查询请求+回执 3 对）；27 entries / 下一阶段 P4-MSG-G（batch2）候选 3008/3020/3103/3108 / P4-MSG-H（batch3）候选 3113/3115/3120 + 9XXX 通用报文（9000/9005/9006/9007/9008/9009/9100/9120）独立 Plan 处理。"

- [ ] **Step 4b: 升级既有 `supportingMoreThan21Entries` 测试为 27 entries**

**实测确认 (v2 修订, 2026-05-13)**: `BodyClassRegistryTest.java:204` 方法名为 `registry_shouldUseMapOfEntries_supportingMoreThan21Entries`，line 206 断言 `assertThat(countRegistryEntries()).isEqualTo(21)`。本 Task 必须在同一 commit 内重命名 + 升级断言：

```java
// fep-web/src/test/java/com/puchain/fep/web/outbound/consumer/BodyClassRegistryTest.java
// 方法名 21→27
void registry_shouldUseMapOfEntries_supportingMoreThan27Entries() throws Exception {
    // ...
    assertThat(countRegistryEntries()).isEqualTo(27);  // ← 21→27
}
```

> **红线 `feedback_plan_must_grep_actual_api` 修订**: 起草时 Plan v1 推断"可能 16"基于过时记忆，未 grep 实测 — 已在 v2 修订实测纠正。Plan v3 之前 implementer 起 Task 时再 grep 实测确认无 v2-v3 间 baseline drift。
>
> **实测命令** (Task 启动前 implementer 必跑):
> ```bash
> grep -n "supportingMoreThan\|isEqualTo([0-9]" fep-web/src/test/java/com/puchain/fep/web/outbound/consumer/BodyClassRegistryTest.java
> ```
> 期望: line 204 方法名 + line 206 isEqualTo(21)。若 baseline drift 已变为其他值，调整为 `<实测值> → 27`。

- [ ] **Step 5: 运行测试确认通过**

```bash
JAVA_HOME=/usr/local/opt/openjdk@17 PATH=/usr/local/opt/openjdk@17/bin:$PATH \
  ./mvnw test -pl fep-web -am \
  -Dtest='BodyClassRegistryTest' \
  -Dsurefire.failIfNoSpecifiedTests=false
```
期望: BUILD SUCCESS，全 BodyClassRegistryTest case 绿（含新增 6 case + 升级后 `supportingMoreThan27Entries` 反映 27 entries）。

- [ ] **Step 6: 提交**

```bash
git add fep-web/src/main/java/com/puchain/fep/web/outbound/consumer/BodyClassRegistry.java \
        fep-web/src/test/java/com/puchain/fep/web/outbound/consumer/BodyClassRegistryTest.java
git commit -m "$(cat <<'EOF'
feat(web): register 3001-3006 supplychain query bodies in BodyClassRegistry (P4-MSG-F T1)

注册 6 个供应链查询 outbound Body POJO 映射，REGISTRY 21→27 entries：
- 3001 ProgressQuery3001（业务进展实时查询请求）
- 3002 ProgressQueryReturn3002（业务进展查询回执）
- 3003 PzInfoQuery3003（电子凭证融资状态查询请求）
- 3004 PzInfoReturn3004（电子凭证融资状态查询回执）
- 3005 QyAccQuery3005（对公账户状态查询请求）
- 3006 QyAccQueryReturn3006（对公客户状态查询回执）

设计依据：FEP 双角色 outbound symmetric registration 模式（与 P4-MSG-E
1001/2001/1004/2004 注册策略一致）— 3001/3005 信息服务角色 outbound，
3002/3006 银行角色 outbound，3003 双角色 outbound，3004 注册兜底用于
inbound 3003 响应回路（运行时 fep.role 决定 active dispatch 集合）。

PRD: v1.3 §3.2 报文结构 + §4.6 报文方向 + §4.2 实时类业务报文
FR: FR-MSG-3001/3002/3003/3004/3005/3006 outbound wire-out 维度
Tests: BodyClassRegistryTest +6 cases shouldResolveSupplychainQueryBodies

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

### Task 2: OutboundWireShapeDispatcher +6 case（3001-3006 wire-shape 路由） `模式 B`

**PRD 依据:** v1.3 §3.2 报文结构 + §4.2 实时类业务报文 + §4.6 报文方向 + §4.7 处理模式 1 同步
**追溯 ID:** FR-MSG-3001 / FR-MSG-3002 / FR-MSG-3003 / FR-MSG-3004 / FR-MSG-3005 / FR-MSG-3006

**验收标准:**
1. `dispatcher.describeFor("3001")` → `WireShapeDescriptor("RealHead3001", RequestBusinessHead.class, false)`
2. `dispatcher.describeFor("3002")` → `WireShapeDescriptor("RealHead3002", ResponseBusinessHead.class, true)`
3. `dispatcher.describeFor("3003")` → `WireShapeDescriptor("RealHead3003", RequestBusinessHead.class, false)`
4. `dispatcher.describeFor("3004")` → `WireShapeDescriptor("RealHead3004", ResponseBusinessHead.class, true)`
5. `dispatcher.describeFor("3005")` → `WireShapeDescriptor("RealHead3005", RequestBusinessHead.class, false)`
6. `dispatcher.describeFor("3006")` → `WireShapeDescriptor("RealHead3006", ResponseBusinessHead.class, true)`
7. `dispatcher.isRegisteredOutboundMsgNo("3001"..."3006")` → 全 true
8. `dispatcher.describeFor("9999")` 抛 `FepBusinessException` + `OUTBOUND_5108_MSGNO_INVALID`（既有 default 分支不变）
9. Javadoc 21→27 entries + 类级文档 "未登记的 inbound-only 3003/3005" 表述移除 / 修订
10. wire-shape 4 类目段同步更新（"RealHead + RequestBusinessHead + false: 1001/1004/3000/3001/3003/3005/3007/3009"等）

**Files:**
- Modify: `fep-converter/src/main/java/com/puchain/fep/converter/wire/OutboundWireShapeDispatcher.java`
- Modify: `fep-converter/src/test/java/com/puchain/fep/converter/wire/OutboundWireShapeDispatcherTest.java`（若文件不存在则 Create — grep 实测先确认）

- [ ] **Step 1: 实测 OutboundWireShapeDispatcherTest 是否存在**

```bash
ls fep-converter/src/test/java/com/puchain/fep/converter/wire/OutboundWireShapeDispatcherTest.java || \
  echo "TEST FILE MISSING — implementer 需 Create"
```

若不存在，本 Step 实测后 Step 2 改为 Create 全新测试文件（沿用 P4-MSG-A 测试模板，参考 `OutboundBatchWireTest` 命名）。

- [ ] **Step 2: 编写失败测试 — OutboundWireShapeDispatcherTest +6 case**

```java
// fep-converter/src/test/java/com/puchain/fep/converter/wire/OutboundWireShapeDispatcherTest.java
// 既有 @Test 保留，本 Step 仅扩展 supplychain query +6 case

@ParameterizedTest(name = "[{index}] msgNo={0} → head={1}({2}), result={3}")
@MethodSource("supplychainQueryShapeMatrix")
@DisplayName("3001-3006 supplychain query wire-shape 路由")
void describeFor_shouldRouteSupplychainQuery(
        final String msgNo,
        final String expectedHeadElementName,
        final Class<? extends RequestBusinessHead> expectedHeadClass,
        final boolean expectedRequiresResultCode) {

    final WireShapeDescriptor desc = dispatcher.describeFor(msgNo);
    assertThat(desc.headElementName()).isEqualTo(expectedHeadElementName);
    assertThat(desc.headClass()).isEqualTo(expectedHeadClass);
    assertThat(desc.requiresResultCode()).isEqualTo(expectedRequiresResultCode);
    assertThat(dispatcher.isRegisteredOutboundMsgNo(msgNo))
            .as("msgNo=%s 必须在 isRegisteredOutboundMsgNo true", msgNo)
            .isTrue();
}

static Stream<Arguments> supplychainQueryShapeMatrix() {
    return Stream.of(
            Arguments.of("3001", "RealHead3001", RequestBusinessHead.class, false),
            Arguments.of("3002", "RealHead3002", ResponseBusinessHead.class, true),
            Arguments.of("3003", "RealHead3003", RequestBusinessHead.class, false),
            Arguments.of("3004", "RealHead3004", ResponseBusinessHead.class, true),
            Arguments.of("3005", "RealHead3005", RequestBusinessHead.class, false),
            Arguments.of("3006", "RealHead3006", ResponseBusinessHead.class, true)
    );
}
```

- [ ] **Step 3: 运行测试确认失败**

```bash
JAVA_HOME=/usr/local/opt/openjdk@17 PATH=/usr/local/opt/openjdk@17/bin:$PATH \
  ./mvnw test -pl fep-converter -am \
  -Dtest=OutboundWireShapeDispatcherTest#describeFor_shouldRouteSupplychainQuery \
  -Dsurefire.failIfNoSpecifiedTests=false
```
期望: 6 case FAIL — `describeFor("3001")` 抛 OUTBOUND_5108_MSGNO_INVALID（未在 switch 集合）

- [ ] **Step 4: 修改 OutboundWireShapeDispatcher.describeFor + isRegisteredOutboundMsgNo**

`describeFor` switch 扩展（在既有 case 标签上 append msgNo）：

```java
return switch (msgNo) {
    case "1001", "1004",
         "3000", "3001", "3003", "3005",
         "3007", "3009" -> new WireShapeDescriptor(
            "RealHead" + msgNo, RequestBusinessHead.class, false);
    case "1101", "1102", "1103", "1104",
         "3102", "3105", "3107", "3109", "3112", "3116" -> new WireShapeDescriptor(
            "BatchHead" + msgNo, RequestBusinessHead.class, false);
    case "2001", "2004",
         "3002", "3004", "3006" -> new WireShapeDescriptor(
            "RealHead" + msgNo, ResponseBusinessHead.class, true);
    case "3101", "2102", "2103", "2104" -> new WireShapeDescriptor(
            "BatchHead" + msgNo, ResponseBusinessHead.class, true);
    default -> throw new FepBusinessException(
            FepErrorCode.OUTBOUND_5108_MSGNO_INVALID,
            "msgNo 不在 27 上行报文集合: " + msgNo);
};
```

`isRegisteredOutboundMsgNo` switch 扩展：

```java
return switch (msgNo) {
    case "1001", "1004", "2001", "2004",
         "1101", "1102", "1103", "1104",
         "2102", "2103", "2104",
         "3000", "3001", "3002", "3003", "3004", "3005", "3006",
         "3007", "3009", "3101", "3102",
         "3105", "3107", "3109", "3112", "3116" -> true;
    default -> false;
};
```

Javadoc 类级修订（line 10 区域 "21 上行报文" → "27 上行报文"）：

```java
/**
 * 出站报文 wire-shape 路由 — 单一真相源，决定 27 上行报文 msgNo 对应的
 * head 元素名 / head 类型 / 是否要求 ResultCode（PRD v1.3 §3.2 + §4.6）。
 *
 * <p>实测自 27 份 XSD（{@code fep-processor/src/main/resources/xsd/{1001,1004,1101,1102,1103,1104,
 * 2001,2004,2102,2103,2104,3000,3001,3002,3003,3004,3005,3006,3007,3009,3101,3102,3105,
 * 3107,3109,3112,3116}.xsd}）：</p>
 * <ul>
 *   ... 既有 entries 不动 ...
 *   <li>3001 → {@code RealHead3001} + {@link RequestBusinessHead}（业务进展实时查询请求，P4-MSG-F T2）</li>
 *   <li>3002 → {@code RealHead3002} + {@link ResponseBusinessHead}（业务进展查询回执，含 ResultCode，P4-MSG-F T2）</li>
 *   <li>3003 → {@code RealHead3003} + {@link RequestBusinessHead}（凭证融资状态查询请求，P4-MSG-F T2）</li>
 *   <li>3004 → {@code RealHead3004} + {@link ResponseBusinessHead}（凭证融资状态查询回执，含 ResultCode，P4-MSG-F T2）</li>
 *   <li>3005 → {@code RealHead3005} + {@link RequestBusinessHead}（对公账户状态查询请求，P4-MSG-F T2）</li>
 *   <li>3006 → {@code RealHead3006} + {@link ResponseBusinessHead}（对公客户状态查询回执，含 ResultCode，P4-MSG-F T2）</li>
 * </ul>
 *
 * <p>4 类 wire-shape：</p>
 * <ul>
 *   <li>RealHead + RequestBusinessHead + false: 1001/1004/3000/3001/3003/3005/3007/3009</li>
 *   <li>RealHead + ResponseBusinessHead + true: 2001/2004/3002/3004/3006</li>
 *   <li>BatchHead + RequestBusinessHead + false: 1101/1102/1103/1104/3102/3105/3107/3109/3112/3116（既有，本 Plan 不动）</li>
 *   <li>BatchHead + ResponseBusinessHead + true: 2102/2103/2104/3101（既有，本 Plan 不动；3101 历史归类于此类目，参见 PRD v1.3 §4.6）</li>
 * </ul>
 *
 * <p>非法 msgNo（{@code null} / 非 4 位数字 / 不在 27 集合）抛
 * {@link FepBusinessException} + {@link FepErrorCode#OUTBOUND_5108_MSGNO_INVALID}。</p>
 */
```

`isRegisteredOutboundMsgNo` Javadoc 修订（line 89-94 区域 "未登记的（例如 inbound-only 的 3003 / 3005 / 9000）走 legacy 路径"）→

```java
/**
 * 判断 msgNo 是否在已登记的 27 上行报文集合内。
 *
 * <p>用于 {@code BatchMessageProcessorService.wrapBodyInCfx} 等 inbound + outbound
 * 共用方法识别"是否为已登记 outbound msgNo"，未登记的（例如 9000/9005 心跳类
 * 通用报文）走 legacy 路径，避免对 inbound 链路产生回归。</p>
 *
 * @param msgNo 4 位数字报文号；{@code null} 或非法格式返回 {@code false}
 * @return {@code true} 当且仅当 msgNo 是 27 上行报文之一
 */
```

- [ ] **Step 5: 运行测试确认通过**

```bash
JAVA_HOME=/usr/local/opt/openjdk@17 PATH=/usr/local/opt/openjdk@17/bin:$PATH \
  ./mvnw test -pl fep-converter -am \
  -Dtest='OutboundWireShapeDispatcherTest' \
  -Dsurefire.failIfNoSpecifiedTests=false
```
期望: BUILD SUCCESS，全 case 绿。

- [ ] **Step 6: 提交**

```bash
git add fep-converter/src/main/java/com/puchain/fep/converter/wire/OutboundWireShapeDispatcher.java \
        fep-converter/src/test/java/com/puchain/fep/converter/wire/OutboundWireShapeDispatcherTest.java
git commit -m "$(cat <<'EOF'
feat(converter): route 3001-3006 supplychain query in OutboundWireShapeDispatcher (P4-MSG-F T2)

OutboundWireShapeDispatcher 21→27 上行报文 + 6 新 case：
- 3001/3003/3005 加入 RealHead+RequestBusinessHead+false 既有类目
- 3002/3004/3006 加入 RealHead+ResponseBusinessHead+true（P4-MSG-E 新类目）

Javadoc 修订：
- 类级 "21 上行报文" → "27 上行报文"
- wire-shape 4 类目段同步追加 3001/3003/3005 / 3002/3004/3006
- isRegisteredOutboundMsgNo "inbound-only 3003/3005" 过时注释移除

PRD: v1.3 §3.2 + §4.6 + §4.7 模式 1 同步
FR: FR-MSG-3001/3002/3003/3004/3005/3006 outbound wire-shape 路由
Tests: OutboundWireShapeDispatcherTest +6 cases describeFor_shouldRouteSupplychainQuery

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

### Task 3: 6 XsdValidationTest 落盘（3001-3006 fixture-driven schema validate） `模式 A`

**PRD 依据:** v1.3 §3.2 报文结构 + §5.3.2.7 (3005→3006) + §5.3.2.9 (3003→3004) + §5.3.2.15 (3001→3002)
**追溯 ID:** FR-MSG-3001 / FR-MSG-3002 / FR-MSG-3003 / FR-MSG-3004 / FR-MSG-3005 / FR-MSG-3006

**验收标准（每个 *XsdValidationTest 共 3 case）:**
1. `validXmlWithAllRequiredFields_shouldPass` — 全字段满足 DataType.xsd 约束的 XML schema validate 通过
2. `validXmlWithOptionalOmitted_shouldPass` — 仅必填字段（无 ExtInfo / ReturnMemo 等 minOccurs=0）schema validate 通过
3. `invalidXmlMissingRequiredField_shouldReject` — 缺少必填字段（如 SerialNo 或 hxqyCode）schema validate 拒绝（SAXException）

> **红线 `feedback_fixture_data_must_satisfy_xsd_constraints`**: fixture XML 所有 fields 必须按 DataType.xsd 实测约束（length / minLength / maxLength / pattern）填写。negative case 故意违反单一约束，其他字段必须满足。
>
> **DataType.xsd 实测约束参考**（已在 CompanyInfoRequest1001XsdValidationTest Javadoc 沉淀）：
> - SerialNo length=30（流水号）
> - SendNodeCode/DesNodeCode NodeCode length=14
> - SendOrgCode OrgCode length=14
> - qyName length=2..100
> - qyCode length=18（USCI）
> - pzNo（凭证编号）length=… (需 grep DataType.xsd 实测)
> - ReturnCode Number1to2 / Number3to5
> - ReturnMemo String0to200 / String0to500

**Files:** 6 个新文件 Create:
- `fep-processor/src/test/java/com/puchain/fep/processor/validation/ProgressQuery3001XsdValidationTest.java`
- `fep-processor/src/test/java/com/puchain/fep/processor/validation/ProgressQueryReturn3002XsdValidationTest.java`
- `fep-processor/src/test/java/com/puchain/fep/processor/validation/PzInfoQuery3003XsdValidationTest.java`
- `fep-processor/src/test/java/com/puchain/fep/processor/validation/PzInfoReturn3004XsdValidationTest.java`
- `fep-processor/src/test/java/com/puchain/fep/processor/validation/QyAccQuery3005XsdValidationTest.java`
- `fep-processor/src/test/java/com/puchain/fep/processor/validation/QyAccQueryReturn3006XsdValidationTest.java`

- [ ] **Step 1: 先 grep 实测 DataType.xsd 全部相关 simpleType 约束**

```bash
cd /Users/muzhou/FEP_v1.0_wt-p4-msg-f-batch1
# 实测每个 simpleType 的 length / pattern / minLen / maxLen 约束（不可 head 截断，全集必读）
grep -A 4 'simpleType name="\(SerialNo\|NodeCode\|OrgCode\|TransitionNo\|qyName\|qyCode\|pzNo\|AccName\|AccNumber\|Number1to2\|Result\|AddWord\|String0to100\|String0to200\)"' fep-processor/src/main/resources/xsd/DataType.xsd
```

**实测预期结果**（v2 修订实测沉淀，2026-05-13）:
- SerialNo: length=30
- NodeCode: length=14（SendNodeCode / DesNodeCode 共用）
- OrgCode: length=14（SendOrgCode 用）
- TransitionNo: length=30
- qyName: minLen=2 / maxLen=100（hxqyName 用）
- qyCode: length=18 USCI（hxqyCode 用，3001-3004）
- pzNo: 凭证编号约束（3003/3004 用，本步骤 grep 实测得出）
- AccName: 账户名称约束（3005/3006 qyAccName 用）
- AccNumber: 账户号约束（3005/3006 qyAccCode 用）
- Number1to2: 1-2 位数字（QueryType 用，3001/3002）
- **Result: length=5（5 位数字，3002/3004/3006 ResponseHead 必填）**
- AddWord: 可选附言（ResponseHead minOccurs=0）

- [ ] **Step 1b: 实测 RequestHead 与 ResponseHead 字段集（Base.xsd）**

```bash
grep -A 20 'complexType name="RequestHead"\|complexType name="ResponseHead"' fep-processor/src/main/resources/xsd/Base.xsd
```

**实测沉淀（v2 修订）**:

- `RequestHead`（3001/3003/3005 用）字段集（required，按 sequence 顺序）：
  1. SendOrgCode (OrgCode, length=14)
  2. EntrustDate (Date, YYYYMMDD)
  3. TransitionNo (TransitionNo, length=30)

- `ResponseHead`（3002/3004/3006 用）字段集（required，按 sequence 顺序）：
  1. SendOrgCode (OrgCode, length=14)
  2. EntrustDate (Date, YYYYMMDD)
  3. TransitionNo (TransitionNo, length=30)
  4. **Result (Number length=5, required) — 例 "10000" 成功 / "13001" 字段缺失等**
  5. AddWord (可选 minOccurs=0)

**关键差异**: 3002/3004/3006 fixture 必须含 Result 元素（5 位数字），否则 schema validate 立即 reject — Plan v2 T3 Step 2 起 fixture 分两套模板。

- [ ] **Step 2: 编写失败测试 — ProgressQuery3001XsdValidationTest（请求侧 template，3 case）**

参考 `CompanyInfoRequest1001XsdValidationTest` 既有结构（line 33-100+）。**请求侧 fixture template**（3001/3003/3005 用 RealHead{n} type="RequestHead"，仅 SendOrgCode/EntrustDate/TransitionNo 3 字段）— 注意 `RealHead3001` PascalCase + `ProgressQuery3001` body root PascalCase:

```java
package com.puchain.fep.processor.validation;

import com.puchain.fep.converter.type.MessageType;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * XSD-driven validation tests for the 3001 (outbound ProgressQuery) message body.
 *
 * <p>Coverage (P4-MSG-F T3，对齐 {@link CompanyInfoRequest1001XsdValidationTest} pattern):</p>
 * <ul>
 *     <li>Valid 3001 XML with all required + optional ExtInfo fields passes schema validation</li>
 *     <li>Valid 3001 XML with optional ExtInfo omitted still passes</li>
 *     <li>Invalid 3001 XML missing required hxqyCode is rejected</li>
 *     <li>{@link XsdSchemaRegistry} resolves {@link MessageType#MSG_3001}</li>
 * </ul>
 *
 * <p>DataType.xsd 实测约束（fixture 全部满足）: SerialNo(length=30) /
 * NodeCode(length=14) / qyName(minLen=2, maxLen=100) / qyCode(length=18, USCI) /
 * Number1to2(QueryType 1-2 位数字) / String0to100(QueryKey).</p>
 *
 * <p>3001.xsd root element 命名: PascalCase {@code ProgressQuery3001} (P4-Plan-C T1 实测)。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class ProgressQuery3001XsdValidationTest extends AbstractXsdValidationTest {

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
                <RealHead3001>
                  <SendOrgCode>30500000000000</SendOrgCode>
                  <EntrustDate>20260513</EntrustDate>
                  <TransitionNo>20260513</TransitionNo>
                </RealHead3001>
                <ProgressQuery3001>
                  <SerialNo>SN3001202605130000000000000001</SerialNo>
                  <SendNodeCode>A1000142000001</SendNodeCode>
                  <DesNodeCode>A1000143000104</DesNodeCode>
                  <hxqyName>核心企业测试有限公司</hxqyName>
                  <hxqyCode>91110000ABCDEFGH12</hxqyCode>
                  <QueryType>1</QueryType>
                  <QueryKey>BIZ20260513000001</QueryKey>
                  <ExtInfo>
                    <k>customKey</k>
                    <v>customValue</v>
                  </ExtInfo>
                </ProgressQuery3001>
              </MSG>
            </CFX>
            """;

    private static final String VALID_OPTIONAL_OMITTED_XML = VALID_FULL_FIELDS_XML
            .replaceAll("(?s)<ExtInfo>.*?</ExtInfo>", "");

    private static final String INVALID_MISSING_HXQYCODE_XML = VALID_FULL_FIELDS_XML
            .replaceAll("(?s)<hxqyCode>[^<]*</hxqyCode>\\s*", "");

    @Test
    void validXmlWithAllRequiredFields_shouldPass() {
        assertThat(validator.validate(MessageType.MSG_3001,
                VALID_FULL_FIELDS_XML.getBytes(StandardCharsets.UTF_8)))
                .isTrue();
    }

    @Test
    void validXmlWithOptionalExtInfoOmitted_shouldPass() {
        assertThat(validator.validate(MessageType.MSG_3001,
                VALID_OPTIONAL_OMITTED_XML.getBytes(StandardCharsets.UTF_8)))
                .isTrue();
    }

    @Test
    void invalidXmlMissingRequiredHxqyCode_shouldReject() {
        assertThatThrownBy(() ->
                validator.validate(MessageType.MSG_3001,
                        INVALID_MISSING_HXQYCODE_XML.getBytes(StandardCharsets.UTF_8)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("hxqyCode");
    }
}
```

> **注**: `validator.validate(MessageType, byte[])` API 已在 AbstractXsdValidationTest 既有 — implementer Step 2 前必须 grep `XsdValidator.java` 确认 API signature 一致；P4-Plan-C T1 测试与本 fixture 风格完全匹配，不再重复。

- [ ] **Step 2b: 回执侧 fixture template（3002/3004/3006 用 RealHead{n} type="ResponseHead"，含 Result 5 位 + 可选 AddWord）**

```java
// 回执侧 RealHead 模板（替换 Step 2 的 RealHead 段，body 段按各 code 替换）
<RealHead3002>
  <SendOrgCode>30500000000000</SendOrgCode>
  <EntrustDate>20260513</EntrustDate>
  <TransitionNo>SN3002202605130000000000000001</TransitionNo>
  <Result>10000</Result>
  <AddWord>查询成功</AddWord>
</RealHead3002>
```

> **关键差异 (v2 修订)**: 3002/3004/3006 RealHead 含 `Result` (length=5, required) + 可选 `AddWord` (minOccurs=0)。**实测 v2 grep**:
> ```bash
> grep -E "RealHead.*type=\"" fep-processor/src/main/resources/xsd/{3002,3004,3006}.xsd
> ```
> 期望: `RealHead3002 type="ResponseHead"` 等 — 3 个 .xsd 文件中 RealHead 引用 ResponseHead 类型。

- [ ] **Step 3: 同 pattern 落盘 3002 / 3003 / 3004 / 3005 / 3006 XsdValidationTest 5 个**

复用 Step 2/2b 模板，按下表替换关键字段：

| Test 文件 | MessageType | root element 大小写 | RealHead type | body 字段（必填）| 缺失字段 negative case |
|----------|------------|------------------|--------------|----------------|--------------------|
| ProgressQueryReturn3002XsdValidationTest | MSG_3002 | `ProgressQueryReturn3002` PascalCase | **ResponseHead** (含 Result+可选 AddWord) | SerialNo / SendNodeCode / DesNodeCode / hxqyName / hxqyCode / QueryType / QueryKey / **ReturnCode** | ReturnCode |
| PzInfoQuery3003XsdValidationTest | MSG_3003 | **`pzInfoQuery3003`** camelCase | RequestHead | SerialNo / SendNodeCode / DesNodeCode / hxqyName / hxqyCode / **pzNo** | pzNo |
| PzInfoReturn3004XsdValidationTest | MSG_3004 | **`pzInfoReturn3004`** camelCase | **ResponseHead** | SerialNo / SendNodeCode / DesNodeCode / hxqyName / hxqyCode / pzNo / **ReturnCode** | ReturnCode |
| QyAccQuery3005XsdValidationTest | MSG_3005 | **`qyAccQuery3005`** camelCase | RequestHead | SerialNo / SendNodeCode / DesNodeCode / **qyAccName** / **qyAccCode** | qyAccCode |
| QyAccQueryReturn3006XsdValidationTest | MSG_3006 | **`qyAccQueryReturn3006`** camelCase | **ResponseHead** | SerialNo / SendNodeCode / DesNodeCode / qyAccName / qyAccCode / **AccReturnCode** | AccReturnCode |

> **关键提醒**: 3002/3004/3006 fixture 必须将 RealHead 段替换为 Step 2b 的 ResponseHead 模板（含 Result）；3003/3005 复用 Step 2 RequestHead 模板。混用会触发 XSD validate 立即 reject（红线 `feedback_fixture_data_must_satisfy_xsd_constraints`）。
>
> **body 字段约束**: implementer Step 3 起前必须先看 Step 1 实测的 DataType.xsd 约束表（pzNo / AccName / AccNumber 等具体 length / pattern），不可推测。

- [ ] **Step 4: 运行测试确认通过**

```bash
JAVA_HOME=/usr/local/opt/openjdk@17 PATH=/usr/local/opt/openjdk@17/bin:$PATH \
  ./mvnw test -pl fep-processor -am \
  -Dtest='ProgressQuery3001XsdValidationTest,ProgressQueryReturn3002XsdValidationTest,PzInfoQuery3003XsdValidationTest,PzInfoReturn3004XsdValidationTest,QyAccQuery3005XsdValidationTest,QyAccQueryReturn3006XsdValidationTest' \
  -Dsurefire.failIfNoSpecifiedTests=false
```
期望: BUILD SUCCESS，18 case 全绿（6 file × 3 case）。

xmllint 双向验证（红线 `feedback_xsd_validation_gap`）：

```bash
cd /Users/muzhou/FEP_v1.0_wt-p4-msg-f-batch1/fep-processor/src/main/resources/xsd
# 把 Step 2 fixture VALID_FULL_FIELDS_XML 落盘成 /tmp/3001-valid.xml 后执行
xmllint --noout --schema 3001.xsd /tmp/3001-valid.xml
# 期望 stdout: /tmp/3001-valid.xml validates
```

- [ ] **Step 5: 提交**

```bash
git add fep-processor/src/test/java/com/puchain/fep/processor/validation/ProgressQuery3001XsdValidationTest.java \
        fep-processor/src/test/java/com/puchain/fep/processor/validation/ProgressQueryReturn3002XsdValidationTest.java \
        fep-processor/src/test/java/com/puchain/fep/processor/validation/PzInfoQuery3003XsdValidationTest.java \
        fep-processor/src/test/java/com/puchain/fep/processor/validation/PzInfoReturn3004XsdValidationTest.java \
        fep-processor/src/test/java/com/puchain/fep/processor/validation/QyAccQuery3005XsdValidationTest.java \
        fep-processor/src/test/java/com/puchain/fep/processor/validation/QyAccQueryReturn3006XsdValidationTest.java
git commit -m "$(cat <<'EOF'
test(processor): add XSD validation tests for 3001-3006 supplychain query bodies (P4-MSG-F T3)

6 个新 XsdValidationTest 落盘（共 18 case，沿用
CompanyInfoRequest1001XsdValidationTest pattern）：
- ProgressQuery3001XsdValidationTest (PascalCase root)
- ProgressQueryReturn3002XsdValidationTest (PascalCase)
- PzInfoQuery3003XsdValidationTest (camelCase root)
- PzInfoReturn3004XsdValidationTest (camelCase)
- QyAccQuery3005XsdValidationTest (camelCase)
- QyAccQueryReturn3006XsdValidationTest (camelCase)

Fixture 实测：3001-3006 XSD 混合命名（3001/3002 PascalCase + 3003-3006
camelCase）— 与 P4-Plan-C T1 已落地的 VALID_3NN_XML_TEMPLATE 风格一致。
所有字段按 DataType.xsd 实测约束（SerialNo length=30 / qyCode length=18 /
NodeCode length=14 等）填写满足 schema validation；每文件 1 valid +
1 optional-omitted + 1 missing-required negative case。

PRD: v1.3 §3.2 + §5.3.2.7 + §5.3.2.9 + §5.3.2.15
FR: FR-MSG-3001..3006 XSD validation 维度
Tests: 18 cases passing

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

### Task 4: OutboundSupplychainQueryWireTest — 6 case 端到端 wire bean IT `模式 A`

**PRD 依据:** v1.3 §3.2 报文结构 + §4.2 实时类业务报文 + §4.6 报文方向 + §4.7 模式 1 同步
**追溯 ID:** FR-MSG-3001/3002/3003/3004/3005/3006 wire chain 集成

**验收标准:**
1. Spring context boot 成功（@SpringBootTest，fep.collector.scheduling.enabled=false + management.health.redis.enabled=false 复用 P4-MSG-E 既有 properties）
2. 对 3001-3006 每个 msgNo 同时验证：
   - `registry.resolve(msgNo)` → 期望 Body POJO Class
   - `dispatcher.describeFor(msgNo).headElementName()` → `RealHead{n}`
   - `dispatcher.describeFor(msgNo).headClass()` → `RequestBusinessHead.class` (3001/3003/3005) 或 `ResponseBusinessHead.class` (3002/3004/3006)
   - `dispatcher.describeFor(msgNo).requiresResultCode()` → false (1xxx 请求) 或 true (2xxx/30N4/30N6 回执)
   - `dispatcher.isRegisteredOutboundMsgNo(msgNo)` → true
3. 6 case 全绿，单次 Spring context 启动复用（红线 `feedback_plan_perf_budget_must_account_for_call_overhead`）

**Perf 预算估算**（红线 `feedback_plan_perf_budget_must_account_for_call_overhead`）:
- @SpringBootTest 单 context 启动: ~65s（fep-web 实测）
- 单 case 执行: ~50ms
- T4 总耗时 = 1 × 65s + 6 × 50ms ≈ 65.3s（context 启动主导）
- 整 fep-web test phase（reactor verify）: ~2-15min（含其他 IT）

**Files:**
- Create: `fep-web/src/test/java/com/puchain/fep/web/outbound/consumer/OutboundSupplychainQueryWireTest.java`

- [ ] **Step 1: 编写完整测试（沿用 OutboundEnterpriseQueryRealtimeWireTest 模板）**

```java
package com.puchain.fep.web.outbound.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import com.puchain.fep.converter.model.RequestBusinessHead;
import com.puchain.fep.converter.model.ResponseBusinessHead;
import com.puchain.fep.converter.wire.OutboundWireShapeDispatcher;
import com.puchain.fep.converter.wire.WireShapeDescriptor;
import com.puchain.fep.processor.body.supplychain.ProgressQuery3001;
import com.puchain.fep.processor.body.supplychain.ProgressQueryReturn3002;
import com.puchain.fep.processor.body.supplychain.PzInfoQuery3003;
import com.puchain.fep.processor.body.supplychain.PzInfoReturn3004;
import com.puchain.fep.processor.body.supplychain.QyAccQuery3005;
import com.puchain.fep.processor.body.supplychain.QyAccQueryReturn3006;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.stream.Stream;

/**
 * P4-MSG-F T4 — 6 供应链查询报文 outbound wire 链路 bean 集成 IT（参数化 × 6 case）。
 *
 * <p>对每个供应链查询 msgNo（3001/3002/3003/3004/3005/3006）验证 Spring context 内
 * {@link BodyClassRegistry} + {@link OutboundWireShapeDispatcher} 两个 wire 链路 bean
 * 协调一致：</p>
 *
 * <ul>
 *   <li>3001/3003/3005 上行请求 → RealHead{n} + RequestBusinessHead + no result</li>
 *   <li>3002/3004/3006 上行回执 → RealHead{n} + ResponseBusinessHead + result=true（与
 *       P4-MSG-E 2001/2004 同类目）</li>
 *   <li>6 msgNo 全部 isRegisteredOutboundMsgNo=true</li>
 * </ul>
 *
 * <p>与既有 {@code OutboundEnterpriseQueryRealtimeWireTest} (P4-MSG-E T4) /
 * {@code OutboundBatchWireTest} (P4-MSG-A T3) pattern 完全对齐：仅断言 registry+dispatcher
 * bean 协调，不调用 {@code OutboundCfxEnvelopeBuilder}。XSD validate 由 T3 6
 * {@code *XsdValidationTest} 内存路径覆盖；端到端 XML 装配由 {@code OutboundCfxEnvelopeBuilderTest}
 * 既有覆盖（21+6 msgNos 自动通过 dispatcher 路由覆盖）。</p>
 *
 * <p>共享 {@code @TestPropertySource} 配置（{@code fep.collector.scheduling.enabled=false} +
 * {@code management.health.redis.enabled=false}）以触发 Spring context 缓存复用，与
 * {@link OutboundEnterpriseQueryRealtimeWireTest} 同 context。</p>
 *
 * <p>PRD 依据: v1.3 §3.2 报文结构 + §4.2 实时类业务报文 + §4.6 报文方向 + §4.7 模式 1 同步。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
@TestPropertySource(properties = {
        "fep.collector.scheduling.enabled=false",
        "management.health.redis.enabled=false"
})
class OutboundSupplychainQueryWireTest {

    @Autowired
    private BodyClassRegistry registry;

    @Autowired
    private OutboundWireShapeDispatcher dispatcher;

    static Stream<Arguments> supplychainQueryWireMatrix() {
        return Stream.of(
                Arguments.of("3001", ProgressQuery3001.class,
                        "RealHead3001", RequestBusinessHead.class, false),
                Arguments.of("3002", ProgressQueryReturn3002.class,
                        "RealHead3002", ResponseBusinessHead.class, true),
                Arguments.of("3003", PzInfoQuery3003.class,
                        "RealHead3003", RequestBusinessHead.class, false),
                Arguments.of("3004", PzInfoReturn3004.class,
                        "RealHead3004", ResponseBusinessHead.class, true),
                Arguments.of("3005", QyAccQuery3005.class,
                        "RealHead3005", RequestBusinessHead.class, false),
                Arguments.of("3006", QyAccQueryReturn3006.class,
                        "RealHead3006", ResponseBusinessHead.class, true)
        );
    }

    @ParameterizedTest(name = "[{index}] msgNo={0} → body={1}, head={2}({3}), result={4}")
    @MethodSource("supplychainQueryWireMatrix")
    @DisplayName("6 supplychain query outbound wire bean 协调")
    void wire_supplychainQuery_should_resolve_consistently(
            final String msgNo,
            final Class<?> expectedBodyClass,
            final String expectedHeadElementName,
            final Class<? extends RequestBusinessHead> expectedHeadClass,
            final boolean expectedRequiresResultCode) {

        // BodyClassRegistry: msgNo → SupplyChain query POJO Class
        assertThat(registry.resolve(msgNo))
                .as("BodyClassRegistry.resolve(\"%s\") 必须返回 %s（P4-MSG-F T1 注册）",
                        msgNo, expectedBodyClass.getSimpleName())
                .isEqualTo(expectedBodyClass);

        // OutboundWireShapeDispatcher: msgNo → wire-shape 描述符
        final WireShapeDescriptor desc = dispatcher.describeFor(msgNo);
        assertThat(desc.headElementName())
                .as("msgNo=%s headElementName（与 %s.xsd 一致）", msgNo, msgNo)
                .isEqualTo(expectedHeadElementName);
        assertThat(desc.headClass())
                .as("msgNo=%s headClass（%s）", msgNo,
                        msgNo.endsWith("1") || msgNo.endsWith("3") || msgNo.endsWith("5")
                                ? "请求" : "回执")
                .isEqualTo(expectedHeadClass);
        assertThat(desc.requiresResultCode())
                .as("msgNo=%s requiresResultCode（%s）", msgNo,
                        expectedRequiresResultCode ? "回执含 ReturnCode" : "请求不带 ReturnCode")
                .isEqualTo(expectedRequiresResultCode);

        // BatchMessageProcessorService.resolveHeadElementName 路径
        assertThat(dispatcher.isRegisteredOutboundMsgNo(msgNo))
                .as("msgNo=%s 必须在 isRegisteredOutboundMsgNo true，否则 inbound 路径会走 legacy fallback",
                        msgNo)
                .isTrue();
    }
}
```

- [ ] **Step 2: 运行测试**

```bash
JAVA_HOME=/usr/local/opt/openjdk@17 PATH=/usr/local/opt/openjdk@17/bin:$PATH \
  ./mvnw test -pl fep-web -am \
  -Dtest='OutboundSupplychainQueryWireTest' \
  -Dsurefire.failIfNoSpecifiedTests=false
```
期望: BUILD SUCCESS，6 case 全绿，Spring context 启动 ~65s + case 执行 ~300ms。

- [ ] **Step 3: 提交**

```bash
git add fep-web/src/test/java/com/puchain/fep/web/outbound/consumer/OutboundSupplychainQueryWireTest.java
git commit -m "$(cat <<'EOF'
test(web): add 6 supplychain query outbound wire IT (P4-MSG-F T4)

OutboundSupplychainQueryWireTest @ParameterizedTest × 6 case 端到端 wire
链路 bean 集成 IT：对 3001-3006 每个 msgNo 验证 BodyClassRegistry +
OutboundWireShapeDispatcher 协调一致 — Body POJO Class 解析 / wire-shape
描述符 / isRegisteredOutboundMsgNo flag。

复用 P4-MSG-E OutboundEnterpriseQueryRealtimeWireTest 与 P4-MSG-A
OutboundBatchWireTest 测试 pattern，@TestPropertySource fep.collector.
scheduling.enabled=false + management.health.redis.enabled=false 触发
Spring context 缓存共享。

PRD: v1.3 §3.2 + §4.2 + §4.6 + §4.7 模式 1 同步
FR: FR-MSG-3001..3006 outbound wire chain 集成
Tests: 6 cases passing

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

### Task 5: closing — Plan 阶段收口（不强制四步，挪到 session-end） `模式 A`

**目标:** Plan 内 Task 1-4 全 ship 后做最小回归 + push origin/main，正式四步收尾（Simplify / 8 维技术文档 / Daily Report / git push 确认）挪到 `/session-end` 阶段执行（红线 `feedback_mandatory_post_task` + `feedback_four_step_closing` 修订 2026-05-06）。

**Files:**
- 无新文件；本 Task 仅做 git operations + 最小回归

- [ ] **Step 1: 最小回归验证（red line `feedback_plan_regression_scope_explicit` strong + minimum 两层）**

**Strong** (本机或 GHA CI 二选一，按 sandbox 状态决定):

本机:
```bash
JAVA_HOME=/usr/local/opt/openjdk@17 PATH=/usr/local/opt/openjdk@17/bin:$PATH \
  ./mvnw verify --batch-mode --no-transfer-progress > /tmp/p4-msg-f-verify.log 2>&1 &
# 等通知，不轮询；红线 feedback_pipe_tail_deadlock_with_bg_bash — 直接 redirect to file，禁止 | tail
```
期望: 9/9 模块 BUILD SUCCESS / JaCoCo coverage 持平 / Checkstyle 0 / SpotBugs+find-sec-bugs 0 / ArchUnit PASS。

GHA CI fallback（红线 `feedback_mvn_sandbox_exit144_pattern`）:
```bash
git push origin HEAD:feat/p4-msg-f-supplychain-query-batch1
# 然后 gh run watch (或登录 GitHub 查看 Actions)
```

**Minimum** (本机沙盒 exit 144 时使用):
```bash
JAVA_HOME=/usr/local/opt/openjdk@17 PATH=/usr/local/opt/openjdk@17/bin:$PATH \
  ./mvnw test -pl fep-web -am \
  -Dtest='OutboundSupplychainQueryWireTest,BodyClassRegistryTest,OutboundEnterpriseQueryRealtimeWireTest,OutboundBatchWireTest' \
  -Dsurefire.failIfNoSpecifiedTests=false
```
期望: BUILD SUCCESS for 这 4 个 wire-test（含本 Plan +6 case + 既有 16 case 不回归）。

- [ ] **Step 2: rebase 检查 — origin/main 是否前进**

```bash
git fetch origin
git log --oneline origin/main..HEAD
git log --oneline HEAD..origin/main
```
若 origin/main 已前进（含别会话 commit）→ `git rebase origin/main`，rebase 后重跑 Step 1 minimum。

- [ ] **Step 3: merge to main 并 push（subagent-driven-development 自动 merge 或由 muzhou 决策）**

按 muzhou 决策（推荐 fast-forward merge 到 main，复用 P4-MSG-E 实践）:

```bash
git checkout main
git merge --ff-only feat/p4-msg-f-supplychain-query-batch1
git push origin main
```

> **muzhou 决策项**: merge 策略走 ff-only 还是 PR review 走 GitHub Actions CI 兜底？AskUserQuestion 单点询问。

- [ ] **Step 4: worktree 清理（红线 `feedback_worktree_for_parallel_work` 闭环）**

```bash
cd /Users/muzhou/FEP_v1.0
git worktree remove /Users/muzhou/FEP_v1.0_wt-p4-msg-f-batch1
git branch -d feat/p4-msg-f-supplychain-query-batch1
git worktree list  # 验证残留
```

- [ ] **Step 5: 触发 /session-end skill**

session-end 6-phase 全流程对本 Plan 闭环：
- Phase 1: 合规检查 + worktree 实测 + PHASE_HISTORY metadata drift 检查
- Phase 2: 四步收尾 — Simplify 三审（reuse / quality / efficiency 并行 subagent） + 8 维技术文档（api / modules / database 等）+ Daily Report `docs/daily_reports/2026-05-13-p4-msg-f-progress-report.md` + git push 确认
- Phase 3: 候选新红线扫描（本 Plan 触发的新教训 → memory）
- Phase 4: 报告 + 衔接 prompt
- Phase 5: 衔接提示词（next-session-prompt for batch2 候选 ramp-up）
- Phase 6: PRD 矩阵自动更新 — FR-MSG-3001..3006 outbound wire-out 列追加 ✅ + line 6 "最后更新" 段补 P4-MSG-F 闭环

> **红线遵守**: `feedback_session_end_auto_invoke_on_task_completion` + `feedback_session_end_full_workflow_required` + `feedback_session_end_prd_matrix_auto_update` + `feedback_session_end_phase_history_metadata_drift_check`

---

## 自检清单

### 1. PRD 覆盖度 ✅
- FR-MSG-3001 / FR-MSG-3002 / FR-MSG-3003 / FR-MSG-3004 / FR-MSG-3005 / FR-MSG-3006 全部 outbound wire-out 维度覆盖
- 不在本 Plan: 3001-3006 实际 outbound TLQ 发送（依赖 ScheduledOutboundProducer + fep.role 配置，独立 ticket）

### 2. 安全边界 ✅
- 本 Plan 不涉及 SM2/SM3/SM4/密钥/脱敏/审计完整性
- 无 ⛔ 模式 E Task

### 3. 占位符扫描 ✅
- 无 "TBD" / "TODO" / "类似 Task N"
- T3 5 个 sibling test 文件用表格 + Step 2 模板说明，无占位

### 4. 类型一致性 ✅
- ProgressQuery3001 / ProgressQueryReturn3002 / PzInfoQuery3003 / PzInfoReturn3004 / QyAccQuery3005 / QyAccQueryReturn3006 — 全 grep 实测 import 路径 (`com.puchain.fep.processor.body.supplychain.*`)
- RequestBusinessHead / ResponseBusinessHead — 既有 (`com.puchain.fep.converter.model.*`)
- WireShapeDescriptor — 既有 (`com.puchain.fep.converter.wire.*`)
- AbstractXsdValidationTest — 既有基类 (`com.puchain.fep.processor.validation.*`)
- MessageType.MSG_3001..MSG_3006 — 既有 enum（grep 已实测）

### 5. 测试命令可执行 ✅
- `BodyClassRegistryTest#shouldResolveSupplychainQueryBodies` — Task 1 新 @ParameterizedTest method
- `OutboundWireShapeDispatcherTest#describeFor_shouldRouteSupplychainQuery` — Task 2
- 6 个 `*XsdValidationTest` 类名匹配 T3 落盘文件名
- `OutboundSupplychainQueryWireTest` — Task 4 新文件
- 所有 `-Dtest=` 参数与实测类/方法名一致

### 6. CLAUDE.md 更新 ✅
- T5 session-end Phase 2 Daily Report 阶段更新 "当前项目状态" 段
- 新增 P4-MSG-F 阶段记录 + BodyClassRegistry 21→27 + OutboundWireShapeDispatcher 21→27
- "下一步候选" 段移除 #8 P4-MSG-F batch1 + 添加 batch2/batch3 提示

### 7. 验收标准完整性 ✅
- Task 1-4 每 Task 5-10 条验收标准，全部从 PRD §3.2 / §4.6 / §4.7 / §5.3.2 推导
- 断言值（"RealHead3001" / RequestBusinessHead.class / true/false）从 XSD 实测推导，可手算验证

### 8. 共享工具类无遗漏 ✅
- AbstractXsdValidationTest 复用（fep-processor 既有）
- BodyClassRegistry / OutboundWireShapeDispatcher 既有，本 Plan 仅 append
- 无新增共享工具

### 9. 核心类职责边界 ✅
- BodyClassRegistry 依赖 0 / 行数 119→128（< 200 上限）
- OutboundWireShapeDispatcher 依赖 0 / 行数 112→124（< 200 上限）
- 无新增依赖 ≥3 的 Service

### 10. Worktree 触发条件自检 ✅
- ② 与 `feat/p4-msg-d-2101-inbound-consumer` 已签字 worktree 并存 — 命中
- ⑤ T4 fep-web @SpringBootTest context ~65s + 6 case verify ≥5min — 命中
- ① 跨 fep-processor + fep-converter + fep-web 3 模块 — 命中
- 头部 `执行 Worktree:` 字段已填具体路径与分支
- T5 Step 4 含 `git worktree remove` 实测命令

---

## 风险登记

| 风险 | 触发条件 | 缓解 |
|------|---------|------|
| 本机 mvn 沙盒 exit 144 | macOS Claude Code 沙盒 syscall 拦截累积 ≥2 次 | GHA CI 远端兜底（T5 Step 1 strong fallback） |
| macOS APFS fork classloader race | 本机 reactor verify 间歇 fail | 单模块 -am 跑 / GHA CI 兜底 |
| baseline drift（origin/main 前进） | long review cycle ≥4h / 跨日 / muzhou 并行会话 push | 每轮 grep 实测 HEAD（T5 Step 2 rebase 检查） |
| 3004 注册争议 | PRD 双角色均"被动接收"但 Plan 注册为 outbound | §设计背景段已记录设计依据 + reviewer 反馈通道 / 必要时 muzhou 拍板 reduce 至 5 codes |
| subagent commit-step dual-fail | implementer subagent 跑 mvn timeout 不 commit | Plan 内 subagent prompt 严格禁用 run_in_background + 主对话务实接管 fallback（红线 `feedback_subagent_commit_step_dual_fail_pattern`） |
| Surefire param 误用 | `-DfailIfNoTests=false` (Surefire 2.x) vs `-Dsurefire.failIfNoSpecifiedTests=false` (3.x) | Plan 内所有 mvn test 命令统一用 3.x 参数（红线 `feedback_surefire3_failifno_specified_tests_param_rename`） |

---

## 执行交接

**⚠️ 重要**: Plan 不能直接执行。必须先经 **Plan 评审 + 批准签字** 流程（见 `docs/guides/plan-review-checklist.md`）。

### 步骤 1: AI 独立评审

派发独立 AI 评审 agent（plan-eng-review skill 或 code-reviewer agent）：
- 输入: 本 Plan 全文 + PRD v1.3 §3.2 / §4.6 / §4.7 / §5.3.2 + `docs/guides/plan-review-checklist.md` 7 项清单
- 输出: ✅ 通过项 / ❌ 问题项

### 步骤 2: 人工 Plan Approver 签字

AI 评审通过后，提交给 muzhou:
1. 阅读 Plan + AI 评审报告
2. 对照 PRD §3.2 + §4.6 + §4.7 抽样核对（3001/3003 双角色方向 + 3004 注册争议是否接受 + camelCase root 混合命名）
3. 决策: 批准 / 驳回 / 部分修改
4. Plan 文件末尾追加批准签字

### 步骤 3: 执行方式选择

签字后，muzhou 选择执行方式:

**1. Subagent 驱动（推荐）** — 每 Task 派发独立 subagent，Task 之间 muzhou 审核质量
**2. 内联执行** — 当前会话 superpowers:executing-plans 逐步执行，关键节点暂停审核

**禁止: 未签字直接执行**。

---

## 修订记录

### v2 修订 (2026-05-13, 应 reviewer 报告)

- **BLOCKER #1 修订**: Task 1 §298-302 Note 推断"`supportingMoreThan16Entries` 16"基于过时记忆，实测纠正为真实方法名 `registry_shouldUseMapOfEntries_supportingMoreThan21Entries` (line 204) + 断言 `isEqualTo(21)` (line 206) → Plan 升级到 27 + 提供实测命令；新增 Step 4b 明确重命名 + 断言升级动作（红线 `feedback_plan_must_grep_actual_api` 实证 + v2 修订 sealed）
- **MAJOR #2 修订**: Task 3 Step 1 grep 命令扩展为全集 simpleType 实测 + 新增 Step 1b RequestHead/ResponseHead 字段集实测；ResponseHead 含 `Result` (length=5 required) + 可选 `AddWord` 已明示
- **MAJOR #2 隐含 BLOCKER 升级**: Task 3 Step 2 fixture template 拆分为请求侧 (RequestHead, 3001/3003/3005) + Step 2b 回执侧 (ResponseHead, 3002/3004/3006) 两套，避免 3002/3004/3006 fixture 用 RequestHead 模板导致 schema validate 立即 reject
- **MAJOR #3 修订**: Task 3 Step 1 grep 命令显式列出 qyCode / qyName / pzNo / AccName / AccNumber / SerialNo / Result / AddWord 8 个 simpleType（fallback 不再 head 截断）
- **MAJOR #4 修订**: Task 2 Step 4 wire-shape 4 类目段 BatchHead 行加 "(既有，本 Plan 不动)" 标注 + 3101 历史归类参考 PRD §4.6
- **MINOR #5 修订**: §设计背景 Worktree 触发条件 6 项 checkbox 一致化为 [x]/[ ]，原 ① 命中但 [ ] 矛盾的混合状态消除
- **MINOR #7 修订**: T1 Step 4 BodyClassRegistry 类级 Javadoc 修订段落扩写 next-batch 候选清单（P4-MSG-G batch2 = 3008/3020/3103/3108 / P4-MSG-H batch3 = 3113/3115/3120 / 9XXX 通用独立 Plan）
- **MINOR #5 worktree §59 冲突盲区**: 头部补 "本 Plan 改动范围 BodyClassRegistry + OutboundWireShapeDispatcher 与 P4-MSG-D 改动范围 InboundMessageDispatcher 无文件级冲突，实测确认"

### v2 仍未消化项（implementer 实施阶段处理）

- **MINOR #5 (reviewer 报告)**: 已修订（worktree checkbox 一致化）
- **MINOR #8**: T5 Step 1 Strong 本机 mvn 用 `&` 后台 — Plan 已合规（`run_in_background:true` + 等通知模式 + 直接 redirect to file），等价于 reviewer 期望
- **NIT #9 #10**: 术语统一 / XSD 行号补全 — implementer 实施阶段顺手补

---

## 批准签字

- **AI 独立评审 v1**: ✅ general-purpose reviewer 完成（2026-05-13），输出 1 BLOCKER + 3 MAJOR + 4 MINOR + 2 NIT
- **Plan 修订 v1→v2**: ✅ BLOCKER #1（`supportingMoreThan21Entries`→27） + MAJOR #2/#3/#4 + 隐含 BLOCKER（ResponseHead fixture 含 Result）+ MINOR #5/#7 修订完成
- **AI 独立评审 v2**: 跳过（v1 reviewer 建议"修订后 muzhou 直接复审签字即可"，muzhou 拍板接受）
- **muzhou Plan Approver 签字**: ✅ APPROVED 2026-05-14
- **签字日期**: 2026-05-14
- **签字范围**: P4-MSG-F 全 Plan（T1-T5），含：
  - 3004 注册策略 — 对称注册 ALL 6 codes（与 P4-MSG-E 2001/2004 一致）
  - ResponseHead fixture v2 修订 — 3002/3004/3006 RealHead 必含 `Result` (length=5)
  - BodyClassRegistryTest `supportingMoreThan21Entries`→`supportingMoreThan27Entries` + 断言 21→27 升级
  - DataType.xsd / Base.xsd 全集 simpleType + complexType 实测沉淀
  - wire-shape 4 类目段 "(既有，本 Plan 不动)" 标注
  - 全部 v2 修订项
- **执行授权**: 待 muzhou 选定执行方式（subagent-driven-development 推荐 / executing-plans 备选）后启动
