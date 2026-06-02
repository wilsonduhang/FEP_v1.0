# FEP P4-MSG-M 9020 实时业务通用应答机械注册实施计划 v0.2

> **执行方式:** 使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐 Task 执行。步骤使用 `- [ ]` 复选框跟踪。

## v0.1 → v0.2 修订纪要（2026-06-02）

v0.1 起草 → Round 1 AI 评审 **PASS**（零 API drift，全 grep 锚点实测对齐）+ 2 🟡 CONCERN → boil-lake 修订（红线 `feedback_concern_boil_lake_when_cheap_and_safe`）：
1. BodyClassRegistry 锚点改实测末 entry `Map.entry("9120", MsgReturn9120.class)`（9000 在中段，非末行）
2. T2 Step 5 显式点名重命名 `InboundMessageDispatcherTest.bodyTypeRegistry_contains23Entries → contains24Entries`（避免方法名计数腐烂）
3. inbound 描述 "21→24" 措辞清理为实测 "23→24"

**目标:** PRD v1.3 §4.5 通用 9 报文 — 9020 实时业务通用应答机械双向注册（outbound wire-shape + BodyClassRegistry + inbound BODY_TYPE_REGISTRY），镜像 P4-MSG-L，无新业务行为。

**前置依赖:**
- P4-MSG-I（outbound wire-shape 6 类目结构稳定）— origin/main
- P4-MSG-L（9006-9009 + NodeLifecycleAckListener + SerialNoBearing 红线确立）— origin/main `3f45c67` MERGED

**执行 Worktree:** `/Users/muzhou/FEP_v1.0_wt-p4-msg-m`（分支 `feat/p4-msg-m-9020-general-response`，触发条件第 2 项 + 第 6 项）
> 触发条件第 2 项：与多个别会话已签字/未实施 Plan 并存（主 worktree untracked Plans）
> 触发条件第 6 项：多并行 AI 会话同时活跃（7+ worktree）— 红线 `feedback_worktree_isolates_fs_not_logic_domain`

**Baseline:** origin/main `f61749a`（2026-06-02 实测；P4-MSG-L `3f45c67` + PR #38/#42 已 merge。红线 `feedback_baseline_drift_during_long_review_cycle` — 签字与实施跨时须重测）

**架构:**
- 9020 是 COMMON BIDIRECTIONAL 实时业务通用应答（`MessageType.MSG_9020` responseMsgNo=null 已存）。
- **outbound**（T1）: `OutboundWireShapeDispatcher.REAL_HEAD_RESPONSE_MSG_NOS` +9020（RealHead9020 type=ResponseHead → `ResponseBusinessHead` Java 类 + requiresResultCode=**true**，与 2001/3002 完全同类目，envelope builder 注入 5 位 Result 占位）；`REGISTERED_MSG_NO_COUNT` 39→40；`BodyClassRegistry`（fep-web outbound.consumer）+9020→MsgReturn9020。
- **inbound**（T2）: `InboundMessageDispatcher.BODY_TYPE_REGISTRY` +9020→MsgReturn9020（实测 **23**→**24**，23 已含 P4-MSG-L 9007/9009）；`MsgReturn9020` 加 `implements SerialNoBearing`（getSerialNo→null，OriMsgNo 非 SerialNo，红线 `feedback_registered_inbound_body_must_implement_serialnobearing`，镜像 2101-2104）。
- **不加 listener**（muzhou brainstorming 决策）：9020 通用应答无明确业务处理逻辑，仅 dispatcher 注册（unmarshal + publish event 可用）。
- 真 `XsdValidator` 验证 9020 outbound marshal + inbound unmarshal 完整 CFX envelope。
- **9005 不在本 Plan**（head-only outbound 需改造 OutboundCfxEnvelopeBuilder 支持无 body，独立 Plan）。

**技术栈:** Java 17 / Spring Boot 3.x / JAXB 4 (Jakarta) / Maven / JUnit 5 / AssertJ

**AI 协同模式:**

| 模式 | AI 占比 | 本计划中的应用 |
|:----:|:-------:|--------------|
| A | 90% | 全 Task — 纯 outbound/inbound 注册扩展 + SerialNoBearing interface，无安全代码 |

**无 ⛔ 模式 E**：不涉 `security/impl/`、SM2/SM3/SM4、脱敏核心规则。

**PR 大小预估:** T1 ~90 LOC + T2 ~130 LOC（含真 XsdValidator 测试）≈ **~220 LOC**；测试为红线强制（`feedback_xsd_validator_requires_full_envelope_redline`），若超 400 参 P4-MSG-L 先例 muzhou 签字豁免。

---

## 设计背景

### PRD 依据

PRD v1.3 §4.5 通用 9 报文 — 9020 实时业务通用应答。**MessageType.java 实测:** `MSG_9020("9020", "实时业务通用应答", COMMON, BIDIRECTIONAL, null)`。

### XSD 字段实测（含 minOccurs 列 — 红线 `feedback_plan_xsd_field_table_default_not_optional`）

#### MsgReturn9020（RealHead9020 ResponseHead + MsgReturn9020 body）

