export const SUCCESS_CODE = 'SUCCESS';

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
