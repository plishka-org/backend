package org.plishka.backend.dto.admin.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.plishka.backend.openapi.support.OpenApiExampleValues;

@Schema(description = "Admin product create/update request.")
public record AdminProductRequestDto(
        @Schema(description = "Product name.", example = OpenApiExampleValues.PRIMARY_PRODUCT_NAME)
        @NotBlank(message = "Product name is required")
        @Size(max = 255, message = "Product name must contain at most 255 characters")
        String name,

        @Schema(
                description = "Product description.",
                example = OpenApiExampleValues.PRODUCT_DESCRIPTION
        )
        @NotBlank(message = "Product description is required")
        @Size(max = 5000, message = "Product description must contain at most 5000 characters")
        String description,

        @Schema(description = "Price as integer amount in whole Ukrainian hryvnias (UAH).", example = "1499")
        @NotNull(message = "Product price is required")
        @Positive(message = "Product price must be greater than 0")
        @Max(value = 10_000_000L, message = "Product price must be at most 10000000")
        Long price,

        @Schema(description = "Category id.", example = "1")
        @NotNull(message = "Product category is required")
        @Positive
        Long categoryId
) {
}
