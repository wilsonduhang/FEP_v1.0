# §5.9 报表生成与报送（FR-WEB-REPORT）缺口盘点 + 分阶段实施 Plan

> **For Claude:** 本文件是**缺口盘点 + 决策门 Plan**（doc-only 起草），非可直接执行的 TDD 代码 Plan。原因见 §4：§5.9 当前同时受**原型缺失**与**报表内容/格式规范未定**双重阻塞，强行编写「报表模板=X、列=Y」级别的 TDD 任务会落在未决假设上构建（违反 CLAUDE.md AI 诚信铁律 §决策门纪律）。本 Plan 的交付物是「把缺口、阻塞与决策门讲清楚，给 muzhou/甲方一个可签字的方向」，待阻塞清除后再派生可执行的 TDD 子 Plan。

**Goal（本 Plan）:** 盘清 PRD §5.9「报表生成与报送」(FR-WEB-REPORT) 的 PRD 要求 vs 代码现状缺口，精确定位阻塞源与决策门，给出在阻塞清除前**可建/不可建**的判断与分阶段路线，避免 YAGNI 投机式造引擎。

**Architecture（盘点结论速览）:** §5.9 三能力（报表模板管理 / 报表生成 Excel·PDF·XML / 报表报送经 TLQ）在代码中**基本未建**；现有 `fep-web/submission/*` 模块与 `SubReportController` 覆盖的是**报送记录/配置/概况**（邻接功能）而非「报表模板驱动的生成」。报表数据源（`integration/tracking/*` 业务字段专表）已具雏形。主要阻塞 = 原型缺失（UI）+ 报表内容/格式规范未定（PRD §5.9 与《监管报送前置系统产品设计文档》§4 实测均为纲领级、无报表清单/字段口径，须甲方逐条定义）+ 报送通道归属未定。

**Tech Stack（若 Phase A 后续落地）:** Java 17 / Spring Boot 3.x；报表生成候选 Apache POI(Excel) + OpenPDF/PDFBox(PDF) + JAXB(XML)；数据源 JPA（`integration/tracking/*`）；报送复用既有 TLQ outbound 流水线。**本 Plan 不引入任何依赖、不写任何代码。**

**FR-ID:** FR-WEB-REPORT（PRD §5.9 报表生成与报送）。
**不在范围:** FR-WEB-OPS（PRD §5.9.1 运维监控）——独立 FR、独立 Plan，本盘点不覆盖。

**执行 Worktree:** `E:\FEP_v1.0_wt-report-gap`（分支 `docs/msg59-report-gap-plan`，触发条件第 2 项「与已签字未执行的 Plan 并存」+ 多会话并发 4 活跃 worktree）。本 Plan 为 doc-only，落 `docs/plans/`，零代码零构建。

**开发模式:** 盘点/Plan 起草 = Mode A（AI 主导）；后续实施模式待阻塞清除后按子能力定（报表生成引擎倾向 Mode B/C，报送签验若涉 SM2 则 ⛔ 走安全评审网）。

---

## §1 PRD §5.9 要求（逐条复述，源 PRD v1.3 行 1604-1610）

> 数据来源：《监管报送前置系统产品设计文档》第 3.4 节。**原型中无专门页面，需补充原型设计。**（PRD v1.3:1606 原文）
>
> **盘点期实测订正（红线 `feedback_prd_audit` 追溯）：** 该设计文档**存在于仓内** `docs/PRD/人行前置业务平台材料/监管报送前置系统产品设计文档.docx`（374KB，已 python-docx 提取通读）。但 PRD 引「§3.4」**有误**——报表内容实际位于该文档 **§4「报表生成与报送」**，且 §4 仅 4.1/4.2/4.3 **三条纲领 bullet，与 PRD §5.9 近乎逐字相同**；全文 119 段、仅 1 张表，实测 `报表名称/报表清单/报表编号/报送频率/日报/月报` 命中均为 **0**（`字段`=1、`模板`=3）。即：**作为「报表规范源」的设计文档，其报表段同样停留在纲领级，没有报表清单/字段口径/模板格式定义。** → B-2 阻塞在源头被实证（非「文档缺失」，而是「文档在手但报表规范本身未定义」）。

§5.9 三项能力：

1. **报表模板管理** —— 支持自定义报表格式。
2. **报表生成** —— 自动根据校验后数据生成报表（**Excel / PDF / XML**）。
3. **报表报送** —— 通过 TLQ 节点传输至 HNDEMP，记录报送日志。

