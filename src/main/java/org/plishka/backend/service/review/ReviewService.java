package org.plishka.backend.service.review;

import java.util.List;
import org.plishka.backend.dto.common.PageResponse;
import org.plishka.backend.dto.review.ReviewDto;

public interface ReviewService {
    PageResponse<ReviewDto> getApprovedReviews(int page, int size);

    List<ReviewDto> getFeaturedReviews();
}
