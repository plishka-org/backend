package org.plishka.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.plishka.backend.validation.ValidEmail;

@Schema(description = "Verification email resend request.")
public record ResendVerificationEmailRequestDto(
        @Schema(description = "Account email address.", example = "olena@example.com")
        @NotBlank
        @ValidEmail
        @Size(min = 6, max = 128, message = "Email must be between 6 and 128 characters long")
        String email
) {
}
