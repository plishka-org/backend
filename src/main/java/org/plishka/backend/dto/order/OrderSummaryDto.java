package org.plishka.backend.dto.order;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderSummaryDto(
        Long orderId,
        String orderNumber,
        BigDecimal totalPrice,
        Instant createdAt
) {
}
