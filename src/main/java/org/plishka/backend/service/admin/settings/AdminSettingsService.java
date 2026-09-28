package org.plishka.backend.service.admin.settings;

import org.plishka.backend.dto.admin.settings.AdminSettingsDto;
import org.plishka.backend.dto.admin.settings.AdminSettingsRequestDto;

public interface AdminSettingsService {
    AdminSettingsDto getSettings();

    AdminSettingsDto updateSettings(AdminSettingsRequestDto settingsRequest);
}
