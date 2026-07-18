CREATE DATABASE IF NOT EXISTS web_audio CHARACTER SET utf8mb4 COLLATE utf8mb4_polish_ci;

USE web_audio;

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL DEFAULT 'USER',
    plan_type VARCHAR(20) NOT NULL DEFAULT 'FREE',
    plan_expires_at DATE DEFAULT NULL,
    notification_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    phone VARCHAR(20) DEFAULT NULL,
    birthday DATE DEFAULT NULL,
    gender VARCHAR(10) DEFAULT NULL,
    avatar VARCHAR(1000) DEFAULT NULL,
    auth_provider VARCHAR(20) DEFAULT 'LOCAL',
    google_sub VARCHAR(128) DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_polish_ci;

-- Migration cho DB đã tồn tại (chạy nếu bảng users cũ thiếu cột)
-- ALTER TABLE users ADD COLUMN auth_provider VARCHAR(20) DEFAULT 'LOCAL';
-- ALTER TABLE users ADD COLUMN google_sub VARCHAR(128) DEFAULT NULL;
-- ALTER TABLE users MODIFY COLUMN avatar VARCHAR(1000) DEFAULT NULL;

CREATE TABLE audios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(100) NOT NULL,
    genre VARCHAR(50) NOT NULL,
    duration VARCHAR(20) NOT NULL,
    file_size BIGINT NOT NULL DEFAULT 0,
    audio_url VARCHAR(500) NOT NULL,
    cover_image_url VARCHAR(500) DEFAULT NULL,
    view_count BIGINT NOT NULL DEFAULT 0,
    like_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_polish_ci;

CREATE TABLE listening_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_email VARCHAR(255) NOT NULL,
    audio_id BIGINT NOT NULL,
    audio_title VARCHAR(255) NOT NULL,
    audio_author VARCHAR(100) NOT NULL,
    audio_genre VARCHAR(50) NOT NULL,
    audio_duration VARCHAR(20) NOT NULL,
    audio_url VARCHAR(500) NOT NULL,
    cover_image_url VARCHAR(500) DEFAULT NULL,
    listened_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    progress INT DEFAULT 0,
    INDEX idx_user_email (user_email),
    INDEX idx_listened_at (listened_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_polish_ci;

INSERT INTO audios (title, author, genre, duration, audio_url, cover_image_url) VALUES
('Mộng Hoa Lục', 'Thiên Diệp', 'Huyền Huyễn', '45:00',
'https://upload.wikimedia.org/wikipedia/commons/2/22/Tone_clip.ogg',
'https://ui-avatars.com/api/?name=ML&background=7c3aed&color=fff&size=128'),
('Sài Gòn Mưa Rơi', 'Mộc Trà', 'Ngôn Tình', '38:20',
'https://upload.wikimedia.org/wikipedia/commons/2/22/Tone_clip.ogg',
'https://ui-avatars.com/api/?name=SG&background=ec4899&color=fff&size=128'),
('Oán Hồn Xóm Nhỏ', 'Kỳ Án', 'Kinh Dị', '52:10',
'https://upload.wikimedia.org/wikipedia/commons/2/22/Tone_clip.ogg',
'https://ui-avatars.com/api/?name=OH&background=ef4444&color=fff&size=128'),
('Vụ Án Đêm Trăng', 'Hoàng Nam', 'Trinh Thám', '12:45',
'https://upload.wikimedia.org/wikipedia/commons/2/22/Tone_clip.ogg',
'https://ui-avatars.com/api/?name=VA&background=3b82f6&color=fff&size=128'),
('Yêu Lại Từ Đầu', 'An Nhiên', 'Ngôn Tình', '08:30',
'https://upload.wikimedia.org/wikipedia/commons/2/22/Tone_clip.ogg',
'https://ui-avatars.com/api/?name=YL&background=f97316&color=fff&size=128');

CREATE TABLE user_likes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_email VARCHAR(255) NOT NULL,
    audio_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_audio (user_email, audio_id),
    INDEX idx_user_email (user_email),
    INDEX idx_audio_id (audio_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_polish_ci;

CREATE TABLE premium_registrations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_email VARCHAR(255) NOT NULL,
    user_name VARCHAR(255) NOT NULL,
    plan_name VARCHAR(100) NOT NULL,
    price VARCHAR(50) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    receipt_image VARCHAR(500) DEFAULT NULL,
    registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_email (user_email),
    INDEX idx_status (status),
    INDEX idx_registered_at (registered_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_polish_ci;

CREATE TABLE playlists (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    user_email VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_email (user_email),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_polish_ci;

CREATE TABLE playlist_audios (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    playlist_id BIGINT NOT NULL,
    audio_id BIGINT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_playlist_audio (playlist_id, audio_id),
    INDEX idx_playlist_id (playlist_id),
    INDEX idx_audio_id (audio_id),
    CONSTRAINT fk_playlist FOREIGN KEY (playlist_id) REFERENCES playlists(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_polish_ci;

CREATE TABLE notifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_email VARCHAR(255) NOT NULL,
    audio_id BIGINT NULL,
    audio_title VARCHAR(255) NULL,
    audio_author VARCHAR(100) NULL,
    audio_genre VARCHAR(50) NULL,
    audio_duration VARCHAR(20) NULL,
    audio_url VARCHAR(500) NULL,
    cover_image_url VARCHAR(500) DEFAULT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_email (user_email),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_polish_ci;

CREATE TABLE feedbacks (
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

CREATE TABLE advertisements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    url VARCHAR(500) NOT NULL,
    name VARCHAR(255) DEFAULT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_polish_ci;

INSERT INTO advertisements (url, name) VALUES
('https://example.com/ad1', 'Quảng cáo mẫu 1'),
('https://example.com/ad2', 'Quảng cáo mẫu 2'),
('https://example.com/ad3', 'Quảng cáo mẫu 3');
