package org.plishka.backend.service.about.impl;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.plishka.backend.domain.media.MediaType;
import org.plishka.backend.exception.RequiredSingletonUnavailableException;
import org.plishka.backend.repository.about.AboutPageContentRepository;
import org.plishka.backend.repository.about.AboutPageMediaRepository;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AboutMediaAttachmentHandlerTest {
    private static final long CONTENT_ID = 1L;
    private static final String S3_KEY =
            "about/1/images/2026/05/7223994a-bf40-4cba-9f60-234162a211fa.jpg";

    @Mock
    private AboutPageContentRepository contentRepository;

    @Mock
    private AboutPageMediaRepository mediaRepository;

    @InjectMocks
    private AboutMediaAttachmentHandler handler;

    @Test
    void attachValidatedMedia_ShouldThrowOperationalException_WhenContentMissing() {
        when(contentRepository.findByIdForUpdate(CONTENT_ID)).thenReturn(Optional.empty());

        assertThrows(
                RequiredSingletonUnavailableException.class,
                () -> handler.attachValidatedMedia(CONTENT_ID, S3_KEY, MediaType.IMAGE)
        );

        verify(contentRepository).findByIdForUpdate(CONTENT_ID);
    }
}
