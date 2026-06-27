package org.plishka.backend.dto.admin.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record AdminProductRequestDto(
        @NotBlank(message = "Product name is required")
        @Size(max = 255, message = "Product name must contain at most 255 characters")
        String name,

        @NotBlank(message = "Product description is required")
        @Size(max = 5000, message = "Product description must contain at most 5000 characters")
        String description,

        @NotNull(message = "Product price is required")
        @DecimalMin(value = "0.01", message = "Product price must be greater than 0")
        @Digits(integer = 8, fraction = 2, message = "Product price must fit DECIMAL(10,2)")
        BigDecimal price,

        @NotNull(message = "Product category is required")
        @Positive
        Long categoryId
) {
}
