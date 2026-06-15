# EFF-P3-1 — RuleContext.values() 零拷贝不可修改视图

> 类型: Simplify efficiency deferred 池 drain（standalone micro-Plan）
> 开发模式: A（AI 主导，≤2 LOC 纯效率优化）
> 起草日期: 2026-06-14（追溯性文档：实现已于本会话完成，本 Plan 作为追溯记录补全，§8 签字门如实标注）
> 票据来源: `docs/daily_reports/2026-06-14-rule-engine-phase3-next-session-prompt.md` §建议下一步 #2（PR #90 rule-phase3 Simplify 三审 efficiency 入池）
> PRD 追溯: 无（efficiency 重构，无新 FR；基础设施/元流程 Plan 免 FR-ID）
> **执行 Worktree:** `main` 上的 feature 分支 `chore/eff-p3-1-rulecontext-unmodifiable-view`（本机 Windows、单模块 fep-processor、≤2 LOC 改动，worktree 6 触发条件均不命中；活跃别会话 wt-gm-s2b 等在 macOS 独立物理机，与本机无 fs/index 争用，仅共享远端）

## 1. 背景与目标

PR #90（规则引擎 Phase 3）Simplify 三审 efficiency agent 标记 `RuleContext.values()` 与已落地的 **F1** 优化（`MessageRuleRegistry.rulesFor`）同型遗漏：

- **F1 已落地**（`MessageRuleRegistry.java:37-41`，merged main）：
  ```java
  public List<ValidationRule> rulesFor(final MessageType type) {
      // 注册表 write-once-at-startup / read-only-at-runtime，返回零拷贝
      // 不可修改视图而非 List.copyOf，省去每报文一次数组克隆（Simplify efficiency F1）。
      return Collections.unmodifiableList(rules.getOrDefault(type, List.of()));
  }
  ```
- **EFF-P3-1 目标**（`RuleContext.java` `values()`，优化前）：
  ```java
  public List<String> values(final String localName) {
      return List.copyOf(fields.getOrDefault(localName, List.of()));
  }
  ```

`values()` 每次调用 `List.copyOf` 对内部字段值列表做一次防御性数组克隆。`RuleContext` 是 `final` 类 + 全 `final` 字段、构造后无任何 mutator（不可变对象），其 `fields` 在 `parse()` 内一次性构建后**永不再变**——与注册表「构建后只读」不变量完全一致，故防御拷贝是多余的。

**目标**：将 `List.copyOf` 改为 `Collections.unmodifiableList`，省去热路径（每报文 × 规则数 × 字段值列表长度）每次一回的数组克隆，**行为保持**。

## 2. 语义安全分析（copyOf vs unmodifiableList — 行为等价证明）

| 维度 | `List.copyOf(src)`（优化前） | `Collections.unmodifiableList(src)`（优化后） | 本场景是否等价 |
|------|--------------------------|--------------------------------------------|----------------|
| 可修改性 | 不可变 | 不可修改视图（mutator 抛 `UnsupportedOperationException`） | ✅ 调用方契约「不可修改」保持 |
| 底层 list 后续被改 | 快照，不反映 | 活视图，会反映 | ✅ **moot**：`fields` 及内层 `ArrayList` 构造后永不 mutate（不可变对象，无 mutator 路径） |
| null 元素 | 抛 NPE | 允许 | ✅ **moot**：内层 list 仅装 `text.trim()`（被 `text != null && !text.isBlank()` 守护，恒非 null） |
| 空字段（absent） | `List.of()`→`List.of()` | `unmodifiableList(List.of())` | ✅ 两者皆空且不可修改 |
| 每次调用对象身份 | 新快照 | 新 wrapper（同一 backing） | ✅ 无调用方依赖 `==` 身份；backing 不变 → 观测等价 |

**结论**：本场景下 `RuleContext` 的「构造后只读」不变量使两者**行为等价**，与 F1 同一论证。spec + quality 双 AI review 均逐路径核验「构造后无 mutate 路径」成立（见 §8）。

## 3. 范围

**In scope**：
- `RuleContext.java` `values()` 单行 return 改写 + 同步行内注释（镜像 F1 注释风格，说明「不可变对象 → 零拷贝视图」依据）+ `import java.util.Collections;`。

**Out of scope（明示 deferred，防 scope creep）**：
- **EFF-P3-2**（`GroupCooccurrenceRule` 违规分支双 stream 遍历）——进度报告标「规模极小仅违规分支，收益微」，本 Plan **不纳入**，留 deferred 池。
- `RuleContext` 其它方法（`first`/`has`/`hasElement*`）——`first` 不返回集合无拷贝问题；`Set.copyOf`（`parse()` 内 `presentElements`/`headElements`）是构造期一次性快照非热路径，不动。

