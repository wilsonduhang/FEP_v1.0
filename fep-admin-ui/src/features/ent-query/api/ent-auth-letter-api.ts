import { httpClient } from '@/shared/http/client';
import type { PageResult } from '@/shared/types/page-result';

export type AuthType = 'PAPER' | 'ELECTRONIC';
export type LetterStatus = 'DRAFT' | 'SUBMITTED' | 'ACKNOWLEDGED' | 'REJECTED';

/** Mirrors AuthLetterCreateRequest */
export interface AuthLetterCreateRequest {
  enterpriseId: string; // @NotBlank
  authType: AuthType; // @NotBlank @Pattern PAPER|ELECTRONIC
  authScope?: string; // @Size max=500
  authorizedUsci: string; // @NotBlank @Pattern ^[0-9A-Z]{18}$
  authorizedName?: string; // @Size max=200
  filePath?: string; // @Size max=500
}

/** Mirrors AuthLetterResponse */
export interface AuthLetterResponse {
  letterId: string;
  enterpriseId: string;
  authType: AuthType;
  authScope: string | null;
  authorizedUsci: string;
  authorizedName: string | null;
  filePath: string | null;
  letterStatus: LetterStatus;
  messageId: string | null;
  submitTime: string | null;
  ackTime: string | null;
  rejectReason: string | null;
  createTime: string;
  updateTime: string;
}

export interface AuthLetterSearchParams {
  pageNum: number;
  pageSize: number;
  letterStatus?: LetterStatus;
  authType?: AuthType;
  keyword?: string;
}

const BASE = '/api/v1/ent-query/auth-letters';

export const entAuthLetterApi = {
  search: (p: AuthLetterSearchParams): Promise<PageResult<AuthLetterResponse>> =>
    httpClient.get(BASE, { params: p }),
  getById: (letterId: string): Promise<AuthLetterResponse> => httpClient.get(`${BASE}/${letterId}`),
  create: (req: AuthLetterCreateRequest): Promise<AuthLetterResponse> => httpClient.post(BASE, req),
  update: (letterId: string, req: AuthLetterCreateRequest): Promise<AuthLetterResponse> =>
    httpClient.put(`${BASE}/${letterId}`, req),
  submit: (letterId: string): Promise<AuthLetterResponse> =>
    httpClient.post(`${BASE}/${letterId}/submit`),
  delete: (letterId: string): Promise<void> => httpClient.delete(`${BASE}/${letterId}`),
};
