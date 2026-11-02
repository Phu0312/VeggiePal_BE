# Admin prompt versions

The AI service exposes these endpoints through the gateway under `/api/ai/admin/prompts` (or directly under `/ai/admin/prompts`). All require an access token with `ROLE_ADMIN`. No user quota is spent by preview; Groq usage still costs provider tokens.

| Method | Path | Purpose |
|---|---|---|
| GET | `/` | List all eight prompt slots, active version, variables and content |
| GET | `/{key}` | Current content for one slot |
| GET | `/{key}/versions` | Drafts and immutable published versions, including built-in version 0 |
| GET | `/{key}/activations` | Who activated which version and when |
| POST | `/{key}/drafts` | Create draft; body `{}` copies the active content, or `{"content":"..."}` sets it |
| PUT | `/{key}/drafts/{version}` | Edit draft with `{"content":"..."}` |
| POST | `/{key}/drafts/{version}/preview` | Call Groq with the draft and sample data, without publishing |
| POST | `/{key}/drafts/{version}/publish` | Publish draft and make it active atomically |
| POST | `/{key}/versions/{version}/activate` | Re-activate a published version, or version 0 to restore the built-in default |

Prompt keys: `CHAT_SYSTEM`, `VIDEO_EXTRACTION`, `MEAL_PLAN_SYSTEM`, `MEAL_PLAN_CREATE`, `MEAL_PLAN_REPLACE`, `RECOMMENDATION`, `MODERATION_SYSTEM`, `MODERATION_USER`.

Each template must keep all of its listed `{{variables}}` and cannot introduce unknown variables. Published versions are immutable; edit by creating another draft. The code still validates video JSON and meal-plan safety. A preview's `jsonShapeValid` checks only the basic response shape, so review the sample output before publishing.

Preview accepts `{"sampleInput":"...","variables":{...}}`. `sampleInput` is required for chat and video; for moderation system it is the sample content to classify. For meal-plan system, pass a complete sample user prompt in `sampleInput`. For prompts with variables, supply every listed variable in `variables`, for example:

```json
{
  "variables": {
    "limit": "3",
    "interest": "món chay giàu protein",
    "candidates": "[{\"type\":\"RECIPE\",\"id\":1,\"title\":\"Đậu hũ sốt cà\"}]"
  }
}
```

No draft affects live calls. Video jobs snapshot the active prompt when the worker starts and expose its `promptVersion` (0 means built-in default) in the job response. Stored results retain their existing 30-day retention and are not regenerated on publish.
