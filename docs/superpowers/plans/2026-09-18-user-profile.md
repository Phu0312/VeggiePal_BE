# User Profile & Nutrition Profile Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Người dùng đã đăng nhập có thể xem/sửa profile, upload avatar, đổi mật khẩu (identity-service), ghi lịch sử chiều cao/cân nặng/BMI và khai báo dị ứng (nutrition-service mới). Tất cả đều gọi qua api-gateway và xác thực bằng JWT.

**Architecture:** identity-service và nutrition-service đều là OAuth2 Resource Server, kiểm tra JWT HS256 bằng một secret dùng chung. Avatar lưu trên S3 API (MinIO khi chạy local) thông qua interface `FileStorageService`. nutrition-service là một Maven project độc lập, copy các class dùng chung (`ApiResponse`, `ErrorCode`, `GlobalExceptionHandler`, security) từ identity-service.

**Tech Stack:** Java 21 (chạy được trên JDK 25), Spring Boot 4.1.1, Spring Security 7.1.1, Spring Data JPA (Hibernate 7), MySQL 8.4, MapStruct 1.6.3, Lombok, jjwt 0.12.6, AWS SDK v2 2.55.0 (`s3`), MinIO, Spring Cloud Gateway Server WebMVC 5.0.3, JUnit 6 + Mockito 5 + spring-security-test.

**Spec:** `docs/superpowers/specs/2026-09-17-user-profile-design.md`

## Global Constraints

- Mỗi service là một Maven project độc lập (không có parent POM). Chạy lệnh Maven **trong thư mục của service**, dùng Git Bash: `./mvnw ...`.
- Boot parent `4.1.1`, `java.version` `21`, springdoc `3.1.0` (giống identity-service), MapStruct `1.6.3`.
- Convention code:
  - Service và controller: `@RequiredArgsConstructor` + `@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)`, field khai báo không có modifier.
  - DTO: `@Data @Builder @NoArgsConstructor @AllArgsConstructor @FieldDefaults(level = AccessLevel.PRIVATE)`.
  - Mọi endpoint trả `ApiResponse<T>`.
  - Lỗi nghiệp vụ: `throw new AppException(ErrorCode.X)`.
  - Validation message là **tên hằng `ErrorCode`**.
- JWT:
  - `jwt.secret=${JWT_SECRET:veggiepal-secret-key-must-be-at-least-32-characters}`
  - Ký và kiểm tra đều ghi rõ **HS256**
  - Claims: `sub` = email, `userId`, `role`; hạn 24h
- `userId` **luôn lấy từ claim `userId` của JWT**, không bao giờ lấy từ body hay path.
- Mã lỗi:
  - Mã chung, giống nhau ở cả 2 service: `1001 INVALID_KEY`, `1008 UNAUTHENTICATED`, `1009 UNAUTHORIZED`, `1018 INVALID_REQUEST`, `9999 UNCATEGORIZED_EXCEPTION`
  - identity-service: `1011`–`1017`
  - nutrition-service: `2001`–`2007`
  - Message copy **nguyên văn** từ spec §6.3.
- Không ghi vào log: mật khẩu, token, chiều cao, cân nặng, BMI, dị ứng.
- api-gateway **không được** khai báo `spring.servlet.multipart.*`. Gateway tự tắt multipart để chuyển body thẳng xuống service.
- Image MinIO: `quay.io/minio/minio:RELEASE.2025-09-07T16-13-09Z` và `quay.io/minio/mc:RELEASE.2025-08-13T08-35-41Z`.
- **Commit:** người dùng đã từ chối một lần commit trước đó. **Hỏi người dùng trước lần commit đầu tiên.** Nếu họ không đồng ý thì bỏ qua mọi bước "Commit". Commit message kết thúc bằng dòng `Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>`.

## Trước khi bắt đầu

- Unit test và `@WebMvcTest` **không cần Docker**. Test `contextLoads` và Task 13 cần Docker Desktop đang chạy (MySQL, MinIO).
- Chạy một test class: `./mvnw test -Dtest=TenClass`.
- Chạy toàn bộ test trừ `contextLoads` (khi chưa có MySQL):
  - identity-service: `./mvnw test -Dtest='!VeggiepalApplicationTests'`
  - nutrition-service: `./mvnw test -Dtest='!NutritionServiceApplicationTests'`
- Nhánh làm việc: `feature/user-profile`.

## File Structure

**identity-service** (`identity-service/src/main/java/com/veggiepal/`)

| File | Trạng thái | Trách nhiệm |
|---|---|---|
| `configuration/JwtConfig.java` | mới | Tạo secret key HMAC; bean `JwtDecoder` (HS256) |
| `configuration/SecurityConfig.java` | sửa | Filter chain Resource Server, danh sách path public, `BearerTokenResolver`, stateless |
| `configuration/SecurityExceptionHandler.java` | mới | Ghi `ApiResponse` cho lỗi 401/403 |
| `configuration/StorageProperties.java` | mới | `@ConfigurationProperties("storage.s3")` |
| `configuration/S3Config.java` | mới | Bean `S3Client` |
| `configuration/OpenApiConfig.java` | sửa | Security scheme `bearerAuth` |
| `controller/CurrentUser.java` | mới | Đọc `userId` từ `Jwt` |
| `controller/ProfileController.java` | mới | `/users/me/**` |
| `dto/request/UpdateProfileRequest.java`, `ChangePasswordRequest.java` | mới | |
| `dto/response/UserProfileResponse.java` | mới | |
| `entity/User.java` | sửa | Thêm `dateOfBirth` |
| `enums/ImageType.java` | mới | JPEG/PNG/WEBP → content-type, đuôi file |
| `exception/ErrorCode.java` | sửa | 1011–1018 |
| `exception/GlobalExceptionHandler.java` | sửa | `INVALID_REQUEST`, lỗi multipart |
| `mapper/UserMapper.java` | sửa | `toUserProfileResponse`, `updateProfile` |
| `service/JwtService.java` | sửa | Secret lấy từ cấu hình, ký HS256 |
| `service/ProfileService.java` | mới | Profile, đổi mật khẩu, avatar |
| `service/FileStorageService.java` | mới | Interface lưu file |
| `service/S3FileStorageService.java` | mới | Cài đặt S3 |
| `service/ImageTypeDetector.java` | mới | Nhận diện loại ảnh bằng magic bytes |

**nutrition-service** (mới, `nutrition-service/src/main/java/com/veggiepal/nutrition/`)

| File | Trách nhiệm |
|---|---|
| `NutritionServiceApplication.java` | Class main |
| `configuration/JwtConfig.java`, `SecurityConfig.java`, `SecurityExceptionHandler.java`, `OpenApiConfig.java` | Bảo mật và tài liệu Swagger |
| `controller/CurrentUser.java`, `HealthRecordController.java`, `AllergyController.java` | API |
| `dto/request/HealthRecordRequest.java`, `UpdateAllergiesRequest.java` | |
| `dto/response/ApiResponse.java`, `PageResponse.java`, `HealthRecordResponse.java`, `AllergenResponse.java` | |
| `entity/HealthRecord.java`, `Allergen.java`, `UserAllergy.java` | |
| `enums/AllergenCategory.java` | |
| `exception/AppException.java`, `ErrorCode.java`, `GlobalExceptionHandler.java` | |
| `mapper/HealthRecordMapper.java`, `AllergenMapper.java` | |
| `repository/HealthRecordRepository.java`, `AllergenRepository.java`, `UserAllergyRepository.java` | |
| `service/HealthRecordService.java`, `AllergyService.java` | |
| `resources/application.properties`, `resources/data.sql` | |

**Khác:** `docker-compose.yml` (MinIO), `api-gateway/src/main/resources/application.yaml` (routes), `CLAUDE.md`.

---

### Task 1: identity-service: ký JWT bằng HS256 từ secret cấu hình được + `JwtDecoder`

**Files:**
- Modify: `identity-service/pom.xml`
- Modify: `identity-service/src/main/resources/application.properties`
- Create: `identity-service/src/main/java/com/veggiepal/configuration/JwtConfig.java`
- Modify: `identity-service/src/main/java/com/veggiepal/service/JwtService.java`
- Test: `identity-service/src/test/java/com/veggiepal/service/JwtServiceTest.java`

**Interfaces:**
- Produces: `JwtConfig.signingKey(String secret): SecretKey` (static); bean `JwtDecoder JwtConfig#jwtDecoder(@Value("${jwt.secret}") String secret)`; constructor `JwtService(@Value("${jwt.secret}") String secret)`; `JwtService.generateToken(User): String` giữ nguyên chữ ký.

- [ ] **Step 1: Thêm dependency**

Trong `identity-service/pom.xml`, thêm ngay sau dependency `spring-boot-starter-security`:

```xml
		<!-- OAuth2 Resource Server: validate JWT -->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-security-oauth2-resource-server</artifactId>
		</dependency>
```

Thêm ngay sau dependency `spring-boot-starter-test`:

```xml
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-webmvc-test</artifactId>
			<scope>test</scope>
		</dependency>
```

- [ ] **Step 2: Thêm cấu hình secret**

Thêm vào cuối `identity-service/src/main/resources/application.properties`:

```properties

jwt.secret=${JWT_SECRET:veggiepal-secret-key-must-be-at-least-32-characters}
```

- [ ] **Step 3: Viết test fail**

Tạo `identity-service/src/test/java/com/veggiepal/service/JwtServiceTest.java`:

```java
package com.veggiepal.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import com.veggiepal.configuration.JwtConfig;
import com.veggiepal.entity.User;
import com.veggiepal.enums.Role;

class JwtServiceTest {

    private static final String SECRET = "veggiepal-secret-key-must-be-at-least-32-characters";

    @Test
    void generateToken_isAcceptedByJwtDecoder() {
        User user = User.builder()
                .id(7L)
                .email("an@example.com")
                .role(Role.USER)
                .build();

        String token = new JwtService(SECRET).generateToken(user);
        JwtDecoder decoder = new JwtConfig().jwtDecoder(SECRET);
        Jwt jwt = decoder.decode(token);

        assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
        assertThat(jwt.getSubject()).isEqualTo("an@example.com");
        assertThat(((Number) jwt.getClaim("userId")).longValue()).isEqualTo(7L);
        assertThat(jwt.getClaimAsString("role")).isEqualTo("USER");
        assertThat(jwt.getExpiresAt()).isAfter(jwt.getIssuedAt());
    }
}
```

- [ ] **Step 4: Chạy test, xác nhận fail**

Run: `cd identity-service && ./mvnw test -Dtest=JwtServiceTest`
Expected: FAIL khi biên dịch. Lỗi là `cannot find symbol: class JwtConfig` và `constructor JwtService in class JwtService cannot be applied`.

- [ ] **Step 5: Tạo `JwtConfig`**

`identity-service/src/main/java/com/veggiepal/configuration/JwtConfig.java`:

```java
package com.veggiepal.configuration;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
public class JwtConfig {

    public static SecretKey signingKey(String secret) {
        return new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );
    }

    @Bean
    public JwtDecoder jwtDecoder(@Value("${jwt.secret}") String secret) {
        return NimbusJwtDecoder
                .withSecretKey(signingKey(secret))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}
```

- [ ] **Step 6: Sửa `JwtService`**

Thay toàn bộ `identity-service/src/main/java/com/veggiepal/service/JwtService.java`:

```java
package com.veggiepal.service;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.veggiepal.configuration.JwtConfig;
import com.veggiepal.entity.User;

import io.jsonwebtoken.Jwts;

@Service
public class JwtService {

    private static final long EXPIRATION =
            1000 * 60 * 60 * 24;

    private final SecretKey signingKey;

    public JwtService(@Value("${jwt.secret}") String secret) {
        this.signingKey = JwtConfig.signingKey(secret);
    }

    public String generateToken(User user) {

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId())
                .claim("role", user.getRole().name())
                .issuedAt(new Date())
                .expiration(
                        new Date(
                                System.currentTimeMillis()
                                        + EXPIRATION
                        )
                )
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }
}
```

- [ ] **Step 7: Chạy test, xác nhận pass**

Run: `./mvnw test -Dtest=JwtServiceTest`
Expected: `Tests run: 1, Failures: 0, Errors: 0` và `BUILD SUCCESS`.

- [ ] **Step 8: Commit** (chỉ khi người dùng đã đồng ý commit)

```bash
git add identity-service/pom.xml identity-service/src/main/resources/application.properties identity-service/src/main/java/com/veggiepal/configuration/JwtConfig.java identity-service/src/main/java/com/veggiepal/service/JwtService.java identity-service/src/test/java/com/veggiepal/service/JwtServiceTest.java
git commit -m "feat(identity): sign JWT with configurable HS256 secret and add JwtDecoder

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 2: identity-service: Resource Server filter chain, lỗi 401/403 dạng `ApiResponse`, bỏ qua token ở path public

**Files:**
- Create: `identity-service/src/main/java/com/veggiepal/configuration/SecurityExceptionHandler.java`
- Modify: `identity-service/src/main/java/com/veggiepal/configuration/SecurityConfig.java` (thay toàn bộ)
- Test: `identity-service/src/test/java/com/veggiepal/configuration/SecurityExceptionHandlerTest.java`
- Test: `identity-service/src/test/java/com/veggiepal/configuration/SecurityConfigTest.java`

**Interfaces:**
- Consumes: bean `JwtDecoder` (Task 1).
- Produces: `SecurityExceptionHandler` (bean, implements `AuthenticationEntryPoint` và `AccessDeniedHandler`, constructor `SecurityExceptionHandler(JsonMapper)`); `SecurityConfig.PUBLIC_ENDPOINTS`. **Mọi `@WebMvcTest` từ đây trở đi phải có** `@Import({SecurityConfig.class, JwtConfig.class, SecurityExceptionHandler.class})`.

- [ ] **Step 1: Viết test fail cho `SecurityExceptionHandler`**

`identity-service/src/test/java/com/veggiepal/configuration/SecurityExceptionHandlerTest.java`:

```java
package com.veggiepal.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import tools.jackson.databind.json.JsonMapper;

class SecurityExceptionHandlerTest {

    private final SecurityExceptionHandler handler =
            new SecurityExceptionHandler(JsonMapper.builder().build());

    @Test
    void commence_writesUnauthenticatedApiResponse() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.commence(new MockHttpServletRequest(), response, new BadCredentialsException("bad token"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith("application/json");
        assertThat(response.getContentAsString())
                .contains("\"code\":1008")
                .contains("\"message\":\"Unauthenticated\"");
    }

    @Test
    void handle_writesUnauthorizedApiResponse() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(new MockHttpServletRequest(), response, new AccessDeniedException("denied"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("\"code\":1009");
    }
}
```

- [ ] **Step 2: Viết test fail cho filter chain**

`identity-service/src/test/java/com/veggiepal/configuration/SecurityConfigTest.java`:

```java
package com.veggiepal.configuration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.veggiepal.controller.UserController;
import com.veggiepal.dto.response.LoginResponse;
import com.veggiepal.service.UserService;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtConfig.class, SecurityExceptionHandler.class})
class SecurityConfigTest {

    private static final String LOGIN_BODY = """
            {"email": "an@example.com", "password": "secret123"}
            """;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserService userService;

