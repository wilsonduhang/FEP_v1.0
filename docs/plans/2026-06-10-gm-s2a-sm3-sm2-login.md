# FEP 国密 S2a 实施计划 — SM3 摘要 + SM2 登录解密/公钥分发 + KeyServiceImpl SM2 登录密钥加载

> **版本:** v0.3（起草 2026-06-10；Round 1 双 REVISE → v0.2 闭合 B-1/B-2 + 全部 CONCERN boil-lake 加固 → Round 2 复核全项闭合、仅余 4 处 32918.4 归属残留 + 3 NIT → v0.3 机械修正收口。两评审员均判定无需 Round 3。待 muzhou 签字）
>
> **v0.2 修订记录（Round 1 闭合）:**
> - **B-1**: SM3 "abc" 向量字面值修正（3 处）——原值为网络传抄变体；真值经 Round 1 两评审员独立实现（OpenSSL 3 `dgst -sm3` + 手写规格实现）+ 本机 `node sm-crypto sm3('abc')` 三方仲裁
> - **B-2**: `CallbackCredentialEncryptionFacadeTest.java:119` 匿名 `new KeyService()` 补列入 T2（接口扩展编译破坏点）；文件结构表 20→21；T4 验收 #5 改"生产行为不变"
> - **C-1**: baseline 重 stamp `709a0907` → `f22763f5`（PR #76 §5.8 R2，实测与本 Plan 触碰文件零交集）
> - **C-2**: T2 Step 4 补 `test-compile -pl fep-web -am`（B-2 最早闸口）
> - **C-3**: T4 IT 补 `LoginVerifier.resolveClearPassword` 真链路用例（对应验收 #2）
> - **C-4(crypto)**: 私钥启动校验补 hex 字符集 + 1≤d≤n-1 范围检查（+对应负例测试）
> - **C-2(crypto)**: SM2 向量出处部号修正 32918.4-2016 → **GB/T 32918.5-2017 附录 A**（推荐曲线示例；32918.4 自身附录用旧示例曲线；密文结构 C1∥C3∥C2 定义仍属 32918.4-2016）
> - **C-3(crypto)/N-2**: prod yml 样板改字面 map key（YAML key 不解析占位符）+ 对齐 sm4 块激活态形态
> - **N-1/N-3/N-5**: 删测试样例未用 import；行号修正（KeyServiceImpl L91-104 / AuthController L156）；泄漏断言补 cause 链
>
> **v0.3 修订记录（Round 2 闭合）:**
> - 4 处 32918.4 归属残留机械修正（文件结构表 / FepSecuritySm2Properties Javadoc 跨行 / T4 IT Javadoc / PR body）——余下 `32918.4` 仅指密文结构 C1∥C3∥C2 定义归属（合法保留）
> - 公钥启动校验改 `04[0-9a-fA-F]{128}` regex（补字符集语义）；Sm2LoginCipher 删除随之无引用的 2 个长度常量
> - T3 Step 3 标注 Sm2LoginCipher import 增量（SM2Engine/ECPrivateKeyParameters）
> - prod yml `login-active-key-id` 对齐 sm4 块 env+default 形态；map-key 字面值注释移位至 key 行
> **执行方式:** hybrid（红线 `feedback_harness_bg_detach_hybrid_default`）— 主对话实施 edits + 前台 mvn + commit；subagent 仅派发 spec review + quality review + 密码学专项 review。步骤使用 `- [ ]` 复选框跟踪。

**目标:** 实装 roadmap Phase A S2a：① SM3 摘要能力（`HashService`，FileContentHash §3.3.2 的算法基座）② KeyServiceImpl SM2 登录密钥加载（多版本）③ SM2 登录密码解密真实化（`decryptLoginPassword`，BC 进程内）④ SM2 公钥分发真实化（`getSm2PublicKeyBase64` + AuthController `/api/v1/auth/public-key`）。**全部 §0.3 豁免项**（roadmap §3.1 实测：§0.3 决策门只卡 S2b SM2 报文签验；登录 SM2 解密 + SM3 纯哈希明文豁免）。

**前置依赖:** S0 ✅（PR #70）+ S1 ✅（PR #73，KeyServiceImpl SM4 框架 + GmSecurityConfiguration provider 开关）+ S4 ✅（PR #75，DesensitizeConfiguration always-on 范式）。baseline origin/main = `f22763f5`（2026-06-10 Round 1 修订轮实测；PR #76 §5.8 R2 与本 Plan 触碰文件零交集；红线 `feedback_baseline_drift_during_long_review_cycle`：签字/实施各轮须重测）。

**执行 Worktree:** `/Users/muzhou/FEP_v1.0_wt-gm-s2a`（分支 `feat/gm-s2a-sm3-sm2-login`，触发条件第 ①⑤⑥ 项：跨 4 个 Maven 模块 / 全 reactor verify >5min / 多会话并发活跃——实测 `wt-rule-master-r2` + `wt-simplify-q-drain` + `.e2e` 别会话 worktree 在场）
> worktree 建立后第一步：将本签字 Plan 复制入 `docs/plans/` 并 `git add` 提交（沿用 §5.8 / S1 / S4 范式）

**架构:** 全部国密原语收敛在 `fep-security-impl`（ArchUnit R1: BouncyCastle 仅 `security.impl..` 包），经接口 `fep-security-api` 暴露。SM3 哈希无密钥材料 → **always-on @Bean 装配**（镜像 S4 `DesensitizeConfiguration`）；SM2 登录密钥 → **provider=impl 门控装配**（沿用 S1 `GmSecurityConfiguration`，红线 `feedback_provider_switch_impl_no_stereotype_bean_registration`：实现类无 stereotype）。SM2 解密用 BC lightweight API（`SM2Engine` C1C3C2），不走 JCA KeyFactory。

**技术栈:** Java 17 / Spring Boot 3.x / Maven / BouncyCastle `org.bouncycastle:bcprov-jdk18on:1.84`（root pom L153-159 实测，版本继承 dependencyManagement）

**🔓 国密授权声明:** 2026-06-07 muzhou 授权 AI 进入国密安全域开发。评审 = santa 双审 + **密码学专项 review**（GB/T 对照 + 测试向量 + 算法参数）+ muzhou 签字。真实密钥材料永不入 repo；本 Plan 全部密钥字面值为 **GB/T 32918.5-2017 附录 A 公开标准测试向量**（推荐曲线 sm2p256v1 示例，公开标准数据，非生产密钥）。

**AI 协同模式:**

| 模式 | AI 占比 | 本计划中的应用 |
|:----:|:-------:|--------------|
| A | 90% | T5 文档/收尾 |
| B | 70% | T1-T4 国密实现 + 测试（🔓 解禁域，密码学专项 review 强制） |
| E | 0%  | （本 Plan 无 ⛔ Task——S2b 报文签验/getSignPrivateKey 不在范围） |

---

## 设计背景（实测依据，2026-06-10 grep @ `709a0907`，Round 1/2 评审于 `f22763f5` 双点复核）

### PRD / roadmap 依据

| 来源 | 内容 |
|------|------|
| PRD v1.3 §3.3.2（L621-627） | 加签流程 step 2：携带文件 → 计算文件 SM3 散列值 → 填入 `<FileContentHash>` |
| PRD v1.3 §3.2.2（L588） | `FileContentHash` 字段：Hex / ..64 / 可选 / "文件内容 SM3 散列值" |
| PRD v1.3 §3.3.3（L631） | 验签流程 step 1：按 SrcNode 定位发起方公钥；step 4：SM3 比对（→ 验签 wiring 属 S2b，SM3 算法能力属本 Plan） |
| PRD v1.3 §5.1.5（L934） | "密码传输前端加密（SM2 公钥加密）" |
| roadmap §3.1（2026-06-09 muzhou 决策） | S2a = SM3 摘要 + SM2 登录解密 + 公钥分发 + KeyServiceImpl SM2 登录密钥加载，§0.3 豁免可现在执行 |

**追溯 ID:**（session-end 自动写入 prd-traceability-matrix，红线 `feedback_session_end_prd_matrix_auto_update`）
- `FR-INFRA-GM-SM3-HASH`（新增）— SM3 摘要能力，PRD §3.3.2 + §3.2.2
- `FR-INFRA-GM-SM2-LOGIN`（新增）— SM2 登录解密 + 公钥分发 + 登录密钥加载，PRD §5.1.5 + §3.3.4（密钥对形态）

**不在本 Plan 范围**（理由明示）：
- S2b SM2 报文签名/验签（`SignServiceImpl` / `getSignPrivateKey` / OutboundSignAdapter 真实化 / FileContentHash 报文 wiring）— 🚫 被 §0.3 决策门卡（落地形态 A 外部签名验签服务器 1818 vs B 进程内 BC 待 muzhou+甲方定调）
- `provider=impl` prod cutover — gated on S2b（R7：`getSignPrivateKey` 本 Plan 后仍抛 UnsupportedOperationException）
- 密钥管理完整框架（轮换/销毁/HSM）— S3

### 现状基线（关键 API 逐字实测；起草 @ `709a0907`，Round 1 评审员于 `f22763f5` 双点复核全部命中）

| 资产 | 路径:行号 | 现状 |
|------|----------|------|
| `KeyService` 接口 | `fep-security-api/.../api/KeyService.java` L23/L30/L40/L55 | `getSm2PublicKeyBase64()` / `getKeyId()` / `decryptLoginPassword(String,String)` / `getSignPrivateKey()` |
| `KeyServiceImpl` | `fep-security-impl/.../impl/key/KeyServiceImpl.java` L91-104 | SM2 三方法抛 `UnsupportedOperationException(S2_PENDING)`；SM4 多版本已实装（L44-89） |
| `GmSecurityConfiguration` | `fep-security-impl/.../impl/GmSecurityConfiguration.java` L28-63 | `@ConditionalOnProperty(provider=impl)` + `@EnableConfigurationProperties(FepSecurityKeyProperties)` + 3 @Bean |
| `DesensitizeConfiguration` | `fep-security-impl/.../impl/DesensitizeConfiguration.java` | S4 always-on @Bean 范式（无 provider 门控）——本 Plan SM3 镜像此范式 |
| `MockKeyService` | `fep-security-mock/.../mock/MockKeyService.java` L64-66 | `decryptLoginPassword` = Base64 decode 假解密；`MOCK_KEY_ID="mock-key-v1"` |
| `LoginVerifier` | `fep-web/.../auth/service/LoginVerifier.java` L60-69 | `resolveClearPassword`：encryptedPassword 非空 → `keyService.decryptLoginPassword(encryptedPassword, keyId)` |
| `AuthController.getPublicKey` | `fep-web/.../auth/controller/AuthController.java` L147-157 | `GET /api/v1/auth/public-key` → `new PublicKeyResponse(getSm2PublicKeyBase64(), getKeyId(), "SM2")` |
| 前端 `sm2-cipher.ts` | `fep-admin-ui/src/features/auth/crypto/sm2-cipher.ts` L145-152 | mode='sm2' 时 `encryptedPassword` = **sm-crypto `doEncrypt(msg, hexKey, 1)` 原始 hex，C1C3C2，无 `04` 前缀**（L147-148 注释明确"backend must accept sm-crypto-compatible C1C3C2"） |
| 前端公钥 normalize | 同上 L83-89 | `normalizePublicKey`：hex 透传 lowercase / Base64 → 原字节 hex。**不解析 X.509 SPKI DER** |
| S1 IT 范式 | `fep-web/.../callback/credential/migration/CallbackLegacyCredentialMigrationTest.java` L31-53 | `@SpringBootTest`+`@ActiveProfiles("dev")`+`@TestPropertySource(provider=impl + sm4 keys)`+`@MockBean SignService` |
| JaCoCo excludes | root `pom.xml` L266-282 | `GmSecurityConfiguration`/`DesensitizeConfiguration` 等配置类已排除——新配置类须同步加入 |
| `getKeyId()` 双消费 | AuthController L156 + `CallbackCredentialEncryptionFacade` L60/L89 | **同一 keyId 同时当 SM2 公钥版本与 SM4 凭证密钥版本用** → S2a 拆分后冲突（见抉择⑤；2026-06-10 dry-run grep 确认无第三消费点） |

