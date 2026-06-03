# FEP P4-MSG-N 9005 心跳 dispatcher 覆盖注册实施计划 v0.2

> **执行方式:** 使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐 Task 执行。步骤使用 `- [ ]` 复选框跟踪。

## v0.1 → v0.2 修订纪要（2026-06-03）

v0.1 起草（grep 实测全 API）→ Round 1 AI 评审 santa-method **REVISE**（API 真实性/不改 builder 论断/head-only 测试全 PASS，仅第 4 项 cross-task 枚举不全）→ boil-lake 补列（红线 `feedback_concern_boil_lake_when_cheap_and_safe` + `feedback_cross_task_obsolete_fixture_assumption_when_set_extended`）：
1. **补列 2 处生产代码 doc 腐烂**（reviewer 独立 grep 揪出，与 P4-MSG-M BodyClassRegistryTest 同型翻车点）：`OutboundWireShapeDispatcher.java:176-178`（9005 "未登记"反例注释注册后说谎）+ `BodyClassRegistry.java:105`（9005 列"后续待处理"ship 后腐烂）→ 枚举表新增 (B) 段 + Step 3 第 6 点 + Step 4
2. `OutboundWireShapeDispatcherTest` stale "39" 实测 3 处（:22/:26/:36）非 1 处
3. Step 4 grep 复核加扫生产代码（`grep -rn '9005' fep-converter/src/main fep-web/src/main`），不止 test count 断言

**目标:** PRD v1.3 §4.5 通用 9 报文 — 9005 节点心跳 outbound dispatcher 覆盖注册（精确镜像 9006/9008 双轨），无功能行为，无 OutboundCfxEnvelopeBuilder 改造。

**前置依赖:**
- P4-MSG-L（9006/9008 双轨 dispatcher 覆盖先例）— origin/main `3f45c67`
- P4-MSG-M（9020 + REGISTERED_MSG_NO_COUNT=40 + cross-task 计数同步先例）— origin/main `42ce336`

**执行 Worktree:** `/Users/muzhou/FEP_v1.0_wt-p4-msg-n`（分支 `feat/p4-msg-n-9005-heartbeat-dispatcher`，触发条件第 2 项 + 第 6 项）
> 触发条件第 2 项：与多别会话 Plan 并存；第 6 项：多并行 AI 会话活跃 — 红线 `feedback_worktree_isolates_fs_not_logic_domain`

**Baseline:** origin/main `42ce336`（2026-06-03 实测；P4-MSG-M 后 REGISTERED_MSG_NO_COUNT=40。红线 `feedback_baseline_drift_during_long_review_cycle` — 签字与实施跨时须重测）

**架构:**
- 9005 是 COMMON BIDIRECTIONAL 节点心跳（`MessageType.MSG_9005` 已存），与 9006(登录)/9008(登出) 同由 `NodeLifecycleManager`(fep-transport) 管理，走 TlqNodeLoginService/MessageEncoder **直接路径**（双轨）。
- **outbound dispatcher 覆盖注册**（T1）: `REAL_HEAD_REQUEST_MSG_NOS` +9005（11→12，同 9006/9008 类目 RealHead+RequestBusinessHead+requiresResultCode=false）；`REGISTERED_MSG_NO_COUNT` 40→41。
- **9005 head-only**（9005.xsd MSG 仅 RealHead9005 type=RequestHead，无 body 元素）。
- **不改 OutboundCfxEnvelopeBuilder**：9005（像 9006/9008）走直接路径不经 builder；`build("9005")` 抛 OUTBOUND_5107 与 9006/9008 一致（双轨预期非缺陷）。**不入** BodyClassRegistry。**无 inbound**（head-only 无 body + 心跳 inbound 经 NodeLifecycleManager transport 层，出范围）。
- 真 `XsdValidator` 验证 9005 head-only 完整 CFX envelope（红线 `feedback_xsd_validator_requires_full_envelope_redline` + `feedback_xsd_compliance_fix_real_validator_on_sut`）。

**技术栈:** Java 17 / Spring Boot 3.x / JAXB 4 / Maven / JUnit 5 / AssertJ

**AI 协同模式:** A（90%）— 纯 dispatcher 注册扩展 + head-only 测试，无安全代码。**无 ⛔ 模式 E**。

