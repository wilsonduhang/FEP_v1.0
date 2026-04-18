import { httpClient } from '@/shared/http/client';
import type { PageResult } from '@/shared/types/page-result';
import type { EnableDisableStatus } from '@/shared/types/enum-maps';

/**
 * Business Scene REST client.
 *
 * Contract aligns with backend SubBusinessSceneController (PRD §5.5.4,
 * FR-WEB-SUB-SCENE). Wire-contract notes (contract baseline):
 *  - `pushMethod` is a 2-value enum AUTO|MANUAL. There is NO SCHEDULE mode
 *    and NO cron field — scheduling will be layered on after P1.
 *  - `importTemplatePath` is a plain string file-path reference (no upload
 *    endpoint); required iff `pushMethod === 'MANUAL'`.
 *  - `toggleStatus` is a PATCH with NO body (server derives target state).
 */
export type ScenePushMethod = 'AUTO' | 'MANUAL';

/**
 * Create / update request body — mirrors BusinessSceneCreateRequest on the
 * backend. Same DTO is used for both POST (create) and PUT (update).
 */
export interface BusinessSceneRequest {
  /** 场景名称，3-30 字符 */
  sceneName: string;
  /** 业务类型 ID，必填 */
  businessTypeId: string;
  /** 推送方式：AUTO（自动）/ MANUAL（手动） */
  pushMethod: ScenePushMethod;
  /** 导入模板路径（纯字符串引用）；MANUAL 模式必填，AUTO 模式应为空 */
  importTemplatePath?: string | null;
  /** 请求地址，http(s) URL，必填 */
  requestUrl: string;
  /** 排序值，整数 */
  sortOrder: number;
}

/** Full response record returned by backend. */
export interface BusinessSceneResponse extends BusinessSceneRequest {
  sceneId: string;
  sceneStatus: EnableDisableStatus;
  createTime: string;
  updateTime: string;
}

/** Search query params. */
export interface BusinessSceneSearchParams {
  pageNum: number;
  pageSize: number;
  keyword?: string;
  businessTypeId?: string;
}

const BASE = '/api/v1/submission/scenes';

export const subBusinessSceneApi = {
  search: (params: BusinessSceneSearchParams): Promise<PageResult<BusinessSceneResponse>> =>
    httpClient.get(BASE, { params }),
  getById: (id: string): Promise<BusinessSceneResponse> => httpClient.get(`${BASE}/${id}`),
  create: (req: BusinessSceneRequest): Promise<BusinessSceneResponse> => httpClient.post(BASE, req),
  update: (id: string, req: BusinessSceneRequest): Promise<BusinessSceneResponse> =>
    httpClient.put(`${BASE}/${id}`, req),
  /**
   * Toggle ENABLED↔DISABLED. Backend derives the target state, so the client
   * MUST NOT send a body (PATCH is called single-arg).
   */
  toggleStatus: (id: string): Promise<BusinessSceneResponse> =>
    httpClient.patch(`${BASE}/${id}/status`),
  remove: (id: string): Promise<void> => httpClient.delete(`${BASE}/${id}`),
};
