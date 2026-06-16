# B4 — DzpzInfo3000FieldMapper（3000 电子凭证信息登记 数仓模式 collector mapper）实施计划

> **For Claude:** REQUIRED SUB-SKILL: 用 superpowers:executing-plans 逐 Task 实施。
> **治理前置（FEP 强制）:** 本 Plan 须经 santa-method 独立评审 + muzhou 签字后方可进入实施（无签字禁实施）。
>
> **修订记录:**
> - v0.1（2026-06-16）起草。
> - v0.2（2026-06-16）santa Round1 REVISE 闭环：修 BLOCKER-1（XsdComplianceHelper.buildCfxEnvelope switch 缺 3000 case → Task3 Step0 显式补 `case "3000"→RealHead3000(RequestHead)`，3000.xsd:31 实测确认）+ MINOR M1（Task3 Step2「实施期决定」改为评审期已定）。
>
> **✅ muzhou 签字 APPROVED（2026-06-16）** —— santa Round1 REVISE 闭环 + 修复自验（3000.xsd:31 实测），Round2 形式化跳过，准予进入实施。`Reviewed-By: muzhou`。

**执行 Worktree:** `E:\FEP_v1.0_wt-b4-dzpz3000`（分支 `feat/b4-dzpzinfo3000-field-mapper`，触发条件第 2 项「与已签字未执行的 Plan 并存」+ 多会话并发；基于 origin/main `900bfac`）

**Goal:** 为 3000 电子凭证信息登记报文补齐数仓模式（MODE_3）collector field mapper，闭合 `Mode3Routes` 唯一缺失的一条路由。

**Architecture:** 3000 报文的 outbound 链路（Body POJO `DzpzInfo3000` / MessageType / MessageDirectionMap MODE_3 OUTBOUND_ACTIVE / 3000.xsd / OutboundWireShapeDispatcher / BodyClassRegistry / §5.8 规则母本）**均已实装**（P4-MSG-B T4，2026-05-08 ship）。本 Plan 仅补数仓模式下「从行内采集的 rawData Map 组装 `DzpzInfo3000` body」的 `FieldMapper` 实现 + `Mode3Routes` 注册 + 真 XsdValidator on SUT 合规测试。实现与既有 `QyRegister3109FieldMapper`（4 必填标量 + 嵌套可选 stub）**完全同型**。

**Tech Stack:** Java 17 / Spring Boot 3.x / JUnit 5 + AssertJ / JAXB / fep-collector + fep-web 模块。

**PRD 追溯:** FR-MSG-3000（PRD §4.6 行1「3000 电子凭证信息登记」+ §2.2 数仓模式 + §3.2 报文体）。

**Scope 边界（YAGNI）:**
- ✅ 4 必填标量映射：SerialNo / SendNodeCode / DesNodeCode / ApplyMode
- ⏸ `pzInfo`（PzInfo）/ `extInfo`（ExtInfo）两个 `minOccurs="0"` 嵌套块**留 stub**（镜像 3109 先例，业务深化 Plan 再补 raw→嵌套映射）；本 Plan 不调对应 setter
- ⏸ inbound 受理 / 回执：3000 系 OUTBOUND_ACTIVE 模式3 异步无回执，N/A

**⚠️ 母本值域约束（红线 `feedback_rule_master_plan_prescan_fixture_value_domain` — 刚补对账本的 Q-F1 同源教训）:**
`ApplyMode` 受 §5.8 母本约束 —— 表 5.1.7-22 凭证登记业务分类 allowed = **{1, 2}**（实测 `RuleMasterSupplyChainCodesTest`：legal=2 / illegal=3；生产 `application.yml:104-106`）。**所有 fixture 的 ApplyMode 必须取 "1" 或 "2"**，否则绑生产 yml 流水线 IT 触 PROC_8507。本 Plan fixture 统一用 `ApplyMode="1"`（XSD + 母本双合法）。

---

### Task 1: DzpzInfo3000FieldMapper（fep-collector）

