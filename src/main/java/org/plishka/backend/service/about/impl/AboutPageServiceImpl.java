package org.plishka.backend.service.about.impl;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.domain.about.AboutPageContent;
import org.plishka.backend.domain.about.AboutPageMedia;
import org.plishka.backend.domain.media.MediaType;
import org.plishka.backend.dto.about.AboutPageContentDto;
import org.plishka.backend.dto.about.AboutPageMediaDto;
import org.plishka.backend.dto.about.AboutPageResponse;
import org.plishka.backend.dto.file.AttachMediaRequestDto;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.repository.about.AboutPageContentRepository;
import org.plishka.backend.repository.about.AboutPageMediaRepository;
import org.plishka.backend.service.about.AboutPageService;
import org.plishka.backend.service.storage.ObjectStorageService;
import org.plishka.backend.service.storage.model.StorageObjectMetadata;
import org.plishka.backend.service.storage.validation.MediaFileTypeRules;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AboutPageServiceImpl implements AboutPageService {
    private static final long SINGLETON_CONTENT_ID = 1L;

    private final AboutPageContentRepository contentRepository;
    private final AboutPageMediaRepository mediaRepository;
    private final ObjectStorageService objectStorageService;
    private final MediaFileTypeRules mediaFileTypeRules;
    private final AboutPageMediaRepository aboutPageMediaRepository;

    @Override
    @Transactional(readOnly = true)
    public AboutPageResponse getAboutPageData() {
        log.debug("Fetching about page data");

        AboutPageContent content = findContentOrThrow();
        List<AboutPageMediaDto> media = getMediaForPage();

        AboutPageResponse response = new AboutPageResponse(
                new AboutPageContentDto(content.getHistoryText()),
                media
        );

        log.info("About page data fetched successfully: mediaCount={}", media.size());

        return response;
    }

    @Override
    @Transactional
    public void attachMedia(AttachMediaRequestDto request) {
        String s3Key = request.s3Key();

        if (aboutPageMediaRepository.existsByS3Key(s3Key)) {
            log.warn("Media with S3 key {} is already attached to About page", s3Key);
            throw new BadRequestException("This media file is already attached.");
        }

        StorageObjectMetadata metadata = objectStorageService.getObjectMetadata(s3Key);

        MediaType mediaType = mediaFileTypeRules.mediaTypeForContentType(metadata.contentType())
                .orElseThrow(() -> new BadRequestException("Unsupported media content type from S3"));

        Long aboutPageId = 1L;
        int nextDisplayOrder = aboutPageMediaRepository.findMaxDisplayOrderByAboutPageId(aboutPageId) + 1;

        AboutPageMedia media = new AboutPageMedia();
        media.setAboutPageId(aboutPageId);
        media.setS3Key(s3Key);
        media.setMediaType(mediaType.name());
        media.setDisplayOrder(nextDisplayOrder);

        aboutPageMediaRepository.save(media);

        try {
            objectStorageService.markObjectAsAttached(s3Key);
        } catch (Exception e) {
            log.warn("Failed to update S3 tags for {}. Tigris might not support tagging yet. Error: {}",
                    s3Key, e.getMessage());
        }

        log.info("Successfully attached media {} to About page with order {}", s3Key, nextDisplayOrder);
    }

    private AboutPageContent findContentOrThrow() {
        return contentRepository.findById(SINGLETON_CONTENT_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "About page content not found. Please verify database initialization."
                ));
    }

    private List<AboutPageMediaDto> getMediaForPage() {
        return mediaRepository.findAllByAboutPageIdOrderByDisplayOrderAsc(SINGLETON_CONTENT_ID)
                .stream()
                .map(this::mapToMediaDto)
                .toList();
    }

    private AboutPageMediaDto mapToMediaDto(AboutPageMedia media) {
        return new AboutPageMediaDto(
                media.getId(),
                media.getS3Key(),
                media.getMediaType()
        );
    }
}
