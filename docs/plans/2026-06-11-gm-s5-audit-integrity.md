# FEP 国密 S5 实施计划 — 审计日志完整性（SM3 hash 链 + SM2 行签名）

> **版本:** v0.3（起草 2026-06-12；Round 1 双 REVISE → v0.2 全闭合 → Round 2 双 PASS WITH CONCERNS（Round 1 全 11 项实测闭合 + claim 命令 dry-run 双 true）→ v0.3 吸收 2C+6N 行级修订收口。两评审员均判定无需 Round 3 全量重审。待 muzhou 签字）
>
> **v0.3 修订记录（Round 2 闭合）：**
> - **C-NEW-1（双路同源·dev 域）**: mock `getAuditVerifyPublicKeyHex` 三桩钉死**合法 130-hex**（复用 GBT 公钥 hex 字面——parseHex 先于 MockSignService 执行，非 hex 串致 dev /integrity 恒报 UNKNOWN_KEY 假断点）+ MockKeyServiceTest 增 parseHex 可解析断言 + T6 增 mock 全 context 非空链 intact 正向用例
> - **crypto C-NEW-2（misconfig 披露）**: 抉择 ⑪ 新增——provider=impl 且 audit 段漏配 → append ISE 被切面 WARN 吞 = 审计行静默丢失；fail-fast 不可行（破 S1/S2a IT 零修改），采纳**披露 + writer 启动期探测 WARN**（recoverChainTail 末尾 try auditKeyId catch ISE → log.warn，~5 LOC）+ prod 部署注记"provider=impl 必须同时配置 audit 段"
> - **santa C-NEW-2（测试可执行性）**: AuditChainWriterTest 注记扩展——@DataJpaTest 回滚域 vs REQUIRES_NEW 交互：seed/断言用 `@Transactional(propagation = NOT_SUPPORTED)` 或 JdbcTemplate 自动提交（镜像红线 provider_impl_full_context_test ②③）；全部 append 用例真提交不回滚，逐用例以 recoverChainTail + 相对链尾断言隔离
> - **N**: 修订记录 B-2 数据点 36→37 一致化 / 自检 #9 同步（依赖 4 / ~190 行）/ T5 测试直构句补 4 依赖（@Autowired PlatformTransactionManager + new SimpleMeterRegistry()）/ `AuditIntegrityService.verifyEntry` Javadoc +@throws ISE（audit 段未配置→端点 500=配置错误信号，与 signEntryHash 对称）/ T6 AC1 标签口径对齐（链首 prevHash≠GENESIS 由 PREV_LINK 捕获，GAP 仅 seq 不连续）/ T6 增 intact 正向用例（与 C-NEW-1 用例合一）
>
> **v0.2 修订记录（Round 1 闭合）：**
> - **B-1（双路同源·安全洞）**: 删除 verifyChain 的 `MOCK_SIGNATURE` 值判定跳过（prod 绕过面：攻击者重算无密钥 SM3 链 + signature 列改写 MOCK 串即过检）→ **验签恒执行**（mock 域 MockSignService.verify 恒 true 语义不变；impl 域 MOCK 串非法 Base64 → false）；breakType 增 `UNKNOWN_KEY`（unknown sign_key_id catch IAE 计为断点而非整体异常）；披露 dev→prod 同库晋升时 mock 历史行诚实告警（不设豁免态——任何豁免重新打开洞）
> - **B-2**: `OperationLogAspectTest`（直构 aspect，构造器换依赖编译破坏点）补列 T5 + 文件表 + 数据点 35→37（v0.3 N 一致化：+ChainVerifyResult）
> - **B-3**: 端点路径修正 `/api/v1/sys/logs/integrity`（实测 Controller @RequestMapping("/api/v1/sys/logs")）
> - **C-1（boil-lake）**: append 落库改 `TransactionTemplate(REQUIRES_NEW)`（消除"外层 @Transactional rollback 后链尾已推进"假阳隐患，事务边界显式化）
> - **C-2（boil-lake）**: catch `DataIntegrityViolationException` → `recoverChainTail()` 自愈 + rethrow（poison-state 不再静默持续）
> - **C-3**: T6 补 AuditChainVerifier/ArchUnit 完整实现代码 + breakType 增 `GAP`（seq 不连续/链首非 1）+ repository 分页方法改 `findBySeqIsNotNullOrderBySeqAsc(Pageable)`
> - **C-4**: prod yml audit 段样板自带"单写者/单实例部署前提"第一手注记
> - **C-5**: claim 清单仲裁命令落盘完整可复跑版
> - **crypto C-1（截断攻击披露 + 轻量外锚）**: 抉择 ⑩ 新增——纯删尾攻击残余风险显式披露；链尾 seq 经 Micrometer gauge `fep_audit_chain_tail_seq` 外锚（监控侧可发现回退），恢复/推进 INFO 日志（仅 long seq，无 taint 字段）
> - **NIT**: T1 fixture 单一违反拆分（N-3）/ SignService Javadoc 注明 verify 异常面不对称（crypto N-1）/ canonical "先截断后入链"不变量声明（crypto N-2）/ roadmap "V37+" 陈旧口径 T8 顺手修正（N-1）/ 并发测试残留行注记（N-2）/ 私钥 JVM 驻留接受风险归 T8 安全文档（crypto N-3）/ 矩阵"全量=@OperationLog 注解面"口径（N-4）
>
> **执行方式:** hybrid（红线 `feedback_harness_bg_detach_hybrid_default`）— 主对话实施 edits + 前台 mvn（redirect-to-file + 显式 JAVA_HOME，见 CLAUDE.md 快速命令）+ commit；subagent 仅评审。步骤使用 `- [ ]` 复选框跟踪。

**目标:** 实装 roadmap Phase B S5：操作审计日志防篡改——① `SignServiceImpl`（SM3withSM2 裸签原语，进程内 BC，§0.3 豁免域）② 审计专用 SM2 密钥段 ③ `AuditIntegrityService`（SM3 hash 链 + 每行 SM2 签名原语）④ `t_sys_operation_log` V36 扩列（hash/prev_hash/seq/signature/sign_key_id/trace_id）⑤ 串行化链式写入 + 启动恢复 ⑥ 篡改检测端点 + TraceId 查询 + append-only 约束。**muzhou 2026-06-11 拍板「一并」**（hash 链 + SM2 行签名同期，AskUserQuestion 决策）。

**前置依赖:** S0 ✅ + S1 ✅（provider 开关/密钥范式）+ S2a ✅（HashService SM3 基座 + FepSecuritySm2Properties + Sm2LoginCipher 曲线原语）。baseline origin/main = `46bdb279`（2026-06-12 起草实测；评审/签字/实施各轮重测）。

**执行 Worktree:** `/Users/muzhou/FEP_v1.0_wt-gm-s5`（分支 `feat/gm-s5-audit-integrity`，触发条件第 ①⑤⑥ 项：跨 4 模块[api/impl/mock/web] / 全 reactor verify >5min / 多会话并发活跃实测 `.e2e`+`wt-simplify-q-drain` 在场）
> worktree 建立后第一步：签字 Plan 复制入 `docs/plans/` 并 git add 提交（S1/S2a/S4 范式）

**架构:** SM3withSM2 签名原语收敛 `fep-security-impl`（ArchUnit R1）经 `SignService` 既有接口暴露（provider=impl 门控，不动报文签验 wiring——`getSignPrivateKey`/`OutboundSignAdapter` 仍 S2b/§0.3）。审计链组件 `AuditIntegrityService`（api SPI + impl）封装 hash 链与行签名原语；`fep-web` 侧 `AuditChainWriter` 单写者串行化（单实例部署假设实测：application.yml L54-56 注释明示）。dev/mock 域签名为 `MOCK_SIGNATURE`（镜像登录 mock 哲学），hash 链 SM3 真算法 always-on。

**技术栈:** Java 17 / Spring Boot 3.x / BC bcprov-jdk18on 1.84（既有）/ Hibernate @Immutable / Flyway V36

**🔓 国密授权声明:** 2026-06-07 muzhou 授权。评审 = santa + **密码学专项**（GB/T 32918.2 签名流程/ZA/向量逐字节）+ muzhou 签字。repo 内密钥字面值 = GB/T 32918.5-2017 附录 A 公开标准测试向量；生产审计密钥 env 注入永不入 repo。**§0.3 边界**：本 Plan 实装的是 SM2 **签名算法原语**（审计行签名 = 进程内用途，roadmap §3.1 明示豁免）；SM2 **报文**签验的落地形态（外部签名验签服务器 1818 vs 进程内）仍待 §0.3 定调，`OutboundSignAdapter`/`getSignPrivateKey` 零触碰。

**AI 协同模式:**

| 模式 | AI 占比 | 本计划中的应用 |
|:----:|:-------:|--------------|
| A | 90% | T7 Simplify drain / T8 文档收尾 |
| B | 70% | T1-T6 国密实现 + 审计链 + 测试（🔓 解禁域，密码学专项 review 强制） |
| E | 0%  | （无 ⛔ Task；报文签验 wiring 不在范围） |

---

## 设计背景（实测依据，2026-06-12 grep @ `46bdb279`）

### PRD / 架构 / roadmap 依据

| 来源 | 内容 |
|------|------|
| PRD v1.3 §8.3（L2196） | "操作审计日志全覆盖"；（L2192）"国密 SM2/SM3/SM4 算法覆盖报文签名、加密、哈希" |
| 架构 §1218-1224（审计日志节实测） | "所有交换操作全量记录 / 管理操作全量记录 / **日志不可篡改 (append-only)** / 保留 ≥ 5 年 / 支持按 TraceId 查询完整链路" |
| PRD v1.3 §3.3.1（L613-619） | SM3withSM2 **裸签**（Raw Signature）、Base64 编码——审计行签名沿用同形态（与 SignService 既有契约一致） |
| roadmap S5（L152-158） | AuditIntegrityService：SM3 hash 链（prev_hash）+ SM2 签名每行；t_sys_operation_log 加列 hash/prev_hash/signature/seq；OperationLogAspect 集成；篡改检测；append-only + ≥5 年 + TraceId 查询 |
| roadmap §3.1 Phase B（L186） | "防篡改用 SM3；SM2 审计签名为进程内非 §0.3 报文签验" + **muzhou 2026-06-11 AskUserQuestion 拍板「一并」** |

**追溯 ID:**（session-end 写入矩阵）
- `FR-INFRA-GM-AUDIT-INTEGRITY`（新增）— 审计日志完整性：hash 链 + 行签名 + 篡改检测 + append-only + TraceId 查询
- 其组成 `SignServiceImpl`（SM3withSM2 原语）同时为 S2b 形态 B 备好算法件——但**报文签验 wiring 不在本 Plan 范围**（理由：§0.3 决策门未定调；`getSignPrivateKey` 维持 UnsupportedOperationException；OutboundSignAdapter 零触碰）

**不在范围**：报文签验 wiring（S2b）/ prod cutover（R7 仍 gated S2b）/ 密钥轮换工作流（S3）/ 审计日志归档作业（≥5 年保留 = append-only 无删除面 + 文档化，无 TTL 代码）/ 历史行回填（V36 前存量行 hash 列 NULL = 链外，genesis 从部署后首行起）。

### 现状基线（关键 API 逐字实测 @ `46bdb279`）

| 资产 | 路径:行号 | 现状 |
|------|----------|------|
| `t_sys_operation_log` | `fep-web/.../db/migration/V4__create_p6a2_tables.sql` L67-86 | 13 列，PK=log_id VARCHAR(32) UUID，**TIMESTAMP 无小数位**；索引 user_account/module/create_time；**无 trace_id 列** |
| `SysOperationLog` entity | `fep-web/.../sysmgmt/log/domain/SysOperationLog.java` | 13 字段；`createTime updatable=false`；其余列无 updatable 限制；无 @Immutable |
| `OperationLogAspect` | `fep-web/.../sysmgmt/log/aspect/OperationLogAspect.java` L67-120 | @Around("@annotation(annotation)")；saveLog **同步直写** `operationLogRepository.save(entity)`（L116）；失败仅 WARN；无 @Transactional；唯一写入点（手工调用 grep 0） |
| 修改面 | Controller L60-95 / Service / Repository | 仅 GET search+findById；repository 仅 search 自定义查询；**无 update/delete 端点**；无 @SQLDelete/@Immutable |
| `TraceIdFilter` | `fep-common/.../trace/TraceIdFilter.java` L32-65 | MDC_KEY="traceId"；格式 `yyyyMMddHHmmss-NNNNNN`；X-Trace-Id header 复用；**无 SkyWalking** |
| Flyway max | `fep-web/.../db/migration/` | **V35**（V35__callback_credential_expires_at.sql）→ 本 Plan 用 **V36**（签字+实施前重 grep，红线 plan_flyway_v_collision_check） |
| `SignService` | `fep-security-api/.../SignService.java` L13-35 | `String sign(byte[] data, byte[] privateKey)` → Base64；`boolean verify(byte[] data, String signature, byte[] publicKey)`；impl **不存在**（mock 固定 "MOCK_SIGNATURE"/true） |
| `HashService` | S2a ✅ | `sm3Hex(byte[])` always-on（GmHashConfiguration） |
| `FepSecuritySm2Properties` | S2a ✅ `fep-security-impl/.../key/` | prefix `fep.security.sm2`：loginActiveKeyId + loginKeys{privateKeyHex,publicKeyHex}；[d]G 校验在 KeyServiceImpl |
| `Sm2LoginCipher.DOMAIN` | S2a ✅（包私有 static） | sm2p256v1 ECDomainParameters——T1 SignServiceImpl 在 impl.sign 包**不可直接引用**（包私有）→ T1 自建同源 DOMAIN（GMNamedCurves，2 行）并注记与 Sm2LoginCipher 同源（Simplify 候选见共享工具表注） |
| 部署形态 | `fep-web/.../application.yml` L54-56 | 注释明示"单机 FEP 部署足以覆盖" → 单写者串行化成立 |
| `KeyService` 实现点 | grep `implements KeyService\|new KeyService()` | **4 处**：KeyServiceImpl / MockKeyService / TestKeyServiceConfiguration 匿名 / CallbackCredentialEncryptionFacadeTest 匿名（接口扩展须 4 点同 commit——S2a B-2 教训） |