**Files:**
- Create: `fep-collector/src/main/java/com/puchain/fep/collector/assembler/mapper/DzpzInfo3000FieldMapper.java`
- Test: `fep-collector/src/test/java/com/puchain/fep/collector/assembler/mapper/DzpzInfo3000FieldMapperTest.java`
- 参照先例: `QyRegister3109FieldMapper.java`（结构母版）+ `RzReturnInfo3009FieldMapperTest.java`（测试母版）

**Step 1: 写失败单测**

```java
package com.puchain.fep.collector.assembler.mapper;

import com.puchain.fep.collector.CollectorProperties;
import com.puchain.fep.common.exception.FepBusinessException;
import com.puchain.fep.common.util.FepConstants;
import com.puchain.fep.processor.body.supplychain.DzpzInfo3000;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DzpzInfo3000FieldMapperTest {

    private static final String VALID_INSTITUTION_CODE = "A1000143000999";

    private CollectorProperties props;
    private DzpzInfo3000FieldMapper mapper;

    @BeforeEach
    void setup() {
        props = new CollectorProperties();
        props.setInstitutionCode(VALID_INSTITUTION_CODE);
        mapper = new DzpzInfo3000FieldMapper(props);
    }

    @Test
    void happyPath_allRequired_shouldFillBody() {
        DzpzInfo3000 body = (DzpzInfo3000) mapper.toMessageBody(baseRequired());

        assertThat(body.getSerialNo()).isNotBlank().hasSize(30);
        assertThat(body.getSendNodeCode()).isEqualTo(VALID_INSTITUTION_CODE);
        assertThat(body.getDesNodeCode()).isEqualTo(FepConstants.HNDEMP_NODE_CODE);
        assertThat(body.getApplyMode()).isEqualTo("1");   // 母本 {1,2} 合法
        // 嵌套可选块本 Plan 不映射 → null
        assertThat(body.getPzInfo()).isNull();
        assertThat(body.getExtInfo()).isNull();
    }

    @Test
    void missingApplyMode_shouldThrow() {
        Map<String, Object> raw = baseRequired();
        raw.remove("apply_mode");
        assertThatThrownBy(() -> mapper.toMessageBody(raw))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("missing required field for 3000")
                .hasMessageContaining("applyMode");
    }

    @Test
    void serialNoMissing_shouldFallbackToUuid32() {
        Map<String, Object> raw = baseRequired();
        raw.remove("serial_no");
        DzpzInfo3000 body = (DzpzInfo3000) mapper.toMessageBody(raw);
        assertThat(body.getSerialNo()).isNotNull().hasSize(30);
    }

    @Test
    void institutionCodeMissing_shouldThrow() {
        props.setInstitutionCode(null);
        assertThatThrownBy(() -> mapper.toMessageBody(baseRequired()))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("missing required field for 3000: sendNodeCode");
    }

    @Test
    void institutionCodeInvalidLength_shouldThrow() {
        props.setInstitutionCode("SHORT");
        assertThatThrownBy(() -> mapper.toMessageBody(baseRequired()))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("institutionCode length must be 14");
    }

    @ParameterizedTest
    @CsvSource({"1", "2"})
    void applyMode_acceptsMubonLegalValues(String legal) {
        Map<String, Object> raw = baseRequired();
        raw.put("apply_mode", legal);
        DzpzInfo3000 body = (DzpzInfo3000) mapper.toMessageBody(raw);
        assertThat(body.getApplyMode()).isEqualTo(legal);
    }

    private static Map<String, Object> baseRequired() {
        Map<String, Object> raw = new HashMap<>();
        raw.put("serial_no", "FIX3000SERIAL00000000000000001");  // 30 chars
        raw.put("apply_mode", "1");                               // 母本 {1,2}
        return raw;
    }
}
```

**Step 2: 跑测试确认失败**

Run（红线 `feedback_single_module_regression_no_am_flag`：单模块 `-o` 不带 `-am`；前缀 JAVA_HOME 红线 `feedback_bg_bash_path_inheritance`）:
```bash
cd /e/FEP_v1.0_wt-b4-dzpz3000 && ./mvnw -pl fep-collector -o test -Dtest=DzpzInfo3000FieldMapperTest
```
Expected: 编译失败（`DzpzInfo3000FieldMapper` 不存在）。

