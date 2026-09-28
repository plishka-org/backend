package org.plishka.backend.dto.about;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "About page response.")
public record AboutPageResponse(
        @Schema(description = "About page content.")
        AboutPageContentDto content,
        @Schema(description = "About page media.")
        List<AboutPageMediaDto> media
) {
}