### GB/T 标准测试向量（实现级仲裁，2026-06-12 本机实测）

**SM2 签名（GB/T 32918.5-2017 附录 A 推荐曲线签名示例；仲裁 = 本机 sm-crypto@0.3.13 `doVerifySignature(msg, rs, pub, {hash:true, der:false})` → `true`，2026-06-12 实测）**

| 项 | 值 |
|----|----|
| 密钥对 | 同 S2a：dA `3945208f7b2144b13f36e38ac6d39f95889393692860b51a42fb81ef4df7c5b8` / 公钥 `0409f9df…2da9ad13`（Sm2TestVectors 既有常量） |
| 明文 M | `"message digest"`（14 字节 ASCII） |
| ZA 用户标识 | 默认 ID `1234567812345678`（GB/T 32918.2；BC `SM2Signer` 默认 = sm-crypto `hash:true` 默认，一致） |
| 签名 r∥s（raw 128 hex） | `f5a03b0648d2c4630eeac513e1bb81a15944da3827d5b74143ac7eaceee720b3` ∥ `b1b6aa29df212fd8763182bc0d421ca1bb9038fd1f7f42d4840b69c485bbc1aa` |

> 签名含随机 k → 正向签名无字节级 KAT；**验签 KAT**（标准 r∥s 验签 GREEN）+ BC roundtrip + sm-crypto 跨实现 fixture 三方互证（S2a 同构）。

**sm-crypto 跨实现签名 fixture（2026-06-12 本机生成：`doSignature(msg, dA, {hash:true, der:false})`，self-verify ✓）**

- 明文: `"audit-hash-0123456789abcdef"`（28 字节）
- 签名 r∥s（128 hex，冻结）:
```
4eb778ecc265d4ccf7f27e0e1db0e63ab03c4c50496613b7517527acb36a049e29963e2a443dee34a589e0968fa1ca0b918751c40a8a9e5c414541a31d5d1ee1
```

**SM3**：复用 S2a 已仲裁向量（HashServiceImplTest 既有，本 Plan 不重复）。

### 设计抉择（评审重点）

| # | 抉择 | 理由 |
|---|------|------|
| ① | **SignServiceImpl 实装**（SM3withSM2，BC `SM2Signer` + `PlainDSAEncoding`（raw r∥s 64B）+ 默认 ID ZA，Base64 输出），provider=impl 门控 @Bean；私钥/公钥参数形态 = 接口既有契约（`byte[]`）——审计调用方传 **32 字节标量 d 原始字节**（私钥）/ **65 字节裸点**（公钥），Javadoc 修正既有 "PKCS#8/X.509" 描述（与 S2a 抉择②同理：BC lightweight 直用，缩 ASN.1 面；S2b 报文签验若选形态 A 外部服务器则该 impl 不被报文链使用，零冲突） | muzhou 拍板一并；裸签 Base64 与 PRD §3.3.1 报文签名形态同构，S2b 形态 B 可直接复用 |
| ② | 审计密钥独立命名空间：`FepSecuritySm2Properties` 增 `auditActiveKeyId` + `Map<String,LoginKeyPair> auditKeys`（**复用 LoginKeyPair 结构**），KeyServiceImpl 校验逻辑抽 `validateSm2KeySection(...)` 复用（login/audit 两段对称——红线 mapper_helper_trim_consistency 精神） | 审计签名密钥与登录解密密钥用途隔离独立轮换；结构复用避免第二套 POJO |
| ③ | `KeyService` +2 方法：`byte[] getAuditSignPrivateKey()`（活跃版本标量 d 字节，防御副本）+ `String getAuditVerifyPublicKeyHex(String keyId)`（按版本取裸点 hex，供历史行验签）；+`String getAuditKeyId()`（活跃版本号，落 sign_key_id 列）——**3 方法 ×4 实现点同 commit**（S2a B-2 教训，mock/test 桩返回 mock 常量） | 多版本验签必须按行记录的 keyId 路由 |
| ④ | canonical 串 = netstring 风格 `<len>:<utf8value>`（null → `-1:`）按固定 15 字段序拼接：`seq, logId, userId, userAccount, module, operation.name(), description, method, requestUrl, requestParams, responseStatus, ipAddress, durationMs, createTimeIso, traceId`；**createTime 截断到秒**（`truncatedTo(SECONDS)` + `ISO_LOCAL_DATE_TIME`）——V4 列 TIMESTAMP 无小数位，DB 往返截断会致重算 hash 假阳断链（关键陷阱，写入与校验两侧同规则） | 确定性 + 无分隔符注入歧义 + DB 往返稳定 |
| ⑤ | hash = `HashService.sm3Hex(prevHashHex_utf8 ∥ canonical_utf8)`；GENESIS prevHash = 64×'0'；signature = `SignService.sign(hashHex_utf8, auditPrivateKey)`（签 hash 而非原文——hash 已绑定全部内容+链位置）；mock provider 下 signature = "MOCK_SIGNATURE"（dev 哲学同登录，IT 在 provider=impl 验真签名） | 链式绑定 + 签名输入最小化 |
| ⑥ | 串行化：`AuditChainWriter`（fep-web）`synchronized` 临界区 + 内存链尾（lastSeq/lastHash）+ `@PostConstruct` 从 `findTopBySeqIsNotNullOrderBySeqDesc` 恢复 + **save 成功才推进链尾**（失败链尾不动 → 无空洞断链；切面"失败仅 WARN 不影响主业务"语义保持）+ DB `UNIQUE KEY uk_audit_seq(seq)` 兜底（链分叉即唯一键冲突暴露） | 单实例实测假设成立；多实例部署是 roadmap 外未来项（文档注记） |
| ⑦ | append-only：entity 加 Hibernate `@Immutable` + 新增列全部 `updatable=false` + ArchUnit 规则（fep-web 生产代码禁调用 `SysOperationLogRepository` 的 delete*/saveAll 之外修改方法——白名单 save/search/find*）+ 既有无 update/delete 端点维持 | DB 触发器跨 H2/MySQL/PG/达梦/金仓（PRD §8.4）不可移植，应用层+评审约束务实 |
| ⑧ | V36 前存量行：6 新列全 nullable，存量行 NULL = 链外（不回填——历史完整性无法事后背书）；篡改检测从链上首行（min seq）起 | 诚实披露而非伪造历史背书 |
| ⑨ | 篡改检测：`AuditChainVerifier`（fep-web）分页批读（500 行/批防 OOM）重算 hash 链 + 按 sign_key_id **恒验签**（v0.2 B-1：无任何按值跳过——mock 域 MockSignService.verify 恒 true 自然通过；impl 域 MOCK 占位串非法 Base64 → false；unknown keyId catch IAE → UNKNOWN_KEY 断点）；管理端点 `GET /api/v1/sys/logs/integrity`（复用既有 Controller `@RequestMapping("/api/v1/sys/logs")` 实测，@OperationLog 注解自身入链）返回 {totalChecked, intact, firstBreakSeq, breakType(GAP/PREV_LINK/HASH_MISMATCH/SIGNATURE_INVALID/UNKNOWN_KEY)}；**dev→prod 同库晋升披露**：mock 历史行在 impl 域报 UNKNOWN_KEY = 诚实告警非误报（与抉择⑧同哲学，不设豁免态——任何豁免重新打开绕过面） | §1219 篡改可检；签名是唯一绑定私钥的防伪要素，验签环节不可有值判定旁路 |
| ⑪ | **misconfig 披露（v0.3 crypto C-NEW-2）**：provider=impl 且 audit 段漏配 → `append` 内 `getAuditSignPrivateKey` ISE → 切面既有 catch WARN 吞 → 审计行静默丢失（劣于 S5 前"无签名但落库"）。fail-fast 不可行（KeyServiceImpl 层强制 audit 段会破 S1/S2a provider=impl IT 零修改）→ 采纳：writer 启动期探测 WARN（recoverChainTail 末尾）+ prod 部署注记"provider=impl 必须同时配置 audit 段" | 审计可用性 fail-silent 面显式化，部署期可发现 |
| ⑩ | **截断攻击残余风险（披露 + 轻量外锚）**：DB 级攻击者纯删尾段（seq k+1..n）后剩余链自洽——hash 链结构性盲区（中间删行可检=PREV_LINK；纯删尾不可）。缓解：链尾 seq 经 Micrometer gauge `fep_audit_chain_tail_seq` 外锚（Prometheus 侧 seq 回退即告警线索）+ 恢复/推进 INFO 日志（仅 long seq，无 taint 面）；完全消除需周期性链尾外部 checkpoint，归 S6 密评阶段评估 | 行业通行残余，显式披露优于伪装完备 |

### 共享工具类清单

| 工具类 | 包 | 关键方法 | 提供者 Task | 消费者 Task |
|---|---|---|---|---|
| `Sm2SignSupport`（DOMAIN 常量） | `security.impl.sign`（包私有） | `DOMAIN`（GMNamedCurves sm2p256v1） | T1 | T1（与 S2a `Sm2LoginCipher.DOMAIN` 同源重复 2 行——跨包不可见，列 Simplify 候选注记，本期不合并） |
| `LoginKeyPair`（复用） | `security.impl.key`（S2a 既有） | privateKeyHex/publicKeyHex | — | T2（audit 段复用） |
| `AuditCanonicalizer` | `web.sysmgmt.log.audit` | `canonicalize(SysOperationLog, long seq)` | T5 | T5（写）/ T6（校验重算，同一实现保证两侧一致） |
| `Sm2TestVectors`（扩展） | `security.impl.key`（test，S2a 既有） | +签名向量常量 | T1 | T1/T2 测试 |

### AuditChainWriter 职责边界

**负责**: seq 分配、prevHash 链接、hash/签名计算编排、串行化落库、启动链尾恢复
**不负责**: canonical 规则（→AuditCanonicalizer）/ SM3/SM2 原语（→AuditIntegrityService）/ 日志字段采集（→OperationLogAspect）/ 篡改检测（→AuditChainVerifier）
**依赖**: 4（SysOperationLogRepository / AuditIntegrityService / PlatformTransactionManager / MeterRegistry——上限 7 ✓；v0.2 C-1/⑩ 增 2）
**行数**: 预计 ~190（上限 300 ✓）

### 文件结构

