# Callback Phase 2b — 凭证 + DLQ 重放 + IN_APP 告警 + Stale Reaper 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development`（推荐）或 `superpowers:executing-plans` 按 Task 实施。Steps 用 checkbox (`- [ ]`) 跟踪。
>
> **设计文档**: [`/Users/muzhou/FEP/docs/plans/2026-05-28-callback-phase2b-credential-dlq-alert-reaper-design.md`](../../../FEP/docs/plans/2026-05-28-callback-phase2b-credential-dlq-alert-reaper-design.md)（非 git tracked，brainstorming 产出）

**Goal**: 闭环 Callback 接口模式回调子系统 — 接通 TOKEN/OAuth2 凭证（SM4 加密）+ DLQ 复制重放（金融审计链）+ IN_APP 站内信告警（事件解耦）+ stale-SENDING 自动回收（防 runner crash 死锁）。

**Architecture**: 新增 4 子包 (`callback.credential` / `callback.dlq` / `callback.reaper` / 既有 `callback.*` 扩展)；凭证独立表 `callback_credential` FK→`sub_output_interface`；OAuth2 access_token Caffeine in-memory cache（TTL=expires_in-30s）；DLQ 复制重放 + `original_dlq_id` 审计链；`CallbackDeadLetterEvent` 事件解耦 → `InAppNotificationListener` 写 `in_app_notification` 表；`CallbackStaleReaper` @Scheduled 60s + 300s 窗口 revert PENDING + retryCount++；⛔ SM4 加密走 `fep-security-api/CryptoService`，`KeyService` 由 ③ 安全专家在 impl 新增 `getSm4CredentialMasterKey()`。

**Tech Stack**: Java 17 / Spring Boot 3.x / JPA 3 / Flyway / Caffeine (root pom 已锁) / `fep-security-api`（SM4/SM2 接口）/ JUnit 5 + Mockito + WireMock / ArchUnit / Vue3 + Element Plus + Vitest + Playwright.

---

## §0 元信息（治理）

| 项 | 值 |
|---|---|
| 作者 | Claude Code（mode A） |
| 起草日期 | 2026-05-28 |
| Plan version | v0.3（Round 2 NEEDS REVISION 修订: B5 PR 拓扑编译序 + N4 Checker Framework + N5 mock helper 注，待 muzhou 签字） |
| **执行 Worktree** | `/Users/muzhou/FEP_v1.0_wt-callback-p2b`（分支 `feat/callback-phase2b-credential-dlq-alert-reaper`，AI 后端 Task — 触发条件 ② + ③）+ `/Users/muzhou/FEP_v1.0_wt-callback-p2b-sec`（分支 `feat/callback-phase2b-security-credential-key`，⛔ Mode E ③ 安全专家独立 — 触发条件 ③） |
| **PR 拓扑**（M3 修订 + B5 修订 v0.3） | **双 PR + 编译序闸**: ① ⛔ Mode E PR 先 ship — `wt-callback-p2b-sec` 含 **T1（fep-security-api KeyService 接口加 `getSm4CredentialMasterKey` + SecurityApiConfiguration mock impl）+ T2（fep-security-impl KeyServiceImpl SM4 主密钥实现）**，独立 PR (`feat/callback-phase2b-security-credential-key`) muzhou + ③ 双签后 merge to main；② AI 后端 PR 后 ship — `wt-callback-p2b` rebase main（pull T1+T2 commit）ship **T3-T14+T16+T17**，主 PR (`feat/callback-phase2b-credential-dlq-alert-reaper`) merge to main；③ T15 (fep-admin-ui) 由独立子 Plan `2026-05-28-callback-phase2b-ui.md` 处理（第 3 个 PR）。**B5 修订理由**: T1 接口归 ⛔ Mode E PR 因 `getSm4CredentialMasterKey` 触及 CLAUDE.md "密钥管理" 禁区（接口设计 + 语义本身 ③ 安全专家拍板，AI 起草仅作 reviewer aid），且 T2 `@Override` 需要 T1 interface 在 baseline 上存在（避免独立 PR compile fail） |
| Baseline | **假设 PR #27 b26f4a8 (`feat/callback-module-phase2`) 已 merge** 到 origin/main；起草日 origin/main HEAD = `4fcac99`，Round 1 评审日 drift 到 `3300533`（R-NEW-1 follow-on XsdTestSupport.pad30 ship，与本 Plan 无冲突）；实施前必重 grep（红线 `feedback_baseline_drift_during_long_review_cycle`） |
| Flyway V_N | 预占 V30 / V31 / V32（起草日 main 最大 V28，PR #27 占 V29；实施前重 grep `ls FEP_v1.0/fep-web/src/main/resources/db/migration/`） |
| 依赖 | **强依赖**: PR #27 `b26f4a8` merge 到 origin/main（T8 ALTER + T10 RetryHandler publish event 都基于 PR #27 引入的 `retry_count/next_retry_at/claimed_at` + `markDeadLetter()` API） |
| FR-ID | FR-INFRA-CALLBACK-CREDENTIAL / FR-INFRA-CALLBACK-DLQ-REPLAY / FR-INFRA-CALLBACK-IN-APP-ALERT / FR-INFRA-CALLBACK-STALE-REAPER（prd-traceability-matrix 实施时 4 行新建） |
| AI 评审 | Round 1 (2026-05-28) NEEDS REVISION (BLOCKER × 4 + MAJOR × 4)；Round 2 待重派（修订 v0.2 后）（红线 `secondary-ai-review`） |
| muzhou 签字 | 一票否决（7 项 plan-review-checklist），Round 2 PASS 后签字 |
| ③ 安全专家 | T2 KeyServiceImpl 单独签字（`Security-Reviewed-By` footer，不含 `AI-Generated`） |
| 子 Plan | T15 fep-admin-ui 整段迁至 [`2026-05-28-callback-phase2b-ui.md`](2026-05-28-callback-phase2b-ui.md)（M2 修订：拆独立子 Plan 避 ≤400 行 PR 门禁爆裂） |

---

## §1 Scope Check + Grep 实测证据

### 1.1 Scope Check（主 Plan 16 Task + 子 Plan 1 Task）

**决策门 3 muzhou 选定**：1 主 Plan 后端 4 项打包 + 1 子 Plan UI + Plan 内显式标注 AI Task vs ③ 安全 Task 角色 + worktree 独立。

- AI 后端 Task 14 个（T3-T14, T16-T17）→ `wt-callback-p2b`（AI 主 PR）
- ⛔ Mode E Task 2 个（**T1 + T2**，v0.3 B5 修订）→ `wt-callback-p2b-sec`（⛔ 安全 PR）
  - **T1 ownership 修订**（v0.3 / B5）: 原 (AI / Mode B) → 改 (⛔ Mode E — ③ 安全专家拍板 + AI 起草作 reviewer aid)。理由：`KeyService.getSm4CredentialMasterKey()` 接口设计触及 CLAUDE.md "密钥管理" ⛔ 禁区，**API 语义本身**（密钥长度/生命周期/来源 HSM）须由 ③ 安全专家拍板；mock impl 16-byte 零向量虽 AI 可写但同属 SM4 主密钥 API 表面，归 ⛔ Mode E PR 处理避免 compile 序冲突 + ownership 一致
  - **T2 PR 分支重命名**（v0.3 / B5）: 原 `feat/callback-phase2b-keyservice-sm4` → 改 `feat/callback-phase2b-security-credential-key`（合并 T1+T2 scope 后命名更准）
- AI UI Task 1 个（T15）→ 整段迁至 [`2026-05-28-callback-phase2b-ui.md`](2026-05-28-callback-phase2b-ui.md) 子 Plan（M2 修订 / muzhou 2026-05-28 拍板）
  - 拆分原因：T15 含 7 Vue + 3 TS + 1 router = 11 文件 ~600-800 行，**确定爆 ≤400 行 PR 门禁**
  - 后端 PR + UI PR 独立 merge，互不阻塞；UI PR 依赖后端 controller API（实施时 UI 子 Plan 实施前后端 PR merge）
- **主 Plan Task 数自洽校验**（红线 `feedback_plan_template_data_point_self_consistency`）: §3 列出 17 个 `### Task T<N>` heading，其中 T15 已替换为占位 stub 指向子 Plan；主 Plan 实质 16 Task（T1-T14 + T16 + T17）+ 1 子 Plan 引用占位（T15 stub）；其中 T1+T2 在 ⛔ Mode E PR (`wt-callback-p2b-sec`)，T3-T14+T16+T17 在 AI 主 PR (`wt-callback-p2b`)

### 1.2 Grep 实测证据（起草日 2026-05-28，实施前重测）

> **实测来源**（M1 修订）: 行 49-53 的 PR #27 字段实测自 `wt-callback-p2` worktree（PR #27 `b26f4a8` 未 merge 到 origin/main）；PR #27 squash/rebase merge 后 commit SHA 会变，**实施前必比对 V29 / `markDeadLetter` / `claimed_at` 字段在 main 上一致**（见 §8 Handoff）。其他行实测来源为 origin/main / fep-security-api / fep-common 本身（与 PR #27 无关）。

| 实测项 | 命中 | 实测命令 / 实测来源 |
|---|---|---|
| origin/main HEAD（起草日） | `4fcac99` | `git rev-parse --short origin/main` |
| origin/main HEAD（Round 1 评审日） | `3300533`（drift +1 = R-NEW-1 follow-on XsdTestSupport.pad30，与本 Plan 无冲突） | 同上 |
| PR #27 HEAD | `b26f4a8` (`feat/callback-module-phase2`) — **未 merge** | `cd wt-callback-p2 && git log --oneline -1` |
| `callback_queue` 表名（**无 `t_` 前缀**，新业务表 convention） | `CallbackQueueEntity.java:22` | wt-callback-p2: `grep '@Table' fep-web/src/main/java/com/puchain/fep/web/callback/domain/*.java` |
| **`t_sub_output_interface` 表名（带 `t_` 前缀，旧业务表）**（B1 修订） | `SubOutputInterface.java` `@Table(name = "t_sub_output_interface")` | origin/main: `grep '@Table.*output_interface' fep-web/src/main/java/com/puchain/fep/web/submission/outputinterface/domain/*.java` |
| `CallbackQueueStatus` 终态 + 类型 | **`public final class` + `public static final String PENDING / DEAD_LETTER`**（非 enum） | wt-callback-p2: `cat CallbackQueueStatus.java` |
| PK column = `queue_id` | `CallbackQueueEntity.java:31` | wt-callback-p2: `grep '@Id\|@Column' CallbackQueueEntity.java` |
| 既有字段（PR #27 后）| `queue_id / idempotency_key / target_interface_id / msg_no / payload_json / status / last_error / retry_count / next_retry_at / claimed_at / create_time / update_time` | wt-callback-p2: `cat CallbackQueueEntity.java` |
| `markDeadLetter(int newRetryCount, String error)` 方法 | `CallbackRetryHandler.java:96, 109` 调用 | wt-callback-p2: `grep markDeadLetter` |
| `CallbackDeadLetterHandler` 类 | **不存在** | `grep -rn 'CallbackDeadLetterHandler'` 0 命中 |
| 新业务表命名 convention | 无 `t_` 前缀（callback / collection / integration / reconciliation）；**旧业务表保留 `t_` 前缀（sys_* / t_sub_*）** | `grep @Table fep-web/src/main` |
| `CryptoService` SM4 接口 | `fep-security-api/.../CryptoService.java:13` (encrypt/decrypt byte[]+key) | `Read CryptoService.java` |
| `KeyService` SM4 业务 key | **不存在**（仅 SM2 login/sign） | `Read KeyService.java` |
| `InterfaceAuthType` 枚举 | TOKEN / OAUTH2 / NONE | `Read InterfaceAuthType.java` |
| `NotifyMethod` 枚举 | EMAIL / IN_APP / SMS | `Read NotifyMethod.java` |
| `SysAlertRule` 模型 | 单 row 配置，无 category / 无 dispatch | `Read SysAlertRule + SysAlertRuleService` |
| Email/SMS sender 实现 | grep 0 命中 | `grep -rn 'MailSender\|EmailService\|SmsSender' fep-web/src/main` |
| Caffeine 已在 root pom | `pom.xml:60, 140`（P3a T3 用过） | `grep Caffeine pom.xml` |
| **`FepErrorCode` 类 + 包路径**（B3 修订） | `com.puchain.fep.common.domain.FepErrorCode`（不是 `.common.exception`）；`BIZ_5001 / BIZ_5002` 风格存在 | `find fep-common -name FepErrorCode.java` → `fep-common/src/main/java/com/puchain/fep/common/domain/FepErrorCode.java` |
| **`FepBusinessException` 包路径**（B3 修订） | `com.puchain.fep.common.exception.FepBusinessException`（与 FepErrorCode 不同包） | `find fep-common -name FepBusinessException.java` |
| `SubOutputInterface.interfaceId` FK 目标 | `SubOutputInterface.java:50 authType field` | `grep InterfaceAuthType fep-web/src/main` |
| **`SysUserRepository` 方法清单**（B2 修订） | `findByUserAccount / existsByUserAccount / findByUserStatus / findByUserNameContainingOrUserAccountContaining` — **无任何 role 关联方法** | `cat fep-web/src/main/java/com/puchain/fep/web/sysmgmt/user/repository/SysUserRepository.java` |
| **`SysRoleRepository.findByRoleCode(String) → Optional<SysRole>`**（B2 修订） | 存在 | `cat fep-web/src/main/java/com/puchain/fep/web/sysmgmt/role/repository/SysRoleRepository.java` |
| **`SysUserRoleRepository.findByRoleId(String) → List<SysUserRole>`**（B2 修订） | 存在 | `cat fep-web/src/main/java/com/puchain/fep/web/sysmgmt/rel/repository/SysUserRoleRepository.java`（**注: 包路径 `sysmgmt.rel.repository`，非 `sysmgmt.userrole`**） |

---

## §2 File Structure

### 2.1 fep-security-api（③ 安全 Task）

```
fep-security-api/
└── src/main/java/com/puchain/fep/security/api/
    └── KeyService.java                              # MODIFY: 加 getSm4CredentialMasterKey()
```

### 2.2 fep-security-impl（⛔ Mode E）

```
fep-security-impl/
└── src/main/java/com/puchain/fep/security/impl/
    └── KeyServiceImpl.java                          # MODIFY: 实现 getSm4CredentialMasterKey() ⛔
```

### 2.3 fep-web — 新增

```
fep-web/src/main/java/com/puchain/fep/web/callback/
├── credential/
│   ├── domain/
│   │   └── CallbackCredentialEntity.java            # CREATE
│   ├── repository/
│   │   └── CallbackCredentialRepository.java       # CREATE
│   ├── crypto/
│   │   └── CredentialEncryptionFacade.java         # CREATE
│   ├── oauth/
│   │   ├── OAuth2TokenCache.java                   # CREATE
│   │   ├── OAuth2ClientCredentialsClient.java     # CREATE
│   │   └── OAuth2TokenResponse.java                # CREATE (record)
│   ├── service/
│   │   ├── CallbackCredentialAdminService.java    # CREATE
│   │   └── CallbackCredentialResolver.java        # CREATE
│   ├── controller/
│   │   └── CallbackCredentialController.java      # CREATE
│   └── dto/
│       ├── CredentialCreateRequest.java            # CREATE
│       ├── CredentialUpdateRequest.java            # CREATE
│       └── CredentialResponse.java                 # CREATE（不回显密文）
├── dlq/
│   ├── event/
│   │   └── CallbackDeadLetterEvent.java            # CREATE
│   ├── service/
│   │   └── CallbackReplayService.java              # CREATE
│   ├── controller/
│   │   └── CallbackDlqController.java              # CREATE
│   └── dto/
│       ├── DlqEntryResponse.java                   # CREATE
│       └── DlqReplayResponse.java                  # CREATE
├── notification/
│   ├── domain/
│   │   └── InAppNotificationEntity.java            # CREATE
│   ├── repository/
│   │   └── InAppNotificationRepository.java       # CREATE
│   ├── listener/
│   │   └── InAppNotificationListener.java         # CREATE（订阅 CallbackDeadLetterEvent）
│   ├── service/
│   │   └── InAppNotificationService.java          # CREATE
│   ├── controller/
│   │   └── InAppNotificationController.java       # CREATE
│   └── dto/
│       └── NotificationResponse.java               # CREATE
└── reaper/
    └── CallbackStaleReaper.java                    # CREATE（@Scheduled）
```

### 2.4 fep-web — 修改

```
fep-web/src/main/java/com/puchain/fep/web/callback/
├── domain/
│   └── CallbackQueueEntity.java                     # MODIFY: +3 字段 (original_dlq_id, replayed_by, replayed_at) + getters + factory copy
├── repository/
│   └── CallbackQueueRepository.java                 # MODIFY: +findStaleSending + findDeadLetterRange + findById
├── runner/
│   └── CallbackRetryHandler.java                    # MODIFY: markDeadLetter 后 publishEvent(CallbackDeadLetterEvent)
└── http/
    └── CallbackHttpClient.java                      # MODIFY: 注入 CallbackCredentialResolver 替换 P1 scaffold
```

### 2.5 fep-web — Flyway

```
fep-web/src/main/resources/db/migration/
├── V30__callback_credential.sql                     # CREATE
├── V31__callback_queue_replay_link.sql             # CREATE
└── V32__in_app_notification.sql                    # CREATE
```

### 2.6 fep-web — 配置

```
fep-web/src/main/resources/application.yml           # MODIFY: +fep.callback.reaper 段
fep-web/src/main/java/.../callback/config/
└── CallbackQueueProperties.java                     # MODIFY: +ReaperProperties record
```

### 2.7 fep-admin-ui — Vue3 + TS（**已迁至子 Plan，本 Plan 不实施**）

> M2 修订：T15 fep-admin-ui (~600-800 行 / 11 文件) 拆至独立子 Plan [`2026-05-28-callback-phase2b-ui.md`](2026-05-28-callback-phase2b-ui.md) 避 ≤400 行 PR 门禁爆裂。本节保留以提供 UI 文件清单概览，**实际 file write + 测试 + commit + PR 全在子 Plan 内**。

```
fep-admin-ui/src/views/callback/         # 迁至子 Plan
├── CredentialList.vue
├── CredentialForm.vue
├── DlqList.vue
└── DlqReplayConfirm.vue
fep-admin-ui/src/components/Notification/  # 迁至子 Plan
└── NotificationBell.vue
fep-admin-ui/src/api/                     # 迁至子 Plan
├── callbackCredential.ts
├── callbackDlq.ts
└── inAppNotification.ts
fep-admin-ui/src/router/                  # 迁至子 Plan
└── routes.ts (MODIFY +3 menu entries)
```

### 2.8 测试

```
fep-web/src/test/java/com/puchain/fep/web/callback/
├── credential/
│   ├── crypto/CredentialEncryptionFacadeTest.java
│   ├── oauth/OAuth2TokenCacheTest.java
│   ├── oauth/OAuth2ClientCredentialsClientIT.java
│   ├── service/CallbackCredentialAdminServiceTest.java
│   ├── service/CallbackCredentialResolverTest.java
│   └── controller/CallbackCredentialControllerIT.java
├── dlq/
│   ├── service/CallbackReplayServiceTest.java
│   └── controller/CallbackDlqControllerIT.java
├── notification/
│   ├── listener/InAppNotificationListenerTest.java
│   ├── service/InAppNotificationServiceTest.java
│   └── controller/InAppNotificationControllerIT.java
├── reaper/CallbackStaleReaperTest.java
├── CallbackPhase2bEndToEndIT.java                   # 端到端（凭证→OAuth2→callback→DLQ→IN_APP→重放）
└── CallbackModuleArchTest.java                      # MODIFY: +invariants
```

