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
});
