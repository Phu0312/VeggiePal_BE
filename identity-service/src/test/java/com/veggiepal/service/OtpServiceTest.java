package com.veggiepal.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Optional;
import com.veggiepal.entity.EmailOtp;
import com.veggiepal.entity.User;
import com.veggiepal.enums.OtpPurpose;
import com.veggiepal.exception.AppException;
import com.veggiepal.exception.ErrorCode;
import com.veggiepal.repository.EmailOtpRepository;
import com.veggiepal.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

class OtpServiceTest {
    static final String EMAIL = "an@example.com";
    EmailOtpRepository repository = mock(EmailOtpRepository.class);
    UserRepository users = mock(UserRepository.class);
    OtpMailer mailer = mock(OtpMailer.class);
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(4);
    PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
    OtpService service = new OtpService(repository, users, mailer, encoder, manager);

    @BeforeEach
    void setUp() {
        when(manager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(users.lockByEmail(EMAIL)).thenReturn(Optional.of(User.builder().id(7L).email(EMAIL).build()));
        when(repository.save(any())).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void issue_mailsSixDigitsAndPersistsOnlyBcrypt() {
        service.issue(EMAIL, OtpPurpose.RESET_PASSWORD);
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(mailer).send(eq(EMAIL), anyString(), code.capture());
        ArgumentCaptor<EmailOtp> row = ArgumentCaptor.forClass(EmailOtp.class);
        verify(repository).save(row.capture());
        assertThat(code.getValue()).matches("\\d{6}");
        assertThat(row.getValue().getCodeHash()).isNotEqualTo(code.getValue());
        assertThat(encoder.matches(code.getValue(), row.getValue().getCodeHash())).isTrue();
        assertThat(row.getValue().getExpiresAt()).isBetween(LocalDateTime.now().plusMinutes(9), LocalDateTime.now().plusMinutes(10));
    }

    @Test
    void expiredCode_neverRunsTheProtectedAction() {
        EmailOtp expired = EmailOtp.builder().codeHash(encoder.encode("123456"))
                .expiresAt(LocalDateTime.now().minusSeconds(1)).build();
        when(repository.findCurrentForUpdate(EMAIL, OtpPurpose.RESET_PASSWORD)).thenReturn(Optional.of(expired));
        assertThatThrownBy(() -> service.verifyAndApply(EMAIL, OtpPurpose.RESET_PASSWORD, "123456",
                (user, otp) -> { throw new AssertionError("Expired code authorized action"); }))
                .isInstanceOfSatisfying(AppException.class,
                        e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.OTP_EXPIRED));
    }
}
