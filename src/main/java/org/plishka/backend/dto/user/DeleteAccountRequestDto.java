package org.plishka.backend.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Account deletion request.")
public record DeleteAccountRequestDto(
        @Schema(
                description = "Current password.",
                example = "Password1",
                format = "password",
                accessMode = Schema.AccessMode.WRITE_ONLY
        )
        @NotBlank(message = "Current password is required")
        String currentPassword
) {
}
