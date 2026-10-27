# Hiểu VeggiePal backend từ folder đến workflow

Ngày đọc source: **27/10/2026**. Phiên bản đối chiếu: `21a45e9ec09316fd5293151ad891ef990c45e159`.

**Điều cần nắm trước tiên:** VeggiePal_BE là một repository chứa **8 ứng dụng Spring Boot độc lập**: một Gateway và bảy service nghiệp vụ. Folder cho biết “ai phụ trách việc gì”; workflow cho biết “một yêu cầu đi qua những ai”. Khi nối được hai góc nhìn này, bạn sẽ biết nên đọc và sửa file nào.

Tài liệu dựa trên source và cấu hình hiện tại. Các ví dụ request dùng để học; không khẳng định hệ thống đang chạy hay API ngoài đã được cấu hình thành công. README và một số hướng dẫn cũ chưa phản ánh hết các service đã có.

## 1. Cách học tài liệu này

1. Mở [bản đồ service](D:/FPT/SWP/VeggiePal/VeggiePal_BE/.archify/architecture-veggiepal-workflow-20261027-114048/ban-do-service.html) để biết chức năng nằm ở đâu.
2. Đọc mục 2–5 để hiểu folder và vai trò từng lớp.
3. Mở [workflow ghi sức khỏe](D:/FPT/SWP/VeggiePal/VeggiePal_BE/.archify/architecture-veggiepal-workflow-20261027-114048/workflow-ghi-suc-khoe.html), rồi đọc mục 6 cùng source.
4. Đọc các luồng liên service ở mục 7–10 sau khi đã hiểu một request đơn giản.
5. Dùng mục 12–14 khi tự sửa tính năng hoặc tìm lỗi.

Nội dung sơ đồ bằng tiếng Việt; một số điều khiển của Archify Viewer vẫn dùng tiếng Anh. Các sơ đồ đã qua kiểm tra tự động của Archify, bao gồm kiểm tra bằng trình duyệt; chưa thực hiện đánh giá trực quan thủ công.

## 2. Bức tranh tổng thể: một hệ thống có nhiều bộ phận

Hãy hình dung VeggiePal như một trung tâm dịch vụ. Gateway là quầy tiếp nhận và chuyển yêu cầu. Mỗi service nghiệp vụ là một bộ phận chuyên môn, có dữ liệu và quy tắc riêng.

| Folder | Cổng mặc định | Trách nhiệm chính | Đường dẫn qua Gateway tiêu biểu |
|---|---:|---|---|
| `api-gateway/` | 8080 | Nhận request, định tuyến, CORS, tập hợp Swagger | `/api/**`; riêng AI còn có `/ai/**` |
| `identity-service/` | 8081 | Đăng ký, đăng nhập, Google login, JWT, hồ sơ, avatar | `/api/auth/**`, `/api/users/**` |
| `nutrition-service/` | 8082 | Lịch sử chiều cao/cân nặng/BMI, danh mục và lựa chọn dị ứng | `/api/nutrition/**` |
| `blog-service/` | 8083 | Bài viết, video cộng đồng, category, comment, vote, trạng thái kiểm duyệt | `/api/blogs/**`, `/api/videos/**`, `/api/categories/**`, `/api/comments/**` |
| `recipe-service/` | 8084 | Công thức, nguyên liệu, định lượng, bước nấu và trạng thái công thức | `/api/recipes/**`, `/api/ingredients/**` |
| `location-service/` | 8085 | Tìm quán chay gần tọa độ người dùng | `/api/restaurants/**` |
| `ai-service/` | 8086 | Chat, thực đơn, gợi ý nội dung, kiểm duyệt AI, trích xuất video, quota/usage | `/api/ai/**` hoặc `/ai/**` |
| `payment-service/` | 8087 | Gói sử dụng, checkout payOS, webhook, đơn thanh toán, subscription, quản trị thanh toán | `/api/payments/**`, `/api/subscriptions/**`, `/api/admin/payments/**` |

**“Độc lập” có nghĩa gì?** Mỗi ứng dụng có `pom.xml`, Maven Wrapper, cấu hình, source, test và điểm khởi động riêng. Chạy Gateway không tự khởi động bảy service còn lại. Repository không có `pom.xml` tổng ở root để build tất cả trong một lần.

**Một request không phải đi qua tất cả service.** Đọc sức khỏe đi tới nutrition; tìm quán đi tới location. Tạo thực đơn mới cần ai phối hợp với nhiều service khác.

Căn cứ định tuyến: [application.properties của Gateway](D:/FPT/SWP/VeggiePal/VeggiePal_BE/api-gateway/src/main/resources/application.properties:9).

## 3. Folder structure ở root: phần nào phục vụ việc gì?

```text
VeggiePal_BE/
├── api-gateway/            Cửa vào HTTP
├── identity-service/       Tài khoản và danh tính
├── nutrition-service/      Sức khỏe và dị ứng
├── blog-service/           Nội dung cộng đồng
├── recipe-service/         Công thức và nguyên liệu
├── location-service/       Tìm quán chay
├── ai-service/             Điều phối các tính năng AI
├── payment-service/        Thanh toán và quyền sử dụng gói
├── docs/                   Tài liệu và lịch sử thiết kế
├── docker-compose.yml      Khai báo container hiện có
├── .env.example            Mẫu biến môi trường
├── .env                    Giá trị môi trường của máy, bị Git ignore
├── README.md               Hướng dẫn tổng quan, có phần đã cũ
├── CLAUDE.md               Quy ước làm việc và ghi chú kỹ thuật
├── .gitignore              Quy tắc loại file khỏi Git
├── .claude/, .superpowers/  Thư mục công cụ hỗ trợ phát triển
├── .idea/, .git/            Metadata IDE và lịch sử Git
├── tmp/                    Thư mục tạm ở root; không phải service
└── .archify/               Tài liệu và sơ đồ được tạo cho lần giải thích này
```

`docs/superpowers/specs/` chứa thiết kế; `docs/superpowers/plans/` chứa kế hoạch thực hiện. Chúng giải thích lý do của quyết định, nhưng cần đối chiếu implementation khi muốn biết hành vi đang có. `docs/nutrition-audit/` lưu căn cứ và truy vấn phục vụ rà soát dữ liệu dinh dưỡng.

