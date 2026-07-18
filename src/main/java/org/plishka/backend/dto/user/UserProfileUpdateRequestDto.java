package org.plishka.backend.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.plishka.backend.validation.ValidName;
import org.plishka.backend.validation.ValidPhone;

@Schema(description = "User profile update request.")
public record UserProfileUpdateRequestDto(
        @Schema(description = "User display name.", example = "Olena Shevchenko")
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters long")
        @ValidName
        String name,

        @Schema(description = "Optional phone number.", example = "+380501234567", nullable = true)
        @ValidPhone
        String phone
) {
}
