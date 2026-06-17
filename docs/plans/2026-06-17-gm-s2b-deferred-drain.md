# GM S2b Deferred 池 Drain Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: 实施时用 superpowers:executing-plans 逐 Task 推进；每 Task 完成派独立 spec + quality review subagent（红线 `feedback_task_review_discipline`）。

**Goal:** 清空 GM S2b（PR #103）Simplify 三审遗留的低风险 deferred 项（REUSE-1/2/3 + EFF-1/5 + QUAL-1~6），纯 refactor，**零运行期行为变更**。

**Architecture:** 把 `KeyServiceImpl` 与 `BcMessageSignPort` 两构造器重复的 peer-verify-keys 深拷贝骨架抽到 `PeerVerifyKeyMaps` 工具类（REUSE-1），同时让验签侧在**构造期一次性 `parseHex`** 预解码为 `byte[]`（EFF-1/5，消除每次 `verify` 的 hex 解析）；`FepSecuritySm2Properties` 三段同形 setter 归一到一个泛型 `copyOrEmpty` 私有 helper（REUSE-2）；测试侧 sm2p256v1 生成元常量上移到 `Sm2TestVectors`（REUSE-3）；Javadoc 澄清（QUAL）。EFF-2（双 unmarshal 审计）**本轮 scope-out**——`verify-inbound` 默认关、非热路径，留 cutover 时评。

**Tech Stack:** Java 17 / Spring Boot 3.x / Maven 多模块 / BouncyCastle（复用，不引新原语）/ JUnit 5 + AssertJ。

**执行 Worktree:** `E:\FEP_v1.0_wt-gm-s2b-drain`（分支 `refactor/gm-s2b-deferred-drain`，触发条件第 2 项「与已签字未执行 Plan 并存」+ 第 6 项「共享工作树多会话」/ MEMORY `shared-working-tree-needs-worktree`）

**PRD 追溯:** 元流程 / Simplify 技术债 drain，FR-ID 豁免（基础设施/元流程 Plan 除外条款）。来源 = PR #103（`fa4951b`）GM S2b Simplify 三审 POST-MERGE deferred 池（0 BLOCKER / 0 bug）。承接 PRD §3.3 报文签验链既有实现，不新增需求面。

**形态/安全:** 触及 `security/impl/` 但**零新密码学原语**——`parseHex` 从每次 `verify` 移到构造期、`SignService` 委托不变、SM2 算法参数不变。仍按国密评审网走：santa 双审 + **密码学专项 review**（确认无算法/密钥语义漂移）+ muzhou 签字。真实密钥永不入 repo（全程用 GB/T 32918.5-2017 附录 A 公开测试向量）。

**回归验收（两层，红线 `feedback_plan_regression_scope_explicit`）:**
- **Minimum（本地，每 Task）:** `./mvnw -pl fep-security-impl -o test`（REUSE/EFF Task）；QUAL Task 触 fep-web 则 `-pl fep-web -o test`（先确保上游 SNAPSHOT 已装，见下「构建注意」）。`spotbugs:check`（须 recompile，见红线 `feedback_spotbugs_check_needs_recompile_after_annotation`）。
- **Strong（GHA）:** PR 触发 Build/Test & Quality 全 reactor + SonarCloud，作为权威背书。

**构建注意（红线群）:**
- 共享 `~/.m2` 跨会话 clobber：离线（`-o`）构建遇「找不到符号/无 bean」**先证伪 baseline/.m2 漂移**（`git fetch` + `rev-parse HEAD origin/main`），再疑代码（`feedback_shared_m2_snapshot_cross_session_clobber`）。
- 单模块回归用 `-pl <module> -o test` **不带 `-am`**（`feedback_single_module_regression_no_am_flag`）；上游 SNAPSHOT 缺则先一次性 `-am install -DskipTests` 装 jar。
- worktree 内首次需 `-am install -DskipTests` 在 reactor 重建上游避 stale .m2。
- PowerShell：`-D` 点号参数须单引号 `'-Dtest=...'`；长跑 mvn `*> file.log` 禁 `|tail`（`feedback_pipe_tail_deadlock_with_bg_bash`）。

---

## Task 1: REUSE-1 + EFF-1/5 — `PeerVerifyKeyMaps` 工具 + 验签侧构造期预解码

