# FEP EFF-S5-1 实施计划 — 审计链校验 checkpoint 增量化（超时面修复 + 截断攻击外锚加固）

> **版本:** v0.2（起草 2026-06-12；Round 1 santa REVISE[2B+6C] + 密码学 PASS-W-C[1C+3m] → 修订全闭合；Round 2 spot-check PASS WITH NOTES 微修闭合；muzhou ✅ 签字 2026-06-12）
>
> **v0.2 修订记录：**
> - **B1（santa·红线 98 自身违反）**: T2 的 checkpoint 推进是新增持久副作用而清理排 T4 → T2 commit 点跨类污染 RED。修：T2 测试步骤显式含 checkpoint 清理（VerifierTest setUp/cleanUp + ControllerTest intact 用例 + 逐 incremental 用例复位）；T4 池② 改"收编 T2 已有清理入 resetChain"；AC1 措辞改"6 场景断言不变，调用处改 `verifyChain(VerifyMode.FULL)`"
> - **B2（santa·数据点）**: 文件表修正 = 新建 5 + 修改 11（+SysOperationLogRepository 行 + ArchUnit 行改"修改"[ALLOWED +2]）共 16 行（17 文件——池⑤ 行含 2 文件）；claim 清单第 5 条同步
> - **crypto C-1（联合攻击披露 + boil-lake gauge）**: 抉择③⑥ 补披露"删尾可检前提 = 攻击者无法删除/回填旧 checkpoint；回退/删锚+删尾联合攻击使检测退回 S5 基线（非低于），由监控外锚兜底"；advanceCheckpoint 同步 set 新 gauge `fep_audit_chain_checkpoint_seq`（Verifier 注入 MeterRegistry，依赖 3→4）
> - **C1（boil-lake）**: checkpoint 缺失退化 full 加 1 行 WARN（纯常量文案零 taint）
> - **C2（boil-lake）**: 首建竞争双 INSERT SINGLETON → advanceCheckpoint catch DataIntegrityViolationException → WARN + 返回校验结果不推进（下轮自愈，~3 LOC）
> - **C3（crypto 裁定采纳）**: unknown sign_key_id → CHECKPOINT_INVALID 保持 + Javadoc 披露轮换语义（退役 keyId 旧锚报 CHECKPOINT_INVALID；下次 intact 推进以活跃密钥重签自愈）
> - **C4**: 旧分页方法 `findBySeqIsNotNullOrderBySeqAsc` **保留**（测试侧消费实存，白名单不减）
> - **C5**: 零新增行 → intact(0) 不重签不推进；AC4 矛盾统一 "DELETE seq IN (4,5) 使尾=3 < 5"
> - **C6**: T2 commit message 明示默认行为切 INCREMENTAL
> - **crypto minor**: TRUNCATION Javadoc 注明含锚行缺失 + 备份恢复=正确数据丢失证据；verifyEntry @param 一并放宽；T2 验收 6 注明直构真 SignServiceImpl harness + 入 claim 清单；避让项归属修正 "S5 池 Reuse①④"
> **执行方式:** hybrid（红线 `feedback_harness_bg_detach_hybrid_default`）— 主对话实施 + 前台 mvn（显式 JAVA_HOME + redirect-to-file）+ commit；subagent 仅评审。**红线 98 `feedback_commit_tree_self_consistent_per_commit`：每 commit 点独立可编译，接口/record/签名变更与全部调用方同 commit。**

**目标:** 修复 S5 Simplify Efficiency 实质 Deferred **EFF-S5-1**：`AuditChainVerifier.verifyChain` 全链 O(n)×~1-3ms/行（SM2 验签主导）→ 10 万行链（~1-3 月管理操作量）/integrity 端点 ~3-5 min 必超 HTTP 超时。方案 = **checkpoint 增量校验**（V37 单行锚表 + 锚行一致性校验 + intact 才推进）+ **TRUNCATION 检测**（链尾 < checkpoint = 删尾证据——S5 抉择⑩ 残余风险的实质缓解，外锚从 gauge 升级为持久化+签名锚）+ full 模式保留为权威 + 阈值披露。顺路 drain **S5 Simplify 池 test 域 6 项**（池子既定触发条件 = 下个触碰 fep-web 测试的 PR，即本 PR）。

**前置依赖:** S5 ✅（PR #82 `52ce5fe7`——Verifier/Writer/checkpoint 所依赖的链与签名设施全部就绪）。baseline origin/main = `52ce5fe7`（2026-06-12 起草实测）。

