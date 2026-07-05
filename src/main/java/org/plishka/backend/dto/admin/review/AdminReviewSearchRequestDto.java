package org.plishka.backend.dto.admin.review;

import jakarta.validation.constraints.Size;

public record AdminReviewSearchRequestDto(
        @Size(max = 100, message = "Search must contain at most 100 characters")
        String search
) {
}
