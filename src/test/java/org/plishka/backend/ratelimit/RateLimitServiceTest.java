package org.plishka.backend.ratelimit;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.plishka.backend.config.properties.RateLimitProperties;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitServiceTest {
    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        RateLimitProperties properties = new RateLimitProperties(
                true,
                defaultCache(),
                Map.of(
                        RateLimitPolicy.AUTH_LOGIN_IP.getPropertyName(),
                        policy(2, Duration.ofHours(1))
                ));
        rateLimitService = new RateLimitService(properties, Caffeine.newBuilder().build());
    }

    @Test
    void consume_ShouldAllowRequestsUntilLimit() {
        RateLimitKey key = key("127.0.0.1");

        assertTrue(rateLimitService.consume(List.of(key)).allowed());
        assertTrue(rateLimitService.consume(List.of(key)).allowed());
    }

    @Test
    void consume_ShouldBlockAfterLimit() {
        RateLimitKey key = key("127.0.0.1");

        rateLimitService.consume(List.of(key));
        rateLimitService.consume(List.of(key));

        assertFalse(rateLimitService.consume(List.of(key)).allowed());
    }

    @Test
    void consume_ShouldReturnRetryAfter_WhenBlocked() {
        RateLimitKey key = key("127.0.0.1");

        rateLimitService.consume(List.of(key));
        rateLimitService.consume(List.of(key));

        RateLimitResult result = rateLimitService.consume(List.of(key));

        assertFalse(result.allowed());
        assertTrue(result.retryAfter().toNanos() > 0);
        assertTrue(result.retryAfterSeconds() > 0);
    }

    @Test
    void consume_ShouldNotShareLimitsBetweenDifferentKeys() {
        RateLimitKey firstKey = key("127.0.0.1");
        RateLimitKey secondKey = key("127.0.0.2");

        rateLimitService.consume(List.of(firstKey));
        rateLimitService.consume(List.of(firstKey));

        assertFalse(rateLimitService.consume(List.of(firstKey)).allowed());
        assertTrue(rateLimitService.consume(List.of(secondKey)).allowed());
    }

    @Test
    void consume_ShouldBlockDefaultHighLimitPolicyAfterCapacity() {
        RateLimitProperties properties = defaultProperties();
        RateLimitService service = new RateLimitService(properties, Caffeine.newBuilder().build());
        RateLimitKey key = new RateLimitKey(
                RateLimitPolicy.FILE_DOWNLOAD_PRESIGN,
                "127.0.0.1"
        );

        for (int i = 0; i < 300; i++) {
            assertTrue(service.consume(List.of(key)).allowed());
        }

        assertFalse(service.consume(List.of(key)).allowed());
    }

    private static RateLimitKey key(String identity) {
        return new RateLimitKey(RateLimitPolicy.AUTH_LOGIN_IP, identity);
    }

    private static RateLimitProperties defaultProperties() {
        return new RateLimitProperties(true, defaultCache(), Map.of());
    }

    private static RateLimitProperties.Cache defaultCache() {
        return new RateLimitProperties.Cache(Duration.ofHours(24), 100_000L);
    }

    private static RateLimitProperties.Policy policy(long capacity, Duration period) {
        RateLimitProperties.Bandwidth bandwidth = new RateLimitProperties.Bandwidth(capacity, period);
        return new RateLimitProperties.Policy(List.of(bandwidth));
    }
}