**为什么合并:** EFF-1/5 把 `BcMessageSignPort` 的存储从 `List<String>` 改成预解码 `List<byte[]>`，会使其构造块不再与 `KeyServiceImpl` 字节级相同；故 REUSE-1 的「抽公共骨架」与 EFF-1/5 同一刀完成最干净——helper 暴露两个变体（hex 不可变副本 / byte[] 预解码副本），共享内部 `copy` 骨架。

**Files:**
- Create: `fep-security-impl/src/main/java/com/puchain/fep/security/impl/key/PeerVerifyKeyMaps.java`
- Create: `fep-security-impl/src/test/java/com/puchain/fep/security/impl/key/PeerVerifyKeyMapsTest.java`
- Modify: `fep-security-impl/src/main/java/com/puchain/fep/security/impl/key/KeyServiceImpl.java`（ctor L88-91 → helper）
- Modify: `fep-security-impl/src/main/java/com/puchain/fep/security/impl/sign/BcMessageSignPort.java`（字段 L32 + ctor L46-49 + verify L60-72）

**Step 1: 写 `PeerVerifyKeyMapsTest`（失败测试）**

```java
package com.puchain.fep.security.impl.key;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** PeerVerifyKeyMaps 深拷贝 + 预解码工具单测（REUSE-1 / EFF-1/5）。 */
class PeerVerifyKeyMapsTest {

    private static final String PUB = "04"
            + "09f9df311e5421a150dd7d161e4bc5c672179fad1833fc076bb08ff356f35020"
            + "ccea490ce26775a52dc6ea718cc1aa600aed05fbf35e084a6632f6072da9ad13";

    @Test
    void immutableHexCopy_isDeepAndUnmodifiable() {
        final Map<String, List<String>> src = new java.util.LinkedHashMap<>();
        src.put("N1", new ArrayList<>(List.of(PUB)));

        final Map<String, List<String>> copy = PeerVerifyKeyMaps.immutableHexCopy(src);

        assertThat(copy).containsOnlyKeys("N1");
        assertThat(copy.get("N1")).containsExactly(PUB);
        // 源后续 mutate 不影响副本（深拷贝）
        src.get("N1").clear();
        assertThat(copy.get("N1")).containsExactly(PUB);
        // 返回不可变
        assertThatThrownBy(() -> copy.get("N1").add("x"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void immutableHexCopy_nullList_becomesEmpty() {
        final Map<String, List<String>> src = new java.util.LinkedHashMap<>();
        src.put("N1", null);
        assertThat(PeerVerifyKeyMaps.immutableHexCopy(src).get("N1")).isEmpty();
    }

    @Test
    void decodedCopy_parsesHexToBytesOnce() {
        final Map<String, List<String>> src = Map.of("N1", List.of(PUB));
        final Map<String, List<byte[]>> decoded = PeerVerifyKeyMaps.decodedCopy(src);
        assertThat(decoded.get("N1")).hasSize(1);
        assertThat(decoded.get("N1").get(0))
                .containsExactly(java.util.HexFormat.of().parseHex(PUB));
    }

    @Test
    void decodedCopy_nullList_becomesEmpty() {
        final Map<String, List<String>> src = new java.util.LinkedHashMap<>();
        src.put("N1", null);
        assertThat(PeerVerifyKeyMaps.decodedCopy(src).get("N1")).isEmpty();
    }
}
```

**Step 2: 运行确认失败**

Run: `./mvnw -pl fep-security-impl -o test '-Dtest=PeerVerifyKeyMapsTest'`
Expected: 编译失败 `cannot find symbol: class PeerVerifyKeyMaps`。

**Step 3: 写 `PeerVerifyKeyMaps`**

