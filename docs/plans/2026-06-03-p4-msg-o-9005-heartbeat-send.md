# FEP P4-MSG-O 9005 心跳发送能力（最小）实施计划 v0.2

> **执行方式:** 使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐 Task 执行。步骤使用 `- [ ]` 复选框跟踪。

## v0.1 → v0.2 修订纪要（2026-06-03）

v0.1 起草 → Round 1 AI 评审 santa-method **REVISE**（API 真实性/安全边界/head-only 测试全 PASS；2 BLOCKER 均 Plan-vs-codebase 偏离非设计缺陷）→ boil-lake 修订：
1. **CRLF 策略实测纠正**：v0.1 误指 heartbeat 加方法级 `@SuppressFBWarnings`；实测 TlqNodeLoginService **无任何 @SuppressFBWarnings**，CRLF 由 `spotbugs-exclude.xml:1270-1273` **整类 Match** 覆盖 → heartbeat 自动覆盖**无需注解**（加注解反风格不一致）。删 Step 3 注解 + import note + 验收 #5 + 红线表 T2 + Step 5 回归验证文案（Round 2 补漏）。
2. **lifecycle 论据纠正**：`NodeLifecycleManager.handleHeartbeat()` **存在**（inbound 非状态变更），架构段补澄清（结论 heartbeat 不调 lifecycle 仍正确）。
3. **head-only 测试锚点（advisory）**：T2 Step 1 显式 `getBodies().hasSize(1)` + `doesNotContain("LoginRequest")` 防 head-only 退化假绿。

**目标:** PRD v1.3 §4.5 + §5.7.5 — `TlqNodeLoginService` 加 `heartbeat(nodeId)` 主动发送 head-only 9005 节点心跳能力（fire-and-forget，镜像 login/logout，sign=false，无 lifecycle 状态变更）。

**前置依赖:**
- P1c T7（TlqNodeLoginService login/logout 9006/9008 编排）— origin/main
- P4-MSG-N（9005 dispatcher 覆盖注册）— origin/main `46d0a25`

**执行 Worktree:** `/Users/muzhou/FEP_v1.0_wt-p4-msg-o`（分支 `feat/p4-msg-o-9005-heartbeat-send`，触发条件第 2 项 + 第 6 项）
> 多别会话 Plan 并存 + 多并行 AI 会话活跃 — 红线 `feedback_worktree_isolates_fs_not_logic_domain`

**Baseline:** origin/main `cb5733f`（2026-06-03 实测，红线 `feedback_baseline_drift_during_long_review_cycle`）

**架构:**
- `heartbeat(nodeId)`：findById → `build9005Message`（CommonHead MsgNo=9005 + RealHead9005，**head-only 无 body**）→ `encoder.encode(cfx, defaultPipelineOpts())`（sign=false）→ `producer.send` → return success。**无 lifecycle 调用**（心跳是 keepalive 不改 ONLINE/OFFLINE，区别于 login() 调 `lifecycle.login()`）。
  > v0.2 评审纠正：`NodeLifecycleManager` 有 `handleHeartbeat()`（实测 `:32`），但它是 **inbound 接收 9005 时调用 + 不改 state**（2 impl 仅 LOG，测试 `handleHeartbeat_shouldNotMutateState` 断言）；与本 Plan **outbound 主动发送** 9005 正交。outbound heartbeat 发送不调 lifecycle 正确。
- **新建 `RealHead9005`**（fep-converter model）：`@XmlRootElement(name="RealHead9005") extends AbstractRealHead`（平行 RealHead9006/9008，无额外字段；3 字段 SendOrgCode/EntrustDate/TransitionNo 在父类）。
- **安全**: sign=false（镜像 9006/9008，节点登录前无业务密钥）→ **无新 signer/security 代码，非 ⛔ AI 禁入**。

**技术栈:** Java 17 / Spring Boot 3.x / JAXB 4 / Maven / JUnit 5 / Mockito / AssertJ

**AI 协同模式:** A（90%）— 节点心跳发送编排 + trivial JAXB POJO，无安全代码。**无 ⛔ 模式 E**。

**PR 大小预估:** RealHead9005 ~18 + test ~40 + heartbeat 方法 ~25 + build9005Message ~20 + heartbeat test ~35 ≈ **~140 LOC**。