**执行 Worktree:** `/Users/muzhou/FEP_v1.0_wt-eff-s5-1`（分支 `feat/eff-s5-1-audit-checkpoint`，触发条件第 ⑤⑥ 项：>5min verify / **3 路别会话并发实测**——`wt-rule-batch2`+`wt-s1-followup`(均活跃 mvn)+`wt-simplify-q-drain`；起草时点 load 406 爆表，实施期本地验证须先 `pgrep MavenWrapperMain` 错峰，必要时 GHA 委托[六连先例]）
> worktree 建立后第一步：签字 Plan 入库 commit（既定范式）

**架构:** 全部改动在 fep-web `sysmgmt.log.audit` 域（+V37）。checkpoint = 单行表（固定 id）持 `verified_until_seq + anchor_hash + checkpoint_signature`；签名经既有 `AuditIntegrityService`（**域分隔输入** `"audit-checkpoint:" + seq + ":" + anchorHash`——与行签名输入空间不相交，防"复制行签名伪造 checkpoint"）。incremental 模式：验 checkpoint 签名 → 验锚行在场且 hash 匹配 → 从锚行续链至尾 → 全过推进 checkpoint；full = 既有全链行为 + intact 同样推进。

**技术栈:** Java 17 / Spring Boot 3.x / Flyway V37（见 ⚠️V 编号风险）

**⚠️ V 编号竞争（红线 plan_flyway_v_collision_check 强化）:** 起草实测 max=V36，但 **wt-s1-followup 别会话并发活跃**（S1 sweep，可能建 V37）→ 签字时点 + T0 实施时点**双重重 grep**，被占则全 Plan V37→V38 顺延（机械替换 + 评审注记，不需重评审）。

**AI 协同模式:**

| 模式 | AI 占比 | 本计划中的应用 |
|:----:|:-------:|--------------|
| A | 90% | T4 Simplify drain / T5 收尾 |
| B | 70% | T1-T3 checkpoint 机制（🔓 审计完整性域，密码学专项 review 强制） |
| E | 0%  | （无 ⛔ Task） |

---

## 设计背景（实测依据 @ `52ce5fe7`——S5 代码本会话 ship，引用即实测）

### 量级依据（S5 Simplify Efficiency 审实测论证）

- verifyChain 每行成本 = SM3 重算（µs 级）+ **SM2 验签 ~1-3 ms（2 次 EC 点乘，>95% 主导）**；分页查询非主导。
- 链增长：管理 Web 人类操作 ~1-5k 行/天 → **10 万行约 1-3 个月** → full 校验 ~100-300 s，同步 HTTP 端点（`GET /api/v1/sys/logs/integrity`）必超网关 30s 超时——超时面真实非理论。
- 增量窗口：日检频率下 Δ ~1-5k 行 → incremental ~1-15 s，可同步返回。

### 现状基线（S5 ship 终态，关键签名）

| 资产 | 现状 |
|------|------|
| `AuditChainVerifier`（fep-web sysmgmt.log.audit）| `verifyChain()` 无参全链：GENESIS 起 seq 连续/prev 链接/hash 重算/恒验签，500/批 `findBySeqIsNotNullOrderBySeqAsc(Pageable)`，首断点即停；`BreakType{GAP,PREV_LINK,HASH_MISMATCH,SIGNATURE_INVALID,UNKNOWN_KEY}` 嵌套枚举 |
| `ChainVerifyResult` | record(totalChecked, intact, firstBreakSeq, breakType) + 静态工厂 intact/broken |
| `AuditIntegrityService` | `signEntryHash(hashHex)`（requireHash 仅 null 检，入参无格式约束——域分隔串可传）/ `verifyEntry(hashHex, signature, keyId)` / `auditKeyId()` |
| `AuditChainWriter` | synchronized 单写者 + gauge `fep_audit_chain_tail_seq`（易失外锚——本 Plan 升级为持久化锚） |
| Controller | `GET /api/v1/sys/logs/integrity` 无参，@OperationLog 自身入链 |
| Flyway max | **V36**（双时点重 grep 见 ⚠️） |
| S5 Simplify 池（test 域 6 项待 drain） | ①PREV_LINK 断点无直达用例 ②链清理 3 处重复 + VerifierTest @AfterEach 后 context bean 链尾 stale（flake 面）③AspectTest 内联 FQN+类头 doc-rot+canBeConstructed 弱断言折叠 ④VerifierTest:158 "64 字节全 'A'" 注释 fact-fix ⑤TestKeyServiceConfiguration/FacadeTest 130-hex 收编 MockKeyService 常量（fep-web→mock compile scope 实测可见）⑥Writer/Verifier 测试 setUp 重复 → `AuditIntegrityTestSupport` util（SignService 参数化） |

### 设计抉择（评审重点）

