package org.plishka.backend.ratelimit;

import com.github.benmanes.caffeine.cache.Cache;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.EstimationProbe;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.config.properties.RateLimitProperties;
import org.plishka.backend.monitoring.metrics.RateLimitMetricsRecorder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RateLimitService {
    private static final String CACHE_KEY_SEPARATOR = "|";

    private final RateLimitProperties rateLimitProperties;
    private final Cache<String, Bucket> bucketCache;
    private final RateLimitMetricsRecorder rateLimitMetricsRecorder;

    public RateLimitResult consume(List<RateLimitKey> keys) {
        if (!rateLimitProperties.enabled() || keys.isEmpty()) {
            return RateLimitResult.allowedResult();
        }

        List<Bucket> buckets = keys.stream()
                .map(this::bucketFor)
                .toList();

        Duration retryAfter = maxRetryAfter(buckets);
        if (!retryAfter.isZero()) {
            RateLimitResult result = RateLimitResult.blockedResult(retryAfter);
            recordDecision(keys, result);
            return result;
        }

        RateLimitResult result = consumeAll(buckets);
        recordDecision(keys, result);
        return result;
    }

    private Duration maxRetryAfter(List<Bucket> buckets) {
        Duration retryAfter = Duration.ZERO;

        for (Bucket bucket : buckets) {
            EstimationProbe probe = bucket.estimateAbilityToConsume(1);
            if (!probe.canBeConsumed()) {
                Duration bucketRetryAfter = Duration.ofNanos(probe.getNanosToWaitForRefill());
                if (bucketRetryAfter.compareTo(retryAfter) > 0) {
                    retryAfter = bucketRetryAfter;
                }
            }
        }

        return retryAfter;
    }

    private RateLimitResult consumeAll(List<Bucket> buckets) {
        Duration retryAfter = Duration.ZERO;

        for (Bucket bucket : buckets) {
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
            if (!probe.isConsumed()) {
                Duration bucketRetryAfter = Duration.ofNanos(probe.getNanosToWaitForRefill());
                if (bucketRetryAfter.compareTo(retryAfter) > 0) {
                    retryAfter = bucketRetryAfter;
                }
            }
        }

        return retryAfter.isZero()
                ? RateLimitResult.allowedResult()
                : RateLimitResult.blockedResult(retryAfter);
    }

    private Bucket bucketFor(RateLimitKey key) {
        return bucketCache.get(cacheKey(key), ignored -> buildBucket(key.policy()));
    }

    private String cacheKey(RateLimitKey key) {
        return key.policy().getPropertyName() + CACHE_KEY_SEPARATOR + key.identity();
    }

    private Bucket buildBucket(RateLimitPolicy policy) {
        var builder = Bucket.builder();
        resolveBandwidths(policy).forEach(bandwidth -> builder.addLimit(Bandwidth.builder()
                .capacity(bandwidth.capacity())
                .refillGreedy(bandwidth.capacity(), bandwidth.period())
                .build()));
        return builder.build();
    }

    private void recordDecision(List<RateLimitKey> keys, RateLimitResult result) {
        String decision = result.allowed()
                ? RateLimitMetricsRecorder.DECISION_ALLOWED
                : RateLimitMetricsRecorder.DECISION_BLOCKED;
        keys.forEach(key -> rateLimitMetricsRecorder.recordDecision(key.policy(), decision));
    }

    private List<RateLimitPolicy.BandwidthLimit> resolveBandwidths(RateLimitPolicy policy) {
        RateLimitProperties.Policy configuredPolicy = rateLimitProperties.policies().get(policy.getPropertyName());
        if (configuredPolicy == null) {
            return policy.getDefaultBandwidths();
        }

        return configuredPolicy.bandwidths().stream()
                .map(bandwidth -> new RateLimitPolicy.BandwidthLimit(
                        bandwidth.capacity(),
                        bandwidth.period()
                ))
                .toList();
    }
}
