# FEP Collector Mapper Mode3 Boil-Lake 实施计划

> **执行方式:** 使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐 Task 执行。步骤使用 `- [ ]` 复选框跟踪。

**目标:** 抽取 `AbstractFieldMapper` 共用基类（boil-lake refactor 消除 3101/3102 helper 8-份重复），补 3101/3102 dedicated unit test gap，实装 Mode3 数仓模式 3 个 stub mapper（3109 企业信息登记 / 3116 银行资金日对账 / 3009 电子凭证融资结果登记），每 mapper 配真 XsdValidator on SUT 集成 case。

**前置依赖:**
- P4-MSG-K (`415771d`, 2026-05-26 inbound 3105/3009/3103/3113 受理批处理) — 已 ship origin/main
- R-NEW-1 真 XsdValidator (`4fcac99`, 2026-05-28 别会话) — 已 ship origin/main
- spec doc `/Users/muzhou/FEP/docs/superpowers/specs/2026-05-28-collector-mapper-boil-lake-design.md`（2026-05-28 muzhou ✅ 拍板）

**Baseline:** `origin/main` = `3300533` (2026-05-28 重 grep 实测，v0.2 修订 — 别会话 ship `XsdTestSupport.pad30` helper 暴露 SerialNo XSD length=30 latent bug；红线 `feedback_baseline_drift_during_long_review_cycle` 触发，本 Plan v0.2 一并修复 mapper 侧 uuid32 32 字符兜底 → 30 字符)
**版本:** v0.4（2026-05-28 — v0.3 AI 增量评审揪出 1 v0.3 FAIL (2 处 "5 helper" 残留) + 1 NEW MINOR (Step 4 + Step 2 模板 static call 残留)，本版本一并清理 4 处残留）

**版本历史:**
- v0.1 (2026-05-28): 初稿 (6 Task)
- v0.2 (2026-05-28): 修订 v0.1 评审 2 MAJOR + 4 MINOR + baseline drift MAJOR #3 (新增 Task A7, 7 Task)
- v0.3 (2026-05-28): 修订 v0.2 评审 1 NEW MAJOR (XsdValidator 包路径错) + 2 NEW MINOR (helper 计数 5→6 / static/instance 设计 bug)
- v0.4 (2026-05-28): 修订 v0.3 评审 1 FAIL (2 处 "5 helper" 残留 line 623+2755) + 1 NEW MINOR (XsdComplianceHelper static call 模板残留 line 2569 Step 2 + line 2662 Step 4) — 4 处清理

**执行 Worktree:** main（无需独立 worktree，触发条件均不命中）

> 红线 `feedback_worktree_for_parallel_work` + `feedback_worktree_isolates_fs_not_logic_domain` 6+1 触发条件实测:
> - ❌ 跨 ≥3 Maven 模块（仅 fep-collector 内改）
> - ❌ 与已签字未执行 Plan 并存（PR #27 callback Phase 2 + PR #29 Simplify Q drain 已 ship 等 GHA billing，非"未执行 Plan"）
> - ❌ ⛔ 安全 vs AI 并行（无 security 改动）
> - ❌ TLQ tongtech profile 联调
> - ❌ ≥5min long-running verify（fep-collector 单模块 verify ~2-3 min）
> - ❌ muzhou WIP 与 AI 并存
> - ❌ 多会话活跃（单会话）
>
> 命中 CLAUDE.md "例外"条款：小改动 + 单模块 + 串行可控 → 普通 feature branch + stash 即可。
>
> 分支命名: `refactor/collector-mapper-mode3-boil-lake`（含 helper 抽取 = refactor 性质）

**架构:** `fep-collector` 模块内 `assembler.mapper` 包新增抽象基类 `AbstractFieldMapper`，提供 **6** 个 helper（`requireInstitutionCode` / `requireString` / `requireBooleanString` / `applyOptional` / `optString` / **`serialNoOrFallback`** 新增 v0.2 — XSD length=30 兼容兜底）。现有 2 个 mapper（3101/3102）重构 extends，新增 3 个 mapper（3109/3116/3009）implements。Body POJO 嵌套复杂字段（XSD complex type）暂留 Javadoc stub（除 3116 CheckDetailInfo 必填嵌套已实装）。**v0.2 跨 2 模块**：fep-collector 主体 + fep-web 新增 5 个 XsdComplianceTest（Task A7），真 `XsdValidator` on SUT 验证 mapper 产出全 XSD 合规。

**技术栈:** Java 17 / Spring Boot 3.x / Maven / JUnit 5 / AssertJ / Mockito (fep-collector 模块自有依赖，无新增 Maven dependency)

**AI 协同模式:**

| 模式 | AI 占比 | 本计划中的应用 |
|:----:|:-------:|--------------|
| A | 90% | 补 dedicated test + helper 抽取 + closing 文档 |
| B | 70% | 3 mapper 业务逻辑实装 |
| C | 60% | （本 Plan 无） |
| D | 50% | （本 Plan 无） |
| E | 0%  | ⛔（本 Plan 无安全代码） |

---

## 设计背景

源自 spec `/Users/muzhou/FEP/docs/superpowers/specs/2026-05-28-collector-mapper-boil-lake-design.md`（muzhou 2026-05-28 ✅ 拍板）。完整 brainstorming 决策链：

1. **Scope** — muzhou 2026-05-28 AskUserQuestion ✅ Recommended "6 全做"（spec §3.1 → 拆 Plan A Mode3 3 mapper + Plan B Mode2 3 mapper，本 Plan = Plan A）
2. **Approach** — ✅ Recommended "Approach 2: Boil-lake refactor first"（spec §1.2 命中新红线 `feedback_concern_boil_lake_when_cheap_and_safe`）
3. **Helper style** — ✅ Recommended "Option (a) AbstractFieldMapper abstract base class"（参考 `AbstractAck9120InboundListener` 2026-05-26 `415771d` 同模式）
4. **PR 大小** — ✅ Recommended "muzhou 签字豁免两 PR 超 400 行"（mapper 骨架重复 80%+，拆碎反而难审）

### 关键发现

- 现状 `ContractInfo3101FieldMapper` (206 行) + `ArchiveInfo3102FieldMapper` (134 行) 已实装；6 stub 全 31 行抛 `UnsupportedOperationException`
- 3101/3102 helper 完全复制粘贴（`requireString`/`applyOptional`/`optString`/`requireInstitutionCode` 字面一致，38 处调用），构成 Rule-of-Three 触发点
- 3101/3102 **缺 dedicated unit test**（仅 `DefaultPayloadAssemblerTest` 集成测试覆盖）— boil-lake refactor 前必须先补
- 6 报文 PRD `FR-MSG-3xxx` 矩阵当前声明 ✅ "outbound wire 已完成"，但底层 6 stub mapper 未实装 → **矩阵 status drift**（红线 `feedback_prd_matrix_status_drift` 触发，闭环时 Task A6 修正）

---

## 文件结构

| 文件路径 | 职责 | 操作 | AI 模式 |
|----------|------|------|:-------:|
| `fep-collector/src/test/java/com/puchain/fep/collector/assembler/mapper/ContractInfo3101FieldMapperTest.java` | 3101 dedicated unit test（Task A1） | 新建 | A |
| `fep-collector/src/test/java/com/puchain/fep/collector/assembler/mapper/ArchiveInfo3102FieldMapperTest.java` | 3102 dedicated unit test（Task A1） | 新建 | A |
| `fep-collector/src/main/java/com/puchain/fep/collector/assembler/mapper/AbstractFieldMapper.java` | 共用基类，提供 6 helper（v0.2: requireInstitutionCode/requireString/requireBooleanString/applyOptional/optString + serialNoOrFallback）（Task A2） | 新建 | A |
| `fep-collector/src/test/java/com/puchain/fep/collector/assembler/mapper/AbstractFieldMapperTest.java` | 基类 helper 共用逻辑单元测试（Task A2） | 新建 | A |
| `fep-collector/src/main/java/com/puchain/fep/collector/assembler/mapper/ContractInfo3101FieldMapper.java` | 重构 extends AbstractFieldMapper（Task A2） | 修改 | A |
| `fep-collector/src/main/java/com/puchain/fep/collector/assembler/mapper/ArchiveInfo3102FieldMapper.java` | 重构 extends AbstractFieldMapper（Task A2） | 修改 | A |
| `fep-collector/src/main/java/com/puchain/fep/collector/assembler/mapper/QyRegister3109FieldMapper.java` | 3109 mapper 实装（Task A3） | 修改 | B |
| `fep-collector/src/test/java/com/puchain/fep/collector/assembler/mapper/QyRegister3109FieldMapperTest.java` | 3109 unit test | 新建 | B |
| `fep-collector/src/main/java/com/puchain/fep/collector/assembler/mapper/BankCheckDay3116FieldMapper.java` | 3116 mapper 实装（Task A4） | 修改 | B |
| `fep-collector/src/test/java/com/puchain/fep/collector/assembler/mapper/BankCheckDay3116FieldMapperTest.java` | 3116 unit test | 新建 | B |
| `fep-collector/src/main/java/com/puchain/fep/collector/assembler/mapper/RzReturnInfo3009FieldMapper.java` | 3009 mapper 实装（Task A5） | 修改 | B |
| `fep-collector/src/test/java/com/puchain/fep/collector/assembler/mapper/RzReturnInfo3009FieldMapperTest.java` | 3009 unit test | 新建 | B |
| `fep-collector/src/test/java/com/puchain/fep/collector/assembler/DefaultPayloadAssemblerTest.java` | A2 Step 7 同步 stub bean 注册（props 注入）+ delete deferredMapper3109 测试；A6 扩展 3 happy path 集成 case | 修改 | A |
| `fep-web/src/test/java/com/puchain/fep/web/collector/mapper/XsdComplianceHelper.java` | Task A7 共享 JAXB+XsdValidator helper | 新建 | A |
| `fep-web/src/test/java/com/puchain/fep/web/collector/mapper/ContractInfo3101XsdComplianceTest.java` | Task A7: 3101 真 XsdValidator on SUT 集成 case | 新建 | A |
| `fep-web/src/test/java/com/puchain/fep/web/collector/mapper/ArchiveInfo3102XsdComplianceTest.java` | Task A7: 3102 真 XsdValidator on SUT 集成 case | 新建 | A |
| `fep-web/src/test/java/com/puchain/fep/web/collector/mapper/QyRegister3109XsdComplianceTest.java` | Task A7: 3109 XSD 合规集成 case | 新建 | A |
| `fep-web/src/test/java/com/puchain/fep/web/collector/mapper/BankCheckDay3116XsdComplianceTest.java` | Task A7: 3116 XSD 合规集成 case | 新建 | A |
| `fep-web/src/test/java/com/puchain/fep/web/collector/mapper/RzReturnInfo3009XsdComplianceTest.java` | Task A7: 3009 XSD 合规集成 case | 新建 | A |
| `docs/plans/prd-traceability-matrix.md` | 修正 FR-MSG-3009/3109/3116 注释扩展 + 新增 FR-COMM-COLLECTOR-MAPPER-HELPER（Task A6） | 修改 | A |
| `docs/plans/PHASE_HISTORY.md` | 新增 Plan A 行（Task A6） | 修改 | A |
| `CLAUDE.md` | "当前项目状态" 段更新（Task A6） | 修改 | A |

### 共享工具类清单

| 工具类 | 包 | 关键方法 | 提供者 Task | 消费者 Task |
|---|---|---|---|---|
| `AbstractFieldMapper` (新) | `com.puchain.fep.collector.assembler.mapper` | **6** helper: `requireInstitutionCode()` / `requireString()` / `requireBooleanString()` / `applyOptional()` / `optString()` (protected) / **`serialNoOrFallback()`** (v0.2 新增, XSD length=30) | Task A2 | Task A2 (3101/3102 refactor) / A3 (3109) / A4 (3116) / A5 (3009) |
| `IdGenerator` (复用) | `com.puchain.fep.common.util` | `uuid32()` (内部被 AbstractFieldMapper.serialNoOrFallback() 截断到 30 字符使用) | 既有 | Task A2 helper 内部调用 |
| `LogSanitizer` (复用) | `com.puchain.fep.common.util` | `sanitize(String)` | 既有 | `AbstractFieldMapper` 内部异常 message wrap |
| `FepConstants` (复用) | `com.puchain.fep.common.util` | `HNDEMP_NODE_CODE = "A1000143000104"` | 既有 | `AbstractFieldMapper.DES_NODE_CODE_HNDEMP_CENTER` 引用 |
| `FepErrorCode` (复用) | `com.puchain.fep.common.domain` | `COLLECT_ASSEMBLE_FAILURE` ("COLLECT_5002") | 既有 | `AbstractFieldMapper` 内部异常 |
| `XsdValidator` (复用，跨模块) | `com.puchain.fep.processor.validation` (v0.3 grep 实测，**fep-processor 模块**，非 fep-converter) | `validate(MessageType, byte[]) → ValidationResult` | 既有 (fep-processor) | Task A7 (XsdComplianceHelper 内部调用) |
| `MessageType` (复用，跨模块) | `com.puchain.fep.converter.type` (fep-converter) | `byMsgNo(String) → Optional<MessageType>` | 既有 | Task A7 helper |
| `ValidationResult` (复用) | `com.puchain.fep.processor.validation` | record: `valid: boolean, errors: List<String>` | 既有 | Task A7 helper |
| `XsdComplianceHelper` (新, v0.2) | `com.puchain.fep.web.collector.mapper` (test scope) | `validateMapperOutput(msgNo, body)` | Task A7 | Task A7 5 个 XsdComplianceTest |

### 核心类职责边界声明

#### AbstractFieldMapper 职责边界

**负责** (v0.2 update — 6 helper):
- 6 个共用 helper：必填 String / 必填 Boolean / 可选 String / institutionCode 14 位校验 / utf8 raw read / **SerialNo 30 字符兜底**（v0.2 新增）
- 异常 message 统一 wrap：`"missing required field for <msgNo>: <field>"` + `LogSanitizer.sanitize(field)`
- 子类共享常量：`DES_NODE_CODE_HNDEMP_CENTER` / `XSD_BOOLEAN_FALSE` / `XSD_BOOLEAN_TRUE`
- `serialNoOrFallback()` SerialNo XSD length=30 兼容兜底（v0.2 新增，修 R-NEW-1 + XsdTestSupport.pad30 暴露的 latent bug）

**不负责**:
- XSD facet 校验（length/pattern/enum） → 下游 P5+ 队列消费侧 `XsdValidator`（**例外**: SerialNo length=30 在 helper 内做兜底，是 XSD 合规的最小必要)
- raw data 编码转换 → `CollectorAdapter` 上游已统一 UTF-8
- 业务规则校验（如 contractNo 格式） → mapper 仅做"存在性"必填校验
- 嵌套 complex 字段映射（如 `HxqyInfo3109` / `RzAmtInfo3009`） → 由后续业务深化 Plan 补
- 真 XSD on SUT 验证 → Task A7 XsdComplianceHelper / XsdValidator 在 fep-web 模块负责

**依赖上限**: 6 个（`CollectorProperties`, `FepConstants`, `FepErrorCode`, `FepBusinessException`, `LogSanitizer`, `IdGenerator`，仍符合 ArchUnit 7 上限）

**行数上限**: 250 行（含 Javadoc + `serialNoOrFallback`，v0.2 估算 ~220 行）

**如果超出**:
- helper 方法 > 7 → 拆 `RawFieldExtractor` (静态工具) + `AbstractFieldMapper` (基类骨架) 二层
- 行数 > 300 → 把 Boolean 处理拆 `BooleanFieldHelper` 独立类

---

## PRD 追溯

| FR-ID | PRD 章节 | 需求描述 | 目标 Task | 状态 |
|---|---|---|---|---|
| `FR-COMM-COLLECTOR-MAPPER-HELPER`（**新增**） | §2.2.1 / §2.2.2 双模式 mapper 共用基础 | fep-collector FieldMapper 共用基类抽取（boil-lake） | Task A2 | ⏸ pending |
| `FR-MSG-3009-COLLECTOR-MAPPER`（**新增子项**） | §4 §6.2 line 791 + §841 模式 3 信息发送 | 3009 融资结果登记 raw → Body POJO 字段映射 | Task A5 | ⏸ pending |
| `FR-MSG-3109-COLLECTOR-MAPPER`（**新增子项**） | §4 §6.2 line 798 + §841 模式 3 + §2.2.3 ✅ 数仓推荐 | 3109 企业信息登记 raw → Body POJO 字段映射 | Task A3 | ⏸ pending |
| `FR-MSG-3116-COLLECTOR-MAPPER`（**新增子项**） | §4 §6.2 line 802 + §841 模式 3 + §2.2.3 ✅ 日终对账 | 3116 银行资金日对账 raw → Body POJO 字段映射 | Task A4 | ⏸ pending |
| `FR-MSG-3101`（**修正注释**） | §4 §6.2 line 794 | 补 ContractInfo3101 dedicated unit test + extends AbstractFieldMapper refactor | Task A1+A2 | 现 ✅ outbound wire → 注释扩展 "+ dedicated unit test + base class refactor" |
| `FR-MSG-3102`（**修正注释**） | §4 §6.2 line 795 | 补 ArchiveInfo3102 dedicated unit test + extends AbstractFieldMapper refactor | Task A1+A2 | 现 ✅ outbound wire → 注释扩展 "+ dedicated unit test + base class refactor" |

> ⚠️ **PRD 矩阵 status drift 发现**（Task A6 闭环修正）：FR-MSG-3009/3105/3107/3109/3112/3116 当前矩阵注释声明 ✅ "outbound wire (build→sign→send→SENT)"，未涵盖 6 stub mapper UnsupportedOperationException 缺口。本 Plan A merge 后 Task A6 必须修正注释扩展含 collector mapper 字段映射状态。Plan B 完成后做 FR-MSG-3105/3107/3112 同步修正。

---

## Task A1: 补 3101/3102 dedicated unit test `模式 A`

**PRD 依据:** v1.3 §4 报文接入 + §6.2 报文类型清单 line 794 (3101) / line 795 (3102)
**追溯 ID:** FR-MSG-3101 (注释扩展) + FR-MSG-3102 (注释扩展)

**目标:** 在 boil-lake refactor (Task A2) 前补齐 3101/3102 dedicated unit test safety net。当前 2 mapper 仅 `DefaultPayloadAssemblerTest` 集成测试覆盖，无独立单元测试 → Task A2 重构 extends `AbstractFieldMapper` 后微回归风险无法用单元测试预警。Task A1 先把 mapper 行为冻结为 dedicated unit test，作为 A2 重构的 invariant 守护。

**验收标准（从 3101/3102 mapper 现有实装行为 + spec §5.1 4 类用例推导）:**

