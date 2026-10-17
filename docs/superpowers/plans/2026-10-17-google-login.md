# Đăng nhập bằng Google Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** identity-service có `POST /auth/google`: nhận Google ID token từ FE, xác minh, tạo hoặc liên kết user theo email, rồi trả `LoginResponse` (JWT VeggiePal) giống hệt `/auth/login`.

**Architecture:** `GoogleTokenVerifier` (service mới) giữ một `NimbusJwtDecoder` **riêng, không phải bean** trỏ tới JWKS của Google, với validator `aud`/`iss`. `UserService.loginWithGoogle` dùng nó, áp quy tắc tài khoản của spec rồi tái dùng `JwtService`. `UserController` thêm endpoint, `SecurityConfig` mở public. Không đụng gateway hay các service khác.

**Tech Stack:** Java 21, Spring Boot 4.1.1, Spring Security 7.1.1 (`spring-security-oauth2-jose`, có sẵn qua resource server), Lombok, JUnit 5 + Mockito + MockMvc.

**Spec:** [`docs/superpowers/specs/2026-10-17-google-login-design.md`](../specs/2026-10-17-google-login-design.md)

## Global Constraints

- **Không commit, không push.** Long tự review diff và tự commit. Step cuối mỗi task chỉ dừng lại và bàn giao.
- **Không có parent POM.** Chạy Maven từ trong `identity-service/`. Trên Windows dùng Git Bash (`./mvnw`), vì PowerShell làm hỏng tham số `-Dtest=...`.
- **Không thêm dependency.** Không dùng `spring-boot-starter-oauth2-client` hay `google-api-client`.
- Package `com.veggiepal`. Style: `@RequiredArgsConstructor` + `@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)`, field khai báo trần. DTO là class Lombok `@Data @Builder @NoArgsConstructor @AllArgsConstructor`.
- Mã lỗi mới, đúng số: `GOOGLE_TOKEN_INVALID(1019, "Google sign-in failed", HttpStatus.UNAUTHORIZED)`.
- Property: `google.client-id=${GOOGLE_CLIENT_ID:}` (mặc định rỗng).
- Hằng số Google, đúng chuỗi: JWKS `https://www.googleapis.com/oauth2/v3/certs`; issuer hợp lệ `https://accounts.google.com` **và** `accounts.google.com`.
- **Đọc issuer bằng `jwt.getClaimAsString(JwtClaimNames.ISS)`, không dùng `jwt.getIssuer()`.** `getIssuer()` ép sang `URL` và ném `IllegalArgumentException` với `accounts.google.com` (không có scheme).
- `GoogleTokenVerifier` **không được** khai báo `JwtDecoder` làm `@Bean`. identity-service đã có bean `jwtDecoder` (HS256) cho resource server; thêm bean thứ hai sẽ làm hỏng xác thực.
- Email: `trim().toLowerCase()` trước khi tìm hoặc lưu.
- Vòng test nhanh: `./mvnw test -Dtest='!VeggiepalApplicationTests'`.

---

## File Structure

| File | Trách nhiệm |
|---|---|
| `identity-service/src/main/java/com/veggiepal/service/GoogleTokenVerifier.java` | (mới) Xác minh Google ID token: chữ ký, thời hạn, `aud`, `iss` |
| `identity-service/src/main/java/com/veggiepal/exception/ErrorCode.java` | (sửa) thêm `GOOGLE_TOKEN_INVALID` |
| `identity-service/src/main/resources/application.properties` | (sửa) `google.client-id` |
| `.env.example` | (sửa) mục `GOOGLE_CLIENT_ID` |
| `identity-service/src/main/java/com/veggiepal/service/UserService.java` | (sửa) `loginWithGoogle`, tách `toLoginResponse` |
| `identity-service/src/main/java/com/veggiepal/dto/request/GoogleLoginRequest.java` | (mới) body `{idToken}` |
| `identity-service/src/main/java/com/veggiepal/controller/UserController.java` | (sửa) `POST /auth/google` |
| `identity-service/src/main/java/com/veggiepal/configuration/SecurityConfig.java` | (sửa) `/auth/google` vào `PUBLIC_ENDPOINTS` |
| `identity-service/src/test/java/com/veggiepal/service/GoogleTokenVerifierTest.java` | (mới) |
| `identity-service/src/test/java/com/veggiepal/service/UserServiceTest.java` | (mới) |
| `identity-service/src/test/java/com/veggiepal/configuration/SecurityConfigTest.java` | (sửa) 2 case cho `/auth/google` |
| `CLAUDE.md` | (sửa) ghi chú Google login trong mục Auth |

