# Chuyển lưu trữ ảnh từ MinIO/S3 sang Cloudinary — Design Spec

| Thuộc tính | Giá trị |
|---|---|
| Ngày | 2026-10-14 |
| Nhánh | `refactor/change-image-storage-service` |
| Trạng thái | Đã duyệt thiết kế, chờ review spec |
| Phạm vi service | identity-service (avatar), blog-service (thumbnail) |

## 1. Mục tiêu

Ảnh avatar và thumbnail blog được lưu trên Cloudinary thay cho MinIO/S3. Credential Cloudinary nằm trong file `.env` ở gốc repo và không bao giờ được commit.

Hợp đồng API với FE **không đổi**. Response vẫn trả `avatarUrl`/`thumbnailUrl` dạng chuỗi, chỉ có giá trị đổi từ `http://localhost:9000/...` sang `https://res.cloudinary.com/<cloud>/image/upload/...`.

## 2. Phạm vi

**Trong phạm vi**
- identity-service, blog-service: thay implementation của `FileStorageService`, cấu hình, dependency.
- Nạp `.env` vào Spring Boot.
- `.gitignore`, `.env.example`, `docker-compose.yml`.
- Test cho implementation mới ở cả hai service.
- Cập nhật `CLAUDE.md`, `README.md`, `docs/identity-service-guide.md`, `docs/blog-service-guide.md`.

**Ngoài phạm vi**
- Giữ MinIO song song hoặc chọn provider bằng property. Đã quyết định thay hẳn.
- Migrate ảnh cũ trên MinIO. Đây là dữ liệu dev, ai cần thì upload lại.
- Upload ảnh cho recipe-service. Service này chỉ nhận `imageUrl` dạng chuỗi do FE gửi.
- Transformation hoặc resize của Cloudinary, signed URL, upload thẳng từ FE.
- Folder gốc riêng (ví dụ `veggiepal/`) trên Cloudinary. Chỉ cần khi account dùng chung với dự án khác.

## 3. Kiến trúc

```
FE ──► api-gateway :8080 ─┬─► identity-service :8081 ──► Cloudinary (avatars/…)
                          └─► blog-service     :8083 ──► Cloudinary (thumbnails/…)
```

Hai service dùng chung một account Cloudinary. Key hiện tại đã có tiền tố `avatars/<userId>/<uuid>` và `thumbnails/<blogId>/<uuid>`, nên không thể trùng nhau.

Mỗi service vẫn tự giữ bản copy của các lớp storage, không tạo module dùng chung, giống quy ước hiện tại của repo.

### 3.1 Những gì giữ nguyên

- Interface `FileStorageService` (`upload(key, content, contentType)` và `delete(url)`), kèm hợp đồng: `delete` bỏ qua URL không do chính nó tạo ra.
- `ProfileService.uploadAvatar`, `BlogService.uploadThumbnail`, các bước validate (kích thước, content type, magic bytes qua `ImageTypeDetector`) và logic dọn ảnh (`deleteQuietly`).
- `ProfileServiceTest` và `BlogServiceTest`. Hai test này mock interface nên không bị ảnh hưởng. Các URL MinIO trong fixture chỉ là chuỗi bất kỳ, giữ nguyên.
- Error code `FILE_UPLOAD_FAILED` (1017, HTTP 503).

## 4. Cấu hình và secret

### 4.1 File `.env`

Đặt ở gốc repo (`VeggiePal_BE/.env`), dùng chung cho cả hai service:

```properties
CLOUDINARY_CLOUD_NAME=...
CLOUDINARY_API_KEY=...
CLOUDINARY_API_SECRET=...
```

- **Trước khi** tạo `.env`, thêm `.env` vào `.gitignore`. Hiện tại `.gitignore` chưa chặn file này.
- Commit kèm `.env.example`, giữ nguyên ba key nhưng để trống giá trị.
- Các thành viên khác nhận giá trị thật qua kênh riêng, không qua git.

