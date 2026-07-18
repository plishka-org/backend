package org.plishka.backend.dto.about;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "About page content.")
public record AboutPageContentDto(
        @Schema(description = "Main title.", example = "About Plishka")
        String mainTitle,
        @Schema(description = "Main subtitle.", example = "We craft wooden pieces for warm, practical interiors.")
        String mainSubtitle,
        @Schema(description = "Secondary title.", example = "Our Story")
        String secondaryTitle,
        @Schema(description = "Secondary subtitle.", example = "Handcrafted wooden goods for everyday home comfort.")
        String secondarySubtitle
) {
}
