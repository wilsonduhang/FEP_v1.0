import { describe, expect, it, vi, beforeEach } from 'vitest';
import { httpClient } from '@/shared/http/client';
import { subReportApi } from '../sub-report-api';

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

describe('subReportApi', () => {
  it('searchRecords passes keyword/time/page params as GET query', async () => {
    mockGet.mockResolvedValue({
      records: [],
      total: 0,
      pageNum: 1,
      pageSize: 10,
      totalPages: 0,
    });
    await subReportApi.searchRecords({
      keyword: '3001',
      startTime: '2026-04-01T00:00:00',
      endTime: '2026-04-18T23:59:59',
      pageNum: 1,
      pageSize: 10,
    });
    expect(mockGet).toHaveBeenCalledWith('/api/v1/report/records', {
      params: {
        keyword: '3001',
        startTime: '2026-04-01T00:00:00',
        endTime: '2026-04-18T23:59:59',
        pageNum: 1,
        pageSize: 10,
      },
    });
  });

  it('getRecord fetches single record by ID', async () => {
    mockGet.mockResolvedValue({ recordId: 'R1', messageType: '3001' });
    const r = await subReportApi.getRecord('R1');
    expect(mockGet).toHaveBeenCalledWith('/api/v1/report/records/R1');
    expect(r.recordId).toBe('R1');
  });

  it('uploadRecord sends 5 @RequestParam as query (NOT body, NOT multipart)', async () => {
    mockPost.mockResolvedValue({ recordId: 'R2' });
    await subReportApi.uploadRecord({
      messageType: '3101',
      messageName: '电子合同信息流转报文',
      businessTypeId: 'BIZ001',
      dataCount: 100,
      entryBy: 'zhangsan',
    });
    expect(mockPost).toHaveBeenCalledWith('/api/v1/report/upload', null, {
      params: {
        messageType: '3101',
        messageName: '电子合同信息流转报文',
        businessTypeId: 'BIZ001',
        dataCount: 100,
        entryBy: 'zhangsan',
      },
    });
  });

  it('triggerPush sends recordIds as plain array body', async () => {
    mockPost.mockResolvedValue([{ recordId: 'R1', pushStatus: 'PUSHING' }]);
    await subReportApi.triggerPush(['R1', 'R2']);
    expect(mockPost).toHaveBeenCalledWith('/api/v1/report/push', ['R1', 'R2']);
  });

  it('getBlocked fetches PUSHING+FAILED records paginated', async () => {
    mockGet.mockResolvedValue({
      records: [],
      total: 0,
      pageNum: 1,
      pageSize: 100,
      totalPages: 0,
    });
    await subReportApi.getBlocked(1, 100);
    expect(mockGet).toHaveBeenCalledWith('/api/v1/report/push/blocked', {
      params: { pageNum: 1, pageSize: 100 },
    });
  });

  it('getByMessageType fetches records for a given type', async () => {
    mockGet.mockResolvedValue({
      records: [],
      total: 0,
      pageNum: 1,
      pageSize: 10,
      totalPages: 0,
    });
    await subReportApi.getByMessageType('3001', 1, 10);
    expect(mockGet).toHaveBeenCalledWith('/api/v1/report/records/by-type/3001', {
      params: { pageNum: 1, pageSize: 10 },
    });
  });

  it('getTrend returns period/count array', async () => {
    mockGet.mockResolvedValue([
      { period: '2026-01', count: 10 },
      { period: '2026-02', count: 25 },
    ]);
    const trend = await subReportApi.getTrend('3001');
    expect(mockGet).toHaveBeenCalledWith('/api/v1/report/records/by-type/3001/trend');
    expect(trend).toHaveLength(2);
    expect(trend[0].period).toBe('2026-01');
    expect(trend[0].count).toBe(10);
  });
});
