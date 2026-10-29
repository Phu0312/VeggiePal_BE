# Backend changes: validation, pagination, diaries and community

## Approved contracts

- Blog content: at most 50,000 Unicode code points, including all submitted markup and whitespace. This is not a byte limit. A supplementary character counts once; combining marks count separately. No normalization is performed. Both create and update return HTTP 400 / code 3016 before persistence or moderation when exceeded. The existing minimum of 20 UTF-16 code units remains unchanged.
- Pagination: `items`, zero-based `page`, default `size=20`, maximum 100. Sizes are clamped to 1..100 and negative pages to zero. Shape: `{items, page, size, totalElements, totalPages}`. `first` and `last` are removed. The temporary `content` alias on recipe-service and ai-service pages was removed on 2026-10-29 (task A07) after the frontend moved to `items`; ai-service now reads only `items` from recipe-service pages.
- Diary calendar days use `Asia/Ho_Chi_Minh`. Explicit dates are ISO `YYYY-MM-DD`; omitted dates mean today in that zone. Clients should send an explicit date for retries spanning midnight. Stored confirmation instants use UTC.
- Community uses a separate following feed. Follow lists, counts and the following feed hide non-ACTIVE accounts, preserve relationships, and restore visibility on reactivation. Blocked actors cannot use follow APIs. Existing general public blog APIs keep their current behavior.

## Fixes

- AI profile codes remain 6020/6021. Video queue full is 6025 (HTTP 503), unsupported source is 6026 (HTTP 422). All AI enum codes are checked for uniqueness.
- Registration rejects null, empty, whitespace-only and passwords shorter than 6 characters with HTTP 400 / PASSWORD_REQUIRED (1010) or INVALID_PASSWORD (1003). Passwords remain encoded before persistence and are excluded from RegisterRequest.toString().
- Recipe create/update validate title <=200, description <=2000, imageUrl <=500, ingredient unit <=30, ingredient note <=300 and step instruction <=2000. Codes 4110..4115 respectively, HTTP 400. Existing database failures remain HTTP 500.
- AI reads recipe candidates through up to two 100-item pages from `items`.
- `GET /api/blogs/me` items also carry `moderationReason` and `aiReviewInProgress` (task B04), same meaning as on `GET /api/blogs/{id}`. Public lists omit both fields.
- `GET /api/ai/admin/monitoring/summary` and `/logs/search` accept `excludeFeature` (e.g. `MODERATION`, case-insensitive) to leave one feature out of the numbers (task B06).

## Meal, water and activity diary API

All paths below include the gateway `/api` prefix and require Bearer authentication. Identity always comes from JWT; clients cannot choose a data owner.

| Method | Path | Body / query |
|---|---|---|
| GET | `/api/nutrition/me/diary` | optional `?date=2026-10-27` |
| PUT | `/api/nutrition/me/diary/meal-plans/{planId}/meals/{mealType}` | `{ "date": "2026-10-27", "servings": 1.5 }` |
| DELETE | `/api/nutrition/me/diary/meal-plans/{planId}/meals/{mealType}` | optional `?date=2026-10-27` |
| POST | `/api/nutrition/me/diary/water` | `{ "date": "2026-10-27", "milliliters": 250 }` |
| PUT | `/api/nutrition/me/diary/water/{id}` | same body |
| DELETE | `/api/nutrition/me/diary/water/{id}` | none |
| POST | `/api/nutrition/me/diary/activities` | `{ "date": "2026-10-27", "activityType": "Walking", "minutes": 30 }` |
| PUT | `/api/nutrition/me/diary/activities/{id}` | same body |
| DELETE | `/api/nutrition/me/diary/activities/{id}` | none |

`mealType`: BREAKFAST, LUNCH, DINNER, SNACK. Existing AI plans are weekly: the consumed date selects its weekday (e.g. a Tuesday date selects TUESDAY). A missing day/meal or a plan not owned by the caller returns 404/2014. Nutrition forwards the original JWT to the AI plan detail endpoint, which enforces ownership. A missing/unavailable source returns 503/2015; confirmation is not saved with guessed/zero nutrition.

