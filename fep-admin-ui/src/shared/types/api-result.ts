/**
 * Success code returned by backend FepErrorCode.SUCCESS.
 * NOTE: backend uses "200" (HTTP status style), not "SUCCESS".
 * Aligned during P7.1 E2E smoke testing.
 */
export const SUCCESS_CODE = '200';

export interface ApiResult<T> {
  code: string;
  message: string;
  data: T | null;
}

export function isSuccess<T>(
  result: ApiResult<T>,
): result is ApiResult<T> & { data: NonNullable<T> } {
  return result.code === SUCCESS_CODE && result.data !== null && result.data !== undefined;
}
