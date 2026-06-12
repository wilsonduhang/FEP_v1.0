# FEP §5.8 规则母本扩批 Batch 2 + GroupCooccurrence 落地 + 引擎 5 项小扩展 实施计划

> **执行方式:** 使用 superpowers:subagent-driven-development（评审）+ 主对话实施（hybrid，红线 `feedback_harness_bg_detach_hybrid_default`）。步骤使用 `- [ ]` 复选框跟踪。

**目标:** 把业务校验规则引擎的规则母本从 1 个报文（1001，2 条规则）扩批到 29 个报文 + 全报文通配，共 **72 条新规则注册**（GROUP_COOCCURRENCE 5 条 + ENUM/DEPENDENT_ENUM 67 条），并为此补齐引擎 5 项小扩展（element-presence / GROUP 判定基准 / ENUM 全值校验 / DEPENDENT_ENUM 成对迭代 / `"*"` 通配报文号）。

**前置依赖:** §5.8 引擎 Phase 1（PR #71）+ 新规则类型 DEPENDENT_ENUM/GROUP_COOCCURRENCE（PR #74）+ Batch/Async 三通路接入（PR #77）— 全部已 MERGED。

**执行 Worktree:** `/Users/muzhou/FEP_v1.0_wt-rule-batch2`（分支 `feat/rule-master-batch2`，触发条件第 ⑥ 项：多会话并发活跃日（wt-simplify-q-drain 等别会话 worktree 在册），红线 `feedback_worktree_isolates_fs_not_logic_domain` 会话起始即隔离；兼命中第 ⑤ 项：fep-web 全模块测试 >5min）

**架构:** 纯配置驱动扩批 — 规则值全部进 `fep-web/src/main/resources/application.yml`（生产配置），引擎只做 5 项行为小扩展（fep-processor `validation/rule/` 包内 5 个类的最小改动），不新增任何 Service/Controller/表。每条母本规则可追溯到《湖南省金融大数据中心数据交换管理平台接口报文规范 V2.0.0-1009 修订版》（下称"报文规范"）具体表号 + 页码。

**技术栈:** Java 17 / Spring Boot 3.x / Maven / JUnit 5 + AssertJ（无新增依赖）

**AI 协同模式:**

| 模式 | AI 占比 | 本计划中的应用 |
|:----:|:-------:|--------------|
| B | 70% | Task 1-5 引擎扩展（行为变更，TDD） |
| C | 60% | Task 6-10 规则母本（值集逐条来自报文规范 PDF，muzhou 签字 = 领域确认；AI 编码落 yaml + 测试） |

---

## 设计背景（研究实测记录，2026-06-11）

### muzhou 已拍板决策（2026-06-11 AskUserQuestion）

1. **G2 引擎扩展**：RuleContext 增加元素存在性索引，GroupCooccurrenceRule 改按"元素存在"判定 → 4 个分组全落地。
2. **G1 注册范围**：全 44 报文（经 `"[*]"` 通配实现，见 Task 5 设计说明）。
3. **Plan 打包**：GroupCooccurrence 与规则母本扩批合并一个 Plan（即本 Plan）。

### santa Round 1（REVISE）后 muzhou 重拍（2026-06-12 AskUserQuestion）

4. **BLOCKER-1 → Result 取值域统一全 52 码表**（替代 v0.1 两档闭集）。规范实证：回执 Result 合法携带系统码——行 10907「查询不存在的凭证，3004 回执处理结果代码为 95007」、行 12569「核心企业未注册返回 95001」、2001/2004/2102/2103/2104 五处"重要"段（行 2067/2743/4047/4839/5659）定义 Result 取非"业务处理结果"码时报文不含业务体的合法形态。脚注"的'业务处理结果'"为正常受理语义描述、非封闭取值域。19 报文统一全表，仍拦截垃圾值。
5. **BLOCKER-2 → G1 改 HEAD-scope 引擎扩展，保全 44 报文**。v0.1"无误伤"论证被 XSD 实测推翻：1102.xsd:80 / 2102.xsd:80 / 2103.xsd:100 / 1104.xsd:105（required）body 内有同名 FileName（语义为历史已报送文件名，非本报文传输文件）→ 全报文扁平视图必误拒。修复：RuleContext 增 HEAD 子树存在性索引，GROUP_COOCCURRENCE 支持 `scope: HEAD`——分组本就定义在 HEAD（表 2.2.2.2-1），语义最忠实，未来新报文 body 同名字段亦免疫。
6. **MAJOR-1 → yaml 通配键用 `"[*]"` bracket notation**：Spring relaxed binding 会剥除未加 `[]` 的 key 中 `*` 字符（变空 key → 工厂 byMsgNo("") 抛异常 → 生产启动失败）。

### GROUP_COOCCURRENCE 全规范扫描结果

对报文规范全文（pdftotext 31512 行）扫描分组标记 `{O` / `O}`（§2.1.3.1 定义，第 5-6 页），**全规范仅 4 个分组**，已逐个对 PDF 原页核验：

| 组 | 位置 | 分组字段 | 报文规范源 | 可表达性（实测依据） |
|---|------|---------|-----------|---------------------|
| G1 | 通用报文头 HEAD | FileName + FileContentHash + FileSize | 表 2.2.2.2-1，第 8 页（脚注 9-11"携带文件传输时必填本字段"） | ✅（须 `scope: HEAD`）HEAD 单实例、叶子字段；引擎收完整 envelope（`SyncMessageProcessorService.java:144` 与 XSD 校验同一 bytes）；⚠ 1102/2102/2103/1104 body 内有同名 FileName（1102.xsd:80 / 2102.xsd:80 / 2103.xsd:100 / 1104.xsd:105，santa R1 实测）→ 判定必须限 HEAD 子树（决策 5） |
| G2 | 3004 顶层 | RiskRate + edUpdateDateTime | 表 3.3.8.2-3，第 84 页 | ⛔→✅ RiskRate 为 XML 容器（3004.xsd:93 `minOccurs="0" maxOccurs="10"`，无直接文本）→ 需 Task 1/2 element-presence 扩展 |
| G3 | pzInfo 共享结构（3000/3004） | SignElement + klzrfSign | 表 3.3.13.2-10，第 111 页（DataType.xsd:1005-1010） | ✅ 3000/3004 精确（pzInfo `minOccurs="0"` 单实例，envelope 内无同名冲突）；⛔ **3105 禁注册** — 3105.xsd:438 自有 SignInfo.SignElement 为 required（无 minOccurs="0"），扁平视图必误拒合法报文 |
| G4 | 3115 顶层 | SignElement + qsfqSign | 表 3.3.20.2-3，第 149 页（3115.xsd:83-88，均 minOccurs="0"） | ✅ PlatPay3115 顶层单实例，3115.xsd 全文件仅 1 处 SignElement |

> 注：3105 体内 pzInfo/zpzInfo（3105.xsd:158/163）虽含该分组，但因上述 SignInfo.SignElement 冲突排除；XSD minOccurs 兜底不受影响。

### ENUM/DEPENDENT_ENUM 扩批清单（值集逐条 PDF 核验）

报文规范全文 93 处枚举标记（85 `M*` + 8 `O*`）逐处定位字段 + 所属报文 + 枚举源表，并对交付 XSD（`fep-processor/src/main/resources/xsd/`，46 文件）实测字段存在性与 local-name 冲突。结论：

**Result 取值域 — 统一全 52 码表**（决策 4）：
- 脚注两档措辞客观存在（15 业务回执报文脚注带「的"业务处理结果"」限定；9007/9009/9020/9120 无限定），v0.1 据此设计两档闭集；
- santa R1 以规范行 10907（3004 回执 95007）/ 12569（95001）/ 五处"重要"段实证回执 Result 合法携带系统码 → 限定措辞非封闭取值域；
- **v0.2 起 19 报文 Result 统一允许全表 52 码**（表 5.1.2-3，第 197-199 页），仍拦截表外垃圾值。

**排除清单（7 类，附排除理由）：**

