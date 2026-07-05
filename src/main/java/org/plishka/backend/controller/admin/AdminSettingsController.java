package org.plishka.backend.controller.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.admin.settings.AdminSettingsRequestDto;
import org.plishka.backend.dto.settings.SystemSettingsDto;
import org.plishka.backend.service.admin.settings.AdminSettingsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/settings")
@RequiredArgsConstructor
public class AdminSettingsController {
    private final AdminSettingsService adminSettingsService;

    @GetMapping
    public SystemSettingsDto getSettings() {
        return adminSettingsService.getSettings();
    }

    @PutMapping
    public SystemSettingsDto updateSettings(@Valid @RequestBody AdminSettingsRequestDto request) {
        return adminSettingsService.updateSettings(request);
    }
}
