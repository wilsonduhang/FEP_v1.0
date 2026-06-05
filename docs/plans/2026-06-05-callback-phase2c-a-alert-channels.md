# FEP Callback Phase 2c-A 统一告警引擎 + Email/SMS 通道 实施计划

> **执行方式:** 使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐 Task 执行。步骤使用 `- [ ]` 复选框跟踪。
> **本 Plan 为 Phase 2c 第一子计划（2c-A）。** 第二子计划 2c-B（凭证 3 增强：操作审计 + 有效期 + multi-key_id 轮换）在本 Plan ship 且 CI 绿灯后单独起草。

**目标:** 把回调死信告警从「IN_APP 硬编码无条件发」重构为「统一告警引擎按 `t_sys_alert_rule` 配置分发到 IN_APP / EMAIL / SMS 三渠道」，并新增 Email（SMTP 真实发送）与 SMS（接口抽象 + log 桩，真网关后续）两通道。

**前置依赖:** Callback Phase 2b 全 merge（origin/main `369c88b5`，含 IN_APP 通知 T11/T12 + DLQ 重放 T8 + 端到端 IT T16）。

**执行 Worktree:** `/Users/muzhou/FEP_v1.0_wt-callback-p2c-alert`（分支 `feat/callback-p2c-a-alert-channels`，触发条件第 ② + ⑥ 项）
> 红线 `feedback_worktree_for_parallel_work` 触发判定：② 与已签字未执行 Plan 并存（主 worktree 当前有别会话 19+ untracked `docs/plans/*.md` + `2026-06-05-msg-business-rule-validation-engine.md` 等并行 Plan）；⑥ 多会话并发（`.e2e` + `wt-simplify-q-drain` 别会话活跃）。**执行起始即 `git worktree add`，不在主 worktree 直接改 fep-web。** 触发条件动态判定（红线 `feedback_worktree_trigger_is_dynamic_recheck_at_execution`），T0 前重测。

**架构:** 引入 `callback.alert` 子包：`CallbackAlertEvaluator`（单一 `@EventListener` 订阅 `CallbackDeadLetterEvent`）读取 `t_sys_alert_rule` 配置（启用开关 + 多渠道集合 + 收件邮箱/手机），构造统一 `CallbackAlertMessage`，按 `NotifyMethod` 分发到实现 `CallbackAlertChannel` 接口的三个渠道（`CallbackInAppAlertChannel` 承接原 `CallbackNotificationListener` 的 ADMIN 站内信扇出逻辑、`CallbackEmailAlertChannel` 经 `JavaMailSender` 发邮件、`CallbackSmsAlertChannel` 经 `CallbackSmsGateway` 抽象发短信）。`t_sys_alert_rule` 由单值 `notify_method` 升级为多值 `notify_methods` + 新增 `alert_phone`（V34 迁移），默认行翻为「启用 + IN_APP」以保留现有 IN_APP 常开语义。

**技术栈:** Java 17 / Spring Boot 3.x / Maven / Spring `JavaMailSender`（`spring-boot-starter-mail`）/ Micrometer / Flyway / ArchUnit / JUnit 5 + Mockito + AssertJ。

**AI 协同模式:**

| 模式 | AI 占比 | 本计划中的应用 |
|:----:|:-------:|--------------|
| A | 90% | 迁移脚本 / DTO / 配置属性 / 测试 |
| B | 70% | 告警引擎分发逻辑 / 渠道实现 / 邮件组装 |
| E | 0%  | ⛔ 本 Plan 不涉国密安全（Email/SMS 凭证走 Nacos 配置注入，非密钥派生） |

> **⚠️ 安全边界声明:** 本 Plan **不触及** SM2/SM3/SM4、密钥派生、`security/impl/`。SMTP 密码 / SMS 网关密钥属**配置密钥**（`@ConfigurationProperties` + Nacos/环境变量注入），禁硬编码（质量门禁 #6）。告警正文禁含敏感数据明文（身份证/卡号/手机号），统一经 `LogSanitizer` 处理后落库/发送。

---

## 设计背景

### 现状（grep 实测 origin/main `369c88b5`）

| 组件 | 路径 | 现状 |
|---|---|---|
| `CallbackDeadLetterEvent` | `fep-web/.../callback/dlq/event/CallbackDeadLetterEvent.java:24` | record(queueId, targetInterfaceId, msgNo, retryCount, lastError, occurredAt) |
| `CallbackNotificationListener` | `fep-web/.../callback/notification/listener/CallbackNotificationListener.java:80` | `@EventListener @Transactional onDeadLetter` — **无条件**为每个 ADMIN 落 IN_APP 通知，**不读 SysAlertRule** |
| `CallbackNotificationEntity.of(...)` | `.../callback/notification/domain/CallbackNotificationEntity.java:93` | `of(userId, category, level, title, message, refId, refType)` 工厂 |
| `SysAlertRule` | `.../sysmgmt/config/alert/domain/SysAlertRule.java:23` | `t_sys_alert_rule` 单条配置；字段 ruleId/alertEnabled/threshold/alertEmail/**notifyMethod(单值)**/alertFrequency |
| `NotifyMethod` | `.../sysmgmt/config/alert/domain/NotifyMethod.java:11` | enum EMAIL/IN_APP/SMS |
| `AlertFrequency` | `.../sysmgmt/config/alert/domain/AlertFrequency.java:11` | enum REALTIME/HOURLY/DAILY |
| `SysAlertRuleService` | `.../config/alert/service/SysAlertRuleService.java:51,71` | `getRule()` / `updateRule(req)` 单条配置 CRUD |
| 默认 seed | `db/migration/V6__create_p6a3_config_tables.sql:145` | `alert_enabled=FALSE, notify_method='EMAIL', alert_frequency='REALTIME'` |
| ArchUnit | `fep-web/src/test/.../callback/CallbackModuleArchTest.java` | R1-R8（R4 命名 Callback 前缀 / R6 notification 不依赖 credential|reaper） |
| Flyway 最大版本 | `db/migration/` | **V33**（新迁移从 **V34**） |
| spring-mail 依赖 | `fep-web/pom.xml` | **不存在**（需新引入） |
| 告警前端 | `fep-admin-ui` | **无 alert 组件**（admin API 改契约零前端回归） |

### 关键设计决策

1. **IN_APP 常开语义保留（决策门 — 行为保持）**：当前 IN_APP 由 listener 无条件发（绕过 SysAlertRule），默认 seed 却是 `disabled + EMAIL`。统一引擎让 IN_APP 受配置门控后，必须在 V34 迁移把默认行翻为 `alert_enabled=TRUE, notify_methods='IN_APP'`，否则现有 IN_APP 行为回归 + 端到端 IT（`CallbackPhase2bEndToEndIT` 断言 IN_APP 出现）失败。

2. **单值 → 多值**：`notify_method`(单值，三选一) 无法表达「IN_APP + EMAIL 并发」。V34 新增 `notify_methods VARCHAR(60)`（逗号连接，如 `IN_APP,EMAIL`），经 JPA `AttributeConverter` 映射为 `Set<NotifyMethod>`；同时新增 `alert_phone VARCHAR(50)`（SMS 收件号）。旧 `notify_method` 列在 V34 中迁移后 DROP（admin DTO 同步改 `notifyMethods`，零前端依赖）。

3. **Frequency 范围界定（防隐藏占位）**：本 Plan **完整实现 REALTIME**（事件到达即分发）。`HOURLY`/`DAILY` 汇总窗口（缓冲 + 定时 flush）**显式 deferred** 到后续 Plan 2c-A-freq；本 Plan 中 `CallbackAlertEvaluator` 对非 REALTIME 频率采用**保守降级 = 按 REALTIME 立即分发**并 `LOG.debug` 标注，非 silent skip（完整代码、明确语义、文档化限制，符合无占位符规则）。

4. **threshold 语义**：现有 `threshold` 字段语义在 Phase 2b 未被消费。本 Plan 中死信告警为**逐事件触发**（每条 DEAD_LETTER 一次评估），`threshold` 暂作为「retryCount ≥ threshold 才告警」的过滤器（threshold=0 → 全部告警，兼容默认）。完整实现，语义文档化。

5. **包归属与命名**：告警引擎落 `com.puchain.fep.web.callback.alert`（源事件 `CallbackDeadLetterEvent` 在 callback 包，与现有 IN_APP listener 同域）。ArchUnit R4 要求 callback 包顶层非 record/enum 类以 `Callback` 前缀命名 → 所有新类（含接口）`Callback*` 前缀；record（`CallbackAlertMessage`）被 R4 `areNotRecords()` 排除但仍加前缀保持一致。`NotifyMethodSetConverter` 落 `sysmgmt.config.alert.domain`（不受 R4 约束）。

### 文件结构

| 文件路径 | 职责 | 操作 | AI 模式 |
|----------|------|------|:-------:|
| `db/migration/V34__alert_rule_multi_method.sql` | notify_methods 多值 + alert_phone + 翻默认行 | 新建 | A |
| `.../sysmgmt/config/alert/domain/NotifyMethodSetConverter.java` | `Set<NotifyMethod>` ↔ 逗号字符串 JPA 转换器 | 新建 | A |
| `.../sysmgmt/config/alert/domain/SysAlertRule.java` | notifyMethod(单值)→notifyMethods(Set) + alertPhone | 修改 | A |
| `.../config/alert/dto/AlertRuleResponse.java` | 同步多值 + alertPhone | 修改 | A |
| `.../config/alert/dto/AlertRuleUpdateRequest.java` | 同步多值 + alertPhone | 修改 | A |
| `.../config/alert/service/SysAlertRuleService.java` | updateRule 设多值 + alertPhone | 修改 | A |
| `.../callback/alert/CallbackAlertMessage.java` | 统一告警消息 record | 新建 | B |
| `.../callback/alert/channel/CallbackAlertChannel.java` | 渠道接口 supports/send | 新建 | B |
| `.../callback/alert/channel/CallbackInAppAlertChannel.java` | IN_APP 扇出（承接原 listener） | 新建 | B |
| `.../callback/alert/channel/CallbackEmailAlertChannel.java` | EMAIL 经 JavaMailSender | 新建 | B |
| `.../callback/alert/channel/CallbackSmsAlertChannel.java` | SMS 经 CallbackSmsGateway | 新建 | B |
| `.../callback/alert/sms/CallbackSmsGateway.java` | SMS 网关接口 | 新建 | B |
| `.../callback/alert/sms/CallbackLoggingSmsGateway.java` | log 桩实现（dev/CI 默认） | 新建 | A |
| `.../callback/alert/config/CallbackAlertEmailProperties.java` | SMTP 发件人/from 配置 | 新建 | A |
| `.../callback/alert/config/CallbackAlertSmsProperties.java` | SMS 网关配置 | 新建 | A |
| `.../callback/alert/config/CallbackAlertConfiguration.java` | `@EnableConfigurationProperties` 注册两个 properties（对齐 `CallbackConsumerConfiguration` 范式） | 新建/修改 | A |
| `.../callback/alert/CallbackAlertEvaluator.java` | 统一引擎：订阅事件→读配置→分发 | 新建 | B |
| `.../callback/notification/listener/CallbackNotificationListener.java` | **删除**（逻辑迁入 InAppChannel） | 删除 | B |
| `fep-web/pom.xml` | 加 spring-boot-starter-mail | 修改 | A |
| `CallbackModuleArchTest.java` | R9 alert 不依赖 credential/reaper | 修改 | A |

