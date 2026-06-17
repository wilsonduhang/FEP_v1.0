# B8 §5.8 多级审核工作流 Phase2 后端 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 在已闭合的 §5.8 校验引擎之上，新增「业务规则失败（PROC_8507）报文 → 人工审核任务 → 单级通过/驳回」的后端工作流（表 + 服务 + REST API + RBAC seed），不改动已闭合的状态机与三通路。

**Architecture:** 加法式（additive）。引擎对业务规则失败的报文仍照常落 `FAILED`/`PROC_8507`（语义不变）；审核任务在 fep-web 的**唯一持久化扼点** `JpaMessageProcessStore.updateStatus` 处旁路生成（`FAILED && errorCode==PROC_8507` 时创建一条 `message_review_task`），与 FAILED 落库**同事务**保证原子性。审核工作流（列表/通过/驳回）是 fep-web 独立模块，单级审核，层级数做成配置项 `fep.review.levels`（默认 1）预留多级扩展点。**零** fep-processor 代码改动。

**Tech Stack:** Java 17 / Spring Boot 3.x / Spring Data JPA / Flyway (H2 MODE=MySQL test + MySQL prod) / JUnit 5 + MockMvc / OpenAPI(springdoc) / SpotBugs + ArchUnit + Checkstyle 门禁。

> **签字状态:** ✅ APPROVED 2026-06-17（muzhou 签字，含 D5 Batch-deferred 边界确认）。santa-method PASS-WITH-MINOR（无 BLOCKER；M1 `@PreAuthorize` 理由订正 + m1 引用 + m3 PRD trace + n1 幂等测试 已 boil-lake 全修）。执行方式：本会话 hybrid（主对话 edits+mvn+commit，subagent 仅只读评审）。

---

## 元信息

- **PRD 依据:** §5.8 数据校验与审核（行 1570-1600）功能点 2「多级审核：系统自动校验 → 业务人员人工审核（可配置）」。
- **FR-ID:** FR-WEB-AUDIT（矩阵 line 286，§5.8）；本阶段落 **FR-WEB-AUDIT-REVIEW**（Phase2 人工审核子需求，签字时并入矩阵）。
- **开发模式:** A（AI 主导）。**非安全禁入区**（不触 `security/impl`，不涉密钥/国密）。
- **执行 Worktree:** `E:\FEP_v1.0_wt-msg-review-p2`（分支 `feat/b8-msg-review-phase2`，触发条件第 2 项「与已签字未执行 Plan 并存」+ 第 6 项「多会话并发」）。
  ⚠️ 命名澄清：本 worktree **不是** `wt-b8-deferred`（那是别会话 **B-8 Dashboard WebSocket** 的 deferred 池清理，与本 §5.8 **B8 多级审核** 同名不同物）。
- **代码仓:** `E:\FEP_v1.0`（PR + GHA CI）。**文档仓:** `E:\FEP`（直接 push main，session-end Phase 7）。
- **回归口径（红线 `plan_regression_scope_explicit` + `single_module_regression_no_am_flag`）:**
  - minimum: `./mvnw -pl fep-web -o test`（不带 `-am`；上游 SNAPSHOT 缺则先一次性 `-am install -DskipTests`）。
  - strong: GHA CI（Build/Test/Quality + SonarCloud）。
  - ⚠️ 多会话共享 4 核机：`uptime` 实测 load>100 立即 `pkill` **本 worktree-slug 精确匹配**的 fork，不碰别会话；load<30 续。

---

## 范围决策（muzhou 已拍板 2026-06-16 + 实测衍生边界）

