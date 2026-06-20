package org.plishka.backend.dto.callback;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.plishka.backend.validation.ValidName;
import org.plishka.backend.validation.ValidPhone;

public record CallbackRequestCreateDto(
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters long")
        @ValidName
        String name,

        @NotBlank(message = "Phone is required")
        @ValidPhone
        String phone,

        @NotBlank(message = "Message is required")
        @Size(max = 300, message = "Message must not exceed 300 characters")
        String message
) {
}