| 排除项 | 理由 |
|--------|------|
| ContractType（3101） | 脚注（行 6458）"一般由业务发起方自行定义" — 非闭集 |
| qyClass（3102） | 脚注（行 7216）"参考【GB/T4754-2017】标准" — 外部行业分类标准，非规范内闭集 |
| Boolean 型字段（3101×1 + 3109×2） | DataType.xsd 唯一 enumeration 类型，XSD 已强制，引擎规则冗余 |
| CreditStatus（表 5.3.1-2） | 位于 §5.3 附件文件数据结构（贷款业务明细表）— 非 XML 报文字段，引擎不触达 |
| OriMsgNo（9020/9120） | "按原值回填" — 关联回填语义非静态枚举，defer（未来 CROSS_FIELD/关联校验考虑） |
| SendOrgCode（2101 等业务头 M*） | 枚举源为表 5.2.3 金融机构代码表 — 动态机构域，静态母本不适用 |
| MainClass/SecondClass @ 1004/2004/1104/2104 | PDF 字段表有，但**交付 XSD 实测无此字段**（1004/2004/1104/2104.xsd own=0）— 注册即死规则（F3 纪律） |

**规范笔误注记（2 处，母本按去重/合理语义落，muzhou 签字确认）：**
1. 表 5.1.6-1（第 201 页）`10002` 重复出现两行（企业未授权 / 企业授权过期）— 同码双义，值集去重为单个 `10002`。
2. 3115 qsReturnCode 脚注（行 22480）「0-失败;1-失败成功」—"1-失败成功"按上下文为"1-成功"笔误，值集 {0,1}。

### 引擎现状缺口（实测）

| 缺口 | 实测位置 | 影响 | 修复 Task |
|------|---------|------|----------|
| RuleContext 仅索引含直接文本的元素 | `RuleContext.java:72-75` | 容器元素（如 RiskRate）`has()` 恒 false → G2 误判 | Task 1 |
| GroupCooccurrenceRule 按 `ctx.has`（文本）判定 | `GroupCooccurrenceRule.java:29` | 同上；且"使用"语义应为元素存在（§2.1.3.1） | Task 2 |
| EnumMembershipRule 仅校验 `first(field)` | `EnumMembershipRule.java:27` | 批量报文嵌套重复项（核对项/查询项×N）只验第 1 个 | Task 3 |
| DependentEnumRule 仅取 `first(key)/first(field)` | `DependentEnumRule.java:42-43` | 同上，重复项的 key/value 配对丢失 | Task 4 |
| ConfiguredRuleFactory 无全报文注册方式 | `ConfiguredRuleFactory.java:35-41`（未知 msgNo 直接抛异常） | G1 需注册全 44 报文，逐条配置 44 份重复 | Task 5（`"*"` 通配，yaml 键写 `"[*]"`） |
| RuleContext 无 HEAD 子树作用域视图 | `RuleContext.java:56-58`（单一全报文扁平收集） | G1 与 1102/2102/2103/1104 body 同名 FileName 冲突误拒 | Task 1（headElements）+ Task 2（scope: HEAD） |

### PRD 覆盖度声明

- **覆盖:** PRD v1.3 §5.8 业务校验规则（FR-WEB-AUDIT 之规则引擎可配置语义校验）— 本 Plan 为其规则母本数据填充 + 引擎表达力补强。规则值权威源为甲方《报文规范 V2.0.0-1009 修订版》（PRD §3.2 报文校验的上游规范）。
- **不在本 Plan 范围:** 规则引擎多级审核流、异常可视化（Phase 1 即 deferred，见 PR #71 记录）；CONDITIONAL_REQUIRED/CROSS_FIELD 新母本（本轮扫描未发现可静态表达且 XSD 未覆盖的条目——"仅回执时必填"类条件依赖报文方向上下文，引擎 Phase 1 谓词不支持，defer）；OriMsgNo 关联校验（见排除清单）。

---

## 文件结构

| 文件路径 | 职责 | 操作 | AI 模式 |
|----------|------|------|:-------:|
| `fep-processor/src/main/java/com/puchain/fep/processor/validation/rule/RuleContext.java` | 增 element-presence 索引 + `hasElement()` + HEAD 子树索引 `hasElementInHead()` | 修改 | B |
| `fep-processor/src/main/java/com/puchain/fep/processor/validation/rule/GroupCooccurrenceRule.java` | 判定基准切换 `hasElement` + `scope: HEAD` 支持 | 修改 | B |
| `fep-processor/src/main/java/com/puchain/fep/processor/validation/rule/EnumMembershipRule.java` | 校验全部 values | 修改 | B |
| `fep-processor/src/main/java/com/puchain/fep/processor/validation/rule/DependentEnumRule.java` | key/value 成对迭代 | 修改 | B |
| `fep-processor/src/main/java/com/puchain/fep/processor/validation/rule/ConfiguredRuleFactory.java` | `"*"` 通配注册全报文（yaml 键 `"[*]"`） | 修改 | B |
| `fep-processor/src/main/java/com/puchain/fep/processor/validation/rule/RuleDefinitionProperties.java` | RuleDef 增 `scope` 属性（getter/setter） | 修改 | B |
| `fep-processor/src/test/java/com/puchain/fep/processor/validation/rule/RuleContextTest.java` | E1 测试 | 修改 | B |
| `fep-processor/src/test/java/com/puchain/fep/processor/validation/rule/RuleTypesTest.java` | E2/E3/E4 测试 | 修改 | B |
| `fep-processor/src/test/java/com/puchain/fep/processor/validation/rule/ConfiguredRuleFactoryTest.java` | E5 测试 | 修改 | B |
| `fep-web/src/main/resources/application.yml` | 72 条母本规则 | 修改 | C |
| `fep-web/src/test/java/com/puchain/fep/web/validation/RuleMasterTestSupport.java` | 共享：生产 yaml 绑定 + envelope 构造 + 校验执行 | 新建 | B |
| `fep-web/src/test/java/com/puchain/fep/web/validation/RuleMasterMainClass1001Test.java` | 改用共享 support（行为不变） | 修改 | B |
| `fep-web/src/test/java/com/puchain/fep/web/validation/RuleMasterGroupCooccurrenceTest.java` | G1-G4 母本测试 | 新建 | C |
| `fep-web/src/test/java/com/puchain/fep/web/validation/RuleMasterResultTest.java` | Result 全表母本测试 | 新建 | C |
| `fep-web/src/test/java/com/puchain/fep/web/validation/RuleMasterMainSecondClassBatchTest.java` | MainClass/SecondClass 扩批测试 | 新建 | C |
| `fep-web/src/test/java/com/puchain/fep/web/validation/RuleMasterBatchTransferCodesTest.java` | Period/Type/Status/QueryResult/RecordResult 测试 | 新建 | C |
| `fep-web/src/test/java/com/puchain/fep/web/validation/RuleMasterSupplyChainCodesTest.java` | 供应链枚举 + 节点 Status 测试 | 新建 | C |

### 共享工具类清单

| 工具类 | 包 | 关键方法 | 提供者 Task | 消费者 Task |
|---|---|---|---|---|
| RuleMasterTestSupport | web.validation (test) | bindProductionRules() / firstViolation(msgNo, xml) | Task 6 | Task 6/7/8/9/10（+ 回扫改造 RuleMasterMainClass1001Test） |

> **规则**: 写新 private 方法前，先 grep 现有代码有没有同名/同功能方法。

### 核心类职责边界声明

本 Plan 无依赖 ≥3 的新 Service。`ConfiguredRuleFactory` 维持 2 依赖（properties + registry），新增 `"*"` 分支后仍 <100 行。

### PR 拆分与 PR-Size 预告

- **PR-A**（Task 1-5）：引擎扩展，预估净增 ~150 LOC（5 类小改 + 测试）— 应在 400 行门禁内。
- **PR-B**（Task 6-11）：母本 yaml（~370 行）+ 5 个测试类（~450 行）— **必超 400 行门禁**，签字时请 muzhou 预批 PR-Size 豁免（先例 PR #71 992 LOC `--admin --squash`）。yaml 为纯配置数据（mode C 母本），无业务逻辑代码。

---

## Task 1: RuleContext element-presence 索引 `模式 B`

**PRD 依据:** v1.3 §5.8 业务校验规则（引擎表达力）+ 报文规范 §2.1.3.1（第 5-6 页，"使用"= 字段出现在报文实例中）
**追溯 ID:** FR-WEB-AUDIT