| 字段 | XSD type | minOccurs | 业务约束 | required 属性 |
|---|---|:---:|---|:---:|
| OriMsgNo | MsgNo (Number) | 1 | 原报文号 | true |
| Debug | Debug (String) | 0 | 调试信息（可选） | false |

source: `9020.xsd:34-44`（RealHead9020 type=ResponseHead + MsgReturn9020）+ `9020.xsd:47-63`（MsgReturn9020: OriMsgNo MsgNo required + Debug minOccurs=0）+ `DataType.xsd:369`（MsgNo simpleType base=Number）。

**字段自洽核对（红线 `feedback_plan_template_data_point_self_consistency`）**: MsgReturn9020 = 2 字段（OriMsgNo req + Debug opt）= 1+1 = **2** ✓

#### RealHead9020（ResponseHead，Base.xsd 实测含 Result）

`ResponseHead`（Base.xsd）字段：SendOrgCode + EntrustDate + TransitionNo + **Result**（type=Result，5 位数字）。→ Java `ResponseBusinessHead extends RequestBusinessHead`（`@XmlElement(name="Result", required=true)` setResult 校验 5 位数字）。**与 2001/3002 RealHead 完全同结构**（均 ResponseHead），故 9020 归 REAL_HEAD_RESPONSE 类目（requiresResultCode=true）。

### 复用已存类（实测）

| 类 | 路径 | 状态 |
|---|---|---|
| `MsgReturn9020` | `fep-processor/.../body/common/MsgReturn9020.java` | 已存（`extends CfxBody`，propOrder `{oriMsgNo, debug}`），**本 Plan T2 加 implements SerialNoBearing** |
| `ResponseBusinessHead` | `fep-converter/.../model/ResponseBusinessHead.java` | 已存（含 Result），复用 |
| `SerialNoBearing` | `fep-converter/.../model/SerialNoBearing.java` | 已存（`String getSerialNo()`），T2 implements |

### 实测 baseline（grep 确认）

- `OutboundWireShapeDispatcher.REAL_HEAD_RESPONSE_MSG_NOS` = `Set.of("2001","2004","3002","3004","3006","3008")` 6 元素 → +9020 = 7
- `OutboundWireShapeDispatcher.REGISTERED_MSG_NO_COUNT` = 39 → 40
- `BodyClassRegistry`（`com.puchain.fep.web.outbound.consumer`）REGISTRY 实测末 entry `Map.entry("9120", MsgReturn9120.class)`（9000 在中段）→ 在末行 9120 后追加 `Map.entry("9020", MsgReturn9020.class)`（Map 无序约束，按序或末行均可）
- `InboundMessageDispatcher.BODY_TYPE_REGISTRY` = 23 entry（末 MSG_9007/MSG_9009）→ +9020 = 24
- `describeFor` REAL_HEAD_RESPONSE 分支返回 `new WireShapeDescriptor("RealHead"+msgNo, ResponseBusinessHead.class, true)`

---

## 文件结构设计

| 文件路径 | 职责 | 操作 | AI 模式 |
|---|---|---|:---:|
| `fep-converter/.../wire/OutboundWireShapeDispatcher.java` | REAL_HEAD_RESPONSE_MSG_NOS +9020 + count 39→40 + Javadoc | 修改 | A |
| `fep-web/.../outbound/consumer/BodyClassRegistry.java` | REGISTRY +9020→MsgReturn9020 + Javadoc | 修改 | A |
| `fep-processor/.../body/common/MsgReturn9020.java` | 加 implements SerialNoBearing + getSerialNo()→null | 修改 | A |
| `fep-web/.../messageinbound/service/InboundMessageDispatcher.java` | BODY_TYPE_REGISTRY +9020 → 24 + Javadoc | 修改 | A |
| `fep-processor/.../wire/OutboundWireShape9020XsdComplianceTest.java` | 9020 outbound 真 XsdValidator 完整 envelope 测试 | 新建 | A |
| `fep-web/.../messageinbound/Inbound9020WireTest.java` | 9020 inbound dispatcher routing + 真 XsdValidator 测试 | 新建 | A |
| `fep-web/.../architecture/InboundRegistryArchTest.java` | snapshot 23→24 | 修改 | A |
| `fep-web/.../messageinbound/service/InboundMessageDispatcherTest.java` | snapshot 23→24 + 9020 entry | 修改 | A |
| `fep-processor/.../body/supplychain/SerialNoBearingComplianceTest.java` | Javadoc 计数同步（如引用 inbound 数）| 修改 | A |

**总: 4 修改生产 + 2 新测试 + 3 修改测试 = 9 文件改动（复用 MsgReturn9020/ResponseBusinessHead/SerialNoBearing）**

### 共享工具类清单

无新增共享工具类。复用 `AbstractXsdValidationTest.SHARED_VALIDATOR` + `wrapCfxTemplate`（fep-processor test）；fep-web 测试因模块边界 inline wrapCfx（同 P4-MSG-L `Inbound9007And9009WireTest` 先例）。

### 核心类职责边界

本 Plan 无新增 ≥3 依赖的 Service（纯注册扩展 + interface 实现）。

---

## Task 1: 9020 outbound 注册（wire-shape + BodyClassRegistry + 真 XSD 合规测试）`模式 A`

