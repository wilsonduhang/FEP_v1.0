# PageResult.from 泛型工厂 + 行为保持迁移 实施 Plan

> **For Claude:** 本 Plan 为 **Simplify deferred drain**（reuse 类，纯 refactor 零行为变更）。来源 = §5.8 多级审核 Phase2（PR #109 `c0bbc64`）Simplify 三审 deferred 池「PaginationHelper / TimeProvider / 泛型 `PageResult.from`（跨模块）」。执行经 santa 评审 + muzhou 签字 后，hybrid 实施（主对话 edits+mvn+commit / 只读 Explore 评审，禁 mvn）。

**Goal:** 在 `fep-common` 的 `PageResult` 上新增泛型静态工厂 `from(Page, pageNum, pageSize, mapper)`，把 fep-web 中 `Page<Entity> → .stream().map(...).toList() → new PageResult<>(...)` 重复样板收敛为单次调用；**逐字节行为保持**（reported pageNum/pageSize/total/totalPages 不变），由既有端点测试做回归背书。

**Architecture:** `PageResult.from` 封装「`page.getContent()` 映射 + `page.getTotalElements()` 取 total + 透传调用方 1-based pageNum/pageSize」。**关键不变量**：reported pageNum/pageSize 仍取调用方传入值（**不**取 Spring `Page.getNumber()`，避免 0-based↔1-based 偏移——红线 `feedback_pagination_adapter`）。仅迁移真正持有 Spring `Page<E>` 的构造点；自定义分页（`ReconciliationController` 的 `PagedResult`、`DirMapConfigController` 的内存 `subList`）**排除**，不强行套用。

**Tech Stack:** Java 17 / Spring Boot 3.x；`org.springframework.data.domain.Page`（新增 `spring-data-commons` 到 fep-common，轻量 core jar，fep-common 已耦合 `spring-boot-starter-web`）；JUnit 5 + AssertJ。

**执行 Worktree:** `E:\FEP_v1.0_wt-pageresult-from`（分支 `refactor/pageresult-from`，触发条件第 1 项「跨 ≥? 模块 refactor」实为 2 模块但 26+ 文件 + 第 6 项「多会话高频并发」+ 已签字未执行 Plan 并存）。

**开发模式:** Mode A（AI 主导，纯机械 refactor + 既有测试回归）。

**FR-ID:** 无直接 FR（基础设施/质量 refactor）。追溯 = §5.8 review Phase2 Simplify deferred drain；间接服务 PRD §9.2 列表与分页规范（`PageResult` 既有归属）。

---

## 决策点（santa + muzhou 签字前定）

| # | 决策 | 选项 | 推荐 |
|---|------|------|------|
| D1 | 工厂归属 | (A) `PageResult.from` 在 fep-common + 加 `spring-data-commons` 依赖 / (B) fep-web `PaginationSupport` util 保 fep-common 无 spring-data | **(A)** — 对齐 deferred 票「PageResult.from」；fep-common 已依赖 `spring-boot-starter-web`（非纯 POJO 模块），加 `spring-data-commons`（core jar）一致，API 最简洁 |
| D2 | 签名 | `from(Page<E>, int pageNum, int pageSize, Function<? super E,? extends T> mapper)` | 透传 pageNum/pageSize（保 1-based 不变量）；返回 `PageResult<T>` |
| D3 | 排除站点 | `ReconciliationController:170`（custom `PagedResult`）/ `DirMapConfigController:88`（内存 subList）2 处不迁移 | 二者无 Spring `Page` 对象，套用即破坏；保持现状 |
| D4 | TimeProvider / PaginationHelper(请求→PageRequest) | 本 Plan **不含**（另一关注点 = 时钟抽象 + PageRequest 构建） | 留 deferred；本 Plan 仅收敛「Page→PageResult」出参侧样板，避免无界扩张 |
| D5 | Page-of-DTO 站点（`TlqConnectivityController:91`，已是 `Page<DTO>`）| 用 identity mapper `Function.identity()` 迁移，或保留不动 | 用 `from(page, pageNum, pageSize, Function.identity())` 统一（亦可加 `from(Page,pageNum,pageSize)` 重载） |

> **ArchUnit 前置核实（实施 Task 1 前 grep）**：确认无规则禁 fep-common 依赖 spring-data；若有则回落 D1-(B)。

---

## 站点清单（grep 实测 `new PageResult<>` = 29 处 / 26 文件 @ `c0bbc64`）

**迁移组（Spring `Page`，behavior-preserving）：**

