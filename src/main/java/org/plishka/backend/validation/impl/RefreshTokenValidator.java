package org.plishka.backend.validation.impl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.plishka.backend.util.TokenGenerator;
import org.plishka.backend.validation.ValidRefreshToken;

public class RefreshTokenValidator
        implements ConstraintValidator<ValidRefreshToken, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return false;
        }

        return value.length() == TokenGenerator.REFRESH_TOKEN_LENGTH
                && value.matches(TokenGenerator.URL_SAFE_TOKEN_REGEX);
    }
}
