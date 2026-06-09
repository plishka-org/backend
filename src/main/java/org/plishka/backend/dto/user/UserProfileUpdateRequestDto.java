package org.plishka.backend.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.plishka.backend.validation.ValidName;
import org.plishka.backend.validation.ValidPhone;

public record UserProfileUpdateRequestDto(
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters long")
        @ValidName
        String name,

        @ValidPhone
        String phone
) {
}
