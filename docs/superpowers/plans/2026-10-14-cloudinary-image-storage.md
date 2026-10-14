# Cloudinary Image Storage Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Thay MinIO/S3 bằng Cloudinary để lưu avatar (identity-service) và thumbnail blog (blog-service), credential đọc từ `.env` ở gốc repo.

**Architecture:** Giữ nguyên interface `FileStorageService` ở mỗi service, thay implementation `S3FileStorageService` bằng `CloudinaryFileStorageService` dùng Cloudinary Java SDK. Spring Boot nạp `.env` qua `spring.config.import`. Gỡ MinIO khỏi docker-compose.

**Tech Stack:** Java 21, Spring Boot 4.1.1, `com.cloudinary:cloudinary-http5:2.5.0`, JUnit 5 + Mockito + AssertJ.

**Spec:** `docs/superpowers/specs/2026-10-14-cloudinary-image-storage-design.md`

## Global Constraints

- **Không chạy `git commit`.** User tự review và commit. Mỗi task kết thúc bằng checkpoint báo cáo các file đã đổi.
- **Không bao giờ ghi giá trị thật của `CLOUDINARY_API_KEY` / `CLOUDINARY_API_SECRET`** vào file được git track (plan, spec, docs, properties, test). Giá trị thật chỉ nằm trong `.env`.
- Dependency: `com.cloudinary:cloudinary-http5`, version `2.5.0`.
- Tên biến môi trường: `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`. Prefix property: `storage.cloudinary`.
- Interface `FileStorageService` giữ nguyên chữ ký: `String upload(String key, byte[] content, String contentType)` và `void delete(String url)`.
- Mọi lỗi từ SDK (`IOException`, `RuntimeException`) → `new AppException(ErrorCode.FILE_UPLOAD_FAILED)` (1017, HTTP 503).
- Maven chạy **trong thư mục service**: `./mvnw` (Git Bash) hoặc `.\mvnw.cmd` (PowerShell). Không có parent POM.
- Test cần DB (`VeggiepalApplicationTests`, `BlogServiceApplicationTests`, `BlogServiceIntegrationTests`) cần MySQL: `docker compose up -d mysql` ở gốc repo.

## File Structure

| File | Trách nhiệm |
|---|---|
| `.gitignore` | Chặn `.env` |
| `.env.example` (mới, tracked) | Template ba biến Cloudinary, giá trị rỗng |
| `.env` (mới, **không** tracked) | Credential thật |
| `identity-service/pom.xml`, `blog-service/pom.xml` | Bỏ AWS SDK, thêm Cloudinary |
| `*/configuration/StorageProperties.java` | Record `storage.cloudinary.*` |
| `*/configuration/CloudinaryConfig.java` (mới) | Bean `Cloudinary` |
| `*/configuration/S3Config.java` | Xoá |
| `*/service/CloudinaryFileStorageService.java` (mới) | Upload/delete trên Cloudinary |
| `*/service/S3FileStorageService.java` | Xoá |
| `*/src/main/resources/application.properties` | Import `.env`, property Cloudinary |
| `identity-service/.../S3FileStorageServiceTest.java` | Xoá |
| `*/service/CloudinaryFileStorageServiceTest.java` (mới) | Unit test |
| `docker-compose.yml` | Gỡ MinIO, `env_file` cho identity-service |
| `README.md`, `CLAUDE.md`, `docs/identity-service-guide.md`, `docs/blog-service-guide.md` | Cập nhật tài liệu |

Ngoài phạm vi: `IMPLEMENTATION_PHASES*.md` (roadmap lịch sử) và các spec/plan cũ trong `docs/superpowers/`. Fixture URL MinIO trong `ProfileServiceTest`, `BlogServiceTest`, `PublicUserServiceTest` và `PublicUserControllerTest` cũng giữ nguyên, vì chúng chỉ là chuỗi bất kỳ trên mock.

---

### Task 1: Secret plumbing (`.gitignore`, `.env.example`, `.env`)

> Controller tự làm task này, **không** giao cho subagent: subagent không có secret, và secret không được ghi vào plan. Giá trị thật lấy từ tin nhắn của user trong phiên chat.

**Files:**
- Modify: `.gitignore` (thêm vào cuối file)
- Create: `.env.example`
- Create: `.env` (không tracked)

**Interfaces:**
- Produces: các biến `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` trong `VeggiePal_BE/.env`. Task 2, 3 và 4 dùng các biến này.

- [ ] **Step 1: Chặn `.env` trong `.gitignore`**

Thêm vào cuối `.gitignore`:

```gitignore

### Secrets ###
.env
```

- [ ] **Step 2: Kiểm tra `.env` đã bị ignore TRƯỚC khi tạo file**

Run: `git check-ignore -v .env`
Expected: `.gitignore:<số dòng>:.env	.env`. Nếu không in gì thì DỪNG, chưa được tạo `.env`.

- [ ] **Step 3: Tạo `.env.example`** (được commit, không chứa secret)

```properties
# Copy to .env (same folder) and fill in. .env is gitignored - never commit it.
# Cloudinary Console -> Settings -> API Keys
CLOUDINARY_CLOUD_NAME=
CLOUDINARY_API_KEY=
CLOUDINARY_API_SECRET=
```

- [ ] **Step 4: Tạo `.env`** cùng định dạng, điền ba giá trị thật user đã đưa (`cloudname`, `apikey`, `apisecret`). Không có khoảng trắng quanh `=` và không có dấu nháy.

- [ ] **Step 5: Kiểm tra**

Run: `git status --porcelain`
Expected: có `.gitignore` (M) và `.env.example` (??), **không** có `.env`.

- [ ] **Step 6: Checkpoint.** Báo user các file đã đổi. Không commit.

---

### Task 2: identity-service chuyển sang Cloudinary