| 决策 | 选择 | 依据 |
|---|---|---|
| D1 接入方式 | **加法 `message_review_task` 表** | 不动已闭合状态机/三通路；旁路扼点 = `JpaMessageProcessStore.updateStatus`（fep-web 适配器，非 fep-processor） |
| D2 审核层级 | **单级 + 配置预留** `fep.review.levels`（默认 1） | YAGNI；多级 L1→L2 路由 Phase-next |
| D3 交付边界 | **只后端 + REST API + RBAC seed** | §5.8/5.9 无原型，Vue UI Phase3 受阻 |
| **D4 触发范围（实测衍生）** | **仅 PROC_8507（业务规则失败）** | PROC_8501（XSD 结构失败）= 报文格式错误，属「自动拒绝/通知修正」（PRD §5.8 异常流 Phase3），非人工业务审核对象 |
| **D5 通路覆盖（实测衍生，⚠️ 须 muzhou 确认）** | **Sync + Async 覆盖；Batch 审核 deferred** | 实测：Sync/Async 经 `failWith` 落 `errorCode=PROC_8507`（扼点可见，`SyncMessageProcessorService:151` / `AsyncMessageProcessorService` failWith 路径）；**Batch 经 `transition(...FAILED)`，`error_code` 故意保持 null**（明细仅存 `BatchResult.errors` 内存，不持久化，见 `BatchMessageProcessorService:152-153,163-165`）→ 扼点结构性不可见。覆盖 Batch 须改 batch 流水线，超出「不改三通路」约定 |

> **D5 是本 Plan 最关键的 scope 边界**，请评审/签字重点确认：Phase2 仅覆盖单条同步/异步报文的业务规则审核；批量报文（2101/2102/2103/2104 等）的逐条审核留待后续（届时需 muzhou 决策是否放宽「不改三通路」以持久化 batch 逐条违规明细）。

### 已实测确认的事实锚（红线 `plan_must_grep_actual_api`）

- 最高 Flyway = **V40**（`ls db/migration | sort -V | tail`）→ 本 Plan 用 **V41**(表) + **V42**(RBAC seed)；**实施第一步须重 grep 最高 V**（多会话并发，红线 `plan_flyway_v_collision_check`），冲突则顺延。
- 扼点 `JpaMessageProcessStore.updateStatus(id, newStatus, errorCode, errorMessage)` @Transactional，`newStatus==FAILED` 时写 errorCode/errorMessage（`JpaMessageProcessStore.java:79-96`）。
- `FepErrorCode.PROC_8507` 在 fep-common，fep-web 可见。
- Web 控制器范式：`ApiResult<PageResult<T>>`、**1-based** 分页（`@RequestParam(defaultValue="1") pageNum`）、`@Tag/@Operation/@ApiResponse`、`@OperationLog`。
- 鉴权：新控制器 `@PreAuthorize("hasRole('SYSTEM_ADMIN')")`。**理由（santa M1 订正）**：`JwtAuthFilter:132-133` 把 `roleCode → "ROLE_"+code`，`V2__seed_admin_user.sql:30` 实 seed `role_code=SYSTEM_ADMIN` → `hasRole('SYSTEM_ADMIN')` 恰好授权已 seed 的超管，且 V42 给同一 `...010` 角色授 `view`，自洽。⚠️ **不**对齐 callback 控制器（`CallbackDlqController:36` 用 `hasRole('ADMIN')`，但 `ADMIN` role_code **未 seed**，是那些控制器的潜伏缺陷，非可援引先例）；collector 控制器干脆不加 `@PreAuthorize`。
- **PRD 主体（santa m3）**：PRD §5.8:1577 指明审核主体为「业务人员」，§5.10 预定义业务人员为 seeded 角色。本 Phase2 仅 seed 超管 `view` + **业务人员角色 / 细粒度 `audit:review:approve`·`:reject` 权限点 deferred**（镜像 V24:14 trigger 权限 deferral）——故 `SYSTEM_ADMIN`-only 是 Phase2 **过渡门**（UI 无消费方），非误读 PRD；业务人员绑定随 Phase3 UI + RBAC 对齐 ticket 落地。
- `PageResult<T>` 构造：`new PageResult<>(records, total, pageNum, pageSize)`。
- 菜单父节点 `DATA_AUDIT` 已存在：`menu_id=10000000000000000000000000000008`, `route_path=/audit`（`V3__seed_default_menus.sql:15`）。子菜单 menu_id 走 `2000...00XX` 段，**实施时 grep 现有最大 id 避撞**。
- 表 `t_sys_menu(menu_id,menu_code,menu_name,parent_id,menu_level,menu_icon,sort_order,menu_status,route_path)` + `t_sys_role_permission(role_id,menu_id,permission_code)`；超管 role_id=`00000000000000000000000000000010`。

