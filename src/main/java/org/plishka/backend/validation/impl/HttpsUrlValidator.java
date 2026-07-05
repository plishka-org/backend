package org.plishka.backend.validation.impl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.plishka.backend.util.UrlValidationUtil;
import org.plishka.backend.validation.ValidHttpsUrl;

public class HttpsUrlValidator implements ConstraintValidator<ValidHttpsUrl, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }

        return UrlValidationUtil.isValidAbsoluteHttpsUrl(value);
    }
}
