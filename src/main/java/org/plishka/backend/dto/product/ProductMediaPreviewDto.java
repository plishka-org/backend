package org.plishka.backend.dto.product;

import org.plishka.backend.domain.media.MediaType;

public record ProductMediaPreviewDto(
        Long productMediaId,
        String s3Key,
        MediaType mediaType
) {
}
