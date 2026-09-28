package org.plishka.backend.service.admin.review;

public interface AdminReviewMediaService {
    void deleteMedia(Long reviewId, Long mediaId);

    void deleteAllMedia(Long reviewId);

    void markPrimary(Long reviewId, Long mediaId);
}
