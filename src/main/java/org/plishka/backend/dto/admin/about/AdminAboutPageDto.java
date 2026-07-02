package org.plishka.backend.dto.admin.about;

import java.util.List;
import org.plishka.backend.dto.about.AboutPageContentDto;

public record AdminAboutPageDto(
        AboutPageContentDto content,
        List<AdminAboutPageMediaDto> media
) {
}
