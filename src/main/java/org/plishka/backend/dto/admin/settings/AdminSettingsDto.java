package org.plishka.backend.dto.admin.settings;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "System settings visible to administrators.")
public record AdminSettingsDto(
        @Schema(description = "Whether shop mode is enabled.", example = "true")
        Boolean isShopModeEnabled,

        @Schema(description = "Recipient email for admin notifications.", example = "admin@example.com")
        String adminEmail
) {
}