---

## 设计背景

### PRD 依据

PRD v1.3 §4.5 通用 9 报文 9005 节点心跳 + §5.7.5 连通性测试。`TlqConnectivityController` 实测 Javadoc："实际 9005 心跳发送待 P1 TLQ SDK 就绪后接入"（P1c 已就绪）。本 Plan 实装**发送能力**；连通性 RTT rewire / 调度 / 往返关联属独立 initiative（范围外）。

### 实测事实（grep 确认，origin/main cb5733f / worktree）

- `RealHead9006` = `@XmlRootElement(name="RealHead9006") extends AbstractRealHead {}`（仅 18 行，无字段）；`AbstractRealHead` propOrder `{sendOrgCode, entrustDate, transitionNo}`（14/8/8 字符）。**RealHead9005 不存在，需新建**。
- `TlqNodeLoginService.login(nodeId)`（实测 line 137-162）：
  ```
  findById(nodeId) (BIZ_5015 if absent) → build9006Message → encoder.encode(cfx, defaultPipelineOpts())
  → new TlqMessage(payload, TlqMessageAttributes.forRealtime(IdGenerator.uuid20()), TlqChannel.REALTIME_SEND)
  → producer.send → if !success LOG.warn return false; LOG.info; return lifecycle.login()
  ```
- `build9006Message`（line 216-238）：CommonHead(Version "1.0"/SrcNode srcNode/DesNode HNDEMP_DEST_NODE/App "HNDEMP"/MsgNo "9006"/MsgId bodyMsgIdGenerator.generate()/CorrMsgId CORR_MSG_ID_NEW_SESSION/WorkDate now) + RealHead9006(SendOrgCode srcNode/EntrustDate workDate/TransitionNo deriveTransitionNo(msgId)) + LoginRequest9006 → `CfxMessage.of(commonHead, realHead, body)`
- `CfxMessage.of(CommonHead head, Object... bodies)` 跳 null → **head-only 支持**（`CfxMessage.of(commonHead, realHead9005)` 单 head 元素无 body）
- 字段：lifecycle/producer/encoder/nodeRepository/srcNode/brokerPassword/bodyMsgIdGenerator；常量 DATE_FMT/HNDEMP_DEST_NODE/CORR_MSG_ID_NEW_SESSION
- `defaultPipelineOpts()` 设 `opts.setSign(false)`
- TlqNodeLoginServiceTest mock：lifecycle/producer/encoder/nodeRepository/bodyMsgIdGenerator；`when(encoder.encode(any,any)).thenReturn(SAMPLE_ENCODED)` + `when(bodyMsgIdGenerator.generate())` + login test `when(findById)`/`when(producer.send)`/`when(lifecycle.login())`
- RealHead9006Test：JAXBContext roundtrip 3 字段 + element name

### ⚠️ CRLF 红线（v0.2 评审实测纠正 — P4-MSG-L 教训本类已有既定机制）

新 `heartbeat()` 方法含 `LOG.warn`/`LOG.info`（镜像 login）→ 每个 log 参数必须 `LogSanitizer.sanitize()` wrap（与 login/logout 完全镜像）。

**关键实测（v0.2 评审纠正 v0.1 错误策略）**: `TlqNodeLoginService` 现有 login/logout **无 `@SuppressFBWarnings`**（grep 实测 0 命中）；CRLF_INJECTION_LOGS 误报由 **`spotbugs-exclude.xml:1270-1273` 整类 Match** 覆盖（`<Class name="...TlqNodeLoginService"/>` + `<Bug pattern="CRLF_INJECTION_LOGS"/>` + `EI_EXPOSE_REP2`）。→ **新 heartbeat 方法自动被现有整类 Match 覆盖，无需任何 @SuppressFBWarnings 注解/import**（加注解反而风格不一致违反自检 #9）。红线 `feedback_logsanitizer_alone_insufficient_for_findsecbugs` 在本类以"整类 exclude"形式满足（非方法级注解）。implementer 仅需 LogSanitizer.sanitize wrap + 信赖现有整类 exclude；Step 5 全 reactor verify spotbugs gate 确认 0 CRLF。

---

## 文件结构设计

