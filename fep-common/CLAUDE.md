# fep-common 模块指南

## 职责
公共基础模块：提供所有子模块共享的工具类、异常体系、通用 DTO、校验注解。修改影响全部 8 个子模块，必须向前兼容。

## 核心类

| 类 | 职责 |
|---|---|
| ApiResult | 统一 API 响应包装（code/message/data） |
| FepErrorCode | 全局错误码枚举（AUTH_04xx/BIZ_50xx/SYS_99xx） |
| FepBusinessException | 业务异常基类（含 errorCode + message） |
| FepAuthException | 认证异常（继承 FepBusinessException） |
| GlobalExceptionHandler | @RestControllerAdvice 全局异常处理 |
| PageQuery / PageResult | 分页请求/响应 |
| LogSanitizer | 日志脱敏（替换 \r\n，防 CRLF 注入） |
| IdGenerator | UUID 生成（uuid32 去横线） |
| PasswordHasher | 密码散列接口（impl 在 fep-web） |
| PasswordComplexity | 密码复杂度校验注解 + Validator |
| TraceIdFilter | 请求链路追踪 ID 注入 |

## 包结构
```
common/
├── domain/         # ApiResult, PageQuery, PageResult, FepErrorCode
├── exception/      # FepBusinessException, FepAuthException, GlobalExceptionHandler
├── security/       # PasswordHasher 接口
├── util/           # LogSanitizer, IdGenerator
├── validation/     # PasswordComplexity 注解 + Validator
└── trace/          # TraceIdFilter
```

## 依赖规则
- ✅ 可以依赖: Spring Web, Validation API, SLF4J（仅标准库）
- ❌ 禁止依赖: 任何其他 fep-* 模块（ArchUnit 强制：common 是最底层）
- ❌ 禁止引入: JPA, Redis, 消息中间件等重型依赖

## 向前兼容约束
- **不要删除或重命名** 现有公共类/方法/枚举值（其他模块在用）
- **新增错误码**: 按前缀分组（AUTH_04xx, BIZ_50xx, SYS_99xx）
- **新增工具类**: 先 grep 是否已有同功能方法
- **修改接口签名**: 需确认所有调用方都更新

## 测试策略
- **纯单元测试**: 不启动 Spring 上下文（速度快）
- **Pitest mutation score ≥ 80%**: nightly 检查
- **覆盖率**: 行 ≥ 80% / 分支 ≥ 70%

## 常见陷阱
1. **GlobalExceptionHandler 顺序**: @Order 注解影响异常处理优先级
2. **FepErrorCode 新增**: 枚举值必须有唯一 code 和中文 message
3. **LogSanitizer**: 仅处理 CRLF 注入，不做业务脱敏（身份证/卡号脱敏在 security 模块）
4. **PasswordComplexity**: 至少两类字符（大小写/数字/特殊），长度 8-32

## 新增文件 Checklist
- [ ] 工具类 → 检查是否已有同功能方法（grep 先行）
- [ ] 异常类 → 继承 FepBusinessException 或 FepAuthException
- [ ] 枚举 → 值不可重复，含 Javadoc 说明
- [ ] 公共方法 → 完整 Javadoc + 边界测试（null/空/极值）