### 共享工具类清单

| 工具类 | 包 | 关键方法 | 提供者 Task | 消费者 Task |
|---|---|---|---|---|
| `LogSanitizer` | common.util | `sanitize(String)` | 既有 | T5/T6/T7（CRLF 防护 + 告警正文脱敏） |
| `CallbackAlertMessage` | callback.alert | record 构造 | T4 | T5/T6/T7/T8 |
| `CallbackAlertChannel` | callback.alert.channel | `supports(NotifyMethod)` / `send(CallbackAlertMessage)` | T4 | T5/T6/T7/T8 |

### 核心类职责边界声明

#### CallbackAlertEvaluator 职责边界

**负责**: 订阅 `CallbackDeadLetterEvent`；读取 `SysAlertRule` 配置（启用/阈值/频率/渠道集合/收件人）；按 `threshold`+`frequency` 决定是否告警；构造 `CallbackAlertMessage`；遍历启用的 `NotifyMethod` 分发到匹配 `CallbackAlertChannel`；单渠道异常隔离（一个渠道失败不影响其他渠道 + 不影响死信主流程）。
**不负责**: 具体渠道发送实现（→ `CallbackAlertChannel` 实现类）；ADMIN 用户查询（→ `CallbackInAppAlertChannel`）；SysAlertRule 读取细节（→ 复用 `SysAlertRuleService`/`SysAlertRuleRepository`）；HOURLY/DAILY 汇总窗口（→ deferred）。
**依赖上限**: 7 个（ArchUnit 强制）。当前依赖：`SysAlertRuleRepository` + `List<CallbackAlertChannel>`（Spring 注入全部渠道 bean）= 2。
**行数上限**: 300 行。
**如果超出**: 频率汇总逻辑拆 `CallbackAlertFrequencyGate`。

---

## Task 1: V34 迁移 — notify_methods 多值 + alert_phone + 翻默认行 `模式 A`

**PRD 依据:** v1.3 §5.10.7.2d 接口预警管理 + §5.5.3 回调可靠性告警
**追溯 ID:** FR-WEB-SYS-CONF-ALERT + FR-INFRA-CALLBACK-ALERT

**验收标准:**
1. 迁移后 `t_sys_alert_rule` 含 `notify_methods VARCHAR(60) NOT NULL` 列 + `alert_phone VARCHAR(50) NULL` 列；`notify_method` 列已 DROP。
2. 既有默认行 `default_alert_rule_00000000001` 迁移后：`alert_enabled=TRUE`、`notify_methods='IN_APP'`（保留 IN_APP 常开语义）、`alert_phone=NULL`。
3. Flyway `flyway:info` 显示 V34 为最新且 `flyway:validate` 通过（无 checksum 冲突，V34 > V33 无碰撞）。

> **规则**: V34 是新迁移，不违反 F 级硬冻结（仅冻结 V1-V18）。签字前 + 实施前各 grep 实测最大版本号确认无冲突（红线 `feedback_plan_flyway_v_collision_check`）。

**Files:**
- Create: `fep-web/src/main/resources/db/migration/V34__alert_rule_multi_method.sql`

- [ ] **Step 1: 实施前 grep 确认 V34 无冲突**

```bash
ls fep-web/src/main/resources/db/migration/ | grep -oE '^V[0-9]+' | sort -t V -k2 -n | tail -3
```
期望: 最大为 `V33`，V34 可用。

- [ ] **Step 2: 编写迁移脚本**

```sql
-- fep-web/src/main/resources/db/migration/V34__alert_rule_multi_method.sql
-- Phase 2c-A: t_sys_alert_rule 单值 notify_method 升级为多值 notify_methods + 新增 alert_phone（SMS 收件号）。
-- 默认行翻为「启用 + IN_APP」以保留 Phase 2b IN_APP 常开语义（迁移前 IN_APP 由 listener 无条件发，
-- 统一引擎门控后须默认启用 IN_APP 否则行为回归）。参见 docs/plans/2026-06-05-callback-phase2c-a-alert-channels.md。

ALTER TABLE t_sys_alert_rule ADD COLUMN notify_methods VARCHAR(60) NOT NULL DEFAULT 'IN_APP' COMMENT '启用渠道集合，逗号连接 EMAIL/IN_APP/SMS';
ALTER TABLE t_sys_alert_rule ADD COLUMN alert_phone VARCHAR(50) DEFAULT NULL COMMENT 'SMS 告警收件手机号';

-- 回填：既有单值 notify_method 平移到 notify_methods
UPDATE t_sys_alert_rule SET notify_methods = notify_method;

-- 翻默认行：保留 IN_APP 常开（迁移前 IN_APP 无条件发）
UPDATE t_sys_alert_rule SET alert_enabled = TRUE, notify_methods = 'IN_APP'
 WHERE rule_id = 'default_alert_rule_00000000001';

ALTER TABLE t_sys_alert_rule DROP COLUMN notify_method;
```

- [ ] **Step 3: 验证迁移（dev profile，H2）**

```bash
cd /Users/muzhou/FEP_v1.0
./mvnw -pl fep-web flyway:info -Dspring.profiles.active=dev 2>&1 | grep -E 'V34|34 '
```
期望: V34 列出，状态可应用 / 已应用。

- [ ] **Step 4: 提交**

```bash
git add fep-web/src/main/resources/db/migration/V34__alert_rule_multi_method.sql
git commit -m "$(cat <<'EOF'
feat(web): V34 alert_rule multi notify_methods + alert_phone (Phase 2c-A T1)

Upgrade t_sys_alert_rule single notify_method to multi notify_methods +
add alert_phone; flip default row to enabled+IN_APP to preserve Phase 2b
IN_APP always-on semantics under the unified alert engine.

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

## Task 2: NotifyMethodSetConverter + SysAlertRule 多值字段 `模式 A`

**PRD 依据:** v1.3 §5.10.7.2d 接口预警管理
**追溯 ID:** FR-WEB-SYS-CONF-ALERT

**验收标准:**
1. `NotifyMethodSetConverter.convertToDatabaseColumn(Set.of(IN_APP, EMAIL))` → `"IN_APP,EMAIL"`（枚举名升序拼接，保证确定性）。
2. `convertToEntityAttribute("IN_APP,EMAIL")` → `Set` 含且仅含 `IN_APP`、`EMAIL`。
3. `convertToEntityAttribute(null)` 与 `convertToEntityAttribute("")` → 空 `Set`（非 null）。
4. `SysAlertRule.getNotifyMethods()` 返回 `Set<NotifyMethod>`；`getAlertPhone()` 返回 `String`（可 null）；旧 `getNotifyMethod()/setNotifyMethod()` 移除。

**Files:**
- Create: `fep-web/src/main/java/com/puchain/fep/web/sysmgmt/config/alert/domain/NotifyMethodSetConverter.java`
- Create: `fep-web/src/test/java/com/puchain/fep/web/sysmgmt/config/alert/domain/NotifyMethodSetConverterTest.java`
- Modify: `fep-web/src/main/java/com/puchain/fep/web/sysmgmt/config/alert/domain/SysAlertRule.java`

- [ ] **Step 1: 写失败测试**

```java
// NotifyMethodSetConverterTest.java
package com.puchain.fep.web.sysmgmt.config.alert.domain;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.assertj.core.api.Assertions.assertThat;

class NotifyMethodSetConverterTest {

    private final NotifyMethodSetConverter conv = new NotifyMethodSetConverter();

    @Test
    void toDatabaseColumn_shouldJoinSortedByEnumName() {
        assertThat(conv.convertToDatabaseColumn(Set.of(NotifyMethod.IN_APP, NotifyMethod.EMAIL)))
                .isEqualTo("EMAIL,IN_APP");
    }

    @Test
    void toEntityAttribute_shouldParseAllMethods() {
        assertThat(conv.convertToEntityAttribute("IN_APP,EMAIL"))
                .containsExactlyInAnyOrder(NotifyMethod.IN_APP, NotifyMethod.EMAIL);
    }

    @Test
    void toEntityAttribute_shouldReturnEmptySetForNullOrBlank() {
        assertThat(conv.convertToEntityAttribute(null)).isEmpty();
        assertThat(conv.convertToEntityAttribute("")).isEmpty();
    }

    @Test
    void toDatabaseColumn_shouldReturnEmptyStringForEmptySet() {
        assertThat(conv.convertToDatabaseColumn(Set.of())).isEmpty();
    }
}
```

- [ ] **Step 2: 运行确认失败**

```bash
cd /Users/muzhou/FEP_v1.0
./mvnw test -pl fep-web -Dtest=NotifyMethodSetConverterTest -Dsurefire.failIfNoSpecifiedTests=false
```
期望: 编译失败 — `cannot find symbol: class NotifyMethodSetConverter`。

- [ ] **Step 3: 写转换器实现**

```java
// NotifyMethodSetConverter.java
package com.puchain.fep.web.sysmgmt.config.alert.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * {@link NotifyMethod} 集合 ↔ 逗号连接字符串的 JPA 转换器（映射 t_sys_alert_rule.notify_methods）。
 *
 * <p>序列化按枚举名升序拼接保证确定性（避免 HashSet 顺序漂移导致 DB 值不稳定）；
 * 反序列化对 null/空串返回空集合，未知 token 跳过。参见 PRD v1.3 §5.10.7.2d
 * 接口预警管理（FR-WEB-SYS-CONF-ALERT）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Converter
