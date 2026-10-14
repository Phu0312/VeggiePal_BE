# VeggiePal Backend

Backend microservice cho VeggiePal / VeganApp — nền tảng cộng đồng và dinh dưỡng thuần chay. Repository hiện cung cấp xác thực người dùng, hồ sơ cá nhân, theo dõi BMI, quản lý dị ứng, blog cộng đồng, danh mục, bình luận và bình chọn qua một API Gateway thống nhất.

> Dự án đang trong quá trình phát triển. Recipe, video, AI chatbot, meal plan và tìm nhà hàng chưa được triển khai. Xem [roadmap tiếng Việt](IMPLEMENTATION_PHASES_VI.md) hoặc [roadmap tiếng Anh](IMPLEMENTATION_PHASES.md) để biết trạng thái và thứ tự triển khai đề xuất.

## Mục lục

- [Kiến trúc hiện tại](#kiến-trúc-hiện-tại)
- [Công nghệ](#công-nghệ)
- [Cấu trúc repository](#cấu-trúc-repository)
- [Yêu cầu môi trường](#yêu-cầu-môi-trường)
- [Khởi chạy local](#khởi-chạy-local)
- [Cấu hình](#cấu-hình)
- [API và Swagger](#api-và-swagger)
- [Kiểm thử](#kiểm-thử)
- [Dữ liệu và lưu trữ](#dữ-liệu-và-lưu-trữ)
- [Quy ước phát triển](#quy-ước-phát-triển)
- [Trạng thái chức năng](#trạng-thái-chức-năng)
- [Tài liệu](#tài-liệu)

## Kiến trúc hiện tại

```text
Web / Mobile / Admin Client
            |
            v
 api-gateway :8080
    |       |       |
    |       |       +----> blog-service :8083 ----> MySQL: veggiepal_blog
    |       |                    |
    |       |                    +---------------> Cloudinary thumbnails
    |       |
    |       +------------> nutrition-service :8082 -> MySQL: veggiepal_nutrition
    |
    +--------------------> identity-service :8081 -> MySQL: veggiepal_identity
                                      |
                                      +-----------> Cloudinary avatars
```

Mỗi service là một Maven project độc lập, có Maven Wrapper riêng và không có parent/aggregator POM ở thư mục gốc.

| Thành phần | Port | Trách nhiệm hiện tại |
|---|---:|---|
| `api-gateway` | 8080 | Routing, CORS, Swagger UI tổng hợp |
| `identity-service` | 8081 | Đăng ký/đăng nhập, JWT, hồ sơ, mật khẩu, avatar, tra cứu tác giả |
| `nutrition-service` | 8082 | Lịch sử chiều cao/cân nặng/BMI, danh mục dị ứng và dị ứng người dùng |
| `blog-service` | 8083 | Blog, danh mục, bình luận/reply, vote, thumbnail, trạng thái nội dung |
| MySQL | 3307 (host) | Ba schema độc lập cho ba domain service |
| Cloudinary | — | Lưu ảnh avatar và thumbnail (dịch vụ ngoài, credential trong `.env`) |

### Nguyên tắc sở hữu dữ liệu

- `identity-service` là nơi duy nhất sở hữu tài khoản và thông tin xác thực.
- `nutrition-service` chỉ lưu `userId` lấy từ JWT; không truy cập database identity.
- `blog-service` chỉ lưu `authorId`/`userId`; client có thể dùng `GET /api/users/batch` để lấy tên và avatar.
- Không tạo foreign key giữa database của các service.
- Giao tiếp liên service trong tương lai phải qua API contract rõ ràng.

## Công nghệ

- Java 21
- Spring Boot 4.1.1
- Spring Cloud Gateway Server WebMVC 2025.1.3
- Spring MVC, Spring Data JPA, Spring Security OAuth2 Resource Server
- MySQL 8.4
- JWT HS256 (`jjwt` phát hành token, Nimbus decoder xác thực token)
- MapStruct và Lombok
- Cloudinary Java SDK (`cloudinary-http5`) để lưu ảnh
- Springdoc OpenAPI / Swagger UI
- JUnit 5, Mockito, Spring Security Test
- Docker Compose cho hạ tầng local

## Cấu trúc repository

```text
VeganApp_BE/
├── api-gateway/              # Gateway và Swagger tổng hợp
├── identity-service/         # Authentication và hồ sơ người dùng
├── nutrition-service/        # BMI, health record và allergy
├── blog-service/             # Blog, category, comment và vote
├── docs/                     # Hướng dẫn và tài liệu thiết kế hiện có
├── docker-compose.yml        # MySQL local
├── .env.example              # Template credential Cloudinary (copy thành .env)
├── IMPLEMENTATION_PHASES.md  # Roadmap tiếng Anh
└── IMPLEMENTATION_PHASES_VI.md # Roadmap tiếng Việt
```

Mỗi service có cấu trúc chuẩn:

```text
src/main/java/.../
├── configuration/
├── controller/
├── dto/request/
├── dto/response/
├── entity/
├── exception/
├── mapper/
├── repository/
└── service/
```

## Yêu cầu môi trường

- JDK 21
- Docker Desktop hoặc Docker Engine có Docker Compose ≥ 2.24 (cần cho `env_file` có `required: false` trong `docker-compose.yml`)
- PowerShell trên Windows; có thể dùng shell tương đương trên Linux/macOS
- Các port `8080`–`8083`, `3307` đang trống

Không cần cài Maven toàn cục vì mỗi service có Maven Wrapper.

## Khởi chạy local

### 1. Khởi động MySQL

Tại thư mục gốc repository:

```powershell
docker compose up -d mysql
docker compose ps
```

Lệnh trên chỉ khởi động MySQL. `docker-compose.yml` còn định nghĩa container `identity-service` (port 8081); đừng bật nó khi chạy identity-service bằng Maven ở bước 4, vì hai bên trùng port.

### 2. Tạo database cho identity-service

`nutrition-service` và `blog-service` có `createDatabaseIfNotExist=true`. `identity-service` hiện chưa có tùy chọn này nên cần tạo schema một lần:

```powershell
docker exec veggiepal-mysql mysql -uroot -p12345 -e "CREATE DATABASE IF NOT EXISTS veggiepal_identity"
```

Các schema được dùng:

- `veggiepal_identity`
- `veggiepal_nutrition`
- `veggiepal_blog`

### 3. Cấu hình Cloudinary (`.env`)

Ảnh avatar và thumbnail được lưu trên Cloudinary. Credential nằm trong file `.env` ở thư mục gốc repository. File này đã bị `.gitignore` chặn và **không bao giờ được commit**.

```powershell
Copy-Item .env.example .env
```

Điền ba giá trị, lấy từ Cloudinary Console (Settings → API Keys) hoặc từ trưởng nhóm:

```properties
CLOUDINARY_CLOUD_NAME=...
CLOUDINARY_API_KEY=...
CLOUDINARY_API_SECRET=...
```

identity-service và blog-service tự đọc file này qua `spring.config.import`. Thiếu `.env` thì service vẫn khởi động, chỉ riêng upload ảnh trả 503 (`FILE_UPLOAD_FAILED`).

### 4. Khởi động từng service

Mở bốn terminal riêng biệt từ thư mục gốc:

```powershell
cd identity-service
.\mvnw.cmd spring-boot:run
```

```powershell
cd nutrition-service
.\mvnw.cmd spring-boot:run
```

```powershell
cd blog-service
.\mvnw.cmd spring-boot:run
```

```powershell
cd api-gateway
.\mvnw.cmd spring-boot:run
```

### 5. Kiểm tra

- Gateway: `http://localhost:8080`
- Swagger tổng hợp: `http://localhost:8080/swagger-ui.html`

Mọi request từ frontend nên đi qua Gateway thay vì gọi thẳng service.

## Cấu hình

Các service hiện có giá trị mặc định phục vụ phát triển local. Nên thiết lập biến môi trường ở mọi môi trường dùng chung hoặc production.

| Biến | Service | Ý nghĩa | Mặc định local hiện tại |
|---|---|---|---|
| `JWT_SECRET` | identity, nutrition, blog | Secret ký/xác thực JWT; phải giống nhau | Secret development trong properties |
| `CLOUDINARY_CLOUD_NAME` | identity, blog | Cloud name của account Cloudinary | Rỗng (đọc từ `.env`) |
| `CLOUDINARY_API_KEY` | identity, blog | API key Cloudinary | Rỗng (đọc từ `.env`) |
| `CLOUDINARY_API_SECRET` | identity, blog | API secret Cloudinary | Rỗng (đọc từ `.env`) |

Datasource username/password/URL hiện được đặt trực tiếp trong từng `application.properties`. Roadmap yêu cầu chuyển chúng sang cấu hình môi trường và bổ sung migration trước khi triển khai production.

### JWT

- Token hiện có thời hạn 24 giờ.
- Subject là email; claims gồm `userId` và `role`.
- Các resource service xác thực cùng HS256 secret.
- Chưa có refresh token, revoke/logout thực sự, issuer/audience validation hoặc key rotation.
- Không sử dụng secret mặc định ngoài môi trường local.

## API và Swagger

Gateway thêm prefix `/api`, sau đó bỏ một path segment trước khi forward đến service.

### Authentication và user

| Method | Gateway path | Mô tả |
|---|---|---|
| `POST` | `/api/auth/register` | Đăng ký tài khoản |
| `POST` | `/api/auth/login` | Đăng nhập và nhận access token |
| `GET` | `/api/users/me` | Lấy hồ sơ hiện tại |
| `PATCH` | `/api/users/me` | Cập nhật hồ sơ |
| `PUT` | `/api/users/me/password` | Đổi mật khẩu |
| `POST` | `/api/users/me/avatar` | Upload avatar |
| `GET` | `/api/users/batch?ids=1,2` | Lấy tên/avatar công khai của tối đa 50 user |

### Nutrition

| Method | Gateway path | Mô tả |
|---|---|---|
| `GET` | `/api/nutrition/allergens` | Danh mục chất/nguyên liệu dị ứng |
| `GET` | `/api/nutrition/me/allergies` | Dị ứng của user hiện tại |
| `PUT` | `/api/nutrition/me/allergies` | Thay toàn bộ danh sách dị ứng |
| `POST` | `/api/nutrition/me/health-records` | Ghi chiều cao/cân nặng; server tính BMI |
| `GET` | `/api/nutrition/me/health-records` | Lịch sử health record có phân trang |
| `GET` | `/api/nutrition/me/health-records/latest` | Health record mới nhất |
| `PUT` | `/api/nutrition/me/health-records/{id}` | Chỉnh health record của chính user |

### Blog, category, comment và vote

| Nhóm | Gateway path chính | Mô tả |
|---|---|---|
| Blog | `/api/blogs/**` | CRUD/draft/submit, public search, related, thumbnail, view count |
| Category | `/api/categories/**` | Cây category công khai; admin tạo/sửa/xóa |
| Comment | `/api/comments/**` | Comment gốc, reply một cấp, sửa/xóa |
| Vote | `/api/blogs/{id}/vote` | Vote `1` hoặc `-1`; gửi lại cùng giá trị để bỏ vote |

Các API đọc blog/category/comment công khai được phép không cần token. API tạo/sửa/xóa cần Bearer token. Swagger của từng service khai báo `bearerAuth` để thử API bảo vệ.

### Response envelope

Endpoint trả về cấu trúc thống nhất:

```json
{
  "code": 1000,
  "message": "...",
  "result": {}
}
```

Lỗi authentication/authorization cũng dùng envelope JSON thay vì response mặc định của Spring Security.

## Kiểm thử

Chạy test trong đúng thư mục của từng service.

### Toàn bộ test của một service

```powershell
cd identity-service
.\mvnw.cmd test
```

Thay thư mục bằng `nutrition-service`, `blog-service` hoặc `api-gateway` khi cần.

### Fast test không cần MySQL

```powershell
cd identity-service
.\mvnw.cmd test "-Dtest=!VeggiepalApplicationTests"
```

```powershell
cd nutrition-service
.\mvnw.cmd test "-Dtest=!NutritionServiceApplicationTests"
```

```powershell
cd blog-service
.\mvnw.cmd test "-Dtest=!BlogServiceApplicationTests,!BlogServiceIntegrationTests"
```

### Integration test blog-service

MySQL phải đang chạy:

```powershell
cd blog-service
.\mvnw.cmd test "-Dtest=BlogServiceApplicationTests,BlogServiceIntegrationTests"
```

`BlogServiceIntegrationTests` là gate bắt buộc khi thay đổi entity, repository hoặc JPA query của blog-service. Test dùng schema riêng `veggiepal_blog_it`.

### Build package

```powershell
.\mvnw.cmd clean package
```

Repository chưa có lệnh build tổng ở root; cần build từng Maven project.

## Dữ liệu và lưu trữ

### Database

- Mỗi domain service dùng schema MySQL riêng.
- `nutrition-service` seed danh mục allergen bằng `data.sql` với `INSERT IGNORE`.
- Hiện tại Hibernate dùng `ddl-auto=update`; chưa có Flyway/Liquibase.
- Không xem schema hiện tại là production-ready cho đến khi có versioned migrations.
- Hibernate có thể map enum thành MySQL `ENUM`; thêm enum Java không đảm bảo database tự cập nhật giá trị.

### Lưu trữ ảnh (Cloudinary)

- Avatar: Cloudinary `avatars/{userId}/...`, giới hạn 2 MB.
- Blog thumbnail: Cloudinary `thumbnails/{blogId}/...`, giới hạn nghiệp vụ 5 MB.
- JPEG, PNG và WEBP được kiểm tra cả MIME type lẫn magic bytes.
- Khi thay ảnh, service cố gắng xóa ảnh cũ; lỗi cleanup được log nhưng không làm mất bản ghi đã lưu.

## Quy ước phát triển

- Giữ luồng `controller -> service -> repository`.
- Controller lấy user hiện tại từ claim `userId`; không nhận user ID sở hữu từ request body.
- Dùng DTO request/response; không trả JPA entity trực tiếp.
- Dùng `ApiResponse<T>` và `AppException(ErrorCode.X)` theo convention hiện có.
- Validation message là tên hằng `ErrorCode`, ví dụ `@NotBlank(message = "EMAIL_REQUIRED")`.
- Không tạo quan hệ JPA hoặc foreign key sang database service khác.
- Giữ Gateway routes và Swagger aggregation đồng bộ khi thêm endpoint/service.
- Luôn thêm test authorization cho public, member/owner và admin.
- Khi thêm external API/AI: có timeout, retry giới hạn, validate response và không log secret.
- Không commit password, token, API key hoặc credential thật.

## Trạng thái chức năng

| Domain | Trạng thái ngắn |
|---|---|
| Gateway/Swagger | Có nền tảng, còn thiếu cấu hình deploy và route test |
| Authentication/JWT | Có đăng ký, đăng nhập, access token; thiếu verification/refresh/revoke và kiểm tra status |
| User profile | Có hồ sơ cơ bản, avatar, đổi mật khẩu |
| Health/BMI | Có lịch sử BMI; thiếu TDEE, goal, activity và region |
| Allergy | Có catalog và lựa chọn; chưa liên kết ingredient chuẩn |
| Blog/category | Phần lõi đã triển khai |
| Comment/vote | Hoàn thành cho blog; chưa bật cho video |
| Moderation | Chỉ có interface; implementation hiện auto-approve |
| Recipe/ingredient | Chưa triển khai |
| Video | Chưa triển khai |
| AI/chatbot/meal plan | Chưa triển khai |
| Restaurant/location | Chưa triển khai |

Chi tiết đánh giá, kiến trúc đích, 11 phase và ranh giới MVP nằm trong roadmap.

## Tài liệu

- [Roadmap triển khai — tiếng Việt](IMPLEMENTATION_PHASES_VI.md)
- [Implementation roadmap — English](IMPLEMENTATION_PHASES.md)
- [Identity service guide](docs/identity-service-guide.md)
- [Nutrition service guide](docs/nutrition-service-guide.md)
- [Blog service guide](docs/blog-service-guide.md)
- [API Gateway guide](docs/api-gateway-guide.md)

## Lưu ý production

Phiên bản hiện tại phù hợp cho local development/demo, chưa nên triển khai production nếu chưa hoàn thành tối thiểu:

- Bắt buộc hóa secret và credential qua môi trường/secret manager.
- Chặn đăng nhập cho tài khoản chưa xác minh hoặc bị khóa.
- Bổ sung refresh/revocation và chiến lược key rotation.
- Thay `ddl-auto=update` bằng migration có version.
- Container hóa các service và cấu hình service discovery/DNS phù hợp.
- Xem xét chế độ public/private (signed URL) cho ảnh trên Cloudinary.
- Bổ sung observability, backup, rate limit và integration/end-to-end tests.

## License

Repository hiện chưa khai báo license. Không nên giả định quyền phân phối hoặc sử dụng thương mại cho đến khi dự án bổ sung tệp `LICENSE`.