1. **3101**: ~14 个测试用例全 GREEN（happy path × 1 + 6 必填缺失 × 6 + 5 可选缺失 × 1 + boolean 边界 × 4 + institutionCode 异常 × 2）
2. **3102**: ~10 个测试用例全 GREEN（happy path × 1 + 5 必填缺失 × 5 + 4 可选缺失 × 1 + institutionCode 异常 × 2）
3. **TDD inverse**: 因 mapper 已实装，Task A1 写测试**应自动通过**；任何 RED 立即停下排查（说明 mapper 行为偏离 PRD/spec 期望，须 muzhou 拍板修哪一边）
4. **断言深度**: 必填缺失 case 必须 `assertThatThrownBy + isInstanceOf(FepBusinessException) + hasMessageContaining("missing required field for 3101:")` + `hasMessageContaining("<fieldName>")` 双重断言（不止 `isInstanceOf`）
5. **fixture 边界**: institutionCode `"A1000143000999"` (14 位测试值)、合同号 `"CON202611280001"`、digitalSeal `"1"` / `Boolean.TRUE` / `"true"` / `"TRUE"` 共 4 真值字面 + `"0"` / `Boolean.FALSE` / `"false"` / `"FALSE"` 共 4 假值字面 + `"yes"` 1 个非法字面

**Files:**
- Create: `fep-collector/src/test/java/com/puchain/fep/collector/assembler/mapper/ContractInfo3101FieldMapperTest.java`
- Create: `fep-collector/src/test/java/com/puchain/fep/collector/assembler/mapper/ArchiveInfo3102FieldMapperTest.java`
- (No production code changes in Task A1)

- [ ] **Step 1: 创建测试包路径**

```bash
cd /Users/muzhou/FEP_v1.0
mkdir -p fep-collector/src/test/java/com/puchain/fep/collector/assembler/mapper/
```

- [ ] **Step 2: 写 3101 测试文件**

```java
// fep-collector/src/test/java/com/puchain/fep/collector/assembler/mapper/ContractInfo3101FieldMapperTest.java
package com.puchain.fep.collector.assembler.mapper;

import com.puchain.fep.collector.CollectorProperties;
import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.FepConstants;
import com.puchain.fep.processor.body.supplychain.ContractInfo3101;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Task A1: ContractInfo3101FieldMapper dedicated unit test
 * （Plan 2026-05-28-collector-mapper-mode3-boil-lake §A1 safety net for Task A2 refactor）。
 *
 * <p>测试覆盖：happy path / 6 必填缺失 / 6 可选 / 4+4 boolean 字面 / 1 非法 boolean / institutionCode 2 异常
 *
 * @since 1.0.0
 */
class ContractInfo3101FieldMapperTest {

    /** 14 位 NodeCode 合法测试值（满足 XSD NodeCode facet）。 */
    private static final String VALID_INSTITUTION_CODE = "A1000143000999";

    private CollectorProperties props;
    private ContractInfo3101FieldMapper mapper;

    @BeforeEach
    void setup() {
        props = new CollectorProperties();
        props.setInstitutionCode(VALID_INSTITUTION_CODE);
        mapper = new ContractInfo3101FieldMapper(props);
    }

    /** 完整 raw → body 所有 setter 命中（含 6 可选）。 */
    @Test
    void happyPath_allFieldsPresent_shouldFillBody() {
        Map<String, Object> raw = baseRequired();
        raw.put("hxqy_code", "91110000111111111X");
        raw.put("cert_filename", "cert.pdf");
        raw.put("jfqy_code", "91110000222222222Y");
        raw.put("yfqy_code", "91110000333333333Z");
        raw.put("sx_date", "20261128");
        raw.put("qz_date", "20271128");

        ContractInfo3101 body = (ContractInfo3101) mapper.toMessageBody(raw);

        assertThat(body.getSerialNo()).isNotBlank().hasSize(30);  // uuid32 兜底 or raw serial_no
        assertThat(body.getSendNodeCode()).isEqualTo(VALID_INSTITUTION_CODE);
        assertThat(body.getDesNodeCode()).isEqualTo(FepConstants.HNDEMP_NODE_CODE);
        assertThat(body.getContractNo()).isEqualTo("CON202611280001");
        assertThat(body.getContractType()).isEqualTo("01");
        assertThat(body.getDigitalSeal()).isEqualTo("1");
        assertThat(body.getContractFilename()).isEqualTo("contract.pdf");
        assertThat(body.getJfqyName()).isEqualTo("甲方公司");
        assertThat(body.getYfqyName()).isEqualTo("乙方公司");
        assertThat(body.getHxqyCode()).isEqualTo("91110000111111111X");
        assertThat(body.getCertFilename()).isEqualTo("cert.pdf");
        assertThat(body.getJfqyCode()).isEqualTo("91110000222222222Y");
        assertThat(body.getYfqyCode()).isEqualTo("91110000333333333Z");
        assertThat(body.getSxDate()).isEqualTo("20261128");
        assertThat(body.getQzDate()).isEqualTo("20271128");
    }

    /** 6 必填字段单独缺失 → 抛 FepBusinessException(COLLECT_ASSEMBLE_FAILURE)。 */
    @ParameterizedTest
    @CsvSource({
            "contract_no, contractNo",
            "contract_type, contractType",
            "contract_filename, contractFilename",
            "jfqy_name, jfqyName",
            "yfqy_name, yfqyName"
    })
    void missingRequired_shouldThrowWithFieldName(String rawKey, String logicalField) {
        Map<String, Object> raw = baseRequired();
        raw.remove(rawKey);
        assertThatThrownBy(() -> mapper.toMessageBody(raw))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("missing required field for 3101")
                .hasMessageContaining(logicalField);
    }

    /** digitalSeal 缺失单独测（不在 CsvSource 因为它是特殊 Boolean 字段）。 */
    @Test
    void missingDigitalSeal_shouldThrow() {
        Map<String, Object> raw = baseRequired();
        raw.remove("digital_seal");
        assertThatThrownBy(() -> mapper.toMessageBody(raw))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("missing required field for 3101: digitalSeal");
    }

    /** 6 可选字段全缺失 → 不抛，对应 getter 返 null。 */
    @Test
    void optionalFieldsMissing_shouldSkipSetter() {
        ContractInfo3101 body = (ContractInfo3101) mapper.toMessageBody(baseRequired());

        assertThat(body.getHxqyCode()).isNull();
        assertThat(body.getCertFilename()).isNull();
        assertThat(body.getJfqyCode()).isNull();
        assertThat(body.getYfqyCode()).isNull();
        assertThat(body.getSxDate()).isNull();
        assertThat(body.getQzDate()).isNull();
    }

    /** digitalSeal 接受 4 个真值字面 → 规整为 "1"。 */
    @ParameterizedTest
    @ValueSource(strings = {"1", "true", "TRUE", "True"})
    void digitalSealTrueLiterals_shouldYield1(String literal) {
        Map<String, Object> raw = baseRequired();
        raw.put("digital_seal", literal);
        ContractInfo3101 body = (ContractInfo3101) mapper.toMessageBody(raw);
        assertThat(body.getDigitalSeal()).isEqualTo("1");
    }

    /** digitalSeal 接受 4 个假值字面 → 规整为 "0"。 */
    @ParameterizedTest
    @ValueSource(strings = {"0", "false", "FALSE", "False"})
    void digitalSealFalseLiterals_shouldYield0(String literal) {
        Map<String, Object> raw = baseRequired();
        raw.put("digital_seal", literal);
        ContractInfo3101 body = (ContractInfo3101) mapper.toMessageBody(raw);
        assertThat(body.getDigitalSeal()).isEqualTo("0");
    }

    /** digitalSeal Boolean.TRUE/FALSE 类型 → 同样规整。 */
    @Test
    void digitalSealBooleanType_shouldYieldStringEquivalent() {
        Map<String, Object> raw = baseRequired();
        raw.put("digital_seal", Boolean.TRUE);
        assertThat(((ContractInfo3101) mapper.toMessageBody(raw)).getDigitalSeal()).isEqualTo("1");
        raw.put("digital_seal", Boolean.FALSE);
        assertThat(((ContractInfo3101) mapper.toMessageBody(raw)).getDigitalSeal()).isEqualTo("0");
    }

    /** digitalSeal 非法字面 → 抛业务异常。 */
    @Test
    void digitalSealInvalidLiteral_shouldThrow() {
        Map<String, Object> raw = baseRequired();
        raw.put("digital_seal", "yes");
        assertThatThrownBy(() -> mapper.toMessageBody(raw))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("digitalSeal");
    }

    /** serial_no 缺失 → uuid32 兜底，body.serialNo 非 null 且长度 32。 */
    @Test
    void serialNoMissing_shouldFallbackToUuid32() {
        Map<String, Object> raw = baseRequired();
        raw.remove("serial_no");
        ContractInfo3101 body = (ContractInfo3101) mapper.toMessageBody(raw);
        assertThat(body.getSerialNo()).isNotNull().hasSize(30);
    }

    /** institutionCode 未配置 → 抛业务异常。 */
    @Test
    void institutionCodeMissing_shouldThrow() {
        props.setInstitutionCode(null);
        assertThatThrownBy(() -> mapper.toMessageBody(baseRequired()))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("sendNodeCode")
                .hasMessageContaining("institution-code 未配置");
    }

    /** institutionCode 长度非 14 → 抛业务异常。 */
    @Test
    void institutionCodeInvalidLength_shouldThrow() {
        props.setInstitutionCode("SHORT");
        assertThatThrownBy(() -> mapper.toMessageBody(baseRequired()))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("institutionCode length must be 14");
    }

    /** desNodeCode 始终 = HNDEMP_NODE_CODE（不可被 raw 覆盖）。 */
    @Test
    void desNodeCodeIsAlwaysHndempCenter() {
        Map<String, Object> raw = baseRequired();
        raw.put("des_node_code", "OVERRIDE_TRY");
        ContractInfo3101 body = (ContractInfo3101) mapper.toMessageBody(raw);
        assertThat(body.getDesNodeCode()).isEqualTo(FepConstants.HNDEMP_NODE_CODE);
    }

    /**
     * 9 必填 + serial_no 完整 fixture（uuid32 兜底由具体测试覆盖）。
     */
    private static Map<String, Object> baseRequired() {
        Map<String, Object> raw = new HashMap<>();
        raw.put("serial_no", "FIXSERIAL000000000000000000001");
        raw.put("contract_no", "CON202611280001");
        raw.put("contract_type", "01");
        raw.put("digital_seal", "1");
        raw.put("contract_filename", "contract.pdf");
        raw.put("jfqy_name", "甲方公司");
        raw.put("yfqy_name", "乙方公司");
        return raw;
    }
}
```

- [ ] **Step 3: 写 3102 测试文件**

```java
// fep-collector/src/test/java/com/puchain/fep/collector/assembler/mapper/ArchiveInfo3102FieldMapperTest.java
package com.puchain.fep.collector.assembler.mapper;

import com.puchain.fep.collector.CollectorProperties;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.FepConstants;
import com.puchain.fep.processor.body.supplychain.ArchiveInfo3102;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Task A1: ArchiveInfo3102FieldMapper dedicated unit test
 * （Plan 2026-05-28-collector-mapper-mode3-boil-lake §A1 safety net for Task A2 refactor）。
 *
 * <p>测试覆盖：happy path / 5 必填缺失 / 4 可选 / institutionCode 2 异常
 *
 * @since 1.0.0
 */
class ArchiveInfo3102FieldMapperTest {

    private static final String VALID_INSTITUTION_CODE = "A1000143000999";

    private CollectorProperties props;
    private ArchiveInfo3102FieldMapper mapper;

    @BeforeEach
    void setup() {
        props = new CollectorProperties();
        props.setInstitutionCode(VALID_INSTITUTION_CODE);
        mapper = new ArchiveInfo3102FieldMapper(props);
    }

    @Test
    void happyPath_allFieldsPresent_shouldFillBody() {
        Map<String, Object> raw = baseRequired();
        raw.put("group_name", "集团 A");
        raw.put("group_code", "91110000444444444A");
        raw.put("rzqy_plat_no", "PLAT202611280001");
        raw.put("rzqy_ca_filename", "ca.pdf");

        ArchiveInfo3102 body = (ArchiveInfo3102) mapper.toMessageBody(raw);

        assertThat(body.getSerialNo()).isNotBlank();
        assertThat(body.getSendNodeCode()).isEqualTo(VALID_INSTITUTION_CODE);
        assertThat(body.getDesNodeCode()).isEqualTo(FepConstants.HNDEMP_NODE_CODE);
        assertThat(body.getApplyMode()).isEqualTo("01");
        assertThat(body.getHxqyName()).isEqualTo("核心企业 A");
        assertThat(body.getHxqyCode()).isEqualTo("91110000111111111X");
        assertThat(body.getRzqyName()).isEqualTo("融资企业 A");
        assertThat(body.getRzqyCode()).isEqualTo("91110000222222222Y");
        assertThat(body.getGroupName()).isEqualTo("集团 A");
        assertThat(body.getGroupCode()).isEqualTo("91110000444444444A");
        assertThat(body.getRzqyPlatNo()).isEqualTo("PLAT202611280001");
        assertThat(body.getRzqyCAFilename()).isEqualTo("ca.pdf");
    }

    @ParameterizedTest
    @CsvSource({
            "apply_mode, applyMode",
            "hxqy_name, hxqyName",
            "hxqy_code, hxqyCode",
            "rzqy_name, rzqyName",
            "rzqy_code, rzqyCode"
    })
    void missingRequired_shouldThrowWithFieldName(String rawKey, String logicalField) {
        Map<String, Object> raw = baseRequired();
        raw.remove(rawKey);
        assertThatThrownBy(() -> mapper.toMessageBody(raw))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("missing required field for 3102")
                .hasMessageContaining(logicalField);
    }

    @Test
    void optionalFieldsMissing_shouldSkipSetter() {
        ArchiveInfo3102 body = (ArchiveInfo3102) mapper.toMessageBody(baseRequired());

        assertThat(body.getGroupName()).isNull();
        assertThat(body.getGroupCode()).isNull();
        assertThat(body.getRzqyPlatNo()).isNull();
        assertThat(body.getRzqyCAFilename()).isNull();
    }

    @Test
    void serialNoMissing_shouldFallbackToUuid32() {
        Map<String, Object> raw = baseRequired();
        raw.remove("serial_no");
        ArchiveInfo3102 body = (ArchiveInfo3102) mapper.toMessageBody(raw);
        assertThat(body.getSerialNo()).isNotNull().hasSize(30);
    }

    @Test
    void institutionCodeMissing_shouldThrow() {
        props.setInstitutionCode(null);
        assertThatThrownBy(() -> mapper.toMessageBody(baseRequired()))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("institution-code 未配置");
    }

    @Test
    void institutionCodeInvalidLength_shouldThrow() {
        props.setInstitutionCode("SHORT");
        assertThatThrownBy(() -> mapper.toMessageBody(baseRequired()))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("institutionCode length must be 14");
    }

    private static Map<String, Object> baseRequired() {
        Map<String, Object> raw = new HashMap<>();
        raw.put("serial_no", "FIXSERIAL000000000000000000002");
        raw.put("apply_mode", "01");
        raw.put("hxqy_name", "核心企业 A");
        raw.put("hxqy_code", "91110000111111111X");
        raw.put("rzqy_name", "融资企业 A");
        raw.put("rzqy_code", "91110000222222222Y");
        return raw;
    }
}
```

- [ ] **Step 4: 运行新测试确认 GREEN（TDD inverse — mapper 已实装）**

```bash
cd /Users/muzhou/FEP_v1.0
./mvnw test -pl fep-collector \
  -Dtest='ContractInfo3101FieldMapperTest,ArchiveInfo3102FieldMapperTest' \
  -Dsurefire.failIfNoSpecifiedTests=false --no-transfer-progress
```

期望: `Tests run: 24+, Failures: 0, Errors: 0, Skipped: 0`（具体数依 ParameterizedTest 展开后）

⚠️ **任何 RED 立即停下** — 说明 mapper 现有行为偏离 spec §5.1 期望，须 muzhou 拍板修哪一边（mapper 改还是测试改），不要绕过。

- [ ] **Step 5: 跑 fep-collector 全模块测试确认零回归**

```bash
cd /Users/muzhou/FEP_v1.0
./mvnw test -pl fep-collector --no-transfer-progress
```

期望: `BUILD SUCCESS` + 新增 ~24 个 test 全 GREEN（与现有 ~50 个 fep-collector 测试合计 ~74 全 GREEN）

- [ ] **Step 6: 提交**

