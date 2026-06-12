# FEP 国密 S1 Follow-up 实施计划 — legacy 凭证主动 sweep + F4 残余面清理 + R4 mock 收敛

> **执行方式:** hybrid（红线 `feedback_harness_bg_detach_hybrid_default`）— 主会话实施 edits + 前台 mvn（redirect-to-file）+ commit；subagent 仅做每 Task spec/quality review。步骤使用 `- [ ]` 复选框跟踪。

**版本:** v0.5（2026-06-12 修订）
**Plan 作者:** Claude Code（mode A）
**起草 Baseline:** origin/main = main = `52ce5fe7`（**GM S5 PR #82 已 merge**，2026-06-12 v0.4 轮 git fetch 实测；执行前须按红线 `feedback_baseline_drift_during_long_review_cycle` 再重测）

**修订记录:**
- v0.4 → v0.5（santa Round 3 REVISE 闭合）:
  - **santa M-NEW-3**: 补 1 处验证盲区残留 `fep-security-api/.../package-info.java:7`（"接口变更需经安全工程师审核"——窄 pattern 锚定变体漏检，主会话 sed 亲测）→ B4 **34 处（4 模块 15 文件）**，专项措辞 5→6；Step 1/Step 3 窄 pattern 增 `安全工程师审核|安全工程师确认`（加宽后 dry-run 命中 9 = 原 8 + package-info，零噪声）
  - **santa m-NEW-4**: 头部与 B7 引言 "4 个别会话 worktree" → "3 个"（与自检 #10 对齐，wt-gm-s5 teardown 留档）
  - **santa c-NEW-1**: 签字区 Round 3 条目更新为 v0.4 口径
- v0.3 → v0.4（baseline 跳变：S5 PR #82 `52ce5fe7` merge + 本地 main ff + `wt-gm-s5` teardown，红线 `baseline_drift_during_long_review_cycle` 每轮实测抓获）:
  - **B4 #5（SignService:8）剔除** — S5 重写 SignService Javadoc 已含 🔓 解禁措辞；F4 残余面 34 → **33 处（4 模块 14 文件）**（v0.4 轮无 carve-out 全仓 grep 实测：窄 8 行[除豁免] + 宽 "Mode E" 27 行 7 文件，与 B4 表逐行吻合）
  - **SignServiceImpl 已实装**（fep-security-impl/sign/，GM S5 进程内 BC 审计签名）→ B4 #1/#2 措辞由"S2b 未实装"改为"已实装 + 报文签验 wiring 待 §0.3（S2b）"，镜像 S5 签字的 SignService 新 Javadoc 版式
  - **R4 冲突预案消解** — S5 已 merge 进 baseline，TestKeyServiceConfiguration 无预期冲突；Task 3 Step 0 改为确认性检查
  - **Task 4 Step 3 验证式简化** — S5 carve-out 全部移除（其 6 文件已清零），窄 grep 仅排 BlockedMessageTypes、宽 grep 置换后期望全仓 0
  - **B7 并发表更新** — 别会话 worktree 实测 3 个（.e2e / wt-rule-batch2[已推进 `4dc231d6`] / wt-simplify-q-drain）
- v0.2 → v0.3（santa Round 2 REVISE 闭合，muzhou 2026-06-12 拍板 F4 全量扩批）:
  - **santa M-NEW-1**: F4 残余面扩为 **34 处（4 模块 15 文件）**——补 2 处必修 ArchUnit 文案（ProcessorArchitectureTest:29 / CallbackModuleArchTest:96）+ PK7/reconciliation 宽面 22 处同源 "Mode E" 措辞（muzhou 拍板全量扩批，消除同域措辞分裂）；全部锚点主会话重新 grep 亲测；Task 4 Step 3 验证式加 **S5 未 merge carve-out**（本分支 base `46bdb279` 早于 S5 T7，其 6 文件旧措辞属 S5 所有，本 Plan 不触碰）+ 表外残留兜底条款
  - **santa M-NEW-2**: Task 4 Step 1 脚本 PackageStructureTest 路径修正为 `fep-web/src/test/java/com/puchain/fep/architecture/`（实测包名无 web 段）；全脚本路径已 dry-run（`ls` 验证通过）
  - **santa m-NEW-3**: "7 文件"→"15 文件"（扩批后 distinct 实算）
  - **santa C-NEW-1**: B4 增噪声豁免说明（"must NOT" 英文惯用语 ~12 文件与治理措辞无关，不入表）
- v0.1 → v0.2（santa Round 1 REVISE + T0 并发重测撞车发现，muzhou 2026-06-12 拍板收缩重排）:
  - **撞车收缩**: T0 时点重测（红线 `parallel_session_task_allocation_discipline`）发现别会话 `wt-gm-s5`（S5 审计链，活跃未推送）T7 commit `b90429f6`（2026-06-12 11:39）已完成原 Task 3 全部（F1/F2 四处 FQN + R5 null guard + F5a[其 T1] + **F7 真身复原=测试 helper static 化**）及 F4 主锚点（KeyService.getSignPrivateKey + MockKeyService ×3 + TestKeyServiceConfiguration ×4 含 F3 CBC→ECB，措辞已随 S5 Plan muzhou 签字）→ **原 Task 3 整体剔除；F4 收缩为残余面**（B4 v0.2 表）
  - **santa B-1**: Task 1 测试 fixture 改用 `CallbackCredentialEntity.newToken` 静态工厂（实体无 setter、构造器 protected，grep :101/:115 实测）
  - **santa B-2**: `migrateLegacy()` 改为**惰性读** legacyProps（方法内取集合），@InjectMocks + 测试期 stub 时序成立，消除 UnnecessaryStubbingException
  - **santa M-1**: F4 扩为全仓宽 pattern grep（增 "AI 禁入|安全工程师|安全专家"），残余面 12 处逐条实测入表 + 1 处豁免披露（BlockedMessageTypes 与 P3 BLOCKED ADR 现行表述一致）
  - **santa M-2**: 并发 worktree 实测 4 个别会话（.e2e / wt-gm-s5 / wt-rule-batch2 / wt-simplify-q-drain），交集披露 + Task 5 收尾文案改为"以实测为准、勿动别会话"
  - **santa M-3**: 原 Task 4↔5 同文件死改动时序问题随撞车收缩自然消解（F4 残余面不再触 TestKeyServiceConfiguration；R4 与 F4 残余面文件全交集为空）
  - **santa m-1/m-2/m-3/m-4**: Task 2 RED 期望改写（编译通过、404 断言失败）/ 删未用 `times` import / "4 模块"数据点修正 / 各 Task 增"前置"声明
  - **santa C-1 消解**: Task 3（fep-security-impl 改动）剔除后，本 Plan 不再改 fep-security-impl main 代码，`-o` stale jar 关注点消失
  - **santa C-2**: Task 2 验收 4 补披露（@OperationLog 仅注解存在性，standalone 无 AOP，与模块既有惯例一致）

**目标:** ① 收口 S1 遗留的冷接口 legacy 明文凭证滞留（admin 端点主动批量迁移，muzhou 2026-06-12 拍板形态）；② F4 Mode E 治理措辞 doc-rot **残余面**清理（S5 已覆盖主锚点并 merge；残余 34 处全量扩批，措辞对齐 S5 已签字版式，muzhou 2026-06-12 二次拍板）；③ R4 TestKeyServiceConfiguration↔MockKeyService 平行 mock 收敛（前置审计已完成，证据见 B5）。

**前置依赖:** S1（PR #73 MERGED）+ S2a（PR #80 MERGED `46bdb279`）+ S5（PR #82 MERGED `52ce5fe7`，v0.4 轮实测）已闭环。其余并发见 B7 披露。

**执行 Worktree:** `/Users/muzhou/FEP_v1.0_wt-s1-followup`（分支 `feat/gm-s1-followup-sweep`，触发条件第 1 项：跨 4 个 Maven 模块 fep-web / fep-security-api / fep-processor / fep-converter；另命中红线 `feedback_worktree_isolates_fs_not_logic_domain`：3 个别会话 worktree 活跃[v0.5 实测]，多会话并发须会话级隔离；worktree 基于 baseline `52ce5fe7` 创建）

