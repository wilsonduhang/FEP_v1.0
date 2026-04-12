import { beforeEach, describe, it, expect } from 'vitest';
import { TokenStorage } from '../token-storage';

describe('TokenStorage', () => {
  beforeEach(() => localStorage.clear());

  it('set/get access token', () => {
    TokenStorage.set('abc');
    expect(TokenStorage.get()).toBe('abc');
  });

  it('set/get refresh token independently', () => {
    TokenStorage.setRefresh('rtk');
    expect(TokenStorage.getRefresh()).toBe('rtk');
    expect(TokenStorage.get()).toBeNull();
  });

  it('clear removes both tokens', () => {
    TokenStorage.set('a');
    TokenStorage.setRefresh('r');
    TokenStorage.clear();
    expect(TokenStorage.get()).toBeNull();
    expect(TokenStorage.getRefresh()).toBeNull();
  });
});
