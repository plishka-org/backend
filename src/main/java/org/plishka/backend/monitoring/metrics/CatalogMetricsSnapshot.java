package org.plishka.backend.monitoring.metrics;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.plishka.backend.repository.product.CategoryRepository;
import org.plishka.backend.repository.product.ProductRepository;
import org.plishka.backend.repository.review.ReviewRepository;
import org.plishka.backend.service.settings.ShopModeService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class CatalogMetricsSnapshot {
    private static final long REFRESH_DELAY_MILLIS = 300_000L;

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ReviewRepository reviewRepository;
    private final ShopModeService shopModeService;

    private final AtomicLong shopModeEnabled = new AtomicLong();
    private final AtomicLong visibleProducts = new AtomicLong();
    private final AtomicLong visibleCategories = new AtomicLong();
    private final AtomicLong visibleReviews = new AtomicLong();

    @PostConstruct
    void initialize() {
        refresh();
    }

    @Scheduled(fixedDelay = REFRESH_DELAY_MILLIS, initialDelay = REFRESH_DELAY_MILLIS)
    @Transactional(readOnly = true)
    public void refresh() {
        try {
            shopModeEnabled.set(shopModeService.isShopModeEnabled() ? 1L : 0L);
            visibleProducts.set(productRepository.countByCategoryIsNotNull());
            visibleCategories.set(categoryRepository.count());
            visibleReviews.set(reviewRepository.count());
        } catch (RuntimeException exception) {
            log.warn("Catalog metrics snapshot refresh failed", exception);
        }
    }

    public long shopModeEnabled() {
        return shopModeEnabled.get();
    }

    public long visibleProducts() {
        return visibleProducts.get();
    }

    public long visibleCategories() {
        return visibleCategories.get();
    }

    public long visibleReviews() {
        return visibleReviews.get();
    }
}