---

## §3 Tasks

### Task T1 — KeyService API 扩展（**⛔ Mode E — ③ 安全专家 + AI 起草 reviewer aid，v0.3 B5 修订**）

**Owner**: ③ 安全专家拍板 + AI 起草 reviewer aid（**T1 PR 与 T2 同 PR**: `wt-callback-p2b-sec` / 分支 `feat/callback-phase2b-security-credential-key`）
**Worktree**: `wt-callback-p2b-sec`（与 T2 共用，编译序闸：T1 interface 必须先于 T2 `@Override` 存在）
**Commit footer**: `Security-Reviewed-By: ③<姓名>`（不含 `AI-Generated`）— v0.3 / B5 修订 ownership 转 ⛔ Mode E 后 commit footer 同步

**Files**:
- Modify: `fep-security-api/src/main/java/com/puchain/fep/security/api/KeyService.java`
- Modify: `fep-security-api/src/test/java/com/puchain/fep/security/api/SecurityApiConfigurationTest.java`（mock impl 同步加方法）

- [ ] **Step 1: 写失败测试** — `SecurityApiConfigurationTest`（既有 mock impl 覆盖测试）追加方法存在性测试

```java
@Test
void mockKeyServiceProvidesSm4CredentialMasterKey() {
    byte[] key = keyService.getSm4CredentialMasterKey();
    assertThat(key).isNotNull().hasSize(16);
}
```

- [ ] **Step 2: 运行测试确认失败**

```
cd /Users/muzhou/FEP_v1.0_wt-callback-p2b
./mvnw -pl fep-security-api test -Dtest=SecurityApiConfigurationTest -Dsurefire.failIfNoSpecifiedTests=false
```

Expected: FAIL — `getSm4CredentialMasterKey` undefined。

- [ ] **Step 3: KeyService.java 加方法**

```java
/**
 * 返回 callback 凭证加密用 SM4 主密钥（16 字节）。
 *
 * <p><strong>⛔ Mode E:</strong> {@code fep-security-impl} 实现必须由安全工程师
 * 编写，AI 禁入。密钥材料须来自 HSM / sealed key store；mock impl 仅返
 * 16 字节零向量供单元测试，**禁止用于生产**。</p>
 *
 * @return SM4 master key (16 bytes), 永不为 null
 */
byte[] getSm4CredentialMasterKey();
```

- [ ] **Step 4: mock impl 加方法（SecurityApiConfiguration 内）**

```java
@Bean
@ConditionalOnMissingBean(KeyService.class)
public KeyService keyServiceMock() {
    return new KeyService() {
        // ... 既有方法 ...
        @Override
        public byte[] getSm4CredentialMasterKey() {
            return new byte[16];  // mock for unit test only; impl in security-impl
        }
    };
}
```

- [ ] **Step 5: 运行测试通过**

```
./mvnw -pl fep-security-api test -Dtest=SecurityApiConfigurationTest
```

Expected: PASS。

- [ ] **Step 6: Commit**（v0.3 B5 修订: footer 改 `Security-Reviewed-By` 不含 `AI-Generated`，T1 ownership ⛔ Mode E）

```bash
cd /Users/muzhou/FEP_v1.0_wt-callback-p2b-sec
git add fep-security-api/
git commit -m "feat(security-api): KeyService.getSm4CredentialMasterKey() API + mock zero-vector

T1 of Callback Phase 2b — adds API hook for SM4 credential master key.
Mock impl returns 16-byte zeros for unit tests only; real impl in
fep-security-impl by security specialist (T2, same PR).

API semantics (key length, lifecycle, HSM source) pinned by ③ security
specialist; AI drafted as reviewer aid only.

Security-Reviewed-By: ③<姓名>"
```

---

### Task T2 — KeyServiceImpl SM4 master key（**⛔ Mode E — ③ 安全专家**）

**Owner**: ③ 安全/密码专家（**AI 禁入**）
**Worktree**: `wt-callback-p2b-sec`（与 T1 共用，分支 `feat/callback-phase2b-security-credential-key`，v0.3 B5 修订）
**Files**:
- Modify: `fep-security-impl/src/main/java/com/puchain/fep/security/impl/KeyServiceImpl.java`
- Modify: `fep-security-impl/src/test/java/com/puchain/fep/security/impl/KeyServiceImplTest.java`

- [ ] **Step 1（③ 安全专家）**: 在 `KeyServiceImpl` 实现 `getSm4CredentialMasterKey()`，密钥来源 HSM / sealed key store / 配置加密文件
- [ ] **Step 2（③ 安全专家）**: 单元测试验证 key 长度 16 字节 + 非零 + 来源符合密钥管理规范
- [ ] **Step 3（③ 安全专家）**: 安全审计 — 密钥不出现在日志 / 不出现在异常信息 / 不缓存在 String 字段（用 byte[] 即用即销）
- [ ] **Step 4（③ 安全专家）**: Commit with `Security-Reviewed-By: ③<姓名>` footer（**不含 `AI-Generated`**）

```bash
git add fep-security-impl/
git commit -m "feat(security-impl): KeyServiceImpl.getSm4CredentialMasterKey() HSM-backed

T2 of Callback Phase 2b — Mode E by security specialist.
Key material from <HSM/sealed store/encrypted file>;
byte[] used directly without String intermediate.

Security-Reviewed-By: ③<姓名>"
```

**注**: T6 可在 T2 完成前用 T1 mock impl 推进（不阻塞）；T2 必须在 PR merge 到 main 前完成。

---

### Task T3 — V30 callback_credential 表 + Entity + Repository（AI / Mode A）

**Files**:
- Create: `fep-web/src/main/resources/db/migration/V30__callback_credential.sql`
- Create: `fep-web/src/main/java/com/puchain/fep/web/callback/credential/domain/CallbackCredentialEntity.java`
- Create: `fep-web/src/main/java/com/puchain/fep/web/callback/credential/repository/CallbackCredentialRepository.java`
- Create: `fep-web/src/test/java/com/puchain/fep/web/callback/credential/repository/CallbackCredentialRepositoryTest.java`

- [ ] **Step 1: V30 Flyway 迁移**

```sql
-- Callback Phase 2b T3: 凭证存储（SM4 加密列 + 1:1 FK to sub_output_interface）
CREATE TABLE callback_credential (
  credential_id    VARCHAR(32)  NOT NULL,
  interface_id     VARCHAR(32)  NOT NULL,
  auth_type        VARCHAR(20)  NOT NULL,            -- TOKEN / OAUTH2
  -- TOKEN 字段
  token_ciphertext            VARBINARY(512),
  token_header                VARCHAR(50) DEFAULT 'Authorization',
  -- OAUTH2 字段
  oauth_client_id_ciphertext     VARBINARY(512),
  oauth_client_secret_ciphertext VARBINARY(1024),
  oauth_token_endpoint           VARCHAR(500),
  oauth_scope                    VARCHAR(200),
  -- 公共
  key_id           VARCHAR(32)  NOT NULL,             -- SM4 key version
  create_time      TIMESTAMP    NOT NULL,
  update_time      TIMESTAMP    NOT NULL,
  rotated_at       TIMESTAMP    NULL,
  PRIMARY KEY (credential_id),
  CONSTRAINT uk_callback_credential_interface UNIQUE (interface_id),
  CONSTRAINT fk_callback_credential_interface
    FOREIGN KEY (interface_id) REFERENCES t_sub_output_interface(interface_id)  -- B1 修订: 旧业务表带 t_ 前缀
);
CREATE INDEX idx_callback_credential_interface ON callback_credential(interface_id);
```

- [ ] **Step 2: CallbackCredentialEntity**

```java
package com.puchain.fep.web.callback.credential.domain;

import com.puchain.fep.web.submission.outputinterface.domain.InterfaceAuthType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "callback_credential",
        uniqueConstraints = @UniqueConstraint(name = "uk_callback_credential_interface",
                columnNames = "interface_id"))
public class CallbackCredentialEntity {

    @Id
    @Column(name = "credential_id", length = 32)
    private String credentialId;

    @Column(name = "interface_id", nullable = false, length = 32, unique = true)
    private String interfaceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_type", nullable = false, length = 20)
    private InterfaceAuthType authType;

    @Column(name = "token_ciphertext")
    private byte[] tokenCiphertext;

    @Column(name = "token_header", length = 50)
    private String tokenHeader;

    @Column(name = "oauth_client_id_ciphertext")
    private byte[] oauthClientIdCiphertext;

    @Column(name = "oauth_client_secret_ciphertext")
    private byte[] oauthClientSecretCiphertext;

    @Column(name = "oauth_token_endpoint", length = 500)
    private String oauthTokenEndpoint;

    @Column(name = "oauth_scope", length = 200)
    private String oauthScope;

    @Column(name = "key_id", nullable = false, length = 32)
    private String keyId;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @Column(name = "rotated_at")
    private LocalDateTime rotatedAt;

    protected CallbackCredentialEntity() { /* for JPA */ }

    public static CallbackCredentialEntity newToken(String interfaceId, byte[] tokenCipher,
                                                    String tokenHeader, String keyId) {
        CallbackCredentialEntity e = new CallbackCredentialEntity();
        e.credentialId = UUID.randomUUID().toString().replace("-", "");
        e.interfaceId = interfaceId;
        e.authType = InterfaceAuthType.TOKEN;
        e.tokenCiphertext = tokenCipher;
        e.tokenHeader = tokenHeader != null ? tokenHeader : "Authorization";
        e.keyId = keyId;
        e.createTime = LocalDateTime.now();
        e.updateTime = e.createTime;
        return e;
    }

    public static CallbackCredentialEntity newOauth(String interfaceId,
            byte[] clientIdCipher, byte[] clientSecretCipher, String tokenEndpoint,
            String scope, String keyId) {
        CallbackCredentialEntity e = new CallbackCredentialEntity();
        e.credentialId = UUID.randomUUID().toString().replace("-", "");
        e.interfaceId = interfaceId;
        e.authType = InterfaceAuthType.OAUTH2;
        e.oauthClientIdCiphertext = clientIdCipher;
        e.oauthClientSecretCiphertext = clientSecretCipher;
        e.oauthTokenEndpoint = tokenEndpoint;
        e.oauthScope = scope;
        e.keyId = keyId;
        e.createTime = LocalDateTime.now();
        e.updateTime = e.createTime;
        return e;
    }

    public void rotate(byte[] newTokenCipher, byte[] newClientIdCipher,
                       byte[] newClientSecretCipher, String newKeyId) {
        if (this.authType == InterfaceAuthType.TOKEN && newTokenCipher != null) {
            this.tokenCiphertext = newTokenCipher;
        }
        if (this.authType == InterfaceAuthType.OAUTH2) {
            if (newClientIdCipher != null) this.oauthClientIdCiphertext = newClientIdCipher;
            if (newClientSecretCipher != null) this.oauthClientSecretCiphertext = newClientSecretCipher;
        }
        this.keyId = newKeyId;
        this.rotatedAt = LocalDateTime.now();
        this.updateTime = this.rotatedAt;
    }

    // ===== Getters =====（省略，按 checkstyle 全 Javadoc）
    public String getCredentialId() { return credentialId; }
    public String getInterfaceId() { return interfaceId; }
    public InterfaceAuthType getAuthType() { return authType; }
    public byte[] getTokenCiphertext() { return tokenCiphertext; }
    public String getTokenHeader() { return tokenHeader; }
    public byte[] getOauthClientIdCiphertext() { return oauthClientIdCiphertext; }
    public byte[] getOauthClientSecretCiphertext() { return oauthClientSecretCiphertext; }
    public String getOauthTokenEndpoint() { return oauthTokenEndpoint; }
    public String getOauthScope() { return oauthScope; }
    public String getKeyId() { return keyId; }
    public LocalDateTime getCreateTime() { return createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public LocalDateTime getRotatedAt() { return rotatedAt; }
}
```

- [ ] **Step 3: Repository**

```java
package com.puchain.fep.web.callback.credential.repository;

import com.puchain.fep.web.callback.credential.domain.CallbackCredentialEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CallbackCredentialRepository
        extends JpaRepository<CallbackCredentialEntity, String> {

    Optional<CallbackCredentialEntity> findByInterfaceId(String interfaceId);

    void deleteByInterfaceId(String interfaceId);
}
```

- [ ] **Step 4: Repository 测试**

```java
@DataJpaTest
class CallbackCredentialRepositoryTest {
    @Autowired CallbackCredentialRepository repo;

    @Test
    void findByInterfaceIdReturnsTokenCredential() {
        var entity = CallbackCredentialEntity.newToken("IF-001",
                new byte[]{1,2,3}, "Authorization", "KEY-V1");
        repo.save(entity);
        var found = repo.findByInterfaceId("IF-001");
        assertThat(found).isPresent();
        assertThat(found.get().getAuthType()).isEqualTo(InterfaceAuthType.TOKEN);
        assertThat(found.get().getTokenCiphertext()).containsExactly(1,2,3);
    }

    @Test
    void uniqueConstraintOnInterfaceId() {
        repo.saveAndFlush(CallbackCredentialEntity.newToken("IF-001",
                new byte[]{1}, null, "KEY-V1"));
        assertThatThrownBy(() -> repo.saveAndFlush(
                CallbackCredentialEntity.newToken("IF-001", new byte[]{2}, null, "KEY-V1")))
            .isInstanceOf(DataIntegrityViolationException.class);
    }
}
```

- [ ] **Step 5: 跑全 fep-web 模块测试**（红线 `feedback_full_regression_before_commit`）

```
cd /Users/muzhou/FEP_v1.0_wt-callback-p2b
./mvnw -pl fep-web test -am
```

Expected: PASS（含 V30 Flyway migration 成功）。

- [ ] **Step 6: Commit**

```bash
git add fep-web/src/main/resources/db/migration/V30__callback_credential.sql \
        fep-web/src/main/java/com/puchain/fep/web/callback/credential/domain/ \
        fep-web/src/main/java/com/puchain/fep/web/callback/credential/repository/ \
        fep-web/src/test/java/com/puchain/fep/web/callback/credential/repository/
git commit -m "feat(web): callback_credential V30 + Entity + Repository

T3 of Callback Phase 2b — 1:1 FK to sub_output_interface, SM4 ciphertext columns,
key_id for rotation. TOKEN + OAUTH2 fields per auth_type discriminator.

AI-Generated: claude-code
Reviewed-By: pending"
```

---

### Task T4 — CredentialEncryptionFacade (SM4 调用层)（AI / Mode A）

**Files**:
- Create: `fep-web/src/main/java/com/puchain/fep/web/callback/credential/crypto/CredentialEncryptionFacade.java`
- Create: `fep-web/src/test/java/com/puchain/fep/web/callback/credential/crypto/CredentialEncryptionFacadeTest.java`

- [ ] **Step 1: 写失败测试 — encrypt→decrypt roundtrip**

```java
@ExtendWith(MockitoExtension.class)
class CredentialEncryptionFacadeTest {

    @Mock CryptoService cryptoService;
    @Mock KeyService keyService;
    CredentialEncryptionFacade facade;

    @BeforeEach
    void setUp() {
        facade = new CredentialEncryptionFacade(cryptoService, keyService);
    }

    @Test
    void encryptCallsCryptoServiceWithMasterKey() {
        byte[] key = new byte[16];
        when(keyService.getSm4CredentialMasterKey()).thenReturn(key);
        when(cryptoService.encrypt(any(), eq(key))).thenReturn(new byte[]{9,9,9});

        EncryptedCredential result = facade.encrypt("plain-token");

        assertThat(result.ciphertext()).containsExactly(9,9,9);
        assertThat(result.keyId()).isNotNull();  // implementation-dependent key id
    }

    @Test
    void decryptCallsCryptoServiceAndReturnsString() {
        when(keyService.getSm4CredentialMasterKey()).thenReturn(new byte[16]);
        when(cryptoService.decrypt(eq(new byte[]{9,9,9}), any()))
            .thenReturn("plain-token".getBytes(StandardCharsets.UTF_8));

        String result = facade.decrypt(new byte[]{9,9,9}, "KEY-V1");

        assertThat(result).isEqualTo("plain-token");
    }

    @Test
    void encryptNullPlaintextThrows() {
        assertThatThrownBy(() -> facade.encrypt(null))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void encryptEmptyPlaintextThrows() {
        assertThatThrownBy(() -> facade.encrypt(""))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("empty");
    }
}
```

- [ ] **Step 2: 运行测试确认失败** — class undefined。

- [ ] **Step 3: 实现 CredentialEncryptionFacade**

```java
package com.puchain.fep.web.callback.credential.crypto;

import com.puchain.fep.security.api.CryptoService;
import com.puchain.fep.security.api.KeyService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 凭证加密 facade — callback 模块对 fep-security-api SM4 的统一调用层。
 * <p>plain String ↔ ciphertext byte[] roundtrip；keyId 来源 KeyService 当前版本。</p>
 * <p>⛔ 实际 SM4 加解密在 fep-security-impl（AI 禁入），本 facade 仅 wire 调用。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
@SuppressFBWarnings(value = "EI_EXPOSE_REP2",
        justification = "constructor stores Spring-managed singletons; safe by container contract")
public class CredentialEncryptionFacade {

    /** 当前 KeyService 实现未提供 keyId 版本号，本期固定 V1（轮换支持留 §5 风险段）。 */
    private static final String CURRENT_KEY_ID = "KEY-V1";

    private final CryptoService cryptoService;
    private final KeyService keyService;

    public CredentialEncryptionFacade(final CryptoService cryptoService,
                                       final KeyService keyService) {
        this.cryptoService = Objects.requireNonNull(cryptoService);
        this.keyService = Objects.requireNonNull(keyService);
    }

    /**
     * 加密明文凭证为密文 + 当前 keyId。
     *
     * @param plaintext 明文，非 null 非空
     * @return 加密结果（ciphertext + keyId）
     */
    public EncryptedCredential encrypt(final String plaintext) {
        Objects.requireNonNull(plaintext, "plaintext");
        if (plaintext.isEmpty()) {
            throw new IllegalArgumentException("plaintext is empty");
        }
        final byte[] key = keyService.getSm4CredentialMasterKey();
        final byte[] cipher = cryptoService.encrypt(
                plaintext.getBytes(StandardCharsets.UTF_8), key);
        return new EncryptedCredential(cipher, CURRENT_KEY_ID);
    }

    /**
     * 解密密文 + keyId 还原明文。
     *
     * @param ciphertext 密文，非 null
     * @param keyId      历史 keyId（多版本 key 共存支持，本期仅 V1）
     * @return 明文 String
     */
    public String decrypt(final byte[] ciphertext, final String keyId) {
        Objects.requireNonNull(ciphertext, "ciphertext");
        // keyId 暂只有 V1，多版本支持留 Phase 3
        final byte[] key = keyService.getSm4CredentialMasterKey();
        final byte[] plain = cryptoService.decrypt(ciphertext, key);
        return new String(plain, StandardCharsets.UTF_8);
    }

    /** 加密结果记录。 */
    public record EncryptedCredential(byte[] ciphertext, String keyId) { }
}
```