## 4. TDD / 测试守护

**净新增 1 条契约守护**：`RuleContextTest.values_returnedListShouldBeUnmodifiable` —— 断言 present（`"Amt"`）+ absent（`"Missing"`）两路返回均 `.add` 抛 `UnsupportedOperationException`，锁定本次唯一变更点（copyOf→视图）的「不可修改」契约。该测试在优化前（`List.copyOf`）已 GREEN（characterization 基线），优化后（`unmodifiableList`）仍 GREEN（行为保持守护）。

**内容/顺序回归（复用既有，不新增）**：既有 `values_shouldReturnAllRepeatedElementsInDocumentOrder` + `values_shouldMergeSameLocalNameAcrossNestingLevels` 已断言 `containsExactly`，覆盖「零拷贝视图内容/顺序与优化前一致」。实施期评估认为另起 `values_unmodifiableView_shouldStillReflect...` 与之**完全重叠**，故**未新增**（避免冗余测试）。

> 注：本项是 behavior-preserving refactor，非 RED→GREEN 新功能；测试角色为「改前 GREEN（契约已立）→ 改后仍 GREEN（保持）」回归守护。实施顺序：先跑 `RuleContextTest` 确认基线 GREEN（实测 13 tests 0 fail），再改实现，再整模块复跑确认不变。

## 5. 回归命令（Windows 本机，实测记录）

```powershell
# 首次：上游 SNAPSHOT 缺失 → 一次性装 ~/.m2（红线 single_module_regression_no_am_flag）
.\mvnw.cmd -pl fep-processor -am install -DskipTests   # 实测 BUILD SUCCESS
# 聚焦回归（离线，不带 -am）
.\mvnw.cmd -pl fep-processor -o test -Dtest=RuleContextTest   # 实测基线 13 tests 0 fail
# 整模块门禁（test + spotbugs:check + ArchUnit）
.\mvnw.cmd -pl fep-processor -o verify   # 实测 Tests run 636, Failures 0, Errors 0; BugInstance 0; BUILD SUCCESS
```
全量回归委托 GHA（PR 触发 Build/Test/Quality + SonarCloud）。`load>100` 禁本机并发全量。

## 6. Tasks

### T1 — EFF-P3-1 实现 + 测试守护（已完成，commit `f14fc81`）
1. ✅ 跑既有 `RuleContextTest` 确认基线 GREEN（13 tests 0 fail）。
2. ✅ 改 `RuleContext.java` `values()`：`List.copyOf(...)` → `Collections.unmodifiableList(...)`；加 `import java.util.Collections;`；行内注释镜像 F1 依据。
3. ✅ 净新增 `values_returnedListShouldBeUnmodifiable`（present+absent 双路；未加冗余的 content/order 重复测试，见 §4）。
4. ✅ 整模块 `verify` 全绿：Tests run 636, Failures 0, Errors 0；SpotBugs BugInstance 0；ArchUnit PASS。
5. ✅ Commit `f14fc81`：`perf(processor): EFF-P3-1 RuleContext.values() 零拷贝不可修改视图（镜像 F1）` + `AI-Generated: claude-code` + `Reviewed-By: pending`。

## 7. 闭环（session-end 统一做）
- 本 Plan：1 代码 commit（≤2 LOC production + 1 test）+ 1 docs commit（本 Plan）；无独立 worktree（feature 分支，merge 后删本地+远端）。
- session-end Phase 2：Simplify 三审（预期 0 Applied，本身即 efficiency drain）+ 技术文档（efficiency 类，简）+ Daily Report + push。
- PR + GHA 三检为唯一外部背书（红线诚信铁律：本机测试绿不背书 commit 树）。

## 8. 评审 / 签字门（如实状态）
- [x] **AI 双 review PASS（代行 santa-method）** —— spec review PASS（1 可选 MINOR：未来可加守护防 `fields` 后置 mutate，当前不变量牢固，不阻塞，未采纳）；quality review PASS（0 BLOCKER/MINOR，确认 SpotBugs EI_EXPOSE 真净、线程安全、null 路径与旧实现等价）。
- [x] **muzhou 选定执行**（2026-06-14 AskUserQuestion 选定 EFF-P3-1 为下一步 + 选定「开 PR + 保留 Plan doc」）
- [ ] **muzhou 最终签字** —— 于 PR merge 时（GHA Build/Test/Quality + SonarCloud 三检绿后）
- [ ] 实施 → PR → GHA 三检绿 → merge（进行中）