`docker-compose.yml` hiện định nghĩa **MySQL, identity-service và payment-service**. File này chưa khai báo toàn bộ tám ứng dụng, nên không thể hiểu `docker compose up` là đã bật đủ backend.

`tmp/` ở root không nên bị đồng nhất với mọi file tạm của AI: `ai.video.storage-dir` mặc định là `./tmp/veggiepal-ai-video`, được giải quyết theo thư mục làm việc của tiến trình AI hoặc giá trị cấu hình ghi đè.

## 4. Bên trong một service: đọc cây thư mục như thế nào?

Mẫu sau áp dụng cho phần lớn service; không phải service nào cũng có đủ mọi package:

```text
<service>/
├── pom.xml
├── mvnw, mvnw.cmd
├── .mvn/wrapper/
├── src/
│   ├── main/
│   │   ├── java/com/veggiepal/<domain>/
│   │   │   ├── <Tên>Application.java
│   │   │   ├── configuration/
│   │   │   ├── controller/
│   │   │   ├── dto/request/
│   │   │   ├── dto/response/
│   │   │   ├── service/
│   │   │   ├── repository/
│   │   │   ├── entity/
│   │   │   ├── mapper/
│   │   │   ├── exception/
│   │   │   └── enums/
│   │   └── resources/
│   │       ├── application.properties
│   │       └── data.sql                 Có ở một số service
│   └── test/
│       ├── java/...                     Test Java
│       └── resources/...                Cấu hình/dữ liệu test nếu có
└── target/                              Kết quả build, có thể được sinh ra
```

Identity dùng package gốc `com.veggiepal`; các service khác thêm domain như `com.veggiepal.nutrition`. Vì thế đường dẫn identity không có thêm đoạn `/identity/`.

### 4.1. Những file giúp ứng dụng khởi động

| File/folder | Vai trò | Cách hiểu dễ nhớ |
|---|---|---|
| `pom.xml` | Khai báo dependency và cấu hình build | Danh sách thư viện và cách đóng gói ứng dụng |
| `mvnw`, `mvnw.cmd`, `.mvn/wrapper/` | Chạy Maven Wrapper | Công cụ chạy build; Windows thường dùng `mvnw.cmd` |
| `*Application.java` | Có `main()` gọi `SpringApplication.run()` | Công tắc khởi động service |
| `application.properties` | Port, database, JWT, URL service khác, giới hạn… | Bảng thiết lập của tiến trình |
| `data.sql` | Nạp dữ liệu khởi tạo theo cấu hình SQL init | Nội dung mẫu/cơ sở mà chức năng sử dụng |
| `db/migration/` trong AI | Các migration Flyway có phiên bản | Lịch sử thay đổi schema được áp dụng theo thứ tự |
| `target/` | Class đã biên dịch, JAR, báo cáo test, source sinh tự động | Sản phẩm của build; không phải nơi sửa source gốc |

Khi chạy service, Spring đọc cấu hình, tạo các đối tượng được quản lý gọi là **bean**, kết nối các bean với nhau và khởi tạo hạ tầng cần thiết. Chẳng hạn, Controller cần Service và Service cần Repository; Spring cung cấp chúng thông qua constructor.

`@RequiredArgsConstructor` của Lombok sinh constructor cần thiết; `@FieldDefaults(... makeFinal = true)` được nhiều class dùng để biến field thành `private final`. Vì vậy không thấy `new HealthRecordService(...)` trong Controller vẫn là bình thường: Spring thực hiện việc kết nối này.

### 4.2. Trách nhiệm của từng package

| Package | Trả lời câu hỏi | Việc thực tế trong repo | Khi nào bạn mở nó? |
|---|---|---|---|
| `configuration/` | Ứng dụng được thiết lập ra sao? | Security/JWT, Swagger; một số service có Cloudinary, HTTP client hoặc cấu hình upload | Lỗi 401, cấu hình token, Swagger, timeout, tích hợp ngoài |
| `controller/` | URL nào gọi hàm nào? Ai đang gửi request? | Mapping URL, nhận body/query/path, `@Valid`, lấy user từ JWT, gọi service, bọc response | Muốn bắt đầu lần theo một API |
| `dto/request/` | Client được gửi những gì? | Trường đầu vào và annotation validation | Thêm trường input, kiểm tra body 400 |
| `dto/response/` | Client nhận những gì? | Dữ liệu trả ra, `ApiResponse`, `PageResponse` | Đổi dữ liệu frontend nhìn thấy |
| `service/` | Quy tắc nghiệp vụ là gì? | Tính BMI, kiểm tra chủ sở hữu, trạng thái, gọi repository hoặc tích hợp ngoài | Thay đổi cách chức năng hoạt động |
| `repository/` | Đọc/ghi dữ liệu bằng cách nào? | Spring Data JPA, derived query, `@Query`, khóa dữ liệu khi cần | Sai kết quả truy vấn, cần query mới |
| `entity/` | Dữ liệu được lưu dưới hình dạng nào? | `@Entity`, `@Table`, `@Column`, ID, quan hệ nội bộ domain | Thêm/sửa cột hoặc quan hệ dữ liệu |
| `mapper/` | Chuyển đổi giữa các kiểu dữ liệu thế nào? | Các interface MapStruct, ví dụ entity → DTO | Trường response bị thiếu hoặc map sai |
| `exception/` | Lỗi nghiệp vụ được biểu diễn và trả ra sao? | `ErrorCode`, `AppException`, `GlobalExceptionHandler` | Thêm lỗi có mã/HTTP status rõ ràng |
| `enums/` | Những giá trị hữu hạn được phép là gì? | Role, trạng thái, loại nội dung… | Đọc hoặc sửa vòng đời/trạng thái |

**DTO và Entity khác nhau ở mục đích.** `HealthRecordRequest` chỉ nhận chiều cao/cân nặng; `HealthRecord` còn có ID, userId, BMI, thời gian và ánh xạ database. Client không được tự quyết định mọi cột của entity.

**Mapper không phải một “trạm” mọi request luôn đi qua.** Nó là helper được gọi ở nơi cần chuyển kiểu. Ví dụ ghi sức khỏe: service tạo entity, lưu rồi gọi mapper; controller bọc kết quả. Một số service hoặc tính năng AI tự dựng DTO mà không có package mapper riêng.

