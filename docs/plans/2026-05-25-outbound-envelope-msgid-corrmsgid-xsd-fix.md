# FEP outbound envelope MsgId/CorrMsgId XSD 校验修复实施计划（P0 生产缺陷）

> **执行方式:** 使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐 Task 执行。步骤使用 `- [ ]` 复选框跟踪。

**目标:** 修复 outbound CFX envelope 装配的 P0 生产缺陷——`CommonHeadComposer.compose()` 写入非数字占位 `MsgId="PLACEHOLDER_T6_INJEC"` + `CorrMsgId=null`，二者均过不了真 `XsdValidator` 校验（`OutboundCfxEnvelopeBuilder.build()` step VII），致生产环境每个 outbound 报文 build 抛 `OUTBOUND_5102_XSD_VALIDATION_FAILURE` 永远发不出去。修法：在 `build()` 内用 `BodyMsgIdGenerator` 生成 20 位数字 MsgId 注入 envelope HEAD/MsgId，CorrMsgId 置 20 零（新请求），统一 envelope HEAD/MsgId = TLQ 属性 msgId = `entity.msg_id`。

**前置依赖:** P5 outbound-send v0.7（已完成，2026-05-06）；root cause 7 P5 DirtiesContext（已 merge origin/main `77bdd1e`，2026-05-25）。

**执行 Worktree:** `/Users/muzhou/FEP_v1.0_wt-outbound-msgid-xsd-fix`（分支 `fix/outbound-envelope-msgid-corrmsgid-xsd`，触发条件第 2 + 第 7 项命中）
> 红线 `feedback_worktree_for_parallel_work` + `feedback_worktree_isolates_fs_not_logic_domain`：起草时 `git worktree list` 实测多会话活跃（`.e2e` smoke + `wt-p4-msg-j` 别会话 3112 inbound + `wt-rc7-dirtiescontext`），第 7 项「多会话即触发」命中 → 须独立 worktree（会话起始即建，文件级无交集不豁免）。
> 起草前 baseline：origin/main = `77bdd1e`（2026-05-25 实测，含 rc7 P5 DirtiesContext merge）。worktree 从 `origin/main` 派生。
> **并排除项**：`wt-p4-msg-j [feat/p4-msg-j-3112-inbound]` 锁定 **inbound** 域（`messageinbound/` 3112 listener/dispatcher + Body 3112）；本 Plan 仅动 **outbound** 域（`outbound/consumer/` builder/composer/sender/runner），两域文件级无交集。**不触 `P5OutboundEndToEndIntegrationTest`**（rc7 已 merge，本 Plan 通过 runner Spring 装配间接覆盖，build() 内部签名变更不影响 P5 source）。

**架构:** Design A'（builder 自洽生成 + 回传 msgId）。`OutboundCfxEnvelopeBuilder` 注入 `BodyMsgIdGenerator`，`build()` 开头生成 20 位数字 msgId → 透传 `compose(entity, headFields, msgId)` → envelope HEAD/MsgId 合法；`build()` 返回类型 `String` → `EnvelopeBuildResult(envelope, msgId)` record，runner 用 `.envelope()` 加签、`.msgId()` 传 `send(signedXml, msgId)`。`OutboundTlqSender` 去掉自带 `BodyMsgIdGenerator`，改收 runner 传入的 msgId（统一三处 msgId）。

**技术栈:** Java 17 / Spring Boot 3.x / Maven / JAXB / JUnit 5 / AssertJ / fep-processor XsdValidator（真 XSD 校验）。

**Currency / 验证约束:** 本机 `mvn verify` 沙盒受限（红线 `feedback_mvn_sandbox_exit144_pattern`，exit 144）→ 全程跳本机长跑 mvn，TDD red/green + 回归由 **GHA CI** 兜底确认；Plan 起草已静态确认根因（决定性）。

**AI 协同模式:**

| 模式 | AI 占比 | 本计划中的应用 |
|:----:|:-------:|--------------|
| B | 70% | outbound 装配链 bug 修复 + 测试（业务逻辑，触及签名管线入参，非国密 impl） |

> **⚠️ 签名管线提示（非 ⛔ 模式 E）:** 本 fix 改变**送签内容**——加签前 envelope 现含真数字 MsgId（原为非法占位）。`OutboundSignAdapter.embedSignatureAsComment` **调用方式与实现均不变**（仍 `(envelope) → signedXml`），不触 `security/impl/` SM2 代码。但「送签内容含真 MsgId」是正确性改善（原占位 envelope 被签是错误状态），需 muzhou 签字时知悉。

---

## 设计背景

### 根因（Phase 1-3 systematic-debugging，决定性静态证据）

`OutboundQueueRunnerImpl.run()`（L114-120）流水：`build()` → `sign()` → `send()` → `recordSent()`。`build()` step VII（`OutboundCfxEnvelopeBuilder` L149-152）用真 `XsdValidator.validate(type, envelope)` 校验，失败抛 `OUTBOUND_5102`。composed envelope HEAD 有**两处违反 XSD**：

