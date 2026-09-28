package org.plishka.backend.dto.product;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "Product list query parameters.")
public record ProductListRequestDto(
        @Parameter(
                description = "Category filter as repeated query params: ?categoryIds=1&categoryIds=2.",
                example = "1"
        )
        @Schema(nullable = true)
        @Size(max = 20, message = "Category filter must contain at most 20 category ids")
        List<@NotNull @Positive Long> categoryIds,

        @Parameter(
                description = "Sort value: name,asc | name,desc | price,asc | price,desc. "
                        + "Price sorting is available only when shop mode is enabled; otherwise returns 400.",
                example = "price,asc"
        )
        @Schema(
                description = "Sort value: name,asc | name,desc | price,asc | price,desc. "
                        + "Price sorting is available only when shop mode is enabled; otherwise returns 400.",
                example = "price,asc",
                nullable = true
        )
        String sort
) {
    public List<Long> resolveCategoryIds() {
        return categoryIds == null ? List.of() : categoryIds;
    }
}
