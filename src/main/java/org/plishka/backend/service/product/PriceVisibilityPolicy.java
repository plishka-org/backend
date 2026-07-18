package org.plishka.backend.service.product;

import lombok.RequiredArgsConstructor;
import org.plishka.backend.domain.product.Product;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.service.settings.ShopModeService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PriceVisibilityPolicy {
    private static final String PRICE_SORT_DISABLED_MESSAGE =
            "Price sorting is not available when shop mode is disabled";

    private final ShopModeService shopModeService;

    public boolean isCurrentPriceVisible() {
        return shopModeService.isShopModeEnabled();
    }

    public Long visiblePrice(Product product) {
        return visiblePrice(product, isCurrentPriceVisible());
    }

    public Long visiblePrice(Product product, boolean currentPriceVisible) {
        return currentPriceVisible ? product.getPrice() : null;
    }

    public void assertPriceSortAllowed(String sort, boolean currentPriceVisible) {
        if (isPriceSortDisabled(sort, currentPriceVisible)) {
            throw new BadRequestException(PRICE_SORT_DISABLED_MESSAGE);
        }
    }

    private boolean isPriceSortDisabled(String sort, boolean currentPriceVisible) {
        return ProductSortResolver.isPriceSort(sort) && !currentPriceVisible;
    }
}
