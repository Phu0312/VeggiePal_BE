# User Profile & Nutrition Profile — Design Spec

| Thuộc tính | Giá trị |
|---|---|
| Ngày | 2026-09-17 |
| Nhánh | `feature/user-profile` |
| Trạng thái | Chờ review |
| Tài liệu gốc | VeggiePalApp SRS v1.0: mục 2.4 (BR-04, BR-05, BR-06), 5.4, 5.5, 6.1; UC-06 (FR-06-01, FR-06-03) |

## 1. Mục tiêu

Người dùng đã đăng nhập có thể:

1. Xem và sửa thông tin cá nhân: họ tên, số điện thoại, ngày sinh.
2. Upload ảnh avatar.
3. Đổi mật khẩu.
4. Ghi lại chiều cao và cân nặng theo thời gian. Hệ thống tự tính BMI.
5. Khai báo các thực phẩm bị dị ứng hoặc cần tránh.

Mục 4 và 5 là dữ liệu đầu vào cho AI Meal Planner (UC-06) sau này.

Hiện tại hệ thống cấp JWT khi đăng nhập nhưng **chưa có chỗ nào kiểm tra token**. Module này bổ sung phần kiểm tra đó, vì mọi API ở đây đều cần biết người gọi là ai.

## 2. Phạm vi

**Trong phạm vi**
- identity-service: API profile, avatar, đổi mật khẩu; kiểm tra JWT; sửa `JwtService`.
- nutrition-service (service mới): health records, danh mục allergens, dị ứng của user.
- api-gateway: thêm route và tài liệu Swagger cho các API mới.
- docker-compose: thêm MinIO.
- Cập nhật `CLAUDE.md`.

**Ngoài phạm vi**
- Đổi email, xác thực email, xóa avatar.
- Thu hồi token ngay khi đổi mật khẩu (cần blacklist hoặc refresh token).
- Client gửi thẳng BMI thay vì chiều cao và cân nặng. FR-06-01 có ghi "hoặc BMI", nhưng module này chưa hỗ trợ.
- Phân loại BMI (thiếu cân, bình thường, thừa cân...).
- Nguyên liệu sẵn có (`User Ingredients`). Phần này thuộc Meal Planner.
- API admin để quản lý danh mục allergens.
- Quan hệ giữa ingredient và allergen, cần cho BR-05 sau này.
- Integration test có database (H2 hoặc Testcontainers).

## 3. Kiến trúc

```
FE ──► api-gateway :8080 ─┬─► identity-service  :8081 ──► MySQL veggiepal_identity
                          │                            └─► MinIO / S3 (avatar)
                          └─► nutrition-service :8082 ──► MySQL veggiepal_nutrition
```

### 3.1 Phân chia trách nhiệm

| Service | Trách nhiệm |
|---|---|
| identity-service | Đăng ký/đăng nhập (đã có), profile, avatar, đổi mật khẩu |
| nutrition-service | Health records, allergens, dị ứng của user. Sau này chứa luôn Meal Planner. |

nutrition-service **không gọi sang identity-service** và **không có khóa ngoại tới bảng users**. `user_id` được lấy từ claim `userId` trong JWT.

Các class dùng chung (`ApiResponse`, `AppException`, `ErrorCode`, `GlobalExceptionHandler`, cấu hình bảo mật) được **copy** sang nutrition-service. Không tạo module dùng chung, vì repo không có parent POM và mỗi service phải build độc lập được.

### 3.2 Gateway routes

Thêm vào `api-gateway/src/main/resources/application.yaml`. Mọi route dùng `StripPrefix=1`.

| Route id | Predicate | URI |
|---|---|---|
| `identity-service-users` | `Path=/api/users/**` | `http://localhost:8081` |
| `nutrition-service` | `Path=/api/nutrition/**` | `http://localhost:8082` |
| `nutrition-service-docs` | `Path=/nutrition-service/v3/api-docs/**` | `http://localhost:8082` |

Thêm vào `springdoc.swagger-ui.urls`: `{name: Nutrition Service, url: /nutrition-service/v3/api-docs}`.

### 3.3 Xác thực JWT

Cả hai service hoạt động như **OAuth2 Resource Server**, dùng chung một secret HMAC.

- **Cấu hình secret** (khai báo ở cả hai service):
  `jwt.secret=${JWT_SECRET:veggiepal-secret-key-must-be-at-least-32-characters}`
