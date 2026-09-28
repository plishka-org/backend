package org.plishka.backend.service.storage.model;

import java.time.Instant;
import java.util.Map;
import lombok.Builder;

@Builder
public record PresignedStorageUrl(
        String url,
        Instant expiresAt,
        Map<String, String> requiredHeaders
) {
}
