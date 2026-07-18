package org.plishka.backend.monitoring.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.monitoring.sentry.SentryMonitoringService;
import org.plishka.backend.monitoring.transaction.TransactionalMetricsPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SchedulerMetricsRecorder {
    private static final String OUTCOME_FAILURE = "failure";
    private static final String OUTCOME_SUCCESS = "success";

    private final MeterRegistry meterRegistry;
    private final SentryMonitoringService sentryMonitoringService;
    private final TransactionalMetricsPublisher transactionalMetricsPublisher;

    public void recordJob(String job, Runnable action) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            action.run();
            transactionalMetricsPublisher.afterCompletionOrNow(
                    () -> recordCompletedJob(sample, job, OUTCOME_SUCCESS),
                    () -> recordCompletedJob(sample, job, OUTCOME_FAILURE)
            );
        } catch (RuntimeException exception) {
            recordCompletedJob(sample, job, OUTCOME_FAILURE);
            sentryMonitoringService.captureException(exception, "scheduler", job);
            throw exception;
        }
    }

    private void recordCompletedJob(Timer.Sample sample, String job, String outcome) {
        counter(job, outcome).increment();
        sample.stop(Timer.builder("scheduler.job.duration")
                .tag("job", job)
                .tag("outcome", outcome)
                .register(meterRegistry));
    }

    private Counter counter(String job, String outcome) {
        return Counter.builder("scheduler.job")
                .tag("job", job)
                .tag("outcome", outcome)
                .register(meterRegistry);
    }
}
