package org.plishka.backend.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Admin - Categories")
@SecurityRequirement(name = "bearerAuth")
public class AdminCategoryController {
    private final AdminCategoryService adminCategoryService;

    @Operation(
            operationId = "adminGetCategories",
            summary = "Admin list categories",
            description = "Requires active user with ROLE_ADMIN. Optional search is limited to 100 characters."
    )
    @GetMapping
    public List<CategoryDto> getCategories(
            @Parameter(description = "Optional category name search.", example = "boxes")
            @Size(max = 100, message = "Search must contain at most 100 characters")
            @RequestParam(required = false) String search
    ) {
        return adminCategoryService.getCategories(search);
    }

    @Operation(
            operationId = "adminCreateCategory",
            summary = "Admin create category",
            description = "Requires active user with ROLE_ADMIN."
    )
    @ApiResponse(responseCode = "201", description = "Category created.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryDto createCategory(@Valid @RequestBody AdminCategoryRequestDto request) {
        return adminCategoryService.createCategory(request);
    }

    @Operation(
            operationId = "adminUpdateCategory",
            summary = "Admin update category",
            description = "Requires active user with ROLE_ADMIN."
    )
    @PutMapping("/{id}")
    public CategoryDto updateCategory(
            @Positive @PathVariable Long id,
            @Valid @RequestBody AdminCategoryRequestDto request
    ) {
        return adminCategoryService.updateCategory(id, request);
    }

    @Operation(
            operationId = "adminDeleteCategory",
            summary = "Admin delete category",
            description = "Requires active user with ROLE_ADMIN. targetCategoryId is required only when "
                    + "strategy=MOVE_PRODUCTS."
    )
    @ApiResponse(responseCode = "204", description = "Category deleted.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(
            @Positive @PathVariable Long id,
            @Parameter(description = "Delete strategy: KEEP_PRODUCTS, MOVE_PRODUCTS, or DELETE_PRODUCTS.")
            @RequestParam CategoryDeleteStrategy strategy,
            @Parameter(description = "Target category id. Required only with strategy=MOVE_PRODUCTS.")
            @Positive @RequestParam(required = false) Long targetCategoryId
    ) {
        adminCategoryService.deleteCategory(id, strategy, targetCategoryId);
    }
}