| 文件路径 | 职责 | 操作 | AI 模式 |
|---|---|---|:---:|
| `fep-converter/.../model/RealHead9005.java` | 9005 心跳业务头 POJO（@XmlRootElement RealHead9005 extends AbstractRealHead）| 新建 | A |
| `fep-converter/.../model/RealHead9005Test.java` | JAXB roundtrip 3 字段 + element name（镜像 RealHead9006Test）| 新建 | A |
| `fep-web/.../tlq/node/service/TlqNodeLoginService.java` | 加 heartbeat(nodeId) + build9005Message（head-only）| 修改 | A |
| `fep-web/.../tlq/node/service/TlqNodeLoginServiceTest.java` | heartbeat 单测（send 成功/失败 + 无 lifecycle 调用 + 9005 head-only 装配）| 修改 | A |

**总: 1 新 POJO + 1 新 POJO 测试 + 1 改 service + 1 改 test = 4 文件**

### 共享工具类 / 职责边界
无新增共享工具。TlqNodeLoginService 现 7 字段（依赖上限内），加方法不增依赖。

---

## Task 1: RealHead9005 POJO + JAXB 测试 `模式 A`

**PRD 依据:** v1.3 §3.2.2 + §4.5
**追溯 ID:** FR-MSG-9005

**验收标准:**
1. `RealHead9005` `@XmlRootElement(name="RealHead9005")` + extends AbstractRealHead，无额外字段
2. JAXB marshal `new RealHead9005()` set 3 字段 → 根元素名 `<RealHead9005>` + 含 `<SendOrgCode>/<EntrustDate>/<TransitionNo>`
3. roundtrip（marshal+unmarshal）3 字段保持

**Files:**
- Create: `fep-converter/src/main/java/com/puchain/fep/converter/model/RealHead9005.java`
- Create: `fep-converter/src/test/java/com/puchain/fep/converter/model/RealHead9005Test.java`

- [ ] **Step 1: 编写失败测试 RealHead9005Test**（镜像 RealHead9006Test，implementer Read 它复制结构改 9006→9005）

```java
package com.puchain.fep.converter.model;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 单元测试：{@link RealHead9005} 9005 节点心跳业务头 POJO（镜像 {@link RealHead9006Test}）。
 *
 * @author FEP Team
 * @since 1.0.0
 */
class RealHead9005Test {

    @Test
    void roundtrip_shouldPreserve3Fields() throws Exception {
        RealHead9005 head = new RealHead9005();
        head.setSendOrgCode("10000000000001");
        head.setEntrustDate("20260421");
        head.setTransitionNo("00000005");

        JAXBContext ctx = JAXBContext.newInstance(RealHead9005.class);
        Marshaller m = ctx.createMarshaller();
        StringWriter sw = new StringWriter();
        m.marshal(head, sw);
        String xml = sw.toString();

        assertThat(xml).contains("<RealHead9005>")
                .contains("<SendOrgCode>10000000000001</SendOrgCode>")
                .contains("<EntrustDate>20260421</EntrustDate>")
                .contains("<TransitionNo>00000005</TransitionNo>");

        Unmarshaller u = ctx.createUnmarshaller();
        RealHead9005 back = (RealHead9005) u.unmarshal(new StringReader(xml));
        assertThat(back.getSendOrgCode()).isEqualTo("10000000000001");
        assertThat(back.getEntrustDate()).isEqualTo("20260421");
        assertThat(back.getTransitionNo()).isEqualTo("00000005");
    }
}
```

- [ ] **Step 2: 运行确认失败**

```bash
cd /Users/muzhou/FEP_v1.0_wt-p4-msg-o && ./mvnw test -pl fep-converter -Dtest=RealHead9005Test -Dsurefire.failIfNoSpecifiedTests=false
```
期望: 编译失败 `cannot find symbol: class RealHead9005`。

- [ ] **Step 3: 新建 RealHead9005**（镜像 RealHead9006，仅改 element name + Javadoc）

```java
package com.puchain.fep.converter.model;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * 9005 节点心跳请求业务头 POJO（PRD v1.3 §3.2.2 + §4.5）。
 *
 * <p>结构与字段约束见父类 {@link AbstractRealHead}；本类仅声明
 * {@code @XmlRootElement(name="RealHead9005")} 区分根元素名（head-only 心跳报文无 body）。</p>
 *
 * <p>P4-MSG-O 创建（TlqNodeLoginService.build9005Message 依赖；与 {@link RealHead9006}/{@link RealHead9008}
 * 共享 3 字段父类 {@link AbstractRealHead}）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@XmlRootElement(name = "RealHead9005")
public class RealHead9005 extends AbstractRealHead {
}
```