### GB/T 标准测试向量（实现级仲裁核验，2026-06-10 v0.2）

**SM3（GB/T 32905-2016 附录 A；核验 = Round 1 两评审员独立实现仲裁（OpenSSL 3 `dgst -sm3` + 按算法规格手写实现）+ 本机 `node -e "require('sm-crypto').sm3('abc')"` 实测三方一致。⚠️ v0.1 的 A.1 原值为网络广泛传抄的腐烂变体（index 47 插 '3' 丢尾 '0'），文献引用不可作向量核验源，必须实现级仲裁）**

| # | 输入 | 期望摘要（hex，64 字符） |
|---|------|--------------------------|
| A.1 | `"abc"`（3 字节 UTF-8） | `66c7f0f462eeedd9d1f2d46bdc10e4e24167c4875cf2f7a2297da02b8f4ba8e0` |
| A.2 | `"abcd"` ×16（64 字节） | `debe9ff92275b8a138604889c18e5a4d6fdb70e5387e5765293dcba39c0c5732` |

**SM2（**GB/T 32918.5-2017 附录 A** 推荐曲线 sm2p256v1 加密示例——32918.4-2016 自身附录用旧 Fp-256 示例曲线，密文结构 C1∥C3∥C2 的定义属 32918.4-2016 正文；核验 = Round 1 密码学评审员独立 EC 实现完整解密实跑（[d]C1→KDF→XOR→C3 比对 GREEN）+ [dB]G 逐字节配对 + greendow/SM2-encrypt-and-decrypt C 源逐字节）**

| 项 | 值（hex） |
|----|----------|
| 明文 M | `"encryption standard"`（19 字节 ASCII） |
| 私钥 dB | `3945208f7b2144b13f36e38ac6d39f95889393692860b51a42fb81ef4df7c5b8` |
| 公钥（04∥x∥y，65 字节） | `0409f9df311e5421a150dd7d161e4bc5c672179fad1833fc076bb08ff356f35020ccea490ce26775a52dc6ea718cc1aa600aed05fbf35e084a6632f6072da9ad13` |
| C1.x | `04ebfc718e8d1798620432268e77feb6415e2ede0e073c0f4f640ecd2e149a73` |
| C1.y | `e858f9d81e5430a57b36daab8f950a3c64e6ee6a63094d99283aff767e124df0` |
| C3 | `59983c18f809e262923c53aec295d30383b54e39d609d160afcb1908d0bd8766` |
| C2 | `21886ca989ca9c7d58087307ca93092d651efa` |

**解密 KAT 输入（前端线格式 = x1∥y1∥C3∥C2，无 04 前缀，230 hex 字符）：**
```
04ebfc718e8d1798620432268e77feb6415e2ede0e073c0f4f640ecd2e149a73e858f9d81e5430a57b36daab8f950a3c64e6ee6a63094d99283aff767e124df059983c18f809e262923c53aec295d30383b54e39d609d160afcb1908d0bd876621886ca989ca9c7d58087307ca93092d651efa
```

**sm-crypto 跨实现互操作 fixture（2026-06-10 本机实测生成：`fep-admin-ui` node + sm-crypto@0.3.13 `doEncrypt(msg, pub, 1)`，self-decrypt 验证 ✓）：**
- 明文: `Sm2@LoginPwd2026`（16 字节）
- 密钥对: 上表 GB/T 标准密钥对
- 密文（hex 无 04 前缀，224 字符 = C1 64B + C3 32B + C2 16B）:
```
7f124706e5f4e8c618dd4d46bd2e52ee8d4583597b7105284f41f2098865b349613c972491f8aa8741253d09d66c31cc026f63cb8743808dc066b77da18a0da9c886117e757723bc3d3391ba6d7c5f0549da88cf94e5374ba3e06e12358a371cd5cf6ea9209ecf782d390ce6fcc1733f
```
> SM2 加密含随机 k → 此 fixture 一次生成永久冻结（确定性解密断言）；它是生产真实路径（前端 sm-crypto 产文 → 后端 BC 解密）的唯一跨实现锚点。

### 设计抉择（评审重点）

| # | 抉择 | 理由 |
|---|------|------|
| ① | SM3 = 独立 `HashService` 接口 + `HashServiceImpl` + **always-on** `GmHashConfiguration`（非 provider 门控、无 mock 对称物） | 纯哈希无密钥材料，dev/prod 同算法（镜像 S4 DesensitizeConfiguration 先例）；放 SignService 内会把无密钥能力错误绑进 §0.3 卡的 S2b；FileContentHash 报文 wiring 属加签流程 → S2b deferred |
| ② | `getSm2PublicKeyBase64()` 返回 **Base64(04∥x∥y 65 字节裸点)**，并修正 API Javadoc（原写 X.509 SubjectPublicKeyInfo） | 前端 `normalizePublicKey` 实测只做 Base64→原字节 hex，不解析 SPKI DER；返回 SPKI 会让 sm-crypto 拿到带 ASN.1 头的错误密钥材料。裸点 Base64 经前端 decode 后 = `04`+x+y hex 130 字符，sm-crypto 直接可用；`resolveMode` Base64 分支 decode 65 字节 ≥64 → mode='sm2' ✓ |
| ③ | `decryptLoginPassword` 线格式 = **严格 sm-crypto hex C1C3C2 无 04 前缀**（实现内部统一补 `0x04` 再喂 BC `SM2Engine`），不做带/不带 04 双格式探测 | 前端是唯一生产者且固定无 04（sm2-cipher.ts L147-148）；按长度/首字节探测有歧义（x1 首字节 1/256 概率 = 0x04）。接口参数改名 `encryptedBase64`→`encryptedPassword` + Javadoc 按 provider 注明线格式（impl=hex / mock=Base64 明文） |
| ④ | 新增 `FepSecuritySm2Properties`（prefix `fep.security.sm2`，独立于 SM4 的 `fep.security.sm4`）：`loginActiveKeyId` + `Map<keyId, LoginKeyPair{privateKeyHex(64), publicKeyHex(130)}>`；**配置可选** — 未配置时 SM2 登录方法抛 `IllegalStateException`（区别"未实装"的 `UnsupportedOperationException`） | 多版本镜像 S1 SM4 范式（接口 `decryptLoginPassword` 本就带 keyId 参数）；可选配置保 S1 既有 provider=impl IT（只配 sm4 props）零修改通过；私钥形态 = 32 字节标量 d hex（非 PKCS#8，BC lightweight API 直用，避免 ASN.1 解析面）；@PostConstruct 校验含 **[d]G == pub 配对一致性**（密码学级配置完整性） |
| ⑤ | 新增接口方法 `KeyService.getSm2LoginKeyId()`，AuthController 公钥端点改用之；`getKeyId()` 保持 = SM4 凭证密钥版本（CallbackCredentialEncryptionFacade 继续用） | 实测 `getKeyId()` 双消费冲突：S2a 后 SM2 登录 keyId 与 SM4 凭证 keyId 是两个命名空间，若 AuthController 继续返回 SM4 keyId，前端回传后 `decryptLoginPassword(keyId)` 查不到登录密钥 |
| ⑥ | SM2 解密原语收敛在包私有 `Sm2LoginCipher`（`impl.key` 包，无 stereotype），`KeyServiceImpl` 仅做密钥路由门面 | KeyServiceImpl 行数控制（现 105 行，+SM2 后 <300 上限）；BC lightweight API（`SM2Engine(Mode.C1C3C2)` + `ECPrivateKeyParameters` + `GMNamedCurves.getByName("sm2p256v1")`）与 JCA 隔离 |

### 共享工具类清单

| 工具类 | 包 | 关键方法 | 提供者 Task | 消费者 Task |
|---|---|---|---|---|
| `Sm2LoginCipher` | `security.impl.key`（包私有） | `isMatchingKeyPair(privHex, pubHex)` / `decryptC1C3C2(cipherHex, privHex)` | Task 2（配对校验）/ Task 3（解密） | Task 2/3 |
| `Sm2TestVectors` | `security.impl.key`（test scope） | GB/T 向量常量 + fixture 常量 | Task 2 | Task 2/3 |

> fep-web 的 T4 IT 与 fep-security-impl test util 跨模块无法共享（不引 test-jar）——T4 在 `@TestPropertySource` 内重复 3 个向量字面值，注释标注来源，属可接受重复。

### KeyServiceImpl 职责边界（修订）

**负责**: SM4/SM2 登录密钥的配置加载、版本路由、启动期校验、登录解密门面
**不负责**: SM2 算法原语（→ `Sm2LoginCipher`）/ SM2 报文签名密钥（S2b ⛔ 待 §0.3）/ 密钥轮换工作流（S3）
**依赖**: 2 个（`FepSecurityKeyProperties` + `FepSecuritySm2Properties`，上限 7 ✓）
**行数**: 实施后预计 ~230（上限 300 ✓）；超出则拆 `Sm4KeyStore`/`Sm2LoginKeyStore`

### 文件结构

