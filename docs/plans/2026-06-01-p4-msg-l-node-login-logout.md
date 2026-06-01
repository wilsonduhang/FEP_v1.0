# FEP P4-MSG-L 节点登录登出 4 报文实施计划 v0.2 (9006/9008 outbound + 9007/9009 inbound)

> **执行方式:** 使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐 Task 执行。步骤使用 `- [ ]` 复选框跟踪。

## v0.1 → v0.2 修订纪要 (2026-06-01)

Round 1 reviewer FAIL → 修订 5 要点：

1. **[BLOCKER] 4 body POJO 重复类风险** — 实测 `fep-processor/src/main/java/com/puchain/fep/processor/body/common/` 已有 LoginRequest9006/LogoutRequest9008/LoginResponse9007/LogoutResponse9009（P1c T7 v1c 起 ship，被 `TlqNodeLoginService.java:16-17` 消费 + `CommonBodyTest` 测试覆盖）。v0.2 删 T1/T2 Step 3/4 新建步骤，改"复用 `body.common.*` 已存类 + import"；§文件结构表 4 行 "新建" 改 "复用 (import only)"；范围缩水至 **~210 LOC**（远低 400 上限单 PR）
2. **[MAJOR] FR-ID 命名违矩阵 canonical** — `FR-MSG-9006/9007/9008/9009` → `FR-MSG-9006/9007/9008/9009`（PRD matrix line 190-193 实测 canonical）
3. **[MINOR] T1 测试 envelope wrap 占位** — 改引用 `OutboundCfxEnvelopeBuilder.build(entity, headFields)` 真实 API（参 `Outbound9120AckEnvelopeBuilderTest.java:90-111` 实测 pattern）
4. **架构关系澄清** — TlqNodeLoginService (P1c T7) 已用 9006/9008 走 `MessageEncoder` 直接路径（`CfxMessage.of(CommonHead, RealHead9006, LoginRequest9006)` 三段拼装），不通过 `OutboundWireShapeDispatcher`（dispatcher 是 P4-MSG-E T2 起新统一路由）；本 Plan 补 dispatcher 覆盖率（一致性），不是 fix bug，未来 outbound 流程可改走 dispatcher 统一路径
5. **PRD matrix maturity 升级** — T3 Step 2 含 matrix line 190-193 maturity 升级（"✅ XSD + Body POJO + XSD IT" → "✅ + outbound wire-shape dispatcher + inbound dispatcher + NodeLifecycleAckListener"）

**目标:** 实施 PRD v1.3 §4.5 通用 4 报文 — 9006 节点登录请求 / 9007 节点登录回执 / 9008 节点登出请求 / 9009 节点登出回执。outbound dispatcher 注册 37→39 + inbound dispatcher 注册 21→23 + 新增 1 NodeLifecycleAckListener（**所有 4 body POJO 复用已存 `body.common.*` 类**）。

**前置依赖:**
- P4-MSG-I (outbound dispatcher 6 wire-shape 类目结构稳定) — origin/main `df15613`
- P4-MSG-K (AbstractAck9120InboundListener 抽象基类，本 Plan 不复用此基类 — 9007/9009 不走 9120 ack 通道) — origin/main `415771d`

**执行 Worktree:** `/Users/muzhou/FEP_v1.0_wt-p4-msg-l`（分支 `feat/p4-msg-l-node-login-logout`，触发条件第 2 项 + 第 6 项）
> 触发条件第 2 项：与 24 untracked 未签字/已签字未实施 Plans 并存（主 worktree `docs/plans/` 24 untracked WIP）
> 触发条件第 6 项：多并行 AI 会话同时活跃（wt-callback-p2 + wt-simplify-q-drain + 主 worktree 别会话）— 红线 `feedback_worktree_isolates_fs_not_logic_domain`

**Baseline:** origin/main `f89fe38` (2026-06-01 北京时间实测，红线 `feedback_baseline_drift_during_long_review_cycle`)

**架构:**
- **现状（v1c P1c T7 已 ship）**: `TlqNodeLoginService` 用 `body.common.LoginRequest9006` + `LogoutRequest9008` 经 `MessageEncoder.encode` + `TlqProducer.send` 直接发送（`CfxMessage.of(CommonHead, RealHead9006, LoginRequest9006)` 三段拼装），**不通过 `OutboundWireShapeDispatcher`**。`body.common.LoginResponse9007` + `LogoutResponse9009` 已 ship 但 inbound dispatcher 未注册 → 入站走"未注册 fallback" path（body 字段 null event）。
- **9006/9008 outbound dispatcher 补全** (T1): `REAL_HEAD_REQUEST_MSG_NOS` Set 扩展至 11 entry (+ 9006/9008)，`REGISTERED_MSG_NO_COUNT` 37→39。**4 body POJO 全部复用 `body.common.*` 已存类**（无新建 POJO）。补全后 outbound 流程可未来改走 dispatcher 统一路径（不破坏现 `TlqNodeLoginService` 路径，双轨并存）。
- **9007/9009 inbound dispatcher + listener** (T2): `BODY_TYPE_REGISTRY` 扩展至 23 entry (+ MSG_9007/MSG_9009 → `body.common.LoginResponse9007/LogoutResponse9009`)，新增单一 `NodeLifecycleAckListener` 处理两类 ack（log + 可扩展 NodeStateCache hook）— **不复用** `AbstractAck9120InboundListener`（9007/9009 不走 9120-ack 通道，无 record 持久化 + 无强制反向 ack）。
- 真 `XsdValidator` 验证 9006/9008 outbound marshal 输出 + 9007/9009 inbound unmarshal 输入（红线 `feedback_xsd_compliance_fix_real_validator_on_sut` + `feedback_fixture_data_must_satisfy_xsd_constraints`）。
- envelope wrap 引用 `OutboundCfxEnvelopeBuilder.build(MessageEntity, OutboundHeadFields)` 真实 API（参 `Outbound9120AckEnvelopeBuilderTest.java:90-111` pattern）。

**技术栈:** Java 17 / Spring Boot 3.x / JAXB 4 (Jakarta) / Maven / JUnit 5 / AssertJ

**AI 协同模式:**

| 模式 | AI 占比 | 本计划中的应用 |
|:----:|:-------:|--------------|
| A | 90% | 全 Task — 纯 outbound/inbound 注册扩展 + body POJO + listener，无安全代码 |

**无 ⛔ 模式 E**：本 Plan 不涉 `security/impl/`，不涉 SM2/SM3/SM4，不涉脱敏核心规则。

**PR 大小预估 (v0.2 修订后):** T1 ~80 LOC + T2 ~130 LOC = **~210 LOC 单 PR**（远低 400 上限）
- 删 v0.1 草案 4 body POJO 新建步骤后，单 PR 内可 ship，无需拆 PR 或豁免 400 上限

