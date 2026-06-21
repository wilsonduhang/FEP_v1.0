# DEF-PAG-1 PaginationHelper safePageable Clamping 重载 Implementation Plan

> **For Claude:** 本 Plan 按 FEP 规范执行：每 Task 完成派独立 spec + quality review subagent（红线 `feedback_task_review_discipline`）；**执行模式 hybrid**（主对话实施 edits + 前台 `.\mvnw.cmd` + commit / review subagent 只读静态评审禁 mvn，红线 `feedback_harness_bg_detach_hybrid_default` + `feedback_review_subagent_must_not_run_mvn`）。

**Goal:** 给 `PaginationHelper` 增加做边界钳制（clamping）的 `safePageable` 重载，收敛两个手写 clamping 的 Pageable 站点（`ReconciliationQueryService` / `MessageReviewTaskService`），消除 `Math.max/Math.min` 钳制样板的重复，与既有内联写法**字节级等价、零行为变更**。

**Architecture:** 采用 muzhou 选定的「参数化上限重载」策略——`safePageable(pageNum, pageSize, sort)` 只钳下限（≥1）不设上限；`safePageable(pageNum, pageSize, maxPageSize, sort)` 额外钳上限。Reconciliation 走无上限重载，MessageReview 走带上限重载（传入其 `MAX_PAGE_SIZE`）。`DirMapConfigController` 内存 `subList` 分页**排除**（返回型不兼容 Pageable，本 Plan 不触碰）。

**Tech Stack:** Java 17 / Spring Boot 3.x / Spring Data `PageRequest`·`Pageable`·`Sort` / JUnit 5 + AssertJ。

**执行 Worktree:** `E:\FEP_v1.0_wt-def-pag-1`（分支 `refactor/def-pag-1-safepageable-clamping`，触发条件第 2 项「与已签字未执行 Plan 并存」+ 多会话高并发实测：同时存在 `wt-dzpz-record`/`wt-b9-tlq-alert`/`wt-def-q-new-1`/`wt-metrics-count-cache` 别会话活跃）。base = origin/main `a1ef884`（#119）。

**FR 追溯:** 无新增 FR（基础设施/Simplify deferred drain，CLAUDE.md「基础设施/元流程 Plan 除外」适用）。来源 = PR #119「PaginationHelper 输入侧收敛 26 站点」的 deferred 池 **DEF-PAG-1**（收敛被排除的 clamping 站点）。muzhou 2026-06-21 选定「参数化上限重载」策略。

**前置实测背书（起草时 grep origin/main `a1ef884`）:**
- `PaginationHelper`（`fep-common/.../domain/PaginationHelper.java`）现有 `pageable(int,int)` + `pageable(int,int,Sort)`，Javadoc L13 明确「不做 clamping」，本 Plan 新增 clamping 重载。
- `ReconciliationQueryService.search()` L97-99：`safePage=Math.max(1,pageNum)-1` / `safeSize=Math.max(1,pageSize)`（**无上限**）/ `PageRequest.of(safePage,safeSize,DEFAULT_SORT)`；**`safePage/safeSize` 仅用于构造 Pageable，不回显**（返回 `PagedResult(content,totalElements,totalPages)` 不带 pageNum/pageSize）。
- `MessageReviewTaskService.list()` L138-141：`safePage=Math.max(pageNum,1)` / `safeSize=Math.min(Math.max(pageSize,1),MAX_PAGE_SIZE)`（`MAX_PAGE_SIZE=200` L46）/ `PageRequest.of(safePage-1,safeSize,Sort.by(DESC,"createdAt"))`；**clamped 值复用于 L145 回显** `PageResult.from(page, safePage, safeSize, ReviewTaskResponse::from)`。
- `DirMapConfigController` L85-88：内存 `subList` 索引计算，非 Pageable → **排除**。

---

## Task 1: PaginationHelper 新增 safePageable clamping 重载（fep-common）

**Files:**
- Modify: `fep-common/src/main/java/com/puchain/fep/common/domain/PaginationHelper.java`
- Test: `fep-common/src/test/java/com/puchain/fep/common/domain/PaginationHelperTest.java`

**Step 1: 写失败测试**（追加到 `PaginationHelperTest`）

