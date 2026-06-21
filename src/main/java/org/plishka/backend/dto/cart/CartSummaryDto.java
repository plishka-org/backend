package org.plishka.backend.dto.cart;

import java.math.BigDecimal;
import java.util.List;

public record CartSummaryDto(
        List<CartItemSummaryDto> items,
        BigDecimal totalPrice
) {
}
