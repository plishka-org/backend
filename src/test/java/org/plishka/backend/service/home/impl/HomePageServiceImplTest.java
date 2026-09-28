package org.plishka.backend.service.home.impl;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.plishka.backend.domain.home.HomePageContent;
import org.plishka.backend.domain.home.HomePageProduct;
import org.plishka.backend.domain.product.Category;
import org.plishka.backend.domain.product.Product;
import org.plishka.backend.dto.home.HomePageContentDto;
import org.plishka.backend.dto.home.HomePageProductDto;
import org.plishka.backend.dto.home.HomePageResponse;
import org.plishka.backend.dto.product.CategoryDto;
import org.plishka.backend.exception.RequiredSingletonUnavailableException;
import org.plishka.backend.mapper.home.HomePageMapper;
import org.plishka.backend.repository.home.HomePageContentRepository;
import org.plishka.backend.repository.home.HomePageProductRepository;
import org.plishka.backend.service.product.PriceVisibilityPolicy;
import org.plishka.backend.service.product.ProductMediaQueryService;
import org.plishka.backend.service.review.FeaturedReviewQueryService;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomePageServiceImplTest {
    @Mock
    private HomePageContentRepository contentRepository;

    @Mock
    private HomePageProductRepository homePageProductRepository;

    @Mock
    private ProductMediaQueryService productMediaQueryService;

    @Mock
    private FeaturedReviewQueryService featuredReviewQueryService;

    @Mock
    private HomePageMapper homePageMapper;

    @Mock
    private PriceVisibilityPolicy priceVisibilityPolicy;

    private HomePageServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new HomePageServiceImpl(
                contentRepository,
                homePageProductRepository,
                productMediaQueryService,
                featuredReviewQueryService,
                homePageMapper,
                priceVisibilityPolicy
        );
    }

    @Test
    void getHomePageData_ShouldUseOnlyVisibleHomeProducts() {
        HomePageContent content = homeContent();
        HomePageProduct homePageProduct = homePageProduct();
        Product product = homePageProduct.getProduct();
        HomePageContentDto contentDto = new HomePageContentDto("Plishka", "Handcrafted wooden goods.");
        HomePageProductDto productDto = new HomePageProductDto(
                product.getId(),
                product.getName(),
                new CategoryDto(product.getCategory().getId(), product.getCategory().getName()),
                1200L,
                null
        );
        HomePageResponse response = new HomePageResponse(contentDto, List.of(productDto), List.of());

        when(contentRepository.findById(1L)).thenReturn(Optional.of(content));
        when(homePageProductRepository.findAllVisibleByOrderByDisplayOrderAsc()).thenReturn(List.of(homePageProduct));
        when(productMediaQueryService.findPrimaryMediaForProducts(List.of(product))).thenReturn(Map.of());
        when(priceVisibilityPolicy.isCurrentPriceVisible()).thenReturn(true);
        when(priceVisibilityPolicy.visiblePrice(product, true)).thenReturn(1200L);
        when(homePageMapper.toContentDto(content)).thenReturn(contentDto);
        when(featuredReviewQueryService.findFeaturedReviews(5)).thenReturn(List.of());
        when(homePageMapper.toHomePageProductDto(homePageProduct, null, 1200L)).thenReturn(productDto);
        when(homePageMapper.toResponse(contentDto, List.of(productDto), List.of())).thenReturn(response);

        HomePageResponse result = service.getHomePageData();

        assertSame(response, result);
        verify(homePageProductRepository).findAllVisibleByOrderByDisplayOrderAsc();
    }

    @Test
    void getHomePageData_ShouldThrowOperationalException_WhenContentMissing() {
        when(contentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RequiredSingletonUnavailableException.class, service::getHomePageData);
    }

    private static HomePageContent homeContent() {
        HomePageContent content = new HomePageContent();
        content.setId(1L);
        content.setTitle("Plishka");
        content.setDescription("Handcrafted wooden goods.");
        return content;
    }

    private static HomePageProduct homePageProduct() {
        HomePageProduct homePageProduct = new HomePageProduct();
        homePageProduct.setId(1L);
        homePageProduct.setProduct(productWithCategory());
        homePageProduct.setDisplayOrder(1);
        return homePageProduct;
    }

    private static Product productWithCategory() {
        Category category = new Category();
        category.setId(2L);
        category.setName("Garden");

        Product product = new Product();
        product.setId(10L);
        product.setName("Garden bench");
        product.setPrice(1200L);
        product.setCategory(category);
        return product;
    }
}