| 文件路径 | 职责 | 操作 | AI 模式 |
|----------|------|------|:-------:|
| `fep-security-impl/.../impl/sign/SignServiceImpl.java` | SM3withSM2 裸签/验签（无 stereotype） | 新建 | B |
| `fep-security-impl/.../impl/sign/Sm2SignSupport.java` | DOMAIN 曲线常量（包私有） | 新建 | B |
| `fep-security-impl/.../impl/GmSecurityConfiguration.java` | +@Bean signService | 修改 | B |
| `fep-security-api/.../api/SignService.java` | Javadoc 修正（裸签 r∥s/密钥字节形态/ZA 默认 ID） | 修改 | B |
| `fep-security-api/.../api/KeyService.java` | +getAuditSignPrivateKey/getAuditVerifyPublicKeyHex/getAuditKeyId | 修改 | B |
| `fep-security-api/.../api/AuditIntegrityService.java` | 审计链原语 SPI | 新建 | B |
| `fep-security-impl/.../impl/key/FepSecuritySm2Properties.java` | +audit 段（auditActiveKeyId/auditKeys） | 修改 | B |
| `fep-security-impl/.../impl/key/KeyServiceImpl.java` | +audit 密钥加载/校验抽取复用/3 方法 | 修改 | B |
| `fep-security-impl/.../impl/audit/AuditIntegrityServiceImpl.java` | hash 链 + 行签名原语实现（无 stereotype） | 新建 | B |
| `fep-security-impl/.../impl/GmAuditConfiguration.java` | AuditIntegrityService @Bean（always-on，签名经注入 SignService bean——mock/impl 自适应） | 新建 | B |
| `fep-security-mock/.../mock/MockKeyService.java` | +3 audit 方法 mock | 修改 | A |
| `fep-web/src/test/.../config/TestKeyServiceConfiguration.java` | 匿名 +3 方法 | 修改 | A |
| `fep-web/src/test/.../crypto/CallbackCredentialEncryptionFacadeTest.java` | 匿名 +3 方法 | 修改 | A |
| `fep-web/.../db/migration/V36__operation_log_integrity.sql` | 6 新列 + unique(seq) + trace_id 索引 | 新建 | B |
| `fep-web/.../sysmgmt/log/domain/SysOperationLog.java` | +6 字段 + @Immutable | 修改 | B |
| `fep-web/.../sysmgmt/log/repository/SysOperationLogRepository.java` | +findTopBySeqIsNotNullOrderBySeqDesc + 分页链读 + trace_id search | 修改 | B |
| `fep-web/.../sysmgmt/log/audit/AuditCanonicalizer.java` | canonical 串（netstring） | 新建 | B |
| `fep-web/.../sysmgmt/log/audit/AuditChainWriter.java` | 串行化链式写入 + 启动恢复 | 新建 | B |
| `fep-web/.../sysmgmt/log/audit/AuditChainVerifier.java` | 篡改检测（分页重算+恒验签+5 断点类型） | 新建 | B |
| `fep-web/.../sysmgmt/log/audit/ChainVerifyResult.java` | 校验结果 record | 新建 | B |
| `fep-web/.../sysmgmt/log/aspect/OperationLogAspect.java` | save → auditChainWriter.append | 修改 | B |
| `fep-web/.../sysmgmt/log/controller/SysOperationLogController.java` | +integrity 端点 + traceId 查询参数 | 修改 | B |
| `fep-web/.../sysmgmt/log/service/SysOperationLogService.java` | +traceId 透传 | 修改 | B |
| `fep-web/src/test/.../architecture/OperationLogAppendOnlyArchTest.java` | append-only ArchUnit | 新建 | B |
| 新建测试 6：`SignServiceImplTest` / `AuditIntegrityServiceImplTest` / `SysOperationLogIntegrityColumnsTest` / `AuditCanonicalizerTest` / `AuditChainWriterTest` / `AuditChainVerifierTest` | — | 新建 | B |
| 修改测试 3：`KeyServiceImplTest`（T2 增例 + T7 F2/F7）/ `MockKeyServiceTest`（T2 增例）/ `Sm2TestVectors`（T1 签名向量 + 可见性 public + T7 F5a） | — | 修改 | B |
| root `pom.xml`（JaCoCo +GmAuditConfiguration）/ `application-prod.yml`（audit 段样板） | T3 / T8 | 修改 | A |
| `fep-web/src/test/.../aspect/OperationLogAspectTest.java` | 直构 aspect mock 换 AuditChainWriter（v0.2 B-2） | 修改 | A |
| （T7 drain 新增触碰）`Sm2LoginDecryptionProviderImplTest`（F1；其余 F2-F7/F4 触碰文件已列于上表） | — | 修改 | A |

> LOC 预估 ~1300（生产 ~700 + 测试 ~600）>400 → PR-Size 走 muzhou `--admin --squash` 豁免先例（S1/S4/S2a）。

---

## Task 0: Worktree + Plan 入库 `模式 A`

- [ ] **Step 1: 4 时点重测**：`cd /Users/muzhou/FEP_v1.0 && git fetch && git rev-parse main origin/main && git worktree list && (pgrep -fl MavenWrapperMain || echo no-mvn)`；**重 grep Flyway max**：`ls fep-web/src/main/resources/db/migration/ | sort -V | tail -3`（期望 max=V35；若别会话已占 V36 → 全 Plan V36→V37 顺延并重评审该 Task）
- [ ] **Step 2**: `git worktree add -b feat/gm-s5-audit-integrity /Users/muzhou/FEP_v1.0_wt-gm-s5 origin/main`
- [ ] **Step 3**: 复制本 Plan 入 `docs/plans/` + `git add` + commit（`docs(plans): add signed GM S5 plan` + AI-Generated + Reviewed-By: muzhou）

---

## Task 1: SignServiceImpl — SM3withSM2 裸签原语 `模式 B`

**PRD 依据:** v1.3 §3.3.1（SM3withSM2 裸签 Base64）+ §8.3（国密算法覆盖签名）
**追溯 ID:** FR-INFRA-GM-AUDIT-INTEGRITY（组成件）

**验收标准:**
1. **GB/T 32918.5 验签 KAT**: `verify("message digest".bytes, Base64(标准 r∥s), 标准公钥 65B)` == true（标准向量经 sm-crypto 独立仲裁 2026-06-12）
2. **sm-crypto 跨实现**: `verify("audit-hash-0123456789abcdef".bytes, Base64(fixture r∥s), 公钥)` == true（前端库产签 → BC 验）
3. **roundtrip**: `sign` 任意 UTF-8 数据 → `verify` true；签名 Base64 解码恒 64 字节（raw r∥s 非 DER）
4. 数据/签名任一字节篡改 → verify false（不抛异常）
5. null 参数 → IllegalArgumentException；私钥非 32 字节 / 公钥非 65 字节（04 头）→ IllegalArgumentException
6. ZA 用户标识 = BC 默认 ID `1234567812345678`（与 sm-crypto hash:true 默认一致——KAT 通过即锚定）

**Files:** Create `fep-security-impl/.../impl/sign/SignServiceImpl.java` + `.../impl/sign/Sm2SignSupport.java`；Modify `GmSecurityConfiguration.java` / `SignService.java`（Javadoc）；Create test `SignServiceImplTest.java`；Modify test `Sm2TestVectors.java`（+签名向量）

- [ ] **Step 1: 失败测试**

```java
// fep-security-impl/src/test/java/com/puchain/fep/security/impl/sign/SignServiceImplTest.java
package com.puchain.fep.security.impl.sign;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.puchain.fep.security.impl.key.Sm2TestVectors;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

/**
 * SM3withSM2 裸签 GB/T 32918.5-2017 附录 A 验签 KAT + sm-crypto 跨实现互操作。
 */
class SignServiceImplTest {

    private final SignServiceImpl signService = new SignServiceImpl();

    private static byte[] priv() {
        return HexFormat.of().parseHex(Sm2TestVectors.GBT_PRIVATE_KEY_HEX);
    }

    private static byte[] pub() {
        return HexFormat.of().parseHex(Sm2TestVectors.GBT_PUBLIC_KEY_HEX);
    }

    @Test
    void verify_gbt32918AppendixASignatureVector_returnsTrue() {
        final String sigBase64 = Base64.getEncoder().encodeToString(
                HexFormat.of().parseHex(Sm2TestVectors.GBT_SIGN_RS_HEX));
        assertThat(signService.verify(
                Sm2TestVectors.GBT_SIGN_PLAINTEXT.getBytes(StandardCharsets.US_ASCII),
                sigBase64, pub())).isTrue();
    }

    @Test
    void verify_smCryptoProducedSignature_returnsTrue() {
        final String sigBase64 = Base64.getEncoder().encodeToString(
                HexFormat.of().parseHex(Sm2TestVectors.SM_CRYPTO_SIGN_FIXTURE_RS_HEX));
        assertThat(signService.verify(
                Sm2TestVectors.SM_CRYPTO_SIGN_FIXTURE_PLAINTEXT.getBytes(StandardCharsets.UTF_8),
                sigBase64, pub())).isTrue();
    }

    @Test
    void signThenVerify_roundtrip_andRawRs64Bytes() {
        final byte[] data = "审计链 hash ✓ 2026".getBytes(StandardCharsets.UTF_8);
        final String sig = signService.sign(data, priv());
        assertThat(Base64.getDecoder().decode(sig)).hasSize(64);
        assertThat(signService.verify(data, sig, pub())).isTrue();
    }

    @Test
    void verify_tamperedDataOrSignature_returnsFalse() {
        final byte[] data = "row-hash".getBytes(StandardCharsets.UTF_8);
        final String sig = signService.sign(data, priv());
        final byte[] tamperedData = "row-hasH".getBytes(StandardCharsets.UTF_8);
        assertThat(signService.verify(tamperedData, sig, pub())).isFalse();
        final byte[] sigBytes = Base64.getDecoder().decode(sig);
        sigBytes[0] ^= 0x01;
        assertThat(signService.verify(data,
                Base64.getEncoder().encodeToString(sigBytes), pub())).isFalse();
    }

    @Test
    void malformedKeysOrNulls_throwIllegalArgument() {
        final byte[] data = "x".getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> signService.sign(null, priv()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> signService.sign(data, new byte[16]))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> signService.verify(data, "AAAA", new byte[64]))
                .isInstanceOf(IllegalArgumentException.class); // 公钥长度违反（签名格式合法 Base64）
        assertThatThrownBy(() -> signService.verify(data, null, pub()))
                .isInstanceOf(IllegalArgumentException.class);
        // 单一约束：公钥合法 + 签名非 Base64 → false（值域错非参数错，不抛）
        assertThat(signService.verify(data, "not-base64_!", pub())).isFalse();
    }
}
```

`Sm2TestVectors` 追加常量（test util，S2a 既有文件）：

```java
    /** GB/T 32918.5-2017 附录 A 签名示例明文。 */
    static final String GBT_SIGN_PLAINTEXT = "message digest";

    /** GB/T 32918.5-2017 附录 A 签名 r∥s（raw 128 hex；2026-06-12 sm-crypto 独立验签仲裁 GREEN）。 */
    static final String GBT_SIGN_RS_HEX =
            "f5a03b0648d2c4630eeac513e1bb81a15944da3827d5b74143ac7eaceee720b3"
                    + "b1b6aa29df212fd8763182bc0d421ca1bb9038fd1f7f42d4840b69c485bbc1aa";

    /** sm-crypto@0.3.13 doSignature({hash:true,der:false}) 实测签名 fixture 明文。 */
    static final String SM_CRYPTO_SIGN_FIXTURE_PLAINTEXT = "audit-hash-0123456789abcdef";

    /** sm-crypto 签名 fixture r∥s（128 hex，2026-06-12 生成冻结，self-verify ✓）。 */
    static final String SM_CRYPTO_SIGN_FIXTURE_RS_HEX =
            "4eb778ecc265d4ccf7f27e0e1db0e63ab03c4c50496613b7517527acb36a049e"
                    + "29963e2a443dee34a589e0968fa1ca0b918751c40a8a9e5c414541a31d5d1ee1";
```
> 注：`Sm2TestVectors` 为包私有（`security.impl.key`），`SignServiceImplTest` 在 `security.impl.sign` 包——**将 Sm2TestVectors 改为 `public final class` + 常量 `public static final`**（test util 可见性放宽，同 commit；既有引用不破）。

- [ ] **Step 2: RED**

```bash
export JAVA_HOME=/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home; export PATH="$JAVA_HOME/bin:$PATH"
cd /Users/muzhou/FEP_v1.0_wt-gm-s5 && ./mvnw test -pl fep-security-impl -am -Dtest=SignServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false --batch-mode --no-transfer-progress > /tmp/gm-s5-t1-red.log 2>&1; echo "EXIT=$?"; tail -15 /tmp/gm-s5-t1-red.log
```
期望: 编译失败 `找不到符号: SignServiceImpl`

- [ ] **Step 3: 实现**

```java
// fep-security-impl/src/main/java/com/puchain/fep/security/impl/sign/Sm2SignSupport.java
package com.puchain.fep.security.impl.sign;

import org.bouncycastle.asn1.gm.GMNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.params.ECDomainParameters;

/**
 * SM2 签名域参数（sm2p256v1，GB/T 32918）。包私有；与 security.impl.key.Sm2LoginCipher.DOMAIN
 * 同源（跨包不可见故各持 2 行，Simplify 候选注记于 Plan §共享工具表）。
 *
 * @author FEP Team
 * @since 1.0.0
 */
final class Sm2SignSupport {

    /** SM2 推荐曲线参数。 */
    private static final X9ECParameters SM2_X9 = GMNamedCurves.getByName("sm2p256v1");

    /** SM2 椭圆曲线 domain 参数。 */
    static final ECDomainParameters DOMAIN = new ECDomainParameters(
            SM2_X9.getCurve(), SM2_X9.getG(), SM2_X9.getN(), SM2_X9.getH());

    private Sm2SignSupport() {
    }
}
```

