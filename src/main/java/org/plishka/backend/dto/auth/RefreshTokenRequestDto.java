package org.plishka.backend.dto.auth;

import org.plishka.backend.validation.ValidRefreshToken;

public record RefreshTokenRequestDto(
        @ValidRefreshToken
        String refreshToken
) {
}