| 组 | 文件 : 行 |
|---|---|
| G1 audit/collector/dashboard | `audit/review/service/MessageReviewTaskService:136`、`collector/service/CollectionRunQueryService:103`、`dashboard/todo/service/DashboardTodoService:100` |
| G2 entquery | `entquery/auth/service/EntAuthLetterService:214`、`entquery/task/service/EntQueryTaskService:186` |
| G3 bizdata | `bizdata/record/service/BizMessageRecordService:155`、`bizdata/definition/service/BizMessageDefinitionService:77` |
| G4 tlq | `tlq/node/service/TlqNodeService:128`、`tlq/connectivity/controller/TlqConnectivityController:91`（D5 Page-of-DTO） |
| G5 submission | `submission/scene/service/SubBusinessSceneService:73`、`submission/datasource/service/SubDataSourceService:62`、`submission/outputinterface/service/SubOutputInterfaceService:84`、`submission/record/service/SubSubmissionRecordService:118/:218/:240`（3 处） |
| G6 sysmgmt/user-role-message-log-download | `sysmgmt/user/service/SysUserService:241`、`sysmgmt/role/service/SysRoleService:183`、`sysmgmt/message/service/SysMessageService:129/:148`（2 处）、`sysmgmt/log/service/SysOperationLogService:80`、`sysmgmt/download/service/DownloadTaskService:150` |
| G7 sysmgmt/config | `sysmgmt/config/receiver/.../SysDataReceiverService:75`、`.../outputtype/SysOutputTypeService:75`、`.../pushinterface/SysPushInterfaceService:96`、`.../businesstype/SysBusinessTypeService:84`、`.../enterprise/SysEnterpriseService:98`、`.../datatypeconfig/SysDataTypeConfigService:74` |

**排除组（非 Spring `Page`，D3）：** `reconciliation/controller/ReconciliationController:170`、`sysmgmt/config/dirmap/controller/DirMapConfigController:88`。

> 实施期每组迁移前对该文件 `git grep -n "new PageResult"` 复核行号（baseline drift 防护，红线 `baseline_drift_during_long_review_cycle`）。`SubSubmissionRecordService` 13 处 PageResult 引用中仅 3 处为 `new PageResult<>` 构造点，其余为类型签名/import，**不动**。

---

## Task 1 — `PageResult.from` 工厂 + spring-data-commons 依赖（TDD）

**Files:**
- Modify: `fep-common/pom.xml`（加 `spring-data-commons` 依赖）
- Modify: `fep-common/src/main/java/com/puchain/fep/common/domain/PageResult.java`（加 `from` 静态工厂）
- Test: `fep-common/src/test/java/com/puchain/fep/common/domain/PageResultTest.java`（既有，追加 `from` 用例）

**Step 1 — 写失败测试（追加到 `PageResultTest`，沿用既有 JUnit-5 `Assertions` 风格，红线质量清单 #9；新增 import：`org.springframework.data.domain.Page`/`PageImpl`/`PageRequest`、`static org.junit.jupiter.api.Assertions.assertThrows`/`assertEquals`/`assertTrue`）**

```java
@Test
void fromShouldMapContentAndPreserveRequestPaging() {
    Page<String> page = new PageImpl<>(
            List.of("a", "b"),
            PageRequest.of(0, 10),
            23L); // total 23 across pages
    PageResult<Integer> r = PageResult.from(page, 1, 10, String::length);
    assertEquals(List.of(1, 1), r.getRecords());
    assertEquals(23L, r.getTotal());
    assertEquals(1, r.getPageNum());      // 1-based 调用方值，非 page.getNumber()(0)
    assertEquals(10, r.getPageSize());
    assertEquals(3, r.getTotalPages());   // ceil(23/10)
}

@Test
void fromEmptyPageShouldReturnEmptyRecords() {
    Page<String> page = new PageImpl<>(List.of(), PageRequest.of(2, 10), 0L);
    PageResult<Integer> r = PageResult.from(page, 3, 10, String::length);
    assertTrue(r.getRecords().isEmpty());
    assertEquals(0L, r.getTotal());
    assertEquals(3, r.getPageNum());
    assertEquals(0, r.getTotalPages());
}

@Test
void fromNullMapperShouldThrowNpe() {
    Page<String> page = new PageImpl<>(List.of("a"), PageRequest.of(0, 10), 1L);
    assertThrows(NullPointerException.class, () -> PageResult.from(page, 1, 10, null));
}
```

**Step 2 — 跑测试确认 RED**

Run（worktree 内）：
```
$env:JAVA_HOME=...; .\mvnw.cmd -q -pl fep-common -o test -Dtest=PageResultTest > C:\tmp\t1.log 2>&1
```
Expected: 编译失败 / `from` 方法不存在（先证 RED；若 `-o` 缺上游则先 `-am install -DskipTests` 装 fep-parent）。

**Step 3 — 最小实现**