1. **MsgId**（`CommonHeadComposer.compose()` L75）：无条件 `head.setMsgId("PLACEHOLDER_T6_INJEC")`（字母+下划线）。
   - `CommonHead.setMsgId` 仅校验 `length==20`，不校验内容（`CommonHead.java` L181-185）。
   - `@XmlElement(name="MsgId", required=true)`（L171）。
   - XSD `MsgId` = `DataType.xsd` L378-384 `Number` + `length 20`；`Number` = L12-18 `xsd:string` + `pattern="[0-9]*"`（纯数字）。
   - → 占位符违反 `[0-9]*` pattern facet → 校验失败。

2. **CorrMsgId**（`compose()` L76）：`head.setCorrMsgId(null)`。
   - `@XmlElement(name="CorrMsgId", required=true)`（L193），但 JAXB marshal 省略 null 字段 → XML 中元素缺失。
   - `Base.xsd` HEAD `CorrMsgId`（L38）无 `minOccurs` → 默认 `minOccurs=1` 必填。
   - → 缺失必填元素 → 校验失败。

真 20 位数字 MsgId 由 `OutboundTlqSender.send()` L65 `msgIdGenerator.generate()` 生成——**在 build() 之后**（sequencing bug），仅用于 TLQ 属性 `TlqMessageAttributes.forBatch(msgId)` + 回传 `outcome.msgId()` → 持久化 `entity.msg_id`，**从未进 envelope HEAD**。

`BodyMsgIdGenerator.generate()`（L72-82）返回 `dt(14位) + String.format("%06d", seq)(6位)` = **20 位全数字**（Javadoc 明示），正是 envelope MsgId 所需。

### 为何长期未暴露
所有 4 个驱动 `build()` 的测试都 `@MockBean XsdValidator` 返回 `ValidationResult.ok()`（`P5OutboundEndToEndIntegrationTest` L88-97 / `Outbound9120AckEnvelopeBuilderTest` L88 / `OutboundCfxEnvelopeBuilderTest` / `OutboundQueueRunnerImplTest`）+ 真 TLQ broker 联调 BLOCKED（无端到端真跑真 XSD）。fep-processor `*XsdValidationTest` 用手工合法 20 位数字 MsgId fixture（`AbstractXsdValidationTest.wrapCfx` Javadoc "MsgId is a single full 20-digit value"）→ 不覆盖 `compose()` 产物。**无任何测试以真 XsdValidator 跑过 compose→build 全链。**

### 修法（Design A'，推荐）

| 类 | 变更 | 依赖数 |
|----|------|:---:|
| `CommonHeadComposer` | `compose(entity, headFields)` → `compose(entity, headFields, msgId)`；`setMsgId(msgId)`（替占位）+ `setCorrMsgId(CORR_MSG_ID_NONE)`（20 零替 null）；删 `MSG_ID_PLACEHOLDER` 常量、加 `CORR_MSG_ID_NONE="00000000000000000000"` | 不变 |
| `OutboundCfxEnvelopeBuilder` | 注入 `BodyMsgIdGenerator`（4→5）；`build()` 开头 `msgId=generator.generate()`，step III 传 msgId，返回 `EnvelopeBuildResult(envelope, msgId)` record（替 `String`） | 4→5 |
| `OutboundQueueRunnerImpl` | `var built = build(...)` → `sign(built.envelope())` → `send(signedXml, built.msgId())`（用 record 字段；deps 不变 6） | 6（不变）|
| `OutboundTlqSender` | `send(signedXml)` → `send(signedXml, msgId)`；删 `BodyMsgIdGenerator` 依赖 + `generate()` 调用，用入参 msgId 构 `forBatch` + `outcome` | 2→1 |

结果：envelope HEAD/MsgId = `forBatch` TLQ 属性 msgId = `entity.msg_id`（三处统一，利后续 HNDEMP CorrMsgId 关联）。

**备选 Design A（runner 生成 + build/send +param）**：runner 6→7 deps，build() 返回保持 String。**已否决**——非硬门拦截（`ClassDesignTest.java:34/59` `MAX_CONSTRUCTOR_PARAMS=7` 判 `>7` 才 fail，7 参恰在上限**通过** ArchUnit；M2 reviewer 校正），而是 **runner 责任蔓延 + 顶满依赖上限无余量** 的设计取舍。采 A'（builder 自洽，msgId 属 envelope 身份由 builder 生成更内聚，builder 4→5 deps 余量充足）。

**CorrMsgId Layer 2 follow-on（不在本 Plan）:** 响应类报文（9120/3113 ack）CorrMsgId 应=被关联报文真 MsgId，需扩 `OutboundHeadFields` 携带（`compose()` Javadoc "spec N3 P4 协调"）。本 Plan 仅 Layer 1（新请求统一 20 零，让 outbound 可发 + 过 XSD）；Layer 2 待响应报文回填路径单列。

