package org.plishka.backend.dto.cart;

public record CartItemSummaryDto(
        Long cartItemId,
        Long productId,
        String productName,
        String categoryName,
        Integer quantity,
        Long unitPrice,
        Long subtotal
) {
}
