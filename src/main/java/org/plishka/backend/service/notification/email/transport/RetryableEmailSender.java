package org.plishka.backend.service.notification.email.transport;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.exception.RetryableEmailException;
import org.plishka.backend.monitoring.metrics.EmailMetricsRecorder;
import org.plishka.backend.monitoring.sentry.SentryMonitoringService;
import org.plishka.backend.service.notification.email.EmailType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetryableEmailSender {
    private final EmailTransport emailTransport;
    private final EmailMetricsRecorder emailMetricsRecorder;
    private final SentryMonitoringService sentryMonitoringService;

    @Retryable(
            retryFor = RetryableEmailException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2.0)
    )
    public void sendEmail(EmailType emailType, String to, String subject, String text) {
        emailTransport.sendEmail(emailType, to, subject, text);
        emailMetricsRecorder.recordSent(emailType);

        log.info("Email successfully sent: emailType={}", emailType.getMetricValue());
    }

    @Recover
    public void recover(
            RetryableEmailException exception,
            EmailType emailType,
            String to,
            String subject,
            String text
    ) {
        emailMetricsRecorder.recordRetryableFailure(emailType);
        sentryMonitoringService.captureException(exception, "email", emailType.getMetricValue());
        log.error(
                "Email delivery failed after all retries: emailType={}",
                emailType.getMetricValue(),
                exception
        );
    }
}