```java
// fep-security-impl/src/main/java/com/puchain/fep/security/impl/sign/SignServiceImpl.java
package com.puchain.fep.security.impl.sign;

import com.puchain.fep.security.api.SignService;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Base64;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.signers.PlainDSAEncoding;
import org.bouncycastle.crypto.signers.SM2Signer;

/**
 * SM3withSM2 裸签实现（GB/T 32918.2；BC SM2Signer + PlainDSAEncoding = raw r∥s 64 字节）。
 *
 * <p>ZA 用户标识 = BC 默认 ID {@code 1234567812345678}（GB/T 32918 默认，与前端
 * sm-crypto {@code hash:true} 默认一致）。私钥 = 32 字节标量 d 原始字节；公钥 = 65 字节
 * 未压缩裸点 04∥x∥y。无 Spring stereotype，经 GmSecurityConfiguration @Bean 注册
 * （provider=impl 门控）。S5 服务审计行签名；S2b 报文签验 wiring 与落地形态另由 §0.3 定调。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class SignServiceImpl implements SignService {

    /** 私钥标量长度（字节）。 */
    private static final int PRIVATE_KEY_LENGTH = 32;

    /** 未压缩裸点公钥长度（字节）。 */
    private static final int PUBLIC_KEY_LENGTH = 65;

    @Override
    public String sign(final byte[] data, final byte[] privateKey) {
        requireData(data);
        if (privateKey == null || privateKey.length != PRIVATE_KEY_LENGTH) {
            throw new IllegalArgumentException(
                    "SM2 private key must be 32-byte raw scalar d");
        }
        final SM2Signer signer = new SM2Signer(PlainDSAEncoding.INSTANCE);
        signer.init(true, new ParametersWithRandom(new ECPrivateKeyParameters(
                new BigInteger(1, privateKey), Sm2SignSupport.DOMAIN), new SecureRandom()));
        signer.update(data, 0, data.length);
        try {
            return Base64.getEncoder().encodeToString(signer.generateSignature());
        } catch (final CryptoException e) {
            throw new IllegalStateException("SM2 signature generation failed", e);
        }
    }

    @Override
    public boolean verify(final byte[] data, final String signature, final byte[] publicKey) {
        requireData(data);
        if (signature == null) {
            throw new IllegalArgumentException("signature must not be null");
        }
        if (publicKey == null || publicKey.length != PUBLIC_KEY_LENGTH
                || publicKey[0] != 0x04) {
            throw new IllegalArgumentException(
                    "SM2 public key must be 65-byte uncompressed point (04||x||y)");
        }
        final byte[] rawSignature;
        try {
            rawSignature = Base64.getDecoder().decode(signature);
        } catch (final IllegalArgumentException e) {
            return false;
        }
        final SM2Signer signer = new SM2Signer(PlainDSAEncoding.INSTANCE);
        try {
            signer.init(false, new ECPublicKeyParameters(
                    Sm2SignSupport.DOMAIN.getCurve().decodePoint(publicKey),
                    Sm2SignSupport.DOMAIN));
        } catch (final IllegalArgumentException e) {
            return false;
        }
        signer.update(data, 0, data.length);
        return signer.verifySignature(rawSignature);
    }

    private static void requireData(final byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }
    }
}
```

`GmSecurityConfiguration` 追加（@ConditionalOnProperty(provider=impl) 既有类内）：

```java
    /**
     * SM3withSM2 裸签真实服务（S5 审计行签名；S2b 报文签验 wiring 待 §0.3）。
     *
     * @return SignService 实现
     */
    @Bean
    public SignService signService() {
        return new SignServiceImpl();
    }
```

`SignService`（api）Javadoc 修正：sign 的 `privateKey` 注明"32 字节标量 d 原始字节（impl provider；mock 忽略）"；verify 的 `publicKey` 注明"65 字节未压缩裸点 04∥x∥y"；类注明"裸签 raw r∥s 64 字节 Base64（PRD §3.3.1），ZA 默认 ID 1234567812345678"；**异常面不对称注明**（v0.2 crypto N-1）：verify 对密钥长度/前缀错抛 IAE（参数契约错），对合法格式但非曲线点/非法 Base64 签名静默 false（值域错）——调用方按此区分配置错误与验证失败。

- [ ] **Step 4: GREEN + 门禁**

```bash
export JAVA_HOME=/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home; export PATH="$JAVA_HOME/bin:$PATH"
cd /Users/muzhou/FEP_v1.0_wt-gm-s5 && ./mvnw test -pl fep-security-impl -am -Dtest='SignServiceImplTest,Sm2LoginCipherTest,KeyServiceImplTest' -Dsurefire.failIfNoSpecifiedTests=false --batch-mode --no-transfer-progress > /tmp/gm-s5-t1-green.log 2>&1; echo "EXIT=$?"; grep -E "Tests run|BUILD" /tmp/gm-s5-t1-green.log | tail -5
cd /Users/muzhou/FEP_v1.0_wt-gm-s5 && ./mvnw compile spotbugs:check -pl fep-security-api,fep-security-impl -am --batch-mode --no-transfer-progress > /tmp/gm-s5-t1-sb.log 2>&1; echo "SB=$?"; grep -cE "BugInstance size is 0" /tmp/gm-s5-t1-sb.log
```
> 注意：provider=impl 全 context 测试（S1 `CallbackLegacyCredentialMigrationTest` / S2a `Sm2LoginDecryptionProviderImplTest`）的 `@MockBean SignService` 在真 bean 出现后**仍有效**（@MockBean 覆盖语义），T8 全量回归确认零破坏。

- [ ] **Step 5: 提交** `feat(security): real SM3withSM2 raw signature SignServiceImpl (GM S5 T1)` + AI-Generated/Reviewed-By: pending
- [ ] **Step 6: spec+quality review subagent**

---

## Task 2: 审计密钥段 + KeyService 接口扩展 `模式 B`

**PRD 依据:** v1.3 §8.3 + §3.3.4（密钥形态）
**追溯 ID:** FR-INFRA-GM-AUDIT-INTEGRITY

**验收标准:**
1. 配置 `fep.security.sm2.audit-active-key-id` + `audit-keys.<id>.{private-key-hex,public-key-hex}` 后：`getAuditKeyId()` 返回活跃版本；`getAuditSignPrivateKey()` 返回 32 字节防御副本；`getAuditVerifyPublicKeyHex("<id>")` 返回 130 hex 裸点
2. audit 段校验与 login 段**完全对称**（字符集/1≤d≤n-1/裸点格式/[d]G 配对——抽取 `validateSm2KeySection` 复用，红线 mapper_helper_trim_consistency）
3. 未配置 audit 段 → 3 方法抛 IllegalStateException（含 "not configured"）；unknown keyId → IllegalArgumentException
4. login 段不配 audit 段配（及反之）→ 各自独立工作（互不耦合）
5. 接口扩展 **4 实现点同 commit** 编译绿（`./mvnw test-compile -pl fep-web -am`）；S1/S2a 既有 IT 零修改
6. mock 域：`getAuditKeyId()`="mock-key-v1"、`getAuditSignPrivateKey()`=32 字节 mock、`getAuditVerifyPublicKeyHex`=固定 mock hex（MockSignService 反正忽略密钥）

**Files:** Modify `KeyService.java` / `FepSecuritySm2Properties.java` / `KeyServiceImpl.java` / `MockKeyService.java` / `TestKeyServiceConfiguration.java` / `CallbackCredentialEncryptionFacadeTest.java` / `KeyServiceImplTest.java` / `MockKeyServiceTest.java`

- [ ] **Step 1: 失败测试**（`KeyServiceImplTest` 增，复用 S2a 助手模式）

```java
    private static KeyService newServiceWithAudit(final String keyId,
            final String privHex, final String pubHex) {
        final FepSecuritySm2Properties sm2 = new FepSecuritySm2Properties();
        sm2.setAuditActiveKeyId(keyId);
        final FepSecuritySm2Properties.LoginKeyPair pair = new FepSecuritySm2Properties.LoginKeyPair();
        pair.setPrivateKeyHex(privHex);
        pair.setPublicKeyHex(pubHex);
        sm2.getAuditKeys().put(keyId, pair);
        final FepSecurityKeyProperties sm4 = new FepSecurityKeyProperties();
        sm4.setActiveKeyId("sm4-cred-v2");
        sm4.getSm4Keys().put("sm4-cred-v2", GBT_KEY_HEX);
        final KeyServiceImpl svc = new KeyServiceImpl(sm4, sm2);
        svc.validateOnStartup();
        return svc;
    }

    @Test
    void auditKeys_valid_exposeKeyIdPrivateBytesAndPublicHex() {
        final KeyService svc = newServiceWithAudit("sm2-audit-v1",
                Sm2TestVectors.GBT_PRIVATE_KEY_HEX, Sm2TestVectors.GBT_PUBLIC_KEY_HEX);
        assertThat(svc.getAuditKeyId()).isEqualTo("sm2-audit-v1");
        assertThat(svc.getAuditSignPrivateKey()).hasSize(32);
        assertThat(svc.getAuditVerifyPublicKeyHex("sm2-audit-v1"))
                .isEqualTo(Sm2TestVectors.GBT_PUBLIC_KEY_HEX);
    }

    @Test
    void auditSignPrivateKey_returnsDefensiveCopy() {
        final KeyService svc = newServiceWithAudit("sm2-audit-v1",
                Sm2TestVectors.GBT_PRIVATE_KEY_HEX, Sm2TestVectors.GBT_PUBLIC_KEY_HEX);
        final byte[] first = svc.getAuditSignPrivateKey();
        first[0] = (byte) 0xFF;
        assertThat(svc.getAuditSignPrivateKey()[0]).isNotEqualTo((byte) 0xFF);
    }

    @Test
    void auditKeys_withoutConfig_throwIllegalState_loginUnaffected() {
        final KeyService svc = newServiceWithSm2("sm2-login-v1",
                Sm2TestVectors.GBT_PRIVATE_KEY_HEX, Sm2TestVectors.GBT_PUBLIC_KEY_HEX);
        assertThatThrownBy(svc::getAuditSignPrivateKey)
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("not configured");
        assertThatThrownBy(svc::getAuditKeyId).isInstanceOf(IllegalStateException.class);
        assertThat(svc.getSm2LoginKeyId()).isEqualTo("sm2-login-v1");
    }

    @Test
    void auditKeys_mismatchedPair_failsStartup() {
        final String tampered = Sm2TestVectors.GBT_PUBLIC_KEY_HEX
                .substring(0, Sm2TestVectors.GBT_PUBLIC_KEY_HEX.length() - 2) + "14";
        assertThatThrownBy(() -> newServiceWithAudit("sm2-audit-v1",
                Sm2TestVectors.GBT_PRIVATE_KEY_HEX, tampered))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("key pair");
    }

    @Test
    void auditVerifyPublicKeyHex_unknownKeyId_throwsIllegalArgument() {
        final KeyService svc = newServiceWithAudit("sm2-audit-v1",
                Sm2TestVectors.GBT_PRIVATE_KEY_HEX, Sm2TestVectors.GBT_PUBLIC_KEY_HEX);
        assertThatThrownBy(() -> svc.getAuditVerifyPublicKeyHex("ghost"))
                .isInstanceOf(IllegalArgumentException.class);
    }
```

`MockKeyServiceTest` +1：`getAuditKeyId_returnsMockKeyId()` 断言 "mock-key-v1"。

- [ ] **Step 2: RED**（同 T1 命令模式，`-Dtest='KeyServiceImplTest,MockKeyServiceTest' -pl fep-security-impl,fep-security-mock -am`，log `/tmp/gm-s5-t2-red.log`）期望 `找不到符号: setAuditActiveKeyId`

- [ ] **Step 3: 实现**

`KeyService` 接口追加（Javadoc 注明审计用途/防御副本/ISE-IAE 条款，风格镜像 S2a getSm2LoginKeyId）：

```java
    String getAuditKeyId();

    byte[] getAuditSignPrivateKey();

    String getAuditVerifyPublicKeyHex(String keyId);
```

`FepSecuritySm2Properties` 追加（对称 login 段；getter @SuppressFBWarnings(EI_EXPOSE_REP) 同 justification；setter 防御拷贝 + **null guard**——同 commit 顺带 R5 对称修法的 audit 侧原生带 guard，login 侧 setter null guard 归 T7）：

```java
    /** 当前活跃审计签名密钥版本号。 */
    private String auditActiveKeyId;

    /** keyId → 审计签名密钥对（多版本，轮换期历史行验签）。 */
    private Map<String, LoginKeyPair> auditKeys = new LinkedHashMap<>();
    // getter/setter 镜像 loginKeys（getter @SuppressFBWarnings EI_EXPOSE_REP 同 justification；
    // setter: this.auditKeys = loginKeys == null ? new LinkedHashMap<>() : new LinkedHashMap<>(auditKeys 参数)）
```

