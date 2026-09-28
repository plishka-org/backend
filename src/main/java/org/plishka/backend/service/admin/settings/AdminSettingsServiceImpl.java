package org.plishka.backend.service.admin.settings;

import lombok.RequiredArgsConstructor;
import org.plishka.backend.domain.notification.AdminNotificationOutboxStatus;
import org.plishka.backend.domain.settings.SystemSettings;
import org.plishka.backend.dto.admin.settings.AdminSettingsDto;
import org.plishka.backend.dto.admin.settings.AdminSettingsRequestDto;
import org.plishka.backend.exception.RequiredSingletonUnavailableException;
import org.plishka.backend.mapper.settings.SystemSettingsMapper;
import org.plishka.backend.repository.notification.AdminNotificationOutboxRepository;
import org.plishka.backend.repository.settings.SystemSettingsRepository;
import org.plishka.backend.util.UserInputNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminSettingsServiceImpl implements AdminSettingsService {
    private static final String SETTINGS_NOT_FOUND_MESSAGE = "System settings not found";

    private final SystemSettingsRepository systemSettingsRepository;
    private final AdminNotificationOutboxRepository adminNotificationOutboxRepository;
    private final SystemSettingsMapper systemSettingsMapper;

    @Override
    @Transactional(readOnly = true)
    public AdminSettingsDto getSettings() {
        return systemSettingsMapper.toAdminDto(findSingletonSettingsOrThrow());
    }

    @Override
    @Transactional
    public AdminSettingsDto updateSettings(AdminSettingsRequestDto settingsRequest) {
        SystemSettings settings = lockSingletonSettingsOrThrow();
        String newAdminEmail = UserInputNormalizer.normalizeEmail(settingsRequest.adminEmail());
        final boolean isAdminEmailChanged = !settings.getAdminEmail().equals(newAdminEmail);

        applySettingsUpdate(settings, settingsRequest.isShopModeEnabled(), newAdminEmail);
        systemSettingsRepository.saveAndFlush(settings);
        updatePendingNotificationRecipientsIfAdminEmailChanged(newAdminEmail, isAdminEmailChanged);

        return systemSettingsMapper.toAdminDto(settings);
    }

    private SystemSettings findSingletonSettingsOrThrow() {
        return systemSettingsRepository.findById(SystemSettings.SINGLETON_ID)
                .orElseThrow(() -> new RequiredSingletonUnavailableException(SETTINGS_NOT_FOUND_MESSAGE));
    }

    private SystemSettings lockSingletonSettingsOrThrow() {
        return systemSettingsRepository.findByIdForUpdate(SystemSettings.SINGLETON_ID)
                .orElseThrow(() -> new RequiredSingletonUnavailableException(SETTINGS_NOT_FOUND_MESSAGE));
    }

    private void applySettingsUpdate(SystemSettings settings, boolean isShopModeEnabled, String newAdminEmail) {
        settings.setIsShopModeEnabled(isShopModeEnabled);
        settings.setAdminEmail(newAdminEmail);
    }

    private void updatePendingNotificationRecipientsIfAdminEmailChanged(
            String newAdminEmail,
            boolean isAdminEmailChanged
    ) {
        if (!isAdminEmailChanged) {
            return;
        }

        adminNotificationOutboxRepository.updateRecipientsByStatus(
                newAdminEmail,
                AdminNotificationOutboxStatus.PENDING
        );
    }
}
