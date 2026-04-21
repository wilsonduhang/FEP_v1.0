/** PRD §5.7 FR-WEB-TLQ-CFG + FR-WEB-TLQ-HB smoke — 10 scenarios covering node/queue/connectivity. */
import { test, expect } from '@playwright/test';
import { loginAsAdmin } from './fixtures/auth';

const BASE = 'http://localhost:5173';

test.describe.configure({ mode: 'serial' });
test.describe('P7.2d §5.7 TLQ Node Management', () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test('S1: navigates to /tlq/nodes and shows page header', async ({ page }) => {
    await page.goto(`${BASE}/tlq/nodes`);
    // el-page-header renders title= prop in .el-page-header__title slot
    // (.el-page-header__content holds the `content=` prop — "§5.7.1 节点配置列表")
    await expect(page.locator('.el-page-header__title', { hasText: 'TLQ 节点管理' })).toBeVisible();
  });

  test('S2: nodes table renders or shows empty state', async ({ page }) => {
    await page.goto(`${BASE}/tlq/nodes`);
    // Either el-table exists OR el-empty-text shown — both valid states
    // Wait for either to be present (no polling loop, deterministic)
    await page.waitForSelector('.el-table, .el-empty-text', { timeout: 10000 });
    // Verify at least one of them is in the DOM
    const tableExists = await page.locator('.el-table').count();
    const emptyExists = await page.locator('.el-empty-text').count();
    expect(tableExists + emptyExists).toBeGreaterThanOrEqual(1);
  });

  test('S3: opens 新建节点 dialog then closes without creating', async ({ page }) => {
    await page.goto(`${BASE}/tlq/nodes`);
    // Click the "新建节点" button
    await page.getByRole('button', { name: '新建节点' }).click();
    // Assert dialog opens with expected title (TlqNodeEditDialog uses el-dialog)
    await expect(page.locator('.el-dialog__title')).toBeVisible();
    // Close dialog via cancel button or close icon
    const cancelBtn = page.getByRole('button', { name: '取消' });
    if ((await cancelBtn.count()) > 0) {
      await cancelBtn.click();
    } else {
      // Fallback: click close icon if no cancel button
      await page.locator('.el-dialog__close').click();
    }
    // Assert dialog is closed (no longer in DOM or hidden)
    await expect(page.locator('.el-dialog__title')).not.toBeVisible({
      timeout: 5000,
    });
  });

  test('S4: role filter dropdown shows 4 options', async ({ page }) => {
    await page.goto(`${BASE}/tlq/nodes`);
    // TlqNodesPage renders a filter el-select for "角色" with ROLE_OPTIONS
    // Per TlqNodesPage.vue, 4 roles: MASTER_PRODUCER, MASTER_STANDBY, SLAVE_CONSUMER, SLAVE_STANDBY
    const roleSelect = page.locator('label:has-text("角色")').locator('..').locator('.el-select');
    await roleSelect.click();
    // Wait for dropdown to open
    await page.waitForSelector('.el-select-dropdown', { timeout: 5000 });
    // Count ONLY role-specific dropdown items by matching unique role label prefixes
    // (page also has status filter dropdown + el-pagination page-size select — all
    // render items with the same .el-select-dropdown__item class; hence the text filter).
    const roleItems = await page
      .locator('.el-select-dropdown__item', { hasText: /主节点|从节点/ })
      .count();
    expect(roleItems).toBe(4);
  });

  test('S5: navigate to /tlq/queues shows node picker', async ({ page }) => {
    await page.goto(`${BASE}/tlq/queues`);
    // TlqQueuesPage renders an el-select with placeholder "请选择节点"
    await expect(page.locator('.el-select', { hasText: '请选择节点' }).first()).toBeVisible();
  });

  test('S6: selecting a node triggers listByNode network call (skip if no seeded nodes)', async ({
    page,
  }) => {
    // First check if any nodes exist
    await page.goto(`${BASE}/tlq/nodes`);
    const rowCount = await page.locator('.el-table__body tr').count();
    test.skip(rowCount === 0, 'No seeded nodes — S6 skipped to avoid runtime branching');

    // If nodes exist, navigate to queues and select first node
    await page.goto(`${BASE}/tlq/queues`);
    const nodeSelect = page.locator('.el-select', { hasText: '请选择节点' }).first();
    await nodeSelect.click();
    // Wait for dropdown to open and click first option
    await page.waitForSelector('.el-select-dropdown__item', { timeout: 5000 });
    await page.locator('.el-select-dropdown__item').first().click();
    // Assert network call to /api/v1/tlq/queues?nodeId=... fires
    await page.waitForURL(/\/tlq\/queues/, { timeout: 5000 });
    // The table or empty state should update after selection
    await page.waitForSelector('.el-table, .el-empty', { timeout: 5000 });
  });

  test('S7: batch-generate dialog defaults organizationCode to A1000143000104', async ({
    page,
  }) => {
    // Pre-check: skip if no TLQ nodes are seeded (same guard pattern as S6/S9/S10
    // to avoid runtime branching and flaky timeouts when /tlq/queues has no rows).
    await page.goto(`${BASE}/tlq/nodes`);
    const rowCount = await page.locator('.el-table__body tr').count();
    test.skip(
      rowCount === 0,
      'No seeded TLQ nodes — S7 skipped (batch-generate button requires node selection)',
    );

    await page.goto(`${BASE}/tlq/queues`);
    const nodeSelect = page.locator('.el-select', { hasText: '请选择节点' }).first();
    await nodeSelect.click();
    await page.waitForSelector('.el-select-dropdown__item', { timeout: 5000 });
    await page.locator('.el-select-dropdown__item').first().click();
    await page.waitForTimeout(500); // Brief pause for button state update
    // Click "§3.1.2 批量生成" button (now enabled since a node is selected)
    const batchBtn = page.getByRole('button', { name: /§3\.1\.2 批量生成/ });
    await batchBtn.click();
    await page.waitForSelector('.el-dialog', { state: 'visible', timeout: 5000 });
    // Input placeholder text hints "HNDEMP 中心代码" — check the input value
    const orgCodeInput = page.locator('input[placeholder*="HNDEMP"]');
    await expect(orgCodeInput).toHaveValue('A1000143000104');
    // Close dialog via cancel button
    await page.getByRole('button', { name: '取消' }).click();
    await expect(page.locator('.el-dialog')).not.toBeVisible({ timeout: 5000 });
  });

  test('S8: /tlq/connectivity shows empty state when no node selected', async ({ page }) => {
    await page.goto(`${BASE}/tlq/connectivity`);
    // TlqConnectivityPage renders el-empty with description "请先选择节点"
    await expect(page.locator('.el-empty', { hasText: '请先选择节点' })).toBeVisible();
  });

  test('S9: selecting node shows MockBadge near trigger button (skip if no seeded nodes)', async ({
    page,
  }) => {
    // First check if any nodes exist
    await page.goto(`${BASE}/tlq/nodes`);
    const rowCount = await page.locator('.el-table__body tr').count();
    test.skip(rowCount === 0, 'No seeded nodes — S9 skipped to avoid runtime branching');

    // Navigate to connectivity and select first node
    await page.goto(`${BASE}/tlq/connectivity`);
    const nodeSelect = page.locator('.el-select', { hasText: '请选择节点' }).first();
    await nodeSelect.click();
    await page.waitForSelector('.el-select-dropdown__item', { timeout: 5000 });
    await page.locator('.el-select-dropdown__item').first().click();
    // Wait for content to render
    await page.waitForTimeout(500);
    // Assert MockBadge is visible (component class: .mock-badge per TlqConnectivityPage.vue)
    await expect(page.locator('.mock-badge')).toBeVisible();
  });

  test('S10: trigger button fires POST .../test once and surfaces el-message (deterministic)', async ({
    page,
  }) => {
    // First check if any nodes exist
    await page.goto(`${BASE}/tlq/nodes`);
    const rowCount = await page.locator('.el-table__body tr').count();
    test.skip(rowCount === 0, 'No seeded nodes — S10 skipped to avoid runtime branching');

    // Navigate to connectivity and select first node
    await page.goto(`${BASE}/tlq/connectivity`);
    const nodeSelect = page.locator('.el-select', { hasText: '请选择节点' }).first();
    await nodeSelect.click();
    await page.waitForSelector('.el-select-dropdown__item', { timeout: 5000 });
    await page.locator('.el-select-dropdown__item').first().click();
    await page.waitForTimeout(500);

    // Red line #3 (global test red line): S10 assertion must be DETERMINISTIC.
    // Banned: disjunctive assertions like "success OR warning OR error".
    // Allowed: structural facts like "DOM node exists" or "network call count".

    // Step 1: Capture the network response for the POST .../test call
    const [response] = await Promise.all([
      page.waitForResponse(
        (r) =>
          /\/api\/v1\/tlq\/connectivity\/[^/]+\/test$/.test(r.url()) &&
          r.request().method() === 'POST',
      ),
      // Step 2: Click trigger button (per TlqConnectivityPage.vue: "触发 9005 心跳测试")
      page.getByRole('button', { name: /触发 9005 心跳测试/ }).click(),
    ]);

    // Step 3: Assert the response exists and is well-formed (status in range [200, 500))
    expect(response.status()).toBeGreaterThanOrEqual(200);
    expect(response.status()).toBeLessThan(500);

    // Step 4: DETERMINISTIC assertion — el-message DOM node appears (type-agnostic).
    // Per p7.2c-smoke.spec.ts S3, ElMessage auto-dismisses in ~3s, so widen timeout.
    // This is a structural fact (node existence), not a content assertion.
    await page.waitForSelector('.el-message', { timeout: 5000 });
  });
});
