package org.plishka.backend.repository.review;

import java.util.List;
import org.plishka.backend.domain.review.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findAllByIsApprovedTrueOrderByCreatedAtDesc(Pageable pageable);

    List<Review> findAllByIsApprovedTrueAndIsFeaturedTrueOrderByCreatedAtDesc();
}