**验收标准（从报文规范推导）:**
1. 解析 `<CFX><Body><RiskRate><a>1</a></RiskRate></Body></CFX>` → `hasElement("RiskRate")` = true（容器元素，无直接文本）
2. 同上 XML → `has("RiskRate")` = false（既有文本语义不变，零回归）
3. `hasElement("不存在的元素")` = false
4. 空白文本叶子 `<f>  </f>` → `hasElement("f")` = true（元素存在即"使用"）
5. `<CFX><HEAD><FileName>a.zip</FileName></HEAD><MSG><Item><FileName>b.csv</FileName><FileSize>9</FileSize></Item></MSG></CFX>` → `hasElementInHead("FileName")` = true、`hasElementInHead("FileSize")` = false（body 同名不串扰，决策 5）、`hasElement("FileSize")` = true（全报文视图不变）

**Files:**
- Modify: `fep-processor/src/main/java/com/puchain/fep/processor/validation/rule/RuleContext.java`
- Modify: `fep-processor/src/test/java/com/puchain/fep/processor/validation/rule/RuleContextTest.java`

- [ ] **Step 1: 编写失败测试**（追加到 RuleContextTest）

```java
    @Test
    void hasElement_shouldDetectContainerElementWithoutDirectText() {
        RuleContext ctx = RuleContext.parse(
                "<CFX><Body><RiskRate><a>1</a></RiskRate></Body></CFX>"
                        .getBytes(StandardCharsets.UTF_8));
        assertThat(ctx.hasElement("RiskRate")).isTrue();
        assertThat(ctx.has("RiskRate")).isFalse();
    }

    @Test
    void hasElement_shouldReturnFalseForAbsentElement() {
        RuleContext ctx = RuleContext.parse(
                "<CFX><Body><a>1</a></Body></CFX>".getBytes(StandardCharsets.UTF_8));
        assertThat(ctx.hasElement("RiskRate")).isFalse();
    }

    @Test
    void hasElement_shouldDetectBlankLeafElement() {
        RuleContext ctx = RuleContext.parse(
                "<CFX><Body><f>  </f></Body></CFX>".getBytes(StandardCharsets.UTF_8));
        assertThat(ctx.hasElement("f")).isTrue();
        assertThat(ctx.has("f")).isFalse();
    }

    @Test
    void hasElementInHead_shouldScopeToHeadSubtreeOnly() {
        RuleContext ctx = RuleContext.parse(
                ("<CFX><HEAD><FileName>a.zip</FileName></HEAD>"
                        + "<MSG><Item><FileName>b.csv</FileName><FileSize>9</FileSize></Item></MSG></CFX>")
                        .getBytes(StandardCharsets.UTF_8));
        assertThat(ctx.hasElementInHead("FileName")).isTrue();
        assertThat(ctx.hasElementInHead("FileSize")).isFalse();
        assertThat(ctx.hasElement("FileSize")).isTrue();
    }
```

- [ ] **Step 2: 运行测试确认失败**

```bash
cd /Users/muzhou/FEP_v1.0_wt-rule-batch2 && ./mvnw test -pl fep-processor -o -Dtest=RuleContextTest
```
期望: 编译失败 — `cannot find symbol: method hasElement`

- [ ] **Step 3: 最小实现** — RuleContext 增字段 + 收集 + 方法（保持既有字段/方法签名不动）：

```java
    private final Map<String, List<String>> fields;
    private final Set<String> presentElements;
    private final Set<String> headElements;

    private RuleContext(final Map<String, List<String>> fields,
                        final Set<String> presentElements,
                        final Set<String> headElements) {
        this.fields = fields;
        this.presentElements = presentElements;
        this.headElements = headElements;
    }
```

`parse` 中新增 `final Set<String> present = new HashSet<>(); final Set<String> head = new HashSet<>();`，调用 `collect(doc.getDocumentElement(), collected, present, head, false)`，构造 `new RuleContext(collected, Set.copyOf(present), Set.copyOf(head))`。`collect` 签名加三参（present / head / inHead 标志）：取得 `localName` 后无条件 `present.add(localName)`；`final boolean nowInHead = inHead || "HEAD".equals(localName);` 若 `nowInHead` 再 `head.add(localName)`；子节点递归传 `nowInHead`。新增方法：

```java
    /**
     * 元素是否出现在报文中（不要求有文本，容器元素亦可探测）。
     *
     * <p>对应报文规范 §2.1.3.1 分组可选字段的"使用"语义。</p>
     *
     * @param localName 元素本地名
     * @return 元素存在返回 true
     */
    public boolean hasElement(final String localName) {
        return presentElements.contains(localName);
    }

    /**
     * 元素是否出现在报文头 HEAD 子树内（报文规范表 2.2.2.2-1）。
     *
     * <p>供 HEAD 作用域分组规则使用，避免与 body 内同名元素串扰。</p>
     *
     * @param localName 元素本地名
     * @return HEAD 子树内存在返回 true
     */
    public boolean hasElementInHead(final String localName) {
        return headElements.contains(localName);
    }
```

- [ ] **Step 4: 运行测试确认通过 + 模块回归**

```bash
cd /Users/muzhou/FEP_v1.0_wt-rule-batch2 && ./mvnw test -pl fep-processor -o
```
期望: BUILD SUCCESS，0 fail（含既有 RuleContextTest 全部用例）

- [ ] **Step 5: 提交**

```bash
git add fep-processor/src/
git commit -m "$(cat <<'EOF'
feat(processor): add element-presence index to RuleContext

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

## Task 2: GroupCooccurrenceRule 判定基准切换 `模式 B`

**PRD 依据:** v1.3 §5.8 + 报文规范 §2.1.3.1（第 5-6 页，"同一分组的可选字段必须同时使用或同时不使用"）
**追溯 ID:** FR-WEB-AUDIT

**验收标准:**
1. 分组 {RiskRate, edUpdateDateTime}，报文含 `<RiskRate><a>1</a></RiskRate>` 无 edUpdateDateTime → 违规（容器探测生效）
2. 两者全有 / 全无 → 通过
3. 违规消息含已填与缺失字段清单（既有格式不变）
4. `scope=HEAD` 的分组 {FileName, FileContentHash, FileSize}：HEAD 内仅 FileName → 违规；HEAD 无三字段而 body 有 `<FileName>`（1102 核对项形态）→ 通过（决策 5 防误伤）
5. `scope` 缺省 = MESSAGE（全报文），既有构造器语义不变

> 该规则类型当前无任何已注册母本（生产 yaml grep GROUP_COOCCURRENCE = 0），判定基准切换零存量影响。

**Files:**
- Modify: `fep-processor/src/main/java/com/puchain/fep/processor/validation/rule/GroupCooccurrenceRule.java`
- Modify: `fep-processor/src/main/java/com/puchain/fep/processor/validation/rule/RuleDefinitionProperties.java`（RuleDef 增 scope 属性）
- Modify: `fep-processor/src/main/java/com/puchain/fep/processor/validation/rule/ConfiguredRuleFactory.java`（仅 GROUP_COOCCURRENCE build 分支传 scope；通配注册循环归 Task 5）
- Modify: `fep-processor/src/test/java/com/puchain/fep/processor/validation/rule/RuleTypesTest.java`

- [ ] **Step 1: 失败测试**（追加到 RuleTypesTest）

```java
    @Test
    void groupCooccurrence_shouldDetectContainerElementAsUsed() {
        GroupCooccurrenceRule rule = new GroupCooccurrenceRule(List.of("RiskRate", "edUpdateDateTime"));
        RuleContext partial = RuleContext.parse(
                "<CFX><Body><RiskRate><a>1</a></RiskRate></Body></CFX>".getBytes(StandardCharsets.UTF_8));
        assertThat(rule.evaluate(partial)).isPresent();
        assertThat(rule.evaluate(partial).orElseThrow()).contains("edUpdateDateTime");

        RuleContext both = RuleContext.parse(
                "<CFX><Body><RiskRate><a>1</a></RiskRate><edUpdateDateTime>20260611120000</edUpdateDateTime></Body></CFX>"
                        .getBytes(StandardCharsets.UTF_8));
        assertThat(rule.evaluate(both)).isEmpty();
    }

    @Test
    void groupCooccurrence_headScope_shouldIgnoreBodySameNameElements() {
        GroupCooccurrenceRule rule = new GroupCooccurrenceRule(
                List.of("FileName", "FileContentHash", "FileSize"), GroupCooccurrenceRule.Scope.HEAD);
        RuleContext bodyOnly = RuleContext.parse(
                ("<CFX><HEAD><MsgNo>1102</MsgNo></HEAD>"
                        + "<MSG><Item><FileName>old.csv</FileName></Item></MSG></CFX>")
                        .getBytes(StandardCharsets.UTF_8));
        assertThat(rule.evaluate(bodyOnly)).isEmpty();

        RuleContext headPartial = RuleContext.parse(
                ("<CFX><HEAD><FileName>a.zip</FileName><FileSize>10</FileSize></HEAD>"
                        + "<MSG><Body/></MSG></CFX>").getBytes(StandardCharsets.UTF_8));
        assertThat(rule.evaluate(headPartial)).isPresent();
        assertThat(rule.evaluate(headPartial).orElseThrow()).contains("FileContentHash");
    }
