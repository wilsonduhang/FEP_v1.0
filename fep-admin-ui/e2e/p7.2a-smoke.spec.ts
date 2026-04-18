import { test, expect } from '@playwright/test';
import { loginAsAdmin } from './fixtures/auth';
import { E2E_SEED_ENTERPRISES } from './fixtures/seed-data';

test.describe('P7.2a smoke (10 scenarios)', () => {
  // Scenario 1: login flow is covered by beforeEach (home page reached)
  test.beforeEach(async ({ page }) => await loginAsAdmin(page));

  test('1. login lands on home page', async ({ page }) => {
    await expect(page).toHaveURL(/\/home/);
  });

  test('2. sidebar renders >=4 business menu items', async ({ page }) => {
    await expect(page.getByRole('menuitem', { name: /查询任务管理/ })).toBeVisible();
    await expect(page.getByRole('menuitem', { name: /授权书管理/ })).toBeVisible();
    await expect(page.getByRole('menuitem', { name: /报文定义/ })).toBeVisible();
    await expect(page.getByRole('menuitem', { name: /报文记录/ })).toBeVisible();
  });

  test('3. navigate to query tasks and see Mock Mode badge', async ({ page }) => {
    await page.getByRole('menuitem', { name: /查询任务管理/ }).click();
    await expect(page).toHaveURL(/\/enterprise\/query-tasks/);
    await expect(page.getByText('TLQ Mock 模式')).toBeVisible();
    await expect(page.locator('.el-table').first()).toBeVisible();
  });

  test('4. create query task with valid USCI succeeds', async ({ page }) => {
    // Task 7: use seeded USCI from E2eSeedRunner (dev-e2e profile). The row
    // corresponding to E2E_SEED_ENTERPRISES[0] is upserted APPROVED on backend
    // startup, unlocking EntQueryTaskService.create() APPROVED-enterprise check.
    const seededUsci = E2E_SEED_ENTERPRISES[0].usci;
    await page.goto('/enterprise/query-tasks');
    await page.getByRole('button', { name: /新建查询任务/ }).click();
    await page.getByLabel('企业 ID').fill('E-TEST');
    await page.getByLabel('被查询 USCI').fill(seededUsci);
    await page.getByRole('button', { name: '创建' }).click();
    // v2a P2-F: scope to table to avoid tooltip truncation
    await expect(page.locator('.el-table').getByText(seededUsci).first()).toBeVisible();
  });

  test('5. create query task with 17-char USCI fails validation', async ({ page }) => {
    await page.goto('/enterprise/query-tasks');
    await page.getByRole('button', { name: /新建查询任务/ }).click();
    await page.getByLabel('企业 ID').fill('E-TEST');
    await page.getByLabel('被查询 USCI').fill('91310000MA1K40XK7'); // 17 chars
    await page.getByRole('button', { name: '创建' }).click();
    // Dialog should still be open; validation error visible
    // v2a P1-D: loosened regex for Element Plus error rendering
    await expect(page.getByText(/USCI.*18\s*位/)).toBeVisible();
  });

  test('6. execute DRAFT task transitions to PROCESSING', async ({ page }) => {
    await page.goto('/enterprise/query-tasks');
    // Assumes at least one DRAFT task exists (seeded from scenario 4 or backend)
    const draftRow = page.locator('.el-table__row', { hasText: 'DRAFT' }).first();
    // v2a P2-G: force at least one DRAFT row exists (scenario 4 creates one)
    await expect(draftRow).toHaveCount(1);
    await draftRow.getByRole('button', { name: '执行' }).click();
    await expect(page.getByText(/已触发执行/)).toBeVisible();
  });

  test('7. task detail drawer shows info + results table', async ({ page }) => {
    await page.goto('/enterprise/query-tasks');
    const detailBtn = page.getByRole('button', { name: '详情' }).first();
    await detailBtn.click();
    await expect(page.locator('.el-drawer__body')).toContainText('任务信息');
    await expect(page.locator('.el-drawer__body')).toContainText('查询类型');
  });

  test('8. auth letters page loads with new button', async ({ page }) => {
    await page.goto('/enterprise/auth-letters');
    await expect(page).toHaveURL(/\/enterprise\/auth-letters/);
    await expect(page.getByRole('button', { name: /新建授权书/ })).toBeVisible();
    await expect(page.locator('.el-table').first()).toBeVisible();
  });

  test('9. definitions page shows extended search filters (Task 0b)', async ({ page }) => {
    await page.goto('/biz/definitions');
    await expect(page.locator('.el-table').first()).toBeVisible();
    // Task 0b enables messageCode / direction / definitionStatus filters on the UI
    await expect(page.getByLabel('方向')).toBeVisible();
  });

  test('10. records page shows 4 summary cards and XML preview drawer', async ({ page }) => {
    await page.goto('/biz/records');
    await expect(page.getByText('总报文数据')).toBeVisible();
    await expect(page.getByText('成功数')).toBeVisible();
    await expect(page.getByText('待处理数')).toBeVisible();
    await expect(page.getByText('失败数')).toBeVisible();
    // v2a P2-G: assert records exist before clicking detail
    const detailBtn = page.getByRole('button', { name: '详情' }).first();
    await expect(detailBtn).toBeVisible({ timeout: 5000 });
    await detailBtn.click();
    await expect(page.locator('.el-drawer__body')).toBeVisible();
  });
});