**架构:** sweep 复用既有 `CallbackLegacyCredentialMigrator.migrateToActiveKey`（REQUIRES_NEW 逐行隔离 + 幂等 + C1 双重守护），仅新增枚举查询 + 编排循环 + admin 端点；不引入任何新密码学原语。F4/R4 为注释与测试基建级改动。

**技术栈:** Java 17 / Spring Boot 3.x / Maven / Mockito + AssertJ / MockMvc standaloneSetup

**AI 协同模式:**

| 模式 | AI 占比 | 本计划中的应用 |
|:----:|:-------:|--------------|
| A | 90% | 全部 5 个 Task（sweep 编排/REST 端点/注释清理/mock 收敛；无新密码学原语，无 ⛔ Mode E Task） |

**评审规格:** Plan santa 独立评审（Round 1 REVISE 已闭合 → Round 2 复核）→ muzhou 签字 → 实施；每 Task 完成后独立 spec + quality review subagent（红线 `feedback_task_review_discipline`）；final whole-impl review。无新密码学算法故不强制密码学专项 review（santa Round 1 检查项 7 同判）。

**回归验收（红线 `feedback_plan_regression_scope_explicit` 两层声明）:**
- **Strong（权威门禁）**: GHA `Build/Test & Quality` PASS（S1/S4/S2a 三连先例：本机 load 常 100+，全量回归委托 GHA）
- **Minimum（本机）**: 每 Task 焦点测试 GREEN + 改动模块 `./mvnw -pl <module> -o test`（**不带 `-am`**，红线 `feedback_single_module_regression_no_am_flag`）+ `compile` 后 `spotbugs:check` BugInstance 0（红线 `feedback_spotbugs_check_needs_recompile_after_annotation`）
- mvn 前缀强制：`export JAVA_HOME=/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home; export PATH="$JAVA_HOME/bin:$PATH"`；长跑输出一律 `> /tmp/<log>.log 2>&1`（禁 `|tail`，红线 `feedback_pipe_tail_deadlock_with_bg_bash`）

**PR 预算:** 新增 ≈ 75 main + 110 test，删除 ≈ 55（R4 收敛）+ 注释/字符串置换 ≈ 70 行 → 远低于 400 行新增，无需 PR-Size 豁免。

---

## 设计背景

### B1 — sweep 需求与现状（S1 Daily §6 风险记录）

S1 惰性双读迁移（`CallbackLegacyCredentialMigrator`）只在**读路径**触发：`CallbackCredentialResolver.resolveToken/resolveOAuth2` 检测 legacy key_id → 明文直用 + 触发 REQUIRES_NEW 重加密。**冷接口（无回调流量）凭证永不被读 → 明文永久滞留**。监控 gauge `fep_callback_credential_legacy_remaining`（`CallbackMetrics.registerLegacyCredentialGauge`，migrator @PostConstruct 注册）可见但无收口手段。

**muzhou 拍板（2026-06-12 AskUserQuestion）**: Admin 端点形态 — `POST /api/v1/callback/credentials/migrate-legacy`，挂既有 `CallbackCredentialController`（ROLE_ADMIN + @OperationLog），运维 DB 备份后主动触发（S1 风险：部分迁移后回退有损，回滚窗口=首行迁移前）。逐行 REQUIRES_NEW 隔离，单行失败不阻断（计数披露）。

### B2 — 既有 API 实测（红线 `feedback_plan_must_grep_actual_api`，2026-06-12 主会话逐文件 grep；santa Round 1 核验项 1 全 ✅）

| API | 实测签名/事实 | 位置 |
|---|---|---|
| `CallbackLegacyCredentialMigrator.migrateToActiveKey(String interfaceId)` | `@Transactional(REQUIRES_NEW)`，幂等（非 legacy 直接 return），内部已 `metrics.recordCredentialMigrated()` + C1 运行时守护 | migration/CallbackLegacyCredentialMigrator.java:110-132 |
| `CallbackCredentialRepository` | 现有 `findByInterfaceId` / `deleteByInterfaceId` / `countByKeyIdIn(Collection<String>)`；**无按 keyId 集合枚举行的方法 → Task 1 新增** | repository/CallbackCredentialRepository.java |
| `CallbackCredentialEntity` | **构造器 protected（:101）、无 setter（:27 设计禁旁路状态机）**；静态工厂 `newToken(String interfaceId, byte[] tokenCipher, String tokenHeader, String keyId, LocalDateTime expiresAt)`（:115-127，tokenCipher 非 null 必填）/ `newOauth`（:144） | domain/CallbackCredentialEntity.java |
| `CallbackLegacyCredentialKeyIdProperties.getLegacyPlaintextKeyIds()` | 返回 `List<String>`（live 引用）；**字段默认值 `["mock-key-v1"]`（:23）**；setter null guard :34-37 | migration/CallbackLegacyCredentialKeyIdProperties.java |
| `CallbackCredentialAdminService` | `@Service @Transactional`（类级），构造注入 repo/facade/tokenCache（3 参，将扩 5 参）；构造器级 `@SuppressFBWarnings(EI_EXPOSE_REP2)` 既有 justification 覆盖新增 Spring 单例参 | service/CallbackCredentialAdminService.java |
| `CallbackCredentialController` | `@RequestMapping("/api/v1/callback/credentials")` + 类级 `@PreAuthorize("hasRole('ADMIN')")`；rotate-key 端点先例 :126 | controller/CallbackCredentialController.java |
| `ApiResult.success(T)` / `ApiResult.success()` | 既有响应包装 | common.domain.ApiResult |
| `LogSanitizer.sanitize(String)` | `public static`，CRLF 清洗 | fep-common util/LogSanitizer.java:38 |
| Controller 测试基调 | `MockMvcBuilders.standaloneSetup` + `GlobalExceptionHandler`（@WebMvcTest 会触发 JPA 装配，规避） | CallbackCredentialControllerTest.java:42-48 |
| Service 测试基调 | `@ExtendWith(MockitoExtension.class)`（**默认 STRICT_STUBS**）+ `@Mock`/`@InjectMocks` | CallbackCredentialAdminServiceTest.java |

### B3 — 原 S2a Simplify 微调批：S5 T7 已覆盖，整体剔除（v0.2）

别会话 `wt-gm-s5` commit `b90429f6`（git show 实测）已 ship：F1/F2（KeyServiceImplTest:126/186/187 + Sm2LoginDecryptionProviderImplTest:90 全部 import 化）/ F3（SM4-CBC→事实修正）/ R5（`setLoginKeys` null guard，与 setSm4Keys 对称）/ F7（newService/keys 测试 helper static 化 — v0.1 曾因落盘记录缺失剔除，现真身复原）/ F5a（其 T1 完成）/ F4 主锚点（KeyService + MockKeyService ×3 + TestKeyServiceConfiguration ×4）。**本 Plan 不再触碰上述任何锚点**（避免 race-loser 冲突，红线 `session_start_existing_worktree_must_question` 精神）。

### B4 — F4 残余面（v0.4 在新 baseline `52ce5fe7` 上无 carve-out 全仓 grep 亲测：窄 pattern 8 行[除豁免] + 宽 "Mode E" 27 行恰为 7 文件，逐行与下表吻合；santa Round 2 增补 22 处）

**处置 34 处（4 模块 15 文件）+ 豁免 1 处 + S5 已清理 1 处：**

