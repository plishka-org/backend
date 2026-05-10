package org.plishka.backend.repository.review;

import java.util.List;
import org.plishka.backend.domain.review.ReviewMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewMediaRepository extends JpaRepository<ReviewMedia, Long> {
    List<ReviewMedia> findAllByReview_IdInOrderByDisplayOrderAsc(List<Long> reviewIds);

    boolean existsByS3Key(String s3Key);

    @Query("SELECT COALESCE(MAX(m.displayOrder), 0) FROM ReviewMedia m WHERE m.review.id = :reviewId")
    Integer findMaxDisplayOrderByReviewId(@Param("reviewId") Long reviewId);
}
