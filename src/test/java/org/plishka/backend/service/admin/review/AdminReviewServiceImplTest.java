package org.plishka.backend.service.admin.review;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.plishka.backend.domain.media.MediaTargetType;
import org.plishka.backend.domain.review.Review;
import org.plishka.backend.domain.review.ReviewMedia;
import org.plishka.backend.dto.admin.review.AdminReviewFeaturedRequestDto;
import org.plishka.backend.dto.admin.review.AdminReviewRequestDto;
import org.plishka.backend.dto.admin.review.AdminReviewSearchRequestDto;
import org.plishka.backend.dto.admin.review.AdminReviewSummaryDto;
import org.plishka.backend.dto.common.PageResponse;
import org.plishka.backend.dto.file.AttachMediaRequestDto;
import org.plishka.backend.dto.review.ReviewMediaPreviewDto;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.mapper.review.ReviewMapper;
import org.plishka.backend.repository.review.ReviewMediaRepository;
import org.plishka.backend.repository.review.ReviewRepository;
import org.plishka.backend.service.file.MediaAttachmentService;
import org.plishka.backend.service.review.ReviewMediaQueryService;
import org.plishka.backend.service.storage.StorageDeletionOutboxService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.plishka.backend.domain.media.MediaType.IMAGE;

@ExtendWith(MockitoExtension.class)
class AdminReviewServiceImplTest {
    private static final long REVIEW_ID = 7L;
    private static final Instant CREATED_AT = Instant.parse("2026-04-15T10:00:00Z");
    private static final String REVIEW_MEDIA_KEY =
            "reviews/7/images/2026/05/7223994a-bf40-4cba-9f60-234162a211fa.jpg";
    private static final String REVIEW_IMAGE_DELETE_KEY =
            "reviews/7/images/2026/05/550e8400-e29b-41d4-a716-446655440020.jpg";
    private static final String REVIEW_VIDEO_DELETE_KEY =
            "reviews/7/videos/2026/05/550e8400-e29b-41d4-a716-446655440021.mp4";

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewMediaRepository reviewMediaRepository;

    @Mock
    private ReviewMediaQueryService reviewMediaQueryService;

    @Mock
    private MediaAttachmentService mediaAttachmentService;

    @Mock
    private StorageDeletionOutboxService storageDeletionOutboxService;

    @Mock
    private ReviewMapper reviewMapper;

    @InjectMocks
    private AdminReviewServiceImpl service;

    @Test
    void getReviews_ShouldReturnMappedPageWithPrimaryMedia() {
        Review review = review(REVIEW_ID, "Author", "Content", true);
        ReviewMedia primaryMedia = reviewMedia();
        AdminReviewSummaryDto summaryDto = new AdminReviewSummaryDto(
                REVIEW_ID,
                "Author",
                "Content",
                CREATED_AT,
                true,
                new ReviewMediaPreviewDto(3L, REVIEW_MEDIA_KEY, IMAGE)
        );
        Page<Review> reviewPage = new PageImpl<>(List.of(review), PageRequest.of(0, 10), 1);

        when(reviewRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(reviewPage);
        when(reviewMediaQueryService.findPrimaryMediaForReviews(List.of(review)))
                .thenReturn(Map.of(REVIEW_ID, primaryMedia));
        when(reviewMapper.toAdminSummaryDto(review, primaryMedia)).thenReturn(summaryDto);

        PageResponse<AdminReviewSummaryDto> result =
                service.getReviews(new AdminReviewSearchRequestDto(null), 0, 10);

        assertEquals(List.of(summaryDto), result.content());
        verify(reviewMediaQueryService).findPrimaryMediaForReviews(List.of(review));
    }

    @Test
    void attachMedia_ShouldDelegateToMediaAttachmentService() {
        AttachMediaRequestDto request = new AttachMediaRequestDto(REVIEW_MEDIA_KEY);

        service.attachMedia(REVIEW_ID, request);

        verify(mediaAttachmentService).attachMedia(MediaTargetType.REVIEW, REVIEW_ID, REVIEW_MEDIA_KEY);
    }

    @Test
    void createReview_ShouldPersistNormalizedReviewWithFeaturedFalse() {
        when(reviewRepository.saveAndFlush(any(Review.class))).thenAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            review.setId(REVIEW_ID);
            return review;
        });

