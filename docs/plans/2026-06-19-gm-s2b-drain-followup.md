# GM S2b Deferred 池 Drain Follow-up（DEF-DRAIN-1 + DEF-DRAIN-2）Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: 实施用 superpowers:executing-plans 逐 Task 推进；Task 完成派独立 spec + quality review subagent（Explore 只读，显式禁 mvn — 红线 `feedback_task_review_discipline` + `feedback_review_subagent_must_not_run_mvn`）。

**Goal:** 清空 GM S2b deferred 池 drain（PR #107）后残留的两项低风险尾项，纯 **test + Javadoc**，**零生产逻辑 / 零运行期行为变更**：
- **DEF-DRAIN-1**：`PeerVerifyKeyMapsTest` 补 `decodedCopy` 的**内层 `List<byte[]>` 不可变断言**——当前 `immutableHexCopy` 侧已有内层 List 不可变断言（test 既有），但 `decodedCopy`（`.toList()`）侧仅断言 size/内容，未断言内层不可变。补齐对称覆盖。
- **DEF-DRAIN-2**：`PeerVerifyKeyMaps` Javadoc 澄清——① **null 值键保留**语义（key 保留、value→空列表，非丢键）② 类级 ADR 文本引用（形态决策 ADR 2026-06-12 §0.3 C-演进式）。

**Architecture:** 不动 `copy` 骨架、`immutableHexCopy` / `decodedCopy` 任何逻辑。DEF-DRAIN-1 仅在测试类**新增一个 `@Test`** 锁定 `.toList()` 既有不可变契约（Java 16+ `Stream.toList()` 返回 unmodifiable list）。DEF-DRAIN-2 仅改 `PeerVerifyKeyMaps.java` 的 Javadoc 文本。

**Tech Stack:** Java 17 / Spring Boot 3.x / Maven 多模块 / JUnit 5 + AssertJ。无新依赖、无新原语。

**执行 Worktree:** `E:\FEP_v1.0_wt-def-drain`（分支 `chore/gm-s2b-drain-followup`，off `origin/main` `69b0f80`；触发条件第 3 项「security/impl 区域」+ 第 6 项「共享工作树多会话」/ MEMORY `shared-working-tree-needs-worktree`）

**PRD 追溯:** 元流程 / Simplify 技术债 drain，FR-ID 豁免（基础设施/元流程 Plan 除外条款）。来源 = GM S2b drain（PR #107 `3d6ed43`）Simplify 三审 deferred 池 DEF-DRAIN-1/2。承接 PRD §3.3 报文签验链既有实现，不新增需求面。DEF-DRAIN-3/4（`setSm4Keys` 同形 / 构造期分配）经评估**无收益保留**，不在本轮。

**形态/安全:** 触及 `security/impl/` 但**仅 Javadoc + 新增测试**——`copy`/`immutableHexCopy`/`decodedCopy` 生产逻辑逐字节不动，**零算法 / 零密钥语义 / 零密码学原语变更**。评审网：santa 双审 + **密码学专项轻量确认**（仅核「确无算法/密钥/解析路径漂移」，因本轮不触生产逻辑）+ muzhou 签字。真实密钥永不入 repo（测试沿用既有 GB/T 公开曲线点向量 `PUB`）。

**回归验收（两层，红线 `feedback_plan_regression_scope_explicit`）:**
- **Minimum（本地，Task 内）:** `./mvnw -pl fep-security-impl -o test`（红线 `feedback_single_module_regression_no_am_flag` — 不带 `-am`；worktree 首次上游 SNAPSHOT 缺则先一次性 `-am install -DskipTests`）。`spotbugs:check`（须先 compile，红线 `feedback_spotbugs_check_needs_recompile_after_annotation` — 本轮无注解改动但仍按规程 recompile 后 check）。
- **Strong（GHA）:** PR 触发 Build/Test & Quality 全 reactor + SonarCloud，作为权威背书。

**构建注意（红线群）:**
- 共享 `~/.m2` 跨会话 clobber：离线（`-o`）构建遇「找不到符号/无 bean」**先证伪 baseline/.m2 漂移**（`git fetch` + `rev-parse HEAD origin/main`）再疑代码（`feedback_shared_m2_snapshot_cross_session_clobber`）。
- PowerShell：cwd 每命令重置回 `E:\FEP`，跑 `mvnw.cmd` 须先 `Set-Location E:\FEP_v1.0_wt-def-drain`；`-D` 点号参数须单引号；长跑 mvn `*> file.log 2>&1` 禁 `|tail`（`feedback_pipe_tail_deadlock_with_bg_bash`）。
- 多会话并发：跑前 `uptime`/load 自查，load>100 杀**自己 worktree-slug 匹配**的 fork，不碰别会话。

**自洽性（红线 `feedback_commit_tree_self_consistent_per_commit`）:** 单 commit（test + Javadoc 同文件域、无签名变更、独立可编译），无跨 Task 切点问题。

---

## Task 1: DEF-DRAIN-1（decodedCopy 内层不可变断言）+ DEF-DRAIN-2（Javadoc 澄清）