- **Phía ký token** (`JwtService` ở identity-service):
  - Đọc secret từ `jwt.secret`.
  - Ký **chỉ định rõ `Jwts.SIG.HS256`**. Hiện tại `signWith(key)` không ghi thuật toán, nên jjwt tự chọn theo độ dài key. Secret 408 bit khiến token đang được ký bằng HS384.
  - Các claim giữ nguyên: `sub` = email, `userId`, `role`, hạn 24h.
- **Phía kiểm tra token:** bean `JwtDecoder` dùng `NimbusJwtDecoder.withSecretKey(...)` với `.macAlgorithm(MacAlgorithm.HS256)`. Hạn token được kiểm tra bằng validator mặc định.
- **Phân quyền:** `JwtAuthenticationConverter` đọc claim `role` và thêm tiền tố `ROLE_`, tạo ra `ROLE_USER` hoặc `ROLE_ADMIN`.
- **Stateless:** bật `sessionManagement(STATELESS)`.
- **Bỏ qua token ở path public:** thêm một `BearerTokenResolver` tùy chỉnh trả `null` cho các path public. Nếu không có nó, một token hết hạn gửi kèm `/auth/login` sẽ bị trả 401 dù endpoint là `permitAll`. Danh sách path public được khai báo **một lần** (mảng `PUBLIC_ENDPOINTS` đã có sẵn nhưng chưa dùng) và dùng cho cả `permitAll` lẫn resolver.
  - Path public của identity-service: `/auth/register`, `/auth/login`, `/auth/test`, `/swagger-ui/**`, `/swagger-ui.html`, `/v3/api-docs/**`.
  - Path public của nutrition-service: chỉ các path Swagger.
- **Lấy user hiện tại:** controller nhận `@AuthenticationPrincipal Jwt jwt`, đọc `userId` từ claim (kiểu `Number`, đổi sang `Long`), rồi truyền `Long userId` xuống service.
- **Swagger:** `OpenApiConfig` của cả hai service khai báo `SecurityScheme` tên `bearerAuth` (HTTP bearer, JWT) và áp dụng cho mọi endpoint.
- **Ảnh hưởng:** token cũ đang ký bằng HS384 sẽ bị từ chối. Người dùng chỉ cần đăng nhập lại.

### 3.4 Hạ tầng

**docker-compose.yml** thêm hai container:
- `minio`:
  - lệnh chạy `server /data --console-address ":9001"`
  - port `9000` cho API, `9001` cho giao diện web
  - `MINIO_ROOT_USER` / `MINIO_ROOT_PASSWORD` là `minioadmin` / `minioadmin`
  - volume `minio_data`, có healthcheck
- `minio-init`:
  - dùng image `quay.io/minio/mc` (xem R4), chỉ chạy sau khi `minio` healthy
  - tạo bucket `veggiepal-avatars` bằng `mc mb --ignore-existing`
  - cho phép đọc công khai bằng `mc anonymous set download`

Service **không tự tạo bucket**, nên khi chạy trên S3 thật không cần quyền tạo bucket.

**Database:** nutrition-service dùng `jdbc:mysql://localhost:3307/veggiepal_nutrition?createDatabaseIfNotExist=true`.

**Biến môi trường mới** (đều có giá trị mặc định cho dev):

| Biến | Mặc định dev | Dùng ở |
|---|---|---|
| `JWT_SECRET` | chuỗi hiện tại | identity, nutrition |
| `S3_ENDPOINT` | `http://localhost:9000` (để trống khi dùng AWS S3) | identity |
| `S3_REGION` | `us-east-1` | identity |
| `S3_ACCESS_KEY` / `S3_SECRET_KEY` | `minioadmin` / `minioadmin` | identity |
| `S3_BUCKET` | `veggiepal-avatars` | identity |
| `S3_PUBLIC_URL` | `http://localhost:9000/veggiepal-avatars` | identity |

## 4. identity-service

### 4.1 Dữ liệu

- `User` thêm field `LocalDate dateOfBirth`, cột `date_of_birth`, được phép null. Hibernate `ddl-auto=update` tự thêm cột.
- `UserMapper.toUser` thêm `@Mapping(target = "dateOfBirth", ignore = true)`.

### 4.2 API

Controller map `/users/me`. Client gọi qua gateway với tiền tố `/api`. Mọi endpoint yêu cầu JWT.

| Method | Path (qua gateway) | Request | `result` |
|---|---|---|---|
| GET | `/api/users/me` | — | `UserProfileResponse` |
| PATCH | `/api/users/me` | `UpdateProfileRequest` (JSON) | `UserProfileResponse` |
| POST | `/api/users/me/avatar` | `multipart/form-data`, part `file` | `UserProfileResponse` |
| PUT | `/api/users/me/password` | `ChangePasswordRequest` (JSON) | không có (`code: 1000`) |