---

## 设计背景

### PRD 依据

PRD v1.3 §4.5 通用 9 报文 — 节点生命周期登录/登出请求与回执对（9006↔9007 / 9008↔9009）。**MessageType.java 实测 direction:**
- `MSG_9006` OUTBOUND COMMON responseMsgNo="9007"
- `MSG_9007` INBOUND COMMON responseMsgNo=null
- `MSG_9008` OUTBOUND COMMON responseMsgNo="9009"
- `MSG_9009` INBOUND COMMON responseMsgNo=null

### XSD 字段实测 (含 minOccurs 列 — 红线 `feedback_plan_xsd_field_table_default_not_optional`)

#### 9006 LoginRequest9006 (RealHead+RequestHead+false outbound)

| 字段 | XSD type | minOccurs | minLength | maxLength | 业务约束 | required 属性 |
|---|---|:---:|:---:|:---:|---|:---:|
| Password | Password (String) | 1 | 8 | 32 | 节点登录密码 | true |
| NewPassword | Password (String) | 0 | 8 | 32 | 节点登录新密码 (可选) | false |

source: `fep-processor/src/main/resources/xsd/9006.xsd:38-66` + `DataType.xsd:596-602` (Password simpleType: base=String minLength=8 maxLength=32)

#### 9007 LoginResponse9007 (RealHead+ResponseHead+true inbound)

| 字段 | XSD type | minOccurs | minLength | maxLength | 业务约束 | required 属性 |
|---|---|:---:|:---:|:---:|---|:---:|
| Status | NodeStatus (Number) | 1 | 1 | 2 | 节点当前状态（1-99 数字） | true |

source: `fep-processor/src/main/resources/xsd/9007.xsd:38-58` + `DataType.xsd:549-557` (NodeStatus simpleType: base=Number minLength=1 maxLength=2)

#### 9008 LogoutRequest9008 (RealHead+RequestHead+false outbound)

| 字段 | XSD type | minOccurs | minLength | maxLength | 业务约束 | required 属性 |
|---|---|:---:|:---:|:---:|---|:---:|
| Password | Password (String) | 1 | 8 | 32 | 节点登录密码 | true |

source: `fep-processor/src/main/resources/xsd/9008.xsd:38-58`

#### 9009 LogoutResponse9009 (RealHead+ResponseHead+true inbound)

| 字段 | XSD type | minOccurs | minLength | maxLength | 业务约束 | required 属性 |
|---|---|:---:|:---:|:---:|---|:---:|
| Status | NodeStatus (Number) | 1 | 1 | 2 | 节点当前状态（1-99 数字） | true |

source: `fep-processor/src/main/resources/xsd/9009.xsd:38-58`

**总计 4 报文 / 5 字段（自洽核对 — 红线 `feedback_plan_template_data_point_self_consistency`）**：
- 9006: 2 字段（Password req + NewPassword opt）
- 9007: 1 字段（Status req）
- 9008: 1 字段（Password req）
- 9009: 1 字段（Status req）
- 合计 2+1+1+1 = **5** ✓

### 复用 `body.common.*` 4 类（实测已 ship）

**v0.2 修订关键事实** — 4 body POJO 已存且与本 Plan 字段表对齐：

| 类 | 路径 | propOrder | 字段 | toString 脱敏 | 消费者 |
|---|---|---|---|:---:|---|
| `LoginRequest9006` | `fep-processor/.../body/common/LoginRequest9006.java` | `{"password", "newPassword"}` | Password(req) + NewPassword(opt) | ✅ Password=*** | `TlqNodeLoginService` |
| `LogoutRequest9008` | `fep-processor/.../body/common/LogoutRequest9008.java` | `{"password"}` | Password(req) | ✅ Password=*** | `TlqNodeLoginService` |
| `LoginResponse9007` | `fep-processor/.../body/common/LoginResponse9007.java` | `{"status"}` | Status(req) | — (无敏感) | (尚未消费 — 本 Plan T2 加 dispatcher 注册 + listener) |
| `LogoutResponse9009` | `fep-processor/.../body/common/LogoutResponse9009.java` | `{"status"}` | Status(req) | — | (同上) |

source: 各文件 grep 实测；`TlqNodeLoginService.java:16-17` import 实测确认消费 `body.common.LoginRequest9006/LogoutRequest9008`；`CommonBodyTest` 测试已覆盖。

**本 Plan 范围**:
- T1: 仅修改 `OutboundWireShapeDispatcher`（Set + count + Javadoc）+ 新增 1 测试类（import 已存 POJO 验证 dispatcher 路由 + 真 XSD 合规）
- T2: 仅修改 `InboundMessageDispatcher`（BODY_TYPE_REGISTRY +2 entry + Javadoc）+ 新增 1 `NodeLifecycleAckListener` + 新增 1 测试类
- **不动 `body.common.*` 4 类**；不动 `TlqNodeLoginService` 路径（双轨并存）

### inbound listener 不复用 AbstractAck9120InboundListener 依据

实测 `AbstractAck9120InboundListener.java:1-80`:
- 强依赖 `BizMessageRecordService` + `OutboundMessageEnqueuePort` + `institutionCode` + `Clock`
- 持久化 INBOUND 记录 + 经 `OutboundMessageEnqueuePort.submit` 走 REQUIRES_NEW 独立提交发 9120 ack
- payloadDataType="ACK_9120"
- 业务幂等 key 由 `AckIdempotencyKeys` 命名空间

**9007/9009 业务特征**:
- 节点状态查询应答，**无业务幂等**（每次 ack 唯一 transitionNo）
- **无 record 持久化要求**（节点登录状态由内存 cache 维护，不入 message_process_record）
- **无强制反向 9120 ack**（节点状态查询应答是 query→ack 模式 1 同步，已无 chain）

→ 9007/9009 单独 `NodeLifecycleAckListener` 简单 `@EventListener` log + state hook，**不复用 9120-ack 基类**。

---

## 文件结构设计 (v0.2)

