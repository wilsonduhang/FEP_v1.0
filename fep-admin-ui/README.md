# FEP Admin UI

FEP 综合前置平台管理 Web 前端 (Vue3 + TypeScript + Vite + Element Plus)。

## 环境要求

- Node.js >= 20.0.0
- pnpm >= 9.0.0
- 后端 `fep-web` 运行在 `http://localhost:8080`
- **本地 Redis 7.x**（P7.0.2 之后后端 dev profile 强依赖，用于验证码 + 登录失败计数）：
  ```bash
  docker run -d --name fep-redis -p 6379:6379 redis:7-alpine
  ```

## 安装

```bash
pnpm install
```

## 开发

**启动顺序**（三项齐全才能完整跑通登录 + 首页看板）：

1. **Redis**（后端强依赖）
   ```bash
   docker start fep-redis  # 首次用 docker run -d --name fep-redis -p 6379:6379 redis:7-alpine
   ```

2. **后端 fep-web**（默认端口 8080）
   ```bash
   cd /Users/muzhou/FEP_v1.0
   ./mvnw spring-boot:run -pl fep-web -Dspring-boot.run.profiles=dev --batch-mode --no-transfer-progress
   ```

3. **Vite 前端**（默认端口 5173，proxy → 8080）
   ```bash
   cd fep-admin-ui
   pnpm dev
   ```

浏览器访问 `http://127.0.0.1:5173/login`，使用种子账号 `admin1` / `admin@FEP2026` 登录。

> **首次登录**会提示修改密码（`passwordChangeRequired=true`），可直接继续使用。
>
> **Redis 未启动时**：后端会成功启动（lettuce 懒连接），但调 `/auth/captcha` 等 Redis-dependent 端点会返 5xx + 日志含 `Connection refused`。

### 其他常用命令

```bash
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
- [x] P7.0.2 完成（2026-04-13）：契约对齐 + pre-existing bug 修复（F1/F2/F4/F5/F6 + R1/R2）
- [ ] 业务信息管理（PRD §5.3）
- [ ] 企业信息查询（PRD §5.4）
- [ ] 数据报送管理（PRD §5.5）
- [ ] 报送管理 / TLQ 节点管理 / 系统管理（PRD §5.6–§5.10）
- [ ] 实时推送（WebSocket 或 SSE，待评估）

## 开发规范

遵循 `/Users/muzhou/FEP/CLAUDE.md` 与 `docs/guides/ai-code-review-checklist.md`。
