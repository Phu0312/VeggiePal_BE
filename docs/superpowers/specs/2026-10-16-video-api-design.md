# Video API (US 4) — Design Spec

| Thuộc tính | Giá trị |
|---|---|
| Ngày | 2026-10-16 |
| Nhánh | chưa tạo (đề xuất `feature/video`, cắt từ `dev`) |
| Trạng thái | Chờ review |
| Tài liệu gốc | Task sheet "PHÂN CHIA TASK", tab `TASK`, US 4: `[BE] API Get Video List`, `[BE] API Upload/Add Video`, `[FE] UI Grid Video`, `[FE] Form đăng Video`, `[FE] UI Video Player`; tab `Trang tính2` (cột DETAIL); roadmap `IMPLEMENTATION_PHASES.md` Phase 3 |

## 1. Mục tiêu

BUSINESS RULE trên task sheet (cột BE ghi Phú; Long làm spec này):

> **[BE] API Get Video List** — Tương tự Blog, phân trang 10 items. Chỉ lấy video có status "Public".
>
> **[BE] API Upload/Add Video** — Validate file up lên phải là .mp4, dung lượng < 30MB (có thể cấu hình trong application.properties).

Cột DETAIL bổ sung: "Xử lý upload file mp4 lên server **hoặc lưu link nhúng YouTube**".

Phía FE kéo theo: Grid cần thumbnail (FE tự fallback khi lỗi), Form cần progress bar % cho **một** lần gọi Axios, Player cần trang chi tiết.

Phạm vi đã chốt với Long: **US4 + quản lý của chủ video** (đủ cho Grid/Form/Player và My Dashboard của US6).

## 2. Hiện trạng

| Yêu cầu | Nguồn | Hiện trạng |
|---|---|---|
| Bảng/entity video | US4 | **Chưa có.** `TargetType.VIDEO` đã khai báo sẵn trong blog-service |
| List video Public, 10/trang | `[BE] API Get Video List` | **Chưa có** |
| Upload .mp4 < 30MB (cấu hình được) hoặc link YouTube | `[BE] API Upload/Add Video` | **Chưa có** |
| Trang chi tiết cho Player | `[FE] UI Video Player` | **Chưa có** |
| Không xóa category đang được Video dùng | `[BE] API Quản lý Danh mục` (US5) | Chỉ kiểm blog và category con |
| Moderation cho VIDEO | BR-02 | ai-service nhận `contentType=VIDEO`, nhưng `ModerationOwnerClient.apply` từ chối VIDEO (`DOWNSTREAM_UNAVAILABLE`) vì chưa có Video API |
| Upload file lớn qua blog-service | — | `spring.servlet.multipart.max-file-size=6MB`; gateway đã cho 100MB |

Roadmap Phase 3 đặt video **trong blog-service** (dùng chung category, moderation, storage; comment/vote đã đa hình). Spec này theo hướng đó, không tạo service mới.

## 3. Thiết kế

### 3.1 Entity `Video` (bảng `videos`)

| Field | Cột | Ghi chú |
|---|---|---|
| `id` | `id` | IDENTITY |
| `authorId` | `author_id` NOT NULL | Từ JWT `userId`, không FK (user ở DB khác) |
| `category` | `category_id` NOT NULL | `@ManyToOne(LAZY, optional=false)`, `@ToString.Exclude`, `@EqualsAndHashCode.Exclude` |
| `title` | `title` VARCHAR(200) NOT NULL | DTO giới hạn 150, như Blog |
| `description` | `description` TEXT | Không bắt buộc. `columnDefinition = "TEXT"`, **không `@Lob`** (gotcha CLOB trong CLAUDE.md) |
| `sourceType` | `source_type` NOT NULL | Enum mới `VideoSource { UPLOAD, YOUTUBE }` |
| `videoUrl` | `video_url` VARCHAR(512) NOT NULL | Luôn do BE dựng (§3.4), không lưu nguyên chuỗi người dùng gửi |
| `thumbnailUrl` | `thumbnail_url` VARCHAR(512) | Do BE dựng (§3.4) |
| `status` | `status` NOT NULL | Dùng lại `ContentStatus` |
| `viewCount` | `view_count` NOT NULL | Mặc định 0 trong `@PrePersist` |
| `publishedAt` | `published_at` | Đóng dấu một lần, như Blog |
| `createdAt`, `updatedAt` | | `@PrePersist` / `@PreUpdate` |

