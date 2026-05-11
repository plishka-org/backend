package org.plishka.backend.repository.review;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.plishka.backend.domain.review.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = "media")
    List<Review> findAllByIsFeaturedTrueOrderByCreatedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM Review r WHERE r.id = :id")
    Optional<Review> findByIdForUpdate(@Param("id") Long id);
}