---

## 文件清单（总览）

新增（均在 fep-web）：
- `db/migration/V41__create_message_review_task.sql`
- `db/migration/V42__seed_msg_review_menu.sql`
- `web/audit/review/domain/ReviewStatus.java`（枚举 PENDING/APPROVED/REJECTED）
- `web/audit/review/domain/MessageReviewTaskEntity.java`（JPA 实体）
- `web/audit/review/repository/MessageReviewTaskRepository.java`（Spring Data）
- `web/audit/review/config/ReviewWorkflowProperties.java`（`fep.review.levels` 默认 1）
- `web/audit/review/service/MessageReviewTaskService.java`
- `web/audit/review/dto/ReviewTaskResponse.java` / `ReviewDecisionRequest.java`
- `web/audit/review/controller/MessageReviewController.java`

修改（fep-web，加法）：
- `web/integration/processor/JpaMessageProcessStore.java`（`updateStatus` 末尾旁路调用 review 服务；+1 依赖注入）

测试（fep-web）：每 Task 对应 `src/test/.../audit/review/*Test.java`

---

## Task 0：worktree 隔离（执行起点）

**Step 0.1** 实测本地 main 已 ff（红线 `stale_local_main_worktree`）

```bash
git -C E:/FEP_v1.0 fetch
git -C E:/FEP_v1.0 rev-parse --short HEAD origin/main   # 两值须相等；不等则先在自建 clean worktree 操作
```

**Step 0.2** 建 worktree（off origin/main，红线 `shared-working-tree-needs-worktree`）

```bash
git -C E:/FEP_v1.0 worktree add -b feat/b8-msg-review-phase2 ../FEP_v1.0_wt-msg-review-p2 origin/main
```

**Step 0.3** 重 grep 最高 Flyway V（多会话并发；红线 `plan_flyway_v_collision_check`）

```bash
ls E:/FEP_v1.0_wt-msg-review-p2/fep-web/src/main/resources/db/migration | sort -V | tail -3
```
预期 V40 最高 → 用 V41/V42；若已被别会话占用则顺延并全 Plan 同步改号。

**Step 0.4** 把本 Plan 文件 copy 进 worktree 的 `docs/plans/` 并 `git add` + commit（签字后）。

---

## Task 1：V41 表 + 实体 + 仓储

**Files:**
- Create: `fep-web/src/main/resources/db/migration/V41__create_message_review_task.sql`
- Create: `fep-web/src/main/java/com/puchain/fep/web/audit/review/domain/MessageReviewTaskEntity.java`
- Create: `fep-web/src/main/java/com/puchain/fep/web/audit/review/domain/ReviewStatus.java`
- Create: `fep-web/src/main/java/com/puchain/fep/web/audit/review/repository/MessageReviewTaskRepository.java`
- Test: `fep-web/src/test/java/com/puchain/fep/web/audit/review/repository/MessageReviewTaskRepositoryTest.java`

**Step 1.1：写失败测试**（@DataJpaTest 或 @SpringBootTest）— 插入一条 PENDING 任务，按 `messageRecordId` 唯一约束二次插入抛异常，按 status 分页查询命中。

```java
@SpringBootTest
@Transactional
class MessageReviewTaskRepositoryTest {
    @Autowired MessageReviewTaskRepository repo;

    @Test
    void save_then_findByStatus_returnsPending() {
        MessageReviewTaskEntity t = newPending("rec-001", "txn-001");
        repo.save(t);
        Page<MessageReviewTaskEntity> page =
            repo.findByReviewStatus(ReviewStatus.PENDING.name(), PageRequest.of(0, 20));
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).getTransitionNo()).isEqualTo("txn-001");
    }

    @Test
    void uniqueMessageRecordId_rejectsDuplicate() {
        repo.saveAndFlush(newPending("rec-dup", "txn-a"));
        assertThatThrownBy(() -> repo.saveAndFlush(newPending("rec-dup", "txn-b")))
            .isInstanceOf(DataIntegrityViolationException.class);
    }
    // newPending(...) builds entity with reviewId=uuid32, status=PENDING, createdAt=now millis, reviewLevel=1, currentLevel=1
}
```