| # | 抉择 | 理由 |
|---|------|------|
| ① | checkpoint = **单行表** `audit_chain_checkpoint`（`id` 固定 `'SINGLETON'`，verified_until_seq/anchor_hash/checkpoint_signature/sign_key_id/verified_at），不留历史 | 锚语义只需最新；历史轨迹由既有 gauge+INFO 日志承载；多行历史 = 无消费方的 YAGNI |
| ② | **incremental 流程**：(a) checkpoint 不存在 → 退化 full 语义（从 GENESIS）(b) 验 checkpoint 签名（域分隔输入，失败 → `CHECKPOINT_INVALID`）(c) 链尾 < verified_until_seq → `TRUNCATION` (d) 读锚行（seq=verified_until_seq）：缺失 → `TRUNCATION`；hash ≠ anchor_hash → `HASH_MISMATCH`@锚 seq (e) 从锚行之后续链校验至尾（expectedPrev = anchor_hash）(f) 全过 → intact + 推进 checkpoint 至本次链尾 | 锚一致性三查（签名/在场/hash）使 checkpoint 不可伪造前移、删尾可检 |
| ③ | **TRUNCATION 新 breakType**（②c/d，**含锚行缺失**——中段删除致锚不在场同报，Javadoc 注明；备份恢复触发 = 正确的数据丢失证据非误报）。**前提披露（crypto C-1）**：删尾可检前提 = 攻击者无法删除或回填旧 checkpoint；**回退/删锚+删尾联合攻击**使检测退回 S5 基线（非低于），由双 gauge 外锚兜底（既有 `fep_audit_chain_tail_seq` + 新增 `fep_audit_chain_checkpoint_seq`，推进时 set——任一回退即告警线索）。连带 `CHECKPOINT_INVALID` 共 +2 枚举 | §1219 实质加固 + 残余诚实披露 |
| ④ | **checkpoint 签名复用 `signEntryHash`/`verifyEntry`，输入 = 域分隔串** `"audit-checkpoint:" + verifiedUntilSeq + ":" + anchorHash`（UTF-8）——与行签名输入（64 hex）空间不相交 → 攻击者无法复制链行 signature 列伪造 checkpoint；sign_key_id 落列支持轮换；mock 域占位签名 + MockSignService 恒 true（同 S5 哲学，恒验签无值判定旁路）；`signEntryHash`/`verifyEntry` 的 hashHex 入参语义放宽在 Javadoc 一并注明（"hashHex 或域分隔锚串"）；**轮换语义（C3）**：退役 sign_key_id 旧锚验签 → CHECKPOINT_INVALID（取证粒度可接受，Javadoc 披露）；下次 intact 推进以活跃密钥重签自愈 | 零接口扩展；签名内容含 seq 绑定位置 + hash 绑定内容 |
| ⑤ | **推进时机**：intact 才推进（至本次校验末行）；broken 不推进；零新增行不重签不推进（C5）；无锁——并发推进 = 真三元组间幂等覆盖（最坏回退到另一真锚，性能退化下轮自愈）；**首建竞争**（双 verify 双 INSERT SINGLETON）→ advanceCheckpoint catch DataIntegrityViolationException → WARN + 照常返回结果不推进（C2） | 简单正确 + 失败面收口 |
| ⑥ | **full 仍权威 + 信任边界披露**：incremental 跳过已验段且**不检测已验段内事后篡改**（OpenAPI 显式句）；checkpoint 行被删 → 退化 full + **WARN 一条**（C1——同库攻击者可删锚使退化静默，WARN+gauge 双信号抬高门槛，外锚价值非完备删尾检测）；运维建议周期 full（披露 full O(n)×~1-3ms/行 + 链长 ≤1 万行同步 full，超出低频窗口） | checkpoint = 性能优化 + 删尾锚，不替代 full 权威 |
| ⑦ | **校验中并发 append 容忍**：verify 无事务，分页迭代天然捡到尾部新增行（append-only 尾插），推进以"实际校验末行"为准——无快照一致性需求 | 单写者 + append-only 结构保证 |
| ⑧ | `ChainVerifyResult` +2 字段（`mode`, `checkpointSeq`）= record 组件追加 → **静态工厂签名同步扩 + 全部调用方同 commit**（红线 98）；端点 JSON 向后兼容（字段追加） | 可观测性：响应自述本次模式与推进后锚位 |
| ⑨ | S5 Simplify 池 **test 域 6 项**顺路 drain（独立 commit）；**不动** S5 池 Reuse①④（DOMAIN 合并/LoginKeyPair 改名——security 模块评审成本）与 S2a 池 R4（避让 wt-s1-followup 并发会话 credential 域） | 池子既定触发条件命中；域避让红线 parallel_session_task_allocation |

### 共享工具类清单

| 工具类 | 包 | 关键方法 | 提供者 Task | 消费者 Task |
|---|---|---|---|---|
| `AuditChainCheckpointRepository` | web.sysmgmt.log.audit | findById/save | T1 | T2 |
| `AuditIntegrityTestSupport`（test util，池⑥） | web.sysmgmt.log.audit（test） | GBT 密钥常量 + `newIntegrityService(SignService)` + `resetChain(JdbcTemplate, AuditChainWriter...)`（池②） | T4 | T4（Writer/Verifier/Controller 测试改造） |

