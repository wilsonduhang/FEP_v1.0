# ADR — Nightly OWASP NVD API 429 系统性阻塞（W21 周缓存 chicken-and-egg + 无 NVD API key）

> 类型: 独立 CI infra 阻塞 ADR（非某 Plan 失败 — 由 Pitest minion 修复 Plan Step 5 contingency 升级而来）
> 生成日期: 2026-05-19 | 升级来源: `2026-05-19-pitest-junit5-plugin-junit512-fix.md` Step 5（muzhou 2026-05-19 AskUserQuestion 决策「确定推断收口 + NVD 升独立 ticket」）
> 状态: ✅ RESOLVED（2026-05-19 本会话 NVD #2+#3 Plan Task 3 暖缓存 Nightly 实证 — 详见 §closure；修法链: #1 NVD API key `f728759`（必要不充分）+ #2 seed `3771adf` + #3 去周轮换 `3771adf`（充分））

## 1. 现象

2026-05-19 Pitest 修复分支 Nightly 两次（run `26068982532` / `26069287590`）均在 ~7-8min 于 OWASP step BUILD FAILURE：

```
[ERROR] Failed to execute goal org.owasp:dependency-check-maven:12.2.2:check
  (owasp-dependency-check) on project fep-parent: Fatal exception(s):
    UpdateException: Error updating the NVD Data
      caused by NvdApiException: NVD Returned Status Code: 429
    NoDataException: No documents exist
```

主干 scheduled Nightly 同期 5/5 连续 failure（2026-05-15 ~ 2026-05-18）。

## 2. 根因（grep 实测）

`.github/workflows/nightly.yml`:
- NVD 缓存 key = `owasp-nvd-${{ runner.os }}-${{ week }}`（**周轮换**）+ `restore-keys: owasp-nvd-${{ runner.os }}-`
- **无 NVD API key 配置**（无 `--nvdApiKey` / `NVD_API_KEY` secret 引用）

2026-05-19 进入新周 **2026-W21**。Pitest 分支 run2 日志实测：
```
Cache OWASP NVD data  key: owasp-nvd-Linux-2026-W21
Cache not found for input keys: owasp-nvd-Linux-2026-W21, owasp-nvd-Linux-
```

**Chicken-and-egg**：W21 无任何缓存 → 每次冷全量下载 NVD → 无 API key 下 NVD 速率限制极低（5 req/30s）→ 立即 429 → BUILD FAILURE **早于 cache-save step** → W21 缓存永远无法被填充 → 下一次仍冷启动 → 死循环。主干 + 所有 feature 分支同等受影响（缓存非分支隔离，但 W21 根本无缓存可 restore）。

## 3. 正交性 / 范围裁定

- **与 Pitest minion 修复正交**：`mvn verify` 中 `pitest:mutationCoverage` goal 严格在 `owasp:check` 之后；OWASP 在 NVD 429 死，Pitest goal 永远跑不到。Pitest 修复（`71bf5cf` pitest-maven 1.23.1 + junit5-plugin 1.2.3）已 ship origin/main，确定性充分（版本对 Maven Central metadata 双源 + hcoles/pitest#1389 精确匹配 + PR #24 CI 全绿证依赖解析不破坏构建）。
- **与 dependency-check 12.2.2 升级（根因 1，Plan A `9ba7e04`）相关但非其引入**：12.2.2 冷启动 NVD 与 10.0.4 同样需 NVD API；本质是「无 API key + 周轮换冷缓存」长期隐患，被 W21 切换触发显形。

## 4. 候选修复方向（待 owning session 评估，本 ADR 不实施）

1. **配置 NVD API key**（首选）：muzhou 申请 NVD API key（https://nvd.nist.gov/developers/request-an-api-key）→ 存 GitHub secret `NVD_API_KEY` → nightly.yml dependency-check 加 `-DnvdApiKey=${{ secrets.NVD_API_KEY }}`。授权后速率限制大幅提升，冷启动可完成。
2. **缓存种子引导**：手动一次性在低峰期跑通 NVD 下载（容忍长耗时 + 退避重试）填充 W21 缓存，后续 restore-keys 命中走增量。脆弱（每周轮换重复）。
3. **缓存 key 去周轮换** + 增量更新策略：改长周期 key + 定期刷新，减少冷启动频次。
4. **dependency-check 降级非阻塞 / 拆分**：不可接受（丢 CVSS≥7 门禁，与 Plan C 同理由排除）。