---

### Task 1: `GoogleTokenVerifier` + mã lỗi + cấu hình

**Files:**
- Create: `identity-service/src/main/java/com/veggiepal/service/GoogleTokenVerifier.java`
- Modify: `identity-service/src/main/java/com/veggiepal/exception/ErrorCode.java:47`
- Modify: `identity-service/src/main/resources/application.properties` (sau dòng `jwt.secret`)
- Modify: `.env.example` (cuối mục 3)
- Test: `identity-service/src/test/java/com/veggiepal/service/GoogleTokenVerifierTest.java`

**Interfaces:**
- Consumes: không có.
- Produces:
  - `com.veggiepal.service.GoogleTokenVerifier` (`@Service`), constructor `GoogleTokenVerifier(@Value("${google.client-id}") String clientId)`.
  - `public Jwt verify(String idToken)`: trả `org.springframework.security.oauth2.jwt.Jwt` đã xác minh, ném `AppException(ErrorCode.GOOGLE_TOKEN_INVALID)` với mọi `JwtException`.
  - `static OAuth2TokenValidator<Jwt> validator(String clientId)` (package-private, cho test).
  - `ErrorCode.GOOGLE_TOKEN_INVALID` (1019, 401).

- [ ] **Step 1: Viết test fail**

`identity-service/src/test/java/com/veggiepal/service/GoogleTokenVerifierTest.java`:

```java
package com.veggiepal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;

import com.veggiepal.exception.AppException;
import com.veggiepal.exception.ErrorCode;

class GoogleTokenVerifierTest {

    static final String CLIENT_ID = "test-client.apps.googleusercontent.com";

    final OAuth2TokenValidator<Jwt> validator = GoogleTokenVerifier.validator(CLIENT_ID);

    @Test
    void validator_acceptsTokenIssuedForThisClient() {
        Jwt jwt = token(CLIENT_ID, "https://accounts.google.com", Instant.now().plusSeconds(600));

        assertThat(validator.validate(jwt).hasErrors()).isFalse();
    }

    @Test
    void validator_acceptsIssuerWithoutScheme() {
        Jwt jwt = token(CLIENT_ID, "accounts.google.com", Instant.now().plusSeconds(600));

        assertThat(validator.validate(jwt).hasErrors()).isFalse();
    }

    @Test
    void validator_rejectsTokenForAnotherClient() {
        Jwt jwt = token("other-client.apps.googleusercontent.com", "https://accounts.google.com",
                Instant.now().plusSeconds(600));

        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }

    @Test
    void validator_rejectsNonGoogleIssuer() {
        Jwt jwt = token(CLIENT_ID, "https://evil.example.com", Instant.now().plusSeconds(600));

        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }

    @Test
    void validator_rejectsExpiredToken() {
        Jwt jwt = token(CLIENT_ID, "https://accounts.google.com", Instant.now().minusSeconds(600));

        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }

    @Test
    void verify_malformedToken_throwsGoogleTokenInvalid() {
        // Parsing fails before the decoder ever fetches Google's JWKS, so this needs no network.
        GoogleTokenVerifier verifier = new GoogleTokenVerifier(CLIENT_ID);

        assertThatThrownBy(() -> verifier.verify("not-a-jwt"))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.GOOGLE_TOKEN_INVALID));
    }

    static Jwt token(String audience, String issuer, Instant expiresAt) {
        return Jwt.withTokenValue("google-id-token")
                .header("alg", "RS256")
                .audience(List.of(audience))
                .claim("iss", issuer)
                .issuedAt(expiresAt.minusSeconds(3600))
                .expiresAt(expiresAt)
                .build();
    }
}
```

