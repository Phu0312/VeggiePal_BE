-- Run against the identity-service schema. This file is not auto-executed.
CREATE TABLE IF NOT EXISTS user_follows (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    follower_id BIGINT NOT NULL,
    following_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_user_follow UNIQUE (follower_id, following_id),
    INDEX idx_user_follow_following (following_id, follower_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