    @Test
    void protectedEndpoint_withoutToken_returns401ApiResponse() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1008));
    }

    @Test
    void protectedEndpoint_withMalformedToken_returns401ApiResponse() throws Exception {
        mockMvc.perform(get("/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1008));
    }

    @Test
    void login_withStaleBearerHeader_isNotRejected() throws Exception {
        when(userService.login(any()))
                .thenReturn(LoginResponse.builder().accessToken("token").build());

        mockMvc.perform(post("/auth/login")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer expired-or-garbage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(LOGIN_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.accessToken").value("token"));
    }
}
```

- [ ] **Step 3: Chạy test, xác nhận fail**

Run: `./mvnw test -Dtest='SecurityExceptionHandlerTest,SecurityConfigTest'`
Expected: FAIL khi biên dịch, lỗi `cannot find symbol: class SecurityExceptionHandler`.

- [ ] **Step 4: Tạo `SecurityExceptionHandler`**

`identity-service/src/main/java/com/veggiepal/configuration/SecurityExceptionHandler.java`:

```java
package com.veggiepal.configuration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.veggiepal.dto.response.ApiResponse;
import com.veggiepal.exception.ErrorCode;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SecurityExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    JsonMapper jsonMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {

        writeError(response, ErrorCode.UNAUTHENTICATED);
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {

        writeError(response, ErrorCode.UNAUTHORIZED);
    }

    private void writeError(
            HttpServletResponse response,
            ErrorCode errorCode
    ) throws IOException {

        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();

        response.setStatus(errorCode.getStatusCode().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(jsonMapper.writeValueAsString(apiResponse));
    }
}
```

- [ ] **Step 5: Thay toàn bộ `SecurityConfig`**

`identity-service/src/main/java/com/veggiepal/configuration/SecurityConfig.java`:

```java
package com.veggiepal.configuration;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
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
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SecurityConfig {

    static final String[] PUBLIC_ENDPOINTS = {
            "/auth/register",
            "/auth/login",
            "/auth/test",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**"
    };

    SecurityExceptionHandler securityExceptionHandler;

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity httpSecurity
    ) throws Exception {

        httpSecurity
                .authorizeHttpRequests(
                        request -> request
                                .requestMatchers(PUBLIC_ENDPOINTS)
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

    @Bean
    PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder(10);
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {

        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("role");
        authoritiesConverter.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    // Public endpoints ignore the Authorization header, so a stale token cannot block login/register.
    private BearerTokenResolver publicEndpointAwareBearerTokenResolver() {

        DefaultBearerTokenResolver defaultResolver = new DefaultBearerTokenResolver();
        List<RequestMatcher> publicMatchers = Arrays.stream(PUBLIC_ENDPOINTS)
                .map(pattern -> (RequestMatcher) PathPatternRequestMatcher.withDefaults().matcher(pattern))
                .toList();

        return request -> publicMatchers.stream().anyMatch(matcher -> matcher.matches(request))
                ? null
                : defaultResolver.resolve(request);
    }
}
```

- [ ] **Step 6: Chạy test, xác nhận pass**

Run: `./mvnw test -Dtest='SecurityExceptionHandlerTest,SecurityConfigTest,JwtServiceTest'`
Expected: `Tests run: 6, Failures: 0, Errors: 0`.

- [ ] **Step 7: Commit** (chỉ khi người dùng đã đồng ý)

```bash
git add identity-service/src/main/java/com/veggiepal/configuration/SecurityExceptionHandler.java identity-service/src/main/java/com/veggiepal/configuration/SecurityConfig.java identity-service/src/test/java/com/veggiepal/configuration
git commit -m "feat(identity): validate JWT as resource server with ApiResponse 401/403

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 3: identity-service: `INVALID_REQUEST` (1018) cho JSON sai hoặc tham số sai kiểu

**Files:**
- Modify: `identity-service/src/main/java/com/veggiepal/exception/ErrorCode.java`
- Modify: `identity-service/src/main/java/com/veggiepal/exception/GlobalExceptionHandler.java`
- Test: `identity-service/src/test/java/com/veggiepal/exception/GlobalExceptionHandlerTest.java`

**Interfaces:**
- Produces: `ErrorCode.INVALID_REQUEST`. `GlobalExceptionHandler` có thêm helper `private ResponseEntity<ApiResponse<?>> errorResponse(ErrorCode)`, được dùng lại ở Task 7.
- **Quy ước cho các task sau:** hằng `ErrorCode` mới luôn được chèn **ngay trước** dòng `INVALID_REQUEST(1018, ...)`.

- [ ] **Step 1: Viết test fail**

`identity-service/src/test/java/com/veggiepal/exception/GlobalExceptionHandlerTest.java`:

```java
package com.veggiepal.exception;

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

import com.veggiepal.configuration.JwtConfig;
import com.veggiepal.configuration.SecurityConfig;
import com.veggiepal.configuration.SecurityExceptionHandler;
import com.veggiepal.controller.UserController;
import com.veggiepal.service.UserService;

@WebMvcTest(UserController.class)
@Import({SecurityConfig.class, JwtConfig.class, SecurityExceptionHandler.class})
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    UserService userService;

    @Test
    void malformedJson_returnsInvalidRequest() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1018))
                .andExpect(jsonPath("$.message").value("Invalid request data"));
    }
}
```

- [ ] **Step 2: Chạy test, xác nhận fail**

Run: `./mvnw test -Dtest=GlobalExceptionHandlerTest`
Expected: FAIL: `Status expected:<400> but was:<500>`. Lúc này handler chung đang trả 9999.

- [ ] **Step 3: Thêm `INVALID_REQUEST` vào `ErrorCode`**

Trong `ErrorCode.java`, thay dòng:

```java
    UNAUTHORIZED(1009, "You do not have permission", HttpStatus.FORBIDDEN);
```

bằng:

```java
    UNAUTHORIZED(1009, "You do not have permission", HttpStatus.FORBIDDEN),

    INVALID_REQUEST(1018, "Invalid request data", HttpStatus.BAD_REQUEST);
```

- [ ] **Step 4: Thêm handler vào `GlobalExceptionHandler`**

Thêm 2 import:

```java
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
```

Thêm 2 method sau vào trong class, ngay trước method `private String mapAttribute(...)`:

```java
    @ExceptionHandler(value = {
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class
    })
    ResponseEntity<ApiResponse<?>> handlingInvalidRequest(
            Exception exception
    ) {

        return errorResponse(ErrorCode.INVALID_REQUEST);
    }

    private ResponseEntity<ApiResponse<?>> errorResponse(
            ErrorCode errorCode
    ) {

        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();

        return ResponseEntity
                .status(errorCode.getStatusCode())
                .body(apiResponse);
    }
```

- [ ] **Step 5: Chạy test, xác nhận pass**

Run: `./mvnw test -Dtest=GlobalExceptionHandlerTest`
Expected: `Tests run: 1, Failures: 0`.

- [ ] **Step 6: Commit** (chỉ khi người dùng đã đồng ý)

```bash
git add identity-service/src/main/java/com/veggiepal/exception identity-service/src/test/java/com/veggiepal/exception
git commit -m "feat(identity): map malformed requests to INVALID_REQUEST (1018)

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 4: identity-service: xem và sửa profile (`GET`/`PATCH /users/me`)

**Files:**
- Modify: `identity-service/src/main/java/com/veggiepal/entity/User.java`
- Create: `identity-service/src/main/java/com/veggiepal/dto/request/UpdateProfileRequest.java`
- Create: `identity-service/src/main/java/com/veggiepal/dto/response/UserProfileResponse.java`
- Modify: `identity-service/src/main/java/com/veggiepal/mapper/UserMapper.java` (thay toàn bộ)
- Modify: `identity-service/src/main/java/com/veggiepal/exception/ErrorCode.java`
- Create: `identity-service/src/main/java/com/veggiepal/service/ProfileService.java`
- Create: `identity-service/src/main/java/com/veggiepal/controller/CurrentUser.java`
- Create: `identity-service/src/main/java/com/veggiepal/controller/ProfileController.java`
- Test: `identity-service/src/test/java/com/veggiepal/service/ProfileServiceTest.java`
- Test: `identity-service/src/test/java/com/veggiepal/controller/ProfileControllerTest.java`

**Interfaces:**
- Consumes: `SecurityConfig`, `JwtConfig`, `SecurityExceptionHandler` (Task 2); `ErrorCode.INVALID_REQUEST` (Task 3).
- Produces:
  - `User.dateOfBirth: LocalDate`
  - `UserMapper.toUserProfileResponse(User): UserProfileResponse`
  - `UserMapper.updateProfile(@MappingTarget User, UpdateProfileRequest): void`
  - `ProfileService.getProfile(Long userId): UserProfileResponse`
  - `ProfileService.updateProfile(Long userId, UpdateProfileRequest request): UserProfileResponse`
  - `CurrentUser.id(Jwt jwt): Long` (public static)
  - `ProfileServiceTest.existingUser(): User` (static, dùng lại ở Task 5 và 7)

- [ ] **Step 1: Thêm `dateOfBirth` vào `User`**

Trong `User.java`, thêm `import java.time.LocalDate;` và thêm field ngay sau field `avatarUrl`:

```java
    @Column(name = "date_of_birth")
    LocalDate dateOfBirth;
```

- [ ] **Step 2: Tạo các DTO**

`identity-service/src/main/java/com/veggiepal/dto/request/UpdateProfileRequest.java`:

```java
package com.veggiepal.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateProfileRequest {

    // null keeps the current value; when present it must contain a non-blank character
    @Pattern(regexp = "(?s).*\\S.*", message = "FULL_NAME_REQUIRED")
    String fullName;

    // "" clears the phone number
    String phone;

    @Past(message = "INVALID_DATE_OF_BIRTH")
    LocalDate dateOfBirth;
}
```

`identity-service/src/main/java/com/veggiepal/dto/response/UserProfileResponse.java`:

```java
package com.veggiepal.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.veggiepal.enums.Role;
import com.veggiepal.enums.UserStatus;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserProfileResponse {
    Long id;

    String email;

    String fullName;

    String phone;

    String avatarUrl;

    LocalDate dateOfBirth;

    Role role;

    UserStatus status;

    Boolean emailVerified;

    LocalDateTime createdAt;
}
```

- [ ] **Step 3: Thay toàn bộ `UserMapper`**

```java
package com.veggiepal.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.veggiepal.dto.request.RegisterRequest;
import com.veggiepal.dto.request.UpdateProfileRequest;
import com.veggiepal.dto.response.RegisterResponse;
import com.veggiepal.dto.response.UserProfileResponse;
import com.veggiepal.entity.User;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "passwordHash", ignore = true)
    @Mapping(target = "avatarUrl", ignore = true)
    @Mapping(target = "dateOfBirth", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "emailVerified", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    User toUser(RegisterRequest request);

    RegisterResponse toUserResponse(User user);

    UserProfileResponse toUserProfileResponse(User user);

    @BeanMapping(
            ignoreByDefault = true,
            nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
    )
    @Mapping(target = "fullName", source = "fullName")
    @Mapping(target = "phone", source = "phone")
    @Mapping(target = "dateOfBirth", source = "dateOfBirth")
    void updateProfile(@MappingTarget User user, UpdateProfileRequest request);
}
```

- [ ] **Step 4: Thêm mã lỗi**

Trong `ErrorCode.java`, chèn ngay **trước** dòng `INVALID_REQUEST(1018, ...)`:

```java
    INVALID_DATE_OF_BIRTH(1013, "Date of birth must be in the past", HttpStatus.BAD_REQUEST),

```

- [ ] **Step 5: Viết test fail cho service**

`identity-service/src/test/java/com/veggiepal/service/ProfileServiceTest.java`:

```java
package com.veggiepal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.veggiepal.dto.request.UpdateProfileRequest;
import com.veggiepal.dto.response.UserProfileResponse;
import com.veggiepal.entity.User;
import com.veggiepal.enums.Role;
import com.veggiepal.enums.UserStatus;
import com.veggiepal.exception.AppException;
import com.veggiepal.exception.ErrorCode;
import com.veggiepal.mapper.UserMapper;
import com.veggiepal.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    static final Long USER_ID = 7L;

    @Mock
    UserRepository userRepository;

    @Spy
    UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    @InjectMocks
    ProfileService profileService;

    @Test
    void getProfile_returnsCurrentUserProfile() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser()));

        UserProfileResponse profile = profileService.getProfile(USER_ID);

        assertThat(profile.getId()).isEqualTo(USER_ID);
        assertThat(profile.getEmail()).isEqualTo("an@example.com");
        assertThat(profile.getDateOfBirth()).isEqualTo(LocalDate.of(2000, 1, 31));
    }

    @Test
    void getProfile_unknownUser_throwsUserNotExisted() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.getProfile(USER_ID))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.USER_NOT_EXISTED));
    }

    @Test
    void updateProfile_nullFields_keepExistingValues() {
        User user = existingUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        profileService.updateProfile(USER_ID, new UpdateProfileRequest());

        assertThat(user.getFullName()).isEqualTo("Nguyen Van An");
        assertThat(user.getPhone()).isEqualTo("0901234567");
        assertThat(user.getDateOfBirth()).isEqualTo(LocalDate.of(2000, 1, 31));
        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_trimsValuesAndClearsBlankPhone() {
        User user = existingUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("  Tran Thi Binh  ")
                .phone("   ")
                .dateOfBirth(LocalDate.of(1999, 12, 1))
                .build();

        UserProfileResponse profile = profileService.updateProfile(USER_ID, request);

        assertThat(user.getFullName()).isEqualTo("Tran Thi Binh");
        assertThat(user.getPhone()).isNull();
        assertThat(user.getDateOfBirth()).isEqualTo(LocalDate.of(1999, 12, 1));
        assertThat(profile.getFullName()).isEqualTo("Tran Thi Binh");
    }

    @Test
    void updateProfile_trimsNewPhone() {
        User user = existingUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        profileService.updateProfile(USER_ID, UpdateProfileRequest.builder().phone(" 0987654321 ").build());

        assertThat(user.getPhone()).isEqualTo("0987654321");
    }

    static User existingUser() {
        return User.builder()
                .id(USER_ID)
                .email("an@example.com")
                .passwordHash("hashed-old-password")
                .fullName("Nguyen Van An")
                .phone("0901234567")
                .dateOfBirth(LocalDate.of(2000, 1, 31))
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .emailVerified(false)
                .build();
    }
}
```

- [ ] **Step 6: Viết test fail cho controller**

`identity-service/src/test/java/com/veggiepal/controller/ProfileControllerTest.java`:

```java
package com.veggiepal.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.veggiepal.configuration.JwtConfig;
import com.veggiepal.configuration.SecurityConfig;
import com.veggiepal.configuration.SecurityExceptionHandler;
import com.veggiepal.dto.request.UpdateProfileRequest;
import com.veggiepal.dto.response.UserProfileResponse;
import com.veggiepal.service.ProfileService;

@WebMvcTest(ProfileController.class)
@Import({SecurityConfig.class, JwtConfig.class, SecurityExceptionHandler.class})
class ProfileControllerTest {

    static final Long USER_ID = 7L;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ProfileService profileService;

    static RequestPostProcessor currentUser() {
        return jwt().jwt(token -> token.claim("userId", USER_ID).claim("role", "USER"));
    }