**`UserProfileResponse`:** `id, email, fullName, phone, avatarUrl, dateOfBirth, role, status, emailVerified, createdAt`

**`UpdateProfileRequest`:** cả ba field đều không bắt buộc.

| Field | Validation |
|---|---|
| `fullName` | `@Pattern(regexp = ".*\\S.*", message = "FULL_NAME_REQUIRED")`. `null` được bỏ qua; chuỗi rỗng hoặc chỉ có khoảng trắng thì lỗi. |
| `phone` | Không kiểm tra định dạng |
| `dateOfBirth` | `@Past(message = "INVALID_DATE_OF_BIRTH")`. Ngày hôm nay cũng không hợp lệ. |

**`ChangePasswordRequest`:**

| Field | Validation |
|---|---|
| `currentPassword` | `@NotBlank(message = "PASSWORD_REQUIRED")` |
| `newPassword` | `@NotBlank(message = "PASSWORD_REQUIRED")`, `@Size(min = 6, message = "INVALID_PASSWORD")` |

### 4.3 Quy tắc nghiệp vụ

Mọi thao tác đều tìm user theo `userId` trong token. Không tìm thấy thì trả `USER_NOT_EXISTED`.

**PATCH profile:**
- Field `null` hoặc không gửi thì **giữ nguyên** giá trị cũ. Mapper dùng `@BeanMapping(nullValuePropertyMappingStrategy = IGNORE)` với `@MappingTarget User`.
- Sau khi map, service trim `fullName` và `phone`. Nếu `phone` rỗng sau khi trim thì lưu `null`, tức là xóa số điện thoại.
- `dateOfBirth` chỉ sửa được, không xóa được (hạn chế đã chấp nhận).

**Upload avatar**, xử lý theo thứ tự:
1. File không có hoặc rỗng → `AVATAR_REQUIRED`.
2. File lớn hơn 2MB → `AVATAR_TOO_LARGE`. Giới hạn được chặn ở `spring.servlet.multipart.max-file-size=2MB`, và service kiểm tra lại lần nữa.
3. Content-type phải thuộc `image/jpeg`, `image/png`, `image/webp`, **và** magic bytes phải khớp đúng loại đó. Không thỏa thì trả `INVALID_AVATAR_TYPE`.
   - JPEG: `FF D8 FF`
   - PNG: `89 50 4E 47 0D 0A 1A 0A`
   - WEBP: byte 0–3 là `RIFF` và byte 8–11 là `WEBP`
4. Upload với key `avatars/{userId}/{uuid}.{jpg|png|webp}`, content-type lấy từ loại đã xác định ở bước 3. Kết quả là URL `{S3_PUBLIC_URL}/{key}`.
5. Gán `avatarUrl` mới rồi lưu user. **Nếu lưu lỗi** thì xóa file vừa upload và ném lại lỗi gốc. Nếu chính bước dọn file này cũng lỗi thì chỉ ghi log `warn`, không để lỗi đó che mất lỗi gốc.
6. Xóa avatar cũ nếu URL cũ thuộc bucket này. Xóa lỗi thì chỉ ghi log `warn`, request vẫn thành công.

**Đổi mật khẩu:**
1. `currentPassword` không khớp hash → `WRONG_PASSWORD`.
2. `newPassword` khớp hash hiện tại → `PASSWORD_UNCHANGED`.
3. Mã hóa `newPassword` bằng `PasswordEncoder` rồi lưu.

Token đã cấp trước đó vẫn dùng được tới khi hết hạn.

### 4.4 Thành phần code mới

| Thành phần | Package | Vai trò |
|---|---|---|
| `ProfileController` | `controller` | `@Tag(name = "Profile")`. `UserController` giữ nguyên, chỉ lo auth. |
| `ProfileService` | `service` | Profile, avatar, đổi mật khẩu |
| `FileStorageService` | `service` | Interface gồm `String upload(String key, byte[] content, String contentType)` (trả public URL) và `void delete(String url)` (bỏ qua URL không thuộc bucket) |
| `S3FileStorageService` | `service` | Cài đặt bằng AWS SDK v2 `S3Client`. `endpointOverride` bật khi có `S3_ENDPOINT`, dùng path-style access. Lỗi từ SDK được đổi thành `AppException(FILE_UPLOAD_FAILED)`. |
| `ImageTypeDetector` | `service` | Hàm thuần: nhận bytes, trả loại ảnh theo magic bytes |
| `StorageProperties` | `configuration` | `@ConfigurationProperties("storage.s3")` |
| `S3Config` | `configuration` | Bean `S3Client` |
| `UserProfileResponse`, `UpdateProfileRequest`, `ChangePasswordRequest` | `dto` | |
| `UserMapper` | `mapper` | Thêm `toUserProfileResponse(User)` và `updateProfile(@MappingTarget User, UpdateProfileRequest)` |

