package com.veggiepal.service;

import com.veggiepal.exception.AppException;
import com.veggiepal.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Plain text email; code logging is explicitly opt-in for local development. */
@Component
@Slf4j
public class OtpMailer {
    private final ObjectProvider<JavaMailSender> mailSender;
    private final String host;
    private final String from;
    private final boolean logCodes;

    public OtpMailer(ObjectProvider<JavaMailSender> mailSender, @Value("${spring.mail.host:}") String host,
                     @Value("${mail.from:}") String from, @Value("${otp.log-codes:false}") boolean logCodes) {
        this.mailSender = mailSender;
        this.host = host;
        this.from = from;
        this.logCodes = logCodes;
    }

    public void send(String to, String subject, String code) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (!StringUtils.hasText(host) || sender == null) {
            if (logCodes) {
                log.warn("DEV ONLY, no SMTP configured: OTP for {} is {}", to, code);
                return;
            }
            throw new AppException(ErrorCode.MAIL_UNAVAILABLE);
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        if (StringUtils.hasText(from)) message.setFrom(from);
        message.setSubject(subject);
        message.setText("Mã xác nhận VeggiePal của bạn là: " + code
                + "\n\nMã có hiệu lực trong 10 phút. Nếu bạn không yêu cầu, hãy bỏ qua email này.");
        try {
            sender.send(message);
        } catch (MailException exception) {
            log.error("Could not send OTP mail to {}", to, exception);
            throw new AppException(ErrorCode.MAIL_UNAVAILABLE);
        }
    }
}