矩阵状态（`docs/plans/prd-traceability-matrix.md:296`）：`FR-WEB-REPORT | §5.9 | 报表生成与报送 | P6 | 🟡`（部分/未完成）。

---

## §2 代码现状盘点（grep 实测，worktree `wt-report-gap` @ origin/main `3d6ed43`）

### 2.1 邻接但**非** §5.9 的既有实现（报送记录/配置，已建）

`fep-web/src/main/java/com/puchain/fep/web/submission/` 下五子模块（实测 `git ls-files`）：
- `dashboard/`（SubDashboardController/Service + 趋势/分布 DTO）—— 报送概况仪表盘。
- `datasource/`（SubDataSource* ）—— 数据源配置。
- `outputinterface/`（SubOutputInterface*）—— 输出接口配置。
- `record/`（**SubReportController** + SubMessageSummaryController + SubSubmissionRecord*）—— 报送**记录**。
- `scene/`（SubBusinessScene*）—— 业务场景。
- 迁移：`V10__create_p6d_submission_tables.sql` + `V17__tune_sub_submission_record_indexes.sql`。

**`SubReportController`（`/api/v1/report`）实测端点**（grep `@*Mapping`）：`GET /records`、`GET /records/{recordId}`、`POST /upload`、`POST /push`、`GET /push/blocked`、`GET /records/by-type/{messageType}`、`GET /records/by-type/{messageType}/trend`。
→ 结论：这是**报送记录列表/推送/按类型统计**，**不是**「报表模板驱动的 Excel/PDF/XML 生成」。命名含 "report" 但语义是「报送记录」非「报表生成」。

### 2.2 §5.9 三能力对应实现 —— 实测**未建**

grep（`reportTemplate|ReportGenerator|generateReport|ExcelExport|XSSFWorkbook|itext|pdfbox|报表模板|报表生成`）于 `fep-web/src/main`、`fep-processor/src/main`、`fep-common/src/main`：
- 命中仅 `fep-web/.../sysmgmt/download/domain/TaskType.java`（通用下载任务类型枚举，非报表生成引擎）。
- **报表模板管理：无**（无 template 实体/Repository/Controller）。
- **报表生成（Excel/PDF/XML）：无**（无 POI/PDF 生成代码；`poi-ooxml` 虽在依赖树但无报表生成调用方——2026-06-14 PR #92 升级系传递依赖，非本能力）。
- **报表报送 + 报送日志：无**（无报表→TLQ 的专用组装/日志链路；现有 TLQ outbound 是报文 wire-out，非报表文件报送）。

### 2.3 报表数据源（已具雏形，可复用）

`fep-web/src/main/java/com/puchain/fep/web/integration/tracking/` 业务字段专表（矩阵 §349 注：「从报文体提取业务维度供 §5.9 报表/运维查询」）：
- `CorporateAccountRecordEntity`（V40）+ Repository
- `FinancingApplicationRecordEntity` + Repository
- `InvoiceVerificationRecordEntity` + Repository
- （`BatchForwardRecordEntity` 由并发会话 `feat/forward-record-tables` 在建，**本 Plan 不依赖、不触碰**）

辅以 `integration/processor/MessageProcessRecordEntity`（通用流水）、`integration/reconciliation/*`（对账/清算记录）。
→ 结论：报表「校验后数据」的来源**已有结构化落点**，报表生成引擎落地时**无须新建数据采集**，从专表查询即可。

---

## §3 缺口矩阵

| §5.9 能力 | PRD 要求 | 代码现状 | 缺口 | 主阻塞 |
|---|---|---|---|---|
| 报表模板管理 | 自定义报表格式 | 无 | 模板实体/存储/CRUD + 模板格式定义 全缺 | **报表格式规范未定**（自定义到什么粒度？模板 DSL？）+ 原型缺失 |
| 报表生成 | Excel/PDF/XML 自动生成 | 无 | 生成引擎（格式 writer）+ 报表内容/列定义 全缺 | **报表内容规范未定**（哪些报表？各报表字段/口径？源《产品设计文档》§4 已读、实测同为纲领级无清单/字段） |
| 报表报送 | TLQ 报送 + 日志 | 无（仅报文 wire-out） | 报表文件→报送通道组装 + 报送日志表 全缺 | **报送通道归属未定**（走 1101 既有批量报送路？还是新报文/文件通道？是否需 SM2 签名？） |
| 数据源 | 校验后数据 | `integration/tracking/*` 专表已建 | 无（可复用） | — |
| UI 页面 | （PRD 未定页面） | 无 | 全缺 | **原型缺失**（PRD v1.3:1606 明示「需补充原型设计」） |

