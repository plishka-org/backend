package org.plishka.backend.dto.user;

import jakarta.validation.constraints.NotBlank;

public record DeleteAccountRequestDto(
        @NotBlank(message = "Current password is required")
        String currentPassword
) {
}
