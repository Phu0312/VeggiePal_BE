CREATE TABLE ai_guest_sessions (
    id VARCHAR(36) NOT NULL,
    ip_hash VARCHAR(64) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_guest_sessions_ip_created (ip_hash, created_at),
    INDEX idx_guest_sessions_expires (expires_at)
);
