package org.plishka.backend.service.review;

import org.plishka.backend.dto.common.PageResponse;
import org.plishka.backend.dto.file.AttachMediaRequestDto;
import org.plishka.backend.dto.review.ReviewDetailDto;
import org.plishka.backend.dto.review.ReviewSummaryDto;

public interface ReviewService {
    PageResponse<ReviewSummaryDto> getReviews(int page, int size);

    ReviewDetailDto getReview(Long id);

    void attachMedia(Long reviewId, AttachMediaRequestDto request);
}
