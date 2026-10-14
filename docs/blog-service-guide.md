# Blog Service - Tài liệu kiến trúc và giải thích mã nguồn

## 1. Vai trò

`blog-service` quản lý nội dung cộng đồng của VeggiePal:

- Bài blog và vòng đời draft/moderation/published/rejected/banned.
- Cây category hai cấp.
- Tìm kiếm, lọc, sắp xếp và bài liên quan.
- Thumbnail lưu trên Cloudinary.
- Comment và một cấp reply.
- Upvote/downvote và tổng điểm vote.
- Điểm mở rộng cho moderation service.

Service lưu `authorId/userId` lấy từ JWT, nhưng không có entity/foreign key tới bảng user của identity service.

```text
Client -> Gateway :8080 -> Blog :8083 -> veggiepal_blog
                                  |
                                  +-> Cloudinary thumbnails
```

Tên/avatar tác giả không được join trong service. Frontend gom `authorId` rồi gọi identity service `/api/users/batch`.

## 2. Công nghệ và package

Service dùng Java 21, Spring Boot 4.1.1, MVC, JPA, MySQL, Spring Security Resource Server, Jakarta Validation, MapStruct, Lombok, Cloudinary SDK và Springdoc.

```text
com.veggiepal.blog
├── configuration  security, JWT, OpenAPI, Cloudinary
├── controller     blogs, categories, comments
├── dto            API request/response
├── entity         Blog, Category, Comment, ContentVote
├── enums          trạng thái/loại
├── exception      error code/handler
├── mapper         MapStruct
├── moderation     moderation abstraction
├── repository     query database
├── service        business logic/storage
└── validation     @MaxWords
```

## 3. Cấu hình runtime

Service chạy port `8083`, kết nối:

```properties
jdbc:mysql://localhost:3307/veggiepal_blog?createDatabaseIfNotExist=true
```

Hibernate dùng `ddl-auto=update`, show/format SQL và tắt Open Session in View.

JWT dùng `JWT_SECRET` giống identity service và thuật toán HS256.

Thumbnail lưu trên Cloudinary; credential đọc từ `.env` ở gốc repo (template `.env.example`):

```properties
spring.config.import=optional:file:.env[.properties],optional:file:../.env[.properties]
storage.cloudinary.cloud-name=${CLOUDINARY_CLOUD_NAME:}
storage.cloudinary.api-key=${CLOUDINARY_API_KEY:}
storage.cloudinary.api-secret=${CLOUDINARY_API_SECRET:}
```

Multipart framework cho phép file 6 MB/request 7 MB, cao hơn business limit 5 MB để `BlogService` có cơ hội trả đúng error code `THUMBNAIL_TOO_LARGE`.

## 4. API tổng hợp

### Blog

| Method | URL gateway | Auth | Chức năng |
|---|---|---|---|
| POST | `/api/blogs` | User | Tạo draft hoặc publish ngay |
| GET | `/api/blogs/me` | User | Blog của chính user, mọi status |
| PUT | `/api/blogs/{id}` | Owner/Admin | Sửa bài |
| POST | `/api/blogs/{id}/submit` | Owner | Submit draft |
| POST | `/api/blogs/{id}/thumbnail` | Owner/Admin | Upload/replace thumbnail |
| DELETE | `/api/blogs/{id}` | Owner/Admin | Owner xóa thật; admin ban bài người khác |
| GET | `/api/blogs` | Public | Danh sách published |
| GET | `/api/blogs/{id}` | Public | Chi tiết published, tăng view |
| GET | `/api/blogs/{id}/related` | Public | Tối đa 5 bài cùng category |
| POST | `/api/blogs/{id}/vote` | User | Vote 1/-1 hoặc toggle off |
| DELETE | `/api/blogs/{id}/vote` | User | Gỡ vote |
| GET | `/api/blogs/me/votes?blogIds=...` | User | Vote của user trên danh sách blog |

### Category

| Method | URL | Auth | Chức năng |
|---|---|---|---|
| GET | `/api/categories` | Public | Cây category hai cấp |
| GET | `/api/categories/{id}` | Public | Một category |
| POST | `/api/categories` | ADMIN | Tạo category |
| PUT | `/api/categories/{id}` | ADMIN | Đổi tên/thứ tự/active |
| DELETE | `/api/categories/{id}` | ADMIN | Xóa category không được dùng |

### Comment

