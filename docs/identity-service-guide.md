# Identity Service - Tài liệu kiến trúc và giải thích mã nguồn

## 1. Mục đích tài liệu

Tài liệu này giải thích `identity-service` của VeggiePal dựa trên mã nguồn hiện tại, bao gồm:

- Service chịu trách nhiệm về domain nào.
- Request đi từ client qua API Gateway vào service như thế nào.
- Chức năng của từng package, class, annotation và khối code quan trọng.
- Luồng đăng ký, đăng nhập, xác thực JWT, xem/sửa profile, đổi mật khẩu và upload avatar.
- Cấu trúc dữ liệu, DTO, mapper, repository và cách xử lý lỗi.
- Cách cấu hình, chạy, kiểm thử và debug service.
- Những chức năng chưa có và các giới hạn của thiết kế hiện tại.

Phạm vi của tài liệu là thư mục `identity-service/`. Những service khác chỉ được nhắc đến khi chúng tương tác với identity service.

---

## 2. Identity service chịu trách nhiệm gì?

Trong kiến trúc hiện tại, `identity-service` sở hữu cả **account**, **authentication** và **basic profile**:

| Nhóm | Trách nhiệm hiện tại |
|---|---|
| Authentication | Đăng ký, đăng nhập, tạo JWT |
| Account | Email, mật khẩu đã hash, role, trạng thái tài khoản, trạng thái xác minh email |
| Profile | Họ tên, số điện thoại, ngày sinh, avatar |
| Public identity | Trả tên và avatar theo danh sách user ID |
| Security | Kiểm tra JWT, ánh xạ role thành authority, trả lỗi 401/403 |
| File storage | Upload/xóa avatar trên Cloudinary |

Tên `identity-service` vẫn phù hợp vì identity không chỉ là login/logout. Nó còn quản lý danh tính và tài khoản cơ bản của người dùng.

Service hiện **chưa có**:

- API logout.
- Refresh token.
- Thu hồi hoặc blacklist access token.
- Xác minh email thực tế.
- Quên/đặt lại mật khẩu.
- MFA/2FA.
- API quản trị role hoặc trạng thái tài khoản.

---

## 3. Bức tranh tổng thể

```text
Client / Frontend
       |
       | HTTP :8080
       v
API Gateway
  /api/auth/**  ---------+
  /api/users/** ---------+----> identity-service :8081
                                      |
                         +------------+-------------+
                         |                          |
                         v                          v
                  MySQL :3307                 Cloudinary
             veggiepal_identity        avatars/{userId}/...
```

Gateway dùng `StripPrefix=1`, vì vậy:

```text
Client gọi:  POST /api/auth/login
Service nhận: POST /auth/login

Client gọi:  GET /api/users/me
Service nhận: GET /users/me
```

Identity service không giữ HTTP session. Mỗi request bảo vệ phải gửi:

```http
Authorization: Bearer <access-token>
```

---

## 4. Công nghệ và dependency

File: `identity-service/pom.xml`

### Nền tảng

| Thành phần | Phiên bản/vai trò |
|---|---|
| Java | Java 21 |
| Spring Boot | 4.1.1 |
| Maven | Build và quản lý dependency |
| Spring MVC | REST controller |
| Spring Data JPA | Truy cập MySQL qua Hibernate |
| Spring Security | Authorization và security filter chain |
| OAuth2 Resource Server | Decode và xác thực bearer JWT |
| JJWT | Phát hành JWT lúc login |
| Jakarta Validation | Validate request DTO |
| MapStruct | Sinh code chuyển đổi DTO/entity |
| Lombok | Sinh getter, setter, builder, constructor |
| Cloudinary SDK (`cloudinary-http5`) | Upload/xóa ảnh avatar |
| Springdoc OpenAPI | Sinh OpenAPI và Swagger UI |

### Vì sao vừa có JJWT vừa có OAuth2 Resource Server?

- `JJWT` được `JwtService` dùng để **tạo token**.
- `NimbusJwtDecoder` của Spring Security được dùng để **kiểm tra token** ở request đi vào.

Hai bên phải dùng cùng secret và cùng thuật toán `HS256`.

### Annotation processor

Trong `maven-compiler-plugin`, Lombok được chạy trước MapStruct. Điều này cần thiết vì MapStruct phải nhìn thấy các getter/setter do Lombok sinh ra khi tạo implementation của `UserMapper`.

---

## 5. Cấu trúc thư mục

```text
identity-service/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/veggiepal/
    │   │   ├── VeggiepalApplication.java
    │   │   ├── configuration/
    │   │   ├── controller/
    │   │   ├── dto/
    │   │   │   ├── request/
    │   │   │   └── response/
    │   │   ├── entity/
    │   │   ├── enums/
    │   │   ├── exception/
    │   │   ├── mapper/
    │   │   ├── repository/
    │   │   └── service/
    │   └── resources/application.properties
    └── test/java/com/veggiepal/
```

Luồng phụ thuộc chính:

```text
Controller -> Service -> Repository -> MySQL
                  |
                  +-> Mapper
                  +-> JwtService
                  +-> FileStorageService -> Cloudinary
```

Controller không truy cập repository trực tiếp. Business rule nằm trong service.

---

## 6. Điểm khởi động ứng dụng

File: `VeggiepalApplication.java`

```java
@SpringBootApplication
public class VeggiepalApplication {
    public static void main(String[] args) {
        SpringApplication.run(VeggiepalApplication.class, args);
    }
}
```

