# §6.4.1 业务跟踪表实施 Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 补齐 PRD v1.3 §6.4.1「业务跟踪表」中缺失的 5 张表（发票核验 / 融资申请结果跟踪 / 对公账户信息 / 实时业务转发 / 非实时业务转发），含 Flyway 迁移 + JPA 实体 + Repository + 事件驱动写入路径 + 读取查询，使业务报文处理过程沉淀为可查询的业务维度持久化数据。

**Architecture:** 严格镜像已落地的 `reconciliation_records` 模式（P2e/P3）：Flyway 迁移（`fep-web/.../db/migration/V38+`）→ JPA Entity（`fep-web/.../integration/tracking/`，`@PrePersist/@PreUpdate` 补 audit 时间戳）→ Spring Data Repository（按业务键派生查询 + 幂等 upsert）→ 写入路径 `@EventListener onProcessed(InboundMessageProcessedEvent)`（按 `MessageType` 过滤 + `event.bodyAs()` 提字段 + 事务内持久化，事务回滚与 `message_process_record` 一致）→ 读取 QueryService。**tracer-bullet：先 发票核验记录表 端到端走通（3008 单报文 hook，字段全覆盖），再批量复制。**

**Tech Stack:** Java 17 / Spring Boot 3.x / Spring Data JPA / Flyway / H2(MODE=MySQL, test) + MySQL 8.0+(prod) / JUnit 5 + AssertJ / Maven 多模块。

**执行 Worktree:** `E:\FEP_v1.0_wt-tracking-tables`（分支 `feat/section-6-4-1-business-tracking-tables`，触发条件第 2 项「与已签字未执行的 Plan 并存」+ 第 6 项「muzhou/别会话 WIP 并存」——本 Plan 签字后即为已签字待执行 Plan，且 CLAUDE.md 记录有别会话活跃，须隔离避免污染 main commit history 与共享 `target/` race）。

---

## PRD 追溯

| FR-ID | PRD 章节 | 需求 | 本 Plan Task |
|---|---|---|---|
| FR-DATA-DB-01（矩阵 §6.4） | §6.4.1 | 业务跟踪表（电子凭证/发票/融资/账户/转发记录） | T1-T4 |
| FR-WEB-REPORT（数据基础） | §5.9 + §6.4.1 | 报表生成的源数据沉淀（报表 UI 受 ⚠️ 无原型阻塞，本 Plan 仅建**数据层**） | T1-T4 |

> **PRD §6.4.1 全集与现状（2026-06-14 git 实测）**：PRD 定义 8 张业务跟踪表。已实装 2 张 —— `reconciliation_records`（资金日对账信息表）+ `clearing_instruction_records`（资金清算指令表），均在 `V18__create_reconciliation_tables.sql`。**缺失 5 张**（全 SQL + Entity grep 返回空）：
> 1. 发票核验记录表（PRD §1970）→ T1 tracer-bullet
> 2. 融资申请结果跟踪表（PRD §1945）→ T2
> 3. 对公账户信息表（PRD §1958）→ T3
> 4. 实时业务转发记录表（PRD §2008）+ 非实时业务转发记录表（PRD §2020）→ T4

---

## 背景与设计依据（reconciliation 黄金模式实测）

写入路径与持久化分层**完全复刻**已评审落地的对账记录链（避免另起炉灶）：

| 层 | reconciliation 样板（实测路径） | 本 Plan 对应 |
|---|---|---|
| 迁移 | `fep-web/src/main/resources/db/migration/V18__create_reconciliation_tables.sql` | V38-V41（每 Task 一个 V，F 级 append-only，避免跨批 collision） |
| Entity | `fep-web/.../integration/reconciliation/ReconciliationRecordEntity.java`（`@Entity @Table` + `@PrePersist/@PreUpdate` 补 created_at/updated_at，因 SQL 无 DEFAULT） | `fep-web/.../integration/tracking/*Entity.java` |
| Repository | `fep-web/.../integration/reconciliation/ReconciliationRecordRepository.java`（派生查询 + `@Query` count） | `fep-web/.../integration/tracking/*Repository.java` |
| 写入路径 | `fep-web/.../reconciliation/listener/BankReconciliationEventListener.java`（`@EventListener onProcessed(InboundMessageProcessedEvent)`，filter `event.type()==MSG_3116`，`event.bodyAs(BankCheckDay3116.class)`，事务内 persist） | `fep-web/.../tracking/listener/*EventListener.java` |
| 事件 | `fep-processor/.../event/InboundMessageProcessedEvent.java`（record，`bodyAs(Class<T>)` / `type()` / `serialNo()`） | 复用，不改 |

