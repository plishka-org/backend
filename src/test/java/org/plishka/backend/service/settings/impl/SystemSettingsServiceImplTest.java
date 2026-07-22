package org.plishka.backend.service.settings.impl;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.plishka.backend.domain.settings.SystemSettings;
import org.plishka.backend.dto.settings.SystemSettingsDto;
import org.plishka.backend.exception.RequiredSingletonUnavailableException;
import org.plishka.backend.mapper.settings.SystemSettingsMapper;
import org.plishka.backend.repository.settings.SystemSettingsRepository;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemSettingsServiceImplTest {
    @Mock
    private SystemSettingsRepository systemSettingsRepository;

    @Mock
    private SystemSettingsMapper systemSettingsMapper;

    @InjectMocks
    private SystemSettingsServiceImpl service;

    @Test
    void getPublicSettings_ShouldReturnMappedSettings() {
        SystemSettings settings = settings();
        SystemSettingsDto dto = new SystemSettingsDto(true);
        when(systemSettingsRepository.findById(SystemSettings.SINGLETON_ID)).thenReturn(Optional.of(settings));
        when(systemSettingsMapper.toDto(settings)).thenReturn(dto);

        service.getPublicSettings();

        verify(systemSettingsMapper).toDto(settings);
    }

    @Test
    void getPublicSettings_ShouldThrowOperationalException_WhenSettingsMissing() {
        when(systemSettingsRepository.findById(SystemSettings.SINGLETON_ID)).thenReturn(Optional.empty());

        assertThrows(RequiredSingletonUnavailableException.class, service::getPublicSettings);
    }

    private static SystemSettings settings() {
        SystemSettings settings = new SystemSettings();
        settings.setId(SystemSettings.SINGLETON_ID);
        settings.setIsShopModeEnabled(true);
        settings.setAdminEmail("admin@example.com");
        return settings;
    }
}
