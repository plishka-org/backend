package org.plishka.backend.dto.user;

public record UserProfileUpdateResponseDto(
        Long id,
        String name,
        String email,
        String phone
) {
}
