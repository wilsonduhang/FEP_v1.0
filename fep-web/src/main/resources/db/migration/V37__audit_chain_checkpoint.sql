-- V37__audit_chain_checkpoint.sql
-- EFF-S5-1: 审计链校验 checkpoint 锚（单行表；持久化+SM2 签名锚，S5 抉择⑩ 截断攻击缓解升级）
CREATE TABLE audit_chain_checkpoint (
    id                   VARCHAR(16)  NOT NULL COMMENT '固定 SINGLETON（单行锚）',
    verified_until_seq   BIGINT       NOT NULL COMMENT '已验证至链 seq（含）',
    anchor_hash          VARCHAR(64)  NOT NULL COMMENT '锚行 hash（t_sys_operation_log.seq=verified_until_seq 行）',
    checkpoint_signature VARCHAR(120) NOT NULL COMMENT 'SM2 裸签 Base64，输入=域分隔串 audit-checkpoint:<seq>:<hash>（mock 域占位串）',
    sign_key_id          VARCHAR(64)  NOT NULL COMMENT '签名时审计密钥版本',
    verified_at          TIMESTAMP    NOT NULL COMMENT '推进时刻',
    PRIMARY KEY (id)
) COMMENT '审计链校验 checkpoint（EFF-S5-1）';
