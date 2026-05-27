# FEP R-NEW-1 follow-on：5 测试删除 @MockBean XsdValidator + fixture 真 XSD 合规化 (v0.3)

> **执行方式:** 使用 superpowers:subagent-driven-development（推荐）逐 Task 执行。步骤使用 `- [ ]` 复选框跟踪。

**目标:** 删除 fep-web 模块 5 个 `@MockBean XsdValidator` + `@SpringBootTest` 测试类中的 mock 注入与 mock setup，将 fixture body XML 改为满足报文各自 XSD（DataType.xsd / Base.xsd / `<msgNo>.xsd`）实测约束，统一 SpringBootTest ApplicationContext cache key（消除"XSD-mock 守护盲区"，与 5-25 P0 outbound envelope XSD 修复闭环）。

**前置依赖:**
- 2026-05-25 outbound envelope MsgId/CorrMsgId/App XSD P0 修复（origin/main `f39bf86`）已 ship
- 2026-05-25 `OutboundEnvelopeXsdComplianceTest`（真 XsdValidator 范本）已落盘
- baseline HEAD = `df15613`（origin/main，pitest-maven 1.25.0 升级别会话已 merge；本 worktree HEAD 仍 `457a5e4`，Task 6 Step 1 rebase onto df15613 必跑）
- v0.1 → v0.2 修订：BLOCKER 1 全文 §3.1.3 → §3.2.2（PRD §3.1.3 实为 TLQ 消息属性，MsgId 报文标识号 在 §3.2.2）+ BLOCKER 2 baseline 457a5e4 → df15613 + MAJOR-1 加 wt-simplify-q-drain 并排除 + MAJOR-2 dispatcher → pipeline 路径 + MAJOR-3 fixture 字段集起草阶段 grep 落定

**执行 Worktree:** `/Users/muzhou/FEP_v1.0_wt-r-new-1-real-xsd`（分支 `refactor/r-new-1-real-xsd-validator`，触发条件第 7 项 `feedback_worktree_isolates_fs_not_logic_domain` 多会话活跃即触发：当前活跃 `wt-callback-p2` PR #27 + `wt-pitest-maven-125-upgrade` + `.e2e`，本会话亦活跃）
> 红线 `feedback_worktree_for_parallel_work` 触发条件: ① 跨 ≥3 模块 refactor ② 与已签字 Plan 并存 ③ ⛔ 安全 vs AI 并行 ④ TLQ tongtech 联调 ⑤ >5min long-running verify 并行 ⑥ muzhou WIP 与 AI 并存
> 命中: 第 7 项（红线 `feedback_worktree_isolates_fs_not_logic_domain`）+ 第 5 项（fep-web 全模块 verify ≥5min 与别会话 PR #27 + pitest 升级 + .e2e 并行）。

**架构:** 单模块 fep-web 测试基础设施 refactor（src/main/ 生产代码不动）。删除 5 测试的 mock XsdValidator → 让 Spring context 注入真实 `XsdValidator` bean → 通过 `fep-processor/.../pipeline/BatchMessageProcessorService:137`（inbound 路径）+ `fep-web/.../outbound/consumer/OutboundCfxEnvelopeBuilder:156`（outbound 路径）触发实际 XSD 校验 → fixture body XML 必须按 DataType.xsd / Base.xsd / 各 `<msgNo>.xsd` 实测约束填充（Token/Number/length/pattern/minOccurs 等）。

**技术栈:** Java 17 / Spring Boot 3.x / Maven / JUnit 5 / Spring Boot Test / AssertJ / Awaitility / PostgreSQL (prod) + H2 (dev/test scope) / Mockito (保留非 XsdValidator mock)

**AI 协同模式:**

| 模式 | AI 占比 | 本计划中的应用 |
|:----:|:-------:|--------------|
| A | 90% | 全部 Task（纯测试基础设施 refactor / 无业务逻辑 / 无新生产代码） |
| B | 70% | 不适用 |
| C | 60% | 不适用 |
| D | 50% | 不适用 |
| E | 0%  | ⛔ 不涉及（不动 security/impl/） |

---

## 设计背景

### R-NEW-1 来源
2026-05-25 outbound envelope MsgId/CorrMsgId/App XSD P0 修复（`f39bf86`）发现：4 个 build()-测试均 `@MockBean XsdValidator` → 所有 build() 调用路径长期没有真实 XSD 守护，导致：
1. `CommonHeadComposer.compose` 产 MsgId="PLACEHOLDER_T6_INJEC"（非数字）违反 `Number length=20`
2. `CorrMsgId=null` 违反 Base.xsd HEAD `CorrMsgId minOccurs=1`
3. `App="FEP"` 3 字符违反 DataType.xsd `App minLength=4` + PRD §3.2.2 固定 HNDEMP

P0 修复 commit `dfd6e37`+`f39bf86` 已 ship + 新增 `OutboundEnvelopeXsdComplianceTest`（真 XsdValidator 1101 envelope 范本）。next-session-prompt 明确：本 R-NEW-1 follow-on 删除全部 `@MockBean XsdValidator`，让所有相关 `@SpringBootTest` 走真校验 + 统一 cache key（与已用真校验的 sibling 测试合并 context）。

### 5-25 P0 教训复用（红线 `feedback_xsd_compliance_fix_real_validator_on_sut`）
P0 修复时 reviewer 静态逐字段比对仅查出 2/3 违规（MsgId + CorrMsgId 漏 App），quality reviewer **实跑** `OutboundEnvelopeXsdComplianceTest` 才揪出第 3 字段。本 Plan 起草阶段静态 grep 已预测 2 处违规（Inbound2101 SecondClass 下划线 + CorrMsgId 空），但执行阶段必须用**真 validator 跑 SUT 实际产物**（不仅静态比对），并对 reviewer 实跑揪出的额外违规字段逐个回溯 facet+spec 修正循环到 GREEN（不打地鼠）。

### 已知静态预测违规（执行阶段须真 validator 验证完整性）

| 测试 | 预测违规 | XSD 依据 |
|------|---------|---------|
| Outbound9120AckEnvelopeBuilderTest | （无，P0 fix 后 MsgId 20 数字 + CorrMsgId 20 零 + App=HNDEMP + body OriMsgNo=2101 4 数字） | 9120.xsd MsgReturn9120 |
| Inbound2101WireTest | (a) SecondClass="LOAN_REPORT" 含 `_` 违反 Token pattern `[A-Za-z0-9]*` (b) CorrMsgId="" 违反 MsgId length=20 | DataType.xsd Token + Base.xsd HEAD |
| Inbound3112WireTest | (a) `<MSG>` 缺 `<BatchHead3112 type="RequestHead">` 段（违反 3112.xsd MSG sequence required first element） (b) CorrMsgId="" 同上 | 3112.xsd MSG + Base.xsd |
| InboundAck9120BatchWireTest | (a) `wrapCfx` 缺 BatchHead/RealHead 段（3009 特殊用 `<RealHead3009 type="RequestHead">`，其他 3 用 `<BatchHead<code>>`；3103/3113 是 ResponseHead 需 Result 字段） (b) CorrMsgId="" 同上 | 3105/3009/3103/3113.xsd MSG + Base.xsd |
| P5OutboundEndToEndIntegrationTest | 8 messages 全部 body 自闭合空标签（`<rzReturnInfo3009/>` 等）违反各 XSD 必填 sequence；envelope HEAD 由 builder 注入应过 | 3009/3101/3102/3105/3107/3109/3112/3116.xsd |

> **真 validator 实跑发现的额外违规** 必须按 facet+spec 逐个修正，不打地鼠。

---

## 范围与并排除项

### 范围内（5 测试 + 1 SQL fixture）
1. `fep-web/src/test/java/com/puchain/fep/web/outbound/consumer/Outbound9120AckEnvelopeBuilderTest.java`
2. `fep-web/src/test/java/com/puchain/fep/web/outbound/consumer/P5OutboundEndToEndIntegrationTest.java`
3. `fep-web/src/test/resources/sql/p5/outbound_queue_8_messages.sql`（P5 测试 fixture）
4. `fep-web/src/test/java/com/puchain/fep/web/messageinbound/listener/Inbound2101WireTest.java`
5. `fep-web/src/test/java/com/puchain/fep/web/messageinbound/listener/Inbound3112WireTest.java`
6. `fep-web/src/test/java/com/puchain/fep/web/messageinbound/listener/InboundAck9120BatchWireTest.java`

### 并排除项（不动）
- `src/main/` 生产代码（OutboundCfxEnvelopeBuilder / BatchMessageProcessorService / XsdValidator / XsdSchemaRegistry 等均不动）
- `fep-processor/src/test/java/.../BatchMessageProcessorServiceTest.java`（pure Mockito @Mock 单测，不污染 Spring context cache）
- `fep-web/src/test/java/.../OutboundCfxEnvelopeBuilderTest.java`（pure Mockito `mock()` 单测，非 SpringBootTest，scope 决策跳过）
- `fep-web/src/test/java/.../OutboundQueueRunnerImplTest.java`（实测不引用 XsdValidator，衔接 prompt 列错）
- PR #27 callback-phase2 任何文件（别会话 ownership / wt-callback-p2 @ `b26f4a8`）
- `wt-pitest-maven-125-upgrade` 任何 pom 变更（已 merge origin/main `df15613`，本 Plan 不动）
- `wt-simplify-q-drain` 任何文件（chore/simplify-q-drain-p4-msg-i 别会话 ownership @ `457a5e4`）
- ⛔ `security/impl/*` 任何代码（红线 + ③ 安全专家所有）

---

## PRD 依据 & 追溯

**PRD v1.3 章节** (守护既有 FR，无新增 FR-ID):
- §3.2 报文结构（HEAD + MSG + body 三段）
- §3.2.2 报文头（HEAD）数据结构（MsgId/CorrMsgId Number length=20 + App 固定 HNDEMP）
- §4.6 各报文方向（2101/3009/3101/3102/3105/3107/3109/3112/3113/3116/3103/9120）
- §4.7 模式 5/6（受理侧 9120 强制 ack）

**追溯 ID**: 本 Plan 是测试基础设施 refactor，**不引入新 FR-ID**；守护既有 FR-MSG-2101 / FR-MSG-3009 / FR-MSG-3101 / FR-MSG-3102 / FR-MSG-3103 / FR-MSG-3105 / FR-MSG-3107 / FR-MSG-3109 / FR-MSG-3112 / FR-MSG-3113 / FR-MSG-3116 / FR-MSG-9120 系列 XSD 合规度。

