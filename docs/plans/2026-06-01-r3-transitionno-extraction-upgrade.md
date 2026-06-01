# FEP R3 transitionNo 派生规范化升级 实施计划

> **执行方式:** 使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐 Task 执行。步骤使用 `- [ ]` 复选框跟踪。

**目标:** 修复 `TlqInboundListener` 用 msgId 末 8 位伪派生 transitionNo 的占位缺陷 —— 改为从 inbound 报文业务头（BatchHead/RealHead 内 `<TransitionNo>`）提取真实值，msgId 末 8 位降级为仅 XML 异常时的 fallback。

**前置依赖:** P3 wiring（`560e4aa..6dc438b`）已 ship；ADR `2026-05-05-inbound-realhead-extraction-blocked.md` R3 已由 muzhou 2026-06-01 授权部分解锁。

**执行 Worktree:** `/Users/muzhou/FEP_v1.0_wt-r3-transitionno`（分支 `fix/r3-transitionno-extraction`，触发条件第 ② 项「与已签字未执行 Plan 并存」+ 红线 `feedback_worktree_isolates_fs_not_logic_domain` 第 7 项「多会话活跃自身即触发」—— 当前 main + 4 别会话 worktree（callback-v04opt / p4-msg-l 进行中 + callback-p2 / simplify-q-drain 已 merged 可清理）+ 20 untracked 签字 Plan 并存）
> 红线 `feedback_worktree_for_parallel_work` 触发条件: ① 跨 ≥3 模块 refactor ② 与已签字 Plan 并存 ③ ⛔ 安全 vs AI 并行 ④ TLQ tongtech 联调 ⑤ >5min long-running verify 并行 ⑥ muzhou WIP 与 AI 并存

**架构:** 新增无状态工具类 `InboundTransitionNoExtractor`，用 **XXE-hardened DOM + XPath** 从 raw payload 提取 `/CFX/MSG/*/TransitionNo` 文本值，返回 `Optional<String>`。`TlqInboundListener.onMessage` 用 `extractor.extract(payload).orElseGet(() -> deriveTransitionNo(head.getMsgId()))` 取值。**不修改 `InboundMessageDispatcher`** —— 与 P3 Task 5 finding（dispatcher 刻意用 `@XmlAnyElement(lax=true)` 避开 BatchHead，因业务头 POJO 无 `@XmlRootElement` 会回退为 DOM Element）架构决策保持一致。

**技术栈:** Java 17 / Spring Boot 3.x / Maven / JAXB（既有）/ `javax.xml.parsers` + `javax.xml.xpath`（JDK 内置，无新依赖）

**AI 协同模式:**

| 模式 | AI 占比 | 本计划中的应用 |
|:----:|:-------:|--------------|
| A | 90% | ADR Addendum / CLAUDE.md 状态 / closing |
| B | 70% | XPath 提取器 + listener wiring（业务逻辑） |

> **无安全禁入区域**：本 Plan 不触碰 SM2/SM3/SM4 / 密钥 / 签名 / 脱敏。transitionNo 是 8 位流水号（非敏感数据），不需脱敏（既有代码 LogSanitizer 已 wrap 日志输出，本 Plan 沿用）。

---

## 设计背景

### 缺陷现状（grep 实测 2026-06-01，baseline `eaf81b5`；起草时 `d8322a3` → 评审期别会话 push `eaf81b5` docs-only #33，未触及 R3 目标文件）

`fep-web/.../listener/TlqInboundListener.java:90` 当前：
```java
final String transitionNo = deriveTransitionNo(head.getMsgId());  // 占位：msgId 末 8 位
```
`deriveTransitionNo`（行 124-129）取 msgId 末 8 字符。Javadoc（行 43、115-118）误称此为「PRD §3.2.4」行为。

### 真实承载位置（code-explorer 测绘 + grep 实证）

- 全部 **21 个注册 inbound msgNo**（2101/2102/2103/2104 + 3001-3009 + 3103/3105/3107/3108/3112/3113/3115/3116）的报文，其业务头（`BatchHeadXXXX` 或 `RealHeadXXXX`）**均含 `<TransitionNo>`**（XSD `Base.xsd` RequestHead:85 / ResponseHead:107 / RequestResponseHead:139 强制 required）。无「某 msgNo 无 transitionNo」情形 —— fallback 仅针对 XML 异常/空值。
- `BatchHead3115/3107/3116/3108` 无独立 Java 类，是 XSD 元素别名，映射 `RequestBusinessHead`/`ResponseBusinessHead`/`RequestResponseHead`（均有 `getTransitionNo()`，继承 `RequestBusinessHead.java:89` `@XmlElement(name="TransitionNo", required=true)`）。
- 这些业务头类**无 `@XmlRootElement`** → `@XmlAnyElement(lax=true)` 下解析为 `org.w3c.dom.Element`，**不能 cast 为 POJO**（`cfx.getBodies()` 拿到的是 DOM Element）—— 这正是 dispatcher 刻意避开 BatchHead 的 P3 Task 5 finding 根因。
- 报文无 XML namespace（`<CFX>` 裸根，grep 实测 3115-valid.xml 无 xmlns）→ XPath 无需 namespace context。

