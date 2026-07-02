package org.plishka.backend.dto.admin.home;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminHomePageAdvantageRequestDto(
        @NotBlank(message = "Advantage title is required")
        @Size(max = 100, message = "Advantage title must contain at most 100 characters")
        String title,

        @Size(max = 255, message = "Advantage description must contain at most 255 characters")
        String description,

        @Size(max = 512, message = "Icon S3 key must contain at most 512 characters")
        String iconS3Key
) {
}
