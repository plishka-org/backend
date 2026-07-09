package org.plishka.backend.dto.admin.review;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Admin review create/update request.")
public record AdminReviewRequestDto(
        @Schema(description = "Review author name.", example = "Olena")
        @NotBlank(message = "Review author name is required")
        @Size(max = 100, message = "Review author name must contain at most 100 characters")
        String authorName,

        @Schema(description = "Review content.", example = "Great quality and fast delivery.")
        @NotBlank(message = "Review content is required")
        @Size(max = 10000, message = "Review content must contain at most 10000 characters")
        String content
) {
}
