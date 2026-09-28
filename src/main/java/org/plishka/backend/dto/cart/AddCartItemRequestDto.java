package org.plishka.backend.dto.cart;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Add cart item request.")
public record AddCartItemRequestDto(
        @Schema(description = "Product id.", example = "123")
        @NotNull(message = "Product ID is required")
        @Positive(message = "Product ID must be positive")
        Long productId,

        @Schema(description = "Quantity to add. Maximum per item: 50.", example = "2")
        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be positive")
        @Max(value = 50, message = "Maximum quantity per item is 50")
        Integer quantity
) {
}
