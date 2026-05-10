package org.plishka.backend.controller.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.common.PageResponse;
import org.plishka.backend.dto.review.ReviewDto;
import org.plishka.backend.service.review.ReviewService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@Validated
public class ReviewController {
    private final ReviewService reviewService;

    @GetMapping
    public PageResponse<ReviewDto> getReviews(
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "Page must be >= 0") int page,
            @RequestParam(defaultValue = "16")
            @Min(value = 1, message = "Size must be >= 1")
            @Max(value = 100, message = "Size must be <= 100")
            int size
    ) {
        return reviewService.getApprovedReviews(page, size);
    }

    @GetMapping("/featured")
    public List<ReviewDto> getFeaturedReviews() {
        return reviewService.getFeaturedReviews();
    }
}
