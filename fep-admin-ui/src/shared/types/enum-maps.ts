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

/**
 * Shared enable/disable status type, mirrors backend {@code EnableDisableStatus}.
 * Centralized here to avoid sibling coupling between the submission API modules
 * (previously exported from {@code sub-output-interface-api}, which the data-source
 * and business-scene API modules imported from).
 */
export type EnableDisableStatus = 'ENABLED' | 'DISABLED';

export const INTERFACE_AUTH_TYPE_MAP: Record<string, StatusMapping> = {
  TOKEN: { label: 'Token', type: 'primary' },
  OAUTH2: { label: 'OAuth2', type: 'warning' },
  NONE: { label: '无', type: 'info' },
};

export const SCENE_PUSH_METHOD_MAP: Record<string, StatusMapping> = {
  AUTO: { label: '自动', type: 'success' },
  MANUAL: { label: '手动', type: 'warning' },
};

// v1b new — aligned with submission.EntryMethod = API_CALL/MANUAL_ENTRY
export const SUB_ENTRY_METHOD_MAP: Record<string, StatusMapping> = {
  API_CALL: { label: '接口调取', type: 'primary' },
  MANUAL_ENTRY: { label: '手工录入', type: 'info' },
};

// v1b new — aligned with submission.PushStatus = PENDING/PUSHING/PUSHED/FAILED
export const PUSH_STATUS_MAP: Record<string, StatusMapping> = {
  PENDING: { label: '待推送', type: 'info' },
  PUSHING: { label: '推送中', type: 'warning' },
  PUSHED: { label: '已推送', type: 'success' },
  FAILED: { label: '推送失败', type: 'danger' },
};

// §5.7 TLQ 节点角色（PRD §2.5.3 双主双从架构）
export const TLQ_NODE_ROLE_MAP: Record<string, StatusMapping> = {
  MASTER_PRODUCER: { label: '主节点（生产者）', type: 'primary' },
  MASTER_STANDBY: { label: '主节点（备用）', type: 'info' },
  SLAVE_CONSUMER: { label: '从节点（消费者）', type: 'success' },
  SLAVE_STANDBY: { label: '从节点（备用）', type: 'warning' },
};

// §5.7 TLQ 节点状态（状态机: UNKNOWN→ONLINE↔OFFLINE）
export const TLQ_NODE_STATUS_MAP: Record<string, StatusMapping> = {
  ONLINE: { label: '在线', type: 'success' },
  OFFLINE: { label: '离线', type: 'danger' },
  UNKNOWN: { label: '未知', type: 'info' },
};

// §3.1.1 TLQ 通道类型
export const TLQ_CHANNEL_TYPE_MAP: Record<string, StatusMapping> = {
  REALTIME: { label: '实时通道', type: 'primary' },
  BATCH: { label: '批量通道', type: 'info' },
};

// §3.1.2 TLQ 队列类型
export const TLQ_QUEUE_TYPE_MAP: Record<string, StatusMapping> = {
  LOCAL: { label: '本地队列', type: 'success' },
  REMOTE: { label: '远端队列', type: 'primary' },
  DEST: { label: '目标队列', type: 'info' },
  SEND: { label: '发送队列', type: 'warning' },
  DEAD: { label: '死信队列', type: 'danger' },
};

// §5.7.5 连通性测试结果
export const CONNECTIVITY_RESULT_MAP: Record<string, StatusMapping> = {
  SUCCESS: { label: '成功', type: 'success' },
  FAILURE: { label: '失败', type: 'danger' },
  TIMEOUT: { label: '超时', type: 'warning' },
};
