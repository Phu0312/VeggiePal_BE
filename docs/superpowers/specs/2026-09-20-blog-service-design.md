# Blog Service — Design Spec

| Thuộc tính | Giá trị |
|---|---|
| Ngày | 2026-09-20 |
| Nhánh | `feature/blog-service` |
| Trạng thái | Chờ review |
| Tài liệu gốc | VeggiePalApp SRS v1.0: UC-02, UC-03, UC-04, UC-05, UC-10, UC-11; BR-02, BR-03, BR-07, BR-08; mục 3.3, 6.1, 6.2, 10 — và tài liệu Requirement/Entities/ERD của nhóm |

## 1. Mục tiêu

Xây dựng service nội dung cộng đồng cho VeggiePal. Sau module này:

1. Khách chưa đăng nhập tìm kiếm và đọc được blog công khai (UC-02).
2. Thành viên viết, sửa, xóa blog của chính mình, upload ảnh bìa (UC-03).
3. Thành viên bình luận, trả lời bình luận và vote lên blog của người khác (UC-04).
4. Người dùng tìm blog theo từ khóa và xem nội dung liên quan (UC-05).
5. Admin quản lý danh mục nội dung, và gỡ được blog/bình luận vi phạm (UC-10, UC-11).

Module này là chỗ cắm sẵn cho hai thứ chưa có: **video** (cùng service) và **AI moderation** (thay implement).

## 2. Phạm vi

**Trong phạm vi**

- `blog-service` (service mới): blogs, categories, comments, votes.
- `identity-service`: thêm endpoint `GET /users/batch` để FE resolve tên và avatar tác giả.
- `api-gateway`: thêm 4 route và một mục Swagger.
- `docker-compose.yml`: thêm bucket MinIO cho ảnh bìa.
- Cập nhật `CLAUDE.md`.

**Ngoài phạm vi**

- Video (`videos`, upload/nhúng YouTube, `video_ai_summaries`). Data model đã chừa chỗ, xem mục 4.5.
- `moderation_cases` và màn hình hàng chờ kiểm duyệt của Admin (UC-12 FR-12-02..05).
- `admin_logs` (audit log thao tác admin).
- AI thật cho moderation và cho gợi ý nội dung liên quan (FR-05-03 có bản không-AI, xem 6.3).
- `recipes` và liên kết `blog.recipe_id`.
- Full-text search / Elasticsearch — SRS mục 10 ghi rõ không bắt buộc.
- Integration test có database thật (H2 hoặc Testcontainers).
- Đếm view chống spam (mỗi lần GET là một view).

## 3. Kiến trúc

```
FE ──► api-gateway :8080 ─┬─► identity-service  :8081 ──► MySQL veggiepal_identity
                          ├─► nutrition-service :8082 ──► MySQL veggiepal_nutrition
                          └─► blog-service      :8083 ─┬─► MySQL veggiepal_blog
                                                       └─► MinIO / S3 (ảnh bìa blog)
```

### 3.1 Phân chia trách nhiệm

| Service | Trách nhiệm |
|---|---|
| identity-service | Tài khoản, profile, avatar, đổi mật khẩu; **mới**: trả thông tin công khai của user theo lô |
| nutrition-service | Health records, allergens; sau này là Meal Planner |
| blog-service | Blog, danh mục, bình luận, vote; sau này là video |

`blog-service` **không gọi sang identity-service** và **không có khóa ngoại tới bảng `users`**, giống nutrition-service. `author_id` / `user_id` lấy từ claim `userId` trong JWT.

Các class dùng chung (`ApiResponse`, `PageResponse`, `AppException`, `ErrorCode`, `GlobalExceptionHandler`, `SecurityConfig`, `JwtConfig`, `SecurityExceptionHandler`, `CurrentUser`, `OpenApiConfig`) được **copy** sang, không tạo shared module — repo không có parent POM và mỗi service phải build độc lập.

Project mới: Maven riêng, wrapper riêng, `spring-boot-starter-parent` 4.1.1, Java 21, package `com.veggiepal.blog`. Dependency lấy đúng danh sách của nutrition-service, cộng thêm AWS SDK v2 S3 (cho ảnh bìa) như identity-service.

### 3.2 Gateway routes

Thêm vào `api-gateway/src/main/resources/application.yaml`. Mọi route dùng `StripPrefix=1`, nên controller map path **không có** tiền tố `/api`.

| Route id | Predicate | URI | Controller map |
|---|---|---|---|
| `blog-service-blogs` | `Path=/api/blogs/**` | `http://localhost:8083` | `/blogs/**` |
| `blog-service-categories` | `Path=/api/categories/**` | `http://localhost:8083` | `/categories/**` |
| `blog-service-comments` | `Path=/api/comments/**` | `http://localhost:8083` | `/comments/**` |
| `blog-service-docs` | `Path=/blog-service/v3/api-docs/**` | `http://localhost:8083` | `/v3/api-docs` |

Tách theo tài nguyên thay vì gom một tiền tố `/api/blog/**`, theo đúng tiền lệ của identity-service (`/api/auth/**`, `/api/users/**`) và để URL không thành `/api/blog/blogs`.

