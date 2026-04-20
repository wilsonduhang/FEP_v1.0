import { beforeEach, describe, expect, it, vi } from 'vitest';
import { httpClient } from '@/shared/http/client';
import { tlqConnectivityApi } from '../tlq-connectivity-api';
import type {
  ConnectivityRecordResponse,
  ConnectivitySummaryResponse,
  ConnectivityTestResponse,
} from '../../types';

vi.mock('@/shared/http/client', () => ({
  httpClient: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

const mockGet = vi.mocked(httpClient.get);
const mockPost = vi.mocked(httpClient.post);

beforeEach(() => {
  mockGet.mockReset();
  mockPost.mockReset();
});

describe('tlqConnectivityApi', () => {
  it('triggerTest POSTs to /connectivity/{nodeId}/test', async () => {
    mockPost.mockResolvedValue({
      recordId: 'R1',
      nodeId: 'N1',
      result: 'SUCCESS',
      rttMs: 42,
      message: 'ok',
      testTime: '2026-04-20T10:00:00',
    } as ConnectivityTestResponse);
    const result = await tlqConnectivityApi.triggerTest('N1');
    expect(mockPost).toHaveBeenCalledWith('/api/v1/tlq/connectivity/N1/test');
    expect(result.result).toBe('SUCCESS');
  });

  it('listRecords converts pageNum=1 to page=0 on outbound request', async () => {
    // Mock response uses distinct pageNum (999) to prevent tautological passthrough.
    mockGet.mockResolvedValue({
      records: [],
      total: 0,
      pageNum: 999,
      pageSize: 999,
      totalPages: 0,
    });
    await tlqConnectivityApi.listRecords('N1', { pageNum: 1, pageSize: 20 });
    expect(mockGet).toHaveBeenCalledWith('/api/v1/tlq/connectivity/N1/records', {
      params: { page: 0, size: 20 },
    });
  });

  it('listRecords passes through backend PageResult.pageNum (1-based) unchanged', async () => {
    mockGet.mockResolvedValue({
      records: [{ recordId: 'R1' } as ConnectivityRecordResponse],
      total: 1,
      pageNum: 2,
      pageSize: 20,
      totalPages: 3,
    });
    const result = await tlqConnectivityApi.listRecords('N1', {
      pageNum: 2,
      pageSize: 20,
    });
    expect(result.pageNum).toBe(2);
    expect(result.totalPages).toBe(3);
    expect(result.records).toHaveLength(1);
  });

  it('getSummary GETs /connectivity/{nodeId}/summary', async () => {
    mockGet.mockResolvedValue({
      nodeId: 'N1',
      lastResult: 'SUCCESS',
      lastTestTime: '2026-04-20T10:00:00',
      totalTests: 100,
      successCount: 98,
      successRate: 98.0,
    } as ConnectivitySummaryResponse);
    const result = await tlqConnectivityApi.getSummary('N1');
    expect(mockGet).toHaveBeenCalledWith('/api/v1/tlq/connectivity/N1/summary');
    expect(result.successRate).toBe(98.0);
  });
});