| # | 文件 | 锚点 | 现状措辞 | 处置措辞类别 |
|:-:|---|---|---|---|
| 1 | `fep-web/.../outbound/consumer/OutboundSignAdapter.java` | :23-26 类 Javadoc | "⛔ Mode E 边界 … ③ 安全工程师人工编写（AI 禁入）" | S2b 边界（对齐 S5 版式） |
| 2 | 同上 | :48 @param signService | "真实实现 ⛔ Mode E" | S2b 边界 |
| 3 | 同上 | :49 @param keyService | "真实实现 ⛔ Mode E" | 已实装（KeyServiceImpl S1/S2a） |
| 4 | `fep-security-api/.../CryptoService.java` | :8 | "接口变更需安全工程师确认，实现类为 AI 禁入区域" | 已实装（SM4 加解密 S1） |
| ~~5~~ | ~~`fep-security-api/.../SignService.java`~~ | ~~:8~~ | **已被 S5 PR #82 清理（重写 Javadoc 含 🔓 措辞），剔除** | — |
| 6 | `fep-web/.../callback/credential/crypto/CallbackCredentialEncryptionFacade.java` | :19 | "fep-security-impl（AI 禁入区域，由安全专家人工编写）" | 已实装 |
| 7 | `fep-web/.../callback/credential/service/CallbackCredentialAdminService.java` | :137 rotateKey Javadoc | "适用于 ③ 安全专家轮换活跃 SM4 主密钥后" | "适用于运维轮换活跃 SM4 主密钥后"（③ 角色已撤销） |
| 8 | `fep-web/src/test/.../architecture/PackageStructureTest.java` | :27 .because 文案 | "(⛔ AI 禁入区域)" | "(密钥材料隔离域——2026-06-07 解禁后分层隔离保留)"；**ArchUnit 规则本体逐字不动** |
| 9 | `fep-processor/.../reconciliation/ClearingInstructionService.java` | :39-44 h3 + 段落 | "Mode E 安全守护 … ⛔ Mode E 禁入区域，安全实现尚未到位 … 待安全专家集成后再回填" | "PK7 签名守护：国密 SM2 签名实现属 S2b（🔓 解禁治理，待 §0.3 决策门），尚未到位 … 待 S2b 实施后再回填"；**守护行为逐字不动** |
| 10 | 同上 | :92 | "Mode E PK7 字段" | "PK7 签名字段（S2b 未实施守护）" |
| 11 | 同上 | :110 | "Mode E 安全守护：PK7 字段必须为 null" | "S2b 守护：PK7 字段必须为 null" |
| 12 | `fep-converter/src/test/.../ConverterAutoConfigurationTest.java` | :20 | "fep-security-impl 的 AI 禁入区域" | 已实装/隔离域措辞 |
| 35 | `fep-security-api/.../package-info.java` | :7 | "接口变更需经安全工程师审核"（v0.5 增补，santa Round 3 揪出验证盲区） | 专项："接口变更经密码学专项 review + muzhou 签字（🔓 2026-06-07 解禁治理）" |
| 13 | `fep-processor/src/test/.../architecture/ProcessorArchitectureTest.java` | :29 .because 文案 | "security.impl 必须由安全专家人工编写" | 隔离域文案（同 #8）；**ArchUnit 规则本体逐字不动** |
| 14 | `fep-web/src/test/.../callback/CallbackModuleArchTest.java` | :96 | "（国密实现，③ 安全专家人工编写）" | 隔离域文案；**规则本体逐字不动** |
| 15-17 | `fep-web/.../reconciliation/controller/SettlementInstructionController.java` | :44 / :131 / :145 | "PK7 字段守护（Mode E 安全集成边界）" / "the Mode E security…" / "Mode E real signing pending" | PK7→S2b |
| 18 | `fep-web/.../reconciliation/dto/QsInfoRequest.java` | :17-18（块） | "Mode E 安全集成尚未到位" | PK7→S2b |
| 19-25 | `fep-web/.../reconciliation/dto/SettlementInstructionRequest.java` | :23 / :54 / :57 / :60 / :161 / :179 / :197 | 类 Javadoc + @Schema "（Mode E 集成前必须为 null）" ×3 + getter Javadoc "(Mode E placeholder…)" ×3 | PK7→S2b（@Schema 为 OpenAPI 展示字符串，零执行语义） |
| 26-31 | `fep-processor/src/test/.../reconciliation/ClearingInstructionServiceTest.java` | :36 / :37 / :38 / :92 / :108 / :120 | 类 Javadoc "（Mode E 守护）" ×3 + @DisplayName "(Mode E)" ×3 | PK7→S2b（DisplayName 为展示字符串） |
| 32-34 | `fep-web/src/test/.../integration/p2e/ReconciliationE2EIntegrationTest.java` | :96 / :98 / :476 | "Mode E + Flyway F…" h3 / "Mode E security guard contract" / "(Mode E security guard)" | PK7→S2b |
| 豁免 | `fep-web/.../requeststate/BlockedMessageTypes.java` | :21 | "依赖 ③ 安全工程师 + PRD instruction_id 协议" | **不动** — 与 P3 Phase 2 🚫 BLOCKED ADR 现行表述一致（CLAUDE.md 下一步候选 #5 同措辞），非 doc-rot；在此披露 |

**统一措辞版式（v0.4 对齐 S5 已签字的 SignService 新 Javadoc 版式 — S5 后 SignServiceImpl 已实装，"未实装"类别消失）:**
- **签名链已实装 + wiring 待定**（#1/2，镜像 SignService.java:15-17 S5 版式）: `真实实现 SignServiceImpl（fep-security-impl，GM S5）已由 AI 编写 + 密码学专项 review；SM2 报文签验 wiring 与落地形态（外部签名验签服务器 1818 vs 进程内）待架构 §0.3 决策门定调（S2b）。真实密钥材料部署期注入，永不入 repo。`
- **已实装类**（#3/4/6/12）: `真实实现 KeyServiceImpl/CryptoServiceImpl（fep-security-impl）已由 AI 编写 + 密码学专项 review（🔓 2026-06-07 解禁，S1/S2a 实装）；真实密钥材料永不入 repo，部署期注入。`
- **专项措辞**（#7/8/9/10/11/#35）按表内文案。
- **隔离域文案**（#13/14，同 #8 版式）: `security.impl 为密钥材料隔离域（2026-06-07 解禁后分层隔离保留），<原依赖方向语义不变>`。
- **PK7→S2b**（#15-34）: "Mode E" → "S2b（🔓 待 §0.3）"，例 "PK7 签名元素（S2b 集成前必须为 null）" / "@DisplayName(...(S2b 守护))"；**PK7 守护行为与测试断言逐字不动**，仅展示字符串与注释（签名原语已实装[S5]，报文/PK7 签验 wiring 未接，守护仍必要——措辞准确性经 v0.4 重核）。

**数据点自洽（v0.5）**: 签名链已实装+wiring 待定 2 + 已实装 4 + 专项 6（#7-11 + #35）+ 隔离域 2 + PK7→S2b 20 = **34** ✅；distinct 文件 14 + 1（package-info 增补）= **15** ✅。

**噪声豁免说明（santa C-NEW-1）**: 宽 pattern 中 "must NOT" 另命中 ~12 文件英文惯用语（"must NOT mask/leak/throw…"），与治理措辞无关，不入表不处置。

**表外残留兜底**: 实施期 Step 3 验证 grep 若出表外同类残留 → 按对应措辞类别补置换并在 commit msg 披露（防打地鼠遗漏），禁静默跳过。

### B5 — R4 前置审计结论（起草期已完成，santa Round 1 核验项 4 全 ✅；v0.4 注：S5 T7 仅改匿名类内注释措辞不改行为，审计结论在新 baseline `52ce5fe7` 下不变）

**审计命题:** fep-web 测试是否依赖 `TestKeyServiceConfiguration` 匿名 KeyService 的**具体密钥材料**（零字节 key），使其无法直接换成 `MockKeyService`（0x01..0x20 / 0x30..0x3F 常量 key）？