```

- [ ] **Step 2: 确认失败** — `./mvnw test -pl fep-processor -o -Dtest=RuleTypesTest`，期望新用例 RED（容器 has=false 时 present 为空 → 误判通过）

- [ ] **Step 3: 最小实现** —
  1. 增 `public enum Scope { MESSAGE, HEAD }` + 字段 `private final Scope scope`；既有构造器委托 `this(groupFields, Scope.MESSAGE)`，新构造器 `GroupCooccurrenceRule(List<String> groupFields, Scope scope)`；
  2. `evaluate` 中探测函数按 scope 选 `ctx::hasElement`（MESSAGE）或 `ctx::hasElementInHead`（HEAD），两处判定统一走该函数；
  3. Javadoc"有值"描述改"元素出现（{@link RuleContext#hasElement} / HEAD 作用域 {@link RuleContext#hasElementInHead}）"。
  4. `RuleDefinitionProperties.RuleDef` 增 `private String scope;` + getter/setter（含 Javadoc）；`ConfiguredRuleFactory.build` 的 GROUP_COOCCURRENCE 分支改 `new GroupCooccurrenceRule(def.getGroupFields(), def.getScope() == null ? GroupCooccurrenceRule.Scope.MESSAGE : GroupCooccurrenceRule.Scope.valueOf(def.getScope()))`。

- [ ] **Step 4: 通过 + 回归** — `./mvnw test -pl fep-processor -o`，0 fail

- [ ] **Step 5: 提交**（消息 `feat(processor): switch GroupCooccurrenceRule to element-presence semantics`，含 AI-Generated/Reviewed-By 标注，同 Task 1 格式）

---

## Task 3: EnumMembershipRule 全值校验 `模式 B`

**PRD 依据:** v1.3 §5.8 + 报文规范批量报文嵌套项结构（如表 3.2.3.2-4 数据报送核对项，重复出现）
**追溯 ID:** FR-WEB-AUDIT

**验收标准:**
1. 报文含 `<MainClass>GYL</MainClass>...<MainClass>BAD</MainClass>`（重复项）→ 违规，消息含 `BAD`
2. 全部值合法 → 通过；字段缺失 → 通过（可选语义不变）
3. 多个非法值 → 一条违规消息列出全部非法值

**Files:**
- Modify: `fep-processor/src/main/java/com/puchain/fep/processor/validation/rule/EnumMembershipRule.java`
- Modify: `fep-processor/src/test/java/com/puchain/fep/processor/validation/rule/RuleTypesTest.java`

- [ ] **Step 1: 失败测试**

```java
    @Test
    void enumMembership_shouldValidateEveryOccurrenceNotJustFirst() {
        EnumMembershipRule rule = new EnumMembershipRule("MainClass", Set.of("GYL", "EAST"));
        RuleContext ctx = RuleContext.parse(
                ("<CFX><Body><Item><MainClass>GYL</MainClass></Item>"
                        + "<Item><MainClass>BAD</MainClass></Item></Body></CFX>")
                        .getBytes(StandardCharsets.UTF_8));
        assertThat(rule.evaluate(ctx)).isPresent();
        assertThat(rule.evaluate(ctx).orElseThrow()).contains("BAD");
    }
```

（锚点注：实施 Step 1 必须按本 Task 验收标准 1-3 写齐用例——含"多非法值一条消息列全"用例——再进 Step 2；spec review 按此核对。）

- [ ] **Step 2: 确认失败** — 现实现 `first()` 只看 GYL → RED

- [ ] **Step 3: 最小实现** — `evaluate` 改：

```java
    @Override
    public Optional<String> evaluate(final RuleContext ctx) {
        final List<String> bad = ctx.values(field).stream()
                .filter(v -> !allowed.contains(v)).toList();
        if (bad.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of("字段 " + field + " 值 " + bad + " 不在允许集合 " + allowed);
    }
```

- [ ] **Step 4: 通过 + 回归** — `./mvnw test -pl fep-processor -o`，0 fail（注意既有单值违规用例消息格式从 `[v]` 变 `[v]` 列表形态——如有断言精确匹配旧消息需同步调整并在 commit message 注明）

- [ ] **Step 5: 提交**（`feat(processor): validate all occurrences in EnumMembershipRule`）

---

## Task 4: DependentEnumRule 成对迭代 `模式 B`

**PRD 依据:** 同 Task 3（批量报文核对项/查询项中 MainClass+SecondClass 成对重复）
**追溯 ID:** FR-WEB-AUDIT

**验收标准:**
1. 两对 (GYL,HX01) + (EAST,BAD) → 违规，消息含 `BAD` 与 `EAST`
2. 两对全合法 → 通过
3. key/value 出现次数不等 → 按 min(两侧次数) 成对校验（超出部分由 XSD minOccurs 兜底，规则不误报）
4. key 未配置 allowedByKey（如 GENERAL）→ 该对不约束（既有语义保持）

**Files:**
- Modify: `fep-processor/src/main/java/com/puchain/fep/processor/validation/rule/DependentEnumRule.java`
- Modify: `fep-processor/src/test/java/com/puchain/fep/processor/validation/rule/RuleTypesTest.java`

- [ ] **Step 1: 失败测试**

```java
    @Test
    void dependentEnum_shouldValidatePairwiseAcrossRepeatedItems() {
        DependentEnumRule rule = new DependentEnumRule("SecondClass", "MainClass",
                Map.of("GYL", List.of("HX01"), "EAST", List.of("V50")));
        RuleContext ctx = RuleContext.parse(
                ("<CFX><Body><Item><MainClass>GYL</MainClass><SecondClass>HX01</SecondClass></Item>"
                        + "<Item><MainClass>EAST</MainClass><SecondClass>BAD</SecondClass></Item></Body></CFX>")
                        .getBytes(StandardCharsets.UTF_8));
        assertThat(rule.evaluate(ctx)).isPresent();
        assertThat(rule.evaluate(ctx).orElseThrow()).contains("BAD").contains("EAST");
    }
```

（锚点注：实施 Step 1 必须按本 Task 验收标准 1-4 写齐用例——含"次数不等按 min 成对"用例——再进 Step 2；spec review 按此核对。）

- [ ] **Step 2: 确认失败** — 现实现仅 first/first 校验 (GYL,HX01) → RED

- [ ] **Step 3: 最小实现** — `evaluate` 改：

```java
    @Override
    public Optional<String> evaluate(final RuleContext ctx) {
        final List<String> keys = ctx.values(keyField);
        final List<String> vals = ctx.values(field);
        final int n = Math.min(keys.size(), vals.size());
        final List<String> violations = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            final Set<String> allowed = allowedByKey.get(keys.get(i));
            if (allowed != null && !allowed.contains(vals.get(i))) {
                violations.add("字段 " + field + " 值 [" + vals.get(i) + "] 在 "
                        + keyField + "=[" + keys.get(i) + "] 下不在允许集合 " + allowed);
            }
        }
        return violations.isEmpty() ? Optional.empty()
                : Optional.of(String.join("; ", violations));
    }
