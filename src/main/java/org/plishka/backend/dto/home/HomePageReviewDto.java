package org.plishka.backend.dto.home;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import org.plishka.backend.dto.review.ReviewMediaDto;

@Schema(description = "Home page featured review.")
public record HomePageReviewDto(
        @Schema(description = "Review id.", example = "1")
        Long reviewId,
        @Schema(description = "Review author name.", example = "Olena")
        String authorName,
        @Schema(description = "Review content.", example = "Great quality and fast delivery.")
        String content,
        @Schema(description = "Review media.")
        List<ReviewMediaDto> media
) {
}