Thêm vào `springdoc.swagger-ui.urls`: `{name: Blog Service, url: /blog-service/v3/api-docs}`.

`OpenApiConfig` của blog-service đặt server URL `/api` và khai báo scheme `bearerAuth`, để nút **Authorize** trên Swagger tổng gửi kèm JWT.

Gateway **không** khai báo `spring.servlet.multipart.*` — giữ nguyên như hiện tại, để upload ảnh bìa stream thẳng xuống service.

### 3.3 Xác thực và phân quyền

blog-service là OAuth2 Resource Server, dùng chung secret HMAC `jwt.secret=${JWT_SECRET:...}` và thuật toán HS256 với hai service kia. Cấu hình `SecurityConfig` copy nguyên từ nutrition-service, chỉ đổi danh sách endpoint public.

`PUBLIC_ENDPOINTS` phải phân biệt theo **method**, vì `/blogs` là public với `GET` nhưng cần đăng nhập với `POST`. Dùng `PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, pattern)` thay cho matcher chỉ có pattern.

| Method | Pattern |
|---|---|
| mọi method | `/swagger-ui/**`, `/swagger-ui.html`, `/v3/api-docs/**` |
| GET | `/blogs`, `/blogs/{id:[0-9]+}`, `/blogs/{id:[0-9]+}/related` |
| GET | `/categories`, `/categories/{id:[0-9]+}` |
| GET | `/comments`, `/comments/{id:[0-9]+}/replies` |

**Ràng buộc `[0-9]+` trong path variable là bắt buộc, không phải trang trí.** `/blogs/me` khớp với pattern `/blogs/{id}`. Nếu để `{id}` trần, `/blogs/me` bị xếp vào nhóm public → `BearerTokenResolver` vứt token → endpoint `authenticated()` trả 401 vĩnh viễn, và triệu chứng nhìn như lỗi token chứ không ai nghĩ tới matcher. Khóa `{id}` về chữ số thì `/blogs/me` không còn khớp. `PathPatternParser` mà `PathPatternRequestMatcher` dùng hỗ trợ cú pháp `{name:regex}` này.

Phía controller **không cần** ràng buộc đó: Spring MVC ưu tiên segment chữ (`/me`) hơn segment template (`/{id}`) khi so khớp, nên `@GetMapping("/me")` thắng `@GetMapping("/{id}")` cho đường dẫn `/blogs/me`.

`PUBLIC_ENDPOINTS` vẫn là **một danh sách duy nhất** dùng cho cả `permitAll` lẫn `BearerTokenResolver`, đúng như hai service hiện có. Hệ quả: ở endpoint public, token bị bỏ qua hoàn toàn nên controller không biết ai đang gọi. Đây là lựa chọn có chủ ý — đổi lại, một token hết hạn không làm khách xem blog bị 401.

Dữ liệu riêng của user trên trang công khai được FE lấy bằng lời gọi riêng có token:

- Tên và avatar tác giả → `GET /api/users/batch?ids=...` (identity-service)
- Trạng thái vote của chính mình → `GET /api/blogs/me/votes?blogIds=...`

`JwtAuthenticationConverter` đọc claim `role`, thêm tiền tố `ROLE_` → `ROLE_USER` / `ROLE_ADMIN`. Endpoint admin dùng `@PreAuthorize("hasRole('ADMIN')")`, cần `@EnableMethodSecurity` trên `SecurityConfig`.

Giá trị role giữ nguyên `USER` / `ADMIN` như enum `Role` đang có trong code, **không** đổi thành `MEMBER` như tài liệu Entities ghi.

### 3.4 Hạ tầng

**Database:** `jdbc:mysql://localhost:3307/veggiepal_blog?createDatabaseIfNotExist=true` — service tự tạo DB, không phải tạo tay. Bảng sinh bằng Hibernate `ddl-auto=update`, không có migration, giống hai service kia.

**MinIO:** thêm bucket `veggiepal-blog-thumbnails` vào bước `minio-init` trong `docker-compose.yml`:

```
mc mb --ignore-existing local/veggiepal-blog-thumbnails &&
mc anonymous set download local/veggiepal-blog-thumbnails
```

Service không tự tạo bucket, để khi chạy trên S3 thật không cần quyền tạo bucket — giữ nguyên cách làm của identity-service.

**Biến môi trường** (đều có mặc định cho dev):

| Biến | Mặc định dev |
|---|---|
| `JWT_SECRET` | giống hai service kia |
| `S3_ENDPOINT` | `http://localhost:9000` |
| `S3_REGION` | `us-east-1` |
| `S3_ACCESS_KEY` / `S3_SECRET_KEY` | `minioadmin` / `minioadmin` |
| `S3_BUCKET` | `veggiepal-blog-thumbnails` |
| `S3_PUBLIC_URL` | `http://localhost:9000/veggiepal-blog-thumbnails` |

`FileStorageService`, `S3FileStorageService`, `ImageTypeDetector`, `S3Config`, `StorageProperties` copy từ identity-service. Ảnh bìa vẫn validate bằng **cả content type lẫn magic bytes**, giới hạn 5MB (avatar là 2MB).

