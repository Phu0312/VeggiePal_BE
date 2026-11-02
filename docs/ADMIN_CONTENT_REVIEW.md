# Admin content review (B17)

All requests below go through the gateway and require a JWT with `ROLE_ADMIN`. A signed-in non-admin receives HTTP 403; a request without a token receives HTTP 401.

| Method | Path | Purpose |
|---|---|---|
| GET | `/api/admin/blogs` | List blogs in every status |
| GET | `/api/admin/videos` | List videos in every status |
| GET | `/api/admin/recipes` | List recipes in every status |
| GET | `/api/admin/videos/{id}` | Read a video in any status |
| PUT | `/api/admin/blogs/{id}/status` | Resolve a pending or rejected blog |
| PUT | `/api/admin/videos/{id}/status` | Resolve a pending or rejected video |

Each list accepts optional `status`, `authorId`, `keyword`, `page` (default 0), and `size` (default 20, clamped to 1–100). No `status` means every status. Results are sorted by `createdAt DESC, id DESC`. `authorId` refers to `user_id` for recipes. Blog/video statuses are `DRAFT`, `PENDING`, `PUBLISHED`, `REJECTED`, `BANNED`; recipe statuses are `DRAFT`, `PUBLISHED`, `HIDDEN`, `ARCHIVED`. The existing `GET /api/blogs/{id}` and `GET /api/recipes/{id}` let admins read nonpublic details. Public list APIs still show published content only.

Status requests use JSON such as `{"status":"PUBLISHED","reason":"AI false positive"}`. `reason` is required (up to 1000 characters). Only `PENDING` or `REJECTED` can move to `PUBLISHED` or `REJECTED`; `BANNED` remains terminal. Every successful decision is recorded in `content_status_audits` with the admin's JWT user ID, reason, old and new status, and timestamp. Rejected video reasons are persisted so the author can see them later in `/api/videos/me`.

The AI moderation queue still contains only `REVIEW_REQUIRED` cases. Its "Skip" (`DELETE /api/ai/admin/moderation/{id}` or `PUT` with `DISMISSED`) returns the case unchanged in `REVIEW_REQUIRED`, leaving it available for a later decision. A review callback that discovers a deleted target or a decision already made through the admin content API closes the stale case automatically. Admins can use the B17 lists and status endpoints to resolve content that was previously stranded in `PENDING`.

Recipe `ARCHIVED` remains read-only and available by ID to existing meal plans; this work does not add a restore action.