```java
package com.puchain.fep.security.impl.key;

import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * {@code peer-verify-keys}（SrcNode → 对端验签公钥列表）配置的不可变深拷贝工具。
 *
 * <p>REUSE-1（GM S2b Simplify）：消除 {@link KeyServiceImpl} 与
 * {@code BcMessageSignPort} 两构造器重复的 deep-copy 骨架。两消费方需求不同形态：
 * 校验侧（KeyServiceImpl）保留 hex 字符串做启动期曲线点校验；验签侧
 * （BcMessageSignPort）构造期一次性 {@code parseHex} 预解码为 {@code byte[]}
 * 消除每次 {@code verify} 的 hex 解析（EFF-1/5）。骨架（迭代 + null 列表归空 +
 * 不可变化）共享，逐元素映射函数不同。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public final class PeerVerifyKeyMaps {

    private PeerVerifyKeyMaps() {
    }

    /**
     * SrcNode → 对端公钥 hex 列表的不可变深拷贝（null 列表 → 空列表）。
     * 校验侧用（{@link KeyServiceImpl#validateOnStartup()} 读 hex 验曲线点）。
     *
     * @param source live 配置 map（可含 null 列表值），非 null
     * @return 不可变深拷贝（key/value 均独立，源后续 mutate 不影响）
     */
    public static Map<String, List<String>> immutableHexCopy(
            final Map<String, List<String>> source) {
        return copy(source, List::copyOf);
    }

    /**
     * SrcNode → 对端公钥**已解码** {@code byte[]} 列表（构造期 {@code parseHex} 一次）。
     * 验签侧用（{@code BcMessageSignPort#verify} 直接喂 byte[] 给 SignService）。
     *
     * <p>预解码的 {@code byte[]} 不对外暴露（私有 final 字段 + verify 只读消费），
     * 无 EI 泄漏面。hex 合法性由 {@link KeyServiceImpl#validateOnStartup()} 先期
     * fail-fast 保证（keyService bean 的 {@code @PostConstruct} 在依赖它的
     * BcMessageSignPort 构造前完成）。</p>
     *
     * @param source live 配置 map（可含 null 列表值），非 null
     * @return SrcNode → 解码后 byte[] 列表的不可变 map
     */
    public static Map<String, List<byte[]>> decodedCopy(
            final Map<String, List<String>> source) {
        return copy(source, hexes -> hexes.stream()
                .map(hex -> HexFormat.of().parseHex(hex))
                .toList());
    }

    private static <V> Map<String, V> copy(final Map<String, List<String>> source,
            final Function<List<String>, V> valueMapper) {
        final Map<String, V> result = new LinkedHashMap<>();
        source.forEach((srcNode, hexes) ->
                result.put(srcNode, valueMapper.apply(hexes == null ? List.of() : hexes)));
        return result;
    }
}
```

**Step 4: 运行 helper 测试通过**

Run: `./mvnw -pl fep-security-impl -o test '-Dtest=PeerVerifyKeyMapsTest'`
Expected: PASS（4 tests）。

**Step 5: 改 `KeyServiceImpl` ctor（L88-91）**

把：
```java
        final Map<String, List<String>> peers = new LinkedHashMap<>();
        sm2Props.getPeerVerifyKeys().forEach((srcNode, hexes) ->
                peers.put(srcNode, hexes == null ? List.of() : List.copyOf(hexes)));
        this.peerVerifyKeys = peers;
```
换成：
```java
        this.peerVerifyKeys = PeerVerifyKeyMaps.immutableHexCopy(sm2Props.getPeerVerifyKeys());
```
> 注：`PeerVerifyKeyMaps` 与 `KeyServiceImpl` 同包（`...impl.key`），无需 import。删除不再使用的 `List`/`LinkedHashMap` import **当且仅当**其它地方不再引用（`KeyServiceImpl` 仍用 `LinkedHashMap`/`List` 于其它字段——保留 import，勿误删，编译会提示）。

**Step 6: 改 `BcMessageSignPort`（字段 + ctor + verify）**

> 本 Step 的 verify 代码块是**改造后目标代码**（非磁盘现状）。磁盘现状 L59-72 为 `List<String> pubHexes` + 循环内 `HexFormat.of().parseHex(pubHex)`；下方替换为 `List<byte[]> pubKeys` + 构造期已解码、循环内直接喂 byte[]。

字段 L32：`private final Map<String, List<String>> peerVerifyKeys;`
→ `private final Map<String, List<byte[]>> peerVerifyKeys;`

ctor L46-49：
```java
        final Map<String, List<String>> peers = new LinkedHashMap<>();
        sm2Props.getPeerVerifyKeys().forEach((srcNode, hexes) ->
                peers.put(srcNode, hexes == null ? List.of() : List.copyOf(hexes)));
        this.peerVerifyKeys = peers;
```
→
```java
        this.peerVerifyKeys = PeerVerifyKeyMaps.decodedCopy(sm2Props.getPeerVerifyKeys());
```

