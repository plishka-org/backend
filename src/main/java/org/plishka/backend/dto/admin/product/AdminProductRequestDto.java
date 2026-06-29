package org.plishka.backend.dto.admin.product;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AdminProductRequestDto(
        @NotBlank(message = "Product name is required")
        @Size(max = 255, message = "Product name must contain at most 255 characters")
        String name,

        @NotBlank(message = "Product description is required")
        @Size(max = 5000, message = "Product description must contain at most 5000 characters")
        String description,

        @NotNull(message = "Product price is required")
        @Positive(message = "Product price must be greater than 0")
        @Max(value = 10_000_000L, message = "Product price must be at most 10000000")
        Long price,

        @NotNull(message = "Product category is required")
        @Positive
        Long categoryId
) {
}