Index giống `blogs`: `idx_videos_status_published (status, published_at)`, `idx_videos_author (author_id, created_at)`, `idx_videos_category (category_id, status)`.

Bảng mới nên Hibernate tạo cột ENUM với đủ hằng số ngay lần đầu; không vướng gotcha `ALTER TABLE` của enum.

Bỏ qua có chủ đích: `vote_score` (vote video chưa trong phạm vi; cột int thêm sau bằng `ddl-auto` không vấn đề), thời lượng video, thumbnail tự upload.

### 3.2 API

Controller `VideoController` map `/videos`. Gateway: `/api/videos/**` → blog-service.

| Method | Path | Quyền | Mô tả |
|---|---|---|---|
| GET | `/videos?categoryId&page=0&size=10` | public | Chỉ `PUBLISHED`, mới publish trước (`publishedAt desc, id desc`) |
| GET | `/videos/{id}` | public | Chỉ `PUBLISHED`; `view_count + 1` |
| POST | `/videos` (multipart/form-data) | đăng nhập | Tạo video, xem §3.3 |
| GET | `/videos/me?status&page=0&size=10` | đăng nhập | Video của mình, mọi trạng thái, mới tạo trước |
| PUT | `/videos/{id}` (JSON) | chủ / admin | Sửa `title`, `description`, `categoryId`. Nguồn video không đổi được — muốn đổi thì xóa và đăng lại |
| POST | `/videos/{id}/submit` | chủ | Gửi DRAFT đi moderation |
| DELETE | `/videos/{id}` | chủ / admin | Chủ: xóa thật + xóa file Cloudinary. Admin xóa video người khác: chuyển `BANNED` |

Phân trang dùng lại `BlogService.pageRequest` (size kẹp 1..100) và `PageResponse`.

**Request** `VideoRequest` — một class, bind bằng `@ModelAttribute` ở POST (multipart) và `@RequestBody` ở PUT:

| Field | Ràng buộc | Mã lỗi |
|---|---|---|
| `title` | `@NotBlank`, `@Size(max=150)` | `VIDEO_TITLE_REQUIRED`, `INVALID_VIDEO_TITLE` |
| `description` | `@Size(max=5000)` | `INVALID_VIDEO_DESCRIPTION` |
| `categoryId` | `@NotNull` | `CATEGORY_ID_REQUIRED` (dùng lại) |
| `youtubeUrl` | — (kiểm ở service) | `INVALID_YOUTUBE_URL` |
| `publish` | — | `false`/vắng = DRAFT; `true` = chạy moderation ngay |

PUT bỏ qua `youtubeUrl` và `publish` (giống `BlogRequest.publish` ở `updateBlog`). Ghi rõ trong `@Operation` của Swagger.

POST nhận thêm `@RequestPart(name = "file", required = false) MultipartFile file`.

**Response** `VideoResponse` — dùng cho cả list và chi tiết (mô tả ngắn, không cần Summary DTO riêng):
`id, authorId, categoryId, categoryName, title, description, sourceType, videoUrl, thumbnailUrl, status, viewCount, publishedAt, createdAt, updatedAt, moderationReason`.
`VideoMapper` (MapStruct) map `category.id`/`category.name`, `ignore` `moderationReason`.

FE: `sourceType=YOUTUBE` → `<iframe src=videoUrl>`; `UPLOAD` → `<video src=videoUrl poster=thumbnailUrl>`.

### 3.3 Luồng tạo video (`POST /videos`)

Một `@Transactional`, cùng khuôn với `BlogService.uploadThumbnail`:

1. **Đúng một nguồn**: có `file` (không rỗng) XOR có `youtubeUrl` (không blank). Cả hai hoặc không có → `VIDEO_SOURCE_REQUIRED`.
2. **Validate nguồn** trước khi chạm DB:
   - File: `size > video.max-file-size` → `VIDEO_TOO_LARGE`; content type khác `video/mp4` hoặc magic bytes sai → `INVALID_VIDEO_TYPE` (§3.5).
   - YouTube: tách video id (§3.4); không hợp lệ → `INVALID_YOUTUBE_URL`.
