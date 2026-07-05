package org.plishka.backend.service.admin.home;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.plishka.backend.domain.home.HomePageContent;
import org.plishka.backend.dto.admin.home.AdminHomePageContentRequestDto;
import org.plishka.backend.mapper.home.HomePageMapper;
import org.plishka.backend.repository.home.HomePageContentRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminHomePageServiceImplTest {
    private static final long CONTENT_ID = 1L;

    @Mock
    private HomePageContentRepository contentRepository;

    @Mock
    private HomePageMapper homePageMapper;

    @InjectMocks
    private AdminHomePageServiceImpl service;

    @Test
    void updateHomePageContent_ShouldPersistNormalizedContent() {
        HomePageContent content = content();
        givenContentLocked(content);
        when(contentRepository.saveAndFlush(content)).thenReturn(content);

        service.updateHomePageContent(new AdminHomePageContentRequestDto("New title", "New description"));

        assertEquals("New title", content.getTitle());
        assertEquals("New description", content.getDescription());
    }

    private void givenContentLocked(HomePageContent content) {
        when(contentRepository.findByIdForUpdate(CONTENT_ID)).thenReturn(Optional.of(content));
    }

    private static HomePageContent content() {
        HomePageContent content = new HomePageContent();
        content.setId(CONTENT_ID);
        content.setTitle("Old title");
        content.setDescription("Old description");
        return content;
    }
}