```bash
cd /Users/muzhou/FEP_v1.0
git checkout -b refactor/collector-mapper-mode3-boil-lake
git add fep-collector/src/test/java/com/puchain/fep/collector/assembler/mapper/ContractInfo3101FieldMapperTest.java
git add fep-collector/src/test/java/com/puchain/fep/collector/assembler/mapper/ArchiveInfo3102FieldMapperTest.java
git commit -m "$(cat <<'EOF'
test(collector): add dedicated unit tests for ContractInfo3101 + ArchiveInfo3102 FieldMapper

Pre-refactor safety net for Task A2 (AbstractFieldMapper boil-lake extract).
Covers happy path + required field missing + optional skip + boolean literals + institutionCode boundary.
~24 test cases total (3101: ~14, 3102: ~10).

Plan: docs/plans/2026-05-28-collector-mapper-mode3-boil-lake.md §A1
Spec: /Users/muzhou/FEP/docs/superpowers/specs/2026-05-28-collector-mapper-boil-lake-design.md §5.1
PRD: §4 + §6.2 line 794/795
FR-ID: FR-MSG-3101 (注释扩展) + FR-MSG-3102 (注释扩展)

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

## Task A2: 抽 AbstractFieldMapper + 3101/3102 refactor `模式 A`

**PRD 依据:** v1.3 §2.2.1 / §2.2.2 双模式 mapper 共用基础 + §3.2 报文结构 (sendNodeCode/desNodeCode/serialNo 约束)
**追溯 ID:** FR-COMM-COLLECTOR-MAPPER-HELPER（新增） + FR-MSG-3101 / FR-MSG-3102（注释扩展）

**目标:** 抽取 `AbstractFieldMapper` 抽象基类提供 5 共用 helper（boil-lake refactor 命中红线 `feedback_concern_boil_lake_when_cheap_and_safe`），重构 3101/3102 `extends AbstractFieldMapper`，Task A1 dedicated unit test + 现有 `DefaultPayloadAssemblerTest` 集成测试全 GREEN 守护零回归。

**验收标准（从 spec §4.2/§4.5 + Task A1 invariant 推导）:**

1. `AbstractFieldMapper` 5 个 helper 公开签名:
   - `requireInstitutionCode()` 返 String 非空 14 位（缺失/长度违规抛 `FepBusinessException(COLLECT_ASSEMBLE_FAILURE)`）
   - `requireString(Map, String, String)` 返 String 非空非空白
   - `requireBooleanString(Map, String, String)` 接受 Boolean/String 4 真 + 4 假字面，规整 "1"/"0"
   - `applyOptional(Map, String, Consumer<String>)` 缺失/空白跳过 setter
   - `optString(Map, String)` static，null safe return
2. 异常 message 统一格式: `"missing required field for <msgNo>: <field>"` + `LogSanitizer.sanitize(field)` wrap
3. 3 共享常量公开: `DES_NODE_CODE_HNDEMP_CENTER` (= `FepConstants.HNDEMP_NODE_CODE`) / `XSD_BOOLEAN_FALSE` ("0") / `XSD_BOOLEAN_TRUE` ("1")
4. 3101 mapper 类码减少 ~80 行（移除 5 个 helper 本地拷贝），仍 `@Component` + Spring 注入正常
5. 3102 mapper 类码减少 ~50 行，仍 `@Component` + Spring 注入正常
6. Task A1 ~24 个 dedicated test 全 GREEN（重构后零回归）
7. `DefaultPayloadAssemblerTest` 集成测试全 GREEN
8. `AbstractFieldMapperTest` ~12 个测试 GREEN（helper 共用逻辑覆盖）

**Files:**
- Create: `fep-collector/src/main/java/com/puchain/fep/collector/assembler/mapper/AbstractFieldMapper.java`
- Create: `fep-collector/src/test/java/com/puchain/fep/collector/assembler/mapper/AbstractFieldMapperTest.java`
- Modify: `fep-collector/src/main/java/com/puchain/fep/collector/assembler/mapper/ContractInfo3101FieldMapper.java`
- Modify: `fep-collector/src/main/java/com/puchain/fep/collector/assembler/mapper/ArchiveInfo3102FieldMapper.java`

- [ ] **Step 1: 写 AbstractFieldMapperTest 失败 (RED)**

```java
// fep-collector/src/test/java/com/puchain/fep/collector/assembler/mapper/AbstractFieldMapperTest.java
package com.puchain.fep.collector.assembler.mapper;

import com.puchain.fep.collector.CollectorProperties;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.FepConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Task A2: AbstractFieldMapper 共用 helper 单元测试。
 *
 * <p>测试 6 helper：requireInstitutionCode / requireString / requireBooleanString
 * / applyOptional / optString / serialNoOrFallback，以及 3 共享常量 + msgNo 注入异常 message 格式。
 *
 * <p>测试 fixture 使用匿名子类 {@code TestHarness}（msgNo="TEST"），不依赖任何具体 mapper。
 *
 * @since 1.0.0
 */
class AbstractFieldMapperTest {

    private static final String VALID_INSTITUTION_CODE = "A1000143000999";

    private CollectorProperties props;
    private TestHarness mapper;

    @BeforeEach
    void setup() {
        props = new CollectorProperties();
        props.setInstitutionCode(VALID_INSTITUTION_CODE);
        mapper = new TestHarness(props);
    }

    @Test
    void constants_shouldExposeHndempCenterAndBooleanLiterals() {
        assertThat(AbstractFieldMapper.DES_NODE_CODE_HNDEMP_CENTER)
                .isEqualTo(FepConstants.HNDEMP_NODE_CODE);
        assertThat(AbstractFieldMapper.XSD_BOOLEAN_TRUE).isEqualTo("1");
        assertThat(AbstractFieldMapper.XSD_BOOLEAN_FALSE).isEqualTo("0");
    }

    @Test
    void requireInstitutionCode_validCode_shouldReturn() {
        assertThat(mapper.callRequireInstitutionCode()).isEqualTo(VALID_INSTITUTION_CODE);
    }

    @Test
    void requireInstitutionCode_null_shouldThrow() {
        props.setInstitutionCode(null);
        assertThatThrownBy(mapper::callRequireInstitutionCode)
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("missing required field for TEST: sendNodeCode")
                .hasMessageContaining("institution-code 未配置");
    }

    @Test
    void requireInstitutionCode_invalidLength_shouldThrow() {
        props.setInstitutionCode("SHORT");
        assertThatThrownBy(mapper::callRequireInstitutionCode)
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("institutionCode length must be 14");
    }

    @Test
    void requireString_present_shouldReturn() {
        Map<String, Object> raw = Map.of("key", "value");
        assertThat(mapper.callRequireString(raw, "key", "logical")).isEqualTo("value");
    }

    @Test
    void requireString_missing_shouldThrow() {
        assertThatThrownBy(() -> mapper.callRequireString(new HashMap<>(), "key", "logical"))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("missing required field for TEST: logical");
    }

    @Test
    void requireString_blank_shouldThrow() {
        Map<String, Object> raw = Map.of("key", "   ");
        assertThatThrownBy(() -> mapper.callRequireString(raw, "key", "logical"))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("logical");
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "true", "TRUE", "True"})
    void requireBooleanString_trueLiterals_shouldYield1(String literal) {
        Map<String, Object> raw = Map.of("key", literal);
        assertThat(mapper.callRequireBooleanString(raw, "key", "logical")).isEqualTo("1");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "false", "FALSE", "False"})
    void requireBooleanString_falseLiterals_shouldYield0(String literal) {
        Map<String, Object> raw = Map.of("key", literal);
        assertThat(mapper.callRequireBooleanString(raw, "key", "logical")).isEqualTo("0");
    }

    @Test
    void requireBooleanString_booleanType_shouldYield() {
        assertThat(mapper.callRequireBooleanString(Map.of("key", Boolean.TRUE), "key", "k"))
                .isEqualTo("1");
        assertThat(mapper.callRequireBooleanString(Map.of("key", Boolean.FALSE), "key", "k"))
                .isEqualTo("0");
    }

    @Test
    void requireBooleanString_missing_shouldThrow() {
        assertThatThrownBy(() -> mapper.callRequireBooleanString(new HashMap<>(), "key", "logical"))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("missing required field for TEST: logical");
    }

    @Test
    void requireBooleanString_invalidLiteral_shouldThrow() {
        Map<String, Object> raw = Map.of("key", "yes");
        assertThatThrownBy(() -> mapper.callRequireBooleanString(raw, "key", "logical"))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("logical");
    }

    @Test
    void applyOptional_present_shouldCallSetter() {
        AtomicReference<String> captured = new AtomicReference<>();
        Map<String, Object> raw = Map.of("key", "value");
        mapper.callApplyOptional(raw, "key", captured::set);
        assertThat(captured.get()).isEqualTo("value");
    }

    @Test
    void applyOptional_missing_shouldSkipSetter() {
        AtomicReference<String> captured = new AtomicReference<>();
        mapper.callApplyOptional(new HashMap<>(), "key", captured::set);
        assertThat(captured.get()).isNull();
    }

    @Test
    void applyOptional_blank_shouldSkipSetter() {
        AtomicReference<String> captured = new AtomicReference<>();
        Map<String, Object> raw = Map.of("key", "   ");
        mapper.callApplyOptional(raw, "key", captured::set);
        assertThat(captured.get()).isNull();
    }

    @Test
    void optString_null_shouldReturnNull() {
        assertThat(AbstractFieldMapper.optString(new HashMap<>(), "missing")).isNull();
    }

    @Test
    void optString_objectToString_shouldReturnString() {
        assertThat(AbstractFieldMapper.optString(Map.of("key", 42), "key")).isEqualTo("42");
    }

    // ── serialNoOrFallback() v0.2 tests ────────────────────────────────

    @Test
    void serialNoOrFallback_missing_shouldReturnUuid32Truncated30() {
        String result = mapper.callSerialNoOrFallback(new HashMap<>());
        assertThat(result).isNotNull().hasSize(30);
    }

    @Test
    void serialNoOrFallback_validLength30_shouldReturnAsIs() {
        String input = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"; // 30 chars
        Map<String, Object> raw = Map.of("serial_no", input);
        assertThat(mapper.callSerialNoOrFallback(raw)).isEqualTo(input);
    }

    @Test
    void serialNoOrFallback_invalidLength32_shouldThrow() {
        String input = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"; // 32 chars
        Map<String, Object> raw = Map.of("serial_no", input);
        assertThatThrownBy(() -> mapper.callSerialNoOrFallback(raw))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("invalid serialNo for TEST")
                .hasMessageContaining("XSD requires length=30")
                .hasMessageContaining("got 32");
    }

    @Test
    void serialNoOrFallback_invalidLength10_shouldThrow() {
        Map<String, Object> raw = Map.of("serial_no", "SHORTSTR10");
        assertThatThrownBy(() -> mapper.callSerialNoOrFallback(raw))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("got 10");
    }

    /** 测试用匿名子类（msgNo="TEST"，暴露 protected helper 调用入口）。 */
    private static final class TestHarness extends AbstractFieldMapper {
        TestHarness(final CollectorProperties props) {
            super(props, "TEST");
        }

        @Override
        public Object toMessageBody(final Map<String, Object> rawData) {
            throw new UnsupportedOperationException("test harness");
        }

        // 暴露 protected helper 给测试调用
        String callRequireInstitutionCode() { return requireInstitutionCode(); }
        String callRequireString(Map<String, Object> r, String k, String l) {
            return requireString(r, k, l);
        }
        String callRequireBooleanString(Map<String, Object> r, String k, String l) {
            return requireBooleanString(r, k, l);
        }
        void callApplyOptional(Map<String, Object> r, String k, java.util.function.Consumer<String> s) {
            applyOptional(r, k, s);
        }
        String callSerialNoOrFallback(Map<String, Object> r) {
            return serialNoOrFallback(r);
        }
    }
}
```

- [ ] **Step 2: 运行测试确认 RED（AbstractFieldMapper 类尚未存在）**

```bash
cd /Users/muzhou/FEP_v1.0
./mvnw test -pl fep-collector -Dtest=AbstractFieldMapperTest \
  -Dsurefire.failIfNoSpecifiedTests=false --no-transfer-progress
```

期望: 编译失败 — `cannot find symbol: class AbstractFieldMapper`

- [ ] **Step 3: 写 AbstractFieldMapper 抽象基类**

```java
// fep-collector/src/main/java/com/puchain/fep/collector/assembler/mapper/AbstractFieldMapper.java
package com.puchain.fep.collector.assembler.mapper;

import com.puchain.fep.collector.CollectorProperties;
import com.puchain.fep.collector.assembler.FieldMapper;
import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.FepConstants;
import com.puchain.fep.common.util.LogSanitizer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

/**
 * fep-collector FieldMapper 共用抽象基类（Plan 2026-05-28-collector-mapper-mode3-boil-lake §A2）。
 *
 * <p>抽取自 {@link ContractInfo3101FieldMapper} + {@link ArchiveInfo3102FieldMapper} helper
 * 重复（red flag: Rule-of-Three 6+ usage），命中红线
 * {@code feedback_concern_boil_lake_when_cheap_and_safe}。
 *
 * <p>子类约定：
 * <ul>
 *   <li>构造时一次性注入 {@link CollectorProperties} + msgNo 字面（如 "3101"）。</li>
 *   <li>必填字段缺失 → {@link FepBusinessException}({@link FepErrorCode#COLLECT_ASSEMBLE_FAILURE})，
 *       message 形如 {@code "missing required field for <msgNo>: <logicalField>"}。</li>
 *   <li>可选字段缺失 → 跳过 setter（不抛）。</li>
 *   <li>嵌套 complex 类型（如 {@code HxqyInfo3109}）暂留 Javadoc stub，由后续业务深化补。</li>
 * </ul>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public abstract class AbstractFieldMapper implements FieldMapper {

    /** XSD Boolean type 允许的"假"字面量。 */
    public static final String XSD_BOOLEAN_FALSE = "0";

    /** XSD Boolean type 允许的"真"字面量。 */
    public static final String XSD_BOOLEAN_TRUE = "1";

    /** HNDEMP 平台中心节点代码（CLAUDE.md 已知约束）。 */
    public static final String DES_NODE_CODE_HNDEMP_CENTER = FepConstants.HNDEMP_NODE_CODE;

    /** Accepted "true" literal forms (case-explicit; no Unicode case-folding). */
    private static final Set<String> TRUE_LITERALS = Set.of("1", "true", "TRUE", "True");

    /** Accepted "false" literal forms (case-explicit; no Unicode case-folding). */
    private static final Set<String> FALSE_LITERALS = Set.of("0", "false", "FALSE", "False");

    /** PRD §3.2 sendNodeCode 14 位 NodeCode 长度（XSD Base.xsd:NodeCode）。 */
    private static final int NODE_CODE_LENGTH = 14;

    /** 数据采集配置（用于读取 institutionCode）。 */
    protected final CollectorProperties props;

    /** 子类报文类型字面（如 "3101"），由构造函数注入，参与异常 message 拼装。 */
    protected final String msgNo;

    /**
     * 构造基类。
     *
     * @param props 数据采集配置（非 null）
     * @param msgNo 报文 msgNo 字面（非 null，参与异常 message）
     */
    protected AbstractFieldMapper(final CollectorProperties props, final String msgNo) {
        this.props = Objects.requireNonNull(props, "props");
        this.msgNo = Objects.requireNonNull(msgNo, "msgNo");
    }

    /**
     * 读取 institutionCode，校验非空 + 14 位长度。
     *
     * @return 非空 14 位 institutionCode
     * @throws FepBusinessException COLLECT_ASSEMBLE_FAILURE 缺失或长度违规
     */
    protected final String requireInstitutionCode() {
        final String code = props.getInstitutionCode();
        if (code == null || code.isBlank()) {
            throw new FepBusinessException(
                    FepErrorCode.COLLECT_ASSEMBLE_FAILURE,
                    "missing required field for " + msgNo + ": sendNodeCode "
                            + "(fep.collector.institution-code 未配置)");
        }
        if (code.length() != NODE_CODE_LENGTH) {
            throw new FepBusinessException(
                    FepErrorCode.COLLECT_ASSEMBLE_FAILURE,
                    "invalid sendNodeCode for " + msgNo + ": institutionCode length must be "
                            + NODE_CODE_LENGTH + ", got " + code.length());
        }
        return code;
    }

    /**
     * 读取必填 String 字段。
     *
     * @param rawData       原始字段 Map（非 null）
     * @param rawKey        rawData 的 key
     * @param logicalField  Java 字段名（异常 message 使用，已 LogSanitizer wrap）
     * @return 非空 trim 后字符串
     * @throws FepBusinessException COLLECT_ASSEMBLE_FAILURE 缺失或空白
     */
    @SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
            justification = "logicalField 经 LogSanitizer.sanitize() wrap，CRLF 已被中和；"
                    + "find-sec-bugs 默认 sink 列表未识别 LogSanitizer，需显式抑制。")
    protected final String requireString(final Map<String, Object> rawData,
                                         final String rawKey,
                                         final String logicalField) {
        final String value = optString(rawData, rawKey);
        if (value == null || value.isBlank()) {
            throw new FepBusinessException(
                    FepErrorCode.COLLECT_ASSEMBLE_FAILURE,
                    "missing required field for " + msgNo + ": "
                            + LogSanitizer.sanitize(logicalField));
        }
        return value;
    }

    /**
     * 读取必填 Boolean 字段（接受 {@link Boolean} 或 String 0/1/true/false 4+4 字面），
     * 规整为 XSD Boolean type 允许的 "0"/"1"。
     *
     * <p><b>大小写处理：</b>采用显式字面量集合 {@link #TRUE_LITERALS} / {@link #FALSE_LITERALS}
     * 而非 {@code equalsIgnoreCase} / {@code toLowerCase} — 避免任何 Unicode case-folding
     * 行为（SpotBugs IMPROPER_UNICODE 友好）。
     *
     * @param rawData       原始字段（非 null）
     * @param rawKey        rawData 的 key
     * @param logicalField  Java 字段名（异常 message 使用）
     * @return "0" 或 "1"
     * @throws FepBusinessException COLLECT_ASSEMBLE_FAILURE 缺失或非法字面
     */
    @SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
            justification = "logicalField + raw value 均经 LogSanitizer.sanitize() wrap")
    protected final String requireBooleanString(final Map<String, Object> rawData,
                                                final String rawKey,
                                                final String logicalField) {
        final Object raw = rawData.get(rawKey);
        if (raw == null) {
            throw new FepBusinessException(
                    FepErrorCode.COLLECT_ASSEMBLE_FAILURE,
                    "missing required field for " + msgNo + ": "
                            + LogSanitizer.sanitize(logicalField));
        }
        if (raw instanceof Boolean b) {
            return b ? XSD_BOOLEAN_TRUE : XSD_BOOLEAN_FALSE;
        }
        final String s = raw.toString().trim();
        if (TRUE_LITERALS.contains(s)) {
            return XSD_BOOLEAN_TRUE;
        }
        if (FALSE_LITERALS.contains(s)) {
            return XSD_BOOLEAN_FALSE;
        }
        throw new FepBusinessException(
                FepErrorCode.COLLECT_ASSEMBLE_FAILURE,
                "missing required field for " + msgNo + ": "
                        + LogSanitizer.sanitize(logicalField)
                        + " (expected 0/1/true/false, got "
                        + LogSanitizer.sanitize(s) + ")");
    }

    /**
     * 读取可选 String 字段，缺失/空白则跳过 setter 调用。
     *
     * @param rawData 原始字段
     * @param rawKey  rawData 的 key
     * @param setter  setter 方法引用
     */
    protected final void applyOptional(final Map<String, Object> rawData,
                                       final String rawKey,
                                       final Consumer<String> setter) {
        final String value = optString(rawData, rawKey);
        if (value != null && !value.isBlank()) {
            setter.accept(value);
        }
    }

    /**
     * 读取 String 字段（{@code Object.toString()}），null 直接返回 null。
     *
     * <p>v0.2: 可见性从 package-private 改为 {@code protected static} —
     * 与 spec §4.2 设计声明对齐，允许跨 package 的测试辅助调用（如 fep-web XsdComplianceTest 引用）。
     *
     * @param rawData 原始字段
     * @param key     key
     * @return String 或 null
     */
    protected static String optString(final Map<String, Object> rawData, final String key) {
        final Object raw = rawData.get(key);
        return raw == null ? null : raw.toString();
    }

    /**
     * 读取 SerialNo 字段，缺失时用 {@link IdGenerator#uuid32()} 截断到 30 字符兜底。
     *
     * <p>v0.2 新增 — 兼容 {@code DataType.xsd} {@code SerialNo simpleType}
     * {@code <xsd:length value="30"/>} 固定长度约束（线 616-623）。
     * 之前直接用 {@code uuid32()} 返 32 字符违反 XSD（生产侧 R-NEW-1 删除
     * {@code @MockBean XsdValidator} 后暴露 latent bug，2026-05-28 别会话
     * ship {@code XsdTestSupport.pad30} test helper 验证此约束）。
     *
     * <p><b>语义</b>:
     * <ul>
     *   <li>raw["serial_no"] 缺失 → 返 {@code uuid32().substring(0, 30)} 30 字符 fallback</li>
     *   <li>raw["serial_no"] 存在且长度 = 30 → 直接返回</li>
     *   <li>raw["serial_no"] 存在且长度 ≠ 30 → 抛
     *       {@link FepBusinessException}({@link FepErrorCode#COLLECT_ASSEMBLE_FAILURE})</li>
     * </ul>
     *
     * @param rawData 原始字段
     * @return 30 字符 SerialNo（XSD compliance）
     * @throws FepBusinessException raw serialNo 长度违规
     */
    @SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
            justification = "数字长度字面无 CRLF 注入风险")
    protected final String serialNoOrFallback(final Map<String, Object> rawData) {
        final String raw = optString(rawData, "serial_no");
        if (raw == null) {
            return com.puchain.fep.common.util.IdGenerator.uuid32().substring(0, 30);
        }
        if (raw.length() != 30) {
            throw new FepBusinessException(
                    FepErrorCode.COLLECT_ASSEMBLE_FAILURE,
                    "invalid serialNo for " + msgNo
                            + ": XSD requires length=30, got " + raw.length());
        }
        return raw;
    }
}
```

- [ ] **Step 4: 运行 AbstractFieldMapperTest 确认 GREEN**

```bash
cd /Users/muzhou/FEP_v1.0
./mvnw test -pl fep-collector -Dtest=AbstractFieldMapperTest \
  -Dsurefire.failIfNoSpecifiedTests=false --no-transfer-progress