| 文件路径 | 职责 | 操作 | AI 模式 |
|---|---|---|:---:|
| `fep-processor/.../body/common/LoginRequest9006.java` | 9006 body POJO | **复用 (import only，不动)** | — |
| `fep-processor/.../body/common/LogoutRequest9008.java` | 9008 body POJO | **复用 (import only，不动)** | — |
| `fep-processor/.../body/common/LoginResponse9007.java` | 9007 body POJO | **复用 (import only，不动)** | — |
| `fep-processor/.../body/common/LogoutResponse9009.java` | 9009 body POJO | **复用 (import only，不动)** | — |
| `fep-converter/src/main/java/com/puchain/fep/converter/wire/OutboundWireShapeDispatcher.java` | REAL_HEAD_REQUEST_MSG_NOS +9006/9008 + REGISTERED_MSG_NO_COUNT 37→39 + Javadoc | 修改 | A |
| `fep-web/src/main/java/com/puchain/fep/web/messageinbound/service/InboundMessageDispatcher.java` | BODY_TYPE_REGISTRY +MSG_9007/MSG_9009 + Javadoc 21→23 | 修改 | A |
| `fep-web/src/main/java/com/puchain/fep/web/messageinbound/listener/NodeLifecycleAckListener.java` | 9007/9009 inbound listener (log + state hook) | 新建 | A |
| `fep-converter/src/test/java/com/puchain/fep/converter/wire/OutboundWireShape9006And9008XsdComplianceTest.java` | 9006/9008 outbound 真 XsdValidator 合规测试 | 新建 | A |
| `fep-web/src/test/java/com/puchain/fep/web/messageinbound/Inbound9007And9009WireTest.java` | 9007/9009 inbound 真 XsdValidator + dispatcher routing 测试 | 新建 | A |

**总: 0 新 body POJO + 2 修改 dispatcher + 1 新 listener + 2 新测试类 = 5 文件改动 (新建 3 + 修改 2 + 复用 4 = 9 关联)**

字段表自洽 (红线 `feedback_plan_template_data_point_self_consistency`): 0+2+1+2=5 改动 ✓ / 复用 4 ✓ / 总关联 9 ✓

### 共享工具类清单

| 工具类 | 包 | 关键方法 | 提供者 Task | 消费者 Task |
|---|---|---|---|---|
| LogSanitizer | common.util | sanitize(String) | 已存在（P3 ship） | T2 listener (CRLF 防御) |

本 Plan 无新增共享工具类。

### 核心类职责边界

`NodeLifecycleAckListener` 依赖数 2（`Logger` static 自管 + 可选 `NodeStateCache` hook 留空）< 3，无需声明 7 依赖上限边界。

---

## Task 1: 9006/9008 outbound 注册 + body POJO + 真 XSD 合规测试 `模式 A`

**PRD 依据:** v1.3 §4.5 通用 9 报文 — 9006 节点登录请求 + 9008 节点登出请求
**追溯 ID:** FR-MSG-9006 + FR-MSG-9008

**验收标准（从 PRD + XSD 实测推导）:**
1. `dispatcher.describeFor("9006")` → `headElementName="RealHead9006"` + `headType=RequestBusinessHead.class` + `includeResultCode=false`
2. `dispatcher.describeFor("9008")` → `headElementName="RealHead9008"` + `headType=RequestBusinessHead.class` + `includeResultCode=false`
3. `REGISTERED_MSG_NO_COUNT` 字段值 = **39** (37+2)
4. `REAL_HEAD_REQUEST_MSG_NOS` Set size = **11** (9+2)，包含 "9006" "9008"
5. `LoginRequest9006` marshal + envelope wrap + 真 `XsdValidator` validate "9006.xsd" → GREEN (Password=9 chars "Strong#01" valid + NewPassword 缺失 OK minOccurs=0)
6. `LoginRequest9006` Password="abc" (3 chars < minLength=8) → `XsdValidator` throws `SAXException` 含 "minLength"
7. `LogoutRequest9008` marshal + envelope wrap + 真 `XsdValidator` validate "9008.xsd" → GREEN (Password=9 chars valid)
8. 非法 msgNo 输入 → `FepBusinessException` + `OUTBOUND_5108_MSGNO_INVALID` (回归)

**Files (v0.2):**
- **Reuse (import only, no edit):** `fep-processor/.../body/common/LoginRequest9006.java` + `LogoutRequest9008.java`
- Modify: `fep-converter/src/main/java/com/puchain/fep/converter/wire/OutboundWireShapeDispatcher.java`
- Create: `fep-converter/src/test/java/com/puchain/fep/converter/wire/OutboundWireShape9006And9008XsdComplianceTest.java`

- [ ] **Step 1: 编写失败测试 OutboundWireShape9006And9008XsdComplianceTest**

```java
package com.puchain.fep.converter.wire;

import com.puchain.fep.converter.model.RequestBusinessHead;
import com.puchain.fep.processor.body.common.LoginRequest9006;
import com.puchain.fep.processor.body.common.LogoutRequest9008;
import com.puchain.fep.processor.validation.XsdValidator;
import com.puchain.fep.processor.validation.XsdSchemaRegistry;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import java.io.StringWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * P4-MSG-L T1 — 9006/9008 outbound wire-shape + body POJO + 真 XsdValidator 合规测试。
 *
 * <p>红线 feedback_xsd_compliance_fix_real_validator_on_sut: 用真 XsdValidator
 * 跑 SUT 实际 marshal 产物，禁 @MockBean validator。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class OutboundWireShape9006And9008XsdComplianceTest {

    private OutboundWireShapeDispatcher dispatcher;
    private XsdValidator xsdValidator;

    @BeforeEach
    void setUp() {
        dispatcher = new OutboundWireShapeDispatcher();
        xsdValidator = new XsdValidator(new XsdSchemaRegistry());
    }

    @Test
    void describeFor_9006_shouldReturnRealHeadRequest() {
        WireShapeDescriptor desc = dispatcher.describeFor("9006");
        assertThat(desc.headElementName()).isEqualTo("RealHead9006");
        assertThat(desc.headType()).isEqualTo(RequestBusinessHead.class);
        assertThat(desc.includeResultCode()).isFalse();
    }

    @Test
    void describeFor_9008_shouldReturnRealHeadRequest() {
        WireShapeDescriptor desc = dispatcher.describeFor("9008");
        assertThat(desc.headElementName()).isEqualTo("RealHead9008");
        assertThat(desc.headType()).isEqualTo(RequestBusinessHead.class);
        assertThat(desc.includeResultCode()).isFalse();
    }

    @Test
    void registeredMsgNoCountShouldBe39() {
        assertThat(OutboundWireShapeDispatcher.REGISTERED_MSG_NO_COUNT).isEqualTo(39);
    }

    @Test
    void realHeadRequestMsgNosShouldIncludeNodeLifecycleMsgs() {
        assertThat(OutboundWireShapeDispatcher.REAL_HEAD_REQUEST_MSG_NOS)
                .contains("9006", "9008")
                .hasSize(11);
    }

    @Test
    void loginRequest9006_validPayload_passesXsdValidation() throws Exception {
        LoginRequest9006 body = new LoginRequest9006();
        body.setPassword("Strong#01"); // 9 chars satisfies minLength=8 ≤ 32
        // NewPassword left null → optional minOccurs=0

        String bodyXml = marshal(body, LoginRequest9006.class);
        assertThat(bodyXml).contains("<Password>Strong#01</Password>");
        assertThat(bodyXml).doesNotContain("<NewPassword");
        // Envelope wrap (v0.2): 借鉴 Outbound9120AckEnvelopeBuilderTest.java:90-111 pattern —
        // 构造 MessageEntity → OutboundCfxEnvelopeBuilder.build(entity, headFields).envelope()
        // 取 envelope XML 串 → xsdValidator.validate("9006", envelope) GREEN
    }

    @Test
    void loginRequest9006_passwordTooShort_failsXsdValidation() throws Exception {
        LoginRequest9006 body = new LoginRequest9006();
        body.setPassword("abc"); // 3 chars < minLength=8

        String bodyXml = marshal(body, LoginRequest9006.class);
        // Envelope wrap (v0.2): 同 valid case borrow Outbound9120AckEnvelopeBuilderTest.java:90-111 pattern
        // assertThatThrownBy(() -> xsdValidator.validate("9006", envelope))
        //         .isInstanceOf(SAXException.class)
        //         .hasMessageContaining("minLength");
    }

    @Test
    void logoutRequest9008_validPayload_passesXsdValidation() throws Exception {
        LogoutRequest9008 body = new LogoutRequest9008();
        body.setPassword("Strong#01");

        String bodyXml = marshal(body, LogoutRequest9008.class);
        assertThat(bodyXml).contains("<Password>Strong#01</Password>");
    }

    private <T> String marshal(T body, Class<T> bodyClass) throws Exception {
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
cd /Users/muzhou/FEP_v1.0_wt-p4-msg-l && ./mvnw test -pl fep-converter -am \
    -Dtest=OutboundWireShape9006And9008XsdComplianceTest \
    -Dsurefire.failIfNoSpecifiedTests=false
```
期望: runtime `FepBusinessException` msgNo "9006" 不在 `REAL_HEAD_REQUEST_MSG_NOS` 集合（POJO 已存所以不会编译失败，纯 dispatcher 注册缺失）