**矩阵更新**: session-end Phase 6 加注"R-NEW-1 (2026-05-26) XSD 守护层闭环"小注（红线 `feedback_session_end_prd_matrix_auto_update`）。

---

## 文件结构

| 文件路径 | 职责 | 操作 | AI 模式 |
|----------|------|------|:-------:|
| `fep-web/src/test/java/com/puchain/fep/web/outbound/consumer/Outbound9120AckEnvelopeBuilderTest.java` | 9120 ack envelope build() IT | Modify | A |
| `fep-web/src/test/java/com/puchain/fep/web/outbound/consumer/P5OutboundEndToEndIntegrationTest.java` | P5 8 报文 outbound E2E IT | Modify | A |
| `fep-web/src/test/resources/sql/p5/outbound_queue_8_messages.sql` | P5 测试 SQL fixture | Modify | A |
| `fep-web/src/test/java/com/puchain/fep/web/messageinbound/listener/Inbound2101WireTest.java` | 2101 inbound wire IT | Modify | A |
| `fep-web/src/test/java/com/puchain/fep/web/messageinbound/listener/Inbound3112WireTest.java` | 3112 inbound wire IT | Modify | A |
| `fep-web/src/test/java/com/puchain/fep/web/messageinbound/listener/InboundAck9120BatchWireTest.java` | 4 报文 inbound wire IT (parameterized) | Modify | A |

### 共享工具类清单
无新增。本 Plan 复用既有：
- `OutboundEnvelopeXsdComplianceTest`（2026-05-25 P0 范本）— 实测 fixture 满足 XSD 的取值参考
- `LogSanitizer`（XsdValidator 内部用）— 不动
- `AckIdempotencyKeys`（InboundAck9120Batch 复用，不动）

### 核心类职责边界
本 Plan **不动** `XsdValidator` / `XsdSchemaRegistry` / `OutboundCfxEnvelopeBuilder` / `BatchMessageProcessorService` 任何生产代码，无职责边界声明需更新。

---

## 验收标准（从 PRD + 5-25 P0 经验推导，不从代码推导）

### Plan 整体（PRD §3.2 + §3.2.2 + §4.6 守护）
1. 5 个测试 `@MockBean XsdValidator` 全部删除 + 相关 `xsdValidator` 字段声明删除 + 相关 `when(xsdValidator.validate(any(), any())).thenReturn(ValidationResult.ok())` mock setup 全部删除
2. 5 个测试 `@SpringBootTest` 启动加载真实 `XsdValidator` bean（验证：删除 mock 后测试运行能触达 `XsdValidator.validate` 真实调用）
3. 5 个测试在真 XsdValidator 下全部 GREEN（fixture body 各满足报文 XSD）
4. Spring context cache key 统一（验证：5 测试 + 既有 OutboundEnvelopeXsdComplianceTest + 各 `Outbound<NNNN>WireTest` 同 cache，全 fep-web Surefire run 内 context 复用计数减少）
5. 0 个 silent-dead 残留 mock setup（红线 `feedback_obsolete_negative_test_cleanup`，grep `XsdValidator` 在 5 改造测试文件中 0 命中）
6. 0 个腐烂 Javadoc 提及"@MockBean XsdValidator"（红线 `feedback_cross_task_obsolete_fixture_assumption_when_set_extended`，5 测试类 Javadoc 删除/重写相关段）
7. fep-web 全模块 `./mvnw test -pl fep-web -am` GREEN（包括 ArchUnit 不变量保持）
8. 0 个新引入的 SpotBugs / find-sec-bugs finding（红线 `feedback_security_doc_must_distinguish_spotbugs_layers`）

### Task 级验收
各 Task 内详。

---

## Tasks

### Task 1: Outbound9120AckEnvelopeBuilderTest 真 XSD 化 `模式 A`

**PRD 依据:** v1.3 §3.2 报文结构 + §3.2.2 HEAD MsgId + §4.7 模式 6 + 9120.xsd（MsgReturn9120 OriMsgNo MsgNo 4 数字 + Debug optional Text 0-1000）
**追溯 ID:** 守护 FR-MSG-9120 + FR-MSG-2101 模式 6 ack 装配

**验收标准（从 9120.xsd 推导）:**
1. 删除 `@MockBean private XsdValidator xsdValidator;`（字段 + 相关 import `org.springframework.boot.test.mock.mockito.MockBean` 如不再使用）+ 删除 `when(xsdValidator.validate(any(), any())).thenReturn(ValidationResult.ok());` mock setup（行 102；删除 `import com.puchain.fep.processor.validation.ValidationResult;` + `import static org.mockito.ArgumentMatchers.any;` + `import static org.mockito.Mockito.when;` 如不再使用）
2. 删除 `import com.puchain.fep.processor.validation.XsdValidator;`（如不再使用）
3. fixture `BODY_XML_9120_ACK` 字段值满足 9120.xsd MsgReturn9120：OriMsgNo MsgNo length=4 numeric（当前 "2101" 符合，保留）+ Debug Text maxLength=1000（当前 "mode-6-ack" 符合，保留）
4. test method `build9120AckEnvelope_shouldSucceedAfterT1T2Registration` 真 XsdValidator 下 GREEN（envelope HEAD 由 5-25 P0 fix 的 `BodyMsgIdGenerator` 注入 20 数字 MsgId + CommonHeadComposer 注入 CorrMsgId 20 零 + App=HNDEMP；BatchHead9120 ResponseHead 含 Result="00000" placeholder）
5. Javadoc 删除 "<b>@MockBean {@link XsdValidator}</b>" 整段 (行 50-58)，改写为说明"真 XsdValidator（自 R-NEW-1 起，与 OutboundEnvelopeXsdComplianceTest 同 cache key）"
6. 单测 `./mvnw test -pl fep-web -am -Dtest=Outbound9120AckEnvelopeBuilderTest -Dsurefire.failIfNoSpecifiedTests=false` GREEN

**Files:**
- Modify: `fep-web/src/test/java/com/puchain/fep/web/outbound/consumer/Outbound9120AckEnvelopeBuilderTest.java`

- [ ] **Step 1: Subagent 重读当前文件 + 执行 TDD red 验证（删 mock 看红）**

```bash
cd /Users/muzhou/FEP_v1.0_wt-r-new-1-real-xsd && \
./mvnw test -pl fep-web -am \
  -Dtest=Outbound9120AckEnvelopeBuilderTest \
  -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | tail -30
```
期望（当前 GREEN，因 mock 还在）: `BUILD SUCCESS` 1 test passed
> 这是基线，下一步删 mock 看是否 still GREEN（如果真 GREEN 说明 P0 fix 已闭合；如果 RED 则按违规修复）

- [ ] **Step 2: 删除 @MockBean XsdValidator + mock setup + 相关 imports**

将以下行从测试类删除：
- 行 5: `import static org.mockito.ArgumentMatchers.any;`（若 mock setup 全删除则同删）
- 行 6: `import static org.mockito.Mockito.when;`（同上）
- 行 9: `import com.puchain.fep.processor.validation.ValidationResult;`
- 行 10: `import com.puchain.fep.processor.validation.XsdValidator;`
- 行 16: `import org.springframework.boot.test.mock.mockito.MockBean;`
- 行 95-96: `@MockBean\n    private XsdValidator xsdValidator;`
- 行 101-102: 整个 `when(xsdValidator.validate(any(), any())).thenReturn(ValidationResult.ok());` 行 + 注释行

保留：`@SpringBootTest` + `@TestPropertySource` + `@Autowired private OutboundCfxEnvelopeBuilder builder;` + `BODY_XML_9120_ACK` 常量 + fixture `givenAck9120Entity()`。

- [ ] **Step 3: Javadoc 重写 — 删除 @MockBean 段 + 加 R-NEW-1 标注**

将原 50-58 行 Javadoc `<p><b>@MockBean {@link XsdValidator}</b>` 整段（直至下一 `</p>`）替换为：

```
 * <p><b>真 XsdValidator（R-NEW-1 起）</b>：自 2026-05-26 R-NEW-1 起，本测试不再
 * {@code @MockBean XsdValidator}，由 Spring context 注入真实 bean。fixture
 * {@code BODY_XML_9120_ACK} 字段值满足 9120.xsd MsgReturn9120 sequence：
 * OriMsgNo MsgNo {@code length=4} numeric + Debug Text optional。envelope HEAD 由
 * 5-25 P0 fix 的 {@code BodyMsgIdGenerator} 注入 20 数字 MsgId + {@code CommonHeadComposer}
 * 注入 CorrMsgId 20 零 + App=HNDEMP；BatchHead9120 ResponseHead 含 Result="00000"
 * placeholder（{@code OutboundCfxEnvelopeBuilder} 装配段）。与 sibling
 * {@link OutboundEnvelopeXsdComplianceTest} 同 cache key（统一 ApplicationContext 复用）。</p>
```

- [ ] **Step 4: 运行单测确认真 XSD 下 GREEN**

```bash
cd /Users/muzhou/FEP_v1.0_wt-r-new-1-real-xsd && \
./mvnw test -pl fep-web -am \
  -Dtest=Outbound9120AckEnvelopeBuilderTest \
  -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | tail -30
```
期望: `BUILD SUCCESS` 1 test passed

> **如 RED**：grep `XsdValidator.validate` log 看具体违规字段 → 按 9120.xsd 实测约束修 fixture（不打地鼠，按 facet+spec 逐个回溯）。

- [ ] **Step 5: 全 fep-web 测试快速回归（确保未破其他）**

```bash
cd /Users/muzhou/FEP_v1.0_wt-r-new-1-real-xsd && \
./mvnw test -pl fep-web -am 2>&1 | tail -20
```
期望: `BUILD SUCCESS` 全部 GREEN（如 perf flake `AsyncPipelineIntegrationTest#performanceBaseline_*` 失败属 known issue 不阻塞，参 CLAUDE.md "已知约束"）

- [ ] **Step 6: 提交**

