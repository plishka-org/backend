package org.plishka.backend.service.admin.catalog.product;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.plishka.backend.domain.product.Category;
import org.plishka.backend.domain.product.Product;
import org.plishka.backend.dto.admin.common.SelectionMode;
import org.plishka.backend.dto.admin.product.AdminProductFiltersDto;
import org.plishka.backend.dto.admin.product.AdminProductRequestDto;
import org.plishka.backend.dto.admin.product.BulkProductCategoryRequestDto;
import org.plishka.backend.dto.admin.product.BulkProductPriceOperation;
import org.plishka.backend.dto.admin.product.BulkProductPriceRequestDto;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.exception.ConflictException;
import org.plishka.backend.exception.ResourceNotFoundException;
import org.plishka.backend.repository.product.CategoryRepository;
import org.plishka.backend.repository.product.ProductRepository;
import org.plishka.backend.service.product.ProductMediaQueryService;
import org.plishka.backend.service.admin.catalog.support.AdminProductBulkTargetResolver;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminProductServiceImplTest {
    private static final long CATEGORY_ID = 5L;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductDeletionService productDeletionService;

    @Mock
    private AdminProductDtoAssembler productDtoAssembler;

    @Mock
    private ProductMediaQueryService productMediaQueryService;

    private AdminProductServiceImpl service;

    @BeforeEach
    void setUp() {
        AdminProductBulkTargetResolver productBulkTargetResolver =
                new AdminProductBulkTargetResolver(productRepository);
        service = new AdminProductServiceImpl(
                productRepository,
                categoryRepository,
                productDeletionService,
                productBulkTargetResolver,
                productMediaQueryService,
                productDtoAssembler
        );
    }

    @Test
    void createProduct_ShouldThrowConflict_WhenNameRaceHitsUniqueConstraint() {
        when(productRepository.existsByNameIgnoreCase("Gazebo")).thenReturn(false);
        when(categoryRepository.findByIdForUpdate(CATEGORY_ID)).thenReturn(Optional.of(category(CATEGORY_ID)));
        when(productRepository.saveAndFlush(any(Product.class)))
                .thenThrow(duplicateKey("uk_products_name"));

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> service.createProduct(productRequest(" Gazebo "))
        );

        assertEquals("Product with this name already exists", exception.getMessage());
    }

    @Test
    void updateProduct_ShouldThrowConflict_WhenNameRaceHitsUniqueConstraint() {
        Product product = product(1L, "10.00");
        when(categoryRepository.findByIdForUpdate(CATEGORY_ID)).thenReturn(Optional.of(category(CATEGORY_ID)));
        when(productRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(product));
        when(productRepository.existsByNameIgnoreCaseAndIdNot("Gazebo", 1L)).thenReturn(false);
        when(productRepository.saveAndFlush(any(Product.class)))
                .thenThrow(duplicateKey("uk_products_name"));

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> service.updateProduct(1L, productRequest(" Gazebo "))
        );

        assertEquals("Product with this name already exists", exception.getMessage());
    }

    @Test
    void updatePrices_ShouldRejectOperation_WhenResultingPriceIsNonPositive() {
        Product product = product(1L, "1.00");
        when(productRepository.findAllByIdInForUpdateOrderById(List.of(1L))).thenReturn(List.of(product));

        BulkProductPriceRequestDto request = new BulkProductPriceRequestDto(
                SelectionMode.SELECTED,
                List.of(1L),
                null,
                BulkProductPriceOperation.DECREASE_AMOUNT,
                new BigDecimal("2.00")
        );

        assertThrows(BadRequestException.class, () -> service.updatePrices(request));
    }

    @Test
    void updatePrices_ShouldRejectOperationAndNotMutateProducts_WhenResultingPriceExceedsDecimalLimit() {
        Product firstProduct = product(1L, "10.00");
        Product secondProduct = product(2L, "99999999.99");
        when(productRepository.findAllByIdInForUpdateOrderById(List.of(1L, 2L)))
                .thenReturn(List.of(firstProduct, secondProduct));

        BulkProductPriceRequestDto request = new BulkProductPriceRequestDto(
                SelectionMode.SELECTED,
                List.of(1L, 2L),
                null,
                BulkProductPriceOperation.INCREASE_AMOUNT,
                new BigDecimal("0.01")
        );

        assertThrows(BadRequestException.class, () -> service.updatePrices(request));
        assertEquals(new BigDecimal("10.00"), firstProduct.getPrice());
        assertEquals(new BigDecimal("99999999.99"), secondProduct.getPrice());
    }

    @Test
    void updatePrices_ShouldRejectSelectedNullProductIds() {
        BulkProductPriceRequestDto request = new BulkProductPriceRequestDto(
                SelectionMode.SELECTED,
                Arrays.asList((Long) null),
                null,
                BulkProductPriceOperation.INCREASE_AMOUNT,
                BigDecimal.ONE
        );

        assertThrows(BadRequestException.class, () -> service.updatePrices(request));
    }

    @Test
    void updatePrices_ShouldThrow404_WhenSelectedProductIsMissing() {
        when(productRepository.findAllByIdInForUpdateOrderById(List.of(1L, 2L)))
                .thenReturn(List.of(product(1L, "10.00")));

        BulkProductPriceRequestDto request = new BulkProductPriceRequestDto(
                SelectionMode.SELECTED,
                List.of(2L, 1L),
                null,
                BulkProductPriceOperation.INCREASE_AMOUNT,
                BigDecimal.ONE
        );

        assertThrows(ResourceNotFoundException.class, () -> service.updatePrices(request));
    }

    @Test
    void updateCategories_ShouldApplyExceptSelectedToFilteredProducts() {
        Product first = product(1L, "10.00");
        Product second = product(2L, "20.00");
        Category targetCategory = category(5L);
        when(categoryRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(targetCategory));
        when(productRepository.findAll(any(Specification.class), any(Sort.class))).thenReturn(List.of(first, second));
        when(productRepository.findAllByIdInForUpdateOrderById(List.of(1L))).thenReturn(List.of(first));

        BulkProductCategoryRequestDto request = new BulkProductCategoryRequestDto(
                SelectionMode.EXCEPT_SELECTED,
                List.of(2L),
                new AdminProductFiltersDto(null, null, null),
                5L
        );

        assertEquals(1, service.updateCategories(request).affectedCount());
        assertEquals(targetCategory, first.getCategory());
    }

    @Test
    void updateCategories_ShouldRejectMissingTargetCategory() {
        BulkProductCategoryRequestDto request = new BulkProductCategoryRequestDto(
                SelectionMode.SELECTED,
                List.of(1L),
                null,
                null
        );

        BadRequestException exception = assertThrows(BadRequestException.class, () -> service.updateCategories(request));

        assertEquals("Target category is required", exception.getMessage());
    }

    private static Product product(Long id, String price) {
        Product product = new Product();
        product.setId(id);
        product.setPrice(new BigDecimal(price));
        return product;
    }

    private static AdminProductRequestDto productRequest(String name) {
        return new AdminProductRequestDto(
                name,
                "Product description",
                new BigDecimal("10.00"),
                CATEGORY_ID
        );
    }

    private static Category category(Long id) {
        Category category = new Category();
        category.setId(id);
        category.setName("Category " + id);
        return category;
    }

    private static DataIntegrityViolationException duplicateKey(String constraintName) {
        return new DataIntegrityViolationException("Duplicate entry for key '" + constraintName + "'");
    }
}