    @Test
    void getProfile_usesUserIdFromToken() throws Exception {
        when(profileService.getProfile(USER_ID))
                .thenReturn(UserProfileResponse.builder().id(USER_ID).email("an@example.com").build());

        mockMvc.perform(get("/users/me").with(currentUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.email").value("an@example.com"));
    }

    @Test
    void updateProfile_passesRequestForCurrentUser() throws Exception {
        when(profileService.updateProfile(eq(USER_ID), any(UpdateProfileRequest.class)))
                .thenReturn(UserProfileResponse.builder().id(USER_ID).fullName("Tran Thi Binh").build());

        mockMvc.perform(patch("/users/me").with(currentUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName": "Tran Thi Binh"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.fullName").value("Tran Thi Binh"));

        verify(profileService).updateProfile(eq(USER_ID), any(UpdateProfileRequest.class));
    }

    @Test
    void updateProfile_blankFullName_returnsFullNameRequired() throws Exception {
        mockMvc.perform(patch("/users/me").with(currentUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fullName": "   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1006));
    }

    @Test
    void updateProfile_futureDateOfBirth_returnsInvalidDateOfBirth() throws Exception {
        String tomorrow = LocalDate.now().plusDays(1).toString();

        mockMvc.perform(patch("/users/me").with(currentUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dateOfBirth\": \"" + tomorrow + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1013));
    }

    @Test
    void updateProfile_invalidDateFormat_returnsInvalidRequest() throws Exception {
        mockMvc.perform(patch("/users/me").with(currentUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dateOfBirth": "2000-13-40"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1018));
    }
}
```

- [ ] **Step 7: Chạy test, xác nhận fail**

Run: `./mvnw test -Dtest='ProfileServiceTest,ProfileControllerTest'`
Expected: FAIL khi biên dịch, lỗi `cannot find symbol: class ProfileService` / `class ProfileController`.

- [ ] **Step 8: Tạo `ProfileService`**

`identity-service/src/main/java/com/veggiepal/service/ProfileService.java`:

```java
package com.veggiepal.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.veggiepal.dto.request.UpdateProfileRequest;
import com.veggiepal.dto.response.UserProfileResponse;
import com.veggiepal.entity.User;
import com.veggiepal.exception.AppException;
import com.veggiepal.exception.ErrorCode;
import com.veggiepal.mapper.UserMapper;
import com.veggiepal.repository.UserRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProfileService {

    UserRepository userRepository;
    UserMapper userMapper;

    public UserProfileResponse getProfile(Long userId) {

        return userMapper.toUserProfileResponse(findUser(userId));
    }

    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {

        User user = findUser(userId);

        userMapper.updateProfile(user, request);
        user.setFullName(user.getFullName().trim());
        user.setPhone(
                StringUtils.hasText(user.getPhone())
                        ? user.getPhone().trim()
                        : null
        );

        userRepository.save(user);
        return userMapper.toUserProfileResponse(user);
    }

    private User findUser(Long userId) {

        return userRepository
                .findById(userId)
                .orElseThrow(
                        () -> new AppException(
                                ErrorCode.USER_NOT_EXISTED
                        )
                );
    }
}
```

- [ ] **Step 9: Tạo `CurrentUser` và `ProfileController`**

`identity-service/src/main/java/com/veggiepal/controller/CurrentUser.java`:

```java
package com.veggiepal.controller;

import org.springframework.security.oauth2.jwt.Jwt;

import com.veggiepal.exception.AppException;
import com.veggiepal.exception.ErrorCode;

public final class CurrentUser {

    private static final String USER_ID_CLAIM = "userId";

    private CurrentUser() {
    }

    public static Long id(Jwt jwt) {

        Object userId = jwt.getClaim(USER_ID_CLAIM);

        if (userId instanceof Number number) {
            return number.longValue();
        }

        throw new AppException(ErrorCode.UNAUTHENTICATED);
    }
}
```

`identity-service/src/main/java/com/veggiepal/controller/ProfileController.java`:

```java
package com.veggiepal.controller;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import com.veggiepal.dto.request.UpdateProfileRequest;
import com.veggiepal.dto.response.ApiResponse;
import com.veggiepal.dto.response.UserProfileResponse;
import com.veggiepal.service.ProfileService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/users/me")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Profile", description = "Current user profile APIs")
public class ProfileController {

    ProfileService profileService;

    @Operation(
            summary = "Get current user profile"
    )
    @GetMapping
    ApiResponse<UserProfileResponse> getProfile(@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {

        return ApiResponse
                .<UserProfileResponse>builder()
                .result(profileService.getProfile(CurrentUser.id(jwt)))
                .build();
    }

    @Operation(
            summary = "Update current user profile; null fields are left unchanged, phone \"\" clears it"
    )
    @PatchMapping
    ApiResponse<UserProfileResponse> updateProfile(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid UpdateProfileRequest request
    ) {

        return ApiResponse
                .<UserProfileResponse>builder()
                .result(profileService.updateProfile(CurrentUser.id(jwt), request))
                .build();
    }
}
```

- [ ] **Step 10: Chạy test, xác nhận pass**

Run: `./mvnw test -Dtest='ProfileServiceTest,ProfileControllerTest'`
Expected: `Tests run: 10, Failures: 0, Errors: 0`.

- [ ] **Step 11: Commit** (chỉ khi người dùng đã đồng ý)

```bash
git add identity-service/src
git commit -m "feat(identity): add GET/PATCH /users/me profile endpoints

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 5: identity-service: đổi mật khẩu (`PUT /users/me/password`)

**Files:**
- Create: `identity-service/src/main/java/com/veggiepal/dto/request/ChangePasswordRequest.java`
- Modify: `identity-service/src/main/java/com/veggiepal/exception/ErrorCode.java`
- Modify: `identity-service/src/main/java/com/veggiepal/service/ProfileService.java`
- Modify: `identity-service/src/main/java/com/veggiepal/controller/ProfileController.java`
- Test: `identity-service/src/test/java/com/veggiepal/service/ProfileServiceTest.java`
- Test: `identity-service/src/test/java/com/veggiepal/controller/ProfileControllerTest.java`

**Interfaces:**
- Consumes: bean `PasswordEncoder` (`SecurityConfig`); `ProfileServiceTest.existingUser()` (Task 4, `passwordHash = "hashed-old-password"`).
- Produces: `ProfileService.changePassword(Long userId, ChangePasswordRequest request): void`; `ChangePasswordRequest(String currentPassword, String newPassword)` (constructor theo đúng thứ tự này).

- [ ] **Step 1: Tạo DTO và mã lỗi**

`identity-service/src/main/java/com/veggiepal/dto/request/ChangePasswordRequest.java`:

```java
package com.veggiepal.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChangePasswordRequest {

    @NotBlank(message = "PASSWORD_REQUIRED")
    String currentPassword;

    @NotBlank(message = "PASSWORD_REQUIRED")
    @Size(min = 6, message = "INVALID_PASSWORD")
    String newPassword;
}
```

Trong `ErrorCode.java`, chèn ngay **trước** dòng `INVALID_REQUEST(1018, ...)`:

```java
    WRONG_PASSWORD(1011, "Current password is incorrect", HttpStatus.BAD_REQUEST),

    PASSWORD_UNCHANGED(1012, "New password must be different from current password", HttpStatus.BAD_REQUEST),

```

- [ ] **Step 2: Viết test fail cho service**

Trong `ProfileServiceTest.java`:
- Thêm import: `com.veggiepal.dto.request.ChangePasswordRequest`, `org.springframework.security.crypto.password.PasswordEncoder`, `static org.mockito.ArgumentMatchers.any`, `static org.mockito.Mockito.never`.
- Thêm field mock ngay sau `userRepository`:

```java
    @Mock
    PasswordEncoder passwordEncoder;
```

Thêm các test:

```java
    @Test
    void changePassword_wrongCurrentPassword_throwsWrongPassword() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser()));
        when(passwordEncoder.matches("wrong-pass", "hashed-old-password")).thenReturn(false);

        assertThatThrownBy(() -> profileService.changePassword(
                USER_ID, new ChangePasswordRequest("wrong-pass", "new-secret")))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.WRONG_PASSWORD));
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_sameAsCurrent_throwsPasswordUnchanged() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(existingUser()));
        when(passwordEncoder.matches("old-secret", "hashed-old-password")).thenReturn(true);

        assertThatThrownBy(() -> profileService.changePassword(
                USER_ID, new ChangePasswordRequest("old-secret", "old-secret")))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.PASSWORD_UNCHANGED));
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_success_storesNewHash() {
        User user = existingUser();
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-secret", "hashed-old-password")).thenReturn(true);
        when(passwordEncoder.matches("new-secret", "hashed-old-password")).thenReturn(false);
        when(passwordEncoder.encode("new-secret")).thenReturn("hashed-new-password");

        profileService.changePassword(USER_ID, new ChangePasswordRequest("old-secret", "new-secret"));

        assertThat(user.getPasswordHash()).isEqualTo("hashed-new-password");
        verify(userRepository).save(user);
    }
```

- [ ] **Step 3: Viết test fail cho controller**

Trong `ProfileControllerTest.java`:
- Thêm import: `static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put`, `com.veggiepal.dto.request.ChangePasswordRequest`.
- Thêm các test:

```java
    @Test
    void changePassword_success_returnsCode1000() throws Exception {
        mockMvc.perform(put("/users/me/password").with(currentUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword": "old-secret", "newPassword": "new-secret"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000));

        verify(profileService).changePassword(eq(USER_ID), any(ChangePasswordRequest.class));
    }

    @Test
    void changePassword_shortNewPassword_returnsInvalidPassword() throws Exception {
        mockMvc.perform(put("/users/me/password").with(currentUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword": "old-secret", "newPassword": "abc"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1003))
                .andExpect(jsonPath("$.message").value("Password must be at least 6 characters"));
    }

    @Test
    void changePassword_blankCurrentPassword_returnsPasswordRequired() throws Exception {
        mockMvc.perform(put("/users/me/password").with(currentUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword": "", "newPassword": "new-secret"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1010));
    }
```

- [ ] **Step 4: Chạy test, xác nhận fail**

Run: `./mvnw test -Dtest='ProfileServiceTest,ProfileControllerTest'`
Expected: FAIL khi biên dịch, lỗi `cannot find symbol: method changePassword`.

- [ ] **Step 5: Cài đặt trong service**

Trong `ProfileService.java`:
- Thêm import: `com.veggiepal.dto.request.ChangePasswordRequest`, `org.springframework.security.crypto.password.PasswordEncoder`.
- Thêm field ngay sau `UserMapper userMapper;`:

```java
    PasswordEncoder passwordEncoder;
```

- Thêm method ngay sau `updateProfile`:

```java
    public void changePassword(Long userId, ChangePasswordRequest request) {

        User user = findUser(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.WRONG_PASSWORD);
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.PASSWORD_UNCHANGED);
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }
```

- [ ] **Step 6: Thêm endpoint vào controller**

Trong `ProfileController.java`, thêm import `com.veggiepal.dto.request.ChangePasswordRequest` và thêm method:

```java
    @Operation(
            summary = "Change current user password"
    )
    @PutMapping("/password")
    ApiResponse<Void> changePassword(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid ChangePasswordRequest request
    ) {

        profileService.changePassword(CurrentUser.id(jwt), request);

        return ApiResponse
                .<Void>builder()
                .build();
    }
```

- [ ] **Step 7: Chạy test, xác nhận pass**

Run: `./mvnw test -Dtest='ProfileServiceTest,ProfileControllerTest'`
Expected: `Tests run: 16, Failures: 0, Errors: 0`.

- [ ] **Step 8: Commit** (chỉ khi người dùng đã đồng ý)

```bash
git add identity-service/src
git commit -m "feat(identity): add PUT /users/me/password

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 6: identity-service: lưu file qua S3 API + MinIO trong docker-compose

**Files:**
- Modify: `identity-service/pom.xml`
- Modify: `identity-service/src/main/resources/application.properties`
- Create: `identity-service/src/main/java/com/veggiepal/configuration/StorageProperties.java`
- Create: `identity-service/src/main/java/com/veggiepal/configuration/S3Config.java`
- Create: `identity-service/src/main/java/com/veggiepal/service/FileStorageService.java`
- Create: `identity-service/src/main/java/com/veggiepal/service/S3FileStorageService.java`
- Modify: `identity-service/src/main/java/com/veggiepal/exception/ErrorCode.java`
- Modify: `docker-compose.yml` (thay toàn bộ)
- Test: `identity-service/src/test/java/com/veggiepal/service/S3FileStorageServiceTest.java`

**Interfaces:**
- Produces:
  - `FileStorageService.upload(String key, byte[] content, String contentType): String` (trả public URL)
  - `FileStorageService.delete(String url): void` (bỏ qua `null` và URL không thuộc bucket)
  - `record StorageProperties(String endpoint, String region, String accessKey, String secretKey, String bucket, String publicUrl)`
  - `ErrorCode.FILE_UPLOAD_FAILED`

- [ ] **Step 1: Thêm dependency AWS SDK**

Trong `identity-service/pom.xml`:
- Trong `<properties>`, thêm `<aws-sdk.version>2.55.0</aws-sdk.version>`.
- Thêm dependency ngay sau dependency JWT `jjwt-jackson`:

```xml
		<!-- S3 API: MinIO locally, AWS S3 in production -->
		<dependency>
			<groupId>software.amazon.awssdk</groupId>
			<artifactId>s3</artifactId>
		</dependency>
```

- Thêm ngay sau `</dependencies>` (trước `<build>`):

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

- [ ] **Step 2: Thêm cấu hình storage**

Thêm vào cuối `identity-service/src/main/resources/application.properties`:

```properties

storage.s3.endpoint=${S3_ENDPOINT:http://localhost:9000}
storage.s3.region=${S3_REGION:us-east-1}
storage.s3.access-key=${S3_ACCESS_KEY:minioadmin}
storage.s3.secret-key=${S3_SECRET_KEY:minioadmin}
storage.s3.bucket=${S3_BUCKET:veggiepal-avatars}
storage.s3.public-url=${S3_PUBLIC_URL:http://localhost:9000/veggiepal-avatars}
```

- [ ] **Step 3: Thêm mã lỗi**

Trong `ErrorCode.java`, chèn ngay **trước** dòng `INVALID_REQUEST(1018, ...)`:

```java
    FILE_UPLOAD_FAILED(1017, "Could not upload file, please try again later", HttpStatus.SERVICE_UNAVAILABLE),

```

- [ ] **Step 4: Viết test fail**

`identity-service/src/test/java/com/veggiepal/service/S3FileStorageServiceTest.java`:

```java
package com.veggiepal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.veggiepal.configuration.StorageProperties;
import com.veggiepal.exception.AppException;
import com.veggiepal.exception.ErrorCode;

import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@ExtendWith(MockitoExtension.class)
class S3FileStorageServiceTest {

    private static final StorageProperties PROPERTIES = new StorageProperties(
            "http://localhost:9000",
            "us-east-1",
            "minioadmin",
            "minioadmin",
            "veggiepal-avatars",
            "http://localhost:9000/veggiepal-avatars"
    );

    private static final String OBJECT_URL =
            "http://localhost:9000/veggiepal-avatars/avatars/7/a.png";

    @Mock
    S3Client s3Client;

    S3FileStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new S3FileStorageService(s3Client, PROPERTIES);
    }

    @Test
    void upload_putsObjectAndReturnsPublicUrl() {
        String url = storageService.upload("avatars/7/a.png", new byte[]{1, 2, 3}, "image/png");

        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
        assertThat(captor.getValue().bucket()).isEqualTo("veggiepal-avatars");
        assertThat(captor.getValue().key()).isEqualTo("avatars/7/a.png");
        assertThat(captor.getValue().contentType()).isEqualTo("image/png");
        assertThat(url).isEqualTo(OBJECT_URL);
    }

    @Test
    void upload_storageFailure_throwsFileUploadFailed() {
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(SdkClientException.create("storage down"));

        assertThatThrownBy(() -> storageService.upload("avatars/7/a.png", new byte[]{1}, "image/png"))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FILE_UPLOAD_FAILED));
    }

    @Test
    void delete_ownUrl_deletesObjectByKey() {
        storageService.delete(OBJECT_URL);

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertThat(captor.getValue().bucket()).isEqualTo("veggiepal-avatars");
        assertThat(captor.getValue().key()).isEqualTo("avatars/7/a.png");
    }

    @Test
    void delete_nullOrForeignUrl_doesNothing() {
        storageService.delete(null);
        storageService.delete("https://cdn.example.com/avatars/7/a.png");

        verifyNoInteractions(s3Client);
    }

    @Test
    void delete_storageFailure_throwsFileUploadFailed() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(SdkClientException.create("storage down"));

        assertThatThrownBy(() -> storageService.delete(OBJECT_URL))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FILE_UPLOAD_FAILED));
    }
}
```

- [ ] **Step 5: Chạy test, xác nhận fail**

Run: `./mvnw test -Dtest=S3FileStorageServiceTest`
Expected: FAIL khi biên dịch, lỗi `cannot find symbol: class StorageProperties` / `S3FileStorageService`.

- [ ] **Step 6: Tạo phần cấu hình**

`identity-service/src/main/java/com/veggiepal/configuration/StorageProperties.java`:

```java
package com.veggiepal.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage.s3")
public record StorageProperties(
        String endpoint,
        String region,
        String accessKey,
        String secretKey,
        String bucket,
        String publicUrl
) {
}
```

`identity-service/src/main/java/com/veggiepal/configuration/S3Config.java`:

```java
package com.veggiepal.configuration;

import java.net.URI;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class S3Config {

    @Bean
    S3Client s3Client(StorageProperties storageProperties) {

        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(storageProperties.region()))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(
                                        storageProperties.accessKey(),
                                        storageProperties.secretKey()
                                )
                        )
                )
                // MinIO needs path-style URLs and only the checksums the S3 API requires
                .forcePathStyle(true)
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED);

        if (StringUtils.hasText(storageProperties.endpoint())) {
            builder.endpointOverride(URI.create(storageProperties.endpoint()));
        }

        return builder.build();
    }
}
```

- [ ] **Step 7: Tạo interface và bản cài đặt S3**

`identity-service/src/main/java/com/veggiepal/service/FileStorageService.java`:

```java
package com.veggiepal.service;

public interface FileStorageService {

    /** Stores the object and returns its public URL. */
    String upload(String key, byte[] content, String contentType);

    /** Deletes the object behind a URL returned by {@link #upload}; other URLs are ignored. */
    void delete(String url);
}
```

`identity-service/src/main/java/com/veggiepal/service/S3FileStorageService.java`:

```java
package com.veggiepal.service;

import org.springframework.stereotype.Service;