```bash
cd /Users/muzhou/FEP_v1.0_wt-r-new-1-real-xsd && \
git add fep-web/src/test/java/com/puchain/fep/web/outbound/consumer/Outbound9120AckEnvelopeBuilderTest.java && \
git commit -m "$(cat <<'EOF'
refactor(web): Outbound9120AckEnvelopeBuilderTest 删 @MockBean XsdValidator (R-NEW-1)

删除 @MockBean XsdValidator + mock setup + 相关 imports (ValidationResult /
XsdValidator / MockBean / ArgumentMatchers.any / Mockito.when)。fixture
BODY_XML_9120_ACK 已满足 9120.xsd MsgReturn9120 (OriMsgNo MsgNo length=4 numeric +
Debug Text optional)，envelope HEAD 由 5-25 P0 fix BodyMsgIdGenerator 注入 20
数字 MsgId / CommonHeadComposer 注入 CorrMsgId 20 零 / App=HNDEMP。Javadoc
@MockBean 段重写为 R-NEW-1 真 XsdValidator 标注，统一 cache key 与 sibling
OutboundEnvelopeXsdComplianceTest 复用 ApplicationContext。

PRD: v1.3 §3.2 + §3.2.2 + §4.7 模式 6
守护 FR: FR-MSG-9120 + FR-MSG-2101 模式 6 ack 装配

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

### Task 2: Inbound2101WireTest 真 XSD 化 `模式 A`

**PRD 依据:** v1.3 §3.2 报文结构 + §4.6 2101 数据推送 + §4.7 模式 6 + 2101.xsd（CFX → HEAD + MSG → BatchHead2101 RequestHead + DataTransfer2101 含 MainClass Token 2-16 / SecondClass Token 2-16 / Period Number 1-2 / Type Number 1-2 / FileDate Date）+ Base.xsd HEAD（CorrMsgId 类型 MsgId length=20 minOccurs=1）
**追溯 ID:** 守护 FR-MSG-2101

**验收标准（从 2101.xsd + Base.xsd 推导）:**
1. 删除 `@MockBean private XsdValidator xsdValidator;` (行 114-115) + 相关 imports + Javadoc @MockBean 段（行 66-70）
2. fixture `buildCfxEnvelope2101` 修正：
   - SecondClass 当前 "LOAN_REPORT" 含 `_` 违反 Token pattern `[A-Za-z0-9]*` → 改 "LOAN001"（Token 7 字符 in 2-16）
   - CorrMsgId 当前 `<CorrMsgId></CorrMsgId>` 空违反 MsgId length=20 → 改 `<CorrMsgId>00000000000000000000</CorrMsgId>`（20 零）
   - HEAD 当前缺 `<MSG>` 段前必填的 `<BatchHead2101>` ⚠️ 实测：CFX → MSG → 先 BatchHead2101 再 DataTransfer2101 — 当前 fixture 直接进 DataTransfer2101 ❌ 违反 sequence ordering → 必须在 MSG 段前补 `<BatchHead2101><SendOrgCode>BANK0010000001</SendOrgCode><EntrustDate>20260511</EntrustDate><TransitionNo>{transitionNo}</TransitionNo></BatchHead2101>`（RequestHead: SendOrgCode OrgCode length=14 + EntrustDate Date + TransitionNo Number length=8）
3. test method `inbound2101_shouldPersistAndEnqueue9120` 真 XsdValidator 下 GREEN
4. 9120 ack outbound 由本 IT @Scheduled outbound queue runner 不触发（`poll-interval-ms=99999`），故 outbound XSD 不阻塞
5. Javadoc 修正"<b>@MockBean XsdValidator</b>" 段（行 66-70）改写为 R-NEW-1 真 XsdValidator 标注

**Files:**
- Modify: `fep-web/src/test/java/com/puchain/fep/web/messageinbound/listener/Inbound2101WireTest.java`

- [ ] **Step 1: 基线确认（当前 mock 下 GREEN）**

```bash
cd /Users/muzhou/FEP_v1.0_wt-r-new-1-real-xsd && \
./mvnw test -pl fep-web -am \
  -Dtest=Inbound2101WireTest \
  -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | tail -30
```
期望: `BUILD SUCCESS` 1 test passed（基线）

- [ ] **Step 2: 删除 @MockBean + 相关 imports + Javadoc 段**

删除：
- 行 4-5: `import com.puchain.fep.processor.validation.ValidationResult;` + `import com.puchain.fep.processor.validation.XsdValidator;`
- 行 19: `import org.springframework.boot.test.mock.mockito.MockBean;`
- 行 31-32: `import static org.mockito.ArgumentMatchers.any;` + `import static org.mockito.Mockito.when;`
- 行 66-70: Javadoc `<p><b>@MockBean XsdValidator</b>...` 整段
- 行 109-115: `/** fixture 使用最小 DataTransfer2101 body...` Javadoc 块 + `@MockBean\n    private XsdValidator xsdValidator;`
- 行 120: `when(xsdValidator.validate(any(), any())).thenReturn(ValidationResult.ok());`

- [ ] **Step 3: 修复 fixture `buildCfxEnvelope2101` 违规字段**

应用以下 3 处修正：

(a) 行 192 (SecondClass)：`<SecondClass>LOAN_REPORT</SecondClass>` → `<SecondClass>LOAN001</SecondClass>`

(b) 行 192-193 之间 — 即 CorrMsgId 行（约 192）：`<CorrMsgId></CorrMsgId>` → `<CorrMsgId>00000000000000000000</CorrMsgId>`

(c) MSG 段补 BatchHead2101 — 行 195-196 之间 `<MSG>` 后立即插入：
```
+ "<BatchHead2101>"
+ "<SendOrgCode>BANK0010000001</SendOrgCode>"
+ "<EntrustDate>20260511</EntrustDate>"
+ "<TransitionNo>" + cfxMsgId.substring(cfxMsgId.length() - 8) + "</TransitionNo>"
+ "</BatchHead2101>"
```

> TransitionNo 末 8 位与 `transitionNo` 变量同源（CFX HEAD `<MsgId>` 末 8 位），保持 MSG 段 BatchHead 与 dispatcher.extractSerialNo fallback 语义一致。

- [ ] **Step 4: Javadoc 重写**

将 Javadoc 行 66-70 段替换为：

```
 * <p><b>真 XsdValidator（R-NEW-1 起）</b>：自 2026-05-26 R-NEW-1 起，本测试不再
 * {@code @MockBean XsdValidator}，由 Spring context 注入真实 bean。fixture
 * {@code buildCfxEnvelope2101} 字段值满足 2101.xsd + Base.xsd 完整约束：
 * HEAD CorrMsgId 20 零（MsgId length=20）、BatchHead2101 RequestHead 含
 * SendOrgCode/EntrustDate/TransitionNo、DataTransfer2101 含 MainClass Token
 * "FINANCE"（2-16）、SecondClass Token "LOAN001"（2-16 / 无下划线）、Period/Type
 * Number 1-2、FileDate Date yyyyMMdd。与 sibling
 * {@link Inbound3112WireTest}/{@link InboundAck9120BatchWireTest} 同 cache key
 * （R-NEW-1 统一 cache）。</p>
```

- [ ] **Step 5: 运行测试确认真 XSD 下 GREEN**

```bash
cd /Users/muzhou/FEP_v1.0_wt-r-new-1-real-xsd && \
./mvnw test -pl fep-web -am \
  -Dtest=Inbound2101WireTest \
  -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | tail -30
```
期望: `BUILD SUCCESS` 1 test passed

> **如 RED**：日志会显示 XSD validator 第 N 行第 M 列的具体违规元素（如 `cvc-pattern-valid: Value 'XXX' is not facet-valid with respect to pattern...`），按 2101.xsd 实测约束补齐，循环修复到 GREEN。

- [ ] **Step 6: 提交**

```bash
cd /Users/muzhou/FEP_v1.0_wt-r-new-1-real-xsd && \
git add fep-web/src/test/java/com/puchain/fep/web/messageinbound/listener/Inbound2101WireTest.java && \
git commit -m "$(cat <<'EOF'
refactor(web): Inbound2101WireTest 删 @MockBean XsdValidator + fixture 真 XSD 合规 (R-NEW-1)

删除 @MockBean XsdValidator + mock setup + 相关 imports。fixture
buildCfxEnvelope2101 3 处违规修正：(a) SecondClass "LOAN_REPORT" 含 _ 违反 Token
pattern → "LOAN001" (b) CorrMsgId 空违反 MsgId length=20 → 20 零 (c) MSG 段补
BatchHead2101 RequestHead (SendOrgCode/EntrustDate/TransitionNo) 满足 2101.xsd
MSG sequence ordering。Javadoc R-NEW-1 真 XsdValidator 标注。

PRD: v1.3 §3.2 + §4.6 2101 + §4.7 模式 6
守护 FR: FR-MSG-2101

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

### Task 3: Inbound3112WireTest 真 XSD 化 `模式 A`

**PRD 依据:** v1.3 §3.2 + §4.6 3112 核心企业授信查询 + §4.7 模式 5 + 3112.xsd（CFX → HEAD + MSG → BatchHead3112 RequestHead + hxqyCreditAmt3112 含 SerialNo SerialNo length=30 + SendNodeCode/DesNodeCode NodeCode length=14 + QueryDate Date + hxqyInfoNum + hxqyInfo nested）
**追溯 ID:** 守护 FR-MSG-3112

**验收标准（从 3112.xsd + Base.xsd 起草阶段 grep 落定）:**

> **起草阶段实测 hxqyCreditAmt3112 完整 sequence**（grep 3112.xsd）：SerialNo + SendNodeCode + DesNodeCode + QueryDate + hxqyInfoNum(Integer) + **hxqyInfo (maxOccurs=200，含 hxqyName qyName 2-50 + hxqyCode qyCode length=18)** + ExtInfo optional。当前 fixture 已含 SerialNo + SendNodeCode + DesNodeCode + QueryDate + hxqyInfoNum=1 + hxqyInfo(hxqyName="核心企业测试" + hxqyCode="91110000100000000X" 18 chars) ✅ — fixture 字段集已完整，**唯一变更：SerialNo pad 到 30 字符**。

1. 删除 `@MockBean private XsdValidator xsdValidator;` (行 97-98) + 相关 imports + Javadoc @MockBean 段
2. fixture `buildCfxEnvelope3112` 修正：
   - CorrMsgId 空 → 20 零（同 Task 2）
   - MSG 段补 `<BatchHead3112 type="RequestHead">` 段（SendOrgCode/EntrustDate/TransitionNo）
   - hxqyCreditAmt3112 body SerialNo 需 length=30（DataType.xsd SerialNo `<xsd:length value="30"/>`）— 当前 fixture `serialNo = "SN3112" + msgIdSeq` 仅 12 字符 ❌ → 改为 pad 到 30 字符（如 `"SN3112" + msgIdSeq + "0".repeat(30 - 12)`）；测试 assertion `findBySerialNo(serialNo)` 同步用 padded 值，记得 derive ack idempotency key 也用 padded
3. test method `inbound3112_shouldPersistAndEnqueue9120` 真 XsdValidator 下 GREEN
4. Javadoc 修正

**Files:**
- Modify: `fep-web/src/test/java/com/puchain/fep/web/messageinbound/listener/Inbound3112WireTest.java`

- [ ] **Step 1: 基线确认 + 实测 3112.xsd hxqyCreditAmt3112 完整 sequence**

```bash
cd /Users/muzhou/FEP_v1.0_wt-r-new-1-real-xsd && \
./mvnw test -pl fep-web -am \
  -Dtest=Inbound3112WireTest \
  -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | tail -30 && \
grep -A 60 'complexType name="hxqyCreditAmt3112"' fep-processor/src/main/resources/xsd/3112.xsd | head -65
```
期望基线: `BUILD SUCCESS` + XSD 输出列出 hxqyCreditAmt3112 所有 element 及类型

> Subagent 必须依据 XSD 输出确定完整必填字段列表（minOccurs=1 默认）。若实测发现额外 required field（如 SerialNo 之外有其他 length 约束），按 facet 补齐 fixture。

- [ ] **Step 2: 删除 @MockBean + 相关 imports + Javadoc 段**

删除：
- 行 4-5: `import com.puchain.fep.processor.validation.ValidationResult;` + `import com.puchain.fep.processor.validation.XsdValidator;`
- 行 19: `import org.springframework.boot.test.mock.mockito.MockBean;`
- 行 31-32: `import static org.mockito.ArgumentMatchers.any;` + `import static org.mockito.Mockito.when;`
- 行 53-55: Javadoc `<p><b>profile / @MockBean</b>...` 段中 `+ @MockBean XsdValidator 返回 ok（CFX fixture 专注 wire 路径，真 XSD 校验已在 fep-processor + Task 2 dispatch 单测覆盖）` 子句
- 行 97-98: `@MockBean\n    private XsdValidator xsdValidator;`
- 行 103: `when(xsdValidator.validate(any(), any())).thenReturn(ValidationResult.ok());`

- [ ] **Step 3: 修复 fixture `buildCfxEnvelope3112`**

(a) 行 ~108：`final String serialNo = "SN3112" + msgIdSeq;` → 改为 `final String serialNo = pad30("SN3112" + msgIdSeq);` 并新增静态 helper：

```java
/** Pad to 30 chars with '0' suffix to satisfy DataType.xsd SerialNo length=30. */
private static String pad30(final String raw) {
    final int pad = 30 - raw.length();
    if (pad <= 0) return raw.substring(0, 30);
    return raw + "0".repeat(pad);
}
```

(b) `buildCfxEnvelope3112` 内行 ~160 `<CorrMsgId></CorrMsgId>` → `<CorrMsgId>00000000000000000000</CorrMsgId>`

(c) `buildCfxEnvelope3112` 内 `<MSG>` 后立即插入：
```
+ "<BatchHead3112>"
+ "<SendOrgCode>12345678901234</SendOrgCode>"
+ "<EntrustDate>20260524</EntrustDate>"
+ "<TransitionNo>" + cfxMsgId.substring(cfxMsgId.length() - 8) + "</TransitionNo>"
+ "</BatchHead3112>"
```

(d) 实测 Step 1 输出，按 hxqyCreditAmt3112 完整 sequence 校对当前 fixture 字段；若有 minOccurs=1 缺失字段，按 XSD facet 补齐。

- [ ] **Step 4: Javadoc 重写**

类 Javadoc 段（行 53-55）改为：

```
 * <p><b>真 XsdValidator（R-NEW-1 起）</b>：自 2026-05-26 R-NEW-1 起，本测试不再
 * {@code @MockBean XsdValidator}，fixture {@code buildCfxEnvelope3112} 满足 3112.xsd
 * 完整约束（HEAD CorrMsgId 20 零、BatchHead3112 RequestHead、hxqyCreditAmt3112
 * SerialNo length=30 pad、SendNodeCode/DesNodeCode NodeCode length=14、QueryDate
 * Date、hxqyInfoNum + hxqyInfo nested）。与 sibling {@link Inbound2101WireTest}/
 * {@link InboundAck9120BatchWireTest} 同 cache key（R-NEW-1 统一 cache）。</p>