### 占位缺陷严重性（下游消费 grep 实证）

transitionNo 取错值的真实影响：
1. **幂等键错位** — `SyncMessageProcessorService.java:111` `store.findByTransitionNo` 以 transitionNo 做幂等 key，落库 `message_process_record.transition_no`（V16，`VARCHAR(30) NOT NULL UNIQUE`）。
2. **9120 ack 对账歧义** — `AbstractAck9120InboundListener.java:168` 用 `event.transitionNo()` 组装 9120 ack outbound head 的 `<TransitionNo>`。占位取错 → 回执 TransitionNo 与原始入站不符。

占位「碰巧正确」仅因现有 fixture **有意构造** MsgId 末 8 位 = BatchHead TransitionNo（4 fixture 全相等，逆推自占位）。真实生产报文中两者独立赋值（实证反例：`fep-processor/.../samples/9006-valid.xml` MsgId 末 8=`00000003` vs TransitionNo=`00000002`，**不等**；`9100-valid.xml` 末 8=`00000010` vs `00000007`，**不等**）。

### PRD 追溯

- `FR-COMM-MSG-REQ` §3.2.3 请求类业务头（`RequestBusinessHead` 含 TransitionNo）
- `FR-COMM-MSG-RSP` §3.2.4 回执类业务头（`ResponseBusinessHead`/`RequestResponseHead`）
- `FR-WEB-MSG-INBOUND` §5.3.2.13 InboundMessageDispatcher（transitionNo 提取入口）

R3 是上述 FR 的**缺陷修复**：让 listener 尊重业务头真实 TransitionNo（§3.2.3/§3.2.4），而非 msgId 末 8 位伪派生。**不在本 Plan 范围**：本 Plan 不新增 FR，不改 PRD 矩阵 ✅/🟡 状态字段（缺陷修复非新覆盖）；矩阵仅在 closing 备注 R3 修复链接。

### 范围边界（明确排除）

- ❌ **RealHead9006/9008 不在本 Plan 范围** —— 它们有 `@XmlRootElement` + 继承 `AbstractRealHead`，但**未注册** `BODY_TYPE_REGISTRY`（dispatcher `tryUnmarshalBody` 返回 null），不在 21 个注册 msgNo 内。XPath `/CFX/MSG/*/TransitionNo` 对 9006/9008 报文同样有效（其 RealHead 也是 MSG 直接子元素含 TransitionNo），但 9006/9008 的端到端处理路径未启用，留 follow-up（视 P1c-IT-bridge 联调需求独立处理）。
- ❌ **真 broker 端到端验证** —— 本 Plan 用合成独立 fixture（transitionNo≠msgId 末 8 位）做 TDD 证伪占位，证明提取逻辑读业务头而非 msgId。真 broker 投递真值的最终验证留 follow-up（依赖 P1c-IT-bridge）。
- ❌ **不改 `InboundMessageDispatcher`** / 不改 DB schema（无新 Flyway V，最新 V29）/ 不改 dispatch 签名。

### 共享工具类清单

| 工具类 | 包 | 关键方法 | 提供者 Task | 消费者 Task |
|---|---|---|---|---|
| `InboundTransitionNoExtractor` | `web.messageinbound.listener` | `extract(String): Optional<String>`（static） | Task 1 | Task 2 |
| `LogSanitizer`（既有，复用） | `common.util` | `sanitize(String)` | — | Task 2（日志 wrap，沿用既有） |

### 核心类职责边界 — InboundTransitionNoExtractor

**负责**: 从 raw CFX payload XML 提取业务头 `<TransitionNo>` 文本值（trim 后）；XXE-hardened 解析；缺失/空白/解析失败 → `Optional.empty()`。
**不负责**: TransitionNo 格式校验（8 位数字校验归 XSD / body POJO）→ XsdValidator / RequestBusinessHead.setTransitionNo；fallback 派生（归 listener）。
**依赖上限**: 0（无 Spring 依赖，纯 static 工具）。
**行数上限**: 80 行。
**如果超出**: 不应超出（单一职责）。

---

## Task 1: InboundTransitionNoExtractor 提取器 `模式 B`

**PRD 依据:** v1.3 §3.2.3 请求类业务头 + §3.2.4 回执类业务头（TransitionNo 字段定义）
**追溯 ID:** FR-COMM-MSG-REQ / FR-COMM-MSG-RSP（对照 `docs/plans/prd-traceability-matrix.md` line 88-89）

