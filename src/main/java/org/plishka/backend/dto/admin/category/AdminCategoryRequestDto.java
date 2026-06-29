package org.plishka.backend.dto.admin.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminCategoryRequestDto(
        @NotBlank(message = "Category name is required")
        @Size(max = 100, message = "Category name must contain at most 100 characters")
        String name
) {
}