**R-NEW-1 解锁（不在本 Plan）:** 本 fix 后 envelope 过真 XSD → `Outbound9120AckEnvelopeBuilderTest` 等的 `@MockBean XsdValidator` 可删（统一 9 outbound consumer 测试 context cache key）。本 Plan **保留** 既有 @MockBean（聚焦 fix，避免 blast radius 扩到 P5 区域测试），R-NEW-1 作独立 follow-on。

### 范围排除
- ❌ 不动 inbound 域（`messageinbound/`）— 别会话 `wt-p4-msg-j` 3112 inbound 锁定。
- ❌ 不动 `P5OutboundEndToEndIntegrationTest`（rc7 已 merge；本 Plan 通过 runner 间接覆盖）。
- ❌ 不删既有 `@MockBean XsdValidator`（R-NEW-1 follow-on）。
- ❌ 不改 `OutboundSignAdapter` / `security/impl/` SM2（仅送签内容含真 MsgId，调用不变）。
- ❌ Layer 2 CorrMsgId 真关联 id（响应报文回填，需 OutboundHeadFields 扩展，单列）。

---

## 文件结构

| 文件路径 | 职责 | 操作 | AI 模式 |
|----------|------|------|:-------:|
| `fep-web/.../outbound/consumer/CommonHeadComposer.java` | compose +msgId 参数 / MsgId 真值 / CorrMsgId 20 零 | 修改 | B |
| `fep-web/.../outbound/consumer/OutboundCfxEnvelopeBuilder.java` | 注入 generator / build 生成 msgId / 返回 record | 修改 | B |
| `fep-web/.../outbound/consumer/OutboundQueueRunnerImpl.java` | 用 record 字段 .envelope()/.msgId() | 修改 | B |
| `fep-web/.../outbound/consumer/OutboundTlqSender.java` | send +msgId 参数 / 去 generator | 修改 | B |
| `fep-web/.../outbound/consumer/OutboundEnvelopeXsdComplianceTest.java` | **新建** 真 XsdValidator 回归守护 | 新建 | B |
| `fep-web/.../outbound/consumer/CommonHeadComposerTest.java` | compose +msgId 调用 + MsgId/CorrMsgId 断言 | 修改 | B |
| `fep-web/.../outbound/consumer/OutboundCfxEnvelopeBuilderTest.java` | builder +generator / build record 断言 | 修改 | B |
| `fep-web/.../outbound/consumer/Outbound9120AckEnvelopeBuilderTest.java` | build record `.envelope()` 断言（保留 @MockBean） | 修改 | B |
| `fep-web/.../outbound/consumer/OutboundTlqSenderTest.java` | send +msgId / 去 generator mock | 修改 | B |
| `fep-web/.../outbound/consumer/OutboundQueueRunnerImplTest.java` | build mock 返 record / send +msgId 交互 | 修改 | B |

**共享工具类清单**: 无新增（复用既有 `BodyMsgIdGenerator`）。
**核心类职责边界**: `OutboundCfxEnvelopeBuilder` 4→5 deps（dispatcher/registry/composer/xsdValidator + **msgIdGenerator**，≤7 上限）；`OutboundQueueRunnerImpl` 维持 6 deps；`OutboundTlqSender` 2→1 deps。
**PRD 追溯**: PRD v1.3 §3.1.3 报文标识号（20 位 14 日期+6 顺序）+ §3.2.2 HEAD 结构 + Base.xsd/DataType.xsd 约束。FR-MSG-OUTBOUND-SEND（修复正确性，非新 FR）。

---

## Task 1: 修复 compose/build/runner/sender + 新增真 XSD 回归守护 `模式 B`

**PRD 依据:** v1.3 §3.1.3 报文标识号格式（20 位全数字）+ §3.2.2 HEAD 结构；XSD `DataType.xsd` MsgId(Number+length20) + `Base.xsd` HEAD CorrMsgId(minOccurs=1)。
**追溯 ID:** FR-MSG-OUTBOUND-SEND（生产正确性修复，对照 `docs/plans/prd-traceability-matrix.md`）。

**验收标准（从 PRD/XSD 推导）:**
1. `compose(entity, headFields, "20251231120000000001")` → CommonHead.msgId = `"20251231120000000001"`（透传），CommonHead.corrMsgId = `"00000000000000000000"`（20 零，非 null）。
2. `build(entity, headFields)`（真 XsdValidator，1101 报文 fixture）→ **不抛 OUTBOUND_5102**；返回 `EnvelopeBuildResult`，`.envelope()` 含 `<MsgId>NNNNNNNNNNNNNNNNNNNN</MsgId>`（20 位数字，正则 `[0-9]{20}`）+ `<CorrMsgId>00000000000000000000</CorrMsgId>`；`.msgId()` 为该 20 位数字且 = envelope 内 MsgId。
3. **回归基准**：修复前同一真-XSD build 抛 `OUTBOUND_5102`（占位 MsgId 违反 `[0-9]*` + CorrMsgId 缺失）；修复后通过。
4. `send(signedXml, "20251231120000000001")` → `TlqMessageAttributes.forBatch("20251231120000000001")`；`outcome.msgId()` = 入参（不再内部 generate）。
5. runner `run()` → `build` 回传 msgId 经 `send` 透传 → `recordSent(queueId, msgId, ...)` 持久化同值（envelope=TLQ=entity.msg_id 统一）。
6. 全 outbound consumer 模块测试编译通过 + GREEN（GHA）。

