# AI video API (asynchronous MVP)

Upload/history/job-detail endpoints require the existing bearer JWT. Job IDs are visible only to their owner.

## Submit

`POST /ai/videos/process` accepts multipart field `file`. The service validates the declared size/MIME and uses
`ffprobe` to confirm a video stream and duration (maximum 100 MB and 30 minutes) before reserving quota.

On acceptance it returns HTTP `202` with the existing `VideoJobResponse` shape. `result.id` is the job ID and
`result.status` is `PROCESSING`; transcript/extraction remain null until completion. Validation and quota failures
remain synchronous error responses.

## Poll and history

- `GET /ai/videos/{id}` returns the current owner-scoped job. Terminal statuses are `COMPLETED` and `FAILED`.
- `GET /ai/videos?page=0&size=20` returns owner-scoped history, newest first. Size is capped at 100.

`createdAt` and `updatedAt` are UTC instants serialized with a `Z` suffix (for example,
`2026-10-22T06:00:00Z`).

## Existing blog videos

- `POST /ai/videos/process-existing` with `{ "videoId": 8 }` starts extraction or reuses a retained result/job.
- `GET /ai/videos/by-video/{videoId}` is the polling endpoint for a shared blog video. Use this endpoint even
  when `process-existing` returned a job created by another user; `GET /ai/videos/{id}` is owner-scoped and returns
  404 for that user.
- `by-video` returns 409 / code 6022 while processing, 422 / code 6023 after failure, and 404 when no retained
  extraction exists (including after the 30-day output purge).
- A failed job and a completed job whose extraction has expired can be submitted again. The retry creates a new
  job and reserves quota normally; an unexpired `COMPLETED` result or a `PROCESSING` job is reused.

Processing is backed by a bounded in-process executor and bounded local temporary storage. Existing processing jobs
are failed and refunded on application restart because this MVP intentionally has no durable queue; it does not claim
that work resumes across container restarts.
