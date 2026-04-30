import { httpClient } from '@/shared/http/client';

/** 报文方向映射配置（对应 com.puchain.fep.web.sysconfig.domain.DirMapConfig） */
export interface DirMapConfig {
  messageType: string;
  messageName: string;
  accessRole: 'ACCEPTING_ORG' | 'INFO_SERVICE_ORG';
  direction: 'OUTBOUND_ACTIVE' | 'INBOUND_PASSIVE' | 'NOT_APPLICABLE';
  requiresFep: boolean;
  processingMode: 'MODE_1' | 'MODE_2' | 'MODE_3' | 'MODE_5';
  updatedBy: string;
  updatedAt: string;
}

/** 方向映射变更历史 */
export interface DirMapHistory {
  historyId: string;
  oldDirection: string | null;
  oldRequiresFep: boolean | null;
  oldMode: string | null;
  newDirection: string;
  newRequiresFep: boolean;
  newMode: string;
  changedBy: string;
  changedAt: string;
  changeReason: string | null;
}

/** 方向映射更新请求 */
export interface DirMapUpdateRequest {
  direction: string;
  requiresFep: boolean;
  processingMode: string;
  changeReason?: string;
}

/**
 * 后端分页结果（对应 com.puchain.fep.common.domain.PageResult）。
 * 字段名为 `records`（与 fep-common PageResult 序列化形状一致），不是 `list`。
 */
export interface PageResult<T> {
  records: T[];
  total: number;
  pageNum: number;
  pageSize: number;
}

/** 拉取方向映射列表（默认一次拉满 88 行） */
export async function listDirMap(
  pageNum = 1,
  pageSize = 100,
): Promise<PageResult<DirMapConfig>> {
  return httpClient.get('/api/v1/sys/config/dir-map', {
    params: { pageNum, pageSize },
  });
}

/** 更新指定 (messageType, accessRole) 的方向映射 */
export async function updateDirMap(
  messageType: string,
  accessRole: string,
  req: DirMapUpdateRequest,
): Promise<DirMapConfig> {
  return httpClient.put(
    `/api/v1/sys/config/dir-map/${messageType}/${accessRole}`,
    req,
  );
}

/** 拉取指定 (messageType, accessRole) 的变更历史（倒序） */
export async function listDirMapHistory(
  messageType: string,
  accessRole: string,
): Promise<DirMapHistory[]> {
  return httpClient.get(
    `/api/v1/sys/config/dir-map/${messageType}/${accessRole}/history`,
  );
}
