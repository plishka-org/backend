package org.plishka.backend.dto.user;

public record UserProfileDto(
        Long id,
        String name,
        String email,
        String phone
) {
}
