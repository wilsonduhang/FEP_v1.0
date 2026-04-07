# fep-web 模块指南

## 职责
管理 Web 后端：提供 REST API（认证/RBAC/系统管理 8 模块），是用户直接交互的入口层。

## 核心类

| 类 | 职责 | 依赖数 | 行数上限 |
|---|---|:---:|:---:|
| AuthService | 登录/登出/刷新 token 的流程编排 | ≤ 7 | ≤ 300 |
| SysUserService | 用户 CRUD + 锁定/解锁/重置密码 | ≤ 5 | ≤ 300 |
| SysRoleService | 角色 CRUD + 权限分配 | ≤ 4 | ≤ 250 |
| SysMenuService | 菜单树管理 + 用户菜单查询 | ≤ 4 | ≤ 250 |
| SecurityConfiguration | Spring Security 过滤链配置 | — | ≤ 150 |

## 包结构
```
web/
├── auth/           # 认证（JWT/验证码/SSO/登录尝试）
├── config/         # Spring 配置（JPA/Redis/OpenAPI/Security）
└── sysmgmt/        # 系统管理
    ├── user/       # 用户管理
    ├── role/       # 角色管理
    ├── menu/       # 菜单管理
    ├── message/    # 消息管理 (P6a.2)
    ├── download/   # 下载任务 (P6a.2)
    ├── log/        # 操作日志 (P6a.2)
    ├── help/       # 帮助面板 (P6a.2)
    └── rel/        # 关联表 Repository
```

## 依赖规则
- ✅ 可以依赖: fep-common, fep-security-api, fep-security-mock(test scope)
- ❌ 禁止依赖: fep-transport, fep-converter, fep-processor, fep-collector
- ⛔ 禁止: Controller 直接注入 Repository（ArchUnit 强制）

## 测试策略
- **单元测试**: Mock 外部依赖（Redis 用 TestRedisConfiguration 提供 ConcurrentHashMap 替代）
- **集成测试**: @SpringBootTest + H2 + Flyway 自动迁移（profile=test）
- **Controller 测试**: @SpringBootTest + @AutoConfigureMockMvc + @MockBean Service 层
- **覆盖率**: 行 ≥ 80% / 分支 ≥ 70%（Entity/DTO/Configuration 已排除）

## 常见陷阱
1. **@Configuration 命名**: 必须以 "Configuration" 结尾（不是 "Config"），ArchUnit 强制
2. **JaCoCo 排除**: 新增 Entity/DTO 后必须在父 POM `jacoco-maven-plugin` excludes 中添加排除
3. **SpotBugs CRLF 注入**: 日志中记录用户输入必须经 `LogSanitizer.sanitize()` 处理
4. **Redis 测试**: 不要 mock Redis 本身，使用 TestRedisConfiguration（ConcurrentHashMap 支撑）
5. **构造器参数 ≤ 7**: ArchUnit ClassDesignTest 强制，超过需要拆分 Service

## 新增文件 Checklist
- [ ] Entity/DTO → 更新父 POM JaCoCo excludes
- [ ] @Configuration → 类名以 "Configuration" 结尾
- [ ] Controller → @Tag + @Operation + @Parameter + @ApiResponse 注解
- [ ] Service → 检查构造器参数数 ≤ 7
- [ ] 日志中有用户输入 → `LogSanitizer.sanitize()`
- [ ] Flyway 迁移 → 版本号递增（V{N}__description.sql）
- [ ] 新增公共方法 → Javadoc（描述行为，非参数名）
