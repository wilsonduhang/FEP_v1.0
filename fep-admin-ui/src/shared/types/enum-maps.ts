import type { StatusMapping } from '@/shared/components/StatusTag.vue';

export const QUERY_TYPE_MAP: Record<string, StatusMapping> = {
  REALTIME: { label: '实时', type: 'primary' },
  BATCH: { label: '批量', type: 'info' },
};

export const QUERY_TASK_STATUS_MAP: Record<string, StatusMapping> = {
  DRAFT: { label: '草稿', type: 'info' },
  PROCESSING: { label: '处理中', type: 'warning' },
  COMPLETED: { label: '已完成', type: 'success' },
  FAILED: { label: '失败', type: 'danger' },
};

export const RESULT_STATUS_MAP: Record<string, StatusMapping> = {
  NORMAL: { label: '正常', type: 'success' },
  ERROR: { label: '错误', type: 'danger' },
};

export const AUTH_TYPE_MAP: Record<string, StatusMapping> = {
  PAPER: { label: '纸质', type: 'info' },
  ELECTRONIC: { label: '电子', type: 'primary' },
};

export const LETTER_STATUS_MAP: Record<string, StatusMapping> = {
  DRAFT: { label: '草稿', type: 'info' },
  SUBMITTED: { label: '已提交', type: 'warning' },
  ACKNOWLEDGED: { label: '已确认', type: 'success' },
  REJECTED: { label: '已拒绝', type: 'danger' },
};

export const MESSAGE_DIRECTION_MAP: Record<string, StatusMapping> = {
  OUTBOUND: { label: '出站', type: 'primary' },
  INBOUND: { label: '入站', type: 'success' },
  BIDIRECTIONAL: { label: '双向', type: 'info' },
};

export const MESSAGE_PROCESS_STATUS_MAP: Record<string, StatusMapping> = {
  PENDING: { label: '待处理', type: 'info' },
  PROCESSING: { label: '处理中', type: 'warning' },
  SUCCESS: { label: '成功', type: 'success' },
  FAILED: { label: '失败', type: 'danger' },
};

export const ENTRY_METHOD_MAP: Record<string, StatusMapping> = {
  API: { label: '接口', type: 'primary' },
  MANUAL: { label: '手工', type: 'info' },
};

export const ENABLE_DISABLE_STATUS_MAP: Record<string, StatusMapping> = {
  ENABLED: { label: '启用', type: 'success' },
  DISABLED: { label: '禁用', type: 'info' },
};
