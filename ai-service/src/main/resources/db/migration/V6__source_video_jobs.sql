ALTER TABLE ai_video_jobs ADD COLUMN source_video_id BIGINT NULL;

CREATE UNIQUE INDEX uk_video_jobs_source_video ON ai_video_jobs(source_video_id);
