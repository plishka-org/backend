package org.plishka.backend.service.admin.catalog.category;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.plishka.backend.domain.product.Category;
import org.plishka.backend.dto.admin.category.AdminCategoryRequestDto;
import org.plishka.backend.dto.admin.category.CategoryDeleteStrategy;
import org.plishka.backend.dto.admin.category.CategoryOrderRequestDto;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.exception.ConflictException;
import org.plishka.backend.mapper.product.CategoryMapper;
import org.plishka.backend.repository.home.HomePageProductRepository;
import org.plishka.backend.repository.product.CategoryRepository;
import org.plishka.backend.repository.product.ProductRepository;
import org.plishka.backend.service.admin.catalog.product.ProductDeletionService;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminCategoryServiceImplTest {
    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private HomePageProductRepository homePageProductRepository;

    @Mock
    private ProductDeletionService productDeletionService;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private AdminCategoryServiceImpl service;

    @Test
    void createCategory_ShouldThrowConflict_WhenNameRaceHitsUniqueConstraint() {
        when(categoryRepository.existsByNameIgnoreCase("Gazebos")).thenReturn(false);
        when(categoryRepository.saveAndFlush(any(Category.class)))
                .thenThrow(duplicateKey("uk_categories_name"));

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> service.createCategory(new AdminCategoryRequestDto(" Gazebos "))
        );

        assertEquals("Category with this name already exists", exception.getMessage());
    }

    @Test
    void updateCategory_ShouldThrowConflict_WhenNameRaceHitsUniqueConstraint() {
        Category category = category(1L);
        when(categoryRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByNameIgnoreCaseAndIdNot("Gazebos", 1L)).thenReturn(false);
        when(categoryRepository.saveAndFlush(any(Category.class)))
                .thenThrow(duplicateKey("uk_categories_name"));

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> service.updateCategory(1L, new AdminCategoryRequestDto(" Gazebos "))
        );

        assertEquals("Category with this name already exists", exception.getMessage());
    }

    @Test
    void updateCategoryOrder_ShouldApplyRequestedDisplayOrder() {
        Category first = category(1L);
        first.setDisplayOrder(1);
        Category second = category(2L);
        second.setDisplayOrder(2);
        when(categoryRepository.findAllForUpdateOrderByDisplayOrder()).thenReturn(List.of(first, second));

        service.updateCategoryOrder(new CategoryOrderRequestDto(List.of(2L, 1L)));

        assertEquals(2, first.getDisplayOrder());
        assertEquals(1, second.getDisplayOrder());
        verify(categoryRepository).flush();
    }

    @Test
    void updateCategoryOrder_ShouldRejectIncompleteIdSet() {
        when(categoryRepository.findAllForUpdateOrderByDisplayOrder()).thenReturn(List.of(category(1L), category(2L)));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> service.updateCategoryOrder(new CategoryOrderRequestDto(List.of(1L)))
        );

        assertEquals("Category order must contain the current category ids", exception.getMessage());
    }

    @Test
    void deleteCategory_ShouldKeepProductsByClearingCategory() {
        Category category = category(1L);
        when(categoryRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(category));

        service.deleteCategory(1L, CategoryDeleteStrategy.KEEP_PRODUCTS, null);

        verify(homePageProductRepository).deleteByProductCategoryId(1L);
        verify(productRepository).clearCategoryByCategoryId(1L);
        verify(categoryRepository).deleteByIdDirect(1L);
    }

    @Test
    void deleteCategory_ShouldMoveProductsAfterLockingSourceAndTargetCategories() {
        Category targetCategory = category(1L);
        Category sourceCategory = category(5L);
        when(categoryRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(targetCategory));
        when(categoryRepository.findByIdForUpdate(5L)).thenReturn(Optional.of(sourceCategory));

        service.deleteCategory(5L, CategoryDeleteStrategy.MOVE_PRODUCTS, 1L);

        InOrder inOrder = inOrder(categoryRepository, productRepository);
        inOrder.verify(categoryRepository).findByIdForUpdate(1L);
        inOrder.verify(categoryRepository).findByIdForUpdate(5L);
        inOrder.verify(productRepository).moveCategoryByCategoryId(5L, 1L);
        inOrder.verify(categoryRepository).deleteByIdDirect(5L);
    }

    @Test
    void deleteCategory_ShouldDeleteProductsAssignedToCategory() {
        Category category = category(1L);
        when(categoryRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(category));
        when(productRepository.findIdsByCategoryIdOrderById(1L)).thenReturn(List.of(10L, 20L));

        service.deleteCategory(1L, CategoryDeleteStrategy.DELETE_PRODUCTS, null);

        verify(productDeletionService).deleteProductsByIdsIgnoringMissing(List.of(10L, 20L));
        verify(categoryRepository).deleteByIdDirect(1L);
    }

    @Test
    void deleteCategory_ShouldRejectMoveToDeletedCategory() {
        assertThrows(
                BadRequestException.class,
                () -> service.deleteCategory(1L, CategoryDeleteStrategy.MOVE_PRODUCTS, 1L)
        );

        verify(categoryRepository, never()).findByIdForUpdate(any());
        verify(productRepository, never()).moveCategoryByCategoryId(any(), any());
        verify(categoryRepository, never()).deleteByIdDirect(any());
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
