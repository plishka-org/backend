package org.plishka.backend.service.notification.email.transport;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AsyncEmailSender {
    private final RetryableEmailSender retryableEmailSender;

    @Async("emailTaskExecutor")
    public void sendEmailAsync(String to, String subject, String text) {
        try {
            retryableEmailSender.sendEmail(to, subject, text);
        } catch (Exception exception) {
            log.error(
                    "Failed to send email: to={}, subject={}",
                    to,
                    subject,
                    exception
            );
        }
    }
}