**PR 大小预估:** T1 ~80 LOC（dispatcher 改 + head-only 测试 + cross-task fixup）。

---

## 设计背景

### PRD 依据 + 关键实测发现

PRD v1.3 §4.5 — 9005 连通性测试（实为**节点心跳**）。`MessageType.MSG_9005`("9005","连通性测试",COMMON,BIDIRECTIONAL,null)。

- 9005 是节点心跳，与 9006/9008 同由 `NodeLifecycleManager`(fep-transport) 管（Javadoc 实测："负责节点登录(9006)、登出(9008)和心跳(9005)处理"）。
- 已有连通性模块 `TlqConnectivityController/Service`（PRD §5.7.5）做 TCP ping 占位，"实际 9005 心跳发送待 P1 TLQ SDK 接入"——**独立 initiative 不在本 Plan**。
- 9006/9008 实测**未入 BodyClassRegistry**（双轨），XSD 测试经 `wrapCfxTemplate` 手动 wrap 不经 OutboundCfxEnvelopeBuilder → 9005 精确镜像，**不需改 builder**。

### XSD 字段实测（head-only，红线 `feedback_plan_xsd_field_table_default_not_optional`）

#### 9005 RealHead9005（RequestHead，无 body）

| 字段 | XSD type | minOccurs | 业务约束 | required |
|---|---|:---:|---|:---:|
| SendOrgCode | OrgCode | 1 | 发送机构码 | true |
| EntrustDate | Date | 1 | 委托日期 | true |
| TransitionNo | TransitionNo | 1 | 流水号 | true |

source: `9005.xsd:34`（RealHead9005 type=RequestHead）+ Base.xsd RequestHead（SendOrgCode/EntrustDate/TransitionNo，**无 Result**）。**MSG 下无 body 元素**（head-only，9005.xsd:29-40 MSG complexType 仅 RealHead9005）。

### 实测 baseline（grep 确认，origin/main 42ce336）

- `REAL_HEAD_REQUEST_MSG_NOS` = `Set.of("1001","1004","3000","3001","3003","3005","3007","3009","9000","9006","9008")` 11 元素 → +9005 = 12
- `REGISTERED_MSG_NO_COUNT` = 40 → 41
- `describeFor` REAL_HEAD_REQUEST 分支返回 `WireShapeDescriptor("RealHead"+msgNo, RequestBusinessHead.class, false)`
- `wrapCfxTemplate(srcNode, desNode, app, msgNo, msgId, corrMsgId, workDate, msgInnerXml)` 8 参（`AbstractXsdValidationTest:110`）
- RealHead9005 wire head = `RequestBusinessHead`（经 JAXBElement QName "RealHead9005"，同 9006）

### 下游 cross-task 计数断言 + doc 腐烂全枚举（红线 `feedback_cross_task_obsolete_fixture_assumption_when_set_extended`，P4-MSG-M BodyClassRegistryTest 漏检教训）

加 9005 到 REAL_HEAD_REQUEST(→12) + COUNT(40→41) 影响以下**所有**下游断言/Javadoc，T1 须**同 commit**全部同步：

**(A) 测试 count/size 断言:**

| 文件 | 行 | 改动 |
|---|---|---|
| `OutboundWireShape9006And9008XsdComplianceTest` | :82 | `isEqualTo(40)` → 41 |
| 同上 | :89 | `hasSize(11)` → 12 |
| 同上 | :28 | Javadoc `REGISTERED_MSG_NO_COUNT=39` → 41（39 是 P4-MSG-M 遗留 stale，一并修正） |
| `OutboundWireShape9020XsdComplianceTest` | :67 | `isEqualTo(40)` → 41 |
| 同上 | :28 | Javadoc `=40` → 41 |
| `OutboundWireShapeDispatcherTest` | :22 / :36 | Javadoc "覆盖 39 上行报文"/"不在 39 集合" 当前总数声明 **stale → 41**（P4-MSG-M 遗留）|
| 同上 | :26 | "P4-MSG-L 起 39 含 9006/9008" 是**历史里程碑 narrative（非 stale，勿改）** → 在其后**追加新行** "P4-MSG-N 起 41 含 9005 心跳"；implementer 须 grep 确认该测试有无 count 断言一并改 |
| `BodyClassRegistryTest` | :172 | Javadoc "REGISTERED_MSG_NO_COUNT 40" → 41（仅 Javadoc 引用；growth-guard 断言 38 **不变**，9005 不入 BodyClassRegistry） |

