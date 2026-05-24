package org.plishka.backend.dto.review;

import org.plishka.backend.domain.media.MediaType;

public record ReviewMediaDto(
        Long reviewMediaId,
        String s3Key,
        MediaType mediaType,
        Boolean isPrimary,
        Integer displayOrder
) {
}