**Files:**
- Modify: `fep-web/src/main/java/com/puchain/fep/web/outbound/consumer/CommonHeadComposer.java`
- Modify: `fep-web/src/main/java/com/puchain/fep/web/outbound/consumer/OutboundCfxEnvelopeBuilder.java`
- Modify: `fep-web/src/main/java/com/puchain/fep/web/outbound/consumer/OutboundQueueRunnerImpl.java`
- Modify: `fep-web/src/main/java/com/puchain/fep/web/outbound/consumer/OutboundTlqSender.java`
- Create: `fep-web/src/test/java/com/puchain/fep/web/outbound/consumer/OutboundEnvelopeXsdComplianceTest.java`
- Modify: `CommonHeadComposerTest` / `OutboundCfxEnvelopeBuilderTest` / `Outbound9120AckEnvelopeBuilderTest` / `OutboundTlqSenderTest` / `OutboundQueueRunnerImplTest`

- [ ] **Step 1: 新增真 XSD 回归守护测试（TDD red — 修前 FAIL）**

新建 `OutboundEnvelopeXsdComplianceTest.java`：`@SpringBootTest` + 与 `AbstractOutboundWireMatrixTest` 同 `@TestPropertySource`，**无 @MockBean XsdValidator**（用真 bean）。@Autowired `OutboundCfxEnvelopeBuilder`。构造一个 1101（外联机构数据报送，outbound，REGISTERED）`OutboundMessageQueueEntity`（messageType="1101"，messageBodyXml=合法 `DataTransfer1101` body，messageHeadXml=合法 `<OutboundHeadFields>` 占位）+ `OutboundHeadFields(sendOrgCode 14 位 NodeCode, entrustDate yyyyMMdd, transitionNo 8 位)`。

> **M1（reviewer）— body fixture 须满足 1101.xsd 全部必填字段**（红线 `feedback_fixture_data_must_satisfy_xsd_constraints`）：`1101.xsd` `DataTransfer1101` complexType 有 **5 个必填子元素** `MainClass` / `SecondClass` / `Period` / `Type` / `FileDate`（仅 `Parameters` minOccurs=0）。fixture 每个字段值须按 `DataType.xsd` 实测约束填（implementer 落盘前 `xmllint --noout --schema fep-processor/src/main/resources/xsd/1101.xsd <fixture>` 双向验证）。**目的是隔离 HEAD MsgId/CorrMsgId bug**——body 任一必填缺失会让 XSD 因 body（非 HEAD）失败 → false-red/false-green。

断言：
```java
final EnvelopeBuildResult built = builder.build(entity, headFields);
assertThat(built.envelope())
        .as("envelope HEAD/MsgId 必须 20 位全数字（真 XsdValidator 通过的前提）")
        .containsPattern("<MsgId>[0-9]{20}</MsgId>");
assertThat(built.envelope())
        .as("envelope HEAD/CorrMsgId 必须存在（minOccurs=1）且 20 位数字")
        .containsPattern("<CorrMsgId>[0-9]{20}</CorrMsgId>");
assertThat(built.msgId())
        .as("回传 msgId 与 envelope 内 MsgId 一致（统一 envelope=TLQ=entity）")
        .matches("[0-9]{20}");
assertThat(built.envelope()).contains("<MsgId>" + built.msgId() + "</MsgId>");
```
> 修前：`build()` 抛 `FepBusinessException(OUTBOUND_5102)`（占位 MsgId）→ 测试 FAIL（且因 build 返回 String 与新 record 签名编译错——本测试按 Step 2-4 修后签名写）。GHA 确认 red→green。fixture 字段值须按 `DataType.xsd` 实测约束（红线 `feedback_fixture_data_must_satisfy_xsd_constraints`：NodeCode length / Date yyyyMMdd / 1101 body 必填字段）。

- [ ] **Step 2: 修 `CommonHeadComposer`（msgId 参数 + CorrMsgId 20 零）**

