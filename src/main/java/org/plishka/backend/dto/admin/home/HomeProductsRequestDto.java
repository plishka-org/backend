package org.plishka.backend.dto.admin.home;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record HomeProductsRequestDto(
        @NotNull(message = "Product ids are required")
        @Size(max = 100, message = "Home products must contain at most 100 ids")
        List<@NotNull @Positive Long> productIds
) {
}
