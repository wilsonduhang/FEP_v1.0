import { httpClient } from '@/shared/http/client';
import type { PageResult } from '@/shared/types/page-result';
import type { MessageDirection } from './biz-message-definition-api';

export type MessageProcessStatus = 'PENDING' | 'PROCESSING' | 'SUCCESS' | 'FAILED';
export type EntryMethod = 'API' | 'MANUAL';

export interface RecordCreateRequest {
  messageCode: string; // @NotBlank @Pattern \d{1,5}
  serialNo: string; // @NotBlank @Size max=50
  direction: MessageDirection; // @NotNull
  senderNode?: string;
  receiverNode?: string;
  businessNo?: string;
  amount?: string; // BigDecimal serialized as string
  xmlContent?: string;
}

export interface RecordResponse {
  recordId: string;
  messageCode: string;
  serialNo: string;
  senderNode: string | null;
  receiverNode: string | null;
  direction: MessageDirection;
  processStatus: MessageProcessStatus;
  businessNo: string | null;
  amount: string | null;
  xmlContent: string | null;
  entryMethod: EntryMethod;
  accessCount: number;
  errorMessage: string | null;
  processTime: string | null;
  createTime: string;
  updateTime: string;
}

export interface RecordSummaryItem {
  messageCode: string;
  messageName: string;
  totalCount: number;
  successCount: number;
  pendingCount: number;
  failedCount: number;
}

export interface RecordSearchParams {
  pageNum: number;
  pageSize: number;
  messageCode?: string;
  status?: MessageProcessStatus;
  direction?: MessageDirection;
  startDate?: string;
  endDate?: string;
}

const BASE = '/api/v1/bizdata/records';

export const bizMessageRecordApi = {
  search: (p: RecordSearchParams): Promise<PageResult<RecordResponse>> =>
    httpClient.get(BASE, { params: p }),
  getSummary: (): Promise<RecordSummaryItem[]> => httpClient.get(`${BASE}/summary`),
  getById: (id: string): Promise<RecordResponse> => httpClient.get(`${BASE}/${id}`),
  create: (req: RecordCreateRequest): Promise<RecordResponse> => httpClient.post(BASE, req),
  resubmit: (id: string): Promise<RecordResponse> => httpClient.post(`${BASE}/${id}/resubmit`),
  exportRecords: (p: RecordSearchParams): Promise<string> =>
    httpClient.post(`${BASE}/export`, null, { params: p }),
};
