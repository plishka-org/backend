package org.plishka.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.plishka.backend.validation.FieldMatch;
import org.plishka.backend.validation.ValidEmailActionToken;
import org.plishka.backend.validation.ValidPassword;

@FieldMatch(first = "password", second = "confirmPassword",
        message = "Password and confirm password must match exactly")
public record ResetPasswordRequestDto(
        @NotBlank(message = "Token is required")
        @ValidEmailActionToken(message = "Reset token has invalid format")
        String token,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters long")
        @ValidPassword
        String password,

        @NotBlank(message = "Confirm password is required")
        @Size(min = 8, max = 64, message =
                "Confirm password must be between 8 and 64 characters long")
        String confirmPassword
) {
}
