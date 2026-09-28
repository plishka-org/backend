package org.plishka.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.plishka.backend.validation.FieldMatch;
import org.plishka.backend.validation.ValidEmail;
import org.plishka.backend.validation.ValidName;
import org.plishka.backend.validation.ValidPassword;
import org.plishka.backend.validation.ValidPhone;

@FieldMatch(first = "password", second = "confirmPassword",
        message = "Password and confirm password must match exactly")
@Schema(description = "User registration request.")
public record RegisterRequestDto(
        @Schema(description = "User display name.", example = "Olena Shevchenko")
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters long")
        @ValidName
        String name,

        @Schema(description = "Account email address.", example = "olena@example.com")
        @NotBlank(message = "Email is required")
        @ValidEmail
        @Size(min = 6, max = 128, message = "Email must be between 6 and 128 characters long")
        String email,

        @Schema(
                description = "Optional Ukrainian phone number in +380XXXXXXXXX format.",
                example = "+380501234567",
                nullable = true
        )
        @ValidPhone
        String phone,

        @Schema(
                description = "Password.",
                example = "Password1",
                format = "password",
                accessMode = Schema.AccessMode.WRITE_ONLY
        )
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters long")
        @ValidPassword
        String password,

        @Schema(
                description = "Must match password.",
                example = "Password1",
                format = "password",
                accessMode = Schema.AccessMode.WRITE_ONLY
        )
        @NotBlank(message = "Confirm password is required")
        @Size(min = 8, max = 64, message =
                "Confirm password must be between 8 and 64 characters long")
        String confirmPassword
) {
}
