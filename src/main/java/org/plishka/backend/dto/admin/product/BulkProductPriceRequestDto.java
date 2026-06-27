package org.plishka.backend.dto.admin.product;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
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
        @DecimalMin(value = "0.01", message = "Price value must be greater than 0")
        @Digits(integer = 8, fraction = 2, message = "Price value must fit DECIMAL(10,2)")
        BigDecimal value
) {
}
