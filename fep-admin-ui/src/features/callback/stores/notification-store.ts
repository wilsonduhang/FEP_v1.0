import { defineStore } from 'pinia';
import {
  callbackNotificationApi,
  type CallbackNotificationResponse,
} from '../api/callbackNotification';

const POLL_INTERVAL_MS = 30_000;

interface State {
  unread: number;
  items: CallbackNotificationResponse[];
  timer: number | null;
}

export const useNotificationStore = defineStore('callbackNotification', {
  state: (): State => ({ unread: 0, items: [], timer: null }),
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
  },
});