- [ ] **Step 4: 运行测试通过**

```
./mvnw -pl fep-web test -Dtest=CredentialEncryptionFacadeTest
```

Expected: PASS。

- [ ] **Step 5: Commit**

```bash
git add fep-web/src/main/java/com/puchain/fep/web/callback/credential/crypto/ \
        fep-web/src/test/java/com/puchain/fep/web/callback/credential/crypto/
git commit -m "feat(web): CredentialEncryptionFacade — SM4 wire to fep-security-api

T4 of Callback Phase 2b — plain String ↔ ciphertext byte[] via CryptoService;
keyId scaffold V1 (rotation留 Phase 3).

AI-Generated: claude-code
Reviewed-By: pending"
```

---

### Task T5 — OAuth2TokenCache (Caffeine) + Client (AI / Mode A)

**Files**:
- Create: `fep-web/src/main/java/com/puchain/fep/web/callback/credential/oauth/OAuth2TokenCache.java`
- Create: `fep-web/src/main/java/com/puchain/fep/web/callback/credential/oauth/OAuth2ClientCredentialsClient.java`
- Create: `fep-web/src/main/java/com/puchain/fep/web/callback/credential/oauth/OAuth2TokenResponse.java`
- Create: `fep-web/src/test/java/com/puchain/fep/web/callback/credential/oauth/OAuth2TokenCacheTest.java`
- Create: `fep-web/src/test/java/com/puchain/fep/web/callback/credential/oauth/OAuth2ClientCredentialsClientIT.java`

- [ ] **Step 1: OAuth2TokenResponse 测试 + record**

```java
// Test
class OAuth2TokenResponseTest {
    @Test
    void recordHoldsTokenAndExpiry() {
        var r = new OAuth2TokenResponse("abc", 3600, "Bearer");
        assertThat(r.accessToken()).isEqualTo("abc");
        assertThat(r.expiresIn()).isEqualTo(3600);
    }
}

// Impl
public record OAuth2TokenResponse(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("expires_in")    int expiresIn,
    @JsonProperty("token_type")    String tokenType) {
}
```

- [ ] **Step 2: OAuth2TokenCache 测试（Caffeine TTL）**

```java
@ExtendWith(MockitoExtension.class)
class OAuth2TokenCacheTest {
    OAuth2TokenCache cache;

    @BeforeEach
    void setUp() { cache = new OAuth2TokenCache(); }

    @Test
    void putAndGetReturnsToken() {
        cache.put("IF-001", "token-abc", Duration.ofSeconds(60));
        assertThat(cache.get("IF-001")).isEqualTo(Optional.of("token-abc"));
    }

    @Test
    void getReturnsEmptyAfterTtl() throws InterruptedException {
        cache.put("IF-001", "token-abc", Duration.ofMillis(100));
        Thread.sleep(150);
        assertThat(cache.get("IF-001")).isEmpty();
    }

    @Test
    void invalidateRemovesEntry() {
        cache.put("IF-001", "token", Duration.ofMinutes(5));
        cache.invalidate("IF-001");
        assertThat(cache.get("IF-001")).isEmpty();
    }
}
```

- [ ] **Step 3: OAuth2TokenCache 实现**（**N1 修订: 简化为 entry-wrapped expireAfterWrite，删除 expireVariably / cache.put 双写矛盾**）

```java
package com.puchain.fep.web.callback.credential.oauth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
// N4 v0.3 修订: 移除 import org.checkerframework.checker.index.qual.NonNegative;
// Caffeine Expiry 接口签名中 @NonNegative 是接口契约层，覆写方法不需重复声明，
// 且 codebase 未配置 Checker Framework processor，注解仅 documentation。

/**
 * 行内 OAuth2 access_token 内存缓存（per interfaceId）。
 * <p>TTL 由 token 的 {@code expires_in - 30s} safety margin 决定；
 * 401 时调用方应主动 {@link #invalidate}。重启 cache 清空。</p>
 *
 * <p>N1 修订（v0.2）: 用 {@link Expiry} per-entry expireAfterCreate 表达可变 TTL；
 * v0.1 用 expireVariably().ifPresent + cache.put 双写有 race，本版改 entry-wrapped
 * Expiry 单写路径。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class OAuth2TokenCache {

    private final Cache<String, CachedToken> cache;

    public OAuth2TokenCache() {
        this.cache = Caffeine.newBuilder()
            .maximumSize(1_000)
            .expireAfter(new Expiry<String, CachedToken>() {
                @Override
                public long expireAfterCreate(String key, CachedToken value, long currentTime) {
                    return value.ttlNanos();
                }
                @Override
                public long expireAfterUpdate(String key, CachedToken value, long currentTime,
                                              long currentDuration) {  // N4: 移除 @NonNegative
                    return value.ttlNanos();
                }
                @Override
                public long expireAfterRead(String key, CachedToken value, long currentTime,
                                            long currentDuration) {  // N4: 移除 @NonNegative
                    return currentDuration;
                }
            })
            .build();
    }

    public void put(final String interfaceId, final String token, final Duration ttl) {
        Objects.requireNonNull(interfaceId);
        Objects.requireNonNull(token);
        Objects.requireNonNull(ttl);
        cache.put(interfaceId, new CachedToken(token, ttl.toNanos()));
    }

    private record CachedToken(String token, long ttlNanos) { }


    public Optional<String> get(final String interfaceId) {
        return Optional.ofNullable(cache.getIfPresent(interfaceId)).map(CachedToken::token);
    }

    public void invalidate(final String interfaceId) {
        cache.invalidate(interfaceId);
    }
}
```

> **注（N1 修订 / v0.2）**: v0.1 用 `cache.policy().expireVariably().ifPresent(policy -> policy.put(...))` + `cache.put(...)` 双写有 race（前者条件命中 + 后者无条件覆盖），TTL 行为不可预测。v0.2 改 entry-wrapped `CachedToken record(token, ttlNanos)` + `Expiry.expireAfterCreate(value.ttlNanos())`，单写路径 + per-entry 动态 TTL 兼得。Caffeine `expireAfter(Expiry)` 不需 `scheduler` 启用（仅触发 eviction 才需 explicit scheduler）。

- [ ] **Step 4: OAuth2ClientCredentialsClient IT — WireMock**

```java
@ExtendWith(WireMockExtension.class)
@SpringBootTest
class OAuth2ClientCredentialsClientIT {

    @RegisterExtension
    static WireMockExtension oauth = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort()).build();

    @Autowired OAuth2ClientCredentialsClient client;

    @Test
    void fetchTokenViaClientCredentialsGrant() {
        oauth.stubFor(post("/token")
            .withRequestBody(containing("grant_type=client_credentials"))
            .willReturn(okJson("""
                {"access_token":"tok-xyz","expires_in":3600,"token_type":"Bearer"}""")));

        OAuth2TokenResponse resp = client.fetchToken(
            oauth.baseUrl() + "/token", "client-id", "client-secret", "read");

        assertThat(resp.accessToken()).isEqualTo("tok-xyz");
        assertThat(resp.expiresIn()).isEqualTo(3600);
    }

    @Test
    void fetch5xxThrowsRetryableException() {
        oauth.stubFor(post("/token").willReturn(serverError()));
        assertThatThrownBy(() -> client.fetchToken(
            oauth.baseUrl() + "/token", "x", "y", ""))
            .isInstanceOf(OAuth2RetryableException.class);
    }

    @Test
    void fetch401ThrowsNonRetryableException() {
        oauth.stubFor(post("/token").willReturn(unauthorized()));
        assertThatThrownBy(() -> client.fetchToken(
            oauth.baseUrl() + "/token", "x", "y", ""))
            .isInstanceOf(OAuth2InvalidCredentialException.class);
    }
}
```

- [ ] **Step 5: OAuth2ClientCredentialsClient 实现**

```java
package com.puchain.fep.web.callback.credential.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
public class OAuth2ClientCredentialsClient {

    private final HttpClient http;
    private final ObjectMapper mapper;

    public OAuth2ClientCredentialsClient(final ObjectMapper mapper) {
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.mapper = mapper;
    }

    public OAuth2TokenResponse fetchToken(final String tokenEndpoint,
            final String clientId, final String clientSecret, final String scope) {
        final String body = "grant_type=client_credentials"
            + "&client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
            + "&client_secret=" + URLEncoder.encode(clientSecret, StandardCharsets.UTF_8)
            + (scope != null && !scope.isEmpty()
               ? "&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8) : "");

        final HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(tokenEndpoint))
            .timeout(Duration.ofSeconds(15))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        try {
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            int s = resp.statusCode();
            if (s == 200) return mapper.readValue(resp.body(), OAuth2TokenResponse.class);
            if (s == 401 || s == 403) throw new OAuth2InvalidCredentialException(
                "OAuth2 endpoint rejected credentials, status=" + s);
            throw new OAuth2RetryableException("OAuth2 endpoint failure, status=" + s);
        } catch (IOException e) {
            throw new OAuth2RetryableException("OAuth2 IO failure: "
                + e.getClass().getSimpleName(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OAuth2RetryableException("OAuth2 interrupted", e);
        }
    }
}
```

加上 2 个异常类（同包）:

```java
public class OAuth2RetryableException extends RuntimeException {
    public OAuth2RetryableException(String m) { super(m); }
    public OAuth2RetryableException(String m, Throwable c) { super(m, c); }
}
public class OAuth2InvalidCredentialException extends RuntimeException {
    public OAuth2InvalidCredentialException(String m) { super(m); }
}
```

- [ ] **Step 6: 运行测试通过 + 全 fep-web test**

```
./mvnw -pl fep-web test -am
```

- [ ] **Step 7: Commit**

```bash
git add fep-web/src/main/java/com/puchain/fep/web/callback/credential/oauth/ \
        fep-web/src/test/java/com/puchain/fep/web/callback/credential/oauth/
git commit -m "feat(web): OAuth2 Client Credentials client + Caffeine token cache

T5 of Callback Phase 2b — RFC 6749 §4.4 Client Credentials Grant only;
Caffeine in-memory cache per interfaceId; 401/403 → InvalidCredential
non-retryable; 5xx/IO → Retryable.

AI-Generated: claude-code
Reviewed-By: pending"
```

---

### Task T6 — CallbackCredentialResolver + CallbackHttpClient 接通真凭证（AI / Mode A）

**Files**:
- Create: `fep-web/src/main/java/com/puchain/fep/web/callback/credential/service/CallbackCredentialResolver.java`
- Modify: `fep-web/src/main/java/com/puchain/fep/web/callback/http/CallbackHttpClient.java`
- Create: `fep-web/src/test/java/com/puchain/fep/web/callback/credential/service/CallbackCredentialResolverTest.java`

> **N2 修订（v0.2）**: T6 修改 `CallbackHttpClient` 构造器（无参 → 注入 `CallbackCredentialResolver`），实施前必须 grep 既有 `new CallbackHttpClient(` 调用点确认无遗漏：
> ```bash
> grep -rn 'new CallbackHttpClient(' fep-web/src --include="*.java"
> ```
> 预期命中：Spring `@Component` 自动装配（无手动 new）+ 单测 mock 构造（若有需同步改 mock）。任何手动 new 调用点必须同 commit 改为注入。

- [ ] **Step 0（N2）: 实施前 grep 既有构造调用点**

```bash
cd /Users/muzhou/FEP_v1.0_wt-callback-p2b
grep -rn 'new CallbackHttpClient(' fep-web/src --include="*.java"
# 期望: 0 命中（Spring 自动装配） 或 全部为单测 mock new（同步改）
```

- [ ] **Step 1: Resolver 测试 — 三分支 + 缺失 credential**

```java
@ExtendWith(MockitoExtension.class)
class CallbackCredentialResolverTest {
    @Mock CallbackCredentialRepository repo;
    @Mock CredentialEncryptionFacade facade;
    @Mock OAuth2TokenCache cache;
    @Mock OAuth2ClientCredentialsClient oauthClient;
    CallbackCredentialResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new CallbackCredentialResolver(repo, facade, cache, oauthClient);
    }

    @Test
    void resolveNoneReturnsEmptyHeader() {
        var target = mockTarget("IF-001", InterfaceAuthType.NONE);
        Optional<AuthHeader> h = resolver.resolveAuthHeader(target);
        assertThat(h).isEmpty();
    }

    @Test
    void resolveTokenReturnsDecryptedToken() {
        var target = mockTarget("IF-001", InterfaceAuthType.TOKEN);
        var entity = CallbackCredentialEntity.newToken("IF-001",
                new byte[]{1}, "X-Auth", "KEY-V1");
        when(repo.findByInterfaceId("IF-001")).thenReturn(Optional.of(entity));
        when(facade.decrypt(new byte[]{1}, "KEY-V1")).thenReturn("tok-plain");

        Optional<AuthHeader> h = resolver.resolveAuthHeader(target);

        assertThat(h).isPresent();
        assertThat(h.get().name()).isEqualTo("X-Auth");
        assertThat(h.get().value()).isEqualTo("tok-plain");
    }

    @Test
    void resolveOAuth2CacheHitSkipsFetch() {
        var target = mockTarget("IF-001", InterfaceAuthType.OAUTH2);
        when(cache.get("IF-001")).thenReturn(Optional.of("cached-tok"));

        Optional<AuthHeader> h = resolver.resolveAuthHeader(target);

        assertThat(h.get().value()).isEqualTo("Bearer cached-tok");
        verifyNoInteractions(oauthClient);
    }

    @Test
    void resolveOAuth2CacheMissFetchesDecryptsAndCaches() {
        var target = mockTarget("IF-001", InterfaceAuthType.OAUTH2);
        when(cache.get("IF-001")).thenReturn(Optional.empty());
        var cred = CallbackCredentialEntity.newOauth("IF-001",
                new byte[]{1}, new byte[]{2}, "https://idp/token", "read", "KEY-V1");
        when(repo.findByInterfaceId("IF-001")).thenReturn(Optional.of(cred));
        when(facade.decrypt(new byte[]{1}, "KEY-V1")).thenReturn("clid");
        when(facade.decrypt(new byte[]{2}, "KEY-V1")).thenReturn("csec");
        when(oauthClient.fetchToken("https://idp/token", "clid", "csec", "read"))
            .thenReturn(new OAuth2TokenResponse("new-tok", 3600, "Bearer"));

        Optional<AuthHeader> h = resolver.resolveAuthHeader(target);

        assertThat(h.get().value()).isEqualTo("Bearer new-tok");
        verify(cache).put(eq("IF-001"), eq("new-tok"),
                          argThat(d -> d.toSeconds() == 3600 - 30));
    }

    @Test
    void resolveTokenMissingCredentialThrows() {
        var target = mockTarget("IF-001", InterfaceAuthType.TOKEN);
        when(repo.findByInterfaceId("IF-001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resolver.resolveAuthHeader(target))
            .isInstanceOf(CallbackCredentialMissingException.class);
    }

    private SubOutputInterface mockTarget(String id, InterfaceAuthType at) {
        var t = new SubOutputInterface();
        t.setInterfaceId(id);
        t.setAuthType(at);
        return t;
    }
}
```

- [ ] **Step 2: Resolver 实现**

```java
package com.puchain.fep.web.callback.credential.service;

import com.puchain.fep.web.callback.credential.crypto.CredentialEncryptionFacade;
import com.puchain.fep.web.callback.credential.domain.CallbackCredentialEntity;
import com.puchain.fep.web.callback.credential.oauth.*;
import com.puchain.fep.web.callback.credential.repository.CallbackCredentialRepository;
import com.puchain.fep.web.submission.outputinterface.domain.SubOutputInterface;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class CallbackCredentialResolver {

    private static final int OAUTH_SAFETY_MARGIN_SECONDS = 30;

    private final CallbackCredentialRepository repo;
    private final CredentialEncryptionFacade facade;
    private final OAuth2TokenCache cache;
    private final OAuth2ClientCredentialsClient oauthClient;

    public CallbackCredentialResolver(CallbackCredentialRepository repo,
            CredentialEncryptionFacade facade, OAuth2TokenCache cache,
            OAuth2ClientCredentialsClient oauthClient) {
        this.repo = repo; this.facade = facade;
        this.cache = cache; this.oauthClient = oauthClient;
    }

    public Optional<AuthHeader> resolveAuthHeader(final SubOutputInterface target) {
        return switch (target.getAuthType()) {
            case NONE   -> Optional.empty();
            case TOKEN  -> Optional.of(resolveToken(target.getInterfaceId()));
            case OAUTH2 -> Optional.of(resolveOAuth2(target.getInterfaceId()));
        };
    }

    public void invalidateOAuthToken(final String interfaceId) {
        cache.invalidate(interfaceId);
    }

    private AuthHeader resolveToken(final String interfaceId) {
        CallbackCredentialEntity e = repo.findByInterfaceId(interfaceId)
            .orElseThrow(() -> new CallbackCredentialMissingException(
                "TOKEN credential missing for interfaceId=" + interfaceId));
        String plain = facade.decrypt(e.getTokenCiphertext(), e.getKeyId());
        return new AuthHeader(e.getTokenHeader(), plain);
    }

    private AuthHeader resolveOAuth2(final String interfaceId) {
        Optional<String> cached = cache.get(interfaceId);
        if (cached.isPresent()) {
            return new AuthHeader("Authorization", "Bearer " + cached.get());
        }
        CallbackCredentialEntity e = repo.findByInterfaceId(interfaceId)
            .orElseThrow(() -> new CallbackCredentialMissingException(
                "OAUTH2 credential missing for interfaceId=" + interfaceId));
        String clientId = facade.decrypt(e.getOauthClientIdCiphertext(), e.getKeyId());
        String clientSecret = facade.decrypt(e.getOauthClientSecretCiphertext(), e.getKeyId());
        OAuth2TokenResponse resp = oauthClient.fetchToken(
            e.getOauthTokenEndpoint(), clientId, clientSecret, e.getOauthScope());
        cache.put(interfaceId, resp.accessToken(),
                  Duration.ofSeconds(resp.expiresIn() - OAUTH_SAFETY_MARGIN_SECONDS));
        return new AuthHeader("Authorization", "Bearer " + resp.accessToken());
    }

    public record AuthHeader(String name, String value) { }
}

// Exception
public class CallbackCredentialMissingException extends RuntimeException {
    public CallbackCredentialMissingException(String m) { super(m); }
}
```

- [ ] **Step 3: 修改 CallbackHttpClient.buildRequest — 接通 resolver**