### 3.5 Thay đổi ở identity-service

Thêm endpoint để FE resolve thông tin tác giả:

```
GET /users/batch?ids=1,2,3   →   ApiResponse<List<PublicUserResponse>>
```

`PublicUserResponse` chỉ có `id`, `fullName`, `avatarUrl`.

| Ràng buộc | Lý do |
|---|---|
| Public (không cần token) | Khách chưa đăng nhập cũng phải thấy tên tác giả bài viết |
| Chỉ trả user `status = ACTIVE` | Không lộ tài khoản bị khóa hoặc chưa kích hoạt |
| Tối đa 50 id mỗi lần, quá thì `INVALID_REQUEST` | Chặn quét sạch bảng users bằng một request |
| **Không** trả email, phone, ngày sinh | Chỉ đúng phần dữ liệu công khai hiển thị trên blog |
| Id không tồn tại thì bỏ qua, không báo lỗi | Response ngắn hơn danh sách hỏi là chuyện bình thường |

Thêm `/users/batch` vào `PUBLIC_ENDPOINTS` của identity-service. Route gateway `Path=/api/users/**` đã có sẵn nên không cần route mới.

Ở đây không dính cái bẫy pattern của mục 3.3: identity-service chỉ có `/users/me` (`ProfileController`), không có `/users/{id}`, nên `/users/batch` và `/users/me` đều là segment chữ, không khớp chéo nhau.

## 4. Data model

Mọi khóa chính là `Long` + `GenerationType.IDENTITY`, **không** dùng `CHAR(36)` UUID như tài liệu Entities — để đồng bộ với `User`, `HealthRecord`, `UserAllergy` đang có, và vì claim `userId` trong JWT đang là số.

Entity dùng `@Data @Builder @NoArgsConstructor @AllArgsConstructor @FieldDefaults(level = PRIVATE)`, timestamp set trong `@PrePersist` / `@PreUpdate` — theo đúng `HealthRecord`.

```
categories ──┐ (parent_id, tự tham chiếu, tối đa 2 cấp)
     │       │
     │ 1..N  │
     ▼       │
   blogs ◄───┘
     ▲
     │ (target_type = BLOG, target_id — không FK, validate ở tầng service)
     ├── comments ──┐ (parent_comment_id, tự tham chiếu, reply 1 cấp)
     │       ▲      │
     │       └──────┘
     └── content_votes
```

### 4.1 `categories`

| Cột | Kiểu | Ràng buộc |
|---|---|---|
| `id` | BIGINT | PK, auto |
| `parent_id` | BIGINT | NULL, FK → `categories.id` |
| `type` | ENUM | `FOOD_TYPE` / `RECIPE_TYPE`, NOT NULL |
| `name` | VARCHAR(100) | NOT NULL |
| `display_order` | SMALLINT | NOT NULL, mặc định 0 |
| `is_active` | BOOLEAN | NOT NULL, mặc định true |
| `created_at`, `updated_at` | DATETIME | NOT NULL |

Index: `idx_categories_parent (parent_id, display_order)`, `idx_categories_type (type, is_active)`.

**Tối đa 2 cấp.** Category đã có `parent_id` thì không được làm cha của category khác → `CATEGORY_DEPTH_EXCEEDED`. Tài liệu ERD gợi ý cây sâu tùy ý, nhưng chặn ở 2 cấp thì loại bỏ hoàn toàn khả năng tạo vòng lặp mà vẫn đủ cho ví dụ trong tài liệu (*Công thức → Món chính / Món phụ / Đồ uống*).

**Trùng tên** trong cùng một `parent_id` bị từ chối → `CATEGORY_NAME_DUPLICATED`. Check ở service chứ không dùng unique constraint, vì MySQL coi các `NULL` là khác nhau nên unique `(parent_id, name)` không chặn được category gốc trùng tên.

**Xóa** (FR-11-03, UC-11 luồng thay thế): từ chối nếu còn blog tham chiếu hoặc còn category con → `CATEGORY_IN_USE`. Muốn ẩn danh mục mà không xóa thì đặt `is_active = false`; blog cũ vẫn giữ nguyên, chỉ không chọn được khi tạo bài mới.

`is_active` đáp ứng cột `status` mà SRS mục 6.1 liệt kê cho Categories.

### 4.2 `blogs`

| Cột | Kiểu | Ràng buộc |
|---|---|---|
| `id` | BIGINT | PK, auto |
| `author_id` | BIGINT | NOT NULL, từ JWT, không FK |
| `category_id` | BIGINT | NOT NULL, FK → `categories.id` |
| `title` | VARCHAR(200) | NOT NULL |
| `content` | LONGTEXT | NOT NULL |
| `thumbnail_url` | VARCHAR(512) | NULL |
| `status` | ENUM | `DRAFT` / `PENDING` / `PUBLISHED` / `REJECTED`, NOT NULL |
| `view_count` | INT | NOT NULL, mặc định 0 |
| `vote_score` | INT | NOT NULL, mặc định 0 |
| `published_at` | DATETIME | NULL |
| `created_at`, `updated_at` | DATETIME | NOT NULL |