import com.veggiepal.configuration.StorageProperties;
import com.veggiepal.exception.AppException;
import com.veggiepal.exception.ErrorCode;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class S3FileStorageService implements FileStorageService {

    S3Client s3Client;
    StorageProperties storageProperties;

    @Override
    public String upload(String key, byte[] content, String contentType) {

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(storageProperties.bucket())
                .key(key)
                .contentType(contentType)
                .build();

        try {
            s3Client.putObject(request, RequestBody.fromBytes(content));
        } catch (SdkException exception) {
            log.error("Could not upload object to storage", exception);
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        return publicUrlPrefix() + key;
    }

    @Override
    public void delete(String url) {

        String prefix = publicUrlPrefix();

        if (url == null || !url.startsWith(prefix)) {
            return;
        }

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(storageProperties.bucket())
                .key(url.substring(prefix.length()))
                .build();

        try {
            s3Client.deleteObject(request);
        } catch (SdkException exception) {
            log.error("Could not delete object from storage", exception);
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    private String publicUrlPrefix() {

        return storageProperties.publicUrl() + "/";
    }
}
```

- [ ] **Step 8: Chạy test, xác nhận pass**

Run: `./mvnw test -Dtest=S3FileStorageServiceTest`
Expected: `Tests run: 5, Failures: 0, Errors: 0`.

- [ ] **Step 9: Thêm MinIO vào docker-compose**

Thay toàn bộ `docker-compose.yml` ở thư mục gốc repo:

```yaml
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

  minio:
    image: quay.io/minio/minio:RELEASE.2025-09-07T16-13-09Z
    container_name: veggiepal-minio
    restart: always
    command: server /data --console-address ":9001"

    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin

    ports:
      - "9000:9000"
      - "9001:9001"

    volumes:
      - minio_data:/data

    healthcheck:
      test: ["CMD", "mc", "ready", "local"]
      interval: 5s
      timeout: 5s
      retries: 12

  # One-shot: create the avatar bucket and allow anonymous downloads
  minio-init:
    image: quay.io/minio/mc:RELEASE.2025-08-13T08-35-41Z
    container_name: veggiepal-minio-init
    depends_on:
      minio:
        condition: service_healthy
    entrypoint: >
      /bin/sh -c "
      mc alias set local http://minio:9000 minioadmin minioadmin &&
      mc mb --ignore-existing local/veggiepal-avatars &&
      mc anonymous set download local/veggiepal-avatars
      "

volumes:
  mysql_data:
  minio_data:
```

- [ ] **Step 10: Kiểm tra cú pháp compose**

Run (ở thư mục gốc repo): `docker compose config --quiet && echo OK`
Expected: `OK`. Lệnh này không cần Docker daemon đang chạy.

- [ ] **Step 11: Commit** (chỉ khi người dùng đã đồng ý)

```bash
git add identity-service/pom.xml identity-service/src docker-compose.yml
git commit -m "feat(identity): add S3 file storage and MinIO for local development

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 7: identity-service: upload avatar (`POST /users/me/avatar`)

**Files:**
- Create: `identity-service/src/main/java/com/veggiepal/enums/ImageType.java`
- Create: `identity-service/src/main/java/com/veggiepal/service/ImageTypeDetector.java`
- Modify: `identity-service/src/main/java/com/veggiepal/exception/ErrorCode.java`
- Modify: `identity-service/src/main/java/com/veggiepal/exception/GlobalExceptionHandler.java`
- Modify: `identity-service/src/main/resources/application.properties`
- Modify: `identity-service/src/main/java/com/veggiepal/service/ProfileService.java`
- Modify: `identity-service/src/main/java/com/veggiepal/controller/ProfileController.java`
- Test: `identity-service/src/test/java/com/veggiepal/service/ImageTypeDetectorTest.java`
- Test: `identity-service/src/test/java/com/veggiepal/service/ProfileServiceTest.java`
- Test: `identity-service/src/test/java/com/veggiepal/controller/ProfileControllerTest.java`

**Interfaces:**
- Consumes: `FileStorageService` (Task 6); `errorResponse(ErrorCode)` trong `GlobalExceptionHandler` (Task 3).
- Produces:
  - `enum ImageType { JPEG, PNG, WEBP }` với `getContentType()`, `getExtension()`
  - `ImageTypeDetector.detect(byte[]): Optional<ImageType>`
  - `ProfileService.uploadAvatar(Long userId, MultipartFile file): UserProfileResponse`

- [ ] **Step 1: Viết test fail cho `ImageTypeDetector`**

`identity-service/src/test/java/com/veggiepal/service/ImageTypeDetectorTest.java`:

```java
package com.veggiepal.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.veggiepal.enums.ImageType;

class ImageTypeDetectorTest {

    static final byte[] JPEG_BYTES = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00};

    static final byte[] PNG_BYTES = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};

    static final byte[] WEBP_BYTES = {'R', 'I', 'F', 'F', 0x10, 0x00, 0x00, 0x00, 'W', 'E', 'B', 'P', 'V', 'P', '8', ' '};

    @Test
    void detect_recognisesSupportedImages() {
        assertThat(ImageTypeDetector.detect(JPEG_BYTES)).contains(ImageType.JPEG);
        assertThat(ImageTypeDetector.detect(PNG_BYTES)).contains(ImageType.PNG);
        assertThat(ImageTypeDetector.detect(WEBP_BYTES)).contains(ImageType.WEBP);
    }

    @Test
    void detect_rejectsOtherContent() {
        assertThat(ImageTypeDetector.detect("GIF89a-not-supported".getBytes(StandardCharsets.US_ASCII))).isEmpty();
        assertThat(ImageTypeDetector.detect("<html></html>".getBytes(StandardCharsets.US_ASCII))).isEmpty();
    }

    @Test
    void detect_rejectsTooShortOrNull() {
        assertThat(ImageTypeDetector.detect(new byte[]{(byte) 0xFF, (byte) 0xD8})).isEmpty();
        assertThat(ImageTypeDetector.detect(new byte[]{'R', 'I', 'F', 'F', 0x10})).isEmpty();
        assertThat(ImageTypeDetector.detect(null)).isEmpty();
    }
}
```

- [ ] **Step 2: Viết test fail cho avatar trong service**

Trong `ProfileServiceTest.java`:
- Thêm import:
  - `org.springframework.mock.web.MockMultipartFile`
  - `org.mockito.ArgumentCaptor`
  - `static org.mockito.ArgumentMatchers.anyString`
  - `static org.mockito.ArgumentMatchers.aryEq`
  - `static org.mockito.ArgumentMatchers.eq`
  - `static org.mockito.Mockito.doThrow`
  - `static org.mockito.Mockito.verifyNoInteractions`
  - `java.util.Arrays`
- Thêm field mock ngay sau `passwordEncoder`:

```java
    @Mock
    FileStorageService fileStorageService;
```

Thêm hằng số và các test:

```java
    static final byte[] PNG_BYTES = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};

    static final String OLD_AVATAR_URL = "http://localhost:9000/veggiepal-avatars/avatars/7/old.png";

    static final String NEW_AVATAR_URL = "http://localhost:9000/veggiepal-avatars/avatars/7/new.png";

    @Test
    void uploadAvatar_emptyFile_throwsAvatarRequired() {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> profileService.uploadAvatar(USER_ID, file))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AVATAR_REQUIRED));
        verifyNoInteractions(fileStorageService);
    }

    @Test
    void uploadAvatar_tooLarge_throwsAvatarTooLarge() {
        byte[] content = Arrays.copyOf(PNG_BYTES, 2 * 1024 * 1024 + 1);
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", content);

        assertThatThrownBy(() -> profileService.uploadAvatar(USER_ID, file))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AVATAR_TOO_LARGE));
        verifyNoInteractions(fileStorageService);
    }

    @Test
    void uploadAvatar_unsupportedContentType_throwsInvalidAvatarType() {
        MockMultipartFile file = new MockMultipartFile("file", "a.gif", "image/gif", PNG_BYTES);

        assertThatThrownBy(() -> profileService.uploadAvatar(USER_ID, file))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_AVATAR_TYPE));
        verifyNoInteractions(fileStorageService);
    }

    @Test
    void uploadAvatar_bytesDoNotMatchContentType_throwsInvalidAvatarType() {
        MockMultipartFile file = new MockMultipartFile("file", "a.jpg", "image/jpeg", PNG_BYTES);

        assertThatThrownBy(() -> profileService.uploadAvatar(USER_ID, file))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_AVATAR_TYPE));
        verifyNoInteractions(fileStorageService);
    }

    @Test
    void uploadAvatar_success_uploadsSavesAndDeletesPreviousAvatar() {
        User user = existingUser();
        user.setAvatarUrl(OLD_AVATAR_URL);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(fileStorageService.upload(anyString(), aryEq(PNG_BYTES), eq("image/png"))).thenReturn(NEW_AVATAR_URL);
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", PNG_BYTES);

        UserProfileResponse profile = profileService.uploadAvatar(USER_ID, file);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(fileStorageService).upload(keyCaptor.capture(), aryEq(PNG_BYTES), eq("image/png"));
        assertThat(keyCaptor.getValue()).matches("avatars/7/[0-9a-f-]{36}\\.png");
        assertThat(user.getAvatarUrl()).isEqualTo(NEW_AVATAR_URL);
        assertThat(profile.getAvatarUrl()).isEqualTo(NEW_AVATAR_URL);
        verify(userRepository).save(user);
        verify(fileStorageService).delete(OLD_AVATAR_URL);
    }

    @Test
    void uploadAvatar_deletingPreviousAvatarFails_stillSucceeds() {
        User user = existingUser();
        user.setAvatarUrl(OLD_AVATAR_URL);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(fileStorageService.upload(anyString(), aryEq(PNG_BYTES), eq("image/png"))).thenReturn(NEW_AVATAR_URL);
        doThrow(new AppException(ErrorCode.FILE_UPLOAD_FAILED)).when(fileStorageService).delete(OLD_AVATAR_URL);
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", PNG_BYTES);

        UserProfileResponse profile = profileService.uploadAvatar(USER_ID, file);

        assertThat(profile.getAvatarUrl()).isEqualTo(NEW_AVATAR_URL);
    }

    @Test
    void uploadAvatar_saveFails_deletesNewFileAndRethrows() {
        User user = existingUser();
        user.setAvatarUrl(OLD_AVATAR_URL);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(fileStorageService.upload(anyString(), aryEq(PNG_BYTES), eq("image/png"))).thenReturn(NEW_AVATAR_URL);
        when(userRepository.save(user)).thenThrow(new IllegalStateException("db down"));
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", PNG_BYTES);

        assertThatThrownBy(() -> profileService.uploadAvatar(USER_ID, file))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("db down");
        verify(fileStorageService).delete(NEW_AVATAR_URL);
        verify(fileStorageService, never()).delete(OLD_AVATAR_URL);
    }
```

- [ ] **Step 3: Viết test fail cho controller**

Trong `ProfileControllerTest.java`:
- Thêm import:
  - `static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart`
  - `org.springframework.mock.web.MockMultipartFile`
  - `org.springframework.web.multipart.MultipartFile`
- Thêm các test:

```java
    @Test
    void uploadAvatar_passesFileForCurrentUser() throws Exception {
        String avatarUrl = "http://localhost:9000/veggiepal-avatars/avatars/7/a.png";
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", new byte[]{(byte) 0x89, 0x50});
        when(profileService.uploadAvatar(eq(USER_ID), any(MultipartFile.class)))
                .thenReturn(UserProfileResponse.builder().avatarUrl(avatarUrl).build());

        mockMvc.perform(multipart("/users/me/avatar").file(file).with(currentUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.avatarUrl").value(avatarUrl));
    }

    @Test
    void uploadAvatar_missingFile_returnsAvatarRequired() throws Exception {
        mockMvc.perform(multipart("/users/me/avatar").with(currentUser()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1014));
    }
```

- [ ] **Step 4: Chạy test, xác nhận fail**

Run: `./mvnw test -Dtest='ImageTypeDetectorTest,ProfileServiceTest,ProfileControllerTest'`
Expected: FAIL khi biên dịch, lỗi `cannot find symbol: class ImageTypeDetector` / `method uploadAvatar`.

- [ ] **Step 5: Tạo `ImageType` và `ImageTypeDetector`**

`identity-service/src/main/java/com/veggiepal/enums/ImageType.java`:

```java
package com.veggiepal.enums;

import lombok.Getter;

@Getter
public enum ImageType {

    JPEG("image/jpeg", "jpg"),

    PNG("image/png", "png"),

    WEBP("image/webp", "webp");

    ImageType(
            String contentType,
            String extension
    ) {
        this.contentType = contentType;
        this.extension = extension;
    }

    final String contentType;

    final String extension;
}
```

`identity-service/src/main/java/com/veggiepal/service/ImageTypeDetector.java`:

```java
package com.veggiepal.service;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

import com.veggiepal.enums.ImageType;

public final class ImageTypeDetector {

    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    private static final byte[] RIFF_SIGNATURE = "RIFF".getBytes(StandardCharsets.US_ASCII);

    private static final byte[] WEBP_SIGNATURE = "WEBP".getBytes(StandardCharsets.US_ASCII);

    private ImageTypeDetector() {
    }

    public static Optional<ImageType> detect(byte[] content) {

        if (content == null) {
            return Optional.empty();
        }

        if (hasSignature(content, 0, JPEG_SIGNATURE)) {
            return Optional.of(ImageType.JPEG);
        }

        if (hasSignature(content, 0, PNG_SIGNATURE)) {
            return Optional.of(ImageType.PNG);
        }

        if (hasSignature(content, 0, RIFF_SIGNATURE) && hasSignature(content, 8, WEBP_SIGNATURE)) {
            return Optional.of(ImageType.WEBP);
        }

        return Optional.empty();
    }

    private static boolean hasSignature(byte[] content, int offset, byte[] signature) {

        if (content.length < offset + signature.length) {
            return false;
        }

        return Arrays.equals(
                content, offset, offset + signature.length,
                signature, 0, signature.length
        );
    }
}
```

- [ ] **Step 6: Thêm mã lỗi, handler multipart và giới hạn kích thước**

Trong `ErrorCode.java`, chèn ngay **trước** dòng `INVALID_REQUEST(1018, ...)`:

```java
    AVATAR_REQUIRED(1014, "Avatar file is required", HttpStatus.BAD_REQUEST),

    INVALID_AVATAR_TYPE(1015, "Avatar must be a JPEG, PNG or WEBP image", HttpStatus.BAD_REQUEST),

    AVATAR_TOO_LARGE(1016, "Avatar must not exceed 2MB", HttpStatus.BAD_REQUEST),

```

Trong `GlobalExceptionHandler.java`:
- Thêm import:
  - `org.springframework.web.multipart.MaxUploadSizeExceededException`
  - `org.springframework.web.multipart.support.MissingServletRequestPartException`
- Thêm các method sau ngay sau `handlingInvalidRequest`:

```java
    @ExceptionHandler(value = MaxUploadSizeExceededException.class)
    ResponseEntity<ApiResponse<?>> handlingMaxUploadSize(
            MaxUploadSizeExceededException exception
    ) {

        return errorResponse(ErrorCode.AVATAR_TOO_LARGE);
    }

    @ExceptionHandler(value = MissingServletRequestPartException.class)
    ResponseEntity<ApiResponse<?>> handlingMissingPart(
            MissingServletRequestPartException exception
    ) {

        return errorResponse(ErrorCode.AVATAR_REQUIRED);
    }
```

Thêm vào cuối `identity-service/src/main/resources/application.properties`:

```properties

spring.servlet.multipart.max-file-size=2MB
spring.servlet.multipart.max-request-size=3MB
```

- [ ] **Step 7: Cài đặt `uploadAvatar`**

Thay toàn bộ `ProfileService.java` bằng:

```java
package com.veggiepal.service;

import java.io.IOException;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.veggiepal.dto.request.ChangePasswordRequest;
import com.veggiepal.dto.request.UpdateProfileRequest;
import com.veggiepal.dto.response.UserProfileResponse;
import com.veggiepal.entity.User;
import com.veggiepal.enums.ImageType;
import com.veggiepal.exception.AppException;
import com.veggiepal.exception.ErrorCode;
import com.veggiepal.mapper.UserMapper;
import com.veggiepal.repository.UserRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ProfileService {

    private static final long MAX_AVATAR_BYTES = 2L * 1024 * 1024;

    UserRepository userRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;
    FileStorageService fileStorageService;

    public UserProfileResponse getProfile(Long userId) {

        return userMapper.toUserProfileResponse(findUser(userId));
    }

    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {

        User user = findUser(userId);

        userMapper.updateProfile(user, request);
        user.setFullName(user.getFullName().trim());
        user.setPhone(
                StringUtils.hasText(user.getPhone())
                        ? user.getPhone().trim()
                        : null
        );

        userRepository.save(user);
        return userMapper.toUserProfileResponse(user);
    }

    public void changePassword(Long userId, ChangePasswordRequest request) {

        User user = findUser(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.WRONG_PASSWORD);
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.PASSWORD_UNCHANGED);
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    public UserProfileResponse uploadAvatar(Long userId, MultipartFile file) {

        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.AVATAR_REQUIRED);
        }

        if (file.getSize() > MAX_AVATAR_BYTES) {
            throw new AppException(ErrorCode.AVATAR_TOO_LARGE);
        }

        byte[] content = readContent(file);

        // The declared content type must match the real file signature
        ImageType imageType = ImageTypeDetector
                .detect(content)
                .filter(type -> type.getContentType().equals(file.getContentType()))
                .orElseThrow(
                        () -> new AppException(
                                ErrorCode.INVALID_AVATAR_TYPE
                        )
                );

        User user = findUser(userId);
        String previousAvatarUrl = user.getAvatarUrl();

        String key = "avatars/" + userId + "/" + UUID.randomUUID() + "." + imageType.getExtension();
        String avatarUrl = fileStorageService.upload(key, content, imageType.getContentType());

        user.setAvatarUrl(avatarUrl);

        try {
            userRepository.save(user);
        } catch (RuntimeException exception) {
            deleteQuietly(avatarUrl);
            throw exception;
        }

        deleteQuietly(previousAvatarUrl);
        return userMapper.toUserProfileResponse(user);
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
            log.warn("Could not delete avatar object from storage", exception);
        }
    }

    private User findUser(Long userId) {

        return userRepository
                .findById(userId)
                .orElseThrow(
                        () -> new AppException(
                                ErrorCode.USER_NOT_EXISTED
                        )
                );
    }
}
```

- [ ] **Step 8: Thêm endpoint vào controller**

Trong `ProfileController.java`, thêm import `org.springframework.http.MediaType` và `org.springframework.web.multipart.MultipartFile`, rồi thêm method:

```java
    @Operation(
            summary = "Upload current user avatar (JPEG, PNG or WEBP, max 2MB)"
    )
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ApiResponse<UserProfileResponse> uploadAvatar(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @RequestPart("file") MultipartFile file
    ) {

        return ApiResponse
                .<UserProfileResponse>builder()
                .result(profileService.uploadAvatar(CurrentUser.id(jwt), file))
                .build();
    }
```

- [ ] **Step 9: Chạy test, xác nhận pass**

Run: `./mvnw test -Dtest='!VeggiepalApplicationTests'`
Expected: toàn bộ test của identity-service pass (`BUILD SUCCESS`), trong đó `ImageTypeDetectorTest` có 3 test, `ProfileServiceTest` có 15 test, `ProfileControllerTest` có 10 test.

- [ ] **Step 10: Commit** (chỉ khi người dùng đã đồng ý)

```bash
git add identity-service/src
git commit -m "feat(identity): add POST /users/me/avatar with magic-byte validation

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 8: nutrition-service: dựng khung project (bảo mật, lỗi chung, Swagger)

**Files:**
- Create: `nutrition-service/pom.xml`
- Create: `nutrition-service/mvnw`, `nutrition-service/mvnw.cmd`, `nutrition-service/.mvn/wrapper/maven-wrapper.properties` (copy)
- Create: `nutrition-service/src/main/resources/application.properties`
- Create (tất cả trong `nutrition-service/src/main/java/com/veggiepal/nutrition/`):
  - `NutritionServiceApplication.java`
  - `configuration/JwtConfig.java`, `configuration/SecurityConfig.java`, `configuration/SecurityExceptionHandler.java`, `configuration/OpenApiConfig.java`
  - `controller/CurrentUser.java`
  - `dto/response/ApiResponse.java`
  - `exception/AppException.java`, `exception/ErrorCode.java`, `exception/GlobalExceptionHandler.java`
- Test (trong `nutrition-service/src/test/java/com/veggiepal/nutrition/`):
  - `NutritionServiceApplicationTests.java`
  - `configuration/JwtConfigTest.java`, `configuration/SecurityExceptionHandlerTest.java`, `configuration/SecurityConfigTest.java`

**Interfaces:**
- Produces:
  - Trong package `com.veggiepal.nutrition.*`: `ApiResponse<T>`, `AppException(ErrorCode)`, `ErrorCode` (các mã chung), `GlobalExceptionHandler` (có helper `errorResponse`), `CurrentUser.id(Jwt): Long`
  - Mọi `@WebMvcTest` của nutrition-service phải có `@Import({SecurityConfig.class, JwtConfig.class, SecurityExceptionHandler.class})`
  - **Quy ước:** hằng `ErrorCode` mới luôn được chèn **ngay trước** dòng `INVALID_REQUEST(1018, ...)`

- [ ] **Step 1: Copy Maven wrapper**

Run (ở thư mục gốc repo):

```bash
mkdir -p nutrition-service
cp identity-service/mvnw identity-service/mvnw.cmd nutrition-service/
cp -r identity-service/.mvn nutrition-service/
```

- [ ] **Step 2: Tạo `pom.xml`**

`nutrition-service/pom.xml`:

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
	<artifactId>nutrition-service</artifactId>
	<version>0.0.1-SNAPSHOT</version>

	<name>nutrition-service</name>
	<description>VeggiePal Nutrition Service</description>

	<properties>
		<java.version>21</java.version>
		<mapstruct.version>1.6.3</mapstruct.version>
	</properties>

	<dependencies>

		<!-- REST API -->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-webmvc</artifactId>
		</dependency>

		<!-- JPA / Hibernate -->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-data-jpa</artifactId>
		</dependency>

		<!-- Spring Security -->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-security</artifactId>
		</dependency>

		<!-- OAuth2 Resource Server: validate JWT -->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-security-oauth2-resource-server</artifactId>
		</dependency>

		<!-- Validation -->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-starter-validation</artifactId>
		</dependency>

		<!-- MySQL -->
		<dependency>
			<groupId>com.mysql</groupId>
			<artifactId>mysql-connector-j</artifactId>
			<scope>runtime</scope>
		</dependency>

		<!-- Lombok -->
		<dependency>
			<groupId>org.projectlombok</groupId>
			<artifactId>lombok</artifactId>
			<optional>true</optional>
		</dependency>

		<!-- DevTools -->
		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-devtools</artifactId>
			<scope>runtime</scope>
			<optional>true</optional>
		</dependency>

		<!-- Swagger -->
		<dependency>
			<groupId>org.springdoc</groupId>
			<artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
			<version>3.1.0</version>
		</dependency>

		<!-- MapStruct -->
		<dependency>
			<groupId>org.mapstruct</groupId>
			<artifactId>mapstruct</artifactId>
			<version>${mapstruct.version}</version>
		</dependency>

		<!-- Test -->
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

	<build>
		<plugins>

			<!-- Spring Boot -->
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
			</plugin>

			<!-- Lombok + MapStruct annotation processors -->
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

- [ ] **Step 3: Tạo `application.properties` và class main**

`nutrition-service/src/main/resources/application.properties`:

```properties
spring.application.name=nutrition-service
server.port=8082

spring.datasource.url=jdbc:mysql://localhost:3307/veggiepal_nutrition?createDatabaseIfNotExist=true
spring.datasource.username=root
spring.datasource.password=12345

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.open-in-view=false

jwt.secret=${JWT_SECRET:veggiepal-secret-key-must-be-at-least-32-characters}

springdoc.swagger-ui.path=/swagger-ui.html
springdoc.api-docs.path=/v3/api-docs
```

`nutrition-service/src/main/java/com/veggiepal/nutrition/NutritionServiceApplication.java`:

```java
package com.veggiepal.nutrition;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NutritionServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(NutritionServiceApplication.class, args);
	}

}
```

- [ ] **Step 4: Tạo các class dùng chung (response, exception)**

`nutrition-service/src/main/java/com/veggiepal/nutrition/dto/response/ApiResponse.java`:

```java
package com.veggiepal.nutrition.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    @Builder.Default
    int code = 1000;

    String message;

    T result;
}
```

`nutrition-service/src/main/java/com/veggiepal/nutrition/exception/AppException.java`:

```java
package com.veggiepal.nutrition.exception;

public class AppException extends RuntimeException {

    private ErrorCode errorCode;

    public AppException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(ErrorCode errorCode) {
        this.errorCode = errorCode;
    }
}
```

`nutrition-service/src/main/java/com/veggiepal/nutrition/exception/ErrorCode.java`:

```java
package com.veggiepal.nutrition.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // Shared codes: keep the same numbers as identity-service
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),

    INVALID_KEY(1001, "Invalid validation key", HttpStatus.BAD_REQUEST),

    UNAUTHENTICATED(1008, "Unauthenticated", HttpStatus.UNAUTHORIZED),

    UNAUTHORIZED(1009, "You do not have permission", HttpStatus.FORBIDDEN),

    INVALID_REQUEST(1018, "Invalid request data", HttpStatus.BAD_REQUEST);

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

