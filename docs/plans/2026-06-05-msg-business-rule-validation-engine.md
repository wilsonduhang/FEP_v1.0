# FEP 报文业务校验规则引擎 实施计划（§5.8 Phase 1）

> **执行方式:** 使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐 Task 执行。步骤使用 `- [ ]` 复选框跟踪。
> **本 harness 注意:** 涉长跑 mvn 的 Task 默认 hybrid（主对话实施 edits + 前台 mvn + commit / subagent 仅评审），见红线 `feedback_harness_bg_detach_hybrid_default`。

**目标:** 在现有 XSD 结构校验之上，新增一层**可配置的业务语义/跨字段校验引擎**（XSD 表达不了的规则：条件必填、跨字段比较、枚举/码表业务合法性），集成进同步处理流水线的校验 gate。

**前置依赖:** P2a 业务处理层（XsdValidator / ValidationResult / SyncMessageProcessorService 状态机流水线）已完成（实测 `fep-processor/validation/` + `pipeline/SyncMessageProcessorService.java` 存在）。

**执行 Worktree:** `/Users/muzhou/FEP_v1.0_wt-msg-rule-engine`（分支 `feat/msg-business-rule-validation-engine`，触发条件第 ② 项「与已签字未执行的 Plan 并存」+ 第 ⑥ 项「muzhou/别会话 WIP 并存」）
> 当前多会话高速并发（S2 / callback-p2c / p4-msg-n 等 worktree in-flight），本 Plan 签字后须独立 worktree 隔离，禁止在主 worktree 硬撑。闭环 Task 含 `git worktree remove` 实测命令。

**架构:** 引入 `BusinessRuleValidator`（fep-processor）作为 XSD 之后的第二道校验关。规则以 `ValidationRule` SPI 抽象，按 `MessageType` 注册到 `MessageRuleRegistry`。规则**内容**走配置驱动（mode C，由 ② 领域专家按人行规范填充权威规则母本），本 Plan 只交付**引擎框架 + 可复用规则类型 + 流水线集成 + 框架验证用示例规则**。`SyncMessageProcessorService` 在 XSD 通过后串联业务规则校验，失败转 `FAILED(PROC_8507)`。

**技术栈:** Java 17 / Spring Boot 3.x / Maven / JUnit 5 + AssertJ / 标准 JAXP DOM（无新增第三方依赖）

**AI 协同模式:**

| 模式 | AI 占比 | 本计划中的应用 |
|:----:|:-------:|--------------|
| A | 90% | 配置/测试/文档/CLAUDE.md 更新 |
| B | 70% | 引擎框架/SPI/流水线集成 |
| C | 60% | **规则类型 + 示例规则**（规则内容由领域专家定，AI 编码）|
| E | 0%  | ⛔ 本 Plan 不涉及国密安全区域 |

> **范围声明（防 6264 臆造复发，红线 `feedback_prd_audit`）:** 本 Plan **不实装任何具体的"N 条规则母本"**。"6264 条规则"是无源臆造数字（PRD/甲方材料/报文规范/codebase 全 0 命中，仅 FEP 自有文档繁殖 54 处），本 Plan 不引用该提法。权威规则母本来自人行规范 + 介绍文档逐字段规则，由 ② 领域专家以 mode C 配置填充，**不属本 Plan 范围**。本 Plan 交付的是"能装规则的引擎"，不是"规则本身"。

---

## 设计背景

### PRD 依据（实测 PRD v1.3 §5.8 line 1570-1600）

> §5.8 数据校验与审核 — 1. **规则引擎**：内置 XSD Schema 校验 + 自定义业务规则引擎；2. 多级审核；3. 异常处理流程可视化。
> 数据来源：《监管报送前置系统产品设计文档》§3.1「规则引擎，支持自定义校验规则，对数据进行审核」+ §5.4。PRD 原文标「**原型中无专门页面，需补充原型设计**」（故 Phase 3 异常可视化 UI 前置依赖 = 原型补充）。

§5.8 三能力的本 Plan 切分：

| 子能力 | 本 Plan | 说明 |
|---|:--:|---|
| 规则引擎 — XSD 结构校验 | ✅ 已存在 | `XsdValidator` (P2a) |
| 规则引擎 — **自定义业务规则引擎** | ⭐ **本 Plan Phase 1** | XSD 表达不了的语义/跨字段层 |
| 多级审核（自动→人工）| ❌ 不在本 Plan | Phase 2 独立 Plan（需审核工作流定义）|
| 异常处理流程可视化 | ❌ 不在本 Plan | Phase 3 独立 Plan（需原型）|

### 现有 API（实测，禁靠记忆 — 红线 `feedback_plan_must_grep_actual_api`）

```
fep-processor/src/main/java/com/puchain/fep/processor/validation/
├── ValidationResult.java   // record(boolean valid, List<String> errors); ok() / failed(List)
├── XsdValidator.java        // public ValidationResult validate(MessageType type, byte[] xml)
├── XsdSchemaRegistry.java
└── ValidationException.java

pipeline/SyncMessageProcessorService.java
  构造器: (XsdValidator validator, MessageStateMachine stateMachine, MessageProcessStore store)
  集成点 line 124: ValidationResult vr = validator.validate(type, xml);
                   if (!vr.valid()) return stateMachine.failWith(saved, FepErrorCode.PROC_8501, firstError);
                   → transition VALIDATED → PROCESSING → COMPLETED

fep-common/.../domain/FepErrorCode.java
  PROC_8501..PROC_8506 已占；下一个可用 = PROC_8507（实施时 grep 复核）

com.puchain.fep.converter.type.MessageType  // .msgNo() / byMsgNo(...)
com.puchain.fep.common.util.{LogSanitizer, IdGenerator}  // 已存在，复用
```

### 引擎数据流

```
process(type, xml)
  → XsdValidator.validate ──invalid──► FAILED(PROC_8501)   [现有，不改语义]
        │ valid
        ▼
  BusinessRuleValidator.validate(type, xml)                [新增第二关]
        │ 构建 RuleContext(字段 local-name → values, DOM 解析一次)
        │ MessageRuleRegistry.rulesFor(type) 逐条 evaluate
        ├──有违规──► FAILED(PROC_8507)
        │ 全通过
        ▼
  transition VALIDATED → PROCESSING → COMPLETED            [现有]
```

---

## 文件结构

| 文件路径 | 职责 | 操作 | AI 模式 |
|----------|------|------|:-------:|
| `fep-processor/.../validation/rule/RuleContext.java` | 报文字段解析视图（DOM local-name → values）| 新建 | B |
| `fep-processor/.../validation/rule/ValidationRule.java` | 规则 SPI（evaluate → Optional<String>）| 新建 | B |
| `fep-processor/.../validation/rule/ConditionalRequiredRule.java` | 可复用规则：条件必填 | 新建 | C |
| `fep-processor/.../validation/rule/CrossFieldComparisonRule.java` | 可复用规则：跨字段比较 | 新建 | C |
| `fep-processor/.../validation/rule/EnumMembershipRule.java` | 可复用规则：枚举/码表成员 | 新建 | C |
| `fep-processor/.../validation/rule/MessageRuleRegistry.java` | MessageType → List<ValidationRule> 注册表 | 新建 | B |
| `fep-processor/.../validation/BusinessRuleValidator.java` | 业务规则编排器 → ValidationResult | 新建 | B |
| `fep-processor/.../validation/rule/RuleDefinitionProperties.java` | 配置驱动规则定义（@ConfigurationProperties）| 新建 | C |
| `fep-processor/.../validation/rule/ConfiguredRuleFactory.java` | 配置 → ValidationRule 实例 | 新建 | C |
| `fep-processor/.../pipeline/SyncMessageProcessorService.java` | 串联 businessRuleValidator | 修改 | B |
| `fep-processor/.../ProcessorAutoConfiguration.java` | 装配新 Bean（若非 @ComponentScan）| 修改 | A |
| `fep-common/.../domain/FepErrorCode.java` | 新增 PROC_8507 | 修改 | A |
| 对应 `*Test.java` ×7 | 各 Task TDD 测试 | 新建 | A |

