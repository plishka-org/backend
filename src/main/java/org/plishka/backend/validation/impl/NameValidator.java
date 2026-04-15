package org.plishka.backend.validation.impl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;
import org.plishka.backend.validation.ValidName;

public class NameValidator implements ConstraintValidator<ValidName, String> {
    private static final Pattern NAME_PATTERN = Pattern.compile(
            "^[\\p{IsLatin}\\p{IsCyrillic}]+(?:[ '\\-’ʼ][\\p{IsLatin}\\p{IsCyrillic}]+)*$"
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }

        return NAME_PATTERN.matcher(value).matches();
    }
}
