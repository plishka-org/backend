package org.plishka.backend.dto.cart;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record MergeCartRequestDto(
        @NotEmpty(message = "Items list must not be empty")
        @Size(max = 100, message = "Items list must not contain more than 100 items")
        List<@NotNull(message = "Item must not be null") @Valid AddCartItemRequestDto> items
) {
}
