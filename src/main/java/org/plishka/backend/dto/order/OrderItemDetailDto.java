package org.plishka.backend.dto.order;

public record OrderItemDetailDto(
        Long orderItemId,
        Long productId,
        String productName,
        String categoryName,
        Integer quantity,
        Long unitPrice,
        Long subtotal
) {
}