**写入触发机制（实测确认）**：`InboundMessageDispatcher.dispatch` 在 `@Transactional(rollbackFor=Exception.class)` 边界内，**仅当处理 `status==COMPLETED`** 时对已注册报文发布 5 参 record `InboundMessageProcessedEvent(MessageType type, String transitionNo, String serialNo, Object body, Instant occurredAt)`；监听器抛异常即回滚外层事务，保证 `message_process_record` 与跟踪表一致。本 Plan 的跟踪表均挂在**已在 BODY_TYPE_REGISTRY 注册**的报文上（3008/3006 = BIDIRECTIONAL、3009 = OUTBOUND，三者均经 registry 走 inbound 解包路径——按 MessageDirection 枚举实值描述，非笼统 INBOUND），无需新增 body 注册（不触 `feedback_registered_inbound_body_must_implement_serialnobearing` 红线；24 项 `InboundRegistryArchTest` 快照不动）。

### ⚠️ 风险与诚实披露（须 muzhou 知情，已在选型时确认推进）

1. **消费方就绪度**：跟踪表的下游消费 = §5.9 报表 UI，当前受「⚠️ 无原型 + 依赖[C]监管报送文档 §3.4」阻塞。本 Plan **仅建数据层 + 写入路径**，使数据**从现在起开始沉淀**（历史数据不可追溯补建，故先建表有价值），但短期内无 UI 消费者。这是知情的「数据先行」决策，非「建了没人用」失误。读取层仅提供最小 QueryService（供后续报表/运维查询/调试），不建 Controller/UI。
2. **PRD 字段 ≠ 报文 body 字段 1:1**：部分 PRD 表字段在报文 body 中不存在或语义不同（见各 Task 字段映射表「来源」列）。映射偏差**逐字段透明记录**（镜像 `V18` 头部 `ADR-P2e-3 transparent type deviations`），body 无对应字段者标 `生成` / `事件时间` / `DEFERRED(领域确认)`，**禁臆造业务字段**。
3. **返回码语义不解释**：发票/账户/融资的 HNDEMP 返回码（如 `invoCheckReturnCode`）→ 业务结果 ENUM（核验通过/不通过）的映射涉及成功码语义争议（参 `DEF-B2-2` ClearingInstructionService 成功码颠倒上报）。本 Plan **存原始返回码**，`*_result` 列存原始码值 + Javadoc 注明「语义 ENUM 映射 DEFERRED 待②领域专家」，**不在本 Plan 臆造成功码规则**。
4. **转发记录表（T4）写入路径未定**：实时/非实时通用转发报文（3020/9000/3120/9100）为 BIDIRECTIONAL 且**不在 inbound body registry**（转发逻辑非 body 解包），写入 hook 点与前 3 表不同。T4 **第一步为 write-path 调研**，调研明确前不实装；若调研显示无干净 hook → T4 降级为「建表 + 实体 + repo，写入路径 deferred」并显式记录，禁强行套用 inbound-event 模式。

---

## Task 0：Worktree 隔离 + 基线核对

**Step 1: 建隔离 worktree（基于最新 origin/main）**

```bash
cd /e/FEP_v1.0
git fetch origin
git rev-parse HEAD origin/main   # 确认 HEAD == origin/main（无 drift），否则先对齐
git worktree add -b feat/section-6-4-1-business-tracking-tables ../FEP_v1.0_wt-tracking-tables origin/main
```
Expected: worktree 创建于 `E:\FEP_v1.0_wt-tracking-tables`，分支 `feat/section-6-4-1-business-tracking-tables` 指向 origin/main HEAD。

**Step 2: Flyway 版本号 collision 核对（红线 `feedback_plan_flyway_v_collision_check`）**

