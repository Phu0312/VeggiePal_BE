# Blog Service Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Dựng `blog-service` — service nội dung cộng đồng cho VeggiePal: blog, danh mục, bình luận và vote, có chỗ cắm sẵn cho video và AI moderation.

**Architecture:** Microservice Spring Boot thứ ba, đứng sau `api-gateway`, DB MySQL riêng, không gọi sang service khác lúc chạy. Xác thực bằng JWT dưới vai trò OAuth2 Resource Server, dùng chung secret HMAC với hai service hiện có. Comment và vote được mô hình hóa polymorphic (`target_type` + `target_id`) để thêm video sau này không phải migrate.

**Tech Stack:** Java 21, Spring Boot 4.1.1, Spring Cloud Gateway Server WebMVC, Spring Data JPA + Hibernate, MySQL 8.4, MapStruct 1.6.3, Lombok, springdoc-openapi 3.1.0, AWS SDK v2 (S3/MinIO), JUnit 5 + Mockito + AssertJ.

**Spec:** [`docs/superpowers/specs/2026-09-20-blog-service-design.md`](../specs/2026-09-20-blog-service-design.md)

## Global Constraints

Mọi task ngầm hiểu là phải tuân thủ mục này.

- **Commit sau mỗi task, chỉ trên nhánh `feature/blog-service`.** Người dùng cho phép riêng cho lần chạy này (2026-09-20) vì quy trình review cần diff theo từng task. Dùng đúng message ghi ở step Checkpoint của mỗi task. **Không bao giờ** `git push`, `git merge`, hay đụng tới `main` — phần đó vẫn là của người dùng.
- **Không có parent POM.** Mọi lệnh Maven chạy từ trong thư mục service: `cd blog-service && ./mvnw ...`. Trên Windows dùng `mvnw.cmd` hoặc Git Bash.
- **Không có linter/formatter** trong repo. Bám theo style của file xung quanh.
- **Spring Boot 4 khác Boot 3 ở ba chỗ dễ sai:**
  - starter là `spring-boot-starter-webmvc`, **không** phải `spring-boot-starter-web`
  - `@WebMvcTest` nằm ở `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest`, **không** phải `org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest`
  - Jackson 3: `tools.jackson.databind.json.JsonMapper`, **không** phải `com.fasterxml.jackson.databind.ObjectMapper`
- **Dependency injection:** `@RequiredArgsConstructor` + `@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)`, field khai báo trần.
- **Response envelope:** mọi endpoint trả `ApiResponse<T>`, `code` mặc định `1000`.
- **Lỗi:** `throw new AppException(ErrorCode.X)`. Validation message là **tên hằng số trong `ErrorCode`**, ví dụ `@NotBlank(message = "BLOG_TITLE_REQUIRED")`.
- **Dải error code:** blog-service dùng **30xx**. Mã dùng chung giữ nguyên số: `1001 INVALID_KEY`, `1008 UNAUTHENTICATED`, `1009 UNAUTHORIZED`, `1017 FILE_UPLOAD_FAILED`, `1018 INVALID_REQUEST`, `9999 UNCATEGORIZED_EXCEPTION`.
- **User id luôn lấy từ JWT** qua `CurrentUser.id(jwt)`. **Không bao giờ** lấy từ body hoặc path.
- **Không có khóa ngoại tới bảng `users`** và **không gọi HTTP sang identity-service** từ blog-service.
- **Enum trong DB:** Hibernate map `@Enumerated(EnumType.STRING)` thành `ENUM` native của MySQL, `ddl-auto=update` **không tự thêm hằng số mới**. Vì vậy `TargetType.VIDEO` và `CommentStatus.PENDING` phải có mặt ngay từ Task đầu tiên khai báo enum đó, dù chưa dùng.
- **Mock MapStruct trong unit test:** `@Spy XMapper mapper = Mappers.getMapper(XMapper.class);` — đúng idiom `HealthRecordServiceTest` đang dùng. Đừng `new XMapperImpl()`: tên class sinh ra là chi tiết nội bộ của MapStruct.
- **Chiều phụ thuộc giữa các service class** (không được tạo vòng lặp):
  ```
  BlogService    ──► CategoryService ──► BlogRepository
  CommentService ──► BlogService
  VoteService    ──► BlogService, BlogRepository
  ```
  `CategoryService` cần biết "còn blog nào dùng danh mục này không", nhưng nó phụ thuộc vào **`BlogRepository`**, không phải `BlogService`. Nếu ai đó đổi thành `BlogService`, Spring sẽ báo circular dependency lúc khởi động.
- **Chạy test bỏ qua phần cần MySQL:** `./mvnw test -Dtest='!BlogServiceApplicationTests'`.
- **Hạ tầng phải chạy trước khi test tay:** `docker compose up -d`.

---

## File Structure

### Tạo mới — `blog-service/`

| File | Trách nhiệm |
|---|---|
| `pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn/**` | Maven project độc lập |
| `src/main/resources/application.properties` | Port 8083, datasource, jwt secret, storage |
| `BlogServiceApplication.java` | Entry point |
| `configuration/JwtConfig.java` | `JwtDecoder` HS256 |
| `configuration/SecurityConfig.java` | Filter chain, `PUBLIC_ENDPOINTS` phân biệt theo method |
| `configuration/SecurityExceptionHandler.java` | 401/403 trả dạng `ApiResponse` |
| `configuration/OpenApiConfig.java` | Swagger, server `/api`, `bearerAuth` |
| `configuration/S3Config.java`, `StorageProperties.java` | Client S3/MinIO cho ảnh bìa |
| `controller/CurrentUser.java` | Đọc claim `userId` |
| `controller/CategoryController.java` | `/categories` |
| `controller/BlogController.java` | `/blogs` |
| `controller/CommentController.java` | `/comments` |
| `dto/request/*`, `dto/response/*` | DTO vào/ra |
| `entity/Category.java`, `Blog.java`, `Comment.java`, `ContentVote.java` | 4 bảng |
| `enums/*` | `CategoryType`, `ContentStatus`, `CommentStatus`, `TargetType`, `ImageType` |
| `exception/AppException.java`, `ErrorCode.java`, `GlobalExceptionHandler.java` | Xử lý lỗi |
| `mapper/CategoryMapper.java`, `BlogMapper.java`, `CommentMapper.java` | MapStruct |
| `moderation/ContentModerationService.java` + `AutoApproveContentModerationService.java` + `ModerationResult.java` + `ModerationDecision.java` | Điểm cắm AI |
| `repository/*` | 4 repository JPA |
| `service/CategoryService.java`, `BlogService.java`, `CommentService.java`, `VoteService.java` | Nghiệp vụ |
| `service/FileStorageService.java`, `S3FileStorageService.java`, `ImageTypeDetector.java` | Upload ảnh bìa |

### Sửa file có sẵn

| File | Sửa gì |
|---|---|
| `api-gateway/src/main/resources/application.yaml` | 4 route + 1 mục `springdoc.swagger-ui.urls` |
| `docker-compose.yml` | Bucket `veggiepal-blog-thumbnails` trong `minio-init` |
| `identity-service/.../controller/ProfileController.java` hoặc controller mới | `GET /users/batch` |
| `identity-service/.../dto/response/PublicUserResponse.java` | DTO mới |
| `identity-service/.../repository/UserRepository.java` | Query theo danh sách id + status |
| `identity-service/.../configuration/SecurityConfig.java` | Thêm `/users/batch` vào `PUBLIC_ENDPOINTS` |
| `CLAUDE.md` | Mục blog-service |

---

## Thứ tự task

```
Task 1  Scaffold service (chạy được, contextLoads xanh)
Task 2  Plumbing dùng chung: ApiResponse, lỗi, bảo mật, Swagger
Task 3  Category: entity → repository → service → controller
Task 4  Moderation hook + Blog CRUD của chủ sở hữu
Task 5  Blog công khai: list, search, detail, related, view count
Task 6  Upload ảnh bìa
Task 7  Comment + reply
Task 8  Vote + vote_score
Task 9  identity-service GET /users/batch
Task 10 Gateway, docker-compose, CLAUDE.md
```

Task 1–2 là nền, phải làm trước. Task 3 trước Task 4 vì blog cần `category_id` hợp lệ. Task 9 và 10 độc lập với 3–8, có thể làm song song nếu cần.

---

## Task 1: Scaffold `blog-service`

**Files:**
- Create: `blog-service/pom.xml`
- Create: `blog-service/mvnw`, `blog-service/mvnw.cmd`, `blog-service/.mvn/wrapper/maven-wrapper.properties` (copy từ `nutrition-service`)
- Create: `blog-service/src/main/resources/application.properties`
- Create: `blog-service/src/main/java/com/veggiepal/blog/BlogServiceApplication.java`
- Test: `blog-service/src/test/java/com/veggiepal/blog/BlogServiceApplicationTests.java`

**Interfaces:**
- Consumes: không có
- Produces: package gốc `com.veggiepal.blog`; service chạy ở cổng `8083`; DB `veggiepal_blog`

- [ ] **Step 1: Copy Maven wrapper**

```bash
cd D:/FPT/SWP/VeggiePal/VeggiePal_BE
mkdir -p blog-service
cp -r nutrition-service/.mvn blog-service/.mvn
cp nutrition-service/mvnw blog-service/mvnw
cp nutrition-service/mvnw.cmd blog-service/mvnw.cmd
```

- [ ] **Step 2: Tạo `blog-service/pom.xml`**

Giống `nutrition-service/pom.xml`, khác `artifactId`/`name`/`description`, và **có thêm** AWS SDK v2 S3 (cho ảnh bìa ở Task 6).

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
		 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">

	<modelVersion>4.0.0</modelVersion>

	<parent>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-parent</artifactId>
		<version>4.1.1</version>
		<relativePath/>
	</parent>

	<groupId>com.veggiepal</groupId>
	<artifactId>blog-service</artifactId>
	<version>0.0.1-SNAPSHOT</version>

	<name>blog-service</name>
	<description>VeggiePal Blog Service</description>

	<properties>
		<java.version>21</java.version>
		<mapstruct.version>1.6.3</mapstruct.version>
		<aws-sdk.version>2.55.0</aws-sdk.version>
	</properties>

	<dependencies>

		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-webmvc</artifactId>
		</dependency>

		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-jpa</artifactId>
		</dependency>

		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-security</artifactId>
		</dependency>

		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-security-oauth2-resource-server</artifactId>
		</dependency>

		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-validation</artifactId>
		</dependency>

		<dependency>
			<groupId>com.mysql</groupId>
			<artifactId>mysql-connector-j</artifactId>
			<scope>runtime</scope>
		</dependency>

		<dependency>
			<groupId>org.projectlombok</groupId>
			<artifactId>lombok</artifactId>
			<optional>true</optional>
		</dependency>

		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-devtools</artifactId>
			<scope>runtime</scope>
			<optional>true</optional>
		</dependency>

		<dependency>
			<groupId>org.springdoc</groupId>
			<artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
			<version>3.1.0</version>
		</dependency>

		<dependency>
			<groupId>org.mapstruct</groupId>
			<artifactId>mapstruct</artifactId>
			<version>${mapstruct.version}</version>
		</dependency>

		<!-- S3 API: MinIO locally, AWS S3 in production -->
		<dependency>
			<groupId>software.amazon.awssdk</groupId>
			<artifactId>s3</artifactId>
		</dependency>

		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-test</artifactId>
			<scope>test</scope>
		</dependency>

		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-webmvc-test</artifactId>
			<scope>test</scope>
		</dependency>

		<dependency>
			<groupId>org.springframework.security</groupId>
			<artifactId>spring-security-test</artifactId>
			<scope>test</scope>
		</dependency>

	</dependencies>

	<dependencyManagement>
		<dependencies>
			<dependency>
				<groupId>software.amazon.awssdk</groupId>
				<artifactId>bom</artifactId>
				<version>${aws-sdk.version}</version>
				<type>pom</type>
				<scope>import</scope>
			</dependency>
		</dependencies>
	</dependencyManagement>

	<build>
		<plugins>

			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
			</plugin>

			<!-- Lombok must come before MapStruct -->
			<plugin>
				<groupId>org.apache.maven.plugins</groupId>
				<artifactId>maven-compiler-plugin</artifactId>

				<configuration>
					<annotationProcessorPaths>
						<path>
							<groupId>org.projectlombok</groupId>
							<artifactId>lombok</artifactId>
						</path>

						<path>
							<groupId>org.mapstruct</groupId>
							<artifactId>mapstruct-processor</artifactId>
							<version>${mapstruct.version}</version>
						</path>
					</annotationProcessorPaths>
				</configuration>
			</plugin>

		</plugins>
	</build>

</project>
```

- [ ] **Step 3: Tạo `blog-service/src/main/resources/application.properties`**

Đuôi **`.properties`**, giống `identity-service` và `nutrition-service`. Chỉ `api-gateway` dùng `application.yaml`, và file đó là YAML thật.

```properties
spring.application.name=blog-service
server.port=8083

spring.datasource.url=jdbc:mysql://localhost:3307/veggiepal_blog?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=12345

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.open-in-view=false

jwt.secret=${JWT_SECRET:veggiepal-secret-key-must-be-at-least-32-characters}

springdoc.swagger-ui.path=/swagger-ui.html
springdoc.api-docs.path=/v3/api-docs

storage.s3.endpoint=${S3_ENDPOINT:http://localhost:9000}
storage.s3.region=${S3_REGION:us-east-1}
storage.s3.access-key=${S3_ACCESS_KEY:minioadmin}
storage.s3.secret-key=${S3_SECRET_KEY:minioadmin}
storage.s3.bucket=${S3_BUCKET:veggiepal-blog-thumbnails}
storage.s3.public-url=${S3_PUBLIC_URL:http://localhost:9000/veggiepal-blog-thumbnails}

# Blog thumbnails are bigger than avatars
spring.servlet.multipart.max-file-size=5MB
spring.servlet.multipart.max-request-size=6MB
```

- [ ] **Step 4: Tạo entry point**

`blog-service/src/main/java/com/veggiepal/blog/BlogServiceApplication.java`:

```java
package com.veggiepal.blog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class BlogServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(BlogServiceApplication.class, args);
    }
}
```

- [ ] **Step 5: Tạo test contextLoads**

`blog-service/src/test/java/com/veggiepal/blog/BlogServiceApplicationTests.java`:

```java
package com.veggiepal.blog;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BlogServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
```

- [ ] **Step 6: Chạy build và test**

```bash
cd D:/FPT/SWP/VeggiePal/VeggiePal_BE/blog-service && ./mvnw clean test
```

Expected: PASS. Test này cần MySQL đang chạy (`docker compose up -d`) vì nó nâng cả context lên; database `veggiepal_blog` sẽ được tự tạo nhờ `createDatabaseIfNotExist=true`.

Nếu MySQL không chạy, test đỏ với `Communications link failure` — đó là môi trường thiếu, không phải code sai.

- [ ] **Step 7: Checkpoint**

Báo cáo: service scaffold xong, `contextLoads` xanh, DB `veggiepal_blog` đã được tạo. Commit với message dưới đây.

```
feat(blog): scaffold blog-service with Maven wrapper and MySQL datasource
```

---

## Task 2: Plumbing dùng chung

Copy 10 class nền từ `nutrition-service` và đổi package, cộng thêm phần bảo mật phân biệt theo method — đây là điểm khác biệt thật sự của service này.

**Files:**
- Create: `blog-service/src/main/java/com/veggiepal/blog/dto/response/ApiResponse.java`
- Create: `.../dto/response/PageResponse.java`
- Create: `.../exception/AppException.java`
- Create: `.../exception/ErrorCode.java`
- Create: `.../exception/GlobalExceptionHandler.java`
- Create: `.../controller/CurrentUser.java`
- Create: `.../configuration/JwtConfig.java`
- Create: `.../configuration/SecurityExceptionHandler.java`
- Create: `.../configuration/SecurityConfig.java`
- Create: `.../configuration/OpenApiConfig.java`
- Test: `.../configuration/SecurityConfigTest.java`, `JwtConfigTest.java`, `SecurityExceptionHandlerTest.java`
- Test: `.../exception/GlobalExceptionHandlerTest.java`

**Interfaces:**
- Consumes: Task 1 (package `com.veggiepal.blog`)
- Produces:
  - `ApiResponse.<T>builder().result(t).build()` — envelope mọi controller dùng
  - `PageResponse.<T>builder().items(List<T>).page(int).size(int).totalElements(long).totalPages(int).build()`
  - `new AppException(ErrorCode.X)`
  - `CurrentUser.id(Jwt) → Long`
  - `SecurityConfig.PUBLIC_ENDPOINTS` — danh sách `PublicEndpoint(HttpMethod, String)`

- [ ] **Step 1: Copy 6 class không cần sửa logic**

Copy từ `nutrition-service/src/main/java/com/veggiepal/nutrition/` sang `blog-service/src/main/java/com/veggiepal/blog/`, chỉ đổi `package`/`import` từ `com.veggiepal.nutrition` sang `com.veggiepal.blog`:

| Nguồn | Đích |
|---|---|
| `dto/response/ApiResponse.java` | y hệt |
| `dto/response/PageResponse.java` | y hệt |
| `exception/AppException.java` | y hệt |
| `exception/GlobalExceptionHandler.java` | y hệt, **trừ** comment về health/allergy — xem Step 2 |
| `controller/CurrentUser.java` | y hệt |
| `configuration/JwtConfig.java` | y hệt |
| `configuration/SecurityExceptionHandler.java` | y hệt |

```bash
cd D:/FPT/SWP/VeggiePal/VeggiePal_BE
for f in dto/response/ApiResponse.java dto/response/PageResponse.java exception/AppException.java exception/GlobalExceptionHandler.java controller/CurrentUser.java configuration/JwtConfig.java configuration/SecurityExceptionHandler.java; do
  mkdir -p "blog-service/src/main/java/com/veggiepal/blog/$(dirname $f)"
  sed 's/com\.veggiepal\.nutrition/com.veggiepal.blog/g' "nutrition-service/src/main/java/com/veggiepal/nutrition/$f" > "blog-service/src/main/java/com/veggiepal/blog/$f"
done
```

- [ ] **Step 2: Sửa comment trong `GlobalExceptionHandler`**

Comment ở `handlingDataIntegrityViolation` đang nói về dữ liệu sức khỏe. Ở đây lý do không log vẫn còn giá trị nhưng khác nội dung — thay bằng:

```java
    @ExceptionHandler(value = DataIntegrityViolationException.class)
    ResponseEntity<ApiResponse<?>> handlingDataIntegrityViolation(
            DataIntegrityViolationException exception
    ) {

        // Never log the exception/message here: a unique-constraint message leaks
        // which user voted on which content (e.g. "Duplicate entry '7-BLOG-12'").
        log.warn("Data integrity violation");

        return errorResponse(ErrorCode.UNCATEGORIZED_EXCEPTION);
    }
```

- [ ] **Step 3: Viết `ErrorCode` đầy đủ**

`blog-service/src/main/java/com/veggiepal/blog/exception/ErrorCode.java`. Khai báo **toàn bộ** mã ngay từ đầu, kể cả mã của task sau — như vậy các task sau không phải sửa file này và không giẫm chân nhau.

```java
package com.veggiepal.blog.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // Shared codes: keep the same numbers as identity-service and nutrition-service
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),

    INVALID_KEY(1001, "Invalid validation key", HttpStatus.BAD_REQUEST),

    UNAUTHENTICATED(1008, "Unauthenticated", HttpStatus.UNAUTHORIZED),

    UNAUTHORIZED(1009, "You do not have permission", HttpStatus.FORBIDDEN),

    FILE_UPLOAD_FAILED(1017, "Could not upload file, please try again later", HttpStatus.SERVICE_UNAVAILABLE),

    INVALID_REQUEST(1018, "Invalid request data", HttpStatus.BAD_REQUEST),

    // Category
    CATEGORY_NAME_REQUIRED(3001, "Category name is required", HttpStatus.BAD_REQUEST),

    CATEGORY_TYPE_REQUIRED(3002, "Category type is required", HttpStatus.BAD_REQUEST),

    CATEGORY_NOT_EXISTED(3003, "Category not existed", HttpStatus.NOT_FOUND),

    CATEGORY_NAME_DUPLICATED(3004, "A category with this name already exists under the same parent", HttpStatus.BAD_REQUEST),

    CATEGORY_IN_USE(3005, "Category is still used by blogs or child categories", HttpStatus.BAD_REQUEST),

    CATEGORY_DEPTH_EXCEEDED(3006, "Category tree is limited to two levels", HttpStatus.BAD_REQUEST),

    CATEGORY_INACTIVE(3007, "Category is not active", HttpStatus.BAD_REQUEST),

    CATEGORY_ID_REQUIRED(3008, "Category is required", HttpStatus.BAD_REQUEST),

    // Blog
    BLOG_TITLE_REQUIRED(3010, "Blog title is required", HttpStatus.BAD_REQUEST),

    INVALID_BLOG_TITLE(3011, "Blog title must be at most {max} characters", HttpStatus.BAD_REQUEST),

    BLOG_CONTENT_REQUIRED(3012, "Blog content is required", HttpStatus.BAD_REQUEST),

    INVALID_BLOG_CONTENT(3013, "Blog content must be at least {min} characters", HttpStatus.BAD_REQUEST),

    BLOG_NOT_EXISTED(3014, "Blog not existed", HttpStatus.NOT_FOUND),

    INVALID_BLOG_STATUS_TRANSITION(3015, "Blog is not in a state that allows this action", HttpStatus.BAD_REQUEST),

    // Thumbnail
    THUMBNAIL_REQUIRED(3020, "Thumbnail file is required", HttpStatus.BAD_REQUEST),

    INVALID_THUMBNAIL_TYPE(3021, "Thumbnail must be a JPEG, PNG or WEBP image", HttpStatus.BAD_REQUEST),

    THUMBNAIL_TOO_LARGE(3022, "Thumbnail must not exceed 5MB", HttpStatus.BAD_REQUEST),

    // Comment
    COMMENT_CONTENT_REQUIRED(3030, "Comment content is required", HttpStatus.BAD_REQUEST),

    INVALID_COMMENT_CONTENT(3031, "Comment must be at most {max} characters", HttpStatus.BAD_REQUEST),

    COMMENT_NOT_EXISTED(3032, "Comment not existed", HttpStatus.NOT_FOUND),

    COMMENT_REPLY_TOO_DEEP(3033, "Replies are limited to one level", HttpStatus.BAD_REQUEST),

    INVALID_COMMENT_PARENT(3034, "Parent comment belongs to different content", HttpStatus.BAD_REQUEST),

    COMMENT_TARGET_NOT_EXISTED(3035, "The content being commented on does not exist", HttpStatus.BAD_REQUEST),

    UNSUPPORTED_TARGET_TYPE(3036, "This content type is not supported yet", HttpStatus.BAD_REQUEST),

    // Vote
    INVALID_VOTE_VALUE(3040, "Vote value must be 1 or -1", HttpStatus.BAD_REQUEST),

    CANNOT_VOTE_OWN_CONTENT(3041, "You cannot vote on your own content", HttpStatus.BAD_REQUEST),

    BLOG_IDS_REQUIRED(3042, "Blog id list is required", HttpStatus.BAD_REQUEST);

    ErrorCode(
            int code,
            String message,
            HttpStatusCode statusCode
    ) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    final int code;

    final String message;

    final HttpStatusCode statusCode;
}
```

**Lưu ý về `{max}`:** `GlobalExceptionHandler` copy từ nutrition-service **chỉ thay được `{min}`**. `INVALID_BLOG_TITLE` và `INVALID_COMMENT_CONTENT` dùng `{max}`, nên phải mở rộng handler ở Step 4.

- [ ] **Step 4: Viết test cho việc thay `{max}` (test trước)**

`blog-service/src/test/java/com/veggiepal/blog/exception/GlobalExceptionHandlerTest.java` — thêm test này vào file đã copy:

```java
package com.veggiepal.blog.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class GlobalExceptionHandlerTest {

    GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapAttribute_replacesMinPlaceholder() {
        String result = ReflectionTestUtils.invokeMethod(
                handler, "mapAttribute",
                "Content must be at least {min} characters",
                Map.of("min", 10));

        assertThat(result).isEqualTo("Content must be at least 10 characters");
    }

    @Test
    void mapAttribute_replacesMaxPlaceholder() {
        String result = ReflectionTestUtils.invokeMethod(
                handler, "mapAttribute",
                "Title must be at most {max} characters",
                Map.of("max", 200));

        assertThat(result).isEqualTo("Title must be at most 200 characters");
    }

    @Test
    void mapAttribute_leavesMessageAloneWhenAttributeMissing() {
        String result = ReflectionTestUtils.invokeMethod(
                handler, "mapAttribute",
                "Blog title is required",
                Map.of());

        assertThat(result).isEqualTo("Blog title is required");
    }
}
```

- [ ] **Step 5: Chạy test để thấy nó đỏ**

```bash
cd D:/FPT/SWP/VeggiePal/VeggiePal_BE/blog-service && ./mvnw test -Dtest=GlobalExceptionHandlerTest
```

Expected: FAIL ở `mapAttribute_replacesMaxPlaceholder` — kết quả vẫn là `"Title must be at most {max} characters"`.

- [ ] **Step 6: Mở rộng `mapAttribute` cho cả `{min}` và `{max}`**

Trong `blog-service/.../exception/GlobalExceptionHandler.java`, thay hằng số và method:

```java
    private static final String MIN_ATTRIBUTE = "min";

    private static final String MAX_ATTRIBUTE = "max";
