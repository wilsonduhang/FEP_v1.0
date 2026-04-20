import { beforeEach, describe, expect, it, vi } from 'vitest';
import { httpClient } from '@/shared/http/client';
import { tlqNodeApi } from '../tlq-node-api';
import { toZeroBasedPage } from '../paging';
import type { TlqNodeResponse } from '../../types';

vi.mock('@/shared/http/client', () => ({
  httpClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
    patch: vi.fn(),
  },
}));

const mockGet = vi.mocked(httpClient.get);
const mockPost = vi.mocked(httpClient.post);
const mockPut = vi.mocked(httpClient.put);
const mockDelete = vi.mocked(httpClient.delete);
const mockPatch = vi.mocked(httpClient.patch);

beforeEach(() => {
  mockGet.mockReset();
  mockPost.mockReset();
  mockPut.mockReset();
  mockDelete.mockReset();
  mockPatch.mockReset();
});

describe('toZeroBasedPage', () => {
  it('throws on pageNum < 1', () => {
    expect(() => toZeroBasedPage(0)).toThrow(/pageNum >= 1/);
    expect(() => toZeroBasedPage(-1)).toThrow(/pageNum >= 1/);
  });

  it('converts 1-based pageNum to 0-based page', () => {
    expect(toZeroBasedPage(1)).toBe(0);
    expect(toZeroBasedPage(5)).toBe(4);
  });
});

describe('tlqNodeApi', () => {
  it('listNodes converts pageNum=3 to page=2 on outbound request', async () => {
    // Mock response uses distinct pageNum (999) to prevent tautological passthrough.
    mockGet.mockResolvedValue({
      records: [],
      total: 0,
      pageNum: 999,
      pageSize: 999,
      totalPages: 0,
    });
    await tlqNodeApi.listNodes({ pageNum: 3, pageSize: 10 });
    expect(mockGet).toHaveBeenCalledWith('/api/v1/tlq/nodes', {
      params: { page: 2, size: 10 },
    });
  });

  it('listNodes passes through backend PageResult.pageNum (1-based) unchanged', async () => {
    mockGet.mockResolvedValue({
      records: [{ nodeId: 'N1' } as TlqNodeResponse],
      total: 1,
      pageNum: 3,
      pageSize: 10,
      totalPages: 5,
    });
    const result = await tlqNodeApi.listNodes({ pageNum: 3, pageSize: 10 });
    expect(result.pageNum).toBe(3);
    expect(result.pageSize).toBe(10);
    expect(result.totalPages).toBe(5);
    expect(result.records).toHaveLength(1);
  });

  it('listNodes omits role/status query params when undefined', async () => {
    mockGet.mockResolvedValue({
      records: [],
      total: 0,
      pageNum: 999,
      pageSize: 999,
      totalPages: 0,
    });
    await tlqNodeApi.listNodes({ pageNum: 1, pageSize: 10 });
    expect(mockGet).toHaveBeenCalledWith('/api/v1/tlq/nodes', {
      params: { page: 0, size: 10 },
    });
    const callArg = mockGet.mock.calls[0]![1] as { params: Record<string, unknown> };
    expect(callArg.params).not.toHaveProperty('role');
    expect(callArg.params).not.toHaveProperty('status');
  });

  it('listNodes includes role and status when provided', async () => {
    mockGet.mockResolvedValue({
      records: [],
      total: 0,
      pageNum: 999,
      pageSize: 999,
      totalPages: 0,
    });
    await tlqNodeApi.listNodes({
      pageNum: 2,
      pageSize: 20,
      role: 'MASTER_PRODUCER',
      status: 'ONLINE',
    });
    expect(mockGet).toHaveBeenCalledWith('/api/v1/tlq/nodes', {
      params: { page: 1, size: 20, role: 'MASTER_PRODUCER', status: 'ONLINE' },
    });
  });

  it('createNode POSTs body to /nodes', async () => {
    mockPost.mockResolvedValue({ nodeId: 'N1' } as TlqNodeResponse);
    const req = {
      nodeName: 'primary-producer',
      nodeRole: 'MASTER_PRODUCER' as const,
      hostIp: '10.0.0.1',
      port: 9001,
    };
    const result = await tlqNodeApi.createNode(req);
    expect(mockPost).toHaveBeenCalledWith('/api/v1/tlq/nodes', req);
    expect(result.nodeId).toBe('N1');
  });

  it('getNode GETs /nodes/{id}', async () => {
    mockGet.mockResolvedValue({ nodeId: 'N42' } as TlqNodeResponse);
    const result = await tlqNodeApi.getNode('N42');
    expect(mockGet).toHaveBeenCalledWith('/api/v1/tlq/nodes/N42');
    expect(result.nodeId).toBe('N42');
  });

  it('updateNode PUTs partial body to /nodes/{id}', async () => {
    mockPut.mockResolvedValue({ nodeId: 'N1', nodeName: 'renamed' } as TlqNodeResponse);
    await tlqNodeApi.updateNode('N1', { nodeName: 'renamed', port: 9002 });
    expect(mockPut).toHaveBeenCalledWith('/api/v1/tlq/nodes/N1', {
      nodeName: 'renamed',
      port: 9002,
    });
  });

  it('deleteNode DELETEs /nodes/{id}', async () => {
    mockDelete.mockResolvedValue(undefined);
    await tlqNodeApi.deleteNode('N9');
    expect(mockDelete).toHaveBeenCalledWith('/api/v1/tlq/nodes/N9');
  });

  it('changeStatus PATCHes with target query and null body', async () => {
    mockPatch.mockResolvedValue({ nodeId: 'N1', nodeStatus: 'ONLINE' } as TlqNodeResponse);
    await tlqNodeApi.changeStatus('N1', 'ONLINE');
    expect(mockPatch).toHaveBeenCalledWith(
      '/api/v1/tlq/nodes/N1/status',
      null,
      { params: { target: 'ONLINE' } },
    );
  });
});
