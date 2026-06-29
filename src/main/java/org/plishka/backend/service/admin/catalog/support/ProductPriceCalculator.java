package org.plishka.backend.service.admin.catalog.support;

import org.plishka.backend.dto.admin.product.BulkProductPriceOperation;
import org.plishka.backend.exception.BadRequestException;
import org.plishka.backend.service.pricing.PriceCalculator;

public final class ProductPriceCalculator {
    private static final long MAX_PRODUCT_PRICE = 10_000_000L;
    private static final long PERCENT_DIVISOR = 100L;
    private static final String PRODUCT_PRICE_REQUIRED_MESSAGE = "Product price is required";
    private static final String PRODUCT_PRICE_POSITIVE_MESSAGE = "Product price must be greater than 0";
    private static final String PRICE_OPERATION_REQUIRED_MESSAGE = "Price operation is required";
    private static final String PRICE_VALUE_POSITIVE_MESSAGE = "Price value must be greater than 0";
    private static final String BULK_PRICE_NON_POSITIVE_MESSAGE =
            "Bulk price operation would produce non-positive price";
    private static final String BULK_PRICE_OVERFLOW_MESSAGE =
            "Bulk price operation would exceed maximum product price";

    private ProductPriceCalculator() {
    }

    public static Long calculate(
            Long currentPrice,
            BulkProductPriceOperation operation,
            Long value
    ) {
        long validatedCurrentPrice = validateProductPrice(currentPrice);
        BulkProductPriceOperation validatedOperation = requireOperation(operation);
        long validatedValue = validateOperationValue(value);

        long calculatedPrice = applyOperation(validatedCurrentPrice, validatedOperation, validatedValue);

        return validateCalculatedPrice(calculatedPrice);
    }

    public static Long validateProductPrice(Long price) {
        if (price == null) {
            throw new BadRequestException(PRODUCT_PRICE_REQUIRED_MESSAGE);
        }
        if (price <= 0) {
            throw new BadRequestException(PRODUCT_PRICE_POSITIVE_MESSAGE);
        }
        if (price > MAX_PRODUCT_PRICE) {
            throw new BadRequestException("Product price must be at most " + MAX_PRODUCT_PRICE);
        }

        return price;
    }

    private static Long validateCalculatedPrice(long price) {
        if (price <= 0) {
            throw new BadRequestException(BULK_PRICE_NON_POSITIVE_MESSAGE);
        }
        if (price > MAX_PRODUCT_PRICE) {
            throw new BadRequestException(BULK_PRICE_OVERFLOW_MESSAGE);
        }

        return price;
    }

    private static BulkProductPriceOperation requireOperation(BulkProductPriceOperation operation) {
        if (operation == null) {
            throw new BadRequestException(PRICE_OPERATION_REQUIRED_MESSAGE);
        }

        return operation;
    }

    private static long validateOperationValue(Long value) {
        if (value == null || value <= 0) {
            throw new BadRequestException(PRICE_VALUE_POSITIVE_MESSAGE);
        }
        if (value > MAX_PRODUCT_PRICE) {
            throw new BadRequestException("Price value must be at most " + MAX_PRODUCT_PRICE);
        }

        return value;
    }

    private static long applyOperation(
            long currentPrice,
            BulkProductPriceOperation operation,
            long value
    ) {
        return switch (operation) {
            case INCREASE_PERCENT -> increaseByPercent(currentPrice, value);
            case DECREASE_PERCENT -> decreaseByPercent(currentPrice, value);
            case INCREASE_AMOUNT -> increaseByAmount(currentPrice, value);
            case DECREASE_AMOUNT -> decreaseByAmount(currentPrice, value);
        };
    }

    private static long increaseByPercent(long currentPrice, long percent) {
        return applyPercentDelta(currentPrice, percent);
    }

    private static long decreaseByPercent(long currentPrice, long percent) {
        return applyPercentDelta(currentPrice, -percent);
    }

    private static long applyPercentDelta(long currentPrice, long percentDelta) {
        long percentMultiplier = PriceCalculator.checkedAdd(
                PERCENT_DIVISOR,
                percentDelta,
                BULK_PRICE_OVERFLOW_MESSAGE
        );
        long unroundedPrice = PriceCalculator.checkedMultiply(
                currentPrice,
                percentMultiplier,
                BULK_PRICE_OVERFLOW_MESSAGE
        );
        return divideHalfUp(unroundedPrice, PERCENT_DIVISOR);
    }

    private static long divideHalfUp(long dividend, long divisor) {
        long halfDivisor = divisor / 2;
        if (dividend >= 0) {
            return (dividend + halfDivisor) / divisor;
        }

        return (dividend - halfDivisor) / divisor;
    }

    private static long increaseByAmount(long currentPrice, long amount) {
        return PriceCalculator.checkedAdd(currentPrice, amount, BULK_PRICE_OVERFLOW_MESSAGE);
    }

    private static long decreaseByAmount(long currentPrice, long amount) {
        return PriceCalculator.checkedAdd(currentPrice, -amount, BULK_PRICE_OVERFLOW_MESSAGE);
    }
}