| Method | URL | Auth | Chức năng |
|---|---|---|---|
| GET | `/api/comments?targetId=...` | Public | Root comments |
| GET | `/api/comments/{id}/replies` | Public | Replies của comment |
| POST | `/api/comments` | User | Tạo comment/reply |
| PUT | `/api/comments/{id}` | Owner | Sửa comment của mình |
| DELETE | `/api/comments/{id}` | Owner/Admin | Soft-delete comment |

## 5. Security

`SecurityConfig` bật cả web security và method security:

```java
@EnableWebSecurity
@EnableMethodSecurity
```

Public endpoint được khai báo theo cả HTTP method và path. Chỉ GET listing/detail/category/comment read là public; các method ghi trên cùng path vẫn cần token.

Pattern public detail dùng `{id:[0-9]+}`. Ràng buộc chữ số rất quan trọng: nếu dùng `/blogs/{id}`, `/blogs/me` cũng có thể bị nhận là public. Custom bearer resolver khi đó sẽ bỏ token, sau đó endpoint `/blogs/me` lại yêu cầu auth và luôn trả 401.

Public endpoint bỏ qua Authorization header để token cũ/hỏng không chặn người dùng đọc nội dung công khai.

JWT claim:

- `userId` -> ownership.
- `role` -> `ROLE_USER`/`ROLE_ADMIN` và `CurrentUser.isAdmin()`.

Category write dùng `@PreAuthorize("hasRole('ADMIN')")`. Blog/comment kiểm tra owner/admin trong business service vì rule phụ thuộc entity owner.

`SecurityExceptionHandler` trả JSON thống nhất cho 401/403. `GlobalExceptionHandler` phải rethrow `AccessDeniedException` từ method-security để nó đi ra filter chain và trở thành 403 thay vì bị catch-all biến thành 500.

## 6. Entity và database

### `Blog`

| Field | Ý nghĩa |
|---|---|
| `authorId` | ID từ JWT, không FK sang identity |
| `category` | Many-to-one nội bộ tới Category |
| `title` | Tiêu đề |
| `content` | `LONGTEXT` |
| `thumbnailUrl` | URL public, tối đa 512 ký tự |
| `status` | Trạng thái nội dung |
| `viewCount` | Lượt xem |
| `voteScore` | Tổng vote |
| `publishedAt` | Lần publish đầu tiên |
| `createdAt/updatedAt` | Audit time |

Các index tối ưu feed published, danh sách theo author và lọc category/status.

`content` không dùng `@Lob`: Hibernate mới có thể map String thành CLOB làm query `lower()/like` không validate. `columnDefinition="LONGTEXT"` giữ Java mapping là String nhưng DB vẫn có dung lượng lớn.

`@PrePersist` khởi tạo timestamp, viewCount và voteScore bằng 0.

### `Category`

Category tự tham chiếu `parent`, nhưng business rule chỉ cho hai cấp. `type` gồm `FOOD_TYPE` hoặc `RECIPE_TYPE`; child luôn kế thừa type của parent.

Tên unique toàn cây, so sánh không phân biệt hoa/thường nhưng có phân biệt dấu tiếng Việt bằng collation `utf8mb4_0900_as_ci`.

Index hỗ trợ lấy children theo parent/display order và root theo type/active.

### `Comment`

Comment lưu:

- `authorId`.
- `targetType` và `targetId` polymorphic.
- Optional parent comment.
- Nội dung `TEXT`.
- Status moderation/deletion.

Không có FK `targetId -> blogs`, vì schema đã dự phòng `VIDEO`. Service tự kiểm tra target tồn tại/published.

### `ContentVote`

Vote lưu `(userId, targetType, targetId, value)`. Unique constraint trên ba cột đầu bảo đảm một vote/user/content ở tầng DB. Value là `-1` hoặc `1`, thuận tiện tính delta số học.

### Enum trạng thái

`ContentStatus`:

```text
DRAFT -> PUBLISHED / REJECTED / PENDING
PENDING/REJECTED/PUBLISHED --edit--> moderation lại
BANNED -> terminal
```

`CommentStatus`: `PENDING`, `VISIBLE`, `HIDDEN`, `DELETED`.

`TargetType` đã có `BLOG` và `VIDEO` để tránh phải sửa native MySQL enum sau này, nhưng logic hiện chỉ hỗ trợ BLOG.

## 7. DTO và validation

### `BlogRequest`

- Title bắt buộc, tối đa 150 ký tự.
- Content bắt buộc, tối thiểu 20 ký tự.
- Category ID bắt buộc.
- `publish=true`: moderation ngay; false/null: lưu draft.
- Client không gửi status, tránh bypass state machine.

### `CategoryRequest`

