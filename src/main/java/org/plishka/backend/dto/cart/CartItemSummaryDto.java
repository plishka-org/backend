package org.plishka.backend.dto.cart;

import java.math.BigDecimal;

public record CartItemSummaryDto(
        Long cartItemId,
        Long productId,
        String productName,
        String categoryName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
}