verify L59-72：
```java
    @Override
    public boolean verify(final byte[] data, final String signatureBase64, final String srcNode) {
        final List<byte[]> pubKeys = peerVerifyKeys.get(srcNode);
        if (pubKeys == null || pubKeys.isEmpty()) {
            throw new IllegalStateException(
                    "no peer verify public key configured for srcNode: " + srcNode);
        }
        // list 化抗轮换：任一已配置公钥验过即真（SM2 验签公开运算，try-each 无安全损失）。
        // 公钥已于构造期一次性 parseHex（EFF-1/5），verify 路径不再做 hex 解析。
        for (final byte[] pubKey : pubKeys) {
            if (signService.verify(data, signatureBase64, pubKey)) {
                return true;
            }
        }
        return false;
    }
```
import 调整：增 `import com.puchain.fep.security.impl.key.PeerVerifyKeyMaps;`；删 `import java.util.HexFormat;`（verify 不再用）+ `import java.util.LinkedHashMap;`（ctor 不再 new）；`List`/`Map`/`Objects` 保留。
ctor Javadoc L34-35 的「对端公钥深拷贝」改述为「对端公钥构造期预解码（byte[]，EFF-1/5）」。

**Step 7: 全模块单测确认零行为变更（回归网）**

Run: `./mvnw -pl fep-security-impl -o test`
Expected: PASS——尤其 `BcMessageSignPortTest`（6）+ `KeyServiceImplTest`（32）全绿（既有签验 roundtrip / try-each 轮换 / 未配置抛 ISE 行为逐字不变）。

**Step 8: spotbugs（须先 compile，红线 `spotbugs_check_needs_recompile_after_annotation`）**

Run: `./mvnw -pl fep-security-impl -o compile spotbugs:check`
Expected: `BugInstance size is 0`。重点确认 `List<byte[]>` 存储未触 `EI_EXPOSE_REP`（byte[] 不对外暴露、无 getter）。若触，加构造器/字段级 justified `@SuppressFBWarnings` 并复跑。

**Step 9: Commit**

```bash
git add fep-security-impl/src/main/java/com/puchain/fep/security/impl/key/PeerVerifyKeyMaps.java \
        fep-security-impl/src/test/java/com/puchain/fep/security/impl/key/PeerVerifyKeyMapsTest.java \
        fep-security-impl/src/main/java/com/puchain/fep/security/impl/key/KeyServiceImpl.java \
        fep-security-impl/src/main/java/com/puchain/fep/security/impl/sign/BcMessageSignPort.java
git commit -m "refactor(security): PeerVerifyKeyMaps 抽公共深拷贝 + 验签侧构造期预解码 (REUSE-1/EFF-1/5)

AI-Generated: claude-code
Reviewed-By: pending"
```

---

## Task 2: REUSE-2 — `FepSecuritySm2Properties` 三段 setter 归一

**Files:**
- Modify: `fep-security-impl/src/main/java/com/puchain/fep/security/impl/key/FepSecuritySm2Properties.java`（setLoginKeys L85-87 / setAuditKeys L126-128 / setMsgSignKeys L167-170）
- **Create**: `fep-security-impl/src/test/java/com/puchain/fep/security/impl/key/FepSecuritySm2PropertiesTest.java`（santa MAJOR-1：磁盘实测该测试类**不存在**，须新建整文件——非"补充既有"）

**Step 1: 新建 `FepSecuritySm2PropertiesTest`（整文件，锁定归一前行为基线）**

