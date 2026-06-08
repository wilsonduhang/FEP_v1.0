# FEP S1 — SM4 国密真实化 + KeyServiceImpl + callback_credential 惰性双读迁移 实施计划

> **执行方式:** 使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐 Task 执行。步骤使用 `- [ ]` 复选框跟踪。

**目标:** 用 BouncyCastle 实现真实 SM4/ECB/PKCS7 加解密 (`CryptoServiceImpl`) + SM4 主密钥多版本加载框架 (`KeyServiceImpl`)，并以**惰性双读重加密**策略把 callback_credential 表 mock 透传期存的明文凭证灰度迁移为真实 SM4 密文，解锁 callback 凭证生产部署。

**前置依赖:** S0（fep-security-impl 模块 + BouncyCastle GM provider + GB/T 32907 向量，PR #70 ✅ MERGED `9fa194f5`，现 origin/main 已进至 `b3b217f5`）。

**执行 Worktree:** `/Users/muzhou/FEP_v1.0_wt-gm-s1`（分支 `feat/gm-s1-sm4-keyservice`，触发条件第 ② 项 + 第 ⑤ 项）
> 红线 `feedback_worktree_for_parallel_work` 命中：② 与已签字未执行 Plan 并存（§5.8 业务校验引擎 Plan 已签字未执行 `wt-msg-rule-engine` 别会话活跃 + `wt-simplify-q-drain` 别会话）；⑤ fep-web `@SpringBootTest` 全模块 verify > 5min long-running，期间别会话并行。建立 worktree：`git worktree add -b feat/gm-s1-sm4-keyservice /Users/muzhou/FEP_v1.0_wt-gm-s1 origin/main`（基线 `b3b217f5` = 实测 origin/main，含 §5.8 rule engine #71，与安全正交；S0 模块仍在）。**执行时重 grep origin/main 确认无新 drift（红线 `feedback_baseline_drift_during_long_review_cycle`）。**

**架构:**
1. `fep-security-impl` 新增 `CryptoServiceImpl`（SM4/ECB/PKCS7Padding，复用 S0 已验证的 BC `"BC"` provider）+ `KeyServiceImpl`（从 `@ConfigurationProperties` 注入的多版本 16 字节主密钥 Map 加载，真实密钥部署期经 env/sealed store 注入，**永不入 repo**）。
2. 通过 `fep.security.provider` 单一开关互斥切换：mock `@Service` 加 `@ConditionalOnProperty(havingValue="mock", matchIfMissing=true)`（默认仍 mock，零回归）、impl `@ConditionalOnProperty(havingValue="impl")` → 任一时刻只装配一套 `CryptoService`/`KeyService` bean（消除双 bean 冲突 R3）。
3. callback_credential 迁移用**惰性双读**：以 `key_id` 为判别器（mock 期固定 `"mock-key-v1"` = 明文标记），读取时若为 legacy keyId → 字节按 UTF-8 当明文用 + `@Transactional(REQUIRES_NEW)` 重加密回写（`entity.rotate(...)` 翻 key_id 到真实版本）；非 legacy → 真实 SM4 解密。**无 Flyway schema 变更**（复用现有列 + key_id 鉴别）。

**技术栈:** Java 17 / Spring Boot 3.x / Maven / BouncyCastle (bcprov-jdk18on 1.84) / Spring `@ConfigurationProperties` + `@ConditionalOnProperty`。

**AI 协同模式:**

| 模式 | AI 占比 | 本计划中的应用 |
|:----:|:-------:|--------------|
| A | 90% | CryptoServiceImpl / KeyServiceImpl / 配置 / 测试（2026-06-07 muzhou 解禁国密域，国密实现现为 AI 可写 Mode A，配密码学专项 review）|
| B | 70% | callback_credential 惰性双读迁移业务逻辑（resolver 改造 + migrator）|

> **🔓 治理基线（必读）:** 2026-06-07 muzhou 授权 AI 进入国密安全域开发。本 Plan 的 SM4 实现为 **Mode A（AI 主导）**，非旧 ⛔ Mode E。评审 = santa + **密码学专项 review agent**（对照 GB/T 32907 + 标准测试向量）+ muzhou 签字；commit footer 用 `AI-Generated: claude-code` + `Reviewed-By`（**不**用 `Security-Reviewed-By: ③`）。**真实 SM4 主密钥永不入 repo**，dev/CI 用 GB/T 测试密钥，生产部署期注入。

---

## 设计背景

### 现状实测（baseline origin/main `b3b217f5`，已 grep 验证带行号；R6 修订）

**fep-security-api 接口（已声明，S1 仅实现，无 API 变更）：**
- `CryptoService.encrypt(byte[] plaintext, byte[] key)` / `decrypt(byte[] ciphertext, byte[] key)` — SM4/ECB/PKCS7Padding，key 16 字节，null/长度异常抛 `IllegalArgumentException`（`CryptoService.java:23,33`）。
- `KeyService`（`KeyService.java`）：`getSm2PublicKeyBase64():23` / `getKeyId():30` / `decryptLoginPassword(String,String):40` / `getSignPrivateKey():55` / `getSm4CredentialMasterKey():75` / `getSm4CredentialMasterKey(String keyId):95`。**S1 仅实现 SM4 两个重载 + getKeyId**；SM2 三方法（getSm2PublicKeyBase64/decryptLoginPassword/getSignPrivateKey）属 S2（+ §0.3 决策门）→ S1 中抛 `UnsupportedOperationException`，由 `@ConditionalOnProperty` 显式 opt-in 保证不破坏现有环境。

**fep-security-mock（@Profile("dev")，不动）：** `MockCryptoService` 明文透传（encrypt/decrypt 直返入参）；`MockKeyService.getKeyId()` 返回常量 `"mock-key-v1"`，`getSm4CredentialMasterKey()` 返回 16 字节占位常量。→ **mock 透传期 callback_credential 密文列实存明文 UTF-8 字节，key_id 全为 `"mock-key-v1"`**（迁移判别器依据）。

**callback_credential 表（V30 建 + V35 加 expires_at；无需 S1 schema 变更）：** 密文列 `token_ciphertext`(VARBINARY 512) / `oauth_client_id_ciphertext`(512) / `oauth_client_secret_ciphertext`(1024)；`key_id`(VARCHAR 32, NOT NULL)；轮换 `rotated_at`(TIMESTAMP)。Entity `CallbackCredentialEntity` 工厂法 `newToken`/`newOauth` + `rotate(newTokenCipher, newClientIdCipher, newClientSecretCipher, newKeyId)`（状态机式轮换，已存在，迁移复用）。

**凭证加解密链路：**
- Facade `CallbackCredentialEncryptionFacade`（`crypto/`）：`encrypt(String):EncryptedCredential(cipher, keyService.getKeyId())` 用 `getSm4CredentialMasterKey()`；`decrypt(byte[] ciphertext, String keyId):String` 用 `getSm4CredentialMasterKey(keyId)`。`EncryptedCredential` 为 facade 内嵌 record（ciphertext 防御性 clone）。
- Resolver `CallbackCredentialResolver`（`service/`）：`resolveToken`/`resolveOAuth2` 查库 `repo.findByInterfaceId` → `facade.decrypt(...)` 还原 → `AuthHeader`。**惰性双读改造点。**

**Flyway 最大编号 = V35** → **本 Plan 无新 V 迁移**（惰性双读复用现有列 + key_id 判别，免 schema lock；红线 `feedback_plan_flyway_v_collision_check` 实测确认无需 V36）。

**BC SM4 调用（复用 S0 `Sm4GbtVectorTest` 同款）：** `Cipher.getInstance("SM4/ECB/PKCS7Padding", "BC")` + `new SecretKeySpec(key, "SM4")`；GB/T 32907 标准向量 key/plain = `0123456789abcdeffedcba9876543210`，单块 NoPadding 密文 `681edf34d206965e86b3e94f536e4246`。

### 惰性双读迁移设计（高风险，muzhou 选定策略）

```
读路径 (resolver.resolveToken / resolveOAuth2)：
  entity = repo.findByInterfaceId(id)
  if (legacyKeyIds.contains(entity.getKeyId())):       # "mock-key-v1" 等
      plain = new String(cipherBytes, UTF_8)           # legacy 字节即明文
      migrator.migrateToActiveKey(id)                  # @Transactional(REQUIRES_NEW) 重加密回写
      return plain
  else:
      return facade.decrypt(cipherBytes, entity.getKeyId())   # 真实 SM4 解密
```

**判别器 = key_id**（确定性，非 try-catch 猜测）：mock 写入恒 `key_id="mock-key-v1"`，真实 `KeyServiceImpl.getKeyId()` 返回配置的活跃版本（如 `"sm4-cred-v1"`）。`legacyKeyIds` 配置化（默认 `["mock-key-v1"]`）。

**重加密（migrator，REQUIRES_NEW + 幂等）：** 重读 entity → 若已非 legacy 直接 return（并发/竞态安全）→ 对每个非 null 密文列：明文 = `new String(legacyBytes, UTF_8)` → `facade.encrypt(plain)` 得真实密文 → `entity.rotate(newTokenCipher, newClientIdCipher, newClientSecretCipher, activeKeyId)` → `repo.save`。读路径立即返回明文，迁移在独立事务完成，单行只迁一次（之后 key_id 翻转走真实解密）。

### ⚠️ 回滚预案与风险（高风险，muzhou 签字须知悉）

| 风险 | 说明 | 缓解 |
|------|------|------|
| 部分迁移后回滚有损 | 一旦某行已迁移为真实密文（key_id 翻转），回退到 mock profile 后 mock 透传解密会返回密文字节乱码 | **回滚窗口 = 首行迁移前**；启用 impl 前须 DB 全表备份；迁移开始后前向唯一（或保留 mock key 兼容期） |
| 真实主密钥配错 | 已迁移行用错误 key 解密失败 | 启用前用 GB/T 向量自检 + 灰度单接口先行 + `key_id` 多版本回退 |
| legacy 明文误判 | 若活跃 keyId 误配为 legacy 标记（如 `"mock-key-v1"`）致真密文被当明文泄漏 + 二次损坏 | **交集校验落地于 `CallbackLegacyCredentialMigrator` @PostConstruct（provider=impl 时校验 `facade.activeKeyId()` ∉ legacyKeyIds，违反抛 IllegalStateException 启动失败）**（C1 修订：KeyServiceImpl 在 fep-security-impl 看不见 fep-web legacy 配置，校验须落在两配置可见的汇聚点 = migrator）+ migrateToActiveKey 运行时二次守护 |
| 读路径写放大 | 惰性迁移在读时触发写 | REQUIRES_NEW 独立短事务 + 单行幂等只迁一次 + 迁移计数 metrics 可观测 |
| 冷接口明文滞留（C2）| 惰性 only：长期不被读的凭证密文列明文长期驻留 DB | legacy 行计数 gauge + 启动 WARN 日志（可观测剩余量）；**主动批量 sweep 列 S1 follow-up backlog**（冷行收口）；muzhou 知悉惰性策略固有滞留 |
| provider=impl 翻转全 KeyService（R7）| `provider=impl` 同时切换 SM2 方法（login `decryptLoginPassword` / 签名 `getSignPrivateKey` / 公钥分发），S1 中抛 UnsupportedOperationException → 若 prod 有 SM2 调用方即崩 | T3 grep 实测 prod-active SM2 调用方清单；**prod cutover（设 provider=impl）gated on S2（SM2 实现）或确认 prod SM2 链路未激活**；S1 仅交付 impl + wiring + 迁移能力，不强制 prod 立即切换 |

