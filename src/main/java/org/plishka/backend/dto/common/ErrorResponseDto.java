package org.plishka.backend.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.Builder;

@Builder
@Schema(name = "ErrorResponseDto", description = "Common API error response.")
public record ErrorResponseDto(
        @Schema(
                description = "UTC ISO-8601 timestamp.",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Instant timestamp,
        @Schema(description = "HTTP status code.", example = "401", requiredMode = Schema.RequiredMode.REQUIRED)
        int status,
        @Schema(
                description = "HTTP reason phrase.",
                example = "Unauthorized",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String error,
        @Schema(
                description = "Privacy-safe error message.",
                example = "Invalid or missing credentials",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String message,
        @Schema(description = "Request path.", example = "/api/auth/login", requiredMode = Schema.RequiredMode.REQUIRED)
        String path
) {
}
