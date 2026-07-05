package org.plishka.backend.controller.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.admin.review.AdminReviewDetailDto;
import org.plishka.backend.dto.admin.review.AdminReviewFeaturedRequestDto;
import org.plishka.backend.dto.admin.review.AdminReviewRequestDto;
import org.plishka.backend.dto.admin.review.AdminReviewSummaryDto;
import org.plishka.backend.dto.common.PageResponse;
import org.plishka.backend.dto.common.PaginationRequestDto;
import org.plishka.backend.service.admin.review.AdminReviewService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewController {
    private static final int DEFAULT_PAGE_SIZE = 16;

    private final AdminReviewService adminReviewService;

    @GetMapping
    public PageResponse<AdminReviewSummaryDto> getReviews(
            @Valid @ModelAttribute PaginationRequestDto paginationRequest
    ) {
        return adminReviewService.getReviews(
                paginationRequest.resolvePage(),
                paginationRequest.resolveSize(DEFAULT_PAGE_SIZE)
        );
    }

    @GetMapping("/{id}")
    public AdminReviewDetailDto getReview(@Positive @PathVariable Long id) {
        return adminReviewService.getReview(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminReviewDetailDto createReview(@Valid @RequestBody AdminReviewRequestDto request) {
        return adminReviewService.createReview(request);
    }

    @PutMapping("/{id}")
    public AdminReviewDetailDto updateReview(
            @Positive @PathVariable Long id,
            @Valid @RequestBody AdminReviewRequestDto request
    ) {
        return adminReviewService.updateReview(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReview(@Positive @PathVariable Long id) {
        adminReviewService.deleteReview(id);
    }

    @PatchMapping("/{id}/featured")
    public AdminReviewDetailDto updateFeatured(
            @Positive @PathVariable Long id,
            @Valid @RequestBody AdminReviewFeaturedRequestDto request
    ) {
        return adminReviewService.updateFeatured(id, request);
    }
}
