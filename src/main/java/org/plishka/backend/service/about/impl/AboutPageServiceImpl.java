package org.plishka.backend.service.about.impl;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.domain.about.AboutPageContent;
import org.plishka.backend.domain.about.AboutPageMedia;
import org.plishka.backend.domain.media.MediaTargetType;
import org.plishka.backend.dto.about.AboutPageResponse;
import org.plishka.backend.dto.file.AttachMediaRequestDto;
import org.plishka.backend.exception.RequiredSingletonUnavailableException;
import org.plishka.backend.mapper.about.AboutPageMapper;
import org.plishka.backend.repository.about.AboutPageContentRepository;
import org.plishka.backend.repository.about.AboutPageMediaRepository;
import org.plishka.backend.service.about.AboutPageService;
import org.plishka.backend.service.file.MediaAttachmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AboutPageServiceImpl implements AboutPageService {
    private static final long SINGLETON_CONTENT_ID = 1L;

    private final AboutPageContentRepository contentRepository;
    private final AboutPageMediaRepository mediaRepository;
    private final MediaAttachmentService mediaAttachmentService;
    private final AboutPageMapper aboutPageMapper;

    @Override
    @Transactional(readOnly = true)
    public AboutPageResponse getAboutPageData() {
        log.debug("Fetching about page data");

        AboutPageContent content = findContentOrThrow();
        List<AboutPageMedia> media = findAboutPageMedia();
        AboutPageResponse response = aboutPageMapper.toResponse(content, media);

        log.debug("About page data fetched successfully: mediaCount={}", media.size());

        return response;
    }

    @Override
    public void attachMedia(AttachMediaRequestDto request) {
        mediaAttachmentService.attachMedia(MediaTargetType.ABOUT, SINGLETON_CONTENT_ID, request.s3Key());
    }

    private AboutPageContent findContentOrThrow() {
        return contentRepository.findById(SINGLETON_CONTENT_ID)
                .orElseThrow(() -> new RequiredSingletonUnavailableException(
                        "About page content not found. Please verify database initialization."
                ));
    }

    private List<AboutPageMedia> findAboutPageMedia() {
        return mediaRepository.findAllByAboutPage_IdOrderByDisplayOrderAsc(SINGLETON_CONTENT_ID);
    }
}
