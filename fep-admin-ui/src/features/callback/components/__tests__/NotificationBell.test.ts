import { mount, flushPromises } from '@vue/test-utils';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import ElementPlus from 'element-plus';
import NotificationBell from '../NotificationBell.vue';
import { callbackNotificationApi } from '../../api/callbackNotification';

vi.mock('../../api/callbackNotification');

const { wsConnect, wsDisconnect } = vi.hoisted(() => ({
  wsConnect: vi.fn(),
  wsDisconnect: vi.fn(),
}));

vi.mock('@/shared/ws/dashboardWsClient', () => ({
  resolveDashboardWsUrl: () => 'ws://test/ws/dashboard',
  createDashboardWsClient: () => ({ connect: wsConnect, disconnect: wsDisconnect }),
}));

/**
 * NotificationBell renders an el-badge over a bell icon inside an el-popover.
 *
 * The popover panel (per-item 标记已读 buttons) is lazily teleported and only
 * mounts once Popper opens it on a real reference click — which jsdom cannot drive
 * (Popper needs layout). The mark-read + polling business logic therefore lives in,
 * and is fully covered by, notification-store.test.ts (api call + local drop +
 * count refetch + interval start/stop). Here we cover the component wiring that is
 * deterministic in jsdom: the bell reference renders and polling starts on mount.
 */
describe('NotificationBell', () => {
  let container: HTMLDivElement;

  beforeEach(() => {
    setActivePinia(createPinia());
    vi.clearAllMocks();
    vi.mocked(callbackNotificationApi.unreadCount).mockResolvedValue(2);
    vi.mocked(callbackNotificationApi.listUnread).mockResolvedValue([]);
    vi.mocked(callbackNotificationApi.markRead).mockResolvedValue(undefined);
    container = document.createElement('div');
    document.body.appendChild(container);
  });

  afterEach(() => {
    while (document.body.firstChild) document.body.removeChild(document.body.firstChild);
  });

  it('renders the bell reference in the host', async () => {
    mount(NotificationBell, { global: { plugins: [ElementPlus] }, attachTo: container });
    await flushPromises();
    expect(container.querySelector('.bell-badge')).toBeTruthy();
  });

  it('starts polling on mount (fetches unread count)', async () => {
    mount(NotificationBell, { global: { plugins: [ElementPlus] }, attachTo: container });
    await flushPromises();
    expect(callbackNotificationApi.unreadCount).toHaveBeenCalled();
  });

  it('subscribes to realtime WebSocket on mount and disconnects on unmount', async () => {
    const wrapper = mount(NotificationBell, {
      global: { plugins: [ElementPlus] },
      attachTo: container,
    });
    await flushPromises();
    expect(wsConnect).toHaveBeenCalled();

    wrapper.unmount();
    expect(wsDisconnect).toHaveBeenCalled();
  });
});