**PRD 依据:** v1.3 §4.5 通用 9 报文 — 9020 实时业务通用应答
**追溯 ID:** FR-MSG-9020

**验收标准（从 PRD + XSD 实测推导）:**
1. `dispatcher.describeFor("9020")` → `headElementName()="RealHead9020"` + `headClass()==ResponseBusinessHead.class` + `requiresResultCode()==true`
2. `OutboundWireShapeDispatcher.REGISTERED_MSG_NO_COUNT == 40`
3. `OutboundWireShapeDispatcher.REAL_HEAD_RESPONSE_MSG_NOS` size==7 含 "9020"
4. `BodyClassRegistry.resolve("9020") == MsgReturn9020.class`（不再抛 OUTBOUND_5107）
5. `MsgReturn9020` OriMsgNo="3000"（valid MsgNo Number）marshal + RealHead9020(ResponseBusinessHead, Result=占位) + 完整 CFX envelope → 真 `XsdValidator.validate(MessageType.MSG_9020, bytes)` → `result.valid()==true`
6. OriMsgNo 违反 MsgNo facet（如非数字 "abcd"）→ `result.valid()==false` + errors 含约束关键词

**Files:**
- Modify: `fep-converter/src/main/java/com/puchain/fep/converter/wire/OutboundWireShapeDispatcher.java`
- Modify: `fep-web/src/main/java/com/puchain/fep/web/outbound/consumer/BodyClassRegistry.java`
- Create: `fep-processor/src/test/java/com/puchain/fep/processor/wire/OutboundWireShape9020XsdComplianceTest.java`

- [ ] **Step 1: 编写失败测试 OutboundWireShape9020XsdComplianceTest**

> 镜像已 ship 的 `OutboundWireShape9006And9008XsdComplianceTest`（P4-MSG-L T1，fep-processor 同包），复用 `AbstractXsdValidationTest.SHARED_VALIDATOR` + `wrapCfxTemplate`。implementer 先 `Read` 该参照测试 + grep `MsgReturn9020` getter/setter + `wrapCfxTemplate` 签名 + `MsgNo` XSD facet 精确 length，按真实 API 写。9020 envelope 须含 RealHead9020（ResponseHead：SendOrgCode/EntrustDate/TransitionNo/**Result** 5 位）+ MsgReturn9020（OriMsgNo + 可选 Debug）。

```java
package com.puchain.fep.processor.wire;

import com.puchain.fep.converter.model.ResponseBusinessHead;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.converter.wire.OutboundWireShapeDispatcher;
import com.puchain.fep.converter.wire.WireShapeDescriptor;
import com.puchain.fep.processor.body.common.MsgReturn9020;
import com.puchain.fep.processor.validation.AbstractXsdValidationTest;
import com.puchain.fep.processor.validation.ValidationResult;
import com.puchain.fep.processor.validation.XsdValidator;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P4-MSG-M T1 — 9020 outbound wire-shape + BodyClassRegistry + 真 {@link XsdValidator} 合规测试。
 *
 * <p>红线 feedback_xsd_compliance_fix_real_validator_on_sut（真 validator 跑 SUT 产物）
 * + feedback_xsd_validator_requires_full_envelope_redline（完整 CFX envelope）。
 * 9020 与 2001/3002 同 REAL_HEAD_RESPONSE 类目（RealHead+ResponseBusinessHead+requiresResultCode=true）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class OutboundWireShape9020XsdComplianceTest {

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
    void describeFor_9020_shouldReturnRealHeadResponse() {
        WireShapeDescriptor desc = dispatcher.describeFor("9020");
        assertThat(desc.headElementName()).isEqualTo("RealHead9020");
        assertThat(desc.headClass()).isEqualTo(ResponseBusinessHead.class);
        assertThat(desc.requiresResultCode()).isTrue();
    }

    @Test
    void registeredMsgNoCountShouldBe40() {
        assertThat(OutboundWireShapeDispatcher.REGISTERED_MSG_NO_COUNT).isEqualTo(40);
    }

    @Test
    void realHeadResponseMsgNosShouldIncludeGeneralResponse() {
        assertThat(OutboundWireShapeDispatcher.REAL_HEAD_RESPONSE_MSG_NOS)
                .contains("9020")
                .hasSize(7);
    }

    @Test
    void msgReturn9020_validPayload_passesXsdValidation() throws Exception {
        MsgReturn9020 body = new MsgReturn9020();
        body.setOriMsgNo("3000"); // valid MsgNo (Number)

        String bodyXml = marshal(body, MsgReturn9020.class);
        assertThat(bodyXml).contains("<OriMsgNo>3000</OriMsgNo>");

        String envelope = wrap9020(bodyXml);
        ValidationResult result = validator.validate(MessageType.MSG_9020,
                envelope.getBytes(StandardCharsets.UTF_8));
        assertThat(result.valid())
                .as("9020 valid payload errors=%s", result.errors())
                .isTrue();
    }

    @Test
    void msgReturn9020_invalidOriMsgNo_failsXsdValidation() throws Exception {
        MsgReturn9020 body = new MsgReturn9020();
        body.setOriMsgNo("abcd"); // 非数字，违反 MsgNo base=Number

        String bodyXml = marshal(body, MsgReturn9020.class);
        String envelope = wrap9020(bodyXml);
        ValidationResult result = validator.validate(MessageType.MSG_9020,
                envelope.getBytes(StandardCharsets.UTF_8));
        assertThat(result.valid())
                .as("9020 OriMsgNo='abcd' 非数字必须 fail")
                .isFalse();
    }

    private static String wrap9020(final String bodyXml) {
        // RealHead9020 ResponseHead: SendOrgCode/EntrustDate/TransitionNo/Result(5位)
        // implementer 按 wrapCfxTemplate 真实签名装配（参 OutboundWireShape9006And9008XsdComplianceTest）
        return AbstractXsdValidationTest.wrapCfxTemplate(
                SRC_NODE, DES_NODE, APP, "9020",
                "20260421100000000010", "20260421100000000011", "20260421", """
                <RealHead9020>
                  <SendOrgCode>10000000000001</SendOrgCode>
                  <EntrustDate>20260421</EntrustDate>
                  <TransitionNo>00000010</TransitionNo>
                  <Result>00000</Result>
                </RealHead9020>
                """ + bodyXml);
    }

    private static <T> String marshal(final T body, final Class<T> bodyClass) throws Exception {
        JAXBContext ctx = JAXBContext.newInstance(bodyClass);
        Marshaller m = ctx.createMarshaller();
        m.setProperty(Marshaller.JAXB_FRAGMENT, true);
        StringWriter sw = new StringWriter();
        m.marshal(body, sw);
        return sw.toString();
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd /Users/muzhou/FEP_v1.0_wt-p4-msg-m && ./mvnw test -pl fep-processor -am \
    -Dtest=OutboundWireShape9020XsdComplianceTest \
    -Dsurefire.failIfNoSpecifiedTests=false
```
期望: `describeFor("9020")` 抛 `FepBusinessException`（9020 未注册）或 count!=40 assertion FAIL。

- [ ] **Step 3: 修改 OutboundWireShapeDispatcher**

```java
// 1) REAL_HEAD_RESPONSE_MSG_NOS Javadoc + Set +9020：
/** RealHead + {@link ResponseBusinessHead} + true 类目 msgNo 集合（P4-MSG-G T3 扩展 3008；P4-MSG-M 扩展 9020）。 */
public static final Set<String> REAL_HEAD_RESPONSE_MSG_NOS = Set.of(
        "2001", "2004", "3002", "3004", "3006", "3008", "9020");

