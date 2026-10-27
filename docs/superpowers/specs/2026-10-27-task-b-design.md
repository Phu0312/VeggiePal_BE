# Task B: tác giả seed của blog + kiểm duyệt AI bất đồng bộ — Design Spec

| Thuộc tính | Giá trị |
|---|---|
| Ngày | 2026-10-27 |
| Nhánh | đề xuất `fix/task-b` (cắt từ `dev`) |
| Trạng thái | Long đã duyệt hướng giải quyết ngày 2026-10-27 |
| Tài liệu gốc | `task_be.docx` mục B (8, 9). Task sheet "PHÂN CHIA TASK": US12 `[BE] API AI Kiểm duyệt (Async)` — *"Phản hồi nhanh với Client, xử lý duyệt chạy nền"*; US12 `[BE] Logic Gắn cờ (Flag)` — *"Khi bị Flag, bài viết lập tức không hiển thị ở các API Public"*; SRS BR-02 (mọi đường vào `PUBLISHED` đều qua kiểm duyệt) |

## 1. Mục 8 — tác giả của dữ liệu seed blog

### 1.1 Hiện trạng

- `blog-service/src/main/resources/data.sql` seed blog, comment và vote với `author_id` **1, 2, 3** (tài liệu B ghi nhầm là 1, 2).
- identity-service không seed user nào. Trên DB mới, `GET /users/batch?ids=1,2,3` trả `[]`, nên tên tác giả hiển thị trống.
- **Hệ quả nặng hơn:** ba người đăng ký đầu tiên trên DB mới nhận id 1–3 và âm thầm "sở hữu" bài, bình luận và vote seed. Quyền sở hữu được kiểm tra bằng `author_id`, nên họ sửa và xóa được các bài đó, và thấy chúng trong `GET /blogs/me`.

### 1.2 Giải pháp

Thêm `identity-service/src/main/resources/data.sql` để seed ba tài khoản tác giả với **id cố định 1, 2, 3**:

| id | email | full_name |
|---|---|---|
| 1 | `author1@veggiepal.invalid` | `VeggiePal Team` |
| 2 | `author2@veggiepal.invalid` | `Bếp Chay Mỗi Ngày` |
| 3 | `author3@veggiepal.invalid` | `Sống Xanh` |

- `role='USER'`, `status='ACTIVE'` (vì `/users/batch` chỉ trả user ACTIVE), `email_verified=1`.
- **Không đăng nhập được** (Long chọn ngày 2026-10-27, vì stack chạy qua ngrok):
  - `password_hash = 'SEED_ACCOUNT_NO_LOGIN'` không phải chuỗi BCrypt, nên `BCryptPasswordEncoder.matches` luôn trả `false`.
  - Domain `.invalid` (RFC 2606) không bao giờ là tài khoản Google thật, nên `/auth/google` không liên kết được vào các tài khoản này.
- Mỗi dòng là `INSERT … SELECT … FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM users WHERE id = N)`. Chèn id tường minh cũng đẩy AUTO_INCREMENT lên, nên user đăng ký thật bắt đầu từ id 4.
- `identity-service/application.properties` thêm `spring.jpa.defer-datasource-initialization=true` và `spring.sql.init.mode=always`, giống blog-service, để `data.sql` chạy sau khi Hibernate tạo bảng.

### 1.3 Giới hạn đã chấp nhận

DB cũ đã có user id 1–3 thì `NOT EXISTS` bỏ qua dòng seed, và những user đó vẫn "sở hữu" dữ liệu seed. Muốn sạch thì phải tạo lại `veggiepal_identity`. Mức này chấp nhận được cho môi trường dev.

## 2. Mục 9 — kiểm duyệt AI cho blog chạy bất đồng bộ

### 2.1 Hiện trạng và các lỗi

`BlogService.createBlog/updateBlog/submitBlog` gọi `AiContentModerationService.moderate` **đồng bộ, bên trong `@Transactional`**, với JWT của người gọi:

1. **AI sập hoặc chậm thì không đăng được bài.** ai-service không truy cập được, hoặc quá 20s (`services.ai.read-timeout`), thì trả 503 `MODERATION_UNAVAILABLE` và rollback cả bài.
2. **Có thể cạn connection pool.** Mỗi lần publish giữ một connection DB tới 23s (3s connect + 20s read). Khoảng 10 request publish chậm cùng lúc làm cạn pool Hikari mặc định, và cả blog-service đứng, kể cả các API đọc công khai.
3. **Timeout hai bên lệch nhau.** ai-service chờ Groq tới 45s × (1 + 2 lần retry). blog-service bỏ cuộc ở giây 20 và rollback, trong khi ai-service vẫn chạy tiếp và lưu `ModerationCase` cho một id blog không còn tồn tại.
4. **Kiểm duyệt bị tính vào quota AI theo ngày của user.** Đó là `usage.checkBudget("user:"+id)`, trả 429 `QUOTA_EXCEEDED`. User dùng chatbot hoặc meal plan nhiều thì không đăng được bài hay bình luận nữa, và blog-service còn báo nhầm thành 503.

Task sheet US12 yêu cầu ngược lại: phản hồi nhanh, kiểm duyệt chạy nền.

### 2.2 Quyết định (Long, 2026-10-27)

| Câu hỏi | Chọn |
|---|---|
| Đồng bộ hay bất đồng bộ | **Bất đồng bộ** theo US12 |
| Hiển thị trong lúc chờ AI | **Ẩn cho tới khi được duyệt** (pre-moderation, giữ BR-02). Không publish trước rồi gỡ sau |
| Quota | **Kiểm duyệt không tính vào quota user.** blog-service gọi một endpoint nội bộ mới của ai-service bằng `X-Internal-Service-Token` |
| Phạm vi | Chỉ **blog** chuyển sang bất đồng bộ. Video và comment vẫn đồng bộ như cũ. Chúng chỉ thay đổi theo vì dùng chung `AiContentModerationService`: đổi cách xác thực và hết bị tính quota. Code của `VideoService` và `CommentService` không phải sửa |

### 2.3 Luồng mới

```
POST /blogs {publish:true} | PUT /blogs/{id} (bài không phải DRAFT) | POST /blogs/{id}/submit
  └─ BlogService (trong transaction)
       status = PENDING, moderation_reason = null, moderation_requested_at = now()
       publishEvent(BlogModerationRequested(blogId, authorId, title + "\n" + content))
  └─ trả 200 ngay: status = PENDING, aiReviewInProgress = true

Sau khi commit (@Async + @TransactionalEventListener(AFTER_COMMIT)):
  BlogModerationWorker.moderate(blogId, authorId, text)
    └─ AiContentModerationService.moderate("BLOG", …)  → ai-service POST /internal/moderation/check
    └─ thành công → BlogService.applyAiModeration(blogId, text, result)   (transaction ngắn, riêng)
    └─ AppException (AI sập, phản hồi sai) → log WARN, bài vẫn PENDING + moderation_requested_at ≠ null

@Scheduled mỗi moderation.retry-interval (mặc định PT2M):
  tối đa 20 bài PENDING có moderation_requested_at < now - interval → gọi moderate(...) lại
```

Không còn transaction nào ôm lời gọi HTTP, nên lỗi 2 được xử lý. Lỗi 1 không còn trên đường blog. Lỗi 3 không còn gây hại: một lần gọi bị bỏ dở chỉ để lại bài ở `PENDING` cho job retry, và ai-service làm mới đúng case đang mở (`findFirst…ReviewedAtIsNull`) chứ không tạo case trùng.

### 2.4 Dữ liệu

Hai cột mới trên `blogs`, được `ddl-auto=update` tự thêm. Cả hai nullable và không phải ENUM, nên không cần `ALTER` tay:

| Cột | Kiểu | Ý nghĩa |
|---|---|---|
| `moderation_requested_at` | `DATETIME(6) NULL` | Khác null nghĩa là đang chờ AI. Đặt khi gửi đi kiểm duyệt, xóa khi AI đã trả lời hoặc admin đã quyết định. `PENDING` + null = đang chờ admin (REVIEW_REQUIRED) |
| `moderation_reason` | `VARCHAR(1000) NULL` | Lý do AI từ chối hoặc chuyển admin. Độ dài bằng cột `reason` của `moderation_cases` bên ai-service |

Các bài `PENDING` có sẵn từ trước có `moderation_requested_at = null`, nên job retry không đụng tới chúng (chúng đang chờ admin).

### 2.5 Trạng thái theo từng endpoint

