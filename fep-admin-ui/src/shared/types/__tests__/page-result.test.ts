import { describe, it, expect } from 'vitest';
import type { PageResult } from '../page-result';

describe('PageResult', () => {
  it('accepts non-empty page with backend field names', () => {
    const page: PageResult<string> = {
      records: ['a', 'b', 'c'],
      total: 25,
      pageNum: 1,
      pageSize: 10,
      totalPages: 3,
    };
    expect(page.records).toHaveLength(3);
    expect(page.total).toBe(25);
    expect(page.pageNum).toBe(1);
    expect(page.pageSize).toBe(10);
    expect(page.totalPages).toBe(3);
  });

  it('accepts empty page', () => {
    const empty: PageResult<number> = {
      records: [],
      total: 0,
      pageNum: 1,
      pageSize: 20,
      totalPages: 0,
    };
    expect(empty.records).toEqual([]);
    expect(empty.totalPages).toBe(0);
  });

  it('supports generic record shapes', () => {
    interface User {
      userId: string;
      userName: string;
    }
    const page: PageResult<User> = {
      records: [{ userId: 'u1', userName: 'alice' }],
      total: 1,
      pageNum: 1,
      pageSize: 10,
      totalPages: 1,
    };
    expect(page.records[0].userName).toBe('alice');
  });
});