- [ ] **Step 2: Chạy test, xác nhận fail**

Run (Git Bash, trong `identity-service/`): `./mvnw test -Dtest=GoogleTokenVerifierTest`
Expected: COMPILATION ERROR, `cannot find symbol: class GoogleTokenVerifier` và `GOOGLE_TOKEN_INVALID`.

- [ ] **Step 3: Thêm mã lỗi**

`ErrorCode.java`: thay dòng cuối `INVALID_REQUEST(...)`;` thành:

```java
    INVALID_REQUEST(1018, "Invalid request data", HttpStatus.BAD_REQUEST),

    GOOGLE_TOKEN_INVALID(1019, "Google sign-in failed", HttpStatus.UNAUTHORIZED);
```

- [ ] **Step 4: Viết `GoogleTokenVerifier`**

`identity-service/src/main/java/com/veggiepal/service/GoogleTokenVerifier.java`:

```java
package com.veggiepal.service;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

import com.veggiepal.exception.AppException;
import com.veggiepal.exception.ErrorCode;

// Deliberately not a JwtDecoder bean: the resource server already uses the HS256 jwtDecoder bean.
@Service
public class GoogleTokenVerifier {

    private static final String JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";

    private static final Set<String> ISSUERS = Set.of("https://accounts.google.com", "accounts.google.com");

    private final NimbusJwtDecoder decoder;

    public GoogleTokenVerifier(@Value("${google.client-id}") String clientId) {
        this.decoder = NimbusJwtDecoder.withJwkSetUri(JWK_SET_URI).build();
        this.decoder.setJwtValidator(validator(clientId));
    }

    static OAuth2TokenValidator<Jwt> validator(String clientId) {
        // getClaimAsString, not getIssuer(): getIssuer() coerces to URL and throws on "accounts.google.com".
        OAuth2TokenValidator<Jwt> audienceAndIssuer = jwt -> {
            List<String> audience = jwt.getAudience();
            boolean valid = audience != null
                    && audience.contains(clientId)
                    && ISSUERS.contains(jwt.getClaimAsString(JwtClaimNames.ISS));
            return valid
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(
                            new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, "Not a Google ID token for this client", null));
        };
        return JwtValidators.createDefaultWithValidators(audienceAndIssuer);
    }

    public Jwt verify(String idToken) {
        try {
            return decoder.decode(idToken);
        } catch (JwtException e) {
            throw new AppException(ErrorCode.GOOGLE_TOKEN_INVALID);
        }
    }
}
```

- [ ] **Step 5: Thêm cấu hình**

`identity-service/src/main/resources/application.properties`, ngay sau dòng `jwt.secret=...`:

```properties