| 文件路径 | 职责 | 操作 | AI 模式 |
|----------|------|------|:-------:|
| `fep-security-api/src/main/java/com/puchain/fep/security/api/HashService.java` | SM3 摘要接口 | 新建 | B |
| `fep-security-api/src/main/java/com/puchain/fep/security/api/KeyService.java` | +`getSm2LoginKeyId()`，Javadoc 修正（抉择②③⑤） | 修改 | B |
| `fep-security-impl/src/main/java/com/puchain/fep/security/impl/hash/HashServiceImpl.java` | BC SM3Digest 实现（无 stereotype） | 新建 | B |
| `fep-security-impl/src/main/java/com/puchain/fep/security/impl/GmHashConfiguration.java` | always-on @Bean 装配 | 新建 | B |
| `fep-security-impl/src/main/java/com/puchain/fep/security/impl/key/FepSecuritySm2Properties.java` | SM2 登录密钥配置绑定 | 新建 | B |
| `fep-security-impl/src/main/java/com/puchain/fep/security/impl/key/Sm2LoginCipher.java` | SM2 曲线参数 + 配对校验 + C1C3C2 解密 | 新建 | B |
| `fep-security-impl/src/main/java/com/puchain/fep/security/impl/key/KeyServiceImpl.java` | +SM2 登录密钥加载/路由/解密门面 | 修改 | B |
| `fep-security-impl/src/main/java/com/puchain/fep/security/impl/GmSecurityConfiguration.java` | @EnableConfigurationProperties 增 Sm2Properties + keyService @Bean 双参 | 修改 | B |
| `fep-security-mock/src/main/java/com/puchain/fep/security/mock/MockKeyService.java` | +`getSm2LoginKeyId()` → MOCK_KEY_ID | 修改 | A |
| `fep-web/src/test/java/com/puchain/fep/web/config/TestKeyServiceConfiguration.java` | 匿名类 +`getSm2LoginKeyId()` | 修改 | A |
| `fep-web/src/test/.../callback/credential/crypto/CallbackCredentialEncryptionFacadeTest.java` | L119 匿名 KeyService +`getSm2LoginKeyId()`（B-2） | 修改 | A |
| `fep-web/src/main/java/com/puchain/fep/web/auth/controller/AuthController.java` | 公钥端点 keyId 改用 `getSm2LoginKeyId()` | 修改 | B |
| `fep-security-impl/src/test/java/com/puchain/fep/security/impl/hash/HashServiceImplTest.java` | GB/T 32905 KAT | 新建 | B |
| `fep-security-impl/src/test/java/com/puchain/fep/security/impl/key/Sm2TestVectors.java` | 向量常量（test util） | 新建 | B |
| `fep-security-impl/src/test/java/com/puchain/fep/security/impl/key/Sm2LoginCipherTest.java` | GB/T 32918.5 KAT + fixture + roundtrip + 边界 | 新建 | B |
| `fep-security-impl/src/test/java/com/puchain/fep/security/impl/key/KeyServiceImplTest.java` | S2 边界测试改造 + SM2 加载测试 | 修改 | B |
| `fep-security-mock/src/test/java/com/puchain/fep/security/mock/MockKeyServiceTest.java` | +getSm2LoginKeyId 断言 | 修改 | A |
| `fep-web/src/test/java/com/puchain/fep/web/auth/controller/AuthControllerTest.java` | mock stub 更新 | 修改 | A |
| `fep-web/src/test/java/com/puchain/fep/web/auth/service/Sm2LoginDecryptionProviderImplTest.java` | provider=impl 全 context IT（命名 `*Test` 防 Surefire 静默跳过） | 新建 | B |
| root `pom.xml` | JaCoCo excludes +GmHashConfiguration | 修改 | A |
| `fep-web/src/main/resources/application-prod.yml` | sm2 登录密钥 env 注入注释样板 | 修改 | A |

---

## Task 0: Worktree 建立 + 签字 Plan 入库 `模式 A`

- [ ] **Step 1: 4 时点重测并发状态**（红线 `feedback_worktree_trigger_is_dynamic_recheck_at_execution`）

```bash
cd /Users/muzhou/FEP_v1.0 && git fetch && git rev-parse main origin/main && git worktree list && pgrep -fl "mvn|mvnw" || true
```
期望: main == origin/main（不等则先 ff）；记录别会话活跃 worktree。

- [ ] **Step 2: 建 worktree**

```bash
cd /Users/muzhou/FEP_v1.0 && git worktree add -b feat/gm-s2a-sm3-sm2-login /Users/muzhou/FEP_v1.0_wt-gm-s2a origin/main
```

- [ ] **Step 3: 签字 Plan 入库（第一 commit）**

```bash
cp /Users/muzhou/FEP/docs/plans/2026-06-10-gm-s2a-sm3-sm2-login.md /Users/muzhou/FEP_v1.0_wt-gm-s2a/docs/plans/
cd /Users/muzhou/FEP_v1.0_wt-gm-s2a && git add docs/plans/2026-06-10-gm-s2a-sm3-sm2-login.md && git commit -m "$(cat <<'EOF'
docs(plans): add signed GM S2a plan (SM3 hash + SM2 login decrypt)

AI-Generated: claude-code
Reviewed-By: muzhou
EOF
)"
```

---

## Task 1: HashService SM3 摘要（api + impl + always-on 装配） `模式 B`

**PRD 依据:** v1.3 §3.3.2 加签流程 step 2 + §3.2.2 FileContentHash（Hex ..64）
**追溯 ID:** FR-INFRA-GM-SM3-HASH

**验收标准（从 PRD/GB/T 推导）:**
1. `sm3Hex("abc".getBytes(UTF_8))` == GB/T 32905 A.1 向量（64 hex 字符，小写）
2. `sm3Hex(64 字节 "abcd"×16)` == GB/T 32905 A.2 向量
3. `sm3Hex(null)` → `IllegalArgumentException`
4. 任意输入输出恒为 64 个 `[0-9a-f]` 字符（FileContentHash XSD Hex ..64 兼容）
5. Spring 容器中 `HashService` bean **always-on**（provider=mock 与 =impl 均装配，单 bean）

**Files:**
- Create: `fep-security-api/src/main/java/com/puchain/fep/security/api/HashService.java`
- Create: `fep-security-impl/src/main/java/com/puchain/fep/security/impl/hash/HashServiceImpl.java`
- Create: `fep-security-impl/src/main/java/com/puchain/fep/security/impl/GmHashConfiguration.java`
- Create: `fep-security-impl/src/test/java/com/puchain/fep/security/impl/hash/HashServiceImplTest.java`
- Modify: root `pom.xml`（JaCoCo excludes + `**/GmHashConfiguration.class`）

- [ ] **Step 1: 编写失败测试**

```java
// fep-security-impl/src/test/java/com/puchain/fep/security/impl/hash/HashServiceImplTest.java
package com.puchain.fep.security.impl.hash;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/**
 * SM3 摘要 GB/T 32905-2016 附录 A 标准向量逐字节验证。
 */
class HashServiceImplTest {

    private final HashServiceImpl hashService = new HashServiceImpl();

    @Test
    void sm3Hex_abcVector_matchesGbt32905AppendixA1() {
        assertThat(hashService.sm3Hex("abc".getBytes(StandardCharsets.UTF_8)))
                .isEqualTo("66c7f0f462eeedd9d1f2d46bdc10e4e24167c4875cf2f7a2297da02b8f4ba8e0");
    }

    @Test
    void sm3Hex_abcd16Vector_matchesGbt32905AppendixA2() {
        assertThat(hashService.sm3Hex("abcd".repeat(16).getBytes(StandardCharsets.UTF_8)))
                .isEqualTo("debe9ff92275b8a138604889c18e5a4d6fdb70e5387e5765293dcba39c0c5732");
    }

    @Test
    void sm3Hex_nullData_throwsIllegalArgument() {
        assertThatThrownBy(() -> hashService.sm3Hex(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void sm3Hex_emptyData_returns64LowercaseHexChars() {
        assertThat(hashService.sm3Hex(new byte[0])).matches("[0-9a-f]{64}");
    }
}
```

- [ ] **Step 2: 运行确认失败**

```bash
cd /Users/muzhou/FEP_v1.0_wt-gm-s2a && ./mvnw test -pl fep-security-impl -am -Dtest=HashServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false
```
期望: 编译失败 — `cannot find symbol: class HashServiceImpl`

- [ ] **Step 3: 接口 + 实现 + 装配**

```java
// fep-security-api/src/main/java/com/puchain/fep/security/api/HashService.java
package com.puchain.fep.security.api;

/**
 * 国密 SM3 摘要服务（GB/T 32905-2016）。
 *
 * <p>报文携带文件场景的 {@code FileContentHash}（PRD §3.3.2 / §3.2.2，Hex ..64）算法基座。
 * 纯哈希无密钥材料，always-on 装配（不随 {@code fep.security.provider} 切换）。
 * 报文加签/验签流程的 FileContentHash 填充与比对 wiring 属 S2b。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public interface HashService {

    /**
     * 计算 SM3 摘要。
     *
     * @param data 待摘要数据，非 null（可为空数组）
     * @return 64 字符小写十六进制摘要（与 DataType.xsd Hex 类型兼容）
     * @throws IllegalArgumentException data 为 null
     */
    String sm3Hex(byte[] data);
}
```

```java
// fep-security-impl/src/main/java/com/puchain/fep/security/impl/hash/HashServiceImpl.java
package com.puchain.fep.security.impl.hash;

import com.puchain.fep.security.api.HashService;
import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.util.encoders.Hex;

/**
 * SM3 摘要实现（BouncyCastle lightweight API，GB/T 32905-2016）。
 *
 * <p>无 Spring stereotype，经 {@code GmHashConfiguration @Bean} 注册
 * （红线 feedback_provider_switch_impl_no_stereotype_bean_registration）。
 * lightweight {@link SM3Digest} 不依赖 JCA provider 注册，线程安全由每次调用新建 digest 保证。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public class HashServiceImpl implements HashService {

    @Override
    public String sm3Hex(final byte[] data) {
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }
        final SM3Digest digest = new SM3Digest();
        digest.update(data, 0, data.length);
        final byte[] out = new byte[digest.getDigestSize()];
        digest.doFinal(out, 0);
        return Hex.toHexString(out);
    }
}
```

```java
// fep-security-impl/src/main/java/com/puchain/fep/security/impl/GmHashConfiguration.java
package com.puchain.fep.security.impl;

import com.puchain.fep.security.api.HashService;
import com.puchain.fep.security.impl.hash.HashServiceImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SM3 摘要装配 — always-on（纯哈希无密钥/无 mock/无 provider 之分，镜像 DesensitizeConfiguration）。
 *
 * <p>实现类 {@link HashServiceImpl} 无 Spring stereotype，经本类 {@code @Bean} 注册。
 * 不带 @ConditionalOnProperty——SM3 摘要 dev/prod 同算法始终启用。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Configuration
public class GmHashConfiguration {

    /**
     * SM3 摘要服务（always-on 单例）。
     *
     * @return HashService 实现
     */
    @Bean
    public HashService hashService() {
        return new HashServiceImpl();
    }
}
```

root `pom.xml` JaCoCo excludes（紧邻既有 `**/DesensitizeConfiguration.class` 行后插入）：
```xml
<exclude>**/GmHashConfiguration.class</exclude>
```

- [ ] **Step 4: 测试通过 + 门禁**

