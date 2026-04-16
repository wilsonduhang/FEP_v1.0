import { type Page, expect } from '@playwright/test';

/**
 * Logs in as admin1 using the E2E captcha bypass token configured by
 * backend dev-e2e profile (see Task 0a, application-dev-e2e.yml).
 *
 * Prerequisites:
 *  - backend running with --spring.profiles.active=dev,dev-e2e
 *  - FEP_E2E_CAPTCHA_BYPASS_TOKEN env (defaults to 'e2e-bypass' per yml)
 *  - Redis on localhost:6379
 *  - V2 seed admin1 / admin@FEP2026
 *
 * Flow:
 *  1. load /login
 *  2. fill form fields via getByLabel (now reachable after Task 0a for/id fix)
 *  3. fill captcha with the bypass token — backend accepts without Redis lookup
 *  4. click login button
 *  5. expect navigation to /home
 */
const CAPTCHA_BYPASS_TOKEN = process.env.FEP_E2E_CAPTCHA_BYPASS_TOKEN ?? 'e2e-bypass';

export async function loginAsAdmin(page: Page): Promise<void> {
  await page.goto('/login');
  await page.getByLabel('账号').fill('admin1');
  await page.getByLabel('密码').fill('admin@FEP2026');
  await page.getByLabel('验证码').fill(CAPTCHA_BYPASS_TOKEN);
  await page.getByRole('button', { name: '登录' }).click();
  await expect(page).toHaveURL(/\/home/);
}