### 共享工具类清单

| 工具类 | 包 | 关键方法 | 提供者 Task | 消费者 Task |
|---|---|---|---|---|
| RuleContext | validation.rule | values(localName) / first(localName) | Task 1 | Task 2/3/4/5 |
| ValidationRule | validation.rule | evaluate(RuleContext) | Task 2 | Task 3/4/5 |
| LogSanitizer（已存在）| common.util | sanitize(String) | — | Task 4 |

> **规则**: subagent prompt 强制"写新 private 方法前先 grep 现有代码有无同名/同功能方法"。

### 核心类职责边界声明

#### BusinessRuleValidator 职责边界

**负责**: 对单条报文，加载该 MessageType 注册的业务规则并逐条求值，聚合违规为 `ValidationResult`。
**不负责**: XSD 结构校验（→ XsdValidator）/ 规则内容定义（→ 配置 + 领域专家）/ 状态机流转（→ MessageStateMachine）/ 报文解析持久化（→ pipeline）。
**依赖上限**: 7 个（设计自律目标，**无 ArchUnit 强制**——fep-processor `ProcessorArchitectureTest` 实测仅含包依赖方向/security.impl 隔离/CfxBody 约束 6 条规则，无依赖计数/行数规则；靠人工评审兜底）。当前依赖：MessageRuleRegistry（1 个）。
**行数上限**: 300 行（设计目标；机器兜底为 checkstyle FileLength≤400，见 CLAUDE.md）。
**如果超出**: 规则求值并发/缓存等优化拆 `RuleEvaluationStrategy`；当前单线程顺序求值不拆。

---

## Task 1: RuleContext 报文字段解析视图 `模式 B`

**PRD 依据:** v1.3 §5.8 规则引擎（业务规则需读取报文字段值）
**追溯 ID:** FR-WEB-AUDIT (§5.8)（对照 `docs/plans/prd-traceability-matrix.md`）

**验收标准（从 PRD 推导）:**
1. 输入合法 XML 字节 → `RuleContext.first("SerialNo")` 返回该元素文本值
2. 同名元素多次出现（明细列表）→ `values("Amt")` 返回全部值（按文档顺序）
3. 不存在的字段 → `first("Missing")` 返回 `Optional.empty()`，`values("Missing")` 返回空 List
4. 命名空间无关：`<ns:SerialNo>` 与 `<SerialNo>` 均按 local-name `SerialNo` 命中（与 XSD envelope 命名空间策略一致）
5. 非法 XML（解析失败）→ 抛 `ValidationException`（不静默吞）
6. **跨层同名字段语义（明示）**：CFX envelope 多层嵌套（HEAD/MSG/Head/body）中若同一 local-name 出现在不同层（如 HEAD 与 body 均有某字段），`values(name)` 按文档顺序合并全部层的值——Phase 1 业务规则均针对浅层 body 字段，此扁平化可接受；如需层级定位（XPath），留 Phase 2。须有 1 条测试覆盖此跨层合并行为（验证语义而非视为 bug）。

**Files:**
- Create: `fep-processor/src/main/java/com/puchain/fep/processor/validation/rule/RuleContext.java`
- Create: `fep-processor/src/test/java/com/puchain/fep/processor/validation/rule/RuleContextTest.java`

- [ ] **Step 1: 编写失败测试**

```java
// fep-processor/src/test/java/com/puchain/fep/processor/validation/rule/RuleContextTest.java
package com.puchain.fep.processor.validation.rule;

import com.puchain.fep.processor.validation.ValidationException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuleContextTest {

    private static RuleContext of(String xml) {
        return RuleContext.parse(xml.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void first_shouldReturnElementText() {
        RuleContext ctx = of("<CFX><SerialNo>SN001</SerialNo></CFX>");
        assertThat(ctx.first("SerialNo")).contains("SN001");
    }

    @Test
    void values_shouldReturnAllRepeatedElementsInDocumentOrder() {
        RuleContext ctx = of("<CFX><D><Amt>10.00</Amt></D><D><Amt>20.00</Amt></D></CFX>");
        assertThat(ctx.values("Amt")).containsExactly("10.00", "20.00");
    }

    @Test
    void first_shouldBeEmptyWhenFieldAbsent() {
        RuleContext ctx = of("<CFX><SerialNo>SN001</SerialNo></CFX>");
        assertThat(ctx.first("Missing")).isEmpty();
        assertThat(ctx.values("Missing")).isEmpty();
    }

    @Test
    void parse_shouldBeNamespaceAgnostic() {
        RuleContext ctx = of("<n:CFX xmlns:n=\"urn:x\"><n:SerialNo>SN9</n:SerialNo></n:CFX>");
        assertThat(ctx.first("SerialNo")).contains("SN9");
    }

    @Test
    void parse_shouldThrowOnMalformedXml() {
        assertThatThrownBy(() -> of("<CFX><unclosed></CFX>"))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void values_shouldMergeSameLocalNameAcrossNestingLevels() {
        // 跨层同名字段：HEAD 与 body 均有 WorkDate → 按文档顺序合并（语义明示，非 bug）
        RuleContext ctx = of("<CFX><HEAD><WorkDate>20260601</WorkDate></HEAD>"
                + "<MSG><body><WorkDate>20260605</WorkDate></body></MSG></CFX>");
        assertThat(ctx.values("WorkDate")).containsExactly("20260601", "20260605");
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd /Users/muzhou/FEP_v1.0_wt-msg-rule-engine
mvn -q -pl fep-processor test -Dtest=RuleContextTest
```
期望: 编译失败 — `cannot find symbol: class RuleContext`

- [ ] **Step 3: 编写最小实现**

