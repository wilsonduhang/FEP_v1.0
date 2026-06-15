/**
 * Dashboard 实时告警 WebSocket 客户端（B-8，FR-WEB-DASH-REFRESH）。
 *
 * 单向 server→client 通知推送的浏览器侧封装：
 *  - 连接建立后发首帧 `{type:'auth', token}`（muzhou 签字方案 a；浏览器原生
 *    WebSocket 无法设 Authorization header）。
 *  - onmessage 解析 JSON 帧分发给 `onMessage`。
 *  - 断开/出错自动重连（指数退避，上限 `maxReconnectDelayMs` 默认 2min），并触发
 *    `onFallback` 让调用方回退既有轮询（WS 是增强非替代，断开不破坏现状）。
 *  - `disconnect()` 显式关闭，阻止后续重连。
 */
export interface DashboardWsClientOptions {
  /** WebSocket URL（ws:// 或 wss://）。 */
  readonly url: string;
  /** 取当前 JWT 的回调（连接时刻惰性获取，可返回 null）。 */
  readonly token: () => string | null;
  /** 收到（已解析的）服务端帧时回调。 */
  readonly onMessage: (data: unknown) => void;
  /** 连接断开/不可用时回调（调用方据此启用轮询兜底）。 */
  readonly onFallback?: () => void;
  /** 重连退避上限（毫秒），默认 120000（2 分钟）。 */
  readonly maxReconnectDelayMs?: number;
}

export interface DashboardWsClient {
  connect(): void;
  disconnect(): void;
}

const BASE_RECONNECT_DELAY_MS = 1000;
const DEFAULT_MAX_RECONNECT_DELAY_MS = 120_000;
const DASHBOARD_WS_PATH = '/ws/dashboard';

/**
 * 由 `VITE_API_BASE_URL`（绝对 http(s) 时）或当前 `window.location` 推导 WS URL，
 * http→ws / https→wss，拼接 `/ws/dashboard`（禁硬编码 host/协议）。
 */
export function resolveDashboardWsUrl(): string {
  const base = import.meta.env.VITE_API_BASE_URL as string | undefined;
  let protocol: string;
  let host: string;
  if (base && /^https?:\/\//i.test(base)) {
    const u = new URL(base);
    protocol = u.protocol;
    host = u.host;
  } else {
    protocol = window.location.protocol;
    host = window.location.host;
  }
  const wsProtocol = protocol === 'https:' ? 'wss:' : 'ws:';
  return `${wsProtocol}//${host}${DASHBOARD_WS_PATH}`;
}

/**
 * 创建一个 Dashboard WebSocket 客户端（未连接，需调用 connect）。
 *
 * @param options 客户端选项
 * @returns 客户端句柄
 */
export function createDashboardWsClient(options: DashboardWsClientOptions): DashboardWsClient {
  const maxDelay = options.maxReconnectDelayMs ?? DEFAULT_MAX_RECONNECT_DELAY_MS;
  let ws: WebSocket | null = null;
  let reconnectTimer: number | null = null;
  let attempts = 0;
  let closedByClient = false;

  const scheduleReconnect = (): void => {
    if (closedByClient || reconnectTimer !== null) {
      return;
    }
    const delay = Math.min(BASE_RECONNECT_DELAY_MS * 2 ** attempts, maxDelay);
    attempts += 1;
    reconnectTimer = window.setTimeout(open, delay);
  };

  function open(): void {
    reconnectTimer = null;
    let socket: WebSocket;
    try {
      socket = new WebSocket(options.url);
    } catch {
      options.onFallback?.();
      scheduleReconnect();
      return;
    }
    ws = socket;
    socket.onopen = (): void => {
      attempts = 0;
      const token = options.token();
      if (!token) {
        // 无凭证无法认证：交还轮询兜底并关闭（连接会被服务端超时清理）。
        options.onFallback?.();
        socket.close();
        return;
      }
      socket.send(JSON.stringify({ type: 'auth', token }));
    };
    socket.onmessage = (event: MessageEvent): void => {
      try {
        options.onMessage(JSON.parse(String(event.data)));
      } catch {
        // 畸形帧忽略，不影响连接。
      }
    };
    socket.onclose = (): void => {
      ws = null;
      options.onFallback?.();
      scheduleReconnect();
    };
    socket.onerror = (): void => {
      // 出错后浏览器会触发 onclose（重连在 onclose 统一处理）；确保 socket 关闭。
      socket.close();
    };
  }

  return {
    connect(): void {
      closedByClient = false;
      attempts = 0;
      open();
    },
    disconnect(): void {
      closedByClient = true;
      if (reconnectTimer !== null) {
        window.clearTimeout(reconnectTimer);
        reconnectTimer = null;
      }
      if (ws !== null) {
        ws.close();
        ws = null;
      }
    },
  };
}
