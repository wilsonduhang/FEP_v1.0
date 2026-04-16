import { httpClient } from '@/shared/http/client';
import type { PageResult } from '@/shared/types/page-result';

export type MessageDirection = 'OUTBOUND' | 'INBOUND' | 'BIDIRECTIONAL';
export type EnableDisableStatus = 'ENABLED' | 'DISABLED';

export interface DefinitionCreateRequest {
  messageCode: string; // @NotBlank @Pattern \d{1,5}
  messageName: string; // @NotBlank @Size min=2 max=200
  direction: MessageDirection; // @NotNull
  businessTypeId?: string;
  /**
   * v2 fix P1-6: backend field is `int` primitive, not Integer.
   * Omitting in JSON causes Jackson to default to 0. Declared as
   * required to match the wire contract; frontend form supplies 0
   * when user leaves blank.
   */
  fieldCount: number;
  fieldSummary?: string;
  sampleXml?: string;
  /** v2 fix P1-6: int primitive, default 0, declared required. */
  sortOrder: number;
}

export interface DefinitionUpdateRequest {
  messageCode?: string;
  messageName?: string;
  direction?: MessageDirection;
  businessTypeId?: string;
  fieldCount?: number;
  fieldSummary?: string;
  sampleXml?: string;
  sortOrder?: number;
}

export interface DefinitionResponse {
  definitionId: string;
  messageCode: string;
  messageName: string;
  businessTypeId: string | null;
  direction: MessageDirection;
  fieldCount: number;
  fieldSummary: string | null;
  sampleXml: string | null;
  definitionStatus: EnableDisableStatus;
  sortOrder: number;
  createTime: string;
  updateTime: string;
}

/**
 * v2 note: messageCode / direction / definitionStatus are supported by the
 * backend only after Task 0b extends BizMessageDefinitionController.search
 * (see META-P7.2A-PREP-02). Task 7 depends on Task 0b having landed first.
 */
export interface DefinitionSearchParams {
  pageNum: number;
  pageSize: number;
  keyword?: string;
  messageCode?: string;
  direction?: MessageDirection;
  definitionStatus?: EnableDisableStatus;
}

const BASE = '/api/v1/bizdata/definitions';

export const bizMessageDefinitionApi = {
  search: (p: DefinitionSearchParams): Promise<PageResult<DefinitionResponse>> =>
    httpClient.get(BASE, { params: p }),
  getById: (id: string): Promise<DefinitionResponse> => httpClient.get(`${BASE}/${id}`),
  create: (req: DefinitionCreateRequest): Promise<DefinitionResponse> => httpClient.post(BASE, req),
  update: (id: string, req: DefinitionUpdateRequest): Promise<DefinitionResponse> =>
    httpClient.put(`${BASE}/${id}`, req),
  toggleStatus: (id: string): Promise<DefinitionResponse> =>
    httpClient.put(`${BASE}/${id}/toggle-status`),
  delete: (id: string): Promise<void> => httpClient.delete(`${BASE}/${id}`),
};