```bash
cd /e/FEP_v1.0_wt-tracking-tables
ls fep-web/src/main/resources/db/migration/ | grep -oE 'V[0-9]+' | sort -t V -k2 -n | tail -1
```
Expected: 最大 `V37`。本 Plan 用 **V38(发票)/V39(融资)/V40(账户)/V41(转发)**。**实施前若已 >V37，顺延并在 PR 中明示**。

---

## Task 1（tracer-bullet）：发票核验记录表 `invoice_verification_records`

**触发报文**：3008 `InvoCheckReturn3008`（INBOUND，已在 `BODY_TYPE_REGISTRY` 注册，已 `implements SerialNoBearing`，单报文携带全部 PRD 字段——零相关性最干净）。

**字段映射表（PRD §1970 → body / DDL）**：

| PRD 字段 | PRD 类型 | DDL 列 | DDL 类型 | 来源（InvoCheckReturn3008） | 备注 |
|---|---|---|---|---|---|
| invoice_id | VARCHAR(32) | `invoice_id` | VARCHAR(32) PK | 生成（serialNo 派生 32 位，镜像 reconciliation_id） | 系统生成唯一标识 |
| invoice_code | VARCHAR(12) | `invoice_code` | VARCHAR(12) | `invoCode` | PRD 12 位数字；body String |
| invoice_number | VARCHAR(8) | `invoice_number` | VARCHAR(8) | `invoNum` | |
| invoice_amount | DECIMAL(12,2) | `invoice_amount` | DECIMAL(20,4) | `invoAmt`(String→BigDecimal) | 类型偏差同 ADR-P2e-3（统一 20,4） |
| invoice_date | DATE | `invoice_date` | DATE | `invoDate`(String→LocalDate, yyyyMMdd) | 解析失败置 null + WARN |
| verification_result | ENUM | `verification_result` | VARCHAR(20) | `invoCheckReturnCode`（**存原始码**） | 语义 ENUM 映射 DEFERRED（风险披露#3）。`invoCheckReturnCode` 在 3008 为 `required=true`，正常非空；**缺失则抛异常回滚**（不存占位、不与「解析容错 null」混淆）。VARCHAR(20)：实施前 grep 返回码实际长度域确认（镜像 T3 AccReturnCode errata 核验，红线 `rule_master_plan_prescan_fixture_value_domain`） |
| verification_time | TIMESTAMP | `verification_time` | TIMESTAMP | 事件处理时间 `LocalDateTime.now()` | |
| failure_reason | VARCHAR(200) | `failure_reason` | VARCHAR(200) | `invoCheckReturnMemo` | |
| —（扩展） | | `serial_no` | VARCHAR(64) | `serialNo` | 幂等键 + 关联（镜像 recon serial_no），UNIQUE |
| —（扩展） | | `created_at`/`updated_at` | TIMESTAMP | audit（@PrePersist/@PreUpdate） | 镜像 recon |

**Files:**
- Create: `fep-web/src/main/resources/db/migration/V38__create_invoice_verification_records.sql`
- Create: `fep-web/src/main/java/com/puchain/fep/web/integration/tracking/InvoiceVerificationRecordEntity.java`
- Create: `fep-web/src/main/java/com/puchain/fep/web/integration/tracking/InvoiceVerificationRecordRepository.java`
- Create: `fep-web/src/main/java/com/puchain/fep/web/tracking/listener/InvoiceVerificationEventListener.java`
- Create: `fep-web/src/main/java/com/puchain/fep/web/tracking/service/InvoiceVerificationTrackingService.java`（字段映射 + 幂等 upsert，放 fep-web 服务层；纯映射无跨模块依赖）
- Test: `fep-web/src/test/java/com/puchain/fep/web/integration/tracking/InvoiceVerificationRecordRepositoryTest.java`（@DataJpaTest）
- Test: `fep-web/src/test/java/com/puchain/fep/web/tracking/listener/InvoiceVerificationEventListenerTest.java`（单元，mock service）
- Test: `fep-web/src/test/java/com/puchain/fep/web/tracking/InvoiceVerificationTrackingIntegrationTest.java`（@SpringBootTest 端到端：发布 3008 event → 表有行）

**Step 1: 写失败测试（Repository 持久化 + 幂等 upsert）**

