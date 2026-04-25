package org.plishka.backend.dto.file;

import java.time.Instant;
import java.util.Map;
import lombok.Builder;

@Builder
public record PresignUploadResponseDto(
        String s3Key,
        String uploadUrl,
        String method,
        Instant expiresAt,
        Map<String, String> requiredHeaders
) {
}