```

期望: `Tests run: ~18, Failures: 0, Errors: 0`（含 ParameterizedTest 展开）

- [ ] **Step 5: 重构 ContractInfo3101FieldMapper extends AbstractFieldMapper**

```java
// fep-collector/src/main/java/com/puchain/fep/collector/assembler/mapper/ContractInfo3101FieldMapper.java
package com.puchain.fep.collector.assembler.mapper;

import com.puchain.fep.collector.CollectorProperties;
import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.processor.body.supplychain.ContractInfo3101;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * 3101 电子合同信息流转 FieldMapper（9 必填 + 6 可选示范）。
 *
 * <p>Plan §A2 (2026-05-28-collector-mapper-mode3-boil-lake) refactor:
 * extends {@link AbstractFieldMapper}，移除本地 helper 拷贝（已抽至基类），仅保留业务字段映射逻辑。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class ContractInfo3101FieldMapper extends AbstractFieldMapper {

    /**
     * 构造 3101 mapper。
     *
     * @param props 数据采集配置（用于读取 institutionCode）
     */
    public ContractInfo3101FieldMapper(final CollectorProperties props) {
        super(props, "3101");
    }

    @Override
    public Object toMessageBody(final Map<String, Object> rawData) {
        Objects.requireNonNull(rawData, "rawData");

        final ContractInfo3101 body = new ContractInfo3101();

        // ── 必填字段（9） ───────────────────────────────────────────────────
        body.setSerialNo(serialNoOrFallback(rawData));
        body.setSendNodeCode(requireInstitutionCode());
        body.setDesNodeCode(DES_NODE_CODE_HNDEMP_CENTER);
        body.setContractNo(requireString(rawData, "contract_no", "contractNo"));
        body.setContractType(requireString(rawData, "contract_type", "contractType"));
        body.setDigitalSeal(requireBooleanString(rawData, "digital_seal", "digitalSeal"));
        body.setContractFilename(requireString(rawData, "contract_filename", "contractFilename"));
        body.setJfqyName(requireString(rawData, "jfqy_name", "jfqyName"));
        body.setYfqyName(requireString(rawData, "yfqy_name", "yfqyName"));

        // ── 可选字段（6 示范） ─────────────────────────────────────────────
        applyOptional(rawData, "hxqy_code", body::setHxqyCode);
        applyOptional(rawData, "cert_filename", body::setCertFilename);
        applyOptional(rawData, "jfqy_code", body::setJfqyCode);
        applyOptional(rawData, "yfqy_code", body::setYfqyCode);
        applyOptional(rawData, "sx_date", body::setSxDate);
        applyOptional(rawData, "qz_date", body::setQzDate);

        return body;
    }
}
```

- [ ] **Step 6: 重构 ArchiveInfo3102FieldMapper extends AbstractFieldMapper**

```java
// fep-collector/src/main/java/com/puchain/fep/collector/assembler/mapper/ArchiveInfo3102FieldMapper.java
package com.puchain.fep.collector.assembler.mapper;

import com.puchain.fep.collector.CollectorProperties;
import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.processor.body.supplychain.ArchiveInfo3102;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * 3102 融资企业开户建档申请 FieldMapper（8 必填 + 4 可选示范）。
 *
 * <p>Plan §A2 (2026-05-28-collector-mapper-mode3-boil-lake) refactor:
 * extends {@link AbstractFieldMapper}，移除本地 helper 拷贝。
 *
 * <p>嵌套 complex 字段（{@code rzqyBaseInfo} / {@code rzqyAccInfo} / ...）由后续业务深化时再补。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class ArchiveInfo3102FieldMapper extends AbstractFieldMapper {

    public ArchiveInfo3102FieldMapper(final CollectorProperties props) {
        super(props, "3102");
    }

    @Override
    public Object toMessageBody(final Map<String, Object> rawData) {
        Objects.requireNonNull(rawData, "rawData");

        final ArchiveInfo3102 body = new ArchiveInfo3102();

        // ── 必填字段（8） ───────────────────────────────────────────────────
        body.setSerialNo(serialNoOrFallback(rawData));
        body.setSendNodeCode(requireInstitutionCode());
        body.setDesNodeCode(DES_NODE_CODE_HNDEMP_CENTER);
        body.setApplyMode(requireString(rawData, "apply_mode", "applyMode"));
        body.setHxqyName(requireString(rawData, "hxqy_name", "hxqyName"));
        body.setHxqyCode(requireString(rawData, "hxqy_code", "hxqyCode"));
        body.setRzqyName(requireString(rawData, "rzqy_name", "rzqyName"));
        body.setRzqyCode(requireString(rawData, "rzqy_code", "rzqyCode"));

        // ── 可选字段（4 示范） ─────────────────────────────────────────────
        applyOptional(rawData, "group_name", body::setGroupName);
        applyOptional(rawData, "group_code", body::setGroupCode);
        applyOptional(rawData, "rzqy_plat_no", body::setRzqyPlatNo);
        applyOptional(rawData, "rzqy_ca_filename", body::setRzqyCAFilename);

        return body;
    }
}
```

- [ ] **Step 7 (v0.2 新增): 同步修改 DefaultPayloadAssemblerTest stub mapper 注册 + delete deferredMapper3109 测试**

⚠️ 修复 v0.1 评审 MAJOR #1 — A3-A5 mapper 实装后，DefaultPayloadAssemblerTest line 61-65 的无参 `QyRegister3109FieldMapper::new` / `BankCheckDay3116FieldMapper::new` / `RzReturnInfo3009FieldMapper::new` 会**编译失败**（mapper extends AbstractFieldMapper 后构造函数变 `(CollectorProperties props)`）。本 step 在 A2 commit 中同步修正，避免 A3-A5 verify 失败。

```java
// 修改 fep-collector/src/test/java/com/puchain/fep/collector/assembler/DefaultPayloadAssemblerTest.java

// 1. 替换 line 60-71 stub mapper 注册段（统一改为 props 注入风格）:

        // 8 mappers (uniform props injection per AbstractFieldMapper refactor)
        appContext.registerBean(ContractInfo3101FieldMapper.class,
                () -> new ContractInfo3101FieldMapper(props));
        appContext.registerBean(ArchiveInfo3102FieldMapper.class,
                () -> new ArchiveInfo3102FieldMapper(props));
        appContext.registerBean(QyRegister3109FieldMapper.class,
                () -> new QyRegister3109FieldMapper(props));
        appContext.registerBean(BankCheckDay3116FieldMapper.class,
                () -> new BankCheckDay3116FieldMapper(props));
        appContext.registerBean(RzReturnInfo3009FieldMapper.class,
                () -> new RzReturnInfo3009FieldMapper(props));
        // Mode2 stub mappers still throw UnsupportedOperationException
        // (实装由 Plan B 完成); v0.2 暂保留 stub 兼容构造器,但等 Plan B 起手将同步改 props 注入
        appContext.registerBean(RzApplyInfo3105FieldMapper.class, RzApplyInfo3105FieldMapper::new);
        appContext.registerBean(PzCheckQuery3107FieldMapper.class,
                PzCheckQuery3107FieldMapper::new);
        appContext.registerBean(HxqyCreditAmt3112FieldMapper.class,
                HxqyCreditAmt3112FieldMapper::new);

// 2. 删除 line 182-189 的 assemble_deferredMapper3109_throwsUnsupportedOperation() 测试方法
//    （3109 mapper 在 Task A3 即将实装，该 negative test 立即 obsolete；
//    红线 feedback_obsolete_negative_test_cleanup + feedback_cross_task_obsolete_fixture_assumption_when_set_extended 同步触发）。
//    删除整个方法:
//        /** Plan §6 acceptance: deferred mapper invocation → UnsupportedOperationException. */
//        @Test
//        void assemble_deferredMapper3109_throwsUnsupportedOperation() { ... }
```

注：Mode2 stub mapper (3105/3107/3112) 暂保留无参构造器 — 这些 mapper 在 Plan A 不实装（Plan B 范围），保留 stub `UnsupportedOperationException` 与现有 mapper 类签名一致，避免 Plan A 范围蔓延到 Plan B mapper class。Plan B Task 1 实装 Mode2 mapper 时同步改 props 注入。

- [ ] **Step 8: 跑 Task A1 + AbstractFieldMapperTest + 全 fep-collector 测试确认零回归**

```bash
cd /Users/muzhou/FEP_v1.0
./mvnw test -pl fep-collector --no-transfer-progress
```

期望: `BUILD SUCCESS` + 全部 ~74-80 个 fep-collector 测试 GREEN（含 Task A1 ~24 + AbstractFieldMapperTest ~22 + 现有 ~50+ DefaultPayloadAssemblerTest 等 — 注意 deferredMapper3109 测试已删除 -1）

⚠️ **任何 RED 立即停下** — 3101/3102 refactor 引入回归，需对照 A1 测试 case 定位差异。

- [ ] **Step 9: SpotBugs + find-sec-bugs 本机 verify（CI 兜底前置）**

```bash
cd /Users/muzhou/FEP_v1.0
./mvnw verify -pl fep-collector --no-transfer-progress
```

期望: `BUILD SUCCESS`（含 Checkstyle + SpotBugs + find-sec-bugs + JaCoCo 全过；CRLF_INJECTION_LOGS 由 `@SuppressFBWarnings` 抑制，参考红线 `feedback_logsanitizer_alone_insufficient_for_findsecbugs`）

- [ ] **Step 10: 提交（v0.2 含 6 文件改动）**

```bash
cd /Users/muzhou/FEP_v1.0
git add fep-collector/src/main/java/com/puchain/fep/collector/assembler/mapper/AbstractFieldMapper.java
git add fep-collector/src/test/java/com/puchain/fep/collector/assembler/mapper/AbstractFieldMapperTest.java
git add fep-collector/src/main/java/com/puchain/fep/collector/assembler/mapper/ContractInfo3101FieldMapper.java
git add fep-collector/src/main/java/com/puchain/fep/collector/assembler/mapper/ArchiveInfo3102FieldMapper.java
git add fep-collector/src/test/java/com/puchain/fep/collector/assembler/DefaultPayloadAssemblerTest.java
git commit -m "$(cat <<'EOF'
refactor(collector): extract AbstractFieldMapper base class (boil-lake), 3101/3102 extends + SerialNo XSD length=30 fix

Helper boil-lake refactor per red-line feedback_concern_boil_lake_when_cheap_and_safe:
- Extract 6 common helpers (requireInstitutionCode/requireString/requireBooleanString/
  applyOptional/optString/serialNoOrFallback) + 3 constants to AbstractFieldMapper base class
- serialNoOrFallback() truncates uuid32 to 30 chars + rejects raw serial_no length != 30
  → fixes latent XSD compliance bug (DataType.xsd SerialNo length=30) revealed by R-NEW-1
  + 2026-05-28 XsdTestSupport.pad30 follow-on (red-line feedback_baseline_drift_during_long_review_cycle)
- 3101/3102 extends base, removing ~130 LOC of duplicated helper copies
- optString visibility raised from package-private to protected static (spec §4.2 alignment +
  allows cross-package XsdComplianceTest access in fep-web Task A7)
- DefaultPayloadAssemblerTest: stub mapper registration unified to props injection style
  + assemble_deferredMapper3109_throwsUnsupportedOperation test deleted (obsolete after A3 ship,
  red-line feedback_obsolete_negative_test_cleanup + feedback_cross_task_obsolete_fixture_assumption)
- AbstractFieldMapperTest covers all 6 helpers + Boolean literal variants + SerialNo boundary
- Task A1 dedicated unit tests + existing DefaultPayloadAssemblerTest all GREEN

Plan: docs/plans/2026-05-28-collector-mapper-mode3-boil-lake.md §A2 (v0.2)
Spec: /Users/muzhou/FEP/docs/superpowers/specs/2026-05-28-collector-mapper-boil-lake-design.md §4.2
PRD: §2.2.1/§2.2.2 双模式 mapper 共用基础 + §3.2 报文头约束
FR-ID: FR-COMM-COLLECTOR-MAPPER-HELPER (新增) + FR-MSG-3101 (注释扩展) + FR-MSG-3102 (注释扩展)

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

## Task A3: 3109 QyRegister 实装 + unit test `模式 B`

**PRD 依据:** v1.3 §4 报文接入 + §6.2 line 798 (3109 企业信息登记) + §841 模式 3 信息发送 + §2.2.3 ✅ 数仓推荐
**追溯 ID:** FR-MSG-3109-COLLECTOR-MAPPER (新增子项)
**XSD ref:** `fep-processor/src/main/resources/xsd/3109.xsd` complexType `qyRegister3109` (line 45-)

**字段映射表**（按 XSD 3109.xsd minOccurs grep 实测）:

| 字段 | XSD type | minOccurs | mapper 处理 |
|---|---|---|---|
| SerialNo | SerialNo | required | `serial_no` raw, IdGenerator.uuid32() 兜底 |
| SendNodeCode | NodeCode | required | `requireInstitutionCode()` from props |
| DesNodeCode | NodeCode | required | `DES_NODE_CODE_HNDEMP_CENTER` 固定 |
| qyFlag | Number1to2 | required | `qy_flag` raw, requireString |
| hxqyInfo | hxqyInfo | optional (minOccurs=0) | ⏸ Javadoc stub (嵌套 complex，后续业务深化) |
| qyAccLockInfo | qyAccLockInfo | optional | ⏸ Javadoc stub |
| PlatInfo | PlatInfo | optional | ⏸ Javadoc stub |
| ExtInfo | extInfo | optional | ⏸ Javadoc stub |

**验收标准:**

1. 5 个测试用例全 GREEN — happy path × 1 + missingQyFlag × 1 + serialNoMissing × 1 + institutionCodeMissing × 1 + institutionCodeInvalidLength × 1（v0.2 修正：v0.1 误标"必填缺失 × 1 + institutionCode 2 异常 = 4"，实际 5）
2. 4 嵌套 optional 字段 Javadoc 明示 `⏸ stub (后续业务深化)` + 不调用对应 setter
3. mapper 不再抛 `UnsupportedOperationException`
4. `RouteRegistryTest` (现有) 全 GREEN；`DefaultPayloadAssemblerTest.assemble_deferredMapper3109_throwsUnsupportedOperation()` 测试方法已在 Task A2 Step 7 删除（v0.2 修复 MAJOR #1：避免 A3 commit 携带 known fail/编译失败）

**Files:**
- Modify: `fep-collector/src/main/java/com/puchain/fep/collector/assembler/mapper/QyRegister3109FieldMapper.java`
- Create: `fep-collector/src/test/java/com/puchain/fep/collector/assembler/mapper/QyRegister3109FieldMapperTest.java`

- [ ] **Step 1: 写 QyRegister3109FieldMapperTest 失败 (RED)**

```java
// fep-collector/src/test/java/com/puchain/fep/collector/assembler/mapper/QyRegister3109FieldMapperTest.java
package com.puchain.fep.collector.assembler.mapper;

import com.puchain.fep.collector.CollectorProperties;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.FepConstants;
import com.puchain.fep.processor.body.supplychain.QyRegister3109;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Task A3: QyRegister3109FieldMapper unit test
 * （Plan 2026-05-28-collector-mapper-mode3-boil-lake §A3）。
 */
class QyRegister3109FieldMapperTest {

    private static final String VALID_INSTITUTION_CODE = "A1000143000999";

    private CollectorProperties props;
    private QyRegister3109FieldMapper mapper;

    @BeforeEach
    void setup() {
        props = new CollectorProperties();
        props.setInstitutionCode(VALID_INSTITUTION_CODE);
        mapper = new QyRegister3109FieldMapper(props);
    }

    @Test
    void happyPath_allRequired_shouldFillBody() {
        Map<String, Object> raw = baseRequired();
        QyRegister3109 body = (QyRegister3109) mapper.toMessageBody(raw);

        assertThat(body.getSerialNo()).isNotBlank();
        assertThat(body.getSendNodeCode()).isEqualTo(VALID_INSTITUTION_CODE);
        assertThat(body.getDesNodeCode()).isEqualTo(FepConstants.HNDEMP_NODE_CODE);
        assertThat(body.getQyFlag()).isEqualTo("1");
        assertThat(body.getHxqyInfo()).isNull();
        assertThat(body.getQyAccLockInfo()).isNull();
        assertThat(body.getPlatInfo()).isNull();
        assertThat(body.getExtInfo()).isNull();
    }

    @Test
    void missingQyFlag_shouldThrow() {
        Map<String, Object> raw = baseRequired();
        raw.remove("qy_flag");
        assertThatThrownBy(() -> mapper.toMessageBody(raw))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("missing required field for 3109: qyFlag");
    }

    @Test
    void serialNoMissing_shouldFallbackToUuid32() {
        Map<String, Object> raw = baseRequired();
        raw.remove("serial_no");
        QyRegister3109 body = (QyRegister3109) mapper.toMessageBody(raw);
        assertThat(body.getSerialNo()).isNotNull().hasSize(30);
    }

    @Test
    void institutionCodeMissing_shouldThrow() {
        props.setInstitutionCode(null);
        assertThatThrownBy(() -> mapper.toMessageBody(baseRequired()))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("missing required field for 3109: sendNodeCode");
    }

    @Test
    void institutionCodeInvalidLength_shouldThrow() {
        props.setInstitutionCode("SHORT");
        assertThatThrownBy(() -> mapper.toMessageBody(baseRequired()))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("institutionCode length must be 14");
    }

    private static Map<String, Object> baseRequired() {
        Map<String, Object> raw = new HashMap<>();
        raw.put("serial_no", "FIXSERIAL000000000000000000003");
        raw.put("qy_flag", "1");
        return raw;
    }
}
```

