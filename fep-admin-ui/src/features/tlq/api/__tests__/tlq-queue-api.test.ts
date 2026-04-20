import { beforeEach, describe, expect, it, vi } from 'vitest';
import { httpClient } from '@/shared/http/client';
import { tlqQueueApi } from '../tlq-queue-api';
import type { TlqQueueConfigResponse } from '../../types';

vi.mock('@/shared/http/client', () => ({
  httpClient: {
    get: vi.fn(),
    post: vi.fn(),
    delete: vi.fn(),
  },
}));

const mockGet = vi.mocked(httpClient.get);
const mockPost = vi.mocked(httpClient.post);
const mockDelete = vi.mocked(httpClient.delete);

beforeEach(() => {
  mockGet.mockReset();
  mockPost.mockReset();
  mockDelete.mockReset();
});

describe('tlqQueueApi', () => {
  it('createQueue POSTs body to /queues', async () => {
    mockPost.mockResolvedValue({ queueId: 'Q1' } as TlqQueueConfigResponse);
    const req = {
      nodeId: 'N1',
      queueName: 'SEND.REALTIME.A1000143000104',
      channelType: 'REALTIME' as const,
      queueType: 'SEND' as const,
    };
    const result = await tlqQueueApi.createQueue(req);
    expect(mockPost).toHaveBeenCalledWith('/api/v1/tlq/queues', req);
    expect(result.queueId).toBe('Q1');
  });

  it('batchGenerate POSTs to /queues/batch-generate and returns array', async () => {
    const mockReturn: TlqQueueConfigResponse[] = [
      { queueId: 'Q1' } as TlqQueueConfigResponse,
      { queueId: 'Q2' } as TlqQueueConfigResponse,
    ];
    mockPost.mockResolvedValue(mockReturn);
    const result = await tlqQueueApi.batchGenerate({
      nodeId: 'N1',
      organizationCode: 'A1000143000104',
    });
    expect(mockPost).toHaveBeenCalledWith('/api/v1/tlq/queues/batch-generate', {
      nodeId: 'N1',
      organizationCode: 'A1000143000104',
    });
    expect(result).toHaveLength(2);
    expect(result[0]!.queueId).toBe('Q1');
  });

  it('listByNode GETs /queues with nodeId query and returns array (no PageResult wrap)', async () => {
    const mockReturn: TlqQueueConfigResponse[] = [
      { queueId: 'Q1', nodeId: 'N1' } as TlqQueueConfigResponse,
    ];
    mockGet.mockResolvedValue(mockReturn);
    const result = await tlqQueueApi.listByNode('N1');
    expect(mockGet).toHaveBeenCalledWith('/api/v1/tlq/queues', {
      params: { nodeId: 'N1' },
    });
    expect(Array.isArray(result)).toBe(true);
    expect(result).toHaveLength(1);
  });

  it('deleteQueue DELETEs /queues/{id}', async () => {
    mockDelete.mockResolvedValue(undefined);
    await tlqQueueApi.deleteQueue('Q7');
    expect(mockDelete).toHaveBeenCalledWith('/api/v1/tlq/queues/Q7');
  });
});