- Name bắt buộc, tối đa 100.
- Root cần `type`; child truyền `parentId` và kế thừa type.
- `displayOrder` mặc định 0 khi create.
- `active` mặc định true khi create.
- Update không đổi parent/type; chỉ name/order/active.

### `CommentRequest`

- Target type và target ID bắt buộc.
- `parentCommentId=null` tạo root; có ID tạo reply.
- Content không blank.
- Tối đa 500 từ bằng custom `@MaxWords`.
- Tối đa 5000 ký tự để một “từ” cực dài không đụng giới hạn DB.

### `VoteRequest`

DTO chỉ yêu cầu non-null; service kiểm tra chính xác giá trị `1` hoặc `-1`.

### Response

- `BlogResponse`: chi tiết, content, moderationReason.
- `BlogSummaryResponse`: không có full content, dùng cho list.
- `CategoryResponse`: có `children` do service ráp cây.
- `CommentResponse`: deleted flag, null content cho tombstone, replyCount.
- `VoteResponse`: blogId, myVote và voteScore khi phù hợp.
- `PageResponse`: metadata phân trang chuẩn.

## 8. MapStruct

`BlogMapper` map nested `category.id/name`; `moderationReason` được service set thủ công vì không nằm trong entity.

`CategoryMapper` map `parent.id`; `children` do `CategoryService` ráp sau.

`CommentMapper` map `parent.id`; `deleted/replyCount` do service tính theo context query.

MapStruct implementation được sinh khi compile và đăng ký Spring bean.

## 9. BlogService và state machine

### Tạo blog

```text
validate request
 -> lấy authorId từ JWT
 -> require active category (cả parent nếu có)
 -> trim title, tạo DRAFT, counters = 0
 -> publish=true ? moderation : giữ DRAFT
 -> save
```

Moderation APPROVED chuyển thành `PUBLISHED` và set `publishedAt` lần đầu. REJECTED/PENDING chuyển status tương ứng và response mang reason.

### Sửa blog

Owner hoặc admin được sửa. `BANNED` là terminal, không cho sửa để owner không tự gỡ lệnh admin.

- Draft: sửa và vẫn draft, chưa moderation.
- Bài đã từng review: sửa sẽ moderation lại.
- `publishedAt` chỉ đóng dấu lần đầu, edit/re-publish không đẩy bài lên đầu feed bằng timestamp mới.

### Submit draft

Chỉ owner và chỉ `DRAFT` được submit. Service kiểm tra lại category vẫn active vì category có thể bị admin tắt sau lúc tạo draft.

### Delete/takedown

- Owner xóa bài của chính mình: physical delete row.
- Admin xóa bài người khác: đổi status `BANNED`, giữ row để owner thấy trạng thái.

Điểm cần lưu ý: comment/vote dùng target ID không FK, và thumbnail ở object storage; physical delete hiện không thể hiện cleanup các dữ liệu/file liên quan trong `deleteBlog`.

### Public feed/search

Chỉ query `PUBLISHED`. Optional filter category và keyword; keyword tìm không phân biệt hoa/thường trong title/content. Blank keyword được đổi thành null.

Sort:

- Mặc định: `publishedAt DESC, id DESC`.
- `popular`: `voteScore DESC, id DESC`.
- `mostViewed`: `viewCount DESC, id DESC`.
- Giá trị sort lạ fallback mặc định thay vì trả lỗi.

Page size clamp 1..100 và chống overflow offset.

### Chi tiết và view count

Chỉ published blog được xem công khai. View count tăng bằng một câu JPQL atomic:

```sql
UPDATE blogs SET view_count = view_count + 1 WHERE id = ?
```

Không dùng read-modify-write để tránh hai request đồng thời ghi đè lượt xem. Vì JPQL update bypass persistence context, response cộng 1 thủ công vào giá trị entity đang giữ.

### Related blogs

Tìm tối đa 5 bài published cùng category, loại bài hiện tại và sort theo vote score.

## 10. CategoryService

### Lấy cây

Service query roots trước, sau đó query toàn bộ children của các root bằng một query và group theo parent ID. Cách này tránh N+1 query.

Optional:

- `type`: chỉ lấy một category tree type.
- `activeOnly=true`: lọc root và child inactive.

### Tạo category

- Root bắt buộc có type.
- Child bắt buộc parent tồn tại.
- Parent đã là child thì từ chối, giữ độ sâu tối đa hai.
- Child kế thừa type.
- Tên unique toàn cây.

### Update/delete

Update chỉ thay name, displayOrder, active khi field tương ứng có giá trị.

