# 业务转发记录表（实时 + 非实时）Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: 用 superpowers:executing-plans 逐 Task 实施；每 Task 完成派独立 spec + quality review subagent（红线 `feedback_task_review_discipline`）。

**Goal:** 为 PRD v1.3 §6.4.1 定义但尚未建专表的两张业务跟踪表——**实时业务转发记录表** + **非实时业务转发记录表**——建立专用 DB 表 + 事件驱动写入路径，收口 FR-DATA-DB-01 在 §6.4.1 的最后两个子项。

**Architecture:** 镜像 #96（PR `399606e`，invoice/financing/corporate 三表）已验证的「fep-processor 发领域事件 → fep-web `@EventListener` 同步落库」模式。新增两个 fep-processor 领域事件（`BatchForwardProcessedEvent` / `SyncForwardProcessedEvent`，停留在 converter + java.* 表面，ArchUnit 不破），由批量/同步流水线在终态 publish；fep-web 侧新增两套 `Entity + Repository + TrackingService + EventListener` 落库。非实时表数据源 `BatchMessageProcessorService.process → BatchResult(total/success/failed)` 干净直映；实时表 `response_content` 无 S1 上行网关来源 → 透明 nullable DEFERRED（沿用 #96「无报文体来源字段透明 nullable 零臆造」先例）。

**Tech Stack:** Java 17 / Spring Boot 3.x / Flyway (V41/V42) / JPA (H2 MODE=MySQL 测试 + MySQL 8 生产) / Spring `ApplicationEventPublisher` 同步事件 / JUnit 5 + `@SpringBootTest`。

**执行 Worktree:** `E:\FEP_v1.0_wt-dzpz-record`（分支 `feat/forward-record-tables`，触发条件第 2 项「与已签字未执行 Plan 并存」+ 第 6 项「多会话并发，3 个别会话 worktree 活跃」）。base = `d5e3edb`（origin/main，2026-06-16 实测）。

**开发模式:** A（AI 主导 90%）。**⛔ 安全禁入检查：本 Plan 不触碰 `security/impl/`、不涉及密钥材料、不涉及国密签名/加解密。** 纯平台内部 DB 跟踪 + 事件落库。（报文体落库脱敏见 DECISION-5。）

**修订记录:** v0.1 起草 → santa Round1 REVISE（0 BLOCKER / 3 CONCERN / 3 MINOR）→ **v0.2 全 boil-lake 加固**：CONCERN-1 实时 scope 加 direction=OUT 维度并升必答门（DECISION-2）；CONCERN-2 澄清批量幂等键语义（msgId 稳定 upsert / 随机占位新插，均不违反 UNIQUE）；CONCERN-3 排除 `@TransactionalEventListener(AFTER_COMMIT)` 静默丢弃陷阱（DECISION-4）；MINOR-4 migration header 补 PRD 默认值不落地说明；MINOR-5 T5 调用方更正（全为测试类，生产 DI 注入不需改）；MINOR-6 报文体脱敏升 DECISION-5。→ **santa Round2 PASS-WITH-MINOR**（3 CONCERN + 3 MINOR 全 CLOSED）→ **v0.2.1 最后修正**：实测 `DesensitizeService`（`fep-security-api`，仅 4 字段级 mask，无全报文脱敏器）→ DECISION-5 推荐默认改为「request_content 仅存元信息 `msgNo:transitionNo`」零 PII 零新建脱敏器，request_content 列降为 VARCHAR(255)。→ **待 muzhou 签字**（必答门 DECISION-2 实时 scope msgNo 集合 + DECISION-5 存储范围）。

---

## 0. 前置实测基线（起草期 grep 实证，执行前须重测）

