import { httpClient } from '@/shared/http/client';

export interface CallbackNotificationResponse {
  notificationId: string;
  category: string;
  level: string;
  title: string;
  message: string;
  refId: string | null;
  refType: string | null;
  read: boolean;
  createTime: string;
  readAt: string | null;
}

const BASE = '/api/v1/notifications';

export const callbackNotificationApi = {
  listUnread: (): Promise<CallbackNotificationResponse[]> => httpClient.get(`${BASE}/unread`),
  unreadCount: (): Promise<number> => httpClient.get(`${BASE}/unread/count`),
  markRead: (id: string): Promise<void> => httpClient.put(`${BASE}/${id}/read`),
};