- [ ] **Step 4: 运行确认通过**

```bash
cd /Users/muzhou/FEP_v1.0_wt-p4-msg-o && ./mvnw test -pl fep-converter -Dtest=RealHead9005Test -Dsurefire.failIfNoSpecifiedTests=false
```
期望: BUILD SUCCESS。

- [ ] **Step 5: Commit**

```bash
cd /Users/muzhou/FEP_v1.0_wt-p4-msg-o
git add fep-converter/src/main/java/com/puchain/fep/converter/model/RealHead9005.java
git add fep-converter/src/test/java/com/puchain/fep/converter/model/RealHead9005Test.java
git commit -m "$(cat <<'EOF'
feat(converter): P4-MSG-O T1 — RealHead9005 心跳业务头 POJO

@XmlRootElement(name=RealHead9005) extends AbstractRealHead（平行 RealHead9006/9008，head-only 无 body）
RealHead9005Test JAXB roundtrip 3 字段 + element name

PRD: v1.3 §3.2.2 + §4.5 (FR-MSG-9005)

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

## Task 2: TlqNodeLoginService.heartbeat(nodeId) head-only 发送 + 单测 `模式 A`

**PRD 依据:** v1.3 §4.5 + §5.7.5
**追溯 ID:** FR-MSG-9005

**验收标准:**
1. `heartbeat(nodeId)` node 不存在 → `FepBusinessException(BIZ_5015)`（同 login）
2. node 存在 + `producer.send` 成功 → return `true`，**不调 `lifecycle.login/logout`**（fire-and-forget 无状态变更）
3. `producer.send` 失败 → LOG.warn + return `false`
4. `build9005Message` 装配：CommonHead MsgNo="9005" + RealHead9005（SendOrgCode/EntrustDate/TransitionNo）+ **无 body**（CfxMessage.of(commonHead, realHead9005)）；encode 用 sign=false（defaultPipelineOpts）
5. 心跳 log 参数全 LogSanitizer.sanitize（镜像 login/logout）；CRLF 由 `spotbugs-exclude.xml` 整类 Match 覆盖**无需注解**（红线在本类以整类 exclude 形式满足）

**Files:**
- Modify: `fep-web/.../tlq/node/service/TlqNodeLoginService.java`
- Modify: `fep-web/.../tlq/node/service/TlqNodeLoginServiceTest.java`

- [ ] **Step 1: 编写失败测试**（镜像 login test，implementer Read TlqNodeLoginServiceTest login 测试复制改 9005）

> implementer 按现有 login test mock 拓扑写：`when(nodeRepository.findById(NODE_ID)).thenReturn(Optional.of(existingNode()))` + `when(producer.send(any)).thenReturn(SendResult.ok("MSG-9005"))`；断言 `heartbeat(NODE_ID)==true` + `verify(lifecycle, never()).login()` + `verify(lifecycle, never()).logout()`（关键：无 lifecycle 调用）+ 捕获 producer.send 的 TlqMessage 断言 payload/channel（参 login test 断言风格）。失败用例 `SendResult.fail(...)` → return false。
> **head-only 关键锚点（v0.2 评审 advisory，防 head-only 退化为含 body 假绿）**：捕获 encoder.encode 的 `CfxMessage`（参 login test `cfxCaptor`）断言 `cfx.getBodies()).hasSize(1)`（仅 RealHead9005，对比 9006 的 hasSize(2)=RealHead+Login）+ 捕获 producer.send payload `.contains("<RealHead9005>")` + `.doesNotContain("LoginRequest")`（grep login test `getBodies().hasSize(2)` / `payload.contains("<RealHead9006>")` 真实断言风格镜像）。

- [ ] **Step 2: 运行确认失败**

```bash
cd /Users/muzhou/FEP_v1.0_wt-p4-msg-o && ./mvnw test -pl fep-web -am -Dtest=TlqNodeLoginServiceTest -Dsurefire.failIfNoSpecifiedTests=false
```
期望: `heartbeat` 方法不存在编译失败。

- [ ] **Step 3: 加 heartbeat(nodeId) + build9005Message**（镜像 login，无 lifecycle）

```java
/**
 * 节点心跳：拼 head-only 9005 → encode(sign=false) → send，fire-and-forget。
 *
 * <p>区别于 {@link #login}：心跳是 keepalive，**不调 lifecycle 状态机**（不改 ONLINE/OFFLINE）。
 * 9005 head-only（无 body，仅 RealHead9005）。</p>
 *
 * @param nodeId TLQ 节点 ID，非空
 * @return {@code true} 当 9005 发送成功
 * @throws FepBusinessException 节点不存在（BIZ_5015）
 */
