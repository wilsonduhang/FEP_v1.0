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
├── auth/                  # 认证（JWT/验证码/SSO/登录尝试） P6a.1
├── common/                # web 模块共享 helper
├── config/                # Spring 配置（JPA/Redis/OpenAPI/Security）
├── dashboard/             # 首页看板统计 P6e + P7.1
├── sysmgmt/               # 系统管理 P6a.1-P6a.2
│   ├── user/              #   用户管理
│   ├── role/              #   角色管理
│   ├── menu/              #   菜单管理
│   ├── message/           #   消息管理
│   ├── download/          #   下载任务
│   ├── log/               #   操作日志
│   ├── help/              #   帮助面板
│   └── rel/               #   关联表 Repository
├── system/                # 系统配置（10 张表 / 8 子模块）P6a.3
├── entquery/              # 企业信息查询管理（查询任务/授权书/结果）P6c
├── submission/            # 数据报送管理（输出接口/数据源/场景/记录）P6d / P7.2c
├── bizdata/               # 业务数据管理（待办/快捷/报文定义/报文记录）P6e
├── tlq/                   # TLQ 节点管理（节点/队列/连通性）P6f
├── reconciliation/        # 对账引擎 + Listener（FR-PROC-RECON-*）P2e / P3
│   └── listener/          #   3 EventListener: BankRecon / PlatformRecon / ClearingInstr
├── messageinbound/        # 入站报文调度（POST /api/v1/messages/inbound + dispatcher + ApplicationEvent）P3
└── integration/           # 跨模块集成（事件总线 / TLQ Consumer wiring）
```

## 依赖规则
- ✅ 可以依赖: fep-common, fep-security-api, fep-security-mock(test scope), fep-processor（P2e 后允许）, fep-converter（P3 后允许）
- ❌ 禁止依赖: fep-transport, fep-collector（保持单向 web → processor → converter）
- ⛔ 禁止: Controller 直接注入 Repository（ArchUnit `controllers_must_not_directly_depend_on_repositories` 强制 — P2e Task 7 触发引入 2 个 Query Service 修复）

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
6. **测试类命名 `*Test.java`**: Surefire 默认仅 include `*Test.java` / `*Tests.java` / `*TestCase.java`，命名为 `*IT.java` 会被静默跳过（P2b-DEFECT-002 教训：11 个测试静默 4 周；用 Failsafe 跑 IT 需额外配置，本项目目前所有 IT 都用 `*Test.java` 后缀）
7. **Controller 不直接注入 Repository**: ArchUnit `controllers_must_not_directly_depend_on_repositories` 强制，需用 thin Query Service 隔离（P2e Task 7 触发，引入 2 个 Query Service 修复）
8. **ApplicationEvent 事务边界**: dispatcher `@Transactional REQUIRED` + listener throw 会回滚整事务（P3 Case 7 @SpyBean 测试验证双表 0 行）— 若需 listener 失败不影响主流程，需 `@TransactionalEventListener(phase=AFTER_COMMIT)`

## 新增文件 Checklist
- [ ] Entity/DTO → 更新父 POM JaCoCo excludes
- [ ] @Configuration → 类名以 "Configuration" 结尾
- [ ] Controller → @Tag + @Operation + @Parameter + @ApiResponse 注解
- [ ] Service → 检查构造器参数数 ≤ 7
- [ ] 日志中有用户输入 → `LogSanitizer.sanitize()`
- [ ] Flyway 迁移 → 版本号递增（V{N}__description.sql）
- [ ] 新增公共方法 → Javadoc（描述行为，非参数名）