```java
// CallbackHttpClient 增加注入 + 替换 scaffold 逻辑
private final CallbackCredentialResolver credentialResolver;

public CallbackHttpClient(final CallbackCredentialResolver credentialResolver) {
    this.httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1).build();
    this.credentialResolver = credentialResolver;
}

private HttpRequest buildRequest(final SubOutputInterface target, final String payloadJson) {
    final HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create(target.getInterfaceUrl()))
            .timeout(Duration.ofSeconds(target.getTimeoutSeconds()))
            .header("Content-Type", CONTENT_TYPE_JSON)
            .POST(HttpRequest.BodyPublishers.ofString(payloadJson, StandardCharsets.UTF_8));

    credentialResolver.resolveAuthHeader(target).ifPresent(h ->
        builder.header(h.name(), h.value()));

    return builder.build();
}

// 401 处理（在 post() 方法）— invalidate token cache + 一次重试
public CallbackResult post(final SubOutputInterface target, final String payloadJson) {
    CallbackResult result = sendOnce(target, payloadJson);
    if (result.statusCode() == 401 && target.getAuthType() == InterfaceAuthType.OAUTH2) {
        LOG.warn("OAuth2 token rejected, invalidating cache and retrying once interfaceId={}",
                LogSanitizer.sanitize(target.getInterfaceId()));
        credentialResolver.invalidateOAuthToken(target.getInterfaceId());
        result = sendOnce(target, payloadJson);
    }
    return result;
}

private CallbackResult sendOnce(final SubOutputInterface target, final String payloadJson) {
    final HttpRequest request = buildRequest(target, payloadJson);
    try {
        final HttpResponse<Void> response = httpClient.send(request,
                HttpResponse.BodyHandlers.discarding());
        // ... existing 2xx / non-2xx 处理 ...
    } catch (...) { ... }
}
```

- [ ] **Step 4: 跑 Resolver + HttpClient + 全 fep-web 测试**

```
./mvnw -pl fep-web test -am
```

- [ ] **Step 5: Commit**

```bash
git add fep-web/src/main/java/com/puchain/fep/web/callback/credential/service/ \
        fep-web/src/main/java/com/puchain/fep/web/callback/http/CallbackHttpClient.java \
        fep-web/src/test/java/com/puchain/fep/web/callback/credential/service/
git commit -m "feat(web): CallbackCredentialResolver + HttpClient 接通真凭证 + 401 重试

T6 of Callback Phase 2b — resolves NONE/TOKEN/OAUTH2 per interface,
caches OAuth2 access_token (TTL = expires_in - 30s), invalidates on 401
and retries once.

AI-Generated: claude-code
Reviewed-By: pending"
```

---

### Task T7 — 凭证 admin Service + Controller + DTO（AI / Mode A）

**Files**:
- Create: `fep-web/src/main/java/com/puchain/fep/web/callback/credential/service/CallbackCredentialAdminService.java`
- Create: `fep-web/src/main/java/com/puchain/fep/web/callback/credential/controller/CallbackCredentialController.java`
- Create: `fep-web/src/main/java/com/puchain/fep/web/callback/credential/dto/*.java`
- Create: `fep-web/src/test/java/com/puchain/fep/web/callback/credential/service/CallbackCredentialAdminServiceTest.java`
- Create: `fep-web/src/test/java/com/puchain/fep/web/callback/credential/controller/CallbackCredentialControllerIT.java`

- [ ] **Step 1: DTO**

```java
// CredentialCreateRequest
public class CredentialCreateRequest {
    @NotBlank private String interfaceId;
    @NotNull  private InterfaceAuthType authType;
    private String token;             // TOKEN 用，明文 POST，server 端加密
    private String tokenHeader;       // 可选，默认 Authorization
    private String oauthClientId;
    private String oauthClientSecret;
    private String oauthTokenEndpoint;
    private String oauthScope;
    // getters/setters
}

// CredentialUpdateRequest（partial update — 字段为 null = 不修改对应密文列）
public class CredentialUpdateRequest {
    // 同 Create 但全 Optional
}

// CredentialResponse（不回显任何 ciphertext / decrypted）
public class CredentialResponse {
    private String credentialId;
    private String interfaceId;
    private InterfaceAuthType authType;
    private String tokenHeader;
    private String oauthTokenEndpoint;
    private String oauthScope;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime rotatedAt;
    // mask 标记
    private boolean tokenConfigured;
    private boolean oauthClientIdConfigured;
    private boolean oauthClientSecretConfigured;

    public static CredentialResponse from(CallbackCredentialEntity e) { /* fill */ }
}
```

- [ ] **Step 2: AdminService 测试**

```java
@ExtendWith(MockitoExtension.class)
class CallbackCredentialAdminServiceTest {
    @Mock CallbackCredentialRepository repo;
    @Mock CredentialEncryptionFacade facade;
    @Mock OAuth2TokenCache cache;
    @InjectMocks CallbackCredentialAdminService svc;

    @Test
    void createTokenEncryptsAndPersists() {
        var req = new CredentialCreateRequest();
        req.setInterfaceId("IF-001");
        req.setAuthType(InterfaceAuthType.TOKEN);
        req.setToken("plain-tok");
        when(facade.encrypt("plain-tok")).thenReturn(
            new CredentialEncryptionFacade.EncryptedCredential(new byte[]{9}, "KEY-V1"));

        CredentialResponse resp = svc.create(req);

        ArgumentCaptor<CallbackCredentialEntity> cap =
            ArgumentCaptor.forClass(CallbackCredentialEntity.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getTokenCiphertext()).containsExactly(9);
        assertThat(resp.isTokenConfigured()).isTrue();
    }

    @Test
    void createOauth2EncryptsBothIdAndSecret() {
        var req = new CredentialCreateRequest();
        req.setInterfaceId("IF-001");
        req.setAuthType(InterfaceAuthType.OAUTH2);
        req.setOauthClientId("clid");
        req.setOauthClientSecret("csec");
        req.setOauthTokenEndpoint("https://idp/token");
        when(facade.encrypt("clid")).thenReturn(
            new CredentialEncryptionFacade.EncryptedCredential(new byte[]{1}, "KEY-V1"));
        when(facade.encrypt("csec")).thenReturn(
            new CredentialEncryptionFacade.EncryptedCredential(new byte[]{2}, "KEY-V1"));

        svc.create(req);

        ArgumentCaptor<CallbackCredentialEntity> cap =
            ArgumentCaptor.forClass(CallbackCredentialEntity.class);
        verify(repo).save(cap.capture());
        assertThat(cap.getValue().getOauthClientIdCiphertext()).containsExactly(1);
        assertThat(cap.getValue().getOauthClientSecretCiphertext()).containsExactly(2);
    }

    @Test
    void updatePartialKeepsExistingFields() {
        var existing = CallbackCredentialEntity.newOauth("IF-001",
            new byte[]{1}, new byte[]{2}, "https://idp/token", "read", "KEY-V1");
        when(repo.findByInterfaceId("IF-001")).thenReturn(Optional.of(existing));
        var req = new CredentialUpdateRequest();
        // only update client_secret; client_id null
        req.setOauthClientSecret("new-csec");
        when(facade.encrypt("new-csec")).thenReturn(
            new CredentialEncryptionFacade.EncryptedCredential(new byte[]{99}, "KEY-V1"));

        svc.update("IF-001", req);

        assertThat(existing.getOauthClientIdCiphertext()).containsExactly(1);  // unchanged
        assertThat(existing.getOauthClientSecretCiphertext()).containsExactly(99);  // updated
        assertThat(existing.getRotatedAt()).isNotNull();
        verify(cache).invalidate("IF-001");
    }

    @Test
    void getByInterfaceIdReturnsResponseWithoutCiphertext() {
        var entity = CallbackCredentialEntity.newToken("IF-001",
            new byte[]{1,2,3}, "Authorization", "KEY-V1");
        when(repo.findByInterfaceId("IF-001")).thenReturn(Optional.of(entity));

        CredentialResponse resp = svc.get("IF-001");

        // response 不应含 ciphertext 字段（DTO 设计上没有）
        assertThat(resp.getCredentialId()).isEqualTo(entity.getCredentialId());
        assertThat(resp.isTokenConfigured()).isTrue();
    }

    @Test
    void deleteRemovesAndInvalidatesCache() {
        when(repo.findByInterfaceId("IF-001")).thenReturn(Optional.of(
            CallbackCredentialEntity.newToken("IF-001", new byte[]{1}, null, "KEY-V1")));
        svc.delete("IF-001");
        verify(repo).deleteByInterfaceId("IF-001");
        verify(cache).invalidate("IF-001");
    }
}
```

- [ ] **Step 3: AdminService 实现**（**B3 修订: 加 FepErrorCode + FepBusinessException 显式 import**）

```java
import com.puchain.fep.common.domain.FepErrorCode;             // B3: 包路径实测 .common.domain
import com.puchain.fep.common.exception.FepBusinessException;  // B3: 包路径实测 .common.exception

@Service
@Transactional
public class CallbackCredentialAdminService {

    private final CallbackCredentialRepository repo;
    private final CredentialEncryptionFacade facade;
    private final OAuth2TokenCache tokenCache;

    public CallbackCredentialAdminService(CallbackCredentialRepository repo,
            CredentialEncryptionFacade facade, OAuth2TokenCache tokenCache) {
        this.repo = repo; this.facade = facade; this.tokenCache = tokenCache;
    }

    public CredentialResponse create(final CredentialCreateRequest req) {
        if (repo.findByInterfaceId(req.getInterfaceId()).isPresent()) {
            throw new FepBusinessException(FepErrorCode.BIZ_5001,
                "credential already exists for interfaceId=" + req.getInterfaceId());
        }
        CallbackCredentialEntity entity = switch (req.getAuthType()) {
            case TOKEN -> {
                var enc = facade.encrypt(req.getToken());
                yield CallbackCredentialEntity.newToken(req.getInterfaceId(),
                    enc.ciphertext(), req.getTokenHeader(), enc.keyId());
            }
            case OAUTH2 -> {
                var encId = facade.encrypt(req.getOauthClientId());
                var encSec = facade.encrypt(req.getOauthClientSecret());
                yield CallbackCredentialEntity.newOauth(req.getInterfaceId(),
                    encId.ciphertext(), encSec.ciphertext(),
                    req.getOauthTokenEndpoint(), req.getOauthScope(), encId.keyId());
            }
            case NONE -> throw new FepBusinessException(FepErrorCode.BIZ_5001,
                "NONE authType has no credential to persist");
        };
        repo.save(entity);
        return CredentialResponse.from(entity);
    }

    public CredentialResponse update(final String interfaceId,
                                      final CredentialUpdateRequest req) {
        CallbackCredentialEntity e = repo.findByInterfaceId(interfaceId)
            .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                "credential not found, interfaceId=" + interfaceId));
        byte[] newTokenCipher = req.getToken() != null
            ? facade.encrypt(req.getToken()).ciphertext() : null;
        byte[] newClientIdCipher = req.getOauthClientId() != null
            ? facade.encrypt(req.getOauthClientId()).ciphertext() : null;
        byte[] newClientSecretCipher = req.getOauthClientSecret() != null
            ? facade.encrypt(req.getOauthClientSecret()).ciphertext() : null;
        e.rotate(newTokenCipher, newClientIdCipher, newClientSecretCipher, "KEY-V1");
        // partial update 非密文字段
        if (req.getTokenHeader() != null) e.setTokenHeader(req.getTokenHeader());
        if (req.getOauthTokenEndpoint() != null) e.setOauthTokenEndpoint(req.getOauthTokenEndpoint());
        if (req.getOauthScope() != null) e.setOauthScope(req.getOauthScope());
        tokenCache.invalidate(interfaceId);
        return CredentialResponse.from(e);
    }

    @Transactional(readOnly = true)
    public CredentialResponse get(final String interfaceId) {
        return repo.findByInterfaceId(interfaceId).map(CredentialResponse::from)
            .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                "credential not found, interfaceId=" + interfaceId));
    }

    @Transactional(readOnly = true)
    public List<CredentialResponse> list() {
        return repo.findAll().stream().map(CredentialResponse::from).toList();
    }

    public void delete(final String interfaceId) {
        repo.findByInterfaceId(interfaceId).ifPresent(e -> {
            repo.deleteByInterfaceId(interfaceId);
            tokenCache.invalidate(interfaceId);
        });
    }
}
```

- [ ] **Step 4: Controller**

```java
@RestController
@RequestMapping("/api/callback/credentials")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Callback Credentials", description = "凭证管理（密文不回显）")
public class CallbackCredentialController {

    private final CallbackCredentialAdminService svc;

    public CallbackCredentialController(CallbackCredentialAdminService svc) {
        this.svc = svc;
    }

    @PostMapping
    @Operation(summary = "新建凭证")
    public ApiResult<CredentialResponse> create(@Valid @RequestBody CredentialCreateRequest req) {
        return ApiResult.success(svc.create(req));
    }

    @GetMapping("/{interfaceId}")
    @Operation(summary = "查询凭证 (不回显密文)")
    public ApiResult<CredentialResponse> get(@PathVariable String interfaceId) {
        return ApiResult.success(svc.get(interfaceId));
    }

    @GetMapping
    @Operation(summary = "列表")
    public ApiResult<List<CredentialResponse>> list() {
        return ApiResult.success(svc.list());
    }

    @PutMapping("/{interfaceId}")
    @Operation(summary = "更新（partial — 字段空=保留原值，密文字段非空=轮换）")
    public ApiResult<CredentialResponse> update(@PathVariable String interfaceId,
            @Valid @RequestBody CredentialUpdateRequest req) {
        return ApiResult.success(svc.update(interfaceId, req));
    }

    @DeleteMapping("/{interfaceId}")
    @Operation(summary = "删除")
    public ApiResult<Void> delete(@PathVariable String interfaceId) {
        svc.delete(interfaceId);
        return ApiResult.success();
    }
}
```

- [ ] **Step 5: Controller IT — Spring Security + @WebMvcTest**

```java
@WebMvcTest(CallbackCredentialController.class)
class CallbackCredentialControllerIT {
    @Autowired MockMvc mvc;
    @MockBean CallbackCredentialAdminService svc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void postCreateReturnsResponseWithoutCiphertext() throws Exception {
        var resp = new CredentialResponse();
        resp.setCredentialId("CRED-001");
        resp.setInterfaceId("IF-001");
        resp.setTokenConfigured(true);
        when(svc.create(any())).thenReturn(resp);

        mvc.perform(post("/api/callback/credentials")
                .contentType(APPLICATION_JSON)
                .content("""
                    {"interfaceId":"IF-001","authType":"TOKEN","token":"plain"}"""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.credentialId").value("CRED-001"))
            .andExpect(jsonPath("$.data.tokenConfigured").value(true))
            .andExpect(jsonPath("$.data.token").doesNotExist())  // 密文不回显
            .andExpect(jsonPath("$.data.tokenCiphertext").doesNotExist());
    }

    @Test
    @WithMockUser(roles = "USER")  // non-admin
    void nonAdminGetReturns403() throws Exception {
        mvc.perform(get("/api/callback/credentials/IF-001"))
            .andExpect(status().isForbidden());
    }
}
```

- [ ] **Step 6: 跑 T7 全测 + fep-web -am**

```
./mvnw -pl fep-web test -am
```

- [ ] **Step 7: Commit**

```bash
git add fep-web/src/main/java/com/puchain/fep/web/callback/credential/service/CallbackCredentialAdminService.java \
        fep-web/src/main/java/com/puchain/fep/web/callback/credential/controller/ \
        fep-web/src/main/java/com/puchain/fep/web/callback/credential/dto/ \
        fep-web/src/test/java/com/puchain/fep/web/callback/credential/service/CallbackCredentialAdminServiceTest.java \
        fep-web/src/test/java/com/puchain/fep/web/callback/credential/controller/
git commit -m "feat(web): Credential admin service + REST controller + DTO (密文不回显)

T7 of Callback Phase 2b — admin CRUD with @PreAuthorize ROLE_ADMIN;
update is partial (null fields preserved); ciphertext fields never in response;
update invalidates OAuth2 token cache to force re-fetch with new credentials.

AI-Generated: claude-code
Reviewed-By: pending"
```

---

### Task T8 — V31 ALTER + CallbackQueueEntity 扩展 3 字段（AI / Mode A）

**Files**:
- Create: `fep-web/src/main/resources/db/migration/V31__callback_queue_replay_link.sql`
- Modify: `fep-web/src/main/java/com/puchain/fep/web/callback/domain/CallbackQueueEntity.java`
- Modify: `fep-web/src/main/java/com/puchain/fep/web/callback/repository/CallbackQueueRepository.java`
- Modify: `fep-web/src/test/java/com/puchain/fep/web/callback/repository/CallbackQueueRepositoryTest.java`

- [ ] **Step 1: V31 SQL**

```sql
-- Callback Phase 2b T8: DLQ 复制重放审计链（金融审计）
ALTER TABLE callback_queue ADD COLUMN original_dlq_id VARCHAR(64) NULL;
ALTER TABLE callback_queue ADD COLUMN replayed_by     VARCHAR(64) NULL;
ALTER TABLE callback_queue ADD COLUMN replayed_at     TIMESTAMP NULL;
CREATE INDEX idx_callback_queue_original_dlq ON callback_queue(original_dlq_id);
```

- [ ] **Step 2: CallbackQueueEntity 加 3 字段 + factory `copyForReplay` + transition method `initializeReplayState`**

> **B4 修订（v0.2）**: `CallbackQueueStatus` 实测为 `public final class` String constants 非 enum；`CallbackQueueEntity.status` 字段是 `private String` 无 setter（仅 named transition methods）。原 v0.1 `copyForReplay` 内 `copy.status = CallbackQueueStatus.PENDING` 走私有字段直接访问 — Java 同类合法，但与 entity 既有 "transition method only" 设计哲学冲突，ArchUnit/code review 可能拒收。v0.2 改：在 entity 内加 package-private `initializeReplayState(...)` transition method 集中处理复制初始化，static factory 调用之。

```java
@Column(name = "original_dlq_id", length = 64)
private String originalDlqId;

@Column(name = "replayed_by", length = 64)
private String replayedBy;

@Column(name = "replayed_at")
private LocalDateTime replayedAt;

/**
 * 复制重放：以原 DEAD_LETTER 行为模板创建新 PENDING 行，关联 original_dlq_id。
 *
 * <p>B4 修订（v0.2）: 内部使用 {@link #initializeReplayState} transition method
 * 而非直接私有字段写，与 entity 既有 {@code markAsDone / markAsSending / markDeadLetter}
 * 等 transition method 设计风格一致。</p>
 */
public static CallbackQueueEntity copyForReplay(final CallbackQueueEntity original,
                                                 final String adminUserId) {
    if (!CallbackQueueStatus.DEAD_LETTER.equals(original.getStatus())) {
        throw new IllegalStateException(
            "only DEAD_LETTER rows replayable, actual status=" + original.getStatus());
    }
    final CallbackQueueEntity copy = new CallbackQueueEntity(
        original.getIdempotencyKey() + "-RPL-" + System.currentTimeMillis(),
        original.getTargetInterfaceId(), original.getMsgNo(), original.getPayloadJson());
    copy.initializeReplayState(original.getQueueId(), adminUserId);
    return copy;
}

/**
 * Replay 初始化 transition method（package-private, only called by {@link #copyForReplay}）。
 * <p>B4 修订（v0.2）: 集中处理 status / retryCount / 3 个 replay 字段初始化 + create/update time，
 * 替代原 v0.1 在 static factory 内直接私有字段写。</p>
 */
void initializeReplayState(final String originalDlqId, final String adminUserId) {
    this.status = CallbackQueueStatus.PENDING;
    this.retryCount = 0;
    this.originalDlqId = originalDlqId;
    this.replayedBy = adminUserId;
    this.replayedAt = LocalDateTime.now();
    this.createTime = this.replayedAt;
    this.updateTime = this.replayedAt;
}

// getters
public String getOriginalDlqId() { return originalDlqId; }
public String getReplayedBy() { return replayedBy; }
public LocalDateTime getReplayedAt() { return replayedAt; }
```

- [ ] **Step 3: Repository 加 `findStaleSending` + `findDeadLetterPage` + `findByOriginalDlqId`**