Index:

| Index | Phục vụ |
|---|---|
| `idx_blogs_status_published (status, published_at DESC)` | Feed công khai |
| `idx_blogs_author (author_id, created_at DESC)` | `GET /blogs/me` |
| `idx_blogs_category (category_id, status)` | Lọc theo danh mục, và check `CATEGORY_IN_USE` |

`view_count` tăng bằng một câu lệnh atomic, **không** đọc-sửa-ghi:

```sql
UPDATE blogs SET view_count = view_count + 1 WHERE id = :id
```

Hai request đồng thời theo kiểu đọc-sửa-ghi sẽ ăn mất lượt của nhau.

`published_at` chỉ set lần đầu tiên bài chuyển sang `PUBLISHED`; sửa bài rồi publish lại không ghi đè, để thứ tự feed không nhảy lung tung.

**Không có `recipe_id`** so với tài liệu Entities: `recipes` chưa tồn tại và nhiều khả năng thuộc nutrition-service. Thêm sau chỉ là một cột nullable.

### 4.3 `comments`

| Cột | Kiểu | Ràng buộc |
|---|---|---|
| `id` | BIGINT | PK, auto |
| `author_id` | BIGINT | NOT NULL, không FK |
| `target_type` | ENUM | `BLOG` / `VIDEO`, NOT NULL |
| `target_id` | BIGINT | NOT NULL, không FK |
| `parent_comment_id` | BIGINT | NULL, FK → `comments.id` |
| `content` | TEXT | NOT NULL, validate ≤ 2000 ký tự ở DTO |
| `status` | ENUM | `PENDING` / `VISIBLE` / `HIDDEN` / `DELETED`, NOT NULL |
| `created_at`, `updated_at` | DATETIME | NOT NULL |

Index: `idx_comments_target (target_type, target_id, created_at)`, `idx_comments_parent (parent_comment_id, created_at)`, `idx_comments_author (author_id, created_at DESC)`.

**Reply đúng 1 cấp** như tài liệu ERD mô tả. `parent_comment_id` phải trỏ tới comment có `parent_comment_id IS NULL` → nếu không, `COMMENT_REPLY_TOO_DEEP`. Comment cha phải cùng `target_type` và `target_id` → nếu không, `INVALID_COMMENT_PARENT`.

**Xóa mềm**: chuyển `status = DELETED`, giữ nguyên row để các reply bên dưới không mồ côi. Response trả `content = null` và `deleted = true`; FE hiển thị "Bình luận đã bị xóa".

`target_id` không có FK xuống `blogs`. Bù lại, service kiểm tra blog tồn tại và đang `PUBLISHED` trước khi tạo comment → `COMMENT_TARGET_NOT_EXISTED`.

### 4.4 `content_votes`

| Cột | Kiểu | Ràng buộc |
|---|---|---|
| `id` | BIGINT | PK, auto |
| `user_id` | BIGINT | NOT NULL, không FK |
| `target_type` | ENUM | `BLOG` / `VIDEO`, NOT NULL |
| `target_id` | BIGINT | NOT NULL, không FK |
| `value` | TINYINT | NOT NULL, chỉ nhận `-1` hoặc `1` |
| `created_at`, `updated_at` | DATETIME | NOT NULL |

UNIQUE `uk_content_votes_user_target (user_id, target_type, target_id)` — chính là FR-04-03 "không tạo vote trùng", để database chặn thay vì tin vào code.

Tài liệu Entities dùng khóa chính tổ hợp `(user_id, blog_id)`. Ở đây dùng khóa thay thế `id` cộng unique constraint, vì JPA làm việc với khóa đơn dễ hơn nhiều mà hiệu lực ràng buộc là như nhau.

`value` là `TINYINT` chứ không phải enum, để cộng dồn `vote_score` bằng số học được luôn.

### 4.5 Enum và một lưu ý về `ddl-auto=update`

| Enum | Giá trị |
|---|---|
| `ContentStatus` | `DRAFT`, `PENDING`, `PUBLISHED`, `REJECTED` |
| `CommentStatus` | `PENDING`, `VISIBLE`, `HIDDEN`, `DELETED` |
| `TargetType` | `BLOG`, `VIDEO` |
| `CategoryType` | `FOOD_TYPE`, `RECIPE_TYPE` |

`CLAUDE.md` ghi: Hibernate map `@Enumerated(EnumType.STRING)` thành cột `ENUM` native của MySQL, và `ddl-auto=update` **không tự thêm hằng số mới** — muốn thêm phải `ALTER TABLE ... MODIFY COLUMN` bằng tay.

Vì vậy hai giá trị chưa dùng tới vẫn được khai báo ngay từ đợt này:

- `TargetType.VIDEO` — khi làm video, không phải ALTER cả `comments` lẫn `content_votes` trên mọi môi trường.
- `CommentStatus.PENDING` — khi AI moderation chạy bất đồng bộ, không phải ALTER `comments`.

`targetType = VIDEO` gửi lên ở đợt này bị từ chối ở tầng service với `UNSUPPORTED_TARGET_TYPE`. Cột chấp nhận giá trị đó, nhưng API thì chưa.

