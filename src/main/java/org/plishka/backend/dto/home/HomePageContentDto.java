package org.plishka.backend.dto.home;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Home page content.")
public record HomePageContentDto(
        @Schema(description = "Home page title.", example = "Plishka")
        String title,
        @Schema(description = "Home page description.", example = "Handcrafted wooden goods for everyday home comfort.")
        String description
) {
}
