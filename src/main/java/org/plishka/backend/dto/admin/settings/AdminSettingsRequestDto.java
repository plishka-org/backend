package org.plishka.backend.dto.admin.settings;

import jakarta.validation.constraints.NotNull;

public record AdminSettingsRequestDto(
        @NotNull(message = "Shop mode flag is required")
        Boolean isShopModeEnabled
) {
}