### AuditChainVerifier 职责边界（修订）

**负责**: 链校验（full/incremental）+ checkpoint 锚管理（读/验/推进）
**不负责**: 链写入（→AuditChainWriter）/ canonical（→AuditCanonicalizer）/ 签名原语（→AuditIntegrityService）
**依赖**: 4（SysOperationLogRepository / AuditIntegrityService / AuditChainCheckpointRepository / MeterRegistry[checkpoint gauge 外锚]——上限 7 ✓）
**行数**: 现 ~120 → 预计 ~230（上限 300 ✓；超出则拆 CheckpointManager）

### 文件结构

| 文件路径 | 职责 | 操作 | AI 模式 |
|----------|------|------|:-------:|
| `fep-web/.../db/migration/V37__audit_chain_checkpoint.sql` | 单行锚表 | 新建 | B |
| `fep-web/.../sysmgmt/log/audit/AuditChainCheckpoint.java` | 锚 entity | 新建 | B |
| `fep-web/.../sysmgmt/log/audit/AuditChainCheckpointRepository.java` | 锚仓储 | 新建 | B |
| `fep-web/.../sysmgmt/log/audit/AuditChainVerifier.java` | +incremental/TRUNCATION/CHECKPOINT_INVALID/推进 | 修改 | B |
| `fep-web/.../sysmgmt/log/audit/ChainVerifyResult.java` | +mode/checkpointSeq | 修改 | B |
| `fep-web/.../sysmgmt/log/controller/SysOperationLogController.java` | +?mode 参数 + OpenAPI 阈值披露 | 修改 | B |
| `fep-security-api/.../AuditIntegrityService.java` | signEntryHash Javadoc 入参语义放宽注明（抉择④，仅 doc） | 修改 | A |
| `fep-web/src/test/.../audit/AuditChainCheckpointTest.java` | V37+entity 往返/单行约束 | 新建 | B |
| `fep-web/src/test/.../audit/AuditChainVerifierTest.java` | +incremental 6 场景（见 T2）+ 池①④ drain | 修改 | B |
| `fep-web/src/test/.../audit/AuditChainWriterTest.java` | 池⑥ setUp util 化 | 修改 | A |
| `fep-web/src/test/.../audit/AuditIntegrityTestSupport.java` | 池②⑥ test util | 新建 | A |
| `fep-web/src/test/.../aspect/OperationLogAspectTest.java` | 池③ | 修改 | A |
| `fep-web/src/test/.../config/TestKeyServiceConfiguration.java` + `.../crypto/CallbackCredentialEncryptionFacadeTest.java` | 池⑤ hex 收编 | 修改 | A |
| `fep-web/src/test/.../controller/SysOperationLogControllerTest.java` | mode 参数用例 + 池② resetChain 接入 | 修改 | B |
| `fep-web/.../sysmgmt/log/repository/SysOperationLogRepository.java` | +findBySeq/findBySeqGreaterThanEqualOrderBySeqAsc（旧分页方法保留——测试侧消费实存，C4） | 修改 | B |
| `fep-web/src/test/.../architecture/OperationLogAppendOnlyArchTest.java` | ALLOWED 白名单 +2（与 repo 新方法名逐字一致；checkpoint repo 不受该规则约束） | 修改 | B |

> LOC 预估 ~550（生产 ~250 + 测试 ~300）>400 → PR-Size muzhou 豁免先例（六连）。

---

## Task 0: Worktree + Plan 入库 `模式 A`

- [ ] **Step 1**: 4 时点重测 + **V 编号重 grep**：`cd /Users/muzhou/FEP_v1.0 && git fetch && git rev-parse main origin/main && git worktree list && (pgrep -fl MavenWrapperMain || echo no-mvn) && ls fep-web/src/main/resources/db/migration/ | sort -V | tail -2`（V36 仍 max → V37 可用；被 wt-s1-followup 占则全 Plan V37→V38 顺延）
- [ ] **Step 2**: `git worktree add -b feat/eff-s5-1-audit-checkpoint /Users/muzhou/FEP_v1.0_wt-eff-s5-1 origin/main`
- [ ] **Step 3**: 签字 Plan 复制入 `docs/plans/` + commit（`docs(plans): add signed EFF-S5-1 plan` + AI-Generated + Reviewed-By: muzhou）

---

## Task 1: V37 锚表 + entity/repository `模式 B`

**PRD 依据:** 架构 §1219 不可篡改（锚加固）+ §8.3
**追溯 ID:** FR-INFRA-GM-AUDIT-INTEGRITY（增强，非新 FR——矩阵状态列追加注记）