```

```java
    private String mapAttribute(
            String message,
            Map<String, Object> attributes
    ) {

        String mapped = message;

        for (String attribute : new String[]{MIN_ATTRIBUTE, MAX_ATTRIBUTE}) {

            Object value = attributes.get(attribute);

            if (value != null) {
                mapped = mapped.replace("{" + attribute + "}", String.valueOf(value));
            }
        }

        return mapped;
    }
```

Bản cũ gọi `String.valueOf(attributes.get("min"))` vô điều kiện, nên với constraint không có `min` nó ghi chữ `"null"` vào message. Bản này bỏ qua attribute vắng mặt.

- [ ] **Step 7: Chạy lại test**

```bash
cd D:/FPT/SWP/VeggiePal/VeggiePal_BE/blog-service && ./mvnw test -Dtest=GlobalExceptionHandlerTest
```

Expected: PASS cả 3 test.

- [ ] **Step 8: Viết `SecurityConfig` với `PUBLIC_ENDPOINTS` phân biệt theo method**

`blog-service/src/main/java/com/veggiepal/blog/configuration/SecurityConfig.java`:

```java
package com.veggiepal.blog.configuration;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SecurityConfig {

    /** A public endpoint; a null method means "any method". */
    record PublicEndpoint(HttpMethod method, String pattern) {
    }

    // The [0-9]+ constraint is load-bearing: "/blogs/me" matches a bare "/blogs/{id}",
    // which would make it public, strip its token, and then 401 forever on an
    // authenticated endpoint. Keep the digits.
    static final List<PublicEndpoint> PUBLIC_ENDPOINTS = List.of(
            new PublicEndpoint(null, "/swagger-ui/**"),
            new PublicEndpoint(null, "/swagger-ui.html"),
            new PublicEndpoint(null, "/v3/api-docs/**"),

            new PublicEndpoint(HttpMethod.GET, "/blogs"),
            new PublicEndpoint(HttpMethod.GET, "/blogs/{id:[0-9]+}"),
            new PublicEndpoint(HttpMethod.GET, "/blogs/{id:[0-9]+}/related"),

            new PublicEndpoint(HttpMethod.GET, "/categories"),
            new PublicEndpoint(HttpMethod.GET, "/categories/{id:[0-9]+}"),

            new PublicEndpoint(HttpMethod.GET, "/comments"),
            new PublicEndpoint(HttpMethod.GET, "/comments/{id:[0-9]+}/replies")
    );

    SecurityExceptionHandler securityExceptionHandler;

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity httpSecurity
    ) throws Exception {

        httpSecurity
                .authorizeHttpRequests(
                        request -> request
                                .requestMatchers(publicMatchers().toArray(RequestMatcher[]::new))
                                .permitAll()

                                .anyRequest()
                                .authenticated()
                )
                .oauth2ResourceServer(
                        oauth2 -> oauth2
                                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                                .bearerTokenResolver(publicEndpointAwareBearerTokenResolver())
                                .authenticationEntryPoint(securityExceptionHandler)
                                .accessDeniedHandler(securityExceptionHandler)
                )
                .exceptionHandling(
                        exceptions -> exceptions
                                .authenticationEntryPoint(securityExceptionHandler)
                                .accessDeniedHandler(securityExceptionHandler)
                )
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        httpSecurity.csrf(
                AbstractHttpConfigurer::disable
        );

        return httpSecurity.build();
    }

    static List<RequestMatcher> publicMatchers() {

        return PUBLIC_ENDPOINTS.stream()
                .map(SecurityConfig::toMatcher)
                .toList();
    }

    private static RequestMatcher toMatcher(PublicEndpoint endpoint) {

        return endpoint.method() == null
                ? PathPatternRequestMatcher.withDefaults().matcher(endpoint.pattern())
                : PathPatternRequestMatcher.withDefaults().matcher(endpoint.method(), endpoint.pattern());
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("role");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    // Public endpoints ignore the Authorization header, so a stale token cannot block them.
    private BearerTokenResolver publicEndpointAwareBearerTokenResolver() {

        DefaultBearerTokenResolver defaultResolver = new DefaultBearerTokenResolver();
        List<RequestMatcher> publicMatchers = publicMatchers();

        return request -> publicMatchers.stream().anyMatch(matcher -> matcher.matches(request))
                ? null
                : defaultResolver.resolve(request);
    }
}
```

- [ ] **Step 9: Viết `OpenApiConfig`**

`blog-service/src/main/java/com/veggiepal/blog/configuration/OpenApiConfig.java` — copy từ nutrition-service, đổi title và description:

```java
package com.veggiepal.blog.configuration;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI veggiePalOpenAPI() {

        return new OpenAPI()
                .info(
                        new Info()
                                .title("VeggiePal Blog Service API")
                                .version("1.0")
                                .description("Blog, category, comment and vote APIs for VeggiePal")
                )
                .servers(
                        List.of(
                                new Server()
                                        .url("/api")
                                        .description("API Gateway")
                        )
                )
                .components(
                        new Components()
                                .addSecuritySchemes(
                                        BEARER_AUTH,
                                        new SecurityScheme()
                                                .type(SecurityScheme.Type.HTTP)
                                                .scheme("bearer")
                                                .bearerFormat("JWT")
                                )
                )
                .addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
    }
}
```

- [ ] **Step 10: Viết test cho `PUBLIC_ENDPOINTS` — cái bẫy `/blogs/me`**

`blog-service/src/test/java/com/veggiepal/blog/configuration/SecurityConfigTest.java`:

```java
package com.veggiepal.blog.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;

class SecurityConfigTest {

    static boolean isPublic(String method, String uri) {

        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        request.setServletPath(uri);

        return SecurityConfig.publicMatchers().stream().anyMatch(matcher -> matcher.matches(request));
    }

    @Test
    void blogListAndDetail_arePublicForGet() {
        assertThat(isPublic("GET", "/blogs")).isTrue();
        assertThat(isPublic("GET", "/blogs/12")).isTrue();
        assertThat(isPublic("GET", "/blogs/12/related")).isTrue();
    }

    @Test
    void blogWrites_areNotPublic() {
        assertThat(isPublic("POST", "/blogs")).isFalse();
        assertThat(isPublic("PUT", "/blogs/12")).isFalse();
        assertThat(isPublic("DELETE", "/blogs/12")).isFalse();
    }

    // The whole reason /blogs/{id} is constrained to digits.
    @Test
    void ownBlogEndpoints_areNotPublic() {
        assertThat(isPublic("GET", "/blogs/me")).isFalse();
        assertThat(isPublic("GET", "/blogs/me/votes")).isFalse();
    }

    @Test
    void categoryReadsArePublic_butWritesAreNot() {
        assertThat(isPublic("GET", "/categories")).isTrue();
        assertThat(isPublic("GET", "/categories/3")).isTrue();
        assertThat(isPublic("POST", "/categories")).isFalse();
        assertThat(isPublic("DELETE", "/categories/3")).isFalse();
    }

    @Test
    void commentReadsArePublic_butWritesAreNot() {
        assertThat(isPublic("GET", "/comments")).isTrue();
        assertThat(isPublic("GET", "/comments/5/replies")).isTrue();
        assertThat(isPublic("POST", "/comments")).isFalse();
    }

    @Test
    void swaggerIsPublicForAnyMethod() {
        assertThat(isPublic("GET", "/swagger-ui.html")).isTrue();
        assertThat(isPublic("GET", "/v3/api-docs")).isTrue();
        assertThat(isPublic("GET", "/v3/api-docs/swagger-config")).isTrue();
    }

    @Test
    void publicMatchers_coversEveryDeclaredEndpoint() {
        assertThat(SecurityConfig.publicMatchers())
                .hasSameSizeAs(SecurityConfig.PUBLIC_ENDPOINTS)
                .allSatisfy(matcher -> assertThat(matcher).isInstanceOf(RequestMatcher.class));
    }
}
```

- [ ] **Step 11: Chạy test**

```bash
cd D:/FPT/SWP/VeggiePal/VeggiePal_BE/blog-service && ./mvnw test -Dtest=SecurityConfigTest
```

Expected: PASS. Nếu `ownBlogEndpoints_areNotPublic` đỏ, nghĩa là `{id:[0-9]+}` đã bị nới thành `{id}` — đọc lại comment trong `SecurityConfig`.

- [ ] **Step 12: Copy hai test còn lại**

Copy `JwtConfigTest.java` và `SecurityExceptionHandlerTest.java` từ `nutrition-service/src/test/java/com/veggiepal/nutrition/configuration/`, đổi package và import. Nội dung không cần sửa logic.

```bash
cd D:/FPT/SWP/VeggiePal/VeggiePal_BE
for f in JwtConfigTest SecurityExceptionHandlerTest; do
  sed 's/com\.veggiepal\.nutrition/com.veggiepal.blog/g' \
    "nutrition-service/src/test/java/com/veggiepal/nutrition/configuration/$f.java" \
    > "blog-service/src/test/java/com/veggiepal/blog/configuration/$f.java"
done
```

- [ ] **Step 13: Chạy toàn bộ test trừ contextLoads**

```bash
cd D:/FPT/SWP/VeggiePal/VeggiePal_BE/blog-service && ./mvnw test -Dtest='!BlogServiceApplicationTests'
```

Expected: PASS hết.

- [ ] **Step 14: Checkpoint**

```
feat(blog): add shared response envelope, error codes and JWT security config
```

---

## Task 3: Category

**Files:**
- Create: `blog-service/src/main/java/com/veggiepal/blog/enums/CategoryType.java`
- Create: `.../entity/Category.java`
- Create: `.../repository/CategoryRepository.java`
- Create: `.../dto/request/CategoryRequest.java`
- Create: `.../dto/response/CategoryResponse.java`
- Create: `.../mapper/CategoryMapper.java`
- Create: `.../service/CategoryService.java`
- Create: `.../controller/CategoryController.java`
- Test: `.../service/CategoryServiceTest.java`
- Test: `.../controller/CategoryControllerTest.java`

**Interfaces:**
- Consumes: Task 2 (`ApiResponse`, `AppException`, `ErrorCode`, `SecurityConfig`)
- Produces:
  - `CategoryType` — `FOOD_TYPE`, `RECIPE_TYPE`
  - `Category` entity — getter `getId()`, `getParent()`, `getType()`, `getName()`, `getDisplayOrder()`, `getActive()`
  - `CategoryRepository extends JpaRepository<Category, Long>`
  - `CategoryService.requireActiveCategory(Long id) → Category` — Task 4 dùng để validate `categoryId` khi tạo blog
  - `CategoryResponse` — `id`, `type`, `name`, `displayOrder`, `active`, `children`

**Phạm vi đã giới hạn có chủ ý:**
- `parentId` chỉ đặt lúc tạo. `PUT` không cho chuyển category sang cha khác — tránh phải kiểm tra lại độ sâu và vòng lặp cho một thao tác admin hiếm dùng.
- `type` chỉ khai báo ở category gốc; category con **kế thừa** `type` của cha, nên không thể có cây trộn hai loại.
- Kiểm tra "còn blog tham chiếu" khi xóa **chưa làm ở task này** vì `BlogRepository` chưa tồn tại. Task 4 Step 12 bổ sung.

- [ ] **Step 1: Viết enum và entity**

`.../enums/CategoryType.java`:

```java
package com.veggiepal.blog.enums;

public enum CategoryType {
    FOOD_TYPE,
    RECIPE_TYPE
}
```

`.../entity/Category.java`:

```java
package com.veggiepal.blog.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import com.veggiepal.blog.enums.CategoryType;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(
        name = "categories",
        indexes = {
                @Index(name = "idx_categories_parent", columnList = "parent_id, display_order"),
                @Index(name = "idx_categories_type", columnList = "type, is_active")
        }
)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    // Self-reference: excluded from toString/equals or a parent-child pair recurses forever
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Category parent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    CategoryType type;

    @Column(nullable = false, length = 100)
    String name;

    @Column(name = "display_order", nullable = false)
    Short displayOrder;

    @Column(name = "is_active", nullable = false)
    Boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

- [ ] **Step 2: Viết repository, DTO và mapper**

`.../repository/CategoryRepository.java`:

```java
package com.veggiepal.blog.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.veggiepal.blog.entity.Category;
import com.veggiepal.blog.enums.CategoryType;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByParentIsNullOrderByDisplayOrderAscIdAsc();

    /** Unordered sibling lookup, used for the duplicate-name check. */
    List<Category> findByParentIsNull();

    List<Category> findByParentIsNullAndTypeOrderByDisplayOrderAscIdAsc(CategoryType type);

    List<Category> findByParentIdInOrderByDisplayOrderAscIdAsc(Collection<Long> parentIds);

    List<Category> findByParentId(Long parentId);

    boolean existsByParentId(Long parentId);
}
```

`.../dto/request/CategoryRequest.java`:

```java
package com.veggiepal.blog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import com.veggiepal.blog.enums.CategoryType;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CategoryRequest {

    @NotBlank(message = "CATEGORY_NAME_REQUIRED")
    @Size(max = 100, message = "CATEGORY_NAME_REQUIRED")
    String name;

    /** Required for a root category; a child inherits its parent's type. */
    CategoryType type;

    /** Null creates a root category. Ignored on update. */
    Long parentId;

    Short displayOrder;

    Boolean active;
}
```

`.../dto/response/CategoryResponse.java`:

```java
package com.veggiepal.blog.dto.response;

import java.util.List;

import com.veggiepal.blog.enums.CategoryType;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CategoryResponse {

    Long id;

    Long parentId;

    CategoryType type;

    String name;

    Short displayOrder;

    Boolean active;

    List<CategoryResponse> children;
}
```

`.../mapper/CategoryMapper.java`:

```java
package com.veggiepal.blog.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.veggiepal.blog.dto.response.CategoryResponse;
import com.veggiepal.blog.entity.Category;

@Mapper(componentModel = "spring")
public interface CategoryMapper {

    @Mapping(target = "parentId", source = "parent.id")
    @Mapping(target = "children", ignore = true)
    CategoryResponse toCategoryResponse(Category category);
}
```

- [ ] **Step 3: Viết test cho `CategoryService` (test trước)**

`blog-service/src/test/java/com/veggiepal/blog/service/CategoryServiceTest.java`:

```java
package com.veggiepal.blog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.veggiepal.blog.dto.request.CategoryRequest;
import com.veggiepal.blog.dto.response.CategoryResponse;
import com.veggiepal.blog.entity.Category;
import com.veggiepal.blog.enums.CategoryType;
import com.veggiepal.blog.exception.AppException;
import com.veggiepal.blog.exception.ErrorCode;
import com.veggiepal.blog.mapper.CategoryMapper;
import com.veggiepal.blog.repository.CategoryRepository;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    CategoryRepository categoryRepository;

    @Spy
    CategoryMapper categoryMapper = Mappers.getMapper(CategoryMapper.class);

    @InjectMocks
    CategoryService categoryService;

    static Category root(Long id, String name) {
        return Category.builder()
                .id(id).name(name).type(CategoryType.RECIPE_TYPE)
                .displayOrder((short) 0).active(true)
                .build();
    }

    static Category child(Long id, String name, Category parent) {
        return Category.builder()
                .id(id).name(name).parent(parent).type(parent.getType())
                .displayOrder((short) 0).active(true)
                .build();
    }

    @Test
    void create_root_savesWithGivenTypeAndDefaults() {
        when(categoryRepository.findByParentIsNull()).thenReturn(List.of());
        when(categoryRepository.save(any(Category.class))).thenAnswer(call -> call.getArgument(0));

        categoryService.create(CategoryRequest.builder()
                .name("  Món chính  ")
                .type(CategoryType.RECIPE_TYPE)
                .build());

        ArgumentCaptor<Category> saved = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(saved.capture());

        assertThat(saved.getValue().getName()).isEqualTo("Món chính");
        assertThat(saved.getValue().getParent()).isNull();
        assertThat(saved.getValue().getType()).isEqualTo(CategoryType.RECIPE_TYPE);
        assertThat(saved.getValue().getDisplayOrder()).isZero();
        assertThat(saved.getValue().getActive()).isTrue();
    }

    @Test
    void create_rootWithoutType_throwsTypeRequired() {
        assertThatThrownBy(() -> categoryService.create(
                CategoryRequest.builder().name("Món chính").build()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CATEGORY_TYPE_REQUIRED);

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void create_child_inheritsParentTypeEvenWhenRequestSaysOtherwise() {
        Category parent = root(1L, "Công thức");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(categoryRepository.findByParentId(1L)).thenReturn(List.of());
        when(categoryRepository.save(any(Category.class))).thenAnswer(call -> call.getArgument(0));

        categoryService.create(CategoryRequest.builder()
                .name("Món phụ")
                .parentId(1L)
                .type(CategoryType.FOOD_TYPE)
                .build());

        ArgumentCaptor<Category> saved = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(saved.capture());

        assertThat(saved.getValue().getType()).isEqualTo(CategoryType.RECIPE_TYPE);
    }

    @Test
    void create_grandChild_throwsDepthExceeded() {
        Category parent = root(1L, "Công thức");
        Category level2 = child(2L, "Món chính", parent);
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(level2));

        assertThatThrownBy(() -> categoryService.create(
                CategoryRequest.builder().name("Món xào").parentId(2L).build()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CATEGORY_DEPTH_EXCEEDED);
    }

    @Test
    void create_duplicateNameUnderSameParent_throwsDuplicated() {
        Category parent = root(1L, "Công thức");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(parent));
        when(categoryRepository.findByParentId(1L)).thenReturn(List.of(child(2L, "Món chính", parent)));

        assertThatThrownBy(() -> categoryService.create(
                CategoryRequest.builder().name("  món chính ").parentId(1L).build()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CATEGORY_NAME_DUPLICATED);
    }

    // MySQL treats each NULL as distinct, so a unique(parent_id, name) index would
    // NOT stop two root categories sharing a name. The service has to.
    @Test
    void create_duplicateRootName_throwsDuplicated() {
        when(categoryRepository.findByParentIsNull()).thenReturn(List.of(root(1L, "Công thức")));

        assertThatThrownBy(() -> categoryService.create(
                CategoryRequest.builder().name("CÔNG THỨC").type(CategoryType.RECIPE_TYPE).build()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CATEGORY_NAME_DUPLICATED);
    }

    @Test
    void update_sameCategoryKeepsItsOwnName_isAllowed() {
        Category parent = root(1L, "Công thức");
        Category target = child(2L, "Món chính", parent);
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(target));
        when(categoryRepository.findByParentId(1L)).thenReturn(List.of(target));
        when(categoryRepository.save(any(Category.class))).thenAnswer(call -> call.getArgument(0));

        categoryService.update(2L, CategoryRequest.builder()
                .name("Món chính").displayOrder((short) 5).active(false).build());

        assertThat(target.getDisplayOrder()).isEqualTo((short) 5);
        assertThat(target.getActive()).isFalse();
    }

    @Test
    void delete_withChildren_throwsInUse() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(root(1L, "Công thức")));
        when(categoryRepository.existsByParentId(1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.delete(1L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CATEGORY_IN_USE);

        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void delete_leafCategory_deletes() {
        Category leaf = root(1L, "Công thức");
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(leaf));
        when(categoryRepository.existsByParentId(1L)).thenReturn(false);

        categoryService.delete(1L);

        verify(categoryRepository).delete(leaf);
    }

    @Test
    void requireActiveCategory_inactive_throwsCategoryInactive() {
        Category inactive = root(1L, "Công thức");
        inactive.setActive(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> categoryService.requireActiveCategory(1L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CATEGORY_INACTIVE);
    }

    @Test
    void requireActiveCategory_missing_throwsNotExisted() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.requireActiveCategory(99L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CATEGORY_NOT_EXISTED);
    }

    @Test
    void getTree_nestsChildrenUnderRoots() {
        Category parent = root(1L, "Công thức");
        when(categoryRepository.findByParentIsNullAndTypeOrderByDisplayOrderAscIdAsc(CategoryType.RECIPE_TYPE))
                .thenReturn(List.of(parent));
        when(categoryRepository.findByParentIdInOrderByDisplayOrderAscIdAsc(List.of(1L)))
                .thenReturn(List.of(child(2L, "Món chính", parent), child(3L, "Món phụ", parent)));

        List<CategoryResponse> tree = categoryService.getTree(CategoryType.RECIPE_TYPE, true);

        assertThat(tree).hasSize(1);
        assertThat(tree.getFirst().getChildren())
                .extracting(CategoryResponse::getName)
                .containsExactly("Món chính", "Món phụ");
    }

    @Test
    void getTree_activeOnly_dropsInactiveChildren() {
        Category parent = root(1L, "Công thức");
        Category hidden = child(3L, "Món phụ", parent);
        hidden.setActive(false);

        when(categoryRepository.findByParentIsNullAndTypeOrderByDisplayOrderAscIdAsc(CategoryType.RECIPE_TYPE))
                .thenReturn(List.of(parent));
        when(categoryRepository.findByParentIdInOrderByDisplayOrderAscIdAsc(List.of(1L)))
                .thenReturn(List.of(child(2L, "Món chính", parent), hidden));

        List<CategoryResponse> tree = categoryService.getTree(CategoryType.RECIPE_TYPE, true);

        assertThat(tree.getFirst().getChildren())
                .extracting(CategoryResponse::getName)
                .containsExactly("Món chính");
    }
}
```

