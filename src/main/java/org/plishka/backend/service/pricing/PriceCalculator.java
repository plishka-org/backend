package org.plishka.backend.service.pricing;

import java.util.Collection;
import org.plishka.backend.exception.BadRequestException;

public final class PriceCalculator {
    private static final String UNIT_PRICE_REQUIRED_MESSAGE = "Unit price is required";
    private static final String QUANTITY_REQUIRED_MESSAGE = "Quantity is required";
    private static final String PRICE_AMOUNTS_REQUIRED_MESSAGE = "Price amounts are required";
    private static final String PRICE_AMOUNT_REQUIRED_MESSAGE = "Price amount is required";
    private static final String LINE_TOTAL_OVERFLOW_MESSAGE = "Line total exceeds maximum supported value";
    private static final String TOTAL_PRICE_OVERFLOW_MESSAGE = "Total price exceeds maximum supported value";

    private PriceCalculator() {
    }

    public static Long calculateLineTotal(Long unitPrice, Integer quantity) {
        long requiredUnitPrice = requireLong(unitPrice, UNIT_PRICE_REQUIRED_MESSAGE);
        int requiredQuantity = requireInteger(quantity, QUANTITY_REQUIRED_MESSAGE);

        return checkedMultiply(requiredUnitPrice, requiredQuantity, LINE_TOTAL_OVERFLOW_MESSAGE);
    }

    public static Long calculateTotal(Collection<Long> amounts) {
        if (amounts == null) {
            throw new BadRequestException(PRICE_AMOUNTS_REQUIRED_MESSAGE);
        }

        long total = 0L;
        for (Long amount : amounts) {
            total = checkedAdd(
                    total,
                    requireLong(amount, PRICE_AMOUNT_REQUIRED_MESSAGE),
                    TOTAL_PRICE_OVERFLOW_MESSAGE
            );
        }

        return total;
    }

    public static long checkedAdd(long left, long right, String overflowMessage) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException exception) {
            throw new BadRequestException(overflowMessage, exception);
        }
    }

    public static long checkedMultiply(long left, long right, String overflowMessage) {
        try {
            return Math.multiplyExact(left, right);
        } catch (ArithmeticException exception) {
            throw new BadRequestException(overflowMessage, exception);
        }
    }

    private static long requireLong(Long value, String message) {
        if (value == null) {
            throw new BadRequestException(message);
        }

        return value;
    }

    private static int requireInteger(Integer value, String message) {
        if (value == null) {
            throw new BadRequestException(message);
        }

        return value;
    }
}
