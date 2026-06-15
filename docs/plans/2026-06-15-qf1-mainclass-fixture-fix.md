# Q-F1 → 全量夹具母本值域合规审计

> ⚠️ 范围演进（2026-06-15 muzhou 三次拍板）：原票仅 MainClass/SecondClass=MainA01/SubA0101
> → 预扫发现同类占位 LSDX/FZMD/QYXX/MainB01/YWTB01（muzhou「扩全部 Category A」）
> → 再发现 Period/Type/Status/Result/RetCode/QueryResult/RecordResult/QueryType/ApplyMode/
>   AccReturnCode/CreationRetCode/rzPhaseCode/qyType/qySize 等多字段广泛不合规
> → muzhou「本 PR：全量夹具母本审计」。本 Plan §3.5 起为审计扩展。


> 类型：测试夹具值域合规修复（test-only，0 生产代码变更）
> 模式：A（AI 主导）
> 作者：Claude Code · 起草 2026-06-15
> 批准：muzhou ✅ APPROVED 2026-06-15（「批准，不加 guard」，T3 guard 不实施）
> **执行 Worktree:** `E:\FEP_v1.0_wt-qf1-fixture`（分支 `fix/qf1-mainclass-fixture`，触发条件第 2 项：多会话共享单一工作树，与别会话 #95/#96 并存）

## 1. 背景与问题

§5.8 规则母本 Batch2（PR #81 `8e82f5ec`）为 `MainClass`/`SecondClass` 引入值域规则（ENUM + DEPENDENT_ENUM），源《报文规范 V2.0.0》§5.1.3 表 5.1.3-1（p199）。但历史测试夹具普遍使用**臆造占位** `MainClass=MainA01` / `SecondClass=SubA0101`，二者均**不在母本允许集**：

- 合法 `MainClass ∈ {EAST, COINFO, COAUTH, GYL, MONITOR, STATS, YWTB, ZFJS, SYSTEM, GENERAL}`（application.yml:75）
- `MainA01` / `SubA0101` 均非成员 → 违反母本。

**当前 0 失败**：业务规则引擎在 fep-processor 测试中 registry 为空，且 fep-web 的 dispatcher/wire 测试不调用 `BusinessRuleValidator`。一旦后续**绑定生产 application.yml 的流水线 IT** 出现，这些 -valid 夹具将被母本判违规而引爆（红线 `feedback_rule_master_plan_prescan_fixture_value_domain` 所述 latent 债）。

## 2. 影响范围（grep 实测，2026-06-15）

`MainA01`/`SubA0101` 出现于 **16 文件 / 2 模块**：

**fep-processor（test）— 14 文件**
- 样本 XML（4）：`samples/1001-valid.xml`、`1001-missing-company-name.xml`、`1001-invalid-date.xml`、`2001-valid.xml`
- body roundtrip（4）：`CompanyInfoRequest1001Test`、`CompanyInfoResponse2001Test`、`DataTransferCheckBatchRequest1102Test`、`DataTransferCheckBatchResponse2102Test`、`CompanyInfoBatchRequest1103Test`、`CompanyInfoBatchResponse2103Test`（6）
- XSD validation（4）：`Batch1102XsdValidationTest`、`Batch2102XsdValidationTest`、`Batch1103XsdValidationTest`、`Batch2103XsdValidationTest`

**fep-web（test）— 2 文件**
- `messageinbound/service/InboundMessageDispatcherTest`
- `messageinbound/listener/InboundListenerWireTest`

无 SQL seed 命中。

## 3. 修复方案

统一替换：`MainA01` → `GYL`，`SubA0101` → `HX01`。

**选值依据（禁臆造，逐条核实）**
- `GYL` 是母本 10 大类合法成员（application.yml:75）。
- `HX01 ∈ GYL` 的 SecondClass 允许集（application.yml:86 `GYL: [HX01, HX02, HX03, HX04, ...]`）→ 同时满足 DEPENDENT_ENUM 成对约束。
- XSD 合规：`DataType.xsd` `MainClass`/`SecondClass` 均 `Token` `minLength=2`/`maxLength=16`；`GYL`(3)/`HX01`(4) 满足（红线要求改夹具值连带核 XSD facet ✓）。
- `GYL`/`HX01` 已是 `RuleMasterMainSecondClassBatchTest` 的 canonical 合法对 → 与现有母本测试一致。

**负向测试不受损**：`1001-invalid-date`（坏 BeginDate）、`1001-missing-company-name`（缺名）的被测缺陷与 MainClass 无关；roundtrip 断言（`.contains(...)`/`.isEqualTo(...)`）随同文件内字面量同步替换，保持自洽。