public class NotifyMethodSetConverter implements AttributeConverter<Set<NotifyMethod>, String> {

    private static final String SEP = ",";

    @Override
    public String convertToDatabaseColumn(final Set<NotifyMethod> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "";
        }
        return attribute.stream().map(Enum::name).sorted().collect(Collectors.joining(SEP));
    }

    @Override
    public Set<NotifyMethod> convertToEntityAttribute(final String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return new TreeSet<>(java.util.Comparator.comparing(Enum::name));
        }
        return Arrays.stream(dbData.split(SEP))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(NotifyMethod::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(NotifyMethod.class)));
    }
}
```

- [ ] **Step 4: 改 SysAlertRule（替换 notifyMethod 字段 + 加 alertPhone）**

替换 `SysAlertRule.java` 中 `notifyMethod` 相关字段/getter/setter 为：

```java
    // 替换原 @Enumerated notifyMethod 字段（行 38-40）为：
    @jakarta.persistence.Convert(converter = NotifyMethodSetConverter.class)
    @Column(name = "notify_methods", nullable = false, length = 60)
    private java.util.Set<NotifyMethod> notifyMethods = new java.util.TreeSet<>(java.util.Comparator.comparing(Enum::name));

    @Column(name = "alert_phone", length = 50)
    private String alertPhone;

    /**
     * 获取启用的通知渠道集合。
     *
     * @return 渠道集合（非 null，可能为空）
     */
    public java.util.Set<NotifyMethod> getNotifyMethods() {
        return notifyMethods;
    }

    /**
     * 设置启用的通知渠道集合。
     *
     * @param notifyMethods 渠道集合（非 null）
     */
    public void setNotifyMethods(final java.util.Set<NotifyMethod> notifyMethods) {
        this.notifyMethods = notifyMethods;
    }

    /**
     * 获取 SMS 告警收件手机号。
     *
     * @return 手机号，可能为 null
     */
    public String getAlertPhone() {
        return alertPhone;
    }

    /**
     * 设置 SMS 告警收件手机号。
     *
     * @param alertPhone 手机号（可为 null）
     */
    public void setAlertPhone(final String alertPhone) {
        this.alertPhone = alertPhone;
    }
```

> 同步移除原 `getNotifyMethod()/setNotifyMethod()` 与 `import ...Enumerated/EnumType`（若不再被其他字段使用，`alertFrequency` 仍用 `@Enumerated` 则保留）。

- [ ] **Step 5: 运行确认通过**

```bash
./mvnw test -pl fep-web -Dtest=NotifyMethodSetConverterTest -Dsurefire.failIfNoSpecifiedTests=false
```
期望: `BUILD SUCCESS`, 4 tests passed。

- [ ] **Step 6: 提交**

```bash
git add fep-web/src/main/java/com/puchain/fep/web/sysmgmt/config/alert/domain/
git add fep-web/src/test/java/com/puchain/fep/web/sysmgmt/config/alert/domain/
git commit -m "$(cat <<'EOF'
feat(web): NotifyMethodSet converter + SysAlertRule multi-method (Phase 2c-A T2)

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

## Task 3: AlertRule DTO + Service 同步多值 + alertPhone `模式 A`

**PRD 依据:** v1.3 §5.10.7.2d 接口预警管理
**追溯 ID:** FR-WEB-SYS-CONF-ALERT

**验收标准:**
1. `AlertRuleResponse.from(rule)` 暴露 `notifyMethods: Set<NotifyMethod>` + `alertPhone: String`（移除单值 `notifyMethod`）。
2. `AlertRuleUpdateRequest` 含 `notifyMethods`（`@NotEmpty`）+ `alertPhone`（可空）+ 保留 `alertEmail` `@Email`。
3. `SysAlertRuleService.updateRule(req)` 写入多值集合 + alertPhone；`getRule()` 回读一致。
4. admin 改 `notify_methods=[IN_APP,EMAIL]` + `alertEmail=a@b.com` 后 `getRule()` 返回相同集合。

**Files:**
- Modify: `fep-web/.../config/alert/dto/AlertRuleResponse.java`
- Modify: `fep-web/.../config/alert/dto/AlertRuleUpdateRequest.java`
- Modify: `fep-web/.../config/alert/service/SysAlertRuleService.java`
- Modify: `fep-web/src/test/.../config/alert/controller/SysAlertRuleControllerTest.java`（更新单值→多值断言）

- [ ] **Step 1: 改 AlertRuleResponse**

将 `private NotifyMethod notifyMethod` 字段替换为 `private Set<NotifyMethod> notifyMethods` + `private String alertPhone`，`from(rule)` 工厂改用 `rule.getNotifyMethods()` / `rule.getAlertPhone()`，getter 同步。（保持原 record/class 风格——grep 文件确认是 class 还是 record 后对齐。）

- [ ] **Step 2: 改 AlertRuleUpdateRequest**

```java
    // 替换 notifyMethod 字段为：
    @jakarta.validation.constraints.NotEmpty(message = "通知方式不能为空")
    private java.util.Set<NotifyMethod> notifyMethods;

    @jakarta.validation.constraints.Size(max = 50, message = "手机号长度不能超过 50")
    private String alertPhone;
```
+ 对应 getter/setter + Javadoc。保留 `@Email private String alertEmail`。

- [ ] **Step 3: 改 SysAlertRuleService.updateRule**

将 `updateRule` 中 `rule.setNotifyMethod(request.getNotifyMethod())` 替换为：

```java
        rule.setNotifyMethods(request.getNotifyMethods());
        rule.setAlertPhone(request.getAlertPhone());
```

- [ ] **Step 4: 改测试 + 运行**

更新 `SysAlertRuleControllerTest` 中所有 `notifyMethod` 单值断言为 `notifyMethods` 集合断言（grep 实测原断言行后逐处改）。

```bash
./mvnw test -pl fep-web -Dtest=SysAlertRuleControllerTest -Dsurefire.failIfNoSpecifiedTests=false
```
期望: `BUILD SUCCESS`。

- [ ] **Step 5: 提交**

```bash
git add fep-web/src/main/java/com/puchain/fep/web/sysmgmt/config/alert/dto/
git add fep-web/src/main/java/com/puchain/fep/web/sysmgmt/config/alert/service/SysAlertRuleService.java
git add fep-web/src/test/java/com/puchain/fep/web/sysmgmt/config/alert/controller/SysAlertRuleControllerTest.java
git commit -m "$(cat <<'EOF'
feat(web): alert rule DTO+service multi notify_methods + alertPhone (Phase 2c-A T3)

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

## Task 4: 渠道抽象 — CallbackAlertMessage + CallbackAlertChannel `模式 B`

**PRD 依据:** v1.3 §5.5.3 回调可靠性告警
**追溯 ID:** FR-INFRA-CALLBACK-ALERT

**验收标准:**
1. `CallbackAlertMessage` record 携带 `level/title/body/refId/refType/alertEmail/alertPhone`，工厂 `ofDeadLetter(CallbackDeadLetterEvent, alertEmail, alertPhone)` 组装标题/正文（正文经 `LogSanitizer.sanitize` 防注入）。
2. `CallbackAlertChannel` 接口含 `boolean supports(NotifyMethod)` + `void send(CallbackAlertMessage)`。

**Files:**
- Create: `fep-web/.../callback/alert/CallbackAlertMessage.java`
- Create: `fep-web/.../callback/alert/channel/CallbackAlertChannel.java`
- Create: `fep-web/src/test/.../callback/alert/CallbackAlertMessageTest.java`

- [ ] **Step 1: 写失败测试**

```java
// CallbackAlertMessageTest.java
package com.puchain.fep.web.callback.alert;

import com.puchain.fep.web.callback.dlq.event.CallbackDeadLetterEvent;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.assertThat;

class CallbackAlertMessageTest {

    @Test
    void ofDeadLetter_shouldComposeTitleAndBody() {
        CallbackDeadLetterEvent ev = new CallbackDeadLetterEvent(
                "q1", "IF-001", "9120", 3, "HTTP 500", LocalDateTime.now());
        CallbackAlertMessage msg = CallbackAlertMessage.ofDeadLetter(ev, "a@b.com", "13800000000");
        assertThat(msg.title()).contains("IF-001");
        assertThat(msg.body()).contains("q1").contains("9120").contains("3");
        assertThat(msg.level()).isEqualTo("ERROR");
        assertThat(msg.refId()).isEqualTo("q1");
        assertThat(msg.alertEmail()).isEqualTo("a@b.com");
        assertThat(msg.alertPhone()).isEqualTo("13800000000");
    }
}
```

- [ ] **Step 2: 运行确认失败**

```bash
./mvnw test -pl fep-web -Dtest=CallbackAlertMessageTest -Dsurefire.failIfNoSpecifiedTests=false
```
期望: 编译失败 — `cannot find symbol: class CallbackAlertMessage`。

- [ ] **Step 3: 写 record + 接口**

```java
// CallbackAlertMessage.java
package com.puchain.fep.web.callback.alert;

import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.web.callback.dlq.event.CallbackDeadLetterEvent;

/**
 * 统一告警消息：由 {@code CallbackAlertEvaluator} 从 {@link CallbackDeadLetterEvent} 与
 * {@code SysAlertRule} 收件人配置组装，分发给各 {@code CallbackAlertChannel}。
 *
 * <p>{@code body} 在工厂中经 {@link LogSanitizer#sanitize(String)} 处理，去除 CRLF 注入风险
 * （质量门禁 #4）。参见 PRD v1.3 §5.5.3 回调可靠性告警（FR-INFRA-CALLBACK-ALERT）。</p>
 *
 * @param level      级别（ERROR/WARN/INFO）
 * @param title      标题
 * @param body       正文（已 sanitize）
 * @param refId      关联业务对象 id（死信 queueId）
 * @param refType    关联业务对象类型
 * @param alertEmail EMAIL 渠道收件邮箱（可 null）
 * @param alertPhone SMS 渠道收件手机号（可 null）
 * @author FEP Team
 * @since 1.0.0
 */