A meal entry includes date, source=MEAL_PLAN, mealPlanId, mealType, recipeId, title, servings, calories/protein/carbohydrate/fat snapshot, and recordedAt. Servings are numbers of recipe portions, 0.01..100 with up to two decimals; omit to use the plan's servingMultiplier (default 1). Plan meal nutrition is already scaled: actual snapshot = planned nutrition * actual servings / planned servings, rounded to 4 decimals. New/edited plan snapshots carry recipeTitle; legacy plans resolve it through recipe-service, falling back to `Recipe #id` only if the recipe was deleted.

Unique key `(userId, planId, date, mealType)` guarantees one confirmation per slot, including concurrent requests. First confirmation wins; repeats return the saved entry, even if different servings are submitted. To change servings or refresh a snapshot, cancel then confirm again. Cancel is idempotent and can only affect the caller's entries. Editing/deleting a plan later does not rewrite saved diary entries. The source enum also reserves MANUAL with nullable mealPlanId for future manual-entry support; no manual food API is implemented in this change.

GET returns `{date, timezone, meals, nutrition, hasMeals, water, totalWaterMl, activities, totalActivityMinutes}` inside the standard result envelope. Only saved confirmations contribute to nutrition. `nutrition` is always an object (zeros when nothing is logged); `hasMeals=false` means no meal was confirmed that day, so the FE can show "chưa ghi" instead of a logged 0 kcal day (task B01). Activity does not add back calories. Water is a positive integer in ml (max 100,000 per entry); activity type is nonblank <=100 characters and minutes 1..1440. Water/activity updates or deletes of another user's entry return 404/2013.

## Video recipe extraction from a published blog video (task I01)

Both endpoints extract a recipe from a video already published in blog-service, so the FE never re-uploads the file. Only `UPLOAD` videos (Cloudinary .mp4) are supported; YouTube videos are refused. One extraction is kept per blog video and shared by every viewer.

### `POST /api/ai/videos/process-existing`

- Auth: Bearer JWT (any signed-in user).
- Body: `{ "videoId": 42 }` (blog-service video id, required).
- Response: **202** with a `VideoJobResponse`: `{id, sourceVideoId, status, filename, transcript, extraction, error, createdAt, updatedAt}`. `status` is `PROCESSING` for a new or running extraction, or `COMPLETED` with `extraction` when a result already exists. `transcript` is always null here.
- Quota: a new extraction reserves one slot of the caller's monthly `VIDEO` quota (refunded if it cannot start). Returning an existing `PROCESSING`/`COMPLETED` result costs nothing. A `FAILED` result is discarded and the next call starts over.
- Then poll `GET /api/ai/videos/by-video/{videoId}` (or `GET /api/ai/videos/{id}` with the returned job id, requester only).

| HTTP | Code | When |
|---|---|---|
| 400 | 6000 | `videoId` missing or not positive |
| 401 | 6001 | no or invalid JWT |
| 404 | 6002 | video does not exist or is not `PUBLISHED` |
| 422 | 6026 | video is not an `UPLOAD` (e.g. YouTube) |
| 422 | 6008 | the video file could not be downloaded or is not valid media |
| 413 | 6009 | the video file is larger than the download limit |
| 429 | 6003 | monthly video quota used up |
| 503 | 6025 | processing queue full (quota refunded) |
| 503 | 6006 | blog-service unreachable or internal token not configured |

### `GET /api/ai/videos/by-video/{videoId}`

- Auth: **public** (`videoId` must be digits). A token, if sent, is ignored.
- Response: **200** `{ sourceVideoId, extraction, completedAt }`, where `extraction` is the extracted recipe JSON and `completedAt` is UTC.

| HTTP | Code | When |
|---|---|---|
| 404 | 6002 | video missing or not `PUBLISHED`, or nobody has requested an extraction yet |
| 422 | 6026 | video is not an `UPLOAD` |
| 409 | 6022 | extraction still running: keep polling |
| 422 | 6023 | extraction failed: offer `process-existing` again |
| 503 | 6006 | blog-service unreachable |

