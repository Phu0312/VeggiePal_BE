package com.veggiepal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import com.veggiepal.dto.request.ChangePasswordRequest;
import com.veggiepal.dto.request.LoginRequest;
import com.veggiepal.dto.request.UpdateProfileRequest;
import com.veggiepal.mapper.UserMapper;
import org.springframework.mock.web.MockMultipartFile;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import com.veggiepal.dto.response.LoginResponse;
import com.veggiepal.entity.User;
import com.veggiepal.enums.Role;
import com.veggiepal.enums.UserStatus;
import com.veggiepal.exception.AppException;
import com.veggiepal.exception.ErrorCode;
import com.veggiepal.repository.RefreshTokenRepository;
import com.veggiepal.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Real MySQL transactions; spies only coordinate calls, never replace persistence. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:mysql://localhost:${DB_PORT:3307}/veggiepal_identity_session_it?createDatabaseIfNotExist=true",
        "spring.datasource.username=${DB_USERNAME:root}",
        "spring.datasource.password=${DB_PASSWORD:12345}",
        "spring.datasource.hikari.connection-init-sql=SET SESSION innodb_lock_wait_timeout = 10",
        "spring.jpa.hibernate.ddl-auto=update",
        "spring.jpa.show-sql=false",
        "spring.sql.init.mode=never",
        "spring.config.import=",
        "jwt.secret=veggiepal-secret-key-must-be-at-least-32-characters"
})
class SessionConcurrencyIntegrationTests {

    static final String SCHEMA = "veggiepal_identity_session_it";
    static final String OLD_PASSWORD = "old-session-password";
    static final String NEW_PASSWORD = "new-session-password";

    @Autowired UserService userService;
    @Autowired ProfileService profileService;
    @Autowired AdminUserService adminService;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactionManager;
    @MockitoSpyBean RefreshTokenService tokens;
    @MockitoSpyBean RefreshTokenRepository tokenRepository;
    @MockitoBean GoogleTokenVerifier googleTokenVerifier;
    @MockitoBean FileStorageService storage;
    @MockitoSpyBean UserMapper mapper;
    @MockitoSpyBean UserRepository observedUsers;

