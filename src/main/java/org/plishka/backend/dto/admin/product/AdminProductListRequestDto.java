package org.plishka.backend.dto.admin.product;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AdminProductListRequestDto(
        @Size(max = 20, message = "Category filter must contain at most 20 category ids")
        List<@NotNull @Positive Long> categoryIds,

        Boolean uncategorized,

        @Size(max = 100, message = "Search must contain at most 100 characters")
        String search,

        String sort
) {
    public AdminProductFiltersDto filters() {
        return new AdminProductFiltersDto(categoryIds, uncategorized, search);
    }
}
