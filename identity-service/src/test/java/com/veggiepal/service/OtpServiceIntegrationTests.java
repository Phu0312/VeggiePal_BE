package com.veggiepal.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import com.veggiepal.entity.EmailOtp;
import com.veggiepal.entity.User;
import com.veggiepal.repository.RefreshTokenRepository;
import com.veggiepal.enums.OtpPurpose;
import com.veggiepal.enums.Role;
import com.veggiepal.enums.UserStatus;
import com.veggiepal.exception.AppException;
import com.veggiepal.exception.ErrorCode;
import com.veggiepal.repository.EmailOtpRepository;
import com.veggiepal.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** No enclosing test transaction: every assertion observes a completed production transaction. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:mysql://localhost:${DB_PORT:3307}/veggiepal_identity_otp_it?createDatabaseIfNotExist=true",
        "spring.datasource.username=${DB_USERNAME:root}",
        "spring.datasource.password=${DB_PASSWORD:12345}",
        "spring.datasource.hikari.connection-init-sql=SET SESSION innodb_lock_wait_timeout = 10",
        "spring.jpa.hibernate.ddl-auto=update", "spring.jpa.show-sql=false",
        "spring.sql.init.mode=never", "spring.config.import=",
        "jwt.secret=veggiepal-secret-key-must-be-at-least-32-characters"
})
@AutoConfigureMockMvc
class OtpServiceIntegrationTests {
    static final OtpPurpose PURPOSE = OtpPurpose.RESET_PASSWORD;
    static final String SCHEMA = "veggiepal_identity_otp_it";
    @Autowired OtpService service;
    @Autowired UserService userService;
    @Autowired RefreshTokenService refreshService;
    @Autowired AdminUserService adminService;
    @Autowired JwtDecoder decoder;
    @Autowired MockMvc mvc;
    @Autowired EmailOtpRepository otps;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager manager;
    @MockitoBean OtpMailer mailer;
    @MockitoBean GoogleTokenVerifier googleTokenVerifier;
    @MockitoSpyBean EmailOtpRepository observedOtps;
    @MockitoSpyBean UserRepository observedUsers;
    @MockitoSpyBean RefreshTokenRepository tokenRepository;
    ExecutorService executor;
    Gate gate;
    String email;
    Long userId;
    Long otherUserId;
    String otherEmail;
    AtomicReference<String> deliveredCode;
    AtomicInteger deliveries;

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(2);
        gate = new Gate();
        deliveredCode = new AtomicReference<>();
        deliveries = new AtomicInteger();
        email = "otp-it-" + UUID.randomUUID() + "@veggiepal.invalid";
        userId = users.saveAndFlush(User.builder().email(email).fullName("OTP integration fixture")
                .passwordHash(encoder.encode("fixture-password")).role(Role.USER)
                .status(UserStatus.PENDING).emailVerified(false).build()).getId();
        doAnswer(call -> {
            deliveredCode.set(call.getArgument(2));
            deliveries.incrementAndGet();
            return null;
        }).when(mailer).send(eq(email), anyString(), anyString());
    }

    @AfterEach
    void cleanUp() throws Exception {
        gate.open();
        executor.shutdown();
        assertThat(executor.awaitTermination(25, TimeUnit.SECONDS)).isTrue();
        userId = users.findByEmail(email).map(User::getId).orElse(userId);
        if (userId != null) {
            jdbc.update("delete from email_otps where email = ?", email);
            jdbc.update("delete from refresh_tokens where user_id = ?", userId);
            jdbc.update("delete from users where id = ? and email = ?", userId, email);
        }
        if (otherUserId != null) {
            jdbc.update("delete from email_otps where email = ?", otherEmail);
            jdbc.update("delete from users where id = ? and email = ?", otherUserId, otherEmail);
        }
    }

    @Test
    void registration_smtpFailureReturns200AndKeepsPendingAccountAndOtpCommitted() throws Exception {
        users.deleteById(userId);
        userId = null;
        doAnswer(call -> {
            // Read on another connection while SMTP is in flight: account and OTP must already be committed.
            var committed = submit(() -> users.findByEmail(email).orElseThrow());
            assertThat(result(committed).success().getStatus()).isEqualTo(UserStatus.PENDING);
            assertThat(verificationOtp().getId()).isNotNull();
            throw new AppException(ErrorCode.MAIL_UNAVAILABLE);
        }).when(mailer).send(eq(email), anyString(), anyString());

        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                {"email":"%s","password":"secret123","fullName":"New OTP fixture"}
                """.formatted(email)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.status").value("PENDING"))
                .andExpect(jsonPath("$.result.emailVerified").value(false));
        User saved = users.findByEmail(email).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(UserStatus.PENDING);
        assertThat(saved.getEmailVerified()).isFalse();
        assertThat(encoder.matches("secret123", saved.getPasswordHash())).isTrue();
        assertThat(verificationOtp().getVerifiedAt()).isNull();
    }

    @Test
    void resendVerification_smtpFailureAndUnknownAccountReturnIdentical200() throws Exception {
        doThrow(new AppException(ErrorCode.MAIL_UNAVAILABLE)).when(mailer).send(eq(email), anyString(), anyString());
        String pendingResponse = mvc.perform(post("/auth/resend-verification")
                        .header("Authorization", "Bearer stale-token").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(1000))
                .andReturn().getResponse().getContentAsString();
        String unknownResponse = mvc.perform(post("/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"unknown-" + email + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(pendingResponse).isEqualTo(unknownResponse);
        assertThat(verificationOtp().getId()).isNotNull();
    }

    @Test
    void verifyEmail_wrongAttemptsPersistThroughPublicUserService() {
        verificationFixture();
        for (int attempt = 1; attempt <= 5; attempt++) {
            assertCode(() -> userService.verifyEmail(email, "000000"),
                    attempt == 5 ? ErrorCode.OTP_EXPIRED : ErrorCode.OTP_INVALID);
            assertThat(verificationOtp().getAttempts()).isEqualTo(attempt);
        }
        assertCode(() -> userService.verifyEmail(email, "123456"), ErrorCode.OTP_EXPIRED);
        assertThat(users.findById(userId).orElseThrow().getStatus()).isEqualTo(UserStatus.PENDING);
        assertThat(refreshCount()).isZero();
    }

    @Test
    void verifyEmail_pendingAccountActivatesAndReturnsUsableTokensOnce() {
        verificationFixture();
        var response = userService.verifyEmail(" " + email.toUpperCase(java.util.Locale.ROOT) + " ", "123456");
        User saved = users.findById(userId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(saved.getEmailVerified()).isTrue();
        assertThat(decoder.decode(response.getAccessToken()).getClaimAsString("userId")).isEqualTo(userId.toString());
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getExpiresIn()).isEqualTo(900L);
        assertThat(jdbc.queryForObject("select token_hash from refresh_tokens where user_id = ?", String.class, userId))
                .isEqualTo(RefreshTokenService.sha256(response.getRefreshToken()));
        assertThat(otps.findFirstByEmailAndPurposeOrderByCreatedAtDesc(email, OtpPurpose.VERIFY_EMAIL)).isEmpty();
        assertCode(() -> userService.verifyEmail(email, "123456"), ErrorCode.OTP_EXPIRED);
        assertThat(refreshCount()).isEqualTo(1);
    }

    @Test
    void verifyEmail_oldRegistrationCodeAfterGoogleLinkIsExpiredAndGoogleSessionRemainsUsable() {
        service.issue(email, OtpPurpose.VERIFY_EMAIL);
        String registrationCode = deliveredCode.get();
        when(googleTokenVerifier.verify("google-proof")).thenReturn(
                org.springframework.security.oauth2.jwt.Jwt.withTokenValue("google-proof")
                        .header("alg", "RS256").claim("email", email).claim("email_verified", true).build());

        var googleSession = userService.loginWithGoogle("google-proof");
        User linked = users.findById(userId).orElseThrow();
        assertThat(linked.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(linked.getEmailVerified()).isTrue();
        assertThat(encoder.matches("fixture-password", linked.getPasswordHash())).isFalse();
        assertThat(refreshCount()).isEqualTo(1);

        assertCode(() -> userService.verifyEmail(email, registrationCode), ErrorCode.OTP_EXPIRED);

        assertThat(refreshCount()).isEqualTo(1);
        assertThat(verificationOtp().getVerifiedAt()).isNull();
        var refreshed = userService.refresh(googleSession.getRefreshToken());
        assertThat(refreshed.getUserId()).isEqualTo(userId);
        assertThat(refreshed.getRefreshToken()).isNotBlank().isNotEqualTo(googleSession.getRefreshToken());
        assertThat(refreshCount()).isEqualTo(1);
        assertThat(jdbc.queryForObject("select token_hash from refresh_tokens where user_id = ?", String.class, userId))
                .isEqualTo(RefreshTokenService.sha256(refreshed.getRefreshToken()));
    }

    @Test
    void verifyEmail_legacyActiveUnverifiedAccountCanStillProveItsEmail() {
        verificationFixture();
        jdbc.update("update users set status = 'ACTIVE' where id = ?", userId);

        var response = userService.verifyEmail(email, "123456");

        User saved = users.findById(userId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(saved.getEmailVerified()).isTrue();
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getRefreshToken()).isNotBlank();
        assertThat(refreshCount()).isEqualTo(1);
        assertThat(otps.findFirstByEmailAndPurposeOrderByCreatedAtDesc(email, OtpPurpose.VERIFY_EMAIL)).isEmpty();
    }

    @Test
    void verifyEmail_refreshStorageFailureRollsBackActivationAndOtpConsumption() {
        verificationFixture();
        doAnswer(call -> {
            org.mockito.Mockito.mockingDetails(tokenRepository).getMockCreationSettings().getDefaultAnswer().answer(call);
            throw new IllegalStateException("fixture refresh storage failure");
        }).when(tokenRepository).save(any(com.veggiepal.entity.RefreshToken.class));

        assertThatThrownBy(() -> userService.verifyEmail(email, "123456"))
                .isInstanceOf(org.springframework.dao.InvalidDataAccessApiUsageException.class)
                .hasRootCauseInstanceOf(IllegalStateException.class).hasRootCauseMessage("fixture refresh storage failure");
        User saved = users.findById(userId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(UserStatus.PENDING);
        assertThat(saved.getEmailVerified()).isFalse();
        assertThat(verificationOtp().getVerifiedAt()).isNull();
        assertThat(refreshCount()).isZero();
        reset(tokenRepository);
        assertThat(userService.verifyEmail(email, "123456").getRefreshToken()).isNotBlank();
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.CsvSource({"BLOCKED,ACCOUNT_LOCKED,false", "BLOCKED,ACCOUNT_LOCKED,true",
            "INACTIVE,ACCOUNT_NOT_ACTIVE,false", "INACTIVE,ACCOUNT_NOT_ACTIVE,true"})
    void verifyEmail_nonActiveAuthoritativeStateIsPreserved(UserStatus status, ErrorCode error, boolean emailVerified) {
        verificationFixture();
        jdbc.update("update users set status = ?, email_verified = ?, locked_reason = 'existing lock', locked_by = -1 where id = ?",
                status.name(), emailVerified, userId);
        assertCode(() -> userService.verifyEmail(email, "123456"), error);
        User saved = users.findById(userId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(status);
        assertThat(saved.getEmailVerified()).isEqualTo(emailVerified);
        assertThat(saved.getLockedReason()).isEqualTo("existing lock");
        assertThat(saved.getLockedBy()).isEqualTo(-1L);
        assertThat(verificationOtp().getVerifiedAt()).isNull();
        assertThat(refreshCount()).isZero();
    }

    @Test
    void verifyEmail_adminLockCommitsFirstAndVerificationCannotOverwriteIt() throws Exception {
        verificationFixture();
        AtomicBoolean first = new AtomicBoolean(true);
        doAnswer(call -> {
            Object value = org.mockito.Mockito.mockingDetails(observedUsers).getMockCreationSettings()
                    .getDefaultAnswer().answer(call);
            if (first.getAndSet(false)) gate.pause();
            return value;
        }).when(observedUsers).lockById(userId);
        var locking = submit(() -> adminService.lock(-1L, userId, "Admin wins"));
        gate.awaitReached();
        var verifying = submit(() -> userService.verifyEmail(email, "123456"));
        try { awaitDatabaseWaitOrCompletion(verifying); }
        finally { gate.open(); }
        result(locking).success();
        assertError(result(verifying).failure(), ErrorCode.ACCOUNT_LOCKED);
        assertBlocked("Admin wins");
        assertThat(users.findById(userId).orElseThrow().getEmailVerified()).isFalse();
        assertThat(verificationOtp().getVerifiedAt()).isNull();
        assertThat(refreshCount()).isZero();
    }

    @Test
    void verifyEmail_commitsFirstAndWaitingAdminLockRevokesItsNewSession() throws Exception {
        verificationFixture();
        AtomicBoolean first = new AtomicBoolean(true);
        doAnswer(call -> {
            Object value = org.mockito.Mockito.mockingDetails(observedOtps).getMockCreationSettings()
                    .getDefaultAnswer().answer(call);
            if (first.getAndSet(false)) gate.pause();
            return value;
        }).when(observedOtps).findCurrentForUpdate(email, OtpPurpose.VERIFY_EMAIL);
        var verifying = submit(() -> userService.verifyEmail(email, "123456"));
        gate.awaitReached();
        var locking = submit(() -> adminService.lock(-1L, userId, "Admin after verification"));
        try { awaitDatabaseWaitOrCompletion(locking); }
        finally { gate.open(); }
        assertThat(result(verifying).success().getRefreshToken()).isNotBlank();
        result(locking).success();
        assertBlocked("Admin after verification");
        assertThat(users.findById(userId).orElseThrow().getEmailVerified()).isTrue();
        assertThat(otps.findFirstByEmailAndPurposeOrderByCreatedAtDesc(email, OtpPurpose.VERIFY_EMAIL)).isEmpty();
        assertThat(refreshCount()).isZero();
    }

    void verificationFixture() {
        otps.saveAndFlush(EmailOtp.builder().email(email).purpose(OtpPurpose.VERIFY_EMAIL)
                .codeHash(encoder.encode("123456")).attempts(0).createdAt(LocalDateTime.now().minusMinutes(2))
                .expiresAt(LocalDateTime.now().plusMinutes(5)).build());
    }

    @Test
    void forgotPassword_mailFailureUnknownIneligibleAndCooldownHaveIdenticalResponses() throws Exception {
        doThrow(new AppException(ErrorCode.MAIL_UNAVAILABLE)).when(mailer).send(eq(email), anyString(), anyString());
        String expected = forgotResponse(email);
        assertThat(current().getId()).isNotNull();
        assertThat(forgotResponse(email)).isEqualTo(expected); // cooldown retains the committed challenge
        assertThat(forgotResponse("unknown-" + email)).isEqualTo(expected);
        for (String state : java.util.List.of("BLOCKED", "INACTIVE")) {
            jdbc.update("update users set status = ? where id = ?", state, userId);
            assertThat(forgotResponse(email)).isEqualTo(expected);
        }
        assertThat(expected).contains("If the email is registered, a code has been sent");
    }

    String forgotResponse(String address) throws Exception {
        return mvc.perform(post("/auth/forgot-password").header("Authorization", "Bearer stale-token")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"" + address + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result").doesNotExist()).andReturn().getResponse().getContentAsString();
    }

    @Test
    void verifyResetOtp_wrongAttemptsCommitThroughPublicFacade() {
        fixture();
        for (int attempt = 1; attempt <= 5; attempt++) {
            assertCode(() -> userService.verifyResetOtp(email, "000000"),
                    attempt == 5 ? ErrorCode.OTP_EXPIRED : ErrorCode.OTP_INVALID);
            assertThat(current().getAttempts()).isEqualTo(attempt);
        }
        assertCode(() -> userService.verifyResetOtp(email, "123456"), ErrorCode.OTP_EXPIRED);
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.EnumSource(UserStatus.class)
    void resetPassword_changesCredentialsRevokesSessionsAndPreservesNonPendingState(UserStatus state) {
        fixture();
        jdbc.update("update users set status = ?, locked_reason = 'retain metadata', locked_by = -1 where id = ?", state.name(), userId);
        String oldRefresh = refreshService.issue(userId);
        var proof = userService.verifyResetOtp(" " + email.toUpperCase(java.util.Locale.ROOT) + " ", "123456");
        assertThat(proof.getExpiresIn()).isEqualTo(600L);
        assertThat(current().getResetTokenHash()).isEqualTo(RefreshTokenService.sha256(proof.getResetToken()));
        assertCode(() -> userService.verifyResetOtp(email, "123456"), ErrorCode.OTP_EXPIRED);

        userService.resetPassword(proof.getResetToken(), "newSecret1");

        User saved = users.findById(userId).orElseThrow();
        assertThat(encoder.matches("newSecret1", saved.getPasswordHash())).isTrue();
        assertThat(saved.getEmailVerified()).isTrue();
        assertThat(saved.getStatus()).isEqualTo(state == UserStatus.PENDING ? UserStatus.ACTIVE : state);
        assertThat(saved.getLockedReason()).isEqualTo("retain metadata");
        assertThat(saved.getLockedBy()).isEqualTo(-1L);
        assertThat(refreshCount()).isZero();
        assertThat(otps.findFirstByEmailAndPurposeOrderByCreatedAtDesc(email, PURPOSE)).isEmpty();
        assertCode(() -> userService.refresh(oldRefresh), ErrorCode.REFRESH_TOKEN_INVALID);
        assertCode(() -> userService.resetPassword(proof.getResetToken(), "different-password"), ErrorCode.RESET_TOKEN_INVALID);
    }

    @Test
    void verifyEmail_oldRegistrationChallengeAfterResetIsExpired() {
        verificationFixture();
        fixture();
        var reset = userService.verifyResetOtp(email, "123456");
        userService.resetPassword(reset.getResetToken(), "newSecret1");
        assertCode(() -> userService.verifyEmail(email, "123456"), ErrorCode.OTP_EXPIRED);
        assertThat(verificationOtp().getVerifiedAt()).isNull();
        assertThat(refreshCount()).isZero();
        assertThat(encoder.matches("newSecret1", users.findById(userId).orElseThrow().getPasswordHash())).isTrue();
    }

    @Test
    void resetPassword_revokeFailureRollsBackPasswordTokenAndSessionDeletion() {
        fixture();
        String refresh = refreshService.issue(userId);
        String raw = userService.verifyResetOtp(email, "123456").getResetToken();
        doAnswer(call -> {
            Object result = org.mockito.Mockito.mockingDetails(tokenRepository).getMockCreationSettings().getDefaultAnswer().answer(call);
            tokenRepository.flush();
            throw new IllegalStateException("fixture revoke failure");
        }).when(tokenRepository).deleteByUserId(userId);
        assertThatThrownBy(() -> userService.resetPassword(raw, "newSecret1"))
                .hasRootCauseMessage("fixture revoke failure");
        User saved = users.findById(userId).orElseThrow();
        assertThat(encoder.matches("fixture-password", saved.getPasswordHash())).isTrue();
        assertThat(saved.getStatus()).isEqualTo(UserStatus.PENDING);
        assertThat(saved.getEmailVerified()).isFalse();
        assertThat(current().getResetTokenHash()).isEqualTo(RefreshTokenService.sha256(raw));
        assertThat(jdbc.queryForObject("select token_hash from refresh_tokens where user_id = ?", String.class, userId))
                .isEqualTo(RefreshTokenService.sha256(refresh));
        reset(tokenRepository);
        userService.resetPassword(raw, "newSecret1");
        assertThat(refreshCount()).isZero();
    }

    @Test
    void resetPassword_concurrentConsumersYieldOneSuccessAndOne1030() throws Exception {
        fixture();
        String raw = userService.verifyResetOtp(email, "123456").getResetToken();
        CyclicBarrier bothReadHint = new CyclicBarrier(2);
        doAnswer(call -> {
            Object value = org.mockito.Mockito.mockingDetails(observedOtps).getMockCreationSettings().getDefaultAnswer().answer(call);
            bothReadHint.await(15, TimeUnit.SECONDS);
            return value;
        }).when(observedOtps).findEmailByResetTokenHash(RefreshTokenService.sha256(raw));
        var first = submit(() -> { userService.resetPassword(raw, "password-first"); return "password-first"; });
        var second = submit(() -> { userService.resetPassword(raw, "password-second"); return "password-second"; });
        Outcome<String> one = result(first), two = result(second);
        assertThat(java.util.stream.Stream.of(one, two).filter(o -> o.failure() == null).count()).isEqualTo(1);
        assertError(one.failure() == null ? two.failure() : one.failure(), ErrorCode.RESET_TOKEN_INVALID);
        assertThat(encoder.matches(one.failure() == null ? one.value() : two.value(), users.findById(userId).orElseThrow().getPasswordHash())).isTrue();
        assertThat(refreshCount()).isZero();
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {true, false})
    void resetPassword_serializesBothRefreshOrderings(boolean resetFirst) throws Exception {
        fixture();
        jdbc.update("update users set status = 'ACTIVE' where id = ?", userId);
        String refresh = refreshService.issue(userId);
        String raw = userService.verifyResetOtp(email, "123456").getResetToken();
        pauseAfterFirstUserLock();
        var first = submit(() -> {
            if (resetFirst) { userService.resetPassword(raw, "newSecret1"); return null; }
            return userService.refresh(refresh);
        });
        gate.awaitReached();
        var second = submit(() -> {
            if (resetFirst) return userService.refresh(refresh);
            userService.resetPassword(raw, "newSecret1"); return null;
        });
        try { awaitDatabaseWaitOrCompletion(second); } finally { gate.open(); }
        result(first).success();
        if (resetFirst) assertError(result(second).failure(), ErrorCode.REFRESH_TOKEN_INVALID);
        else result(second).success();
        assertThat(refreshCount()).isZero();
        assertThat(encoder.matches("newSecret1", users.findById(userId).orElseThrow().getPasswordHash())).isTrue();
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(booleans = {true, false})
    void resetPassword_serializesBothAdminBanOrderings(boolean resetFirst) throws Exception {
        fixture();
        String raw = userService.verifyResetOtp(email, "123456").getResetToken();
        pauseAfterFirstUserLock();
        var first = submit(() -> {
            if (resetFirst) userService.resetPassword(raw, "newSecret1");
            else adminService.lock(-1L, userId, "Admin ban");
            return null;
        });
        gate.awaitReached();
        var second = submit(() -> {
            if (resetFirst) adminService.lock(-1L, userId, "Admin ban");
            else userService.resetPassword(raw, "newSecret1");
            return null;
        });
        try { awaitDatabaseWaitOrCompletion(second); } finally { gate.open(); }
        result(first).success(); result(second).success();
        assertBlocked("Admin ban");
        assertThat(users.findById(userId).orElseThrow().getEmailVerified()).isTrue();
        assertThat(refreshCount()).isZero();
    }

    void pauseAfterFirstUserLock() {
        AtomicBoolean first = new AtomicBoolean(true);
        doAnswer(call -> {
            Object value = org.mockito.Mockito.mockingDetails(observedUsers).getMockCreationSettings().getDefaultAnswer().answer(call);
            if (first.getAndSet(false)) gate.pause();
            return value;
        }).when(observedUsers).lockById(userId);
    }
    EmailOtp verificationOtp() {
        return otps.findFirstByEmailAndPurposeOrderByCreatedAtDesc(email, OtpPurpose.VERIFY_EMAIL).orElseThrow();
    }
    int refreshCount() {
        return jdbc.queryForObject("select count(*) from refresh_tokens where user_id = ?", Integer.class, userId);
    }
    void assertBlocked(String reason) {
        User saved = users.findById(userId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(UserStatus.BLOCKED);
        assertThat(saved.getLockedReason()).isEqualTo(reason);
        assertThat(saved.getLockedBy()).isEqualTo(-1L);
        assertThat(saved.getLockedAt()).isNotNull();
    }

    @Test
    void wrongAttempts_commitBeforeTheError_andFifthFailureExpiresTheCode() {
        fixture();
        for (int attempt = 1; attempt <= 5; attempt++) {
            assertCode(() -> service.verifyAndApply(email, PURPOSE, "000000", (u, o) -> null),
                    attempt == 5 ? ErrorCode.OTP_EXPIRED : ErrorCode.OTP_INVALID);
            assertThat(current().getAttempts()).isEqualTo(attempt);
        }
        assertCode(() -> service.verifyAndApply(email, PURPOSE, "123456", (u, o) -> null), ErrorCode.OTP_EXPIRED);
    }

    @Test
    void verifyHelper_alsoCommitsFailedAttempts() {
        fixture();
        assertCode(() -> service.verify(email, PURPOSE, "000000"), ErrorCode.OTP_INVALID);
        assertThat(current().getAttempts()).isEqualTo(1);
    }

    @Test
    void secondExchange_rejectsReplayAndPreservesTheFirstResetToken() {
        fixture();
        String raw = exchange();
        LocalDateTime verifiedAt = current().getVerifiedAt();
        assertCode(this::exchange, ErrorCode.OTP_EXPIRED);
        assertThat(current().getVerifiedAt()).isEqualTo(verifiedAt);
        assertThat(service.consumeResetToken(raw)).isEqualTo(email);
        assertCode(() -> service.consumeResetToken(raw), ErrorCode.RESET_TOKEN_INVALID);
    }

    @Test
    void concurrentFirstIssue_obeysCooldownAndSendsOneMail() throws Exception {
        pauseAfterFirstCurrentRead();
        var first = submit(() -> { service.issue(email, PURPOSE); return null; });
        gate.awaitReached();
        var second = submit(() -> { service.issue(email, PURPOSE); return null; });
        try { awaitDatabaseWaitOrCompletion(second); }
        finally { gate.open(); }
        result(first).success();
        result(second).success();
        assertThat(deliveries).hasValue(1);
        assertThat(jdbc.queryForObject("select count(*) from email_otps where email = ?", Integer.class, email)).isEqualTo(1);
        assertThat(encoder.matches(deliveredCode.get(), current().getCodeHash())).isTrue();
    }

    @Test
    void differentAccounts_firstIssuesDoNotDeadlockOnAnEmptyOtpIndex() throws Exception {
        otherEmail = "otp-it-" + UUID.randomUUID() + "@veggiepal.invalid";
        otherUserId = users.saveAndFlush(User.builder().email(otherEmail).fullName("Other OTP fixture")
                .passwordHash(encoder.encode("fixture-password")).role(Role.USER)
                .status(UserStatus.PENDING).emailVerified(false).build()).getId();
        CyclicBarrier bothReadEmpty = new CyclicBarrier(2);
        doAnswer(call -> {
            Object value = org.mockito.Mockito.mockingDetails(observedOtps).getMockCreationSettings()
                    .getDefaultAnswer().answer(call);
            bothReadEmpty.await(15, TimeUnit.SECONDS);
            return value;
        }).when(observedOtps).findCurrentForUpdate(anyString(), eq(PURPOSE));
        var first = submit(() -> { service.issue(email, PURPOSE); return null; });
        var second = submit(() -> { service.issue(otherEmail, PURPOSE); return null; });
        Outcome<?> one = result(first), two = result(second);
        one.success();
        two.success();
        assertThat(current().getId()).isNotNull();
        assertThat(otps.findFirstByEmailAndPurposeOrderByCreatedAtDesc(otherEmail, PURPOSE)).isPresent();
    }

    @Test
    void concurrentWrongGuesses_bothPersist() throws Exception {
        fixture();
        pauseAfterFirstCurrentRead();
        var first = submit(() -> service.verifyAndApply(email, PURPOSE, "000000", (u, o) -> null));
        gate.awaitReached();
        var second = submit(() -> service.verifyAndApply(email, PURPOSE, "999999", (u, o) -> null));
        try { awaitDatabaseWaitOrCompletion(second); }
        finally { gate.open(); }
        assertError(result(first).failure(), ErrorCode.OTP_INVALID);
        assertError(result(second).failure(), ErrorCode.OTP_INVALID);
        assertThat(current().getAttempts()).isEqualTo(2);
    }

    @Test
    void concurrentExchanges_yieldOneUsableToken() throws Exception {
        fixture();
        pauseAfterFirstCurrentRead();
        var first = submit(this::exchange);
        gate.awaitReached();
        var second = submit(this::exchange);
        try { awaitDatabaseWaitOrCompletion(second); }
        finally { gate.open(); }
        Outcome<String> one = result(first), two = result(second);
        assertThat(java.util.stream.Stream.of(one, two).filter(o -> o.failure() == null).count()).isEqualTo(1);
        assertError(one.failure() == null ? two.failure() : one.failure(), ErrorCode.OTP_EXPIRED);
        assertThat(service.consumeResetToken(one.failure() == null ? one.value() : two.value())).isEqualTo(email);
    }

    @Test
    void concurrentResetConsumption_yieldsOneSuccessAndOne1030() throws Exception {
        fixture();
        String raw = exchange();
        CyclicBarrier hintsRead = new CyclicBarrier(2);
        doAnswer(call -> {
            Object value = org.mockito.Mockito.mockingDetails(observedOtps).getMockCreationSettings()
                    .getDefaultAnswer().answer(call);
            hintsRead.await(15, TimeUnit.SECONDS);
            return value;
        }).when(observedOtps).findEmailByResetTokenHash(RefreshTokenService.sha256(raw));
        var first = submit(() -> service.consumeResetToken(raw));
        var second = submit(() -> service.consumeResetToken(raw));
        Outcome<String> one = result(first), two = result(second);
        assertThat(java.util.stream.Stream.of(one, two).filter(o -> o.failure() == null).count()).isEqualTo(1);
        assertError(one.failure() == null ? two.failure() : one.failure(), ErrorCode.RESET_TOKEN_INVALID);
    }

    @Test
    void callbackFailure_rollsBackVerifiedStateAndUserMutation() {
        fixture();
        assertThatThrownBy(() -> service.verifyAndApply(email, PURPOSE, "123456", (u, o) -> {
            u.setFullName("must roll back");
            service.discard(o);
            throw new IllegalStateException("fixture action failure");
        })).isInstanceOf(IllegalStateException.class).hasMessage("fixture action failure");
        assertThat(current().getVerifiedAt()).isNull();
        assertThat(users.findById(userId).orElseThrow().getFullName()).isEqualTo("OTP integration fixture");
        assertThat(exchange()).isNotBlank();
    }

    @Test
    void resetCallbackFailure_rollsBackConsumptionAndKeepsTokenUsable() {
        fixture();
        String raw = exchange();
        assertThatThrownBy(() -> service.consumeResetTokenAndApply(raw, (u, o) -> {
            u.setFullName("must roll back");
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        })).isInstanceOf(AppException.class);
        assertThat(users.findById(userId).orElseThrow().getFullName()).isEqualTo("OTP integration fixture");
        assertThat(service.consumeResetToken(raw)).isEqualTo(email);
    }

    @Test
    void smtpFailure_keepsTheCommittedOtpAndCooldown() {
        doThrow(new AppException(ErrorCode.MAIL_UNAVAILABLE)).when(mailer).send(eq(email), anyString(), anyString());
        assertCode(() -> service.issue(email, PURPOSE), ErrorCode.MAIL_UNAVAILABLE);
        EmailOtp row = current();
        assertThat(row.getId()).isNotNull();
        assertThatCode(() -> service.issue(email, PURPOSE)).doesNotThrowAnyException();
        assertThat(current().getId()).isEqualTo(row.getId());
    }

    @Test
    void blockedSmtp_doesNotHoldUserLockAndPreparationIsAlreadyCommitted() throws Exception {
        doAnswer(call -> { gate.pause(); return null; }).when(mailer).send(eq(email), anyString(), anyString());
        var sending = submit(() -> { service.issue(email, PURPOSE); return null; });
        gate.awaitReached();
        try {
            assertThat(current().getId()).isNotNull();
            var accountWrite = submit(() -> new TransactionTemplate(manager).execute(status -> {
                User locked = users.lockById(userId).orElseThrow();
                locked.setFullName("Changed during SMTP");
                return locked.getId();
            }));
            assertThat(accountWrite.get(5, TimeUnit.SECONDS).success()).isEqualTo(userId);
        } finally { gate.open(); }
        result(sending).success();
        assertThat(users.findById(userId).orElseThrow().getFullName()).isEqualTo("Changed during SMTP");
    }

    @Test
    void resend_invalidatesDetachedOtpAndItsResetToken() {
        fixture();
        String raw = exchange();
        EmailOtp detached = current();
        jdbc.update("update email_otps set created_at = ? where id = ?", LocalDateTime.now().minusMinutes(2), detached.getId());
        service.issue(email, PURPOSE);
        assertCode(() -> service.issueResetToken(detached), ErrorCode.OTP_EXPIRED);
        assertCode(() -> service.consumeResetToken(raw), ErrorCode.RESET_TOKEN_INVALID);
    }

    @Test
    void resetHashIsRecheckedAfterWaitingForTheUserLock() throws Exception {
        fixture();
        String raw = exchange();
        AtomicReference<CompletableFuture<Outcome<String>>> consuming = new AtomicReference<>();
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            users.lockById(userId).orElseThrow();
            consuming.set(submit(() -> service.consumeResetToken(raw)));
            awaitDatabaseWaitOrCompletion(consuming.get());
            jdbc.update("delete from email_otps where email = ?", email);
        });
        assertError(result(consuming.get()).failure(), ErrorCode.RESET_TOKEN_INVALID);
    }

    @Test
    void expiredResetTokenIsRejected_withoutExtendingItsDeadline() {
        fixture();
        String raw = exchange();
        jdbc.update("update email_otps set verified_at = ? where email = ?", LocalDateTime.now().minusMinutes(11), email);
        assertCode(() -> service.consumeResetToken(raw), ErrorCode.RESET_TOKEN_INVALID);
    }

    @Test
    void owningOperations_rejectAnEnclosingTransaction() {
        fixture();
        new TransactionTemplate(manager).executeWithoutResult(status -> {
            assertThatThrownBy(() -> service.issue(email, PURPOSE)).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> service.verifyAndApply(email, PURPOSE, "123456", (u, o) -> null))
                    .isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> service.consumeResetTokenAndApply("raw", (u, o) -> null))
                    .isInstanceOf(IllegalStateException.class);
        });
    }

    void fixture() {
        otps.saveAndFlush(EmailOtp.builder().email(email).purpose(PURPOSE).codeHash(encoder.encode("123456"))
                .attempts(0).createdAt(LocalDateTime.now().minusMinutes(2))
                .expiresAt(LocalDateTime.now().plusMinutes(5)).build());
    }
    EmailOtp current() { return otps.findFirstByEmailAndPurposeOrderByCreatedAtDesc(email, PURPOSE).orElseThrow(); }
    String exchange() { return service.verifyAndApply(email, PURPOSE, "123456", (u, o) -> service.issueResetToken(o)); }
    void pauseAfterFirstCurrentRead() {
        AtomicBoolean firstLookup = new AtomicBoolean(true);
        doAnswer(call -> {
            Object value = org.mockito.Mockito.mockingDetails(observedOtps).getMockCreationSettings()
                    .getDefaultAnswer().answer(call);
            // Hold the first transaction after its read; the competitor must serialize.
            if (firstLookup.getAndSet(false)) gate.pause();
            return value;
        }).when(observedOtps).findCurrentForUpdate(email, PURPOSE);
    }
    static void assertCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, ErrorCode expected) {
        assertThatThrownBy(call).isInstanceOfSatisfying(AppException.class, e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }
    static void assertError(Throwable failure, ErrorCode expected) {
        assertThat(failure).isInstanceOfSatisfying(AppException.class, e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }
    <T> CompletableFuture<Outcome<T>> submit(Supplier<T> action) {
        return CompletableFuture.supplyAsync(() -> {
            try { return new Outcome<>(action.get(), null); }
            catch (Throwable failure) { return new Outcome<>(null, failure); }
        }, executor);
    }
    static <T> Outcome<T> result(CompletableFuture<Outcome<T>> task) throws Exception { return task.get(20, TimeUnit.SECONDS); }
    record Outcome<T>(T value, Throwable failure) { T success() { assertThat(failure).isNull(); return value; } }
    void awaitDatabaseWaitOrCompletion(CompletableFuture<?> operation) {
        long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
        while (!operation.isDone()) {
            Integer waiting = jdbc.queryForObject("""
                    select count(*) from performance_schema.data_lock_waits w
                    join performance_schema.data_locks l
                      on l.ENGINE_LOCK_ID = w.REQUESTING_ENGINE_LOCK_ID and l.ENGINE = w.ENGINE
                    where l.OBJECT_SCHEMA = ?
                    """, Integer.class, SCHEMA);
            if (waiting > 0) return;
            if (System.nanoTime() > deadline) throw new AssertionError("Operation neither completed nor waited for a DB lock");
            Thread.yield();
        }
    }
    static class Gate {
        CountDownLatch reached = new CountDownLatch(1), release = new CountDownLatch(1);
        void pause() throws InterruptedException {
            reached.countDown();
            if (!release.await(20, TimeUnit.SECONDS)) throw new AssertionError("SMTP gate timed out");
        }
        void awaitReached() throws InterruptedException { assertThat(reached.await(15, TimeUnit.SECONDS)).isTrue(); }
        void open() { release.countDown(); }
    }
}