```

- [ ] **Step 5: 运行测试确认真 XSD 下 GREEN**

```bash
cd /Users/muzhou/FEP_v1.0_wt-r-new-1-real-xsd && \
./mvnw test -pl fep-web -am \
  -Dtest=Inbound3112WireTest \
  -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | tail -30
```
期望: `BUILD SUCCESS` 1 test passed（如 RED，按 XSD 输出定位违规字段，循环修复）

- [ ] **Step 6: 提交**

```bash
cd /Users/muzhou/FEP_v1.0_wt-r-new-1-real-xsd && \
git add fep-web/src/test/java/com/puchain/fep/web/messageinbound/listener/Inbound3112WireTest.java && \
git commit -m "$(cat <<'EOF'
refactor(web): Inbound3112WireTest 删 @MockBean XsdValidator + fixture 真 XSD 合规 (R-NEW-1)

删除 @MockBean XsdValidator + mock setup + 相关 imports。fixture
buildCfxEnvelope3112 修正：(a) SerialNo pad 到 30 字符 (DataType.xsd
length=30) + helper pad30 (b) CorrMsgId 20 零 (c) MSG 段补 BatchHead3112
RequestHead。Javadoc R-NEW-1 真 XsdValidator 标注。

PRD: v1.3 §3.2 + §4.6 3112 + §4.7 模式 5
守护 FR: FR-MSG-3112

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

### Task 4: InboundAck9120BatchWireTest 真 XSD 化（4 报文 parameterized） `模式 A`

**PRD 依据:** v1.3 §3.2 + §4.6 3105/3009/3103/3113 + §4.7 模式 2/3/5 受理 + 各 XSD：
- 3105.xsd: MSG → BatchHead3105 RequestHead + rzApplyInfo3105
- 3009.xsd: MSG → **RealHead3009** RequestHead（特殊命名）+ rzReturnInfo3009 minOccurs=0
- 3103.xsd: MSG → BatchHead3103 **ResponseHead**（含 Result）+ ArchiveReturnInfo3103
- 3113.xsd: MSG → BatchHead3113 **ResponseHead**（含 Result）+ hxqyCreditAmt3113 minOccurs=0

**追溯 ID:** 守护 FR-MSG-3105 + FR-MSG-3009 + FR-MSG-3103 + FR-MSG-3113

**起草阶段 grep 落定 4 body 必填字段集**:

| body | required sequence（XSD minOccurs=1） | 当前 fixture | 落差修订 |
|------|-----------------------------------|------------|---------|
| rzApplyInfo3105 | SerialNo + SendNodeCode + DesNodeCode + ApplyMode + PlatApplyNo + StdBizMode(default 11) + hxqyName + hxqyCode + rzpzNo + rzqyName + rzqyCode + rzqyPlatNo | 已含 ✅ | 仅 SerialNo pad30 |
| rzReturnInfo3009 | SerialNo + SendNodeCode + DesNodeCode + PlatApplyNo + hxqyName + rzpzNo + rzPhaseCode | 已含 ✅ | 仅 SerialNo pad30 |
| ArchiveReturnInfo3103 | SerialNo + SendNodeCode + DesNodeCode + **CreationRetCode Number1to2(1-2 字符)** + hxqyName + hxqyCode + rzqyName + rzqyCode | 当前 CreationRetCode="00000" 5 字符 ❌ 违反 Number1to2 | **改 "01"** + SerialNo pad30 |
| hxqyCreditAmt3113 | SerialNo + SendNodeCode + DesNodeCode + QueryDate + CreditInfoNum(Integer) + **CreditInfo (maxOccurs=200，含 hxqyName + hxqyCode + RetCode Result length=5)** | 当前缺 CreditInfo 列表（仅 CreditInfoNum=1） ❌ | **补 1 个 `<CreditInfo><hxqyName>核心企业测试</hxqyName><hxqyCode>91110000100000000X</hxqyCode><RetCode>00000</RetCode></CreditInfo>`** + SerialNo pad30 |

**验收标准（从 4 XSD + Base.xsd 起草阶段 grep 落定）:**
1. 删除 `@MockBean private XsdValidator xsdValidator;` (行 106-107) + 相关 imports + Javadoc @MockBean 段
2. `wrapCfx` 修正：
   - CorrMsgId 空 → 20 零
   - MSG 段必须按 code 路由插入正确的 BatchHead/RealHead：
     - 3105: `<BatchHead3105>` (RequestHead) — SendOrgCode/EntrustDate/TransitionNo
     - 3009: `<RealHead3009>` (RequestHead) — 同上（命名特殊！）
     - 3103: `<BatchHead3103>` (ResponseHead) — SendOrgCode/EntrustDate/TransitionNo/Result="00000"
     - 3113: `<BatchHead3113>` (ResponseHead) — 同 3103
3. 4 body fixture SerialNo 当前 "SNK" + code + msgIdSeq (约 13 字符) → 按 DataType.xsd SerialNo length=30 pad，复用 Task 3 模式新增 `pad30` helper（已 grep 验证 fep-web/src/test/java 下无 testutil/testsupport/fixture 共享包，本测试复制 pad30 helper 合理 / 不动 src/main scope）
4. 4 body fixture 必填字段按起草阶段 grep 落定表修订：BODY_3103 CreationRetCode "00000" → "01"（Number1to2 1-2 字符）+ BODY_3113 补 1 个 CreditInfo nested element（hxqyName + hxqyCode + RetCode "00000"）；BODY_3105 / BODY_3009 字段已完整无变更
5. ack idempotency key derivation 同步：`AckIdempotencyKeys.derive(code, serialNo)` 输入用 padded serialNo
6. test method `inboundReport_shouldPersistAndEnqueue9120` 4 参数全部真 XsdValidator 下 GREEN
7. Javadoc 修正"<b>profile / @MockBean</b>" 段 + 其他相关注释

**Files:**
- Modify: `fep-web/src/test/java/com/puchain/fep/web/messageinbound/listener/InboundAck9120BatchWireTest.java`

- [ ] **Step 1: 基线 + 4 XSD 实测**

```bash
cd /Users/muzhou/FEP_v1.0_wt-r-new-1-real-xsd && \
./mvnw test -pl fep-web -am \
  -Dtest=InboundAck9120BatchWireTest \
  -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | tail -30 && \
for code in 3105 3009 3103 3113; do
  echo "=== $code.xsd MSG + body ==="
  sed -n '/complexType name="MSG"/,/<\/xsd:complexType>/p' fep-processor/src/main/resources/xsd/${code}.xsd | head -20
  echo ""
done
```
期望基线: `BUILD SUCCESS` 4 tests passed + XSD 输出 4 个 MSG 结构

