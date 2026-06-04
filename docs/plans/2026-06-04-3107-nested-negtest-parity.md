# FEP 3107 嵌套必填字段 negative test parity 实施计划

> **执行方式:** 单 Task，test-only。使用 superpowers:executing-plans 内联执行（micro-drain，无需 subagent 驱动）。步骤使用 `- [ ]` 复选框跟踪。

**目标:** 为 `PzCheckQuery3107FieldMapper` 补齐 hxqyInfo 内层必填字段（hxqyName / hxqyCode）缺失的 negative test，与 `BankCheckDay3116FieldMapperTest`（detailMissingRzAmt / detailMissingAmt 内层 negative test）测试覆盖对齐。

**前置依赖:** Plan B（3105/3107/3112 collector mapper，PR #35 已 merge）— `PzCheckQuery3107FieldMapper` 已实装。

**执行 Worktree:** `/Users/muzhou/FEP_v1.0_wt-3107-negtest`（分支 `chore/3107-nested-negtest-parity`，触发条件：红线 `feedback_worktree_isolates_fs_not_logic_domain` — 本仓库当前 7 活跃 worktree 多会话并发，会话起始即须独立 worktree，文件级无交集不豁免）
> CLAUDE.md 6 触发条件均不直接命中（test-only / 单模块 / 非安全 / 非 TLQ / 非 >5min / 无 muzhou WIP），但 `feedback_worktree_isolates_fs_not_logic_domain` 在多会话活跃时强制隔离（git stage area 工作树级共享，杂散 commit 防护）。

**架构:** test-only 加固。`mapHxqyInfo()` 已调 `requireString(rawItem, "hxqy_name", "hxqyName")` / `requireString(rawItem, "hxqy_code", "hxqyCode")`，内层缺失即抛 `FepBusinessException(COLLECT_ASSEMBLE_FAILURE, "missing required field for 3107: hxqyName/hxqyCode")`。既有测试仅覆盖顶层/列表级（missingHxqyInfo / emptyHxqyInfo / exceeds200 / missingCheckDate），缺内层字段缺失覆盖。本 Plan 补 2 个 negative test 覆盖既有行为（**TDD-inverse**：mapper 已实装，测试应自动 GREEN；任何 RED 立即停下排查行为偏离）。

**技术栈:** Java 17 / Spring Boot 3.x / Maven / JUnit 5 + AssertJ

**XSD 依据**（红线 `feedback_plan_xsd_field_table_default_not_optional` 已核）:

| 字段 | XSD 元素 | minOccurs | 结论 |
|------|----------|:---------:|------|
| hxqyName | `fep-processor/src/main/resources/xsd/3107.xsd:90` `<xsd:element name="hxqyName" type="qyName">` | 无属性 → default=1 | required ✅ |
| hxqyCode | `fep-processor/src/main/resources/xsd/3107.xsd:95` `<xsd:element name="hxqyCode" type="qyCode">` | 无属性 → default=1 | required ✅ |

**AI 协同模式:**

| 模式 | AI 占比 | 本计划中的应用 |
|:----:|:-------:|--------------|
| A | 90% | test-only 加固 |

---

## PRD 追溯

| FR-ID | PRD 章节 | 需求描述 | 目标 Task | 状态 |
|-------|----------|----------|-----------|------|
| `FR-MSG-3107-COLLECTOR-MAPPER` | v1.3 §4 报文接入 + §6.2 报文类型清单（3107 平台凭证对账查询）+ §2.2.2 接口模式 mapper | 3107 raw → Body POJO 字段映射的**内层必填字段缺失校验测试覆盖**（parity 加固，无生产代码改动） | Task A1 | ⏸ pending |

> 本 Plan 不改生产代码，不引入新 FR，仅加固既有 `FR-MSG-3107-COLLECTOR-MAPPER` 的测试覆盖完整性。PRD 矩阵无需更新（status 不变）。

---

## 文件结构

| 文件路径 | 职责 | 操作 | AI 模式 |
|----------|------|------|:-------:|
| `fep-collector/src/test/java/com/puchain/fep/collector/assembler/mapper/PzCheckQuery3107FieldMapperTest.java` | 补 2 个内层必填字段 negative test | 修改 | A |

