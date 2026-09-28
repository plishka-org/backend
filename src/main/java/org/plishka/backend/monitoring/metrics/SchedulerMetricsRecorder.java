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

    public void recordJob(String schedulerJob, Runnable action) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            action.run();
            transactionalMetricsPublisher.afterCompletionOrNow(
                    () -> recordCompletedJob(sample, schedulerJob, OUTCOME_SUCCESS),
                    () -> recordCompletedJob(sample, schedulerJob, OUTCOME_FAILURE)
            );
        } catch (RuntimeException exception) {
            recordCompletedJob(sample, schedulerJob, OUTCOME_FAILURE);
            sentryMonitoringService.captureException(exception, "scheduler", schedulerJob);
            throw exception;
        }
    }

    private void recordCompletedJob(Timer.Sample sample, String schedulerJob, String outcome) {
        counter(schedulerJob, outcome).increment();
        sample.stop(Timer.builder("scheduler.job.duration")
                .tag("scheduler_job", schedulerJob)
                .tag("outcome", outcome)
                .register(meterRegistry));
    }

    private Counter counter(String schedulerJob, String outcome) {
        return Counter.builder("scheduler.job")
                .tag("scheduler_job", schedulerJob)
                .tag("outcome", outcome)
                .register(meterRegistry);
    }
}
