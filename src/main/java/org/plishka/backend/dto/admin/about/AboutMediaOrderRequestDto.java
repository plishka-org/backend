package org.plishka.backend.dto.admin.about;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AboutMediaOrderRequestDto(
        @NotNull(message = "Media ids are required")
        @Size(max = 100, message = "Media ids must contain at most 100 ids")
        List<@NotNull @Positive Long> mediaIds
) {
}
