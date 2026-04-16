package org.plishka.backend.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.exception.ResendEmailException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetryableEmailSender {
    private final ResendEmailClient resendEmailClient;

    @Retryable(
            retryFor = ResendEmailException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2.0)
    )
    public void sendEmail(String to, String subject, String text) {
        resendEmailClient.sendEmail(to, subject, text);

        log.info("Email successfully sent: to={}, subject={}", to, subject);
    }

    @Recover
    public void recover(ResendEmailException exception, String to, String subject) {
        log.error(
                "Email delivery failed after all retries: to={}, subject={}",
                to,
                subject,
                exception
        );
    }
}
