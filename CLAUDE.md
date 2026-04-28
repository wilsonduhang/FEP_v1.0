# FEP_v1.0 代码仓库

> 本仓库 (`FEP_v1.0`) 是 FEP 综合前置平台的代码实现。
> **项目知识库主入口**: `../FEP/CLAUDE.md`（包含项目定位、AI 禁入区域、Plan 治理、质量门禁、红线索引、阶段历史等）
>
> 目录布局假设：本仓库与项目知识库目录 `FEP/` 并列存放（如 `~/FEP/` 与 `~/FEP_v1.0/`）。`FEP/` 不是 git 仓库，由 muzhou 单独维护文档与 Plan，团队成员需向 muzhou 索取或独立访问。

## 当前阶段
P3 完成 — 详见 `../FEP/docs/plans/PHASE_HISTORY.md`

## 模块入口
- `fep-common/CLAUDE.md` — 公共基础模块（异常 / DTO / 校验 / 工具类）
- `fep-web/CLAUDE.md` — 管理 Web 后端（REST API / RBAC / 系统管理）
- 其他子模块（`fep-collector` / `fep-converter` / `fep-processor` / `fep-transport` / `fep-security-api` / `fep-security-mock`）— 模块约束见 ArchUnit 测试 + `../FEP/CLAUDE.md` "包结构"段

## 本地一键门禁
```bash
./mvnw verify --batch-mode --no-transfer-progress
```

## 常用快速命令
见 `../FEP/CLAUDE.md` "快速命令"段（后端 verify / Redis / 前端 / E2E / Flyway）。