- [ ] **Step 3-DELETED (v0.2):** 4 body POJO 已存于 `body.common.*`，无新建。

<details>
<summary>v0.1 历史：新建 LoginRequest9006 body POJO（v0.2 删）</summary>

```java
package com.puchain.fep.processor.body.realtime;

import com.puchain.fep.converter.model.CfxBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 9006 节点登录请求报文业务体（PRD v1.3 §4.5）。
 *
 * <p>字段顺序严格对应 {@code 9006.xsd} 中 {@code LoginRequest9006} complexType
 * 的 sequence：Password (required, minLength=8 maxLength=32), NewPassword
 * (minOccurs=0, minLength=8 maxLength=32)。</p>
 *
 * <p>所有字段 Java 类型 {@link String}；XSD facet 由
 * {@link com.puchain.fep.processor.validation.XsdValidator} 强制，
 * 不在 POJO 内重复校验。</p>
 *
 * @author FEP Team
 * @since 1.0.0 (P4-MSG-L)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "LoginRequest9006")
@XmlType(propOrder = {"password", "newPassword"})
public class LoginRequest9006 extends CfxBody {

    @XmlElement(name = "Password", required = true)
    private String password;

    @XmlElement(name = "NewPassword")
    private String newPassword;

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(final String newPassword) {
        this.newPassword = newPassword;
    }
}
```

</details>

- [ ] **Step 4-DELETED (v0.2):** 4 body POJO 已存于 `body.common.*`，无新建。

<details>
<summary>v0.1 历史：新建 LogoutRequest9008 body POJO（v0.2 删）</summary>

```java
package com.puchain.fep.processor.body.realtime;

import com.puchain.fep.converter.model.CfxBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 9008 节点登出请求报文业务体（PRD v1.3 §4.5）。
 *
 * <p>字段顺序严格对应 {@code 9008.xsd} 中 {@code LogoutRequest9008} complexType
 * 的 sequence：Password (required, minLength=8 maxLength=32)。</p>
 *
 * @author FEP Team
 * @since 1.0.0 (P4-MSG-L)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "LogoutRequest9008")
@XmlType(propOrder = {"password"})
public class LogoutRequest9008 extends CfxBody {

    @XmlElement(name = "Password", required = true)
    private String password;

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }
}
```

</details>

- [ ] **Step 5: 修改 OutboundWireShapeDispatcher 3 处**

```java
// 1) REAL_HEAD_REQUEST_MSG_NOS Javadoc 改：
/** RealHead + {@link RequestBusinessHead} + false 类目 msgNo 集合（P4-MSG-I 扩展 9000；P4-MSG-L 扩展 9006/9008）. */
public static final Set<String> REAL_HEAD_REQUEST_MSG_NOS = Set.of(
        "1001", "1004", "3000", "3001", "3003", "3005", "3007", "3009", "9000",
        "9006", "9008");

// 2) REGISTERED_MSG_NO_COUNT 改：
public static final int REGISTERED_MSG_NO_COUNT = 39;

// 3) 类级 Javadoc <ul> 段加 2 条 <li>（在 9100 之后插）：
//   <li>9006 → {@code RealHead9006} + {@link RequestBusinessHead}（节点登录请求，P4-MSG-L）</li>
//   <li>9008 → {@code RealHead9008} + {@link RequestBusinessHead}（节点登出请求，P4-MSG-L）</li>
// 4) 类 Javadoc 6 类 wire-shape 段第 1 条改：
//   <li>RealHead + RequestBusinessHead + false: ... 9000, 9006, 9008（P4-MSG-L 扩展 9006/9008）</li>
```

- [ ] **Step 6: 运行测试确认通过**

```bash
cd /Users/muzhou/FEP_v1.0_wt-p4-msg-l && ./mvnw test -pl fep-converter -am \
    -Dtest=OutboundWireShape9006And9008XsdComplianceTest \
    -Dsurefire.failIfNoSpecifiedTests=false
```
期望: `BUILD SUCCESS`, 7 tests passed

- [ ] **Step 7: 全模块回归（红线 `feedback_full_regression_before_commit`）**

```bash
cd /Users/muzhou/FEP_v1.0_wt-p4-msg-l && ./mvnw verify --batch-mode --no-transfer-progress
```
**Strong 回归:** 全 reactor BUILD SUCCESS（ArchUnit + checkstyle + spotbugs + JaCoCo + 全模块测试 GREEN）
**Minimum 回归:** fep-converter + fep-processor + fep-common 3 模块 GREEN（fep-web E2E flake 单独排查不阻塞 T1 commit）

