package org.plishka.backend.validation.impl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;
import org.plishka.backend.validation.ValidOrderNote;

public class OrderNoteValidator implements ConstraintValidator<ValidOrderNote, String> {
    private static final Pattern CONTAINS_LETTER = Pattern.compile("[\\p{IsLatin}\\p{IsCyrillic}]");
    private static final Pattern ALLOWED_CHARACTERS = Pattern.compile(
            "^[\\p{IsLatin}\\p{IsCyrillic}\\d\\s.,!?;:()'\"\\-/№#%&+@\\x{2019}\\x{02BC}]*$"
    );

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }

        return CONTAINS_LETTER.matcher(value).find()
                && ALLOWED_CHARACTERS.matcher(value).matches();
    }
}
