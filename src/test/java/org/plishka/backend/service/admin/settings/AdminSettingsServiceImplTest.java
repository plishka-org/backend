package org.plishka.backend.service.admin.settings;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.plishka.backend.domain.settings.SystemSettings;
import org.plishka.backend.dto.admin.settings.AdminSettingsRequestDto;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.mapper.settings.SystemSettingsMapper;
import org.plishka.backend.repository.settings.SystemSettingsRepository;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminSettingsServiceImplTest {
    private static final long SETTINGS_ID = 1L;

    @Mock
    private SystemSettingsRepository systemSettingsRepository;

    @Mock
    private SystemSettingsMapper systemSettingsMapper;

    @InjectMocks
    private AdminSettingsServiceImpl service;

    @Test
    void updateSettings_ShouldPersistShopModeFlag() {
        SystemSettings settings = settings();
        givenSettingsLocked(settings);
        when(systemSettingsRepository.saveAndFlush(settings)).thenReturn(settings);

        service.updateSettings(new AdminSettingsRequestDto(true));

        assertTrue(settings.getIsShopModeEnabled());
        verify(systemSettingsRepository).saveAndFlush(settings);
    }

    @Test
    void updateSettings_ShouldThrow_WhenSettingsMissing() {
        givenSettingsMissingForUpdate();

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.updateSettings(new AdminSettingsRequestDto(true))
        );
    }

    @Test
    void getSettings_ShouldThrow_WhenSettingsMissing() {
        givenSettingsMissingForRead();

        assertThrows(ResourceNotFoundException.class, () -> service.getSettings());
    }

    private void givenSettingsLocked(SystemSettings settings) {
        when(systemSettingsRepository.findByIdForUpdate(SETTINGS_ID)).thenReturn(Optional.of(settings));
    }

    private void givenSettingsMissingForUpdate() {
        when(systemSettingsRepository.findByIdForUpdate(SETTINGS_ID)).thenReturn(Optional.empty());
    }

    private void givenSettingsMissingForRead() {
        when(systemSettingsRepository.findById(SETTINGS_ID)).thenReturn(Optional.empty());
    }

    private static SystemSettings settings() {
        SystemSettings settings = new SystemSettings();
        settings.setId(AdminSettingsServiceImplTest.SETTINGS_ID);
        settings.setIsShopModeEnabled(false);
        return settings;
    }
}
