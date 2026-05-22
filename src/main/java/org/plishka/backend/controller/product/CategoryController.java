package org.plishka.backend.controller.product;

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
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping
    public List<CategoryDto> getCategories() {
        return categoryService.getCategories();
    }
}
