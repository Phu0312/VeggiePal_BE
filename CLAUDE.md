# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

VeggiePal backend: Spring Boot microservices (Java 21, Spring Boot 4.1.1). There is **no parent/aggregator POM**. Each service (`api-gateway/`, `identity-service/`, `nutrition-service/`, `blog-service/`, `location-service/`) is a separate Maven project with its own wrapper, so run Maven commands from inside the service directory. Only `api-gateway` uses `application.yaml`; every other service, including `blog-service`, uses `application.properties`.

## Workflow and skills

Four skill families are installed for this project. Pick by situation — do not load all four for every task. They are local plugins; a session without one simply skips that step.

| Situation | Skill |
|---|---|
| Starting a spec or feature, or any change in behaviour | `superpowers:brainstorming` — design and the user's approval before any code |
| Spec approved and the work has several steps | `superpowers:writing-plans`, then `superpowers:subagent-driven-development` to execute it |
| Writing production code | `superpowers:test-driven-development` |
| A bug, a failing test, unexpected behaviour | `superpowers:systematic-debugging` before proposing a fix |
| About to say "done", commit or push | `superpowers:verification-before-completion` — run the commands and read the output |
| Feature finished | `superpowers:requesting-code-review`, then `superpowers:finishing-a-development-branch` |
| "Have we solved this before?", or resuming work from an earlier session | `claude-mem:mem-search` |
| Finding a function, class or call site without reading whole files | `claude-mem:smart-explore` |
| Architecture or cross-service questions ("what touches X", "how does Y reach Z") | `graphify` — `graphify query "..."` when `graphify-out/` exists. Building the graph costs an extraction pass, so run `/graphify` for a spec that spans several services, and use `smart-explore` for a one-off lookup |
| Deciding how much to build, or a design or diff that feels heavy | `ponytail` in `lite` mode (`/ponytail lite`). `ponytail:ponytail-review` on a diff, `ponytail:ponytail-audit` on the repo |

**Starting a spec:**
1. `claude-mem:mem-search` for earlier work and decisions in that area.
2. Read the team task sheet rows for the feature (Google Sheet "PHÂN CHIA TASK", tab `TASK`). Its BUSINESS RULE column is the contract the frontend builds against and wins where it differs from the SRS. blog-service was designed from the SRS alone, and six rules had to be reworked after the fact.
3. Map the code the spec touches: `graphify query` if the graph exists, otherwise `smart-explore`.
4. `superpowers:brainstorming` → spec in `docs/superpowers/specs/` → the user approves it.
5. `superpowers:writing-plans` → plan in `docs/superpowers/plans/`.

**How they fit together:**
- **superpowers owns the process; ponytail only shapes the solution.** ponytail's "never stall on an answer you can default" does not override brainstorming's approval gate, TDD, verification, or the `BlogServiceIntegrationTests` gate below. Use it to cut scope and speculative abstractions, never to skip a step.
- **`lite`, not ponytail's default `full`.** Features here are fixed by the task sheet and graded against it; `full` questions whether a task needs to exist at all, which is the wrong question for a required feature. `lite` builds what is asked and names the lazier alternative in one line.
- **Plan and execute with superpowers, not claude-mem's `make-plan` / `do`.** Both pipelines exist; specs and plans in this repo live under `docs/superpowers/`, so use one.

## Commands

```bash
# Start MySQL (host 3307, root/12345).
# Images live on Cloudinary: copy .env.example to .env at the repo root and fill CLOUDINARY_* (never commit .env).
docker compose up -d mysql   # compose also defines an identity-service container on :8081 — don't run it alongside mvnw

# Run a service (from its directory; on Windows use mvnw.cmd or Git Bash)
cd identity-service && ./mvnw spring-boot:run    # :8081
cd nutrition-service && ./mvnw spring-boot:run   # :8082
cd blog-service && ./mvnw spring-boot:run        # :8083
cd location-service && ./mvnw spring-boot:run    # :8085
cd api-gateway && ./mvnw spring-boot:run         # :8080

# Build / test
./mvnw clean package
./mvnw test
./mvnw test -Dtest=ProfileServiceTest                         # single class
./mvnw test -Dtest=ProfileServiceTest#changePassword_success_storesNewHash  # single method
./mvnw test -Dtest='!VeggiepalApplicationTests'               # identity-service: everything except the MySQL-backed contextLoads
./mvnw test -Dtest='!NutritionServiceApplicationTests'        # nutrition-service: same
./mvnw test -Dtest='!BlogServiceApplicationTests,!BlogServiceIntegrationTests'   # blog-service: same
./mvnw test -Dtest='!LocationServiceIntegrationTests'        # location-service: same

# blog-service only: the tests that need a real database, run on their own
./mvnw test -Dtest='BlogServiceApplicationTests,BlogServiceIntegrationTests'

# location-service only: the test that needs a real database
./mvnw test -Dtest=LocationServiceIntegrationTests
```

