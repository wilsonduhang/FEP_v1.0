import { describe, expect, it, vi, beforeEach } from 'vitest';
import { httpClient } from '@/shared/http/client';
import { bizMessageRecordApi } from '../biz-message-record-api';

vi.mock('@/shared/http/client', () => ({
  httpClient: {
    get: vi.fn(),
    post: vi.fn(),
  },
}));

describe('bizMessageRecordApi', () => {
  beforeEach(() => { vi.clearAllMocks(); });

  it('search calls GET /api/v1/bizdata/records with params', async () => {
    vi.mocked(httpClient.get).mockResolvedValue({
      records: [],
      total: 0,
      pageNum: 1,
      pageSize: 20,
      totalPages: 0,
    });
    await bizMessageRecordApi.search({ pageNum: 1, pageSize: 20, messageCode: '1001' });
    expect(httpClient.get).toHaveBeenCalledWith('/api/v1/bizdata/records', {
      params: { pageNum: 1, pageSize: 20, messageCode: '1001' },
    });
  });

  it('getSummary calls GET on /records/summary', async () => {
    vi.mocked(httpClient.get).mockResolvedValue([]);
    await bizMessageRecordApi.getSummary();
    expect(httpClient.get).toHaveBeenCalledWith('/api/v1/bizdata/records/summary');
  });

  it('getById calls GET with id in path', async () => {
    vi.mocked(httpClient.get).mockResolvedValue({});
    await bizMessageRecordApi.getById('R-001');
    expect(httpClient.get).toHaveBeenCalledWith('/api/v1/bizdata/records/R-001');
  });

  it('create calls POST with request body', async () => {
    vi.mocked(httpClient.post).mockResolvedValue({});
    await bizMessageRecordApi.create({
      messageCode: '1001',
      serialNo: 'SN-001',
      direction: 'OUTBOUND',
    });
    expect(httpClient.post).toHaveBeenCalledWith(
      '/api/v1/bizdata/records',
      expect.objectContaining({ serialNo: 'SN-001' }),
    );
  });

  it('resubmit calls POST on /records/{id}/resubmit', async () => {
    vi.mocked(httpClient.post).mockResolvedValue({});
    await bizMessageRecordApi.resubmit('R-001');
    expect(httpClient.post).toHaveBeenCalledWith('/api/v1/bizdata/records/R-001/resubmit');
  });

  it('exportRecords calls POST on /records/export with params', async () => {
    vi.mocked(httpClient.post).mockResolvedValue('download-url');
    await bizMessageRecordApi.exportRecords({ pageNum: 1, pageSize: 20 });
    expect(httpClient.post).toHaveBeenCalledWith('/api/v1/bizdata/records/export', null, {
      params: { pageNum: 1, pageSize: 20 },
    });
  });
});
