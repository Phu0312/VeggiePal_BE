CREATE TABLE ai_prompt_definitions (
    prompt_key VARCHAR(64) PRIMARY KEY,
    active_version INT NULL,
    next_version INT NOT NULL DEFAULT 1
);

CREATE TABLE ai_prompt_versions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    prompt_key VARCHAR(64) NOT NULL,
    version INT NOT NULL,
    content LONGTEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    published_by BIGINT NULL,
    published_at DATETIME(6) NULL,
    CONSTRAINT uk_prompt_key_version UNIQUE (prompt_key, version),
    CONSTRAINT fk_prompt_definition FOREIGN KEY (prompt_key) REFERENCES ai_prompt_definitions(prompt_key)
);

CREATE TABLE ai_prompt_activations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    prompt_key VARCHAR(64) NOT NULL,
    version INT NOT NULL,
    activated_by BIGINT NOT NULL,
    activated_at DATETIME(6) NOT NULL,
    CONSTRAINT fk_activation_definition FOREIGN KEY (prompt_key) REFERENCES ai_prompt_definitions(prompt_key)
);

INSERT INTO ai_prompt_definitions (prompt_key) VALUES
('CHAT_SYSTEM'), ('VIDEO_EXTRACTION'), ('MEAL_PLAN_SYSTEM'), ('MEAL_PLAN_CREATE'),
('MEAL_PLAN_REPLACE'), ('RECOMMENDATION'), ('MODERATION_SYSTEM'), ('MODERATION_USER');

ALTER TABLE ai_video_jobs ADD COLUMN prompt_version INT NULL;