```bash
cd /Users/muzhou/FEP_v1.0_wt-gm-s2a && ./mvnw test -pl fep-security-impl -am -Dtest=HashServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false
cd /Users/muzhou/FEP_v1.0_wt-gm-s2a && ./mvnw compile spotbugs:check -pl fep-security-api,fep-security-impl -am
```
期望: 4 tests passed；`BugInstance size 0`（红线 `feedback_spotbugs_check_needs_recompile_after_annotation`：compile 与 check 同命令）

- [ ] **Step 5: 提交**

```bash
cd /Users/muzhou/FEP_v1.0_wt-gm-s2a && git add fep-security-api/src fep-security-impl/src pom.xml && git commit -m "$(cat <<'EOF'
feat(security): add SM3 HashService with GB/T 32905 vectors (GM S2a T1)

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

- [ ] **Step 6: 派发 spec review + quality review subagent**（红线 `feedback_task_review_discipline`）

---

## Task 2: SM2 登录密钥加载（properties + 接口扩展 + KeyServiceImpl） `模式 B`

**PRD 依据:** v1.3 §5.1.5 密码传输前端加密 + §3.3.4 SM2 密钥对形态
**追溯 ID:** FR-INFRA-GM-SM2-LOGIN

**验收标准:**
1. 配置 `fep.security.sm2.login-active-key-id` + `login-keys.<id>.{private-key-hex,public-key-hex}` 后：`getSm2LoginKeyId()` 返回 activeId；`getSm2PublicKeyBase64()` 返回 Base64(65 字节裸点)，Base64-decode 后首字节 `0x04`、长 65
2. 未配置 SM2 段时：`getSm2PublicKeyBase64()`/`getSm2LoginKeyId()`/`decryptLoginPassword(..)` 抛 `IllegalStateException`（含 "not configured"）；`getSignPrivateKey()` 仍抛 `UnsupportedOperationException`（S2b 边界）
3. 启动校验：activeId 不在 map / 私钥非 64 hex 字符（`[0-9a-fA-F]{64}`）/ **私钥标量越界（d≤0 或 d≥n）** / 公钥非 130 hex 或不以 `04` 开头 / **[d]G ≠ 公钥点** → `IllegalStateException`
4. GB/T 标准密钥对（dB ↔ 04∥x∥y）通过 [d]G 配对校验；私钥不变公钥末位篡改 → 启动失败；全零私钥（d=0）→ 启动失败
5. S1 既有 `CallbackLegacyCredentialMigrationTest`（只配 sm4 props）不修改且保持 GREEN；`CallbackCredentialEncryptionFacadeTest` 匿名 KeyService 补 1 个 override 后既有断言零修改 GREEN
6. T2 commit 点 fep-web **test sources** 编译 GREEN（接口扩展的全部实现点同 commit 闭合）

**Files:**
- Create: `fep-security-impl/.../impl/key/FepSecuritySm2Properties.java`
- Create: `fep-security-impl/.../impl/key/Sm2LoginCipher.java`（本 Task 仅曲线参数 + 配对校验）
- Create: `fep-security-impl/src/test/.../impl/key/Sm2TestVectors.java`
- Modify: `fep-security-api/.../api/KeyService.java`（+`getSm2LoginKeyId()` + Javadoc 修正 + 参数改名）
- Modify: `fep-security-impl/.../impl/key/KeyServiceImpl.java`
- Modify: `fep-security-impl/.../impl/GmSecurityConfiguration.java`
- Modify: `fep-security-mock/.../mock/MockKeyService.java` + `MockKeyServiceTest.java`
- Modify: `fep-web/src/test/.../config/TestKeyServiceConfiguration.java`
- Modify: `fep-web/src/test/.../callback/credential/crypto/CallbackCredentialEncryptionFacadeTest.java`（L119 `perKeyIdKeyService()` 匿名 `new KeyService()` 补 override——接口扩展编译破坏点，Round 1 B-2）
- Modify: `fep-security-impl/src/test/.../impl/key/KeyServiceImplTest.java`（S2 边界测试改造，红线 `feedback_obsolete_negative_test_cleanup` 同 commit）

- [ ] **Step 1: 测试向量 util + 失败测试**

```java
// fep-security-impl/src/test/java/com/puchain/fep/security/impl/key/Sm2TestVectors.java
package com.puchain.fep.security.impl.key;

/**
 * GB/T 32918.5-2017 附录 A 推荐曲线（sm2p256v1）标准测试向量（公开标准数据，非生产密钥）+
 * sm-crypto@0.3.13 跨实现互操作 fixture（2026-06-10 生成，见 Plan §设计背景）。
 */
final class Sm2TestVectors {

    /** GB/T 32918.5-2017 附录 A 私钥 dB（32 字节标量 hex）。 */
    static final String GBT_PRIVATE_KEY_HEX =
            "3945208f7b2144b13f36e38ac6d39f95889393692860b51a42fb81ef4df7c5b8";

    /** GB/T 32918.5-2017 附录 A 公钥（04∥x∥y 130 hex）。 */
    static final String GBT_PUBLIC_KEY_HEX =
            "0409f9df311e5421a150dd7d161e4bc5c672179fad1833fc076bb08ff356f35020"
                    + "ccea490ce26775a52dc6ea718cc1aa600aed05fbf35e084a6632f6072da9ad13";

    /** GB/T 32918.5-2017 附录 A 密文（C1C3C2，前端线格式无 04 前缀），明文 "encryption standard"。 */
    static final String GBT_CIPHER_C1C3C2_NO_PREFIX_HEX =
            "04ebfc718e8d1798620432268e77feb6415e2ede0e073c0f4f640ecd2e149a73"
                    + "e858f9d81e5430a57b36daab8f950a3c64e6ee6a63094d99283aff767e124df0"
                    + "59983c18f809e262923c53aec295d30383b54e39d609d160afcb1908d0bd8766"
                    + "21886ca989ca9c7d58087307ca93092d651efa";

    /** GB/T 32918.5-2017 附录 A 明文。 */
    static final String GBT_PLAINTEXT = "encryption standard";

    /** sm-crypto@0.3.13 doEncrypt(msg, GBT_PUBLIC, cipherMode=1) 实测 fixture，明文 Sm2@LoginPwd2026。 */
    static final String SM_CRYPTO_FIXTURE_CIPHER_HEX =
            "7f124706e5f4e8c618dd4d46bd2e52ee8d4583597b7105284f41f2098865b349"
                    + "613c972491f8aa8741253d09d66c31cc026f63cb8743808dc066b77da18a0da9"
                    + "c886117e757723bc3d3391ba6d7c5f0549da88cf94e5374ba3e06e12358a371c"
                    + "d5cf6ea9209ecf782d390ce6fcc1733f";

    /** sm-crypto fixture 明文。 */
    static final String SM_CRYPTO_FIXTURE_PLAINTEXT = "Sm2@LoginPwd2026";

    private Sm2TestVectors() {
    }
}
```

`KeyServiceImplTest` 新增/改造（关键用例；既有 SM4 用例不动）：

```java
// 改造既有 sm2Methods_throwUnsupportedForS2Boundary →
@Test
void sm2LoginMethods_withoutSm2Config_throwIllegalState_andSignKeyStaysS2b() {
    final KeyService svc = newService("sm4-cred-v2", keys("sm4-cred-v2", GBT_KEY_HEX));
    assertThatThrownBy(svc::getSm2PublicKeyBase64)
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("not configured");
    assertThatThrownBy(svc::getSm2LoginKeyId)
            .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(() -> svc.decryptLoginPassword("00", "any"))
            .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(svc::getSignPrivateKey)
            .isInstanceOf(UnsupportedOperationException.class);
}

@Test
void sm2LoginConfig_valid_exposesActiveKeyIdAndRawPointBase64() {
    final KeyService svc = newServiceWithSm2("sm2-login-v1",
            Sm2TestVectors.GBT_PRIVATE_KEY_HEX, Sm2TestVectors.GBT_PUBLIC_KEY_HEX);
    assertThat(svc.getSm2LoginKeyId()).isEqualTo("sm2-login-v1");
    final byte[] point = java.util.Base64.getDecoder().decode(svc.getSm2PublicKeyBase64());
    assertThat(point).hasSize(65);
    assertThat(point[0]).isEqualTo((byte) 0x04);
}

@Test
void sm2LoginConfig_mismatchedKeyPair_failsStartup() {
    // 公钥末位 13 → 14（[d]G 配对校验必须抓住）
    final String tampered = Sm2TestVectors.GBT_PUBLIC_KEY_HEX
            .substring(0, Sm2TestVectors.GBT_PUBLIC_KEY_HEX.length() - 2) + "14";
    assertThatThrownBy(() -> newServiceWithSm2("sm2-login-v1",
            Sm2TestVectors.GBT_PRIVATE_KEY_HEX, tampered))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("key pair");
}

@Test
void sm2LoginConfig_activeIdNotInMap_failsStartup() {
    assertThatThrownBy(() -> newServiceWithSm2Raw("missing-id", "sm2-login-v1",
            Sm2TestVectors.GBT_PRIVATE_KEY_HEX, Sm2TestVectors.GBT_PUBLIC_KEY_HEX))
            .isInstanceOf(IllegalStateException.class);
}

@Test
void sm2LoginConfig_privateKeyZeroScalar_failsStartup() {
    assertThatThrownBy(() -> newServiceWithSm2("sm2-login-v1",
            "00".repeat(32), Sm2TestVectors.GBT_PUBLIC_KEY_HEX))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("out of range");
}
```

测试助手（加入既有 `newService`/`keys` 旁）：

```java
private static KeyService newServiceWithSm2(final String keyId,
        final String privHex, final String pubHex) {
    return newServiceWithSm2Raw(keyId, keyId, privHex, pubHex);
}