**Files:**
- Modify: `identity-service/pom.xml` (property `aws-sdk.version` dòng 26; dependency S3 dòng 118-122; khối `<dependencyManagement>` dòng 145-155)
- Modify: `identity-service/src/main/java/com/veggiepal/configuration/StorageProperties.java`
- Delete: `identity-service/src/main/java/com/veggiepal/configuration/S3Config.java`
- Create: `identity-service/src/main/java/com/veggiepal/configuration/CloudinaryConfig.java`
- Delete: `identity-service/src/main/java/com/veggiepal/service/S3FileStorageService.java`
- Create: `identity-service/src/main/java/com/veggiepal/service/CloudinaryFileStorageService.java`
- Modify: `identity-service/src/main/resources/application.properties` (khối `storage.s3.*`)
- Delete: `identity-service/src/test/java/com/veggiepal/service/S3FileStorageServiceTest.java`
- Create: `identity-service/src/test/java/com/veggiepal/service/CloudinaryFileStorageServiceTest.java`

**Interfaces:**
- Consumes: `FileStorageService` (giữ nguyên), `AppException(ErrorCode)`, `AppException#getErrorCode()`, `ErrorCode.FILE_UPLOAD_FAILED`; biến `CLOUDINARY_*` từ Task 1.
- Produces: `record StorageProperties(String cloudName, String apiKey, String apiSecret)`; bean `com.cloudinary.Cloudinary`; `CloudinaryFileStorageService(Cloudinary, StorageProperties)` (constructor do Lombok `@RequiredArgsConstructor` sinh, theo thứ tự field).

- [ ] **Step 1: Đổi dependency trong `identity-service/pom.xml`**

Trong `<properties>`, thay

```xml
		<aws-sdk.version>2.55.0</aws-sdk.version>
```

bằng

```xml
		<cloudinary.version>2.5.0</cloudinary.version>
```

Thay khối dependency

```xml
		<!-- S3 API: MinIO locally, AWS S3 in production -->
		<dependency>
			<groupId>software.amazon.awssdk</groupId>
			<artifactId>s3</artifactId>
		</dependency>
```

bằng

```xml
		<!-- Image storage: Cloudinary -->
		<dependency>
			<groupId>com.cloudinary</groupId>
			<artifactId>cloudinary-http5</artifactId>
			<version>${cloudinary.version}</version>
		</dependency>
```

Xoá toàn bộ khối sau, vì nó chỉ chứa AWS BOM:

```xml
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

```

- [ ] **Step 2: Xoá code S3 và viết lại `StorageProperties`**

Run (từ gốc repo):

```bash
git rm -q identity-service/src/main/java/com/veggiepal/configuration/S3Config.java \
          identity-service/src/main/java/com/veggiepal/service/S3FileStorageService.java \
          identity-service/src/test/java/com/veggiepal/service/S3FileStorageServiceTest.java
```

(`git rm` chỉ stage việc xoá, không commit.)

Ghi đè `identity-service/src/main/java/com/veggiepal/configuration/StorageProperties.java`:

```java
package com.veggiepal.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage.cloudinary")
public record StorageProperties(
        String cloudName,
        String apiKey,
        String apiSecret
) {
}
```

- [ ] **Step 3: Viết test lỗi**

Tạo `identity-service/src/test/java/com/veggiepal/service/CloudinaryFileStorageServiceTest.java`:

```java
package com.veggiepal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.veggiepal.configuration.StorageProperties;
import com.veggiepal.exception.AppException;
import com.veggiepal.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class CloudinaryFileStorageServiceTest {

    private static final StorageProperties PROPERTIES =
            new StorageProperties("demo-cloud", "api-key", "api-secret");

    private static final String IMAGE_URL =
            "https://res.cloudinary.com/demo-cloud/image/upload/v1712345678/avatars/7/a.png";

    @Mock
    Cloudinary cloudinary;

    @Mock
    Uploader uploader;

    CloudinaryFileStorageService storageService;

    @BeforeEach
    void setUp() {
        // lenient: the null/foreign URL case never reaches the SDK
        lenient().when(cloudinary.uploader()).thenReturn(uploader);
        storageService = new CloudinaryFileStorageService(cloudinary, PROPERTIES);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void upload_sendsPublicIdWithoutExtensionAndReturnsSecureUrl() throws IOException {
        byte[] content = {1, 2, 3};
        when(uploader.upload(eq(content), anyMap())).thenReturn(Map.of("secure_url", IMAGE_URL));

        String url = storageService.upload("avatars/7/a.png", content, "image/png");

        ArgumentCaptor<Map> options = ArgumentCaptor.forClass(Map.class);
        verify(uploader).upload(eq(content), options.capture());
        assertThat((Map<String, Object>) options.getValue())
                .containsEntry("public_id", "avatars/7/a")
                .containsEntry("resource_type", "image")
                .containsEntry("overwrite", false);
        assertThat(url).isEqualTo(IMAGE_URL);
    }

    @Test
    void upload_networkFailure_throwsFileUploadFailed() throws IOException {
        when(uploader.upload(any(), anyMap())).thenThrow(new IOException("network down"));

        assertThatThrownBy(() -> storageService.upload("avatars/7/a.png", new byte[]{1}, "image/png"))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FILE_UPLOAD_FAILED));
    }

    @Test
    void upload_apiError_throwsFileUploadFailed() throws IOException {
        // The SDK reports API errors (bad credentials, missing cloud_name) as RuntimeException
        when(uploader.upload(any(), anyMap())).thenThrow(new RuntimeException("Invalid Signature"));

        assertThatThrownBy(() -> storageService.upload("avatars/7/a.png", new byte[]{1}, "image/png"))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FILE_UPLOAD_FAILED));
    }

    @Test
    void delete_versionedUrl_destroysByPublicId() throws IOException {
        storageService.delete(IMAGE_URL);

        verify(uploader).destroy(eq("avatars/7/a"), anyMap());
    }

    @Test
    void delete_unversionedUrl_destroysByPublicId() throws IOException {
        storageService.delete("https://res.cloudinary.com/demo-cloud/image/upload/avatars/7/a.png");

        verify(uploader).destroy(eq("avatars/7/a"), anyMap());
    }

    @Test
    void delete_nullOrForeignUrl_doesNothing() {
        storageService.delete(null);
        storageService.delete("http://localhost:9000/veggiepal-avatars/avatars/7/a.png");
        storageService.delete("https://res.cloudinary.com/other-cloud/image/upload/v1/avatars/7/a.png");

        verifyNoInteractions(uploader);
    }

    @Test
    void delete_storageFailure_throwsFileUploadFailed() throws IOException {
        when(uploader.destroy(anyString(), anyMap())).thenThrow(new IOException("network down"));

        assertThatThrownBy(() -> storageService.delete(IMAGE_URL))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FILE_UPLOAD_FAILED));
    }
}
```