**Repository là interface vẫn hoạt động được** vì Spring Data tạo implementation khi khởi động. `findByIdAndUserId(id, userId)` diễn tả truy vấn cần lọc cả ID bản ghi lẫn người sở hữu.

### 4.3. Vì sao `ai-service` trông khác?

AI gom code **theo tính năng**. Thay vì tất cả Controller nằm chung một folder, `chat/` đặt Controller, Service, Entity, Repository và DTO của chat cạnh nhau.

```text
ai-service/src/main/java/com/veggiepal/ai/
├── AiServiceApplication.java
├── configuration/     Security/JWT, OpenAPI, HTTP client
├── security/          Helper xác định user hiện tại
├── common/            ApiResponse, PageResponse, lỗi dùng trong AI
├── groq/              Adapter gọi Groq, cấu hình model, DTO của provider
├── chat/              Hội thoại, tin nhắn, guest session, IP, lưu chat
├── mealplan/          Sinh/sửa thực đơn, yêu thích, phiên bản, kiểm tra dinh dưỡng
├── moderation/        Hồ sơ kiểm duyệt, kết quả AI, xử lý quyết định admin
├── recommendation/    Lấy nội dung thật và nhờ AI xếp hạng gợi ý
├── quota/             Giới hạn lượt dùng theo tính năng và kỳ
├── subscription/      Đọc quyền lợi gói từ payment-service
├── monitoring/        Lưu/đọc usage, token, thời gian và tình trạng gọi AI
└── video/             Job nền, lưu tạm, media tools, trích xuất công thức
```

**Cách tìm file:** cần hiểu chat thì bắt đầu ở `chat/ChatController.java`, tiếp đến `ChatService.java`. Cần hiểu thực đơn thì bắt đầu ở `mealplan/MealPlanController.java`. Chức năng vẫn có các vai trò Controller/Service/Repository, chỉ thay đổi cách sắp xếp folder.

Hai phần có cách tổ chức chuyên biệt khác là `blog-service/moderation/` (cầu nối kiểm duyệt), `blog-service/validation/` (constraint `MaxWords` và validator), cùng `payment-service/payos/` và `security/` (adapter payOS và xác thực lời gọi nội bộ).

## 5. Dữ liệu được chia cho service như thế nào?

| Service | Schema MySQL mặc định | Dữ liệu tiêu biểu thuộc quyền quản lý |
|---|---|---|
| identity | `veggiepal_identity` | User, password hash, role, thông tin hồ sơ |
| nutrition | `veggiepal_nutrition` | Health record, catalog allergen, lựa chọn dị ứng |
| blog | `veggiepal_blog` | Blog, video, category, comment, vote |
| recipe | `veggiepal_recipe` | Recipe, ingredient, recipe ingredient, recipe step |
| location | `veggiepal_location` | Quán chay và tọa độ |
| ai | `veggiepal_ai` | Chat, quota, usage, meal plan, phiên bản/yêu thích, moderation case, video job |
| payment | `veggiepal_payment` | Subscription plan, payment order, user subscription |

Bảy schema có thể cùng nằm trong một MySQL server. Docker Compose hiện dùng một container MySQL với volume `mysql_data`; không có bằng chứng rằng dự án đang chạy bảy máy database riêng.

Ví dụ nutrition lưu `Long userId`, không đặt quan hệ JPA sang entity `User` của identity. Muốn lấy dữ liệu domain khác, service phối hợp qua HTTP khi workflow cần. Đây là lý do AI gọi API hồ sơ và API sức khỏe, thay vì import Repository của hai service đó.

Hai loại lưu trữ khác cũng cần nhớ:

- **Cloudinary:** lưu media như avatar, thumbnail/video cộng đồng. Database lưu URL và metadata tương ứng.
- **Ổ đĩa tạm của AI:** giữ video/audio trong lúc xử lý job. Kết quả và trạng thái job được lưu riêng trong database; file tạm được cleanup theo luồng xử lý/maintenance.

AI dùng Flyway với các file `V1` đến `V7` hiện có và Hibernate `ddl-auto=validate`. Các service dữ liệu còn lại cấu hình `ddl-auto=update`. Vì vậy quy trình đổi schema của AI khác với các service đó.

Căn cứ: `application.properties` trong từng service, [cấu hình AI](D:/FPT/SWP/VeggiePal/VeggiePal_BE/ai-service/src/main/resources/application.properties:19) và [Docker Compose](D:/FPT/SWP/VeggiePal/VeggiePal_BE/docker-compose.yml:3).

## 6. Luồng mẫu nền tảng: ghi một bản ghi sức khỏe

Đây là luồng nên học đầu tiên vì đủ Controller, DTO, Service, Repository, Entity, Mapper và Security nhưng ít phụ thuộc ngoài.

Ví dụ học tập:

```http
POST /api/nutrition/me/health-records
Authorization: Bearer <access-token>
Content-Type: application/json

{"heightCm": 170, "weightKg": 65}
```

### Bước 1 — Gateway tìm service theo URL

Route `/api/nutrition/**` chuyển tới nutrition-service. `StripPrefix=1` bỏ một đoạn đầu, nên URL tại service trở thành `/nutrition/me/health-records`.

Đọc [route nutrition](D:/FPT/SWP/VeggiePal/VeggiePal_BE/api-gateway/src/main/resources/application.properties:38). Gateway không chạy phép tính BMI.

### Bước 2 — Security của nutrition kiểm tra người gọi

Endpoint ghi sức khỏe không nằm trong danh sách public, nên phải xác thực JWT. Spring Security đọc Bearer token và dùng cấu hình JWT của service. Lỗi xác thực đi qua `SecurityExceptionHandler` và không chạy nghiệp vụ ghi sức khỏe.

Đọc [SecurityConfig](D:/FPT/SWP/VeggiePal/VeggiePal_BE/nutrition-service/src/main/java/com/veggiepal/nutrition/configuration/SecurityConfig.java:39). Chữ ký JWT được kiểm tra tại service; không có lời gọi identity để xác thực mỗi request sức khỏe.

### Bước 3 — Spring chuyển JSON thành DTO và kiểm tra đầu vào

`@RequestBody @Valid HealthRecordRequest` yêu cầu hai trường không null, chiều cao trong 50–250 cm, cân nặng trong 20–300 kg và tối đa một chữ số thập phân. Kiểm tra này diễn ra trước thân method Controller.

