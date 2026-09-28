package org.plishka.backend.service.admin.review;

import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.plishka.backend.domain.media.MediaType;
import org.plishka.backend.domain.review.Review;
import org.plishka.backend.domain.review.ReviewMedia;
import org.plishka.backend.dto.admin.review.AdminReviewFeaturedRequestDto;
import org.plishka.backend.dto.admin.review.AdminReviewRequestDto;
import org.plishka.backend.dto.admin.review.AdminReviewSearchRequestDto;
import org.plishka.backend.dto.admin.review.AdminReviewSummaryDto;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.repository.review.ReviewMediaRepository;
import org.plishka.backend.repository.review.ReviewRepository;
import org.plishka.backend.service.notification.email.transport.resend.ResendEmailTransport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminReviewServiceImplIntegrationTest {
    private static final String REVIEW_IMAGE_UUID = "550e8400-e29b-41d4-a716-446655440004";
    private static final String REVIEW_VIDEO_UUID = "550e8400-e29b-41d4-a716-446655440005";

    @Autowired
    private AdminReviewService adminReviewService;

    @Autowired
    private AdminReviewMediaService adminReviewMediaService;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ReviewMediaRepository reviewMediaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private ResendEmailTransport resendEmailTransport;

    @Test
    void getReviews_ShouldSearchByAuthorNameOrContent() {
        adminReviewService.createReview(new AdminReviewRequestDto("Olha Petrova", "Great craftsmanship"));
        adminReviewService.createReview(new AdminReviewRequestDto("Ivan Koval", "Olha recommended this shop"));
        adminReviewService.createReview(new AdminReviewRequestDto("Someone Else", "Unrelated text"));

        var byAuthor = adminReviewService.getReviews(new AdminReviewSearchRequestDto("olha"), 0, 10);
        var byContent = adminReviewService.getReviews(new AdminReviewSearchRequestDto("recommended"), 0, 10);
        var allReviews = adminReviewService.getReviews(new AdminReviewSearchRequestDto(null), 0, 10);

        assertEquals(2, byAuthor.totalElements());
        assertEquals(1, byContent.totalElements());
        assertEquals(3, allReviews.totalElements());
        assertEquals(10, allReviews.pageSize());
    }

    @Test
    void getReviews_ShouldTreatLikeWildcardsAsLiteralText() {
        var percentMatch = adminReviewService.createReview(
                new AdminReviewRequestDto("Review % Literal", "Percent content")
        );
        var underscoreMatch = adminReviewService.createReview(
                new AdminReviewRequestDto("Review _ Literal", "Underscore content")
        );
        adminReviewService.createReview(new AdminReviewRequestDto("Regular Review", "Regular content"));

        assertEquals(List.of(percentMatch.reviewId()), findReviewIds("%"));
        assertEquals(List.of(underscoreMatch.reviewId()), findReviewIds("_"));
    }

    @Test
    void reviewsFlow_ShouldManageReviewFeaturedStateMediaAndDeletionOutbox() {
        var createdReview = adminReviewService.createReview(
                new AdminReviewRequestDto("Olha Petrova", "Great craftsmanship")
        );

        Long reviewId = createdReview.reviewId();
        String reviewImageKey = reviewImageKey(reviewId);
        String reviewVideoKey = reviewVideoKey(reviewId);

        ReviewMedia image = createReviewMedia(reviewId, reviewImageKey);
        ReviewMedia video = createReviewMedia(
                reviewId,
                reviewVideoKey,
                false,
                2,
                MediaType.VIDEO
        );
        Long imageId = image.getId();
        Long videoId = video.getId();

        adminReviewMediaService.markPrimary(reviewId, image.getId());
        adminReviewMediaService.deleteMedia(reviewId, videoId);
        adminReviewService.updateFeatured(reviewId, new AdminReviewFeaturedRequestDto(true));

        assertTrue(findReviewFeatured(reviewId));
        assertEquals(0, countReviewMediaRows(videoId));
        assertEquals(1, countStorageDeletionOutboxRows(reviewVideoKey));

        seedFeaturedReviews();
        var sixthReview = adminReviewService.createReview(
                new AdminReviewRequestDto("Sixth author", "Sixth review")
        );

        assertThrows(
                BadRequestException.class,
                () -> adminReviewService.updateFeatured(
                        sixthReview.reviewId(),
                        new AdminReviewFeaturedRequestDto(true)
                )
        );

        entityManager.flush();
        entityManager.clear();

        adminReviewService.deleteReview(reviewId);

        assertEquals(0, countReviewMediaRows(imageId));
        assertEquals(1, countStorageDeletionOutboxRows(reviewImageKey));
    }

    private static String reviewImageKey(Long reviewId) {
        return "reviews/%d/images/2026/05/%s.jpg".formatted(reviewId, REVIEW_IMAGE_UUID);
    }

    private static String reviewVideoKey(Long reviewId) {
        return "reviews/%d/videos/2026/05/%s.mp4".formatted(reviewId, REVIEW_VIDEO_UUID);
    }

    private void seedFeaturedReviews() {
        for (int index = 0; index < 5; index++) {
            Review review = new Review();
            review.setAuthorName("Featured author " + index);
            review.setContent("Featured review " + index);
            review.setIsFeatured(true);
            reviewRepository.saveAndFlush(review);
        }
    }

    private ReviewMedia createReviewMedia(Long reviewId, String s3Key) {
        return createReviewMedia(reviewId, s3Key, true, 1, MediaType.IMAGE);
    }

    private ReviewMedia createReviewMedia(
            Long reviewId,
            String s3Key,
            boolean primary,
            int displayOrder,
            MediaType mediaType
    ) {
        Review review = reviewRepository.getReferenceById(reviewId);
        ReviewMedia media = new ReviewMedia();
        media.setReview(review);
        media.setS3Key(s3Key);
        media.setMediaType(mediaType);
        media.setIsPrimary(primary);
        media.setDisplayOrder(displayOrder);
        return reviewMediaRepository.saveAndFlush(media);
    }

    private List<Long> findReviewIds(String searchQuery) {
        return adminReviewService.getReviews(new AdminReviewSearchRequestDto(searchQuery), 0, 10)
                .content()
                .stream()
                .map(AdminReviewSummaryDto::reviewId)
                .toList();
    }

    private boolean findReviewFeatured(Long reviewId) {
        Boolean featured = jdbcTemplate.queryForObject(
                "select is_featured from reviews where id = ?",
                Boolean.class,
                reviewId
        );
        return Boolean.TRUE.equals(featured);
    }

    private Integer countReviewMediaRows(Long mediaId) {
        return jdbcTemplate.queryForObject(
                "select count(*) from review_media where id = ?",
                Integer.class,
                mediaId
        );
    }

    private Integer countStorageDeletionOutboxRows(String s3Key) {
        return jdbcTemplate.queryForObject(
                "select count(*) from storage_deletion_outbox where s3_key = ?",
                Integer.class,
                s3Key
        );
    }
}
