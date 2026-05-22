package org.plishka.backend.service.review;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.domain.review.Review;
import org.plishka.backend.domain.review.ReviewMedia;
import org.plishka.backend.repository.review.ReviewRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FeaturedReviewQueryService {
    private static final Sort FEATURED_REVIEWS_SORT = Sort.by(
            Sort.Order.desc("createdAt"),
            Sort.Order.desc("id")
    );

    private final ReviewRepository reviewRepository;
    private final ReviewMediaQueryService reviewMediaQueryService;

    @Transactional(readOnly = true)
    public List<FeaturedReview> findFeaturedReviews(int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("Featured reviews limit must be positive");
        }

        List<Review> reviews = reviewRepository.findAllByIsFeaturedTrue(
                PageRequest.of(0, limit, FEATURED_REVIEWS_SORT)
        );
        Map<Long, List<ReviewMedia>> mediaByReviewId = reviewMediaQueryService.findMediaForReviews(reviews);

        return reviews.stream()
                .map(review -> new FeaturedReview(
                        review,
                        mediaByReviewId.getOrDefault(review.getId(), List.of())
                ))
                .toList();
    }

    public record FeaturedReview(
            Review review,
            List<ReviewMedia> media
    ) {
    }
}