```

- [ ] **Step 4: 通过 + 回归** — `./mvnw test -pl fep-processor -o`，0 fail

- [ ] **Step 5: 提交**（`feat(processor): pairwise key-value iteration in DependentEnumRule`）

---

## Task 5: ConfiguredRuleFactory `"*"` 通配报文号 `模式 B`

**PRD 依据:** 报文规范表 2.2.2.2-1（第 8 页）— G1 分组定义在全报文通用 HEAD；muzhou 拍板"全 44 报文"
**追溯 ID:** FR-WEB-AUDIT

**设计说明:** muzhou 拍板 G1 注册全 44 报文。逐报文复制 44 份相同 yaml 违反 DRY 且新报文接入时易漏；`"*"` 通配键在装配期展开为对 `MessageType.values()` 全部注册，语义即"全 44 报文"，新报文随枚举自动覆盖。**yaml 键必须写 `"[*]"`**（决策 6：Spring relaxed binding 对未加 `[]` 的 Map key 剥除非字母数字字符，`"*"` 会变空 key 致生产启动失败；bracket notation 绑定后 Java Map 中的 key 即字面 `*`，工厂代码按 `"*"` 判定）。签字即确认此实现方式。

**验收标准:**
1. yaml `"*"` 下的规则 → `MessageType.values()` 每个类型的 `rulesFor` 都含该规则
2. `"*"` 与具体 msgNo 并存 → 具体报文聚合两者
3. 未知具体 msgNo（非 `"*"`）仍抛 IllegalArgumentException（既有守护不变）

**Files:**
- Modify: `fep-processor/src/main/java/com/puchain/fep/processor/validation/rule/ConfiguredRuleFactory.java`
- Modify: `fep-processor/src/test/java/com/puchain/fep/processor/validation/rule/ConfiguredRuleFactoryTest.java`

- [ ] **Step 1: 失败测试**（按 ConfiguredRuleFactoryTest 既有构造模式，grep 现文件复用其 properties/registry 构造写法）

```java
    @Test
    void wildcardMsgNo_shouldRegisterRuleForAllMessageTypes() {
        RuleDefinitionProperties props = new RuleDefinitionProperties();
        RuleDefinitionProperties.RuleDef def = new RuleDefinitionProperties.RuleDef();
        def.setType("GROUP_COOCCURRENCE");
        def.setGroupFields(List.of("FileName", "FileContentHash", "FileSize"));
        def.setScope("HEAD");
        props.setRules(Map.of("*", List.of(def)));
        MessageRuleRegistry registry = new MessageRuleRegistry();
        new ConfiguredRuleFactory(props, registry).registerConfiguredRules();
        for (MessageType type : MessageType.values()) {
            assertThat(registry.rulesFor(type)).as("type %s", type).hasSize(1);
        }
    }

    @Test
    void wildcardAndSpecificMsgNo_shouldAggregate() {
        // 验收 2：通配与具体 msgNo 并存 → 具体报文聚合两者
        RuleDefinitionProperties props = new RuleDefinitionProperties();
        RuleDefinitionProperties.RuleDef wild = new RuleDefinitionProperties.RuleDef();
        wild.setType("GROUP_COOCCURRENCE");
        wild.setGroupFields(List.of("FileName", "FileContentHash", "FileSize"));
        wild.setScope("HEAD");
        RuleDefinitionProperties.RuleDef specific = new RuleDefinitionProperties.RuleDef();
        specific.setType("ENUM");
        specific.setField("Result");
        specific.setAllowed(List.of("90000"));
        props.setRules(Map.of("*", List.of(wild), "3116", List.of(specific)));
        MessageRuleRegistry registry = new MessageRuleRegistry();
        new ConfiguredRuleFactory(props, registry).registerConfiguredRules();
        assertThat(registry.rulesFor(MessageType.MSG_3116)).hasSize(2);
    }

    @Test
    void wildcardYamlBracketKey_shouldBindLiteralStarThroughRelaxedBinding() {
        // 防回归（决策 6）：经真实 Binder 验证 "[*]" 绑定为字面 "*" key
        StandardEnvironment env = new StandardEnvironment();
        env.getPropertySources().addFirst(new MapPropertySource("t", Map.of(
                "fep.validation.rules.[*][0].type", "GROUP_COOCCURRENCE",
                "fep.validation.rules.[*][0].scope", "HEAD",
                "fep.validation.rules.[*][0].group-fields[0]", "FileName",
                "fep.validation.rules.[*][0].group-fields[1]", "FileContentHash",
                "fep.validation.rules.[*][0].group-fields[2]", "FileSize")));
        RuleDefinitionProperties bound = Binder.get(env)
                .bind("fep.validation", RuleDefinitionProperties.class).orElseThrow();
        assertThat(bound.getRules()).containsKey("*");
    }
```

（锚点注：实施 Step 1 必须按本 Task 验收标准 1-3 写齐用例再进 Step 2；验收 3 由既有"未知 msgNo 抛异常"守护用例覆盖，确认其仍 GREEN 即可。）

- [ ] **Step 2: 确认失败** — `"*"` 走 `byMsgNo` 查找 → IllegalArgumentException → RED

- [ ] **Step 3: 最小实现** — `registerConfiguredRules` 循环体开头加：

```java
            if ("*".equals(msgNo)) {
                defs.forEach(def -> {
                    final ValidationRule rule = build(def);
                    for (final MessageType t : MessageType.values()) {
                        registry.register(t, rule);
                    }
                });
                return;
            }
```

（Javadoc 补 `"*"` 语义说明：通配注册到全部已注册报文类型。）

- [ ] **Step 4: 通过 + 回归 + 质量门**

```bash
cd /Users/muzhou/FEP_v1.0_wt-rule-batch2 && ./mvnw test -pl fep-processor -o \
  && ./mvnw compile spotbugs:check -pl fep-processor -o
```
期望: 0 fail + `BugInstance size is 0`

- [ ] **Step 5: 提交**（`feat(processor): wildcard msgNo registers rule for all message types`）→ **此处开 PR-A**（gh pr create，正文含 Task 1-5 摘要 + 🤖 footer），等 CI 绿后按治理 merge。

---

## Task 6: G 组母本（GROUP_COOCCURRENCE 5 条注册）+ 共享测试支撑 `模式 C`

**PRD 依据:** 报文规范 表 2.2.2.2-1（第 8 页）/ 表 3.3.8.2-3（第 84 页）/ 表 3.3.13.2-10（第 111 页）/ 表 3.3.20.2-3（第 149 页）
**追溯 ID:** FR-WEB-AUDIT

**验收标准:**
1. 任意报文（取 9005 连通性测试为样本）HEAD 含 FileName+FileSize 缺 FileContentHash → 违规
2. 3004 含 RiskRate 容器缺 edUpdateDateTime → 违规；两者全有 → 通过
3. 3000/3004 含 SignElement 缺 klzrfSign → 违规
4. 3115 含 SignElement 缺 qsfqSign → 违规
5. **3105 无 {SignElement,klzrfSign} 注册**（负向断言：3105 报文 SignInfo.SignElement 存在 + 无 klzrfSign → 通过）
6. 生产 yaml 绑定后规则总注册条目数可实算（测试断言 `"*"` 1 条 + 3000 1 条 + 3004 2 条 + 3115 1 条）
7. **G1 HEAD-scope 防误伤回归（决策 5）**：1102 样本 HEAD 无文件三字段 + body 核对项含 `<FileName>` → 通过

**Files:**
- Modify: `fep-web/src/main/resources/application.yml`
- Create: `fep-web/src/test/java/com/puchain/fep/web/validation/RuleMasterTestSupport.java`
- Create: `fep-web/src/test/java/com/puchain/fep/web/validation/RuleMasterGroupCooccurrenceTest.java`
- Modify: `fep-web/src/test/java/com/puchain/fep/web/validation/RuleMasterMainClass1001Test.java`（改用 support，断言不变）

- [ ] **Step 1: 抽取共享测试支撑**（从 RuleMasterMainClass1001Test 现有 `bindProductionRules()` 原样上提，新增校验辅助）

```java
package com.puchain.fep.web.validation;

import com.puchain.fep.converter.type.MessageType;
import com.puchain.fep.processor.validation.BusinessRuleValidator;
import com.puchain.fep.processor.validation.ValidationResult;
import com.puchain.fep.processor.validation.rule.ConfiguredRuleFactory;
import com.puchain.fep.processor.validation.rule.MessageRuleRegistry;
import com.puchain.fep.processor.validation.rule.RuleDefinitionProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 规则母本测试共享支撑：绑定生产 application.yml 规则配置并对样本报文执行业务规则校验。
 */
