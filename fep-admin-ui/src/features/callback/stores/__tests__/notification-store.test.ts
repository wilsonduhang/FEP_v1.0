import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { createPinia, setActivePinia } from 'pinia';
import { callbackNotificationApi } from '../../api/callbackNotification';
import { useNotificationStore } from '../notification-store';

vi.mock('../../api/callbackNotification');

const { wsConnect, wsDisconnect, captured } = vi.hoisted(() => ({
  wsConnect: vi.fn(),
  wsDisconnect: vi.fn(),
  captured: { onMessage: undefined as undefined | ((d: unknown) => void) },
}));

vi.mock('@/shared/ws/dashboardWsClient', () => ({
  resolveDashboardWsUrl: () => 'ws://test/ws/dashboard',
  createDashboardWsClient: (opts: { onMessage: (d: unknown) => void }) => {
    captured.onMessage = opts.onMessage;
    return { connect: wsConnect, disconnect: wsDisconnect };
  },
}));

const sample = {
  notificationId: 'N1',
  category: 'CALLBACK_DLQ',
  level: 'ERROR',
  title: '回调死信 - IF-001',
  message: 'queueId=Q1',
  refId: 'Q1',
  refType: 'CALLBACK_DLQ_ENTRY',
  read: false,
  createTime: 't',
  readAt: null,
};

describe('notification-store', () => {
  beforeEach(() => {
    setActivePinia(createPinia());
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('fetchCount stores the unread count', async () => {
    vi.mocked(callbackNotificationApi.unreadCount).mockResolvedValue(3);
    const store = useNotificationStore();
    await store.fetchCount();
    expect(store.unread).toBe(3);
  });

  it('fetchList stores the unread items', async () => {
    vi.mocked(callbackNotificationApi.listUnread).mockResolvedValue([sample]);
    const store = useNotificationStore();
    await store.fetchList();
    expect(store.items).toHaveLength(1);
    expect(store.items[0].notificationId).toBe('N1');
  });

  it('markRead calls the API, drops the item locally, and refetches count', async () => {
    vi.mocked(callbackNotificationApi.listUnread).mockResolvedValue([sample]);
    vi.mocked(callbackNotificationApi.markRead).mockResolvedValue(undefined);
    vi.mocked(callbackNotificationApi.unreadCount).mockResolvedValue(0);
    const store = useNotificationStore();
    await store.fetchList();

    await store.markRead('N1');

    expect(callbackNotificationApi.markRead).toHaveBeenCalledWith('N1');
    expect(store.items).toHaveLength(0);
    expect(callbackNotificationApi.unreadCount).toHaveBeenCalled();
    expect(store.unread).toBe(0);
  });

  it('startPolling fetches immediately then on the interval, and is idempotent', async () => {
    vi.useFakeTimers();
    vi.mocked(callbackNotificationApi.unreadCount).mockResolvedValue(1);
    const store = useNotificationStore();

    store.startPolling();
    expect(callbackNotificationApi.unreadCount).toHaveBeenCalledTimes(1); // immediate
    const timerAfterFirst = store.timer;
    store.startPolling(); // idempotent — must not start a second interval
    expect(store.timer).toBe(timerAfterFirst);

    vi.advanceTimersByTime(30_000);
    expect(callbackNotificationApi.unreadCount).toHaveBeenCalledTimes(2);
    vi.advanceTimersByTime(30_000);
    expect(callbackNotificationApi.unreadCount).toHaveBeenCalledTimes(3);
  });

  it('stopPolling clears the timer (no leaked interval)', async () => {
    vi.useFakeTimers();
    vi.mocked(callbackNotificationApi.unreadCount).mockResolvedValue(1);
    const store = useNotificationStore();
    store.startPolling();
    expect(store.timer).not.toBeNull();

    store.stopPolling();
    expect(store.timer).toBeNull();

    vi.advanceTimersByTime(90_000);
    // only the single immediate call from startPolling — interval was cleared
    expect(callbackNotificationApi.unreadCount).toHaveBeenCalledTimes(1);
  });

  it('subscribeRealtime connects WS and keeps polling baseline', () => {
    vi.useFakeTimers();
    vi.mocked(callbackNotificationApi.unreadCount).mockResolvedValue(0);
    const store = useNotificationStore();

    store.subscribeRealtime();

    expect(wsConnect).toHaveBeenCalledTimes(1);
    expect(store.timer).not.toBeNull(); // 轮询基线仍启用（WS 是增强非替代）
    expect(store.wsClient).not.toBeNull();

    store.subscribeRealtime(); // idempotent — 不重复建连
    expect(wsConnect).toHaveBeenCalledTimes(1);
  });

  it('a realtime notification frame refreshes count and list', () => {
    vi.mocked(callbackNotificationApi.unreadCount).mockResolvedValue(5);
    vi.mocked(callbackNotificationApi.listUnread).mockResolvedValue([sample]);
    const store = useNotificationStore();
    store.subscribeRealtime();

    captured.onMessage?.({ type: 'notification', notificationId: 'N1' });

    expect(callbackNotificationApi.unreadCount).toHaveBeenCalled();
    expect(callbackNotificationApi.listUnread).toHaveBeenCalled();
  });

  it('ignores non-notification frames (no list refresh)', () => {
    vi.mocked(callbackNotificationApi.unreadCount).mockResolvedValue(0);
    const store = useNotificationStore();
    store.subscribeRealtime();

    captured.onMessage?.({ type: 'heartbeat' });

    expect(callbackNotificationApi.listUnread).not.toHaveBeenCalled();
  });

  it('unsubscribeRealtime disconnects WS and stops polling', () => {
    vi.useFakeTimers();
    vi.mocked(callbackNotificationApi.unreadCount).mockResolvedValue(0);
    const store = useNotificationStore();
    store.subscribeRealtime();

    store.unsubscribeRealtime();

    expect(wsDisconnect).toHaveBeenCalledTimes(1);
    expect(store.wsClient).toBeNull();
    expect(store.timer).toBeNull();
  });
});