---

## §4 阻塞分类学 + 决策门

§5.9 是**三重阻塞叠加**，必须分清——不可笼统记为「受原型阻塞」（那只是其一）：

- **B-1 原型阻塞（UI）** —— PRD v1.3:1606 明文「原型中无专门页面，需补充原型设计」。管理 Web 页面在原型出来前无法定 DOM/交互。
- **B-2 报表内容/格式规范阻塞（核心）** —— PRD §5.9 仅 3 行纲领，实质内容指向《监管报送前置系统产品设计文档》（仓内**存在**，§1 已实测订正：报表段在 §4 非 §3.4，且同为 3 条纲领 bullet、0 报表清单/字段/频率）。即**连「应做哪些报表、各报表字段口径、模板自定义粒度、Excel/PDF/XML 各自适用场景」的源规范都不存在**——本仓两份可得文档（PRD + 设计文档）在此处都只到纲领级。**这是比原型更硬的阻塞**：没有报表规范，后端引擎的领域模型无法定；且**无法靠读文档解锁**（已读，源文档本身无细节），只能由甲方逐条定义。
- **B-3 报送通道归属阻塞** —— 「经 TLQ 报送至 HNDEMP」与既有报文报送（如 1101 外联机构数据报送报文）关系未定：报表是作为文件附在某报文里？还是独立通道？是否需 SM2 签名（涉 ⛔ 安全评审网）？

### 决策门（须 muzhou / 甲方答复，禁 AI 臆造）

> 对照红线 `feedback_prd_audit`（数据点须可追溯源文档）+ `feedback_decision_via_askuserquestion` + AI 诚信铁律 §决策门纪律（不在未答复假设上构建门后实装）。

- **Q1（B-2）** 报表内容规范由谁定？**已实测：本仓两份可得文档（PRD §5.9 + 设计文档 §4）报表段均为纲领级，无报表清单/字段口径**——故 §5.9「应生成哪些报表、各报表字段与口径、报送频率」**只能由甲方逐条确认**（非「去找文档」可解）。**在 Q1 答复前，报表生成引擎的领域模型不可建。**
- **Q2（B-2）** 「报表模板管理-自定义格式」的自定义粒度：固定模板集（枚举）？还是用户可视化自定义模板（DSL/拖拽）？二者工作量差一个数量级。
- **Q3（B-3）** 报表报送通道：复用既有报文报送链（指明报文号），还是新建文件报送通道？是否需 SM2 签名？
- **Q4（B-1）** UI 原型补充时间表：原型未出前，Phase C（管理 Web）不启动。
- **Q5（YAGNI 守门）** 是否允许在 Q1-Q3 未决前先建「格式无关的报表生成引擎骨架」？**默认不允许**（见 §5 风险）——除非 muzhou 显式接受「先建骨架」的投机风险。

---

## §5 分阶段路线（条件化，非承诺式）

**总原则：YAGNI + 不在未决规范上造引擎。** 项目已有明训「retrofit 避『建了没人用』」（S2 note）。报表生成引擎若在 B-2 未决时先建，极可能与最终报表规范错配返工，且零消费者。故路线按阻塞清除条件化推进：

### Phase 0（本 Plan，doc-only，✅ 可立即做） —— 缺口盘点 + 决策门
- 交付：本文件。出口：muzhou 就 §4 Q1-Q5 拍板 + 推动甲方答复 Q1/Q3。
- **不含任何代码 Task。**

### Phase A（后端引擎，**门控于 Q1+Q2+Q5**） —— 报表生成引擎 + 模板管理
- 前置：Q1（报表清单/字段口径）+ Q2（模板粒度）+ Q5（或显式接受骨架投机风险）全部答复。
- 内容（答复后派生独立 TDD 子 Plan，届时逐 Task 写失败测试→最小实现→commit）：报表模板实体 + 生成引擎（按 Q1 清单实现 Excel/PDF/XML writer，TDD 用**真生成器**断言产物结构，镜像红线 `xsd_compliance_fix_real_validator_on_sut` 思路）+ 数据源查询（复用 `integration/tracking/*`）。
- 迁移：新增 `report_template` / `report_generation_log` 表须 **grep 实测当时最大 V 编号取 V_n+1**（红线 `feedback_plan_flyway_v_collision_check`；注意并发会话已占 V41，本 Plan 落地时至少 V43+，须重测）。

