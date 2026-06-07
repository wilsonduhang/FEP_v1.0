-- 回调凭证有效期（工程加固，FR-INFRA-CALLBACK-CREDENTIAL-EXPIRY）。
-- NULL = 永不过期（向后兼容存量凭证）。解析期 now > expires_at 拒用。
ALTER TABLE callback_credential ADD COLUMN expires_at TIMESTAMP NULL;
