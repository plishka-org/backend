package org.plishka.backend.dto.admin.review;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import org.plishka.backend.dto.review.ReviewMediaDto;

@Schema(description = "Admin review details.")
public record AdminReviewDetailDto(
        @Schema(description = "Review id.", example = "1")
        Long reviewId,
        @Schema(description = "Review author name.", example = "Olena")
        String authorName,
        @Schema(description = "Review content.", example = "Great quality and fast delivery.")
        String content,
        @Schema(description = "Creation timestamp in UTC ISO-8601 format.", example = "2026-07-06T12:00:00Z")
        Instant createdAt,
        @Schema(description = "Whether the review is featured.", example = "true")
        Boolean isFeatured,
        @Schema(description = "Review media.")
        List<ReviewMediaDto> media
) {
}
