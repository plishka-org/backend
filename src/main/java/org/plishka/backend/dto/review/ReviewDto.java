package org.plishka.backend.dto.review;

import java.time.Instant;
import java.util.List;

public record ReviewDto(
        Long id,
        String authorName,
        String content,
        Instant createdAt,
        List<ReviewMediaDto> media
) {
}