google.client-id=${GOOGLE_CLIENT_ID:}
```

`.env.example`, ngay sau dòng `CLOUDINARY_API_SECRET=your_cloudinary_api_secret` (trước mục 4):

```
# ------------------------------------------------------------------------------
# 3b. GOOGLE SIGN-IN (identity-service: POST /auth/google)
# ------------------------------------------------------------------------------
# Web client ID (không phải secret), phải TRÙNG với VITE_GOOGLE_CLIENT_ID bên FE.
GOOGLE_CLIENT_ID=your_web_client_id.apps.googleusercontent.com
```

- [ ] **Step 6: Chạy test, xác nhận pass**

Run: `./mvnw test -Dtest=GoogleTokenVerifierTest`
Expected: `Tests run: 6, Failures: 0, Errors: 0`.

- [ ] **Step 7: Dừng, bàn giao cho Long review** (không commit).

---

### Task 2: `UserService.loginWithGoogle`

**Files:**
- Modify: `identity-service/src/main/java/com/veggiepal/service/UserService.java`
- Test: `identity-service/src/test/java/com/veggiepal/service/UserServiceTest.java` (mới)

**Interfaces:**
- Consumes: `GoogleTokenVerifier.verify(String) : Jwt`, `ErrorCode.GOOGLE_TOKEN_INVALID` (Task 1).
- Produces: `public LoginResponse loginWithGoogle(String idToken)` trên `UserService`.

- [ ] **Step 1: Viết test fail**

`identity-service/src/test/java/com/veggiepal/service/UserServiceTest.java`:

```java
package com.veggiepal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;

