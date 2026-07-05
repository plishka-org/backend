package org.plishka.backend.dto.admin.review;

import java.time.Instant;
import org.plishka.backend.dto.review.ReviewMediaPreviewDto;

public record AdminReviewSummaryDto(
        Long reviewId,
        String authorName,
        String content,
        Instant createdAt,
        Boolean isFeatured,
        ReviewMediaPreviewDto primaryMedia
) {
}
