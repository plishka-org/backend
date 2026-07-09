package org.plishka.backend.dto.home;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Home page response.")
public record HomePageResponse(
        @Schema(description = "Home page content.")
        HomePageContentDto content,
        @Schema(description = "Products displayed on the home page.")
        List<HomePageProductDto> products,
        @Schema(description = "Featured reviews. Maximum 5.")
        List<HomePageReviewDto> featuredReviews
) {
}