`nutrition-service/src/main/java/com/veggiepal/nutrition/exception/GlobalExceptionHandler.java`:

```java
package com.veggiepal.nutrition.exception;

import java.util.Map;
import java.util.Objects;

import jakarta.validation.ConstraintViolation;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.veggiepal.nutrition.dto.response.ApiResponse;

import lombok.extern.slf4j.Slf4j;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String MIN_ATTRIBUTE = "min";

    @ExceptionHandler(value = Exception.class)
    ResponseEntity<ApiResponse<?>> handlingException(
            Exception exception
    ) {

        log.error("Exception: ", exception);

        return errorResponse(ErrorCode.UNCATEGORIZED_EXCEPTION);
    }

    @ExceptionHandler(value = AppException.class)
    ResponseEntity<ApiResponse<?>> handlingAppException(
            AppException exception
    ) {

        return errorResponse(exception.getErrorCode());
    }

    @ExceptionHandler(value = {
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class
    })
    ResponseEntity<ApiResponse<?>> handlingInvalidRequest(
            Exception exception
    ) {

        return errorResponse(ErrorCode.INVALID_REQUEST);
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<?>> handlingValidation(
            MethodArgumentNotValidException exception
    ) {

        String enumKey =
                exception.getFieldError().getDefaultMessage();

        ErrorCode errorCode =
                ErrorCode.INVALID_KEY;

        Map<String, Object> attributes = null;

        try {

            errorCode =
                    ErrorCode.valueOf(enumKey);

            ConstraintViolation<?> constraintViolation =
                    exception
                            .getBindingResult()
                            .getAllErrors()
                            .getFirst()
                            .unwrap(ConstraintViolation.class);

            attributes =
                    constraintViolation
                            .getConstraintDescriptor()
                            .getAttributes();

        } catch (IllegalArgumentException ignored) {

        }

        ApiResponse<?> apiResponse =
                ApiResponse.builder()
                        .code(errorCode.getCode())
                        .message(
                                Objects.nonNull(attributes)
                                        ? mapAttribute(
                                        errorCode.getMessage(),
                                        attributes
                                )
                                        : errorCode.getMessage()
                        )
                        .build();

        return ResponseEntity
                .status(errorCode.getStatusCode())
                .body(apiResponse);
    }

    private ResponseEntity<ApiResponse<?>> errorResponse(
            ErrorCode errorCode
    ) {

        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();

        return ResponseEntity
                .status(errorCode.getStatusCode())
                .body(apiResponse);
    }

    private String mapAttribute(
            String message,
            Map<String, Object> attributes
    ) {

        String minValue =
                String.valueOf(
                        attributes.get(MIN_ATTRIBUTE)
                );

        return message.replace(
                "{" + MIN_ATTRIBUTE + "}",
                minValue
        );
    }
}
```

- [ ] **Step 5: Viết test fail cho phần bảo mật**

`nutrition-service/src/test/java/com/veggiepal/nutrition/NutritionServiceApplicationTests.java`:

```java
package com.veggiepal.nutrition;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class NutritionServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
```

`nutrition-service/src/test/java/com/veggiepal/nutrition/configuration/JwtConfigTest.java`:

```java
package com.veggiepal.nutrition.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

class JwtConfigTest {

    private static final String SECRET = "veggiepal-secret-key-must-be-at-least-32-characters";

    private final JwtDecoder decoder = new JwtConfig().jwtDecoder(SECRET);

    @Test
    void jwtDecoder_acceptsHs256TokenSignedWithSharedSecret() throws Exception {
        Jwt jwt = decoder.decode(signedToken(JWSAlgorithm.HS256));

        assertThat(((Number) jwt.getClaim("userId")).longValue()).isEqualTo(7L);
        assertThat(jwt.getClaimAsString("role")).isEqualTo("USER");
    }

    @Test
    void jwtDecoder_rejectsTokenSignedWithOtherAlgorithm() throws Exception {
        String hs384Token = signedToken(JWSAlgorithm.HS384);

        assertThatThrownBy(() -> decoder.decode(hs384Token)).isInstanceOf(JwtException.class);
    }

    private static String signedToken(JWSAlgorithm algorithm) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("an@example.com")
                .claim("userId", 7L)
                .claim("role", "USER")
                .issueTime(new Date())
                .expirationTime(new Date(System.currentTimeMillis() + 60_000))
                .build();

        SignedJWT jwt = new SignedJWT(new JWSHeader(algorithm), claims);
        jwt.sign(new MACSigner(SECRET.getBytes(StandardCharsets.UTF_8)));
        return jwt.serialize();
    }
}
```

`nutrition-service/src/test/java/com/veggiepal/nutrition/configuration/SecurityExceptionHandlerTest.java`:

```java
package com.veggiepal.nutrition.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import tools.jackson.databind.json.JsonMapper;

class SecurityExceptionHandlerTest {

    private final SecurityExceptionHandler handler =
            new SecurityExceptionHandler(JsonMapper.builder().build());

    @Test
    void commence_writesUnauthenticatedApiResponse() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.commence(new MockHttpServletRequest(), response, new BadCredentialsException("bad token"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("\"code\":1008");
    }

    @Test
    void handle_writesUnauthorizedApiResponse() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(new MockHttpServletRequest(), response, new AccessDeniedException("denied"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("\"code\":1009");
    }
}
```

`nutrition-service/src/test/java/com/veggiepal/nutrition/configuration/SecurityConfigTest.java`. File này có một controller chỉ dùng cho test, để kiểm tra filter chain và `CurrentUser`:

```java
package com.veggiepal.nutrition.configuration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.veggiepal.nutrition.controller.CurrentUser;
import com.veggiepal.nutrition.dto.response.ApiResponse;

@WebMvcTest(controllers = SecurityConfigTest.ProbeController.class)
@Import({SecurityConfig.class, JwtConfig.class, SecurityExceptionHandler.class})
class SecurityConfigTest {

    @RestController
    static class ProbeController {

        @GetMapping("/nutrition/me/probe")
        ApiResponse<Long> probe(@AuthenticationPrincipal Jwt jwt) {
            return ApiResponse.<Long>builder().result(CurrentUser.id(jwt)).build();
        }
    }

    @Autowired
    MockMvc mockMvc;

    @Test
    void withoutToken_returns401ApiResponse() throws Exception {
        mockMvc.perform(get("/nutrition/me/probe"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1008));
    }

    @Test
    void malformedToken_returns401ApiResponse() throws Exception {
        mockMvc.perform(get("/nutrition/me/probe").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1008));
    }

    @Test
    void validToken_exposesUserIdClaim() throws Exception {
        mockMvc.perform(get("/nutrition/me/probe").with(jwt().jwt(token -> token.claim("userId", 7L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(7));
    }

    @Test
    void tokenWithoutUserId_returns401ApiResponse() throws Exception {
        mockMvc.perform(get("/nutrition/me/probe").with(jwt()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1008));
    }
}
```

- [ ] **Step 6: Chạy test, xác nhận fail**

Run: `cd nutrition-service && ./mvnw test -Dtest='!NutritionServiceApplicationTests'`
Expected: FAIL khi biên dịch, lỗi `cannot find symbol: class JwtConfig` / `SecurityConfig` / `SecurityExceptionHandler` / `CurrentUser`.

- [ ] **Step 7: Tạo các class bảo mật và Swagger**

`nutrition-service/src/main/java/com/veggiepal/nutrition/configuration/JwtConfig.java`:

```java
package com.veggiepal.nutrition.configuration;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@Configuration
public class JwtConfig {

    // Must match identity-service JwtConfig: same secret, HS256
    @Bean
    public JwtDecoder jwtDecoder(@Value("${jwt.secret}") String secret) {

        SecretKey signingKey = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );

        return NimbusJwtDecoder
                .withSecretKey(signingKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}
```

`nutrition-service/src/main/java/com/veggiepal/nutrition/configuration/SecurityExceptionHandler.java`:

```java
package com.veggiepal.nutrition.configuration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.veggiepal.nutrition.dto.response.ApiResponse;
import com.veggiepal.nutrition.exception.ErrorCode;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SecurityExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    JsonMapper jsonMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {

        writeError(response, ErrorCode.UNAUTHENTICATED);
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {

        writeError(response, ErrorCode.UNAUTHORIZED);
    }

    private void writeError(
            HttpServletResponse response,
            ErrorCode errorCode
    ) throws IOException {

        ApiResponse<?> apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();

        response.setStatus(errorCode.getStatusCode().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(jsonMapper.writeValueAsString(apiResponse));
    }
}
```

`nutrition-service/src/main/java/com/veggiepal/nutrition/configuration/SecurityConfig.java`:

```java
package com.veggiepal.nutrition.configuration;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SecurityConfig {

    static final String[] PUBLIC_ENDPOINTS = {
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**"
    };

    SecurityExceptionHandler securityExceptionHandler;

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity httpSecurity
    ) throws Exception {

        httpSecurity
                .authorizeHttpRequests(
                        request -> request
                                .requestMatchers(PUBLIC_ENDPOINTS)
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
        List<RequestMatcher> publicMatchers = Arrays.stream(PUBLIC_ENDPOINTS)
                .map(pattern -> (RequestMatcher) PathPatternRequestMatcher.withDefaults().matcher(pattern))
                .toList();

        return request -> publicMatchers.stream().anyMatch(matcher -> matcher.matches(request))
                ? null
                : defaultResolver.resolve(request);
    }
}
```

`nutrition-service/src/main/java/com/veggiepal/nutrition/configuration/OpenApiConfig.java`:

```java
package com.veggiepal.nutrition.configuration;

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
                                .title("VeggiePal Nutrition Service API")
                                .version("1.0")
                                .description("Nutrition APIs for VeggiePal")
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

`nutrition-service/src/main/java/com/veggiepal/nutrition/controller/CurrentUser.java`:

```java
package com.veggiepal.nutrition.controller;

import org.springframework.security.oauth2.jwt.Jwt;

import com.veggiepal.nutrition.exception.AppException;
import com.veggiepal.nutrition.exception.ErrorCode;

public final class CurrentUser {

    private static final String USER_ID_CLAIM = "userId";

    private CurrentUser() {
    }

    public static Long id(Jwt jwt) {

        Object userId = jwt.getClaim(USER_ID_CLAIM);

        if (userId instanceof Number number) {
            return number.longValue();
        }

        throw new AppException(ErrorCode.UNAUTHENTICATED);
    }
}
```

- [ ] **Step 8: Chạy test, xác nhận pass**

Run: `./mvnw test -Dtest='!NutritionServiceApplicationTests'`
Expected: `BUILD SUCCESS`: `JwtConfigTest` có 2 test, `SecurityExceptionHandlerTest` có 2, `SecurityConfigTest` có 4. `contextLoads` sẽ được chạy ở Task 13, khi đã có MySQL.

- [ ] **Step 9: Commit** (chỉ khi người dùng đã đồng ý)

```bash
git add nutrition-service
git commit -m "feat(nutrition): scaffold nutrition-service with JWT resource server

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 9: nutrition-service: tạo, xem lịch sử và xem chỉ số mới nhất của health record

**Files:**
- Modify: `nutrition-service/src/main/java/com/veggiepal/nutrition/exception/ErrorCode.java`
- Create (trong `nutrition-service/src/main/java/com/veggiepal/nutrition/`):
  - `entity/HealthRecord.java`
  - `repository/HealthRecordRepository.java`
  - `dto/request/HealthRecordRequest.java`
  - `dto/response/HealthRecordResponse.java`, `dto/response/PageResponse.java`
  - `mapper/HealthRecordMapper.java`
  - `service/HealthRecordService.java`
  - `controller/HealthRecordController.java`
- Test: `nutrition-service/src/test/java/com/veggiepal/nutrition/service/HealthRecordServiceTest.java`
- Test: `nutrition-service/src/test/java/com/veggiepal/nutrition/controller/HealthRecordControllerTest.java`

**Interfaces:**
- Consumes: các class ở Task 8.
- Produces:
  - `HealthRecordService.calculateBmi(BigDecimal heightCm, BigDecimal weightKg): BigDecimal` (static, package-private)
  - `HealthRecordService.createRecord(Long userId, HealthRecordRequest request): HealthRecordResponse`
  - `HealthRecordService.getRecords(Long userId, int page, int size): PageResponse<HealthRecordResponse>`
  - `HealthRecordService.getLatestRecord(Long userId): HealthRecordResponse`
  - `HealthRecordRequest(BigDecimal heightCm, BigDecimal weightKg)`
  - `HealthRecordServiceTest.record(Long id): HealthRecord` (static, dùng lại ở Task 10)
  - `HealthRecordControllerTest.currentUser()` (dùng lại ở Task 10)

- [ ] **Step 1: Thêm mã lỗi**

Trong `ErrorCode.java` của nutrition, chèn ngay **trước** dòng `INVALID_REQUEST(1018, ...)`:

```java
    HEIGHT_REQUIRED(2001, "Height is required", HttpStatus.BAD_REQUEST),

    INVALID_HEIGHT(2002, "Height must be between 50 and 250 cm with at most 1 decimal", HttpStatus.BAD_REQUEST),

    WEIGHT_REQUIRED(2003, "Weight is required", HttpStatus.BAD_REQUEST),

    INVALID_WEIGHT(2004, "Weight must be between 20 and 300 kg with at most 1 decimal", HttpStatus.BAD_REQUEST),

    HEALTH_RECORD_NOT_EXISTED(2005, "Health record not existed", HttpStatus.NOT_FOUND),

```

- [ ] **Step 2: Tạo entity, repository, DTO, mapper**

`entity/HealthRecord.java`:

```java
package com.veggiepal.nutrition.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(
        name = "health_records",
        indexes = @Index(name = "idx_health_records_user_recorded", columnList = "user_id, recorded_at")
)
public class HealthRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "user_id", nullable = false)
    Long userId;

    @Column(name = "height_cm", nullable = false, precision = 4, scale = 1)
    BigDecimal heightCm;

    @Column(name = "weight_kg", nullable = false, precision = 4, scale = 1)
    BigDecimal weightKg;

    // precision 5: the extreme 300 kg / 0.5 m² gives 1200.0
    @Column(nullable = false, precision = 5, scale = 1)
    BigDecimal bmi;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    LocalDateTime recordedAt;

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

`repository/HealthRecordRepository.java`:

```java
package com.veggiepal.nutrition.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.veggiepal.nutrition.entity.HealthRecord;

@Repository
public interface HealthRecordRepository extends JpaRepository<HealthRecord, Long> {

    Page<HealthRecord> findByUserId(Long userId, Pageable pageable);

    Optional<HealthRecord> findFirstByUserIdOrderByRecordedAtDescIdDesc(Long userId);
}
```

`dto/request/HealthRecordRequest.java`:

```java
package com.veggiepal.nutrition.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HealthRecordRequest {

    @NotNull(message = "HEIGHT_REQUIRED")
    @DecimalMin(value = "50", message = "INVALID_HEIGHT")
    @DecimalMax(value = "250", message = "INVALID_HEIGHT")
    @Digits(integer = 3, fraction = 1, message = "INVALID_HEIGHT")
    BigDecimal heightCm;

    @NotNull(message = "WEIGHT_REQUIRED")
    @DecimalMin(value = "20", message = "INVALID_WEIGHT")
    @DecimalMax(value = "300", message = "INVALID_WEIGHT")
    @Digits(integer = 3, fraction = 1, message = "INVALID_WEIGHT")
    BigDecimal weightKg;
}
```

`dto/response/HealthRecordResponse.java`:

```java
package com.veggiepal.nutrition.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HealthRecordResponse {
    Long id;

    BigDecimal heightCm;

    BigDecimal weightKg;

    BigDecimal bmi;

    LocalDateTime recordedAt;
}
```

`dto/response/PageResponse.java`:

```java
package com.veggiepal.nutrition.dto.response;

import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PageResponse<T> {
    List<T> items;

    int page;

    int size;

    long totalElements;

    int totalPages;
}
```

`mapper/HealthRecordMapper.java`:

```java
package com.veggiepal.nutrition.mapper;

import org.mapstruct.Mapper;

import com.veggiepal.nutrition.dto.response.HealthRecordResponse;
import com.veggiepal.nutrition.entity.HealthRecord;

@Mapper(componentModel = "spring")
public interface HealthRecordMapper {

    HealthRecordResponse toHealthRecordResponse(HealthRecord healthRecord);
}
```

- [ ] **Step 3: Viết test fail cho service**

`nutrition-service/src/test/java/com/veggiepal/nutrition/service/HealthRecordServiceTest.java`:

```java
package com.veggiepal.nutrition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.veggiepal.nutrition.dto.request.HealthRecordRequest;
import com.veggiepal.nutrition.dto.response.HealthRecordResponse;
import com.veggiepal.nutrition.dto.response.PageResponse;
import com.veggiepal.nutrition.entity.HealthRecord;
import com.veggiepal.nutrition.exception.AppException;
import com.veggiepal.nutrition.exception.ErrorCode;
import com.veggiepal.nutrition.mapper.HealthRecordMapper;
import com.veggiepal.nutrition.repository.HealthRecordRepository;

@ExtendWith(MockitoExtension.class)
class HealthRecordServiceTest {

    static final Long USER_ID = 7L;

    @Mock
    HealthRecordRepository healthRecordRepository;

    @Spy
    HealthRecordMapper healthRecordMapper = Mappers.getMapper(HealthRecordMapper.class);

    @InjectMocks
    HealthRecordService healthRecordService;

    @Test
    void calculateBmi_roundsHalfUpToOneDecimal() {
        assertThat(HealthRecordService.calculateBmi(new BigDecimal("170"), new BigDecimal("65")))
                .isEqualByComparingTo("22.5");
        // 89.8 / 2.0² = 22.45 exactly: HALF_UP gives 22.5 (HALF_EVEN would give 22.4)
        assertThat(HealthRecordService.calculateBmi(new BigDecimal("200"), new BigDecimal("89.8")))
                .isEqualByComparingTo("22.5");
        assertThat(HealthRecordService.calculateBmi(new BigDecimal("50"), new BigDecimal("300")))
                .isEqualByComparingTo("1200.0");
    }