```java
@Query("""
    SELECT q FROM CallbackQueueEntity q
    WHERE q.status = 'SENDING' AND q.claimedAt < :threshold
    """)
List<CallbackQueueEntity> findStaleSending(@Param("threshold") LocalDateTime threshold);

@Query("""
    SELECT q FROM CallbackQueueEntity q
    WHERE q.status = 'DEAD_LETTER'
    ORDER BY q.updateTime DESC
    """)
List<CallbackQueueEntity> findDeadLetter(Pageable pageable);

List<CallbackQueueEntity> findByOriginalDlqId(String originalDlqId);
```

- [ ] **Step 4: 测试**

```java
@Test
void copyForReplayCreatesNewRowLinkingOriginal() {
    var dead = createDeadLetterRow("IF-001", "tok-001");

    var replay = CallbackQueueEntity.copyForReplay(dead, "admin-user-x");

    assertThat(replay.getQueueId()).isNotEqualTo(dead.getQueueId());
    assertThat(replay.getStatus()).isEqualTo(CallbackQueueStatus.PENDING);
    assertThat(replay.getRetryCount()).isZero();
    assertThat(replay.getOriginalDlqId()).isEqualTo(dead.getQueueId());
    assertThat(replay.getReplayedBy()).isEqualTo("admin-user-x");
    assertThat(replay.getReplayedAt()).isNotNull();
    assertThat(replay.getTargetInterfaceId()).isEqualTo(dead.getTargetInterfaceId());
    assertThat(replay.getPayloadJson()).isEqualTo(dead.getPayloadJson());
}

@Test
void copyForReplayNonDeadLetterThrows() {
    var pending = createPendingRow("IF-001");
    assertThatThrownBy(() -> CallbackQueueEntity.copyForReplay(pending, "u"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("DEAD_LETTER");
}

@Test
void findStaleSendingFiltersByClaimedAt() {
    var stale = createSendingRow("IF-001", LocalDateTime.now().minusMinutes(10));
    var fresh = createSendingRow("IF-002", LocalDateTime.now().minusMinutes(1));
    repo.saveAll(List.of(stale, fresh));
    var found = repo.findStaleSending(LocalDateTime.now().minusMinutes(5));
    assertThat(found).hasSize(1);
    assertThat(found.get(0).getQueueId()).isEqualTo(stale.getQueueId());
}
```

- [ ] **Step 5: 跑测试 + 全 fep-web**

```
./mvnw -pl fep-web test -am
```

- [ ] **Step 6: Commit**

```bash
git add fep-web/src/main/resources/db/migration/V31__callback_queue_replay_link.sql \
        fep-web/src/main/java/com/puchain/fep/web/callback/domain/CallbackQueueEntity.java \
        fep-web/src/main/java/com/puchain/fep/web/callback/repository/CallbackQueueRepository.java \
        fep-web/src/test/java/com/puchain/fep/web/callback/repository/CallbackQueueRepositoryTest.java \
        fep-web/src/test/java/com/puchain/fep/web/callback/domain/
git commit -m "feat(web): V31 callback_queue replay link + Entity copyForReplay + repo queries

T8 of Callback Phase 2b — adds original_dlq_id / replayed_by / replayed_at for
audit chain; copyForReplay factory enforces DEAD_LETTER source; adds
findStaleSending + findDeadLetter queries.

AI-Generated: claude-code
Reviewed-By: pending"
```

---

### Task T9 — CallbackReplayService + DlqController `/replay`（AI / Mode A）

**Files**:
- Create: `fep-web/src/main/java/com/puchain/fep/web/callback/dlq/service/CallbackReplayService.java`
- Create: `fep-web/src/main/java/com/puchain/fep/web/callback/dlq/controller/CallbackDlqController.java`
- Create: `fep-web/src/main/java/com/puchain/fep/web/callback/dlq/dto/*.java`
- Create: 测试

- [ ] **Step 1: ReplayService 测试**

```java
@ExtendWith(MockitoExtension.class)
class CallbackReplayServiceTest {
    @Mock CallbackQueueRepository repo;
    @InjectMocks CallbackReplayService svc;

    @Test
    void replayCreatesCopyAndLeavesOriginalUnchanged() {
        var dead = createDeadLetterRow("D1", "IF-001");
        when(repo.findById("D1")).thenReturn(Optional.of(dead));

        DlqReplayResponse resp = svc.replay("D1", "admin-user-x");

        ArgumentCaptor<CallbackQueueEntity> cap =
            ArgumentCaptor.forClass(CallbackQueueEntity.class);
        verify(repo).save(cap.capture());
        var copy = cap.getValue();
        assertThat(copy.getOriginalDlqId()).isEqualTo("D1");
        assertThat(copy.getStatus()).isEqualTo("PENDING");
        assertThat(copy.getRetryCount()).isZero();
        assertThat(copy.getReplayedBy()).isEqualTo("admin-user-x");
        // original 不变
        assertThat(dead.getStatus()).isEqualTo("DEAD_LETTER");
        assertThat(resp.newQueueId()).isEqualTo(copy.getQueueId());
        assertThat(resp.originalDlqId()).isEqualTo("D1");
    }

    @Test
    void replayNonExistentThrowsBiz() {
        when(repo.findById("D-NOPE")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> svc.replay("D-NOPE", "u"))
            .isInstanceOf(FepBusinessException.class)
            .extracting("errorCode").isEqualTo(FepErrorCode.BIZ_5001);
    }

    @Test
    void replayNonDeadLetterThrowsBiz() {
        var pending = createPendingRow("P1", "IF-001");
        when(repo.findById("P1")).thenReturn(Optional.of(pending));
        assertThatThrownBy(() -> svc.replay("P1", "u"))
            .isInstanceOf(FepBusinessException.class);
    }

    @Test
    void listDeadLetterReturnsResponses() {
        var d1 = createDeadLetterRow("D1", "IF-001");
        var d2 = createDeadLetterRow("D2", "IF-002");
        when(repo.findDeadLetter(any())).thenReturn(List.of(d1, d2));
        List<DlqEntryResponse> result = svc.list(PageRequest.of(0, 20));
        assertThat(result).hasSize(2);
        assertThat(result.get(0).queueId()).isEqualTo("D1");
    }

    @Test
    void findReplayChainReturnsLinkedEntries() {
        var d1 = createDeadLetterRow("D1", "IF-001");
        var d2 = createDeadLetterRow("D2", "IF-001");
        d2.setOriginalDlqId("D1");  // d2 was replayed from d1
        when(repo.findByOriginalDlqId("D1")).thenReturn(List.of(d2));
        List<DlqEntryResponse> chain = svc.findReplayChain("D1");
        assertThat(chain).hasSize(1);
    }
}
```

- [ ] **Step 2: ReplayService 实现**（**B3 修订: 显式 FepErrorCode + FepBusinessException import**）

```java
import com.puchain.fep.common.domain.FepErrorCode;             // B3: 包路径实测 .common.domain
import com.puchain.fep.common.exception.FepBusinessException;  // B3: 包路径实测 .common.exception

@Service
@Transactional
public class CallbackReplayService {

    private final CallbackQueueRepository repo;

    public CallbackReplayService(CallbackQueueRepository repo) { this.repo = repo; }

    public DlqReplayResponse replay(final String dlqId, final String adminUserId) {
        CallbackQueueEntity dead = repo.findById(dlqId)
            .orElseThrow(() -> new FepBusinessException(FepErrorCode.BIZ_5001,
                "DLQ entry not found, id=" + dlqId));
        if (!CallbackQueueStatus.DEAD_LETTER.equals(dead.getStatus())) {
            throw new FepBusinessException(FepErrorCode.BIZ_5001,
                "only DEAD_LETTER replayable, actual status=" + dead.getStatus());
        }
        CallbackQueueEntity copy = CallbackQueueEntity.copyForReplay(dead, adminUserId);
        repo.save(copy);
        return new DlqReplayResponse(copy.getQueueId(), dead.getQueueId(),
            copy.getReplayedAt());
    }

    @Transactional(readOnly = true)
    public List<DlqEntryResponse> list(final Pageable pageable) {
        return repo.findDeadLetter(pageable).stream()
            .map(DlqEntryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<DlqEntryResponse> findReplayChain(final String dlqId) {
        return repo.findByOriginalDlqId(dlqId).stream()
            .map(DlqEntryResponse::from).toList();
    }
}
```

- [ ] **Step 3: DTOs**

```java
public record DlqEntryResponse(String queueId, String targetInterfaceId, String msgNo,
        String status, int retryCount, String lastError, LocalDateTime updateTime,
        String originalDlqId, String replayedBy, LocalDateTime replayedAt) {
    public static DlqEntryResponse from(CallbackQueueEntity e) {
        return new DlqEntryResponse(e.getQueueId(), e.getTargetInterfaceId(), e.getMsgNo(),
            e.getStatus(), e.getRetryCount(), e.getLastError(), e.getUpdateTime(),
            e.getOriginalDlqId(), e.getReplayedBy(), e.getReplayedAt());
    }
}

public record DlqReplayResponse(String newQueueId, String originalDlqId,
        LocalDateTime replayedAt) { }
```

- [ ] **Step 4: Controller**

```java
@RestController
@RequestMapping("/api/callback/dlq")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Callback DLQ", description = "死信队列查看 + 复制重放")
public class CallbackDlqController {

    private final CallbackReplayService svc;

    public CallbackDlqController(CallbackReplayService svc) { this.svc = svc; }

    @GetMapping
    @Operation(summary = "DLQ 列表（DEAD_LETTER）")
    public ApiResult<List<DlqEntryResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResult.success(svc.list(PageRequest.of(page, size)));
    }

    @PostMapping("/{dlqId}/replay")
    @Operation(summary = "复制重放（原 DEAD 行保留作审计证据）")
    public ApiResult<DlqReplayResponse> replay(@PathVariable String dlqId,
            Authentication auth) {
        return ApiResult.success(svc.replay(dlqId, auth.getName()));
    }

    @GetMapping("/{dlqId}/chain")
    @Operation(summary = "查看从该 dlqId 衍生的重放链")
    public ApiResult<List<DlqEntryResponse>> chain(@PathVariable String dlqId) {
        return ApiResult.success(svc.findReplayChain(dlqId));
    }
}
```

- [ ] **Step 5: Controller IT**

```java
@WebMvcTest(CallbackDlqController.class)
class CallbackDlqControllerIT {
    @Autowired MockMvc mvc;
    @MockBean CallbackReplayService svc;

    @Test
    @WithMockUser(roles = "ADMIN", username = "admin-x")
    void postReplayPassesUsernameToService() throws Exception {
        when(svc.replay(eq("D1"), eq("admin-x"))).thenReturn(
            new DlqReplayResponse("NEW-001", "D1", LocalDateTime.now()));
        mvc.perform(post("/api/callback/dlq/D1/replay"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.newQueueId").value("NEW-001"))
            .andExpect(jsonPath("$.data.originalDlqId").value("D1"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void nonAdminReplayReturns403() throws Exception {
        mvc.perform(post("/api/callback/dlq/D1/replay"))
            .andExpect(status().isForbidden());
    }
}
```

- [ ] **Step 6: 跑测试 + 全 fep-web -am**

- [ ] **Step 7: Commit**

```bash
git add fep-web/src/main/java/com/puchain/fep/web/callback/dlq/ \
        fep-web/src/test/java/com/puchain/fep/web/callback/dlq/
git commit -m "feat(web): CallbackReplayService + DLQ admin controller (复制重放审计链)

T9 of Callback Phase 2b — DEAD_LETTER → new PENDING copy with original_dlq_id;
list/chain queries; ROLE_ADMIN only; original row immutable as audit evidence.

AI-Generated: claude-code
Reviewed-By: pending"
```

---

### Task T10 — CallbackDeadLetterEvent + RetryHandler publishEvent（AI / Mode A）

**Files**:
- Create: `fep-web/src/main/java/com/puchain/fep/web/callback/dlq/event/CallbackDeadLetterEvent.java`
- Modify: `fep-web/src/main/java/com/puchain/fep/web/callback/runner/CallbackRetryHandler.java`
- Modify: 测试

- [ ] **Step 1: Event 定义**

```java
package com.puchain.fep.web.callback.dlq.event;

import java.time.LocalDateTime;

public record CallbackDeadLetterEvent(
    String queueId,
    String targetInterfaceId,
    String msgNo,
    int retryCount,
    String lastError,
    LocalDateTime occurredAt) { }
```

- [ ] **Step 2: 测试 — RetryHandler 在 markDeadLetter 后 publishEvent**

```java
// CallbackRetryHandlerTest 既有 → 加 assertion

@Mock ApplicationEventPublisher publisher;

@Test
void markDeadLetterPublishesEvent() {
    var entity = createSendingEntity("Q1", "IF-001", "9001", 2);
    handler.handleFailure(entity, 400, "bad request");  // 4xx → DEAD_LETTER

    ArgumentCaptor<CallbackDeadLetterEvent> cap =
        ArgumentCaptor.forClass(CallbackDeadLetterEvent.class);
    verify(publisher).publishEvent(cap.capture());
    assertThat(cap.getValue().queueId()).isEqualTo("Q1");
    assertThat(cap.getValue().lastError()).contains("400");
}

@Test
void retryDoesNotPublishEvent() {
    var entity = createSendingEntity("Q1", "IF-001", "9001", 0);
    handler.handleFailure(entity, 500, "server err");  // 5xx → RETRY (count<max)
    verifyNoInteractions(publisher);
}
```

- [ ] **Step 3: RetryHandler 接 publisher**

```java
// CallbackRetryHandler 构造器
private final ApplicationEventPublisher publisher;

public CallbackRetryHandler(/* existing deps */, ApplicationEventPublisher publisher) {
    /* ... */
    this.publisher = publisher;
}

// handleFailure 内：markDeadLetter 调用之后立即 publish
private CallbackFailureOutcome markAndPublishDeadLetter(CallbackQueueEntity entity,
        int newRetryCount, String error) {
    entity.markDeadLetter(newRetryCount, error);
    publisher.publishEvent(new CallbackDeadLetterEvent(
        entity.getQueueId(), entity.getTargetInterfaceId(), entity.getMsgNo(),
        newRetryCount, error, LocalDateTime.now()));
    return CallbackFailureOutcome.DEAD_LETTER;
}

// 替换既有两处 markDeadLetter 调用为 markAndPublishDeadLetter
```

- [ ] **Step 4: 跑测试通过**

```
./mvnw -pl fep-web test -Dtest=CallbackRetryHandlerTest
./mvnw -pl fep-web test -am
```

- [ ] **Step 5: Commit**

```bash
git add fep-web/src/main/java/com/puchain/fep/web/callback/dlq/event/CallbackDeadLetterEvent.java \
        fep-web/src/main/java/com/puchain/fep/web/callback/runner/CallbackRetryHandler.java \
        fep-web/src/test/java/com/puchain/fep/web/callback/runner/CallbackRetryHandlerTest.java
git commit -m "feat(web): CallbackDeadLetterEvent + RetryHandler publishes on markDeadLetter

T10 of Callback Phase 2b — Spring ApplicationEventPublisher publishes
CallbackDeadLetterEvent immediately after markDeadLetter; listeners decoupled.

AI-Generated: claude-code
Reviewed-By: pending"
```

---

### Task T11 — V32 in_app_notification 表 + Entity + Repo（AI / Mode A）

**Files**:
- Create: `fep-web/src/main/resources/db/migration/V32__in_app_notification.sql`
- Create: `fep-web/src/main/java/com/puchain/fep/web/callback/notification/domain/InAppNotificationEntity.java`
- Create: `fep-web/src/main/java/com/puchain/fep/web/callback/notification/repository/InAppNotificationRepository.java`
- Create: 测试

- [ ] **Step 1: V32 SQL**

```sql
-- Callback Phase 2b T11: 站内通知（事件解耦目标，扩展点 EMAIL/SMS Phase 2c）
CREATE TABLE in_app_notification (
  notification_id VARCHAR(32)  NOT NULL,
  user_id         VARCHAR(64)  NOT NULL,
  category        VARCHAR(50)  NOT NULL,
  level           VARCHAR(20)  NOT NULL,
  title           VARCHAR(200) NOT NULL,
  message         VARCHAR(1000) NOT NULL,
  ref_id          VARCHAR(64),
  ref_type        VARCHAR(50),
  is_read         BOOLEAN      NOT NULL DEFAULT FALSE,
  create_time     TIMESTAMP    NOT NULL,
  read_at         TIMESTAMP    NULL,
  PRIMARY KEY (notification_id)
);
CREATE INDEX idx_notification_user_unread ON in_app_notification(user_id, is_read);
CREATE INDEX idx_notification_create_time ON in_app_notification(create_time);
```

- [ ] **Step 2: Entity + factory**

```java
@Entity
@Table(name = "in_app_notification")
public class InAppNotificationEntity {

    @Id @Column(name = "notification_id", length = 32)
    private String notificationId;

    @Column(name = "user_id", nullable = false, length = 64)
    private String userId;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "level", nullable = false, length = 20)
    private String level;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @Column(name = "ref_id", length = 64)
    private String refId;

    @Column(name = "ref_type", length = 50)
    private String refType;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "create_time", nullable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    protected InAppNotificationEntity() { /* JPA */ }

    public static InAppNotificationEntity of(String userId, String category, String level,
            String title, String message, String refId, String refType) {
        var e = new InAppNotificationEntity();
        e.notificationId = UUID.randomUUID().toString().replace("-", "");
        e.userId = userId; e.category = category; e.level = level;
        e.title = title; e.message = message;
        e.refId = refId; e.refType = refType;
        e.read = false;
        e.createTime = LocalDateTime.now();
        return e;
    }

    public void markRead() {
        this.read = true;
        this.readAt = LocalDateTime.now();
    }
    // getters
}
```

- [ ] **Step 3: Repository**

```java
public interface InAppNotificationRepository extends JpaRepository<InAppNotificationEntity, String> {
    List<InAppNotificationEntity> findByUserIdAndReadFalseOrderByCreateTimeDesc(String userId);
    long countByUserIdAndReadFalse(String userId);
    Page<InAppNotificationEntity> findByUserIdOrderByCreateTimeDesc(String userId, Pageable p);
}
```

- [ ] **Step 4: 测试 + Commit**

```bash
git add fep-web/src/main/resources/db/migration/V32__in_app_notification.sql \
        fep-web/src/main/java/com/puchain/fep/web/callback/notification/domain/ \
        fep-web/src/main/java/com/puchain/fep/web/callback/notification/repository/ \
        fep-web/src/test/java/com/puchain/fep/web/callback/notification/
git commit -m "feat(web): V32 in_app_notification + Entity + Repository

T11 of Callback Phase 2b — generic in-app notification table for
callback DLQ alerts (and Phase 2c EMAIL/SMS extension targets).

AI-Generated: claude-code
Reviewed-By: pending"
```

---

### Task T12 — InAppNotificationListener + Service + Controller（AI / Mode A）

**Files**:
- Create: `.../callback/notification/listener/InAppNotificationListener.java`
- Create: `.../callback/notification/service/InAppNotificationService.java`
- Create: `.../callback/notification/controller/InAppNotificationController.java`
- Create: 测试

