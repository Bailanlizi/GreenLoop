-- Apply once to existing GreenLoop databases before enabling notifications.async-enabled.
ALTER TABLE notifications ADD COLUMN related_type VARCHAR(32) NULL AFTER related_id;
ALTER TABLE notifications ADD COLUMN source_event_id VARCHAR(64) NULL AFTER related_type;
ALTER TABLE notifications ADD UNIQUE KEY uk_notification_source_event (source_event_id);
ALTER TABLE notifications ADD KEY idx_notifications_user_read_time (user_id, is_read, create_time);

CREATE TABLE IF NOT EXISTS outbox_event (
  id BIGINT AUTO_INCREMENT PRIMARY KEY, event_id VARCHAR(64) NOT NULL, event_type VARCHAR(50) NOT NULL,
  recipient_id BIGINT NOT NULL, related_id BIGINT NULL, related_type VARCHAR(32) NULL, content VARCHAR(255) NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING', retry_count INT NOT NULL DEFAULT 0, next_retry_time DATETIME NULL,
  last_error VARCHAR(500) NULL, published_time DATETIME NULL, create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_outbox_event_id (event_id), KEY idx_outbox_status_retry (status, next_retry_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