`InvoiceVerificationRecordRepositoryTest`（@DataJpaTest，H2 MODE=MySQL，Flyway 跑 V38）：
- `save_then_findBySerialNo_returnsRow()`：存一行 → `findBySerialNo("S001")` 命中且字段相符。
- `upsert_sameSerialNo_noDuplicate()`：同 serialNo 存两次（先 find 再 update）→ `count()==1`（验 UNIQUE + 幂等）。
- `prePersist_fillsAuditTimestamps()`：不显式 set created_at → 保存后非 null（验 @PrePersist）。

**Step 2: 运行确认失败**

Run: `cd /e/FEP_v1.0_wt-tracking-tables && .\mvnw.cmd -pl fep-web -o test -Dtest=InvoiceVerificationRecordRepositoryTest`
Expected: FAIL（Entity/Repository/迁移不存在，编译错误或表不存在）。

**Step 3: 写最小实现**

3a. `V38__create_invoice_verification_records.sql`（镜像 V18 风格：`CREATE TABLE IF NOT EXISTS` + PK + UNIQUE(serial_no) + index + 头部 ADR 类型偏差注释 + F 级注记 V1-V37 zero modification）：

```sql
-- §6.4.1 FR-DATA-DB-01: invoice verification tracking table (PRD v1.3 §1970)
-- H2 MODE=MySQL (test) and MySQL 8.0+ (prod).
-- Transparent deviations from PRD:
--   1. invoice_amount DECIMAL(20,4) instead of PRD DECIMAL(12,2) — unified with recon ADR-P2e-3.
--   2. verification_result stores RAW HNDEMP return code (invoCheckReturnCode); semantic ENUM
--      mapping (核验通过/不通过/异常) DEFERRED to domain expert (see DEF-B2-2 success-code dispute).
--   3. serial_no / created_at / updated_at — non-PRD columns for idempotency + audit (mirror recon).
-- F-level compliance: V1-V37 zero modification.
CREATE TABLE IF NOT EXISTS invoice_verification_records (
    invoice_id          VARCHAR(32)   NOT NULL,
    invoice_code        VARCHAR(12),
    invoice_number      VARCHAR(8),
    invoice_amount      DECIMAL(20,4),
    invoice_date        DATE,
    verification_result VARCHAR(20)   NOT NULL,
    verification_time   TIMESTAMP     NOT NULL,
    failure_reason      VARCHAR(200),
    serial_no           VARCHAR(64)   NOT NULL,
    created_at          TIMESTAMP     NOT NULL,
    updated_at          TIMESTAMP     NOT NULL,
    CONSTRAINT pk_invoice_verif PRIMARY KEY (invoice_id),
    CONSTRAINT uq_invoice_verif_serial UNIQUE (serial_no)
);
CREATE INDEX IF NOT EXISTS idx_invoice_verif_date ON invoice_verification_records (verification_time);
CREATE INDEX IF NOT EXISTS idx_invoice_verif_code_num ON invoice_verification_records (invoice_code, invoice_number);
```

3b. `InvoiceVerificationRecordEntity`：`@Entity @Table(name="invoice_verification_records")`，字段镜像 `ReconciliationRecordEntity`（`@Id @Column`、`@PrePersist onCreate()` 补 created_at/updated_at、`@PreUpdate onUpdate()`、no-arg 构造器、全 getter/setter、完整 Javadoc）。`invoiceAmount` 用 `BigDecimal`，`invoiceDate` 用 `LocalDate`，`verificationTime/createdAt/updatedAt` 用 `LocalDateTime`。

3c. `InvoiceVerificationRecordRepository extends JpaRepository<InvoiceVerificationRecordEntity, String>`：`Optional<...> findBySerialNo(String serialNo)` + `Page<...> findByVerificationTimeBetween(LocalDateTime from, LocalDateTime to, Pageable p)` + `@Query("SELECT COUNT(r) FROM InvoiceVerificationRecordEntity r WHERE r.verificationResult = :code") long countByVerificationResult(@Param("code") String code)`。

**Step 4: 运行确认通过**

Run: `.\mvnw.cmd -pl fep-web -o test -Dtest=InvoiceVerificationRecordRepositoryTest`
Expected: PASS（3 测试绿）。

**Step 5: 写失败测试（写入路径 listener + service）**