**实测证据（2026-06-12，santa 独立复验通过）:**
1. `fep-web/pom.xml:27-30` — fep-security-mock 为 **default（compile）scope** 依赖 → 测试类路径可直接 `new MockKeyService()`，不依赖 @Profile("dev") bean 装配。
2. fep-web/src/test 硬编码密文 fixture = **0 处**（凭证加解密测试全为 roundtrip 或匿名自包含 KeyService）。
3. `getSignPrivateKey` 测试消费方全部 Mockito stub（OutboundSignAdapterTest ×4）或测试私有匿名类（CallbackCredentialEncryptionFacadeTest:141），无人消费 TestKeyServiceConfiguration 的 sign key 值。
4. 行为对齐（santa 逐方法核验）：sentinel 公钥/keyId/login keyId/Base64 decrypt 逐字等价；`getSm4CredentialMasterKey(String)` SHA-256 前 16 字节派生算法一致（MockKeyService.deriveMock16 :96-106 ↔ 匿名类同算法）。唯二差异为 key 字节值，由证据 2/3 知无测试断言依赖。
5. `@Service @Profile("dev") @ConditionalOnProperty` 对 `new` 直接实例化惰性，prod 装配语义零变化。
6. （santa C-4 正面补强）收敛后同 JVM 全 context 统一 MockKeyService 常量 key，**消除**此前 zeros（TestConfig）vs 0x30..（dev-profile MockKeyService）潜在混用面。

**结论:** 收敛 = `TestKeyServiceConfiguration.keyService()` 方法体改为 `return new MockKeyService();`，删除 ~52 行匿名类。`@Configuration @ConditionalOnProperty(provider=mock, matchIfMissing=true)` + `@ConditionalOnMissingBean` 门控不动。残余风险由 Task 3 Step 2 全模块回归覆盖。

### B6 — 既有 Deferred 处置盘点（本轮不实施项）

- **QUAL-S1**（resolver 双读分支抽 helper）: 继续 Deferred — sweep 收口 + 未来 prod cutover 后 legacy 分支整体可删，为将死代码加抽象违反 YAGNI。
- **REUSE-S1**（EI_EXPOSE justification 常量，实测 13 处全 fep-web）: muzhou 2026-06-12 拍板继续 Deferred（机械 churn 稀释安全 PR 评审焦点，留待 cosmetic 批）。
- **R2/EFF-3**（S2a）: 触发再议，不动。

### B7 — 并发会话披露（红线 `parallel_session_task_allocation_discipline` T0 实测 2026-06-12）

`git worktree list` 实测 3 个别会话 worktree + 1 个已 teardown 留档（**勿动**）：

| Worktree | 分支 | 活跃证据 | 与本 Plan 交集 |
|---|---|---|---|
| `.e2e` | e2e/p7.1-smoke-local | 长期驻留 | 无 |
| ~~`wt-gm-s5`~~ | ~~feat/gm-s5-audit-integrity~~ | **v0.4 已 teardown**（PR #82 merge 进 baseline `52ce5fe7`）| 撞车收缩（B3）+ R4 冲突预案均已消解；其 T7 措辞改动已在 baseline 内 |
| `wt-rule-batch2` | feat/rule-master-batch2 | M ×2 + ?? ×2 + commits（v0.4 实测推进至 `4dc231d6`） | 其触 fep-processor rule 类 + fep-web application.yml；本 Plan 触 fep-processor `reconciliation/ClearingInstructionService(Test).java`（**不同文件，无行级冲突预期**），披露备查 |
| `wt-simplify-q-drain` | chore/simplify-q-drain-p4-msg-i | PR #29 历史遗留 | 无 |

**T4 前（实施启动时）须按红线 4 时点纪律重测本表。**

### 文件结构

| 文件路径 | 职责 | 操作 | AI 模式 | Task |
|----------|------|------|:-------:|:----:|
| `fep-web/.../callback/credential/repository/CallbackCredentialRepository.java` | + `findByKeyIdIn` 枚举 legacy 行 | 修改 | A | 1 |
| `fep-web/.../callback/credential/dto/CallbackCredentialSweepResponse.java` | sweep 结果 DTO（record） | 新建 | A | 1 |
| `fep-web/.../callback/credential/service/CallbackCredentialAdminService.java` | + `migrateLegacy()` 编排；:137 措辞（F4 #7） | 修改 | A | 1, 4 |
| `fep-web/.../callback/credential/controller/CallbackCredentialController.java` | + `POST /migrate-legacy` 端点 | 修改 | A | 2 |
| `fep-web/src/test/.../service/CallbackCredentialAdminServiceTest.java` | + sweep 单测 3 个 | 修改 | A | 1 |
| `fep-web/src/test/.../controller/CallbackCredentialControllerTest.java` | + 端点单测 1 个 | 修改 | A | 2 |
| `fep-web/src/test/.../config/TestKeyServiceConfiguration.java` | R4 收敛 → MockKeyService | 修改 | A | 3 |
| `fep-web/.../outbound/consumer/OutboundSignAdapter.java` | F4 #1-3 | 修改 | A | 4 |
| `fep-security-api/.../CryptoService.java` | F4 #4 | 修改 | A | 4 |
| `fep-security-api/.../package-info.java` | F4 #35 | 修改 | A | 4 |
| `fep-web/.../callback/credential/crypto/CallbackCredentialEncryptionFacade.java` | F4 #6 | 修改 | A | 4 |
| `fep-web/src/test/java/com/puchain/fep/architecture/PackageStructureTest.java` | F4 #8 | 修改 | A | 4 |
| `fep-processor/.../reconciliation/ClearingInstructionService.java` | F4 #9-11 | 修改 | A | 4 |
| `fep-converter/src/test/.../converter/ConverterAutoConfigurationTest.java` | F4 #12 | 修改 | A | 4 |
| `fep-processor/src/test/.../architecture/ProcessorArchitectureTest.java` | F4 #13 | 修改 | A | 4 |
| `fep-web/src/test/.../callback/CallbackModuleArchTest.java` | F4 #14 | 修改 | A | 4 |
| `fep-web/.../reconciliation/controller/SettlementInstructionController.java` | F4 #15-17 | 修改 | A | 4 |
| `fep-web/.../reconciliation/dto/QsInfoRequest.java` | F4 #18 | 修改 | A | 4 |
| `fep-web/.../reconciliation/dto/SettlementInstructionRequest.java` | F4 #19-25 | 修改 | A | 4 |
| `fep-processor/src/test/.../reconciliation/ClearingInstructionServiceTest.java` | F4 #26-31 | 修改 | A | 4 |
| `fep-web/src/test/.../integration/p2e/ReconciliationE2EIntegrationTest.java` | F4 #32-34 | 修改 | A | 4 |

### 共享工具类清单

无新增共享工具（sweep 复用既有 `CallbackLegacyCredentialMigrator` / `LogSanitizer`；各 Task 间无新共享逻辑）。

### 核心类职责边界声明 — CallbackCredentialAdminService（依赖将达 5）

**负责**: 凭证 admin CRUD + 密钥轮换 + legacy 批量迁移**编排**（循环调 migrator，计数聚合）
**不负责**: legacy 判别与单行迁移事务语义 → `CallbackLegacyCredentialMigrator`；加解密 → `CallbackCredentialEncryptionFacade`
**依赖**: repo / facade / tokenCache / **+migrator / +legacyProps** = 5 个（上限 7 内）
**行数**: 现 ~230 行 + ~45 → ~275（上限 300 内）
**如果超出**: 拆 `CallbackCredentialSweepService`

---

## Task 1: legacy 凭证批量迁移服务层 `模式 A`

**前置:** 无（本 Plan 首个 Task）
**PRD 依据:** v1.3 §5.5.3 凭证配置 + §8.3 敏感数据保护
**追溯 ID:** FR-INFRA-CALLBACK-CREDENTIAL