**验收标准（从 PRD / XSD 业务规则推导）:**
1. 输入含 `<MSG><BatchHead3115>...<TransitionNo>88888888</TransitionNo>...</BatchHead3115><PlatPay3115>...</PlatPay3115></MSG>` 的 CFX XML（MsgId 末 8 位=`00000111` ≠ `88888888`）→ 返回 `Optional.of("88888888")`（证明读业务头而非 msgId）。
2. 输入真实样本 `samples/3115-valid.xml`（BatchHead TransitionNo=`00000111`）→ 返回 `Optional.of("00000111")`（真实报文形状回归）。
3. 输入 `<MSG>` 仅含无 TransitionNo 子元素的 body（如现有 listener test 的 `<BankCheckDay3116><SerialNo>...`）→ 返回 `Optional.empty()`。
4. 输入 TransitionNo 元素存在但文本空白（`<TransitionNo>   </TransitionNo>`）→ 返回 `Optional.empty()`（trim 后空）。
5. 输入畸形 XML（`<not-cfx>broken`）→ 返回 `Optional.empty()`（不抛异常）。
6. 输入含 DOCTYPE/外部实体的 XML → 解析被 XXE-hardening 拒绝 → 返回 `Optional.empty()`（不解析外部实体，安全）。

> **规则**: 提取器忠实返回业务头 TransitionNo 文本（trim），不做格式校验。空白/缺失/解析异常一律 `Optional.empty()`，由 listener 决定 fallback。

**Files:**
- Create: `fep-web/src/main/java/com/puchain/fep/web/messageinbound/listener/InboundTransitionNoExtractor.java`
- Create: `fep-web/src/test/java/com/puchain/fep/web/messageinbound/listener/InboundTransitionNoExtractorTest.java`

- [ ] **Step 1: 编写失败测试**

```java
// fep-web/src/test/java/com/puchain/fep/web/messageinbound/listener/InboundTransitionNoExtractorTest.java
package com.puchain.fep.web.messageinbound.listener;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link InboundTransitionNoExtractor}.
 *
 * <p>R3 transitionNo 派生规范化升级：验证从业务头提取真实 TransitionNo，
 * 而非 msgId 末 8 位派生。关键反占位用例 {@link #extract_bodyTransitionNo_independentOfMsgId()}
 * 故意令 TransitionNo({@code 88888888}) ≠ MsgId 末 8 位({@code 00000111})。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
class InboundTransitionNoExtractorTest {

    /** TransitionNo=88888888 故意 ≠ MsgId 末 8 位 00000111（反占位证伪）。 */
    private static final String INDEPENDENT_3115_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<CFX>"
                    + "<HEAD>"
                    + "<Version>1.0</Version>"
                    + "<SrcNode>A1000143000104</SrcNode>"
                    + "<DesNode>B43010104B0001</DesNode>"
                    + "<App>HNDEMP</App>"
                    + "<MsgNo>3115</MsgNo>"
                    + "<MsgId>20260424105000000111</MsgId>"
                    + "<CorrMsgId></CorrMsgId>"
                    + "<WorkDate>20260424</WorkDate>"
                    + "</HEAD>"
                    + "<MSG>"
                    + "<BatchHead3115>"
                    + "<SendOrgCode>A1000143000104</SendOrgCode>"
                    + "<EntrustDate>20260424</EntrustDate>"
                    + "<TransitionNo>88888888</TransitionNo>"
                    + "<Result>00000</Result>"
                    + "</BatchHead3115>"
                    + "<PlatPay3115>"
                    + "<SerialNo>SN2026042410500000000000000111</SerialNo>"
                    + "</PlatPay3115>"
                    + "</MSG>"
                    + "</CFX>";

    private static final String NO_TRANSITION_NO_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<CFX><HEAD><MsgNo>3116</MsgNo><MsgId>20260428000000000001</MsgId></HEAD>"
                    + "<MSG><BankCheckDay3116><SerialNo>SN20260428BANK</SerialNo></BankCheckDay3116></MSG></CFX>";

    private static final String BLANK_TRANSITION_NO_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<CFX><HEAD><MsgNo>3115</MsgNo></HEAD>"
                    + "<MSG><BatchHead3115><TransitionNo>   </TransitionNo></BatchHead3115></MSG></CFX>";

    private static final String XXE_PROBE_XML =
            "<?xml version=\"1.0\"?>"
                    + "<!DOCTYPE foo [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>"
                    + "<CFX><MSG><BatchHead3115><TransitionNo>&xxe;</TransitionNo></BatchHead3115></MSG></CFX>";

    @Test
    @DisplayName("业务头 TransitionNo 与 msgId 末 8 位独立 → 提取业务头值（反占位证伪）")
    void extract_bodyTransitionNo_independentOfMsgId() {
        assertThat(InboundTransitionNoExtractor.extract(INDEPENDENT_3115_XML))
                .contains("88888888");
    }

    @Test
    @DisplayName("真实样本 3115-valid.xml → 提取 BatchHead 内 00000111")
    void extract_realSample3115_returnsBatchHeadValue() throws IOException {
        final String xml = readSample("samples/3115-valid.xml");
        assertThat(InboundTransitionNoExtractor.extract(xml)).contains("00000111");
    }

    @Test
    @DisplayName("MSG 无 TransitionNo 子元素 → empty")
    void extract_noTransitionNo_returnsEmpty() {
        assertThat(InboundTransitionNoExtractor.extract(NO_TRANSITION_NO_XML)).isEmpty();
    }

    @Test
    @DisplayName("TransitionNo 文本空白 → empty")
    void extract_blankTransitionNo_returnsEmpty() {
        assertThat(InboundTransitionNoExtractor.extract(BLANK_TRANSITION_NO_XML)).isEmpty();
    }

    @Test
    @DisplayName("畸形 XML → empty（不抛异常）")
    void extract_malformedXml_returnsEmpty() {
        assertThat(InboundTransitionNoExtractor.extract("<not-cfx>broken</not-cfx>")).isEmpty();
    }

    @Test
    @DisplayName("含 DOCTYPE 外部实体 → XXE-hardening 拒绝 → empty")
    void extract_xxePayload_rejectedReturnsEmpty() {
        assertThat(InboundTransitionNoExtractor.extract(XXE_PROBE_XML)).isEmpty();
    }

    private static String readSample(final String path) throws IOException {
        try (InputStream in = InboundTransitionNoExtractorTest.class
                .getClassLoader().getResourceAsStream(path)) {
            assertThat(in).as("test resource %s present", path).isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd /Users/muzhou/FEP_v1.0_wt-r3-transitionno
./mvnw test -pl fep-web -am -Dtest=InboundTransitionNoExtractorTest -Dsurefire.failIfNoSpecifiedTests=false
```
期望: 编译失败 — `cannot find symbol: class InboundTransitionNoExtractor`