`KeyServiceImpl`：字段 +auditActiveKeyId/auditKeys（构造拷贝）；`validateOnStartup` 将 S2a 的 login 段校验体抽为私有 `validateSm2KeySection(String sectionName, String activeId, Map<String, LoginKeyPair> keys)`（消息含 sectionName 区分 login/audit），login/audit 两段各调一次（行为保持——login 消息文本不变量由既有测试钉住，抽取时消息格式 `"SM2 " + sectionName + " ..."` 须与既有断言 `hasMessageContaining("key pair")/("out of range")/("not configured")` 兼容）；3 新方法：

```java
    @Override
    public String getAuditKeyId() {
        return requireAuditConfigured();
    }

    @Override
    public byte[] getAuditSignPrivateKey() {
        final FepSecuritySm2Properties.LoginKeyPair active = auditKeys.get(requireAuditConfigured());
        return HexFormat.of().parseHex(active.getPrivateKeyHex());
    }

    @Override
    public String getAuditVerifyPublicKeyHex(final String keyId) {
        requireAuditConfigured();
        final FepSecuritySm2Properties.LoginKeyPair pair = auditKeys.get(keyId);
        if (pair == null) {
            throw new IllegalArgumentException("Unknown SM2 audit keyId: " + keyId);
        }
        return pair.getPublicKeyHex();
    }

    private String requireAuditConfigured() {
        if (auditActiveKeyId == null || auditKeys.isEmpty()) {
            throw new IllegalStateException(
                    "SM2 audit keys not configured (fep.security.sm2.audit-active-key-id / audit-keys)");
        }
        return auditActiveKeyId;
    }
```
> `parseHex` 每次新建数组即防御副本。

mock/test 三处桩（MockKeyService / TestKeyServiceConfiguration 匿名 / CallbackCredentialEncryptionFacadeTest 匿名）各加 3 方法：`getAuditKeyId()` → 常量（"mock-key-v1"/"mock-key-v1"/"k-new"）、`getAuditSignPrivateKey()` → `new byte[32]` 或 clone 常量、`getAuditVerifyPublicKeyHex(id)` → **固定合法 130-hex 常量**（复用 GB/T 公钥 hex 字面值——v0.3 C-NEW-1：`AuditIntegrityServiceImpl.verifyEntry` 的 `parseHex` 先于 MockSignService 执行，非 hex 串将致 dev /integrity 恒报 UNKNOWN_KEY 假断点；MockSignService 忽略密钥内容，仅需可解析）。`MockKeyServiceTest` 另增断言 `getAuditVerifyPublicKeyHex("any")` matches `"04[0-9a-fA-F]{128}"`。

- [ ] **Step 4: GREEN + 4 实现点编译闸口 + 门禁**

```bash
export JAVA_HOME=/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home; export PATH="$JAVA_HOME/bin:$PATH"
cd /Users/muzhou/FEP_v1.0_wt-gm-s5 && ./mvnw test -pl fep-security-impl,fep-security-mock -am -Dtest='KeyServiceImplTest,MockKeyServiceTest' -Dsurefire.failIfNoSpecifiedTests=false --batch-mode --no-transfer-progress > /tmp/gm-s5-t2-green.log 2>&1; echo "EXIT=$?"; grep -E "Tests run:.*Failures|BUILD" /tmp/gm-s5-t2-green.log | tail -4
cd /Users/muzhou/FEP_v1.0_wt-gm-s5 && ./mvnw test-compile -pl fep-web -am --batch-mode --no-transfer-progress > /tmp/gm-s5-t2-tc.log 2>&1; echo "TC=$?"; tail -2 /tmp/gm-s5-t2-tc.log
cd /Users/muzhou/FEP_v1.0_wt-gm-s5 && ./mvnw compile spotbugs:check -pl fep-security-api,fep-security-impl,fep-security-mock -am --batch-mode --no-transfer-progress > /tmp/gm-s5-t2-sb.log 2>&1; echo "SB=$?"; grep -cE "BugInstance size is 0" /tmp/gm-s5-t2-sb.log
```

- [ ] **Step 5: 提交** `feat(security): SM2 audit key section + KeyService audit accessors (GM S5 T2)`
- [ ] **Step 6: spec+quality review subagent**

---

## Task 3: AuditIntegrityService — hash 链 + 行签名原语 `模式 B`

**PRD 依据:** 架构 §1219 不可篡改 + PRD §8.3
**追溯 ID:** FR-INFRA-GM-AUDIT-INTEGRITY

**验收标准:**
1. `GENESIS_PREV_HASH` = 64 个 '0'
2. `computeEntryHash(prevHashHex, canonicalBytes)` = `SM3(prevHashHex_utf8 ∥ canonicalBytes)` 64 小写 hex；可与 HashServiceImplTest 向量交叉手算锚定（prev=GENESIS、canonical="abc" 时 = `sm3Hex(("0"*64 + "abc").bytes)`——测试以 HashService 直算对照，防实现自说自话）
3. `signEntryHash(hashHex)` 委托 `SignService.sign(hashHex_utf8, keyService.getAuditSignPrivateKey())`，返回签名串；`auditKeyId()` 透传 `keyService.getAuditKeyId()`
4. `verifyEntry(hashHex, signature, keyId)` 委托 `SignService.verify(hashHex_utf8, signature, parseHex(getAuditVerifyPublicKeyHex(keyId)))`
5. null 入参 → IllegalArgumentException
6. 装配：`GmAuditConfiguration` **always-on** @Bean，依赖注入容器内 `SignService`/`KeyService`/`HashService` bean（mock provider 下= mock 签名，impl 下= 真签名——无 @ConditionalOnProperty）

**Files:** Create `fep-security-api/.../api/AuditIntegrityService.java` + `fep-security-impl/.../impl/audit/AuditIntegrityServiceImpl.java` + `fep-security-impl/.../impl/GmAuditConfiguration.java` + test `AuditIntegrityServiceImplTest.java`；Modify root `pom.xml`（JaCoCo +`**/GmAuditConfiguration.class`）

- [ ] **Step 1: 失败测试**（直构 `new AuditIntegrityServiceImpl(new HashServiceImpl(), new SignServiceImpl(), keyServiceWithAudit)`，keyService 用 T2 助手实例；用例：genesis 常量 / computeEntryHash 与 HashService 直算对照 ×2（含中文 canonical）/ sign→verify roundtrip GREEN / verify 篡改 hash false / unknown keyId IAE / null IAE）
- [ ] **Step 2: RED**（log `/tmp/gm-s5-t3-red.log`）
- [ ] **Step 3: 实现**

```java
// fep-security-api/src/main/java/com/puchain/fep/security/api/AuditIntegrityService.java
package com.puchain.fep.security.api;

/**
 * 审计日志完整性原语（SM3 hash 链 + SM2 行签名，架构 §1219 不可篡改）。
 *
 * <p>hash = SM3(prevHashHex ∥ canonical)；signature = SM2(SM3withSM2 裸签) over hashHex。
 * canonical 规范化由调用方（fep-web AuditCanonicalizer）负责；本 SPI 不感知日志字段结构。
 * 签名密钥 = fep.security.sm2.audit-* 段（KeyService audit accessors）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public interface AuditIntegrityService {

    /** 链首前驱哈希（64 个 '0'）。 */
    String GENESIS_PREV_HASH = "0".repeat(64);

    /**
     * 计算链式行哈希。
     *
     * @param prevHashHex 前一行 hash（64 hex；链首用 {@link #GENESIS_PREV_HASH}），非 null
     * @param canonical   本行规范化字节，非 null
     * @return 64 字符小写 hex
     * @throws IllegalArgumentException 入参 null 或 prevHashHex 非 64 hex
     */
    String computeEntryHash(String prevHashHex, byte[] canonical);

    /**
     * 用活跃审计私钥签名行哈希。
     *
     * @param hashHex 行 hash（64 hex），非 null
     * @return 签名串（impl=Base64(r∥s 64B)；mock=占位）
     * @throws IllegalStateException 审计密钥未配置（impl provider）
     */
    String signEntryHash(String hashHex);

    /**
     * 当前活跃审计密钥版本（落 sign_key_id 列）。
     *
     * @return 审计密钥版本号
     */
    String auditKeyId();

    /**
     * 按版本验签行哈希。
     *
     * @param hashHex   行 hash，非 null
     * @param signature 签名串，非 null
     * @param keyId     签名时密钥版本，非 null
     * @return 验签通过 true
     * @throws IllegalArgumentException keyId 未知或入参 null
     * @throws IllegalStateException    审计密钥段未配置（impl provider）——配置错误信号，
     *                                  与 {@link #signEntryHash} 对称；verifier 不捕获此异常
     */
    boolean verifyEntry(String hashHex, String signature, String keyId);
}
```

```java
// fep-security-impl/src/main/java/com/puchain/fep/security/impl/audit/AuditIntegrityServiceImpl.java
package com.puchain.fep.security.impl.audit;

import com.puchain.fep.security.api.AuditIntegrityService;
import com.puchain.fep.security.api.HashService;
import com.puchain.fep.security.api.KeyService;
import com.puchain.fep.security.api.SignService;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Objects;

/**
 * 审计完整性原语实现（组合 HashService SM3 + SignService SM2 + KeyService 审计密钥）。
 *
 * <p>无 Spring stereotype，经 GmAuditConfiguration @Bean 注册（always-on——hash 链恒真 SM3；
 * 签名强度随 provider：mock 域占位签名、impl 域真 SM2，镜像登录 mock 哲学）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class AuditIntegrityServiceImpl implements AuditIntegrityService {

    private final HashService hashService;
    private final SignService signService;
    private final KeyService keyService;

    /**
     * 组合构造。
     *
     * @param hashService SM3 摘要，非 null
     * @param signService SM2 签名，非 null
     * @param keyService  审计密钥访问，非 null
     */
    public AuditIntegrityServiceImpl(final HashService hashService,
            final SignService signService, final KeyService keyService) {
        this.hashService = Objects.requireNonNull(hashService, "hashService");
        this.signService = Objects.requireNonNull(signService, "signService");
        this.keyService = Objects.requireNonNull(keyService, "keyService");
    }

    @Override
    public String computeEntryHash(final String prevHashHex, final byte[] canonical) {
        if (prevHashHex == null || !prevHashHex.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("prevHashHex must be 64 lowercase hex chars");
        }
        if (canonical == null) {
            throw new IllegalArgumentException("canonical must not be null");
        }
        final byte[] prev = prevHashHex.getBytes(StandardCharsets.UTF_8);
        final byte[] joined = new byte[prev.length + canonical.length];
        System.arraycopy(prev, 0, joined, 0, prev.length);
        System.arraycopy(canonical, 0, joined, prev.length, canonical.length);
        return hashService.sm3Hex(joined);
    }

    @Override
    public String signEntryHash(final String hashHex) {
        requireHash(hashHex);
        return signService.sign(hashHex.getBytes(StandardCharsets.UTF_8),
                keyService.getAuditSignPrivateKey());
    }

    @Override
    public String auditKeyId() {
        return keyService.getAuditKeyId();
    }

    @Override
    public boolean verifyEntry(final String hashHex, final String signature, final String keyId) {
        requireHash(hashHex);
        if (signature == null || keyId == null) {
            throw new IllegalArgumentException("signature/keyId must not be null");
        }
        final byte[] publicKey = HexFormat.of()
                .parseHex(keyService.getAuditVerifyPublicKeyHex(keyId));
        return signService.verify(hashHex.getBytes(StandardCharsets.UTF_8), signature, publicKey);
    }

    private static void requireHash(final String hashHex) {
        if (hashHex == null) {
            throw new IllegalArgumentException("hashHex must not be null");
        }
    }
}
```

```java
// fep-security-impl/src/main/java/com/puchain/fep/security/impl/GmAuditConfiguration.java
package com.puchain.fep.security.impl;

import com.puchain.fep.security.api.AuditIntegrityService;
import com.puchain.fep.security.api.HashService;
import com.puchain.fep.security.api.KeyService;
import com.puchain.fep.security.api.SignService;
import com.puchain.fep.security.impl.audit.AuditIntegrityServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 审计完整性装配 — always-on（hash 链恒真 SM3；签名实现随容器内 SignService bean
 * 自适应 mock/impl，无 @ConditionalOnProperty——镜像 GmHashConfiguration）。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Configuration
public class GmAuditConfiguration {

    /**
     * 审计完整性原语（always-on 单例）。
     *
     * @param hashService SM3
     * @param signService SM2（mock/impl 随 provider）
     * @param keyService  审计密钥
     * @return AuditIntegrityService 实现
     */
    @Bean
    public AuditIntegrityService auditIntegrityService(final HashService hashService,
            final SignService signService, final KeyService keyService) {
        return new AuditIntegrityServiceImpl(hashService, signService, keyService);
    }
}
```
root pom JaCoCo excludes +`**/GmAuditConfiguration.class`（紧邻 GmHashConfiguration 行）。