| 事实 | 实测来源 | 值 |
|---|---|---|
| #96 模式四件套 | `fep-web/.../tracking/{listener,service}` + `.../integration/tracking/` | `InvoiceVerificationRecordEntity` / `...Repository` / `InvoiceVerificationTrackingService` / `InvoiceVerificationEventListener` |
| 既有领域事件契约 | `fep-processor/.../event/InboundMessageProcessedEvent.java` | `record(type, transitionNo, serialNo, Object body, Instant occurredAt)` + `bodyAs(Class)`；body 可空 |
| 既有事件 publish 站点 | `InboundMessageDispatcher.java:184,203` | `@Transactional(REQUIRED, rollbackFor=Exception)` dispatch → COMPLETED 时 `eventPublisher.publishEvent(...)` |
| 批量流水线终态数据 | `BatchMessageProcessorService.java:107-183` | `process(CfxMessage)` 返回 `BatchResult(total, success, failed, errors)`；内有 `type`/`transitionNo`/`records.size()`/状态机终态 |
| 同步流水线终态数据 | `SyncMessageProcessorService.java:107-161` | `process(type, transitionNo, xml, direction)` 返回 COMPLETED/FAILED 的 `MessageProcessRecord`；持 xml(request) |
| 既有 record 表 SQL 模板 | `db/migration/V38__create_invoice_verification_records.sql` | VARCHAR ENUM 列 + serial_no UNIQUE + created_at/updated_at（无 SQL DEFAULT，靠 `@PrePersist`） |
| Flyway 最大版本 | 全 4 活跃分支 `git ls-tree` 实测 | **V40**（V41/V42 空闲；**执行前须重 grep**，红线 `feedback_plan_flyway_v_collision_check` + 多会话并发） |
| PRD 列定义 | `docs/PRD/FEP_综合前置平台_PRD_v1.3.md:2008-2031` | 实时转发 7 列 / 非实时转发 8 列（见下方 §字段表，含 minOccurs/默认值） |
| ArchUnit 约束 | 既有 `InboundMessageProcessedEvent` Javadoc | fep-processor 禁依赖 fep-web；事件须停在 converter + java.* 表面 |

> **执行前重测铁律（红线 `feedback_baseline_drift_during_long_review_cycle` + `plan_flyway_v_collision_check` + `shared_m2_snapshot_cross_session_clobber`）：** T0 与每个建 migration 的 Task 起手须 `git fetch` + 重 grep 最大 V；3 个别会话 worktree 任一可能抢占 V41/V42。

---

## 1. PRD 字段表（§6.4.1 逐列实测，含透明偏差）

### 1.1 非实时业务转发记录表 `batch_forward_records`（PRD §6.4.1，line 2020-2031）

| PRD 字段 | 数据类型 | minOccurs | 默认值 | 表列（VARCHAR-ENUM 化）| 数据源 | 偏差/说明 |
|---|---|:--:|---|---|---|---|
| batch_forward_id | VARCHAR(32) | 1 | - | `batch_forward_id` PK | 系统生成 `IdGenerator.uuid32()` | - |
| batch_type | ENUM | 1 | 数据同步 | `batch_type VARCHAR(20)` | `MessageType` 派生（raw msgNo + category） | **语义 ENUM 映射 DEFERRED**（域专家，沿用 #96 verification_result 先例，零臆造）|
| total_record_count | INT | 1 | - | `total_record_count INT` | `BatchResult.total()` | - |
| success_record_count | INT | 1 | 0 | `success_record_count INT` | `BatchResult.success()` | - |
| process_start_time | TIMESTAMP | 1 | - | `process_start_time TIMESTAMP` | process 起始 `Instant` | - |
| process_end_time | TIMESTAMP | 0 | - | `process_end_time TIMESTAMP` nullable | process 终止 `Instant` | - |
| batch_status | ENUM | 1 | 待处理 | `batch_status VARCHAR(20)` | `failed==0 ? COMPLETED : FAILED`（状态机拒绝 → 处理异常）| 语义映射 DEFERRED，存原始状态名 |
| error_log_path | VARCHAR(200) | 0 | - | `error_log_path VARCHAR(200)` nullable | **无来源** | 透明 nullable DEFERRED（FEP 当前不落 batch 错误日志文件，零臆造）|
| — 非 PRD 扩展 — | | | | `serial_no VARCHAR(64)` UNIQUE | transitionNo | 幂等键，镜像 #96。**幂等语义说明**：批量 transitionNo 来源 = `msg.getHead().getMsgId()`（稳定）**或** `IdGenerator.uuid32()` 随机占位（`BatchMessageProcessorService.java:124-127`）。msgId 稳定时同提交重投 → upsert 去重更新；随机占位时每次新插（distinct forward 事件，语义正确）。**两情形均不违反 UNIQUE**（uuid32 唯一 / msgId 提交内唯一）。upsert 仅在 msgId 稳定路径生效。|
| — 非 PRD 扩展 — | | | | `created_at` / `updated_at` TIMESTAMP NOT NULL | `@PrePersist`/`@PreUpdate` | 审计，镜像 #96（V 无 DEFAULT）|