private static KeyService newServiceWithSm2Raw(final String activeId, final String keyId,
        final String privHex, final String pubHex) {
    final FepSecuritySm2Properties sm2 = new FepSecuritySm2Properties();
    sm2.setLoginActiveKeyId(activeId);
    final FepSecuritySm2Properties.LoginKeyPair pair = new FepSecuritySm2Properties.LoginKeyPair();
    pair.setPrivateKeyHex(privHex);
    pair.setPublicKeyHex(pubHex);
    sm2.getLoginKeys().put(keyId, pair);
    final FepSecurityKeyProperties sm4 = new FepSecurityKeyProperties();
    sm4.setActiveKeyId("sm4-cred-v2");
    sm4.getSm4Keys().put("sm4-cred-v2", GBT_KEY_HEX);
    final KeyServiceImpl svc = new KeyServiceImpl(sm4, sm2);
    svc.validateOnStartup();
    return svc;
}
```
> 既有 `newService(..)` 助手改为内部委托 `new KeyServiceImpl(props, new FepSecuritySm2Properties())`（SM2 空配置 = 未配置语义），既有 SM4 用例零改动。

- [ ] **Step 2: 运行确认失败**

```bash
cd /Users/muzhou/FEP_v1.0_wt-gm-s2a && ./mvnw test -pl fep-security-impl -am -Dtest=KeyServiceImplTest -Dsurefire.failIfNoSpecifiedTests=false
```
期望: 编译失败 — `cannot find symbol: FepSecuritySm2Properties`

- [ ] **Step 3: 最小实现**

```java
// fep-security-impl/src/main/java/com/puchain/fep/security/impl/key/FepSecuritySm2Properties.java
package com.puchain.fep.security.impl.key;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * SM2 登录密钥多版本配置（{@code fep.security.sm2.*}）。
 *
 * <p>真实密钥生产期经 env/sealed store 注入，永不入 repo；dev/CI 用 GB/T 32918.5-2017
 * 附录 A 公开标准测试密钥对。私钥为 32 字节标量 d 的 hex（64 字符），公钥为
 * 未压缩裸点 04∥x∥y 的 hex（130 字符）。配置整段可选——未配置时 SM2 登录方法
 * 抛 IllegalStateException（KeyServiceImpl）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "fep.security.sm2")
public class FepSecuritySm2Properties {

    /** 当前活跃登录密钥版本号（公钥分发与新登录解密使用）。 */
    private String loginActiveKeyId;

    /** keyId → 登录密钥对（多版本，轮换期共存）。 */
    private Map<String, LoginKeyPair> loginKeys = new LinkedHashMap<>();

    /**
     * @return 活跃登录密钥版本号
     */
    public String getLoginActiveKeyId() {
        return loginActiveKeyId;
    }

    /**
     * @param loginActiveKeyId 活跃登录密钥版本号
     */
    public void setLoginActiveKeyId(final String loginActiveKeyId) {
        this.loginActiveKeyId = loginActiveKeyId;
    }

    /**
     * Spring relaxed binding 需 live 引用填充（红线
     * feedback_configurationproperties_collection_getter_ei_expose）；
     * 下游 KeyServiceImpl 构造期拷贝，无 live 泄漏。
     *
     * @return keyId → 登录密钥对 map（live 引用）
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP",
            justification = "Spring relaxed binding mutates via live getter; "
                    + "KeyServiceImpl copies on construction, no live reference escapes")
    public Map<String, LoginKeyPair> getLoginKeys() {
        return loginKeys;
    }

    /**
     * @param loginKeys keyId → 登录密钥对（防御拷贝）
     */
    public void setLoginKeys(final Map<String, LoginKeyPair> loginKeys) {
        this.loginKeys = new LinkedHashMap<>(loginKeys);
    }

    /**
     * 单版本 SM2 登录密钥对。
     */
    public static class LoginKeyPair {

        /** 私钥标量 d（hex 64 字符）。 */
        private String privateKeyHex;

        /** 公钥未压缩裸点 04∥x∥y（hex 130 字符）。 */
        private String publicKeyHex;

        /**
         * @return 私钥 hex
         */
        public String getPrivateKeyHex() {
            return privateKeyHex;
        }

        /**
         * @param privateKeyHex 私钥 hex
         */
        public void setPrivateKeyHex(final String privateKeyHex) {
            this.privateKeyHex = privateKeyHex;
        }

        /**
         * @return 公钥 hex
         */
        public String getPublicKeyHex() {
            return publicKeyHex;
        }

        /**
         * @param publicKeyHex 公钥 hex
         */
        public void setPublicKeyHex(final String publicKeyHex) {
            this.publicKeyHex = publicKeyHex;
        }
    }
}
```

```java
// fep-security-impl/src/main/java/com/puchain/fep/security/impl/key/Sm2LoginCipher.java
package com.puchain.fep.security.impl.key;

import java.math.BigInteger;
import org.bouncycastle.asn1.gm.GMNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.encoders.Hex;

/**
 * SM2 登录解密原语（BouncyCastle lightweight API，曲线 sm2p256v1，GB/T 32918）。
 *
 * <p>包私有：仅 KeyServiceImpl 经由本类触达 BC SM2 原语（ArchUnit R1：BC 仅
 * security.impl 包）。无 Spring stereotype。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
final class Sm2LoginCipher {

    /** SM2 推荐曲线参数（GB/T 32918.5）。 */
    private static final X9ECParameters SM2_X9 = GMNamedCurves.getByName("sm2p256v1");

    /** SM2 椭圆曲线 domain 参数。 */
    static final ECDomainParameters DOMAIN = new ECDomainParameters(
            SM2_X9.getCurve(), SM2_X9.getG(), SM2_X9.getN(), SM2_X9.getH());

    private Sm2LoginCipher() {
    }

    /**
     * 校验 [d]G 与公钥点配对一致（启动期配置完整性）。
     *
     * @param privateKeyHex 私钥标量 hex（64 字符）
     * @param publicKeyHex  公钥裸点 hex（130 字符，04 开头）
     * @return 配对一致返回 true
     */
    static boolean isMatchingKeyPair(final String privateKeyHex, final String publicKeyHex) {
        final BigInteger d = new BigInteger(privateKeyHex, 16);
        final ECPoint derived = DOMAIN.getG().multiply(d).normalize();
        return Hex.toHexString(derived.getEncoded(false)).equalsIgnoreCase(publicKeyHex);
    }
}
```

`KeyService` 接口（修改——新增方法 + Javadoc 修正；既有方法签名仅参数改名）：

```java
/**
 * 当前活跃 SM2 登录密钥版本号。
 *
 * <p>与 {@link #getSm2PublicKeyBase64()} 公钥配对，由公钥分发端点随公钥下发、
 * 登录请求回传用于 {@link #decryptLoginPassword} 版本路由。区别于
 * {@link #getKeyId()}（SM4 凭证主密钥版本，回调凭证加密用）。</p>
 *
 * @return SM2 登录密钥版本号
 * @throws IllegalStateException impl provider 下 SM2 登录密钥未配置
 */
String getSm2LoginKeyId();
```
> 同步修正既有 Javadoc：`getSm2PublicKeyBase64()` 注明返回 **Base64(65 字节未压缩裸点 04∥x∥y)**（原 X.509 SubjectPublicKeyInfo 描述与前端 sm2-cipher.ts normalize 实测不兼容，抉择②）；`decryptLoginPassword(String encryptedPassword, String keyId)` 参数改名并注明线格式按 provider：impl = sm-crypto hex C1C3C2 无 04 前缀（fep-admin-ui sm2-cipher.ts 契约）/ mock = Base64(明文)。

`KeyServiceImpl`（修改——新增字段/构造参数/校验/三方法；SM4 逻辑零改动）：

```java
/** S2b 边界提示（仅余报文签名私钥）。 */
private static final String S2B_PENDING =
        "SM2 message-sign key operations are pending S2b (roadmap §0.3 sign-verify form decision)";

/** SM2 登录密钥未配置提示。 */
private static final String SM2_LOGIN_NOT_CONFIGURED =
        "SM2 login keys not configured (fep.security.sm2.login-active-key-id / login-keys)";

private final String loginActiveKeyId;
private final Map<String, FepSecuritySm2Properties.LoginKeyPair> loginKeys;

/**
 * 从配置构造：SM4 hex 密钥解码 + SM2 登录密钥对拷贝。
 *
 * @param props    SM4 密钥配置，非 null
 * @param sm2Props SM2 登录密钥配置，非 null（内容可为空 = 未配置）
 */
public KeyServiceImpl(final FepSecurityKeyProperties props,
                      final FepSecuritySm2Properties sm2Props) {
    Objects.requireNonNull(props, "props");
    Objects.requireNonNull(sm2Props, "sm2Props");
    this.activeKeyId = props.getActiveKeyId();
    final Map<String, byte[]> decoded = new LinkedHashMap<>();
    props.getSm4Keys().forEach((keyId, hex) ->
            decoded.put(keyId, HexFormat.of().parseHex(hex)));
    this.keysByVersion = decoded;
    this.loginActiveKeyId = sm2Props.getLoginActiveKeyId();
    this.loginKeys = new LinkedHashMap<>(sm2Props.getLoginKeys());
}
```

`validateOnStartup()` 追加（既有 SM4 校验后）：

```java
if (loginActiveKeyId != null || !loginKeys.isEmpty()) {
    if (loginActiveKeyId == null || !loginKeys.containsKey(loginActiveKeyId)) {
        throw new IllegalStateException("fep.security.sm2.login-active-key-id ["
                + loginActiveKeyId + "] not present in loginKeys");
    }
    loginKeys.forEach((keyId, pair) -> {
        final String priv = pair.getPrivateKeyHex();
        final String pub = pair.getPublicKeyHex();
        if (priv == null || !priv.matches("[0-9a-fA-F]{64}")) {
            throw new IllegalStateException("SM2 login private key [" + keyId
                    + "] must be 64 hex chars (32-byte scalar)");
        }
        final java.math.BigInteger d = new java.math.BigInteger(priv, 16);
        if (d.signum() <= 0 || d.compareTo(Sm2LoginCipher.DOMAIN.getN()) >= 0) {
            throw new IllegalStateException("SM2 login private key [" + keyId
                    + "] scalar out of range (require 1 <= d <= n-1)");
        }
        if (pub == null || !pub.matches("04[0-9a-fA-F]{128}")) {
            throw new IllegalStateException("SM2 login public key [" + keyId
                    + "] must be 130 hex chars starting with 04 (uncompressed point)");
        }
        if (!Sm2LoginCipher.isMatchingKeyPair(priv, pub)) {
            throw new IllegalStateException("SM2 login key pair [" + keyId
                    + "] mismatch: [d]G does not equal configured public key");
        }
    });
}
```

三方法实装（`decryptLoginPassword` 本 Task 仅密钥路由门面，解密原语 Task 3 补齐——本 Task 暂委托 `Sm2LoginCipher.decryptC1C3C2`，方法在 Task 3 创建前以编译需要先建签名见 Step 3 末注）：

```java
@Override
public String getSm2PublicKeyBase64() {
    final FepSecuritySm2Properties.LoginKeyPair active = requireLoginKey(requireLoginConfigured());
    return java.util.Base64.getEncoder()
            .encodeToString(HexFormat.of().parseHex(active.getPublicKeyHex()));
}

@Override
public String getSm2LoginKeyId() {
    return requireLoginConfigured();
}

@Override
public String decryptLoginPassword(final String encryptedPassword, final String keyId) {
    requireLoginConfigured();
    final FepSecuritySm2Properties.LoginKeyPair pair = loginKeys.get(keyId);
    if (pair == null) {
        throw new IllegalArgumentException("Unknown SM2 login keyId: " + keyId);
    }
    final byte[] plain = Sm2LoginCipher.decryptC1C3C2(encryptedPassword, pair.getPrivateKeyHex());
    return new String(plain, java.nio.charset.StandardCharsets.UTF_8);
}