## 5. API

Mọi endpoint trả `ApiResponse<T>` (`code` mặc định 1000, kèm `message` và `result`, field null bị lược bỏ). Controller nhận `@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt` và gọi `CurrentUser.id(jwt)`; **không bao giờ** lấy user id từ body hay path.

### 5.1 Categories — `/categories`

| Method | Path | Quyền | Mô tả |
|---|---|---|---|
| GET | `/categories?type=&activeOnly=` | Public | Cây 2 cấp, sắp theo `display_order` |
| GET | `/categories/{id}` | Public | |
| POST | `/categories` | ADMIN | |
| PUT | `/categories/{id}` | ADMIN | |
| DELETE | `/categories/{id}` | ADMIN | |

`activeOnly` mặc định `true` cho client công khai; màn hình admin gọi với `false` để thấy cả danh mục đã ẩn.

### 5.2 Blogs — `/blogs`

| Method | Path | Quyền | Mô tả |
|---|---|---|---|
| GET | `/blogs?page&size&categoryId&keyword&sort` | Public | Chỉ `PUBLISHED` |
| GET | `/blogs/{id}` | Public | Chỉ `PUBLISHED`, tăng `view_count` |
| GET | `/blogs/{id}/related` | Public | Cùng category, top 5 theo `vote_score` |
| POST | `/blogs` | Member | Body có cờ `publish` |
| GET | `/blogs/me?status=&page&size` | Member | Mọi status của chính mình |
| PUT | `/blogs/{id}` | Chủ sở hữu hoặc ADMIN | |
| DELETE | `/blogs/{id}` | Chủ sở hữu hoặc ADMIN | |
| POST | `/blogs/{id}/thumbnail` | Chủ sở hữu | multipart, ≤ 5MB |
| POST | `/blogs/{id}/submit` | Chủ sở hữu | `DRAFT` → gửi duyệt |
| PUT | `/blogs/{id}/vote` | Member | Body `{ "value": 1 }` hoặc `-1` |
| DELETE | `/blogs/{id}/vote` | Member | Bỏ vote, idempotent |
| GET | `/blogs/me/votes?blogIds=` | Member | Overlay trạng thái vote, tối đa 100 id |

`sort` nhận `newest` (mặc định), `popular` (`vote_score DESC`), `mostViewed` (`view_count DESC`). Giá trị lạ thì dùng `newest`, không báo lỗi.

Phân trang dùng `PageResponse<T>` và chặn `size` như `HealthRecordService` đang làm: `size` kẹp trong `[1, 100]`, `page` không âm.

### 5.3 Comments — `/comments`

| Method | Path | Quyền | Mô tả |
|---|---|---|---|
| GET | `/comments?targetType=&targetId=&page&size` | Public | Chỉ comment gốc, kèm `replyCount` |
| GET | `/comments/{id}/replies?page&size` | Public | |
| POST | `/comments` | Member | Body `{ targetType, targetId, parentCommentId?, content }` |
| PUT | `/comments/{id}` | Chủ sở hữu | |
| DELETE | `/comments/{id}` | Chủ sở hữu hoặc ADMIN | Xóa mềm |

Reply tách thành endpoint riêng thay vì lồng sẵn trong response comment gốc: nếu lồng, một bài nhiều tương tác có 500 reply sẽ trả payload không giới hạn trong một request.

Comment có `status != VISIBLE` không xuất hiện ở endpoint public, trừ `DELETED` — vẫn trả về nhưng `content = null`, để thread reply không bị đứt đoạn.

### 5.4 Ma trận phân quyền

Ánh xạ từ SRS mục 3.3:

| Chức năng | Guest | Member | Admin |
|---|:-:|:-:|:-:|
| Xem, tìm kiếm blog công khai | ✓ | ✓ | ✓ |
| Xem danh mục | ✓ | ✓ | ✓ |
| Xem bình luận | ✓ | ✓ | ✓ |
| Tạo / sửa / xóa blog của mình | — | ✓ | ✓ |
| Bình luận, trả lời bình luận | — | ✓ | ✓ |
| Vote blog của người khác | — | ✓ | ✓ |
| Gỡ blog / bình luận của người khác | — | — | ✓ |
| Tạo, sửa, xóa danh mục | — | — | ✓ |

Admin ở đợt này **không có controller riêng**. FR-10-03 và FR-10-04 được đáp ứng bằng cách nới điều kiện kiểm tra sở hữu trên `PUT` / `DELETE` thành `authorId.equals(currentUserId) || hasRole('ADMIN')` — đúng BR-07, không phát sinh API surface mới.

## 6. Luồng nghiệp vụ

### 6.1 Publish và moderation

Client **không bao giờ gửi `status` trực tiếp**. Nếu gửi được, member tự đặt `PUBLISHED` là đi vòng qua moderation, vi phạm BR-02. Thay vào đó `CreateBlogRequest` có cờ boolean `publish`.