- [ ] **Step 4: GREEN + spotbugs**（log `/tmp/gm-s5-t3-{green,sb}.log`，命令模式同 T1）
- [ ] **Step 5: 提交** `feat(security): AuditIntegrityService hash-chain + row-signature primitives (GM S5 T3)`
- [ ] **Step 6: spec+quality review subagent**

---

## Task 4: V36 迁移 + entity 扩展 + append-only `模式 B`

**PRD 依据:** 架构 §1219（append-only / TraceId 查询）
**追溯 ID:** FR-INFRA-GM-AUDIT-INTEGRITY

**验收标准:**
1. V36 在 dev H2 上 flyway 迁移成功；6 新列全 nullable；`UNIQUE` 约束 seq；trace_id 普通索引
2. entity 6 新字段（seq Long / prevHash / hash / signature / signKeyId / traceId）全 `updatable=false` + 类级 `@Immutable`
3. repository `findTopBySeqIsNotNullOrderBySeqDesc()` 返回链尾行；`findBySeqIsNotNullOrderBySeqAsc(Pageable)` 分页链读（v0.2 C-3：全链扫描形态，链外行天然过滤）
4. 存量行（hash NULL）不破坏查询/详情既有行为（search/findById 回归绿）
5. seq 重复插入 → 唯一键冲突异常（链分叉兜底实测）

**Files:** Create `V36__operation_log_integrity.sql`；Modify `SysOperationLog.java` / `SysOperationLogRepository.java`；Create test `SysOperationLogIntegrityColumnsTest.java`（@DataJpaTest）

- [ ] **Step 1: V36**

```sql
-- V36__operation_log_integrity.sql
-- GM S5: 审计日志完整性列（SM3 hash 链 + SM2 行签名 + TraceId，架构 §1219）
-- 全列 nullable：V36 前存量行为链外（不回填——历史完整性无法事后背书，Plan 抉择⑧）
ALTER TABLE t_sys_operation_log ADD COLUMN seq BIGINT DEFAULT NULL COMMENT '链序号（部署后首行=1，连续递增）';
ALTER TABLE t_sys_operation_log ADD COLUMN prev_hash VARCHAR(64) DEFAULT NULL COMMENT '前行 SM3 hash（链首=64个0）';
ALTER TABLE t_sys_operation_log ADD COLUMN hash VARCHAR(64) DEFAULT NULL COMMENT '本行 SM3 hash = SM3(prev_hash || canonical)';
ALTER TABLE t_sys_operation_log ADD COLUMN signature VARCHAR(120) DEFAULT NULL COMMENT 'SM2 裸签 Base64（r||s 64字节→88字符；mock 域为占位串）';
ALTER TABLE t_sys_operation_log ADD COLUMN sign_key_id VARCHAR(64) DEFAULT NULL COMMENT '签名时审计密钥版本';
ALTER TABLE t_sys_operation_log ADD COLUMN trace_id VARCHAR(64) DEFAULT NULL COMMENT '链路追踪 ID（TraceIdFilter MDC）';
ALTER TABLE t_sys_operation_log ADD CONSTRAINT uk_audit_seq UNIQUE (seq);
CREATE INDEX idx_log_trace ON t_sys_operation_log (trace_id);
```

- [ ] **Step 2: entity 扩展**（6 字段 `@Column(name=..., updatable = false)` + getter/setter Javadoc；类级 `@org.hibernate.annotations.Immutable`；既有 13 字段不动）+ repository 2 方法 + **失败测试**：

```java
// fep-web/src/test/java/com/puchain/fep/web/sysmgmt/log/domain/SysOperationLogIntegrityColumnsTest.java
// @DataJpaTest（既有 fep-web @DataJpaTest 范式）：
// ① save 含 6 新列的行 → findById 读回逐列相等（含 64 hex hash / 88 字符 Base64 signature）
// ② findTopBySeqIsNotNullOrderBySeqDesc：插 seq=1,2,3 → 返回 seq=3 行
// ③ 同 seq 二次 save（不同 logId）→ DataIntegrityViolationException（uk_audit_seq 兜底）
// ④ 存量形态行（6 列全 null）save/findById 正常（V36 前行为保持）
// ⑤ findBySeqIsNotNullOrderBySeqAsc(PageRequest.of(0,2)) → 2 行升序（链外 null-seq 行不出现）
```
（测试完整代码实施时按上述 5 断言写，字段值用确定字面值；H2 @DataJpaTest 自动跑 V36。）

- [ ] **Step 3: RED→GREEN**（log `/tmp/gm-s5-t4-{red,green}.log`；`-Dtest=SysOperationLogIntegrityColumnsTest -pl fep-web -am`）+ spotbugs fep-web
- [ ] **Step 4: 提交** `feat(web): V36 operation-log integrity columns + immutable entity (GM S5 T4)`
- [ ] **Step 5: spec+quality review subagent**

---

## Task 5: AuditChainWriter 串行化写入 + 切面集成 `模式 B`

**PRD 依据:** 架构 §1219 + PRD §8.3 全覆盖
**追溯 ID:** FR-INFRA-GM-AUDIT-INTEGRITY

**验收标准:**
1. `AuditCanonicalizer.canonicalize(entity, seq)`：netstring 15 字段（抉择④），null → `-1:`；同一 entity 幂等输出；createTime 截断秒（用 `truncatedTo(ChronoUnit.SECONDS)` + `ISO_LOCAL_DATE_TIME`）；确定性断言（同输入两次同输出 + 已知 entity 手算字面值锚定 1 例）
2. `AuditChainWriter.append(entity)`：填 seq=last+1 / prevHash=lastHash(链首 GENESIS) / hash / signature / signKeyId / traceId(MDC，无则 null) → save → **save 成功才推进内存链尾**
3. save 抛异常 → 链尾不动 + 异常向上抛（由切面既有 catch WARN 吞——主业务不受影响语义保持）；下一次 append 复用同 seq（无空洞）
4. 并发 16 线程 ×20 行 append → 320 行 seq 连续 1..320 无重复、逐行 hash 链重算全通（串行化正确性）
5. `@PostConstruct` 启动恢复：DB 已有 seq=5 链尾 → 新 append seq=6 且 prevHash=尾行 hash；空表 → seq=1 + GENESIS
6. `OperationLogAspect.saveLog` 改为 `auditChainWriter.append(entity)`（原 `operationLogRepository.save` 不再直调）；切面对 writer 异常仍仅 WARN（既有 catch 不变）
7. mock 域全 context：append 后行 signature="MOCK_SIGNATURE"、hash 为真 SM3（@SpringBootTest dev 既有测试不破坏）

**Files:** Create `AuditCanonicalizer.java` / `AuditChainWriter.java` + tests（`AuditCanonicalizerTest` / `AuditChainWriterTest`[@DataJpaTest + 直构 writer + 真 AuditIntegrityServiceImpl(HashServiceImpl + MockSignService 实例 + T2 助手 keyService)；**N-2 注记（v0.3 扩展）**：REQUIRES_NEW 下**全部** append 用例真提交不回滚——seed/断言用 `@Transactional(propagation = NOT_SUPPORTED)` 或 JdbcTemplate 自动提交（镜像红线 provider_impl_full_context ②③：未提交测试事务内 seed 会让 REQUIRES_NEW 撞行锁超时而非确定性 DIVE）；逐用例先 `recoverChainTail()` 并以相对链尾断言隔离残留；直构 writer 4 依赖 = repo + 真 AuditIntegrityServiceImpl + `@Autowired PlatformTransactionManager` + `new SimpleMeterRegistry()`]）；Modify `OperationLogAspect.java` + **`OperationLogAspectTest.java`**（v0.2 B-2：实测 L88 直构 `new OperationLogAspect(mockRepo)`——构造器换依赖后编译破坏点；mock 改 `@Mock AuditChainWriter` + 断言 `verify(auditChainWriter).append(any())`）

**v0.2 增补验收：**
8. append 落库在 `REQUIRES_NEW` 独立事务（C-1）
9. `DataIntegrityViolationException` → `recoverChainTail()` 自愈 + rethrow（C-2：测试预插 seq 占用行 → append 撞唯一键 → 自愈后下一次 append seq 正确且成功）
10. gauge `fep_audit_chain_tail_seq` 随推进/恢复更新（抉择⑩）

**AuditChainWriter 核心实现（完整）：**

```java
// fep-web/src/main/java/com/puchain/fep/web/sysmgmt/log/audit/AuditChainWriter.java
// import 增量: io.micrometer.core.instrument.{Gauge,MeterRegistry} / java.util.concurrent.atomic.AtomicLong
//             org.springframework.dao.DataIntegrityViolationException
//             org.springframework.transaction.{PlatformTransactionManager,TransactionDefinition}
//             org.springframework.transaction.support.TransactionTemplate
package com.puchain.fep.web.sysmgmt.log.audit;

import com.puchain.fep.security.api.AuditIntegrityService;
import com.puchain.fep.web.sysmgmt.log.domain.SysOperationLog;
import com.puchain.fep.web.sysmgmt.log.repository.SysOperationLogRepository;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * 审计日志链式写入（单写者串行化，架构 §1219 不可篡改）。
 *
 * <p>单实例部署假设（application.yml 注记）下以 synchronized 临界区保证链不分叉；
 * DB uk_audit_seq 唯一约束兜底（意外多实例时链分叉立即以唯一键冲突暴露而非静默）。
 * save 成功才推进内存链尾——失败时链尾不动，下次复用同 seq，链无空洞。
 * 启动期从链尾行恢复（seq 最大且非 null 行）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class AuditChainWriter {

    /** MDC 链路追踪键（TraceIdFilter 约定）。 */
    private static final String MDC_TRACE_ID = "traceId";

    private static final org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger(AuditChainWriter.class);

    private final SysOperationLogRepository repository;
    private final AuditIntegrityService auditIntegrityService;
    private final TransactionTemplate requiresNewTx;
    private final AtomicLong tailSeqGauge = new AtomicLong();

    private long lastSeq;
    private String lastHash;

    /**
     * 构造（落库 REQUIRES_NEW 独立事务，v0.2 C-1——外层业务事务 rollback 不产生
     * "行消失而链尾已推进"假阳；gauge 外锚见抉择⑩）。
     *
     * @param repository            操作日志仓储
     * @param auditIntegrityService 完整性原语
     * @param transactionManager    事务管理器
     * @param meterRegistry         指标注册
     */
    public AuditChainWriter(final SysOperationLogRepository repository,
            final AuditIntegrityService auditIntegrityService,
            final PlatformTransactionManager transactionManager,
            final MeterRegistry meterRegistry) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.auditIntegrityService = Objects.requireNonNull(auditIntegrityService,
                "auditIntegrityService");
        this.requiresNewTx = new TransactionTemplate(
                Objects.requireNonNull(transactionManager, "transactionManager"));
        this.requiresNewTx.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        Gauge.builder("fep_audit_chain_tail_seq", tailSeqGauge, AtomicLong::get)
                .description("审计 hash 链尾 seq（回退即截断告警线索，架构 §1219）")
                .register(Objects.requireNonNull(meterRegistry, "meterRegistry"));
    }

    /**
     * 启动期链尾恢复：取 seq 最大行；无链上行则从 GENESIS 起。
     */
    @PostConstruct
    public synchronized void recoverChainTail() {
        final Optional<SysOperationLog> tail = repository.findTopBySeqIsNotNullOrderBySeqDesc();
        if (tail.isPresent()) {
            this.lastSeq = tail.get().getSeq();
            this.lastHash = tail.get().getHash();
        } else {
            this.lastSeq = 0L;
            this.lastHash = AuditIntegrityService.GENESIS_PREV_HASH;
        }
        tailSeqGauge.set(lastSeq);
        // 仅 long seq 入日志（无 taint 字段，CRLF 面为零）
        LOG.info("audit chain tail recovered: seq={}", lastSeq);
        try {
            auditIntegrityService.auditKeyId();
        } catch (final IllegalStateException e) {
            // v0.3 抉择⑪：impl 域 audit 段漏配 = 部署错误，append 将持续失败（切面 WARN 吞）
            // ——启动期一次性醒目告警（消息为静态配置键文案，无 taint）
            LOG.warn("audit signing keys not configured - operation log rows will fail "
                    + "to persist until fep.security.sm2.audit-* is provided: {}", e.getMessage());
        }
    }

    /**
     * 链式落库：分配 seq、链接 prevHash、计算 hash 与行签名后保存。
     *
     * @param entity 已填业务字段的日志实体（seq/hash 等完整性字段由本方法填充）
     */
    public synchronized void append(final SysOperationLog entity) {
        final long seq = lastSeq + 1;
        entity.setSeq(seq);
        entity.setPrevHash(lastHash);
        entity.setTraceId(MDC.get(MDC_TRACE_ID));
        final byte[] canonical = AuditCanonicalizer.canonicalize(entity, seq)
                .getBytes(StandardCharsets.UTF_8);
        final String hash = auditIntegrityService.computeEntryHash(lastHash, canonical);
        entity.setHash(hash);
        entity.setSignature(auditIntegrityService.signEntryHash(hash));
        entity.setSignKeyId(auditIntegrityService.auditKeyId());
        try {
            requiresNewTx.executeWithoutResult(status -> repository.save(entity));
        } catch (final DataIntegrityViolationException e) {
            // 意外 seq 占用（如多实例误部署）：重锚 DB 链尾自愈后上抛（切面 WARN 吞），
            // 下一次 append 在正确链尾继续——poison-state 不静默持续（v0.2 C-2）
            recoverChainTail();
            throw e;
        }
        // 提交成功才推进链尾（失败时下次复用同 seq，链无空洞）
        this.lastSeq = seq;
        this.lastHash = hash;
        tailSeqGauge.set(seq);
    }
}
```

