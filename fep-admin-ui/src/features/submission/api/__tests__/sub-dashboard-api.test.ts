import { describe, expect, it, vi, beforeEach } from 'vitest';
import { httpClient } from '@/shared/http/client';
import { subDashboardApi } from '../sub-dashboard-api';

vi.mock('@/shared/http/client', () => ({
  httpClient: {
    get: vi.fn(),
  },
}));

describe('subDashboardApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('getOverview() GETs /dashboard and returns 6 aggregated fields', async () => {
    vi.mocked(httpClient.get).mockResolvedValue({
      totalInterfaceCount: 10,
      enabledInterfaceCount: 8,
      totalDataSourceCount: 5,
      totalRecordCount: 100,
      pushedRecordCount: 80,
      pendingRecordCount: 20,
    });
    const result = await subDashboardApi.getOverview();
    expect(httpClient.get).toHaveBeenCalledWith('/api/v1/submission/dashboard');
    expect(result.totalInterfaceCount).toBe(10);
    expect(result.enabledInterfaceCount).toBe(8);
    expect(result.totalDataSourceCount).toBe(5);
    expect(result.totalRecordCount).toBe(100);
    expect(result.pushedRecordCount).toBe(80);
    expect(result.pendingRecordCount).toBe(20);
  });

  it('getTrend(7) GETs /dashboard/trend with days=7 param', async () => {
    vi.mocked(httpClient.get).mockResolvedValue({
      dates: ['2026-04-11', '2026-04-12'],
      pushedCounts: [3, 5],
      pendingCounts: [1, 2],
    });
    const result = await subDashboardApi.getTrend(7);
    expect(httpClient.get).toHaveBeenCalledWith('/api/v1/submission/dashboard/trend', {
      params: { days: 7 },
    });
    expect(result.dates).toHaveLength(2);
    expect(result.pushedCounts).toEqual([3, 5]);
    expect(result.pendingCounts).toEqual([1, 2]);
  });

  it('getDistribution("messageType") GETs /dashboard/distribution with dim=messageType', async () => {
    vi.mocked(httpClient.get).mockResolvedValue([
      { name: '3001', value: 12 },
      { name: '3002', value: 7 },
    ]);
    const result = await subDashboardApi.getDistribution('messageType');
    expect(httpClient.get).toHaveBeenCalledWith('/api/v1/submission/dashboard/distribution', {
      params: { dim: 'messageType' },
    });
    expect(result).toHaveLength(2);
    expect(result[0].name).toBe('3001');
    expect(result[0].value).toBe(12);
  });
});