// 2) REGISTERED_MSG_NO_COUNT 39 → 40

// 3) 类级 Javadoc <ul> per-msgNo 段加 1 <li>（在 3008 后或末尾按序）：
//   <li>9020 → {@code RealHead9020} + {@link ResponseBusinessHead}（实时业务通用应答，含 ResultCode，P4-MSG-M）</li>
// 4) 6 类目第 2 条（RealHead + ResponseBusinessHead + true）扩展列表加 9020 + (P4-MSG-M 扩展 9020)
// 5) "实测自 40 份 XSD" {@code} 列表加 9020（按序插入，实测 9020.xsd 存在）
```

- [ ] **Step 4: 修改 BodyClassRegistry（fep-web outbound.consumer）**

```java
// imports 加：import com.puchain.fep.processor.body.common.MsgReturn9020;
// REGISTRY Map.ofEntries 实测末行 Map.entry("9120", MsgReturn9120.class) 后追加（改末行分号位置）：
        Map.entry("9020", MsgReturn9020.class));
// 类级 Javadoc <ul> 注册段加：
//   <li>9020 → {@link MsgReturn9020}（实时业务通用应答，P4-MSG-M）</li>
// 范围说明加 P4-MSG-M
```

- [ ] **Step 5: 运行测试确认通过**

```bash
cd /Users/muzhou/FEP_v1.0_wt-p4-msg-m && ./mvnw test -pl fep-processor,fep-web -am \
    -Dtest=OutboundWireShape9020XsdComplianceTest \
    -Dsurefire.failIfNoSpecifiedTests=false
```
期望: `BUILD SUCCESS`, 5 tests passed。

- [ ] **Step 6: 全模块回归（红线 `feedback_full_regression_before_commit`）**

```bash
cd /Users/muzhou/FEP_v1.0_wt-p4-msg-m && ./mvnw verify --batch-mode --no-transfer-progress -Dsurefire.rerunFailingTestsCount=1
```
**Strong 回归:** 全 reactor BUILD SUCCESS（含 ArchUnit/checkstyle/spotbugs；P4DataCollectorEndToEndIntegrationTest E2E flake 由 rerun 兜底）
**Minimum 回归:** fep-converter + fep-processor + fep-web + fep-common GREEN

- [ ] **Step 7: Commit**

```bash
cd /Users/muzhou/FEP_v1.0_wt-p4-msg-m
git add fep-converter/src/main/java/com/puchain/fep/converter/wire/OutboundWireShapeDispatcher.java
git add fep-web/src/main/java/com/puchain/fep/web/outbound/consumer/BodyClassRegistry.java
git add fep-processor/src/test/java/com/puchain/fep/processor/wire/OutboundWireShape9020XsdComplianceTest.java
git commit -m "$(cat <<'EOF'
feat(converter): P4-MSG-M T1 — 9020 outbound 通用应答 dispatcher 注册