- [ ] **Step 3: 编写最小实现**

```java
// fep-web/src/main/java/com/puchain/fep/web/messageinbound/listener/InboundTransitionNoExtractor.java
package com.puchain.fep.web.messageinbound.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Extracts the authoritative {@code TransitionNo} from an inbound CFX payload's
 * business head (BatchHead/RealHead) element.
 *
 * <p>R3 transitionNo 派生规范化升级：PRD §3.2.3/§3.2.4 业务头携带真实
 * {@code TransitionNo}（8 位流水号）。本类用 XXE-hardened DOM + XPath
 * {@code /CFX/MSG/*}{@code /TransitionNo} 直接读取业务头文本值，替代
 * {@code TlqInboundListener} 历史占位（msgId 末 8 位伪派生）。</p>
 *
 * <p>与 {@code InboundMessageDispatcher} P3 Task 5 finding 一致：业务头
 * POJO（{@code RequestBusinessHead} 等）无 {@code @XmlRootElement}，在
 * {@code @XmlAnyElement(lax=true)} 下解析为 DOM Element 不可 cast，故不走
 * JAXB POJO 路径而用 XPath 直读 raw XML。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
final class InboundTransitionNoExtractor {

    private static final Logger LOG = LoggerFactory.getLogger(InboundTransitionNoExtractor.class);

    /** 业务头 TransitionNo 文本：CFX/MSG 任一直接孙元素（仅 BatchHead/RealHead 含此元素）。 */
    private static final String TRANSITION_NO_XPATH = "/CFX/MSG/*/TransitionNo/text()";

    /**
     * XXE-hardened factory built once at class-load. A parser that cannot apply
     * the required hardening features fails fast here (loud {@link IllegalStateException}
     * at startup) rather than silently degrading every extraction to the msgId
     * fallback at runtime — see MAJOR-1 review finding.
     */
    private static final DocumentBuilderFactory DBF = createHardenedFactory();

    private InboundTransitionNoExtractor() {
        // utility class — no instances
    }

    /**
     * Extract the business-head {@code TransitionNo} from a CFX payload.
     *
     * @param payloadXml the raw CFX XML string, may be {@code null}
     * @return trimmed TransitionNo, or {@link Optional#empty()} when absent,
     *         blank, or the payload cannot be parsed safely
     */
    static Optional<String> extract(final String payloadXml) {
        if (payloadXml == null || payloadXml.isBlank()) {
            return Optional.empty();
        }
        try {
            // Parse-time errors (malformed XML, DOCTYPE rejected by hardening,
            // XPath eval) are legitimate "no real value" cases → caller falls back.
            // Note: parser CONFIG errors already failed fast at class-load (DBF init),
            // so reaching here means the parser itself is healthy.
            final Document doc = DBF.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(
                            payloadXml.getBytes(StandardCharsets.UTF_8)));
            final XPath xpath = XPathFactory.newInstance().newXPath();
            final String value = (String) xpath.evaluate(
                    TRANSITION_NO_XPATH, doc, XPathConstants.STRING);
            if (value == null || value.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(value.trim());
        } catch (ParserConfigurationException | org.xml.sax.SAXException
                | java.io.IOException | javax.xml.xpath.XPathExpressionException e) {
            LOG.debug("TransitionNo extract failed, will fall back to derived: {}",
                    e.getMessage());
            return Optional.empty();
        }
    }

    private static DocumentBuilderFactory createHardenedFactory() {
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            // XXE hardening — inbound payload is external, untrusted.
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        } catch (ParserConfigurationException e) {
            // fail-fast: a parser that cannot be hardened must NOT silently process
            // untrusted XML (and must not silently degrade every extraction to fallback).
            throw new IllegalStateException(
                    "XML parser does not support required XXE-hardening features", e);
        }
        dbf.setExpandEntityReferences(false);
        dbf.setXIncludeAware(false);
        dbf.setNamespaceAware(false);
        return dbf;
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

```bash
cd /Users/muzhou/FEP_v1.0_wt-r3-transitionno
./mvnw test -pl fep-web -am -Dtest=InboundTransitionNoExtractorTest -Dsurefire.failIfNoSpecifiedTests=false
```
期望: `BUILD SUCCESS`, 6 tests passed

- [ ] **Step 5: 全 fep-web 回归（红线 `feedback_full_regression_before_commit`）**

```bash
cd /Users/muzhou/FEP_v1.0_wt-r3-transitionno
./mvnw verify -pl fep-web -am --batch-mode --no-transfer-progress
```
期望: `BUILD SUCCESS`；Checkstyle 0 / SpotBugs 0 / ArchUnit 0 / JaCoCo 达标。
> 沙盒 mvn exit 144 时按红线 `feedback_mvn_sandbox_exit144_pattern` 跳本机 + 静态等价 review + GHA CI 兜底。

- [ ] **Step 6: 提交**

```bash
cd /Users/muzhou/FEP_v1.0_wt-r3-transitionno
git add fep-web/src/main/java/com/puchain/fep/web/messageinbound/listener/InboundTransitionNoExtractor.java
git add fep-web/src/test/java/com/puchain/fep/web/messageinbound/listener/InboundTransitionNoExtractorTest.java
git commit -m "$(cat <<'EOF'
feat(web): add InboundTransitionNoExtractor for business-head TransitionNo