- [ ] **Step 2: 跑测试确认 RED**

```bash
cd /Users/muzhou/FEP_v1.0
./mvnw test -pl fep-collector -Dtest=QyRegister3109FieldMapperTest \
  -Dsurefire.failIfNoSpecifiedTests=false --no-transfer-progress
```
期望: 测试 RED — mapper 现在抛 `UnsupportedOperationException`

- [ ] **Step 3: 实装 QyRegister3109FieldMapper**

```java
// fep-collector/src/main/java/com/puchain/fep/collector/assembler/mapper/QyRegister3109FieldMapper.java
package com.puchain.fep.collector.assembler.mapper;

import com.puchain.fep.collector.CollectorProperties;
import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.processor.body.supplychain.QyRegister3109;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * 3109 企业信息登记 FieldMapper (Plan §A3, 4 String 必填字段)。
 *
 * <p>PRD §2.2.3 ✅ 数仓推荐场景；PRD §841 模式 3 信息发送。
 *
 * <p><b>嵌套 complex 字段（hxqyInfo / qyAccLockInfo / PlatInfo / ExtInfo）暂留 stub</b>：
 * XSD 3109.xsd 中 4 嵌套字段均 {@code minOccurs="0"}（可选），mapper 不调对应 setter。
 * 未来业务深化 Plan 时补充 raw → 嵌套对象映射逻辑。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class QyRegister3109FieldMapper extends AbstractFieldMapper {

    public QyRegister3109FieldMapper(final CollectorProperties props) {
        super(props, "3109");
    }

    @Override
    public Object toMessageBody(final Map<String, Object> rawData) {
        Objects.requireNonNull(rawData, "rawData");

        final QyRegister3109 body = new QyRegister3109();

        body.setSerialNo(serialNoOrFallback(rawData));
        body.setSendNodeCode(requireInstitutionCode());
        body.setDesNodeCode(DES_NODE_CODE_HNDEMP_CENTER);
        body.setQyFlag(requireString(rawData, "qy_flag", "qyFlag"));

        return body;
    }
}
```

- [ ] **Step 4: 跑测试确认 GREEN**

```bash
cd /Users/muzhou/FEP_v1.0
./mvnw test -pl fep-collector -Dtest=QyRegister3109FieldMapperTest \
  -Dsurefire.failIfNoSpecifiedTests=false --no-transfer-progress
```
期望: `Tests run: 5, Failures: 0`

- [ ] **Step 5: 全模块 verify**

```bash
cd /Users/muzhou/FEP_v1.0
./mvnw verify -pl fep-collector --no-transfer-progress
```
期望: `BUILD SUCCESS`，全部测试 GREEN（v0.2: deferredMapper3109 test 已在 Task A2 Step 7 删除，本 step 期望 0 failure 0 编译错误 — 不再"已知失败放行"）

⚠️ **任何 RED 立即停下** — 不放行任何已知失败（红线 `feedback_full_regression_before_commit`）。

- [ ] **Step 6: 提交**

```bash
cd /Users/muzhou/FEP_v1.0
git add fep-collector/src/main/java/com/puchain/fep/collector/assembler/mapper/QyRegister3109FieldMapper.java
git add fep-collector/src/test/java/com/puchain/fep/collector/assembler/mapper/QyRegister3109FieldMapperTest.java
git commit -m "$(cat <<'EOF'
feat(collector): implement QyRegister3109FieldMapper (4 String required fields)

PRD §2.2.3 ✅ 数仓推荐 (信息登记); PRD §841 模式 3 信息发送.
Implements 4 required String fields per XSD 3109.xsd qyRegister3109 complexType:
SerialNo (uuid32 fallback) / SendNodeCode (from CollectorProperties.institutionCode) /
DesNodeCode (固定 HNDEMP) / qyFlag.
4 optional nested complex fields (hxqyInfo/qyAccLockInfo/PlatInfo/ExtInfo) remain
Javadoc stub.

v0.2: A2 Step 7 已删除 obsolete deferredMapper3109_throwsUnsupportedOperation test
+ unified stub mapper bean registration to props injection style. A3 commit clean.

Plan: docs/plans/2026-05-28-collector-mapper-mode3-boil-lake.md §A3 (v0.2)
PRD: §4 §6.2 line 798 + §841 模式 3 + §2.2.3
FR-ID: FR-MSG-3109-COLLECTOR-MAPPER

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

## Task A4: 3116 BankCheckDay 实装 + CheckDetailInfo nested list + unit test `模式 B`

**PRD 依据:** v1.3 §4 报文接入 + §6.2 line 802 (3116 银行资金日对账) + §841 模式 3 + §2.2.3 ✅ 日终对账推荐
**追溯 ID:** FR-MSG-3116-COLLECTOR-MAPPER (新增子项)
**XSD ref:** `fep-processor/src/main/resources/xsd/3116.xsd` complexType `BankCheckDay3116` (line 45-) + `CheckDetailInfo` (line 95-180)

**字段映射表 — BankCheckDay3116 顶层 (9 字段):**

| 字段 | XSD type | minOccurs | mapper 处理 |
|---|---|---|---|
| SerialNo | SerialNo | required | `serial_no` raw + uuid32 兜底 |
| SendNodeCode | NodeCode | required | `requireInstitutionCode()` |
| DesNodeCode | NodeCode | required | `DES_NODE_CODE_HNDEMP_CENTER` |
| hxqyName | qyName | required | `hxqy_name` raw, requireString |
| hxqyCode | qyCode | required | `hxqy_code` raw, requireString |
| CheckDate | Date | required | `check_date` raw, requireString |
| CheckDetailNum | Integer | required | `check_detail_num` raw, requireString |
| **CheckDetailInfo** | List<CheckDetailInfo> | **required** maxOccurs="200" | `check_detail_info` raw 为 `List<Map<String,Object>>`，私有 `mapDetail()` 递归映射 |
| ExtInfo | extInfo | optional | ⏸ Javadoc stub |

**字段映射表 — CheckDetailInfo 每条 (17 字段):**

| 字段 | XSD type | minOccurs | mapper 处理 |
|---|---|---|---|
| sid | Integer | required | `sid`, requireString |
| PlatNodeCode | NodeCode | required | `plat_node_code`, requireString |
| pzNo | pzNo | optional | `pz_no`, applyOptional |
| BizType | Number1to2 | required | `biz_type`, requireString |
| BillNo | String0to30 | optional | `bill_no`, applyOptional |
| rzqyName | qyName | required | `rzqy_name`, requireString |
| rzqyCode | qyCode | required | `rzqy_code`, requireString |
| rzAmt | Currency | required | `rz_amt`, requireString |
| rzRate | Rate | required | `rz_rate`, requireString |
| rzStartDate | Date | required | `rz_start_date`, requireString |
| rzEndDate | Date | required | `rz_end_date`, requireString |
| **Amt** | Currency default="0.00" | **required** | `amt`, requireString（XSD default ≠ optional，R-NEW-1 教训） |
| RepayStyle | String0to100 | optional | `repay_style`, applyOptional |
| lxAmt | Currency default="0.00" | optional | `lx_amt`, applyOptional |
| dbAmt | Currency default="0.00" | optional | `db_amt`, applyOptional |
| PlatServiceAmt | Currency default="0.00" | optional | `plat_service_amt`, applyOptional |
| CheckMemo | String0to100 | optional | `check_memo`, applyOptional |

**验收标准:**

1. 顶层 7 String 必填 + uuid32 兜底 + 1 List required + 1 嵌套 optional Javadoc stub
2. CheckDetailInfo 每条 10 必填字段 + 7 可选字段映射；list 大小 ≥1 且 ≤200
3. raw["check_detail_info"] 非 List 或空 list → 抛 `FepBusinessException(COLLECT_ASSEMBLE_FAILURE)`
4. happy path × 1 + 必填缺失 × 3 + list 边界 × 2 + institutionCode 1 异常 = ~7 测试

**Files:**
- Modify: `fep-collector/src/main/java/com/puchain/fep/collector/assembler/mapper/BankCheckDay3116FieldMapper.java`
- Create: `fep-collector/src/test/java/com/puchain/fep/collector/assembler/mapper/BankCheckDay3116FieldMapperTest.java`

- [ ] **Step 1: 写 BankCheckDay3116FieldMapperTest 失败 (RED)**

```java
// fep-collector/src/test/java/com/puchain/fep/collector/assembler/mapper/BankCheckDay3116FieldMapperTest.java
package com.puchain.fep.collector.assembler.mapper;

import com.puchain.fep.collector.CollectorProperties;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.processor.body.supplychain.BankCheckDay3116;
import com.puchain.fep.processor.body.supplychain.CheckDetailInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BankCheckDay3116FieldMapperTest {

    private static final String VALID_INSTITUTION_CODE = "A1000143000999";

    private CollectorProperties props;
    private BankCheckDay3116FieldMapper mapper;

    @BeforeEach
    void setup() {
        props = new CollectorProperties();
        props.setInstitutionCode(VALID_INSTITUTION_CODE);
        mapper = new BankCheckDay3116FieldMapper(props);
    }

    @Test
    void happyPath_oneDetail_shouldFillBody() {
        Map<String, Object> raw = baseRequiredTop();
        raw.put("check_detail_info", List.of(baseRequiredDetail()));

        BankCheckDay3116 body = (BankCheckDay3116) mapper.toMessageBody(raw);

        assertThat(body.getSerialNo()).isNotBlank();
        assertThat(body.getSendNodeCode()).isEqualTo(VALID_INSTITUTION_CODE);
        assertThat(body.getHxqyName()).isEqualTo("核心企业 A");
        assertThat(body.getHxqyCode()).isEqualTo("91110000111111111X");
        assertThat(body.getCheckDate()).isEqualTo("20261128");
        assertThat(body.getCheckDetailNum()).isEqualTo("1");
        assertThat(body.getCheckDetailInfo()).hasSize(1);

        CheckDetailInfo detail = body.getCheckDetailInfo().get(0);
        assertThat(detail.getSid()).isEqualTo("1");
        assertThat(detail.getPlatNodeCode()).isEqualTo("A1000143000888");
        assertThat(detail.getBizType()).isEqualTo("01");
        assertThat(detail.getRzqyName()).isEqualTo("融资企业 A");
        assertThat(detail.getRzqyCode()).isEqualTo("91110000222222222Y");
        assertThat(detail.getRzAmt()).isEqualTo("100000.00");
        assertThat(detail.getRzRate()).isEqualTo("0.0480");
        assertThat(detail.getRzStartDate()).isEqualTo("20261101");
        assertThat(detail.getRzEndDate()).isEqualTo("20261131");
        assertThat(detail.getAmt()).isEqualTo("100000.00");
        assertThat(detail.getPzNo()).isNull();
        assertThat(detail.getBillNo()).isNull();
        assertThat(detail.getRepayStyle()).isNull();
        assertThat(body.getExtInfo()).isNull();
    }

    @Test
    void missingHxqyName_top_shouldThrow() {
        Map<String, Object> raw = baseRequiredTop();
        raw.remove("hxqy_name");
        raw.put("check_detail_info", List.of(baseRequiredDetail()));
        assertThatThrownBy(() -> mapper.toMessageBody(raw))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("missing required field for 3116: hxqyName");
    }

    @Test
    void missingCheckDetailInfoList_shouldThrow() {
        Map<String, Object> raw = baseRequiredTop();
        assertThatThrownBy(() -> mapper.toMessageBody(raw))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("missing required field for 3116: checkDetailInfo");
    }

    @Test
    void emptyCheckDetailInfoList_shouldThrow() {
        Map<String, Object> raw = baseRequiredTop();
        raw.put("check_detail_info", new ArrayList<>());
        assertThatThrownBy(() -> mapper.toMessageBody(raw))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("missing required field for 3116: checkDetailInfo")
                .hasMessageContaining("non-empty");
    }

    @Test
    void detailMissingRzAmt_shouldThrow() {
        Map<String, Object> raw = baseRequiredTop();
        Map<String, Object> detail = baseRequiredDetail();
        detail.remove("rz_amt");
        raw.put("check_detail_info", List.of(detail));
        assertThatThrownBy(() -> mapper.toMessageBody(raw))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("missing required field for 3116")
                .hasMessageContaining("rzAmt");
    }

    @Test
    void listSizeExceeds200_shouldThrow() {
        Map<String, Object> raw = baseRequiredTop();
        List<Map<String, Object>> big = new ArrayList<>();
        for (int i = 0; i < 201; i++) {
            big.add(baseRequiredDetail());
        }
        raw.put("check_detail_info", big);
        assertThatThrownBy(() -> mapper.toMessageBody(raw))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("checkDetailInfo")
                .hasMessageContaining("max 200");
    }

    @Test
    void institutionCodeMissing_shouldThrow() {
        props.setInstitutionCode(null);
        Map<String, Object> raw = baseRequiredTop();
        raw.put("check_detail_info", List.of(baseRequiredDetail()));
        assertThatThrownBy(() -> mapper.toMessageBody(raw))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("missing required field for 3116: sendNodeCode");
    }

    private static Map<String, Object> baseRequiredTop() {
        Map<String, Object> raw = new HashMap<>();
        raw.put("serial_no", "FIXSERIAL000000000000000000004");
        raw.put("hxqy_name", "核心企业 A");
        raw.put("hxqy_code", "91110000111111111X");
        raw.put("check_date", "20261128");
        raw.put("check_detail_num", "1");
        return raw;
    }

    private static Map<String, Object> baseRequiredDetail() {
        Map<String, Object> d = new HashMap<>();
        d.put("sid", "1");
        d.put("plat_node_code", "A1000143000888");
        d.put("biz_type", "01");
        d.put("rzqy_name", "融资企业 A");
        d.put("rzqy_code", "91110000222222222Y");
        d.put("rz_amt", "100000.00");
        d.put("rz_rate", "0.0480");
        d.put("rz_start_date", "20261101");
        d.put("rz_end_date", "20261131");
        d.put("amt", "100000.00");
        return d;
    }
}
```

- [ ] **Step 2: 跑测试确认 RED**

```bash
cd /Users/muzhou/FEP_v1.0
./mvnw test -pl fep-collector -Dtest=BankCheckDay3116FieldMapperTest \
  -Dsurefire.failIfNoSpecifiedTests=false --no-transfer-progress
```
期望: 测试全 RED (stub 抛 UnsupportedOperationException)

- [ ] **Step 3: 实装 BankCheckDay3116FieldMapper（含 nested list 支持）**

```java
// fep-collector/src/main/java/com/puchain/fep/collector/assembler/mapper/BankCheckDay3116FieldMapper.java
package com.puchain.fep.collector.assembler.mapper;

import com.puchain.fep.collector.CollectorProperties;
import com.puchain.fep.common.domain.FepErrorCode;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.processor.body.supplychain.BankCheckDay3116;
import com.puchain.fep.processor.body.supplychain.CheckDetailInfo;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 3116 银行资金日对账 FieldMapper (Plan §A4, 7 String 必填 + CheckDetailInfo nested list)。
 *
 * <p>PRD §2.2.3 ✅ 数仓推荐场景；PRD §841 模式 3 信息发送。
 *
 * <p><b>CheckDetailInfo nested list</b>：raw["check_detail_info"] 期望类型为
 * {@code List<Map<String, Object>>}，每个 Map 含 10 必填 + 7 可选字段。
 * 列表大小须 1-200（XSD maxOccurs="200"，minOccurs=1）。
 *
 * <p><b>ExtInfo（顶层 optional）</b>：Javadoc stub。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class BankCheckDay3116FieldMapper extends AbstractFieldMapper {

    /** XSD 3116.xsd CheckDetailInfo maxOccurs="200"。 */
    private static final int CHECK_DETAIL_MAX_SIZE = 200;

    public BankCheckDay3116FieldMapper(final CollectorProperties props) {
        super(props, "3116");
    }

    @Override
    public Object toMessageBody(final Map<String, Object> rawData) {
        Objects.requireNonNull(rawData, "rawData");

        final BankCheckDay3116 body = new BankCheckDay3116();

        body.setSerialNo(serialNoOrFallback(rawData));
        body.setSendNodeCode(requireInstitutionCode());
        body.setDesNodeCode(DES_NODE_CODE_HNDEMP_CENTER);
        body.setHxqyName(requireString(rawData, "hxqy_name", "hxqyName"));
        body.setHxqyCode(requireString(rawData, "hxqy_code", "hxqyCode"));
        body.setCheckDate(requireString(rawData, "check_date", "checkDate"));
        body.setCheckDetailNum(requireString(rawData, "check_detail_num", "checkDetailNum"));
        body.setCheckDetailInfo(requireCheckDetailInfoList(rawData));

        return body;
    }

    @SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
            justification = "异常 message 已 LogSanitizer wrap")
    private List<CheckDetailInfo> requireCheckDetailInfoList(final Map<String, Object> rawData) {
        final Object raw = rawData.get("check_detail_info");
        if (!(raw instanceof List<?> list) || list.isEmpty()) {
            throw new FepBusinessException(
                    FepErrorCode.COLLECT_ASSEMBLE_FAILURE,
                    "missing required field for 3116: checkDetailInfo "
                            + "(expected non-empty List, got "
                            + LogSanitizer.sanitize(raw == null ? "null"
                                    : raw.getClass().getSimpleName()) + ")");
        }
        if (list.size() > CHECK_DETAIL_MAX_SIZE) {
            throw new FepBusinessException(
                    FepErrorCode.COLLECT_ASSEMBLE_FAILURE,
                    "checkDetailInfo size " + list.size()
                            + " exceeds max " + CHECK_DETAIL_MAX_SIZE);
        }
        final List<CheckDetailInfo> result = new ArrayList<>(list.size());
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> rawDetail)) {
                throw new FepBusinessException(
                        FepErrorCode.COLLECT_ASSEMBLE_FAILURE,
                        "checkDetailInfo item must be Map, got "
                                + LogSanitizer.sanitize(
                                        item == null ? "null" : item.getClass().getSimpleName()));
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> typed = (Map<String, Object>) rawDetail;
            result.add(mapDetail(typed));
        }
        return result;
    }

    private CheckDetailInfo mapDetail(final Map<String, Object> rawDetail) {
        final CheckDetailInfo d = new CheckDetailInfo();

        d.setSid(requireString(rawDetail, "sid", "sid"));
        d.setPlatNodeCode(requireString(rawDetail, "plat_node_code", "platNodeCode"));
        d.setBizType(requireString(rawDetail, "biz_type", "bizType"));
        d.setRzqyName(requireString(rawDetail, "rzqy_name", "rzqyName"));
        d.setRzqyCode(requireString(rawDetail, "rzqy_code", "rzqyCode"));
        d.setRzAmt(requireString(rawDetail, "rz_amt", "rzAmt"));
        d.setRzRate(requireString(rawDetail, "rz_rate", "rzRate"));
        d.setRzStartDate(requireString(rawDetail, "rz_start_date", "rzStartDate"));
        d.setRzEndDate(requireString(rawDetail, "rz_end_date", "rzEndDate"));
        d.setAmt(requireString(rawDetail, "amt", "amt"));

        applyOptional(rawDetail, "pz_no", d::setPzNo);
        applyOptional(rawDetail, "bill_no", d::setBillNo);
        applyOptional(rawDetail, "repay_style", d::setRepayStyle);
        applyOptional(rawDetail, "lx_amt", d::setLxAmt);
        applyOptional(rawDetail, "db_amt", d::setDbAmt);
        applyOptional(rawDetail, "plat_service_amt", d::setPlatServiceAmt);
        applyOptional(rawDetail, "check_memo", d::setCheckMemo);

        return d;
    }
}
```

- [ ] **Step 4: 跑测试确认 GREEN**

```bash
cd /Users/muzhou/FEP_v1.0
./mvnw test -pl fep-collector -Dtest=BankCheckDay3116FieldMapperTest \
  -Dsurefire.failIfNoSpecifiedTests=false --no-transfer-progress
