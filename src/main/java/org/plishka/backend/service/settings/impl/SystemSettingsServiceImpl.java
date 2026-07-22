package org.plishka.backend.service.settings.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.domain.settings.SystemSettings;
import org.plishka.backend.dto.settings.SystemSettingsDto;
import org.plishka.backend.exception.RequiredSingletonUnavailableException;
import org.plishka.backend.mapper.settings.SystemSettingsMapper;
import org.plishka.backend.repository.settings.SystemSettingsRepository;
import org.plishka.backend.service.settings.SystemSettingsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemSettingsServiceImpl implements SystemSettingsService {
    private final SystemSettingsRepository systemSettingsRepository;
    private final SystemSettingsMapper systemSettingsMapper;

    @Override
    @Transactional(readOnly = true)
    public SystemSettingsDto getPublicSettings() {
        log.debug("Fetching public system settings");

        SystemSettings settings = systemSettingsRepository.findById(SystemSettings.SINGLETON_ID)
                .orElseThrow(() -> new RequiredSingletonUnavailableException(
                        "System settings not found. Please verify database initialization."
                ));

        log.debug("Public system settings fetched successfully: isShopModeEnabled={}", settings.getIsShopModeEnabled());

        return systemSettingsMapper.toDto(settings);
    }
}
