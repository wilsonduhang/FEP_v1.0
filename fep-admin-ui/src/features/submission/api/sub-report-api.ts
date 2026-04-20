import { httpClient } from '@/shared/http/client';
import type { PageResult } from '@/shared/types/page-result';

/**
 * Submission Report REST client (PRD §5.6).
 *
 * <p>Endpoints under {@code /api/v1/report} — see backend
 * {@code SubReportController} (FR-WEB-REP-UPLOAD / LIST / VIEW / PUSH).</p>
 *
 * <p>Contract notes (verified against backend):</p>
 * <ul>
 *   <li>{@code upload} uses 5 {@code @RequestParam}, NOT {@code @RequestBody},
 *       NOT {@code multipart/form-data}. Pass via {@code params}, body = {@code null}.</li>
 *   <li>{@code push} uses {@code @RequestBody List<String>}; send the raw array
 *       (not an object wrapper).</li>
 *   <li>{@code getRecord} 404 maps to {@code BIZ_5001}; {@code httpClient} throws
 *       {@code {code, message}}.</li>
 *   <li>{@code triggerPush} with no PENDING records maps to {@code BIZ_5003} (HTTP 400).</li>
 *   <li>{@code amount} is serialized as a JSON string via {@code ToStringSerializer}
 *       to preserve BigDecimal precision beyond {@code Number.MAX_SAFE_INTEGER} (2^53).</li>
 * </ul>
 */

/** Push status lifecycle (backend enum {@code PushStatus}). */
export type PushStatus = 'PENDING' | 'PUSHING' | 'PUSHED' | 'FAILED';

/** Entry method for submission records (backend enum {@code EntryMethod}). */
export type EntryMethod = 'API_CALL' | 'MANUAL_ENTRY';

/**
 * Submission record response, aligned with backend
 * {@code SubmissionRecordResponse} (16 fields).
 */
export interface SubmissionRecordResponse {
  /** 记录 ID */
  recordId: string;
  /** 报文号 */
  messageType: string;
  /** 报文名称 */
  messageName: string;
  /** 关联业务类型 ID（可为 null） */
  businessTypeId: string | null;
  /** 报送单位名称（可为 null） */
  submitterName: string | null;
  /** 业务编号（可为 null） */
  businessNo: string | null;
  /** 金额（万元）— BigDecimal 序列化为 JSON string 以保全精度；可为 null */
  amount: string | null;
  /** 数据条数 */
  dataCount: number;
  /** 录入方式 */
  entryMethod: EntryMethod;
  /** 录入人（可为 null） */
  entryBy: string | null;
  /** 推送状态 */
  pushStatus: PushStatus;
  /** 推送时间（ISO 8601，可为 null） */
  pushTime: string | null;
  /** 推送失败原因（可为 null） */
  errorMessage: string | null;
  /** 排序数值 */
  sortOrder: number;
  /** 创建时间（ISO 8601） */
  createTime: string;
  /** 更新时间（ISO 8601） */
  updateTime: string;
}

/** Search parameters for {@link subReportApi.searchRecords}. */
export interface ReportSearchParams {
  /** 关键字（可选，匹配报文名称或业务编号） */
  keyword?: string;
  /** 起始时间（ISO 8601，可选） */
  startTime?: string;
  /** 截止时间（ISO 8601，可选） */
  endTime?: string;
  /** 页码（1-based，默认 1） */
  pageNum?: number;
  /** 每页大小（默认 10） */
  pageSize?: number;
}

/** Manual upload payload for {@link subReportApi.uploadRecord}. */
export interface UploadRecordRequest {
  /** 报文类型（必填） */
  messageType: string;
  /** 报文名称（必填） */
  messageName: string;
  /** 业务类型 ID（可选） */
  businessTypeId?: string;
  /** 数据条数（必填） */
  dataCount: number;
  /** 录入人（可选） */
  entryBy?: string;
}

/** Trend aggregation point, one row per month. */
export interface TrendPoint {
  /** 聚合周期（如 {@code "2026-04"}） */
  period: string;
  /** 聚合计数 */
  count: number;
}

const BASE = '/api/v1/report';

export const subReportApi = {
  /** §5.6.2 — Search records with pagination. */
  searchRecords: (params: ReportSearchParams): Promise<PageResult<SubmissionRecordResponse>> =>
    httpClient.get(`${BASE}/records`, { params }),

  /** Fetch single record by ID. 404 → {@code BIZ_5001}. */
  getRecord: (recordId: string): Promise<SubmissionRecordResponse> =>
    httpClient.get(`${BASE}/records/${recordId}`),

  /**
   * §5.6.1 — Manual upload.
   *
   * <p>Backend uses 5 {@code @RequestParam}; pass as {@code params}, body =
   * {@code null}. True file parsing is deferred to P1; this endpoint only
   * creates metadata.</p>
   */
  uploadRecord: (request: UploadRecordRequest): Promise<SubmissionRecordResponse> =>
    httpClient.post(`${BASE}/upload`, null, { params: { ...request } }),

  /**
   * §5.6.4 — Trigger push.
   *
   * <p>Body is a raw {@code List<String>}; backend uses {@code @RequestBody}.
   * No PENDING records → {@code BIZ_5003} (HTTP 400).</p>
   */
  triggerPush: (recordIds: string[]): Promise<SubmissionRecordResponse[]> =>
    httpClient.post(`${BASE}/push`, recordIds),

  /** §5.6.4 — Blocked records ({@code PushStatus IN (PUSHING, FAILED)}) paginated. */
  getBlocked: (pageNum = 1, pageSize = 100): Promise<PageResult<SubmissionRecordResponse>> =>
    httpClient.get(`${BASE}/push/blocked`, { params: { pageNum, pageSize } }),

  /** §5.6.3 — By-type records. */
  getByMessageType: (
    messageType: string,
    pageNum = 1,
    pageSize = 10,
  ): Promise<PageResult<SubmissionRecordResponse>> =>
    httpClient.get(`${BASE}/records/by-type/${messageType}`, {
      params: { pageNum, pageSize },
    }),

  /** §5.6.3 — By-type trend (monthly aggregation). */
  getTrend: (messageType: string): Promise<TrendPoint[]> =>
    httpClient.get(`${BASE}/records/by-type/${messageType}/trend`),
};