**Step 3: 写最小实现**

```java
package com.puchain.fep.collector.assembler.mapper;

import com.puchain.fep.collector.CollectorProperties;
import com.puchain.fep.processor.body.supplychain.DzpzInfo3000;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * 3000 电子凭证信息登记 FieldMapper（4 String 必填字段）。
 *
 * <p>PRD §4.6 行1（3000 电子凭证信息登记）+ §2.2 数仓模式（MODE_3 OUTBOUND_ACTIVE）。
 * 镜像 {@link QyRegister3109FieldMapper} 形态：顶层 4 必填标量映射。
 *
 * <p><b>嵌套 complex 字段（pzInfo / ExtInfo）暂留 stub</b>：
 * {@code 3000.xsd} 中两块均 {@code minOccurs="0"}（可选），mapper 不调对应 setter。
 * 未来业务深化 Plan 补 raw → 嵌套对象映射逻辑。
 *
 * <p><b>ApplyMode 母本</b>：§5.8 表 5.1.7-22 凭证登记业务分类 allowed {1,2}（生产 yml）；
 * 本 mapper 透传 raw 值，值域合规由 §5.8 BusinessRuleValidator 流水线强制（非 mapper 内重复校验）。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
public class DzpzInfo3000FieldMapper extends AbstractFieldMapper {

    /**
     * 构造 3000 mapper。
     *
     * @param props 数据采集配置（用于读取 institutionCode）
     */
    public DzpzInfo3000FieldMapper(final CollectorProperties props) {
        super(props, "3000");
    }

    @Override
    public Object toMessageBody(final Map<String, Object> rawData) {
        Objects.requireNonNull(rawData, "rawData");

        final DzpzInfo3000 body = new DzpzInfo3000();

        body.setSerialNo(serialNoOrFallback(rawData));
        body.setSendNodeCode(requireInstitutionCode());
        body.setDesNodeCode(DES_NODE_CODE_HNDEMP_CENTER);
        body.setApplyMode(requireString(rawData, "apply_mode", "applyMode"));

        return body;
    }
}
```

**Step 4: 跑测试确认通过**

```bash
cd /e/FEP_v1.0_wt-b4-dzpz3000 && ./mvnw -pl fep-collector -o test -Dtest=DzpzInfo3000FieldMapperTest
```
Expected: PASS（6 testcase 全绿）。

**Step 5: Commit**（红线 `feedback_commit_no_chain_with_verify_command`：commit 独立，不与验证命令 `&&` 链式）

