package org.plishka.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.plishka.backend.openapi.support.OpenApiExampleValues;
import org.plishka.backend.validation.FieldMatch;
import org.plishka.backend.validation.ValidEmailActionToken;
import org.plishka.backend.validation.ValidPassword;

@FieldMatch(first = "password", second = "confirmPassword",
        message = "Password and confirm password must match exactly")
@Schema(description = "Password reset confirmation request.")
public record ResetPasswordRequestDto(
        @Schema(
                description = "Password reset token. Example is intentionally not a real token.",
                example = OpenApiExampleValues.EMAIL_ACTION_TOKEN,
                minLength = 43,
                maxLength = 43,
                pattern = "^[A-Za-z0-9_-]{43}$",
                accessMode = Schema.AccessMode.WRITE_ONLY
        )
        @NotBlank(message = "Token is required")
        @ValidEmailActionToken(message = "Reset token has invalid format")
        String token,

        @Schema(
                description = "New password.",
                example = "NewPassword1",
                format = "password",
                accessMode = Schema.AccessMode.WRITE_ONLY
        )
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters long")
        @ValidPassword
        String password,

        @Schema(
                description = "Must match password.",
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