> **PRD 业务默认值偏差（batch_type=数据同步 / batch_status=待处理 / forward_status=转发成功）：** 这些列由事件数据**总会赋值**（NOT NULL），故 DDL 不设 SQL DEFAULT、`@PrePersist` 也只填审计列不填业务默认值。migration header 须透明声明「PRD 默认值不落地，因事件路径总赋值」。

### 1.2 实时业务转发记录表 `realtime_forward_records`（PRD §6.4.1，line 2008-2018）

| PRD 字段 | 数据类型 | minOccurs | 默认值 | 表列 | 数据源 | 偏差/说明 |
|---|---|:--:|---|---|---|---|
| forward_id | VARCHAR(32) | 1 | - | `forward_id` PK | `IdGenerator.uuid32()` | - |
| business_type | ENUM | 1 | - | `business_type VARCHAR(50)` | `MessageType` 派生 | 语义 ENUM DEFERRED，存 raw |
| request_content | TEXT | 1 | - | `request_content VARCHAR(255)` | **元信息 `msgNo:transitionNo`**（DECISION-5 推荐默认，非全报文） | PRD 称 JSON，本表存元信息字符串规避 PII 落库（透明偏差）；存全报文需另建脱敏管线 |
| response_content | TEXT | 0 | - | `response_content CLOB/TEXT` nullable | **无来源**（S1 上行网关未建，gated Q5）| **透明 nullable DEFERRED**（零臆造，#96 先例；S1 落地后回填）|
| forward_status | ENUM | 1 | 转发成功 | `forward_status VARCHAR(20)` | COMPLETED→成功 / FAILED→失败 | 「转发超时」无来源不产出，语义 DEFERRED 存原始状态名 |
| request_time | TIMESTAMP | 1 | - | `request_time TIMESTAMP` | process 起始 `Instant` | - |
| response_time | TIMESTAMP | 0 | - | `response_time TIMESTAMP` nullable | process 终止 `Instant` | - |
| — 非 PRD 扩展 — | | | | `serial_no` UNIQUE + `created_at`/`updated_at` | 同 §1.1 | 幂等 + 审计 |

> **DECISION-1（实时表 response_content 来源）— 推荐：透明 nullable DEFERRED。** 端到端「请求→转发→响应」的 response 捕获需 S1 上行业务受理网关（FR-API-BANK §7.1，实测**完全不存在**，gated Q5 接口契约权威）。强行编造 response = 臆造。沿用 #96「无来源字段透明 nullable」零臆造先例，S1 落地后由后续 Plan 回填。**待 muzhou 签字确认。**

---

## 2. 架构决策（muzhou 签字门）

**DECISION-2（实时转发 scope 谓词）⚑ muzhou 必答门 — 推荐：实时类 `MessageCategory` ∧ direction=OUT。** `SyncMessageProcessorService.process` 是 **private 且 inbound/outbound 共用**（`processInbound`/`processOutbound` 同入口，`SyncMessageProcessorService.java:101-110`）。仅按 `MessageCategory` 过滤会把**入站**实时报文也记成「转发」，而「实时业务**转发**」语义是对外发送（OUT）。故谓词须**双维度**：实时类 ∧ `direction=="OUT"`。publish 须在 process 内能拿到 direction 形参的位置（private process 已有 `direction`）。**具体「实时类」msgNo 集合待 muzhou 确认**（禁臆造哪些属实时转发；保守白名单 + append-only）。若 muzhou 认为实时表也应与 S1/Q5 同 gated，则 Phase 2 整体 deferred、仅 ship Phase 1。

**DECISION-3（事件粒度）— 推荐：两个独立 record 事件。** `BatchForwardProcessedEvent` + `SyncForwardProcessedEvent` 各自字段精确（批量带 counts，同步带 request/status），不强行合并。停留在 `fep-processor/.../event/` + converter + java.* 表面，保持 ArchUnit `processor 禁依赖 web`。

