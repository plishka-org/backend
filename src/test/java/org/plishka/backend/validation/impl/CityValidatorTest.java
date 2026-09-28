package org.plishka.backend.validation.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CityValidatorTest {
    private final CityValidator validator = new CityValidator();

    @Test
    void isValid_ShouldAcceptDeliveryAddressesWithDigitsPeriodsAndCommas() {
        assertTrue(validator.isValid("вул. Шевченка, 25", null));
        assertTrue(validator.isValid("м. Конотоп обл. Сумська", null));
    }

    @Test
    void isValid_ShouldRejectUnsupportedCharacters() {
        assertFalse(validator.isValid("вул. Шевченка, 25@", null));
    }
}
