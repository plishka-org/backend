package org.plishka.backend.service.admin.review;

import org.plishka.backend.dto.admin.review.AdminReviewDetailDto;
import org.plishka.backend.dto.admin.review.AdminReviewFeaturedRequestDto;
import org.plishka.backend.dto.admin.review.AdminReviewRequestDto;
import org.plishka.backend.dto.admin.review.AdminReviewSummaryDto;
import org.plishka.backend.dto.common.PageResponse;
import org.plishka.backend.dto.file.AttachMediaRequestDto;

public interface AdminReviewService {
    PageResponse<AdminReviewSummaryDto> getReviews(int page, int size);

    AdminReviewDetailDto getReview(Long reviewId);

    AdminReviewDetailDto createReview(AdminReviewRequestDto request);

    AdminReviewDetailDto updateReview(Long reviewId, AdminReviewRequestDto request);

    void deleteReview(Long reviewId);

    AdminReviewDetailDto updateFeatured(Long reviewId, AdminReviewFeaturedRequestDto request);

    void attachMedia(Long reviewId, AttachMediaRequestDto request);
}
