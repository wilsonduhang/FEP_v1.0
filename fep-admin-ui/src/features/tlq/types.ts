/** TLQ 节点角色，对齐后端 com.puchain.fep.web.tlq.node.domain.TlqNodeRole */
export type TlqNodeRole = 'MASTER_PRODUCER' | 'MASTER_STANDBY' | 'SLAVE_CONSUMER' | 'SLAVE_STANDBY';

/** TLQ 节点状态（状态机: UNKNOWN→ONLINE↔OFFLINE） */
export type TlqNodeStatus = 'ONLINE' | 'OFFLINE' | 'UNKNOWN';

export type TlqChannelType = 'REALTIME' | 'BATCH';
export type TlqQueueType = 'LOCAL' | 'REMOTE' | 'DEST' | 'SEND' | 'DEAD';
export type ConnectivityTestResult = 'SUCCESS' | 'FAILURE' | 'TIMEOUT';
export type EnableDisableStatus = 'ENABLED' | 'DISABLED';

/** POST /api/v1/tlq/nodes request */
export interface TlqNodeCreateRequest {
  nodeName: string;
  nodeRole: TlqNodeRole;
  hostIp: string;
  port: number;
  vipAddress?: string;
  protocol?: string;
  description?: string;
}

/** PUT /api/v1/tlq/nodes/{id} request (partial; nodeRole 不可修改 — 后端契约) */
export interface TlqNodeUpdateRequest {
  nodeName?: string;
  hostIp?: string;
  port?: number;
  vipAddress?: string;
  protocol?: string;
  description?: string;
}

/** 对齐后端 TlqNodeResponse */
export interface TlqNodeResponse {
  nodeId: string;
  nodeName: string;
  nodeRole: TlqNodeRole;
  hostIp: string;
  port: number;
  vipAddress: string | null;
  protocol: string;
  nodeStatus: TlqNodeStatus;
  description: string | null;
  lastHeartbeat: string | null; // ISO-8601
  createTime: string;
  updateTime: string;
}

export interface TlqQueueConfigCreateRequest {
  nodeId: string;
  queueName: string;
  channelType: TlqChannelType;
  queueType: TlqQueueType;
  description?: string;
}

export interface TlqQueueBatchGenerateRequest {
  nodeId: string;
  organizationCode: string;
}

export interface TlqQueueConfigResponse {
  queueId: string;
  nodeId: string;
  queueName: string;
  channelType: TlqChannelType;
  queueType: TlqQueueType;
  queueStatus: EnableDisableStatus;
  description: string | null;
  createTime: string;
  updateTime: string;
}

export interface ConnectivityRecordResponse {
  recordId: string;
  nodeId: string;
  testTime: string;
  testResult: ConnectivityTestResult;
  rttMs: number | null;
  errorMessage: string | null;
  triggeredBy: string;
}

export interface ConnectivityTestResponse {
  recordId: string;
  nodeId: string;
  result: ConnectivityTestResult;
  rttMs: number | null;
  message: string;
  testTime: string;
}

export interface ConnectivitySummaryResponse {
  nodeId: string;
  lastResult: ConnectivityTestResult | null;
  lastTestTime: string | null;
  totalTests: number;
  successCount: number;
  successRate: number; // 0-100.0
}