```java
// fep-processor/src/main/java/com/puchain/fep/processor/validation/rule/RuleContext.java
package com.puchain.fep.processor.validation.rule;

import com.puchain.fep.processor.validation.ValidationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 报文字段的只读解析视图，供业务校验规则按 local-name 读取字段值。
 *
 * <p>命名空间无关：所有元素按本地名（local-name）索引，与 CFX envelope 多命名空间策略一致。
 * 同名元素（明细列表）按文档顺序保留全部文本值。</p>
 *
 * <p>解析时启用 XXE 防护（禁用 DOCTYPE / 外部实体），与 {@code XsdSchemaRegistry} 守护一致。</p>
 */
public final class RuleContext {

    private final Map<String, List<String>> fields;

    private RuleContext(final Map<String, List<String>> fields) {
        this.fields = fields;
    }

    /**
     * 解析 UTF-8 XML 字节为只读字段视图。
     *
     * @param xml UTF-8 编码报文字节，非空
     * @return 字段视图
     * @throws ValidationException XML 非良构或解析失败
     */
    public static RuleContext parse(final byte[] xml) {
        try {
            final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbf.setExpandEntityReferences(false);
            final DocumentBuilder builder = dbf.newDocumentBuilder();
            final Document doc = builder.parse(new ByteArrayInputStream(xml));
            final Map<String, List<String>> collected = new HashMap<>();
            collect(doc.getDocumentElement(), collected);
            return new RuleContext(collected);
        } catch (final ValidationException ex) {
            throw ex;
        } catch (final Exception ex) {
            throw new ValidationException("rule context parse failed: " + ex.getMessage(), ex);
        }
    }

    private static void collect(final Node node, final Map<String, List<String>> out) {
        if (node == null || node.getNodeType() != Node.ELEMENT_NODE) {
            return;
        }
        final Element el = (Element) node;
        final String localName = el.getLocalName() != null ? el.getLocalName() : el.getTagName();
        final String text = directTextOf(el);
        if (text != null && !text.isBlank()) {
            out.computeIfAbsent(localName, k -> new ArrayList<>()).add(text.trim());
        }
        final NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            collect(children.item(i), out);
        }
    }

    private static String directTextOf(final Element el) {
        final NodeList children = el.getChildNodes();
        final StringBuilder sb = new StringBuilder();
        boolean hasText = false;
        for (int i = 0; i < children.getLength(); i++) {
            final Node child = children.item(i);
            if (child.getNodeType() == Node.TEXT_NODE || child.getNodeType() == Node.CDATA_SECTION_NODE) {
                sb.append(child.getNodeValue());
                hasText = true;
            }
        }
        return hasText ? sb.toString() : null;
    }

    /**
     * 返回指定 local-name 字段的全部文本值（文档顺序）。
     *
     * @param localName 元素本地名
     * @return 不可修改的值列表；字段不存在时为空 List
     */
    public List<String> values(final String localName) {
        return List.copyOf(fields.getOrDefault(localName, List.of()));
    }

    /**
     * 返回指定 local-name 字段的首个文本值。
     *
     * @param localName 元素本地名
     * @return 首个值；字段不存在时 {@link Optional#empty()}
     */
    public Optional<String> first(final String localName) {
        final List<String> vs = fields.get(localName);
        return (vs == null || vs.isEmpty()) ? Optional.empty() : Optional.of(vs.get(0));
    }

    /**
     * 字段是否存在且有非空白值。
     *
     * @param localName 元素本地名
     * @return 存在且非空白返回 true
     */
    public boolean has(final String localName) {
        return first(localName).isPresent();
    }
}
```

> **实施提示:** grep 确认 `ValidationException` 是否有 `(String, Throwable)` 构造器；若无，按其现有构造器签名调整（红线 `feedback_plan_must_grep_actual_api`）。

- [ ] **Step 4: 运行测试确认通过**

```bash
cd /Users/muzhou/FEP_v1.0_wt-msg-rule-engine
mvn -q -pl fep-processor test -Dtest=RuleContextTest
```
期望: `BUILD SUCCESS`, 6 tests passed

- [ ] **Step 5: spotbugs + ArchUnit 自检（红线 `feedback_subagent_must_run_spotbugs_check`）**

```bash
mvn -q -pl fep-processor spotbugs:check
mvn -q -pl fep-processor test -Dtest=ProcessorArchitectureTest
```
期望: BugInstance size 0；ArchTest PASS

- [ ] **Step 6: 提交**

