package org.plishka.backend.service.admin.settings;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.plishka.backend.domain.notification.AdminNotificationOutboxStatus;
import org.plishka.backend.domain.settings.SystemSettings;
import org.plishka.backend.dto.admin.settings.AdminSettingsRequestDto;
import org.plishka.backend.exception.RequiredSingletonUnavailableException;
import org.plishka.backend.mapper.settings.SystemSettingsMapper;
import org.plishka.backend.repository.notification.AdminNotificationOutboxRepository;
import org.plishka.backend.repository.settings.SystemSettingsRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSettingsServiceImplTest {
    @Mock
    private SystemSettingsRepository systemSettingsRepository;

    @Mock
    private AdminNotificationOutboxRepository adminNotificationOutboxRepository;

    @Mock
    private SystemSettingsMapper systemSettingsMapper;

    @InjectMocks
    private AdminSettingsServiceImpl service;

    @Test
    void updateSettings_ShouldPersistShopModeFlag() {
        SystemSettings settings = settings();
        givenSettingsLocked(settings);
        when(systemSettingsRepository.saveAndFlush(settings)).thenReturn(settings);

        service.updateSettings(new AdminSettingsRequestDto(true, "new-admin@example.com"));

        assertTrue(settings.getIsShopModeEnabled());
        assertEquals("new-admin@example.com", settings.getAdminEmail());
        verify(systemSettingsRepository).saveAndFlush(settings);
        verify(adminNotificationOutboxRepository).updateRecipientsByStatus(
                "new-admin@example.com",
                AdminNotificationOutboxStatus.PENDING
        );
    }

    @Test
    void updateSettings_ShouldThrowOperationalError_WhenSettingsMissing() {
        givenSettingsMissingForUpdate();

        assertThrows(
                RequiredSingletonUnavailableException.class,
                () -> service.updateSettings(new AdminSettingsRequestDto(true, "admin@example.com"))
        );
    }

    @Test
    void getSettings_ShouldThrowOperationalError_WhenSettingsMissing() {
        givenSettingsMissingForRead();

        assertThrows(RequiredSingletonUnavailableException.class, () -> service.getSettings());
    }

    private void givenSettingsLocked(SystemSettings settings) {
        when(systemSettingsRepository.findByIdForUpdate(SystemSettings.SINGLETON_ID)).thenReturn(Optional.of(settings));
    }

    private void givenSettingsMissingForUpdate() {
        when(systemSettingsRepository.findByIdForUpdate(SystemSettings.SINGLETON_ID)).thenReturn(Optional.empty());
    }

    private void givenSettingsMissingForRead() {
        when(systemSettingsRepository.findById(SystemSettings.SINGLETON_ID)).thenReturn(Optional.empty());
    }

    private static SystemSettings settings() {
        SystemSettings settings = new SystemSettings();
        settings.setId(SystemSettings.SINGLETON_ID);
        settings.setIsShopModeEnabled(false);
        settings.setAdminEmail("admin@example.com");
        return settings;
    }
}
