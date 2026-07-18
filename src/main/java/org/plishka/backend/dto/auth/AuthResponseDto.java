package org.plishka.backend.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Authentication token pair.")
public record AuthResponseDto(
        @Schema(
                description = "JWT access token. Example is intentionally not a real token.",
                example = "access_token_example",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        String accessToken,

        @Schema(
                description = "Refresh token. Example is intentionally not a real token.",
                example = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                accessMode = Schema.AccessMode.READ_ONLY
        )
        String refreshToken
) {
}