**验收标准（从 PRD §8.3 "敏感数据不得明文滞留" + S1 既定迁移语义推导）:**
1. 库中存在 N 行 legacy key_id 凭证 → `migrateLegacy()` 对每行调用一次 `migrator.migrateToActiveKey(interfaceId)`，返回 `migrated=N, failed=0`
2. 某行迁移抛 RuntimeException → 该行计入 `failed`，**其余行继续迁移**（逐行 REQUIRES_NEW 隔离），不向上抛
3. 0 行 legacy → 返回 `(0, 0, remaining)` 且不调用 migrator
4. `remaining` = 迁移循环后 `repo.countByKeyIdIn(legacyKeyIds)` 实测值（含失败残留）
5. WARN 日志中 interfaceId 必须经 `LogSanitizer.sanitize`（红线 `feedback_logsanitizer_alone_insufficient_for_findsecbugs`：同 commit 加 `@SuppressFBWarnings("CRLF_INJECTION_LOGS")`）
6. `migrateLegacy()` 不得运行在外层长事务中（类级 @Transactional 须以 `Propagation.NOT_SUPPORTED` 覆盖，保 migrator REQUIRES_NEW 逐行短事务语义）

**Files:**
- Modify: `fep-web/src/main/java/com/puchain/fep/web/callback/credential/repository/CallbackCredentialRepository.java`
- Create: `fep-web/src/main/java/com/puchain/fep/web/callback/credential/dto/CallbackCredentialSweepResponse.java`
- Modify: `fep-web/src/main/java/com/puchain/fep/web/callback/credential/service/CallbackCredentialAdminService.java`
- Modify: `fep-web/src/test/java/com/puchain/fep/web/callback/credential/service/CallbackCredentialAdminServiceTest.java`

- [ ] **Step 1: 编写失败测试**（在 `CallbackCredentialAdminServiceTest` 追加；entity 经 `newToken` 静态工厂构造[santa B-1]；legacyProps stub 在 `migrateLegacy()` 调用期被惰性消费[santa B-2]，STRICT_STUBS 成立）

```java
// 追加到 CallbackCredentialAdminServiceTest.java（imports 增补:
//   com.puchain.fep.web.callback.credential.dto.CallbackCredentialSweepResponse;
//   com.puchain.fep.web.callback.credential.migration.CallbackLegacyCredentialMigrator;
//   com.puchain.fep.web.callback.credential.migration.CallbackLegacyCredentialKeyIdProperties;
//   java.util.List; org.mockito.Mockito.doThrow;
//   org.mockito.ArgumentMatchers.anyCollection;）

    @Mock
    private CallbackLegacyCredentialMigrator migrator;

    @Mock
    private CallbackLegacyCredentialKeyIdProperties legacyProps;

    @Test
    void migrateLegacySweepsEveryLegacyRow() {
        when(legacyProps.getLegacyPlaintextKeyIds()).thenReturn(List.of("mock-key-v1"));
        final CallbackCredentialEntity a = CallbackCredentialEntity.newToken(
                "IF-COLD-1", new byte[]{1}, null, "mock-key-v1", null);
        final CallbackCredentialEntity b = CallbackCredentialEntity.newToken(
                "IF-COLD-2", new byte[]{2}, null, "mock-key-v1", null);
        when(repo.findByKeyIdIn(anyCollection())).thenReturn(List.of(a, b));
        when(repo.countByKeyIdIn(anyCollection())).thenReturn(0L);

        final CallbackCredentialSweepResponse resp = svc.migrateLegacy();

        verify(migrator).migrateToActiveKey("IF-COLD-1");
        verify(migrator).migrateToActiveKey("IF-COLD-2");
        assertThat(resp.migrated()).isEqualTo(2);
        assertThat(resp.failed()).isZero();
        assertThat(resp.remaining()).isZero();
    }

    @Test
    void migrateLegacySingleRowFailureDoesNotAbortSweep() {
        when(legacyProps.getLegacyPlaintextKeyIds()).thenReturn(List.of("mock-key-v1"));
        final CallbackCredentialEntity bad = CallbackCredentialEntity.newToken(
                "IF-BAD", new byte[]{1}, null, "mock-key-v1", null);
        final CallbackCredentialEntity ok = CallbackCredentialEntity.newToken(
                "IF-OK", new byte[]{2}, null, "mock-key-v1", null);
        when(repo.findByKeyIdIn(anyCollection())).thenReturn(List.of(bad, ok));
        when(repo.countByKeyIdIn(anyCollection())).thenReturn(1L);
        doThrow(new IllegalStateException("re-encrypt produced legacy keyId"))
                .when(migrator).migrateToActiveKey("IF-BAD");

        final CallbackCredentialSweepResponse resp = svc.migrateLegacy();

        verify(migrator).migrateToActiveKey("IF-OK");
        assertThat(resp.migrated()).isEqualTo(1);
        assertThat(resp.failed()).isEqualTo(1);
        assertThat(resp.remaining()).isEqualTo(1L);
    }

    @Test
    void migrateLegacyWithNoLegacyRowsIsNoOp() {
        when(legacyProps.getLegacyPlaintextKeyIds()).thenReturn(List.of("mock-key-v1"));
        when(repo.findByKeyIdIn(anyCollection())).thenReturn(List.of());
        when(repo.countByKeyIdIn(anyCollection())).thenReturn(0L);

        final CallbackCredentialSweepResponse resp = svc.migrateLegacy();

        verify(migrator, never()).migrateToActiveKey(any());
        assertThat(resp.migrated()).isZero();
        assertThat(resp.failed()).isZero();
    }
```

- [ ] **Step 2: 运行测试确认失败**

```bash
export JAVA_HOME=/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home; export PATH="$JAVA_HOME/bin:$PATH"
cd /Users/muzhou/FEP_v1.0_wt-s1-followup
./mvnw -pl fep-web -o test -Dtest=CallbackCredentialAdminServiceTest --batch-mode > /tmp/t1-red.log 2>&1; tail -30 /tmp/t1-red.log
```
期望: 编译失败 — `找不到符号: 方法 findByKeyIdIn` / `migrateLegacy` / `类 CallbackCredentialSweepResponse`

- [ ] **Step 3: 最小实现 — repository 方法**

```java
// CallbackCredentialRepository.java 追加（import java.util.List 增补）:

    /**
     * 枚举 key_id 属于给定集合（legacy 明文标记）的全部凭证行（主动批量迁移用）。
     *
     * @param keyIds legacy keyId 集合
     * @return 匹配的凭证实体列表
     */
    List<CallbackCredentialEntity> findByKeyIdIn(Collection<String> keyIds);
```

- [ ] **Step 4: 最小实现 — DTO record**

```java
// 新建 fep-web/src/main/java/com/puchain/fep/web/callback/credential/dto/CallbackCredentialSweepResponse.java
package com.puchain.fep.web.callback.credential.dto;

/**
 * legacy 明文凭证批量迁移结果（密文/明文均不回显，仅计数）。
 *
 * <p>参见 PRD v1.3 §5.5.3 凭证配置（FR-INFRA-CALLBACK-CREDENTIAL）+ §8.3 敏感数据保护。</p>
 *
 * @param migrated  本次成功迁移行数
 * @param failed    本次迁移失败行数（详情见 WARN 日志，单行失败不阻断扫描）
 * @param remaining 迁移后仍滞留 legacy 明文的行数（监控 gauge 同源计数）
 * @author FEP Team
 * @since 1.0.0
 */
public record CallbackCredentialSweepResponse(int migrated, int failed, long remaining) {
}
```

- [ ] **Step 5: 最小实现 — AdminService 编排方法**（构造器追加 migrator + legacyProps 两参并存字段；既有构造器级 `@SuppressFBWarnings(EI_EXPOSE_REP2)` justification 覆盖新增 Spring 单例参；legacy 集合**惰性读**[santa B-2]；类 imports 增补 `CallbackLegacyCredentialMigrator` / `CallbackLegacyCredentialKeyIdProperties` / `CallbackCredentialSweepResponse` / `LogSanitizer` / `Propagation` / `Logger` / `LoggerFactory` / `Set` / `HashSet`）