**Step 1.2：跑测试确认失败** — `./mvnw -pl fep-web -o test -Dtest=MessageReviewTaskRepositoryTest`（预期编译失败：类不存在）。

**Step 1.3：写迁移 V41**（DDL 风格对齐 V38；H2 MODE=MySQL + MySQL 兼容）

```sql
-- §5.8 FR-WEB-AUDIT-REVIEW: 业务规则失败报文的人工审核任务表（Phase2 单级）。
-- 数据来源：JpaMessageProcessStore.updateStatus 旁路（FAILED && PROC_8507）。
-- Supports H2 MODE=MySQL (test) and MySQL 8.0+ (prod).
-- F-level compliance: V1-V40 zero modification.

CREATE TABLE IF NOT EXISTS message_review_task (
    review_id           VARCHAR(32)   NOT NULL,
    message_record_id   VARCHAR(32)   NOT NULL,   -- 关联 message_process_record.id
    message_type        VARCHAR(8)    NOT NULL,
    transition_no       VARCHAR(30)   NOT NULL,
    error_code          VARCHAR(16)   NOT NULL,   -- 固定 PROC_8507（预留未来其他可审核码）
    violation_summary   VARCHAR(512),             -- 首条违规文案（= record.error_message）
    review_status       VARCHAR(16)   NOT NULL,   -- PENDING/APPROVED/REJECTED
    review_level        INT           NOT NULL DEFAULT 1,  -- 需经过的总层级（配置 fep.review.levels 快照）
    current_level       INT           NOT NULL DEFAULT 1,  -- 当前已审到的层级
    assigned_reviewer_id VARCHAR(32),
    reviewer_id         VARCHAR(32),
    review_comment      VARCHAR(500),
    created_at          BIGINT        NOT NULL,   -- epoch millis（对齐 message_process_record）
    reviewed_at         BIGINT,
    CONSTRAINT pk_msg_review PRIMARY KEY (review_id),
    CONSTRAINT uq_msg_review_record UNIQUE (message_record_id)
);

CREATE INDEX IF NOT EXISTS idx_msg_review_status ON message_review_task (review_status);
CREATE INDEX IF NOT EXISTS idx_msg_review_created ON message_review_task (created_at);
```

**Step 1.4：写实体 + 枚举 + 仓储**（实体范式对齐 `MessageProcessRecordEntity`：plain mutable POJO + getters/setters；epoch millis）。仓储：

```java
public interface MessageReviewTaskRepository extends JpaRepository<MessageReviewTaskEntity, String> {
    Page<MessageReviewTaskEntity> findByReviewStatus(String reviewStatus, Pageable pageable);
    Optional<MessageReviewTaskEntity> findByMessageRecordId(String messageRecordId);
}
```

**Step 1.5：跑测试确认通过** — 同 1.2 命令，预期 PASS。

**Step 1.6：commit**（独立命令，红线 `commit_no_chain_with_verify_command`）

```bash
git add fep-web/src/main/resources/db/migration/V41__create_message_review_task.sql \
        fep-web/src/main/java/com/puchain/fep/web/audit/review/domain/ \
        fep-web/src/main/java/com/puchain/fep/web/audit/review/repository/ \
        fep-web/src/test/java/com/puchain/fep/web/audit/review/repository/
git commit -m "feat(audit): V41 message_review_task table + entity + repository (§5.8 Phase2)

AI-Generated: claude-code
Reviewed-By: pending"
```

---

## Task 2：审核任务创建服务 + 配置

**Files:**
- Create: `.../audit/review/config/ReviewWorkflowProperties.java`
- Create: `.../audit/review/service/MessageReviewTaskService.java`
- Modify: 在 fep-web `@ConfigurationPropertiesScan` 或 `@EnableConfigurationProperties` 注册（grep 现有注册方式，对齐）
- Test: `.../audit/review/service/MessageReviewTaskServiceCreateTest.java`

