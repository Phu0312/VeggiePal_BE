# VeggiePal AI Service

## Runtime

- Java 21, Spring Boot 4.1.1, MySQL, Flyway, Groq and FFmpeg.
- Port: `8086`; direct service base URL: `http://localhost:8086`.
- This service intentionally has no API Gateway route yet. The current task explicitly excludes Gateway and Docker Compose changes.
- Schema is created by `db/migration/V1__create_ai_schema.sql`; Hibernate uses `ddl-auto=validate`.

Required environment variables:

```text
GROQ_API_KEY=<rotated Groq key>
JWT_SECRET=<same value as identity-service>
DB_USERNAME=root
DB_PASSWORD=12345
FFMPEG_PATH=ffmpeg
```

Optional model variables are `GROQ_CHAT_MODEL`, `GROQ_MODERATION_MODEL`,
`GROQ_RECOMMENDATION_MODEL`, `GROQ_MEAL_PLAN_MODEL`,
`GROQ_VIDEO_EXTRACTION_MODEL`, and `GROQ_TRANSCRIPTION_MODEL`.

Start Identity (`8081`), Nutrition (`8082`), Blog (`8083`), Recipe (`8084`), MySQL and then:

```powershell
cd ai-service
.\mvnw.cmd spring-boot:run
```

Swagger is available directly at `http://localhost:8086/swagger-ui.html`.

## API contract

Success responses use `{ "code": 1000, "result": ... }`. Protected endpoints require
`Authorization: Bearer <identity-service JWT>`. Admin endpoints require JWT claim
`role=ADMIN`.

| Method and path | Auth | Purpose |
|---|---|---|
| `POST /ai/chat/guest` | Public | Guest trial with daily quota (`guestSession`, `message`) |
| `POST /ai/chat` | Member | New or multi-turn chat (`conversationId` optional) |
| `GET /ai/chat/conversations` | Member | Conversation list |
| `GET /ai/chat/conversations/{id}` | Owner | Full persisted history |
| `POST /ai/moderation/check` | Member/service JWT | Moderate BLOG, VIDEO or COMMENT and persist a case |
| `GET /ai/admin/moderation` | Admin | Manual-review queue |
| `PUT /ai/admin/moderation/{id}` | Admin | Approve/reject a queued case |
| `GET /ai/recommendations?interest=&limit=` | Member | Rank real Blog/Recipe API records |
| `POST /ai/meal-plans` | Member | Generate and save a validated seven-day plan |
| `GET /ai/meal-plans` | Member | Saved plans |
| `GET /ai/meal-plans/{id}` | Owner | Plan detail |
| `POST /ai/meal-plans/{id}/replace` | Owner | Replace one meal with a real Recipe ID |
| `POST /ai/videos/process` | Member | Multipart video -> FFmpeg -> Whisper -> structured recipe |
| `GET /ai/videos/{id}` | Owner | Processing result |
| `GET /ai/admin/monitoring/summary?days=7` | Admin | Requests and token totals |
| `GET /ai/admin/monitoring/logs` | Admin | Latest 100 provider calls |
| `GET /ai/admin/monitoring/models` | Admin | Effective per-feature model configuration |

## Postman smoke test

Create environment variables `aiUrl=http://localhost:8086`, `token`, and
`adminToken`.

1. Guest chatbot — `POST {{aiUrl}}/ai/chat/guest`:

```json
{
  "guestSession": "browser-session-001",
  "message": "Nguồn protein chay nào phù hợp sau khi tập?"
}
```

2. Member chatbot — Bearer `{{token}}`, `POST {{aiUrl}}/ai/chat`:

```json
{
  "message": "Tôi cần tăng protein nhưng dị ứng đậu phộng."
}
```

Copy `result.conversationId` into the next request to verify multi-turn context.

3. Moderation — Bearer `{{token}}`, `POST {{aiUrl}}/ai/moderation/check`:

```json
{
  "contentType": "COMMENT",
  "externalContentId": 12,
  "authorId": 7,
  "text": "Nội dung cần kiểm duyệt"
}
```

4. Recommendations — Bearer `{{token}}`, `GET
{{aiUrl}}/ai/recommendations?interest=protein&limit=5`. Blog and Recipe services
must be running and contain published records.

5. Meal plan — Bearer `{{token}}`, `POST {{aiUrl}}/ai/meal-plans`:

```json
{
  "name": "Tuần tăng cơ",
  "healthGoal": "Tăng protein và giữ cân",
  "preferences": ["món Việt", "ít cay"],
  "availableIngredients": ["đậu hũ", "nấm"]
}
```

6. Video — Bearer `{{token}}`, multipart `POST {{aiUrl}}/ai/videos/process`, key
`file` of type File. Verify `ffmpeg -version` first.

7. Monitoring — Bearer `{{adminToken}}`, call the three `/ai/admin/monitoring/**`
endpoints.

## Verified and pending integration

Verified locally: Java compilation; Flyway migration and Hibernate validation on H2
in MySQL mode; application context; chatbot quota/token unit tests. Live Groq, live
MySQL, FFmpeg, and cross-service end-to-end calls require their external processes and
credentials and are not covered by the automated suite.

Blog moderation is exposed as a real AI contract, but `blog-service` still uses its
existing auto-approve implementation. Wiring that implementation to this endpoint is
a separate cross-service change. Video recommendations cannot be returned until
Blog/Video exposes a real public Video listing API. Runtime model changes require an
environment/config restart; the admin API currently reports effective models but does
not mutate deployment configuration.