- [ ] **Step 4: Chạy test để thấy nó đỏ**

```bash
cd D:/FPT/SWP/VeggiePal/VeggiePal_BE/blog-service && ./mvnw test -Dtest=CategoryServiceTest
```

Expected: FAIL khi compile — `CategoryService` chưa tồn tại.

- [ ] **Step 5: Viết `CategoryService`**

`.../service/CategoryService.java`:

```java
package com.veggiepal.blog.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veggiepal.blog.dto.request.CategoryRequest;
import com.veggiepal.blog.dto.response.CategoryResponse;
import com.veggiepal.blog.entity.Category;
import com.veggiepal.blog.enums.CategoryType;
import com.veggiepal.blog.exception.AppException;
import com.veggiepal.blog.exception.ErrorCode;
import com.veggiepal.blog.mapper.CategoryMapper;
import com.veggiepal.blog.repository.CategoryRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CategoryService {

    CategoryRepository categoryRepository;
    CategoryMapper categoryMapper;

    public List<CategoryResponse> getTree(CategoryType type, boolean activeOnly) {

        List<Category> roots = type == null
                ? categoryRepository.findByParentIsNullOrderByDisplayOrderAscIdAsc()
                : categoryRepository.findByParentIsNullAndTypeOrderByDisplayOrderAscIdAsc(type);

        if (activeOnly) {
            roots = roots.stream().filter(Category::getActive).toList();
        }

        if (roots.isEmpty()) {
            return List.of();
        }

        List<Long> rootIds = roots.stream().map(Category::getId).toList();

        Map<Long, List<Category>> childrenByParent =
                categoryRepository.findByParentIdInOrderByDisplayOrderAscIdAsc(rootIds).stream()
                        .filter(child -> !activeOnly || child.getActive())
                        .collect(Collectors.groupingBy(child -> child.getParent().getId()));

        return roots.stream()
                .map(root -> {
                    CategoryResponse response = categoryMapper.toCategoryResponse(root);
                    response.setChildren(
                            childrenByParent.getOrDefault(root.getId(), List.of()).stream()
                                    .map(categoryMapper::toCategoryResponse)
                                    .toList()
                    );
                    return response;
                })
                .toList();
    }

    public CategoryResponse getById(Long id) {

        return categoryMapper.toCategoryResponse(findCategory(id));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {

        String name = request.getName().trim();

        Category parent = null;
        CategoryType type;

        if (request.getParentId() == null) {

            if (request.getType() == null) {
                throw new AppException(ErrorCode.CATEGORY_TYPE_REQUIRED);
            }

            type = request.getType();
            requireNameFree(categoryRepository.findByParentIsNull(), name, null);

        } else {

            parent = findCategory(request.getParentId());

            // Two levels only: a category that already has a parent cannot become one
            if (parent.getParent() != null) {
                throw new AppException(ErrorCode.CATEGORY_DEPTH_EXCEEDED);
            }

            // A child always follows its parent, so one tree cannot mix both types
            type = parent.getType();
            requireNameFree(categoryRepository.findByParentId(parent.getId()), name, null);
        }

        Category category = Category.builder()
                .parent(parent)
                .type(type)
                .name(name)
                .displayOrder(request.getDisplayOrder() == null ? (short) 0 : request.getDisplayOrder())
                .active(request.getActive() == null || request.getActive())
                .build();

        return categoryMapper.toCategoryResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {

        Category category = findCategory(id);
        String name = request.getName().trim();

        List<Category> siblings = category.getParent() == null
                ? categoryRepository.findByParentIsNull()
                : categoryRepository.findByParentId(category.getParent().getId());

        requireNameFree(siblings, name, id);

        category.setName(name);

        if (request.getDisplayOrder() != null) {
            category.setDisplayOrder(request.getDisplayOrder());
        }

        if (request.getActive() != null) {
            category.setActive(request.getActive());
        }

        return categoryMapper.toCategoryResponse(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id) {

        Category category = findCategory(id);

        if (categoryRepository.existsByParentId(id)) {
            throw new AppException(ErrorCode.CATEGORY_IN_USE);
        }

        categoryRepository.delete(category);
    }

    /** Used by BlogService: the category must exist and still be selectable. */
    public Category requireActiveCategory(Long id) {

        Category category = findCategory(id);

        if (!category.getActive()) {
            throw new AppException(ErrorCode.CATEGORY_INACTIVE);
        }

        return category;
    }

    private Category findCategory(Long id) {

        return categoryRepository
                .findById(id)
                .orElseThrow(
                        () -> new AppException(
                                ErrorCode.CATEGORY_NOT_EXISTED
                        )
                );
    }

    private void requireNameFree(List<Category> siblings, String name, Long excludedId) {

        boolean taken = siblings.stream()
                .filter(sibling -> excludedId == null || !excludedId.equals(sibling.getId()))
                .anyMatch(sibling -> sibling.getName().equalsIgnoreCase(name));

        if (taken) {
            throw new AppException(ErrorCode.CATEGORY_NAME_DUPLICATED);
        }
    }
}
```

- [ ] **Step 6: Chạy test**

```bash
cd D:/FPT/SWP/VeggiePal/VeggiePal_BE/blog-service && ./mvnw test -Dtest=CategoryServiceTest
```

Expected: PASS cả 12 test.

- [ ] **Step 7: Viết `CategoryController`**

`.../controller/CategoryController.java`:

```java
package com.veggiepal.blog.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.veggiepal.blog.dto.request.CategoryRequest;
import com.veggiepal.blog.dto.response.ApiResponse;
import com.veggiepal.blog.dto.response.CategoryResponse;
import com.veggiepal.blog.enums.CategoryType;
import com.veggiepal.blog.service.CategoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Category", description = "Content categories by food type and recipe type")
public class CategoryController {

    CategoryService categoryService;

    @Operation(summary = "Category tree, two levels deep")
    @GetMapping
    ApiResponse<List<CategoryResponse>> getTree(
            @RequestParam(name = "type", required = false) CategoryType type,
            @RequestParam(name = "activeOnly", defaultValue = "true") boolean activeOnly
    ) {

        return ApiResponse
                .<List<CategoryResponse>>builder()
                .result(categoryService.getTree(type, activeOnly))
                .build();
    }

    @Operation(summary = "One category")
    @GetMapping("/{id}")
    ApiResponse<CategoryResponse> getById(
            @PathVariable("id") Long id
    ) {

        return ApiResponse
                .<CategoryResponse>builder()
                .result(categoryService.getById(id))
                .build();
    }

    @Operation(summary = "Create a category (admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    ApiResponse<CategoryResponse> create(
            @RequestBody @Valid CategoryRequest request
    ) {

        return ApiResponse
                .<CategoryResponse>builder()
                .result(categoryService.create(request))
                .build();
    }

    @Operation(summary = "Rename, reorder or hide a category (admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    ApiResponse<CategoryResponse> update(
            @PathVariable("id") Long id,
            @RequestBody @Valid CategoryRequest request
    ) {

        return ApiResponse
                .<CategoryResponse>builder()
                .result(categoryService.update(id, request))
                .build();
    }

    @Operation(summary = "Delete a category that nothing uses (admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    ApiResponse<Void> delete(
            @PathVariable("id") Long id
    ) {

        categoryService.delete(id);
        return ApiResponse.<Void>builder().build();
    }
}
```

- [ ] **Step 8: Viết controller slice test**

`blog-service/src/test/java/com/veggiepal/blog/controller/CategoryControllerTest.java`:

```java
package com.veggiepal.blog.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.veggiepal.blog.configuration.JwtConfig;
import com.veggiepal.blog.configuration.SecurityConfig;
import com.veggiepal.blog.configuration.SecurityExceptionHandler;
import com.veggiepal.blog.dto.request.CategoryRequest;
import com.veggiepal.blog.dto.response.CategoryResponse;
import com.veggiepal.blog.enums.CategoryType;
import com.veggiepal.blog.service.CategoryService;

@WebMvcTest(CategoryController.class)
@Import({SecurityConfig.class, JwtConfig.class, SecurityExceptionHandler.class})
class CategoryControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CategoryService categoryService;

    static RequestPostProcessor member() {
        return jwt().jwt(token -> token.claim("userId", 7L).claim("role", "USER"));
    }

    static RequestPostProcessor admin() {
        return jwt().jwt(token -> token.claim("userId", 1L).claim("role", "ADMIN"));
    }

    @Test
    void getTree_withoutToken_isPublic() throws Exception {
        when(categoryService.getTree(null, true)).thenReturn(List.of(
                CategoryResponse.builder().id(1L).name("Công thức").type(CategoryType.RECIPE_TYPE).build()));

        mockMvc.perform(get("/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result[0].name").value("Công thức"));
    }

    @Test
    void create_withoutToken_returnsUnauthenticated() throws Exception {
        mockMvc.perform(post("/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Công thức", "type": "RECIPE_TYPE"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1008));

        verify(categoryService, never()).create(any());
    }

    @Test
    void create_asMember_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/categories").with(member())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Công thức", "type": "RECIPE_TYPE"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1009));

        verify(categoryService, never()).create(any());
    }

    @Test
    void create_asAdmin_succeeds() throws Exception {
        when(categoryService.create(any(CategoryRequest.class)))
                .thenReturn(CategoryResponse.builder().id(1L).name("Công thức").build());

        mockMvc.perform(post("/categories").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Công thức", "type": "RECIPE_TYPE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(1));
    }

    @Test
    void create_blankName_returnsCategoryNameRequired() throws Exception {
        mockMvc.perform(post("/categories").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "   ", "type": "RECIPE_TYPE"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(3001));
    }

    @Test
    void delete_asAdmin_succeeds() throws Exception {
        mockMvc.perform(delete("/categories/3").with(admin()))
                .andExpect(status().isOk());

        verify(categoryService).delete(3L);
    }
}
```

- [ ] **Step 9: Chạy test**

```bash
cd D:/FPT/SWP/VeggiePal/VeggiePal_BE/blog-service && ./mvnw test -Dtest='CategoryServiceTest,CategoryControllerTest'
```

Expected: PASS.

Nếu `create_asMember_returnsUnauthorized` trả 200 thay vì 403, nguyên nhân gần như chắc chắn là thiếu `@EnableMethodSecurity` trên `SecurityConfig` — `@PreAuthorize` không có nó thì bị bỏ qua im lặng.

- [ ] **Step 10: Checkpoint**

```
feat(blog): add two-level category tree with admin-only writes
```

---

## Task 4: Moderation hook + Blog CRUD của chủ sở hữu

**Files:**
- Create: `.../enums/ContentStatus.java`
- Create: `.../moderation/ModerationDecision.java`, `ModerationResult.java`, `ContentModerationService.java`, `AutoApproveContentModerationService.java`
- Create: `.../entity/Blog.java`
- Create: `.../repository/BlogRepository.java`
- Create: `.../dto/request/BlogRequest.java`
- Create: `.../dto/response/BlogResponse.java`, `BlogSummaryResponse.java`
- Create: `.../mapper/BlogMapper.java`
- Create: `.../service/BlogService.java`
- Create: `.../controller/BlogController.java`
- Modify: `.../service/CategoryService.java` (thêm check blog khi xóa)
- Test: `.../service/BlogServiceTest.java`, `.../controller/BlogControllerTest.java`
- Test: `.../service/CategoryServiceTest.java` (thêm 1 test)

**Interfaces:**
- Consumes: Task 3 (`CategoryService.requireActiveCategory(Long) → Category`)
- Produces:
  - `ContentStatus` — `DRAFT`, `PENDING`, `PUBLISHED`, `REJECTED`
  - `ContentModerationService.moderate(String) → ModerationResult`
  - `ModerationResult(ModerationDecision decision, String reason)`
  - `Blog` entity — `getId()`, `getAuthorId()`, `getCategory()`, `getStatus()`, `getVoteScore()`, `getPublishedAt()`
  - `BlogRepository extends JpaRepository<Blog, Long>` với `existsByCategoryId(Long)`
  - `BlogService.requirePublishedBlog(Long) → Blog` — Task 7, 8 dùng
  - `BlogService.createBlog`, `updateBlog`, `deleteBlog`, `submitBlog`, `getOwnBlogs`

- [ ] **Step 1: Viết enum và điểm cắm moderation**

`.../enums/ContentStatus.java`:

```java
package com.veggiepal.blog.enums;

public enum ContentStatus {
    DRAFT,
    PENDING,
    PUBLISHED,
    REJECTED
}
```

`.../moderation/ModerationDecision.java`:

```java
package com.veggiepal.blog.moderation;

public enum ModerationDecision {
    APPROVED,
    REJECTED,
    /** The AI implementation may need to answer later; content waits instead of going public. */
    PENDING
}
```

`.../moderation/ModerationResult.java`:

```java
package com.veggiepal.blog.moderation;

public record ModerationResult(ModerationDecision decision, String reason) {

    public static ModerationResult approved() {
        return new ModerationResult(ModerationDecision.APPROVED, null);
    }
}
```

`.../moderation/ContentModerationService.java`:

```java
package com.veggiepal.blog.moderation;

/**
 * BR-02: user content goes through moderation before it becomes public.
 *
 * <p>The AI service does not exist yet, so {@link AutoApproveContentModerationService}
 * stands in. Replacing it is the whole extension point: no caller changes.
 */
public interface ContentModerationService {

    ModerationResult moderate(String text);
}
```

`.../moderation/AutoApproveContentModerationService.java`:

```java
package com.veggiepal.blog.moderation;

import org.springframework.stereotype.Service;

/** Placeholder until the AI moderation service exists. Approves everything. */
@Service
public class AutoApproveContentModerationService implements ContentModerationService {

    @Override
    public ModerationResult moderate(String text) {
        return ModerationResult.approved();
    }
}
```

- [ ] **Step 2: Viết entity `Blog` và repository**

`.../entity/Blog.java`:

```java
package com.veggiepal.blog.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import com.veggiepal.blog.enums.ContentStatus;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(
        name = "blogs",
        indexes = {
                @Index(name = "idx_blogs_status_published", columnList = "status, published_at"),
                @Index(name = "idx_blogs_author", columnList = "author_id, created_at"),
                @Index(name = "idx_blogs_category", columnList = "category_id, status")
        }
)
public class Blog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    // From the JWT userId claim. No FK: users live in another service's database.
    @Column(name = "author_id", nullable = false)
    Long authorId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Category category;

    @Column(nullable = false, length = 200)
    String title;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    String content;

    @Column(name = "thumbnail_url", length = 512)
    String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    ContentStatus status;

    @Column(name = "view_count", nullable = false)
    Integer viewCount;

    @Column(name = "vote_score", nullable = false)
    Integer voteScore;

    @Column(name = "published_at")
    LocalDateTime publishedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();

        if (viewCount == null) {
            viewCount = 0;
        }

        if (voteScore == null) {
            voteScore = 0;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

`.../repository/BlogRepository.java` — phần dùng cho task này; Task 5 và 8 bổ sung thêm:

```java
package com.veggiepal.blog.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.veggiepal.blog.entity.Blog;
import com.veggiepal.blog.enums.ContentStatus;

@Repository
public interface BlogRepository extends JpaRepository<Blog, Long> {

    Page<Blog> findByAuthorId(Long authorId, Pageable pageable);

    Page<Blog> findByAuthorIdAndStatus(Long authorId, ContentStatus status, Pageable pageable);

    Optional<Blog> findByIdAndStatus(Long id, ContentStatus status);

    boolean existsByCategoryId(Long categoryId);
}
```

- [ ] **Step 3: Viết DTO và mapper**

`.../dto/request/BlogRequest.java`:

```java
package com.veggiepal.blog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlogRequest {

    @NotBlank(message = "BLOG_TITLE_REQUIRED")
    @Size(max = 200, message = "INVALID_BLOG_TITLE")
    String title;

    @NotBlank(message = "BLOG_CONTENT_REQUIRED")
    @Size(min = 20, message = "INVALID_BLOG_CONTENT")
    String content;

    // Not CATEGORY_NOT_EXISTED: that code carries HTTP 404, and a missing field
    // in the request body is a 400. The handler takes the status from the code.
    @NotNull(message = "CATEGORY_ID_REQUIRED")
    Long categoryId;

    /**
     * false (or absent) saves a draft. true runs moderation right away.
     * The client never sends a status: that would be a way around BR-02.
     */
    Boolean publish;
}
```

`.../dto/response/BlogResponse.java`:

```java
package com.veggiepal.blog.dto.response;

import java.time.LocalDateTime;

import com.veggiepal.blog.enums.ContentStatus;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlogResponse {

    Long id;

    Long authorId;

    Long categoryId;

    String categoryName;

    String title;

    String content;

    String thumbnailUrl;

    ContentStatus status;

    Integer viewCount;

    Integer voteScore;

    LocalDateTime publishedAt;

    LocalDateTime createdAt;

    LocalDateTime updatedAt;

    /** Only filled when moderation rejected or deferred the content. */
    String moderationReason;
}
```

`.../dto/response/BlogSummaryResponse.java` — dùng cho list, bỏ `content` để payload không phình:

```java
package com.veggiepal.blog.dto.response;

import java.time.LocalDateTime;

import com.veggiepal.blog.enums.ContentStatus;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlogSummaryResponse {

    Long id;

    Long authorId;

    Long categoryId;

    String categoryName;

    String title;

    String thumbnailUrl;

    ContentStatus status;

    Integer viewCount;

    Integer voteScore;

    LocalDateTime publishedAt;

    LocalDateTime createdAt;
}
```

`.../mapper/BlogMapper.java`:

```java
package com.veggiepal.blog.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.veggiepal.blog.dto.response.BlogResponse;
import com.veggiepal.blog.dto.response.BlogSummaryResponse;
import com.veggiepal.blog.entity.Blog;

@Mapper(componentModel = "spring")
public interface BlogMapper {

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "moderationReason", ignore = true)
    BlogResponse toBlogResponse(Blog blog);

    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    BlogSummaryResponse toBlogSummaryResponse(Blog blog);
}
```

- [ ] **Step 4: Viết test cho `BlogService` (test trước)**

`blog-service/src/test/java/com/veggiepal/blog/service/BlogServiceTest.java`:

```java
package com.veggiepal.blog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mapstruct.factory.Mappers;
import org.mockito.junit.jupiter.MockitoExtension;

import com.veggiepal.blog.dto.request.BlogRequest;
import com.veggiepal.blog.dto.response.BlogResponse;
import com.veggiepal.blog.entity.Blog;
import com.veggiepal.blog.entity.Category;
import com.veggiepal.blog.enums.CategoryType;
import com.veggiepal.blog.enums.ContentStatus;
import com.veggiepal.blog.exception.AppException;
import com.veggiepal.blog.exception.ErrorCode;
import com.veggiepal.blog.mapper.BlogMapper;
import com.veggiepal.blog.moderation.ContentModerationService;
import com.veggiepal.blog.moderation.ModerationDecision;
import com.veggiepal.blog.moderation.ModerationResult;
import com.veggiepal.blog.repository.BlogRepository;

@ExtendWith(MockitoExtension.class)
class BlogServiceTest {

    static final Long AUTHOR_ID = 7L;
    static final Long STRANGER_ID = 8L;
    static final Long ADMIN_ID = 1L;

    @Mock
    BlogRepository blogRepository;

    @Mock
    CategoryService categoryService;

    @Mock
    ContentModerationService contentModerationService;

    @Spy
    BlogMapper blogMapper = Mappers.getMapper(BlogMapper.class);

    @InjectMocks
    BlogService blogService;

    static Category category() {
        return Category.builder()
                .id(3L).name("Món chính").type(CategoryType.RECIPE_TYPE)
                .displayOrder((short) 0).active(true)
                .build();
    }

    static Blog blog(ContentStatus status) {
        return Blog.builder()
                .id(10L).authorId(AUTHOR_ID).category(category())
                .title("Đậu hũ sốt cà").content("x".repeat(50))
                .status(status).viewCount(0).voteScore(0)
                .build();
    }