**Step 2.1：写失败测试** — `createFromFailedRecord(entity)`：FAILED+PROC_8507 的 `MessageProcessRecordEntity` → 落一条 PENDING 任务（reviewLevel 取自配置默认 1，violationSummary=record.errorMessage）；**幂等**：同 messageRecordId 二次调用不抛、不重复落（先查 `findByMessageRecordId`）。

```java
@Test
void create_fromFailedRecord_persistsPendingTask() {
    MessageProcessRecordEntity rec = failedRecord("rec-1","txn-1","1001","PROC_8507","field X invalid");
    reviewService.createFromFailedRecord(rec);
    MessageReviewTaskEntity t = repo.findByMessageRecordId("rec-1").orElseThrow();
    assertThat(t.getReviewStatus()).isEqualTo(ReviewStatus.PENDING.name());
    assertThat(t.getReviewLevel()).isEqualTo(1);
    assertThat(t.getViolationSummary()).isEqualTo("field X invalid");
}

@Test
void create_isIdempotent_onDuplicateRecordId() {
    MessageProcessRecordEntity rec = failedRecord("rec-2","txn-2","1001","PROC_8507","x");
    reviewService.createFromFailedRecord(rec);
    reviewService.createFromFailedRecord(rec);   // 不抛（app-guard findByMessageRecordId 命中）
    assertThat(repo.findAll()).hasSize(1);
}
```

> n1（santa）：上面只覆盖串行 app-guard 路径。app-guard 是 TOCTOU，真正后盾是 `uq_msg_review_record UNIQUE`。补一条测试：绕过 app-guard 直接 `repo.saveAndFlush` 两条同 `messageRecordId` → 断言第二条抛 `DataIntegrityViolationException`（即 Task 1 的 `uniqueMessageRecordId_rejectsDuplicate`，已覆盖约束后盾；此处显式交叉引用，确认双重幂等闭环）。

**Step 2.2：跑测试确认失败。**

**Step 2.3：写实现** — `ReviewWorkflowProperties`（`@ConfigurationProperties("fep.review")`，字段 `int levels = 1`，注释「预留多级；MVP 单级」）；`MessageReviewTaskService.createFromFailedRecord`：`@Transactional`（随调用方扼点同事务），先 `findByMessageRecordId` 幂等守卫，构造 PENDING 实体（reviewId=uuid32, reviewLevel=props.levels, currentLevel=1, createdAt=now millis）save。

**Step 2.4：跑测试确认通过 + `spotbugs:check`**（红线 `subagent_must_run_spotbugs_check` / `spotbugs_check_needs_recompile_after_annotation`：改注解后先 compile 再 check；`int levels` 非集合，无 EI_EXPOSE）。

```bash
./mvnw -pl fep-web -o compile -am   # 首次确保上游 jar
./mvnw -pl fep-web -o test -Dtest=MessageReviewTaskServiceCreateTest
./mvnw -pl fep-web spotbugs:check    # 须先 compile
```

**Step 2.5：commit**（同范式，独立命令）。

---

## Task 3：扼点旁路接入（零 processor 改动）

**Files:**
- Modify: `fep-web/src/main/java/com/puchain/fep/web/integration/processor/JpaMessageProcessStore.java`
- Test: `.../integration/processor/JpaMessageProcessStoreReviewHookTest.java`（@SpringBootTest，真 JPA）

**Step 3.1：写失败测试** — 通过 store 落一条 RECEIVED 记录，再 `updateStatus(id, FAILED, "PROC_8507", "biz rule X")` → `message_review_task` 出现 1 条 PENDING；另一条 `updateStatus(id2, FAILED, "PROC_8501", "xsd err")`（XSD 失败）→ **不**产生审核任务（D4：仅 PROC_8507）；`updateStatus(id3, COMPLETED, null, null)` → 不产生。