- `InvoiceVerificationEventListenerTest`（单元，mock `InvoiceVerificationTrackingService`）：
  - `type_not_3008_returns_silently()`：event.type()=MSG_3116 → service 零调用。
  - `body_null_skips()`：3008 但 bodyAs 返回 null → service 零调用 + debug log。
  - `valid_3008_invokes_service()`：3008 + 真 `InvoCheckReturn3008` → service.track(body, serialNo) 调用 1 次。
- `InvoiceVerificationTrackingIntegrationTest`（@SpringBootTest）：用**正确 5 参构造器**发布事件
  `eventPublisher.publishEvent(new InboundMessageProcessedEvent(MessageType.MSG_3008, "T0000001", "S001", body, java.time.Instant.now()))`
  → `repository.findBySerialNo("S001")` 命中且 invoice_code/amount/result 正确映射。
  > ⚠️ **覆盖边界（MAJOR-2）**：此 IT 直接 `publishEvent` 只验证 listener→service→repo 链，**不经 `InboundMessageDispatcher.dispatch` 的 `@Transactional` + `status==COMPLETED` 门控**。须**补 1 个经 dispatcher 的端到端用例**（构造真实 3008 CFX envelope 走 `dispatch()` → 验跟踪表有行），证明生产路径真触发；若 dispatcher 端到端 IT 成本过高，则在测试 Javadoc 显式声明本 IT 不覆盖 dispatcher 触发门，并由 T5 strong 回归（GHA）兜底。

**Step 6: 运行确认失败** → **Step 7: 写实现**

7a. `InvoiceVerificationTrackingService`（`@Service`）：`track(InvoCheckReturn3008 body, String serialNo)` —— 映射字段（String→BigDecimal/LocalDate 容错，解析失败 null+WARN，用 `LogSanitizer.sanitize` 包敏感日志，红线 `logsanitizer_alone_insufficient_for_findsecbugs` → `@SuppressFBWarnings(CRLF_INJECTION_LOGS)`）→ `findBySerialNo` 存在则 update 否则 insert（幂等）→ save。invoice_id 用 serialNo 派生 32 位。

7b. `InvoiceVerificationEventListener`（`@Component`，镜像 `BankReconciliationEventListener`）：`@EventListener onProcessed(InboundMessageProcessedEvent event)` → `if(event.type()!=MessageType.MSG_3008) return;` → `bodyAs(InvoCheckReturn3008.class)` → null 则 debug skip → `service.track(body, event.serialNo())` → INFO log（sanitized）。

**Step 8: 运行确认通过 + spotbugs + ArchUnit**

Run: `.\mvnw.cmd -pl fep-web -o test -Dtest=InvoiceVerificationRecordRepositoryTest,InvoiceVerificationEventListenerTest,InvoiceVerificationTrackingIntegrationTest`
Run（MAJOR-1，与 T5 回归口径统一，**默认无 `-am`**）：`.\mvnw.cmd -pl fep-web -o verify`（触发 spotbugs:check + ArchUnit；红线 `feedback_subagent_must_run_spotbugs_check` + `spotbugs_check_needs_recompile_after_annotation` → verify 含重编译）。**仅当上游 SNAPSHOT 缺失**时先一次性 `.\mvnw.cmd -am install -DskipTests` 装上游 jar 再 `-pl fep-web -o verify`（红线 `single_module_regression_no_am_flag`：禁常态 `-am`，多会话并发放大 load + target race）。
Expected: 测试全绿 + `BugInstance size is 0` + ArchUnit PASS（新包 `integration.tracking` / `tracking.listener` / `tracking.service` 符合 8 层依赖方向 + 命名规范）。

> ⚠️ ArchUnit 预判：`tracking.listener`/`tracking.service` 在 fep-web，依赖 fep-processor 的 body POJO + event（与 reconciliation listener 同向，合规）。`@Service` 命名须 `*Service` 结尾、`@Component` listener 类名 `*EventListener`（已遵）。

**Step 9: Commit**

```bash
git add fep-web/src/main/resources/db/migration/V38__create_invoice_verification_records.sql fep-web/src/main/java/com/puchain/fep/web/integration/tracking/ fep-web/src/main/java/com/puchain/fep/web/tracking/ fep-web/src/test/java/com/puchain/fep/web/integration/tracking/ fep-web/src/test/java/com/puchain/fep/web/tracking/
git commit -m "feat(web): §6.4.1 invoice verification tracking table (tracer-bullet) — V38 + entity + repo + 3008 event write-path

AI-Generated: claude-code
Reviewed-By: pending"
```

