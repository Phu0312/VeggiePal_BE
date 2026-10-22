ALTER TABLE ai_video_jobs ADD COLUMN temp_file_path VARCHAR(1000) NULL;
ALTER TABLE ai_video_jobs ADD COLUMN quota_reserved BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE ai_video_jobs ADD COLUMN quota_refunded BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE ai_video_jobs ADD COLUMN quota_period_date DATE NULL;

CREATE INDEX idx_video_jobs_status_updated ON ai_video_jobs(status, updated_at);
