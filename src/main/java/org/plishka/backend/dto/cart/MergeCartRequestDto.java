package org.plishka.backend.dto.cart;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record MergeCartRequestDto(
        @NotNull(message = "Items list is required")
        List<@Valid AddCartItemRequestDto> items
) {
}
