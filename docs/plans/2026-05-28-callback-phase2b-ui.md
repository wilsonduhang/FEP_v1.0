# Callback Phase 2b — fep-admin-ui 子 Plan（凭证 + DLQ + 站内信前端）

> **起草状态**: stub v0.1 — 主 Plan v0.2 拆分产出（M2 修订 / muzhou 2026-05-28 拍板）
> **For agentic workers:** REQUIRED SUB-SKILL: `superpowers:subagent-driven-development` 或 `superpowers:executing-plans` 按 Task 实施。

**主 Plan 引用**: [`2026-05-28-callback-phase2b-credential-dlq-alert-reaper.md`](2026-05-28-callback-phase2b-credential-dlq-alert-reaper.md) §3 T15 整段迁至本 Plan。

**Goal**: 闭环 Callback Phase 2b 管理 Web 前端 — 凭证管理（TOKEN/OAuth2 CRUD + masking）+ DLQ 列表查看 + 重放确认 + 站内信 bell + unread 列表。

---

## §0 元信息（治理）

| 项 | 值 |
|---|---|
| 作者 | Claude Code（mode A） |
| 起草日期 | 2026-05-28 |
| Plan version | v0.1 stub（主 Plan v0.2 拆分起草 — 待 AI 评审 + muzhou 签字 + 内容细化） |
| **执行 Worktree** | `/Users/muzhou/FEP_v1.0_wt-callback-p2b-ui`（分支 `feat/callback-phase2b-ui`，触发条件 ② + ⑦） |
| Baseline | **假设主 Plan PR (`feat/callback-phase2b-credential-dlq-alert-reaper`) merge to main**（含 6 个 REST endpoint）；实施前重 grep `git log origin/main --grep="Callback Phase 2b"` |
| 依赖 | **强依赖**: 主 Plan 后端 PR merge — 后端 controller 提供 endpoint `/api/callback/credentials/*` + `/api/callback/dlq/*` + `/api/notifications/*` |
| FR-ID | 共享主 Plan 4 FR-ID（UI 是 FR-INFRA-CALLBACK-CREDENTIAL/DLQ-REPLAY/IN-APP-ALERT 的前端实现） |
| AI 评审 | general-purpose 独立评审（红线 `secondary-ai-review`） |
| muzhou 签字 | 一票否决（7 项 plan-review-checklist） |
| 父 Plan | [`2026-05-28-callback-phase2b-credential-dlq-alert-reaper.md`](2026-05-28-callback-phase2b-credential-dlq-alert-reaper.md) |

---

## §1 Scope（11 文件 / 拆 4 commit ≤400 行）

**主 Plan T15 占位 stub 衔接**: 主 Plan T15 仅"实施前置检查 + 触发本子 Plan 实施" 2 step，本子 Plan 含真 file write + 测试 + commit + PR。

### 1.1 文件清单（11 文件，~600-800 行）

```
fep-admin-ui/src/views/callback/
├── CredentialList.vue                    # CREATE
├── CredentialForm.vue                    # CREATE（不回显密文）
├── DlqList.vue                           # CREATE
└── DlqReplayConfirm.vue                  # CREATE
fep-admin-ui/src/components/Notification/
└── NotificationBell.vue                  # CREATE（顶部 bell + 红点）
fep-admin-ui/src/api/
├── callbackCredential.ts                 # CREATE
├── callbackDlq.ts                        # CREATE
└── inAppNotification.ts                  # CREATE
fep-admin-ui/src/router/
└── routes.ts                             # MODIFY: +3 menu entries (admin role)
```

### 1.2 ≤400 行 PR 门禁拆分（4 commit）

| Commit | 文件 | 估行数 |
|---|---|---|
| C1 | `api/callbackCredential.ts` + `api/callbackDlq.ts` + `api/inAppNotification.ts` | ~150 |
| C2 | `views/callback/CredentialList.vue` + `views/callback/CredentialForm.vue` + Vitest tests | ~350 |
| C3 | `views/callback/DlqList.vue` + `views/callback/DlqReplayConfirm.vue` + Vitest tests | ~300 |
| C4 | `components/Notification/NotificationBell.vue` + `router/routes.ts` + E2E spec | ~250 |

---

## §2 Tasks（占位 — v0.1 stub，待主 Plan PR merge 后细化）

> 主 Plan T15 step 1-8 原版代码 sample 见父 Plan 历史版本（v0.1）或 git log。本 stub 仅留 outline，**v0.2 起草必含每 step 完整 file write 代码 + Vitest 用例 + Playwright E2E 用例**。

### 2.1 Task TU1 — API 客户端层 (commit C1)

- [ ] **Step 1**: 创建 `callbackCredential.ts` 含 6 个方法（listCredentials / getCredential / createCredential / updateCredential / deleteCredential / rotateCredential）
- [ ] **Step 2**: 创建 `callbackDlq.ts` 含 3 个方法（listDlq / replayDlq / findReplayChain）
- [ ] **Step 3**: 创建 `inAppNotification.ts` 含 3 个方法（unreadCount / listUnread / markNotificationRead）
- [ ] **Step 4**: Vitest 单测 — mock axios 验证每方法 URL + payload
- [ ] **Step 5**: Commit C1（红线 `feedback_unit_test_bypass`: trigger + flushPromises）

### 2.2 Task TU2 — 凭证管理页 (commit C2)

