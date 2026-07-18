package org.plishka.backend.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Current user profile.")
public record UserProfileDto(
        @Schema(description = "User id.", example = "1")
        Long id,
        @Schema(description = "User display name.", example = "Olena Shevchenko")
        String name,
        @Schema(description = "User email address.", example = "olena@example.com")
        String email,
        @Schema(description = "Optional phone number.", example = "+380501234567", nullable = true)
        String phone
) {
}
