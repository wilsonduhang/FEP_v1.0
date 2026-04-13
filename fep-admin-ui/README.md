# FEP Admin UI

FEP 综合前置平台管理 Web 前端 (Vue3 + TypeScript + Vite + Element Plus)。

## 环境要求

- Node.js >= 20.0.0
- pnpm >= 9.0.0
- 后端 `fep-web` 运行在 `http://localhost:8080`

## 安装

```bash
pnpm install
```

## 开发

```bash
pnpm dev          # 启动 Vite dev server on http://127.0.0.1:5173
pnpm type-check   # TypeScript 静态检查
pnpm lint         # ESLint
pnpm test         # Vitest 单元测试
pnpm build        # 生产构建到 dist/
```

Dev 模式下，所有 `/api/*` 请求通过 Vite proxy 转发到 `http://localhost:8080`。

## 目录结构

```
src/
├── shared/      # 跨 feature 共享：类型、HTTP 客户端、工具
├── stores/      # Pinia 全局 store
├── router/      # 路由表 + 守卫
├── layouts/     # 布局壳（BlankLayout / AdminLayout）
└── features/    # 业务模块，每个模块自含 api/store/views/components
```

## 已知待办（P7.2 及以后）

- [x] P7.1 完成（2026-04-12）：SM2 登录加固 + 首页看板 + 权限守卫
- [ ] 业务信息管理（PRD §5.3）
- [ ] 企业信息查询（PRD §5.4）
- [ ] 数据报送管理（PRD §5.5）
- [ ] 报送管理 / TLQ 节点管理 / 系统管理（PRD §5.6–§5.10）
- [ ] 实时推送（WebSocket 或 SSE，待评估）

## 开发规范

遵循 `/Users/muzhou/FEP/CLAUDE.md` 与 `docs/guides/ai-code-review-checklist.md`。