**DECISION-4（事务边界 + 失败降级）— TrackingService 方法级 `@Transactional` + listener try/catch 吞 + WARN。** 批量/同步流水线在 **fep-processor 内 publish**（无 web 事务上下文，区别于 #96 全经 `InboundMessageDispatcher` 的 web `@Transactional`）。故 fep-web 落库 Service 方法自带 `@Transactional`（独立事务，不依赖调用方事务存在），listener 同步派发时 **try/catch 吞异常 + WARN**——转发记录是旁路审计，落库失败**不应**回滚/毁业务流水线主链（与 #96「抛出回滚」相反，因转发记录非主链一致性要求）。
> ⚠️ **不可用 `@TransactionalEventListener(AFTER_COMMIT)`**：事件在 processor 内 publish 时无事务上下文，AFTER_COMMIT 监听器会**静默丢弃**（除非 fallbackExecution=true，但那又退化为无事务保护）→ 记录永不落库。此备选**已排除**，仅用 `@EventListener` + try/catch。**降级语义待 muzhou 确认。**

**DECISION-5（报文体落库范围 / 脱敏）⚑ — 推荐：request_content 仅存元信息（msgNo + transitionNo），不存全报文。** 实时表 `request_content`/`response_content` 若存整段报文体（TEXT/CLOB）可能含身份证/银行卡号/手机号（架构 §8.3 + §1208），直存明文违反脱敏红线。**实测约束（grep `fep-security-api/.../DesensitizeService.java`）：现有 `DesensitizeService`（接口在 `fep-security-api`，fep-web 已 always-on DI 装配，复用不触 ⛔`security/impl/`）仅 4 个字段级 mask（`maskIdCard`/`maskBankCard`/`maskPhone`/`maskUsci`），无整段 XML 报文体脱敏器**——「复用脱敏全报文」会依赖不存在的字段抽取逻辑，埋返工。故**推荐默认 = request_content 仅存 msgNo+transitionNo 元信息**（response_content 已 nullable DEFERRED），既满足 PRD「记录转发」语义又零 PII 落库、零新建脱敏器。**若 muzhou 坚持存全报文**，须另立子任务建报文体字段抽取+脱敏管线（更大 scope，不在本 Plan）。**待 muzhou 拍板存储范围。**

> 以上 DECISION-1~5 起草期给出推荐默认值；muzhou 签字时确认或调整。⚑ = 必答门。santa 评审须逐条核 PRD trace + 臆造风险。

---

## 3. 回归验收（两层，红线 `feedback_plan_regression_scope_explicit`）

- **minimum（本机，单模块）：** `cd E:/FEP_v1.0_wt-dzpz-record && export JAVA_HOME=... PATH=...; ./mvnw -pl fep-web -o test`（**不带 `-am`**，红线 `feedback_single_module_regression_no_am_flag`；上游 SNAPSHOT 缺则先一次性 `-am install -DskipTests`）。fep-processor 改动单独 `-pl fep-processor -o test`。**长跑 mvn 输出 redirect to file，禁 |tail**（红线 `feedback_pipe_tail_deadlock_with_bg_bash`）。load>100 杀本 worktree-slug 精确 fork（红线 single_module_regression）。
- **strong（GHA）：** PR 触发 ci.yml Build/Test/Quality + SonarCloud。**两张表的 `@SpringBootTest` 集成测试 + Flyway migrate 真跑须 GHA 验**（本机 surefire 仅 `*Test`，IT 命名注意）。billing 已恢复，常规 CI 门禁（红线 `feedback_systemic_ci_blocker_defers_positive_backing` 不再适用）。
- **spotbugs/ArchUnit：** 每建 entity/event/listener 的 Task 须 `spotbugs:check` + ArchUnit（红线 `feedback_subagent_must_run_spotbugs_check`）；`@ConfigurationProperties` 不涉及。Collection getter EI_EXPOSE 不涉及（无集合列）。新 logger 调用 wrap `LogSanitizer` + `@SuppressFBWarnings(CRLF_INJECTION_LOGS)`（红线 `feedback_logsanitizer_alone_insufficient_for_findsecbugs`）。

---

## Phase 1 — 非实时业务转发记录表（clean，高置信）

