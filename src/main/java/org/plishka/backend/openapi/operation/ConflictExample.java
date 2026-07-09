package org.plishka.backend.openapi.operation;

public enum ConflictExample {
    EMAIL_ALREADY_EXISTS("duplicateResource", "Duplicate resource", "Email already exists"),
    CATEGORY_ALREADY_EXISTS("duplicateResource", "Duplicate resource", "Category already exists"),
    PRODUCT_ALREADY_EXISTS("duplicateResource", "Duplicate resource", "Product already exists"),
    IDEMPOTENCY_CONFLICT(
            "idempotencyConflict",
            "Idempotency conflict",
            "Idempotency key was already used with a different request payload"
    );

    private final String exampleName;
    private final String summary;
    private final String message;

    ConflictExample(String exampleName, String summary, String message) {
        this.exampleName = exampleName;
        this.summary = summary;
        this.message = message;
    }

    public String exampleName() {
        return exampleName;
    }

    public String summary() {
        return summary;
    }

    public String message() {
        return message;
    }
}
