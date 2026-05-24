package org.plishka.backend.service.product.impl;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.domain.media.MediaTargetType;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.repository.product.ProductMediaRepository;
import org.plishka.backend.repository.product.ProductRepository;
import org.plishka.backend.service.file.MediaReferenceHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductMediaReferenceHandler implements MediaReferenceHandler {
    private final ProductRepository productRepository;
    private final ProductMediaRepository productMediaRepository;

    @Override
    public MediaTargetType targetType() {
        return MediaTargetType.PRODUCT;
    }

    @Override
    @Transactional(readOnly = true)
    public void assertParentExists(Long targetId) {
        log.debug("Verifying existence of Product with ID: {}", targetId);

        if (!productRepository.existsById(targetId)) {
            log.debug("Attempted to reference media for non-existent Product (ID: {})", targetId);
            throw new ResourceNotFoundException("Product with id " + targetId + " not found");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByS3Key(String s3Key) {
        log.debug("Checking if media file with S3 key is already attached to a Product: {}", s3Key);
        return productMediaRepository.existsByS3Key(s3Key);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findAttachedS3Keys() {
        return productMediaRepository.findAllS3Keys();
    }
}