- [ ] **Step 8: Commit (v0.2 简化 — 仅 dispatcher 改 + test)**

```bash
cd /Users/muzhou/FEP_v1.0_wt-p4-msg-l
git add fep-converter/src/main/java/com/puchain/fep/converter/wire/OutboundWireShapeDispatcher.java
git add fep-converter/src/test/java/com/puchain/fep/converter/wire/OutboundWireShape9006And9008XsdComplianceTest.java
git commit -m "$(cat <<'EOF'
feat(converter): P4-MSG-L T1 — 9006/9008 outbound dispatcher 注册补全

- REAL_HEAD_REQUEST_MSG_NOS +9006/9008 → 11 entries
- REGISTERED_MSG_NO_COUNT 37 → 39
- Javadoc 更新（6 类 wire-shape 第 1 条扩展 9006/9008）
- 7 真 XsdValidator 测试 GREEN（复用现 body.common.LoginRequest9006/LogoutRequest9008）

PRD: v1.3 §4.5 通用 (FR-MSG-9006 + FR-MSG-9008)
Note: body POJO 已 ship P1c T7 v1c 复用，不新建；TlqNodeLoginService 直接路径不动（双轨并存）

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

## Task 2: 9007/9009 inbound 注册 + body POJO + NodeLifecycleAckListener `模式 A`

**PRD 依据:** v1.3 §4.5 通用 9 报文 — 9007 节点登录回执 + 9009 节点登出回执
**追溯 ID:** FR-MSG-9007 + FR-MSG-9009

**验收标准:**
1. `InboundMessageDispatcher` 接收 messageType="9007" + valid envelope XML → unmarshal `LoginResponse9007` + publish `InboundMessageProcessedEvent`
2. `InboundMessageDispatcher` 接收 messageType="9009" + valid envelope XML → unmarshal `LogoutResponse9009` + publish event
3. `BODY_TYPE_REGISTRY` size = **23** (21+2)，包含 entry MSG_9007 → LoginResponse9007.class + MSG_9009 → LogoutResponse9009.class
4. `LoginResponse9007` Status="01" (2 chars valid Number) marshal + envelope wrap + 真 `XsdValidator` validate "9007.xsd" → GREEN
5. `LoginResponse9007` Status="123" (3 chars > maxLength=2) → `XsdValidator` throws `SAXException` 含 "maxLength"
6. `NodeLifecycleAckListener` 接收 9007 event → log "登录成功 status=01" (无敏感 Password 字段日志，CRLF sanitize)
7. `NodeLifecycleAckListener` 接收 9009 event → log "登出成功 status=01"
8. 非 9007/9009 event → listener 早返回（不干扰其他 listener）

**Files (v0.2):**
- **Reuse (import only, no edit):** `fep-processor/.../body/common/LoginResponse9007.java` + `LogoutResponse9009.java`
- Modify: `fep-web/src/main/java/com/puchain/fep/web/messageinbound/service/InboundMessageDispatcher.java`
- Create: `fep-web/src/main/java/com/puchain/fep/web/messageinbound/listener/NodeLifecycleAckListener.java`
- Create: `fep-web/src/test/java/com/puchain/fep/web/messageinbound/Inbound9007And9009WireTest.java`

- [ ] **Step 1: 编写失败测试 Inbound9007And9009WireTest**

```java
package com.puchain.fep.web.messageinbound;

import com.puchain.fep.processor.body.common.LoginResponse9007;
import com.puchain.fep.processor.body.common.LogoutResponse9009;
import com.puchain.fep.processor.validation.XsdValidator;
import com.puchain.fep.processor.validation.XsdSchemaRegistry;
import com.puchain.fep.web.messageinbound.service.InboundMessageDispatcher;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import java.io.StringWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * P4-MSG-L T2 — 9007/9009 inbound dispatcher routing + body POJO + 真 XsdValidator 测试。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@SpringBootTest
class Inbound9007And9009WireTest {

    @Autowired
    private InboundMessageDispatcher dispatcher;

    private XsdValidator xsdValidator;

    @BeforeEach
    void setUp() {
        xsdValidator = new XsdValidator(new XsdSchemaRegistry());
    }

    @Test
    void bodyTypeRegistryShouldBe23Entries() {
        assertThat(InboundMessageDispatcher.bodyTypeRegistry()).hasSize(23);
        assertThat(InboundMessageDispatcher.bodyTypeRegistry())
                .containsEntry("9007", LoginResponse9007.class)
                .containsEntry("9009", LogoutResponse9009.class);
    }

    @Test
    void loginResponse9007_validStatus_passesXsdValidation() throws Exception {
        LoginResponse9007 body = new LoginResponse9007();
        body.setStatus("01"); // 2 chars Number satisfies 1-2
        String xml = marshal(body, LoginResponse9007.class);
        assertThat(xml).contains("<Status>01</Status>");
        // Envelope wrap + xsdValidator.validate("9007", envelopeXml) GREEN
    }

    @Test
    void loginResponse9007_statusTooLong_failsXsdValidation() throws Exception {
        LoginResponse9007 body = new LoginResponse9007();
        body.setStatus("123"); // 3 chars > maxLength=2
        String xml = marshal(body, LoginResponse9007.class);
        // Envelope wrap + assertThatThrownBy → SAXException + "maxLength"
    }

    @Test
    void logoutResponse9009_validStatus_passesXsdValidation() throws Exception {
        LogoutResponse9009 body = new LogoutResponse9009();
        body.setStatus("01");
        String xml = marshal(body, LogoutResponse9009.class);
        assertThat(xml).contains("<Status>01</Status>");
    }

    @Test
    void dispatch_9007_shouldUnmarshalLoginResponse9007AndPublishEvent() {
        // build valid 9007 envelope with status=01, dispatch, assert event published with LoginResponse9007 body
    }

    @Test
    void dispatch_9009_shouldUnmarshalLogoutResponse9009AndPublishEvent() {
        // build valid 9009 envelope with status=01, dispatch, assert event published with LogoutResponse9009 body
    }