删 `static final String MSG_ID_PLACEHOLDER = "PLACEHOLDER_T6_INJEC";`，加：
```java
/** 新请求无关联报文时 CorrMsgId 占位 — 20 位全零满足 MsgId 类型（Number+length 20）。
 *  响应类报文真关联 id 由 Layer 2（OutboundHeadFields 扩展）回填。 */
static final String CORR_MSG_ID_NONE = "00000000000000000000";
```
`compose` 签名 + 体改为：
```java
public CommonHead compose(final OutboundMessageQueueEntity entity,
                          final OutboundHeadFields headFields,
                          final String msgId) {
    final CommonHead head = new CommonHead();
    head.setVersion("1.0");
    head.setSrcNode(headFields.sendOrgCode());
    head.setDesNode(HNDEMP_NODE);
    head.setApp("FEP");
    head.setMsgNo(entity.getMessageType());
    head.setMsgId(msgId);
    head.setCorrMsgId(CORR_MSG_ID_NONE);
    head.setWorkDate(LocalDate.now(BIZ_ZONE).format(WORK_DATE_FMT));
    return head;
}
```
更新类 Javadoc（删占位/T6 注入描述，改为 msgId 由 build 透传 + CorrMsgId 20 零新请求）。

- [ ] **Step 3: 修 `OutboundCfxEnvelopeBuilder`（注入 generator + 生成 + record 返回）**

构造注入加 `BodyMsgIdGenerator msgIdGenerator`（第 5 依赖，`Objects.requireNonNull`）。`build()`：
- 开头（解析 msgNo 后）`final String msgId = msgIdGenerator.generate();`
- step III：`commonHeadComposer.compose(entity, headFields, msgId)`
- 返回类型 `String` → `EnvelopeBuildResult`；`return new EnvelopeBuildResult(xml, msgId);`
- 新增嵌套 record：
```java
/** build 产物：完整 CFX envelope + 其 HEAD/MsgId（runner 透传 send，统一 TLQ 属性 + entity.msg_id）。 */
public record EnvelopeBuildResult(String envelope, String msgId) { }
```
更新类 Javadoc（build 返回 record + msgId 生成时机）。

- [ ] **Step 4: 修 `OutboundQueueRunnerImpl` + `OutboundTlqSender`**

runner `run()` L114-120：
```java
final OutboundCfxEnvelopeBuilder.EnvelopeBuildResult built = envelopeBuilder.build(entity, headFields);
final String signedXml = signAdapter.embedSignatureAsComment(built.envelope());
final OutboundSendOutcome outcome = tlqSender.send(signedXml, built.msgId());
```
（runner deps 不变 6；`built.msgId()` 与 `outcome.msgId()` 同值。）

`OutboundTlqSender`：删 `BodyMsgIdGenerator msgIdGenerator` 字段 + 构造参数（仅留 `TlqProducer producer`）；`send(String signedXml)` → `send(String signedXml, String msgId)`，删 `final String msgId = msgIdGenerator.generate();`，直接用入参 msgId 构 `TlqMessageAttributes.forBatch(msgId)` + `return new OutboundSendOutcome(result.success(), msgId, tlqResult);`。更新类/方法 Javadoc（msgId 由 runner 传入，统一 envelope=TLQ=entity）。

- [ ] **Step 5: 更新受影响既有测试（签名对齐）**

- `CommonHeadComposerTest`：`compose(entity, headFields, "<20位数字>")` 调用；断言 `getMsgId()`=传入值、`getCorrMsgId()`="00000000000000000000"（替原占位断言）。
- `OutboundCfxEnvelopeBuilderTest`：构造 builder 加 `new BodyMsgIdGenerator(Clock.fixed(...))`；`build()` 返回改 `.envelope()` / `.msgId()` 断言。
- `Outbound9120AckEnvelopeBuilderTest`（**保留 @MockBean XsdValidator** — R-NEW-1 follow-on 才删）：仅取 String 的行改 `builder.build(...).envelope()`（N2 reviewer：`assertThatCode(() -> builder.build(...))` lambda 忽略返回值，record 化后仍编译，无需改）。
- **N3 doc-rot 清理**：`CommonHeadComposer` 类 Javadoc（占位/T6 注入描述）+ `CommonHeadComposerTest` 类 Javadoc（L19 占位描述）同步更新为「msgId 由 build 透传 + CorrMsgId 20 零新请求」，避免 doc 腐烂（红线 `feedback_cross_task_obsolete_fixture_assumption_when_set_extended` 同精神）。
- `OutboundTlqSenderTest`（**M3 reviewer — `@InjectMocks` 严格 stub 陷阱**）：该测试用 `@Mock BodyMsgIdGenerator msgIdGen` + `@InjectMocks`。sender 去 generator 依赖后须**删 `@Mock msgIdGen` 字段 + 3 处 `when(msgIdGen.generate())` stub**（否则 `MockitoExtension` strict-stubbing 抛 `UnnecessaryStubbingException`）；3 个测试方法改调 `send(signedXml, "<20位数字>")`；断言 `outcome.msgId()`=入参 + Mockito verify `forBatch`/`TlqMessage` 属性收该值。
- `OutboundQueueRunnerImplTest`：mock `envelopeBuilder.build(...)` 返 `new EnvelopeBuildResult(envelope, msgId)`；`tlqSender.send(signedXml, msgId)` 交互；`recordSent` verify msgId 一致。

