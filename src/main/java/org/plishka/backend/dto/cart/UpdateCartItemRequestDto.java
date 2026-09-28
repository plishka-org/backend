package org.plishka.backend.dto.cart;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Update cart item request.")
public record UpdateCartItemRequestDto(
        @Schema(description = "New quantity. Maximum per item: 50.", example = "3")
        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be positive")
        @Max(value = 50, message = "Maximum quantity per item is 50")
        Integer quantity
) {
}
