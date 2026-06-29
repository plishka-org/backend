package org.plishka.backend.dto.admin.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.plishka.backend.dto.admin.common.SelectionMode;

public record BulkProductDeleteRequestDto(
        @NotNull(message = "Selection mode is required")
        SelectionMode selectionMode,

        @Size(max = 500, message = "Product selection must contain at most 500 ids")
        List<@NotNull @Positive Long> productIds,

        @Valid
        AdminProductFiltersDto filters
) {
}