```java
// CallbackCredentialAdminService.java — 字段/构造器修改 + 新方法:

    private static final Logger LOG = LoggerFactory.getLogger(CallbackCredentialAdminService.class);

    private final CallbackLegacyCredentialMigrator migrator;
    private final CallbackLegacyCredentialKeyIdProperties legacyProps;

    // 构造器追加两参（既有 Javadoc 同步补 @param migrator/@param legacyProps）:
    //   final CallbackLegacyCredentialMigrator migrator,
    //   final CallbackLegacyCredentialKeyIdProperties legacyProps
    // 构造体追加:
    //   this.migrator = migrator;
    //   this.legacyProps = legacyProps;

    /**
     * 主动批量迁移全部 legacy 明文凭证（冷接口收口，运维 DB 备份后触发）。
     *
     * <p>逐行委托 {@link CallbackLegacyCredentialMigrator#migrateToActiveKey}
     * （REQUIRES_NEW 独立短事务 + 幂等 + C1 守护）；单行失败 WARN 计数后继续，
     * 不阻断扫描。本方法以 {@link Propagation#NOT_SUPPORTED} 覆盖类级事务，
     * 避免外层长事务包裹批量循环。legacy 集合每次调用从配置惰性读取。</p>
     *
     * @return 迁移/失败/剩余计数（不回显任何凭证内容）
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
            justification = "interfaceId sanitized via LogSanitizer.sanitize before logging")
    public CallbackCredentialSweepResponse migrateLegacy() {
        final Set<String> legacyKeyIds = new HashSet<>(legacyProps.getLegacyPlaintextKeyIds());
        final List<CallbackCredentialEntity> rows = repo.findByKeyIdIn(legacyKeyIds);
        int migrated = 0;
        int failed = 0;
        for (final CallbackCredentialEntity row : rows) {
            try {
                migrator.migrateToActiveKey(row.getInterfaceId());
                migrated++;
            } catch (final RuntimeException ex) {
                failed++;
                LOG.warn("legacy credential sweep failed for interfaceId={}; continuing",
                        LogSanitizer.sanitize(row.getInterfaceId()), ex);
            }
        }
        final long remaining = repo.countByKeyIdIn(legacyKeyIds);
        LOG.info("legacy credential sweep done: migrated={}, failed={}, remaining={}",
                migrated, failed, remaining);
        return new CallbackCredentialSweepResponse(migrated, failed, remaining);
    }
```

- [ ] **Step 6: 运行测试确认通过**

```bash
./mvnw -pl fep-web -o test -Dtest=CallbackCredentialAdminServiceTest --batch-mode > /tmp/t1-green.log 2>&1; tail -15 /tmp/t1-green.log
```
期望: `BUILD SUCCESS`，既有 + 新增 3 测试全 GREEN（既有测试经 @InjectMocks 5 参构造注入，构造期不调用 props，无 UnnecessaryStubbing）

- [ ] **Step 7: spotbugs + 提交**

```bash
./mvnw -pl fep-web -o compile spotbugs:check --batch-mode > /tmp/t1-sb.log 2>&1; grep -c "BugInstance size is 0" /tmp/t1-sb.log
git add fep-web/src/
git commit -m "$(cat <<'EOF'
feat(web): add legacy credential bulk sweep service (S1 follow-up C2)

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

## Task 2: sweep admin 端点 `模式 A`

**前置:** Task 1（DTO + service 方法）
**PRD 依据:** v1.3 §5.5.3 凭证配置（admin 管理操作）
**追溯 ID:** FR-INFRA-CALLBACK-CREDENTIAL

**验收标准:**
1. `POST /api/v1/callback/credentials/migrate-legacy` → 200，body `$.data.migrated/.failed/.remaining` 为 service 返回计数
2. 响应不含任何凭证明文/密文字段（仅 3 个计数）
3. 端点受类级 `@PreAuthorize("hasRole('ADMIN')")` 覆盖（standalone 测试不验鉴权，与既有测试类自述一致）
4. `@OperationLog` 注解存在（type=UPDATE）— 仅注解存在性验收：standalone MockMvc 无 AOP，落日志由生产链路覆盖，与模块既有端点测试惯例一致（santa C-2 披露）

**Files:**
- Modify: `fep-web/src/main/java/com/puchain/fep/web/callback/credential/controller/CallbackCredentialController.java`
- Modify: `fep-web/src/test/java/com/puchain/fep/web/callback/credential/controller/CallbackCredentialControllerTest.java`

- [ ] **Step 1: 编写失败测试**

```java
// CallbackCredentialControllerTest.java 追加（import CallbackCredentialSweepResponse）:

    @Test
    void postMigrateLegacyReturnsCountsOnly() throws Exception {
        when(service.migrateLegacy())
                .thenReturn(new CallbackCredentialSweepResponse(3, 1, 1L));

        mvc.perform(post("/api/v1/callback/credentials/migrate-legacy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.migrated").value(3))
                .andExpect(jsonPath("$.data.failed").value(1))
                .andExpect(jsonPath("$.data.remaining").value(1))
                .andExpect(jsonPath("$.data.token").doesNotExist())
                .andExpect(jsonPath("$.data.tokenCiphertext").doesNotExist());
        verify(service).migrateLegacy();
    }
```

- [ ] **Step 2: 运行测试确认失败**

```bash
./mvnw -pl fep-web -o test -Dtest=CallbackCredentialControllerTest --batch-mode > /tmp/t2-red.log 2>&1; tail -20 /tmp/t2-red.log
```
期望: **编译通过**（service.migrateLegacy/DTO 已于 Task 1 建立），新测试 FAIL — standalone MockMvc 无 `/migrate-legacy` 映射 → 404，`status().isOk()` 断言失败（santa m-1 修订）

- [ ] **Step 3: 最小实现 — controller 端点**

```java
// CallbackCredentialController.java 追加（import CallbackCredentialSweepResponse）:

    /**
     * 批量迁移 legacy 明文凭证（冷接口收口；运维 DB 备份后触发）。
     *
     * @return 迁移/失败/剩余计数（不回显凭证内容）
     */
    @PostMapping("/migrate-legacy")
    @OperationLog(module = "回调凭证管理", type = OperationType.UPDATE,
            description = "批量迁移 legacy 明文凭证")
    @Operation(summary = "批量迁移 legacy 明文凭证",
            description = "逐行 REQUIRES_NEW 重加密为活跃 SM4 主密钥；单行失败不阻断，计数披露。"
                    + "迁移有损回退（回滚窗口=首行迁移前），触发前须 DB 备份")
    @ApiResponse(responseCode = "200", description = "扫描完成（含失败计数）")
    public ApiResult<CallbackCredentialSweepResponse> migrateLegacy() {
        return ApiResult.success(service.migrateLegacy());
    }
```

- [ ] **Step 4: 确认通过 + 提交**

```bash
./mvnw -pl fep-web -o test -Dtest=CallbackCredentialControllerTest --batch-mode > /tmp/t2-green.log 2>&1; tail -15 /tmp/t2-green.log
./mvnw -pl fep-web -o compile spotbugs:check --batch-mode > /tmp/t2-sb.log 2>&1; grep -c "BugInstance size is 0" /tmp/t2-sb.log
git add fep-web/src/
git commit -m "$(cat <<'EOF'
feat(web): expose POST /callback/credentials/migrate-legacy admin endpoint

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

## Task 3: R4 — TestKeyServiceConfiguration → MockKeyService 收敛 `模式 A`

**前置:** Task 2（无文件依赖，仅保持线性提交序）；实施前按 B7 检查 S5 merge 状态
**PRD 依据:** 元流程（测试基建去重，无 FR-ID — 元流程豁免）；审计证据见 B5

**验收标准:**
1. `TestKeyServiceConfiguration.keyService()` 方法体为 `return new MockKeyService();`，匿名类 ~52 行删除；`@Configuration`/`@ConditionalOnProperty`/`@ConditionalOnMissingBean` 门控逐字不动
2. 全 fep-web 单模块测试 0 fail（mock key 字节 zeros→MockKeyService 常量的变化不破坏任何断言 — B5 证据 2/3 预核）
3. Javadoc 更新为指向 MockKeyService 复用（消除"镜像"重复维护义务表述）

