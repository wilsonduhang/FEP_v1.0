import { describe, it, expect } from 'vitest';
import { isSuccess, SUCCESS_CODE, type ApiResult } from '../api-result';

describe('ApiResult', () => {
  it('isSuccess returns true and narrows data when code is SUCCESS and data non-null', () => {
    const ok: ApiResult<string> = { code: SUCCESS_CODE, message: 'ok', data: 'hello' };
    expect(isSuccess(ok)).toBe(true);
    if (isSuccess(ok)) {
      const narrowed: string = ok.data;
      expect(narrowed).toBe('hello');
    }
  });

  it('isSuccess returns false when code is error', () => {
    const err: ApiResult<string> = { code: 'ERR_AUTH_001', message: 'bad', data: null };
    expect(isSuccess(err)).toBe(false);
  });

  it('isSuccess returns false when data is null even if code is SUCCESS', () => {
    const blank: ApiResult<string> = { code: SUCCESS_CODE, message: 'ok', data: null };
    expect(isSuccess(blank)).toBe(false);
  });
});
