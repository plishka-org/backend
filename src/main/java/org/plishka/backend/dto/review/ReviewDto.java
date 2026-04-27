package org.plishka.backend.dto.review;

import java.time.LocalDateTime;
import java.util.List;

public record ReviewDto(
        Long id,
        String authorName,
        String content,
        LocalDateTime createdAt,
        List<ReviewMediaDto> media
) {
}
