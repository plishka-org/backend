package org.plishka.backend.controller.review;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.common.PageResponse;
import org.plishka.backend.dto.review.ReviewDto;
import org.plishka.backend.service.review.ReviewService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @GetMapping
    public PageResponse<ReviewDto> getReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "16") int size
    ) {
        return reviewService.getApprovedReviews(page, size);
    }

    @GetMapping("/featured")
    public List<ReviewDto> getFeaturedReviews() {
        return reviewService.getFeaturedReviews();
    }
}
