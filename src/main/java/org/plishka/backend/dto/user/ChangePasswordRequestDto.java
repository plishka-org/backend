package org.plishka.backend.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.plishka.backend.validation.FieldMatch;
import org.plishka.backend.validation.FieldNotMatch;
import org.plishka.backend.validation.ValidPassword;

@FieldMatch(first = "newPassword", second = "confirmPassword",
        message = "New password and confirm password must match exactly")
@FieldNotMatch(first = "currentPassword", second = "newPassword",
        message = "New password must be different from current password")
@Schema(description = "Password change request.")
public record ChangePasswordRequestDto(
        @Schema(
                description = "Current password.",
                example = "Password1",
                format = "password",
                accessMode = Schema.AccessMode.WRITE_ONLY
        )
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @Schema(
                description = "New password.",
                example = "NewPassword1",
                format = "password",
                accessMode = Schema.AccessMode.WRITE_ONLY
        )
        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 64, message = "New password must be between 8 and 64 characters long")
        @ValidPassword
        String newPassword,

        @Schema(
                description = "Must match newPassword.",
                example = "NewPassword1",
                format = "password",
                accessMode = Schema.AccessMode.WRITE_ONLY
        )
        @NotBlank(message = "Confirm password is required")
        @Size(min = 8, max = 64, message =
                "Confirm password must be between 8 and 64 characters long")
        String confirmPassword
) {
}