3. `categoryService.requireActiveCategory(categoryId)`.
4. `saveAndFlush` bản DRAFT để có `id`. Với YouTube, `videoUrl`/`thumbnailUrl` đã có sẵn ở bước này; với file, `video_url` NOT NULL nên lưu tạm chuỗi rỗng `""` rồi thay ở bước 6.
5. `publish=true` → moderation (§3.6). ai-service lỗi → 503, transaction rollback, **chưa upload gì**.
6. File: upload Cloudinary key `videos/{id}/{uuid}.mp4` (`public_id` = `videos/{id}/{uuid}`), gán `videoUrl` = `secure_url`, `thumbnailUrl` = `secure_url` đổi đuôi thành `.jpg` (Cloudinary tự sinh ảnh poster từ video).
7. `saveAndFlush` (flush để lỗi DB nổ ngay trong `try`, không phải lúc commit). Lỗi → `deleteQuietly(videoUrl)` rồi ném lại, như thumbnail blog.

Cloudinary SDK (`cloudinary-http5` 2.5.0) chỉ dùng `timeout` chung của client (20s, `CloudinaryConfig`), không nhận timeout riêng cho từng lần upload. Smoke test phải gửi thử file gần 30MB; nếu hết giờ thì nâng `timeout` trong `CloudinaryConfig`.

`ponytail:` connection DB bị giữ trong lúc upload (≤ 30MB, vài giây). Đủ cho quy mô này; khi cần thì tách upload ra ngoài transaction.

Đọc file bằng `file.getBytes()` (≤ 30MB trong heap, chấp nhận được).

### 3.4 Nguồn YouTube

`YouTubeUrls.extractId(String url): Optional<String>` (class tiện ích static trong `service`, như `ImageTypeDetector`):

- Parse bằng `java.net.URI`; scheme `http`/`https`.
- Host cho phép: `youtube.com`, `www.youtube.com`, `m.youtube.com`, `youtu.be`.
- Dạng nhận: `youtube.com/watch?v={id}`, `youtu.be/{id}`, `youtube.com/embed/{id}`, `youtube.com/shorts/{id}`.
- `{id}` phải khớp `[A-Za-z0-9_-]{11}`.

BE lưu:
- `videoUrl` = `https://www.youtube.com/embed/{id}?enablejsapi=1` (`enablejsapi` để FE dùng IFrame API tự pause khi cuộn khỏi màn hình hoặc đổi tab — rule của `[FE] UI Video Player`).
- `thumbnailUrl` = `https://img.youtube.com/vi/{id}/hqdefault.jpg`.

Không gọi YouTube để kiểm video còn tồn tại; FE đã có fallback thumbnail mặc định.

### 3.5 Validate file .mp4 và giới hạn dung lượng

- `application.properties`: `video.max-file-size=${VIDEO_MAX_FILE_SIZE:30MB}`, inject kiểu `DataSize` qua `@Value`.
- `Mp4Detector.isMp4(byte[])`: dài ≥ 12 byte và byte 4..7 là ASCII `ftyp` (box đầu của ISO-BMFF). Kèm content type khai báo phải là `video/mp4`.
  `ponytail:` `.mov`/`.3gp` cũng có `ftyp`; khi cần chặt hơn thì thêm allowlist brand (byte 8..11: `isom`, `mp41`, `mp42`, `avc1`…).
- `spring.servlet.multipart.max-file-size` 6MB → **31MB**, `max-request-size` 7MB → **32MB**. Phải lớn hơn `video.max-file-size` để chính service trả `VIDEO_TOO_LARGE`; comment trong file properties cập nhật theo. Nâng `VIDEO_MAX_FILE_SIZE` thì nâng cả hai giá trị này.
- `GlobalExceptionHandler.handlingMaxUploadSize` hiện luôn trả `THUMBNAIL_TOO_LARGE`. Đổi thành chọn theo đường dẫn: URI bắt đầu `/videos` → `VIDEO_TOO_LARGE`, còn lại giữ `THUMBNAIL_TOO_LARGE`.
- Gateway đã có `max-file-size=100MB`; không đổi.

