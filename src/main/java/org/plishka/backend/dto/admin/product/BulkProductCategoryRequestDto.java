package org.plishka.backend.dto.admin.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.plishka.backend.dto.admin.common.SelectionMode;

@Schema(description = "Bulk product category update request.")
public record BulkProductCategoryRequestDto(
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

        @Schema(description = "Target category id.", example = "2")
        @NotNull(message = "Target category is required")
        @Positive
        Long categoryId
) {
}
