import { test, expect } from '@playwright/test';
import { loginAsAdmin } from './fixtures/auth';

/**
 * P7.2b submission smoke — 6 scenarios covering the /submit/* module delivered
 * by Task 1-6. Runs alongside the P7.2a spec against the dev-e2e backend
 * profile (Task 0a E2eSeedRunner + Task 0b dashboard endpoints).
 *
 * Selector conventions are aligned with the actual components (verified prior
 * to authoring):
 *   - Dashboard cards: `data-test="card-<fieldName>"` (SubmissionDashboardPage)
 *   - MockBadge: `.mock-badge` class on shared/components/MockBadge.vue
 *   - DataSourceEditDialog submit button label: "创建" (create mode) /
 *     "保存" (edit mode)
 *   - BusinessSceneEditDialog `推送方式` is an el-select with labels
 *     "自动"/"手动" (values AUTO/MANUAL); manual-mode validation message is
 *     "MANUAL 模式需填模板路径"
 *   - MessageSummaryPage renders `.summary-card` el-card tiles or the
 *     "暂无数据" empty state when no rows exist
 *
 * Execution prerequisites (same as p7.2a-smoke):
 *   - Redis container running on localhost:6379
 *   - Backend started with `--spring.profiles.active=dev,dev-e2e`
 *   - Frontend `pnpm dev` on http://localhost:5173 (Playwright webServer auto)
 *
 * Actual playwright run is deferred to an environment with Docker; this file
 * only needs to parse + selectors-best-effort for Task 7 spec authoring.
 */
test.describe('P7.2b submission smoke (6 scenarios)', () => {
  test.beforeEach(async ({ page }) => await loginAsAdmin(page));

  test('1. navigate to submission dashboard via direct URL', async ({ page }) => {
    await page.goto('/submit/dashboard');
    await expect(page).toHaveURL(/\/submit\/dashboard/);
    // At least one overview card renders — the 6 cards share the `data-test`
    // prefix `card-`. Using a CSS attribute-prefix selector here.
    await expect(page.locator('[data-test^="card-"]').first()).toBeVisible();
  });

  test('2. output interfaces page shows MockBadge + data table', async ({ page }) => {
    await page.goto('/submit/output-interfaces');
    await expect(page).toHaveURL(/\/submit\/output-interfaces/);
    // Data table always renders (empty or populated).
    await expect(page.locator('.el-table').first()).toBeVisible();
    // MockBadge is rendered next to connectivity-test buttons per row. If the
    // dev-e2e seed leaves the table empty, no MockBadge will be visible —
    // the page still satisfies the contract, so we assert existence OR empty
    // state.
    const mockBadge = page.locator('.mock-badge').first();
    const emptyState = page.locator('.el-table__empty-text').first();
    const hasMock = await mockBadge.isVisible().catch(() => false);
    const hasEmpty = await emptyState.isVisible().catch(() => false);
    expect(hasMock || hasEmpty).toBe(true);
  });

  test('3. data source create rejects non-digit phone (regex validation)', async ({ page }) => {
    await page.goto('/submit/data-sources');
    // Page button label: "+ 新建数据源"
    await page.getByRole('button', { name: /新建数据源/ }).click();
    // Wait for dialog to become visible
    await page.waitForSelector('.el-dialog', { state: 'visible', timeout: 5000 });
    // Fill required fields so the phone rule isolates the failure
    await page.getByLabel('数据源名称').fill('E2E_phone_test');
    await page.getByLabel('联系地址').fill('E2E_addr');
    await page.getByLabel('联系电话').fill('abc123');
    // Submit button in DataSourceEditDialog is labelled "创建" in create mode
    await page.locator('.el-dialog').getByRole('button', { name: '创建' }).click();
    // Validation message from rules.contactPhone.pattern
    await expect(page.getByText('联系电话为 1-11 位数字')).toBeVisible();
  });

  test('4. business scene MANUAL mode requires importTemplatePath', async ({ page }) => {
    await page.goto('/submit/scenes');
    // Page button label: "+ 新建业务场景"
    await page.getByRole('button', { name: /新建业务场景/ }).click();
    await page.waitForSelector('.el-dialog', { state: 'visible', timeout: 5000 });

    // Fill required fields so only importTemplatePath triggers the error.
    await page.getByLabel('场景名称').fill('E2E_scene_test');

    // 业务类型 — pick first option from the DEFAULT_BIZ_TYPE_OPTIONS list.
    await page.getByLabel('业务类型').click();
    await page.locator('.el-select-dropdown__item').first().click();

    // 推送方式 — switch to MANUAL (option label is "手动", value "MANUAL").
    await page.getByLabel('推送方式').click();
    await page.locator('.el-select-dropdown__item').getByText('手动').click();

    // 请求地址 must be a valid http(s) URL per rules.requestUrl.pattern.
    await page.getByLabel('请求地址').fill('https://example.com/hook');

    // Leave importTemplatePath blank; attempt save. Submit button is "创建".
    await page.locator('.el-dialog').getByRole('button', { name: '创建' }).click();

    // Validator error from rules.importTemplatePath
    await expect(page.getByText('MANUAL 模式需填模板路径')).toBeVisible();
  });

  test('5. dashboard renders 6 overview cards + trend switch', async ({ page }) => {
    await page.goto('/submit/dashboard');
    // All six cards must render even when values are "-" (overview unresolved).
    await expect(page.locator('[data-test="card-totalInterfaceCount"]')).toBeVisible();
    await expect(page.locator('[data-test="card-enabledInterfaceCount"]')).toBeVisible();
    await expect(page.locator('[data-test="card-totalDataSourceCount"]')).toBeVisible();
    await expect(page.locator('[data-test="card-totalRecordCount"]')).toBeVisible();
    await expect(page.locator('[data-test="card-pushedRecordCount"]')).toBeVisible();
    await expect(page.locator('[data-test="card-pendingRecordCount"]')).toBeVisible();

    // Toggle the trend window from 7-day default to 30-day. `el-radio-button`
    // renders as a label containing the inner text, reachable via getByText.
    await page.getByText('30 日', { exact: true }).click();
    // After the change the trend chart still mounts — the section header
    // remains visible and no crash surfaces as an uncaught ElMessage.error
    // toast beyond the normal "数据概况加载失败" (which only appears if the
    // backend is unavailable).
    await expect(page.getByText('推送趋势')).toBeVisible();
  });

  test('6. message summary page renders cards or empty state', async ({ page }) => {
    await page.goto('/submit/message-summary');
    await expect(page).toHaveURL(/\/submit\/message-summary/);
    // Either summary cards exist (populated) OR "暂无数据" (empty backend).
    const hasCards = await page
      .locator('.summary-card')
      .first()
      .isVisible()
      .catch(() => false);
    const hasEmpty = await page
      .getByText('暂无数据')
      .isVisible()
      .catch(() => false);
    expect(hasCards || hasEmpty).toBe(true);
  });
});
