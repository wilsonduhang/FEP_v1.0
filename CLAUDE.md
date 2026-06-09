# FEP_v1.0 代码仓库

> 本仓库 (`FEP_v1.0`) 是 FEP 综合前置平台的代码实现。
> **项目知识库主入口**: `../FEP/CLAUDE.md`（包含项目定位、AI 禁入区域、Plan 治理、质量门禁、红线索引、阶段历史等）
>
> 目录布局假设：本仓库与项目知识库目录 `FEP/` 并列存放（如 `~/FEP/` 与 `~/FEP_v1.0/`）。`FEP/` 不是 git 仓库，由 muzhou 单独维护文档与 Plan，团队成员需向 muzhou 索取或独立访问。

## 当前阶段
S1 国密 SM4 + KeyServiceImpl + callback_credential 惰性双读迁移 — 详见 `../FEP/docs/plans/PHASE_HISTORY.md`
（CryptoServiceImpl SM4/ECB/PKCS7 + KeyServiceImpl 多版本主密钥 + GmSecurityConfiguration `provider=impl` 门控 + 惰性迁移；`provider=impl` prod cutover gated on S2 SM2。完整状态见 `../FEP/CLAUDE.md` 当前项目状态段）

## 模块入口
- `fep-common/CLAUDE.md` — 公共基础模块（异常 / DTO / 校验 / 工具类）
- `fep-web/CLAUDE.md` — 管理 Web 后端（REST API / RBAC / 系统管理）
- 其他子模块（`fep-collector` / `fep-converter` / `fep-processor` / `fep-transport` / `fep-security-api` / `fep-security-mock`）— 模块约束见 ArchUnit 测试 + `../FEP/CLAUDE.md` "包结构"段

## 本地一键门禁
```bash
./mvnw verify --batch-mode --no-transfer-progress
```

## 微 Plan verify 策略（避免全 reactor 长跑）
全 reactor `./mvnw verify` 实测约 8m43s（P4-MSG-I 基线）。**单模块 / 跨少数模块的微 Plan** 推荐缩窄范围，待 PR 触发 GHA CI 跑全量门禁兜底：

```bash
# 仅改某模块（如 fep-web）的测试/小改动 —— 最快，依赖上次已构建产物
./mvnw test -pl fep-web -o -Dtest=XxxTest -Dsurefire.failIfNoSpecifiedTests=false
# 跨模块（如 fep-converter + fep-web）或上游产物未构建 —— -am 连带构建依赖模块
./mvnw test -pl fep-converter,fep-web -am
```
注意：
- `-o`（offline）依赖本地 `~/.m2` 已有上游产物；首次或上游变更后去掉 `-o` 或加 `-am`。
- `-Dtest=` 精确目标时配 `-Dsurefire.failIfNoSpecifiedTests=false`（Surefire 3.x，见 `../FEP/CLAUDE.md` 红线索引 `feedback_surefire3_failifno_specified_tests_param_rename`）。
- 缩窄 verify 仅本机自检提速；**合并前仍以 PR 触发的 GHA CI 全量门禁为准**（Checkstyle/SpotBugs/JaCoCo/ArchUnit）。

## 常用快速命令
见 `../FEP/CLAUDE.md` "快速命令"段（后端 verify / Redis / 前端 / E2E / Flyway）。
