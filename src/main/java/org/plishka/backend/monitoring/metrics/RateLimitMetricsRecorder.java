package org.plishka.backend.monitoring.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.ratelimit.RateLimitPolicy;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RateLimitMetricsRecorder {
    public static final String DECISION_ALLOWED = "allowed";
    public static final String DECISION_BLOCKED = "blocked";

    private final MeterRegistry meterRegistry;

    public void recordDecision(RateLimitPolicy policy, String decision) {
        Counter.builder("rate.limit.requests")
                .tag("policy", policy.getPropertyName())
                .tag("decision", decision)
                .register(meterRegistry)
                .increment();
    }
}