import com.veggiepal.dto.response.LoginResponse;
import com.veggiepal.entity.User;
import com.veggiepal.enums.Role;
import com.veggiepal.enums.UserStatus;
import com.veggiepal.exception.AppException;
import com.veggiepal.exception.ErrorCode;
import com.veggiepal.mapper.UserMapper;
import com.veggiepal.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    static final String ID_TOKEN = "google-id-token";
    static final String PICTURE = "https://lh3.googleusercontent.com/a/photo";

    @Mock
    UserRepository userRepository;

    @Mock
    UserMapper userMapper;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    JwtService jwtService;

    @Mock
    GoogleTokenVerifier googleTokenVerifier;

    @InjectMocks
    UserService userService;

    @Test
    void loginWithGoogle_newEmail_createsActiveVerifiedUser() {
        when(googleTokenVerifier.verify(ID_TOKEN)).thenReturn(googleJwt(" An@Gmail.com ", true, "An Nguyen"));
        when(userRepository.findByEmail("an@gmail.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("random-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(7L);
            return user;
        });
        when(jwtService.generateToken(any(User.class))).thenReturn("app-token");

        LoginResponse response = userService.loginWithGoogle(ID_TOKEN);

        assertThat(response.getAccessToken()).isEqualTo("app-token");
        assertThat(response.getUserId()).isEqualTo(7L);
        assertThat(response.getEmail()).isEqualTo("an@gmail.com");
        assertThat(response.getFullName()).isEqualTo("An Nguyen");
        assertThat(response.getRole()).isEqualTo(Role.USER);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(saved.getValue().getEmailVerified()).isTrue();
        assertThat(saved.getValue().getAvatarUrl()).isEqualTo(PICTURE);
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("random-hash");
    }

    @Test
    void loginWithGoogle_newEmailWithoutName_usesEmailLocalPart() {
        when(googleTokenVerifier.verify(ID_TOKEN)).thenReturn(googleJwt("an@gmail.com", true, null));
        when(userRepository.findByEmail("an@gmail.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("random-hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(User.class))).thenReturn("app-token");

        LoginResponse response = userService.loginWithGoogle(ID_TOKEN);

        assertThat(response.getFullName()).isEqualTo("an");
    }

    @Test
    void loginWithGoogle_unverifiedExistingUser_replacesPasswordAndActivates() {
        User existing = existingUser(false, UserStatus.PENDING);
        when(googleTokenVerifier.verify(ID_TOKEN)).thenReturn(googleJwt("an@gmail.com", true, "An Nguyen"));
        when(userRepository.findByEmail("an@gmail.com")).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode(anyString())).thenReturn("random-hash");
        when(jwtService.generateToken(existing)).thenReturn("app-token");

        LoginResponse response = userService.loginWithGoogle(ID_TOKEN);

        assertThat(response.getUserId()).isEqualTo(7L);
        assertThat(existing.getPasswordHash()).isEqualTo("random-hash");
        assertThat(existing.getEmailVerified()).isTrue();
        assertThat(existing.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(existing.getAvatarUrl()).isEqualTo("https://res.cloudinary.com/demo/image/upload/own.jpg");
        verify(userRepository).save(existing);
    }

    @Test
    void loginWithGoogle_unverifiedBlockedUser_staysBlocked() {
        User existing = existingUser(false, UserStatus.BLOCKED);
        when(googleTokenVerifier.verify(ID_TOKEN)).thenReturn(googleJwt("an@gmail.com", true, "An Nguyen"));
        when(userRepository.findByEmail("an@gmail.com")).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode(anyString())).thenReturn("random-hash");
        when(jwtService.generateToken(existing)).thenReturn("app-token");

        userService.loginWithGoogle(ID_TOKEN);

        assertThat(existing.getStatus()).isEqualTo(UserStatus.BLOCKED);
        assertThat(existing.getPasswordHash()).isEqualTo("random-hash");
    }

    @Test
    void loginWithGoogle_verifiedExistingUser_isLeftUntouched() {
        User existing = existingUser(true, UserStatus.ACTIVE);
        when(googleTokenVerifier.verify(ID_TOKEN)).thenReturn(googleJwt("an@gmail.com", true, "An Nguyen"));
        when(userRepository.findByEmail("an@gmail.com")).thenReturn(Optional.of(existing));
        when(jwtService.generateToken(existing)).thenReturn("app-token");

        LoginResponse response = userService.loginWithGoogle(ID_TOKEN);

        assertThat(response.getAccessToken()).isEqualTo("app-token");
        assertThat(existing.getPasswordHash()).isEqualTo("own-hash");
        verify(userRepository, never()).save(any());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void loginWithGoogle_emailNotVerifiedByGoogle_throwsGoogleTokenInvalid() {
        when(googleTokenVerifier.verify(ID_TOKEN)).thenReturn(googleJwt("an@gmail.com", false, "An Nguyen"));

        assertThatThrownBy(() -> userService.loginWithGoogle(ID_TOKEN))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.GOOGLE_TOKEN_INVALID));
        verifyNoInteractions(userRepository, jwtService);
    }

    static Jwt googleJwt(String email, boolean emailVerified, String name) {
        Jwt.Builder builder = Jwt.withTokenValue(ID_TOKEN)
                .header("alg", "RS256")
                .claim("email", email)
                .claim("email_verified", emailVerified)
                .claim("picture", PICTURE);
        if (name != null) {
            builder.claim("name", name);
        }
        return builder.build();
    }

    static User existingUser(boolean emailVerified, UserStatus status) {
        return User.builder()
                .id(7L)
                .email("an@gmail.com")
                .fullName("An")
                .passwordHash("own-hash")
                .avatarUrl("https://res.cloudinary.com/demo/image/upload/own.jpg")
                .role(Role.USER)
                .status(status)
                .emailVerified(emailVerified)
                .build();
    }
}
```

- [ ] **Step 2: Chạy test, xác nhận fail**

Run: `./mvnw test -Dtest=UserServiceTest`
Expected: COMPILATION ERROR, `cannot find symbol: method loginWithGoogle(String)`.

- [ ] **Step 3: Implement**

Trong `UserService.java`:

Thêm import:

```java
import java.util.UUID;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;
```

Thêm field, ngay sau `JwtService jwtService;`:

```java
    GoogleTokenVerifier googleTokenVerifier;
```

Thay đoạn cuối của `login()`, từ `String token =` tới hết `.build();`, bằng:

```java
        return toLoginResponse(user);
