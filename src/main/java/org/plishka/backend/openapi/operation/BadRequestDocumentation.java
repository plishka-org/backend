package org.plishka.backend.openapi.operation;

import java.util.List;

public record BadRequestDocumentation(
        boolean validation,
        boolean requestBody,
        String businessMessage,
        List<String> validationFieldErrors
) {
    private static final BadRequestDocumentation NONE =
            new BadRequestDocumentation(false, false, null, List.of());

    public BadRequestDocumentation {
        validationFieldErrors = List.copyOf(validationFieldErrors);
    }

    static BadRequestDocumentation none() {
        return NONE;
    }

    static BadRequestDocumentation validation(boolean requestBody, String... fieldErrors) {
        return new BadRequestDocumentation(true, requestBody, null, List.of(fieldErrors));
    }

    static BadRequestDocumentation validationAndBusiness(
            boolean requestBody,
            String businessMessage,
            String... fieldErrors
    ) {
        return new BadRequestDocumentation(true, requestBody, businessMessage, List.of(fieldErrors));
    }

    public boolean hasBusiness() {
        return businessMessage != null;
    }

    boolean any() {
        return validation || hasBusiness();
    }
}