```bash
git add fep-collector/src/main/java/com/puchain/fep/collector/assembler/mapper/DzpzInfo3000FieldMapper.java \
        fep-collector/src/test/java/com/puchain/fep/collector/assembler/mapper/DzpzInfo3000FieldMapperTest.java
git commit -m "$(cat <<'EOF'
feat(collector): DzpzInfo3000FieldMapper — 3000 电子凭证信息登记数仓 mapper (B4)

补 Mode3 唯一缺失 mapper，镜像 QyRegister3109FieldMapper。4 必填标量 +
pzInfo/ExtInfo 嵌套可选 stub deferred。ApplyMode fixture 取母本 {1,2}。

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

### Task 2: Mode3Routes 注册（fep-collector）

**Files:**
- Modify: `fep-collector/src/main/java/com/puchain/fep/collector/assembler/Mode3Routes.java`
- Test: `fep-collector/src/test/java/com/puchain/fep/collector/assembler/Mode3RoutesTest.java`（若已存在则追加 case；否则参照既有 RouteContributor 测试新建）

**Step 1: 先确认是否已有 Mode3RoutesTest**

```bash
cd /e/FEP_v1.0_wt-b4-dzpz3000 && ls fep-collector/src/test/java/com/puchain/fep/collector/assembler/ | grep -i route
```
按结果决定追加 case 或新建测试类（红线 `feedback_plan_must_grep_actual_api`：实施期 grep 真实测试 API 再写）。

**Step 2: 写失败断言**（断言 `contribute()` 含 `DZPZ_3000 → AssemblerRoute("3000", DzpzInfo3000FieldMapper.class)`）

```java
@Test
void contribute_shouldRegister3000DzpzRoute() {
    Map<String, AssemblerRoute> routes = new Mode3Routes().contribute();
    AssemblerRoute r = routes.get(Mode3Routes.PAYLOAD_TYPE_DZPZ_3000);
    assertThat(r).isNotNull();
    assertThat(r.messageType()).isEqualTo("3000");
    assertThat(r.fieldMapperClass()).isEqualTo(DzpzInfo3000FieldMapper.class);
}
```

**Step 3: 跑测试确认失败**

```bash
cd /e/FEP_v1.0_wt-b4-dzpz3000 && ./mvnw -pl fep-collector -o test -Dtest=Mode3RoutesTest
```
Expected: FAIL（`PAYLOAD_TYPE_DZPZ_3000` 未定义 / route 缺失）。

**Step 4: 实现 — 注册路由**

在 `Mode3Routes` 加常量 + import + `contribute()` Map 追加一条（`Map.of` 当前 4 条，加到 5 条；注意 `Map.of` 最多 10 对，5 对仍可用）:

```java
import com.puchain.fep.collector.assembler.mapper.DzpzInfo3000FieldMapper;

/** payloadDataType — 电子凭证信息登记。 */
public static final String PAYLOAD_TYPE_DZPZ_3000 = "DZPZ_3000";
```
`contribute()` 内 `Map.of(...)` 追加:
```java
                PAYLOAD_TYPE_DZPZ_3000,
                new AssemblerRoute("3000", DzpzInfo3000FieldMapper.class)
```
并同步更新类 Javadoc 路由清单（4 条→5 条，新增 `DZPZ_3000 → 3000 + DzpzInfo3000FieldMapper`），避免 doc-rot（红线 `feedback_cross_task_obsolete_fixture_assumption`）。

**Step 5: 跑测试确认通过**

```bash
cd /e/FEP_v1.0_wt-b4-dzpz3000 && ./mvnw -pl fep-collector -o test -Dtest=Mode3RoutesTest
```
Expected: PASS。

**Step 6: Commit**

```bash
git add fep-collector/src/main/java/com/puchain/fep/collector/assembler/Mode3Routes.java \
        fep-collector/src/test/java/com/puchain/fep/collector/assembler/Mode3RoutesTest.java
git commit -m "$(cat <<'EOF'
feat(collector): register DZPZ_3000 route in Mode3Routes (B4)

Mode3 路由 4→5 条，DZPZ_3000 → 3000 + DzpzInfo3000FieldMapper。同步 Javadoc 清单。

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

### Task 3: 真 XsdValidator on SUT 合规测试（fep-web）

> 红线 `feedback_xsd_compliance_fix_real_validator_on_sut` + `feedback_xsd_validator_requires_full_envelope_redline`：mapper 产物须经真 XsdValidator 校验完整 CFX envelope，不止 JAXB roundtrip。

**Files:**
- Modify: `fep-web/src/test/java/com/puchain/fep/web/collector/mapper/XsdComplianceHelper.java`（**santa Round1 BLOCKER-1 修复**：`buildCfxEnvelope` switch 补 3000 case）
- Test: `fep-web/src/test/java/com/puchain/fep/web/collector/mapper/DzpzInfo3000XsdComplianceTest.java`
- 参照先例: `RzReturnInfo3009XsdComplianceTest.java`（envelope helper 母版）

> **santa Round1 评审实测确认（2026-06-16）**：`XsdComplianceHelper.buildCfxEnvelope`（line 137-148）是**显式 switch**，覆盖 8 个 msgNo（3101/3102/3109/3116/3009/3105/3107/3112），**缺 3000**，default 抛 `IllegalArgumentException`。3000.xsd:31 实测 `<xsd:element name="RealHead3000" type="RequestHead">`（与 3009.xsd:31 `RealHead3009` type=RequestHead 完全同型）→ 修复 = 补一条 `case "3000" -> new EnvelopeConfig("RealHead3000", REQUEST_HEAD_FIELDS)`（RequestHead 无 Result，用 REQUEST_HEAD_FIELDS）。