**AuditCanonicalizer 核心实现（完整）：**

```java
// fep-web/src/main/java/com/puchain/fep/web/sysmgmt/log/audit/AuditCanonicalizer.java
package com.puchain.fep.web.sysmgmt.log.audit;

import com.puchain.fep.web.sysmgmt.log.domain.SysOperationLog;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * 审计行规范化（netstring 风格，确定性 + DB 往返稳定）。
 *
 * <p>字段序固定 15 项：seq, logId, userId, userAccount, module, operation, description,
 * method, requestUrl, requestParams, responseStatus, ipAddress, durationMs,
 * createTime(截断秒 ISO_LOCAL_DATE_TIME——t_sys_operation_log TIMESTAMP 无小数位，
 * 截断保证写入与校验重算两侧一致), traceId。每字段编码为 {@code <utf8字节长>:<值>}，
 * null 编码为 {@code -1:}。写入（AuditChainWriter）与校验（AuditChainVerifier）共用本类。
 * <strong>不变量（v0.2 crypto N-2）：</strong>入链字段必须在 save 前完成一切截断/规范化
 * （requestParams 已由切面截 2000；description 等若未来引入超列宽来源须先截断再 append，
 * 否则 DB 静默截断致校验假阳 HASH_MISMATCH）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public final class AuditCanonicalizer {

    private AuditCanonicalizer() {
    }

    /**
     * 生成规范化串。
     *
     * @param e   日志实体（业务字段已填）
     * @param seq 链序号
     * @return 确定性规范化串
     */
    public static String canonicalize(final SysOperationLog e, final long seq) {
        final StringBuilder sb = new StringBuilder(256);
        field(sb, Long.toString(seq));
        field(sb, e.getLogId());
        field(sb, e.getUserId());
        field(sb, e.getUserAccount());
        field(sb, e.getModule());
        field(sb, e.getOperation() == null ? null : e.getOperation().name());
        field(sb, e.getDescription());
        field(sb, e.getMethod());
        field(sb, e.getRequestUrl());
        field(sb, e.getRequestParams());
        field(sb, e.getResponseStatus() == null ? null : e.getResponseStatus().toString());
        field(sb, e.getIpAddress());
        field(sb, e.getDurationMs() == null ? null : e.getDurationMs().toString());
        field(sb, e.getCreateTime() == null ? null
                : DateTimeFormatter.ISO_LOCAL_DATE_TIME
                        .format(e.getCreateTime().truncatedTo(ChronoUnit.SECONDS)));
        field(sb, e.getTraceId());
        return sb.toString();
    }

    private static void field(final StringBuilder sb, final String value) {
        if (value == null) {
            sb.append("-1:");
            return;
        }
        sb.append(value.getBytes(StandardCharsets.UTF_8).length).append(':').append(value);
    }
}
```

**切面集成**（`OperationLogAspect.saveLog` 内唯一改动行 + 构造器换依赖）：

```java
// 原: operationLogRepository.save(entity);
auditChainWriter.append(entity);
// + entity.setCreateTime(LocalDateTime.now()) 改为
//   entity.setCreateTime(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
//   （canonical 截断与存储值一致化，抉择④；构造器注入 AuditChainWriter 替换 SysOperationLogRepository）
```

- [ ] Steps: 失败测试（含 16 线程并发用例，`ExecutorService` + `CountDownLatch` 同时放行，事后全表重算链）→ RED `/tmp/gm-s5-t5-red.log` → 实现 → GREEN + fep-web spotbugs（`/tmp/gm-s5-t5-{green,sb}.log`）→ 提交 `feat(web): serialized audit hash-chain writer + aspect integration (GM S5 T5)` → spec+quality review

---

## Task 6: 篡改检测端点 + TraceId 查询 + append-only ArchUnit `模式 B`

**PRD 依据:** 架构 §1219（篡改可检 / TraceId 查询）
**追溯 ID:** FR-INFRA-GM-AUDIT-INTEGRITY

**验收标准（v0.2 B-1/B-3/C-3 重订）:**
1. `AuditChainVerifier.verifyChain()`：分页（500/批）`findBySeqIsNotNullOrderBySeqAsc` 升序重算——逐行 ⓪ seq 连续（首链上行 seq==1，后续 seq==prev+1，否则 `GAP`；链首 prevHash≠GENESIS 由 ① PREV_LINK 捕获——expectedPrev 初值即 GENESIS，v0.3 标签口径）① prevHash 与前行 hash 一致（`PREV_LINK`）② hash 重算一致（AuditCanonicalizer 同实现，`HASH_MISMATCH`）③ **恒验签**（无任何按值跳过；`verifyEntry` false → `SIGNATURE_INVALID`；unknown sign_key_id 的 IAE catch → `UNKNOWN_KEY`）；返回 `ChainVerifyResult{totalChecked, intact, firstBreakSeq, breakType(GAP|PREV_LINK|HASH_MISMATCH|SIGNATURE_INVALID|UNKNOWN_KEY)}`，首断点即停
2. 篡改实测（@DataJpaTest + JdbcTemplate 直改）：改 description → HASH_MISMATCH 且 firstBreakSeq 正确；改 hash 列 → 本行 HASH_MISMATCH（重算不符）；DELETE 中间行 → GAP；改 signature 为非法串 → SIGNATURE_INVALID（mock 域 MockSignService 恒 true，故该用例以**直构 verifier + 真 SignServiceImpl + T2 助手 keyService** 跑 impl 语义）；改 sign_key_id 为未配置版本 → UNKNOWN_KEY
3. 端点 `GET /api/v1/sys/logs/integrity`（实测既有 `@RequestMapping("/api/v1/sys/logs")` 复用；管理员权限随既有类级安全配置；@OperationLog 注解——校验操作自身入链）返回 result JSON
4. `search` 增 `traceId` 可选参数（repository @Query 增 trace_id 条件 + Controller/Service 透传；既有调用零破坏——参数可空）
5. ArchUnit `OperationLogAppendOnlyArchTest`（代码见 Step 3）：fep-web 生产代码对 `SysOperationLogRepository` 调用方法白名单 {save, findTopBySeqIsNotNullOrderBySeqDesc, findBySeqIsNotNullOrderBySeqAsc, search, findById}——delete*/saveAll 等出现即 FAIL
6. 既有 search/findById 回归绿
7. **intact 正向用例**（v0.3 C-NEW-1/N-NEW-6）：mock 全 context（@SpringBootTest dev）经切面真实写入 ≥3 行后调端点 → intact==true 且 totalChecked≥3（钉死 dev 域恒验签链路毕通：parseHex(合法 mock hex) → MockSignService true）

**Files:** Create `AuditChainVerifier.java` / `ChainVerifyResult.java` / `OperationLogAppendOnlyArchTest.java` + tests（`AuditChainVerifierTest`）；Modify `SysOperationLogController.java` / `SysOperationLogService.java` / `SysOperationLogRepository.java`

- [ ] **Step 1: 失败测试**（按验收 2 的 6 个篡改/断点场景逐用例；RED `/tmp/gm-s5-t6-red.log`）
- [ ] **Step 2: AuditChainVerifier 实现**

```java
// fep-web/src/main/java/com/puchain/fep/web/sysmgmt/log/audit/AuditChainVerifier.java
package com.puchain.fep.web.sysmgmt.log.audit;

import com.puchain.fep.security.api.AuditIntegrityService;
import com.puchain.fep.web.sysmgmt.log.domain.SysOperationLog;
import com.puchain.fep.web.sysmgmt.log.repository.SysOperationLogRepository;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * 审计链篡改检测（架构 §1219）：分页重算 hash 链 + 逐行恒验签（v0.2 B-1——
 * 无任何按签名值跳过的旁路；mock 域 MockSignService 恒 true 自然通过，
 * impl 域占位/篡改签名诚实报断点）。纯删尾截断为结构性盲区（抉择⑩ 披露，
 * 缓解靠链尾 gauge 外锚）。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class AuditChainVerifier {

    /** 分页批量（防全表载入 OOM）。 */
    private static final int PAGE_SIZE = 500;

    private final SysOperationLogRepository repository;
    private final AuditIntegrityService auditIntegrityService;

    /**
     * 构造。
     *
     * @param repository            操作日志仓储
     * @param auditIntegrityService 完整性原语
     */
    public AuditChainVerifier(final SysOperationLogRepository repository,
            final AuditIntegrityService auditIntegrityService) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.auditIntegrityService = Objects.requireNonNull(auditIntegrityService,
                "auditIntegrityService");
    }

    /**
     * 全链校验：seq 连续性 → prev 链接 → hash 重算 → 行验签，首断点即停。
     *
     * @return 校验结果（intact / 首断点 seq 与类型）
     */
    public ChainVerifyResult verifyChain() {
        long checked = 0;
        long expectedSeq = 1;
        String expectedPrev = AuditIntegrityService.GENESIS_PREV_HASH;
        int page = 0;
        while (true) {
            final Page<SysOperationLog> batch = repository
                    .findBySeqIsNotNullOrderBySeqAsc(PageRequest.of(page, PAGE_SIZE));
            for (final SysOperationLog row : batch.getContent()) {
                final long seq = row.getSeq();
                if (seq != expectedSeq) {
                    return ChainVerifyResult.broken(checked, seq, BreakType.GAP);
                }
                if (!expectedPrev.equals(row.getPrevHash())) {
                    return ChainVerifyResult.broken(checked, seq, BreakType.PREV_LINK);
                }
                final byte[] canonical = AuditCanonicalizer.canonicalize(row, seq)
                        .getBytes(StandardCharsets.UTF_8);
                final String recomputed = auditIntegrityService
                        .computeEntryHash(expectedPrev, canonical);
                if (!recomputed.equals(row.getHash())) {
                    return ChainVerifyResult.broken(checked, seq, BreakType.HASH_MISMATCH);
                }
                final boolean signatureValid;
                try {
                    signatureValid = auditIntegrityService.verifyEntry(
                            row.getHash(), row.getSignature(), row.getSignKeyId());
                } catch (final IllegalArgumentException e) {
                    // unknown sign_key_id（含 dev→prod 晋升后的 mock 历史行）→ 诚实断点
                    return ChainVerifyResult.broken(checked, seq, BreakType.UNKNOWN_KEY);
                }
                if (!signatureValid) {
                    return ChainVerifyResult.broken(checked, seq, BreakType.SIGNATURE_INVALID);
                }
                checked++;
                expectedSeq = seq + 1;
                expectedPrev = row.getHash();
            }
            if (!batch.hasNext()) {
                return ChainVerifyResult.intact(checked);
            }
            page++;
        }
    }

    /** 断点类型。 */
    public enum BreakType {
        /** seq 不连续（删行/链首非 1）。 */
        GAP,
        /** prev_hash 与前行 hash 失配。 */
        PREV_LINK,
        /** 行 hash 重算失配（字段被改）。 */
        HASH_MISMATCH,
        /** SM2 行验签失败（签名被改/伪造）。 */
        SIGNATURE_INVALID,
        /** sign_key_id 不在配置密钥集（含 mock 历史行于 impl 域）。 */
        UNKNOWN_KEY
    }
}
```

```java
// fep-web/src/main/java/com/puchain/fep/web/sysmgmt/log/audit/ChainVerifyResult.java
package com.puchain.fep.web.sysmgmt.log.audit;

/**
 * 审计链校验结果（端点 JSON 载体）。
 *
 * @param totalChecked  已校验链上行数
 * @param intact        链完整
 * @param firstBreakSeq 首断点 seq（intact 时 null）
 * @param breakType     断点类型（intact 时 null）
 * @author FEP Team
 * @since 1.0.0
 */
public record ChainVerifyResult(long totalChecked, boolean intact,
        Long firstBreakSeq, AuditChainVerifier.BreakType breakType) {

    /**
     * 完整链结果。
     *
     * @param totalChecked 已校验行数
     * @return intact 结果
     */
    public static ChainVerifyResult intact(final long totalChecked) {
        return new ChainVerifyResult(totalChecked, true, null, null);
    }

    /**
     * 断链结果。
     *
     * @param totalChecked 断点前已校验行数
     * @param firstBreakSeq 首断点 seq
     * @param breakType    断点类型
     * @return broken 结果
     */
    public static ChainVerifyResult broken(final long totalChecked,
            final long firstBreakSeq, final AuditChainVerifier.BreakType breakType) {
        return new ChainVerifyResult(totalChecked, false, firstBreakSeq, breakType);
    }
}
```

