package org.plishka.backend.service.home.impl;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.domain.home.HomePageAdvantage;
import org.plishka.backend.domain.home.HomePageContent;
import org.plishka.backend.domain.home.HomePageProduct;
import org.plishka.backend.dto.home.HomePageAdvantageDto;
import org.plishka.backend.dto.home.HomePageContentDto;
import org.plishka.backend.dto.home.HomePageProductDto;
import org.plishka.backend.dto.home.HomePageResponse;
import org.plishka.backend.repository.home.HomePageAdvantageRepository;
import org.plishka.backend.repository.home.HomePageContentRepository;
import org.plishka.backend.repository.home.HomePageProductRepository;
import org.plishka.backend.service.home.HomePageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class HomePageServiceImpl implements HomePageService {
    private static final long SINGLETON_CONTENT_ID = 1L;

    private final HomePageContentRepository contentRepository;
    private final HomePageAdvantageRepository advantageRepository;
    private final HomePageProductRepository homePageProductRepository;

    @Override
    @Transactional(readOnly = true)
    public HomePageResponse getHomePageData() {
        log.debug("Fetching home page data");

        HomePageContent content = findContentOrThrow();
        List<HomePageAdvantageDto> advantages = getAdvantages();
        List<HomePageProductDto> products = getProducts();

        HomePageResponse response = new HomePageResponse(
                mapToContentDto(content),
                advantages,
                products
        );

        log.info(
                "Home page data fetched successfully: advantagesCount={}, productsCount={}",
                advantages.size(),
                products.size()
        );

        return response;
    }

    private HomePageContent findContentOrThrow() {
        return contentRepository.findById(SINGLETON_CONTENT_ID)
                .orElseThrow(() -> new IllegalStateException(
                        "Home page content not found. Please verify database initialization."
                ));
    }

    private List<HomePageAdvantageDto> getAdvantages() {
        return advantageRepository.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(this::mapToAdvantageDto)
                .toList();
    }

    private List<HomePageProductDto> getProducts() {
        return homePageProductRepository.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(this::mapToProductDto)
                .toList();
    }

    private HomePageContentDto mapToContentDto(HomePageContent content) {
        return new HomePageContentDto(
                content.getTitle(),
                content.getDescription()
        );
    }

    private HomePageAdvantageDto mapToAdvantageDto(HomePageAdvantage advantage) {
        return new HomePageAdvantageDto(
                advantage.getId(),
                advantage.getTitle(),
                advantage.getDescription(),
                advantage.getIconS3Key()
        );
    }

    private HomePageProductDto mapToProductDto(HomePageProduct product) {
        // TODO: PLIS-XXX - Fetch real product name and photoUrl from ProductRepository
        // when the Product entity is implemented.
        return new HomePageProductDto(
                product.getProductId(),
                "Виріб #" + product.getProductId(),
                null
        );
    }
}
