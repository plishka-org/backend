package org.plishka.backend.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Admin - Settings")
@SecurityRequirement(name = "bearerAuth")
public class AdminSettingsController {
    private final AdminSettingsService adminSettingsService;

    @Operation(
            operationId = "adminGetSettings",
            summary = "Admin get settings",
            description = "Requires active user with ROLE_ADMIN."
    )
    @GetMapping
    public SystemSettingsDto getSettings() {
        return adminSettingsService.getSettings();
    }

    @Operation(
            operationId = "adminUpdateSettings",
            summary = "Admin update settings",
            description = "Requires active user with ROLE_ADMIN."
    )
    @PutMapping
    public SystemSettingsDto updateSettings(@Valid @RequestBody AdminSettingsRequestDto request) {
        return adminSettingsService.updateSettings(request);
    }
}