## 5. 影响

- Nightly Deep Scan 持续 RED（OWASP step 终止），但根因已剥到此 NVD infra 层 —— OWASP CVE 检测本身（SB 3.5.14 修复）+ OSS Index disable + Pitest minion 修复三层均已 ship origin/main，**修复就位待 NVD 解封后暖缓存 Nightly 自动正向背书**。
- 本 ADR 是 Nightly RED 洋葱「根因 4 = NVD 429 W21 infra」层登记，与根因 1/2/3 正交。

## 6. 衔接

- Pitest 修复 Plan `2026-05-19-pitest-junit5-plugin-junit512-fix.md` 验收 1/2 已满足（config 生效 + PR CI 绿）；验收 3/4/5/6 标注「待 NVD-429 W21 解封后暖缓存 Nightly 背书」，非本 Plan 失败。
- owning session 立项后，本 ADR slug 可升格为正式 infra Plan 或并入别会话 `2026-05-18-nightly-owasp-dependency-check-infra-fix.md` 后续。

---

## §finding（2026-05-19）: 修法 #1 已 ship 但不充分 — 实证

> 前序 owning Plan: `docs/plans/2026-05-19-nightly-nvd-api-key-systemic-fix.md`（v3，ship `f728759`）

修法 #1（pom `<nvdApiKeyEnvironmentVariable>NVD_API_KEY` + nightly.yml `NVD_API_KEY` env）
验证 run `26083645875`（gh run --log 实测）:
- `grep -c "429|UpdateException"` = **0** → 429 死循环**已打破**（#1 必要且正确）
- `NVD API has 351,500 records`（08:32:54）→ 授权 API 正常（key 生效）
- 下载 08:37 3% → 09:32 **34% (120,000/351,501)** → `##[error]The operation was canceled`
  （60min job timeout 在 34% 时 cancel）；cancelled job 不存 actions/cache
- 结论: #1 必要不充分。429 消除但冷全量 ~175min >> 60min timeout，
  cancelled-no-cache → W21 永冷 → 循环换形态（timeout 替代 429）

→ 触发 ADR §4 #2（一次性 long-timeout seed 填稳定 key 缓存）+ #3（去周轮换持久化）。
互补 owning Plan: `docs/plans/2026-05-19-nightly-nvd-cache-seed-derotate.md`
（muzhou 2026-05-19 AskUserQuestion 决策「#2+#3」，v1.1 AI 评审 PASS WITH MINOR + muzhou 签字）。

## §closure（2026-05-19）: ✅ RESOLVED — NVD infra 层根因 4 完全消除

> 状态: 🟢 OWNED → **✅ RESOLVED**（本会话 2026-05-19 NVD #2+#3 Plan Task 3 暖缓存 Nightly 实证）。

### 修法 ship 实证

- 互补 owning Plan: `docs/plans/2026-05-19-nightly-nvd-cache-seed-derotate.md`（v1.1，AI 评审 PASS WITH MINOR，muzhou 2026-05-19 签字）
- ship commit: **`3771adf`** (origin/main)
  - `.github/workflows/nvd-cache-seed.yml` NEW（#2 一次性 seed workflow，timeout-minutes:300 + workflow_dispatch + `dependency-check-maven:12.2.2:update-only` goal + `concurrency: nightly`）
  - `.github/workflows/nightly.yml` modified（#3 去周轮换 — 移除 `Compute NVD cache key (weekly rotation)` step + cache key 改稳定 `owasp-nvd-${{ runner.os }}-v1`）

### tier-A 实证（本 ADR 范围）

**Seed run `26100940590`**（GHA workflow_dispatch on `chore/nightly-nvd-cache-seed`）:
- duration: **4h0m19s**（300min timeout 充分，无 cancellation）
- NVD download: **`Downloaded 351,616/351,616 (100%)`** 全量完整
- BUILD SUCCESS（dependency-check-maven:12.2.2:update-only 退出 0）
- `Cache saved with key: owasp-nvd-Linux-v1`（actions/cache post-step 实测落盘）
- 0 个 429 / 0 个 timeout / 0 个 cancelled