### Task 1: V41 migration + `BatchForwardRecordEntity` + Repository

**Files:**
- 重测最大 V，确认无冲突后 Create: `fep-web/src/main/resources/db/migration/V41__create_batch_forward_records.sql`
- Create: `fep-web/src/main/java/com/puchain/fep/web/integration/tracking/BatchForwardRecordEntity.java`
- Create: `fep-web/src/main/java/com/puchain/fep/web/integration/tracking/BatchForwardRecordRepository.java`
- Test: `fep-web/src/test/java/com/puchain/fep/web/integration/tracking/BatchForwardRecordRepositoryTest.java`

**Step 1: 重测 Flyway 版本**

Run: `git fetch origin && ls fep-web/src/main/resources/db/migration/ | grep -oE "V[0-9]+" | sort -t V -k2 -n | tail -1`
Expected: `V40`（若已被别会话占 V41，顺延 V43 并同步改本 Task 全部文件名 + 后续 V42→V44）。

**Step 2: 写 migration**（镜像 V38 header 透明偏差注释 + F 级合规声明）

```sql
-- §6.4.1 FR-DATA-DB-01: non-real-time (batch) business forward tracking table (PRD v1.3 §2020).
-- Supports H2 MODE=MySQL (test) and MySQL 8.0+ (prod).
--
-- Transparent deviations from PRD §2020:
--   1. batch_type / batch_status stored as VARCHAR holding raw derived values; semantic ENUM
--      mapping (合同备案/额度调整/... ; 待处理/处理中/...) DEFERRED to domain expert (mirror #96 DEF-B2-2).
--   2. error_log_path always NULL — FEP currently does not persist batch error-log files (zero fabrication).
--   3. serial_no / created_at / updated_at — non-PRD idempotency + audit columns (mirror V38).
--   4. PRD business defaults (batch_type=数据同步 / batch_status=待处理) NOT applied as SQL DEFAULT:
--      event path always assigns these NOT NULL columns, so no default is reachable.
-- F-level compliance: V1-V40 zero modification.
CREATE TABLE IF NOT EXISTS batch_forward_records (
    batch_forward_id     VARCHAR(32)  NOT NULL,
    batch_type           VARCHAR(20)  NOT NULL,
    total_record_count   INT          NOT NULL,
    success_record_count INT          NOT NULL,
    process_start_time   TIMESTAMP    NOT NULL,
    process_end_time     TIMESTAMP,
    batch_status         VARCHAR(20)  NOT NULL,
    error_log_path       VARCHAR(200),
    serial_no            VARCHAR(64)  NOT NULL,
    created_at           TIMESTAMP    NOT NULL,
    updated_at           TIMESTAMP    NOT NULL,
    CONSTRAINT pk_batch_forward PRIMARY KEY (batch_forward_id),
    CONSTRAINT uq_batch_forward_serial UNIQUE (serial_no)
);
CREATE INDEX IF NOT EXISTS idx_batch_forward_start ON batch_forward_records (process_start_time);
CREATE INDEX IF NOT EXISTS idx_batch_forward_type ON batch_forward_records (batch_type);
```

**Step 3: Entity**（逐字镜像 `InvoiceVerificationRecordEntity` 结构：`@Entity @Table` + `@Id` 业务键 + `@PrePersist/@PreUpdate` 填 created_at/updated_at + 全 getter/setter）。列：batchForwardId(PK,32) / batchType(20) / totalRecordCount(int) / successRecordCount(int) / processStartTime(LocalDateTime) / processEndTime(LocalDateTime,nullable) / batchStatus(20) / errorLogPath(200,nullable) / serialNo(64) / createdAt / updatedAt。

**Step 4: Repository**（镜像 `InvoiceVerificationRecordRepository`）

```java
public interface BatchForwardRecordRepository extends JpaRepository<BatchForwardRecordEntity, String> {
    Optional<BatchForwardRecordEntity> findBySerialNo(String serialNo);
}
```

**Step 5: 写失败测试**（`@DataJpaTest` 或 `@SpringBootTest`，镜像 `InvoiceVerificationRecordRepositoryTest`）：save → findBySerialNo 命中 + 字段回读一致 + `@PrePersist` 填 created_at/updated_at + UNIQUE(serial_no) 二次 save 同 serial 抛 / 或走 upsert（upsert 在 Service 测）。先确认 RED（表/类不存在编译失败 → 建后 GREEN）。

