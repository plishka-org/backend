package org.plishka.backend.dto.admin.about;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Admin about page content update request.")
public record AdminAboutPageContentRequestDto(
        @Schema(description = "Main title.", example = "About Plishka")
        @NotBlank(message = "Main title is required")
        @Size(max = 255, message = "Main title must contain at most 255 characters")
        String mainTitle,

        @Schema(description = "Main subtitle.", example = "We craft wooden pieces for warm, practical interiors.")
        @NotBlank(message = "Main subtitle is required")
        @Size(max = 10000, message = "Main subtitle must contain at most 10000 characters")
        String mainSubtitle,

        @Schema(description = "Secondary title.", example = "Our Story")
        @NotBlank(message = "Secondary title is required")
        @Size(max = 255, message = "Secondary title must contain at most 255 characters")
        String secondaryTitle,

        @Schema(description = "Secondary subtitle.", example = "Handcrafted wooden goods for everyday home comfort.")
        @NotBlank(message = "Secondary subtitle is required")
        @Size(max = 10000, message = "Secondary subtitle must contain at most 10000 characters")
        String secondarySubtitle
) {
}
