package org.plishka.backend.service.review.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.domain.media.MediaType;
import org.plishka.backend.domain.review.Review;
import org.plishka.backend.domain.review.ReviewMedia;
import org.plishka.backend.dto.common.PageResponse;
import org.plishka.backend.dto.file.AttachMediaRequestDto;
import org.plishka.backend.dto.review.ReviewDto;
import org.plishka.backend.dto.review.ReviewMediaDto;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.repository.review.ReviewMediaRepository;
import org.plishka.backend.repository.review.ReviewRepository;
import org.plishka.backend.service.review.ReviewService;
import org.plishka.backend.service.storage.ObjectStorageService;
import org.plishka.backend.service.storage.model.StorageObjectMetadata;
import org.plishka.backend.service.storage.validation.MediaFileTypeRules;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {
    private final ReviewRepository reviewRepository;
    private final ReviewMediaRepository reviewMediaRepository;
    private final ObjectStorageService objectStorageService;
    private final MediaFileTypeRules mediaFileTypeRules;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewDto> getApprovedReviews(int page, int size) {
        log.debug("Fetching approved reviews: page={}, size={}", page, size);

        Page<Review> reviewPage = reviewRepository.findAllByIsApprovedTrueOrderByCreatedAtDesc(
                PageRequest.of(page, size)
        );

        List<Long> reviewIds = reviewPage.getContent().stream()
                .map(Review::getId)
                .toList();

        Map<Long, List<ReviewMedia>> mediaByReviewId = reviewMediaRepository
                .findAllByReviewIdInOrderByDisplayOrderAsc(reviewIds)
                .stream()
                .collect(Collectors.groupingBy(ReviewMedia::getReviewId));

        List<ReviewDto> content = reviewPage.getContent().stream()
                .map(review -> mapToReviewDto(review, mediaByReviewId.getOrDefault(review.getId(), List.of())))
                .toList();

        log.info("Successfully fetched {} reviews", content.size());

        return new PageResponse<>(
                content,
                reviewPage.getNumber(),
                reviewPage.getSize(),
                reviewPage.getTotalElements(),
                reviewPage.getTotalPages(),
                reviewPage.isLast()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewDto> getFeaturedReviews() {
        log.debug("Fetching featured reviews");

        List<Review> featuredReviews = reviewRepository.findAllByIsApprovedTrueAndIsFeaturedTrueOrderByCreatedAtDesc();

        if (featuredReviews.isEmpty()) {
            return List.of();
        }

        List<Long> reviewIds = featuredReviews.stream()
                .map(Review::getId)
                .toList();

        Map<Long, List<ReviewMedia>> mediaByReviewId = reviewMediaRepository
                .findAllByReviewIdInOrderByDisplayOrderAsc(reviewIds)
                .stream()
                .collect(Collectors.groupingBy(ReviewMedia::getReviewId));

        List<ReviewDto> result = featuredReviews.stream()
                .map(review -> mapToReviewDto(review, mediaByReviewId.getOrDefault(review.getId(), List.of())))
                .toList();

        log.info("Successfully fetched {} featured reviews", result.size());

        return result;
    }

    @Override
    @Transactional
    public void attachMedia(Long reviewId, AttachMediaRequestDto request) {
        String s3Key = request.s3Key();

        if (!reviewRepository.existsById(reviewId)) {
            throw new ResourceNotFoundException("Review with ID " + reviewId + " not found");
        }

        if (reviewMediaRepository.existsByS3Key(s3Key)) {
            log.warn("Media with S3 key {} is already attached to Review {}", s3Key, reviewId);
            throw new BadRequestException("This media file is already attached.");
        }

        StorageObjectMetadata metadata = objectStorageService.getObjectMetadata(s3Key);

        MediaType mediaType = mediaFileTypeRules.mediaTypeForContentType(metadata.contentType())
                .orElseThrow(() -> new BadRequestException("Unsupported media content type from S3"));

        int nextDisplayOrder = reviewMediaRepository.findMaxDisplayOrderByReviewId(reviewId) + 1;
        boolean isFirstMedia = (nextDisplayOrder == 1);

        ReviewMedia media = new ReviewMedia();
        media.setReviewId(reviewId);
        media.setS3Key(s3Key);
        media.setMediaType(mediaType.name());
        media.setDisplayOrder(nextDisplayOrder);
        media.setIsPrimary(isFirstMedia);

        reviewMediaRepository.save(media);

        try {
            objectStorageService.markObjectAsAttached(s3Key);
        } catch (Exception e) {
            log.warn("Failed to update S3 tags for {}. Tigris might not support tagging yet. Error: {}",
                    s3Key, e.getMessage());
        }

        log.info("Successfully attached media {} to Review {} with primary status: {}", s3Key, reviewId, isFirstMedia);
    }

    private ReviewDto mapToReviewDto(Review review, List<ReviewMedia> mediaList) {
        List<ReviewMediaDto> mediaDtos = mediaList.stream()
                .map(m -> new ReviewMediaDto(
                        m.getId(),
                        m.getS3Key(),
                        m.getMediaType(),
                        m.getIsPrimary()
                ))
                .toList();

        return new ReviewDto(
                review.getId(),
                review.getAuthorName(),
                review.getContent(),
                review.getCreatedAt(),
                mediaDtos
        );
    }
}
