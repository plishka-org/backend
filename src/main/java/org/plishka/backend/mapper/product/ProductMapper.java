package org.plishka.backend.mapper.product;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.plishka.backend.config.MapStructConfig;
import org.plishka.backend.domain.product.Product;
import org.plishka.backend.domain.product.ProductMedia;
import org.plishka.backend.dto.product.ProductDetailDto;
import org.plishka.backend.dto.product.ProductMediaDto;
import org.plishka.backend.dto.product.ProductMediaPreviewDto;
import org.plishka.backend.dto.product.ProductSummaryDto;

@Mapper(config = MapStructConfig.class, uses = CategoryMapper.class)
public interface ProductMapper {
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "name", source = "product.name")
    @Mapping(target = "category", source = "product.category")
    @Mapping(target = "price", source = "product.price")
    @Mapping(target = "primaryMedia", source = "primaryMedia")
    ProductSummaryDto toSummaryDto(Product product, ProductMedia primaryMedia);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "name", source = "product.name")
    @Mapping(target = "description", source = "product.description")
    @Mapping(target = "price", source = "product.price")
    @Mapping(target = "category", source = "product.category")
    @Mapping(target = "media", source = "media")
    ProductDetailDto toDetailDto(Product product, List<ProductMedia> media);

    @Mapping(target = "productMediaId", source = "id")
    ProductMediaDto toMediaDto(ProductMedia media);

    @Mapping(target = "productMediaId", source = "id")
    ProductMediaPreviewDto toMediaPreviewDto(ProductMedia media);
}