## 5. nutrition-service

### 5.1 Khung project

- Maven project độc lập tại `nutrition-service/`: `groupId com.veggiepal`, `artifactId nutrition-service`, Spring Boot 4.1.1, Java 21. Maven wrapper copy từ identity-service.
- **Dependencies:** webmvc, data-jpa, security, OAuth2 resource server, validation, mysql-connector-j, lombok, mapstruct 1.6.3, springdoc-openapi-starter-webmvc-ui, devtools, cùng test và spring-security-test. Không cần jjwt.
- **Package gốc:** `com.veggiepal.nutrition`. Class main là `NutritionServiceApplication`. Sub-package giống identity-service: `configuration, controller, dto/request, dto/response, entity, enums, exception, mapper, repository, service`.
- **`application.properties`:**
  - port `8082`
  - datasource như mục 3.4
  - `ddl-auto=update`, `open-in-view=false`
  - `spring.sql.init.mode=always`, `spring.jpa.defer-datasource-initialization=true`
  - `jwt.secret`
  - springdoc paths giống identity-service

### 5.2 Dữ liệu

**`health_records`** (entity `HealthRecord`)

| Cột | Kiểu | Ràng buộc |
|---|---|---|
| `id` | BIGINT | PK, identity |
| `user_id` | BIGINT | NOT NULL |
| `height_cm` | DECIMAL(4,1) | NOT NULL |
| `weight_kg` | DECIMAL(4,1) | NOT NULL |
| `bmi` | DECIMAL(5,1) | NOT NULL. Giá trị lớn nhất có thể là 300 / 0.5² = 1200.0, nên cần precision 5. |
| `recorded_at` | DATETIME | NOT NULL, không thay đổi sau khi tạo |
| `created_at` / `updated_at` | DATETIME | NOT NULL, dùng `@PrePersist` / `@PreUpdate` như `User` |

Index: `(user_id, recorded_at)`.

**`allergens`** (entity `Allergen`)

| Cột | Kiểu | Ràng buộc |
|---|---|---|
| `id` | BIGINT | PK, identity |
| `code` | VARCHAR | NOT NULL, UNIQUE |
| `name` | VARCHAR | NOT NULL, tên tiếng Việt |
| `category` | VARCHAR (enum `AllergenCategory`, `EnumType.STRING`) | NOT NULL |

`AllergenCategory` gồm: `GRAIN, LEGUME, NUT_SEED, VEGETABLE, FRUIT, MUSHROOM, SPICE, ADDITIVE`.

**`user_allergies`** (entity `UserAllergy`)

| Cột | Kiểu | Ràng buộc |
|---|---|---|
| `id` | BIGINT | PK, identity |
| `user_id` | BIGINT | NOT NULL |
| `allergen_id` | BIGINT | NOT NULL, FK tới `allergens.id` (`@ManyToOne`) |
| `created_at` | DATETIME | NOT NULL |

Ràng buộc unique: `(user_id, allergen_id)`.

### 5.3 API

Controller map `/nutrition/**`. Mọi endpoint yêu cầu JWT.

| Method | Path (qua gateway) | Request | `result` |
|---|---|---|---|
| POST | `/api/nutrition/me/health-records` | `HealthRecordRequest` | `HealthRecordResponse` |
| GET | `/api/nutrition/me/health-records?page=0&size=20` | — | `PageResponse<HealthRecordResponse>` |
| GET | `/api/nutrition/me/health-records/latest` | — | `HealthRecordResponse` |
| PUT | `/api/nutrition/me/health-records/{id}` | `HealthRecordRequest` | `HealthRecordResponse` |
| GET | `/api/nutrition/allergens` | — | `List<AllergenResponse>` |
| GET | `/api/nutrition/me/allergies` | — | `List<AllergenResponse>` |
| PUT | `/api/nutrition/me/allergies` | `UpdateAllergiesRequest` | `List<AllergenResponse>` |

**`HealthRecordRequest`:**

