package org.plishka.backend.controller.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.file.AttachMediaRequestDto;
import org.plishka.backend.service.review.ReviewService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/reviews")
@RequiredArgsConstructor
public class AdminReviewMediaController {
    private final ReviewService reviewService;

    @PostMapping("/{id}/media/attach")
    public void attachMedia(@Positive @PathVariable Long id, @Valid @RequestBody AttachMediaRequestDto request) {
        reviewService.attachMedia(id, request);
    }
}