**(B) ⚠️ 生产代码 doc 腐烂（v0.2 Round 1 AI 评审补列 — 与 P4-MSG-M 同型翻车点，最高优先）:**

| 文件 | 行 | 改动 |
|---|---|---|
| `OutboundWireShapeDispatcher.java` | :176-178 | 现 Javadoc 把 **9005 当"未登记"反例**举例（"未登记的（例如 9005 心跳类通用报文，9005.xsd MSG 下无 body 元素）走 legacy 路径"）。9005 注册后 `isRegisteredOutboundMsgNo("9005")` 返 true，**此 comment 说谎** → 改写：换其他真·未登记 msgNo 举例（如**未在任何类目的 msgNo**，implementer grep 确认一个真未登记的），或删 9005 具名举例保留语义 |
| `BodyClassRegistry.java` | :105 | Javadoc "后续 9XXX 通用报文（9005/9006/9007/9008/9009）独立 Plan 处理" — 9005 ship 后不再 pending，移除 9005（建议连带已 ship 的 9006/9008 一并校正为准确状态） |

**BodyClassRegistry REGISTRY / BodyClassRegistryTest growth-guard(38) 不受影响**（9005 镜像 9006/9008 不入 BodyClassRegistry；仅上方 :105 Javadoc + :172 测试 Javadoc 引用需校正）。

---

## 文件结构设计

| 文件路径 | 职责 | 操作 | AI 模式 |
|---|---|---|:---:|
| `fep-converter/.../wire/OutboundWireShapeDispatcher.java` | REAL_HEAD_REQUEST_MSG_NOS +9005 + count 40→41 + Javadoc | 修改 | A |
| `fep-processor/.../wire/OutboundWireShape9005XsdComplianceTest.java` | 9005 head-only 真 XsdValidator 测试 | 新建 | A |
| `fep-processor/.../wire/OutboundWireShape9006And9008XsdComplianceTest.java` | count 40→41 + size 11→12 + Javadoc（cross-task） | 修改 | A |
| `fep-processor/.../wire/OutboundWireShape9020XsdComplianceTest.java` | count 40→41 + Javadoc（cross-task） | 修改 | A |
| `fep-converter/.../wire/OutboundWireShapeDispatcherTest.java` | Javadoc count 3 处 + narrative（cross-task） | 修改 | A |
| `fep-web/.../outbound/consumer/BodyClassRegistryTest.java` | Javadoc count 引用 40→41（cross-task，仅 doc） | 修改 | A |
| `fep-web/.../outbound/consumer/BodyClassRegistry.java` | Javadoc :105 移除 9005 "后续待处理"（v0.2 评审补，生产 doc） | 修改 | A |

**总: 2 修改生产（dispatcher + BodyClassRegistry doc）+ 1 新测试 + 4 cross-task test fixup = 7 文件改动**

### 共享工具类清单 / 核心类职责边界

无新增共享工具类（复用 `AbstractXsdValidationTest.SHARED_VALIDATOR` + `wrapCfxTemplate`）；无新增 ≥3 依赖 Service。

---

## Task 1: 9005 head-only dispatcher 覆盖注册 + 真 XSD 测试 + cross-task 同步 `模式 A`

**PRD 依据:** v1.3 §4.5 通用 9 报文 — 9005 节点心跳
**追溯 ID:** FR-MSG-9005

**验收标准（从 PRD + XSD 实测推导）:**
1. `dispatcher.describeFor("9005")` → `headElementName()="RealHead9005"` + `headClass()==RequestBusinessHead.class` + `requiresResultCode()==false`
2. `REGISTERED_MSG_NO_COUNT == 41`
3. `REAL_HEAD_REQUEST_MSG_NOS` size==12 含 "9005"
4. 9005 head-only 完整 CFX envelope（RealHead9005: SendOrgCode/EntrustDate/TransitionNo，**无 body**）→ 真 `XsdValidator.validate(MessageType.MSG_9005, bytes)` → `result.valid()==true`
5. RealHead9005 违反约束（如 TransitionNo 非法）→ `result.valid()==false`
6. 全部 cross-task 计数断言同步（6006/9008 test 40→41/11→12 + 9020 test 40→41 + 2 Javadoc 引用），全 reactor verify GREEN

