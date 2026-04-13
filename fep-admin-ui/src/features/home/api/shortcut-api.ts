import { httpClient } from '@/shared/http/client';

/**
 * Per-user shortcut entry on the home dashboard.
 *
 * <p>Aligned with backend DTO {@code ShortcutResponse}; see PRD v1.3 §5.2.4.</p>
 */
export interface ShortcutResponse {
  shortcutId: string;
  userId: string;
  shortcutName: string;
  targetUrl: string;
  icon: string;
  sortOrder: number;
  visible: boolean;
  createTime: string;
  updateTime: string;
}

/**
 * Payload for creating a new shortcut.
 */
export interface ShortcutCreateRequest {
  shortcutName: string;
  targetUrl: string;
  icon: string;
  sortOrder: number;
}

/**
 * Single reorder item, aligned with backend nested DTO
 * {@code ShortcutReorderRequest.ReorderItem}.
 */
export interface ShortcutReorderItem {
  shortcutId: string;
  sortOrder: number;
}

/**
 * Payload for bulk-reordering shortcuts.
 *
 * <p>Field name {@code items} is load-bearing: backend
 * {@code ShortcutReorderRequest} reads {@code getItems()}.</p>
 */
export interface ShortcutReorderRequest {
  items: ShortcutReorderItem[];
}

export const shortcutApi = {
  list: (): Promise<ShortcutResponse[]> =>
    httpClient.get('/api/v1/dashboard/shortcuts'),
  create: (req: ShortcutCreateRequest): Promise<ShortcutResponse> =>
    httpClient.post('/api/v1/dashboard/shortcuts', req),
  reorder: (req: ShortcutReorderRequest): Promise<void> =>
    httpClient.put('/api/v1/dashboard/shortcuts/reorder', req),
  toggleVisibility: (shortcutId: string): Promise<ShortcutResponse> =>
    httpClient.put(`/api/v1/dashboard/shortcuts/${shortcutId}/toggle-visibility`),
  delete: (shortcutId: string): Promise<void> =>
    httpClient.delete(`/api/v1/dashboard/shortcuts/${shortcutId}`),
};