- REAL_HEAD_RESPONSE_MSG_NOS +9020 → 7（RealHead9020 ResponseHead，requiresResultCode=true，同 2001/3002）
- REGISTERED_MSG_NO_COUNT 39 → 40
- BodyClassRegistry +9020 → MsgReturn9020（outbound body 解析）
- 5 真 XsdValidator 测试 GREEN（完整 CFX envelope + Result 5 位占位）

PRD: v1.3 §4.5 通用 (FR-MSG-9020)
Note: MsgReturn9020 body 复用已存；9005 head-only 拆独立 Plan

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

## Task 2: 9020 inbound 注册 + MsgReturn9020 implements SerialNoBearing `模式 A`

**PRD 依据:** v1.3 §4.5 通用 9 报文 — 9020 实时业务通用应答（inbound 接收）
**追溯 ID:** FR-MSG-9020

**验收标准:**
1. `InboundMessageDispatcher.bodyTypeRegistry()` size==24，含 entry "9020" → MsgReturn9020.class
2. dispatch messageType="9020" + valid envelope → unmarshal MsgReturn9020 + publish InboundMessageProcessedEvent（event.body() instanceof MsgReturn9020 + getOriMsgNo()=="3000"）
3. `MsgReturn9020 instanceof SerialNoBearing` == true；`new MsgReturn9020().getSerialNo() == null`（OriMsgNo 非 SerialNo，fallback transitionNo）
4. `InboundRegistryArchTest.allRegisteredBodies_mustImplementSerialNoBearing` GREEN（9020 入 registry 后不变量满足）
5. MsgReturn9020 OriMsgNo="3000" valid inbound 真 XsdValidator 完整 envelope → result.valid()==true

**Files:**
- Modify: `fep-processor/src/main/java/com/puchain/fep/processor/body/common/MsgReturn9020.java`
- Modify: `fep-web/src/main/java/com/puchain/fep/web/messageinbound/service/InboundMessageDispatcher.java`
- Create: `fep-web/src/test/java/com/puchain/fep/web/messageinbound/Inbound9020WireTest.java`
- Modify: `fep-web/src/test/java/com/puchain/fep/architecture/InboundRegistryArchTest.java`（snapshot 23→24）
- Modify: `fep-web/src/test/java/com/puchain/fep/web/messageinbound/service/InboundMessageDispatcherTest.java`（snapshot 23→24 + 9020 entry）
- Modify: `fep-processor/.../body/supplychain/SerialNoBearingComplianceTest.java`（如引用 inbound 计数则同步）

- [ ] **Step 1: 编写失败测试 Inbound9020WireTest**

> 镜像已 ship `Inbound9007And9009WireTest`（P4-MSG-L T2，fep-web @SpringBootTest，inline wrapCfx）。implementer 先 `Read` 该参照 + grep `InboundMessageDispatcher.dispatch` 真实签名 + `bodyTypeRegistry()` accessor，按真实 API 写。

```java
package com.puchain.fep.web.messageinbound;

import com.puchain.fep.converter.model.SerialNoBearing;
import com.puchain.fep.processor.body.common.MsgReturn9020;
import com.puchain.fep.web.messageinbound.service.InboundMessageDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P4-MSG-M T2 — 9020 inbound dispatcher routing + MsgReturn9020 SerialNoBearing 测试。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
class Inbound9020WireTest {

    @Autowired
    private InboundMessageDispatcher dispatcher;

    @Test
    void bodyTypeRegistryShouldBe24EntriesIncluding9020() {
        assertThat(InboundMessageDispatcher.bodyTypeRegistry()).hasSize(24);
        assertThat(InboundMessageDispatcher.bodyTypeRegistry())
                .containsEntry("9020", MsgReturn9020.class);
    }

    @Test
    void msgReturn9020_shouldImplementSerialNoBearingReturningNull() {
        MsgReturn9020 body = new MsgReturn9020();
        body.setOriMsgNo("3000");
        assertThat(body).isInstanceOf(SerialNoBearing.class);
        assertThat(((SerialNoBearing) body).getSerialNo())
                .as("OriMsgNo 非业务 SerialNo → null fallback transitionNo（镜像 2102）")
                .isNull();
    }

    @Test
    void dispatch_9020_shouldUnmarshalMsgReturn9020AndPublishEvent() {
        // implementer 按 Inbound9007And9009WireTest dispatch_9007 pattern：
        // 构造 valid 9020 envelope（RealHead9020 ResponseHead + MsgReturn9020 OriMsgNo=3000）
        // → dispatcher.dispatch("9020", transitionNo, bytes) → 捕获 event → assert body instanceof MsgReturn9020 + getOriMsgNo()=="3000"
    }

    @Test
    void msgReturn9020_validInbound_passesXsdValidation() throws Exception {
        // implementer 复用真 XsdValidator + 完整 envelope（同 T1 wrap9020），断言 result.valid()
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd /Users/muzhou/FEP_v1.0_wt-p4-msg-m && ./mvnw test -pl fep-web -am \
    -Dtest=Inbound9020WireTest -Dsurefire.failIfNoSpecifiedTests=false
```
期望: registry size==23 assertion FAIL 或 MsgReturn9020 非 SerialNoBearing（编译失败/instanceof false）。