**Files:**
- Modify: `fep-web/src/test/java/com/puchain/fep/web/config/TestKeyServiceConfiguration.java`

- [ ] **Step 0: baseline 确认（v0.4 起 S5 已 merge，确认性检查）**

```bash
git merge-base --is-ancestor 52ce5fe7 HEAD && echo "S5 已在 baseline 内，无冲突预期" || echo "worktree 未含 S5 — 先 git merge origin/main"
```

- [ ] **Step 1: 收敛实现**（行为保持 refactor，以全模块既有测试为回归网，无新增 RED 测试 — wiring 类先例：高区分力既有断言替代独立 RED 轮，显式披露）

```java
// TestKeyServiceConfiguration.java keyService() 改为:
    /**
     * Returns the shared dev {@link MockKeyService}（fep-security-mock，compile-scope 依赖）。
     *
     * <p>直接 {@code new} 实例化绕过其 {@code @Profile("dev")} bean 装配门（注解对直接
     * 实例化惰性），消除此前 ~50 行逐方法镜像匿名类（Simplify R4）。仅在 context 无其他
     * {@link KeyService} bean 时注册；dev profile 下 fep-security-mock 的 bean 优先。</p>
     *
     * @return test KeyService implementation
     */
    @Bean
    @ConditionalOnMissingBean(KeyService.class)
    public KeyService keyService() {
        return new MockKeyService();
    }
// import com.puchain.fep.security.mock.MockKeyService; 增补；
// java.util.Base64 / StandardCharsets / MessageDigest 相关 import 与匿名类一并删除
```

- [ ] **Step 2: fep-web 全模块回归（本 Task 关键验证 — bean 影响多个 @SpringBootTest context）**

```bash
./mvnw -pl fep-web -o test --batch-mode > /tmp/t3-fepweb.log 2>&1; tail -25 /tmp/t3-fepweb.log
```
期望: `BUILD SUCCESS` 0 fail。**本机 load > 100 时**：fallback 至焦点集（`AuthControllerTest,LoginVerifierTest,Sm2LoginDecryptionProviderImplTest,Inbound2101WireTest,P5OutboundEndToEndIntegrationTest`）本机 GREEN + GHA 全量兜底（S1/S4/S2a 先例），并在 commit msg 与 PR body 披露。

- [ ] **Step 3: 提交**

```bash
git add fep-web/src/test/
git commit -m "$(cat <<'EOF'
refactor(web): converge TestKeyServiceConfiguration onto MockKeyService (Simplify R4)

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

## Task 4: F4 残余面 — Mode E 治理措辞 doc-rot 清理 34 处 `模式 A`

**前置:** Task 3（无文件依赖，仅保持线性提交序）
**PRD 依据:** 元流程（治理措辞与 2026-06-07 解禁决议对齐，无 FR-ID — 元流程豁免）

**验收标准:**
1. B4 表 34 处全部按表内措辞类别置换（签名链已实装+wiring 待定 2 / 已实装 4 / 专项 6 / 隔离域 2 / PK7→S2b 20），逐块对照 B4 表逐项核销；豁免 1 处（BlockedMessageTypes:21）不动；#5 已被 S5 清理不在本批
2. 纯注释/Javadoc/展示字符串改动 — 不改任何执行语义：PackageStructureTest/ProcessorArchitectureTest/CallbackModuleArchTest **ArchUnit 规则本体逐字不动**（仅 .because/Javadoc 文案）；ClearingInstructionService **PK7 守护行为与其测试断言逐字不动**；@Schema/@DisplayName 仅展示字符串
3. 改后窄 grep（Step 3 命令，仅排 BlockedMessageTypes 豁免）期望 0 行；宽 "Mode E" grep 期望全仓 0 行（v0.4 起 S5 已 merge，无 carve-out）；表外残留按 B4 兜底条款补置换并披露
4. 4 个改动模块焦点测试 0 fail（含 ClearingInstructionServiceTest 自身 DisplayName 改后 GREEN）

**Files:**（见文件结构表 Task 4 行，4 模块 15 文件）

- [ ] **Step 1: 置换前逐文件复核行号**（防 drift；路径已全部 `ls` dry-run 验证 — santa M-NEW-2 闭合）

```bash
for f in fep-web/src/main/java/com/puchain/fep/web/outbound/consumer/OutboundSignAdapter.java \
         fep-security-api/src/main/java/com/puchain/fep/security/api/CryptoService.java \
         fep-security-api/src/main/java/com/puchain/fep/security/api/package-info.java \
         fep-web/src/main/java/com/puchain/fep/web/callback/credential/crypto/CallbackCredentialEncryptionFacade.java \
         fep-web/src/main/java/com/puchain/fep/web/callback/credential/service/CallbackCredentialAdminService.java \
         fep-web/src/test/java/com/puchain/fep/architecture/PackageStructureTest.java \
         fep-processor/src/main/java/com/puchain/fep/processor/reconciliation/ClearingInstructionService.java \
         fep-converter/src/test/java/com/puchain/fep/converter/ConverterAutoConfigurationTest.java \
         fep-processor/src/test/java/com/puchain/fep/processor/architecture/ProcessorArchitectureTest.java \
         fep-web/src/test/java/com/puchain/fep/web/callback/CallbackModuleArchTest.java \
         fep-web/src/main/java/com/puchain/fep/web/reconciliation/controller/SettlementInstructionController.java \
         fep-web/src/main/java/com/puchain/fep/web/reconciliation/dto/QsInfoRequest.java \
         fep-web/src/main/java/com/puchain/fep/web/reconciliation/dto/SettlementInstructionRequest.java \
         fep-processor/src/test/java/com/puchain/fep/processor/reconciliation/ClearingInstructionServiceTest.java \
         fep-web/src/test/java/com/puchain/fep/web/integration/p2e/ReconciliationE2EIntegrationTest.java; do
  echo "=== $f"; grep -n "Mode E\|must NOT\|禁入\|安全工程师\|安全专家" "$f"; done
```

- [ ] **Step 2: 按 B4 表逐块置换**（示例 #1，其余 33 块按表内措辞类别同法，B4 表为唯一事实源）:

```java
// OutboundSignAdapter.java :23-26 原段:
//   <p><strong>⛔ Mode E 边界:</strong> 本适配器仅做编排（orchestration），不直接实现任何
//   国密密码学原语。所有 SM2/SM3 计算委托给 {@link SignService} 接口；私钥来源委托给
//   {@link KeyService} 接口。两个接口的真实实现位于 {@code fep-security-impl}，由
//   ③ 安全工程师人工编写（AI 禁入）。</p>
// 置换为（v0.4 镜像 S5 签字的 SignService.java:15-17 版式）:
 * <p><strong>🔓 2026-06-07 解禁治理:</strong> 本适配器仅做编排（orchestration），
 * 不直接实现任何国密密码学原语。所有 SM2/SM3 计算委托给 {@link SignService} 接口（真实实现
 * {@code SignServiceImpl}，GM S5 已实装 + 密码学专项 review）；私钥来源委托给
 * {@link KeyService} 接口（{@code KeyServiceImpl}，S1/S2a 已实装）。SM2 报文签验 wiring
 * 与落地形态（外部签名验签服务器 1818 vs 进程内）待架构 §0.3 决策门定调（S2b）。
 * 真实密钥材料部署期注入，永不入 repo。</p>