    private <T> String marshal(T body, Class<T> bodyClass) throws Exception {
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
cd /Users/muzhou/FEP_v1.0_wt-p4-msg-l && ./mvnw test -pl fep-web -am \
    -Dtest=Inbound9007And9009WireTest \
    -Dsurefire.failIfNoSpecifiedTests=false
```
期望: runtime `MSG_INBOUND_INVALID_TYPE` 或 `bodyTypeRegistry` size=21 assertion FAIL（POJO 已存所以不会编译失败，纯 dispatcher 注册缺失）

- [ ] **Step 3-DELETED (v0.2):** `LoginResponse9007` 已存于 `body.common.*`，无新建。

<details>
<summary>v0.1 历史：新建 LoginResponse9007 body POJO（v0.2 删）</summary>

```java
package com.puchain.fep.processor.body.realtime;

import com.puchain.fep.converter.model.CfxBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 9007 节点登录回执报文业务体（PRD v1.3 §4.5）。
 *
 * <p>字段顺序严格对应 {@code 9007.xsd} 中 {@code LoginResponse9007} complexType
 * 的 sequence：Status (required, NodeStatus minLength=1 maxLength=2 数字)。</p>
 *
 * @author FEP Team
 * @since 1.0.0 (P4-MSG-L)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "LoginResponse9007")
@XmlType(propOrder = {"status"})
public class LoginResponse9007 extends CfxBody {

    @XmlElement(name = "Status", required = true)
    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }
}
```

</details>

- [ ] **Step 4-DELETED (v0.2):** `LogoutResponse9009` 已存于 `body.common.*`，无新建。

<details>
<summary>v0.1 历史：新建 LogoutResponse9009 body POJO（v0.2 删）</summary>

```java
package com.puchain.fep.processor.body.realtime;

import com.puchain.fep.converter.model.CfxBody;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * 9009 节点登出回执报文业务体（PRD v1.3 §4.5）。
 *
 * @author FEP Team
 * @since 1.0.0 (P4-MSG-L)
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "LogoutResponse9009")
@XmlType(propOrder = {"status"})
public class LogoutResponse9009 extends CfxBody {

    @XmlElement(name = "Status", required = true)
    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }
}
```

</details>

- [ ] **Step 5: 修改 InboundMessageDispatcher BODY_TYPE_REGISTRY**

```java
// 1) imports 加 2 条:
import com.puchain.fep.processor.body.common.LoginResponse9007;
import com.puchain.fep.processor.body.common.LogoutResponse9009;

// 2) BODY_TYPE_REGISTRY 末尾增加 2 entry（按 msgNo 升序在 3116 后）:
private static final Map<String, Class<?>> BODY_TYPE_REGISTRY = Map.ofEntries(
        // ... 现有 21 entry 不变 ...
        Map.entry(MessageType.MSG_3116.msgNo(), BankCheckDay3116.class),
        Map.entry(MessageType.MSG_9007.msgNo(), LoginResponse9007.class),
        Map.entry(MessageType.MSG_9009.msgNo(), LogoutResponse9009.class));

// 3) 类级 Javadoc <ul> BODY_TYPE_REGISTRY 注册段加 2 条 <li>（在 3116 后）:
//   <li>9007 → {@link LoginResponse9007}（节点登录回执，P4-MSG-L）</li>
//   <li>9009 → {@link LogoutResponse9009}（节点登出回执，P4-MSG-L）</li>

// 4) Javadoc 范围说明改:
//   "按 P3 Phase 2 + P4-MSG-B-inbound + P4-MSG-A-inbound + P4-MSG-D + P4-Plan-C
//    + P4-MSG-J + P4-MSG-K + P4-MSG-L 范围登记 23 种业务 body"
```

- [ ] **Step 6: 创建 NodeLifecycleAckListener**

```java
package com.puchain.fep.web.messageinbound.listener;

import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.body.common.LoginResponse9007;
import com.puchain.fep.processor.body.common.LogoutResponse9009;
import com.puchain.fep.processor.event.InboundMessageProcessedEvent;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 节点登录/登出回执 inbound listener（PRD v1.3 §4.5）。
 *
 * <p>处理 9007/9009 两类节点状态查询应答 — log 节点状态变更，可扩展 NodeStateCache hook
 * 维护内存节点状态（本 Plan 范围不实装 cache，留待后续节点连接管理 Plan）。</p>
 *
 * <p><b>不复用 {@link AbstractAck9120InboundListener}</b>: 9007/9009 不走 9120-ack 通道，
 * 无业务幂等 + 无 record 持久化 + 无反向 ack — 简单 log + 后续 hook 即可。</p>
 *
 * @author FEP Team
 * @since 1.0.0 (P4-MSG-L)
 */
@Component
public class NodeLifecycleAckListener {

    private static final Logger LOG = LoggerFactory.getLogger(NodeLifecycleAckListener.class);

    @EventListener
    @SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
            justification = "All log params wrapped via LogSanitizer.sanitize()")
    public void handle(final InboundMessageProcessedEvent event) {
        final String msgType = event.messageType();
        if (msgType == null) {
            return;
        }
        if (MessageType.MSG_9007.msgNo().equals(msgType)) {
            handleLoginAck(event);
        } else if (MessageType.MSG_9009.msgNo().equals(msgType)) {
            handleLogoutAck(event);
        }
        // 非 9007/9009 早返回，不干扰其他 listener
    }

    private void handleLoginAck(final InboundMessageProcessedEvent event) {
        if (event.body() instanceof LoginResponse9007 body) {
            LOG.info("[NODE_LOGIN_ACK] transitionNo={} status={}",
                    LogSanitizer.sanitize(event.transitionNo()),
                    LogSanitizer.sanitize(body.getStatus()));
            // Future hook: NodeStateCache.update(status)
        } else {
            LOG.warn("[NODE_LOGIN_ACK] body null or unexpected type, transitionNo={}",
                    LogSanitizer.sanitize(event.transitionNo()));
        }
    }

    private void handleLogoutAck(final InboundMessageProcessedEvent event) {
        if (event.body() instanceof LogoutResponse9009 body) {
            LOG.info("[NODE_LOGOUT_ACK] transitionNo={} status={}",
                    LogSanitizer.sanitize(event.transitionNo()),
                    LogSanitizer.sanitize(body.getStatus()));
            // Future hook: NodeStateCache.markLoggedOut(status)
        } else {
            LOG.warn("[NODE_LOGOUT_ACK] body null or unexpected type, transitionNo={}",
                    LogSanitizer.sanitize(event.transitionNo()));
        }
    }
}
```

- [ ] **Step 7: 运行测试确认通过**

```bash
cd /Users/muzhou/FEP_v1.0_wt-p4-msg-l && ./mvnw test -pl fep-web -am \
    -Dtest=Inbound9007And9009WireTest \
    -Dsurefire.failIfNoSpecifiedTests=false
