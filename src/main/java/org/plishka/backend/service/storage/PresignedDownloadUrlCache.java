package org.plishka.backend.service.storage;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.Collection;
import java.util.function.Function;
import org.plishka.backend.service.storage.model.PresignedStorageUrl;

public class PresignedDownloadUrlCache {
    private static final long MAXIMUM_SIZE = 10000L;
    private static final int EXPIRY_MARGIN_DIVISOR = 7;

    private final Cache<String, PresignedStorageUrl> cache;

    public PresignedDownloadUrlCache(Duration presignTtl) {
        this(Caffeine.newBuilder()
                .expireAfterWrite(cacheTtl(presignTtl))
                .maximumSize(MAXIMUM_SIZE)
                .build());
    }

    PresignedDownloadUrlCache(Cache<String, PresignedStorageUrl> cache) {
        this.cache = cache;
    }

    public PresignedStorageUrl get(String s3Key, Function<String, PresignedStorageUrl> loader) {
        return cache.get(s3Key, loader);
    }

    public void invalidateAll(Collection<String> s3Keys) {
        cache.invalidateAll(s3Keys);
    }

    static Duration cacheTtl(Duration presignTtl) {
        return presignTtl.minus(presignTtl.dividedBy(EXPIRY_MARGIN_DIVISOR));
    }
}
