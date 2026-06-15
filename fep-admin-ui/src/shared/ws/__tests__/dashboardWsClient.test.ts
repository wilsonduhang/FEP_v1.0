import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { createDashboardWsClient, resolveDashboardWsUrl } from '../dashboardWsClient';

/** 可控的 WebSocket 替身。 */
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