public record CallbackAlertMessage(
        String level, String title, String body,
        String refId, String refType, String alertEmail, String alertPhone) {

    private static final String LEVEL_ERROR = "ERROR";
    private static final String REF_TYPE_DLQ = "CALLBACK_DLQ_ENTRY";

    /**
     * 从死信事件组装告警消息。
     *
     * @param ev         死信事件
     * @param alertEmail EMAIL 收件邮箱（可 null）
     * @param alertPhone SMS 收件手机号（可 null）
     * @return 告警消息
     */
    public static CallbackAlertMessage ofDeadLetter(final CallbackDeadLetterEvent ev,
            final String alertEmail, final String alertPhone) {
        final String title = "回调死信 - " + ev.targetInterfaceId();
        final String body = LogSanitizer.sanitize(String.format(
                "queueId=%s msgNo=%s retryCount=%d error=%s",
                ev.queueId(), ev.msgNo(), ev.retryCount(), ev.lastError()));
        return new CallbackAlertMessage(LEVEL_ERROR, title, body,
                ev.queueId(), REF_TYPE_DLQ, alertEmail, alertPhone);
    }
}
```

```java
// CallbackAlertChannel.java
package com.puchain.fep.web.callback.alert.channel;

import com.puchain.fep.web.callback.alert.CallbackAlertMessage;
import com.puchain.fep.web.sysmgmt.config.alert.domain.NotifyMethod;

/**
 * 告警渠道策略接口：每个实现声明所支持的 {@link NotifyMethod}，由
 * {@code CallbackAlertEvaluator} 按配置启用的渠道集合分发。
 *
 * <p>实现：{@code CallbackInAppAlertChannel}（IN_APP）、{@code CallbackEmailAlertChannel}（EMAIL）、
 * {@code CallbackSmsAlertChannel}（SMS）。参见 PRD v1.3 §5.5.3（FR-INFRA-CALLBACK-ALERT）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public interface CallbackAlertChannel {

    /**
     * @param method 通知方式
     * @return 本渠道是否支持该通知方式
     */
    boolean supports(NotifyMethod method);

    /**
     * 发送告警（实现须自隔离异常，不向上抛以免影响其他渠道与死信主流程）。
     *
     * @param message 统一告警消息
     */
    void send(CallbackAlertMessage message);
}
```

- [ ] **Step 4: 运行 + 提交**

```bash
./mvnw test -pl fep-web -Dtest=CallbackAlertMessageTest -Dsurefire.failIfNoSpecifiedTests=false
git add fep-web/src/main/java/com/puchain/fep/web/callback/alert/ fep-web/src/test/java/com/puchain/fep/web/callback/alert/
git commit -m "$(cat <<'EOF'
feat(web): CallbackAlertMessage + CallbackAlertChannel abstraction (Phase 2c-A T4)

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

## Task 5: CallbackInAppAlertChannel — 承接原 listener 扇出逻辑 `模式 B`

**PRD 依据:** v1.3 §5.5.3 + §5.10.7.2d
**追溯 ID:** FR-INFRA-CALLBACK-IN-APP-ALERT

**验收标准:**
1. `supports(IN_APP)` 返回 true，其余 false。
2. `send(msg)`：经 `SysRoleRepository.findByRoleCode("ADMIN")` → `SysUserRoleRepository.findByRoleId` → `SysUserRepository.findAllById` 三步查 ADMIN 用户，为每人落一条 `CallbackNotificationEntity`（category=`CALLBACK_DLQ`, level=msg.level, title=msg.title, message=msg.body, refId=msg.refId, refType=msg.refType）。
3. 无 ADMIN 角色 / 无 ADMIN 用户 → 记 WARN 安全返回，不抛异常（保持原 listener 容错语义）。

> **行为保持验证**: 本 Task 把原 `CallbackNotificationListener.onDeadLetter`（grep 实测 `:80-108`）的扇出逻辑原样迁入渠道，断言值与原 `CallbackNotificationListenerTest` 一致（迁移而非重写）。

**Files:**
- Create: `fep-web/.../callback/alert/channel/CallbackInAppAlertChannel.java`
- Create: `fep-web/src/test/.../callback/alert/channel/CallbackInAppAlertChannelTest.java`

- [ ] **Step 1: 写失败测试**（沿用原 listener 测试断言）

```java
// CallbackInAppAlertChannelTest.java
package com.puchain.fep.web.callback.alert.channel;

import com.puchain.fep.web.callback.alert.CallbackAlertMessage;
import com.puchain.fep.web.callback.notification.domain.CallbackNotificationEntity;
import com.puchain.fep.web.callback.notification.repository.CallbackNotificationRepository;
import com.puchain.fep.web.sysmgmt.config.alert.domain.NotifyMethod;
import com.puchain.fep.web.sysmgmt.rel.domain.SysUserRole;
import com.puchain.fep.web.sysmgmt.rel.repository.SysUserRoleRepository;
import com.puchain.fep.web.sysmgmt.role.domain.SysRole;
import com.puchain.fep.web.sysmgmt.role.repository.SysRoleRepository;
import com.puchain.fep.web.sysmgmt.user.domain.SysUser;
import com.puchain.fep.web.sysmgmt.user.repository.SysUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CallbackInAppAlertChannelTest {

    @Mock SysRoleRepository roleRepo;
    @Mock SysUserRoleRepository userRoleRepo;
    @Mock SysUserRepository userRepo;
    @Mock CallbackNotificationRepository notifRepo;

    private CallbackAlertMessage msg() {
        return new CallbackAlertMessage("ERROR", "回调死信 - IF-1", "queueId=q1 msgNo=9120",
                "q1", "CALLBACK_DLQ_ENTRY", null, null);
    }

    @Test
    void supports_onlyInApp() {
        CallbackInAppAlertChannel ch = new CallbackInAppAlertChannel(roleRepo, userRoleRepo, userRepo, notifRepo);
        assertThat(ch.supports(NotifyMethod.IN_APP)).isTrue();
        assertThat(ch.supports(NotifyMethod.EMAIL)).isFalse();
        assertThat(ch.supports(NotifyMethod.SMS)).isFalse();
    }

    @Test
    void send_shouldPersistOneNotificationPerAdmin() {
        SysRole admin = mock(SysRole.class);
        when(admin.getRoleId()).thenReturn("r-admin");
        when(roleRepo.findByRoleCode("ADMIN")).thenReturn(Optional.of(admin));
        SysUserRole ur = mock(SysUserRole.class);
        when(ur.getUserId()).thenReturn("u1");
        when(userRoleRepo.findByRoleId("r-admin")).thenReturn(List.of(ur));
        SysUser u1 = mock(SysUser.class);
        when(u1.getUserId()).thenReturn("u1");
        when(userRepo.findAllById(List.of("u1"))).thenReturn(List.of(u1));

        new CallbackInAppAlertChannel(roleRepo, userRoleRepo, userRepo, notifRepo).send(msg());

        ArgumentCaptor<CallbackNotificationEntity> cap = ArgumentCaptor.forClass(CallbackNotificationEntity.class);
        verify(notifRepo).save(cap.capture());
        assertThat(cap.getValue().getUserId()).isEqualTo("u1");
        assertThat(cap.getValue().getCategory()).isEqualTo("CALLBACK_DLQ");
        assertThat(cap.getValue().getRefId()).isEqualTo("q1");
    }

    @Test
    void send_shouldWarnSafelyWhenNoAdminRole() {
        when(roleRepo.findByRoleCode("ADMIN")).thenReturn(Optional.empty());
        new CallbackInAppAlertChannel(roleRepo, userRoleRepo, userRepo, notifRepo).send(msg());
        verify(notifRepo, never()).save(any());
    }
}
```

- [ ] **Step 2: 运行确认失败**

```bash
./mvnw test -pl fep-web -Dtest=CallbackInAppAlertChannelTest -Dsurefire.failIfNoSpecifiedTests=false
```
期望: 编译失败 — `cannot find symbol: class CallbackInAppAlertChannel`。

- [ ] **Step 3: 写实现**

```java
// CallbackInAppAlertChannel.java
package com.puchain.fep.web.callback.alert.channel;

import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.web.callback.alert.CallbackAlertMessage;
import com.puchain.fep.web.callback.notification.domain.CallbackNotificationEntity;
import com.puchain.fep.web.callback.notification.repository.CallbackNotificationRepository;
import com.puchain.fep.web.sysmgmt.config.alert.domain.NotifyMethod;
import com.puchain.fep.web.sysmgmt.rel.domain.SysUserRole;
import com.puchain.fep.web.sysmgmt.rel.repository.SysUserRoleRepository;
import com.puchain.fep.web.sysmgmt.role.domain.SysRole;
import com.puchain.fep.web.sysmgmt.role.repository.SysRoleRepository;
import com.puchain.fep.web.sysmgmt.user.domain.SysUser;
import com.puchain.fep.web.sysmgmt.user.repository.SysUserRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * IN_APP 告警渠道：为每个 ADMIN 角色用户落一条站内通知（承接 Phase 2b
 * {@code CallbackNotificationListener} 的扇出逻辑，行为保持）。
 *
 * <p>ADMIN 定位三步查询（{@code SysUserRepository.findByRoleCode} 不存在）：
 * {@link SysRoleRepository#findByRoleCode} → {@link SysUserRoleRepository#findByRoleId} →
 * {@link SysUserRepository#findAllById}。参见 PRD v1.3 §5.5.3 / §5.10.7.2d
 * （FR-INFRA-CALLBACK-IN-APP-ALERT）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
        justification = "refId passed through LogSanitizer.sanitize() prior to LOG.warn")
public class CallbackInAppAlertChannel implements CallbackAlertChannel {

    private static final Logger LOG = LoggerFactory.getLogger(CallbackInAppAlertChannel.class);
    private static final String ADMIN_ROLE_CODE = "ADMIN";
    private static final String CATEGORY_DLQ = "CALLBACK_DLQ";

    private final SysRoleRepository roleRepo;
    private final SysUserRoleRepository userRoleRepo;
    private final SysUserRepository userRepo;
    private final CallbackNotificationRepository notifRepo;

    /**
     * @param roleRepo     角色仓储
     * @param userRoleRepo 用户-角色关联仓储
     * @param userRepo     用户仓储
     * @param notifRepo    站内通知仓储
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring-managed repository singletons stored by reference per container contract")
    public CallbackInAppAlertChannel(final SysRoleRepository roleRepo,
            final SysUserRoleRepository userRoleRepo, final SysUserRepository userRepo,
            final CallbackNotificationRepository notifRepo) {
        this.roleRepo = roleRepo;
        this.userRoleRepo = userRoleRepo;
        this.userRepo = userRepo;
        this.notifRepo = notifRepo;
    }

    @Override
    public boolean supports(final NotifyMethod method) {
        return method == NotifyMethod.IN_APP;
    }

    @Override
    @Transactional
    public void send(final CallbackAlertMessage message) {
        final Optional<SysRole> adminRole = roleRepo.findByRoleCode(ADMIN_ROLE_CODE);
        if (adminRole.isEmpty()) {
            LOG.warn("DLQ alert but ADMIN role not configured, refId={}",
                    LogSanitizer.sanitize(message.refId()));
            return;
        }
        final List<String> adminUserIds = userRoleRepo.findByRoleId(adminRole.get().getRoleId())
                .stream().map(SysUserRole::getUserId).toList();
        if (adminUserIds.isEmpty()) {
            LOG.warn("DLQ alert but no users assigned ADMIN role, refId={}",
                    LogSanitizer.sanitize(message.refId()));
            return;
        }
        final List<SysUser> admins = userRepo.findAllById(adminUserIds);
        for (final SysUser u : admins) {
            notifRepo.save(CallbackNotificationEntity.of(
                    u.getUserId(), CATEGORY_DLQ, message.level(),
                    message.title(), message.body(), message.refId(), message.refType()));
        }
    }
}
```

