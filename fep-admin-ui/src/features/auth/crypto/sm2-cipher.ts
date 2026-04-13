// sm-crypto@0.3.13 ships as CommonJS without bundled type declarations
// (no @types/sm-crypto on npm). To keep the declaration scope local to this
// feature (no env.d.ts edit), we import it untyped and narrow the surface
// with a minimal interface covering only the SM2 API we actually use
// (doEncrypt with cipherMode 0=C1C2C3 / 1=C1C3C2).
// eslint-disable-next-line @typescript-eslint/ban-ts-comment
// @ts-expect-error sm-crypto has no bundled type declarations.
import smCryptoRaw from 'sm-crypto';

interface SmCryptoSm2 {
  doEncrypt(msg: string, publicKey: string, cipherMode?: 0 | 1): string;
}

interface SmCryptoLib {
  sm2: SmCryptoSm2;
}

const smCrypto: SmCryptoLib = smCryptoRaw as SmCryptoLib;

export type EncryptMode = 'sm2' | 'mock' | 'plaintext';

export interface EncryptResult {
  encryptedPassword: string | null;
  plaintextPassword: string | null;
  mode: EncryptMode;
  keyId: string | null;
}

/**
 * MOCK_ prefix is a front-back implicit contract.
 * MUST match backend MockKeyService.MOCK_PUBLIC_KEY constant
 * (fep-security-mock/.../MockKeyService.java).
 * If the backend mock prefix changes, this constant must be updated.
 */
const MOCK_KEY_PREFIX = 'MOCK_';

/** A hex SM2 public key is at least 128 chars (64 bytes, typically 130 with 04 prefix). */
const SM2_MIN_HEX_LENGTH = 128;

/** A decoded SM2 public key is at least 64 bytes (compressed: 33, uncompressed: 65). */
const SM2_MIN_KEY_BYTES = 64;

const HEX_REGEX = /^[0-9a-fA-F]+$/;
const BASE64_REGEX = /^[A-Za-z0-9+/]+={0,2}$/;

function isExplicitMode(value: string): value is EncryptMode {
  return value === 'sm2' || value === 'mock' || value === 'plaintext';
}

function utf8ToBase64(input: string): string {
  // btoa only accepts Latin-1; encode to UTF-8 bytes first.
  const bytes = new TextEncoder().encode(input);
  let binary = '';
  for (const b of bytes) {
    binary += String.fromCharCode(b);
  }
  return btoa(binary);
}

function base64ToBytes(input: string): Uint8Array {
  const binary = atob(input);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i += 1) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes;
}

function bytesToHex(bytes: Uint8Array): string {
  let hex = '';
  for (const b of bytes) {
    hex += b.toString(16).padStart(2, '0');
  }
  return hex;
}

export const sm2Cipher = {
  /**
   * Normalizes a public key string to hex format (required by sm-crypto).
   * If the input is already hex, returns as-is (lowercased).
   * If the input is Base64, decodes and re-encodes as hex.
   */
  normalizePublicKey(publicKey: string): string {
    if (HEX_REGEX.test(publicKey)) {
      return publicKey.toLowerCase();
    }
    // Treat as Base64
    return bytesToHex(base64ToBytes(publicKey));
  },

  resolveMode(publicKey: string): EncryptMode {
    const envMode = (import.meta.env.VITE_LOGIN_ENCRYPT_MODE ?? '').trim();
    if (isExplicitMode(envMode)) {
      return envMode;
    }
    if (!publicKey) {
      return 'plaintext';
    }
    if (publicKey.startsWith(MOCK_KEY_PREFIX)) {
      return 'mock';
    }
    // Hex branch: at least 128 hex chars (= 64 bytes)
    if (HEX_REGEX.test(publicKey) && publicKey.length >= SM2_MIN_HEX_LENGTH) {
      return 'sm2';
    }
    // Base64 branch: decoded byte length >= 64
    if (BASE64_REGEX.test(publicKey)) {
      try {
        const bytes = base64ToBytes(publicKey);
        if (bytes.length >= SM2_MIN_KEY_BYTES) {
          return 'sm2';
        }
      } catch {
        // Not valid Base64, fall through
      }
    }
    return 'plaintext';
  },

  encryptLoginPassword(
    clearText: string,
    publicKey: string,
    keyId: string | null = null,
  ): EncryptResult {
    const mode = this.resolveMode(publicKey);

    if (mode === 'plaintext') {
      return {
        encryptedPassword: null,
        plaintextPassword: clearText,
        mode,
        keyId: null,
      };
    }

    if (mode === 'mock') {
      return {
        encryptedPassword: utf8ToBase64(clearText),
        plaintextPassword: null,
        mode,
        keyId,
      };
    }

    // mode === 'sm2': normalize public key to hex, then call sm-crypto.
    // cipherMode=1 is C1C3C2 (FEP backend convention).
    // Returns raw hex ciphertext WITHOUT leading '04' prefix — the
    // backend KeyServiceImpl must accept sm-crypto-compatible C1C3C2 format.
    const hexKey = this.normalizePublicKey(publicKey);
    const rawCipher = smCrypto.sm2.doEncrypt(clearText, hexKey, 1);
    return {
      encryptedPassword: rawCipher,
      plaintextPassword: null,
      mode,
      keyId,
    };
  },
};
