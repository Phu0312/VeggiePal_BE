# AI video API (asynchronous MVP)

All endpoints require the existing bearer JWT. Jobs are visible only to their owner.

## Submit

`POST /ai/videos/process` accepts multipart field `file`. The service validates the declared size/MIME and uses
`ffprobe` to confirm a video stream and duration (maximum 100 MB and 30 minutes) before reserving quota.

On acceptance it returns HTTP `202` with the existing `VideoJobResponse` shape. `result.id` is the job ID and
`result.status` is `PROCESSING`; transcript/extraction remain null until completion. Validation and quota failures
remain synchronous error responses.

## Poll and history

- `GET /ai/videos/{id}` returns the current owner-scoped job. Terminal statuses are `COMPLETED` and `FAILED`.
- `GET /ai/videos?page=0&size=20` returns owner-scoped history, newest first. Size is capped at 100.

Processing is backed by a bounded in-process executor and bounded local temporary storage. Existing processing jobs
are failed and refunded on application restart because this MVP intentionally has no durable queue; it does not claim
that work resumes across container restarts.