    @Test
    void createRecord_savesRecordForUserWithBmiAndRecordedAt() {
        HealthRecordResponse response = healthRecordService.createRecord(
                USER_ID, new HealthRecordRequest(new BigDecimal("170"), new BigDecimal("65")));

        ArgumentCaptor<HealthRecord> captor = ArgumentCaptor.forClass(HealthRecord.class);
        verify(healthRecordRepository).save(captor.capture());
        HealthRecord saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getBmi()).isEqualByComparingTo("22.5");
        assertThat(saved.getRecordedAt()).isNotNull();
        assertThat(response.getBmi()).isEqualByComparingTo("22.5");
    }

    @Test
    void getLatestRecord_returnsNewestRecord() {
        when(healthRecordRepository.findFirstByUserIdOrderByRecordedAtDescIdDesc(USER_ID))
                .thenReturn(Optional.of(record(3L)));

        assertThat(healthRecordService.getLatestRecord(USER_ID).getId()).isEqualTo(3L);
    }

    @Test
    void getLatestRecord_noRecord_throwsHealthRecordNotExisted() {
        when(healthRecordRepository.findFirstByUserIdOrderByRecordedAtDescIdDesc(USER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> healthRecordService.getLatestRecord(USER_ID))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.HEALTH_RECORD_NOT_EXISTED));
    }

    @Test
    void getRecords_clampsPagingAndSortsNewestFirst() {
        when(healthRecordRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(List.of(record(3L)), invocation.getArgument(1), 1));

        PageResponse<HealthRecordResponse> page = healthRecordService.getRecords(USER_ID, -1, 500);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(healthRecordRepository).findByUserId(eq(USER_ID), captor.capture());
        Pageable pageable = captor.getValue();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(100);
        assertThat(pageable.getSort())
                .isEqualTo(Sort.by(Sort.Order.desc("recordedAt"), Sort.Order.desc("id")));
        assertThat(page.getItems()).extracting(HealthRecordResponse::getId).containsExactly(3L);
        assertThat(page.getPage()).isZero();
        assertThat(page.getSize()).isEqualTo(100);
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getTotalPages()).isEqualTo(1);
    }

    @Test
    void getRecords_sizeBelowOne_usesOne() {
        when(healthRecordRepository.findByUserId(eq(USER_ID), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(List.of(), invocation.getArgument(1), 0));

        healthRecordService.getRecords(USER_ID, 2, 0);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(healthRecordRepository).findByUserId(eq(USER_ID), captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(captor.getValue().getPageSize()).isEqualTo(1);
    }

    static HealthRecord record(Long id) {
        return HealthRecord.builder()
                .id(id)
                .userId(USER_ID)
                .heightCm(new BigDecimal("170.0"))
                .weightKg(new BigDecimal("65.0"))
                .bmi(new BigDecimal("22.5"))
                .recordedAt(LocalDateTime.of(2026, 9, 1, 8, 0))
                .build();
    }
}
```

- [ ] **Step 4: Viết test fail cho controller**

`nutrition-service/src/test/java/com/veggiepal/nutrition/controller/HealthRecordControllerTest.java`:

```java
package com.veggiepal.nutrition.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.veggiepal.nutrition.configuration.JwtConfig;
import com.veggiepal.nutrition.configuration.SecurityConfig;
import com.veggiepal.nutrition.configuration.SecurityExceptionHandler;
import com.veggiepal.nutrition.dto.request.HealthRecordRequest;
import com.veggiepal.nutrition.dto.response.HealthRecordResponse;
import com.veggiepal.nutrition.exception.AppException;
import com.veggiepal.nutrition.exception.ErrorCode;
import com.veggiepal.nutrition.service.HealthRecordService;

@WebMvcTest(HealthRecordController.class)
@Import({SecurityConfig.class, JwtConfig.class, SecurityExceptionHandler.class})
class HealthRecordControllerTest {

    static final Long USER_ID = 7L;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    HealthRecordService healthRecordService;

    static RequestPostProcessor currentUser() {
        return jwt().jwt(token -> token.claim("userId", USER_ID).claim("role", "USER"));
    }

    @Test
    void createRecord_valid_passesRequestForCurrentUser() throws Exception {
        when(healthRecordService.createRecord(eq(USER_ID), any(HealthRecordRequest.class)))
                .thenReturn(HealthRecordResponse.builder().id(1L).bmi(new BigDecimal("22.5")).build());

        mockMvc.perform(post("/nutrition/me/health-records").with(currentUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"heightCm": 170, "weightKg": 65}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.bmi").value(22.5));

        verify(healthRecordService).createRecord(eq(USER_ID), any(HealthRecordRequest.class));
    }

    @Test
    void createRecord_heightTooSmall_returnsInvalidHeight() throws Exception {
        mockMvc.perform(post("/nutrition/me/health-records").with(currentUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"heightCm": 10, "weightKg": 65}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(2002));
    }

    @Test
    void createRecord_heightWithTwoDecimals_returnsInvalidHeight() throws Exception {
        mockMvc.perform(post("/nutrition/me/health-records").with(currentUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"heightCm": 170.55, "weightKg": 65}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(2002));
    }

    @Test
    void createRecord_missingWeight_returnsWeightRequired() throws Exception {
        mockMvc.perform(post("/nutrition/me/health-records").with(currentUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"heightCm": 170}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(2003));
    }

    @Test
    void getRecords_defaultPaging() throws Exception {
        mockMvc.perform(get("/nutrition/me/health-records").with(currentUser()))
                .andExpect(status().isOk());

        verify(healthRecordService).getRecords(USER_ID, 0, 20);
    }

    @Test
    void getRecords_nonNumericPage_returnsInvalidRequest() throws Exception {
        mockMvc.perform(get("/nutrition/me/health-records").param("page", "abc").with(currentUser()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1018));
    }

    @Test
    void getLatestRecord_returnsRecord() throws Exception {
        when(healthRecordService.getLatestRecord(USER_ID))
                .thenReturn(HealthRecordResponse.builder().id(3L).bmi(new BigDecimal("22.5")).build());

        mockMvc.perform(get("/nutrition/me/health-records/latest").with(currentUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(3));
    }

    @Test
    void getLatestRecord_none_returns404() throws Exception {
        when(healthRecordService.getLatestRecord(USER_ID))
                .thenThrow(new AppException(ErrorCode.HEALTH_RECORD_NOT_EXISTED));

        mockMvc.perform(get("/nutrition/me/health-records/latest").with(currentUser()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(2005));
    }
}
```

- [ ] **Step 5: Chạy test, xác nhận fail**

Run: `./mvnw test -Dtest='HealthRecordServiceTest,HealthRecordControllerTest'`
Expected: FAIL khi biên dịch, lỗi `cannot find symbol: class HealthRecordService` / `HealthRecordController`.

- [ ] **Step 6: Cài đặt service**

`nutrition-service/src/main/java/com/veggiepal/nutrition/service/HealthRecordService.java`:

```java
package com.veggiepal.nutrition.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.veggiepal.nutrition.dto.request.HealthRecordRequest;
import com.veggiepal.nutrition.dto.response.HealthRecordResponse;
import com.veggiepal.nutrition.dto.response.PageResponse;
import com.veggiepal.nutrition.entity.HealthRecord;
import com.veggiepal.nutrition.exception.AppException;
import com.veggiepal.nutrition.exception.ErrorCode;
import com.veggiepal.nutrition.mapper.HealthRecordMapper;
import com.veggiepal.nutrition.repository.HealthRecordRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HealthRecordService {

    private static final int MAX_PAGE_SIZE = 100;

    private static final Sort NEWEST_FIRST =
            Sort.by(Sort.Order.desc("recordedAt"), Sort.Order.desc("id"));

    HealthRecordRepository healthRecordRepository;
    HealthRecordMapper healthRecordMapper;

    public HealthRecordResponse createRecord(Long userId, HealthRecordRequest request) {

        HealthRecord healthRecord = HealthRecord.builder()
                .userId(userId)
                .heightCm(request.getHeightCm())
                .weightKg(request.getWeightKg())
                .bmi(calculateBmi(request.getHeightCm(), request.getWeightKg()))
                .recordedAt(LocalDateTime.now())
                .build();

        healthRecordRepository.save(healthRecord);
        return healthRecordMapper.toHealthRecordResponse(healthRecord);
    }

    public PageResponse<HealthRecordResponse> getRecords(Long userId, int page, int size) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        Page<HealthRecord> records = healthRecordRepository.findByUserId(
                userId,
                PageRequest.of(safePage, safeSize, NEWEST_FIRST)
        );

        return PageResponse.<HealthRecordResponse>builder()
                .items(records.getContent().stream()
                        .map(healthRecordMapper::toHealthRecordResponse)
                        .toList())
                .page(records.getNumber())
                .size(records.getSize())
                .totalElements(records.getTotalElements())
                .totalPages(records.getTotalPages())
                .build();
    }

    public HealthRecordResponse getLatestRecord(Long userId) {

        return healthRecordRepository
                .findFirstByUserIdOrderByRecordedAtDescIdDesc(userId)
                .map(healthRecordMapper::toHealthRecordResponse)
                .orElseThrow(
                        () -> new AppException(
                                ErrorCode.HEALTH_RECORD_NOT_EXISTED
                        )
                );
    }

    // BR-04: BMI = weight(kg) / height(m)^2, rounded HALF_UP to 1 decimal
    static BigDecimal calculateBmi(BigDecimal heightCm, BigDecimal weightKg) {

        BigDecimal heightM = heightCm.movePointLeft(2);

        return weightKg.divide(heightM.multiply(heightM), 1, RoundingMode.HALF_UP);
    }
}
```

- [ ] **Step 7: Cài đặt controller**

`nutrition-service/src/main/java/com/veggiepal/nutrition/controller/HealthRecordController.java`:

```java
package com.veggiepal.nutrition.controller;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import com.veggiepal.nutrition.dto.request.HealthRecordRequest;
import com.veggiepal.nutrition.dto.response.ApiResponse;
import com.veggiepal.nutrition.dto.response.HealthRecordResponse;
import com.veggiepal.nutrition.dto.response.PageResponse;
import com.veggiepal.nutrition.service.HealthRecordService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/nutrition/me/health-records")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Health Record", description = "Height, weight and BMI history of the current user")
public class HealthRecordController {

    HealthRecordService healthRecordService;

    @Operation(
            summary = "Record height and weight; BMI is calculated by the server"
    )
    @PostMapping
    ApiResponse<HealthRecordResponse> createRecord(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid HealthRecordRequest request
    ) {

        return ApiResponse
                .<HealthRecordResponse>builder()
                .result(healthRecordService.createRecord(CurrentUser.id(jwt), request))
                .build();
    }

    @Operation(
            summary = "Health record history, newest first"
    )
    @GetMapping
    ApiResponse<PageResponse<HealthRecordResponse>> getRecords(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {

        return ApiResponse
                .<PageResponse<HealthRecordResponse>>builder()
                .result(healthRecordService.getRecords(CurrentUser.id(jwt), page, size))
                .build();
    }

    @Operation(
            summary = "Latest health record (current height, weight and BMI)"
    )
    @GetMapping("/latest")
    ApiResponse<HealthRecordResponse> getLatestRecord(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {

        return ApiResponse
                .<HealthRecordResponse>builder()
                .result(healthRecordService.getLatestRecord(CurrentUser.id(jwt)))
                .build();
    }
}
```

- [ ] **Step 8: Chạy test, xác nhận pass**

Run: `./mvnw test -Dtest='HealthRecordServiceTest,HealthRecordControllerTest'`
Expected: `Tests run: 14, Failures: 0, Errors: 0`.

- [ ] **Step 9: Commit** (chỉ khi người dùng đã đồng ý)

```bash
git add nutrition-service/src
git commit -m "feat(nutrition): record health data with server-side BMI and history

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 10: nutrition-service: sửa health record (`PUT /nutrition/me/health-records/{id}`)

**Files:**
- Modify: `nutrition-service/src/main/java/com/veggiepal/nutrition/repository/HealthRecordRepository.java`
- Modify: `nutrition-service/src/main/java/com/veggiepal/nutrition/service/HealthRecordService.java`
- Modify: `nutrition-service/src/main/java/com/veggiepal/nutrition/controller/HealthRecordController.java`
- Test: `nutrition-service/src/test/java/com/veggiepal/nutrition/service/HealthRecordServiceTest.java`
- Test: `nutrition-service/src/test/java/com/veggiepal/nutrition/controller/HealthRecordControllerTest.java`

**Interfaces:**
- Consumes: `HealthRecordServiceTest.record(Long)`, `HealthRecordControllerTest.currentUser()` (Task 9).
- Produces: `HealthRecordRepository.findByIdAndUserId(Long id, Long userId): Optional<HealthRecord>`; `HealthRecordService.updateRecord(Long userId, Long recordId, HealthRecordRequest request): HealthRecordResponse`.

- [ ] **Step 1: Viết test fail cho service**

Thêm vào `HealthRecordServiceTest.java`:

```java
    @Test
    void updateRecord_keepsRecordedAtAndRecalculatesBmi() {
        HealthRecord existing = record(5L);
        when(healthRecordRepository.findByIdAndUserId(5L, USER_ID)).thenReturn(Optional.of(existing));

        HealthRecordResponse response = healthRecordService.updateRecord(
                USER_ID, 5L, new HealthRecordRequest(new BigDecimal("200"), new BigDecimal("89.8")));

        assertThat(existing.getHeightCm()).isEqualByComparingTo("200");
        assertThat(existing.getWeightKg()).isEqualByComparingTo("89.8");
        assertThat(existing.getBmi()).isEqualByComparingTo("22.5");
        assertThat(existing.getRecordedAt()).isEqualTo(LocalDateTime.of(2026, 9, 1, 8, 0));
        assertThat(response.getRecordedAt()).isEqualTo(LocalDateTime.of(2026, 9, 1, 8, 0));
        verify(healthRecordRepository).save(existing);
    }

    @Test
    void updateRecord_notOwnedOrMissing_throwsHealthRecordNotExisted() {
        when(healthRecordRepository.findByIdAndUserId(5L, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> healthRecordService.updateRecord(
                USER_ID, 5L, new HealthRecordRequest(new BigDecimal("170"), new BigDecimal("65"))))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.HEALTH_RECORD_NOT_EXISTED));
    }
```

- [ ] **Step 2: Viết test fail cho controller**

Trong `HealthRecordControllerTest.java`, thêm import `static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put` và các test:

```java
    @Test
    void updateRecord_valid_passesIdAndCurrentUser() throws Exception {
        when(healthRecordService.updateRecord(eq(USER_ID), eq(5L), any(HealthRecordRequest.class)))
                .thenReturn(HealthRecordResponse.builder().id(5L).bmi(new BigDecimal("24.2")).build());

        mockMvc.perform(put("/nutrition/me/health-records/5").with(currentUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"heightCm": 170, "weightKg": 70}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.id").value(5));

        verify(healthRecordService).updateRecord(eq(USER_ID), eq(5L), any(HealthRecordRequest.class));
    }

    @Test
    void updateRecord_nonNumericId_returnsInvalidRequest() throws Exception {
        mockMvc.perform(put("/nutrition/me/health-records/abc").with(currentUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"heightCm": 170, "weightKg": 70}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1018));
    }

    @Test
    void updateRecord_invalidBody_returnsValidationCode() throws Exception {
        mockMvc.perform(put("/nutrition/me/health-records/5").with(currentUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"heightCm": 300, "weightKg": 70}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(2002));
    }
```

- [ ] **Step 3: Chạy test, xác nhận fail**

Run: `./mvnw test -Dtest='HealthRecordServiceTest,HealthRecordControllerTest'`
Expected: FAIL khi biên dịch, lỗi `cannot find symbol: method findByIdAndUserId` / `updateRecord`.

- [ ] **Step 4: Cài đặt**

Trong `HealthRecordRepository.java`, thêm:

```java
    Optional<HealthRecord> findByIdAndUserId(Long id, Long userId);
```

Trong `HealthRecordService.java`, thêm method ngay sau `getLatestRecord`:

```java
    public HealthRecordResponse updateRecord(Long userId, Long recordId, HealthRecordRequest request) {

        // Filtering by userId too: someone else's record looks exactly like a missing one
        HealthRecord healthRecord = healthRecordRepository
                .findByIdAndUserId(recordId, userId)
                .orElseThrow(
                        () -> new AppException(
                                ErrorCode.HEALTH_RECORD_NOT_EXISTED
                        )
                );

        healthRecord.setHeightCm(request.getHeightCm());
        healthRecord.setWeightKg(request.getWeightKg());
        healthRecord.setBmi(calculateBmi(request.getHeightCm(), request.getWeightKg()));

        healthRecordRepository.save(healthRecord);
        return healthRecordMapper.toHealthRecordResponse(healthRecord);
    }
```

Trong `HealthRecordController.java`, thêm method:

```java
    @Operation(
            summary = "Correct a health record; recordedAt is kept and BMI is recalculated"
    )
    @PutMapping("/{id}")
    ApiResponse<HealthRecordResponse> updateRecord(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @PathVariable("id") Long id,
            @RequestBody @Valid HealthRecordRequest request
    ) {

        return ApiResponse
                .<HealthRecordResponse>builder()
                .result(healthRecordService.updateRecord(CurrentUser.id(jwt), id, request))
                .build();
    }
```

- [ ] **Step 5: Chạy test, xác nhận pass**

Run: `./mvnw test -Dtest='HealthRecordServiceTest,HealthRecordControllerTest'`
Expected: `Tests run: 19, Failures: 0, Errors: 0`.

- [ ] **Step 6: Commit** (chỉ khi người dùng đã đồng ý)

```bash
git add nutrition-service/src
git commit -m "feat(nutrition): allow correcting own health records

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 11: nutrition-service: danh mục allergens + dị ứng của user

**Files:**
- Modify: `nutrition-service/src/main/resources/application.properties`
- Create: `nutrition-service/src/main/resources/data.sql`
- Modify: `nutrition-service/src/main/java/com/veggiepal/nutrition/exception/ErrorCode.java`
- Create (trong `nutrition-service/src/main/java/com/veggiepal/nutrition/`):
  - `enums/AllergenCategory.java`
  - `entity/Allergen.java`, `entity/UserAllergy.java`
  - `repository/AllergenRepository.java`, `repository/UserAllergyRepository.java`
  - `dto/request/UpdateAllergiesRequest.java`, `dto/response/AllergenResponse.java`
  - `mapper/AllergenMapper.java`
  - `service/AllergyService.java`
  - `controller/AllergyController.java`
- Test: `nutrition-service/src/test/java/com/veggiepal/nutrition/service/AllergyServiceTest.java`
- Test: `nutrition-service/src/test/java/com/veggiepal/nutrition/controller/AllergyControllerTest.java`

**Interfaces:**
- Produces:
  - `AllergyService.getAllAllergens(): List<AllergenResponse>`
  - `AllergyService.getMyAllergies(Long userId): List<AllergenResponse>`
  - `AllergyService.replaceAllergies(Long userId, UpdateAllergiesRequest request): List<AllergenResponse>`
  - `UpdateAllergiesRequest(List<Long> allergenIds)`
- Thứ tự sắp xếp: theo thứ tự khai báo của `AllergenCategory`, rồi theo `name` bằng `Collator` tiếng Việt, **sắp xếp trong Java** (spec §5.4).

- [ ] **Step 1: Bật dữ liệu seed**

Thêm vào cuối `nutrition-service/src/main/resources/application.properties`:

```properties

# Run data.sql after Hibernate has created the tables
spring.jpa.defer-datasource-initialization=true
spring.sql.init.mode=always
spring.sql.init.encoding=UTF-8
```

`nutrition-service/src/main/resources/data.sql`:

```sql
-- Allergen catalog. INSERT IGNORE keeps re-runs idempotent (code is unique).
-- Animal-derived groups (milk, egg, seafood) are omitted: the platform is fully vegan (BR-06).
INSERT IGNORE INTO allergens (code, name, category) VALUES
('GLUTEN', 'Gluten (lúa mì, lúa mạch)', 'GRAIN'),
('BUCKWHEAT', 'Kiều mạch', 'GRAIN'),
('CORN', 'Bắp (ngô)', 'GRAIN'),
('PEANUT', 'Đậu phộng', 'LEGUME'),
('SOY', 'Đậu nành', 'LEGUME'),
('MUNG_BEAN', 'Đậu xanh', 'LEGUME'),
('CASHEW', 'Hạt điều', 'NUT_SEED'),
('TREE_NUT', 'Hạt cây khác (hạnh nhân, óc chó, mắc ca)', 'NUT_SEED'),
('SESAME', 'Mè (vừng)', 'NUT_SEED'),
('COCONUT', 'Dừa', 'NUT_SEED'),
('TOMATO', 'Cà chua', 'VEGETABLE'),
('EGGPLANT', 'Cà tím', 'VEGETABLE'),
('TARO', 'Khoai môn', 'VEGETABLE'),
('CASSAVA', 'Khoai mì (sắn)', 'VEGETABLE'),
('BAMBOO_SHOOT', 'Măng', 'VEGETABLE'),
('CELERY', 'Cần tây', 'VEGETABLE'),
('CORIANDER', 'Rau mùi (ngò)', 'VEGETABLE'),
('GARLIC', 'Tỏi', 'VEGETABLE'),
('ONION', 'Hành', 'VEGETABLE'),
('MANGO', 'Xoài', 'FRUIT'),
('PINEAPPLE', 'Dứa (thơm)', 'FRUIT'),
('PAPAYA', 'Đu đủ', 'FRUIT'),
('AVOCADO', 'Bơ', 'FRUIT'),
('DURIAN', 'Sầu riêng', 'FRUIT'),
('JACKFRUIT', 'Mít', 'FRUIT'),
('LYCHEE', 'Vải', 'FRUIT'),
('STRAWBERRY', 'Dâu tây', 'FRUIT'),
('MUSHROOM', 'Nấm (các loại)', 'MUSHROOM'),
('CHILI', 'Ớt', 'SPICE'),
('MUSTARD', 'Mù tạt', 'SPICE'),
('SULPHITE', 'Sulfite', 'ADDITIVE'),
('MSG', 'Bột ngọt (MSG)', 'ADDITIVE');
```

- [ ] **Step 2: Thêm mã lỗi**

Trong `ErrorCode.java` của nutrition, chèn ngay **trước** dòng `INVALID_REQUEST(1018, ...)`:

```java
    ALLERGEN_IDS_REQUIRED(2006, "Allergen list is required", HttpStatus.BAD_REQUEST),

    ALLERGEN_NOT_EXISTED(2007, "Allergen not existed", HttpStatus.BAD_REQUEST),

```

- [ ] **Step 3: Tạo enum, entity, repository, DTO, mapper**

`enums/AllergenCategory.java`:

```java
package com.veggiepal.nutrition.enums;

// Declaration order is the display order of the catalog
public enum AllergenCategory {
    GRAIN,
    LEGUME,
    NUT_SEED,
    VEGETABLE,
    FRUIT,
    MUSHROOM,
    SPICE,
    ADDITIVE
}
```

`entity/Allergen.java`:

```java
package com.veggiepal.nutrition.entity;

import jakarta.persistence.*;

import com.veggiepal.nutrition.enums.AllergenCategory;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(name = "allergens")
public class Allergen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, unique = true)
    String code;

    @Column(nullable = false)
    String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    AllergenCategory category;
}
```

`entity/UserAllergy.java`:

```java
package com.veggiepal.nutrition.entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Entity
@Table(
        name = "user_allergies",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_allergies_user_allergen",
                columnNames = {"user_id", "allergen_id"}
        )
)
public class UserAllergy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "user_id", nullable = false)
    Long userId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "allergen_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    Allergen allergen;

    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = LocalDateTime.now();
    }
}
```

`repository/AllergenRepository.java`:

```java
package com.veggiepal.nutrition.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.veggiepal.nutrition.entity.Allergen;