| Field | Validation |
|---|---|
| `heightCm` (BigDecimal) | `@NotNull(HEIGHT_REQUIRED)`, `@DecimalMin("50")` / `@DecimalMax("250")` / `@Digits(integer = 3, fraction = 1)` đều trả `INVALID_HEIGHT` |
| `weightKg` (BigDecimal) | `@NotNull(WEIGHT_REQUIRED)`, `@DecimalMin("20")` / `@DecimalMax("300")` / `@Digits(integer = 3, fraction = 1)` đều trả `INVALID_WEIGHT` |

**Các DTO còn lại:**
- `HealthRecordResponse`: `id, heightCm, weightKg, bmi, recordedAt`
- `PageResponse<T>`: `items, page, size, totalElements, totalPages`
- `AllergenResponse`: `id, code, name, category`
- `UpdateAllergiesRequest`: `List<@NotNull(message = "ALLERGEN_NOT_EXISTED") Long> allergenIds`, với `@NotNull(message = "ALLERGEN_IDS_REQUIRED")` cho cả danh sách

### 5.4 Quy tắc nghiệp vụ

**Tính BMI (BR-04):**
- `bmi = weightKg / (heightCm / 100)²`, dùng `BigDecimal`, làm tròn 1 chữ số theo `HALF_UP`.
- Ví dụ: 170 cm, 65 kg → 22.5.

**Health records:**
- **POST:** tính BMI, `recordedAt = now`, lưu với `userId` lấy từ token.
- **GET danh sách:**
  - sắp xếp `recordedAt DESC, id DESC`
  - `page < 0` thì dùng 0, `size` bị giới hạn trong khoảng 1–100, mặc định 20
  - không báo lỗi khi tham số phân trang sai, chỉ tự điều chỉnh
- **GET latest:** lấy bản ghi mới nhất theo cùng thứ tự sắp xếp. Chưa có bản ghi nào thì trả `HEALTH_RECORD_NOT_EXISTED` (404).
- **PUT {id}:**
  - Tìm theo `id` **và** `userId`. Không tìm thấy (kể cả khi bản ghi thuộc user khác) thì trả `HEALTH_RECORD_NOT_EXISTED` (404), để không lộ bản ghi của người khác có tồn tại hay không.
  - Ghi đè `heightCm` và `weightKg`, tính lại BMI, **giữ nguyên `recordedAt`**.
- Không có API xóa bản ghi.

**Allergens:**
- **Thứ tự sắp xếp (dùng chung cho cả 3 API allergens):** sắp xếp **trong service bằng Java**, theo thứ tự khai báo của enum `AllergenCategory` (GRAIN → ADDITIVE), rồi theo `name` bằng `Collator` tiếng Việt. Không dùng `ORDER BY` trong DB, vì Hibernate có thể tạo cột `category` dạng `ENUM` của MySQL, khi đó thứ tự sắp xếp phụ thuộc vào kiểu cột. FE tự nhóm theo `category`.
- **GET `/allergens`:** trả toàn bộ danh mục.
- **GET `/me/allergies`:** dị ứng của user.
- **PUT `/me/allergies`** (`@Transactional`, thay toàn bộ danh sách):
  1. Gộp các ID trùng.
  2. Load allergens theo danh sách ID. Nếu số lượng load được ít hơn số ID thì trả `ALLERGEN_NOT_EXISTED` và **không thay đổi gì**.
  3. Xóa các `UserAllergy` không còn trong danh sách, thêm các mục mới.
  4. Trả danh sách allergens đã chọn, sắp xếp theo thứ tự chung ở trên.
  - Gửi `[]` sẽ xóa toàn bộ dị ứng của user.

### 5.5 Dữ liệu seed: `nutrition-service/src/main/resources/data.sql`

Dùng `INSERT IGNORE INTO allergens (code, name, category) VALUES ...`. Chạy lại nhiều lần vẫn an toàn nhờ `code` unique.

