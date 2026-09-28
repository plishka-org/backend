package org.plishka.backend.validation.impl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;
import org.plishka.backend.validation.ValidPhone;

public class PhoneValidator implements ConstraintValidator<ValidPhone, String> {
    private static final Pattern PHONE_REGEX = Pattern.compile("^\\+380\\d{9}$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return PHONE_REGEX.matcher(value).matches();
    }
}
