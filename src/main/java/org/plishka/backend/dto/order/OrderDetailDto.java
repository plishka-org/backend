package org.plishka.backend.dto.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.plishka.backend.domain.order.OrderStatus;

public record OrderDetailDto(
        Long orderId,
        String orderNumber,
        String customerName,
        OrderStatus status,
        BigDecimal totalPrice,
        String deliveryCity,
        String phone,
        String notes,
        Instant createdAt,
        List<OrderItemDetailDto> items
) {
}
