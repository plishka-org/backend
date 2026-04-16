package org.plishka.backend.dto.common;

import java.time.Instant;
import java.util.List;
import lombok.Builder;

@Builder
public record ValidationErrorResponseDto(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> fieldErrors
) {
}
