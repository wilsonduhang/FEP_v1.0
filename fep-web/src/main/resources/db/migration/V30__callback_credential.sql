-- Callback Phase 2b T3: 凭证存储（SM4 加密列 + 1:1 FK to t_sub_output_interface）
-- 镜像 callback_queue (V27) 风格；密文列用 VARBINARY 避免字符集污染。
-- TOKEN auth: token_ciphertext + token_header
-- OAUTH2 auth: oauth_client_id_ciphertext + oauth_client_secret_ciphertext + oauth_token_endpoint + oauth_scope
-- 公共: key_id (SM4 master key version, 支持轮换), create_time/update_time/rotated_at
CREATE TABLE callback_credential (
  credential_id                  VARCHAR(32)  NOT NULL,
  interface_id                   VARCHAR(32)  NOT NULL,
  auth_type                      VARCHAR(20)  NOT NULL,
  token_ciphertext               VARBINARY(512),
  token_header                   VARCHAR(50) DEFAULT 'Authorization',
  oauth_client_id_ciphertext     VARBINARY(512),
  oauth_client_secret_ciphertext VARBINARY(1024),
  oauth_token_endpoint           VARCHAR(500),
  oauth_scope                    VARCHAR(200),
  key_id                         VARCHAR(32)  NOT NULL,
  create_time                    TIMESTAMP    NOT NULL,
  update_time                    TIMESTAMP    NOT NULL,
  rotated_at                     TIMESTAMP    NULL,
  PRIMARY KEY (credential_id),
  CONSTRAINT uk_callback_credential_interface UNIQUE (interface_id),
  CONSTRAINT fk_callback_credential_interface
    FOREIGN KEY (interface_id) REFERENCES t_sub_output_interface(interface_id)
);
CREATE INDEX idx_callback_credential_interface ON callback_credential(interface_id);
