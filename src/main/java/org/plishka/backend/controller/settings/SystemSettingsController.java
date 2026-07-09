package org.plishka.backend.controller.settings;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.settings.SystemSettingsDto;
import org.plishka.backend.service.settings.SystemSettingsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
@Tag(name = "Settings")
public class SystemSettingsController {
    private final SystemSettingsService systemSettingsService;

    @Operation(
            operationId = "getSettings",
            summary = "Get public settings",
            description = "Public system settings, including current shop mode."
    )
    @GetMapping
    public SystemSettingsDto getSettings() {
        return systemSettingsService.getPublicSettings();
    }
}
