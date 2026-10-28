package com.veggiepal.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.veggiepal.exception.AppException;
import com.veggiepal.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class OtpMailerTest {
    @SuppressWarnings("unchecked")
    ObjectProvider<JavaMailSender> provider(JavaMailSender sender) {
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(sender);
        return provider;
    }

    @Test
    void noSmtp_andLoggingOff_failsClosed() {
        assertUnavailable(() -> new OtpMailer(provider(null), "", "", false)
                .send("an@example.com", "subject", "123456"));
    }

    @Test
    void noSmtp_andLoggingOn_onlyLogs() {
        assertThatCode(() -> new OtpMailer(provider(null), "", "", true)
                .send("an@example.com", "subject", "123456")).doesNotThrowAnyException();
    }

    @Test
    void configuredSmtp_sendsTheCodeToTheRecipient() {
        JavaMailSender sender = mock(JavaMailSender.class);
        new OtpMailer(provider(sender), "smtp.example.com", "sender@example.com", false)
                .send("an@example.com", "Verify email", "123456");
        ArgumentCaptor<SimpleMailMessage> sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(sent.capture());
        assertThat(sent.getValue().getTo()).containsExactly("an@example.com");
        assertThat(sent.getValue().getFrom()).isEqualTo("sender@example.com");
        assertThat(sent.getValue().getSubject()).isEqualTo("Verify email");
        assertThat(sent.getValue().getText()).contains("123456", "10 phút");
    }

    @Test
    void smtpFailure_mapsToMailUnavailable() {
        JavaMailSender sender = mock(JavaMailSender.class);
        doThrow(new MailSendException("fixture delivery failure")).when(sender).send(any(SimpleMailMessage.class));
        assertUnavailable(() -> new OtpMailer(provider(sender), "smtp.example.com", "", false)
                .send("an@example.com", "subject", "123456"));
    }

    void assertUnavailable(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call).isInstanceOfSatisfying(AppException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.MAIL_UNAVAILABLE));
    }
}
