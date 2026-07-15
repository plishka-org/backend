package org.plishka.backend.service.notification.email.transport;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.exception.NonRetryableEmailException;
import org.plishka.backend.exception.RetryableEmailException;
import org.plishka.backend.monitoring.metrics.EmailMetricsRecorder;
import org.plishka.backend.monitoring.sentry.SentryMonitoringService;
import org.plishka.backend.service.notification.email.EmailType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AsyncEmailSender {
    private final RetryableEmailSender retryableEmailSender;
    private final EmailMetricsRecorder emailMetricsRecorder;
    private final SentryMonitoringService sentryMonitoringService;

    @Async("emailTaskExecutor")
    public void sendEmailAsync(EmailType emailType, String to, String subject, String text) {
        try {
            retryableEmailSender.sendEmail(emailType, to, subject, text);
        } catch (NonRetryableEmailException exception) {
            emailMetricsRecorder.recordNonRetryableFailure(emailType);
            sentryMonitoringService.captureException(exception, "email", emailType.getMetricValue());
            log.error("Email delivery failed permanently: emailType={}", emailType.getMetricValue(), exception);
        } catch (RetryableEmailException exception) {
            emailMetricsRecorder.recordRetryableFailure(emailType);
            sentryMonitoringService.captureException(exception, "email", emailType.getMetricValue());
            log.error(
                    "Email delivery failed after retry handling: emailType={}",
                    emailType.getMetricValue(),
                    exception
            );
        } catch (Exception exception) {
            emailMetricsRecorder.recordNonRetryableFailure(emailType);
            sentryMonitoringService.captureException(exception, "email", emailType.getMetricValue());
            log.error("Unexpected email delivery failure: emailType={}", emailType.getMetricValue(), exception);
        }
    }
}