无新增共享工具类。无依赖 ≥3 的新 Service。

---

### Task A1: 3107 hxqyInfo 内层必填字段 negative test `模式 A`

**PRD 依据:** v1.3 §4 报文接入 + §6.2（3107 平台凭证对账查询）+ §2.2.2 接口模式 mapper
**追溯 ID:** FR-MSG-3107-COLLECTOR-MAPPER（测试覆盖加固，无生产改动）

**验收标准（从 XSD + mapper 既有契约推导）:**
1. hxqyInfo 列表中某 entry 缺 `hxqy_name` → 抛 `FepBusinessException`，errorCode = `COLLECT_ASSEMBLE_FAILURE`，message 含 `"missing required field for 3107: hxqyName"`
2. hxqyInfo 列表中某 entry 缺 `hxqy_code` → 抛 `FepBusinessException`，message 含 `"missing required field for 3107: hxqyCode"`

> 断言值来源：`AbstractFieldMapper.requireString()` message 模板 `"missing required field for " + msgNo + ": " + logicalField`（msgNo="3107"，logicalField 见 `mapHxqyInfo()` 第 59/60 行），非从代码反推业务含义。

**Files:**
- Modify: `fep-collector/src/test/java/com/puchain/fep/collector/assembler/mapper/PzCheckQuery3107FieldMapperTest.java`

- [ ] **Step 1: 写 negative test（既有 mapper 已实装，应自动 GREEN — TDD-inverse）**

在 `PzCheckQuery3107FieldMapperTest` 中 `toMessageBody_hxqyInfoExceeds200_throws` 与 `toMessageBody_missingCheckDate_throws` 之间插入以下 2 个测试方法（复用既有 `hxqy()` / `requiredRaw()` helper；`hxqy()` 返回可变 `HashMap`，`remove()` 可用）：

```java
    @Test
    void toMessageBody_hxqyEntryMissingName_throws() {
        final Map<String, Object> raw = requiredRaw();
        final Map<String, Object> entry = hxqy("核心企业甲", "91110000111111111X");
        entry.remove("hxqy_name");
        raw.put("hxqy_info", List.of(entry));
        assertThatThrownBy(() -> mapper.toMessageBody(raw))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("missing required field for 3107: hxqyName")
                .extracting(e -> ((FepBusinessException) e).getErrorCode())
                .isEqualTo(FepErrorCode.COLLECT_ASSEMBLE_FAILURE);
    }

    @Test
    void toMessageBody_hxqyEntryMissingCode_throws() {
        final Map<String, Object> raw = requiredRaw();
        final Map<String, Object> entry = hxqy("核心企业甲", "91110000111111111X");
        entry.remove("hxqy_code");
        raw.put("hxqy_info", List.of(entry));
        assertThatThrownBy(() -> mapper.toMessageBody(raw))
                .isInstanceOf(FepBusinessException.class)
                .hasMessageContaining("missing required field for 3107: hxqyCode");
    }
```

- [ ] **Step 2: 运行新测试确认 GREEN（TDD-inverse 校验）**

```bash
cd /Users/muzhou/FEP_v1.0_wt-3107-negtest && JAVA_HOME=/usr/local/opt/openjdk@17 PATH=/usr/local/opt/openjdk@17/bin:$PATH ./mvnw test -pl fep-collector -am -Dtest=PzCheckQuery3107FieldMapperTest -Dsurefire.failIfNoSpecifiedTests=false
```
期望: `BUILD SUCCESS`，`PzCheckQuery3107FieldMapperTest` 全部测试通过（含新增 2 个）。若新增测试 RED → 立即停下排查 mapper 行为是否偏离 XSD/spec（红线：TDD-inverse 任何 RED 须 muzhou 拍板修哪一边）。

- [ ] **Step 3: 跑 fep-collector 全模块 verify（回归 + 质量门禁）**