        service.createReview(new AdminReviewRequestDto("Olha Petrova", "Great work"));

        ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).saveAndFlush(reviewCaptor.capture());
        Review savedReview = reviewCaptor.getValue();
        assertEquals("Olha Petrova", savedReview.getAuthorName());
        assertEquals("Great work", savedReview.getContent());
        assertFalse(savedReview.getIsFeatured());
    }

    @Test
    void updateReview_ShouldPersistNormalizedReview() {
        Review review = review(REVIEW_ID, "Old author", "Old content", false);
        givenReviewLocked(review);
        when(reviewRepository.saveAndFlush(review)).thenReturn(review);

        service.updateReview(REVIEW_ID, new AdminReviewRequestDto("New author", "New content"));

        assertEquals("New author", review.getAuthorName());
        assertEquals("New content", review.getContent());
    }

    @Test
    void updateFeatured_ShouldRejectWhenLimitReached() {
        Review review = review(REVIEW_ID, "Author", "Content", false);
        givenReviewLocked(review);
        givenFeaturedReviewsAtLimit();

        assertThrows(
                BadRequestException.class,
                () -> service.updateFeatured(REVIEW_ID, new AdminReviewFeaturedRequestDto(true))
        );

        verify(reviewRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateFeatured_ShouldAllowUnfeaturingWithoutCapacityCheck() {
        Review review = review(REVIEW_ID, "Author", "Content", true);
        givenReviewLocked(review);
        when(reviewRepository.saveAndFlush(review)).thenReturn(review);

        service.updateFeatured(REVIEW_ID, new AdminReviewFeaturedRequestDto(false));

        assertFalse(review.getIsFeatured());
        verify(reviewRepository, never()).findAllFeaturedForUpdate();
    }

    @Test
    void deleteReview_ShouldEnqueueMediaDeletes() {
        Review review = review(REVIEW_ID, "Author", "Content", false);
        givenReviewLocked(review);
        when(reviewMediaRepository.findS3KeysByReviewIdOrderById(REVIEW_ID))
                .thenReturn(List.of(REVIEW_IMAGE_DELETE_KEY, REVIEW_VIDEO_DELETE_KEY));

        service.deleteReview(REVIEW_ID);

        verify(reviewRepository).delete(review);
        verify(storageDeletionOutboxService).enqueueDeletes(List.of(REVIEW_IMAGE_DELETE_KEY, REVIEW_VIDEO_DELETE_KEY));
    }

    @Test
    void updateReview_ShouldThrow_WhenReviewMissing() {
        givenReviewMissing();

        assertThrows(
                ResourceNotFoundException.class,
                () -> service.updateReview(99L, new AdminReviewRequestDto("Author", "Content"))
        );
    }

    private void givenReviewLocked(Review review) {
        when(reviewRepository.findByIdForUpdate(AdminReviewServiceImplTest.REVIEW_ID)).thenReturn(Optional.of(review));
    }

    private void givenReviewMissing() {
        when(reviewRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());
    }

    private void givenFeaturedReviewsAtLimit() {
        when(reviewRepository.findAllFeaturedForUpdate()).thenReturn(List.of(
                review(1L, "A1", "C1", true),
                review(2L, "A2", "C2", true),
                review(3L, "A3", "C3", true),
                review(4L, "A4", "C4", true),
                review(5L, "A5", "C5", true)
        ));
    }

    private static Review review(Long id, String authorName, String content, boolean featured) {
        Review review = new Review();
        review.setId(id);
        review.setAuthorName(authorName);
        review.setContent(content);
        review.setIsFeatured(featured);
        review.setCreatedAt(CREATED_AT);
        return review;
    }

    private static ReviewMedia reviewMedia() {
        ReviewMedia media = new ReviewMedia();
        media.setId(3L);
        media.setS3Key(REVIEW_MEDIA_KEY);
        media.setMediaType(IMAGE);
        return media;
    }
}