---

## 文件结构

| 文件路径 | 职责 | 操作 | AI 模式 |
|----------|------|------|:-------:|
| `fep-security-impl/.../crypto/CryptoServiceImpl.java` | SM4/ECB/PKCS7 真实加解密 | 新建 | A |
| `fep-security-impl/.../crypto/CryptoServiceImplTest.java` | GB/T 32907 服务级向量 + 边界 | 新建 | A |
| `fep-security-impl/.../key/FepSecurityKeyProperties.java` | SM4 多版本主密钥配置绑定 | 新建 | A |
| `fep-security-impl/.../key/KeyServiceImpl.java` | SM4 主密钥多版本加载 + getKeyId（SM2 方法抛 S2）| 新建 | A |
| `fep-security-impl/.../key/KeyServiceImplTest.java` | 多版本路由 + 防御性副本 + 配置校验 | 新建 | A |
| `fep-security-impl/.../GmSecurityImplAutoConfiguration.java` | @ConditionalOnProperty 装配真实实现 | 新建 | A |
| `fep-security-impl/src/main/resources/META-INF/spring/...AutoConfiguration.imports` | Spring Boot 3 自动配置注册 | 新建 | A |
| `fep-security-api/.../KeyService.java` | SM4 方法 Javadoc ⛔Mode E → 🔓Mode A 解禁更新 | 修改 | A |
| `fep-web/.../credential/migration/CallbackLegacyCredentialMigrator.java` | 惰性双读重加密（REQUIRES_NEW + 幂等）| 新建 | B |
| `fep-web/.../credential/migration/CallbackLegacyCredentialKeyIdProperties.java` | legacy 明文 keyId 集合配置 | 新建 | A |
| `fep-web/.../credential/crypto/CallbackCredentialEncryptionFacade.java` | 增 `activeKeyId()`（暴露当前 keyId 供 migrator 交集校验，C1）| 修改 | A |
| `fep-web/.../credential/repository/CallbackCredentialRepository.java` | 增 `countByKeyIdIn(Collection)`（legacy 行计数 gauge，C2）| 修改 | A |
| `fep-web/.../credential/service/CallbackCredentialResolver.java` | resolveToken/resolveOAuth2 惰性双读改造 | 修改 | B |
| `fep-web/.../credential/migration/CallbackLegacyCredentialMigrationTest.java` | legacy 行 → 双读明文 + 重加密 + key_id 翻转端到端 | 新建 | A |
| `fep-web/.../metrics/CallbackMetrics.java` | 增 recordCredentialMigrated() 计数（registry.counter 惰性风格，R4）| 修改 | A |
| `fep-web/pom.xml` | 增 fep-security-impl runtime 依赖（R1：impl 类运行期可装配，interface-only 引用守 R5/ArchUnit）| 修改 | A |
| `fep-security-mock/.../Mock{Crypto,Key,Sign}Service.java` | 加 `@ConditionalOnProperty(provider=mock, matchIfMissing=true)` 互斥（R3，默认仍 mock 零回归）| 修改 | A |
| `fep-web/src/main/resources/application-prod.yml` | MERGE 进既有 `fep:` 块：impl provider + env 引用密钥（无真实值，R5）| 修改 | A |
| `CLAUDE.md` | 当前项目状态更新 | 修改 | A |

### 共享工具类清单

| 工具类 | 包 | 关键方法 | 提供者 Task | 消费者 Task |
|---|---|---|---|---|
| BouncyCastleGmProviderConfig（S0 已存）| security.impl.crypto | BC provider 幂等注册 | S0（已有）| T1 CryptoServiceImpl 隐式依赖（"BC" provider 已注册）|
| FepSecurityKeyProperties | security.impl.key | sm4Key(keyId) / activeKeyId() | T2 | T2 KeyServiceImpl |

### 核心类职责边界声明

#### KeyServiceImpl 职责边界
- **负责**: 从 `FepSecurityKeyProperties` 加载 16 字节 SM4 主密钥（当前活跃 + 按 keyId 历史版本）+ 返回 getKeyId()。
- **不负责**: SM2 密钥/签名/登录解密（→ S2 + §0.3 决策门，S1 抛 `UnsupportedOperationException`）；密钥生成（→ 密码设备/HSM，部署期）；密钥持久化（→ 配置/sealed store 注入）。
- **依赖上限**: 7（实际 1：FepSecurityKeyProperties）。
- **行数上限**: 300。
- **如果超出**: 拆 SM4KeyLoader 独立组件。

#### CallbackCredentialResolver 职责边界（修改既有，引用 Callback Phase 2b 原声明）
- **负责**: 按 authType 解析鉴权头 + 本 Plan 新增惰性双读分流（legacy → 明文+触发迁移 / 真实 → SM4 解密）。
- **不负责**: 重加密事务（→ `CallbackLegacyCredentialMigrator`）；密钥加载（→ KeyService）。
- **依赖上限**: 7（现 6：repo/facade/cache/oauthClient/clock/metrics + 新增 migrator + legacyKeyIds = 8 → 超限）。
- **⚠️ 超限处置**: 新增 2 依赖致 8 > 7 → 将 `migrator` 与 `legacyKeyIds` 判别封装进 `CallbackLegacyCredentialMigrator`（migrator 自身持有 legacyKeyIds + 暴露 `boolean isLegacy(String keyId)`），resolver 仅 +1 依赖（migrator）= 7，守住上限。

---

### Task 1: CryptoServiceImpl（SM4/ECB/PKCS7 真实加解密）`模式 A`

**PRD 依据:** v1.3 §3.4.2 SM4 对称加密（ECB + PKCS#7）+ §8.3 安全
**追溯 ID:** FR-INFRA-GM-SM4（对照 `docs/plans/prd-traceability-matrix.md`，本 Plan 新增）

**验收标准（从 GB/T 32907-2016 + PRD §3.4.2 推导，不从代码推导）:**
1. GB/T 32907 标准向量：key=plain=`0123456789abcdeffedcba9876543210`，`encrypt` 单块（用 NoPadding 等价对照）密文 = `681edf34d206965e86b3e94f536e4246`。本服务用 PKCS7 → 对 16 字节明文追加整块填充，密文 32 字节，但 `decrypt(encrypt(p))==p` 必须成立。
2. roundtrip：任意 UTF-8 明文（含中文报文内容）`decrypt(encrypt(plain,key),key)` 逐字节等于 plain。
3. 边界：`plaintext`/`key` 为 null → `IllegalArgumentException`；`key` 长度 ≠ 16 → `IllegalArgumentException`。
4. 与 S0 `Sm4GbtVectorTest` 同 BC 调用（`"SM4/ECB/PKCS7Padding"`,`"BC"`,`SecretKeySpec(key,"SM4")`），确保国标合规一致。

**Files:**
- Create: `fep-security-impl/src/main/java/com/puchain/fep/security/impl/crypto/CryptoServiceImpl.java`
- Create: `fep-security-impl/src/test/java/com/puchain/fep/security/impl/crypto/CryptoServiceImplTest.java`

- [ ] **Step 1: 编写失败测试**

```java
// fep-security-impl/src/test/java/com/puchain/fep/security/impl/crypto/CryptoServiceImplTest.java
package com.puchain.fep.security.impl.crypto;

import com.puchain.fep.security.api.CryptoService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * CryptoServiceImpl SM4/ECB/PKCS7 真实加解密验证（GB/T 32907-2016 合规 + 边界）。
 *
 * <p>向量值为国标真值，不匹配=实现有问题，禁改向量迁就实现。</p>
 */
class CryptoServiceImplTest {

    private static final byte[] GBT_KEY =
            HexFormat.of().parseHex("0123456789abcdeffedcba9876543210");

    private final CryptoService crypto = new CryptoServiceImpl();

    @BeforeAll
    static void registerBc() {
        new BouncyCastleGmProviderConfig();
    }

    @Test
    void encryptThenDecrypt_recoversGbtStandardPlaintext() {
        final byte[] plain = HexFormat.of().parseHex("0123456789abcdeffedcba9876543210");
        final byte[] cipher = crypto.encrypt(plain, GBT_KEY);
        // PKCS7 对完整 16 字节块追加整块填充 → 密文 32 字节
        assertThat(cipher).hasSize(32);
        assertThat(crypto.decrypt(cipher, GBT_KEY)).isEqualTo(plain);
    }

    @Test
    void sm4SingleBlock_matchesGbtStandardVector_selfContainedAnchor() throws Exception {
        // C3：T1 自包含国标锚定 — 用 service 同款 "BC" provider 的 SM4/ECB/NoPadding 验单块
        // 已知答案（GB/T 32907-2016 标准向量），与 S0 Sm4GbtVectorTest 同值，确认 BC SM4 原语合规。
        final javax.crypto.Cipher c = javax.crypto.Cipher.getInstance("SM4/ECB/NoPadding", "BC");
        c.init(javax.crypto.Cipher.ENCRYPT_MODE,
                new javax.crypto.spec.SecretKeySpec(GBT_KEY, "SM4"));
        final byte[] singleBlock = c.doFinal(
                HexFormat.of().parseHex("0123456789abcdeffedcba9876543210"));
        assertThat(HexFormat.of().formatHex(singleBlock))
                .isEqualTo("681edf34d206965e86b3e94f536e4246");
    }

    @Test
    void roundtrip_utf8MessageContent_recoversPlaintext() {
        final byte[] plain = "FEP 报文 xmlstr 敏感内容 ¥123.45".getBytes(StandardCharsets.UTF_8);
        assertThat(crypto.decrypt(crypto.encrypt(plain, GBT_KEY), GBT_KEY)).isEqualTo(plain);
    }

    @Test
    void encrypt_nullPlaintext_throws() {
        assertThatThrownBy(() -> crypto.encrypt(null, GBT_KEY))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void encrypt_nullKey_throws() {
        assertThatThrownBy(() -> crypto.encrypt("x".getBytes(StandardCharsets.UTF_8), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void encrypt_wrongKeyLength_throws() {
        assertThatThrownBy(() -> crypto.encrypt(
                "x".getBytes(StandardCharsets.UTF_8), new byte[15]))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void decrypt_wrongKeyLength_throws() {
        assertThatThrownBy(() -> crypto.decrypt(new byte[32], new byte[17]))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd /Users/muzhou/FEP_v1.0_wt-gm-s1 && JAVA_HOME=/usr/local/opt/openjdk@17 PATH=$JAVA_HOME/bin:$PATH ./mvnw test -Dtest=CryptoServiceImplTest -pl fep-security-impl -am
```
期望: 编译失败 — `cannot find symbol: class CryptoServiceImpl`

- [ ] **Step 3: 编写最小实现**

