package org.plishka.backend.service.about.impl;

import lombok.RequiredArgsConstructor;
import org.plishka.backend.domain.about.AboutPageContent;
import org.plishka.backend.domain.about.AboutPageMedia;
import org.plishka.backend.domain.media.MediaTargetType;
import org.plishka.backend.domain.media.MediaType;
import org.plishka.backend.exception.RequiredSingletonUnavailableException;
import org.plishka.backend.repository.about.AboutPageContentRepository;
import org.plishka.backend.repository.about.AboutPageMediaRepository;
import org.plishka.backend.service.file.MediaAttachmentHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AboutMediaAttachmentHandler implements MediaAttachmentHandler {
    private final AboutPageContentRepository contentRepository;
    private final AboutPageMediaRepository mediaRepository;

    @Override
    public MediaTargetType targetType() {
        return MediaTargetType.ABOUT;
    }

    @Override
    public String targetName() {
        return "about";
    }

    @Override
    public String targetDescription(Long targetId) {
        return "About page";
    }

    @Override
    public boolean existsByS3Key(String s3Key) {
        return mediaRepository.existsByS3Key(s3Key);
    }

    @Override
    public void attachValidatedMedia(Long targetId, String s3Key, MediaType mediaType) {
        AboutPageContent content = lockContentOrThrow(targetId);
        AboutPageMedia media = buildAboutPageMedia(content, s3Key, mediaType);

        mediaRepository.saveAndFlush(media);
    }

    private AboutPageContent lockContentOrThrow(Long targetId) {
        return contentRepository.findByIdForUpdate(targetId)
                .orElseThrow(() -> new RequiredSingletonUnavailableException(
                        "About page content not found. Please verify database initialization."
                ));
    }

    private AboutPageMedia buildAboutPageMedia(AboutPageContent content, String s3Key, MediaType mediaType) {
        int nextDisplayOrder = mediaRepository.findMaxDisplayOrderByAboutPageId(content.getId()) + 1;

        AboutPageMedia media = new AboutPageMedia();
        media.setAboutPage(content);
        media.setS3Key(s3Key);
        media.setMediaType(mediaType);
        media.setDisplayOrder(nextDisplayOrder);

        return media;
    }
}