R3 transitionNo 派生规范化升级 Task 1. XXE-hardened DOM+XPath extracts
authoritative TransitionNo from CFX business head, replacing the msgId
last-8 placeholder. Optional return; caller falls back when absent.

PRD §3.2.3/§3.2.4 (FR-COMM-MSG-REQ/RSP)
AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

## Task 2: TlqInboundListener wiring + 反占位证伪测试 `模式 B`

**PRD 依据:** v1.3 §5.3.2.13 InboundMessageDispatcher（transitionNo 提取入口）+ §3.2.3/§3.2.4
**追溯 ID:** FR-WEB-MSG-INBOUND（对照矩阵 line 120）

**验收标准（从 PRD / 缺陷修复推导）:**
1. inbound payload 业务头 TransitionNo=`88888888`、MsgId 末 8 位=`00000111` → `dispatcher.dispatch` 被调用时 transitionNo 参数=`88888888`（**不是** `00000111`）。这是修前 RED（现取 `00000111`）/ 修后 GREEN 的核心断言。
2. inbound payload **MSG 无 BatchHead 子元素**（如 `<MSG><BankCheckDay3116><SerialNo>...`，extract→empty，MsgId 末 8=`00000001`）→ fallback → dispatch transitionNo 参数=`00000001`（向后兼容，现有 3 测试不破）。
   - 注：另一条 fallback 触发路径「BatchHead 存在但 TransitionNo 空白」由 Task 1 `extract_blankTransitionNo_returnsEmpty` 在提取器层覆盖（extract→empty 语义与本用例同），listener 层不重复集成测试。
3. 业务头 TransitionNo 与 msgId 末 8 位都缺（msgId=null/畸形/无 head）→ 既有 silent-failure 行为不变（dispatcher 不被调用 / 异常吞掉）。此路径由现有 `onMessage_malformedXml_dispatcherUntouched`（XML 解析失败进 catch）覆盖，本 Task 不新增 null-msgId 集成测试（extract empty + deriveTransitionNo null → line 91 null-check 提前 return，行为与既有一致）。

