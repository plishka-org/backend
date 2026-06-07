package org.plishka.backend.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.plishka.backend.validation.ValidEmail;

public record ChangeEmailRequestDto(
        @NotBlank(message = "New email is required")
        @ValidEmail
        @Size(min = 6, max = 128, message = "New email must be between 6 and 128 characters long")
        String newEmail,

        @NotBlank(message = "Current password is required")
        String currentPassword
) {
}
