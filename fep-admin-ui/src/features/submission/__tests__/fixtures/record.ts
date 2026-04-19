import type { SubmissionRecordResponse } from '../../api/sub-report-api';

/**
 * Shared test fixture for SubmissionRecordResponse.
 *
 * Returns a fully-populated record with sensible defaults; pass overrides to
 * customize for specific tests. Replaces the scattered `{...} as any` casts
 * that bypassed the 16-field interface (v1b Simplify F6).
 */
export function makeRecord(
  overrides: Partial<SubmissionRecordResponse> = {},
): SubmissionRecordResponse {
  return {
    recordId: 'R1',
    messageType: '3001',
    messageName: 'Query Request',
    businessTypeId: null,
    submitterName: null,
    businessNo: null,
    amount: null,
    dataCount: 0,
    entryMethod: 'API_CALL',
    entryBy: null,
    pushStatus: 'PENDING',
    pushTime: null,
    errorMessage: null,
    sortOrder: 0,
    createTime: '2026-04-18T10:00:00',
    updateTime: '2026-04-18T10:00:00',
    ...overrides,
  };
}