**Step 0（前置修复 BLOCKER-1）：XsdComplianceHelper 补 3000 case**

在 `buildCfxEnvelope` switch 内 `case "3112"` 之后、`default` 之前插入一行：
```java
            case "3000" -> new EnvelopeConfig("RealHead3000", REQUEST_HEAD_FIELDS);
```
并同步更新该方法 Javadoc 的 msgNo 清单（8→9，加 `3000: RealHead3000 (RequestHead)`），避免 doc-rot。

**Step 1: 写测试**

```java
package com.puchain.fep.web.collector.mapper;

import com.puchain.fep.collector.assembler.mapper.DzpzInfo3000FieldMapper;
import com.puchain.fep.processor.body.supplychain.DzpzInfo3000;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 3000 DzpzInfo mapper 真 XsdValidator on SUT 集成测试（B4）。
 *
 * <p>验证 {@link DzpzInfo3000FieldMapper#toMessageBody(Map)} 产出经 JAXB marshal +
 * 完整 CFX envelope 包裹后通过真 {@code XsdValidator}（红线
 * feedback_xsd_compliance_fix_real_validator_on_sut）。
 */
@SpringBootTest
@TestPropertySource(properties = {
        "fep.collector.scheduling.enabled=false",
        "fep.collector.institution-code=A1000143000999",
        "management.health.redis.enabled=false"
})
class DzpzInfo3000XsdComplianceTest {

    @Autowired
    private DzpzInfo3000FieldMapper mapper;

    @Autowired
    private XsdComplianceHelper helper;

    @Test
    void mapperOutput_shouldPassRealXsdValidator() {
        Map<String, Object> raw = new HashMap<>();
        raw.put("serial_no", "FIX3000SERIAL00000000000000001");  // 30 chars
        raw.put("apply_mode", "1");                               // XSD + 母本双合法

        DzpzInfo3000 body = (DzpzInfo3000) mapper.toMessageBody(raw);

        assertThatCode(() -> helper.validateMapperOutput("3000", body))
                .doesNotThrowAnyException();
    }
}
```

**Step 2:（已并入 Step 0）** helper 补 case 已在 Step 0 完成（santa Round1 评审期静态确认，非实施期再决定）。实施时仅需确认 Step 0 的编辑已落地：`grep -n 'case "3000"' fep-web/src/test/java/com/puchain/fep/web/collector/mapper/XsdComplianceHelper.java` 应命中一行。

**Step 3: 跑测试**（fep-web @SpringBootTest，本地需 Redis 容器或 `management.health.redis.enabled=false` 已设）

```bash
cd /e/FEP_v1.0_wt-b4-dzpz3000 && ./mvnw -pl fep-web -o test -Dtest=DzpzInfo3000XsdComplianceTest
```
Expected: PASS（mapper 产物经真 XsdValidator 校验通过）。
> 若 fep-web `-o` 报上游 SNAPSHOT 缺失（本 worktree 首次构建），先一次性 `./mvnw -pl fep-collector,fep-processor -am install -DskipTests` 装上游 jar 到 ~/.m2，再单模块跑（红线 `feedback_shared_m2_snapshot_cross_session_clobber`）。

**Step 4: Commit**

