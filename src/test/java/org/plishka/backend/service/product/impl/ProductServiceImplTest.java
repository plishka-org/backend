package org.plishka.backend.service.product.impl;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.plishka.backend.domain.product.Product;
import org.plishka.backend.domain.product.Category;
import org.plishka.backend.dto.product.ProductDetailDto;
import org.plishka.backend.dto.product.CategoryDto;
import org.plishka.backend.dto.product.ProductSummaryDto;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.mapper.product.ProductMapper;
import org.plishka.backend.repository.product.ProductRepository;
import org.plishka.backend.service.file.MediaAttachmentService;
import org.plishka.backend.service.product.PriceVisibilityPolicy;
import org.plishka.backend.service.product.ProductSummaryAssembler;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {
    private static final long PRODUCT_ID = 10L;
    private static final long RELATED_PRODUCT_ID = 11L;
    private static final long CATEGORY_ID = 5L;
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
        Product product = productWithCategory(PRODUCT_ID);
        ProductDetailDto response = new ProductDetailDto(
                PRODUCT_ID,
                product.getName(),
                product.getDescription(),
                null,
                categoryDto(),
                List.of()
        );

        when(productRepository.findVisibleDetailsById(PRODUCT_ID)).thenReturn(Optional.of(product));
        when(priceVisibilityPolicy.visiblePrice(product)).thenReturn(null);
        when(productMapper.toDetailDto(product, product.getMedia(), null)).thenReturn(response);

        ProductDetailDto result = service.getProduct(PRODUCT_ID);

        assertSame(response, result);
        verify(priceVisibilityPolicy).visiblePrice(product);
        verify(productMapper).toDetailDto(product, product.getMedia(), null);
    }

    @Test
    void getProducts_ShouldUseCategorizedPublicQuery_WhenNoCategoryFilter() {
        Product product = productWithCategory(PRODUCT_ID);
        ProductSummaryDto productSummary = productSummary(PRODUCT_ID);
        PageRequest pageRequest = PageRequest.of(0, 16);
        when(priceVisibilityPolicy.isCurrentPriceVisible()).thenReturn(true);
        when(productRepository.findAllVisibleWithCategory(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(product), pageRequest, 1));
        when(productSummaryAssembler.toDtos(List.of(product), true)).thenReturn(List.of(productSummary));

        var result = service.getProducts(List.of(), null, 0, 16);

        assertEquals(List.of(productSummary), result.content());
        verify(productRepository).findAllVisibleWithCategory(any(PageRequest.class));
        verify(productSummaryAssembler).toDtos(List.of(product), true);
    }

    @Test
    void getProduct_ShouldThrowNotFound_WhenProductIsNotPubliclyVisible() {
        when(productRepository.findVisibleDetailsById(PRODUCT_ID)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.getProduct(PRODUCT_ID)
        );

        assertEquals("Product with ID 10 not found", exception.getMessage());
    }

    @Test
    void getRelatedProducts_ShouldThrowNotFound_WhenSourceProductIsNotPubliclyVisible() {
        when(productRepository.findVisibleByIdWithCategory(PRODUCT_ID)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> service.getRelatedProducts(PRODUCT_ID, 0, 4)
        );

        assertEquals("Product with ID 10 not found", exception.getMessage());
    }

    @Test
    void getRelatedProducts_ShouldUseCategoryQueryForPublicRelatedProducts() {
        Product sourceProduct = productWithCategory(PRODUCT_ID);
        Product relatedProduct = productWithCategory(RELATED_PRODUCT_ID);
        ProductSummaryDto relatedProductSummary = productSummary(RELATED_PRODUCT_ID);
        PageRequest pageRequest = PageRequest.of(0, 4);
        when(productRepository.findVisibleByIdWithCategory(PRODUCT_ID)).thenReturn(Optional.of(sourceProduct));
        when(productRepository.findAllByCategory_IdAndIdNot(eq(CATEGORY_ID), eq(PRODUCT_ID), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(relatedProduct), pageRequest, 1));
        when(priceVisibilityPolicy.isCurrentPriceVisible()).thenReturn(true);
        when(productSummaryAssembler.toDtos(List.of(relatedProduct), true)).thenReturn(List.of(relatedProductSummary));

        var result = service.getRelatedProducts(PRODUCT_ID, 0, 4);

        assertEquals(List.of(relatedProductSummary), result.content());
        verify(productRepository).findAllByCategory_IdAndIdNot(eq(CATEGORY_ID), eq(PRODUCT_ID), any(PageRequest.class));
    }

    private Product productWithCategory(Long productId) {
        Product product = new Product();
        product.setId(productId);
        product.setName("Oak Garden Bench");
        product.setDescription("Handmade oak garden bench");
        product.setPrice(PRODUCT_PRICE);
        product.setCategory(category());
        return product;
    }

    private ProductSummaryDto productSummary(Long productId) {
        return new ProductSummaryDto(
                productId,
                "Oak Garden Bench",
                categoryDto(),
                PRODUCT_PRICE,
                null
        );
    }

    private Category category() {
        Category category = new Category();
        category.setId(CATEGORY_ID);
        category.setName("Outdoor Tables and Benches");
        return category;
    }

    private CategoryDto categoryDto() {
        return new CategoryDto(CATEGORY_ID, "Outdoor Tables and Benches");
    }
}
