package org.plishka.backend.service.admin.catalog.product;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.plishka.backend.domain.product.Product;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.repository.cart.CartItemRepository;
import org.plishka.backend.repository.cart.CartRepository;
import org.plishka.backend.repository.product.ProductMediaRepository;
import org.plishka.backend.repository.product.ProductRepository;
import org.plishka.backend.service.storage.StorageDeletionOutboxService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductDeletionServiceTest {
    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMediaRepository productMediaRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private StorageDeletionOutboxService storageDeletionOutboxService;

    @InjectMocks
    private ProductDeletionService service;

    @Test
    void deleteProductsByIdsOrThrowIfMissing_ShouldLockAffectedCartsBeforeDeletingCartItemsAndProducts() {
        List<Long> productIds = List.of(1L, 2L);
        List<Long> cartIds = List.of(10L, 20L);
        when(productRepository.findAllByIdInForUpdateOrderById(productIds))
                .thenReturn(List.of(product(1L), product(2L)));
        when(cartItemRepository.findCartIdsByProductIdInOrderById(productIds))
                .thenReturn(cartIds);
        when(productMediaRepository.findS3KeysByProductIdInOrderByProductIdAndId(productIds))
                .thenReturn(List.of("first.jpg", "second.jpg"));

        int affectedCount = service.deleteProductsByIdsOrThrowIfMissing(productIds);

        assertEquals(2, affectedCount);
        InOrder inOrder = inOrder(
                productRepository,
                productMediaRepository,
                storageDeletionOutboxService,
                cartItemRepository,
                cartRepository
        );
        inOrder.verify(productRepository).findAllByIdInForUpdateOrderById(productIds);
        inOrder.verify(cartItemRepository).findCartIdsByProductIdInOrderById(productIds);
        inOrder.verify(cartRepository).findAllByIdInForUpdateOrderById(cartIds);
        inOrder.verify(productMediaRepository).findS3KeysByProductIdInOrderByProductIdAndId(productIds);
        inOrder.verify(storageDeletionOutboxService).enqueueDeletes(List.of("first.jpg", "second.jpg"));
        inOrder.verify(cartItemRepository).deleteAllByProductIdIn(productIds);
        inOrder.verify(productRepository).deleteAllByIdInBatch(productIds);
        inOrder.verify(productRepository).flush();
    }

    @Test
    void deleteProductsByIdsOrThrowIfMissing_ShouldThrow_WhenRequiredProductIsMissing() {
        when(productRepository.findAllByIdInForUpdateOrderById(List.of(1L, 2L)))
                .thenReturn(List.of(product(1L)));

        assertThrows(ResourceNotFoundException.class, () -> service.deleteProductsByIdsOrThrowIfMissing(List.of(1L, 2L)));

        verify(cartItemRepository, never()).findCartIdsByProductIdInOrderById(List.of(1L, 2L));
        verify(cartItemRepository, never()).deleteAllByProductIdIn(List.of(1L, 2L));
        verify(storageDeletionOutboxService, never()).enqueueDeletes(List.of("first.jpg"));
        verify(productRepository, never()).deleteAllByIdInBatch(List.of(1L, 2L));
    }

    @Test
    void deleteProductsByIdsIgnoringMissing_ShouldDeleteOnlyExistingProducts() {
        when(productRepository.findAllByIdInForUpdateOrderById(List.of(1L, 2L)))
                .thenReturn(List.of(product(1L)));
        when(productMediaRepository.findS3KeysByProductIdInOrderByProductIdAndId(List.of(1L)))
                .thenReturn(List.of("first.jpg"));

        int affectedCount = service.deleteProductsByIdsIgnoringMissing(List.of(1L, 2L));

        assertEquals(1, affectedCount);
        verify(cartItemRepository).deleteAllByProductIdIn(List.of(1L));
        verify(productRepository).deleteAllByIdInBatch(List.of(1L));
        verify(storageDeletionOutboxService).enqueueDeletes(List.of("first.jpg"));
    }

    private static Product product(Long id) {
        Product product = new Product();
        product.setId(id);
        return product;
    }
}