**B2 修订**: 原 v0.1 使用 `SysUserRepository.findByRoleCode("ADMIN")` — 该方法**不存在**（red line `feedback_plan_must_grep_actual_api` 实质违反）。改为两步查询：
1. `SysRoleRepository.findByRoleCode("ADMIN") → Optional<SysRole>` → 取 `roleId`
2. `SysUserRoleRepository.findByRoleId(roleId) → List<SysUserRole>` → 收集 `userId` set
3. `SysUserRepository.findAllById(userIds) → List<SysUser>`

包路径实测: `SysRoleRepository` 在 `com.puchain.fep.web.sysmgmt.role.repository`；`SysUserRoleRepository` 在 `com.puchain.fep.web.sysmgmt.rel.repository`（**`.rel`，非 `.userrole`**）。

- [ ] **Step 1: Listener 测试**

```java
import com.puchain.fep.web.sysmgmt.role.repository.SysRoleRepository;
import com.puchain.fep.web.sysmgmt.role.domain.SysRole;
import com.puchain.fep.web.sysmgmt.rel.repository.SysUserRoleRepository;
import com.puchain.fep.web.sysmgmt.rel.domain.SysUserRole;
import com.puchain.fep.web.sysmgmt.user.repository.SysUserRepository;
import com.puchain.fep.web.sysmgmt.user.domain.SysUser;

@ExtendWith(MockitoExtension.class)
class InAppNotificationListenerTest {
    @Mock SysRoleRepository roleRepo;
    @Mock SysUserRoleRepository userRoleRepo;
    @Mock SysUserRepository userRepo;
    @Mock InAppNotificationRepository notifRepo;
    @InjectMocks InAppNotificationListener listener;

    @Test
    void onDeadLetterInsertsOneRowPerAdmin() {
        var adminRole = mockSysRole("ROLE-1", "ADMIN");
        var ur1 = mockSysUserRole("USR-1", "ROLE-1");
        var ur2 = mockSysUserRole("USR-2", "ROLE-1");
        var u1 = mockSysUser("USR-1");
        var u2 = mockSysUser("USR-2");
        when(roleRepo.findByRoleCode("ADMIN")).thenReturn(Optional.of(adminRole));
        when(userRoleRepo.findByRoleId("ROLE-1")).thenReturn(List.of(ur1, ur2));
        when(userRepo.findAllById(List.of("USR-1", "USR-2"))).thenReturn(List.of(u1, u2));

        listener.onDeadLetter(new CallbackDeadLetterEvent(
            "Q1", "IF-001", "9001", 5, "io timeout", LocalDateTime.now()));

        ArgumentCaptor<InAppNotificationEntity> cap =
            ArgumentCaptor.forClass(InAppNotificationEntity.class);
        verify(notifRepo, times(2)).save(cap.capture());
        assertThat(cap.getAllValues()).extracting(InAppNotificationEntity::getUserId)
            .containsExactlyInAnyOrder("USR-1", "USR-2");
        assertThat(cap.getAllValues()).extracting(InAppNotificationEntity::getCategory)
            .allMatch(c -> "CALLBACK_DLQ".equals(c));
        assertThat(cap.getAllValues()).extracting(InAppNotificationEntity::getRefId)
            .allMatch(r -> "Q1".equals(r));
    }

    @Test
    void onDeadLetterWithNoAdminRoleLogsWarn() {
        when(roleRepo.findByRoleCode("ADMIN")).thenReturn(Optional.empty());
        listener.onDeadLetter(new CallbackDeadLetterEvent("Q1","IF-001","9001",5,"e",LocalDateTime.now()));
        verifyNoInteractions(notifRepo);
        verifyNoInteractions(userRoleRepo);
        verifyNoInteractions(userRepo);
    }

    @Test
    void onDeadLetterWithNoAdminUsersLogsWarn() {
        var adminRole = mockSysRole("ROLE-1", "ADMIN");
        when(roleRepo.findByRoleCode("ADMIN")).thenReturn(Optional.of(adminRole));
        when(userRoleRepo.findByRoleId("ROLE-1")).thenReturn(List.of());
        listener.onDeadLetter(new CallbackDeadLetterEvent("Q1","IF-001","9001",5,"e",LocalDateTime.now()));
        verifyNoInteractions(notifRepo);
        verifyNoInteractions(userRepo);
    }

    // N5 v0.3 修订: 3 个 mock helper 由 implementer inline 实现或抽到测试 fixture util。
    // 推荐 inline 风格（避免 helper class drift）:
    private SysRole mockSysRole(String roleId, String roleCode) {
        SysRole r = new SysRole();
        r.setRoleId(roleId);
        r.setRoleCode(roleCode);
        return r;
    }
    private SysUserRole mockSysUserRole(String userId, String roleId) {
        SysUserRole ur = new SysUserRole();
        ur.setUserId(userId);
        ur.setRoleId(roleId);
        return ur;
    }
    private SysUser mockSysUser(String userId) {
        SysUser u = new SysUser();
        u.setUserId(userId);
        return u;
    }
}
```

- [ ] **Step 2: Listener 实现（两步查询）**

```java
import com.puchain.fep.web.sysmgmt.role.repository.SysRoleRepository;
import com.puchain.fep.web.sysmgmt.role.domain.SysRole;
import com.puchain.fep.web.sysmgmt.rel.repository.SysUserRoleRepository;
import com.puchain.fep.web.sysmgmt.rel.domain.SysUserRole;
import com.puchain.fep.web.sysmgmt.user.repository.SysUserRepository;
import com.puchain.fep.web.sysmgmt.user.domain.SysUser;
import com.puchain.fep.common.log.LogSanitizer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Component
@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
    justification = "queueId passed through LogSanitizer.sanitize() prior to LOG.warn")
public class InAppNotificationListener {

    private static final Logger LOG = LoggerFactory.getLogger(InAppNotificationListener.class);
    private static final String ADMIN_ROLE_CODE = "ADMIN";

    private final SysRoleRepository roleRepo;
    private final SysUserRoleRepository userRoleRepo;
    private final SysUserRepository userRepo;
    private final InAppNotificationRepository notifRepo;

    public InAppNotificationListener(SysRoleRepository roleRepo,
            SysUserRoleRepository userRoleRepo, SysUserRepository userRepo,
            InAppNotificationRepository notifRepo) {
        this.roleRepo = roleRepo;
        this.userRoleRepo = userRoleRepo;
        this.userRepo = userRepo;
        this.notifRepo = notifRepo;
    }

    @EventListener
    @Transactional
    public void onDeadLetter(final CallbackDeadLetterEvent ev) {
        // Step 1: find ADMIN role
        Optional<SysRole> adminRole = roleRepo.findByRoleCode(ADMIN_ROLE_CODE);
        if (adminRole.isEmpty()) {
            LOG.warn("DLQ event but ADMIN role not configured, queueId={}",
                LogSanitizer.sanitize(ev.queueId()));
            return;
        }
        // Step 2: find user-role rels
        List<String> adminUserIds = userRoleRepo.findByRoleId(adminRole.get().getRoleId())
            .stream().map(SysUserRole::getUserId).toList();
        if (adminUserIds.isEmpty()) {
            LOG.warn("DLQ event but no users assigned ADMIN role, queueId={}",
                LogSanitizer.sanitize(ev.queueId()));
            return;
        }
        // Step 3: load users + insert notifications
        List<SysUser> admins = userRepo.findAllById(adminUserIds);
        for (SysUser u : admins) {
            notifRepo.save(InAppNotificationEntity.of(
                u.getUserId(),
                "CALLBACK_DLQ",
                "ERROR",
                "回调死信 - " + ev.targetInterfaceId(),
                String.format("queueId=%s msgNo=%s retryCount=%d error=%s",
                    ev.queueId(), ev.msgNo(), ev.retryCount(), ev.lastError()),
                ev.queueId(),
                "CALLBACK_DLQ_ENTRY"));
        }
    }
}
```

> **注**: §2.3 文件结构 §"fep-web — 新增"段引用的 Repository 不变（仅 InAppNotificationRepository 新建）；T12 注入的 3 个既有 Repository (`SysRoleRepository / SysUserRoleRepository / SysUserRepository`) 不在 §2.4 修改清单（仅读取，无 MODIFY）。

- [ ] **Step 3: Service + Controller**

```java
@Service
public class InAppNotificationService {

    private final InAppNotificationRepository repo;

    public InAppNotificationService(InAppNotificationRepository repo) { this.repo = repo; }

    @Transactional(readOnly = true)
    public List<NotificationResponse> listUnread(final String userId) {
        return repo.findByUserIdAndReadFalseOrderByCreateTimeDesc(userId).stream()
            .map(NotificationResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(final String userId) {
        return repo.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markRead(final String notificationId, final String userId) {
        repo.findById(notificationId)
            .filter(n -> userId.equals(n.getUserId()))
            .ifPresent(InAppNotificationEntity::markRead);
    }
}

@RestController
@RequestMapping("/api/notifications")
public class InAppNotificationController {

    private final InAppNotificationService svc;

    @GetMapping("/unread")
    public ApiResult<List<NotificationResponse>> unread(Authentication auth) {
        return ApiResult.success(svc.listUnread(auth.getName()));
    }

    @GetMapping("/unread/count")
    public ApiResult<Long> count(Authentication auth) {
        return ApiResult.success(svc.unreadCount(auth.getName()));
    }

    @PutMapping("/{id}/read")
    public ApiResult<Void> markRead(@PathVariable String id, Authentication auth) {
        svc.markRead(id, auth.getName());
        return ApiResult.success();
    }
}
```

- [ ] **Step 4: 跑测试 + Commit**

```bash
git add fep-web/src/main/java/com/puchain/fep/web/callback/notification/listener/ \
        fep-web/src/main/java/com/puchain/fep/web/callback/notification/service/ \
        fep-web/src/main/java/com/puchain/fep/web/callback/notification/controller/ \
        fep-web/src/main/java/com/puchain/fep/web/callback/notification/dto/ \
        fep-web/src/test/java/com/puchain/fep/web/callback/notification/
git commit -m "feat(web): InAppNotificationListener + Service + Controller (CallbackDeadLetterEvent → admin notify)

T12 of Callback Phase 2b — @EventListener subscribes to CallbackDeadLetterEvent;
inserts one notification row per admin (role=ADMIN); REST endpoints for unread
list / count / mark-read.

AI-Generated: claude-code
Reviewed-By: pending"
```

---

### Task T13 — CallbackStaleReaper @Scheduled（AI / Mode A）

**Files**:
- Create: `.../callback/reaper/CallbackStaleReaper.java`
- Modify: `.../callback/config/CallbackQueueProperties.java`（加 ReaperProperties）
- Create: 测试

- [ ] **Step 1: ReaperProperties 加到 CallbackQueueProperties record**

```java
public record CallbackQueueProperties(
    // existing fields ...
    int batchSize,
    int maxAttempts,
    long baseBackoffMs,
    long maxBackoffMs,
    ReaperProperties reaper) {

    public record ReaperProperties(
        boolean enabled,
        long intervalMs,
        long staleAfterSeconds) { }
}
```

`application.yml`:

```yaml
fep:
  callback:
    # ... existing ...
    reaper:
      enabled: true
      interval-ms: 60000     # 60s scan
      stale-after-seconds: 300  # 5min stale
```

- [ ] **Step 2: Reaper 测试 — TestClock 控时间**

```java
@ExtendWith(MockitoExtension.class)
class CallbackStaleReaperTest {
    @Mock CallbackQueueRepository repo;
    @Mock CallbackQueueProperties props;
    @Mock CallbackQueueProperties.ReaperProperties reaperProps;
    CallbackStaleReaper reaper;
    Clock fixedClock = Clock.fixed(Instant.parse("2026-05-28T12:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        when(props.reaper()).thenReturn(reaperProps);
        when(reaperProps.staleAfterSeconds()).thenReturn(300L);
        reaper = new CallbackStaleReaper(repo, props, fixedClock);
    }

    @Test
    void reapRevertsStaleSendingRows() {
        var stale = mockSendingEntity("Q1", LocalDateTime.parse("2026-05-28T11:54:00"));  // 6min old
        var notStale = mockSendingEntity("Q2", LocalDateTime.parse("2026-05-28T11:58:00"));  // 2min old
        when(repo.findStaleSending(LocalDateTime.parse("2026-05-28T11:55:00")))
            .thenReturn(List.of(stale));

        reaper.reap();

        verify(stale).markAsStaleReclaim();  // assumes method exists
        verify(repo).save(stale);
        verify(repo, never()).save(notStale);
    }

    @Test
    void reapEmptyDoesNothing() {
        when(repo.findStaleSending(any())).thenReturn(List.of());
        reaper.reap();
        verify(repo, never()).save(any());
    }
}
```

- [ ] **Step 3: Reaper 实现**（**N3 修订: enabled flag 改 `@ConditionalOnProperty` Bean 级条件装配，disabled 时 Bean 不创建，避免 @Scheduled 空跑产生噪声日志**）

```java
package com.puchain.fep.web.callback.reaper;

import com.puchain.fep.web.callback.config.CallbackQueueProperties;
import com.puchain.fep.web.callback.domain.CallbackQueueEntity;
import com.puchain.fep.web.callback.repository.CallbackQueueRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Callback stale SENDING row reaper.
 *
 * <p>N3 修订（v0.2）: 用 {@code @ConditionalOnProperty(name = "fep.callback.reaper.enabled",
 * havingValue = "true", matchIfMissing = true)} Bean 级条件装配；disabled 时 Bean 完全不创建，
 * @Scheduled 任务不注册，避免 v0.1 内部 if 短路造成的"始终调度+空跑"噪声日志。</p>
 */
@Component
@ConditionalOnProperty(name = "fep.callback.reaper.enabled", havingValue = "true",
    matchIfMissing = true)
public class CallbackStaleReaper {

    private static final Logger LOG = LoggerFactory.getLogger(CallbackStaleReaper.class);

    private final CallbackQueueRepository repo;
    private final CallbackQueueProperties props;
    private final Clock clock;
    private final Counter revertedCounter;

    public CallbackStaleReaper(CallbackQueueRepository repo,
            CallbackQueueProperties props, Clock clock, MeterRegistry meter) {
        this.repo = repo; this.props = props; this.clock = clock;
        this.revertedCounter = Counter.builder("fep_callback_reaper_reverted")
            .description("stale SENDING rows reverted to PENDING")
            .register(meter);
    }

    @Scheduled(fixedRateString = "${fep.callback.reaper.interval-ms:60000}")
    @Transactional
    public void reap() {
        LocalDateTime threshold = LocalDateTime.now(clock)
            .minusSeconds(props.reaper().staleAfterSeconds());
        List<CallbackQueueEntity> stale = repo.findStaleSending(threshold);
        if (stale.isEmpty()) return;
        LOG.warn("reaper found {} stale SENDING rows older than {}", stale.size(), threshold);
        for (CallbackQueueEntity e : stale) {
            e.markAsStaleReclaim();  // method added in T8 — SENDING→PENDING, retryCount++, claimedAt=null
            repo.save(e);
            revertedCounter.increment();
        }
    }
}
```

- [ ] **Step 4: 在 T8 CallbackQueueEntity 加 markAsStaleReclaim() 方法**（如未在 T8 加，回填）

```java
/** 由 CallbackStaleReaper 调用：SENDING 视为僵尸，回 PENDING + retryCount++。 */
public void markAsStaleReclaim() {
    if (!CallbackQueueStatus.SENDING.equals(this.status)) {
        throw new IllegalStateException(
            "markAsStaleReclaim only valid from SENDING, actual=" + this.status);
    }
    this.status = CallbackQueueStatus.PENDING;
    this.retryCount++;
    this.claimedAt = null;
    this.updateTime = LocalDateTime.now();
}
```

- [ ] **Step 5: 启用 @EnableScheduling**（如未启用 — grep 实测 `@EnableScheduling`）

- [ ] **Step 6: 跑测试 + 全 fep-web -am + Commit**

```bash
git add fep-web/src/main/java/com/puchain/fep/web/callback/reaper/ \
        fep-web/src/main/java/com/puchain/fep/web/callback/config/CallbackQueueProperties.java \
        fep-web/src/main/java/com/puchain/fep/web/callback/domain/CallbackQueueEntity.java \
        fep-web/src/main/resources/application.yml \
        fep-web/src/test/java/com/puchain/fep/web/callback/reaper/
git commit -m "feat(web): CallbackStaleReaper @Scheduled 60s scan / 300s window

T13 of Callback Phase 2b — SENDING + claimed_at < now-300s → PENDING + retryCount++;
retry handler takes over, exceeds maxAttempts → DEAD_LETTER + DLQ event published.

AI-Generated: claude-code
Reviewed-By: pending"
```

---

### Task T14 — ArchUnit invariant 扩展（AI / Mode A）

**Files**: Modify: `fep-web/src/test/java/com/puchain/fep/web/callback/CallbackModuleArchTest.java`

- [ ] **Step 1: 加 4 条 invariant**

```java
@Test
void credentialCryptoOnlyDependsOnSecurityApi() {
    noClasses().that().resideInAPackage("..callback.credential.crypto..")
        .should().dependOnClassesThat().resideInAPackage("..security.impl..")
        .check(classes);
}

@Test
void notificationListenerDoesNotDirectlyDependOnCredentialOrReaper() {
    noClasses().that().resideInAPackage("..callback.notification..")
        .should().dependOnClassesThat().resideInAPackage("..callback.credential..")
        .orShould().dependOnClassesThat().resideInAPackage("..callback.reaper..")
        .check(classes);
}

@Test
void callbackControllersDoNotDirectlyCallRepository() {
    noClasses().that().resideInAPackage("..callback..").and().areAnnotatedWith(RestController.class)
        .should().dependOnClassesThat().resideInAPackage("..callback..repository..")
        .check(classes);
}

@Test
void credentialFacadeOnlyUsedInCredentialSubpackage() {
    classes().that().areAssignableTo(CredentialEncryptionFacade.class)
        .should().onlyHaveDependentClassesThat()
            .resideInAPackage("..callback.credential..")
        .check(classes);
}
```

- [ ] **Step 2: 跑测试 + Commit**

```bash
git add fep-web/src/test/java/com/puchain/fep/web/callback/CallbackModuleArchTest.java
git commit -m "test(web): ArchUnit invariants for Phase 2b boundaries

T14 of Callback Phase 2b — credential.crypto isolated to security.api;
notification decoupled from credential/reaper; controllers don't call
repository; encryption facade restricted to credential subpackage.

AI-Generated: claude-code
Reviewed-By: pending"
```

---

### Task T15 — fep-admin-ui Vue 页面（**已迁至子 Plan，本 Plan 不实施**）

> **M2 修订 / muzhou 2026-05-28 拍板**: T15 原 ~600-800 行（11 文件: 7 Vue + 3 TS + 1 router），确定爆 ≤400 行 PR 门禁；整段迁至独立子 Plan [`2026-05-28-callback-phase2b-ui.md`](2026-05-28-callback-phase2b-ui.md)。
>
> **后端 ↔ UI 协调**:
> - 后端 PR (`feat/callback-phase2b-credential-dlq-alert-reaper`) 提供 6 个 REST endpoint: `POST/GET/PUT/DELETE /api/callback/credentials` + `GET /api/callback/dlq` + `POST /api/callback/dlq/{id}/replay` + `GET /api/notifications/{unread,unread/count,{id}/read}`
> - UI 子 Plan 实施前后端 PR 必须 merge to main（baseline 锁定）
> - UI 子 Plan 内独立 santa-method 评审 + muzhou 签字 + ≤400 行 PR 多 commit 拆分