| category | code | name |
|---|---|---|
| GRAIN | GLUTEN | Gluten (lúa mì, lúa mạch) |
| GRAIN | BUCKWHEAT | Kiều mạch |
| GRAIN | CORN | Bắp (ngô) |
| LEGUME | PEANUT | Đậu phộng |
| LEGUME | SOY | Đậu nành |
| LEGUME | MUNG_BEAN | Đậu xanh |
| NUT_SEED | CASHEW | Hạt điều |
| NUT_SEED | TREE_NUT | Hạt cây khác (hạnh nhân, óc chó, mắc ca) |
| NUT_SEED | SESAME | Mè (vừng) |
| NUT_SEED | COCONUT | Dừa |
| VEGETABLE | TOMATO | Cà chua |
| VEGETABLE | EGGPLANT | Cà tím |
| VEGETABLE | TARO | Khoai môn |
| VEGETABLE | CASSAVA | Khoai mì (sắn) |
| VEGETABLE | BAMBOO_SHOOT | Măng |
| VEGETABLE | CELERY | Cần tây |
| VEGETABLE | CORIANDER | Rau mùi (ngò) |
| VEGETABLE | GARLIC | Tỏi |
| VEGETABLE | ONION | Hành |
| FRUIT | MANGO | Xoài |
| FRUIT | PINEAPPLE | Dứa (thơm) |
| FRUIT | PAPAYA | Đu đủ |
| FRUIT | AVOCADO | Bơ |
| FRUIT | DURIAN | Sầu riêng |
| FRUIT | JACKFRUIT | Mít |
| FRUIT | LYCHEE | Vải |
| FRUIT | STRAWBERRY | Dâu tây |
| MUSHROOM | MUSHROOM | Nấm (các loại) |
| SPICE | CHILI | Ớt |
| SPICE | MUSTARD | Mù tạt |
| ADDITIVE | SULPHITE | Sulfite |
| ADDITIVE | MSG | Bột ngọt (MSG) |

Các nhóm có nguồn gốc động vật (sữa, trứng, hải sản) không có trong danh sách, vì hệ thống thuần chay (BR-06). Nhiều mục trong bảng là không dung nạp hoặc kích ứng chứ không phải dị ứng theo nghĩa y khoa, nên FE nên đặt nhãn là "Dị ứng / Thực phẩm cần tránh".

### 5.6 Thành phần code mới

`HealthRecordController`, `AllergyController` (gồm cả `/allergens`), `HealthRecordService`, `AllergyService`, `HealthRecordRepository`, `AllergenRepository`, `UserAllergyRepository`, `HealthRecordMapper`, `AllergenMapper`, các DTO ở mục 5.3, cùng các class copy từ identity-service theo mục 3.1.

## 6. Xử lý lỗi

### 6.1 Lỗi từ Spring Security

Các lỗi này xảy ra trong filter chain, trước khi request tới `@ControllerAdvice`. Mỗi service cần thêm:

- **`AuthenticationEntryPoint`:** HTTP 401, body `ApiResponse{code: 1008, message: "Unauthenticated"}`. Dùng chung cho mọi trường hợp: thiếu token, hết hạn, sai chữ ký, sai định dạng. Không trả lý do cụ thể.
- **`AccessDeniedHandler`:** HTTP 403, body `ApiResponse{code: 1009}`.

### 6.2 Bổ sung `GlobalExceptionHandler`

| Exception | Mã | Áp dụng cho |
|---|---|---|
| `HttpMessageNotReadableException` | 1018 `INVALID_REQUEST` | cả hai service |
| `MethodArgumentTypeMismatchException` | 1018 `INVALID_REQUEST` | cả hai service |
| `MaxUploadSizeExceededException` | 1016 `AVATAR_TOO_LARGE` | identity-service |
| `MissingServletRequestPartException` | 1014 `AVATAR_REQUIRED` | identity-service |

### 6.3 Danh mục mã lỗi

**Mã chung:** giữ đúng số ở cả hai service. 1018 là mã mới.

| Mã | Tên | HTTP | Message |
|---|---|---|---|
| 1001 | `INVALID_KEY` | 400 | Invalid validation key |
| 1008 | `UNAUTHENTICATED` | 401 | Unauthenticated |
| 1009 | `UNAUTHORIZED` | 403 | You do not have permission |
| 1018 | `INVALID_REQUEST` | 400 | Invalid request data |
| 9999 | `UNCATEGORIZED_EXCEPTION` | 500 | Uncategorized error |

**identity-service** (mã mới):

| Mã | Tên | HTTP | Message |
|---|---|---|---|
| 1011 | `WRONG_PASSWORD` | 400 | Current password is incorrect |
| 1012 | `PASSWORD_UNCHANGED` | 400 | New password must be different from current password |
| 1013 | `INVALID_DATE_OF_BIRTH` | 400 | Date of birth must be in the past |
| 1014 | `AVATAR_REQUIRED` | 400 | Avatar file is required |
| 1015 | `INVALID_AVATAR_TYPE` | 400 | Avatar must be a JPEG, PNG or WEBP image |
| 1016 | `AVATAR_TOO_LARGE` | 400 | Avatar must not exceed 2MB |
| 1017 | `FILE_UPLOAD_FAILED` | 503 | Could not upload file, please try again later |

**nutrition-service** (dải 2xxx):