```
期望: `Tests run: 7, Failures: 0`

- [ ] **Step 5: 全模块 verify**

```bash
cd /Users/muzhou/FEP_v1.0
./mvnw verify -pl fep-collector --no-transfer-progress
```
期望: `BUILD SUCCESS`，0 failure（v0.2 修复后 A3 不再产生已知失败）

- [ ] **Step 6: 提交**

```bash
cd /Users/muzhou/FEP_v1.0
git add fep-collector/src/main/java/com/puchain/fep/collector/assembler/mapper/BankCheckDay3116FieldMapper.java
git add fep-collector/src/test/java/com/puchain/fep/collector/assembler/mapper/BankCheckDay3116FieldMapperTest.java
git commit -m "$(cat <<'EOF'
feat(collector): implement BankCheckDay3116FieldMapper with CheckDetailInfo nested list

PRD §2.2.3 ✅ 数仓推荐 (日终对账); PRD §841 模式 3 信息发送.
Implements 7 top-level required String fields + CheckDetailInfo nested list (10
required + 7 optional fields each, size 1-200 per XSD maxOccurs).
ExtInfo top-level optional remains Javadoc stub.

Per muzhou 2026-05-28 AskUserQuestion 决策: 3116 mapper 含 CheckDetailInfo nested
list 实装（非 stub），因 CheckDetailInfo 是 XSD required (无 minOccurs="0")，stub
会导致下游 XsdValidator 拒绝 (R-NEW-1 同源教训).

Plan: docs/plans/2026-05-28-collector-mapper-mode3-boil-lake.md §A4
PRD: §4 §6.2 line 802 + §841 模式 3 + §2.2.3
FR-ID: FR-MSG-3116-COLLECTOR-MAPPER

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

## Task A5: 3009 RzReturnInfo 实装 + unit test `模式 B`

**PRD 依据:** v1.3 §4 报文接入 + §6.2 line 791 (3009 电子凭证融资结果登记) + §841 模式 3 信息发送
**追溯 ID:** FR-MSG-3009-COLLECTOR-MAPPER (新增子项)
**XSD ref:** `fep-processor/src/main/resources/xsd/3009.xsd` complexType `rzReturnInfo3009` (line 45-)

**字段映射表:**

| 字段 | XSD type | minOccurs | mapper 处理 |
|---|---|---|---|
| SerialNo | SerialNo | required | uuid32 兜底 |
| SendNodeCode | NodeCode | required | requireInstitutionCode |
| DesNodeCode | NodeCode | required | HNDEMP 固定 |
| PlatApplyNo | String0to100 | required | `plat_apply_no`, requireString |
| hxqyName | qyName | required | `hxqy_name`, requireString |
| rzpzNo | pzNo | required | `rzpz_no`, requireString |
| rzPhaseCode | int pattern \d{2} | required | `rz_phase_code`, requireString |
| rzPhaseInfo | String0to100 | optional | `rz_phase_info`, applyOptional |
| rzAmtInfo | rzAmtInfo (嵌套) | optional | ⏸ Javadoc stub |
| dbInfo | dbInfo (嵌套) | optional | ⏸ Javadoc stub |
| ExtInfo | extInfo (嵌套) | optional | ⏸ Javadoc stub |

**验收标准:** happy path × 1 + 4 必填缺失 × 4 + rzPhaseInfo optional skip × 1 + uuid32 fallback × 1 + institutionCode 2 异常 = ~9 测试

**Files:**
- Modify: `fep-collector/src/main/java/com/puchain/fep/collector/assembler/mapper/RzReturnInfo3009FieldMapper.java`
- Create: `fep-collector/src/test/java/com/puchain/fep/collector/assembler/mapper/RzReturnInfo3009FieldMapperTest.java`

- [ ] **Step 1: 写 RzReturnInfo3009FieldMapperTest 失败 (RED)**

```java
// fep-collector/src/test/java/com/puchain/fep/collector/assembler/mapper/RzReturnInfo3009FieldMapperTest.java
package com.puchain.fep.collector.assembler.mapper;

import com.puchain.fep.collector.CollectorProperties;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.FepConstants;
import com.puchain.fep.processor.body.supplychain.RzReturnInfo3009;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RzReturnInfo3009FieldMapperTest {

    private static final String VALID_INSTITUTION_CODE = "A1000143000999";

    private CollectorProperties props;
    private RzReturnInfo3009FieldMapper mapper;

    @BeforeEach
    void setup() {
        props = new CollectorProperties();
        props.setInstitutionCode(VALID_INSTITUTION_CODE);
        mapper = new RzReturnInfo3009FieldMapper(props);
    }

    @Test
    void happyPath_allRequired_shouldFillBody() {
        Map<String, Object> raw = baseRequired();
        raw.put("rz_phase_info", "审批通过");

        RzReturnInfo3009 body = (RzReturnInfo3009) mapper.toMessageBody(raw);

        assertThat(body.getSerialNo()).isNotBlank();
        assertThat(body.getSendNodeCode()).isEqualTo(VALID_INSTITUTION_CODE);
        assertThat(body.getDesNodeCode()).isEqualTo(FepConstants.HNDEMP_NODE_CODE);
        assertThat(body.getPlatApplyNo()).isEqualTo("PLAT202611280001");
        assertThat(body.getHxqyName()).isEqualTo("核心企业 A");
        assertThat(body.getRzpzNo()).isEqualTo("PZ202611280001");
        assertThat(body.getRzPhaseCode()).isEqualTo("99");
        assertThat(body.getRzPhaseInfo()).isEqualTo("审批通过");
        assertThat(body.getRzAmtInfo()).isNull();
        assertThat(body.getDbInfo()).isNull();
        assertThat(body.getExtInfo()).isNull();
    }

    @ParameterizedTest
    @CsvSource({
            "plat_apply_no, platApplyNo",
            "hxqy_name, hxqyName",
            "rzpz_no, rzpzNo",
            "rz_phase_code, rzPhaseCode"
    })
    void missingRequired_shouldThrow(String rawKey, String logicalField) {
        Map<String, Object> raw = baseRequired();
        raw.remove(rawKey);
        assertThatThrownBy(() -> mapper.toMessageBody(raw))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("missing required field for 3009")
                .hasMessageContaining(logicalField);
    }

    @Test
    void optionalRzPhaseInfoMissing_shouldSkip() {
        RzReturnInfo3009 body = (RzReturnInfo3009) mapper.toMessageBody(baseRequired());
        assertThat(body.getRzPhaseInfo()).isNull();
    }

    @Test
    void serialNoMissing_shouldFallbackToUuid32() {
        Map<String, Object> raw = baseRequired();
        raw.remove("serial_no");
        RzReturnInfo3009 body = (RzReturnInfo3009) mapper.toMessageBody(raw);
        assertThat(body.getSerialNo()).isNotNull().hasSize(30);
    }

    @Test
    void institutionCodeMissing_shouldThrow() {
        props.setInstitutionCode(null);
        assertThatThrownBy(() -> mapper.toMessageBody(baseRequired()))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("missing required field for 3009: sendNodeCode");
    }

    @Test
    void institutionCodeInvalidLength_shouldThrow() {
        props.setInstitutionCode("SHORT");
        assertThatThrownBy(() -> mapper.toMessageBody(baseRequired()))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("institutionCode length must be 14");
    }

    private static Map<String, Object> baseRequired() {
        Map<String, Object> raw = new HashMap<>();
        raw.put("serial_no", "FIXSERIAL000000000000000000005");
        raw.put("plat_apply_no", "PLAT202611280001");
        raw.put("hxqy_name", "核心企业 A");
        raw.put("rzpz_no", "PZ202611280001");
        raw.put("rz_phase_code", "99");
        return raw;
    }
}
```

- [ ] **Step 2: 跑测试确认 RED**

```bash
cd /Users/muzhou/FEP_v1.0
./mvnw test -pl fep-collector -Dtest=RzReturnInfo3009FieldMapperTest \
  -Dsurefire.failIfNoSpecifiedTests=false --no-transfer-progress
```
期望: 测试 RED

- [ ] **Step 3: 实装 RzReturnInfo3009FieldMapper**

```java
// fep-collector/src/main/java/com/puchain/fep/collector/assembler/mapper/RzReturnInfo3009FieldMapper.java
package com.puchain.fep.collector.assembler.mapper;

import com.puchain.fep.collector.CollectorProperties;
import com.puchain.fep.common.util.IdGenerator;
import com.puchain.fep.processor.body.supplychain.RzReturnInfo3009;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * 3009 电子凭证融资结果登记 FieldMapper (Plan §A5, 7 String 必填 + 1 String 可选)。
 *
 * <p>PRD §841 模式 3 信息发送（受理单位主动报送融资结果）。
 *
 * <p><b>嵌套 complex 字段（rzAmtInfo / dbInfo / ExtInfo）暂留 stub</b>：
 * XSD 3009.xsd 中 3 嵌套字段均 {@code minOccurs="0"}，mapper 不调对应 setter。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class RzReturnInfo3009FieldMapper extends AbstractFieldMapper {

    public RzReturnInfo3009FieldMapper(final CollectorProperties props) {
        super(props, "3009");
    }

    @Override
    public Object toMessageBody(final Map<String, Object> rawData) {
        Objects.requireNonNull(rawData, "rawData");

        final RzReturnInfo3009 body = new RzReturnInfo3009();

        body.setSerialNo(serialNoOrFallback(rawData));
        body.setSendNodeCode(requireInstitutionCode());
        body.setDesNodeCode(DES_NODE_CODE_HNDEMP_CENTER);
        body.setPlatApplyNo(requireString(rawData, "plat_apply_no", "platApplyNo"));
        body.setHxqyName(requireString(rawData, "hxqy_name", "hxqyName"));
        body.setRzpzNo(requireString(rawData, "rzpz_no", "rzpzNo"));
        body.setRzPhaseCode(requireString(rawData, "rz_phase_code", "rzPhaseCode"));

        applyOptional(rawData, "rz_phase_info", body::setRzPhaseInfo);

        return body;
    }
}
```

- [ ] **Step 4: 跑测试确认 GREEN**

```bash
cd /Users/muzhou/FEP_v1.0
./mvnw test -pl fep-collector -Dtest=RzReturnInfo3009FieldMapperTest \
  -Dsurefire.failIfNoSpecifiedTests=false --no-transfer-progress
```
期望: `Tests run: 9 (含 ParameterizedTest 展开), Failures: 0`

- [ ] **Step 5: 全模块 verify**

```bash
cd /Users/muzhou/FEP_v1.0
./mvnw verify -pl fep-collector --no-transfer-progress
```
期望: `BUILD SUCCESS`，0 failure（v0.2 修复后无已知失败）

- [ ] **Step 6: 提交**

```bash
cd /Users/muzhou/FEP_v1.0
git add fep-collector/src/main/java/com/puchain/fep/collector/assembler/mapper/RzReturnInfo3009FieldMapper.java
git add fep-collector/src/test/java/com/puchain/fep/collector/assembler/mapper/RzReturnInfo3009FieldMapperTest.java
git commit -m "$(cat <<'EOF'
feat(collector): implement RzReturnInfo3009FieldMapper (7 String required + 1 optional)

PRD §841 模式 3 信息发送（受理单位主动报送融资结果）.
Implements 7 String required fields per XSD 3009.xsd rzReturnInfo3009 complexType:
SerialNo (uuid32 fallback) / SendNodeCode / DesNodeCode / PlatApplyNo / hxqyName /
rzpzNo / rzPhaseCode + 1 optional String (rzPhaseInfo).
3 nested optional complex fields (rzAmtInfo/dbInfo/ExtInfo) remain Javadoc stub.

Plan: docs/plans/2026-05-28-collector-mapper-mode3-boil-lake.md §A5
PRD: §4 §6.2 line 791 + §841 模式 3
FR-ID: FR-MSG-3009-COLLECTOR-MAPPER

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

## Task A6: Closing — 集成测试扩展 + 文档更新 + push `模式 A`

**PRD 依据:** v1.3 §9 全局规范（文档治理） + 项目 Plan 治理流程
**追溯 ID:** Plan A 闭环（5 FR-MSG-* + 1 FR-COMM-COLLECTOR-MAPPER-HELPER）

**目标:**
1. 修正 `DefaultPayloadAssemblerTest.assemble_deferredMapper3109_throwsUnsupportedOperation` 测试（3109 已实装不再抛 UnsupportedOperationException）
2. 扩展 `DefaultPayloadAssemblerTest` 加 3 个 happy path 集成 case（3109/3116/3009）
3. 全模块 `mvn verify` 全 GREEN
4. 更新 `docs/plans/prd-traceability-matrix.md`（FR-MSG-3009/3109/3116 注释扩展 + 新增 FR-COMM-COLLECTOR-MAPPER-HELPER row）
5. 更新 `docs/plans/PHASE_HISTORY.md` 加 Plan A 行
6. 更新 `/Users/muzhou/FEP/CLAUDE.md` "当前项目状态" 段（file write only, 红线 `feedback_fep_docs_repo_commit_taboo`）
7. git push + 创建 PR
8. **v0.2 升级**：5 个 *XsdComplianceTest（spec §5.3 设计）直接在 Plan A 内 Task A7 实装（muzhou 2026-05-28 AskUserQuestion 决策 — 不 defer Plan A.3）— 跨 fep-web 模块写入 5 个测试，真 `XsdValidator` on SUT 验证 5 mapper 产出 XSD 合规

**验收标准:**

1. `DefaultPayloadAssemblerTest` 全 GREEN（含修正后的 3109 test + 3 新加 happy path 集成 case）
2. `./mvnw verify -pl fep-collector` `BUILD SUCCESS` 0 failure
3. PRD 矩阵 FR-MSG-3009/3109/3116 注释含 "+ collector mapper N 字段实装" + 新 FR-COMM-COLLECTOR-MAPPER-HELPER row
4. PHASE_HISTORY 新增 1 行 Plan A 信息
5. CLAUDE.md "下一步候选" 段移除 "数仓 collector field mapper 实装" item（已部分完成 Mode3，剩 Mode2 由 Plan B 接手）
6. git push 到 `refactor/collector-mapper-mode3-boil-lake` 分支
7. PR #N 创建带 muzhou 签字豁免 400 行 PR 大小要求

**Files:**
- Modify: `fep-collector/src/test/java/com/puchain/fep/collector/assembler/DefaultPayloadAssemblerTest.java`
- Modify: `docs/plans/prd-traceability-matrix.md`
- Modify: `docs/plans/PHASE_HISTORY.md`
- Modify: `/Users/muzhou/FEP/CLAUDE.md` (file write only, 非 git tracked)

- [ ] **Step 1: 修正 DefaultPayloadAssemblerTest 中 deferredMapper3109 test 方法**

```java
// 修改 fep-collector/src/test/java/com/puchain/fep/collector/assembler/DefaultPayloadAssemblerTest.java
// line 183 附近，替换 assemble_deferredMapper3109_throwsUnsupportedOperation() 测试方法

// 原方法（删除）:
//   /** Plan §6 acceptance: deferred mapper invocation → UnsupportedOperationException. */
//   @Test
//   void assemble_deferredMapper3109_throwsUnsupportedOperation() {
//       assertThatThrownBy(() -> assembler.assemble(record(
//               Mode3Routes.PAYLOAD_TYPE_QY_REGISTER_3109, Map.of())))
//               .isInstanceOf(UnsupportedOperationException.class)
//               .hasMessageContaining("mapper not implemented")
//               .hasMessageContaining("3109");
//   }

// 替换为 3 个 happy path 集成 case（3109/3116/3009）:

/** Plan A §A6: 3109 mapper 已实装，验证集成 happy path。 */
@Test
void assemble_qyRegister3109_happyPath() {
    Map<String, Object> raw = new HashMap<>();
    raw.put("qy_flag", "1");
    Object body = assembler.assemble(record(
            Mode3Routes.PAYLOAD_TYPE_QY_REGISTER_3109, raw)).messageBody();
    assertThat(body).isInstanceOf(
            com.puchain.fep.processor.body.supplychain.QyRegister3109.class);
    com.puchain.fep.processor.body.supplychain.QyRegister3109 qy =
            (com.puchain.fep.processor.body.supplychain.QyRegister3109) body;
    assertThat(qy.getQyFlag()).isEqualTo("1");
    assertThat(qy.getSendNodeCode()).isEqualTo(props.getInstitutionCode());
}