Giải thích:

- `@SpringBootApplication` kết hợp configuration, auto-configuration và component scanning.
- Vì class nằm tại package `com.veggiepal`, Spring quét các package con như `controller`, `service`, `repository` và `configuration`.
- `SpringApplication.run(...)` tạo application context và khởi động web server ở port `8081`.

---

## 7. Cấu hình chạy ứng dụng

File: `src/main/resources/application.properties`

### Tên và port

```properties
spring.application.name=identity-service
server.port=8081
```

Service chạy trực tiếp tại `http://localhost:8081`.

### MySQL

```properties
spring.datasource.url=jdbc:mysql://localhost:3307/veggiepal_identity
spring.datasource.username=root
spring.datasource.password=12345
```

Service kết nối database `veggiepal_identity` qua port host `3307`.

```properties
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.open-in-view=false
```

- `ddl-auto=update`: Hibernate tự cập nhật schema dựa trên entity. Tiện cho phát triển, nhưng production nên dùng migration như Flyway/Liquibase.
- `show-sql=true`: in SQL ra log.
- `format_sql=true`: format SQL để dễ đọc.
- `open-in-view=false`: đóng persistence context trước tầng view/controller; tránh query ngầm ngoài service.

### JWT

```properties
jwt.secret=${JWT_SECRET:veggiepal-secret-key-must-be-at-least-32-characters}
```

- Nếu có biến môi trường `JWT_SECRET`, service dùng giá trị đó.
- Nếu không có, service dùng secret mặc định phục vụ local development.
- Các microservice xác thực token phải dùng cùng secret.
- Không nên dùng default secret trong production.

### Cloudinary

```properties
spring.config.import=optional:file:.env[.properties],optional:file:../.env[.properties]
storage.cloudinary.cloud-name=${CLOUDINARY_CLOUD_NAME:}
storage.cloudinary.api-key=${CLOUDINARY_API_KEY:}
storage.cloudinary.api-secret=${CLOUDINARY_API_SECRET:}
```

Credential lấy từ file `.env` ở gốc repository (đã gitignore, template là `.env.example`). `../.env` dùng khi chạy Maven trong thư mục service; `.env` dùng khi IDE chạy ở gốc repo. Biến môi trường của OS (ví dụ `env_file` trong docker-compose) được ưu tiên hơn file. Giá trị mặc định rỗng: service vẫn khởi động khi thiếu `.env`, chỉ upload ảnh trả 503.

### Giới hạn multipart

```properties
spring.servlet.multipart.max-file-size=2MB
spring.servlet.multipart.max-request-size=3MB
```

- Một file không được quá 2 MB ở tầng web server.
- Cả multipart request không được quá 3 MB.
- `ProfileService` còn kiểm tra lại kích thước bằng byte để business rule không chỉ phụ thuộc cấu hình framework.

---

## 8. Entity và database

### `User`

File: `entity/User.java`

```java
@Entity
@Table(name = "users")
public class User { ... }
```

- `@Entity`: class được JPA quản lý.
- `@Table(name = "users")`: ánh xạ tới bảng `users`.

### Các cột

| Field Java | Cột DB | Ý nghĩa/ràng buộc |
|---|---|---|
| `id` | `id` | Primary key, DB tự tăng |
| `email` | `email` | Bắt buộc, unique |
| `passwordHash` | `password_hash` | BCrypt hash, không lưu mật khẩu gốc |
| `fullName` | `full_name` | Bắt buộc |
| `phone` | `phone` | Có thể null |
| `avatarUrl` | `avatar_url` | URL public của avatar, có thể null |
| `dateOfBirth` | `date_of_birth` | Ngày sinh, có thể null |
| `role` | `role` | `USER` hoặc `ADMIN` dạng text |
| `status` | `status` | `PENDING`, `ACTIVE`, `INACTIVE`, `BLOCKED` |
| `emailVerified` | `email_verified` | Cờ đã xác minh email |
| `createdAt` | `created_at` | Thời điểm tạo, không update |
| `updatedAt` | `updated_at` | Thời điểm cập nhật cuối |

```java
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
Long id;
```

Database sinh ID khi insert.

```java
@Enumerated(EnumType.STRING)
Role role;
```

Enum được lưu bằng tên như `USER`, thay vì ordinal `0`, `1`. Cách này an toàn hơn khi thay đổi thứ tự enum.

```java
@PrePersist
void prePersist() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
}
```

Chạy ngay trước insert để gán timestamp.

```java
@PreUpdate
void preUpdate() {
    updatedAt = LocalDateTime.now();
}
```

Chạy trước update để cập nhật `updatedAt`.

