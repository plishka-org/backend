package org.plishka.backend.service.admin.catalog.home;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminHomeProductServiceImplTest {
    @Mock
    private HomePageProductRepository homePageProductRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private AdminHomeProductServiceImpl service;

    @Test
    void replaceHomeProducts_ShouldSaveProductsInRequestOrder() {
        Product first = product(1L, category());
        Product second = product(2L, category());
        when(productRepository.findAllByIdInForUpdateOrderById(List.of(2L, 1L)))
                .thenReturn(List.of(first, second));

        service.replaceHomeProducts(new HomeProductsRequestDto(List.of(2L, 1L)));

        ArgumentCaptor<HomePageProduct> rowsCaptor = ArgumentCaptor.forClass(HomePageProduct.class);
        verify(homePageProductRepository, times(2)).save(rowsCaptor.capture());
        List<HomePageProduct> savedRows = rowsCaptor.getAllValues();
        assertEquals(2L, savedRows.get(0).getProduct().getId());
        assertEquals(1, savedRows.get(0).getDisplayOrder());
        assertEquals(1L, savedRows.get(1).getProduct().getId());
        assertEquals(2, savedRows.get(1).getDisplayOrder());
    }

    @Test
    void replaceHomeProducts_ShouldRejectUncategorizedProducts() {
        when(productRepository.findAllByIdInForUpdateOrderById(List.of(1L))).thenReturn(List.of(product(1L, null)));

        assertThrows(
                BadRequestException.class,
                () -> service.replaceHomeProducts(new HomeProductsRequestDto(List.of(1L)))
        );

        verify(homePageProductRepository, never()).deleteAllInBatch();
    }

    @Test
    void reorderHomeProducts_ShouldRejectDifferentProductSet() {
        when(homePageProductRepository.findAllForUpdateOrderByDisplayOrder())
                .thenReturn(List.of(homeRow(product(1L, category()))));

        assertThrows(
                BadRequestException.class,
                () -> service.reorderHomeProducts(new HomeProductsRequestDto(List.of(2L)))
        );

        verify(productRepository, never()).findAllByIdInForUpdateOrderById(List.of(2L));
    }

    @Test
    void reorderHomeProducts_ShouldRejectUncategorizedProductsAfterProductLock() {
        when(homePageProductRepository.findAllForUpdateOrderByDisplayOrder())
                .thenReturn(List.of(homeRow(product(1L, category()))));
        when(productRepository.findAllByIdInForUpdateOrderById(List.of(1L))).thenReturn(List.of(product(1L, null)));

        assertThrows(
                BadRequestException.class,
                () -> service.reorderHomeProducts(new HomeProductsRequestDto(List.of(1L)))
        );

        verify(homePageProductRepository, never()).deleteAllInBatch();
    }

    @Test
    void replaceHomeProducts_ShouldClearHomeProducts_WhenRequestIsEmpty() {
        service.replaceHomeProducts(new HomeProductsRequestDto(List.of()));

        verify(productRepository, never()).findAllByIdInForUpdateOrderById(List.of());
        verify(homePageProductRepository).deleteAllInBatch();
        verify(homePageProductRepository, times(2)).flush();
    }

    @Test
    void replaceHomeProducts_ShouldRejectNullProductIds() {
        assertThrows(
                BadRequestException.class,
                () -> service.replaceHomeProducts(new HomeProductsRequestDto(Arrays.asList(1L, null)))
        );

        verify(productRepository, never()).findAllByIdInForUpdateOrderById(List.of(1L));
    }

    private static HomePageProduct homeRow(Product product) {
        HomePageProduct row = new HomePageProduct();
        row.setProduct(product);
        row.setDisplayOrder(1);
        return row;
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
