package org.plishka.backend.service.home.impl;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.domain.home.HomePageContent;
import org.plishka.backend.domain.home.HomePageProduct;
import org.plishka.backend.domain.product.Product;
import org.plishka.backend.domain.product.ProductMedia;
import org.plishka.backend.dto.home.HomePageProductDto;
import org.plishka.backend.dto.home.HomePageResponse;
import org.plishka.backend.dto.home.HomePageReviewDto;
import org.plishka.backend.mapper.home.HomePageMapper;
import org.plishka.backend.repository.home.HomePageContentRepository;
import org.plishka.backend.repository.home.HomePageProductRepository;
import org.plishka.backend.service.home.HomePageService;
import org.plishka.backend.service.product.ProductMediaQueryService;
import org.plishka.backend.service.review.FeaturedReviewQueryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class HomePageServiceImpl implements HomePageService {
    private static final long SINGLETON_CONTENT_ID = 1L;
    private static final int FEATURED_REVIEWS_LIMIT = 5;

    private final HomePageContentRepository contentRepository;
    private final HomePageProductRepository homePageProductRepository;
    private final ProductMediaQueryService productMediaQueryService;
    private final FeaturedReviewQueryService featuredReviewQueryService;
    private final HomePageMapper homePageMapper;

    @Override
    @Transactional(readOnly = true)
    public HomePageResponse getHomePageData() {
        log.debug("Fetching home page data");

        HomePageContent content = findContentOrThrow();
        List<HomePageProductDto> products = getHomePageProducts();
        List<HomePageReviewDto> featuredReviews = getFeaturedReviews();

        HomePageResponse response = homePageMapper.toResponse(
                homePageMapper.toContentDto(content),
                products,
                featuredReviews
        );

        log.debug(
                "Home page data fetched successfully: productsCount={}, featuredReviewsCount={}",
                products.size(),
                featuredReviews.size()
        );

        return response;
    }

    private HomePageContent findContentOrThrow() {
        return contentRepository.findById(SINGLETON_CONTENT_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "Home page content not found. Please verify database initialization."
                ));
    }

    private List<HomePageProductDto> getHomePageProducts() {
        List<HomePageProduct> homePageProducts = homePageProductRepository.findAllByOrderByDisplayOrderAsc();
        Map<Long, ProductMedia> primaryMediaByProductId = productMediaQueryService.findPrimaryMediaForProducts(
                extractProducts(homePageProducts)
        );

        return homePageProducts.stream()
                .map(homePageProduct -> homePageMapper.toHomePageProductDto(
                        homePageProduct,
                        primaryMediaByProductId.get(homePageProduct.getProduct().getId())
                ))
                .toList();
    }

    private List<Product> extractProducts(List<HomePageProduct> homePageProducts) {
        return homePageProducts.stream()
                .map(HomePageProduct::getProduct)
                .toList();
    }

    private List<HomePageReviewDto> getFeaturedReviews() {
        return featuredReviewQueryService.findFeaturedReviews(FEATURED_REVIEWS_LIMIT)
                .stream()
                .map(featuredReview -> homePageMapper.toReviewDto(
                        featuredReview.review(),
                        featuredReview.media()
                ))
                .toList();
    }
}
