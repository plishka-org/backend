package org.plishka.backend.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Order summary.")
public record OrderSummaryDto(
        @Schema(description = "Order id.", example = "1001")
        Long orderId,
        @Schema(description = "Human-readable order number.", example = "PL-20260706-0001")
        String orderNumber,
        @Schema(description = "Total price as integer amount in whole Ukrainian hryvnias (UAH).", example = "2998")
        Long totalPrice,
        @Schema(description = "Creation timestamp in UTC ISO-8601 format.", example = "2026-07-06T12:00:00Z")
        Instant createdAt
) {
}
