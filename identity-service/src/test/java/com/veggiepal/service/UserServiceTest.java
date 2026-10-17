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