`fep-common/pom.xml` 加（version 由 Spring Boot BOM 管理，不写显式版本）：
```xml
<dependency>
    <groupId>org.springframework.data</groupId>
    <artifactId>spring-data-commons</artifactId>
</dependency>
```

`PageResult.java` 加 import `org.springframework.data.domain.Page` + `java.util.Objects` + `java.util.function.Function`，并加：
```java
/**
 * 由 Spring Data {@link Page} 构造分页响应：映射当前页内容，透传调用方 1-based 页码/页大小。
 *
 * <p>页码/页大小取调用方传入值（非 {@code page.getNumber()}），以保持既有 1-based 语义，
 * 不引入 0-based↔1-based 偏移（参见 PRD §9.2 与既有控制器 adapter 约定）。</p>
 *
 * @param page     Spring Data 分页结果（提供 content 与 totalElements）
 * @param pageNum  当前页码（1-based，调用方语义）
 * @param pageSize 每页大小
 * @param mapper   实体 → 记录 DTO 映射函数
 * @param <E>      源实体类型
 * @param <T>      目标记录类型
 * @return 分页响应
 */
public static <E, T> PageResult<T> from(
        final Page<E> page,
        final int pageNum,
        final int pageSize,
        final Function<? super E, ? extends T> mapper) {
    Objects.requireNonNull(page, "page");
    Objects.requireNonNull(mapper, "mapper");
    final List<T> records = page.getContent().stream()
            .<T>map(mapper)
            .toList();
    return new PageResult<>(records, page.getTotalElements(), pageNum, pageSize);
}
```

**Step 4 — 跑测试确认 GREEN**

Run: `.\mvnw.cmd -q -pl fep-common -o test -Dtest=PageResultTest > C:\tmp\t1.log 2>&1` → 读 log 确认 `BUILD SUCCESS` + 0 fail（独立 `cat` 命令读结果，禁链式 commit，红线 `commit_no_chain_with_verify_command`）。

**Step 5 — spotbugs + 全 fep-common 回归**

Run: `.\mvnw.cmd -pl fep-common -o verify > C:\tmp\t1v.log 2>&1` → spotbugs 0（`from` 无 EI_EXPOSE：`List.copyOf` 已在构造器）。

**Step 6 — Commit**

```
git add fep-common/pom.xml fep-common/src/main/java/com/puchain/fep/common/domain/PageResult.java fep-common/src/test/java/com/puchain/fep/common/domain/PageResultTest.java
git commit  (-m "refactor(common): add PageResult.from(Page,pageNum,pageSize,mapper) generic factory"  -m "AI-Generated: claude-code"  -m "Reviewed-By: pending")
```

> Task 1 后派 spec + quality review（只读 Explore，禁 mvn）。

---

## Task 2..8 — 逐组迁移站点（G1..G7，behavior-preserving）

**每组（以 G1 为模板）的 5 步循环：**

**Step A — 复核行号 + 读当前块**：`git grep -n "new PageResult" <file>`，读构造块确认形态 = `records = page.getContent().stream().map(M).toList(); return new PageResult<>(records, page.getTotalElements(), pageNum, pageSize);`。

**Step B — 替换为工厂调用**（示例 `CollectionRunQueryService`）：
```java
// before
final List<CollectionRunResponse> records = page.getContent().stream()
        .map(CollectionRunResponse::from)
        .toList();
return new PageResult<>(records, page.getTotalElements(), req.getPageNum(), req.getPageSize());
// after
return PageResult.from(page, req.getPageNum(), req.getPageSize(), CollectionRunResponse::from);
```
- 删 import `PageResult`? 否——`from` 仍在 `PageResult`，import 保留；若文件不再用 `Page` 须保留（仍用 `page` 变量）。
- 删除孤立的 `records` 局部变量；若 `List` import 因此变孤立 → 清理（checkstyle UnusedImports）。
- **pageNum/pageSize 来源逐站点照抄原值**（`req.getPageNum()` / `pageNum` / `safePage,safeSize` 等），**不得**改成 `page.getNumber()+1`。
- mapper 为 lambda（如 `SysUserService` `u -> UserResponse.from(u, ...)`、`SysMessageService` `m -> MessageResponse.from(m, readIds...)`）→ 原 lambda 整体作为 `mapper` 实参。
- D5 `TlqConnectivityController`（`Page<ConnectivityRecordResponse>` 已是 DTO）→ `PageResult.from(result, page + 1, size, Function.identity())`（`page` 为 0-based 入参，原已 `page+1`，照抄）。

