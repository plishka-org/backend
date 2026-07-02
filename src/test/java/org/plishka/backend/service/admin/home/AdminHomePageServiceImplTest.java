package org.plishka.backend.service.admin.home;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.plishka.backend.domain.home.HomePageAdvantage;
import org.plishka.backend.domain.home.HomePageContent;
import org.plishka.backend.dto.admin.home.AdminHomePageAdvantageDto;
import org.plishka.backend.dto.admin.home.AdminHomePageAdvantageRequestDto;
import org.plishka.backend.dto.admin.home.AdminHomePageContentRequestDto;
import org.plishka.backend.dto.admin.home.HomeAdvantagesOrderRequestDto;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.mapper.home.HomePageMapper;
import org.plishka.backend.repository.home.HomePageAdvantageRepository;
import org.plishka.backend.repository.home.HomePageContentRepository;
import org.plishka.backend.service.storage.StorageDeletionOutboxService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminHomePageServiceImplTest {
    private static final long CONTENT_ID = 1L;

    @Mock
    private HomePageContentRepository contentRepository;

    @Mock
    private HomePageAdvantageRepository advantageRepository;

    @Mock
    private HomePageMapper homePageMapper;

    @Mock
    private StorageDeletionOutboxService storageDeletionOutboxService;

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

    @Test
    void createAdvantage_ShouldAssignNextDisplayOrder() {
        when(advantageRepository.count()).thenReturn(1L);
        when(advantageRepository.findMaxDisplayOrder()).thenReturn(2);
        when(advantageRepository.saveAndFlush(any(HomePageAdvantage.class))).thenAnswer(invocation -> {
            HomePageAdvantage advantage = invocation.getArgument(0);
            advantage.setId(10L);
            return advantage;
        });

        AdminHomePageAdvantageRequestDto request =
                new AdminHomePageAdvantageRequestDto("Fast delivery", "Within 3 days", "icons/fast.png");
        service.createAdvantage(request);

        ArgumentCaptor<HomePageAdvantage> advantageCaptor = ArgumentCaptor.forClass(HomePageAdvantage.class);
        verify(advantageRepository).saveAndFlush(advantageCaptor.capture());
        HomePageAdvantage savedAdvantage = advantageCaptor.getValue();
        assertEquals("Fast delivery", savedAdvantage.getTitle());
        assertEquals("Within 3 days", savedAdvantage.getDescription());
        assertEquals("icons/fast.png", savedAdvantage.getIconS3Key());
        assertEquals(3, savedAdvantage.getDisplayOrder());
    }

    @Test
    void createAdvantage_ShouldRejectWhenLimitReached() {
        when(advantageRepository.count()).thenReturn(20L);

        assertThrows(
                BadRequestException.class,
                () -> service.createAdvantage(new AdminHomePageAdvantageRequestDto("Title", null, null))
        );

        verify(advantageRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateAdvantage_ShouldEnqueuePreviousIconDeletion_WhenIconChanges() {
        HomePageAdvantage advantage = advantage(5L, 1, "icons/old.png", "Title");
        givenAdvantageLocked(5L, advantage);
        when(advantageRepository.saveAndFlush(advantage)).thenReturn(advantage);
        when(homePageMapper.toAdminAdvantageDto(advantage)).thenReturn(
                new AdminHomePageAdvantageDto(5L, "Title", "Description", "icons/new.png", 1)
        );

        service.updateAdvantage(
                5L,
                new AdminHomePageAdvantageRequestDto("Title", "Description", "icons/new.png")
        );

        verify(storageDeletionOutboxService).enqueueDelete("icons/old.png");
        assertEquals("icons/new.png", advantage.getIconS3Key());
    }

    @Test
    void deleteAdvantage_ShouldEnqueueIconDeletion() {
        HomePageAdvantage advantage = advantage(7L, 2, "icons/remove.png", "Title");
        givenAdvantageLocked(7L, advantage);

        service.deleteAdvantage(7L);

        verify(advantageRepository).delete(advantage);
        verify(storageDeletionOutboxService).enqueueDelete("icons/remove.png");
    }

    @Test
    void reorderAdvantages_ShouldRejectDifferentAdvantageSet() {
        givenCurrentAdvantages(advantage(1L, 1, null, "First"));

        assertThrows(
                BadRequestException.class,
                () -> service.reorderAdvantages(new HomeAdvantagesOrderRequestDto(List.of(2L)))
        );

        verify(advantageRepository, never()).flush();
    }

    @Test
    void reorderAdvantages_ShouldApplyFinalOrder() {
        HomePageAdvantage first = advantage(1L, 1, null, "First");
        HomePageAdvantage second = advantage(2L, 2, null, "Second");
        givenCurrentAdvantages(first, second);

        service.reorderAdvantages(new HomeAdvantagesOrderRequestDto(List.of(2L, 1L)));

        assertEquals(2, first.getDisplayOrder());
        assertEquals(1, second.getDisplayOrder());
        verify(advantageRepository, times(2)).flush();
    }

    @Test
    void updateAdvantage_ShouldThrow_WhenAdvantageMissing() {
        givenAdvantageMissing();

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.updateAdvantage(
                        99L,
                        new AdminHomePageAdvantageRequestDto("Title", null, null)
                )
        );
    }

    @Test
    void createAdvantage_ShouldNormalizeBlankOptionalFieldsToNull() {
        when(advantageRepository.count()).thenReturn(0L);
        when(advantageRepository.findMaxDisplayOrder()).thenReturn(0);
        when(advantageRepository.saveAndFlush(any(HomePageAdvantage.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.createAdvantage(new AdminHomePageAdvantageRequestDto("Title", "", ""));

        ArgumentCaptor<HomePageAdvantage> advantageCaptor = ArgumentCaptor.forClass(HomePageAdvantage.class);
        verify(advantageRepository).saveAndFlush(advantageCaptor.capture());
        HomePageAdvantage savedAdvantage = advantageCaptor.getValue();
        assertNull(savedAdvantage.getDescription());
        assertNull(savedAdvantage.getIconS3Key());
    }

    private void givenContentLocked(HomePageContent content) {
        when(contentRepository.findByIdForUpdate(CONTENT_ID)).thenReturn(Optional.of(content));
    }

    private void givenAdvantageLocked(Long advantageId, HomePageAdvantage advantage) {
        when(advantageRepository.findByIdForUpdate(advantageId)).thenReturn(Optional.of(advantage));
    }

    private void givenAdvantageMissing() {
        when(advantageRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());
    }

    private void givenCurrentAdvantages(HomePageAdvantage... advantages) {
        when(advantageRepository.findAllForUpdateOrderByDisplayOrder()).thenReturn(List.of(advantages));
    }

    private static HomePageContent content() {
        HomePageContent content = new HomePageContent();
        content.setId(AdminHomePageServiceImplTest.CONTENT_ID);
        content.setTitle("Old title");
        content.setDescription("Old description");
        return content;
    }

    private static HomePageAdvantage advantage(Long id, int displayOrder, String iconS3Key, String title) {
        HomePageAdvantage advantage = new HomePageAdvantage();
        advantage.setId(id);
        advantage.setDisplayOrder(displayOrder);
        advantage.setIconS3Key(iconS3Key);
        advantage.setTitle(title);
        return advantage;
    }
}
