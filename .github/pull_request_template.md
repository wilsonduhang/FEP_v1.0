## 变更概要

<!-- 1-3 句话说明这个 PR 做了什么、为什么做 -->

## 变更类型

- [ ] feat — 新功能
- [ ] fix — Bug 修复
- [ ] refactor — 重构（无行为变更）
- [ ] test — 测试
- [ ] docs — 文档
- [ ] chore — 杂项（构建/依赖/配置）
- [ ] ci — CI/CD 配置
- [ ] build — 构建系统

## AI 协同模式

- [ ] 模式 A (90% AI) — CRUD / 配置 / 测试 / 文档
- [ ] 模式 B (70% AI) — 业务逻辑 / 适配器 / 报文
- [ ] 模式 C (60% AI) — 校验规则 / 字段映射
- [ ] 模式 D (50% AI) — 技术攻关 / 复杂问题
- [ ] 模式 E (0% AI) — ⛔ 国密/密钥/安全逻辑 (人工编写)

## AI 代码评审清单

> 对照 `docs/guides/ai-code-review-checklist.md` 逐项确认（AI 生成代码强制）:

- [ ] 所有 catch 块都有处理（无吞异常/空 catch）
- [ ] 测试断言验证真实业务含义（不是空 `assertNotNull`）
- [ ] 边界条件覆盖：null / 空集合 / 极限值
- [ ] 日志中无敏感数据（身份证/银行卡号/手机号/密钥）
- [ ] 没有引入未使用的抽象 / 未用到的参数
- [ ] 与本模块已有代码风格一致
- [ ] 无硬编码配置（URL / 密码 / 密钥）
- [ ] 公共方法有完整 Javadoc
- [ ] 无 `System.out.println` / `e.printStackTrace()`

## 测试说明

<!-- 列出新增/修改的测试，以及手动测试步骤 -->

- 新增测试：
- 修改测试：
- 手动测试步骤：

## 二次 AI 评审（核心模块必填）

> 适用于 `security/` `converter/` `processor/`（对账） 等核心模块

- [ ] 不适用（非核心模块）
- [ ] 已通过 santa-method 二次评审，报告链接：
- [ ] 已通过 code-reviewer agent 评审，报告链接：

## 本地质量门禁

- [ ] `./mvnw verify` 本地通过（Checkstyle / SpotBugs / JaCoCo / ArchUnit 零违规）
- [ ] 新增代码有对应测试
- [ ] CI 绿灯

## Commit 签名

- AI-Generated: claude-code
- Reviewed-By: <你的姓名>
- <!-- 如涉及安全代码，额外加：Security-Reviewed-By: ③<安全工程师姓名> -->