/** Plan A §A6: 3116 mapper 已实装含 CheckDetailInfo nested list。 */
@Test
void assemble_bankCheckDay3116_happyPath() {
    Map<String, Object> detail = new HashMap<>();
    detail.put("sid", "1");
    detail.put("plat_node_code", "A1000143000888");
    detail.put("biz_type", "01");
    detail.put("rzqy_name", "融资企业 A");
    detail.put("rzqy_code", "91110000222222222Y");
    detail.put("rz_amt", "100000.00");
    detail.put("rz_rate", "0.0480");
    detail.put("rz_start_date", "20261101");
    detail.put("rz_end_date", "20261131");
    detail.put("amt", "100000.00");

    Map<String, Object> raw = new HashMap<>();
    raw.put("hxqy_name", "核心企业 A");
    raw.put("hxqy_code", "91110000111111111X");
    raw.put("check_date", "20261128");
    raw.put("check_detail_num", "1");
    raw.put("check_detail_info", List.of(detail));

    Object body = assembler.assemble(record(
            Mode3Routes.PAYLOAD_TYPE_BANK_CHECK_DAY_3116, raw)).messageBody();
    assertThat(body).isInstanceOf(
            com.puchain.fep.processor.body.supplychain.BankCheckDay3116.class);
    com.puchain.fep.processor.body.supplychain.BankCheckDay3116 b =
            (com.puchain.fep.processor.body.supplychain.BankCheckDay3116) body;
    assertThat(b.getCheckDetailInfo()).hasSize(1);
}

/** Plan A §A6: 3009 mapper 已实装。 */
@Test
void assemble_rzReturnInfo3009_happyPath() {
    Map<String, Object> raw = new HashMap<>();
    raw.put("plat_apply_no", "PLAT202611280001");
    raw.put("hxqy_name", "核心企业 A");
    raw.put("rzpz_no", "PZ202611280001");
    raw.put("rz_phase_code", "99");

    Object body = assembler.assemble(record(
            Mode3Routes.PAYLOAD_TYPE_RZ_RETURN_3009, raw)).messageBody();
    assertThat(body).isInstanceOf(
            com.puchain.fep.processor.body.supplychain.RzReturnInfo3009.class);
}
```

注意：v0.2 修订 — `.body()` 已 grep 实测改为 `.messageBody()`（与 `assemble_contract3101_happyPath` line 90-100 已有测试的 `assembler.assemble(...).messageBody()` 风格一致）。

- [ ] **Step 2: 跑 DefaultPayloadAssemblerTest 全套确认 GREEN**

```bash
cd /Users/muzhou/FEP_v1.0
./mvnw test -pl fep-collector -Dtest=DefaultPayloadAssemblerTest \
  -Dsurefire.failIfNoSpecifiedTests=false --no-transfer-progress
```
期望: `BUILD SUCCESS`，全部测试 GREEN

- [ ] **Step 3: 全 reactor verify (fep-collector + 下游影响)**

```bash
cd /Users/muzhou/FEP_v1.0
./mvnw verify -pl fep-collector -am --no-transfer-progress
```
期望: `BUILD SUCCESS`，0 failure（CI 兜底前置本地实测，红线 `feedback_spotbugs_baseline_rot` 不信任快照）

⚠️ 若失败：定位失败模块/测试，必要时 fallback 单模块 verify + 注释跳过 plugin（红线 `feedback_concurrent_work_blocks_verify`）

- [ ] **Step 4: 更新 PRD 追溯矩阵**

```bash
cd /Users/muzhou/FEP_v1.0
# 实操：用 Edit 工具修改 docs/plans/prd-traceability-matrix.md
# 1. FR-MSG-3009 行末注释扩展：现 ✅ 加 "+ collector mapper 7+1 字段实装（Plan A `<sha>`）"
# 2. FR-MSG-3109 行末注释扩展：现 ✅ 加 "+ collector mapper 4 String 实装（Plan A `<sha>`）"
# 3. FR-MSG-3116 行末注释扩展：现 ✅ 加 "+ collector mapper 7 String + CheckDetailInfo nested list 实装（Plan A `<sha>`）"
# 4. FR-MSG-3101 行末注释扩展：现 ✅ 加 "+ dedicated unit test + AbstractFieldMapper refactor（Plan A `<sha>`）"
# 5. FR-MSG-3102 行末注释扩展：现 ✅ 加 "+ dedicated unit test + AbstractFieldMapper refactor（Plan A `<sha>`）"
# 6. 新增 FR-COMM 段下 1 行：
#    | FR-COMM-COLLECTOR-MAPPER-HELPER | §2.2.1/§2.2.2 双模式 mapper 共用 | AbstractFieldMapper 抽取（6 helpers + 3 constants，含 v0.2 新增 serialNoOrFallback XSD length=30 兜底） | Plan A `<sha>` | ✅ boil-lake refactor，红线 feedback_concern_boil_lake_when_cheap_and_safe + feedback_baseline_drift_during_long_review_cycle |
```

具体 `<sha>` 在 Step 7 commit 后回填。本步骤先用占位 `<sha>` 标记，commit 后做 second-pass 修正。

- [ ] **Step 5: 更新 PHASE_HISTORY.md 加 Plan A 行**

```bash
cd /Users/muzhou/FEP_v1.0
# 实操：用 Edit 工具在 docs/plans/PHASE_HISTORY.md 顶部表中新增 1 行
# | 2026-05-28 | Plan A: Collector Mapper Mode3 Boil-Lake | ✅ AbstractFieldMapper 抽取 + 3101/3102 refactor + 3109/3116/3009 实装（5 mapper + 4 unit test + 1 集成扩展） | `<sha>` | -- |
```

同时更新 PHASE_HISTORY.md 顶部 "当前进度（YYYY-MM-DD）" 日期到 `2026-05-28`（红线 `feedback_session_end_phase_history_metadata_drift_check`）

- [ ] **Step 6: 更新 /Users/muzhou/FEP/CLAUDE.md「当前项目状态」段 (file write only)**

CLAUDE.md "快照" 段 + "下一步候选" 段做以下修改（**file write only，不 git add/commit，红线 `feedback_fep_docs_repo_commit_taboo`**）:

```bash
# 1. "快照" 段 origin/main HEAD 更新到 Plan A merge sha
# 2. "本会话 ship 分支" 移除 PR #29 + 加 Plan A 信息（如 PR #30）
# 3. "最近里程碑" 顶部新增 1 行：
#    - 2026-05-28 **Plan A: Collector Mapper Mode3 Boil-Lake**（AbstractFieldMapper 抽取消除 8 份 helper 重复 + 3101/3102 refactor + 3109/3116/3009 实装；5 mapper + 4 unit test + 1 集成 case 扩展；命中红线 feedback_concern_boil_lake_when_cheap_and_safe；XsdComplianceTest deferred 到 Plan A.3）— PR #N `<sha>`
# 4. "下一步候选" 段：把 #9 "数仓模式 collector field mapper 实装" 改为 #9 "Plan B: Mode2 接口模式 3 mapper 实装 (3105/3107/3112)" + 新增 #10 "Plan A.3: Collector Mapper XsdComplianceTest 全 8 mapper 一波扫"
```

- [ ] **Step 7: 提交 Task A6 改动**

```bash
cd /Users/muzhou/FEP_v1.0
git add fep-collector/src/test/java/com/puchain/fep/collector/assembler/DefaultPayloadAssemblerTest.java
git add docs/plans/prd-traceability-matrix.md
git add docs/plans/PHASE_HISTORY.md
git add docs/plans/2026-05-28-collector-mapper-mode3-boil-lake.md
git commit -m "$(cat <<'EOF'
chore(plans): Plan A closing — DefaultPayloadAssemblerTest 扩展 + 矩阵/PHASE_HISTORY 更新

- 修正 DefaultPayloadAssemblerTest.assemble_deferredMapper3109_throwsUnsupportedOperation
  → 替换为 3 个 happy path 集成 case (3109/3116/3009)
- PRD 矩阵更新 FR-MSG-3009/3101/3102/3109/3116 注释扩展 + 新增 FR-COMM-COLLECTOR-MAPPER-HELPER
- PHASE_HISTORY 新增 Plan A 行 + 顶部日期同步到 2026-05-28
- Plan A 文档落盘 (本文件 in commit)

XsdComplianceTest 5 个 deferred 到独立 Plan A.3 (跨 fep-web 模块成本管理)。

Plan: docs/plans/2026-05-28-collector-mapper-mode3-boil-lake.md §A6
PRD 矩阵 status drift 修正（红线 feedback_prd_matrix_status_drift）

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

- [ ] **Step 8: PRD 矩阵 sha 回填 second-pass**

```bash
cd /Users/muzhou/FEP_v1.0
# 用上一 commit 的真 sha 替换 docs/plans/prd-traceability-matrix.md 里的占位 <sha>
git log --oneline -1   # 拿到 sha7
# Edit prd-traceability-matrix.md 全文替换 `<sha>` → 实 sha
git add docs/plans/prd-traceability-matrix.md
git commit -m "$(cat <<'EOF'
chore(plans): backfill PRD matrix Plan A sha references

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

- [ ] **Step 9: 写 CLAUDE.md file write only**

```bash
# Edit /Users/muzhou/FEP/CLAUDE.md per Step 6 描述（仅 file write，不 git add/commit）
# 红线 feedback_fep_docs_repo_commit_taboo: /FEP/docs/* + /FEP/CLAUDE.md 全部非 git tracked
```

- [ ] **Step 10: 推到 origin + 创建 PR**

```bash
cd /Users/muzhou/FEP_v1.0
git push -u origin refactor/collector-mapper-mode3-boil-lake

gh pr create \
  --title "feat(collector): Plan A — AbstractFieldMapper boil-lake + Mode3 3 mapper 实装" \
  --body "$(cat <<'EOF'
## Summary

Plan A 实施: docs/plans/2026-05-28-collector-mapper-mode3-boil-lake.md (muzhou 2026-05-28 ✅ 签字)

- Boil-lake refactor: 抽取 AbstractFieldMapper 基类提供 5 共用 helper + 3 常量，消除 3101/3102 helper 8 份重复
- 补 3101/3102 dedicated unit test (~24 cases，refactor safety net)
- 实装 Mode3 数仓模式 3 个 stub mapper:
  - 3109 QyRegister (4 String required，4 嵌套 optional stub)
  - 3116 BankCheckDay (7 String required + CheckDetailInfo nested list 10 required + 7 optional，max 200)
  - 3009 RzReturnInfo (7 String required + 1 optional，3 嵌套 optional stub)
- 扩展 DefaultPayloadAssemblerTest 3 happy path 集成 case
- Task A7: 5 个 XsdComplianceTest in fep-web (真 XsdValidator on SUT 验证 5 mapper 产出全 XSD 合规)
- PRD 矩阵 status drift 修正 + 新增 FR-COMM-COLLECTOR-MAPPER-HELPER

## PR 大小豁免

PR diff ~2200 行（mapper 骨架重复 80%+）— muzhou 2026-05-28 AskUserQuestion 已签字豁免 400 行硬上限。
Plan §6.3 PR 大小预算 / Plan §设计背景 §决策链 #4。

## Red-lines 遵守

- ✅ feedback_concern_boil_lake_when_cheap_and_safe（boil-lake 主驱动）
- ✅ feedback_prd_matrix_status_drift（A6 修正 6 FR-MSG 注释）
- ✅ feedback_plan_must_grep_actual_api（API/XSD 全 grep 实测）
- ✅ feedback_logsanitizer_alone_insufficient_for_findsecbugs（AbstractFieldMapper + 3116 nested 异常 wrap LogSanitizer + @SuppressFBWarnings）
- ✅ feedback_xsd_compliance_fix_real_validator_on_sut（XsdComplianceTest deferred 到 Plan A.3 明示）
- ✅ feedback_task_review_discipline（每 Task 派发独立 spec + quality reviewer）

## Test plan

- [x] Task A1 `ContractInfo3101FieldMapperTest` + `ArchiveInfo3102FieldMapperTest` GREEN
- [x] Task A2 `AbstractFieldMapperTest` + 3101/3102 refactor 零回归 GREEN
- [x] Task A3 `QyRegister3109FieldMapperTest` GREEN
- [x] Task A4 `BankCheckDay3116FieldMapperTest` + CheckDetailInfo nested 测试 GREEN
- [x] Task A5 `RzReturnInfo3009FieldMapperTest` GREEN
- [x] Task A6 `DefaultPayloadAssemblerTest` + 3 happy path 集成 case GREEN
- [x] 全模块 `./mvnw verify -pl fep-collector -am` BUILD SUCCESS
- [ ] CI 通过（待 GHA billing 解决）

## 下一步

- Plan B: Mode2 接口模式 3 mapper 实装 (3105/3107/3112) — Plan A merge 后启动
- v0.2: XsdComplianceTest 5 个已纳入本 Plan Task A7（muzhou 2026-05-28 决策不 defer）；3105/3107/3112 的 XsdComplianceTest 由 Plan B 同步补

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

- [ ] **Step 11: PR 提交后等 GHA 触发 + muzhou merge**

```bash
cd /Users/muzhou/FEP_v1.0
gh pr view --json url,state
# 等 muzhou merge 后回到 main 拉取
git checkout main
git pull origin main
```

如 GHA billing 仍阻塞 → 走红线 `feedback_systemic_ci_blocker_defers_positive_backing` 模式：tier-A 本机 verify 充分即 CLOSED，tier-B GHA 绿背书 deferred。

---

## Task A7: XsdComplianceTest 5 个 in fep-web `模式 A` (v0.2 新增)

**PRD 依据:** v1.3 §3.2.2 报文 XSD 合规 + §4 报文接入 + §6.2 报文清单
**追溯 ID:** FR-MSG-3101/3102/3009/3109/3116 全部 (collector mapper 产出真 XSD 合规守护)
**XSD ref:** `fep-processor/src/main/resources/xsd/{3101,3102,3009,3109,3116}.xsd` + `DataType.xsd`

**目标:** 5 个 mapper（3101/3102/3009/3109/3116）真 `XsdValidator` on SUT 集成测试，每 mapper 1 happy path 测试，验证 mapper.toMessageBody() 产出的 Body POJO 经 JAXB marshal 后通过真 XSD 校验。

**为什么这个 Task 在 fep-web 模块？** `fep-collector` 模块 ArchUnit invariant 禁依赖 `fep-converter`/`XsdValidator`。`fep-web` 已含 fep-collector + fep-converter + XsdValidator 依赖，参考 `fep-web/src/test/java/com/puchain/fep/web/outbound/consumer/OutboundEnvelopeXsdComplianceTest.java`（R-NEW-1 后实证模式 + 2026-05-28 `XsdTestSupport.pad30` follow-on）。

**验收标准:**

1. 5 个 *XsdComplianceTest 集成测试全 GREEN（每 mapper 1 个真 XsdValidator on SUT 测试）
2. Body POJO 实例经 JAXB marshal → 完整 CFX envelope XML → `XsdValidator.validateBodyXml(xml, msgNo)` 不抛
3. fixture SerialNo 用 30 字符（红线 `feedback_fixture_data_must_satisfy_xsd_constraints` + A1/A2 fixture 30 字符一致性）
4. 测试规约：`@SpringBootTest` + 真 `XsdValidator` bean 注入（沿用 OutboundEnvelopeXsdComplianceTest pattern），禁 `@MockBean XsdValidator`（红线 `feedback_xsd_compliance_fix_real_validator_on_sut`）
5. fixture USCI 18 位标准格式（如 `91110000111111111X`）、NodeCode 14 位（如 `A1000143000999`）、Currency 含小数（如 `100000.00`）

**Files:**
- Create: `fep-web/src/test/java/com/puchain/fep/web/collector/mapper/ContractInfo3101XsdComplianceTest.java`
- Create: `fep-web/src/test/java/com/puchain/fep/web/collector/mapper/ArchiveInfo3102XsdComplianceTest.java`
- Create: `fep-web/src/test/java/com/puchain/fep/web/collector/mapper/QyRegister3109XsdComplianceTest.java`
- Create: `fep-web/src/test/java/com/puchain/fep/web/collector/mapper/BankCheckDay3116XsdComplianceTest.java`
- Create: `fep-web/src/test/java/com/puchain/fep/web/collector/mapper/RzReturnInfo3009XsdComplianceTest.java`

- [ ] **Step 1: 创建包路径**

```bash
cd /Users/muzhou/FEP_v1.0
mkdir -p fep-web/src/test/java/com/puchain/fep/web/collector/mapper/
```

- [ ] **Step 2: 写 ContractInfo3101XsdComplianceTest（pattern reference for 其他 4 个）**

```java
// fep-web/src/test/java/com/puchain/fep/web/collector/mapper/ContractInfo3101XsdComplianceTest.java
package com.puchain.fep.web.collector.mapper;

import com.puchain.fep.collector.CollectorProperties;
import com.puchain.fep.collector.assembler.mapper.ContractInfo3101FieldMapper;
import com.puchain.fep.processor.body.supplychain.ContractInfo3101;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Task A7 (Plan A v0.2): 3101 ContractInfo mapper 真 XsdValidator on SUT 集成测试。
 *
 * <p>验证 {@link ContractInfo3101FieldMapper#toMessageBody(Map)} 产出经 JAXB marshal 后通过
 * 真 {@code XsdValidator} 校验（红线 feedback_xsd_compliance_fix_real_validator_on_sut）。
 *
 * <p>Pattern 沿用 {@code OutboundEnvelopeXsdComplianceTest}（R-NEW-1, 2026-05-27）。
 * fixture 字段值满足 DataType.xsd facet 约束（SerialNo length=30 / NodeCode 14 位 / USCI 18 位）。
 *
 * @since 1.0.0
 */
@SpringBootTest
@TestPropertySource(properties = {
        "fep.collector.scheduling.enabled=false",
        "fep.collector.institution-code=A1000143000999",
        "management.health.redis.enabled=false"
})
class ContractInfo3101XsdComplianceTest {

    @Autowired
    private ContractInfo3101FieldMapper mapper;

    @Autowired
    private CollectorProperties props;

    @Autowired
    private XsdComplianceHelper helper;  // v0.4: instance helper @Autowired

    @Test
    void mapperOutput_shouldPassRealXsdValidator() {
        Map<String, Object> raw = new HashMap<>();
        raw.put("serial_no", "FIX3101SERIAL00000000000000001");  // 30 chars
        raw.put("contract_no", "CON202611280001");
        raw.put("contract_type", "01");
        raw.put("digital_seal", "1");
        raw.put("contract_filename", "contract.pdf");
        raw.put("jfqy_name", "甲方公司");
        raw.put("yfqy_name", "乙方公司");
        // 6 可选字段
        raw.put("hxqy_code", "91110000111111111X");
        raw.put("cert_filename", "cert.pdf");
        raw.put("jfqy_code", "91110000222222222Y");
        raw.put("yfqy_code", "91110000333333333Z");
        raw.put("sx_date", "20261128");
        raw.put("qz_date", "20271128");

        ContractInfo3101 body = (ContractInfo3101) mapper.toMessageBody(raw);

        assertThatCode(() -> helper.validateMapperOutput("3101", body))
                .doesNotThrowAnyException();
    }
}
```