### 4.2 Nạp `.env` vào Spring Boot

Spring Boot không tự đọc `.env`. Mỗi service thêm vào `application.properties`:

```properties
spring.config.import=optional:file:.env[.properties],optional:file:../.env[.properties]
```

- `../.env` dùng khi chạy `./mvnw spring-boot:run` hoặc `./mvnw test` trong thư mục service (working dir là thư mục service).
- `.env` dùng khi IDE chạy với working dir là gốc repo.
- `[.properties]` báo cho Spring đọc file theo định dạng properties. Định dạng `KEY=VALUE` của `.env` tương thích hoàn toàn.
- `optional:` để service vẫn khởi động khi không có file, ví dụ trong container Docker, nơi biến được truyền qua môi trường.

### 4.3 Property

```properties
storage.cloudinary.cloud-name=${CLOUDINARY_CLOUD_NAME:}
storage.cloudinary.api-key=${CLOUDINARY_API_KEY:}
storage.cloudinary.api-secret=${CLOUDINARY_API_SECRET:}
```

Mặc định rỗng là có chủ đích. Thiếu `.env` thì service vẫn khởi động, các test `contextLoads` và `BlogServiceIntegrationTests` vẫn chạy được mà không cần secret. Chỉ riêng thao tác upload thất bại, trả `FILE_UPLOAD_FAILED` và có log lỗi. Vì giá trị rỗng là chuỗi `""` chứ không phải null, SDK vẫn gửi request; Cloudinary trả lỗi HTTP, SDK ném `RuntimeException` và lỗi này được chuyển thành `FILE_UPLOAD_FAILED`.

Toàn bộ property `storage.s3.*` bị xoá.

### 4.4 docker-compose

- Xoá service `minio`, `minio-init` và volume `minio_data`.
- `identity-service`: xoá các biến `S3_*` và khối `depends_on: minio`, thêm:
  ```yaml
  env_file:
    - path: .env
      required: false
  ```
  Đặt `required: false` để lệnh `docker compose up -d` (thường dùng để bật MySQL) không lỗi với người chưa có `.env`.

## 5. Thành phần code

Mỗi service có các thay đổi giống nhau. Package của identity-service là `com.veggiepal`, của blog-service là `com.veggiepal.blog`.

| File | Thay đổi |
|---|---|
| `pom.xml` | Bỏ `software.amazon.awssdk:s3`, BOM AWS trong `dependencyManagement` và property `aws-sdk.version`. Thêm `com.cloudinary:cloudinary-http5:2.5.0`. |
| `configuration/StorageProperties.java` | `@ConfigurationProperties(prefix = "storage.cloudinary") record StorageProperties(String cloudName, String apiKey, String apiSecret)` |
| `configuration/S3Config.java` | Xoá. |
| `configuration/CloudinaryConfig.java` | Mới. `@EnableConfigurationProperties(StorageProperties.class)`, bean `Cloudinary` khởi tạo từ `cloud_name`, `api_key`, `api_secret` và `secure=true`. |
| `service/S3FileStorageService.java` | Xoá. |
| `service/CloudinaryFileStorageService.java` | Mới, implement `FileStorageService` (mục 5.1). |

### 5.1 `CloudinaryFileStorageService`

**`upload(key, content, contentType)`**
1. `publicId` = `key` bỏ phần đuôi sau dấu `.` cuối cùng. Ví dụ `avatars/7/abc.png` thành `avatars/7/abc`. Cloudinary tự thêm định dạng vào URL; nếu để nguyên đuôi trong public_id thì URL sẽ có đuôi bị lặp.
2. Gọi `cloudinary.uploader().upload(content, {public_id: publicId, resource_type: "image", overwrite: false})`.
3. Trả về `secure_url` lấy từ kết quả.
4. Bỏ qua `contentType`. Cloudinary tự nhận dạng định dạng từ byte, còn magic bytes đã được `ImageTypeDetector` kiểm tra trước khi vào hàm này.

