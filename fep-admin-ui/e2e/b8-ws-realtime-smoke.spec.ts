/**
 * B-8 DEF-4 — Dashboard 关键告警 WebSocket 实时层端到端 smoke。
 *
 * 验证真实浏览器登录后 NotificationBell 挂载 → dashboardWsClient 经 Vite `/ws` 代理连到
 * 后端 `/ws/dashboard`，发出 `{type:'auth',token}` 首帧，且连接被服务端接受（短窗口内
 * 未被 close——认证失败会 POLICY_VIOLATION 立即关闭，故"保持打开"即"认证通过"）。
 *
 * 这是后端 IT（DashboardWebSocketAuthIT 用 StandardWebSocketClient）与前端单测
 * （dashboardWsClient.test）覆盖不到的一段：真实前端客户端 + 真实 JWT + Vite `/ws` 代理
 * + 真实后端握手鉴权的整链。
 *
 * 前置（同 e2e 既有约定）：
 *  - 后端 `--spring.profiles.active=dev,dev-e2e`（:8080，captcha bypass + V2 seed admin1）
 *  - Redis on localhost:6379
 *  - 前端 dev（:5173，playwright webServer 自启）
 *  - Vite `/ws` 代理（vite.config.ts，本 DEF-4 一并补）
 */
import { test, expect } from '@playwright/test';
import { loginAsAdmin } from './fixtures/auth';

test.describe.configure({ mode: 'serial' });

test.describe('B-8 DEF-4 Dashboard realtime WebSocket', () => {
  test('connects to /ws/dashboard, sends auth first-frame, server keeps it open', async ({ page }) => {
    const observed = { url: '', sentFrames: [] as string[], closed: false };

    const authFrameSent = new Promise<void>((resolve) => {
      page.on('websocket', (ws) => {
        if (!ws.url().includes('/ws/dashboard')) {
          return;
        }
        observed.url = ws.url();
        ws.on('framesent', (frame) => {
          const payload =
            typeof frame.payload === 'string' ? frame.payload : frame.payload.toString('utf8');
          observed.sentFrames.push(payload);
          if (payload.includes('"type":"auth"')) {
            resolve();
          }
        });
        ws.on('close', () => {
          observed.closed = true;
        });
      });
    });

    // 登录 → /home，NotificationBell 挂载触发 subscribeRealtime → WS 连接 + auth 首帧。
    await loginAsAdmin(page);

    // 等到 auth 首帧发出（或超时兜底，避免无限等待）。
    await Promise.race([authFrameSent, page.waitForTimeout(8000)]);

    expect(observed.url, 'a WebSocket to /ws/dashboard should open').toContain('/ws/dashboard');
    const authFrame = observed.sentFrames.find((p) => p.includes('"type":"auth"'));
    expect(authFrame, 'an auth first-frame should be sent over the wire').toBeTruthy();
    expect(authFrame as string).toContain('token');

    // 认证通过 → 服务端不应在短窗口内主动 close。
    await page.waitForTimeout(2500);
    expect(observed.closed, 'WS should stay open after a successful auth (not POLICY_VIOLATION closed)').toBe(
      false,
    );
  });
});