```java
@Test
void updateStatus_failedWithProc8507_createsReviewTask() {
    store.save(MessageProcessRecord.initial("rec-h1", MessageType.byMsgNo("1001").orElseThrow(), "txn-h1", Instant.now()));
    store.updateStatus("rec-h1", MessageProcessStatus.FAILED, FepErrorCode.PROC_8507.getCode(), "biz rule X");
    assertThat(reviewRepo.findByMessageRecordId("rec-h1")).isPresent();
}

@Test
void updateStatus_failedWithXsd8501_noReviewTask() {
    store.save(MessageProcessRecord.initial("rec-h2", MessageType.byMsgNo("1001").orElseThrow(), "txn-h2", Instant.now()));
    store.updateStatus("rec-h2", MessageProcessStatus.FAILED, FepErrorCode.PROC_8501.getCode(), "xsd err");
    assertThat(reviewRepo.findByMessageRecordId("rec-h2")).isEmpty();
}
```

**Step 3.2：跑测试确认失败。**

**Step 3.3：写实现** — `JpaMessageProcessStore` 注入 `MessageReviewTaskService`（构造器 +1 参数；同 commit 改全部构造器调用方/测试，红线 `commit_tree_self_consistent_per_commit`）。在 `updateStatus` 末尾、`return` 前：

```java
final MessageProcessRecordEntity savedEntity = repository.save(entity);
if (newStatus == MessageProcessStatus.FAILED
        && FepErrorCode.PROC_8507.getCode().equals(errorCode)) {
    // best-effort：createFromFailedRecord 为 REQUIRES_NEW 独立事务，失败不回滚 FAILED
    try {
        reviewTaskService.createFromFailedRecord(
                savedEntity.getId(), savedEntity.getMessageType(),
                savedEntity.getTransitionNo(), errorCode, savedEntity.getErrorMessage());
    } catch (RuntimeException ex) {
        log.warn("review task creation failed (best-effort) for recordId={}; "
                + "FAILED state persisted regardless", LogSanitizer.sanitize(id), ex);
    }
}
return toDomain(savedEntity);
```

> 注：旁路只读 `entity`（已含 id/messageType/transitionNo/errorMessage），**不改 fep-processor 任何类**、不改方法签名、不改状态机。ArchUnit：新增依赖均在 fep-web 内，不破层。
> ⚠️ Task2 实施订正（santa Task2 NIT）：`createFromFailedRecord` 实际签名为 5 primitives（非 Plan 初稿的 `entity` 入参），以解耦 audit.review ↔ integration.processor；hook 传 entity getters 即可，无破坏。
> ⚠️ **Task3 事务语义订正（santa Task3 quality BLOCKER → muzhou 2026-06-17 决策 best-effort）**：`createFromFailedRecord` 改 `@Transactional(REQUIRES_NEW)` 独立事务 + `updateStatus` try-catch 记 WARN 续行。审核任务创建失败（极罕见并发唯一冲突）**不回滚**报文 FAILED——中转 liveness 不变量（报文必达终态）优先于审核完整性；丢失审核任务可由 FAILED+PROC_8507 记录扫描回填。**推翻 Plan 初稿「同事务，原子」**。
> ⚠️ **测试隔离副作用**：REQUIRES_NEW 提交独立于测试 `@Transactional` 回滚 → 触发 hook 的测试须**非事务 + @BeforeEach/@AfterEach 显式清理**共享 H2（`message_review_task` 整表 deleteAll / `message_process_record` 按 `rec-h%`·`rec-f%` 前缀 DELETE，前缀非 hex 不撞真实 UUID 记录），否则提交残留污染 `@Transactional` Task1 仓储测试的绝对计数断言（红线 `shared_h2_..._test_isolation`）。新增 `JpaMessageProcessStoreReviewHookFailureTest`（@MockBean 抛异常验 FAILED 仍落库）。

**Step 3.4：跑测试 + 全 fep-web 回归**（新增整链接入须全模块跑，红线 `full_regression_before_commit`；扼点构造器变更影响面）

```bash
./mvnw -pl fep-web -o test > /tmp/t3.log 2>&1   # 后台/重定向，禁 |tail（红线 pipe_tail_deadlock）
```
单独 `cat /tmp/t3.log | grep -E "Tests run|BUILD"` 确认 GREEN 后再 commit（红线 `commit_no_chain_with_verify_command`）。

