# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

VeggiePal backend: Spring Boot microservices (Java 21, Spring Boot 4.1.1). There is **no parent/aggregator POM**. Each service (`api-gateway/`, `identity-service/`, `nutrition-service/`, `blog-service/`) is a separate Maven project with its own wrapper, so run Maven commands from inside the service directory. Only `api-gateway` uses `application.yaml`; every other service, including `blog-service`, uses `application.properties`.

## Commands

```bash
# Start MySQL (host 3307, root/12345) and MinIO (API 9000, console 9001, minioadmin/minioadmin).
# minio-init creates the public-read buckets veggiepal-avatars and veggiepal-blog-thumbnails.
docker compose up -d

# Run a service (from its directory; on Windows use mvnw.cmd or Git Bash)
cd identity-service && ./mvnw spring-boot:run    # :8081
cd nutrition-service && ./mvnw spring-boot:run   # :8082
cd blog-service && ./mvnw spring-boot:run        # :8083
cd api-gateway && ./mvnw spring-boot:run         # :8080

# Build / test
./mvnw clean package
./mvnw test
./mvnw test -Dtest=ProfileServiceTest                         # single class
./mvnw test -Dtest=ProfileServiceTest#changePassword_success_storesNewHash  # single method
./mvnw test -Dtest='!VeggiepalApplicationTests'               # identity-service: everything except the MySQL-backed contextLoads
./mvnw test -Dtest='!NutritionServiceApplicationTests'        # nutrition-service: same
./mvnw test -Dtest='!BlogServiceApplicationTests'             # blog-service: same
```

There is no linter or formatter configured.

**Database gotchas:**
- `docker-compose.yml` creates a database named `veggiepal`. identity-service connects to `veggiepal_identity` without `createDatabaseIfNotExist`, so create it by hand: `docker exec veggiepal-mysql mysql -uroot -p12345 -e "CREATE DATABASE IF NOT EXISTS veggiepal_identity"`. nutrition-service creates `veggiepal_nutrition` itself, and blog-service creates `veggiepal_blog` itself the same way.
- Tables come from Hibernate `ddl-auto=update`; there are no migrations. nutrition-service seeds the `allergens` catalog from `src/main/resources/data.sql` (`INSERT IGNORE`, runs on every start).
- `minio-init` creates two public-read buckets: `veggiepal-avatars` (identity-service) and `veggiepal-blog-thumbnails` (blog-service).
- The `@SpringBootTest` `contextLoads` tests use the same MySQL (no test profile or H2). Unit tests and `@WebMvcTest` tests need no database.
- Hibernate maps `@Enumerated(EnumType.STRING)` to a native MySQL `ENUM` column. `ddl-auto=update` does not add new constants to it, so adding an enum value needs a manual `ALTER TABLE ... MODIFY COLUMN`.
- `@Lob` on a `String` field maps it to CLOB in Hibernate 6+, and `lower()`/`like` against a CLOB fails query validation at application startup (this took down blog-service entirely once a `search` query added `lower()` on a `@Lob` column). A text column that needs searching should get its real column type from `columnDefinition` alone, without `@Lob`.

## Architecture

### Request flow through the gateway

`api-gateway` uses **Spring Cloud Gateway Server WebMVC** (servlet-based, not the reactive WebFlux gateway). Routes are defined in `api-gateway/src/main/resources/application.yaml`, and downstream URIs are hardcoded `localhost` ports (no service discovery).

- Routes (all `StripPrefix=1`): `/api/auth/**` and `/api/users/**` → identity-service (8081); `/api/nutrition/**` → nutrition-service (8082); `/api/blogs/**`, `/api/categories/**`, `/api/comments/**` → blog-service (8083).
- Do not set `spring.servlet.multipart.*` in api-gateway: the gateway disables multipart parsing on its own so file uploads stream through to the service.
- Controllers in a service therefore map paths **without** the `/api` prefix, and identity-service's `SecurityConfig` matchers use the un-prefixed paths (`/auth/login`).
- CORS is configured **only** in the gateway (`CorsConfig`, allowing `http://localhost:*` with credentials). The frontend must go through the gateway.

### Swagger aggregation

The gateway serves a combined Swagger UI at `http://localhost:8080/swagger-ui.html`. The pieces fit together like this:
1. A gateway route `/identity-service/v3/api-docs/**` with `StripPrefix=1` forwards to the service's `/v3/api-docs`.
2. An entry under `springdoc.swagger-ui.urls` in the gateway yaml points at that route.
3. The service's `OpenApiConfig` sets the server URL to `/api`, so "Try it out" requests go back through the gateway.

A new service needs all three: an API route, a docs route, and a springdoc `urls` entry. It also needs its own `OpenApiConfig`. Each service's `OpenApiConfig` also declares the `bearerAuth` scheme so the Swagger **Authorize** button sends the JWT.

### identity-service conventions