- [ ] **Step 4: 运行确认通过 + spotbugs:check**

```bash
./mvnw -pl fep-web -am compile
./mvnw test -pl fep-web -Dtest=CallbackInAppAlertChannelTest -Dsurefire.failIfNoSpecifiedTests=false
./mvnw spotbugs:check -pl fep-web
```
期望: 测试通过；SpotBugs `BugInstance size is 0`（EI_EXPOSE_REP2 已注解 constructor-level）。

> **规则**: 改注解后须先 `compile -am` 再 `spotbugs:check`（红线 `feedback_spotbugs_check_needs_recompile_after_annotation`）；`EI_EXPOSE_REP2` 抑制注解打**构造器**非 class-level。

- [ ] **Step 5: 提交**

```bash
git add fep-web/src/main/java/com/puchain/fep/web/callback/alert/channel/CallbackInAppAlertChannel.java
git add fep-web/src/test/java/com/puchain/fep/web/callback/alert/channel/CallbackInAppAlertChannelTest.java
git commit -m "$(cat <<'EOF'
feat(web): CallbackInAppAlertChannel (migrate listener fanout) (Phase 2c-A T5)

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

## Task 6: CallbackEmailAlertChannel — SMTP 真实发送 `模式 B`

**PRD 依据:** v1.3 §5.5.3 + §5.10.7.2d（EMAIL 通知方式）
**追溯 ID:** FR-INFRA-CALLBACK-ALERT

**验收标准:**
1. `supports(EMAIL)` 返回 true，其余 false。
2. `send(msg)`：`alertEmail` 非空时构造 `SimpleMailMessage`（from=配置 from，to=alertEmail，subject=msg.title，text=msg.body）调 `JavaMailSender.send`；`alertEmail` 为 null/blank → 记 WARN 安全返回不发。
3. `JavaMailSender.send` 抛 `MailException` → catch 记 ERROR 不上抛（渠道异常隔离）。

**Files:**
- Modify: `fep-web/pom.xml`（加 `spring-boot-starter-mail`）
- Create: `fep-web/.../callback/alert/config/CallbackAlertEmailProperties.java`
- Create: `fep-web/.../callback/alert/channel/CallbackEmailAlertChannel.java`
- Create: `fep-web/src/test/.../callback/alert/channel/CallbackEmailAlertChannelTest.java`

- [ ] **Step 1: 加 Maven 依赖**

```xml
<!-- fep-web/pom.xml — dependencies 段 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```
> Spring Boot BOM 管理版本，不写 `<version>`（质量门禁 #6 无硬编码）。

- [ ] **Step 2: 配置属性**

```java
// CallbackAlertEmailProperties.java
package com.puchain.fep.web.callback.alert.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 回调告警 Email 渠道配置（发件人 from 地址；SMTP host/port/username/password 走标准
 * {@code spring.mail.*} 由 {@code JavaMailSender} 自动装配，密钥经 Nacos/环境变量注入，禁硬编码）。
 *
 * <p>参见 PRD v1.3 §5.5.3 回调可靠性告警（FR-INFRA-CALLBACK-ALERT）。</p>
 *
 * @param from 发件人地址（如 fep-alert@example.com）
 * @author FEP Team
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "fep.callback.alert.email")
public record CallbackAlertEmailProperties(String from) {
}
```

- [ ] **Step 3: 写失败测试**

```java
// CallbackEmailAlertChannelTest.java
package com.puchain.fep.web.callback.alert.channel;

import com.puchain.fep.web.callback.alert.CallbackAlertMessage;
import com.puchain.fep.web.callback.alert.config.CallbackAlertEmailProperties;
import com.puchain.fep.web.sysmgmt.config.alert.domain.NotifyMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CallbackEmailAlertChannelTest {

    @Mock JavaMailSender mailSender;

    private CallbackEmailAlertChannel channel() {
        return new CallbackEmailAlertChannel(mailSender,
                new CallbackAlertEmailProperties("fep-alert@example.com"));
    }

    private CallbackAlertMessage msg(String email) {
        return new CallbackAlertMessage("ERROR", "回调死信 - IF-1", "queueId=q1",
                "q1", "CALLBACK_DLQ_ENTRY", email, null);
    }

    @Test
    void supports_onlyEmail() {
        assertThat(channel().supports(NotifyMethod.EMAIL)).isTrue();
        assertThat(channel().supports(NotifyMethod.IN_APP)).isFalse();
    }

    @Test
    void send_shouldDispatchSimpleMailMessage() {
        channel().send(msg("ops@bank.com"));
        ArgumentCaptor<SimpleMailMessage> cap = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(cap.capture());
        assertThat(cap.getValue().getTo()).containsExactly("ops@bank.com");
        assertThat(cap.getValue().getFrom()).isEqualTo("fep-alert@example.com");
        assertThat(cap.getValue().getSubject()).contains("IF-1");
    }

    @Test
    void send_shouldSkipWhenNoRecipient() {
        channel().send(msg(null));
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void send_shouldIsolateMailException() {
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));
        assertThatCode(() -> channel().send(msg("ops@bank.com"))).doesNotThrowAnyException();
    }
}
```

- [ ] **Step 4: 运行确认失败**

```bash
./mvnw test -pl fep-web -Dtest=CallbackEmailAlertChannelTest -Dsurefire.failIfNoSpecifiedTests=false
```
期望: 编译失败 — `cannot find symbol: class CallbackEmailAlertChannel`。

- [ ] **Step 5: 写实现**

```java
// CallbackEmailAlertChannel.java
package com.puchain.fep.web.callback.alert.channel;

import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.web.callback.alert.CallbackAlertMessage;
import com.puchain.fep.web.callback.alert.config.CallbackAlertEmailProperties;
import com.puchain.fep.web.sysmgmt.config.alert.domain.NotifyMethod;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * EMAIL 告警渠道：经 {@link JavaMailSender} 向 {@code SysAlertRule.alertEmail} 发送纯文本告警。
 *
 * <p>SMTP 连接参数走标准 {@code spring.mail.*}（密钥 Nacos/环境变量注入，禁硬编码）；发件人 from
 * 由 {@link CallbackAlertEmailProperties} 提供。{@code MailException} 自隔离不上抛（渠道异常不
 * 影响其他渠道与死信主流程）。参见 PRD v1.3 §5.5.3（FR-INFRA-CALLBACK-ALERT）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
        justification = "recipient passed through LogSanitizer.sanitize() prior to LOG")
public class CallbackEmailAlertChannel implements CallbackAlertChannel {

    private static final Logger LOG = LoggerFactory.getLogger(CallbackEmailAlertChannel.class);

    private final JavaMailSender mailSender;
    private final CallbackAlertEmailProperties props;

