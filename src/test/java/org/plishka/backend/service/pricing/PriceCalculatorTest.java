package org.plishka.backend.service.pricing;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.plishka.backend.exception.BadRequestException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PriceCalculatorTest {
    @Test
    void lineTotal_ShouldMultiplyUnitPriceByQuantity() {
        assertEquals(900L, PriceCalculator.calculateLineTotal(450L, 2));
    }

    @Test
    void lineTotal_ShouldThrowBadRequest_WhenMultiplicationOverflows() {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> PriceCalculator.calculateLineTotal(Long.MAX_VALUE, 2)
        );

        assertEquals("Line total exceeds maximum supported value", exception.getMessage());
    }

    @Test
    void total_ShouldAddAmounts() {
        assertEquals(1350L, PriceCalculator.calculateTotal(List.of(450L, 900L)));
    }

    @Test
    void total_ShouldThrowBadRequest_WhenAdditionOverflows() {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> PriceCalculator.calculateTotal(List.of(Long.MAX_VALUE, 1L))
        );

        assertEquals("Total price exceeds maximum supported value", exception.getMessage());
    }
}