- [ ] **Step 3: 写共享 helper `XsdComplianceHelper`（避免 5 个 test 重复 JAXB+XsdValidator wiring）**

⚠️ v0.3 修订 — v0.2 reviewer 揪出真实 API：
- `XsdValidator` 真实包: `com.puchain.fep.processor.validation` (fep-processor 模块，不是 fep-converter)
- `MessageType` 真实包: `com.puchain.fep.converter.type`（fep-converter 模块）+ 静态工厂 `byMsgNo(String)` 返 `Optional<MessageType>`
- `XsdValidator.validate(MessageType, byte[]) → ValidationResult` (record: valid: boolean, errors: List<String>)
- helper 改 instance method + Spring `@Autowired` 注入（避免 v0.2 static/instance 矛盾）

```java
// fep-web/src/test/java/com/puchain/fep/web/collector/mapper/XsdComplianceHelper.java
package com.puchain.fep.web.collector.mapper;

import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.validation.ValidationResult;
import com.puchain.fep.processor.validation.XsdValidator;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Task A7 共用 helper — Body POJO → JAXB marshal → XsdValidator 真验证。
 * 由 5 个 XsdComplianceTest 复用（Rule-of-Three 前置抽取）。
 *
 * <p>v0.3: instance method + @Autowired，匹配 fep-web 现有
 * {@code OutboundEnvelopeXsdComplianceTest} 注入风格。
 *
 * @since 1.0.0
 */
@Component
public class XsdComplianceHelper {

    private final XsdValidator validator;

    @Autowired
    public XsdComplianceHelper(final XsdValidator validator) {
        this.validator = validator;
    }

    /**
     * 验证 Body POJO 经 JAXB marshal 后通过真 XSD 校验。
     *
     * @param msgNo Body 对应 msgNo (如 "3101")
     * @param body Body POJO 实例 (如 ContractInfo3101)
     * @throws Exception JAXB marshal 失败或 XSD 校验失败 (含 errors)
     */
    public void validateMapperOutput(final String msgNo, final Object body) throws Exception {
        final Optional<MessageType> type = MessageType.byMsgNo(msgNo);
        assertThat(type)
                .as("MessageType.byMsgNo(%s) must be registered", msgNo)
                .isPresent();

        final JAXBContext ctx = JAXBContext.newInstance(body.getClass());
        final Marshaller m = ctx.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.FALSE);
        final StringWriter sw = new StringWriter();
        m.marshal(body, sw);

        final ValidationResult result = validator.validate(
                type.get(), sw.toString().getBytes(StandardCharsets.UTF_8));
        assertThat(result.valid())
                .as("XSD validation must pass for msgNo=%s, errors=%s", msgNo, result.errors())
                .isTrue();
    }
}
```

⚠️ **implementer subagent 执行注意** (v0.3):
- 上述 import 已 grep 实测真实包路径 — 无需 implementer 重 grep
- `MessageType.byMsgNo("3101")` 返 `Optional<MessageType>` — 须 `.get()` 解包 (本 helper 内 `assertThat().isPresent()` 守护)
- `validator.validate(type, byte[])` 返 `ValidationResult` record — 内部 `result.valid()` boolean + `result.errors()` List<String>
- JAXB marshal body POJO 到 XML String → `getBytes(UTF_8)` 传入 validator
- 若需要外层 envelope（CFX HEAD + MSG），mapper 测试粒度仅验 Body POJO XSD 合规 — 与 outbound wire path 不同(那里需 envelope)

- [ ] **Step 4: 写其余 4 个 XsdComplianceTest（pattern mirror Step 2）**

每个测试文件结构同 Step 2，只改：
- 类名: `ArchiveInfo3102XsdComplianceTest` / `QyRegister3109XsdComplianceTest` / `BankCheckDay3116XsdComplianceTest` / `RzReturnInfo3009XsdComplianceTest`
- `@Autowired` mapper 类: `ArchiveInfo3102FieldMapper` / `QyRegister3109FieldMapper` / `BankCheckDay3116FieldMapper` / `RzReturnInfo3009FieldMapper`
- fixture: 对应 mapper 的必填字段 + 30 字符 SerialNo (例: `"FIX3102SERIAL00000000000000002"` / `"FIX3109SERIAL00000000000000003"` / `"FIX3116SERIAL00000000000000004"` / `"FIX3009SERIAL00000000000000005"`)
- 3116 fixture 额外含 `check_detail_info` List<Map> with ≥1 detail (10 必填 detail 字段)
- assertion 模式同 Step 2 — `@Autowired XsdComplianceHelper helper;` 注入后调用 `assertThatCode(() -> helper.validateMapperOutput("<msgNo>", body)).doesNotThrowAnyException()` (v0.4: helper 已改 instance method，禁 static call)

implementer subagent 起手前先 Read Step 2 模板 + 对应 mapper 必填字段表 (Plan §A1-§A5 fixture)，再 mirror。

- [ ] **Step 5: 跑 5 个 XsdComplianceTest 确认 GREEN**

```bash
cd /Users/muzhou/FEP_v1.0
./mvnw test -pl fep-web \
  -Dtest='ContractInfo3101XsdComplianceTest,ArchiveInfo3102XsdComplianceTest,QyRegister3109XsdComplianceTest,BankCheckDay3116XsdComplianceTest,RzReturnInfo3009XsdComplianceTest' \
  -Dsurefire.failIfNoSpecifiedTests=false --no-transfer-progress
```

期望: `Tests run: 5, Failures: 0, Errors: 0`

⚠️ **任何 RED 必须实跑揪出真违规字段 + 回溯 XSD facet/spec 修复 mapper**（红线 `feedback_xsd_compliance_fix_real_validator_on_sut` — 不打地鼠，循环 RED→修→GREEN 直到完整通过）。

- [ ] **Step 6: 全 reactor verify (fep-web + 下游模块)**

```bash
cd /Users/muzhou/FEP_v1.0
./mvnw verify -pl fep-web -am --no-transfer-progress
```

期望: `BUILD SUCCESS` 0 failure（含 fep-collector + fep-converter + fep-processor + fep-web 全模块）

- [ ] **Step 7: 提交**

```bash
cd /Users/muzhou/FEP_v1.0
git add fep-web/src/test/java/com/puchain/fep/web/collector/mapper/
git commit -m "$(cat <<'EOF'
test(web): add 5 XsdComplianceTest for collector mapper (3101/3102/3109/3116/3009)

Real XsdValidator on SUT integration tests per red-line
feedback_xsd_compliance_fix_real_validator_on_sut. Each test invokes the mapper's
toMessageBody() with a complete fixture, JAXB-marshals the result, and validates
against the real XSD via XsdValidator (no @MockBean).

XsdComplianceHelper shared helper avoids 5x JAXB+validator wiring duplication
(Rule-of-Three pre-emptive extract).

Pattern reference: OutboundEnvelopeXsdComplianceTest (R-NEW-1, 2026-05-27 ship)
+ XsdTestSupport.pad30 (3300533, 2026-05-28).

Plan: docs/plans/2026-05-28-collector-mapper-mode3-boil-lake.md §A7 (v0.2 新增)
PRD: §3.2.2 报文 XSD 合规 + §4 报文接入 + §6.2 报文清单
FR-ID: FR-MSG-3101/3102/3009/3109/3116 (XSD compliance 守护)

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

## 自检清单（Plan 作者必跑，AI 评审前）

### 1. PRD 覆盖度
- [x] 5 个 FR-MSG-* + 1 个 FR-COMM-COLLECTOR-MAPPER-HELPER 全在 Task 中有覆盖
- [x] FR-MSG-3105/3107/3112 明示在 Plan B 范围（本 Plan 头部 §2.2 已说）

### 2. 安全边界
- [x] 无 SM2/SM3/SM4/密钥/脱敏/审计 关键词命中（grep 实测：mapper 仅做字段映射）
- [x] AbstractFieldMapper helper 不引入安全/加密相关接口
- [x] `LogSanitizer.sanitize()` 用于异常 message 字段名 wrap（CRLF 防御），不涉及任何敏感数据明文

### 3. 占位符扫描
- [x] grep "TBD/TODO/待补/后续/类似" — 0 命中（除 Javadoc 中 "后续业务深化" 是嵌套字段处置的预期表述，spec §3.2 明示 out-of-scope，合规）

### 4. 类型一致性
- [x] `AbstractFieldMapper` (A2 定义) 在 A3/A4/A5 mapper 中被引用 — signature 一致
- [x] `requireString(Map, String, String)` 在所有 mapper 中调用签名一致
- [x] `CheckDetailInfo` POJO setter 在 A4 mapper.mapDetail() 中全 17 setter 覆盖（grep 实测匹配）

### 5. 测试命令可执行
- [x] 每个 `mvn test -Dtest=...` 类名与文件路径匹配
- [x] `-Dsurefire.failIfNoSpecifiedTests=false` 用 Surefire 3.x 正确参数名（红线 `feedback_surefire3_failifno_specified_tests_param_rename`）

### 6. CLAUDE.md 更新
- [x] Task A6 Step 6 + Step 9 显式更新 CLAUDE.md "当前项目状态" + "下一步候选" 段

### 7. 验收标准完整性
- [x] 每 Task 验收标准来自 PRD §6.2/§841 + XSD facet（非从代码推测）
- [x] 断言值（如 "1"/"0"/"A1000143000999"/"100000.00"）从 PRD/XSD 手算验证

### 8. 共享工具类无遗漏
- [x] `AbstractFieldMapper` 6 helper + 3 常量（含 v0.2 新增 serialNoOrFallback）在共享工具类清单已登记
- [x] `IdGenerator.uuid32()` / `LogSanitizer.sanitize()` / `FepConstants.HNDEMP_NODE_CODE` / `FepErrorCode.COLLECT_ASSEMBLE_FAILURE` 全在清单
- [x] 不重复造 helper（A3/A4/A5 都 extends AbstractFieldMapper，不内嵌 helper 拷贝）

### 9. 核心类职责边界
- [x] `AbstractFieldMapper` 职责边界声明（§核心类职责边界 段）：负责 6 helper（v0.2 加 serialNoOrFallback）、不负责 XSD facet / 嵌套 complex / 业务规则、依赖上限 6、行数上限 250

### 10. Worktree 触发条件自检（红线 `feedback_worktree_for_parallel_work`）

逐条核对（含红线 #7 多会话）:
- [x] 跨 ≥3 Maven 模块? **否**（v0.2: fep-collector 主体 + fep-web 5 个 XsdComplianceTest = 2 模块，仍 < 3 阈值）
- [x] 与已签字未执行 Plan 并存? **否**（PR #27 callback + PR #29 simplify 已 ship 等 GHA billing 非"未执行 Plan"）
- [x] ⛔ 安全 vs AI 并行? **否**
- [x] TLQ tongtech profile 联调? **否**
- [x] ≥5min long-running verify 并行? **否**（fep-collector 单模块 verify ~2-3 min）
- [x] muzhou WIP 与 AI 并存? **否**
- [x] 多会话活跃? **否**（单会话）

⟹ 全 ❌ → 命中 CLAUDE.md "例外"条款，普通 feature branch + stash 即可。Plan 头部已声明 `执行 Worktree: main（无需独立 worktree）`。

---

## 执行交接

**⚠️ 重要**: 本 Plan 完成起草。**禁止直接执行**，必须先经 **Plan 评审 + muzhou 签字** 流程。

### 步骤 1: AI 独立评审

派发独立 AI 评审 agent（santa-method 或 code-reviewer subagent）:
- 输入: 本 Plan 全文 + spec doc + PRD 矩阵 + `docs/guides/plan-review-checklist.md` 7 项清单
- 输出: ✅ 通过项 / ❌ 问题项（引用具体 Task 编号 + 违反清单编号）

若评审发现 ≥1 个问题 → 回作者修订 → 再次评审 → 直到通过。

### 步骤 2: muzhou 签字

AI 评审通过后提交 muzhou:
1. 阅读 Plan + AI 评审报告
2. 对照 PRD/spec 抽样核对
3. 决策：批准 / 驳回 / 部分修改
4. 在 Plan 文件末尾追加批准签字

### 步骤 3: 执行方式选择

签字后两种执行选项:

**1. Subagent 驱动（推荐）** — `superpowers:subagent-driven-development`，每 Task 派独立 subagent，主对话审 quality

**2. 内联执行** — `superpowers:executing-plans`，当前会话逐 Step 执行，关键节点暂停审核

---

## 批准签字

**Plan Author:** Claude Opus 4.7 (mode A, /Users/muzhou/FEP brainstorming + writing-plans skill, 2026-05-28)

**AI 独立评审 v0.1:** ✅ 完成 2026-05-28 (feature-dev:code-reviewer agentId `aadb053aedf628fc5`) — `DONE_WITH_CONCERNS`，揪出 2 MAJOR + 4 MINOR + baseline drift 揭示 MAJOR #3

**Plan v0.1 → v0.2 修订:** 2026-05-28
- ✅ MAJOR #1: A2 Step 7 新增同步修改 DefaultPayloadAssemblerTest（stub bean props 注入 + delete deferredMapper3109 测试）
- ✅ MAJOR #2: 新增 Task A7 = 5 个 XsdComplianceTest in fep-web（muzhou AskUserQuestion 决策）
- ✅ MAJOR #3 (baseline drift 揭示): AbstractFieldMapper 新增 `serialNoOrFallback()` 30 字符兜底 + 5 mapper 全改 + 全 fixture 30 字符
- ✅ MINOR #1: `optString` 改 `protected static`
- ✅ MINOR #2: A3 验收标准计数对齐（5 cases）
- ✅ MINOR #3: A6 Step 1 `.body()` → `.messageBody()`
- ✅ Baseline `4fcac99` → `3300533` 同步

**AI 独立评审 v0.2:** ✅ 完成 2026-05-28 (feature-dev:code-reviewer agentId `a5c78cf7aab877176`) — `DONE_WITH_CONCERNS`：v0.1 修复 7 项全 PASS；揪出 1 NEW MAJOR (XsdValidator 错包路径 `fep-converter` → 真实 `fep-processor.validation`) + 2 NEW MINOR (helper 计数 5→6 三处未同步 / XsdComplianceHelper static/instance 矛盾)

**Plan v0.2 → v0.3 修订:** 2026-05-28
- ✅ NEW MAJOR: XsdComplianceHelper 改 instance method @Autowired + import 改 `com.puchain.fep.processor.validation.XsdValidator` + `com.puchain.fep.converter.type.MessageType` + `validate(MessageType, byte[]) → ValidationResult` 真实 API
- ✅ NEW MINOR 1: "5 helper" → "6 helper" 3 处同步（文件结构表/自检/PRD 矩阵 Step 4 注释）
- ✅ NEW MINOR 2: XsdComplianceHelper 从 `@Autowired field + static method` 改为 `constructor-injection + instance method`
- ✅ 共享工具类清单加 `MessageType` + `ValidationResult` 跨模块依赖

**AI 独立评审 v0.3:** ✅ 完成 2026-05-28 (feature-dev:code-reviewer agentId `a1d569a0f01f7bdbe`) — `DONE_WITH_CONCERNS`：v0.3 NEW MAJOR (XsdValidator API) 全 PASS；揪出 v0.3 FAIL (2 处 "5 helper" 残留) + 1 NEW MINOR (Step 2 + Step 4 assertion 模板 static call 与 v0.3 instance helper 设计矛盾)

**Plan v0.3 → v0.4 修订:** 2026-05-28
- ✅ v0.3 FAIL #1: line 623 AbstractFieldMapperTest Javadoc "5 helper" → "6 helper" + 补 serialNoOrFallback
- ✅ v0.3 FAIL #2: line 2755 自检 §9 "5 helper" → "6 helper" + 依赖上限 5→6 + 行数上限 200→250
- ✅ v0.3 MINOR-NEW-1: line 2569 Step 2 + line 2662 Step 4 assertion 模板 — 加 `@Autowired XsdComplianceHelper helper` field + 调 instance method `helper.validateMapperOutput(...)`，禁 static call

**AI 独立评审 v0.4:** ✅ 完成 2026-05-28 (feature-dev:code-reviewer agentId `a3b82c3be16a78d6a`) — **`DONE`**：4 处 v0.3 残留全清零（V1-V4 PASS）+ V5 全文 grep 0 static call / 0 实质 "5 helper" 使用 — 综合可交 muzhou 签字

**评审趋势 (问题数量递减)**:
- v0.1 → 7 problems (2 MAJOR + 4 MINOR + baseline drift MAJOR)
- v0.2 → 3 problems (1 MAJOR + 2 MINOR)
- v0.3 → 3 problems (1 FAIL 含 2 处 + 1 NEW MINOR)
- v0.4 → **0 problems ✅ DONE**

## 批准签字

**muzhou ✅ APPROVED v0.4（2026-05-28）**

- **决策**: 批准 + Subagent 驱动执行（AskUserQuestion 选择 Recommended）
- **AI 评审 4 轮全 ✅**: v0.1 (7 problems) → v0.2 (3) → v0.3 (3) → v0.4 (**0** DONE)
- **PR 大小豁免**: 估算 ~2700 行（mapper 骨架重复 80%+），muzhou 已 AskUserQuestion 签字豁免 400 行硬上限
- **3116 CheckDetailInfo nested list 含实装**: muzhou AskUserQuestion 决策 (Option a Recommended)
- **XsdComplianceTest 5 个含 Task A7 跨 fep-web**: muzhou AskUserQuestion 决策 (非 defer)
- **SerialNo XSD length=30 修复含 AbstractFieldMapper.serialNoOrFallback()**: muzhou AskUserQuestion 决策 (Plan A 内修，Recommended)
- **执行方式**: `superpowers:subagent-driven-development`，每 Task 独立 implementer + spec/quality reviewer
- **Worktree**: T0 实施前 4 步动态复检 (红线 `feedback_worktree_trigger_is_dynamic_recheck_at_execution`)；当前 7 触发项全 ❌，main 分支 + `refactor/collector-mapper-mode3-boil-lake` feature branch
- **Baseline**: `origin/main = 3300533` (签字时实测，<1h 与 v0.4 reviewer 完成同窗)

---

> **生成方式**: superpowers:brainstorming → spec → writing-plans (FEP 定制版) → 本 Plan
> **Baseline HEAD**: `4fcac99` (2026-05-28 grep 实测)
> **下游 Plan**: Plan B (Mode2 3 mapper) — Plan A merge 后起草；Plan A.3 (XsdComplianceTest 全 8 mapper) — muzhou 优先级决策
