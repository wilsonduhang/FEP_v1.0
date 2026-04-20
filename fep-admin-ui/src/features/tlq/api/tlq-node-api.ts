import { httpClient } from '@/shared/http/client';
import type { PageResult } from '@/shared/types/page-result';
import type {
  TlqNodeCreateRequest,
  TlqNodeResponse,
  TlqNodeRole,
  TlqNodeStatus,
  TlqNodeUpdateRequest,
} from '../types';
import { toZeroBasedPage } from './paging';

/**
 * TLQ node management REST client (PRD §5.7).
 *
 * <p>Endpoints under {@code /api/v1/tlq/nodes} — see backend
 * {@code TlqNodeController} (FR-WEB-TLQ).</p>
 *
 * <p>Contract notes (verified against backend):</p>
 * <ul>
 *   <li>{@code listNodes} uses {@code page} (0-based) + {@code size} on the
 *       request side; adapter converts from UI {@code pageNum} (1-based).
 *       The returned {@code PageResult.pageNum} is 1-based — passthrough.</li>
 *   <li>{@code changeStatus} PATCH body is {@code null}; {@code target} is a
 *       {@code @RequestParam} query, not body.</li>
 *   <li>{@code updateNode} performs partial update; {@code nodeRole} is not
 *       modifiable by backend contract.</li>
 *   <li>{@code deleteNode} returns HTTP 409 if the node has associated queues.</li>
 * </ul>
 */

const BASE = '/api/v1/tlq/nodes';

/**
 * Page size used when a page needs the full node list (≤ 1000 nodes
 * assumed adequate for TLQ deployment; larger deployments should
 * paginate or filter). Used by {@code TlqQueuesPage} +
 * {@code TlqConnectivityPage} node selectors.
 */
export const ALL_NODES_PAGE_SIZE = 1000;

/** Query parameters for {@link tlqNodeApi.listNodes}. */
export interface NodeListParams {
  /** 1-based page number (converted to 0-based on the wire). */
  pageNum: number;
  /** Page size. */
  pageSize: number;
  /** Optional role filter. */
  role?: TlqNodeRole;
  /** Optional status filter. */
  status?: TlqNodeStatus;
}

export const tlqNodeApi = {
  /** POST /nodes — create new node (nodeRole required on create). */
  createNode: (request: TlqNodeCreateRequest): Promise<TlqNodeResponse> =>
    httpClient.post(BASE, request),

  /** GET /nodes/{id} — fetch single node. */
  getNode: (nodeId: string): Promise<TlqNodeResponse> =>
    httpClient.get(`${BASE}/${nodeId}`),

  /**
   * GET /nodes — paginated list.
   *
   * <p>UI uses {@code pageNum} (1-based); backend uses {@code page} (0-based).
   * Adapter converts outbound only; response {@code PageResult.pageNum} is
   * passthrough (1-based per {@code PageResult} contract).</p>
   */
  listNodes: (params: NodeListParams): Promise<PageResult<TlqNodeResponse>> => {
    const query: Record<string, unknown> = {
      page: toZeroBasedPage(params.pageNum),
      size: params.pageSize,
    };
    if (params.role !== undefined) {
      query.role = params.role;
    }
    if (params.status !== undefined) {
      query.status = params.status;
    }
    return httpClient.get(BASE, { params: query });
  },

  /** PUT /nodes/{id} — partial update; nodeRole not modifiable (backend contract). */
  updateNode: (nodeId: string, request: TlqNodeUpdateRequest): Promise<TlqNodeResponse> =>
    httpClient.put(`${BASE}/${nodeId}`, request),

  /** DELETE /nodes/{id} — physical delete; returns 409 if node has associated queues. */
  deleteNode: (nodeId: string): Promise<void> =>
    httpClient.delete(`${BASE}/${nodeId}`),

  /** PATCH /nodes/{id}/status?target=... — state machine transition. */
  changeStatus: (nodeId: string, target: TlqNodeStatus): Promise<TlqNodeResponse> =>
    httpClient.patch(`${BASE}/${nodeId}/status`, null, { params: { target } }),
};
