package org.plishka.backend.dto.settings;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "System settings visible to clients.")
public record SystemSettingsDto(
        @Schema(description = "Whether shop mode is enabled.", example = "true")
        Boolean isShopModeEnabled
) {
}
