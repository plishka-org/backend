package org.plishka.backend.service.review;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.domain.review.Review;
import org.plishka.backend.domain.review.ReviewMedia;
import org.plishka.backend.repository.review.ReviewMediaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewMediaQueryService {
    private final ReviewMediaRepository reviewMediaRepository;

    @Transactional(readOnly = true)
    public Map<Long, ReviewMedia> findPrimaryMediaForReviews(Collection<Review> reviews) {
        List<Long> reviewIds = extractReviewIds(reviews);

        if (reviewIds.isEmpty()) {
            return Map.of();
        }

        return reviewMediaRepository.findPrimaryMediaByReviewIds(reviewIds)
                .stream()
                .collect(Collectors.toMap(media -> media.getReview().getId(), Function.identity()));
    }

    @Transactional(readOnly = true)
    public Map<Long, List<ReviewMedia>> findMediaForReviews(Collection<Review> reviews) {
        List<Long> reviewIds = extractReviewIds(reviews);

        if (reviewIds.isEmpty()) {
            return Map.of();
        }

        return reviewMediaRepository.findMediaByReviewIds(reviewIds)
                .stream()
                .collect(Collectors.groupingBy(media -> media.getReview().getId()));
    }

    private List<Long> extractReviewIds(Collection<Review> reviews) {
        return reviews.stream()
                .map(Review::getId)
                .toList();
    }
}