### 3.6 Moderation và trạng thái

Giống Blog (BR-02):

- `applyModeration`: `contentModerationService.moderate("VIDEO", id, authorId, title + "\n" + (description ?? ""))`. `APPROVED` → `PUBLISHED` (+ `publishedAt` nếu chưa có), `REJECTED` → `REJECTED`, `REVIEW_REQUIRED` → `PENDING`. Lý do trả về `moderationReason`.
- `updateVideo`: `BANNED` → `INVALID_VIDEO_STATUS_TRANSITION`; DRAFT sửa không cần moderation; trạng thái khác chạy lại moderation.
- `submitVideo`: chỉ chủ, chỉ DRAFT (`INVALID_VIDEO_STATUS_TRANSITION`), kiểm lại category còn active.
- `applyAdminModeration(id, expectedAuthorId, approved)`: như `BlogService.applyAdminModeration`, lỗi lệch tác giả → `MODERATION_TARGET_MISMATCH`.
- `ModerationAdminController`: thêm `case "VIDEO" -> videoService.applyAdminModeration(...)`.
- **ai-service** `ModerationOwnerClient.apply`: cho phép `"VIDEO"` (hiện chỉ BLOG/COMMENT) và sửa comment. `ModerationOwnerClientTest.applyThrowsDownstreamUnavailableWhenContentTypeIsUnsupported` đang dùng `"VIDEO"` làm ví dụ không hỗ trợ → đổi sang `"RECIPE"`, thêm case VIDEO gửi PUT. Đây là code của Phú — báo trước khi sửa.

`VideoService` tự chứa logic trạng thái, không gộp chung với `BlogService`: mới có hai nơi dùng, entity khác nhau, gộp lúc này là trừu tượng sớm.

### 3.7 Cloudinary

`CloudinaryFileStorageService`, không đổi interface `FileStorageService`:

- `upload`: `resource_type` = `"video"` nếu `contentType` bắt đầu `video/`, ngược lại `"image"`.
- `delete`: nhận cả prefix `https://res.cloudinary.com/<cloud>/image/upload/` và `.../video/upload/`; `destroy` với `resource_type` tương ứng. URL khác (YouTube, MinIO cũ) vẫn bị bỏ qua — nên `deleteVideo` gọi `deleteQuietly(videoUrl)` cho mọi nguồn.
- Log message đổi "image" → "file".

### 3.8 Thay đổi ở chỗ khác

| Chỗ | Thay đổi |
|---|---|
| `CategoryService.delete` | Thêm `videoRepository.existsByCategoryId(id)`; message `CATEGORY_IN_USE` → "Category is still used by blogs, videos or child categories" |
| `SecurityConfig.PUBLIC_ENDPOINTS` | `GET /videos`, `GET /videos/{id:[0-9]+}` (giữ ràng buộc chữ số để `/videos/me` không thành public) |
| `api-gateway/.../application.properties` | `routes[17]` id `blog-service-videos`, `Path=/api/videos/**`, `StripPrefix=1`. Docs route của blog-service đã có |
| `ErrorCode` (blog-service) | Mã mới §3.9 |
| `CLAUDE.md` | Thêm mục video trong blog-service; sửa ý đã cũ: gateway dùng `application.properties` (không còn yaml), moderation đã gọi ai-service thật (không còn stub), route `/api/videos/**` |

### 3.9 Mã lỗi mới (blog-service, dải 30xx)

| Mã | Tên | HTTP | Message |
|---|---|---|---|
| 3060 | `VIDEO_NOT_EXISTED` | 404 | Video not existed |
| 3061 | `VIDEO_TITLE_REQUIRED` | 400 | Video title is required |
| 3062 | `INVALID_VIDEO_TITLE` | 400 | Video title must be at most {max} characters |
| 3063 | `INVALID_VIDEO_DESCRIPTION` | 400 | Video description must be at most {max} characters |
| 3064 | `VIDEO_SOURCE_REQUIRED` | 400 | Provide either a video file or a YouTube link, not both |
| 3065 | `INVALID_VIDEO_TYPE` | 400 | Video must be an MP4 file |
| 3066 | `VIDEO_TOO_LARGE` | 400 | Video exceeds the upload size limit |
| 3067 | `INVALID_YOUTUBE_URL` | 400 | YouTube link is not valid |
| 3068 | `INVALID_VIDEO_STATUS_TRANSITION` | 400 | Video is not in a state that allows this action |

