package org.plishka.backend.validation.impl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.UUID;
import org.plishka.backend.validation.ValidDeviceId;

public class DeviceIdValidator implements ConstraintValidator<ValidDeviceId, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return false;
        }

        String trimmedValue = value.trim();

        try {
            UUID parsedUuid = UUID.fromString(trimmedValue);
            return parsedUuid.toString().equalsIgnoreCase(trimmedValue);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
