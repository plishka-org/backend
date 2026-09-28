package org.plishka.backend.dto.admin.settings;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.plishka.backend.validation.ValidEmail;

@Schema(description = "Admin settings update request.")
public record AdminSettingsRequestDto(
        @Schema(description = "Whether shop mode is enabled.", example = "true")
        @NotNull(message = "Shop mode flag is required")
        Boolean isShopModeEnabled,

        @Schema(description = "Recipient email for admin notifications.", example = "admin@example.com")
        @NotBlank(message = "Admin email is required")
        @ValidEmail
        @Size(max = 128, message = "Admin email must contain at most 128 characters")
        String adminEmail
) {
}
