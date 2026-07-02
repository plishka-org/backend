package org.plishka.backend.dto.admin.review;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminReviewRequestDto(
        @NotBlank(message = "Review author name is required")
        @Size(max = 100, message = "Review author name must contain at most 100 characters")
        String authorName,

        @NotBlank(message = "Review content is required")
        @Size(max = 10000, message = "Review content must contain at most 10000 characters")
        String content
) {
}