**验收标准:**
1. V37 在 dev H2 迁移成功；表仅 1 行语义（PK 固定值 `'SINGLETON'`）
2. entity 往返：save → findById 读回逐列相等（verified_until_seq/anchor_hash 64hex/checkpoint_signature/sign_key_id/verified_at）
3. 更新语义：同 id 二次 save = 覆盖（JPA merge），无第二行

- [ ] **Step 1: V37**

```sql
-- V37__audit_chain_checkpoint.sql
-- EFF-S5-1: 审计链校验 checkpoint 锚（单行表；持久化+SM2 签名锚，S5 抉择⑩ 截断攻击缓解升级）
CREATE TABLE audit_chain_checkpoint (
    id                   VARCHAR(16)  NOT NULL COMMENT '固定 SINGLETON（单行锚）',
    verified_until_seq   BIGINT       NOT NULL COMMENT '已验证至链 seq（含）',
    anchor_hash          VARCHAR(64)  NOT NULL COMMENT '锚行 hash（t_sys_operation_log.seq=verified_until_seq 行）',
    checkpoint_signature VARCHAR(120) NOT NULL COMMENT 'SM2 裸签 Base64，输入=域分隔串 audit-checkpoint:<seq>:<hash>（mock 域占位串）',
    sign_key_id          VARCHAR(64)  NOT NULL COMMENT '签名时审计密钥版本',
    verified_at          TIMESTAMP    NOT NULL COMMENT '推进时刻',
    PRIMARY KEY (id)
) COMMENT '审计链校验 checkpoint（EFF-S5-1）';
```

- [ ] **Step 2: 失败测试**（`AuditChainCheckpointTest`，@SpringBootTest dev + @Transactional：往返逐列 + 覆盖语义 + `findById("SINGLETON")` 空表 empty）
- [ ] **Step 3: entity + repository 实现**

```java
// fep-web/src/main/java/com/puchain/fep/web/sysmgmt/log/audit/AuditChainCheckpoint.java
package com.puchain.fep.web.sysmgmt.log.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * 审计链校验 checkpoint 锚（单行，PK 固定 {@link #SINGLETON_ID}；EFF-S5-1）。
 *
 * <p>持久化 + SM2 签名锚：签名输入为域分隔串 {@code audit-checkpoint:<seq>:<hash>}
 * （与行签名输入空间不相交，防复制行签名伪造，Plan 抉择④）。删尾攻击经
 * "链尾 &lt; verified_until_seq → TRUNCATION" 可检（S5 抉择⑩ 升级）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Entity
@Table(name = "audit_chain_checkpoint")
public class AuditChainCheckpoint {

    /** 单行锚固定主键。 */
    public static final String SINGLETON_ID = "SINGLETON";

    @Id
    @Column(name = "id", length = 16)
    private String id = SINGLETON_ID;

    /** 已验证至链 seq（含）。 */
    @Column(name = "verified_until_seq", nullable = false)
    private Long verifiedUntilSeq;

    /** 锚行 hash。 */
    @Column(name = "anchor_hash", nullable = false, length = 64)
    private String anchorHash;

    /** checkpoint SM2 签名（域分隔输入）。 */
    @Column(name = "checkpoint_signature", nullable = false, length = 120)
    private String checkpointSignature;

    /** 签名时审计密钥版本。 */
    @Column(name = "sign_key_id", nullable = false, length = 64)
    private String signKeyId;

    /** 推进时刻。 */
    @Column(name = "verified_at", nullable = false)
    private LocalDateTime verifiedAt;

    // getter/setter 全 7 对（checkstyle allowMissingPropertyJavadoc 豁免下紧凑书写，
    // 实施按 S5 SysOperationLog 新增段同形态）
}
```

```java
// fep-web/src/main/java/com/puchain/fep/web/sysmgmt/log/audit/AuditChainCheckpointRepository.java
package com.puchain.fep.web.sysmgmt.log.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * checkpoint 锚仓储（单行：findById(SINGLETON_ID) + save 覆盖；EFF-S5-1）。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Repository
public interface AuditChainCheckpointRepository
        extends JpaRepository<AuditChainCheckpoint, String> {
}
```

- [ ] **Step 4**: RED→GREEN（`-Dtest=AuditChainCheckpointTest -pl fep-web -am` + surefire3 参数；log `/tmp/eff-s5-1-t1-{red,green}.log`；**先 pgrep 错峰**）+ `compile spotbugs:check -pl fep-web -am`
- [ ] **Step 5**: 提交 `feat(web): V37 audit chain checkpoint anchor table (EFF-S5-1 T1)` → spec+quality review subagent

---

## Task 2: Verifier 增量化 + TRUNCATION + 推进 `模式 B`

