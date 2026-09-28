package org.plishka.backend.dto.admin.about;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "About media order request.")
public record AboutMediaOrderRequestDto(
        @Schema(description = "Media ids in display order. Maximum 100.", example = "[10, 11, 12]")
        @NotNull(message = "Media ids are required")
        @Size(max = 100, message = "Media ids must contain at most 100 ids")
        List<@NotNull @Positive Long> mediaIds
) {
}
