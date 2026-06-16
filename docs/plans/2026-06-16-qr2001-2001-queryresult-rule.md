# Plan: 2001 母本补全 QueryResult ENUM 规则（表 5.1.6-1）

> 起草: 2026-06-16 · Claude Code (mode A 起草)
> 追溯: FR-WEB-AUDIT · 报文规范 表 5.1.6-1 企业信息查询结果代码（p201）
> 开发模式: **mode C**（人定规则集/取值域，AI 编码）
> 状态: santa Round 1 PASS ✅ → **muzhou 签字批准 2026-06-16 ✅** → 实施中

**执行 Worktree:** `E:\FEP_v1.0_wt-qr2001`（分支 `feat/qr2001-queryresult-rule`，触发条件第 2 项「与已签字未执行 Plan / 多活跃别会话 worktree 并存」+ 多会话共用单一工作树）

---

## 1. 背景与问题（实测取证）

DEF-B2-2 池母本完整性复核（2026-06-16 会话）实测确认一个**母本不对称缺口**：

- 母本 `fep-web/src/main/resources/application.yml`：`2001` 规则块（L257-267）**仅** `field: Result`（52 码表）；其结构对等的批量回执 `2103`（L336-367）同时校验 `Result`(52码) **与** `QueryResult`(10码·表5.1.6-1)。
- `2001.xsd` L78：`CompanyInfoResponse2001/QueryResult` 类型 `Result`（字符串 simpleType，**非枚举** → XSD 仅约束 5 字符串形状，不约束码表）。`2103.xsd` 同名 `<QueryResult>` 元素。全 46 XSD 仅此两报文带 `<QueryResult>` 元素。
- 后果：2001 body 的 `<QueryResult>` **当前不受业务规则码表约束**，码表外垃圾值（如 `20000`）不被拦截，而结构等价的 2103 会拦。
- 根因：Batch2 Plan（`2026-06-11-rule-master-batch2-engine-ext-cooccurrence.md`）Task 9 设计表（L824）只登记「2103 | QueryResult」，而同 Plan Task 8 已把 2001/2103 当 analog 并列处理（L786）→ Task 9 scoping 漏了 2001。

**注：衔接提示词原关切「2001 `Result` 规则可能非绑定」为误报** —— `Result` 绑定到响应业务头 `RealHead2001/Result`（`2001-valid.xml` L19 实证 `<Result>90000</Result>`），经 `RuleContext` 命名空间无关扁平化命中，按 52 码表正确校验，**非 no-op**。本 Plan 修复的是**相邻的真实缺口**：缺 `QueryResult` 规则。

## 2. 取值域（逐字来自表 5.1.6-1 p201，禁臆造）

`QueryResult ∈ {90000, 10001, 10002, 10003, 10004, 10005, 10006, 10007, 10008, 19999}`（共 10 码；规范 10002 重复笔误已去重，与 2103 既有母本 L367 **完全同源同值**）。

## 3. Fixture 值域 prescan（红线 `feedback_rule_master_plan_prescan_fixture_value_domain` 强制）

`grep -rn QueryResult **/src/test/**` 全取值入口（元素/setter/断言）逐条对码表标定：

| 位置 | 值 | 报文 | 路径性质 | 过规则引擎? | 判定 |
|---|---|---|---|---|---|
| `2001-valid.xml:26` | `90000` | **2001** | envelope sample，**被 `SyncMessageProcessorServiceIntegrationTest` 全流水线消费** | **是（真规则路径）** | ✓ 合规（关键正样本） |
| `CompanyInfoResponse2001Test:25` | `90000` | 2001 | JAXB roundtrip | 否 | ✓ 合规 |
| `CompanyInfoResponse2001Test:59` | `00001` | 2001 | JAXB roundtrip (minimal) | 否 | ⚠️ 非码表，但纯 marshal 不过规则引擎 → **无害，不改** |
| `CompanyInfoResponse2001XsdValidationTest:52/70` | `90000` | 2001 | XSD 校验测试 | 否 | ✓ |
| `CompanyInfoResponse2001XsdValidationTest:165-166` | `90000` / `0000` | 2001 | XSD length facet 负向 | 否 | ✓ / 故意 4 字符违 XSD facet |
| `InboundMessageDispatcherTest:240` | `90000` | **2103** | 派发链（mock `SyncMessageProcessorService`） | 否（mock + 非 2001） | ✓ 与本 Plan 无关 |
| `InboundListenerWireTest:172` | `90000` | **2103** | wire 测试（mock 处理器） | 否（mock + 非 2001） | ✓ 与本 Plan 无关 |
| `RuleMasterBatchTransferCodesTest:48-49` | `10008/90000`(legal) `20000/00000`(illegal) | 2103 | 规则测试 | 是 | ✓ / 故意非法 ✓ |

**结论：唯一流经 `BusinessRuleValidator` 的 2001 envelope fixture = `2001-valid.xml`（经 `SyncMessageProcessorServiceIntegrationTest` 全流水线），取值 `90000`（合规）→ 加规则后无既有测试会破，无 fixture 需修复。** `InboundMessageDispatcherTest:240`/`InboundListenerWireTest:172` 经 santa 复核实为 **2103** fixture 且 mock 了处理器，与本 Plan 无关（原行误标 2001/「视测试而定」已订正）。唯一非码表值 `00001`（`CompanyInfoResponse2001Test:59`）在纯 JAXB body roundtrip 测试，不构造 envelope、不过规则引擎，无害，本 Plan 不改（改动反而可能扰动其 minimal-body marshal 意图）。

