package org.plishka.backend.dto.review;

import java.time.Instant;
import java.util.List;

public record ReviewDetailDto(
        Long reviewId,
        String authorName,
        String content,
        Instant createdAt,
        List<ReviewMediaDto> media
) {
}
