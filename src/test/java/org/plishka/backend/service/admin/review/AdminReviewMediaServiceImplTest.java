package org.plishka.backend.service.admin.review;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.plishka.backend.domain.media.MediaType;
import org.plishka.backend.domain.review.Review;
import org.plishka.backend.domain.review.ReviewMedia;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.repository.review.ReviewMediaRepository;
import org.plishka.backend.repository.review.ReviewRepository;
import org.plishka.backend.service.storage.StorageDeletionOutboxService;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminReviewMediaServiceImplTest {
    private static final long REVIEW_ID = 10L;
    private static final long MEDIA_ID = 5L;
    private static final String PRIMARY_MEDIA_KEY =
            "reviews/10/images/2026/05/550e8400-e29b-41d4-a716-446655440030.jpg";
    private static final String NEXT_MEDIA_KEY =
            "reviews/10/images/2026/05/550e8400-e29b-41d4-a716-446655440031.jpg";
    private static final String VIDEO_MEDIA_KEY =
            "reviews/10/videos/2026/05/550e8400-e29b-41d4-a716-446655440032.mp4";
    private static final String CURRENT_PRIMARY_MEDIA_KEY =
            "reviews/10/images/2026/05/550e8400-e29b-41d4-a716-446655440033.jpg";
    private static final String NEW_PRIMARY_MEDIA_KEY =
            "reviews/10/images/2026/05/550e8400-e29b-41d4-a716-446655440034.jpg";
    private static final String FIRST_MEDIA_KEY =
            "reviews/10/images/2026/05/550e8400-e29b-41d4-a716-446655440035.jpg";
    private static final String SECOND_MEDIA_KEY =
            "reviews/10/videos/2026/05/550e8400-e29b-41d4-a716-446655440036.mp4";

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewMediaRepository reviewMediaRepository;

    @Mock
    private StorageDeletionOutboxService storageDeletionOutboxService;

    @InjectMocks
    private AdminReviewMediaServiceImpl service;

    @Test
    void deleteMedia_ShouldPromoteNextImage_WhenPrimaryIsDeleted() {
        ReviewMedia primary = media(MEDIA_ID, MediaType.IMAGE, true, PRIMARY_MEDIA_KEY);
        ReviewMedia next = media(6L, MediaType.IMAGE, false, NEXT_MEDIA_KEY);
        when(reviewRepository.findByIdForUpdate(REVIEW_ID)).thenReturn(Optional.of(new Review()));
        when(reviewMediaRepository.findByReviewIdAndIdForUpdate(REVIEW_ID, MEDIA_ID))
                .thenReturn(Optional.of(primary));
        when(reviewMediaRepository.findImagesByReviewIdForUpdateOrderByDisplayOrder(REVIEW_ID))
                .thenReturn(List.of(next));

        service.deleteMedia(REVIEW_ID, MEDIA_ID);

        assertTrue(next.getIsPrimary());
        verify(reviewMediaRepository).delete(primary);
        verify(storageDeletionOutboxService).enqueueDelete(PRIMARY_MEDIA_KEY);
    }

    @Test
    void markPrimary_ShouldRejectVideo() {
        ReviewMedia video = media(MEDIA_ID, MediaType.VIDEO, false, VIDEO_MEDIA_KEY);
        when(reviewRepository.findByIdForUpdate(REVIEW_ID)).thenReturn(Optional.of(new Review()));
        when(reviewMediaRepository.findByReviewIdAndIdForUpdate(REVIEW_ID, MEDIA_ID))
                .thenReturn(Optional.of(video));

        assertThrows(BadRequestException.class, () -> service.markPrimary(REVIEW_ID, MEDIA_ID));

        verify(reviewMediaRepository, never()).saveAndFlush(video);
    }

    @Test
    void markPrimary_ShouldReplaceCurrentPrimaryImage() {
        ReviewMedia currentPrimary = media(4L, MediaType.IMAGE, true, CURRENT_PRIMARY_MEDIA_KEY);
        ReviewMedia newPrimary = media(MEDIA_ID, MediaType.IMAGE, false, NEW_PRIMARY_MEDIA_KEY);
        when(reviewRepository.findByIdForUpdate(REVIEW_ID)).thenReturn(Optional.of(new Review()));
        when(reviewMediaRepository.findByReviewIdAndIdForUpdate(REVIEW_ID, MEDIA_ID))
                .thenReturn(Optional.of(newPrimary));
        when(reviewMediaRepository.findPrimaryByReviewIdForUpdate(REVIEW_ID))
                .thenReturn(Optional.of(currentPrimary));

        service.markPrimary(REVIEW_ID, MEDIA_ID);

        assertFalse(currentPrimary.getIsPrimary());
        assertTrue(newPrimary.getIsPrimary());
        verify(reviewMediaRepository).saveAndFlush(currentPrimary);
        verify(reviewMediaRepository).saveAndFlush(newPrimary);
    }

    @Test
    void deleteAllMedia_ShouldDeleteRowsAndObjects() {
        ReviewMedia first = media(1L, MediaType.IMAGE, true, FIRST_MEDIA_KEY);
        ReviewMedia second = media(2L, MediaType.VIDEO, false, SECOND_MEDIA_KEY);
        when(reviewRepository.findByIdForUpdate(REVIEW_ID)).thenReturn(Optional.of(new Review()));
        when(reviewMediaRepository.findAllByReviewIdForUpdateOrderByDisplayOrder(REVIEW_ID))
                .thenReturn(List.of(first, second));

        service.deleteAllMedia(REVIEW_ID);

        verify(reviewMediaRepository).deleteAll(List.of(first, second));
        verify(storageDeletionOutboxService).enqueueDeletes(List.of(FIRST_MEDIA_KEY, SECOND_MEDIA_KEY));
    }

    private static ReviewMedia media(Long id, MediaType mediaType, boolean primary, String s3Key) {
        ReviewMedia media = new ReviewMedia();
        media.setId(id);
        media.setMediaType(mediaType);
        media.setIsPrimary(primary);
        media.setS3Key(s3Key);
        media.setDisplayOrder(1);
        return media;
    }
}