| Endpoint | Trước | Sau |
|---|---|---|
| `POST /blogs` `publish=false` | `DRAFT` | không đổi |
| `POST /blogs` `publish=true` | `PUBLISHED`/`REJECTED`/`PENDING`, hoặc 503 | luôn trả 200, `PENDING`, `aiReviewInProgress=true` |
| `PUT /blogs/{id}` trên `DRAFT` | `DRAFT`, không kiểm duyệt | không đổi |
| `PUT /blogs/{id}` trên `PUBLISHED`/`REJECTED`/`PENDING` | kiểm duyệt lại đồng bộ | `PENDING` + chạy nền. **Bài đang public bị ẩn vài giây** cho tới khi AI duyệt lại: đây là cái giá của pre-moderation, đã chấp nhận |
| `PUT /blogs/{id}` trên `BANNED` | 400 3015 | không đổi |
| `POST /blogs/{id}/submit` (từ `DRAFT`) | kiểm duyệt đồng bộ | `PENDING` + chạy nền |

`published_at` vẫn chỉ đặt một lần, ở lần duyệt đầu tiên.

### 2.6 Áp kết quả AI (`BlogService.applyAiModeration`)

**Bổ sung đã được Long duyệt sau review:** transaction ngắn này đọc bài qua `BlogRepository.findForModeration` với `PESSIMISTIC_WRITE` (truy vấn theo khóa chính). Khóa giữ từ lúc đọc/kiểm tra cho tới commit hoặc rollback, để kết quả AI cũ không ghi đè một lệnh ban hoặc lần sửa nội dung đồng thời. Lời gọi HTTP tới AI vẫn ở ngoài transaction và ngoài thời gian giữ khóa. Không thêm cột hoặc dependency.

Kết quả bị **bỏ qua** khi bài:
- không còn tồn tại (chủ bài đã xóa),
- không còn `PENDING` (admin đã ban hoặc đã quyết định),
- có `moderation_requested_at = null` (đã có kết quả khác áp vào),
- hoặc có `title + "\n" + content` khác với đoạn text đã gửi đi (chủ bài sửa tiếp trong lúc AI đang xét; lần sửa đó đã phát một yêu cầu kiểm duyệt riêng).

Ngược lại thì xóa `moderation_requested_at` và:

| Quyết định AI | `status` | `moderation_reason` | `published_at` |
|---|---|---|---|
| `APPROVED` | `PUBLISHED` | `null` | `now()` nếu đang null |
| `REJECTED` | `REJECTED` | lý do của AI | không đổi |
| `REVIEW_REQUIRED` | `PENDING` (chờ admin) | lý do của AI | không đổi |

`applyAdminModeration` (lúc admin quyết định một case) cũng xóa `moderation_requested_at`, và khi duyệt thì xóa luôn `moderation_reason`.

Admin ban qua `deleteBlog` cũng xóa `moderation_requested_at`; bài `BANNED` trả `aiReviewInProgress=false`, kể cả khi kết quả AI đến muộn.

### 2.7 Retry

- Property `moderation.retry-interval=${MODERATION_RETRY_INTERVAL:PT2M}`, dùng cho cả chu kỳ của `@Scheduled` lẫn ngưỡng tuổi của bài cần retry.
- Mỗi lượt xử lý tối đa 20 bài, bài cũ nhất trước.
- **Ceiling đã chấp nhận:**
  - AI sập lâu thì job thử lại mãi mãi, cứ mỗi 2 phút. Khi hàng chờ đủ lớn thì thêm số lần thử hoặc backoff.
  - Callback async và job retry có thể gọi AI trùng trên cùng một bài. Lúc áp kết quả, mỗi callback dùng khóa hàng ngắn ở 2.6; callback sau phải kiểm tra lại trạng thái và text sau khi lấy khóa. Không khóa trong thời gian gọi AI và không thêm cơ chế chống gọi HTTP trùng.

### 2.8 Endpoint nội bộ của ai-service

`POST /internal/moderation/check`:
- Header `X-Internal-Service-Token`, so khớp `services.internal-token` bằng `MessageDigest.isEqual`. Lớp `InternalServiceToken` sao chép từ blog-service. Thiếu token hoặc sai token thì 401 `UNAUTHENTICATED` (6001).
- Body giống hệt `ModerationRequest` của `/ai/moderation/check`.
- Trong Spring Security, endpoint là `permitAll` và nằm trong `PUBLIC_ENDPOINTS`, để JWT cũ không làm nó trả 401. Token được kiểm tra trong controller.
- Gateway không có route `/internal/**` (chỉ có `/ai/**` và `/api/ai/**`), nên từ ngoài không gọi được.
- Gọi `ModerationService.moderateForService`: **không gọi `checkBudget`**, ghi usage log với `userId = authorId` và principal `service:moderation` (US19 vẫn thấy được token đã dùng).