### Phase B（报送链路，**门控于 Q3 + 可能 ⛔ 安全网**） —— 报表报送 + 日志
- 前置：Q3（通道归属/签名）答复。若需 SM2 签名 → ⛔ 走 santa 双审 + 密码学专项 + muzhou 签字。
- 内容：报表文件→报送通道组装 + 报送日志表 + 失败重试（可复用 callback/outbound retry 范式）。

### Phase C（管理 Web，**门控于 Q4 原型**） —— 前端页面
- 前置：Q4 原型补充完成。内容：报表模板配置页 + 报表生成/下载页 + 报送记录页（Vue3 + Element Plus，复用 `features/submission` 范式）。
- **原型未出前不启动**（红线：spec_drift_caught_by_e2e —— 无原型的 UI 必返工）。

> 依赖序：Phase A → Phase B（报送依赖有报表产物）；Phase C 并行于 A/B 但门控独立原型。Phase 0 之后**任何 Phase 不得在其门控 Q 未答复前启动实装**。

---

## §6 给 muzhou 的决策清单（建议走 AskUserQuestion）

1. **§4 Q1-Q5** 逐项拍板（尤其 Q1 报表规范来源、Q5 是否允许骨架投机）。
2. 是否需要我**向甲方发起 Q1/Q3 澄清**（拟澄清问题清单，doc-only，可由本会话起草）？
3. §5.9 优先级：当前 4 会话并发、§5.9 三重阻塞，是否**暂缓 §5.9 至阻塞清除**，本会话转其他独立项（如 mac 侧红线同步 / Simplify deferred drain）？

---

## §7 本 Plan 自检（对照 plan-review-checklist + 诚信铁律）

- [x] 每条 PRD 要求引 PRD v1.3 行号；现状每条引 grep 实测（非记忆）。
- [x] 阻塞与决策门显式化，**未在未决规范上编造 TDD 代码 Task**（诚信铁律 §决策门纪律）。
- [x] §5.9.1 运维监控明示出范围（独立 FR-WEB-OPS）。
- [x] Flyway V 冲突风险显式标注（并发会话占 V41，落地须重测）。
- [x] 执行 Worktree 已声明（doc-only，`wt-report-gap`）。
- [x] 未触碰 4 活跃会话文件域（security-impl / audit-review / processor-pipeline+tracking / dispatcher-test）。
- [x] **santa 独立评审 PASS**（5 轴全 PASS：PRD 对齐/现状准确/阻塞逻辑/诚信/不撞车）；评审揪出「设计文档实际存在」→ 本 Plan 已实测通读该 docx 并把 B-2 由「文档缺失」**强化订正**为「文档在手但报表规范本身纲领级、源头未定义」（§1 订正块 + §4 B-2/Q1 + §3 矩阵 + Architecture 行均已同步）。
- [x] **muzhou 签字**（2026-06-17）—— 决定：**签字归档本盘点 Plan + 起草甲方澄清问题清单**（§6.2 选项）。§6.3 优先级（是否暂缓 §5.9）随澄清问题发出后再议。

---

> **签字:** muzhou（2026-06-17）· Plan 状态 = SIGNED（盘点 baseline 已锁；Phase A/B/C 实装仍门控 §4 Q1-Q5，未解锁前不启动）。
> **下一动作（本会话）:** 起草 `docs/plans/2026-06-17-msg59-report-clarification-questions.md`（甲方澄清问题清单，doc-only）。

---

## §8 Worktree 闭环（session-end 时执行，本 Plan 收尾）

```bash
# 本 Plan 为 doc-only，commit 后：
git -C E:\FEP_v1.0_wt-report-gap add docs/plans/2026-06-17-msg59-report-gap-and-plan.md
git -C E:\FEP_v1.0_wt-report-gap commit -m "docs(plans): §5.9 报表 FR-WEB-REPORT 缺口盘点 + 决策门 Plan"
# 评审/签字后按 muzhou 决定推送/开 PR；worktree teardown：
git worktree remove E:\FEP_v1.0_wt-report-gap
```
