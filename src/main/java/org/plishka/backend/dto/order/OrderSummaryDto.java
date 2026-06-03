package org.plishka.backend.dto.order;

import java.math.BigDecimal;
import java.time.Instant;
import org.plishka.backend.domain.order.OrderStatus;

public record OrderSummaryDto(
        Long orderId,
        String orderNumber,
        OrderStatus status,
        BigDecimal totalPrice,
        Instant createdAt
) {
}