- [ ] **Step 4: Chạy test để xác nhận nó lỗi**

Run (trong `identity-service/`): `./mvnw test -Dtest=CloudinaryFileStorageServiceTest`
Expected: `COMPILATION ERROR` với `cannot find symbol ... class CloudinaryFileStorageService`.

- [ ] **Step 5: Viết `CloudinaryConfig`**

Tạo `identity-service/src/main/java/com/veggiepal/configuration/CloudinaryConfig.java`:

```java
package com.veggiepal.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class CloudinaryConfig {

    @Bean
    Cloudinary cloudinary(StorageProperties storageProperties) {

        // No network call here: empty credentials still build a client, and only uploads fail
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", storageProperties.cloudName(),
                "api_key", storageProperties.apiKey(),
                "api_secret", storageProperties.apiSecret(),
                "secure", true
        ));
    }
}
```

- [ ] **Step 6: Viết `CloudinaryFileStorageService`**

Tạo `identity-service/src/main/java/com/veggiepal/service/CloudinaryFileStorageService.java`:

```java
package com.veggiepal.service;

import java.io.IOException;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.veggiepal.configuration.StorageProperties;
import com.veggiepal.exception.AppException;
import com.veggiepal.exception.ErrorCode;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CloudinaryFileStorageService implements FileStorageService {

    Cloudinary cloudinary;
    StorageProperties storageProperties;

    @Override
    public String upload(String key, byte[] content, String contentType) {

        // Cloudinary detects the format from the bytes and appends it to the URL itself,
        // so an extension left in the public_id would end up doubled ("a.png.png")
        Map<?, ?> options = ObjectUtils.asMap(
                "public_id", stripExtension(key),
                "resource_type", "image",
                "overwrite", false
        );

        try {
            Map<?, ?> result = cloudinary.uploader().upload(content, options);
            return (String) result.get("secure_url");
        } catch (IOException | RuntimeException exception) {
            log.error("Could not upload image to Cloudinary", exception);
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public void delete(String url) {

        String prefix = uploadUrlPrefix();

        if (url == null || !url.startsWith(prefix)) {
            return;
        }

        // v1712345678/avatars/7/abc.png -> avatars/7/abc
        String publicId = stripExtension(url.substring(prefix.length()).replaceFirst("^v\\d+/", ""));

        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(
                    "resource_type", "image",
                    "invalidate", true
            ));
        } catch (IOException | RuntimeException exception) {
            log.error("Could not delete image from Cloudinary", exception);
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    private String uploadUrlPrefix() {

        return "https://res.cloudinary.com/" + storageProperties.cloudName() + "/image/upload/";
    }

    private static String stripExtension(String path) {

        int dot = path.lastIndexOf('.');
        return dot > path.lastIndexOf('/') ? path.substring(0, dot) : path;
    }
}
```

- [ ] **Step 7: Chạy test để xác nhận nó pass**

Run: `./mvnw test -Dtest=CloudinaryFileStorageServiceTest`
Expected: `Tests run: 7, Failures: 0, Errors: 0` và `BUILD SUCCESS`.

- [ ] **Step 8: Cập nhật `application.properties`**

Trong `identity-service/src/main/resources/application.properties`, thay khối

```properties
storage.s3.endpoint=${S3_ENDPOINT:http://localhost:9000}
storage.s3.region=${S3_REGION:us-east-1}
storage.s3.access-key=${S3_ACCESS_KEY:minioadmin}
storage.s3.secret-key=${S3_SECRET_KEY:minioadmin}
storage.s3.bucket=${S3_BUCKET:veggiepal-avatars}
storage.s3.public-url=${S3_PUBLIC_URL:http://localhost:9000/veggiepal-avatars}
```

bằng

```properties
# Cloudinary credentials come from the repo-root .env (gitignored; template: .env.example).
# ../.env when run from this directory (mvnw, tests), .env when an IDE runs from the repo root.
# Empty defaults keep the context loadable without .env; only uploads then fail (503).
spring.config.import=optional:file:.env[.properties],optional:file:../.env[.properties]
storage.cloudinary.cloud-name=${CLOUDINARY_CLOUD_NAME:}
storage.cloudinary.api-key=${CLOUDINARY_API_KEY:}
storage.cloudinary.api-secret=${CLOUDINARY_API_SECRET:}
```

- [ ] **Step 9: Kiểm tra không còn tham chiếu AWS hoặc S3**

Run (trong `identity-service/`): `grep -rn "awssdk\|S3Client\|storage\.s3\|S3FileStorageService\|S3Config" src pom.xml`
Expected: không có kết quả.

- [ ] **Step 10: Chạy toàn bộ test (cần MySQL)**

Run (gốc repo): `docker compose up -d mysql`
Run (trong `identity-service/`): `./mvnw test`
Expected: `BUILD SUCCESS`, gồm `VeggiepalApplicationTests` (context load được với bean `Cloudinary`) và `ProfileServiceTest` không đổi.

- [ ] **Step 11: Checkpoint.** Báo user các file đã đổi. Không commit.