    /**
     * @param mailSender Spring 邮件发送器
     * @param props      Email 渠道配置（发件人 from）
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring-managed singletons stored by reference per container contract")
    public CallbackEmailAlertChannel(final JavaMailSender mailSender,
            final CallbackAlertEmailProperties props) {
        this.mailSender = mailSender;
        this.props = props;
    }

    @Override
    public boolean supports(final NotifyMethod method) {
        return method == NotifyMethod.EMAIL;
    }

    @Override
    public void send(final CallbackAlertMessage message) {
        final String to = message.alertEmail();
        if (to == null || to.isBlank()) {
            LOG.warn("EMAIL alert configured but alertEmail is blank, refId={}",
                    LogSanitizer.sanitize(message.refId()));
            return;
        }
        final SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(props.from());
        mail.setTo(to);
        mail.setSubject(message.title());
        mail.setText(message.body());
        try {
            mailSender.send(mail);
            LOG.info("DLQ alert email sent, refId={}", LogSanitizer.sanitize(message.refId()));
        } catch (final MailException ex) {
            LOG.error("DLQ alert email failed, refId={}", LogSanitizer.sanitize(message.refId()), ex);
        }
    }
}
```

- [ ] **Step 6: 注册配置属性（@EnableConfigurationProperties，对齐既有范式）**

> **评审 BLOCKER-1 修复**: 仓库 `@ConfigurationProperties` record 注册范式 = `@EnableConfigurationProperties(Xxx.class)` 在 `@Configuration` 类（实测 `CallbackConsumerConfiguration.java:18` 注册 `CallbackQueueProperties`），**非** `@ConfigurationPropertiesScan`，**也非**在 record 上加 `@Component`（record-based `@ConfigurationProperties` 加 `@Component` 不激活属性绑定，会启动报 `No qualifying bean`）。本 Step 新建 `CallbackAlertConfiguration` 注册 Email properties；T7 Step 3 追加 Sms properties。

```java
// CallbackAlertConfiguration.java
package com.puchain.fep.web.callback.alert.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 回调告警渠道配置属性注册（对齐 {@code CallbackConsumerConfiguration} 的
 * {@code @EnableConfigurationProperties} 范式）。T7 追加 {@link CallbackAlertSmsProperties}。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Configuration
@EnableConfigurationProperties(CallbackAlertEmailProperties.class)
public class CallbackAlertConfiguration {
}
```

- [ ] **Step 7: 运行 + spotbugs**

```bash
./mvnw -pl fep-web -am compile
./mvnw test -pl fep-web -Dtest=CallbackEmailAlertChannelTest -Dsurefire.failIfNoSpecifiedTests=false
./mvnw spotbugs:check -pl fep-web
```
期望: 测试通过；`BugInstance size is 0`。

- [ ] **Step 8: 提交**

```bash
git add fep-web/pom.xml fep-web/src/main/java/com/puchain/fep/web/callback/alert/ fep-web/src/test/java/com/puchain/fep/web/callback/alert/channel/CallbackEmailAlertChannelTest.java
git commit -m "$(cat <<'EOF'
feat(web): CallbackEmailAlertChannel via JavaMailSender (Phase 2c-A T6)

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

## Task 7: CallbackSmsAlertChannel + CallbackSmsGateway 抽象 + log 桩 `模式 B`

**PRD 依据:** v1.3 §5.5.3 + §5.10.7.2d（SMS 通知方式）
**追溯 ID:** FR-INFRA-CALLBACK-ALERT

**验收标准:**
1. `CallbackSmsGateway` 接口：`void send(String phone, String content)`。
2. `CallbackLoggingSmsGateway`（dev/CI 默认 bean）：`send` 仅 `LOG.info` 脱敏后的手机号 + 内容摘要，不发真实短信。
3. `CallbackSmsAlertChannel.supports(SMS)` true；`send(msg)`：`alertPhone` 非空 → `gateway.send(phone, body)`；为空 → WARN 安全返回。
4. 真实网关（阿里云/腾讯云 SDK）**显式 deferred** 到独立 Plan，本 Task 仅接口 + log 桩。

**Files:**
- Create: `fep-web/.../callback/alert/sms/CallbackSmsGateway.java`
- Create: `fep-web/.../callback/alert/sms/CallbackLoggingSmsGateway.java`
- Create: `fep-web/.../callback/alert/config/CallbackAlertSmsProperties.java`
- Create: `fep-web/.../callback/alert/channel/CallbackSmsAlertChannel.java`
- Create: `fep-web/src/test/.../callback/alert/channel/CallbackSmsAlertChannelTest.java`

- [ ] **Step 1: 写失败测试**

```java
// CallbackSmsAlertChannelTest.java
package com.puchain.fep.web.callback.alert.channel;

import com.puchain.fep.web.callback.alert.CallbackAlertMessage;
import com.puchain.fep.web.callback.alert.sms.CallbackSmsGateway;
import com.puchain.fep.web.sysmgmt.config.alert.domain.NotifyMethod;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CallbackSmsAlertChannelTest {

    @Mock CallbackSmsGateway gateway;

    private CallbackAlertMessage msg(String phone) {
        return new CallbackAlertMessage("ERROR", "回调死信 - IF-1", "queueId=q1",
                "q1", "CALLBACK_DLQ_ENTRY", null, phone);
    }

    @Test
    void supports_onlySms() {
        CallbackSmsAlertChannel ch = new CallbackSmsAlertChannel(gateway);
        assertThat(ch.supports(NotifyMethod.SMS)).isTrue();
        assertThat(ch.supports(NotifyMethod.EMAIL)).isFalse();
    }

    @Test
    void send_shouldDelegateToGateway() {
        new CallbackSmsAlertChannel(gateway).send(msg("13800000000"));
        verify(gateway).send(eq("13800000000"), any());
    }

    @Test
    void send_shouldSkipWhenNoPhone() {
        new CallbackSmsAlertChannel(gateway).send(msg(null));
        verify(gateway, never()).send(any(), any());
    }
}
```

- [ ] **Step 2: 运行确认失败**

```bash
./mvnw test -pl fep-web -Dtest=CallbackSmsAlertChannelTest -Dsurefire.failIfNoSpecifiedTests=false
```
期望: 编译失败 — `cannot find symbol: class CallbackSmsAlertChannel`。

- [ ] **Step 3: 写接口 + 桩 + 配置 + 渠道**

```java
// CallbackSmsGateway.java
package com.puchain.fep.web.callback.alert.sms;

/**
 * SMS 网关抽象：屏蔽具体短信服务商（阿里云/腾讯云/自建）。本 Plan 仅提供
 * {@code CallbackLoggingSmsGateway} log 桩；真实网关 SDK 接入由独立 Plan 完成（⛔ 网关密钥
 * 走配置注入禁硬编码）。参见 PRD v1.3 §5.5.3（FR-INFRA-CALLBACK-ALERT）。
 *
 * <p><b>bean 名约定</b>：log 桩 {@code CallbackLoggingSmsGateway} 标
 * {@code @ConditionalOnMissingBean(name = "realSmsGateway")}。将来真实网关实现**必须**以 bean 名
 * {@code realSmsGateway} 注册（{@code @Bean CallbackSmsGateway realSmsGateway()} 或
 * {@code @Component("realSmsGateway")}），否则与 log 桩冲突（NoUniqueBeanDefinition）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
public interface CallbackSmsGateway {

    /**
     * 发送短信。
     *
     * @param phone   收件手机号
     * @param content 短信内容
     */
    void send(String phone, String content);
}
```

```java
// CallbackLoggingSmsGateway.java
package com.puchain.fep.web.callback.alert.sms;

import com.puchain.fep.common.util.LogSanitizer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * SMS 网关 log 桩（dev/CI 默认）：不发真实短信，仅记录脱敏后的发送意图。真实网关 bean 引入时
 * 经 {@link ConditionalOnMissingBean} 自动让位。手机号经 {@link LogSanitizer} 脱敏（质量门禁 #4）。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
@ConditionalOnMissingBean(name = "realSmsGateway")
@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
        justification = "phone passed through LogSanitizer.sanitize() prior to LOG")
public class CallbackLoggingSmsGateway implements CallbackSmsGateway {

    private static final Logger LOG = LoggerFactory.getLogger(CallbackLoggingSmsGateway.class);

    @Override
    public void send(final String phone, final String content) {
        LOG.info("[SMS-STUB] would send to phone={} contentLen={}",
                LogSanitizer.sanitize(phone), content == null ? 0 : content.length());
    }
}
```

```java
// CallbackAlertSmsProperties.java
package com.puchain.fep.web.callback.alert.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 回调告警 SMS 渠道配置（真实网关 URL / accessKey；secret 走 Nacos/环境变量，禁硬编码）。
 * 本 Plan log 桩不消费这些字段，预留供真实网关 Plan 使用。
 * 参见 PRD v1.3 §5.5.3（FR-INFRA-CALLBACK-ALERT）。
 *
 * @param gatewayUrl 网关地址
 * @param accessKey  访问 key
 * @author FEP Team
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "fep.callback.alert.sms")
public record CallbackAlertSmsProperties(String gatewayUrl, String accessKey) {
}
```

```java
// CallbackSmsAlertChannel.java
package com.puchain.fep.web.callback.alert.channel;

import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.web.callback.alert.CallbackAlertMessage;
import com.puchain.fep.web.callback.alert.sms.CallbackSmsGateway;
import com.puchain.fep.web.sysmgmt.config.alert.domain.NotifyMethod;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * SMS 告警渠道：委派 {@link CallbackSmsGateway} 向 {@code SysAlertRule.alertPhone} 发短信。
 * 网关异常由网关实现自隔离；本渠道仅做收件人空值守卫。参见 PRD v1.3 §5.5.3
 * （FR-INFRA-CALLBACK-ALERT）。
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
        justification = "phone passed through LogSanitizer.sanitize() prior to LOG")
public class CallbackSmsAlertChannel implements CallbackAlertChannel {

    private static final Logger LOG = LoggerFactory.getLogger(CallbackSmsAlertChannel.class);

    private final CallbackSmsGateway gateway;

