package org.plishka.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.plishka.backend.openapi.support.OpenApiExampleValues;
import org.plishka.backend.validation.ValidEmailActionToken;

@Schema(description = "Email change verification request.")
public record VerifyEmailChangeRequestDto(
        @Schema(
                description = "Email change token. Example is intentionally not a real token.",
                example = OpenApiExampleValues.EMAIL_ACTION_TOKEN,
                minLength = 43,
                maxLength = 43,
                pattern = "^[A-Za-z0-9_-]{43}$",
                accessMode = Schema.AccessMode.WRITE_ONLY
        )
        @NotBlank(message = "Token is required")
        @ValidEmailActionToken(message = "Email change token has invalid format")
        String token
) {
}
