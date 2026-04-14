package org.plishka.backend.validation.impl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;
import org.plishka.backend.validation.ValidPassword;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {
    private static final Pattern PASSWORD_REGEX =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d~!\\?@#$%^&*_\\-+()\\[\\]{}><\\\\/|\"'.,:]+$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return PASSWORD_REGEX.matcher(value).matches();
    }
}
