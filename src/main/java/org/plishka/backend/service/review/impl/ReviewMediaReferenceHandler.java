package org.plishka.backend.service.review.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.domain.media.MediaTargetType;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.repository.review.ReviewMediaRepository;
import org.plishka.backend.repository.review.ReviewRepository;
import org.plishka.backend.service.file.MediaReferenceHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewMediaReferenceHandler implements MediaReferenceHandler {
    private final ReviewRepository reviewRepository;
    private final ReviewMediaRepository reviewMediaRepository;

    @Override
    public MediaTargetType targetType() {
        return MediaTargetType.REVIEW;
    }

    @Override
    @Transactional(readOnly = true)
    public void assertParentExists(Long targetId) {
        log.debug("Verifying existence of Review with ID: {}", targetId);

        if (!reviewRepository.existsById(targetId)) {
            log.warn("Attempted to reference media for non-existent Review (ID: {})", targetId);
            throw new ResourceNotFoundException("Review with id " + targetId + " not found");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByS3Key(String s3Key) {
        log.debug("Checking if media file with S3 key is already attached to a Review: {}", s3Key);
        return reviewMediaRepository.existsByS3Key(s3Key);
    }
}