public boolean heartbeat(final String nodeId) {
    final TlqNode node = nodeRepository.findById(nodeId)
            .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5015,
                    "TLQ 节点不存在: " + nodeId));
    final CfxMessage cfx = build9005Message(node);
    final EncodeResult encoded = encoder.encode(cfx, defaultPipelineOpts());
    final TlqMessage msg = new TlqMessage(
            encoded.getPayload(),
            TlqMessageAttributes.forRealtime(IdGenerator.uuid20()),
            TlqChannel.REALTIME_SEND);
    final SendResult result = producer.send(msg);
    if (!result.success()) {
        LOG.warn("9005 heartbeat send failed nodeId={} error={}",
                LogSanitizer.sanitize(nodeId),
                LogSanitizer.sanitize(result.error()));
        return false;
    }
    LOG.info("9005 heartbeat sent for nodeId={} msgId={}",
            LogSanitizer.sanitize(nodeId),
            LogSanitizer.sanitize(result.msgId()));
    return true;
}

/**
 * 9005 节点心跳报文装配（head-only，结构同 9006 但无 body）。
 */
private CfxMessage build9005Message(final TlqNode node) {
    final String msgId = bodyMsgIdGenerator.generate();
    final String workDate = LocalDateTime.now().format(DATE_FMT);

    final CommonHead commonHead = new CommonHead();
    commonHead.setVersion("1.0");
    commonHead.setSrcNode(srcNode);
    commonHead.setDesNode(HNDEMP_DEST_NODE);
    commonHead.setApp("HNDEMP");
    commonHead.setMsgNo("9005");
    commonHead.setMsgId(msgId);
    commonHead.setCorrMsgId(CORR_MSG_ID_NEW_SESSION);
    commonHead.setWorkDate(workDate);

    final RealHead9005 realHead = new RealHead9005();
    realHead.setSendOrgCode(srcNode);
    realHead.setEntrustDate(workDate);
    realHead.setTransitionNo(deriveTransitionNo(msgId));

    return CfxMessage.of(commonHead, realHead);  // head-only，无 body
}
```
> import 加 `com.puchain.fep.converter.model.RealHead9005`（**不加** SuppressFBWarnings import — 本类 CRLF 由 `spotbugs-exclude.xml:1270-1273` 整类 Match 覆盖，heartbeat 自动覆盖，见 §CRLF 红线）。`node` 参数若 build9005Message 未用可改签名（grep build9006Message 实测未用 node 字段除 srcNode，build9005Message 同；若编译告警 unused 改无参或保留对齐 build9006 风格，implementer 判定）。

- [ ] **Step 4: 运行确认通过**

```bash
cd /Users/muzhou/FEP_v1.0_wt-p4-msg-o && ./mvnw test -pl fep-web -am -Dtest=TlqNodeLoginServiceTest -Dsurefire.failIfNoSpecifiedTests=false
```
期望: BUILD SUCCESS。

- [ ] **Step 5: 全模块回归（红线 `feedback_full_regression_before_commit`，含 CRLF spotbugs gate）**

```bash
cd /Users/muzhou/FEP_v1.0_wt-p4-msg-o && ./mvnw verify --batch-mode --no-transfer-progress -Dsurefire.rerunFailingTestsCount=1
```
**Strong 回归:** 全 reactor BUILD SUCCESS（特别确认 **spotbugs-check verify-phase gate 到达** + 0 CRLF bug — 验证 heartbeat log 经 `spotbugs-exclude.xml` 整类 Match 覆盖 0 CRLF，**无方法级注解**，P4-MSG-L CRLF 教训）；E2E flake rerun 兜底。

- [ ] **Step 6: Commit**

```bash
cd /Users/muzhou/FEP_v1.0_wt-p4-msg-o
git add fep-web/src/main/java/com/puchain/fep/web/tlq/node/service/TlqNodeLoginService.java
git add fep-web/src/test/java/com/puchain/fep/web/tlq/node/service/TlqNodeLoginServiceTest.java
git commit -m "$(cat <<'EOF'
feat(web): P4-MSG-O T2 — TlqNodeLoginService.heartbeat(nodeId) 9005 心跳发送

