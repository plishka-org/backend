package org.plishka.backend.dto.product;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ProductListRequestDto(
        @Size(max = 20, message = "Category filter must contain at most 20 category ids")
        List<@Positive Long> categoryIds,
        String sort
) {
    public List<Long> resolveCategoryIds() {
        return categoryIds == null ? List.of() : categoryIds;
    }
}