> commit 前先独立跑验证、独立 cat 日志确认 GREEN、再独立 commit（红线 `feedback_commit_no_chain_with_verify_command`，禁 `cat log && commit`）。

---

## Task 2：融资申请结果跟踪表 `financing_application_records`

**触发报文**：3009 `RzReturnInfo3009`（融资结果登记，OUTBOUND 但经 registry 走 inbound 解包，已注册 + implements SerialNoBearing）。3105 `RzApplyInfo3105`（融资申请）为补充来源（申请额度等），但跨报文相关性复杂——**T2 仅以 3009 单报文 hook 落「结果」侧**，申请侧字段标 DEFERRED（避免臆造相关性，与 tracer-bullet 同纪律）。

**字段映射表（PRD §1945 → body）**：

| PRD 字段 | DDL 列 | 来源（RzReturnInfo3009） | 备注 |
|---|---|---|---|
| application_id | `application_id` PK | `platApplyNo`（平台申请号） | PRD「人行平台生成全局 ID」= platApplyNo |
| enterprise_id | `enterprise_id` | `hxqyName`/无 USCI 字段 | ⚠️ body 无 USCI；存 hxqyName，enterprise_id DEFERRED |
| application_amount | `application_amount` | `rzAmtInfo`（嵌套，融资金额信息） | 嵌套块，取主额度字段（实施时读 RzAmtInfo POJO 结构） |
| application_time | `application_time` | 事件时间 | body 无申请时间（在 3105），用结果到达时间 |
| approval_status | `approval_status` | `rzPhaseCode`（**存原始码**） | 融资阶段码；语义 ENUM DEFERRED（风险#3） |
| approval_amount | `approval_amount` | `rzAmtInfo` 审批额度字段 | |
| result_notice_time | `result_notice_time` | 事件时间 | |
| reject_reason | `reject_reason` | `rzPhaseInfo` | 阶段说明 |
| —（扩展） | `serial_no`/`created_at`/`updated_at` | `serialNo`/audit | 幂等 + audit |

> **实施前置**：读 `RzReturnInfo3009` 的 `rzAmtInfo`（RzAmtInfo）+ `dbInfo` 嵌套 POJO 实际字段（grep `body/.../RzAmtInfo*.java`），按真实字段填映射，**禁臆造嵌套字段名**（红线 `plan_revision_must_grep_actual_api`）。

**Files / Steps：** 镜像 Task 1（V39 迁移 + `FinancingApplicationRecordEntity/Repository` + `FinancingApplicationEventListener`(filter MSG_3009) + `FinancingApplicationTrackingService` + 3 类测试）。TDD 五步同 Task 1。Commit footer 同。

---

## Task 3：对公账户信息表 `corporate_account_records`

**触发报文**：3006 `QyAccQueryReturn3006`（对公客户状态查询回执，BIDIRECTIONAL，已注册 + implements SerialNoBearing）。
> ⚠️ 术语冲突（Round2 建议）：body Javadoc 称「对公客户状态查询回执」，而 `MessageType.MSG_3006` displayName 为「对公账户查询回执」——双权威源不一致（非本 Plan 自造）。本表沿用 body Javadoc 命名；实施时不强行统一，仅在字段映射表保留此注。

**字段映射表（PRD §1958 → body）**：

| PRD 字段 | DDL 列 | 来源（QyAccQueryReturn3006） | 备注 |
|---|---|---|---|
| enterprise_id | `enterprise_id` | `qyAccCode`（企业账户代码） | PRD「USCI」；body 为账户代码，语义对齐实施时核 |
| account_number | `account_number` PK 组件 | `qyAccCode` | |
| account_name | `account_name` | `qyAccName` | |
| opening_bank | `opening_bank` | 无 | ⚠️ body 无开户行；DEFERRED null |
| account_type | `account_type` | 无 | ⚠️ DEFERRED null |
| account_status | `account_status` | `accReturnCode`（**存原始码**） | 账户返回码；语义 ENUM DEFERRED（风险#3，注：AccReturnCode errata 已补 4 值，参 `rule_master_plan_prescan_fixture_value_domain`） |
| last_verification_time | `last_verification_time` | 事件时间 | |
| —（扩展） | `serial_no`/`created_at`/`updated_at` | `serialNo`/audit | |