> **规则**: 验收标准 1 的断言值 `88888888` 来自 fixture 业务头有意构造的独立值，不来自代码。

**Files:**
- Modify: `fep-web/src/main/java/com/puchain/fep/web/messageinbound/listener/TlqInboundListener.java`
- Modify: `fep-web/src/test/java/com/puchain/fep/web/messageinbound/listener/TlqInboundListenerTest.java`

- [ ] **Step 1: 编写失败测试（追加到 TlqInboundListenerTest）**

在 `TlqInboundListenerTest` 中新增独立 fixture 常量 + 2 个测试方法（保留现有 3 个不动）：

```java
    /**
     * BatchHead3115 TransitionNo=88888888 故意 ≠ MsgId 末 8 位 00000111（反占位证伪）。
     */
    private static final String INDEPENDENT_3115_XML =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<CFX>"
                    + "<HEAD>"
                    + "<Version>1.0</Version>"
                    + "<SrcNode>" + FepConstants.HNDEMP_NODE_CODE + "</SrcNode>"
                    + "<DesNode>B43010104B0001</DesNode>"
                    + "<App>HNDEMP</App>"
                    + "<MsgNo>3115</MsgNo>"
                    + "<MsgId>20260424105000000111</MsgId>"
                    + "<CorrMsgId></CorrMsgId>"
                    + "<WorkDate>20260424</WorkDate>"
                    + "</HEAD>"
                    + "<MSG>"
                    + "<BatchHead3115>"
                    + "<SendOrgCode>A1000143000104</SendOrgCode>"
                    + "<EntrustDate>20260424</EntrustDate>"
                    + "<TransitionNo>88888888</TransitionNo>"
                    + "<Result>00000</Result>"
                    + "</BatchHead3115>"
                    + "<PlatPay3115>"
                    + "<SerialNo>SN2026042410500000000000000111</SerialNo>"
                    + "</PlatPay3115>"
                    + "</MSG>"
                    + "</CFX>";

    @Test
    @DisplayName("业务头 TransitionNo 覆盖 msgId 末 8 位派生 → dispatch 用业务头真值 88888888")
    void onMessage_bodyTransitionNo_overridesDerived() {
        final TlqMessage message = newMessage(INDEPENDENT_3115_XML);
        when(dispatcher.dispatch(eq("3115"), eq("88888888"), any(byte[].class)))
                .thenReturn(new InboundMessageResponse("rec-115", "COMPLETED", true));

        listener.onMessage(message);

        verify(dispatcher).dispatch(eq("3115"), eq("88888888"), any(byte[].class));
    }

    @Test
    @DisplayName("无业务头 TransitionNo → fallback msgId 末 8 位 00000001（向后兼容）")
    void onMessage_noBodyTransitionNo_fallsBackToDerived() {
        final TlqMessage message = newMessage(VALID_3116_XML);
        when(dispatcher.dispatch(eq("3116"), eq("00000001"), any(byte[].class)))
                .thenReturn(new InboundMessageResponse("rec-116", "COMPLETED", true));

        listener.onMessage(message);

        verify(dispatcher).dispatch(eq("3116"), eq("00000001"), any(byte[].class));
    }
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd /Users/muzhou/FEP_v1.0_wt-r3-transitionno
./mvnw test -pl fep-web -am -Dtest=TlqInboundListenerTest#onMessage_bodyTransitionNo_overridesDerived -Dsurefire.failIfNoSpecifiedTests=false
```
期望: 测试失败 — `dispatch(eq("3115"), eq("88888888"), ...)` 未匹配（实际仍传 msgId 末 8 位 `00000111`，because listener 当前用 deriveTransitionNo）。

- [ ] **Step 3: 编写最小实现（修改 TlqInboundListener）**

3a. 修改 `onMessage` 行 90，从业务头提取优先、占位 fallback：

```java
            final String messageType = head.getMsgNo();
            final String transitionNo = InboundTransitionNoExtractor.extract(payload)
                    .orElseGet(() -> deriveTransitionNo(head.getMsgId()));
```

3b. 更新 `deriveTransitionNo` Javadoc（行 111-123），澄清其降级为 fallback（去除「PRD §3.2.4 = 末 8 位」误读）：

```java
    /**
     * Fallback transitionNo derivation — last {@value #TRANSITION_NO_LEN}
     * characters of the CFX {@code msgId}.
     *
     * <p>R3 升级后此为**兜底**路径：仅当 {@link InboundTransitionNoExtractor}
     * 无法从业务头（BatchHead/RealHead）提取真实 {@code TransitionNo}（XML
     * 异常 / 缺失字段）时使用。正常路径取业务头真值（PRD §3.2.3/§3.2.4
     * 「按原值回填」）。历史占位语义见 ADR
     * {@code 2026-05-05-inbound-realhead-extraction-blocked.md} §R3 Addendum。</p>
     *
     * @param msgId the CFX msgId, may be {@code null}
     * @return derived transitionNo or {@code null} when {@code msgId} cannot
     *         supply at least {@link #TRANSITION_NO_LEN} characters
     */
```

