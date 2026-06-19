# GM S2b Deferred 池 Drain — DEF-DRAIN-1 + DEF-DRAIN-2

> 类型: Simplify deferred-pool drain（quality 打磨；零运行期行为变更）
> 日期: 2026-06-19
> 上游: GM S2b deferred drain（PR #107 `3d6ed43`）§4 新生 deferred 池
> 性质: test-only（T1）+ doc-only（T2）；**无生产逻辑/算法/参数/向量改动**
> 状态: **SIGNED**（muzhou 2026-06-19 签字 — execute as written，含密码学专项 review）；独立 Plan 评审 PASS（7/7 claim 实测核验）

**执行 Worktree:** `E:\FEP_v1.0_wt-gm-s2b-def12`（分支 `chore/gm-s2b-drain-def12`，触发条件第 2 项——与多个别会话 worktree 并存 + 共享单一工作树）

## 0. 背景与范围

承接 `docs/plans/2026-06-17-gm-s2b-deferred-drain.md` 的 Daily Report §4「Simplify 三审本轮新生 deferred」。该轮清空了 REUSE-1/2/3 + EFF-1/5 + QUAL-1~6，剩 4 项 polish 入池。本 Plan 清空其中 **2 项 worth-doing**：

| Ticket | 类别 | 处置 |
|--------|------|------|
| DEF-DRAIN-1 | quality (test) | ✅ 本 Plan T1 |
| DEF-DRAIN-2 | quality (doc) | ✅ 本 Plan T2 |
| DEF-DRAIN-3 | reuse | ❌ **保留**（DRY 阈值不达，Effective Java §67 可接受重复——前轮已判定 won't-do，不反转） |
| DEF-DRAIN-4 | efficiency | ❌ **保留**（构造期冷路径无 ROI + Spring relaxed binding 需可变引用——前轮已判定 won't-do，不反转） |

范围外（blocked / 别会话 worktree 占用，本 Plan 不触碰）：DEF-B2-1/2（甲方 ResultCode 语义）、DEF-3（B-9 TLQ 告警源）、B-8 审核工作流 Phase2 特性、`PageResult`/`forward-record-tables`。

PRD lineage：PRD §3.3 报文 SM2 签验（`MessageSignPort` 形态 C-ev）。本 Plan 为 quality drain，**无新 FR**（meta/质量 Plan）。

## 1. 文件清单

| 文件 | 模块 | 改动 |
|------|------|------|
| `fep-security-impl/.../impl/key/PeerVerifyKeyMapsTest.java` | fep-security-impl (test) | T1：`decodedCopy_returnedMapIsUnmodifiable` 补内层 `List<byte[]>` 不可变断言 |
| `fep-security-impl/.../impl/key/PeerVerifyKeyMaps.java` | fep-security-impl (main) | T2a：`immutableHexCopy`/`decodedCopy` Javadoc 补「源 null 列表值 → 空列表，SrcNode 键保留」 |
| `fep-security-impl/.../impl/sign/BcMessageSignPort.java` | fep-security-impl (main) | T2b：`{@code GmSecurityConfiguration}` → `{@link ...GmSecurityConfiguration}`（同模块可解析）+ ADR 文档路径 `{@code}` 引用 |

## 2. Task

### T1 — DEF-DRAIN-1：内层 List 不可变断言（test-only）

`decodedCopy` 经 `.toList()` 返回不可变内层 `List<byte[]>`，但 `decodedCopy_returnedMapIsUnmodifiable` 仅断言外层 Map 不可变，未显式断言内层 List。补一行 `assertThatThrownBy(() -> copy.get("N1").add(...)).isInstanceOf(UnsupportedOperationException.class)` 锁定该不变量（防未来 `.toList()` 被改为可变实现而无回归信号）。

- **TDD 说明（诚信）**：此为**断言缺口补齐**，非 bug 修复——新断言对现实现**立即 GREEN**（`.toList()` 已保证）。无 RED 阶段，因无生产缺陷。价值=锁定不变量防回归（若未来内层改可变，此断言变 RED）。报告如实记录「无 RED」。
- 验证：`fep-security-impl` 模块 `PeerVerifyKeyMapsTest` 全绿（7 + 本断言仍 7 个 test 方法，断言内含）。
- spec + quality review（read-only subagent，禁 mvn）。

### T2 — DEF-DRAIN-2：Javadoc 澄清（doc-only）

