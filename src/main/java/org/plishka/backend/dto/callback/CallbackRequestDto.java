package org.plishka.backend.dto.callback;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Callback request.")
public record CallbackRequestDto(
        @Schema(description = "Callback request id.", example = "1")
        Long callbackRequestId,
        @Schema(description = "Requester name.", example = "Olena Shevchenko")
        String name,
        @Schema(description = "Phone number.", example = "+380501234567")
        String phone,
        @Schema(description = "Callback message.", example = "Please call me back about my order")
        String message,
        @Schema(description = "Creation timestamp in UTC ISO-8601 format.", example = "2026-07-06T12:00:00Z")
        Instant createdAt
) {
}
