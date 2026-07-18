package org.plishka.backend.service.settings.impl;

import lombok.RequiredArgsConstructor;
import org.plishka.backend.domain.settings.SystemSettings;
import org.plishka.backend.exception.ForbiddenException;
import org.plishka.backend.repository.settings.SystemSettingsRepository;
import org.plishka.backend.service.settings.ShopModeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShopModeServiceImpl implements ShopModeService {
    private static final long SINGLETON_SETTINGS_ID = 1L;

    private final SystemSettingsRepository systemSettingsRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean isShopModeEnabled() {
        return Boolean.TRUE.equals(findSettingsOrThrow().getIsShopModeEnabled());
    }

    @Override
    @Transactional(readOnly = true)
    public void requireEnabled() {
        if (!Boolean.TRUE.equals(findSettingsOrThrow().getIsShopModeEnabled())) {
            throw new ForbiddenException(SHOP_MODE_DISABLED_MESSAGE);
        }
    }

    private SystemSettings findSettingsOrThrow() {
        return systemSettingsRepository.findById(SINGLETON_SETTINGS_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "System settings not found. Please verify database initialization."
                ));
    }
}
