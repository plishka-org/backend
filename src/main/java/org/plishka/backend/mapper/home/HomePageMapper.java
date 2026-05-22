package org.plishka.backend.mapper.home;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.plishka.backend.config.MapStructConfig;
import org.plishka.backend.domain.home.HomePageAdvantage;
import org.plishka.backend.domain.home.HomePageContent;
import org.plishka.backend.domain.home.HomePageProduct;
import org.plishka.backend.domain.product.ProductMedia;
import org.plishka.backend.domain.review.Review;
import org.plishka.backend.domain.review.ReviewMedia;
import org.plishka.backend.dto.home.HomePageAdvantageDto;
import org.plishka.backend.dto.home.HomePageContentDto;
import org.plishka.backend.dto.home.HomePageProductDto;
import org.plishka.backend.dto.home.HomePageResponse;
import org.plishka.backend.dto.home.HomePageReviewDto;
import org.plishka.backend.mapper.product.CategoryMapper;
import org.plishka.backend.mapper.product.ProductMapper;
import org.plishka.backend.mapper.review.ReviewMapper;

@Mapper(config = MapStructConfig.class, uses = {CategoryMapper.class, ProductMapper.class, ReviewMapper.class})
public interface HomePageMapper {
    HomePageResponse toResponse(
            HomePageContentDto content,
            List<HomePageAdvantageDto> advantages,
            List<HomePageProductDto> products,
            List<HomePageReviewDto> featuredReviews
    );

    HomePageContentDto toContentDto(HomePageContent content);

    @Mapping(target = "homePageAdvantageId", source = "id")
    HomePageAdvantageDto toAdvantageDto(HomePageAdvantage advantage);

    @Mapping(target = "productId", source = "homePageProduct.product.id")
    @Mapping(target = "name", source = "homePageProduct.product.name")
    @Mapping(target = "category", source = "homePageProduct.product.category")
    @Mapping(target = "primaryMedia", source = "primaryMedia")
    HomePageProductDto toHomePageProductDto(HomePageProduct homePageProduct, ProductMedia primaryMedia);

    @Mapping(target = "reviewId", source = "review.id")
    @Mapping(target = "media", source = "media")
    HomePageReviewDto toReviewDto(Review review, List<ReviewMedia> media);
}