3c. 更新 `TRANSITION_NO_LEN` 字段 Javadoc（行 42-44）去除「PRD §3.2.4」误标：

```java
    /**
     * Fallback transitionNo length — last 8 digits of msgId when the business
     * head TransitionNo is unavailable.
     */
```

- [ ] **Step 4: 运行测试确认通过（新增 2 + 现有 3 全绿）**

```bash
cd /Users/muzhou/FEP_v1.0_wt-r3-transitionno
./mvnw test -pl fep-web -am -Dtest=TlqInboundListenerTest -Dsurefire.failIfNoSpecifiedTests=false
```
期望: `BUILD SUCCESS`, 5 tests passed（新 2 + 旧 3 向后兼容）

- [ ] **Step 5: 全 fep-web verify（门禁自检）**

```bash
cd /Users/muzhou/FEP_v1.0_wt-r3-transitionno
./mvnw verify -pl fep-web -am --batch-mode --no-transfer-progress
```
期望: `BUILD SUCCESS`；Checkstyle 0 / SpotBugs 0（含 find-sec-bugs，提取器无 logger 注入新风险，TransitionNo 已 LogSanitizer wrap）/ ArchUnit 0 / JaCoCo 达标。
> 沙盒 exit 144 → 红线 `feedback_mvn_sandbox_exit144_pattern` 兜底。

- [ ] **Step 6: 提交**

