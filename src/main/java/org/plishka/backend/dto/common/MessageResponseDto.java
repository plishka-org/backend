package org.plishka.backend.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Generic message response.")
public record MessageResponseDto(
        @Schema(description = "Human-readable response message.", example = "Operation completed successfully.")
        String message
) {
}
