package com.veggiepal.service;

import java.util.UUID;

import com.veggiepal.dto.request.LoginRequest;
import com.veggiepal.dto.response.LoginResponse;
import com.veggiepal.dto.response.ResetTokenResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.veggiepal.dto.request.RegisterRequest;
import com.veggiepal.dto.response.RegisterResponse;
import com.veggiepal.entity.User;
import com.veggiepal.enums.Role;
import com.veggiepal.enums.UserStatus;
import com.veggiepal.enums.OtpPurpose;
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
public class UserService {

    UserRepository userRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;
    JwtService jwtService;
    GoogleTokenVerifier googleTokenVerifier;
    RefreshTokenService refreshTokenService;
    OtpService otpService;

    public void forgotPassword(String email) {
        String normalized = email.trim().toLowerCase();
        userRepository.findByEmail(normalized)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE || user.getStatus() == UserStatus.PENDING)
                .ifPresent(user -> {
                    try {
                        otpService.issue(normalized, OtpPurpose.RESET_PASSWORD);
                    } catch (AppException exception) {
                        if (exception.getErrorCode() != ErrorCode.MAIL_UNAVAILABLE) throw exception;
                        log.warn("Could not send the reset code to {}: {}", normalized, exception.getErrorCode());
                    }
                });
    }

    /** OTP verification and one-use proof issuance share the owning OTP transaction. */
    public ResetTokenResponse verifyResetOtp(String email, String code) {
        String resetToken = otpService.verifyAndApply(email.trim().toLowerCase(), OtpPurpose.RESET_PASSWORD, code,
                (user, otp) -> otpService.issueResetToken(otp));
        return ResetTokenResponse.builder().resetToken(resetToken).expiresIn(600L).build();
    }

    /** Credentials, account flags, session revocation and proof deletion commit together. */
    public void resetPassword(String resetToken, String newPassword) {
        otpService.consumeResetTokenAndApply(resetToken, (user, otp) -> {
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            user.setEmailVerified(true);
            if (user.getStatus() == UserStatus.PENDING) user.setStatus(UserStatus.ACTIVE);
            userRepository.save(user);
            refreshTokenService.revokeAll(user.getId());
            return null;
        });
    }

    /** Verification and session issuance share the OTP transaction and authoritative user lock. */
    public LoginResponse verifyEmail(String email, String code) {
        String normalized = email.trim().toLowerCase();
        return otpService.verifyAndApply(normalized, OtpPurpose.VERIFY_EMAIL, code, (user, otp) -> {
            if (user.getStatus() == UserStatus.PENDING) {
                user.setStatus(UserStatus.ACTIVE);
            }
            requireActive(user);
            // Google linking or password recovery may have already completed this proof.
            if (Boolean.TRUE.equals(user.getEmailVerified())) {
                throw new AppException(ErrorCode.OTP_EXPIRED);
            }
            user.setEmailVerified(true);
            userRepository.save(user);
            otpService.discard(otp);
            return toLoginResponse(user);
        });
    }

    /** A generic response also covers ineligible emails, cooldown and unavailable SMTP. */
    public void resendVerification(String email) {
        String normalized = email.trim().toLowerCase();
        userRepository.findByEmail(normalized)
                .filter(user -> user.getStatus() == UserStatus.PENDING)
                .ifPresent(user -> {
                    try {
                        otpService.issue(normalized, OtpPurpose.VERIFY_EMAIL);
                    } catch (AppException exception) {
                        if (exception.getErrorCode() != ErrorCode.MAIL_UNAVAILABLE) throw exception;
                        log.warn("Could not send the verification code to {}: {}", normalized, exception.getErrorCode());
                    }
                });
    }

    public RegisterResponse createUser(RegisterRequest request) {

        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new AppException(ErrorCode.EMAIL_EXISTED);
        }

        User user = userMapper.toUser(request);

        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.USER);
        user.setStatus(UserStatus.PENDING);
        user.setEmailVerified(false);

        userRepository.save(user);
        // Repository save commits before the OTP's owning transaction and SMTP delivery.
        try {
            otpService.issue(email, OtpPurpose.VERIFY_EMAIL);
        } catch (AppException exception) {
            if (exception.getErrorCode() != ErrorCode.MAIL_UNAVAILABLE) throw exception;
            log.warn("Registered {} but could not send the verification code: {}", email, exception.getErrorCode());
        }
        return userMapper.toUserResponse(user);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {

        String email =
                request.getEmail()
                        .trim()
                        .toLowerCase();

        User user = userRepository
                .lockByEmail(email)
                .orElseThrow(
                        () -> new AppException(
                                ErrorCode.UNAUTHENTICATED
                        )
                );

        boolean passwordMatched =
                passwordEncoder.matches(
                        request.getPassword(),
                        user.getPasswordHash()
                );

        if (!passwordMatched) {
            throw new AppException(
                    ErrorCode.UNAUTHENTICATED
            );
        }

        // After the password check, so only someone who knows it learns the account's state (US1)
        requireActive(user);

        return toLoginResponse(user);
    }

    @Transactional
    public LoginResponse loginWithGoogle(String idToken) {

        Jwt google = googleTokenVerifier.verify(idToken);
        String email = google.getClaimAsString("email");

        if (!Boolean.TRUE.equals(google.getClaimAsBoolean("email_verified")) || !StringUtils.hasText(email)) {
            throw new AppException(ErrorCode.GOOGLE_TOKEN_INVALID);
        }

        String normalizedEmail = email.trim().toLowerCase();

        User user = userRepository.lockByEmail(normalizedEmail)
                .map(this::linkGoogleAccount)
                .orElseGet(() -> createGoogleUser(normalizedEmail, google));

        // Linking may have just activated a PENDING account; BLOCKED/INACTIVE still get no token
        requireActive(user);

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
        // Replacing a pre-registered password must also invalidate sessions it already created.
        refreshTokenService.revokeAll(user.getId());
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

    /** Task_be A6: only ACTIVE accounts are issued tokens (login, Google, refresh). */
    static void requireActive(User user) {

        switch (user.getStatus()) {
            case ACTIVE -> {
            }
            case BLOCKED -> throw new AppException(ErrorCode.ACCOUNT_LOCKED);
            case PENDING -> throw new AppException(ErrorCode.ACCOUNT_NOT_VERIFIED);
            case INACTIVE -> throw new AppException(ErrorCode.ACCOUNT_NOT_ACTIVE);
        }
    }

    private LoginResponse toLoginResponse(User user) {

        return LoginResponse.builder()
                .accessToken(jwtService.generateToken(user))
                .refreshToken(refreshTokenService.issue(user.getId()))
                .expiresIn(jwtService.accessTtlSeconds())
                .userId(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }

    /** task_be C11: one-use refresh, with the same ACTIVE gate as login. */
    @Transactional
    public LoginResponse refresh(String refreshToken) {

        Long userId = refreshTokenService.rotate(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.REFRESH_TOKEN_INVALID));

        requireActive(user);

        return toLoginResponse(user);
    }
}
