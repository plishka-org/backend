package org.plishka.backend.service.settings.impl;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.plishka.backend.domain.settings.SystemSettings;
import org.plishka.backend.exception.ForbiddenException;
import org.plishka.backend.exception.RequiredSingletonUnavailableException;
import org.plishka.backend.repository.settings.SystemSettingsRepository;
import org.plishka.backend.service.settings.ShopModeService;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShopModeServiceImplTest {
    @Mock
    private SystemSettingsRepository systemSettingsRepository;

    @InjectMocks
    private ShopModeServiceImpl service;

    @Test
    void isShopModeEnabled_ShouldReturnCurrentFlag() {
        givenSettings(false);

        assertFalse(service.isShopModeEnabled());
    }

    @Test
    void requireEnabled_ShouldPass_WhenShopModeEnabled() {
        givenSettings(true);

        service.requireEnabled();
    }

    @Test
    void requireEnabled_ShouldThrowForbidden_WhenShopModeDisabled() {
        givenSettings(false);

        ForbiddenException exception = assertThrows(ForbiddenException.class, service::requireEnabled);

        assertTrue(exception.getMessage().contains(ShopModeService.SHOP_MODE_DISABLED_MESSAGE));
    }

    @Test
    void requireEnabled_ShouldThrowOperationalException_WhenSettingsMissing() {
        when(systemSettingsRepository.findById(SystemSettings.SINGLETON_ID)).thenReturn(Optional.empty());

        assertThrows(RequiredSingletonUnavailableException.class, service::requireEnabled);
    }

    private void givenSettings(boolean shopModeEnabled) {
        SystemSettings settings = new SystemSettings();
        settings.setId(SystemSettings.SINGLETON_ID);
        settings.setIsShopModeEnabled(shopModeEnabled);
        settings.setAdminEmail("admin@example.com");
        when(systemSettingsRepository.findById(SystemSettings.SINGLETON_ID)).thenReturn(Optional.of(settings));
    }
}
