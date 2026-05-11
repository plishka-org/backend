package org.plishka.backend.repository.review;

import java.util.List;
import org.plishka.backend.domain.review.ReviewMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewMediaRepository extends JpaRepository<ReviewMedia, Long> {
    List<ReviewMedia> findAllByReviewIdInOrderByDisplayOrderAsc(List<Long> reviewIds);
}
