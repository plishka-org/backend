package org.plishka.backend.controller.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.admin.category.AdminCategoryRequestDto;
import org.plishka.backend.dto.admin.category.CategoryDeleteStrategy;
import org.plishka.backend.dto.product.CategoryDto;
import org.plishka.backend.service.admin.catalog.category.AdminCategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {
    private final AdminCategoryService adminCategoryService;

    @GetMapping
    public List<CategoryDto> getCategories(
            @Size(max = 100, message = "Search must contain at most 100 characters")
            @RequestParam(required = false) String search
    ) {
        return adminCategoryService.getCategories(search);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto createCategory(@Valid @RequestBody AdminCategoryRequestDto request) {
        return adminCategoryService.createCategory(request);
    }

    @PutMapping("/{id}")
    public CategoryDto updateCategory(
            @Positive @PathVariable Long id,
            @Valid @RequestBody AdminCategoryRequestDto request
    ) {
        return adminCategoryService.updateCategory(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(
            @Positive @PathVariable Long id,
            @RequestParam CategoryDeleteStrategy strategy,
            @Positive @RequestParam(required = false) Long targetCategoryId
    ) {
        adminCategoryService.deleteCategory(id, strategy, targetCategoryId);
    }
}
