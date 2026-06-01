-- Callback Phase 2b T11: 站内信通知表（DLQ 死信告警落地，决策门 6 事件解耦）
-- 通用站内信表（category 区分来源），首个使用方 CALLBACK_DLQ；未来 EMAIL/SMS 扩展目标。
CREATE TABLE in_app_notification (
  notification_id VARCHAR(32)   NOT NULL,
  user_id         VARCHAR(64)   NOT NULL,
  category        VARCHAR(50)   NOT NULL,
  level           VARCHAR(20)   NOT NULL,
  title           VARCHAR(200)  NOT NULL,
  message         VARCHAR(1000) NOT NULL,
  ref_id          VARCHAR(64)   DEFAULT NULL,
  ref_type        VARCHAR(50)   DEFAULT NULL,
  is_read         BOOLEAN       NOT NULL DEFAULT FALSE,
  create_time     TIMESTAMP     NOT NULL,
  read_at         TIMESTAMP     DEFAULT NULL,
  PRIMARY KEY (notification_id)
);
CREATE INDEX idx_notification_user_unread ON in_app_notification (user_id, is_read);
CREATE INDEX idx_notification_create_time ON in_app_notification (create_time);