```java
package com.puchain.fep.security.impl.key;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** FepSecuritySm2Properties 三段 setter 防御拷贝 + null-guard 归一基线（REUSE-2）。 */
class FepSecuritySm2PropertiesTest {

    @Test
    void setLoginKeys_null_becomesEmptyMutableMap() {
        final FepSecuritySm2Properties p = new FepSecuritySm2Properties();
        p.setLoginKeys(null);
        assertThat(p.getLoginKeys()).isEmpty();
        // 仍可 relaxed-binding mutate（live 引用）
        p.getLoginKeys().put("k", new FepSecuritySm2Properties.LoginKeyPair());
        assertThat(p.getLoginKeys()).containsKey("k");
    }

    @Test
    void setAuditKeys_null_becomesEmptyMutableMap() {
        final FepSecuritySm2Properties p = new FepSecuritySm2Properties();
        p.setAuditKeys(null);
        assertThat(p.getAuditKeys()).isEmpty();
        p.getAuditKeys().put("k", new FepSecuritySm2Properties.LoginKeyPair());
        assertThat(p.getAuditKeys()).containsKey("k");
    }

    @Test
    void setMsgSignKeys_isDefensiveCopy() {
        final FepSecuritySm2Properties p = new FepSecuritySm2Properties();
        final Map<String, FepSecuritySm2Properties.LoginKeyPair> src = new LinkedHashMap<>();
        src.put("k", new FepSecuritySm2Properties.LoginKeyPair());
        p.setMsgSignKeys(src);
        src.clear();                       // 源清空不应影响已 set 的副本
        assertThat(p.getMsgSignKeys()).containsKey("k");
    }
}
```

**Step 2: 运行确认新建测试编译 + 绿（归一前行为基线已锁定）**

Run: `./mvnw -pl fep-security-impl -o test '-Dtest=FepSecuritySm2PropertiesTest'`
Expected: PASS（3 tests；此时 setter 尚未归一，测试锁定既有行为，归一后须仍绿）。

**Step 3: 抽私有泛型 helper + 三 setter 改用**

在类尾（`LoginKeyPair` 内部类之前）加：
```java
    /**
     * Map 防御拷贝 + null guard 归一（REUSE-2）：三段 SM2 密钥 setter 共用。
     * 返回**可变** LinkedHashMap——Spring relaxed binding 经 getter 取 live 引用
     * 逐 key 填充，故不可返回不可变副本。
     *
     * @param source 入参 map（可 null）
     * @param <V>    值类型
     * @return source 的可变浅拷贝；source 为 null 时返回新空 map
     */
    private static <V> Map<String, V> copyOrEmpty(final Map<String, V> source) {
        return source == null ? new LinkedHashMap<>() : new LinkedHashMap<>(source);
    }
```
三 setter 体改为：
```java
    public void setLoginKeys(final Map<String, LoginKeyPair> loginKeys) {
        this.loginKeys = copyOrEmpty(loginKeys);
    }
    // setAuditKeys / setMsgSignKeys 同形
```
> `setPeerVerifyKeys`（L191-198，深拷贝 List 值）**不动**——它是两层深拷贝，语义不同，强行归一会降清晰度（YAGNI）。Javadoc 保留各 setter「与 setLoginKeys 对称」表述。

**Step 4: 运行测试通过**

Run: `./mvnw -pl fep-security-impl -o test '-Dtest=FepSecuritySm2PropertiesTest,KeyServiceImplTest'`
Expected: PASS（归一零行为变更）。

**Step 5: spotbugs**

Run: `./mvnw -pl fep-security-impl -o compile spotbugs:check`
Expected: `BugInstance size is 0`（既有 `@SuppressFBWarnings(EI_EXPOSE_REP)` getter 不受影响）。

**Step 6: Commit**

```bash
git add fep-security-impl/src/main/java/com/puchain/fep/security/impl/key/FepSecuritySm2Properties.java \
        fep-security-impl/src/test/java/com/puchain/fep/security/impl/key/FepSecuritySm2PropertiesTest.java
git commit -m "refactor(security): SM2 三段 setter 归一 copyOrEmpty helper (REUSE-2)

AI-Generated: claude-code
Reviewed-By: pending"
```

---

## Task 3: REUSE-3 — sm2p256v1 生成元常量上移 `Sm2TestVectors`

**Files:**
- Modify: `fep-security-impl/src/test/java/com/puchain/fep/security/impl/key/Sm2TestVectors.java`（加常量）
- Modify: `fep-security-impl/src/test/java/com/puchain/fep/security/impl/sign/BcMessageSignPortTest.java`（删内联 L29-32，引用 `Sm2TestVectors`）

**Step 1: 加常量到 `Sm2TestVectors`**