`AiContentModerationService` của blog-service chuyển sang gọi endpoint này, kèm header token (`services.internal-token`, đã có sẵn trong properties). Nó không còn đọc `SecurityContextHolder`, nên chạy được trên thread nền và trong job `@Scheduled`. `docker-compose.yml` thêm `INTERNAL_SERVICE_TOKEN` (bắt buộc) vào khối blog-service, giống ai-service và payment-service.

`/ai/moderation/check` (endpoint cũ, xác thực bằng JWT user) được giữ lại, vì chưa rõ FE có gọi trực tiếp hay không.

### 2.9 Hợp đồng với FE

- `BlogResponse` thêm `aiReviewInProgress` (boolean, `= moderation_requested_at != null`).
- `moderationReason` giờ đọc từ DB, nên đọc lại bài lúc nào cũng thấy (trước đây chỉ có trong response của lần publish).
- Sau khi publish, sửa hoặc submit: FE nhận `status=PENDING`, `aiReviewInProgress=true`. FE poll `GET /blogs/{id}` (có token của chủ bài) khoảng 3 giây một lần, cho tới khi `aiReviewInProgress=false`, rồi:
  - `PUBLISHED`: đã đăng.
  - `REJECTED` + `moderationReason`: hiện lý do.
  - `PENDING` + `moderationReason`: "đang chờ admin duyệt".
- Blog không còn trả 503 `MODERATION_UNAVAILABLE` nữa. Video và comment vẫn trả như cũ.

### 2.10 Cấu hình

| File | Thay đổi |
|---|---|
| `blog-service/application.properties` | `moderation.retry-interval=${MODERATION_RETRY_INTERVAL:PT2M}` |
| `.env.example` | `MODERATION_RETRY_INTERVAL=PT2M` |
| `docker-compose.yml` | khối `blog-service` thêm `INTERNAL_SERVICE_TOKEN: ${INTERNAL_SERVICE_TOKEN:?INTERNAL_SERVICE_TOKEN must be set in .env}` |
| `BlogServiceApplication` | `@EnableAsync`, `@EnableScheduling` |

## 3. Ngoài phạm vi

- Chuyển video và comment sang bất đồng bộ. Chúng vẫn ôm lời gọi HTTP trong transaction (lỗi 2) và vẫn có timeout lệch (lỗi 3). Làm sau theo cùng khuôn nếu cần.
- Quét từ khóa cục bộ ("thịt, bạo lực, xúc phạm" trong US12). Prompt của AI đã bao phủ các nhóm vi phạm này. Riêng "thịt" (nội dung không chay) chưa có trong prompt: cần chốt chính sách trước khi thêm.
- Gỡ endpoint cũ `/ai/moderation/check`, khi FE xác nhận không dùng.
- Đẩy kết quả qua WebSocket/SSE thay cho polling.
- Bài `PENDING` có sẵn trước thay đổi nhưng không có case trong ai-service: vẫn cần admin xử lý tay như hiện nay.

## 4. Kiểm thử

| Test | Nội dung |
|---|---|
| `VeggiepalApplicationTests` (identity, MySQL) | id 1–3 tồn tại và ACTIVE. Các tài khoản `@veggiepal.invalid` không khớp mật khẩu nào |
| `ModerationServiceTest` (ai) | `moderateForService` không gọi `checkBudget` và ghi usage dưới `service:moderation` |
| `InternalModerationControllerTest`, `InternalServiceTokenTest` (ai) | sai hoặc thiếu token thì ném lỗi và không gọi service. Đúng token thì gọi `moderateForService` |
| `AiContentModerationServiceTest` (blog) | gọi `/internal/moderation/check` kèm header token, không cần `SecurityContext` |
| `BlogServiceTest` | publish, sửa bài không phải draft và submit → `PENDING` + phát event, không gọi AI. Bốn guard và ba nhánh của `applyAiModeration`. `applyAdminModeration` xóa `moderation_requested_at` |
| `BlogModerationWorkerTest` | thành công thì `applyAiModeration`. `AppException` bị nuốt. Retry lấy đúng bài quá hạn |
| `BlogServiceIntegrationTests` (**gate bắt buộc**: đổi entity và repository) | helper chờ bài thành `PUBLISHED` qua `GET /blogs/me`. AI sập: bài vẫn `PENDING`, bị ẩn công khai, `aiReviewInProgress=true`. Test race trên hai transaction MySQL: kết quả AI cũ không ghi đè lệnh admin ban; trạng thái cuối `BANNED`, `aiReviewInProgress=false` |