Đọc [HealthRecordRequest](D:/FPT/SWP/VeggiePal/VeggiePal_BE/nutrition-service/src/main/java/com/veggiepal/nutrition/dto/request/HealthRecordRequest.java:18). Ví dụ gửi `heightCm=0` sẽ bị từ chối trước khi tính BMI.

### Bước 4 — Controller lấy userId đáng tin cậy rồi giao việc

Controller gọi `CurrentUser.id(jwt)`, rồi `healthRecordService.createRecord(userId, request)`. Client không gửi userId sở hữu trong body này.

Đọc [createRecord trong Controller](D:/FPT/SWP/VeggiePal/VeggiePal_BE/nutrition-service/src/main/java/com/veggiepal/nutrition/controller/HealthRecordController.java:35) và [CurrentUser](D:/FPT/SWP/VeggiePal/VeggiePal_BE/nutrition-service/src/main/java/com/veggiepal/nutrition/controller/CurrentUser.java:15).

### Bước 5 — Service thực hiện quy tắc nghiệp vụ

Service tạo `HealthRecord`, gán userId, chiều cao, cân nặng, thời điểm ghi và BMI. Công thức đang dùng là `weightKg / heightM²`, làm tròn `HALF_UP` đến một chữ số thập phân. Ví dụ trên cho `22.5`.

Đọc [HealthRecordService](D:/FPT/SWP/VeggiePal/VeggiePal_BE/nutrition-service/src/main/java/com/veggiepal/nutrition/service/HealthRecordService.java:38). Đây là chỗ tìm khi cần hiểu “BMI được tính như thế nào”.

### Bước 6 — Repository/JPA lưu entity

`healthRecordRepository.save(healthRecord)` giao việc lưu cho Spring Data JPA. Entity khai báo bảng `health_records`, các cột và cách sinh ID. JPA/Hibernate phối hợp với datasource để thực hiện SQL; thời điểm flush/commit do transaction và persistence context quyết định.

Đọc [Repository](D:/FPT/SWP/VeggiePal/VeggiePal_BE/nutrition-service/src/main/java/com/veggiepal/nutrition/repository/HealthRecordRepository.java:12) và [Entity](D:/FPT/SWP/VeggiePal/VeggiePal_BE/nutrition-service/src/main/java/com/veggiepal/nutrition/entity/HealthRecord.java:16).

### Bước 7 — Mapper tạo dữ liệu trả về

Service gọi `healthRecordMapper.toHealthRecordResponse(healthRecord)`. MapStruct sinh phần implementation khi build; bạn sửa interface mapper hoặc các kiểu dữ liệu gốc khi cần, không sửa generated source trong `target/`.

Đọc [Mapper](D:/FPT/SWP/VeggiePal/VeggiePal_BE/nutrition-service/src/main/java/com/veggiepal/nutrition/mapper/HealthRecordMapper.java:8).

### Bước 8 — Controller bọc response và trả cho client

Controller đặt DTO vào `ApiResponse.result`. Với builder hiện tại, `code` mặc định là `1000`. Jackson chuyển object thành JSON, HTTP response quay lại qua Gateway. Trường null có thể bị bỏ khỏi JSON theo `@JsonInclude`.

```json
{
  "code": 1000,
  "result": {
    "...": "Các trường của HealthRecordResponse"
  }
}
```

Đây là minh họa envelope, không phải payload response đầy đủ. `code=1000` là mã ứng dụng; nó khác với HTTP status 200/400/401/500.

### Nếu xảy ra lỗi thì sao?

- JWT không hợp lệ/thiếu ở endpoint bảo vệ: Security xử lý trước Controller.
- DTO không hợp lệ: `GlobalExceptionHandler` xử lý lỗi validation và chọn `ErrorCode`.
- Bản ghi không tồn tại hoặc không thuộc user khi sửa: service có thể ném `AppException`. Query sửa sức khỏe lọc theo cả `recordId` và `userId`.
- Lỗi không dự kiến: catch-all của handler trả mã lỗi chung theo cấu hình `ErrorCode`.

Hai lớp kiểm tra có vai trò khác nhau: **DTO kiểm tra hình dạng/giới hạn input; Service kiểm tra ý nghĩa nghiệp vụ và quyền sở hữu**.

## 7. Danh tính, hồ sơ và đăng nhập

### 7.1. Đăng ký

```text
Client → Gateway → UserController → UserService.createUser
       → Chuẩn hóa email, kiểm tra trùng
       → Mapper tạo User, BCrypt băm mật khẩu
       → Gán USER/ACTIVE/emailVerified=false
       → UserRepository.save → Response DTO
```

Password hash là kết quả băm, không phải mật khẩu rõ. Các giá trị role/status ban đầu được service gán; không để client quyết định qua DTO đăng ký.

### 7.2. Đăng nhập bằng mật khẩu

1. Client gọi `POST /api/auth/login`; đây là endpoint public.
2. `UserService.login()` chuẩn hóa email và tìm User.
3. `PasswordEncoder.matches()` so mật khẩu gửi lên với hash đã lưu.
4. Nếu đúng, `JwtService.generateToken()` phát access token.
5. Token hiện chứa subject email, claim `userId`, `role` và thời hạn 24 giờ.
6. Những request bảo vệ tiếp theo đính kèm `Authorization: Bearer …`.

Đọc [UserService](D:/FPT/SWP/VeggiePal/VeggiePal_BE/identity-service/src/main/java/com/veggiepal/service/UserService.java:39) và [JwtService](D:/FPT/SWP/VeggiePal/VeggiePal_BE/identity-service/src/main/java/com/veggiepal/service/JwtService.java:27).

### 7.3. Google login và cập nhật hồ sơ

Google login xác minh ID token qua `GoogleTokenVerifier`, kiểm tra email đã được xác minh, rồi tìm/liên kết hoặc tạo User trước khi phát JWT của VeggiePal. Đây vẫn là JWT của ứng dụng dùng cho các request nghiệp vụ sau đó.

`ProfileService` lo đọc/sửa profile, đổi mật khẩu và upload avatar. Luồng avatar kiểm tra dung lượng, MIME và chữ ký file, upload Cloudinary, lưu URL vào User và cleanup ảnh phù hợp. Ngày sinh, giới tính và mức vận động trong profile cũng là đầu vào khi AI tự tính mục tiêu dinh dưỡng.

