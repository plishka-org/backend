package org.plishka.backend.validation.impl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;
import org.plishka.backend.validation.ValidCity;

public class CityValidator implements ConstraintValidator<ValidCity, String> {
    private static final Pattern CITY_PATTERN = Pattern.compile(
            "^[\\p{IsLatin}\\p{IsCyrillic}]+(?:[ '\\-\\x{2019}\\x{02BC}][\\p{IsLatin}\\p{IsCyrillic}]+)*$"
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }

        return CITY_PATTERN.matcher(value).matches();
    }
}