```

Thêm vào cuối class:

```java
    public LoginResponse loginWithGoogle(String idToken) {

        Jwt google = googleTokenVerifier.verify(idToken);
        String email = google.getClaimAsString("email");

        if (!Boolean.TRUE.equals(google.getClaimAsBoolean("email_verified")) || !StringUtils.hasText(email)) {
            throw new AppException(ErrorCode.GOOGLE_TOKEN_INVALID);
        }

        String normalizedEmail = email.trim().toLowerCase();

        User user = userRepository.findByEmail(normalizedEmail)
                .map(this::linkGoogleAccount)
                .orElseGet(() -> createGoogleUser(normalizedEmail, google));

        return toLoginResponse(user);
    }

    // Google proved who owns this email, so a password set by whoever registered it first must stop working.
    private User linkGoogleAccount(User user) {

        if (Boolean.TRUE.equals(user.getEmailVerified())) {
            return user;
        }

        user.setPasswordHash(randomPasswordHash());
        user.setEmailVerified(true);
        if (user.getStatus() == UserStatus.PENDING) {
            user.setStatus(UserStatus.ACTIVE);
        }
        userRepository.save(user);
        return user;
    }

    private User createGoogleUser(String email, Jwt google) {

        String name = google.getClaimAsString("name");

        return userRepository.save(User.builder()
                .email(email)
                .fullName(StringUtils.hasText(name) ? name.trim() : email.substring(0, email.indexOf('@')))
                .avatarUrl(google.getClaimAsString("picture"))
                .passwordHash(randomPasswordHash())
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build());
    }

    private String randomPasswordHash() {
        return passwordEncoder.encode(UUID.randomUUID().toString());
    }

    private LoginResponse toLoginResponse(User user) {

        return LoginResponse.builder()
                .accessToken(jwtService.generateToken(user))
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }
```

- [ ] **Step 4: Chạy test, xác nhận pass**

Run: `./mvnw test -Dtest=UserServiceTest`
Expected: `Tests run: 6, Failures: 0, Errors: 0`.

- [ ] **Step 5: Chạy vòng nhanh để chắc `login()` vẫn chạy**

Run: `./mvnw test -Dtest='!VeggiepalApplicationTests'`
Expected: BUILD SUCCESS, 0 failures.

- [ ] **Step 6: Dừng, bàn giao cho Long review** (không commit).

---

### Task 3: Endpoint `POST /auth/google` và mở public

**Files:**
- Create: `identity-service/src/main/java/com/veggiepal/dto/request/GoogleLoginRequest.java`
- Modify: `identity-service/src/main/java/com/veggiepal/controller/UserController.java`
- Modify: `identity-service/src/main/java/com/veggiepal/configuration/SecurityConfig.java:32-40`
- Test: `identity-service/src/test/java/com/veggiepal/configuration/SecurityConfigTest.java`

**Interfaces:**
- Consumes: `UserService.loginWithGoogle(String) : LoginResponse` (Task 2).
- Produces: `POST /auth/google`, body `{"idToken": "..."}`, trả `ApiResponse<LoginResponse>`.

- [ ] **Step 1: Viết test fail**

Thêm vào cuối class `SecurityConfigTest` (trước dấu `}` cuối):

```java
    @Test
    void googleLogin_withStaleBearerHeader_isNotRejected() throws Exception {
        when(userService.loginWithGoogle("google-id-token"))
                .thenReturn(LoginResponse.builder().accessToken("token").build());

        mockMvc.perform(post("/auth/google")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer expired-or-garbage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idToken": "google-id-token"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.accessToken").value("token"));
    }

    @Test
    void googleLogin_blankIdToken_returns400InvalidRequest() throws Exception {
        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"idToken": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(1018));
    }