Đọc [ProfileService](D:/FPT/SWP/VeggiePal/VeggiePal_BE/identity-service/src/main/java/com/veggiepal/service/ProfileService.java:39).

## 8. Blog, kiểm duyệt và quyền quản lý nội dung

`blog-service` quản lý nội dung gốc; `ai-service` phân tích và lưu hồ sơ kiểm duyệt. Hai nơi có dữ liệu với ý nghĩa khác nhau: **Blog là bài viết; ModerationCase là hồ sơ đánh giá bài viết**.

### 8.1. Tạo bản nháp hoặc xuất bản blog

1. `BlogService.createBlog()` tạo entity với trạng thái `DRAFT`, tác giả từ JWT và category hợp lệ.
2. Blog được `saveAndFlush` để có ID cho hồ sơ kiểm duyệt.
3. Nếu request không yêu cầu publish, luồng giữ bản nháp và không gọi moderation tại bước này.
4. Nếu yêu cầu publish, `AiContentModerationService` gửi HTTP tới `/ai/moderation/check`, kèm JWT người gọi và thông tin nội dung.
5. AI gọi Groq, đọc kết quả, lưu ModerationCase và trả quyết định.
6. BlogService áp dụng trạng thái rồi lưu và trả response.

| Quyết định AI | Trạng thái blog sau xử lý | Ý nghĩa |
|---|---|---|
| `APPROVED` | `PUBLISHED` | Được xuất bản |
| `REJECTED` | `REJECTED` | Bị từ chối |
| `REVIEW_REQUIRED` | `PENDING` | Chờ admin xét |

AI không truy cập trực tiếp bảng blog để đặt trạng thái. Chủ dữ liệu blog là BlogService. Khi AI không gọi được, adapter blog trả `MODERATION_UNAVAILABLE`; transaction của luồng tạo blog có thể rollback phần ghi của blog. Transaction ở blog không biến hai database thành một transaction phân tán.

Đọc [createBlog](D:/FPT/SWP/VeggiePal/VeggiePal_BE/blog-service/src/main/java/com/veggiepal/blog/service/BlogService.java:61), [adapter AI](D:/FPT/SWP/VeggiePal/VeggiePal_BE/blog-service/src/main/java/com/veggiepal/blog/moderation/AiContentModerationService.java:35) và [áp dụng trạng thái](D:/FPT/SWP/VeggiePal/VeggiePal_BE/blog-service/src/main/java/com/veggiepal/blog/service/BlogService.java:331).

### 8.2. Khi admin duyệt lại

Admin xử lý case chờ trong AI. `ModerationService.review()` gọi `ModerationOwnerClient` về API nội bộ của blog để áp dụng quyết định, sau đó cập nhật hồ sơ xét duyệt. Nhánh `DISMISSED` đóng case mà không yêu cầu đổi nội dung; nội dung đã bị xóa có cách xử lý đóng case riêng.

Blog có trạng thái `BANNED` khi bị quản trị viên gỡ theo quy tắc tương ứng. Các kiểm tra trong service ngăn owner sửa/xuất bản hoặc quyết định kiểm duyệt cũ làm bài viết được mở lại.

Đọc [review trong AI](D:/FPT/SWP/VeggiePal/VeggiePal_BE/ai-service/src/main/java/com/veggiepal/ai/moderation/ModerationService.java:69). Các helper có tên “AutoApprove” không đủ chứng minh runtime đang tự duyệt; bean adapter đang đăng ký là `AiContentModerationService`.

Comment và vote có service riêng trong cùng domain. Đây không phải các ứng dụng Spring Boot riêng. Video cộng đồng thuộc blog; xử lý AI từ nội dung video thuộc ai-service — hai chữ “video” có hai trách nhiệm.

## 9. AI tạo thực đơn: ví dụ phối hợp nhiều service

Điểm vào: `POST /api/ai/meal-plans` hoặc `/ai/meal-plans` qua Gateway. Controller ở AI nhận JWT và chuyển xuống `MealPlanService.generate()`.

```text
Client → Gateway → AI MealPlanController → MealPlanService
                                        ├─ Recipe: lấy công thức đã xuất bản
                                        ├─ Nutrition: dị ứng + health record mới nhất
                                        ├─ Identity: profile nếu cần tự tính target
                                        ├─ Payment: quyền lợi gói để áp dụng quota
                                        ├─ Groq: đề xuất thực đơn từ candidates
                                        └─ Validator → lưu MealPlan trong schema AI
```

Các nhánh minh họa dependency; thứ tự thực thi chính xác là các bước dưới đây, không ngụ ý mọi lời gọi chạy song song.

### 9.1. Đọc dữ liệu thật trước khi nhờ AI

1. Lấy tối đa 200 công thức qua `/recipes?size=200` từ recipe-service.
2. Lấy dị ứng của user qua nutrition.
3. Lấy health record mới nhất qua nutrition.
4. Nếu request cung cấp `dailyNutritionTarget`, dùng target đó và kiểm tra. Nếu không, đọc profile qua identity để `NutritionTargetCalculator` tính mục tiêu từ thông tin cần thiết.
5. Chỉ sau khi target được giải quyết và hợp lệ, reserve quota tạo thực đơn.
6. Lọc candidates bằng `MealPlanValidator.safeCandidates()`, xét dị ứng/nguyên liệu và dữ liệu công thức.

Vì vậy, một số lỗi hồ sơ/target xảy ra trước khi bị tính một lượt quota. Đồng thời, việc có target từ client không có nghĩa là bỏ qua toàn bộ health record: implementation hiện vẫn lấy health record từ bước đầu.

### 9.2. Groq đề xuất, backend kiểm tra

Prompt yêu cầu thực đơn bảy ngày, mỗi ngày có BREAKFAST/LUNCH/DINNER và chỉ dùng recipe ID thật đã cung cấp. Groq trả dữ liệu đề xuất; backend không coi JSON đó là dữ liệu đã đúng.

`MealPlanValidator.validateAndEnrich()` kiểm tra/bổ sung kết quả trên tập công thức an toàn và mục tiêu đã chọn. Các helper như `AllergenDetector`, `IngredientUnitConverter`, `NutritionTargetCalculator` tách những phần kiểm tra/tính toán chuyên biệt khỏi bước gọi model.