```bash
git add fep-web/src/test/java/com/puchain/fep/web/collector/mapper/DzpzInfo3000XsdComplianceTest.java \
        fep-web/src/test/java/com/puchain/fep/web/collector/mapper/XsdComplianceHelper.java
git commit -m "$(cat <<'EOF'
test(web): DzpzInfo3000 mapper real XsdValidator on SUT compliance (B4)

完整 CFX envelope 真 XsdValidator 校验 mapper 产物。ApplyMode=1 XSD+母本双合法。
XsdComplianceHelper buildCfxEnvelope switch 补 case "3000"→RealHead3000(RequestHead)
（santa Round1 BLOCKER-1 修复，8→9 msgNo；3000.xsd:31 实测 type=RequestHead）。

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

> 注：helper 与新增 test 同 commit（逐 commit 自洽：helper 改动单独无消费方，与 3000 test 同来同去；红线 `feedback_commit_tree_self_consistent_per_commit`）。

---

### Task 4: 回归 + spotbugs/ArchUnit + closing

**Step 1: 单模块回归**（红线 `feedback_subagent_must_run_spotbugs_check`：须跑 spotbugs+ArchUnit 非仅自己单测）

```bash
cd /e/FEP_v1.0_wt-b4-dzpz3000 && ./mvnw -pl fep-collector -o test > /tmp/b4-collector.log 2>&1
cd /e/FEP_v1.0_wt-b4-dzpz3000 && ./mvnw -pl fep-collector spotbugs:check > /tmp/b4-spotbugs.log 2>&1
```
> 长跑 mvn 须 redirect-to-file，禁 |tail（红线 `feedback_pipe_tail_deadlock_with_bg_bash`）。spotbugs 须先 compile 再 check（红线 `feedback_spotbugs_check_needs_recompile_after_annotation`）；本 Plan 无新 @SuppressFBWarnings（复用基类 helper），但仍以 `test` phase 触发 compile 后再 check。
> 跑前 `uptime` 看 load，>100 杀**自己** worktree-slug 精确匹配 fork，等 load<30 再续（红线 `feedback_single_module_regression_no_am_flag`）。

**Step 2: 实测看结果**（独立命令 `cat`，明述确认 GREEN，红线 `feedback_commit_no_chain_with_verify_command`）

```bash
grep -E "Tests run|BUILD|BugInstance" /tmp/b4-collector.log /tmp/b4-spotbugs.log
```

**回归验收（两层，红线 `feedback_plan_regression_scope_explicit`）:**
- **minimum（本地 tier-A）**: fep-collector `-o test` 0 fail + spotbugs BugInstance 0 + ArchUnit PASS；fep-web `DzpzInfo3000XsdComplianceTest` PASS
- **strong（GHA tier-B）**: PR 触发 ci.yml Build/Test/Quality 全绿（fep-collector + fep-web 全量含 @SpringBootTest IT）。⚠️ 若 GHA billing 阻塞（近期系统性），tier-A 充分则依红线 `feedback_systemic_ci_blocker_defers_positive_backing` 由 muzhou admin override，billing 恢复后补背书。

**Step 3:** 推分支 + 开 PR（FR-MSG-3000 数仓 mapper 闭合）；session-end 跑四步收尾 + 更新 `docs/progress-reconciliation.md` §3 B4（标 collector mapper 已补）+ §5 追加记录 + prd-traceability-matrix。

---

## 评审 checklist（reviewer claim-grep）

- [ ] `DzpzInfo3000FieldMapper` 与 `QyRegister3109FieldMapper` 结构一致（构造器注 props+"3000"，4 必填标量，嵌套 stub）
- [ ] ApplyMode fixture 全部用母本合法值 {1,2}（grep 测试无 "apply_mode".*[^12]"）
- [ ] serial_no fixture 全 30 字符（XSD length=30）
- [ ] Mode3Routes 路由数 4→5，Javadoc 清单同步
- [ ] XSD 合规测试用真 XsdValidator + 完整 CFX envelope（非 body-only / 非 @MockBean validator）
- [ ] 无新 @SuppressFBWarnings（若有须 spotbugs-annotations 依赖 + compile 后 check）
- [ ] 逐 commit 自洽可编译（Task1 mapper+test 同 commit / Task2 路由+test 同 commit）
- [ ] 每 Task 独立 spec review + quality review subagent（红线 `feedback_task_review_discipline`；用只读 Explore agent 禁 mvn，红线 `feedback_review_subagent_must_not_run_mvn` + `feedback_review_subagent_must_be_readonly_agent_type`）
