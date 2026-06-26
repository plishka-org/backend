package org.plishka.backend.service.admin.catalog.product;

import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.domain.product.Product;
import org.plishka.backend.repository.cart.CartItemRepository;
import org.plishka.backend.repository.cart.CartRepository;
import org.plishka.backend.repository.product.ProductMediaRepository;
import org.plishka.backend.repository.product.ProductRepository;
import org.plishka.backend.service.storage.StorageDeletionOutboxService;
import org.plishka.backend.util.BulkIdNormalizer;
import org.plishka.backend.util.EntityPresenceValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductDeletionService {
    private final ProductRepository productRepository;
    private final ProductMediaRepository productMediaRepository;
    private final CartItemRepository cartItemRepository;
    private final CartRepository cartRepository;
    private final StorageDeletionOutboxService storageDeletionOutboxService;

    @Transactional
    public int deleteProductsByIdsOrThrowIfMissing(Collection<Long> productIds) {
        return deleteProducts(productIds, MissingProductPolicy.FAIL);
    }

    @Transactional
    public int deleteProductsByIdsIgnoringMissing(Collection<Long> productIds) {
        return deleteProducts(productIds, MissingProductPolicy.IGNORE);
    }

    private int deleteProducts(Collection<Long> productIds, MissingProductPolicy missingProductPolicy) {
        List<Long> targetProductIds = resolveProductIdsForDeletion(productIds, missingProductPolicy);
        if (targetProductIds.isEmpty()) {
            return 0;
        }

        lockAffectedCarts(targetProductIds);
        enqueueMediaDeletes(targetProductIds);
        deleteProductRows(targetProductIds);

        return targetProductIds.size();
    }

    private List<Long> resolveProductIdsForDeletion(
            Collection<Long> productIds,
            MissingProductPolicy missingProductPolicy
    ) {
        List<Long> normalizedIds = BulkIdNormalizer.normalize(productIds);
        if (normalizedIds.isEmpty()) {
            return List.of();
        }

        List<Product> lockedProducts = productRepository.findAllByIdInForUpdateOrderById(normalizedIds);
        List<Long> existingIds = lockedProducts.stream()
                .map(Product::getId)
                .toList();
        if (missingProductPolicy == MissingProductPolicy.FAIL) {
            EntityPresenceValidator.requireAllIdsFound(normalizedIds, existingIds, "Product");
        }

        return existingIds;
    }

    private void enqueueMediaDeletes(List<Long> productIds) {
        storageDeletionOutboxService.enqueueDeletes(
                productMediaRepository.findS3KeysByProductIdInOrderByProductIdAndId(productIds)
        );
    }

    private void deleteProductRows(List<Long> productIds) {
        cartItemRepository.deleteAllByProductIdIn(productIds);
        productRepository.deleteAllByIdInBatch(productIds);
        productRepository.flush();
    }

    private void lockAffectedCarts(List<Long> productIds) {
        List<Long> cartIds = cartItemRepository.findCartIdsByProductIdInOrderById(productIds);
        if (!cartIds.isEmpty()) {
            cartRepository.findAllByIdInForUpdateOrderById(cartIds);
        }
    }

    private enum MissingProductPolicy {
        FAIL,
        IGNORE
    }
}
