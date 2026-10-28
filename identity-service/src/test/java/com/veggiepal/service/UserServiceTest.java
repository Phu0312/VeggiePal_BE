package com.veggiepal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;

import com.veggiepal.dto.request.LoginRequest;
import com.veggiepal.dto.request.RegisterRequest;
import com.veggiepal.dto.response.RegisterResponse;
import com.veggiepal.dto.response.LoginResponse;
import com.veggiepal.entity.User;
import com.veggiepal.enums.Role;
import com.veggiepal.enums.UserStatus;
import com.veggiepal.enums.OtpPurpose;
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

    @Mock
    RefreshTokenService refreshTokenService;

    @Mock
    OtpService otpService;

    @InjectMocks
    UserService userService;

    @Test
    void createUser_startsPendingAndMailsACodeAfterSaving() {
        RegisterRequest request = registration();
        userService.createUser(request);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        var ordered = org.mockito.Mockito.inOrder(userRepository, otpService);
        ordered.verify(userRepository).existsByEmail("an@example.com");
        ordered.verify(userRepository).save(saved.capture());
        ordered.verify(otpService).issue("an@example.com", OtpPurpose.VERIFY_EMAIL);
        assertThat(saved.getValue().getStatus()).isEqualTo(UserStatus.PENDING);
        assertThat(saved.getValue().getEmailVerified()).isFalse();
        assertThat(saved.getValue().getEmail()).isEqualTo("an@example.com");
    }

    @Test
    void createUser_mailDown_stillReturnsTheRegistration() {
        RegisterRequest request = registration();
        when(userMapper.toUserResponse(any(User.class))).thenReturn(new RegisterResponse());
        doThrow(new AppException(ErrorCode.MAIL_UNAVAILABLE))
                .when(otpService).issue("an@example.com", OtpPurpose.VERIFY_EMAIL);

        assertThat(userService.createUser(request)).isNotNull();
    }

    @Test
    void createUser_otherAppFailureIsNotSwallowed() {
        RegisterRequest request = registration();
        doThrow(new AppException(ErrorCode.INVALID_REQUEST))
                .when(otpService).issue("an@example.com", OtpPurpose.VERIFY_EMAIL);

        assertThatThrownBy(() -> userService.createUser(request)).isInstanceOfSatisfying(AppException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void createUser_unexpectedFailureIsNotSwallowed() {
        RegisterRequest request = registration();
        doThrow(new IllegalStateException("storage unavailable"))
                .when(otpService).issue("an@example.com", OtpPurpose.VERIFY_EMAIL);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(IllegalStateException.class).hasMessage("storage unavailable");
    }

    RegisterRequest registration() {
        RegisterRequest request = RegisterRequest.builder()
                .email(" An@Example.com ").password("secret123").fullName("An").build();
        when(userMapper.toUser(request)).thenReturn(new User());
        when(passwordEncoder.encode("secret123")).thenReturn("hash");
        return request;
    }

    @Test
    void resendVerification_pendingNormalizesEmailAndSendsCode() {
        when(userRepository.findByEmail("an@example.com")).thenReturn(Optional.of(passwordUser(UserStatus.PENDING)));
        userService.resendVerification(" An@Example.com ");
        verify(otpService).issue("an@example.com", OtpPurpose.VERIFY_EMAIL);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = UserStatus.class, names = {"ACTIVE", "BLOCKED", "INACTIVE"})
    void resendVerification_ineligibleAccountSendsNothing(UserStatus status) {
        when(userRepository.findByEmail("an@example.com")).thenReturn(Optional.of(passwordUser(status)));
        userService.resendVerification("an@example.com");
        verifyNoInteractions(otpService);
    }

    @Test
    void resendVerification_unknownEmailSendsNothing() {
        userService.resendVerification("ghost@example.com");
        verifyNoInteractions(otpService);
    }

    @Test
    void resendVerification_mailDownStillSucceeds() {
        when(userRepository.findByEmail("an@example.com")).thenReturn(Optional.of(passwordUser(UserStatus.PENDING)));
        doThrow(new AppException(ErrorCode.MAIL_UNAVAILABLE))
                .when(otpService).issue("an@example.com", OtpPurpose.VERIFY_EMAIL);
        org.assertj.core.api.Assertions.assertThatCode(() -> userService.resendVerification("an@example.com"))
                .doesNotThrowAnyException();
    }

    @Test
    void resendVerification_otherAppFailureIsNotSwallowed() {
        when(userRepository.findByEmail("an@example.com")).thenReturn(Optional.of(passwordUser(UserStatus.PENDING)));
        doThrow(new AppException(ErrorCode.INVALID_REQUEST))
                .when(otpService).issue("an@example.com", OtpPurpose.VERIFY_EMAIL);
        assertThatThrownBy(() -> userService.resendVerification("an@example.com"))
                .isInstanceOfSatisfying(AppException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    static User passwordUser(UserStatus status) {
        return User.builder().id(7L).email("an@example.com").fullName("An")
                .passwordHash("hash").role(Role.USER).status(status).emailVerified(true).build();
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = UserStatus.class, names = {"ACTIVE", "PENDING"})
    void forgotPassword_eligibleAccountNormalizesAndSendsResetCode(UserStatus status) {
        when(userRepository.findByEmail("an@example.com")).thenReturn(Optional.of(passwordUser(status)));
        userService.forgotPassword(" An@Example.com ");
        verify(otpService).issue("an@example.com", OtpPurpose.RESET_PASSWORD);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(value = UserStatus.class, names = {"BLOCKED", "INACTIVE"})
    void forgotPassword_lockedAccountSendsNothing(UserStatus status) {
        when(userRepository.findByEmail("an@example.com")).thenReturn(Optional.of(passwordUser(status)));
        userService.forgotPassword("an@example.com");
        verifyNoInteractions(otpService);
    }

    @Test
    void forgotPassword_unknownAccountIsSilent() {
        userService.forgotPassword("ghost@example.com");
        verifyNoInteractions(otpService);
    }

    @Test
    void forgotPassword_mailUnavailableIsSilent() {
        when(userRepository.findByEmail("an@example.com")).thenReturn(Optional.of(passwordUser(UserStatus.ACTIVE)));
        doThrow(new AppException(ErrorCode.MAIL_UNAVAILABLE)).when(otpService).issue("an@example.com", OtpPurpose.RESET_PASSWORD);
        org.assertj.core.api.Assertions.assertThatCode(() -> userService.forgotPassword("an@example.com")).doesNotThrowAnyException();
    }

    @Test
    void forgotPassword_otherAppFailurePropagates() {
        when(userRepository.findByEmail("an@example.com")).thenReturn(Optional.of(passwordUser(UserStatus.ACTIVE)));
        doThrow(new AppException(ErrorCode.INVALID_REQUEST)).when(otpService).issue("an@example.com", OtpPurpose.RESET_PASSWORD);
        assertThatThrownBy(() -> userService.forgotPassword("an@example.com")).isInstanceOfSatisfying(AppException.class,
                error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST));
    }

    @Test
    void forgotPassword_unexpectedFailurePropagates() {
        when(userRepository.findByEmail("an@example.com")).thenReturn(Optional.of(passwordUser(UserStatus.ACTIVE)));
        doThrow(new IllegalStateException("storage unavailable")).when(otpService).issue("an@example.com", OtpPurpose.RESET_PASSWORD);
        assertThatThrownBy(() -> userService.forgotPassword("an@example.com")).isInstanceOf(IllegalStateException.class);
    }

    static LoginRequest loginRequest() {
        return LoginRequest.builder().email(" An@Example.com ").password("secret123").build();
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"BLOCKED, ACCOUNT_LOCKED", "PENDING, ACCOUNT_NOT_VERIFIED", "INACTIVE, ACCOUNT_NOT_ACTIVE"})
    void login_rightPasswordButNotActive_isRefusedWithoutAToken(UserStatus status, ErrorCode expected) {
        when(userRepository.lockByEmail("an@example.com")).thenReturn(Optional.of(passwordUser(status)));
        when(passwordEncoder.matches("secret123", "hash")).thenReturn(true);

        assertThatThrownBy(() -> userService.login(loginRequest()))
                .isInstanceOfSatisfying(AppException.class, e -> assertThat(e.getErrorCode()).isEqualTo(expected));
        verify(jwtService, never()).generateToken(any());
    }

    // Task sheet US1: without the password nobody learns that an account is locked
    @Test
    void login_wrongPasswordOnALockedAccount_staysGeneric() {
        when(userRepository.lockByEmail("an@example.com")).thenReturn(Optional.of(passwordUser(UserStatus.BLOCKED)));
        when(passwordEncoder.matches("secret123", "hash")).thenReturn(false);

        assertThatThrownBy(() -> userService.login(loginRequest()))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.UNAUTHENTICATED));
    }

    @Test
    void loginWithGoogle_newEmail_createsActiveVerifiedUser() {
        when(googleTokenVerifier.verify(ID_TOKEN)).thenReturn(googleJwt(" An@Gmail.com ", true, "An Nguyen"));
        when(userRepository.lockByEmail("an@gmail.com")).thenReturn(Optional.empty());
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
        when(userRepository.lockByEmail("an@gmail.com")).thenReturn(Optional.empty());
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
        when(userRepository.lockByEmail("an@gmail.com")).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode(anyString())).thenReturn("random-hash");
        when(jwtService.generateToken(existing)).thenReturn("app-token");

        LoginResponse response = userService.loginWithGoogle(ID_TOKEN);

        assertThat(response.getUserId()).isEqualTo(7L);
        assertThat(existing.getPasswordHash()).isEqualTo("random-hash");
        assertThat(existing.getEmailVerified()).isTrue();
        assertThat(existing.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(existing.getAvatarUrl()).isEqualTo("https://res.cloudinary.com/demo/image/upload/own.jpg");
        verify(userRepository).save(existing);
        verify(refreshTokenService).revokeAll(7L);
    }

    @Test
    void loginWithGoogle_blockedUser_staysBlockedAndGetsNoToken() {
        User existing = existingUser(false, UserStatus.BLOCKED);
        when(googleTokenVerifier.verify(ID_TOKEN)).thenReturn(googleJwt("an@gmail.com", true, "An Nguyen"));
        when(userRepository.lockByEmail("an@gmail.com")).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode(anyString())).thenReturn("random-hash");
        assertThatThrownBy(() -> userService.loginWithGoogle(ID_TOKEN))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_LOCKED));

        assertThat(existing.getStatus()).isEqualTo(UserStatus.BLOCKED);
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void loginWithGoogle_verifiedExistingUser_isLeftUntouched() {
        User existing = existingUser(true, UserStatus.ACTIVE);
        when(googleTokenVerifier.verify(ID_TOKEN)).thenReturn(googleJwt("an@gmail.com", true, "An Nguyen"));
        when(userRepository.lockByEmail("an@gmail.com")).thenReturn(Optional.of(existing));
        when(jwtService.generateToken(existing)).thenReturn("app-token");

        LoginResponse response = userService.loginWithGoogle(ID_TOKEN);

        assertThat(response.getAccessToken()).isEqualTo("app-token");
        assertThat(existing.getPasswordHash()).isEqualTo("own-hash");
        verify(userRepository, never()).save(any());
        verify(refreshTokenService, never()).revokeAll(any());
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

    @Test void registrationPersistsOnlyEncodedPassword() {
        var request = com.veggiepal.dto.request.RegisterRequest.builder()
                .email("member@example.com").password("secret123").fullName("Member").build();
        when(userMapper.toUser(request)).thenReturn(new User());
        when(passwordEncoder.encode("secret123")).thenReturn("bcrypt-hash");
        userService.createUser(request);
        var saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("bcrypt-hash").isNotEqualTo(request.getPassword());
    }

    @Test
    void login_returnsARefreshTokenToo() {
        when(userRepository.lockByEmail("an@example.com")).thenReturn(Optional.of(passwordUser(UserStatus.ACTIVE)));
        when(passwordEncoder.matches("secret123", "hash")).thenReturn(true);
        when(jwtService.generateToken(any(User.class))).thenReturn("access");
        when(jwtService.accessTtlSeconds()).thenReturn(900L);
        when(refreshTokenService.issue(7L)).thenReturn("refresh");

        LoginResponse response = userService.login(loginRequest());

        assertThat(response.getRefreshToken()).isEqualTo("refresh");
        assertThat(response.getExpiresIn()).isEqualTo(900L);
    }

    @Test
    void refresh_rotatesForAnActiveUser() {
        when(refreshTokenService.rotate("old")).thenReturn(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(passwordUser(UserStatus.ACTIVE)));
        when(jwtService.generateToken(any(User.class))).thenReturn("access");
        when(refreshTokenService.issue(7L)).thenReturn("new");

        assertThat(userService.refresh("old").getRefreshToken()).isEqualTo("new");
    }

    // A user locked after signing in loses access at the next refresh
    @Test
    void refresh_lockedSinceSignIn_isRefused() {
        when(refreshTokenService.rotate("old")).thenReturn(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(passwordUser(UserStatus.BLOCKED)));

        assertThatThrownBy(() -> userService.refresh("old"))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_LOCKED));
        verify(refreshTokenService, never()).issue(any());
    }
}
