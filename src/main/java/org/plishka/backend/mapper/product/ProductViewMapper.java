package org.plishka.backend.mapper.product;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.plishka.backend.config.MapStructConfig;
import org.plishka.backend.domain.product.ProductView;
import org.plishka.backend.dto.product.ProductSummaryDto;
import org.plishka.backend.dto.product.ProductViewDto;

@Mapper(config = MapStructConfig.class)
public interface ProductViewMapper {
    @Mapping(target = "productViewId", source = "productView.id")
    @Mapping(target = "viewedAt", source = "productView.viewedAt")
    @Mapping(target = "product", source = "product")
    ProductViewDto toDto(ProductView productView, ProductSummaryDto product);
}
