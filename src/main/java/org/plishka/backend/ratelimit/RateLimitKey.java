package org.plishka.backend.ratelimit;

public record RateLimitKey(
        RateLimitPolicy policy,
        String identity
) {
}
