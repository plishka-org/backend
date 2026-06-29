package org.plishka.backend.dto.order;

import java.time.Instant;
import java.util.List;

public record OrderDetailDto(
        Long orderId,
        String orderNumber,
        String customerName,
        Long totalPrice,
        String deliveryCity,
        String phone,
        String notes,
        Instant createdAt,
        List<OrderItemDetailDto> items
) {
}
