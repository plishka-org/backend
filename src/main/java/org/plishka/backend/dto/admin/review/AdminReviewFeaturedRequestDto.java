package org.plishka.backend.dto.admin.review;

import jakarta.validation.constraints.NotNull;

public record AdminReviewFeaturedRequestDto(
        @NotNull(message = "Featured flag is required")
        Boolean featured
) {
}
