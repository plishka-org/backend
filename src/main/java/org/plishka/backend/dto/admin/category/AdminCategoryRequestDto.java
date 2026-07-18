package org.plishka.backend.dto.admin.category;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.plishka.backend.openapi.support.OpenApiExampleValues;

@Schema(description = "Admin category create/update request.")
public record AdminCategoryRequestDto(
        @Schema(description = "Category name.", example = OpenApiExampleValues.GENERIC_CATEGORY_NAME)
        @NotBlank(message = "Category name is required")
        @Size(max = 100, message = "Category name must contain at most 100 characters")
        String name
) {
}