- [ ] **Step 2: 删除 @MockBean + 相关 imports + Javadoc 段**

删除：
- 行 4-5: `import com.puchain.fep.processor.validation.ValidationResult;` + `import com.puchain.fep.processor.validation.XsdValidator;`
- 行 21: `import org.springframework.boot.test.mock.mockito.MockBean;`
- 行 31-32: `import static org.mockito.ArgumentMatchers.any;` + `import static org.mockito.Mockito.when;`
- 行 62-67: Javadoc `<p><b>profile / @MockBean</b>...` 段中 `+ @MockBean XsdValidator 返回 ok（CFX fixture 专注 wire 路径，真 XSD 校验已在 fep-processor + dispatch 单测覆盖）` 子句 + 后续 fixture 简化说明里"XsdValidator mocked 故结构完整即可 unmarshal"子句改写
- 行 106-107: `@MockBean\n    private XsdValidator xsdValidator;`
- 行 125: `when(xsdValidator.validate(any(), any())).thenReturn(ValidationResult.ok());`

- [ ] **Step 3: wrapCfx 路由式 BatchHead/RealHead 注入**

修改 `wrapCfx` signature 增加 code 路由逻辑，按 code 注入正确的 BatchHead/RealHead：

```java
private static String wrapCfx(final String code, final String cfxMsgId, final String body) {
    final String batchHead = buildBatchHeadByCode(code, cfxMsgId);
    return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
            + "<CFX>"
            + "<HEAD>"
            + "<Version>1.0</Version>"
            + "<SrcNode>" + FepConstants.HNDEMP_NODE_CODE + "</SrcNode>"
            + "<DesNode>12345678901234</DesNode>"
            + "<App>HNDEMP</App>"
            + "<MsgNo>" + code + "</MsgNo>"
            + "<MsgId>" + cfxMsgId + "</MsgId>"
            + "<CorrMsgId>00000000000000000000</CorrMsgId>"
            + "<WorkDate>20260526</WorkDate>"
            + "</HEAD>"
            + "<MSG>"
            + batchHead
            + body
            + "</MSG>"
            + "</CFX>";
}

/**
 * Build the BatchHead/RealHead element per the message code:
 * <ul>
 *   <li>3105: BatchHead3105 (RequestHead)</li>
 *   <li>3009: RealHead3009 (RequestHead, special naming)</li>
 *   <li>3103/3113: BatchHead<code> (ResponseHead, includes Result)</li>
 * </ul>
 *
 * @param code      4-digit message code
 * @param cfxMsgId  20-digit CFX HEAD MsgId; last 8 chars = TransitionNo
 * @return BatchHead/RealHead XML fragment
 */
private static String buildBatchHeadByCode(final String code, final String cfxMsgId) {
    final String transitionNo = cfxMsgId.substring(cfxMsgId.length() - 8);
    final String elementName = switch (code) {
        case "3009" -> "RealHead3009";
        default -> "BatchHead" + code;
    };
    final boolean responseHead = "3103".equals(code) || "3113".equals(code);
    final String resultElement = responseHead ? "<Result>00000</Result>" : "";
    return "<" + elementName + ">"
            + "<SendOrgCode>12345678901234</SendOrgCode>"
            + "<EntrustDate>20260526</EntrustDate>"
            + "<TransitionNo>" + transitionNo + "</TransitionNo>"
            + resultElement
            + "</" + elementName + ">";
}
```

- [ ] **Step 4: serialNo pad 到 30**

修改 test method `inboundReport_shouldPersistAndEnqueue9120`：

```java
final String rawSerial = "SNK" + code + msgIdSeq;  // 13 chars
final String serialNo = pad30(rawSerial);          // 30 chars per DataType.xsd
```

新增静态 helper（与 Task 3 同实现）：

```java
/** Pad to 30 chars with '0' suffix to satisfy DataType.xsd SerialNo length=30. */
private static String pad30(final String raw) {
    final int pad = 30 - raw.length();
    if (pad <= 0) return raw.substring(0, 30);
    return raw + "0".repeat(pad);
}
```

- [ ] **Step 5: body fixture 按起草阶段 grep 落定表修订**

依据 §起草阶段 grep 落定 4 body 必填字段集 表格修订 BODY_*_TEMPLATE：

(a) **BODY_3103_TEMPLATE** (行 222-232): CreationRetCode "00000" → "01"（Number1to2 1-2 字符约束）— Edit 单点：

```
old: "<CreationRetCode>00000</CreationRetCode>"
new: "<CreationRetCode>01</CreationRetCode>"
```

(b) **BODY_3113_TEMPLATE** (行 235-242): 在 `<CreditInfoNum>1</CreditInfoNum>` 之后立即插入 CreditInfo nested element：

```
old:
        "<CreditInfoNum>1</CreditInfoNum>"
                + "</hxqyCreditAmt3113>";

new:
        "<CreditInfoNum>1</CreditInfoNum>"
                + "<CreditInfo>"
                + "<hxqyName>核心企业测试</hxqyName>"
                + "<hxqyCode>91110000100000000X</hxqyCode>"
                + "<RetCode>00000</RetCode>"
                + "</CreditInfo>"
                + "</hxqyCreditAmt3113>";
```

(c) BODY_3105_TEMPLATE + BODY_3009_TEMPLATE: 字段集已完整，**无变更**。

(d) 维持 InboundAck9120BatchWireTest 既有 disjoint-seed 设计（SEQ 300000+ → transitionNo 00300000..00300003 互不碰撞）。

> 若真 validator 实跑发现起草阶段 grep 未预测的额外违规字段（如某个 nested type 的子字段 minOccurs=1 未在表中），按红线 `feedback_xsd_compliance_fix_real_validator_on_sut` 逐个回溯 facet+spec 不打地鼠循环到 GREEN。

- [ ] **Step 6: Javadoc 重写**

类 Javadoc 段（行 62-67）改为：

```
 * <p><b>真 XsdValidator（R-NEW-1 起）/ profile</b>：自 2026-05-26 R-NEW-1 起，本测试
 * 不再 {@code @MockBean XsdValidator}，fixture 4 body + wrapCfx 满足各 XSD 完整约束
 * （HEAD CorrMsgId 20 零、BatchHead<code>/RealHead3009 RequestHead/ResponseHead 按
 * code 路由、body SerialNo length=30 pad）。默认 dev profile
 * （MockSignService/MockKeyService 让 9120 outbound 加签通过）。与 sibling
 * {@link Inbound2101WireTest}/{@link Inbound3112WireTest} 同 cache key（R-NEW-1 统一）。</p>
```

- [ ] **Step 7: 运行测试确认 4 参数真 XSD 下 GREEN**

```bash
cd /Users/muzhou/FEP_v1.0_wt-r-new-1-real-xsd && \
./mvnw test -pl fep-web -am \
  -Dtest=InboundAck9120BatchWireTest \
  -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | tail -30
```
期望: `BUILD SUCCESS` 4 tests passed

> 如 RED 按 XSD 报错循环修复（红线 `feedback_xsd_compliance_fix_real_validator_on_sut`：facet+spec 逐个回溯，不打地鼠）。

- [ ] **Step 8: 提交**