**Files:** （见文件结构表）

- [ ] **Step 1: 编写失败测试 OutboundWireShape9005XsdComplianceTest**

> 镜像已 ship `OutboundWireShape9006And9008XsdComplianceTest`，复用 `AbstractXsdValidationTest.SHARED_VALIDATOR` + `wrapCfxTemplate`。**9005 head-only — msgInnerXml 仅 RealHead9005，无 body POJO marshal**。implementer 先 `Read` 参照测试 + grep `wrapCfxTemplate` 签名 + 9005.xsd RealHead9005/RequestHead 字段精确约束（OrgCode/Date/TransitionNo facet）。

```java
package com.puchain.fep.processor.wire;

import com.puchain.fep.converter.model.RequestBusinessHead;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.converter.wire.OutboundWireShapeDispatcher;
import com.puchain.fep.converter.wire.WireShapeDescriptor;
import com.puchain.fep.processor.validation.AbstractXsdValidationTest;
import com.puchain.fep.processor.validation.ValidationResult;
import com.puchain.fep.processor.validation.XsdValidator;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P4-MSG-N T1 — 9005 节点心跳 head-only outbound wire-shape + 真 {@link XsdValidator} 合规测试。
 *
 * <p>9005.xsd MSG 仅 RealHead9005（RequestHead）无 body 元素 — head-only。镜像 9006/9008 双轨
 * （dispatcher 覆盖注册，走 TlqNodeLoginService 直接路径，不经 OutboundCfxEnvelopeBuilder，
 * 不入 BodyClassRegistry）。红线 feedback_xsd_validator_requires_full_envelope_redline：
 * 完整 CFX envelope（HEAD + MSG + RealHead9005，MSG 内无 body）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class OutboundWireShape9005XsdComplianceTest {

    private static final String SRC_NODE = "10000000000001";
    private static final String DES_NODE = "A1000143000104";
    private static final String APP = "HNDEMP";

    private OutboundWireShapeDispatcher dispatcher;
    private XsdValidator validator;

    @BeforeEach
    void setUp() {
        dispatcher = new OutboundWireShapeDispatcher();
        validator = AbstractXsdValidationTest.SHARED_VALIDATOR;
    }

    @Test
    void describeFor_9005_shouldReturnRealHeadRequest() {
        WireShapeDescriptor desc = dispatcher.describeFor("9005");
        assertThat(desc.headElementName()).isEqualTo("RealHead9005");
        assertThat(desc.headClass()).isEqualTo(RequestBusinessHead.class);
        assertThat(desc.requiresResultCode()).isFalse();
    }

    @Test
    void registeredMsgNoCountShouldBe41() {
        assertThat(OutboundWireShapeDispatcher.REGISTERED_MSG_NO_COUNT).isEqualTo(41);
    }

    @Test
    void realHeadRequestMsgNosShouldIncludeHeartbeat() {
        assertThat(OutboundWireShapeDispatcher.REAL_HEAD_REQUEST_MSG_NOS)
                .contains("9005")
                .hasSize(12);
    }

    @Test
    void heartbeat9005_headOnly_passesXsdValidation() {
        // head-only：msgInnerXml 仅 RealHead9005，无 body
        String envelope = wrap9005("""
                <RealHead9005>
                  <SendOrgCode>10000000000001</SendOrgCode>
                  <EntrustDate>20260421</EntrustDate>
                  <TransitionNo>00000005</TransitionNo>
                </RealHead9005>
                """);
        ValidationResult result = validator.validate(MessageType.MSG_9005,
                envelope.getBytes(StandardCharsets.UTF_8));
        assertThat(result.valid())
                .as("9005 head-only valid envelope errors=%s", result.errors())
                .isTrue();
    }

    @Test
    void heartbeat9005_invalidTransitionNo_failsXsdValidation() {
        // implementer 按 9005.xsd TransitionNo facet 实测构造单一违反约束的非法值（其余字段满足）
        String envelope = wrap9005("""
                <RealHead9005>
                  <SendOrgCode>10000000000001</SendOrgCode>
                  <EntrustDate>20260421</EntrustDate>
                  <TransitionNo>BAD</TransitionNo>
                </RealHead9005>
                """);
        ValidationResult result = validator.validate(MessageType.MSG_9005,
                envelope.getBytes(StandardCharsets.UTF_8));
        assertThat(result.valid())
                .as("9005 TransitionNo='BAD' 违反约束必须 fail")
                .isFalse();
    }

    private static String wrap9005(final String realHeadXml) {
        return AbstractXsdValidationTest.wrapCfxTemplate(
                SRC_NODE, DES_NODE, APP, "9005",
                "20260421100000000020", "20260421100000000021", "20260421", realHeadXml);
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd /Users/muzhou/FEP_v1.0_wt-p4-msg-n && ./mvnw test -pl fep-processor -am \
    -Dtest=OutboundWireShape9005XsdComplianceTest -Dsurefire.failIfNoSpecifiedTests=false
```
期望: `describeFor("9005")` 抛 `FepBusinessException`（9005 未注册）或 count!=41 FAIL。

