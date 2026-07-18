package org.plishka.backend.dto.admin.settings;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Admin settings update request.")
public record AdminSettingsRequestDto(
        @Schema(description = "Whether shop mode is enabled.", example = "true")
        @NotNull(message = "Shop mode flag is required")
        Boolean isShopModeEnabled
) {
}