**Nightly verify run `26114517797`**（GHA scheduled→workflow_dispatch on `3771adf` main）:
- duration: **1m20s**（vs 之前 60min timeout 在 34% 死掉 — 暖缓存命中绕过冷下载）
- `Cache restored from key: owasp-nvd-Linux-v1` ✅
- `[INFO] Skipping the NVD API Update as it was completed within the last 240 minutes` ✅（NVD DB 鲜度 30min vs 240min 阈值）
- `[INFO] Finished NVD CVE Analyzer (0 seconds)` ✅（CVE 分析 ≤ 1s）
- `[INFO] Writing XML report to: target/dependency-check-report.xml` + `Writing HTML report` ✅（parent + fep-common 两份 report 落盘）
- `Artifact owasp-dependency-check-reports has been successfully uploaded! Final size is 104521 bytes` ✅（artifact 上传成功）
- 0 个 429 / 0 个 cancelled / 0 个 NVD timeout

→ **W21 chicken-and-egg 死循环已破除**。NVD infra 层根因 4 = NVD 429 W21 系统性阻塞 → ✅ 解决。

### tier-B 实证（OWASP 层附带验证）

- OWASP `dependency-check:12.2.2:check` BUILD 阶段 SUCCESS（既未 NVD 错误也未触发 CVSS≥7 gate）
- log4j-to-slf4j suppression（commit `a79574d`，已在 origin/main `7f2b72e` 之前）实证生效 — `vulnerabilityFailOnCVSS=7` 未阻断 OWASP step
- **OWASP layer 在 Nightly 中首次完整跑通并产出 report** — SB 3.5.14 T1#4「OWASP CVSS≥7=0」warm-cache Nightly 实证终于关闭（衔接 prompt priority #1）

### 出范围 — 新 systemic CI blocker（与本 ADR 正交）

Nightly run `26114517797` 最终 `BUILD FAILURE`（exit 1）但**故障点不在 NVD/OWASP 层**：

```
[ERROR] Failed to execute goal org.pitest:pitest-maven:1.23.1:mutationCoverage
  (pitest-mutation-coverage) on project fep-common: Execution
  pitest-mutation-coverage of goal org.pitest:pitest-maven:1.23.1:mutationCoverage
  failed: History has been enabled but no history plugin has been
  installed/activated.
[ERROR] If you are using https://www.arcmutate.com remember to activate the
  history plugin with +arcmutate_history
```

- **判定**：pitest 1.23.x 配置漂移（pom `<historyInputFile>`/`<historyOutputFile>` 配置存在但未装 arcmutate history plugin）。NVD/OWASP 层完全清洁，pitest 是独立 systemic CI blocker。
- **范围排除依据**：本 ADR 范围严格限于 NVD infra 层根因 4。pitest history plugin 是新的、与 NVD #1/#2/#3 修法正交的独立问题。
- **follow-up 候选**：升独立 ADR / Plan（候选 owner 待 muzhou 指定），可能复用 pitest minion 修复 Plan 路径或新建专项。
- **红线适用**：候选新红线 `feedback_systemic_ci_blocker_defers_positive_backing`（本会话 CLAUDE.md 当前状态登记） — Plan A 同型闭环（objective 达成 + ship + 静态验证；正向 Nightly 背书 deferred 出范围系统性 CI 阻塞解决）。

### 同源阻塞群组关系

- 根因 1（OWASP dependency-check 10.0.4 → 12.2.2 H2 schema overflow）：Plan A `9ba7e04` ✅ RESOLVED（别会话）
- 根因 2（OSS Index 401 anonymous）：Plan B `549a0b2` ✅ RESOLVED（别会话）
- 根因 3（log4j-to-slf4j suppression 覆盖 gap）：gap Plan `a79574d` ✅ RESOLVED（别会话）
- 根因 4（本 ADR — NVD 429 W21 chicken-and-egg）：✅ RESOLVED
  - 修法 #1（NVD API key）：`f728759` 必要不充分（429 消除但 60min timeout）
  - 修法 #2（一次性 seed）+ #3（去周轮换）：`3771adf` 充分（暖缓存 restore 1m20s 绕过冷下载）
- 根因 5（pitest history plugin）：🆕 NEW，独立 follow-up，与本 ADR 群组正交