### Lombok trên entity

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
```

- `@Data`: sinh getter, setter, `equals`, `hashCode`, `toString`.
- `@Builder`: cho phép `User.builder()...build()`.
- Hai annotation constructor phục vụ JPA, builder và test.
- `@FieldDefaults`: field không ghi modifier sẽ là `private`.

---

## 9. Enum

### `Role`

```java
public enum Role {
    USER,
    ADMIN
}
```

Role được đưa vào JWT và chuyển thành authority `ROLE_USER` hoặc `ROLE_ADMIN`.

### `UserStatus`

```java
PENDING, ACTIVE, INACTIVE, BLOCKED
```

Ý nghĩa dự kiến:

- `PENDING`: vừa đăng ký/chưa hoàn tất kích hoạt.
- `ACTIVE`: hoạt động bình thường.
- `INACTIVE`: ngừng hoạt động.
- `BLOCKED`: bị khóa.

Lưu ý: code login hiện tại chưa kiểm tra status, nên mọi status vẫn có thể login nếu email và mật khẩu đúng. Riêng API public batch chỉ trả user `ACTIVE`.

### `ImageType`

Mỗi loại ảnh giữ MIME type và extension:

| Enum | MIME type | Extension |
|---|---|---|
| `JPEG` | `image/jpeg` | `jpg` |
| `PNG` | `image/png` | `png` |
| `WEBP` | `image/webp` | `webp` |

---

## 10. Repository

File: `repository/UserRepository.java`

```java
public interface UserRepository extends JpaRepository<User, Long>
```

`JpaRepository` cung cấp sẵn `findById`, `save`, `delete`, paging và nhiều thao tác CRUD.

Các method khai báo thêm:

```java
boolean existsByEmail(String email);
```

Spring Data tự sinh query kiểm tra email tồn tại, dùng khi đăng ký.

```java
Optional<User> findByEmail(String email);
```

Tìm account khi login. `Optional` buộc code xử lý trường hợp không tìm thấy.

```java
List<User> findByIdInAndStatus(Collection<Long> ids, UserStatus status);
```

Sinh query có ý nghĩa tương đương:

```sql
SELECT * FROM users
WHERE id IN (...)
  AND status = 'ACTIVE';
