package org.plishka.backend.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.admin.review.AdminReviewDetailDto;
import org.plishka.backend.dto.admin.review.AdminReviewFeaturedRequestDto;
import org.plishka.backend.dto.admin.review.AdminReviewRequestDto;
import org.plishka.backend.dto.admin.review.AdminReviewSearchRequestDto;
import org.plishka.backend.dto.admin.review.AdminReviewSummaryDto;
import org.plishka.backend.dto.common.PageResponse;
import org.plishka.backend.dto.common.PaginationRequestDto;
import org.plishka.backend.service.admin.review.AdminReviewService;
import org.springdoc.core.annotations.ParameterObject;
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
@Tag(name = "Admin - Reviews")
@SecurityRequirement(name = "bearerAuth")
public class AdminReviewController {
    private static final int DEFAULT_PAGE_SIZE = 10;

    private final AdminReviewService adminReviewService;

    @Operation(
            operationId = "adminGetReviews",
            summary = "Admin list reviews",
            description = "Requires active user with ROLE_ADMIN. Pagination defaults: page=0, size=10."
    )
    @GetMapping
    public PageResponse<AdminReviewSummaryDto> getReviews(
            @ParameterObject @Valid @ModelAttribute AdminReviewSearchRequestDto reviewRequest,
            @ParameterObject @Valid @ModelAttribute PaginationRequestDto paginationRequest
    ) {
        return adminReviewService.getReviews(
                reviewRequest,
                paginationRequest.resolvePage(),
                paginationRequest.resolveSize(DEFAULT_PAGE_SIZE)
        );
    }

    @Operation(
            operationId = "adminGetReview",
            summary = "Admin get review",
            description = "Requires active user with ROLE_ADMIN."
    )
    @GetMapping("/{id}")
    public AdminReviewDetailDto getReview(@Positive @PathVariable Long id) {
        return adminReviewService.getReview(id);
    }

    @Operation(
            operationId = "adminCreateReview",
            summary = "Admin create review",
            description = "Requires active user with ROLE_ADMIN."
    )
    @ApiResponse(responseCode = "201", description = "Review created.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminReviewDetailDto createReview(@Valid @RequestBody AdminReviewRequestDto request) {
        return adminReviewService.createReview(request);
    }

    @Operation(
            operationId = "adminUpdateReview",
            summary = "Admin update review",
            description = "Requires active user with ROLE_ADMIN."
    )
    @PutMapping("/{id}")
    public AdminReviewDetailDto updateReview(
            @Positive @PathVariable Long id,
            @Valid @RequestBody AdminReviewRequestDto request
    ) {
        return adminReviewService.updateReview(id, request);
    }

    @Operation(
            operationId = "adminDeleteReview",
            summary = "Admin delete review",
            description = "Requires active user with ROLE_ADMIN."
    )
    @ApiResponse(responseCode = "204", description = "Review deleted.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReview(@Positive @PathVariable Long id) {
        adminReviewService.deleteReview(id);
    }

    @Operation(
            operationId = "adminUpdateReviewFeatured",
            summary = "Admin update featured review flag",
            description = "Requires active user with ROLE_ADMIN. Featured reviews are limited to a maximum of 5."
    )
    @PatchMapping("/{id}/featured")
    public AdminReviewDetailDto updateFeatured(
            @Positive @PathVariable Long id,
            @Valid @RequestBody AdminReviewFeaturedRequestDto request
    ) {
        return adminReviewService.updateFeatured(id, request);
    }
}
