package org.plishka.backend.dto.admin.product;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "Admin product filter set.")
public record AdminProductFiltersDto(
        @Schema(
                description = "Category filter as repeated query params or JSON array, depending on endpoint.",
                nullable = true
        )
        @Size(max = 20, message = "Category filter must contain at most 20 category ids")
        List<@NotNull @Positive Long> categoryIds,

        @Schema(description = "Filter uncategorized products.", example = "false", nullable = true)
        Boolean uncategorized,

        @Schema(description = "Search text.", example = "storage box", nullable = true)
        @Size(max = 100, message = "Search must contain at most 100 characters")
        String search
) {
}
