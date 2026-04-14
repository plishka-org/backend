package org.plishka.backend.service.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AsyncEmailSender {
    private final RetryableEmailSender retryableEmailSender;

    @Async
    public void sendEmailAsync(String to, String subject, String text) {
        retryableEmailSender.sendEmail(to, subject, text);
    }
}
