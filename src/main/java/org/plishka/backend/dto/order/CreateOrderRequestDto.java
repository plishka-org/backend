package org.plishka.backend.dto.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.plishka.backend.validation.ValidCity;
import org.plishka.backend.validation.ValidName;
import org.plishka.backend.validation.ValidOrderNote;
import org.plishka.backend.validation.ValidPhone;

@Schema(description = "Order checkout request.")
public record CreateOrderRequestDto(
        @Schema(description = "Customer full name.", example = "Olena Shevchenko")
        @NotBlank(message = "Customer name is required")
        @Size(min = 2, max = 100, message = "Customer name must be between 2 and 100 characters")
        @ValidName
        String customerName,

        @Schema(description = "Delivery city.", example = "Kyiv")
        @NotBlank(message = "Delivery city is required")
        @Size(min = 2, max = 100, message = "Delivery city must be between 2 and 100 characters")
        @ValidCity
        String deliveryCity,

        @Schema(description = "Ukrainian phone number in +380XXXXXXXXX format.", example = "+380501234567")
        @NotBlank(message = "Phone is required")
        @ValidPhone
        String phone,

        @Schema(description = "Optional order notes.", example = "Call before delivery", nullable = true)
        @Size(max = 500, message = "Notes must not exceed 500 characters")
        @ValidOrderNote
        String notes
) {
}
