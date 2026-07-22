package org.plishka.backend.dto.admin.order;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "Admin order details.")
public record AdminOrderDetailDto(
        @Schema(description = "Order id.", example = "1001")
        Long orderId,

        @Schema(description = "Human-readable order number.", example = "PL-20260706-0001")
        String orderNumber,

        @Schema(description = "Creation timestamp in UTC ISO-8601 format.", example = "2026-07-06T12:00:00Z")
        Instant createdAt,

        @Schema(description = "Customer name snapshot.", example = "Olena Shevchenko")
        String customerName,

        @Schema(description = "Customer phone number.", example = "+380501234567")
        String phone,

        @Schema(description = "Ordered products.")
        List<AdminOrderItemDetailDto> items,

        @Schema(description = "Total price in whole UAH.", example = "2998")
        Long totalPrice,

        @Schema(description = "Delivery city.", example = "Kyiv")
        String deliveryCity,

        @Schema(description = "Optional order notes.", nullable = true)
        String notes
) {
}