- [ ] **Step 3: 修改 OutboundWireShapeDispatcher**

```java
// 1) REAL_HEAD_REQUEST_MSG_NOS Javadoc + Set +9005：
/** RealHead + {@link RequestBusinessHead} + false 类目 msgNo 集合（P4-MSG-I 扩展 9000；P4-MSG-L 扩展 9006/9008；P4-MSG-N 扩展 9005 心跳 head-only）。 */
public static final Set<String> REAL_HEAD_REQUEST_MSG_NOS = Set.of(
        "1001", "1004", "3000", "3001", "3003", "3005", "3007", "3009", "9000",
        "9005", "9006", "9008");

// 2) REGISTERED_MSG_NO_COUNT 40 → 41

// 3) 类级 Javadoc per-msgNo <ul> 加 1 <li>（按序在 9006 前或邻近）：
//   <li>9005 → {@code RealHead9005} + {@link RequestBusinessHead}（节点心跳，head-only 无 body，P4-MSG-N）</li>
// 4) 6 类目第 1 条（RealHead+RequestBusinessHead+false）扩展列表加 9005 + (P4-MSG-N 扩展 9005)
// 5) "实测自 41 份 XSD" {@code} 列表加 9005（按序插入，实测 9005.xsd 存在）
// 6) ⚠️ :176-178 注释改写（v0.2 评审补）：现把 9005 当"未登记"反例（"未登记的（例如 9005 心跳类...）走 legacy 路径"），
//    9005 注册后此 comment 说谎 → 改用真·未登记 msgNo 举例（implementer grep isRegisteredOutboundMsgNo 逻辑 +
//    确认一个真不在任何 *_MSG_NOS 集合的 msgNo），或删 9005 具名举例保留 legacy-path 语义说明
```

- [ ] **Step 4: cross-task 计数断言全同步（红线，P4-MSG-M 漏检教训）**

```bash
# 按 §设计背景"下游 cross-task 计数断言 + doc 腐烂全枚举"表 (A)+(B) 逐一改：
# (A) 测试 count/size 断言:
# - OutboundWireShape9006And9008XsdComplianceTest:82 (40→41) + :89 (11→12) + :28 Javadoc (39→41)
# - OutboundWireShape9020XsdComplianceTest:67 (40→41) + :28 Javadoc (40→41)
# - OutboundWireShapeDispatcherTest:22/:26/:36 Javadoc 3 处 (39→41 + P4-MSG-N narrative)；grep 该测试有无 count 断言一并改
# - BodyClassRegistryTest:172 Javadoc (40→41，仅 doc，growth-guard 38 不变)
# (B) ⚠️ 生产代码 doc 腐烂（v0.2 评审补，最高优先）:
# - OutboundWireShapeDispatcher.java:176-178 改写 9005 "未登记"反例注释（见 Step 3 第 6 点）
# - BodyClassRegistry.java:105 移除 9005 从"后续待处理"列表（连带校正 9006/9008）
# implementer 复核 grep（穷举，含生产代码）：
#   grep -rn 'isEqualTo(40)\|hasSize(11)\|REGISTERED_MSG_NO_COUNT.*40\|覆盖 39\|不在 39\|=39\|=40' fep-*/src/test
#   grep -rn '9005' fep-converter/src/main fep-web/src/main   # 扫生产代码 9005 具名 doc 腐烂
```

