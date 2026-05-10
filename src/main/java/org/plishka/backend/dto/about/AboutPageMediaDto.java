package org.plishka.backend.dto.about;

import org.plishka.backend.domain.media.MediaType;

public record AboutPageMediaDto(
        Long id,
        String s3Key,
        MediaType mediaType
) {
}
