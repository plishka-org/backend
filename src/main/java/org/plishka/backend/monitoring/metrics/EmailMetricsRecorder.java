package org.plishka.backend.monitoring.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.service.notification.email.EmailType;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailMetricsRecorder {
    public static final String OUTCOME_FAILED_NON_RETRYABLE = "failed_non_retryable";
    public static final String OUTCOME_FAILED_RETRYABLE = "failed_retryable";
    public static final String OUTCOME_QUEUED = "queued";
    public static final String OUTCOME_REJECTED = "rejected";
    public static final String OUTCOME_SENT = "sent";

    private final MeterRegistry meterRegistry;

    public void recordQueued(EmailType emailType) {
        record(emailType, OUTCOME_QUEUED);
    }

    public void recordSent(EmailType emailType) {
        record(emailType, OUTCOME_SENT);
    }

    public void recordRejected(EmailType emailType) {
        record(emailType, OUTCOME_REJECTED);
    }

    public void recordRetryableFailure(EmailType emailType) {
        record(emailType, OUTCOME_FAILED_RETRYABLE);
    }

    public void recordNonRetryableFailure(EmailType emailType) {
        record(emailType, OUTCOME_FAILED_NON_RETRYABLE);
    }

    private void record(EmailType emailType, String outcome) {
        Counter.builder("email.delivery")
                .tag("email_type", emailType.getMetricValue())
                .tag("outcome", outcome)
                .register(meterRegistry)
                .increment();
    }
}