---

### Task 3: blog-service chuyển sang Cloudinary

**Files:**
- Modify: `blog-service/pom.xml` (property `aws-sdk.version` dòng 26; dependency S3 dòng 87-91; khối `<dependencyManagement>` dòng 113-123)
- Modify: `blog-service/src/main/java/com/veggiepal/blog/configuration/StorageProperties.java`
- Delete: `blog-service/src/main/java/com/veggiepal/blog/configuration/S3Config.java`
- Create: `blog-service/src/main/java/com/veggiepal/blog/configuration/CloudinaryConfig.java`
- Delete: `blog-service/src/main/java/com/veggiepal/blog/service/S3FileStorageService.java`
- Create: `blog-service/src/main/java/com/veggiepal/blog/service/CloudinaryFileStorageService.java`
- Modify: `blog-service/src/main/resources/application.properties` (khối `storage.s3.*`)
- Create: `blog-service/src/test/java/com/veggiepal/blog/service/CloudinaryFileStorageServiceTest.java`

**Interfaces:**
- Consumes: `com.veggiepal.blog.service.FileStorageService` (giữ nguyên), `com.veggiepal.blog.exception.AppException(ErrorCode)`, `AppException#getErrorCode()`, `ErrorCode.FILE_UPLOAD_FAILED`; biến `CLOUDINARY_*` từ Task 1.
- Produces: `record StorageProperties(String cloudName, String apiKey, String apiSecret)` trong `com.veggiepal.blog.configuration`; bean `Cloudinary`; `CloudinaryFileStorageService(Cloudinary, StorageProperties)`.

- [ ] **Step 1: Đổi dependency trong `blog-service/pom.xml`**

Trong `<properties>`, thay

```xml
		<aws-sdk.version>2.55.0</aws-sdk.version>
```

bằng

```xml
		<cloudinary.version>2.5.0</cloudinary.version>
```

Thay khối dependency

```xml
		<!-- S3 API: MinIO locally, AWS S3 in production -->
		<dependency>
			<groupId>software.amazon.awssdk</groupId>
			<artifactId>s3</artifactId>
		</dependency>
```

bằng

```xml
		<!-- Image storage: Cloudinary -->
		<dependency>
			<groupId>com.cloudinary</groupId>
			<artifactId>cloudinary-http5</artifactId>
			<version>${cloudinary.version}</version>
		</dependency>
```

Xoá toàn bộ khối sau, vì nó chỉ chứa AWS BOM:

```xml
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

```

- [ ] **Step 2: Xoá code S3 và viết lại `StorageProperties`**

Run (từ gốc repo):

```bash
git rm -q blog-service/src/main/java/com/veggiepal/blog/configuration/S3Config.java \
          blog-service/src/main/java/com/veggiepal/blog/service/S3FileStorageService.java
```

Ghi đè `blog-service/src/main/java/com/veggiepal/blog/configuration/StorageProperties.java`:

```java
package com.veggiepal.blog.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage.cloudinary")
public record StorageProperties(
        String cloudName,
        String apiKey,
        String apiSecret
) {
}
```

- [ ] **Step 3: Viết test lỗi**

Tạo `blog-service/src/test/java/com/veggiepal/blog/service/CloudinaryFileStorageServiceTest.java`:

```java
package com.veggiepal.blog.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.veggiepal.blog.configuration.StorageProperties;
import com.veggiepal.blog.exception.AppException;
import com.veggiepal.blog.exception.ErrorCode;

@ExtendWith(MockitoExtension.class)
class CloudinaryFileStorageServiceTest {

    private static final StorageProperties PROPERTIES =
            new StorageProperties("demo-cloud", "api-key", "api-secret");

    private static final String IMAGE_URL =
            "https://res.cloudinary.com/demo-cloud/image/upload/v1712345678/thumbnails/12/a.png";

    @Mock
    Cloudinary cloudinary;

    @Mock
    Uploader uploader;

    CloudinaryFileStorageService storageService;

    @BeforeEach
    void setUp() {
        // lenient: the null/foreign URL case never reaches the SDK
        lenient().when(cloudinary.uploader()).thenReturn(uploader);
        storageService = new CloudinaryFileStorageService(cloudinary, PROPERTIES);
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void upload_sendsPublicIdWithoutExtensionAndReturnsSecureUrl() throws IOException {
        byte[] content = {1, 2, 3};
        when(uploader.upload(eq(content), anyMap())).thenReturn(Map.of("secure_url", IMAGE_URL));

        String url = storageService.upload("thumbnails/12/a.png", content, "image/png");

        ArgumentCaptor<Map> options = ArgumentCaptor.forClass(Map.class);
        verify(uploader).upload(eq(content), options.capture());
        assertThat((Map<String, Object>) options.getValue())
                .containsEntry("public_id", "thumbnails/12/a")
                .containsEntry("resource_type", "image")
                .containsEntry("overwrite", false);
        assertThat(url).isEqualTo(IMAGE_URL);
    }

    @Test
    void upload_networkFailure_throwsFileUploadFailed() throws IOException {
        when(uploader.upload(any(), anyMap())).thenThrow(new IOException("network down"));

        assertThatThrownBy(() -> storageService.upload("thumbnails/12/a.png", new byte[]{1}, "image/png"))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FILE_UPLOAD_FAILED));
    }

    @Test
    void upload_apiError_throwsFileUploadFailed() throws IOException {
        // The SDK reports API errors (bad credentials, missing cloud_name) as RuntimeException
        when(uploader.upload(any(), anyMap())).thenThrow(new RuntimeException("Invalid Signature"));

        assertThatThrownBy(() -> storageService.upload("thumbnails/12/a.png", new byte[]{1}, "image/png"))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FILE_UPLOAD_FAILED));
    }

    @Test
    void delete_versionedUrl_destroysByPublicId() throws IOException {
        storageService.delete(IMAGE_URL);

        verify(uploader).destroy(eq("thumbnails/12/a"), anyMap());
    }

    @Test
    void delete_unversionedUrl_destroysByPublicId() throws IOException {
        storageService.delete("https://res.cloudinary.com/demo-cloud/image/upload/thumbnails/12/a.png");

        verify(uploader).destroy(eq("thumbnails/12/a"), anyMap());
    }

    @Test
    void delete_nullOrForeignUrl_doesNothing() {
        storageService.delete(null);
        storageService.delete("http://localhost:9000/veggiepal-blog-thumbnails/thumbnails/12/a.png");
        storageService.delete("https://res.cloudinary.com/other-cloud/image/upload/v1/thumbnails/12/a.png");

        verifyNoInteractions(uploader);
    }

    @Test
    void delete_storageFailure_throwsFileUploadFailed() throws IOException {
        when(uploader.destroy(anyString(), anyMap())).thenThrow(new IOException("network down"));

        assertThatThrownBy(() -> storageService.delete(IMAGE_URL))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FILE_UPLOAD_FAILED));
    }
}
```