**Fallback có điều kiện:** một số lỗi sinh thực đơn/response từ Groq đi vào planner xác định qua validator. Những lỗi như `QUOTA_EXCEEDED`, `UNAUTHENTICATED`, `GROQ_NOT_CONFIGURED` được ném tiếp trong nhánh catch của `generate()`. Không thể kết luận “Groq lỗi kiểu nào cũng trả được thực đơn”.

### 9.3. Lưu kết quả và các chức năng tiếp theo

Service lưu JSON thực đơn cùng các ràng buộc liên quan như dị ứng, health record, target, sở thích vào MealPlan của AI. Service còn có các luồng thay bữa, favorite, chỉnh tay, cập nhật từ sức khỏe và xem phiên bản. Phần này không biến kết quả AI thành một Recipe mới trong recipe-service một cách tự động.

Đọc [generate](D:/FPT/SWP/VeggiePal/VeggiePal_BE/ai-service/src/main/java/com/veggiepal/ai/mealplan/MealPlanService.java:88) và [computeTarget](D:/FPT/SWP/VeggiePal/VeggiePal_BE/ai-service/src/main/java/com/veggiepal/ai/mealplan/MealPlanService.java:365).

### 9.4. Chat và recommendation khác luồng thực đơn thế nào?

- **Chat thành viên:** đọc quyền lợi/quota, tạo hoặc kiểm tra hội thoại thuộc user, lấy lịch sử gần đây, gọi Groq, ghi usage và lưu cặp tin nhắn. `ChatPersistenceService` lo phần lưu exchange. Chat khách dùng guest session và dấu vết IP theo cơ chế riêng.
- **Recommendation:** lấy blog/recipe thật, có thể quay về danh sách mới nhất nếu keyword không có candidates, nhờ Groq xếp hạng, rồi lọc ID trả về theo candidates. Nếu một nguồn nội dung lỗi vẫn có thể dùng nguồn còn lại; cả hai nguồn lỗi mới thành outage theo implementation hiện có.
- **Monitoring/usage:** ghi token, thời gian và thành công/thất bại để theo dõi và kiểm soát việc dùng AI; quota tính năng và ngân sách token là hai mối quan tâm liên quan nhưng khác nhau.

Đọc [ChatService](D:/FPT/SWP/VeggiePal/VeggiePal_BE/ai-service/src/main/java/com/veggiepal/ai/chat/ChatService.java:41) và [RecommendationService](D:/FPT/SWP/VeggiePal/VeggiePal_BE/ai-service/src/main/java/com/veggiepal/ai/recommendation/RecommendationService.java:41).

## 10. Thanh toán và video nền: hai luồng cần phân biệt thời điểm hoàn tất

### 10.1. Mua gói sử dụng qua payOS

```text
Client → POST checkout → PaymentService → tạo/tái dùng đơn PENDING
                                       → PayOsGateway tạo link/QR
Client mở link/QR và thanh toán
payOS → webhook → xác minh → kiểm tra đơn/số tiền/định danh
                        → lưu PAID + kích hoạt subscription
AI → internal subscription API → áp dụng quyền lợi/quota
```

Chi tiết cần nhớ:

1. Checkout kiểm tra gói tồn tại, active và mua được; gói miễn phí không đi qua luồng mua này.
2. Giá được lấy từ plan trong backend. Service có cơ chế tái dùng đơn `PENDING` còn phù hợp.
3. Adapter `PayOsGateway`/`PayOsSdkAdapter` đóng gói chi tiết SDK payOS; Controller chỉ nhận request API.
4. Webhook phải qua `verifyWebhook`, kiểm tra mã thành công/tiền tệ và đối chiếu đơn, số tiền, payment link khi có.
5. Đơn đã `PAID` hoặc `CANCELLED` không bị kích hoạt lại bằng cách xử lý lặp cùng webhook. Probe đã ký hợp lệ mà không có đơn tương ứng được acknowledge mà không đổi trạng thái.
6. Khi đơn hợp lệ được ghi `PAID`, `SubscriptionService.activate()` cập nhật gói và thời hạn trong transaction của luồng thanh toán. Mua lại cùng gói còn hạn có nhánh cộng thời gian từ hạn hiện tại.
7. `SubscriptionService` còn xử lý quay về FREE khi hết hạn, lúc đọc hoặc qua scheduled job.

**Mở trang return URL chưa phải là bằng chứng backend đã kích hoạt gói.** Chỗ đổi trạng thái thành công trong code là luồng webhook được xác minh.

AI đọc quyền lợi qua `/internal/subscriptions/{userId}` bằng header `X-Internal-Service-Token`, không tự quyết định gói từ một field frontend gửi. Nếu truy vấn subscription thất bại hoặc không còn hiệu lực, `PremiumQuotaService` áp dụng quyền lợi FREE theo nhánh fallback hiện tại. Quota chat dùng kỳ ngày; meal plan và video dùng kỳ tháng trong class này.

Đọc [PaymentService](D:/FPT/SWP/VeggiePal/VeggiePal_BE/payment-service/src/main/java/com/veggiepal/payment/service/PaymentService.java:35), [webhook](D:/FPT/SWP/VeggiePal/VeggiePal_BE/payment-service/src/main/java/com/veggiepal/payment/service/PaymentService.java:104), [SubscriptionService](D:/FPT/SWP/VeggiePal/VeggiePal_BE/payment-service/src/main/java/com/veggiepal/payment/service/SubscriptionService.java:22) và [PremiumQuotaService](D:/FPT/SWP/VeggiePal/VeggiePal_BE/ai-service/src/main/java/com/veggiepal/ai/quota/PremiumQuotaService.java:20).

### 10.2. Trích xuất công thức từ video bằng job nền

AI có hai điểm vào chính: upload file để xử lý và xử lý một video đã có trong blog. Controller trả **HTTP 202 Accepted** với thông tin job: server đã nhận việc, kết quả có thể chưa sẵn sàng.

```text
Request → kiểm tra file/video nguồn → lưu file tạm + kiểm tra media
        → reserve quota → lưu VideoJob → đưa vào Executor → trả 202

Worker → bắt đầu job → ffmpeg tách audio
       → Groq transcription → transcript
       → Groq extraction → JSON công thức
       → kiểm tra cấu trúc → COMPLETED hoặc FAILED
       → cleanup file tạm

Client → API đọc trạng thái/kết quả job
```

