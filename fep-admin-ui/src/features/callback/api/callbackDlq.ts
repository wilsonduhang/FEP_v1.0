import { httpClient } from '@/shared/http/client';

export interface DlqEntryResponse {
  queueId: string;
  targetInterfaceId: string;
  msgNo: string;
  status: string;
  retryCount: number;
  lastError: string | null;
  updateTime: string;
  originalDlqId: string | null;
  replayedBy: string | null;
  replayedAt: string | null;
}

export interface DlqReplayResponse {
  newQueueId: string;
  originalDlqId: string;
  replayedAt: string;
}

export interface DlqListParams {
  page: number; // 0-based, 直传后端
  size: number;
}

const BASE = '/api/v1/callback/dlq';

export const callbackDlqApi = {
  list: (params: DlqListParams): Promise<DlqEntryResponse[]> => httpClient.get(BASE, { params }),
  replay: (dlqId: string): Promise<DlqReplayResponse> =>
    httpClient.post(`${BASE}/${dlqId}/replay`),
  chain: (dlqId: string): Promise<DlqEntryResponse[]> => httpClient.get(`${BASE}/${dlqId}/chain`),
};