- **T2a** `PeerVerifyKeyMaps`：`immutableHexCopy` 与 `decodedCopy` 的 Javadoc 在「null 列表 → 空列表」基础上补「SrcNode 键保留」语义（私有 `copy` 的 `forEach` 对 null 值映射为 `List.of()` 但 key 仍 put）。
- **T2b** `BcMessageSignPort` 类级 Javadoc：
  - 将既有 `{@code GmSecurityConfiguration}`（line 22）升级为 `{@link com.puchain.fep.security.impl.GmSecurityConfiguration}`（同模块 fep-security-impl，编译期可解析，镜像既有 `{@link KeyServiceImpl}` 用法）。
  - 将「ADR 2026-06-12」补为可定位的文档路径 `{@code docs/decisions/2026-06-12-s2b-sm2-message-signing-form-decision-gate.md}`（markdown 非 Java 元素，用 `{@code}` 而非 `{@link}` 避免 Javadoc 失配）。
- **FileLength 核查**：`BcMessageSignPort` 当前 89 行，远低于 Checkstyle FileLength≤400 硬墙，加 2 处 Javadoc 无撞墙风险（区别于前轮 `InboundMessageDispatcher` 399 行 case）。
- 验证：`checkstyle:check` clean（Javadoc 格式）。
- spec + quality review（read-only subagent，禁 mvn）。

## 3. 评审网

- 本 Plan：1 独立 read-only Plan 评审（santa-style）+ muzhou 签字。
- 每 Task：spec + quality review（read-only subagent，禁 mvn——红线 `review_subagent_must_not_run_mvn`）。
- **密码学专项 review**：read-only 确认零算法/参数/向量/密钥泄漏面改动（T1 断言不可变性、T2 纯 Javadoc）→ 预期 trivial CRYPTO-PASS（仍走流程，国密模块）。
- final whole-impl review：逐 commit 自洽。

## 4. 回归验收（分两层，红线 `plan_regression_scope_explicit`）

- **minimum（本地）**：`Set-Location E:\FEP_v1.0_wt-gm-s2b-def12; .\mvnw.cmd -pl fep-security-impl -o verify`（含 test + checkstyle + spotbugs）。首次若上游 SNAPSHOT 缺则先 `-am install -DskipTests` 装上游再单模块 `-o`。load>100 则杀本 worktree-slug fork 等 load<30（红线 `single_module_regression_no_am_flag` / fork 限流）。本地全量 verify 不可靠时先证伪 .m2/baseline 漂移。
- **strong（权威）**：GHA Build/Test & Quality + SonarCloud（PR 触发）。**国密模块 @SpringBootTest / 装配 IT 以 GHA 为准**。

## 5. Commit 切点（红线 `commit_tree_self_consistent_per_commit`）

逐 commit 独立可编译：
1. `docs(plans)`: 本 Plan（签字后）。
2. T1 test-only：`PeerVerifyKeyMapsTest` 断言。
3. T2 doc-only：`PeerVerifyKeyMaps` + `BcMessageSignPort` Javadoc。

每 commit 含 `AI-Generated: claude-code` + `Reviewed-By: <姓名>`。提交独立命令，禁与验证命令链式（红线 `commit_no_chain_with_verify_command`）；PowerShell 多 `-m` 旗标禁 here-string。

## 6. 闭环（session-end）

- `git worktree remove E:\FEP_v1.0_wt-gm-s2b-def12` + 删本地/远端分支（红线 `worktree_for_parallel_work`）。
- Simplify 三审（drain 本身即清理，预期 0~少量 applied）。
- 9 维技术文档（有 code commit → 不豁免，红线 `infra_plan_still_needs_full_8dim_docs`；但 test+doc-only 体量极小，按实记录）。
- Daily Report（含 §教训）+ git push。
- 更新 GM S2b deferred 池状态：DEF-DRAIN-1/2 → CLOSED；DEF-DRAIN-3/4 → 保留（won't-do 记录）。

## 7. 验收标准（DoD）

- [ ] DEF-DRAIN-1 断言合入，`PeerVerifyKeyMapsTest` 全绿。
- [ ] DEF-DRAIN-2 Javadoc 合入，checkstyle clean。
- [ ] 零生产逻辑/算法改动（密码学 review PASS）。
- [ ] GHA Build/Test & Quality + SonarCloud GREEN。
- [ ] muzhou squash merge + worktree teardown + 分支删。