```

- [ ] **Step 3: 验证 + 提交**（v0.4 起 S5 已 merge 无 carve-out；两 grep 已在新 baseline dry-run：置换前窄 9 行[v0.5 加宽 pattern] + 宽 27 行/7 文件，与 B4 表 34 处映射，命令可执行判定明确）

```bash
grep -rn "③ 安全工程师\|③ 安全专家\|安全专家人工\|安全专家确认\|安全工程师审核\|安全工程师确认\|AI 禁入" --include="*.java" . | grep -v target | grep -v "BlockedMessageTypes"
# 期望 0 行
grep -rn "Mode E" --include="*.java" . | grep -v target
# 期望 0 行；任一 grep 出表外残留 → 按 B4 兜底条款补置换 + commit msg 披露
./mvnw -pl fep-security-api -o test --batch-mode > /tmp/t4-api.log 2>&1; tail -8 /tmp/t4-api.log
./mvnw -pl fep-processor -o test -Dtest=ClearingInstructionServiceTest,ProcessorArchitectureTest --batch-mode -Dsurefire.failIfNoSpecifiedTests=false > /tmp/t4-proc.log 2>&1; tail -8 /tmp/t4-proc.log
./mvnw -pl fep-converter -o test -Dtest=ConverterAutoConfigurationTest --batch-mode > /tmp/t4-conv.log 2>&1; tail -8 /tmp/t4-conv.log
./mvnw -pl fep-web -o test -Dtest=OutboundSignAdapterTest,PackageStructureTest,CallbackModuleArchTest --batch-mode > /tmp/t4-web.log 2>&1; tail -8 /tmp/t4-web.log
git add fep-web/ fep-security-api/ fep-processor/ fep-converter/
git commit -m "$(cat <<'EOF'
docs(security): align residual Mode E governance wording with 2026-06-07 GM unlock (F4)

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

## Task 5: 闭环 — PR + 门禁 + worktree 收尾 `模式 A`

**前置:** Task 1-4 全部完成
**PRD 依据:** 元流程（无 FR-ID）

- [ ] **Step 1: baseline 重测 + merge origin/main**（红线 `feedback_baseline_drift_during_long_review_cycle`：多会话并发，PR 前必须吸收别会话 merge；S5 冲突处置见 B7）

```bash
cd /Users/muzhou/FEP_v1.0_wt-s1-followup && git fetch
git log --oneline HEAD..origin/main | head   # 非空则 git merge origin/main 并重跑焦点测试（v0.4 起 S5 已在 baseline，无已知冲突源；新增冲突按通用处置）
```

- [ ] **Step 2: 开 PR + GHA 权威门禁**

```bash
git push -u origin feat/gm-s1-followup-sweep
gh pr create --title "feat(web/security): S1 follow-up legacy credential sweep + F4 residual drain + R4 mock convergence" --body "<对照本 Plan 各 Task 摘要 + Strong/Minimum 两层回归证据 + B5 审计证据 + B7 并发披露（含 S5 TestKeyServiceConfiguration 预期冲突处置）>"
# GHA Build/Test & Quality PASS 后 merge（squash）；遇网络错按红线 gh_mutation_network_error_verify_before_retry 先 read-verify
```

- [ ] **Step 3: 收尾**

```bash
# merge 后:
cd /Users/muzhou/FEP_v1.0 && git fetch && git merge --ff-only origin/main
git worktree remove /Users/muzhou/FEP_v1.0_wt-s1-followup
git branch -d feat/gm-s1-followup-sweep 2>/dev/null; git push origin --delete feat/gm-s1-followup-sweep 2>/dev/null
git worktree list   # 确认本会话 worktree 已移除；别会话 worktree（.e2e / wt-rule-batch2 / wt-simplify-q-drain 等）以当时实测为准，勿动（santa M-2 修订）
```

- [ ] **Step 4: CLAUDE.md「当前项目状态」+ memory 更新**（file write only — 红线 `feedback_fep_docs_repo_commit_taboo`）：S1 follow-up sweep ✅ / S2a Simplify Deferred 池清账（微调批由 S5 T7 承载；R4 本 Plan ship；R2/EFF-3/REUSE-S1/QUAL-S1 仍 Deferred）/ F4 残余面 ✅（BlockedMessageTypes 豁免留档）。四步收尾（Simplify 三审/9 维文档/Daily/push）由 session-end 统一执行。

---

## 自检清单核销（writing-plans 10 项，v0.2 重核）

1. **PRD 覆盖度**: 核心覆盖 FR-INFRA-CALLBACK-CREDENTIAL（凭证安全闭环）；Task 3/4/5 元流程豁免已逐个标注理由 ✅
2. **安全边界**: 全 Plan 无 `security/impl/` 改动（v0.2 起 fep-security-impl 零触碰）；无 ⛔ Mode E Task；sweep 复用既有迁移器不触加解密实现 ✅
3. **占位符扫描**: 无 TBD/TODO/类似 Task N（Task 4 Step 2 "其余 33 块同法"附 B4 完整对照表 + 五类完整措辞全文，非占位） ✅
4. **类型一致性**: `CallbackCredentialSweepResponse(int,int,long)` 在 Task 1/2 一致；`migrateLegacy()` 签名一致；`newToken` 5 参与实测 :115-127 一致 ✅
5. **测试命令可执行**: `-Dtest=` 类名逐个核对；fep-processor 焦点测试加 `-Dsurefire.failIfNoSpecifiedTests=false`（红线 surefire3 参数名） ✅
6. **CLAUDE.md 更新**: Task 5 Step 4 ✅
7. **验收标准完整性**: Task 1/2 来自 PRD §5.5.3/§8.3 + muzhou 拍板语义；断言值（2/1/0 计数）可手算 ✅
8. **共享工具类**: 无新增（清单已列空理由） ✅
9. **核心类职责边界**: CallbackCredentialAdminService 5 依赖/≤300 行声明已列 ✅
10. **Worktree 触发条件**: 命中第 1 项（跨 4 模块：fep-web/fep-security-api/fep-processor/fep-converter）+ 多会话并发红线（v0.4 实测 3 别会话 worktree）；头部已声明路径/分支；Task 5 含 worktree remove 实测命令；**worktree 须基于新 baseline `52ce5fe7` 创建** ✅

---

## 评审与签字

- [x] santa Round 1（2026-06-12）: **REVISE** — B-1/B-2 BLOCKER + M-1/M-2/M-3 + m-1~4 + C-1~4，全部已在 v0.2 闭合（见修订记录）
- [x] santa Round 2（2026-06-12）: **REVISE** — B-1/B-2/M-2/M-3/m-1~4/C-1/C-2 闭合确认 ✅ + 撞车收缩/B7 披露/Task 1-3 实测无新问题；新出 M-NEW-1（F4 残余面 +22 处）/M-NEW-2（脚本路径）/m-NEW-3（文件数）/C-NEW-1（噪声豁免），全部已在 v0.3 闭合（见修订记录；扩批口径 muzhou 2026-06-12 二次拍板）
- [x] santa Round 3（2026-06-12）: **REVISE** — v0.4 增量 8 项核验全 ✅（B4 33 处与新 baseline 双 grep bijective 吻合 / 14 路径 ls 全过 / S5 实装措辞事实成立 / b90429f6 object-store 取证 B5 注成立）；新出 M-NEW-3（package-info.java:7 验证盲区残留）/ m-NEW-4（"4 个"stale ×2）/ c-NEW-1，全部已在 v0.5 闭合（见修订记录）
- [x] santa Round 4 spot-check（2026-06-12）: **REVISE → 即闭** — 5 项核销 4 ✅（#35 锚点逐字吻合 / 加宽 pattern dry-run 9 行 bijective / 15 路径 ls 全过 / m-NEW-4+c-NEW-1 已改）；唯一 m-R4-1（L121 专项枚举漏 #35）已即时修复为 v0.5.1（本行上方"统一措辞版式"段）；评审员判定修后无需 Round 5 直接进签字
- [x] **muzhou 批准签字 ✅ APPROVED（2026-06-12，AskUserQuestion 拍板"批准签字，开始实施"）** — v0.5.1，baseline `52ce5fe7`，含三项前置拍板：① sweep=Admin 端点形态 ② F4 全量扩批（两轮：主锚点措辞 + 残余面 34 处）③ REUSE-S1 继续 Deferred；撞车收缩（S5 T7）经 muzhou "收缩重排 v0.2" 拍板
