package org.plishka.backend.service.admin.catalog.home;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.plishka.backend.domain.home.HomePageProduct;
import org.plishka.backend.domain.product.Category;
import org.plishka.backend.domain.product.Product;
import org.plishka.backend.dto.admin.home.HomeProductsRequestDto;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.repository.home.HomePageProductRepository;
import org.plishka.backend.repository.product.ProductRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminHomeProductServiceImplTest {
    private static final Long FIRST_PRODUCT_ID = 1L;
    private static final Long SECOND_PRODUCT_ID = 2L;

    @Mock
    private HomePageProductRepository homePageProductRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private AdminHomeProductServiceImpl service;

    @Test
    void replaceHomeProducts_ShouldSaveProductsInRequestOrder() {
        List<Long> requestedProductIds = List.of(SECOND_PRODUCT_ID, FIRST_PRODUCT_ID);
        givenProductsLoadedWithCategory(
                requestedProductIds,
                categorizedProduct(FIRST_PRODUCT_ID),
                categorizedProduct(SECOND_PRODUCT_ID)
        );

        service.replaceHomeProducts(new HomeProductsRequestDto(requestedProductIds));

        verifyProductLockThenCategoryLoad(requestedProductIds);
        verifySavedHomeRows(SECOND_PRODUCT_ID, FIRST_PRODUCT_ID);
    }

    @Test
    void replaceHomeProducts_ShouldRejectUncategorizedProducts() {
        List<Long> requestedProductIds = List.of(FIRST_PRODUCT_ID);
        givenProductsLoadedWithCategory(requestedProductIds, uncategorizedProduct(FIRST_PRODUCT_ID));

        assertThrows(
                BadRequestException.class,
                () -> service.replaceHomeProducts(new HomeProductsRequestDto(requestedProductIds))
        );

        verifyProductLockThenCategoryLoad(requestedProductIds);
        verify(homePageProductRepository, never()).deleteAllInBatch();
    }

    @Test
    void reorderHomeProducts_ShouldRejectDifferentProductSet() {
        List<Long> requestedProductIds = List.of(SECOND_PRODUCT_ID);
        givenCurrentHomeProducts(categorizedProduct(FIRST_PRODUCT_ID));

        assertThrows(
                BadRequestException.class,
                () -> service.reorderHomeProducts(new HomeProductsRequestDto(requestedProductIds))
        );

        verifyNoProductLoad();
    }

    @Test
    void reorderHomeProducts_ShouldRejectUncategorizedProductsAfterProductLock() {
        List<Long> requestedProductIds = List.of(FIRST_PRODUCT_ID);
        givenCurrentHomeProducts(categorizedProduct(FIRST_PRODUCT_ID));
        givenProductsLoadedWithCategory(requestedProductIds, uncategorizedProduct(FIRST_PRODUCT_ID));

        assertThrows(
                BadRequestException.class,
                () -> service.reorderHomeProducts(new HomeProductsRequestDto(requestedProductIds))
        );

        verifyProductLockThenCategoryLoad(requestedProductIds);
        verify(homePageProductRepository, never()).deleteAllInBatch();
    }

    @Test
    void replaceHomeProducts_ShouldClearHomeProducts_WhenRequestIsEmpty() {
        service.replaceHomeProducts(new HomeProductsRequestDto(List.of()));

        verifyNoProductLoad();
        verify(homePageProductRepository).deleteAllInBatch();
        verify(homePageProductRepository, times(2)).flush();
    }

    @Test
    void replaceHomeProducts_ShouldRejectNullProductIds() {
        assertThrows(
                BadRequestException.class,
                () -> service.replaceHomeProducts(new HomeProductsRequestDto(Arrays.asList(FIRST_PRODUCT_ID, null)))
        );

        verifyNoProductLoad();
    }

    private void givenProductsLoadedWithCategory(List<Long> productIds, Product... products) {
        when(productRepository.findAllWithCategoryByIdInOrderById(productIds))
                .thenReturn(List.of(products));
    }

    private void givenCurrentHomeProducts(Product... products) {
        List<HomePageProduct> rows = Arrays.stream(products)
                .map(AdminHomeProductServiceImplTest::homeRow)
                .toList();
        when(homePageProductRepository.findAllForUpdateOrderByDisplayOrder()).thenReturn(rows);
    }

    private void verifyProductLockThenCategoryLoad(List<Long> productIds) {
        InOrder inOrder = inOrder(productRepository);
        inOrder.verify(productRepository).findAllByIdInForUpdateOrderById(productIds);
        inOrder.verify(productRepository).findAllWithCategoryByIdInOrderById(productIds);
    }

    private void verifyNoProductLoad() {
        verify(productRepository, never()).findAllByIdInForUpdateOrderById(any());
        verify(productRepository, never()).findAllWithCategoryByIdInOrderById(any());
    }

    private void verifySavedHomeRows(Long... expectedProductIds) {
        ArgumentCaptor<HomePageProduct> rowsCaptor = ArgumentCaptor.forClass(HomePageProduct.class);
        verify(homePageProductRepository, times(expectedProductIds.length)).save(rowsCaptor.capture());

        List<HomePageProduct> savedRows = rowsCaptor.getAllValues();
        assertEquals(expectedProductIds.length, savedRows.size());
        for (int index = 0; index < expectedProductIds.length; index++) {
            assertEquals(expectedProductIds[index], savedRows.get(index).getProduct().getId());
            assertEquals(index + 1, savedRows.get(index).getDisplayOrder());
        }
    }

    private static HomePageProduct homeRow(Product product) {
        HomePageProduct row = new HomePageProduct();
        row.setProduct(product);
        row.setDisplayOrder(1);
        return row;
    }

    private static Product categorizedProduct(Long id) {
        return product(id, category());
    }

    private static Product uncategorizedProduct(Long id) {
        return product(id, null);
    }

    private static Product product(Long id, Category category) {
        Product product = new Product();
        product.setId(id);
        product.setCategory(category);
        return product;
    }

    private static Category category() {
        Category category = new Category();
        category.setId(1L);
        return category;
    }
}