**为什么合并:** 两项均在 `key/PeerVerifyKeyMaps.java`(+ 其 test) 同一文件域、均零生产逻辑、各 ≤6 行；拆 Task 反增 review 开销。单 commit 自洽。

**Files:**
- Modify: `fep-security-impl/src/test/java/com/puchain/fep/security/impl/key/PeerVerifyKeyMapsTest.java`（新增 1 `@Test`：`decodedCopy_innerListIsUnmodifiable`）
- Modify: `fep-security-impl/src/main/java/com/puchain/fep/security/impl/key/PeerVerifyKeyMaps.java`（仅 Javadoc）

**Step 1: DEF-DRAIN-1 — 新增 `decodedCopy` 内层不可变测试**

```java
@Test
void decodedCopy_innerListIsUnmodifiable() {
    final Map<String, List<byte[]>> copy =
            PeerVerifyKeyMaps.decodedCopy(Map.of("N1", List.of(PUB)));
    assertThatThrownBy(() -> copy.get("N1").add(new byte[] {0x01}))
            .isInstanceOf(UnsupportedOperationException.class);
}
```

> **TDD 注记（诚实）:** 该断言锁定 `Stream.toList()`（Java 16+）**既有**不可变契约——首跑即 **GREEN**，非 RED→GREEN（无生产改动，纯覆盖补缺，对称于既有 `immutableHexCopy_isDeepAndUnmodifiable` 的内层断言）。这是 characterization test，价值在防未来把 `.toList()` 改成可变实现时回归无声漂移。实施时**先单跑确认 GREEN**（`'-Dtest=PeerVerifyKeyMapsTest'`），佐证 `.toList()` 契约属实。

**Step 2: DEF-DRAIN-2 — Javadoc 澄清（仅文档）**

1. **类级 Javadoc**（单一来源，两 public 方法均经 `copy` 骨架，避免 `@param` 双写漂移）增一句澄清：**null 值列表的键被保留**（key 不丢，value 归一为空列表 `List.of()`），非「丢弃该 srcNode 条目」。锚现有行为 `result.put(srcNode, ... hexes == null ? List.of() : hexes)`（main line 62）。两 public 方法既有 `@param source` 文案保持不变（已含「可含 null 列表值」），不重复。
2. 类级 Javadoc 增**文本** ADR 引用（非字面 `{@link}` — 指向 `/FEP/docs/decisions` 非 Java 程序元素，`{@link}` 会触 javadoc/Checkstyle lint 失败）：用 `@see` 文本或 `<p>` 段，如：
   `<p>报文签验落地形态见 ADR 2026-06-12「§0.3 决策门 — SM2 报文签验落地形态」（形态 C-演进式）。</p>`

> **Checkstyle 注意:** 仅改 Javadoc 文本，不新增 public 方法/类 → 不触发 missing-Javadoc；`PeerVerifyKeyMaps.java` 现 66 行，远低于 FileLength≤400，安全。

**Step 3: 回归 + 自检**
- `Set-Location E:\FEP_v1.0_wt-def-drain; .\mvnw.cmd -pl fep-security-impl -o test '-Dtest=PeerVerifyKeyMapsTest' *> E:\tmp\drain-t1.log 2>&1`（worktree 首次先 `-am install -DskipTests`）→ 读 log 确认 GREEN。
- 全模块 `-pl fep-security-impl -o test` 确认无回归（既有 93 tests + 新 1 = 94）。
- `spotbugs:check`（compile 后）→ BugInstance 0（预期：无生产改动不引入新 finding）。
- 9 项清单自检：仅 test + Javadoc，逐项 N/A 或 PASS。

**Step 4: Commit**（独立命令，红线 `feedback_commit_no_chain_with_verify_command`；先单独 `cat` log 确认 GREEN，再单独 commit）
- message: `test(security): DEF-DRAIN-1/2 — decodedCopy 内层不可变断言 + PeerVerifyKeyMaps Javadoc 澄清`
- footer: `AI-Generated: claude-code` + `Reviewed-By: pending`（注：纯 test+Javadoc 非新密码学逻辑，沿用 AI-Generated 标注）

**Review:** Task 完成派 spec review + quality review（Explore 只读，禁 mvn）；密码学专项轻量确认「零算法/密钥/解析漂移」。

---

## Task 2（Closing）: 收尾

- PR 开 → GHA Build/Test & Quality + SonarCloud 全绿背书（Strong 层权威）。
- merge（muzhou 授权 squash）→ read-verify MERGED（红线 `feedback_gh_mutation_network_error_verify_before_retry`，不盲 retry）→ main ff。
- **worktree teardown:** `git -C E:\FEP_v1.0 worktree remove E:\FEP_v1.0_wt-def-drain` + 分支 local+remote 删（merge 后）。
- 四步收尾 / 技术文档 / Daily Report 归 session-end 统一做（红线 `feedback_four_step_closing`）。

---

## 签字

- Plan 作者: Claude Code
- santa 评审: 待派
- 密码学专项: 待派（轻量 — 零生产逻辑）
- **muzhou 批准签字: 待签**（一票否决）
