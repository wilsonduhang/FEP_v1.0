import { test, expect } from '@playwright/test';
import { loginAsAdmin } from './fixtures/auth';

test.describe('P2c callback UI', () => {
  test.beforeEach(async ({ page }) => await loginAsAdmin(page));

  test('notification bell renders in header', async ({ page }) => {
    await expect(page).toHaveURL(/\/home/);
    await expect(page.locator('.bell-badge')).toBeVisible();
  });

  test('credential page: create TOKEN credential', async ({ page }) => {
    // R1 ISSUE-5: unique interfaceId per run — backend rejects duplicate (BIZ_5002),
    // so a hardcoded id would 500 on the 2nd suite run. No teardown needed.
    const interfaceId = `IF-E2E-${Date.now()}`;
    await page.goto('/callback/credentials');
    await page.getByRole('button', { name: '新建凭证' }).click();
    await page.getByLabel('接口 ID').fill(interfaceId);
    await page.getByLabel('Token').fill('e2e-secret-token');
    await page.getByRole('button', { name: '确定' }).click();
    await expect(page.locator('.el-table').getByText(interfaceId)).toBeVisible();
  });

  test('dlq page loads', async ({ page }) => {
    await page.goto('/callback/dlq');
    await expect(page.getByText('回调死信队列')).toBeVisible();
  });
});