在 `GBT_SIGN_RS_HEX` 后插入：
```java
    /**
     * sm2p256v1 标准生成元 G（04∥Gx∥Gy，130 hex）——合法曲线点但非任何签名方公钥。
     * 用于 try-each 抗轮换测试：作"已配置但不匹配"的对端公钥（启动校验过、验签不过）。
     */
    public static final String SM2_GENERATOR_HEX =
            "04" + "32c4ae2c1f1981195f9904466a39c9948fe30bbff2660be1715a4589334c74c7"
                 + "bc3736a2f4f6779c59bdcee36b692153d0a9877cc62a474002df32e52139f0a0";
```

**Step 2: `BcMessageSignPortTest` 删内联常量 L29-32，改引用**

删除：
```java
    /** sm2p256v1 标准生成元 G（04∥Gx∥Gy）= 合法曲线点、非 GBT 签名方公钥。 */
    private static final String SM2_GENERATOR_HEX =
            "04" + "32c4ae2c1f1981195f9904466a39c9948fe30bbff2660be1715a4589334c74c7"
                 + "bc3736a2f4f6779c59bdcee36b692153d0a9877cc62a474002df32e52139f0a0";
```
L85 / L92 的 `SM2_GENERATOR_HEX` → `Sm2TestVectors.SM2_GENERATOR_HEX`（`Sm2TestVectors` 已 import）。

**Step 3: 运行测试通过（值逐字节不变 → 行为不变）**

Run: `./mvnw -pl fep-security-impl -o test '-Dtest=BcMessageSignPortTest'`
Expected: PASS（6 tests，含 `verify_tryEachRotation_secondKeyMatches` / `verify_allConfiguredKeysWrong_returnsFalse`）。

**Step 4: Commit**

```bash
git add fep-security-impl/src/test/java/com/puchain/fep/security/impl/key/Sm2TestVectors.java \
        fep-security-impl/src/test/java/com/puchain/fep/security/impl/sign/BcMessageSignPortTest.java
git commit -m "test(security): sm2p256v1 生成元常量上移 Sm2TestVectors (REUSE-3)

AI-Generated: claude-code
Reviewed-By: pending"
```

---

## Task 4: QUAL-1~6 — Javadoc polish + EFF-2 deferred 注记

**纯文档，零代码逻辑变更。** 不跑测试逻辑，仅编译 + checkstyle。

**Files:**
- Modify: `fep-security-impl/.../sign/BcMessageSignPort.java`（verify try-each 语义注 / 失败路径 doc）
- Modify: `fep-security-impl/.../key/KeyServiceImpl.java`（Roadmap §3 形态 C-ev 交叉引用澄清）
- Modify: `fep-web/.../messageinbound/service/InboundMessageDispatcher.java`（`extractSrcNode`↔`tryUnmarshalBody` 双 unmarshal 交叉引用 + EFF-2 deferred 注记）

**Step 1: `InboundMessageDispatcher` 双 unmarshal 交叉引用 + EFF-2 注**

> 行号锚（santa MINOR-4）：`extractSrcNode` 方法 Javadoc L256-264 / 方法体 L265-278；`tryUnmarshalBody` 方法 Javadoc L280-307 / 方法体 L308-345。两处 Javadoc 互加 `{@link}` 交叉引用。

在 `extractSrcNode` Javadoc（L256-264）尾加一句，并在 `tryUnmarshalBody` Javadoc 互指：
```
     * <p>EFF-2（GM S2b Simplify，deferred）：{@code verify-inbound=true} 时本方法与
     * {@link #tryUnmarshalBody} 各 unmarshal 一次同一 xml。当前 {@code verify-inbound}
     * 默认关、非热路径，单次提取复用留 cutover 启用验签后评估，不在本轮 drain 范围。</p>
```

**Step 2: `BcMessageSignPort.verify` 失败路径 + try-each 语义注**

补充 verify 方法 Javadoc（明确「全部公钥验不过返回 false 而非抛异常」「未配置 srcNode 抛 ISE」两条失败语义对比），并交叉引用 `KeyServiceImpl.validatePeerVerifyKeys` 启动 fail-fast。

**Step 3: `KeyServiceImpl` Roadmap 注澄清**

`getSignPrivateKey` Javadoc 补「形态 C-ev / MessageSignPort 隔离」交叉引用一句（与类级 §GM S2b 段呼应，避免读者只看方法不知形态边界）。

