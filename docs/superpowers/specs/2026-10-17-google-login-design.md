# Đăng nhập bằng Google — Design Spec

| Thuộc tính | Giá trị |
|---|---|
| Ngày | 2026-10-17 |
| Nhánh | `feature/GGOauth-integration` (cắt từ `dev`) |
| Trạng thái | Chờ review |
| Tài liệu gốc | Task sheet "PHÂN CHIA TASK" **không có dòng Google login**. Áp dụng rule chung của US1 `[BE] API Đăng nhập & Auth`: "Phản hồi chung chung … để bảo mật" |

## 1. Mục tiêu

FE (Vite) đã có Google Client ID (`VITE_GOOGLE_CLIENT_ID`, loại Web application) và dùng Google Identity Services để lấy **ID token** (`credential`). BE cần nhận token đó, xác minh, rồi trả về **JWT của VeggiePal**, giống hệt `POST /auth/login`. Nhờ vậy mọi service khác không phải sửa gì.

## 2. Luồng

```
FE: Google Sign-In → credential (ID token, RS256, do Google ký)
  → POST /api/auth/google  {"idToken": "<credential>"}
  → gateway (/api/auth/** → identity-service, StripPrefix=1)
  → identity-service POST /auth/google
      1. GoogleTokenVerifier.verify: chữ ký (JWKS của Google), exp/nbf, aud == google.client-id, iss ∈ {https://accounts.google.com, accounts.google.com}
      2. email_verified phải là true
      3. tìm user theo email (trim + lowercase), tạo hoặc liên kết (mục 3)
      4. JwtService.generateToken(user) → LoginResponse
```

Không dùng `spring-boot-starter-oauth2-client`, không có session, không có redirect URI và không cần Client Secret. Không thêm dependency: `NimbusJwtDecoder` đã có sẵn trong `spring-boot-starter-security-oauth2-resource-server`.

## 3. Quy tắc tài khoản

| Trường hợp | Xử lý |
|---|---|
| Email chưa có user | Tạo user: `role=USER`, `status=ACTIVE`, `emailVerified=true`, `fullName` = claim `name` (thiếu hoặc rỗng thì lấy phần trước `@` của email), `avatarUrl` = claim `picture`, `passwordHash` = BCrypt của một UUID ngẫu nhiên (cột `nullable=false`) |
| Có user, `emailVerified=false` | **Liên kết và vô hiệu mật khẩu**: đổi `passwordHash` thành hash ngẫu nhiên, `emailVerified=true`, `PENDING → ACTIVE` (trạng thái khác giữ nguyên) |
| Có user, `emailVerified=true` | Không sửa gì, phát token luôn |
| Google trả `email_verified != true` | 401 `GOOGLE_TOKEN_INVALID` |

**Vì sao vô hiệu mật khẩu** (Long đã chọn ngày 2026-10-17): để chặn kiểu chiếm tài khoản đăng ký trước. Kẻ xấu đăng ký trước bằng email của nạn nhân và biết mật khẩu. Khi nạn nhân login Google, nếu giữ nguyên mật khẩu thì kẻ xấu vẫn vào được tài khoản. Hiện chưa có luồng xác minh email, nên mọi tài khoản đăng ký bằng mật khẩu đều có `emailVerified=false`. **Hệ quả đã chấp nhận:** chủ tài khoản thật, sau lần đầu login Google, sẽ không login bằng mật khẩu cũ được nữa và chỉ còn đăng nhập bằng Google.

Avatar của user đã có không bị ghi đè. `CloudinaryFileStorageService.delete` bỏ qua URL không phải Cloudinary, nên thay avatar sau này không đụng tới ảnh Google.

## 4. API

`POST /auth/google` (public, qua gateway là `/api/auth/google`)

Request:
```json
{ "idToken": "eyJhbGciOiJSUzI1NiIs..." }
```

Response 200: giống hệt `/auth/login`:
```json
{ "code": 1000, "result": { "accessToken": "...", "userId": 7, "email": "an@gmail.com", "fullName": "An Nguyen", "role": "USER" } }
```

Lỗi:

| Khi nào | HTTP | code |
|---|---|---|
| `idToken` thiếu hoặc rỗng | 400 | 1018 `INVALID_REQUEST` |
| Token sai chữ ký, hết hạn, sai `aud`/`iss`, malformed, `email_verified` không phải true | 401 | 1019 `GOOGLE_TOKEN_INVALID` ("Google sign-in failed") |

## 5. Cấu hình

- `identity-service/application.properties`: `google.client-id=${GOOGLE_CLIENT_ID:}`. Mặc định rỗng để context (kể cả `contextLoads`) vẫn chạy được khi không có `.env`. Khi rỗng, mọi Google login đều bị từ chối vì sai `aud`.
- `.env` gốc (Long tự điền): `GOOGLE_CLIENT_ID=` trùng **đúng** giá trị `VITE_GOOGLE_CLIENT_ID` của FE.
- `.env.example`: thêm mục `GOOGLE_CLIENT_ID`.
- `docker-compose.yml` đã nạp `.env` qua `env_file`, không cần sửa. Gateway cũng không cần sửa.

## 6. Ngoài phạm vi

- Chặn user `BLOCKED`/`INACTIVE` khi login: `/auth/login` hiện cũng không chặn, giữ nhất quán.
- Lưu `google_sub` hay bảng liên kết provider: email là khóa liên kết, không cần migration.
- Lấy lại mật khẩu / đặt mật khẩu cho tài khoản Google.
- Phân biệt "Google không truy cập được" (JWKS lỗi mạng) với "token sai": cả hai đều trả 401 `GOOGLE_TOKEN_INVALID`.
- Hai request login Google lần đầu cùng email chạy song song: request thua sẽ vướng unique constraint và trả 500.
- Tin `email_verified` cho mọi domain: Google chỉ có thẩm quyền tuyệt đối với `@gmail.com` hoặc tài khoản Workspace (claim `hd`). Với domain khác, người từng kiểm soát hộp thư cũ có thể liên kết vào tài khoản. Chấp nhận ở phạm vi đồ án; muốn chặt hơn thì chỉ liên kết khi email `@gmail.com` hoặc có `hd`.
- Chặn hoàn toàn chiếm tài khoản đăng ký trước: đổi mật khẩu chặn login mới, nhưng JWT kẻ đăng ký trước đang giữ vẫn dùng được tới khi hết hạn (24h, không có revocation).

## 7. Kiểm thử

- `GoogleTokenVerifierTest` (không cần mạng): validator chấp nhận đúng `aud` và cả hai dạng `iss`, từ chối sai `aud`, sai `iss`, token hết hạn. `verify("not-a-jwt")` ném `GOOGLE_TOKEN_INVALID` (lỗi parse xảy ra trước khi gọi JWKS).
- `UserServiceTest` (mới): tạo user mới, fallback tên, liên kết tài khoản chưa verify (đổi hash, kích hoạt), tài khoản đã verify không bị sửa, `email_verified=false` bị từ chối.
- `SecurityConfigTest`: `/auth/google` là public (kể cả khi có bearer cũ), `idToken` rỗng trả 400/1018.
