package org.plishka.backend.service.admin.catalog.category;

import java.util.List;
import org.plishka.backend.dto.admin.category.AdminCategoryRequestDto;
import org.plishka.backend.dto.admin.category.CategoryDeleteStrategy;
import org.plishka.backend.dto.admin.category.CategoryOrderRequestDto;
import org.plishka.backend.dto.product.CategoryDto;

public interface AdminCategoryService {
    List<CategoryDto> getCategories(String search);

    CategoryDto createCategory(AdminCategoryRequestDto request);

    CategoryDto updateCategory(Long categoryId, AdminCategoryRequestDto request);

    void updateCategoryOrder(CategoryOrderRequestDto categoryOrderRequest);

    void deleteCategory(Long categoryId, CategoryDeleteStrategy strategy, Long targetCategoryId);
}