- heartbeat(nodeId): 拼 head-only 9005（CommonHead MsgNo=9005 + RealHead9005，无 body）
  → encode(sign=false) → producer.send，fire-and-forget，无 lifecycle 状态变更
- build9005Message head-only（CfxMessage.of(commonHead, realHead9005)）
- 单测：send 成功→true / 失败→false / verify lifecycle never called / head-only 装配
- CRLF：heartbeat log 参数 LogSanitizer.sanitize（CRLF 由 spotbugs-exclude.xml 整类 Match 覆盖，无需注解）

PRD: v1.3 §4.5 + §5.7.5 (FR-MSG-9005)
Note: sign=false 镜像 9006/9008（非 ⛔ 禁入）；连通性 RTT/调度接入属独立 initiative

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

## Task 3: 闭环 + CLAUDE.md/PRD matrix + push + PR + worktree `模式 A`

**验收标准:**
1. 全 reactor verify BUILD SUCCESS
2. `/Users/muzhou/FEP/docs/plans/prd-traceability-matrix.md` FR-MSG-9005 maturity 追加心跳发送能力（file write only，**禁 commit**，红线 `feedback_fep_docs_repo_commit_taboo`）
3. 分支 push + PR
4. PR merge 后 worktree remove

**Files:** Modify `/Users/muzhou/FEP/docs/plans/prd-traceability-matrix.md`（file write only，禁 commit）；CLAUDE.md outbound 进度无变化（9005 dispatcher 已 P4-MSG-N 注册，本 Plan 加发送能力非新报文）。

- [ ] **Step 1: 全 reactor 回归**

```bash
cd /Users/muzhou/FEP_v1.0_wt-p4-msg-o && ./mvnw verify --batch-mode --no-transfer-progress -Dsurefire.rerunFailingTestsCount=1 2>&1 | tail -30
```

- [ ] **Step 2: PRD matrix 更新（file write only，禁 git add）**

```
# /Users/muzhou/FEP/docs/plans/prd-traceability-matrix.md（非 git）:
#   FR-MSG-9005 maturity 追加 "+ heartbeat 主动发送能力 (P4-MSG-O — TlqNodeLoginService.heartbeat head-only 9005 send，
#   sign=false fire-and-forget；连通性 RTT/调度接入独立 initiative)"
```

- [ ] **Step 3: push + PR**