**PRD 依据:** 架构 §1219 + §8.1 性能要求（管理端点可用性）
**追溯 ID:** FR-INFRA-GM-AUDIT-INTEGRITY（增强）

**验收标准（含 checkpoint 攻防 6 场景）:**
1. `verifyChain(VerifyMode.FULL)` 行为 = S5 既有全链校验（**既有 6 场景断言不变**，调用处改 `verifyChain(VerifyMode.FULL)`）+ intact 时推进 checkpoint（新断言）；**T2 内测试隔离**（B1）：VerifierTest setUp/cleanUp 增 `DELETE FROM audit_chain_checkpoint` + ControllerTest intact 用例同步清 checkpoint + 逐 incremental 用例显式复位——checkpoint 持久副作用不得跨用例/跨类泄漏（shared-H2 红线）
2. `verifyChain(VerifyMode.INCREMENTAL)`：有 checkpoint 时仅校验锚后新增行（实测：3 行链推进后再 append 2 行 → incremental totalChecked==2 且 intact + checkpoint 推进至 5）
3. checkpoint 缺失 → incremental 退化 full 语义（totalChecked = 全链）
4. **TRUNCATION**：推进至 seq=5 后 `DELETE seq IN (4,5)` 使链尾=3 < 5 → incremental 报 TRUNCATION 且 firstBreakSeq=checkpoint seq
5. **锚失配**：UPDATE 锚行 hash 列 → incremental 报 HASH_MISMATCH@锚 seq；DELETE 锚行（保留其后行）→ TRUNCATION
6. **伪造前移拒绝**：手工 UPDATE checkpoint（verified_until_seq 前移 + anchor_hash 填真实行 hash + checkpoint_signature **复制该行 signature 列**）→ incremental 报 `CHECKPOINT_INVALID`（域分隔输入使行签名不可复用——抉择④ 攻防证明；**必须沿用 VerifierTest 既有直构真 SignServiceImpl harness**——mock context bean 验签恒 true 致本用例无效，crypto C-4）
7. broken 不推进（场景 4-6 后 checkpoint 行原值不变）
8. `ChainVerifyResult` +mode/checkpointSeq 字段，静态工厂同步扩，全部调用方同 commit 编译绿（红线 98）
9. 零新增行 → incremental intact(0) 且不重签不推进（C5）；checkpoint 缺失退化 full 时 WARN 一条（C1）；首建/推进 DIVE → WARN 不推进照常返回（C2）；推进成功后 gauge `fep_audit_chain_checkpoint_seq` = 新 verified_until_seq（SimpleMeterRegistry 断言）

**核心实现（verifyChain 重构骨架，完整逻辑）:**

```java
    /** 校验模式。 */
    public enum VerifyMode {
        /** 全链权威校验（O(n)，运维低频窗口使用）。 */
        FULL,
        /** checkpoint 增量校验（O(Δ)，默认）。 */
        INCREMENTAL
    }

    /** checkpoint 签名域分隔前缀（与行签名输入空间不相交，抉择④）。 */
    private static final String CHECKPOINT_SIGN_PREFIX = "audit-checkpoint:";

    /**
     * 链校验入口。
     *
     * @param mode FULL=GENESIS 起全链；INCREMENTAL=checkpoint 锚后增量（缺锚退化 FULL）
     * @return 校验结果（intact 时已推进 checkpoint）
     */
    public ChainVerifyResult verifyChain(final VerifyMode mode) {
        long startSeq = 1;
        String expectedPrev = AuditIntegrityService.GENESIS_PREV_HASH;
        Long checkpointSeq = null;
        if (mode == VerifyMode.INCREMENTAL) {
            final var cp = checkpointRepository.findById(AuditChainCheckpoint.SINGLETON_ID);
            if (cp.isPresent()) {
                final AuditChainCheckpoint anchor = cp.get();
                checkpointSeq = anchor.getVerifiedUntilSeq();
                final String signedPayload = CHECKPOINT_SIGN_PREFIX
                        + anchor.getVerifiedUntilSeq() + ":" + anchor.getAnchorHash();
                final boolean cpSigValid;
                try {
                    cpSigValid = auditIntegrityService.verifyEntry(
                            signedPayload, anchor.getCheckpointSignature(), anchor.getSignKeyId());
                } catch (final IllegalArgumentException e) {
                    return ChainVerifyResult.broken(0, anchor.getVerifiedUntilSeq(),
                            BreakType.CHECKPOINT_INVALID, mode, checkpointSeq);
                }
                if (!cpSigValid) {
                    return ChainVerifyResult.broken(0, anchor.getVerifiedUntilSeq(),
                            BreakType.CHECKPOINT_INVALID, mode, checkpointSeq);
                }
                final var tail = logRepository.findTopBySeqIsNotNullOrderBySeqDesc();
                if (tail.isEmpty() || tail.get().getSeq() < anchor.getVerifiedUntilSeq()) {
                    return ChainVerifyResult.broken(0, anchor.getVerifiedUntilSeq(),
                            BreakType.TRUNCATION, mode, checkpointSeq);
                }
                final var anchorRow = logRepository.findBySeq(anchor.getVerifiedUntilSeq());
                if (anchorRow.isEmpty()) {
                    return ChainVerifyResult.broken(0, anchor.getVerifiedUntilSeq(),
                            BreakType.TRUNCATION, mode, checkpointSeq);
                }
                if (!anchorRow.get().getHash().equals(anchor.getAnchorHash())) {
                    return ChainVerifyResult.broken(0, anchor.getVerifiedUntilSeq(),
                            BreakType.HASH_MISMATCH, mode, checkpointSeq);
                }
                startSeq = anchor.getVerifiedUntilSeq() + 1;
                expectedPrev = anchor.getAnchorHash();
            }
        }
        return scanChain(startSeq, expectedPrev, mode, checkpointSeq);
    }
```
> `scanChain` = 既有逐行循环抽出（seq 起点/expectedPrev 参数化，分页改 `findBySeqGreaterThanEqualOrderBySeqAsc(startSeq, Pageable)`——repository +2 方法 `findBySeq(Long)`/`findBySeqGreaterThanEqualOrderBySeqAsc`，**ArchUnit 白名单同步 +2**）；intact 末尾 `advanceCheckpoint(lastSeq, lastHash, mode...)`（构造域分隔串 → signEntryHash → save SINGLETON 行 → set gauge `fep_audit_chain_checkpoint_seq`（MeterRegistry，crypto C-1 外锚））；空链（startSeq 起无行且无既有锚）intact(0)。Controller 既有调用 `verifyChain()` → `verifyChain(VerifyMode.INCREMENTAL)`（T3 同 commit？——**T2 内先加默认重载 `verifyChain()` 委托 INCREMENTAL**，T3 仅 Controller 加参数——每 commit 自洽，红线 98）。

