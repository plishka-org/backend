package org.plishka.backend.dto.about;

import java.util.List;

public record AboutPageResponse(
        AboutPageContentDto content,
        List<AboutPageMediaDto> media
) {
}
