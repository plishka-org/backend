package org.plishka.backend.dto.admin.home;

import io.swagger.v3.oas.annotations.media.Schema;
import org.plishka.backend.dto.home.HomePageContentDto;

@Schema(description = "Admin home page response.")
public record AdminHomePageDto(
        @Schema(description = "Home page content.")
        HomePageContentDto content
) {
}