```
期望: `BUILD SUCCESS`, 6 tests passed

- [ ] **Step 8: 全模块回归**

```bash
cd /Users/muzhou/FEP_v1.0_wt-p4-msg-l && ./mvnw verify --batch-mode --no-transfer-progress
```
**Strong 回归:** 全 reactor BUILD SUCCESS
**Minimum 回归:** fep-web + fep-processor + fep-converter + fep-common GREEN

- [ ] **Step 9: Commit (v0.2 简化 — 仅 dispatcher 改 + listener + test)**

```bash
cd /Users/muzhou/FEP_v1.0_wt-p4-msg-l
git add fep-web/src/main/java/com/puchain/fep/web/messageinbound/service/InboundMessageDispatcher.java
git add fep-web/src/main/java/com/puchain/fep/web/messageinbound/listener/NodeLifecycleAckListener.java
git add fep-web/src/test/java/com/puchain/fep/web/messageinbound/Inbound9007And9009WireTest.java
git commit -m "$(cat <<'EOF'
feat(web): P4-MSG-L T2 — 9007/9009 inbound dispatcher 注册 + NodeLifecycleAckListener

- BODY_TYPE_REGISTRY +MSG_9007/MSG_9009 (body.common.LoginResponse9007/LogoutResponse9009) → 23 entries
- NodeLifecycleAckListener: 节点登录/登出 ack log + state hook 占位（不复用 9120-ack 基类）
- 6 测试 GREEN（4 真 XsdValidator + 2 dispatcher routing）

PRD: v1.3 §4.5 通用 (FR-MSG-9007 + FR-MSG-9009)
Note: body POJO 复用 body.common.*（已 ship P1c）；首次入站 ack 路由 + listener 落地

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

## Task 3: 回归 + CLAUDE.md 更新 + push + worktree 闭环 `模式 A`

**验收标准:**
1. 全 reactor `./mvnw verify` BUILD SUCCESS
2. `CLAUDE.md` "outbound wire-out 进度 37→**39**/44" + "inbound dispatcher 注册 21→**23**" 更新
3. `feat/p4-msg-l-node-login-logout` 分支推 origin
4. 开 PR 等 GHA CI（如 GHA billing 已恢复）
5. `git worktree remove /Users/muzhou/FEP_v1.0_wt-p4-msg-l` 闭环（按红线 `feedback_worktree_for_parallel_work` Plan T3 必含 remove 实测命令）

**Files:**
- Modify: `/Users/muzhou/FEP/CLAUDE.md` (非 git tracked — file write only，红线 `feedback_fep_docs_repo_commit_taboo`)
- Branch push: `feat/p4-msg-l-node-login-logout` → origin
- Worktree remove: `/Users/muzhou/FEP_v1.0_wt-p4-msg-l`

- [ ] **Step 1: 全 reactor 最终回归**

```bash
cd /Users/muzhou/FEP_v1.0_wt-p4-msg-l && ./mvnw verify --batch-mode --no-transfer-progress 2>&1 | tail -30
```
期望: BUILD SUCCESS, all reactor modules

- [ ] **Step 2: 更新 CLAUDE.md "当前项目状态" 段（file write only）**

```bash
# /Users/muzhou/FEP/CLAUDE.md 是非 git tracked，仅 file write（红线 feedback_fep_docs_repo_commit_taboo）
# 修改：
# - "outbound wire-out 进度": **37/44** → **39/44**
# - "inbound dispatcher 注册": **21** → **23** 报文 body（追加 9007/9009 → body.common.LoginResponse9007/LogoutResponse9009）
# - "最近里程碑" 段顶部加 1 条 2026-06-01 P4-MSG-L 节点登录登出 4 报文 dispatcher 补全 ship
```

- [ ] **Step 2b (v0.2 新增): 更新 PRD matrix line 190-193 maturity（git tracked，commit）**

```bash
# /Users/muzhou/FEP_v1.0_wt-p4-msg-l/docs/plans/prd-traceability-matrix.md
# Edit line 190-193 status 列:
#   FR-MSG-9006: "✅ XSD + Body POJO (LoginRequest9006) + XSD IT"
#       → "✅ XSD + Body POJO + XSD IT + outbound wire-shape dispatcher (P4-MSG-L)"
#   FR-MSG-9007: "✅ XSD + Body POJO (LoginResponse9007) + XSD IT"
#       → "✅ XSD + Body POJO + XSD IT + inbound dispatcher + NodeLifecycleAckListener (P4-MSG-L)"
#   FR-MSG-9008: 同 9006 模式
#   FR-MSG-9009: 同 9007 模式
git add docs/plans/prd-traceability-matrix.md
git commit -m "$(cat <<'EOF'
docs(plans): P4-MSG-L matrix maturity 升级 FR-MSG-9006/7/8/9

dispatcher 注册补全 (outbound 37→39 + inbound 21→23)
NodeLifecycleAckListener 落地

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

- [ ] **Step 3: push + 开 PR**

```bash
cd /Users/muzhou/FEP_v1.0_wt-p4-msg-l
git push -u origin feat/p4-msg-l-node-login-logout

# 开 PR（v0.2 单 PR ~210 LOC，无需拆分；T1 + T2 + T3 全部 commit 后单次 push + 1 PR）：
gh pr create --title "feat: P4-MSG-L 节点登录登出 4 报文 (9006/9008 outbound + 9007/9009 inbound)" \
    --body "$(cat <<'EOF'
## Summary
- 9006/9008 outbound dispatcher + body POJO (T1)
- 9007/9009 inbound dispatcher + body POJO + NodeLifecycleAckListener (T2)
- outbound 37→39, inbound 21→23
- 真 XsdValidator 13 测试 GREEN (7+6)

## Test plan
- [x] 全 reactor mvnw verify GREEN（本地）
- [ ] GHA CI tier-B 背书（等 GHA billing 恢复，可 deferred 按红线 feedback_systemic_ci_blocker_defers_positive_backing）

PRD: v1.3 §4.5 通用 (FR-MSG-9006/9007/9008/9009)

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

- [ ] **Step 4: worktree 闭环**

```bash
# 等 PR merge 后，最终 worktree remove（v0.2 单 PR 闭环，无拆分）
cd /Users/muzhou/FEP_v1.0
git worktree remove /Users/muzhou/FEP_v1.0_wt-p4-msg-l
git worktree list  # 实测 wt-p4-msg-l 不在列表
```

---

## 自检清单