- [ ] Steps: 失败测试（9 条验收中 8 个测试场景；AC8 record 扩参为编译级非测试场景）→ RED → 实现 → GREEN + 既有 6 用例回归 + spotbugs + ArchUnit（log `/tmp/eff-s5-1-t2-*.log`）→ 提交 `feat(web): incremental chain verification with signed checkpoint anchor (EFF-S5-1 T2)`（message 注明 "/integrity 默认行为自本 commit 切 INCREMENTAL，T3 暴露 mode 参数"——C6）→ spec+quality review

---

## Task 3: 端点 mode 参数 + 阈值披露 `模式 B`

**验收标准:**
1. `GET /api/v1/sys/logs/integrity?mode=full|incremental`（默认 incremental，大小写不敏感，非法值 400）
2. OpenAPI @Operation description 披露：full O(n)×~1-3ms/行、建议链长 ≤1 万行同步 full、超出走低频窗口；incremental O(Δ)
3. 响应 JSON +mode/checkpointSeq 字段（向后兼容追加）；既有 intact 端到端用例改造为 incremental 默认下断言（+1 个 ?mode=full 用例）
4. ControllerTest 回归绿

- [ ] Steps: 失败测试 → RED → Controller 实现（`@RequestParam(defaultValue = "incremental") String mode` → enum 解析，IllegalArgumentException → 既有全局 400 处理）→ GREEN + spotbugs → 提交 `feat(web): integrity endpoint mode param + threshold disclosure (EFF-S5-1 T3)` → spec+quality review

---

## Task 4: S5 Simplify 池 test 域 drain `模式 A`

逐项（全 test 域，独立 commit）：
- [ ] **池⑥+②**: 新建 `AuditIntegrityTestSupport`（GBT 密钥常量 + `newIntegrityService(SignService)` + `resetChain(...)`[清链行+清 checkpoint+重锚 context bean]）；三测试接入——**收编 T2 已内联的 checkpoint 清理**（B1：清理 T2 先行保证该 commit 点绿，T4 仅重构归一不改语义）；Verifier @AfterEach 补 context bean 重锚修 stale 链尾 flake 面；删冗余 DELETE
- [ ] **池①**: VerifierTest +1 用例：UPDATE seq=2 行 prev_hash 列 → PREV_LINK（BreakType 枚举全覆盖闭合——注意 TRUNCATION/CHECKPOINT_INVALID 已由 T2 覆盖）
- [ ] **池④**: VerifierTest:158 注释 fact-fix（"64 零字节（Base64 后全 'A'）"）
- [ ] **池③**: AspectTest 内联 FQN→import + 类头 doc-rot 刷新 + 折叠 canBeConstructed 弱断言
- [ ] **池⑤**: TestKeyServiceConfiguration/FacadeTest 130-hex → `MockKeyService.MOCK_AUDIT_PUBLIC_KEY_HEX` 引用（+import）
- [ ] 验证：受影响测试全量 + spotbugs → 提交 `chore(web): drain S5 simplify deferred test-domain batch (EFF-S5-1 T4)` → quality review（单路，零行为微调批）