    /**
     * @param gateway SMS 网关（dev/CI 为 log 桩）
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring-managed singleton stored by reference per container contract")
    public CallbackSmsAlertChannel(final CallbackSmsGateway gateway) {
        this.gateway = gateway;
    }

    @Override
    public boolean supports(final NotifyMethod method) {
        return method == NotifyMethod.SMS;
    }

    @Override
    public void send(final CallbackAlertMessage message) {
        final String phone = message.alertPhone();
        if (phone == null || phone.isBlank()) {
            LOG.warn("SMS alert configured but alertPhone is blank, refId={}",
                    LogSanitizer.sanitize(message.refId()));
            return;
        }
        gateway.send(phone, message.body());
    }
}
```

- [ ] **Step 4: 在 CallbackAlertConfiguration 追加 Sms properties 注册（评审 IMPORTANT-1 修复）**

修改 T6 创建的 `CallbackAlertConfiguration.java`，把 `@EnableConfigurationProperties` 扩为两项（防 `CallbackAlertSmsProperties` 下版接真网关时才暴露未注册）：

```java
@Configuration
@EnableConfigurationProperties({CallbackAlertEmailProperties.class, CallbackAlertSmsProperties.class})
public class CallbackAlertConfiguration {
}
```

- [ ] **Step 5: 运行 + spotbugs**

```bash
./mvnw -pl fep-web -am compile
./mvnw test -pl fep-web -Dtest=CallbackSmsAlertChannelTest -Dsurefire.failIfNoSpecifiedTests=false
./mvnw spotbugs:check -pl fep-web
```
期望: 测试通过；`BugInstance size is 0`。

- [ ] **Step 6: 提交**

```bash
git add fep-web/src/main/java/com/puchain/fep/web/callback/alert/sms/ fep-web/src/main/java/com/puchain/fep/web/callback/alert/config/ fep-web/src/main/java/com/puchain/fep/web/callback/alert/channel/CallbackSmsAlertChannel.java fep-web/src/test/java/com/puchain/fep/web/callback/alert/channel/CallbackSmsAlertChannelTest.java
git commit -m "$(cat <<'EOF'
feat(web): CallbackSmsAlertChannel + SmsGateway abstraction + log stub (Phase 2c-A T7)

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

## Task 8: CallbackAlertEvaluator — 统一引擎 + 删除旧 listener `模式 B`

**PRD 依据:** v1.3 §5.5.3 回调可靠性告警
**追溯 ID:** FR-INFRA-CALLBACK-ALERT

**验收标准:**
1. `@EventListener onDeadLetter(ev)`：读 `SysAlertRule`（`SysAlertRuleService.getRule()` 或 repository）；`alertEnabled=false` → 不告警直接返回。
2. `retryCount < threshold` → 不告警（threshold 过滤；默认 threshold=0 → 全告警）。
3. `alertEnabled=true` 且通过阈值 → 构造 `CallbackAlertMessage.ofDeadLetter(ev, rule.alertEmail, rule.alertPhone)`，遍历 `rule.getNotifyMethods()`，对每个 method 找 `supports(method)` 为 true 的渠道并 `send`。
4. 单渠道 `send` 抛异常 → catch 记 ERROR 继续下一渠道（异常隔离），不影响死信主流程。
5. 频率非 REALTIME → 按 REALTIME 立即分发 + `LOG.debug` 标注（HOURLY/DAILY 汇总 deferred，非 silent skip）。
6. 旧 `CallbackNotificationListener` + 其测试删除；端到端 IT（`CallbackPhase2bEndToEndIT`）仍 GREEN（默认行已翻 enabled+IN_APP）。

**Files:**
- Create: `fep-web/.../callback/alert/CallbackAlertEvaluator.java`
- Create: `fep-web/src/test/.../callback/alert/CallbackAlertEvaluatorTest.java`
- Delete: `fep-web/.../callback/notification/listener/CallbackNotificationListener.java`
- Delete: `fep-web/src/test/.../callback/notification/listener/CallbackNotificationListenerTest.java`

- [ ] **Step 1: 写失败测试**

```java
// CallbackAlertEvaluatorTest.java
package com.puchain.fep.web.callback.alert;

import com.puchain.fep.web.callback.alert.channel.CallbackAlertChannel;
import com.puchain.fep.web.callback.dlq.event.CallbackDeadLetterEvent;
import com.puchain.fep.web.sysmgmt.config.alert.domain.NotifyMethod;
import com.puchain.fep.web.sysmgmt.config.alert.domain.SysAlertRule;
import com.puchain.fep.web.sysmgmt.config.alert.repository.SysAlertRuleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CallbackAlertEvaluatorTest {

    @Mock SysAlertRuleRepository ruleRepo;
    @Mock CallbackAlertChannel inApp;
    @Mock CallbackAlertChannel email;

    private CallbackDeadLetterEvent ev(int retryCount) {
        return new CallbackDeadLetterEvent("q1", "IF-1", "9120", retryCount, "HTTP 500", LocalDateTime.now());
    }

    private SysAlertRule rule(boolean enabled, int threshold, Set<NotifyMethod> methods) {
        SysAlertRule r = new SysAlertRule();
        r.setAlertEnabled(enabled);
        r.setThreshold(threshold);
        r.setNotifyMethods(methods);
        r.setAlertEmail("ops@bank.com");
        r.setAlertFrequency(com.puchain.fep.web.sysmgmt.config.alert.domain.AlertFrequency.REALTIME);
        return r;
    }

    private CallbackAlertEvaluator evaluator() {
        when(inApp.supports(NotifyMethod.IN_APP)).thenReturn(true);
        lenient().when(inApp.supports(NotifyMethod.EMAIL)).thenReturn(false);
        lenient().when(email.supports(NotifyMethod.EMAIL)).thenReturn(true);
        lenient().when(email.supports(NotifyMethod.IN_APP)).thenReturn(false);
        return new CallbackAlertEvaluator(ruleRepo, List.of(inApp, email));
    }

    @Test
    void shouldDispatchToEnabledChannels() {
        when(ruleRepo.findAll()).thenReturn(List.of(
                rule(true, 0, new TreeSet<>(Set.of(NotifyMethod.IN_APP, NotifyMethod.EMAIL)))));
        evaluator().onDeadLetter(ev(3));
        verify(inApp).send(any());
        verify(email).send(any());
    }

    @Test
    void shouldSkipWhenDisabled() {
        when(ruleRepo.findAll()).thenReturn(List.of(
                rule(false, 0, new TreeSet<>(Set.of(NotifyMethod.IN_APP)))));
        evaluator().onDeadLetter(ev(3));
        verify(inApp, never()).send(any());
    }

    @Test
    void shouldSkipBelowThreshold() {
        when(ruleRepo.findAll()).thenReturn(List.of(
                rule(true, 5, new TreeSet<>(Set.of(NotifyMethod.IN_APP)))));
        evaluator().onDeadLetter(ev(3));
        verify(inApp, never()).send(any());
    }

    @Test
    void shouldIsolateChannelException() {
        when(ruleRepo.findAll()).thenReturn(List.of(
                rule(true, 0, new TreeSet<>(Set.of(NotifyMethod.IN_APP, NotifyMethod.EMAIL)))));
        doThrow(new RuntimeException("boom")).when(inApp).send(any());
        CallbackAlertEvaluator ev = evaluator();
        assertThatCode(() -> ev.onDeadLetter(ev(3))).doesNotThrowAnyException();
        verify(email).send(any());
    }
}
```

- [ ] **Step 2: 运行确认失败**

```bash
./mvnw test -pl fep-web -Dtest=CallbackAlertEvaluatorTest -Dsurefire.failIfNoSpecifiedTests=false
```
期望: 编译失败 — `cannot find symbol: class CallbackAlertEvaluator`。

- [ ] **Step 3: 写实现**

```java
// CallbackAlertEvaluator.java
package com.puchain.fep.web.callback.alert;

import com.puchain.fep.common.util.LogSanitizer;
import com.puchain.fep.web.callback.alert.channel.CallbackAlertChannel;
import com.puchain.fep.web.callback.dlq.event.CallbackDeadLetterEvent;
import com.puchain.fep.web.sysmgmt.config.alert.domain.AlertFrequency;
import com.puchain.fep.web.sysmgmt.config.alert.domain.NotifyMethod;
import com.puchain.fep.web.sysmgmt.config.alert.domain.SysAlertRule;
import com.puchain.fep.web.sysmgmt.config.alert.repository.SysAlertRuleRepository;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 统一告警引擎：单一 {@link EventListener} 订阅 {@link CallbackDeadLetterEvent}，按
 * {@code t_sys_alert_rule} 配置（启用/阈值/频率/渠道集合/收件人）分发到各 {@link CallbackAlertChannel}。
 *
 * <p>替代 Phase 2b {@code CallbackNotificationListener}（IN_APP 硬编码），将 IN_APP 纳入配置门控。
 * 单渠道异常隔离；频率 HOURLY/DAILY 汇总窗口 deferred（当前按 REALTIME 立即分发）。
 * 参见 PRD v1.3 §5.5.3 回调可靠性告警（FR-INFRA-CALLBACK-ALERT）。</p>
 *
 * @author FEP Team
 * @since 1.0.0
 */
@Component
@SuppressFBWarnings(value = "CRLF_INJECTION_LOGS",
        justification = "refId passed through LogSanitizer.sanitize() prior to LOG")
public class CallbackAlertEvaluator {

    private static final Logger LOG = LoggerFactory.getLogger(CallbackAlertEvaluator.class);

    private final SysAlertRuleRepository ruleRepo;
    private final List<CallbackAlertChannel> channels;

    /**
     * @param ruleRepo 预警规则仓储（单条全局配置）
     * @param channels 全部告警渠道 bean（Spring 注入）
     */
    @SuppressFBWarnings(value = "EI_EXPOSE_REP2",
            justification = "Spring-managed singletons stored by reference per container contract")
    public CallbackAlertEvaluator(final SysAlertRuleRepository ruleRepo,
            final List<CallbackAlertChannel> channels) {
        this.ruleRepo = ruleRepo;
        this.channels = channels;
    }

    /**
     * 处理死信事件：按配置分发告警。无配置 / 未启用 / 未达阈值 → 安全返回不告警。
     *
     * @param ev 死信事件
     */
    @EventListener
    public void onDeadLetter(final CallbackDeadLetterEvent ev) {
        final List<SysAlertRule> rules = ruleRepo.findAll();
        if (rules.isEmpty()) {
            LOG.warn("DLQ event but no alert rule configured, queueId={}",
                    LogSanitizer.sanitize(ev.queueId()));
            return;
        }
        final SysAlertRule rule = rules.get(0);
        if (!Boolean.TRUE.equals(rule.getAlertEnabled())) {
            return;
        }
        final int threshold = rule.getThreshold() == null ? 0 : rule.getThreshold();
        if (ev.retryCount() < threshold) {
            return;
        }
        if (rule.getAlertFrequency() != AlertFrequency.REALTIME) {
            LOG.debug("alert frequency {} aggregation deferred; dispatching as REALTIME, queueId={}",
                    rule.getAlertFrequency(), LogSanitizer.sanitize(ev.queueId()));
        }
        final CallbackAlertMessage msg = CallbackAlertMessage.ofDeadLetter(
                ev, rule.getAlertEmail(), rule.getAlertPhone());
        for (final NotifyMethod method : rule.getNotifyMethods()) {
            for (final CallbackAlertChannel ch : channels) {
                if (ch.supports(method)) {
                    dispatchIsolated(ch, msg, method, ev.queueId());
                }
            }
        }
    }