```bash
cd /Users/muzhou/FEP_v1.0_wt-r-new-1-real-xsd && \
git add fep-web/src/test/java/com/puchain/fep/web/messageinbound/listener/InboundAck9120BatchWireTest.java && \
git commit -m "$(cat <<'EOF'
refactor(web): InboundAck9120BatchWireTest 删 @MockBean XsdValidator + fixture 真 XSD 合规 (R-NEW-1)

删除 @MockBean XsdValidator + mock setup + 相关 imports。wrapCfx 重构：
(a) buildBatchHeadByCode 按 code 路由 BatchHead/RealHead/RequestHead/ResponseHead
(3009 特殊用 RealHead3009 / 3103+3113 用 ResponseHead 含 Result="00000") (b)
CorrMsgId 20 零 (c) 4 body SerialNo pad 到 30 字符 (DataType.xsd length=30) +
helper pad30 (d) 4 body fixture 按 XSD 实测约束校对补齐。Javadoc R-NEW-1 真
XsdValidator 标注。

PRD: v1.3 §3.2 + §4.6 3105/3009/3103/3113 + §4.7 模式 2/3/5
守护 FR: FR-MSG-3105 + FR-MSG-3009 + FR-MSG-3103 + FR-MSG-3113

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

### Task 5: P5OutboundEndToEndIntegrationTest + SQL fixture 真 XSD 化 `模式 A`

**PRD 依据:** v1.3 §3.2 + §3.2.2 + §4.6 3009/3101/3102/3105/3107/3109/3112/3116 + 各 outbound XSD（fep-processor/src/main/resources/xsd/*.xsd）
**追溯 ID:** 守护 FR-MSG-3009 + FR-MSG-3101 + FR-MSG-3102 + FR-MSG-3105 + FR-MSG-3107 + FR-MSG-3109 + FR-MSG-3112 + FR-MSG-3116

**起草阶段 grep 落定 8 outbound body 必填字段集**:

| body | required sequence（XSD minOccurs=1） |
|------|-----------------------------------|
| rzReturnInfo3009 | SerialNo + SendNodeCode + DesNodeCode + PlatApplyNo + hxqyName + rzpzNo + rzPhaseCode |
| ContractInfo3101 | SerialNo + SendNodeCode + DesNodeCode + ContractNo(htSerialNo 6-50) + ContractType(String0to30) + DigitalSeal(Boolean default 0) + ContractFilename(FileName 5-100) + jfqyName(qyName 2-50) + yfqyName(qyName 2-50) |
| ArchiveInfo3102 | SerialNo + SendNodeCode + DesNodeCode + ApplyMode(Integer default 1) + hxqyName + hxqyCode + rzqyName + rzqyCode |
| rzApplyInfo3105 | SerialNo + SendNodeCode + DesNodeCode + ApplyMode + PlatApplyNo + StdBizMode(default 11) + hxqyName + hxqyCode + rzpzNo + rzqyName + rzqyCode + rzqyPlatNo(rzqyPlatCode 10-30) |
| pzCheckQuery3107 | SerialNo + SendNodeCode + DesNodeCode + CheckDate + hxqyNum(Number1to2) + hxqyInfo(maxOccurs=200; hxqyName + hxqyCode) |
| qyRegister3109 | SerialNo + SendNodeCode + DesNodeCode + qyFlag(Number1to2) — 其余（hxqyInfo/qyAccLockInfo/PlatInfo/ExtInfo）全 optional |
| hxqyCreditAmt3112 | SerialNo + SendNodeCode + DesNodeCode + QueryDate + hxqyInfoNum(Integer) + hxqyInfo(maxOccurs=200; hxqyName + hxqyCode) |
| BankCheckDay3116 | SerialNo + SendNodeCode + DesNodeCode + hxqyName + hxqyCode + CheckDate + CheckDetailNum(Integer) + CheckDetailInfo(maxOccurs=200; **sid + PlatNodeCode + BizType + rzqyName(qyName 2-50) + rzqyCode(qyCode 18) + rzAmt(Currency) + rzRate(Rate `[0-9]{1,2}\.[0-9]{4}`) + rzStartDate(Date) + rzEndDate(Date)** — 9 minOccurs=1 字段；Amt(default 0.00) / RepayStyle / lxAmt / dbAmt / PlatServiceAmt / CheckMemo / pzNo / BillNo 全 optional) |

> SerialNo 全 8 body 须 length=30（pad SerialNo 至 30 字符 "0" 后缀，如 "SN3009000000000000000000000001"）。日期字段 yyyyMMdd 格式（如 "20260526"）。Node/Org Code length=14。Number 类型为数字字符串无小数。

**验收标准（从 8 outbound XSD + Base.xsd 起草阶段 grep 落定）:**
1. 删除 `@MockBean private XsdValidator xsdValidator;` (行 83-91) + 相关 import + Javadoc @MockBean 段（行 44-46）
2. 删除 3 处 mock setup `when(xsdValidator.validate(any(), any())).thenReturn(ValidationResult.ok());` (行 99, 128, 156, 182 — verify 4 处实际行号)
3. SQL fixture `outbound_queue_8_messages.sql` 8 行 body XML 替换：
   - 当前自闭合空标签 `<rzReturnInfo3009/>` / `<ContractInfo3101/>` / `<ArchiveInfo3102/>` / `<rzApplyInfo3105/>` / `<pzCheckQuery3107/>` / `<qyRegister3109/>` / `<hxqyCreditAmt3112/>` / `<BankCheckDay3116/>` 全部违反各 XSD body sequence minOccurs=1 必填
   - 每行 body 替换为按各 XSD 实测约束的最小满足 fixture（含所有 minOccurs=1 元素 + DataType.xsd facet 满足）
   - 注意：outbound 路径是 builder 装配整 CFX envelope（HEAD 由 BodyMsgIdGenerator/CommonHeadComposer 注入 / BatchHead<code> 由 dispatcher 按 msgNo 装配），SQL 中 `message_body_xml` 仅 body XML（不含 HEAD/BatchHead），故只需保证 body element 满足各 body complexType
4. test method 4 个全部真 XsdValidator 下 GREEN（`e2e_8_messages_should_all_reach_SENT_with_valid_msg_id` + `e2e_5_consecutive_failures_should_reach_DEAD_LETTER` + `e2e_metrics_endpoint_should_expose_outbound_send_total` + `send_shouldExecuteOutsideTransaction`）
5. Javadoc 修正"<b>@MockBean XsdValidator</b>" 段（行 44-46）改写为 R-NEW-1 真 XsdValidator 标注

**Files:**
- Modify: `fep-web/src/test/java/com/puchain/fep/web/outbound/consumer/P5OutboundEndToEndIntegrationTest.java`
- Modify: `fep-web/src/test/resources/sql/p5/outbound_queue_8_messages.sql`

- [ ] **Step 1: 基线 + 8 outbound XSD 实测 body sequence**

```bash
cd /Users/muzhou/FEP_v1.0_wt-r-new-1-real-xsd && \
./mvnw test -pl fep-web -am \
  -Dtest=P5OutboundEndToEndIntegrationTest \
  -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | tail -30 && \
for code in 3009 3101 3102 3105 3107 3109 3112 3116; do
  echo "=== $code body complexType + sequence ==="
  awk '/complexType name="MSG"/,/<\/xsd:complexType>/' fep-processor/src/main/resources/xsd/${code}.xsd | grep -E 'element name|complexType|complexType' | head -10
done
```
期望基线: `BUILD SUCCESS` 4 tests passed + 8 outbound XSD body sequence 输出

> Subagent 必须按 8 个 body complexType（如 `RzReturnInfo3009` / `ContractInfo3101` 等）grep 完整 sequence + DataType.xsd facet 后再写 SQL fixture body。

- [ ] **Step 2: 删除 @MockBean + 相关 imports + Javadoc 段**

修改 `P5OutboundEndToEndIntegrationTest.java`：
- 行 4-5: `import static org.mockito.ArgumentMatchers.any;` + `import static org.mockito.Mockito.when;` — `Mockito.when` 仍被 mockProducer 用，**保留**；`ArgumentMatchers.any` 仍被 mockProducer 用，**保留**
- 行 7: `import com.puchain.fep.processor.validation.ValidationResult;` 删除
- 行 8: `import com.puchain.fep.processor.validation.XsdValidator;` 删除
- 行 19: `import org.springframework.boot.test.mock.mockito.MockBean;` **保留**（mockProducer 仍用）
- 行 44-46: Javadoc `<p><b>@MockBean XsdValidator</b>...` 整段（直到下一 `</p>`）
- 行 83-91: 整段 `@MockBean private XsdValidator xsdValidator;`（含 Javadoc 注释块）
- 4 处 `when(xsdValidator.validate(any(), any())).thenReturn(ValidationResult.ok());`：行 99 / 128 / 156 / 182（按实测行号验证）

- [ ] **Step 3: SQL fixture 8 body 重写（起草阶段 grep 落定 8 字符串）**

按 §起草阶段 grep 落定 8 outbound body 必填字段集 表格，**8 行 INSERT 第 6 列 `message_body_xml` 各替换为以下 SQL 字符串（一行不换行）**：

```
-- 行 28-31 (3009): '<rzReturnInfo3009/>' →
'<rzReturnInfo3009><SerialNo>SN3009000000000000000000000001</SerialNo><SendNodeCode>BANK0010000001</SendNodeCode><DesNodeCode>HNDM0010000001</DesNodeCode><PlatApplyNo>PLAT001</PlatApplyNo><hxqyName>核心企业测试</hxqyName><rzpzNo>PZ001</rzpzNo><rzPhaseCode>01</rzPhaseCode></rzReturnInfo3009>'

-- 行 32-35 (3101): '<ContractInfo3101/>' →
'<ContractInfo3101><SerialNo>SN3101000000000000000000000002</SerialNo><SendNodeCode>BANK0010000001</SendNodeCode><DesNodeCode>HNDM0010000001</DesNodeCode><ContractNo>HT2026000001</ContractNo><ContractType>融资合同</ContractType><DigitalSeal>1</DigitalSeal><ContractFilename>contract.pdf</ContractFilename><jfqyName>甲方企业测试</jfqyName><yfqyName>乙方企业测试</yfqyName></ContractInfo3101>'

-- 行 36-39 (3102): '<ArchiveInfo3102/>' →
'<ArchiveInfo3102><SerialNo>SN3102000000000000000000000003</SerialNo><SendNodeCode>BANK0010000001</SendNodeCode><DesNodeCode>HNDM0010000001</DesNodeCode><ApplyMode>1</ApplyMode><hxqyName>核心企业测试</hxqyName><hxqyCode>91110000100000000X</hxqyCode><rzqyName>融资企业测试</rzqyName><rzqyCode>91110000200000000Y</rzqyCode></ArchiveInfo3102>'

-- 行 40-43 (3105): '<rzApplyInfo3105/>' →
'<rzApplyInfo3105><SerialNo>SN3105000000000000000000000004</SerialNo><SendNodeCode>BANK0010000001</SendNodeCode><DesNodeCode>HNDM0010000001</DesNodeCode><ApplyMode>1</ApplyMode><PlatApplyNo>PLAT2026000004</PlatApplyNo><StdBizMode>01</StdBizMode><hxqyName>核心企业测试</hxqyName><hxqyCode>91110000100000000X</hxqyCode><rzpzNo>PZ20260526001</rzpzNo><rzqyName>融资企业测试</rzqyName><rzqyCode>91110000200000000Y</rzqyCode><rzqyPlatNo>RZPLAT2026000004</rzqyPlatNo></rzApplyInfo3105>'

-- 行 44-47 (3107): '<pzCheckQuery3107/>' →
'<pzCheckQuery3107><SerialNo>SN3107000000000000000000000005</SerialNo><SendNodeCode>BANK0010000001</SendNodeCode><DesNodeCode>HNDM0010000001</DesNodeCode><CheckDate>20260505</CheckDate><hxqyNum>1</hxqyNum><hxqyInfo><hxqyName>核心企业测试</hxqyName><hxqyCode>91110000100000000X</hxqyCode></hxqyInfo></pzCheckQuery3107>'

-- 行 48-51 (3109): '<qyRegister3109/>' →
'<qyRegister3109><SerialNo>SN3109000000000000000000000006</SerialNo><SendNodeCode>BANK0010000001</SendNodeCode><DesNodeCode>HNDM0010000001</DesNodeCode><qyFlag>1</qyFlag></qyRegister3109>'

-- 行 52-55 (3112): '<hxqyCreditAmt3112/>' →
'<hxqyCreditAmt3112><SerialNo>SN3112000000000000000000000007</SerialNo><SendNodeCode>BANK0010000001</SendNodeCode><DesNodeCode>HNDM0010000001</DesNodeCode><QueryDate>20260505</QueryDate><hxqyInfoNum>1</hxqyInfoNum><hxqyInfo><hxqyName>核心企业测试</hxqyName><hxqyCode>91110000100000000X</hxqyCode></hxqyInfo></hxqyCreditAmt3112>'