---

## Task 5: 回归 + 文档 + PR + closing `模式 A`

- **回归两层**（红线 plan_regression_scope_explicit）：strong = worktree 全 reactor verify（**先 pgrep 错峰**，load>50 或并发 mvn 在场 → GHA 委托[六连先例]）；minimum = `-pl fep-web -am verify`
- [ ] Step 1: strong 回归（或 GHA 委托实测定）
- [ ] Step 2: roadmap S5 行追加 checkpoint 注记 + S5 Simplify 池标记 drain 完成 + CLAUDE.md 状态（file write only）
- [ ] Step 3: push + `gh pr create`（mutation 网络错 read-verify 红线）；PR-Size 豁免先例注记
- [ ] Step 4: final whole-impl review（跨 Task：checkpoint 攻防语义/红线 98 逐 commit 自洽/池 drain 零行为）
- [ ] Step 5: merge 后 closing（fetch+ff main+worktree remove+分支删）

---

## 评审与签字流程

1. **santa 双审**（7 项清单 + 抉择②⑥ 信任边界 + 红线 98 commit 切点规划审）
2. **密码学专项**（域分隔签名输入的不可伪造性论证 / 锚三查完备性 / TRUNCATION 检测边界 / mock 恒验签无旁路延续）
3. **muzhou 签字**

### 评审员 claim grep 清单
- [ ] S5 基线引用与 `52ce5fe7` 实际代码一致（Verifier/ChainVerifyResult/repository 方法签名）
- [ ] V37 双时点重 grep 步骤在场（T0 + 签字时点）
- [ ] 红线 98：T2 默认重载策略使 Controller 调用在 T2/T3 各 commit 点自洽——逐 commit 推演
- [ ] ArchUnit 白名单 +2 方法与 repository 新方法名逐字一致
- [ ] 数据点自洽：Task 5 个（T0 除外 T1-T5）/ 文件结构表 16 行 = 新建 5 + 修改 11
- [ ] T2 验收 6 伪造拒绝用例使用直构真 SignServiceImpl harness（非 context mock bean，crypto C-4）

## 自检清单（起草自检 2026-06-12）

| # | 项 | 结果 |
|---|----|------|
| 1 | 需求覆盖 | EFF-S5-1 两子项（超时面+披露）+ 抉择⑩ 升级全覆盖；异步 job 明示不做（YAGNI，增量后单次 Δ 小） |
| 2 | 安全边界 | 审计完整性域 → 密码学专项强制；无 ⛔ E |
| 3 | 占位符 | T1 entity getter/setter "按 S5 同形态" = 既有先例引用非 TBD；T2 scanChain 抽取以骨架+完整语义描述给出（既有循环体 S5 已 ship 逐字可拼） |
| 4 | 类型一致性 | VerifyMode/CHECKPOINT_INVALID/TRUNCATION/工厂扩参跨 Task 一致 |
| 5 | 测试命令 | -Dtest 与新测试类匹配 + surefire3 参数 + 错峰 pgrep |
| 6 | CLAUDE.md | T5 Step 2 |
| 7 | 验收来源 | 量级数据 = S5 Efficiency 审实测论证；攻防场景 = 抉择②④⑥ 推导 |
| 8 | 共享工具 | AuditIntegrityTestSupport/CheckpointRepository 登记 |
| 9 | 职责边界 | AuditChainVerifier 修订（依赖 4 含 MeterRegistry / ~230 行 / 超限拆 CheckpointManager） |
| 10 | Worktree | 命中 ⑤⑥ → wt-eff-s5-1；closing 含 remove |

## 签字区

- [ ] santa Round 1: ____
- [ ] 密码学专项 review: ____
- [ ] **muzhou 批准**: ____（签字后方可执行）


---

## 批准签字

- **AI 独立评审**: santa Round 1 REVISE（2 BLOCKER + 6 CONCERN，基线七项实测全绿）+ 密码学专项 Round 1 PASS WITH CONCERNS（C-1 联合攻击披露 + gauge 外锚）→ v0.2 全闭合 → Round 2 spot-check PASS WITH NOTES（3 处文本微修已落盘，无需 Round 3）
- **Plan Approver**: muzhou ✅ **批准，立即执行**（2026-06-12，AskUserQuestion 第一项 Recommended）
- **执行方式**: hybrid（主对话实施 + 前台 mvn + commit；subagent 仅评审）@ wt-eff-s5-1