```bash
cd /Users/muzhou/FEP_v1.0_wt-r3-transitionno
git add fep-web/src/main/java/com/puchain/fep/web/messageinbound/listener/TlqInboundListener.java
git add fep-web/src/test/java/com/puchain/fep/web/messageinbound/listener/TlqInboundListenerTest.java
git commit -m "$(cat <<'EOF'
fix(web): TlqInboundListener uses business-head TransitionNo over msgId-derived

R3 transitionNo 派生规范化升级 Task 2. onMessage now prefers
InboundTransitionNoExtractor (real BatchHead/RealHead TransitionNo) and
falls back to msgId-last-8 only on extraction miss. Fixes latent
idempotency-key / 9120-ack reconciliation drift when transitionNo !=
last8(msgId). Adds anti-placeholder test (TransitionNo 88888888 vs
msgId-last8 00000111) + backward-compat fallback test.

PRD §3.2.3/§3.2.4 §5.3.2.13 (FR-WEB-MSG-INBOUND)
AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

## Task 3: ADR Addendum + 状态收口 `模式 A`（doc-only / 无 git commit）

**PRD 依据:** N/A（治理/文档元流程 Task，缺陷修复闭环登记）
**追溯 ID:** N/A

> **⚠️ 本 Task 全部产物在非 git tracked 路径**（`/Users/muzhou/FEP/docs/*` + `/Users/muzhou/FEP/CLAUDE.md`，红线 `feedback_fep_docs_repo_commit_taboo`）→ **仅 file write，NO git add / NO commit**。代码仓库内唯一动作是 closing 的 worktree remove。

- [ ] **Step 1: ADR Addendum（file write only，无 commit）**

向 `/Users/muzhou/FEP/docs/decisions/2026-05-05-inbound-realhead-extraction-blocked.md` 追加 §R3 Addendum（2026-06-01）：
- 记录 muzhou 2026-06-01 授权 R3 部分解锁
- 记录关键发现：4 inbound fixture（3115/3107/3116/3108-valid.xml）TransitionNo 全=MsgId 末 8 位 = 逆推自占位，**不构成 ADR 矩阵所述「真解锁条件」**（修正矩阵误导）
- 记录实现：路径 C（`InboundTransitionNoExtractor` XPath，不动 dispatcher）+ 合成独立 fixture（TransitionNo `88888888`≠msgId 末 8）TDD 证伪
- 记录 follow-up（仍 BLOCKED 子集）：① RealHead9006/9008 端到端（未注册 BODY_TYPE_REGISTRY，待 P1c-IT-bridge）② 真 broker 投递真值最终验证
- R3 状态：占位缺陷修复 ✅ DONE；真 broker 验证 deferred（不阻断 ship，红线 `feedback_systemic_ci_blocker_defers_positive_backing` 同型 tier 划分）

- [ ] **Step 2: CLAUDE.md「下一步候选」状态更新（file write only，无 commit）**

`/Users/muzhou/FEP/CLAUDE.md` 候选 #4 R3 从 🚫 BLOCKED 改为：✅ 占位缺陷修复 ship（commit SHA 回填）+ 真 broker 验证 deferred follow-up + ADR Addendum 引用。

- [ ] **Step 3: closing — worktree remove + 回归确认**

```bash
# 全 commit 已 push + PR 创建后执行
cd /Users/muzhou/FEP_v1.0
git worktree list                       # 确认 wt-r3-transitionno 存在
git -C /Users/muzhou/FEP_v1.0_wt-r3-transitionno status --short   # 期望干净
git -C /Users/muzhou/FEP_v1.0_wt-r3-transitionno log origin/fix/r3-transitionno-extraction..HEAD --oneline  # 期望空（已 push）
git worktree remove /Users/muzhou/FEP_v1.0_wt-r3-transitionno
git worktree list                       # 确认已移除
```

> Task 3 无 git commit（产物全在非 tracked 知识库）。Plan 文件 `docs/plans/2026-06-01-r3-transitionno-extraction-upgrade.md` 的 `docs(plans): signed` commit 在签字后、Task 1 前于 worktree 内单独执行（标准流程）。

---

## 自检清单

1. **PRD 覆盖度**: R3 是 FR-COMM-MSG-REQ/RSP（§3.2.3/§3.2.4）+ FR-WEB-MSG-INBOUND（§5.3.2.13）缺陷修复，无新 FR；不在范围项（RealHead9006/9008 + 真 broker 验证）已在「范围边界」明示。✅
2. **安全边界**: 无 SM2/SM3/SM4/密钥/脱敏；XXE-hardening 是防御性解析非加密。✅
3. **占位符扫描**: 无 TBD/TODO/待/类似 Task。✅
4. **类型一致性**: `InboundTransitionNoExtractor.extract(String): Optional<String>`（Task 1 定义）= Task 2 调用一致；`dispatcher.dispatch(String,String,byte[])` 与实测签名一致。✅
5. **测试命令可执行**: `-Dtest=InboundTransitionNoExtractorTest` / `TlqInboundListenerTest` 与类名匹配；用 `-Dsurefire.failIfNoSpecifiedTests=false`（红线 `feedback_surefire3_failifno_specified_tests_param_rename`）。✅
6. **CLAUDE.md 更新**: Task 3 Step 2 更新「下一步候选 #4」。✅
7. **验收标准完整性**: 各 Task 验收值（`88888888`/`00000111`/`00000001`）来自 fixture 有意构造 + 真实样本，可手算核对。✅
8. **共享工具类**: `InboundTransitionNoExtractor` 已登记清单（Task 1 提供 / Task 2 消费）。✅
9. **核心类职责边界**: `InboundTransitionNoExtractor` 依赖 0 / ≤80 行声明已列。✅
10. **Worktree 触发条件**: 命中第 ② 项 + 多会话第 7 项 → 头部已声明 `wt-r3-transitionno` + closing 含 `git worktree remove` 实测命令。✅

---

## 执行交接

**⚠️ 本 Plan 未签字，禁止直接执行。** 流程：
1. **AI 独立评审**（santa-method）— 输入本 Plan 全文 + PRD §3.2.3/§3.2.4/§5.3.2.13 + plan-review-checklist 7 项 + 重点核查路径 C 与 P3 Task 5 架构一致性 / XPath 对多 body 报文的正确性 / fallback 向后兼容。
2. **muzhou 签字** — 阅读 Plan + AI 评审报告 + 抽样核对。
3. **执行选择** — subagent 驱动（推荐）/ 内联执行。

---

## muzhou 批准签字

- **AI 评审**: ✅ PASS（feature-dev:code-reviewer 独立 reviewer 2 轮，2026-06-01）— Round 1 REVISE（BLOCKER-1 `readSample` static `getClass()` 编译错误 + MAJOR-1 XXE `setFeature` 被宽泛 `catch(Exception)` 吞掉致提取器全局静默退化假绿）→ 修订（`getClass()`→类字面量 + 静态 hardened factory fail-fast + catch 收窄解析期异常 + boil-lake MAJOR-2/MINOR-1/2）→ Round 2 ✅ PASS（8 项 API claim 全 grep VERIFIED：TlqInboundListener:90 / dispatch 签名 / RequestBusinessHead 无 @XmlRootElement / CfxMessage lax / 3115-valid.xml TransitionNo=00000111 / VALID_3116_XML 无 BatchHead / body POJO 无 TransitionNo 子元素 / XPath 多 body 不误匹配）；plan-review-checklist 7 项无违规；门禁标注合规
- **baseline**: 起草 `d8322a3` → 签字 `eaf81b5`（评审期别会话 push #33 docs-only，grep 实测未触及 R3 目标文件，无害）
- **muzhou 批准**: ✅ APPROVED（2026-06-01）— 执行方式：subagent-driven-development
