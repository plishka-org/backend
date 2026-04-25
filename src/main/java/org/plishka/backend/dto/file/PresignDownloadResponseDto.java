package org.plishka.backend.dto.file;

import java.time.Instant;
import lombok.Builder;

@Builder
public record PresignDownloadResponseDto(
        String s3Key,
        String downloadUrl,
        String method,
        Instant expiresAt
) {
}