- [ ] **Step 5: 运行测试确认通过**

```bash
cd /Users/muzhou/FEP_v1.0_wt-p4-msg-n && ./mvnw test -pl fep-processor,fep-converter,fep-web -am \
    -Dtest=OutboundWireShape9005XsdComplianceTest,OutboundWireShape9006And9008XsdComplianceTest,OutboundWireShape9020XsdComplianceTest,OutboundWireShapeDispatcherTest,BodyClassRegistryTest \
    -Dsurefire.failIfNoSpecifiedTests=false
```
期望: `BUILD SUCCESS`, 全 GREEN（5 9005 测试 + cross-task 测试）。

- [ ] **Step 6: 全模块回归（红线 `feedback_full_regression_before_commit` — count 断言散落多文件，单跑漏，P4-MSG-M BodyClassRegistryTest 教训）**

```bash
cd /Users/muzhou/FEP_v1.0_wt-p4-msg-n && ./mvnw verify --batch-mode --no-transfer-progress -Dsurefire.rerunFailingTestsCount=1
```
**Strong 回归:** 全 reactor BUILD SUCCESS（checkstyle + spotbugs-check gate 到达 + ArchUnit GREEN；P4DataCollectorEndToEndIntegrationTest E2E flake 由 rerun 兜底）

- [ ] **Step 7: Commit**

```bash
cd /Users/muzhou/FEP_v1.0_wt-p4-msg-n
git add fep-converter/src/main/java/com/puchain/fep/converter/wire/OutboundWireShapeDispatcher.java
git add fep-processor/src/test/java/com/puchain/fep/processor/wire/OutboundWireShape9005XsdComplianceTest.java
git add fep-processor/src/test/java/com/puchain/fep/processor/wire/OutboundWireShape9006And9008XsdComplianceTest.java
git add fep-processor/src/test/java/com/puchain/fep/processor/wire/OutboundWireShape9020XsdComplianceTest.java
git add fep-converter/src/test/java/com/puchain/fep/converter/wire/OutboundWireShapeDispatcherTest.java
git add fep-web/src/test/java/com/puchain/fep/web/outbound/consumer/BodyClassRegistryTest.java
git add fep-web/src/main/java/com/puchain/fep/web/outbound/consumer/BodyClassRegistry.java
git commit -m "$(cat <<'EOF'
feat(converter): P4-MSG-N — 9005 节点心跳 head-only dispatcher 覆盖注册

- REAL_HEAD_REQUEST_MSG_NOS +9005 → 12（head-only，同 9006/9008 双轨类目）
- REGISTERED_MSG_NO_COUNT 40 → 41
- OutboundWireShape9005XsdComplianceTest 5 真 XsdValidator 测试（head-only 完整 CFX envelope，无 body）
- cross-task 计数同步：9006/9008 test(40→41/11→12) + 9020 test(40→41) + dispatcher/BodyClassRegistry test Javadoc
  （红线 feedback_cross_task_obsolete_fixture_assumption_when_set_extended，全枚举防 P4-MSG-M BodyClassRegistryTest 漏检重演）

PRD: v1.3 §4.5 通用 (FR-MSG-9005)
Note: 9005 head-only 走 NodeLifecycleManager 直接路径双轨，不入 BodyClassRegistry/不经 OutboundCfxEnvelopeBuilder；
心跳功能实装（连通性模块占位接入）属独立 initiative 待 P1 TLQ SDK 心跳接入

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

## Task 2: 回归 + CLAUDE.md/PRD matrix 更新 + push + PR + worktree 闭环 `模式 A`

**验收标准:**
1. 全 reactor `./mvnw verify` BUILD SUCCESS
2. `/Users/muzhou/FEP/CLAUDE.md` outbound 40→41 更新（file write only，红线 `feedback_fep_docs_repo_commit_taboo`，**禁 commit**）
3. `/Users/muzhou/FEP/docs/plans/prd-traceability-matrix.md` FR-MSG-9005 maturity 升级（file write only，**禁 commit**）
4. 分支 push + 开 PR
5. PR merge 后 `git worktree remove /Users/muzhou/FEP_v1.0_wt-p4-msg-n`

**Files:**
- Modify: `/Users/muzhou/FEP/CLAUDE.md`（file write only，**禁 commit**）
- Modify: `/Users/muzhou/FEP/docs/plans/prd-traceability-matrix.md`（file write only，**禁 commit**）

- [ ] **Step 1: 全 reactor 最终回归**

```bash
cd /Users/muzhou/FEP_v1.0_wt-p4-msg-n && ./mvnw verify --batch-mode --no-transfer-progress -Dsurefire.rerunFailingTestsCount=1 2>&1 | tail -30
```

- [ ] **Step 2: 更新 CLAUDE.md + PRD matrix（均 file write only，禁 git add）**

```
# /Users/muzhou/FEP/CLAUDE.md（非 git）:
#   "outbound wire-out 进度": **40/44** → **41/44**（P4-MSG-N 补 9005 节点心跳 head-only dispatcher 覆盖）
#   "最近里程碑" 加 1 条 2026-06-03 P4-MSG-N
# /Users/muzhou/FEP/docs/plans/prd-traceability-matrix.md（非 git）:
#   FR-MSG-9005 maturity "🟡 XSD 已支持（XSD 无业务 body）"
#     → "✅ XSD（head-only 无 body）+ outbound wire-shape dispatcher 覆盖注册 (P4-MSG-N，镜像 9006/9008 双轨；
#        9005 走 NodeLifecycleManager 直接路径，心跳功能实装独立 initiative)"
```

- [ ] **Step 3: push + 开 PR**

```bash
cd /Users/muzhou/FEP_v1.0_wt-p4-msg-n
git push -u origin feat/p4-msg-n-9005-heartbeat-dispatcher
gh pr create --base main --title "feat: P4-MSG-N 9005 节点心跳 head-only dispatcher 覆盖注册" \
    --body "$(cat <<'EOF'