```java
// fep-security-impl/src/main/java/com/puchain/fep/security/impl/crypto/CryptoServiceImpl.java
package com.puchain.fep.security.impl.crypto;

import com.puchain.fep.security.api.CryptoService;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;

/**
 * SM4/ECB/PKCS7Padding 真实加解密实现（BouncyCastle "BC" provider，GB/T 32907-2016）。
 *
 * <p>BC GM provider 由 {@link BouncyCastleGmProviderConfig}（S0）启动时幂等注册。
 * 严格遵 PRD §3.4.2（ECB + PKCS#7），与 HNDEMP 互通。密钥由 {@code KeyService} 提供，
 * 本类不持有任何密钥材料（无状态单例）。</p>
 *
 * <p><strong>🔓 Mode A:</strong> 2026-06-07 muzhou 解禁国密域，本实现 AI 编写 + 密码学
 * 专项 review（GB/T 32907 标准向量逐字节验证）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class CryptoServiceImpl implements CryptoService {

    /** SM4 算法变换字符串（ECB + PKCS#7 填充）。 */
    private static final String SM4_TRANSFORMATION = "SM4/ECB/PKCS7Padding";

    /** SM4 密钥算法名。 */
    private static final String SM4_ALGORITHM = "SM4";

    /** BouncyCastle provider 名（S0 注册）。 */
    private static final String BC_PROVIDER = "BC";

    /** SM4 密钥长度（字节）。 */
    private static final int SM4_KEY_LENGTH = 16;

    @Override
    public byte[] encrypt(final byte[] plaintext, final byte[] key) {
        validate(plaintext, key);
        return doCipher(Cipher.ENCRYPT_MODE, plaintext, key);
    }

    @Override
    public byte[] decrypt(final byte[] ciphertext, final byte[] key) {
        validate(ciphertext, key);
        return doCipher(Cipher.DECRYPT_MODE, ciphertext, key);
    }

    private static void validate(final byte[] data, final byte[] key) {
        if (data == null) {
            throw new IllegalArgumentException("data must not be null");
        }
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        if (key.length != SM4_KEY_LENGTH) {
            throw new IllegalArgumentException(
                    "SM4 key length must be " + SM4_KEY_LENGTH + " bytes, got " + key.length);
        }
    }

    private static byte[] doCipher(final int mode, final byte[] data, final byte[] key) {
        try {
            final Cipher cipher = Cipher.getInstance(SM4_TRANSFORMATION, BC_PROVIDER);
            cipher.init(mode, new SecretKeySpec(key, SM4_ALGORITHM));
            return cipher.doFinal(data);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SM4 " + (mode == Cipher.ENCRYPT_MODE
                    ? "encryption" : "decryption") + " failed", e);
        }
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

```bash
cd /Users/muzhou/FEP_v1.0_wt-gm-s1 && JAVA_HOME=/usr/local/opt/openjdk@17 PATH=$JAVA_HOME/bin:$PATH ./mvnw test -Dtest=CryptoServiceImplTest -pl fep-security-impl -am
```
期望: `BUILD SUCCESS`, 7 tests passed

- [ ] **Step 5: spotbugs + ArchUnit 自检**（红线 `feedback_subagent_must_run_spotbugs_check` + `feedback_spotbugs_check_needs_recompile_after_annotation`：先 compile -am 再 check）

```bash
cd /Users/muzhou/FEP_v1.0_wt-gm-s1 && JAVA_HOME=/usr/local/opt/openjdk@17 PATH=$JAVA_HOME/bin:$PATH ./mvnw compile spotbugs:check -pl fep-security-impl -am > /tmp/s1-t1-spotbugs.log 2>&1; echo "EXIT=$?"; grep -c "BugInstance" /tmp/s1-t1-spotbugs.log
```
期望: `EXIT=0`，BugInstance size 0（CryptoServiceImpl 无状态、不持引用，无 EI_EXPOSE）

- [ ] **Step 6: 提交**

```bash
cd /Users/muzhou/FEP_v1.0_wt-gm-s1 && git add fep-security-impl/src/ && git commit -m "$(cat <<'EOF'
feat(security): implement CryptoServiceImpl SM4/ECB/PKCS7 (GB/T 32907)

Real SM4 encryption via BouncyCastle "BC" provider (registered by S0
BouncyCastleGmProviderConfig). Strict ECB+PKCS7Padding per PRD §3.4.2 for
HNDEMP interop. Stateless singleton, holds no key material. 7 tests: GB/T
32907 standard-vector roundtrip + single-block known-answer + UTF-8 message
roundtrip + null/length guards.

FR-INFRA-GM-SM4. Authorized 2026-06-07 (GM domain unlock, Mode A + crypto review).
AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

### Task 2: KeyServiceImpl（SM4 主密钥多版本加载框架）`模式 A`

**PRD 依据:** v1.3 §3.3.4 密钥管理（加载/版本/轮换框架）+ §3.4.2
**追溯 ID:** FR-INFRA-GM-KEYSVC

**验收标准（从 PRD §3.3.4 推导）:**
1. `getSm4CredentialMasterKey()` 返回配置 `active-key-id` 对应的 16 字节密钥（防御性副本，连续两次调用返回不同数组实例但内容相等）。
2. `getSm4CredentialMasterKey(keyId)`：已知 keyId 返回对应 16 字节密钥；未知 keyId 抛 `IllegalArgumentException`。
3. `getKeyId()` 返回配置的 `active-key-id`。
4. 配置校验：`active-key-id` 必须在 keys map 中；keys 值必须 16 字节（hex 解码后）；启动期违反抛 `IllegalStateException`。
5. SM2 方法（getSm2PublicKeyBase64/decryptLoginPassword/getSignPrivateKey）抛 `UnsupportedOperationException`（S2 边界）。
6. **真实密钥永不入 repo**：测试用 GB/T 测试密钥（hex），生产值经 env 注入。

**Files:**
- Create: `fep-security-impl/src/main/java/com/puchain/fep/security/impl/key/FepSecurityKeyProperties.java`
- Create: `fep-security-impl/src/main/java/com/puchain/fep/security/impl/key/KeyServiceImpl.java`
- Create: `fep-security-impl/src/test/java/com/puchain/fep/security/impl/key/KeyServiceImplTest.java`

- [ ] **Step 1: 编写失败测试**

```java
// fep-security-impl/src/test/java/com/puchain/fep/security/impl/key/KeyServiceImplTest.java
package com.puchain.fep.security.impl.key;

import com.puchain.fep.security.api.KeyService;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * KeyServiceImpl SM4 主密钥多版本加载 + 配置校验 + 防御性副本 + S2 边界。
 *
 * <p>测试密钥为 GB/T 测试向量 hex（公开值），非生产密钥。</p>
 */
class KeyServiceImplTest {

    private static final String GBT_KEY_HEX = "0123456789abcdeffedcba9876543210";
    private static final String OLD_KEY_HEX = "fedcba98765432100123456789abcdef";

    private KeyServiceImpl newService(final String activeKeyId, final Map<String, String> keys) {
        final FepSecurityKeyProperties props = new FepSecurityKeyProperties();
        props.setActiveKeyId(activeKeyId);
        props.setSm4Keys(keys);
        final KeyServiceImpl svc = new KeyServiceImpl(props);
        svc.validateOnStartup();
        return svc;
    }

    private Map<String, String> keys(final String... kv) {
        final Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put(kv[i], kv[i + 1]);
        }
        return m;
    }

    @Test
    void getKeyId_returnsActiveKeyId() {
        final KeyService svc = newService("sm4-cred-v2", keys("sm4-cred-v2", GBT_KEY_HEX));
        assertThat(svc.getKeyId()).isEqualTo("sm4-cred-v2");
    }

    @Test
    void getActiveMasterKey_returnsSixteenBytes() {
        final KeyService svc = newService("sm4-cred-v2", keys("sm4-cred-v2", GBT_KEY_HEX));
        assertThat(svc.getSm4CredentialMasterKey()).hasSize(16);
    }

    @Test
    void getActiveMasterKey_returnsDefensiveCopy() {
        final KeyService svc = newService("sm4-cred-v2", keys("sm4-cred-v2", GBT_KEY_HEX));
        final byte[] first = svc.getSm4CredentialMasterKey();
        first[0] = (byte) 0xFF;
        assertThat(svc.getSm4CredentialMasterKey()[0]).isNotEqualTo((byte) 0xFF);
    }

    @Test
    void getMasterKeyByVersion_resolvesHistoricalKey() {
        final KeyService svc = newService("sm4-cred-v2",
                keys("sm4-cred-v2", GBT_KEY_HEX, "sm4-cred-v1", OLD_KEY_HEX));
        assertThat(svc.getSm4CredentialMasterKey("sm4-cred-v1")).hasSize(16);
        assertThat(svc.getSm4CredentialMasterKey("sm4-cred-v1"))
                .isNotEqualTo(svc.getSm4CredentialMasterKey("sm4-cred-v2"));
    }

    @Test
    void getMasterKeyByVersion_unknownKeyId_throws() {
        final KeyService svc = newService("sm4-cred-v2", keys("sm4-cred-v2", GBT_KEY_HEX));
        assertThatThrownBy(() -> svc.getSm4CredentialMasterKey("nonexistent"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void startup_activeKeyNotInMap_throws() {
        assertThatThrownBy(() -> newService("missing", keys("sm4-cred-v2", GBT_KEY_HEX)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void startup_keyNotSixteenBytes_throws() {
        assertThatThrownBy(() -> newService("bad", keys("bad", "00112233")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void sm2Methods_throwUnsupportedForS2Boundary() {
        final KeyService svc = newService("sm4-cred-v2", keys("sm4-cred-v2", GBT_KEY_HEX));
        assertThatThrownBy(svc::getSm2PublicKeyBase64)
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> svc.decryptLoginPassword("x", "y"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(svc::getSignPrivateKey)
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd /Users/muzhou/FEP_v1.0_wt-gm-s1 && JAVA_HOME=/usr/local/opt/openjdk@17 PATH=$JAVA_HOME/bin:$PATH ./mvnw test -Dtest=KeyServiceImplTest -pl fep-security-impl -am
```
期望: 编译失败 — `cannot find symbol: class KeyServiceImpl / FepSecurityKeyProperties`

- [ ] **Step 3: 编写配置绑定类**

```java
// fep-security-impl/src/main/java/com/puchain/fep/security/impl/key/FepSecurityKeyProperties.java
package com.puchain.fep.security.impl.key;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * SM4 主密钥配置绑定（前缀 {@code fep.security.sm4}）。
 *
 * <p><strong>真实密钥永不入 repo:</strong> {@code sm4Keys} 的值（hex 编码 16 字节）在
 * 生产环境经 environment variable / sealed key store / envelope-encrypted 配置部署期注入
 * （如 application-prod.yml 用 {@code ${FEP_SM4_KEY_V1}}）；dev/CI 用 GB/T 测试密钥。</p>
 *
 * <p>多版本：{@code sm4Keys} 以 keyId → hex 密钥保存历史版本，支持轮换期共存；
 * {@code activeKeyId} 指向当前活跃版本（新加密用）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "fep.security.sm4")
public class FepSecurityKeyProperties {

    /** 当前活跃密钥版本号（新加密使用）。 */
    private String activeKeyId;

    /** keyId → hex 编码 16 字节 SM4 密钥（多版本共存）。 */
    private Map<String, String> sm4Keys = new LinkedHashMap<>();

    public String getActiveKeyId() {
        return activeKeyId;
    }

    public void setActiveKeyId(final String activeKeyId) {
        this.activeKeyId = activeKeyId;
    }

    public Map<String, String> getSm4Keys() {
        return sm4Keys;
    }

    public void setSm4Keys(final Map<String, String> sm4Keys) {
        this.sm4Keys = sm4Keys == null ? new LinkedHashMap<>() : new LinkedHashMap<>(sm4Keys);
    }
}
```

