package org.plishka.backend.service.notification;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.plishka.backend.exception.RetryableEmailException;
import org.plishka.backend.monitoring.metrics.EmailMetricsRecorder;
import org.plishka.backend.monitoring.sentry.SentryMonitoringService;
import org.plishka.backend.service.notification.email.EmailType;
import org.plishka.backend.service.notification.email.transport.AsyncEmailSender;
import org.plishka.backend.service.notification.email.transport.RetryableEmailSender;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AsyncEmailSenderTest {
    private static final String USER_EMAIL = "user@example.com";
    private static final String SUBJECT = "Підтвердження email";
    private static final String TEXT = "text body";

    @Mock
    private RetryableEmailSender retryableEmailSender;

    private SimpleMeterRegistry meterRegistry;
    private AsyncEmailSender asyncEmailSender;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        asyncEmailSender = new AsyncEmailSender(
                retryableEmailSender,
                new EmailMetricsRecorder(meterRegistry),
                new SentryMonitoringService()
        );
    }

    @Test
    void sendEmailAsync_ShouldSendEmail() {
        asyncEmailSender.sendEmailAsync(EmailType.VERIFICATION, USER_EMAIL, SUBJECT, TEXT);

        verify(retryableEmailSender).sendEmail(EmailType.VERIFICATION, USER_EMAIL, SUBJECT, TEXT);
    }

    @Test
    void sendEmailAsync_ShouldNotPropagateSendFailures() {
        doThrow(new RetryableEmailException("provider down"))
                .when(retryableEmailSender)
                .sendEmail(EmailType.VERIFICATION, USER_EMAIL, SUBJECT, TEXT);

        assertDoesNotThrow(() -> asyncEmailSender.sendEmailAsync(EmailType.VERIFICATION, USER_EMAIL, SUBJECT, TEXT));
        assertEquals(1.0, emailDeliveryCounter(EmailMetricsRecorder.OUTCOME_FAILED_RETRYABLE));
    }

    private double emailDeliveryCounter(String outcome) {
        return meterRegistry.get("email.delivery")
                .tag("email_type", EmailType.VERIFICATION.getMetricValue())
                .tag("outcome", outcome)
                .counter()
                .count();
    }
}
