import { httpClient } from '@/shared/http/client';
import type { PageResult } from '@/shared/types/page-result';

/**
 * Output Interface REST client.
 *
 * Contract aligns with backend SubOutputInterfaceController (PRD §5.5.2,
 * FR-WEB-SUB-OUT). Key wire-contract notes:
 *  - `toggleStatus` is a PATCH with NO body (server toggles based on current state).
 *  - `test` POST returns a raw `Boolean` (not wrapped object); `true` = reachable.
 *  - `callCount` is a long on the backend; JS Number handles counts safely up to
 *    2^53 which is far beyond realistic invocation counts.
 */
export type InterfaceAuthType = 'TOKEN' | 'OAUTH2' | 'NONE';
export type EnableDisableStatus = 'ENABLED' | 'DISABLED';

/**
 * Create / update request body — mirrors OutputInterfaceCreateRequest on the
 * backend. Same DTO is used for both POST (create) and PUT (update).
 */
export interface OutputInterfaceRequest {
  /** 接口名称，1-30 字符 */
  interfaceName: string;
  /** 接口地址，http(s) URL */
  interfaceUrl: string;
  /** 关联业务类型 ID（可空） */
  businessTypeId?: string | null;
  /** 鉴权类型，必填 */
  authType: InterfaceAuthType;
  /** 超时秒数，1-300 */
  timeoutSeconds: number;
  /** 重试次数，0-10 */
  retryCount: number;
}

/** Full response record returned by backend. */
export interface OutputInterfaceResponse extends OutputInterfaceRequest {
  interfaceId: string;
  interfaceStatus: EnableDisableStatus;
  lastCallTime?: string | null;
  callCount: number;
  createTime: string;
  updateTime: string;
}

/** Search query params. */
export interface OutputInterfaceSearchParams {
  pageNum: number;
  pageSize: number;
  keyword?: string;
}

const BASE = '/api/v1/submission/output-interfaces';

export const subOutputInterfaceApi = {
  search: (params: OutputInterfaceSearchParams): Promise<PageResult<OutputInterfaceResponse>> =>
    httpClient.get(BASE, { params }),
  getById: (id: string): Promise<OutputInterfaceResponse> => httpClient.get(`${BASE}/${id}`),
  create: (req: OutputInterfaceRequest): Promise<OutputInterfaceResponse> =>
    httpClient.post(BASE, req),
  update: (id: string, req: OutputInterfaceRequest): Promise<OutputInterfaceResponse> =>
    httpClient.put(`${BASE}/${id}`, req),
  /**
   * Toggle ENABLED↔DISABLED. Backend derives the target state, so the client
   * MUST NOT send a body (PATCH is called single-arg).
   */
  toggleStatus: (id: string): Promise<OutputInterfaceResponse> =>
    httpClient.patch(`${BASE}/${id}/status`),
  /** Connectivity test; returns a bare boolean (Mock mode until P1 TLQ). */
  test: (id: string): Promise<boolean> => httpClient.post(`${BASE}/${id}/test`),
  remove: (id: string): Promise<void> => httpClient.delete(`${BASE}/${id}`),
};