```java
@Test
void safePageableClampsPageNumAndSizeLowerBound() {
    // pageNum<1 → 1（0-based 0）；pageSize<1 → 1
    final Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
    final Pageable p = PaginationHelper.safePageable(0, 0, sort);
    assertThat(p.getPageNumber()).isZero();
    assertThat(p.getPageSize()).isEqualTo(1);
    assertThat(p.getSort()).isEqualTo(sort);
}

@Test
void safePageableNoUpperCapIsByteEquivalentToReconciliationInline() {
    // 收敛不变量：== ReconciliationQueryService 既有内联（无上限）
    final Sort sort = Sort.by(Sort.Direction.ASC, "reconciliationDate");
    assertThat(PaginationHelper.safePageable(0, 50, sort))
            .isEqualTo(PageRequest.of(Math.max(1, 0) - 1, Math.max(1, 50), sort));
    assertThat(PaginationHelper.safePageable(3, 9999, sort))   // 无上限：9999 原样
            .isEqualTo(PageRequest.of(2, 9999, sort));
}

@Test
void safePageableWithMaxClampsUpperBound() {
    final Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
    // pageSize>max → max；max 路径下下限同样钳制
    assertThat(PaginationHelper.safePageable(1, 500, 200, sort).getPageSize()).isEqualTo(200);
    assertThat(PaginationHelper.safePageable(1, 0, 200, sort).getPageSize()).isEqualTo(1);
}

@Test
void safePageableWithMaxIsByteEquivalentToMessageReviewInline() {
    // 收敛不变量：== MessageReviewTaskService 既有内联（MAX_PAGE_SIZE=200）
    final Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
    final int max = 200;
    assertThat(PaginationHelper.safePageable(2, 300, max, sort))
            .isEqualTo(PageRequest.of(Math.max(2, 1) - 1, Math.min(Math.max(300, 1), max), sort));
}

@Test
void safePageableNullSortThrowsNpe() {
    assertThatThrownBy(() -> PaginationHelper.safePageable(1, 10, null))
            .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> PaginationHelper.safePageable(1, 10, 200, null))
            .isInstanceOf(NullPointerException.class);
}

@Test
void safePageableInvalidMaxThrows() {
    final Sort sort = Sort.by("createdAt");
    assertThatThrownBy(() -> PaginationHelper.safePageable(1, 10, 0, sort))
            .isInstanceOf(IllegalArgumentException.class);
}
```

**Step 2: 运行确认失败**

Run（worktree 内，Windows）:
```powershell
$env:JAVA_HOME=(本机 JDK17 路径)
cd E:\FEP_v1.0_wt-def-pag-1
.\mvnw.cmd -pl fep-common -o test "-Dtest=PaginationHelperTest" > E:\tmp\t1-red.log 2>&1
```
Expected: FAIL（`safePageable` 方法不存在，编译错误）

**Step 3: 写最小实现**（追加到 `PaginationHelper`，置于现有 `pageable(...,Sort)` 之后）

```java
    /**
     * 构造带边界钳制的分页请求（无 pageSize 上限）。
     *
     * <p>{@code pageNum} 钳为 ≥1，{@code pageSize} 钳为 ≥1，再归一为 0-based
     * {@link Pageable}。与既有内联 {@code Math.max(1,pageNum)-1 / Math.max(1,pageSize)}
     * 写法字节级等价。需 pageSize 上限的站点用
     * {@link #safePageable(int, int, int, Sort)} 重载。</p>
     *
     * @param pageNum  当前页码（1-based；&lt;1 归一为 1）
     * @param pageSize 每页大小（&lt;1 归一为 1；无上限）
     * @param sort     排序规则（非 null）
     * @return 0-based 钳制后的 {@link Pageable}
     */
    public static Pageable safePageable(final int pageNum, final int pageSize, final Sort sort) {
        Objects.requireNonNull(sort, "sort");
        final int safePage = Math.max(1, pageNum) - 1;
        final int safeSize = Math.max(1, pageSize);
        return PageRequest.of(safePage, safeSize, sort);
    }

    /**
     * 构造带边界钳制的分页请求（含 pageSize 上限）。
     *
     * <p>{@code pageNum} 钳为 ≥1，{@code pageSize} 钳为 {@code [1, maxPageSize]}，
     * 再归一为 0-based {@link Pageable}。与既有内联
     * {@code Math.min(Math.max(pageSize,1),MAX) } 写法字节级等价，防御超大 pageSize。</p>
     *
     * @param pageNum     当前页码（1-based；&lt;1 归一为 1）
     * @param pageSize    每页大小（钳制到 {@code [1, maxPageSize]}）
     * @param maxPageSize 每页大小上限（须 ≥1）
     * @param sort        排序规则（非 null）
     * @return 0-based 钳制后的 {@link Pageable}
     * @throws IllegalArgumentException 当 {@code maxPageSize < 1}
     */
    public static Pageable safePageable(final int pageNum, final int pageSize,
                                        final int maxPageSize, final Sort sort) {
        Objects.requireNonNull(sort, "sort");
        if (maxPageSize < 1) {
            throw new IllegalArgumentException("maxPageSize must be >= 1, got " + maxPageSize);
        }
        final int safePage = Math.max(1, pageNum) - 1;
        final int safeSize = Math.min(Math.max(1, pageSize), maxPageSize);
        return PageRequest.of(safePage, safeSize, sort);
    }
```