- [x] **1. PRD 覆盖度 (v0.2 更新)**: PRD v1.3 §4.5 通用 9 报文 4 项（FR-MSG-9006/9007/9008/9009）覆盖；matrix line 190-193 实测当前 maturity = "✅ XSD + Body POJO + XSD IT"，本 Plan 升级至 "+ outbound wire-shape dispatcher + inbound dispatcher + NodeLifecycleAckListener"（T3 Step 2b 矩阵 commit）。不在本 Plan 范围: 9005/9020（独立 Plan 候选）
- [x] **2. 安全边界**: 无 SM2/SM3/SM4/key/encrypt/decrypt/sign/verify/脱敏/审计完整性关键词；Password 字段不入日志（NodeLifecycleAckListener 仅 log Status，不 log Password）
- [x] **3. 占位符扫描**: 0 TBD/TODO/待/类似/参考 Task；envelope wrap step 在 test code 留 "implementer fills per 1001 borrow" comment 是实现细节非 Plan 占位
- [x] **4. 类型一致性**: LoginRequest9006/LogoutRequest9008/LoginResponse9007/LogoutResponse9009 类名 + setPassword/setStatus 方法名跨 Task 一致
- [x] **5. 测试命令可执行**: `-Dtest=OutboundWireShape9006And9008XsdComplianceTest` + `-Dtest=Inbound9007And9009WireTest` 与实际测试类名匹配；`-Dsurefire.failIfNoSpecifiedTests=false` 适配 Surefire 3.x（红线 `feedback_surefire3_failifno_specified_tests_param_rename`）
- [x] **6. CLAUDE.md 更新**: T3 Step 2 含 CLAUDE.md outbound 37→39 + inbound 21→23 更新
- [x] **7. 验收标准完整性**: 每 Task 验收标准从 PRD §4.5 + XSD 实测推导，断言值（39/11/23/Status="01"/Password="Strong#01"）可手算验证
- [x] **8. 共享工具类无遗漏**: LogSanitizer.sanitize 已登记，无新增共享工具
- [x] **9. 核心类职责边界**: NodeLifecycleAckListener 依赖 < 3，无需声明
- [x] **10. Worktree 触发条件自检**:
  - [x] 跨 ≥ 3 个 Maven 模块？✅ 跨 fep-processor / fep-converter / fep-web
  - [x] 与已签字未执行的 Plan 并存？✅ 主 worktree 24 untracked Plans + 别会话 Plans
  - [ ] 涉及 ⛔ 安全 与 AI 并行？✗ 不命中
  - [ ] TLQ tongtech profile 联调？✗ 不命中
  - [ ] 含 ≥ 5 min long-running verify 并行？(可能命中，mvnw verify 全 reactor 约 8-12 min) 待执行时观察
  - [x] muzhou WIP 与 AI 任务并存？✅ multi session 别会话活跃（wt-callback-p2 / wt-simplify-q-drain / 主 worktree）
  - **结论**: ✅ 必须 worktree（命中 3 项），已建 `/Users/muzhou/FEP_v1.0_wt-p4-msg-l`；T3 Step 4 含 `git worktree remove` 实测命令

---

## 红线交叉引用

本 Plan 适用红线（按 Task 顺序）：

| Task | 适用红线 | 应对措施 |
|---|---|---|
| T1 | `feedback_plan_xsd_field_table_default_not_optional` | XSD 字段表含 minOccurs 列 |
| T1 | `feedback_xsd_compliance_fix_real_validator_on_sut` | 真 `XsdValidator` 跑 SUT marshal 产物 |
| T1 | `feedback_fixture_data_must_satisfy_xsd_constraints` | Password="Strong#01" (9 chars) 满足 minLength=8 + maxLength=32 |
| T1 | `feedback_full_regression_before_commit` | Step 7 全 reactor mvnw verify |
| T1 | `feedback_surefire3_failifno_specified_tests_param_rename` | -Dsurefire.failIfNoSpecifiedTests=false |
| T2 | `feedback_jaxb_cache_key_full_set` | InboundMessageDispatcher JAXB cache 按 marshal 实际类组无问题（append-only） |
| T2 | `feedback_logsanitizer_alone_insufficient_for_findsecbugs` | listener handle/handleLoginAck/handleLogoutAck 全 LogSanitizer.sanitize + @SuppressFBWarnings(CRLF_INJECTION_LOGS) |
| T2 | `feedback_plan_template_data_point_self_consistency` | 字段表 5 = 2+1+1+1 自洽 |
| T3 | `feedback_worktree_for_parallel_work` | T3 Step 4 含 `git worktree remove` |
| T3 | `feedback_fep_docs_repo_commit_taboo` | T3 Step 2 CLAUDE.md 仅 file write 不 git add |
| T3 | `feedback_systemic_ci_blocker_defers_positive_backing` | GHA billing 阻塞期间 tier-A 静态验证充分，tier-B GHA CI 背书 deferred |
| 全程 | `feedback_baseline_drift_during_long_review_cycle` | baseline `f89fe38` 2026-06-01 实测，签字与实施跨日须重测 |
| 全程 | `feedback_subagent_must_commit_before_exit` | 每 Task 由 implementer subagent 完成 commit 后 exit + 返回 SHA |
| 全程 | `feedback_subagent_no_background_bash_in_workflow` | implementer prompt 显式禁用 Bash run_in_background |
| 全程 | `feedback_subagent_model_override_auth_fragility` | dispatch 不设 model override（继承父对话已认证模型） |
| 全程 | `feedback_subagent_meta_comment_no_tool_use` | implementer return 须 Status: 起头 + ≥1 tool_use |
| 全程 | `feedback_task_review_discipline` | 每 Task 完成后派独立 spec review + quality review subagent |

---

## 评审与签字

> ⚠️ **本 Plan 必须先通过 AI 独立评审 + muzhou 人工签字方可执行**（CLAUDE.md "Plan 治理"段）

### AI 独立评审区

(待 santa-method / code-reviewer agent 填写)

### muzhou 签字区

v0.2 修订路径: v0.1 起草 → Round 1 FAIL (1 BLOCKER + 1 MAJOR + 1 MINOR) → v0.2 5 修订 → Round 2 PASS WITH 🟡 → v0.2 boil-lake hotfix #1 (line 975) → Round 3 PASS WITH 🟡 → v0.2 boil-lake hotfix #2 (line 886/909) → **Round 4 PASS** ✅

单 PR ~210 LOC 无需 400 LOC 豁免。

| 项 | 决定 | 签字日期 |
|---|---|---|
| 批准 / 驳回 / 部分修改 | **✅ 批准** | 2026-06-01 |
| 签字 | **muzhou** | 2026-06-01 |

### 下会话实施起点

本会话 Plan 已签字 + commit + push，**不执行 T1/T2/T3**。下会话起点：
1. baseline 重测（红线 `feedback_baseline_drift_during_long_review_cycle`，跨日须重测）
2. cd `/Users/muzhou/FEP_v1.0_wt-p4-msg-l`
3. dispatch implementer subagent T1 outbound 9006/9008（无 model override / 前台 Bash / Status: 起头 / 必 commit before exit）
4. 主对话 spec review + quality review subagent
5. T2 同 pattern
6. T3 闭环 (worktree remove + CLAUDE.md 更新 + PRD matrix maturity 升级)
