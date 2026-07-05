package org.plishka.backend.dto.admin.home;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminHomePageContentRequestDto(
        @NotBlank(message = "Home page title is required")
        @Size(max = 255, message = "Home page title must contain at most 255 characters")
        String title,

        @Size(max = 5000, message = "Home page description must contain at most 5000 characters")
        String description
) {
}
