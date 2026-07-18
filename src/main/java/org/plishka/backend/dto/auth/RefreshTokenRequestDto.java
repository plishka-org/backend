package org.plishka.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import org.plishka.backend.validation.ValidRefreshToken;

@Schema(description = "Refresh token request.")
public record RefreshTokenRequestDto(
        @Schema(
                description = "Refresh token issued by the backend. Example is intentionally not a real token.",
                example = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                minLength = 86,
                maxLength = 86,
                pattern = "^[A-Za-z0-9_-]{86}$",
                requiredMode = Schema.RequiredMode.REQUIRED,
                accessMode = Schema.AccessMode.WRITE_ONLY
        )
        @ValidRefreshToken
        String refreshToken
) {
}