- [ ] **Step 4: 编写 KeyServiceImpl**

```java
// fep-security-impl/src/main/java/com/puchain/fep/security/impl/key/KeyServiceImpl.java
package com.puchain.fep.security.impl.key;

import com.puchain.fep.security.api.KeyService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * SM4 主密钥多版本加载框架（PRD §3.3.4 密钥加载/版本/轮换）。
 *
 * <p>从 {@link FepSecurityKeyProperties} 加载 keyId → 16 字节 SM4 密钥的多版本映射，
 * 解码 hex 配置值（生产经 env/sealed store 注入，永不入 repo）。当前活跃版本由
 * {@code activeKeyId} 指向；历史版本供轮换期解密旧密文。</p>
 *
 * <p><strong>🔓 Mode A:</strong> 2026-06-07 muzhou 解禁国密域，AI 编写加载框架 + 密码学
 * 专项 review。真实密钥材料由密码设备生成、部署期注入，本类仅做加载/路由/校验。</p>
 *
 * <p><strong>S2 边界:</strong> SM2 相关方法（公钥分发/登录解密/签名私钥）抛
 * {@link UnsupportedOperationException}，属 S2 阶段（+ 架构 §0.3 决策门）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Service
public class KeyServiceImpl implements KeyService {

    /** SM4 密钥长度（字节）。 */
    private static final int SM4_KEY_LENGTH = 16;

    /** S2 边界提示。 */
    private static final String S2_PENDING =
            "SM2 key operations are pending S2 (see roadmap §0.3 sign-verify form decision)";

    private final String activeKeyId;
    private final Map<String, byte[]> keysByVersion;

    /**
     * 从配置构造，解码 hex 密钥为字节多版本映射。
     *
     * @param props SM4 密钥配置，非 null
     */
    public KeyServiceImpl(final FepSecurityKeyProperties props) {
        Objects.requireNonNull(props, "props");
        this.activeKeyId = props.getActiveKeyId();
        final Map<String, byte[]> decoded = new LinkedHashMap<>();
        props.getSm4Keys().forEach((keyId, hex) ->
                decoded.put(keyId, HexFormat.of().parseHex(hex)));
        this.keysByVersion = decoded;
    }

    /**
     * 启动期配置校验：activeKeyId 须存在于 keys map，每个密钥须 16 字节。
     *
     * @throws IllegalStateException 配置非法
     */
    @PostConstruct
    public void validateOnStartup() {
        if (activeKeyId == null || !keysByVersion.containsKey(activeKeyId)) {
            throw new IllegalStateException(
                    "fep.security.sm4.active-key-id [" + activeKeyId + "] not present in sm4Keys");
        }
        keysByVersion.forEach((keyId, key) -> {
            if (key.length != SM4_KEY_LENGTH) {
                throw new IllegalStateException("SM4 key [" + keyId + "] must be "
                        + SM4_KEY_LENGTH + " bytes (hex 32 chars), got " + key.length);
            }
        });
    }

    @Override
    public String getKeyId() {
        return activeKeyId;
    }

    @Override
    public byte[] getSm4CredentialMasterKey() {
        return keysByVersion.get(activeKeyId).clone();
    }

    @Override
    public byte[] getSm4CredentialMasterKey(final String keyId) {
        final byte[] key = keysByVersion.get(keyId);
        if (key == null) {
            throw new IllegalArgumentException("unknown SM4 key version: " + keyId);
        }
        return key.clone();
    }

    @Override
    public String getSm2PublicKeyBase64() {
        throw new UnsupportedOperationException(S2_PENDING);
    }

    @Override
    public String decryptLoginPassword(final String encryptedBase64, final String keyId) {
        throw new UnsupportedOperationException(S2_PENDING);
    }

    @Override
    public byte[] getSignPrivateKey() {
        throw new UnsupportedOperationException(S2_PENDING);
    }
}
```

- [ ] **Step 5: 运行测试确认通过**

```bash
cd /Users/muzhou/FEP_v1.0_wt-gm-s1 && JAVA_HOME=/usr/local/opt/openjdk@17 PATH=$JAVA_HOME/bin:$PATH ./mvnw test -Dtest=KeyServiceImplTest -pl fep-security-impl -am
```
期望: `BUILD SUCCESS`, 8 tests passed

- [ ] **Step 6: spotbugs 自检**（KeyServiceImpl 构造器存 `keysByVersion`(Map<String,byte[]>) → 可能 EI_EXPOSE_REP2；构造器内已 decode 新建 map 不存外部引用，但 byte[] 值经 getter clone 返回。先 compile -am 再 check，红线 `feedback_spotbugs_check_needs_recompile_after_annotation`）

```bash
cd /Users/muzhou/FEP_v1.0_wt-gm-s1 && JAVA_HOME=/usr/local/opt/openjdk@17 PATH=$JAVA_HOME/bin:$PATH ./mvnw compile spotbugs:check -pl fep-security-impl -am > /tmp/s1-t2-spotbugs.log 2>&1; echo "EXIT=$?"; grep -c "BugInstance" /tmp/s1-t2-spotbugs.log
```
期望: `EXIT=0`，BugInstance 0。**若报 EI_EXPOSE_REP2 于构造器**（FepSecurityKeyProperties 引用）：构造器已不持 props 引用（仅读取 decode），如仍报则于**构造器**加 `@SuppressFBWarnings(value="EI_EXPOSE_REP2", justification="props read-only at construction, decoded into private map; no external reference retained")`（注解打构造器非 class-level，红线 `feedback_spotbugs_check_needs_recompile_after_annotation`）。

- [ ] **Step 7: 提交**

```bash
cd /Users/muzhou/FEP_v1.0_wt-gm-s1 && git add fep-security-impl/src/ && git commit -m "$(cat <<'EOF'
feat(security): implement KeyServiceImpl SM4 multi-version key loading

SM4 master key loading framework (PRD §3.3.4): keyId→16-byte key map from
FepSecurityKeyProperties (hex-decoded; real keys env-injected at deploy, never
in repo). Active version + historical versions for rotation-period decrypt.
Startup config validation (active-key present + 16-byte length). SM2 methods
throw UnsupportedOperationException (S2 boundary per roadmap §0.3). 8 tests.

FR-INFRA-GM-KEYSVC. Authorized 2026-06-07 (Mode A + crypto review).
AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

### Task 3: 自动配置装配 + 生产配置 + API Javadoc 解禁更新 `模式 A`

**PRD 依据:** v1.3 §3.3.4 + §8.3（部署期密钥注入）
**追溯 ID:** FR-INFRA-GM-WIRING

**验收标准:**
1. `provider` 单开关互斥：impl beans `@ConditionalOnProperty(havingValue="impl")` + mock beans `@ConditionalOnProperty(havingValue="mock", matchIfMissing=true)` → 任一时刻只装配一套 CryptoService/KeyService bean。
2. 默认（无 provider 属性）/dev → mock 装配，impl 不装配；CI 全 reactor verify 仍 GREEN（零回归）。
3. `fep-web` 依赖 `fep-security-impl`（runtime scope），impl 类运行期可装配；fep-web source 仍仅引用 security.api（ArchUnit R5/BC 边界不破）。
4. `application-prod.yml` MERGE 进**既有** `fep:` 根块（实测 line 49），用 env 引用配密钥（`${FEP_SM4_KEY_*}`），**无任何明文真实密钥**。
5. `KeyService.java` SM4 两方法 Javadoc `⛔ Mode E` → 🔓 Mode A 解禁说明（密钥仍部署期注入不入 repo）。
6. **R7 SM2 阻断披露**：grep 实测 prod-active（非 @Profile dev）SM2 调用方清单；prod cutover gated on S2 / 确认 SM2 prod 未激活（S1 不强制 prod 切 impl）。

**Files:**
- Create: `fep-security-impl/src/main/java/com/puchain/fep/security/impl/GmSecurityImplAutoConfiguration.java`
- Create: `fep-security-impl/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Modify: `fep-security-mock/.../Mock{Crypto,Key,Sign}Service.java`（加 @ConditionalOnProperty 互斥）
- Modify: `fep-web/pom.xml`（加 fep-security-impl runtime 依赖）
- Modify: `fep-security-api/src/main/java/com/puchain/fep/security/api/KeyService.java`
- Modify: `fep-web/src/main/resources/application-prod.yml`

- [ ] **Step 1: 编写自动配置类**

```java
// fep-security-impl/src/main/java/com/puchain/fep/security/impl/GmSecurityImplAutoConfiguration.java
package com.puchain.fep.security.impl;

import com.puchain.fep.security.impl.key.FepSecurityKeyProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

/**
 * 国密真实实现装配 — 仅当 {@code fep.security.provider=impl} 时启用。
 *
 * <p>默认不激活（dev 走 fep-security-mock 的 {@code @Profile("dev")}），保证零回归。
 * 生产部署设 {@code fep.security.provider=impl} + 注入 {@code fep.security.sm4.*} 密钥后
 * 装配 CryptoServiceImpl / KeyServiceImpl（component-scan 本包 crypto/key）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "fep.security", name = "provider", havingValue = "impl")
@EnableConfigurationProperties(FepSecurityKeyProperties.class)
@ComponentScan(basePackages = {
        "com.puchain.fep.security.impl.crypto",
        "com.puchain.fep.security.impl.key"
})
public class GmSecurityImplAutoConfiguration {
}
```

> **说明:** `BouncyCastleGmProviderConfig`（S0，crypto 包，无 @ConditionalOnProperty）已总是注册 BC provider，与本装配正交；CryptoServiceImpl/KeyServiceImpl（@Service）由本类 `@ComponentScan` 收集，整个 autoconfig 受 `@ConditionalOnProperty(impl)` 门控 → 仅 impl 激活时建 bean（fep-security-impl 无独立 @SpringBootApplication，靠 auto-configuration imports 生效）。

- [ ] **Step 2: 注册 Spring Boot 3 自动配置**

```
# fep-security-impl/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
com.puchain.fep.security.impl.GmSecurityImplAutoConfiguration
```

- [ ] **Step 3: fep-web 依赖 fep-security-impl（R1，runtime scope 守 ArchUnit 边界）**

在 `fep-web/pom.xml` 的 `<dependencies>` 中 `fep-security-mock` 依赖后追加（runtime scope：impl 类运行期可装配，但 fep-web source 编译期不可见 impl 类 → 不会产生 callback→security.impl 的 source 依赖，守 ArchUnit R5 + BC 边界）：

```xml
        <dependency>
            <groupId>com.puchain</groupId>
            <artifactId>fep-security-impl</artifactId>
            <scope>runtime</scope>
        </dependency>
```
> **核实:** 加依赖后须确认 `CallbackModuleArchTest` R5（credential.crypto 不依赖 security.impl）+ S0 `GmSecurityImplArchTest`（非 impl 包不依赖 bouncycastle）仍 GREEN —— fep-web 仅经 Spring 装配 impl bean、source 仅 import security.api 接口，无字节码层 source 依赖，应安全（Step 6 verify 实测确认）。

