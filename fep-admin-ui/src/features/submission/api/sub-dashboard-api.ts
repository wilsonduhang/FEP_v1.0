import { httpClient } from '@/shared/http/client';

/**
 * Submission Dashboard REST client.
 *
 * <p>Contract aligns with backend {@code SubDashboardController}
 * (PRD §5.5.1, FR-WEB-SUB-DASH / FR-WEB-SUB-API). Three endpoints:</p>
 * <ul>
 *   <li>{@code GET /dashboard} — 6 aggregated overview counts</li>
 *   <li>{@code GET /dashboard/trend?days=7|30} — pushed/pending daily series</li>
 *   <li>{@code GET /dashboard/distribution?dim=messageType|businessType} — Top 10 pie</li>
 * </ul>
 *
 * <p>Server-side counts are Java {@code long} but counts here are well below
 * {@link Number.MAX_SAFE_INTEGER} (2^53) so JS {@code number} is safe.</p>
 */

/** 6-field overview aggregation, from {@code DashboardResponse}. */
export interface DashboardOverview {
  /** 总接口数 */
  totalInterfaceCount: number;
  /** 启用接口数 */
  enabledInterfaceCount: number;
  /** 总数据源数 */
  totalDataSourceCount: number;
  /** 总报送记录数 */
  totalRecordCount: number;
  /** 已推送记录数 (push_status=PUSHED) */
  pushedRecordCount: number;
  /** 待推送记录数 (push_status=PENDING) */
  pendingRecordCount: number;
}

/** Daily trend series, from {@code DashboardTrendResponse}. Three arrays equal length. */
export interface DashboardTrend {
  /** ISO date series (yyyy-MM-dd) */
  dates: string[];
  /** Pushed count series, aligned with {@link dates} */
  pushedCounts: number[];
  /** Pending count series, aligned with {@link dates} */
  pendingCounts: number[];
}

/** Single Top-N slice, from {@code DashboardDistributionItem}. */
export interface DashboardDistributionItem {
  /** 分组维度值 (messageType code or businessType code) */
  name: string;
  /** 聚合计数 */
  value: number;
}

/** Supported trend window (days). */
export type DashboardTrendDays = 7 | 30;

/** Supported distribution dimensions. */
export type DashboardDistributionDim = 'messageType' | 'businessType';

const BASE = '/api/v1/submission/dashboard';

export const subDashboardApi = {
  /** Fetches 6-field overview aggregation. */
  getOverview: (): Promise<DashboardOverview> => httpClient.get(BASE),

  /**
   * Fetches pushed/pending daily time series for the trend chart.
   *
   * @param days 7 or 30; backend rejects other values with HTTP 400
   */
  getTrend: (days: DashboardTrendDays): Promise<DashboardTrend> =>
    httpClient.get(`${BASE}/trend`, { params: { days } }),

  /**
   * Fetches Top-10 distribution grouped by {@code dim} for the pie chart.
   *
   * @param dim grouping dimension; backend rejects other values with HTTP 400
   */
  getDistribution: (dim: DashboardDistributionDim): Promise<DashboardDistributionItem[]> =>
    httpClient.get(`${BASE}/distribution`, { params: { dim } }),
};