> ⚠️ 多 PRD 字段（opening_bank/account_type）body 无来源 → 透明标 DEFERRED + nullable，**不臆造**。PK 用 `serial_no`（单值，最简幂等），enterprise_id+account_number 加 index。

**Files / Steps：** 镜像 Task 1（V40 + Entity/Repository/Listener(MSG_3006)/Service + 3 测试）。

---

## Task 4：转发记录表（实时 + 非实时）—— 写入路径调研先行

**Step 1: 写入路径调研（不写代码，纯实测）**

通用转发报文 3020/9000（实时）、3120/9100（非实时）为 BIDIRECTIONAL 且**不在 `BODY_TYPE_REGISTRY`**。调研：
```bash
cd /e/FEP_v1.0_wt-tracking-tables
git grep -nE "3020|9000|9100|3120|Forward|forward" -- 'fep-web/**/*.java' 'fep-processor/**/*.java' | grep -iE "listener|dispatch|process|route|forward" | head -40
```
确认：① 转发报文实际流经哪个处理点（同步流水线？转发服务？）② 有无可挂的事件/hook ③ 转发的 request/response content 在哪可取（PRD 表要 request_content TEXT / response_content TEXT）。

**Step 2: 决策门（按调研结果二选一）**

- **若存在干净 hook**（如转发处理点可发布事件或可直接注入跟踪 service）：实装 V41 双表（`realtime_forward_records` + `batch_forward_records`）+ Entity/Repository/Service/写入路径，TDD 五步同 Task 1。字段映射表（PRD §2008/§2020）实施前 grep 真实转发 body/上下文字段填。
- **若无干净 hook**（转发为透传无业务字段沉淀点）：T4 降级为 **deferred** —— 仅建表 + Entity + Repository（数据层就绪），写入路径列入 backlog 并在 Daily Report + PRD 矩阵显式记录「写入路径待转发流程重构」，**禁强套 inbound-event 模式臆造 hook**（风险披露#4）。

**Step 3: Commit**（按 Step 2 结果，footer 同 Task 1）

### ✅ T4 调研结论与决议（2026-06-14 实施）

**调研实测**（git grep）：转发 4 报文 3020/9000/3120/9100 均为 `MessageDirection.BIDIRECTIONAL`、**不在 `InboundMessageDispatcher.BODY_TYPE_REGISTRY`**、**无任何转发 listener/service**（仅 body POJO Forward9000/9100/3020/3120 + 通用 `MessageProcessRecordEntity`）。dispatcher 不为未注册报文发布带 body 的 `InboundMessageProcessedEvent` → **无干净 event hook**。

**决议（muzhou 2026-06-14 拍板）：T4 两张转发表完全 deferred —— 不建空表/实体/repo**（偏离 Plan 原述「仅建表」，muzhou 选更 honest 路线）。理由：转发为透传无业务字段沉淀点；强行注册 Forward body 仅为写记录会触 `feedback_registered_inbound_body_must_implement_serialnobearing` 红线 + 臆造 hook；建无 writer/reader 的空 schema = YAGNI/「建了没人用」。待转发流程未来提供真实 hook 再建。已在 Daily Report + PRD 矩阵显式记录。

---

## Task 5（closing）：worktree 闭环 + 矩阵更新

**Step 1: PRD 矩阵更新（红线 `feedback_session_end_prd_matrix_auto_update`）**

⚠️ **跨仓注意（MINOR-4）**：矩阵 `prd-traceability-matrix.md` 在**文档仓 `E:\FEP`**（非代码 worktree `E:\FEP_v1.0_wt-tracking-tables`）。须在 `E:\FEP` 仓内编辑并经 session-end Phase 7 commit+push（红线 `feedback_fep_docs_repo_commit_taboo` 已取代版），**不可在代码 worktree 提交**。
更新内容：FR-DATA-DB-01 §6.4 由 🟡 → 🟢（发票/融资/账户）+ 转发表按 T4 结果标 🟢/🟡；**同步修正 FR-DATA-DB-01 描述文案**（现为「电子凭证记录表」，本 Plan 实覆盖发票/融资/账户/转发 4 类——避免 FR-ID 语义漂移，评审① MINOR-3）；同步 §6.4.1 表清单实测真相（8 张：2 已存 + 本 Plan 新增）。

