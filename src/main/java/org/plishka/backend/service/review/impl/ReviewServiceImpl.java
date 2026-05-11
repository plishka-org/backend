package org.plishka.backend.service.review.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.domain.media.MediaTargetType;
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
import org.plishka.backend.service.storage.validation.S3ObjectKeyValidator;
import org.plishka.backend.service.storage.validation.S3ObjectKeyValidator.ValidatedS3ObjectKey;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final S3ObjectKeyValidator s3ObjectKeyValidator;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ReviewDto> getReviews(int page, int size) {
        log.debug("Fetching reviews: page={}, size={}", page, size);

        Page<Review> reviewPage = reviewRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(page, size)
        );

        List<Long> reviewIds = reviewPage.getContent().stream()
                .map(Review::getId)
                .toList();

        Map<Long, List<ReviewMedia>> mediaByReviewId = reviewMediaRepository
                .findAllByReview_IdInOrderByDisplayOrderAsc(reviewIds)
                .stream()
                .collect(Collectors.groupingBy(m -> m.getReview().getId()));

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

        List<ReviewDto> result = reviewRepository.findAllByIsFeaturedTrueOrderByCreatedAtDesc()
                .stream()
                .map(review -> mapToReviewDto(review, review.getMedia()))
                .toList();

        log.info("Successfully fetched {} featured reviews", result.size());

        return result;
    }

    @Override
    @Transactional
    public void attachMedia(Long reviewId, AttachMediaRequestDto request) {
        ValidatedS3ObjectKey s3ObjectKey = validateReviewMediaKey(reviewId, request.s3Key());
        Review review = lockReviewOrThrow(reviewId);
        requireMediaNotAttached(s3ObjectKey.value(), reviewId);

        MediaType mediaType = resolveAndValidateMediaType(s3ObjectKey);
        ReviewMedia media = buildReviewMedia(review, s3ObjectKey.value(), mediaType);

        saveMediaOrThrowConflict(media, s3ObjectKey.value(), reviewId);
        objectStorageService.markObjectAsAttached(s3ObjectKey.value());

        log.info("Successfully attached media {} to Review {} with primary status: {}",
                s3ObjectKey.value(), reviewId, media.getIsPrimary());
    }

    private ValidatedS3ObjectKey validateReviewMediaKey(Long reviewId, String s3Key) {
        ValidatedS3ObjectKey validatedKey = s3ObjectKeyValidator.validateAndParseS3Key(s3Key);

        if (validatedKey.targetType() != MediaTargetType.REVIEW || !validatedKey.targetId().equals(reviewId)) {
            throw new BadRequestException("S3 key does not match review target");
        }

        return validatedKey;
    }

    private Review lockReviewOrThrow(Long reviewId) {
        return reviewRepository.findByIdForUpdate(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review with ID " + reviewId + " not found"));
    }

    private void requireMediaNotAttached(String s3Key, Long reviewId) {
        if (reviewMediaRepository.existsByS3Key(s3Key)) {
            log.warn("Media with S3 key {} is already attached to Review {}", s3Key, reviewId);
            throw new BadRequestException("This media file is already attached.");
        }
    }

    private MediaType resolveAndValidateMediaType(ValidatedS3ObjectKey s3ObjectKey) {
        StorageObjectMetadata metadata = objectStorageService.getObjectMetadata(s3ObjectKey.value());
        MediaType mediaType = mediaFileTypeRules.mediaTypeForContentType(metadata.contentType())
                .orElseThrow(() -> new BadRequestException("Unsupported media content type from S3"));

        if (mediaType != s3ObjectKey.mediaType()) {
            throw new BadRequestException("S3 key media type does not match file content type");
        }

        return mediaType;
    }

    private void saveMediaOrThrowConflict(ReviewMedia media, String s3Key, Long reviewId) {
        try {
            reviewMediaRepository.saveAndFlush(media);
        } catch (DataIntegrityViolationException exception) {
            log.warn("Failed to attach media {} because it conflicts with existing Review {} media",
                    s3Key, reviewId, exception);
            throw new BadRequestException("This media file is already attached or media order has changed.");
        }
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

    private ReviewMedia buildReviewMedia(Review review, String s3Key, MediaType mediaType) {
        int nextDisplayOrder = reviewMediaRepository.findMaxDisplayOrderByReviewId(review.getId()) + 1;
        boolean isFirstMedia = (nextDisplayOrder == 1);

        ReviewMedia media = new ReviewMedia();
        media.setReview(review);
        media.setS3Key(s3Key);
        media.setMediaType(mediaType);
        media.setDisplayOrder(nextDisplayOrder);
        media.setIsPrimary(isFirstMedia);

        return media;
    }
}
