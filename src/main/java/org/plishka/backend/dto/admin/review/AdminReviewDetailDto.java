package org.plishka.backend.dto.admin.review;

import java.time.Instant;
import java.util.List;
import org.plishka.backend.dto.review.ReviewMediaDto;

public record AdminReviewDetailDto(
        Long reviewId,
        String authorName,
        String content,
        Instant createdAt,
        Boolean isFeatured,
        List<ReviewMediaDto> media
) {
}