    ExecutorService executor;
    Gate gate;
    Long userId;
    String email;
    String raw;
    java.util.List<String> extraEmails = new java.util.ArrayList<>();

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(2);
        gate = new Gate();
        email = "session-it-" + UUID.randomUUID() + "@veggiepal.invalid";
        userId = users.saveAndFlush(User.builder().email(email).fullName("Session integration fixture")
                .passwordHash(encoder.encode(OLD_PASSWORD)).role(Role.USER).status(UserStatus.ACTIVE)
                .emailVerified(true).build()).getId();
        raw = tokens.issue(userId);
        when(googleTokenVerifier.verify("google-fixture")).thenReturn(Jwt.withTokenValue("google-fixture")
                .header("alg", "RS256").claim("email", email).claim("email_verified", true).build());
    }

    @AfterEach
    void cleanUp() throws Exception {
        gate.open();
        executor.shutdown();
        assertThat(executor.awaitTermination(25, TimeUnit.SECONDS)).isTrue();
        // Only this test's rows, never DELETE ALL or drop a schema.
        if (userId != null) {
            jdbc.update("delete from refresh_tokens where user_id = ?", userId);
            jdbc.update("delete from users where id = ? and email = ?", userId, email);
        }
        for (String fixtureEmail : extraEmails) {
            users.findByEmail(fixtureEmail).ifPresent(user -> {
                jdbc.update("delete from refresh_tokens where user_id = ?", user.getId());
                jdbc.update("delete from users where id = ? and email = ?", user.getId(), fixtureEmail);
            });
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void avatarUpload_preservesConcurrentBanOrRoleAndPasswordChange(boolean promote) throws Exception {
        setAvatar("old-avatar");
        doAnswer(call -> { gate.pause(); return "new-avatar"; }).when(storage).upload(anyString(), any(), anyString());
        var avatar = submit(() -> profileService.uploadAvatar(userId, avatarFile()));
        gate.awaitReached();
        // These actual transactions must finish while the network boundary is still paused.
        result(submit(() -> {
            changePassword();
            if (promote) adminService.changeRole(-1L, userId, Role.ADMIN);
            else adminService.lock(-1L, userId, "fixture ban");
            return null;
        })).success();
        gate.open();
        result(avatar).success();

        User current = users.findById(userId).orElseThrow();
        assertThat(current.getAvatarUrl()).isEqualTo("new-avatar");
        assertThat(encoder.matches(NEW_PASSWORD, current.getPasswordHash())).isTrue();
        assertThat(encoder.matches(OLD_PASSWORD, current.getPasswordHash())).isFalse();
        assertThat(current.getEmailVerified()).isTrue();
        if (promote) assertThat(current.getRole()).isEqualTo(Role.ADMIN);
        else assertBan(current);
        assertThat(sessionCount()).isZero();
        assertError(loginWithOldPassword().failure(), ErrorCode.UNAUTHENTICATED);
        verify(storage).delete("old-avatar");
    }

    @Test
    void profileMapping_serializesWithAdminBanAndPasswordChange() throws Exception {
        doAnswer(call -> { gate.pause(); return call.callRealMethod(); })
                .when(mapper).updateProfile(any(User.class), any(UpdateProfileRequest.class));
        var patch = submit(() -> profileService.updateProfile(userId,
                UpdateProfileRequest.builder().fullName("Updated fixture").build()));
        gate.awaitReached();
        var mutation = submit(() -> {
            changePassword();
            adminService.lock(-1L, userId, "fixture ban");
            return null;
        });
        try { awaitDatabaseWaitOrCompletion(mutation); }
        finally { gate.open(); }
        result(patch).success();
        result(mutation).success();
        User current = users.findById(userId).orElseThrow();
        assertThat(current.getFullName()).isEqualTo("Updated fixture");
        assertThat(encoder.matches(NEW_PASSWORD, current.getPasswordHash())).isTrue();
        assertBan(current);
        assertError(loginWithOldPassword().failure(), ErrorCode.UNAUTHENTICATED);
    }

    @Test
    void avatarCommitFailure_deletesNewUploadAndPreservesOldAvatar() {
        setAvatar("old-avatar");
        String tooLong = "x".repeat(300);
        when(storage.upload(anyString(), any(), anyString())).thenReturn(tooLong);
        assertThatThrownBy(() -> profileService.uploadAvatar(userId, avatarFile()))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(users.findById(userId).orElseThrow().getAvatarUrl()).isEqualTo("old-avatar");
        verify(storage).delete(tooLong);
        verify(storage, never()).delete("old-avatar");
    }

    void setAvatar(String url) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            User user = users.lockById(userId).orElseThrow();
            user.setAvatarUrl(url);
        });
    }

    MockMultipartFile avatarFile() {
        return new MockMultipartFile("file", "fixture.png", "image/png", ProfileServiceTest.PNG_BYTES);
    }

    Outcome<LoginResponse> loginWithOldPassword() {
        try { return new Outcome<>(userService.login(LoginRequest.builder().email(email).password(OLD_PASSWORD).build()), null); }
        catch (Throwable failure) { return new Outcome<>(null, failure); }
    }

    void assertBan(User user) {
        assertThat(user.getStatus()).isEqualTo(UserStatus.BLOCKED);
        assertThat(user.getLockedReason()).isEqualTo("fixture ban");
        assertThat(user.getLockedAt()).isNotNull();
        assertThat(user.getLockedBy()).isEqualTo(-1L);
    }

    @Test
    void distinctNewGoogleAccounts_inSameEmailGap_bothCommitUsableSessions() throws Exception {
        String prefix = "zz-google-it-" + UUID.randomUUID();
        String lower = prefix + "-0@veggiepal.invalid";
        String upper = prefix + "-9@veggiepal.invalid";
        String firstEmail = prefix + "-3@veggiepal.invalid";
        String secondEmail = prefix + "-6@veggiepal.invalid";
        extraEmails.addAll(java.util.List.of(lower, upper, firstEmail, secondEmail));
        for (String boundary : java.util.List.of(lower, upper)) {
            users.saveAndFlush(User.builder().email(boundary).fullName("Gap boundary fixture")
                    .passwordHash(encoder.encode(OLD_PASSWORD)).role(Role.USER).status(UserStatus.ACTIVE)
                    .emailVerified(true).build());
        }
        CyclicBarrier emptyReads = new CyclicBarrier(2);
        for (String fixtureEmail : java.util.List.of(firstEmail, secondEmail)) {
            when(googleTokenVerifier.verify(fixtureEmail)).thenReturn(Jwt.withTokenValue(fixtureEmail)
                    .header("alg", "RS256").claim("email", fixtureEmail).claim("email_verified", true).build());
            doAnswer(call -> {
                Object found = org.mockito.Mockito.mockingDetails(observedUsers).getMockCreationSettings()
                        .getDefaultAnswer().answer(call);
                assertThat((java.util.Optional<?>) found).isEmpty();
                emptyReads.await(15, TimeUnit.SECONDS);
                return found;
            }).when(observedUsers).lockByEmail(fixtureEmail);
        }
        var first = submit(() -> userService.loginWithGoogle(firstEmail));
        var second = submit(() -> userService.loginWithGoogle(secondEmail));
        Outcome<LoginResponse> one = result(first);
        Outcome<LoginResponse> two = result(second);
        assertThat(one.failure()).as("first creation; second failure=%s", two.failure()).isNull();
        assertThat(two.failure()).as("second creation").isNull();
        for (LoginResponse response : java.util.List.of(one.value(), two.value())) {
            assertThat(users.findById(response.getUserId()).orElseThrow().getEmailVerified()).isTrue();
            assertThat(userService.refresh(response.getRefreshToken()).getUserId()).isEqualTo(response.getUserId());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void issuanceBeforePasswordChange_cannotLeaveASuccessorSession(boolean googleLogin) throws Exception {
        AtomicBoolean first = new AtomicBoolean(true);
        doAnswer(call -> {
            if (first.getAndSet(false)) gate.pause();
            return call.callRealMethod();
        }).when(tokens).issue(userId);

        CompletableFuture<Outcome<LoginResponse>> issuance = submit(() -> googleLogin
                ? userService.loginWithGoogle("google-fixture") : userService.refresh(raw));
        gate.awaitReached();
        CompletableFuture<Outcome<Void>> change = submit(() -> { changePassword(); return null; });
        try {
            awaitDatabaseWaitOrCompletion(change);
        } finally {
            gate.open();
        }

        LoginResponse response = result(issuance).success();
        result(change).success();
        assertThat(sessionCount()).isZero();
        assertInvalidRefresh(response.getRefreshToken());
    }

    @Test
    void passwordChangeBeforeRefresh_rejectsTheOldSessionWith1027() throws Exception {
        doAnswer(call -> {
            Object result = call.callRealMethod();
            gate.pause();
            return result;
        }).when(tokens).revokeAll(userId);

        CompletableFuture<Outcome<Void>> change = submit(() -> { changePassword(); return null; });
        gate.awaitReached();
        CompletableFuture<Outcome<LoginResponse>> refresh = submit(() -> userService.refresh(raw));
        try {
            awaitDatabaseWaitOrCompletion(refresh);
        } finally {
            gate.open();
        }

        result(change).success();
        assertError(result(refresh).failure(), ErrorCode.REFRESH_TOKEN_INVALID);
        assertThat(sessionCount()).isZero();
    }

    @Test
    void twoRequestsForOneToken_haveOneSuccessAndOne1027() throws Exception {
        CyclicBarrier bothReadOldToken = new CyclicBarrier(2);
        doAnswer(call -> {
            // Spring Data exposes an interface proxy, so use the spy's delegate to run its real query.
            Object result = org.mockito.Mockito.mockingDetails(tokenRepository).getMockCreationSettings()
                    .getDefaultAnswer().answer(call);
            bothReadOldToken.await(15, TimeUnit.SECONDS);
            return result;
        }).when(tokenRepository).findUserIdByTokenHash(RefreshTokenService.sha256(raw));

        var first = submit(() -> userService.refresh(raw));
        var second = submit(() -> userService.refresh(raw));
        Outcome<LoginResponse> one = result(first);
        Outcome<LoginResponse> two = result(second);

        assertThat(java.util.stream.Stream.of(one, two).filter(o -> o.failure() == null).count())
                .as("first=%s, second=%s", one, two).isEqualTo(1);
        assertError(one.failure() == null ? two.failure() : one.failure(), ErrorCode.REFRESH_TOKEN_INVALID);
        assertThat(sessionCount()).isEqualTo(1);
    }

    @Test
    void revocationFailure_rollsBackThePasswordChange() {
        doThrow(new IllegalStateException("fixture revocation failure")).when(tokens).revokeAll(userId);

        assertThatThrownBy(this::changePassword).isInstanceOf(IllegalStateException.class)
                .hasMessage("fixture revocation failure");

        assertThat(encoder.matches(OLD_PASSWORD, users.findById(userId).orElseThrow().getPasswordHash())).isTrue();
        assertThat(sessionCount()).isEqualTo(1);
    }

    @Test
    void googleLinking_unverifiedAccount_revokesOldSessionAndIssuesUsableOwnerSession() {
        markUnverified();

        LoginResponse google = userService.loginWithGoogle("google-fixture");

        assertInvalidRefresh(raw);
        LoginResponse renewed = userService.refresh(google.getRefreshToken());
        assertThat(renewed.getUserId()).isEqualTo(userId);
        assertThat(renewed.getRefreshToken()).isNotBlank().isNotEqualTo(google.getRefreshToken());
        assertThat(sessionCount()).isEqualTo(1);
    }

    @Test
    void googleLinking_verifiedAccount_preservesItsExistingSession() {
        LoginResponse google = userService.loginWithGoogle("google-fixture");

        assertThat(userService.refresh(raw).getUserId()).isEqualTo(userId);
        assertThat(userService.refresh(google.getRefreshToken()).getUserId()).isEqualTo(userId);
        assertThat(sessionCount()).isEqualTo(2);
    }

    @Test
    void googleLinking_revocationFailure_rollsBackPasswordAndVerification() {
        markUnverified();
        doThrow(new IllegalStateException("fixture linking revocation failure")).when(tokens).revokeAll(userId);

        assertThatThrownBy(() -> userService.loginWithGoogle("google-fixture"))
                .isInstanceOf(IllegalStateException.class).hasMessage("fixture linking revocation failure");

        User unchanged = users.findById(userId).orElseThrow();
        assertThat(unchanged.getEmailVerified()).isFalse();
        assertThat(encoder.matches(OLD_PASSWORD, unchanged.getPasswordHash())).isTrue();
        assertThat(sessionCount()).isEqualTo(1);
        assertThat(userService.refresh(raw).getUserId()).isEqualTo(userId);
    }

    void markUnverified() {
        User user = users.findById(userId).orElseThrow();
        user.setEmailVerified(false);
        users.saveAndFlush(user);
    }

    @Test
    void loginWaitingForPasswordChange_checksTheCommittedPassword() throws Exception {
        AtomicReference<CompletableFuture<Outcome<LoginResponse>>> login = new AtomicReference<>();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            User locked = users.lockById(userId).orElseThrow();
            locked.setPasswordHash(encoder.encode(NEW_PASSWORD));
            users.saveAndFlush(locked);
            login.set(submit(() -> userService.login(LoginRequest.builder()
                    .email(email).password(OLD_PASSWORD).build())));
            awaitDatabaseWaitOrCompletion(login.get());
        });

        assertError(result(login.get()).failure(), ErrorCode.UNAUTHENTICATED);
        assertThat(sessionCount()).isEqualTo(1);
    }

    void changePassword() {
        profileService.changePassword(userId, new ChangePasswordRequest(OLD_PASSWORD, NEW_PASSWORD));
    }

    long sessionCount() {
        return jdbc.queryForObject("select count(*) from refresh_tokens where user_id = ?", Long.class, userId);
    }

    void assertInvalidRefresh(String token) {
        assertThatThrownBy(() -> userService.refresh(token)).isInstanceOfSatisfying(AppException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.REFRESH_TOKEN_INVALID));
    }

    static void assertError(Throwable error, ErrorCode expected) {
        assertThat(error).isInstanceOfSatisfying(AppException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(expected));
    }

    // Wait for observable database contention OR completed competing work, never a scheduling sleep.
    // On the broken code the competing operation can finish; with locking it waits in InnoDB.
    void awaitDatabaseWaitOrCompletion(CompletableFuture<?> operation) {
        long deadline = System.nanoTime() + Duration.ofSeconds(15).toNanos();
        while (!operation.isDone()) {
            Integer waiters = jdbc.queryForObject("""
                    select count(*) from performance_schema.data_lock_waits w
                    join performance_schema.data_locks l
                      on l.ENGINE_LOCK_ID = w.REQUESTING_ENGINE_LOCK_ID and l.ENGINE = w.ENGINE
                    where l.OBJECT_SCHEMA = ?
                    """, Integer.class, SCHEMA);
            if (waiters > 0) return;
            if (System.nanoTime() > deadline) throw new AssertionError("Competing operation neither completed nor waited for a DB lock");
            Thread.yield();
        }
    }

    <T> CompletableFuture<Outcome<T>> submit(Supplier<T> action) {
        return CompletableFuture.supplyAsync(() -> {
            try { return new Outcome<>(action.get(), null); }
            catch (Throwable failure) { return new Outcome<>(null, failure); }
        }, executor);
    }

    static <T> Outcome<T> result(CompletableFuture<Outcome<T>> task) throws Exception {
        return task.get(20, TimeUnit.SECONDS);
    }

    record Outcome<T>(T value, Throwable failure) {
        T success() {
            assertThat(failure).isNull();
            return value;
        }
    }

    static class Gate {
        final CountDownLatch reached = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        void pause() throws InterruptedException {
            reached.countDown();
            if (!release.await(20, TimeUnit.SECONDS)) throw new AssertionError("Boundary gate timed out");
        }
        void awaitReached() throws InterruptedException {
            assertThat(reached.await(15, TimeUnit.SECONDS)).isTrue();
        }
        void open() { release.countDown(); }
    }
}
