# ADR — Nightly OWASP NVD API 429 系统性阻塞（W21 周缓存 chicken-and-egg + 无 NVD API key）

> 类型: 独立 CI infra 阻塞 ADR（非某 Plan 失败 — 由 Pitest minion 修复 Plan Step 5 contingency 升级而来）
> 生成日期: 2026-05-19 | 升级来源: `2026-05-19-pitest-junit5-plugin-junit512-fix.md` Step 5（muzhou 2026-05-19 AskUserQuestion 决策「确定推断收口 + NVD 升独立 ticket」）
> 状态: 🟢 OWNED — owning Plan: docs/plans/2026-05-19-nightly-nvd-api-key-systemic-fix.md（muzhou 2026-05-19 指定本会话；修法 #1 NVD API key 实施中）

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

## §closure（待 Task 4 填）

> 待 #2+#3 ship + seed workflow green + Nightly 暖缓存验证后填：commit SHA /
> seed run-id（cache saved owasp-nvd-Linux-v1）/ Nightly run-id（无 429 + 无
> timeout + NVD 分析完整 + report）/ tier-B（log4j CVSS gate 通过 → 全绿）/
> 状态 🟢 OWNED → ✅ RESOLVED。
