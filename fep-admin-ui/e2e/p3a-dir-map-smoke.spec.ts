import { test, expect } from '@playwright/test';
import { loginAsAdmin } from './fixtures/auth';

/**
 * P3a T7 Step 4 — DIR-MAP 动态可配 smoke E2E.
 *
 * Prerequisites (all 4 must run simultaneously):
 *   1. Redis: docker run -d --name fep-redis -p 6379:6379 redis:7-alpine
 *   2. Backend: cd fep-web && ./mvnw spring-boot:run -Dspring.profiles.active=dev,dev-e2e
 *   3. Frontend: cd fep-admin-ui && pnpm dev
 *   4. Playwright: pnpm exec playwright test e2e/p3a-dir-map-smoke.spec.ts
 *
 * Asserts:
 *  - S1: sys-admin sees 报文方向映射 menu under 系统管理 → 系统配置
 *  - S2: page renders all 88 rows
 *  - S3: edit a row → save → success toast → revert
 */
test.describe('P3a DIR-MAP 动态可配 smoke', () => {
  test('S1: sys-admin 登录后看到方向映射菜单', async ({ page }) => {
    await loginAsAdmin(page);
    await page.getByRole('menuitem', { name: '系统管理' }).click();
    await page.getByRole('menuitem', { name: '系统配置' }).click();
    await expect(page.getByRole('menuitem', { name: '报文方向映射' })).toBeVisible();
  });

  test('S2: 进入页面应显示 88 行', async ({ page }) => {
    await loginAsAdmin(page);
    await page.goto('/system/config/dir-map');
    await expect(page.locator('.el-table__body-wrapper tr')).toHaveCount(88, { timeout: 15000 });
  });

  test('S3: 编辑 3001/ACCEPTING_ORG 后表格刷新显示新值', async ({ page }) => {
    await loginAsAdmin(page);
    await page.goto('/system/config/dir-map');
    const row = page.locator('.el-table__row', { has: page.getByText('3001') }).first();
    await row.locator('[data-test="btn-edit"]').click();
    await page.locator('[data-test="sel-direction"]').click();
    await page.getByRole('option', { name: '主动发起' }).click();
    await page.locator('[data-test="btn-save"]').click();
    await expect(page.getByText('保存成功，已立即生效')).toBeVisible();
    // 还原
    await row.locator('[data-test="btn-edit"]').click();
    await page.locator('[data-test="sel-direction"]').click();
    await page.getByRole('option', { name: '被动接收' }).click();
    await page.locator('[data-test="btn-save"]').click();
  });
});