```
POST /blogs { ..., publish: false }   → DRAFT, không gọi moderation
POST /blogs { ..., publish: true  }   → moderate()
POST /blogs/{id}/submit               → moderate()   (bài DRAFT đã lưu)
PUT  /blogs/{id} trên bài PUBLISHED   → moderate() lại   (BR-02)

moderate() ─► APPROVED → PUBLISHED, published_at = now() nếu còn null
          ├─► REJECTED → REJECTED
          └─► PENDING  → PENDING
```

`POST /blogs/{id}/submit` trên bài không ở trạng thái `DRAFT` → `INVALID_BLOG_STATUS_TRANSITION`.

`PUT /blogs/{id}` xử lý theo status hiện tại của bài:

| Status trước khi sửa | Sau khi sửa |
|---|---|
| `DRAFT` | vẫn `DRAFT`, **không** gọi moderation — bài nháp chưa công khai nên chưa cần duyệt |
| `PUBLISHED` | gọi `moderate()` lại (BR-02); `APPROVED` thì giữ `PUBLISHED`, ngược lại xuống `REJECTED` / `PENDING` |
| `REJECTED` | gọi `moderate()` lại — đây chính là cách người dùng sửa bài bị từ chối rồi gửi lại |
| `PENDING` | gọi `moderate()` lại |

Bài bị từ chối **không phải lỗi HTTP**: trả 200, `status = REJECTED`, kèm `moderationReason`. Lời gọi API đã thành công, chỉ là kết quả không như người dùng mong đợi.

Comment đi qua đúng cơ chế đó: `APPROVED` → `VISIBLE`, `REJECTED` → `HIDDEN`, `PENDING` → `PENDING`.

Điểm cắm cho AI:

```java
public interface ContentModerationService {
    ModerationResult moderate(String text);
}

public record ModerationResult(ModerationDecision decision, String reason) {}

public enum ModerationDecision { APPROVED, REJECTED, PENDING }
```

Đợt này chỉ có một implement, `AutoApproveContentModerationService`, luôn trả `APPROVED` với `reason = null`. Khi AI service có thật, thêm implement mới và bỏ implement cũ — `BlogService` và `CommentService` không phải sửa một dòng nào.

### 6.2 Vote và `vote_score`

`PUT` đặt giá trị (idempotent), `DELETE` bỏ vote. Không dùng kiểu "bấm lại cùng giá trị để hủy" trong `PUT`, vì UC-04 chỉ nói "không tạo bản ghi trùng" mà không nói rõ có toggle hay không — tách hai method thì không còn chỗ hiểu nhầm, và FE muốn toggle vẫn tự làm được bằng cách gọi `DELETE`.

`vote_score` trên `blogs` là giá trị denormalize. Cập nhật trong **cùng một transaction** với bảng vote, bằng câu lệnh atomic:

```sql
UPDATE blogs SET vote_score = vote_score + :delta WHERE id = :id
```

| Vote hiện có | Hành động | delta |
|---|---|:-:|
| chưa có | `PUT 1` | +1 |
| chưa có | `PUT -1` | −1 |
| `1` | `PUT -1` | −2 |
| `-1` | `PUT 1` | +2 |
| `1` | `PUT 1` | 0 |
| `-1` | `PUT -1` | 0 |
| `1` | `DELETE` | −1 |
| `-1` | `DELETE` | +1 |
| chưa có | `DELETE` | 0 |

Ràng buộc:

- Chỉ vote được bài `PUBLISHED` → nếu không, `BLOG_NOT_EXISTED`.
- **Không vote được bài của chính mình** → `CANNOT_VOTE_OWN_CONTENT`. Lấy thẳng từ UC-04, nguyên văn *"bình luận và đánh giá (vote) trên các bài viết của người dùng khác"*.
- `value` khác `1` và `-1` → `INVALID_VOTE_VALUE`.
- `DELETE` khi chưa từng vote là no-op, trả 200 — xóa cái không tồn tại thì kết quả mong muốn đã đạt.

### 6.3 Tìm kiếm và nội dung liên quan

`keyword` dịch thành `title LIKE %kw% OR content LIKE %kw%` trên tập `status = PUBLISHED`. Không tận dụng được index, nhưng ở quy mô đồ án thì chấp nhận được, và SRS mục 10 ghi rõ Elasticsearch không phải Must-Have. Khi dữ liệu lớn lên, đổi sang `FULLTEXT INDEX` là thay đổi cục bộ trong repository.

Không có kết quả thì trả **page rỗng, HTTP 200** (FR-05-04), không ném lỗi — MSG-01 là việc hiển thị của FE.

`GET /blogs/{id}/related` là bản không-AI của FR-05-03: cùng `category_id`, loại chính bài đó, chỉ lấy `PUBLISHED`, sắp theo `vote_score DESC`, giới hạn 5. Khi có AI gợi ý, thay phần thân, giữ nguyên contract.

## 7. Error code

Dải mã theo quy ước trong `CLAUDE.md`: identity 10xx, nutrition 20xx, **blog 30xx**. Mã dùng chung giữ nguyên số ở mọi service: `1001 INVALID_KEY`, `1008 UNAUTHENTICATED`, `1009 UNAUTHORIZED`, `1018 INVALID_REQUEST`, `9999 UNCATEGORIZED_EXCEPTION`. `FILE_UPLOAD_FAILED` giữ **1017** đúng như identity-service — cùng một ý nghĩa mà đánh số khác nhau ở mỗi service thì FE phải xử lý hai nhánh cho cùng một tình huống.