```bash
cd /Users/muzhou/FEP_v1.0_wt-p4-msg-o
git push -u origin feat/p4-msg-o-9005-heartbeat-send
gh pr create --base main --title "feat: P4-MSG-O 9005 心跳发送能力 (TlqNodeLoginService.heartbeat)" \
    --body "$(cat <<'EOF'
## Summary
- RealHead9005 心跳业务头 POJO（fep-converter，head-only 无 body）
- TlqNodeLoginService.heartbeat(nodeId)：拼 head-only 9005 → encode(sign=false) → send，fire-and-forget 无 lifecycle
- sign=false 镜像 9006/9008（非 ⛔ 安全禁入）；连通性 RTT/调度接入属独立 initiative

## Test plan
- [x] 全 reactor mvnw verify GREEN（本地，spotbugs CRLF gate 到达）
- [ ] GHA Build & Quality Gates 背书

PRD: v1.3 §4.5 + §5.7.5 (FR-MSG-9005)

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

- [ ] **Step 4: worktree 闭环（PR merge 后）**

```bash
cd /Users/muzhou/FEP_v1.0
git worktree remove /Users/muzhou/FEP_v1.0_wt-p4-msg-o
git branch -d feat/p4-msg-o-9005-heartbeat-send
```

---

## 自检清单

- [x] **1. PRD 覆盖度**: FR-MSG-9005 心跳发送能力。连通性 RTT/调度/往返关联明示范围外。
- [x] **2. 安全边界**: sign=false（无 signer 调用）；无 SM2/SM3/SM4/脱敏；RealHead9005 仅机构码/日期/流水号无 PII。
- [x] **3. 占位符扫描**: 0 TBD/TODO。
- [x] **4. 类型一致性**: RealHead9005/heartbeat/build9005Message 跨 Task 一致。
- [x] **5. 测试命令可执行**: `-Dtest=RealHead9005Test` + `-Dtest=TlqNodeLoginServiceTest`；`-Dsurefire.failIfNoSpecifiedTests=false`。
- [x] **6. CLAUDE.md**: outbound 进度无变化（9005 已 P4-MSG-N 注册）；本 Plan PRD matrix maturity 追加。
- [x] **7. 验收标准完整性**: 断言（true/false/never lifecycle/9005 head-only）可验证。
- [x] **8. 共享工具类无遗漏**: 复用 LogSanitizer（CRLF）。
- [x] **9. 核心类职责边界**: TlqNodeLoginService 加方法不增依赖（7 内）。
- [x] **10. Worktree 触发自检**: ✅ 多会话并存 + 与签字 Plan 并存 → 必须 worktree，已建 wt-p4-msg-o；T3 含 remove。

---

## 红线交叉引用

| Task | 红线 | 应对 |
|---|---|---|
| T1 | `feedback_plan_must_grep_actual_api` | RealHead9006 + AbstractRealHead grep 实测镜像 |
| T2 | **`feedback_logsanitizer_alone_insufficient_for_findsecbugs`** | heartbeat log 参数 LogSanitizer.sanitize；CRLF 由 `spotbugs-exclude.xml:1270-1273` 整类 Match 覆盖（本类既定机制，**无需方法级注解**，v0.2 评审实测纠正 v0.1 错误策略） |
| T2 | `feedback_full_regression_before_commit` | Step 5 全 reactor verify 确认 spotbugs CRLF gate 到达 |
| T2 | `feedback_dispatcher_payload_shape_blind_spot` | heartbeat 测试断言真实 9005 head-only 装配（捕获 CfxMessage/payload 非仅 verify encode called） |
| T3 | `feedback_fep_docs_repo_commit_taboo` | PRD matrix file write only 禁 commit |
| T3 | `feedback_worktree_for_parallel_work` | T3 Step 4 worktree remove |
| 全程 | `feedback_baseline_drift_during_long_review_cycle` | baseline cb5733f，签字实施跨时重测 |
| 全程 | subagent 全周期纪律 + `feedback_task_review_discipline` | implementer dispatch + 每 Task spec+quality review |

---

## 评审与签字

> ⚠️ **本 Plan 必须先通过 AI 独立评审 + muzhou 人工签字方可执行**

### AI 独立评审区
- **Round 1（santa-method）REVISE** — API 真实性全 grep 命中（RealHead9006/AbstractRealHead/login 体/build9006Message/CfxMessage.of head-only/TlqMessage/mock 拓扑）+ 安全边界 sign=false + head-only 测试断言 PASS；2 BLOCKER 均 Plan-vs-codebase 偏离：(1) CRLF 误指方法级注解（实测整类 exclude 覆盖）(2) lifecycle 论据（handleHeartbeat 存在 inbound）。
- **v0.2 boil-lake** — 2 BLOCKER 实测纠正 + head-only 测试锚点 advisory；自确认 spotbugs-exclude.xml:1270-1273 整类 Match + handleHeartbeat:32。
- **Round 2（santa-method）** — BLOCKER-2 全解；BLOCKER-1 唯 Step 5 回归验证文案残留 v0.1 "@SuppressFBWarnings 生效"措辞（删除清单漏 Step 5）→ 修正为"整类 Match 覆盖 0 CRLF 无方法级注解" → **段间一致，PASS**。

### muzhou 签字区
v0.2 路径: v0.1 grep 全 API → Round 1 REVISE(2 BLOCKER Plan-vs-codebase 偏离) → boil-lake 实测纠正 → muzhou 签字。

| 项 | 决定 | 签字日期 |
|---|---|---|
| 批准 / 驳回 / 部分修改 | **✅ 批准** | 2026-06-04 |
| 签字 | **muzhou** | 2026-06-04 |
