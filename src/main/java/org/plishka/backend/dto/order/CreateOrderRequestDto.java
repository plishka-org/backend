package org.plishka.backend.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.plishka.backend.validation.ValidCity;
import org.plishka.backend.validation.ValidName;
import org.plishka.backend.validation.ValidOrderNote;
import org.plishka.backend.validation.ValidPhone;

public record CreateOrderRequestDto(
        @NotBlank(message = "Customer name is required")
        @Size(min = 2, max = 100, message = "Customer name must be between 2 and 100 characters")
        @ValidName
        String customerName,

        @NotBlank(message = "Delivery city is required")
        @Size(min = 2, max = 100, message = "Delivery city must be between 2 and 100 characters")
        @ValidCity
        String deliveryCity,

        @NotBlank(message = "Phone is required")
        @ValidPhone
        String phone,

        @Size(max = 500, message = "Notes must not exceed 500 characters")
        @ValidOrderNote
        String notes
) {
}
