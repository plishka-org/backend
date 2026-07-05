package org.plishka.backend.service.admin.about;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.domain.about.AboutPageContent;
import org.plishka.backend.domain.about.AboutPageMedia;
import org.plishka.backend.dto.about.AboutPageContentDto;
import org.plishka.backend.dto.admin.about.AboutMediaOrderRequestDto;
import org.plishka.backend.dto.admin.about.AdminAboutPageContentRequestDto;
import org.plishka.backend.dto.admin.about.AdminAboutPageDto;
import org.plishka.backend.dto.admin.about.AdminAboutPageMediaDto;
import org.plishka.backend.dto.file.AttachMediaRequestDto;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.mapper.about.AboutPageMapper;
import org.plishka.backend.repository.about.AboutPageContentRepository;
import org.plishka.backend.repository.about.AboutPageMediaRepository;
import org.plishka.backend.service.about.AboutPageService;
import org.plishka.backend.service.storage.StorageDeletionOutboxService;
import org.plishka.backend.util.EntityPresenceValidator;
import org.plishka.backend.util.UserInputNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminAboutPageServiceImpl implements AdminAboutPageService {
    private static final long SINGLETON_CONTENT_ID = 1L;
    private static final String CONTENT_NOT_FOUND_MESSAGE = "About page content not found";
    private static final String MEDIA_NOT_FOUND_MESSAGE = "About page media with ID %d not found";
    private static final String MEDIA_ORDER_MISMATCH_MESSAGE =
            "Media order must contain the current about page media ids";

    private final AboutPageContentRepository contentRepository;
    private final AboutPageMediaRepository mediaRepository;
    private final AboutPageMapper aboutPageMapper;
    private final AboutPageService aboutPageService;
    private final StorageDeletionOutboxService storageDeletionOutboxService;

    @Override
    @Transactional(readOnly = true)
    public AdminAboutPageDto getAboutPage() {
        AboutPageContent content = findContentOrThrow();
        List<AdminAboutPageMediaDto> media =
                mediaRepository.findAllByAboutPage_IdOrderByDisplayOrderAsc(SINGLETON_CONTENT_ID)
                        .stream()
                        .map(aboutPageMapper::toAdminMediaDto)
                        .toList();

        return new AdminAboutPageDto(aboutPageMapper.toContentDto(content), media);
    }

    @Override
    @Transactional
    public AboutPageContentDto updateAboutPageContent(AdminAboutPageContentRequestDto request) {
        AboutPageContent content = findContentForUpdateOrThrow();
        content.setMainTitle(UserInputNormalizer.normalizeName(request.mainTitle()));
        content.setMainSubtitle(UserInputNormalizer.normalizeName(request.mainSubtitle()));
        content.setSecondaryTitle(UserInputNormalizer.normalizeName(request.secondaryTitle()));
        content.setSecondarySubtitle(UserInputNormalizer.normalizeName(request.secondarySubtitle()));

        return aboutPageMapper.toContentDto(contentRepository.saveAndFlush(content));
    }

    @Override
    @Transactional
    public void attachMedia(AttachMediaRequestDto request) {
        aboutPageService.attachMedia(request);
    }

    @Override
    @Transactional
    public void deleteMedia(Long mediaId) {
        lockContentOrThrow();
        AboutPageMedia media = findMediaForUpdateOrThrow(mediaId);
        String s3Key = media.getS3Key();

        mediaRepository.delete(media);
        mediaRepository.flush();

        storageDeletionOutboxService.enqueueDelete(s3Key);
    }

    @Override
    @Transactional
    public void deleteAllMedia() {
        lockContentOrThrow();
        List<AboutPageMedia> media =
                mediaRepository.findAllByAboutPageIdForUpdateOrderByDisplayOrder(SINGLETON_CONTENT_ID);
        if (media.isEmpty()) {
            return;
        }

        List<String> s3Keys = media.stream()
                .map(AboutPageMedia::getS3Key)
                .toList();

        mediaRepository.deleteAll(media);
        mediaRepository.flush();
        storageDeletionOutboxService.enqueueDeletes(s3Keys);
    }

    @Override
    @Transactional
    public void reorderMedia(AboutMediaOrderRequestDto request) {
        lockContentOrThrow();
        List<Long> mediaIds = requireUniqueMediaIds(request.mediaIds());
        List<AboutPageMedia> currentMedia =
                mediaRepository.findAllByAboutPageIdForUpdateOrderByDisplayOrder(SINGLETON_CONTENT_ID);
        Set<Long> currentIds = currentMedia.stream()
                .map(AboutPageMedia::getId)
                .collect(Collectors.toSet());

        if (!currentIds.equals(Set.copyOf(mediaIds))) {
            throw new BadRequestException(MEDIA_ORDER_MISMATCH_MESSAGE);
        }

        if (mediaIds.isEmpty()) {
            return;
        }

        Map<Long, AboutPageMedia> mediaById = currentMedia.stream()
                .collect(Collectors.toMap(AboutPageMedia::getId, Function.identity()));
        EntityPresenceValidator.requireAllIdsFound(mediaIds, mediaById.keySet(), "About page media");

        applyTemporaryDisplayOrders(mediaIds, mediaById);
        applyFinalDisplayOrders(mediaIds, mediaById);
        mediaRepository.flush();
    }

    private void applyTemporaryDisplayOrders(List<Long> mediaIds, Map<Long, AboutPageMedia> mediaById) {
        for (int index = 0; index < mediaIds.size(); index++) {
            AboutPageMedia media = mediaById.get(mediaIds.get(index));
            media.setDisplayOrder(-(index + 1));
        }
        mediaRepository.flush();
    }

    private void applyFinalDisplayOrders(List<Long> mediaIds, Map<Long, AboutPageMedia> mediaById) {
        for (int index = 0; index < mediaIds.size(); index++) {
            AboutPageMedia media = mediaById.get(mediaIds.get(index));
            media.setDisplayOrder(index + 1);
        }
    }

    private List<Long> requireUniqueMediaIds(List<Long> mediaIds) {
        if (mediaIds == null) {
            throw new BadRequestException("Media ids are required");
        }

        if (mediaIds.stream().anyMatch(Objects::isNull)) {
            throw new BadRequestException("Media ids must not contain null values");
        }

        Set<Long> uniqueIds = new HashSet<>(mediaIds);
        if (uniqueIds.size() != mediaIds.size()) {
            throw new BadRequestException("Media ids must be unique");
        }

        return List.copyOf(mediaIds);
    }

    private AboutPageContent findContentOrThrow() {
        return contentRepository.findById(SINGLETON_CONTENT_ID)
                .orElseThrow(() -> new ResourceNotFoundException(CONTENT_NOT_FOUND_MESSAGE));
    }

    private AboutPageContent findContentForUpdateOrThrow() {
        return contentRepository.findByIdForUpdate(SINGLETON_CONTENT_ID)
                .orElseThrow(() -> new ResourceNotFoundException(CONTENT_NOT_FOUND_MESSAGE));
    }

    private void lockContentOrThrow() {
        findContentForUpdateOrThrow();
    }

    private AboutPageMedia findMediaForUpdateOrThrow(Long mediaId) {
        return mediaRepository.findByAboutPageIdAndIdForUpdate(SINGLETON_CONTENT_ID, mediaId)
                .orElseThrow(() -> new ResourceNotFoundException(MEDIA_NOT_FOUND_MESSAGE.formatted(mediaId)));
    }
}
