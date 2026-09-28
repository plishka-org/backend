package org.plishka.backend.service.admin.about;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.plishka.backend.domain.about.AboutPageContent;
import org.plishka.backend.domain.about.AboutPageMedia;
import org.plishka.backend.domain.media.MediaType;
import org.plishka.backend.dto.about.AboutPageContentDto;
import org.plishka.backend.dto.admin.about.AboutMediaOrderRequestDto;
import org.plishka.backend.dto.admin.about.AdminAboutPageContentRequestDto;
import org.plishka.backend.dto.admin.about.AdminAboutPageDto;
import org.plishka.backend.dto.admin.about.AdminAboutPageMediaDto;
import org.plishka.backend.dto.file.AttachMediaRequestDto;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.exception.RequiredSingletonUnavailableException;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.mapper.about.AboutPageMapper;
import org.plishka.backend.repository.about.AboutPageContentRepository;
import org.plishka.backend.repository.about.AboutPageMediaRepository;
import org.plishka.backend.service.about.AboutPageService;
import org.plishka.backend.service.storage.StorageDeletionOutboxService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAboutPageServiceImplTest {
    private static final long CONTENT_ID = 1L;
    private static final String ABOUT_MEDIA_KEY =
            "about/1/images/2026/05/7223994a-bf40-4cba-9f60-234162a211fa.jpg";
    private static final String ABOUT_FIRST_MEDIA_KEY =
            "about/1/images/2026/05/550e8400-e29b-41d4-a716-446655440010.jpg";
    private static final String ABOUT_SECOND_MEDIA_KEY =
            "about/1/images/2026/05/550e8400-e29b-41d4-a716-446655440011.jpg";

    @Mock
    private AboutPageContentRepository contentRepository;

    @Mock
    private AboutPageMediaRepository mediaRepository;

    @Mock
    private AboutPageMapper aboutPageMapper;

    @Mock
    private AboutPageService aboutPageService;

    @Mock
    private StorageDeletionOutboxService storageDeletionOutboxService;

    @InjectMocks
    private AdminAboutPageServiceImpl service;

    @Test
    void getAboutPage_ShouldReturnMappedAboutPage() {
        AboutPageContent content = content();
        AboutPageMedia mediaEntity = media(5L, 1, ABOUT_MEDIA_KEY);
        AboutPageContentDto contentDto = new AboutPageContentDto("Main", "Main sub", "Secondary", "Secondary sub");
        AdminAboutPageMediaDto mediaDto = new AdminAboutPageMediaDto(5L, ABOUT_MEDIA_KEY, MediaType.IMAGE, 1);

        when(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.of(content));
        when(mediaRepository.findAllByAboutPage_IdOrderByDisplayOrderAsc(CONTENT_ID)).thenReturn(List.of(mediaEntity));
        when(aboutPageMapper.toContentDto(content)).thenReturn(contentDto);
        when(aboutPageMapper.toAdminMediaDto(mediaEntity)).thenReturn(mediaDto);

        AdminAboutPageDto result = service.getAboutPage();

        assertEquals(contentDto, result.content());
        assertEquals(List.of(mediaDto), result.media());
    }

    @Test
    void updateAboutPageContent_ShouldPersistNormalizedContent() {
        AboutPageContent content = content();
        givenContentLocked(content);
        when(contentRepository.saveAndFlush(content)).thenReturn(content);
        when(aboutPageMapper.toContentDto(content)).thenReturn(
                new AboutPageContentDto("Main title", "Main subtitle", "Secondary title", "Secondary subtitle")
        );

        service.updateAboutPageContent(new AdminAboutPageContentRequestDto(
                "Main title",
                "Main subtitle",
                "Secondary title",
                "Secondary subtitle"
        ));

        assertEquals("Main title", content.getMainTitle());
        assertEquals("Main subtitle", content.getMainSubtitle());
        assertEquals("Secondary title", content.getSecondaryTitle());
        assertEquals("Secondary subtitle", content.getSecondarySubtitle());
        verify(aboutPageMapper).toContentDto(content);
    }

    @Test
    void getAboutPage_ShouldThrowOperationalException_WhenContentMissing() {
        when(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.empty());

        assertThrows(RequiredSingletonUnavailableException.class, service::getAboutPage);
    }

    @Test
    void updateAboutPageContent_ShouldThrowOperationalException_WhenContentMissing() {
        when(contentRepository.findByIdForUpdate(CONTENT_ID)).thenReturn(Optional.empty());

        assertThrows(
                RequiredSingletonUnavailableException.class,
                () -> service.updateAboutPageContent(new AdminAboutPageContentRequestDto(
                        "Main title",
                        "Main subtitle",
                        "Secondary title",
                        "Secondary subtitle"
                ))
        );
    }

    @Test
    void attachMedia_ShouldDelegateToAboutPageService() {
        AttachMediaRequestDto request = new AttachMediaRequestDto(ABOUT_MEDIA_KEY);

        service.attachMedia(request);

        verify(aboutPageService).attachMedia(request);
    }

    @Test
    void deleteMedia_ShouldEnqueueS3Deletion() {
        AboutPageMedia media = media(5L, 1, ABOUT_MEDIA_KEY);
        givenContentWithMedia(media);

        service.deleteMedia(5L);

        verify(mediaRepository).delete(media);
        verify(storageDeletionOutboxService).enqueueDelete(ABOUT_MEDIA_KEY);
    }

    @Test
    void deleteAllMedia_ShouldEnqueueAllS3Keys() {
        AboutPageMedia first = media(1L, 1, ABOUT_FIRST_MEDIA_KEY);
        AboutPageMedia second = media(2L, 2, ABOUT_SECOND_MEDIA_KEY);
        givenContentLocked();
        givenMediaForContent(List.of(first, second));

        service.deleteAllMedia();

        verify(mediaRepository).deleteAll(List.of(first, second));
        verify(storageDeletionOutboxService).enqueueDeletes(List.of(ABOUT_FIRST_MEDIA_KEY, ABOUT_SECOND_MEDIA_KEY));
    }

    @Test
    void reorderMedia_ShouldRejectDifferentMediaSet() {
        givenContentLocked();
        givenMediaForContent(List.of(media(1L, 1, ABOUT_FIRST_MEDIA_KEY)));

        assertThrows(
                BadRequestException.class,
                () -> service.reorderMedia(new AboutMediaOrderRequestDto(List.of(2L)))
        );

        verify(mediaRepository, never()).flush();
    }

    @Test
    void reorderMedia_ShouldApplyFinalOrder() {
        AboutPageMedia first = media(1L, 1, ABOUT_FIRST_MEDIA_KEY);
        AboutPageMedia second = media(2L, 2, ABOUT_SECOND_MEDIA_KEY);
        givenContentLocked();
        givenMediaForContent(List.of(first, second));

        service.reorderMedia(new AboutMediaOrderRequestDto(List.of(2L, 1L)));

        assertEquals(2, first.getDisplayOrder());
        assertEquals(1, second.getDisplayOrder());
        verify(mediaRepository, times(2)).flush();
    }

    @Test
    void deleteMedia_ShouldThrow_WhenMediaMissing() {
        givenContentLocked();
        givenMediaMissing();

        assertThrows(ResourceNotFoundException.class, () -> service.deleteMedia(99L));
        verify(storageDeletionOutboxService, never()).enqueueDelete(any());
    }

    @Test
    void deleteAllMedia_ShouldThrowOperationalException_WhenContentMissing() {
        when(contentRepository.findByIdForUpdate(CONTENT_ID)).thenReturn(Optional.empty());

        assertThrows(RequiredSingletonUnavailableException.class, service::deleteAllMedia);
        verify(storageDeletionOutboxService, never()).enqueueDeletes(any());
    }

    private void givenContentLocked() {
        givenContentLocked(content());
    }

    private void givenContentLocked(AboutPageContent content) {
        when(contentRepository.findByIdForUpdate(CONTENT_ID)).thenReturn(Optional.of(content));
    }

    private void givenMediaForContent(List<AboutPageMedia> media) {
        when(mediaRepository.findAllByAboutPageIdForUpdateOrderByDisplayOrder(CONTENT_ID)).thenReturn(media);
    }

    private void givenContentWithMedia(AboutPageMedia media) {
        givenContentLocked();
        when(mediaRepository.findByAboutPageIdAndIdForUpdate(CONTENT_ID, 5L))
                .thenReturn(Optional.of(media));
    }

    private void givenMediaMissing() {
        when(mediaRepository.findByAboutPageIdAndIdForUpdate(CONTENT_ID, 99L))
                .thenReturn(Optional.empty());
    }

    private static AboutPageContent content() {
        AboutPageContent content = new AboutPageContent();
        content.setId(CONTENT_ID);
        content.setMainTitle("Main");
        content.setMainSubtitle("Main sub");
        content.setSecondaryTitle("Secondary");
        content.setSecondarySubtitle("Secondary sub");
        return content;
    }

    private static AboutPageMedia media(Long id, int displayOrder, String s3Key) {
        AboutPageMedia media = new AboutPageMedia();
        media.setId(id);
        media.setDisplayOrder(displayOrder);
        media.setS3Key(s3Key);
        media.setMediaType(MediaType.IMAGE);
        return media;
    }
}
