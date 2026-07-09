package org.plishka.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.plishka.backend.validation.ValidEmail;

@Schema(description = "Login credentials.")
public record LoginRequestDto(
        @Schema(description = "Account email address.", example = "olena@example.com")
        @NotBlank(message = "Email is required")
        @ValidEmail
        @Size(min = 6, max = 128, message = "Email must be between 6 and 128 characters long")
        String email,

        @Schema(
                description = "Password.",
                example = "Password1",
                format = "password",
                accessMode = Schema.AccessMode.WRITE_ONLY
        )
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters long")
        String password
) {
}
