package org.plishka.backend.service.admin.catalog.product;

public interface AdminProductMediaService {
    void deleteMedia(Long productId, Long mediaId);

    void deleteAllMedia(Long productId);

    void markPrimary(Long productId, Long mediaId);
}
