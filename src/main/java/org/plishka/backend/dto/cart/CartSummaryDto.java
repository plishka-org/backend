package org.plishka.backend.dto.cart;

import java.util.List;

public record CartSummaryDto(
        List<CartItemSummaryDto> items,
        Long totalPrice
) {
}
