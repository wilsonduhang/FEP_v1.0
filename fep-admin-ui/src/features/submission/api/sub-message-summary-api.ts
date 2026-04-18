import { httpClient } from '@/shared/http/client';

/**
 * Submission Message Summary REST client.
 *
 * <p>Contract aligns with backend {@code SubMessageSummaryController}
 * (PRD §5.5.5, FR-WEB-SUB-LIST). Single endpoint:</p>
 * <ul>
 *   <li>{@code GET /api/v1/submission/message-summary} — per-message-type summary list</li>
 * </ul>
 *
 * <p>Each summary item groups by {@code messageType} and carries
 * {@code totalCount} / {@code pushedCount} / {@code pendingCount}.
 * Clicking a card in the UI navigates to the message records list
 * ({@code /report/records?messageType=...}) — delivered by P7.2c.</p>
 *
 * <p>Server-side counts are Java {@code long} but counts here are well below
 * {@link Number.MAX_SAFE_INTEGER} (2^53) so JS {@code number} is safe.</p>
 */

/** Per-message-type summary, from backend {@code MessageSummaryResponse}. */
export interface MessageSummaryItem {
  /** 报文类型代码 (e.g. "3001") */
  messageType: string;
  /** 报文类型中文名 (e.g. "查询请求") */
  messageName: string;
  /** 业务类型 ID (e.g. "BIZ001") */
  businessTypeId: string;
  /** 该报文类型总数 */
  totalCount: number;
  /** 已推送数 (push_status=PUSHED) */
  pushedCount: number;
  /** 待推送数. Backend derives via getter {@code totalCount - pushedCount}; Jackson auto-serialises. */
  pendingCount: number;
}

const BASE = '/api/v1/submission/message-summary';

export const subMessageSummaryApi = {
  /** Fetches per-message-type summary list. */
  getSummary: (): Promise<MessageSummaryItem[]> => httpClient.get(BASE),
};