final class RuleMasterTestSupport {

    private RuleMasterTestSupport() {
    }

    /** 绑定生产 application.yml 的 fep.validation 规则配置。 */
    static RuleDefinitionProperties bindProductionRules() throws IOException {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        List<PropertySource<?>> sources =
                loader.load("application", new ClassPathResource("application.yml"));
        StandardEnvironment env = new StandardEnvironment();
        sources.forEach(env.getPropertySources()::addFirst);
        return Binder.get(env).bind("fep.validation", RuleDefinitionProperties.class)
                .orElseGet(RuleDefinitionProperties::new);
    }

    /** 用生产规则配置校验样本报文，返回校验结果。 */
    static ValidationResult validate(String msgNo, String envelopeXml) throws IOException {
        MessageRuleRegistry registry = new MessageRuleRegistry();
        new ConfiguredRuleFactory(bindProductionRules(), registry).registerConfiguredRules();
        MessageType type = MessageType.byMsgNo(msgNo).orElseThrow();
        return new BusinessRuleValidator(registry)
                .validate(type, envelopeXml.getBytes(StandardCharsets.UTF_8));
    }
}
```

> 实施时 grep `BusinessRuleValidator` 构造器与 `ValidationResult` API 实际签名（红线 `feedback_plan_must_grep_actual_api` 执行期复核），如与上述样例有出入按真实 API 调整并在 commit message 注明。

- [ ] **Step 2: 失败测试**（RuleMasterGroupCooccurrenceTest，节选骨架——实施按验收标准 1-6 全写）

```java
class RuleMasterGroupCooccurrenceTest {

    @Test
    void headFileGroup_partialUse_shouldViolate_onAnyMessage() throws IOException {
        String xml = "<CFX><HEAD><MsgNo>9005</MsgNo><FileName>a.zip</FileName>"
                + "<FileSize>10</FileSize></HEAD><MSG><EchoTest9005><SerialNo>1</SerialNo>"
                + "</EchoTest9005></MSG></CFX>";
        ValidationResult r = RuleMasterTestSupport.validate("9005", xml);
        assertThat(r.valid()).isFalse();
        assertThat(r.errors().get(0)).contains("FileContentHash");
    }

    @Test
    void group3004_riskRateWithoutEdUpdateDateTime_shouldViolate() throws IOException { /* 验收 2 */ }

    @Test
    void group3000And3004_signElementWithoutKlzrfSign_shouldViolate() throws IOException { /* 验收 3 */ }

    @Test
    void group3115_signElementWithoutQsfqSign_shouldViolate() throws IOException { /* 验收 4 */ }

    @Test
    void msg3105_signInfoSignElementAlone_shouldPass_groupNotRegistered() throws IOException { /* 验收 5 */ }

    @Test
    void productionYaml_groupRuleEntryCounts_shouldMatchPlan() throws IOException { /* 验收 6 */ }

    @Test
    void msg1102_bodyFileNameWithoutHeadFileFields_shouldPass_headScope() throws IOException { /* 验收 7 */ }
}
```

（Plan 评审注：骨架内注释为验收标准锚点非占位符——实施 Step 2 必须按验收标准 1-7 写齐 7 个完整用例后才进 Step 3；spec review 按此核对。）

- [ ] **Step 3: 确认失败** — 规则未配置 → 违规断言 RED

- [ ] **Step 4: 母本 yaml**（追加到 `fep.validation.rules`，紧跟既有 `"1001"` 条目后）

```yaml
      # ===== GROUP_COOCCURRENCE 母本（报文规范 §2.1.3.1 分组可选字段，全规范仅 4 组）=====
      # G1：通用报文头文件传输三字段组（表 2.2.2.2-1 p8，脚注 9-11"携带文件传输时必填"）
      # muzhou 拍板注册全 44 报文（"[*]" 通配，Task 5 引擎扩展；bracket key 防 relaxed binding 剥 *，决策 6）
      # scope: HEAD —— 1102/2102/2103/1104 body 有同名 FileName（历史已报送文件），限 HEAD 子树防误拒（决策 5）
      "[*]":
        - type: GROUP_COOCCURRENCE
          groupFields: [FileName, FileContentHash, FileSize]
          scope: HEAD
      # G3(3000)：电子凭证信息 pzInfo 凭证签名组（表 3.3.13.2-10 p111；共享结构 DataType.xsd pzInfo）
      "3000":
        - type: GROUP_COOCCURRENCE
          groupFields: [SignElement, klzrfSign]
      # G2(3004)：融资风险比例参考 + 额度更新时间（表 3.3.8.2-3 p84；RiskRate 为容器，按元素存在判定）
      # G3(3004)：同 3000 pzInfo 凭证签名组
      "3004":
        - type: GROUP_COOCCURRENCE
          groupFields: [RiskRate, edUpdateDateTime]
        - type: GROUP_COOCCURRENCE
          groupFields: [SignElement, klzrfSign]
      # G4(3115)：清算指令签名组（表 3.3.20.2-3 p149）
      # ⚠ 3105 不注册 G3：3105.xsd SignInfo.SignElement 为 required，扁平视图会误拒（见 Plan §设计背景）
      "3115":
        - type: GROUP_COOCCURRENCE
          groupFields: [SignElement, qsfqSign]
```

- [ ] **Step 5: 测试通过 + 回归**

```bash
cd /Users/muzhou/FEP_v1.0_wt-rule-batch2 && ./mvnw -pl fep-processor -am install -DskipTests -o \
  && ./mvnw test -pl fep-web -o
```
期望: 0 fail（含既有 RuleMasterMainClass1001Test 改造后全绿）

- [ ] **Step 6: 提交**（`feat(web): register GROUP_COOCCURRENCE rule master G1-G4 (5 entries)`）

---

## Task 7: Result 母本（统一全 52 码表，19 条注册）`模式 C`

**PRD 依据:** 报文规范 表 5.1.2-3 处理结果代码表（第 197-199 页）；取值域统一依据见设计背景"决策 4"（santa R1 BLOCKER-1 实证 + muzhou 2026-06-12 重拍）
**追溯 ID:** FR-WEB-AUDIT

**验收标准:**
1. 19 报文（2001/2004/2102/2103/2104/3002/3004/3006/3008/3101/3103/3108/3113/3115/3020 + 9007/9009/9020/9120）Result ∈ 全 52 码表
2. 规范点名场景不误拒：3004 样本 `<Result>95007</Result>`（查询凭证不存在，规范行 10907）→ 通过
3. 表外垃圾值拦截：任一报文样本 `<Result>12345</Result>` → 违规
4. Result 缺失（请求方向）→ 通过

**Files:**
- Modify: `fep-web/src/main/resources/application.yml`
- Create: `fep-web/src/test/java/com/puchain/fep/web/validation/RuleMasterResultTest.java`

- [ ] **Step 1: 失败测试**（@ParameterizedTest 两组 msgNo，envelope 用各报文真实业务头元素名——实施时逐报文 grep 对应 XSD 取 `BatchHead/RealHead` 元素名构造样本）

```java
class RuleMasterResultTest {

    private static final List<String> RESULT_MSGNOS = List.of(
            "2001", "2004", "2102", "2103", "2104", "3002", "3004", "3006",
            "3008", "3101", "3103", "3108", "3113", "3115", "3020",
            "9007", "9009", "9020", "9120");

    @ParameterizedTest
    @MethodSource("resultMsgNos")
    void anyRegisteredMessage_fullTableCode_shouldPass_garbageShouldViolate(String msgNo) throws IOException {
        assertThat(RuleMasterTestSupport.validate(msgNo,
                envelopeWithResult(msgNo, "90000")).valid()).as("%s 90000", msgNo).isTrue();
        assertThat(RuleMasterTestSupport.validate(msgNo,
                envelopeWithResult(msgNo, "12345")).valid()).as("%s 12345", msgNo).isFalse();
    }

