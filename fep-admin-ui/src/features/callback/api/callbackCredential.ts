import { httpClient } from '@/shared/http/client';

export type CallbackAuthType = 'TOKEN' | 'OAUTH2';

export interface CallbackCredentialResponse {
  credentialId: string;
  interfaceId: string;
  authType: CallbackAuthType;
  tokenHeader: string | null;
  oauthTokenEndpoint: string | null;
  oauthScope: string | null;
  createTime: string;
  updateTime: string;
  rotatedAt: string | null;
  tokenConfigured: boolean;
  oauthClientIdConfigured: boolean;
  oauthClientSecretConfigured: boolean;
}

export interface CallbackCredentialCreateRequest {
  interfaceId: string;
  authType: CallbackAuthType;
  token?: string;
  tokenHeader?: string;
  oauthClientId?: string;
  oauthClientSecret?: string;
  oauthTokenEndpoint?: string;
  oauthScope?: string;
}

export type CallbackCredentialUpdateRequest = Omit<
  CallbackCredentialCreateRequest,
  'interfaceId' | 'authType'
>;

const BASE = '/api/v1/callback/credentials';

export const callbackCredentialApi = {
  list: (): Promise<CallbackCredentialResponse[]> => httpClient.get(BASE),
  get: (interfaceId: string): Promise<CallbackCredentialResponse> =>
    httpClient.get(`${BASE}/${interfaceId}`),
  create: (req: CallbackCredentialCreateRequest): Promise<CallbackCredentialResponse> =>
    httpClient.post(BASE, req),
  update: (
    interfaceId: string,
    req: CallbackCredentialUpdateRequest,
  ): Promise<CallbackCredentialResponse> => httpClient.put(`${BASE}/${interfaceId}`, req),
  delete: (interfaceId: string): Promise<void> => httpClient.delete(`${BASE}/${interfaceId}`),
};
