package org.plishka.backend.config;

import com.github.benmanes.caffeine.cache.Cache;
import io.github.bucket4j.Bucket;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.config.MeterFilter;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import org.plishka.backend.domain.storage.StorageDeletionOutboxStatus;
import org.plishka.backend.monitoring.http.HttpAreaObservationConvention;
import org.plishka.backend.monitoring.metrics.CatalogMetricsSnapshot;
import org.plishka.backend.repository.storage.StorageDeletionOutboxRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.observation.ServerRequestObservationConvention;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class MonitoringConfig {
    private static final String HTTP_SERVER_REQUESTS = "http.server.requests";

    @Bean
    public ServerRequestObservationConvention serverRequestObservationConvention() {
        return new HttpAreaObservationConvention();
    }

    @Bean
    public MeterFilter denyInternalHttpMetrics() {
        return MeterFilter.deny(this::isIgnoredHttpMetric);
    }

    @Bean
    public MeterBinder emailTaskExecutorMetrics(
            @Qualifier("emailTaskExecutor") ThreadPoolTaskExecutor emailTaskExecutor
    ) {
        return registry -> {
            Gauge.builder("email.task.executor.queue.size", emailTaskExecutor, this::emailQueueSize)
                    .description("Queued tasks in emailTaskExecutor")
                    .register(registry);
            Gauge.builder("email.task.executor.active", emailTaskExecutor, ThreadPoolTaskExecutor::getActiveCount)
                    .description("Active threads in emailTaskExecutor")
                    .register(registry);
            Gauge.builder("email.task.executor.pool.size", emailTaskExecutor, ThreadPoolTaskExecutor::getPoolSize)
                    .description("Current pool size of emailTaskExecutor")
                    .register(registry);
        };
    }

    @Bean
    public MeterBinder rateLimitCacheMetrics(Cache<String, Bucket> rateLimitBucketCache) {
        return registry -> Gauge.builder("rate.limit.bucket.cache.size", rateLimitBucketCache, Cache::estimatedSize)
                .description("Estimated number of rate limit bucket cache entries")
                .register(registry);
    }

    @Bean
    public MeterBinder storageDeletionOutboxGauges(
            StorageDeletionOutboxRepository repository,
            Clock clock
    ) {
        return registry -> {
            Gauge.builder("storage.deletion.outbox.pending", repository, this::pendingOutboxCount)
                    .description("Pending storage deletion outbox entries")
                    .register(registry);
            Gauge.builder("storage.deletion.outbox.due", repository, repo -> dueOutboxCount(repo, clock))
                    .description("Due storage deletion outbox entries")
                    .register(registry);
            Gauge.builder("storage.deletion.outbox.failed", repository, this::failedOutboxCount)
                    .description("Failed storage deletion outbox entries")
                    .register(registry);
            Gauge.builder("storage.deletion.outbox.oldest.pending.age", repository,
                            repo -> oldestPendingAgeSeconds(repo, clock))
                    .baseUnit("seconds")
                    .description("Age of the oldest pending storage deletion outbox entry")
                    .register(registry);
        };
    }

    @Bean
    public MeterBinder catalogGauges(CatalogMetricsSnapshot snapshot) {
        return registry -> {
            Gauge.builder("shop.mode.enabled", snapshot, CatalogMetricsSnapshot::shopModeEnabled)
                    .description("1 when shop mode is enabled, otherwise 0")
                    .register(registry);
            Gauge.builder("catalog.visible.products", snapshot, CatalogMetricsSnapshot::visibleProducts)
                    .description("Visible product count")
                    .register(registry);
            Gauge.builder("catalog.visible.categories", snapshot, CatalogMetricsSnapshot::visibleCategories)
                    .description("Visible category count")
                    .register(registry);
            Gauge.builder("catalog.visible.reviews", snapshot, CatalogMetricsSnapshot::visibleReviews)
                    .description("Visible review count")
                    .register(registry);
        };
    }

    private boolean isIgnoredHttpMetric(Meter.Id id) {
        return HTTP_SERVER_REQUESTS.equals(id.getName()) && isIgnoredHttpUri(id.getTag("uri"));
    }

    private boolean isIgnoredHttpUri(String uri) {
        return uri != null && (uri.startsWith("/actuator")
                || uri.startsWith("/swagger")
                || uri.startsWith("/v3/api-docs"));
    }

    private double emailQueueSize(ThreadPoolTaskExecutor executor) {
        return executor.getThreadPoolExecutor().getQueue().size();
    }

    private double pendingOutboxCount(StorageDeletionOutboxRepository repository) {
        return repository.countByStatus(StorageDeletionOutboxStatus.PENDING);
    }

    private double dueOutboxCount(StorageDeletionOutboxRepository repository, Clock clock) {
        return repository.countByStatusAndNextAttemptAtLessThanEqual(
                StorageDeletionOutboxStatus.PENDING,
                Instant.now(clock)
        );
    }

    private double failedOutboxCount(StorageDeletionOutboxRepository repository) {
        return repository.countByStatus(StorageDeletionOutboxStatus.FAILED);
    }

    private double oldestPendingAgeSeconds(StorageDeletionOutboxRepository repository, Clock clock) {
        return repository.findOldestCreatedAtByStatus(StorageDeletionOutboxStatus.PENDING)
                .map(createdAt -> Duration.between(createdAt, Instant.now(clock)).toSeconds())
                .filter(ageSeconds -> ageSeconds > 0)
                .orElse(0L);
    }
}