**`BlogServiceIntegrationTests` is a required gate before committing a change to an entity, a repository or a `@Query`** — not an optional extra. Two defects that made blog-service completely unusable (see the `@Lob` note below and `@Transactional` on the read methods) survived 126 mock-based tests and eight code reviews, because the one test that starts a Spring context was excluded from the loop for eight consecutive tasks while four of them added queries. The fast command above still excludes both so the loop stays runnable without Docker; the second command is what you owe the change.

In `-Dtest`, separate classes with a comma. `+` only joins methods inside one class (`Class#m1+m2`); `A+B` across classes matches nothing and fails with "No tests matching pattern".

There is no linter or formatter configured.

**Database gotchas:**
- `docker-compose.yml` creates a database named `veggiepal`. identity-service connects to `veggiepal_identity` without `createDatabaseIfNotExist`, so create it by hand: `docker exec veggiepal-mysql mysql -uroot -p12345 -e "CREATE DATABASE IF NOT EXISTS veggiepal_identity"`. nutrition-service creates `veggiepal_nutrition` itself, blog-service creates `veggiepal_blog` itself the same way, and location-service creates `veggiepal_location`.
- Tables come from Hibernate `ddl-auto=update`; there are no migrations. nutrition-service seeds the `allergens` catalog from `src/main/resources/data.sql` (`INSERT IGNORE`, runs on every start).
- Images live on Cloudinary. identity-service and blog-service read `CLOUDINARY_CLOUD_NAME` / `CLOUDINARY_API_KEY` / `CLOUDINARY_API_SECRET` from the repo-root `.env` through `spring.config.import=optional:file:.env[.properties],optional:file:../.env[.properties]` (`../.env` because Maven runs inside the service directory). `.env` is gitignored; `.env.example` is the template. The properties default to empty, so every context (including `BlogServiceIntegrationTests`) loads without `.env` and only uploads fail with 503.
- The `@SpringBootTest` `contextLoads` tests use the same MySQL (no test profile or H2). Unit tests and `@WebMvcTest` tests need no database.
- Hibernate maps `@Enumerated(EnumType.STRING)` to a native MySQL `ENUM` column. `ddl-auto=update` does not add new constants to it, so adding an enum value needs a manual `ALTER TABLE ... MODIFY COLUMN`. This happened with `ContentStatus.BANNED`: a database created before it existed rejects the value and the admin takedown answers 500 until the column is altered.
- `@Lob` on a `String` field maps it to CLOB in Hibernate 6+, and `lower()`/`like` against a CLOB fails query validation at application startup (this took down blog-service entirely once a `search` query added `lower()` on a `@Lob` column). A text column that needs searching should get its real column type from `columnDefinition` alone, without `@Lob`.

## Architecture

### Request flow through the gateway

`api-gateway` uses **Spring Cloud Gateway Server WebMVC** (servlet-based, not the reactive WebFlux gateway). Routes are defined in `api-gateway/src/main/resources/application.yaml`, and downstream URIs are hardcoded `localhost` ports (no service discovery).

- Routes (all `StripPrefix=1`): `/api/auth/**` and `/api/users/**` → identity-service (8081); `/api/nutrition/**` → nutrition-service (8082); `/api/blogs/**`, `/api/categories/**`, `/api/comments/**` → blog-service (8083); `/api/restaurants/**` → location-service (8085).
- Do not set `spring.servlet.multipart.*` in api-gateway: the gateway disables multipart parsing on its own so file uploads stream through to the service.
- Controllers in a service therefore map paths **without** the `/api` prefix, and identity-service's `SecurityConfig` matchers use the un-prefixed paths (`/auth/login`).
- CORS is configured **only** in the gateway (`CorsConfig`, allowing `http://localhost:*` with credentials). The frontend must go through the gateway.

### Swagger aggregation

The gateway serves a combined Swagger UI at `http://localhost:8080/swagger-ui.html`. The pieces fit together like this:
1. A gateway route `/identity-service/v3/api-docs/**` with `StripPrefix=1` forwards to the service's `/v3/api-docs`.
2. An entry under `springdoc.swagger-ui.urls` in the gateway yaml points at that route.
3. The service's `OpenApiConfig` sets the server URL to `/api`, so "Try it out" requests go back through the gateway.

A new service needs all three: an API route, a docs route, and a springdoc `urls` entry. It also needs its own `OpenApiConfig`. Each service's `OpenApiConfig` except location-service's (it has no authenticated endpoint) also declares the `bearerAuth` scheme so the Swagger **Authorize** button sends the JWT.

### identity-service conventions

