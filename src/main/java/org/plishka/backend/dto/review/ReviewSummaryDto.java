package org.plishka.backend.dto.review;

import java.time.Instant;

public record ReviewSummaryDto(
        Long reviewId,
        String authorName,
        String content,
        Instant createdAt,
        ReviewMediaPreviewDto primaryMedia
) {
}
