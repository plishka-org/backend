package org.plishka.backend.controller.settings;

import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.settings.SystemSettingsDto;
import org.plishka.backend.service.settings.SystemSettingsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SystemSettingsController {
    private final SystemSettingsService systemSettingsService;

    @GetMapping
    public SystemSettingsDto getSettings() {
        return systemSettingsService.getPublicSettings();
    }
}
