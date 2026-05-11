package org.plishka.backend.service.about.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.domain.media.MediaTargetType;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.repository.about.AboutPageContentRepository;
import org.plishka.backend.repository.about.AboutPageMediaRepository;
import org.plishka.backend.service.file.MediaReferenceHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AboutMediaReferenceHandler implements MediaReferenceHandler {
    private final AboutPageContentRepository aboutPageContentRepository;
    private final AboutPageMediaRepository aboutPageMediaRepository;

    @Override
    public MediaTargetType targetType() {
        return MediaTargetType.ABOUT;
    }

    @Override
    @Transactional(readOnly = true)
    public void assertParentExists(Long targetId) {
        log.debug("Verifying existence of About page with ID: {}", targetId);

        if (!aboutPageContentRepository.existsById(targetId)) {
            log.warn("Attempted to reference media for non-existent About page (ID: {})", targetId);
            throw new ResourceNotFoundException("About page with id " + targetId + " not found");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByS3Key(String s3Key) {
        log.debug("Checking if media file with S3 key is already attached: {}", s3Key);
        return aboutPageMediaRepository.existsByS3Key(s3Key);
    }
}
