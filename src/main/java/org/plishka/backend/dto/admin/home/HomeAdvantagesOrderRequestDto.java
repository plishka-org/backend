package org.plishka.backend.dto.admin.home;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record HomeAdvantagesOrderRequestDto(
        @NotNull(message = "Advantage ids are required")
        @Size(max = 20, message = "Advantage ids must contain at most 20 ids")
        List<@NotNull @Positive Long> advantageIds
) {
}
