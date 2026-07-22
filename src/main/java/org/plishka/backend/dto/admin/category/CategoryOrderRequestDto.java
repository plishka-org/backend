package org.plishka.backend.dto.admin.category;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "Complete category id list in display order.")
public record CategoryOrderRequestDto(
        @Schema(description = "Category ids in display order.", example = "[10001, 10003, 10002]")
        @NotNull(message = "Category ids are required")
        @Size(min = 1, message = "Category ids must not be empty")
        List<@NotNull @Positive Long> categoryIds
) {
}