Delete bị chặn nếu category còn child hoặc còn blog tham chiếu. `requireActiveCategory` còn kiểm tra parent active để không chọn child nằm dưới root đã tắt.

## 11. CommentService

### Tạo comment/reply

```text
target phải là BLOG
 -> blog phải PUBLISHED
 -> nếu reply: parent tồn tại, là root, cùng target
 -> tạo PENDING
 -> moderation
 -> save
```

Chỉ một cấp reply. Reply vào một reply trả `COMMENT_REPLY_TOO_DEEP`; parent thuộc thread khác trả `INVALID_COMMENT_PARENT`.

### Sửa comment

Chỉ chính author được sửa, admin cũng không được sửa lời người khác. Comment `DELETED` không được sửa. Request có target/parent nhưng update chỉ thay content; thread identity không đổi. Nội dung mới được moderation lại.

### Delete comment

Owner hoặc admin đổi status thành `DELETED`, không xóa row. Public response giữ tombstone với `deleted=true`, `content=null`, nhờ vậy reply thread không bị đứt.

### Đọc comment

Chỉ `VISIBLE` và `DELETED` được public. Root sort cũ trước. Reply count được group bằng một query cho cả page, tránh một count query cho từng root.

Khi đọc root/reply, service kiểm tra blog vẫn published; một bài bị ban/unpublish sẽ đóng luôn public thread.

## 12. VoteService

Rule:

- Chỉ vote published blog.
- Không vote bài của chính mình.
- Chỉ `1` hoặc `-1`.
- Cùng giá trị lần hai -> bỏ vote.
- Giá trị đối diện -> chuyển vote.

Delta tổng quát:

```java
delta = (next == null ? 0 : next) - (previous == null ? 0 : previous);
```

| Previous | Next | Delta |
|---:|---:|---:|
| null | 1 | +1 |
| null | -1 | -1 |
| 1 | null | -1 |
| -1 | null | +1 |
| 1 | -1 | -2 |
| -1 | 1 | +2 |

Vote score update bằng JPQL atomic để concurrent vote không lost update.

`DELETE vote` idempotent: chưa vote thì vẫn thành công. `GET /blogs/me/votes` giới hạn tối đa 100 ID và trả một entry cho mỗi ID request; `voteScore` ở endpoint overlay này là null.

## 13. Moderation

`ContentModerationService` là abstraction:

```java
ModerationResult moderate(String text);
```

Kết quả có decision `APPROVED`, `REJECTED`, `PENDING` và optional reason.

Implementation hiện tại `AutoApproveContentModerationService` **approve tất cả**. Vì vậy production moderation thực tế chưa tồn tại. Interface cho phép thay bằng AI/external service mà không đổi `BlogService` và `CommentService`.

Blog moderation nhận `title + newline + content`; comment moderation nhận content.

## 14. Thumbnail và Cloudinary

Business limit là 5 MB. Chỉ JPEG/PNG/WEBP, kiểm tra cả MIME type và magic bytes thật.

Trình tự upload:

```text
validate file/size
 -> kiểm tra ownership trước khi ghi storage
 -> đọc bytes/detect type
 -> upload thumbnails/{blogId}/{UUID}.{ext}
 -> save URL mới vào DB
 -> xóa thumbnail cũ best-effort
```

Nếu upload thành công nhưng DB save thất bại, service xóa file mới để bù trừ. Nếu xóa file cũ thất bại, request vẫn thành công và log warning.

`FileStorageService` tách business logic khỏi Cloudinary SDK. `CloudinaryFileStorageService` dùng key bỏ đuôi làm `public_id` (`thumbnails/{blogId}/{UUID}`) và trả `secure_url`. Nó chỉ xóa URL bắt đầu bằng `https://res.cloudinary.com/{cloudName}/image/upload/`; URL MinIO cũ và URL ngoài bị bỏ qua. Lỗi SDK thành `FILE_UPLOAD_FAILED` (503).

## 15. Repository/query đáng chú ý

- `BlogRepository.search`: status/category/keyword dynamic filter.
- `incrementViewCount` và `addVoteScore`: atomic update.
- `CategoryRepository`: roots, children batch, unique name lookup.
- `CommentRepository.countRepliesByParentIds`: grouped projection, tránh N+1.
- `ContentVoteRepository`: lookup unique vote và batch overlay vote.

Transaction bao quanh các thao tác thay đổi state. Tuy vậy object storage không tham gia transaction DB nên code dùng compensation thủ công.

## 16. Validation tùy chỉnh `@MaxWords`

Annotation `MaxWords` gắn `MaxWordsValidator`. Validator:

- Cho null/blank đi qua để `@NotBlank` chịu trách nhiệm required.
- `strip()` rồi split theo whitespace.
- So số từ với `max`.

`GlobalExceptionHandler` đọc attribute `max` để biến message `Comment must be at most {max} words` thành `...500 words`.

## 17. Error model

Shared code gồm 1001, 1008, 1009, 1017, 1018, 9999. Blog-specific dùng dải 30xx:

| Dải | Nhóm |
|---|---|
| 3001-3008 | Category |
| 3010-3015 | Blog/content |
| 3020-3022 | Thumbnail |
| 3030-3037 | Comment |
| 3040-3042 | Vote |

`AppException` mang `ErrorCode`; global handler đổi thành `ApiResponse`. Malformed JSON, thiếu query param, sai enum/path type thành `INVALID_REQUEST`. Multipart thiếu/quá lớn được map riêng.

`DataIntegrityViolationException` không log message DB để tránh lộ mapping user-vote-target.

## 18. Ví dụ luồng

### Tạo và publish blog

```http
POST /api/blogs
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "Mon chay de nau",
  "content": "Noi dung co it nhat hai muoi ky tu...",
  "categoryId": 3,
  "publish": true
}
```

Với implementation moderation hiện tại, bài chuyển ngay sang `PUBLISHED`.

### Comment

```json
{
  "targetType": "BLOG",
  "targetId": 12,
  "parentCommentId": null,
  "content": "Bai viet rat huu ich"
}
```

### Vote toggle

Gửi `{"value":1}` lần đầu -> upvote. Gửi lại cùng body -> vote bị gỡ. Gửi `-1` khi đang `1` -> score giảm 2.

## 19. Test

Test suite bao phủ:

- Security public/protected route và regex `/blogs/{id}`.
- JWT HS256 và từ chối thuật toán khác.
- Controller mapping, validation, ownership input từ JWT.
- Blog create/update/submit/state transition/search/sort/paging/view/related/thumbnail.
- Category tree, depth, duplicate, active/in-use và admin authorization.
- Comment target, parent/thread/depth, moderation, tombstone và reply counts.
- Vote validation, own-content restriction, toggle/switch/delta, batch limit.
- Custom word validator.
- Full integration flow qua MockMvc với database thật.

Chạy unit/slice test bỏ context/integration DB:

```powershell
cd blog-service
./mvnw.cmd test -Dtest='!BlogServiceApplicationTests,!BlogServiceIntegrationTests'
```

Chạy toàn bộ cần MySQL/database sẵn sàng:

```powershell
./mvnw.cmd test
```

## 20. Chạy local

```powershell
docker compose up -d mysql
cd blog-service
./mvnw.cmd spring-boot:run
```

Database tự tạo nhờ `createDatabaseIfNotExist=true`. Credential Cloudinary đọc từ `.env` ở gốc repo (copy từ `.env.example`).

Swagger tổng: `http://localhost:8080/swagger-ui.html` khi gateway chạy.

## 21. Giới hạn và rủi ro hiện tại

1. Moderation đang auto-approve mọi nội dung.
2. Không xác minh author ID trực tiếp với identity; tin JWT.
3. Physical delete blog có nguy cơ để lại comment/vote/thumbnail orphan vì target không FK và storage ngoài DB.
4. View count tăng mỗi lần GET, không chống refresh/bot/duplicate viewer.
5. Vote score là denormalized counter; lỗi transaction/manual DB edit có thể làm lệch so với bảng vote.
6. Shared HS256 secret phải được bảo vệ ở mọi service.
7. Search `LIKE %keyword%` trên LONGTEXT khó scale; khi dữ liệu lớn nên cân nhắc full-text/search engine.
8. Chưa có optimistic locking; update đồng thời có thể last-write-wins.
9. Category name error message nói “same parent” nhưng code/database đang unique toàn cây.
10. `VIDEO` đã có trong enum/schema nhưng API cố ý chưa hỗ trợ.
11. `ddl-auto=update`, credential và endpoint mặc định chỉ phù hợp local.
12. Các public endpoint bỏ qua token; response công khai không cá nhân hóa, vote của user phải gọi endpoint overlay riêng.

## 22. Tóm tắt

Blog service sở hữu toàn bộ lifecycle nội dung cộng đồng. Nó bảo vệ write API bằng JWT, kiểm tra owner/admin trong đúng tầng, giữ public read độc lập, dùng state machine cho blog/comment, atomic update cho view/vote, soft-delete comment để giữ thread và storage abstraction cho thumbnail. User data chỉ được tham chiếu qua ID, đúng boundary microservice.