- [ ] **Step 4: mock beans 加 provider 互斥开关（R3，默认 dev 仍 mock 零回归）**

给 `fep-security-mock` 的 `MockCryptoService` / `MockKeyService` / `MockSignService` 三个 `@Service @Profile("dev")` 类各追加一个 class-level 注解（保留既有 `@Profile("dev")` 不动）：

```java
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
// ... 类注解：
@Service
@Profile("dev")
@ConditionalOnProperty(prefix = "fep.security", name = "provider", havingValue = "mock", matchIfMissing = true)
public class MockCryptoService implements CryptoService {
```
> **互斥语义:** 无 provider 属性 → `matchIfMissing=true` mock 装配 / impl（havingValue=impl）不装配；`provider=impl` → mock 不匹配（mock≠impl）/ impl 装配 → **任一时刻单 bean**。`SecurityMockConfiguration`（@Configuration @Profile("dev")）保留不动（不创建 service bean）。

- [ ] **Step 5: 更新 KeyService SM4 方法 Javadoc（⛔ Mode E → 🔓 Mode A）**

在 `fep-security-api/src/main/java/com/puchain/fep/security/api/KeyService.java` 中，将 `getSm4CredentialMasterKey()`（行 ~65-71）与 `getSm4CredentialMasterKey(String keyId)`（行 ~85-89）Javadoc 内的 `⛔ Mode E` 段替换为：

```java
     * <p><strong>🔓 Mode A (2026-06-07 解禁):</strong> muzhou 授权 AI 进入国密域，本方法的
     * 真实实现 {@code KeyServiceImpl}（fep-security-impl）由 AI 编写 + 密码学专项 review。
     * 真实密钥材料仍由密码设备生成、部署期经 HSM/sealed key store/envelope-encrypted 配置
     * 注入，<strong>永不入 repo/git</strong>；dev/CI 用 GB/T 测试密钥。返回 16 字节
     * （GB/T 32907-2016 SM4 密钥长度）防御性副本，调用方不得跨单次加解密保留引用。</p>
```

> **注:** `getSignPrivateKey()` 的 ⛔ Mode E 保留不动（SM2 签名属 S2 + §0.3 决策门，仍未解锁实现细节）。仅改 SM4 两方法。

- [ ] **Step 6: R7 实测 — prod-active SM2 调用方清单**（披露 provider=impl 阻断面）

```bash
# zsh-safe：--include 加引号防 glob 扩展失败空跑（R2-2），显式列模块路径（落盘已 dry-run EXIT=0）
cd /Users/muzhou/FEP_v1.0_wt-gm-s1 && grep -rn --include="*.java" "decryptLoginPassword\|getSm2PublicKeyBase64\|getSignPrivateKey" fep-web/src/main/java fep-converter/src/main/java fep-transport/src/main/java fep-processor/src/main/java fep-collector/src/main/java | grep -v "fep-security-mock\|/api/KeyService.java\|/impl/key/KeyServiceImpl.java"
```
将命中调用方逐一标注其触发环境（@Profile dev 仅 / prod-active）。**实测结论（R7，已 grep 确认）：存在 3 个 prod-active SM2 调用方** —— `AuthController.getSm2PublicKeyBase64`（登录公钥分发）、`LoginVerifier.decryptLoginPassword`（登录解密）、`OutboundSignAdapter.getSignPrivateKey`（@Component 出站报文签名核心链路，**非 dev-gated**）。→ **application-prod.yml `provider: impl` 必须保持 commented**（Step 7），否则 `provider=impl` 使这 3 处抛 `UnsupportedOperationException`，出站签名 + 登录在 prod 崩溃。**prod cutover（取消注释 provider: impl）gated on S2 SM2 实现**。S1 仅交付 impl + wiring + 迁移能力 + IT 隔离验证，不激活 prod。

- [ ] **Step 7: 生产配置 MERGE 进既有 `fep:` 块（R5，env 引用，无真实密钥）**

`application-prod.yml` 第 49 行已有 `fep:` 根块 → **在该既有 `fep:` 块下** MERGE（禁追加第二个 `fep:` 根键，否则 SnakeYAML DuplicateKeyException / last-wins 覆盖既有配置）。在既有 `fep:` 块内增 `security:` 子节（若 `fep:` 下已有其他子键则并列追加）：

```yaml
fep:
  # ... 既有 fep 子配置（callback/transport 等，保持不动）...
  security:
    # ⚠️ R7/[R2-1]：provider: impl 翻转全 KeyService → SM2 方法（AuthController/LoginVerifier/
    # OutboundSignAdapter 出站签名）抛 UnsupportedOperationException，prod 出站签名+登录崩溃。
    # 故 S1 保持注释；prod cutover（取消下行注释）GATED ON S2（SM2 实现后方可启用）。
    # provider: impl
    sm4:
      active-key-id: ${FEP_SM4_ACTIVE_KEY_ID:sm4-cred-v1}
      sm4-keys:
        # hex 编码 16 字节 SM4 密钥，部署期经环境变量/sealed store 注入（密码设备生成），永不入 repo
        sm4-cred-v1: ${FEP_SM4_KEY_V1}
```
> S1 交付 sm4 密钥配置模板（forward-looking）+ provider 注释；S2 实现 SM2 后取消 `provider: impl` 注释完成 prod cutover。实施时先 `sed -n '45,60p' application-prod.yml` 看清既有 `fep:` 块缩进与子键，再手工 MERGE。

- [ ] **Step 8: 全 reactor verify 确认零回归（dev 默认 mock）**

```bash
cd /Users/muzhou/FEP_v1.0_wt-gm-s1 && JAVA_HOME=/usr/local/opt/openjdk@17 PATH=$JAVA_HOME/bin:$PATH ./mvnw verify -pl fep-security-impl,fep-web -am > /tmp/s1-t3-verify.log 2>&1; echo "VERIFY_EXIT=$?"; grep -E "BUILD SUCCESS|BUILD FAILURE|Tests run" /tmp/s1-t3-verify.log | tail -5
```
期望: `VERIFY_EXIT=0`；dev mock 仍生效、ArchUnit R5 + S0 BC 边界 GREEN、无双 bean 冲突。

- [ ] **Step 9: 提交**

```bash
cd /Users/muzhou/FEP_v1.0_wt-gm-s1 && git add fep-security-impl/src/ fep-security-mock/src/ fep-web/pom.xml fep-security-api/src/ fep-web/src/main/resources/application-prod.yml && git commit -m "$(cat <<'EOF'
feat(security): wire GM impl via provider switch + prod key config

Single fep.security.provider switch: impl beans @ConditionalOnProperty(impl) +
mock beans @ConditionalOnProperty(mock, matchIfMissing=true) — exactly one
CryptoService/KeyService bean active (default/dev still mock, zero regression).
fep-web runtime-depends on fep-security-impl (interface-only source refs keep
ArchUnit R5/BC boundary). application-prod.yml merges into existing fep: block,
SM4 keys via env (${FEP_SM4_KEY_V1}), no key material in repo. KeyService SM4
Javadoc ⛔ Mode E → 🔓 Mode A. R7: provider=impl prod cutover gated on S2.

FR-INFRA-GM-WIRING.
AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

### Task 4: callback_credential 惰性双读迁移组件 `模式 B`

**PRD 依据:** v1.3 §5.5.3 凭证配置 + §3.4.2（SM4 at-rest 加密）
**追溯 ID:** FR-INFRA-GM-CRED-MIGRATION

**验收标准:**
1. `CallbackLegacyCredentialMigrator.isLegacy(keyId)`：keyId ∈ 配置 `legacy-plaintext-key-ids`（默认 `["mock-key-v1"]`）→ true，否则 false。
2. `migrateToActiveKey(interfaceId)`（REQUIRES_NEW）：重读 entity；若已非 legacy → 直接 return（幂等/竞态安全）；否则对每个非 null 密文列把 legacy 字节（UTF-8 明文）经 `facade.encrypt` 重加密 → `entity.rotate(...)` 翻 key_id 到活跃版本 → save + `metrics.recordCredentialMigrated()`。
3. resolver：legacy 行读取立即返回 `new String(bytes, UTF_8)` 明文并触发迁移；真实行走 `facade.decrypt`。
4. resolver 依赖数守住 7（migrator 封装 legacyKeyIds 判别）。

**Files:**
- Create: `fep-web/src/main/java/com/puchain/fep/web/callback/credential/migration/CallbackLegacyCredentialKeyIdProperties.java`
- Create: `fep-web/src/main/java/com/puchain/fep/web/callback/credential/migration/CallbackLegacyCredentialMigrator.java`
- Modify: `fep-web/src/main/java/com/puchain/fep/web/callback/credential/service/CallbackCredentialResolver.java`
- Modify: `fep-web/src/main/java/com/puchain/fep/web/callback/metrics/CallbackMetrics.java`

- [ ] **Step 1: 编写失败测试（migrator 单元）**

```java
// fep-web/src/test/java/com/puchain/fep/web/callback/credential/migration/CallbackLegacyCredentialMigratorTest.java
package com.puchain.fep.web.callback.credential.migration;

import com.puchain.fep.web.callback.credential.crypto.CallbackCredentialEncryptionFacade;
import com.puchain.fep.web.callback.credential.crypto.CallbackCredentialEncryptionFacade.EncryptedCredential;
import com.puchain.fep.web.callback.credential.domain.CallbackCredentialEntity;
import com.puchain.fep.web.callback.credential.repository.CallbackCredentialRepository;
import com.puchain.fep.web.callback.metrics.CallbackMetrics;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * CallbackLegacyCredentialMigrator 判别 + 幂等 + 重加密回写单元测试。
 */
class CallbackLegacyCredentialMigratorTest {

    private final CallbackCredentialRepository repo = mock(CallbackCredentialRepository.class);
    private final CallbackCredentialEncryptionFacade facade =
            mock(CallbackCredentialEncryptionFacade.class);
    private final CallbackMetrics metrics = mock(CallbackMetrics.class);

    private CallbackLegacyCredentialMigrator newMigrator() {
        final CallbackLegacyCredentialKeyIdProperties props = new CallbackLegacyCredentialKeyIdProperties();
        props.setLegacyPlaintextKeyIds(List.of("mock-key-v1"));
        // provider="mock"：单测不经 Spring，@PostConstruct 不自动触发；C1 校验 IT(T5,provider=impl) 覆盖
        return new CallbackLegacyCredentialMigrator(repo, facade, metrics, props, "mock");
    }

    @Test
    void isLegacy_matchesConfiguredKeyId() {
        final CallbackLegacyCredentialMigrator migrator = newMigrator();
        assertThat(migrator.isLegacy("mock-key-v1")).isTrue();
        assertThat(migrator.isLegacy("sm4-cred-v1")).isFalse();
    }

