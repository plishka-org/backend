package org.plishka.backend.service.admin.settings;

import org.plishka.backend.dto.admin.settings.AdminSettingsRequestDto;
import org.plishka.backend.dto.settings.SystemSettingsDto;

public interface AdminSettingsService {
    SystemSettingsDto getSettings();

    SystemSettingsDto updateSettings(AdminSettingsRequestDto request);
}