**主 Plan T15 占位 step**:
- [ ] **Step 1**: 确认后端 PR (`feat/callback-phase2b-credential-dlq-alert-reaper`) merge to main（baseline check `git log origin/main --grep="Callback Phase 2b"`）
- [ ] **Step 2**: 触发子 Plan 实施（独立 worktree `wt-callback-p2b-ui` + 独立分支 `feat/callback-phase2b-ui`）

---

### Task T16 — 二次 AI 评审（santa-method）+ 端到端 IT（AI / Mode A）

**Files**:
- Create: `fep-web/src/test/java/com/puchain/fep/web/callback/CallbackPhase2bEndToEndIT.java`

- [ ] **Step 1: 端到端 IT — 全链路**

```java
@SpringBootTest
@AutoConfigureMockMvc
class CallbackPhase2bEndToEndIT {

    @RegisterExtension
    static WireMockExtension idp = WireMockExtension.newInstance()
        .options(wireMockConfig().port(9001)).build();
    @RegisterExtension
    static WireMockExtension bank = WireMockExtension.newInstance()
        .options(wireMockConfig().port(9002)).build();

    @Autowired CallbackEnqueueService enqueueService;
    @Autowired CallbackQueueRunner runner;
    @Autowired CallbackCredentialAdminService credSvc;
    @Autowired CallbackQueueRepository queueRepo;
    @Autowired InAppNotificationRepository notifRepo;
    @Autowired CallbackReplayService replaySvc;
    @Autowired CallbackStaleReaper reaper;

    @BeforeEach
    void seed() {
        // Seed admin user + interface + OAuth2 credential
        // ...
    }

    @Test
    void fullChain_credentialOAuth2_callback_dlq_inAppNotify_replay() {
        // 1. Create OAuth2 credential
        var req = new CredentialCreateRequest();
        req.setInterfaceId("IF-001");
        req.setAuthType(InterfaceAuthType.OAUTH2);
        req.setOauthClientId("clid");
        req.setOauthClientSecret("csec");
        req.setOauthTokenEndpoint(idp.baseUrl() + "/token");
        credSvc.create(req);

        // 2. IDP returns token
        idp.stubFor(post("/token").willReturn(okJson(
            "{\"access_token\":\"tok-1\",\"expires_in\":3600,\"token_type\":\"Bearer\"}")));

        // 3. Bank endpoint returns 400 (4xx → DEAD_LETTER directly)
        bank.stubFor(post("/callback").willReturn(badRequest()));

        // 4. Enqueue + run
        enqueueService.enqueue("Q1", "IF-001", "9001", "{\"data\":\"x\"}");
        runner.runOnce();

        // 5. Assert DEAD_LETTER + IN_APP notification inserted
        var dead = queueRepo.findById("Q1").orElseThrow();
        assertThat(dead.getStatus()).isEqualTo("DEAD_LETTER");
        var notifs = notifRepo.findByUserIdAndReadFalseOrderByCreateTimeDesc("admin-x");
        assertThat(notifs).hasSize(1);
        assertThat(notifs.get(0).getCategory()).isEqualTo("CALLBACK_DLQ");
        assertThat(notifs.get(0).getRefId()).isEqualTo(dead.getQueueId());

        // 6. Bank fixed; admin replays
        bank.stubFor(post("/callback").willReturn(ok()));
        DlqReplayResponse rsp = replaySvc.replay(dead.getQueueId(), "admin-x");
        runner.runOnce();
        var replayed = queueRepo.findById(rsp.newQueueId()).orElseThrow();
        assertThat(replayed.getStatus()).isEqualTo("DONE");
        assertThat(replayed.getOriginalDlqId()).isEqualTo(dead.getQueueId());
    }
}
```

- [ ] **Step 2: 主对话 dispatch general-purpose subagent (santa-method) 评审 ⛔ 安全相关代码**

主对话 Agent tool 派发（**M4 修订: 禁 `model:` override，继承父对话已认证模型** — 红线 `feedback_subagent_model_override_auth_fragility`）：

```
Agent(
  subagent_type=general-purpose,
  description="santa-method review callback credential security path",
  # NO model parameter — 红线 feedback_subagent_model_override_auth_fragility:
  # 设 `model:` override 触发 "Not logged in" auth 失败（0 产出空跑 ~200s + 可能留半成品未提交）
  # 省略 model 继承父对话已认证模型即正常
  prompt="""
  Independent santa-method security review for Callback Phase 2b T1-T6 (credential path).
  Read files: <list of credential/ subpackage files>.
  Check: SM4 key never logged / ciphertext columns never returned in DTOs /
  LogSanitizer + @SuppressFBWarnings on every logger statement touching credentials /
  OAuth2 token cache TTL safety margin / 401 invalidate+retry-once semantics /
  partial UPDATE preserves untouched ciphertext columns.
  Report: PASS / PASS WITH MINOR / FAIL with specific file:line findings.
  禁用 background bash + 不修 Plan/代码（仅评审输出）；红线 feedback_subagent_no_background_bash_in_workflow + feedback_subagent_meta_comment_no_tool_use (Status 起头 + ≥1 tool_use)。
  """
)
```

- [ ] **Step 3: Commit**

```bash
git add fep-web/src/test/java/com/puchain/fep/web/callback/CallbackPhase2bEndToEndIT.java
git commit -m "test(web): Phase 2b end-to-end IT (credential→OAuth2→callback→DLQ→IN_APP→replay)

T16 of Callback Phase 2b — full chain WireMock IDP + bank;
verifies DEAD_LETTER triggers IN_APP, admin replay recovers via new
PENDING row with original_dlq_id audit link.

AI-Generated: claude-code
Reviewed-By: pending"
```

---

### Task T17 — Plan closing + worktree cleanup + ③ 安全专家最终签字

- [ ] **Step 1: 跑全 reactor verify**（red line `feedback_full_regression_before_commit`）

```
cd /Users/muzhou/FEP_v1.0_wt-callback-p2b
./mvnw verify --batch-mode --no-transfer-progress
```

Expected: BUILD SUCCESS。

- [ ] **Step 2: PR 拓扑实施**（M3 + B5 v0.3 修订）

**双 PR + 编译序闸**（§0 元信息 §PR 拓扑 段对齐）:

```bash
# ① ⛔ Mode E PR 先 ship — T1+T2 同 PR (在 wt-callback-p2b-sec)
cd /Users/muzhou/FEP_v1.0_wt-callback-p2b-sec
# T1 commit (security-api KeyService interface + SecurityApiConfiguration mock impl)
# T2 commit (security-impl KeyServiceImpl SM4)
git push origin feat/callback-phase2b-security-credential-key
gh pr create --title "feat(security): SM4 credential master key API + HSM-backed impl (Mode E)" \
  --body "T1 + T2 of Callback Phase 2b — ⛔ Mode E ③ 安全专家拍板 PR。
  T1: fep-security-api KeyService.getSm4CredentialMasterKey() interface + mock zero-vector
  T2: fep-security-impl KeyServiceImpl HSM-backed impl
  Both commits: Security-Reviewed-By: ③<姓名> （不含 AI-Generated footer）
  
  B5 v0.3 修订: T1 ownership 由 (AI/Mode B) 转 ⛔ Mode E 因 API 触及密钥管理禁区；
  T1+T2 同 PR 保证 T2 @Override 在 baseline 上有 interface 方法可继承。"

# ② muzhou + ③ 双签后 merge T1+T2 PR to main

# ③ AI 后端 rebase main + ship 主 PR (含 T3-T14 + T16 + T17)
cd /Users/muzhou/FEP_v1.0_wt-callback-p2b
git fetch origin && git rebase origin/main   # 拉入 T1+T2 commit (含 KeyService + KeyServiceImpl)
git push origin feat/callback-phase2b-credential-dlq-alert-reaper
gh pr create --title "feat(web): Callback Phase 2b — credential + DLQ replay + IN_APP + reaper" \
  --body "T3-T14, T16-T17 of Callback Phase 2b。T1+T2 已先 merge（⛔ Mode E PR）。"
```

- [ ] **Step 3: ③ 安全专家整体 PR 审计 + 在主 PR 加 `Security-Reviewed-By` footer commit**（T1+T2 ⛔ PR commit footer 已含；本 step 是主 PR 整体审计 — T4/T6 等触及凭证密文路径）

- [ ] **Step 4: muzhou 最终签字 merge 主 PR**

- [ ] **Step 5: cleanup worktrees**

```bash
git worktree remove /Users/muzhou/FEP_v1.0_wt-callback-p2b
git worktree remove /Users/muzhou/FEP_v1.0_wt-callback-p2b-sec
git worktree list  # 验证清理
```

- [ ] **Step 6: PRD trace matrix 同步**（session-end Phase 6 自动同步）

- [ ] **Step 7: 触发 P2b-UI 子 Plan 实施**（M2 拆 P2b-UI 后续 — 主 PR merge 后 baseline 就绪）

- [ ] **Step 8: 触发 /session-end 6-phase 全流程**

---

## §4 风险 + 假设 + PRD 追溯

### 4.1 风险

| # | 风险 | 缓解 |
|---|---|---|
| R1 | T2 ③ 安全专家 Task block 整 Plan | T2 优先开工 + T1 mock impl 让 T3-T6 不阻塞；T2 独立 PR 先 merge |
| R2 | PR #27 未 merge → T8/T10 baseline 不一致 | **实施前重 grep `git log origin/main --grep="Callback Phase 2"` 比对 V29 / `markDeadLetter` / `claimed_at` 字段在 main 上存在**（M1 修订：squash/rebase merge 后 SHA 会变，仅 grep `b26f4a8` 不够）；如未 merge，rebase 或暂停实施（**候选新红线 `feedback_plan_hypothetical_merged_baseline_must_recheck_before_exec`**） |
| R3 | Flyway V_N 冲突 | 起草 + 实施前各 grep 实测最大 V（红线 `feedback_plan_flyway_v_collision_check`）；签字与实施跨 ≥24h 时实施前重 grep（红线 `feedback_dependency_plan_currency_recheck`） |
| R4 | T15 admin UI ≤400 行爆门禁 | **拆 P2b-UI 独立子 Plan**（M2 修订 / muzhou 2026-05-28 拍板，[`2026-05-28-callback-phase2b-ui.md`](2026-05-28-callback-phase2b-ui.md)）；T15 主 Plan 占位 stub 不实施 |
| R5 | OAuth2 401 与 retry handler 4xx 规则冲突 | T6 区分：401 凭证类是 invalidate+1 retry；持续 401 → 4xx → DEAD_LETTER |
| R6 | 多实例 OAuth2TokenCache 不一致 | 接受，单实例足够；Phase 2c 升 DB cache |
| R7 | Caffeine cache 内存 | 100 if × 2KB = 200KB negligible |
| R8 | reaper 多实例同时跑 | DB SELECT FOR UPDATE SKIP LOCKED 互斥；reaper @ConditionalOnProperty（N3 修订） |
| R9 | 凭证 admin XSS / SQL inject | JPA prepared + Bean Validation + Spring Security |
| R10 | 凭证日志 CRLF injection | LogSanitizer + @SuppressFBWarnings（红线 `feedback_logsanitizer_alone_insufficient_for_findsecbugs`） |
| R11 | Caffeine `expireAfter(Expiry)` 复杂度 | T5 Step 3 简化为 `expireAfterWrite(expires_in - 30s)`（N1 修订） |
| R12 | T1+T2 安全 PR + T3-T17 AI 主 PR 拓扑 cross-merge 冲突 | M3 + B5 v0.3 修订：**双 PR 编译序闸** — T1（interface）+ T2（impl）同 ⛔ Mode E PR 先 merge（避免 T2 `@Override` 在 baseline 上无 interface 方法 compile fail），AI 主分支 rebase main 拉入后再 ship T3-T14+T16+T17 |
| R13 | subagent dispatch `model:` override 触发 "Not logged in" auth 失败 | T16 Agent dispatch **禁 `model:` 参数**（M4 修订，红线 `feedback_subagent_model_override_auth_fragility`） |

### 4.2 假设

- ③ 安全专家在 1 周内可完成 KeyServiceImpl.getSm4CredentialMasterKey 实现
- OAuth2 endpoint 返 RFC 6749 §4.4.3 标准字段（`access_token`/`expires_in`/`token_type`）
- ~~`SysUserRepository.findByRoleCode("ADMIN")` 方法存在或可简单添加（实施前 grep 实测）~~ **B2 修订（v0.2）: 改为两步查询 `SysRoleRepository.findByRoleCode("ADMIN") → SysUserRoleRepository.findByRoleId(roleId) → SysUserRepository.findAllById(userIds)`，3 个既有 Repository 现存，无需新增 method**
- PR #27 (b26f4a8) 在本 Plan 实施前 merge
- T15 fep-admin-ui 由 P2b-UI 子 Plan 独立 ship（不阻塞后端主 PR merge）

### 4.3 PRD 追溯

| FR-ID | PRD § | Task |
|---|---|---|
| FR-INFRA-CALLBACK-CREDENTIAL | §5.5.2 + §2.2.1 | T1-T7（**T15 UI 见子 Plan**） |
| FR-INFRA-CALLBACK-DLQ-REPLAY | §2.2.1 | T8-T9（**T15 UI 见子 Plan**） |
| FR-INFRA-CALLBACK-IN-APP-ALERT | §5.10.7.2d + 决策门 6 | T10-T12（**T15 UI 见子 Plan**） |
| FR-INFRA-CALLBACK-STALE-REAPER | §2.2.1 | T13 |

---

## §5 验收 / Closing Checklist

**Strong 验收**（命中即 ✅ ship — M2 修订：主 Plan 后端，T15 UI 独立子 Plan ship）:
- [ ] T1-T14 + T16-T17 全部 Step 完成（**主 Plan 16 Task，T15 stub 占位指向子 Plan**）
- [ ] `./mvnw verify` BUILD SUCCESS（fep-web + fep-security-api + fep-security-impl 全绿）
- [ ] JaCoCo 行覆盖 ≥ 80% / 分支 ≥ 70%（fep-web/callback 模块）
- [ ] ArchUnit 4 条新 invariant 全绿
- [ ] CallbackPhase2bEndToEndIT 全绿
- [ ] santa-method 二次评审 PASS / PASS WITH MINOR
- [ ] muzhou 7 项 plan-review-checklist 签字
- [ ] ③ 安全专家在 T2 PR commit 加 `Security-Reviewed-By` footer（独立 PR `feat/callback-phase2b-keyservice-sm4`）
- [ ] T2 PR + AI 主 PR 双 PR merge to main（M3 PR 拓扑）
- [ ] T15 fep-admin-ui 子 Plan signed（独立 [`2026-05-28-callback-phase2b-ui.md`](2026-05-28-callback-phase2b-ui.md)）

**Minimum 验收**（妥协 ship — 后端独立完成，UI 延后）:
- [ ] T1-T14 + T16-T17 后端全 PASS
- [ ] Phase 2b 后端主 PR merge to main
- [ ] T15 UI 子 Plan 未 ship 不阻塞（后端 controller API 已可独立使用）

**Closing 命令**（参见 §3 T17 Step 2 - 双 PR 拓扑）。

---

## §6 AI 评审记录

### Round 1 — 2026-05-28（santa-method via general-purpose）

> 评审者: general-purpose subagent (agentId `a3883ad2173bd3303`，无 model override，santa-method adversarial / spec-coverage / consistency 三视角)
> 评审日期: 2026-05-28
> baseline HEAD 实测: `3300533`（起草时 `4fcac99`，drift +1 commit = R-NEW-1 follow-on XsdTestSupport.pad30 helper ship；本评审基于起草版本，与 3300533 无冲突）
> **评审结果: NEEDS REVISION**（BLOCKER × 4 + MAJOR × 4 + MINOR × 6 + NIT × 3 + POSITIVE × 3）

**BLOCKER × 4**（必须修订才能签字 / 主对话 grep 实证全部命中）:

1. **B1 — `sub_output_interface` 表名错（实际 `t_sub_output_interface`，带 t_ 前缀）**
   - File:line: Plan `:52` (§1.2 grep evidence row) + `:353` (T3 V30 FK 约束)
   - 实证: `fep-web/src/main/java/com/puchain/fep/web/submission/outputinterface/domain/SubOutputInterface.java:@Table(name = "t_sub_output_interface")`
   - 修订: T3 V30 SQL `REFERENCES t_sub_output_interface(interface_id)` + §1.2 行明示带 `t_` 前缀（旧业务表保留 prefix，新业务表 callback_queue 无前缀）

2. **B2 — `SysUserRepository.findByRoleCode("ADMIN")` 方法不存在（API 编造）**
   - File:line: Plan `:2161 / :2180 / :2207`（T12 三处 stub + impl）
   - 实证: `SysUserRepository.java` 仅 `findByUserAccount / existsByUserAccount / findByUserStatus / findByUserNameContainingOrUserAccountContaining`，**无任何 role 关联方法**；本 codebase 用 `SysUserRoleRepository.findByRoleId` + 二次 join
   - 修订: T12 改两步查询 `SysRoleRepository.findByRoleCode("ADMIN") → roleId` + `SysUserRoleRepository.findByRoleId → List<SysUserRole>` + `SysUserRepository.findAllById`；§2.3 文件结构补 SysUserRepository / SysRoleRepository / SysUserRoleRepository 引用

3. **B3 — `FepErrorCode` 包路径未明注（实际 `com.puchain.fep.common.domain.FepErrorCode`）**
   - File:line: §1.2 `:63` evidence row + T7 `:1395/:1411/:1421/:1441` + T9 `:1798/:1801`
   - 实证: `fep-common/src/main/java/com/puchain/fep/common/domain/FepErrorCode.java`（不是 `.common.exception`）
   - 修订: §1.2 evidence row 加包路径标注；T7/T9 代码 sample 加显式 `import com.puchain.fep.common.domain.FepErrorCode;` + `import com.puchain.fep.common.exception.FepBusinessException;`

4. **B4 — `CallbackQueueStatus` 是 `public final class` String constants 非 enum（reviewer 建议降 MAJOR，主对话采纳降级）**
   - File:line: T8 `:1604/:1611` + T9 `:1738/:1742`
   - 实证: `CallbackQueueStatus.java` = `public final class` + `public static final String PENDING / DEAD_LETTER`；`CallbackQueueEntity.status` 私有 String 无 setter（仅 named transition methods）
   - 修订: `copyForReplay` 在 entity 类内通过 private field 直接构造合法，Plan §3 T8 加注或在 entity 内加 package-private `markAsReplayPending()` transition method 替代直接字段写

**MAJOR × 4**:

