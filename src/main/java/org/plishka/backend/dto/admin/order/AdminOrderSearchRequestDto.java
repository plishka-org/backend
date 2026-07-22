package org.plishka.backend.dto.admin.order;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Admin order list query parameters.")
public record AdminOrderSearchRequestDto(
        @Parameter(description = "Order number, customer name, or exact total price search.", example = "1499")
        @Schema(nullable = true)
        @Size(max = 100, message = "Search must contain at most 100 characters")
        String search,

        @Parameter(
                description = "Sort value: createdAt,desc | createdAt,asc | totalPrice,desc | totalPrice,asc.",
                example = "createdAt,desc"
        )
        @Schema(nullable = true)
        String sort
) {
}
