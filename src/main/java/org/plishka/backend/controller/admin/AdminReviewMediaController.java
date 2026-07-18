package org.plishka.backend.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.file.AttachMediaRequestDto;
import org.plishka.backend.service.admin.review.AdminReviewMediaService;
import org.plishka.backend.service.admin.review.AdminReviewService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@Tag(name = "Admin - Review Media")
@SecurityRequirement(name = "bearerAuth")
public class AdminReviewMediaController {
    private final AdminReviewService adminReviewService;
    private final AdminReviewMediaService adminReviewMediaService;

    @Operation(
            operationId = "adminAttachReviewMedia",
            summary = "Admin attach review media",
            description = "Requires active user with ROLE_ADMIN. S3 keys are opaque strings for clients."
    )
    @ApiResponse(responseCode = "200", description = "Review media attached; empty response body.", content = @Content)
    @PostMapping("/{id}/media/attach")
    public void attachMedia(@Positive @PathVariable Long id, @Valid @RequestBody AttachMediaRequestDto request) {
        adminReviewService.attachMedia(id, request);
    }

    @Operation(
            operationId = "adminDeleteReviewMedia",
            summary = "Admin delete review media",
            description = "Requires active user with ROLE_ADMIN."
    )
    @ApiResponse(responseCode = "204", description = "Review media deleted.")
    @DeleteMapping("/{reviewId}/media/{mediaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMedia(
            @Positive @PathVariable Long reviewId,
            @Positive @PathVariable Long mediaId
    ) {
        adminReviewMediaService.deleteMedia(reviewId, mediaId);
    }

    @Operation(
            operationId = "adminDeleteAllReviewMedia",
            summary = "Admin delete all review media",
            description = "Requires active user with ROLE_ADMIN."
    )
    @ApiResponse(responseCode = "204", description = "All review media deleted.")
    @DeleteMapping("/{reviewId}/media")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAllMedia(@Positive @PathVariable Long reviewId) {
        adminReviewMediaService.deleteAllMedia(reviewId);
    }

    @Operation(
            operationId = "adminMarkReviewMediaPrimary",
            summary = "Admin mark review media as primary",
            description = "Requires active user with ROLE_ADMIN."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Review media marked as primary; empty response body.",
            content = @Content
    )
    @PutMapping("/{reviewId}/media/{mediaId}/primary")
    public void markPrimary(
            @Positive @PathVariable Long reviewId,
            @Positive @PathVariable Long mediaId
    ) {
        adminReviewMediaService.markPrimary(reviewId, mediaId);
    }
}
