package org.plishka.backend.dto.admin.about;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.plishka.backend.dto.about.AboutPageContentDto;

@Schema(description = "Admin about page response.")
public record AdminAboutPageDto(
        @Schema(description = "About page content.")
        AboutPageContentDto content,
        @Schema(description = "About page media.")
        List<AdminAboutPageMediaDto> media
) {
}