- [ ] **Step 6: GHA 验证（本机沙盒跳过，红线 `feedback_mvn_sandbox_exit144_pattern`）**

worktree push 后由 Task 2 merge 触发 GHA reactor verify。本 Step 仅本地静态自检：`grep -rn "PLACEHOLDER_T6_INJEC" fep-web/src` 应 0（占位已除）；`grep -n "EnvelopeBuildResult" OutboundCfxEnvelopeBuilder.java OutboundQueueRunnerImpl.java` 一致。

- [ ] **Step 7: 提交**

```bash
git add fep-web/src/main/java/com/puchain/fep/web/outbound/consumer/CommonHeadComposer.java \
        fep-web/src/main/java/com/puchain/fep/web/outbound/consumer/OutboundCfxEnvelopeBuilder.java \
        fep-web/src/main/java/com/puchain/fep/web/outbound/consumer/OutboundQueueRunnerImpl.java \
        fep-web/src/main/java/com/puchain/fep/web/outbound/consumer/OutboundTlqSender.java \
        fep-web/src/test/java/com/puchain/fep/web/outbound/consumer/
git commit -m "$(cat <<'EOF'
fix(web): inject real 20-digit MsgId into outbound CFX envelope (P0 XSD bug)

CommonHeadComposer wrote MsgId="PLACEHOLDER_T6_INJEC" (non-numeric) +
CorrMsgId=null into the CFX HEAD. OutboundCfxEnvelopeBuilder.build() step VII
validates the envelope with the real XsdValidator: XSD MsgId is Number
(pattern [0-9]*) length 20, and Base.xsd CorrMsgId is minOccurs=1 (required).
So in production every outbound message threw OUTBOUND_5102 and could never be
sent. Masked because all build()-exercising tests @MockBean XsdValidator and
the real TLQ broker integration is blocked (no end-to-end real-XSD run).

Root cause is a sequencing bug: the real 20-digit numeric MsgId was generated
in OutboundTlqSender.send() AFTER build(), only for the TLQ attribute +
entity.msg_id, never reaching the envelope HEAD.

Fix (Design A'): OutboundCfxEnvelopeBuilder injects BodyMsgIdGenerator,
generates the 20-digit MsgId inside build(), threads it into
compose(entity, headFields, msgId), and returns EnvelopeBuildResult(envelope,
msgId). The runner signs built.envelope() and passes built.msgId() to
send(signedXml, msgId), so envelope HEAD/MsgId == TLQ attribute == entity.msg_id
(unified for HNDEMP CorrMsgId correlation). CorrMsgId is set to 20 zeros for new
requests (matches the fep-processor valid-fixture pattern); the real correlation
id for response messages (9120/3113 ack) is a Layer-2 follow-on needing an
OutboundHeadFields extension.

New OutboundEnvelopeXsdComplianceTest runs build() with the REAL XsdValidator
(no mock) and asserts the envelope passes XSD with a 20-digit numeric MsgId +
present CorrMsgId — the regression guard that would have caught this. Existing
@MockBean XSD tests are kept (their removal to unify the Spring context cache
key is a separate R-NEW-1 follow-on).

Plan: docs/plans/2026-05-25-outbound-envelope-msgid-corrmsgid-xsd-fix.md
Worktree: /Users/muzhou/FEP_v1.0_wt-outbound-msgid-xsd-fix

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
git log -1 --format="%H %s"
```

---

## Task 2: 闭环 — GHA reactor verify + merge main + cleanup `模式 A`

**验收标准:**
1. T1 commit rebase onto 最新 origin/main 0 conflict + ff-merge + push。
2. GHA reactor verify GREEN（含 `OutboundEnvelopeXsdComplianceTest` 真 XSD 通过 + 5 受影响测试 GREEN + 全 fep-web 编译通过）。
3. `grep PLACEHOLDER_T6_INJEC` 全仓 0 命中。
4. Worktree + branch cleanup。

**Files:** （无代码改动，纯 cleanup + 远端 verify + 文档）

- [ ] **Step 1: T4 baseline drift 重测**（红线 `feedback_baseline_drift_during_long_review_cycle`）

```bash
cd /Users/muzhou/FEP_v1.0 && git fetch origin
git rev-parse --short origin/main
git log 77bdd1e..origin/main --oneline -- fep-web/src/main/java/com/puchain/fep/web/outbound/consumer/ fep-web/src/test/java/com/puchain/fep/web/outbound/consumer/ | head
git worktree list
```
若 drift touch outbound/consumer/ 文件（别会话）→ pause + report + 重核对；否则继续。

- [ ] **Step 2: Rebase + ff-merge + push**

