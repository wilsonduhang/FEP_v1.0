import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { createDashboardWsClient, resolveDashboardWsUrl } from '../dashboardWsClient';

/**
 * 可控的 WebSocket 替身。
 * 注意：close() 同步触发 onclose（真实 WebSocket 为异步），用于单元测试控制流简化。
 */
class FakeWebSocket {
  static instances: FakeWebSocket[] = [];
  url: string;
  sent: string[] = [];
  closed = false;
  onopen: ((ev?: unknown) => void) | null = null;
  onmessage: ((ev: { data: unknown }) => void) | null = null;
  onclose: ((ev?: unknown) => void) | null = null;
  onerror: ((ev?: unknown) => void) | null = null;

  constructor(url: string) {
    this.url = url;
    FakeWebSocket.instances.push(this);
  }

  send(data: string): void {
    this.sent.push(data);
  }

  close(): void {
    this.closed = true;
    this.onclose?.();
  }

  emitOpen(): void {
    this.onopen?.();
  }

  emitMessage(data: unknown): void {
    this.onmessage?.({ data });
  }
}

describe('dashboardWsClient', () => {
  beforeEach(() => {
    FakeWebSocket.instances = [];
    vi.stubGlobal('WebSocket', FakeWebSocket as unknown as typeof WebSocket);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.unstubAllEnvs();
    vi.useRealTimers();
  });

  it('sends an auth frame with the token on open', () => {
    const client = createDashboardWsClient({
      url: 'ws://host/ws/dashboard',
      token: () => 'jwt-tok',
      onMessage: vi.fn(),
    });
    client.connect();
    const ws = FakeWebSocket.instances[0];
    ws.emitOpen();
    expect(ws.sent).toEqual([JSON.stringify({ type: 'auth', token: 'jwt-tok' })]);
  });

  it('dispatches parsed messages to onMessage', () => {
    const onMessage = vi.fn();
    const client = createDashboardWsClient({
      url: 'ws://host/ws/dashboard',
      token: () => 'tok',
      onMessage,
    });
    client.connect();
    const ws = FakeWebSocket.instances[0];
    ws.emitMessage(JSON.stringify({ type: 'notification', notificationId: 'N1' }));
    expect(onMessage).toHaveBeenCalledWith({ type: 'notification', notificationId: 'N1' });
  });

  it('ignores malformed frames without throwing', () => {
    const onMessage = vi.fn();
    const client = createDashboardWsClient({
      url: 'ws://host/ws/dashboard',
      token: () => 'tok',
      onMessage,
    });
    client.connect();
    const ws = FakeWebSocket.instances[0];
    expect(() => ws.emitMessage('not-json')).not.toThrow();
    expect(onMessage).not.toHaveBeenCalled();
  });

  it('without a token, triggers fallback and closes instead of sending auth', () => {
    const onFallback = vi.fn();
    const client = createDashboardWsClient({
      url: 'ws://host/ws/dashboard',
      token: () => null,
      onMessage: vi.fn(),
      onFallback,
    });
    client.connect();
    const ws = FakeWebSocket.instances[0];
    ws.emitOpen();
    expect(ws.sent).toHaveLength(0);
    expect(ws.closed).toBe(true);
    expect(onFallback).toHaveBeenCalled();
  });

  it('reconnects with backoff after an unexpected close and calls onFallback', () => {
    vi.useFakeTimers();
    const onFallback = vi.fn();
    const client = createDashboardWsClient({
      url: 'ws://host/ws/dashboard',
      token: () => 'tok',
      onMessage: vi.fn(),
      onFallback,
    });
    client.connect();
    const ws = FakeWebSocket.instances[0];
    ws.emitOpen();
    // 服务端意外断开
    ws.onclose?.();
    expect(onFallback).toHaveBeenCalled();
    expect(FakeWebSocket.instances).toHaveLength(1);
    vi.advanceTimersByTime(1000); // 首次退避 1000ms
    expect(FakeWebSocket.instances).toHaveLength(2); // 已重连
  });

  it('disconnect prevents any reconnect', () => {
    vi.useFakeTimers();
    const client = createDashboardWsClient({
      url: 'ws://host/ws/dashboard',
      token: () => 'tok',
      onMessage: vi.fn(),
    });
    client.connect();
    const ws = FakeWebSocket.instances[0];
    client.disconnect();
    expect(ws.closed).toBe(true);
    vi.advanceTimersByTime(200_000);
    expect(FakeWebSocket.instances).toHaveLength(1); // 未重连
  });

  it('on WebSocket construction failure, calls fallback and schedules reconnect', () => {
    vi.useFakeTimers();
    let constructCount = 0;
    vi.stubGlobal('WebSocket', function (this: unknown, _url: string) {
      constructCount += 1;
      throw new Error('network down');
    } as unknown as typeof WebSocket);
    const onFallback = vi.fn();
    const client = createDashboardWsClient({
      url: 'ws://host/ws/dashboard',
      token: () => 'tok',
      onMessage: vi.fn(),
      onFallback,
    });
    client.connect();
    expect(constructCount).toBe(1);
    expect(onFallback).toHaveBeenCalledTimes(1);
    vi.advanceTimersByTime(1000);
    expect(constructCount).toBe(2); // 退避后重试构造
  });

  it('caps reconnect backoff at maxReconnectDelayMs', () => {
    vi.useFakeTimers();
    const client = createDashboardWsClient({
      url: 'ws://host/ws/dashboard',
      token: () => 'tok',
      onMessage: vi.fn(),
      maxReconnectDelayMs: 3000,
    });
    client.connect();
    FakeWebSocket.instances[0].onclose?.(); // 退避 1000
    vi.advanceTimersByTime(1000);
    FakeWebSocket.instances[1].onclose?.(); // 退避 2000
    vi.advanceTimersByTime(2000);
    FakeWebSocket.instances[2].onclose?.(); // 4000→封顶 3000
    vi.advanceTimersByTime(2999);
    expect(FakeWebSocket.instances).toHaveLength(3); // 尚未到 3000
    vi.advanceTimersByTime(1);
    expect(FakeWebSocket.instances).toHaveLength(4); // 封顶 3000 触发重连
  });

  it('resets the backoff attempts counter after a successful connection', () => {
    vi.useFakeTimers();
    const client = createDashboardWsClient({
      url: 'ws://host/ws/dashboard',
      token: () => 'tok',
      onMessage: vi.fn(),
    });
    client.connect();
    FakeWebSocket.instances[0].emitOpen(); // attempts→0
    FakeWebSocket.instances[0].onclose?.(); // 退避 1000
    vi.advanceTimersByTime(1000);
    expect(FakeWebSocket.instances).toHaveLength(2);
    FakeWebSocket.instances[1].emitOpen(); // 再次成功 → attempts 重置
    FakeWebSocket.instances[1].onclose?.(); // 退避应仍为 1000（已重置）
    vi.advanceTimersByTime(999);
    expect(FakeWebSocket.instances).toHaveLength(2);
    vi.advanceTimersByTime(1);
    expect(FakeWebSocket.instances).toHaveLength(3);
  });

  it('disconnect cancels a pending reconnect timer', () => {
    vi.useFakeTimers();
    const client = createDashboardWsClient({
      url: 'ws://host/ws/dashboard',
      token: () => 'tok',
      onMessage: vi.fn(),
    });
    client.connect();
    FakeWebSocket.instances[0].emitOpen();
    FakeWebSocket.instances[0].onclose?.(); // 调度重连
    expect(FakeWebSocket.instances).toHaveLength(1);
    client.disconnect();
    vi.advanceTimersByTime(5000);
    expect(FakeWebSocket.instances).toHaveLength(1); // 待重连被取消
  });

  it('resolveDashboardWsUrl derives wss from an https API base', () => {
    vi.stubEnv('VITE_API_BASE_URL', 'https://api.example.com');
    expect(resolveDashboardWsUrl()).toBe('wss://api.example.com/ws/dashboard');
  });

  it('resolveDashboardWsUrl falls back to window.location when no API base', () => {
    vi.stubEnv('VITE_API_BASE_URL', '');
    const url = resolveDashboardWsUrl();
    expect(url).toMatch(/^wss?:\/\/.+\/ws\/dashboard$/);
  });
});
