package org.plishka.backend.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "Order details.")
public record OrderDetailDto(
        @Schema(description = "Order id.", example = "1001")
        Long orderId,
        @Schema(description = "Human-readable order number.", example = "PL-20260706-0001")
        String orderNumber,
        @Schema(description = "Customer full name.", example = "Olena Shevchenko")
        String customerName,
        @Schema(description = "Total price as integer amount in whole Ukrainian hryvnias (UAH).", example = "2998")
        Long totalPrice,
        @Schema(description = "Delivery city.", example = "Kyiv")
        String deliveryCity,
        @Schema(description = "Phone number.", example = "+380501234567")
        String phone,
        @Schema(description = "Optional order notes.", example = "Call before delivery", nullable = true)
        String notes,
        @Schema(description = "Creation timestamp in UTC ISO-8601 format.", example = "2026-07-06T12:00:00Z")
        Instant createdAt,
        @Schema(description = "Order items.")
        List<OrderItemDetailDto> items
) {
}