-- 行 56-59 (3116): '<BankCheckDay3116/>' →
'<BankCheckDay3116><SerialNo>SN3116000000000000000000000008</SerialNo><SendNodeCode>BANK0010000001</SendNodeCode><DesNodeCode>HNDM0010000001</DesNodeCode><hxqyName>核心企业测试</hxqyName><hxqyCode>91110000100000000X</hxqyCode><CheckDate>20260505</CheckDate><CheckDetailNum>1</CheckDetailNum><CheckDetailInfo><sid>1</sid><PlatNodeCode>PLAT0010000001</PlatNodeCode><BizType>01</BizType><rzqyName>融资企业测试</rzqyName><rzqyCode>91110000200000000Y</rzqyCode><rzAmt>10000.00</rzAmt><rzRate>06.0000</rzRate><rzStartDate>20260101</rzStartDate><rzEndDate>20261231</rzEndDate></CheckDetailInfo></BankCheckDay3116>'
```

> 关键 facet 满足：
> - SerialNo 30 字符 pad（SN + code + 0000... 共 30）
> - SendNodeCode/DesNodeCode/PlatNodeCode NodeCode length=14
> - hxqyCode/rzqyCode qyCode length=18
> - hxqyName/rzqyName/jfqyName/yfqyName qyName 2-50
> - CreationRetCode/ApplyMode/StdBizMode/qyFlag/hxqyNum/BizType Number1to2 (1-2 字符)
> - CheckDate/QueryDate Date pattern `[0-9]{4}(0[1-9]|1[0-2])([0-2][1-9]|[12][0-9]|3[01])`
> - ContractType String0to30
> - ContractFilename FileName 5-100 字符

> SQL body XML 内含中文（"核心企业测试"等）不影响 SQL string（无 `'` 转义需求）。每行 INSERT 仍维持原有 13 列结构（仅 `message_body_xml` 列内容替换）。
>
> **若真 validator 实跑发现起草阶段 grep 未预测的额外违规（如某 nested type 的 minOccurs=1 子字段表中未覆盖），按红线 `feedback_xsd_compliance_fix_real_validator_on_sut` 逐个回溯 facet+spec 修复，不打地鼠。**

- [ ] **Step 4: Javadoc 重写**

行 44-46 改为：

```
 * <p><b>真 XsdValidator（R-NEW-1 起）</b>：自 2026-05-26 R-NEW-1 起，本测试不再
 * {@code @MockBean XsdValidator}。SQL fixture
 * {@code /sql/p5/outbound_queue_8_messages.sql} 8 行 body XML 按各
 * {@code <msgNo>.xsd} 实测约束填（SerialNo length=30 pad、所有 minOccurs=1
 * element 填齐、DataType.xsd facet 满足）。envelope HEAD 由 builder
 * BodyMsgIdGenerator 注入 / BatchHead<code> 由 dispatcher 按 msgNo 装配。
 * {@code @MockBean TlqProducer} 保留（控制 SENT/DEAD_LETTER 路径）。</p>
```

- [ ] **Step 5: 运行测试确认 4 method 真 XSD 下 GREEN**

```bash
cd /Users/muzhou/FEP_v1.0_wt-r-new-1-real-xsd && \
./mvnw test -pl fep-web -am \
  -Dtest=P5OutboundEndToEndIntegrationTest \
  -Dsurefire.failIfNoSpecifiedTests=false 2>&1 | tail -30
```
期望: `BUILD SUCCESS` 4 tests passed

> 如 RED：日志列出 XSD validate 失败的元素名 + line/column → 对应 SQL fixture 行的 body XML 字段，按 XSD 修正循环到 GREEN。

- [ ] **Step 6: 提交**

```bash
cd /Users/muzhou/FEP_v1.0_wt-r-new-1-real-xsd && \
git add fep-web/src/test/java/com/puchain/fep/web/outbound/consumer/P5OutboundEndToEndIntegrationTest.java \
        fep-web/src/test/resources/sql/p5/outbound_queue_8_messages.sql && \
git commit -m "$(cat <<'EOF'
refactor(web): P5OutboundEndToEndIntegrationTest 删 @MockBean XsdValidator + 8 body XSD 合规 (R-NEW-1)

删除 @MockBean XsdValidator + 4 处 mock setup + 相关 imports
(ValidationResult/XsdValidator)，保留 @MockBean TlqProducer (控制 SENT/DLQ
路径)。SQL fixture outbound_queue_8_messages.sql 8 body XML 全部按各 XSD
实测约束重写：SerialNo length=30 pad + 所有 minOccurs=1 element 填齐 +
DataType.xsd facet 满足 (3009/3101/3102/3105/3107/3109/3112/3116)。
Javadoc R-NEW-1 真 XsdValidator 标注。

PRD: v1.3 §3.2 + §3.2.2 + §4.6 8 outbound 报文
守护 FR: FR-MSG-3009/3101/3102/3105/3107/3109/3112/3116

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

### Task 6: 全 fep-web 回归 + Javadoc 巡查 + 闭环 `模式 A`

**PRD 依据:** 守护 Plan 整体验收 #4 (cache key 统一) + #5 (silent-dead 残留 0) + #6 (Javadoc 腐烂 0) + #7 (fep-web 全模块 GREEN) + #8 (新 SpotBugs/find-sec-bugs finding 0)
**追溯 ID:** Plan-level closing

**验收标准:**
1. `./mvnw test -pl fep-web -am` 全 GREEN（known issue perf flake `AsyncPipelineIntegrationTest#performanceBaseline_*` 允许 known 范围内，参 CLAUDE.md "已知约束"）
2. ArchUnit 不变量保持（dispatcher 21 / SerialNoBearing 17 等不变）
3. 5 测试 + SQL fixture 文件中 grep `XsdValidator` 0 命中（已全部删除）
4. 5 测试类 Javadoc grep `MockBean.*XsdValidator` 0 命中（已全部重写）
5. `./mvnw verify -pl fep-web -am -DskipITs=false` 不引入新 SpotBugs / find-sec-bugs finding（红线 `feedback_security_doc_must_distinguish_spotbugs_layers` 明示 core SpotBugs 层 + find-sec-bugs 层均无新增）
6. （可选）worktree teardown：rebase onto latest origin/main + ff-merge + push + `git worktree remove`

**Files:** 无新增/修改（验证 Task）

- [ ] **Step 1: baseline drift 重测 + rebase onto df15613（必跑）**

```bash
cd /Users/muzhou/FEP_v1.0_wt-r-new-1-real-xsd && \
git fetch origin && \
git log origin/main --oneline -3 && \
git rebase origin/main 2>&1 | tail -20
```
期望: `Successfully rebased` — origin/main 至少 `df15613`（pitest-maven 1.25.0 升级别会话已 ship，Plan 起草时 baseline 已对齐）。本 worktree 5 Task commits 应顺利 replay onto `df15613` 上（无 conflict — pitest 升级仅动 pom.xml，本 Plan 仅动 test 代码与 SQL fixture，文件级无交集）。

> 若 conflict（不期望，但若发生）按 `git rebase --abort` 退回 + 评估后再次 rebase；若 origin/main 继续向前推进至 ≥ `df15613` 之上的新 commits，亦正常 rebase 即可（Plan 内 5 Task 改造的测试 / SQL fixture 路径明确，与别会话 callback PR #27 / simplify-q-drain 无重叠）。

- [ ] **Step 2: 全 fep-web 模块回归**

```bash
cd /Users/muzhou/FEP_v1.0_wt-r-new-1-real-xsd && \
./mvnw test -pl fep-web -am 2>&1 | tee /tmp/r-new-1-regression-$(date +%s).log | tail -40
```
期望: `BUILD SUCCESS` 全部 GREEN（known perf flake 例外）

> **如 RED**：grep "Tests run.*Failures: [^0]\|Errors: [^0]" 定位失败测试，分类：(a) R-NEW-1 引入 — 回 Task N 修复 (b) 既有 known issue（如 AsyncPipelineIntegrationTest perf）— 不阻塞 (c) 别会话 main 引入（PR #27 merge / pitest 升级）— 报告但不归属本 Plan。

- [ ] **Step 3: grep 残留巡查**

```bash
cd /Users/muzhou/FEP_v1.0_wt-r-new-1-real-xsd && \
echo "=== grep XsdValidator 在 5 改造文件 ===" && \
for f in \
  "fep-web/src/test/java/com/puchain/fep/web/outbound/consumer/Outbound9120AckEnvelopeBuilderTest.java" \
  "fep-web/src/test/java/com/puchain/fep/web/outbound/consumer/P5OutboundEndToEndIntegrationTest.java" \
  "fep-web/src/test/java/com/puchain/fep/web/messageinbound/listener/Inbound2101WireTest.java" \
  "fep-web/src/test/java/com/puchain/fep/web/messageinbound/listener/Inbound3112WireTest.java" \
  "fep-web/src/test/java/com/puchain/fep/web/messageinbound/listener/InboundAck9120BatchWireTest.java"; do
  echo "--- $f ---"
  grep -n "XsdValidator\|ValidationResult" "$f" || echo "  (clean)"
done && \
echo "" && \
echo "=== grep @MockBean.*XsdValidator 全 fep-web 残留 ===" && \
grep -rn "@MockBean.*XsdValidator\|XsdValidator.*@MockBean" fep-web/src/test/java/ || echo "  (clean)"
```
期望: 5 改造文件全部 `(clean)` + 全 fep-web `(clean)`

- [ ] **Step 4: SpotBugs / find-sec-bugs 双层 check**

```bash
cd /Users/muzhou/FEP_v1.0_wt-r-new-1-real-xsd && \
./mvnw verify -pl fep-web -am -DskipITs=false 2>&1 | tail -30
```
期望: `BUILD SUCCESS` + 0 新增 SpotBugs/find-sec-bugs finding

- [ ] **Step 5: worktree teardown（push 后做，或留待 session-end）**

```bash
cd /Users/muzhou/FEP_v1.0 && \
git checkout main && \
git pull origin main && \
git merge --ff-only refactor/r-new-1-real-xsd-validator && \
git push origin main && \
git worktree remove /Users/muzhou/FEP_v1.0_wt-r-new-1-real-xsd && \
git branch -d refactor/r-new-1-real-xsd-validator && \
git worktree list
```
> session-end Phase 1 worktree 残留检查（红线 `feedback_main_worktree_unpushed_wip_detection`）会验证 `git log origin/main..main` 为空 + `git worktree list` 无残留。

- [ ] **Step 6: NO git commit for this Task（doc-only verification + worktree teardown，无 source 改动）**

> 本 Task 是验证 + 闭环，无新 source 改动。如 Step 1-5 全 PASS 即闭环。如 Step 5 push 失败按 `feedback_main_worktree_unpushed_wip_detection` 排查。

---

## 自检清单

### 1. PRD 覆盖度
- ✅ §3.2 报文结构 (HEAD + MSG + body) — 5 测试 fixture 全段覆盖
- ✅ §3.2.2 报文头 HEAD MsgId/CorrMsgId Number length=20 — 5 测试 HEAD CorrMsgId 20 零修正
- ✅ §3.2.2 报文头 HEAD 固定字段 App=HNDEMP — 5 测试 HEAD 复用 P0 fix
- ✅ §4.6 报文方向 — 8 outbound + 5 inbound 全 XSD 守护
- ✅ §4.7 模式 — 模式 5/6 (9120 ack) + 模式 2/3 (受理) 全覆盖
- 本 Plan **不引入新 FR-ID**，守护既有 FR-MSG-2101 + FR-MSG-3009 + FR-MSG-3101 + FR-MSG-3102 + FR-MSG-3103 + FR-MSG-3105 + FR-MSG-3107 + FR-MSG-3109 + FR-MSG-3112 + FR-MSG-3113 + FR-MSG-3116 + FR-MSG-9120