## Summary
- 9005 节点心跳 head-only outbound dispatcher 覆盖注册（REAL_HEAD_REQUEST +9005 → 12, REGISTERED_MSG_NO_COUNT 40→41），镜像 9006/9008 双轨
- 5 真 XsdValidator 测试（head-only 完整 CFX envelope，无 body）+ cross-task 计数同步（全枚举）
- 不改 OutboundCfxEnvelopeBuilder / 不入 BodyClassRegistry / 无 inbound（9005 走 NodeLifecycleManager 直接路径）
- 心跳功能实装（连通性模块占位接入）属独立 initiative

## Test plan
- [x] 全 reactor mvnw verify GREEN（本地）
- [ ] GHA CI Build & Quality Gates 背书

PRD: v1.3 §4.5 通用 (FR-MSG-9005)

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

- [ ] **Step 4: worktree 闭环（PR merge 后）**

```bash
cd /Users/muzhou/FEP_v1.0
git worktree remove /Users/muzhou/FEP_v1.0_wt-p4-msg-n
git branch -d feat/p4-msg-n-9005-heartbeat-dispatcher
git worktree list  # 确认不在列表
```

---

## 自检清单

- [x] **1. PRD 覆盖度**: FR-MSG-9005 覆盖（matrix line 189 当前 🟡）。心跳功能实装明示不在本 Plan（独立 initiative）。
- [x] **2. 安全边界**: 无 SM2/SM3/SM4/脱敏关键词；RealHead9005 仅机构码/日期/流水号无 PII。
- [x] **3. 占位符扫描**: 0 TBD/TODO；negative case TransitionNo facet 留 implementer 按 9005.xsd 实测是实现细节非占位。
- [x] **4. 类型一致性**: RealHead9005/RequestBusinessHead/MSG_9005 跨 Task 一致。
- [x] **5. 测试命令可执行**: `-Dtest=OutboundWireShape9005XsdComplianceTest` 与类名匹配；`-Dsurefire.failIfNoSpecifiedTests=false`。
- [x] **6. CLAUDE.md 更新**: T2 Step 2 含 outbound 40→41（file write only）。
- [x] **7. 验收标准完整性**: 断言值（41/12/RealHead9005）可手算验证。
- [x] **8. 共享工具类无遗漏**: 无新增；复用 SHARED_VALIDATOR/wrapCfxTemplate。
- [x] **9. 核心类职责边界**: 无新增 ≥3 依赖 Service。
- [x] **10. Worktree 触发条件自检**:
  - [ ] 跨 ≥3 模块？✅ fep-converter + fep-processor + fep-web（test cross-task）
  - [x] 与已签字未执行 Plan 并存？✅ 别会话多 Plan
  - [x] ≥5 min verify 并行？✅ 全 reactor verify ~8-12min
  - [x] 多会话并存？✅ 多 worktree 活跃
  - **结论**: ✅ 必须 worktree，已建 `wt-p4-msg-n`；T2 Step 4 含 remove。