Trong nhánh video đã có, `BlogVideoClient` lấy metadata từ blog; service yêu cầu nguồn phù hợp, có thể dùng lại kết quả/job theo logic của nó và tải video từ nguồn Cloudinary được hỗ trợ. Không nên diễn giải thành “mọi YouTube URL đều được AI tải xuống và xử lý”.

`VideoExecutorConfig` cấu hình executor và hàng đợi **trong tiến trình Java**. Đây không phải bằng chứng dự án dùng Kafka/RabbitMQ. `VideoJobStateService` quản lý cập nhật trạng thái; worker lỗi có nhánh `failAndRefund` và cleanup. `VideoMaintenanceService` xử lý bảo trì định kỳ theo cấu hình.

Trích xuất dựa trên audio/transcript trong worker đang đọc, không phải luồng phân tích từng frame hình ảnh. JSON kết quả được kiểm tra tên trường, title, ingredient/quantity và thứ tự bước trước khi hoàn tất.

Đọc [Controller trả 202](D:/FPT/SWP/VeggiePal/VeggiePal_BE/ai-service/src/main/java/com/veggiepal/ai/video/VideoProcessingController.java:17), [service tiếp nhận](D:/FPT/SWP/VeggiePal/VeggiePal_BE/ai-service/src/main/java/com/veggiepal/ai/video/VideoProcessingService.java:55) và [Worker](D:/FPT/SWP/VeggiePal/VeggiePal_BE/ai-service/src/main/java/com/veggiepal/ai/video/VideoProcessingWorker.java:48).

## 11. Recipe và location: hai domain dễ bị hiểu nhầm

### Recipe là dữ liệu công thức có cấu trúc

`Recipe` có danh sách `RecipeIngredient` và `RecipeStep`; ingredient tham chiếu dữ liệu nguyên liệu mà service quản lý. Tạo/sửa công thức kiểm tra các phần tử, nguyên liệu active và đơn vị hỗ trợ. Service có luồng DRAFT → PUBLISHED; xóa chuyển sang ARCHIVED và có kiểm tra trạng thái/owner.

Dữ liệu dinh dưỡng của nguyên liệu được đưa vào dữ liệu công thức; AI dùng chúng cùng lượng/đơn vị để tính và kiểm tra thực đơn. Vì thế “hồ sơ sức khỏe” thuộc nutrition nhưng “nguyên liệu/công thức” thuộc recipe, còn “tính mục tiêu và lắp thực đơn cá nhân” nằm trong AI.

Đọc [RecipeService](D:/FPT/SWP/VeggiePal/VeggiePal_BE/recipe-service/src/main/java/com/veggiepal/recipe/service/RecipeService.java:57). `recipe-service/resources/data.sql` còn quan trọng vì seed chứa dữ liệu nền; sửa một dòng INSERT có điều kiện tồn tại không có nghĩa database cũ tự nhận giá trị mới.

### Location truy vấn dữ liệu quán đã có

Luồng tìm gần đây là `RestaurantController → RestaurantService.findNearby → RestaurantRepository.findNearby → DTO`.

Service kiểm tra latitude, longitude và bán kính; bán kính tối đa hiện là 10 km. Query MySQL dùng `ST_Distance_Sphere`, lọc bán kính/vegan và sắp theo khoảng cách. Đây là truy vấn database của location, không phải mặc nhiên gọi Google Maps để tìm quán mới theo mỗi request.

Location hiện phục vụ API công khai và không có cấu hình Spring Security/JWT như các service bảo vệ dữ liệu user. Nó cũng không có package mapper riêng; service chuyển projection thành DTO trực tiếp.

Đọc [RestaurantService](D:/FPT/SWP/VeggiePal/VeggiePal_BE/location-service/src/main/java/com/veggiepal/location/service/RestaurantService.java:27) và [query khoảng cách](D:/FPT/SWP/VeggiePal/VeggiePal_BE/location-service/src/main/java/com/veggiepal/location/repository/RestaurantRepository.java:39).

## 12. Đọc và sửa một API: nên bắt đầu ở file nào?

| Bạn đang muốn làm gì? | Nơi bắt đầu | Những chỗ thường phải đi tiếp |
|---|---|---|
| Tìm API cho màn hình frontend | Gateway routes và Controller của domain | Request/Response DTO |
| Thêm trường đầu vào | Request DTO | Service, validation, test; Entity nếu cần lưu |
| Thêm trường trả ra | Response DTO | Mapper hoặc chỗ dựng DTO, nguồn dữ liệu |
| Đổi công thức/quy tắc | Service hoặc helper nghiệp vụ | Test nghiệp vụ tương ứng |
| Đổi cách tìm kiếm/lọc/sắp xếp | Service → Repository | Query, pagination, integration test |
| Thêm cột lưu trữ | Entity | Migration AI hoặc quy trình cập nhật schema của service, DTO/mapper nếu cần |
| API bị 401/403 | SecurityConfig, JwtConfig, SecurityExceptionHandler | JWT/role, CurrentUser, kiểm tra owner trong service |
| Blog publish lỗi dù vẫn lưu draft được | BlogService → AiContentModerationService | AI moderation, Groq, URL/timeout |
| AI lấy sai hạn mức | PremiumQuotaService → PaymentSubscriptionClient | Internal token, SubscriptionService, dữ liệu gói |
| Video nhận 202 nhưng chưa có kết quả | API trạng thái job → worker/state service | ffmpeg/ffprobe, Groq, storage, quota, maintenance |
| Thêm endpoint của service mới | Gateway application.properties | API route, docs route, Swagger urls entry, OpenApiConfig của service |

Một trình tự đọc có hiệu quả là:

```text
URL người dùng gọi
  → Gateway route
  → Security nếu có
  → Controller + Request DTO
  → Service
  → Repository/HTTP client/storage tùy việc
  → Entity hoặc kiểu kết quả
  → Mapper/Response DTO
  → Error handler cho các nhánh thất bại
  → Test gần nhất mô tả các trường hợp đó
```

Không phải thêm một field API là luôn phải sửa tất cả các lớp. Trường tính toán có thể không cần cột; trường chỉ phục vụ lưu trữ có thể không cần trả cho frontend. Hãy xác định vòng đời của chính field đó.

## 13. Kiểm thử và chạy ứng dụng: hiểu đúng phạm vi

### Các loại test giúp kiểm tra điều gì?

