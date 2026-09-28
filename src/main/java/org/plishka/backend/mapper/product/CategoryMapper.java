package org.plishka.backend.mapper.product;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.plishka.backend.config.MapStructConfig;
import org.plishka.backend.domain.product.Category;
import org.plishka.backend.dto.product.CategoryDto;

@Mapper(config = MapStructConfig.class)
public interface CategoryMapper {
    @Mapping(target = "categoryId", source = "id")
    CategoryDto toDto(Category category);
}