**Step 4: 运行确认通过 + spotbugs**

```powershell
.\mvnw.cmd -pl fep-common -o test "-Dtest=PaginationHelperTest" > E:\tmp\t1-green.log 2>&1
.\mvnw.cmd -pl fep-common -o spotbugs:check > E:\tmp\t1-sb.log 2>&1   # 需先 compile（test 已编译）
```
Expected: PASS（新增 6 测试全绿）；spotbugs BugInstance 0。
> 注（红线 `feedback_spotbugs_check_needs_recompile_after_annotation`）：本 Task 无注解改动，但 spotbugs:check 仅分析现有字节码——`test` 阶段已 compile，故顺序安全。

**Step 5: Commit**

```powershell
git add fep-common/src/main/java/com/puchain/fep/common/domain/PaginationHelper.java fep-common/src/test/java/com/puchain/fep/common/domain/PaginationHelperTest.java
git commit -m @'
refactor(common): PaginationHelper 增 safePageable clamping 重载 (DEF-PAG-1)

两个重载：无上限 (pageNum/pageSize ≥1) + 带上限 (pageSize 钳 [1,max])，
与 Reconciliation/MessageReview 既有内联 Math.max/Math.min 写法字节级等价。
maxPageSize<1 防御 IllegalArgumentException。6 新单测含字节级等价不变量。

AI-Generated: claude-code
Reviewed-By: pending
'@
```
> commit 为独立命令，不与验证链式（红线 `feedback_commit_no_chain_with_verify_command`）。本 commit 自洽：fep-common 独立可编译（红线 `feedback_commit_tree_self_consistent_per_commit`）。

---

## Task 2: 收敛 ReconciliationQueryService（无上限重载）

**Files:**
- Modify: `fep-web/src/main/java/com/puchain/fep/web/reconciliation/service/ReconciliationQueryService.java:97-99`

**Step 1: 确认既有测试覆盖**（不新写，复用回归保护行为不变）

```powershell
git -C E:\FEP_v1.0_wt-def-pag-1 grep -l "ReconciliationQueryService" -- "fep-web/src/test/**" 2>&1
```
若无 search() 分页测试，**新增 1 个**断言 clamping 行为（pageNum=0/pageSize=0 不抛、返回首页）保护收敛；有则确认覆盖 clamping 路径。

**Step 2: 替换实现**

L97-99 三行替换为一行（删 `safePage`/`safeSize` 局部变量，因下游不回显）：
```java
        final Pageable pageable = PaginationHelper.safePageable(pageNum, pageSize, DEFAULT_SORT);
```
加 import `com.puchain.fep.common.domain.PaginationHelper`（若未导入）。

**Step 3: 运行确认行为不变**

```powershell
.\mvnw.cmd -pl fep-web -o test "-Dtest=ReconciliationQueryService*Test,Reconciliation*IT" > E:\tmp\t2.log 2>&1
```
> ⚠️ fep-common 上游已改（Task1），跑 fep-web 前须先装：`.\mvnw.cmd -pl fep-common -o install -DskipTests`（红线 `feedback_shared_m2_snapshot_cross_session_clobber`：`-o` 用本地 .m2，先装上游 SNAPSHOT）。
Expected: PASS（行为字节级等价，既有测试全绿）。

**Step 4: Commit**

```powershell
git add fep-web/src/main/java/com/puchain/fep/web/reconciliation/service/ReconciliationQueryService.java <新增测试若有>
git commit -m @'
refactor(web): ReconciliationQueryService 收敛至 PaginationHelper.safePageable (DEF-PAG-1)

L97-99 手写 Math.max clamping → safePageable(pageNum,pageSize,DEFAULT_SORT)。
字节级等价（无上限，下游不回显 safePage/safeSize）。

AI-Generated: claude-code
Reviewed-By: pending
'@
```
> 自洽：依赖 Task1 已 ship 的 safePageable 重载，commit 顺序 T1→T2 保证可编译。

---

## Task 3: 收敛 MessageReviewTaskService（带上限重载 + 回显反推）