@Repository
public interface AllergenRepository extends JpaRepository<Allergen, Long> {
}
```

`repository/UserAllergyRepository.java`:

```java
package com.veggiepal.nutrition.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.veggiepal.nutrition.entity.Allergen;
import com.veggiepal.nutrition.entity.UserAllergy;

@Repository
public interface UserAllergyRepository extends JpaRepository<UserAllergy, Long> {

    List<UserAllergy> findByUserId(Long userId);

    @Query("select ua.allergen from UserAllergy ua where ua.userId = :userId")
    List<Allergen> findAllergensByUserId(@Param("userId") Long userId);
}
```

`dto/request/UpdateAllergiesRequest.java`:

```java
package com.veggiepal.nutrition.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotNull;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UpdateAllergiesRequest {

    // [] clears all allergies; the list replaces the current one
    @NotNull(message = "ALLERGEN_IDS_REQUIRED")
    List<@NotNull(message = "ALLERGEN_NOT_EXISTED") Long> allergenIds;
}
```

`dto/response/AllergenResponse.java`:

```java
package com.veggiepal.nutrition.dto.response;

import com.veggiepal.nutrition.enums.AllergenCategory;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AllergenResponse {
    Long id;

    String code;

    String name;

    AllergenCategory category;
}
```

`mapper/AllergenMapper.java`:

```java
package com.veggiepal.nutrition.mapper;

import org.mapstruct.Mapper;

import com.veggiepal.nutrition.dto.response.AllergenResponse;
import com.veggiepal.nutrition.entity.Allergen;

@Mapper(componentModel = "spring")
public interface AllergenMapper {

    AllergenResponse toAllergenResponse(Allergen allergen);
}
```

- [ ] **Step 4: Viết test fail cho service**

`nutrition-service/src/test/java/com/veggiepal/nutrition/service/AllergyServiceTest.java`:

```java
package com.veggiepal.nutrition.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.veggiepal.nutrition.dto.request.UpdateAllergiesRequest;
import com.veggiepal.nutrition.dto.response.AllergenResponse;
import com.veggiepal.nutrition.entity.Allergen;
import com.veggiepal.nutrition.entity.UserAllergy;
import com.veggiepal.nutrition.enums.AllergenCategory;
import com.veggiepal.nutrition.exception.AppException;
import com.veggiepal.nutrition.exception.ErrorCode;
import com.veggiepal.nutrition.mapper.AllergenMapper;
import com.veggiepal.nutrition.repository.AllergenRepository;
import com.veggiepal.nutrition.repository.UserAllergyRepository;

@ExtendWith(MockitoExtension.class)
class AllergyServiceTest {

    static final Long USER_ID = 7L;

    static final Allergen GLUTEN = allergen(1L, "GLUTEN", "Gluten (lúa mì, lúa mạch)", AllergenCategory.GRAIN);

    static final Allergen PEANUT = allergen(4L, "PEANUT", "Đậu phộng", AllergenCategory.LEGUME);

    static final Allergen SOY = allergen(5L, "SOY", "Đậu nành", AllergenCategory.LEGUME);

    static final Allergen MANGO = allergen(20L, "MANGO", "Xoài", AllergenCategory.FRUIT);

    @Mock
    AllergenRepository allergenRepository;

    @Mock
    UserAllergyRepository userAllergyRepository;

    @Spy
    AllergenMapper allergenMapper = Mappers.getMapper(AllergenMapper.class);

    @InjectMocks
    AllergyService allergyService;

    @Captor
    ArgumentCaptor<Iterable<UserAllergy>> userAllergiesCaptor;

    @Test
    void getAllAllergens_sortedByCategoryThenVietnameseName() {
        when(allergenRepository.findAll()).thenReturn(List.of(MANGO, PEANUT, SOY, GLUTEN));

        List<AllergenResponse> allergens = allergyService.getAllAllergens();

        assertThat(allergens).extracting(AllergenResponse::getCode)
                .containsExactly("GLUTEN", "SOY", "PEANUT", "MANGO");
    }

    @Test
    void getMyAllergies_returnsSortedAllergensOfUser() {
        when(userAllergyRepository.findAllergensByUserId(USER_ID)).thenReturn(List.of(MANGO, GLUTEN));

        assertThat(allergyService.getMyAllergies(USER_ID)).extracting(AllergenResponse::getCode)
                .containsExactly("GLUTEN", "MANGO");
    }

    @Test
    void replaceAllergies_removesAndAddsOnlyTheDifference() {
        UserAllergy peanutAllergy = userAllergy(11L, PEANUT);
        UserAllergy soyAllergy = userAllergy(12L, SOY);
        when(allergenRepository.findAllById(Set.of(5L, 20L))).thenReturn(List.of(SOY, MANGO));
        when(userAllergyRepository.findByUserId(USER_ID)).thenReturn(List.of(peanutAllergy, soyAllergy));

        List<AllergenResponse> result = allergyService.replaceAllergies(
                USER_ID, new UpdateAllergiesRequest(List.of(5L, 20L, 20L)));

        verify(userAllergyRepository).deleteAll(List.of(peanutAllergy));
        verify(userAllergyRepository).saveAll(userAllergiesCaptor.capture());
        assertThat(userAllergiesCaptor.getValue()).singleElement().satisfies(added -> {
            assertThat(added.getUserId()).isEqualTo(USER_ID);
            assertThat(added.getAllergen()).isSameAs(MANGO);
        });
        assertThat(result).extracting(AllergenResponse::getCode).containsExactly("SOY", "MANGO");
    }

    @Test
    void replaceAllergies_emptyList_removesEverything() {
        UserAllergy peanutAllergy = userAllergy(11L, PEANUT);
        when(allergenRepository.findAllById(Set.of())).thenReturn(List.of());
        when(userAllergyRepository.findByUserId(USER_ID)).thenReturn(List.of(peanutAllergy));

        List<AllergenResponse> result = allergyService.replaceAllergies(
                USER_ID, new UpdateAllergiesRequest(List.of()));

        verify(userAllergyRepository).deleteAll(List.of(peanutAllergy));
        verify(userAllergyRepository).saveAll(userAllergiesCaptor.capture());
        assertThat(userAllergiesCaptor.getValue()).isEmpty();
        assertThat(result).isEmpty();
    }

    @Test
    void replaceAllergies_unknownId_throwsAndChangesNothing() {
        when(allergenRepository.findAllById(Set.of(4L, 99L))).thenReturn(List.of(PEANUT));

        assertThatThrownBy(() -> allergyService.replaceAllergies(
                USER_ID, new UpdateAllergiesRequest(List.of(4L, 99L))))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ALLERGEN_NOT_EXISTED));
        verifyNoInteractions(userAllergyRepository);
    }

    static Allergen allergen(Long id, String code, String name, AllergenCategory category) {
        return Allergen.builder().id(id).code(code).name(name).category(category).build();
    }

    // Distinct ids matter: UserAllergy.equals ignores the allergen, so two id-less rows would be equal
    static UserAllergy userAllergy(Long id, Allergen allergen) {
        return UserAllergy.builder().id(id).userId(USER_ID).allergen(allergen).build();
    }
}
```

- [ ] **Step 5: Viết test fail cho controller**

`nutrition-service/src/test/java/com/veggiepal/nutrition/controller/AllergyControllerTest.java`:

```java
package com.veggiepal.nutrition.controller;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

import com.veggiepal.nutrition.configuration.JwtConfig;
import com.veggiepal.nutrition.configuration.SecurityConfig;
import com.veggiepal.nutrition.configuration.SecurityExceptionHandler;
import com.veggiepal.nutrition.dto.response.AllergenResponse;
import com.veggiepal.nutrition.enums.AllergenCategory;
import com.veggiepal.nutrition.service.AllergyService;

@WebMvcTest(AllergyController.class)
@Import({SecurityConfig.class, JwtConfig.class, SecurityExceptionHandler.class})
class AllergyControllerTest {

    static final Long USER_ID = 7L;

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    AllergyService allergyService;

    static RequestPostProcessor currentUser() {
        return jwt().jwt(token -> token.claim("userId", USER_ID).claim("role", "USER"));
    }