```bash
cd /Users/muzhou/FEP_v1.0_wt-3107-negtest && JAVA_HOME=/usr/local/opt/openjdk@17 PATH=/usr/local/opt/openjdk@17/bin:$PATH ./mvnw verify -pl fep-collector -am --batch-mode --no-transfer-progress
```
期望: `BUILD SUCCESS`；Checkstyle / SpotBugs / ArchUnit / JaCoCo 全绿（test-only 改动不应触发任何门禁违规）。

> 回归范围（红线 `feedback_plan_regression_scope_explicit`）：
> - **strong**: `fep-collector -am` verify BUILD SUCCESS（含 Checkstyle/SpotBugs/ArchUnit/JaCoCo）
> - **minimum**: `PzCheckQuery3107FieldMapperTest` 全绿 + fep-collector test phase 无新增 fail
> 本改动 test-only、单模块、无生产类，不需全 reactor verify。

- [ ] **Step 4: 提交**

```bash
cd /Users/muzhou/FEP_v1.0_wt-3107-negtest && git add fep-collector/src/test/java/com/puchain/fep/collector/assembler/mapper/PzCheckQuery3107FieldMapperTest.java
git commit -m "$(cat <<'EOF'
test(collector): add 3107 hxqyInfo nested required-field negative tests

Parity with BankCheckDay3116FieldMapperTest inner-field negative tests
(detailMissingRzAmt/detailMissingAmt). Covers existing requireString throw
behavior for hxqyName/hxqyCode missing within an hxqyInfo entry.
Test-only, no production change. XSD 3107.xsd hxqyName/hxqyCode minOccurs=1.

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

## 闭环 Task: worktree teardown + push + PR

- [ ] **Step 1: push + PR**

```bash
cd /Users/muzhou/FEP_v1.0_wt-3107-negtest && git push -u origin chore/3107-nested-negtest-parity
gh pr create --title "test(collector): 3107 hxqyInfo nested required-field negative test parity" \
  --body "补 PzCheckQuery3107FieldMapperTest 内层必填字段 negative test（hxqyName/hxqyCode），与 3116 对齐。test-only。"
```

- [ ] **Step 2: 独立 review PASS + muzhou merge 后 teardown**

```bash
cd /Users/muzhou/FEP_v1.0 && git worktree remove /Users/muzhou/FEP_v1.0_wt-3107-negtest
git worktree list   # 确认无残留
```

---

## 自检清单

1. **PRD 覆盖度**: ✅ FR-MSG-3107-COLLECTOR-MAPPER 测试覆盖加固，无新 FR，矩阵 status 不变。
2. **安全边界**: ✅ 无 SM2/SM3/SM4/密钥/脱敏/审计逻辑，无 ⛔ 模式 E。
3. **占位符扫描**: ✅ 无 TBD/TODO/待/后续/类似。
4. **类型一致性**: ✅ 复用既有 `hxqy()`/`requiredRaw()` helper + `FepBusinessException`/`FepErrorCode`（已 import）。
5. **测试命令可执行**: ✅ `-Dtest=PzCheckQuery3107FieldMapperTest` 类名匹配；`-Dsurefire.failIfNoSpecifiedTests=false`（Surefire 3.x，红线 `feedback_surefire3_failifno_specified_tests_param_rename`）。
6. **CLAUDE.md 更新**: 由 session-end Phase 处理（micro-drain，不在 Task 内改 CLAUDE.md）。
7. **验收标准完整性**: ✅ 断言值来自 `requireString` message 模板 + XSD minOccurs，可静态验证。
8. **共享工具类**: ✅ 无新增。
9. **核心类职责边界**: ✅ 无新 Service。
10. **Worktree 触发自检**: CLAUDE.md 6 项均不命中；`feedback_worktree_isolates_fs_not_logic_domain`（多会话活跃）命中 → 独立 worktree 必需，闭环含 `git worktree remove` 实测。

---

## 评审与签字

- [x] AI 独立评审（code-reviewer agentId a84e2a89920a89f70）— PASS（7 项全过，0 问题）
- [x] muzhou 签字批准

**批准签字:**
muzhou ✅ APPROVED 2026-06-04（AskUserQuestion "签字 + 执行"）— test-only parity 加固，AI 评审逐字核实 message 模板/编译性/XSD/命令可执行性全过。
