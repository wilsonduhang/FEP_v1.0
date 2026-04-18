import { httpClient } from '@/shared/http/client';
import type { PageResult } from '@/shared/types/page-result';
import type { EnableDisableStatus } from '@/shared/types/enum-maps';

/**
 * Data Source REST client.
 *
 * Contract aligns with backend SubDataSourceController (PRD §5.5.3,
 * FR-WEB-SUB-SRC). Wire-contract notes:
 *  - No toggleStatus API (contract baseline — unlike output interfaces).
 *  - Delete uses a client-side ElMessageBox confirm (caller responsibility).
 */

/**
 * Create / update request body — mirrors DataSourceCreateRequest on the
 * backend. Same DTO is used for both POST (create) and PUT (update).
 */
export interface DataSourceRequest {
  /** 数据源名称，1-30 字符 */
  sourceName: string;
  /** Logo 路径（可空） */
  logoPath?: string | null;
  /** 联系地址，1-50 字符 */
  contactAddress: string;
  /** 联系电话，1-11 位数字 */
  contactPhone: string;
  /** 是否启用推送 */
  pushEnabled: boolean;
  /** 内容类型（可空） */
  contentType?: string | null;
  /** 客户端 ID（可空） */
  clientId?: string | null;
}

/** Full response record returned by backend. */
export interface DataSourceResponse extends DataSourceRequest {
  sourceId: string;
  sourceStatus: EnableDisableStatus;
  createTime: string;
  updateTime: string;
}

/** Search query params. */
export interface DataSourceSearchParams {
  pageNum: number;
  pageSize: number;
  keyword?: string;
}

const BASE = '/api/v1/submission/data-sources';

export const subDataSourceApi = {
  search: (params: DataSourceSearchParams): Promise<PageResult<DataSourceResponse>> =>
    httpClient.get(BASE, { params }),
  getById: (id: string): Promise<DataSourceResponse> => httpClient.get(`${BASE}/${id}`),
  create: (req: DataSourceRequest): Promise<DataSourceResponse> => httpClient.post(BASE, req),
  update: (id: string, req: DataSourceRequest): Promise<DataSourceResponse> =>
    httpClient.put(`${BASE}/${id}`, req),
  remove: (id: string): Promise<void> => httpClient.delete(`${BASE}/${id}`),
};
