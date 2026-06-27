package org.plishka.backend.service.admin.catalog.support;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.plishka.backend.dto.admin.product.BulkProductPriceOperation;
import org.plishka.backend.exception.BadRequestException;

public final class ProductPriceCalculator {
    private static final int PRICE_SCALE = 2;
    private static final BigDecimal PERCENT_DIVISOR = new BigDecimal("100");
    private static final BigDecimal MAX_PRODUCT_PRICE = new BigDecimal("99999999.99");

    private ProductPriceCalculator() {
    }

    public static BigDecimal calculate(
            BigDecimal currentPrice,
            BulkProductPriceOperation operation,
            BigDecimal value
    ) {
        BigDecimal newPrice = switch (operation) {
            case INCREASE_PERCENT -> currentPrice.add(currentPrice.multiply(value).divide(PERCENT_DIVISOR));
            case DECREASE_PERCENT -> currentPrice.subtract(currentPrice.multiply(value).divide(PERCENT_DIVISOR));
            case INCREASE_AMOUNT -> currentPrice.add(value);
            case DECREASE_AMOUNT -> currentPrice.subtract(value);
        };

        newPrice = normalize(newPrice);
        if (newPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Bulk price operation would produce non-positive price");
        }
        if (newPrice.compareTo(MAX_PRODUCT_PRICE) > 0) {
            throw new BadRequestException("Bulk price operation would exceed maximum product price");
        }

        return newPrice;
    }

    public static BigDecimal normalize(BigDecimal price) {
        return price.setScale(PRICE_SCALE, RoundingMode.UP);
    }
}
