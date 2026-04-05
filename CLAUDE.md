# CLAUDE.md

> FEP 综合前置平台 · Claude Code 项目知识库
> 最后更新: 2026-04-03

## 项目概述

**FEP (Front-End Processor) 综合前置平台** — 部署在各接入机构侧（商业银行、供应链信息服务机构等）的统一接入枢纽与数据交换中枢，作为机构内部业务系统与湖南省金融大数据中心 (HNDEMP) 之间的桥梁。

核心能力：
- 44 个报文类型的双向安全交互（供应链融资 23 + 企业查询 12 + 通用 9）
- 双角色：受理单位（银行）/ 供应链信息服务机构，通过配置切换
- 双模式：接口模式（API 对接行内系统）/ 数仓模式（平台自行采集组装）
- 管理 Web：8 大模块（登录/首页/业务管理/API接口/系统管理/报送管理/运维监控/帮助）
- 报文鉴权、国密 SM2/SM3/SM4 签名加密、XML↔JSON 格式转换、TLQ 智能路由

## ⚠️ 关键定位 (必读)

本 FEP 与甲方架构图中第4层"前置系统 port:9001"是两套完全不同的系统！

| 维度 | 我们的 FEP (机构侧) | 架构图中的前置系统 (平台侧) |
|------|-------------------|------------------------|
| 部署位置 | 各接入机构机房 | 人行平台 DMZ 外联区 |
| 服务对象 | 机构业务系统 ↔ HNDEMP | 核心企业/融资企业/CFCA |
| 核心功能 | 报文组装/拆解、签名验签、加解密、路由转发 | 业务受理、凭证登记、融资审批 |

## ⛔ AI 禁入区域 (强制)

以下代码必须由 ③ 安全/密码专家 100% 人工编写，Claude Code 完全禁止参与：

- SM2 数字签名/验签
- SM3 哈希摘要
- SM4 对称加解密
- 密钥管理（生成/派生/存储/轮换/销毁）
- 数据脱敏核心规则（身份证/银行卡号/手机号）
- 审计日志安全逻辑（不可篡改/完整性校验）

安全模块使用分层隔离：`security/api/`（接口定义，Claude Code 可见）vs `security/impl/`（实现，AI 禁入）

## 四层架构

```
数据采集层    → 适配器模式: JDBC/ESB/文件/MQ (因银行而异)
业务处理层    → 校验引擎(XSD+6264规则) + 状态机 + 对账引擎
报文转换层    → 字段映射 + 编码转换 + 脱敏 + 加密 + 报文模板
TLQ 通信层   → TLQ Producer/Consumer + 消息去重 + 重试 + 死信队列
辅助模块      → 鉴权 + 认证 + 沙箱 + 回调
```

## 包结构

```
com.puchain.fep/
├── collector/          # 数据采集层
├── processor/          # 业务处理层
├── converter/          # 报文转换层
├── transport/          # TLQ 通信层
├── security/           # 安全模块 ⛔
│   ├── api/            #   接口定义 (Claude Code 可见)
│   └── impl/           #   实现 (⛔ 人工编写)
├── web/                # 管理 Web API
├── common/             # 公共模块
├── config/             # 配置
└── infra/              # 基础设施
```

## 技术栈

| 层级 | 选型 |
|------|------|
| 语言 | Java 17+ / Spring Boot 3.x |
| 消息中间件 | TLQ (东方通 TongLINK/Q) 8.1.15.2_p6+ |
| 前端 | Vue3 + TypeScript + Element Plus |
| 数据库 | H2 嵌入式 / MySQL / PostgreSQL |
| 安全 | BouncyCastle (国密 SM2/SM3/SM4) |
| 配置中心 | Nacos 2.x |
| 链路追踪 | SkyWalking |
| 监控 | Prometheus + Grafana |

## AI Agent 规范

- **唯一 AI Agent: Claude Code** — 不使用 Codex、DeepSeek 或任何其他 AI
- **5 种开发模式**: A(AI主导90%) / B(AI起草+人审核70%) / C(人定规则+AI编码60%) / D(人机结对50%) / E(⛔纯人工0%)
- **Commit 标注**: `AI-Generated: claude-code` + `Reviewed-By: <姓名>`
- **安全代码 Commit**: `Security-Reviewed-By: ③<姓名>` (不含 AI-Generated)