## 4. Task 1：2001 母本补 QueryResult ENUM 规则 `模式 C`

**PRD 依据:** 报文规范 表 5.1.6-1（p201）+ 2001 字段表（`CompanyInfoResponse2001/QueryResult`）
**追溯 ID:** FR-WEB-AUDIT
**文件:**

| 文件 | 改动 | 模式 |
|---|---|---|
| `fep-web/src/main/resources/application.yml` | `2001` 规则块追加 `QueryResult` ENUM（镜像 2103 L365-367） | C |
| `fep-web/src/test/java/com/puchain/fep/web/validation/RuleMasterBatchTransferCodesTest.java` | `queryResult_table5161_shouldEnforce` 增 2001 两行断言 | C |

**TDD 步骤:**

- [ ] **Step 1: 写失败测试** — 在既有 `queryResult_table5161_shouldEnforce`（L45-50）追加（镜像 2103 模式，body 元素名 `CompanyInfoResponse2001` 已经 `2001.xsd` L43/L78 grep 实测）：
  ```java
  RuleMasterTestSupport.assertRule("2001", "CompanyInfoResponse2001", "QueryResult", "10008", "20000");
  RuleMasterTestSupport.assertRule("2001", "CompanyInfoResponse2001", "QueryResult", "90000", "00000");
  ```
- [ ] **Step 2: 确认失败** — 母本未加规则时，illegal 值 `20000`/`00000` 被判 `valid()` → 断言 `isFalse()` 失败（RED）。`-pl fep-web -o test -Dtest=RuleMasterBatchTransferCodesTest`。
- [ ] **Step 3: 母本 yaml** — `application.yml` 中 `"2001":` 块（L257 起）在既有 `Result` ENUM 之后追加（与 2103 L365-367 逐字同源；缩进对齐 `"2001":` 下规则列表）：
  ```yaml
        # QueryResult：表 5.1.6-1 企业信息查询结果代码（p201；规范 10002 重复笔误已去重；与 2103 同源）
        - type: ENUM
          field: QueryResult
          allowed: ["90000", "10001", "10002", "10003", "10004", "10005", "10006", "10007", "10008", "19999"]
  ```
- [ ] **Step 4: 测试转绿** — 重跑 Step 2 命令，GREEN。
- [ ] **Step 5: 9 项质量自检** — Javadoc/无吞异常/边界（缺失通过已由 ENUM 语义保证）/无硬编码（值集即码表，合理）/风格一致（镜像 2103 既有条目）。
- [ ] **Step 6: spotbugs + ArchUnit**（本 Task 无新 Java 生产类，仅 yml + 测试断言 → spotbugs 面为既有；仍跑 `-pl fep-web -o spotbugs:check` 确认 BugInstance 0 不回退）。
- [ ] **Step 7: commit** — `AI-Generated: claude-code` + `Reviewed-By: pending`，独立 commit（不与验证命令链式，红线 `commit_no_chain_with_verify_command`）。

## 5. 回归验收（红线 `plan_regression_scope_explicit` 两层）

- **minimum（本机）**: `cd E:\FEP_v1.0_wt-qr2001` →（上游 fep-processor/converter jar 若 .m2 缺先一次性 `-pl fep-web -am install -DskipTests`）→ `mvnw -pl fep-web -o test`（**不带 -am**，红线 `single_module_regression_no_am_flag`；本 Task 不改上游）。期望 fep-web 全测试 0 fail（基线 1240/0 → 不增测试方法，仅 +2 断言行，计数不变）。本机 load>100 立即 `pkill` 自己 worktree-slug 匹配的 fork。
- **strong（GHA）**: PR 触发 Build/Test/Quality（Checkstyle/SpotBugs/JaCoCo/ArchUnit）全绿背书。

## 6. 影响面与风险

- **生产行为变更**: 2001 报文经 Sync 流水线时，`<QueryResult>` 码表外值将被 `BusinessRuleValidator` 拦截（`PROC_8507`），与 2103 对齐。这是**补强**（修复漏校验），非破坏。
- **向后兼容**: 合法报文 `QueryResult=90000`（及其余 9 码）不受影响；缺失 `QueryResult`（body minOccurs=0）→ ENUM 缺失语义放行。
- **风险等级**: 低（3 行 yml + 2 行测试，纯增量，无 fixture 需修复，prescan 已证 envelope fixture 全合规）。

## 7. Worktree 闭环（Plan T-closing）

- [ ] PR 合并后（GHA 全绿）：`git worktree remove E:\FEP_v1.0_wt-qr2001` + 删远端分支
- [ ] 主 worktree `git fetch && git log main..origin/main` 确认 ff
- [ ] session-end 四步收尾

## 变更历史

| 版本 | 日期 | 说明 |
|---|---|---|
| v0.1 | 2026-06-16 | 初稿。DEF-B2-2 池母本复核发现 2001 缺 QueryResult 规则（vs 2103 不对称）；fixture prescan 全合规无需修复；单 Task 镜像 2103。 |
| v0.2 | 2026-06-16 | santa Round 1 **PASS**。落 1 MINOR（prescan 表订正）：`InboundMessageDispatcherTest:240`/`InboundListenerWireTest:172` 实为 2103 fixture 且 mock 处理器（原误标 2001/「视测试而定」）；补充关键正样本 `2001-valid.xml` 经 `SyncMessageProcessorServiceIntegrationTest` 全流水线流经 `BusinessRuleValidator`（值 90000 合规）→ 进一步实证无回归。 |
