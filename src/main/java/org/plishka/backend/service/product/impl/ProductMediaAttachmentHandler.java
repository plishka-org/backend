package org.plishka.backend.service.product.impl;

import lombok.RequiredArgsConstructor;
import org.plishka.backend.domain.media.MediaTargetType;
import org.plishka.backend.domain.media.MediaType;
import org.plishka.backend.domain.product.Product;
import org.plishka.backend.domain.product.ProductMedia;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.repository.product.ProductMediaRepository;
import org.plishka.backend.repository.product.ProductRepository;
import org.plishka.backend.service.file.MediaAttachmentHandler;
import org.plishka.backend.service.file.PrimaryMediaAttachmentPolicy;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductMediaAttachmentHandler implements MediaAttachmentHandler {
    private final ProductRepository productRepository;
    private final ProductMediaRepository productMediaRepository;
    private final PrimaryMediaAttachmentPolicy primaryMediaAttachmentPolicy;

    @Override
    public MediaTargetType targetType() {
        return MediaTargetType.PRODUCT;
    }

    @Override
    public String targetName() {
        return "product";
    }

    @Override
    public boolean existsByS3Key(String s3Key) {
        return productMediaRepository.existsByS3Key(s3Key);
    }

    @Override
    public void attachValidatedMedia(Long targetId, String s3Key, MediaType mediaType) {
        Product product = lockProductOrThrow(targetId);
        ProductMedia media = buildProductMedia(product, s3Key, mediaType);

        productMediaRepository.saveAndFlush(media);
    }

    private Product lockProductOrThrow(Long productId) {
        return productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product with ID " + productId + " not found"));
    }

    private ProductMedia buildProductMedia(Product product, String s3Key, MediaType mediaType) {
        boolean hasPrimaryMedia = productMediaRepository.existsByProduct_IdAndIsPrimaryTrue(product.getId());
        boolean isPrimary = primaryMediaAttachmentPolicy.shouldMarkAsPrimary(mediaType, hasPrimaryMedia);
        int nextDisplayOrder = productMediaRepository.findMaxDisplayOrderByProductId(product.getId()) + 1;

        ProductMedia media = new ProductMedia();
        media.setProduct(product);
        media.setS3Key(s3Key);
        media.setMediaType(mediaType);
        media.setDisplayOrder(nextDisplayOrder);
        media.setIsPrimary(isPrimary);

        return media;
    }
}
