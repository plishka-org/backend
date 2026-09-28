package org.plishka.backend.mapper.favorite;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.plishka.backend.config.MapStructConfig;
import org.plishka.backend.domain.favorite.Favorite;
import org.plishka.backend.dto.favorite.FavoriteDto;
import org.plishka.backend.dto.product.ProductSummaryDto;

@Mapper(config = MapStructConfig.class)
public interface FavoriteMapper {
    @Mapping(target = "favoriteId", source = "favorite.id")
    @Mapping(target = "createdAt", source = "favorite.createdAt")
    @Mapping(target = "product", source = "product")
    FavoriteDto toDto(Favorite favorite, ProductSummaryDto product);
}
