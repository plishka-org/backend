package org.plishka.backend.ratelimit;

import java.time.Duration;

public record RateLimitResult(
        boolean allowed,
        Duration retryAfter
) {
    public static RateLimitResult allowedResult() {
        return new RateLimitResult(true, Duration.ZERO);
    }

    public static RateLimitResult blockedResult(Duration retryAfter) {
        return new RateLimitResult(false, retryAfter);
    }

    public long retryAfterSeconds() {
        if (retryAfter.isZero() || retryAfter.isNegative()) {
            return 1L;
        }

        return retryAfter.plusNanos(999_999_999L).toSeconds();
    }
}
