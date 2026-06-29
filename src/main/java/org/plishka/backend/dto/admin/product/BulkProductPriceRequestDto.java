package org.plishka.backend.dto.admin.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.plishka.backend.dto.admin.common.SelectionMode;

public record BulkProductPriceRequestDto(
        @NotNull(message = "Selection mode is required")
        SelectionMode selectionMode,

        @Size(max = 500, message = "Product selection must contain at most 500 ids")
        List<@NotNull @Positive Long> productIds,

        @Valid
        AdminProductFiltersDto filters,

        @NotNull(message = "Price operation is required")
        BulkProductPriceOperation operation,

        @NotNull(message = "Price value is required")
        @Positive(message = "Price value must be greater than 0")
        @Max(value = 10_000_000L, message = "Price value must be at most 10000000")
        Long value
) {
}
