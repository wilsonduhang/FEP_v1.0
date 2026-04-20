import { describe, expect, it } from 'vitest';
import {
  QUERY_TYPE_MAP,
  QUERY_TASK_STATUS_MAP,
  RESULT_STATUS_MAP,
  AUTH_TYPE_MAP,
  LETTER_STATUS_MAP,
  MESSAGE_DIRECTION_MAP,
  MESSAGE_PROCESS_STATUS_MAP,
  ENTRY_METHOD_MAP,
  ENABLE_DISABLE_STATUS_MAP,
  SUB_ENTRY_METHOD_MAP,
  PUSH_STATUS_MAP,
  TLQ_NODE_ROLE_MAP,
  TLQ_NODE_STATUS_MAP,
  TLQ_CHANNEL_TYPE_MAP,
  TLQ_QUEUE_TYPE_MAP,
  CONNECTIVITY_RESULT_MAP,
} from '../enum-maps';

describe('enum-maps', () => {
  it('QUERY_TYPE_MAP has 2 keys', () => {
    expect(Object.keys(QUERY_TYPE_MAP)).toHaveLength(2);
  });

  it('QUERY_TASK_STATUS_MAP has 4 keys', () => {
    expect(Object.keys(QUERY_TASK_STATUS_MAP)).toHaveLength(4);
  });

  it('RESULT_STATUS_MAP has 2 keys', () => {
    expect(Object.keys(RESULT_STATUS_MAP)).toHaveLength(2);
  });

  it('AUTH_TYPE_MAP has 2 keys', () => {
    expect(Object.keys(AUTH_TYPE_MAP)).toHaveLength(2);
  });

  it('LETTER_STATUS_MAP has 4 keys', () => {
    expect(Object.keys(LETTER_STATUS_MAP)).toHaveLength(4);
  });

  it('MESSAGE_DIRECTION_MAP has 3 keys', () => {
    expect(Object.keys(MESSAGE_DIRECTION_MAP)).toHaveLength(3);
  });

  it('MESSAGE_PROCESS_STATUS_MAP has 4 keys', () => {
    expect(Object.keys(MESSAGE_PROCESS_STATUS_MAP)).toHaveLength(4);
  });

  it('ENTRY_METHOD_MAP has 2 keys', () => {
    expect(Object.keys(ENTRY_METHOD_MAP)).toHaveLength(2);
  });

  it('ENABLE_DISABLE_STATUS_MAP has 2 keys', () => {
    expect(Object.keys(ENABLE_DISABLE_STATUS_MAP)).toHaveLength(2);
  });

  it('SUB_ENTRY_METHOD_MAP has API_CALL + MANUAL_ENTRY keys aligned with submission.EntryMethod', () => {
    const keys = Object.keys(SUB_ENTRY_METHOD_MAP);
    expect(keys).toHaveLength(2);
    expect(keys).toContain('API_CALL');
    expect(keys).toContain('MANUAL_ENTRY');
    expect(SUB_ENTRY_METHOD_MAP.API_CALL.label).toBe('接口调取');
    expect(SUB_ENTRY_METHOD_MAP.API_CALL.type).toBe('primary');
    expect(SUB_ENTRY_METHOD_MAP.MANUAL_ENTRY.label).toBe('手工录入');
    expect(SUB_ENTRY_METHOD_MAP.MANUAL_ENTRY.type).toBe('info');
  });

  it('PUSH_STATUS_MAP has 4 keys aligned with backend PushStatus enum', () => {
    const keys = Object.keys(PUSH_STATUS_MAP);
    expect(keys).toHaveLength(4);
    expect(keys).toEqual(expect.arrayContaining(['PENDING', 'PUSHING', 'PUSHED', 'FAILED']));
    expect(PUSH_STATUS_MAP.PENDING.label).toBe('待推送');
    expect(PUSH_STATUS_MAP.PENDING.type).toBe('info');
    expect(PUSH_STATUS_MAP.PUSHING.type).toBe('warning');
    expect(PUSH_STATUS_MAP.PUSHED.type).toBe('success');
    expect(PUSH_STATUS_MAP.FAILED.type).toBe('danger');
  });

  it('TLQ_NODE_ROLE_MAP has 4 keys aligned with backend TlqNodeRole', () => {
    const keys = Object.keys(TLQ_NODE_ROLE_MAP);
    expect(keys).toHaveLength(4);
    expect(keys).toEqual(
      expect.arrayContaining(['MASTER_PRODUCER', 'MASTER_STANDBY', 'SLAVE_CONSUMER', 'SLAVE_STANDBY'])
    );
    expect(TLQ_NODE_ROLE_MAP.MASTER_PRODUCER.label).toBe('主节点（生产者）');
    expect(TLQ_NODE_ROLE_MAP.MASTER_PRODUCER.type).toBe('primary');
    expect(TLQ_NODE_ROLE_MAP.SLAVE_CONSUMER.label).toBe('从节点（消费者）');
    expect(TLQ_NODE_ROLE_MAP.SLAVE_CONSUMER.type).toBe('success');
  });

  it('TLQ_NODE_STATUS_MAP has 3 keys with state machine UNKNOWN→ONLINE↔OFFLINE', () => {
    const keys = Object.keys(TLQ_NODE_STATUS_MAP);
    expect(keys).toHaveLength(3);
    expect(keys).toEqual(expect.arrayContaining(['ONLINE', 'OFFLINE', 'UNKNOWN']));
    expect(TLQ_NODE_STATUS_MAP.ONLINE.label).toBe('在线');
    expect(TLQ_NODE_STATUS_MAP.ONLINE.type).toBe('success');
    expect(TLQ_NODE_STATUS_MAP.OFFLINE.type).toBe('danger');
  });

  it('TLQ_CHANNEL_TYPE_MAP has 2 keys for channel types', () => {
    const keys = Object.keys(TLQ_CHANNEL_TYPE_MAP);
    expect(keys).toHaveLength(2);
    expect(keys).toEqual(expect.arrayContaining(['REALTIME', 'BATCH']));
    expect(TLQ_CHANNEL_TYPE_MAP.REALTIME.label).toBe('实时通道');
    expect(TLQ_CHANNEL_TYPE_MAP.REALTIME.type).toBe('primary');
  });

  it('TLQ_QUEUE_TYPE_MAP has 5 keys for queue types', () => {
    const keys = Object.keys(TLQ_QUEUE_TYPE_MAP);
    expect(keys).toHaveLength(5);
    expect(keys).toEqual(expect.arrayContaining(['LOCAL', 'REMOTE', 'DEST', 'SEND', 'DEAD']));
    expect(TLQ_QUEUE_TYPE_MAP.LOCAL.label).toBe('本地队列');
    expect(TLQ_QUEUE_TYPE_MAP.LOCAL.type).toBe('success');
    expect(TLQ_QUEUE_TYPE_MAP.DEAD.type).toBe('danger');
  });

  it('CONNECTIVITY_RESULT_MAP has 3 keys for test results', () => {
    const keys = Object.keys(CONNECTIVITY_RESULT_MAP);
    expect(keys).toHaveLength(3);
    expect(keys).toEqual(expect.arrayContaining(['SUCCESS', 'FAILURE', 'TIMEOUT']));
    expect(CONNECTIVITY_RESULT_MAP.SUCCESS.label).toBe('成功');
    expect(CONNECTIVITY_RESULT_MAP.SUCCESS.type).toBe('success');
    expect(CONNECTIVITY_RESULT_MAP.FAILURE.type).toBe('danger');
  });
});