@Override
public byte[] getSignPrivateKey() {
    throw new UnsupportedOperationException(S2B_PENDING);
}

private String requireLoginConfigured() {
    if (loginActiveKeyId == null || loginKeys.isEmpty()) {
        throw new IllegalStateException(SM2_LOGIN_NOT_CONFIGURED);
    }
    return loginActiveKeyId;
}

private FepSecuritySm2Properties.LoginKeyPair requireLoginKey(final String keyId) {
    return loginKeys.get(keyId);
}
```
> **Task 2/3 编译衔接**：`Sm2LoginCipher.decryptC1C3C2` 在本 Task 以最小桩创建（`throw new UnsupportedOperationException("S2a T3")`），Task 3 TDD 替换为真实现——保证本 Task commit 点全模块编译绿、且桩在同 Plan 内闭环（非跨 Plan TODO）。

`GmSecurityConfiguration`（修改）：

```java
@EnableConfigurationProperties({FepSecurityKeyProperties.class, FepSecuritySm2Properties.class})
// ...
/**
 * SM4 主密钥 + SM2 登录密钥多版本加载服务。
 *
 * @param props    SM4 密钥配置
 * @param sm2Props SM2 登录密钥配置
 * @return KeyService 实现
 */
@Bean
public KeyService keyService(final FepSecurityKeyProperties props,
                             final FepSecuritySm2Properties sm2Props) {
    return new KeyServiceImpl(props, sm2Props);
}
```

`MockKeyService` + `TestKeyServiceConfiguration` 匿名类各加：

```java
@Override
public String getSm2LoginKeyId() {
    return MOCK_KEY_ID; // TestKeyServiceConfiguration 内为字面值 "mock-key-v1"
}
```

`CallbackCredentialEncryptionFacadeTest.perKeyIdKeyService()`（L119）匿名类加（Round 1 B-2——接口扩展的第 3 个实现点，漏改则 fep-web test sources 编译破坏）：

```java
@Override
public String getSm2LoginKeyId() {
    return "k-new";
}
```

`MockKeyServiceTest` 加断言：

```java
@Test
void getSm2LoginKeyId_returnsMockKeyId() {
    assertThat(service.getSm2LoginKeyId()).isEqualTo("mock-key-v1");
}
```

- [ ] **Step 4: 测试通过 + 门禁（全 security 三模块 + fep-web test sources 编译）**

```bash
cd /Users/muzhou/FEP_v1.0_wt-gm-s2a && ./mvnw test -pl fep-security-impl,fep-security-mock -am -Dtest='KeyServiceImplTest,MockKeyServiceTest' -Dsurefire.failIfNoSpecifiedTests=false
cd /Users/muzhou/FEP_v1.0_wt-gm-s2a && ./mvnw test-compile -pl fep-web -am
cd /Users/muzhou/FEP_v1.0_wt-gm-s2a && ./mvnw compile spotbugs:check -pl fep-security-api,fep-security-impl,fep-security-mock -am
```
期望: 全绿 + fep-web test sources 编译 SUCCESS（验收 #6，B-2 闸口）+ `BugInstance size 0`×3 模块

- [ ] **Step 5: 提交**

```bash
cd /Users/muzhou/FEP_v1.0_wt-gm-s2a && git add fep-security-api/src fep-security-impl/src fep-security-mock/src fep-web/src/test && git commit -m "$(cat <<'EOF'
feat(security): SM2 login key loading with multi-version + [d]G pairing check (GM S2a T2)

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

- [ ] **Step 6: 派发 spec review + quality review subagent**

---

## Task 3: SM2 登录解密实装（GB/T KAT + sm-crypto 互操作） `模式 B`

**PRD 依据:** v1.3 §5.1.5 密码传输前端加密（SM2 公钥加密）
**追溯 ID:** FR-INFRA-GM-SM2-LOGIN

**验收标准:**
1. **GB/T 32918.5-2017 附录 A KAT**: `decryptC1C3C2(标准密文_无04, dB)` == `"encryption standard"`（UTF-8 字节）
2. **sm-crypto 互操作**: `decryptLoginPassword(SM_CRYPTO_FIXTURE_CIPHER_HEX, "sm2-login-v1")` == `"Sm2@LoginPwd2026"`（生产路径：前端 sm-crypto 产文 → 后端 BC 解密）
3. **roundtrip**: BC `SM2Engine(C1C3C2)` 自加密任意 UTF-8 明文 → 去 04 前缀 → 解密还原
4. C3 篡改（任一 hex 字符翻转）→ `IllegalArgumentException`（BC `InvalidCipherTextException` 包装，**异常消息不含密文/明文**）
5. 非 hex / 奇数长度 / null / 长度 < 194 hex（C1 128 + C3 64 + C2 ≥2）→ `IllegalArgumentException`
6. 未知 keyId → `IllegalArgumentException`（Task 2 已建，此处回归）

**Files:**
- Modify: `fep-security-impl/.../impl/key/Sm2LoginCipher.java`（桩 → 真实现）
- Create: `fep-security-impl/src/test/.../impl/key/Sm2LoginCipherTest.java`
- Modify: `fep-security-impl/src/test/.../impl/key/KeyServiceImplTest.java`（+sm-crypto fixture 门面级用例）

- [ ] **Step 1: 失败测试**

```java
// fep-security-impl/src/test/java/com/puchain/fep/security/impl/key/Sm2LoginCipherTest.java
package com.puchain.fep.security.impl.key;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import org.bouncycastle.crypto.engines.SM2Engine;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;

/**
 * SM2 解密 GB/T 32918.5-2017 附录 A 标准向量（推荐曲线加密示例）+ sm-crypto 跨实现互操作验证。
 */
class Sm2LoginCipherTest {

    @Test
    void decrypt_gbt32918AppendixAVector_recoversEncryptionStandard() {
        final byte[] plain = Sm2LoginCipher.decryptC1C3C2(
                Sm2TestVectors.GBT_CIPHER_C1C3C2_NO_PREFIX_HEX,
                Sm2TestVectors.GBT_PRIVATE_KEY_HEX);
        assertThat(new String(plain, StandardCharsets.US_ASCII))
                .isEqualTo(Sm2TestVectors.GBT_PLAINTEXT);
    }

    @Test
    void decrypt_smCryptoProducedCipher_recoversLoginPassword() {
        final byte[] plain = Sm2LoginCipher.decryptC1C3C2(
                Sm2TestVectors.SM_CRYPTO_FIXTURE_CIPHER_HEX,
                Sm2TestVectors.GBT_PRIVATE_KEY_HEX);
        assertThat(new String(plain, StandardCharsets.UTF_8))
                .isEqualTo(Sm2TestVectors.SM_CRYPTO_FIXTURE_PLAINTEXT);
    }

    @Test
    void decrypt_bcSelfEncryptedRoundtrip_recoversUtf8Plaintext() throws Exception {
        final String message = "FEP 登录密码 Roundtrip ✓ 2026";
        final SM2Engine encryptEngine = new SM2Engine(SM2Engine.Mode.C1C3C2);
        final var pubPoint = Sm2LoginCipher.DOMAIN.getCurve().decodePoint(
                Hex.decode(Sm2TestVectors.GBT_PUBLIC_KEY_HEX));
        encryptEngine.init(true, new ParametersWithRandom(
                new ECPublicKeyParameters(pubPoint, Sm2LoginCipher.DOMAIN), new SecureRandom()));
        final byte[] msg = message.getBytes(StandardCharsets.UTF_8);
        final byte[] cipherWithPrefix = encryptEngine.processBlock(msg, 0, msg.length);
        // BC 输出带 04 前缀 → 去前缀模拟前端线格式
        final String wireHex = Hex.toHexString(cipherWithPrefix).substring(2);
        final byte[] plain = Sm2LoginCipher.decryptC1C3C2(
                wireHex, Sm2TestVectors.GBT_PRIVATE_KEY_HEX);
        assertThat(new String(plain, StandardCharsets.UTF_8)).isEqualTo(message);
    }

    @Test
    void decrypt_tamperedC3_throwsWithoutLeakingData() {
        final String cipher = Sm2TestVectors.GBT_CIPHER_C1C3C2_NO_PREFIX_HEX;
        // C3 区间 = [128, 192)，翻转其第 1 个字符
        final char original = cipher.charAt(128);
        final char flipped = original == '5' ? '6' : '5';
        final String tampered = cipher.substring(0, 128) + flipped + cipher.substring(129);
        assertThatThrownBy(() -> Sm2LoginCipher.decryptC1C3C2(
                tampered, Sm2TestVectors.GBT_PRIVATE_KEY_HEX))
                .isInstanceOf(IllegalArgumentException.class)
                .satisfies(e -> {
                    assertThat(e.getMessage())
                            .doesNotContain(tampered)
                            .doesNotContain("encryption standard");
                    if (e.getCause() != null && e.getCause().getMessage() != null) {
                        assertThat(e.getCause().getMessage())
                                .doesNotContain(tampered)
                                .doesNotContain("encryption standard");
                    }
                });
    }

    @Test
    void decrypt_malformedInput_throwsIllegalArgument() {
        final String priv = Sm2TestVectors.GBT_PRIVATE_KEY_HEX;
        assertThatThrownBy(() -> Sm2LoginCipher.decryptC1C3C2(null, priv))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Sm2LoginCipher.decryptC1C3C2("zz" + "00".repeat(96), priv))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Sm2LoginCipher.decryptC1C3C2("0".repeat(193), priv))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Sm2LoginCipher.decryptC1C3C2("00".repeat(64), priv))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
```

`KeyServiceImplTest` 门面级用例（+1）：

```java
@Test
void decryptLoginPassword_smCryptoFixture_recoversPassword() {
    final KeyService svc = newServiceWithSm2("sm2-login-v1",
            Sm2TestVectors.GBT_PRIVATE_KEY_HEX, Sm2TestVectors.GBT_PUBLIC_KEY_HEX);
    assertThat(svc.decryptLoginPassword(
            Sm2TestVectors.SM_CRYPTO_FIXTURE_CIPHER_HEX, "sm2-login-v1"))
            .isEqualTo(Sm2TestVectors.SM_CRYPTO_FIXTURE_PLAINTEXT);
}

@Test
void decryptLoginPassword_unknownKeyId_throwsIllegalArgument() {
    final KeyService svc = newServiceWithSm2("sm2-login-v1",
            Sm2TestVectors.GBT_PRIVATE_KEY_HEX, Sm2TestVectors.GBT_PUBLIC_KEY_HEX);
    assertThatThrownBy(() -> svc.decryptLoginPassword(
            Sm2TestVectors.SM_CRYPTO_FIXTURE_CIPHER_HEX, "ghost-key"))
            .isInstanceOf(IllegalArgumentException.class);
}
```

- [ ] **Step 2: 运行确认失败**

