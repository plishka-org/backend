package org.plishka.backend.dto.admin.about;

import org.plishka.backend.domain.media.MediaType;

public record AdminAboutPageMediaDto(
        Long mediaId,
        String s3Key,
        MediaType mediaType,
        Integer displayOrder
) {
}
