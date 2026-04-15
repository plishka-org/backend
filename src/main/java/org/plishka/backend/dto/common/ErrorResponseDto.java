package org.plishka.backend.dto.common;

import java.time.Instant;

public record ErrorResponseDto(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