**`delete(url)`**
1. `url` là `null` hoặc không bắt đầu bằng `https://res.cloudinary.com/<cloudName>/image/upload/` thì return, không gọi SDK. Các URL MinIO cũ vẫn nằm trong DB nên rơi vào trường hợp này.
2. Phần còn lại sau prefix có dạng `v<số>/avatars/7/abc.png`. Bỏ đoạn version `v\d+/` nếu có, rồi bỏ đuôi file, thu được `avatars/7/abc`.
3. Gọi `cloudinary.uploader().destroy(publicId, {resource_type: "image", invalidate: true})`. Nếu kết quả là `"not found"` thì không coi là lỗi.

**Lỗi:** Ở cả hai hàm, `IOException` (lỗi mạng) và `RuntimeException` (SDK ném khi API trả lỗi, sai credential hoặc thiếu `cloud_name`) đều được log và chuyển thành `AppException(ErrorCode.FILE_UPLOAD_FAILED)`. Cách này giống `S3FileStorageService` cũ. Phía gọi đã có `deleteQuietly` nên lỗi khi xoá không làm hỏng request.

## 6. Dữ liệu cũ

Các bản ghi `users.avatar_url` và `blogs.thumbnail_url` đang trỏ về `http://localhost:9000/...` sẽ thành ảnh hỏng sau khi gỡ MinIO. `delete` bỏ qua những URL này nên upload ảnh mới vẫn chạy bình thường, và ảnh mới ghi đè URL cũ. Không có migration.

## 7. Kiểm thử

**Unit test** `CloudinaryFileStorageServiceTest` cho từng service, mock `Cloudinary` và `Uploader`:
- upload gửi `public_id` không có đuôi và `resource_type=image`, trả về `secure_url`;
- upload: SDK ném `IOException` hoặc `RuntimeException` thì trả `FILE_UPLOAD_FAILED`;
- delete URL có đoạn version `v123/` thì gọi `destroy` với public_id đúng;
- delete URL không có đoạn version cũng ra public_id đúng;
- delete `null`, URL MinIO cũ hoặc URL của cloud khác thì không tương tác với SDK;
- delete: SDK lỗi thì trả `FILE_UPLOAD_FAILED`.

identity-service: test này thay `S3FileStorageServiceTest`. blog-service: test mới, vì trước đây chưa có.

**Gate (bắt buộc):**
- identity-service: `./mvnw test`, gồm `VeggiepalApplicationTests`.
- blog-service: `./mvnw test`, rồi `./mvnw test -Dtest='BlogServiceApplicationTests,BlogServiceIntegrationTests'`.
- Chạy gate một lần khi có `.env` và một lần khi tạm đổi tên `.env`. Cả hai lần context đều phải load được.

**Smoke test thủ công** qua Swagger của gateway:
1. `POST /api/users/me/avatar`: response có `avatarUrl` dạng `https://res.cloudinary.com/r3ubxlz3/...`, mở được trên trình duyệt.
2. Upload avatar lần hai: ảnh cũ biến mất khỏi Media Library.
3. Lặp lại hai bước trên với `POST /api/blogs/{id}/thumbnail`.

## 8. Rủi ro

| Rủi ro | Cách xử lý |
|---|---|
| Lỡ commit `.env` | Thêm vào `.gitignore` trước khi tạo file. Nếu lỡ commit thì rotate API secret trong Cloudinary console. |
| Thành viên chưa có `.env` | Service vẫn chạy, chỉ upload ảnh trả 503. README hướng dẫn copy `.env.example`. |
| `httpclient5` của Cloudinary xung đột với phiên bản Spring Boot quản lý | Build và chạy gate. Nếu xung đột thì xử lý bằng exclusion trong pom. |
| Hạn mức gói Free của Cloudinary | Avatar ≤ 2MB, thumbnail ≤ 5MB, đủ cho dev. |