    static BlogRequest request(boolean publish) {
        return BlogRequest.builder()
                .title("Đậu hũ sốt cà").content("x".repeat(50))
                .categoryId(3L).publish(publish)
                .build();
    }

    @Test
    void createBlog_publishFalse_savesDraftAndSkipsModeration() {
        when(categoryService.requireActiveCategory(3L)).thenReturn(category());
        when(blogRepository.save(any(Blog.class))).thenAnswer(call -> call.getArgument(0));

        BlogResponse response = blogService.createBlog(AUTHOR_ID, request(false));

        assertThat(response.getStatus()).isEqualTo(ContentStatus.DRAFT);
        assertThat(response.getPublishedAt()).isNull();
        verify(contentModerationService, never()).moderate(anyString());
    }

    @Test
    void createBlog_publishTrueAndApproved_publishesAndStampsPublishedAt() {
        when(categoryService.requireActiveCategory(3L)).thenReturn(category());
        when(contentModerationService.moderate(anyString())).thenReturn(ModerationResult.approved());
        when(blogRepository.save(any(Blog.class))).thenAnswer(call -> call.getArgument(0));

        BlogResponse response = blogService.createBlog(AUTHOR_ID, request(true));

        assertThat(response.getStatus()).isEqualTo(ContentStatus.PUBLISHED);
        assertThat(response.getPublishedAt()).isNotNull();
    }

    @Test
    void createBlog_publishTrueButRejected_returnsRejectedWithReason() {
        when(categoryService.requireActiveCategory(3L)).thenReturn(category());
        when(contentModerationService.moderate(anyString()))
                .thenReturn(new ModerationResult(ModerationDecision.REJECTED, "spam"));
        when(blogRepository.save(any(Blog.class))).thenAnswer(call -> call.getArgument(0));

        BlogResponse response = blogService.createBlog(AUTHOR_ID, request(true));

        assertThat(response.getStatus()).isEqualTo(ContentStatus.REJECTED);
        assertThat(response.getModerationReason()).isEqualTo("spam");
        assertThat(response.getPublishedAt()).isNull();
    }

    @Test
    void createBlog_moderationPending_holdsContentBackFromPublic() {
        when(categoryService.requireActiveCategory(3L)).thenReturn(category());
        when(contentModerationService.moderate(anyString()))
                .thenReturn(new ModerationResult(ModerationDecision.PENDING, "queued"));
        when(blogRepository.save(any(Blog.class))).thenAnswer(call -> call.getArgument(0));

        BlogResponse response = blogService.createBlog(AUTHOR_ID, request(true));

        assertThat(response.getStatus()).isEqualTo(ContentStatus.PENDING);
        assertThat(response.getPublishedAt()).isNull();
    }

