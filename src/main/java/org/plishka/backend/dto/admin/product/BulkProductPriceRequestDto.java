package org.plishka.backend.dto.admin.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.plishka.backend.dto.admin.common.SelectionMode;

@Schema(description = "Bulk product price update request.")
public record BulkProductPriceRequestDto(
        @Schema(description = "SELECTED or EXCEPT_SELECTED. EXCEPT_SELECTED means all matching filters "
                + "except productIds.")
        @NotNull(message = "Selection mode is required")
        SelectionMode selectionMode,

        @Schema(description = "Selected or excluded product ids, depending on selectionMode.", nullable = true)
        @Size(max = 500, message = "Product selection must contain at most 500 ids")
        List<@NotNull @Positive Long> productIds,

        @Schema(description = "Filters used when selectionMode=EXCEPT_SELECTED.", nullable = true)
        @Valid
        AdminProductFiltersDto filters,

        @Schema(description = "Price operation.", example = "INCREASE_PERCENT")
        @NotNull(message = "Price operation is required")
        BulkProductPriceOperation operation,

        @Schema(description = "Amount in whole UAH or integer percent, depending on operation.", example = "10")
        @NotNull(message = "Price value is required")
        @Positive(message = "Price value must be greater than 0")
        @Max(value = 10_000_000L, message = "Price value must be at most 10000000")
        Long value
) {
}