- [ ] **Step 4: Chạy test để xác nhận nó lỗi**

Run (trong `blog-service/`): `./mvnw test -Dtest=CloudinaryFileStorageServiceTest`
Expected: `COMPILATION ERROR` với `cannot find symbol ... class CloudinaryFileStorageService`.

- [ ] **Step 5: Viết `CloudinaryConfig`**

Tạo `blog-service/src/main/java/com/veggiepal/blog/configuration/CloudinaryConfig.java`:

```java
package com.veggiepal.blog.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class CloudinaryConfig {

    @Bean
    Cloudinary cloudinary(StorageProperties storageProperties) {

        // No network call here: empty credentials still build a client, and only uploads fail
        return new Cloudinary(ObjectUtils.asMap(
                "cloud_name", storageProperties.cloudName(),
                "api_key", storageProperties.apiKey(),
                "api_secret", storageProperties.apiSecret(),
                "secure", true
        ));
    }
}
```

- [ ] **Step 6: Viết `CloudinaryFileStorageService`**

Tạo `blog-service/src/main/java/com/veggiepal/blog/service/CloudinaryFileStorageService.java`:

```java
package com.veggiepal.blog.service;

import java.io.IOException;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.veggiepal.blog.configuration.StorageProperties;
import com.veggiepal.blog.exception.AppException;
import com.veggiepal.blog.exception.ErrorCode;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class CloudinaryFileStorageService implements FileStorageService {

    Cloudinary cloudinary;
    StorageProperties storageProperties;

    @Override
    public String upload(String key, byte[] content, String contentType) {

        // Cloudinary detects the format from the bytes and appends it to the URL itself,
        // so an extension left in the public_id would end up doubled ("a.png.png")
        Map<?, ?> options = ObjectUtils.asMap(
                "public_id", stripExtension(key),
                "resource_type", "image",
                "overwrite", false
        );

        try {
            Map<?, ?> result = cloudinary.uploader().upload(content, options);
            return (String) result.get("secure_url");
        } catch (IOException | RuntimeException exception) {
            log.error("Could not upload image to Cloudinary", exception);
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public void delete(String url) {

        String prefix = uploadUrlPrefix();

        if (url == null || !url.startsWith(prefix)) {
            return;
        }

        // v1712345678/thumbnails/12/abc.png -> thumbnails/12/abc
        String publicId = stripExtension(url.substring(prefix.length()).replaceFirst("^v\\d+/", ""));

        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap(
                    "resource_type", "image",
                    "invalidate", true
            ));
        } catch (IOException | RuntimeException exception) {
            log.error("Could not delete image from Cloudinary", exception);
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    private String uploadUrlPrefix() {

        return "https://res.cloudinary.com/" + storageProperties.cloudName() + "/image/upload/";
    }

    private static String stripExtension(String path) {

        int dot = path.lastIndexOf('.');
        return dot > path.lastIndexOf('/') ? path.substring(0, dot) : path;
    }
}
```

- [ ] **Step 7: Chạy test để xác nhận nó pass**

Run: `./mvnw test -Dtest=CloudinaryFileStorageServiceTest`
Expected: `Tests run: 7, Failures: 0, Errors: 0` và `BUILD SUCCESS`.

- [ ] **Step 8: Cập nhật `application.properties`**

Trong `blog-service/src/main/resources/application.properties`, thay khối

```properties
storage.s3.endpoint=${S3_ENDPOINT:http://localhost:9000}
storage.s3.region=${S3_REGION:us-east-1}
storage.s3.access-key=${S3_ACCESS_KEY:minioadmin}
storage.s3.secret-key=${S3_SECRET_KEY:minioadmin}
storage.s3.bucket=${S3_BUCKET:veggiepal-blog-thumbnails}
storage.s3.public-url=${S3_PUBLIC_URL:http://localhost:9000/veggiepal-blog-thumbnails}
```

bằng

```properties
# Cloudinary credentials come from the repo-root .env (gitignored; template: .env.example).
# ../.env when run from this directory (mvnw, tests), .env when an IDE runs from the repo root.
# Empty defaults keep the context loadable without .env; only uploads then fail (503).
spring.config.import=optional:file:.env[.properties],optional:file:../.env[.properties]
storage.cloudinary.cloud-name=${CLOUDINARY_CLOUD_NAME:}
storage.cloudinary.api-key=${CLOUDINARY_API_KEY:}
storage.cloudinary.api-secret=${CLOUDINARY_API_SECRET:}
```

Giữ nguyên comment và hai dòng `spring.servlet.multipart.*` ngay bên dưới.

- [ ] **Step 9: Kiểm tra không còn tham chiếu AWS hoặc S3**

Run (trong `blog-service/`): `grep -rn "awssdk\|S3Client\|storage\.s3\|S3FileStorageService\|S3Config" src pom.xml`
Expected: không có kết quả.

