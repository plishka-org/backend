package org.plishka.backend.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import lombok.Builder;

@Builder
@Schema(
        name = "ValidationErrorResponseDto",
        description = "Validation error response for request body, parameter, header, and JSON parse errors."
)
public record ValidationErrorResponseDto(
        @Schema(
                description = "UTC ISO-8601 timestamp.",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Instant timestamp,
        @Schema(description = "HTTP status code.", example = "400", requiredMode = Schema.RequiredMode.REQUIRED)
        int status,
        @Schema(
                description = "HTTP reason phrase.",
                example = "Bad Request",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String error,
        @Schema(
                description = "Validation summary.",
                example = "Validation failed",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String message,
        @Schema(description = "Request path.", example = "/api/auth/login", requiredMode = Schema.RequiredMode.REQUIRED)
        String path,
        @Schema(
                description = "Human-readable field-level validation errors.",
                example = "[\"email: Email must be a valid email address\"]",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        List<String> fieldErrors
) {
}
