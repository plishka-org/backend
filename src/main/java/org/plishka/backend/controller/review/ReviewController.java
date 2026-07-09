package org.plishka.backend.controller.review;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.common.PageResponse;
import org.plishka.backend.dto.common.PaginationRequestDto;
import org.plishka.backend.dto.review.ReviewDetailDto;
import org.plishka.backend.dto.review.ReviewSummaryDto;
import org.plishka.backend.service.review.ReviewService;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews")
public class ReviewController {
    private static final int DEFAULT_PAGE_SIZE = 16;

    private final ReviewService reviewService;

    @Operation(
            operationId = "getReviews",
            summary = "List reviews",
            description = "Public review listing. Pagination defaults: page=0, size=16."
    )
    @GetMapping
    public PageResponse<ReviewSummaryDto> getReviews(
            @ParameterObject @Valid @ModelAttribute PaginationRequestDto paginationRequest
    ) {
        return reviewService.getReviews(
                paginationRequest.resolvePage(),
                paginationRequest.resolveSize(DEFAULT_PAGE_SIZE)
        );
    }

    @Operation(
            operationId = "getReview",
            summary = "Get review details",
            description = "Public review details."
    )
    @GetMapping("/{id}")
    public ReviewDetailDto getReview(@Positive @PathVariable Long id) {
        return reviewService.getReview(id);
    }
}
