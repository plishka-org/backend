package org.plishka.backend.repository.review;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.plishka.backend.domain.review.ReviewMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewMediaRepository extends JpaRepository<ReviewMedia, Long> {
    @Query("SELECT m.s3Key FROM ReviewMedia m")
    List<String> findAllS3Keys();

    @Query("""
            SELECT m FROM ReviewMedia m
            WHERE m.review.id IN :reviewIds
            ORDER BY m.review.id ASC, m.displayOrder ASC, m.id ASC
            """)
    List<ReviewMedia> findMediaByReviewIds(@Param("reviewIds") List<Long> reviewIds);

    @Query("SELECT m FROM ReviewMedia m WHERE m.review.id IN :reviewIds AND m.isPrimary = true")
    List<ReviewMedia> findPrimaryMediaByReviewIds(@Param("reviewIds") List<Long> reviewIds);

    boolean existsByS3Key(String s3Key);

    boolean existsByReview_IdAndIsPrimaryTrue(Long reviewId);

    @Query("SELECT COALESCE(MAX(m.displayOrder), 0) FROM ReviewMedia m WHERE m.review.id = :reviewId")
    Integer findMaxDisplayOrderByReviewId(@Param("reviewId") Long reviewId);

    @Query("""
            select m.s3Key
            from ReviewMedia m
            where m.review.id = :reviewId
            order by m.id
            """)
    List<String> findS3KeysByReviewIdOrderById(@Param("reviewId") Long reviewId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select m
            from ReviewMedia m
            where m.review.id = :reviewId
            and m.id = :mediaId
            """)
    Optional<ReviewMedia> findByReviewIdAndIdForUpdate(
            @Param("reviewId") Long reviewId,
            @Param("mediaId") Long mediaId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select m
            from ReviewMedia m
            where m.review.id = :reviewId
            order by m.displayOrder, m.id
            """)
    List<ReviewMedia> findAllByReviewIdForUpdateOrderByDisplayOrder(@Param("reviewId") Long reviewId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select m
            from ReviewMedia m
            where m.review.id = :reviewId
            and m.isPrimary = true
            """)
    Optional<ReviewMedia> findPrimaryByReviewIdForUpdate(@Param("reviewId") Long reviewId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select m
            from ReviewMedia m
            where m.review.id = :reviewId
            and m.mediaType = org.plishka.backend.domain.media.MediaType.IMAGE
            order by m.displayOrder, m.id
            """)
    List<ReviewMedia> findImagesByReviewIdForUpdateOrderByDisplayOrder(@Param("reviewId") Long reviewId);
}
