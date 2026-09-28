package org.plishka.backend.dto.review;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "Review summary.")
public record ReviewSummaryDto(
        @Schema(description = "Review id.", example = "1")
        Long reviewId,
        @Schema(description = "Review author name.", example = "Olena")
        String authorName,
        @Schema(description = "Review content.", example = "Great quality and fast delivery.")
        String content,
        @Schema(description = "Creation timestamp in UTC ISO-8601 format.", example = "2026-07-06T12:00:00Z")
        Instant createdAt,
        @Schema(description = "Primary media preview. May be null when no media is attached.", nullable = true)
        ReviewMediaPreviewDto primaryMedia
) {
}