```bash
cd /Users/muzhou/FEP_v1.0_wt-outbound-msgid-xsd-fix && git fetch origin && git rebase origin/main
cd /Users/muzhou/FEP_v1.0 && git checkout main && git pull --ff-only origin main
git merge --ff-only fix/outbound-envelope-msgid-corrmsgid-xsd
git push origin main && git log origin/main --oneline -3
```

- [ ] **Step 3: GHA reactor verify watch**

```bash
gh auth status >/dev/null 2>&1 || { echo "ERROR: gh auth required"; exit 1; }
RUN_ID=$(gh run list --workflow=ci.yml --branch=main --limit=1 --json databaseId -q '.[0].databaseId')
gh run watch "$RUN_ID" --exit-status
gh run view "$RUN_ID" --log 2>&1 | grep -iE "OutboundEnvelopeXsdComplianceTest|BUILD SUCCESS|BUILD FAILURE" | head
```
期望：`OutboundEnvelopeXsdComplianceTest` GREEN + BUILD SUCCESS。RED → 独立排查（不强行 merge 后修）。

- [ ] **Step 4: Cleanup worktree + branch**

```bash
cd /Users/muzhou/FEP_v1.0
git worktree remove /Users/muzhou/FEP_v1.0_wt-outbound-msgid-xsd-fix
git branch -d fix/outbound-envelope-msgid-corrmsgid-xsd
git push origin --delete fix/outbound-envelope-msgid-corrmsgid-xsd 2>/dev/null || echo "branch local-only, skip remote delete"
git worktree list
```

- [ ] **Step 5: 文档 — defect memory 转已解决 + CLAUDE.md 状态 + PRD 矩阵**

- `~/.claude/projects/-Users-muzhou-FEP/memory/defect_outbound_envelope_msgid_corrmsgid_xsd.md` 状态改"✅ 已修复 commit `<SHA>`"；MEMORY.md 未决缺陷段移至已解决（file write only，红线 `feedback_fep_docs_repo_commit_taboo` 不涉 — 此为 memory 目录非 docs repo）。
- `/Users/muzhou/FEP/CLAUDE.md`（非 git tracked，file write only）"当前项目状态"段记录本 fix 闭环 + SHA + RUN_ID。
- `/Users/muzhou/FEP/docs/plans/prd-traceability-matrix.md`（非 git tracked，file write only）FR-MSG-OUTBOUND-SEND 行追加正确性修复记录。

- [ ] **Step 6: session-end 触发**（红线 `feedback_session_end_auto_invoke_on_task_completion`）— 8 维技术文档 + Daily Report + 三审。

---

## 自检清单

1. **PRD 覆盖度** — ✅ §3.1.3 + §3.2.2 + XSD 约束；FR-MSG-OUTBOUND-SEND 正确性修复（非新 FR，无遗漏）。
2. **安全边界检查** — ✅ 不动 `security/impl/` SM2；`OutboundSignAdapter` 调用与实现不变（仅送签内容含真 MsgId，已在头部签名管线提示明示，muzhou 签字知悉）。无 SM2/SM3/SM4/密钥/脱敏关键词新增 impl。
3. **占位符扫描** — ✅ Plan 无 TBD/TODO/待/类似；`MSG_ID_PLACEHOLDER` 是被删的 bug 占位（非 Plan 占位）。
4. **类型一致性** — ✅ `EnvelopeBuildResult` record 在 builder 定义，runner/测试引用一致；`compose`/`send` 新签名各调用点对齐。
5. **测试命令可执行** — ✅ 本机沙盒跳过（红线 `feedback_mvn_sandbox_exit144_pattern`），GHA 兜底；Step 1 真 XSD 测试 fixture 按 DataType.xsd 实测约束。
6. **CLAUDE.md 更新** — ✅ Task 2 Step 5。
7. **验收标准完整性** — ✅ T1 6 条（含修前 red 回归基准）+ T2 4 条；断言值（20 位数字 / 20 零）可手算。
8. **共享工具类无遗漏** — ✅ 复用 `BodyMsgIdGenerator`，无新共享类。
9. **核心类职责边界** — ✅ builder 4→5 deps（≤7）；runner 6 不变；sender 2→1。
10. **Worktree 触发条件自检**（红线 `feedback_worktree_for_parallel_work` + `feedback_worktree_isolates_fs_not_logic_domain`）：
    - [x] 跨 ≥3 模块? — NO（仅 fep-web）
    - [x] 与已签字未执行 Plan 并存? — **YES**（`wt-p4-msg-j` 3112 inbound 别会话）
    - [x] ⛔ 安全 vs AI 并行? — NO
    - [x] TLQ tongtech? — NO
    - [x] >5min verify 并行? — NO（GHA 远端）
    - [x] muzhou WIP 与 AI 并存? — NO
    - [x] 多并行会话同仓库? — **YES**（`.e2e` + `wt-p4-msg-j` + `wt-rc7-dirtiescontext`）
    - 第 2 + 第 7 项命中 → 独立 worktree（已填）+ T2 Step 4 cleanup（已含）。**4 时点重测**（起草前 / AI 评审前 / T0 实施前 / T4 merge 前，红线 `feedback_worktree_trigger_is_dynamic_recheck_at_execution`）。
