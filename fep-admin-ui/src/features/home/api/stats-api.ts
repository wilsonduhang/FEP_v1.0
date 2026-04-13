import { httpClient } from '@/shared/http/client';

/**
 * Aligned with backend TimeRange enum (4 values).
 * 'CUSTOM' is declared but not yet used by the UI — will be consumed
 * by a future custom date-range picker. Narrowing it out now would
 * cause TS errors when that feature arrives.
 */
export type TimeRange = 'TODAY' | 'THIS_WEEK' | 'THIS_MONTH' | 'CUSTOM';

export interface StatsCardsResponse {
  /** BigDecimal serialized as string to preserve precision across JS Number boundary. */
  totalAmount: string;
  successCount: number;
  todayMessageCount: number;
  exceptionCount: number;
}

export interface TrendDataPoint {
  label: string;
  sentCount: number;
  receivedCount: number;
}

export interface DistributionItem {
  messageCode: string;
  messageName: string;
  count: number;
  percentage: number;
}

export interface StatusDistributionItem {
  status: string;
  count: number;
  percentage: number;
}

export const statsApi = {
  getCards: (): Promise<StatsCardsResponse> =>
    httpClient.get('/api/v1/dashboard/stats/cards'),
  getTrend: (range: TimeRange = 'THIS_WEEK'): Promise<TrendDataPoint[]> =>
    httpClient.get('/api/v1/dashboard/stats/trend', { params: { range } }),
  getDistribution: (): Promise<DistributionItem[]> =>
    httpClient.get('/api/v1/dashboard/stats/distribution'),
  getStatusDistribution: (): Promise<StatusDistributionItem[]> =>
    httpClient.get('/api/v1/dashboard/stats/status-distribution'),
};