**Step 3.5：commit。**

---

## Task 4：审核决策服务（list / approve / reject，单级 + 多级预留）

**Files:**
- Modify: `.../audit/review/service/MessageReviewTaskService.java`（增 list/approve/reject）
- Test: `.../audit/review/service/MessageReviewTaskServiceDecisionTest.java`

**Step 4.1：写失败测试**
- `approve(reviewId, reviewerId, comment)`：单级（levels=1）→ status APPROVED + reviewerId + reviewedAt 落值；currentLevel==reviewLevel。
- `reject(reviewId, reviewerId, reason)`：status REJECTED + reason 必填（空白抛 IllegalArgumentException）。
- 终态幂等：对已 APPROVED 的任务再 approve/reject 抛 IllegalStateException。
- `list(status, pageNum, pageSize)`：1-based → 转 0-based PageRequest（红线 `pagination_adapter`），返回 `PageResult`。
- **多级预留断言**：levels=2 时 approve 一次 → 仍 PENDING 且 currentLevel=2（未达 reviewLevel 不终结）；第二次 approve → APPROVED。（验证扩展点真实可用，非死代码。）

**Step 4.2：确认失败。**

**Step 4.3：实现** — approve 推进 `currentLevel`，`currentLevel >= reviewLevel` 才置 APPROVED；reject 任一层即 REJECTED。list 做 1-based→0-based 适配 + 组 `PageResult`。

**Step 4.4：测试通过 + spotbugs:check**（DTO/返回若含 List 注意 EI_EXPOSE，按红线 `configurationproperties_collection_getter_ei_expose` 处理）。

**Step 4.5：commit。**

---

## Task 5：REST 控制器

**Files:**
- Create: `.../audit/review/dto/ReviewTaskResponse.java`、`ReviewDecisionRequest.java`（`@NotBlank reason` for reject）
- Create: `.../audit/review/controller/MessageReviewController.java`
- Test: `.../audit/review/controller/MessageReviewControllerTest.java`（@WebMvcTest + MockMvc，service @MockBean）

**Step 5.1：写失败测试**（MockMvc）— GET 列表 200 + ApiResult/PageResult 形状；PUT approve 调 service.approve；PUT reject 缺 reason → 400。

**Step 5.2：确认失败。**

**Step 5.3：实现控制器**（镜像 `BizMessageRecordController`）：

```java
@RestController
@RequestMapping("/api/v1/audit/reviews")
@PreAuthorize("hasRole('SYSTEM_ADMIN')")
@Tag(name = "数据校验审核", description = "PRD §5.8 业务规则失败报文人工审核（Phase2 单级）")
public class MessageReviewController {
    // GET ""            search(status, pageNum=1, pageSize=20) -> ApiResult<PageResult<ReviewTaskResponse>>
    // GET "/{reviewId}" detail
    // PUT "/{reviewId}/approve"  body ReviewDecisionRequest(comment) -> ApiResult<Void>
    // PUT "/{reviewId}/reject"   body ReviewDecisionRequest(reason@NotBlank) -> ApiResult<Void>
    // 每方法 @Operation/@ApiResponse/@OperationLog；reviewerId 取自 SecurityContext 当前登录用户
}
```

> reviewerId 来源：grep 现有控制器如何取当前用户（`SecurityContextHolder` / 自定义 `@CurrentUser`），对齐既有范式，禁臆造。

**Step 5.4：测试通过 + spotbugs:check。**

**Step 5.5：commit。**

---

## Task 6：V42 RBAC 菜单 seed

**Files:**
- Create: `fep-web/src/main/resources/db/migration/V42__seed_msg_review_menu.sql`
- Test: `.../audit/review/MsgReviewMenuSeedTest.java`（@SpringBootTest，断言菜单行存在 + 超管 view 权限）

**Step 6.1：写失败测试** — 查 `t_sys_menu` 存在 `MSG_REVIEW`（parent=DATA_AUDIT），`t_sys_role_permission` 超管有该 menu 的 `view`。

**Step 6.2：确认失败。**

