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
        AboutPageMedia mediaEntity = media(5L, 1, "about/1/images/file.jpg");
        AboutPageContentDto contentDto = new AboutPageContentDto("History", "History text", "Current", "Current text");
        AdminAboutPageMediaDto mediaDto = new AdminAboutPageMediaDto(5L, "about/1/images/file.jpg", MediaType.IMAGE, 1);

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
                new AboutPageContentDto("History title", "History text", "Current title", "Current text")
        );

        service.updateAboutPageContent(new AdminAboutPageContentRequestDto(
                "  History title  ",
                "  History text  ",
                "  Current title  ",
                "  Current text  "
        ));

        assertEquals("History title", content.getHistoryTitle());
        assertEquals("History text", content.getHistoryText());
        assertEquals("Current title", content.getCurrentTitle());
        assertEquals("Current text", content.getCurrentText());
        verify(aboutPageMapper).toContentDto(content);
    }

    @Test
    void attachMedia_ShouldDelegateToAboutPageService() {
        AttachMediaRequestDto request = new AttachMediaRequestDto("about/1/images/file.jpg");

        service.attachMedia(request);

        verify(aboutPageService).attachMedia(request);
    }

    @Test
    void deleteMedia_ShouldEnqueueS3Deletion() {
        AboutPageMedia media = media(5L, 1, "about/1/images/file.jpg");
        givenContentWithMedia(media);

        service.deleteMedia(5L);

        verify(mediaRepository).delete(media);
        verify(storageDeletionOutboxService).enqueueDelete("about/1/images/file.jpg");
    }

    @Test
    void deleteAllMedia_ShouldEnqueueAllS3Keys() {
        AboutPageMedia first = media(1L, 1, "about/1/images/first.jpg");
        AboutPageMedia second = media(2L, 2, "about/1/images/second.jpg");
        givenContentLocked();
        givenMediaForContent(List.of(first, second));

        service.deleteAllMedia();

        verify(mediaRepository).deleteAll(List.of(first, second));
        verify(storageDeletionOutboxService).enqueueDeletes(List.of("about/1/images/first.jpg", "about/1/images/second.jpg"));
    }

    @Test
    void reorderMedia_ShouldRejectDifferentMediaSet() {
        givenContentLocked();
        givenMediaForContent(List.of(media(1L, 1, "about/1/images/first.jpg")));

        assertThrows(
                BadRequestException.class,
                () -> service.reorderMedia(new AboutMediaOrderRequestDto(List.of(2L)))
        );

        verify(mediaRepository, never()).flush();
    }

    @Test
    void reorderMedia_ShouldApplyFinalOrder() {
        AboutPageMedia first = media(1L, 1, "about/1/images/first.jpg");
        AboutPageMedia second = media(2L, 2, "about/1/images/second.jpg");
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
        content.setHistoryTitle("History");
        content.setHistoryText("History text");
        content.setCurrentTitle("Current");
        content.setCurrentText("Current text");
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