**Step C — 跑该模块/相关测试确认 GREEN（回归背书行为保持）**：
```
.\mvnw.cmd -q -pl fep-web -o test -Dtest=<该组相关 *Test> > C:\tmp\g<N>.log 2>&1
```
（单模块 `-o` 不带 `-am`，红线 `single_module_regression_no_am_flag`；长跑 redirect-to-file，禁 |tail）。

**Step D — checkstyle + spotbugs（增量）**：组内文件 compile + `spotbugs:check`（红线 `spotbugs_check_needs_recompile_after_annotation`：先 compile 再 check）。

**Step E — Commit**（每组一 commit，逐 commit 自洽可编译，红线 `commit_tree_self_consistent_per_commit`）：
```
git add <该组文件>
git commit (-m "refactor(<area>): use PageResult.from at <N> sites (behavior-preserving)" -m "AI-Generated: claude-code" -m "Reviewed-By: pending")
```

> 每组（Task）后派 spec + quality review（只读 Explore）。

**组—Task 映射：** Task2=G1、Task3=G2、Task4=G3、Task5=G4、Task6=G5、Task7=G6、Task8=G7。

---

## Task 9 — 全量回归 + 收尾

**Step 1 — 全 fep-web verify（权威回归交 GHA；本地视 load 量力，红线 `single_module_regression_no_am_flag` + `shared_m2_snapshot_cross_session_clobber`）**：
```
.\mvnw.cmd -pl fep-web -o verify > C:\tmp\web-verify.log 2>&1   # 视 load；否则交 GHA strong
```
Expected: fep-web 全测试 0 fail（1290 + 3 新 fep-common 测试不计入 web）+ spotbugs 0 + ArchUnit PASS + JaCoCo 达标。

**Step 2 — 残留站点核实**：`git grep -n "new PageResult<>" fep-web/src/main` → 仅余 D3 排除的 2 处（Reconciliation/DirMap）+ 任何含特殊语义未迁移点；确认迁移完备且无误删。

**Step 3 — 逐 commit 自洽核验**：`git log --oneline origin/main..HEAD` + 抽查 `git show <sha> --stat`，确认每 commit 独立可编译（接口未变，纯调用点内部收敛）。

**回归两层（红线 `plan_regression_scope_explicit`）：**
- **strong**：GHA Build/Test & Quality + SonarCloud 全绿（权威）。
- **minimum**：本地 `fep-common -o verify` GREEN + `fep-web -o test`（量力，或交 GHA）。

---

## 风险与缓解

| 风险 | 等级 | 缓解 |
|------|:----:|------|
| pageNum/pageSize 被误改为 `page.getNumber()`（0-based 回归）| 🟡 中 | 每站点照抄原 1-based 实参；Task1 测试钉死「透传非 page.getNumber()」不变量；既有端点测试回归背书 |
| 误迁移自定义分页站点（Reconciliation/DirMap）破坏 | 🟡 中 | D3 显式排除 + Step2 残留核实确认二者仍在 |
| fep-common 加 spring-data 触 ArchUnit/分层 | 🟢 低 | Task1 前 grep ArchUnit 规则；fep-common 已耦合 spring-boot-starter-web；如禁则回落 D1-(B) fep-web util |
| 多会话共享 .m2 clobber / baseline drift | 🟡 中 | 隔离 worktree；`-o` 遇 ClassNotFound 先查 .m2 漂移（红线 `shared_m2_snapshot_cross_session_clobber`）；每组迁移前复核行号 |
| 孤立 import / 局部变量残留（checkstyle）| 🟢 低 | Step B 清理；spotbugs/checkstyle 增量门禁 |

## 回归验收

- fep-common：`PageResultTest` 新增 3 用例全绿 + spotbugs 0。
- fep-web：既有 1290 测试 0 fail（行为保持，无新测试需求——迁移由既有端点测试覆盖）+ ArchUnit/spotbugs/JaCoCo 达标。
- `new PageResult<>` 构造点：27 迁移 → 0 残留（除 D3 排除 2 处）。

---

## 评审与签字

- **santa 独立评审（只读 Explore）**：Round 1 = **PASS-WITH-MINOR**（0 BLOCKER / 2 MINOR：① G6 漏列 `SysOperationLogService:80` ② 测试风格 AssertJ vs 既有 JUnit-5 Assertions）。两 MINOR 已修订为 v0.2（站点 27 迁移内部自洽；测试 snippet 改 JUnit-5 Assertions 风格对齐既有 `PageResultTest`）。
- **Plan 批准者 muzhou**：✅ **APPROVED**（2026-06-18，AskUserQuestion「签字 + 开始实施」）。
- **实施**：hybrid（主对话 edits+mvn+commit / 只读 Explore spec+quality review，禁 mvn）。每 Task 后 spec+quality review；final Simplify 三审 + session-end 7-phase。
