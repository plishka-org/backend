package org.plishka.backend.dto.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderDetailDto(
        Long orderId,
        String orderNumber,
        String customerName,
        BigDecimal totalPrice,
        String deliveryCity,
        String phone,
        String notes,
        Instant createdAt,
        List<OrderItemDetailDto> items
) {
}