    private void dispatchIsolated(final CallbackAlertChannel ch, final CallbackAlertMessage msg,
            final NotifyMethod method, final String queueId) {
        try {
            ch.send(msg);
        } catch (final RuntimeException ex) {
            LOG.error("alert channel {} failed, queueId={}", method,
                    LogSanitizer.sanitize(queueId), ex);
        }
    }
}
```

- [ ] **Step 4: 删除旧 listener + 测试**

```bash
git rm fep-web/src/main/java/com/puchain/fep/web/callback/notification/listener/CallbackNotificationListener.java
git rm fep-web/src/test/java/com/puchain/fep/web/callback/notification/listener/CallbackNotificationListenerTest.java
```

- [ ] **Step 5: 运行单测 + spotbugs + 全 fep-web verify（含 ArchUnit + E2E IT）**

```bash
./mvnw -pl fep-web -am compile
./mvnw test -pl fep-web -Dtest=CallbackAlertEvaluatorTest -Dsurefire.failIfNoSpecifiedTests=false
./mvnw spotbugs:check -pl fep-web
./mvnw verify -pl fep-web -am
```
期望: 单测通过；`BugInstance size is 0`；全 fep-web verify GREEN（含 `CallbackPhase2bEndToEndIT` IN_APP 断言仍 PASS — 默认行 enabled+IN_APP；ArchUnit R1-R8 PASS）。

> **规则**: 新增/删除 listener 类 commit 前须跑全 fep-web verify（红线 `feedback_full_regression_before_commit` + `feedback_subagent_must_run_spotbugs_check`）。本机超载（load >50）时以 GHA CI 为权威门禁（红线 `feedback_harness_bg_detach_hybrid_default`：长跑 mvn 主对话前台跑等结果）。

- [ ] **Step 6: 提交**

```bash
git add fep-web/src/main/java/com/puchain/fep/web/callback/alert/CallbackAlertEvaluator.java fep-web/src/test/java/com/puchain/fep/web/callback/alert/CallbackAlertEvaluatorTest.java
git commit -m "$(cat <<'EOF'
feat(web): CallbackAlertEvaluator unified engine, retire IN_APP listener (Phase 2c-A T8)

Single @EventListener reads t_sys_alert_rule and fans out DLQ alerts to
IN_APP/EMAIL/SMS channels with per-channel exception isolation. Replaces
the hardcoded CallbackNotificationListener (IN_APP migrated to channel).

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

---

## Task 9: ArchUnit R9 + 全量回归 + CLAUDE.md 状态更新 `模式 A`

**PRD 依据:** 基础设施治理（ArchUnit 架构守护）
**追溯 ID:** N/A（架构约束 Task）

**验收标准:**
1. ArchUnit 新增 R9：`callback.alert..` 不依赖 `callback.credential..` / `callback.reaper..`（镜像 R6 解耦）。
2. 全 fep-web `verify` GREEN（含全部新测试 + R1-R9 + E2E IT）。
3. CLAUDE.md「当前项目状态」段更新：Callback Phase 2c-A 告警引擎 ship 状态 + Flyway V34。

**Files:**
- Modify: `fep-web/src/test/.../callback/CallbackModuleArchTest.java`
- Modify: `/Users/muzhou/FEP/CLAUDE.md`（仅 file write，非 git，红线 `feedback_fep_docs_repo_commit_taboo`）

- [ ] **Step 1: 加 R9 ArchUnit 规则**

```java
    /**
     * R9: {@code callback.alert}（统一告警引擎 + 渠道）不依赖 {@code callback.credential}
     * 或 {@code callback.reaper}。
     *
     * <p>告警子系统经事件解耦（{@code @EventListener CallbackDeadLetterEvent}），与凭证、reaper
     * 子系统功能正交，不得横向耦合（镜像 R6）。</p>
     */
    @ArchTest
    static final ArchRule R9_alert_must_not_depend_on_credential_or_reaper =
            noClasses().that().resideInAPackage("com.puchain.fep.web.callback.alert..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("com.puchain.fep.web.callback.credential..",
                            "com.puchain.fep.web.callback.reaper..");
```

- [ ] **Step 2: 全量回归**

```bash
./mvnw verify -pl fep-web -am
```
期望: GREEN — 全部新单测 + ArchUnit R1-R9 PASS + `CallbackPhase2bEndToEndIT` PASS。
> 回归验收两层（红线 `feedback_plan_regression_scope_explicit`）：
> - **Strong**: 全 fep-web `verify` BUILD SUCCESS（含 IT + ArchUnit + spotbugs）。
> - **Minimum**（本机超载 fallback）: `CallbackAlertEvaluatorTest` + `CallbackInAppAlertChannelTest` + `CallbackEmailAlertChannelTest` + `CallbackSmsAlertChannelTest` + `NotifyMethodSetConverterTest` 单测 GREEN + `spotbugs:check` BugInstance 0 + GHA CI 全绿背书。

- [ ] **Step 3: 提交 ArchUnit**

```bash
git add fep-web/src/test/java/com/puchain/fep/web/callback/CallbackModuleArchTest.java
git commit -m "$(cat <<'EOF'
test(web): ArchUnit R9 alert package decoupling (Phase 2c-A T9)

AI-Generated: claude-code
Reviewed-By: pending
EOF
)"
```

- [ ] **Step 4: 更新 CLAUDE.md（file write only，不 git add）**

更新 `/Users/muzhou/FEP/CLAUDE.md`「当前项目状态」快照：Callback Phase 2c-A 统一告警引擎 + Email/SMS（log 桩）ship；Flyway V33→V34；告警渠道 3（IN_APP 迁入引擎 + EMAIL SMTP + SMS 桩）。

---

## 自检清单（起草完成时核对）

1. **PRD 覆盖度**: FR-INFRA-CALLBACK-ALERT（告警分发）+ FR-WEB-SYS-CONF-ALERT（规则配置多值）+ FR-INFRA-CALLBACK-IN-APP-ALERT（IN_APP 保持）全有 Task 覆盖。EMAIL/SMS 真实网关 SDK = **不在本 Plan**（SMS 桩 + 真网关 deferred 独立 Plan，明示于 T7）。
2. **安全边界**: 无 SM2/SM3/SM4/密钥派生/security.impl。SMTP/SMS 密钥走配置注入（T6/T7 显式）。告警正文经 LogSanitizer（T4 工厂 + 各渠道日志）。✅
3. **占位符扫描**: 无 TBD/TODO/「类似 Task N」。HOURLY/DAILY 频率 = 完整保守降级代码 + 文档化 deferred（非占位）。✅
4. **类型一致性**: `CallbackAlertMessage`/`CallbackAlertChannel`/`NotifyMethod`/`SysAlertRule` 跨 Task 签名一致。✅
5. **测试命令可执行**: 各 `-Dtest=` 与测试类名匹配 + `-Dsurefire.failIfNoSpecifiedTests=false`（Surefire 3.x，红线 `feedback_surefire3_failifno_specified_tests_param_rename`）。✅
6. **CLAUDE.md 更新**: T9 Step 4。✅
7. **验收标准完整性**: 每 Task 验收从 PRD/业务规则推导，断言值可手算。✅
8. **共享工具类**: `CallbackAlertMessage`/`CallbackAlertChannel`（T4 提供，T5-T8 消费）+ `LogSanitizer`（既有）已登记。✅
9. **核心类职责边界**: `CallbackAlertEvaluator` 依赖 2 ≤ 7，行数 < 300，已声明。✅
10. **Worktree 触发**: 命中 ② + ⑥ → 头部已声明 `wt-callback-p2c-alert`；闭环须 `git worktree remove`。✅

### Flyway / Surefire / spotbugs 专项核对

- **Flyway**: V34 > V33 无碰撞（T1 Step 1 + 签字前 + 实施前各 grep；红线 `feedback_plan_flyway_v_collision_check`）。✅
- **spotbugs**: 各渠道/引擎构造器 `EI_EXPOSE_REP2` constructor-level 注解 + 改注解后 `compile -am` 再 `spotbugs:check`（红线 `feedback_spotbugs_check_needs_recompile_after_annotation`）。✅
- **CRLF**: 新 logger 调用全 wrap `LogSanitizer` + `@SuppressFBWarnings(CRLF_INJECTION_LOGS)`（红线 `feedback_logsanitizer_alone_insufficient_for_findsecbugs`）。✅
- **IN_APP 行为保持**: V34 翻默认行 enabled+IN_APP + E2E IT 回归（T1 验收 #2 + T8 验收 #6）。✅

---

## 闭环（session-end / worktree teardown）

- [ ] 全 9 Task commit 完成，分支 `feat/callback-p2c-a-alert-channels` push。
- [ ] PR 开启，等 GHA CI 全绿（Build/Test/Checkstyle/SpotBugs/ArchUnit/JaCoCo）。
- [ ] merge 后 worktree teardown 实测：

```bash
git worktree remove /Users/muzhou/FEP_v1.0_wt-callback-p2c-alert
git worktree list   # 确认无残留
```

- [ ] session-end Phase 2 四步收尾（Simplify 三审 / 9 维技术文档 / Daily Report / push）— 有 code commit 的 Plan 须全 9 维文档（红线 `feedback_infra_plan_still_needs_full_8dim_docs`）。

---

## 执行交接

**⚠️ 本 Plan 未经评审 + 签字，禁止执行。**

### 步骤 1: AI 独立评审（santa-method / code-reviewer）

输入：本 Plan 全文 + PRD §5.5.3/§5.10.7.2d + `docs/guides/plan-review-checklist.md` 7 项。
重点核对：
- IN_APP 行为保持链（V34 默认行翻转 ↔ E2E IT ↔ T8 验收 #6）是否闭环；
- 单值→多值迁移（V34 ↔ Converter ↔ Entity ↔ DTO ↔ Service ↔ ControllerTest）类型一致；
- 渠道异常隔离 + LogSanitizer + spotbugs 注解层级；
- HOURLY/DAILY deferred 是否为完整代码而非 silent skip；
- Worktree 声明 + Flyway V34 无碰撞。

### 步骤 2: 人工 Plan Approver（muzhou）签字

阅读 Plan + AI 评审报告，抽样核对 PRD，决策批准/驳回/修改，文件末尾追加签字。

### 步骤 3: 执行方式选择

签字后选 subagent 驱动（推荐，长跑 mvn 按红线 `feedback_harness_bg_detach_hybrid_default` 走 hybrid：主对话实施 edits + 前台 mvn + commit，subagent 仅评审）或内联执行。

---

## 批准签字

| 角色 | 姓名 | 日期 | 决策 |
|---|---|---|---|
| Plan 作者 | Claude Code (mode A) | 2026-06-05 | 起草完成 |
| AI 独立评审 | code-reviewer (santa-style) | 2026-06-05 | Round 1 REVISE（BLOCKER-1 @ConfigurationProperties 注册）→ 作者修订（新建 `CallbackAlertConfiguration` @EnableConfigurationProperties）→ **Round 2 PASS** |
| **Plan 批准者** | **muzhou** | 2026-06-05 | ✅ **APPROVED** — 批准签字，subagent 驱动执行（hybrid：主对话实施 + 前台 mvn + commit / subagent 评审） |
