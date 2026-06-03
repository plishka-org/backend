package org.plishka.backend.dto.order;

import java.math.BigDecimal;

public record OrderItemDetailDto(
        Long orderItemId,
        Long productId,
        String productName,
        String categoryName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
}
