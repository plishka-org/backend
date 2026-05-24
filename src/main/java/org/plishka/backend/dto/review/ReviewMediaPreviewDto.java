package org.plishka.backend.dto.review;

import org.plishka.backend.domain.media.MediaType;

public record ReviewMediaPreviewDto(
        Long reviewMediaId,
        String s3Key,
        MediaType mediaType
) {
}
