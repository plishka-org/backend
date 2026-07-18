package org.plishka.backend.controller.product;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.plishka.backend.dto.product.CategoryDto;
import org.plishka.backend.service.product.CategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Tag(name = "Categories")
public class CategoryController {
    private final CategoryService categoryService;

    @Operation(
            operationId = "getCategories",
            summary = "List categories",
            description = "Public category listing."
    )
    @GetMapping
    public List<CategoryDto> getCategories() {
        return categoryService.getCategories();
    }
}