- **Layering:** `controller` → `service` → `repository` (Spring Data JPA). `mapper` holds MapStruct interfaces (`componentModel = "spring"`), and `dto/request` and `dto/response` hold the DTOs.
- **Dependency injection style:** `@RequiredArgsConstructor` + `@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)` with fields declared bare (Lombok adds `private final`).
- **Response envelope:** every endpoint returns `ApiResponse<T>` (`code` defaults to `1000` on success, plus `message` and `result`; null fields are omitted).
- **Errors:** throw `new AppException(ErrorCode.X)`. `GlobalExceptionHandler` maps it to `ApiResponse` using the enum's code and HTTP status. Add new cases to the `ErrorCode` enum.
- **Validation messages are `ErrorCode` enum names**, e.g. `@NotBlank(message = "EMAIL_REQUIRED")`. The handler resolves them with `ErrorCode.valueOf(...)`, and an unknown name falls back to `INVALID_KEY`. A `{min}` placeholder in the enum message is filled from the constraint's `min` attribute. The handler only looks at the first field error.
- **MapStruct + Lombok:** both annotation processors are listed in `maven-compiler-plugin` (Lombok first). When mapping a request DTO onto an entity, explicitly `@Mapping(target = ..., ignore = true)` any fields the service sets itself (see `UserMapper`).
- Emails are normalized with `trim().toLowerCase()` before lookup or storage.
- **Error code ranges:** shared codes keep the same number in every service (1001, 1008, 1009, 1018 `INVALID_REQUEST`, 9999); identity-service uses 10xx, nutrition-service uses 20xx.
- **Avatar storage:** `FileStorageService` (S3 API via AWS SDK v2; MinIO locally). Configured with `storage.s3.*` / `S3_*` env vars. Uploads are validated by content type **and** magic bytes (`ImageTypeDetector`).
- **Controller slice tests:** `@WebMvcTest(X.class)` + `@Import({SecurityConfig.class, JwtConfig.class, SecurityExceptionHandler.class})` + `@MockitoBean` for the service; authenticate with `SecurityMockMvcRequestPostProcessors.jwt().jwt(t -> t.claim("userId", 7L))`.

### nutrition-service

Same conventions as identity-service, under package `com.veggiepal.nutrition` (its shared classes are copies, not a shared module). Controllers map `/nutrition/**`. It owns health records (height/weight history; BMI is computed server-side with HALF_UP to 1 decimal) and allergies (seeded `allergens` catalog + `user_allergies`). It stores `userId` from the JWT and never calls identity-service.

### blog-service

Same conventions as identity-service, under package `com.veggiepal.blog` (shared classes are copies, not a shared module). Controllers map `/blogs/**`, `/categories/**`, `/comments/**`. It owns blogs, the category tree, comments and votes, and stores only `author_id` from the JWT — the frontend resolves display names through identity-service's `GET /users/batch`.

- **Comments and votes are polymorphic** (`target_type` + `target_id`) so videos slot in without a migration. `TargetType.VIDEO` and `CommentStatus.PENDING` already exist in the enums for the same reason — `ddl-auto=update` cannot add an ENUM constant later.
- **`SecurityConfig.PUBLIC_ENDPOINTS` here is method-aware** and its path variables are constrained to digits (`/blogs/{id:[0-9]+}`). Without the digits, `/blogs/me` matches `/blogs/{id}`, becomes public, loses its bearer token and then 401s forever. `SecurityConfigTest` guards this.
- **Moderation is a stub.** `ContentModerationService` has one implementation, `AutoApproveContentModerationService`. BR-02 is wired but not really enforced until an AI implementation replaces it.
- **`blogs.vote_score` is denormalized**, kept in sync inside the vote transaction with `UPDATE blogs SET vote_score = vote_score + :delta`. The delta is just `new value - old value`, treating "no vote" as 0.
- Admin has no separate controller: ownership checks widen to `ROLE_ADMIN` on blog and comment `PUT`/`DELETE`.

### Auth (JWT)

- identity-service issues tokens in `JwtService`: HS256 (explicit), subject = email, claims `userId` and `role`, 24h expiry.
- Every service validates tokens as an OAuth2 Resource Server (`JwtConfig` builds a `NimbusJwtDecoder` with the same secret and HS256). The secret is `jwt.secret=${JWT_SECRET:...}` and must be identical in every service.
- `SecurityConfig.PUBLIC_ENDPOINTS` is the single list used for both `permitAll` and a `BearerTokenResolver` that ignores the Authorization header on public paths. Without it, a stale token would make `/auth/login` return 401.
- 401/403 are written by `SecurityExceptionHandler` as `ApiResponse` (1008/1009), because filter-chain errors never reach `@ControllerAdvice`.
- Controllers take `@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt` and call `CurrentUser.id(jwt)`. Never take the user id from the body or the path.
- Tokens stay valid until they expire (no revocation, even after a password change).
- **`@PreAuthorize` trap:** a method-security denial (`AuthorizationDeniedException`, a subclass of `AccessDeniedException`) is thrown by the AOP proxy *while the handler is being invoked*, not in the filter chain, so the catch-all `@ExceptionHandler(Exception.class)` in `GlobalExceptionHandler` catches it first and turns a 403 into a 500. blog-service (the first service to use `@PreAuthorize`, on `CategoryController`) works around this with an `@ExceptionHandler(AccessDeniedException.class)` that just rethrows, letting it propagate to `SecurityExceptionHandler`. identity-service and nutrition-service have the same catch-all and no such handler — the moment either adds `@PreAuthorize`, add this rethrow-handler first.
- A missing required query parameter (`MissingServletRequestParameterException`) maps to 400/`INVALID_REQUEST` in identity-service and blog-service's exception handlers, not the 500 a plain catch-all would give it.