| Mã | Tên | HTTP |
|---|---|---|
| 3001 | `CATEGORY_NAME_REQUIRED` | 400 |
| 3002 | `CATEGORY_TYPE_REQUIRED` | 400 |
| 3003 | `CATEGORY_NOT_EXISTED` | 404 |
| 3004 | `CATEGORY_NAME_DUPLICATED` | 400 |
| 3005 | `CATEGORY_IN_USE` | 400 |
| 3006 | `CATEGORY_DEPTH_EXCEEDED` | 400 |
| 3007 | `CATEGORY_INACTIVE` | 400 |
| 3010 | `BLOG_TITLE_REQUIRED` | 400 |
| 3011 | `INVALID_BLOG_TITLE` | 400 |
| 3012 | `BLOG_CONTENT_REQUIRED` | 400 |
| 3013 | `INVALID_BLOG_CONTENT` | 400 |
| 3014 | `BLOG_NOT_EXISTED` | 404 |
| 3015 | `INVALID_BLOG_STATUS_TRANSITION` | 400 |
| 3020 | `THUMBNAIL_REQUIRED` | 400 |
| 3021 | `INVALID_THUMBNAIL_TYPE` | 400 |
| 3022 | `THUMBNAIL_TOO_LARGE` | 400 |
| 3030 | `COMMENT_CONTENT_REQUIRED` | 400 |
| 3031 | `INVALID_COMMENT_CONTENT` | 400 |
| 3032 | `COMMENT_NOT_EXISTED` | 404 |
| 3033 | `COMMENT_REPLY_TOO_DEEP` | 400 |
| 3034 | `INVALID_COMMENT_PARENT` | 400 |
| 3035 | `COMMENT_TARGET_NOT_EXISTED` | 400 |
| 3036 | `UNSUPPORTED_TARGET_TYPE` | 400 |
| 3040 | `INVALID_VOTE_VALUE` | 400 |
| 3041 | `CANNOT_VOTE_OWN_CONTENT` | 400 |
| 3042 | `BLOG_IDS_REQUIRED` | 400 |

Ba quy ước về cách dùng:

- **Không có mã riêng cho "không phải chủ sở hữu"** — dùng lại `UNAUTHORIZED` (1009). Cả vi phạm sở hữu lẫn thiếu quyền admin đều là "bạn không có quyền"; tách ra chỉ bắt FE xử lý hai nhánh cho cùng một thông báo.
- **Không có mã "bài chưa được publish"**. Query đã lọc sẵn `status = PUBLISHED`, nên bài nháp của người khác trả về đúng như bài không tồn tại → `BLOG_NOT_EXISTED`. Cùng thủ thuật `findByIdAndUserId` mà `HealthRecordService` đang dùng, và nó chặn luôn việc dò xem id nào tồn tại.
- Validation message là **tên enum** (`@NotBlank(message = "BLOG_TITLE_REQUIRED")`), `{min}` trong message được điền từ thuộc tính `min` của constraint. `GlobalExceptionHandler` chỉ đọc field error đầu tiên; tên không hợp lệ rơi về `INVALID_KEY`.

## 8. Test

Theo đúng cách hai service hiện tại đang test: unit test bằng Mockito và controller slice, **không cần database**.

**Service unit test**

| Class | Trọng tâm |
|---|---|
| `VoteServiceTest` | Bảng delta ở 6.2, test tham số hóa đủ 9 trường hợp; chặn tự vote; chặn vote bài chưa publish; `value` không hợp lệ |
| `BlogServiceTest` | Ba nhánh moderation; `publish = false` → `DRAFT`; sửa bài `PUBLISHED` phải moderate lại; `published_at` chỉ set một lần; chủ sở hữu vs admin vs người lạ; `submit` trên bài không phải `DRAFT` |
| `CommentServiceTest` | Reply đúng một cấp; parent khác target bị chặn; xóa mềm giữ reply; `targetType = VIDEO` → `UNSUPPORTED_TARGET_TYPE`; comment vào bài không tồn tại |
| `CategoryServiceTest` | Chặn quá 2 cấp; xóa khi còn blog hoặc còn con; trùng tên trong cùng parent; chọn category `is_active = false` khi tạo blog |

**Controller slice test** — `@WebMvcTest(X.class)` + `@Import({SecurityConfig.class, JwtConfig.class, SecurityExceptionHandler.class})` + `@MockitoBean` cho service, auth bằng `SecurityMockMvcRequestPostProcessors.jwt().jwt(t -> t.claim("userId", 7L))`:

| Trường hợp | Kỳ vọng |
|---|---|
| `GET /blogs` không token | 200 |
| `GET /blogs` với token đã hết hạn | 200 — `BearerTokenResolver` bỏ qua token ở path public |
| `POST /blogs` không token | 401, body `code: 1008` |
| `POST /categories` với `ROLE_USER` | 403, body `code: 1009` |
| `POST /categories` với `ROLE_ADMIN` | 200 |
| `GET /blogs/me` không token | 401 — `/blogs/me` không khớp `/blogs/{id:[0-9]+}` |
| `GET /blogs/me` có token hợp lệ | 200 — token **không** bị `BearerTokenResolver` vứt |

