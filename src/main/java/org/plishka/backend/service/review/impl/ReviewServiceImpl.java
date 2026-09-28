package org.plishka.backend.service.review.impl;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.domain.media.MediaTargetType;
import org.plishka.backend.domain.review.Review;
import org.plishka.backend.domain.review.ReviewMedia;
import org.plishka.backend.dto.common.PageResponse;
import org.plishka.backend.dto.file.AttachMediaRequestDto;
import org.plishka.backend.dto.review.ReviewDetailDto;
import org.plishka.backend.dto.review.ReviewSummaryDto;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.mapper.review.ReviewMapper;
import org.plishka.backend.repository.review.ReviewRepository;
import org.plishka.backend.service.file.MediaAttachmentService;
import org.plishka.backend.service.review.ReviewMediaQueryService;
import org.plishka.backend.service.review.ReviewService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {
    private static final Sort REVIEWS_SORT = Sort.by(
            Sort.Order.desc("createdAt"),
            Sort.Order.desc("id")
    );

    private final ReviewRepository reviewRepository;
    private final ReviewMediaQueryService reviewMediaQueryService;
    private final MediaAttachmentService mediaAttachmentService;
    private final ReviewMapper reviewMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewSummaryDto> getReviews(int page, int size) {
        log.debug("Fetching reviews: page={}, size={}", page, size);

        Page<Review> reviewPage = reviewRepository.findAll(PageRequest.of(page, size, REVIEWS_SORT));

        Map<Long, ReviewMedia> primaryMediaByReviewId = reviewMediaQueryService.findPrimaryMediaForReviews(
                reviewPage.getContent()
        );

        List<ReviewSummaryDto> content = reviewPage.getContent().stream()
                .map(review -> reviewMapper.toSummaryDto(review, primaryMediaByReviewId.get(review.getId())))
                .toList();

        log.debug("Successfully fetched {} reviews", content.size());

        return PageResponse.from(reviewPage, content);
    }

    @Override
    @Transactional(readOnly = true)
    public ReviewDetailDto getReview(Long id) {
        log.debug("Fetching review with ID: {}", id);

        Review review = findReviewDetailsOrThrow(id);

        log.debug("Successfully fetched review with ID: {}", id);

        return reviewMapper.toDetailDto(review, review.getMedia());
    }

    @Override
    public void attachMedia(Long reviewId, AttachMediaRequestDto request) {
        mediaAttachmentService.attachMedia(MediaTargetType.REVIEW, reviewId, request.s3Key());
    }

    private Review findReviewDetailsOrThrow(Long id) {
        return reviewRepository.findDetailsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review with ID " + id + " not found"));
    }
}
