package org.plishka.backend.monitoring.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.domain.media.MediaTargetType;
import org.plishka.backend.domain.media.MediaType;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StorageMetricsRecorder {
    public static final String OUTCOME_FAILURE = "failure";
    public static final String OUTCOME_SUCCESS = "success";
    public static final String PROVIDER_TIGRIS = "tigris";

    private static final String UNKNOWN = "unknown";
    private static final Duration[] S3_OPERATION_DURATION_SLOS = {
            Duration.ofMillis(50),
            Duration.ofMillis(100),
            Duration.ofMillis(250),
            Duration.ofMillis(500),
            Duration.ofSeconds(1),
            Duration.ofSeconds(2),
            Duration.ofSeconds(5),
            Duration.ofSeconds(10)
    };

    private final MeterRegistry meterRegistry;

    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordPresign(
            Timer.Sample sample,
            String operation,
            MediaTargetType targetType,
            MediaType mediaType,
            String outcome
    ) {
        Counter.builder("storage.presign")
                .tag("operation", operation)
                .tag("target_type", metricValue(targetType))
                .tag("media_type", metricValue(mediaType))
                .tag("outcome", outcome)
                .register(meterRegistry)
                .increment();

        sample.stop(Timer.builder("storage.presign.duration")
                .tag("operation", operation)
                .tag("target_type", metricValue(targetType))
                .tag("media_type", metricValue(mediaType))
                .tag("outcome", outcome)
                .register(meterRegistry));
    }

    public void recordMediaAttach(MediaTargetType targetType, MediaType mediaType, String outcome) {
        Counter.builder("storage.media.attach")
                .tag("target_type", metricValue(targetType))
                .tag("media_type", metricValue(mediaType))
                .tag("outcome", outcome)
                .register(meterRegistry)
                .increment();
    }

    public void recordS3Operation(Timer.Sample sample, String operation, String outcome) {
        Counter.builder("storage.s3.operation")
                .tag("provider", PROVIDER_TIGRIS)
                .tag("operation", operation)
                .tag("outcome", outcome)
                .register(meterRegistry)
                .increment();

        sample.stop(Timer.builder("storage.s3.operation.duration")
                .tag("provider", PROVIDER_TIGRIS)
                .tag("operation", operation)
                .tag("outcome", outcome)
                .serviceLevelObjectives(S3_OPERATION_DURATION_SLOS)
                .register(meterRegistry));
    }

    public void recordDeletionOutboxEntries(int processedCount, int deletedCount, int failedCount) {
        increment("storage.deletion.outbox.entries", "processed", processedCount);
        increment("storage.deletion.outbox.entries", "deleted", deletedCount);
        increment("storage.deletion.outbox.entries", "failed", failedCount);
    }

    public void recordOrphanCleanup(int foundCount, int deletedCount, int skippedAttachedCount) {
        increment("storage.orphan.cleanup.objects", "found", foundCount);
        increment("storage.orphan.cleanup.objects", "deleted", deletedCount);
        increment("storage.orphan.cleanup.objects", "skipped_attached", skippedAttachedCount);
    }

    public void recordTagReconciliation(int checkedCount, int repairedCount, int failedCount) {
        increment("storage.tag.reconciliation.objects", "checked", checkedCount);
        increment("storage.tag.reconciliation.objects", "repaired", repairedCount);
        increment("storage.tag.reconciliation.objects", "failed", failedCount);
    }

    private void increment(String name, String outcome, int amount) {
        if (amount <= 0) {
            return;
        }

        Counter.builder(name)
                .tag("outcome", outcome)
                .register(meterRegistry)
                .increment(amount);
    }

    private String metricValue(MediaTargetType targetType) {
        return targetType == null ? UNKNOWN : targetType.name().toLowerCase(Locale.ROOT);
    }

    private String metricValue(MediaType mediaType) {
        return mediaType == null ? UNKNOWN : mediaType.name().toLowerCase(Locale.ROOT);
    }
}