- [ ] **Step 1**: `CredentialList.vue` — 列表渲染 + tokenConfigured/oauthClientIdConfigured 标签 + 编辑/轮换/删除 按钮
- [ ] **Step 2**: `CredentialForm.vue` — TOKEN/OAUTH2 双分支 + 密码字段 `type="password" show-password` + update 时空字段保留语义提示
- [ ] **Step 3**: Vitest unit (rendering + interactions + api mock)
- [ ] **Step 4**: Commit C2

### 2.3 Task TU3 — DLQ 列表 + 重放 confirm 页 (commit C3)

- [ ] **Step 1**: `DlqList.vue` — list + originalDlqId 链 + 重放按钮触发 `DlqReplayConfirm`
- [ ] **Step 2**: `DlqReplayConfirm.vue` — modal 显示 queue 字段 + payload preview + 确认重放
- [ ] **Step 3**: Vitest unit
- [ ] **Step 4**: Commit C3

### 2.4 Task TU4 — NotificationBell + Router + E2E (commit C4)

- [ ] **Step 1**: `NotificationBell.vue` — 顶部组件 + 30s polling /api/notifications/unread/count + 下拉列表
- [ ] **Step 2**: `routes.ts` 加 3 menu (admin role)（红线 `feedback_permission_code_vs_menu_code` — meta.requiresAdmin 非 meta.permission）
- [ ] **Step 3**: Playwright E2E — admin login → 创建 OAuth2 凭证 → 触发 DLQ → 看 bell 红点 → 重放成功
- [ ] **Step 4**: Commit C4

### 2.5 Task TU5 — 独立 santa-method 评审 + closing

- [ ] **Step 1**: dispatch general-purpose santa-method 评审（**禁 model override**，红线 `feedback_subagent_model_override_auth_fragility`）
- [ ] **Step 2**: 跑 `pnpm test:run + pnpm exec playwright test` 全绿
- [ ] **Step 3**: PR 创建 + push（依 PR 拓扑：子 Plan PR 独立 merge to main，与父 Plan PR 解耦）
- [ ] **Step 4**: muzhou 7 项签字 merge
- [ ] **Step 5**: worktree cleanup `git worktree remove /Users/muzhou/FEP_v1.0_wt-callback-p2b-ui`

---

## §3 风险

| # | 风险 | 缓解 |
|---|---|---|
| R1 | 父 Plan PR 未 merge → endpoint 不存在 | 子 Plan 实施前 grep 父 PR merge 状态 |
| R2 | 4 commit 每 commit ≤400 行实际可能超 | C2 ~350 + C3 ~300 留 buffer；如超拆 C2a/C2b |
| R3 | E2E 依赖后端 dev server + Redis + Flyway 真跑 | 独立 worktree + Docker Redis + 后端 dev e2e profile（红线 `feedback_port_alive_vs_correct_service` L3 验证） |
| R4 | Vue3 unit test 误用 vm.method() 绕过 DOM | 红线 `feedback_unit_test_bypass` — 全用 trigger + flushPromises |

---

## §4 PRD 追溯

| FR-ID | PRD § | Task |
|---|---|---|
| FR-INFRA-CALLBACK-CREDENTIAL | §5.5.2 + §2.2.1 | TU1 + TU2 |
| FR-INFRA-CALLBACK-DLQ-REPLAY | §2.2.1 | TU1 + TU3 |
| FR-INFRA-CALLBACK-IN-APP-ALERT | §5.10.7.2d + 决策门 6 | TU1 + TU4 |

---

## §5 验收

- [ ] TU1-TU5 全部 Step 完成
- [ ] `pnpm test:run` 全绿
- [ ] `pnpm exec playwright test e2e/p2b-callback-credentials-and-dlq.spec.ts` 全绿
- [ ] 4 commit 全部 ≤400 行
- [ ] santa-method 独立评审 PASS / PASS WITH MINOR
- [ ] muzhou 7 项 plan-review-checklist 签字
- [ ] 子 Plan PR merge to main

---

## §6 AI 评审记录

> 待 v0.1 stub 内容细化为 v1.0 后派评审

---

## §7 muzhou 签字

> 待 v1.0 评审通过后签字

---

## §8 实施 Handoff

> **stub**: 本 Plan 是 v0.1 stub（仅 outline）。父 Plan PR merge to main 之后，新会话或本会话 (a) 把 v0.1 stub 细化为完整 v1.0（含每 step 完整代码 sample + Vitest + Playwright spec）→ (b) dispatch santa-method 评审 → (c) muzhou 签字 → (d) 实施 TU1-TU5。

实施前 baseline 重 grep（红线 `feedback_baseline_drift_during_long_review_cycle`）：

```bash
cd /Users/muzhou/FEP_v1.0
git fetch origin --quiet
git rev-parse --short origin/main
# 父 Plan PR merge 验证
git log origin/main --grep="Callback Phase 2b" --oneline | grep -v "phase2b-ui"
# 后端 endpoint 实测
grep -rn 'CallbackCredentialController\|CallbackDlqController\|InAppNotificationController' fep-web/src/main --include="*.java" | head -6
```

---

**生成方式**: 主 Plan v0.2 拆 P2b-UI 子 Plan stub（M2 修订 / muzhou 2026-05-28 拍板）
**衔接文件**: 父 Plan [`2026-05-28-callback-phase2b-credential-dlq-alert-reaper.md`](2026-05-28-callback-phase2b-credential-dlq-alert-reaper.md)