- [ ] **Step 3: MsgReturn9020 加 implements SerialNoBearing**

```java
// imports 加：import com.puchain.fep.converter.model.SerialNoBearing;
// class 声明改：
public class MsgReturn9020 extends CfxBody implements SerialNoBearing {
    // ... 既有 oriMsgNo/debug 字段与 getter/setter 不动 ...

    /**
     * {@inheritDoc}
     *
     * <p>9020 实时业务通用应答无业务 SerialNo 字段（仅 OriMsgNo 原报文号 + Debug），
     * 恒返回 {@code null} → {@code InboundMessageDispatcher} fallback 到 RealHead.transitionNo，
     * 与 2101/2102/2103/2104 BATCH 回执 null-fallback 策略一致（红线
     * feedback_registered_inbound_body_must_implement_serialnobearing）。</p>
     */
    @Override
    public String getSerialNo() {
        return null;
    }
}
```

- [ ] **Step 4: 修改 InboundMessageDispatcher BODY_TYPE_REGISTRY**

```java
// imports 加：import com.puchain.fep.processor.body.common.MsgReturn9020;
// BODY_TYPE_REGISTRY Map.ofEntries 末尾（MSG_9009 后）加（注意 MessageType.MSG_9020.msgNo() 风格一致）：
        Map.entry(MessageType.MSG_9020.msgNo(), MsgReturn9020.class));
// 类级 Javadoc <ul> 注册段加：
//   <li>9020 → {@link MsgReturn9020}（实时业务通用应答，P4-MSG-M）</li>
// 范围说明 "...登记 24 种业务 body" + 加 P4-MSG-M
```

- [ ] **Step 5: 同步不变量测试计数（23→24）**

```bash
# implementer grep 下列文件中所有 23 计数声明 + 9007/9009 entry 风格，机械同步 23→24 + 加 9020：
# - fep-web/.../architecture/InboundRegistryArchTest.java（snapshot 23→24 + containsKeys 加 "9020"）
# - fep-web/.../messageinbound/service/InboundMessageDispatcherTest.java（hasSize 23→24 + get("9020")==MsgReturn9020
#     + **重命名测试方法 bodyTypeRegistry_contains23Entries → bodyTypeRegistry_contains24Entries**，避免方法名计数腐烂）
# - fep-processor/.../body/supplychain/SerialNoBearingComplianceTest.java（若 Javadoc 引用 inbound registry 计数则同步；MsgReturn9020 非 supplychain 包，确认是否需加入 REGISTERED_BODIES）
# 红线 feedback_plan_template_data_point_self_consistency：所有 "23"/"24" 声明自洽
```

- [ ] **Step 6: 运行测试确认通过**

```bash
cd /Users/muzhou/FEP_v1.0_wt-p4-msg-m && ./mvnw test -pl fep-web,fep-processor -am \
    -Dtest=Inbound9020WireTest,InboundRegistryArchTest,InboundMessageDispatcherTest \
    -Dsurefire.failIfNoSpecifiedTests=false
```
期望: `BUILD SUCCESS`, 全 GREEN。

- [ ] **Step 7: 全模块回归（红线 `feedback_full_regression_before_commit` — ArchUnit SerialNoBearing 不变量须全模块 verify 抓）**

```bash
cd /Users/muzhou/FEP_v1.0_wt-p4-msg-m && ./mvnw verify --batch-mode --no-transfer-progress -Dsurefire.rerunFailingTestsCount=1
```
**Strong 回归:** 全 reactor BUILD SUCCESS（特别确认 `InboundRegistryArchTest.allRegisteredBodies_mustImplementSerialNoBearing` GREEN）

- [ ] **Step 8: Commit**