```

Method này phục vụ API public batch.

---

## 11. DTO và validation

DTO tách dữ liệu API khỏi entity database. Client không được gửi trực tiếp `User`, nhờ đó không thể tự gán `role`, `status` hay `passwordHash`.

### Request DTO

#### `RegisterRequest`

| Field | Validation |
|---|---|
| `email` | Phải có và đúng định dạng email |
| `password` | Tối thiểu 6 ký tự |
| `fullName` | Không được rỗng |
| `phone` | Không có validation |

Điểm cần chú ý: `password` chỉ có `@Size`, mà Jakarta Validation xem `null` là hợp lệ đối với `@Size`. Vì vậy request không có password có thể lọt qua validation rồi lỗi khi BCrypt encode. Nên có thêm `@NotBlank(message = "PASSWORD_REQUIRED")` nếu hoàn thiện sau này.

#### `LoginRequest`

- Email có cả `@NotBlank` và `@Email`.
- Password có `@NotBlank`.

#### `UpdateProfileRequest`

Đây là PATCH DTO:

- Field `null`: giữ giá trị cũ.
- `fullName` nếu xuất hiện phải có ít nhất một ký tự không phải whitespace.
- `phone = ""` hoặc chỉ có khoảng trắng: xóa số điện thoại.
- `dateOfBirth` phải nằm trong quá khứ.

#### `ChangePasswordRequest`

- `currentPassword`: bắt buộc.
- `newPassword`: bắt buộc và tối thiểu 6 ký tự.

### Response DTO

#### `ApiResponse<T>`

Mọi response business dùng format chung:

```json
{
  "code": 1000,
  "result": {}
}
```

Khi lỗi:

```json
{
  "code": 1008,
  "message": "Unauthenticated"
}
```

`@JsonInclude(NON_NULL)` loại các field null khỏi JSON. `code` mặc định là `1000` nhờ `@Builder.Default`.

#### Các response cụ thể

- `RegisterResponse`: thông tin account sau đăng ký, không chứa password hash.
- `LoginResponse`: access token cùng user ID, email, tên và role.
- `UserProfileResponse`: thông tin profile đầy đủ của chính user.
- `PublicUserResponse`: chỉ `id`, `fullName`, `avatarUrl`; không lộ email hay phone.

---

## 12. MapStruct mapper

File: `mapper/UserMapper.java`

```java
@Mapper(componentModel = "spring")
public interface UserMapper
```

MapStruct sinh implementation lúc compile và đăng ký nó thành Spring bean.

### Register request sang entity

```java
@Mapping(target = "passwordHash", ignore = true)
@Mapping(target = "role", ignore = true)
...
User toUser(RegisterRequest request);
```

Các field nhạy cảm và field do server quản lý bị ignore. Sau mapping, `UserService` tự set password hash, role, status và email verified.

### Entity sang response

```java
RegisterResponse toUserResponse(User user);
UserProfileResponse toUserProfileResponse(User user);
PublicUserResponse toPublicUserResponse(User user);
```

MapStruct tự map các field cùng tên.

### PATCH profile

```java
@BeanMapping(
    ignoreByDefault = true,
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
```

- `ignoreByDefault=true`: chỉ update field được liệt kê rõ.
- `IGNORE` đối với null: request không gửi field nào thì giữ dữ liệu cũ.
- `@MappingTarget User user`: sửa entity hiện có thay vì tạo entity mới.

Chỉ ba field được phép update: `fullName`, `phone`, `dateOfBirth`.

---

## 13. Controller và API

### Danh sách endpoint

| Method | URL qua gateway | Auth | Chức năng |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Đăng ký |
| POST | `/api/auth/login` | Public | Đăng nhập và lấy JWT |
| GET | `/api/users/me` | Bearer JWT | Xem profile hiện tại |
| PATCH | `/api/users/me` | Bearer JWT | Sửa profile |
| PUT | `/api/users/me/password` | Bearer JWT | Đổi mật khẩu |
| POST | `/api/users/me/avatar` | Bearer JWT | Upload avatar |
| GET | `/api/users/batch?ids=1,2` | Public | Lấy tên/avatar công khai theo ID |

### Annotation controller chung

```java
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
```

- `@RestController`: return value được serialize thành JSON.
- `@RequestMapping`: prefix URL của controller.
- `@RequiredArgsConstructor`: Lombok sinh constructor injection cho dependency `final`.
- `@FieldDefaults(...makeFinal=true)`: dependency trở thành private final.

### `UserController`

Controller chỉ nhận HTTP request, kích hoạt validation, gọi `UserService` và bọc kết quả trong `ApiResponse`.

```java
ApiResponse<RegisterResponse> createUser(
    @RequestBody @Valid RegisterRequest request
)
```

- `@RequestBody`: deserialize JSON.
- `@Valid`: chạy validation annotation trong DTO trước khi vào service.

### `ProfileController`

Base path là `/users/me`. Controller không nhận user ID từ body/path mà lấy từ JWT:

```java
@AuthenticationPrincipal Jwt jwt
CurrentUser.id(jwt)
```

Điều này ngăn client giả mạo ID để sửa profile của người khác.

Upload avatar dùng:

```java
@PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
@RequestPart("file") MultipartFile file
```

Client phải gửi multipart part tên `file`.

### `PublicUserController`

```http
GET /api/users/batch?ids=1,2,3
```

Spring chuyển chuỗi `1,2,3` thành `List<Long>`. Endpoint public để frontend resolve tên/avatar tác giả cho blog mà không cần blog service gọi đồng bộ sang identity service.

---

## 14. Lấy user ID từ JWT

File: `controller/CurrentUser.java`

```java
Object userId = jwt.getClaim("userId");

if (userId instanceof Number number) {
    return number.longValue();
}
```

JWT claim có thể được decoder biểu diễn bằng một kiểu `Number` khác `Long`, nên code nhận mọi `Number` rồi chuyển sang `long`.

Nếu claim bị thiếu hoặc sai kiểu, code ném `UNAUTHENTICATED`.

Đây là utility class nên constructor private để không ai tạo instance.

---

## 15. `UserService`: đăng ký và đăng nhập

### Luồng đăng ký

```text
POST /auth/register
  -> Validate RegisterRequest
  -> Chuẩn hóa email
  -> Kiểm tra email tồn tại
  -> Map request thành User
  -> BCrypt password
  -> Set role/status/emailVerified
  -> INSERT users
  -> Map thành RegisterResponse
```

#### Chuẩn hóa email

```java
String email = request.getEmail().trim().toLowerCase();
```

Nhờ đó `User@Example.com` và ` user@example.com ` được xem là cùng email ở tầng application.

#### Kiểm tra trùng

```java
if (userRepository.existsByEmail(email)) {
    throw new AppException(ErrorCode.EMAIL_EXISTED);
}
```

Database vẫn có unique constraint để bảo vệ khi hai request đồng thời cùng đăng ký một email. Tuy nhiên race condition từ unique constraint hiện có thể rơi vào lỗi 9999 vì chưa có handler riêng cho constraint violation.

#### Hash password

```java
user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
```

Service không lưu password plaintext. `PasswordEncoder` là BCrypt strength 10.

#### Giá trị mặc định

```java
user.setRole(Role.USER);
user.setStatus(UserStatus.PENDING);
user.setEmailVerified(false);
```

Client không thể tự đăng ký làm admin.

### Luồng đăng nhập

```text
POST /auth/login
  -> Validate LoginRequest
  -> Chuẩn hóa email
  -> SELECT user by email
  -> BCrypt matches(rawPassword, passwordHash)
  -> Tạo JWT HS256, hạn 24 giờ
  -> Trả token và thông tin user
```

Nếu email không tồn tại và nếu password sai, cả hai đều trả `UNAUTHENTICATED`. Đây là lựa chọn tốt để tránh tiết lộ email nào đã đăng ký.

Hiện tại login chưa kiểm tra:

- `status == ACTIVE`.
- `emailVerified == true`.

Do đó `PENDING`, `INACTIVE` hoặc `BLOCKED` vẫn có thể nhận token nếu mật khẩu đúng.

---

## 16. JWT: phát hành và xác thực

### Phát hành token

File: `service/JwtService.java`

```java
private static final long EXPIRATION = 1000 * 60 * 60 * 24;
```

Token sống 24 giờ, tính bằng millisecond.

Payload được tạo như sau:

```java
Jwts.builder()
    .subject(user.getEmail())
    .claim("userId", user.getId())
    .claim("role", user.getRole().name())
    .issuedAt(new Date())
    .expiration(...)
    .signWith(signingKey, Jwts.SIG.HS256)
    .compact();
```

JWT có các claim chính:

```json
{
  "sub": "user@example.com",
  "userId": 7,
  "role": "USER",
  "iat": 1790750000,
  "exp": 1790836400
}
```

JWT được ký chứ không được mã hóa. Không nên đưa password hoặc dữ liệu riêng tư vào payload.

### Tạo signing key

File: `configuration/JwtConfig.java`

```java
new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256")
```

Secret string được biến thành HMAC-SHA256 key.

### Xác thực token

```java
NimbusJwtDecoder
    .withSecretKey(signingKey(secret))
    .macAlgorithm(MacAlgorithm.HS256)
    .build();
```

Decoder kiểm tra chữ ký, format và thời gian hết hạn trước khi controller được gọi.

### Logout hiện tại

Service stateless và chỉ phát access token. Vì không có token store/blacklist/refresh token nên chưa thể vô hiệu hóa access token trước khi hết hạn. “Logout” phía client hiện chỉ có thể là xóa token local; token cũ vẫn hợp lệ tới `exp` nếu bị lấy cắp.

---

## 17. Spring Security

File: `configuration/SecurityConfig.java`

### Public endpoint

```java
static final String[] PUBLIC_ENDPOINTS = {
    "/auth/register",
    "/auth/login",
    "/auth/test",
    "/users/batch",
    "/swagger-ui/**",
    "/swagger-ui.html",
    "/v3/api-docs/**"
};
```

Mọi endpoint còn lại bắt buộc authenticated.

`/auth/test` đang được permit nhưng hiện không có controller tương ứng.

### Security filter chain

```java
.requestMatchers(PUBLIC_ENDPOINTS).permitAll()
.anyRequest().authenticated()
```

Rule được đọc theo thứ tự: public trước, tất cả endpoint khác cần JWT.

```java
.sessionManagement(session ->
    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
)
```

Server không tạo session; mỗi request phải tự mang bearer token.

```java
httpSecurity.csrf(AbstractHttpConfigurer::disable);
```

CSRF bị tắt vì API stateless không dùng cookie session để xác thực.

### Role mapping

```java
authoritiesConverter.setAuthoritiesClaimName("role");
authoritiesConverter.setAuthorityPrefix("ROLE_");
```

Claim `role: USER` trở thành Spring authority `ROLE_USER`.

Hiện controller chưa có `@PreAuthorize`, nhưng converter đã sẵn sàng cho kiểm tra role.

### Vì sao public endpoint bỏ qua Authorization header?

Custom `BearerTokenResolver` trả `null` đối với URL public:

```java
return request -> publicMatchers.stream().anyMatch(...)
        ? null
        : defaultResolver.resolve(request);
```

Nếu frontend vô tình gửi một token cũ/hỏng khi gọi login, Spring Security sẽ bỏ qua header đó và vẫn cho đăng nhập. Không có logic này, malformed/expired bearer token có thể làm public login trả 401 trước khi controller chạy.

### Password encoder

```java
return new BCryptPasswordEncoder(10);
```

`10` là work factor của BCrypt. BCrypt tự tạo salt; cùng một password có thể tạo hash khác nhau.

---

## 18. Lỗi security 401 và 403

File: `configuration/SecurityExceptionHandler.java`

Class implement hai interface:

- `AuthenticationEntryPoint`: request chưa xác thực/token sai -> 401.
- `AccessDeniedHandler`: đã xác thực nhưng thiếu quyền -> 403.

Nó tự serialize `ApiResponse` vì lỗi xảy ra trong security filter chain, trước controller, nên `GlobalExceptionHandler` không xử lý được.

| Trường hợp | HTTP | Code |
|---|---:|---:|
| Thiếu/sai/hết hạn JWT | 401 | 1008 |
| Đã login nhưng không đủ quyền | 403 | 1009 |

---

## 19. `ProfileService`

### Xem profile

```java
return userMapper.toUserProfileResponse(findUser(userId));
```

Tìm user theo ID lấy từ JWT rồi map sang response. Không trả password hash.

### Update profile

```text
PATCH /users/me
  -> userId lấy từ JWT
  -> Tìm User
  -> MapStruct update field khác null
  -> Trim fullName
  -> Trim phone hoặc đổi blank thành null
  -> Save
  -> Trả profile mới
```

```java
StringUtils.hasText(user.getPhone())
        ? user.getPhone().trim()
        : null
```

Phone trống trở thành `null`, nên client có thể xóa phone bằng chuỗi rỗng.

### Đổi mật khẩu

```text
PUT /users/me/password
  -> Tìm user từ JWT
  -> Kiểm tra currentPassword với BCrypt
  -> Không cho newPassword giống password cũ
  -> BCrypt password mới
  -> Save
```

Không bao giờ so sánh raw password trực tiếp với hash bằng `equals`; phải dùng `passwordEncoder.matches(...)`.

Sau khi đổi password, JWT cũ vẫn hợp lệ vì service chưa có token version hoặc blacklist.

Về domain, đổi mật khẩu thuộc account/security hơn là profile, dù hiện method nằm trong `ProfileService`.

### Upload avatar

```text
POST multipart /users/me/avatar
  -> Kiểm tra file tồn tại
  -> Kiểm tra <= 2 MB
  -> Đọc bytes
  -> Kiểm tra magic bytes
  -> Đối chiếu magic bytes với Content-Type
  -> Tìm user
  -> Tạo object key duy nhất
  -> Upload file mới
  -> Lưu URL mới vào MySQL
  -> Xóa avatar cũ
  -> Trả profile mới
```

Object key:

```text
avatars/{userId}/{random-uuid}.{extension}
```

UUID tránh ghi đè file và tránh collision.

#### Bù trừ khi lưu database thất bại

```java
try {
    userRepository.save(user);
} catch (RuntimeException exception) {
    deleteQuietly(avatarUrl);
    throw exception;
}
```

File storage và MySQL không nằm trong cùng transaction. Nếu upload thành công nhưng save DB thất bại, code xóa file vừa upload để tránh file rác.

Sau khi save thành công, avatar cũ được xóa. Nếu xóa avatar cũ thất bại, request vẫn thành công và chỉ ghi warning; URL mới trong DB vẫn hợp lệ.

---

## 20. Kiểm tra loại ảnh bằng magic bytes

File: `service/ImageTypeDetector.java`

Không thể tin hoàn toàn `Content-Type` do client gửi. Một file thực thi có thể được gắn nhãn `image/png`. Code kiểm tra signature thật ở đầu nội dung:

- JPEG bắt đầu bằng `FF D8 FF`.
- PNG bắt đầu bằng `89 50 4E 47 0D 0A 1A 0A`.
- WEBP có `RIFF` tại offset 0 và `WEBP` tại offset 8.

Sau khi detect bằng bytes, `ProfileService` còn yêu cầu loại detect được trùng với `file.getContentType()`.

`hasSignature` kiểm tra độ dài trước khi so sánh để tránh đọc vượt mảng.

---

## 21. File storage và Cloudinary

### Interface `FileStorageService`

```java
String upload(String key, byte[] content, String contentType);
void delete(String url);
```

Business service phụ thuộc interface, không phụ thuộc trực tiếp Cloudinary SDK. Nhờ đó có thể thay implementation hoặc mock trong unit test.

### `StorageProperties`

```java
@ConfigurationProperties(prefix = "storage.cloudinary")
public record StorageProperties(String cloudName, String apiKey, String apiSecret)
```

### `CloudinaryConfig`

Tạo singleton `Cloudinary` từ `cloud_name`, `api_key`, `api_secret` với `secure=true` (URL `https`). Việc khởi tạo không gọi mạng, nên credential rỗng vẫn tạo được bean.

### `CloudinaryFileStorageService.upload`

Key `avatars/{userId}/{uuid}.{ext}` được bỏ đuôi để làm `public_id` (`avatars/7/abc`), vì Cloudinary tự nhận dạng định dạng từ byte và tự thêm đuôi vào URL. Ảnh upload với `resource_type=image`, `overwrite=false`; method trả `secure_url`:

```text
https://res.cloudinary.com/{cloudName}/image/upload/v{version}/avatars/7/abc.png
```

Tham số `contentType` không được dùng; magic bytes đã được `ImageTypeDetector` kiểm tra trước đó.

### `delete`

Service chỉ xóa URL bắt đầu bằng `https://res.cloudinary.com/{cloudName}/image/upload/`. URL null, URL MinIO cũ và URL ngoài hệ thống bị bỏ qua. `public_id` được lấy từ phần sau prefix sau khi bỏ đoạn `v{version}/` và đuôi file, rồi gọi `destroy`. Kết quả `not found` không bị coi là lỗi.

Ở cả hai method, `IOException` (mạng) và `RuntimeException` (SDK báo lỗi API, ví dụ sai credential) được log và chuyển thành `FILE_UPLOAD_FAILED`/HTTP 503.

---

## 22. Public user batch

`PublicUserService` hỗ trợ frontend hiển thị tác giả của blog:

```text
blog-service lưu author_id
frontend lấy trang blog
frontend gom các author_id duy nhất
frontend gọi /api/users/batch?ids=...
frontend ghép fullName/avatar vào bài viết
```

Service không trả user `PENDING`, `INACTIVE` hoặc `BLOCKED`:

```java
findByIdInAndStatus(ids, UserStatus.ACTIVE)
```

Danh sách rỗng hoặc quá 50 ID trả `INVALID_REQUEST`. Giới hạn 50 ngăn một request public quét toàn bộ bảng user.

ID không tồn tại được bỏ qua, không làm cả batch thất bại.

---

## 23. Xử lý exception

### `AppException`

Đây là runtime exception mang theo một `ErrorCode` có cấu trúc. Business service ném:

```java
throw new AppException(ErrorCode.EMAIL_EXISTED);
```

thay vì tự tạo HTTP response.

### `ErrorCode`

Mỗi error định nghĩa:

- Application code.
- Message.
- HTTP status.

Ví dụ:

```java
UNAUTHENTICATED(1008, "Unauthenticated", HttpStatus.UNAUTHORIZED)
```

### Bảng lỗi

| Code | HTTP | Ý nghĩa |
|---:|---:|---|
| 1001 | 400 | Validation key không hợp lệ |
| 1002 | 400 | Email đã tồn tại |
| 1003 | 400 | Password quá ngắn |
| 1004 | 400 | Email sai định dạng |
| 1005 | 400 | Thiếu email |
| 1006 | 400 | Thiếu họ tên |
| 1007 | 404 | Không tìm thấy user |
| 1008 | 401 | Chưa xác thực/token không hợp lệ |
| 1009 | 403 | Không có quyền |
| 1010 | 400 | Thiếu password |
| 1011 | 400 | Mật khẩu hiện tại sai |
| 1012 | 400 | Mật khẩu mới giống mật khẩu cũ |
| 1013 | 400 | Ngày sinh không nằm trong quá khứ |
| 1014 | 400 | Thiếu file avatar |
| 1015 | 400 | Loại avatar không hợp lệ |
| 1016 | 400 | Avatar quá 2 MB |
| 1017 | 503 | Object storage thất bại |
| 1018 | 400 | Request không hợp lệ |
| 9999 | 500 | Lỗi chưa phân loại |

### `GlobalExceptionHandler`

| Exception | Mapping |
|---|---|
| `AppException` | Dùng `ErrorCode` đi kèm |
| `MethodArgumentNotValidException` | Lấy validation message làm enum key |
| JSON sai format | `INVALID_REQUEST` |
| Query param thiếu/sai kiểu | `INVALID_REQUEST` |
| Multipart quá lớn | `AVATAR_TOO_LARGE` |
| Thiếu multipart part | `AVATAR_REQUIRED` |
| Exception khác | `UNCATEGORIZED_EXCEPTION` |

Đối với message `Password must be at least {min} characters`, handler đọc attribute `min` từ validation constraint và thay `{min}` bằng `6`.

Catch-all exception được log server-side nhưng response không lộ stack trace.

---

## 24. Luồng request hoàn chỉnh

### Ví dụ: xem profile

```text
1. Client gửi GET /api/users/me + Bearer token.
2. Gateway strip `/api`, forward GET /users/me tới port 8081.
3. BearerTokenResolver lấy token từ Authorization header.
4. JwtDecoder kiểm tra chữ ký HS256 và expiration.
5. Spring tạo Authentication chứa Jwt principal.
6. ProfileController nhận Jwt qua @AuthenticationPrincipal.
7. CurrentUser đọc claim userId.
8. ProfileService gọi UserRepository.findById(userId).
9. Hibernate query bảng users.
10. UserMapper map User thành UserProfileResponse.
11. Controller bọc trong ApiResponse.
12. Jackson serialize JSON và gateway trả về client.
```

### Nếu token sai

Request dừng ở bước 4. Controller không chạy. `SecurityExceptionHandler` trả HTTP 401/code 1008.

### Nếu user ID trong token không còn tồn tại

Token vẫn có chữ ký hợp lệ, nhưng `findUser` không tìm thấy record. `GlobalExceptionHandler` trả HTTP 404/code 1007.

---

## 25. Ví dụ request/response

### Đăng ký

```http
POST /api/auth/register
Content-Type: application/json

{
  "email": "User@Example.com",
  "password": "secret12",
  "fullName": "Nguyen Van A",
  "phone": "0900000000"
}
```

Kết quả email được lưu dạng `user@example.com`, password được lưu dưới dạng BCrypt hash, role là `USER`, status là `PENDING`.

### Đăng nhập

```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "secret12"
}
```

```json
{
  "code": 1000,
  "result": {
    "accessToken": "eyJ...",
    "userId": 1,
    "email": "user@example.com",
    "fullName": "Nguyen Van A",
    "role": "USER"
  }
}
```

### Sửa profile

```http
PATCH /api/users/me
Authorization: Bearer eyJ...
Content-Type: application/json

{
  "fullName": "Nguyen Van B",
  "phone": "",
  "dateOfBirth": "2000-01-20"
}
```

Phone rỗng sẽ được lưu thành `null`.

### Đổi mật khẩu

```http
PUT /api/users/me/password
Authorization: Bearer eyJ...
Content-Type: application/json

{
  "currentPassword": "secret12",
  "newPassword": "newSecret34"
}
```

### Upload avatar

```bash
curl -X POST http://localhost:8080/api/users/me/avatar \
  -H "Authorization: Bearer <token>" \
  -F "file=@avatar.png;type=image/png"
```

### Public batch

```http
GET /api/users/batch?ids=1,2,3
```

```json
{
  "code": 1000,
  "result": [
    {
      "id": 1,
      "fullName": "Nguyen Van A",
      "avatarUrl": "https://res.cloudinary.com/<cloud>/image/upload/v1712345678/avatars/1/...png"
    }
  ]
}
```

---

## 26. Swagger/OpenAPI

File: `configuration/OpenApiConfig.java`

OpenAPI khai báo:

- Tên API: VeggiePal Identity Service API.
- Version `1.0`.
- Server base URL `/api`, tương ứng gọi qua gateway.
- HTTP bearer scheme định dạng JWT.

Truy cập qua gateway:

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON của identity service được gateway expose tại:

```text
/identity-service/v3/api-docs
```

Nút **Authorize** thêm header bearer khi thử protected endpoint.

---

## 27. Test hiện có

### Nhóm test security/controller

- `SecurityConfigTest`: protected endpoint thiếu/sai token trả 401; stale token không chặn login.
- `SecurityExceptionHandlerTest`: format response 401 và 403.
- `ProfileControllerTest`: lấy user ID từ JWT, validation update/password/avatar.
- `PublicUserControllerTest`: batch public, bỏ qua token rác, validate query param và giới hạn.
- `GlobalExceptionHandlerTest`: malformed JSON thành invalid request.
- `GlobalExceptionHandlerUnitTest`: multipart quá lớn thành avatar too large.

### Nhóm unit test service

- `ProfileServiceTest`: get/update profile, đổi password, validation và compensation khi upload avatar.
- `PublicUserServiceTest`: field public, user không tồn tại, giới hạn 50 ID.
- `JwtServiceTest`: token phát ra decode được bằng decoder thật.
- `ImageTypeDetectorTest`: nhận JPEG/PNG/WEBP, từ chối dữ liệu khác/null/quá ngắn.
- `CloudinaryFileStorageServiceTest`: public_id bỏ đuôi, trả `secure_url`, parse URL khi delete (có/không có version), bỏ qua URL ngoài, lỗi SDK.

### Context test

`VeggiepalApplicationTests` dùng `@SpringBootTest` để kiểm tra application context. Test này phụ thuộc MySQL cấu hình hiện tại, không dùng H2/test profile.

### Chạy test

Từ thư mục project:

```powershell
cd identity-service
./mvnw.cmd test -Dtest='!VeggiepalApplicationTests'
```

Chạy toàn bộ, gồm context test khi MySQL sẵn sàng:

```powershell
./mvnw.cmd test
```

---

## 28. Cách chạy local

### 1. Khởi động MySQL và tạo `.env`

Tại root repository:

```powershell
docker compose up -d mysql
Copy-Item .env.example .env   # rồi điền CLOUDINARY_*
```

### 2. Tạo database identity nếu chưa có

```powershell
docker exec veggiepal-mysql mysql -uroot -p12345 -e "CREATE DATABASE IF NOT EXISTS veggiepal_identity"
```

### 3. Chạy identity service

```powershell
cd identity-service
./mvnw.cmd spring-boot:run
```

### 4. Chạy API Gateway nếu muốn dùng URL `/api/...`

```powershell
cd api-gateway
./mvnw.cmd spring-boot:run
```

Các địa chỉ chính:

| Thành phần | URL |
|---|---|
| Gateway | `http://localhost:8080` |
| Identity trực tiếp | `http://localhost:8081` |
| Swagger tổng | `http://localhost:8080/swagger-ui.html` |

---

## 29. Cách debug theo triệu chứng

### Login trả 401 dù endpoint public

- Kiểm tra URL service là `/auth/login`, qua gateway là `/api/auth/login`.
- Kiểm tra request JSON và password.
- Public resolver đã chủ động bỏ qua bearer token cũ trên endpoint này.

### Protected endpoint trả 401

- Header phải có đúng `Authorization: Bearer <token>`.
- Token có thể hết hạn sau 24 giờ.
- Secret ở service phát token và service kiểm tra token phải giống nhau.

### Upload avatar trả 400

- Part phải tên `file`.
- File phải <= 2 MB.
- Chỉ JPEG, PNG, WEBP.
- MIME type phải khớp magic bytes thật.

### Upload avatar trả 503

- Kiểm tra `.env` ở gốc repo có đủ `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`.
- Đọc log `Could not upload image to Cloudinary`: exception kèm theo chứa lỗi HTTP Cloudinary trả về (ví dụ `Invalid Signature` khi sai secret, hoặc status 401/404 khi thiếu `CLOUDINARY_*`), hoặc lỗi mạng/timeout.

### Context test không chạy

- Test hiện dùng MySQL thật theo `application.properties`.
- Đảm bảo port 3307 hoạt động và database `veggiepal_identity` tồn tại.

---

## 30. Giới hạn và rủi ro hiện tại

Đây là mô tả hành vi của code hiện tại, không phải thay đổi source:

1. `RegisterRequest.password` thiếu `@NotBlank`; password null có thể thành lỗi 500.
2. Login chưa kiểm tra `status` hoặc `emailVerified`.
3. Không có refresh token và token revocation.
4. Đổi password không vô hiệu hóa token đã phát.
5. Không có logout server-side; client chỉ xóa token local.
6. JWT dùng shared symmetric secret; mọi service biết secret đều có khả năng ký token nếu bị cấu hình sai/rò rỉ.
7. Default secret và thông tin MySQL chỉ phù hợp local development.
8. `ddl-auto=update` không phải chiến lược migration production tốt.
9. Check email trước insert vẫn có race condition; unique constraint bảo vệ DB nhưng exception chưa được map đẹp.
10. `User` dùng Lombok `@Data`, nên `toString()` có thể chứa `passwordHash` nếu entity bị log trực tiếp.
11. `/auth/test` được khai báo public nhưng không có endpoint thực tế.
12. Xóa avatar cũ theo best effort; storage có thể còn orphan file khi delete thất bại.

---

## 31. Boundary kiến trúc và hướng phát triển

Ở quy mô hiện tại, chưa cần tách thêm microservice. Có thể tổ chức logic theo hai module khái niệm trong cùng deployment:

```text
identity-service
├── account/security
│   ├── register/login
│   ├── password
│   ├── role/status
│   ├── email verification
│   └── token/session
└── profile
    ├── fullName/phone/dateOfBirth
    ├── avatar
    └── public profile lookup
```

Nếu profile sau này có lifecycle, tải đọc, team vận hành hoặc mô hình dữ liệu độc lập, boundary tách hợp lý là:

```text
identity-service
- userId, email, password hash
- role, status, verification
- login, token, session, MFA

profile-service
- userId
- fullName, phone, dateOfBirth, avatar
- bio, preferences, privacy, public profile
```

Khi đó hai service dùng database riêng và đồng bộ việc tạo profile qua event như `UserRegistered`. Không nên dùng foreign key chéo database service.

---

## 32. Tóm tắt ngắn

`identity-service` hiện là nguồn dữ liệu chính của account và profile cơ bản. Nó nhận đăng ký/đăng nhập, hash mật khẩu bằng BCrypt, phát JWT HS256 có hạn 24 giờ, xác thực bearer token theo mô hình stateless, quản lý profile và lưu avatar trên Cloudinary. Các controller lấy user ID từ JWT thay vì tin dữ liệu client. Response và exception có format thống nhất. Public batch chỉ lộ ID, tên và avatar của user active.

Luồng quan trọng nhất cần nhớ:

```text
HTTP request
 -> Gateway
 -> Security/JWT filter
 -> Controller + validation
 -> Service/business rule
 -> Repository hoặc object storage
 -> Mapper
 -> ApiResponse JSON
```