- [ ] **Step 3: ArchUnit append-only 规则**

```java
// fep-web/src/test/java/com/puchain/fep/architecture/OperationLogAppendOnlyArchTest.java
package com.puchain.fep.architecture;

import com.puchain.fep.web.sysmgmt.log.repository.SysOperationLogRepository;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.util.Set;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * 审计日志 append-only（架构 §1219）：生产代码对 SysOperationLogRepository
 * 仅允许白名单方法（写入 save + 链读/查询），delete*/saveAll 等修改面禁用。
 */
@AnalyzeClasses(packages = "com.puchain.fep",
        importOptions = ImportOption.DoNotIncludeTests.class)
class OperationLogAppendOnlyArchTest {

    private static final Set<String> ALLOWED = Set.of(
            "save", "search", "findById",
            "findTopBySeqIsNotNullOrderBySeqDesc", "findBySeqIsNotNullOrderBySeqAsc");

    @ArchTest
    static final ArchRule operation_log_repository_is_append_only =
            noClasses().should().callMethodWhere(new DescribedPredicate<JavaCall<?>>(
                    "SysOperationLogRepository 白名单外方法（append-only）") {
                @Override
                public boolean test(final JavaCall<?> call) {
                    return call.getTargetOwner().isAssignableTo(SysOperationLogRepository.class)
                            && !ALLOWED.contains(call.getName());
                }
            }).because("审计日志不可篡改 append-only（架构 §1219）");
}
```

- [ ] **Step 4: 端点 + traceId 查询**（Controller `@GetMapping("/integrity")` + `@OperationLog(module="日志管理", type=QUERY, description="审计链完整性校验")` 委托 verifier；search 链路三层加可空 `traceId` 参数，repository @Query 增 `AND (:traceId IS NULL OR t.traceId = :traceId)` 条件——按既有 search @Query 风格扩展）
- [ ] **Step 5: GREEN + spotbugs**（`/tmp/gm-s5-t6-{green,sb}.log`）→ 提交 `feat(web): audit chain tamper detection + traceId search + append-only arch rule (GM S5 T6)` → spec+quality review

---

## Task 7: S2a Simplify Deferred 微调批 drain `模式 A`

**PRD 依据:** 元任务（质量债 drain，2026-06-11 session-end Deferred 池）
**追溯 ID:** 无（infra/质量）

逐项（全部注释/import/防御级零行为）：
- [ ] **F1**: `Sm2LoginDecryptionProviderImplTest.java:90` `java.nio.charset.StandardCharsets` → import
- [ ] **F2**: `KeyServiceImplTest.java` `java.util.Base64`(:126) / `java.util.Locale.ROOT`(:186-187) → import
- [ ] **F3**: `TestKeyServiceConfiguration.java:72` 注释 `SM4-CBC` → `SM4-ECB`（事实修正，CryptoServiceImpl 实测 ECB）
- [ ] **F5a**: `Sm2TestVectors.java` Javadoc 加反向 cross-ref（fep-web IT 持 fixture 副本，再生成须同步）
- [ ] **R5**: `FepSecuritySm2Properties.setLoginKeys` 补 null guard（与 setSm4Keys 对称；audit 段 setter T2 已原生带）
- [ ] **F7**: `KeyServiceImplTest` 助手统一 static
- [ ] **F4**: `KeyService.getSignPrivateKey()` Javadoc ⛔ Mode E 段替换为现行口径（**措辞随本 Plan 签字由 muzhou 确认**）：
  > `<p><strong>S2b 边界（🔓 2026-06-07 解禁治理）:</strong> 真实实现待 roadmap §0.3 决策门（外部签名验签服务器 1818 vs 进程内 BC）定调后由 AI 实施 + 密码学专项 review；KeyServiceImpl 当前抛 UnsupportedOperationException。真实密钥材料部署期注入，永不入 repo。</p>`
  同型残留同批改：`MockKeyService.java` L33/L44-47/L92、`TestKeyServiceConfiguration.java` L65/L71-72/L82 的 "③ 安全工程师 ⛔ Mode E" 注释 → "🔓 解禁治理（S2b 待 §0.3）/ mock 仅 dev-CI"。
- [ ] 验证：`-Dtest='KeyServiceImplTest,Sm2LoginCipherTest,MockKeyServiceTest' + test-compile fep-web` + spotbugs（`/tmp/gm-s5-t7-*.log`）→ 提交 `chore(security): drain S2a simplify deferred micro-batch (GM S5 T7)` → quality review subagent（单路即可——零行为微调批）

---

## Task 8: 回归 + 文档 + PR + closing `模式 A`

**回归验收（红线 plan_regression_scope_explicit 两层）:**
- **strong（二选一）**: ① worktree 前台 `./mvnw verify` 全 reactor GREEN（显式 JAVA_HOME + redirect-to-file + 先 pgrep 无并发 mvn）或 ② 本机 load >50 → push 后 **GHA Build/Test & Quality SUCCESS 为权威**（S1/S4/S2a 三连先例）
- **minimum**: `./mvnw verify -pl fep-security-api,fep-security-impl,fep-security-mock,fep-web -am`
- 重点回归断言：S1 `CallbackLegacyCredentialMigrationTest` + S2a `Sm2LoginDecryptionProviderImplTest`（@MockBean SignService 在真 bean 后仍生效）零修改 GREEN

- [ ] **Step 1**: prod yml `fep.security.sm2` 块追加 audit 段样板（对称 login 段：`audit-active-key-id: ${FEP_SM2_AUDIT_ACTIVE_KEY_ID:sm2-audit-v1}` + 字面 map key + env 占位；Step 前先 grep 实测既有块行号）+ **同位注释声明"审计 hash 链 = 单写者/单实例部署前提（AuditChainWriter synchronized；多实例需先引入分布式链尾协调，归未来项）"**（v0.2 C-4 第一手声明，替代 Micrometer 注释借证）
- [ ] **Step 2**: strong 回归（或 GHA 委托，实测 load 决定）
- [ ] **Step 3**: roadmap S5 标记 ✅ + 顺手修正 roadmap L155 "V37+" 陈旧口径为实测 V36（v0.2 N-1）+ CLAUDE.md 当前项目状态（file write only，红线 fep_docs_repo_commit_taboo）
- [ ] **Step 4**: push + `gh pr create`（标题 `feat(security): GM S5 — audit log integrity (SM3 hash chain + SM2 row signature)`；body 含 Plan 路径/验收摘要/PR-Size 豁免先例注记/🤖 footer）；mutation 网络错按红线 read-verify
- [ ] **Step 5**: final whole-impl review subagent（跨 Task：canonical 写/校验两侧一致性 / login-audit 段对称 / 异常无敏感数据 / @MockBean 兼容矩阵 / 数据点自洽）
- [ ] **Step 5.5（session-end 文档记录项备忘）**: 安全维度技术文档记录 ① 私钥 JVM 进程内驻留 = 已知接受风险（String 配置链路不可清零，缓解依赖部署侧，HSM 路线归 §0.3/S3）② 截断攻击残余 + gauge 外锚（抉择⑩）③ 矩阵口径"全量记录 = @OperationLog 注解面"（v0.2 N-4）
- [ ] **Step 6**: merge 后 closing：`git -C /Users/muzhou/FEP_v1.0 fetch && git checkout main && git merge --ff-only origin/main && git worktree remove /Users/muzhou/FEP_v1.0_wt-gm-s5 && git branch -d feat/gm-s5-audit-integrity && git push origin --delete feat/gm-s5-audit-integrity && git worktree list`

---

## 评审与签字流程（强制）

1. **santa 双审**（plan-review-checklist 7 项 + 架构 §1219 对照 + 并发/往返稳定性设计审）
2. **密码学专项 review**（GB/T 32918.2 签名流程/ZA 默认 ID/PlainDSAEncoding raw r∥s/向量逐字节/密钥纪律/canonical 确定性）
3. **muzhou 签字**——未签字禁止 Task 0

### 评审员 claim grep 清单
- [ ] 签名向量与本机仲裁记录一致，完整可复跑命令（v0.2 C-5）：
  ```bash
  cd /Users/muzhou/FEP_v1.0/fep-admin-ui && node -e "const sm2=require('sm-crypto').sm2; const pub='0409f9df311e5421a150dd7d161e4bc5c672179fad1833fc076bb08ff356f35020ccea490ce26775a52dc6ea718cc1aa600aed05fbf35e084a6632f6072da9ad13'; console.log(sm2.doVerifySignature('message digest','f5a03b0648d2c4630eeac513e1bb81a15944da3827d5b74143ac7eaceee720b3b1b6aa29df212fd8763182bc0d421ca1bb9038fd1f7f42d4840b69c485bbc1aa',pub,{hash:true,der:false}))"
  ```
  期望 `true`
- [ ] `Sm2TestVectors` 可见性放宽（package→public）不破既有引用
- [ ] KeyService 接口扩展 4 实现点全列（grep `implements KeyService|new KeyService()`）
- [ ] createTime 截断秒在写入（Aspect）与 canonical（Canonicalizer）两侧一致
- [ ] V36 编号无冲突（实施时点重 grep）
- [ ] 每个 `-Dtest=`+`-am` 命令带 `-Dsurefire.failIfNoSpecifiedTests=false`；spotbugs 均 `compile spotbugs:check -am`；长跑 mvn 全部 redirect-to-file
- [ ] 数据点自洽：Task 8 个（T0 元任务除外为 T1-T8）/ 触碰文件 37 = 新建 17（单列 11 + 新建测试 6）+ 修改 20（单列 13 + 修改测试 3 + pom/prod-yml 2 + OperationLogAspectTest 1 + T7 新增 1）

## 自检清单（起草自检 2026-06-12）

| # | 项 | 结果 |
|---|----|------|
| 1 | PRD 覆盖度 | §8.3 审计全覆盖 + §1219 四子项（不可篡改/≥5年/TraceId/全量记录）逐项有 Task；≥5 年=append-only+无删除面（文档化，抉择⑧ 注明无 TTL 代码） |
| 2 | 安全边界 | 无 ⛔ E Task（解禁域）；§0.3 边界显式（报文签验 wiring 零触碰） |
| 3 | 占位符 | T4/T6 测试以验收断言清单形式给出（5/6 断言逐条列明，实施按清单写）——非 TBD；其余 Task 全代码 |
| 4 | 类型一致性 | AuditIntegrityService/AuditChainWriter/AuditCanonicalizer 跨 Task 引用一致 |
| 5 | 测试命令 | -Dtest 类名与新建测试匹配 + surefire3 参数 |
| 6 | CLAUDE.md | T8 Step 3 |
| 7 | 验收来源 | 架构 §1219 原文 + GB/T 向量（实现级仲裁）+ 既有代码实测，非实现倒推 |
| 8 | 共享工具 | AuditCanonicalizer（写/校验共用）/ LoginKeyPair 复用 / Sm2SignSupport 注记 |
| 9 | 职责边界 | AuditChainWriter 声明（v0.2 后依赖 4 / ~190 行，§职责边界已同步） |
| 10 | Worktree | 命中 ①⑤⑥ → wt-gm-s5；closing 含 remove 实测命令 |

## 签字区

- [x] santa Round 1: REVISE（2026-06-12，B-1 MOCK 跳过绕过 + B-2 AspectTest 漏列 + B-3 路径 + C-1~C-5 + 4N）→ v0.2 闭合
- [x] 密码学专项 Round 1: REVISE（2026-06-12，BLOCKER-1 同源 + 截断披露 + 3N；sm-crypto+BC 双端 13 断言实测）→ v0.2 闭合
- [x] santa Round 2: PASS WITH CONCERNS（Round 1 全 11 项实测闭合 + claim 命令 dry-run 双 true + ArchUnit 1.3.0 API javap 实证；2C+6N）→ v0.3 闭合
- [x] 密码学专项 Round 2: PASS WITH CONCERNS（攻击链两路断点实证；C-NEW-1 dev 域 + C-NEW-2 misconfig）→ v0.3 闭合；双路均判定无需 Round 3 全量重审
- [x] **muzhou 批准**: ✅ APPROVED 2026-06-12（AskUserQuestion 选定"批准，立即执行"；v0.3 即签字版；T7 F4 治理措辞随本签字确认）
