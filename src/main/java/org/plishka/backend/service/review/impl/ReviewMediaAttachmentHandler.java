package org.plishka.backend.service.review.impl;

import lombok.RequiredArgsConstructor;
import org.plishka.backend.domain.media.MediaTargetType;
import org.plishka.backend.domain.media.MediaType;
import org.plishka.backend.domain.review.Review;
import org.plishka.backend.domain.review.ReviewMedia;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.repository.review.ReviewMediaRepository;
import org.plishka.backend.repository.review.ReviewRepository;
import org.plishka.backend.service.file.MediaAttachmentHandler;
import org.plishka.backend.service.file.PrimaryMediaAttachmentPolicy;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReviewMediaAttachmentHandler implements MediaAttachmentHandler {
    private final ReviewRepository reviewRepository;
    private final ReviewMediaRepository reviewMediaRepository;
    private final PrimaryMediaAttachmentPolicy primaryMediaAttachmentPolicy;

    @Override
    public MediaTargetType targetType() {
        return MediaTargetType.REVIEW;
    }

    @Override
    public String targetName() {
        return "review";
    }

    @Override
    public boolean existsByS3Key(String s3Key) {
        return reviewMediaRepository.existsByS3Key(s3Key);
    }

    @Override
    public void attachValidatedMedia(Long targetId, String s3Key, MediaType mediaType) {
        Review review = lockReviewOrThrow(targetId);
        ReviewMedia media = buildReviewMedia(review, s3Key, mediaType);

        reviewMediaRepository.saveAndFlush(media);
    }

    private Review lockReviewOrThrow(Long reviewId) {
        return reviewRepository.findByIdForUpdate(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review with ID " + reviewId + " not found"));
    }

    private ReviewMedia buildReviewMedia(Review review, String s3Key, MediaType mediaType) {
        boolean hasPrimaryMedia = reviewMediaRepository.existsByReview_IdAndIsPrimaryTrue(review.getId());
        boolean isPrimary = primaryMediaAttachmentPolicy.shouldMarkAsPrimary(mediaType, hasPrimaryMedia);
        int nextDisplayOrder = reviewMediaRepository.findMaxDisplayOrderByReviewId(review.getId()) + 1;

        ReviewMedia media = new ReviewMedia();
        media.setReview(review);
        media.setS3Key(s3Key);
        media.setMediaType(mediaType);
        media.setDisplayOrder(nextDisplayOrder);
        media.setIsPrimary(isPrimary);

        return media;
    }
}