`VIDEO_TOO_LARGE` không ghi số vì giới hạn cấu hình được; FE tự hiển thị "< 30MB".

### 3.10 Repository

`VideoRepository extends JpaRepository<Video, Long>`:

- `Page<Video> findByStatus(ContentStatus, Pageable)` và `findByStatusAndCategoryId(ContentStatus, Long, Pageable)` — chọn bằng `categoryId == null`, như `getOwnBlogs`.
- `Page<Video> findByAuthorId(Long, Pageable)`, `findByAuthorIdAndStatus(Long, ContentStatus, Pageable)`.
- `Optional<Video> findByIdAndStatus(Long, ContentStatus)`.
- `boolean existsByCategoryId(Long)`.
- `@Modifying @Query("update Video v set v.viewCount = v.viewCount + 1 where v.id = :id") incrementViewCount`.

Mọi method đọc của `VideoService` trả DTO có `categoryName` phải `@Transactional(readOnly = true)` — thiếu nó là `LazyInitializationException`, đúng loại lỗi từng làm sập blog-service.

## 4. Kiểm thử

**Unit / slice (vòng nhanh, không cần MySQL):**

- `VideoServiceTest`: tạo bằng file và bằng YouTube; gửi cả hai / không gửi gì; file quá cỡ; file giả mp4 (content type đúng, magic bytes sai); ba kết quả moderation; moderation 503 thì không upload; save lỗi sau upload thì xóa file đã up; sửa video BANNED bị từ chối; sửa DRAFT không moderation; submit không phải DRAFT bị từ chối; chủ xóa → xóa row + gọi `delete` storage; admin xóa video người khác → BANNED; người lạ sửa/xóa → `UNAUTHORIZED`; detail tăng view.
- `YouTubeUrlsTest`: 4 dạng hợp lệ; host lạ, id sai độ dài, scheme `javascript:`, chuỗi rác.
- `Mp4DetectorTest`: `ftyp` ở offset 4; PNG; mảng ngắn.
- `CloudinaryFileStorageServiceTest`: upload `video/mp4` gửi `resource_type=video`; delete URL `/video/upload/` gọi `destroy` với `resource_type=video`.
- `CategoryServiceTest`: category có video → `CATEGORY_IN_USE`.
- `SecurityConfigTest`: `GET /videos`, `GET /videos/5` public; `GET /videos/me`, `POST /videos` không public.
- `VideoControllerTest` (`@WebMvcTest`): multipart hợp lệ → 200; title rỗng trong multipart → 3061 (xác nhận `@ModelAttribute` đi qua handler `MethodArgumentNotValidException`); POST không token → 401.
- `GlobalExceptionHandlerTest`: `MaxUploadSizeExceededException` trên `/videos` → 3066, trên `/blogs/1/thumbnail` → 3022.
- ai-service `ModerationOwnerClientTest`: như §3.6.

**Cổng bắt buộc — `BlogServiceIntegrationTests`** (MySQL thật, chạy riêng; dùng nguồn YouTube để không cần Cloudinary):

- Tạo video YouTube `publish=true` → `GET /videos` trả `categoryName` (đọc qua lazy proxy).
- `GET /videos/{id}` hai lần → `viewCount` tăng (chứng minh `@Modifying` chạy).
- `DELETE /categories/{id}` khi còn video → `CATEGORY_IN_USE`.
- `GET /videos/me` có token trả cả DRAFT.

Cách chạy: xem `CLAUDE.md` (lệnh fast loop loại trừ hai test MySQL; lệnh thứ hai chạy chúng).

## 5. Ngoài phạm vi

Vote và comment cho video (US5 — `TargetType.VIDEO` đã sẵn), related videos, keyword search video (US7), thumbnail tự upload, thời lượng video, danh sách/lọc cho admin, tóm tắt video bằng AI (Phase 9 — `ai-service/video` hiện là job xử lý riêng, không đụng tới).
