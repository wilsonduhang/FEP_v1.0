import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { sm2Cipher } from '../sm2-cipher';

// 固定的测试用 SM2 公钥（160 字符 hex，符合"长度 ≥ 128"判定）
// 这不是真实密钥，仅用于测试 resolveMode 的长度分支
const FAKE_SM2_PUB_HEX = '04' + 'a'.repeat(158);

// 同一公钥字节数据的 Base64 编码（供 Base64 分支测试）
// 数据为 64 个 0x04 字节 → Base64 长度 88 字符（含 == padding）
// 公式: ceil(64/3)*4 = 88；解码长度: 88/4*3 - 2 = 64 字节
// 64 字节 ≥ SM2_MIN_KEY_BYTES(64)，触发 sm2 分支判定
const FAKE_SM2_PUB_BASE64 = 'BAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBA==';

describe('sm2Cipher.resolveMode', () => {
  beforeEach(() => {
    vi.stubEnv('VITE_LOGIN_ENCRYPT_MODE', '');
  });
  afterEach(() => {
    vi.unstubAllEnvs();
  });

  it('returns plaintext when publicKey is empty', () => {
    expect(sm2Cipher.resolveMode('')).toBe('plaintext');
  });

  it('returns mock when publicKey starts with MOCK_', () => {
    expect(sm2Cipher.resolveMode('MOCK_SM2_PUBLIC_KEY_BASE64_FOR_DEV_ONLY')).toBe('mock');
  });

  it('returns sm2 when publicKey is hex and >=128 chars', () => {
    expect(sm2Cipher.resolveMode(FAKE_SM2_PUB_HEX)).toBe('sm2');
  });

  it('returns sm2 when publicKey is Base64 and decodes to >=64 bytes', () => {
    expect(sm2Cipher.resolveMode(FAKE_SM2_PUB_BASE64)).toBe('sm2');
  });

  it('returns plaintext when publicKey is short and not MOCK_', () => {
    expect(sm2Cipher.resolveMode('short')).toBe('plaintext');
  });

  it('env variable overrides runtime detection', () => {
    vi.stubEnv('VITE_LOGIN_ENCRYPT_MODE', 'plaintext');
    expect(sm2Cipher.resolveMode(FAKE_SM2_PUB_HEX)).toBe('plaintext');
  });
});

describe('sm2Cipher.normalizePublicKey', () => {
  it('returns hex as-is when input is already hex', () => {
    expect(sm2Cipher.normalizePublicKey(FAKE_SM2_PUB_HEX)).toBe(FAKE_SM2_PUB_HEX);
  });

  it('converts Base64 to hex', () => {
    const hex = sm2Cipher.normalizePublicKey(FAKE_SM2_PUB_BASE64);
    // 64 个 0x04 字节 → 128 字符 hex，全是 '04'
    expect(hex).toMatch(/^[0-9a-f]+$/);
    expect(hex.length).toBe(128);
    expect(hex).toBe('04'.repeat(64));
  });
});

describe('sm2Cipher.encryptLoginPassword', () => {
  beforeEach(() => {
    vi.stubEnv('VITE_LOGIN_ENCRYPT_MODE', '');
  });
  afterEach(() => {
    vi.unstubAllEnvs();
  });

  it('plaintext mode returns clearText unchanged', () => {
    const result = sm2Cipher.encryptLoginPassword('Abc12345', '');
    expect(result).toEqual({
      encryptedPassword: null,
      plaintextPassword: 'Abc12345',
      mode: 'plaintext',
      keyId: null,
    });
  });

  it('mock mode returns Base64 of clearText UTF-8 bytes', () => {
    const result = sm2Cipher.encryptLoginPassword(
      'Abc12345',
      'MOCK_SM2_PUBLIC_KEY_BASE64_FOR_DEV_ONLY',
      'mock-key-v1',
    );
    expect(result.mode).toBe('mock');
    expect(result.plaintextPassword).toBeNull();
    // UTF-8 'Abc12345' Base64 = 'QWJjMTIzNDU='
    expect(result.encryptedPassword).toBe('QWJjMTIzNDU=');
    expect(result.keyId).toBe('mock-key-v1');
  });

  it('sm2 mode produces a non-empty hex ciphertext without 04 prefix', () => {
    const result = sm2Cipher.encryptLoginPassword('Abc12345', FAKE_SM2_PUB_HEX, 'real-key-v1');
    expect(result.mode).toBe('sm2');
    expect(result.plaintextPassword).toBeNull();
    expect(result.encryptedPassword).toBeTruthy();
    // sm-crypto 返回纯 hex，无 04 前缀；只断言它是 hex 字符串
    expect(result.encryptedPassword!).toMatch(/^[0-9a-f]+$/);
    expect(result.keyId).toBe('real-key-v1');
  });
});
