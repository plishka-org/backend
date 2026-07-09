package org.plishka.backend.dto.cart;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Current cart summary.")
public record CartSummaryDto(
        @Schema(description = "Cart items.")
        List<CartItemSummaryDto> items,
        @Schema(description = "Total price as integer amount in whole Ukrainian hryvnias (UAH).", example = "2998")
        Long totalPrice
) {
}
