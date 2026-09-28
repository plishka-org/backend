package org.plishka.backend.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.plishka.backend.validation.ValidEmail;

@Schema(description = "Email change request.")
public record ChangeEmailRequestDto(
        @Schema(description = "New email address.", example = "new-email@example.com")
        @NotBlank(message = "New email is required")
        @ValidEmail
        @Size(min = 6, max = 128, message = "New email must be between 6 and 128 characters long")
        String newEmail,

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