    @Test
    void msg3004_systemCode95007_specNamedScenario_shouldPass() throws IOException {
        // 规范行 10907：查询不存在的凭证，3004 回执处理结果代码 95007（决策 4 防误拒回归）
        assertThat(RuleMasterTestSupport.validate("3004",
                envelopeWithResult("3004", "95007")).valid()).isTrue();
    }
    // envelopeWithResult: 按 msgNo 构造含业务头 Result 的最小 envelope（XSD 实测元素名）
}
```

- [ ] **Step 2: 确认失败**

- [ ] **Step 3: 母本 yaml** — 19 报文（"2001"/"2004"/"2102"/"2103"/"2104"/"3002"/"3004"/"3006"/"3008"/"3101"/"3103"/"3108"/"3113"/"3115"/"3020"/"9007"/"9009"/"9020"/"9120"）每个追加全 52 码条目（已有键如 "3004" 追加到其规则列表；值集逐字来自表 5.1.2-3 p197-199，统一取值域依据决策 4）：

```yaml
        - type: ENUM
          field: Result
          allowed: ["90000", "90001", "90500",
                    "21001", "21002", "21003", "21004", "21005", "91005",
                    "92001", "92002", "92003", "92004", "92005", "92006",
                    "93001", "93002", "93003", "93004", "93999",
                    "94001", "94002", "94003", "94004", "94005", "94006", "94007",
                    "95001", "95002", "95003", "95004", "95005", "95006", "95007",
                    "95008", "95009", "95010", "95011", "95012", "95013", "95014",
                    "95015", "95016", "95099",
                    "28001", "28002", "28003", "28004", "98001",
                    "19001", "29002", "29999"]
```

- [ ] **Step 4: 通过 + 回归** — `./mvnw test -pl fep-web -o`，0 fail
- [ ] **Step 5: 提交**（`feat(web): register Result enum rule master, full 52-code table (19 entries)`）

---

## Task 8: MainClass/SecondClass 扩批（7 报文 ×2 = 14 条注册）`模式 C`

**PRD 依据:** 报文规范 表 5.1.3-1 业务类别代码规范（第 199 页，R1/R2 同源）+ 各报文字段表（2001 表 3.1.2.2-3 / 1101 表 3.2.1.2-3 / 2101 表 3.2.2.2-3 / 1102 表 3.2.3.2-4 / 2102 表 3.2.4.2-3 / 1103 表 3.2.5.2-4 / 2103 表 3.2.6.2-4）
**追溯 ID:** FR-WEB-AUDIT

**范围依据（实测）:** 7 报文 XSD own MainClass=1 且 SecondClass=1；1004/2004/1104/2104 交付 XSD 无此字段 → 排除（死规则纪律）。批量报文嵌套项重复 → 依赖 Task 3/4 全值/成对校验。

**验收标准:**
1. 7 报文样本 MainClass=GYL + SecondClass=HX01 → 通过；MainClass=BAD → 违规
2. 重复项第 2 个 (EAST, BAD) → 违规（成对迭代生效，至少 1 个批量报文样本验证）
3. SecondClass 在 MainClass=GENERAL 下任意值 → 通过（allowedByKey 不含 GENERAL，R2 语义一致）

**Files:**
- Modify: `fep-web/src/main/resources/application.yml`
- Create: `fep-web/src/test/java/com/puchain/fep/web/validation/RuleMasterMainSecondClassBatchTest.java`

- [ ] **Step 1: 失败测试**（@ParameterizedTest over 7 msgNos；envelope 项结构元素名实施时逐报文 grep XSD）
- [ ] **Step 2: 确认失败**
- [ ] **Step 3: 母本 yaml** — 7 报文（"2001"/"1101"/"2101"/"1102"/"2102"/"1103"/"2103"）每个追加（与既有 1001 R1/R2 完全同源同值）：

```yaml
        - type: ENUM
          field: MainClass
          allowed: [EAST, COINFO, COAUTH, GYL, MONITOR, STATS, YWTB, ZFJS, SYSTEM, GENERAL]
        - type: DEPENDENT_ENUM
          field: SecondClass
          keyField: MainClass
          allowedByKey:
            EAST: [V50]
            COINFO: [I1001, PKG01]
            COAUTH: [A0001, A0002, PKG01]
            GYL: [HX01, HX02, HX03, HX04, PT01, PT02, PT03, BB01, BB02]
            MONITOR: [ZHJC01, ZTJC01]
            STATS: [LSD01, GYL01]
            YWTB: [ZHYW]
            ZFJS: [FZBMD]
            SYSTEM: [I0001]
```

- [ ] **Step 4: 通过 + 回归** — `./mvnw test -pl fep-web -o`，0 fail
- [ ] **Step 5: 提交**（`feat(web): extend MainClass/SecondClass rule master to 7 batch/realtime messages (14 entries)`）

---

## Task 9: 批量传输码 + 查询/备案结果母本（11 条注册）`模式 C`

**PRD 依据:** 报文规范 表 5.1.4-1/-2/-3（第 200 页）+ 表 5.1.6-1/-2（第 201 页）+ 各报文字段表
**追溯 ID:** FR-WEB-AUDIT

**母本表（值集逐字来自规范）:**

| 报文 | 字段 | 值集 | 源 |
|------|------|------|----|
| 1101, 2101, 1102, 2102 | Period | 1,2,3,4,5,6,7,99 | 表 5.1.4-1 p200 |
| 1101, 2101 | Type | 1,2,3 | 表 5.1.4-2 p200 |
| 1102, 2102 | Status | 1,2,3,4,5,6 | 表 5.1.4-3 p200 |
| 2103 | QueryResult | 90000,10001,10002,10003,10004,10005,10006,10007,10008,19999 | 表 5.1.6-1 p201（10002 规范重复笔误已去重） |
| 2004, 2104 | RecordResult | 90000,91000,10001,10002,10003,10004,11001,11002,11003,11004,11005,19999 | 表 5.1.6-2 p201 |

（条目数自检：Period 4 + Type 2 + Status 2 + QueryResult 1 + RecordResult 2 = 11 ✓。local-name 冲突已实测：1101/2101 Type=1、1102/2102 Status=1、其余 0，见设计背景。）

**验收标准:** 每字段一组（合法值通过 / 非法值违规 / 缺失通过），样本 envelope 用 XSD 实测元素名。

**Files:**
- Modify: `fep-web/src/main/resources/application.yml`
- Create: `fep-web/src/test/java/com/puchain/fep/web/validation/RuleMasterBatchTransferCodesTest.java`

- [ ] **Step 1-2: 失败测试 + 确认 RED**（结构同 Task 7 参数化模式）
- [ ] **Step 3: 母本 yaml**（按上表逐条落，每条带 `# 表 5.1.4-x/5.1.6-x pNNN` 注释；数字值集一律加引号）
- [ ] **Step 4: 通过 + 回归** — `./mvnw test -pl fep-web -o`，0 fail
- [ ] **Step 5: 提交**（`feat(web): register batch transfer + query/record result rule master (11 entries)`）

---

## Task 10: 供应链枚举 + 节点状态母本（23 条注册）`模式 C`

**PRD 依据:** 报文规范 §5.1.7 供应链通用代码规范（第 201-205 页）+ §5.1.5（第 200 页）+ 各字段脚注（行号见设计背景）
**追溯 ID:** FR-WEB-AUDIT

**母本表（值集逐字来自规范，XSD 存在性全部实测 own=1）:**

