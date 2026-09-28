package org.plishka.backend.dto.admin.review;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Admin review search query parameters.")
public record AdminReviewSearchRequestDto(
        @Parameter(description = "Search text.", example = "quality")
        @Schema(nullable = true)
        @Size(max = 100, message = "Search must contain at most 100 characters")
        String search
) {
}