```bash
cd /Users/muzhou/FEP_v1.0_wt-p4-msg-m
git add fep-processor/src/main/java/com/puchain/fep/processor/body/common/MsgReturn9020.java
git add fep-web/src/main/java/com/puchain/fep/web/messageinbound/service/InboundMessageDispatcher.java
git add fep-web/src/test/java/com/puchain/fep/web/messageinbound/Inbound9020WireTest.java
git add fep-web/src/test/java/com/puchain/fep/architecture/InboundRegistryArchTest.java
git add fep-web/src/test/java/com/puchain/fep/web/messageinbound/service/InboundMessageDispatcherTest.java
git add fep-processor/src/test/java/com/puchain/fep/processor/body/supplychain/SerialNoBearingComplianceTest.java
git commit -m "$(cat <<'EOF'
feat(web): P4-MSG-M T2 — 9020 inbound dispatcher 注册 + MsgReturn9020 SerialNoBearing

- BODY_TYPE_REGISTRY +9020 → MsgReturn9020 (23→24)
- MsgReturn9020 implements SerialNoBearing（getSerialNo→null，OriMsgNo 非 SerialNo，镜像 2101-2104；
  红线 feedback_registered_inbound_body_must_implement_serialnobearing）
- 3 不变量测试计数 23→24 同步
- Inbound9020WireTest（registry + SerialNoBearing + dispatch routing + 真 XsdValidator）

PRD: v1.3 §4.5 通用 (FR-MSG-9020)
Note: 不加专用 listener（通用应答无明确业务处理，muzhou brainstorming 决策）

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

## Task 3: 回归 + CLAUDE.md/PRD matrix 更新 + push + PR + worktree 闭环 `模式 A`

**验收标准:**
1. 全 reactor `./mvnw verify` BUILD SUCCESS
2. `/Users/muzhou/FEP/CLAUDE.md` outbound 39→40 + inbound 23→24 更新（file write only，红线 `feedback_fep_docs_repo_commit_taboo`）
3. `/Users/muzhou/FEP/docs/plans/prd-traceability-matrix.md` FR-MSG-9020 maturity 升级（file write only — 该文件在 /FEP/ 非 git，**禁 git add/commit**，红线 `feedback_fep_docs_repo_commit_taboo` + `feedback_plan_step3_commit_template_residue`）
4. 分支 push + 开 PR
5. PR merge 后 `git worktree remove /Users/muzhou/FEP_v1.0_wt-p4-msg-m`

**Files:**
- Modify: `/Users/muzhou/FEP/CLAUDE.md`（file write only，**禁 commit**）
- Modify: `/Users/muzhou/FEP/docs/plans/prd-traceability-matrix.md`（file write only，**禁 commit**）

- [ ] **Step 1: 全 reactor 最终回归**

```bash
cd /Users/muzhou/FEP_v1.0_wt-p4-msg-m && ./mvnw verify --batch-mode --no-transfer-progress -Dsurefire.rerunFailingTestsCount=1 2>&1 | tail -30
```
期望: BUILD SUCCESS。

- [ ] **Step 2: 更新 CLAUDE.md + PRD matrix（均 file write only，禁 git add）**

```
# /Users/muzhou/FEP/CLAUDE.md（非 git）:
#   "outbound wire-out 进度": **39/44** → **40/44**（P4-MSG-M 补 9020）
#   "inbound dispatcher 注册": **23** → **24**（追加 9020 → MsgReturn9020）
#   "最近里程碑" 段加 1 条 2026-06-02 P4-MSG-M 9020 通用应答 ship
# /Users/muzhou/FEP/docs/plans/prd-traceability-matrix.md（非 git）:
#   FR-MSG-9020 maturity "✅ XSD + Body POJO (MsgReturn9020) + XSD IT"
#     → "✅ + outbound wire-shape dispatcher + BodyClassRegistry + inbound dispatcher + SerialNoBearing (P4-MSG-M)"
```

- [ ] **Step 3: push + 开 PR**

```bash
cd /Users/muzhou/FEP_v1.0_wt-p4-msg-m
git push -u origin feat/p4-msg-m-9020-general-response
gh pr create --base main --title "feat: P4-MSG-M 9020 实时业务通用应答机械注册 (outbound + inbound)" \
    --body "$(cat <<'EOF'
## Summary
- 9020 outbound wire-shape (REAL_HEAD_RESPONSE +9020 → 7, count 39→40) + BodyClassRegistry (T1)
- 9020 inbound BODY_TYPE_REGISTRY (23→24) + MsgReturn9020 implements SerialNoBearing (T2)
- 真 XsdValidator 完整 envelope 测试；9005 head-only 拆独立 Plan

## Test plan
- [x] 全 reactor mvnw verify GREEN（本地）
- [ ] GHA CI Build & Quality Gates 背书

PRD: v1.3 §4.5 通用 (FR-MSG-9020)

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

- [ ] **Step 4: worktree 闭环（PR merge 后）**

```bash
cd /Users/muzhou/FEP_v1.0
git worktree remove /Users/muzhou/FEP_v1.0_wt-p4-msg-m
git branch -d feat/p4-msg-m-9020-general-response
git worktree list  # 确认不在列表
```

---

## 自检清单

- [x] **1. PRD 覆盖度**: FR-MSG-9020 覆盖（matrix line ~194）。9005 明示不在本 Plan（head-only outbound 需 envelope builder 改造，独立 Plan）。
- [x] **2. 安全边界**: 无 SM2/SM3/SM4/key/脱敏关键词；MsgReturn9020 仅 OriMsgNo/Debug 无 PII。
- [x] **3. 占位符扫描**: 0 TBD/TODO；test envelope wrap 留 implementer 按真实 wrapCfxTemplate 签名装配是实现细节非 Plan 占位。
- [x] **4. 类型一致性**: MsgReturn9020 / getOriMsgNo / getSerialNo / ResponseBusinessHead 跨 Task 一致。
- [x] **5. 测试命令可执行**: `-Dtest=OutboundWireShape9020XsdComplianceTest` + `-Dtest=Inbound9020WireTest` 与类名匹配；`-Dsurefire.failIfNoSpecifiedTests=false`（红线 `feedback_surefire3_failifno_specified_tests_param_rename`）。
- [x] **6. CLAUDE.md 更新**: T3 Step 2 含 outbound 39→40 + inbound 23→24（file write only）。
- [x] **7. 验收标准完整性**: 断言值（40/7/24/OriMsgNo="3000"/Result="00000"）可手算验证。
- [x] **8. 共享工具类无遗漏**: 无新增；复用 SHARED_VALIDATOR/wrapCfxTemplate。
- [x] **9. 核心类职责边界**: 无新增 ≥3 依赖 Service。
- [x] **10. Worktree 触发条件自检**:
  - [ ] 跨 ≥ 3 模块？✅ fep-converter + fep-processor + fep-web（命中）
  - [x] 与已签字未执行 Plan 并存？✅ 别会话多 Plan
  - [ ] ⛔ 安全 vs AI？✗
  - [ ] TLQ tongtech？✗
  - [x] ≥5 min verify 并行？✅ 全 reactor verify ~8-12min
  - [x] muzhou WIP / 多会话并存？✅ 7+ worktree 活跃
  - **结论**: ✅ 必须 worktree（命中 4 项），已建 `wt-p4-msg-m`；T3 Step 4 含 remove。