11. **测试隔离** — ✅ 不触 `P5OutboundEndToEndIntegrationTest`（rc7 已 merge，间接覆盖）；不触 inbound 域。
12. **ADR / grep 命令可执行性** — ✅ Step 命令标准无 ERE 转义陷阱。

---

## Plan 评审 + 批准签字

### 步骤 1: AI 独立评审
派发独立 AI 评审 agent，输入：本 Plan 全文 + 4 目标类当前源 + DataType.xsd/Base.xsd MsgId/CorrMsgId 约束 + `docs/guides/plan-review-checklist.md`。输出 ✅/❌ + BLOCKER/MAJOR/MINOR/NIT。重点核：(a) 根因双字段证据链；(b) Design A' 签名变更各调用点闭合；(c) P5 不受影响论证；(d) CorrMsgId 20 零 XSD 合法性 + Layer 2 边界；(e) 送签内容变更的安全提示充分性。

### 步骤 2: 人工 Plan Approver 签字（muzhou）
```markdown
---
## Plan Approval
**Plan Approver:** muzhou
**Approval Date:** YYYY-MM-DD
**Decision:** ✅ APPROVED / ❌ REJECTED / 🔄 REVISIONS REQUESTED
**AI Reviewer:** <agent-id> @ <timestamp>
**AI Review Result:** <PASS / NOT CLEARED>
**Approval Notes:** <muzhou 决策依据 — 含 Design A'/A 取舍 + 送签内容变更知悉 + 执行时机（本会话 subagent-driven / 移交 fresh session）>
```

### 步骤 3: 执行选择
签字后：Subagent 驱动（T1 implementer + spec + quality reviewer / T2 主对话直跑）或内联执行。鉴于本机 mvn 沙盒受限（GHA-only TDD 验证）+ P0 安全相邻关键路径，muzhou 可于签字时定执行时机（本会话 / fresh focused session）。

⚠️ **禁止: 未签字直接执行。**

---

## Revision History

| 版本 | 日期 | 修订内容 | AI Reviewer 结果 |
|------|------|----------|------------------|
| v0.1 | 2026-05-25 | 初稿（systematic-debugging Phase 1-3 确认根因 + Design A' 修法 + 真 XSD 回归守护）| general-purpose @ 2026-05-25 → ✅ **PASS WITH MINOR**（0 BLOCKER / 0 MAJOR / 3 MINOR / 3 NIT；13 根因+设计 claim 全独立 grep 验证：双字段根因真 + 4 类签名匹配 + 无漏 caller（CallbackEnqueueService 用的是另一类 CallbackEnvelopeBuilder）+ P5 不受影响 + 1101 fixture 适配 + 安全边界完整）|
| v0.2 | 2026-05-25 | Inline 应用 M1（枚举 1101 5 必填 body 字段）+ M2（Design A 否决理由校正：7 参在 ArchUnit 上限内通过，理由是责任蔓延非硬门）+ M3（OutboundTlqSenderTest @InjectMocks strict-stubbing 删 @Mock+3 stub）+ N2（9120 test L109 lambda 无需改）+ N3（CommonHeadComposer/Test Javadoc doc-rot 清理）| 无需重审（MINOR/NIT 级精确化，无逻辑/设计变更）|

---

## Plan Approval

**Plan Approver:** muzhou
**Approval Date:** 2026-05-25
**Decision:** ✅ APPROVED
**AI Reviewer:** general-purpose @ 2026-05-25
**AI Review Result:** ✅ PASS WITH MINOR（0 BLOCKER / 0 MAJOR / 3 MINOR / 3 NIT — v0.2 inline 全应用）
**Approval Notes:** AskUserQuestion 决策门批准（2026-05-25，"批准 + 本会话 subagent 执行"）。P0 生产正确性修复（outbound envelope HEAD/MsgId 非数字占位 + CorrMsgId null 过不了真 XSD → 生产 outbound 全部 build 抛 OUTBOUND_5102）。采 Design A'（builder 自洽生成 20 位数字 MsgId + record 回传，runner 透传统一 envelope=TLQ=entity.msg_id；CorrMsgId 新请求 20 零，Layer 2 响应报文真关联 id 单列）。送签内容含真 MsgId 为正确性改善（OutboundSignAdapter 调用/实现不变，不触 SM2 impl）已知悉。执行：本会话 subagent-driven（T1 implementer + spec + quality reviewer / T2 主对话 merge + GHA verify + cleanup），TDD red/green 由 GHA 兜底（本机 mvn 沙盒受限 exit 144）。worktree `wt-outbound-msgid-xsd-fix` 触发条件第 2+7 项，rebase onto `77bdd1e`，不触 P5/inbound 域。
