package org.plishka.backend.service.admin.review;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.domain.media.MediaType;
import org.plishka.backend.domain.review.ReviewMedia;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.repository.review.ReviewMediaRepository;
import org.plishka.backend.repository.review.ReviewRepository;
import org.plishka.backend.service.storage.StorageDeletionOutboxService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminReviewMediaServiceImpl implements AdminReviewMediaService {
    private final ReviewRepository reviewRepository;
    private final ReviewMediaRepository reviewMediaRepository;
    private final StorageDeletionOutboxService storageDeletionOutboxService;

    @Override
    @Transactional
    public void deleteMedia(Long reviewId, Long mediaId) {
        lockReviewOrThrow(reviewId);
        ReviewMedia media = findMediaForUpdateOrThrow(reviewId, mediaId);
        final String s3Key = media.getS3Key();
        boolean primaryDeleted = Boolean.TRUE.equals(media.getIsPrimary());

        reviewMediaRepository.delete(media);
        reviewMediaRepository.flush();

        if (primaryDeleted) {
            promoteNextPrimaryImage(reviewId);
        }

        storageDeletionOutboxService.enqueueDelete(s3Key);
    }

    @Override
    @Transactional
    public void deleteAllMedia(Long reviewId) {
        lockReviewOrThrow(reviewId);
        List<ReviewMedia> media = reviewMediaRepository.findAllByReviewIdForUpdateOrderByDisplayOrder(reviewId);
        if (media.isEmpty()) {
            return;
        }

        List<String> s3Keys = media.stream()
                .map(ReviewMedia::getS3Key)
                .toList();

        reviewMediaRepository.deleteAll(media);
        reviewMediaRepository.flush();
        storageDeletionOutboxService.enqueueDeletes(s3Keys);
    }

    @Override
    @Transactional
    public void markPrimary(Long reviewId, Long mediaId) {
        lockReviewOrThrow(reviewId);
        ReviewMedia media = findMediaForUpdateOrThrow(reviewId, mediaId);
        if (media.getMediaType() != MediaType.IMAGE) {
            throw new BadRequestException("Only IMAGE media can be primary");
        }

        if (Boolean.TRUE.equals(media.getIsPrimary())) {
            return;
        }

        reviewMediaRepository.findPrimaryByReviewIdForUpdate(reviewId)
                .ifPresent(currentPrimary -> {
                    currentPrimary.setIsPrimary(false);
                    reviewMediaRepository.saveAndFlush(currentPrimary);
                });

        media.setIsPrimary(true);
        reviewMediaRepository.saveAndFlush(media);
    }

    private void lockReviewOrThrow(Long reviewId) {
        reviewRepository.findByIdForUpdate(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review with ID " + reviewId + " not found"));
    }

    private ReviewMedia findMediaForUpdateOrThrow(Long reviewId, Long mediaId) {
        return reviewMediaRepository.findByReviewIdAndIdForUpdate(reviewId, mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Review media with ID " + mediaId + " not found"));
    }

    private void promoteNextPrimaryImage(Long reviewId) {
        reviewMediaRepository.findImagesByReviewIdForUpdateOrderByDisplayOrder(reviewId)
                .stream()
                .findFirst()
                .ifPresent(nextPrimary -> {
                    nextPrimary.setIsPrimary(true);
                    reviewMediaRepository.saveAndFlush(nextPrimary);
                });
    }
}