**Step 6: 跑测试 + spotbugs**

Run: `./mvnw -pl fep-web -o test -Dtest=BatchForwardRecordRepositoryTest` then `./mvnw -pl fep-web -o spotbugs:check`（须先 compile，红线 `feedback_spotbugs_check_needs_recompile_after_annotation`）
Expected: PASS + BugInstance 0。

**Step 7: Commit**

```bash
git add fep-web/src/main/resources/db/migration/V41__create_batch_forward_records.sql \
        fep-web/src/main/java/com/puchain/fep/web/integration/tracking/BatchForwardRecordEntity.java \
        fep-web/src/main/java/com/puchain/fep/web/integration/tracking/BatchForwardRecordRepository.java \
        fep-web/src/test/java/com/puchain/fep/web/integration/tracking/BatchForwardRecordRepositoryTest.java
git commit -m "feat(tracking): batch_forward_records V41 + entity + repository (§6.4.1 FR-DATA-DB-01)

AI-Generated: claude-code
Reviewed-By: pending"
```

### Task 2: `BatchForwardProcessedEvent` + 流水线 publish

**Files:**
- Create: `fep-processor/src/main/java/com/puchain/fep/processor/event/BatchForwardProcessedEvent.java`
- Modify: `fep-processor/.../pipeline/BatchMessageProcessorService.java`（注入 `ApplicationEventPublisher`，构造器 6→7 参；终态 publish）
- Test: `fep-processor/src/test/java/com/puchain/fep/processor/pipeline/BatchForwardEventPublishTest.java` + 既有 `BatchMessageProcessorServiceTest` 构造器同步

**Step 1: 事件 record**（镜像 `InboundMessageProcessedEvent` 紧凑构造 + 非空校验）

```java
public record BatchForwardProcessedEvent(
        MessageType type, String transitionNo,
        int total, int success, int failed,
        Instant startedAt, Instant finishedAt) {
    public BatchForwardProcessedEvent {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(transitionNo, "transitionNo");
        Objects.requireNonNull(startedAt, "startedAt");
        Objects.requireNonNull(finishedAt, "finishedAt");
    }
}
```

**Step 2: 写失败测试** — 用 Mockito `ApplicationEventPublisher` mock 注入 `BatchMessageProcessorService`，喂一条合法批量 `CfxMessage`，`verify(publisher).publishEvent(event)` 捕获并断言 total/success/failed/type/transitionNo 与 `BatchResult` 一致；空返回路径（msg null / type null / records empty）**不** publish。RED：构造器 arity 不匹配 + 无 publish。

**Step 3: 实装** — 构造器加 `final ApplicationEventPublisher eventPublisher`（`Objects.requireNonNull`）。在 `process()` 两个非空返回点前（Step 3 状态机块后的正常返回 + 状态机拒绝返回）publish `BatchForwardProcessedEvent`（startedAt=入口 `now`，finishedAt=`Instant.now()`，counts 来自局部 `records.size()/success/failed`）。**同 commit 改全部既有调用方/测试构造器**（红线 `feedback_commit_tree_self_consistent_per_commit`：签名变更 + 调用方同 commit）。grep `new BatchMessageProcessorService(` 列全调用点。

**Step 4: 跑 + spotbugs + ArchUnit**

Run: `./mvnw -pl fep-processor -o test -Dtest=BatchForwardEventPublishTest,BatchMessageProcessorServiceTest` + `./mvnw -pl fep-processor -o spotbugs:check`（compile 先行）
Expected: PASS + BugInstance 0 + ArchUnit（事件在 processor 表面，不引入 web 依赖）PASS。

**Step 5: Commit**（`AI-Generated: claude-code` + `Reviewed-By: pending`）

### Task 3: `BatchForwardTrackingService` + `BatchForwardEventListener` + 集成测试

