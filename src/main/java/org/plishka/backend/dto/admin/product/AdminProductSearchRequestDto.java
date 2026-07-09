package org.plishka.backend.dto.admin.product;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "Admin product list query parameters.")
public record AdminProductSearchRequestDto(
        @Parameter(
                description = "Category filter as repeated query params: ?categoryIds=1&categoryIds=2.",
                example = "1"
        )
        @Schema(nullable = true)
        @Size(max = 20, message = "Category filter must contain at most 20 category ids")
        List<@NotNull @Positive Long> categoryIds,

        @Parameter(description = "Filter uncategorized products.", example = "false")
        @Schema(nullable = true)
        Boolean uncategorized,

        @Parameter(description = "Search text.", example = "storage box")
        @Schema(nullable = true)
        @Size(max = 100, message = "Search must contain at most 100 characters")
        String search,

        @Parameter(description = "Sort value: name,asc | name,desc | price,asc | price,desc.", example = "name,asc")
        @Schema(
                description = "Sort value: name,asc | name,desc | price,asc | price,desc.",
                example = "name,asc",
                nullable = true
        )
        String sort
) {
    public AdminProductFiltersDto filters() {
        return new AdminProductFiltersDto(categoryIds, uncategorized, search);
    }
}