Hai trường hợp cuối canh đúng cái bẫy đã mô tả ở 3.3. Nếu ai đó nới `{id:[0-9]+}` thành `{id}`, test thứ hai đỏ ngay — không có nó thì lỗi chỉ lộ ra lúc chạy thật và trông y như token hỏng.

**Copy từ nutrition-service rồi chỉnh**: `JwtConfigTest`, `SecurityConfigTest`, `SecurityExceptionHandlerTest`, `GlobalExceptionHandlerTest`.

**`BlogServiceApplicationTests.contextLoads`** cần MySQL thật, giống hai service kia. Chạy phần còn lại bằng:

```
./mvnw test -Dtest='!BlogServiceApplicationTests'
```

**Ngoài phạm vi test**: integration test có database thật (H2 hoặc Testcontainers) — giữ nguyên quyết định của spec user-profile.

## 9. Truy vết requirement

| FR | Đáp ứng ở |
|---|---|
| FR-02-01, FR-02-02 | `GET /blogs`, `GET /blogs/{id}` public |
| FR-03-01 | `POST /blogs` |
| FR-03-03, FR-03-04 | `PUT` / `DELETE /blogs/{id}` có check sở hữu |
| FR-03-05 | 6.1 — mọi đường vào `PUBLISHED` đều qua `moderate()` |
| FR-04-01 | `POST /comments` |
| FR-04-02 | `PUT /blogs/{id}/vote` |
| FR-04-03 | UNIQUE `(user_id, target_type, target_id)` |
| FR-05-01, FR-05-02 | `GET /blogs?keyword=` |
| FR-05-03 | `GET /blogs/{id}/related` (bản không-AI) |
| FR-05-04 | Page rỗng, HTTP 200 |
| FR-10-03, FR-10-04 | Nới check sở hữu cho `ROLE_ADMIN` trên `PUT` / `DELETE` |
| FR-11-01, FR-11-02, FR-11-03 | `POST` / `PUT` / `DELETE /categories` + `CATEGORY_IN_USE` |
| FR-12-01, FR-12-02 | `ContentModerationService` (implement hiện tại luôn `APPROVED`) |
| BR-07 | Check sở hữu ở `BlogService` và `CommentService` |
| BR-08 | `@PreAuthorize("hasRole('ADMIN')")` trên `CategoryController` |

**Chưa đáp ứng đầy đủ**: FR-03-02 (upload video), FR-12-03..05 (hàng chờ kiểm duyệt và giám sát AI của Admin) — đã nêu ở mục 2 là ngoài phạm vi.

## 10. Đánh đổi đã chấp nhận

| Quyết định | Được | Mất |
|---|---|---|
| `comments` và `content_votes` polymorphic thay vì tách theo loại như ERD | Thêm video chỉ cần thêm một bảng `videos` | Lệch ERD nhóm đã vẽ; không có FK xuống `blogs`, phải validate ở service |
| Chỉ lưu `author_id`, FE resolve tên và avatar | blog-service không phụ thuộc identity-service lúc chạy; tên tác giả luôn mới | FE phải gọi thêm một request cho mỗi trang |
| Token bị bỏ qua ở endpoint public | Token hết hạn không làm khách bị 401 | Trạng thái vote của chính mình phải lấy bằng một call riêng |
| `vote_score` denormalize | Sắp xếp theo độ phổ biến không cần `COUNT` | Phải giữ đồng bộ bằng tay trong transaction; lệch dữ liệu là có thể xảy ra nếu sót nhánh |
| Khóa chính `Long` thay vì `CHAR(36)` UUID | Đồng bộ với code hiện có, index nhỏ hơn | Lệch tài liệu Entities; id đoán được (giảm nhẹ bằng việc chỉ trả `PUBLISHED`) |
| Category tối đa 2 cấp | Không thể tạo vòng lặp | Không mô tả được cây phân loại sâu hơn |
| Search bằng `LIKE` | Không thêm hạ tầng | Quét toàn bảng; chậm dần khi dữ liệu lớn |
| `AutoApproveContentModerationService` | Demo chạy thông, có chỗ cắm AI | Trên thực tế BR-02 chưa được thực thi thật cho tới khi có AI |

## 11. Việc cho đợt sau

1. `videos` + `video_ai_summaries` (UC-08) — cắm vào `TargetType.VIDEO` đã có sẵn.
2. `moderation_cases` + hàng chờ kiểm duyệt của Admin (UC-12 FR-12-02..05).
3. Implement AI cho `ContentModerationService`.
4. `admin_logs` (SRS mục 8: "hệ thống lưu lại quyết định để audit").
5. `blog.recipe_id` khi `recipes` ra đời.
6. Gợi ý nội dung liên quan bằng AI, thay phần thân `GET /blogs/{id}/related`.
7. Chuyển search sang `FULLTEXT INDEX` khi dữ liệu đủ lớn.
