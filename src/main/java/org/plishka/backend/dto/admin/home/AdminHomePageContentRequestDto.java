package org.plishka.backend.dto.admin.home;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Admin home page content update request.")
public record AdminHomePageContentRequestDto(
        @Schema(description = "Home page title.", example = "Plishka")
        @NotBlank(message = "Home page title is required")
        @Size(max = 255, message = "Home page title must contain at most 255 characters")
        String title,

        @Schema(
                description = "Home page description.",
                example = "Handcrafted wooden goods for everyday home comfort.",
                nullable = true
        )
        @Size(max = 5000, message = "Home page description must contain at most 5000 characters")
        String description
) {
}
