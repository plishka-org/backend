package org.plishka.backend.dto.admin.home;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "Home products order request.")
public record HomeProductsRequestDto(
        @Schema(description = "Product ids in display order. Maximum 100.", example = "[123, 124, 125]")
        @NotNull(message = "Product ids are required")
        @Size(max = 100, message = "Home products must contain at most 100 ids")
        List<@NotNull @Positive Long> productIds
) {
}
