package org.plishka.backend.controller.review;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.common.PageResponse;
import org.plishka.backend.dto.common.PaginationRequestDto;
import org.plishka.backend.dto.review.ReviewDetailDto;
import org.plishka.backend.dto.review.ReviewSummaryDto;
import org.plishka.backend.service.review.ReviewService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {
    private static final int DEFAULT_PAGE_SIZE = 4;

    private final ReviewService reviewService;

    @GetMapping
    public PageResponse<ReviewSummaryDto> getReviews(@Valid @ModelAttribute PaginationRequestDto paginationRequest) {
        return reviewService.getReviews(
                paginationRequest.resolvePage(),
                paginationRequest.resolveSize(DEFAULT_PAGE_SIZE)
        );
    }

    @GetMapping("/{id}")
    public ReviewDetailDto getReview(@Positive @PathVariable Long id) {
        return reviewService.getReview(id);
    }
}