```bash
cd /Users/muzhou/FEP_v1.0_wt-gm-s2a && ./mvnw test -pl fep-security-impl -am -Dtest='Sm2LoginCipherTest,KeyServiceImplTest' -Dsurefire.failIfNoSpecifiedTests=false
```
期望: `Sm2LoginCipherTest` 全 RED（桩抛 UnsupportedOperationException）

- [ ] **Step 3: 真实现（替换桩）**

```java
// Sm2LoginCipher 追加（替换 Task 2 桩）
// import 增量: org.bouncycastle.crypto.engines.SM2Engine
//             org.bouncycastle.crypto.params.ECPrivateKeyParameters

/** 前端线格式最小 hex 长度：C1(128) + C3(64) + C2(≥2)。 */
private static final int MIN_CIPHER_HEX_LENGTH = 194;

/** 未压缩点标识字节。 */
private static final byte UNCOMPRESSED_POINT_PREFIX = 0x04;

/**
 * 解密前端线格式 SM2 密文（sm-crypto C1C3C2，hex，无 04 前缀——
 * fep-admin-ui sm2-cipher.ts 契约，内部统一补 0x04 后喂 BC SM2Engine）。
 *
 * @param cipherHexNoPrefix C1C3C2 hex（≥194 字符，无 04 前缀）
 * @param privateKeyHex     私钥标量 hex（64 字符）
 * @return 明文字节
 * @throws IllegalArgumentException 输入格式非法或解密失败（消息不含密文/明文）
 */
static byte[] decryptC1C3C2(final String cipherHexNoPrefix, final String privateKeyHex) {
    if (cipherHexNoPrefix == null || cipherHexNoPrefix.length() < MIN_CIPHER_HEX_LENGTH
            || cipherHexNoPrefix.length() % 2 != 0) {
        throw new IllegalArgumentException(
                "SM2 ciphertext must be even-length hex of at least "
                        + MIN_CIPHER_HEX_LENGTH + " chars (C1C3C2, no 04 prefix)");
    }
    final byte[] c1c3c2;
    try {
        c1c3c2 = Hex.decode(cipherHexNoPrefix);
    } catch (final org.bouncycastle.util.encoders.DecoderException e) {
        throw new IllegalArgumentException("SM2 ciphertext is not valid hex", e);
    }
    final byte[] withPrefix = new byte[c1c3c2.length + 1];
    withPrefix[0] = UNCOMPRESSED_POINT_PREFIX;
    System.arraycopy(c1c3c2, 0, withPrefix, 1, c1c3c2.length);
    final SM2Engine engine = new SM2Engine(SM2Engine.Mode.C1C3C2);
    engine.init(false, new ECPrivateKeyParameters(
            new BigInteger(privateKeyHex, 16), DOMAIN));
    try {
        return engine.processBlock(withPrefix, 0, withPrefix.length);
    } catch (final org.bouncycastle.crypto.InvalidCipherTextException e) {
        throw new IllegalArgumentException("SM2 login password decryption failed", e);
    }
}
```

- [ ] **Step 4: 测试通过 + 门禁**

```bash
cd /Users/muzhou/FEP_v1.0_wt-gm-s2a && ./mvnw test -pl fep-security-impl -am -Dtest='Sm2LoginCipherTest,KeyServiceImplTest,CryptoServiceImplTest,HashServiceImplTest' -Dsurefire.failIfNoSpecifiedTests=false
cd /Users/muzhou/FEP_v1.0_wt-gm-s2a && ./mvnw compile spotbugs:check -pl fep-security-api,fep-security-impl,fep-security-mock -am
```

- [ ] **Step 5: 提交**

```bash
cd /Users/muzhou/FEP_v1.0_wt-gm-s2a && git add fep-security-impl/src && git commit -m "$(cat <<'EOF'
feat(security): SM2 login decryption with GB/T 32918.5 KAT + sm-crypto interop (GM S2a T3)

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

- [ ] **Step 6: 派发 spec review + quality review subagent**

---

## Task 4: 消费链接通（AuthController keyId 切换 + provider=impl IT） `模式 B`

**PRD 依据:** v1.3 §5.1.5 + §5.1.4 登录流程
**追溯 ID:** FR-INFRA-GM-SM2-LOGIN

**验收标准:**
1. `GET /api/v1/auth/public-key`（provider=impl）返回真实 SM2 公钥 Base64 + **SM2 登录 keyId**（非 SM4 凭证 keyId）
2. `LoginVerifier.resolveClearPassword`（provider=impl 全 context，真 `KeyServiceImpl` bean）对 `LoginRequest(encryptedPassword=sm-crypto fixture, keyId=sm2-login-v1)` 还原明文 `Sm2@LoginPwd2026`（IT 用例 `loginVerifier_resolveClearPassword_decryptsViaRealKeyService`）
3. provider=impl 全 context 启动成功（红线 `feedback_provider_impl_full_context_test_needs_mockbean_unimpl_spi`：`@MockBean SignService` 桩补未实装 SPI；SM4 props 必配——KeyServiceImpl SM4 校验仍强制）
4. mock provider 回归零破坏：`AuthControllerTest`（mock stub 改 `getSm2LoginKeyId`）+ `LoginVerifierTest` GREEN
5. `CallbackCredentialEncryptionFacade` **生产行为**不变（继续 `getKeyId()` = SM4 keyId，生产代码零触碰；其测试的匿名桩 override 已在 T2 闭合，既有断言零修改 GREEN）

**Files:**
- Modify: `fep-web/.../auth/controller/AuthController.java`（L156 `keyService.getKeyId()` → `keyService.getSm2LoginKeyId()`）
- Modify: `fep-web/src/test/.../auth/controller/AuthControllerTest.java`（stub 同步）
- Create: `fep-web/src/test/java/com/puchain/fep/web/auth/service/Sm2LoginDecryptionProviderImplTest.java`

- [ ] **Step 1: 失败测试（IT，命名 `*Test` 防 Surefire 静默跳过）**

```java
// fep-web/src/test/java/com/puchain/fep/web/auth/service/Sm2LoginDecryptionProviderImplTest.java
package com.puchain.fep.web.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.puchain.fep.security.api.HashService;
import com.puchain.fep.security.api.KeyService;
import com.puchain.fep.security.api.SignService;
import com.puchain.fep.web.auth.domain.LoginRequest;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * GM S2a provider=impl 全 context 登录解密链路 IT（镜像 S1
 * CallbackLegacyCredentialMigrationTest 范式）。
 *
 * <p>密钥字面值 = GB/T 32918.5-2017 附录 A 公开标准测试向量（非生产密钥）；
 * 密文 fixture 由前端 sm-crypto@0.3.13 doEncrypt(cipherMode=1) 实测生成，
 * 验证生产真实路径（sm-crypto 产文 → BC 解密）。</p>
 */
@SpringBootTest
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "fep.security.provider=impl",
        "fep.security.sm4.active-key-id=sm4-cred-v1",
        "fep.security.sm4.sm4-keys.sm4-cred-v1=0123456789abcdeffedcba9876543210",
        "fep.security.sm2.login-active-key-id=sm2-login-v1",
        "fep.security.sm2.login-keys.sm2-login-v1.private-key-hex="
                + "3945208f7b2144b13f36e38ac6d39f95889393692860b51a42fb81ef4df7c5b8",
        "fep.security.sm2.login-keys.sm2-login-v1.public-key-hex="
                + "0409f9df311e5421a150dd7d161e4bc5c672179fad1833fc076bb08ff356f35020"
                + "ccea490ce26775a52dc6ea718cc1aa600aed05fbf35e084a6632f6072da9ad13"
})
class Sm2LoginDecryptionProviderImplTest {

    /** sm-crypto@0.3.13 实测 fixture（同 fep-security-impl Sm2TestVectors，跨模块重复注明来源）。 */
    private static final String SM_CRYPTO_CIPHER_HEX =
            "7f124706e5f4e8c618dd4d46bd2e52ee8d4583597b7105284f41f2098865b349"
                    + "613c972491f8aa8741253d09d66c31cc026f63cb8743808dc066b77da18a0da9"
                    + "c886117e757723bc3d3391ba6d7c5f0549da88cf94e5374ba3e06e12358a371c"
                    + "d5cf6ea9209ecf782d390ce6fcc1733f";

    @Autowired
    private KeyService keyService;

    @Autowired
    private HashService hashService;

    @Autowired
    private LoginVerifier loginVerifier;

    // provider=impl 下 SignService（SM2 报文签名）属 S2b 未实现、mock 已门控关 → 桩补使全 context 启动
    @MockBean
    private SignService signService;

    @Test
    void publicKeyDistribution_returnsRawPointBase64AndLoginKeyId() {
        assertThat(keyService.getSm2LoginKeyId()).isEqualTo("sm2-login-v1");
        final byte[] point = Base64.getDecoder().decode(keyService.getSm2PublicKeyBase64());
        assertThat(point).hasSize(65);
        assertThat(point[0]).isEqualTo((byte) 0x04);
    }

    @Test
    void decryptLoginPassword_smCryptoWireFormat_recoversPlaintext() {
        assertThat(keyService.decryptLoginPassword(SM_CRYPTO_CIPHER_HEX, "sm2-login-v1"))
                .isEqualTo("Sm2@LoginPwd2026");
    }

    @Test
    void loginVerifier_resolveClearPassword_decryptsViaRealKeyService() {
        final LoginRequest request = new LoginRequest();
        request.setEncryptedPassword(SM_CRYPTO_CIPHER_HEX);
        request.setKeyId("sm2-login-v1");
        assertThat(loginVerifier.resolveClearPassword(request)).isEqualTo("Sm2@LoginPwd2026");
    }

    @Test
    void sm4CredentialKeyId_staysIndependentFromSm2LoginKeyId() {
        assertThat(keyService.getKeyId()).isEqualTo("sm4-cred-v1");
        assertThat(keyService.getSm2LoginKeyId()).isNotEqualTo(keyService.getKeyId());
    }

