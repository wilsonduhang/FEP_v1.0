import { httpClient } from '@/shared/http/client';
import type { PageResult } from '@/shared/types/page-result';

export type QueryType = 'REALTIME' | 'BATCH';
export type QueryTaskStatus = 'DRAFT' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
export type ResultStatus = 'NORMAL' | 'ERROR';

/** Mirrors QueryTaskCreateRequest in com.puchain.fep.web.entquery.task.dto */
export interface QueryTaskCreateRequest {
  enterpriseId: string; // @NotBlank
  queryType: QueryType; // @NotBlank @Pattern REALTIME|BATCH
  usci: string; // @NotBlank @Pattern ^[0-9A-Z]{18}$
  queryTargetName?: string; // @Size max=200
  batchFilePath?: string; // @Size max=500
}

/** Mirrors QueryTaskResponse */
export interface QueryTaskResponse {
  taskId: string;
  enterpriseId: string;
  queryType: QueryType;
  usci: string;
  queryTargetName: string | null;
  taskStatus: QueryTaskStatus;
  messageId: string | null;
  batchFilePath: string | null;
  resultSummary: string | null;
  errorMessage: string | null;
  createTime: string;
  updateTime: string;
  completeTime: string | null;
}

/** Mirrors QueryResultResponse */
export interface QueryResultResponse {
  resultId: string;
  taskId: string;
  resultUsci: string;
  enterpriseName: string | null;
  resultData: string | null;
  resultStatus: ResultStatus;
  errorCode: string | null;
  errorMessage: string | null;
  createTime: string;
}

export interface QueryTaskSearchParams {
  pageNum: number;
  pageSize: number;
  queryType?: QueryType;
  taskStatus?: QueryTaskStatus;
  keyword?: string;
}

const BASE = '/api/v1/ent-query/tasks';

export const entQueryTaskApi = {
  search: (p: QueryTaskSearchParams): Promise<PageResult<QueryTaskResponse>> =>
    httpClient.get(BASE, { params: p }),
  getById: (taskId: string): Promise<QueryTaskResponse> => httpClient.get(`${BASE}/${taskId}`),
  create: (req: QueryTaskCreateRequest): Promise<QueryTaskResponse> => httpClient.post(BASE, req),
  execute: (taskId: string): Promise<QueryTaskResponse> =>
    httpClient.post(`${BASE}/${taskId}/execute`),
  delete: (taskId: string): Promise<void> => httpClient.delete(`${BASE}/${taskId}`),
  listResults: (taskId: string): Promise<QueryResultResponse[]> =>
    httpClient.get(`${BASE}/${taskId}/results`),
  getResult: (taskId: string, resultId: string): Promise<QueryResultResponse> =>
    httpClient.get(`${BASE}/${taskId}/results/${resultId}`),
};
