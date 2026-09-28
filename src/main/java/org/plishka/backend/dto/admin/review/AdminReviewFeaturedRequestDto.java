package org.plishka.backend.dto.admin.review;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Featured review flag update request.")
public record AdminReviewFeaturedRequestDto(
        @Schema(
                description = "Whether the review should be featured. Featured reviews are limited to 5.",
                example = "true"
        )
        @NotNull(message = "Featured flag is required")
        Boolean featured
) {
}
