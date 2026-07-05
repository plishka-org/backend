package org.plishka.backend.controller.admin;

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
public class AdminReviewMediaController {
    private final AdminReviewService adminReviewService;
    private final AdminReviewMediaService adminReviewMediaService;

    @PostMapping("/{id}/media/attach")
    public void attachMedia(@Positive @PathVariable Long id, @Valid @RequestBody AttachMediaRequestDto request) {
        adminReviewService.attachMedia(id, request);
    }

    @DeleteMapping("/{reviewId}/media/{mediaId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMedia(
            @Positive @PathVariable Long reviewId,
            @Positive @PathVariable Long mediaId
    ) {
        adminReviewMediaService.deleteMedia(reviewId, mediaId);
    }

    @DeleteMapping("/{reviewId}/media")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAllMedia(@Positive @PathVariable Long reviewId) {
        adminReviewMediaService.deleteAllMedia(reviewId);
    }

    @PutMapping("/{reviewId}/media/{mediaId}/primary")
    public void markPrimary(
            @Positive @PathVariable Long reviewId,
            @Positive @PathVariable Long mediaId
    ) {
        adminReviewMediaService.markPrimary(reviewId, mediaId);
    }
}