    @Test
    void getAllergens_returnsCatalog() throws Exception {
        when(allergyService.getAllAllergens()).thenReturn(List.of(
                AllergenResponse.builder().id(1L).code("GLUTEN").name("Gluten").category(AllergenCategory.GRAIN).build()));

        mockMvc.perform(get("/nutrition/allergens").with(currentUser()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result[0].code").value("GLUTEN"))
                .andExpect(jsonPath("$.result[0].category").value("GRAIN"));
    }

    @Test
    void getMyAllergies_usesCurrentUser() throws Exception {
        when(allergyService.getMyAllergies(USER_ID)).thenReturn(List.of());

        mockMvc.perform(get("/nutrition/me/allergies").with(currentUser()))
                .andExpect(status().isOk());

        verify(allergyService).getMyAllergies(USER_ID);
    }

    @Test
    void replaceMyAllergies_passesIdsForCurrentUser() throws Exception {
        mockMvc.perform(put("/nutrition/me/allergies").with(currentUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"allergenIds": [1, 2]}
                                """))
                .andExpect(status().isOk());

        verify(allergyService).replaceAllergies(
                eq(USER_ID), argThat(request -> request.getAllergenIds().equals(List.of(1L, 2L))));
    }

    @Test
    void replaceMyAllergies_missingList_returnsAllergenIdsRequired() throws Exception {
        mockMvc.perform(put("/nutrition/me/allergies").with(currentUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(2006));
    }

    @Test
    void replaceMyAllergies_nullId_returnsAllergenNotExisted() throws Exception {
        mockMvc.perform(put("/nutrition/me/allergies").with(currentUser())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"allergenIds": [null]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(2007));
    }
}
```

- [ ] **Step 6: Chạy test, xác nhận fail**

Run: `./mvnw test -Dtest='AllergyServiceTest,AllergyControllerTest'`
Expected: FAIL khi biên dịch, lỗi `cannot find symbol: class AllergyService` / `AllergyController`.

- [ ] **Step 7: Cài đặt service**

`nutrition-service/src/main/java/com/veggiepal/nutrition/service/AllergyService.java`:

```java
package com.veggiepal.nutrition.service;

import java.text.Collator;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.veggiepal.nutrition.dto.request.UpdateAllergiesRequest;
import com.veggiepal.nutrition.dto.response.AllergenResponse;
import com.veggiepal.nutrition.entity.Allergen;
import com.veggiepal.nutrition.entity.UserAllergy;
import com.veggiepal.nutrition.exception.AppException;
import com.veggiepal.nutrition.exception.ErrorCode;
import com.veggiepal.nutrition.mapper.AllergenMapper;
import com.veggiepal.nutrition.repository.AllergenRepository;
import com.veggiepal.nutrition.repository.UserAllergyRepository;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AllergyService {

    // Sorted in Java: the order of a MySQL ENUM column depends on the column type
    private static final Comparator<Allergen> CATALOG_ORDER = Comparator
            .comparing(Allergen::getCategory)
            .thenComparing(Allergen::getName, Collator.getInstance(Locale.forLanguageTag("vi")));

    AllergenRepository allergenRepository;
    UserAllergyRepository userAllergyRepository;
    AllergenMapper allergenMapper;

    public List<AllergenResponse> getAllAllergens() {

        return toSortedResponses(allergenRepository.findAll());
    }

    public List<AllergenResponse> getMyAllergies(Long userId) {

        return toSortedResponses(userAllergyRepository.findAllergensByUserId(userId));
    }

    @Transactional
    public List<AllergenResponse> replaceAllergies(Long userId, UpdateAllergiesRequest request) {

        Set<Long> requestedIds = new HashSet<>(request.getAllergenIds());
        List<Allergen> requestedAllergens = allergenRepository.findAllById(requestedIds);

        if (requestedAllergens.size() != requestedIds.size()) {
            throw new AppException(ErrorCode.ALLERGEN_NOT_EXISTED);
        }

        List<UserAllergy> currentAllergies = userAllergyRepository.findByUserId(userId);
        Set<Long> currentIds = currentAllergies.stream()
                .map(userAllergy -> userAllergy.getAllergen().getId())
                .collect(Collectors.toSet());

        List<UserAllergy> removed = currentAllergies.stream()
                .filter(userAllergy -> !requestedIds.contains(userAllergy.getAllergen().getId()))
                .toList();

        List<UserAllergy> added = requestedAllergens.stream()
                .filter(allergen -> !currentIds.contains(allergen.getId()))
                .map(allergen -> UserAllergy.builder()
                        .userId(userId)
                        .allergen(allergen)
                        .build())
                .toList();

        userAllergyRepository.deleteAll(removed);
        userAllergyRepository.saveAll(added);

        return toSortedResponses(requestedAllergens);
    }

    private List<AllergenResponse> toSortedResponses(Collection<Allergen> allergens) {

        return allergens.stream()
                .sorted(CATALOG_ORDER)
                .map(allergenMapper::toAllergenResponse)
                .toList();
    }
}
```

- [ ] **Step 8: Cài đặt controller**

`nutrition-service/src/main/java/com/veggiepal/nutrition/controller/AllergyController.java`:

```java
package com.veggiepal.nutrition.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import com.veggiepal.nutrition.dto.request.UpdateAllergiesRequest;
import com.veggiepal.nutrition.dto.response.AllergenResponse;
import com.veggiepal.nutrition.dto.response.ApiResponse;
import com.veggiepal.nutrition.service.AllergyService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/nutrition")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Allergy", description = "Allergen catalog and current user allergies")
public class AllergyController {

    AllergyService allergyService;

    @Operation(
            summary = "Allergen catalog, grouped by category"
    )
    @GetMapping("/allergens")
    ApiResponse<List<AllergenResponse>> getAllergens() {

        return ApiResponse
                .<List<AllergenResponse>>builder()
                .result(allergyService.getAllAllergens())
                .build();
    }

    @Operation(
            summary = "Allergies of the current user"
    )
    @GetMapping("/me/allergies")
    ApiResponse<List<AllergenResponse>> getMyAllergies(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt
    ) {

        return ApiResponse
                .<List<AllergenResponse>>builder()
                .result(allergyService.getMyAllergies(CurrentUser.id(jwt)))
                .build();
    }

    @Operation(
            summary = "Replace the current user's allergies; [] clears them"
    )
    @PutMapping("/me/allergies")
    ApiResponse<List<AllergenResponse>> replaceMyAllergies(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid UpdateAllergiesRequest request
    ) {

        return ApiResponse
                .<List<AllergenResponse>>builder()
                .result(allergyService.replaceAllergies(CurrentUser.id(jwt), request))
                .build();
    }
}
```

- [ ] **Step 9: Chạy test, xác nhận pass**

Run: `./mvnw test -Dtest='!NutritionServiceApplicationTests'`
Expected: `BUILD SUCCESS`, toàn bộ test của nutrition-service pass. `AllergyServiceTest` có 5 test, `AllergyControllerTest` có 5.

- [ ] **Step 10: Commit** (chỉ khi người dùng đã đồng ý)

```bash
git add nutrition-service/src
git commit -m "feat(nutrition): add allergen catalog and user allergies

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 12: Gateway routes + Swagger bearer cho identity-service

**Files:**
- Modify: `api-gateway/src/main/resources/application.yaml`
- Modify: `identity-service/src/main/java/com/veggiepal/configuration/OpenApiConfig.java` (thay toàn bộ)

**Interfaces:**
- Produces: các route `/api/users/**` → 8081, `/api/nutrition/**` → 8082, `/nutrition-service/v3/api-docs/**` → 8082; mục "Nutrition Service" trong Swagger UI chung.

- [ ] **Step 1: Thêm routes vào gateway**

Trong `api-gateway/src/main/resources/application.yaml`, ngay sau khối route `identity-service` (khối có `Path=/api/auth/**`) và trước khối `identity-service-docs`, thêm:

```yaml
            - id: identity-service-users
              uri: http://localhost:8081
              predicates:
                - Path=/api/users/**
              filters:
                - StripPrefix=1

```

Ngay sau khối `identity-service-docs` (vẫn nằm trong `routes:`), thêm:

```yaml

            # =========================
            # NUTRITION SERVICE
            # =========================
            - id: nutrition-service
              uri: http://localhost:8082
              predicates:
                - Path=/api/nutrition/**
              filters:
                - StripPrefix=1

            - id: nutrition-service-docs
              uri: http://localhost:8082
              predicates:
                - Path=/nutrition-service/v3/api-docs/**
              filters:
                - StripPrefix=1
```

Trong `springdoc.swagger-ui.urls`, thêm mục sau ngay sau mục `Identity Service`:

```yaml
      - name: Nutrition Service
        url: /nutrition-service/v3/api-docs
```

- [ ] **Step 2: Kiểm tra gateway không cấu hình multipart**

Run (ở thư mục gốc repo): `grep -rn "multipart" api-gateway/src/main/resources/ || echo "no multipart config"`
Expected: `no multipart config`.

- [ ] **Step 3: Thêm bearer scheme vào Swagger của identity**

Thay toàn bộ `identity-service/src/main/java/com/veggiepal/configuration/OpenApiConfig.java`:

```java
package com.veggiepal.configuration;

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
                                .title("VeggiePal Identity Service API")
                                .version("1.0")
                                .description("Identity APIs for VeggiePal")
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

- [ ] **Step 4: Kiểm tra build**

Run: `cd api-gateway && ./mvnw test`
Expected: `ApiGatewayApplicationTests` pass (`BUILD SUCCESS`). Test này không cần DB.

Run: `cd identity-service && ./mvnw test -Dtest='!VeggiepalApplicationTests'`
Expected: `BUILD SUCCESS`.

- [ ] **Step 5: Commit** (chỉ khi người dùng đã đồng ý)

```bash
git add api-gateway/src/main/resources/application.yaml identity-service/src/main/java/com/veggiepal/configuration/OpenApiConfig.java
git commit -m "feat(gateway): route profile and nutrition APIs and aggregate their docs

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```

---

### Task 13: Kiểm tra end-to-end qua gateway (cần Docker)

**Files:** không sửa code. Script test đặt trong thư mục scratchpad của phiên, **không** đặt trong repo.

**Interfaces:**
- Produces: bằng chứng chạy thật (output lệnh), dùng làm dữ kiện cho Task 14: kiểu cột `category` và số dòng seed.

- [ ] **Step 1: Bật hạ tầng**

Run: `docker info --format '{{.ServerVersion}}'`
Expected: in ra số phiên bản. Nếu lỗi `failed to connect to the docker API`, **dừng lại và nhờ người dùng mở Docker Desktop**.

Run (ở thư mục gốc repo):

```bash
docker compose up -d
docker compose ps -a
```

Expected: `veggiepal-mysql` Up, `veggiepal-minio` Up (healthy), `veggiepal-minio-init` Exited (0).

Run: `docker logs veggiepal-minio-init`
Expected: có dòng `Bucket created successfully` (hoặc thông báo bucket đã tồn tại) và `Access permission for ... is set to download`.

- [ ] **Step 2: Tạo DB cho identity**

Run: `docker exec veggiepal-mysql mysql -uroot -p12345 -e "CREATE DATABASE IF NOT EXISTS veggiepal_identity"`
Expected: không có lỗi. Có thể có cảnh báo về việc dùng password trên dòng lệnh.

- [ ] **Step 3: Chạy toàn bộ test của cả 3 service**

Run lần lượt:
- `cd identity-service && ./mvnw test`
- `cd nutrition-service && ./mvnw test`
- `cd api-gateway && ./mvnw test`

Expected: cả 3 đều `BUILD SUCCESS`, kể cả `contextLoads`. `contextLoads` của nutrition-service chạy `data.sql`.

- [ ] **Step 4: Khởi động 3 service ở chế độ nền**

Chạy riêng từng lệnh dưới dạng background process:
- `cd identity-service && ./mvnw spring-boot:run`
- `cd nutrition-service && ./mvnw spring-boot:run`
- `cd api-gateway && ./mvnw spring-boot:run`

Đợi đến khi cả 3 lệnh sau đều trả về `200`:

```bash
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8081/v3/api-docs
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8082/v3/api-docs
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/identity-service/v3/api-docs
```

- [ ] **Step 5: Tạo script E2E**

Tạo `<scratchpad>/e2e.sh`:

```bash
#!/usr/bin/env bash
set -euo pipefail

GW=http://localhost:8080
EMAIL="e2e-$(date +%s)@example.com"
JSON='Content-Type: application/json'

field() {
  python -c '
import json, sys
value = json.load(sys.stdin)
for key in sys.argv[1].split("."):
    value = value[int(key)] if key.isdigit() else value[key]
print(value)
' "$1"
}

count() {
  python -c 'import json, sys; print(len(json.load(sys.stdin)["result"]))'
}

echo "1. register"
curl -sf -X POST "$GW/api/auth/register" -H "$JSON" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"secret123\",\"fullName\":\"E2E User\"}" | field code

echo "2. login"
TOKEN=$(curl -sf -X POST "$GW/api/auth/login" -H "$JSON" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"secret123\"}" | field result.accessToken)
AUTH="Authorization: Bearer $TOKEN"

echo "3. GET /api/users/me"
curl -sf "$GW/api/users/me" -H "$AUTH" | field result.email

echo "4. PATCH /api/users/me"
curl -sf -X PATCH "$GW/api/users/me" -H "$AUTH" -H "$JSON" \
  -d '{"fullName":"  E2E Renamed  ","phone":"0901234567","dateOfBirth":"2000-01-31"}' | field result.fullName

echo "5. POST /api/users/me/avatar (through the gateway)"
python -c "import base64; open('avatar.png','wb').write(base64.b64decode('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg=='))"
AVATAR_URL=$(curl -sf -X POST "$GW/api/users/me/avatar" -H "$AUTH" \
  -F "file=@avatar.png;type=image/png" | field result.avatarUrl)
echo "   $AVATAR_URL"
curl -s -o /dev/null -w "   avatar download HTTP %{http_code}\n" "$AVATAR_URL"

echo "6. PUT /api/users/me/password"
curl -sf -X PUT "$GW/api/users/me/password" -H "$AUTH" -H "$JSON" \
  -d '{"currentPassword":"secret123","newPassword":"secret456"}' | field code

echo "7. login with new password"
TOKEN=$(curl -sf -X POST "$GW/api/auth/login" -H "$JSON" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"secret456\"}" | field result.accessToken)
AUTH="Authorization: Bearer $TOKEN"

echo "8. POST health record"
RECORD_ID=$(curl -sf -X POST "$GW/api/nutrition/me/health-records" -H "$AUTH" -H "$JSON" \
  -d '{"heightCm":170,"weightKg":65}' | field result.id)
echo "   id=$RECORD_ID"

echo "9. PUT health record"
curl -sf -X PUT "$GW/api/nutrition/me/health-records/$RECORD_ID" -H "$AUTH" -H "$JSON" \
  -d '{"heightCm":170,"weightKg":70}' | field result.bmi

echo "10. GET history"
curl -sf "$GW/api/nutrition/me/health-records?page=0&size=5" -H "$AUTH" | field result.totalElements

echo "11. GET latest"
curl -sf "$GW/api/nutrition/me/health-records/latest" -H "$AUTH" | field result.bmi

echo "12. GET allergens"
curl -sf "$GW/api/nutrition/allergens" -H "$AUTH" | count
A1=$(curl -sf "$GW/api/nutrition/allergens" -H "$AUTH" | field result.0.id)
A2=$(curl -sf "$GW/api/nutrition/allergens" -H "$AUTH" | field result.1.id)

echo "13. PUT + GET my allergies"
curl -sf -X PUT "$GW/api/nutrition/me/allergies" -H "$AUTH" -H "$JSON" \
  -d "{\"allergenIds\":[$A1,$A2,$A2]}" | count
curl -sf "$GW/api/nutrition/me/allergies" -H "$AUTH" | field result.0.code

echo "14. no token -> 401"
curl -s -o /dev/null -w "   HTTP %{http_code}\n" "$GW/api/users/me"
curl -s "$GW/api/nutrition/me/allergies" | field code
```

- [ ] **Step 6: Chạy script E2E**

Run: `cd <scratchpad> && bash e2e.sh`
Expected output (thứ tự từng dòng):

```
1. register
1000
2. login
3. GET /api/users/me
e2e-<timestamp>@example.com
4. PATCH /api/users/me
E2E Renamed
5. POST /api/users/me/avatar (through the gateway)
   http://localhost:9000/veggiepal-avatars/avatars/<id>/<uuid>.png
   avatar download HTTP 200
6. PUT /api/users/me/password
1000
7. login with new password
8. POST health record
   id=<number>
9. PUT health record
24.2
10. GET history
1
11. GET latest
24.2
12. GET allergens
32
13. PUT + GET my allergies
2
CORN
14. no token -> 401
   HTTP 401
1008
```

Ở bước 13, allergen đầu tiên là `CORN` vì nhóm GRAIN được sắp theo Collator tiếng Việt: "Bắp (ngô)" < "Gluten…" < "Kiều mạch".

Nếu bước 5 lỗi, kiểm tra log của gateway và identity-service. Rủi ro R1 trong spec chỉ được coi là đóng khi bước 5 trả về HTTP 200.

- [ ] **Step 7: Kiểm tra schema thực tế**

Run:

```bash
docker exec veggiepal-mysql mysql -uroot -p12345 -e "SHOW CREATE TABLE veggiepal_nutrition.allergens\G" | grep -i "category"
docker exec veggiepal-mysql mysql -uroot -p12345 -e "SHOW CREATE TABLE veggiepal_identity.users\G" | grep -i -E "role|date_of_birth"
docker exec veggiepal-mysql mysql -uroot -p12345 -e "SELECT COUNT(*) FROM veggiepal_nutrition.allergens"
```

Ghi lại **nguyên văn** kiểu cột `category` và `role` (`enum(...)` hay `varchar(...)`) để dùng ở Task 14. Kỳ vọng: có cột `date_of_birth` kiểu `date`, và bảng allergens có 32 dòng.

- [ ] **Step 8: Kiểm tra Swagger thủ công**

Mở `http://localhost:8080/swagger-ui.html`. Trong danh sách definition phải có cả "Identity Service" và "Nutrition Service". Nút **Authorize** phải hiện ra. Sau khi dán token, gọi thử `GET /users/me` bằng "Try it out" phải nhận `200`.

- [ ] **Step 9: Dừng các service**

Dừng 3 process `spring-boot:run` đang chạy nền. Giữ container Docker lại, trừ khi người dùng muốn tắt.

---

### Task 14: Cập nhật `CLAUDE.md`

**Files:**
- Modify: `CLAUDE.md`

**Interfaces:**
- Consumes: kết quả Step 7 của Task 13 (kiểu cột enum).

- [ ] **Step 1: Sửa phần Overview và Commands**

Trong `CLAUDE.md`:
- Ở phần Overview, đổi `Each service (\`api-gateway/\`, \`identity-service/\`)` thành `Each service (\`api-gateway/\`, \`identity-service/\`, \`nutrition-service/\`)`.
- Thay khối lệnh trong phần Commands bằng:

```bash
# Start MySQL (host 3307, root/12345) and MinIO (API 9000, console 9001, minioadmin/minioadmin).
# minio-init creates the public-read bucket veggiepal-avatars.
docker compose up -d

# Run a service (from its directory; on Windows use mvnw.cmd or Git Bash)
cd identity-service && ./mvnw spring-boot:run    # :8081
cd nutrition-service && ./mvnw spring-boot:run   # :8082
cd api-gateway && ./mvnw spring-boot:run         # :8080

# Build / test
./mvnw clean package
./mvnw test
./mvnw test -Dtest=ProfileServiceTest                         # single class
./mvnw test -Dtest=ProfileServiceTest#changePassword_success_storesNewHash  # single method
./mvnw test -Dtest='!VeggiepalApplicationTests'               # everything except the MySQL-backed contextLoads
```

- [ ] **Step 2: Sửa đoạn "Database gotcha"**

Thay đoạn **Database gotcha** bằng:

```markdown
**Database gotchas:**
- `docker-compose.yml` creates a database named `veggiepal`. identity-service connects to `veggiepal_identity` without `createDatabaseIfNotExist`, so create it by hand: `docker exec veggiepal-mysql mysql -uroot -p12345 -e "CREATE DATABASE IF NOT EXISTS veggiepal_identity"`. nutrition-service creates `veggiepal_nutrition` itself.
- Tables come from Hibernate `ddl-auto=update`; there are no migrations. nutrition-service seeds the `allergens` catalog from `src/main/resources/data.sql` (`INSERT IGNORE`, runs on every start).
- The `@SpringBootTest` `contextLoads` tests use the same MySQL (no test profile or H2). Unit tests and `@WebMvcTest` tests need no database.
```

Nếu Step 7 của Task 13 cho thấy cột `category` / `role` có kiểu `enum(...)`, thêm dòng sau vào cuối danh sách:

```markdown
- Hibernate maps `@Enumerated(EnumType.STRING)` to a native MySQL `ENUM` column. `ddl-auto=update` does not add new constants to it, so adding an enum value needs a manual `ALTER TABLE ... MODIFY COLUMN`.
```

Nếu cột có kiểu `varchar`, **không** thêm dòng đó.

- [ ] **Step 3: Sửa phần Request flow và Swagger**

Trong phần "Request flow through the gateway", thay gạch đầu dòng đầu tiên bằng:

```markdown
- Routes (all `StripPrefix=1`): `/api/auth/**` and `/api/users/**` → identity-service (8081); `/api/nutrition/**` → nutrition-service (8082).
- Do not set `spring.servlet.multipart.*` in api-gateway: the gateway disables multipart parsing on its own so file uploads stream through to the service.
```

Trong phần Swagger aggregation, thêm câu sau vào cuối đoạn "A new service needs all three...":

```markdown
Each service's `OpenApiConfig` also declares the `bearerAuth` scheme so the Swagger **Authorize** button sends the JWT.
```

- [ ] **Step 4: Thay phần "Auth state" và thêm convention mới**

Thay toàn bộ phần `### Auth state` bằng:

```markdown
### Auth (JWT)

- identity-service issues tokens in `JwtService`: HS256 (explicit), subject = email, claims `userId` and `role`, 24h expiry.
- Every service validates tokens as an OAuth2 Resource Server (`JwtConfig` builds a `NimbusJwtDecoder` with the same secret and HS256). The secret is `jwt.secret=${JWT_SECRET:...}` and must be identical in every service.
- `SecurityConfig.PUBLIC_ENDPOINTS` is the single list used for both `permitAll` and a `BearerTokenResolver` that ignores the Authorization header on public paths. Without it, a stale token would make `/auth/login` return 401.
- 401/403 are written by `SecurityExceptionHandler` as `ApiResponse` (1008/1009), because filter-chain errors never reach `@ControllerAdvice`.
- Controllers take `@Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt` and call `CurrentUser.id(jwt)`. Never take the user id from the body or the path.
- Tokens stay valid until they expire (no revocation, even after a password change).
```

Trong phần `### identity-service conventions`, thêm vào cuối danh sách:

```markdown
- **Error code ranges:** shared codes keep the same number in every service (1001, 1008, 1009, 1018 `INVALID_REQUEST`, 9999); identity-service uses 10xx, nutrition-service uses 20xx.
- **Avatar storage:** `FileStorageService` (S3 API via AWS SDK v2; MinIO locally). Configured with `storage.s3.*` / `S3_*` env vars. Uploads are validated by content type **and** magic bytes (`ImageTypeDetector`).
- **Controller slice tests:** `@WebMvcTest(X.class)` + `@Import({SecurityConfig.class, JwtConfig.class, SecurityExceptionHandler.class})` + `@MockitoBean` for the service; authenticate with `SecurityMockMvcRequestPostProcessors.jwt().jwt(t -> t.claim("userId", 7L))`.
```

Thêm phần mới ngay sau `### identity-service conventions`:

```markdown
### nutrition-service

Same conventions as identity-service, under package `com.veggiepal.nutrition` (its shared classes are copies, not a shared module). Controllers map `/nutrition/**`. It owns health records (height/weight history; BMI is computed server-side with HALF_UP to 1 decimal) and allergies (seeded `allergens` catalog + `user_allergies`). It stores `userId` from the JWT and never calls identity-service.
```

- [ ] **Step 5: Đọc lại toàn bộ file**

Đọc lại `CLAUDE.md` từ đầu đến cuối. Không được còn câu nào nói JWT dùng HS256 "chưa được kiểm tra", và không được còn câu "Nothing validates JWTs yet".

- [ ] **Step 6: Commit** (chỉ khi người dùng đã đồng ý)

```bash
git add CLAUDE.md
git commit -m "docs: document nutrition-service, JWT validation and MinIO in CLAUDE.md

Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"
```
