package com.veggiepal.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import com.veggiepal.entity.EmailOtp;
import com.veggiepal.entity.User;
import com.veggiepal.enums.OtpPurpose;
import com.veggiepal.exception.AppException;
import com.veggiepal.exception.ErrorCode;
import com.veggiepal.repository.EmailOtpRepository;
import com.veggiepal.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

/**
 * OTP mutations serialize on the same user row as refresh/admin operations, then lock
 * the current OTP. Owning facades commit expected bad attempts before throwing;
 * successful callbacks and all their account/session writes commit or roll back together.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OtpService {
    static final Duration TTL = Duration.ofMinutes(10);
    static final Duration RESEND_GAP = Duration.ofSeconds(60);
    static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(10);
    static final int MAX_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    EmailOtpRepository repository;
    UserRepository users;
    OtpMailer mailer;
    PasswordEncoder passwordEncoder;
    PlatformTransactionManager transactionManager;

    /** Call only after the account transaction has committed. SMTP never holds a DB lock. */
    public void issue(String email, OtpPurpose purpose) {
        requireOwningBoundary();
        String normalized = normalize(email);
        Delivery delivery = inTransaction(() -> {
            if (users.lockByEmail(normalized).isEmpty()) return null;
            LocalDateTime now = LocalDateTime.now();
            EmailOtp current = repository.findCurrentForUpdate(normalized, purpose).orElse(null);
            if (current != null && current.getCreatedAt().isAfter(now.minus(RESEND_GAP))) return null;

            String code = String.format(Locale.ROOT, "%06d", RANDOM.nextInt(1_000_000));
            repository.deleteByEmailAndPurpose(normalized, purpose);
            repository.save(EmailOtp.builder().email(normalized).purpose(purpose)
                    .codeHash(passwordEncoder.encode(code)).attempts(0)
                    .createdAt(now).expiresAt(now.plus(TTL)).build());
            return new Delivery(normalized, purpose == OtpPurpose.VERIFY_EMAIL
                    ? "VeggiePal - Xác minh email" : "VeggiePal - Đặt lại mật khẩu", code);
        });
        if (delivery != null) mailer.send(delivery.email(), delivery.subject(), delivery.code());
    }

    /** The callback receives authoritative locked entities; it must not perform network I/O. */
    public <T> T verifyAndApply(String email, OtpPurpose purpose, String code, BiFunction<User, EmailOtp, T> action) {
        requireOwningBoundary();
        String normalized = normalize(email);
        Verification<T> result = inTransaction(() -> {
            User user = users.lockByEmail(normalized).orElse(null);
            if (user == null) return Verification.failed(ErrorCode.OTP_EXPIRED);
            EmailOtp otp = repository.findCurrentForUpdate(normalized, purpose).orElse(null);
            if (otp == null || otp.getVerifiedAt() != null || otp.getAttempts() >= MAX_ATTEMPTS
                    || !otp.getExpiresAt().isAfter(LocalDateTime.now())) {
                return Verification.failed(ErrorCode.OTP_EXPIRED);
            }
            if (!StringUtils.hasText(code) || !passwordEncoder.matches(code, otp.getCodeHash())) {
                otp.setAttempts(otp.getAttempts() + 1);
                return Verification.failed(otp.getAttempts() >= MAX_ATTEMPTS
                        ? ErrorCode.OTP_EXPIRED : ErrorCode.OTP_INVALID);
            }
            otp.setVerifiedAt(LocalDateTime.now());
            return new Verification<>(action.apply(user, otp), null);
        });
        // Normal return commits a wrong guess. Unexpected callback errors roll back instead.
        if (result.error() != null) throw new AppException(result.error());
        return result.value();
    }

    /** Compatibility helper; protected writes should use verifyAndApply instead. */
    public EmailOtp verify(String email, OtpPurpose purpose, String code) { return verifyAndApply(email, purpose, code, (u, o) -> o); }

    /** May join a verification callback; authorization always comes from a locked live row. */
    public String issueResetToken(EmailOtp verified) {
        return inTransaction(() -> {
            EmailOtp current = lockCurrent(verified);
            if (current.getPurpose() != OtpPurpose.RESET_PASSWORD || current.getVerifiedAt() == null
                    || current.getResetTokenHash() != null || current.getAttempts() >= MAX_ATTEMPTS
                    || !current.getExpiresAt().isAfter(LocalDateTime.now())) {
                throw new AppException(ErrorCode.OTP_EXPIRED);
            }
            byte[] bytes = new byte[32];
            RANDOM.nextBytes(bytes);
            String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            current.setResetTokenHash(RefreshTokenService.sha256(raw));
            return raw;
        });
    }

    /** Password changes, session revocation and token deletion belong in this one transaction. */
    public <T> T consumeResetTokenAndApply(String raw, BiFunction<User, EmailOtp, T> action) {
        requireOwningBoundary();
        if (!StringUtils.hasText(raw)) throw new AppException(ErrorCode.RESET_TOKEN_INVALID);
        String hash = RefreshTokenService.sha256(raw);
        return inTransaction(() -> {
            String email = repository.findEmailByResetTokenHash(hash)
                    .orElseThrow(() -> new AppException(ErrorCode.RESET_TOKEN_INVALID));
            User user = users.lockByEmail(email)
                    .orElseThrow(() -> new AppException(ErrorCode.RESET_TOKEN_INVALID));
            EmailOtp otp = repository.findResetTokenForUpdate(hash)
                    .orElseThrow(() -> new AppException(ErrorCode.RESET_TOKEN_INVALID));
            if (!otp.getEmail().equals(user.getEmail()) || otp.getPurpose() != OtpPurpose.RESET_PASSWORD
                    || otp.getVerifiedAt() == null
                    || !otp.getVerifiedAt().plus(RESET_TOKEN_TTL).isAfter(LocalDateTime.now())) {
                throw new AppException(ErrorCode.RESET_TOKEN_INVALID);
            }
            T result = action.apply(user, otp);
            repository.delete(otp);
            return result;
        });
    }

    public String consumeResetToken(String raw) { return consumeResetTokenAndApply(raw, (u, o) -> u.getEmail()); }

    /** Safe inside a verification callback; a replaced detached row cannot delete the new code. */
    public void discard(EmailOtp otp) {
        inTransaction(() -> {
            repository.delete(lockCurrent(otp));
            return null;
        });
    }

    private EmailOtp lockCurrent(EmailOtp hint) {
        if (hint == null || hint.getId() == null || !StringUtils.hasText(hint.getEmail()) || hint.getPurpose() == null)
            throw new AppException(ErrorCode.OTP_EXPIRED);
        String email = normalize(hint.getEmail());
        users.lockByEmail(email).orElseThrow(() -> new AppException(ErrorCode.OTP_EXPIRED));
        return repository.findCurrentForUpdate(email, hint.getPurpose())
                .filter(current -> Objects.equals(current.getId(), hint.getId()))
                .orElseThrow(() -> new AppException(ErrorCode.OTP_EXPIRED));
    }

    private <T> T inTransaction(Supplier<T> action) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        // User rows serialize same-account writes. READ_COMMITTED avoids empty OTP index
        // gap locks deadlocking independent accounts' first inserts under MySQL's default RR.
        // REQUIRED still joins the owning verification callback for the small helpers.
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        return transaction.execute(status -> action.get());
    }

    private void requireOwningBoundary() {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("OTP facade must own its transaction; invoke it outside an enclosing transaction");
        }
    }

    private String normalize(String email) { return email == null ? "" : email.trim().toLowerCase(Locale.ROOT); }
    private record Delivery(String email, String subject, String code) {}
    private record Verification<T>(T value, ErrorCode error) {
        static <T> Verification<T> failed(ErrorCode error) { return new Verification<>(null, error); }
    }
}