## 4. Task 拆分（每 Task 独立 spec + quality review）

| Task | 模块 | 内容 | 回归 |
|------|------|------|------|
| T1 | fep-processor | 14 文件 `MainA01→GYL` / `SubA0101→HX01`（XML + Java 字面量含断言） | `mvnw -pl fep-processor -o test` |
| T2 | fep-web | 2 文件同替换 | `mvnw -pl fep-web -o test` |

**Commit 自洽**（红线 `feedback_commit_tree_self_consistent_per_commit`）：T1/T2 各一 commit，按模块切分，每 commit 独立可编译（纯 test-only 字面量替换，无签名/接口变更）。

## 5. 验收

- T1：fep-processor 全模块 test 0 fail（基线 636 tests）。
- T2：fep-web 全模块 test 0 fail。
- `grep -rn "MainA01\|SubA0101" src/` 命中 = 0（含 XML + Java + 注释）。
- PR + GHA Build/Test/Quality 全绿为权威背书（本机 load 高时降级 GHA strong）。

## 6. 不做（明确边界）

- 不新增 production 代码 / 不改母本 yml。
- 不新增冗余 guard 测试（母本拒绝非法 MainClass 已由 `RuleMasterMainClass1001Test`/`RuleMasterMainSecondClassBatchTest` 覆盖）——如 muzhou 要求绑生产 yml 的 canonical-pair guard，可加 T3（fep-web）。
- 不触碰别会话 WIP（#95 credential / #96 tracking-tables）。

## 3.5 全量审计结果（muzhou「本 PR 全量夹具母本审计」）

逐字段对照生产 application.yml 母本（§5.8）扫描两模块全部正向夹具，母本-illegal 值
按母本映射规范化（保留全部负向/边界测试）。

**值映射（母本-cited）**
- MainClass/SecondClass：MainA01/SubA0101、MainB01/SubB0101 → GYL/HX01；LSDX/LSDX01 → STATS/LSD01（STATS 允许 LSD01）；FZMD/FZMD01 → ZFJS/FZBMD；QYXX/QYXX01 → COINFO/I1001；YWTB01 → ZHYW（YWTB 仅允许 ZHYW）
- Period（1101/2101/1102/2102，允许 1-7,99）：01 → 1
- Type（1101/2101，允许 1,2,3）：01/99 → 1
- Status（1102/2102 允许 1-6；9007/9009 允许 1,2,3,99）：01 → 1；NodeStatus 0/01 → 1，2-char 边界场景 → 99（母本-legal 且保留 maxLength=2 演示意图）
- Result/RetCode（52 码）：10000/20000/00/01 → 90000（多项保持区分用 90001）；99999（错误回执）→ 29999
- QueryType（3001/3002 允许 1,2）：01/02 → 1/2
- ApplyMode（3000 允许 1,2）：01/02 → 1/2
- AccReturnCode（3006 允许 0,1,2,3,4,9）：00/01 → 0/1
- CreationRetCode（3103 允许 11,21,22,23,91,92,93,99）：0/1 → 11
- QueryResult（2103 允许 90000,10001-10008,19999）：99999 → 90000
- RecordResult（2104）：99999 → 90000
- qyType（3102 允许 1-6）：01 → 1；qySize（3102 允许 SC00-03）：0001 → SC00

**保留的负向/边界/合成测试（未改）**
- XSD facet 负例：MainClass `L`（minLength=2）、SecondClass `LSDX0123456789ABZ`（maxLength=16）、QueryResult `0000`（length=5）
- 母本拒绝负例：RuleTypesTest `BAD`/`anything`(GENERAL 不约束)、RuleMaster*Test `BAD`/`12345`、BusinessRuleValidatorTest 合成引擎 `Status=9`（内联规则非生产母本）
- 文档化 XSD 边界：OutboundEnvelopeXsdComplianceTest `EA`/`EA01`（Javadoc 明示 minLength=2）
- samples/invalid/ 整目录负例（缺必填字段，码值附带）

**覆盖文件**：fep-processor 42 文件（body realtime/batch/supplychain/common + validation XSD + samples）+ fep-web ~6 文件（dispatcher/wire/listener）。每次替换用全限定字符串（含 `<tag>`/setter/getter 上下文）规避子串碰撞与 DEPENDENT_ENUM 配对错配；逐文件 git diff + 残留 grep + 全模块回归核验。

## 7. PRD 追溯

FR-WEB-AUDIT（§5.8 业务校验规则引擎）配套夹具合规；规范源 §5.1.3 表 5.1.3-1（p199）。