- [ ] **Step 10: Chạy test nhanh, rồi chạy gate DB (bắt buộc theo `CLAUDE.md`)**

Run (trong `blog-service/`): `./mvnw test -Dtest='!BlogServiceApplicationTests,!BlogServiceIntegrationTests'`
Expected: `BUILD SUCCESS` (gồm `BlogServiceTest` không đổi và 7 test mới).

Run: `./mvnw test -Dtest='BlogServiceApplicationTests,BlogServiceIntegrationTests'`
Expected: `BUILD SUCCESS`. Context load được với bean `Cloudinary`, các test có sẵn vẫn pass.

- [ ] **Step 11: Checkpoint.** Báo user các file đã đổi. Không commit.

---

### Task 4: docker-compose gỡ MinIO

**Files:**
- Modify: `docker-compose.yml`

**Interfaces:**
- Consumes: `.env` ở gốc repo (Task 1). Biến `CLOUDINARY_*` được truyền vào container `identity-service` và đọc qua `${CLOUDINARY_*:}` (Task 2).

- [ ] **Step 1: Ghi đè `docker-compose.yml`**

```yaml
name: veggiepal

services:
  mysql:
    image: mysql:8.4
    container_name: veggiepal-mysql
    restart: always

    environment:
      MYSQL_ROOT_PASSWORD: 12345
      MYSQL_DATABASE: veggiepal

    ports:
      - "3307:3306"

    volumes:
      - mysql_data:/var/lib/mysql


  identity-service:
    build:
      context: ./identity-service
      dockerfile: Dockerfile

    container_name: veggiepal-identity
    restart: unless-stopped

    ports:
      - "8081:8081"

    # CLOUDINARY_* for avatar uploads. Optional so `docker compose up -d` still works without .env.
    env_file:
      - path: .env
        required: false

    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/veggiepal_identity
      SPRING_DATASOURCE_USERNAME: root
      SPRING_DATASOURCE_PASSWORD: 12345

    depends_on:
      - mysql


volumes:
  mysql_data:
```

- [ ] **Step 2: Validate**

Run (gốc repo): `docker compose config --quiet && echo OK`
Expected: `OK`.

Run: `docker compose config | grep -ci minio`
Expected: `0`.

Run: `docker compose config | grep CLOUDINARY_CLOUD_NAME`
Expected: có một dòng `CLOUDINARY_CLOUD_NAME: <cloud name>` (nghĩa là `.env` đã được nạp). Không in toàn bộ output ra log hay báo cáo, vì output chứa secret.

- [ ] **Step 3: Dọn container MinIO cũ**

Run: `docker compose up -d --remove-orphans mysql`
Expected: các container `veggiepal-minio` và `veggiepal-minio-init` bị xoá, `veggiepal-mysql` đang chạy.

Không xoá volume `veggiepal_minio_data`. Việc xoá là thao tác phá huỷ dữ liệu và do user quyết định; nếu muốn, user tự chạy `docker volume rm veggiepal_minio_data`.

- [ ] **Step 4: Checkpoint.** Báo user. Không commit.

---

### Task 5: Cập nhật tài liệu

**Files:**
- Modify: `README.md`
- Modify: `CLAUDE.md`
- Modify: `docs/identity-service-guide.md`
- Modify: `docs/blog-service-guide.md`

**Interfaces:**
- Consumes: tên biến và property ở Global Constraints; hành vi `CloudinaryFileStorageService` ở Task 2/3.

Làm từng thay thế bên dưới. Mỗi đoạn "Thay" là văn bản đang có trong file, khớp nguyên văn.

- [ ] **Step 1: `README.md`**

1. Sơ đồ kiến trúc: `+---------------> S3/MinIO thumbnails` → `+---------------> Cloudinary thumbnails`; `+-----------> S3/MinIO avatars` → `+-----------> Cloudinary avatars`.
2. Bảng thành phần: thay dòng `| MinIO API / Console | 9000 / 9001 | Object storage tương thích S3 |` bằng `| Cloudinary | — | Lưu ảnh avatar và thumbnail (dịch vụ ngoài, credential trong `.env`) |`.
3. Mục Công nghệ: `- AWS SDK for Java v2 để làm việc với S3/MinIO` → `- Cloudinary Java SDK (`cloudinary-http5`) để lưu ảnh`.
4. Cấu trúc repository: `├── docker-compose.yml        # MySQL và MinIO local` → `├── docker-compose.yml        # MySQL local`; thêm ngay dưới dòng đó: `├── .env.example              # Template credential Cloudinary (copy thành .env)`.
5. `### 1. Khởi động MySQL và MinIO` → `### 1. Khởi động MySQL`; câu `Compose hiện tại chỉ khởi động hạ tầng MySQL và MinIO, chưa chạy các Java service.` → `Compose hiện tại chỉ khởi động hạ tầng MySQL, chưa chạy các Java service.`
6. Thay toàn bộ mục `### 3. Bucket MinIO`, từ heading đến hết đoạn `Kiểm tra nhanh: ... trong production.`, bằng:

````markdown
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
````

7. Mục Kiểm tra: xoá dòng `- MinIO Console: `http://localhost:9001``.
8. Bảng cấu hình: thay sáu dòng `S3_ENDPOINT` … `S3_PUBLIC_URL` bằng:

```markdown
| `CLOUDINARY_CLOUD_NAME` | identity, blog | Cloud name của account Cloudinary | Rỗng (đọc từ `.env`) |
| `CLOUDINARY_API_KEY` | identity, blog | API key Cloudinary | Rỗng (đọc từ `.env`) |
| `CLOUDINARY_API_SECRET` | identity, blog | API secret Cloudinary | Rỗng (đọc từ `.env`) |
```

- [ ] **Step 2: `CLAUDE.md`**

1. Trong khối `## Commands`, thay hai dòng