    @Test
    void createBlog_inactiveCategory_propagatesCategoryInactive() {
        when(categoryService.requireActiveCategory(3L))
                .thenThrow(new AppException(ErrorCode.CATEGORY_INACTIVE));

        assertThatThrownBy(() -> blogService.createBlog(AUTHOR_ID, request(false)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CATEGORY_INACTIVE);

        verify(blogRepository, never()).save(any());
    }

    // BR-02: editing something already public sends it back through moderation
    @Test
    void updateBlog_onPublished_reModerates() {
        Blog existing = blog(ContentStatus.PUBLISHED);
        existing.setPublishedAt(LocalDateTime.now().minusDays(3));

        when(blogRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(categoryService.requireActiveCategory(3L)).thenReturn(category());
        when(contentModerationService.moderate(anyString())).thenReturn(ModerationResult.approved());
        when(blogRepository.save(any(Blog.class))).thenAnswer(call -> call.getArgument(0));

        BlogResponse response = blogService.updateBlog(AUTHOR_ID, false, 10L, request(true));

        assertThat(response.getStatus()).isEqualTo(ContentStatus.PUBLISHED);
        verify(contentModerationService).moderate(anyString());
    }

    @Test
    void updateBlog_onDraft_staysDraftWithoutModeration() {
        when(blogRepository.findById(10L)).thenReturn(Optional.of(blog(ContentStatus.DRAFT)));
        when(categoryService.requireActiveCategory(3L)).thenReturn(category());
        when(blogRepository.save(any(Blog.class))).thenAnswer(call -> call.getArgument(0));

        BlogResponse response = blogService.updateBlog(AUTHOR_ID, false, 10L, request(false));

        assertThat(response.getStatus()).isEqualTo(ContentStatus.DRAFT);
        verify(contentModerationService, never()).moderate(anyString());
    }

    // How a member fixes a rejected post and tries again
    @Test
    void updateBlog_onRejected_reModeratesAndCanBecomePublished() {
        when(blogRepository.findById(10L)).thenReturn(Optional.of(blog(ContentStatus.REJECTED)));
        when(categoryService.requireActiveCategory(3L)).thenReturn(category());
        when(contentModerationService.moderate(anyString())).thenReturn(ModerationResult.approved());
        when(blogRepository.save(any(Blog.class))).thenAnswer(call -> call.getArgument(0));

        BlogResponse response = blogService.updateBlog(AUTHOR_ID, false, 10L, request(true));

        assertThat(response.getStatus()).isEqualTo(ContentStatus.PUBLISHED);
        assertThat(response.getPublishedAt()).isNotNull();
    }

    @Test
    void updateBlog_republished_keepsOriginalPublishedAt() {
        LocalDateTime original = LocalDateTime.now().minusDays(3);
        Blog existing = blog(ContentStatus.PUBLISHED);
        existing.setPublishedAt(original);

        when(blogRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(categoryService.requireActiveCategory(3L)).thenReturn(category());
        when(contentModerationService.moderate(anyString())).thenReturn(ModerationResult.approved());
        when(blogRepository.save(any(Blog.class))).thenAnswer(call -> call.getArgument(0));

        blogService.updateBlog(AUTHOR_ID, false, 10L, request(true));

        assertThat(existing.getPublishedAt()).isEqualTo(original);
    }

    @Test
    void updateBlog_byStranger_throwsUnauthorized() {
        when(blogRepository.findById(10L)).thenReturn(Optional.of(blog(ContentStatus.DRAFT)));

        assertThatThrownBy(() -> blogService.updateBlog(STRANGER_ID, false, 10L, request(false)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);

        verify(blogRepository, never()).save(any());
    }

    // BR-07 / FR-10-04: an admin may take down someone else's post
    @Test
    void deleteBlog_byAdmin_deletesSomeoneElsesPost() {
        Blog existing = blog(ContentStatus.PUBLISHED);
        when(blogRepository.findById(10L)).thenReturn(Optional.of(existing));

        blogService.deleteBlog(ADMIN_ID, true, 10L);

        verify(blogRepository).delete(existing);
    }

    @Test
    void deleteBlog_byStranger_throwsUnauthorized() {
        when(blogRepository.findById(10L)).thenReturn(Optional.of(blog(ContentStatus.PUBLISHED)));

        assertThatThrownBy(() -> blogService.deleteBlog(STRANGER_ID, false, 10L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);

        verify(blogRepository, never()).delete(any());
    }

    @Test
    void submitBlog_fromDraft_runsModeration() {
        when(blogRepository.findById(10L)).thenReturn(Optional.of(blog(ContentStatus.DRAFT)));
        when(contentModerationService.moderate(anyString())).thenReturn(ModerationResult.approved());
        when(blogRepository.save(any(Blog.class))).thenAnswer(call -> call.getArgument(0));

        BlogResponse response = blogService.submitBlog(AUTHOR_ID, 10L);

        assertThat(response.getStatus()).isEqualTo(ContentStatus.PUBLISHED);
    }

    @Test
    void submitBlog_whenAlreadyPublished_throwsInvalidTransition() {
        when(blogRepository.findById(10L)).thenReturn(Optional.of(blog(ContentStatus.PUBLISHED)));

        assertThatThrownBy(() -> blogService.submitBlog(AUTHOR_ID, 10L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_BLOG_STATUS_TRANSITION);
    }

    @Test
    void requirePublishedBlog_draft_looksExactlyLikeMissing() {
        when(blogRepository.findByIdAndStatus(10L, ContentStatus.PUBLISHED)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> blogService.requirePublishedBlog(10L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.BLOG_NOT_EXISTED);
    }
}
```

- [ ] **Step 5: Chạy test để thấy nó đỏ**

```bash
cd D:/FPT/SWP/VeggiePal/VeggiePal_BE/blog-service && ./mvnw test -Dtest=BlogServiceTest
```

Expected: FAIL khi compile — `BlogService` chưa tồn tại.

- [ ] **Step 6: Viết `BlogService`**

`.../service/BlogService.java` — phần cho task này. Task 5 sẽ thêm list/search/detail, Task 6 thêm thumbnail.

```java
package com.veggiepal.blog.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veggiepal.blog.dto.request.BlogRequest;
import com.veggiepal.blog.dto.response.BlogResponse;
import com.veggiepal.blog.dto.response.BlogSummaryResponse;
import com.veggiepal.blog.dto.response.PageResponse;
import com.veggiepal.blog.entity.Blog;
import com.veggiepal.blog.enums.ContentStatus;
import com.veggiepal.blog.exception.AppException;
import com.veggiepal.blog.exception.ErrorCode;
import com.veggiepal.blog.mapper.BlogMapper;
import com.veggiepal.blog.moderation.ContentModerationService;
import com.veggiepal.blog.moderation.ModerationResult;
import com.veggiepal.blog.repository.BlogRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BlogService {

    static final int MAX_PAGE_SIZE = 100;

    static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    BlogRepository blogRepository;
    CategoryService categoryService;
    ContentModerationService contentModerationService;
    BlogMapper blogMapper;

    @Transactional
    public BlogResponse createBlog(Long authorId, BlogRequest request) {

        Blog blog = Blog.builder()
                .authorId(authorId)
                .category(categoryService.requireActiveCategory(request.getCategoryId()))
                .title(request.getTitle().trim())
                .content(request.getContent())
                .status(ContentStatus.DRAFT)
                .viewCount(0)
                .voteScore(0)
                .build();

        String reason = Boolean.TRUE.equals(request.getPublish())
                ? applyModeration(blog)
                : null;

        blogRepository.save(blog);
        return withReason(blog, reason);
    }

    @Transactional
    public BlogResponse updateBlog(Long userId, boolean admin, Long blogId, BlogRequest request) {

        Blog blog = findOwnedBlog(userId, admin, blogId);

        blog.setCategory(categoryService.requireActiveCategory(request.getCategoryId()));
        blog.setTitle(request.getTitle().trim());
        blog.setContent(request.getContent());

        // A draft is not public yet, so editing it needs no moderation. Anything
        // else has been (or is being) considered already and must be reconsidered.
        String reason = blog.getStatus() == ContentStatus.DRAFT
                ? null
                : applyModeration(blog);

        blogRepository.save(blog);
        return withReason(blog, reason);
    }

    @Transactional
    public BlogResponse submitBlog(Long userId, Long blogId) {

        Blog blog = findOwnedBlog(userId, false, blogId);

        if (blog.getStatus() != ContentStatus.DRAFT) {
            throw new AppException(ErrorCode.INVALID_BLOG_STATUS_TRANSITION);
        }

        String reason = applyModeration(blog);

        blogRepository.save(blog);
        return withReason(blog, reason);
    }

    @Transactional
    public void deleteBlog(Long userId, boolean admin, Long blogId) {

        blogRepository.delete(findOwnedBlog(userId, admin, blogId));
    }

    public PageResponse<BlogSummaryResponse> getOwnBlogs(
            Long authorId, ContentStatus status, int page, int size
    ) {

        PageRequest pageRequest = pageRequest(page, size, NEWEST_FIRST);

        Page<Blog> blogs = status == null
                ? blogRepository.findByAuthorId(authorId, pageRequest)
                : blogRepository.findByAuthorIdAndStatus(authorId, status, pageRequest);

        return toPageResponse(blogs);
    }

    /** Used by CommentService and VoteService: only published content can be interacted with. */
    public Blog requirePublishedBlog(Long blogId) {

        return blogRepository
                .findByIdAndStatus(blogId, ContentStatus.PUBLISHED)
                .orElseThrow(
                        () -> new AppException(
                                ErrorCode.BLOG_NOT_EXISTED
                        )
                );
    }

    /**
     * BR-02. Returns the moderation reason, or null when the content was approved.
     * Mutates the blog's status in place.
     */
    private String applyModeration(Blog blog) {

        ModerationResult result = contentModerationService.moderate(
                blog.getTitle() + "\n" + blog.getContent()
        );

        switch (result.decision()) {

            case APPROVED -> {
                blog.setStatus(ContentStatus.PUBLISHED);

                // Stamped once: re-publishing an edited post must not reshuffle the feed
                if (blog.getPublishedAt() == null) {
                    blog.setPublishedAt(LocalDateTime.now());
                }

                return null;
            }

            case REJECTED -> blog.setStatus(ContentStatus.REJECTED);

            case PENDING -> blog.setStatus(ContentStatus.PENDING);
        }

        return result.reason();
    }

    private Blog findOwnedBlog(Long userId, boolean admin, Long blogId) {

        Blog blog = blogRepository
                .findById(blogId)
                .orElseThrow(
                        () -> new AppException(
                                ErrorCode.BLOG_NOT_EXISTED
                        )
                );

        // BR-07: the owner, or an admin taking down a violation (FR-10-04)
        if (!admin && !blog.getAuthorId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        return blog;
    }

    private BlogResponse withReason(Blog blog, String reason) {

        BlogResponse response = blogMapper.toBlogResponse(blog);
        response.setModerationReason(reason);
        return response;
    }

    static PageRequest pageRequest(int page, int size, Sort sort) {

        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.min(Math.max(page, 0), Integer.MAX_VALUE / safeSize);

        return PageRequest.of(safePage, safeSize, sort);
    }

    PageResponse<BlogSummaryResponse> toPageResponse(Page<Blog> blogs) {

        return PageResponse.<BlogSummaryResponse>builder()
                .items(blogs.getContent().stream().map(blogMapper::toBlogSummaryResponse).toList())
                .page(blogs.getNumber())
                .size(blogs.getSize())
                .totalElements(blogs.getTotalElements())
                .totalPages(blogs.getTotalPages())
                .build();
    }
}
```

- [ ] **Step 7: Chạy test**

```bash
cd D:/FPT/SWP/VeggiePal/VeggiePal_BE/blog-service && ./mvnw test -Dtest=BlogServiceTest
```

Expected: PASS cả 15 test.

- [ ] **Step 8: Viết `BlogController` (phần của task này)**

`.../controller/BlogController.java`. Task 5 thêm các endpoint public, Task 6 thêm thumbnail, Task 8 thêm vote.

```java
package com.veggiepal.blog.controller;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import com.veggiepal.blog.dto.request.BlogRequest;
import com.veggiepal.blog.dto.response.ApiResponse;
import com.veggiepal.blog.dto.response.BlogResponse;
import com.veggiepal.blog.dto.response.BlogSummaryResponse;
import com.veggiepal.blog.dto.response.PageResponse;
import com.veggiepal.blog.enums.ContentStatus;
import com.veggiepal.blog.service.BlogService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/blogs")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Blog", description = "Community blog posts")
public class BlogController {

    BlogService blogService;

    @Operation(summary = "Create a blog; publish=true runs moderation right away")
    @PostMapping
    ApiResponse<BlogResponse> createBlog(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid BlogRequest request
    ) {

        return ApiResponse
                .<BlogResponse>builder()
                .result(blogService.createBlog(CurrentUser.id(jwt), request))
                .build();
    }

    @Operation(summary = "My blogs in any status")
    @GetMapping("/me")
    ApiResponse<PageResponse<BlogSummaryResponse>> getOwnBlogs(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "status", required = false) ContentStatus status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {

        return ApiResponse
                .<PageResponse<BlogSummaryResponse>>builder()
                .result(blogService.getOwnBlogs(CurrentUser.id(jwt), status, page, size))
                .build();
    }

    @Operation(summary = "Edit a blog; anything already reviewed goes back through moderation")
    @PutMapping("/{id}")
    ApiResponse<BlogResponse> updateBlog(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") Long id,
            @RequestBody @Valid BlogRequest request
    ) {

        return ApiResponse
                .<BlogResponse>builder()
                .result(blogService.updateBlog(CurrentUser.id(jwt), CurrentUser.isAdmin(jwt), id, request))
                .build();
    }

    @Operation(summary = "Submit a draft for moderation")
    @PostMapping("/{id}/submit")
    ApiResponse<BlogResponse> submitBlog(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") Long id
    ) {

        return ApiResponse
                .<BlogResponse>builder()
                .result(blogService.submitBlog(CurrentUser.id(jwt), id))
                .build();
    }

    @Operation(summary = "Delete a blog; the owner or an admin")
    @DeleteMapping("/{id}")
    ApiResponse<Void> deleteBlog(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") Long id
    ) {

        blogService.deleteBlog(CurrentUser.id(jwt), CurrentUser.isAdmin(jwt), id);
        return ApiResponse.<Void>builder().build();
    }
}
```

- [ ] **Step 9: Thêm `isAdmin` vào `CurrentUser`**

`CurrentUser` copy từ nutrition-service chưa có hàm này. Thêm vào `.../controller/CurrentUser.java`:

```java
    private static final String ROLE_CLAIM = "role";

    private static final String ADMIN_ROLE = "ADMIN";

    public static boolean isAdmin(Jwt jwt) {

        return ADMIN_ROLE.equals(jwt.getClaimAsString(ROLE_CLAIM));
    }
```

- [ ] **Step 10: Viết controller slice test**

`blog-service/src/test/java/com/veggiepal/blog/controller/BlogControllerTest.java`:

```java
package com.veggiepal.blog.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.veggiepal.blog.configuration.JwtConfig;
import com.veggiepal.blog.configuration.SecurityConfig;
import com.veggiepal.blog.configuration.SecurityExceptionHandler;
import com.veggiepal.blog.dto.request.BlogRequest;
import com.veggiepal.blog.dto.response.BlogResponse;
import com.veggiepal.blog.dto.response.PageResponse;
import com.veggiepal.blog.enums.ContentStatus;
import com.veggiepal.blog.service.BlogService;

@WebMvcTest(BlogController.class)
@Import({SecurityConfig.class, JwtConfig.class, SecurityExceptionHandler.class})
class BlogControllerTest {

    static final Long USER_ID = 7L;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    BlogService blogService;

    static RequestPostProcessor member() {
        return jwt().jwt(token -> token.claim("userId", USER_ID).claim("role", "USER"));
    }

    static RequestPostProcessor admin() {
        return jwt().jwt(token -> token.claim("userId", 1L).claim("role", "ADMIN"));
    }

    static String validBody() {
        return """
                {"title": "Đậu hũ sốt cà", "content": "%s", "categoryId": 3, "publish": true}
                """.formatted("x".repeat(50));
    }

    @Test
    void createBlog_withToken_usesUserIdFromClaim() throws Exception {
        when(blogService.createBlog(eq(USER_ID), any(BlogRequest.class)))
                .thenReturn(BlogResponse.builder().id(10L).status(ContentStatus.PUBLISHED).build());

        mockMvc.perform(post("/blogs").with(member())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.status").value("PUBLISHED"));

        verify(blogService).createBlog(eq(USER_ID), any(BlogRequest.class));
    }

    @Test
    void createBlog_withoutToken_returnsUnauthenticated() throws Exception {
        mockMvc.perform(post("/blogs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1008));

        verify(blogService, never()).createBlog(any(), any());
    }

    @Test
    void createBlog_blankTitle_returnsTitleRequired() throws Exception {
        mockMvc.perform(post("/blogs").with(member())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "  ", "content": "%s", "categoryId": 3}
                                """.formatted("x".repeat(50))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(3010));
    }

    @Test
    void createBlog_titleTooLong_returnsInvalidTitleWithMaxFilledIn() throws Exception {
        mockMvc.perform(post("/blogs").with(member())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "%s", "content": "%s", "categoryId": 3}
                                """.formatted("t".repeat(201), "x".repeat(50))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(3011))
                .andExpect(jsonPath("$.message").value("Blog title must be at most 200 characters"));
    }

    @Test
    void createBlog_contentTooShort_returnsInvalidContentWithMinFilledIn() throws Exception {
        mockMvc.perform(post("/blogs").with(member())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title": "Đậu hũ", "content": "ngắn", "categoryId": 3}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(3013))
                .andExpect(jsonPath("$.message").value("Blog content must be at least 20 characters"));
    }

    // The trap from the spec: /blogs/me must NOT be treated as /blogs/{id}
    @Test
    void getOwnBlogs_withoutToken_returnsUnauthenticated() throws Exception {
        mockMvc.perform(get("/blogs/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1008));
    }

    @Test
    void getOwnBlogs_withToken_reachesTheService() throws Exception {
        when(blogService.getOwnBlogs(USER_ID, null, 0, 20))
                .thenReturn(PageResponse.<com.veggiepal.blog.dto.response.BlogSummaryResponse>builder()
                        .items(java.util.List.of()).page(0).size(20).totalElements(0).totalPages(0).build());

        mockMvc.perform(get("/blogs/me").with(member()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.totalElements").value(0));

        verify(blogService).getOwnBlogs(USER_ID, null, 0, 20);
    }

    @Test
    void deleteBlog_asAdmin_passesAdminFlag() throws Exception {
        mockMvc.perform(delete("/blogs/10").with(admin()))
                .andExpect(status().isOk());

        verify(blogService).deleteBlog(1L, true, 10L);
    }

    @Test
    void deleteBlog_asMember_passesAdminFlagFalse() throws Exception {
        mockMvc.perform(delete("/blogs/10").with(member()))
                .andExpect(status().isOk());

        verify(blogService).deleteBlog(USER_ID, false, 10L);
    }
}
```

- [ ] **Step 11: Chạy test**

```bash
cd D:/FPT/SWP/VeggiePal/VeggiePal_BE/blog-service && ./mvnw test -Dtest='BlogServiceTest,BlogControllerTest'
```

Expected: PASS. Hai test `getOwnBlogs_*` chính là lưới an toàn cho `{id:[0-9]+}` ở Task 2.

- [ ] **Step 12: Hoàn thiện `CATEGORY_IN_USE` — giờ `BlogRepository` đã có**

Task 3 mới chặn xóa category còn category con. Bổ sung nốt vế "còn blog tham chiếu".

Thêm test vào `CategoryServiceTest`:

```java
    @Test
    void delete_stillUsedByBlogs_throwsInUse() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(root(1L, "Công thức")));
        when(categoryRepository.existsByParentId(1L)).thenReturn(false);
        when(blogRepository.existsByCategoryId(1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.delete(1L))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CATEGORY_IN_USE);

        verify(categoryRepository, never()).delete(any());
    }
```

Thêm mock vào class test:

```java
    @Mock
    BlogRepository blogRepository;
```

Và sửa `delete_leafCategory_deletes` cho khớp:

```java
        when(blogRepository.existsByCategoryId(1L)).thenReturn(false);
```

Trong `CategoryService`, thêm field và điều kiện:

```java
    BlogRepository blogRepository;
```

```java
        if (categoryRepository.existsByParentId(id) || blogRepository.existsByCategoryId(id)) {
            throw new AppException(ErrorCode.CATEGORY_IN_USE);
        }
```

Import `com.veggiepal.blog.repository.BlogRepository`.

- [ ] **Step 13: Chạy toàn bộ test**

```bash
cd D:/FPT/SWP/VeggiePal/VeggiePal_BE/blog-service && ./mvnw test -Dtest='!BlogServiceApplicationTests'
```

Expected: PASS hết.

- [ ] **Step 14: Checkpoint**

```
feat(blog): add blog CRUD with moderation hook and owner/admin checks
```

---

## Task 5: Blog công khai — list, search, detail, related

**Files:**
- Modify: `.../repository/BlogRepository.java`
- Modify: `.../service/BlogService.java`
- Modify: `.../controller/BlogController.java`
- Modify: `.../service/BlogServiceTest.java`, `.../controller/BlogControllerTest.java`

**Interfaces:**
- Consumes: Task 4 (`BlogService`, `BlogRepository`, `BlogMapper`)
- Produces:
  - `BlogService.getPublishedBlogs(Long categoryId, String keyword, String sort, int page, int size) → PageResponse<BlogSummaryResponse>`
  - `BlogService.getPublishedBlog(Long id) → BlogResponse` (tăng view)
  - `BlogService.getRelatedBlogs(Long id) → List<BlogSummaryResponse>`

- [ ] **Step 1: Thêm query vào `BlogRepository`**

```java
    @Query("""
            select b from Blog b
            where b.status = :status
              and (:categoryId is null or b.category.id = :categoryId)
              and (:keyword is null
                   or lower(b.title) like lower(concat('%', :keyword, '%'))
                   or lower(b.content) like lower(concat('%', :keyword, '%')))
            """)
    Page<Blog> search(
            @Param("status") ContentStatus status,
            @Param("categoryId") Long categoryId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    List<Blog> findByCategoryIdAndStatusAndIdNot(
            Long categoryId, ContentStatus status, Long id, Pageable pageable
    );

    /**
     * One atomic statement. Read-modify-write would let two concurrent readers
     * overwrite each other's increment and silently lose views.
     */
    @Modifying
    @Query("update Blog b set b.viewCount = b.viewCount + 1 where b.id = :id")
    void incrementViewCount(@Param("id") Long id);
```

Import thêm: `java.util.List`, `org.springframework.data.jpa.repository.Modifying`, `org.springframework.data.jpa.repository.Query`, `org.springframework.data.repository.query.Param`.

- [ ] **Step 2: Viết test cho phần public (test trước)**

Thêm vào `BlogServiceTest`:

```java
    @Test
    void getPublishedBlogs_blankKeyword_isPassedAsNull() {
        when(blogRepository.search(eq(ContentStatus.PUBLISHED), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(blog(ContentStatus.PUBLISHED))));

        PageResponse<BlogSummaryResponse> result =
                blogService.getPublishedBlogs(null, "   ", null, 0, 20);

        assertThat(result.getItems()).hasSize(1);
        verify(blogRepository).search(eq(ContentStatus.PUBLISHED), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void getPublishedBlogs_noMatch_returnsEmptyPageInsteadOfThrowing() {
        when(blogRepository.search(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PageResponse<BlogSummaryResponse> result =
                blogService.getPublishedBlogs(null, "không có gì", null, 0, 20);

        assertThat(result.getItems()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void getPublishedBlogs_sortPopular_ordersByVoteScore() {
        when(blogRepository.search(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        blogService.getPublishedBlogs(null, null, "popular", 0, 20);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(blogRepository).search(any(), any(), any(), pageable.capture());

        assertThat(pageable.getValue().getSort().getOrderFor("voteScore")).isNotNull();
    }

    @Test
    void getPublishedBlogs_unknownSort_fallsBackToNewest() {
        when(blogRepository.search(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        blogService.getPublishedBlogs(null, null, "chaos", 0, 20);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(blogRepository).search(any(), any(), any(), pageable.capture());

        assertThat(pageable.getValue().getSort().getOrderFor("publishedAt")).isNotNull();
    }

    @Test
    void getPublishedBlogs_oversizedPage_isClampedToHundred() {
        when(blogRepository.search(any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        blogService.getPublishedBlogs(null, null, null, -5, 5000);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(blogRepository).search(any(), any(), any(), pageable.capture());

        assertThat(pageable.getValue().getPageSize()).isEqualTo(100);
        assertThat(pageable.getValue().getPageNumber()).isZero();
    }

    @Test
    void getPublishedBlog_incrementsViewAndReportsTheNewCount() {
        Blog published = blog(ContentStatus.PUBLISHED);
        published.setViewCount(41);
        when(blogRepository.findByIdAndStatus(10L, ContentStatus.PUBLISHED))
                .thenReturn(Optional.of(published));

        BlogResponse response = blogService.getPublishedBlog(10L);

        verify(blogRepository).incrementViewCount(10L);
        // The JPQL update does not refresh the loaded entity, so the service adds the 1 itself
        assertThat(response.getViewCount()).isEqualTo(42);
    }

    @Test
    void getRelatedBlogs_excludesItselfAndCapsAtFive() {
        Blog published = blog(ContentStatus.PUBLISHED);
        when(blogRepository.findByIdAndStatus(10L, ContentStatus.PUBLISHED))
                .thenReturn(Optional.of(published));
        when(blogRepository.findByCategoryIdAndStatusAndIdNot(
                eq(3L), eq(ContentStatus.PUBLISHED), eq(10L), any(Pageable.class)))
                .thenReturn(List.of(blog(ContentStatus.PUBLISHED)));

        List<BlogSummaryResponse> related = blogService.getRelatedBlogs(10L);

        assertThat(related).hasSize(1);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(blogRepository).findByCategoryIdAndStatusAndIdNot(any(), any(), any(), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
    }
```

Import thêm vào `BlogServiceTest`: `static org.mockito.ArgumentMatchers.isNull`, `java.util.List`, `org.springframework.data.domain.PageImpl`, `org.springframework.data.domain.Pageable`, `com.veggiepal.blog.dto.response.BlogSummaryResponse`, `com.veggiepal.blog.dto.response.PageResponse`.

- [ ] **Step 3: Chạy test để thấy nó đỏ**

```bash
cd D:/FPT/SWP/VeggiePal/VeggiePal_BE/blog-service && ./mvnw test -Dtest=BlogServiceTest
```

Expected: FAIL khi compile — các method mới chưa có.

- [ ] **Step 4: Thêm phần public vào `BlogService`**

```java
    static final Sort POPULAR_FIRST = Sort.by(Sort.Order.desc("voteScore"), Sort.Order.desc("id"));

    static final Sort MOST_VIEWED_FIRST = Sort.by(Sort.Order.desc("viewCount"), Sort.Order.desc("id"));

    static final Sort RECENTLY_PUBLISHED_FIRST =
            Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id"));

    static final int RELATED_LIMIT = 5;
```

```java
    public PageResponse<BlogSummaryResponse> getPublishedBlogs(
            Long categoryId, String keyword, String sort, int page, int size
    ) {

        Page<Blog> blogs = blogRepository.search(
                ContentStatus.PUBLISHED,
                categoryId,
                normalizeKeyword(keyword),
                pageRequest(page, size, sortFor(sort))
        );

        return toPageResponse(blogs);
    }

    @Transactional
    public BlogResponse getPublishedBlog(Long id) {

        Blog blog = requirePublishedBlog(id);

        blogRepository.incrementViewCount(id);

        BlogResponse response = blogMapper.toBlogResponse(blog);

        // The JPQL update bypasses the persistence context, so the entity we hold
        // still has the old number. Reflect the increment we just made.
        response.setViewCount(blog.getViewCount() + 1);

        return response;
    }

    public List<BlogSummaryResponse> getRelatedBlogs(Long id) {

        Blog blog = requirePublishedBlog(id);

        return blogRepository
                .findByCategoryIdAndStatusAndIdNot(
                        blog.getCategory().getId(),
                        ContentStatus.PUBLISHED,
                        id,
                        PageRequest.of(0, RELATED_LIMIT, POPULAR_FIRST)
                )
                .stream()
                .map(blogMapper::toBlogSummaryResponse)
                .toList();
    }

    /** A blank keyword means "no filter", not "match the empty string". */
    private static String normalizeKeyword(String keyword) {

        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }

    // An unrecognised sort is a client typo, not a reason to fail the whole request
    private static Sort sortFor(String sort) {

        if (sort == null) {
            return RECENTLY_PUBLISHED_FIRST;
        }

        return switch (sort) {
            case "popular" -> POPULAR_FIRST;
            case "mostViewed" -> MOST_VIEWED_FIRST;
            default -> RECENTLY_PUBLISHED_FIRST;
        };
    }
```

Import thêm: `java.util.List`.

- [ ] **Step 5: Chạy test**

```bash
cd D:/FPT/SWP/VeggiePal/VeggiePal_BE/blog-service && ./mvnw test -Dtest=BlogServiceTest
```

Expected: PASS.

- [ ] **Step 6: Thêm 3 endpoint public vào `BlogController`**

```java
    @Operation(summary = "Published blogs; keyword searches title and content")
    @GetMapping
    ApiResponse<PageResponse<BlogSummaryResponse>> getPublishedBlogs(
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {

        return ApiResponse
                .<PageResponse<BlogSummaryResponse>>builder()
                .result(blogService.getPublishedBlogs(categoryId, keyword, sort, page, size))
                .build();
    }

    @Operation(summary = "One published blog; counts a view")
    @GetMapping("/{id}")
    ApiResponse<BlogResponse> getPublishedBlog(
            @PathVariable("id") Long id
    ) {

        return ApiResponse
                .<BlogResponse>builder()
                .result(blogService.getPublishedBlog(id))
                .build();
    }

    @Operation(summary = "Up to five published blogs in the same category")
    @GetMapping("/{id}/related")
    ApiResponse<List<BlogSummaryResponse>> getRelatedBlogs(
            @PathVariable("id") Long id
    ) {

        return ApiResponse
                .<List<BlogSummaryResponse>>builder()
                .result(blogService.getRelatedBlogs(id))
                .build();
    }
```

Import thêm: `java.util.List`.

- [ ] **Step 7: Thêm test controller cho phần public**

Thêm vào `BlogControllerTest`:

```java
    @Test
    void getPublishedBlogs_withoutToken_isPublic() throws Exception {
        when(blogService.getPublishedBlogs(null, null, null, 0, 20))
                .thenReturn(PageResponse.<BlogSummaryResponse>builder()
                        .items(List.of()).page(0).size(20).totalElements(0).totalPages(0).build());

        mockMvc.perform(get("/blogs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000));
    }

    @Test
    void getPublishedBlog_withoutToken_isPublic() throws Exception {
        when(blogService.getPublishedBlog(10L))
                .thenReturn(BlogResponse.builder().id(10L).viewCount(42).build());

        mockMvc.perform(get("/blogs/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.viewCount").value(42));
    }

    // A guest arriving with a leftover expired token must still be able to read
    @Test
    void getPublishedBlogs_withExpiredLookingToken_stillPublic() throws Exception {
        when(blogService.getPublishedBlogs(null, null, null, 0, 20))
                .thenReturn(PageResponse.<BlogSummaryResponse>builder()
                        .items(List.of()).page(0).size(20).totalElements(0).totalPages(0).build());

        mockMvc.perform(get("/blogs").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isOk());
    }

    @Test
    void getRelatedBlogs_withoutToken_isPublic() throws Exception {
        when(blogService.getRelatedBlogs(10L)).thenReturn(List.of());

        mockMvc.perform(get("/blogs/10/related"))
                .andExpect(status().isOk());
    }
```

Import thêm: `java.util.List`, `com.veggiepal.blog.dto.response.BlogSummaryResponse`.

Test `getPublishedBlogs_withExpiredLookingToken_stillPublic` là bằng chứng cho `BearerTokenResolver` — một chuỗi rác trong header không làm hỏng endpoint public.

- [ ] **Step 8: Chạy test**

```bash
cd D:/FPT/SWP/VeggiePal/VeggiePal_BE/blog-service && ./mvnw test -Dtest='!BlogServiceApplicationTests'
```

Expected: PASS hết.

- [ ] **Step 9: Checkpoint**

```
feat(blog): add public blog listing, keyword search, detail view and related posts
```

---

## Task 6: Upload ảnh bìa

**Files:**
- Create: `.../enums/ImageType.java`
- Create: `.../configuration/StorageProperties.java`, `S3Config.java`
- Create: `.../service/FileStorageService.java`, `S3FileStorageService.java`, `ImageTypeDetector.java`
- Modify: `.../service/BlogService.java`, `.../controller/BlogController.java`
- Modify: `.../service/BlogServiceTest.java`
- Modify: `docker-compose.yml`

**Interfaces:**
- Consumes: Task 4 (`BlogService.findOwnedBlog`)
- Produces: `BlogService.uploadThumbnail(Long userId, boolean admin, Long blogId, MultipartFile file) → BlogResponse`

- [ ] **Step 1: Copy 5 file storage từ identity-service**

```bash
cd D:/FPT/SWP/VeggiePal/VeggiePal_BE
mkdir -p blog-service/src/main/java/com/veggiepal/blog/enums
for f in enums/ImageType.java configuration/StorageProperties.java configuration/S3Config.java service/FileStorageService.java service/S3FileStorageService.java service/ImageTypeDetector.java; do
  sed 's/^package com\.veggiepal\./package com.veggiepal.blog./; s/import com\.veggiepal\./import com.veggiepal.blog./' \
    "identity-service/src/main/java/com/veggiepal/$f" \
    > "blog-service/src/main/java/com/veggiepal/blog/$f"
done
```

Sáu file này copy nguyên văn, không sửa logic. `S3FileStorageService` ném `ErrorCode.FILE_UPLOAD_FAILED` — mã 1017 đã khai báo ở Task 2 nên compile được ngay.

- [ ] **Step 2: Thêm bucket vào `docker-compose.yml`**

Sửa `entrypoint` của service `minio-init`:

```yaml
    entrypoint: >
      /bin/sh -c "
      mc alias set local http://minio:9000 minioadmin minioadmin &&
      mc mb --ignore-existing local/veggiepal-avatars &&
      mc anonymous set download local/veggiepal-avatars &&
      mc mb --ignore-existing local/veggiepal-blog-thumbnails &&
      mc anonymous set download local/veggiepal-blog-thumbnails
      "
```

Chạy lại để bucket được tạo:

```bash
cd D:/FPT/SWP/VeggiePal/VeggiePal_BE && docker compose up -d minio-init
```

Kiểm tra ở http://localhost:9001 (minioadmin/minioadmin) — phải thấy bucket `veggiepal-blog-thumbnails`.

- [ ] **Step 3: Viết test cho upload (test trước)**

Thêm vào `BlogServiceTest`:

```java
    @Mock
    FileStorageService fileStorageService;

    static byte[] pngBytes() {
        byte[] content = new byte[32];
        byte[] signature = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        System.arraycopy(signature, 0, content, 0, signature.length);
        return content;
    }

    @Test
    void uploadThumbnail_validPng_storesAndReplacesPreviousObject() {
        Blog existing = blog(ContentStatus.PUBLISHED);
        existing.setThumbnailUrl("http://localhost:9000/veggiepal-blog-thumbnails/old.png");

        when(blogRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(fileStorageService.upload(anyString(), any(byte[].class), eq("image/png")))
                .thenReturn("http://localhost:9000/veggiepal-blog-thumbnails/new.png");
        when(blogRepository.save(any(Blog.class))).thenAnswer(call -> call.getArgument(0));

        MockMultipartFile file =
                new MockMultipartFile("file", "cover.png", "image/png", pngBytes());

        BlogResponse response = blogService.uploadThumbnail(AUTHOR_ID, false, 10L, file);

        assertThat(response.getThumbnailUrl()).endsWith("new.png");
        verify(fileStorageService).delete("http://localhost:9000/veggiepal-blog-thumbnails/old.png");
    }

    @Test
    void uploadThumbnail_emptyFile_throwsThumbnailRequired() {
        MockMultipartFile file = new MockMultipartFile("file", "x.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> blogService.uploadThumbnail(AUTHOR_ID, false, 10L, file))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.THUMBNAIL_REQUIRED);
    }

    // Declared content type lying about the real bytes is the attack this blocks
    @Test
    void uploadThumbnail_contentTypeDoesNotMatchMagicBytes_throwsInvalidType() {
        MockMultipartFile file =
                new MockMultipartFile("file", "x.jpg", "image/jpeg", pngBytes());

        when(blogRepository.findById(10L)).thenReturn(Optional.of(blog(ContentStatus.PUBLISHED)));

        assertThatThrownBy(() -> blogService.uploadThumbnail(AUTHOR_ID, false, 10L, file))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_THUMBNAIL_TYPE);
    }

    @Test
    void uploadThumbnail_byStranger_throwsUnauthorized() {
        when(blogRepository.findById(10L)).thenReturn(Optional.of(blog(ContentStatus.PUBLISHED)));

        MockMultipartFile file =
                new MockMultipartFile("file", "cover.png", "image/png", pngBytes());

        assertThatThrownBy(() -> blogService.uploadThumbnail(STRANGER_ID, false, 10L, file))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }
```

Import thêm: `org.springframework.mock.web.MockMultipartFile`, `com.veggiepal.blog.service.FileStorageService` (cùng package nên không cần import), `static org.mockito.ArgumentMatchers.anyString`.

**Lưu ý thứ tự kiểm tra:** test `uploadThumbnail_emptyFile_*` **không** stub `blogRepository.findById`, nên file rỗng phải bị chặn **trước** khi tra cứu blog. Test `uploadThumbnail_byStranger_*` thì ngược lại — quyền sở hữu phải được kiểm tra trước khi upload lên storage, để người lạ không ghi rác vào bucket.

- [ ] **Step 4: Chạy test để thấy nó đỏ**

```bash
cd D:/FPT/SWP/VeggiePal/VeggiePal_BE/blog-service && ./mvnw test -Dtest=BlogServiceTest
```

Expected: FAIL — `uploadThumbnail` chưa tồn tại.

- [ ] **Step 5: Viết `uploadThumbnail` trong `BlogService`**

Thêm field:

```java
    FileStorageService fileStorageService;
```

Thêm hằng số và method:

```java
    static final long MAX_THUMBNAIL_BYTES = 5L * 1024 * 1024;
```

```java
    @Transactional
    public BlogResponse uploadThumbnail(Long userId, boolean admin, Long blogId, MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.THUMBNAIL_REQUIRED);
        }

        if (file.getSize() > MAX_THUMBNAIL_BYTES) {
            throw new AppException(ErrorCode.THUMBNAIL_TOO_LARGE);
        }

        // Ownership before storage: a stranger must not be able to write into the bucket
        Blog blog = findOwnedBlog(userId, admin, blogId);

        byte[] content = readContent(file);

        // The declared content type must match the real file signature
        ImageType imageType = ImageTypeDetector
                .detect(content)
                .filter(type -> type.getContentType().equals(file.getContentType()))
                .orElseThrow(
                        () -> new AppException(
                                ErrorCode.INVALID_THUMBNAIL_TYPE
                        )
                );

        String previousThumbnailUrl = blog.getThumbnailUrl();

        String key = "thumbnails/" + blogId + "/" + UUID.randomUUID() + "." + imageType.getExtension();
        String thumbnailUrl = fileStorageService.upload(key, content, imageType.getContentType());

        blog.setThumbnailUrl(thumbnailUrl);

        try {
            blogRepository.save(blog);
        } catch (RuntimeException exception) {
            deleteQuietly(thumbnailUrl);
            throw exception;
        }

        deleteQuietly(previousThumbnailUrl);
        return blogMapper.toBlogResponse(blog);
    }

    private byte[] readContent(MultipartFile file) {

        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    private void deleteQuietly(String url) {

        if (url == null) {
            return;
        }

        try {
            fileStorageService.delete(url);
        } catch (RuntimeException exception) {
            log.warn("Could not delete thumbnail object from storage", exception);
        }
    }
```

Thêm `@Slf4j` lên class `BlogService` và import: `java.io.IOException`, `java.util.UUID`, `org.springframework.web.multipart.MultipartFile`, `com.veggiepal.blog.enums.ImageType`, `lombok.extern.slf4j.Slf4j`.

- [ ] **Step 6: Chạy test**

```bash
cd D:/FPT/SWP/VeggiePal/VeggiePal_BE/blog-service && ./mvnw test -Dtest=BlogServiceTest
```

Expected: PASS.

- [ ] **Step 7: Thêm endpoint vào `BlogController`**

```java
    @Operation(summary = "Upload or replace the cover image")
    @PostMapping(value = "/{id}/thumbnail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ApiResponse<BlogResponse> uploadThumbnail(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") Long id,
            @RequestParam("file") MultipartFile file
    ) {

        return ApiResponse
                .<BlogResponse>builder()
                .result(blogService.uploadThumbnail(
                        CurrentUser.id(jwt), CurrentUser.isAdmin(jwt), id, file))
                .build();
    }
```

Import thêm: `org.springframework.http.MediaType`, `org.springframework.web.multipart.MultipartFile`.

- [ ] **Step 8: Chạy toàn bộ test**

```bash
cd D:/FPT/SWP/VeggiePal/VeggiePal_BE/blog-service && ./mvnw test -Dtest='!BlogServiceApplicationTests'
```

Expected: PASS hết.

- [ ] **Step 9: Checkpoint**

```
feat(blog): add blog thumbnail upload validated by content type and magic bytes
```

---

## Task 7: Comment và reply

**Files:**
- Create: `.../enums/TargetType.java`, `CommentStatus.java`
- Create: `.../entity/Comment.java`
- Create: `.../repository/CommentRepository.java`
- Create: `.../dto/request/CommentRequest.java`
- Create: `.../dto/response/CommentResponse.java`
- Create: `.../mapper/CommentMapper.java`
- Create: `.../service/CommentService.java`
- Create: `.../controller/CommentController.java`
- Test: `.../service/CommentServiceTest.java`, `.../controller/CommentControllerTest.java`

**Interfaces:**
- Consumes: Task 4 (`BlogService.requirePublishedBlog(Long) → Blog`), Task 4 (`ContentModerationService`)
- Produces:
  - `TargetType` — `BLOG`, `VIDEO`
  - `CommentStatus` — `PENDING`, `VISIBLE`, `HIDDEN`, `DELETED`
  - `Comment` entity, `CommentRepository`
  - `CommentService.createComment`, `updateComment`, `deleteComment`, `getRootComments`, `getReplies`

- [ ] **Step 1: Viết enum**

`.../enums/TargetType.java`:

```java
package com.veggiepal.blog.enums;

public enum TargetType {
    BLOG,

    /**
     * Declared before videos exist on purpose. Hibernate maps this enum to a native
     * MySQL ENUM column and ddl-auto=update will NOT add a constant later — adding
     * it then would need a manual ALTER TABLE on every environment.
     */
    VIDEO
}
```

`.../enums/CommentStatus.java`:

```java
package com.veggiepal.blog.enums;

public enum CommentStatus {
    /** Unused today; reserved so async AI moderation needs no ALTER TABLE later. */
    PENDING,

    VISIBLE,

    HIDDEN,

    DELETED
}
```

- [ ] **Step 2: Viết entity và repository**

`.../entity/Comment.java`:

```java
package com.veggiepal.blog.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import com.veggiepal.blog.enums.CommentStatus;
import com.veggiepal.blog.enums.TargetType;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(
        name = "comments",
        indexes = {
                @Index(name = "idx_comments_target", columnList = "target_type, target_id, created_at"),
                @Index(name = "idx_comments_parent", columnList = "parent_comment_id, created_at"),
                @Index(name = "idx_comments_author", columnList = "author_id, created_at")
        }
)
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "author_id", nullable = false)
    Long authorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    TargetType targetType;

    // No FK to blogs: the same column also points at videos later. The service checks it.
    @Column(name = "target_id", nullable = false)
    Long targetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Comment parent;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    CommentStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

`.../repository/CommentRepository.java`:

```java
package com.veggiepal.blog.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.veggiepal.blog.entity.Comment;
import com.veggiepal.blog.enums.CommentStatus;
import com.veggiepal.blog.enums.TargetType;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    Page<Comment> findByTargetTypeAndTargetIdAndParentIsNullAndStatusIn(
            TargetType targetType, Long targetId, Collection<CommentStatus> statuses, Pageable pageable
    );

    Page<Comment> findByParentIdAndStatusIn(
            Long parentId, Collection<CommentStatus> statuses, Pageable pageable
    );

    /** One grouped query instead of a count per comment. */
    @Query("""
            select c.parent.id as parentId, count(c) as total
            from Comment c
            where c.parent.id in :parentIds and c.status in :statuses
            group by c.parent.id
            """)
    List<ReplyCount> countRepliesByParentIds(
            @Param("parentIds") Collection<Long> parentIds,
            @Param("statuses") Collection<CommentStatus> statuses
    );

    interface ReplyCount {
        Long getParentId();

        Long getTotal();
    }
}
```

- [ ] **Step 3: Viết DTO và mapper**

`.../dto/request/CommentRequest.java`:

```java
package com.veggiepal.blog.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.veggiepal.blog.enums.TargetType;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommentRequest {

    @NotNull(message = "UNSUPPORTED_TARGET_TYPE")
    TargetType targetType;

    @NotNull(message = "COMMENT_TARGET_NOT_EXISTED")
    Long targetId;

    /** Null for a top-level comment. Must point at a top-level comment otherwise. */
    Long parentCommentId;

    @NotBlank(message = "COMMENT_CONTENT_REQUIRED")
    @Size(max = 2000, message = "INVALID_COMMENT_CONTENT")
    String content;
}
```

`.../dto/response/CommentResponse.java`:

```java
package com.veggiepal.blog.dto.response;

import java.time.LocalDateTime;

import com.veggiepal.blog.enums.TargetType;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CommentResponse {

    Long id;

    Long authorId;

    TargetType targetType;

    Long targetId;

    Long parentCommentId;

    /** Null when the comment was deleted; the row survives so replies keep their thread. */
    String content;

    boolean deleted;

    Long replyCount;

    LocalDateTime createdAt;

    LocalDateTime updatedAt;
}
```

`.../mapper/CommentMapper.java`:

```java
package com.veggiepal.blog.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.veggiepal.blog.dto.response.CommentResponse;
import com.veggiepal.blog.entity.Comment;

@Mapper(componentModel = "spring")
public interface CommentMapper {

    @Mapping(target = "parentCommentId", source = "parent.id")
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "replyCount", ignore = true)
    CommentResponse toCommentResponse(Comment comment);
}
```

- [ ] **Step 4: Viết test cho `CommentService` (test trước)**

`blog-service/src/test/java/com/veggiepal/blog/service/CommentServiceTest.java`:

```java
package com.veggiepal.blog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.veggiepal.blog.dto.request.CommentRequest;
import com.veggiepal.blog.dto.response.CommentResponse;
import com.veggiepal.blog.dto.response.PageResponse;
import com.veggiepal.blog.entity.Blog;
import com.veggiepal.blog.entity.Comment;
import com.veggiepal.blog.enums.CommentStatus;
import com.veggiepal.blog.enums.TargetType;
import com.veggiepal.blog.exception.AppException;
import com.veggiepal.blog.exception.ErrorCode;
import com.veggiepal.blog.mapper.CommentMapper;
import com.veggiepal.blog.moderation.ModerationDecision;
import com.veggiepal.blog.moderation.ModerationResult;
import com.veggiepal.blog.moderation.ContentModerationService;
import com.veggiepal.blog.repository.CommentRepository;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    static final Long AUTHOR_ID = 7L;
    static final Long STRANGER_ID = 8L;
    static final Long ADMIN_ID = 1L;

    @Mock
    CommentRepository commentRepository;

    @Mock
    BlogService blogService;

    @Mock
    ContentModerationService contentModerationService;

    @Spy
    CommentMapper commentMapper = Mappers.getMapper(CommentMapper.class);

    @InjectMocks
    CommentService commentService;

    static Comment comment(Long id, Comment parent, CommentStatus status) {
        return Comment.builder()
                .id(id).authorId(AUTHOR_ID)
                .targetType(TargetType.BLOG).targetId(10L)
                .parent(parent).content("ngon quá").status(status)
                .build();
    }

    static CommentRequest request(Long parentCommentId) {
        return CommentRequest.builder()
                .targetType(TargetType.BLOG).targetId(10L)
                .parentCommentId(parentCommentId).content("ngon quá")
                .build();
    }

    @Test
    void createComment_approved_becomesVisible() {
        when(blogService.requirePublishedBlog(10L)).thenReturn(Blog.builder().id(10L).build());
        when(contentModerationService.moderate(anyString())).thenReturn(ModerationResult.approved());
        when(commentRepository.save(any(Comment.class))).thenAnswer(call -> call.getArgument(0));

        CommentResponse response = commentService.createComment(AUTHOR_ID, request(null));

        assertThat(response.getContent()).isEqualTo("ngon quá");
        assertThat(response.isDeleted()).isFalse();
    }

    @Test
    void createComment_rejected_isHiddenInsteadOfVisible() {
        when(blogService.requirePublishedBlog(10L)).thenReturn(Blog.builder().id(10L).build());
        when(contentModerationService.moderate(anyString()))
                .thenReturn(new ModerationResult(ModerationDecision.REJECTED, "abuse"));

        java.util.concurrent.atomic.AtomicReference<Comment> saved = new java.util.concurrent.atomic.AtomicReference<>();
        when(commentRepository.save(any(Comment.class))).thenAnswer(call -> {
            saved.set(call.getArgument(0));
            return call.getArgument(0);
        });

        commentService.createComment(AUTHOR_ID, request(null));

        assertThat(saved.get().getStatus()).isEqualTo(CommentStatus.HIDDEN);
    }

    @Test
    void createComment_onMissingBlog_propagatesBlogNotExisted() {
        when(blogService.requirePublishedBlog(10L))
                .thenThrow(new AppException(ErrorCode.BLOG_NOT_EXISTED));

        assertThatThrownBy(() -> commentService.createComment(AUTHOR_ID, request(null)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.BLOG_NOT_EXISTED);

        verify(commentRepository, never()).save(any());
    }

    // The column already accepts VIDEO; the API does not, yet
    @Test
    void createComment_onVideo_throwsUnsupportedTargetType() {
        CommentRequest videoRequest = CommentRequest.builder()
                .targetType(TargetType.VIDEO).targetId(10L).content("ngon quá").build();

        assertThatThrownBy(() -> commentService.createComment(AUTHOR_ID, videoRequest))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNSUPPORTED_TARGET_TYPE);

        verify(commentRepository, never()).save(any());
    }

    @Test
    void createComment_replyToRootComment_isAllowed() {
        when(blogService.requirePublishedBlog(10L)).thenReturn(Blog.builder().id(10L).build());
        when(commentRepository.findById(5L)).thenReturn(Optional.of(comment(5L, null, CommentStatus.VISIBLE)));
        when(contentModerationService.moderate(anyString())).thenReturn(ModerationResult.approved());
        when(commentRepository.save(any(Comment.class))).thenAnswer(call -> call.getArgument(0));

        CommentResponse response = commentService.createComment(AUTHOR_ID, request(5L));

        assertThat(response.getParentCommentId()).isEqualTo(5L);
    }

    @Test
    void createComment_replyToAReply_throwsTooDeep() {
        Comment root = comment(5L, null, CommentStatus.VISIBLE);
        when(blogService.requirePublishedBlog(10L)).thenReturn(Blog.builder().id(10L).build());
        when(commentRepository.findById(6L)).thenReturn(Optional.of(comment(6L, root, CommentStatus.VISIBLE)));

        assertThatThrownBy(() -> commentService.createComment(AUTHOR_ID, request(6L)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.COMMENT_REPLY_TOO_DEEP);
    }

    @Test
    void createComment_parentOnDifferentBlog_throwsInvalidParent() {
        Comment otherThread = comment(5L, null, CommentStatus.VISIBLE);
        otherThread.setTargetId(99L);

        when(blogService.requirePublishedBlog(10L)).thenReturn(Blog.builder().id(10L).build());
        when(commentRepository.findById(5L)).thenReturn(Optional.of(otherThread));

        assertThatThrownBy(() -> commentService.createComment(AUTHOR_ID, request(5L)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_COMMENT_PARENT);
    }

    @Test
    void updateComment_byStranger_throwsUnauthorized() {
        when(commentRepository.findById(5L)).thenReturn(Optional.of(comment(5L, null, CommentStatus.VISIBLE)));

        assertThatThrownBy(() -> commentService.updateComment(STRANGER_ID, 5L, request(null)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    // Soft delete: the row stays so its replies do not become orphans
    @Test
    void deleteComment_byOwner_marksDeletedAndKeepsTheRow() {
        Comment existing = comment(5L, null, CommentStatus.VISIBLE);
        when(commentRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(commentRepository.save(any(Comment.class))).thenAnswer(call -> call.getArgument(0));

        commentService.deleteComment(AUTHOR_ID, false, 5L);

        assertThat(existing.getStatus()).isEqualTo(CommentStatus.DELETED);
        verify(commentRepository, never()).delete(any());
    }

    @Test
    void deleteComment_byAdmin_isAllowedOnSomeoneElsesComment() {
        Comment existing = comment(5L, null, CommentStatus.VISIBLE);
        when(commentRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(commentRepository.save(any(Comment.class))).thenAnswer(call -> call.getArgument(0));

        commentService.deleteComment(ADMIN_ID, true, 5L);

        assertThat(existing.getStatus()).isEqualTo(CommentStatus.DELETED);
    }

    @Test
    void getRootComments_deletedComment_hidesContentButKeepsTheEntry() {
        Comment deleted = comment(5L, null, CommentStatus.DELETED);
        when(commentRepository.findByTargetTypeAndTargetIdAndParentIsNullAndStatusIn(
                any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(deleted)));
        when(commentRepository.countRepliesByParentIds(any(), any())).thenReturn(List.of());

        PageResponse<CommentResponse> result =
                commentService.getRootComments(TargetType.BLOG, 10L, 0, 20);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().getFirst().getContent()).isNull();
        assertThat(result.getItems().getFirst().isDeleted()).isTrue();
    }

    @Test
    void getRootComments_emptyPage_skipsTheReplyCountQuery() {
        when(commentRepository.findByTargetTypeAndTargetIdAndParentIsNullAndStatusIn(
                any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PageResponse<CommentResponse> result =
                commentService.getRootComments(TargetType.BLOG, 10L, 0, 20);

        assertThat(result.getItems()).isEmpty();
        verify(commentRepository, never()).countRepliesByParentIds(any(), any());
    }
}
```

- [ ] **Step 5: Chạy test để thấy nó đỏ**

```bash
cd D:/FPT/SWP/VeggiePal/VeggiePal_BE/blog-service && ./mvnw test -Dtest=CommentServiceTest
```

Expected: FAIL khi compile.

- [ ] **Step 6: Viết `CommentService`**

```java
package com.veggiepal.blog.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veggiepal.blog.dto.request.CommentRequest;
import com.veggiepal.blog.dto.response.CommentResponse;
import com.veggiepal.blog.dto.response.PageResponse;
import com.veggiepal.blog.entity.Comment;
import com.veggiepal.blog.enums.CommentStatus;
import com.veggiepal.blog.enums.TargetType;
import com.veggiepal.blog.exception.AppException;
import com.veggiepal.blog.exception.ErrorCode;
import com.veggiepal.blog.mapper.CommentMapper;
import com.veggiepal.blog.moderation.ContentModerationService;
import com.veggiepal.blog.moderation.ModerationResult;
import com.veggiepal.blog.repository.CommentRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CommentService {

    // DELETED stays visible as a tombstone so a reply thread does not break apart
    static final Set<CommentStatus> PUBLICLY_VISIBLE =
            Set.of(CommentStatus.VISIBLE, CommentStatus.DELETED);

    static final Sort OLDEST_FIRST = Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id"));

    CommentRepository commentRepository;
    BlogService blogService;
    ContentModerationService contentModerationService;
    CommentMapper commentMapper;

    @Transactional
    public CommentResponse createComment(Long authorId, CommentRequest request) {

        requireSupportedTarget(request.getTargetType());

        // Only published content can be commented on
        blogService.requirePublishedBlog(request.getTargetId());

        Comment parent = resolveParent(request);

        Comment comment = Comment.builder()
                .authorId(authorId)
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .parent(parent)
                .content(request.getContent())
                .status(CommentStatus.PENDING)
                .build();

        applyModeration(comment);

        commentRepository.save(comment);
        return toResponse(comment, 0L);
    }

    @Transactional
    public CommentResponse updateComment(Long userId, Long commentId, CommentRequest request) {

        Comment comment = findComment(commentId);

        // Editing someone else's words is never an admin action
        if (!comment.getAuthorId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        comment.setContent(request.getContent());
        applyModeration(comment);

        commentRepository.save(comment);
        return toResponse(comment, null);
    }

    @Transactional
    public void deleteComment(Long userId, boolean admin, Long commentId) {

        Comment comment = findComment(commentId);

        // BR-07 plus FR-10-04: the owner, or an admin removing a violation
        if (!admin && !comment.getAuthorId().equals(userId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        comment.setStatus(CommentStatus.DELETED);
        commentRepository.save(comment);
    }

    public PageResponse<CommentResponse> getRootComments(
            TargetType targetType, Long targetId, int page, int size
    ) {

        Page<Comment> comments = commentRepository
                .findByTargetTypeAndTargetIdAndParentIsNullAndStatusIn(
                        targetType, targetId, PUBLICLY_VISIBLE,
                        BlogService.pageRequest(page, size, OLDEST_FIRST)
                );

        Map<Long, Long> replyCounts = replyCountsFor(comments.getContent());

        return toPageResponse(comments, comment -> replyCounts.getOrDefault(comment.getId(), 0L));
    }

    public PageResponse<CommentResponse> getReplies(Long commentId, int page, int size) {

        Page<Comment> replies = commentRepository.findByParentIdAndStatusIn(
                commentId, PUBLICLY_VISIBLE,
                BlogService.pageRequest(page, size, OLDEST_FIRST)
        );

        // A reply cannot have replies of its own, so the count is always zero
        return toPageResponse(replies, comment -> 0L);
    }

    private Map<Long, Long> replyCountsFor(List<Comment> roots) {

        if (roots.isEmpty()) {
            return Map.of();
        }

        return commentRepository
                .countRepliesByParentIds(roots.stream().map(Comment::getId).toList(), PUBLICLY_VISIBLE)
                .stream()
                .collect(Collectors.toMap(
                        CommentRepository.ReplyCount::getParentId,
                        CommentRepository.ReplyCount::getTotal
                ));
    }

    private Comment resolveParent(CommentRequest request) {

        if (request.getParentCommentId() == null) {
            return null;
        }

        Comment parent = findComment(request.getParentCommentId());

        // One level of replies only, as the ERD describes
        if (parent.getParent() != null) {
            throw new AppException(ErrorCode.COMMENT_REPLY_TOO_DEEP);
        }

        boolean sameThread = parent.getTargetType() == request.getTargetType()
                && parent.getTargetId().equals(request.getTargetId());

        if (!sameThread) {
            throw new AppException(ErrorCode.INVALID_COMMENT_PARENT);
        }

        return parent;
    }

    private void applyModeration(Comment comment) {

        ModerationResult result = contentModerationService.moderate(comment.getContent());

        comment.setStatus(switch (result.decision()) {
            case APPROVED -> CommentStatus.VISIBLE;
            case REJECTED -> CommentStatus.HIDDEN;
            case PENDING -> CommentStatus.PENDING;
        });
    }

    private static void requireSupportedTarget(TargetType targetType) {

        // The column already accepts VIDEO so no ALTER TABLE is needed later,
        // but there is no video API to point at yet.
        if (targetType != TargetType.BLOG) {
            throw new AppException(ErrorCode.UNSUPPORTED_TARGET_TYPE);
        }
    }

    private Comment findComment(Long commentId) {

        return commentRepository
                .findById(commentId)
                .orElseThrow(
                        () -> new AppException(
                                ErrorCode.COMMENT_NOT_EXISTED
                        )
                );
    }

    private PageResponse<CommentResponse> toPageResponse(
            Page<Comment> comments, Function<Comment, Long> replyCount
    ) {

        return PageResponse.<CommentResponse>builder()
                .items(comments.getContent().stream()
                        .map(comment -> toResponse(comment, replyCount.apply(comment)))
                        .toList())
                .page(comments.getNumber())
                .size(comments.getSize())
                .totalElements(comments.getTotalElements())
                .totalPages(comments.getTotalPages())
                .build();
    }

    private CommentResponse toResponse(Comment comment, Long replyCount) {

        CommentResponse response = commentMapper.toCommentResponse(comment);

        boolean deleted = comment.getStatus() == CommentStatus.DELETED;

        response.setDeleted(deleted);
        response.setContent(deleted ? null : comment.getContent());
        response.setReplyCount(replyCount);

        return response;
    }
}
```

`BlogService.pageRequest` đang là `static` package-private — `CommentService` ở cùng package `service` nên gọi được.

- [ ] **Step 7: Chạy test**

```bash
cd D:/FPT/SWP/VeggiePal/VeggiePal_BE/blog-service && ./mvnw test -Dtest=CommentServiceTest
```

Expected: PASS cả 12 test.

- [ ] **Step 8: Viết `CommentController`**

```java
package com.veggiepal.blog.controller;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import com.veggiepal.blog.dto.request.CommentRequest;
import com.veggiepal.blog.dto.response.ApiResponse;
import com.veggiepal.blog.dto.response.CommentResponse;
import com.veggiepal.blog.dto.response.PageResponse;
import com.veggiepal.blog.enums.TargetType;
import com.veggiepal.blog.service.CommentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Comment", description = "Comments and one level of replies")
public class CommentController {

    CommentService commentService;

    @Operation(summary = "Top-level comments on a piece of content")
    @GetMapping
    ApiResponse<PageResponse<CommentResponse>> getRootComments(
            @RequestParam(name = "targetType", defaultValue = "BLOG") TargetType targetType,
            @RequestParam(name = "targetId") Long targetId,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {

        return ApiResponse
                .<PageResponse<CommentResponse>>builder()
                .result(commentService.getRootComments(targetType, targetId, page, size))
                .build();
    }

    @Operation(summary = "Replies under one comment")
    @GetMapping("/{id}/replies")
    ApiResponse<PageResponse<CommentResponse>> getReplies(
            @PathVariable("id") Long id,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {

        return ApiResponse
                .<PageResponse<CommentResponse>>builder()
                .result(commentService.getReplies(id, page, size))
                .build();
    }

    @Operation(summary = "Post a comment or a reply")
    @PostMapping
    ApiResponse<CommentResponse> createComment(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid CommentRequest request
    ) {

        return ApiResponse
                .<CommentResponse>builder()
                .result(commentService.createComment(CurrentUser.id(jwt), request))
                .build();
    }

    @Operation(summary = "Edit your own comment")
    @PutMapping("/{id}")
    ApiResponse<CommentResponse> updateComment(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") Long id,
            @RequestBody @Valid CommentRequest request
    ) {

        return ApiResponse
                .<CommentResponse>builder()
                .result(commentService.updateComment(CurrentUser.id(jwt), id, request))
                .build();
    }

    @Operation(summary = "Delete a comment; the owner or an admin")
    @DeleteMapping("/{id}")
    ApiResponse<Void> deleteComment(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") Long id
    ) {

        commentService.deleteComment(CurrentUser.id(jwt), CurrentUser.isAdmin(jwt), id);
        return ApiResponse.<Void>builder().build();
    }
}
```

- [ ] **Step 9: Viết controller slice test**

`blog-service/src/test/java/com/veggiepal/blog/controller/CommentControllerTest.java`:

```java
package com.veggiepal.blog.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.veggiepal.blog.configuration.JwtConfig;
import com.veggiepal.blog.configuration.SecurityConfig;
import com.veggiepal.blog.configuration.SecurityExceptionHandler;
import com.veggiepal.blog.dto.request.CommentRequest;
import com.veggiepal.blog.dto.response.CommentResponse;
import com.veggiepal.blog.dto.response.PageResponse;
import com.veggiepal.blog.enums.TargetType;
import com.veggiepal.blog.service.CommentService;

@WebMvcTest(CommentController.class)
@Import({SecurityConfig.class, JwtConfig.class, SecurityExceptionHandler.class})
class CommentControllerTest {

    static final Long USER_ID = 7L;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CommentService commentService;

    static RequestPostProcessor member() {
        return jwt().jwt(token -> token.claim("userId", USER_ID).claim("role", "USER"));
    }

    static PageResponse<CommentResponse> emptyPage() {
        return PageResponse.<CommentResponse>builder()
                .items(List.of()).page(0).size(20).totalElements(0).totalPages(0).build();
    }

    @Test
    void getRootComments_withoutToken_isPublic() throws Exception {
        when(commentService.getRootComments(TargetType.BLOG, 10L, 0, 20)).thenReturn(emptyPage());

        mockMvc.perform(get("/comments").param("targetId", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000));
    }

    @Test
    void getReplies_withoutToken_isPublic() throws Exception {
        when(commentService.getReplies(5L, 0, 20)).thenReturn(emptyPage());

        mockMvc.perform(get("/comments/5/replies"))
                .andExpect(status().isOk());
    }

    @Test
    void createComment_withoutToken_returnsUnauthenticated() throws Exception {
        mockMvc.perform(post("/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetType": "BLOG", "targetId": 10, "content": "ngon quá"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1008));

        verify(commentService, never()).createComment(any(), any());
    }

    @Test
    void createComment_withToken_usesUserIdFromClaim() throws Exception {
        when(commentService.createComment(eq(USER_ID), any(CommentRequest.class)))
                .thenReturn(CommentResponse.builder().id(5L).content("ngon quá").build());

        mockMvc.perform(post("/comments").with(member())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetType": "BLOG", "targetId": 10, "content": "ngon quá"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(5));

        verify(commentService).createComment(eq(USER_ID), any(CommentRequest.class));
    }

    @Test
    void createComment_blankContent_returnsContentRequired() throws Exception {
        mockMvc.perform(post("/comments").with(member())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetType": "BLOG", "targetId": 10, "content": "   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(3030));
    }

    @Test
    void createComment_contentTooLong_returnsInvalidContentWithMaxFilledIn() throws Exception {
        mockMvc.perform(post("/comments").with(member())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetType": "BLOG", "targetId": 10, "content": "%s"}
                                """.formatted("x".repeat(2001))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(3031))
                .andExpect(jsonPath("$.message").value("Comment must be at most 2000 characters"));
    }

    @Test
    void deleteComment_passesAdminFlagFromClaim() throws Exception {
        mockMvc.perform(delete("/comments/5").with(member()))
                .andExpect(status().isOk());

        verify(commentService).deleteComment(USER_ID, false, 5L);
    }
}
```

- [ ] **Step 10: Chạy toàn bộ test**

```bash
cd D:/FPT/SWP/VeggiePal/VeggiePal_BE/blog-service && ./mvnw test -Dtest='!BlogServiceApplicationTests'
```

Expected: PASS hết.

- [ ] **Step 11: Checkpoint**

```
feat(blog): add comments with one-level replies and soft delete
```

---

## Task 8: Vote và `vote_score`

**Files:**
- Create: `.../entity/ContentVote.java`
- Create: `.../repository/ContentVoteRepository.java`
- Create: `.../dto/request/VoteRequest.java`
- Create: `.../dto/response/VoteResponse.java`
- Create: `.../service/VoteService.java`
- Modify: `.../repository/BlogRepository.java`, `.../controller/BlogController.java`
- Test: `.../service/VoteServiceTest.java`, `.../controller/BlogControllerTest.java`

**Interfaces:**
- Consumes: Task 4 (`BlogService.requirePublishedBlog`)
- Produces: `VoteService.vote`, `removeVote`, `getMyVotes`

**Ghi chú thiết kế:** bảng delta trong spec rút gọn được thành `after - before` với "chưa vote" = `0`. Kiểm lại cả 9 dòng: `0→1 = +1`, `0→-1 = −1`, `1→-1 = −2`, `-1→1 = +2`, `1→1 = 0`, `-1→-1 = 0`, `1→0 = −1`, `-1→0 = +1`, `0→0 = 0`. Vì vậy code không cần `switch`, chỉ cần một phép trừ — và test vẫn phải đi qua đủ 9 dòng để chứng minh phép rút gọn đó đúng.

- [ ] **Step 1: Viết entity và repository**

`.../entity/ContentVote.java`:

```java
package com.veggiepal.blog.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import com.veggiepal.blog.enums.TargetType;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(
        name = "content_votes",
        uniqueConstraints = @UniqueConstraint(
                // FR-04-03: the database refuses a duplicate vote, not just the code
                name = "uk_content_votes_user_target",
                columnNames = {"user_id", "target_type", "target_id"}
        )
)
public class ContentVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "user_id", nullable = false)
    Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    TargetType targetType;

    @Column(name = "target_id", nullable = false)
    Long targetId;

    /** -1 or 1. A number, not an enum, so vote_score can be summed arithmetically. */
    @Column(nullable = false)
    Integer value;

    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

`.../repository/ContentVoteRepository.java`:

```java
package com.veggiepal.blog.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.veggiepal.blog.entity.ContentVote;
import com.veggiepal.blog.enums.TargetType;

@Repository
public interface ContentVoteRepository extends JpaRepository<ContentVote, Long> {

    Optional<ContentVote> findByUserIdAndTargetTypeAndTargetId(
            Long userId, TargetType targetType, Long targetId
    );

    List<ContentVote> findByUserIdAndTargetTypeAndTargetIdIn(
            Long userId, TargetType targetType, Collection<Long> targetIds
    );
}
```

Thêm vào `BlogRepository`:

```java
    /** Atomic so two concurrent votes cannot overwrite each other's adjustment. */
    @Modifying
    @Query("update Blog b set b.voteScore = b.voteScore + :delta where b.id = :id")
    void addVoteScore(@Param("id") Long id, @Param("delta") int delta);
```

- [ ] **Step 2: Viết DTO**

`.../dto/request/VoteRequest.java`:

```java
package com.veggiepal.blog.dto.request;

import jakarta.validation.constraints.NotNull;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VoteRequest {

    @NotNull(message = "INVALID_VOTE_VALUE")
    Integer value;
}
```

`.../dto/response/VoteResponse.java`:

```java
package com.veggiepal.blog.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VoteResponse {

    Long blogId;

    /** The caller's own vote: 1, -1, or null when they have not voted. */
    Integer myVote;

    Integer voteScore;
}
```

- [ ] **Step 3: Viết test cho `VoteService` (test trước)**

`blog-service/src/test/java/com/veggiepal/blog/service/VoteServiceTest.java`:

```java
package com.veggiepal.blog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.veggiepal.blog.entity.Blog;
import com.veggiepal.blog.entity.ContentVote;
import com.veggiepal.blog.enums.TargetType;
import com.veggiepal.blog.exception.AppException;
import com.veggiepal.blog.exception.ErrorCode;
import com.veggiepal.blog.repository.BlogRepository;
import com.veggiepal.blog.repository.ContentVoteRepository;

@ExtendWith(MockitoExtension.class)
class VoteServiceTest {

    static final Long VOTER_ID = 8L;
    static final Long AUTHOR_ID = 7L;

    @Mock
    ContentVoteRepository contentVoteRepository;

    @Mock
    BlogRepository blogRepository;

    @Mock
    BlogService blogService;

    @InjectMocks
    VoteService voteService;

    static Blog publishedBlog() {
        return Blog.builder().id(10L).authorId(AUTHOR_ID).voteScore(4).build();
    }

    static ContentVote existingVote(int value) {
        return ContentVote.builder()
                .id(1L).userId(VOTER_ID).targetType(TargetType.BLOG).targetId(10L).value(value)
                .build();
    }

    // All nine rows of the delta table in the spec, section 6.2.
    // "no vote" is written as 0 for both the previous and the new value.
    @ParameterizedTest(name = "{0} then {1} moves the score by {2}")
    @CsvSource({
            "0,  1,  1",
            "0, -1, -1",
            "1, -1, -2",
            "-1,  1,  2",
            "1,  1,  0",
            "-1, -1,  0",
            "1,  0, -1",
            "-1,  0,  1",
            "0,  0,  0"
    })
    void voteDelta_matchesTheSpecTable(int previous, int next, int expectedDelta) {
        assertThat(VoteService.voteDelta(previous == 0 ? null : previous, next == 0 ? null : next))
                .isEqualTo(expectedDelta);
    }

    @Test
    void vote_firstTime_savesVoteAndAddsOne() {
        when(blogService.requirePublishedBlog(10L)).thenReturn(publishedBlog());
        when(contentVoteRepository.findByUserIdAndTargetTypeAndTargetId(VOTER_ID, TargetType.BLOG, 10L))
                .thenReturn(Optional.empty());
        when(contentVoteRepository.save(any(ContentVote.class))).thenAnswer(call -> call.getArgument(0));

        var response = voteService.vote(VOTER_ID, 10L, 1);

        verify(blogRepository).addVoteScore(10L, 1);
        assertThat(response.getMyVote()).isEqualTo(1);
        assertThat(response.getVoteScore()).isEqualTo(5);
    }

    @Test
    void vote_flippingUpvoteToDownvote_movesScoreByTwo() {
        when(blogService.requirePublishedBlog(10L)).thenReturn(publishedBlog());
        when(contentVoteRepository.findByUserIdAndTargetTypeAndTargetId(VOTER_ID, TargetType.BLOG, 10L))
                .thenReturn(Optional.of(existingVote(1)));
        when(contentVoteRepository.save(any(ContentVote.class))).thenAnswer(call -> call.getArgument(0));

        var response = voteService.vote(VOTER_ID, 10L, -1);

        verify(blogRepository).addVoteScore(10L, -2);
        assertThat(response.getVoteScore()).isEqualTo(2);
    }

    @Test
    void vote_sameValueTwice_doesNotTouchTheScore() {
        when(blogService.requirePublishedBlog(10L)).thenReturn(publishedBlog());
        when(contentVoteRepository.findByUserIdAndTargetTypeAndTargetId(VOTER_ID, TargetType.BLOG, 10L))
                .thenReturn(Optional.of(existingVote(1)));
        when(contentVoteRepository.save(any(ContentVote.class))).thenAnswer(call -> call.getArgument(0));

        voteService.vote(VOTER_ID, 10L, 1);

        verify(blogRepository, never()).addVoteScore(any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void vote_zeroOrOtherValue_throwsInvalidVoteValue() {
        assertThatThrownBy(() -> voteService.vote(VOTER_ID, 10L, 0))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_VOTE_VALUE);

        assertThatThrownBy(() -> voteService.vote(VOTER_ID, 10L, 5))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_VOTE_VALUE);

        verify(contentVoteRepository, never()).save(any());
    }

    // UC-04: "vote on OTHER users' posts"
    @Test
    void vote_onOwnBlog_throwsCannotVoteOwnContent() {
        when(blogService.requirePublishedBlog(10L)).thenReturn(publishedBlog());

        assertThatThrownBy(() -> voteService.vote(AUTHOR_ID, 10L, 1))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.CANNOT_VOTE_OWN_CONTENT);

        verify(contentVoteRepository, never()).save(any());
    }

    @Test
    void vote_onUnpublishedBlog_propagatesBlogNotExisted() {
        when(blogService.requirePublishedBlog(10L))
                .thenThrow(new AppException(ErrorCode.BLOG_NOT_EXISTED));

        assertThatThrownBy(() -> voteService.vote(VOTER_ID, 10L, 1))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.BLOG_NOT_EXISTED);
    }

    @Test
    void removeVote_existingUpvote_subtractsOne() {
        when(blogService.requirePublishedBlog(10L)).thenReturn(publishedBlog());
        ContentVote existing = existingVote(1);
        when(contentVoteRepository.findByUserIdAndTargetTypeAndTargetId(VOTER_ID, TargetType.BLOG, 10L))
                .thenReturn(Optional.of(existing));

        var response = voteService.removeVote(VOTER_ID, 10L);

        verify(contentVoteRepository).delete(existing);
        verify(blogRepository).addVoteScore(10L, -1);
        assertThat(response.getMyVote()).isNull();
        assertThat(response.getVoteScore()).isEqualTo(3);
    }

    // Deleting something that is not there already achieves the intended result
    @Test
    void removeVote_whenNoVoteExists_isANoOp() {
        when(blogService.requirePublishedBlog(10L)).thenReturn(publishedBlog());
        when(contentVoteRepository.findByUserIdAndTargetTypeAndTargetId(VOTER_ID, TargetType.BLOG, 10L))
                .thenReturn(Optional.empty());

        var response = voteService.removeVote(VOTER_ID, 10L);

        verify(contentVoteRepository, never()).delete(any());
        verify(blogRepository, never()).addVoteScore(any(), org.mockito.ArgumentMatchers.anyInt());
        assertThat(response.getMyVote()).isNull();
    }

    @Test
    void getMyVotes_returnsOneEntryPerRequestedBlog() {
        when(contentVoteRepository.findByUserIdAndTargetTypeAndTargetIdIn(
                VOTER_ID, TargetType.BLOG, List.of(10L, 11L)))
                .thenReturn(List.of(existingVote(1)));

        List<com.veggiepal.blog.dto.response.VoteResponse> votes =
                voteService.getMyVotes(VOTER_ID, List.of(10L, 11L));

        assertThat(votes).hasSize(2);
        assertThat(votes.getFirst().getMyVote()).isEqualTo(1);
        assertThat(votes.getLast().getMyVote()).isNull();
    }

    @Test
    void getMyVotes_emptyList_throwsBlogIdsRequired() {
        assertThatThrownBy(() -> voteService.getMyVotes(VOTER_ID, List.of()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.BLOG_IDS_REQUIRED);
    }

    @Test
    void getMyVotes_tooManyIds_throwsInvalidRequest() {
        List<Long> ids = java.util.stream.LongStream.rangeClosed(1, 101).boxed().toList();

        assertThatThrownBy(() -> voteService.getMyVotes(VOTER_ID, ids))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }
}
```

- [ ] **Step 4: Chạy test để thấy nó đỏ**

```bash
cd D:/FPT/SWP/VeggiePal/VeggiePal_BE/blog-service && ./mvnw test -Dtest=VoteServiceTest
```

Expected: FAIL khi compile.

- [ ] **Step 5: Viết `VoteService`**

```java
package com.veggiepal.blog.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veggiepal.blog.dto.response.VoteResponse;
import com.veggiepal.blog.entity.Blog;
import com.veggiepal.blog.entity.ContentVote;
import com.veggiepal.blog.enums.TargetType;
import com.veggiepal.blog.exception.AppException;
import com.veggiepal.blog.exception.ErrorCode;
import com.veggiepal.blog.repository.BlogRepository;
import com.veggiepal.blog.repository.ContentVoteRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VoteService {

    static final int MAX_BLOG_IDS = 100;

    ContentVoteRepository contentVoteRepository;
    BlogRepository blogRepository;
    BlogService blogService;

    @Transactional
    public VoteResponse vote(Long userId, Long blogId, Integer value) {

        if (value == null || (value != 1 && value != -1)) {
            throw new AppException(ErrorCode.INVALID_VOTE_VALUE);
        }

        Blog blog = requireVotableBlog(userId, blogId);

        Optional<ContentVote> existing = findVote(userId, blogId);

        Integer previous = existing.map(ContentVote::getValue).orElse(null);

        ContentVote vote = existing.orElseGet(() -> ContentVote.builder()
                .userId(userId)
                .targetType(TargetType.BLOG)
                .targetId(blogId)
                .build());

        vote.setValue(value);
        contentVoteRepository.save(vote);

        return applyDelta(blog, previous, value);
    }

    @Transactional
    public VoteResponse removeVote(Long userId, Long blogId) {

        Blog blog = blogService.requirePublishedBlog(blogId);

        Optional<ContentVote> existing = findVote(userId, blogId);

        if (existing.isEmpty()) {
            // Nothing to undo; the caller already has what they asked for
            return response(blogId, null, blog.getVoteScore());
        }

        Integer previous = existing.get().getValue();
        contentVoteRepository.delete(existing.get());

        return applyDelta(blog, previous, null);
    }

    public List<VoteResponse> getMyVotes(Long userId, List<Long> blogIds) {

        if (blogIds == null || blogIds.isEmpty()) {
            throw new AppException(ErrorCode.BLOG_IDS_REQUIRED);
        }

        if (blogIds.size() > MAX_BLOG_IDS) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        Map<Long, Integer> byBlogId = contentVoteRepository
                .findByUserIdAndTargetTypeAndTargetIdIn(userId, TargetType.BLOG, blogIds)
                .stream()
                .collect(Collectors.toMap(ContentVote::getTargetId, ContentVote::getValue));

        // One entry per requested id, so the client can index the result directly
        return blogIds.stream()
                .map(blogId -> response(blogId, byBlogId.get(blogId), null))
                .toList();
    }

    private Blog requireVotableBlog(Long userId, Long blogId) {

        Blog blog = blogService.requirePublishedBlog(blogId);

        // UC-04: members vote on OTHER people's posts
        if (blog.getAuthorId().equals(userId)) {
            throw new AppException(ErrorCode.CANNOT_VOTE_OWN_CONTENT);
        }

        return blog;
    }

    private Optional<ContentVote> findVote(Long userId, Long blogId) {

        return contentVoteRepository
                .findByUserIdAndTargetTypeAndTargetId(userId, TargetType.BLOG, blogId);
    }

    private VoteResponse applyDelta(Blog blog, Integer previous, Integer next) {

        int delta = voteDelta(previous, next);

        if (delta != 0) {
            blogRepository.addVoteScore(blog.getId(), delta);
        }

        // The JPQL update bypasses the persistence context, so add the delta here too
        return response(blog.getId(), next, blog.getVoteScore() + delta);
    }

    /**
     * The whole delta table in the spec collapses to "new minus old" once
     * "not voted" counts as zero.
     */
    static int voteDelta(Integer previous, Integer next) {

        return (next == null ? 0 : next) - (previous == null ? 0 : previous);
    }

    private static VoteResponse response(Long blogId, Integer myVote, Integer voteScore) {

        return VoteResponse.builder()
                .blogId(blogId)
                .myVote(myVote)
                .voteScore(voteScore)
                .build();
    }
}
```

Bỏ import `java.util.function.Function` nếu IDE báo không dùng.

- [ ] **Step 6: Chạy test**

```bash
cd D:/FPT/SWP/VeggiePal/VeggiePal_BE/blog-service && ./mvnw test -Dtest=VoteServiceTest
```

Expected: PASS — 9 case tham số hóa cộng 11 test.

- [ ] **Step 7: Thêm 3 endpoint vote vào `BlogController`**

Thêm field:

```java
    VoteService voteService;
```

```java
    @Operation(summary = "Set your vote on a blog; 1 or -1")
    @PutMapping("/{id}/vote")
    ApiResponse<VoteResponse> vote(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") Long id,
            @RequestBody @Valid VoteRequest request
    ) {

        return ApiResponse
                .<VoteResponse>builder()
                .result(voteService.vote(CurrentUser.id(jwt), id, request.getValue()))
                .build();
    }

    @Operation(summary = "Remove your vote; doing it twice is harmless")
    @DeleteMapping("/{id}/vote")
    ApiResponse<VoteResponse> removeVote(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") Long id
    ) {

        return ApiResponse
                .<VoteResponse>builder()
                .result(voteService.removeVote(CurrentUser.id(jwt), id))
                .build();
    }

    @Operation(summary = "My vote on a list of blogs, to overlay on a public listing")
    @GetMapping("/me/votes")
    ApiResponse<List<VoteResponse>> getMyVotes(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "blogIds") List<Long> blogIds
    ) {

        return ApiResponse
                .<List<VoteResponse>>builder()
                .result(voteService.getMyVotes(CurrentUser.id(jwt), blogIds))
                .build();
    }
```

Import thêm: `com.veggiepal.blog.dto.request.VoteRequest`, `com.veggiepal.blog.dto.response.VoteResponse`, `com.veggiepal.blog.service.VoteService`.

- [ ] **Step 8: Thêm test controller cho vote**

Thêm `@MockitoBean VoteService voteService;` vào `BlogControllerTest`, rồi:

```java
    @Test
    void vote_withoutToken_returnsUnauthenticated() throws Exception {
        mockMvc.perform(put("/blogs/10/vote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"value": 1}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1008));
    }

    @Test
    void vote_withToken_passesValueThrough() throws Exception {
        when(voteService.vote(USER_ID, 10L, 1))
                .thenReturn(VoteResponse.builder().blogId(10L).myVote(1).voteScore(5).build());

        mockMvc.perform(put("/blogs/10/vote").with(member())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"value": 1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.voteScore").value(5));
    }

    @Test
    void vote_missingValue_returnsInvalidVoteValue() throws Exception {
        mockMvc.perform(put("/blogs/10/vote").with(member())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(3040));
    }

    // /blogs/me/votes is two segments deep; it must not be swallowed by /blogs/{id}
    @Test
    void getMyVotes_withoutToken_returnsUnauthenticated() throws Exception {
        mockMvc.perform(get("/blogs/me/votes").param("blogIds", "10,11"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1008));
    }

    @Test
    void getMyVotes_withToken_reachesTheService() throws Exception {
        when(voteService.getMyVotes(USER_ID, List.of(10L, 11L)))
                .thenReturn(List.of(VoteResponse.builder().blogId(10L).myVote(1).build()));

        mockMvc.perform(get("/blogs/me/votes").param("blogIds", "10,11").with(member()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].myVote").value(1));
    }
```

Import thêm: `static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put`, `com.veggiepal.blog.dto.response.VoteResponse`, `com.veggiepal.blog.service.VoteService`.

- [ ] **Step 9: Chạy toàn bộ test**

```bash
cd D:/FPT/SWP/VeggiePal/VeggiePal_BE/blog-service && ./mvnw test -Dtest='!BlogServiceApplicationTests'
```

Expected: PASS hết.

- [ ] **Step 10: Checkpoint**

```
feat(blog): add upvote/downvote with denormalized vote score
```

---

## Task 9: identity-service — `GET /users/batch`

blog-service chỉ lưu `author_id`. Để hiện tên và avatar tác giả, FE gọi identity-service một lần cho cả trang.

**Files:**
- Create: `identity-service/src/main/java/com/veggiepal/dto/response/PublicUserResponse.java`
- Create: `.../service/PublicUserService.java`
- Create: `.../controller/PublicUserController.java`
- Modify: `.../repository/UserRepository.java`
- Modify: `.../mapper/UserMapper.java`
- Modify: `.../configuration/SecurityConfig.java`
- Test: `.../service/PublicUserServiceTest.java`, `.../controller/PublicUserControllerTest.java`

**Interfaces:**
- Consumes: `User` entity, `UserRepository`, `UserStatus.ACTIVE`
- Produces: `GET /users/batch?ids=1,2,3 → ApiResponse<List<PublicUserResponse>>` với `id`, `fullName`, `avatarUrl`

- [ ] **Step 1: Viết DTO và query**

`identity-service/.../dto/response/PublicUserResponse.java`:

```java
package com.veggiepal.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

/** The only user fields that appear next to public content. No email, no phone. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PublicUserResponse {

    Long id;

    String fullName;

    String avatarUrl;
}
```

Thêm vào `identity-service/.../repository/UserRepository.java`:

```java
    List<User> findByIdInAndStatus(Collection<Long> ids, UserStatus status);
```

Import: `java.util.Collection`, `java.util.List`, `com.veggiepal.enums.UserStatus`.

Thêm vào `identity-service/.../mapper/UserMapper.java`:

```java
    PublicUserResponse toPublicUserResponse(User user);
```

Import: `com.veggiepal.dto.response.PublicUserResponse`.

- [ ] **Step 2: Viết test (test trước)**

`identity-service/src/test/java/com/veggiepal/service/PublicUserServiceTest.java`:

```java
package com.veggiepal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.veggiepal.dto.response.PublicUserResponse;
import com.veggiepal.entity.User;
import com.veggiepal.enums.UserStatus;
import com.veggiepal.exception.AppException;
import com.veggiepal.exception.ErrorCode;
import com.veggiepal.mapper.UserMapper;
import com.veggiepal.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class PublicUserServiceTest {

    @Mock
    UserRepository userRepository;

    @Spy
    UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    @InjectMocks
    PublicUserService publicUserService;

    static User user(Long id, String fullName) {
        return User.builder()
                .id(id).email("a@b.com").fullName(fullName)
                .avatarUrl("http://minio/avatars/" + id + ".png")
                .status(UserStatus.ACTIVE)
                .build();
    }

    @Test
    void getPublicUsers_returnsOnlyNameAndAvatar() {
        when(userRepository.findByIdInAndStatus(List.of(1L), UserStatus.ACTIVE))
                .thenReturn(List.of(user(1L, "Long Nguyễn")));

        List<PublicUserResponse> result = publicUserService.getPublicUsers(List.of(1L));

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getFullName()).isEqualTo("Long Nguyễn");
        assertThat(result.getFirst().getAvatarUrl()).isNotNull();
    }

    // A missing or suspended account is simply absent; asking for it is not an error
    @Test
    void getPublicUsers_unknownId_isSkippedSilently() {
        when(userRepository.findByIdInAndStatus(List.of(1L, 99L), UserStatus.ACTIVE))
                .thenReturn(List.of(user(1L, "Long Nguyễn")));

        assertThat(publicUserService.getPublicUsers(List.of(1L, 99L))).hasSize(1);
    }

    @Test
    void getPublicUsers_emptyList_throwsInvalidRequest() {
        assertThatThrownBy(() -> publicUserService.getPublicUsers(List.of()))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);

        verify(userRepository, never()).findByIdInAndStatus(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    // Without a cap, one request could walk the whole users table
    @Test
    void getPublicUsers_moreThanFiftyIds_throwsInvalidRequest() {
        List<Long> ids = java.util.stream.LongStream.rangeClosed(1, 51).boxed().toList();

        assertThatThrownBy(() -> publicUserService.getPublicUsers(ids))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST);
    }
}
```

- [ ] **Step 3: Chạy test để thấy nó đỏ**

```bash
cd D:/FPT/SWP/VeggiePal/VeggiePal_BE/identity-service && ./mvnw test -Dtest=PublicUserServiceTest
```

Expected: FAIL khi compile.

- [ ] **Step 4: Viết `PublicUserService`**

`identity-service/.../service/PublicUserService.java`:

```java
package com.veggiepal.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.veggiepal.dto.response.PublicUserResponse;
import com.veggiepal.enums.UserStatus;
import com.veggiepal.exception.AppException;
import com.veggiepal.exception.ErrorCode;
import com.veggiepal.mapper.UserMapper;
import com.veggiepal.repository.UserRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Lets other services' clients resolve an author id into a display name.
 * blog-service stores only author_id and never calls here itself.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PublicUserService {

    static final int MAX_IDS = 50;

    UserRepository userRepository;
    UserMapper userMapper;

    public List<PublicUserResponse> getPublicUsers(List<Long> ids) {

        if (ids == null || ids.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        // This endpoint is public: a cap keeps it from being a users-table dump
        if (ids.size() > MAX_IDS) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        // Suspended and pending accounts stay invisible
        return userRepository
                .findByIdInAndStatus(ids, UserStatus.ACTIVE)
                .stream()
                .map(userMapper::toPublicUserResponse)
                .toList();
    }
}
```

- [ ] **Step 5: Chạy test**

```bash
cd D:/FPT/SWP/VeggiePal/VeggiePal_BE/identity-service && ./mvnw test -Dtest=PublicUserServiceTest
```

Expected: PASS.

- [ ] **Step 6: Viết controller và mở public**

`identity-service/.../controller/PublicUserController.java`:

```java
package com.veggiepal.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.veggiepal.dto.response.ApiResponse;
import com.veggiepal.dto.response.PublicUserResponse;
import com.veggiepal.service.PublicUserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Public User", description = "Display names and avatars shown next to public content")
public class PublicUserController {

    PublicUserService publicUserService;

    @Operation(summary = "Resolve up to 50 author ids into display names and avatars")
    @GetMapping("/batch")
    ApiResponse<List<PublicUserResponse>> getPublicUsers(
            @RequestParam("ids") List<Long> ids
    ) {

        return ApiResponse
                .<List<PublicUserResponse>>builder()
                .result(publicUserService.getPublicUsers(ids))
                .build();
    }
}
```

Trong `identity-service/.../configuration/SecurityConfig.java`, thêm vào mảng `PUBLIC_ENDPOINTS`:

```java
            "/users/batch",
```

`/users/batch` và `/users/me` đều là segment chữ nên không khớp chéo nhau — identity-service không có `/users/{id}`, nên ở đây không dính cái bẫy pattern như blog-service.

- [ ] **Step 7: Viết controller slice test**

`identity-service/src/test/java/com/veggiepal/controller/PublicUserControllerTest.java`:

```java
package com.veggiepal.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.veggiepal.configuration.JwtConfig;
import com.veggiepal.configuration.SecurityConfig;
import com.veggiepal.configuration.SecurityExceptionHandler;
import com.veggiepal.dto.response.PublicUserResponse;
import com.veggiepal.exception.AppException;
import com.veggiepal.exception.ErrorCode;
import com.veggiepal.service.PublicUserService;

@WebMvcTest(PublicUserController.class)
@Import({SecurityConfig.class, JwtConfig.class, SecurityExceptionHandler.class})
class PublicUserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    PublicUserService publicUserService;

    @Test
    void getPublicUsers_withoutToken_isPublic() throws Exception {
        when(publicUserService.getPublicUsers(List.of(1L, 2L)))
                .thenReturn(List.of(PublicUserResponse.builder()
                        .id(1L).fullName("Long Nguyễn").avatarUrl("http://minio/a.png").build()));

        mockMvc.perform(get("/users/batch").param("ids", "1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].fullName").value("Long Nguyễn"));
    }

    // A stale token in the header must not turn a public endpoint into a 401
    @Test
    void getPublicUsers_withGarbageToken_stillPublic() throws Exception {
        when(publicUserService.getPublicUsers(List.of(1L)))
                .thenReturn(List.of());

        mockMvc.perform(get("/users/batch").param("ids", "1")
                        .header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isOk());
    }

    @Test
    void getPublicUsers_missingIdsParam_returnsInvalidRequest() throws Exception {
        mockMvc.perform(get("/users/batch"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPublicUsers_tooManyIds_returnsInvalidRequest() throws Exception {
        when(publicUserService.getPublicUsers(List.of(1L)))
                .thenThrow(new AppException(ErrorCode.INVALID_REQUEST));

        mockMvc.perform(get("/users/batch").param("ids", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1018));
    }

    @Test
    void getPublicUsers_neverLeaksEmail() throws Exception {
        when(publicUserService.getPublicUsers(List.of(1L)))
                .thenReturn(List.of(PublicUserResponse.builder()
                        .id(1L).fullName("Long Nguyễn").build()));

        mockMvc.perform(get("/users/batch").param("ids", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].email").doesNotExist());
    }
```

Đóng ngoặc class:

```java
}
```

**Lưu ý về test `getPublicUsers_missingIdsParam_*`:** thiếu `@RequestParam` bắt buộc sẽ ném `MissingServletRequestParameterException`. `GlobalExceptionHandler` hiện chỉ bắt `HttpMessageNotReadableException` và `MethodArgumentTypeMismatchException`, nên trường hợp này rơi vào handler `Exception` chung và trả 500 chứ không phải 400. Nếu test đỏ vì lý do đó, thêm `MissingServletRequestParameterException.class` vào danh sách `@ExceptionHandler` của `handlingInvalidRequest` trong identity-service:

```java
    @ExceptionHandler(value = {
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
```

Import `org.springframework.web.bind.MissingServletRequestParameterException`. Làm tương tự cho `GlobalExceptionHandler` của blog-service, vì `GET /blogs/me/votes` và `GET /comments` cũng có tham số bắt buộc.

- [ ] **Step 8: Chạy test identity-service**

```bash
cd D:/FPT/SWP/VeggiePal/VeggiePal_BE/identity-service && ./mvnw test -Dtest='!VeggiepalApplicationTests'
```

Expected: PASS hết, kể cả các test cũ.

- [ ] **Step 9: Checkpoint**

```
feat(identity): expose public display name and avatar lookup by id batch
```

---

## Task 10: Gateway, tài liệu và kiểm thử tay

**Files:**
- Modify: `api-gateway/src/main/resources/application.yaml`
- Modify: `CLAUDE.md`

**Interfaces:**
- Consumes: Task 1–9
- Produces: `/api/blogs/**`, `/api/categories/**`, `/api/comments/**` đi được qua gateway; Swagger tổng có mục Blog Service

- [ ] **Step 1: Thêm 4 route vào gateway**

Trong `api-gateway/src/main/resources/application.yaml`, thêm vào dưới khối NUTRITION SERVICE:

```yaml
            # =========================
            # BLOG SERVICE
            # =========================
            - id: blog-service-blogs
              uri: http://localhost:8083
              predicates:
                - Path=/api/blogs/**
              filters:
                - StripPrefix=1

            - id: blog-service-categories
              uri: http://localhost:8083
              predicates:
                - Path=/api/categories/**
              filters:
                - StripPrefix=1

            - id: blog-service-comments
              uri: http://localhost:8083
              predicates:
                - Path=/api/comments/**
              filters:
                - StripPrefix=1

            - id: blog-service-docs
              uri: http://localhost:8083
              predicates:
                - Path=/blog-service/v3/api-docs/**
              filters:
                - StripPrefix=1
```

Và thêm vào `springdoc.swagger-ui.urls`:

```yaml
      - name: Blog Service
        url: /blog-service/v3/api-docs
```

**Không** thêm `spring.servlet.multipart.*` vào gateway — gateway tự tắt multipart parsing để upload ảnh bìa stream thẳng xuống service.

- [ ] **Step 2: Chạy cả bốn tiến trình**

Bốn terminal riêng:

```bash
cd D:/FPT/SWP/VeggiePal/VeggiePal_BE && docker compose up -d
```

```bash
cd D:/FPT/SWP/VeggiePal/VeggiePal_BE/identity-service && ./mvnw spring-boot:run
```

```bash
cd D:/FPT/SWP/VeggiePal/VeggiePal_BE/blog-service && ./mvnw spring-boot:run
```

```bash
cd D:/FPT/SWP/VeggiePal/VeggiePal_BE/api-gateway && ./mvnw spring-boot:run
```

- [ ] **Step 3: Kiểm tra Swagger tổng**

Mở http://localhost:8080/swagger-ui.html — dropdown phải có **Blog Service**, và các endpoint phải hiện dưới dạng `/api/blogs`, `/api/categories`, `/api/comments`.

- [ ] **Step 4: Kiểm thử tay hết một vòng nghiệp vụ**

Lấy token admin và token member bằng `POST /api/auth/login`. Nếu chưa có tài khoản admin, tạo một user rồi sửa role trong DB:

```bash
docker exec veggiepal-mysql mysql -uroot -p12345 -e "UPDATE veggiepal_identity.users SET role='ADMIN' WHERE email='admin@veggiepal.com'"
```

Lưu ý: `role` là cột `ENUM` native, `ADMIN` đã có sẵn nên không cần `ALTER TABLE`.

Chạy lần lượt, mỗi bước kiểm tra kết quả:

| # | Gọi | Kỳ vọng |
|---|---|---|
| 1 | `POST /api/categories` (admin) `{"name":"Công thức","type":"RECIPE_TYPE"}` | 200, có `id` |
| 2 | `POST /api/categories` (member) | 403, `code: 1009` |
| 3 | `POST /api/categories` (admin) `{"name":"Món chính","parentId":1}` | 200, `type` là `RECIPE_TYPE` dù không gửi |
| 4 | `POST /api/categories` (admin) `{"name":"Món xào","parentId":2}` | 400, `code: 3006` |
| 5 | `GET /api/categories` (không token) | 200, cây 2 cấp |
| 6 | `POST /api/blogs` (member A) `publish: true` | 200, `status: PUBLISHED` |
| 7 | `GET /api/blogs` (không token) | 200, thấy bài vừa đăng |
| 8 | `GET /api/blogs/1` hai lần | `viewCount` tăng mỗi lần |
| 9 | `GET /api/blogs?keyword=đậu` | 200, khớp theo title/content |
| 10 | `GET /api/blogs?keyword=xyzkhongco` | 200, `items` rỗng — **không** phải 404 |
| 11 | `POST /api/blogs/1/thumbnail` (member A, file PNG) | 200, `thumbnailUrl` trỏ vào MinIO, mở được bằng trình duyệt |
| 12 | `PUT /api/blogs/1/vote` (member A — chính chủ) `{"value":1}` | 400, `code: 3041` |
| 13 | `PUT /api/blogs/1/vote` (member B) `{"value":1}` | 200, `voteScore: 1` |
| 14 | `PUT /api/blogs/1/vote` (member B) `{"value":-1}` | 200, `voteScore: -1` |
| 15 | `DELETE /api/blogs/1/vote` (member B) | 200, `voteScore: 0` |
| 16 | `DELETE /api/blogs/1/vote` (member B) lần nữa | 200, `voteScore: 0` — idempotent |
| 17 | `GET /api/blogs/me/votes?blogIds=1` (member B) | 200, `myVote: null` |
| 18 | `GET /api/blogs/me` **không token** | 401, `code: 1008` — **không** phải 404 |
| 19 | `POST /api/comments` (member B) trên blog 1 | 200, `content` hiện ra |
| 20 | `POST /api/comments` với `parentCommentId` của một reply | 400, `code: 3033` |
| 21 | `GET /api/comments?targetId=1` (không token) | 200, `replyCount` đúng |
| 22 | `DELETE /api/comments/1` (admin) | 200 |
| 23 | `GET /api/comments?targetId=1` lại | comment vẫn còn, `content: null`, `deleted: true` |
| 24 | `GET /api/users/batch?ids=1,2` (không token) | 200, có `fullName`, **không** có `email` |
| 25 | `DELETE /api/categories/1` (admin) | 400, `code: 3005` — còn blog dùng |

Bước 18 là bằng chứng cuối cùng cho `{id:[0-9]+}`. Nếu nó trả 404 hoặc 500 thay vì 401, quay lại `SecurityConfig` ở Task 2.

- [ ] **Step 5: Cập nhật `CLAUDE.md`**

Thêm `blog-service` vào phần Overview:

```markdown
VeggiePal backend: Spring Boot microservices (Java 21, Spring Boot 4.1.1). There is **no parent/aggregator POM**. Each service (`api-gateway/`, `identity-service/`, `nutrition-service/`, `blog-service/`) is a separate Maven project with its own wrapper, so run Maven commands from inside the service directory.
```

Thêm vào khối lệnh Commands:

```markdown
cd blog-service && ./mvnw spring-boot:run         # :8083
```

```markdown
./mvnw test -Dtest='!BlogServiceApplicationTests'             # blog-service: same
```

Thêm vào Database gotchas: `blog-service` tự tạo `veggiepal_blog`, và `minio-init` giờ tạo hai bucket.

Thêm route mới vào phần Request flow:

```markdown
- Routes (all `StripPrefix=1`): `/api/auth/**` and `/api/users/**` → identity-service (8081); `/api/nutrition/**` → nutrition-service (8082); `/api/blogs/**`, `/api/categories/**`, `/api/comments/**` → blog-service (8083).
```

Thêm một mục mới sau phần nutrition-service:

```markdown
### blog-service

Same conventions as identity-service, under package `com.veggiepal.blog` (shared classes are copies, not a shared module). Controllers map `/blogs/**`, `/categories/**`, `/comments/**`. It owns blogs, the category tree, comments and votes, and stores only `author_id` from the JWT — the frontend resolves display names through identity-service's `GET /users/batch`.

- **Comments and votes are polymorphic** (`target_type` + `target_id`) so videos slot in without a migration. `TargetType.VIDEO` and `CommentStatus.PENDING` already exist in the enums for the same reason — `ddl-auto=update` cannot add an ENUM constant later.
- **`SecurityConfig.PUBLIC_ENDPOINTS` here is method-aware** and its path variables are constrained to digits (`/blogs/{id:[0-9]+}`). Without the digits, `/blogs/me` matches `/blogs/{id}`, becomes public, loses its bearer token and then 401s forever. `SecurityConfigTest` guards this.
- **Moderation is a stub.** `ContentModerationService` has one implementation, `AutoApproveContentModerationService`. BR-02 is wired but not really enforced until an AI implementation replaces it.
- **`blogs.vote_score` is denormalized**, kept in sync inside the vote transaction with `UPDATE blogs SET vote_score = vote_score + :delta`. The delta is just `new value - old value`, treating "no vote" as 0.
- Admin has no separate controller: ownership checks widen to `ROLE_ADMIN` on blog and comment `PUT`/`DELETE`.
```

- [ ] **Step 6: Chạy toàn bộ test của cả ba service**

```bash
cd D:/FPT/SWP/VeggiePal/VeggiePal_BE/blog-service && ./mvnw test -Dtest='!BlogServiceApplicationTests'
```

```bash
cd D:/FPT/SWP/VeggiePal/VeggiePal_BE/identity-service && ./mvnw test -Dtest='!VeggiepalApplicationTests'
```

```bash
cd D:/FPT/SWP/VeggiePal/VeggiePal_BE/nutrition-service && ./mvnw test -Dtest='!NutritionServiceApplicationTests'
```

Expected: PASS hết. nutrition-service không bị đụng tới nhưng vẫn chạy để chắc chắn không có gì vỡ lây.

- [ ] **Step 7: Checkpoint cuối**

```
chore(gateway): route blog APIs, aggregate blog docs and document blog-service
```

---

## Kiểm tra sau khi xong

Chạy hết plan rồi thì đối chiếu lại với spec:

| Yêu cầu trong spec | Đã có ở |
|---|---|
| FR-02-01, 02-02 — khách search và đọc blog | Task 5 |
| FR-03-01 — tạo blog | Task 4 |
| FR-03-03, 03-04 — sửa/xóa bài của mình | Task 4 |
| FR-03-05 — qua moderation trước khi công khai | Task 4 (`applyModeration`) |
| FR-04-01 — bình luận | Task 7 |
| FR-04-02 — upvote/downvote | Task 8 |
| FR-04-03 — không vote trùng | Task 8 (UNIQUE constraint) |
| FR-05-01, 05-02 — tìm theo từ khóa | Task 5 |
| FR-05-03 — nội dung liên quan | Task 5 (`/related`, bản không-AI) |
| FR-05-04 — không có kết quả | Task 5 (page rỗng, 200) |
| FR-10-03, 10-04 — admin gỡ nội dung | Task 4 + Task 7 (cờ `admin`) |
| FR-11-01..03 — admin CRUD danh mục | Task 3 + Task 4 Step 12 |
| FR-12-01, 12-02 — điểm cắm moderation | Task 4 |
| BR-07 — kiểm tra sở hữu | Task 4, Task 7 |
| BR-08 — chỉ admin sửa danh mục | Task 3 (`@PreAuthorize`) |
| Tác giả hiển thị qua identity-service | Task 9 |
| Gateway + Swagger tổng | Task 10 |

**Cố ý chưa làm** (đã ghi ở mục 2 của spec): video, `moderation_cases`, hàng chờ kiểm duyệt của admin, `admin_logs`, AI thật, `recipes`, FULLTEXT search, integration test có DB.

---

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-09-20-blog-service.md`. Two execution options:

**1. Subagent-Driven (recommended)** — tôi dispatch một subagent mới cho mỗi task, review giữa các task, vòng lặp nhanh.

**2. Inline Execution** — chạy các task ngay trong session này bằng executing-plans, thực thi theo lô với checkpoint để review.

Which approach?