**Files:**
- Create: `fep-web/.../tracking/service/BatchForwardTrackingService.java`（镜像 `InvoiceVerificationTrackingService`：`findBySerialNo.orElseGet(new)` upsert + 防御解析 + `LogSanitizer` + `@SuppressFBWarnings(CRLF_INJECTION_LOGS)` + **方法级 `@Transactional`** per DECISION-4）
- Create: `fep-web/.../tracking/listener/BatchForwardEventListener.java`（`@EventListener onProcessed(BatchForwardProcessedEvent)`；**try/catch 吞 + WARN 降级** per DECISION-4，旁路审计不毁主链）
- Test: `BatchForwardTrackingServiceTest`（upsert 幂等 + batch_status 派生 + error_log_path null）+ `BatchForwardEventListenerTest`（trigger 事件验证落库，**禁 `vm.method()` 直调**，红线 `feedback_unit_test_bypass` 类比；listener 异常吞验证）+ `@SpringBootTest BatchForwardTrackingIntegrationTest`（发 `BatchForwardProcessedEvent` → 真 JPA 落库 + Flyway V41 真 migrate）

**Step 1-4:** TDD：先写 listener+service 失败测试（RED）→ 实装 → GREEN。`batch_type` 派生：`type.msgNo()`（raw，语义 DEFERRED 注释）。`batch_status`：`failed==0 ? "COMPLETED" : "FAILED"`（存原始状态名，语义映射 DEFERRED）。`serial_no`=transitionNo，`batch_forward_id`=`deriveId(transitionNo)`（≤32 截断，镜像 #96 `deriveInvoiceId`）。

**Step 5:** `./mvnw -pl fep-web -o test -Dtest=BatchForward*` + spotbugs:check（compile 先行）→ PASS + BugInstance 0。`@SpringBootTest` IT 本机可能跳过（GHA 验）。

**Step 6: Commit**（自洽：service+listener+测试同 commit）。

---

## Phase 2 — 实时业务转发记录表（含 DECISION-1/2 门，中置信）

### Task 4: V42 migration + `RealtimeForwardRecordEntity` + Repository

镜像 Task 1。表 `realtime_forward_records`：forward_id(PK,32) / business_type(50) / request_content(VARCHAR 255，存元信息 `msgNo:transitionNo`，DECISION-5) / response_content(CLOB,nullable) / forward_status(20) / request_time(TIMESTAMP) / response_time(TIMESTAMP,nullable) / serial_no(64,UNIQUE) / created_at / updated_at。migration header 注明透明偏差：request_content 存元信息非全报文（PRD 称 JSON 全文，规避 PII，DECISION-5）+ response_content nullable DEFERRED（S1 网关未建，DECISION-1）+ business_type/forward_status 语义 ENUM DEFERRED + PRD 默认值不落地（事件总赋值）。**重测最大 V（V41 已被 Task1 占 → 本 Task V42）。** TDD repo 测试 + spotbugs。Commit。

### Task 5: `SyncForwardProcessedEvent` + 同步流水线 publish（scoped per DECISION-2）

镜像 Task 2。事件 `SyncForwardProcessedEvent(type, transitionNo, requestXml(String), status(String), startedAt, finishedAt)`。在 `SyncMessageProcessorService.process` 终态（COMPLETED/FAILED 返回前）publish——**但仅当 `isRealtimeForward(type) && "OUT".equals(direction)`**（DECISION-2 双维度谓词，muzhou 签字确定「实时类」集合；起草默认 = `MessageCategory` 实时类 ∧ direction=OUT，保守白名单 append-only）。注入 `ApplicationEventPublisher`（构造器 4→5 参）。**同 commit 改全部 `new SyncMessageProcessorService(` 调用方**——实测调用方**全为测试类**（`SyncMessageProcessorServiceTest` / `SyncMessageProcessorServiceIntegrationTest`）；生产 `InboundMessageDispatcher`/`OutboundCfxEnvelopeBuilder` 经 Spring DI 字段注入**非 new，不需改**。执行时 grep `new SyncMessageProcessorService(` 复核列全。requestXml 经 DECISION-5 脱敏后 = sanitized payload。status = 终态 record.getStatus().name()。TDD（mock publisher verify：实时类 OUT 报文 publish / inbound 同类不 publish / 非实时类不 publish）+ spotbugs + ArchUnit + commit tree 自洽。

### Task 6: `RealtimeForwardTrackingService` + `RealtimeForwardEventListener` + 集成测试

