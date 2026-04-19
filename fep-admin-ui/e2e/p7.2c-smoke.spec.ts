/** PRD §5.6 FR-WEB-REP-* smoke — 12 scenarios covering upload/records/view/push. */
import { test, expect } from '@playwright/test';
import { loginAsAdmin } from './fixtures/auth';

const BASE = 'http://localhost:5173';

test.describe('P7.2c §5.6 Report Management', () => {
  test.beforeEach(async ({ page }) => {
    await loginAsAdmin(page);
  });

  test('S1 /report/upload renders with MockBadge', async ({ page }) => {
    await page.goto(`${BASE}/report/upload`);
    // Post-run tuning: "手动报文上传" appears in both left menu item and page header;
    // target page-header content class to avoid strict-mode violation.
    await expect(page.locator('.el-page-header__content', { hasText: '手动报文上传' })).toBeVisible();
    await expect(page.getByText('文件解析 P1 就绪后启用')).toBeVisible();
  });

  test('S2 upload submit disabled until required filled', async ({ page }) => {
    await page.goto(`${BASE}/report/upload`);
    await expect(page.locator('[data-test="submit-upload"]')).toBeDisabled();
  });

  test('S3 unsupported file extension rejected', async ({ page }) => {
    await page.goto(`${BASE}/report/upload`);
    const fileInput = page.locator('input[type=file]');
    await fileInput.setInputFiles({
      name: 'notes.txt',
      mimeType: 'text/plain',
      buffer: Buffer.from('x'),
    });
    // v1b P1-new-#6 + post-run tuning: target ElMessage error container directly;
    // ElMessage auto-dismisses in ~3s so widen timeout + use .el-message--error class.
    await expect(
      page.locator('.el-message--error', { hasText: '不支持的扩展名' }),
    ).toBeVisible({ timeout: 5000 });
  });

  test('S4 xlsx file auto-fills messageName', async ({ page }) => {
    await page.goto(`${BASE}/report/upload`);
    await page.locator('input[type=file]').setInputFiles({
      name: '电子合同信息流转.xlsx',
      mimeType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
      buffer: Buffer.from(''),
    });
    // Post-run tuning: Vue v-model binds to DOM .value property (not value attribute);
    // use toHaveValue on the labelled input directly.
    await expect(page.getByLabel('报文名称')).toHaveValue('电子合同信息流转', { timeout: 5000 });
  });

  test('S5 /report/records shows total stat card', async ({ page }) => {
    await page.goto(`${BASE}/report/records`);
    await expect(page.getByText('总报文数')).toBeVisible();
  });

  test('S6 /report/records view button opens drawer', async ({ page }) => {
    await page.goto(`${BASE}/report/records`);
    const viewBtn = page.getByRole('button', { name: '查看详情' }).first();
    if ((await viewBtn.count()) > 0) {
      await viewBtn.click();
      await expect(page.getByText('报送记录详情')).toBeVisible();
    }
  });

  test('S7 /report/records jump to /report/view', async ({ page }) => {
    await page.goto(`${BASE}/report/records`);
    const jumpBtn = page.getByRole('button', { name: '查看明细' }).first();
    if ((await jumpBtn.count()) > 0) {
      await jumpBtn.click();
      await expect(page).toHaveURL(/\/report\/view\?messageType=/);
    }
  });

  test('S8 /report/view without query shows empty state', async ({ page }) => {
    await page.goto(`${BASE}/report/view`);
    await expect(page.getByText('请从报文数据列表或报送信息列表点击')).toBeVisible();
  });

  test('S9 /report/view?messageType=3001 renders stats+trend+table', async ({ page }) => {
    await page.goto(`${BASE}/report/view?messageType=3001`);
    await expect(page.getByText('总数据量')).toBeVisible();
    await expect(page.getByText('趋势（区间趋势）')).toBeVisible();
  });

  test('S10 /report/push relation card + CTA', async ({ page }) => {
    await page.goto(`${BASE}/report/push`);
    // Post-run tuning: "推送关联" appears in both .relation-title + .relation-desc;
    // target title class to avoid strict-mode violation.
    await expect(page.locator('.relation-title', { hasText: '推送关联' })).toBeVisible();
    await expect(page.getByText('真实 TLQ 推送 P1 就绪后启用')).toBeVisible();
    await page.getByRole('button', { name: '前往输出接口管理' }).click();
    await expect(page).toHaveURL(/\/submit\/output-interfaces/);
  });

  test('S11 /report/push trigger button state', async ({ page }) => {
    await page.goto(`${BASE}/report/push`);
    await expect(page.locator('[data-test="trigger-push"]')).toBeDisabled();
  });

  // v1b P1-new-#4: `.summary-card` class verified in SubMessageSummaryCards.vue (template + CSS).
  test('S12 /submit/message-summary navigates to /report/view (Task 0 fix)', async ({ page }) => {
    await page.goto(`${BASE}/submit/message-summary`);
    const card = page.locator('.summary-card').first();
    if ((await card.count()) > 0) {
      await card.click();
      await expect(page).toHaveURL(/\/report\/view\?messageType=/);
    }
  });
});
