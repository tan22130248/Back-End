-- Feedback / góp ý + cho phép notification không gắn audio
USE web_audio;

ALTER TABLE notifications
  MODIFY COLUMN audio_id BIGINT NULL,
  MODIFY COLUMN audio_title VARCHAR(255) NULL,
  MODIFY COLUMN audio_author VARCHAR(100) NULL,
  MODIFY COLUMN audio_genre VARCHAR(50) NULL,
  MODIFY COLUMN audio_duration VARCHAR(20) NULL,
  MODIFY COLUMN audio_url VARCHAR(500) NULL;

CREATE TABLE IF NOT EXISTS feedbacks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_email VARCHAR(255) NOT NULL,
    user_name VARCHAR(100) DEFAULT NULL,
    category VARCHAR(50) NOT NULL DEFAULT 'OTHER',
    subject VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    admin_reply TEXT DEFAULT NULL,
    replied_at TIMESTAMP NULL DEFAULT NULL,
    is_system_issue BOOLEAN NOT NULL DEFAULT FALSE,
    broadcast_sent BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_feedback_user (user_email),
    INDEX idx_feedback_status (status),
    INDEX idx_feedback_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_polish_ci;
