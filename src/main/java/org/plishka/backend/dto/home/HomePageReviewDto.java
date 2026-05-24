package org.plishka.backend.dto.home;

import java.util.List;
import org.plishka.backend.dto.review.ReviewMediaDto;

public record HomePageReviewDto(
        Long reviewId,
        String authorName,
        String content,
        List<ReviewMediaDto> media
) {
}