---

## 红线交叉引用

| Task | 红线 | 应对 |
|---|---|---|
| T1 | `feedback_xsd_validator_requires_full_envelope_redline` + `feedback_xsd_compliance_fix_real_validator_on_sut` | 真 XsdValidator + 完整 head-only CFX envelope |
| T1 | `feedback_fixture_data_must_satisfy_xsd_constraints` | RealHead9005 字段满足 RequestHead facet；negative 仅违反单一约束 |
| T1 | `feedback_cross_task_obsolete_fixture_assumption_when_set_extended` | §设计背景全枚举下游 count 断言 + Step 4 同步 + grep 复核 |
| T1 | `feedback_full_regression_before_commit` | Step 6 全 reactor verify（P4-MSG-M BodyClassRegistryTest 漏检教训：count 断言散落多文件单跑漏） |
| T1 | `feedback_plan_template_data_point_self_consistency` | 41/12 计数全文自洽 |
| T2 | `feedback_fep_docs_repo_commit_taboo` + `feedback_plan_step3_commit_template_residue` | CLAUDE.md + PRD matrix file write only 禁 commit |
| T2 | `feedback_worktree_for_parallel_work` | T2 Step 4 worktree remove |
| 全程 | `feedback_baseline_drift_during_long_review_cycle` | baseline 42ce336，签字与实施跨时重测 |
| 全程 | subagent 全周期纪律（must_commit_before_exit / model_override / no_background_bash / meta_comment） | implementer dispatch |
| 全程 | `feedback_task_review_discipline` | 每 Task 独立 spec + quality review |

---

## 评审与签字

> ⚠️ **本 Plan 必须先通过 AI 独立评审 + muzhou 人工签字方可执行**

### AI 独立评审区

- **Round 1（santa-method）REVISE** — API 真实性（REAL_HEAD_REQUEST 11 / COUNT 40 / describeFor / 9005.xsd head-only / wrapCfxTemplate / 9006-9008 双轨不入 BodyClassRegistry）+ 不改 OutboundCfxEnvelopeBuilder 论断 + head-only 测试可行性全 PASS；阻断项第 4 cross-task 枚举漏 2 处生产 doc 腐烂（`OutboundWireShapeDispatcher.java:176-178` 9005 "未登记"反例 + `BodyClassRegistry.java:105` 9005 "后续待处理"，与 P4-MSG-M BodyClassRegistryTest 同型翻车点）。
- **v0.2 boil-lake** — 补列枚举表 (B) 生产 doc + DispatcherTest 3 处 stale 39 + Step 4 生产 doc grep；自 grep 穷举确认完整。
- **Round 2（santa-method）✅ PASS** — 4 聚焦项全解；枚举表 (A)+(B) 与 codebase grep 实测一致、Plan 各段自洽（Files 表 / Step 3 / Step 4 / Step 7 commit）；DispatcherTest :26 历史 narrative 已澄清追加新行不改。

### muzhou 签字区

v0.2 修订路径: v0.1 起草(grep 实测全 API) → Round 1 REVISE(cross-task 漏 2 生产 doc) → boil-lake 补列 → Round 2 ✅ PASS → muzhou 签字。

| 项 | 决定 | 签字日期 |
|---|---|---|
| 批准 / 驳回 / 部分修改 | **✅ 批准** | 2026-06-03 |
| 签字 | **muzhou** | 2026-06-03 |
