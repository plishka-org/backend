package org.plishka.backend.validation.impl;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;
import org.plishka.backend.validation.ValidEmail;

public class EmailValidator implements ConstraintValidator<ValidEmail, String> {
    private static final Pattern LOCAL_PART_PATTERN =
            Pattern.compile("^[A-Za-z0-9_%+\\-](?:[A-Za-z0-9._%+\\-]*[A-Za-z0-9_%+\\-])?$");
    private static final Pattern DOMAIN_LABEL_PATTERN =
            Pattern.compile("^[A-Za-z0-9](?:[A-Za-z0-9\\-]*[A-Za-z0-9])?$");
    private static final Pattern TOP_LEVEL_DOMAIN_PATTERN = Pattern.compile("^[A-Za-z]{2,63}$");

    private static final int MAX_LOCAL_PART_LENGTH = 64;
    private static final int MAX_DOMAIN_LABEL_LENGTH = 63;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }

        int atIndex = value.indexOf('@');
        if (atIndex <= 0 || atIndex != value.lastIndexOf('@') || atIndex == value.length() - 1) {
            return false;
        }

        String localPart = value.substring(0, atIndex);
        String domainPart = value.substring(atIndex + 1);

        return isValidLocalPart(localPart) && isValidDomainPart(domainPart);
    }

    private boolean isValidLocalPart(String localPart) {
        return localPart.length() <= MAX_LOCAL_PART_LENGTH
                && !localPart.contains("..")
                && LOCAL_PART_PATTERN.matcher(localPart).matches();
    }

    private boolean isValidDomainPart(String domainPart) {
        String[] labels = domainPart.split("\\.", -1);
        if (labels.length < 2) {
            return false;
        }

        for (int index = 0; index < labels.length - 1; index++) {
            if (!isValidDomainLabel(labels[index])) {
                return false;
            }
        }

        String topLevelDomain = labels[labels.length - 1];
        return topLevelDomain.length() <= MAX_DOMAIN_LABEL_LENGTH
                && TOP_LEVEL_DOMAIN_PATTERN.matcher(topLevelDomain).matches();
    }

    private boolean isValidDomainLabel(String label) {
        return !label.isEmpty()
                && label.length() <= MAX_DOMAIN_LABEL_LENGTH
                && DOMAIN_LABEL_PATTERN.matcher(label).matches();
    }
}
