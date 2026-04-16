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
});