| Mã | Tên | HTTP | Message |
|---|---|---|---|
| 2001 | `HEIGHT_REQUIRED` | 400 | Height is required |
| 2002 | `INVALID_HEIGHT` | 400 | Height must be between 50 and 250 cm with at most 1 decimal |
| 2003 | `WEIGHT_REQUIRED` | 400 | Weight is required |
| 2004 | `INVALID_WEIGHT` | 400 | Weight must be between 20 and 300 kg with at most 1 decimal |
| 2005 | `HEALTH_RECORD_NOT_EXISTED` | 404 | Health record not existed |
| 2006 | `ALLERGEN_IDS_REQUIRED` | 400 | Allergen list is required |
| 2007 | `ALLERGEN_NOT_EXISTED` | 400 | Allergen not existed |

## 7. Bảo mật và riêng tư

- Người dùng chỉ đọc và ghi được dữ liệu của chính mình. `userId` luôn lấy từ JWT, không bao giờ lấy từ body hay path (NFR 5.4).
- Không ghi vào log: mật khẩu, token, chiều cao, cân nặng, BMI, dị ứng (NFR 5.4, 5.5).
- Avatar được kiểm tra bằng magic bytes. Content-type khi lưu lên storage lấy từ loại ảnh đã xác định, không lấy từ header client gửi.
- Bucket avatar cho phép đọc công khai. Object key chứa UUID ngẫu nhiên nên không đoán được.

## 8. Kiểm thử

Viết theo TDD.

**Unit test** (JUnit 5 + Mockito, không khởi động Spring context):
- **identity-service:**
  - `ProfileService`:
    - PATCH bỏ qua field `null`, trim giá trị, `phone: ""` thành `null`
    - user không tồn tại → `USER_NOT_EXISTED`
    - đổi mật khẩu: sai mật khẩu hiện tại, mật khẩu mới trùng cũ, thành công (hash mới được lưu)
    - avatar: file rỗng; content-type sai; content-type đúng nhưng magic bytes sai; upload thành công thì avatar cũ bị xóa; xóa avatar cũ lỗi thì request vẫn thành công; lưu DB lỗi thì file mới bị xóa
  - `ImageTypeDetector`: nhận đúng JPEG, PNG, WEBP; từ chối bytes khác hoặc file quá ngắn.
  - **Test ký token rồi giải mã lại:** token do `JwtService` ký phải được bean `JwtDecoder` giải mã, và đọc được `userId` và `role`.
- **nutrition-service:**
  - `HealthRecordService`:
    - tính BMI và làm tròn (170/65 → 22.5)
    - `recordedAt` được gán khi tạo; sửa bản ghi thì giữ nguyên `recordedAt` và tính lại BMI
    - sửa hoặc lấy latest khi không có bản ghi hoặc bản ghi thuộc user khác → 2005
    - page và size được điều chỉnh vào khoảng hợp lệ
  - `AllergyService`: thay cả danh sách (chỉ xóa và thêm phần chênh lệch), gộp ID trùng, `[]` xóa hết, ID không tồn tại thì báo 2007 và không gọi save hay delete.

**Controller slice test** (`@WebMvcTest` + `SecurityMockMvcRequestPostProcessors.jwt()`, service được mock, import `SecurityConfig`):
- Không có token → 401 với body `code: 1008`.
- Token hợp lệ → 200, và `userId` từ claim được truyền đúng xuống service.
- Validation trả đúng mã lỗi: `heightCm: 10` → 2002; thiếu `weightKg` → 2003; `fullName: "  "` → 1006; `dateOfBirth` ở tương lai → 1013.
- JSON sai cú pháp → 1018. Path `/health-records/abc` → 1018.
- Endpoint public `/auth/login` có kèm header Bearer đã hết hạn **không bị** trả 401.

**Kiểm tra thủ công end-to-end qua gateway** (Swagger tại `:8080/swagger-ui.html`), bắt buộc làm trước khi báo hoàn thành:
1. register → login
2. GET và PATCH `/api/users/me`
3. **Upload avatar qua gateway**, mở được URL ảnh trả về
4. Đổi mật khẩu, đăng nhập lại bằng mật khẩu mới
5. Dùng token đó gọi nutrition-service: tạo, sửa, xem danh sách và latest health record
6. GET allergens, PUT rồi GET lại dị ứng
7. Gọi một endpoint bất kỳ không kèm token → 401 với body `ApiResponse`

Test `contextLoads` giữ theo convention hiện tại, vẫn cần MySQL đang chạy.

## 9. Rủi ro và điểm cần xác minh (đã xác minh khi lập plan)