---

## 红线交叉引用

| Task | 适用红线 | 应对 |
|---|---|---|
| T1 | `feedback_plan_xsd_field_table_default_not_optional` | 字段表含 minOccurs（OriMsgNo=1 / Debug=0）|
| T1 | `feedback_xsd_compliance_fix_real_validator_on_sut` + `feedback_xsd_validator_requires_full_envelope_redline` | 真 XsdValidator + 完整 CFX envelope（含 Result 5 位）|
| T1 | `feedback_fixture_data_must_satisfy_xsd_constraints` | OriMsgNo="3000" 满足 MsgNo Number；Result="00000" 满足 5 位 |
| T2 | `feedback_registered_inbound_body_must_implement_serialnobearing` | MsgReturn9020 加 implements SerialNoBearing + getSerialNo→null |
| T2 | `feedback_full_regression_before_commit` | ArchUnit SerialNoBearing 不变量须全模块 verify 抓 |
| T2 | `feedback_plan_template_data_point_self_consistency` | 23→24 计数全文自洽 |
| T3 | `feedback_fep_docs_repo_commit_taboo` + `feedback_plan_step3_commit_template_residue` | CLAUDE.md + PRD matrix 均 file write only 禁 commit |
| T3 | `feedback_worktree_for_parallel_work` | T3 Step 4 含 worktree remove |
| 全程 | `feedback_baseline_drift_during_long_review_cycle` | baseline f61749a，签字与实施跨时重测 |
| 全程 | `feedback_subagent_must_commit_before_exit` / `_model_override_auth_fragility` / `_no_background_bash_in_workflow` / `_meta_comment_no_tool_use` | implementer dispatch 全周期纪律 |
| 全程 | `feedback_task_review_discipline` | 每 Task 派独立 spec + quality review |
| 全程 | `feedback_logsanitizer_alone_insufficient_for_findsecbugs` | 本 Plan 无新 logger（无 listener），不触发；如改动引入 LOG 须 wrap + @SuppressFBWarnings 含 LOG 的方法 |

---

## 评审与签字

> ⚠️ **本 Plan 必须先通过 AI 独立评审 + muzhou 人工签字方可执行**（CLAUDE.md "Plan 治理"段）

### AI 独立评审区

Round 1（santa-method）**✅ PASS** — 零 API drift，全 grep 锚点实测对齐（REAL_HEAD_RESPONSE 6 元素 / REGISTERED_MSG_NO_COUNT 39 / BodyClassRegistry 包路径 / BODY_TYPE_REGISTRY 23 / MsgReturn9020 结构 / ResponseHead.Result / SerialNoBearing / wrapCfxTemplate 8 参 / 参照测试存在）；字段表 minOccurs 准确；数据点自洽；SerialNoBearing 红线正确应用；/FEP/ docs commit taboo 双标注。2 🟡 CONCERN（BodyClassRegistry 锚点末 entry 实测 9120 非 9000 / InboundMessageDispatcherTest 方法名含 23 需重命名）→ v0.2 boil-lake 修订 3 处（红线 `feedback_concern_boil_lake_when_cheap_and_safe`）+ 自洽 grep 自检 clean。

### muzhou 签字区

v0.2 修订路径: v0.1 起草（grep 实测全 API）→ Round 1 AI 评审 PASS + 2 🟡 CONCERN → v0.2 boil-lake 3 修订 → 自检 clean → muzhou ✅ 签字。

| 项 | 决定 | 签字日期 |
|---|---|---|
| 批准 / 驳回 / 部分修改 | **✅ 批准** | 2026-06-02 |
| 签字 | **muzhou** | 2026-06-02 |

### 下会话实施起点

本会话 Plan v0.2 已签字 + commit + push，**不执行 T1/T2/T3**。下会话起点：
1. baseline 重测（红线 `feedback_baseline_drift_during_long_review_cycle`，跨时须重测 + currency 实测确认 origin/main HEAD）
2. cd `/Users/muzhou/FEP_v1.0_wt-p4-msg-m` + 实测 `git log origin/main..HEAD` 确认仅 Plan-only commit + rebase origin/main（若漂移）
3. dispatch implementer T1 → 主对话 spec + quality review → T2 → review → T3 闭环
4. implementer 纪律：无 model override / 前台 Bash / Status 起头 / 必 commit before exit / 全模块 verify before commit
5. T3 后 session-end full 6-phase（产出 production code 非 Lightweight）
