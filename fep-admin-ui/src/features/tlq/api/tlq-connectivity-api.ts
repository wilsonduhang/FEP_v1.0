import { httpClient } from '@/shared/http/client';
import type { PageResult } from '@/shared/types/page-result';
import type {
  ConnectivityRecordResponse,
  ConnectivitySummaryResponse,
  ConnectivityTestResponse,
} from '../types';
import { toZeroBasedPage } from './paging';

/**
 * TLQ connectivity test REST client (PRD §5.7.5).
 *
 * <p>Endpoints under {@code /api/v1/tlq/connectivity} — see backend
 * {@code TlqConnectivityController} (FR-WEB-TLQ-HB).</p>
 *
 * <p>Contract notes (verified against backend):</p>
 * <ul>
 *   <li>{@code triggerTest} is a reserved shell; returns a mock result until
 *       the P1 TLQ SDK (9005 heartbeat) is integrated.</li>
 *   <li>{@code listRecords} uses {@code page} (0-based) + {@code size} on the
 *       request side; adapter converts from UI {@code pageNum} (1-based).
 *       Response {@code PageResult.pageNum} is 1-based — passthrough.</li>
 *   <li>{@code getSummary} returns success rate (0 – 100.0) and last test stats.</li>
 * </ul>
 */

const BASE = '/api/v1/tlq/connectivity';

/** Query parameters for {@link tlqConnectivityApi.listRecords}. */
export interface ConnectivityRecordParams {
  /** 1-based page number (converted to 0-based on the wire). */
  pageNum: number;
  /** Page size. */
  pageSize: number;
}

export const tlqConnectivityApi = {
  /** POST /{nodeId}/test — reserved shell; returns mock result until P1 TLQ SDK ready. */
  triggerTest: (nodeId: string): Promise<ConnectivityTestResponse> =>
    httpClient.post(`${BASE}/${nodeId}/test`),

  /**
   * GET /{nodeId}/records — paginated connectivity history.
   *
   * <p>Outbound {@code pageNum} → {@code page} (0-based); response passthrough.</p>
   */
  listRecords: (
    nodeId: string,
    params: ConnectivityRecordParams,
  ): Promise<PageResult<ConnectivityRecordResponse>> =>
    httpClient.get(`${BASE}/${nodeId}/records`, {
      params: {
        page: toZeroBasedPage(params.pageNum),
        size: params.pageSize,
      },
    }),

  /** GET /{nodeId}/summary — success rate + last test stats. */
  getSummary: (nodeId: string): Promise<ConnectivitySummaryResponse> =>
    httpClient.get(`${BASE}/${nodeId}/summary`),
};