**Files:**
- Modify: `fep-web/src/main/java/com/puchain/fep/web/audit/review/service/MessageReviewTaskService.java:138-145`

**Step 1: 确认既有测试**

`MessageReviewTaskService*Test`（Create/Decision/MultiLevel）回归保护；确认含 list() 分页 + pageSize 上限钳制断言，无则补 1 个（pageSize=500→200 上限）。

**Step 2: 替换实现**

L138-141 四行 → 一行（保留 `MAX_PAGE_SIZE` 常量，作为该 service 的领域策略传入 helper）：
```java
        final Pageable pageable = PaginationHelper.safePageable(
                pageNum, pageSize, MAX_PAGE_SIZE, Sort.by(Sort.Direction.DESC, "createdAt"));
```
L145 回显从 clamped Pageable 反推（原 `safePage`/`safeSize` 局部已删）：
```java
        return PageResult.from(page, pageable.getPageNumber() + 1, pageable.getPageSize(),
                ReviewTaskResponse::from);
```
加 import `PaginationHelper`（若未导入）。`PageRequest` import 若变为未使用则删（避免 checkstyle UnusedImports）。

**Step 3: 运行确认行为不变**

```powershell
.\mvnw.cmd -pl fep-common -o install -DskipTests > E:\tmp\t3-install.log 2>&1
.\mvnw.cmd -pl fep-web -o test "-Dtest=MessageReviewTaskService*Test" > E:\tmp\t3.log 2>&1
```
Expected: PASS。回显值字节级等同（`getPageNumber()+1 == safePage`，`getPageSize() == safeSize`）。

**Step 4: Commit**

```powershell
git add fep-web/src/main/java/com/puchain/fep/web/audit/review/service/MessageReviewTaskService.java <新增测试若有>
git commit -m @'
refactor(web): MessageReviewTaskService 收敛至 PaginationHelper.safePageable (DEF-PAG-1)

L138-141 手写 Math.min/Math.max clamping → safePageable(pageNum,pageSize,MAX_PAGE_SIZE,sort)。
回显改从 pageable.getPageNumber()+1 / getPageSize() 反推（字节级等值）。

AI-Generated: claude-code
Reviewed-By: pending
'@
```

---

## Task 4: 全量回归 + worktree closing

**Step 1: 回归（两层，红线 `feedback_plan_regression_scope_explicit`）**

- **Minimum（本地）**：fep-common 上游已改 → `-am` 正当（红线 `feedback_single_module_regression_no_am_flag`：上游源本次也改才用 `-am`）：
  ```powershell
  .\mvnw.cmd -pl fep-web -am -o test > E:\tmp\t4-web.log 2>&1     # fep-common+fep-web 测试链
  .\mvnw.cmd -pl fep-common,fep-web -o spotbugs:check > E:\tmp\t4-sb.log 2>&1
  ```
  ⚠️ 多会话高并发：跑前 `uptime`/`Get-Counter` 看负载；load>100 等别会话 verify 结束再跑（红线 load-197 事件）；长跑 redirect to file 禁 |tail。
- **Strong（GHA）**：PR 触发 Build/Test/Quality（whole reactor）+ SonarCloud 全绿背书。

**Step 2: PR + worktree teardown**（muzhou 授权 merge 后）

```powershell
# PR 由 muzhou 决策 merge（不可逆）
git -C E:\FEP_v1.0 worktree remove E:\FEP_v1.0_wt-def-pag-1
git -C E:\FEP_v1.0 branch -d refactor/def-pag-1-safepageable-clamping   # merge 后
```

---

## 范围与不做（YAGNI）

- **不加无 sort 重载**：两站点都带 sort，无 sort 的 clamping 站点 0 个 → 不加 `safePageable(int,int)` / `safePageable(int,int,int)`。
- **不收敛 DirMapConfigController**：内存 `subList` 非 Pageable，返回型不兼容；如需统一另起 `clampedRange` helper（本 Plan 外）。
- **零行为变更**：两站点收敛后输出与既有内联**字节级等价**，非语义调整。MessageReview 仍 MAX_PAGE_SIZE=200，Reconciliation 仍无上限。

## 评审与签字门

1. ✅ santa-method Plan 评审（2026-06-21 **PASS-WITH-MINOR**，无 BLOCKER；字节级等价不变量逐站点真实代码核验成立 + 回显反推等值确认 + import 区分正确；2 MINOR 无需改动）
2. ✅ **muzhou 签字（2026-06-21 AskUserQuestion 「签字 + 实施」）**
3. ⏳ 实施（每 Task spec + quality review subagent，hybrid 模式）
4. merge = muzhou 不可逆决策
