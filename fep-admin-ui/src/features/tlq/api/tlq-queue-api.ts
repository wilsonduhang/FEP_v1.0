import { httpClient } from '@/shared/http/client';
import type {
  TlqQueueBatchGenerateRequest,
  TlqQueueConfigCreateRequest,
  TlqQueueConfigResponse,
} from '../types';

/**
 * TLQ queue config REST client (PRD §5.7 / §3.1.2).
 *
 * <p>Endpoints under {@code /api/v1/tlq/queues} — see backend
 * {@code TlqQueueConfigController} (FR-WEB-TLQ-CFG).</p>
 *
 * <p>Contract notes (verified against backend):</p>
 * <ul>
 *   <li>{@code listByNode} returns {@code List<TlqQueueConfigResponse>} —
 *       no pagination on this endpoint.</li>
 *   <li>{@code batchGenerate} produces 9 standard queues per PRD §3.1.2 in
 *       a single transaction.</li>
 *   <li>{@code deleteQueue} is a physical delete; no soft-delete flag.</li>
 * </ul>
 */

const BASE = '/api/v1/tlq/queues';

export const tlqQueueApi = {
  /** POST /queues — create single queue. */
  createQueue: (request: TlqQueueConfigCreateRequest): Promise<TlqQueueConfigResponse> =>
    httpClient.post(BASE, request),

  /** POST /queues/batch-generate — generate 9 standard queues per PRD §3.1.2. */
  batchGenerate: (
    request: TlqQueueBatchGenerateRequest,
  ): Promise<TlqQueueConfigResponse[]> =>
    httpClient.post(`${BASE}/batch-generate`, request),

  /** GET /queues?nodeId — list queues by node (no pagination per backend contract). */
  listByNode: (nodeId: string): Promise<TlqQueueConfigResponse[]> =>
    httpClient.get(BASE, { params: { nodeId } }),

  /** DELETE /queues/{id} — physical delete. */
  deleteQueue: (queueId: string): Promise<void> =>
    httpClient.delete(`${BASE}/${queueId}`),
};
