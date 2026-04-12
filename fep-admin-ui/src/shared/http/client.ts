import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios';
import { ElMessage } from 'element-plus';
import { TokenStorage } from './token-storage';
import { isSuccess, type ApiResult } from '@/shared/types/api-result';

/**
 * Shared axios instance used by all feature modules.
 *
 * Interceptors:
 *  - request: inject `Authorization: Bearer <token>` when TokenStorage has one.
 *  - response (success): unwrap `ApiResult<T>` → `T` on `code === 'SUCCESS'`;
 *    otherwise reject with the raw ApiResult body and surface ElMessage error.
 *  - response (error): on HTTP 401 or `ERR_AUTH_UNAUTHORIZED`, clear tokens and
 *    dispatch a `auth:expired` CustomEvent so the auth store can react without
 *    a direct router import (avoids circular dependency).
 */
export const httpClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
  timeout: 15000,
});

httpClient.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = TokenStorage.get();
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`);
  }
  return config;
});

httpClient.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResult<unknown>;
    if (body && typeof body === 'object' && 'code' in body) {
      if (isSuccess(body)) {
        return body.data;
      }
      ElMessage.error(body.message || '请求失败');
      return Promise.reject(body);
    }
    return response.data;
  },
  (error: AxiosError<ApiResult<unknown>>) => {
    const status = error.response?.status;
    const body = error.response?.data;
    const unauthorized = status === 401 || body?.code === 'ERR_AUTH_UNAUTHORIZED';
    if (unauthorized) {
      TokenStorage.clear();
      window.dispatchEvent(new CustomEvent('auth:expired'));
    }
    const message = body?.message || error.message || '网络错误';
    ElMessage.error(message);
    return Promise.reject(body ?? error);
  },
);