| # | Rủi ro | Cách xử lý |
|---|---|---|
| R1 | Gateway WebMVC có thể tự parse multipart (giới hạn mặc định 1MB) trước khi chuyển tiếp, làm hỏng hoặc chặn request upload avatar. | **Đã xác minh (gateway 5.0.3):** `MultipartEnvironmentPostProcessor` của gateway tự đặt `spring.servlet.multipart.enabled=false` nếu project chưa khai báo, nên body multipart được chuyển thẳng xuống service. **Không được** khai báo `spring.servlet.multipart.*` trong gateway. Bước 3 của kiểm tra thủ công sẽ xác nhận lại. |
| R2 | Spring Boot 4 đã tách và đổi tên một số starter: OAuth2 resource server, các starter test cho `@WebMvcTest`. | **Đã xác minh (Boot 4.1.1):** dùng `spring-boot-starter-security-oauth2-resource-server` và `spring-boot-starter-webmvc-test`. `@WebMvcTest` nằm ở `org.springframework.boot.webmvc.test.autoconfigure`; `@MockitoBean` nằm ở `org.springframework.test.context.bean.override.mockito`. Spring Security là bản 7.1.1: `BearerTokenResolver` và `NimbusJwtDecoder.withSecretKey(...).macAlgorithm(...)` vẫn dùng được. |
| R3 | Spring Boot 4 dùng Jackson 3 (`tools.jackson`). Ảnh hưởng tới cách entry point và access denied handler ghi JSON. | **Đã xác minh:** Boot tạo sẵn bean `tools.jackson.databind.json.JsonMapper`. `@WebMvcTest` cũng có bean này, vì `@AutoConfigureWebMvc` được gắn `@AutoConfigureJson`. |
| R4 | Cách MinIO phát hành image Docker community đã thay đổi. | **Đã xác minh:** `minio/minio` và `minio/mc` không còn trên Docker Hub. Dùng `quay.io/minio/minio:RELEASE.2025-09-07T16-13-09Z` và `quay.io/minio/mc:RELEASE.2025-08-13T08-35-41Z`. |
| R5 | Token cũ (HS384) bị từ chối sau khi triển khai. | Chấp nhận; người dùng đăng nhập lại. |
| R6 | Token vẫn dùng được sau khi đổi mật khẩu. | Chấp nhận; ghi nhận là hạn chế đã biết. |

**Vấn đề phát hiện trong code hiện tại, không sửa trong module này:**
- `RegisterRequest.password` chỉ có `@Size`. Nếu gửi `null`, request đi qua validation rồi gây lỗi 500 khi mã hóa mật khẩu.
- Login không kiểm tra `UserStatus`, nên tài khoản BLOCKED vẫn đăng nhập được.
- DB `veggiepal_identity` không tự tạo được (thiếu `createDatabaseIfNotExist`).

## 10. Các quyết định đã chốt

| Quyết định | Lựa chọn | Lý do |
|---|---|---|
| Vị trí dữ liệu | identity: profile, avatar, mật khẩu. nutrition: health records, dị ứng | Dữ liệu dinh dưỡng nằm cạnh Meal Planner sau này |
| Lưu profile | Giữ trong bảng `users` (thêm `date_of_birth`) | Không đụng tới register và login đã merge |
| Health data | Lưu lịch sử, bản ghi mới nhất là giá trị hiện tại | Khớp SRS 6.1 ("thời điểm ghi nhận") |
| Sửa health record | PUT, không có DELETE | Dùng lại DTO của POST; BMI luôn tính từ đủ dữ liệu |
| Dị ứng | Bảng `allergens` được seed, có `category` | Dữ liệu sạch để lọc theo BR-05; hơn 30 mục cần chia nhóm |
| Lưu avatar | S3 API: MinIO ở local, S3 ở production | Chuẩn, dùng lại được cho upload video |
| Xác thực | OAuth2 Resource Server ở mỗi service, HS256 chung secret | Ít code tự viết, service vẫn an toàn khi bị gọi thẳng vào port |
| Sửa profile | PATCH, `null` nghĩa là giữ nguyên, `phone: ""` để xóa | Đơn giản với MapStruct, không thêm thư viện |
| Số điện thoại | Không kiểm tra định dạng | Theo yêu cầu |
| Mã lỗi | Mã chung giữ nguyên số; identity 10xx; nutrition 20xx | FE xử lý lỗi chung một cách thống nhất |
| Kiểm thử | Unit test + controller slice test + kiểm tra thủ công E2E; không có DB integration test | Repository chỉ dùng query sinh từ tên method |
