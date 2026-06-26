package org.plishka.backend.service.admin.catalog.product;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.domain.media.MediaType;
import org.plishka.backend.domain.product.ProductMedia;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.repository.product.ProductMediaRepository;
import org.plishka.backend.repository.product.ProductRepository;
import org.plishka.backend.service.storage.StorageDeletionOutboxService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminProductMediaServiceImpl implements AdminProductMediaService {
    private final ProductRepository productRepository;
    private final ProductMediaRepository productMediaRepository;
    private final StorageDeletionOutboxService storageDeletionOutboxService;

    @Override
    @Transactional
    public void deleteMedia(Long productId, Long mediaId) {
        lockProductOrThrow(productId);
        ProductMedia media = findMediaForUpdateOrThrow(productId, mediaId);
        final String s3Key = media.getS3Key();
        boolean primaryDeleted = Boolean.TRUE.equals(media.getIsPrimary());

        productMediaRepository.delete(media);
        productMediaRepository.flush();

        if (primaryDeleted) {
            promoteNextPrimaryImage(productId);
        }

        storageDeletionOutboxService.enqueueDelete(s3Key);
    }

    @Override
    @Transactional
    public void deleteAllMedia(Long productId) {
        lockProductOrThrow(productId);
        List<ProductMedia> media = productMediaRepository.findAllByProductIdForUpdateOrderByDisplayOrder(productId);
        if (media.isEmpty()) {
            return;
        }

        List<String> s3Keys = media.stream()
                .map(ProductMedia::getS3Key)
                .toList();

        productMediaRepository.deleteAll(media);
        productMediaRepository.flush();
        storageDeletionOutboxService.enqueueDeletes(s3Keys);
    }

    @Override
    @Transactional
    public void markPrimary(Long productId, Long mediaId) {
        lockProductOrThrow(productId);
        ProductMedia media = findMediaForUpdateOrThrow(productId, mediaId);
        if (media.getMediaType() != MediaType.IMAGE) {
            throw new BadRequestException("Only IMAGE media can be primary");
        }

        if (Boolean.TRUE.equals(media.getIsPrimary())) {
            return;
        }

        productMediaRepository.findPrimaryByProductIdForUpdate(productId)
                .ifPresent(currentPrimary -> {
                    currentPrimary.setIsPrimary(false);
                    productMediaRepository.saveAndFlush(currentPrimary);
                });

        media.setIsPrimary(true);
        productMediaRepository.saveAndFlush(media);
    }

    private void lockProductOrThrow(Long productId) {
        productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID " + productId + " not found"));
    }

    private ProductMedia findMediaForUpdateOrThrow(Long productId, Long mediaId) {
        return productMediaRepository.findByProductIdAndIdForUpdate(productId, mediaId)
                .orElseThrow(() -> new ResourceNotFoundException("Product media with ID " + mediaId + " not found"));
    }

    private void promoteNextPrimaryImage(Long productId) {
        productMediaRepository.findImagesByProductIdForUpdateOrderByDisplayOrder(productId)
                .stream()
                .findFirst()
                .ifPresent(nextPrimary -> {
                    nextPrimary.setIsPrimary(true);
                    productMediaRepository.saveAndFlush(nextPrimary);
                });
    }
}
