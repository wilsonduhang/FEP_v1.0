import { defineStore } from 'pinia';
import { markRaw } from 'vue';
import {
  callbackNotificationApi,
  type CallbackNotificationResponse,
} from '../api/callbackNotification';
import { TokenStorage } from '@/shared/http/token-storage';
import {
  createDashboardWsClient,
  resolveDashboardWsUrl,
  type DashboardWsClient,
} from '@/shared/ws/dashboardWsClient';

const POLL_INTERVAL_MS = 30_000;

function isNotificationFrame(data: unknown): boolean {
  return (
    typeof data === 'object' &&
    data !== null &&
    (data as { type?: unknown }).type === 'notification'
  );
}

interface State {
  unread: number;
  items: CallbackNotificationResponse[];
  timer: number | null;
  wsClient: DashboardWsClient | null;
}

export const useNotificationStore = defineStore('callbackNotification', {
  state: (): State => ({ unread: 0, items: [], timer: null, wsClient: null }),
  actions: {
    async fetchCount() {
      this.unread = await callbackNotificationApi.unreadCount();
    },
    async fetchList() {
      this.items = await callbackNotificationApi.listUnread();
    },
    async markRead(id: string) {
      await callbackNotificationApi.markRead(id);
      this.items = this.items.filter((n) => n.notificationId !== id);
      await this.fetchCount();
    },
    startPolling() {
      if (this.timer !== null) return;
      void this.fetchCount();
      this.timer = window.setInterval(() => void this.fetchCount(), POLL_INTERVAL_MS);
    },
    stopPolling() {
      if (this.timer !== null) {
        window.clearInterval(this.timer);
        this.timer = null;
      }
    },
    /**
     * 订阅实时告警推送（B-8）：保留既有 30s 轮询作基线兜底，并叠加 WebSocket 实时层
     * （WS 是增强非替代——断开自动由轮询覆盖，不破坏现状）。收到 notification 帧即刷新
     * 未读计数与列表，使徽标秒级更新而非等 30s。幂等。
     */
    subscribeRealtime() {
      this.startPolling();
      if (this.wsClient !== null) return;
      this.wsClient = markRaw(
        createDashboardWsClient({
          url: resolveDashboardWsUrl(),
          token: () => TokenStorage.get(),
          onMessage: (data) => {
            if (isNotificationFrame(data)) {
              void this.fetchCount();
              void this.fetchList();
            }
          },
        }),
      );
      this.wsClient.connect();
    },
    /**
     * 取消实时订阅：断开 WebSocket（阻止重连）并停止轮询。
     */
    unsubscribeRealtime() {
      if (this.wsClient !== null) {
        this.wsClient.disconnect();
        this.wsClient = null;
      }
      this.stopPolling();
    },
  },
});
