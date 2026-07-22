package org.plishka.backend.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;
import org.plishka.backend.openapi.support.OpenApiExampleValues;

@Schema(description = "Product category.")
public record CategoryDto(
        @Schema(description = "Category id.", example = "1")
        Long categoryId,

        @Schema(description = "Category name.", example = OpenApiExampleValues.GENERIC_CATEGORY_NAME)
        String name,

        @Schema(description = "Manual category display order.", example = "1")
        Integer displayOrder
) {
    public CategoryDto(Long categoryId, String name) {
        this(categoryId, name, null);
    }
}