```bash
git add fep-processor/src/
git commit -m "$(cat <<'EOF'
feat(processor): add RuleContext namespace-agnostic field view for business rule engine

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

## Task 2: ValidationRule SPI + 三类可复用规则 `模式 C`

**PRD 依据:** v1.3 §5.8 自定义业务规则引擎 + 监管报送产品设计文档 §3.1「支持自定义校验规则」
**追溯 ID:** FR-WEB-AUDIT (§5.8)

**验收标准:**
1. `ValidationRule.evaluate(ctx)` 通过 → `Optional.empty()`；违规 → `Optional.of(错误描述)`
2. `ConditionalRequiredRule("ResultMsg", "ResultCode", v -> !"0000".equals(v))`：当 ResultCode≠0000 且 ResultMsg 缺失 → 违规；ResultCode=0000 时不要求 ResultMsg
3. `CrossFieldComparisonRule("BeginDate","EndDate", LE)`：BeginDate>EndDate → 违规（字符串按 yyyyMMdd 可比）
4. `EnumMembershipRule("Currency", Set.of("CNY","USD"))`：值不在集合 → 违规；字段缺失 → 不违规（必填交给 ConditionalRequired/XSD）
5. 每条规则的错误描述含字段名，便于定位

> **mode C 说明:** 规则**类型**（条件必填/跨字段比较/枚举成员）是通用机制由 AI 编码；规则**实例参数**（哪些字段、哪些枚举值）由 ② 领域专家按人行规范定（本 Task 测试中的实例为框架验证用示例，非权威母本）。

**Files:**
- Create: `fep-processor/.../validation/rule/ValidationRule.java`
- Create: `fep-processor/.../validation/rule/ConditionalRequiredRule.java`
- Create: `fep-processor/.../validation/rule/CrossFieldComparisonRule.java`
- Create: `fep-processor/.../validation/rule/EnumMembershipRule.java`
- Create: `fep-processor/.../validation/rule/RuleTypesTest.java`

- [ ] **Step 1: 编写失败测试**

```java
// fep-processor/src/test/java/com/puchain/fep/processor/validation/rule/RuleTypesTest.java
package com.puchain.fep.processor.validation.rule;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RuleTypesTest {

    private static RuleContext ctx(String xml) {
        return RuleContext.parse(xml.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void conditionalRequired_violatedWhenTriggerMatchesAndTargetMissing() {
        ValidationRule rule = new ConditionalRequiredRule(
                "ResultMsg", "ResultCode", v -> !"0000".equals(v));
        assertThat(rule.evaluate(ctx("<CFX><ResultCode>9999</ResultCode></CFX>")))
                .isPresent();
        assertThat(rule.evaluate(ctx("<CFX><ResultCode>0000</ResultCode></CFX>")))
                .isEmpty();
        assertThat(rule.evaluate(ctx("<CFX><ResultCode>9999</ResultCode><ResultMsg>err</ResultMsg></CFX>")))
                .isEmpty();
    }

    @Test
    void crossFieldComparison_violatedWhenBeginAfterEnd() {
        ValidationRule rule = new CrossFieldComparisonRule(
                "BeginDate", "EndDate", CrossFieldComparisonRule.Operator.LE);
        assertThat(rule.evaluate(ctx("<CFX><BeginDate>20260605</BeginDate><EndDate>20260601</EndDate></CFX>")))
                .isPresent();
        assertThat(rule.evaluate(ctx("<CFX><BeginDate>20260601</BeginDate><EndDate>20260605</EndDate></CFX>")))
                .isEmpty();
    }

    @Test
    void enumMembership_violatedWhenValueNotInSet() {
        ValidationRule rule = new EnumMembershipRule("Currency", Set.of("CNY", "USD"));
        assertThat(rule.evaluate(ctx("<CFX><Currency>JPY</Currency></CFX>"))).isPresent();
        assertThat(rule.evaluate(ctx("<CFX><Currency>CNY</Currency></CFX>"))).isEmpty();
        assertThat(rule.evaluate(ctx("<CFX><Other>x</Other></CFX>"))).isEmpty(); // 缺失不违规
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd /Users/muzhou/FEP_v1.0_wt-msg-rule-engine
mvn -q -pl fep-processor test -Dtest=RuleTypesTest
```
期望: 编译失败 — `cannot find symbol: interface ValidationRule`

- [ ] **Step 3: 编写最小实现**

```java
// fep-processor/.../validation/rule/ValidationRule.java
package com.puchain.fep.processor.validation.rule;

import java.util.Optional;

/**
 * 业务校验规则 SPI。对单条报文的字段视图求值，违规返回错误描述。
 *
 * <p>规则机制由 AI 编码，规则实例参数由领域专家按人行规范定义（mode C）。</p>
 */
@FunctionalInterface
public interface ValidationRule {

    /**
     * 对报文字段视图求值。
     *
     * @param ctx 报文字段视图，非空
     * @return 违规时返回错误描述；通过时 {@link Optional#empty()}
     */
    Optional<String> evaluate(RuleContext ctx);
}
```

```java
// fep-processor/.../validation/rule/ConditionalRequiredRule.java
package com.puchain.fep.processor.validation.rule;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * 条件必填规则：当触发字段满足条件时，目标字段必须有值。
 *
 * <p>典型用途：回执报文 ResultCode≠成功码 时 ResultMsg 必填——XSD 无法表达此条件依赖。</p>
 */
public final class ConditionalRequiredRule implements ValidationRule {

    private final String targetField;
    private final String triggerField;
    private final Predicate<String> triggerCondition;

    /**
     * @param targetField      条件成立时必填的字段 local-name
     * @param triggerField     触发字段 local-name
     * @param triggerCondition 对触发字段值的条件；为 true 时要求 targetField 必填
     */
    public ConditionalRequiredRule(final String targetField,
                                   final String triggerField,
                                   final Predicate<String> triggerCondition) {
        this.targetField = targetField;
        this.triggerField = triggerField;
        this.triggerCondition = triggerCondition;
    }

    @Override
    public Optional<String> evaluate(final RuleContext ctx) {
        final Optional<String> trigger = ctx.first(triggerField);
        if (trigger.isEmpty() || !triggerCondition.test(trigger.get())) {
            return Optional.empty();
        }
        if (ctx.has(targetField)) {
            return Optional.empty();
        }
        return Optional.of("字段 " + targetField + " 在 " + triggerField + " 触发条件下必填");
    }
}
```

```java
// fep-processor/.../validation/rule/CrossFieldComparisonRule.java
package com.puchain.fep.processor.validation.rule;

import java.util.Optional;

/**
 * 跨字段比较规则：两个字段值按字符串自然序比较，不满足关系即违规。
 *
 * <p>适用于定长可比字段（如 yyyyMMdd 日期、零填充定长数值）。两字段任一缺失时不违规
 * （存在性交由 XSD / ConditionalRequiredRule）。</p>
 */
public final class CrossFieldComparisonRule implements ValidationRule {

    /** 比较算子：左字段 OP 右字段 必须成立。 */
    public enum Operator { LE, LT, GE, GT, EQ }

    private final String leftField;
    private final String rightField;
    private final Operator operator;

    /**
     * @param leftField  左字段 local-name
     * @param rightField 右字段 local-name
     * @param operator   要求成立的比较关系
     */
    public CrossFieldComparisonRule(final String leftField,
                                    final String rightField,
                                    final Operator operator) {
        this.leftField = leftField;
        this.rightField = rightField;
        this.operator = operator;
    }

    @Override
    public Optional<String> evaluate(final RuleContext ctx) {
        final Optional<String> left = ctx.first(leftField);
        final Optional<String> right = ctx.first(rightField);
        if (left.isEmpty() || right.isEmpty()) {
            return Optional.empty();
        }
        final int cmp = left.get().compareTo(right.get());
        final boolean ok = switch (operator) {
            case LE -> cmp <= 0;
            case LT -> cmp < 0;
            case GE -> cmp >= 0;
            case GT -> cmp > 0;
            case EQ -> cmp == 0;
        };
        return ok ? Optional.empty()
                : Optional.of("字段 " + leftField + " 与 " + rightField
                        + " 不满足 " + operator + " 关系");
    }
}
```

```java
// fep-processor/.../validation/rule/EnumMembershipRule.java
package com.puchain.fep.processor.validation.rule;

import java.util.Optional;
import java.util.Set;

/**
 * 枚举/码表成员规则：字段值必须属于允许集合。字段缺失时不违规（必填另行规则）。
 *
 * <p>用于 XSD 未约束或需运行时码表（按银行差异化）的枚举校验。</p>
 */
public final class EnumMembershipRule implements ValidationRule {

    private final String field;
    private final Set<String> allowed;

    /**
     * @param field   字段 local-name
     * @param allowed 允许值集合（不可为空）
     */
    public EnumMembershipRule(final String field, final Set<String> allowed) {
        this.field = field;
        this.allowed = Set.copyOf(allowed);
    }

    @Override
    public Optional<String> evaluate(final RuleContext ctx) {
        final Optional<String> value = ctx.first(field);
        if (value.isEmpty() || allowed.contains(value.get())) {
            return Optional.empty();
        }
        return Optional.of("字段 " + field + " 值 [" + value.get() + "] 不在允许集合 " + allowed);
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

```bash
mvn -q -pl fep-processor test -Dtest=RuleTypesTest
```
期望: `BUILD SUCCESS`, 3 tests passed

- [ ] **Step 5: spotbugs + ArchUnit 自检**

```bash
mvn -q -pl fep-processor spotbugs:check
mvn -q -pl fep-processor test -Dtest=ProcessorArchitectureTest
```
期望: BugInstance size 0；ArchTest PASS

- [ ] **Step 6: 提交**

```bash
git add fep-processor/src/
git commit -m "$(cat <<'EOF'
feat(processor): add ValidationRule SPI and three reusable rule types

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

## Task 3: MessageRuleRegistry + BusinessRuleValidator 编排器 `模式 B`

**PRD 依据:** v1.3 §5.8 规则引擎（按报文类型组织规则 + 聚合校验结果）
**追溯 ID:** FR-WEB-AUDIT (§5.8)

**验收标准:**
1. `MessageRuleRegistry.register(type, rule)` 后 `rulesFor(type)` 含该规则；未注册类型 → 空 List
2. `BusinessRuleValidator.validate(type, xml)`：无注册规则 → `ValidationResult.ok()`（默认放行，非阻断）
3. 单规则违规 → `ValidationResult.failed`，errors 含该规则错误描述
4. 多规则多违规 → errors 含**全部**违规（不短路，便于一次性反馈，对齐 XsdValidator 收集式语义）
5. XML 解析失败 → 抛 `ValidationException`（由 pipeline 处理，与 XSD 关一致不静默）

**Files:**
- Create: `fep-processor/.../validation/rule/MessageRuleRegistry.java`
- Create: `fep-processor/.../validation/BusinessRuleValidator.java`
- Create: `fep-processor/.../validation/BusinessRuleValidatorTest.java`

- [ ] **Step 1: 编写失败测试**

```java
// fep-processor/src/test/java/com/puchain/fep/processor/validation/BusinessRuleValidatorTest.java
package com.puchain.fep.processor.validation;

import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.validation.rule.EnumMembershipRule;
import com.puchain.fep.processor.validation.rule.MessageRuleRegistry;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessRuleValidatorTest {

    private static byte[] xml(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    // 实施时用 MessageType 真实枚举常量替换 SAMPLE（grep MessageType 取一个同步报文常量）
    private final MessageType sampleType = MessageType.values()[0];

    @Test
    void validate_shouldReturnOkWhenNoRulesRegistered() {
        MessageRuleRegistry registry = new MessageRuleRegistry();
        BusinessRuleValidator v = new BusinessRuleValidator(registry);
        assertThat(v.validate(sampleType, xml("<CFX><Currency>JPY</Currency></CFX>")).valid())
                .isTrue();
    }

    @Test
    void validate_shouldCollectAllViolations() {
        MessageRuleRegistry registry = new MessageRuleRegistry();
        registry.register(sampleType, new EnumMembershipRule("Currency", Set.of("CNY")));
        registry.register(sampleType, new EnumMembershipRule("Status", Set.of("1")));
        BusinessRuleValidator v = new BusinessRuleValidator(registry);

        ValidationResult r = v.validate(sampleType,
                xml("<CFX><Currency>JPY</Currency><Status>9</Status></CFX>"));

        assertThat(r.valid()).isFalse();
        assertThat(r.errors()).hasSize(2);
    }

    @Test
    void validate_shouldPassWhenAllRulesSatisfied() {
        MessageRuleRegistry registry = new MessageRuleRegistry();
        registry.register(sampleType, new EnumMembershipRule("Currency", Set.of("CNY")));
        BusinessRuleValidator v = new BusinessRuleValidator(registry);
        assertThat(v.validate(sampleType, xml("<CFX><Currency>CNY</Currency></CFX>")).valid())
                .isTrue();
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
mvn -q -pl fep-processor test -Dtest=BusinessRuleValidatorTest
```
期望: 编译失败 — `cannot find symbol: class MessageRuleRegistry`

- [ ] **Step 3: 编写最小实现**

```java
// fep-processor/.../validation/rule/MessageRuleRegistry.java
package com.puchain.fep.processor.validation.rule;

import com.puchain.fep.converter.type.MessageType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 报文类型 → 业务校验规则列表注册表。线程安全前提：注册在应用启动期完成（@PostConstruct / 配置装配），
 * 运行期只读。
 */
@Component
public class MessageRuleRegistry {

    private final Map<MessageType, List<ValidationRule>> rules = new EnumMap<>(MessageType.class);

    /**
     * 为指定报文类型注册一条规则。
     *
     * @param type 报文类型，非空
     * @param rule 规则，非空
     */
    public void register(final MessageType type, final ValidationRule rule) {
        rules.computeIfAbsent(type, k -> new ArrayList<>()).add(rule);
    }

    /**
     * 返回指定报文类型的规则列表（不可修改视图）。
     *
     * @param type 报文类型
     * @return 规则列表；未注册时为空 List
     */
    public List<ValidationRule> rulesFor(final MessageType type) {
        return List.copyOf(rules.getOrDefault(type, List.of()));
    }
}
```

```java
// fep-processor/.../validation/BusinessRuleValidator.java
package com.puchain.fep.processor.validation;

import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.validation.rule.MessageRuleRegistry;
import com.puchain.fep.processor.validation.rule.RuleContext;
import com.puchain.fep.processor.validation.rule.ValidationRule;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 业务语义/跨字段校验器。在 XSD 结构校验通过后运行，按报文类型加载注册规则逐条求值，
 * 聚合全部违规为 {@link ValidationResult}（收集式，不短路）。
 *
 * <p>无注册规则时默认放行（{@link ValidationResult#ok()}），不阻断尚未配置规则的报文类型。</p>
 */
@Component
public class BusinessRuleValidator {

    private final MessageRuleRegistry registry;

    /**
     * @param registry 规则注册表，非空
     */
    public BusinessRuleValidator(final MessageRuleRegistry registry) {
        this.registry = registry;
    }

    /**
     * 对单条报文执行业务规则校验。
     *
     * @param type 报文类型，非空
     * @param xml  UTF-8 报文字节，非空
     * @return 校验结果；全部规则通过或无规则时 valid=true
     * @throws ValidationException XML 解析失败
     */
    public ValidationResult validate(final MessageType type, final byte[] xml) {
        final List<ValidationRule> applicable = registry.rulesFor(type);
        if (applicable.isEmpty()) {
            return ValidationResult.ok();
        }
        final RuleContext ctx = RuleContext.parse(xml);
        final List<String> violations = new ArrayList<>();
        for (final ValidationRule rule : applicable) {
            rule.evaluate(ctx).ifPresent(violations::add);
        }
        return violations.isEmpty() ? ValidationResult.ok() : ValidationResult.failed(violations);
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

```bash
mvn -q -pl fep-processor test -Dtest=BusinessRuleValidatorTest
```
期望: `BUILD SUCCESS`, 3 tests passed

> **实施提示:** 测试中 `MessageType.values()[0]` 仅为占位；实施时 grep `MessageType` 取一个真实同步报文常量（如 11 种同步报文之一）替换，使测试语义明确。

- [ ] **Step 5: spotbugs + ArchUnit 自检**

```bash
mvn -q -pl fep-processor spotbugs:check
mvn -q -pl fep-processor test -Dtest=ProcessorArchitectureTest
```
期望: BugInstance size 0；ProcessorArchitectureTest PASS（包依赖方向 + security.impl 隔离 + CfxBody 约束；注：依赖≤7/行≤300 无 ArchUnit 强制，人工评审核对）

- [ ] **Step 6: 提交**

```bash
git add fep-processor/src/
git commit -m "$(cat <<'EOF'
feat(processor): add MessageRuleRegistry and BusinessRuleValidator orchestrator

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

## Task 4: 流水线集成 + PROC_8507 `模式 B`

**PRD 依据:** v1.3 §5.8（业务校验须作为处理流水线的一道关）+ §4.7 状态机
**追溯 ID:** FR-WEB-AUDIT (§5.8) + FR-PROC-SYNC-PIPELINE (§4.7)

**验收标准:**
1. XSD 通过 + 业务规则通过 → 终态 COMPLETED（行为不变，回归现有同步流水线测试全绿）
2. XSD 通过 + 业务规则违规 → 终态 FAILED，错误码 `PROC_8507`，错误信息含首条违规
3. XSD 失败 → 仍 FAILED(PROC_8501)，**不触发**业务规则校验（顺序：XSD 先，业务后）
4. 无注册规则的报文类型 → 业务关放行，行为与现状一致（向后兼容）
5. `FepErrorCode.PROC_8507` 新增且 code 字面值唯一（grep 复核无冲突）

**Files:**
- Modify: `fep-common/.../domain/FepErrorCode.java`（+PROC_8507）
- Modify: `fep-processor/.../pipeline/SyncMessageProcessorService.java`
- Modify: `fep-processor/src/test/.../pipeline/SyncMessageProcessorServiceTest.java`（+业务规则关测试）

- [ ] **Step 1: 新增错误码（grep 复核下一个可用码）**

```bash
grep -nE 'PROC_85[0-9][0-9]' fep-common/src/main/java/com/puchain/fep/common/domain/FepErrorCode.java
# 实测 8501-8506 已占；确认 8507 未占后新增
```

```java
// fep-common/.../domain/FepErrorCode.java —— 在 PROC_8506 之后新增
    PROC_8507("PROC_8507", "报文业务规则校验失败"),
```

- [ ] **Step 2: 编写失败测试**

```java
// SyncMessageProcessorServiceTest.java —— 新增方法（沿用该测试类既有构造/fixture 风格，grep 确认现有 setUp）
    @Test
    void process_shouldFailWithProc8507_whenBusinessRuleViolated() {
        // 现有 fixture 提供 XSD 合法报文 + sampleType；为 sampleType 注册一条必失败规则
        ruleRegistry.register(sampleType, new EnumMembershipRule("Currency", java.util.Set.of("CNY")));
        byte[] xsdValidButRuleViolating = /* 现有合法 fixture 中 Currency=JPY 的变体 */ ;

        MessageProcessRecord record = service.processInbound(sampleType, "TXN-RULE-1", xsdValidButRuleViolating);

        assertThat(record.getStatus()).isEqualTo(MessageProcessStatus.FAILED);
        assertThat(record.getErrorCode()).isEqualTo("PROC_8507");   // getErrorCode() 返回 String，对齐既有测试 line 95 风格
    }

    @Test
    void process_shouldComplete_whenBusinessRulesPass() {
        // 无注册规则或规则全通过 → COMPLETED（向后兼容）
        MessageProcessRecord record = service.processInbound(sampleType, "TXN-RULE-2", validFixtureXml);
        assertThat(record.getStatus()).isEqualTo(MessageProcessStatus.COMPLETED);
    }
```

> **实施提示:** grep `SyncMessageProcessorServiceTest` 现有 setUp/fixture，复用其报文样本与 stateMachine/store mock 装配。`MessageProcessRecord` 实测为 class（非 record），访问器 `getStatus()`/`getErrorCode()`（后者返 String），已在上方测试代码采用；测试类还须把 `businessRuleValidator`（mock 或真实 + ruleRegistry）注入 service 构造器（实测构造器现为 3 参，Task 4 改为 4 参）。

- [ ] **Step 3: 运行测试确认失败**

```bash
mvn -q -pl fep-processor test -Dtest=SyncMessageProcessorServiceTest
```
期望: 编译失败（service 构造器尚无 businessRuleValidator 参数）或断言失败（PROC_8507 未触发）

- [ ] **Step 4: 修改实现 — 串联业务规则关**

```java
// SyncMessageProcessorService.java —— 字段 + 构造器新增 businessRuleValidator
    private final BusinessRuleValidator businessRuleValidator;

    public SyncMessageProcessorService(final XsdValidator validator,
                                       final BusinessRuleValidator businessRuleValidator,
                                       final MessageStateMachine stateMachine,
                                       final MessageProcessStore store) {
        this.validator = validator;
        this.businessRuleValidator = businessRuleValidator;
        this.stateMachine = stateMachine;
        this.store = store;
    }
```

```java
// process(...) 方法 —— 在 XSD 校验通过后、VALIDATED transition 之前插入业务规则关
        ValidationResult vr = validator.validate(type, xml);
        if (!vr.valid()) {
            String firstError = vr.errors().isEmpty() ? "unknown" : vr.errors().get(0);
            log.warn("[{}] xsd validation failed msg={} transitionNo={} firstError={}",
                    direction, type.msgNo(),
                    LogSanitizer.sanitize(transitionNo), LogSanitizer.sanitize(firstError));
            return stateMachine.failWith(saved, FepErrorCode.PROC_8501, firstError);
        }

        ValidationResult br = businessRuleValidator.validate(type, xml);   // ← 新增第二关
        if (!br.valid()) {
            String firstError = br.errors().isEmpty() ? "unknown" : br.errors().get(0);
            log.warn("[{}] business rule validation failed msg={} transitionNo={} firstError={}",
                    direction, type.msgNo(),
                    LogSanitizer.sanitize(transitionNo), LogSanitizer.sanitize(firstError));
            return stateMachine.failWith(saved, FepErrorCode.PROC_8507, firstError);
        }

        MessageProcessRecord validated = stateMachine.transition(saved, MessageProcessStatus.VALIDATED);
```

> **实施提示:** `BatchMessageProcessorService` / `AsyncMessageProcessorService` 同型集成留 **Phase 1 后续 Task 或 Phase 2**（本 Plan Phase 1 仅 Sync 关闭环，避免一次改三流水线超 PR 400 行）。在 Plan §回归验收中明示 batch/async 暂不接入。须同步更新这两个 service 的构造器调用点？—— **否**，本 Task 只改 Sync；batch/async 构造器不动。

- [ ] **Step 5: 修复 DI 装配（grep ProcessorAutoConfiguration / @ComponentScan 装配点）**

```bash
# 确认 SyncMessageProcessorService 是 @Service 自动扫描还是 @Bean 显式装配
grep -rn 'SyncMessageProcessorService' fep-processor/src/main/java
```
若为 @Service 自动扫描 + 构造注入：BusinessRuleValidator 已是 @Component，Spring 自动注入，无需改 config。若为 @Bean 显式装配：在 ProcessorAutoConfiguration 补 businessRuleValidator 参数。

- [ ] **Step 6: 运行测试 + 全 fep-processor 回归（红线 `feedback_full_regression_before_commit`）**

```bash
mvn -q -pl fep-processor test
```
期望: `BUILD SUCCESS`；新增 2 测试 + 现有同步流水线测试全绿（向后兼容）

- [ ] **Step 7: spotbugs + ArchUnit 自检**

```bash
mvn -q -pl fep-processor spotbugs:check
mvn -q -pl fep-processor test -Dtest=ProcessorArchitectureTest
```
期望: BugInstance size 0；ArchTest PASS

- [ ] **Step 8: 提交**

```bash
git add fep-common/src/ fep-processor/src/
git commit -m "$(cat <<'EOF'
feat(processor): wire BusinessRuleValidator into sync pipeline with PROC_8507

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

## Task 5: 配置驱动规则注册 + 示例规则集 + 端到端 IT + 文档 `模式 C` + `模式 A`

**PRD 依据:** v1.3 §5.8 自定义校验规则（按银行差异化可配置）+ §5.10.7.4 其他系统配置（规则）
**追溯 ID:** FR-WEB-AUDIT (§5.8)

**验收标准:**
1. `application.yml` 配置 `fep.validation.rules` → 启动期装配为注册规则（枚举/条件必填/跨字段比较三类型可声明）
2. 配置加载后，对应报文类型走业务规则关（端到端 IT：配置一条枚举规则 → 违规报文 FAILED(PROC_8507) / 合规报文 COMPLETED）
3. 无 `fep.validation.rules` 配置 → 引擎空注册、全报文业务关放行（向后兼容）
4. CLAUDE.md「当前项目状态」+ prd-traceability-matrix FR-WEB-AUDIT 状态更新（🟡→🟢 引擎部分实施）

> **mode C 边界:** 配置中的规则实例为**框架验证用示例**（明确标注），非权威规则母本。权威母本由 ② 领域专家按人行规范后续填充，不在本 Plan。

**Files:**
- Create: `fep-processor/.../validation/rule/RuleDefinitionProperties.java`（@ConfigurationProperties("fep.validation")）
- Create: `fep-processor/.../validation/rule/ConfiguredRuleFactory.java`（配置 → ValidationRule + @PostConstruct 注册）
- Create: `fep-processor/src/test/.../validation/rule/ConfiguredRuleFactoryTest.java`
- Create: `fep-processor/src/test/.../validation/BusinessRuleEngineIntegrationTest.java`（@SpringBootTest 端到端）
- Modify: `fep-processor/src/test/resources/application-test.yml`（示例规则配置）
- Modify: `/Users/muzhou/FEP/CLAUDE.md`（当前项目状态；file write only，禁 git commit — 红线 `feedback_fep_docs_repo_commit_taboo`）
- Modify: `/Users/muzhou/FEP/docs/plans/prd-traceability-matrix.md`（FR-WEB-AUDIT 状态 + 反向追溯；**非 FEP_v1.0 git 树，file write only**）

- [ ] **Step 1: 编写失败测试（ConfiguredRuleFactory 单测 + 端到端 IT）**

```java
// ConfiguredRuleFactoryTest.java
package com.puchain.fep.processor.validation.rule;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ConfiguredRuleFactoryTest {

    @Test
    void build_shouldCreateEnumRuleFromDefinition() {
        RuleDefinitionProperties.RuleDef def = new RuleDefinitionProperties.RuleDef();
        def.setType("ENUM");
        def.setField("Currency");
        def.setAllowed(java.util.List.of("CNY", "USD"));

        ValidationRule rule = ConfiguredRuleFactory.build(def);

        assertThat(rule).isInstanceOf(EnumMembershipRule.class);
    }
}
```

```java
// BusinessRuleEngineIntegrationTest.java —— @SpringBootTest 端到端（沿用现有 P5/IT 的 SpringBootTest fixture 风格，grep 复核）
// 配置 application-test.yml 一条示例 ENUM 规则，验证违规报文 FAILED(PROC_8507)、合规报文 COMPLETED
```

- [ ] **Step 2: 运行确认失败** → `mvn -q -pl fep-processor test -Dtest=ConfiguredRuleFactoryTest`

- [ ] **Step 3: 实现 RuleDefinitionProperties + ConfiguredRuleFactory**

```java
// RuleDefinitionProperties.java
package com.puchain.fep.processor.validation.rule;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 配置驱动的业务校验规则定义。键为报文 msgNo，值为该报文的规则列表。
 *
 * <p>规则内容由领域专家按人行规范在配置中维护（mode C），引擎启动期装配。</p>
 */
@ConfigurationProperties(prefix = "fep.validation")
public class RuleDefinitionProperties {

    /** msgNo → 规则定义列表。 */
    private Map<String, List<RuleDef>> rules = Map.of();

    public Map<String, List<RuleDef>> getRules() {
        return rules;
    }

    public void setRules(final Map<String, List<RuleDef>> rules) {
        this.rules = rules;
    }

    /** 单条规则定义。type ∈ {ENUM, CONDITIONAL_REQUIRED, CROSS_FIELD}。 */
    public static class RuleDef {
        private String type;
        private String field;
        private String triggerField;
        private String compareField;
        private String operator;
        private List<String> allowed = new ArrayList<>();
        // getters / setters（完整列出，无占位）
        public String getType() { return type; }
        public void setType(final String type) { this.type = type; }
        public String getField() { return field; }
        public void setField(final String field) { this.field = field; }
        public String getTriggerField() { return triggerField; }
        public void setTriggerField(final String triggerField) { this.triggerField = triggerField; }
        public String getCompareField() { return compareField; }
        public void setCompareField(final String compareField) { this.compareField = compareField; }
        public String getOperator() { return operator; }
        public void setOperator(final String operator) { this.operator = operator; }
        public List<String> getAllowed() { return allowed; }
        public void setAllowed(final List<String> allowed) { this.allowed = allowed; }
    }
}
```

```java
// ConfiguredRuleFactory.java
package com.puchain.fep.processor.validation.rule;

import com.puchain.fep.converter.type.MessageType;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 将配置规则定义装配为 {@link ValidationRule} 并注册到 {@link MessageRuleRegistry}（启动期一次）。
 */
@Component
public class ConfiguredRuleFactory {

    private final RuleDefinitionProperties properties;
    private final MessageRuleRegistry registry;

    public ConfiguredRuleFactory(final RuleDefinitionProperties properties,
                                 final MessageRuleRegistry registry) {
        this.properties = properties;
        this.registry = registry;
    }

    /**
     * 启动期把配置规则注册到注册表。
     */
    @PostConstruct
    public void registerConfiguredRules() {
        properties.getRules().forEach((msgNo, defs) -> {
            // 实测 MessageType.byMsgNo 返回 Optional<MessageType>（未知 msgNo → empty，不抛异常）
            final MessageType type = MessageType.byMsgNo(msgNo)
                    .orElseThrow(() -> new IllegalArgumentException("未知报文号配置 fep.validation.rules: " + msgNo));
            defs.forEach(def -> registry.register(type, build(def)));
        });
    }

    /**
     * 按规则定义构建规则实例。
     *
     * @param def 规则定义，非空
     * @return 规则实例
     * @throws IllegalArgumentException type 不识别或必填参数缺失
     */
    public static ValidationRule build(final RuleDefinitionProperties.RuleDef def) {
        return switch (def.getType()) {
            case "ENUM" -> new EnumMembershipRule(def.getField(), Set.copyOf(def.getAllowed()));
            case "CONDITIONAL_REQUIRED" -> new ConditionalRequiredRule(
                    def.getField(), def.getTriggerField(), v -> !v.isBlank());
            case "CROSS_FIELD" -> new CrossFieldComparisonRule(
                    def.getField(), def.getCompareField(),
                    CrossFieldComparisonRule.Operator.valueOf(def.getOperator()));
            default -> throw new IllegalArgumentException("未知规则类型: " + def.getType());
        };
    }
}
```

> **实施提示:** `MessageType.byMsgNo(String)` 实测返回 `Optional<MessageType>`（已在上方代码用 orElseThrow 处理，并须补一条"未知 msgNo 配置 → IllegalArgumentException"单测）。`@ConfigurationProperties(RuleDefinitionProperties)` 实测**未**被 `FepApplication` 的 `@EnableConfigurationProperties({JwtProperties.class})` 包含，须在 `ProcessorAutoConfiguration` 加 `@EnableConfigurationProperties(RuleDefinitionProperties.class)`（见下方 Step 3b），@SpringBootTest IT 须确保扫到该 config。CONDITIONAL_REQUIRED 的 triggerCondition 配置化在 Phase 1 简化为"触发字段非空即要求"，完整谓词表达式 DSL 留 Phase 2。

- [ ] **Step 3b: 装配 @EnableConfigurationProperties（实测 ProcessorAutoConfiguration 空骨架）**

```java
// fep-processor/.../ProcessorAutoConfiguration.java —— 加注解使 RuleDefinitionProperties 被绑定
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.puchain.fep.processor.validation.rule.RuleDefinitionProperties;

@Configuration
@EnableConfigurationProperties(RuleDefinitionProperties.class)
public class ProcessorAutoConfiguration {
}
```
> 实测 `FepApplication` 的 `@EnableConfigurationProperties({JwtProperties.class})` 不含 RuleDefinitionProperties；@Component（MessageRuleRegistry/ConfiguredRuleFactory）靠 `@ComponentScan("com.puchain.fep")` 扫描，但 @ConfigurationProperties 类须显式 enable。

- [ ] **Step 3c: 补 ConfiguredRuleFactory 未知 msgNo 测试**

```java
// ConfiguredRuleFactoryTest.java —— 追加
    @Test
    void registerConfiguredRules_shouldThrowOnUnknownMsgNo() {
        RuleDefinitionProperties props = new RuleDefinitionProperties();
        props.setRules(java.util.Map.of("0000", java.util.List.of(enumDef())));
        ConfiguredRuleFactory factory = new ConfiguredRuleFactory(props, new MessageRuleRegistry());
        assertThatThrownBy(factory::registerConfiguredRules)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("未知报文号");
    }
    // enumDef(): 构造一个 type=ENUM 的 RuleDef helper
```

- [ ] **Step 4: 配置示例规则（application-test.yml，明确标注示例）**

```yaml
# fep-processor/src/test/resources/application-test.yml
# ⚠️ 以下为框架验证用示例规则，非权威规则母本（母本由 ② 领域专家按人行规范填充）
fep:
  validation:
    rules:
      "3116":
        - type: ENUM
          field: Currency
          allowed: [CNY, USD]
```

- [ ] **Step 5: 运行测试 + 全模块回归**

```bash
mvn -q -pl fep-processor test
```
期望: `BUILD SUCCESS`，单测 + 端到端 IT 全绿

- [ ] **Step 6: spotbugs + ArchUnit 自检**

```bash
mvn -q -pl fep-processor spotbugs:check
mvn -q -pl fep-processor test -Dtest=ProcessorArchitectureTest
```

- [ ] **Step 7: 更新文档（均 file write only，禁 git add/commit — 红线 `feedback_fep_docs_repo_commit_taboo`）**
  - `/Users/muzhou/FEP/docs/plans/prd-traceability-matrix.md`：FR-WEB-AUDIT (§5.8) 状态 🟡 → 🟢（业务规则引擎框架 Phase 1 实施；多级审核/异常可视化/规则母本 deferred）+ 反向追溯段补本 Plan 覆盖 FR
    > ⚠️ 实测该矩阵**不在** FEP_v1.0 git 仓库（`ls docs/plans/prd-traceability-matrix.md` → No such file），唯一实体在 `/Users/muzhou/FEP/docs/plans/`（非 git 树）→ **仅 file write，禁 git add**
  - `/Users/muzhou/FEP/CLAUDE.md`「当前项目状态」补一行里程碑（file write only）
  - `/Users/muzhou/FEP/docs/progress-reconciliation.md` §5 对账记录追加一行（file write only）

- [ ] **Step 8: 提交（仅代码；/FEP/docs/* 全部 file write only 不 commit）**

```bash
git add fep-processor/src/
git commit -m "$(cat <<'EOF'
feat(processor): config-driven rule registration + business rule engine integration test

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

## 回归验收（红线 `feedback_plan_regression_scope_explicit`）

**强验收（必须达成）:**
- `mvn -q -pl fep-processor test` 全绿（新增 ~18 测试：Task1=6 + Task2=3 + Task3=3 + Task4=2 + Task5=ConfiguredRuleFactory 2 + IT ~2；落地后 `grep -c @Test` 实算更新 + 现有 fep-processor 测试 0 回归）
- `mvn -q -pl fep-processor spotbugs:check` BugInstance size 0
- `mvn -q -pl fep-processor test -Dtest=ProcessorArchitectureTest` PASS（依赖上限 + 命名 + 层依赖）

**最低验收（环境受限时）:**
- 各 Task `-pl fep-processor -am` 单模块编译 + 对应 `-Dtest=` 测试绿
- GHA CI 兜底全 reactor verify（billing 恢复后）

**明示不在本 Plan 范围:**
- Batch / Async 流水线接入业务规则关（Phase 1 仅 Sync；同型 follow-up）
- §5.8 多级审核工作流（Phase 2 独立 Plan）
- §5.8 异常处理流程可视化 UI（Phase 3 独立 Plan，需原型）
- 权威规则母本填充（mode C，② 领域专家，非 AI 范围）
- 谓词表达式 DSL（CONDITIONAL_REQUIRED 完整条件配置化，Phase 2）

---

## 闭环 Task（session-end / worktree teardown）

- [ ] 全 Task commit 完成后，主 worktree `git fetch origin main` 重测 baseline drift
- [ ] PR 创建：`gh pr create`（base main）
- [ ] worktree 收敛：`git worktree remove /Users/muzhou/FEP_v1.0_wt-msg-rule-engine`（实测命令）
- [ ] 进度对账：`/Users/muzhou/FEP/docs/progress-reconciliation.md` §5 追加一行（FR-WEB-AUDIT 🟡→🟢 + commit SHA）

---

## Worktree 触发条件自检（红线 `feedback_worktree_for_parallel_work`）

- [ ] 跨 ≥ 3 个 Maven 模块？ — **否**（fep-processor 为主 + fep-common 单行错误码）
- [x] 与已签字未执行的 Plan 并存？ — **是**（S2 / callback-p2c 等多会话 in-flight）
- [ ] ⛔ 安全 vs AI 并行？ — 否
- [ ] TLQ tongtech 联调？ — 否
- [ ] ≥5 min long-running verify 并行？ — 可能（fep-processor @SpringBootTest IT）
- [x] muzhou/别会话 WIP 并存？ — **是**

→ 命中 ②⑥ → **必须独立 worktree** `/Users/muzhou/FEP_v1.0_wt-msg-rule-engine`，闭环含 `git worktree remove`。

---

## 自检清单

1. **PRD 覆盖度**: FR-WEB-AUDIT (§5.8) — 本 Plan 覆盖"自定义业务规则引擎"子能力；多级审核 + 异常可视化已在范围声明明示 deferred。✅
2. **安全边界**: 无 SM2/SM3/SM4/密钥/脱敏，无 ⛔ 模式 E Task。✅
3. **占位符扫描**: 代码步骤均含完整代码；测试 fixture 处的"实施提示"为 grep 复核指引（真实 API 对齐），非占位 TODO。✅
4. **类型一致性**: RuleContext/ValidationRule/MessageRuleRegistry/BusinessRuleValidator 跨 Task 引用一致。✅
5. **测试命令可执行**: 各 `-Dtest=` 与测试类名匹配。✅
6. **CLAUDE.md 更新**: Task 5 Step 7。✅
7. **验收标准完整性**: 各 Task 验收从 PRD §5.8 推导，断言值可手算。✅
8. **共享工具类**: RuleContext / ValidationRule 已登记，Task 1/2 提供，后续 import。✅
9. **核心类职责边界**: BusinessRuleValidator 声明（依赖 1 ≤7，≤300 行）。✅
10. **Worktree**: 命中 ②⑥，头部已声明具体路径分支。✅

---

## 执行交接

**⚠️ 本 Plan 未签字，禁止直接执行。** 须先：
1. **AI 独立评审**（santa-method / code-reviewer）：对照本 Plan + PRD §5.8 + plan-review-checklist 7 项
2. **muzhou 签字**：抽样核对 PRD §5.8 + 6264 范围声明 + 引擎 vs 母本切分合理性
3. 签字后选 subagent-driven（推荐）/ inline 执行

> **本 harness 特别提示:** Task 含 @SpringBootTest IT（长跑 mvn），dispatch 设计默认 **hybrid**（主对话实施 edits + 前台 mvn + commit / subagent 仅 spec+quality+whole-impl 评审），见红线 `feedback_harness_bg_detach_hybrid_default`。

---

## 批准签字

| 项 | 内容 |
|---|---|
| **AI 独立评审** | Round 1 ❌ NOT CLEARED（3 BLOCKER + 2 MAJOR + MINOR）→ 修订 → Round 2 ✅ PASS（逐条 codebase 实测核验，无新引入问题）|
| **Plan Approver** | **muzhou** |
| **批准结论** | ✅ **APPROVED** |
| **批准日期** | 2026-06-05 |
| **执行前置条件** | ① 并发会话收敛（open PR 清零 / 无别会话 mvn 活进程）② 独立 worktree `/Users/muzhou/FEP_v1.0_wt-msg-rule-engine`（分支 `feat/msg-business-rule-validation-engine`）③ 执行起始重测 baseline drift |
| **执行方式** | 收敛后另定（subagent-driven hybrid 推荐 / inline）|

> 签字后本 Plan 进入"已签字未执行"状态（worktree 触发条件 ② 对其他并发 Plan 生效）。执行须满足上述前置条件，禁止在并发未收敛时启动。