## Follow and following feed API

| Method | Gateway path | Meaning |
|---|---|---|
| PUT | `/api/users/{id}/follow` | Ensure following; repeated requests are harmless |
| DELETE | `/api/users/{id}/follow` | Ensure not following; repeated requests are harmless |
| GET | `/api/users/{id}/follow-stats` | `{followers, following, followedByMe}` |
| GET | `/api/users/me/following` | Current user's following list |
| GET | `/api/users/me/followers` | Current user's followers |
| GET | `/api/users/{id}/following` | Active user's following list |
| GET | `/api/users/{id}/followers` | Active user's followers |
| GET | `/api/users/me/following/ids` | Active followed author IDs for feed lookup |
| GET | `/api/blogs/feed/following` | Published blogs by active followed authors, publishedAt DESC then id DESC |

Lists and feed accept `page` / `size` with the shared pagination contract. Follow list items contain only `{id, fullName, avatarUrl}`; totalElements is the visible count. Self-follow returns 400/1020. Follow mutations serialize on the actor's user row and have a unique pair constraint. Counts are queried from visible relationships, never incremented on retries.

The frontend button uses its current state: PUT to follow, DELETE to unfollow; do not implement a retry-sensitive toggle endpoint. The following feed fetches the caller's active following IDs from identity-service, then filters and paginates in the blog database, so totalElements is accurate. Identity unavailable returns 503/3070; it never silently returns the general feed. This ID-list query suits the current project; very large follow graphs would require a different cross-service feed design.

## Deployment

- Nutrition needs AI_SERVICE_URL and RECIPE_SERVICE_URL. Blog needs IDENTITY_SERVICE_URL. Docker Compose now supplies container URLs; there are deliberately no new startup dependencies that would create an AI/nutrition dependency cycle.
- Identity and nutrition still use the repository's existing Hibernate ddl-auto=update setup. For a controlled production rollout, review/run `identity-service/src/main/resources/db/manual/20261027_user_follows.sql` in the identity schema and `nutrition-service/src/main/resources/db/manual/20261027_diaries.sql` in the nutrition schema before starting the new code. These manual SQL files are not auto-executed. Their unique indexes are required for correctness.
- No Aiven changes or deployment have been performed by this implementation.

## Verification

Latest reports from the full suites and focused reruns: **651 tests, 0 failures, 0 errors**.

| Service | Tests |
|---|---:|
| AI | 211 |
| Identity | 87 |
| Nutrition | 50 |
| Recipe | 33 |
| Blog | 270 |

Coverage includes error-code uniqueness; password rejection and encoded persistence; recipe boundary/overflow validation on create/update; Vietnamese and supplementary Unicode blog content; real JSON pagination serialization; diary/follow concurrency with a real H2 database; owner isolation; day rollover in Vietnam; HTTP token forwarding and upstream failures; and **25 BlogServiceIntegrationTests against local MySQL 8.4**, including the following feed and lazy category mapping.

The legacy database-backed context-only tests `VeggiepalApplicationTests`, `NutritionServiceApplicationTests`, and `BlogServiceApplicationTests` were excluded. The new diary/follow integration tests start complete application contexts on isolated H2 databases; BlogServiceIntegrationTests starts the blog context on a separate local MySQL schema. AI and recipe full suites ran. Test-only H2 dependencies were added to identity and nutrition. Both manual schema files were executed successfully against separate local MySQL schemas. No test used the deployed Aiven database.

Relevant commands (run inside each service with JDK 21):

- AI / recipe: `./mvnw.cmd test`
- Identity: `./mvnw.cmd '-Dtest=!VeggiepalApplicationTests' test`
- Nutrition: `./mvnw.cmd '-Dtest=!NutritionServiceApplicationTests' test`
- Blog: `./mvnw.cmd '-DDB_PORT=3311' '-Dspring.datasource.username=root' '-Dspring.datasource.password=' '-Dtest=!BlogServiceApplicationTests' test` (requires the isolated local MySQL server on 3311).

`git diff --check` passed. Aiven rollout and frontend integration remain deployment work.