## 文档体系

```
FEP/
├── CLAUDE.md                         ← 本文件
└── docs/
    ├── README.md                     ← 文档索引 (入口)
    ├── PRD/                          ← PRD v1.3 + 原型截图 + 甲方资料[A]-[H]
    ├── architecture/                 ← 架构设计方案 v4.0
    ├── team/                         ← 团队组建方案 v4.0
    ├── guides/                       ← 规范·约束·指引 (统一存放)
    ├── business/                     ← 业务理解
    └── references/                   ← 行业研究资料
```

## 编码规范摘要

- **命名**: 类 `UpperCamelCase`, 方法 `lowerCamelCase`, 常量 `UPPER_SNAKE_CASE`
- **异常**: 禁止吞异常，必须记录日志或向上抛出
- **日志**: ERROR(业务异常) / WARN(可恢复) / INFO(关键节点) / DEBUG(调试)
- **敏感数据**: 日志禁止明文银行卡号/身份证/手机号，必须调用脱敏工具
- **Javadoc**: Claude Code 生成的公共类/方法必须包含完整 Javadoc
- **OpenAPI**: Controller 必须包含 @Tag/@Operation/@Parameter/@ApiResponse 注解
- **格式化**: Checkstyle (阿里巴巴 Java 规范) + .editorconfig

## 已知约束

- 报文 XML 编码必须 UTF-8
- TLQ 单属性最大 8KB (xmlstr/xmlstr1/xmlstr2 共 24KB)
- 大报文分拆不能断 UTF-8 字符
- SM4 加密使用 ECB 模式 + PKCS#7 填充
- 数字签名: SM3withSM2 裸签, Base64 编码, XML 注释方式存储
- HNDEMP 中心节点代码: A1000143000104

## 术语速查

| 术语 | 含义 |
|------|------|
| FEP | Front-End Processor, 综合前置平台 |
| HNDEMP | 湖南省金融大数据中心数据交换管理平台 |
| TLQ | 东方通 TongLINK/Q, 国产消息中间件 |
| USCI | 统一社会信用代码 (18位) |

## 当前项目状态

- **阶段**: P0.5 完成（质量门禁已就位，进入 P1 业务开发）
- **已完成**:
  - PRD v1.3 (3轮审计) / 架构 v4.0 / 团队 v4.0 / 开发规范 v3.0
  - P0: Maven 多模块骨架（parent + 8 子模块）+ GitHub Actions CI
  - P0.5: 5 层质量门禁
- **代码仓库**: `github.com/wilsonduhang/FEP_v1.0`
- **质量门禁（P0.5 交付）**:
  | 层级 | 工具 | 阈值/规则 |
  |------|------|-----------|
  | 风格 | Checkstyle | 零违规，140 行宽，禁用 System.out/printStackTrace |
  | Bug | SpotBugs + Find Security Bugs | effort=Max, threshold=Low, 零违规 |
  | 覆盖率 | JaCoCo | 行 ≥80% / 分支 ≥70% |
  | 架构 | ArchUnit | 8 层依赖方向 + security.impl 隔离 + 命名规范 |
  | 依赖 | OWASP Dependency-Check | CVSS ≥7 阻断构建（nightly） |
  | 变异 | Pitest | fep-common 启用，mutation ≥80%（nightly） |
  | 综合 | SonarCloud 免费版 | Sonar Way 质量门（nightly） |
- **CI 双轨制**:
  - PR 快检（≤5min）: ci.yml — 编译/测试/静态分析/400 行 PR 上限
  - nightly 深检（≤40min）: nightly.yml — OWASP + Pitest + SonarCloud
- **评审流程**:
  - 所有 PR 使用 `.github/pull_request_template.md` 强制填写 AI 占比 + 9 项评审清单
  - 核心模块（security/converter/processor）强制二次 AI 评审（santa-method / code-reviewer agent）
- **下一步**: P1 — TLQ 通信层 (Producer/Consumer/去重/重试/死信队列)