**Step 6.3：写 V42**（实施时 grep `t_sys_menu` 现有最大 `2000...` menu_id 选下一个；parent=`...008` DATA_AUDIT；范式对齐 V24）：

```sql
-- §5.8 FR-WEB-AUDIT-REVIEW: 在「数据校验审核」(DATA_AUDIT, V3:15) 下追加「待审核报文」子菜单。
-- 后端 API: /api/v1/audit/reviews（本 Plan Task 5）。Vue 路由 Phase3（依赖原型）。
-- 细粒度 audit:review:approve / :reject 权限点 + 业务人员角色 → RBAC 对齐 ticket deferred（镜像 V24 trigger 权限）。
INSERT INTO t_sys_menu (menu_id, menu_code, menu_name, parent_id, menu_level, menu_icon, sort_order, menu_status, route_path) VALUES
('20000000000000000000000000000XXX', 'MSG_REVIEW', '待审核报文', '10000000000000000000000000000008', 2, 'audit', 1, 'ACTIVE', '/audit/reviews');
INSERT INTO t_sys_role_permission (role_id, menu_id, permission_code) VALUES
('00000000000000000000000000000010', '20000000000000000000000000000XXX', 'view');
```

**Step 6.4：测试通过。**

**Step 6.5：commit。**

---

## Task 7：收口

**Step 7.1** 全 fep-web 回归（minimum）

```bash
cd E:/FEP_v1.0_wt-msg-review-p2 && ./mvnw -pl fep-web -o test > /tmp/t7.log 2>&1
# 单独 cat /tmp/t7.log | grep -E "Tests run|BUILD|spotbugs|ArchUnit" 确认全 GREEN
./mvnw -pl fep-web spotbugs:check
```

**Step 7.2** 逐 commit 自洽核验（红线 `commit_tree_self_consistent_per_commit`）：`git show <sha>:JpaMessageProcessStore.java` 确认 Task3 构造器变更与全部调用方同 commit。

**Step 7.3** push + 开 PR（GHA strong 背书）

```bash
git push -u origin feat/b8-msg-review-phase2
gh pr create --title "feat(audit): §5.8 多级审核工作流 Phase2 后端 (B8)" --body "..."
```
（mutation 遇网络错先 `gh pr view` 实测再决定，红线 `gh_mutation_network_error_verify_before_retry`。）

**Step 7.4** merge 后 worktree teardown

```bash
git -C E:/FEP_v1.0 worktree remove E:/FEP_v1.0_wt-msg-review-p2
git -C E:/FEP_v1.0 branch -d feat/b8-msg-review-phase2
```

**Step 7.5** session-end（7-phase）：对账本 §5 追加 + 矩阵 FR-WEB-AUDIT-REVIEW + 8 维技术文档 + Daily Report + 文档仓 push。

---

## 验收标准（Definition of Done）

1. fep-web minimum 回归 GREEN + spotbugs:check 0 + ArchUnit PASS；GHA CI strong 全绿。
2. PROC_8507（Sync/Async）业务规则失败 → 自动落 PENDING 审核任务（同事务、幂等）；PROC_8501/COMPLETED 不落（D4 实证）。
3. REST：列表（1-based 分页 + status 筛选）/ 详情 / 通过 / 驳回（reason 必填）全通；OpenAPI 注解齐全。
4. `fep.review.levels` 配置生效：默认 1 单级；=2 时多级推进路径有测试覆盖（扩展点非死代码）。
5. RBAC 菜单 seed 落库，超管可见。
6. **零 fep-processor 改动**（`git diff --stat origin/main -- fep-processor` 为空）；状态机/三通路字节不变。
7. Batch 审核 deferred、细粒度权限点 deferred 在 Plan/Daily/矩阵显式登记（红线 `audit_maturity_label_needs_prd_trace`）。

---

## 评审网（待执行）

- santa-method Plan 评审（PRD 对齐 / 完整性 / 一致性 / D5 scope 边界合理性）→ muzhou 签字。
- 每 Task：独立 spec review + quality review（只读 Explore，禁 mvn，红线 `review_subagent_must_not_run_mvn`）。
- final whole-impl review（逐 commit 自洽）。
