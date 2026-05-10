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
import org.plishka.backend.service.storage.validation.S3ObjectKeyValidator;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final S3ObjectKeyValidator s3ObjectKeyValidator;

    @Override
    @Transactional(readOnly = true)
    public AboutPageResponse getAboutPageData() {
        log.debug("Fetching about page data");

        AboutPageContent content = findContentOrThrow();
        List<AboutPageMediaDto> media = getMediaForPage();

        AboutPageResponse response = new AboutPageResponse(
                new AboutPageContentDto(
                        content.getHistoryTitle(),
                        content.getHistoryText(),
                        content.getCurrentTitle(),
                        content.getCurrentText()
                ),
                media
        );

        log.info("About page data fetched successfully: mediaCount={}", media.size());

        return response;
    }

    @Override
    @Transactional
    public void attachMedia(AttachMediaRequestDto request) {
        S3ObjectKeyValidator.ValidatedS3ObjectKey validatedKey =
                s3ObjectKeyValidator.validateAndParseS3Key(request.s3Key());

        if (validatedKey.targetType() != org.plishka.backend.domain.media.MediaTargetType.ABOUT
                || !validatedKey.targetId().equals(SINGLETON_CONTENT_ID)) {
            throw new BadRequestException("S3 key does not match about target");
        }

        String s3Key = validatedKey.value();

        AboutPageContent content = contentRepository.findByIdForUpdate(SINGLETON_CONTENT_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "About page content not found. Please verify database initialization."
                ));

        if (mediaRepository.existsByS3Key(s3Key)) {
            log.warn("Media with S3 key {} is already attached to About page", s3Key);
            throw new BadRequestException("This media file is already attached.");
        }

        StorageObjectMetadata metadata = objectStorageService.getObjectMetadata(s3Key);

        MediaType mediaType = mediaFileTypeRules.mediaTypeForContentType(metadata.contentType())
                .orElseThrow(() -> new BadRequestException("Unsupported media content type from S3"));

        if (mediaType != validatedKey.mediaType()) {
            throw new BadRequestException("S3 key media type does not match file content type");
        }

        AboutPageMedia media = buildAboutPageMedia(content, s3Key, mediaType);

        try {
            mediaRepository.saveAndFlush(media);
        } catch (DataIntegrityViolationException exception) {
            log.warn("Failed to attach media {} because it conflicts with existing About media", s3Key, exception);
            throw new BadRequestException("This media file is already attached or media order has changed.");
        }

        objectStorageService.markObjectAsAttached(s3Key);

        log.info("Successfully attached media {} to About page with order {}", s3Key, media.getDisplayOrder());
    }

    private AboutPageContent findContentOrThrow() {
        return contentRepository.findById(SINGLETON_CONTENT_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "About page content not found. Please verify database initialization."
                ));
    }

    private List<AboutPageMediaDto> getMediaForPage() {
        return mediaRepository.findAllByAboutPage_IdOrderByDisplayOrderAsc(SINGLETON_CONTENT_ID)
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

    private AboutPageMedia buildAboutPageMedia(AboutPageContent content, String s3Key, MediaType mediaType) {
        int nextDisplayOrder = mediaRepository.findMaxDisplayOrderByAboutPageId(SINGLETON_CONTENT_ID) + 1;

        AboutPageMedia media = new AboutPageMedia();
        media.setAboutPage(content);
        media.setS3Key(s3Key);
        media.setMediaType(mediaType);
        media.setDisplayOrder(nextDisplayOrder);

        return media;
    }
}