| Loại test | Ví dụ hiện có | Điều nó kiểm tra |
|---|---|---|
| Service unit test | `HealthRecordServiceTest`, `PaymentServiceTest`, `MealPlanServiceTest` | Quy tắc nghiệp vụ với dependency được kiểm soát/mock |
| Controller/security test | `HealthRecordControllerTest`, `SecurityConfigTest` | Mapping, validation, quyền truy cập, response HTTP |
| Integration test | `BlogServiceIntegrationTests`, `PaymentFlowIntegrationTest`, `RecipeUpdateIntegrationTest` | Nhiều thành phần thật phối hợp, tùy cấu hình database của test |
| Chuyên biệt dữ liệu/đồng thời | `RecipeSqlDataInitTest`, `QuotaConcurrencyIntegrationTest` | Seed dữ liệu hoặc tình huống cạnh tranh mà mock đơn giản khó bắt |

Test của các service không dùng cùng một cấu hình database. Recipe/payment/AI có cấu hình H2 test; một số test context/integration khác dùng MySQL. Hãy đọc `src/test/resources` và annotation của test trước khi chạy.

Lệnh ví dụ trong đúng thư mục service:

```powershell
.\mvnw.cmd test
.\mvnw.cmd test "-Dtest=HealthRecordServiceTest"
.\mvnw.cmd spring-boot:run
```

Ví dụ test đơn ở trên dùng tại nutrition-service. Quy ước repo yêu cầu chạy `BlogServiceIntegrationTests` khi thay entity/repository/query blog; mock test không đủ chứng minh query JPA và lazy loading hoạt động trên MySQL.

### Chạy local cần lưu ý điều gì từ cấu hình hiện tại?

- Compose ánh xạ MySQL mặc định **host 3307 → container 3306**. Datasource service hiện mặc định `DB_PORT=3306`; khi chạy bằng Maven trên host và dùng MySQL Compose, phải đặt `DB_PORT=3307` phù hợp.
- `docker compose up -d mysql` chỉ bật MySQL. Nếu chạy identity/payment qua Maven, tránh đồng thời bật container cùng service trên cùng cổng.
- Mỗi service đọc cấu hình riêng. Các URL `*_SERVICE_URL` phải đúng nơi các service đang chạy. Bên trong container, `localhost` chỉ chính container đó.
- Tính năng upload cần cấu hình Cloudinary; AI cần Groq và media tools cho luồng tương ứng; payOS cần cấu hình cho checkout/webhook. Khởi động một service và thực hiện thành công mọi tính năng là hai việc khác nhau.
- Swagger tổng hợp đặt tại Gateway `/swagger-ui.html`. Swagger giúp khám phá và thử API; không thay thế business service.

Lần giải thích này chỉ đọc source và tạo tài liệu/sơ đồ. Không chạy toàn bộ backend, không gọi Groq/payOS/Cloudinary và không chạy lại bộ test ứng dụng.

## 14. Những hiểu nhầm nên sửa ngay từ đầu

| Hiểu nhầm | Cách hiểu theo code hiện tại |
|---|---|
| “Gateway xác thực hết, các service chỉ nhận dữ liệu” | Các resource service bảo vệ endpoint bằng Security/JWT của chúng; Gateway lo định tuyến/CORS và phần hạ tầng HTTP liên quan |
| “Tất cả request phải gọi identity” | Service thường lấy userId từ JWT; chỉ gọi API identity khi thật sự cần dữ liệu hồ sơ |
| “Mỗi service là một folder Java trong cùng tiến trình” | Mỗi folder cấp root nói trên là ứng dụng/Maven project riêng |
| “Mọi service có đủ controller/service/repository/mapper” | Gateway, location và AI có cấu trúc/phạm vi khác nhau |
| “Có Long userId thì phải có foreign key tới identity” | Dữ liệu khác domain được giữ ranh giới; HealthRecord lưu userId, không ánh xạ entity User của identity |
| “AI trả JSON là có thể lưu ngay” | Các luồng có bước parse/validate và đối chiếu dữ liệu thật; mức kiểm tra tùy tính năng |
| “202 là đã xử lý video xong” | 202 là đã nhận job; đọc trạng thái/kết quả riêng |
| “Checkout thành công là đã mua gói xong” | Checkout tạo link; webhook hợp lệ mới là luồng ghi PAID/kích hoạt gói |
| “Transaction blog rollback cả dữ liệu AI” | Transaction nội bộ service không bao phủ HTTP và database service khác |
| “Repo chưa có migration” | AI đã có Flyway; các service dữ liệu còn lại vẫn đang dùng update |
| “Sửa README là biết feature đang có” | Đối chiếu Controller/Service/configuration; README có phần chưa theo kịp source |

## 15. Lộ trình tự đọc code cho người mới

1. **Buổi 1 — Một request:** lần theo `HealthRecordController → HealthRecordService → Repository → Entity → Mapper`. Tự chỉ được dòng lấy userId và dòng tính BMI.
2. **Buổi 2 — Security và lỗi:** xem DTO validation, CurrentUser, SecurityConfig và hai nhóm error handler. Giải thích được vì sao input sai và token sai đi vào hai nơi khác nhau.
3. **Buổi 3 — CRUD có trạng thái:** đọc RecipeService hoặc BlogService. Liệt kê trạng thái nào được sửa/xuất bản/xóa và chỗ kiểm tra quyền sở hữu.
4. **Buổi 4 — Phụ thuộc HTTP:** lần từ BlogService tới AI moderation. Chỉ được service nào quyết định trạng thái bài viết và service nào lưu hồ sơ kiểm duyệt.
5. **Buổi 5 — Điều phối AI:** lần từ MealPlanController tới generate, các lời gọi dữ liệu thật, quota, Groq, validator và save.
6. **Buổi 6 — Bất đồng bộ và tính lặp:** đọc worker video và webhook thanh toán. Phân biệt “đã nhận việc”, “đã có kết quả” và “một sự kiện được gửi lại”.

Sau mỗi buổi, chọn một request và tự trả lời năm câu: **Ai nhận? Ai kiểm tra? Ai xử lý? Ai giữ dữ liệu? Ai tạo response?** Nếu trả lời được bằng tên class và method cụ thể, bạn đã hiểu workflow của phần đó đủ để bắt đầu thay đổi có kiểm soát.