```
# Start MySQL (host 3307, root/12345) and MinIO (API 9000, console 9001, minioadmin/minioadmin).
# minio-init creates the public-read buckets veggiepal-avatars and veggiepal-blog-thumbnails.
```

bằng

```
# Start MySQL (host 3307, root/12345).
# Images live on Cloudinary: copy .env.example to .env at the repo root and fill CLOUDINARY_* (never commit .env).
```

2. Trong **Database gotchas**, thay dòng `` - `minio-init` creates two public-read buckets: `veggiepal-avatars` (identity-service) and `veggiepal-blog-thumbnails` (blog-service). `` bằng:

```markdown
- Images live on Cloudinary. identity-service and blog-service read `CLOUDINARY_CLOUD_NAME` / `CLOUDINARY_API_KEY` / `CLOUDINARY_API_SECRET` from the repo-root `.env` through `spring.config.import=optional:file:.env[.properties],optional:file:../.env[.properties]` (`../.env` because Maven runs inside the service directory). `.env` is gitignored; `.env.example` is the template. The properties default to empty, so every context (including `BlogServiceIntegrationTests`) loads without `.env` and only uploads fail with 503.
```

3. Thay dòng `` - **Avatar storage:** `FileStorageService` (S3 API via AWS SDK v2; MinIO locally). Configured with `storage.s3.*` / `S3_*` env vars. Uploads are validated by content type **and** magic bytes (`ImageTypeDetector`). `` bằng:

```markdown
- **Avatar storage:** `FileStorageService` → `CloudinaryFileStorageService` (Cloudinary Java SDK, `storage.cloudinary.*`). The storage key minus its extension becomes the `public_id` (Cloudinary appends the format itself). `delete` only acts on `https://res.cloudinary.com/<cloud>/image/upload/` URLs and ignores everything else, old MinIO URLs included. Uploads are validated by content type **and** magic bytes (`ImageTypeDetector`). blog-service thumbnails use a copy of the same classes.
```

- [ ] **Step 3: `docs/identity-service-guide.md`**

1. `| File storage | Upload/xóa avatar qua S3 API; local dùng MinIO |` → `| File storage | Upload/xóa avatar trên Cloudinary |`
2. Sơ đồ mục 3: `MinIO :9000` → `Cloudinary`; `veggiepal-avatars bucket` → `avatars/{userId}/...`. Giữ nguyên căn lề cột.
3. `| AWS SDK S3 | Làm việc với MinIO hoặc AWS S3 |` → `| Cloudinary SDK (`cloudinary-http5`) | Upload/xóa ảnh avatar |`
4. `+-> FileStorageService -> S3/MinIO` → `+-> FileStorageService -> Cloudinary`
5. Thay mục `### S3/MinIO`, từ heading đến hết câu `Các giá trị mặc định trỏ tới MinIO local. ...`, bằng:

````markdown
### Cloudinary

```properties
spring.config.import=optional:file:.env[.properties],optional:file:../.env[.properties]
storage.cloudinary.cloud-name=${CLOUDINARY_CLOUD_NAME:}
storage.cloudinary.api-key=${CLOUDINARY_API_KEY:}
storage.cloudinary.api-secret=${CLOUDINARY_API_SECRET:}
```

Credential lấy từ file `.env` ở gốc repository (đã gitignore, template là `.env.example`). `../.env` dùng khi chạy Maven trong thư mục service; `.env` dùng khi IDE chạy ở gốc repo. Biến môi trường của OS (ví dụ `env_file` trong docker-compose) được ưu tiên hơn file. Giá trị mặc định rỗng: service vẫn khởi động khi thiếu `.env`, chỉ upload ảnh trả 503.
````

6. Thay toàn bộ mục `## 21. File storage và MinIO/S3`, từ heading đến hết câu `Bucket `veggiepal-avatars` được `minio-init` tạo ...`, bằng:

````markdown
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
````

7. Danh sách test: `- `S3FileStorageServiceTest`: upload, delete, URL và lỗi SDK.` → `- `CloudinaryFileStorageServiceTest`: public_id bỏ đuôi, trả `secure_url`, parse URL khi delete (có/không có version), bỏ qua URL ngoài, lỗi SDK.`
8. Mục 28: thay

````markdown
### 1. Khởi động MySQL và MinIO

Tại root repository:

```powershell
docker compose up -d mysql minio minio-init
```
````

bằng

````markdown
### 1. Khởi động MySQL và tạo `.env`

Tại root repository:

```powershell
docker compose up -d mysql
Copy-Item .env.example .env   # rồi điền CLOUDINARY_*
```
````

9. Bảng địa chỉ mục 28: xoá hai dòng `| MinIO API | ...` và `| MinIO console | ...`.
10. Mục 29 `### Upload avatar trả 503`: thay ba gạch đầu dòng MinIO bằng:

```markdown
- Kiểm tra `.env` ở gốc repo có đủ `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`.
- Đọc log `Could not upload image to Cloudinary`: SDK ghi rõ lý do (sai chữ ký/credential, thiếu `cloud_name`, mất mạng).
```

11. Mục 30: `7. Default secret và thông tin MySQL/MinIO chỉ phù hợp local development.` → `7. Default secret và thông tin MySQL chỉ phù hợp local development.`
12. Mục 32: `lưu avatar trên MinIO/S3` → `lưu avatar trên Cloudinary`.

- [ ] **Step 4: `docs/blog-service-guide.md`**

1. `- Thumbnail lưu trên MinIO/S3.` → `- Thumbnail lưu trên Cloudinary.`
2. Sơ đồ: `+-> MinIO/S3 thumbnails` → `+-> Cloudinary thumbnails`
3. `Lombok, AWS SDK S3 và Springdoc.` → `Lombok, Cloudinary SDK và Springdoc.`
4. Thay

````markdown
Thumbnail dùng bucket `veggiepal-blog-thumbnails`:

