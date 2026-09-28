package org.plishka.backend.service.product;

import java.util.List;
import org.plishka.backend.dto.product.CategoryDto;

public interface CategoryService {
    List<CategoryDto> getCategories();
}