| 报文 | 字段 | 值集 | 源 |
|------|------|------|----|
| 3102 | ApplyMode | 1,2,3 | 表 5.1.7-6 p202（脚注行 7060 区段） |
| 3102 | qyType | 1,2,3,4,5,6 | 表 5.1.7-19 p204 |
| 3102 | qySize | SC00,SC01,SC02,SC03 | 表 5.1.7-20 p204 |
| 3000 | ApplyMode | 1,2 | 表 5.1.7-22 p204（凭证登记业务分类） |
| 3103 | CreationRetCode | 11,21,22,23,91,92,93,99 | 表 5.1.7-3 p201 开户建档段 |
| 3001 | QueryType | 1,2 | 表 5.1.7-18 p204 |
| 3002 | QueryType | 1,2 | 表 5.1.7-18 p204 |
| 3006 | AccReturnCode | 0,1,2,3,9 | 表 5.1.7-2 p201 |
| 3105 | ApplyMode | 1,2,3 | 表 5.1.7-6 p202（脚注行 15017） |
| 3105 | StdBizMode | 11,12,21,31 | 表 5.1.7-4 p202 |
| 3105 | fxMode | 1,2,3 | 表 5.1.7-7 p203 |
| 3004 | rzPhaseCode | 10,11,21,22,23,24,91,92,93,99 | 表 5.1.7-3 p201 凭证融资段（脚注行 11377） |
| 3009 | rzPhaseCode | 10,11,21,22,23,24,91,92,93,99 | 同上（脚注行 17387） |
| 3108 | RetCode | 全 52 码（同 Task 7 全表） | 表 5.1.2-3 p197-199（脚注行 18794） |
| 3113 | RetCode | 全 52 码（同 Task 7 全表） | 表 5.1.2-3 p197-199（脚注行 21584） |
| 3109 | qyFlag | 1,2,3 | 表 5.1.7-9 p203 |
| 3109 | PlatState | 1,2 | 表 5.1.7-10 p203 |
| 3109 | PlatType | 1,2,3,4 | 表 5.1.7-13 p203 |
| 3109 | PlatServiceObject | 1,2,3,4 | 表 5.1.7-14 p203 |
| 3109 | PlatDevelopmentMethod | 1,2,3 | 表 5.1.7-15 p203 |
| 3115 | qsReturnCode | 0,1 | 脚注行 22480（"1-失败成功"为"成功"笔误，签字确认） |
| 9007 | Status | 1,2,3,99 | 表 5.1.5-1 p200 |
| 9009 | Status | 1,2,3,99 | 表 5.1.5-1 p200 |

（条目数实算：3102×3 + 3000×1 + 3103×1 + 3001×1 + 3002×1 + 3006×1 + 3105×3 + 3004×1 + 3009×1 + 3108×1 + 3113×1 + 3109×5 + 3115×1 + 9007×1 + 9009×1 = **23 条** = 上表 23 行，红线 `feedback_plan_template_data_point_self_consistency` 实算自洽 ✓）

**验收标准:** 每字段一组（合法/非法/缺失），3105 三字段可合并一个样本 envelope；9007/9009 Status 注意与其 Result 规则（Task 7）并存聚合断言。

**Files:**
- Modify: `fep-web/src/main/resources/application.yml`
- Create: `fep-web/src/test/java/com/puchain/fep/web/validation/RuleMasterSupplyChainCodesTest.java`

- [ ] **Step 1-2: 失败测试 + 确认 RED**
- [ ] **Step 3: 母本 yaml**（按上表逐条落，每条带源表+页码注释；数字值集加引号）
- [ ] **Step 4: 通过 + 回归** — `./mvnw test -pl fep-web -o`，0 fail
- [ ] **Step 5: 提交**（`feat(web): register supply-chain + node-status enum rule master (23 entries)`）

---

## Task 11: 收尾（全量回归 + 文档 + PR-B）`模式 B`

**验收标准（红线 `feedback_plan_regression_scope_explicit` 两层）:**
- **strong:** worktree 内全 reactor `./mvnw verify --batch-mode --no-transfer-progress` BUILD SUCCESS（确认同时刻无别会话跑全量；load>100 中止并降级 minimum 层 + GHA 兜底）
- **minimum:** `./mvnw test -pl fep-processor -o` + `./mvnw test -pl fep-web -o` 双绿 + `./mvnw compile spotbugs:check -pl fep-processor,fep-web -o` BugInstance 0 + fep-web ArchUnit 套件 PASS

- [ ] **Step 1: 全量回归**（按 strong 层执行；执行前 `uptime` 实测 load）
- [ ] **Step 2: CLAUDE.md「当前项目状态」更新**（/Users/muzhou/FEP/CLAUDE.md 顶部"最后更新"行 + 规则引擎条目——file write only，禁 git add，红线 `feedback_fep_docs_repo_commit_taboo`）
- [ ] **Step 3: 开 PR-B**

```bash
git push -u origin feat/rule-master-batch2
gh pr create --title "feat: rule master batch2 - 72 entries + GROUP_COOCCURRENCE G1-G4" --body "..."
```
（正文含 Task 6-11 摘要 + 排除清单 + PR-Size 豁免申请 + 🤖 footer；CI 绿 + muzhou 豁免后 merge。）
- [ ] **Step 4: worktree 闭环**

```bash
git worktree remove /Users/muzhou/FEP_v1.0_wt-rule-batch2
```

---

## 自检记录（起草自检，10 项）

1. **PRD 覆盖度** ✅ §5.8 FR-WEB-AUDIT；未覆盖项已在"不在本 Plan 范围"列明
2. **安全边界** ✅ 无 SM2/SM3/SM4/密钥/脱敏触碰；纯 validation 规则
3. **占位符扫描** ✅ Task 6-10 测试骨架注释为验收标准锚点（实施步骤明确要求写齐），yaml 值集全量逐字落盘
4. **类型一致性** ✅ hasElement/values/RuleDef setter 均经实测 grep（行号见设计背景）
5. **测试命令可执行** ✅ 统一 `-pl <module> -o test`（红线 `feedback_single_module_regression_no_am_flag`；fep-web 前置一次性 `-pl fep-processor -am install -DskipTests`）
6. **CLAUDE.md 更新** ✅ Task 11 Step 2
7. **验收标准完整性** ✅ 全部从报文规范表号+页码推导，断言值可对 PDF 手核
8. **共享工具类** ✅ RuleMasterTestSupport 已登记
9. **职责边界** ✅ 无新 Service
10. **Worktree 触发** ✅ 命中第 ⑥（多会话并发日）+ 第 ⑤（>5min verify）项 → `wt-rule-batch2`；Task 11 含 remove 实测命令

**数据点自洽实算:** 注册条目 = G 组 5 + Result 19 + Main/Second 14 + 批量传输/查询备案 11 + 供应链/节点 23 = **72 条**（Task 10 起草中已实算纠偏 24→23，全文以 72 为准）。

---

## 修订记录

| 版本 | 日期 | 变更 |
|------|------|------|
| v0.1 | 2026-06-11 | 初稿（研究实测 + 4 分组 + 72 条母本 + 引擎 5 扩展） |
| v0.2 | 2026-06-12 | santa Round 1 REVISE 修订：BLOCKER-1 Result 统一全 52 码（Task 7 重写）；BLOCKER-2 G1 改 `scope: HEAD`（Task 1 增 headElements/hasElementInHead、Task 2 增 Scope 枚举 + RuleDef.scope、Task 6 yaml + 验收 7）；MAJOR-1 yaml 通配键 `"[*]"`（Task 5/6 + Binder 防回归测试）；MINOR-1 DependentEnumRule 行号 42-43 修正。两 BLOCKER 均经 muzhou 2026-06-12 AskUserQuestion 重拍。 |
| v0.3 | 2026-06-12 | santa Round 2 PASS 后落 3 MINOR（评审明示不触发 Round 3）：Task 2 Files 补列 RuleDefinitionProperties/ConfiguredRuleFactory；Task 5 增"通配+具体并存聚合"用例；Task 3/4/5 补"按验收标准全写"锚点注。 |

## 评审与签字

- [x] AI 独立评审 Round 1（santa-method，2026-06-11）：**REVISE** — BLOCKER×2 / MAJOR×1 / MINOR×1，全部已修订并经 muzhou 重拍
- [x] AI 独立评审 Round 2（santa-method，2026-06-12）：**PASS** — 4 项修订全链路落实核对 ✓ + 新设计技术正确性实码核实 ✓ + 数据自洽重算 ✓；3 MINOR（文本完备性）已随 v0.3 落，评审明示不触发 Round 3
- [ ] muzhou 批准签字（签字前对 PDF 抽样核对母本值集）

**Plan 状态:** DRAFT v0.3（评审通过，待签字）

---

## 批准签字

**Plan Approver:** muzhou
**决策:** ✅ 批准（含 PR-B PR-Size 豁免预批 + G1 `"[*]"` HEAD-scope 实现方式确认 + Result 全 52 码统一确认 + 2 处规范笔误处置确认）
**签字日期:** 2026-06-12
**方式:** AskUserQuestion（批准签字，立即实施）

**Plan 状态:** ✅ SIGNED v0.3（实施中）