**Step 2: 回归验收（红线 `feedback_plan_regression_scope_explicit` + `single_module_regression_no_am_flag`）**

- **minimum（本机）**：`.\mvnw.cmd -pl fep-web -o test`（上游 SNAPSHOT 缺则先一次性 `-am install -DskipTests`）—— fep-web 全模块测试 0 fail（含新增跟踪表测试 + 既有回归）。⚠️ 多会话并发监控 load，>100 杀**本 worktree-slug** fork 等 load<30 续（红线 `single_module_regression_no_am_flag`）。
- **strong（CI 兜底）**：PR 触发 GHA Build/Test/Quality 全 success（AI 本机测试结果可信度为零，以 GHA 外部测量为准——AI 诚信铁律 #3）。

**Step 3: worktree teardown**

```bash
cd /e/FEP_v1.0
git worktree remove ../FEP_v1.0_wt-tracking-tables   # 须先 merge/push 完成
git worktree list                                     # 确认无残留
```

---

## 回归 / 质量门禁自检（9 项，每 Task commit 前）

1. 无吞异常 / 空 catch（映射解析失败 → null + WARN，非吞）
2. 测试断言验证业务含义（字段值相符、幂等 count==1、@PrePersist 非 null——非 assertNotNull 假断言）
3. 边界：null body / 空字段 / String→BigDecimal·LocalDate 解析失败 / 重复 serialNo 幂等
4. 日志无敏感数据（serialNo/金额经 `LogSanitizer.sanitize` + `@SuppressFBWarnings(CRLF_INJECTION_LOGS)`）
5. 无未使用抽象（每 Entity/Repository/Service/Listener 均有调用方）
6. 无硬编码（msgNo 用 `MessageType.MSG_xxxx.msgNo()`，无魔法字面量）
7. 公共类/方法 Javadoc（镜像 reconciliation 全 Javadoc）
8. 无 `System.out` / `printStackTrace`
9. 风格一致（严格镜像 `integration/reconciliation` + `reconciliation/listener` 既有风格）

---

## 评审与签字

- **二次 AI 评审**（`processor`/持久化核心强制）：每 Task 完成派独立 spec review + quality review subagent（红线 `feedback_task_review_discipline`，只读静态、禁跑 mvn——红线 `feedback_review_subagent_must_not_run_mvn`）；全 Task 后 final whole-impl review（逐 commit 自洽核验，红线 `feedback_commit_tree_self_consistent_per_commit`）。
- **本 Plan 须 santa-method 独立评审 + muzhou 签字后方可执行**（无签字禁实施）。
- **执行形态**：长跑 mvn → 默认 hybrid（主对话实施 edits + 前台 mvn + commit / subagent 仅评审，红线 `feedback_harness_bg_detach_hybrid_default`）。

### 评审与签字记录

| 角色 | 谁 | 结论 | 日期 |
|---|---|---|---|
| Plan 作者 | Claude Code (mode A) | 起草 v0.2（boil-lake 加固 MAJOR-1/2 + 6 MINOR） | 2026-06-14 |
| santa 评审① PRD 对齐 | Claude Code (santa-method) | **PASS**（16/16 字段实测零臆造，3 MINOR） | 2026-06-14 |
| santa 评审② 质量/风险/红线 | Claude Code (santa-method) | Round1 **REVISE**（2 MAJOR+4 MINOR）→ v0.2 加固后 Round2 | 2026-06-14 |
| **Plan 批准者** | **muzhou** | **✅ APPROVED（签字 + 开始实施）** | 2026-06-14 |

> v0.2 修订项：MAJOR-1（Step 8 `-am verify`→`-o verify` 口径统一）/ MAJOR-2（IT 5 参构造器签名修正 + dispatcher 触发门覆盖边界补充）/ MINOR（INBOUND 措辞→MessageDirection 枚举实值、3006 报文名、verification_result 缺失行为+长度域、矩阵跨仓路径、FR-ID 文案漂移、签字表补全）。
