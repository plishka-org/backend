package org.plishka.backend.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.plishka.backend.validation.FieldMatch;
import org.plishka.backend.validation.ValidPassword;

@FieldMatch(first = "newPassword", second = "confirmPassword",
        message = "New password and confirm password must match exactly")
public record ChangePasswordRequestDto(
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 64, message = "New password must be between 8 and 64 characters long")
        @ValidPassword
        String newPassword,

        @NotBlank(message = "Confirm password is required")
        @Size(min = 8, max = 64, message =
                "Confirm password must be between 8 and 64 characters long")
        String confirmPassword
) {
}