    @Test
    void migrate_legacyTokenRow_reencryptsAndRotates() {
        final byte[] plainBytes = "secret-token".getBytes(StandardCharsets.UTF_8);
        final CallbackCredentialEntity entity = CallbackCredentialEntity.newToken(
                "iface-1", plainBytes, "Authorization", "mock-key-v1", null);
        when(repo.findByInterfaceId("iface-1")).thenReturn(Optional.of(entity));
        when(facade.encrypt("secret-token"))
                .thenReturn(new EncryptedCredential(new byte[]{9, 9, 9}, "sm4-cred-v1"));

        newMigrator().migrateToActiveKey("iface-1");

        assertThat(entity.getKeyId()).isEqualTo("sm4-cred-v1");
        verify(repo).save(entity);
        verify(metrics).recordCredentialMigrated();
    }

    @Test
    void migrate_alreadyMigratedRow_isIdempotentNoop() {
        final CallbackCredentialEntity entity = CallbackCredentialEntity.newToken(
                "iface-2", new byte[]{1, 2, 3}, "Authorization", "sm4-cred-v1", null);
        when(repo.findByInterfaceId("iface-2")).thenReturn(Optional.of(entity));

        newMigrator().migrateToActiveKey("iface-2");

        verify(repo, never()).save(any());
        verify(metrics, never()).recordCredentialMigrated();
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd /Users/muzhou/FEP_v1.0_wt-gm-s1 && JAVA_HOME=/usr/local/opt/openjdk@17 PATH=$JAVA_HOME/bin:$PATH ./mvnw test -Dtest=CallbackLegacyCredentialMigratorTest -pl fep-web -am
```
期望: 编译失败 — `cannot find symbol: class CallbackLegacyCredentialMigrator`

- [ ] **Step 3: 编写配置类**

```java
// fep-web/src/main/java/com/puchain/fep/web/callback/credential/migration/CallbackLegacyCredentialKeyIdProperties.java
package com.puchain.fep.web.callback.credential.migration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 惰性双读迁移的 legacy 明文 keyId 集合配置（前缀 {@code fep.callback.credential.migration}）。
 *
 * <p>mock 透传期写入的凭证 key_id 恒为 {@code "mock-key-v1"}，其密文列实为明文 UTF-8 字节。
 * 本集合标识哪些 key_id 应被当作 legacy 明文处理（读时双读 + 重加密）。默认
 * {@code ["mock-key-v1"]}。{@code active-key-id} 禁与本集合交集（否则真密文被误判明文）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
@ConfigurationProperties(prefix = "fep.callback.credential.migration")
public class CallbackLegacyCredentialKeyIdProperties {

    /** 被视为 legacy 明文的 key_id 集合。 */
    private List<String> legacyPlaintextKeyIds = new ArrayList<>(List.of("mock-key-v1"));

    public List<String> getLegacyPlaintextKeyIds() {
        return legacyPlaintextKeyIds;
    }

    public void setLegacyPlaintextKeyIds(final List<String> legacyPlaintextKeyIds) {
        this.legacyPlaintextKeyIds = legacyPlaintextKeyIds == null
                ? new ArrayList<>() : new ArrayList<>(legacyPlaintextKeyIds);
    }
}
```

- [ ] **Step 4: facade 暴露 activeKeyId（C1 交集校验依赖）**

在 `CallbackCredentialEncryptionFacade` 增方法（R8 合规：migrator 在 `credential.migration` ⊂ `credential..` 可依赖 facade；保持 facade 为凭证子系统对 KeyService 的唯一边界，migrator 不直接注入 KeyService）：

```java
    /**
     * 返回当前活跃密钥版本号（新加密使用），供迁移交集校验。
     *
     * @return 当前活跃 keyId
     */
    public String activeKeyId() {
        return keyService.getKeyId();
    }
```

- [ ] **Step 5: repo 增 legacy 行计数（C2 可观测）**

在 `CallbackCredentialRepository` 增派生查询：

```java
    /**
     * 统计 key_id 属于给定集合（legacy 明文标记）的凭证行数（迁移完成度观测）。
     *
     * @param keyIds legacy keyId 集合
     * @return 匹配行数
     */
    long countByKeyIdIn(java.util.Collection<String> keyIds);
```

- [ ] **Step 6: 编写 migrator**

```java
// fep-web/src/main/java/com/puchain/fep/web/callback/credential/migration/CallbackLegacyCredentialMigrator.java
package com.puchain.fep.web.callback.credential.migration;

import com.puchain.fep.web.callback.credential.crypto.CallbackCredentialEncryptionFacade;
import com.puchain.fep.web.callback.credential.crypto.CallbackCredentialEncryptionFacade.EncryptedCredential;
import com.puchain.fep.web.callback.credential.domain.CallbackCredentialEntity;
import com.puchain.fep.web.callback.credential.repository.CallbackCredentialRepository;
import com.puchain.fep.web.callback.metrics.CallbackMetrics;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * callback_credential 明文→SM4 密文惰性双读迁移器。
 *
 * <p>mock 透传期凭证以明文 UTF-8 字节存于密文列、key_id={@code mock-key-v1}。本迁移器在
 * 读路径（{@code CallbackCredentialResolver}）检测到 legacy key_id 时被触发：在
 * {@link Propagation#REQUIRES_NEW} 独立短事务中把明文字节重加密为真实 SM4 密文并
 * {@link CallbackCredentialEntity#rotate} 翻 key_id 到活跃版本。单行只迁一次（幂等）。</p>
 *
 * <p>判别器为 key_id（确定性，非 try-decrypt 猜测）；legacy 集合配置见
 * {@link CallbackLegacyCredentialKeyIdProperties}。</p>
 *
 * <p><strong>C1 安全不变量:</strong> 当 {@code fep.security.provider=impl}（真实加密）时，
 * 活跃 keyId 不得 ∈ legacyKeyIds（否则新密文被打上 legacy 标记→下次读当明文泄漏）。
 * {@link #assertActiveKeyNotLegacy} 启动校验 + {@link #migrateToActiveKey} 运行时二次守护。
 * mock 透传期 active==legacy（{@code mock-key-v1}）是预期且安全的（密文==明文），故校验仅 impl 生效。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class CallbackLegacyCredentialMigrator {

    private static final Logger LOG = LoggerFactory.getLogger(CallbackLegacyCredentialMigrator.class);

    private final CallbackCredentialRepository repo;
    private final CallbackCredentialEncryptionFacade facade;
    private final CallbackMetrics metrics;
    private final Set<String> legacyKeyIds;
    private final String provider;

    /**
     * 构造迁移器。
     *
     * @param repo     凭证仓储，非 null
     * @param facade   加解密 facade，非 null
     * @param metrics  回调指标门面，非 null
     * @param props    legacy keyId 配置，非 null
     * @param provider 当前安全 provider（mock/impl），决定 C1 校验是否生效
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring-managed singletons stored by reference per container contract")
    public CallbackLegacyCredentialMigrator(final CallbackCredentialRepository repo,
                                    final CallbackCredentialEncryptionFacade facade,
                                    final CallbackMetrics metrics,
                                    final CallbackLegacyCredentialKeyIdProperties props,
                                    @Value("${fep.security.provider:mock}") final String provider) {
        this.repo = Objects.requireNonNull(repo, "repo");
        this.facade = Objects.requireNonNull(facade, "facade");
        this.metrics = Objects.requireNonNull(metrics, "metrics");
        this.legacyKeyIds = new HashSet<>(props.getLegacyPlaintextKeyIds());
        this.provider = provider;
    }

    /**
     * C1 启动校验 + C2 可观测：impl 模式下活跃 keyId 不得为 legacy 标记；注册 legacy 剩余量 gauge + WARN 计数。
     *
     * @throws IllegalStateException impl 模式下活跃 keyId ∈ legacyKeyIds（误配致真密文泄漏）
     */
    @PostConstruct
    public void assertActiveKeyNotLegacy() {
        if ("impl".equals(provider) && legacyKeyIds.contains(facade.activeKeyId())) {
            throw new IllegalStateException("active keyId [" + facade.activeKeyId()
                    + "] must not be a legacy-plaintext marker when provider=impl");
        }
        metrics.registerLegacyCredentialGauge(() -> repo.countByKeyIdIn(legacyKeyIds));
        final long remaining = repo.countByKeyIdIn(legacyKeyIds);
        if (remaining > 0) {
            LOG.warn("callback_credential lazy migration pending: {} legacy-plaintext rows remaining "
                    + "(migrated on next read; cold interfaces stay plaintext until read)", remaining);
        }
    }

    /**
     * 判断 key_id 是否为 legacy 明文标记。
     *
     * @param keyId 凭证记录的 key_id
     * @return true 表示密文列实为明文字节
     */
    public boolean isLegacy(final String keyId) {
        return legacyKeyIds.contains(keyId);
    }

    /**
     * 将指定接口的 legacy 明文凭证重加密为真实 SM4 密文（独立事务 + 幂等）。
     *
     * @param interfaceId 接口标识
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void migrateToActiveKey(final String interfaceId) {
        final CallbackCredentialEntity entity = repo.findByInterfaceId(interfaceId).orElse(null);
        if (entity == null || !isLegacy(entity.getKeyId())) {
            return;
        }
        final EncryptedCredential token = reencrypt(entity.getTokenCiphertext());
        final EncryptedCredential clientId = reencrypt(entity.getOauthClientIdCiphertext());
        final EncryptedCredential clientSecret =
                reencrypt(entity.getOauthClientSecretCiphertext());
        final String activeKeyId = firstNonNullKeyId(token, clientId, clientSecret);
        if (legacyKeyIds.contains(activeKeyId)) {           // C1 运行时二次守护
            throw new IllegalStateException("re-encrypt produced legacy keyId [" + activeKeyId
                    + "]; refusing to write (misconfiguration)");
        }
        entity.rotate(
                token == null ? null : token.ciphertext(),
                clientId == null ? null : clientId.ciphertext(),
                clientSecret == null ? null : clientSecret.ciphertext(),
                activeKeyId);
        repo.save(entity);
        metrics.recordCredentialMigrated();
    }

    private EncryptedCredential reencrypt(final byte[] legacyPlaintextBytes) {
        if (legacyPlaintextBytes == null) {
            return null;
        }
        return facade.encrypt(new String(legacyPlaintextBytes, StandardCharsets.UTF_8));
    }

    private static String firstNonNullKeyId(final EncryptedCredential... candidates) {
        for (final EncryptedCredential c : candidates) {
            if (c != null) {
                return c.keyId();
            }
        }
        throw new IllegalStateException("legacy credential has no non-null ciphertext column");
    }
}
```

> **C2 主动收口（S1 follow-up backlog）:** 惰性 only 致冷接口明文长期滞留。S1 follow-up ticket「主动批量 sweep」= admin 端点/启动 runner 扫 `countByKeyIdIn(legacyKeyIds)>0` 的接口逐个 `migrateToActiveKey`，把明文滞留窗口收口；本 Plan 仅交付惰性 + gauge 可观测（muzhou 选定策略），sweep 独立 Plan。

- [ ] **Step 7: CallbackMetrics 增迁移计数 + legacy gauge（R4 实测惰性风格 + C2）**

实测 `CallbackMetrics` 用 `registry.counter(COUNTER_*).increment()` 惰性风格（无 Counter 字段），新增（对齐 `recordCredentialExpired` line 66-67）：

```java
    // 与既有 COUNTER_CREDENTIAL_EXPIRED（line 29）并列新增常量：
    static final String COUNTER_CREDENTIAL_MIGRATED = "fep_callback_credential_migrated_total";
    static final String GAUGE_CREDENTIAL_LEGACY_REMAINING = "fep_callback_credential_legacy_remaining";

    /**
     * 记录一次凭证惰性迁移（legacy 明文 → SM4 密文重加密）。
     */
    public void recordCredentialMigrated() {
        registry.counter(COUNTER_CREDENTIAL_MIGRATED).increment();
    }

    /**
     * 注册 legacy 明文凭证剩余量 gauge（迁移完成度观测）。
     *
     * @param remaining 当前 legacy 行数供应函数
     */
    public void registerLegacyCredentialGauge(final java.util.function.Supplier<Number> remaining) {
        io.micrometer.core.instrument.Gauge
                .builder(GAUGE_CREDENTIAL_LEGACY_REMAINING, remaining)
                .register(registry);
    }
```
> 实施时 grep `CallbackMetrics` 构造器确认 `registry` 字段名（MeterRegistry）与 import，按实测对齐（红线 `feedback_plan_must_grep_actual_api`）。

- [ ] **Step 8: resolver 惰性双读改造**

在 `CallbackCredentialResolver` 构造器注入 `CallbackLegacyCredentialMigrator migrator`（依赖 6→7，守上限），并改造 `resolveToken` / `resolveOAuth2`：

```java
    private AuthHeader resolveToken(final String interfaceId) {
        final CallbackCredentialEntity entity = repo.findByInterfaceId(interfaceId)
                .orElseThrow(() -> new CallbackCredentialMissingException(
                        "TOKEN credential missing for interfaceId=" + interfaceId));
        ensureNotExpired(entity);
        if (migrator.isLegacy(entity.getKeyId())) {
            final String plain = new String(
                    entity.getTokenCiphertext(), java.nio.charset.StandardCharsets.UTF_8);
            migrator.migrateToActiveKey(interfaceId);
            return new AuthHeader(entity.getTokenHeader(), plain);
        }
        final String plain = facade.decrypt(entity.getTokenCiphertext(), entity.getKeyId());
        return new AuthHeader(entity.getTokenHeader(), plain);
    }
```

`resolveOAuth2` 同款改造（legacy 分支对 `oauthClientIdCiphertext` / `oauthClientSecretCiphertext` 各 `new String(bytes, UTF_8)` 取明文 + 调一次 `migrator.migrateToActiveKey(interfaceId)` 整行迁移）：

```java
    private AuthHeader resolveOAuth2(final String interfaceId) {
        final Optional<String> cached = cache.get(interfaceId);
        if (cached.isPresent()) {
            return new AuthHeader(AUTHORIZATION_HEADER, BEARER_PREFIX + cached.get());
        }
        final CallbackCredentialEntity entity = repo.findByInterfaceId(interfaceId)
                .orElseThrow(() -> new CallbackCredentialMissingException(
                        "OAUTH2 credential missing for interfaceId=" + interfaceId));
        ensureNotExpired(entity);
        final String clientId;
        final String clientSecret;
        if (migrator.isLegacy(entity.getKeyId())) {
            clientId = new String(entity.getOauthClientIdCiphertext(),
                    java.nio.charset.StandardCharsets.UTF_8);
            clientSecret = new String(entity.getOauthClientSecretCiphertext(),
                    java.nio.charset.StandardCharsets.UTF_8);
            migrator.migrateToActiveKey(interfaceId);
        } else {
            clientId = facade.decrypt(entity.getOauthClientIdCiphertext(), entity.getKeyId());
            clientSecret =
                    facade.decrypt(entity.getOauthClientSecretCiphertext(), entity.getKeyId());
        }
        final CallbackOAuth2TokenResponse resp = oauthClient.fetchToken(
                entity.getOauthTokenEndpoint(), clientId, clientSecret, entity.getOauthScope());
        cache.put(interfaceId, resp.accessToken(),
                Duration.ofSeconds((long) resp.expiresIn() - OAUTH_SAFETY_MARGIN_SECONDS));
        return new AuthHeader(AUTHORIZATION_HEADER, BEARER_PREFIX + resp.accessToken());
    }
```

- [ ] **Step 9: 运行测试 + spotbugs 自检**

```bash
cd /Users/muzhou/FEP_v1.0_wt-gm-s1 && JAVA_HOME=/usr/local/opt/openjdk@17 PATH=$JAVA_HOME/bin:$PATH ./mvnw test -Dtest=CallbackLegacyCredentialMigratorTest -pl fep-web -am && JAVA_HOME=/usr/local/opt/openjdk@17 PATH=$JAVA_HOME/bin:$PATH ./mvnw compile spotbugs:check -pl fep-web -am > /tmp/s1-t4-spotbugs.log 2>&1; echo "EXIT=$?"; grep -c "BugInstance" /tmp/s1-t4-spotbugs.log
```
期望: migrator 测试 GREEN（3 passed）+ `EXIT=0` BugInstance 0（migrator 构造器 @SuppressFBWarnings 已加；facade.activeKeyId/repo.countByKeyIdIn 无新 finding）

- [ ] **Step 10: 提交**

```bash
cd /Users/muzhou/FEP_v1.0_wt-gm-s1 && git add fep-web/src/ && git commit -m "$(cat <<'EOF'
feat(callback): lazy dual-read migration of plaintext credentials to SM4

CallbackLegacyCredentialMigrator re-encrypts mock-passthrough plaintext credentials
(key_id=mock-key-v1) to real SM4 ciphertext on read (REQUIRES_NEW, idempotent,
key_id discriminator — no try-decrypt guess). Resolver dual-read: legacy rows
return UTF-8 plaintext + trigger migration; real rows SM4-decrypt. No Flyway
schema change (reuses key_id + existing columns). Migration counter metric.

FR-INFRA-GM-CRED-MIGRATION.
AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

### Task 5: 惰性迁移端到端集成测试 `模式 A`

**PRD 依据:** v1.3 §5.5.3 + §3.4.2
**追溯 ID:** FR-INFRA-GM-CRED-MIGRATION（验收）

**验收标准:**
1. 真实 impl bean（CryptoServiceImpl + KeyServiceImpl + facade）装配下，预置 legacy 行（key_id=mock-key-v1，密文列存明文字节）→ resolver 首次读返回正确明文 + DB 行 key_id 翻转为活跃版本 + 密文列变为真实 SM4 密文（≠ 原明文字节）。
2. 二次读：同一行走真实 SM4 解密路径，返回相同明文。
3. metrics 迁移计数 +1。

**Files:**
- Create: `fep-web/src/test/java/com/puchain/fep/web/callback/credential/migration/CallbackLegacyCredentialMigrationTest.java`

- [ ] **Step 1: 编写集成测试**（命名 `*Test` 非 `*IT` 确保 Surefire 收录 + verify 跑，CLAUDE.md 已知约束；@SpringBootTest + `@TestPropertySource(provider=impl)` → mock 互斥关闭、impl 装配，单 bean。R3 已解决双 bean）

```java
// fep-web/src/test/java/com/puchain/fep/web/callback/credential/migration/CallbackLegacyCredentialMigrationTest.java
package com.puchain.fep.web.callback.credential.migration;

import com.puchain.fep.web.callback.credential.domain.CallbackCredentialEntity;
import com.puchain.fep.web.callback.credential.repository.CallbackCredentialRepository;
import com.puchain.fep.web.callback.credential.service.CallbackCredentialResolver;
import com.puchain.fep.web.callback.credential.service.CallbackCredentialResolver.AuthHeader;
import com.puchain.fep.web.submission.outputinterface.domain.InterfaceAuthType;
import com.puchain.fep.web.submission.outputinterface.domain.SubOutputInterface;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 惰性双读迁移端到端：legacy 明文行 → 首读返回明文 + 重加密 + key_id 翻转 → 二读真实解密。
 *
 * <p>启用真实 impl provider（GB/T 测试密钥）覆盖 mock，验证迁移闭环。</p>
 */
@SpringBootTest
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "fep.security.provider=impl",
        "fep.security.sm4.active-key-id=sm4-cred-v1",
        "fep.security.sm4.sm4-keys.sm4-cred-v1=0123456789abcdeffedcba9876543210",
        "fep.callback.credential.migration.legacy-plaintext-key-ids=mock-key-v1"
})
class CallbackLegacyCredentialMigrationTest {

    @Autowired
    private CallbackCredentialRepository repo;

    @Autowired
    private CallbackCredentialResolver resolver;

    @Test
    void legacyTokenRow_lazilyMigratesToRealSm4OnFirstRead() {
        final byte[] plaintextBytes = "legacy-token-value".getBytes(StandardCharsets.UTF_8);
        final CallbackCredentialEntity legacy = CallbackCredentialEntity.newToken(
                "mig-iface-1", plaintextBytes, "Authorization", "mock-key-v1", null);
        repo.save(legacy);

        final SubOutputInterface target = mock(SubOutputInterface.class);
        when(target.getAuthType()).thenReturn(InterfaceAuthType.TOKEN);
        when(target.getInterfaceId()).thenReturn("mig-iface-1");

        // 首次读：双读返回明文 + 触发迁移
        final Optional<AuthHeader> first = resolver.resolveAuthHeader(target);
        assertThat(first).isPresent();
        assertThat(first.get().value()).isEqualTo("legacy-token-value");

        // DB 行已迁移：key_id 翻转 + 密文列真实加密（≠ 原明文字节）
        final CallbackCredentialEntity migrated = repo.findByInterfaceId("mig-iface-1").orElseThrow();
        assertThat(migrated.getKeyId()).isEqualTo("sm4-cred-v1");
        assertThat(migrated.getTokenCiphertext()).isNotEqualTo(plaintextBytes);

        // 二次读：真实 SM4 解密路径，返回相同明文
        final Optional<AuthHeader> second = resolver.resolveAuthHeader(target);
        assertThat(second).isPresent();
        assertThat(second.get().value()).isEqualTo("legacy-token-value");
    }
}
```

- [ ] **Step 2: 运行集成测试**

```bash
cd /Users/muzhou/FEP_v1.0_wt-gm-s1 && JAVA_HOME=/usr/local/opt/openjdk@17 PATH=$JAVA_HOME/bin:$PATH ./mvnw test -Dtest=CallbackLegacyCredentialMigrationTest -pl fep-web -am > /tmp/s1-t5-it.log 2>&1; echo "EXIT=$?"; tail -15 /tmp/s1-t5-it.log
```
期望: `EXIT=0`，1 test passed（首读明文 + key_id 翻转 + 二读真实解密全绿）

> **双 bean 已解决（R3）:** mock beans 加 `@ConditionalOnProperty(provider=mock, matchIfMissing=true)` 后，本测试 `provider=impl` 使 mock 不装配、impl 装配 → 单 `CryptoService`/`KeyService` bean，无 `NoUniqueBeanDefinitionException`。实施时仍 `tail` context 启动日志确认单 bean（migrator @PostConstruct C1 校验 provider=impl + active=sm4-cred-v1 ∉ legacy 通过）。

- [ ] **Step 3: 提交**

```bash
cd /Users/muzhou/FEP_v1.0_wt-gm-s1 && git add fep-web/src/test/ && git commit -m "$(cat <<'EOF'
test(callback): end-to-end lazy credential migration IT

Real impl provider (GB/T test key): legacy plaintext row → first read returns
plaintext + re-encrypts (key_id flips to sm4-cred-v1, ciphertext becomes real
SM4) → second read uses real SM4 decrypt path. Closes FR-INFRA-GM-CRED-MIGRATION.

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

### Task 6: 全 reactor 回归 + CLAUDE.md 状态更新 `模式 A`

**PRD 依据:** 元流程（无 FR-ID）

**验收标准:**
1. 全 reactor `verify` GREEN（强回归：fep-security-impl + fep-web 全测试 + ArchUnit + spotbugs；最低回归：fep-security-impl/fep-web 模块绿）。
2. CLAUDE.md「当前项目状态」追加 S1 条目。

**Files:**
- Modify: `CLAUDE.md`

- [ ] **Step 1: 全 reactor verify**（红线 `feedback_pipe_tail_deadlock_with_bg_bash`：redirect to file，禁 |tail；marker 判结果）

```bash
cd /Users/muzhou/FEP_v1.0_wt-gm-s1 && JAVA_HOME=/usr/local/opt/openjdk@17 PATH=$JAVA_HOME/bin:$PATH ./mvnw verify --batch-mode --no-transfer-progress > /tmp/s1-full-verify.log 2>&1; echo "FULL_VERIFY_EXIT=$?"; grep -E "BUILD SUCCESS|BUILD FAILURE|Tests run:" /tmp/s1-full-verify.log | tail -20
```
期望: `FULL_VERIFY_EXIT=0` + `BUILD SUCCESS`

> **⚠️ 并发护栏（CLAUDE.md「Surefire fork 限流」）:** 同一时刻只允许一个会话跑全量 verify。别会话（wt-msg-rule-engine / wt-simplify-q-drain）若在跑全量 → 本步改 `-pl fep-security-impl,fep-web -am` 单模块链 verify，避免 4 核 load 冲高。

- [ ] **Step 2: 更新 CLAUDE.md 状态**

在 CLAUDE.md「当前项目状态」快照追加 S1 里程碑（SM4 真实化 + KeyServiceImpl + 惰性双读迁移；新 FR-ID FR-INFRA-GM-SM4/-KEYSVC/-WIRING/-CRED-MIGRATION），并将国密 roadmap 进度 S0→S1。

- [ ] **Step 3: 提交**

```bash
cd /Users/muzhou/FEP_v1.0_wt-gm-s1 && git add CLAUDE.md && git commit -m "$(cat <<'EOF'
docs(claude): update project status for S1 SM4 + credential migration

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

> **注（红线 `feedback_fep_docs_repo_commit_taboo`）:** CLAUDE.md 若在 /Users/muzhou/FEP（非 git tracked）则仅 file-write 不 commit；若 worktree /Users/muzhou/FEP_v1.0_wt-gm-s1 内的 CLAUDE.md 是 git tracked 副本则 commit。实施时 `git status` 实测确认归属。

---

## 闭环（session-end + worktree teardown）

- [ ] PR 创建（base origin/main，标题 `feat(security): S1 SM4 + KeyServiceImpl + lazy credential migration`）；PR-size 若超 400 行向 muzhou 申请豁免或按 T1-T3(impl) / T4-T5(migration) 二拆 PR。
- [ ] GHA 三检（Build/Test/Quality + PR-Size + SonarCloud）GREEN → muzhou merge。
- [ ] worktree teardown（merge 后）：`git worktree remove /Users/muzhou/FEP_v1.0_wt-gm-s1` + 本地 main ff。
- [ ] session-end 四步（Simplify 三审 + 9 维文档含密码学专项 + Daily Report + PRD 矩阵 FR-INFRA-GM-* 转 ✅）。

---

## 自检清单（writing-plans 10 项，作者已逐条核对）

1. **PRD 覆盖度**: FR-INFRA-GM-SM4(§3.4.2) / -KEYSVC(§3.3.4) / -WIRING(§3.3.4) / -CRED-MIGRATION(§5.5.3) 全有 Task 覆盖。SM2 签验(§3.3)明示**不在本 Plan 范围**（S2 + §0.3 决策门，KeyServiceImpl SM2 方法抛 UnsupportedOperationException）。
2. **安全边界**: SM4/key/encrypt/decrypt 均 2026-06-07 解禁为 Mode A（非 ⛔ Mode E）；密码学专项 review 强制；真实密钥永不入 repo（env 注入）。getSignPrivateKey SM2 仍保留 ⛔（S2）。
3. **占位符扫描**: 无 TBD/TODO/待/类似 Task；所有 Step 含完整代码。
4. **类型一致性**: EncryptedCredential（facade 内嵌 record，ciphertext()/keyId()）、entity.rotate(4 参)、newToken(5 参)、InterfaceAuthType.TOKEN、新增 facade.activeKeyId()/repo.countByKeyIdIn(Collection)/CallbackMetrics.registry.counter 风格 — 均 grep 实测签名一致（实施时再核 CallbackMetrics registry 字段名）。
5. **测试命令可执行**: 各 `-Dtest=` 与测试类名匹配（CryptoServiceImplTest/KeyServiceImplTest/CallbackLegacyCredentialMigratorTest/CallbackLegacyCredentialMigrationTest，全 `*Test` 确保 Surefire 收录）。
6. **CLAUDE.md 更新**: T6 含。
7. **验收标准完整性**: 均从 GB/T 32907 / PRD §3.3.4/§3.4.2/§5.5.3 推导，断言值（GB/T 向量 hex `681edf34...`、16 字节、key_id 翻转）可手算/实测。
8. **共享工具类**: BouncyCastleGmProviderConfig(S0 复用) + FepSecurityKeyProperties 已登记。
9. **核心类职责边界**: KeyServiceImpl(依赖 1) + CallbackLegacyCredentialMigrator(依赖 5：repo/facade/metrics/props/provider) + resolver(改造后依赖 7：原 6 + migrator，migrator 封装 legacyKeyIds 守上限) 已声明。
10. **Worktree 触发**: 命中 ②⑤ → 头部已填 `wt-gm-s1` 路径+分支+建立命令；闭环含 `git worktree remove` 实测命令。

---

## 评审与签字

**Plan 状态:** 草稿 v0.3（v0.1→v0.2 修订 R1-R7 + C1-C3；**Round 2**：密码学专项 ✅ PASS 准予签字，santa REVISE 提 3 项 → v0.2→v0.3 修订 [R2-1]/[R2-2]/[R2-3]；待 Round 3 santa 确认 + muzhou 签字）。

### 修订记录 v0.2 → v0.3（Round 2 santa）

| # | 严重度 | 修订 |
|---|:------:|------|
| R2-1 | BLOCKER | T3 Step7 yml `provider: impl` 写成生效值与 Step6 gate 结论矛盾（实测 `OutboundSignAdapter` @Component 出站签名 prod-active + AuthController/LoginVerifier 登录链路）→ **provider: impl 整行注释化**，prod cutover gated on S2；S1 仅交付能力不激活 prod |
| R2-2 | MAJOR | T3 Step6 grep `--include=*.java` zsh 下 glob 失败空跑（放大 R2-1 误启用）→ 改 `--include="*.java"` 引号 + 显式模块路径；已 dry-run EXIT=0 实测 3 处命中 |
| R2-3 | MINOR | T1 Step6 commit body "6 tests" vs "(7 tests)" 矛盾 → 统一 7 |

> **密码学专项 Round 2 PASS:** C1 双层防御（@PostConstruct 启动校验 + 运行时二次守护）+ 汇聚点落地正确 + mock 豁免合理 + 启动顺序无风险；C2 缓解充分；C3 国标锚定实测对齐 S0。2 非阻断观察项（OBS-1 内存零化 / OBS-2 并发幂等测试）列 follow-up。

### 修订记录 v0.1 → v0.2

| # | 来源 | 严重度 | 修订 |
|---|------|:------:|------|
| R1 | santa | BLOCKER | fep-web 实测无 fep-security-impl 依赖 → T3 加 `fep-web/pom.xml` runtime 依赖 + 文件结构表登记 + verify R5/ArchUnit 不破（interface-only 引用，Spring 装配非 source 依赖）|
| R2 | santa | BLOCKER | `LegacyCredential*`（旧名）违反 ArchUnit R4（`callback..` 顶层类须 `Callback` 前缀）→ 全部重命名 `CallbackLegacyCredentialMigrator` / `CallbackLegacyCredentialKeyIdProperties`（+ 测试同步）|
| R3 | santa | BLOCKER | impl/mock 双 bean 冲突 → T3 给 3 个 mock `@Service` 加 `@ConditionalOnProperty(name="fep.security.provider", havingValue="mock", matchIfMissing=true)`（默认 dev 仍 mock，零回归）；impl `havingValue="impl"`；T5 IT `@ActiveProfiles("dev")`+`provider=impl` 单 bean |
| R4 | santa | MAJOR | CallbackMetrics 实测用 `registry.counter(COUNTER_*).increment()` 惰性风格（无 Counter 字段）→ T4 Step5 改为 `static final String COUNTER_CREDENTIAL_MIGRATED` + `registry.counter(...)` |
| R5 | santa | MAJOR | application-prod.yml 实测第 49 行已有 `fep:` 根 → T3 改 MERGE 进既有 `fep:` 块，禁追加第二根键 |
| R6 | santa | MINOR | baseline `9fa194f5` stale → 实测 origin/main=`b3b217f5`（§5.8 rule engine #71 merged，与安全正交）→ 头部 + worktree 命令更新 |
| R7 | santa | MAJOR | `provider=impl` 翻转**全** KeyService bean → SM2 方法（login/sign）抛 UnsupportedOperationException → T3 加 SM2 调用方 grep 实测 + 风险表行 + prod cutover 门禁（gated on S2 / 确认 prod SM2 未激活）|
| C1 | 密码学 | BLOCKER | active-key-id ∉ legacy-keyId 交集校验**仅声明未实现**且 KeyServiceImpl 架构上看不见 fep-web legacy 配置 → 落地到 `CallbackLegacyCredentialMigrator` @PostConstruct（provider=impl 时校验 `facade.activeKeyId()` ∉ legacyKeyIds）+ 修风险表/Javadoc drift |
| C2 | 密码学 | MAJOR | 惰性 only → 冷接口明文长期滞留 DB → 加 legacy 行计数 gauge + 启动 WARN 日志（可观测）+ 主动批量 sweep 列 S1 follow-up backlog |
| C3 | 密码学 | MINOR | T1 加自包含国标单块 NoPadding hex 断言（`681edf34...`，与 S0 同值）|

**密码学专项结论:** SM4 算法参数 / 密钥框架 / 密钥材料红线全部 PASS；唯一 BLOCKER = C1 交集校验未落地（已修）。

**评审输入:** 本 Plan 全文 + PRD §3.3.4/§3.4.2/§5.5.3 + GB/T 32907 + `docs/guides/plan-review-checklist.md` 7 项 + 密码学专项（算法参数/向量/密钥红线）。

**muzhou 签字:** ✅ **APPROVED 2026-06-08 muzhou**（审阅 v0.3 + 密码学专项 PASS + santa 三轮修订闭合后批准；hybrid 执行 — 主对话实施 edits+前台 mvn+commit / subagent 仅评审 spec+quality+密码学专项）。worktree `wt-gm-s1`(origin/main b3b217f5) 建立 + 执行。