- **Layering:** `controller` → `service` → `repository` (Spring Data JPA). `mapper` holds MapStruct interfaces (`componentModel = "spring"`), and `dto/request` and `dto/response` hold the DTOs.
- **Dependency injection style:** `@RequiredArgsConstructor` + `@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)` with fields declared bare (Lombok adds `private final`).
- **Response envelope:** every endpoint returns `ApiResponse<T>` (`code` defaults to `1000` on success, plus `message` and `result`; null fields are omitted).
- **Errors:** throw `new AppException(ErrorCode.X)`. `GlobalExceptionHandler` maps it to `ApiResponse` using the enum's code and HTTP status. Add new cases to the `ErrorCode` enum.
- **Validation messages are `ErrorCode` enum names**, e.g. `@NotBlank(message = "EMAIL_REQUIRED")`. The handler resolves them with `ErrorCode.valueOf(...)`, and an unknown name falls back to `INVALID_KEY`. A `{min}` placeholder in the enum message is filled from the constraint's `min` attribute. The handler only looks at the first field error.
- **MapStruct + Lombok:** both annotation processors are listed in `maven-compiler-plugin` (Lombok first). When mapping a request DTO onto an entity, explicitly `@Mapping(target = ..., ignore = true)` any fields the service sets itself (see `UserMapper`).
- Emails are normalized with `trim().toLowerCase()` before lookup or storage.
- **Error code ranges:** shared codes keep the same number in every service (1001, 1008, 1009, 1018 `INVALID_REQUEST`, 9999); identity-service uses 10xx, nutrition-service 20xx, blog-service 30xx, recipe-service 40xx, location-service 50xx.
- **Avatar storage:** `FileStorageService` → `CloudinaryFileStorageService` (Cloudinary Java SDK, `storage.cloudinary.*`). The storage key minus its extension becomes the `public_id` (Cloudinary appends the format itself). `delete` only acts on `https://res.cloudinary.com/<cloud>/image/upload/` URLs and ignores everything else, old MinIO URLs included. Uploads are validated by content type **and** magic bytes (`ImageTypeDetector`). blog-service thumbnails use a copy of the same classes.
- **Controller slice tests:** `@WebMvcTest(X.class)` + `@Import({SecurityConfig.class, JwtConfig.class, SecurityExceptionHandler.class})` + `@MockitoBean` for the service; authenticate with `SecurityMockMvcRequestPostProcessors.jwt().jwt(t -> t.claim("userId", 7L))`.

### nutrition-service

Same conventions as identity-service, under package `com.veggiepal.nutrition` (its shared classes are copies, not a shared module). Controllers map `/nutrition/**`. It owns health records (height/weight history; BMI is computed server-side with HALF_UP to 1 decimal) and allergies (seeded `allergens` catalog + `user_allergies`). It stores `userId` from the JWT and never calls identity-service.

### blog-service

Same conventions as identity-service, under package `com.veggiepal.blog` (shared classes are copies, not a shared module). Controllers map `/blogs/**`, `/categories/**`, `/comments/**`. It owns blogs, the category tree, comments and votes, and stores only `author_id` from the JWT — the frontend resolves display names through identity-service's `GET /users/batch`.

