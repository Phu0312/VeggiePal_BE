ALTER TABLE ai_moderation_cases
    ADD COLUMN content_text TEXT NULL AFTER author_id;

CREATE INDEX idx_moderation_target
    ON ai_moderation_cases (content_type, external_content_id, author_id);