### 2. 安全边界检查
- ✅ 不动 `security/impl/*` 任何代码
- ✅ 不引入 `Crypto*` / `Sign*` / `Mask*` / 密钥相关代码
- ✅ XsdValidator 内 LogSanitizer 已有不动
- 无 ⛔ 模式 E Task

### 3. 占位符扫描
- ✅ 0 处 "TBD" / "TODO" / "待" / "后续" / "类似 Task N"
- ✅ 所有代码段含完整可粘贴代码
- ⚠️ Task 4 Step 5 + Task 5 Step 3 "依 Step 1 grep 输出"是合理 deferred — subagent 实施时按 XSD 实际输出补齐，**不是占位符**（Plan 起草无法预测每个 minOccurs=1 字段，按红线 `feedback_plan_must_grep_actual_api` 优先 Subagent 阶段实测）

### 4. 类型一致性
- ✅ 5 测试类名、字段名、方法名与现有一致
- ✅ AckIdempotencyKeys.derive(code, serialNo) 签名与现有一致
- ✅ pad30 helper signature 2 处一致 (Task 3 + Task 4)

### 5. 测试命令可执行
- ✅ 6 处 `./mvnw test -pl fep-web -am -Dtest=...` 与实际测试类名匹配
- ✅ `-Dsurefire.failIfNoSpecifiedTests=false` 含本（红线 `feedback_surefire3_failifno_specified_tests_param_rename`）
- ✅ Task 5 含 -DskipITs=false 的 verify（覆盖 SpotBugs/find-sec-bugs）

### 6. CLAUDE.md 更新
- 本 Plan 不动 src/main/，不变更 CLAUDE.md 中 schema/约束/红线条目
- session-end Phase 5 / Phase 6 会更新 "最近里程碑" + PRD 矩阵小注（红线 `feedback_session_end_prd_matrix_auto_update`），本 Plan 内 Task 6 不重复

### 7. 验收标准完整性
- ✅ Plan 整体 8 项 + 各 Task 验收标准均从 XSD 推导（不从代码推测）
- ✅ 断言值可手算验证（MsgId 20 数字 / CorrMsgId 20 零 / SerialNo length=30 pad 等）

### 8. 共享工具类无遗漏
- pad30 helper 2 处使用（Task 3 + Task 4），按 skill 规则可提取到 common.util — 但本 Plan 不动 src/main/，决策**每测试复制 helper**（fixture 测试代码内联，2 处分别 4 行，可接受 / 提取 common.util 须 src/main + 新生产代码增加 src/main scope 违反范围决策）
- 已在 Task 3 + Task 4 Step 内显式声明 helper

### 9. 核心类职责边界
- 不动任何 ≥3 依赖的 Service
- 不变更 `XsdValidator` / `XsdSchemaRegistry` / `OutboundCfxEnvelopeBuilder` / `BatchMessageProcessorService`
- 无新职责声明

### 10. Worktree 触发条件自检
- [x] 跨 ≥ 3 个 Maven 模块？— NO（仅 fep-web）
- [x] 与已签字未执行的 Plan 并存？— NO（pitest 升级已 merge `df15613` / PR #27 已 push 待 merge / simplify-q-drain 别会话 ownership）
- [x] 涉及 ⛔ 安全 (`security/impl/`) 与 AI (`security/api/`) 并行？— NO
- [x] TLQ `tongtech` profile 联调（与 mock dev 并存）？— NO
- [x] 含 ≥ 5 min long-running verify（全 reactor / IT / E2E），且期间需要继续其他工作？— YES（fep-web 全模块 verify ≥5min + 别会话 callback PR #27 / .e2e smoke / simplify-q-drain 并行）
- [x] muzhou WIP 与 AI 任务并存？— NO
- [x] **附加第 7 项（红线 `feedback_worktree_isolates_fs_not_logic_domain`）**：多会话即触发 — YES（当前活跃 4 个 worktrees: main / .e2e / wt-callback-p2 / wt-simplify-q-drain，本 worktree 加入即第 5 个）

任一命中 → 本 Plan worktree `/Users/muzhou/FEP_v1.0_wt-r-new-1-real-xsd`（已建好），分支 `refactor/r-new-1-real-xsd-validator`，闭环 Task 6 Step 5 含 `git worktree remove` 实测命令 ✅

---

## 执行交接

**⚠️ 重要**: 本 Plan v0.1 起草完成，需经 Plan 评审 + 批准签字。

### 步骤 1: AI 独立评审

派发 general-purpose santa-method agent 评审：
- 输入：本 Plan 全文 + 5-25 P0 修复 commit `f39bf86` + `OutboundEnvelopeXsdComplianceTest` 范本 + `docs/guides/plan-review-checklist.md`
- 输出：✅ 通过项 / ❌ 问题项

### 步骤 2: muzhou 签字

AI 评审 PASS（含 MINOR 修订）后 → muzhou 签字 → 进入实施。

### 步骤 3: subagent-driven 执行

按 6 个 Task 顺序派发 implementer subagent，每 Task 主对话夹 spec review + quality review subagent（红线 `feedback_task_review_discipline`），各 dispatch 不设 model override（红线 `feedback_subagent_model_override_auth_fragility`），不用 background bash（红线 `feedback_subagent_no_background_bash_in_workflow`）。

**muzhou 签字后**: 各 Task commit message footer `Reviewed-By: pending` 在 muzhou 签字 + implementer commit 完成的双时点之间，自动 replace 为 `Reviewed-By: muzhou`（实际实施可在 implementer subagent prompt 内显式指定）。

---

## 签字段

**起草人**: Claude Code (claude-opus-4-7[1m])
**起草日期**: 2026-05-26
**版本**: v0.3

**v0.2 → v0.3 修订记录**（针对 v0.2 增量评审 NEEDS FURTHER REVISION 2 NEW BLOCKER + 1 NEW NIT）:
- NEW BLOCKER 1: Task 5 Step 3 + §grep 落定表 example 全 8 SerialNo SQL 字符串 32 字符 → 30 字符（25 zeros → 23 zeros 模式，2+4+23+1=30 符合 DataType.xsd SerialNo length=30）— 9 处 replace_all
- NEW BLOCKER 2: Task 5 Step 3 BankCheckDay3116 SQL fixture 内 `CheckDetailInfo` 补 6 个 minOccurs=1 字段（rzqyName / rzqyCode / rzAmt Currency / rzRate `[0-9]{1,2}\.[0-9]{4}` 格式 / rzStartDate / rzEndDate）+ §grep 落定表 row 8 同步更新 9 minOccurs=1 字段集
- NEW NIT 1: 补 MINOR 2 disposition（"verbose imports list intentional — precision over brevity"）+ MINOR 3 disposition（"Step 1 必跑 implicit prereq for Step 5，chronological 顺序保证"）

**v0.1 → v0.2 修订记录**（针对 v0.1 santa-method 评审 NEEDS REVISION 2 BLOCKER + 3 MAJOR + 4 MINOR）:
- BLOCKER 1: 全文 §3.1.3 报文标识号 → §3.2.2 报文头 HEAD 数据结构（PRD §3.1.3 实为 TLQ 消息属性 / MsgId 报文标识号 实在 §3.2.2）
- BLOCKER 2: baseline `457a5e4` → `df15613`（pitest-maven 1.25.0 升级别会话已 merge）+ Task 6 Step 1 rebase 强调
- MAJOR-1: §并排除项 追加 `wt-simplify-q-drain` 别会话
- MAJOR-2: 架构段 `dispatcher/BatchMessageProcessorService` → `pipeline/BatchMessageProcessorService` 路径修正
- MAJOR-3: Task 3/4/5 fixture 字段集起草阶段 grep 落定（BODY_3113 补 CreditInfo nested + BODY_3103 CreationRetCode "00000"→"01" Number1to2 修正 + Task 5 P5 SQL 8 body XML 完整字符串落定）
- MINOR 1: dfd6e37 commit 实测存在保留无改
- MINOR 2: Task 5 Step 2 imports 列表 verbose 但精确，保留（v0.2 → v0.3 NEW NIT 1 disposition）
- MINOR 3: Task 6 Step 5 worktree teardown rebase 前提 implicit via Step 1 必跑标签 + chronological 顺序保证（v0.2 → v0.3 NEW NIT 1 disposition）
- MINOR 4: pad30 helper 复制决策已 grep 验证（fep-web/src/test/java 下无 testutil/testsupport/fixture 共享包）
- NIT 1: 技术栈 H2 (test scope) → PostgreSQL (prod) + H2 (dev/test scope)
- NIT 2: §执行交接 §步骤 3 加 muzhou 签字后 Reviewed-By: pending → muzhou 显式说明

**AI 独立评审 v0.1**:
- agentId: `ae013b6bbadc826d3`（general-purpose santa-method）
- 评审日期: 2026-05-26
- 评审结论: NEEDS REVISION (2 BLOCKER + 3 MAJOR + 4 MINOR + 2 NIT，全部 v0.2 修订落实)

**AI 独立评审 v0.2**:
- agentId: `a045e829230d483ec`（general-purpose santa-method 增量评审）
- 评审日期: 2026-05-26
- 评审结论: NEEDS FURTHER REVISION (2 NEW BLOCKER + 1 NEW NIT，全部 v0.3 修订落实)

**AI 独立评审 v0.3**:
- agentId: `a99cd656c6bc3aff5`（general-purpose santa-method 增量评审）
- 评审日期: 2026-05-26
- 评审结论: **APPROVE** — 2 NEW BLOCKER + 1 NEW NIT 全部 fixed (grep + wc -c + facet 验证全 GREEN)，无新引入问题，ready for muzhou sign-off

**Plan Approver**: muzhou
**签字日期**: 2026-05-27
**批准结论**: ✅ **APPROVED** — 执行方式: subagent-driven (red line `feedback_decision_via_askuserquestion` AskUserQuestion 批准)

---

> 本 Plan 受 CLAUDE.md "Plan 治理（2026-04-05 建立）" + "并行开发约束（git worktree 强制，2026-05-06 建立）" 强制，签字前禁止 Task 实施。
