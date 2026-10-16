# AI Service implementation phases

1. **Foundation — implemented and tested:** package-by-feature, JWT resource server,
   response/errors, environment-only provider key, per-feature models, timeout/retry,
   Flyway schema, usage logs, request/token quota.
2. **Chatbot — implemented and unit/context tested:** guest quota, member persistence,
   bounded multi-turn context, nutrition safety prompt, history APIs.
3. **Moderation — implemented in AI service:** structured classification, confidence
   threshold, persisted cases and admin review. Cross-service Blog wiring remains.
4. **Recommendation — implemented in AI service:** candidates are read from actual
   Blog and Recipe endpoints, then AI-ranked and ID-validated. Video awaits a source API.
5. **Meal planning — implemented in AI service:** reads real health/allergy/recipe data,
   requires exactly seven days and three meals/day, validates every Recipe ID, saves and
   supports replacement. Live cross-service E2E remains.
6. **Video processing — implemented in AI service:** bounded upload, temporary-file
   cleanup, FFmpeg audio extraction, Groq Whisper transcription, LLM recipe extraction,
   persisted status/result. Live FFmpeg/Groq E2E remains.
7. **Monitoring — implemented read side:** logs, token totals and effective model view.
   Runtime model mutation/alerting remains deployment work.