```properties
storage.s3.endpoint=http://localhost:9000
storage.s3.public-url=http://localhost:9000/veggiepal-blog-thumbnails
```
````

bằng

````markdown
Thumbnail lưu trên Cloudinary; credential đọc từ `.env` ở gốc repo (template `.env.example`):

```properties
spring.config.import=optional:file:.env[.properties],optional:file:../.env[.properties]
storage.cloudinary.cloud-name=${CLOUDINARY_CLOUD_NAME:}
storage.cloudinary.api-key=${CLOUDINARY_API_KEY:}
storage.cloudinary.api-secret=${CLOUDINARY_API_SECRET:}
```
````

5. `## 14. Thumbnail và S3/MinIO` → `## 14. Thumbnail và Cloudinary`
6. Thay đoạn `` `FileStorageService` tách business logic khỏi AWS SDK. `S3FileStorageService` chỉ xóa URL ... AWS S3-compatible storage. `` bằng:

```markdown
`FileStorageService` tách business logic khỏi Cloudinary SDK. `CloudinaryFileStorageService` dùng key bỏ đuôi làm `public_id` (`thumbnails/{blogId}/{UUID}`) và trả `secure_url`. Nó chỉ xóa URL bắt đầu bằng `https://res.cloudinary.com/{cloudName}/image/upload/`; URL MinIO cũ và URL ngoài bị bỏ qua. Lỗi SDK thành `FILE_UPLOAD_FAILED` (503).
```

7. Mục 20: `docker compose up -d mysql minio minio-init` → `docker compose up -d mysql`; câu `Bucket thumbnail do `minio-init` tạo và cấp anonymous download.` → `Credential Cloudinary đọc từ `.env` ở gốc repo (copy từ `.env.example`).`

- [ ] **Step 5: Kiểm tra không còn tham chiếu cũ**

Run (gốc repo): `grep -n -i "minio\|S3_\|storage\.s3\|aws sdk\|awssdk\|S3FileStorage\|S3Config" README.md CLAUDE.md docs/identity-service-guide.md docs/blog-service-guide.md`
Expected: không có kết quả.

- [ ] **Step 6: Checkpoint.** Báo user. Không commit.

---

### Task 6: Gate cuối và smoke test

**Files:** không sửa file nào.

- [ ] **Step 1: Gate khi CÓ `.env`**

Run (gốc repo): `docker compose up -d mysql`
Run (trong `identity-service/`): `./mvnw test` → Expected: `BUILD SUCCESS`.
Run (trong `blog-service/`): `./mvnw test` → Expected: `BUILD SUCCESS`.
Run (trong `blog-service/`): `./mvnw test -Dtest='BlogServiceApplicationTests,BlogServiceIntegrationTests'` → Expected: `BUILD SUCCESS`.

- [ ] **Step 2: Gate khi KHÔNG có `.env`** (kiểm chứng mặc định rỗng ở spec 4.3)

Run (gốc repo): `mv .env .env.bak`
Run (trong `identity-service/`): `./mvnw test -Dtest=VeggiepalApplicationTests` → Expected: `BUILD SUCCESS`.
Run (trong `blog-service/`): `./mvnw test -Dtest='BlogServiceApplicationTests,BlogServiceIntegrationTests'` → Expected: `BUILD SUCCESS`.
Run (gốc repo): `mv .env.bak .env`. **Bắt buộc**, kể cả khi test fail.

- [ ] **Step 3: Smoke test thật** (cần MySQL, identity-service, blog-service, api-gateway đang chạy với `.env` có giá trị thật)

Mở bốn terminal và chạy `./mvnw spring-boot:run` trong `identity-service`, `blog-service`, `api-gateway`. Sau đó tại gốc repo (Git Bash, cần `curl` và `jq`):

```bash
curl -s -X POST localhost:8080/api/auth/register -H 'Content-Type: application/json' \
  -d '{"email":"cloudinary.smoke@test.local","password":"Smoke123!","fullName":"Smoke Test"}'
TOKEN=$(curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"cloudinary.smoke@test.local","password":"Smoke123!"}' | jq -r .result.accessToken)
curl -s -X POST localhost:8080/api/users/me/avatar -H "Authorization: Bearer $TOKEN" \
  -F "file=@<đường dẫn tới một file .png thật>;type=image/png" | jq .result.avatarUrl
```

Expected: `"https://res.cloudinary.com/<cloud name>/image/upload/v.../avatars/<id>/<uuid>.png"`, và URL này mở được trên trình duyệt.

Chạy lệnh upload avatar lần hai, rồi mở Cloudinary Console → Media Library → folder `avatars/<id>`. Expected: chỉ còn ảnh mới, ảnh cũ đã bị `destroy`.

Thumbnail: lấy `id` một blog của user này qua `GET /api/blogs/me` (nếu chưa có thì tạo bằng Swagger tại `http://localhost:8080/swagger-ui.html`), rồi chạy:

```bash
curl -s -X POST localhost:8080/api/blogs/<id>/thumbnail -H "Authorization: Bearer $TOKEN" \
  -F "file=@<đường dẫn tới một file .png thật>;type=image/png" | jq .result.thumbnailUrl
```

Expected: URL `https://res.cloudinary.com/.../thumbnails/<id>/...`. Upload lần hai thì ảnh cũ biến mất khỏi Media Library.

- [ ] **Step 4: Kiểm tra secret không lọt vào git**

Run (gốc repo): `git status --porcelain | grep -c "\.env$"` → Expected: `0`.
Run: `git grep --untracked -l "<giá trị API secret>"`. Lệnh này tìm trong mọi file tracked và untracked chưa bị ignore. Thay chỗ trống bằng secret thật **trên máy** và không dán vào báo cáo. Expected: không in gì (`.env` bị ignore nên không nằm trong phạm vi tìm).

- [ ] **Step 5: Checkpoint cuối.** Báo user kết quả từng gate (số test pass) và kết quả smoke test. User tự review diff và commit.
