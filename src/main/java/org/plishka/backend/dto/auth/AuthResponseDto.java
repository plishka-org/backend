package org.plishka.backend.dto.auth;

public record AuthResponseDto(
        String accessToken,
        String refreshToken
) {
}