**Step 4: 编译 + checkstyle（FileLength 以 checkstyle 报数为准）**

Run: `./mvnw -pl fep-security-impl -o compile checkstyle:check`
Expected: BUILD SUCCESS，0 checkstyle violation。
（dispatcher 在 fep-web：`./mvnw -pl fep-web -o checkstyle:check`，须先 `-am install -DskipTests` 装上游 SNAPSHOT。）

**Step 5: Commit**

```bash
git add fep-security-impl/src/main/java/com/puchain/fep/security/impl/sign/BcMessageSignPort.java \
        fep-security-impl/src/main/java/com/puchain/fep/security/impl/key/KeyServiceImpl.java \
        fep-web/src/main/java/com/puchain/fep/web/messageinbound/service/InboundMessageDispatcher.java
git commit -m "docs(security): 验签失败路径/双 unmarshal/形态 C-ev Javadoc 澄清 + EFF-2 deferred 注 (QUAL-1~6)

AI-Generated: claude-code
Reviewed-By: pending"
```

---

## 收尾（session-end 时统一做，非本 Plan 内）

- Simplify 三审（reuse/quality/efficiency）→ applied + deferred 分类。本 Plan 自身即 drain，预期新增 deferred 极少。
- EFF-2 维持 deferred（cutover 时评，已落 dispatcher Javadoc 注）。
- 8/9 维技术文档（有 code commit，不豁免——红线 `feedback_infra_plan_still_needs_full_8dim_docs`）。
- Daily Report（含 §教训）。
- worktree teardown：`git worktree remove E:\FEP_v1.0_wt-gm-s2b-drain`（红线闭环纪律）。
- PR → GHA Build/Test & Quality + SonarCloud 绿 → muzhou squash merge → main ff → 分支删。

## 评审网（实施前）

1. **santa Plan 评审** — ✅ **Round 1 PASS-WITH-MINOR**（2026-06-17，0 BLOCKER）。MAJOR-1（FepSecuritySm2PropertiesTest 磁盘不存在→改 Create 整文件，v0.2 已修）+ MAJOR-2（verify 块标注改造后目标代码，v0.2 已修）+ MINOR-4（Task 4 tryUnmarshalBody 行号锚，v0.2 已补）。
2. **密码学专项 review** — ✅ **CRYPTO-PASS**（2026-06-17）。逐项实测：parseHex 构造期 vs verify 期字节等价（HexFormat 无状态纯函数）/ try-each 抗轮换语义保序保持 / fail-fast 序安全（KeyServiceImpl @PostConstruct 先于 BcMessageSignPort 构造）/ 预解码 byte[] 私有 final 无 EI 泄漏 / SM2 曲线·签名编码·原语零漂移 / 全程 GB/T 公开向量无真实密钥。
3. **muzhou 签字**（一票否决）— ✅ **APPROVED 2026-06-17**（v0.2，经 AskUserQuestion「签字 → 建 worktree 实施」）。

### 修订记录
- **v0.1**（2026-06-17）起草。
- **v0.2**（2026-06-17）santa Round 1 + crypto review 后修订：Task 2 测试类改 Create 整文件 + 3 断言（MAJOR-1）；Task 1 Step 6 标注改造前后对比（MAJOR-2）；Task 4 Step 1 补 tryUnmarshalBody 行号锚（MINOR-4）。

## 风险与缓解

- **R1（EFF-1/5 启动顺序）:** BcMessageSignPort 构造期 `parseHex` 依赖 hex 已合法。缓解：keyService bean `@PostConstruct validateOnStartup` 在 BcMessageSignPort 构造前完成（Spring 依赖序），且 parseHex 对 130-hex 校验过值不抛。Plan 已 Javadoc 记录；review 复核 GmSecurityConfiguration bean 依赖序。
- **R2（spotbugs EI_EXPOSE on `List<byte[]>`）:** byte[] 可变。缓解：私有 final + 无 getter + verify 只读消费；若 spotbugs 仍报则 justified 抑制（Step 8 已留）。
- **R3（零行为变更证明）:** 既有 6+32 测试为回归网；任一行为漂移即 RED。每 Task `-pl fep-security-impl -o test` 全绿为硬门。
