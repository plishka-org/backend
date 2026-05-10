package org.plishka.backend.dto.review;

import org.plishka.backend.domain.media.MediaType;

public record ReviewMediaDto(
        Long id,
        String s3Key,
        MediaType mediaType,
        Boolean isPrimary
) {
}
