package org.plishka.backend.dto.auth;

import jakarta.validation.constraints.NotBlank;
import org.plishka.backend.validation.ValidEmailActionToken;

public record VerifyEmailChangeRequestDto(
        @NotBlank(message = "Token is required")
        @ValidEmailActionToken(message = "Email change token has invalid format")
        String token
) {
}