    @Test
    void hashService_alwaysOn_availableUnderImplProvider() {
        assertThat(hashService.sm3Hex("abc".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .isEqualTo("66c7f0f462eeedd9d1f2d46bdc10e4e24167c4875cf2f7a2297da02b8f4ba8e0");
    }
}
```

- [ ] **Step 2: 运行确认失败**

```bash
cd /Users/muzhou/FEP_v1.0_wt-gm-s2a && ./mvnw test -pl fep-web -am -Dtest=Sm2LoginDecryptionProviderImplTest -Dsurefire.failIfNoSpecifiedTests=false
```
期望: 通过与否取决于 T2/T3 已实装（应 GREEN——本 IT 是链路验证）；`sm4CredentialKeyId_staysIndependentFromSm2LoginKeyId` 在 AuthController 未切换前不涉及。**AuthController 切换的 RED 锚点**：先改 `AuthControllerTest` stub 期望 `getSm2LoginKeyId`（改后 AuthControllerTest RED→Step 3 切换后 GREEN）。

`AuthControllerTest` stub 同步（既有 `when(keyService.getKeyId())` 公钥端点相关 stub 改为）：

```java
when(keyService.getSm2LoginKeyId()).thenReturn("sm2-login-v1");
// 断言 PublicKeyResponse.keyId == "sm2-login-v1"
```

- [ ] **Step 3: AuthController 切换**

```java
// AuthController.getPublicKey()（L153-156 区域）
return ApiResult.success(new PublicKeyResponse(
        keyService.getSm2PublicKeyBase64(),
        keyService.getSm2LoginKeyId(),
        "SM2"));
```

- [ ] **Step 4: 测试通过 + 门禁（fep-web 全模块测试——`feedback_full_regression_before_commit`）**

```bash
cd /Users/muzhou/FEP_v1.0_wt-gm-s2a && ./mvnw test -pl fep-web -am
cd /Users/muzhou/FEP_v1.0_wt-gm-s2a && ./mvnw compile spotbugs:check -pl fep-web -am
```
期望: fep-web 全测试 GREEN（含 NamingConventionTest/ArchUnit + 既有 LoginVerifierTest/CallbackCredentialEncryptionFacadeTest 零修改）+ BugInstance 0
> 本机 load >50 / 多会话并发 build 时（S4 教训③）：fep-web 全测试委托 GHA 权威门禁，本地跑单测子集 `-Dtest='Sm2LoginDecryptionProviderImplTest,AuthControllerTest,LoginVerifierTest' -Dsurefire.failIfNoSpecifiedTests=false`

- [ ] **Step 5: 提交**

```bash
cd /Users/muzhou/FEP_v1.0_wt-gm-s2a && git add fep-web/src && git commit -m "$(cat <<'EOF'
feat(auth): wire SM2 login keyId into public-key endpoint + provider=impl IT (GM S2a T4)

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

- [ ] **Step 6: 派发 spec review + quality review subagent**

---

## Task 5: 回归验收 + 配置样板 + 收尾 `模式 A`

**PRD 依据:** 元任务（回归/文档），无独立 FR-ID

**回归验收（红线 `feedback_plan_regression_scope_explicit` 两层显式）:**

- **strong（必须，二选一）**: ① worktree 内前台 `./mvnw verify --batch-mode --no-transfer-progress` 全 reactor GREEN（同一时刻仅本会话跑全量——CLAUDE.md fork 限流约束；先 `pgrep -fl mvn` 确认无并发 build）或 ② 本机 load 高 → push 后 GHA Build/Test & Quality SUCCESS 为权威门禁（S1/S4 先例）
- **minimum（strong ① 不可行时的本地底线）**: `./mvnw verify -pl fep-security-api,fep-security-impl,fep-security-mock,fep-web -am` GREEN

- [ ] **Step 1: prod 配置样板（注释，真密钥部署期注入）**

```bash
grep -n "fep:" -A 20 /Users/muzhou/FEP_v1.0_wt-gm-s2a/fep-web/src/main/resources/application-prod.yml
```
对齐既有 sm4 块的**激活态 + env 占位符**形态（属性绑定被 `GmSecurityConfiguration @ConditionalOnProperty(provider=impl)` 门控，provider≠impl 时不触发占位符解析）追加：

```yaml
    sm2:                                        # GM S2a：SM2 登录密钥（真实值部署期 env 注入，永不入 repo）
      login-active-key-id: ${FEP_SM2_LOGIN_ACTIVE_KEY_ID:sm2-login-v1}   # 对齐 sm4 块 env+default 形态
      login-keys:
        sm2-login-v1:                           # map key 须字面值（YAML key 不解析 ${} 占位符），轮换时新增字面 key
          private-key-hex: ${FEP_SM2_LOGIN_PRIVATE_KEY_HEX}   # 32 字节标量 d（hex 64 字符）
          public-key-hex: ${FEP_SM2_LOGIN_PUBLIC_KEY_HEX}     # 04||x||y 65 字节（hex 130 字符）
```
> 缩进以 Step 1 grep 实测的既有 `fep.security.sm4` 块为准对齐。`provider: impl` 本身保持注释态：prod cutover 仍 gated on S2b（R7）。

- [ ] **Step 2: strong 回归**

```bash
cd /Users/muzhou/FEP_v1.0_wt-gm-s2a && pgrep -fl "mvn" || true
cd /Users/muzhou/FEP_v1.0_wt-gm-s2a && ./mvnw verify --batch-mode --no-transfer-progress 2>&1 | tail -40
```

- [ ] **Step 3: CLAUDE.md 当前项目状态 + roadmap §3.1 S2a 标记 ✅（file write，红线 `feedback_fep_docs_repo_commit_taboo`：/FEP 侧文件不 git commit）**

- [ ] **Step 4: 提交 + push + PR**

```bash
cd /Users/muzhou/FEP_v1.0_wt-gm-s2a && git add fep-web/src/main/resources && git commit -m "$(cat <<'EOF'
docs(config): add SM2 login key env-injection template to prod profile (GM S2a T5)

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
cd /Users/muzhou/FEP_v1.0_wt-gm-s2a && git push -u origin feat/gm-s2a-sm3-sm2-login
cd /Users/muzhou/FEP_v1.0_wt-gm-s2a && gh pr create --title "feat(security): GM S2a — SM3 hash + SM2 login decrypt/pubkey distribution" --body "$(cat <<'EOF'
## Summary
- SM3 HashService (GB/T 32905 vectors, always-on)
- SM2 login key loading (multi-version + [d]G pairing check)
- SM2 login password decryption (GB/T 32918.5 KAT + sm-crypto interop fixture)
- AuthController public-key endpoint: SM2 login keyId (decoupled from SM4 credential keyId)

Plan: docs/plans/2026-06-10-gm-s2a-sm3-sm2-login.md (muzhou signed)
roadmap Phase A: S0 ✅ S1 ✅ S4 ✅ → **S2a**; S2b still gated on §0.3.

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```
> PR mutation 遇网络错按红线 `feedback_gh_mutation_network_error_verify_before_retry` 先 read-verify。PR-Size >400 时按 S1 #73 / S4 #75 先例走 muzhou `--admin --squash` 豁免决策。

- [ ] **Step 5: 派发 final whole-impl review subagent**（跨 Task 整体走查：helper 对称性 / Javadoc 与实现一致 / 异常无敏感数据）

- [ ] **Step 6: merge 后 closing（muzhou merge 决策后执行）**

```bash
cd /Users/muzhou/FEP_v1.0 && git fetch && git checkout main && git merge --ff-only origin/main
git -C /Users/muzhou/FEP_v1.0 worktree remove /Users/muzhou/FEP_v1.0_wt-gm-s2a
git -C /Users/muzhou/FEP_v1.0 branch -d feat/gm-s2a-sm3-sm2-login
git -C /Users/muzhou/FEP_v1.0 push origin --delete feat/gm-s2a-sm3-sm2-login
git -C /Users/muzhou/FEP_v1.0 worktree list
```

---

## 评审与签字流程（强制）

1. **santa 双审**（Plan 全文 + PRD §3.3/§5.1.5 + plan-review-checklist 7 项）
2. **密码学专项 review**（对照 GB/T 32905/32918：算法参数（曲线 sm2p256v1 / C1C3C2 / SM3 输出长）、测试向量逐字节、密钥材料无入 repo、异常路径无明文泄漏）
3. **muzhou 签字** — 未签字禁止进入 Task 0

### 评审员 claim grep 清单（增量）
- [ ] 向量字面值与 Plan §设计背景表逐字符一致（`grep -c` 实测，禁目测）
- [ ] 抉择②③（线格式）与 `sm2-cipher.ts` L83-89/L145-152 实测一致
- [ ] 抉择⑤ `getKeyId()` 双消费差集：`grep -rn "getKeyId()" fep-web/src/main` 确认仅 AuthController 切换、Callback 链不动
- [ ] 每个 mvn 命令含 `-Dsurefire.failIfNoSpecifiedTests=false`（凡 `-Dtest=` + `-am` 组合）
- [ ] spotbugs 命令均为 `compile spotbugs:check ... -am`（红线 recompile）
- [ ] 数据点自洽：Task 数 5（T0 元任务除外）/ 新建文件 9 + 修改文件 12（文件结构表 21 行）

---

## 自检清单（起草自检 2026-06-10）

| # | 项 | 结果 |
|---|----|------|
| 1 | PRD 覆盖度 | §3.3.2 SM3 / §5.1.5 SM2 登录 / §3.3.4 密钥形态覆盖；§3.3.1/3.3.3 签验 wiring 明示 S2b 不在范围 |
| 2 | 安全边界 | 本 Plan 无 ⛔ E Task（2026-06-07 解禁 + §0.3 豁免域）；getSignPrivateKey 维持 S2b 边界 |
| 3 | 占位符扫描 | 唯一"桩"= T2→T3 同 Plan 内编译衔接桩（T3 闭环替换，非跨 Plan TODO）✓ |
| 4 | 类型一致性 | Sm2TestVectors/Sm2LoginCipher/FepSecuritySm2Properties 跨 Task 引用一致 ✓ |
| 5 | 测试命令可执行 | -Dtest 类名与新建测试类匹配；surefire3 参数已带 ✓ |
| 6 | CLAUDE.md 更新 | T5 Step 3 ✓ |
| 7 | 验收标准来源 | GB/T 双源核验向量 + PRD 字段定义 + 前端实测契约，非从实现倒推 ✓ |
| 8 | 共享工具类 | Sm2LoginCipher/Sm2TestVectors 已登记 ✓ |
| 9 | 职责边界 | KeyServiceImpl 声明修订（依赖 2 / 预计 ~230 行）✓ |
| 10 | Worktree 触发 | 命中 ①⑤⑥ → wt-gm-s2a 已声明；closing 含 worktree remove 实测命令 ✓ |

---

## 签字区

- [x] santa Round 1: REVISE（2026-06-10，B-1 SM3 向量 + B-2 匿名 KeyService 漏列 + C-1/C-2/C-3 + N-1/N-3）→ v0.2 闭合
- [x] 密码学专项 Round 1: REVISE（2026-06-10，B-1 同源 + C-2 出处部号 + C-3 yml map key + C-4 私钥范围 + N-5）→ v0.2 闭合
- [x] santa Round 2: 仅余 4 处归属残留 + 3 NIT（2026-06-10）→ v0.3 闭合，判定"修后可直接签字，无需 Round 3"
- [x] 密码学专项 Round 2: PASS WITH CONCERNS（2026-06-10，SM2 向量本体独立解密双仲裁 GREEN；CONCERN-1 = 同 4 处归属残留）→ v0.3 闭合，判定"无需 Round 3 密码学复审"
- [x] **muzhou 批准**: ✅ APPROVED 2026-06-10（AskUserQuestion 选定"批准，立即执行"；v0.3 即签字版）
