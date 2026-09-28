package org.plishka.backend.service.about.impl;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.plishka.backend.exception.RequiredSingletonUnavailableException;
import org.plishka.backend.mapper.about.AboutPageMapper;
import org.plishka.backend.repository.about.AboutPageContentRepository;
import org.plishka.backend.repository.about.AboutPageMediaRepository;
import org.plishka.backend.service.file.MediaAttachmentService;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AboutPageServiceImplTest {
    private static final long CONTENT_ID = 1L;

    @Mock
    private AboutPageContentRepository contentRepository;

    @Mock
    private AboutPageMediaRepository mediaRepository;

    @Mock
    private MediaAttachmentService mediaAttachmentService;

    @Mock
    private AboutPageMapper aboutPageMapper;

    @InjectMocks
    private AboutPageServiceImpl service;

    @Test
    void getAboutPageData_ShouldThrowOperationalException_WhenContentMissing() {
        when(contentRepository.findById(CONTENT_ID)).thenReturn(Optional.empty());

        assertThrows(RequiredSingletonUnavailableException.class, service::getAboutPageData);
    }
}
