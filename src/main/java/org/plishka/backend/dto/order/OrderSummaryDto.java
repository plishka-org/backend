package org.plishka.backend.dto.order;

import java.time.Instant;

public record OrderSummaryDto(
        Long orderId,
        String orderNumber,
        Long totalPrice,
        Instant createdAt
) {
}
