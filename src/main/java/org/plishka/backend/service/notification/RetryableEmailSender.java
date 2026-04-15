package org.plishka.backend.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetryableEmailSender {
    private final JavaMailSender mailSender;

    @Retryable(
            retryFor = MailException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2.0)
    )
    public void sendEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);

            log.info("Email successfully sent: to={}, subject={}", to, subject);
        } catch (MailException exception) {
            log.warn(
                    "Email send attempt failed: to={}, subject={}",
                    to,
                    subject
            );
            throw exception;
        }
    }

    @Recover
    public void recover(MailException exception, String to, String subject) {
        log.error(
                "Email delivery failed after all retries: to={}, subject={}",
                to,
                subject,
                exception
        );
    }
}