- **M1 — §1.2 grep evidence 实测自 PR #27 worktree (`wt-callback-p2`) 非 origin/main**: PR #27 squash/rebase merge 后 SHA 会变；§8 Handoff 仅 grep `b26f4a8` 不够。修订: §1.2 加 "实测 worktree: wt-callback-p2"；§8 改 `git log origin/main --grep="Callback Phase 2"` + merge 后字段比对清单
- **M2 — T15 fep-admin-ui ~600-800 行（11 文件: 7 Vue + 3 TS + 1 router）确定爆 ≤400 行 PR 门禁，"起草时决定"是软处理**: 修订: §1.1 改硬决策 — 默认拆 P2b-UI 独立 Plan / 备选每 commit ≤400 行多 commit 拆分（**走 AskUserQuestion 让 muzhou 拍板**）
- **M3 — T2 ⛔ Mode E 分支 `feat/callback-phase2b-keyservice-sm4` vs AI 主分支 `feat/callback-phase2b-credential-dlq-alert-reaper` PR 拓扑未说明**: 单 PR 合并 / 双 PR 顺序 / cherry-pick 三选一未明确。修订: §0 加 "PR 拓扑" 段；T17 加 step
- **M4 — T16 santa-method dispatch 示例未禁 `model:` override**: 红线 `feedback_subagent_model_override_auth_fragility` 已立但 Plan 未引用。修订: T16 Step 2 加注

**MINOR × 6** (可后期 hotfix):
- N1 — T5 OAuth2TokenCache `expireVariably().ifPresent + cache.put` 内部矛盾，建议简化 `expireAfterWrite`
- N2 — T6 CallbackHttpClient 构造器 0-arg → 1-arg 破坏性变更未标 grep `new CallbackHttpClient(` 影响范围
- N3 — T13 reaper `@Scheduled fixedRate` 始终调度 + enabled flag 内部短路，建议 `@ConditionalOnProperty` 或 `SchedulingConfigurer`
- N4 — §6 评审模板缺 baseline drift 提示（本次修订已落实）
- N5 — Plan 未引用红线 `feedback_concern_boil_lake_when_cheap_and_safe` / `feedback_worktree_for_parallel_work` / `feedback_subagent_model_override_auth_fragility`
- N6 — T14 ArchUnit "4 条 invariant" + 4 个 `@Test` 自洽 ✓（红线 `feedback_plan_template_data_point_self_consistency` 通过）

**NIT × 3**:
- T1 mock impl `return new byte[16]` 应 throw `UnsupportedOperationException`
- T2 V32 `level VARCHAR(20)` 缺 CHECK constraint
- T3 JaCoCo include 按 sub-package 配置未明

**POSITIVE × 3**:
- P1 — Mode E ⛔ 安全分层教科书级（mock impl + 独立 worktree + commit footer 不含 AI-Generated）
- P2 — 17 Task TDD 节奏严格 + 红线 `feedback_full_regression_before_commit` 两处引用
- P3 — DLQ 复制重放 `original_dlq_id` 双向 chain 设计符合金融审计标准

**核心症结**: §1.2 实测表"看上去 grep 了 14 项"但 4 项有 BLOCKER 错误（B1 表名、B2 method 不存在、B3 包路径未明、M1 worktree 来源）— 红线 `feedback_plan_must_grep_actual_api` 实质违反。

**boil-lake 触发条件检查**（红线 `feedback_concern_boil_lake_when_cheap_and_safe`）:
- ① reviewer 输出 PASS WITH 🟡 CONCERN？ — **不**，是 NEEDS REVISION (BLOCKER × 4) → 不触发 boil-lake，走标准 NEEDS REVISION 修订流程

**Round 2 评审待 muzhou 决策修订路径后重派**

Recommended changes 落实情况（v0.2 修订记录）:
- [x] B1 — V30 FK 改 `t_sub_output_interface(interface_id)` (line ~353) + §1.2 行明示 t_ 前缀（旧业务表 convention）
- [x] B2 — T12 改两步查询（SysRoleRepository → SysUserRoleRepository → SysUserRepository.findAllById）+ §1.2 实测证据补 3 行 Repository 实测 + listener test 改 3 mock + impl 改 3 step 查询
- [x] B3 — §1.2 evidence 补 FepErrorCode 包路径（`com.puchain.fep.common.domain.FepErrorCode`）+ FepBusinessException（`com.puchain.fep.common.exception.FepBusinessException`）；T7/T9 代码 sample 加 import（见各 Task 代码块）
- [x] B4 — 降 MAJOR 处理；Plan §3 T8 加注 `copyForReplay` static factory 在 entity 类内私有字段访问合法；entity 加 `markAsReplayPending()` package-private transition method 替代直接字段写（详 T8 代码块）
- [x] M1 — §1.2 实测来源标注 worktree 来源（wt-callback-p2 / origin/main）+ §8 Handoff 改 `git log origin/main --grep="Callback Phase 2"` + merge 后字段比对清单
- [x] M2 — §1.1 拆 P2b-UI 独立子 Plan（muzhou 2026-05-28 拍板）+ §3 T15 整段 stub 占位 + §2.7 文件结构标"已迁子 Plan" + §4.1 R4 + §4.2 假设 + §4.3 PRD trace + §5 验收同步
- [x] M3 — §0 加 PR 拓扑段（双 PR + cherry-pick 顺序）+ T17 Step 2 双 PR 实施清单
- [x] M4 — T16 Step 2 dispatch 示例加 `# NO model parameter` 注释 + 红线 `feedback_subagent_model_override_auth_fragility` 引用

**N1-N3 顺手修订**:
- [x] N1 — T5 OAuth2TokenCache 简化 `expireAfterWrite(expires_in - 30s)` 路径（保留原 `expireAfter(Expiry)` 作为参考，Step 3 注释提供简化路径，落实仍需 implementer 选定）
- [x] N2 — T6 加 grep `new CallbackHttpClient(` 影响范围提示（实施前 grep 实测既有调用点）
- [x] N3 — T13 reaper enabled flag 改 `@ConditionalOnProperty` Bean 级条件装配（避免 disabled 时空跑产生噪声日志）— Plan §4.1 R8 修订引用

### Round 2 — 2026-05-28（santa-method via general-purpose）

> 评审者: general-purpose subagent (agentId `a56b5d25413e71604`，无 model override，santa-method)
> 评审日期: 2026-05-28
> baseline HEAD 实测: `3300533`（与 Round 1 一致无 drift）
> **评审结果: NEEDS REVISION**（BLOCKER × 1 新生 + MINOR × 2 + POSITIVE × 5）

**Round 2 核对汇总（Round 1 修订到位）**: B1 / B2 / B3 / B4 / M1 / M2 / M3 / M4 / N1 / N2 / N3 — **11 项全部到位** ✅。但 M3 PR 拓扑修订引入 1 新 BLOCKER。

**🔴 BLOCKER × 1（M3 修订引入）**:

**B5 — M3 PR 拓扑 T1/T2 编译序违反（T2 ship 前 T1 interface 不在 main）**
- File:line: §0:23 + §8.3:3040-3041 + T1:228 + T2:308
- 现状: T1（AI / Mode B）在 fep-security-api 加 `KeyService.getSm4CredentialMasterKey()` 接口归属 AI 主 PR；T2（⛔ Mode E）在 fep-security-impl `@Override` 该方法归属 T2 安全 PR。M3 拓扑要 T2 PR 先 merge，但 `wt-callback-p2b-sec` 从 `origin/main` 创建时 main 上**尚无** T1 interface 方法
- 风险: ③ 安全专家在 T2 Step 1 实现时 `@Override` 找不到父类方法 → fep-security-impl 编译失败 → T2 PR 无法独立通过 CI
- **muzhou 2026-05-28 AskUserQuestion 拍板**: (Recommended) **T1 移入 T2 PR 改 ⛔ Mode E** — 理由：`getSm4CredentialMasterKey` 接口设计触及 CLAUDE.md "密钥管理" ⛔ 禁区，API 语义本身（密钥长度/生命周期/HSM 来源）由 ③ 安全专家拍板更合理；T1+T2 同 PR 避免编译序冲突；ownership 一致
- **v0.3 修订**: §0 PR 拓扑段（双 PR + 编译序闸）+ §1.1 Task ownership 调整（AI 14 + ⛔ 2 + UI 1）+ §3 T1 ownership 改 ⛔ Mode E + commit footer + worktree 共用 + branch 重命名 `feat/callback-phase2b-security-credential-key`（替代原 `feat/callback-phase2b-keyservice-sm4`）+ §4.1 R12 修订引用 + §8.3 worktree 创建顺序调整（先 ⛔ Mode E worktree，AI 主 worktree 在 T3 实施前 rebase main 拉入 T1+T2）

**🟢 MINOR × 2**:

**N4 — OAuth2TokenCache Checker Framework `@NonNegative` 应用代码漏入**
- File:line: T5 Step 3 行 798 + 832 + 837（v0.2）
- 现状: `import org.checkerframework.checker.index.qual.NonNegative;` + 两处 `@NonNegative long currentDuration` 修饰
- 修订: **v0.3 删除 import + 两处注解**（Caffeine `Expiry` 接口契约层使用，覆写方法不需重复声明；codebase 未配置 Checker Framework processor，注解仅 documentation）

**N5 — B2 InAppNotificationListener Test mock 辅助方法未在 Plan 内定义**
- File:line: T12 Step 1 测试行 2228-2232（v0.2）
- 现状: 使用 `mockSysRole / mockSysUserRole / mockSysUser` helper 方法但 Plan 未给出实现
- 修订: **v0.3 在 T12 test class 末尾加 inline helper 实现**（3 个 `private` 方法，避免 helper class drift；implementer 直接复用）

**✅ POSITIVE × 5**（reviewer 表扬，保留供未来 reviewer 参考）:
1. §1.1 Task 数自洽校验 — Plan template self-consistency 满足
2. B4 entity transition method 设计 + Javadoc 解释与既有 markAs* 一致
3. M1 §8 PR #27 merge 验证 4 字段 grep + 备用 `git log --grep="b26f4a8"` 备 squash merge SHA 变化 + "任一缺失 → 实施 BLOCKED" 硬性闸
4. N3 reaper Bean-level `@ConditionalOnProperty` 比内部 if guard 更彻底
5. M4 subagent dispatch 单段引用 model override + background bash + meta-comment 三红线 + 输出格式约束

**boil-lake 触发条件检查**（红线 `feedback_concern_boil_lake_when_cheap_and_safe`）:
- ① reviewer 输出 PASS WITH 🟡 CONCERN？ — **不**，是 NEEDS REVISION (BLOCKER × 1) → 不触发 boil-lake，走标准 NEEDS REVISION 修订流程

**Round 2 → v0.3 修订记录**:
- [x] B5 — §0 PR 拓扑（双 PR + 编译序闸 + B5 修订理由）+ §1.1 Task ownership + §3 T1 ownership 改 ⛔ Mode E + commit footer Security-Reviewed-By + worktree 共用 + branch 名调整 + §3 T17 Step 2 PR 实施清单 + §8.3 worktree 创建顺序调整 + §4.1 R12 修订
- [x] N4 — T5 Step 3 删 `@NonNegative` import + 两处注解（行 798 / 832 / 837 → 仅签名变化）
- [x] N5 — T12 Step 1 test class 末加 3 个 inline mock helper (mockSysRole / mockSysUserRole / mockSysUser)

**Round 3 评审**: 不需要 — B5 + N4 + N5 是局部修订（PR 拓扑 + 注解删除 + helper 加），无新业务逻辑或架构变化；muzhou 在 v0.3 直接 7 项 plan-review-checklist 签字。

---

## §7 muzhou 签字

> 7 项 plan-review-checklist (`docs/guides/plan-review-checklist.md`):
> - [x] **PRD 对齐** — §4.3 PRD trace 4 FR-ID（FR-INFRA-CALLBACK-CREDENTIAL/DLQ-REPLAY/IN-APP-ALERT/STALE-REAPER）映射 §5.5.2 + §2.2.1 + §5.10.7.2d 完整；T15 UI 子 Plan 衔接显式
> - [x] **FR-ID 引用** — §0 元信息 + §4.3 trace 表 + 每 Task 段隐式依 PRD 章节，prd-traceability-matrix session-end 实施时同步
> - [x] **完整性** — 17 Task heading（T1-T17）+ 16 实质 Task + 1 stub 占位 (T15 → 子 Plan)；T1+T2 ⛔ Mode E PR、T3-T14+T16+T17 AI 主 PR、T15 UI 子 Plan 三路覆盖凭证/DLQ/IN_APP/reaper 4 项 brainstorm scope
> - [x] **一致性** — v0.2 Round 1 B1-B4+M1-M4 修订 (11 项) + v0.3 Round 2 B5+N4+N5 修订 (3 项) 实证 §6 评审记录 + grep 命中 (B1×6 / B2×7 / B3×7 / B4×5 / N3×6 / B5 编译序闸×12 / v0.3 标识×19)；跨 Task 类名/方法/包路径全 grep 实测对齐
> - [x] **安全分层** — T1+T2 (⛔ Mode E ③ 安全专家) 独立 worktree + 独立 PR + commit footer `Security-Reviewed-By` 不含 `AI-Generated`；T16 Step 2 dispatch 主对话 santa-method 评审凭证密文路径 + 三红线 (model override/background bash/meta-comment) 引用；B5 修订理由"密钥管理 API 语义触及 ⛔ 禁区"对齐 CLAUDE.md ⛔ 区域
> - [x] **测试策略** — TDD red→green per Task (Step 1 失败测试 → Step N 实现绿)；ArchUnit T14 新 invariant 4 条 (`feedback_plan_template_data_point_self_consistency` 自洽校验通过)；CallbackPhase2bEndToEndIT 全链路 (T16) WireMock IDP+bank；JaCoCo 行 ≥80% / 分支 ≥70%
> - [x] **治理流程** — Plan v0.1 → v0.2 → v0.3 三版 + Round 1 + Round 2 两轮 santa-method 评审完整记录 §6；§8 实施 Handoff 含 baseline 4 时点重 grep + worktree 触发动态判定 + 子 Plan 衔接 + 5 红线引用；M3+B5 修订双 PR + 编译序闸 ownership 清晰；T17 closing 含 session-end 6-phase 触发
>
> 签字日期: 2026-05-28
> 签字人: muzhou
> 备注: AI 起草 + Round 1 santa (BLOCKER × 4) + v0.2 修订 + Round 2 santa (BLOCKER × 1 新生 B5 PR 拓扑) + v0.3 修订 + muzhou 直接 7 项签字（reviewer 明示"不需 Round 3"）。T1+T2 在 ⛔ Mode E PR (`feat/callback-phase2b-security-credential-key`) 先 merge；AI 主 PR (`feat/callback-phase2b-credential-dlq-alert-reaper`) rebase 后 ship；T15 UI 子 Plan stub 待 v1.0 起草+评审+签字+实施。
> v0.3 候选新红线（待后续拍板）: `feedback_pr_topology_interface_impl_split_compile_order` (PR 拆分时 interface+impl 编译序 + Mode E ownership 范围)。

---

## §8 实施 Handoff

### 8.1 baseline 重 grep（实施前必做 4 时点 — 红线 `feedback_baseline_drift_during_long_review_cycle`）

**4 时点**: ① 起草前 ② Round N 评审前 ③ Task T0 实施前最后一道闸 ④ T4 merge 前。

```bash
cd /Users/muzhou/FEP_v1.0
git fetch origin --quiet

# 1) origin/main HEAD 漂移
git rev-parse --short origin/main

# 2) 本会话 unpushed commit（避免别会话已 push 误带）
git log origin/main..main --oneline

# 3) PR #27 merge 状态比对（M1 修订: squash/rebase merge 后 SHA 会变，grep 多关键字）
git log origin/main --grep="Callback Phase 2" --oneline   # 找 merge commit
git log origin/main --grep="b26f4a8" --oneline             # 备用：找原 commit SHA（squash 可能丢）
# 实测验证: 4 关键字段在 main 上存在（PR #27 merge 后必有，未 merge 则缺失）
grep -rn 'markDeadLetter' fep-web/src/main/java --include="*.java" | head -3
ls fep-web/src/main/resources/db/migration/V29__*.sql 2>/dev/null && echo "V29 callback_queue OK"
grep -rn 'claimed_at\|claimedAt' fep-web/src/main/java/com/puchain/fep/web/callback --include="*.java" | head -3
grep -rn 'retry_count\|retryCount' fep-web/src/main/java/com/puchain/fep/web/callback --include="*.java" | head -3

# 如果以上任一项缺失 → PR #27 未 merge → **实施 BLOCKED**（直到 PR #27 merge）

# 4) Flyway 最大 V（红线 feedback_plan_flyway_v_collision_check + feedback_dependency_plan_currency_recheck）
ls fep-web/src/main/resources/db/migration/ | sort -V | tail -3
# 期望: V28 / V29 (PR #27) 最大；如出现 V30+ 表示别会话已占用，Plan V30/V31/V32 须重新规划

# 5) worktree list
git worktree list
# 期望: main + .e2e + wt-callback-p2 (PR #27) + wt-callback-p2b (本 Plan AI) + wt-callback-p2b-sec (T2 ⛔)
# 若 wt-callback-p2b 已存在（别会话先建）→ 协调 owner，不接手别会话 WIP（红线 feedback_new_session_no_intervention_in_prior_tasks）
```

### 8.2 假设兑现 checklist（baseline 假设 PR #27 merge）

- [ ] PR #27 merge to main 验证（above grep 4 字段全命中）
- [ ] Flyway V29 在 main `db/migration/` 目录下存在
- [ ] `markDeadLetter` 方法在 `CallbackRetryHandler` 调用
- [ ] PR #27 squash merge commit hash 实测记录（用于追溯）

### 8.3 worktree 触发条件动态重测（红线 `feedback_worktree_trigger_is_dynamic_recheck_at_execution`）

实施前确认触发条件 ②（与已签字 Plan 并存）+ ③（⛔ 安全 vs AI 并行）+ ⑦（多会话活跃）仍命中 → 必须 `git worktree add` 隔离：

```bash
# ⛔ Mode E PR worktree (T1 + T2)
git worktree add /Users/muzhou/FEP_v1.0_wt-callback-p2b-sec -b feat/callback-phase2b-security-credential-key origin/main
# AI 后端 PR worktree (T3-T14 + T16 + T17)
git worktree add /Users/muzhou/FEP_v1.0_wt-callback-p2b -b feat/callback-phase2b-credential-dlq-alert-reaper origin/main
# 注: wt-callback-p2b 在 T3 实施前需 rebase main 拉入 ⛔ PR merge 后的 T1+T2 (KeyService interface + KeyServiceImpl)
# 编译序闸：T2 @Override getSm4CredentialMasterKey 需要 T1 interface 在 baseline 上存在
```

### 8.4 执行 sub-skill 选项

- **Subagent-Driven**（推荐）— `superpowers:subagent-driven-development` 每 Task fresh subagent + 主对话 review
  - **禁 `model:` override**（红线 `feedback_subagent_model_override_auth_fragility`）
  - **禁 background bash**（红线 `feedback_subagent_no_background_bash_in_workflow`）
  - subagent return `Status: <DONE|BLOCKED|...>` + ≥1 tool_use（红线 `feedback_subagent_meta_comment_no_tool_use`）
  - subagent commit 前 + git SHA return（红线 `feedback_subagent_must_commit_before_exit`）
- **Inline Execution** — `superpowers:executing-plans` 本会话内顺序执行 + checkpoint

### 8.5 子 Plan 衔接

- T15 fep-admin-ui 整段迁至 [`2026-05-28-callback-phase2b-ui.md`](2026-05-28-callback-phase2b-ui.md)
- 后端主 PR merge to main 后子 Plan 实施前 baseline grep 同 §8.1 步骤
- UI worktree: `/Users/muzhou/FEP_v1.0_wt-callback-p2b-ui`（独立分支 `feat/callback-phase2b-ui`）
