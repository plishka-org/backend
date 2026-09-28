package org.plishka.backend.service.storage;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.plishka.backend.service.storage.model.PresignedStorageUrl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class PresignedDownloadUrlCacheTest {
    private static final String S3_KEY = "products/1/images/2026/08/image.jpg";
    private static final Duration CACHE_TTL = Duration.ofDays(6);

    private final AtomicLong tickerNanos = new AtomicLong();
    private final AtomicInteger loadCount = new AtomicInteger();

    private PresignedDownloadUrlCache cache;

    @BeforeEach
    void setUp() {
        Cache<String, PresignedStorageUrl> caffeineCache = Caffeine.newBuilder()
                .expireAfterWrite(CACHE_TTL)
                .ticker(tickerNanos::get)
                .build();
        cache = new PresignedDownloadUrlCache(caffeineCache);
    }

    @Test
    void cacheTtl_ShouldReserveOneSeventhOfPresignTtl() {
        assertEquals(Duration.ofDays(6), PresignedDownloadUrlCache.cacheTtl(Duration.ofDays(7)));
    }

    @Test
    void get_ShouldReuseUrlUntilCacheExpires() {
        PresignedStorageUrl first = cache.get(S3_KEY, this::loadUrl);
        PresignedStorageUrl cached = cache.get(S3_KEY, this::loadUrl);

        assertSame(first, cached);

        tickerNanos.addAndGet(CACHE_TTL.toNanos());

        PresignedStorageUrl refreshed = cache.get(S3_KEY, this::loadUrl);

        assertNotSame(first, refreshed);
    }

    @Test
    void invalidateAll_ShouldRemoveMatchingUrl() {
        PresignedStorageUrl first = cache.get(S3_KEY, this::loadUrl);

        cache.invalidateAll(List.of(S3_KEY));
        PresignedStorageUrl refreshed = cache.get(S3_KEY, this::loadUrl);

        assertNotSame(first, refreshed);
    }

    private PresignedStorageUrl loadUrl(String ignored) {
        int sequence = loadCount.incrementAndGet();
        return PresignedStorageUrl.builder()
                .url("https://storage.example.com/image?signature=" + sequence)
                .expiresAt(Instant.EPOCH.plus(Duration.ofDays(7)))
                .requiredHeaders(Map.of())
                .build();
    }
}
