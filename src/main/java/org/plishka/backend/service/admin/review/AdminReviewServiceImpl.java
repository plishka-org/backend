package org.plishka.backend.service.admin.review;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.domain.media.MediaTargetType;
import org.plishka.backend.domain.review.Review;
import org.plishka.backend.domain.review.ReviewMedia;
import org.plishka.backend.dto.admin.review.AdminReviewDetailDto;
import org.plishka.backend.dto.admin.review.AdminReviewFeaturedRequestDto;
import org.plishka.backend.dto.admin.review.AdminReviewRequestDto;
import org.plishka.backend.dto.admin.review.AdminReviewSearchRequestDto;
import org.plishka.backend.dto.admin.review.AdminReviewSummaryDto;
import org.plishka.backend.dto.common.PageResponse;
import org.plishka.backend.dto.file.AttachMediaRequestDto;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.mapper.review.ReviewMapper;
import org.plishka.backend.repository.review.ReviewMediaRepository;
import org.plishka.backend.repository.review.ReviewRepository;
import org.plishka.backend.service.admin.review.support.AdminReviewSpecifications;
import org.plishka.backend.service.file.MediaAttachmentService;
import org.plishka.backend.service.review.ReviewMediaQueryService;
import org.plishka.backend.service.storage.StorageDeletionOutboxService;
import org.plishka.backend.util.UserInputNormalizer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminReviewServiceImpl implements AdminReviewService {
    private static final Sort REVIEWS_SORT = Sort.by(
            Sort.Order.desc("createdAt"),
            Sort.Order.desc("id")
    );
    private static final String REVIEW_NOT_FOUND_MESSAGE = "Review with ID %d not found";
    private static final String FEATURED_LIMIT_MESSAGE =
            "Home page cannot contain more than %d featured reviews";
    private static final int FEATURED_REVIEWS_LIMIT = 5;

    private final ReviewRepository reviewRepository;
    private final ReviewMediaRepository reviewMediaRepository;
    private final ReviewMediaQueryService reviewMediaQueryService;
    private final MediaAttachmentService mediaAttachmentService;
    private final StorageDeletionOutboxService storageDeletionOutboxService;
    private final ReviewMapper reviewMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminReviewSummaryDto> getReviews(AdminReviewSearchRequestDto request, int page, int size) {
        Specification<Review> searchSpecification = AdminReviewSpecifications.bySearch(request.search());
        Page<Review> reviewPage = reviewRepository.findAll(
                searchSpecification,
                PageRequest.of(page, size, REVIEWS_SORT)
        );
        Map<Long, ReviewMedia> primaryMediaByReviewId = reviewMediaQueryService.findPrimaryMediaForReviews(
                reviewPage.getContent()
        );

        List<AdminReviewSummaryDto> content = reviewPage.getContent().stream()
                .map(review -> reviewMapper.toAdminSummaryDto(
                        review,
                        primaryMediaByReviewId.get(review.getId())
                ))
                .toList();

        return PageResponse.from(reviewPage, content);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminReviewDetailDto getReview(Long reviewId) {
        return toAdminDetailDto(findReviewDetailsOrThrow(reviewId));
    }

    @Override
    @Transactional
    public AdminReviewDetailDto createReview(AdminReviewRequestDto request) {
        Review review = new Review();
        applyReviewState(review, request);
        review.setIsFeatured(false);

        return toAdminDetailDto(reviewRepository.saveAndFlush(review));
    }

    @Override
    @Transactional
    public AdminReviewDetailDto updateReview(Long reviewId, AdminReviewRequestDto request) {
        Review review = findReviewForUpdateOrThrow(reviewId);
        applyReviewState(review, request);

        return toAdminDetailDto(reviewRepository.saveAndFlush(review));
    }

    @Override
    @Transactional
    public void deleteReview(Long reviewId) {
        Review review = findReviewForUpdateOrThrow(reviewId);
        List<String> s3Keys = reviewMediaRepository.findS3KeysByReviewIdOrderById(reviewId);

        reviewRepository.delete(review);
        reviewRepository.flush();

        storageDeletionOutboxService.enqueueDeletes(s3Keys);
    }

    @Override
    @Transactional
    public AdminReviewDetailDto updateFeatured(Long reviewId, AdminReviewFeaturedRequestDto request) {
        Review review = findReviewForUpdateOrThrow(reviewId);
        boolean targetFeatured = request.featured();

        if (targetFeatured && !Boolean.TRUE.equals(review.getIsFeatured())) {
            requireFeaturedCapacityAvailable();
        }

        review.setIsFeatured(targetFeatured);

        return toAdminDetailDto(reviewRepository.saveAndFlush(review));
    }

    @Override
    @Transactional
    public void attachMedia(Long reviewId, AttachMediaRequestDto request) {
        mediaAttachmentService.attachMedia(MediaTargetType.REVIEW, reviewId, request.s3Key());
    }

    private void requireFeaturedCapacityAvailable() {
        List<Review> featuredReviews = reviewRepository.findAllFeaturedForUpdate();
        if (featuredReviews.size() >= FEATURED_REVIEWS_LIMIT) {
            throw new BadRequestException(
                    FEATURED_LIMIT_MESSAGE.formatted(FEATURED_REVIEWS_LIMIT)
            );
        }
    }

    private void applyReviewState(Review review, AdminReviewRequestDto request) {
        review.setAuthorName(UserInputNormalizer.normalizeName(request.authorName()));
        review.setContent(UserInputNormalizer.normalizeName(request.content()));
    }

    private AdminReviewDetailDto toAdminDetailDto(Review review) {
        List<ReviewMedia> media = review.getMedia() == null ? List.of() : review.getMedia();

        return reviewMapper.toAdminDetailDto(review, media);
    }

    private Review findReviewDetailsOrThrow(Long reviewId) {
        return reviewRepository.findDetailsById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException(REVIEW_NOT_FOUND_MESSAGE.formatted(reviewId)));
    }

    private Review findReviewForUpdateOrThrow(Long reviewId) {
        return reviewRepository.findByIdForUpdate(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException(REVIEW_NOT_FOUND_MESSAGE.formatted(reviewId)));
    }
}
