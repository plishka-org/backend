package org.plishka.backend.service.admin.settings;

import lombok.RequiredArgsConstructor;
import org.plishka.backend.domain.settings.SystemSettings;
import org.plishka.backend.dto.admin.settings.AdminSettingsRequestDto;
import org.plishka.backend.dto.settings.SystemSettingsDto;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.mapper.settings.SystemSettingsMapper;
import org.plishka.backend.repository.settings.SystemSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminSettingsServiceImpl implements AdminSettingsService {
    private static final long SINGLETON_SETTINGS_ID = 1L;
    private static final String SETTINGS_NOT_FOUND_MESSAGE = "System settings not found";

    private final SystemSettingsRepository systemSettingsRepository;
    private final SystemSettingsMapper systemSettingsMapper;

    @Override
    @Transactional(readOnly = true)
    public SystemSettingsDto getSettings() {
        return systemSettingsMapper.toDto(findSettingsOrThrow());
    }

    @Override
    @Transactional
    public SystemSettingsDto updateSettings(AdminSettingsRequestDto request) {
        SystemSettings settings = findSettingsForUpdateOrThrow();
        settings.setIsShopModeEnabled(request.isShopModeEnabled());

        return systemSettingsMapper.toDto(systemSettingsRepository.saveAndFlush(settings));
    }

    private SystemSettings findSettingsOrThrow() {
        return systemSettingsRepository.findById(SINGLETON_SETTINGS_ID)
                .orElseThrow(() -> new ResourceNotFoundException(SETTINGS_NOT_FOUND_MESSAGE));
    }

    private SystemSettings findSettingsForUpdateOrThrow() {
        return systemSettingsRepository.findByIdForUpdate(SINGLETON_SETTINGS_ID)
                .orElseThrow(() -> new ResourceNotFoundException(SETTINGS_NOT_FOUND_MESSAGE));
    }
}
