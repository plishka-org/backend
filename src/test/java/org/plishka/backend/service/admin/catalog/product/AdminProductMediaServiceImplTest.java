package org.plishka.backend.service.admin.catalog.product;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.plishka.backend.domain.media.MediaType;
import org.plishka.backend.domain.product.Product;
import org.plishka.backend.domain.product.ProductMedia;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.repository.product.ProductMediaRepository;
import org.plishka.backend.repository.product.ProductRepository;
import org.plishka.backend.service.storage.StorageDeletionOutboxService;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminProductMediaServiceImplTest {
    private static final long PRODUCT_ID = 10L;
    private static final long MEDIA_ID = 5L;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMediaRepository productMediaRepository;

    @Mock
    private StorageDeletionOutboxService storageDeletionOutboxService;

    @InjectMocks
    private AdminProductMediaServiceImpl service;

    @Test
    void deleteMedia_ShouldPromoteNextImage_WhenPrimaryIsDeleted() {
        ProductMedia primary = media(MEDIA_ID, MediaType.IMAGE, true, "primary.jpg");
        ProductMedia next = media(6L, MediaType.IMAGE, false, "next.jpg");
        when(productRepository.findByIdForUpdate(PRODUCT_ID)).thenReturn(Optional.of(new Product()));
        when(productMediaRepository.findByProductIdAndIdForUpdate(PRODUCT_ID, MEDIA_ID))
                .thenReturn(Optional.of(primary));
        when(productMediaRepository.findImagesByProductIdForUpdateOrderByDisplayOrder(PRODUCT_ID))
                .thenReturn(List.of(next));

        service.deleteMedia(PRODUCT_ID, MEDIA_ID);

        assertTrue(next.getIsPrimary());
        verify(productMediaRepository).delete(primary);
        verify(storageDeletionOutboxService).enqueueDelete("primary.jpg");
    }

    @Test
    void markPrimary_ShouldRejectVideo() {
        ProductMedia video = media(MEDIA_ID, MediaType.VIDEO, false, "video.mp4");
        when(productRepository.findByIdForUpdate(PRODUCT_ID)).thenReturn(Optional.of(new Product()));
        when(productMediaRepository.findByProductIdAndIdForUpdate(PRODUCT_ID, MEDIA_ID))
                .thenReturn(Optional.of(video));

        assertThrows(BadRequestException.class, () -> service.markPrimary(PRODUCT_ID, MEDIA_ID));

        verify(productMediaRepository, never()).saveAndFlush(video);
    }

    @Test
    void markPrimary_ShouldReplaceCurrentPrimaryImage() {
        ProductMedia currentPrimary = media(4L, MediaType.IMAGE, true, "current.jpg");
        ProductMedia newPrimary = media(MEDIA_ID, MediaType.IMAGE, false, "new.jpg");
        when(productRepository.findByIdForUpdate(PRODUCT_ID)).thenReturn(Optional.of(new Product()));
        when(productMediaRepository.findByProductIdAndIdForUpdate(PRODUCT_ID, MEDIA_ID))
                .thenReturn(Optional.of(newPrimary));
        when(productMediaRepository.findPrimaryByProductIdForUpdate(PRODUCT_ID))
                .thenReturn(Optional.of(currentPrimary));

        service.markPrimary(PRODUCT_ID, MEDIA_ID);

        assertFalse(currentPrimary.getIsPrimary());
        assertTrue(newPrimary.getIsPrimary());
        verify(productMediaRepository).saveAndFlush(currentPrimary);
        verify(productMediaRepository).saveAndFlush(newPrimary);
    }

    @Test
    void deleteAllMedia_ShouldDeleteRowsAndObjects() {
        ProductMedia first = media(1L, MediaType.IMAGE, true, "first.jpg");
        ProductMedia second = media(2L, MediaType.VIDEO, false, "second.mp4");
        when(productRepository.findByIdForUpdate(PRODUCT_ID)).thenReturn(Optional.of(new Product()));
        when(productMediaRepository.findAllByProductIdForUpdateOrderByDisplayOrder(PRODUCT_ID))
                .thenReturn(List.of(first, second));

        service.deleteAllMedia(PRODUCT_ID);

        verify(productMediaRepository).deleteAll(List.of(first, second));
        verify(storageDeletionOutboxService).enqueueDeletes(List.of("first.jpg", "second.mp4"));
    }

    private static ProductMedia media(Long id, MediaType mediaType, boolean primary, String s3Key) {
        ProductMedia media = new ProductMedia();
        media.setId(id);
        media.setMediaType(mediaType);
        media.setIsPrimary(primary);
        media.setS3Key(s3Key);
        media.setDisplayOrder(1);
        return media;
    }
}
