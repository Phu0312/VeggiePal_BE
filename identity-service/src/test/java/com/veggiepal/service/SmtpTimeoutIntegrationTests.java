package com.veggiepal.service;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;
import java.util.concurrent.*;

import com.veggiepal.entity.User;
import com.veggiepal.enums.Role;
import com.veggiepal.enums.UserStatus;
import com.veggiepal.exception.AppException;
import com.veggiepal.exception.ErrorCode;
import com.veggiepal.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** Real configured SMTP transport, stalled loopback peer and isolated MySQL fixtures. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:mysql://localhost:${DB_PORT:3307}/veggiepal_identity_otp_it?createDatabaseIfNotExist=true",
        "spring.datasource.username=${DB_USERNAME:root}", "spring.datasource.password=${DB_PASSWORD:12345}",
        "spring.jpa.hibernate.ddl-auto=update", "spring.jpa.show-sql=false",
        "spring.sql.init.mode=never", "spring.config.import=",
        "jwt.secret=veggiepal-secret-key-must-be-at-least-32-characters",
        "spring.mail.host=127.0.0.1", "spring.mail.username=", "spring.mail.password=",
        "spring.mail.properties.mail.smtp.auth=false", "spring.mail.properties.mail.smtp.starttls.enable=false",
        "mail.from=fixture@veggiepal.invalid", "otp.log-codes=false"
})
@AutoConfigureMockMvc
class SmtpTimeoutIntegrationTests {
    static final ServerSocket SERVER = openServer();
    @DynamicPropertySource
    static void smtpPort(DynamicPropertyRegistry registry) {
        registry.add("spring.mail.port", SERVER::getLocalPort);
    }
    @Autowired JavaMailSenderImpl sender;
    @Autowired OtpMailer mailer;
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired JdbcTemplate jdbc;
    ExecutorService executor;
    Future<Socket> accepted;
    String email;

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(2);
        accepted = executor.submit(() -> SERVER.accept());
        email = "smtp-it-" + UUID.randomUUID() + "@veggiepal.invalid";
    }

    @AfterEach
    void cleanUp() throws Exception {
        // Closing the peer also releases a broken infinite-timeout sender after RED.
        if (accepted.isDone()) accepted.get().close();
        else accepted.cancel(true);
        executor.shutdownNow();
        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        jdbc.update("delete from email_otps where email = ?", email);
        users.findByEmail(email).ifPresent(user -> {
            jdbc.update("delete from refresh_tokens where user_id = ?", user.getId());
            jdbc.update("delete from users where id = ? and email = ?", user.getId(), email);
        });
    }

    @AfterAll
    static void closeServer() throws Exception { SERVER.close(); }

    @Test
    void configuredSender_stalledGreeting_mapsTo1031WithinDeadline() throws Exception {
        Future<?> delivery = executor.submit(() -> mailer.send(email, "Fixture", "123456"));
        accepted.get(3, TimeUnit.SECONDS);
        assertThatThrownBy(() -> delivery.get(8, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(AppException.class)
                .satisfies(failure -> assertThat(((AppException) failure.getCause()).getErrorCode())
                        .isEqualTo(ErrorCode.MAIL_UNAVAILABLE));
        for (String key : java.util.List.of("connectiontimeout", "timeout", "writetimeout")) {
            assertThat(sender.getJavaMailProperties().getProperty("mail.smtp." + key)).isEqualTo("5000");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"register", "resend-verification", "forgot-password"})
    void publicSend_stalledPeer_returns200WithCommittedProofAndCooldown(String endpoint) throws Exception {
        if (!endpoint.equals("register")) {
            users.saveAndFlush(User.builder().email(email).fullName("SMTP fixture").passwordHash("fixture-unused")
                    .role(Role.USER).status(UserStatus.PENDING).emailVerified(false).build());
        }
        String body = endpoint.equals("register")
                ? "{\"email\":\"" + email + "\",\"password\":\"secret123\",\"fullName\":\"SMTP fixture\"}"
                : "{\"email\":\"" + email + "\"}";
        Future<?> request = executor.submit(() -> {
            mvc.perform(post("/auth/" + endpoint).contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(1000));
            return null;
        });
        accepted.get(3, TimeUnit.SECONDS);
        // Another connection observes the committed data while SMTP still waits for a greeting.
        assertThat(users.findByEmail(email).orElseThrow().getStatus()).isEqualTo(UserStatus.PENDING);
        String purpose = endpoint.equals("forgot-password") ? "RESET_PASSWORD" : "VERIFY_EMAIL";
        Long proofId = jdbc.queryForObject("select id from email_otps where email = ? and purpose = ?",
                Long.class, email, purpose);
        assertThat(proofId).isNotNull();
        assertThat(request.isDone()).isFalse();
        request.get(8, TimeUnit.SECONDS);
        mvc.perform(post("/auth/" + (endpoint.equals("register") ? "resend-verification" : endpoint))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"" + email + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(1000));
        assertThat(jdbc.queryForObject("select id from email_otps where email = ? and purpose = ?",
                Long.class, email, purpose)).isEqualTo(proofId);
    }

    static ServerSocket openServer() {
        try { return new ServerSocket(0, 10, InetAddress.getByName("127.0.0.1")); }
        catch (java.io.IOException failure) { throw new ExceptionInInitializerError(failure); }
    }
}
