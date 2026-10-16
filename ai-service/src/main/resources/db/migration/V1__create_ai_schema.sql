CREATE TABLE ai_conversations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(160) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_ai_conversations_user_updated (user_id, updated_at)
);

CREATE TABLE ai_chat_messages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    conversation_id BIGINT NULL,
    principal_key VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    prompt_tokens INT NOT NULL DEFAULT 0,
    completion_tokens INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_chat_message_conversation FOREIGN KEY (conversation_id)
        REFERENCES ai_conversations(id) ON DELETE CASCADE,
    INDEX idx_chat_messages_conversation_created (conversation_id, created_at)
);

CREATE TABLE ai_daily_quotas (
    id BIGINT NOT NULL AUTO_INCREMENT,
    principal_key VARCHAR(100) NOT NULL,
    feature VARCHAR(40) NOT NULL,
    usage_date DATE NOT NULL,
    request_count INT NOT NULL DEFAULT 0,
    input_tokens BIGINT NOT NULL DEFAULT 0,
    output_tokens BIGINT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT uk_ai_quota_principal_feature_date UNIQUE (principal_key, feature, usage_date)
);

CREATE TABLE ai_usage_logs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NULL,
    principal_key VARCHAR(100) NOT NULL,
    feature VARCHAR(40) NOT NULL,
    model VARCHAR(120) NOT NULL,
    status VARCHAR(30) NOT NULL,
    prompt_tokens INT NOT NULL DEFAULT 0,
    completion_tokens INT NOT NULL DEFAULT 0,
    latency_ms BIGINT NOT NULL DEFAULT 0,
    error_message VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_ai_usage_created_feature (created_at, feature),
    INDEX idx_ai_usage_user_created (user_id, created_at)
);

CREATE TABLE ai_moderation_cases (
    id BIGINT NOT NULL AUTO_INCREMENT,
    content_type VARCHAR(30) NOT NULL,
    external_content_id BIGINT NULL,
    author_id BIGINT NULL,
    decision VARCHAR(30) NOT NULL,
    categories VARCHAR(500) NULL,
    reason VARCHAR(1000) NULL,
    confidence DECIMAL(5,4) NOT NULL,
    reviewed_by BIGINT NULL,
    reviewed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_moderation_decision_created (decision, created_at)
);

CREATE TABLE ai_meal_plans (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(160) NOT NULL,
    goal VARCHAR(500) NOT NULL,
    plan_json LONGTEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_meal_plans_user_created (user_id, created_at)
);

CREATE TABLE ai_video_jobs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    transcript LONGTEXT NULL,
    extraction_json LONGTEXT NULL,
    error_message VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_video_jobs_user_created (user_id, created_at)
);