```

- [ ] **Step 2: Chạy test, xác nhận fail**

Run: `./mvnw test -Dtest=SecurityConfigTest`
Expected: 2 case mới FAIL với `Status expected:<200> but was:<401>` và `Status expected:<400> but was:<401>` (endpoint chưa có và chưa public).

- [ ] **Step 3: Tạo DTO**

`identity-service/src/main/java/com/veggiepal/dto/request/GoogleLoginRequest.java`:

```java
package com.veggiepal.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleLoginRequest {

    @NotBlank(message = "INVALID_REQUEST")
    private String idToken;
}
```

- [ ] **Step 4: Thêm endpoint**

`UserController.java`: thêm import `com.veggiepal.dto.request.GoogleLoginRequest;`, rồi thêm method sau `login(...)`:

```java
    @Operation(
            summary = "Login with a Google ID token"
    )
    @PostMapping("/google")
    ApiResponse<LoginResponse> loginWithGoogle(@RequestBody @Valid GoogleLoginRequest request) {

        return ApiResponse
                .<LoginResponse>builder()
                .result(userService.loginWithGoogle(request.getIdToken()))
                .build();
    }
```

- [ ] **Step 5: Mở public**

`SecurityConfig.java`, trong `PUBLIC_ENDPOINTS`, thêm sau `"/auth/login",`:

```java
            "/auth/google",
```

- [ ] **Step 6: Chạy test, xác nhận pass**

Run: `./mvnw test -Dtest=SecurityConfigTest`
Expected: `Tests run: 5, Failures: 0, Errors: 0`.

- [ ] **Step 7: Dừng, bàn giao cho Long review** (không commit).

---

### Task 4: Tài liệu và kiểm tra toàn bộ

**Files:**
- Modify: `CLAUDE.md` (mục `### Auth (JWT)`, sau bullet `identity-service issues tokens in JwtService...`)

**Interfaces:**
- Consumes: toàn bộ Task 1–3.
- Produces: không có.

- [ ] **Step 1: Cập nhật `CLAUDE.md`**

Thêm ngay sau dòng `- identity-service issues tokens in \`JwtService\`: ...`:

```markdown
- **Google login** (`POST /auth/google`): the FE sends the Google Identity Services ID token (`credential`) and gets the same `LoginResponse` as `/auth/login`. `GoogleTokenVerifier` checks it against Google's JWKS with `aud` = `google.client-id` (`GOOGLE_CLIENT_ID`, which must equal the FE's `VITE_GOOGLE_CLIENT_ID`; empty by default, so every Google login is rejected until it is set). Its decoder is deliberately not a `JwtDecoder` bean (the resource server uses the HS256 one). Accounts link by email; linking an account whose `emailVerified` is false replaces its password hash with a random one (blocks pre-registration takeover), so that user can only sign in with Google afterwards.
```

- [ ] **Step 2: Vòng test nhanh**

Run (Git Bash, trong `identity-service/`): `./mvnw test -Dtest='!VeggiepalApplicationTests'`
Expected: BUILD SUCCESS, 0 failures.

- [ ] **Step 3: `contextLoads` với MySQL thật** (property mới không được làm vỡ context)

Run (ở thư mục gốc repo): `docker compose up -d mysql`, rồi trong `identity-service/`: `./mvnw test -Dtest=VeggiepalApplicationTests`
Expected: BUILD SUCCESS. Nếu lỗi kết nối DB thì kiểm tra `DB_PORT=3307` / `DB_PASSWORD` trong `.env` gốc trước khi nghi code.

- [ ] **Step 4: Smoke test thủ công (Long làm)**

1. Thêm `GOOGLE_CLIENT_ID=<đúng giá trị VITE_GOOGLE_CLIENT_ID>` vào `.env` gốc.
2. Chạy `identity-service` (`./mvnw spring-boot:run`) và `api-gateway`.
3. Từ FE bấm Google Sign-In. Hoặc lấy `credential` trong DevTools rồi gọi:
   `curl -X POST http://localhost:8080/api/auth/google -H "Content-Type: application/json" -d '{"idToken":"<credential>"}'`
4. Kỳ vọng `code: 1000` và có `result.accessToken`. Dùng token đó gọi `GET /api/users/me` phải ra 200.
5. Gọi lại với `"idToken":"abc"`, kỳ vọng HTTP 401 với `code: 1019`.

- [ ] **Step 5: Dừng, bàn giao cho Long review và commit.**