- **Comments and votes are polymorphic** (`target_type` + `target_id`) so videos slot in without a migration. `TargetType.VIDEO` and `CommentStatus.PENDING` already exist in the enums for the same reason — `ddl-auto=update` cannot add an ENUM constant later.
- **`SecurityConfig.PUBLIC_ENDPOINTS` here is method-aware** and its path variables are constrained to digits (`/blogs/{id:[0-9]+}`). Without the digits, `/blogs/me` matches `/blogs/{id}`, becomes public, loses its bearer token and then 401s forever. `SecurityConfigTest` guards this.
- **Moderation is a stub.** `ContentModerationService` has one implementation, `AutoApproveContentModerationService`. BR-02 is wired but not really enforced until an AI implementation replaces it.
- **`blogs.vote_score` is denormalized**, kept in sync inside the vote transaction with `UPDATE blogs SET vote_score = vote_score + :delta`. The delta is just `new value - old value`, treating "no vote" as 0.
- **Voting is `POST /blogs/{id}/vote` and toggles**: the same value a second time takes the vote back (the task sheet's heart button sends `1` on every click), the opposite value switches it. It is POST, not PUT, because it is not idempotent. `DELETE /blogs/{id}/vote` still removes explicitly.
- **Keyword search ranks title matches first** (task sheet US7): `BlogRepository.search` orders by `case when title like %kw% then 0 else 1 end` ahead of the `Pageable` sort, so `sort` only orders rows within each tier. It stays `LIKE`, not FULLTEXT, on purpose: InnoDB FULLTEXT skips 2-letter Vietnamese syllables ("bò", "cà") and matches whole words, which breaks the FE's search-as-you-type from 2 characters. The default `ai_ci` collation makes it accent-insensitive ("pho" finds "Phở").
- Admin has no separate controller: ownership checks widen to `ROLE_ADMIN` on blog and comment `PUT`/`DELETE`. **An admin deleting someone else's blog bans it** (`ContentStatus.BANNED`) rather than removing the row: hidden from every public read, still listed in the owner's `GET /blogs/me`. `BANNED` is terminal — `updateBlog` refuses it, because editing re-runs moderation and would otherwise let the owner lift the ban. An owner deleting their own post is a real delete.
- **Category names are unique across the whole tree**, not per parent, enforced by the service and by `uk_categories_name`. The column is `utf8mb4_0900_as_ci` on purpose: MySQL's default `ai_ci` ignores diacritics, so "Che", "Chè" and "Chế" would collide both in the lookup and in the index.
- **`BlogServiceIntegrationTests`** drives the real filter chain and a real MySQL schema (`veggiepal_blog_it`, created on demand via `createDatabaseIfNotExist`, so it never leaves rows in `veggiepal_blog`). Each case is chosen to fail if one of the two defect classes returns: a keyword search forces `lower()` against the real content column, the list assertions read `categoryName` off a lazy proxy, and reading a blog twice checks the `@Modifying` view counter actually ran. Verified by mutation — restoring `@Lob` breaks the context, dropping `@Transactional` turns the list endpoints into 500s.

### location-service

Package `com.veggiepal.location`, port 8085, controllers map `/restaurants/**`. Everything it serves is public and read-only, so it has **no Spring Security and no JWT**: copy `SecurityConfig`, `JwtConfig` and `SecurityExceptionHandler` from blog-service before adding an endpoint that needs a login. `ApiResponse`, `AppException`, `ErrorCode` and `GlobalExceptionHandler` are trimmed copies.

- `GET /restaurants/nearby?lat&lng[&radiusKm=5][&veganOnly=false]` (task sheet US15): `radiusKm` must be in (0, 10]. Results are nearest first and not paged.
- The `vegetarian_restaurants` table is seeded from `src/main/resources/data.sql` (upsert by id on every start, so editing a row takes effect on the next start; removing a row from the file does not delete it). There is no admin CRUD; edit the seed. Every seeded row currently has `is_vegan_only = 1`.
- The search is a native query on `ST_Distance_Sphere(point(longitude, latitude), ...)`. **x is longitude**: `point(lat, lng)` makes MySQL reject the "latitude" (~106) as out of range.
- **`LocationServiceIntegrationTests` is a required gate** before committing a change to the entity, the repository or `data.sql`: a native query is not validated at startup and a mocked repository never runs it. Its expected values are distances from Bến Thành market pinned to the seed, so moving or adding a restaurant near District 1 means recomputing them.

### Auth (JWT)

- identity-service issues tokens in `JwtService`: HS256 (explicit), subject = email, claims `userId` and `role`, 24h expiry.
- Every service except location-service validates tokens as an OAuth2 Resource Server (`JwtConfig` builds a `NimbusJwtDecoder` with the same secret and HS256). The secret is `jwt.secret=${JWT_SECRET:...}` and must be identical in every service that validates tokens.
- `SecurityConfig.PUBLIC_ENDPOINTS` is the single list used for both `permitAll` and a `BearerTokenResolver` that ignores the Authorization header on public paths. Without it, a stale token would make `/auth/login` return 401.
- 401/403 are written by `SecurityExceptionHandler` as `ApiResponse` (1008/1009), because filter-chain errors never reach `@ControllerAdvice`.
- Controllers take `@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt` and call `CurrentUser.id(jwt)`. Never take the user id from the body or the path.
- Tokens stay valid until they expire (no revocation, even after a password change).
- **`@PreAuthorize` trap:** a method-security denial (`AuthorizationDeniedException`, a subclass of `AccessDeniedException`) is thrown by the AOP proxy *while the handler is being invoked*, not in the filter chain, so the catch-all `@ExceptionHandler(Exception.class)` in `GlobalExceptionHandler` catches it first and turns a 403 into a 500. blog-service (the first service to use `@PreAuthorize`, on `CategoryController`) works around this with an `@ExceptionHandler(AccessDeniedException.class)` that just rethrows, letting it propagate to `SecurityExceptionHandler`. identity-service and nutrition-service have the same catch-all and no such handler — the moment either adds `@PreAuthorize`, add this rethrow-handler first.
- A missing required query parameter (`MissingServletRequestParameterException`) maps to 400/`INVALID_REQUEST` in identity-service, blog-service and location-service's exception handlers, not the 500 a plain catch-all would give it.
