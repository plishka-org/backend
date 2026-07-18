package org.plishka.backend.dto.callback;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.plishka.backend.validation.ValidName;
import org.plishka.backend.validation.ValidPhone;

@Schema(description = "Callback request creation payload.")
public record CallbackRequestCreateDto(
        @Schema(description = "Requester name.", example = "Olena Shevchenko")
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters long")
        @ValidName
        String name,

        @Schema(description = "Ukrainian phone number in +380XXXXXXXXX format.", example = "+380501234567")
        @NotBlank(message = "Phone is required")
        @ValidPhone
        String phone,

        @Schema(description = "Callback message.", example = "Please call me back about my order")
        @NotBlank(message = "Message is required")
        @Size(max = 300, message = "Message must not exceed 300 characters")
        String message
) {
}
