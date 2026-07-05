package org.plishka.backend.service.product.impl;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.plishka.backend.domain.product.Product;
import org.plishka.backend.dto.product.ProductDetailDto;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.mapper.product.ProductMapper;
import org.plishka.backend.repository.product.ProductRepository;
import org.plishka.backend.service.file.MediaAttachmentService;
import org.plishka.backend.service.product.PriceVisibilityPolicy;
import org.plishka.backend.service.product.ProductSummaryAssembler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {
    private static final long PRODUCT_ID = 10L;
    private static final long PRODUCT_PRICE = 450L;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private MediaAttachmentService mediaAttachmentService;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductSummaryAssembler productSummaryAssembler;

    @Mock
    private PriceVisibilityPolicy priceVisibilityPolicy;

    private ProductServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProductServiceImpl(
                productRepository,
                mediaAttachmentService,
                productMapper,
                productSummaryAssembler,
                priceVisibilityPolicy
        );
    }

    @Test
    void getProducts_ShouldRejectPriceSort_WhenShopModeIsDisabled() {
        when(priceVisibilityPolicy.isCurrentPriceVisible()).thenReturn(false);
        doThrow(new BadRequestException("Price sorting is not available when shop mode is disabled"))
                .when(priceVisibilityPolicy).assertPriceSortAllowed("price,desc", false);

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.getProducts(List.of(), "price,desc", 0, 16)
        );

        assertEquals("Price sorting is not available when shop mode is disabled", exception.getMessage());
        verifyNoInteractions(productRepository, productSummaryAssembler);
    }

    @Test
    void getProduct_ShouldHidePrice_WhenShopModeIsDisabled() {
        Product product = product();
        ProductDetailDto response = new ProductDetailDto(
                PRODUCT_ID,
                product.getName(),
                product.getDescription(),
                null,
                null,
                List.of()
        );

        when(productRepository.findDetailsById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(priceVisibilityPolicy.visiblePrice(product)).thenReturn(null);
        when(productMapper.toDetailDto(product, product.getMedia(), null)).thenReturn(response);

        ProductDetailDto result = service.getProduct(PRODUCT_ID);

        assertSame(response, result);
        verify(priceVisibilityPolicy).visiblePrice(product);
        verify(productMapper).toDetailDto(product, product.getMedia(), null);
    }

    private Product product() {
        Product product = new Product();
        product.setId(PRODUCT_ID);
        product.setName("Oak Garden Bench");
        product.setDescription("Handmade oak garden bench");
        product.setPrice(PRODUCT_PRICE);
        return product;
    }
}
