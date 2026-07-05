package org.plishka.backend.dto.admin.about;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminAboutPageContentRequestDto(
        @NotBlank(message = "Main title is required")
        @Size(max = 255, message = "Main title must contain at most 255 characters")
        String mainTitle,

        @NotBlank(message = "Main subtitle is required")
        @Size(max = 10000, message = "Main subtitle must contain at most 10000 characters")
        String mainSubtitle,

        @NotBlank(message = "Secondary title is required")
        @Size(max = 255, message = "Secondary title must contain at most 255 characters")
        String secondaryTitle,

        @NotBlank(message = "Secondary subtitle is required")
        @Size(max = 10000, message = "Secondary subtitle must contain at most 10000 characters")
        String secondarySubtitle
) {
}