镜像 Task 3。`forward_status`：COMPLETED→`"转发成功"` / FAILED→`"转发失败"`（语义 DEFERRED 注释，「转发超时」无来源不产出）。`business_type`=`type.msgNo()`（raw DEFERRED）。`response_content`=**null**（DECISION-1 透明 DEFERRED）。`response_time`=finishedAt。`request_content`=**元信息字符串 `msgNo+":"+transitionNo`**（DECISION-5 推荐默认，零 PII；若 muzhou 改为存全报文则须先建报文体脱敏管线，本 Plan 范围外）。方法级 `@Transactional` + listener try/catch 降级。TDD（断言 request_content 不含明文报文体/PII）+ `@SpringBootTest` IT（GHA 验）+ spotbugs + commit。

---

## Phase 3 — closing

### Task 7: 矩阵更新 + worktree teardown + session-end 衔接

- **修矩阵 drift（红线 `feedback_prd_matrix_status_drift`）：** `docs/plans/prd-traceability-matrix.md` FR-DATA-DB-01 行：标注 §6.4.1 7 表 = 5 已建 + 本 Plan 补 2 转发表 → 评估升 🟡→🟢/✅（保留 error_log_path/response_content DEFERRED 注记）。**顺手纠正 §6.4.1 幽灵标签**：「电子凭证记录表」非 PRD 表，应改述为 §6.4 DB-01 泛指；FR-INFRA-CALLBACK-* 多行「PR #27/#95 待 merge」已 MERGED 应改 ✅（本会话实测发现）。
- **NO code commit 的 doc Task** — 仅 docs 改动，commit message 不含 production class（红线 `feedback_plan_step3_commit_template_residue`）。
- worktree teardown：`git worktree remove E:/FEP_v1.0_wt-dzpz-record`（session-end Phase 1 列残留状态）。
- session-end 跑完整 6-phase（红线 `feedback_session_end_full_workflow_required`）：Simplify 三审 + 9 维技术文档 + Daily Report + push；FEP 文档库 Phase 7。

---

## 4. Task 依赖序 + commit 切点

```
T1(V41+entity+repo) → T2(event+publish) → T3(service+listener+IT)   [Phase 1 非实时，可独立 ship]
T4(V42+entity+repo) → T5(event+publish) → T6(service+listener+IT)   [Phase 2 实时，依赖 DECISION-1/2 签字]
T7(矩阵+closing)
```
每 commit 独立可编译（红线 `feedback_commit_tree_self_consistent_per_commit`）：签名变更（T2/T5 构造器扩参）与全部调用方同 commit。Phase 1 与 Phase 2 解耦，可分两 PR（非实时先行降低评审面）。

## 5. FR 追溯

- **FR-DATA-DB-01**（PRD §6.4）→ 本 Plan T1-T6 补齐 §6.4.1 最后两张业务跟踪表（实时 + 非实时业务转发记录表）。
- 关联：#96（PR `399606e`）已建 invoice/financing/corporate 三表 + 既有 reconciliation/clearing 两表 = §6.4.1 前 5 表。

## 6. 风险与开放项（santa 评审重点）

1. **DECISION-1~5 待 muzhou 签字**（response_content nullable / 实时 scope 谓词⚑ / 事件粒度 / listener 降级语义 / 报文体脱敏⚑）——santa 须逐条核臆造风险 + PRD trace；⚑ 两项为必答门。
2. **实时表 scope（DECISION-2）是最大不确定**：哪些 msgNo 属「实时业务转发」无 PRD 明文清单 → 双维度谓词（实时类 ∧ direction=OUT）+ 保守白名单 + append-only，禁机械全纳。若 muzhou 认为实时表也应 gated（同 S1/Q5），可只 ship Phase 1（非实时），Phase 2 deferred。
5. **报文体脱敏（DECISION-5）**：request_content 可能含 PII，落库须脱敏或仅存元信息，否则违反 §8.3——santa 已升级该项，实施 T6 须含脱敏断言。
3. **事件 publish 改流水线热路径**（T2/T5 改 `BatchMessageProcessorService`/`SyncMessageProcessorService` 构造器）——须全调用方同 commit + 全模块回归（红线 `feedback_full_regression_before_commit`）。
4. Flyway V41/V42 多会话抢占风险——每建 migration Task 起手重 grep。
```
